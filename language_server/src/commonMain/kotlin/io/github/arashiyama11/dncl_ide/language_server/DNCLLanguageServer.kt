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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.coroutineContext
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource


internal expect fun logging(message: String)

@OptIn(ExperimentalTime::class)
internal suspend inline fun <T> logTimed(
    name: String,
    docUri: String?,
    requestId: String?,
    crossinline block: suspend () -> T
): T {
    val mark = TimeSource.Monotonic.markNow()
    try {
        return withTimeout(3000) { block() }
    } finally {
        val elapsed: Duration = mark.elapsedNow()
        logging(
            "op=$name uri=$docUri reqId=$requestId durationMs=${elapsed.inWholeNanoseconds / 1_000_000f}ms"
        )
    }
}

internal inline fun <T> traced(name: String, block: () -> T): T {
    val mark = TimeSource.Monotonic.markNow()
    try {
        return block()
    } finally {
        val elapsed: Duration = mark.elapsedNow()
        logging(
            "$name took ${elapsed.inWholeNanoseconds / 1_000_000f}ms"
        )
    }
}

// 使う側
fun doSomething() = traced("doSomething") {
    // 本体
}


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
    private val semanticTokensService: SemanticTokensService,
    private val astInfoService: AstInfoService,
    private val scheduler: DocumentScheduler = DefaultDocumentScheduler(),
    private val debounceMillis: Long? = null
) {
    private val debug = false
    private val state = MutableStateFlow(LsState())
    private val requestMutex = Mutex()
    private val pendingRequests = mutableMapOf<Long, Job>()
    private val cancelledRequestIds = mutableSetOf<Long>()

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
        val currentJob = coroutineContext[Job]
        try {
            val alreadyCancelled = registerRequest(jsonRpcRequest.id, currentJob)
            if (alreadyCancelled) {
                throw CancellationException("Request ${jsonRpcRequest.id} was already cancelled")
            }


            logTimed(
                jsonRpcRequest.method,
                jsonRpcRequest.params?.jsonObject["textDocument"]?.jsonObject["uri"]?.jsonPrimitive.toString(),
                jsonRpcRequest.id.toString()
            ) {
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
                    "$/cancelRequest" -> handleCancelRequest(jsonRpcRequest)
                    else -> if (jsonRpcRequest.method.startsWith("$/") || jsonRpcRequest.id == null) {
                        // とりあえず Do Nothing
                    } else sendErrorResponse(jsonRpcRequest.id, -32601, "Method not found")
                }
            }
        } catch (e: CancellationException) {
            if (jsonRpcRequest.id != null) {
                withContext(NonCancellable) {
                    sendErrorResponse(jsonRpcRequest.id, -32800, "Request cancelled")
                }
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
        } finally {
            withContext(NonCancellable) {
                unregisterRequest(jsonRpcRequest.id, currentJob)
            }
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
            hoverProvider = true,
            definitionProvider = true,
            referencesProvider = true,
            renameProvider = true,
            documentFormattingProvider = true,
            codeActionProvider = true,
            semanticTokensProvider = SemanticTokensOptions(
                legend = SemanticTokensLegend(
                    tokenTypes = listOf(
                        "keyword",
                        "variable",
                        "function",
                        "number",
                        "string",
                        "comment",
                        "operator",
                        "parameter"
                    ),
                    tokenModifiers = listOf("definition", "readonly")
                ),
                full = true,
                range = false
            )
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
            documentManager = documentManager.setDocument(it.uri, it.text, it.version)
            analyzeAndPublish(it.uri, it.text, it.version, requestId = request.id)
        }
    }

    private suspend fun handleDidChange(request: JsonRpcRequest) {
        val params =
            request.params?.let { json.decodeFromJsonElement<DidChangeTextDocumentParams>(it) }
        params?.textDocument?.let { docId ->
            params.contentChanges.firstOrNull()?.let { change ->
                documentManager = documentManager.setDocument(docId.uri, change.text, docId.version)
                analyzeAndPublish(docId.uri, change.text, docId.version, requestId = request.id)
            }
        }
    }

    private suspend fun analyzeAndPublish(
        uri: String,
        text: String,
        version: Int?,
        requestId: Long?,
        debounce: Long? = debounceMillis,
        publishDiagnostics: Boolean = true
    ) {
        if (debounce != null && debounce > 0) {
            delay(debounce)
        }
        val targetVersion = version ?: ((documentManager.getSnapshot(uri)?.version ?: -1) + 1)
        val handle = scheduler.submit(
            DocumentJobRequest(
                uri = uri,
                version = targetVersion,
                kind = JobKind.ParseAndDiagnose,
                priority = JobPriority.UserAction,
                requestId = requestId
            ) {
                val diagnosticResult = diagnosticService.analyze(uri, text)
                val astInfo = diagnosticResult.program?.let {
                    astInfoService.buildAstInfo(
                        it,
                        uri,
                        diagnosticResult.builtInSignatures
                    )
                }
                DocumentAnalysis(
                    diagnostics = diagnosticResult.diagnostics,
                    astInfo = astInfo
                )
            }
        )

        val analysis = try {
            withTimeoutOrNull(2500) {
                handle.await()
            } ?: run {
                logging(
                    """analyzeAndPublish handle.await() timeout.
                    | uri = $uri,
                    | version = $targetVersion,
                    | kind = ${JobKind.ParseAndDiagnose},
                    | priority = ${JobPriority.UserAction},
                    |requestId = $requestId""".trimMargin()
                )
                logging("${scheduler.dumpState()}")
                null
            }
        } catch (_: CancellationException) {
            null
        }

        if (analysis != null) {
            traced("updateAnalysis") {
                documentManager = documentManager.updateAnalysis(uri, analysis)
                if (publishDiagnostics) {
                    sendNotification(
                        "textDocument/publishDiagnostics",
                        PublishDiagnosticsParams(uri, analysis.diagnostics ?: emptyList())
                    )
                }
            }
        }
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

    private suspend fun handleCancelRequest(request: JsonRpcRequest) {
        val params = request.params?.let { json.decodeFromJsonElement<CancelParams>(it) }
        params?.let {
            cancelRequest(it.id)
            scheduler.cancelByRequestId(it.id)
        }
    }

    private suspend fun registerRequest(id: Long?, job: Job?): Boolean {
        if (id == null || job == null) return false
        var alreadyCancelled = false
        requestMutex.withLock {
            alreadyCancelled = cancelledRequestIds.remove(id)
            if (!alreadyCancelled) {
                pendingRequests[id] = job
            }
        }
        return alreadyCancelled
    }

    private suspend fun unregisterRequest(id: Long?, job: Job?) {
        if (id == null || job == null) return
        requestMutex.withLock {
            pendingRequests[id]?.let {
                if (it == job) {
                    pendingRequests.remove(id)
                }
            }
        }
    }

    private suspend fun cancelRequest(id: Long) {
        logging("Cancelling $id")
        val jobToCancel = requestMutex.withLock {
            val job = pendingRequests.remove(id)
            if (job == null) {
                cancelledRequestIds.add(id)
            }
            job
        }
        jobToCancel?.cancel(CancellationException("Cancelled by client"))
        logging("Cancelled $id")
    }

    private suspend fun ensureSnapshotWithAnalysis(
        uri: String,
        requestId: Long?
    ): DocumentSnapshot? {
        val snapshot = documentManager.getSnapshot(uri) ?: return null
        if (snapshot.astInfo != null) return snapshot
        analyzeAndPublish(
            uri = uri,
            text = snapshot.text,
            version = snapshot.version,
            requestId = requestId,
            debounce = null,
            publishDiagnostics = false
        )
        return documentManager.getSnapshot(uri)
    }

    // 共���処理のヘルパ関数を追加
    private suspend inline fun <reified T, R> handleWithDocument(
        request: JsonRpcRequest,
        handler: (snapshot: DocumentSnapshot, offset: Int, params: T) -> R?
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

            val snapshot = ensureSnapshotWithAnalysis(textDocument.uri, request.id) ?: return
            val offset = calculateOffset(snapshot.text, position.line, position.character)
            val result = handler(snapshot, offset, it)

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
        handleWithDocument<CompletionParams, CompletionList>(request) { snapshot, offset, params ->
            val completionItems = completionService.getCompletionItems(
                code = snapshot.text,
                offset = offset,
                filePath = params?.textDocument?.uri,
                astInfo = snapshot.astInfo
            )
            CompletionList(isIncomplete = false, items = completionItems)
        }
    }

    private suspend fun handleHover(request: JsonRpcRequest) {
        handleWithDocument<HoverParams, Hover?>(request) { snapshot, offset, _ ->
            hoverService.getHover(snapshot.text, offset, snapshot.astInfo)
        }
    }

    private suspend fun handleDefinition(request: JsonRpcRequest) {
        handleWithDocument<DefinitionParams, Location?>(request) { snapshot, offset, params ->
            definitionService.getDefinitionLocation(
                params.textDocument.uri,
                snapshot.text,
                offset,
                snapshot.astInfo
            )
        }
    }

    private suspend fun handleReferences(request: JsonRpcRequest) {
        handleWithDocument<ReferenceParams, List<Location>>(request) { snapshot, offset, params ->
            referenceService.findReferences(
                params.textDocument.uri,
                snapshot.text,
                offset,
                params.context.includeDeclaration,
                cachedAstInfo = snapshot.astInfo
            )
        }
    }

    private suspend fun handleRename(request: JsonRpcRequest) {
        handleWithDocument<RenameParams, WorkspaceEdit?>(request) { snapshot, offset, params ->
            renameService.rename(
                params.textDocument.uri,
                snapshot.text,
                offset,
                params.newName,
                cachedAstInfo = snapshot.astInfo
            )
        }
    }

    private suspend fun handleFormatting(request: JsonRpcRequest) {
        val params =
            request.params?.let { json.decodeFromJsonElement<DocumentFormattingParams>(it) }
        params?.let {
            val snapshot = ensureSnapshotWithAnalysis(it.textDocument.uri, request.id) ?: return
            val edits = formattingService.formatDocument(snapshot.text)
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
            val snapshot = ensureSnapshotWithAnalysis(it.textDocument.uri, request.id) ?: return
            val semanticTokens =
                semanticTokensService.getSemanticTokens(snapshot.text, snapshot.astInfo)
            sendResponse(
                request.id,
                json.encodeToJsonElement(SemanticTokens.serializer(), semanticTokens)
            )
        }
    }
}
