package io.github.arashiyama11.dncl_ide.domain.usecase

import arrow.core.getOrElse
import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.domain.repository.FileRepository
import io.github.arashiyama11.dncl_ide.domain.model.DnclOutput
import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.domain.model.FileName
import io.github.arashiyama11.dncl_ide.domain.model.FolderName
import io.github.arashiyama11.dncl_ide.domain.model.ProgramFile
import io.github.arashiyama11.dncl_ide.domain.repository.SettingsRepository
import io.github.arashiyama11.dncl_ide.interpreter.evaluator.CallBuiltInFunctionScope
import io.github.arashiyama11.dncl_ide.interpreter.evaluator.Evaluator
import io.github.arashiyama11.dncl_ide.interpreter.evaluator.EvaluatorFactory
import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import io.github.arashiyama11.dncl_ide.interpreter.model.DnclObject
import io.github.arashiyama11.dncl_ide.interpreter.model.Environment
import io.github.arashiyama11.dncl_ide.interpreter.parser.Parser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class ExecuteUseCase(
    private val fileRepository: FileRepository,
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(program: String, input: String, arrayOrigin: Int): Flow<DnclOutput> {
        val parser = Parser(Lexer(program)).getOrElse { err ->
            return flowOf(
                DnclOutput.Error(
                    err.explain(program)
                )
            )
        }

        val ast = parser.parseProgram().getOrElse { err ->
            return flowOf(
                DnclOutput.Error(
                    err.explain(program)
                )
            )
        }
        return channelFlow {
            withContext(Dispatchers.Default) {
                val delayDuration = settingsRepository.onEvalDelay.value.toLong()
                val evaluator = getEvaluator(
                    input,
                    arrayOrigin,
                    onStdout = { text ->
                        send(DnclOutput.Stdout(text))
                    },
                    onClear = {
                        send(DnclOutput.Clear)
                    },
                    onEval = if (settingsRepository.debugMode.value) { astNode, environment ->
                        val lineNumber = calculateLineNumber(astNode, program)
                        send(DnclOutput.LineEvaluation(lineNumber))
                        send(DnclOutput.EnvironmentUpdate(environment))
                        delay(delayDuration)
                    } else null
                )

                evaluator.evalProgram(ast).let { err ->
                    if (err.isLeft()) {
                        send(DnclOutput.Error(err.leftOrNull()!!.message.orEmpty()))
                    } else if (err.getOrNull() is DnclObject.Error) {
                        val e = err.getOrNull()!! as DnclObject.Error
                        send(DnclOutput.RuntimeError(e))
                    }
                }
            }
        }
    }

    private fun calculateLineNumber(astNode: AstNode, program: String): Int {
        val index = astNode.range.first
        var idx = 0
        for ((i, line) in program.lines().withIndex()) {
            if (idx + line.length < index) {
                idx += line.length + 1
            } else {
                return i
            }
        }
        return 0
    }

    private fun getEvaluator(
        input: String,
        arrayOrigin: Int,
        onStdout: suspend CallBuiltInFunctionScope.(String) -> Unit,
        onClear: suspend CallBuiltInFunctionScope.() -> Unit,
        onEval: (suspend (AstNode, Environment) -> Unit)?
    ): Evaluator {
        return EvaluatorFactory.create(
            input,
            arrayOrigin,
            onStdout,
            onClear,
            onEval
        ) {
            val str = it.split("/")
            val file = withTimeoutOrNull(100) {
                fileRepository.getEntryByPath(
                    EntryPath(
                        str.dropLast(1).map { FolderName(it) } + FileName(
                            str.last()
                        )
                    ))
            }

            if (file is ProgramFile) {
                val content = fileRepository.getFileContent(file).value
                val parser =
                    Parser(Lexer(content)).getOrElse { err ->
                        return@create DnclObject.RuntimeError(
                            err.explain(content),
                            args[0].astNode
                        )
                    }

                val prog = parser.parseProgram().getOrElse { err ->
                    return@create DnclObject.RuntimeError(
                        err.explain(content),
                        args[0].astNode
                    )
                }
                evaluator.evalProgram(prog, env).fold(ifLeft = {
                    DnclObject.RuntimeError(
                        it.message.orEmpty(),
                        args[0].astNode
                    )
                }, ifRight = {
                    DnclObject.Null(args[0].astNode)
                })
            } else {
                return@create DnclObject.RuntimeError(
                    "ファイル:$str が見つかりません",
                    args[0].astNode
                )
            }
        }
    }
}
