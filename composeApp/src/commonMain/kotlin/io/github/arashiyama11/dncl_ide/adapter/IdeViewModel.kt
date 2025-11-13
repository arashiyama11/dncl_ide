package io.github.arashiyama11.dncl_ide.adapter

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import io.github.arashiyama11.dncl_ide.common.Action
import io.github.arashiyama11.dncl_ide.common.AppStateStore
import io.github.arashiyama11.dncl_ide.common.AppStateStore.Companion.dispatch
import io.github.arashiyama11.dncl_ide.common.StatePermission
import io.github.arashiyama11.dncl_ide.domain.model.CursorPosition
import io.github.arashiyama11.dncl_ide.domain.model.DebugRunningMode
import io.github.arashiyama11.dncl_ide.domain.model.Definition
import io.github.arashiyama11.dncl_ide.domain.model.DnclOutput
import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.domain.model.FileContent
import io.github.arashiyama11.dncl_ide.domain.model.NotebookFile
import io.github.arashiyama11.dncl_ide.domain.model.ProgramFile
import io.github.arashiyama11.dncl_ide.domain.model.SuggestionPanelStyle
import io.github.arashiyama11.dncl_ide.domain.repository.SettingsRepository.Companion.DEFAULT_DEBUG_RUNNING_MODE
import io.github.arashiyama11.dncl_ide.domain.repository.SettingsRepository.Companion.DEFAULT_FONT_SIZE
import io.github.arashiyama11.dncl_ide.domain.usecase.ExecuteUseCase
import io.github.arashiyama11.dncl_ide.domain.usecase.FileUseCase
import io.github.arashiyama11.dncl_ide.editor.compose.toEditorContentUpdate
import io.github.arashiyama11.dncl_ide.editor.core.DefaultEditorSession
import io.github.arashiyama11.dncl_ide.editor.core.EditorDocument
import io.github.arashiyama11.dncl_ide.editor.core.EditorIntent
import io.github.arashiyama11.dncl_ide.editor.core.EditorState
import io.github.arashiyama11.dncl_ide.editor.lsp.LanguageFeatureProvider
import io.github.arashiyama11.dncl_ide.language_server.Diagnostic
import io.github.arashiyama11.dncl_ide.language_server.Position
import io.github.arashiyama11.dncl_ide.language_server.util.calculatePosition
import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import io.github.arashiyama11.dncl_ide.interpreter.model.DnclError
import io.github.arashiyama11.dncl_ide.interpreter.model.Environment
import io.github.arashiyama11.dncl_ide.interpreter.parser.Parser
import io.github.arashiyama11.dncl_ide.util.OutputHandler
import io.github.arashiyama11.dncl_ide.util.Platform
import io.github.arashiyama11.dncl_ide.util.SyntaxHighLighter
import io.github.arashiyama11.dncl_ide.util.currentPlatform
import io.github.arashiyama11.dncl_ide.util.toFileUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlin.math.min


data class IdeUiState(
    val codeTextFieldValue: TextFieldValue = TextFieldValue(""),
    val dnclError: DnclError? = null,
    val annotatedString: AnnotatedString? = null,
    val highlightRevision: Long = 0L,
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
    val isWaitingForInput: Boolean = false,
    val debugMode: Boolean = false,
    val debugRunningMode: DebugRunningMode = DEFAULT_DEBUG_RUNNING_MODE,
    val isDarkTheme: Boolean = false,
    val textSuggestions: List<Definition> = emptyList(),
    val isFocused: Boolean = false,
    val showInlineSuggestions: Boolean = false,
    val languageDiagnostics: List<Diagnostic> = emptyList(),
    val selectedEntryPath: EntryPath? = null,
    val running: Boolean = false,
    val suggestionPanelStyle: SuggestionPanelStyle = SuggestionPanelStyle.BOTTOM_STRIP,
    val textInputMode: TextInputMode = TextInputMode.STANDARD,
    val customImeSnippets: List<CustomImeSnippet> = emptyList(),
    val customImeQuickKeys: List<String> = emptyList(),
    val customImeKeywords: List<CustomImeKeyword> = emptyList(),
    val customImePanelMode: CustomImePanelMode = CustomImePanelMode.QUICK_KEYS
)

enum class TextFieldType {
    OUTPUT, DEBUG_OUTPUT
}

class IdeViewModel(
    private val syntaxHighLighter: SyntaxHighLighter,
    private val executeUseCase: ExecuteUseCase,
    private val fileUseCase: FileUseCase,
    private val appStateStore: AppStateStore<StatePermission.Write>,
    private val languageFeatureProvider: LanguageFeatureProvider
) : ViewModel() {
    private val editorSession = DefaultEditorSession(viewModelScope, languageFeatureProvider)
    private val editorStateFlow = editorSession.state
    private val appState by appStateStore

    private val defaultTextInputMode: TextInputMode =
        if (currentPlatform == Platform.Desktop || currentPlatform == Platform.Web) {
            TextInputMode.CUSTOM
        } else {
            TextInputMode.STANDARD
        }

    private val _localState = MutableStateFlow(
        LocalIdeState(
            codeTextFieldValue = TextFieldValue(""),
            dnclError = null,
            annotatedString = null,
            highlightRevision = 0L,
            output = "",
            currentInput = "",
            isError = false,
            errorRange = null,
            currentEvaluatingLine = null,
            textFieldType = TextFieldType.OUTPUT,
            currentEnvironment = null,
            isStepMode = false,
            isLineMode = false,
            isWaitingForInput = false,
            isDarkTheme = false,
            textSuggestions = emptyList(),
            isFocused = false,
            showInlineSuggestions = false,
            languageDiagnostics = emptyList(),
            textInputMode = defaultTextInputMode,
            customImeSnippets = emptyList(),
            customImeQuickKeys = emptyList(),
            customImeKeywords = emptyList(),
            customImePanelMode = CustomImePanelMode.QUICK_KEYS
        )
    )

    private val customImeController = CustomImeController(
        getCurrentTextValue = { _localState.value.codeTextFieldValue },
        onTextChanged = { value, userTriggered ->
            onTextChanged(value, userTriggered)
        }
    ).apply {
        rankingStrategy = CustomImeController.SnippetRankingStrategy.PrefixMatch
    }

    init {
        _localState.update {
            it.copy(
                customImeSnippets = customImeController.snippets.value,
                customImeQuickKeys = customImeController.quickKeys.value,
                customImeKeywords = customImeController.keywords.value
            )
        }

        viewModelScope.launch {
            editorStateFlow.collect { editorState ->
                val lspSuggestions = editorState.completions.toDefinitionList()
                val editorText = editorState.content.text
                _localState.update { state ->
                    val nextTextFieldValue = when {
                        editorText.text.isEmpty() && state.codeTextFieldValue.text.isNotEmpty() -> state.codeTextFieldValue
                        else -> editorText
                    }
                    state.copy(
                        codeTextFieldValue = nextTextFieldValue,
                        languageDiagnostics = editorState.diagnostics,
                        textSuggestions = lspSuggestions
                    )
                }
            }
        }

        viewModelScope.launch {
            customImeController.snippets.collect { snippets ->
                _localState.update { state ->
                    state.copy(customImeSnippets = snippets)
                }
            }
        }

        viewModelScope.launch {
            customImeController.quickKeys.collect { keys ->
                _localState.update { state ->
                    state.copy(customImeQuickKeys = keys)
                }
            }
        }

        viewModelScope.launch {
            customImeController.keywords.collect { keywords ->
                _localState.update { state ->
                    state.copy(customImeKeywords = keywords)
                }
            }
        }
    }

    val uiState = combine(
        _localState,
        appStateStore.state
    ) { localState, appState ->
        IdeUiState(
            codeTextFieldValue = localState.codeTextFieldValue,
            dnclError = localState.dnclError,
            annotatedString = localState.annotatedString,
            highlightRevision = localState.highlightRevision,
            output = localState.output,
            currentInput = localState.currentInput,
            isError = localState.isError,
            errorRange = localState.errorRange,
            fontSize = appState.uiConfig.fontSize,
            currentEvaluatingLine = localState.currentEvaluatingLine,
            textFieldType = localState.textFieldType,
            currentEnvironment = localState.currentEnvironment,
            isStepMode = localState.isStepMode,
            isLineMode = localState.isLineMode,
            isWaitingForInput = localState.isWaitingForInput,
            debugMode = appState.dnclConfig.debugModeEnabled,
            debugRunningMode = appState.dnclConfig.debugRunningMode,
            isDarkTheme = localState.isDarkTheme,
            textSuggestions = localState.textSuggestions,
            isFocused = localState.isFocused,
            showInlineSuggestions = localState.showInlineSuggestions,
            languageDiagnostics = localState.languageDiagnostics,
            selectedEntryPath = appState.selectedEntryPath,
            running = appState.running,
            suggestionPanelStyle = appState.uiConfig.suggestionPanelStyle,
            textInputMode = localState.textInputMode,
            customImeSnippets = localState.customImeSnippets,
            customImeQuickKeys = localState.customImeQuickKeys,
            customImeKeywords = localState.customImeKeywords,
            customImePanelMode = localState.customImePanelMode
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, IdeUiState())

    private var executeJob: Job? = null
    private var inputChannel: Channel<String>? = null
    val errorChannel = Channel<String>(Channel.BUFFERED)
    private var executeScope: CoroutineScope = CoroutineScope(Dispatchers.Default + Job())
    private lateinit var outputHandler: OutputHandler
    private var completionTriggerJob: Job? = null

    fun onPause() {
        viewModelScope.launch {
            saveFile()
        }
    }

    fun onStart(isDarkTheme: StateFlow<Boolean>) {
        outputHandler = OutputHandler(viewModelScope) { outputs ->
            viewModelScope.launch(Dispatchers.Main) {
                _localState.update {
                    it.copy(output = outputs[null] ?: "")
                }
            }
        }

        viewModelScope.launch {
            isDarkTheme.collect {
                _localState.update { state ->
                    state.copy(isDarkTheme = it)
                }

                onTextChanged(uiState.value.codeTextFieldValue, userTriggeredTyping = false)
            }
        }

        viewModelScope.launch {
            var prePath: EntryPath? = null
            appStateStore.state.collect { appState ->
                val entryPath = appState.selectedEntryPath
                if (entryPath != null && entryPath != prePath) {
                    val programFile = fileUseCase.getEntryByPath(entryPath)
                    when (programFile) {
                        is ProgramFile -> {
                            if (prePath != null) saveFile(prePath)
                            viewModelScope.launch(Dispatchers.Main) {
                                _localState.update {
                                    it.copy(output = "")
                                }
                            }
                            val content = fileUseCase.getFileContent(programFile)
                            val textValue = TextFieldValue(
                                content.value,
                                TextRange(fileUseCase.getCursorPosition(programFile).value)
                            )
                            val uri = programFile.path.toFileUri()

                            val document = EditorDocument(
                                uri = uri,
                                languageId = "dncl",
                                initialText = content.value
                            )
                            editorSession.dispatch(EditorIntent.Initialize(document))

                            onTextChanged(textValue, userTriggeredTyping = false)
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

    fun onTextChanged(text: TextFieldValue, userTriggeredTyping: Boolean? = null) {
        val previousValue = uiState.value.codeTextFieldValue
        val indentedText = autoIndent(previousValue, text)
        val visibilityDecision = decideInlineSuggestionVisibility(
            previousValue = previousValue,
            updatedValue = indentedText,
            userTriggeredTyping = userTriggeredTyping,
            currentVisibility = _localState.value.showInlineSuggestions
        )
        val currentEditorState: EditorState = editorStateFlow.value
        if (currentEditorState.document != null) {
            editorSession.dispatch(
                EditorIntent.UpdateContent(
                    indentedText.toEditorContentUpdate(currentEditorState.content.revision)
                )
            )
        }

        viewModelScope.launch(Dispatchers.Default) {
            viewModelScope.launch(Dispatchers.Main) {
                _localState.update {
                    it.copy(
                        codeTextFieldValue = indentedText
                    ).applyInlineSuggestionDecision(visibilityDecision)
                }
            }

            val tokens = Lexer(indentedText.text).toList()

            val lexicalTokens = tokens.mapNotNull { it.getOrNull() }
            customImeController.onEditorContextChanged(indentedText, lexicalTokens)

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
            viewModelScope.launch(Dispatchers.Main) {
                _localState.update {
                    it.copy(
                        dnclError = finalError,
                        output = finalError?.explain(uiState.value.codeTextFieldValue.text)
                            ?: if (it.dnclError == null) it.output else "",
                        errorRange = finalError?.errorRange
                            ?: if (it.dnclError == null) it.errorRange else null,
                    )
                }
            }

            viewModelScope.launch(Dispatchers.Main) {
                _localState.update {
                    it.copy(
                        annotatedString = annotatedString,
                        highlightRevision = if (it.annotatedString != annotatedString) {
                            it.highlightRevision + 1
                        } else {
                            it.highlightRevision
                        }
                    )
                }
            }

            editorStateFlow.value.document?.let {
                val position: Position =
                    calculatePosition(indentedText.text, indentedText.selection.end)
                triggerCompletionDebounced(position)
            }
        }
    }

    private fun triggerCompletionDebounced(position: Position) {
        completionTriggerJob?.cancel()
        completionTriggerJob = viewModelScope.launch {
            delay(COMPLETION_DEBOUNCE_MS)
            editorSession.dispatch(EditorIntent.TriggerCompletion(position))
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun onRunButtonClicked() {
        viewModelScope.launch {
            saveFile()
        }

        appStateStore.dispatch(Action.SetRunning(true))
        viewModelScope.launch {
            outputHandler.stdout.clear()
        }

        executeJob?.cancel()
        // Cancel previous execution scope and recreate
        executeScope.coroutineContext.job.cancel().also {
            executeScope = CoroutineScope(Dispatchers.Default + Job())
        }

        inputChannel?.close()
        inputChannel = Channel(Channel.UNLIMITED)

        executeJob = executeScope.launch {
            _localState.update {
                it.copy(
                    output = "",
                    isError = false,
                    errorRange = null,
                    currentEvaluatingLine = null,
                    dnclError = null,
                    isWaitingForInput = false
                )
            }
            onTextChanged(uiState.value.codeTextFieldValue, userTriggeredTyping = false)

            executeUseCase(
                uiState.value.codeTextFieldValue.text,
                inputChannel!!,
                appState.dnclConfig.arrayOriginIndex,
            ).collect { output ->
                when (output) {
                    is DnclOutput.RuntimeError -> {
                        viewModelScope.launch(Dispatchers.Main) {
                            _localState.update {
                                it.copy(
                                    // output = it.output + "\n" + output.value.explain(uiState.value.codeTextFieldValue.text), // Output is handled by watchStdoutChannel
                                    isError = true,
                                    errorRange = output.value.astNode.range
                                )
                            }
                        }
                        appStateStore.dispatch(Action.SetRunning(false)) // Removed cast
                    }

                    is DnclOutput.Error -> {
                        viewModelScope.launch(Dispatchers.Main) {
                            _localState.update {
                                it.copy(
                                    // output = "${it.output}\n${output.value}", // Output is handled by watchStdoutChannel
                                    isError = true
                                )
                            }
                        }
                        appStateStore.dispatch(Action.SetRunning(false)) // Removed cast
                    }

                    is DnclOutput.StdoutAppend -> {
                        outputHandler.stdout.append(text = output.value)
                    }

                    is DnclOutput.StdoutClear -> {
                        outputHandler.stdout.clear()
                    }

                    is DnclOutput.StdoutFlush -> {
                        outputHandler.stdout.flush()
                    }

                    is DnclOutput.StdoutCommitFrame -> {
                        outputHandler.stdout.commitFrame()
                    }

                    is DnclOutput.StdoutReplace -> {
                        outputHandler.stdout.replace(text = output.value)
                    }

                    is DnclOutput.LineEvaluation -> {
                        viewModelScope.launch(Dispatchers.Main) {
                            _localState.update {
                                it.copy(
                                    currentEvaluatingLine = output.value
                                )
                            }
                        }
                    }

                    is DnclOutput.EnvironmentUpdate -> {
                        viewModelScope.launch(Dispatchers.Main) {
                            _localState.update {
                                it.copy(
                                    currentEnvironment = output.environment.copy()
                                )
                            }
                        }
                    }

                    is DnclOutput.WaitingForInput -> {
                        viewModelScope.launch(Dispatchers.Main) {
                            _localState.update {
                                it.copy(isWaitingForInput = output.isWaiting)
                            }
                        }
                    }
                }
            }
            outputHandler.stdout.flush()
            delay(50)
            viewModelScope.launch(Dispatchers.Main) {
                _localState.update { it.copy(currentEvaluatingLine = null /*, isExecuting = false */) }
            } // isExecuting controlled by AppState
            appStateStore.dispatch(Action.SetRunning(false)) // Removed cast
            onTextChanged(uiState.value.codeTextFieldValue, userTriggeredTyping = false)
        }
    }

    fun onCancelButtonClicked() {
        executeJob?.cancel()
        executeScope.coroutineContext.job.cancel().also {
            executeScope = CoroutineScope(Dispatchers.Default + Job())
        }
        _localState.update { it.copy(currentEvaluatingLine = null) }
        appStateStore.dispatch(Action.SetRunning(false))
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

    fun toggleTextInputMode() {
        val next = if (uiState.value.textInputMode == TextInputMode.CUSTOM) {
            TextInputMode.STANDARD
        } else {
            TextInputMode.CUSTOM
        }
        setTextInputMode(next)
    }

    fun setTextInputMode(mode: TextInputMode) {
        _localState.update { state ->
            if (state.textInputMode == mode) {
                state
            } else {
                state.copy(
                    textInputMode = mode,
                    showInlineSuggestions = if (mode == TextInputMode.CUSTOM) {
                        false
                    } else {
                        state.showInlineSuggestions
                    }
                )
            }
        }
    }

    fun onCustomImeSnippetSelected(snippet: CustomImeSnippet) {
        customImeController.onSnippetSelected(snippet)
    }

    fun onCustomImeQuickKeySelected(symbol: String) {
        customImeController.onQuickKeySelected(symbol)
    }

    fun onCustomImeKeywordSelected(keyword: CustomImeKeyword) {
        customImeController.onKeywordSelected(keyword)
    }

    fun onCustomImeInsertNewLine() {
        customImeController.onInsertNewLine()
    }

    fun onCustomImeDeleteBackward() {
        customImeController.onDeleteBackward()
    }

    fun onCustomImePanelModeChange(mode: CustomImePanelMode) {
        _localState.update { state ->
            if (state.customImePanelMode == mode) state else state.copy(customImePanelMode = mode)
        }
    }

    fun onConfirmTextSuggestion(suggestionText: String) {
        val currentValue = uiState.value.codeTextFieldValue
        val selection = currentValue.selection
        val beforeText = currentValue.text.substring(0, selection.start)
        val afterText = currentValue.text.substring(selection.end)

        val overlapLength = longestOverlapWithSuffix(beforeText, suggestionText)
        val textToInsert = suggestionText.drop(overlapLength)

        val newText = buildString {
            append(beforeText)
            append(textToInsert)
            append(afterText)
        }
        val newCursor = beforeText.length + textToInsert.length
        onTextChanged(TextFieldValue(newText, TextRange(newCursor)))
    }

    private fun longestOverlapWithSuffix(base: String, suggestion: String): Int {
        val maxLength = min(base.length, suggestion.length)
        for (length in maxLength downTo 0) {
            if (base.endsWith(suggestion.substring(0, length))) {
                return length
            }
        }
        return 0
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
        _localState.update {
            it.copy(textFieldType = next)
        }
    }

    fun onCurrentInputChanged(text: String) {
        _localState.update {
            it.copy(currentInput = text)
        }
    }

    fun onSendInputClicked() {
        val currentInputValue = uiState.value.currentInput
        viewModelScope.launch {
            inputChannel?.send(currentInputValue)
            _localState.update {
                it.copy(
                    currentInput = ""
                )
            }
        }
    }

    fun onCodeEditorFocused(isFocused: Boolean) {
        _localState.update {
            it.withFocusState(isFocused)
        }
    }

    private fun decideInlineSuggestionVisibility(
        previousValue: TextFieldValue,
        updatedValue: TextFieldValue,
        userTriggeredTyping: Boolean?,
        currentVisibility: Boolean
    ): InlineSuggestionVisibilityDecision {
        userTriggeredTyping?.let { explicit ->
            return InlineSuggestionVisibilityDecision(shouldUpdate = true, show = explicit)
        }

        if (previousValue.text != updatedValue.text) {
            return InlineSuggestionVisibilityDecision(shouldUpdate = true, show = true)
        }

        if (previousValue.selection != updatedValue.selection) {
            return InlineSuggestionVisibilityDecision(shouldUpdate = true, show = false)
        }

        return InlineSuggestionVisibilityDecision(shouldUpdate = false, show = currentVisibility)
    }

    private fun LocalIdeState.applyInlineSuggestionDecision(
        decision: InlineSuggestionVisibilityDecision
    ): LocalIdeState =
        if (!decision.shouldUpdate) {
            this
        } else {
            copy(showInlineSuggestions = decision.show)
        }

    private fun LocalIdeState.withFocusState(isFocused: Boolean): LocalIdeState {
        return if (isFocused) {
            copy(isFocused = true)
        } else {
            val nextInputMode = if (textInputMode == TextInputMode.CUSTOM) {
                TextInputMode.CUSTOM
            } else {
                TextInputMode.STANDARD
            }
            copy(
                isFocused = false,
                showInlineSuggestions = false,
                textInputMode = nextInputMode
            )
        }
    }

    private data class InlineSuggestionVisibilityDecision(
        val shouldUpdate: Boolean,
        val show: Boolean
    )

    override fun onCleared() {
        viewModelScope.launch {
            editorSession.close()
        }
        super.onCleared()
    }

    private suspend fun saveFile(entryPath: EntryPath? = null) {
        val path = entryPath ?: appState.selectedEntryPath ?: run {
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

    companion object {
        private const val COMPLETION_DEBOUNCE_MS = 100L
    }

    private data class LocalIdeState(
        val codeTextFieldValue: TextFieldValue,
        val dnclError: DnclError?,
        val annotatedString: AnnotatedString?,
        val highlightRevision: Long,
        val output: String,
        val currentInput: String,
        val isError: Boolean,
        val errorRange: IntRange?,
        val currentEvaluatingLine: Int?,
        val textFieldType: TextFieldType,
        val currentEnvironment: Environment?,
        val isStepMode: Boolean,
        val isLineMode: Boolean,
        val isWaitingForInput: Boolean,
        val isDarkTheme: Boolean,
        val textSuggestions: List<Definition>,
        val isFocused: Boolean,
        val showInlineSuggestions: Boolean,
        val languageDiagnostics: List<Diagnostic>,
        val textInputMode: TextInputMode,
        val customImeSnippets: List<CustomImeSnippet>,
        val customImeQuickKeys: List<String>,
        val customImeKeywords: List<CustomImeKeyword>,
        val customImePanelMode: CustomImePanelMode
    )
}
