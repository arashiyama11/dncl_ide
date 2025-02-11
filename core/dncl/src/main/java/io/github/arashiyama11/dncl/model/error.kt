package io.github.arashiyama11.dncl.model

import kotlin.math.max

sealed interface DnclError {
    val message: String?
    fun explain(program: String): String
}

private fun isHalfWidth(char: Char): Boolean {
    val code = char.code
    return (code in 0x0020..0x007E) || (code in 0xFF61..0xFF9F)
}


sealed class LexerError private constructor(override val message: String, open val index: Int) :
    DnclError {
    override fun explain(program: String): String {
        val programLines = program.split("\n")
        val (column, line, spaces) = run {
            var idx = 0
            for ((i, line) in programLines.withIndex()) {
                if (idx + line.length < index) {
                    idx += line.length + 1
                } else {
                    val col = index - idx
                    println(line)
                    println(line.substring(0, col))
                    val sp = line.substring(0, col)
                        .fold(0) { acc, c -> acc + if (isHalfWidth(c)) 1 else 2 }
                    return@run Triple(col, i, sp)
                }
            }
            return@run Triple(0, 0, 0)
        }

        return """line: $line, column: $column $spaces
$message
${programLines.subList(max(0, line - 5), max(1, line + 1)).joinToString("\n")}
${" ".repeat(spaces)}${"^"}
"""
    }

    data class UnExpectedCharacter(val char: Char, override val index: Int) :
        LexerError("Unexpected character: $char", index)

    data class UnExpectedEOF(override val index: Int) : LexerError("Unexpected EOF", index)
}

sealed class ParserError(override val message: String, open val failToken: Token) : DnclError {
    override fun explain(program: String): String {
        val programLines = program.split("\n")
        val (column, line, spaces) = run {
            var index = 0
            for ((l, str) in programLines.withIndex()) {
                if (index + str.length < failToken.range.first) {
                    index += str.length + 1
                } else {
                    val col = failToken.range.first - index
                    val sp = str.substring(0, col)
                        .fold(0) { acc, c -> acc + if (isHalfWidth(c)) 1 else 2 }
                    return@run Triple(col, l, sp)
                }
            }
            return@run Triple(0, 0, 0)
        }

        return """line: $line, column: $column
$message
${programLines.subList(max(0, line - 5), line + 1).joinToString("\n")}
${" ".repeat(spaces)}${"^".repeat(max(1, failToken.range.last - failToken.range.first))}"""
    }

    data class UnExpectedToken(override val failToken: Token, val expectedToken: String? = null) :
        ParserError(
            "Unexpected token: ${failToken.literal}, ${if (expectedToken != null) "expected: $expectedToken" else ""}",
            failToken
        )

    data class InvalidIntLiteral(override val failToken: Token.Int) :
        ParserError("Invalid integer literal: ${failToken.literal}", failToken)

    data class InvalidFloatLiteral(override val failToken: Token.Float) :
        ParserError("Invalid float literal: ${failToken.literal}", failToken)

    data class UnknownPrefixOperator(override val failToken: Token) :
        ParserError("Unexpected token (Unknown prefix operator): ${failToken.literal}", failToken)

    data class UnknownInfixOperator(override val failToken: Token) :
        ParserError("Unexpected token (Unknown infix operator): ${failToken.literal}", failToken)

    data class UnexpectedIndent(override val failToken: Token) :
        ParserError("Unexpected Indent: ${failToken.literal}", failToken)
}

data class InternalError(override val message: String) : DnclError {
    override fun explain(program: String): String {
        return message
    }
}
