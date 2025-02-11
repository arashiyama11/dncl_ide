package io.github.arashiyama11.dncl.model

sealed interface ExpressionStopToken

sealed class Token(
    val literal: kotlin.String,
    val range: IntRange
) {
    override fun toString(): kotlin.String {
        return "${this::class.simpleName}($literal)"
    }

    class EOF(range: IntRange) : Token("EOF", range), ExpressionStopToken

    class Colon(range: IntRange) : Token("COLON", range)

    class Comma(range: IntRange) : Token("COMMA", range)

    class NewLine(range: IntRange) : Token("NEW_LINE", range), ExpressionStopToken

    class ParenOpen(range: IntRange) : Token("(", range)

    class ParenClose(range: IntRange) : Token(")", range)

    class BracketOpen(range: IntRange) : Token("[", range)

    class BracketClose(range: IntRange) : Token("]", range)

    class BraceOpen(range: IntRange) : Token("{", range)

    class BraceClose(range: IntRange) : Token("}", range)

    class LenticularOpen(range: IntRange) : Token("【", range)

    class LenticularClose(range: IntRange) : Token("】", range)

    class Plus(range: IntRange) : Token("+", range)

    class Minus(range: IntRange) : Token("-", range)

    class Times(range: IntRange) : Token("*", range)

    class DivideInt(range: IntRange) : Token("//", range)

    class Divide(range: IntRange) : Token("/", range)

    class Modulo(range: IntRange) : Token("%", range)

    class Assign(range: IntRange) : Token("=", range)

    class Equal(range: IntRange) : Token("==", range)

    class NotEqual(range: IntRange) : Token("≠", range)

    class GreaterThan(range: IntRange) : Token(">", range)

    class LessThan(range: IntRange) : Token("<", range)

    class GreaterThanOrEqual(range: IntRange) : Token("≧", range)

    class LessThanOrEqual(range: IntRange) : Token("≦", range)

    class Bang(range: IntRange) : Token("!", range)

    class And(range: IntRange) : Token("AND", range)

    class Or(range: IntRange) : Token("OR", range)

    class If(range: IntRange) : Token("IF", range)

    class Then(range: IntRange) : Token("THEN", range), ExpressionStopToken

    class Else(range: IntRange) : Token("ELSE", range), ExpressionStopToken

    class Elif(range: IntRange) : Token("ELIF", range), ExpressionStopToken

    class Wo(range: IntRange) : Token("WO", range), ExpressionStopToken

    class Kara(range: IntRange) : Token("KARA", range), ExpressionStopToken

    class Made(range: IntRange) : Token("MADE", range), ExpressionStopToken

    class While(range: IntRange) : Token("WHILE", range)

    class UpTo(range: IntRange) : Token("UpTo", range), ExpressionStopToken

    class DownTo(range: IntRange) : Token("DownTo", range), ExpressionStopToken

    class Indent(
        val depth: kotlin.Int,
        range: IntRange
    ) : Token("Indent(${depth})", range), ExpressionStopToken

    class Identifier(literal: kotlin.String, range: IntRange) : Token(literal, range)
    class Japanese(literal: kotlin.String, range: IntRange) : Token(literal, range)
    class Int(literal: kotlin.String, range: IntRange) : Token(literal, range)
    class Float(literal: kotlin.String, range: IntRange) : Token(literal, range)
    class String(literal: kotlin.String, range: IntRange) : Token(literal, range)
}