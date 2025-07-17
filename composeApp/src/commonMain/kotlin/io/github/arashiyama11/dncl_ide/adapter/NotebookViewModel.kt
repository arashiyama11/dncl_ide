package io.github.arashiyama11.dncl_ide.adapter

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import arrow.core.getOrElse
import io.github.arashiyama11.dncl_ide.util.OutputEvent
import io.github.arashiyama11.dncl_ide.util.OutputHandler
import io.github.arashiyama11.dncl_ide.common.Action // Add this import
import io.github.arashiyama11.dncl_ide.common.AppStateStore
import io.github.arashiyama11.dncl_ide.common.AppStateStore.Companion.dispatch
import io.github.arashiyama11.dncl_ide.common.StatePermission
import io.github.arashiyama11.dncl_ide.domain.model.CursorPosition
import io.github.arashiyama11.dncl_ide.domain.model.Definition
import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.domain.model.NotebookFile
import io.github.arashiyama11.dncl_ide.domain.notebook.Cell
import io.github.arashiyama11.dncl_ide.domain.notebook.CellType
import io.github.arashiyama11.dncl_ide.domain.notebook.Notebook
import io.github.arashiyama11.dncl_ide.domain.notebook.Output
import io.github.arashiyama11.dncl_ide.domain.usecase.FileUseCase
import io.github.arashiyama11.dncl_ide.domain.usecase.NotebookFileUseCase
import io.github.arashiyama11.dncl_ide.domain.usecase.SuggestionUseCase
import io.github.arashiyama11.dncl_ide.interpreter.evaluator.EvaluatorFactory
import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.model.DnclError
import io.github.arashiyama11.dncl_ide.interpreter.model.Environment
import io.github.arashiyama11.dncl_ide.interpreter.parser.Parser
import io.github.arashiyama11.dncl_ide.util.SyntaxHighLighter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.apply
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class ExecuteRequest(val cellId: String)

data class NotebookUiState(
    val notebook: Notebook? = null,
    val selectedCellId: String? = null,
    val codeCellStateMap: ImmutableMap<String, CodeCellState> = persistentMapOf(),
    val loading: Boolean = true,
    val focusedCellId: String? = null,
    val cellSuggestionsMap: ImmutableMap<String, ImmutableList<Definition>> = persistentMapOf(),
    val fontSize: Int = 16,
    val selectedEntryPath: EntryPath? = null,
    val unsavedChanges: Boolean = false,
    val running: Boolean = false,
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
    data object DeselectCell : NotebookAction
}

@OptIn(ExperimentalTime::class)
class NotebookViewModel(
    private val fileUseCase: FileUseCase,
    private val notebookFileUseCase: NotebookFileUseCase,
    private val syntaxHighLighter: SyntaxHighLighter,
    private val suggestionUseCase: SuggestionUseCase,
    private val appStateStore: AppStateStore<StatePermission.Write>
) : ViewModel() {


    companion object {
        private const val SAVE_DELAY_MS = 500L
        private const val CACHE_EXPIRY_MS = 30000L // 30秒でキャッシュクリア
    }

    // 最適化されたローカル状態
    private val _localState = MutableStateFlow(
        NotebookLocalState(
            domainNotebook = null,
            selectedCellId = null,
            codeCellStateMap = persistentMapOf(),
            loading = true,
            focusedCellId = null,
            cellSuggestionsMap = persistentMapOf(),
            unsavedChanges = false,
            lastUpdateTime = Clock.System.now().toEpochMilliseconds()
        )
    )

    // StateFlowキャッシュ - メモリリーク防止
    private val cellStateFlowCache = mutableMapOf<String, StateFlow<Cell?>>()
    private val isSelectedFlowCache = mutableMapOf<String, StateFlow<Boolean>>()
    private val codeCellStateFlowCache = mutableMapOf<String, StateFlow<CodeCellState>>()
    private val suggestionsFlowCache = mutableMapOf<String, StateFlow<ImmutableList<Definition>>>()

    private val saveJobs = mutableMapOf<String, Job>()

    // 最適化されたuiState - 不要な変換を削減
    val uiState = combine(
        _localState.distinctUntilChangedBy {
            // ハッシュベースの高速比較
            it.hashCode()
        },
        appStateStore.state.distinctUntilChangedBy {
            Triple(it.fontSize, it.selectedEntryPath, it.running)
        }
    ) { localState, appState ->
        NotebookUiState(
            notebook = localState.domainNotebook,
            selectedCellId = localState.selectedCellId,
            codeCellStateMap = localState.codeCellStateMap,
            loading = localState.loading,
            focusedCellId = localState.focusedCellId,
            cellSuggestionsMap = localState.cellSuggestionsMap,
            fontSize = appState.fontSize,
            selectedEntryPath = appState.selectedEntryPath,
            unsavedChanges = localState.unsavedChanges,
            running = appState.running
        )
    }.stateIn(
        viewModelScope,
        started = SharingStarted.Eagerly, // Lazilyから変更でコールドスタート回避
        initialValue = NotebookUiState()
    )

    // セルIDリストの最適化 - 変更検知を高速化
    val cellIdsFlow: StateFlow<List<String>> = _localState
        .map { it.domainNotebook?.cells?.map { cell -> cell.id } ?: emptyList() }
        .distinctUntilChanged { old, new ->
            old.size == new.size && old.zip(new).all { it.first == it.second }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        uiState.distinctUntilChangedBy { it.running }.onEach {
            println("Running changed: ${it.running}")
        }.launchIn(viewModelScope)
    }

    // キャッシュされたStateFlow取得 - メモリ効率向上
    fun cellStateFlow(cellId: String): StateFlow<Cell?> {
        return cellStateFlowCache.getOrPut(cellId) {
            _localState
                .map { state -> state.domainNotebook?.cells?.find { it.id == cellId } }
                .distinctUntilChangedBy { it?.hashCode() } // オブジェクトハッシュで高速比較
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
        }
    }

    fun isSelectedFlow(cellId: String): StateFlow<Boolean> {
        return isSelectedFlowCache.getOrPut(cellId) {
            _localState
                .map { it.selectedCellId == cellId }
                .distinctUntilChanged()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
        }
    }

    fun codeCellStateFlow(cellId: String): StateFlow<CodeCellState> {
        return codeCellStateFlowCache.getOrPut(cellId) {
            _localState
                .map {
                    it.codeCellStateMap[cellId] ?: CodeCellState()
                }
                .distinctUntilChangedBy { "${it.textFieldValue.text}:${it.annotatedString.text}" }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CodeCellState())
        }
    }

    fun suggestionsFlow(cellId: String): StateFlow<ImmutableList<Definition>> {
        return suggestionsFlowCache.getOrPut(cellId) {
            _localState
                .map { it.cellSuggestionsMap[cellId] ?: persistentListOf() }
                .distinctUntilChangedBy { it.size }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), persistentListOf())
        }
    }

    // フォントサイズFlowの最適化
    val fontSizeFlow: StateFlow<Int> = appStateStore.state
        .map { it.fontSize }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 16)

    val errorChannel = Channel<String>()

    private val saveReqChannel = Channel<Unit>(capacity = 64)

    

    private var isExecuting = false

    private val notebookMutex = Mutex()

    private var notebookFile: NotebookFile? = null
    private var selectCellId: String? = null
    private var executeScope: CoroutineScope = CoroutineScope(Dispatchers.Default + Job())
    private lateinit var outputHandler: OutputHandler

    private lateinit var environment: Environment
    private var started = false

    init {
        // 定期的なキャッシュクリーンアップ
        viewModelScope.launch {
            while (isActive) {
                delay(CACHE_EXPIRY_MS)
                cleanupExpiredCaches()
            }
        }
    }

    private fun cleanupExpiredCaches() {
        val currentTime = Clock.System.now().toEpochMilliseconds()
        val lastUpdate = _localState.value.lastUpdateTime

        if (currentTime - lastUpdate > CACHE_EXPIRY_MS) {
            // 使用されていないキャッシュをクリア
            cellStateFlowCache.clear()
            isSelectedFlowCache.clear()
            codeCellStateFlowCache.clear()
            suggestionsFlowCache.clear()
        }
    }


    context(scope: CoroutineScope)
    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    fun onStart() {
        if (started) return
        started = true

        outputHandler = OutputHandler(executeScope) { outputs ->
            viewModelScope.launch(Dispatchers.Main) {
                notebookMutex.withLock {
                    val currentState = _localState.value
                    val notebook = currentState.domainNotebook ?: return@withLock
                    var newNotebook = notebook
                    outputs.forEach { (cellId, outputString) ->
                        if (cellId == null) return@forEach
                        val output = Output(
                            outputType = "stream",
                            name = "stdout",
                            text = persistentListOf(processOutputText(outputString))
                        )
                        newNotebook = notebookFileUseCase.modifyNotebookOutput(
                            newNotebook,
                            cellId,
                            persistentListOf(output)
                        )
                    }
                    _localState.update {
                        it.copy(domainNotebook = newNotebook)
                    }
                }
            }
        }

        appStateStore.state.distinctUntilChangedBy { it.selectedEntryPath }
            .onEach { appState ->
                val entryPath = appState.selectedEntryPath

                saveNotebook()

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

                            val initialCodeCellStateMap = notebook.cells
                                .filter { it.type == CellType.CODE }
                                .associate { cell ->
                                    val text = cell.source.joinToString("\n")
                                    val lexer = Lexer(text)
                                    val tokens = lexer.toList()
                                    val (annotatedString, _) = syntaxHighLighter.highlightWithParsedData(
                                        text, true, null, tokens
                                    )
                                    cell.id to CodeCellState(
                                        textFieldValue = TextFieldValue(text),
                                        annotatedString = annotatedString
                                    )
                                }.toImmutableMap()

                            notebookMutex.withLock {
                                _localState.update {
                                    it.copy(
                                        domainNotebook = notebook,
                                        codeCellStateMap = initialCodeCellStateMap,
                                        loading = false
                                    )
                                }
                            }
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
                        if (outputStr.trim() == "null") return@createBuiltInFunctionEnvironment
                        outputHandler.append(selectCellId, outputStr)
                    }, onClear = {
                        outputHandler.clear(selectCellId)
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

        viewModelScope.launch {
            saveReqChannel.consumeAsFlow().collectLatest {
                delay(SAVE_DELAY_MS)
                val file = notebookFile ?: return@collectLatest
                val content =
                    with(notebookFileUseCase) { _localState.value.domainNotebook?.toFileContent() }
                        ?: return@collectLatest
                notebookFileUseCase.saveNotebookFile(file, content, CursorPosition(0)).join()
                _localState.update { it.copy(unsavedChanges = false) }
            }
        }

        
    }

    /**
     * Add a new cell of the specified type after the cell with the given ID
     */
    fun addCellAfter(afterCellId: String?, cellType: CellType) {
        viewModelScope.launch {
            val file = notebookFile ?: return@launch
            val cellId = generateCellId()
            val defaultSource =
                if (cellType == CellType.CODE) listOf("1+2") else listOf("## 新しいセル")
            val newCell = notebookFileUseCase.createCell(
                id = cellId,
                type = cellType,
                source = defaultSource.toImmutableList(),
                executionCount = if (cellType == CellType.CODE) 0 else null,
                outputs = if (cellType == CellType.CODE) persistentListOf() else null
            )

            notebookMutex.withLock {
                val currentState = _localState.value
                val notebook = currentState.domainNotebook ?: return@withLock
                val newNotebook = notebookFileUseCase.insertCellAndSave(
                    file,
                    notebook,
                    newCell,
                    afterCellId
                )

                val newCodeCellState = if (cellType == CellType.CODE) {
                    val text = newCell.source.joinToString("\n")
                    val (annotatedString, _) = syntaxHighLighter.highlightWithParsedData(
                        text,
                        true,
                        null,
                        emptyList()
                    )
                    CodeCellState(
                        textFieldValue = TextFieldValue(text),
                        annotatedString = annotatedString
                    )
                } else null

                _localState.update {
                    val newMap = if (newCodeCellState != null) {
                        it.codeCellStateMap + (cellId to newCodeCellState)
                    } else {
                        it.codeCellStateMap
                    }
                    it.copy(
                        domainNotebook = newNotebook,
                        selectedCellId = cellId,
                        codeCellStateMap = newMap.toImmutableMap()
                    )
                }
            }
        }
    }

    /**
     * Delete the cell with the given ID
     */
    fun deleteCell(cellId: String) {
        viewModelScope.launch {
            val file = notebookFile ?: return@launch
            notebookMutex.withLock {
                val currentState = _localState.value
                val notebook = currentState.domainNotebook ?: return@withLock

                val newNotebook = notebookFileUseCase.deleteCellAndSave(
                    file,
                    notebook,
                    cellId
                )

                val originalCells = notebook.cells
                val cellIndex = originalCells.indexOfFirst { it.id == cellId }
                val nextSelected = when {
                    newNotebook.cells.isEmpty() -> null
                    cellIndex > 0 -> originalCells[cellIndex - 1].id
                    newNotebook.cells.isNotEmpty() -> newNotebook.cells.first().id
                    else -> null
                }

                val updatedStateMap = currentState.codeCellStateMap - cellId
                _localState.update {
                    it.copy(
                        domainNotebook = newNotebook,
                        selectedCellId = nextSelected,
                        codeCellStateMap = updatedStateMap.toImmutableMap()
                    )
                }
            }
        }
    }

    /**
     * Execute the cell with the given ID
     * キューシステムを使用して堅牢な実行を保証
     */
    fun executeCell(cellId: String) {
        viewModelScope.launch {
            executeCellInternal(cellId)
        }
    }

    /**
     * Execute all cells in the notebook
     * 全てのコードセルを個別にキューに追加して順次実行
     */
    fun executeAllCells() {
        viewModelScope.launch {
            val currentState = _localState.value
            val notebook = currentState.domainNotebook ?: return@launch

            // まず全てのセルの出力をクリア
            val clearedNotebook = notebook.copy(
                cells = notebook.cells.map { cell ->
                    if (cell.type == CellType.CODE) {
                        cell.copy(outputs = persistentListOf(), executionCount = 0)
                    } else {
                        cell
                    }
                }.toImmutableList()
            )

            notebookMutex.withLock {
                _localState.update { it.copy(domainNotebook = clearedNotebook) }
            }

            // 各コードセルを順次実行
            executeScope.launch {
                clearedNotebook.cells
                    .filter { it.type == CellType.CODE }
                    .forEach { cell ->
                        executeCellInternal(cell.id)
                    }
            }
        }
    }

    fun clearCellOutput(cellId: String): Job {
        return viewModelScope.launch {
            val file = notebookFile ?: return@launch
            notebookMutex.withLock {
                val currentState = _localState.value
                val notebook = currentState.domainNotebook ?: return@withLock
                val newNotebook = notebookFileUseCase.clearCellOutput(
                    file,
                    notebook,
                    cellId
                )
                _localState.update {
                    it.copy(domainNotebook = newNotebook)
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
            notebookMutex.withLock {
                val currentState = _localState.value
                val notebook = currentState.domainNotebook ?: return@withLock
                val updatedNotebook = notebookFileUseCase.changeCellTypeAndSave(
                    file,
                    notebook,
                    cellId,
                    newType
                )

                if (newType == CellType.CODE) {
                    val cell = updatedNotebook.cells.first { it.id == cellId }
                    val text = cell.source.joinToString("\n")
                    val (annotatedString, _) =
                        syntaxHighLighter.highlightWithParsedData(text, true, null, emptyList())
                    val newCodeCellState = CodeCellState(
                        textFieldValue = TextFieldValue(text),
                        annotatedString = annotatedString
                    )
                    _localState.update {
                        it.copy(
                            domainNotebook = updatedNotebook,
                            selectedCellId = cellId,
                            focusedCellId = cellId,
                            codeCellStateMap = (it.codeCellStateMap + (cellId to newCodeCellState)).toImmutableMap()
                        )
                    }
                } else {
                    _localState.update {
                        it.copy(
                            domainNotebook = updatedNotebook,
                            selectedCellId = cellId,
                            focusedCellId = cellId,
                            codeCellStateMap = (it.codeCellStateMap - cellId).toImmutableMap()
                        )
                    }
                }
            }
        }
    }

    //existsChangeだめっぽい。確定とかの話
    fun onUpdateCodeCell(
        cellId: String,
        textFieldValue: TextFieldValue,
        existsChange: Boolean? = null//uiState.value.codeCellStateMap[cellId]?.textFieldValue?.text != textFieldValue.text
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
                val parser: Either<DnclError, Parser> = Parser(Lexer(newText))

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

            notebookMutex.withLock {
                val currentState = _localState.value
                val notebook = currentState.domainNotebook ?: return@withLock

                val newNotebook = notebookFileUseCase.modifyNotebookCell(
                    notebook,
                    cellId,
                ) { oldCell ->
                    oldCell.copy(source = newText.split("\n").toImmutableList())
                }

                val newCodeMap = currentState.codeCellStateMap.toMutableMap().apply {
                    this[cellId] = CodeCellState(
                        textFieldValue = newTextFieldValue,
                        annotatedString = annotatedStr
                    )
                }.toImmutableMap()
                val newSugMap = currentState.cellSuggestionsMap.toMutableMap().apply {
                    this[cellId] = suggestions.toImmutableList()
                }.toImmutableMap()

                _localState.update { state ->
                    state.copy(
                        domainNotebook = newNotebook,
                        codeCellStateMap = newCodeMap,
                        cellSuggestionsMap = newSugMap,
                        unsavedChanges = existsChange
                            ?: (state.unsavedChanges || newText != (notebook.cells.firstOrNull { it.id == cellId }?.source?.joinToString(
                                "\n"
                            ))),
                    )
                }
            }
            // Debounce saving cell to file
            saveJobs[cellId]?.cancel()
            saveJobs[cellId] = viewModelScope.launch(Dispatchers.Default) {
                //delay(SAVE_DELAY_MS)
                /*updateLocalNotebook { nb ->
                    notebookFileUseCase.modifyNotebookCell(
                        nb,
                        cellId,
                    ) { oldCell ->
                        oldCell.copy(source = newText.split("\n"))
                    }
                }*/
            }
        }
    }

    fun onUpdateMarkdownCell(
        cellId: String,
        newSource: List<String>,
        existsChange: Boolean = _localState.value.domainNotebook?.cells?.firstOrNull { it.id == cellId }?.source != newSource
    ) {
        viewModelScope.launch {
            notebookMutex.withLock {
                val currentState = _localState.value
                val notebook = currentState.domainNotebook ?: return@withLock
                val newNotebook = notebookFileUseCase.modifyNotebookCell(
                    notebook,
                    cellId,
                ) { oldCell ->
                    oldCell.copy(source = newSource.toImmutableList(), type = CellType.MARKDOWN)
                }
                _localState.update {
                    it.copy(
                        domainNotebook = newNotebook,
                        codeCellStateMap = it.codeCellStateMap,
                        unsavedChanges = existsChange || it.unsavedChanges
                    )
                }
            }

            saveJobs[cellId]?.cancel()
            saveJobs[cellId] = viewModelScope.launch(Dispatchers.Default) {
                /*  delay(SAVE_DELAY_MS)
                  notebookFileUseCase.modifyNotebookCell(
                      _localState.value.notebook ?: return@launch,
                      cellId
                  ) { oldCell -> oldCell.copy(source = newSource) }*/
            }
        }
    }

    fun saveNotebook() = saveReqChannel.trySend(Unit)/*viewModelScope.launch {
        val file = notebookFile ?: return@launch
        val content = with(notebookFileUseCase) { _localState.value.notebook?.toFileContent() }
            ?: return@launch
        notebookFileUseCase.saveNotebookFile(file, content, CursorPosition(0)).join()
        _localState.update { it.copy(unsavedChanges = false) }
    }*/

    fun handleAction(action: NotebookAction) {
        when (action) {
            is NotebookAction.SelectCell -> selectCell(action.cellId)
            is NotebookAction.ExecuteCell -> executeCell(action.cellId)
            is NotebookAction.DeleteCell -> deleteCell(action.cellId)
            is NotebookAction.ExecuteAllCells -> executeAllCells()
            is NotebookAction.CancelExecution -> {
                cancelExecution()
                saveNotebook()
            }

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

            is NotebookAction.DeselectCell -> deselectCell()
        }
    }

    private fun deselectCell() {
        _localState.update { it.copy(selectedCellId = null) }
    }

    private data class NotebookLocalState(
        val domainNotebook: Notebook?,
        val selectedCellId: String?,
        val codeCellStateMap: ImmutableMap<String, CodeCellState>,
        val loading: Boolean,
        val focusedCellId: String?,
        val cellSuggestionsMap: ImmutableMap<String, ImmutableList<Definition>>,
        val unsavedChanges: Boolean,
        val lastUpdateTime: Long = 0L // 最後の更新時刻
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
            outputHandler.stop()

            val currentExecuteScopeJob = executeScope.coroutineContext.job
            currentExecuteScopeJob.cancelAndJoin()
            executeScope = CoroutineScope(Dispatchers.Default + Job())
            outputHandler = OutputHandler(executeScope) { outputs ->
                viewModelScope.launch(Dispatchers.Main) {
                    notebookMutex.withLock {
                        val currentState = _localState.value
                        val notebook = currentState.domainNotebook ?: return@withLock
                        var newNotebook = notebook
                        outputs.forEach { (cellId, outputString) ->
                            if (cellId == null) return@forEach
                            val output = Output(
                                outputType = "stream",
                                name = "stdout",
                                text = persistentListOf(processOutputText(outputString))
                            )
                            newNotebook = notebookFileUseCase.modifyNotebookOutput(
                                newNotebook,
                                cellId,
                                persistentListOf(output)
                            )
                        }
                        _localState.update {
                            it.copy(domainNotebook = newNotebook)
                        }
                    }
                }
            }

            // Only set running to false if there was an active job to cancel
            if (currentExecuteScopeJob.isCancelled) {
                appStateStore.dispatch(Action.SetRunning(false))
            }
        }
    }

    

    /**
     * 単一セルの実際の実行処理（内部用）
     */
    private suspend fun executeCellInternal(cellId: String) {
        selectCellId = cellId
        clearCellOutput(cellId).join()
        cancelExecution().join()

        executeScope.launch {
            saveNotebook()
            delay(100) // 出力クリアを待つ
            isExecuting = true
            appStateStore.dispatch(Action.SetRunning(true))

            val output = notebookFileUseCase.executeCell(
                _localState.value.domainNotebook!!, cellId, environment
            )

            notebookMutex.withLock {
                val processedOutput = output.copy(text = output.text?.let {
                    persistentListOf(
                        processOutputText(
                            it.joinToString("\n")
                        )
                    )
                })
                val newNotebook = notebookFileUseCase.modifyNotebookOutput(
                    _localState.value.domainNotebook!!,
                    cellId,
                    persistentListOf(processedOutput)
                )
                _localState.update {
                    it.copy(domainNotebook = newNotebook)
                }
            }

            outputHandler.commitFrame(cellId)

            delay(200) // 出力のflushを実装したら不要になるはず。
            isExecuting = false
            appStateStore.dispatch(Action.SetRunning(false))
        }.join()
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

    /**
     * 出力文字列を処理して、末尾の空行とnullを適切に処理する
     */
    private fun processOutputText(text: String): String {
        if (text.isEmpty()) return text

        // 行に分割
        val lines = text.lines().toMutableList()

        // 最後が空行なら削除
        while (lines.isNotEmpty() && lines.last().trim().isEmpty()) {
            lines.removeAt(lines.size - 1)
        }

        // 最後の行がnullで、かつ他に内容がある場合はnullを削除
        if (lines.size > 1 && lines.last().trim() == "null") {
            lines.removeAt(lines.size - 1)
        }

        return lines.joinToString("\n")
    }
}

fun <T> Flow<T>.zipWithPrevious(): Flow<Pair<T?, T>> = flow {
    var previous: T? = null
    collect { value ->
        emit(previous to value)
        previous = value
    }
}