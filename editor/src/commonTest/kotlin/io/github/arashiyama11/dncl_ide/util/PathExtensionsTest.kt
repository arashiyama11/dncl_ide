package io.github.arashiyama11.dncl_ide.util

import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import kotlin.test.Test
import kotlin.test.assertEquals

class PathExtensionsTest {
    @Test
    fun `toFileUri prefixes absolute path`() {
        val path = EntryPath.fromString("/Users/test")
        val uri = path.toFileUri()
        assertEquals("file:///Users/test", uri)
    }

    @Test
    fun `toFileUri prefixes relative path`() {
        val path = EntryPath.fromString("projects/dncl")
        val uri = path.toFileUri()
        assertEquals("file:///projects/dncl", uri)
    }
}
