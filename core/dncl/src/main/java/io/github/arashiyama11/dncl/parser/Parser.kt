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
import io.github.arashiyama11.dncl.model.ParserError
import io.github.arashiyama11.dncl.model.Precedence
import io.github.arashiyama11.dncl.model.Token

class Parser private constructor(private val lexer: ILexer) : IParser {
    private lateinit var currentToken: Token
    private lateinit var nextToken: Token


    override fun parseProgram(): Either<DnclError, AstNode.Program> {
        val statements = mutableListOf<AstNode.Statement>()
        while (currentToken !is Token.EOF) {
            val statement = parseStatement().getOrElse {
                return it.left()
            }
            statements.add(statement)
            nextToken().getOrElse { return it.left() }
        }
        return AstNode.Program(statements).right()
    }

    private fun parseStatement(): Either<DnclError, AstNode.Statement> {
        return when (currentToken) {
            is Token.If -> parseIfStatement()

            is Token.Comma -> if (nextToken is Token.Identifier) {
                nextToken().getOrElse { return it.left() }
                when (nextToken) {
                    is Token.Assign -> parseAssignStatement()
                    is Token.Wo -> parseForStatement()
                    is Token.BracketOpen -> {
                        parseIndexAssignStatement().getOrNull()?.right()
                            ?: parseExpressionStatement()
                    }

                    else -> parseExpressionStatement()
                }
            } else {
                parseExpressionStatement()
            }

            is Token.Identifier -> when (nextToken) {
                is Token.Assign -> parseAssignStatement()
                is Token.Wo -> parseForStatement()
                is Token.BracketOpen -> {
                    parseIndexAssignStatement().getOrNull()?.right() ?: parseExpressionStatement()
                }

                else -> parseExpressionStatement()
            }

            is Token.Japanese -> {
                parseExpressionStatement()
            }

            is Token.NewLine -> {
                nextToken().getOrElse { return it.left() }
                if (currentToken is Token.EOF) { //TODO 汚い
                    return AstNode.BlockStatement(emptyList()).right()
                }
                parseStatement()
            }

            is Token.Indent -> {
                return ParserError.UnexpectedIndent(currentToken).left()
            }


            else -> {
                parseExpressionStatement()
            }
        }
    }

    private fun parseExpression(precedence: Precedence): Either<DnclError, AstNode.Expression> {
        var left = prefixParseFn(currentToken).getOrElse { return it.left() }
        while ((nextToken !is ExpressionStopToken && precedence < nextToken.precedence())) {
            nextToken().getOrElse { return it.left() }
            left = infixParseFn(left).getOrElse { return it.left() }
        }
        return left.right()
    }


    private fun parseExpressionStatement(): Either<DnclError, AstNode.ExpressionStatement> =
        either {
            val expression = parseExpression(Precedence.LOWEST).getOrElse { return it.left() }
            /*if (nextToken != Token.EOF && nextToken != Token.NewLine) {
                expectNextToken(Token.NewLine).getOrElse { return it.left() }
            }*/
            if (nextToken is Token.NewLine) {
                nextToken().getOrElse { return it.left() }
            }
            AstNode.ExpressionStatement(expression)
        }

    private fun nextToken(): Either<LexerError, Token> {
        currentToken = nextToken
        nextToken = lexer.nextToken().getOrElse { return it.left() }
        return currentToken.right()
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

    private fun parseBlockStatement(): Either<DnclError, AstNode.BlockStatement> = either {
        val statements = mutableListOf<AstNode.Statement>()
        expectNextToken<Token.NewLine>().getOrElse { return it.left() }
        nextToken()
        val startDepth = if (currentToken is Token.Indent) {
            (currentToken as Token.Indent).depth
        } else raise(ParserError.UnExpectedToken(currentToken))
        nextToken().getOrElse { return it.left() }
        while (currentToken !is Token.EOF) {
            if (currentToken is Token.NewLine) { //TODO 汚い
                val depth = (nextToken as? Token.Indent)?.depth ?: 0
                if (depth < startDepth) {
                    break
                } else {
                    nextToken().getOrElse { return it.left() }
                    nextToken().getOrElse { return it.left() }
                }
            } else if (currentToken is Token.Indent) {
                val depth = (currentToken as Token.Indent).depth
                if (depth < startDepth) {
                    break
                } else {
                    nextToken().getOrElse { return it.left() }
                }
            }
            if (currentToken is Token.EOF) {
                break
            }
            val statement = parseStatement().getOrElse { return it.left() }
            statements.add(statement)
            nextToken().getOrElse { return it.left() }
        }
        return AstNode.BlockStatement(statements).right()
    }

    private fun parseIfStatement(): Either<DnclError, AstNode.Statement> = either {
        nextToken()
        val condition = parseExpression(Precedence.LOWEST).getOrElse { return it.left() }

        expectNextToken<Token.Then>().getOrElse { return it.left() }
        expectNextToken<Token.Colon>().getOrElse { return it.left() }

        val consequence = parseBlockStatement().getOrElse { return it.left() }

        while (nextToken is Token.NewLine || nextToken is Token.Indent) nextToken().getOrElse { return it.left() }

        if (nextToken is Token.Else) {
            expectNextToken<Token.Else>().getOrElse { return it.left() }
            expectNextToken<Token.Colon>().getOrElse { return it.left() }
            val alternative = parseBlockStatement().getOrElse { return it.left() }
            AstNode.IfStatement(condition, consequence, alternative)
        } else if (nextToken is Token.Elif) {
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

    private fun parseAssignStatement(): Either<DnclError, AstNode.AssignStatement> = either {
        val name = (currentToken as? Token.Identifier)
            ?: raise(ParserError.UnExpectedToken(currentToken))
        expectNextToken<Token.Assign>().getOrElse { return it.left() }
        nextToken().getOrElse { return it.left() }
        val value = parseExpression(Precedence.LOWEST).getOrElse { return it.left() }
        AstNode.AssignStatement(name, value)
    }

    private fun parseIndexAssignStatement(): Either<DnclError, AstNode.IndexAssignStatement> =
        either {
            val name = (currentToken as? Token.Identifier)
                ?: raise(ParserError.UnExpectedToken(currentToken))
            expectNextToken<Token.BracketOpen>().getOrElse { return it.left() }
            nextToken().getOrElse { return it.left() }
            val index = parseExpression(Precedence.LOWEST).getOrElse { return it.left() }
            expectNextToken<Token.BracketClose>().getOrElse { return it.left() }
            expectNextToken<Token.Assign>().getOrElse { return it.left() }
            nextToken().getOrElse { return it.left() }
            val value = parseExpression(Precedence.LOWEST).getOrElse { return it.left() }
            AstNode.IndexAssignStatement(name, index, value)
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
                if (currentToken !is Token.ParenClose) {
                    raise(
                        ParserError.UnExpectedToken(
                            currentToken,
                            expectedToken = Token.ParenClose::class.java.simpleName
                        )
                    )
                }
                expression
            }

            is Token.BracketOpen -> {
                nextToken().getOrElse { return it.left() }
                val exps = parseExpressionList<Token.BracketClose>().getOrElse { return it.left() }
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