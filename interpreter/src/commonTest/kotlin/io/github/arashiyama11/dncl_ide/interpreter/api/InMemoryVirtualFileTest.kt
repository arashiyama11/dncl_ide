package io.github.arashiyama11.dncl_ide.interpreter.api

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class InMemoryVirtualFileTest {

    @Test
    fun writeBytes_appendsAndCanReadAsString() = runTest {
        val file = InMemoryVirtualFile(path = "/tmp/test", initialContent = "abc")

        file.write(byteArrayOf(0x64, 0x65, 0x66)) // "def"

        assertEquals("abcdef", file.read())
    }

    @Test
    fun clear_removesAllContent() = runTest {
        val file = InMemoryVirtualFile(path = "/tmp/test", initialContent = "abc")
        file.clear()
        assertEquals("", file.read())
    }

    @Test
    fun read_unsupportedStillThrowsWhenOverrideMissing() = runTest {
        val unsupported = object : VirtualFile {
            override val path: String = "/tmp/nope"
            override suspend fun write(text: String) { }
        }
        assertFailsWith<UnsupportedOperationException> { unsupported.read() }
    }
}
