package io.github.arashiyama11.dncl_ide.adapter

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.arashiyama11.dncl_ide.domain.model.CursorPosition
import io.github.arashiyama11.dncl_ide.domain.model.NotebookFile
import io.github.arashiyama11.dncl_ide.domain.notebook.CellType
import io.github.arashiyama11.dncl_ide.domain.notebook.Notebook
import io.github.arashiyama11.dncl_ide.domain.usecase.FileUseCase
import io.github.arashiyama11.dncl_ide.domain.usecase.NotebookFileUseCase
import io.github.arashiyama11.dncl_ide.domain.usecase.SettingsUseCase
import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.util.SyntaxHighLighter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotebookUiState(
    val notebook: Notebook? = null,
    val selectedCellId: String? = null,
    val codeCellStateMap: Map<String, CodeCellState> = emptyMap(),
    val loading: Boolean = true
)

data class CodeCellState(
    val textFieldValue: TextFieldValue,
    val annotatedString: AnnotatedString
)


sealed interface NotebookAction {
    data class SelectCell(val cellId: String) : NotebookAction
    data class ExecuteCell(val cellId: String) : NotebookAction
    data class DeleteCell(val cellId: String) : NotebookAction
    data object ExecuteAllCells : NotebookAction
    data class AddCellAfter(val cellId: String?, val cellType: CellType) : NotebookAction
    data class ChangeCellType(val cellId: String, val cellType: CellType) : NotebookAction
    data class UpdateCodeCell(val cellId: String, val textFieldValue: TextFieldValue) :
        NotebookAction

    data class UpdateMarkdownCell(val cellId: String, val source: List<String>) : NotebookAction
}


class NotebookViewModel(
    private val fileUseCase: FileUseCase,
    private val notebookFileUseCase: NotebookFileUseCase,
    private val settingsUseCase: SettingsUseCase,
    private val syntaxHighLighter: SyntaxHighLighter
) : ViewModel() {
    private val _uiState = MutableStateFlow(NotebookUiState())
    val uiState = combine(
        _uiState,
        fileUseCase.selectedEntryPath,
        settingsUseCase.settingsState
    ) { state, filePath, settings ->
        state.copy(
            notebook = state.notebook,
        )
    }.stateIn(
        viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
        initialValue = NotebookUiState()
    )

    val errorChannel = Channel<String>()

    var notebookFile: NotebookFile? = null
        private set

    fun onStart() {
        fileUseCase.selectedEntryPath.onEach {
            if (it?.isNotebookFile() == true) {
                val notebook = fileUseCase.getEntryByPath(it)
                if (notebook is NotebookFile) {
                    notebookFileUseCase.getNotebookFileContent(notebook)
                    this.notebookFile = notebook
                    _uiState.update {
                        it.copy(
                            notebook = notebookFileUseCase.getNotebookFileContent(notebook)
                        )
                    }
                    uiState.value.notebook?.cells.orEmpty().forEach { cell ->
                        onUpdateCodeCell(
                            cell.id,
                            TextFieldValue(
                                text = cell.source.joinToString("\n"),
                                selection = TextRange(0)
                            )
                        )
                    }

                    _uiState.update { it.copy(loading = false) }
                } else {
                    errorChannel.send("ノートブックを開くことができません: $notebook")
                }
            } else errorChannel.send("選択されたファイルはノートブックではありません: $it")
        }.launchIn(viewModelScope)
    }

    // New functions for cell operations

    /**
     * Add a new cell of the specified type after the cell with the given ID
     */
    fun addCellAfter(afterCellId: String?, cellType: CellType) {
        // Implementation will be added later
    }

    /**
     * Delete the cell with the given ID
     */
    fun deleteCell(cellId: String) {
        // Implementation will be added later
    }

    /**
     * Execute the cell with the given ID
     */
    fun executeCell(cellId: String) {
        // Implementation will be added later
    }

    /**
     * Execute all cells in the notebook
     */
    fun executeAllCells() {
        // Implementation will be added later
    }

    /**
     * Update the content of the cell with the given ID
     */
    fun updateCellContent(cellId: String, newContent: List<String>) {
        // Implementation will be added later
        viewModelScope.launch {
            val notebook = _uiState.value.notebook ?: return@launch
            val updatedNotebook = notebookFileUseCase.modifyNotebookCell(
                notebook,
                cellId,
                notebook.cells.firstOrNull { it.id == cellId }?.copy(source = newContent)
                    ?: return@launch
            )
            _uiState.update { it.copy(notebook = updatedNotebook) }
        }
    }

    /**
     * Select the cell with the given ID
     */
    fun selectCell(cellId: String) {
        _uiState.update { it.copy(selectedCellId = cellId) }
    }

    /**
     * Clear the outputs of the cell with the given ID
     */
    fun clearCellOutputs(cellId: String) {
        // Implementation will be added later
    }

    /**
     * Change the type of the cell with the given ID
     */
    fun changeCellType(cellId: String, newType: CellType) {
        // Implementation will be added later
    }

    fun onUpdateCodeCell(
        cellId: String,
        textFieldValue: TextFieldValue
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            val newTextFieldValue =
                autoIndent(
                    uiState.value.codeCellStateMap[cellId]?.textFieldValue ?: textFieldValue,
                    textFieldValue
                )

            val notebook = _uiState.value.notebook ?: return@launch
            val newText = newTextFieldValue.text

            val lexer = Lexer(newText)

            val (annotatedStr, error) = syntaxHighLighter.highlightWithParsedData(
                newText, true, null, lexer.toList()
            )

            val updatedNotebook = notebookFileUseCase.modifyNotebookCell(
                notebook,
                cellId,
                notebook.cells.firstOrNull { it.id == cellId }?.copy(source = newText.split("\n"))
                    ?: return@launch
            )


            _uiState.update {
                it.copy(
                    notebook = updatedNotebook,
                    codeCellStateMap = it.codeCellStateMap + (cellId to CodeCellState(
                        textFieldValue = newTextFieldValue,
                        annotatedString = annotatedStr
                    ))
                )
            }
            notebookFileUseCase.saveNotebookFile(
                notebookFile!!, with(notebookFileUseCase) {
                    updatedNotebook.toFileContent()
                }, cursorPosition = CursorPosition(0)
            )
        }
    }

    fun onUpdateMarkdownCell(
        cellId: String,
        newSource: List<String>
    ) {
        viewModelScope.launch {
            val notebook = _uiState.value.notebook ?: return@launch
            val updatedNotebook = notebookFileUseCase.modifyNotebookCell(
                notebook,
                cellId,
                notebook.cells.firstOrNull { it.id == cellId }?.copy(source = newSource)
                    ?: return@launch
            )


            _uiState.update { it.copy(notebook = updatedNotebook) }
            notebookFileUseCase.saveNotebookFile(
                notebookFile!!, with(notebookFileUseCase) {
                    updatedNotebook.toFileContent()
                }, cursorPosition = CursorPosition(0)
            )
        }
    }

    fun handleAction(action: NotebookAction) {
        when (action) {
            is NotebookAction.SelectCell -> selectCell(action.cellId)
            is NotebookAction.ExecuteCell -> executeCell(action.cellId)
            is NotebookAction.DeleteCell -> deleteCell(action.cellId)
            is NotebookAction.ExecuteAllCells -> executeAllCells()
            is NotebookAction.AddCellAfter -> addCellAfter(action.cellId, action.cellType)
            is NotebookAction.ChangeCellType -> changeCellType(action.cellId, action.cellType)
            is NotebookAction.UpdateCodeCell -> onUpdateCodeCell(
                action.cellId,
                action.textFieldValue
            )

            is NotebookAction.UpdateMarkdownCell -> onUpdateMarkdownCell(
                action.cellId,
                action.source
            )
        }
    }

    private fun autoIndent(
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
}
