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
import io.github.arashiyama11.dncl_ide.domain.notebook.Output
import io.github.arashiyama11.dncl_ide.domain.usecase.FileUseCase
import io.github.arashiyama11.dncl_ide.domain.usecase.NotebookFileUseCase
import io.github.arashiyama11.dncl_ide.domain.usecase.SettingsUseCase
import io.github.arashiyama11.dncl_ide.interpreter.evaluator.EvaluatorFactory
import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import io.github.arashiyama11.dncl_ide.interpreter.model.DnclObject
import io.github.arashiyama11.dncl_ide.interpreter.model.Environment
import io.github.arashiyama11.dncl_ide.util.SyntaxHighLighter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class NotebookUiState(
    val notebook: Notebook? = null,
    val selectedCellId: String? = null,
    val codeCellStateMap: Map<String, CodeCellState> = emptyMap(),
    val loading: Boolean = true,
    val focusedCellId: String? = null
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
    data object CancelExecution : NotebookAction
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
        started = SharingStarted.Lazily,
        initialValue = NotebookUiState()
    )

    val errorChannel = Channel<String>()

    private var notebookFile: NotebookFile? = null
    private var selectCellId: String? = null
    private var executeJob: Job? = null


    private lateinit var environment: Environment

    fun onStart() {
        fileUseCase.selectedEntryPath.onEach {
            coroutineScope {
                if (it?.isNotebookFile() == true) {
                    val notebookFile = fileUseCase.getEntryByPath(it)
                    if (notebookFile is NotebookFile) {
                        notebookFileUseCase.getNotebook(notebookFile)
                        this@NotebookViewModel.notebookFile = notebookFile
                        val notebook = notebookFileUseCase.getNotebook(notebookFile)
                        _uiState.update {
                            it.copy(
                                notebook = notebook
                            )
                        }
                        awaitAll(*notebook.cells.map { cell ->
                            async {
                                onUpdateCodeCell(
                                    cell.id,
                                    TextFieldValue(
                                        text = cell.source.joinToString("\n"),
                                        selection = TextRange(0)
                                    )
                                ).join()
                            }
                        }.toTypedArray())

                        _uiState.update { it.copy(loading = false) }
                    } else {
                        errorChannel.send("ノートブックを開くことができません: $notebookFile")
                    }
                } else errorChannel.send("選択されたファイルはノートブックではありません: $it")
            }
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            environment = Environment(
                EvaluatorFactory.createBuiltInFunctionEnvironment(
                    onStdout = { outputStr ->
                        this@NotebookViewModel.viewModelScope.launch {
                            val file = notebookFile ?: return@launch
                            val notebook = uiState.value.notebook ?: return@launch
                            val newOutput = Output(
                                outputType = "stream",
                                name = "stdout",
                                text = listOf(outputStr)
                            )
                            val updatedNotebook = notebookFileUseCase.appendOutputAndSave(
                                file,
                                notebook,
                                selectCellId ?: return@launch,
                                newOutput
                            )
                            _uiState.update { it.copy(notebook = updatedNotebook) }
                        }
                    }, onClear = {
                        clearCellOutput(selectCellId!!)
                    }, onImport = { DnclObject.Null(AstNode.Program(emptyList())) }
                ))
        }
    }

    /**
     * Add a new cell of the specified type after the cell with the given ID
     */
    fun addCellAfter(afterCellId: String?, cellType: CellType) {
        viewModelScope.launch {
            val file = notebookFile ?: return@launch
            val notebook = _uiState.value.notebook ?: return@launch
            // 新しいセルIDとデフォルトソースを生成
            val cellId = generateCellId()
            val defaultSource =
                if (cellType == CellType.CODE) listOf("") else listOf("## 新しいセル")
            val newCell = notebookFileUseCase.createCell(
                id = cellId,
                type = cellType,
                source = defaultSource,
                executionCount = if (cellType == CellType.CODE) 0 else null,
                outputs = if (cellType == CellType.CODE) emptyList() else null
            )
            // セル挿入と保存
            val updatedNotebook = notebookFileUseCase.insertCellAndSave(
                file,
                notebook,
                newCell,
                afterCellId
            )
            // UIステートの更新
            _uiState.update {
                it.copy(notebook = updatedNotebook, selectedCellId = cellId)
            }
        }
    }

    /**
     * Delete the cell with the given ID
     */
    fun deleteCell(cellId: String) {
        viewModelScope.launch {
            val file = notebookFile ?: return@launch
            val notebook = _uiState.value.notebook ?: return@launch
            // セル削除と保存
            val updatedNotebook = notebookFileUseCase.deleteCellAndSave(
                file,
                notebook,
                cellId
            )
            // 次に選択するセルを決定
            val cells = notebook.cells
            val cellIndex = cells.indexOfFirst { it.id == cellId }
            val nextSelected = when {
                cells.size <= 1 -> null
                cellIndex > 0 -> cells[cellIndex - 1].id
                else -> cells[1].id
            }
            // CodeCellStateMapから削除
            val updatedStateMap = _uiState.value.codeCellStateMap - cellId
            // UIステートの更新
            _uiState.update {
                it.copy(
                    notebook = updatedNotebook,
                    selectedCellId = nextSelected,
                    codeCellStateMap = updatedStateMap
                )
            }
        }
    }

    /**
     * Execute the cell with the given ID
     */
    fun executeCell(cellId: String) {
        selectCellId = cellId
        executeJob?.cancel()
        executeJob = viewModelScope.launch {
            clearCellOutput(cellId).join()
            delay(50) //await clear
            notebookFileUseCase.executeCell(
                uiState.value.notebook!!, cellId, environment
            )
        }
    }

    fun clearCellOutput(cellId: String): Job {
        return viewModelScope.launch {
            val file = notebookFile ?: return@launch
            val notebook = _uiState.value.notebook ?: return@launch
            // 出力クリアと保存
            val updatedNotebook = notebookFileUseCase.clearCellOutputAndSave(
                file,
                notebook,
                cellId
            )
            // UIステートの更新
            _uiState.update { it.copy(notebook = updatedNotebook) }
        }
    }

    /**
     * Execute all cells in the notebook
     */
    fun executeAllCells() {
        // Cancel any ongoing execution
        executeJob?.cancel()
        // Launch new execution job
        executeJob = viewModelScope.launch {
            val notebook = _uiState.value.notebook ?: return@launch

            // すべてのセルの出力をクリア
            val clearedNotebook = notebook.copy(
                cells = notebook.cells.map { cell ->
                    cell.copy(outputs = emptyList(), executionCount = 0)
                }
            )

            _uiState.update { it.copy(notebook = clearedNotebook) }

            // 各セルを順番に実行
            for (cell in clearedNotebook.cells) {
                if (cell.type == CellType.CODE) {
                    selectCellId = cell.id
                    // コードセルの実行
                    delay(100) // UIの更新を待つ
                    notebookFileUseCase.executeCell(
                        _uiState.value.notebook!!,
                        cell.id,
                        environment
                    )
                    delay(200) // 実行完了を少し待つ
                }
            }
        }
    }

    /**
     * Select the cell with the given ID
     */
    fun selectCell(cellId: String) {
        _uiState.update { it.copy(selectedCellId = cellId) }
    }

    /**
     * Change the type of the cell with the given ID
     */
    fun changeCellType(cellId: String, newType: CellType) {
        viewModelScope.launch {
            val file = notebookFile ?: return@launch
            val notebook = _uiState.value.notebook ?: return@launch
            // タイプ変更と保存
            val updatedNotebook = notebookFileUseCase.changeCellTypeAndSave(
                file,
                notebook,
                cellId,
                newType
            )
            // UIステート更新
            _uiState.update { it.copy(notebook = updatedNotebook) }
            // コードセルに変更した場合は、CodeCellStateを初期化
            if (newType == CellType.CODE) {
                onUpdateCodeCell(
                    cellId,
                    TextFieldValue(
                        text = notebook.cells.first { it.id == cellId }.source.joinToString("\n"),
                        selection = TextRange(0)
                    )
                )
            }
        }
    }

    fun onUpdateCodeCell(
        cellId: String,
        textFieldValue: TextFieldValue
    ): Job {
        return viewModelScope.launch(Dispatchers.Default) {
            val file = notebookFile ?: return@launch
            // インデント調整
            val newTextFieldValue = autoIndent(
                uiState.value.codeCellStateMap[cellId]?.textFieldValue ?: textFieldValue,
                textFieldValue
            )
            val notebook = _uiState.value.notebook ?: return@launch
            val newText = newTextFieldValue.text
            // シンタックスハイライト用処理
            val lexer = Lexer(newText)
            val (annotatedStr, _) = syntaxHighLighter.highlightWithParsedData(
                newText, true, null, lexer.toList()
            )
            // セル更新と保存をユースケースに委譲
            val updatedNotebook = notebookFileUseCase.updateCellAndSave(
                file,
                notebook,
                cellId
            ) { oldCell ->
                oldCell.copy(source = newText.split("\n"))
            }
            // UIステートの更新
            _uiState.update {
                it.copy(
                    notebook = updatedNotebook,
                    codeCellStateMap = it.codeCellStateMap + (cellId to CodeCellState(
                        textFieldValue = newTextFieldValue,
                        annotatedString = annotatedStr
                    ))
                )
            }
        }
    }

    fun onUpdateMarkdownCell(
        cellId: String,
        newSource: List<String>
    ) {
        viewModelScope.launch {
            val file = notebookFile ?: return@launch
            val notebook = _uiState.value.notebook ?: return@launch
            // セル更新と保存をユースケースに委譲
            val updatedNotebook = notebookFileUseCase.updateCellAndSave(
                file,
                notebook,
                cellId
            ) { oldCell ->
                oldCell.copy(source = newSource)
            }
            // UIステートの更新
            _uiState.update { it.copy(notebook = updatedNotebook) }
        }
    }

    fun handleAction(action: NotebookAction) {
        when (action) {
            is NotebookAction.SelectCell -> selectCell(action.cellId)
            is NotebookAction.ExecuteCell -> executeCell(action.cellId)
            is NotebookAction.DeleteCell -> deleteCell(action.cellId)
            is NotebookAction.ExecuteAllCells -> executeAllCells()
            is NotebookAction.CancelExecution -> executeJob?.cancel()
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

    @OptIn(ExperimentalUuidApi::class)
    fun generateCellId(): String {
        return "cell-" + Uuid.random().toString()
    }
}
