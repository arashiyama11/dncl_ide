package io.github.arashiyama11.dncl_ide.util

import io.github.arashiyama11.dncl_ide.interpreter.api.Stdout
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StdoutTest {

    @Test
    fun `Stdout interface should be accessible`() {
        // Test that we can reference the Stdout interface
        val stdoutClass = Stdout::class
        assertNotNull(stdoutClass)
    }

    @Test
    fun `OutputEvent sealed interface should work correctly`() {
        val appendEvent = OutputEvent.Append("cell1", "text")
        val clearEvent = OutputEvent.Clear("cell2", true)
        val replaceEvent = OutputEvent.Replace("cell3", "replacement")
        val commitEvent = OutputEvent.CommitFrame("cell4")

        // Test that events can be created and have correct properties
        assertEquals("cell1", appendEvent.cellId)
        assertEquals("text", appendEvent.text)

        assertEquals("cell2", clearEvent.cellId)
        assertTrue(clearEvent.immediately)

        assertEquals("cell3", replaceEvent.cellId)
        assertEquals("replacement", replaceEvent.text)

        assertEquals("cell4", commitEvent.cellId)
    }

    @Test
    fun `OutputBroker should be instantiable`() = runTest {
        val eventFlow = kotlinx.coroutines.flow.MutableSharedFlow<OutputEvent>()
        val broker = OutputBroker(eventFlow)

        assertNotNull(broker)
    }

    @Test
    fun `StdoutImpl should be instantiable`() = runTest {
        val eventFlow = kotlinx.coroutines.flow.MutableSharedFlow<OutputEvent>()
        val broker = OutputBroker(eventFlow)
        val stdout = StdoutImpl("test-cell", broker)

        assertNotNull(stdout)
        assertEquals("test-cell", stdout.cellId)
    }

    @Test
    fun `OutputEvent types should be distinguishable`() {
        val events: List<OutputEvent> = listOf(
            OutputEvent.Append("cell1", "text"),
            OutputEvent.Clear("cell2"),
            OutputEvent.Replace("cell3", "replacement"),
            OutputEvent.CommitFrame("cell4")
        )

        assertEquals(4, events.size)

        val appendEvents = events.filterIsInstance<OutputEvent.Append>()
        val clearEvents = events.filterIsInstance<OutputEvent.Clear>()
        val replaceEvents = events.filterIsInstance<OutputEvent.Replace>()
        val commitEvents = events.filterIsInstance<OutputEvent.CommitFrame>()

        assertEquals(1, appendEvents.size)
        assertEquals(1, clearEvents.size)
        assertEquals(1, replaceEvents.size)
        assertEquals(1, commitEvents.size)
    }

    @Test
    fun `string operations should work with output text`() {
        val text1 = "Hello"
        val text2 = "World"
        val combined = "$text1 $text2"

        assertEquals("Hello World", combined)
        assertTrue(combined.contains("Hello"))
        assertTrue(combined.contains("World"))

        val appendEvent = OutputEvent.Append("cell", combined)
        assertEquals(combined, appendEvent.text)
    }

    @Test
    fun `OutputEvent Clear should handle immediately parameter`() {
        val clearImmediate = OutputEvent.Clear("cell1", true)
        val clearNotImmediate = OutputEvent.Clear("cell2", false)
        val clearDefault = OutputEvent.Clear("cell3")

        assertTrue(clearImmediate.immediately)
        assertTrue(!clearNotImmediate.immediately)
        assertTrue(clearDefault.immediately) // Default should be true
    }

    @Test
    fun `cell IDs should be handled correctly`() {
        val events = listOf(
            OutputEvent.Append(null, "global"),
            OutputEvent.Append("cell-1", "cell1 text"),
            OutputEvent.Append("cell-2", "cell2 text")
        )

        val globalEvent = events[0]
        val cell1Event = events[1]
        val cell2Event = events[2]

        assertEquals(null, globalEvent.cellId)
        assertEquals("cell-1", cell1Event.cellId)
        assertEquals("cell-2", cell2Event.cellId)

        assertEquals("global", globalEvent.text)
        assertEquals("cell1 text", cell1Event.text)
        assertEquals("cell2 text", cell2Event.text)
    }
}