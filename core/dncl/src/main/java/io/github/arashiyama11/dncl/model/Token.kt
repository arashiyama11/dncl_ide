package io.github.arashiyama11.dncl.model

sealed interface Token {
    val literal: kotlin.String

    data object EOF : Token {
        override val literal: kotlin.String = "EOF"
    }

    data object Colon : Token {
        override val literal: kotlin.String = "COLON"
    }

    data object NewLine : Token {
        override val literal: kotlin.String = "NEW_LINE"
    }

    data object Space : Token {
        override val literal: kotlin.String = "SPACE"
    }

    data object ParenOpen : Token {
        override val literal: kotlin.String = "("
    }

    data object ParenClose : Token {
        override val literal: kotlin.String = ")"
    }

    data object Assign : Token {
        override val literal: kotlin.String = "ASSIGN"
    }


    data class Identifier(override val literal: kotlin.String) : Token
    data class Int(override val literal: kotlin.String) : Token
    data class Float(override val literal: kotlin.String) : Token
    data class String(override val literal: kotlin.String) : Token
}
