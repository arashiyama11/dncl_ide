package io.github.arashiyama11.dncl.model

sealed interface DnclError {
    val message: String?
}

data class LexerError(override val message: String) : DnclError
data class AstError(override val message: String) : DnclError
data class ParserError(override val message: String) : DnclError
data class InternalError(override val message: String) : DnclError
