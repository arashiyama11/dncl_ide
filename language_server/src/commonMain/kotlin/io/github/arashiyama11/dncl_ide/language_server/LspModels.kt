package io.github.arashiyama11.dncl_ide.language_server

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.SerialName

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
)


@Serializable
data class JsonRpcErrorResponse(
    val jsonrpc: String = "2.0",
    val id: Long?,
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
    @SerialName("textDocument") val textDocument: TextDocumentItem
)

@Serializable
data class TextDocumentItem(
    @SerialName("uri") val uri: String,
    @SerialName("languageId") val languageId: String,
    @SerialName("version") val version: Int,
    @SerialName("text") val text: String
)

@Serializable
data class DidChangeTextDocumentParams(
    @SerialName("textDocument") val textDocument: VersionedTextDocumentIdentifier,
    @SerialName("contentChanges") val contentChanges: List<TextDocumentContentChangeEvent>
)

@Serializable
data class VersionedTextDocumentIdentifier(
    @SerialName("uri") val uri: String,
    @SerialName("version") val version: Int
)

@Serializable
data class TextDocumentContentChangeEvent(
    @SerialName("range") val range: Range? = null,
    @SerialName("rangeLength") val rangeLength: Int? = null,
    @SerialName("text") val text: String
)

@Serializable
data class DidCloseTextDocumentParams(
    val textDocument: TextDocumentIdentifier
)

@Serializable
data class PublishDiagnosticsParams(
    @SerialName("uri") val uri: String,
    @SerialName("diagnostics") val diagnostics: List<Diagnostic>
)

@Serializable
data class Diagnostic(
    @SerialName("range") val range: Range,
    @SerialName("severity") val severity: Int, // 1: Error, 2: Warning, 3: Information, 4: Hint
    @SerialName("message") val message: String,
    @SerialName("source") val source: String? = null
)

@Serializable
data class Range(
    @SerialName("start") val start: Position,
    @SerialName("end") val end: Position
)

@Serializable
data class Position(
    @SerialName("line") val line: Int,
    @SerialName("character") val character: Int
)

@Serializable
data class TextDocumentIdentifier(
    @SerialName("uri") val uri: String
)

@Serializable
data class CompletionParams(
    @SerialName("textDocument") val textDocument: TextDocumentIdentifier,
    @SerialName("position") val position: Position
)

@Serializable
data class CompletionList(
    @SerialName("isIncomplete") val isIncomplete: Boolean,
    @SerialName("items") val items: List<CompletionItem>
)

@Serializable
data class CompletionItem(
    @SerialName("label") val label: String,
    @SerialName("kind") val kind: Int? = null,
    @SerialName("detail") val detail: String? = null,
    @SerialName("documentation") val documentation: String? = null
)

@Serializable
data class CodeActionParams(
    @SerialName("textDocument") val textDocument: TextDocumentIdentifier,
    @SerialName("range") val range: Range,
    @SerialName("context") val context: CodeActionContext
)

@Serializable
data class CodeActionContext(
    @SerialName("diagnostics") val diagnostics: List<Diagnostic>
)

@Serializable
data class CodeAction(
    @SerialName("title") val title: String,
    @SerialName("kind") val kind: String? = null,
    @SerialName("diagnostics") val diagnostics: List<Diagnostic>? = null,
    @SerialName("edit") val edit: WorkspaceEdit? = null
)

@Serializable
data class Command(
    val title: String,
    val command: String,
    val arguments: List<JsonElement>? = null
)

@Serializable
data class DocumentFormattingParams(
    @SerialName("textDocument") val textDocument: TextDocumentIdentifier,
    @SerialName("options") val options: FormattingOptions
)

@Serializable
data class FormattingOptions(
    @SerialName("tabSize") val tabSize: Int,
    @SerialName("insertSpaces") val insertSpaces: Boolean
)

@Serializable
data class RenameParams(
    @SerialName("textDocument") val textDocument: TextDocumentIdentifier,
    @SerialName("position") val position: Position,
    @SerialName("newName") val newName: String
)

@Serializable
data class WorkspaceEdit(
    @SerialName("changes") val changes: Map<String, List<TextEdit>>? = null,
    @SerialName("documentChanges") val documentChanges: List<TextDocumentEdit>? = null
)

@Serializable
data class TextDocumentEdit(
    @SerialName("textDocument") val textDocument: VersionedTextDocumentIdentifier,
    @SerialName("edits") val edits: List<TextEdit>
)

@Serializable
data class TextEdit(
    @SerialName("range") val range: Range,
    @SerialName("newText") val newText: String
)

@Serializable
data class ReferenceParams(
    @SerialName("textDocument") val textDocument: TextDocumentIdentifier,
    @SerialName("position") val position: Position,
    @SerialName("context") val context: ReferenceContext
)

@Serializable
data class ReferenceContext(
    @SerialName("includeDeclaration") val includeDeclaration: Boolean
)

@Serializable
data class DefinitionParams(
    @SerialName("textDocument") val textDocument: TextDocumentIdentifier,
    @SerialName("position") val position: Position
)

@Serializable
data class Location(
    @SerialName("uri") val uri: String,
    @SerialName("range") val range: Range
)

@Serializable
data class HoverParams(
    @SerialName("textDocument") val textDocument: TextDocumentIdentifier,
    @SerialName("position") val position: Position
)

@Serializable
data class Hover(
    @SerialName("contents") val contents: MarkupContent,
    @SerialName("range") val range: Range? = null
)

@Serializable
data class MarkupContent(
    @SerialName("kind") val kind: String, // "plaintext" or "markdown"
    @SerialName("value") val value: String
)

// NotebookCellUri utility class
object NotebookCellUri {
    fun parse(uri: String): NotebookCellUriParts? {
        // Simple implementation for notebook cell URI parsing
        // Expected format: vscode-notebook-cell:/path/to/notebook.ipynb#cell-id
        if (!uri.startsWith("vscode-notebook-cell:")) {
            return null
        }

        val parts = uri.split("#")
        if (parts.size != 2) {
            return null
        }

        val notebookUri = parts[0]
        val cellId = parts[1]

        return NotebookCellUriParts(notebookUri, cellId)
    }
}

data class NotebookCellUriParts(
    val notebookUri: String,
    val cellId: String
)
