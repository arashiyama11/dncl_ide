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
    private lateinit var preToken: Token
    private lateinit var currentToken: Token
    private lateinit var nextToken: Token


    fun parseProgram(): Either<DnclError, AstNode.Program> {
        val statements = mutableListOf<AstNode.Statement>()
        while (currentToken != Token.EOF) {
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
                    Token.Assign -> parseAssignStatement()
                    Token.Wo -> parseForStatement()
                    Token.BracketOpen -> {
                        parseIndexAssignStatement().getOrNull()?.right()
                            ?: parseExpressionStatement()
                    }

                    else -> parseExpressionStatement()
                }
            } else {
                parseExpressionStatement()
            }

            is Token.Identifier -> when (nextToken) {
                Token.Assign -> parseAssignStatement()
                Token.Wo -> parseForStatement()
                Token.BracketOpen -> {
                    parseIndexAssignStatement().getOrNull()?.right() ?: parseExpressionStatement()
                }

                else -> parseExpressionStatement()
            }

            is Token.Japanese -> {
                parseExpressionStatement()
            }

            is Token.NewLine -> {
                nextToken().getOrElse { return it.left() }
                if (currentToken == Token.EOF) { //TODO 汚い
                    return AstNode.BlockStatement(emptyList()).right()
                }
                parseStatement()
            }


            else -> {
                parseExpressionStatement()
            }
        }
    }

    private fun parseExpression(precedence: Precedence): Either<DnclError, AstNode.Expression> {
        var left = prefixParseFn(currentToken).getOrElse { return it.left() }
        while ((nextToken !is ExpressionStopToken && precedence < nextToken.precedence())) {
            /*if (nextToken == Token.While) {
                nextToken().getOrElse { return it.left() }
                expectNextToken(Token.Colon)
                val block = parseBlockStatement().getOrElse { return it.left() }
                return AstNode.WhileExpression(
                    left, block
                ).right()
            }*/
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
            if (nextToken == Token.NewLine) {
                nextToken().getOrElse { return it.left() }
            }
            AstNode.ExpressionStatement(expression)
        }

    private fun nextToken(): Either<LexerError, Token> {
        preToken = currentToken
        currentToken = nextToken
        nextToken = lexer.nextToken().getOrElse { return it.left() }
        return currentToken.right()
    }

    private fun expectNextToken(token: Token): Either<DnclError, Unit> = either {
        nextToken()
        if (currentToken != token) {
            Exception("").printStackTrace()
            raise(ParserError("expectNextToken: expected $token, got $currentToken"))
        }
    }

    private inline fun <reified T> expectNextToken(): Either<DnclError, Unit> = either {
        nextToken()
        if (currentToken is T) {
            Exception("").printStackTrace()
            raise(ParserError("expectNextToken: expected ${T::class.simpleName}, got $currentToken"))
        }
    }

    private fun parseBlockStatement(): Either<DnclError, AstNode.BlockStatement> = either {
        val statements = mutableListOf<AstNode.Statement>()
        expectNextToken(Token.NewLine).getOrElse { return it.left() }
        nextToken()
        val startDepth = if (currentToken is Token.Indent) {
            (currentToken as Token.Indent).depth
        } else raise(ParserError("parseBlockStatement: expected Indent, got $currentToken"))
        nextToken().getOrElse { return it.left() }
        while (currentToken != Token.EOF) {
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
            if (currentToken == Token.EOF) {
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

        expectNextToken(Token.Then).getOrElse { return it.left() }
        expectNextToken(Token.Colon).getOrElse { return it.left() }

        val consequence = parseBlockStatement().getOrElse { return it.left() }

        while (nextToken == Token.NewLine || nextToken is Token.Indent) nextToken().getOrElse { return it.left() }

        if (nextToken == Token.Else) {
            expectNextToken(Token.Else).getOrElse { return it.left() }
            expectNextToken(Token.Colon).getOrElse { return it.left() }
            val alternative = parseBlockStatement().getOrElse { return it.left() }
            AstNode.IfStatement(condition, consequence, alternative)
        } else if (nextToken == Token.Elif) {
            expectNextToken(Token.Elif).getOrElse { return it.left() }
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
            ?: return ParserError("parseForStatement: expected Identifier, got $currentToken").left()
        expectNextToken(Token.Wo).getOrElse { return it.left() }
        nextToken().getOrElse { return it.left() }
        val start = parseExpression(Precedence.LOWEST).getOrElse { return it.left() }
        expectNextToken(Token.Kara).getOrElse { return it.left() }
        nextToken().getOrElse { return it.left() }
        val end = parseExpression(Precedence.LOWEST).getOrElse { return it.left() }
        expectNextToken(Token.Made).getOrElse { return it.left() }
        nextToken().getOrElse { return it.left() }
        val step = parseExpression(Precedence.LOWEST).getOrElse { return it.left() }
        nextToken().getOrElse { return it.left() }
        val type = when (currentToken) {
            Token.UpTo -> AstNode.ForStatement.Companion.StepType.INCREMENT
            Token.DownTo -> AstNode.ForStatement.Companion.StepType.DECREMENT
            else -> return ParserError("parseForStatement: expected 増やしながら繰り返す or 減らしながら繰り返す, got $currentToken").left()
        }
        expectNextToken(Token.Colon).getOrElse { return it.left() }
        val block = parseBlockStatement().getOrElse { return it.left() }
        AstNode.ForStatement(counter, start, end, step, type, block)
    }

    private fun parseAssignStatement(): Either<DnclError, AstNode.AssignStatement> = either {
        val name = (currentToken as? Token.Identifier)
            ?: return ParserError("parseAssignStatement: expected Identifier, got $currentToken").left()
        expectNextToken(Token.Assign).getOrElse { return it.left() }
        nextToken().getOrElse { return it.left() }
        val value = parseExpression(Precedence.LOWEST).getOrElse { return it.left() }
        AstNode.AssignStatement(name, value)
    }

    private fun parseIndexAssignStatement(): Either<DnclError, AstNode.IndexAssignStatement> =
        either {
            val name = (currentToken as? Token.Identifier)
                ?: return ParserError("parseAssignStatement: expected Identifier, got $currentToken").left()
            expectNextToken(Token.BracketOpen).getOrElse { return it.left() }
            nextToken().getOrElse { return it.left() }
            val index = parseExpression(Precedence.LOWEST).getOrElse { return it.left() }
            expectNextToken(Token.BracketClose).getOrElse { return it.left() }
            expectNextToken(Token.Assign).getOrElse { return it.left() }
            nextToken().getOrElse { return it.left() }
            val value = parseExpression(Precedence.LOWEST).getOrElse { return it.left() }
            AstNode.IndexAssignStatement(name, index, value)
        }

    private fun prefixParseFn(token: Token): Either<DnclError, AstNode.Expression> = either {
        when (token) {
            is Token.Identifier -> {
                val identifier = (currentToken as? Token.Identifier)
                    ?: return ParserError("parseIdentifier: expected Identifier, got $currentToken").left()
                AstNode.Identifier(identifier.literal)
            }

            is Token.Japanese -> {
                val identifier = (currentToken as? Token.Japanese)
                    ?: return ParserError("parseIdentifier: expected Identifier, got $currentToken").left()
                AstNode.Identifier(identifier.literal)
            }

            is Token.Int -> {
                val int = (currentToken as? Token.Int)
                    ?: return ParserError("parseIntLiteral: expected IntLiteral, got $currentToken").left()
                AstNode.IntLiteral(
                    int.literal.toIntOrNull()
                        ?: return ParserError("parseIntLiteral: invalid IntLiteral, got $currentToken").left()
                )
            }

            is Token.Float -> {
                val float = (currentToken as? Token.Float)
                    ?: return ParserError("parseFloatLiteral: expected FloatLiteral, got $currentToken").left()
                AstNode.FloatLiteral(
                    float.literal.toFloatOrNull()
                        ?: return ParserError("parseFloatLiteral: invalid FloatLiteral, got $currentToken").left()
                )
            }

            is Token.String -> {
                val string = (currentToken as? Token.String)
                    ?: return ParserError("parseStringLiteral: expected StringLiteral, got $currentToken").left()
                AstNode.StringLiteral(string.literal)
            }

            is Token.Bang, Token.Minus, Token.Plus -> {
                val operator = currentToken
                nextToken().getOrElse { return it.left() }
                val right = parseExpression(Precedence.PREFIX).getOrElse { return it.left() }
                AstNode.PrefixExpression(operator, right)
            }

            is Token.ParenOpen -> {
                nextToken().getOrElse { return it.left() }
                val expression = parseExpression(Precedence.LOWEST).getOrElse { return it.left() }
                expectNextToken(Token.ParenClose).getOrElse { return it.left() }
                if (currentToken != Token.ParenClose) {
                    return ParserError("prefixParseFn: aexpected ParenClose, got $currentToken").left()
                }
                expression
            }

            is Token.BracketOpen -> {
                nextToken().getOrElse { return it.left() }
                val exps = parseExpressionList(Token.BracketClose).getOrElse { return it.left() }
                AstNode.ArrayLiteral(exps)
            }

            else -> {
                Exception("").printStackTrace()
                raise(ParserError("prefixParseFn: no prefix parse function for $token"))
            }
        }
    }

    private fun parseExpressionList(end: Token): Either<DnclError, List<AstNode.Expression>> =
        either {

            if (currentToken == end) {
                return emptyList<AstNode.Expression>().right()
            }
            val list =
                mutableListOf(parseExpression(Precedence.LOWEST).getOrElse { return it.left() })
            while (currentToken is Token.Indent || currentToken == Token.NewLine) nextToken().getOrElse { return it.left() }

            while (nextToken == Token.Comma) {

                nextToken().getOrElse { return it.left() }
                while (currentToken is Token.Indent || currentToken == Token.NewLine) nextToken().getOrElse { return it.left() }


                nextToken().getOrElse { return it.left() }
                while (currentToken is Token.Indent || currentToken == Token.NewLine) nextToken().getOrElse { return it.left() }


                list.add(parseExpression(Precedence.LOWEST).getOrElse { return it.left() })

            }

            expectNextToken(end).getOrElse { return it.left() }
            return list.toList().right()
        }

    private fun infixParseFn(left: AstNode.Expression): Either<DnclError, AstNode.Expression> {
        return when (currentToken) {
            is Token.Plus, Token.Minus, Token.Times, Token.Divide, Token.DivideInt, Token.Modulo, Token.Equal, Token.NotEqual, Token.GreaterThan, Token.LessThan, Token.GreaterThanOrEqual, Token.LessThanOrEqual, Token.And, Token.Or -> {
                val operator = currentToken
                val precedence = currentToken.precedence()
                nextToken().getOrElse { return it.left() }
                val right = parseExpression(precedence).getOrElse { return it.left() }
                AstNode.InfixExpression(left, operator, right).right()
            }

            is Token.BracketOpen -> {
                nextToken().getOrElse { return it.left() }
                val index = parseExpression(Precedence.LOWEST).getOrElse { return it.left() }
                expectNextToken(Token.BracketClose).getOrElse { return it.left() }
                AstNode.IndexExpression(left, index).right()
            }

            is Token.ParenOpen -> {
                nextToken().getOrElse { return it.left() }
                val args = parseExpressionList(Token.ParenClose).getOrElse { return it.left() }
                AstNode.CallExpression(left, args).right()
            }

            is Token.While -> {
                expectNextToken(Token.Colon).getOrElse { return it.left() }
                val block = parseBlockStatement().getOrElse { return it.left() }
                AstNode.WhileExpression(left, block).right()
            }

            else -> ParserError("infixParseFn: no infix parse function for $currentToken").left()
        }
    }


    companion object {
        operator fun invoke(lexer: ILexer): Either<LexerError, Parser> {
            val parser = Parser(lexer)
            parser.preToken = Token.NewLine
            parser.currentToken = lexer.nextToken().getOrElse { return it.left() }
            parser.nextToken = lexer.nextToken().getOrElse { return it.left() }
            return parser.right()
        }

        fun Token.precedence() = when (this) {
            is Token.Plus, Token.Minus -> Precedence.SUM
            is Token.Times, Token.Divide, Token.DivideInt, Token.Modulo -> Precedence.PRODUCT
            is Token.GreaterThan, Token.LessThan, Token.GreaterThanOrEqual, Token.LessThanOrEqual -> Precedence.LESSGREATER
            is Token.Equal, Token.NotEqual -> Precedence.EQUALS
            is Token.Bang -> Precedence.PREFIX
            is Token.BracketOpen -> Precedence.INDEX
            is Token.ParenOpen -> Precedence.CALL
            is Token.While -> Precedence.WHILE
            is Token.And -> Precedence.AND
            is Token.Or -> Precedence.OR
            else -> {
                Precedence.LOWEST
            }
        }
    }
}