package io.github.arashiyama11.dncl_ide.language_server

import io.github.arashiyama11.dncl_ide.domain.usecase.SuggestionUseCase
import arrow.core.Either
import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.model.DnclError
import io.github.arashiyama11.dncl_ide.interpreter.model.Token
import io.github.arashiyama11.dncl_ide.interpreter.parser.Parser
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.internal.throwMissingFieldException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

internal data class LsState(
    val initialized: Boolean = false,
    val rootUri: String? = null,
    val capabilities: ClientCapabilities? = null
)


class DNCLLanguageServer(
    private val documentManager: DocumentManager,
    private val diagnosticService: DiagnosticService,
    private val completionService: CompletionService,
    private val hoverService: HoverService,
    private val definitionService: DefinitionService,
    private val referenceService: ReferenceService,
    private val renameService: RenameService,
    private val formattingService: FormattingService,
    private val codeActionService: CodeActionService,
    private val semanticTokensService: SemanticTokensService,
    private val astInfoService: AstInfoService
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
    private val documentContents = mutableMapOf<String, MutableMap<String, String>>()

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
            documentManager.setDocument(it.uri, it.text)
            astInfoService.parseAndAnalyze(it.text)
            publishDiagnostics(it.uri, it.text)
        }
    }

    private suspend fun handleDidChange(request: JsonRpcRequest) {
        val params =
            request.params?.let { json.decodeFromJsonElement<DidChangeTextDocumentParams>(it) }
        params?.textDocument?.let { docId ->
            params.contentChanges.firstOrNull()?.let { change ->
                documentManager.setDocument(docId.uri, change.text)
                astInfoService.parseAndAnalyze(change.text)
                publishDiagnostics(docId.uri, change.text)
            }
        }
    }

    private suspend fun publishDiagnostics(uri: String, text: String) {
        val diagnostics = DiagnosticService().getDiagnostics(uri, text)
        sendNotification(
            "textDocument/publishDiagnostics",
            PublishDiagnosticsParams(uri, diagnostics)
        )
    }

    

    private suspend fun sendResponse(id: Long?, result: JsonElement?) {
        val response = JsonRpcResponse(id = id, result = result)
        outputChannel.send(json.encodeToString(response))
    }

    private suspend inline fun <reified T> sendNotification(method: String, params: T) {
        val notification =
            JsonRpcRequest(method = method, params = params?.let { json.encodeToJsonElement(it) })
        outputChannel.send(json.encodeToString(notification))
    }

    private suspend fun handleCompletion(request: JsonRpcRequest) {
        val params = request.params?.let { json.decodeFromJsonElement<CompletionParams>(it) }
        params?.let {
            val code = documentManager.getDocument(it.textDocument.uri) ?: return
            val offset = documentManager.calculateOffset(code, it.position.line, it.position.character)
            val completionItems = completionService.getCompletionItems(code, offset)
            sendResponse(
                request.id,
                json.encodeToJsonElement(
                    CompletionList.serializer(),
                    CompletionList(isIncomplete = false, items = completionItems)
                )
            )
        }
    }

    

    private suspend fun handleHover(request: JsonRpcRequest) {
        val params = request.params?.let { json.decodeFromJsonElement<HoverParams>(it) }
        params?.let {
            val code = documentManager.getDocument(it.textDocument.uri) ?: return
            val offset = documentManager.calculateOffset(code, it.position.line, it.position.character)

            val hover = hoverService.getHover(code, offset)
            if (hover != null) {
                sendResponse(request.id, json.encodeToJsonElement(Hover.serializer(), hover))
            } else {
                sendResponse(request.id, null)
            }
        }
    }

    private suspend fun handleDefinition(request: JsonRpcRequest) {
        val params = request.params?.let { json.decodeFromJsonElement<DefinitionParams>(it) }
        params?.let {
            val code = documentManager.getDocument(it.textDocument.uri) ?: return
            val offset = documentManager.calculateOffset(code, it.position.line, it.position.character)

            val definitionLocation = definitionService.getDefinitionLocation(it.textDocument.uri, code, offset)
            sendResponse(
                request.id,
                definitionLocation?.let { json.encodeToJsonElement(Location.serializer(), it) })
        }
    }

    private suspend fun handleReferences(request: JsonRpcRequest) {
        val params = request.params?.let { json.decodeFromJsonElement<ReferenceParams>(it) }
        params?.let {
            val code = documentManager.getDocument(it.textDocument.uri) ?: return
            val offset = documentManager.calculateOffset(code, it.position.line, it.position.character)

            val references = referenceService.getReferences(it.textDocument.uri, code, offset)
            sendResponse(
                request.id,
                json.encodeToJsonElement(ListSerializer(Location.serializer()), references)
            )
        }
    }

    private suspend fun handleRename(request: JsonRpcRequest) {
        val params = request.params?.let { json.decodeFromJsonElement<RenameParams>(it) }
        params?.let {
            val code = documentManager.getDocument(it.textDocument.uri) ?: return
            val offset = documentManager.calculateOffset(code, it.position.line, it.position.character)
            val newName = it.newName

            val workspaceEdit = renameService.getRenameEdits(it.textDocument.uri, code, offset, newName)
            if (workspaceEdit != null) {
                sendResponse(
                    request.id,
                    json.encodeToJsonElement(WorkspaceEdit.serializer(), workspaceEdit)
                )
            } else {
                sendResponse(request.id, null)
            }
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
            val codeActions = codeActionService.getCodeActions(it.textDocument.uri, it.context.diagnostics)
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

    

    

    private suspend fun sendErrorResponse(id: Long?, code: Int, message: String) {
        println("[Info] Sending error response: code=$code, message=$message")
        val error = JsonRpcError(code = code, message = message)
        val response = JsonRpcErrorResponse(id = id, error = error)
        outputChannel.send(json.encodeToString(response))
    }
}

