package io.github.arashiyama11.dncl_ide.editor.core

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import io.github.arashiyama11.dncl_ide.editor.lsp.LanguageFeatureProvider
import io.github.arashiyama11.dncl_ide.editor.lsp.LanguageServerDocument
import io.github.arashiyama11.dncl_ide.language_server.CompletionItem
import io.github.arashiyama11.dncl_ide.language_server.CompletionList
import io.github.arashiyama11.dncl_ide.language_server.Diagnostic
import io.github.arashiyama11.dncl_ide.language_server.Position
import io.github.arashiyama11.dncl_ide.language_server.Range
import io.github.arashiyama11.dncl_ide.language_server.SemanticTokens
import io.github.arashiyama11.dncl_ide.language_server.ServerCapabilities
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultEditorSessionTest {

    private val sampleDocument = EditorDocument(
        uri = "file:///test.dncl",
        languageId = "dncl",
        initialText = "print 1"
    )

    @Test
    fun initializeSuccessUpdatesStateAndReceivesDiagnostics() = runTest {
        val provider = FakeLanguageFeatureProvider()
        val session = DefaultEditorSession(this, provider)

        session.dispatch(EditorIntent.Initialize(sampleDocument))
        advanceUntilIdle()

        val state = session.state.value
        assertTrue(state.isInitialized)
        assertFalse(state.isBusy)
        assertEquals(sampleDocument, state.document)
        assertEquals(sampleDocument.initialText, state.content.text.text)
        assertEquals(0L, state.content.revision)
        assertNull(state.lastError)
        assertEquals(emptyList(), state.diagnostics)
        assertEquals(1, provider.openedDocuments.size)
        val opened = provider.openedDocuments.single()
        assertEquals(sampleDocument.uri, opened.uri)
        assertEquals(sampleDocument.languageId, opened.languageId)
        assertEquals(sampleDocument.initialText, opened.text)

        val diagnostics = listOf(diagnostic("unused"))
        provider.emitDiagnostics(diagnostics)
        advanceUntilIdle()

        assertEquals(diagnostics, session.state.value.diagnostics)

        session.close()
        advanceUntilIdle()
    }

    @Test
    fun initializeFailureRecordsInitializationError() = runTest {
        val provider = FakeLanguageFeatureProvider().apply {
            openDocumentResult = Result.failure<Unit>(TestException("open failed"))
        }
        val session = DefaultEditorSession(this, provider)

        session.dispatch(EditorIntent.Initialize(sampleDocument))
        advanceUntilIdle()

        val state = session.state.value
        assertFalse(state.isBusy)
        assertFalse(state.isInitialized)
        val error = state.lastError
        assertTrue(error is EditorError.Initialization)
        assertEquals("open failed", error?.message)

        session.close()
        advanceUntilIdle()
    }

    @Test
    fun updateContentFailureKeepsDirtyFlagAndStoresLanguageFeatureError() = runTest {
        val provider = FakeLanguageFeatureProvider().apply {
            applyChangesResult = Result.failure<Unit>(TestException("apply failed"))
        }
        val session = DefaultEditorSession(this, provider)

        session.dispatch(EditorIntent.Initialize(sampleDocument))
        advanceUntilIdle()

        val update = EditorContentUpdate(
            content = EditorContent(
                text = TextFieldValue(
                    text = "print 2",
                    selection = TextRange(2)
                )
            )
        )
        session.dispatch(EditorIntent.UpdateContent(update))
        advanceUntilIdle()

        val state = session.state.value
        assertTrue(state.isDirty)
        assertEquals("print 2", state.content.text.text)
        assertEquals(1L, state.content.revision)
        val error = state.lastError
        assertTrue(error is EditorError.LanguageFeature)
        assertEquals("apply failed", error?.message)
        assertEquals(listOf(sampleDocument.uri to "print 2"), provider.applyChangesRequests)

        session.close()
        advanceUntilIdle()
    }

    @Test
    fun triggerCompletionSuccessUpdatesCompletions() = runTest {
        val items = listOf(CompletionItem(label = "print"))
        val provider = FakeLanguageFeatureProvider().apply {
            completionResult = Result.success(CompletionList(isIncomplete = false, items = items))
        }
        val session = DefaultEditorSession(this, provider)

        session.dispatch(EditorIntent.Initialize(sampleDocument))
        advanceUntilIdle()

        session.dispatch(EditorIntent.TriggerCompletion(Position(line = 0, character = 1)))
        advanceUntilIdle()

        val state = session.state.value
        assertFalse(state.isBusy)
        assertEquals(items, state.completions)
        assertNull(state.lastError)

        session.close()
        advanceUntilIdle()
    }

    @Test
    fun closeResetsStateAndClosesDocument() = runTest {
        val provider = FakeLanguageFeatureProvider()
        val session = DefaultEditorSession(this, provider)

        session.dispatch(EditorIntent.Initialize(sampleDocument))
        advanceUntilIdle()

        session.close()
        advanceUntilIdle()

        assertEquals(EditorState(), session.state.value)
        assertEquals(listOf(sampleDocument.uri), provider.closedDocuments)
    }

    private fun diagnostic(message: String): Diagnostic = Diagnostic(
        range = Range(
            start = Position(line = 0, character = 0),
            end = Position(line = 0, character = 5)
        ),
        severity = 2,
        message = message,
        source = "test"
    )

    private class FakeLanguageFeatureProvider : LanguageFeatureProvider {
        var openDocumentResult: Result<Unit> = Result.success(Unit)
        var applyChangesResult: Result<Unit> = Result.success(Unit)
        var closeDocumentResult: Result<Unit> = Result.success(Unit)
        var completionResult: Result<CompletionList> = Result.success(CompletionList(false, emptyList()))
        var semanticTokensResult: Result<SemanticTokens> = Result.success(SemanticTokens(data = emptyList()))

        val openedDocuments = mutableListOf<LanguageServerDocument>()
        val applyChangesRequests = mutableListOf<Pair<String, String>>()
        val closedDocuments = mutableListOf<String>()

        private val diagnosticsFlow = MutableSharedFlow<List<Diagnostic>>(replay = 1)
        private val capabilitiesFlow = MutableStateFlow<ServerCapabilities?>(null)

        init {
            diagnosticsFlow.tryEmit(emptyList())
        }

        suspend fun emitDiagnostics(value: List<Diagnostic>) {
            diagnosticsFlow.emit(value)
        }

        override suspend fun openDocument(document: LanguageServerDocument) {
            openedDocuments += document
            openDocumentResult.getOrThrow()
        }

        override suspend fun applyChanges(uri: String, text: String) {
            applyChangesRequests += uri to text
            applyChangesResult.getOrThrow()
        }

        override suspend fun closeDocument(uri: String) {
            closedDocuments += uri
            closeDocumentResult.getOrThrow()
        }

        override fun diagnostics(uri: String): Flow<List<Diagnostic>> = diagnosticsFlow

        override suspend fun requestCompletion(uri: String, position: Position): CompletionList =
            completionResult.getOrThrow()

        override suspend fun requestSemanticTokens(uri: String): SemanticTokens =
            semanticTokensResult.getOrThrow()

        override val capabilities: Flow<ServerCapabilities?>
            get() = capabilitiesFlow
    }

    private class TestException(message: String) : RuntimeException(message)
}
