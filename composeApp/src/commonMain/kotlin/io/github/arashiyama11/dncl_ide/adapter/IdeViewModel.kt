package io.github.arashiyama11.dncl_ide.adapter

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.arashiyama11.dncl_ide.domain.model.CursorPosition
import io.github.arashiyama11.dncl_ide.domain.model.DnclOutput
import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.domain.model.FileContent
import io.github.arashiyama11.dncl_ide.domain.model.ProgramFile
import io.github.arashiyama11.dncl_ide.domain.repository.SettingsRepository.Companion.DEFAULT_FONT_SIZE
import io.github.arashiyama11.dncl_ide.domain.usecase.ExecuteUseCase
import io.github.arashiyama11.dncl_ide.domain.usecase.FileUseCase
import io.github.arashiyama11.dncl_ide.domain.usecase.SettingsUseCase
import io.github.arashiyama11.dncl_ide.interpreter.model.DnclError
import io.github.arashiyama11.dncl_ide.util.SyntaxHighLighter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class IdeUiState(
    val textFieldValue: TextFieldValue = TextFieldValue(""),
    val dnclError: DnclError? = null,
    val annotatedString: AnnotatedString? = null,
    val output: String = "",
    val input: String = "",
    val isError: Boolean = false,
    val errorRange: IntRange? = null,
    val isInputMode: Boolean = false,
    val fontSize: Int = DEFAULT_FONT_SIZE
)

class IdeViewModel(
    private val syntaxHighLighter: SyntaxHighLighter,
    private val executeUseCase: ExecuteUseCase,
    private val fileUseCase: FileUseCase,
    private val settingsUseCase: SettingsUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(IdeUiState())
    val uiState = combine(_uiState, settingsUseCase.fontSize) { state, fontSize ->
        state.copy(
            fontSize = fontSize
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, IdeUiState())
    private var isDarkThemeCache = false
    private var executeJob: Job? = null
    val errorChannel = Channel<String>(Channel.BUFFERED)

    fun onPause() {
        viewModelScope.launch {
            saveFile()
        }
    }

    fun onStart(isDarkTheme: Boolean) {
        isDarkThemeCache = isDarkTheme
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
                            ), isDarkThemeCache
                        )
                    } else {
                        errorChannel.send("ファイルが開けませんでした")
                    }
                }
                prePath = entryPath
            }
        }
    }

    fun onTextChanged(text: TextFieldValue, isDarkTheme: Boolean) {
        isDarkThemeCache = isDarkTheme
        val indentedText = autoIndent(uiState.value.textFieldValue, text)
        _uiState.update {
            it.copy(textFieldValue = indentedText)
        }
        viewModelScope.launch(Dispatchers.Default) {
            val (annotatedString, error) = syntaxHighLighter(
                indentedText.text, isDarkTheme, uiState.value.errorRange
            )

            if (error != null) {
                _uiState.update {
                    error
                    it.copy(
                        dnclError = error,
                        output = error.explain(uiState.value.textFieldValue.text),
                        errorRange = error.errorRange
                    )
                }
            }

            _uiState.update {
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

        executeJob = viewModelScope.launch {
            _uiState.update { it.copy(output = "", isError = false, errorRange = null) }
            onTextChanged(uiState.value.textFieldValue, isDarkThemeCache)

            executeUseCase(
                uiState.value.textFieldValue.text,
                uiState.value.input,
                settingsUseCase.arrayOriginIndex.value
            ).collect { output ->
                when (output) {
                    is DnclOutput.RuntimeError -> {
                        _uiState.update {
                            it.copy(
                                output = "${it.output}\n${output.value.message}",
                                isError = true,
                                errorRange = output.value.astNode.range
                            )
                        }
                    }

                    is DnclOutput.Error -> {
                        _uiState.update {
                            it.copy(
                                output = "${it.output}\n${output.value}",
                                isError = true
                            )
                        }
                    }

                    is DnclOutput.Stdout -> {
                        _uiState.update {
                            it.copy(
                                output = "${it.output}\n${output.value}",
                            )
                        }
                    }
                }
            }
            delay(50)
            onTextChanged(uiState.value.textFieldValue, isDarkThemeCache)
            executeJob = null
        }
    }

    fun onCancelButtonClicked() {
        executeJob?.cancel()
        executeJob = null
    }

    fun insertText(text: String) {
        val newText = uiState.value.textFieldValue.text.substring(
            0,
            uiState.value.textFieldValue.selection.start
        ) + text + uiState.value.textFieldValue.text.substring(uiState.value.textFieldValue.selection.end)
        val newRange = TextRange(uiState.value.textFieldValue.selection.start + text.length)
        onTextChanged(TextFieldValue(newText, newRange), isDarkThemeCache)
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
            it.copy(isInputMode = !it.isInputMode)
        }
    }

    fun onInputTextChanged(input: String) {
        _uiState.update {
            it.copy(input = input)
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
}