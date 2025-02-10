package io.github.arashiyama11.dncl.model

sealed interface DnclError {
    val message: String?
}


sealed class LexerError(override val message: String) : DnclError {
    data class UnExpectedCharacter(val char: Char) : LexerError("Unexpected character: $char")
    data object UnExpectedEOF : LexerError("Unexpected EOF")
}

sealed class ParserError(override val message: String) : DnclError {
    data class UnExpectedToken(val token: Token, val expectedToken: Token? = null) :
        ParserError("Unexpected token: $token, ${if (expectedToken != null) "expected: $expectedToken" else ""}")

    data class InvalidIntLiteral(val literal: String) :
        ParserError("Invalid integer literal: $literal")

    data class InvalidFloatLiteral(val literal: String) :
        ParserError("Invalid float literal: $literal")

    data class UnknownPrefixOperator(val operator: Token) :
        ParserError("Unknown prefix operator: $operator")

    data class UnknownInfixOperator(val operator: Token) :
        ParserError("Unknown infix operator: $operator")
}

data class InternalError(override val message: String) : DnclError
