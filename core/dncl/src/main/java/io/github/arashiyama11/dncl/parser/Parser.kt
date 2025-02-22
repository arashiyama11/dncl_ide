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
import io.github.arashiyama11.dncl.model.InfixExpressionToken
import io.github.arashiyama11.dncl.model.InternalError
import io.github.arashiyama11.dncl.model.ParserError
import io.github.arashiyama11.dncl.model.Precedence
import io.github.arashiyama11.dncl.model.PrefixExpressionToken
import io.github.arashiyama11.dncl.model.Token

class Parser private constructor(private val lexer: ILexer) : IParser {
    private val indentStack: MutableList<Int> = mutableListOf(0)
    private lateinit var currentToken: Token
    private lateinit var nextToken: Token
    private lateinit var aheadToken: Token


    annotation class EnsuredEndOfLine

    override fun parseProgram(): Either<DnclError, AstNode.Program> = Either.catch {
        indentStack.clear()
        indentStack.add(0)
        while (currentToken is Token.NewLine || (currentToken is Token.Indent && (currentToken as Token.Indent).depth == 0)) {
            nextToken().getOrElse { return it.left() }
        }
        val statements = mutableListOf<AstNode.Statement>()
        while (currentToken !is Token.EOF) {
            val statement = parseStatement().getOrElse {
                return it.left()
            }
            statements.add(statement)

            while (aheadToken is Token.NewLine) {
                expectNextToken<Token.Indent>().getOrElse { return it.left() }
                expectNextToken<Token.NewLine>().getOrElse { return it.left() }
            }

            while (currentToken is Token.NewLine || (currentToken is Token.Indent && (currentToken as Token.Indent).depth == 0)) {
                nextToken().getOrElse { return it.left() }
            }
        }
        AstNode.Program(statements)
    }.mapLeft { InternalError(it.message ?: "") }


    @EnsuredEndOfLine
    private fun parseStatement(): Either<DnclError, AstNode.Statement> = either {
        val node = when (currentToken) {
            is Token.If -> parseIfStatement()
            is Token.Function -> parseFunctionStatement()

            is Token.Identifier -> when (nextToken) {
                is Token.Assign, is Token.BracketOpen -> parseAssignStatement()
                is Token.Wo -> parseForStatement()
                else -> parseExpressionStatement()
            }

            is Token.Indent -> raise(ParserError.IndentError(currentToken, "in $indentStack"))

            else -> parseExpressionStatement()
        }.bind()


        requireEndOfLine().bind()

        return node.right()
    }

    private fun parseExpression(precedence: Precedence): Either<DnclError, AstNode.Expression> =
        either {
            var left = prefixParseFn(currentToken).bind()
            while ((nextToken !is ExpressionStopToken && precedence < nextToken.precedence()) && left !is AstNode.WhileExpression) {
                nextToken().bind()
                left = infixParseFn(left).bind()
            }
            if (nextToken is Token.NewLine || nextToken is Token.EOF) nextToken().bind()
            return left.right()
        }


    @EnsuredEndOfLine
    private fun parseExpressionStatement(): Either<DnclError, AstNode.Statement> =
        either {
            val expression = parseExpression(Precedence.LOWEST).bind()
            requireEndOfLine().bind()
            if (expression is AstNode.WhileExpression) {
                expression.toStatement()
            } else AstNode.ExpressionStatement(expression)
        }

    private fun nextToken(): Either<LexerError, Token> = either {
        currentToken = nextToken
        nextToken = aheadToken
        aheadToken = run {
            var tok = lexer.nextToken().bind()
            while (tok is Token.Comment) {
                tok = lexer.nextToken().bind()
            }
            tok
        }
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
        expectNextToken<Token.NewLine>().bind()
        val startDepth = if (nextToken is Token.Indent) {
            (nextToken as Token.Indent).depth
        } else raise(ParserError.UnExpectedToken(nextToken))
        if ((indentStack.lastOrNull() ?: raise(
                ParserError.IndentError(
                    nextToken,
                    0
                )
            )) >= startDepth
        ) raise(
            ParserError.IndentError(
                nextToken,
                ">${indentStack.lastOrNull()}"
            )
        )
        indentStack.add(startDepth)
        while (currentToken !is Token.EOF) {
            if (currentToken !is Token.NewLine) raise(
                ParserError.UnExpectedToken(
                    currentToken
                )
            )

            while (aheadToken is Token.NewLine) {
                expectNextToken<Token.Indent>().bind()
                expectNextToken<Token.NewLine>().bind()
            }

            val depth = (nextToken as? Token.Indent)?.depth ?: raise(
                ParserError.UnExpectedToken(currentToken)
            )


            if (!indentStack.contains(depth)) {
                raise(ParserError.IndentError(nextToken, "in $indentStack"))
            }

            if (depth < startDepth) {
                indentStack.removeLastOrNull()
                break
            } else {
                nextToken().bind()
                nextToken().bind()
            }
            val statement = parseStatement().bind()
            statements.add(statement)
        }
        requireEndOfLine().bind()
        return AstNode.BlockStatement(statements).right()
    }

    @EnsuredEndOfLine
    private fun parseIfStatement(): Either<DnclError, AstNode.Statement> = either {
        nextToken()
        val condition = parseExpression(Precedence.LOWEST).bind()

        expectNextToken<Token.Then>().bind()
        expectNextToken<Token.Colon>().bind()

        val consequence = parseBlockStatement().bind()
        requireEndOfLine().bind()
        if (aheadToken is Token.Else) {
            expectNextToken<Token.Indent>()
            expectNextToken<Token.Else>().bind()
            expectNextToken<Token.Colon>().bind()
            val alternative = parseBlockStatement().bind()
            AstNode.IfStatement(condition, consequence, alternative)
        } else if (aheadToken is Token.Elif) {
            expectNextToken<Token.Indent>()
            expectNextToken<Token.Elif>().bind()
            AstNode.IfStatement(
                condition,
                consequence,
                AstNode.BlockStatement(listOf(parseIfStatement().bind()))
            )
        } else {
            AstNode.IfStatement(condition, consequence, null)
        }
    }

    @EnsuredEndOfLine
    private fun parseForStatement(): Either<DnclError, AstNode.ForStatement> = either {
        val counter = (currentToken as? Token.Identifier)
            ?: return ParserError.UnExpectedToken(currentToken).left()
        expectNextToken<Token.Wo>().bind()
        nextToken().bind()
        val start = parseExpression(Precedence.LOWEST).bind()
        expectNextToken<Token.Kara>().bind()
        nextToken().bind()
        val end = parseExpression(Precedence.LOWEST).bind()
        expectNextToken<Token.Made>().bind()
        nextToken().bind()
        val step = parseExpression(Precedence.LOWEST).bind()
        nextToken().bind()
        val type = when (currentToken) {
            is Token.UpTo -> AstNode.ForStatement.Companion.StepType.INCREMENT
            is Token.DownTo -> AstNode.ForStatement.Companion.StepType.DECREMENT
            else -> raise(ParserError.UnExpectedToken(currentToken))
        }
        expectNextToken<Token.Colon>().bind()
        val block = parseBlockStatement().bind()
        AstNode.ForStatement(counter, start, end, step, type, block)
    }

    @EnsuredEndOfLine
    private fun parseFunctionStatement(): Either<DnclError, AstNode.FunctionStatement> = either {
        nextToken().bind()
        val name = (currentToken as? Token.Identifier)
            ?: currentToken as? Token.Japanese ?: raise(ParserError.UnExpectedToken(currentToken))
        expectNextToken<Token.ParenOpen>().bind()
        nextToken().bind()
        val params = parseExpressionList<Token.ParenClose>().bind()
        params.any { it !is AstNode.Identifier }.let {
            if (it) raise(ParserError.UnExpectedToken(currentToken))
        }
        expectNextToken<Token.Wo>().bind()
        expectNextToken<Token.Colon>().bind()
        val block = parseBlockStatement().bind()
        expectNextToken<Token.Indent>().bind()
        if ((currentToken as Token.Indent).depth != indentStack.lastOrNull()) {
            raise(ParserError.IndentError(currentToken, indentStack.lastOrNull().toString()))
        }
        expectNextToken<Token.Define>().bind()
        nextToken().bind()
        requireEndOfLine().bind()
        AstNode.FunctionStatement(
            name.literal,
            params.map { (it as AstNode.Identifier).value },
            block
        )
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
                expectNextToken<Token.BracketOpen>()
                nextToken().bind()
                AstNode.IndexExpression(
                    AstNode.Identifier(identifier!!.literal, identifier.range),
                    parseExpressionList<Token.BracketClose>().bind()
                        .firstOrNull() ?: raise(ParserError.UnExpectedToken(currentToken)),
                )
            } else {
                AstNode.Identifier(identifier!!.literal, identifier.range)
            }

            expectNextToken<Token.Assign>().bind()

            nextToken().bind()
            val right = parseExpression(Precedence.LOWEST).bind()
            assignments.add(left to right)
            if (nextToken is Token.Comma) {
                nextToken().bind()
                nextToken().bind()
            } else break
        }
        requireEndOfLine().bind()
        AstNode.AssignStatement(assignments)
    }

    private fun prefixParseFn(token: Token): Either<DnclError, AstNode.Expression> = either {
        when (token) {
            is Token.Identifier -> {
                val identifier = (currentToken as? Token.Identifier)
                    ?: raise(ParserError.UnExpectedToken(currentToken))
                AstNode.Identifier(identifier.literal, identifier.range)
            }

            is Token.Japanese -> {
                val identifier = (currentToken as? Token.Japanese)
                    ?: raise(ParserError.UnExpectedToken(currentToken))
                AstNode.Identifier(identifier.literal, identifier.range)
            }

            is Token.Int -> {
                val int = (currentToken as? Token.Int)
                    ?: raise(ParserError.UnExpectedToken(currentToken))
                AstNode.IntLiteral(
                    int.literal.toIntOrNull()
                        ?: raise(ParserError.InvalidIntLiteral(int)), int.range
                )
            }

            is Token.Float -> {
                val float = (currentToken as? Token.Float)
                    ?: raise(ParserError.UnExpectedToken(currentToken))
                AstNode.FloatLiteral(
                    float.literal.toFloatOrNull()
                        ?: raise(ParserError.InvalidFloatLiteral(float)), float.range
                )
            }

            is Token.String -> {
                val string = (currentToken as? Token.String)
                    ?: return ParserError.UnExpectedToken(currentToken).left()
                AstNode.StringLiteral(string.literal, string.range)
            }

            is PrefixExpressionToken -> {
                val operator = currentToken as PrefixExpressionToken
                nextToken().bind()
                val right = parseExpression(Precedence.PREFIX).bind()
                AstNode.PrefixExpression(operator, right, operator.range.first..right.range.last)
            }

            is Token.ParenOpen -> {
                nextToken().bind()
                val expression = parseExpression(Precedence.LOWEST).bind()
                expectNextToken<Token.ParenClose>().bind()
                expression
            }

            is Token.BracketOpen -> {
                nextToken().bind()
                val exps = parseExpressionList<Token.BracketClose>().bind()
                val close = currentToken as Token.BracketClose
                AstNode.ArrayLiteral(exps, token.range.first..close.range.last)
            }

            is Token.LenticularOpen -> {
                expectNextToken<Token.Japanese>()
                val string = (currentToken as? Token.Japanese) ?: raise(
                    ParserError.UnExpectedToken(
                        currentToken
                    )
                )
                expectNextToken<Token.LenticularClose>().bind()
                AstNode.SystemLiteral(string.literal, token.range.first..currentToken.range.last)
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
                mutableListOf(parseExpression(Precedence.LOWEST).bind())
            while (currentToken is Token.Indent || currentToken is Token.NewLine) nextToken().bind()

            while (nextToken is Token.Comma) {

                nextToken().bind()
                while (currentToken is Token.Indent || currentToken is Token.NewLine) nextToken().bind()


                nextToken().bind()
                while (currentToken is Token.Indent || currentToken is Token.NewLine) nextToken().bind()


                list.add(parseExpression(Precedence.LOWEST).bind())

            }

            expectNextToken<T>().bind()
            return list.toList().right()
        }

    private fun infixParseFn(left: AstNode.Expression): Either<DnclError, AstNode.Expression> =
        either {
            return when (currentToken) {
                is InfixExpressionToken -> {
                    val operator = currentToken as InfixExpressionToken
                    val precedence = currentToken.precedence()
                    nextToken().bind()
                    val right = parseExpression(precedence).bind()
                    AstNode.InfixExpression(left, operator, right).right()
                }

                is Token.BracketOpen -> {
                    nextToken().bind()
                    val index = parseExpression(Precedence.LOWEST).bind()
                    expectNextToken<Token.BracketClose>().bind()
                    AstNode.IndexExpression(left, index).right()
                }

                is Token.ParenOpen -> {
                    nextToken().bind()
                    val args = parseExpressionList<Token.ParenClose>().bind()
                    AstNode.CallExpression(left, args).right()
                }


                is Token.While -> {
                    expectNextToken<Token.Colon>().bind()
                    val block = parseBlockStatement().bind()
                    AstNode.WhileExpression(left, block).right()
                }

                else -> ParserError.UnknownInfixOperator(currentToken).left()
            }
        }


    companion object {
        operator fun invoke(lexer: ILexer): Either<LexerError, Parser> = either {
            val parser = Parser(lexer)
            parser.currentToken = lexer.nextToken().bind()
            parser.nextToken = lexer.nextToken().bind()
            parser.aheadToken = lexer.nextToken().bind()
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