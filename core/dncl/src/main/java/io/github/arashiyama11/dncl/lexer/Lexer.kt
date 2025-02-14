package io.github.arashiyama11.dncl.lexer

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import io.github.arashiyama11.dncl.model.LexerError
import io.github.arashiyama11.dncl.model.Token

class Lexer(private val input: String) : ILexer {
    private var position: Int = 0
    private var readPosition: Int = 0
    private var ch: Char = END_OF_FILE
    private var preToken: Token = Token.NewLine(0..0)

    init {
        readChar()
    }

    override fun nextToken(): Either<LexerError, Token> {
        return either {
            val token = if (preToken is Token.NewLine && ch != ' ') {
                Token.Indent(0, position..position)
            } else when (ch) {
                '\n' -> {
                    do {
                        readChar()
                    } while (ch == '\n')
                    if (ch == END_OF_FILE) Token.EOF(position..position) else Token.NewLine(position..position)
                }

                ' ' -> if (preToken is Token.NewLine) {
                    var depth = 0
                    do {
                        readChar()
                        depth++
                    } while (ch == ' ')
                    Token.Indent(depth, position - depth..position)
                } else {
                    do {
                        readChar()
                    } while (ch == ' ')
                    nextToken().bind()
                }

                '「' -> readString('」').bind()
                '"' -> readString('"').bind()
                '(' -> {
                    readChar()
                    Token.ParenOpen(position..position)
                }

                ')' -> {
                    readChar()
                    Token.ParenClose(position..position)
                }

                '←' -> {
                    readChar()
                    Token.Assign(position..position)
                }

                '=' -> if (peekChar() == '=') {
                    readChar()
                    readChar()
                    Token.Equal(position..position)
                } else {
                    readChar()
                    Token.Assign(position - 1..position)
                }

                '≠' -> {
                    readChar()
                    Token.NotEqual(position..position)
                }

                '＞', '>' -> {
                    readChar()
                    if (ch == '=') {
                        readChar()
                        Token.GreaterThanOrEqual(position - 1..position)
                    } else Token.GreaterThan(position - 1..position)
                }

                '≧' -> {
                    readChar()
                    Token.GreaterThanOrEqual(position..position)
                }

                '＜', '<' -> {
                    readChar()
                    if (ch == '=') {
                        readChar()
                        Token.LessThanOrEqual(position - 1..position)
                    } else
                        Token.LessThan(position..position)
                }

                '≦' -> {
                    readChar()
                    Token.LessThanOrEqual(position..position)
                }

                '[' -> {
                    readChar()
                    Token.BracketOpen(position..position)
                }

                ']' -> {
                    readChar()
                    Token.BracketClose(position..position)
                }

                '{' -> {
                    readChar()
                    Token.BraceOpen(position..position)
                }

                '}' -> {
                    readChar()
                    Token.BraceClose(position..position)
                }

                '【' -> {
                    readChar()
                    Token.LenticularOpen(position..position)
                }

                '】' -> {
                    readChar()
                    Token.LenticularClose(position..position)
                }

                ',' -> {
                    readChar()
                    Token.Comma(position..position)
                }

                '+', '＋' -> {
                    readChar()
                    Token.Plus(position..position)
                }

                '-' -> {
                    readChar()
                    Token.Minus(position..position)
                }

                '*', '×' -> {
                    readChar()
                    Token.Times(position..position)
                }

                '/' -> if (peekChar() == '/') {
                    readChar()
                    readChar()
                    Token.DivideInt(position - 1..position)
                } else {
                    readChar()
                    Token.Divide(position..position)
                }

                '÷' -> {
                    readChar()
                    Token.DivideInt(position..position)
                }

                '%' -> {
                    readChar()
                    Token.Modulo(position..position)
                }

                '!' -> if (peekChar() == '=') {
                    readChar()
                    readChar()
                    Token.NotEqual(position - 1..position)
                } else {
                    readChar()
                    Token.Bang(position..position)
                }

                ':', '：' -> {
                    readChar()
                    Token.Colon(position..position)
                }

                '#' -> {
                    do {
                        readChar()
                    } while (ch != '\n' && ch != END_OF_FILE)
                    nextToken().bind()
                }

                END_OF_FILE -> Token.EOF(position..position)
                else -> when {
                    ch.isDigit() -> readNumber().bind()
                    ch.isLetter() -> if (ch.isAlphaBet()) readIdentifier().bind() else readJapanese().bind()
                    else -> raise(LexerError.UnExpectedCharacter(ch, position))
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
            if (ch == END_OF_FILE) return LexerError.UnExpectedEOF(position).left()
        } while (ch.isLetterOrDigit() || ch == '_')
        return when (val literal = input.substring(pos, position)) {
            "and" -> Token.And(pos until position).right()
            "or" -> Token.Or(pos until position).right()
            else -> Token.Identifier(literal, pos until position).right()
        }
    }

    private fun readJapanese(): Either<LexerError, Token> {
        val pos = position
        do {
            readChar()
            if (ch == END_OF_FILE) return LexerError.UnExpectedEOF(position).left()
        } while (ch.isLetterOrDigit() || ch == '_')
        return when (val literal = input.substring(pos, position)) {
            "もし" -> Token.If(pos until position).right()
            "ならば" -> Token.Then(pos until position).right()
            "そうでなくもし" -> Token.Elif(pos until position).right()
            "そうでなければ" -> Token.Else(pos until position).right()
            "を" -> Token.Wo(pos until position).right()
            "から" -> Token.Kara(pos until position).right()
            "まで" -> Token.Made(pos until position).right()
            "の間繰り返す" -> Token.While(pos until position).right()
            "ずつ増やしながら繰り返す", "ずつ増やしながら" -> Token.UpTo(pos until position).right()
            "ずつ減らしながら繰り返す", "ずつ減らしながら" -> Token.DownTo(pos until position)
                .right()

            "かつ" -> Token.And(pos until position).right()
            "または" -> Token.Or(pos until position).right()
            else -> Token.Japanese(literal, pos until position).right()
        }
    }

    private fun readNumber(): Either<LexerError, Token> {
        val pos = position
        while (ch.isDigit() && ch != END_OF_FILE) {
            readChar()
        }
        return if (ch == '.') {
            readChar()
            while (ch.isDigit() && ch != END_OF_FILE) {
                readChar()
            }
            Token.Float(input.substring(pos, position), pos until position).right()
        } else Token.Int(input.substring(pos, position), pos until position).right()
    }

    private fun readString(end: Char): Either<LexerError, Token> {
        val pos = position + 1
        do {
            readChar()
            if (ch == END_OF_FILE) return LexerError.UnExpectedEOF(position).left()
        } while (ch != end)
        readChar()
        return Token.String(input.substring(pos, position - 1), pos until position).right()
    }

    override fun iterator(): Iterator<Either<LexerError, Token>> =
        object : Iterator<Either<LexerError, Token>> {
            override fun hasNext(): Boolean = preToken !is Token.EOF

            override fun next(): Either<LexerError, Token> = nextToken()
        }

    companion object {
        const val END_OF_FILE = 0.toChar()

        private fun Char.isAlphaBet(): Boolean {
            return this.code in (65..90) + (97..122) || this == '_'
        }
    }
}