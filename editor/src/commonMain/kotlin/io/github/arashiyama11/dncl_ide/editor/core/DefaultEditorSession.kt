package io.github.arashiyama11.dncl_ide.editor.core

import androidx.compose.ui.text.input.TextFieldValue
import io.github.arashiyama11.dncl_ide.editor.lsp.LanguageFeatureProvider
import io.github.arashiyama11.dncl_ide.editor.lsp.LanguageServerDocument
import io.github.arashiyama11.dncl_ide.language_server.CompletionItem
import io.github.arashiyama11.dncl_ide.language_server.Position
import io.github.arashiyama11.dncl_ide.language_server.SemanticTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DefaultEditorSession(
    private val scope: CoroutineScope,
    private val languageFeatureProvider: LanguageFeatureProvider
) : EditorSession {

    private val mutableState = MutableStateFlow(EditorState())
    override val state: StateFlow<EditorState> = mutableState.asStateFlow()

    private var diagnosticsJob: Job? = null
    private var currentDocument: EditorDocument? = null

    override fun dispatch(intent: EditorIntent) {
        when (intent) {
            is EditorIntent.Initialize -> handleInitialize(intent.document)
            is EditorIntent.UpdateContent -> handleContentUpdate(intent.update)
            is EditorIntent.TriggerCompletion -> handleCompletion(intent.position)
            is EditorIntent.RequestSemanticTokens -> handleSemanticTokens()
            is EditorIntent.Close -> scope.launch { closeInternal() }
        }
    }

    override suspend fun close() {
        closeInternal()
    }

    private fun handleInitialize(document: EditorDocument) {
        if (currentDocument?.uri == document.uri && state.value.isInitialized) {
            // 既に初期化済みであれば再初期化は不要。テキストだけ更新する。
            mutableState.update {
                it.copy(
                    document = document,
                    content = it.content.copy(
                        text = TextFieldValue(document.initialText),
                        revision = 0
                    ),
                    isDirty = false,
                    lastError = null
                )
            }
            return
        }

        val previous = currentDocument
        if (previous != null && previous.uri != document.uri) {
            scope.launch {
                runCatching { languageFeatureProvider.closeDocument(previous.uri) }
            }
        }
        currentDocument = document
        mutableState.update {
            it.copy(
                document = document,
                content = EditorContent(
                    text = TextFieldValue(document.initialText),
                    revision = 0
                ),
                diagnostics = emptyList(),
                completions = emptyList(),
                semanticTokens = null,
                isInitialized = false,
                isBusy = true,
                isDirty = false,
                lastError = null
            )
        }

        scope.launch {
            runCatching {
                languageFeatureProvider.openDocument(
                    LanguageServerDocument(
                        uri = document.uri,
                        languageId = document.languageId,
                        text = document.initialText
                    )
                )
            }.onSuccess {
                startDiagnostics(document.uri)
                mutableState.update { state ->
                    state.copy(
                        isInitialized = true,
                        isBusy = false,
                        lastError = null
                    )
                }
            }.onFailure { error ->
                mutableState.update { state ->
                    state.copy(
                        isBusy = false,
                        lastError = EditorError.Initialization(error.message, error)
                    )
                }
            }
        }
    }

    private fun handleContentUpdate(update: EditorContentUpdate) {
        val document = currentDocument ?: return
        val currentContent = mutableState.value.content
        val nextRevision = currentContent.revision + 1
        val nextContent = update.content.copy(revision = nextRevision)

        mutableState.update {
            it.copy(
                content = nextContent,
                isDirty = true
            )
        }

        scope.launch {
            runCatching {
                languageFeatureProvider.applyChanges(document.uri, nextContent.text.text)
            }.onFailure { error ->
                mutableState.update { state ->
                    state.copy(lastError = EditorError.LanguageFeature(error.message, error))
                }
            }
        }
    }

    private fun handleCompletion(position: Position) {
        val document = currentDocument ?: return
        scope.launch {
            mutableState.update { it.copy(isBusy = true) }
            val result = runCatching {
                languageFeatureProvider.requestCompletion(document.uri, position).items
            }
            mutableState.update { state ->
                state.copy(
                    isBusy = false,
                    completions = result.getOrElse { emptyList<CompletionItem>() },
                    lastError = result.exceptionOrNull()
                        ?.let { EditorError.LanguageFeature(it.message, it) }
                )
            }
        }
    }

    private fun handleSemanticTokens() {
        val document = currentDocument ?: return
        scope.launch {
            mutableState.update { it.copy(isBusy = true) }
            val result = runCatching {
                languageFeatureProvider.requestSemanticTokens(document.uri)
            }
            mutableState.update { state ->
                state.copy(
                    isBusy = false,
                    semanticTokens = result.getOrNull(),
                    lastError = result.exceptionOrNull()
                        ?.let { EditorError.LanguageFeature(it.message, it) }
                )
            }
        }
    }

    private suspend fun closeInternal() {
        diagnosticsJob?.cancel()
        diagnosticsJob = null
        currentDocument?.let { document ->
            runCatching { languageFeatureProvider.closeDocument(document.uri) }
        }
        currentDocument = null
        mutableState.value = EditorState()
    }

    private fun startDiagnostics(uri: String) {
        diagnosticsJob?.cancel()
        diagnosticsJob = scope.launch {
            languageFeatureProvider.diagnostics(uri).collect { diagnostics ->
                mutableState.update { state ->
                    state.copy(diagnostics = diagnostics)
                }
            }
        }
    }
}
