package io.github.arashiyama11.dncl.model

sealed interface Token {
    val literal: kotlin.String

    data object EOF : Token {
        override val literal: kotlin.String = "EOF"
    }

    data object Colon : Token {
        override val literal: kotlin.String = "COLON"
    }

    data object Comma : Token {
        override val literal: kotlin.String = "COMMA"
    }

    data object NewLine : Token {
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

    data object If : Token {
        override val literal: kotlin.String = "IF"
    }

    data object Then : Token {
        override val literal: kotlin.String = "THEN"
    }

    data object Else : Token {
        override val literal: kotlin.String = "ELSE"
    }

    data object Elif : Token {
        override val literal: kotlin.String = "ELIF"
    }

    data class Indent(
        val depth: kotlin.Int,
        override val literal: kotlin.String = "Indent(${depth})"
    ) : Token


    data class Identifier(override val literal: kotlin.String) : Token
    data class Japanese(override val literal: kotlin.String) : Token
    data class Int(override val literal: kotlin.String) : Token
    data class Float(override val literal: kotlin.String) : Token
    data class String(override val literal: kotlin.String) : Token
}
