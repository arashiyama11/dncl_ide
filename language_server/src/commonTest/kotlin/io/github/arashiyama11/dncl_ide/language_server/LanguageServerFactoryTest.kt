package io.github.arashiyama11.dncl_ide.language_server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement

class LanguageServerFactoryTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    //@Test
    fun createLanguageServer_emitsDiagnosticsOnDidOpen() = runTest {
        val server = createLanguageServer()

        val params = DidOpenTextDocumentParams(
            textDocument = TextDocumentItem(
                uri = "file:///factory-test.dncl",
                languageId = "dncl",
                version = 0,
                text = "1 @ 2"
            )
        )
        val request = JsonRpcRequest(
            id = null,
            method = "textDocument/didOpen",
            params = json.encodeToJsonElement(params)
        )

        server.handleMessage(request)

        val rawNotification: String = withTimeout(1_000L) {
            server.output.receive()
        }
        val notification = json.decodeFromString<JsonRpcRequest>(rawNotification)
        assertEquals("textDocument/publishDiagnostics", notification.method)
        val diagnostics =
            json.decodeFromJsonElement(PublishDiagnosticsParams.serializer(), notification.params!!)
        assertEquals("file:///factory-test.dncl", diagnostics.uri)
        assertTrue(diagnostics.diagnostics.isNotEmpty())
    }
}
