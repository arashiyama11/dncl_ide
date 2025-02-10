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
            val statement = parseStatement().getOrElse { return it.left() }
            statements.add(statement)
            nextToken().getOrElse { return it.left() }
        }
        return AstNode.Program(statements).right()
    }

    private fun parseStatement(): Either<DnclError, AstNode.Statement> {
        return when (currentToken) {
            is Token.If -> parseIfStatement()

            is Token.Assign -> {
                TODO()
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
        while (nextToken != Token.NewLine && nextToken != Token.EOF && precedence < nextToken.precedence()) {
            nextToken().getOrElse { return it.left() }
            left = infixParseFn(left).getOrElse { return it.left() }
        }
        return left.right()
    }


    private fun parseExpressionStatement(): Either<DnclError, AstNode.ExpressionStatement> =
        either {
            val expression = parseExpression(Precedence.LOWEST).getOrElse { return it.left() }
            if (nextToken != Token.EOF && nextToken != Token.NewLine) {
                expectNextToken(Token.NewLine).getOrElse { return it.left() }
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
            raise(ParserError("expectNextToken: expected $token, got $currentToken"))
        }
    }

    private fun parseBlockStatement(): Either<DnclError, AstNode.BlockStatement> = either {
        val statements = mutableListOf<AstNode.Statement>()
        nextToken()
        while (currentToken != Token.EOF && currentToken != Token.EOF) {
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

        if (nextToken == Token.Else) {
            expectNextToken(Token.Else).getOrElse { return it.left() }
            expectNextToken(Token.Colon).getOrElse { return it.left() }
            val alternative = parseBlockStatement().getOrElse { return it.left() }
            AstNode.IfStatement(condition, consequence, alternative)
        } else {
            AstNode.IfStatement(condition, consequence, null)
        }
    }

    private fun parseAssignStatement(): Either<DnclError, AstNode.AssignStatement> = either {
        val name = (preToken as? Token.Identifier)
            ?: return ParserError("parseAssignStatement: expected Identifier, got $preToken").left()
        nextToken()
        val value = parseExpression(Precedence.LOWEST).getOrElse { return it.left() }
        AstNode.AssignStatement(name, value)
    }

    private fun prefixParseFn(token: Token): Either<DnclError, AstNode.Expression> = either {
        when (token) {
            is Token.Identifier -> {
                val identifier = (currentToken as? Token.Identifier)
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

            is Token.Bang, Token.Minus, Token.Plus -> {
                val operator = currentToken
                nextToken().getOrElse { return it.left() }
                val right = parseExpression(Precedence.PREFIX).getOrElse { return it.left() }
                AstNode.PrefixExpression(operator, right)
            }

            is Token.ParenOpen -> {
                nextToken().getOrElse { return it.left() }
                val expression = parseExpression(Precedence.LOWEST).getOrElse { return it.left() }
                println("$preToken $currentToken $nextToken")
                expectNextToken(Token.ParenClose).getOrElse { return it.left() }
                println("$preToken $currentToken $nextToken")
                if (currentToken != Token.ParenClose) {
                    return ParserError("prefixParseFn: aexpected ParenClose, got $currentToken").left()
                }
                expression
            }

            else -> raise(ParserError("prefixParseFn: no prefix parse function for $token"))
        }
    }

    private fun infixParseFn(left: AstNode.Expression): Either<DnclError, AstNode.Expression> {
        return when (currentToken) {
            is Token.Plus, Token.Minus, Token.Times, Token.Divide, Token.DivideInt, Token.Modulo, Token.Equal, Token.NotEqual, Token.GreaterThan, Token.LessThan, Token.GreaterThanOrEqual, Token.LessThanOrEqual -> {
                val operator = currentToken
                val precedence = currentToken.precedence()
                nextToken().getOrElse { return it.left() }
                val right = parseExpression(precedence).getOrElse { return it.left() }
                AstNode.InfixExpression(left, operator, right).right()
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
            is Token.ParenOpen -> Precedence.CALL
            is Token.EOF -> Precedence.LOWEST
            else -> {
                println("precedence: $this")
                Precedence.LOWEST
            }
        }
    }
}