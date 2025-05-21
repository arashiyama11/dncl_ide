package io.github.arashiyama11.dncl_ide.interpreter.model

import kotlin.math.max
import kotlin.math.min

sealed interface DnclError {
    val message: String?
    val errorRange: IntRange?
    fun explain(program: String): String
}

private fun isHalfWidth(char: Char): Boolean {
    val code = char.code
    return (code in 0x0020..0x007E) || (code in 0xFF61..0xFF9F)
}


sealed class LexerError(override val message: String, open val index: Int) :
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
                    val sp = line.substring(0, col)
                        .fold(0) { acc, c -> acc + if (isHalfWidth(c)) 1 else 2 }
                    return@run Triple(col, i + 1, sp)
                }
            }
            return@run Triple(0, 0, 0)
        }

        return """行: $line, 列: $column $spaces
$message
${programLines.subList(max(0, line - 3), min(programLines.size, line)).joinToString("\n")}
${" ".repeat(spaces)}${"^"}
"""
    }

    data class UnExpectedCharacter(
        val char: Char, override val index: Int,
        override val errorRange: IntRange = index..index
    ) :
        LexerError("予期しない文字: $char", index)

    data class UnExpectedEOF(
        override val index: Int,
        override val errorRange: IntRange = index..index,
        override val message: String
    ) : LexerError(message, index)
}

sealed class ParserError(
    override val message: String, open val failToken: Token,
    override val errorRange: IntRange? = failToken.range
) : DnclError {
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
                    return@run Triple(col, l + 1, sp)
                }
            }
            return@run Triple(0, 0, 0)
        }

        return """行: $line, 列: $column
$message
${programLines.subList(max(0, line - 3), line).joinToString("\n")}
${" ".repeat(spaces)}${"^".repeat(max(1, failToken.range.last - failToken.range.first))}"""
    }

    data class UnExpectedToken(override val failToken: Token, val expectedToken: String? = null) :
        ParserError(
            "予期しないトークン: ${failToken.literal}${if (expectedToken != null) "\n期待されるトークン: $expectedToken" else ""}",
            failToken
        )

    data class InvalidIntLiteral(override val failToken: Token.Int) :
        ParserError("無効な整数リテラル: ${failToken.literal}", failToken)

    data class InvalidFloatLiteral(override val failToken: Token.Float) :
        ParserError("無効な浮動小数点リテラル: ${failToken.literal}", failToken)

    data class UnknownPrefixOperator(override val failToken: Token) :
        ParserError("予期しないトークン（不明な前置演算子）: ${failToken.literal}", failToken)

    data class UnknownInfixOperator(override val failToken: Token) :
        ParserError("予期しないトークン（不明な中置演算子）: ${failToken.literal}", failToken)

    data class IndentError(override val failToken: Token, val expected: String) :
        ParserError("インデントエラー: ${failToken.literal} 期待される値: $expected", failToken) {
        constructor(failToken: Token, expected: Int) : this(failToken, expected.toString())
    }

}

data class InternalError(override val message: String, override val errorRange: IntRange? = null) :
    DnclError {
    override fun explain(program: String): String {
        return message
    }
}
