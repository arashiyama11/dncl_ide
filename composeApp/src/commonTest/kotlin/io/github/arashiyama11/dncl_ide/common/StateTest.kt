package io.github.arashiyama11.dncl_ide.common

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StateTest {
    
    @Test
    fun `MutableStateFlow should handle state updates`() = runTest {
        val stateFlow = MutableStateFlow("initial")
        
        assertEquals("initial", stateFlow.value)
        assertEquals("initial", stateFlow.first())
        
        stateFlow.value = "updated"
        assertEquals("updated", stateFlow.value)
        assertEquals("updated", stateFlow.first())
    }
    
    @Test
    fun `MutableStateFlow should handle boolean states`() = runTest {
        val booleanState = MutableStateFlow(false)
        
        assertFalse(booleanState.value)
        assertFalse(booleanState.first())
        
        booleanState.value = true
        assertTrue(booleanState.value)
        assertTrue(booleanState.first())
    }
    
    @Test
    fun `MutableStateFlow should handle numeric states`() = runTest {
        val numericState = MutableStateFlow(0)
        
        assertEquals(0, numericState.value)
        assertEquals(0, numericState.first())
        
        numericState.value = 42
        assertEquals(42, numericState.value)
        assertEquals(42, numericState.first())
    }
    
    @Test
    fun `MutableStateFlow should handle nullable states`() = runTest {
        val nullableState = MutableStateFlow<String?>(null)
        
        assertEquals(null, nullableState.value)
        assertEquals(null, nullableState.first())
        
        nullableState.value = "not null"
        assertEquals("not null", nullableState.value)
        assertEquals("not null", nullableState.first())
        
        nullableState.value = null
        assertEquals(null, nullableState.value)
        assertEquals(null, nullableState.first())
    }
    
    @Test
    fun `data class should work correctly`() {
        data class TestState(
            val name: String,
            val count: Int,
            val enabled: Boolean
        )
        
        val state1 = TestState("test", 1, true)
        val state2 = TestState("test", 1, true)
        val state3 = TestState("different", 2, false)
        
        assertEquals(state1, state2)
        assertTrue(state1 != state3)
        
        val updated = state1.copy(count = 5)
        assertEquals("test", updated.name)
        assertEquals(5, updated.count)
        assertEquals(true, updated.enabled)
    }
    
    @Test
    fun `when expression should work correctly`() {
        val value = 42
        
        val result = when {
            value < 0 -> "negative"
            value == 0 -> "zero"
            value > 0 -> "positive"
            else -> "unknown"
        }
        
        assertEquals("positive", result)
        
        val stringValue = "test"
        val stringResult = when (stringValue) {
            "test" -> "found test"
            "other" -> "found other"
            else -> "not found"
        }
        
        assertEquals("found test", stringResult)
    }
}