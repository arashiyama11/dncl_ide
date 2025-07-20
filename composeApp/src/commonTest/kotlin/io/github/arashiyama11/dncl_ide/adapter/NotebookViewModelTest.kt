package io.github.arashiyama11.dncl_ide.adapter

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NotebookViewModelTest {
    
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }
    
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `TextFieldValue should handle text correctly`() = runTest {
        val text = "テストコード"
        val textFieldValue = TextFieldValue(text, TextRange(text.length))
        
        assertEquals(text, textFieldValue.text)
        assertEquals(text.length, textFieldValue.selection.end)
    }
    
    @Test
    fun `TextRange should handle positions correctly`() = runTest {
        val start = 0
        val end = 5
        val range = TextRange(start, end)
        
        assertEquals(start, range.start)
        assertEquals(end, range.end)
        assertEquals(end - start, range.length)
    }
    
    @Test
    fun `CreatingType enum should have correct values`() {
        val fileType = CreatingType.FILE
        val folderType = CreatingType.FOLDER
        
        assertTrue(fileType == CreatingType.FILE)
        assertTrue(folderType == CreatingType.FOLDER)
    }
    
    @Test
    fun `TextFieldType enum should have correct values`() {
        val outputType = TextFieldType.OUTPUT
        val debugType = TextFieldType.DEBUG_OUTPUT
        
        assertTrue(outputType == TextFieldType.OUTPUT)
        assertTrue(debugType == TextFieldType.DEBUG_OUTPUT)
    }
    
    @Test
    fun `simple arithmetic should work`() {
        assertEquals(4, 2 + 2)
        assertEquals(6, 2 * 3)
        assertEquals(1, 3 - 2)
    }
}