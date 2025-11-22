package io.github.arashiyama11.dncl_ide.interpreter.preprocessor

import arrow.core.Either
import io.arashiyama11.dncl_ide.generated.DnclLibs
import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.model.LexerError
import io.github.arashiyama11.dncl_ide.interpreter.model.Token
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.takeWhile


fun preProcess(
    tokens: Iterable<Either<LexerError, Token>>,
    resolveLib: suspend (String) -> String
): Flow<Either<LexerError, Token>> {
    return flow {
        for (tok in tokens) {
            if (tok.isLeft()) {
                emit(tok)
                continue
            }

            when (val t = tok.getOrNull()!!) {
                is Token.Comment -> {
                    continue
                }

                is Token.AtMark -> {
                    val at = t as Token.AtMark
                    val target = at.literal.substring(8, at.literal.length - 2)
                    val targetText = if (DnclLibs.texts.containsKey(target)) {
                        DnclLibs.texts[target]!!
                    } else {
                        resolveLib(target)
                    }

                    emitAll(
                        preProcess(
                            Lexer(targetText, target),
                            resolveLib
                        ).takeWhile { it.getOrNull() !is Token.EOF }
                    )
                }

                else -> emit(tok)
            }
        }
    }
}
