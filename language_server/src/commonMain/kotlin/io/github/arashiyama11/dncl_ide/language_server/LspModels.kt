package io.github.arashiyama11.dncl_ide.language_server

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class JsonRpcRequest(
    val jsonrpc: String = "2.0",
    val id: Long? = null,
    val method: String,
    val params: JsonElement? = null
)

@Serializable
data class JsonRpcResponse(
    val jsonrpc: String = "2.0",
    val id: Long?,
    val result: JsonElement? = null,
    val error: JsonRpcError? = null
)

@Serializable
data class JsonRpcError(
    val code: Int,
    val message: String,
    val data: JsonElement? = null
)

@Serializable
data class InitializeParams(
    val processId: Long?,
    val rootUri: String?,
    val capabilities: ClientCapabilities
)

@Serializable
data class ClientCapabilities(
    val workspace: JsonElement? = null,
    val textDocument: JsonElement? = null,
    val window: JsonElement? = null,
    val general: JsonElement? = null,
    val experimental: JsonElement? = null
)

@Serializable
data class InitializeResult(
    val capabilities: ServerCapabilities
)

@Serializable
data class SemanticTokensParams(
    val textDocument: TextDocumentIdentifier
)

@Serializable
data class SemanticTokens(
    val resultId: String? = null,
    val data: List<Int>
)

@Serializable
data class SemanticTokensLegend(
    val tokenTypes: List<String>,
    val tokenModifiers: List<String>
)

@Serializable
data class SemanticTokensOptions(
    val legend: SemanticTokensLegend,
    val full: Boolean? = null,
    val range: Boolean? = null
)

@Serializable
data class ServerCapabilities(
    val textDocumentSync: Int? = null, // 1 for Full, 2 for Incremental
    val completionProvider: CompletionOptions? = null,
    val hoverProvider: Boolean? = null,
    val semanticTokensProvider: SemanticTokensOptions? = null
)

@Serializable
data class CompletionOptions(
    val resolveProvider: Boolean? = null,
    val triggerCharacters: List<String>? = null
)

@Serializable
data class DidOpenTextDocumentParams(
    val textDocument: TextDocumentItem
)

@Serializable
data class TextDocumentItem(
    val uri: String,
    val languageId: String,
    val version: Int,
    val text: String
)

@Serializable
data class DidChangeTextDocumentParams(
    val textDocument: VersionedTextDocumentIdentifier,
    val contentChanges: List<TextDocumentContentChangeEvent>
)

@Serializable
data class VersionedTextDocumentIdentifier(
    val uri: String,
    val version: Int
)

@Serializable
data class TextDocumentContentChangeEvent(
    val range: Range? = null,
    val rangeLength: Int? = null,
    val text: String
)

@Serializable
data class PublishDiagnosticsParams(
    val uri: String,
    val diagnostics: List<Diagnostic>
)

@Serializable
data class Diagnostic(
    val range: Range,
    val severity: Int, // 1: Error, 2: Warning, 3: Information, 4: Hint
    val message: String,
    val source: String? = null
)

@Serializable
data class Range(
    val start: Position,
    val end: Position
)

@Serializable
data class Position(
    val line: Int,
    val character: Int
)

@Serializable
data class CompletionParams(
    val textDocument: TextDocumentIdentifier,
    val position: Position
)

@Serializable
data class TextDocumentIdentifier(
    val uri: String
)

@Serializable
data class CompletionList(
    val isIncomplete: Boolean,
    val items: List<CompletionItem>
)

@Serializable
data class CompletionItem(
    val label: String,
    val kind: Int? = null, // e.g., 14 for Function
    val detail: String? = null
)

@Serializable
data class CodeActionParams(
    val textDocument: TextDocumentIdentifier,
    val range: Range,
    val context: CodeActionContext
)

@Serializable
data class CodeActionContext(
    val diagnostics: List<Diagnostic>
)

@Serializable
data class CodeAction(
    val title: String,
    val kind: String? = null,
    val diagnostics: List<Diagnostic>? = null,
    val isPreferred: Boolean? = null,
    val edit: WorkspaceEdit? = null,
    val command: Command? = null
)

@Serializable
data class Command(
    val title: String,
    val command: String,
    val arguments: List<JsonElement>? = null
)

@Serializable
data class DocumentFormattingParams(
    val textDocument: TextDocumentIdentifier,
    val options: FormattingOptions
)

@Serializable
data class FormattingOptions(
    val tabSize: Int,
    val insertSpaces: Boolean
)

@Serializable
data class RenameParams(
    val textDocument: TextDocumentIdentifier,
    val position: Position,
    val newName: String
)

@Serializable
data class WorkspaceEdit(
    val changes: Map<String, List<TextEdit>>? = null,
    val documentChanges: List<TextDocumentEdit>? = null
)

@Serializable
data class TextDocumentEdit(
    val textDocument: VersionedTextDocumentIdentifier,
    val edits: List<TextEdit>
)

@Serializable
data class TextEdit(
    val range: Range,
    val newText: String
)

@Serializable
data class ReferenceParams(
    val textDocument: TextDocumentIdentifier,
    val position: Position,
    val context: ReferenceContext
)

@Serializable
data class ReferenceContext(
    val includeDeclaration: Boolean
)

@Serializable
data class DefinitionParams(
    val textDocument: TextDocumentIdentifier,
    val position: Position
)

@Serializable
data class Location(
    val uri: String,
    val range: Range
)

@Serializable
data class HoverParams(
    val textDocument: TextDocumentIdentifier,
    val position: Position
)

@Serializable
data class Hover(
    val contents: MarkupContent,
    val range: Range? = null
)

@Serializable
data class MarkupContent(
    val kind: String, // "plaintext" or "markdown"
    val value: String
)

data class NotebookCellUri(
    val notebookUri: String,
    val cellId: String
) {
    companion object {
        fun parse(uri: String): NotebookCellUri? {
            if (!uri.startsWith("dnclnb://")) return null
            val parts = uri.substringAfter("dnclnb://").split("#cell=")
            if (parts.size != 2) return null
            return NotebookCellUri(notebookUri = "dnclnb://" + parts[0], cellId = parts[1])
        }
    }
}
