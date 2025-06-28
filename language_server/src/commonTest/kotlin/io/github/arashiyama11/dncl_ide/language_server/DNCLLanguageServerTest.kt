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
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DNCLLanguageServerTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `handleMessage processes initialize request and sends capabilities`() = runBlocking {
        val astInfoService = AstInfoService()
        val server = DNCLLanguageServer(
            DocumentManager(),
            DiagnosticService(),
            CompletionService(),
            HoverService(astInfoService),
            DefinitionService(astInfoService),
            ReferenceService(astInfoService),
            RenameService(astInfoService),
            FormattingService(),
            CodeActionService(),
            SemanticTokensService()
        )
        val initializeRequest = JsonRpcRequest(
            id = 1,
            method = "initialize",
            params = json.encodeToJsonElement(
                InitializeParams(
                    processId = null,
                    rootUri = null,
                    capabilities = ClientCapabilities()
                )
            )
        )
        val requestJson = json.encodeToString(initializeRequest)
        server.handleMessage(requestJson)

        val responseJson = server.output.receive()
        val response = json.decodeFromString<JsonRpcResponse>(responseJson)

        assertNotNull(response.id)
        assertEquals(1, response.id)
        assertNotNull(response.result)

        val initializeResult =
            json.decodeFromJsonElement(InitializeResult.serializer(), response.result)
        assertNotNull(initializeResult.capabilities)
        assertEquals(1, initializeResult.capabilities.textDocumentSync)
        assertNotNull(initializeResult.capabilities.completionProvider)
        assertEquals(false, initializeResult.capabilities.completionProvider.resolveProvider)
        assertEquals(
            listOf(":", "=", "(", "[", " "),
            initializeResult.capabilities.completionProvider.triggerCharacters
        )
        assertEquals(true, initializeResult.capabilities.hoverProvider)
    }

    @Test
    fun `handleMessage processes textDocument_formatting request`() = runBlocking {
        val astInfoService = AstInfoService()
        val server = DNCLLanguageServer(
            DocumentManager(),
            DiagnosticService(),
            CompletionService(),
            HoverService(astInfoService),
            DefinitionService(astInfoService),
            ReferenceService(astInfoService),
            RenameService(astInfoService),
            FormattingService(),
            CodeActionService(),
            SemanticTokensService()
        )
        // First, open the document
        val didOpenNotification = JsonRpcRequest(
            method = "textDocument/didOpen",
            params = json.encodeToJsonElement(
                DidOpenTextDocumentParams(
                    textDocument = TextDocumentItem(
                        uri = "file:///a.dncl",
                        languageId = "dncl",
                        version = 1,
                        text = "表示(x)\n  計算(y)\n"
                    )
                )
            )
        )
        server.handleMessage(json.encodeToString(didOpenNotification))
        server.output.receive() // Consume diagnostics from didOpen

        val formattingRequest = JsonRpcRequest(
            id = 8,
            method = "textDocument/formatting",
            params = json.encodeToJsonElement(
                DocumentFormattingParams(
                    textDocument = TextDocumentIdentifier("file:///a.dncl"),
                    options = FormattingOptions(
                        tabSize = 2,
                        insertSpaces = true
                    )
                )
            )
        )
        val requestJson = json.encodeToString(formattingRequest)
        server.handleMessage(requestJson)

        val responseJson = server.output.receive()
        val response = json.decodeFromString<JsonRpcResponse>(responseJson)

        assertNotNull(response.id)
        assertEquals(8, response.id)
        assertNotNull(response.result)

        val textEdits =
            json.decodeFromJsonElement(ListSerializer(TextEdit.serializer()), response.result)
        assertEquals(1, textEdits.size)
        assertEquals("表示(x)\n  計算(y)\n", textEdits.first().newText)
    }

    @Test
    fun `handleMessage processes textDocument_codeAction request`() = runBlocking {
        val astInfoService = AstInfoService()
        val server = DNCLLanguageServer(
            DocumentManager(),
            DiagnosticService(),
            CompletionService(),
            HoverService(astInfoService),
            DefinitionService(astInfoService),
            ReferenceService(astInfoService),
            RenameService(astInfoService),
            FormattingService(),
            CodeActionService(),
            SemanticTokensService()
        )
        // First, open the document with an error
        val didOpenNotification = JsonRpcRequest(
            method = "textDocument/didOpen",
            params = json.encodeToJsonElement(
                DidOpenTextDocumentParams(
                    textDocument = TextDocumentItem(
                        uri = "file:///a.dncl",
                        languageId = "dncl",
                        version = 1,
                        text = "未定義の識別��"
                    )
                )
            )
        )
        server.handleMessage(json.encodeToString(didOpenNotification))
        server.output.receive() // Consume diagnostics from didOpen

        val codeActionRequest = JsonRpcRequest(
            id = 9,
            method = "textDocument/codeAction",
            params = json.encodeToJsonElement(
                CodeActionParams(
                    textDocument = TextDocumentIdentifier("file:///a.dncl"),
                    range = Range(Position(0, 0), Position(0, 10)),
                    context = CodeActionContext(
                        diagnostics = listOf(
                            Diagnostic(
                                range = Range(Position(0, 0), Position(0, 10)),
                                severity = 1,
                                message = "未定義の識別子 未定義の識別子",
                                source = "dncl-ls"
                            )
                        )
                    )
                )
            )
        )
        val requestJson = json.encodeToString(codeActionRequest)
        server.handleMessage(requestJson)

        val responseJson = server.output.receive()
        val response = json.decodeFromString<JsonRpcResponse>(responseJson)

        assertNotNull(response.id)
        assertEquals(9, response.id)
        assertNotNull(response.result)

        val codeActions =
            json.decodeFromJsonElement(ListSerializer(CodeAction.serializer()), response.result)
        assertEquals(1, codeActions.size)
        assertEquals("'未定義の識別子' を表示する", codeActions.first().title)
    }

    @Test
    fun `handleMessage processes textDocument_semanticTokens_full request`() = runBlocking {
        val astInfoService = AstInfoService()
        val server = DNCLLanguageServer(
            DocumentManager(),
            DiagnosticService(),
            CompletionService(),
            HoverService(astInfoService),
            DefinitionService(astInfoService),
            ReferenceService(astInfoService),
            RenameService(astInfoService),
            FormattingService(),
            CodeActionService(),
            SemanticTokensService()
        )
        // First, open the document
        val didOpenNotification = JsonRpcRequest(
            method = "textDocument/didOpen",
            params = json.encodeToJsonElement(
                DidOpenTextDocumentParams(
                    textDocument = TextDocumentItem(
                        uri = "file:///a.dncl",
                        languageId = "dncl",
                        version = 1,
                        text = "表示 x\n計算 y"
                    )
                )
            )
        )
        server.handleMessage(json.encodeToString(didOpenNotification))
        server.output.receive() // Consume diagnostics from didOpen

        val semanticTokensRequest = JsonRpcRequest(
            id = 10,
            method = "textDocument/semanticTokens/full",
            params = json.encodeToJsonElement(
                SemanticTokensParams(
                    textDocument = TextDocumentIdentifier("file:///a.dncl")
                )
            )
        )
        val requestJson = json.encodeToString(semanticTokensRequest)
        server.handleMessage(requestJson)

        val responseJson = server.output.receive()
        val response = json.decodeFromString<JsonRpcResponse>(responseJson)

        assertNotNull(response.id)
        assertEquals(10, response.id)
        assertNotNull(response.result)

        val semanticTokens =
            json.decodeFromJsonElement(SemanticTokens.serializer(), response.result)
        assertTrue(semanticTokens.data.isNotEmpty())
    }

    @Test
    fun `handleMessage processes textDocument_hover request`() = runBlocking {
        val astInfoService = AstInfoService()
        val server = DNCLLanguageServer(
            DocumentManager(),
            DiagnosticService(),
            CompletionService(),
            HoverService(astInfoService),
            DefinitionService(astInfoService),
            ReferenceService(astInfoService),
            RenameService(astInfoService),
            FormattingService(),
            CodeActionService(),
            SemanticTokensService()
        )
        // First, open the document
        val didOpenNotification = JsonRpcRequest(
            method = "textDocument/didOpen",
            params = json.encodeToJsonElement(
                DidOpenTextDocumentParams(
                    textDocument = TextDocumentItem(
                        uri = "file:///a.dncl",
                        languageId = "dncl",
                        version = 1,
                        text = "表示する(x)"
                    )
                )
            )
        )
        server.handleMessage(json.encodeToString(didOpenNotification))
        server.output.receive() // Consume diagnostics from didOpen

        val hoverRequest = JsonRpcRequest(
            id = 11,
            method = "textDocument/hover",
            params = json.encodeToJsonElement(
                HoverParams(
                    textDocument = TextDocumentIdentifier("file:///a.dncl"),
                    position = Position(0, 1) // Position inside "表示する"
                )
            )
        )
        val requestJson = json.encodeToString(hoverRequest)
        server.handleMessage(requestJson)

        val responseJson = server.output.receive()
        val response = json.decodeFromString<JsonRpcResponse>(responseJson)

        assertNotNull(response.id)
        assertEquals(11, response.id)
        assertNotNull(response.result)

        val hover = json.decodeFromJsonElement(Hover.serializer(), response.result)
        assertNotNull(hover.contents)
        assertTrue(hover.contents.value.contains("表示する"))
    }

    @Test
    fun `handleMessage processes textDocument_hover request for user-defined variable`() =
        runBlocking {
            val astInfoService = AstInfoService()
            val server = DNCLLanguageServer(
                DocumentManager(),
                DiagnosticService(),
                CompletionService(),
                HoverService(astInfoService),
                DefinitionService(astInfoService),
                ReferenceService(astInfoService),
                RenameService(astInfoService),
                FormattingService(),
                CodeActionService(),
                SemanticTokensService()
            )
            // First, open the document
            val didOpenNotification = JsonRpcRequest(
                method = "textDocument/didOpen",
                params = json.encodeToJsonElement(
                    DidOpenTextDocumentParams(
                        textDocument = TextDocumentItem(
                            uri = "file:///b.dncl",
                            languageId = "dncl",
                            version = 1,
                            text = "x = 10\n表示する(x)"
                        )
                    )
                )
            )
            server.handleMessage(json.encodeToString(didOpenNotification))
            server.output.receive() // Consume diagnostics from didOpen

            val hoverRequest = JsonRpcRequest(
                id = 12,
                method = "textDocument/hover",
                params = json.encodeToJsonElement(
                    HoverParams(
                        textDocument = TextDocumentIdentifier("file:///b.dncl"),
                        position = Position(1, 5) // Position at 'x' in "表示 x"
                    )
                )
            )
            val requestJson = json.encodeToString(hoverRequest)
            server.handleMessage(requestJson)

            val responseJson = server.output.receive()
            val response = json.decodeFromString<JsonRpcResponse>(responseJson)

            assertNotNull(response.id)
            assertEquals(12, response.id)
            val hover = if (response.result != null) {
                json.decodeFromJsonElement(Hover.serializer(), response.result)
            } else {
                null
            }
            assertTrue(
                hover?.contents?.value?.contains("**変数**: `x`") ?: false,
                "ホバー内容に変数名「x」が含まれるはずです"
            )
            assertTrue(
                hover?.contents?.value?.contains("定義位置:") ?: false,
                "ホバー内容に定義位置が含まれるはずです"
            )
        }

//    @Test
//    fun `handleMessage processes textDocument_completion request`() = runBlocking {
//        val server = DNCLLanguageServer()
//        // First, open the document
//        val didOpenNotification = JsonRpcRequest(
//            method = "textDocument/didOpen",
//            params = json.encodeToJsonElement(
//                DidOpenTextDocumentParams(
//                    textDocument = TextDocumentItem(
//                        uri = "file:///a.dncl",
//                        languageId = "dncl",
//                        version = 1,
//                        text = "表示 "
//                    )
//                )
//            )
//        )
//        server.handleMessage(json.encodeToString(didOpenNotification))
//        server.output.receive() // Consume diagnostics from didOpen
//
//        val completionRequest = JsonRpcRequest(
//            id = 12,
//            method = "textDocument/completion",
//            params = json.encodeToJsonElement(
//                CompletionParams(
//                    textDocument = TextDocumentIdentifier("file:///a.dncl"),
//                    position = Position(0, 3) // Position after "表示 "
//                )
//            )
//        )
//        val requestJson = json.encodeToString(completionRequest)
//        server.handleMessage(requestJson)
//
//        val responseJson = server.output.receive()
//        val response = json.decodeFromString<JsonRpcResponse>(responseJson)
//
//        assertNotNull(response.id)
//        assertEquals(12, response.id)
//        assertNotNull(response.result)
//
//        val completionList =
//            json.decodeFromJsonElement(CompletionList.serializer(), response.result)
//        assertNotNull(completionList.items)
//        assertTrue(completionList.items.isNotEmpty())
//    }

    @Test
    fun `handleMessage processes textDocument_didChange request and publishes diagnostics`() =
        runBlocking {
            val astInfoService = AstInfoService()
            val server = DNCLLanguageServer(
                DocumentManager(),
                DiagnosticService(),
                CompletionService(),
                HoverService(astInfoService),
                DefinitionService(astInfoService),
                ReferenceService(astInfoService),
                RenameService(astInfoService),
                FormattingService(),
                CodeActionService(),
                SemanticTokensService()
            )
            // First, open the document
            val didOpenNotification = JsonRpcRequest(
                method = "textDocument/didOpen",
                params = json.encodeToJsonElement(
                    DidOpenTextDocumentParams(
                        textDocument = TextDocumentItem(
                            uri = "file:///a.dncl",
                            languageId = "dncl",
                            version = 1,
                            text = "表示 x"
                        )
                    )
                )
            )
            server.handleMessage(json.encodeToString(didOpenNotification))
            server.output.receive() // Consume diagnostics from didOpen

            // Then, change the document to introduce an error
            val didChangeNotification = JsonRpcRequest(
                method = "textDocument/didChange",
                params = json.encodeToJsonElement(
                    DidChangeTextDocumentParams(
                        textDocument = VersionedTextDocumentIdentifier(
                            uri = "file:///a.dncl",
                            version = 2
                        ),
                        contentChanges = listOf(
                            TextDocumentContentChangeEvent(
                                text = "1 @ 2"
                            )
                        )
                    )
                )
            )
            server.handleMessage(json.encodeToString(didChangeNotification))

            val diagnosticsNotification = server.output.receive()
            println("notice $diagnosticsNotification")
            val notification = json.decodeFromString<JsonRpcRequest>(diagnosticsNotification)

            assertEquals("textDocument/publishDiagnostics", notification.method)
            assertNotNull(notification.params)

            val diagnosticsParams = json.decodeFromJsonElement(
                PublishDiagnosticsParams.serializer(),
                notification.params!!
            )
            assertEquals("file:///a.dncl", diagnosticsParams.uri)
            assertTrue(diagnosticsParams.diagnostics.isNotEmpty())
        }

    @Test
    fun `handleMessage processes textDocument_definition request`() = runBlocking {
        val astInfoService = AstInfoService()
        val server = DNCLLanguageServer(
            DocumentManager(),
            DiagnosticService(),
            CompletionService(),
            HoverService(astInfoService),
            DefinitionService(astInfoService),
            ReferenceService(astInfoService),
            RenameService(astInfoService),
            FormattingService(),
            CodeActionService(),
            SemanticTokensService()
        )
        // First, open the document with a definition and its usage
        val didOpenNotification = JsonRpcRequest(
            method = "textDocument/didOpen",
            params = json.encodeToJsonElement(
                DidOpenTextDocumentParams(
                    textDocument = TextDocumentItem(
                        uri = "file:///a.dncl",
                        languageId = "dncl",
                        version = 1,
                        text = "変数 x = 10\n表示 x"
                    )
                )
            )
        )
        server.handleMessage(json.encodeToString(didOpenNotification))
        server.output.receive() // Consume diagnostics from didOpen

        val definitionRequest = JsonRpcRequest(
            id = 13,
            method = "textDocument/definition",
            params = json.encodeToJsonElement(
                DefinitionParams(
                    textDocument = TextDocumentIdentifier("file:///a.dncl"),
                    position = Position(1, 3) // Position at x in "表示 x"
                )
            )
        )
        val requestJson = json.encodeToString(definitionRequest)
        server.handleMessage(requestJson)

        val responseJson = server.output.receive()
        val response = json.decodeFromString<JsonRpcResponse>(responseJson)

        assertNotNull(response.id)
        assertEquals(13, response.id)
        // For now, we just ensure there is some response - in a real system this would point to the definition
        // The actual definition finding functionality would need to be tested more thoroughly
    }

//    @Test
//    fun `handleMessage processes textDocument_references request`() = runBlocking {
//        val server = DNCLLanguageServer()
//        // First, open the document with multiple occurrences of the same identifier
//        val didOpenNotification = JsonRpcRequest(
//            method = "textDocument/didOpen",
//            params = json.encodeToJsonElement(
//                DidOpenTextDocumentParams(
//                    textDocument = TextDocumentItem(
//                        uri = "file:///a.dncl",
//                        languageId = "dncl",
//                        version = 1,
//                        text = "変数 total = 0\ntotal = total + 10\n表示 total"
//                    )
//                )
//            )
//        )
//        server.handleMessage(json.encodeToString(didOpenNotification))
//        server.output.receive() // Consume diagnostics from didOpen
//
//        val referencesRequest = JsonRpcRequest(
//            id = 14,
//            method = "textDocument/references",
//            params = json.encodeToJsonElement(
//                ReferenceParams(
//                    textDocument = TextDocumentIdentifier("file:///a.dncl"),
//                    position = Position(0, 3), // Position at "total" in first line
//                    context = ReferenceContext(includeDeclaration = true)
//                )
//            )
//        )
//        val requestJson = json.encodeToString(referencesRequest)
//        server.handleMessage(requestJson)
//
//        val responseJson = server.output.receive()
//        val response = json.decodeFromString<JsonRpcResponse>(responseJson)
//
//        assertNotNull(response.id)
//        assertEquals(14, response.id)
//        assertNotNull(response.result)
//
//        val references =
//            json.decodeFromJsonElement(ListSerializer(Location.serializer()), response.result)
//        assertEquals(3, references.size) // Should find all 3 occurrences
//    }
//
//    @Test
//    fun `handleMessage processes textDocument_rename request`() = runBlocking {
//        val server = DNCLLanguageServer()
//        // First, open the document with multiple occurrences of the same identifier
//        val didOpenNotification = JsonRpcRequest(
//            method = "textDocument/didOpen",
//            params = json.encodeToJsonElement(
//                DidOpenTextDocumentParams(
//                    textDocument = TextDocumentItem(
//                        uri = "file:///a.dncl",
//                        languageId = "dncl",
//                        version = 1,
//                        text = "変数 count = 0\ncount = count + 1\n表示 count"
//                    )
//                )
//            )
//        )
//        server.handleMessage(json.encodeToString(didOpenNotification))
//        server.output.receive() // Consume diagnostics from didOpen
//
//        val renameRequest = JsonRpcRequest(
//            id = 15,
//            method = "textDocument/rename",
//            params = json.encodeToJsonElement(
//                RenameParams(
//                    textDocument = TextDocumentIdentifier("file:///a.dncl"),
//                    position = Position(0, 3), // Position at "count" in first line
//                    newName = "counter"
//                )
//            )
//        )
//        val requestJson = json.encodeToString(renameRequest)
//        server.handleMessage(requestJson)
//
//        val responseJson = server.output.receive()
//        val response = json.decodeFromString<JsonRpcResponse>(responseJson)
//
//        assertNotNull(response.id)
//        assertEquals(15, response.id)
//        assertNotNull(response.result)
//
//        val workspaceEdit = json.decodeFromJsonElement(WorkspaceEdit.serializer(), response.result)
//        assertNotNull(workspaceEdit.documentChanges)
//        assertEquals(1, workspaceEdit.documentChanges?.size) // Should have changes for one document
//        val documentEdit = workspaceEdit.documentChanges?.first() as? TextDocumentEdit
//        assertNotNull(documentEdit)
//        assertEquals(3, documentEdit.edits.size) // Should have 3 edits (one per occurrence)
//        assertEquals("counter", documentEdit.edits.first().newText)
//    }
}