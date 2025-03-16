package io.github.arashiyama11.dncl_interpreter.adapter

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.arashiyama11.dncl.model.DnclError
import io.github.arashiyama11.domain.model.CursorPosition
import io.github.arashiyama11.domain.model.DnclOutput
import io.github.arashiyama11.domain.model.EntryPath
import io.github.arashiyama11.domain.model.FileContent
import io.github.arashiyama11.domain.model.ProgramFile
import io.github.arashiyama11.domain.usecase.ExecuteUseCase
import io.github.arashiyama11.domain.usecase.FileUseCase
import io.github.arashiyama11.domain.usecase.SettingsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel


data class IdeUiState(
    val textFieldValue: TextFieldValue = TextFieldValue(
        """Data = [3,18,29,33,48,52,62,77,89,97]
kazu = 要素数(Data)
表示する("0～99の数字を入力してください")
atai = 【外部からの入力】
hidari = 0 , migi = kazu - 1
owari = 0
hidari <= migi and owari == 0 の間繰り返す:
  aida = (hidari+migi) ÷ 2 # 演算子÷は商の整数値を返す
  もし Data[aida] == atai ならば:
    表示する(atai, "は", aida, "番目にありました")
    owari = 1
  そうでなくもし Data[aida] < atai ならば:
    hidari = aida + 1
  そうでなければ:
    migi = aida - 1
もし owari == 0 ならば:
  表示する(atai, "は見つかりませんでした")
表示する("添字", " ", "要素")
i を 0 から kazu - 1 まで 1 ずつ増やしながら繰り返す:
  表示する(i, " ", Data[i])
"""
    ),
    val dnclError: DnclError? = null,
    val annotatedString: AnnotatedString? = null,
    val output: String = "",
    val input: String = "",
    val isError: Boolean = false,
    val errorRange: IntRange? = null,
    val isInputMode: Boolean = false
)

@KoinViewModel
class IdeViewModel(
    private val syntaxHighLighter: SyntaxHighLighter,
    private val executeUseCase: ExecuteUseCase,
    private val fileUseCase: FileUseCase,
    private val settingsUseCase: SettingsUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(IdeUiState())
    val uiState = _uiState.asStateFlow()
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
        _uiState.update {
            it.copy(textFieldValue = text)
        }
        viewModelScope.launch(Dispatchers.Default) {
            val (annotatedString, error) = syntaxHighLighter(
                text.text, isDarkTheme, uiState.value.errorRange
            )

            if (error != null) {
                _uiState.update {
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


    fun onEditorKeyEvent(keyEvent: KeyEvent): Boolean {
        if (keyEvent.type != KeyEventType.KeyUp || keyEvent.key != Key.Enter) return false
        viewModelScope.launch {
            val codeText = uiState.value.textFieldValue
            val cursorPos = codeText.selection.start
            val textBeforeCursor = codeText.text.substring(0, cursorPos - 1)
            val currentLine = textBeforeCursor.substringAfterLast("\n", textBeforeCursor)
            val indent = currentLine.takeWhile { it == ' ' || it == '\t' || it == '　' }
            val insertion = if (currentLine.lastOrNull() == ':') "$indent  " else indent
            val newText =
                codeText.text.substring(0, cursorPos) + insertion + codeText.text.substring(
                    cursorPos
                )
            val newCursorPos = cursorPos + insertion.length

            onTextChanged(
                TextFieldValue(
                    text = newText,
                    selection = TextRange(newCursorPos)
                ), isDarkThemeCache
            )
        }

        return true
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