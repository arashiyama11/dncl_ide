package io.github.arashiyama11.dncl_ide.language_server

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
        val server = DNCLLanguageServer()
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
        val server = DNCLLanguageServer()
        // First, open the document
        val didOpenNotification = JsonRpcRequest(
            method = "textDocument/didOpen",
            params = json.encodeToJsonElement(
                DidOpenTextDocumentParams(
                    textDocument = TextDocumentItem(
                        uri = "file:///a.dncl",
                        languageId = "dncl",
                        version = 1,
                        text = "表示 x\n  計算 y\n終わり"
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
        assertEquals("表示 x\n計算 y\n終わり", textEdits.first().newText)
    }

    @Test
    fun `handleMessage processes textDocument_codeAction request`() = runBlocking {
        val server = DNCLLanguageServer()
        // First, open the document with an error
        val didOpenNotification = JsonRpcRequest(
            method = "textDocument/didOpen",
            params = json.encodeToJsonElement(
                DidOpenTextDocumentParams(
                    textDocument = TextDocumentItem(
                        uri = "file:///a.dncl",
                        languageId = "dncl",
                        version = 1,
                        text = "未定義の識別子"
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
        val server = DNCLLanguageServer()
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
}