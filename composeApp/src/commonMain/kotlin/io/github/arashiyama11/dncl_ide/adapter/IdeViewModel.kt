package io.github.arashiyama11.dncl_ide.adapter

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import io.github.arashiyama11.dncl_ide.util.OutputEvent
import io.github.arashiyama11.dncl_ide.util.OutputHandler
import io.github.arashiyama11.dncl_ide.common.Action
import io.github.arashiyama11.dncl_ide.common.AppStateStore
import io.github.arashiyama11.dncl_ide.common.AppStateStore.Companion.dispatch
import io.github.arashiyama11.dncl_ide.common.StatePermission
import io.github.arashiyama11.dncl_ide.domain.model.CursorPosition
import io.github.arashiyama11.dncl_ide.domain.model.DebugRunningMode
import io.github.arashiyama11.dncl_ide.domain.model.Definition
import io.github.arashiyama11.dncl_ide.domain.model.DnclOutput
import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.domain.model.FileContent
import io.github.arashiyama11.dncl_ide.domain.model.NotebookFile
import io.github.arashiyama11.dncl_ide.domain.model.ProgramFile
import io.github.arashiyama11.dncl_ide.domain.repository.SettingsRepository.Companion.DEFAULT_DEBUG_RUNNING_MODE
import io.github.arashiyama11.dncl_ide.domain.repository.SettingsRepository.Companion.DEFAULT_FONT_SIZE
import io.github.arashiyama11.dncl_ide.domain.usecase.ExecuteUseCase
import io.github.arashiyama11.dncl_ide.domain.usecase.FileUseCase
import io.github.arashiyama11.dncl_ide.domain.usecase.SettingsUseCase
import io.github.arashiyama11.dncl_ide.domain.usecase.SuggestionUseCase
import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import io.github.arashiyama11.dncl_ide.interpreter.model.DnclError
import io.github.arashiyama11.dncl_ide.interpreter.model.Environment
import io.github.arashiyama11.dncl_ide.interpreter.parser.Parser
import io.github.arashiyama11.dncl_ide.util.SyntaxHighLighter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


data class IdeUiState(
    val codeTextFieldValue: TextFieldValue = TextFieldValue(""),
    val dnclError: DnclError? = null,
    val annotatedString: AnnotatedString? = null,
    val output: String = "",
    val currentInput: String = "",
    val isError: Boolean = false,
    val errorRange: IntRange? = null,
    val fontSize: Int = DEFAULT_FONT_SIZE,
    val currentEvaluatingLine: Int? = null,
    val textFieldType: TextFieldType = TextFieldType.OUTPUT,
    val currentEnvironment: Environment? = null,
    val isStepMode: Boolean = false,
    val isLineMode: Boolean = false,
    val isWaitingForInput: Boolean = false,
    val debugMode: Boolean = false,
    val debugRunningMode: DebugRunningMode = DEFAULT_DEBUG_RUNNING_MODE,
    val isDarkTheme: Boolean = false,
    val textSuggestions: List<Definition> = emptyList(),
    val isFocused: Boolean = false,
    val selectedEntryPath: EntryPath? = null,
    val running: Boolean = false
)

enum class TextFieldType {
    OUTPUT, DEBUG_OUTPUT
}

class IdeViewModel(
    private val syntaxHighLighter: SyntaxHighLighter,
    private val executeUseCase: ExecuteUseCase,
    private val fileUseCase: FileUseCase,
    private val settingsUseCase: SettingsUseCase,
    private val suggestionUseCase: SuggestionUseCase,
    private val appStateStore: AppStateStore<StatePermission.Write>
) : ViewModel() {
    private val appState by appStateStore

    private val _localState = MutableStateFlow(
        LocalIdeState(
            codeTextFieldValue = TextFieldValue(""),
            dnclError = null,
            annotatedString = null,
            output = "",
            currentInput = "",
            isError = false,
            errorRange = null,
            currentEvaluatingLine = null,
            textFieldType = TextFieldType.OUTPUT,
            currentEnvironment = null,
            isStepMode = false,
            isLineMode = false,
            isWaitingForInput = false,
            isDarkTheme = false,
            textSuggestions = emptyList(),
            isFocused = false
        )
    )

    val uiState = combine(
        _localState,
        appStateStore.state
    ) { localState, appState ->
        IdeUiState(
            codeTextFieldValue = localState.codeTextFieldValue,
            dnclError = localState.dnclError,
            annotatedString = localState.annotatedString,
            output = localState.output,
            currentInput = localState.currentInput,
            isError = localState.isError,
            errorRange = localState.errorRange,
            fontSize = appState.fontSize,
            currentEvaluatingLine = localState.currentEvaluatingLine,
            textFieldType = localState.textFieldType,
            currentEnvironment = localState.currentEnvironment,
            isStepMode = localState.isStepMode,
            isLineMode = localState.isLineMode,
            isWaitingForInput = localState.isWaitingForInput,
            debugMode = appState.debugModeEnabled,
            debugRunningMode = appState.debugRunningMode,
            isDarkTheme = localState.isDarkTheme,
            textSuggestions = localState.textSuggestions,
            isFocused = localState.isFocused,
            selectedEntryPath = appState.selectedEntryPath,
            running = appState.running
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, IdeUiState())

    private var executeJob: Job? = null
    private var inputChannel: Channel<String>? = null
    val errorChannel = Channel<String>(Channel.BUFFERED)
    private var executeScope: CoroutineScope = CoroutineScope(Dispatchers.Default + Job())
    private lateinit var outputHandler: OutputHandler


    fun onPause() {
        viewModelScope.launch {
            saveFile()
        }
    }

    fun onStart(isDarkTheme: StateFlow<Boolean>) {
        outputHandler = OutputHandler(executeScope) { outputs ->
            viewModelScope.launch(Dispatchers.Main) {
                _localState.update {
                    it.copy(output = outputs[null] ?: "")
                }
            }
        }

        viewModelScope.launch {
            isDarkTheme.collect {
                _localState.update { state ->
                    state.copy(isDarkTheme = it)
                }

                onTextChanged(uiState.value.codeTextFieldValue)
            }
        }

        viewModelScope.launch {
            var prePath: EntryPath? = null
            appStateStore.state.collect { appState ->
                val entryPath = appState.selectedEntryPath
                if (entryPath != null && entryPath != prePath) {
                    val programFile = fileUseCase.getEntryByPath(entryPath)
                    when (programFile) {
                        is ProgramFile -> {
                            if (prePath != null) saveFile(prePath)
                            viewModelScope.launch(Dispatchers.Main) {
                                _localState.update {
                                    it.copy(output = "")
                                }
                            }
                            onTextChanged(
                                TextFieldValue(
                                    fileUseCase.getFileContent(programFile).value,
                                    TextRange(fileUseCase.getCursorPosition(programFile).value)
                                )
                            )
                        }

                        is NotebookFile -> {
                            /*errorChannel.send("ノートブックファイルは直接編集できません")
                            with(notebookFileUseCase) {
                                onTextChanged(
                                    TextFieldValue(
                                        notebookFileUseCase.getNotebookFileContent(programFile)
                                            .toFileContent().value,
                                        TextRange(0)
                                    )
                                )
                            }*/
                        }

                        else -> errorChannel.send("ファイルが開けませんでした")
                    }
                }
                prePath = entryPath
            }
        }
    }

    fun onTextChanged(text: TextFieldValue) {
        val indentedText = autoIndent(uiState.value.codeTextFieldValue, text)

        viewModelScope.launch(Dispatchers.Default) {
            viewModelScope.launch(Dispatchers.Main) {
                _localState.update {
                    it.copy(codeTextFieldValue = indentedText)
                }
            }

            val tokens = Lexer(indentedText.text).toList()
            var error: DnclError? = null
            var parsedProgram: Either<DnclError, AstNode.Program>? = null

            if (tokens.all { it.isRight() }) {
                val parser = Parser(Lexer(indentedText.text)).getOrNull()

                if (parser != null) {
                    parsedProgram = parser.parseProgram()
                    if (parsedProgram.isLeft()) {
                        error = parsedProgram.leftOrNull()
                    }
                }
            } else {
                error = tokens.firstOrNull { it.isLeft() }?.leftOrNull()
            }

            val (annotatedString, highlightError) = syntaxHighLighter.highlightWithParsedData(
                indentedText.text,
                uiState.value.isDarkTheme,
                uiState.value.errorRange,
                tokens,
            )

            val finalError = error ?: highlightError
            viewModelScope.launch(Dispatchers.Main) {
                _localState.update {
                    it.copy(
                        dnclError = finalError,
                        output = finalError?.explain(uiState.value.codeTextFieldValue.text)
                            ?: if (it.dnclError == null) it.output else "",
                        errorRange = finalError?.errorRange
                            ?: if (it.dnclError == null) it.errorRange else null,
                    )
                }
            }

            // Use the shared results for text suggestions
            val suggestions = if (parsedProgram?.isRight() == true) {
                suggestionUseCase.suggestWithParsedData(
                    indentedText.text,
                    indentedText.selection.end,
                    tokens,
                    parsedProgram.getOrNull()!!
                )
            } else {
                emptyList()
            }

            viewModelScope.launch(Dispatchers.Main) {
                _localState.update {
                    it.copy(
                        annotatedString = annotatedString,
                        textSuggestions = suggestions
                    )
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun onRunButtonClicked() {
        viewModelScope.launch {
            saveFile()
        }

        appStateStore.dispatch(Action.SetRunning(true))

        executeJob?.cancel()
        // Cancel previous execution scope and recreate
        executeScope.coroutineContext.job.cancel().also {
            executeScope = CoroutineScope(Dispatchers.Default + Job())
            outputHandler.stop()
            outputHandler = OutputHandler(executeScope) { outputs ->
                viewModelScope.launch(Dispatchers.Main) {
                    _localState.update {
                        it.copy(output = outputs[null] ?: "")
                    }
                }
            }
        }

        inputChannel?.close()
        inputChannel = Channel(Channel.UNLIMITED)

        executeJob = executeScope.launch {
            _localState.update {
                it.copy(
                    output = "",
                    isError = false,
                    errorRange = null,
                    currentEvaluatingLine = null,
                    dnclError = null,
                    isWaitingForInput = false
                )
            }
            onTextChanged(uiState.value.codeTextFieldValue)

            executeUseCase(
                uiState.value.codeTextFieldValue.text,
                inputChannel!!,
                appState.arrayOriginIndex,
            ).collect { output ->
                when (output) {
                    is DnclOutput.RuntimeError -> {
                        viewModelScope.launch(Dispatchers.Main) {
                            _localState.update {
                                it.copy(
                                    // output = it.output + "\n" + output.value.explain(uiState.value.codeTextFieldValue.text), // Output is handled by watchStdoutChannel
                                    isError = true,
                                    errorRange = output.value.astNode.range
                                )
                            }
                        }
                        appStateStore.dispatch(Action.SetRunning(false)) // Removed cast
                    }

                    is DnclOutput.Error -> {
                        viewModelScope.launch(Dispatchers.Main) {
                            _localState.update {
                                it.copy(
                                    // output = "${it.output}\n${output.value}", // Output is handled by watchStdoutChannel
                                    isError = true
                                )
                            }
                        }
                        appStateStore.dispatch(Action.SetRunning(false)) // Removed cast
                    }

                    is DnclOutput.Stdout -> {
                        outputHandler.send(OutputEvent.Stdout(output.value))
                    }

                    is DnclOutput.Clear -> {
                        outputHandler.send(OutputEvent.Clear())
                    }

                    is DnclOutput.LineEvaluation -> {
                        viewModelScope.launch(Dispatchers.Main) {
                            _localState.update {
                                it.copy(
                                    currentEvaluatingLine = output.value
                                )
                            }
                        }
                    }

                    is DnclOutput.EnvironmentUpdate -> {
                        viewModelScope.launch(Dispatchers.Main) {
                            _localState.update {
                                it.copy(
                                    currentEnvironment = output.environment
                                )
                            }
                        }
                    }

                    is DnclOutput.WaitingForInput -> {
                        viewModelScope.launch(Dispatchers.Main) {
                            _localState.update {
                                it.copy(isWaitingForInput = output.isWaiting)
                            }
                        }
                    }
                }
            }
            outputHandler.send(OutputEvent.End())
            delay(50)
            viewModelScope.launch(Dispatchers.Main) {
                _localState.update { it.copy(currentEvaluatingLine = null /*, isExecuting = false */) }
            } // isExecuting controlled by AppState
            appStateStore.dispatch(Action.SetRunning(false)) // Removed cast
            onTextChanged(uiState.value.codeTextFieldValue)
        }
    }

    fun onCancelButtonClicked() {
        executeJob?.cancel()
        executeScope.coroutineContext.job.cancel().also {
            executeScope = CoroutineScope(Dispatchers.Default + Job())
            outputHandler.stop()
            outputHandler = OutputHandler(executeScope) { outputs ->
                viewModelScope.launch(Dispatchers.Main) {
                    _localState.update {
                        it.copy(output = outputs[null] ?: "")
                    }
                }
            }
        }
        _localState.update { it.copy(currentEvaluatingLine = null) }
        appStateStore.dispatch(Action.SetRunning(false))
    }

    fun onStepButtonClicked() {
        viewModelScope.launch {
            executeUseCase.triggerNextStep()
        }
    }

    fun onLineButtonClicked() {
        viewModelScope.launch {
            executeUseCase.triggerNextLine()
        }
    }

    fun insertText(text: String) {
        val newText = uiState.value.codeTextFieldValue.text.substring(
            0,
            uiState.value.codeTextFieldValue.selection.start
        ) + text + uiState.value.codeTextFieldValue.text.substring(uiState.value.codeTextFieldValue.selection.end)
        val newRange = TextRange(uiState.value.codeTextFieldValue.selection.start + text.length)
        onTextChanged(TextFieldValue(newText, newRange))
    }

    fun onConfirmTextSuggestion(text: String) {
        val beforeText = uiState.value.codeTextFieldValue.text.substring(
            0,
            uiState.value.codeTextFieldValue.selection.start
        )
        val toInsert = mutableListOf<Char>()
        for (i in text.indices) {
            if (text[text.length - i - 1] != beforeText.lastOrNull()) {
                toInsert.add(text[text.length - i - 1])
            } else break
        }
        val newText = uiState.value.codeTextFieldValue.text.substring(
            0,
            uiState.value.codeTextFieldValue.selection.start
        ) + toInsert.reversed().joinToString("") + uiState.value.codeTextFieldValue.text.substring(
            uiState.value.codeTextFieldValue.selection.end
        )
        val newRange = TextRange(uiState.value.codeTextFieldValue.selection.start + toInsert.size)
        onTextChanged(TextFieldValue(newText, newRange))
    }


    fun autoIndent(
        oldTextFiledValue: TextFieldValue,
        newTextFiledValue: TextFieldValue
    ): TextFieldValue {
        if (oldTextFiledValue.text.length != newTextFiledValue.text.length - 1) return newTextFiledValue
        if (newTextFiledValue.text.getOrNull(newTextFiledValue.selection.end - 1) != '\n') return newTextFiledValue
        try {
            val cursorPos = newTextFiledValue.selection.start
            val textBeforeCursor = newTextFiledValue.text.substring(0, cursorPos - 1)
            val currentLine = textBeforeCursor.substringAfterLast("\n", textBeforeCursor)
            val indent = currentLine.takeWhile { it == ' ' || it == '\t' || it == '　' }
            val insertion = if (currentLine.lastOrNull() == ':') "$indent  " else indent
            val newText =
                newTextFiledValue.text.substring(
                    0,
                    cursorPos
                ) + insertion + newTextFiledValue.text.substring(
                    cursorPos
                )
            val newCursorPos = cursorPos + insertion.length

            return TextFieldValue(
                text = newText,
                selection = TextRange(newCursorPos)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return newTextFiledValue
        }
    }

    fun onChangeIOButtonClicked() {
        val next = when (uiState.value.textFieldType) {
            TextFieldType.OUTPUT -> if (uiState.value.debugMode) TextFieldType.DEBUG_OUTPUT else TextFieldType.OUTPUT
            TextFieldType.DEBUG_OUTPUT -> TextFieldType.OUTPUT
        }
        _localState.update {
            it.copy(textFieldType = next)
        }
    }

    fun onCurrentInputChanged(text: String) {
        _localState.update {
            it.copy(currentInput = text)
        }
    }

    fun onSendInputClicked() {
        val currentInputValue = uiState.value.currentInput
        viewModelScope.launch {
            inputChannel?.send(currentInputValue)
            _localState.update {
                it.copy(
                    currentInput = ""
                )
            }
        }
    }

    fun onCodeEditorFocused(isFocused: Boolean) {
        _localState.update {
            it.copy(
                isFocused = isFocused
            )
        }
    }

    private suspend fun saveFile(entryPath: EntryPath? = null) {
        val path = entryPath ?: appState.selectedEntryPath ?: run {
            errorChannel.send("ファイルが選択されていません")
            return
        }
        when (val entry = fileUseCase.getEntryByPath(path)) {
            is NotebookFile -> {
                /* notebookFileUseCase.saveNotebookFile(
                     entry,
                     FileContent(uiState.value.codeTextFieldValue.text),
                     CursorPosition(uiState.value.codeTextFieldValue.selection.start)
                 )*/
            }

            is ProgramFile -> {
                fileUseCase.saveFile(
                    entry,
                    FileContent(uiState.value.codeTextFieldValue.text),
                    CursorPosition(uiState.value.codeTextFieldValue.selection.start)
                )
            }

            else -> errorChannel.send("ファイルを保存できませんでした")
        }
    }

    private data class LocalIdeState(
        val codeTextFieldValue: TextFieldValue,
        val dnclError: DnclError?,
        val annotatedString: AnnotatedString?,
        val output: String,
        val currentInput: String,
        val isError: Boolean,
        val errorRange: IntRange?,
        val currentEvaluatingLine: Int?,
        val textFieldType: TextFieldType,
        val currentEnvironment: Environment?,
        val isStepMode: Boolean,
        val isLineMode: Boolean,
        val isWaitingForInput: Boolean,
        val isDarkTheme: Boolean,
        val textSuggestions: List<Definition>,
        val isFocused: Boolean
    )

    private suspend fun MutableStateFlow<LocalIdeState>.updateOnMain(block: (LocalIdeState) -> LocalIdeState) {
        withContext(Dispatchers.Main) {
            this@updateOnMain.update(block)
        }
    }
}
