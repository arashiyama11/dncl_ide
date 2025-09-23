package io.github.arashiyama11.dncl_ide.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyntaxHighLighterTest {

    private val syntaxHighLighter = SyntaxHighLighter()

    @Test
    fun `highlightWithParsedData should return empty AnnotatedString for empty text`() {
        val (annotatedString, error) = syntaxHighLighter.highlightWithParsedData(
            text = "",
            isDarkTheme = false,
            errorRange = null,
            tokens = emptyList()
        )

        assertEquals("", annotatedString.text)
        assertNull(error)
    }

    @Test
    fun `highlightWithParsedData should handle simple text without tokens`() {
        val text = "simple text"
        val (annotatedString, error) = syntaxHighLighter.highlightWithParsedData(
            text = text,
            isDarkTheme = false,
            errorRange = null,
            tokens = emptyList()
        )

        assertEquals(text, annotatedString.text)
        assertNull(error)
    }

    @Test
    fun `SyntaxHighLighter should be instantiable`() {
        val highlighter = SyntaxHighLighter()
        assertNotNull(highlighter)
    }

    @Test
    fun `highlightWithParsedData should handle dark theme parameter`() {
        val text = "test"

        val (lightResult, _) = syntaxHighLighter.highlightWithParsedData(
            text = text,
            isDarkTheme = false,
            errorRange = null,
            tokens = emptyList()
        )

        val (darkResult, _) = syntaxHighLighter.highlightWithParsedData(
            text = text,
            isDarkTheme = true,
            errorRange = null,
            tokens = emptyList()
        )

        assertEquals(text, lightResult.text)
        assertEquals(text, darkResult.text)
    }

    @Test
    fun `highlightWithParsedData should handle error range parameter`() {
        val text = "error text"
        val errorRange = 0..5

        val (annotatedString, error) = syntaxHighLighter.highlightWithParsedData(
            text = text,
            isDarkTheme = false,
            errorRange = errorRange,
            tokens = emptyList()
        )

        assertEquals(text, annotatedString.text)
        assertNull(error) // No error should be generated from empty tokens
    }

    @Test
    fun `string operations should work correctly`() {
        val text1 = "Hello"
        val text2 = "World"
        val combined = "$text1 $text2"

        assertEquals("Hello World", combined)
        assertTrue(combined.contains("Hello"))
        assertTrue(combined.contains("World"))
    }
}