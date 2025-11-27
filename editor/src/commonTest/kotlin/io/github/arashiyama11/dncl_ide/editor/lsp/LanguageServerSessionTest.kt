package io.github.arashiyama11.dncl_ide.editor.lsp

import io.github.arashiyama11.dncl_ide.language_server.ClientCapabilities
import io.github.arashiyama11.dncl_ide.language_server.Position
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalCoroutinesApi::class)
class LanguageServerSessionTest {
    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)

    @AfterTest
    fun tearDown() {
        scope.cancel()
    }


    //@Test
    fun `openDocument emits diagnostics via observeDiagnostics`() = scope.runTest {
        val session = LanguageServerSession(this)
        session.initialize(rootUri = "file:///", clientCapabilities = ClientCapabilities())

        println(1)
        val uri = "file:///session-diagnostics.dncl"
        session.openDocument(
            LanguageServerDocument(
                uri = uri,
                languageId = "dncl",
                text = "1 @ 2"
            )
        )

        println(2)
        advanceUntilIdle()


        println(3)

        val diagnostics = session.observeDiagnostics(uri).first { it.isNotEmpty() }
        println(4)
        assertTrue(diagnostics.isNotEmpty())
        session.shutdown()
    }

    @Test
    fun `requestCompletion delivers suggestions`() = scope.runTest {
        val session = LanguageServerSession(this)
        session.initialize(rootUri = "file:///", clientCapabilities = ClientCapabilities())

        val uri = "file:///session-completion.dncl"
        session.openDocument(
            LanguageServerDocument(
                uri = uri,
                languageId = "dncl",
                text = ""
            )
        )
        advanceUntilIdle()

        val completion = session.requestCompletion(
            uri = uri,
            position = Position(line = 0, character = 0)
        )
        assertTrue(completion.items.isNotEmpty())
        session.shutdown()
    }
}
