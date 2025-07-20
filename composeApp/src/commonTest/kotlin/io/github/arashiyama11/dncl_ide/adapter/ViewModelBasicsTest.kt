package io.github.arashiyama11.dncl_ide.adapter

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ViewModelBasicsTest {
    
    @Test
    fun `TextFieldValue should handle text and selection correctly`() {
        val text = "Hello, World!"
        val selection = TextRange(0, 5)
        val textFieldValue = TextFieldValue(text, selection)
        
        assertEquals(text, textFieldValue.text)
        assertEquals(selection, textFieldValue.selection)
        assertEquals(0, textFieldValue.selection.start)
        assertEquals(5, textFieldValue.selection.end)
    }
    
    @Test
    fun `TextRange should calculate length correctly`() {
        val range1 = TextRange(0, 10)
        val range2 = TextRange(5, 15)
        val range3 = TextRange(3)
        
        assertEquals(10, range1.length)
        assertEquals(10, range2.length)
        assertEquals(0, range3.length) // Collapsed range
    }
    
    @Test
    fun `CreatingType enum should have correct values`() {
        val fileType = CreatingType.FILE
        val folderType = CreatingType.FOLDER
        
        assertTrue(fileType == CreatingType.FILE)
        assertTrue(folderType == CreatingType.FOLDER)
        assertFalse(fileType == folderType)
    }
    
    @Test
    fun `TextFieldType enum should have correct values`() {
        val outputType = TextFieldType.OUTPUT
        val debugType = TextFieldType.DEBUG_OUTPUT
        
        assertTrue(outputType == TextFieldType.OUTPUT)
        assertTrue(debugType == TextFieldType.DEBUG_OUTPUT)
        assertFalse(outputType == debugType)
    }
    
    @Test
    fun `string manipulation should work correctly`() {
        val original = "Hello World"
        val substring = original.substring(0, 5)
        val replaced = original.replace("World", "Kotlin")
        
        assertEquals("Hello", substring)
        assertEquals("Hello Kotlin", replaced)
        assertTrue(original.contains("World"))
        assertFalse(original.contains("Kotlin"))
    }
    
    @Test
    fun `list operations should work correctly`() {
        val list = listOf("a", "b", "c")
        val mutableList = mutableListOf("x", "y", "z")
        
        assertEquals(3, list.size)
        assertEquals("a", list.first())
        assertEquals("c", list.last())
        
        mutableList.add("w")
        assertEquals(4, mutableList.size)
        assertTrue(mutableList.contains("w"))
    }
    
    @Test
    fun `map operations should work correctly`() {
        val map = mapOf("key1" to "value1", "key2" to "value2")
        val mutableMap = mutableMapOf("a" to 1, "b" to 2)
        
        assertEquals("value1", map["key1"])
        assertEquals(2, map.size)
        assertTrue(map.containsKey("key1"))
        assertFalse(map.containsKey("key3"))
        
        mutableMap["c"] = 3
        assertEquals(3, mutableMap.size)
        assertEquals(3, mutableMap["c"])
    }
}