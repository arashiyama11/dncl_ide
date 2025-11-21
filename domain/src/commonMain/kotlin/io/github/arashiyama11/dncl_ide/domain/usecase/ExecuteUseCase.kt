package io.github.arashiyama11.dncl_ide.domain.usecase

import arrow.core.getOrElse
import io.arashiyama11.dncl_ide.generated.DnclLibs
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import io.github.arashiyama11.dncl_ide.interpreter.evaluator.InputLifecycleCallback
import io.github.arashiyama11.dncl_ide.domain.canvas.CanvasFrame
import io.github.arashiyama11.dncl_ide.domain.canvas.CanvasVirtualFile
import io.github.arashiyama11.dncl_ide.domain.model.EntryName
import io.github.arashiyama11.dncl_ide.interpreter.api.InMemoryVirtualFile
import io.github.arashiyama11.dncl_ide.interpreter.api.Stdout
import io.github.arashiyama11.dncl_ide.interpreter.api.StandardVirtualFile
import io.github.arashiyama11.dncl_ide.interpreter.api.VirtualFileSystem
import io.github.arashiyama11.dncl_ide.interpreter.api.asVirtualFile
import io.github.arashiyama11.dncl_ide.interpreter.preprocessor.preProcess
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.toList

private enum class DebugStepRunMode {
    STEP, LINE
}

class ExecuteUseCase(
    private val fileRepository: FileRepository,
    private val settingsRepository: SettingsRepository,
    private val ioDispatcher: CoroutineDispatcher,
) : InputLifecycleCallback {
    private val stepChannel = Channel<Unit>(Channel.CONFLATED)
    private val lineChannel = Channel<Unit>(Channel.CONFLATED)
    private var lastDebugStepRunMode: DebugStepRunMode? = null

    private var currentLineNumber: Int = -1
    private var outputChannel: SendChannel<DnclOutput>? = null

    override suspend fun onWaitingForInput() {
        outputChannel?.send(DnclOutput.WaitingForInput(true))
    }

    override suspend fun onInputReceived() {
        outputChannel?.send(DnclOutput.WaitingForInput(false))
    }

    init {
        CoroutineScope(ioDispatcher).launch {
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

    operator fun invoke(
        program: String,
        inputChannel: ReceiveChannel<String>,
        arrayOrigin: Int
    ): Flow<DnclOutput> {

        return channelFlow {
            val tokens = preProcess(Lexer(program), ::resolveLib).toList()
            println("====")
            println(tokens.joinToString(" ") { it.fold(ifLeft = { "" }) { it.literal } })
            val parser = Parser(tokens).getOrElse { err ->
                send(
                    DnclOutput.Error(
                        err.explain(program)
                    )
                )
                close()
                return@channelFlow
            }

            val ast = parser.parseProgram().getOrElse { err ->
                send(
                    DnclOutput.Error(
                        err.explain(program)
                    )
                )
                close()
                return@channelFlow
            }

            outputChannel = channel
            withContext(Dispatchers.Default) {
                val delayDuration = settingsRepository.onEvalDelay.value.toLong()
                var i = 0
                val evaluator = getEvaluator(
                    inputChannel,
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
                                val step = launch(ioDispatcher) {
                                    stepChannel.receive()
                                }

                                val line = launch(ioDispatcher) {
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
                    } else { _, _ ->
                        if (i++ == 1000000) {
                            ensureActive()
                        }
                    },
                    inputLifecycleCallback = this@ExecuteUseCase
                )


                val stdout = object : Stdout {
                    override suspend fun append(text: String) {
                        send(DnclOutput.StdoutAppend(text))
                    }

                    override suspend fun flush() {
                        send(DnclOutput.StdoutFlush)
                    }

                    override suspend fun clear() {
                        send(DnclOutput.StdoutClear)
                    }

                    override suspend fun commitFrame() {
                        send(DnclOutput.StdoutCommitFrame)
                    }

                    override suspend fun replace(text: String) {
                        send(DnclOutput.StdoutReplace(text))
                    }
                }

                val onCanvasFrame: suspend (CanvasFrame) -> Unit = { frame ->
                    send(DnclOutput.CanvasFrameOutput(frame))
                }

                val virtualFileSystem = VirtualFileSystem(
                    defaultFileFactory = { path ->
                        if (path.startsWith(VirtualFileSystem.CANVAS_PREFIX)) {
                            CanvasVirtualFile(path, onFrameCommitted = onCanvasFrame)
                        } else {
                            InMemoryVirtualFile(path)
                        }
                    }
                ).apply {
                    register(stdout.asVirtualFile(StandardVirtualFile.Stdout.path))
                    openOrCreate(StandardVirtualFile.Stderr.path)
                    openOrCreate(StandardVirtualFile.Stdin.path)
                }

                val globalEnv = Environment(
                    EvaluatorFactory.createBuiltInFunctionEnvironment(
                        virtualFileSystem = virtualFileSystem,
                        onImport = { onImport(it) }
                    )
                )

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
        val content = if (DnclLibs.texts.containsKey(it)) {
            DnclLibs.texts[it]!!
        } else {
            val str = it.split("/")
            val file = withTimeoutOrNull(100) {
                fileRepository.getEntryByPath(
                    EntryPath(
                        str.dropLast(1).map { FolderName(it) } + FileName(
                            str.last()
                        )
                    )).apply { if (this != null) return@withTimeoutOrNull this }

                fileRepository.getEntryByPath(
                    fileRepository.rootPath + EntryPath(
                        str.dropLast(1).map { FolderName(it) } + FileName(
                            str.last()
                        )
                    )
                )
            }

            if (file is ProgramFile) {
                fileRepository.getFileContent(file).value
            } else {
                return DnclObject.RuntimeError(
                    "ファイル:$str が見つかりません",
                    args[0].astNode
                )
            }
        }

        val tokens = preProcess(Lexer(content), ::resolveLib).toList()
        val parser =
            Parser(tokens).getOrElse { err ->
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
        return evaluator.evalProgram(prog, env).fold(ifLeft = {
            DnclObject.RuntimeError(
                it.message.orEmpty(),
                args[0].astNode
            )
        }, ifRight = {
            DnclObject.Null(args[0].astNode)
        })
    }

    private suspend fun resolveLib(path: String): String {
        val entry = fileRepository.getEntryByPath(fileRepository.rootPath + FileName(path))
        println("entry: $entry")
        require(entry is ProgramFile)
        return fileRepository.getFileContent(entry).value
    }

    private fun getEvaluator(
        inputChannel: ReceiveChannel<String>,
        arrayOrigin: Int,
        onEval: (suspend (AstNode, Environment) -> Unit)?,
        inputLifecycleCallback: InputLifecycleCallback?
    ): Evaluator {
        return EvaluatorFactory.create(
            inputChannel,
            arrayOrigin,
            inputLifecycleCallback,
            onEval,
        )
    }
}
