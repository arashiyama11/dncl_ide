package io.github.arashiyama11.dncl_ide.language

import io.github.arashiyama11.dncl_ide.language_server.ClientCapabilities
import io.github.arashiyama11.dncl_ide.language_server.CompletionList
import io.github.arashiyama11.dncl_ide.language_server.Diagnostic
import io.github.arashiyama11.dncl_ide.language_server.Position
import io.github.arashiyama11.dncl_ide.language_server.SemanticTokens
import io.github.arashiyama11.dncl_ide.language_server.ServerCapabilities
import kotlinx.coroutines.flow.Flow

interface LanguageServerClient {
    suspend fun initialize(rootUri: String?, clientCapabilities: ClientCapabilities): ServerCapabilities
    suspend fun openDocument(document: LanguageServerDocument)
    suspend fun applyChanges(uri: String, text: String)
    suspend fun closeDocument(uri: String)
    fun observeDiagnostics(uri: String): Flow<List<Diagnostic>>
    suspend fun requestCompletion(uri: String, position: Position): CompletionList
    suspend fun requestSemanticTokens(uri: String): SemanticTokens
    fun capabilities(): Flow<ServerCapabilities?>
    suspend fun shutdown()
}
