package io.github.arashiyama11.dncl_ide.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UtilityTest {
    
    @Test
    fun `AnnotatedString should handle basic text`() {
        val text = "Hello, World!"
        val annotatedString = AnnotatedString(text)
        
        assertEquals(text, annotatedString.text)
        assertTrue(annotatedString.spanStyles.isEmpty())
    }
    
    @Test
    fun `AnnotatedString should handle styled text`() {
        val text = "Styled Text"
        val annotatedString = buildAnnotatedString {
            withStyle(SpanStyle(color = Color.Red, fontWeight = FontWeight.Bold)) {
                append(text)
            }
        }
        
        assertEquals(text, annotatedString.text)
        assertTrue(annotatedString.spanStyles.isNotEmpty())
        assertEquals(1, annotatedString.spanStyles.size)
    }
    
    @Test
    fun `SpanStyle should handle color and font weight`() {
        val style = SpanStyle(
            color = Color.Blue,
            fontWeight = FontWeight.Bold
        )
        
        assertEquals(Color.Blue, style.color)
        assertEquals(FontWeight.Bold, style.fontWeight)
    }
    
    @Test
    fun `Color should have predefined values`() {
        assertNotNull(Color.Red)
        assertNotNull(Color.Green)
        assertNotNull(Color.Blue)
        assertNotNull(Color.Black)
        assertNotNull(Color.White)
        assertNotNull(Color.Gray)
    }
    
    @Test
    fun `FontWeight should have predefined values`() {
        assertNotNull(FontWeight.Normal)
        assertNotNull(FontWeight.Bold)
        assertNotNull(FontWeight.Light)
        assertNotNull(FontWeight.Medium)
    }
    
    @Test
    fun `buildAnnotatedString should create complex styled text`() {
        val annotatedString = buildAnnotatedString {
            withStyle(SpanStyle(color = Color.Red)) {
                append("Red ")
            }
            withStyle(SpanStyle(color = Color.Blue, fontWeight = FontWeight.Bold)) {
                append("Blue Bold ")
            }
            append("Normal")
        }
        
        assertEquals("Red Blue Bold Normal", annotatedString.text)
        assertEquals(2, annotatedString.spanStyles.size)
    }
    
    @Test
    fun `IntRange should work correctly`() {
        val range1 = 0..10
        val range2 = 5..15
        val range3 = IntRange(3, 8)
        
        assertTrue(5 in range1)
        assertTrue(10 in range2)
        assertTrue(6 in range3)
        
        assertEquals(0, range1.first)
        assertEquals(10, range1.last)
        assertEquals(11, range1.count())
    }
    
    @Test
    fun `String operations should work with Japanese text`() {
        val japanese = "こんにちは世界"
        val english = "Hello World"
        
        assertEquals(7, japanese.length)
        assertEquals(11, english.length)
        
        assertTrue(japanese.contains("こんにちは"))
        assertTrue(english.contains("Hello"))
        
        val combined = "$japanese $english"
        assertEquals("こんにちは世界 Hello World", combined)
    }
}