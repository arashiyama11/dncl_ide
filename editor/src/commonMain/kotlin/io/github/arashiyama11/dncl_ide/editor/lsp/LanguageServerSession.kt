package io.github.arashiyama11.dncl_ide.editor.lsp

import io.github.arashiyama11.dncl_ide.language_server.ClientCapabilities
import io.github.arashiyama11.dncl_ide.language_server.CompletionList
import io.github.arashiyama11.dncl_ide.language_server.CompletionParams
import io.github.arashiyama11.dncl_ide.language_server.DNCLLanguageServer
import io.github.arashiyama11.dncl_ide.language_server.DidChangeTextDocumentParams
import io.github.arashiyama11.dncl_ide.language_server.DidCloseTextDocumentParams
import io.github.arashiyama11.dncl_ide.language_server.DidOpenTextDocumentParams
import io.github.arashiyama11.dncl_ide.language_server.Diagnostic
import io.github.arashiyama11.dncl_ide.language_server.InitializeParams
import io.github.arashiyama11.dncl_ide.language_server.InitializeResult
import io.github.arashiyama11.dncl_ide.language_server.JsonRpcErrorResponse
import io.github.arashiyama11.dncl_ide.language_server.JsonRpcRequest
import io.github.arashiyama11.dncl_ide.language_server.JsonRpcResponse
import io.github.arashiyama11.dncl_ide.language_server.Position
import io.github.arashiyama11.dncl_ide.language_server.PublishDiagnosticsParams
import io.github.arashiyama11.dncl_ide.language_server.SemanticTokens
import io.github.arashiyama11.dncl_ide.language_server.SemanticTokensParams
import io.github.arashiyama11.dncl_ide.language_server.ServerCapabilities
import io.github.arashiyama11.dncl_ide.language_server.TextDocumentContentChangeEvent
import io.github.arashiyama11.dncl_ide.language_server.TextDocumentIdentifier
import io.github.arashiyama11.dncl_ide.language_server.TextDocumentItem
import io.github.arashiyama11.dncl_ide.language_server.VersionedTextDocumentIdentifier
import io.github.arashiyama11.dncl_ide.language_server.createLanguageServer
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * `DNCLLanguageServer` をアプリ内からシンプルに扱うためのセッション層。
 * クライアントからのリクエストとサーバ通知の仲介を担う。
 */
class LanguageServerSession(
    private val scope: CoroutineScope,
    private val serverFactory: () -> DNCLLanguageServer = ::createLanguageServer,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }
) : LanguageServerClient {
    private val server: DNCLLanguageServer = serverFactory()

    private val requestId = atomic(0L)
    private val pendingMutex = Mutex()
    private val pendingResponses = mutableMapOf<Long, CompletableDeferred<JsonElement?>>()

    private val documentVersions = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val diagnosticsState = MutableStateFlow<Map<String, List<Diagnostic>>>(emptyMap())
    private val semanticTokensState = MutableStateFlow<Map<String, SemanticTokens>>(emptyMap())
    private val capabilitiesState = MutableStateFlow<ServerCapabilities?>(null)

    private val outputJob: Job = scope.launch {
        server.output.consumeEach { payload ->
            handleServerPayload(payload)
        }
    }

    override suspend fun initialize(
        rootUri: String?,
        clientCapabilities: ClientCapabilities
    ): ServerCapabilities {
        val params = InitializeParams(
            processId = null,
            rootUri = rootUri,
            capabilities = clientCapabilities
        )
        val response = sendRequest(
            method = "initialize",
            params = json.encodeToJsonElement(InitializeParams.serializer(), params)
        )
        val result = json.decodeFromJsonElement(
            InitializeResult.serializer(),
            response ?: JsonObject(emptyMap())
        )
        capabilitiesState.value = result.capabilities

        sendNotification(method = "initialized", params = null)
        return result.capabilities
    }

    override suspend fun openDocument(document: LanguageServerDocument) {
        documentVersions.update { current -> current + (document.uri to 0) }
        val params = DidOpenTextDocumentParams(
            textDocument = TextDocumentItem(
                uri = document.uri,
                languageId = document.languageId,
                version = 0,
                text = document.text
            )
        )
        sendNotification(
            method = "textDocument/didOpen",
            params = json.encodeToJsonElement(DidOpenTextDocumentParams.serializer(), params)
        )
    }

    override suspend fun applyChanges(uri: String, text: String) {
        val nextVersion = incrementVersion(uri)
        val params = DidChangeTextDocumentParams(
            textDocument = VersionedTextDocumentIdentifier(uri = uri, version = nextVersion),
            contentChanges = listOf(TextDocumentContentChangeEvent(text = text))
        )
        sendNotification(
            method = "textDocument/didChange",
            params = json.encodeToJsonElement(DidChangeTextDocumentParams.serializer(), params)
        )
    }

    override suspend fun closeDocument(uri: String) {
        val params = DidCloseTextDocumentParams(TextDocumentIdentifier(uri))
        sendNotification(
            method = "textDocument/didClose",
            params = json.encodeToJsonElement(DidCloseTextDocumentParams.serializer(), params)
        )
        documentVersions.update { it - uri }
        diagnosticsState.update { it - uri }
        semanticTokensState.update { it - uri }
    }

    override fun observeDiagnostics(uri: String): Flow<List<Diagnostic>> {
        return diagnosticsState
            .map { it[uri] ?: emptyList() }
            .distinctUntilChanged()
    }

    fun observeSemanticTokens(uri: String): Flow<SemanticTokens?> {
        return semanticTokensState
            .map { it[uri] }
            .distinctUntilChanged()
    }

    override suspend fun requestCompletion(uri: String, position: Position): CompletionList {
        val params = CompletionParams(
            textDocument = TextDocumentIdentifier(uri),
            position = position
        )
        val response = sendRequest(
            method = "textDocument/completion",
            params = json.encodeToJsonElement(CompletionParams.serializer(), params)
        ) ?: JsonObject(emptyMap())
        return json.decodeFromJsonElement(CompletionList.serializer(), response)
    }

    override suspend fun requestSemanticTokens(uri: String): SemanticTokens {
        val params = SemanticTokensParams(textDocument = TextDocumentIdentifier(uri))
        val response = sendRequest(
            method = "textDocument/semanticTokens/full",
            params = json.encodeToJsonElement(SemanticTokensParams.serializer(), params)
        ) ?: JsonObject(emptyMap())
        val tokens = json.decodeFromJsonElement(SemanticTokens.serializer(), response)
        semanticTokensState.update { current -> current + (uri to tokens) }
        return tokens
    }

    override fun capabilities(): Flow<ServerCapabilities?> = capabilitiesState

    override suspend fun shutdown() {
        runCatching { sendRequest(method = "shutdown", params = null) }
        outputJob.cancel()
        pendingMutex.withLock {
            pendingResponses.values.forEach { it.cancel() }
            pendingResponses.clear()
        }
    }

    private suspend fun handleServerPayload(payload: String) {
        val element = json.parseToJsonElement(payload)
        val obj = element.jsonObject
        when {
            obj.containsKey("method") -> handleNotification(element)
            obj.containsKey("error") -> handleError(element)
            else -> handleResponse(element)
        }
    }

    private suspend fun handleNotification(element: JsonElement) {
        val request = json.decodeFromJsonElement(JsonRpcRequest.serializer(), element)
        if (request.method == "textDocument/publishDiagnostics") {
            val params = request.params?.let {
                json.decodeFromJsonElement(PublishDiagnosticsParams.serializer(), it)
            } ?: return
            diagnosticsState.update { current -> current + (params.uri to params.diagnostics) }
        }
    }

    private suspend fun handleResponse(element: JsonElement) {
        val response = json.decodeFromJsonElement(JsonRpcResponse.serializer(), element)
        val id = response.id ?: return
        val deferred = pendingMutex.withLock { pendingResponses.remove(id) }
        deferred?.complete(response.result)
    }

    private suspend fun handleError(element: JsonElement) {
        val response = json.decodeFromJsonElement(JsonRpcErrorResponse.serializer(), element)
        val id = response.id ?: return
        val deferred = pendingMutex.withLock { pendingResponses.remove(id) }
        deferred?.completeExceptionally(
            IllegalStateException(
                response.error?.message ?: "Unknown error"
            )
        )
    }

    private suspend fun sendRequest(method: String, params: JsonElement?): JsonElement? {
        val id = requestId.incrementAndGet()
        val deferred = CompletableDeferred<JsonElement?>()
        pendingMutex.withLock {
            pendingResponses[id] = deferred
        }
        server.handleMessage(JsonRpcRequest(id = id, method = method, params = params))
        return deferred.await()
    }

    private suspend fun sendNotification(method: String, params: JsonElement?) {
        server.handleMessage(JsonRpcRequest(id = null, method = method, params = params))
    }

    private fun incrementVersion(uri: String): Int {
        var nextVersion = 1
        documentVersions.update { current ->
            val currentVersion = current[uri] ?: 0
            nextVersion = currentVersion + 1
            current + (uri to nextVersion)
        }
        return nextVersion
    }
}
