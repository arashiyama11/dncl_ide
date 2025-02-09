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
    private var preToken: Token = Token.NewLine

    init {
        readChar()
    }

    //andの扱いをどうするか
    override fun nextToken(): Either<LexerError, Token> {
        return either {
            val token = when (ch) {
                '\n' -> {
                    readChar()
                    Token.NewLine
                }

                ' ' -> if (preToken == Token.NewLine) {
                    var depth = 0
                    do {
                        readChar()
                        depth++
                    } while (ch == ' ')
                    Token.Indent(depth)
                } else {
                    do {
                        readChar()
                    } while (ch == ' ')
                    nextToken().getOrElse { return it.left() }
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

                '←' -> {
                    readChar()
                    Token.Assign
                }

                '=' -> if (peekChar() == '=') {
                    readChar()
                    readChar()
                    Token.Equal
                } else {
                    readChar()
                    Token.Assign
                }

                '≠' -> {
                    readChar()
                    Token.NotEqual
                }

                '＞', '>' -> {
                    readChar()
                    Token.GreaterThan
                }

                '≧' -> {
                    readChar()
                    Token.GreaterThanOrEqual
                }

                '＜', '<' -> {
                    readChar()
                    Token.LessThan
                }

                '≦' -> {
                    readChar()
                    Token.LessThanOrEqual
                }

                '[' -> {
                    readChar()
                    Token.BracketOpen
                }

                ']' -> {
                    readChar()
                    Token.BracketClose
                }

                '{' -> {
                    readChar()
                    Token.BraceOpen
                }

                '}' -> {
                    readChar()
                    Token.BraceClose
                }

                '【' -> {
                    readChar()
                    Token.LenticularOpen
                }

                '】' -> {
                    readChar()
                    Token.LenticularClose
                }

                ',' -> {
                    readChar()
                    Token.Comma
                }

                '+', '＋' -> {
                    readChar()
                    Token.Plus
                }

                '-' -> {
                    readChar()
                    Token.Minus
                }

                '*', '×' -> {
                    readChar()
                    Token.Times
                }

                '/' -> if (peekChar() == '/') {
                    readChar()
                    readChar()
                    Token.DivideInt
                } else {
                    readChar()
                    Token.Divide
                }

                '÷' -> {
                    readChar()
                    Token.Divide
                }

                '%' -> {
                    readChar()
                    Token.Modulo
                }

                '!' -> if (peekChar() == '=') {
                    readChar()
                    readChar()
                    Token.NotEqual
                } else {
                    readChar()
                    Token.Bang
                }

                ':', '：' -> {
                    readChar()
                    Token.Colon
                }

                '#' -> {
                    do {
                        readChar()
                    } while (ch != '\n')
                    nextToken().getOrElse { return it.left() }
                }

                END_OF_FILE -> Token.EOF
                else -> when {
                    ch.isDigit() -> readNumber().getOrElse { return it.left() }
                    ch.isLetter() -> if (ch.isAlphaBet()) readIdentifier().getOrElse { return it.left() } else readJapanese().getOrElse { return it.left() } //TODO 日本語はべつの処理が必要
                    else -> raise(LexerError.UnExpectedCharacter(ch))
                }
            }
            preToken = token
            token
        }
    }

    private fun readChar() {
        ch = if (readPosition >= input.length) {
            END_OF_FILE
        } else {
            input[readPosition]
        }
        position = readPosition
        readPosition++
    }

    private fun peekChar(): Char {
        return if (readPosition >= input.length) {
            END_OF_FILE
        } else {
            input[readPosition]
        }
    }

    private fun readIdentifier(): Either<LexerError, Token> {
        val pos = position
        do {
            readChar()
            if (ch == END_OF_FILE) return LexerError.UnExpectedEOF.left()
        } while (ch.isLetterOrDigit() || ch == '_')
        val literal = input.substring(pos, position)
        return Token.Identifier(literal).right()
    }

    private fun readJapanese(): Either<LexerError, Token> {
        val pos = position
        do {
            readChar()
            if (ch == END_OF_FILE) return LexerError.UnExpectedEOF.left()
        } while (ch.isLetterOrDigit() || ch == '_')
        val literal = input.substring(pos, position)
        return Token.Japanese(literal).right()
    }

    private fun readNumber(): Either<LexerError, Token> {
        val pos = position
        while (ch.isDigit()) {
            readChar()
            if (ch == END_OF_FILE) return LexerError.UnExpectedEOF.left()
        }
        return if (ch == '.') {
            readChar()
            while (ch.isDigit()) {
                readChar()
                if (ch == END_OF_FILE) return LexerError.UnExpectedEOF.left()
            }
            Token.Float(input.substring(pos, position)).right()
        } else Token.Int(input.substring(pos, position)).right()
    }

    private fun readString(end: Char): Either<LexerError, Token> {
        val pos = position + 1
        do {
            readChar()
            if (ch == END_OF_FILE) return LexerError.UnExpectedEOF.left()
        } while (ch != end)
        readChar()
        return Token.String(input.substring(pos, position - 1)).right()
    }

    override fun iterator(): Iterator<Either<LexerError, Token>> =
        object : Iterator<Either<LexerError, Token>> {
            override fun hasNext(): Boolean = preToken != Token.EOF

            override fun next(): Either<LexerError, Token> = nextToken()
        }

    companion object {
        const val END_OF_FILE = 0.toChar()

        private fun Char.isAlphaBet(): Boolean {
            return this.code in (65..90) + (97..122) || this == '_'
        }
    }
}