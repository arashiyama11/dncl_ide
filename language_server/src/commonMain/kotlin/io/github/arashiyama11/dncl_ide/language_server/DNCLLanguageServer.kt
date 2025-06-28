package io.github.arashiyama11.dncl_ide.language_server

import io.github.arashiyama11.dncl_ide.language_server.service.AstInfoService
import io.github.arashiyama11.dncl_ide.language_server.service.CodeActionService
import io.github.arashiyama11.dncl_ide.language_server.service.CompletionService
import io.github.arashiyama11.dncl_ide.language_server.service.DefinitionService
import io.github.arashiyama11.dncl_ide.language_server.service.DiagnosticService
import io.github.arashiyama11.dncl_ide.language_server.service.FormattingService
import io.github.arashiyama11.dncl_ide.language_server.service.HoverService
import io.github.arashiyama11.dncl_ide.language_server.service.ReferenceService
import io.github.arashiyama11.dncl_ide.language_server.service.RenameService
import io.github.arashiyama11.dncl_ide.language_server.service.SemanticTokensService
import io.github.arashiyama11.dncl_ide.language_server.util.calculateOffset
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

internal data class LsState(
    val initialized: Boolean = false,
    val rootUri: String? = null,
    val capabilities: ClientCapabilities? = null
)


class DNCLLanguageServer(
    private var documentManager: DocumentManager,
    private val diagnosticService: DiagnosticService,
    private val completionService: CompletionService,
    private val hoverService: HoverService,
    private val definitionService: DefinitionService,
    private val referenceService: ReferenceService,
    private val renameService: RenameService,
    private val formattingService: FormattingService,
    private val codeActionService: CodeActionService,
    private val semanticTokensService: SemanticTokensService
) {
    private val debug = false
    private val state = MutableStateFlow(LsState())

    private val json = Json {
        prettyPrint = debug
        isLenient = false
        ignoreUnknownKeys = true // ignoreUnknownKeysをtrueに戻す
        encodeDefaults = true
        explicitNulls = false
    }
    private val outputChannel = Channel<String>(1024)
    val output: ReceiveChannel<String> = outputChannel

    suspend fun handleMessage(jsonRpcRequest: JsonRpcRequest) {
        try {
            when (jsonRpcRequest.method) {
                "initialize" -> handleInitialize(jsonRpcRequest)
                "initialized" -> handleInitialized()
                "shutdown" -> handleShutdown(jsonRpcRequest)
                "textDocument/didOpen" -> handleDidOpen(jsonRpcRequest)
                "textDocument/didChange" -> handleDidChange(jsonRpcRequest)
                "textDocument/completion" -> handleCompletion(jsonRpcRequest)
                "textDocument/hover" -> handleHover(jsonRpcRequest)
                "textDocument/definition" -> handleDefinition(jsonRpcRequest)
                "textDocument/references" -> handleReferences(jsonRpcRequest)
                "textDocument/rename" -> handleRename(jsonRpcRequest)
                "textDocument/formatting" -> handleFormatting(jsonRpcRequest)
                "textDocument/codeAction" -> handleCodeAction(jsonRpcRequest)
                "textDocument/semanticTokens/full" -> handleSemanticTokensFull(jsonRpcRequest)
                else -> sendErrorResponse(jsonRpcRequest.id, -32601, "Method not found")
            }
        } catch (e: Exception) {
            if (debug) throw e
            outputChannel.send(
                json.encodeToString(
                    JsonRpcErrorResponse(
                        id = jsonRpcRequest.id,
                        error = JsonRpcError(
                            code = -32603,
                            message = "Internal error: ${e.message}"
                        )
                    )
                )
            )
        }
    }

    suspend fun handleMessage(message: String) {
        try {
            val jsonRpcRequest = json.decodeFromString<JsonRpcRequest>(message)
            handleMessage(jsonRpcRequest)
        } catch (e: Exception) {
            // Handle parsing errors or other exceptions
            if (debug) throw e
            outputChannel.send(
                json.encodeToString(
                    JsonRpcErrorResponse(
                        id = null,
                        error = JsonRpcError(
                            code = -32700,
                            message = "Parse error: ${e.message}"
                        )
                    )
                )
            )
        }
    }

    private suspend fun handleInitialize(request: JsonRpcRequest) {
        val params = request.params?.let { json.decodeFromJsonElement<InitializeParams>(it) }

        state.update {
            it.copy(
                initialized = true,
                rootUri = params?.rootUri ?: it.rootUri,
                capabilities = params?.capabilities ?: it.capabilities
            )
        }

        val capabilities = ServerCapabilities(
            textDocumentSync = 1, // Full text document synchronization
            completionProvider = CompletionOptions(
                resolveProvider = false,
                triggerCharacters = listOf(":", "=", "(", "[", " ")
            ),
            hoverProvider = true
        )
        val result = InitializeResult(capabilities)
        sendResponse(request.id, json.encodeToJsonElement(InitializeResult.serializer(), result))
    }

    private suspend fun handleInitialized() {
        // Client has acknowledged initialization
        // No response needed for notifications
    }

    private suspend fun handleShutdown(request: JsonRpcRequest) {
        // No specific action for now, just respond
        sendResponse(request.id, null)
    }

    private suspend fun handleDidOpen(request: JsonRpcRequest) {
        val params =
            request.params?.let { json.decodeFromJsonElement<DidOpenTextDocumentParams>(it) }
        params?.textDocument?.let {
            documentManager = documentManager.setDocument(it.uri, it.text)
            publishDiagnostics(it.uri, it.text)
        }
    }

    private suspend fun handleDidChange(request: JsonRpcRequest) {
        val params =
            request.params?.let { json.decodeFromJsonElement<DidChangeTextDocumentParams>(it) }
        params?.textDocument?.let { docId ->
            params.contentChanges.firstOrNull()?.let { change ->
                documentManager = documentManager.setDocument(docId.uri, change.text)
                publishDiagnostics(docId.uri, change.text)
            }
        }
    }

    private suspend fun publishDiagnostics(uri: String, text: String) {
        val diagnostics = diagnosticService.getDiagnostics(uri, text)
        sendNotification(
            "textDocument/publishDiagnostics",
            PublishDiagnosticsParams(uri, diagnostics)
        )
    }

    private suspend fun sendResponse(id: Long?, result: JsonElement?) {
        val response = JsonRpcResponse(id = id, result = result ?: JsonObject(emptyMap()))
        outputChannel.send(json.encodeToString(response))
    }

    private suspend fun sendErrorResponse(id: Long?, code: Int, message: String) {
        val errorResponse = JsonRpcErrorResponse(
            id = id,
            error = JsonRpcError(code = code, message = message)
        )
        outputChannel.send(json.encodeToString(errorResponse))
    }

    private suspend inline fun <reified T> sendNotification(method: String, params: T) {
        val notification =
            JsonRpcRequest(method = method, params = params?.let { json.encodeToJsonElement(it) })
        outputChannel.send(json.encodeToString(notification))
    }

    // 共通処理のヘルパ関数を追加
    private suspend inline fun <reified T, R> handleWithDocument(
        request: JsonRpcRequest,
        crossinline handler: (code: String, offset: Int, params: T) -> R?
    ) {
        val params = request.params?.let { json.decodeFromJsonElement<T>(it) }
        params?.let {
            val textDocument = when (it) {
                is CompletionParams -> it.textDocument
                is HoverParams -> it.textDocument
                is DefinitionParams -> it.textDocument
                is ReferenceParams -> it.textDocument
                is RenameParams -> it.textDocument
                else -> null
            } ?: return

            val position = when (it) {
                is CompletionParams -> it.position
                is HoverParams -> it.position
                is DefinitionParams -> it.position
                is ReferenceParams -> it.position
                is RenameParams -> it.position
                else -> null
            } ?: return

            val code = documentManager.getDocument(textDocument.uri) ?: return
            val offset = calculateOffset(code, position.line, position.character)
            val result = handler(code, offset, it)

            when (result) {
                is CompletionList -> sendResponse(
                    request.id,
                    json.encodeToJsonElement(CompletionList.serializer(), result)
                )

                is Hover -> sendResponse(
                    request.id,
                    json.encodeToJsonElement(Hover.serializer(), result)
                )

                is Location -> sendResponse(
                    request.id,
                    json.encodeToJsonElement(Location.serializer(), result)
                )

                is List<*> -> {
                    when {
                        result.firstOrNull() is Location -> sendResponse(
                            request.id,
                            json.encodeToJsonElement(
                                ListSerializer(Location.serializer()),
                                result as List<Location>
                            )
                        )

                        else -> sendResponse(request.id, null)
                    }
                }

                is WorkspaceEdit -> sendResponse(
                    request.id,
                    json.encodeToJsonElement(WorkspaceEdit.serializer(), result)
                )

                null -> sendResponse(request.id, null)
                else -> sendResponse(request.id, null)
            }
        }
    }

    private suspend fun handleCompletion(request: JsonRpcRequest) {
        handleWithDocument<CompletionParams, CompletionList>(request) { code, offset, _ ->
            val completionItems = completionService.getCompletionItems(code, offset)
            CompletionList(isIncomplete = false, items = completionItems)
        }
    }

    private suspend fun handleHover(request: JsonRpcRequest) {
        handleWithDocument<HoverParams, Hover?>(request) { code, offset, _ ->
            hoverService.getHover(code, offset)
        }
    }

    private suspend fun handleDefinition(request: JsonRpcRequest) {
        handleWithDocument<DefinitionParams, Location?>(request) { code, offset, params ->
            definitionService.getDefinitionLocation(params.textDocument.uri, code, offset)
        }
    }

    private suspend fun handleReferences(request: JsonRpcRequest) {
        handleWithDocument<ReferenceParams, List<Location>>(request) { code, offset, params ->
            referenceService.findReferences(
                params.textDocument.uri,
                code,
                offset,
                params.context.includeDeclaration
            )
        }
    }

    private suspend fun handleRename(request: JsonRpcRequest) {
        handleWithDocument<RenameParams, WorkspaceEdit?>(request) { code, offset, params ->
            renameService.rename(params.textDocument.uri, code, offset, params.newName)
        }
    }

    private suspend fun handleFormatting(request: JsonRpcRequest) {
        val params =
            request.params?.let { json.decodeFromJsonElement<DocumentFormattingParams>(it) }
        params?.let {
            val code = documentManager.getDocument(it.textDocument.uri) ?: return
            val edits = formattingService.formatDocument(code)
            sendResponse(
                request.id,
                json.encodeToJsonElement(ListSerializer(TextEdit.serializer()), edits)
            )
        }
    }

    private suspend fun handleCodeAction(request: JsonRpcRequest) {
        val params = request.params?.let { json.decodeFromJsonElement<CodeActionParams>(it) }
        params?.let {
            val codeActions =
                codeActionService.getCodeActions(it.textDocument.uri, it.context.diagnostics)
            sendResponse(
                request.id,
                json.encodeToJsonElement(ListSerializer(CodeAction.serializer()), codeActions)
            )
        }
    }

    private suspend fun handleSemanticTokensFull(request: JsonRpcRequest) {
        val params = request.params?.let { json.decodeFromJsonElement<SemanticTokensParams>(it) }
        params?.let {
            val code = documentManager.getDocument(it.textDocument.uri) ?: return
            val semanticTokens = semanticTokensService.getSemanticTokens(code)
            sendResponse(
                request.id,
                json.encodeToJsonElement(SemanticTokens.serializer(), semanticTokens)
            )
        }
    }
}
