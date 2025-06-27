package io.github.arashiyama11.dncl_ide.language_server

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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
            listOf("："),
            initializeResult.capabilities.completionProvider.triggerCharacters
        )
        assertEquals(true, initializeResult.capabilities.hoverProvider)
    }
}