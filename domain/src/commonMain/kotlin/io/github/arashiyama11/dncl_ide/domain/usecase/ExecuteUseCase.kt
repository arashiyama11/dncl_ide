package io.github.arashiyama11.dncl_ide.domain.usecase

import arrow.core.getOrElse
import io.github.arashiyama11.dncl_ide.domain.model.DebugRunningMode
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private enum class DebugStepRunMode {
    STEP, LINE
}

class ExecuteUseCase(
    private val fileRepository: FileRepository,
    private val settingsRepository: SettingsRepository
) {
    private val stepChannel = Channel<Unit>(Channel.CONFLATED)
    private val lineChannel = Channel<Unit>(Channel.CONFLATED)
    private var lastDebugStepRunMode: DebugStepRunMode? = null

    private var currentLineNumber: Int = -1

    init {
        CoroutineScope(Dispatchers.IO).launch {
            settingsRepository.debugRunningMode.collect {
                if (it == DebugRunningMode.NON_BLOCKING) {
                    lastDebugStepRunMode = null
                    stepChannel.send(Unit)
                    lineChannel.send(Unit)
                }
            }
        }
    }


    suspend fun triggerNextStep() {
        stepChannel.send(Unit)
    }

    suspend fun triggerNextLine() {
        lineChannel.send(Unit)
    }

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
                    onEval = if (settingsRepository.debugMode.value) onEvalLambda@{ astNode, environment ->
                        val lineNumber = calculateLineNumber(astNode, program)
                        send(DnclOutput.LineEvaluation(lineNumber))
                        send(DnclOutput.EnvironmentUpdate(environment))
                        if (lastDebugStepRunMode == DebugStepRunMode.LINE && currentLineNumber == lineNumber) {
                            return@onEvalLambda
                        }
                        when (settingsRepository.debugRunningMode.value) {
                            DebugRunningMode.BUTTON -> {
                                val step = launch(Dispatchers.IO) {
                                    stepChannel.receive()
                                }

                                val line = launch(Dispatchers.IO) {
                                    lineChannel.receive()
                                }

                                suspendCancellableCoroutine<Unit> { cont ->
                                    step.invokeOnCompletion {
                                        if (it != null) return@invokeOnCompletion
                                        currentLineNumber = lineNumber
                                        lastDebugStepRunMode = DebugStepRunMode.STEP
                                        line.cancel()
                                        if (cont.isActive)
                                            cont.resume(Unit)
                                    }

                                    line.invokeOnCompletion {
                                        if (it != null) return@invokeOnCompletion
                                        currentLineNumber = lineNumber
                                        lastDebugStepRunMode = DebugStepRunMode.LINE
                                        step.cancel()
                                        if (cont.isActive)
                                            cont.resume(Unit)
                                    }


                                    cont.invokeOnCancellation {
                                        step.cancel()
                                        line.cancel()
                                    }
                                }
                            }

                            DebugRunningMode.NON_BLOCKING -> {
                                delay(delayDuration)
                            }
                        }
                    } else null
                )

                val globalEnv = Environment(
                    EvaluatorFactory.createBuiltInFunctionEnvironment(
                        onStdout = { text ->
                            send(DnclOutput.Stdout(text))
                        },
                        onClear = {
                            send(DnclOutput.Clear)
                        },
                        onImport = { onImport(it) }
                    ))

                evaluator.evalProgram(ast, globalEnv).let { err ->
                    if (err.isLeft()) {
                        send(DnclOutput.Error(err.leftOrNull()!!.explain(program)))
                    } else if (err.getOrNull() is DnclObject.Error) {
                        val e = err.getOrNull()!! as DnclObject.Error
                        send(DnclOutput.RuntimeError(e))
                    }
                }
            }
        }.also {
            currentLineNumber = -1
            lastDebugStepRunMode = null
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

    private suspend fun CallBuiltInFunctionScope.onImport(it: String): DnclObject {
        val str = it.split("/")
        val file = withTimeoutOrNull(100) {
            fileRepository.getEntryByPath(
                EntryPath(
                    str.dropLast(1).map { FolderName(it) } + FileName(
                        str.last()
                    )
                ))
        }

        return if (file is ProgramFile) {
            val content = fileRepository.getFileContent(file).value
            val parser =
                Parser(Lexer(content)).getOrElse { err ->
                    return DnclObject.RuntimeError(
                        err.explain(content),
                        args[0].astNode
                    )
                }

            val prog = parser.parseProgram().getOrElse { err ->
                return DnclObject.RuntimeError(
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
            DnclObject.RuntimeError(
                "ファイル:$str が見つかりません",
                args[0].astNode
            )
        }
    }

    private fun getEvaluator(
        input: String,
        arrayOrigin: Int,
        onEval: (suspend (AstNode, Environment) -> Unit)?
    ): Evaluator {
        return EvaluatorFactory.create(
            input,
            arrayOrigin, onEval
        )
    }
}
