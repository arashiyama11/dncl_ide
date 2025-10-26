package io.github.arashiyama11.dncl_ide.adapter

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.arashiyama11.dncl_ide.common.Action
import io.github.arashiyama11.dncl_ide.common.AppStateStore
import io.github.arashiyama11.dncl_ide.common.AppStateStore.Companion.dispatch
import io.github.arashiyama11.dncl_ide.common.StatePermission
import io.github.arashiyama11.dncl_ide.domain.model.CursorPosition
import io.github.arashiyama11.dncl_ide.domain.model.Definition
import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.domain.model.NotebookFile
import io.github.arashiyama11.dncl_ide.domain.notebook.CellType
import io.github.arashiyama11.dncl_ide.domain.notebook.Notebook
import io.github.arashiyama11.dncl_ide.domain.notebook.Output
import io.github.arashiyama11.dncl_ide.domain.usecase.FileUseCase
import io.github.arashiyama11.dncl_ide.domain.usecase.NotebookFileUseCase
import io.github.arashiyama11.dncl_ide.editor.lsp.LanguageFeatureProvider
import io.github.arashiyama11.dncl_ide.editor.lsp.LanguageServerDocument
import io.github.arashiyama11.dncl_ide.interpreter.api.Stdout
import io.github.arashiyama11.dncl_ide.interpreter.evaluator.EvaluatorFactory
import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import io.github.arashiyama11.dncl_ide.interpreter.model.DnclObject
import io.github.arashiyama11.dncl_ide.interpreter.model.Environment
import io.github.arashiyama11.dncl_ide.language_server.util.calculatePosition
import io.github.arashiyama11.dncl_ide.util.OutputHandler
import io.github.arashiyama11.dncl_ide.util.SyntaxHighLighter
import io.github.arashiyama11.dncl_ide.util.toFileUri
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    val cellIds: List<String> = emptyList()
)

data class CodeCellState(
    val textFieldValue: TextFieldValue = TextFieldValue(
        text = "",
        selection = TextRange(0)
    ),
    val annotatedString: AnnotatedString = AnnotatedString(""),
    val highlightRevision: Long = 0L
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
    private val appStateStore: AppStateStore<StatePermission.Write>,
    private val languageFeatureProvider: LanguageFeatureProvider
) : ViewModel() {


    companion object {
        private const val SAVE_DELAY_MS = 500L
        private const val COMPLETION_DEBOUNCE_MS = 100L
    }

    private data class NotebookState(
        val notebookFile: NotebookFile? = null,
        val domainNotebook: Notebook? = null,
        val selectedCellId: String? = null,
        val codeCellStateMap: ImmutableMap<String, CodeCellState> = persistentMapOf(),
        val loading: Boolean = true,
        val focusedCellId: String? = null,
        val cellSuggestionsMap: ImmutableMap<String, ImmutableList<Definition>> = persistentMapOf(),
        val unsavedChanges: Boolean = false,
    )

    // 単一の状態ソース
    private val _state = MutableStateFlow(NotebookState())

    private val saveJobs = mutableMapOf<String, Job>()
    private val completionJobs = mutableMapOf<String, Job>()

    // UI状態
    val uiState = combine(
        _state,
        appStateStore.state.distinctUntilChangedBy {
            Triple(it.uiConfig.fontSize, it.selectedEntryPath, it.running)
        }
    ) { state, appState ->
        NotebookUiState(
            notebook = state.domainNotebook,
            selectedCellId = state.selectedCellId,
            codeCellStateMap = state.codeCellStateMap,
            loading = state.loading,
            focusedCellId = state.focusedCellId,
            cellSuggestionsMap = state.cellSuggestionsMap,
            fontSize = appState.uiConfig.fontSize,
            selectedEntryPath = appState.selectedEntryPath,
            unsavedChanges = state.unsavedChanges,
            running = appState.running,
            cellIds = state.domainNotebook?.cells?.map { it.id } ?: emptyList()
        )
    }.stateIn(
        viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = NotebookUiState()
    )

    val errorChannel = Channel<String>()

    private val saveReqChannel = Channel<Unit>(capacity = 64)

    private val executionRequestChannel = Channel<ExecuteRequest>(Channel.UNLIMITED)

    private var executeScope: CoroutineScope = CoroutineScope(Dispatchers.Default + Job())
    private lateinit var outputHandler: OutputHandler

    private lateinit var environment: Environment
    private var started = false
    private lateinit var _currentStdout: Stdout

    private val openedCellUris = mutableSetOf<String>()
    private val notebookDocumentMutex = Mutex()

    private inner class DynamicStdout() : Stdout {
        override suspend fun append(text: String) {
            _currentStdout.append(text)
        }

        override suspend fun flush() {
            _currentStdout.flush()
        }

        override suspend fun clear() {
            _currentStdout.clear()
        }

        override suspend fun commitFrame() {
            _currentStdout.commitFrame()
        }

        override suspend fun replace(text: String) {
            _currentStdout.replace(text)
        }
    }

    init {
        uiState.distinctUntilChangedBy { it.running }.onEach {
            println("Running changed: ${it.running}")
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            saveReqChannel.consumeAsFlow().collectLatest {
                delay(SAVE_DELAY_MS)
                val state = _state.value
                val file = state.notebookFile ?: return@collectLatest
                val content =
                    with(notebookFileUseCase) { state.domainNotebook?.toFileContent() }
                        ?: return@collectLatest
                notebookFileUseCase.saveNotebookFile(file, content, CursorPosition(0)).join()
                _state.update { it.copy(unsavedChanges = false) }
            }
        }

        viewModelScope.launch(Dispatchers.Default) {
            executionRequestChannel.consumeAsFlow()
                .collect { request ->
                    executeCellInternal(request.cellId)
                }
        }
    }


    context(scope: CoroutineScope)
    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    fun onStart() {
        if (started) return
        started = true

        outputHandler = OutputHandler(CoroutineScope(Dispatchers.Default)) { outputs ->
            viewModelScope.launch(Dispatchers.Main) {
                _state.update { currentState ->
                    var newNotebook = currentState.domainNotebook ?: return@update currentState
                    outputs.forEach { (cellId, outputString) ->
                        if (cellId != null) {
                            val output = Output(
                                outputType = "stream",
                                name = "stdout",
                                text = persistentListOf(outputString)
                            )
                            val cellOutputs =
                                newNotebook.cells.firstOrNull { it.id == cellId }?.outputs
                                    ?: persistentListOf()

                            newNotebook = if (cellOutputs.lastOrNull()?.outputType == "stream") {
                                val lastOutput = cellOutputs.last()
                                val updatedText = output.text.orEmpty()
                                notebookFileUseCase.modifyNotebookOutput(
                                    newNotebook,
                                    cellId,
                                    (cellOutputs.dropLast(1) + lastOutput.copy(text = updatedText.toPersistentList())).toPersistentList()
                                )
                            } else {
                                notebookFileUseCase.modifyNotebookOutput(
                                    newNotebook,
                                    cellId,
                                    (cellOutputs + output).toPersistentList()
                                )
                            }
                        }
                    }
                    currentState.copy(domainNotebook = newNotebook)
                }
            }
        }
        _currentStdout = outputHandler.stdout

        appStateStore.state.distinctUntilChangedBy { it.selectedEntryPath }
            .onEach { appState ->
                val entryPath = appState.selectedEntryPath

                saveNotebook()

                coroutineScope {
                    closeAllNotebookCellDocuments()
                    if (entryPath?.isNotebookFile() == true) {
                        val notebookFile = fileUseCase.getEntryByPath(entryPath)
                        if (notebookFile is NotebookFile) {
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
                                        annotatedString = annotatedString,
                                        highlightRevision = 1L
                                    )
                                }.toImmutableMap()

                            _state.update {
                                it.copy(
                                    notebookFile = notebookFile,
                                    domainNotebook = notebook,
                                    codeCellStateMap = initialCodeCellStateMap,
                                    cellSuggestionsMap = persistentMapOf(),
                                    loading = false
                                )
                            }
                        } else {
                            errorChannel.send("ノートブックを開くことができません: $notebookFile")
                        }
                    } else {
                        _state.update {
                            it.copy(
                                notebookFile = null,
                                domainNotebook = null,
                                codeCellStateMap = persistentMapOf(),
                                cellSuggestionsMap = persistentMapOf(),
                                loading = false
                            )
                        }
                    }
                }
            }.launchIn(viewModelScope)

        viewModelScope.launch(Dispatchers.Default) {
            environment = Environment(
                EvaluatorFactory.createBuiltInFunctionEnvironment(
                    stdout = DynamicStdout(),
                    onImport = { importPath ->
                        println("Importing from: $importPath")
                        _state.value.notebookFile?.let { file ->
                            with(notebookFileUseCase) {
                                importAndExecute(
                                    file,
                                    importPath,
                                    environment
                                )
                            }.also { println("Import completed") }
                        } ?: run {
                            errorChannel.send("ノートブックファイルが読み込まれていません。インポートできません。")
                            DnclObject.RuntimeError(
                                "ノートブックファイルが読み込まれていません。インポートできません。",
                                AstNode.Identifier("", 0..0)
                            )
                        }
                    }
                ))
        }
    }

    private fun clearCellOutput(cellId: String) {
        val domainNotebook = _state.value.domainNotebook ?: return
        viewModelScope.launch {
            outputHandler.stdoutFor(cellId).clear()
        }
        val nb = notebookFileUseCase.modifyNotebookOutput(
            domainNotebook,
            cellId,
            persistentListOf()
        )
        _state.update {
            it.copy(
                domainNotebook = nb,
                unsavedChanges = true
            )
        }

    }

    fun saveNotebook() = saveReqChannel.trySend(Unit)

    fun handleAction(action: NotebookAction) {
        when (action) {
            is NotebookAction.SelectCell -> _state.update { it.copy(selectedCellId = action.cellId) }
            is NotebookAction.ExecuteCell -> viewModelScope.launch {
                executionRequestChannel.send(
                    ExecuteRequest(action.cellId)
                )
            }

            is NotebookAction.DeleteCell -> {
                viewModelScope.launch {
                    val state = _state.value
                    val file = state.notebookFile ?: return@launch
                    _state.update { currentState ->
                        val notebook = currentState.domainNotebook ?: return@update currentState

                        val newNotebook = notebookFileUseCase.deleteCellAndSave(
                            file,
                            notebook,
                            action.cellId
                        )

                        val originalCells = notebook.cells
                        val cellIndex = originalCells.indexOfFirst { it.id == action.cellId }
                        val nextSelected = when {
                            newNotebook.cells.isEmpty() -> null
                            cellIndex > 0 -> originalCells[cellIndex - 1].id
                            newNotebook.cells.isNotEmpty() -> newNotebook.cells.first().id
                            else -> null
                        }

                        val updatedStateMap = currentState.codeCellStateMap - action.cellId
                        currentState.copy(
                            domainNotebook = newNotebook,
                            selectedCellId = nextSelected,
                            codeCellStateMap = updatedStateMap.toImmutableMap()
                        )
                    }
                    closeNotebookCellDocument(file.path, action.cellId)
                    completionJobs.remove(action.cellId)?.cancel()
                }
            }

            is NotebookAction.ExecuteAllCells -> {
                viewModelScope.launch {
                    _state.update { currentState ->
                        val notebook = currentState.domainNotebook ?: return@update currentState
                        val clearedNotebook = notebook.copy(
                            cells = notebook.cells.map { cell ->
                                if (cell.type == CellType.CODE) {
                                    cell.copy(outputs = persistentListOf(), executionCount = 0)
                                } else {
                                    cell
                                }
                            }.toImmutableList()
                        )

                        clearedNotebook.cells
                            .filter { it.type == CellType.CODE }
                            .forEach {
                                executionRequestChannel.trySend(ExecuteRequest(it.id))
                            }
                        currentState.copy(domainNotebook = clearedNotebook)
                    }
                }
            }

            is NotebookAction.CancelExecution -> {
                cancelExecution()
                saveNotebook()
            }

            is NotebookAction.AddCellAfter -> {
                viewModelScope.launch {
                    val file = _state.value.notebookFile ?: return@launch
                    val cellId = generateCellId()
                    val defaultSource =
                        if (action.cellType == CellType.CODE) listOf("1+2") else listOf("## 新しいセル")
                    val newCell = notebookFileUseCase.createCell(
                        id = cellId,
                        type = action.cellType,
                        source = defaultSource.toImmutableList(),
                        executionCount = if (action.cellType == CellType.CODE) 0 else null,
                        outputs = if (action.cellType == CellType.CODE) persistentListOf() else null
                    )

                    _state.update { currentState ->
                        val notebook = currentState.domainNotebook ?: return@update currentState
                        val newNotebook = notebookFileUseCase.insertCellAndSave(
                            file,
                            notebook,
                            newCell,
                            action.cellId
                        )

                        val newCodeCellState = if (action.cellType == CellType.CODE) {
                            val text = newCell.source.joinToString("\n")
                            val (annotatedString, _) = syntaxHighLighter.highlightWithParsedData(
                                text,
                                true,
                                null,
                                emptyList()
                            )
                            CodeCellState(
                                textFieldValue = TextFieldValue(text),
                                annotatedString = annotatedString,
                                highlightRevision = 1L
                            )
                        } else null

                        val newMap = if (newCodeCellState != null) {
                            currentState.codeCellStateMap + (cellId to newCodeCellState)
                        } else {
                            currentState.codeCellStateMap
                        }
                        currentState.copy(
                            domainNotebook = newNotebook,
                            selectedCellId = cellId,
                            codeCellStateMap = newMap.toImmutableMap()
                        )
                    }
                }
            }

            is NotebookAction.ChangeCellType -> {
                viewModelScope.launch {
                    val state = _state.value
                    val file = state.notebookFile ?: return@launch
                    _state.update { currentState ->
                        val notebook = currentState.domainNotebook ?: return@update currentState
                        val updatedNotebook = notebookFileUseCase.changeCellTypeAndSave(
                            file,
                            notebook,
                            action.cellId,
                            action.cellType
                        )

                        if (action.cellType == CellType.CODE) {
                            val cell = updatedNotebook.cells.first { it.id == action.cellId }
                            val text = cell.source.joinToString("\n")
                            val (annotatedString, _)
                                    = syntaxHighLighter.highlightWithParsedData(
                                text,
                                true,
                                null,
                                emptyList()
                            )
                            val newCodeCellState = CodeCellState(
                                textFieldValue = TextFieldValue(text),
                                annotatedString = annotatedString,
                                highlightRevision = 1L
                            )
                            currentState.copy(
                                domainNotebook = updatedNotebook,
                                selectedCellId = action.cellId,
                                focusedCellId = action.cellId,
                                codeCellStateMap = (currentState.codeCellStateMap + (action.cellId to newCodeCellState)).toImmutableMap()
                            )
                        } else {
                            currentState.copy(
                                domainNotebook = updatedNotebook,
                                selectedCellId = action.cellId,
                                focusedCellId = action.cellId,
                                codeCellStateMap = (currentState.codeCellStateMap - action.cellId).toImmutableMap()
                            )
                        }
                    }
                    if (action.cellType != CellType.CODE) {
                        closeNotebookCellDocument(file.path, action.cellId)
                        completionJobs.remove(action.cellId)?.cancel()
                    }
                }
            }

            is NotebookAction.UpdateCodeCell -> {
                viewModelScope.launch(Dispatchers.Default) {
                    val newTextFieldValue = autoIndent(
                        uiState.value.codeCellStateMap[action.cellId]?.textFieldValue
                            ?: action.textFieldValue,
                        action.textFieldValue
                    )
                    val newText = newTextFieldValue.text
                    val lexer = Lexer(newText)
                    val tokens = lexer.toList()
                    val (annotatedStr, _) = syntaxHighLighter.highlightWithParsedData(
                        newText, true, null, tokens
                    )

                    val notebookFile = _state.value.notebookFile

                    _state.update { currentState ->
                        val notebook = currentState.domainNotebook ?: return@update currentState

                        val newNotebook = notebookFileUseCase.modifyNotebookCell(
                            notebook,
                            action.cellId,
                        ) { oldCell ->
                            oldCell.copy(source = newText.split('\n').toImmutableList())
                        }

                        val previousRevision = currentState.codeCellStateMap[action.cellId]?.highlightRevision ?: 0L
                        val newCodeMap = currentState.codeCellStateMap.toMutableMap().apply {
                            this[action.cellId] = CodeCellState(
                                textFieldValue = newTextFieldValue,
                                annotatedString = annotatedStr,
                                highlightRevision = previousRevision + 1
                            )
                        }.toImmutableMap()

                        currentState.copy(
                            domainNotebook = newNotebook,
                            codeCellStateMap = newCodeMap,
                            unsavedChanges = (currentState.unsavedChanges || newText != (notebook.cells.firstOrNull { it.id == action.cellId }?.source?.joinToString(
                                "\n"
                            ))),
                        )
                    }
                    if (notebookFile != null) {
                        val notebookPath = notebookFile.path
                        completionJobs[action.cellId]?.cancel()
                        completionJobs[action.cellId] = viewModelScope.launch(Dispatchers.Default) {
                            delay(COMPLETION_DEBOUNCE_MS)
                            val suggestions = requestNotebookCompletions(
                                notebookPath,
                                action.cellId,
                                newText,
                                newTextFieldValue.selection.end
                            )
                            _state.update { currentState ->
                                if (!currentState.codeCellStateMap.containsKey(action.cellId)) {
                                    return@update currentState
                                }
                                val newSugMap = currentState.cellSuggestionsMap.toMutableMap().apply {
                                    this[action.cellId] = suggestions.toImmutableList()
                                }.toImmutableMap()
                                currentState.copy(cellSuggestionsMap = newSugMap)
                            }
                        }
                    } else {
                        completionJobs.remove(action.cellId)?.cancel()
                    }
                    saveJobs[action.cellId]?.cancel()
                    saveJobs[action.cellId] = viewModelScope.launch(Dispatchers.Default) {
                    }
                }
            }

            is NotebookAction.UpdateMarkdownCell -> {
                viewModelScope.launch {
                    _state.update { currentState ->
                        val notebook = currentState.domainNotebook ?: return@update currentState
                        val newNotebook = notebookFileUseCase.modifyNotebookCell(
                            notebook,
                            action.cellId,
                        ) { oldCell ->
                            oldCell.copy(
                                source = action.source.toImmutableList(),
                                type = CellType.MARKDOWN
                            )
                        }
                        currentState.copy(
                            domainNotebook = newNotebook,
                            codeCellStateMap = currentState.codeCellStateMap,
                            unsavedChanges = (currentState.unsavedChanges || _state.value.domainNotebook?.cells?.firstOrNull { it.id == action.cellId }?.source != action.source)
                        )
                    }

                    saveJobs[action.cellId]?.cancel()
                }
            }

            is NotebookAction.DeselectCell -> _state.update { it.copy(selectedCellId = null) }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun generateCellId(): String {
        return Uuid.random().toString()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun cancelExecution(): Job {
        return viewModelScope.launch(Dispatchers.Default) {
            val currentExecuteScopeJob = executeScope.coroutineContext.job
            currentExecuteScopeJob.cancelAndJoin()
            executeScope = CoroutineScope(Dispatchers.Default + Job())
            if (currentExecuteScopeJob.isCancelled) {
                appStateStore.dispatch(Action.SetRunning(false))
            }
        }
    }

    private suspend fun executeCellInternal(cellId: String) {
        println("Executing cell: $cellId")
        _currentStdout = outputHandler.stdoutFor(cellId)
        _state.update { it.copy(selectedCellId = cellId) }
        clearCellOutput(cellId)
        cancelExecution().join()

        saveNotebook()
        delay(100)
        appStateStore.dispatch(Action.SetRunning(true))

        val currentNotebook = _state.value.domainNotebook
        if (currentNotebook == null) {
            errorChannel.send("ノートブックが読み込まれていません。")
            appStateStore.dispatch(Action.SetRunning(false))
            return
        }

        val output = notebookFileUseCase.executeCell(
            currentNotebook, cellId, environment
        )

        if (output.outputType == "stream") {
            val outputText = output.text.orEmpty().joinToString("\n")
            if (outputText != "null") {
                _currentStdout.append(output.text.orEmpty().joinToString("\n"))
            }
        } else if (output.outputType == "error") {
            _state.update { currentState ->
                val notebookFile = currentState.notebookFile
                val currentNotebook = currentState.domainNotebook
                if (notebookFile != null && currentNotebook != null) {
                    val nb = notebookFileUseCase.appendOutput(
                        notebookFile,
                        currentNotebook,
                        cellId,
                        output
                    )
                    currentState.copy(domainNotebook = nb)
                } else {
                    currentState
                }
            }
        }


        _currentStdout.flush()
        appStateStore.dispatch(Action.SetRunning(false))
    }

    private suspend fun requestNotebookCompletions(
        notebookPath: EntryPath,
        cellId: String,
        text: String,
        cursorIndex: Int
    ): List<Definition> {
        if (cursorIndex < 0) return emptyList()
        val targetIndex = cursorIndex.coerceAtMost(text.length)
        val uri = ensureNotebookCellDocument(notebookPath, cellId, text) ?: return emptyList()
        return runCatching {
            val position = calculatePosition(text, targetIndex)
            languageFeatureProvider.requestCompletion(uri, position).items.toDefinitionList()
        }.getOrElse { emptyList() }
    }

    private suspend fun ensureNotebookCellDocument(
        notebookPath: EntryPath,
        cellId: String,
        text: String
    ): String? {
        val uri = buildNotebookCellUri(notebookPath, cellId)
        val needsOpen = notebookDocumentMutex.withLock {
            if (openedCellUris.contains(uri)) {
                false
            } else {
                openedCellUris.add(uri)
                true
            }
        }

        if (needsOpen) {
            val openResult = runCatching {
                languageFeatureProvider.openDocument(
                    LanguageServerDocument(
                        uri = uri,
                        languageId = "dncl",
                        text = text
                    )
                )
            }
            if (openResult.isFailure) {
                notebookDocumentMutex.withLock { openedCellUris.remove(uri) }
                return null
            }
        }

        val applyResult = runCatching {
            languageFeatureProvider.applyChanges(uri, text)
        }
        if (applyResult.isFailure) {
            return null
        }
        return uri
    }

    private suspend fun closeNotebookCellDocument(notebookPath: EntryPath, cellId: String) {
        val uri = buildNotebookCellUri(notebookPath, cellId)
        val shouldClose = notebookDocumentMutex.withLock { openedCellUris.remove(uri) }
        if (shouldClose) {
            runCatching { languageFeatureProvider.closeDocument(uri) }
        }
    }

    private suspend fun closeAllNotebookCellDocuments() {
        val uris = notebookDocumentMutex.withLock {
            val snapshot = openedCellUris.toList()
            openedCellUris.clear()
            snapshot
        }
        uris.forEach { uri ->
            runCatching { languageFeatureProvider.closeDocument(uri) }
        }
    }

    private fun buildNotebookCellUri(notebookPath: EntryPath, cellId: String): String {
        val baseUri = notebookPath.toFileUri()
        return "vscode-notebook-cell:$baseUri#$cellId"
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
            val currentLine = textBeforeCursor.substringAfterLast('\n', textBeforeCursor)
            val indent = currentLine.takeWhile { it == ' ' || it == '\t' }
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
