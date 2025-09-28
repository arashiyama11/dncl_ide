package io.github.arashiyama11.dncl_ide.editor.lsp

import io.github.arashiyama11.dncl_ide.language_server.CompletionList
import io.github.arashiyama11.dncl_ide.language_server.Position
import io.github.arashiyama11.dncl_ide.language_server.SemanticTokens
import io.github.arashiyama11.dncl_ide.language_server.ClientCapabilities
import io.github.arashiyama11.dncl_ide.util.RootPathProvider
import io.github.arashiyama11.dncl_ide.util.toFileUri
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.Volatile

class DefaultLanguageFeatureProvider(
    private val rootPathProvider: RootPathProvider,
    private val client: LanguageServerClient
) : LanguageFeatureProvider {
    private val initializeMutex = Mutex()

    @Volatile
    private var initialized = false

    private suspend fun ensureInitialized() {
        if (initialized) return
        initializeMutex.withLock {
            if (!initialized) {
                val rootUri = rootPathProvider().toFileUri()
                client.initialize(rootUri, ClientCapabilities())
                initialized = true
            }
        }
    }

    override suspend fun openDocument(document: LanguageServerDocument) {
        ensureInitialized()
        client.openDocument(document)
    }

    override suspend fun applyChanges(uri: String, text: String) {
        ensureInitialized()
        client.applyChanges(uri, text)
    }

    override suspend fun closeDocument(uri: String) {
        if (!initialized) return
        client.closeDocument(uri)
    }

    override fun diagnostics(uri: String) = client.observeDiagnostics(uri)

    override suspend fun requestCompletion(uri: String, position: Position): CompletionList {
        ensureInitialized()
        return client.requestCompletion(uri, position)
    }

    override suspend fun requestSemanticTokens(uri: String): SemanticTokens {
        ensureInitialized()
        return client.requestSemanticTokens(uri)
    }

    override val capabilities = client.capabilities()
}
