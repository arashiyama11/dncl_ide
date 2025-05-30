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
                    onStdout = {
                        val newNotebook = uiState.value.notebook!!.copy(
                            cells = uiState.value.notebook!!.cells.map { cell ->
                                if (cell.id == selectCellId) {
                                    cell.copy(
                                        outputs = cell.outputs!! + Output(
                                            outputType = "stream",
                                            name = "stdout",
                                            text = listOf(it)
                                        ),
                                        executionCount = (cell.executionCount ?: 0) + 1
                                    )
                                } else {
                                    cell
                                }
                            }
                        )

                        _uiState.update { it.copy(notebook = newNotebook) }

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
            val notebook = _uiState.value.notebook ?: return@launch
            val cellId = generateCellId()

            // デフォルトの内容を設定
            val defaultSource = when (cellType) {
                CellType.CODE -> listOf("")
                CellType.MARKDOWN -> listOf("## 新しいセル")
            }

            // 新しいセルを挿入する位置を決定
            val cells = notebook.cells.toMutableList()
            val insertIndex = if (afterCellId != null) {
                cells.indexOfFirst { it.id == afterCellId } + 1
            } else {
                0 // afterCellIdがnullの場合は先頭に挿入
            }

            // 新しいセルの作成
            val newCell = notebookFileUseCase.createCell(
                id = cellId,
                type = cellType,
                source = defaultSource,
                executionCount = 0,
                outputs = emptyList()
            )

            // セルを挿入
            if (insertIndex in cells.indices) {
                cells.add(insertIndex, newCell)
            } else {
                cells.add(newCell)
            }

            // ノートブックを更新
            val updatedNotebook = notebook.copy(cells = cells)
            _uiState.update {
                it.copy(
                    notebook = updatedNotebook,
                    selectedCellId = cellId
                )
            }

            // 新しいセルがコードセルの場合は、初期化
            if (cellType == CellType.CODE) {
                onUpdateCodeCell(
                    cellId,
                    TextFieldValue(
                        text = defaultSource.joinToString("\n"),
                        selection = TextRange(0)
                    )
                )
            }

            // ノートブックを保存
            notebookFileUseCase.saveNotebookFile(
                notebookFile!!,
                with(notebookFileUseCase) {
                    updatedNotebook.toFileContent()
                },
                cursorPosition = CursorPosition(0)
            )
        }
    }

    /**
     * Delete the cell with the given ID
     */
    fun deleteCell(cellId: String) {
        viewModelScope.launch {
            val notebook = _uiState.value.notebook ?: return@launch

            // 削除するセルの位置を特定
            val cells = notebook.cells
            val cellIndex = cells.indexOfFirst { it.id == cellId }
            if (cellIndex == -1) return@launch  // セルが見つからない場合

            // 削除後に選択するセルを決定
            val nextSelectedCellId = when {
                cells.size <= 1 -> null  // これが最後のセルならnull
                cellIndex > 0 -> cells[cellIndex - 1].id  // 前のセル
                else -> cells[1].id  // 後ろのセル
            }

            // セルを削除
            val updatedCells = cells.filterNot { it.id == cellId }
            val updatedNotebook = notebook.copy(cells = updatedCells)

            // CodeCellStateMapから削除
            val updatedCellStateMap = _uiState.value.codeCellStateMap.toMutableMap()
            updatedCellStateMap.remove(cellId)

            _uiState.update {
                it.copy(
                    notebook = updatedNotebook,
                    selectedCellId = nextSelectedCellId,
                    codeCellStateMap = updatedCellStateMap
                )
            }

            // ノートブックを保存
            notebookFileUseCase.saveNotebookFile(
                notebookFile!!,
                with(notebookFileUseCase) {
                    updatedNotebook.toFileContent()
                },
                cursorPosition = CursorPosition(0)
            )
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
            val notebook = _uiState.value.notebook ?: return@launch
            val updatedCells = notebook.cells.map { cell ->
                if (cell.id == cellId) {
                    cell.copy(outputs = emptyList(), executionCount = 0)
                } else {
                    cell
                }
            }
            val updatedNotebook = notebook.copy(cells = updatedCells)
            _uiState.update { it.copy(notebook = updatedNotebook) }
            /*notebookFileUseCase.saveNotebookFile(
                notebookFile!!, with(notebookFileUseCase) {
                    updatedNotebook.toFileContent()
                }, cursorPosition = CursorPosition(0)
            )*/
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
     * Update the content of the cell with the given ID
     */
    fun updateCellContent(cellId: String, newContent: List<String>) {
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
        viewModelScope.launch {
            val notebook = _uiState.value.notebook ?: return@launch
            val cell = notebook.cells.firstOrNull { it.id == cellId } ?: return@launch

            if (cell.type == newType) return@launch // 既に同じタイプならば何もしない

            // セルのタイプを変更した新しいセル
            val updatedCell = notebookFileUseCase.createCell(
                id = cellId,
                type = newType,
                source = cell.source,
                executionCount = if (newType == CellType.CODE) 0 else null,
                outputs = if (newType == CellType.CODE) emptyList() else null
            )

            // ノートブックを更新
            val updatedNotebook = notebookFileUseCase.modifyNotebookCell(
                notebook,
                cellId,
                updatedCell
            )

            _uiState.update { it.copy(notebook = updatedNotebook) }

            // コードセルに変更した場合は、CodeCellStateを初期化
            if (newType == CellType.CODE) {
                onUpdateCodeCell(
                    cellId,
                    TextFieldValue(
                        text = cell.source.joinToString("\n"),
                        selection = TextRange(0)
                    )
                )
            }

            // ノートブックを保存
            notebookFileUseCase.saveNotebookFile(
                notebookFile!!,
                with(notebookFileUseCase) {
                    updatedNotebook.toFileContent()
                },
                cursorPosition = CursorPosition(0)
            )
        }
    }

    fun onUpdateCodeCell(
        cellId: String,
        textFieldValue: TextFieldValue
    ): Job {
        return viewModelScope.launch(Dispatchers.Default) {
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
        // KMP-safe unique ID: timestamp + random
        return "cell-" + Uuid.random().toString() + "-" + Random.nextInt(100000, 999999)
            .toString()
    }
}
