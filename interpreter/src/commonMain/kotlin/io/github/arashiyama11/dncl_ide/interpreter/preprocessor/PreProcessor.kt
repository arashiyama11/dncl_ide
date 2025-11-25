package io.github.arashiyama11.dncl_ide.interpreter.preprocessor

import arrow.core.Either
import io.arashiyama11.dncl_ide.generated.DnclLibs
import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.model.BuiltInFunctionSignature
import io.github.arashiyama11.dncl_ide.interpreter.model.LexerError
import io.github.arashiyama11.dncl_ide.interpreter.model.Token
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.takeWhile


fun preProcess(
    tokens: Iterable<Either<LexerError, Token>>,
    resolveLib: suspend (String) -> String,
    onBuiltInSignature: ((BuiltInFunctionSignature) -> Unit)? = null
): Flow<Either<LexerError, Token>> {
    return flow {
        // builtin.dncl を暗黙的に読み込み、シグネチャだけ収集する
        if (tokens.first().getOrNull()?.filePath?.endsWith("builtin.dncl") == false)
            processBuiltinSignatures(onBuiltInSignature)

        val iterator = tokens.iterator()
        while (iterator.hasNext()) {
            val tok = iterator.next()
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
                    if (at.literal.startsWith("@インポート(\"")) {
                        val target = at.literal.substring(8, at.literal.length - 2) //
                        val targetText = resolveLib(target)

                        emitAll(
                            preProcess(
                                Lexer(targetText, target),
                                resolveLib,
                                onBuiltInSignature
                            ).takeWhile { it.getOrNull() !is Token.EOF }
                        )
                    }
                }

                is Token.Japanese -> {
                    if (t.literal == "組み込み関数") {
                        emitAll(consumeBuiltInSignature(t, iterator, onBuiltInSignature))
                        continue
                    }
                    emit(tok)
                }

                else -> emit(tok)
            }
        }
    }
}


@Suppress("FLOW_CONST")
private suspend fun processBuiltinSignatures(
    onBuiltInSignature: ((BuiltInFunctionSignature) -> Unit)?
) {
    if (onBuiltInSignature == null) return
    val builtin = DnclLibs.texts["builtin.dncl"] ?: return
    val lexer = Lexer(builtin, "builtin.dncl")
    val iterator = lexer.iterator()

    while (iterator.hasNext()) {
        val tok = iterator.next()
        val token = tok.getOrNull() ?: continue
        if (token is Token.Japanese && token.literal == "組み込み関数") {
            consumeBuiltInSignature(token, iterator, onBuiltInSignature)
        }
    }
}

private fun consumeBuiltInSignature(
    headToken: Token.Japanese,
    iterator: Iterator<Either<LexerError, Token>>,
    onBuiltInSignature: ((BuiltInFunctionSignature) -> Unit)?
): Flow<Either<LexerError, Token>> {
    if (onBuiltInSignature == null) return flowOf()

    var name: String? = null
    val params = mutableListOf<String>()
    var insideParen = false
    var lastRange: IntRange = headToken.range

    while (iterator.hasNext()) {
        val next = iterator.next()
        val token = next.getOrNull() ?: return flowOf()
        lastRange = token.range
        when (token) {
            is Token.Identifier, is Token.Japanese -> {
                if (name == null) {
                    name = token.literal
                } else if (insideParen) {
                    params.add(token.literal)
                }
            }

            is Token.ParenOpen -> insideParen = true
            is Token.ParenClose -> {
                val signature = name?.let {
                    BuiltInFunctionSignature(
                        name = it,
                        params = params.toList(),
                        range = headToken.range.first..token.range.last,
                        filePath = headToken.filePath
                    )
                }
                signature?.let(onBuiltInSignature)
                return flowOf()
            }

            is Token.NewLine, is Token.EOF -> {
                val signature = name?.let {
                    BuiltInFunctionSignature(
                        name = it,
                        params = params.toList(),
                        range = headToken.range.first..token.range.last,
                        filePath = headToken.filePath
                    )
                }
                signature?.let(onBuiltInSignature)
                return flowOf(next)
            }

            else -> {}
        }
    }
    // EOF without newline
    val signature = name?.let {
        BuiltInFunctionSignature(
            name = it,
            params = params.toList(),
            range = headToken.range.first..lastRange.last,
            filePath = headToken.filePath
        )
    }
    signature?.let(onBuiltInSignature)
    return flowOf()
}

private fun skipUntilLineEnd(iterator: Iterator<Either<LexerError, Token>>) {
    while (iterator.hasNext()) {
        val token = iterator.next().getOrNull() ?: return
        if (token is Token.NewLine || token is Token.EOF) return
    }
}
