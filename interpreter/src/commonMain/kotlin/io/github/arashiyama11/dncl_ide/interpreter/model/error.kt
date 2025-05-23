package io.github.arashiyama11.dncl_ide.interpreter.model

import arrow.core.leftNel
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier

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
        return explainError(program, message, index until index)
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
                    return@run Triple(col + 1, i + 1, sp)
                }
            }
            return@run Triple(0, 0, 0)
        }

        return """${line}行${column}文字目でエラーが発生しました
$message
${"=".repeat(15)}
${
            programLines.withIndex().toList().subList(max(0, line - 3), line)
                .joinToString("\n") { (index, value) ->
                    "${
                        (index + 1).toString().padStart((line).toString().length, ' ')
                    }| $value"
                }
        }
${" ".repeat((line - 1).toString().length + 2 + spaces)}${"^"}
"""
    }

    data class UnExpectedCharacter(
        val char: Char, override val index: Int,
        override val errorRange: IntRange = index..index
    ) :
        LexerError("「$char」は無効な文字です", index)

    data class UnExpectedEOF(
        override val index: Int,
        override val errorRange: IntRange = index..index, override val message: String
    ) : LexerError(message, index)
}

sealed class ParserError(
    override val message: String, open val failToken: Token,
    override val errorRange: IntRange = failToken.range
) : DnclError {
    override fun explain(program: String): String {
        return explainError(program, message, errorRange)
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
                    return@run Triple(col + 1, l + 1, sp)
                }
            }
            return@run Triple(0, 0, 0)
        }

        val hats =
            program.substring(errorRange.first, min(program.length, errorRange.last + 1))
                .fold(0) { acc, c -> acc + if (isHalfWidth(c)) 1 else 2 }
                .let { if (it == 0) 1 else it }
        return """${line}行${column}文字目でエラーが発生しました
$message
${"=".repeat(15)}
${
            programLines.withIndex().toList().subList(max(0, line - 3), line)
                .joinToString("\n") { (index, value) ->
                    "${
                        (index + 1).toString().padStart((line).toString().length, ' ')
                    }| $value"
                }
        }
${" ".repeat((line - 1).toString().length + 2 + spaces)}${"^".repeat(hats)}"""
    }

    data class ParseError(
        override val message: String,
        override val failToken: Token,
        override val errorRange: IntRange = failToken.range
    ) : ParserError(message, failToken, errorRange)

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

    data class IndentError(override val failToken: Token, override val message: String) :
        ParserError(message, failToken) {
        constructor(failToken: Token, indentStack: List<Int>) : this(
            failToken,
            "インデントエラー\n予期しないインデント: ${failToken.literal}\nインデントは ${if (indentStack.size == 1) "${indentStack.single()} である必要があります" else "$indentStack のいずれかである必要があります"}"
        )

        constructor(failToken: Token, expected: Int) : this(
            failToken,
            "インデントエラー\n予期しないインデント: ${failToken.literal}\nインデントは $expected である必要があります"
        )
    }

}

data class InternalError(override val message: String, override val errorRange: IntRange? = null) :
    DnclError {
    override fun explain(program: String): String {
        return message
    }
}


inline fun <reified T : Token> tokenToLiteral(): String? {
    return when (T::class) {
        Token.Colon::class -> ":"
        Token.Comma::class -> ","
        Token.NewLine::class -> "NEW_LINE"
        Token.ParenOpen::class -> "("
        Token.ParenClose::class -> ")"
        Token.BracketOpen::class -> "["
        Token.BracketClose::class -> "]"
        Token.BraceOpen::class -> "{"
        Token.BraceClose::class -> "}"
        Token.LenticularOpen::class -> "【"
        Token.LenticularClose::class -> "】"
        Token.Plus::class -> "+"
        Token.Minus::class -> "-"
        Token.Times::class -> "*"
        Token.DivideInt::class -> "//"
        Token.Divide::class -> "/"
        Token.Modulo::class -> "%"
        Token.Assign::class -> "="
        Token.Equal::class -> "=="
        Token.NotEqual::class -> "≠"
        Token.GreaterThan::class -> ">"
        Token.LessThan::class -> "<"
        Token.GreaterThanOrEqual::class -> "≧"
        Token.LessThanOrEqual::class -> "≦"
        Token.Bang::class -> "!"
        Token.And::class -> "AND"
        Token.Or::class -> "OR"
        Token.If::class -> "もし"
        Token.Then::class -> "ならば"
        Token.Else::class -> "そうでなければ"
        Token.Elif::class -> "そうでなくもし"
        Token.Wo::class -> "を"
        Token.Kara::class -> "から"
        Token.Made::class -> "まで"
        Token.While::class -> "の間繰り返す"
        Token.UpTo::class -> "ずつ増やしながら繰り返す"
        Token.Function::class -> "関数"
        Token.Define::class -> "と定義する"
        Token.DownTo::class -> "ずつ減らしながら"
        else -> ""
    }
}

private fun explainError(
    program: String,
    message: String,
    errorRange: IntRange
): String {
    val programLines = program.split("\n")
    val (column, line, spaces) = run {
        var index = 0
        for ((l, str) in programLines.withIndex()) {
            if (index + str.length < errorRange.first) {
                index += str.length + 1
            } else {
                val col = errorRange.first - index
                val sp = str.substring(0, col)
                    .fold(0) { acc, c -> acc + if (isHalfWidth(c)) 1 else 2 }
                return@run Triple(col + 1, l + 1, sp)
            }
        }
        return@run Triple(0, 0, 0)
    }

    val hats =
        program.substring(errorRange.first, min(program.length, errorRange.last + 1))
            .fold(0) { acc, c -> acc + if (isHalfWidth(c)) 1 else 2 }
            .let { if (it <= 0) 1 else it }
    return """${line}行${column}文字目でエラーが発生しました
$message
${"=".repeat(15)}
${
        programLines.withIndex().toList().subList(max(0, line - 3), line)
            .joinToString("\n") { (index, value) ->
                "${
                    (index + 1).toString().padStart((line).toString().length, ' ')
                }| $value"
            }
    }
${" ".repeat((line - 1).toString().length + 2 + spaces)}${"^".repeat(hats)}"""
}


fun DnclObject.Error.explain(program: String): String {
    return explainError(program, message, astNode.range)
}