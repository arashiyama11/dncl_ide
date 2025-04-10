package io.github.arashiyama11.dncl_ide.interpreter.lexer

import arrow.core.Either
import io.github.arashiyama11.dncl_ide.interpreter.model.LexerError
import io.github.arashiyama11.dncl_ide.interpreter.model.Token

interface ILexer : Iterable<Either<LexerError, Token>> {
    fun nextToken(): Either<LexerError, Token>
}