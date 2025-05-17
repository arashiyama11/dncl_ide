package io.github.arashiyama11.dncl_ide.adapter

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.arashiyama11.dncl_ide.domain.model.CursorPosition
import io.github.arashiyama11.dncl_ide.domain.model.DebugRunningMode
import io.github.arashiyama11.dncl_ide.domain.model.DnclOutput
import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.domain.model.FileContent
import io.github.arashiyama11.dncl_ide.domain.model.ProgramFile
import io.github.arashiyama11.dncl_ide.domain.repository.SettingsRepository.Companion.DEFAULT_DEBUG_RUNNING_MODE
import io.github.arashiyama11.dncl_ide.domain.repository.SettingsRepository.Companion.DEFAULT_FONT_SIZE
import io.github.arashiyama11.dncl_ide.domain.usecase.ExecuteUseCase
import io.github.arashiyama11.dncl_ide.domain.usecase.FileUseCase
import io.github.arashiyama11.dncl_ide.domain.usecase.SettingsUseCase
import io.github.arashiyama11.dncl_ide.interpreter.model.DnclError
import io.github.arashiyama11.dncl_ide.interpreter.model.Environment
import io.github.arashiyama11.dncl_ide.util.SyntaxHighLighter
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
    val textFieldValue: TextFieldValue = TextFieldValue(""),
    val dnclError: DnclError? = null,
    val annotatedString: AnnotatedString? = null,
    val output: String = "",
    val input: String = "",
    val isError: Boolean = false,
    val errorRange: IntRange? = null,
    val fontSize: Int = DEFAULT_FONT_SIZE,
    val currentEvaluatingLine: Int? = null,
    val textFieldType: TextFieldType = TextFieldType.OUTPUT,
    val currentEnvironment: Environment? = null,
    val isStepMode: Boolean = false,
    val isLineMode: Boolean = false,
    val isExecuting: Boolean = false,
    val debugMode: Boolean = false,
    val debugRunningMode: DebugRunningMode = DEFAULT_DEBUG_RUNNING_MODE,
    val isDarkTheme: Boolean = false,
)

enum class TextFieldType {
    OUTPUT, INPUT, DEBUG_OUTPUT
}

class IdeViewModel(
    private val syntaxHighLighter: SyntaxHighLighter,
    private val executeUseCase: ExecuteUseCase,
    private val fileUseCase: FileUseCase,
    private val settingsUseCase: SettingsUseCase
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
                debugRunningMode = settings.debugRunningMode
            )
        }.stateIn(viewModelScope, SharingStarted.Lazily, IdeUiState())
    private var executeJob: Job? = null
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

                onTextChanged(uiState.value.textFieldValue)
            }
        }
        viewModelScope.launch {
            var prePath: EntryPath? = null
            fileUseCase.selectedEntryPath.collect { entryPath ->
                if (entryPath != null) {
                    val programFile = fileUseCase.getEntryByPath(entryPath)
                    if (programFile is ProgramFile) {
                        if (prePath != null) saveFile(prePath)

                        onTextChanged(
                            TextFieldValue(
                                fileUseCase.getFileContent(programFile).value,
                                TextRange(fileUseCase.getCursorPosition(programFile).value)
                            )
                        )
                    } else {
                        errorChannel.send("ファイルが開けませんでした")
                    }
                }
                prePath = entryPath
            }
        }
    }

    fun onTextChanged(text: TextFieldValue) {
        val indentedText = autoIndent(uiState.value.textFieldValue, text)

        viewModelScope.launch(Dispatchers.Default) {
            _uiState.updateOnMain {
                it.copy(textFieldValue = indentedText)
            }
            val (annotatedString, error) = syntaxHighLighter(
                indentedText.text, uiState.value.isDarkTheme, uiState.value.errorRange
            )

            if (error != null) {
                _uiState.updateOnMain {
                    error
                    it.copy(
                        dnclError = error,
                        output = error.explain(uiState.value.textFieldValue.text),
                        errorRange = error.errorRange
                    )
                }
            }

            _uiState.updateOnMain {
                it.copy(
                    annotatedString = annotatedString,
                    isError = error != null
                )
            }
        }
    }

    fun onRunButtonClicked() {
        viewModelScope.launch {
            saveFile()
        }

        executeJob?.cancel()

        executeJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    output = "",
                    isError = false,
                    errorRange = null,
                    currentEvaluatingLine = null,
                    isExecuting = true
                )
            }
            onTextChanged(uiState.value.textFieldValue)

            executeUseCase(
                uiState.value.textFieldValue.text,
                uiState.value.input,
                settingsUseCase.arrayOriginIndex.value
            ).collect { output ->
                when (output) {
                    is DnclOutput.RuntimeError -> {
                        _uiState.updateOnMain {
                            it.copy(
                                output = "${it.output}\n${output.value.message}",
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
                }
            }
            withContext(Dispatchers.IO) {
                delay(50)
            }
            _uiState.updateOnMain { it.copy(currentEvaluatingLine = null, isExecuting = false) }
            onTextChanged(uiState.value.textFieldValue)
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
        val newText = uiState.value.textFieldValue.text.substring(
            0,
            uiState.value.textFieldValue.selection.start
        ) + text + uiState.value.textFieldValue.text.substring(uiState.value.textFieldValue.selection.end)
        val newRange = TextRange(uiState.value.textFieldValue.selection.start + text.length)
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
        _uiState.update {
            it.copy(textFieldType = if (it.textFieldType == TextFieldType.OUTPUT) TextFieldType.INPUT else TextFieldType.OUTPUT)
        }
    }

    fun onInputTextChanged(input: String) {
        _uiState.update {
            it.copy(input = input)
        }
    }

    fun onChangeDebugOutputClicked() {
        _uiState.update {
            it.copy(textFieldType = if (it.textFieldType == TextFieldType.DEBUG_OUTPUT) TextFieldType.OUTPUT else TextFieldType.DEBUG_OUTPUT)
        }
    }

    private suspend fun saveFile(entryPath: EntryPath? = null) {
        val path = entryPath ?: fileUseCase.selectedEntryPath.value ?: run {
            errorChannel.send("ファイルが選択されていません")
            return
        }
        val entry = fileUseCase.getEntryByPath(path)
        if (entry is ProgramFile) {
            fileUseCase.saveFile(
                entry,
                FileContent(uiState.value.textFieldValue.text),
                CursorPosition(uiState.value.textFieldValue.selection.start)
            )
        } else {
            errorChannel.send("ファイルを保存できませんでした")
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
