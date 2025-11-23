package io.github.arashiyama11.dncl_ide.language_server.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class CompletionServiceTest {
    private val completionService = CompletionService()

    @Test
    fun `builtin print appears first when prefix matches`() = runTest {
        val code = "表示"
        val result = completionService.getCompletionItems(
            code = code,
            offset = code.length,
            filePath = "file:///test.dncl"
        )
        assertEquals("表示する", result.first().label)
    }

    @Test
    fun `keyword appears first when no prefix`() = runTest {
        val code = ""
        val result = completionService.getCompletionItems(code = code, offset = 0, filePath = "file:///test.dncl")
        assertEquals("もし", result.first().label)
    }

    @Test
    fun `keyword exact match outranks others`() = runTest {
        val code = "関数"
        val result = completionService.getCompletionItems(code = code, offset = code.length, filePath = "file:///test.dncl")
        println(result.take(5).map { it.label })
        assertEquals("関数", result.first().label)
    }

    @Test
    fun `user defined function outranks builtin for same prefix`() = runTest {
        val code = """
            関数 表示用() を:
              表示する("test")
            と定義する

            表
        """.trimIndent()
        val result = completionService.getCompletionItems(
            code = code,
            offset = code.length,
            filePath = "file:///test.dncl"
        )
        assertEquals("表示用", result.first().label)
    }
}
