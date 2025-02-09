package io.github.arashiyama11.dncl.lexer

import arrow.core.Either
import io.github.arashiyama11.dncl.model.LexerError
import io.github.arashiyama11.dncl.model.Token

interface ILexer : Iterable<Either<LexerError, Token>> {
    fun nextToken(): Either<LexerError, Token>
}