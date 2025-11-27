package io.github.arashiyama11.dncl_ide.interpreter.lexer

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import io.github.arashiyama11.dncl_ide.interpreter.model.LexerError
import io.github.arashiyama11.dncl_ide.interpreter.model.Token

class Lexer(private val input: String, private val filePath: String? = null) : ILexer {
    private var position: Int = 0
    private var readPosition: Int = 0
    private var ch: Char = END_OF_FILE
    private var preToken: Token = Token.NewLine(0..0, filePath = filePath)
    private val whiteSpace = setOf(' ', '\t', '\r', '　')

    init {
        readChar()
    }

    override fun nextToken(): Either<LexerError, Token> {
        return either {
            if (preToken is Token.NewLine && ch !in whiteSpace) {
                Token.Indent(0, position - 1..<position, filePath = filePath)
            } else when (ch) {
                '\n' -> {
                    do {
                        readChar()
                    } while (ch == '\n')
                    if (ch == END_OF_FILE) Token.EOF(
                        position - 1..<position,
                        filePath = filePath
                    ) else Token.NewLine(
                        position - 1..<position
                        ,
                        filePath = filePath
                    )
                }

                in whiteSpace -> if (preToken is Token.NewLine) {
                    var depth = 0
                    do {
                        readChar()
                        depth++
                    } while (ch in whiteSpace)
                    Token.Indent(depth, position - depth..<position, filePath = filePath)
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
                    Token.ParenOpen(position - 1..<position, filePath = filePath)
                }

                ')' -> {
                    readChar()
                    Token.ParenClose(position - 1..<position, filePath = filePath)
                }

                '←' -> {
                    readChar()
                    Token.Assign(position - 1..<position, filePath = filePath)
                }

                '=' -> if (peekChar() == '=') {
                    readChar()
                    readChar()
                    Token.Equal(position - 2..<position, filePath = filePath)
                } else {
                    readChar()
                    Token.Assign(position - 1..<position, filePath = filePath)
                }

                '≠' -> {
                    readChar()
                    Token.NotEqual(position - 1..<position, filePath = filePath)
                }

                '＞', '>' -> {
                    readChar()
                    if (ch == '>') {
                        readChar()
                        Token.ShiftRight(position - 2..<position, filePath = filePath)
                    } else if (ch == '=') {
                        readChar()
                        Token.GreaterThanOrEqual(position - 2..<position, filePath = filePath)
                    } else Token.GreaterThan(position - 1..<position, filePath = filePath)
                }

                '≧' -> {
                    readChar()
                    Token.GreaterThanOrEqual(position - 1..<position, filePath = filePath)
                }

                '＜', '<' -> {
                    readChar()
                    if (ch == '<') {
                        readChar()
                        Token.ShiftLeft(position - 2..<position, filePath = filePath)
                    } else if (ch == '=') {
                        readChar()
                        Token.LessThanOrEqual(position - 2..<position, filePath = filePath)
                    } else
                        Token.LessThan(position - 1..<position, filePath = filePath)
                }

                '≦' -> {
                    readChar()
                    Token.LessThanOrEqual(position - 1..<position, filePath = filePath)
                }

                '[' -> {
                    readChar()
                    Token.BracketOpen(position - 1..<position, filePath = filePath)
                }

                ']' -> {
                    readChar()
                    Token.BracketClose(position - 1..<position, filePath = filePath)
                }

                '{' -> {
                    readChar()
                    Token.BraceOpen(position - 1..<position, filePath = filePath)
                }

                '}' -> {
                    readChar()
                    Token.BraceClose(position - 1..<position, filePath = filePath)
                }

                '【' -> {
                    readChar()
                    Token.LenticularOpen(position - 1..<position, filePath = filePath)
                }

                '】' -> {
                    readChar()
                    Token.LenticularClose(position - 1..<position, filePath = filePath)
                }

                ',' -> {
                    readChar()
                    Token.Comma(position - 1..<position, filePath = filePath)
                }

                '+', '＋' -> {
                    readChar()
                    Token.Plus(position - 1..<position, filePath = filePath)
                }

                '-' -> {
                    readChar()
                    Token.Minus(position - 1..<position, filePath = filePath)
                }

                '*', '×' -> {
                    readChar()
                    Token.Times(position - 1..<position, filePath = filePath)
                }

                '/' -> if (peekChar() == '/') {
                    readChar()
                    readChar()
                    Token.DivideInt(position - 2..<position, filePath = filePath)
                } else {
                    readChar()
                    Token.Divide(position - 1..<position, filePath = filePath)
                }

                '÷' -> {
                    readChar()
                    Token.DivideInt(position - 1..<position, filePath = filePath)
                }

                '%' -> {
                    readChar()
                    Token.Modulo(position - 1..<position, filePath = filePath)
                }

                '&' -> {
                    readChar()
                    Token.BitAnd(position - 1..<position, filePath = filePath)
                }

                '|' -> {
                    readChar()
                    Token.BitOr(position - 1..<position, filePath = filePath)
                }

                '^' -> {
                    readChar()
                    Token.BitXor(position - 1..<position, filePath = filePath)
                }

                '~' -> {
                    readChar()
                    Token.BitNot(position - 1..<position, filePath = filePath)
                }

                '!' -> if (peekChar() == '=') {
                    readChar()
                    readChar()
                    Token.NotEqual(position - 2..<position, filePath = filePath)
                } else {
                    readChar()
                    Token.Bang(position - 1..<position, filePath = filePath)
                }

                ':', '：' -> {
                    readChar()
                    Token.Colon(position - 1..<position, filePath = filePath)
                }

                '#' -> {
                    val start = position
                    do {
                        readChar()
                    } while (ch != '\n' && ch != END_OF_FILE)
                    Token.Comment(input.substring(start, position), start until position, filePath = filePath)
                }

                END_OF_FILE -> Token.EOF(position..position, filePath = filePath)

                '@' -> {
                    val start = position
                    do {
                        readChar()
                    } while (ch != ' ' && ch != '\n' && ch != END_OF_FILE)
                    Token.AtMark(input.substring(start, position), start until position, filePath = filePath)
                }

                else -> when {
                    ch.isDigit() -> readNumber().bind()
                    ch.isLetter() || ch == '_' -> if (ch.isAlphaBet()) readIdentifier().bind() else readJapanese().bind()
                    else -> raise(LexerError.UnExpectedCharacter(ch, position))
                }
            }
        }
            .onLeft { preToken = Token.EOF(it.index..it.index, filePath = filePath) }
            .onRight { preToken = it }
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
            "and" -> Token.And(pos until position, filePath = filePath).right()
            "or" -> Token.Or(pos until position, filePath = filePath).right()
            "true" -> Token.Boolean(true, pos until position, filePath = filePath).right()
            "false" -> Token.Boolean(false, pos until position, filePath = filePath).right()
            else -> Token.Identifier(literal, pos until position, filePath = filePath).right()
        }
    }

    private fun readJapanese(): Either<LexerError, Token> {
        val pos = position
        do {
            readChar()
            if (ch == END_OF_FILE) return break
        } while (ch.isLetterOrDigit() || ch == '_')
        return when (val literal = input.substring(pos, position)) {
            "もし" -> Token.If(pos until position, filePath = filePath).right()
            "ならば" -> Token.Then(pos until position, filePath = filePath).right()
            "そうでなくもし" -> Token.Elif(pos until position, filePath = filePath).right()
            "そうでなければ" -> Token.Else(pos until position, filePath = filePath).right()
            "を" -> Token.Wo(pos until position, filePath = filePath).right()
            "から" -> Token.Kara(pos until position, filePath = filePath).right()
            "まで" -> Token.Made(pos until position, filePath = filePath).right()
            "の間繰り返す" -> Token.While(pos until position, filePath = filePath).right()
            "ずつ増やしながら繰り返す", "ずつ増やしながら" -> Token.UpTo(pos until position, filePath = filePath).right()
            "ずつ減らしながら繰り返す", "ずつ減らしながら" -> Token.DownTo(pos until position, filePath = filePath)
                .right()

            "かつ" -> Token.And(pos until position, filePath = filePath).right()
            "または" -> Token.Or(pos until position, filePath = filePath).right()
            "関数" -> Token.Function(pos until position, filePath = filePath).right()
            "と定義する" -> Token.Define(pos until position, filePath = filePath).right()
            "真" -> Token.Boolean(true, pos until position, filePath = filePath).right()
            "偽" -> Token.Boolean(false, pos until position, filePath = filePath).right()
            else -> Token.Japanese(literal, pos until position, filePath = filePath).right()
        }
    }

    private fun readNumber(): Either<LexerError, Token> {
        val pos = position

        // プレフィックス付き整数: 0b / 0o / 0x
        if (ch == '0' && peekChar().let { it == 'b' || it == 'B' || it == 'o' || it == 'O' || it == 'x' || it == 'X' }) {
            val prefixChar = peekChar()
            readChar() // consume '0'
            readChar() // consume prefix
            val radix = when (prefixChar) {
                'b', 'B' -> 2
                'o', 'O' -> 8
                else -> 16
            }
            while (ch.isValidDigit(radix) && ch != END_OF_FILE) {
                readChar()
            }
            return Token.Int(
                input.substring(pos, position),
                pos until position,
                filePath = filePath
            ).right()
        }

        // 10進整数/浮動小数
        while (ch.isDigit() && ch != END_OF_FILE) {
            readChar()
        }
        return if (ch == '.') {
            readChar()
            while (ch.isDigit() && ch != END_OF_FILE) {
                readChar()
            }
            Token.Float(
                input.substring(pos, position),
                pos until position,
                filePath = filePath
            ).right()
        } else Token.Int(
            input.substring(pos, position),
            pos until position,
            filePath = filePath
        ).right()
    }

    private fun readString(end: Char): Either<LexerError, Token> {
        val pos = position + 1
        val stringBuilder = StringBuilder()
        var inEscape = false
        do {
            readChar()
            if (ch == END_OF_FILE) return LexerError.UnExpectedEOF(
                pos - 1,
                message = "文字列が閉じていません"
            ).left()

            if (inEscape) {
                when (ch) {
                    'n' -> stringBuilder.append('\n')
                    't' -> stringBuilder.append('\t')
                    'r' -> stringBuilder.append('\r')
                    '\\' -> stringBuilder.append('\\')
                    '"' -> stringBuilder.append('"')
                    '「' -> stringBuilder.append('「')
                    '」' -> stringBuilder.append('」')
                    // 他のエスケープシーケンスも必要に応じて追加
                    else -> stringBuilder.append(ch) // 不明なエスケープシーケンスはそのまま追加
                }
                inEscape = false
            } else if (ch == '\\') {
                inEscape = true
            } else if (ch != end) {
                stringBuilder.append(ch)
            }
        } while (ch != end || inEscape) // エスケープシーケンスの途中で終わらないように修正
        readChar()
        return Token.String(
            stringBuilder.toString(),
            pos - 1 until position,
            filePath = filePath
        ).right()
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

        private fun Char.isValidDigit(radix: Int): Boolean {
            return when (radix) {
                2 -> this == '0' || this == '1'
                8 -> this in '0'..'7'
                10 -> this.isDigit()
                16 -> this.isDigit() || this in 'a'..'f' || this in 'A'..'F'
                else -> false
            }
        }
    }
}
