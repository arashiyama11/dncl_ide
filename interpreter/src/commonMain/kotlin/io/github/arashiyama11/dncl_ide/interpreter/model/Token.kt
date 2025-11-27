package io.github.arashiyama11.dncl_ide.interpreter.model

sealed interface ExpressionStopToken : Token

sealed interface PrefixExpressionToken : Token

sealed interface InfixExpressionToken : Token

sealed interface Token {
    val literal: kotlin.String
    val range: IntRange
    val filePath: kotlin.String? get() = null

    data class EOF(
        override val range: IntRange,
        override val literal: kotlin.String = "EOF",
        override val filePath: kotlin.String? = null
    ) : Token

    data class Colon(
        override val range: IntRange,
        override val literal: kotlin.String = ":",
        override val filePath: kotlin.String? = null
    ) : Token

    data class Comma(
        override val range: IntRange,
        override val literal: kotlin.String = ",",
        override val filePath: kotlin.String? = null
    ) : Token

    data class NewLine(
        override val range: IntRange,
        override val literal: kotlin.String = "NEW_LINE",
        override val filePath: kotlin.String? = null
    ) : ExpressionStopToken

    data class ParenOpen(
        override val range: IntRange,
        override val literal: kotlin.String = "(",
        override val filePath: kotlin.String? = null
    ) : Token

    data class ParenClose(
        override val range: IntRange,
        override val literal: kotlin.String = ")",
        override val filePath: kotlin.String? = null
    ) : Token

    data class BracketOpen(
        override val range: IntRange,
        override val literal: kotlin.String = "[",
        override val filePath: kotlin.String? = null
    ) : Token

    data class BracketClose(
        override val range: IntRange,
        override val literal: kotlin.String = "]",
        override val filePath: kotlin.String? = null
    ) : Token

    data class BraceOpen(
        override val range: IntRange,
        override val literal: kotlin.String = "{",
        override val filePath: kotlin.String? = null
    ) : Token

    data class BraceClose(
        override val range: IntRange,
        override val literal: kotlin.String = "}",
        override val filePath: kotlin.String? = null
    ) : Token

    data class LenticularOpen(
        override val range: IntRange,
        override val literal: kotlin.String = "【",
        override val filePath: kotlin.String? = null
    ) : Token

    data class LenticularClose(
        override val range: IntRange,
        override val literal: kotlin.String = "】",
        override val filePath: kotlin.String? = null
    ) : Token

    data class Plus(
        override val range: IntRange,
        override val literal: kotlin.String = "+",
        override val filePath: kotlin.String? = null
    ) : InfixExpressionToken, PrefixExpressionToken

    data class Minus(
        override val range: IntRange,
        override val literal: kotlin.String = "-",
        override val filePath: kotlin.String? = null
    ) : InfixExpressionToken, PrefixExpressionToken

    data class Times(
        override val range: IntRange,
        override val literal: kotlin.String = "*",
        override val filePath: kotlin.String? = null
    ) : InfixExpressionToken

    data class DivideInt(
        override val range: IntRange,
        override val literal: kotlin.String = "//",
        override val filePath: kotlin.String? = null
    ) : InfixExpressionToken

    data class Divide(
        override val range: IntRange,
        override val literal: kotlin.String = "/",
        override val filePath: kotlin.String? = null
    ) : InfixExpressionToken

    data class Modulo(
        override val range: IntRange,
        override val literal: kotlin.String = "%",
        override val filePath: kotlin.String? = null
    ) : InfixExpressionToken

    data class BitAnd(
        override val range: IntRange,
        override val literal: kotlin.String = "&",
        override val filePath: kotlin.String? = null
    ) : InfixExpressionToken

    data class BitOr(
        override val range: IntRange,
        override val literal: kotlin.String = "|",
        override val filePath: kotlin.String? = null
    ) : InfixExpressionToken

    data class BitXor(
        override val range: IntRange,
        override val literal: kotlin.String = "^",
        override val filePath: kotlin.String? = null
    ) : InfixExpressionToken

    data class ShiftLeft(
        override val range: IntRange,
        override val literal: kotlin.String = "<<",
        override val filePath: kotlin.String? = null
    ) : InfixExpressionToken

    data class ShiftRight(
        override val range: IntRange,
        override val literal: kotlin.String = ">>",
        override val filePath: kotlin.String? = null
    ) : InfixExpressionToken

    data class Assign(
        override val range: IntRange,
        override val literal: kotlin.String = "=",
        override val filePath: kotlin.String? = null
    ) : Token

    data class Equal(
        override val range: IntRange,
        override val literal: kotlin.String = "==",
        override val filePath: kotlin.String? = null
    ) : InfixExpressionToken

    data class NotEqual(
        override val range: IntRange,
        override val literal: kotlin.String = "≠",
        override val filePath: kotlin.String? = null
    ) : InfixExpressionToken

    data class GreaterThan(
        override val range: IntRange,
        override val literal: kotlin.String = ">",
        override val filePath: kotlin.String? = null
    ) : InfixExpressionToken

    data class LessThan(
        override val range: IntRange,
        override val literal: kotlin.String = "<",
        override val filePath: kotlin.String? = null
    ) : InfixExpressionToken

    data class GreaterThanOrEqual(
        override val range: IntRange,
        override val literal: kotlin.String = "≧",
        override val filePath: kotlin.String? = null
    ) : InfixExpressionToken

    data class LessThanOrEqual(
        override val range: IntRange,
        override val literal: kotlin.String = "≦",
        override val filePath: kotlin.String? = null
    ) : InfixExpressionToken

    data class Bang(
        override val range: IntRange,
        override val literal: kotlin.String = "!",
        override val filePath: kotlin.String? = null
    ) : PrefixExpressionToken

    data class BitNot(
        override val range: IntRange,
        override val literal: kotlin.String = "~",
        override val filePath: kotlin.String? = null
    ) : PrefixExpressionToken

    data class And(
        override val range: IntRange,
        override val literal: kotlin.String = "AND",
        override val filePath: kotlin.String? = null
    ) : InfixExpressionToken

    data class Or(
        override val range: IntRange,
        override val literal: kotlin.String = "OR",
        override val filePath: kotlin.String? = null
    ) : Token, InfixExpressionToken

    data class If(
        override val range: IntRange,
        override val literal: kotlin.String = "もし",
        override val filePath: kotlin.String? = null
    ) : Token

    data class Then(
        override val range: IntRange,
        override val literal: kotlin.String = "ならば",
        override val filePath: kotlin.String? = null
    ) : ExpressionStopToken

    data class Else(
        override val range: IntRange,
        override val literal: kotlin.String = "そうでなければ",
        override val filePath: kotlin.String? = null
    ) : ExpressionStopToken

    data class Elif(
        override val range: IntRange,
        override val literal: kotlin.String = "そうでなくもし",
        override val filePath: kotlin.String? = null
    ) : ExpressionStopToken

    data class Wo(
        override val range: IntRange,
        override val literal: kotlin.String = "を",
        override val filePath: kotlin.String? = null
    ) : Token, ExpressionStopToken

    data class Kara(
        override val range: IntRange,
        override val literal: kotlin.String = "から",
        override val filePath: kotlin.String? = null
    ) : ExpressionStopToken

    data class Made(
        override val range: IntRange,
        override val literal: kotlin.String = "まで",
        override val filePath: kotlin.String? = null
    ) : ExpressionStopToken

    data class While(
        override val range: IntRange,
        override val literal: kotlin.String = "の間繰り返す",
        override val filePath: kotlin.String? = null
    ) : Token

    data class UpTo(
        override val range: IntRange,
        override val literal: kotlin.String = "ずつ増やしながら繰り返す",
        override val filePath: kotlin.String? = null
    ) : ExpressionStopToken

    data class Function(
        override val range: IntRange,
        override val literal: kotlin.String = "関数",
        override val filePath: kotlin.String? = null
    ) : Token

    data class Define(
        override val range: IntRange,
        override val literal: kotlin.String = "と定義する",
        override val filePath: kotlin.String? = null
    ) : Token

    data class DownTo(
        override val range: IntRange,
        override val literal: kotlin.String = "ずつ減らしながら繰り返す",
        override val filePath: kotlin.String? = null
    ) : ExpressionStopToken

    @ConsistentCopyVisibility
    data class Indent private constructor(
        val depth: kotlin.Int,
        override val range: IntRange,
        override val literal: kotlin.String = "Indent(${depth})",
        override val filePath: kotlin.String? = null
    ) : Token {
        companion object {
            operator fun invoke(
                depth: kotlin.Int,
                range: IntRange,
                filePath: kotlin.String? = null
            ) = Indent(
                depth,
                if (depth == 0) range.first + 1..range.last + 1 else range,
                filePath = filePath
            )
        }
    }

    data class Comment(
        override val literal: kotlin.String,
        override val range: IntRange,
        override val filePath: kotlin.String? = null
    ) : Token

    data class AtMark(
        override val literal: kotlin.String,
        override val range: IntRange,
        override val filePath: kotlin.String? = null
    ) : Token

    data class Identifier(
        override val literal: kotlin.String,
        override val range: IntRange,
        override val filePath: kotlin.String? = null
    ) : Token

    data class Japanese(
        override val literal: kotlin.String,
        override val range: IntRange,
        override val filePath: kotlin.String? = null
    ) : Token

    data class Int(
        override val literal: kotlin.String,
        override val range: IntRange,
        override val filePath: kotlin.String? = null
    ) : Token

    data class Float(
        override val literal: kotlin.String,
        override val range: IntRange,
        override val filePath: kotlin.String? = null
    ) : Token

    data class String(
        override val literal: kotlin.String,
        override val range: IntRange,
        override val filePath: kotlin.String? = null
    ) : Token

    data class Boolean(
        val value: kotlin.Boolean,
        override val range: IntRange,
        override val filePath: kotlin.String? = null
    ) : Token {
        override val literal: kotlin.String
            get() = if (value) if ((range.last - range.first + 1) == 1) "真" else "true" else if ((range.last - range.first + 1) == 1) "偽" else "false"
    }
}
