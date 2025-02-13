package io.github.arashiyama11.dncl.parser

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import io.github.arashiyama11.dncl.lexer.ILexer
import io.github.arashiyama11.dncl.model.DnclError
import io.github.arashiyama11.dncl.model.LexerError
import io.github.arashiyama11.dncl.model.AstNode
import io.github.arashiyama11.dncl.model.ExpressionStopToken
import io.github.arashiyama11.dncl.model.InternalError
import io.github.arashiyama11.dncl.model.ParserError
import io.github.arashiyama11.dncl.model.Precedence
import io.github.arashiyama11.dncl.model.Token

class Parser private constructor(private val lexer: ILexer) : IParser {
    private lateinit var currentToken: Token
    private lateinit var nextToken: Token
    private lateinit var aheadToken: Token

    annotation class EnsuredEndOfLine

    override fun parseProgram(): Either<DnclError, AstNode.Program> {
        try {
            while (currentToken is Token.NewLine || (currentToken is Token.Indent && (currentToken as Token.Indent).depth == 0)) {
                nextToken().getOrElse { return it.left() }
            }
            val statements = mutableListOf<AstNode.Statement>()
            while (currentToken !is Token.EOF) {
                val statement = parseStatement().getOrElse {
                    return it.left()
                }
                statements.add(statement)
                while (currentToken is Token.NewLine || (currentToken is Token.Indent && (currentToken as Token.Indent).depth == 0)) {
                    nextToken().getOrElse { return it.left() }
                }
            }
            return AstNode.Program(statements).right()
        } catch (e: Throwable) {
            return InternalError(e.message ?: e.stackTraceToString()).left()
        }
    }

    @EnsuredEndOfLine
    private fun parseStatement(): Either<DnclError, AstNode.Statement> {
        val node = when (currentToken) {
            is Token.If -> parseIfStatement()

            is Token.Identifier -> when (nextToken) {
                is Token.Assign, is Token.BracketOpen -> parseAssignStatement()
                is Token.Wo -> parseForStatement()
                else -> parseExpressionStatement()
            }

            else -> {
                parseExpressionStatement()
            }
        }

        requireEndOfLine().getOrElse { return it.left() }

        return node
    }

    private fun parseExpression(precedence: Precedence): Either<DnclError, AstNode.Expression> {
        var left = prefixParseFn(currentToken).getOrElse { return it.left() }
        while ((nextToken !is ExpressionStopToken && precedence < nextToken.precedence()) && left !is AstNode.WhileExpression) {
            nextToken().getOrElse { return it.left() }
            left = infixParseFn(left).getOrElse { return it.left() }
        }
        if (nextToken is Token.NewLine || nextToken is Token.EOF) nextToken().getOrElse { return it.left() }
        return left.right()
    }


    @EnsuredEndOfLine
    private fun parseExpressionStatement(): Either<DnclError, AstNode.ExpressionStatement> =
        either {
            val expression = parseExpression(Precedence.LOWEST).getOrElse { return it.left() }
            requireEndOfLine().getOrElse { return it.left() }
            AstNode.ExpressionStatement(expression)
        }

    private fun nextToken(): Either<LexerError, Token> {
        currentToken = nextToken
        nextToken = aheadToken
        aheadToken = lexer.nextToken().getOrElse { return it.left() }
        return currentToken.right()
    }

    private fun requireEndOfLine(): Either<DnclError, Unit> = either {
        if (currentToken is Token.EOF) return@either
        if (currentToken is Token.NewLine && nextToken is Token.Indent) return@either
        raise(
            ParserError.UnExpectedToken(
                currentToken,
                "require NewLine and Indent :${Exception().stackTraceToString()}"
            )
        )
    }

    private inline fun <reified T> expectNextToken(): Either<DnclError, Unit> = either {
        nextToken()
        if (currentToken !is T) {
            raise(
                ParserError.UnExpectedToken(
                    currentToken,
                    expectedToken = T::class.java.simpleName
                )
            )
        }
    }

    @EnsuredEndOfLine
    private fun parseBlockStatement(): Either<DnclError, AstNode.BlockStatement> = either {
        val statements = mutableListOf<AstNode.Statement>()
        expectNextToken<Token.NewLine>().getOrElse { return it.left() }
        nextToken()
        val startDepth = if (currentToken is Token.Indent) {
            (currentToken as Token.Indent).depth
        } else raise(ParserError.UnExpectedToken(currentToken))
        nextToken().getOrElse { return it.left() }
        while (currentToken !is Token.EOF) {
            if (currentToken is Token.NewLine) {
                val depth = (nextToken as? Token.Indent)?.depth ?: raise(
                    ParserError.UnExpectedToken(currentToken, "what the ")
                )
                if (depth > startDepth) {
                    raise(ParserError.IndentError(nextToken, startDepth))
                } else if (depth < startDepth) {
                    break
                } else {
                    nextToken().getOrElse { return it.left() }
                    nextToken().getOrElse { return it.left() }
                }
            }
            val statement = parseStatement().getOrElse { return it.left() }
            statements.add(statement)
        }
        requireEndOfLine().getOrElse { return it.left() }
        return AstNode.BlockStatement(statements).right()
    }

    @EnsuredEndOfLine
    private fun parseIfStatement(): Either<DnclError, AstNode.Statement> = either {
        nextToken()
        val condition = parseExpression(Precedence.LOWEST).getOrElse { return it.left() }

        expectNextToken<Token.Then>().getOrElse { return it.left() }
        expectNextToken<Token.Colon>().getOrElse { return it.left() }

        val consequence = parseBlockStatement().getOrElse { return it.left() }
        requireEndOfLine().getOrElse { return it.left() }
        if (aheadToken is Token.Else) {
            expectNextToken<Token.Indent>()
            expectNextToken<Token.Else>().getOrElse { return it.left() }
            expectNextToken<Token.Colon>().getOrElse { return it.left() }
            val alternative = parseBlockStatement().getOrElse { return it.left() }
            AstNode.IfStatement(condition, consequence, alternative)
        } else if (aheadToken is Token.Elif) {
            expectNextToken<Token.Indent>()
            expectNextToken<Token.Elif>().getOrElse { return it.left() }
            AstNode.IfStatement(
                condition,
                consequence,
                AstNode.BlockStatement(listOf(parseIfStatement().getOrElse { return it.left() }))
            )
        } else {
            AstNode.IfStatement(condition, consequence, null)
        }
    }

    @EnsuredEndOfLine
    private fun parseForStatement(): Either<DnclError, AstNode.ForStatement> = either {
        val counter = (currentToken as? Token.Identifier)
            ?: return ParserError.UnExpectedToken(currentToken).left()
        expectNextToken<Token.Wo>().getOrElse { return it.left() }
        nextToken().getOrElse { return it.left() }
        val start = parseExpression(Precedence.LOWEST).getOrElse { return it.left() }
        expectNextToken<Token.Kara>().getOrElse { return it.left() }
        nextToken().getOrElse { return it.left() }
        val end = parseExpression(Precedence.LOWEST).getOrElse { return it.left() }
        expectNextToken<Token.Made>().getOrElse { return it.left() }
        nextToken().getOrElse { return it.left() }
        val step = parseExpression(Precedence.LOWEST).getOrElse { return it.left() }
        nextToken().getOrElse { return it.left() }
        val type = when (currentToken) {
            is Token.UpTo -> AstNode.ForStatement.Companion.StepType.INCREMENT
            is Token.DownTo -> AstNode.ForStatement.Companion.StepType.DECREMENT
            else -> raise(ParserError.UnExpectedToken(currentToken))
        }
        expectNextToken<Token.Colon>().getOrElse { return it.left() }
        val block = parseBlockStatement().getOrElse { return it.left() }
        AstNode.ForStatement(counter, start, end, step, type, block)
    }

    @EnsuredEndOfLine
    private fun parseAssignStatement(): Either<DnclError, AstNode.AssignStatement> = either {
        val assignments: MutableList<Pair<AstNode.Assignable, AstNode.Expression>> = mutableListOf()
        while (currentToken !is Token.NewLine && currentToken !is Token.EOF) {
            if (currentToken !is Token.Identifier) raise(
                ParserError.UnExpectedToken(
                    currentToken,
                    expectedToken = "Identifier"
                )
            )
            val identifier = (currentToken as? Token.Identifier)
            val left = if (nextToken is Token.BracketOpen) {
                val op = nextToken
                expectNextToken<Token.BracketOpen>()
                nextToken().getOrElse { return it.left() }
                AstNode.IndexExpression(
                    AstNode.Identifier(identifier!!.literal),
                    parseExpressionList<Token.BracketClose>().getOrElse { return it.left() }
                        .firstOrNull() ?: raise(ParserError.UnExpectedToken(currentToken)),
                    op
                )
            } else {
                AstNode.Identifier(identifier!!.literal)
            }

            expectNextToken<Token.Assign>().getOrElse { return it.left() }

            nextToken().getOrElse { return it.left() }
            val right = parseExpression(Precedence.LOWEST).getOrElse { return it.left() }
            assignments.add(left to right)
            if (nextToken is Token.Comma) {
                nextToken().getOrElse { return it.left() }
                nextToken().getOrElse { return it.left() }
            } else break
        }
        requireEndOfLine().getOrElse { return it.left() }
        AstNode.AssignStatement(assignments)
    }

    private fun prefixParseFn(token: Token): Either<DnclError, AstNode.Expression> = either {
        when (token) {
            is Token.Identifier -> {
                val identifier = (currentToken as? Token.Identifier)
                    ?: raise(ParserError.UnExpectedToken(currentToken))
                AstNode.Identifier(identifier.literal)
            }

            is Token.Japanese -> {
                val identifier = (currentToken as? Token.Japanese)
                    ?: raise(ParserError.UnExpectedToken(currentToken))
                AstNode.Identifier(identifier.literal)
            }

            is Token.Int -> {
                val int = (currentToken as? Token.Int)
                    ?: raise(ParserError.UnExpectedToken(currentToken))
                AstNode.IntLiteral(
                    int.literal.toIntOrNull()
                        ?: raise(ParserError.InvalidIntLiteral(int))
                )
            }

            is Token.Float -> {
                val float = (currentToken as? Token.Float)
                    ?: raise(ParserError.UnExpectedToken(currentToken))
                AstNode.FloatLiteral(
                    float.literal.toFloatOrNull()
                        ?: raise(ParserError.InvalidFloatLiteral(float))
                )
            }

            is Token.String -> {
                val string = (currentToken as? Token.String)
                    ?: return ParserError.UnExpectedToken(currentToken).left()
                AstNode.StringLiteral(string.literal)
            }

            is Token.Bang, is Token.Minus, is Token.Plus -> {
                val operator = currentToken
                nextToken().getOrElse { return it.left() }
                val right = parseExpression(Precedence.PREFIX).getOrElse { return it.left() }
                AstNode.PrefixExpression(operator, right)
            }

            is Token.ParenOpen -> {
                nextToken().getOrElse { return it.left() }
                val expression = parseExpression(Precedence.LOWEST).getOrElse { return it.left() }
                expectNextToken<Token.ParenClose>().getOrElse { return it.left() }
                //nextToken().getOrElse { return it.left() }
                expression
            }

            is Token.BracketOpen -> {
                nextToken().getOrElse { return it.left() }
                val exps = parseExpressionList<Token.BracketClose>().getOrElse { return it.left() }
                nextToken().getOrElse { return it.left() }
                AstNode.ArrayLiteral(exps)
            }

            is Token.LenticularOpen -> {
                expectNextToken<Token.Japanese>()
                val string = (currentToken as? Token.Japanese) ?: raise(
                    ParserError.UnExpectedToken(
                        currentToken
                    )
                )
                expectNextToken<Token.LenticularClose>().getOrElse { return it.left() }
                AstNode.SystemLiteral(string.literal)
            }

            else -> {
                raise(ParserError.UnknownPrefixOperator(token))
            }
        }
    }

    private inline fun <reified T> parseExpressionList(): Either<DnclError, List<AstNode.Expression>> =
        either {
            if (currentToken is T) {
                return emptyList<AstNode.Expression>().right()
            }
            val list =
                mutableListOf(parseExpression(Precedence.LOWEST).getOrElse { return it.left() })
            while (currentToken is Token.Indent || currentToken is Token.NewLine) nextToken().getOrElse { return it.left() }

            while (nextToken is Token.Comma) {

                nextToken().getOrElse { return it.left() }
                while (currentToken is Token.Indent || currentToken is Token.NewLine) nextToken().getOrElse { return it.left() }


                nextToken().getOrElse { return it.left() }
                while (currentToken is Token.Indent || currentToken is Token.NewLine) nextToken().getOrElse { return it.left() }


                list.add(parseExpression(Precedence.LOWEST).getOrElse { return it.left() })

            }

            expectNextToken<T>().getOrElse { return it.left() }
            return list.toList().right()
        }

    private fun infixParseFn(left: AstNode.Expression): Either<DnclError, AstNode.Expression> {
        return when (currentToken) {
            is Token.Plus, is Token.Minus, is Token.Times, is Token.Divide, is Token.DivideInt, is Token.Modulo, is Token.Equal, is Token.NotEqual, is Token.GreaterThan, is Token.LessThan, is Token.GreaterThanOrEqual, is Token.LessThanOrEqual, is Token.And, is Token.Or -> {
                val operator = currentToken
                val precedence = currentToken.precedence()
                nextToken().getOrElse { return it.left() }
                val right = parseExpression(precedence).getOrElse { return it.left() }
                AstNode.InfixExpression(left, operator, right).right()
            }

            is Token.BracketOpen -> {
                nextToken().getOrElse { return it.left() }
                val index = parseExpression(Precedence.LOWEST).getOrElse { return it.left() }
                expectNextToken<Token.BracketClose>().getOrElse { return it.left() }
                AstNode.IndexExpression(left, index, currentToken).right()
            }

            is Token.ParenOpen -> {
                nextToken().getOrElse { return it.left() }
                val args = parseExpressionList<Token.ParenClose>().getOrElse { return it.left() }
                AstNode.CallExpression(left, args).right()
            }


            is Token.While -> {
                expectNextToken<Token.Colon>().getOrElse { return it.left() }
                val block = parseBlockStatement().getOrElse { return it.left() }
                AstNode.WhileExpression(left, block).right()
            }

            else -> ParserError.UnknownInfixOperator(currentToken).left()
        }
    }


    companion object {
        operator fun invoke(lexer: ILexer): Either<LexerError, Parser> {
            val parser = Parser(lexer)
            parser.currentToken = lexer.nextToken().getOrElse { return it.left() }
            parser.nextToken = lexer.nextToken().getOrElse { return it.left() }
            parser.aheadToken = lexer.nextToken().getOrElse { return it.left() }
            return parser.right()
        }

        fun Token.precedence() = when (this) {
            is Token.Plus, is Token.Minus -> Precedence.SUM
            is Token.Times, is Token.Divide, is Token.DivideInt, is Token.Modulo -> Precedence.PRODUCT
            is Token.GreaterThan, is Token.LessThan, is Token.GreaterThanOrEqual, is Token.LessThanOrEqual -> Precedence.LESSGREATER
            is Token.Equal, is Token.NotEqual -> Precedence.EQUALS
            is Token.Bang, is Token.LenticularOpen -> Precedence.PREFIX
            is Token.BracketOpen -> Precedence.INDEX
            is Token.ParenOpen -> Precedence.CALL
            is Token.While -> Precedence.WHILE
            is Token.And -> Precedence.AND
            is Token.Or -> Precedence.OR
            else -> Precedence.LOWEST
        }
    }
}