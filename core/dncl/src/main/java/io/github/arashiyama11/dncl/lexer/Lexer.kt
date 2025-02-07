package io.github.arashiyama11.dncl.lexer

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import io.github.arashiyama11.dncl.model.LexerError
import io.github.arashiyama11.dncl.model.Token

class Lexer(private val input: String) : ILexer {
    private var position: Int = 0
    private var readPosition: Int = 0
    private var ch: Char = END_OF_FILE

    init {
        readChar()
    }

    override fun nextToken(): Either<LexerError, Token> {
        return either {
            val token = when (ch) {
                '\n' -> {
                    readChar()
                    Token.NewLine
                }

                ' ' -> {
                    readChar()
                    Token.Space
                }

                '「' -> readString('」').getOrElse { return it.left() }
                '"' -> readString('"').getOrElse { return it.left() }
                '(' -> {
                    readChar()
                    Token.ParenOpen
                }

                ')' -> {
                    readChar()
                    Token.ParenClose
                }
                /*'=' -> Token.EQ
                '≠' -> Token.NOT_EQ
                '+' -> Token.PLUS
                '-' -> Token.MINUS
                '*' -> Token.ASTERISK
                '/' -> Token.SLASH*/

                END_OF_FILE -> Token.EOF
                else -> when {
                    ch.isDigit() -> readNumber()
                    ch.isLetter() -> readIdentifier()

                    else -> raise(LexerError("Unknown character: $ch"))
                }
            }
            token
        }
    }

    override fun readChar() {
        ch = if (readPosition >= input.length) {
            END_OF_FILE
        } else {
            input[readPosition]
        }
        position = readPosition
        readPosition++
    }

    override fun peekChar(): Char {
        return if (readPosition >= input.length) {
            END_OF_FILE
        } else {
            input[readPosition]
        }
    }

    private fun readIdentifier(): Token {
        val pos = position
        do {
            readChar()
        } while (ch.isLetterOrDigit())
        val literal = input.substring(pos, position)
        return Token.Identifier(literal)
    }

    private fun readNumber(): Token {
        val pos = position
        while (ch.isDigit()) {
            readChar()
        }
        return if (ch == '.') {
            readChar()
            while (ch.isDigit()) {
                readChar()
            }
            Token.Float(input.substring(pos, position))
        } else Token.Int(input.substring(pos, position))
    }

    private fun readString(end: Char): Either<LexerError, Token> {
        val pos = position + 1
        do {
            readChar()
        } while (ch != end && ch != END_OF_FILE)

        return if (ch == END_OF_FILE) LexerError("Unterminated string").left() else {
            readChar()
            Token.String(input.substring(pos, position - 1)).right()
        }
    }

    companion object {
        const val END_OF_FILE = 0.toChar()
    }

}