package io.github.arashiyama11.dncl_ide.interpreter.model

sealed interface ExpressionStopToken : Token

sealed interface PrefixExpressionToken : Token

sealed interface InfixExpressionToken : Token

sealed interface Token {
    val literal: kotlin.String
    val range: IntRange

    data class EOF(override val range: IntRange, override val literal: kotlin.String = "EOF") :
        Token

    data class Colon(override val range: IntRange, override val literal: kotlin.String = ":") :
        Token

    data class Comma(override val range: IntRange, override val literal: kotlin.String = ",") :
        Token

    data class NewLine(
        override val range: IntRange,
        override val literal: kotlin.String = "NEW_LINE"
    ) : ExpressionStopToken

    data class ParenOpen(override val range: IntRange, override val literal: kotlin.String = "(") :
        Token

    data class ParenClose(override val range: IntRange, override val literal: kotlin.String = ")") :
        Token

    data class BracketOpen(
        override val range: IntRange,
        override val literal: kotlin.String = "["
    ) : Token

    data class BracketClose(
        override val range: IntRange,
        override val literal: kotlin.String = "]"
    ) : Token

    data class BraceOpen(override val range: IntRange, override val literal: kotlin.String = "{") :
        Token

    data class BraceClose(override val range: IntRange, override val literal: kotlin.String = "}") :
        Token

    data class LenticularOpen(
        override val range: IntRange,
        override val literal: kotlin.String = "【"
    ) : Token

    data class LenticularClose(
        override val range: IntRange,
        override val literal: kotlin.String = "】"
    ) : Token

    data class Plus(override val range: IntRange, override val literal: kotlin.String = "+") :
        InfixExpressionToken, PrefixExpressionToken

    data class Minus(override val range: IntRange, override val literal: kotlin.String = "-") :
        InfixExpressionToken, PrefixExpressionToken

    data class Times(override val range: IntRange, override val literal: kotlin.String = "*") :
        InfixExpressionToken

    data class DivideInt(override val range: IntRange, override val literal: kotlin.String = "//") :
        InfixExpressionToken

    data class Divide(override val range: IntRange, override val literal: kotlin.String = "/") :
        InfixExpressionToken

    data class Modulo(override val range: IntRange, override val literal: kotlin.String = "%") :
        InfixExpressionToken

    data class Assign(override val range: IntRange, override val literal: kotlin.String = "=") :
        Token

    data class Equal(override val range: IntRange, override val literal: kotlin.String = "==") :
        InfixExpressionToken

    data class NotEqual(override val range: IntRange, override val literal: kotlin.String = "≠") :
        InfixExpressionToken

    data class GreaterThan(
        override val range: IntRange,
        override val literal: kotlin.String = ">"
    ) : InfixExpressionToken

    data class LessThan(override val range: IntRange, override val literal: kotlin.String = "<") :
        InfixExpressionToken

    data class GreaterThanOrEqual(
        override val range: IntRange,
        override val literal: kotlin.String = "≧"
    ) : InfixExpressionToken

    data class LessThanOrEqual(
        override val range: IntRange,
        override val literal: kotlin.String = "≦"
    ) : InfixExpressionToken

    data class Bang(override val range: IntRange, override val literal: kotlin.String = "!") :
        PrefixExpressionToken

    data class And(override val range: IntRange, override val literal: kotlin.String = "AND") :
        InfixExpressionToken

    data class Or(override val range: IntRange, override val literal: kotlin.String = "OR") : Token,
        InfixExpressionToken

    data class If(override val range: IntRange, override val literal: kotlin.String = "もし") :
        Token

    data class Then(override val range: IntRange, override val literal: kotlin.String = "ならば") :
        ExpressionStopToken

    data class Else(
        override val range: IntRange,
        override val literal: kotlin.String = "そうでなければ"
    ) :
        ExpressionStopToken

    data class Elif(
        override val range: IntRange,
        override val literal: kotlin.String = "そうでなくもし"
    ) :
        ExpressionStopToken

    data class Wo(override val range: IntRange, override val literal: kotlin.String = "を") : Token,
        ExpressionStopToken

    data class Kara(override val range: IntRange, override val literal: kotlin.String = "から") :
        ExpressionStopToken

    data class Made(override val range: IntRange, override val literal: kotlin.String = "まで") :
        ExpressionStopToken

    data class While(
        override val range: IntRange,
        override val literal: kotlin.String = "の間繰り返す"
    ) :
        Token

    data class UpTo(
        override val range: IntRange,
        override val literal: kotlin.String = "ずつ増やしながら繰り返す"
    ) :
        ExpressionStopToken

    data class Function(
        override val range: IntRange,
        override val literal: kotlin.String = "関数"
    ) : Token

    data class Define(
        override val range: IntRange,
        override val literal: kotlin.String = "と定義する"
    ) : Token

    data class DownTo(
        override val range: IntRange,
        override val literal: kotlin.String = "ずつ減らしながら繰り返す"
    ) : ExpressionStopToken

    data class Indent(
        val depth: kotlin.Int,
        override val range: IntRange,
        override val literal: kotlin.String = "Indent(${depth})"
    ) : Token

    data class Comment(override val literal: kotlin.String, override val range: IntRange) : Token

    data class Identifier(override val literal: kotlin.String, override val range: IntRange) : Token
    data class Japanese(override val literal: kotlin.String, override val range: IntRange) : Token
    data class Int(override val literal: kotlin.String, override val range: IntRange) : Token
    data class Float(override val literal: kotlin.String, override val range: IntRange) : Token
    data class String(override val literal: kotlin.String, override val range: IntRange) : Token
}