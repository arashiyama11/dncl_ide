package io.github.arashiyama11.dncl_ide.editor.lsp

import io.github.arashiyama11.dncl_ide.language_server.CompletionList
import io.github.arashiyama11.dncl_ide.language_server.Diagnostic
import io.github.arashiyama11.dncl_ide.language_server.Hover
import io.github.arashiyama11.dncl_ide.language_server.Position
import io.github.arashiyama11.dncl_ide.language_server.SemanticTokens
import io.github.arashiyama11.dncl_ide.language_server.ServerCapabilities
import kotlinx.coroutines.flow.Flow

interface LanguageFeatureProvider {
    suspend fun openDocument(document: LanguageServerDocument)
    suspend fun applyChanges(uri: String, text: String)
    suspend fun closeDocument(uri: String)
    fun diagnostics(uri: String): Flow<List<Diagnostic>>
    suspend fun requestCompletion(uri: String, position: Position): CompletionList
    suspend fun requestSemanticTokens(uri: String): SemanticTokens
    suspend fun requestHover(uri: String, position: Position): Hover?
    val capabilities: Flow<ServerCapabilities?>
}

/**
 * LSP に渡すドキュメント情報。`languageId` は LSP 側でサポートされる言語識別子。
 */
data class LanguageServerDocument(
    val uri: String,
    val languageId: String,
    val text: String
)
