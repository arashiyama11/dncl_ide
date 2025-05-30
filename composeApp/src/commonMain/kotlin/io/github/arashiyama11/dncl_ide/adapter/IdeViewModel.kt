package io.github.arashiyama11.dncl_ide.adapter

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import io.github.arashiyama11.dncl_ide.domain.model.CursorPosition
import io.github.arashiyama11.dncl_ide.domain.model.DebugRunningMode
import io.github.arashiyama11.dncl_ide.domain.model.Definition
import io.github.arashiyama11.dncl_ide.domain.model.DnclOutput
import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.domain.model.FileContent
import io.github.arashiyama11.dncl_ide.domain.model.Folder
import io.github.arashiyama11.dncl_ide.domain.model.NotebookFile
import io.github.arashiyama11.dncl_ide.domain.model.ProgramFile
import io.github.arashiyama11.dncl_ide.domain.repository.SettingsRepository.Companion.DEFAULT_DEBUG_RUNNING_MODE
import io.github.arashiyama11.dncl_ide.domain.repository.SettingsRepository.Companion.DEFAULT_FONT_SIZE
import io.github.arashiyama11.dncl_ide.domain.usecase.ExecuteUseCase
import io.github.arashiyama11.dncl_ide.domain.usecase.FileUseCase
import io.github.arashiyama11.dncl_ide.domain.usecase.NotebookFileUseCase
import io.github.arashiyama11.dncl_ide.domain.usecase.SettingsUseCase
import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import io.github.arashiyama11.dncl_ide.interpreter.model.DnclError
import io.github.arashiyama11.dncl_ide.interpreter.model.Environment
import io.github.arashiyama11.dncl_ide.interpreter.model.explain
import io.github.arashiyama11.dncl_ide.interpreter.parser.Parser
import io.github.arashiyama11.dncl_ide.util.SyntaxHighLighter
import io.github.arashiyama11.dncl_ide.domain.usecase.SuggestionUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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
    val isExecuting: Boolean = false,
    val isWaitingForInput: Boolean = false,
    val debugMode: Boolean = false,
    val debugRunningMode: DebugRunningMode = DEFAULT_DEBUG_RUNNING_MODE,
    val isDarkTheme: Boolean = false,
    val textSuggestions: List<Definition> = emptyList(),
    val isFocused: Boolean = false
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
) : ViewModel() {
    private val _uiState = MutableStateFlow(IdeUiState())
    val uiState =
        combine(
            _uiState,
            settingsUseCase.settingsState
        ) { state, settings ->
            state.copy(
                fontSize = settings.fontSize,
                debugMode = settings.debugMode,
                debugRunningMode = settings.debugRunningMode,
                isDarkTheme = state.isDarkTheme
            )
        }.stateIn(viewModelScope, SharingStarted.Lazily, IdeUiState())
    private var executeJob: Job? = null
    private var inputChannel: Channel<String>? = null
    val errorChannel = Channel<String>(Channel.BUFFERED)

    fun onPause() {
        viewModelScope.launch {
            saveFile()
        }
    }

    fun onStart(isDarkTheme: StateFlow<Boolean>) {
        viewModelScope.launch {
            isDarkTheme.collect {
                _uiState.update { state ->
                    state.copy(isDarkTheme = it)
                }

                onTextChanged(uiState.value.codeTextFieldValue)
            }
        }
        viewModelScope.launch {
            var prePath: EntryPath? = null
            fileUseCase.selectedEntryPath.collect { entryPath ->
                if (entryPath != null) {
                    val programFile = fileUseCase.getEntryByPath(entryPath)
                    when (programFile) {
                        is ProgramFile -> {
                            if (prePath != null) saveFile(prePath)

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
            _uiState.updateOnMain {
                it.copy(codeTextFieldValue = indentedText)
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
            _uiState.updateOnMain {
                it.copy(
                    dnclError = finalError,
                    output = finalError?.explain(uiState.value.codeTextFieldValue.text)
                        ?: if (it.dnclError == null) it.output else "",
                    errorRange = finalError?.errorRange
                        ?: if (it.dnclError == null) it.errorRange else null,
                )
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
                suggestionUseCase.suggestWhenFailingParse(
                    indentedText.text,
                    indentedText.selection.end
                )
            }

            _uiState.updateOnMain {
                it.copy(
                    annotatedString = annotatedString,
                    isError = error != null || highlightError != null,
                    textSuggestions = suggestions
                )
            }
        }
    }

    fun onRunButtonClicked() {
        viewModelScope.launch {
            saveFile()
        }

        executeJob?.cancel()
        inputChannel?.close()
        inputChannel = Channel(Channel.UNLIMITED)

        executeJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    output = "",
                    isError = false,
                    errorRange = null,
                    currentEvaluatingLine = null,
                    isExecuting = true,
                    dnclError = null,
                    isWaitingForInput = false
                )
            }
            onTextChanged(uiState.value.codeTextFieldValue)

            executeUseCase(
                uiState.value.codeTextFieldValue.text,
                inputChannel!!,
                settingsUseCase.arrayOriginIndex.value
            ).collect { output ->
                when (output) {
                    is DnclOutput.RuntimeError -> {
                        _uiState.updateOnMain {
                            it.copy(
                                output = it.output + "\n" + output.value.explain(uiState.value.codeTextFieldValue.text),
                                isError = true,
                                errorRange = output.value.astNode.range
                            )
                        }
                    }

                    is DnclOutput.Error -> {
                        _uiState.updateOnMain {
                            it.copy(
                                output = "${it.output}\n${output.value}",
                                isError = true
                            )
                        }
                    }

                    is DnclOutput.Stdout -> {
                        _uiState.updateOnMain {
                            it.copy(
                                output = "${it.output}\n${output.value}",
                            )
                        }
                    }

                    is DnclOutput.Clear -> {
                        _uiState.updateOnMain {
                            it.copy(
                                output = "",
                                isError = false,
                                errorRange = null
                            )
                        }
                    }

                    is DnclOutput.LineEvaluation -> {
                        _uiState.updateOnMain {
                            it.copy(
                                currentEvaluatingLine = output.value
                            )
                        }
                    }

                    is DnclOutput.EnvironmentUpdate -> {
                        _uiState.updateOnMain {
                            it.copy(
                                currentEnvironment = output.environment.copy()
                            )
                        }
                    }

                    is DnclOutput.WaitingForInput -> {
                        _uiState.updateOnMain {
                            it.copy(isWaitingForInput = output.isWaiting)
                        }
                    }
                }
            }
            delay(50)
            _uiState.updateOnMain { it.copy(currentEvaluatingLine = null, isExecuting = false) }
            onTextChanged(uiState.value.codeTextFieldValue)
        }
    }

    fun onCancelButtonClicked() {
        executeJob?.cancel()
        _uiState.update { it.copy(currentEvaluatingLine = null, isExecuting = false) }
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
        _uiState.update {
            it.copy(textFieldType = next)
        }
    }

    fun onCurrentInputChanged(text: String) {
        _uiState.update {
            it.copy(currentInput = text)
        }
    }

    fun onSendInputClicked() {
        val currentInputValue = uiState.value.currentInput
        viewModelScope.launch {
            inputChannel?.send(currentInputValue)
            _uiState.update {
                it.copy(
                    currentInput = ""
                )
            }
        }
    }

    fun onCodeEditorFocused(isFocused: Boolean) {
        _uiState.update {
            it.copy(
                isFocused = isFocused
            )
        }
    }

    private suspend fun saveFile(entryPath: EntryPath? = null) {
        val path = entryPath ?: fileUseCase.selectedEntryPath.value ?: run {
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

    private suspend fun <T> MutableStateFlow<T>.updateOnMain(
        block: suspend (T) -> T
    ) {
        withContext(Dispatchers.Main) {
            update { block(value) }
        }
    }
}
