package io.github.arashiyama11.dncl_ide.editor.lsp

import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.editor.lsp.DefaultLanguageFeatureProvider
import io.github.arashiyama11.dncl_ide.editor.lsp.LanguageServerClient
import io.github.arashiyama11.dncl_ide.editor.lsp.LanguageServerDocument
import io.github.arashiyama11.dncl_ide.language_server.ClientCapabilities
import io.github.arashiyama11.dncl_ide.language_server.CompletionItem
import io.github.arashiyama11.dncl_ide.language_server.CompletionList
import io.github.arashiyama11.dncl_ide.language_server.Diagnostic
import io.github.arashiyama11.dncl_ide.language_server.Position
import io.github.arashiyama11.dncl_ide.language_server.SemanticTokens
import io.github.arashiyama11.dncl_ide.language_server.ServerCapabilities
import io.github.arashiyama11.dncl_ide.util.RootPathProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultLanguageFeatureProviderTest {

    @Test
    fun `openDocument initializes session once and forwards call`() = runTest {
        val fakeClient = FakeLanguageServerClient()
        val service = DefaultLanguageFeatureProvider(
            rootPathProvider = object : RootPathProvider {
                override fun invoke(): EntryPath = EntryPath.fromString("/workspace/project")
            },
            client = fakeClient
        )

        val doc = LanguageServerDocument("file:///workspace/project/main.dncl", "dncl", "内容")
        service.openDocument(doc)
        service.openDocument(doc)

        assertEquals(1, fakeClient.initializeCount)
        assertEquals("file:///workspace/project", fakeClient.lastInitializedRoot)
        assertEquals(listOf(doc, doc), fakeClient.openedDocuments)
    }

    @Test
    fun `diagnostics stream reflects client emissions`() = runTest {
        val fakeClient = FakeLanguageServerClient()
        val service = DefaultLanguageFeatureProvider(
            rootPathProvider = object : RootPathProvider {
                override fun invoke(): EntryPath = EntryPath.fromString("/workspace/project")
            },
            client = fakeClient
        )

        val doc = LanguageServerDocument("file:///workspace/project/main.dncl", "dncl", "内容")
        service.openDocument(doc)

        val diagnostic = Diagnostic(
            range = io.github.arashiyama11.dncl_ide.language_server.Range(
                start = Position(0, 0),
                end = Position(0, 1)
            ),
            severity = 1,
            message = "error"
        )
        fakeClient.emitDiagnostics(doc.uri, listOf(diagnostic))

        val collected = service.diagnostics(doc.uri).first { it.isNotEmpty() }
        assertEquals(listOf(diagnostic), collected)
    }

    private class FakeLanguageServerClient : LanguageServerClient {
        var initializeCount = 0
            private set
        var lastInitializedRoot: String? = null
            private set
        val openedDocuments = mutableListOf<LanguageServerDocument>()
        private val diagnosticsFlows = mutableMapOf<String, MutableStateFlow<List<Diagnostic>>>()

        fun emitDiagnostics(uri: String, diagnostics: List<Diagnostic>) {
            diagnosticsFlows.getOrPut(uri) { MutableStateFlow(emptyList()) }.value = diagnostics
        }

        override suspend fun initialize(
            rootUri: String?,
            clientCapabilities: ClientCapabilities
        ): ServerCapabilities {
            initializeCount += 1
            lastInitializedRoot = rootUri
            return ServerCapabilities()
        }

        override suspend fun openDocument(document: LanguageServerDocument) {
            openedDocuments += document
        }

        override suspend fun applyChanges(uri: String, text: String) {
        }

        override suspend fun closeDocument(uri: String) {
        }

        override fun observeDiagnostics(uri: String) =
            diagnosticsFlows.getOrPut(uri) { MutableStateFlow(emptyList()) }

        override suspend fun requestCompletion(uri: String, position: Position): CompletionList {
            return CompletionList(isIncomplete = false, items = listOf(CompletionItem(label = "a")))
        }

        override suspend fun requestSemanticTokens(uri: String): SemanticTokens {
            return SemanticTokens(data = emptyList())
        }

        override fun capabilities() = MutableStateFlow<ServerCapabilities?>(null)

        override suspend fun shutdown() {
        }
    }
}
