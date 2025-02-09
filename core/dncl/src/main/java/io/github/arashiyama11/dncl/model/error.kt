package io.github.arashiyama11.dncl.model

sealed interface DnclError {
    val message: String?
}


sealed class LexerError(override val message: String) : DnclError {
    data class UnExpectedCharacter(val char: Char) : LexerError("Unexpected character: $char")
    data object UnExpectedEOF : LexerError("Unexpected EOF")
}

data class AstError(override val message: String) : DnclError
data class ParserError(override val message: String) : DnclError
data class InternalError(override val message: String) : DnclError
