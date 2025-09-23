package io.github.arashiyama11.dncl_ide.language_server.util

import kotlin.test.Test
import kotlin.test.assertEquals

class OffsetUtilTest {

    @Test
    fun test_calculateLineAndCharacter_singleLine() {
        val code = "Hello World"
        val (line, char) = calculatePosition(code, 6) // 'W'
        assertEquals(0, line)
        assertEquals(6, char)
    }

    @Test
    fun test_calculateLineAndCharacter_multiLine() {
        val code = """
            Line 1
            Line 2
            Line 3
        """.trimIndent()

        // Position at '2' in "Line 2"
        val (line, char) = calculatePosition(
            code,
            code.indexOf("2", code.indexOf("Line 2"))
        )
        assertEquals(1, line)
        assertEquals(5, char) // '2' is at index 5 in "Line 2"
    }

    @Test
    fun test_calculateLineAndCharacter_startOfLine() {
        val code = """
            Line 1
            Line 2
        """.trimIndent()
        val (line, char) = calculatePosition(code, code.indexOf("Line 2"))
        assertEquals(1, line)
        assertEquals(0, char)
    }

    @Test
    fun test_calculateLineAndCharacter_endOfLine() {
        val code = """
            Line 1
            Line 2
        """.trimIndent()
        val (line, char) = calculatePosition(
            code,
            code.indexOf("1") + 1
        ) // After '1' in "Line 1"
        assertEquals(0, line)
        assertEquals(6, char) // After '1'
    }

    @Test
    fun test_calculateLineAndCharacter_withCRLF() {
        val code = "Line 1\r\nLine 2"
        val (line, char) = calculatePosition(code, code.indexOf("Line 2"))
        assertEquals(1, line)
        assertEquals(1, char)
    }

    @Test
    fun test_calculateLineAndCharacter_atEndOfFile() {
        val code = "Hello"
        val (line, char) = calculatePosition(code, code.length)
        assertEquals(0, line)
        assertEquals(5, char)
    }
}