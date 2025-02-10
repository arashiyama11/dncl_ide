package io.github.arashiyama11.dncl.model

sealed interface ExpressionStopToken

sealed interface Token {
    val literal: kotlin.String

    data object EOF : Token, ExpressionStopToken {
        override val literal: kotlin.String = "EOF"
    }

    data object Colon : Token {
        override val literal: kotlin.String = "COLON"
    }

    data object Comma : Token {
        override val literal: kotlin.String = "COMMA"
    }

    data object NewLine : Token, ExpressionStopToken {
        override val literal: kotlin.String = "NEW_LINE"
    }


    data object ParenOpen : Token {
        override val literal: kotlin.String = "("
    }

    data object ParenClose : Token {
        override val literal: kotlin.String = ")"
    }

    data object BracketOpen : Token {
        override val literal: kotlin.String = "["
    }

    data object BracketClose : Token {
        override val literal: kotlin.String = "]"
    }

    data object BraceOpen : Token {
        override val literal: kotlin.String = "{"
    }

    data object BraceClose : Token {
        override val literal: kotlin.String = "}"
    }

    data object LenticularOpen : Token {
        override val literal: kotlin.String = "【"
    }

    data object LenticularClose : Token {
        override val literal: kotlin.String = "】"
    }

    data object Plus : Token {
        override val literal: kotlin.String = "+"
    }

    data object Minus : Token {
        override val literal: kotlin.String = "-"
    }

    data object Times : Token {
        override val literal: kotlin.String = "*"
    }


    data object DivideInt : Token {
        override val literal: kotlin.String = "//"
    }

    data object Divide : Token {
        override val literal: kotlin.String = "/"
    }

    data object Modulo : Token {
        override val literal: kotlin.String = "%"
    }

    data object Assign : Token {
        override val literal = "="
    }

    data object Equal : Token {
        override val literal: kotlin.String = "=="
    }

    data object NotEqual : Token {
        override val literal: kotlin.String = "≠"
    }

    data object GreaterThan : Token {
        override val literal: kotlin.String = ">"
    }

    data object LessThan : Token {
        override val literal: kotlin.String = "<"
    }

    data object GreaterThanOrEqual : Token {
        override val literal: kotlin.String = "≧"
    }

    data object LessThanOrEqual : Token {
        override val literal: kotlin.String = "≦"
    }

    data object Bang : Token {
        override val literal: kotlin.String = "!"
    }

    data object And : Token {
        override val literal: kotlin.String = "AND"
    }

    data object Or : Token {
        override val literal: kotlin.String = "OR"
    }

    data object If : Token {
        override val literal: kotlin.String = "IF"
    }

    data object Then : Token, ExpressionStopToken {
        override val literal: kotlin.String = "THEN"
    }

    data object Else : Token, ExpressionStopToken {
        override val literal: kotlin.String = "ELSE"
    }

    data object Elif : Token, ExpressionStopToken {
        override val literal: kotlin.String = "ELIF"
    }

    data object Wo : Token, ExpressionStopToken {
        override val literal: kotlin.String = "WO"
    }

    data object Kara : Token, ExpressionStopToken {
        override val literal: kotlin.String = "KARA"
    }

    data object Made : Token, ExpressionStopToken {
        override val literal: kotlin.String = "MADE"
    }

    data object While : Token {
        override val literal: kotlin.String = "WHILE"
    }

    data object UpTo : Token, ExpressionStopToken {
        override val literal: kotlin.String = "UpTo"
    }

    data object DownTo : Token, ExpressionStopToken {
        override val literal: kotlin.String = "DownTo"
    }

    data class Indent(
        val depth: kotlin.Int,
        override val literal: kotlin.String = "Indent(${depth})"
    ) : Token, ExpressionStopToken


    data class Identifier(override val literal: kotlin.String) : Token
    data class Japanese(override val literal: kotlin.String) : Token
    data class Int(override val literal: kotlin.String) : Token
    data class Float(override val literal: kotlin.String) : Token
    data class String(override val literal: kotlin.String) : Token
}
