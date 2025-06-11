package io.github.arashiyama11.dncl_ide.adapter

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.getOrElse
import io.github.arashiyama11.dncl_ide.common.AppStateStore
import io.github.arashiyama11.dncl_ide.domain.model.CursorPosition
import io.github.arashiyama11.dncl_ide.domain.model.Definition
import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.domain.model.NotebookFile
import io.github.arashiyama11.dncl_ide.domain.notebook.CellType
import io.github.arashiyama11.dncl_ide.domain.notebook.Notebook
import io.github.arashiyama11.dncl_ide.domain.notebook.Output
import io.github.arashiyama11.dncl_ide.domain.usecase.FileUseCase
import io.github.arashiyama11.dncl_ide.domain.usecase.NotebookFileUseCase
import io.github.arashiyama11.dncl_ide.domain.usecase.SuggestionUseCase
import io.github.arashiyama11.dncl_ide.interpreter.evaluator.EvaluatorFactory
import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.model.Environment
import io.github.arashiyama11.dncl_ide.interpreter.parser.Parser
import io.github.arashiyama11.dncl_ide.util.SyntaxHighLighter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlin.coroutines.coroutineContext
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class NotebookUiState(
    val notebook: Notebook? = null,
    val selectedCellId: String? = null,
    val codeCellStateMap: Map<String, CodeCellState> = emptyMap(),
    val loading: Boolean = true,
    val focusedCellId: String? = null,
    val cellSuggestionsMap: Map<String, List<Definition>> = emptyMap(),
    val fontSize: Int = 16,
    val selectedEntryPath: EntryPath? = null
)

data class CodeCellState(
    val textFieldValue: TextFieldValue = TextFieldValue(
        text = "",
        selection = TextRange(0)
    ),
    val annotatedString: AnnotatedString = AnnotatedString("")
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
    private val syntaxHighLighter: SyntaxHighLighter,
    private val suggestionUseCase: SuggestionUseCase,
    private val appStateStore: AppStateStore
) : ViewModel() {
    companion object {
        private const val SAVE_DELAY_MS = 1000L
    }

    private val saveJobs = mutableMapOf<String, Job>()
    private val _localState = MutableStateFlow(
        NotebookLocalState(
            notebook = null,
            selectedCellId = null,
            codeCellStateMap = emptyMap(),
            loading = true,
            focusedCellId = null,
            cellSuggestionsMap = emptyMap()
        )
    )

    val uiState = combine(
        _localState,
        appStateStore.state
    ) { localState, appState ->
        NotebookUiState(
            notebook = localState.notebook,
            selectedCellId = localState.selectedCellId,
            codeCellStateMap = localState.codeCellStateMap,
            loading = localState.loading,
            focusedCellId = localState.focusedCellId,
            cellSuggestionsMap = localState.cellSuggestionsMap,
            fontSize = appState.fontSize,
            selectedEntryPath = appState.selectedEntryPath
        )
    }.stateIn(
        viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = NotebookUiState()
    )

    val errorChannel = Channel<String>()

    private var stdoutChannel = Channel<String>(capacity = 1024)
    val mutex = Mutex()

    private val notebookMutex = Mutex()
    var pendingCount = 0

    private var notebookFile: NotebookFile? = null
    private var selectCellId: String? = null
    private var executeScope: CoroutineScope = CoroutineScope(Dispatchers.Default + Job())
    //private var watchJob: Job? = null

    private lateinit var environment: Environment
    private var started = false

    @OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    fun onStart() {
        if (started) return
        started = true
        appStateStore.state.onEach { appState ->
            val entryPath = appState.selectedEntryPath

            run {
                notebookFileUseCase.saveNotebookFile(
                    notebookFile ?: return@run,
                    with(notebookFileUseCase) {
                        _localState.value.notebook?.toFileContent() ?: return@run
                    },
                    CursorPosition(0)
                )
            }

            coroutineScope {
                if (entryPath?.isNotebookFile() == true) {
                    val notebookFile = fileUseCase.getEntryByPath(entryPath)
                    if (notebookFile is NotebookFile) {
                        this@NotebookViewModel.notebookFile = notebookFile
                        val notebook =
                            runCatching { notebookFileUseCase.getNotebook(notebookFile) }.onFailure {
                                errorChannel.send("ノートブックの読み込みに失敗しました: ${it.message}")
                                return@coroutineScope
                            }.getOrNull()!!
                        updateLocalNotebook(notebook)

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

                        _localState.update { it.copy(loading = false) }
                    } else {
                        errorChannel.send("ノートブックを開くことができません: $notebookFile")
                    }
                }
            }
        }.launchIn(viewModelScope)

        viewModelScope.launch(Dispatchers.Default) {
            environment = Environment(
                EvaluatorFactory.createBuiltInFunctionEnvironment(
                    onStdout = { outputStr ->
                        if (!stdoutChannel.isClosedForSend)
                            stdoutChannel.send(outputStr)
                        mutex.withLock {
                            pendingCount++
                        }
                    }, onClear = {
                        if (!stdoutChannel.isClosedForSend)
                            stdoutChannel.send("\u0000")
                        mutex.withLock {
                            pendingCount++
                        }
                    }, onImport = { importPath ->
                        // IMPORT 処理をユースケースに委譲
                        println("Importing from: $importPath")
                        with(notebookFileUseCase) {
                            importAndExecute(
                                notebookFile!!,
                                importPath,
                                environment
                            )
                        }.also { println("Import completed") }
                    }
                ))
        }

        executeScope.launch {
            watchStdoutChannel()
        }
    }

    context(scope: CoroutineScope)
    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    private suspend fun watchStdoutChannel() {
        try {
            println("starting watchStdoutChannel stdout")
            var notebookCache = _localState.value.notebook
            val channel = stdoutChannel
            //busy時はcacheを使う
            var isBusy = false
            var i = 0
            for (outputStr in channel) {
                if (channel.isClosedForReceive || !coroutineContext.isActive || !scope.coroutineContext.isActive || scope.coroutineContext.job.isCancelled) {
                    println("stdout channel closed or coroutine context is not active, exiting watchStdoutChannel")
                    println("condition: ${channel.isClosedForReceive} ${coroutineContext.isActive} ${scope.coroutineContext.isActive} ${scope.coroutineContext.job.isCancelled}")
                    return
                }
                if (i++ > 100) {
                    i = 0
                    yield()
                }
                if (channel.isClosedForReceive) {
                    println("stdout channel closed, exiting watchStdoutChannel")
                    return
                }

                mutex.withLock {
                    pendingCount--
                    if (stdoutChannel.isEmpty || pendingCount < 0) {
                        pendingCount = 0
                    }
                }

                val x = 4L - pendingCount.toLong()
                val t = x * x * x + x * 10L
                if (t > 0) {
                    delay(t)
                }
                // busy終了時
                if (stdoutChannel.isEmpty && isBusy) {
                    println("end busy")
                    isBusy = false
                    withContext(Dispatchers.Main.immediate) {
                        updateLocalNotebook(notebookCache ?: return@withContext)
                    }
                    mutex.withLock {
                        pendingCount = 0
                    }
                }

                // busy開始時
                if (!stdoutChannel.isEmpty && !isBusy) {
                    println("start busy")
                    isBusy = true
                    notebookCache = _localState.value.notebook
                }

                if (outputStr == "\u0000") {
                    notebookCache = notebookFileUseCase.clearCellOutput(
                        notebookFile ?: continue,
                        notebookCache ?: continue,
                        selectCellId ?: continue
                    )
                    // busy時は出力消去アップデートをしない
                    if (!isBusy) withContext(Dispatchers.Main) {
                        updateLocalNotebook(notebookCache)
                    }
                    continue
                }
                val newOutput = Output("stream", "stdout", listOf(outputStr))
                notebookCache = notebookFileUseCase.appendOutput(
                    notebookFile ?: continue,
                    notebookCache ?: continue,
                    selectCellId ?: continue,
                    newOutput
                )

                if (isBusy) {
                    if (pendingCount < 20)
                        scope.launch(Dispatchers.Main.immediate) {
                            updateLocalNotebook(notebookCache ?: return@launch)
                        }
                } else {
                    withContext(Dispatchers.Main.immediate) {
                        updateLocalNotebook(notebookCache)
                    }
                }
            }

        } finally {
            println("watchStdoutChannel finished, closing stdoutChannel")
        }
    }

    /**
     * Add a new cell of the specified type after the cell with the given ID
     */
    fun addCellAfter(afterCellId: String?, cellType: CellType) {
        viewModelScope.launch {
            val file = notebookFile ?: return@launch
            val notebook = _localState.value.notebook ?: return@launch
            // 新しいセルIDとデフォルトソースを生成
            val cellId = generateCellId()
            val defaultSource =
                if (cellType == CellType.CODE) listOf("1+2") else listOf("## 新しいセル")
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

            when (cellType) {
                CellType.CODE -> {
                    onUpdateCodeCell(
                        cellId,
                        TextFieldValue(
                            text = newCell.source.joinToString("\n"),
                            selection = TextRange(0)
                        )
                    )
                }

                CellType.MARKDOWN -> {

                }
            }


            // UIステートの更新
            updateLocalNotebook(updatedNotebook)
            _localState.update {
                it.copy(selectedCellId = cellId)
            }
        }
    }

    /**
     * Delete the cell with the given ID
     */
    fun deleteCell(cellId: String) {
        viewModelScope.launch {
            val file = notebookFile ?: return@launch
            val notebook = _localState.value.notebook ?: return@launch
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
            val updatedStateMap = _localState.value.codeCellStateMap - cellId
            // UIステートの更新
            updateLocalNotebook(updatedNotebook)
            _localState.update {
                it.copy(
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
        cancelExecution().invokeOnCompletion {
            executeScope.launch {
                clearCellOutput(cellId).join()
                delay(100) //await clear
                val output = notebookFileUseCase.executeCell(
                    uiState.value.notebook!!, cellId, environment
                )

                val file = notebookFile ?: return@launch
                val notebook = _localState.value.notebook ?: return@launch

                notebookFileUseCase.appendOutput(
                    file,
                    notebook,
                    cellId,
                    output ?: return@launch
                ).also { updatedNotebook ->
                    updateLocalNotebook(updatedNotebook)
                    // UIステートの更新
                    _localState.update {
                        it.copy(
                            selectedCellId = cellId,
                            focusedCellId = cellId
                        )
                    }
                }
            }
        }
    }

    fun clearCellOutput(cellId: String): Job {
        return viewModelScope.launch {
            val file = notebookFile ?: return@launch
            val notebook = _localState.value.notebook ?: return@launch
            // 出力クリアと保存
            val updatedNotebook = notebookFileUseCase.clearCellOutput(
                file,
                notebook,
                cellId
            )
            // UIステートの更新
            updateLocalNotebook(updatedNotebook)
        }
    }

    /**
     * Execute all cells in the notebook
     */
    fun executeAllCells() {
        // Cancel any ongoing execution
        // Launch new execution job
        viewModelScope.launch {
            cancelExecution().join()


            executeScope.launch {

                val notebook = _localState.value.notebook ?: return@launch

                // すべてのセルの出力をクリア
                val clearedNotebook = notebook.copy(
                    cells = notebook.cells.map { cell ->
                        cell.copy(outputs = emptyList(), executionCount = 0)
                    }
                )

                updateLocalNotebook(clearedNotebook)

                // 各セルを順番に実行
                for (cell in clearedNotebook.cells) {
                    if (cell.type == CellType.CODE) {
                        selectCellId = cell.id
                        // コードセルの実行
                        delay(100) // UIの更新を待つ
                        notebookFileUseCase.executeCell(
                            uiState.value.notebook!!,
                            cell.id,
                            environment
                        )
                        delay(200) // 実行完了を少し待つ
                    }
                }
            }
        }
    }

    /**
     * Select the cell with the given ID
     */
    fun selectCell(cellId: String) {
        _localState.update { it.copy(selectedCellId = cellId) }
    }

    /**
     * Change the type of the cell with the given ID
     */
    fun changeCellType(cellId: String, newType: CellType) {
        viewModelScope.launch {
            val file = notebookFile ?: return@launch
            val notebook = _localState.value.notebook ?: return@launch
            // タイプ変更と保存
            val updatedNotebook = notebookFileUseCase.changeCellTypeAndSave(
                file,
                notebook,
                cellId,
                newType
            )
            updateLocalNotebook(updatedNotebook)
            _localState.update {
                it.copy(
                    selectedCellId = cellId,
                    focusedCellId = cellId
                )
            }
            // UIステート更新
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
            // インデント調整
            val newTextFieldValue = autoIndent(
                uiState.value.codeCellStateMap[cellId]?.textFieldValue ?: textFieldValue,
                textFieldValue
            )
            val newText = newTextFieldValue.text
            // シンタックスハイライト用処理
            val lexer = Lexer(newText)
            val tokens = lexer.toList()
            val (annotatedStr, _) = syntaxHighLighter.highlightWithParsedData(
                newText, true, null, tokens
            )

            // Generate suggestions using SuggestionUseCase
            var suggestions = emptyList<Definition>()
            if (newTextFieldValue.selection.end > 0 && newText.isNotEmpty()) {
                val parser = Parser(Lexer(newText))

                val parsedProgram = parser.getOrElse { return@launch }.parseProgram()
                suggestions = if (parsedProgram.isRight()) {
                    // Use parsed data for better suggestions
                    suggestionUseCase.suggestWithParsedData(
                        newText,
                        newTextFieldValue.selection.end,
                        tokens,
                        parsedProgram.getOrNull()!!
                    )
                } else {
                    // Fallback when parsing fails
                    suggestionUseCase.suggestWhenFailingParse(
                        newText,
                        newTextFieldValue.selection.end
                    )
                }
            }

            // Capture file and current state
            val file = notebookFile ?: return@launch
            val currentState = _localState.value
            // Update UI code cell state and suggestions
            val newCodeMap = currentState.codeCellStateMap.toMutableMap().apply {
                this[cellId] = CodeCellState(
                    textFieldValue = newTextFieldValue,
                    annotatedString = annotatedStr
                )
            }
            val newSugMap = currentState.cellSuggestionsMap.toMutableMap().apply {
                this[cellId] = suggestions
            }
            _localState.update { state ->
                state.copy(codeCellStateMap = newCodeMap, cellSuggestionsMap = newSugMap)
            }
            // Debounce saving cell to file
            saveJobs[cellId]?.cancel()
            saveJobs[cellId] = viewModelScope.launch(Dispatchers.Default) {
                delay(SAVE_DELAY_MS)
                val saved = notebookFileUseCase.updateCellAndSave(
                    file,
                    _localState.value.notebook ?: return@launch,
                    cellId
                ) { oldCell -> oldCell.copy(source = newText.split("\n")) }
                withContext(Dispatchers.Main) {
                    updateLocalNotebook(saved)
                }
            }
        }
    }

    fun onUpdateMarkdownCell(
        cellId: String,
        newSource: List<String>
    ) {
        viewModelScope.launch {
            val file = notebookFile ?: return@launch
            // Update UI state
            val updatedNotebook = (_localState.value.notebook
                ?: return@launch).copy(cells = _localState.value.notebook!!.cells.map {
                if (it.id == cellId) it.copy(
                    source = newSource
                ) else it
            })
            updateLocalNotebook(updatedNotebook)
            // Debounce saving markdown cell
            saveJobs[cellId]?.cancel()
            saveJobs[cellId] = viewModelScope.launch(Dispatchers.Default) {
                delay(SAVE_DELAY_MS)
                notebookFileUseCase.updateCellAndSave(
                    file,
                    updatedNotebook,
                    cellId
                ) { oldCell -> oldCell.copy(source = newSource) }
            }
        }
    }

    fun handleAction(action: NotebookAction) {
        when (action) {
            is NotebookAction.SelectCell -> selectCell(action.cellId)
            is NotebookAction.ExecuteCell -> executeCell(action.cellId)
            is NotebookAction.DeleteCell -> deleteCell(action.cellId)
            is NotebookAction.ExecuteAllCells -> executeAllCells()
            is NotebookAction.CancelExecution -> cancelExecution()
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

    private data class NotebookLocalState(
        val notebook: Notebook?,
        val selectedCellId: String?,
        val codeCellStateMap: Map<String, CodeCellState>,
        val loading: Boolean,
        val focusedCellId: String?,
        val cellSuggestionsMap: Map<String, List<Definition>>
    )

    /**
     * Generate a unique ID for a new cell
     */
    @OptIn(ExperimentalUuidApi::class)
    private fun generateCellId(): String {
        return Uuid.random().toString()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun cancelExecution(): Job {
        return viewModelScope.launch(Dispatchers.Default) {
            println("!!!!!cancelExecution called stdout!!!!!")
            //watchJob?.cancelAndJoin()

            println("stdout !!! canceling executeScope !!!")
            executeScope.coroutineContext.job.cancelAndJoin()
            println("stdout !!! executeScope canceled !!!")
            executeScope.cancel()
            executeScope = CoroutineScope(Dispatchers.Default + Job())
            //if (!stdoutChannel.isEmpty) {
            stdoutChannel.close()
            stdoutChannel = Channel(capacity = 1024)
            println("stdout !!! creating new stdout channel !!!")
            executeScope.launch {
                watchStdoutChannel()
            }
            println("stdout !!! end !!!")

            pendingCount = 0
        }
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

    private suspend fun updateLocalNotebook(notebook: Notebook) {
        notebookMutex.withLock {
            _localState.update {
                it.copy(notebook = notebook)
            }
        }
    }
}
