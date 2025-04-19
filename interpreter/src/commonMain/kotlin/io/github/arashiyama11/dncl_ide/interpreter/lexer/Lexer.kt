package io.github.arashiyama11.dncl_ide.interpreter.lexer

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import io.github.arashiyama11.dncl_ide.interpreter.model.LexerError
import io.github.arashiyama11.dncl_ide.interpreter.model.Token

class Lexer(private val input: String) : ILexer {
    private var position: Int = 0
    private var readPosition: Int = 0
    private var ch: Char = END_OF_FILE
    private var preToken: Token = Token.NewLine(0..0)
    private val whiteSpace = setOf(' ', '\t', '\r', '　')

    init {
        readChar()
    }

    override fun nextToken(): Either<LexerError, Token> {
        return either {
            val token = if (preToken is Token.NewLine && ch !in whiteSpace) {
                Token.Indent(0, position - 1..<position)
            } else when (ch) {
                '\n' -> {
                    do {
                        readChar()
                    } while (ch == '\n')
                    if (ch == END_OF_FILE) Token.EOF(position - 1..<position) else Token.NewLine(
                        position - 1..<position
                    )
                }

                in whiteSpace -> if (preToken is Token.NewLine) {
                    var depth = 0
                    do {
                        readChar()
                        depth++
                    } while (ch in whiteSpace)
                    Token.Indent(depth, position - depth..<position)
                } else {
                    do {
                        readChar()
                    } while (ch in whiteSpace)
                    nextToken().bind()
                }

                '「' -> readString('」').bind()
                '"' -> readString('"').bind()
                '(' -> {
                    readChar()
                    Token.ParenOpen(position - 1..<position)
                }

                ')' -> {
                    readChar()
                    Token.ParenClose(position - 1..<position)
                }

                '←' -> {
                    readChar()
                    Token.Assign(position - 1..<position)
                }

                '=' -> if (peekChar() == '=') {
                    readChar()
                    readChar()
                    Token.Equal(position - 2..<position)
                } else {
                    readChar()
                    Token.Assign(position - 1..<position)
                }

                '≠' -> {
                    readChar()
                    Token.NotEqual(position - 1..<position)
                }

                '＞', '>' -> {
                    readChar()
                    if (ch == '=') {
                        readChar()
                        Token.GreaterThanOrEqual(position - 2..<position)
                    } else Token.GreaterThan(position - 1..<position)
                }

                '≧' -> {
                    readChar()
                    Token.GreaterThanOrEqual(position - 1..<position)
                }

                '＜', '<' -> {
                    readChar()
                    if (ch == '=') {
                        readChar()
                        Token.LessThanOrEqual(position - 2..<position)
                    } else
                        Token.LessThan(position - 1..<position)
                }

                '≦' -> {
                    readChar()
                    Token.LessThanOrEqual(position - 1..<position)
                }

                '[' -> {
                    readChar()
                    Token.BracketOpen(position - 1..<position)
                }

                ']' -> {
                    readChar()
                    Token.BracketClose(position - 1..<position)
                }

                '{' -> {
                    readChar()
                    Token.BraceOpen(position - 1..<position)
                }

                '}' -> {
                    readChar()
                    Token.BraceClose(position - 1..<position)
                }

                '【' -> {
                    readChar()
                    Token.LenticularOpen(position - 1..<position)
                }

                '】' -> {
                    readChar()
                    Token.LenticularClose(position - 1..<position)
                }

                ',' -> {
                    readChar()
                    Token.Comma(position - 1..<position)
                }

                '+', '＋' -> {
                    readChar()
                    Token.Plus(position - 1..<position)
                }

                '-' -> {
                    readChar()
                    Token.Minus(position - 1..<position)
                }

                '*', '×' -> {
                    readChar()
                    Token.Times(position - 1..<position)
                }

                '/' -> if (peekChar() == '/') {
                    readChar()
                    readChar()
                    Token.DivideInt(position - 2..<position)
                } else {
                    readChar()
                    Token.Divide(position - 1..<position)
                }

                '÷' -> {
                    readChar()
                    Token.DivideInt(position - 1..<position)
                }

                '%' -> {
                    readChar()
                    Token.Modulo(position - 1..<position)
                }

                '!' -> if (peekChar() == '=') {
                    readChar()
                    readChar()
                    Token.NotEqual(position - 2..<position)
                } else {
                    readChar()
                    Token.Bang(position - 1..<position)
                }

                ':', '：' -> {
                    readChar()
                    Token.Colon(position - 1..<position)
                }

                '#' -> {
                    val start = position
                    do {
                        readChar()
                    } while (ch != '\n' && ch != END_OF_FILE)
                    Token.Comment(input.substring(start, position), start until position)
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
            if (ch == END_OF_FILE) break
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
            if (ch == END_OF_FILE) return break
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
            "関数" -> Token.Function(pos until position).right()
            "と定義する" -> Token.Define(pos until position).right()
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
            if (ch == END_OF_FILE) return LexerError.UnExpectedEOF(
                position,
                message = "文字列が閉じていません"
            ).left()
        } while (ch != end)
        readChar()
        return Token.String(input.substring(pos, position - 1), pos - 1 until position).right()
    }

    override fun iterator(): Iterator<Either<LexerError, Token>> =
        object : Iterator<Either<LexerError, Token>> {
            override fun hasNext(): Boolean = preToken !is Token.EOF

            override fun next(): Either<LexerError, Token> = nextToken()
        }

    companion object {
        const val END_OF_FILE = 0.toChar()

        private fun Char.isAlphaBet(): Boolean {
            return this.code in (65..90) + (97..122)
        }
    }
}