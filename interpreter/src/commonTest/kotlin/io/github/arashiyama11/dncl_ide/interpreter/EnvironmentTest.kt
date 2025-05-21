package io.github.arashiyama11.dncl_ide.interpreter

import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import io.github.arashiyama11.dncl_ide.interpreter.model.DnclObject
import io.github.arashiyama11.dncl_ide.interpreter.model.Environment
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class EnvironmentTest {

    private val dummyAstNode = AstNode.Identifier("dummy", IntRange(0, 0))

    @Test
    fun testGetSet() = runBlocking {
        val env = Environment()
        val value = DnclObject.Int(10, dummyAstNode)

        assertNull(env.get("x"))

        env.set("x", value)
        val retrievedValue = env.get("x")
        assertNotNull(retrievedValue)
        assertEquals(value, retrievedValue)
    }

    @Test
    fun testOuterEnvironment() = runBlocking {
        val outerEnv = Environment()
        val outerValue = DnclObject.Int(20, dummyAstNode)
        outerEnv.set("y", outerValue)

        val env = Environment(outerEnv)
        val innerValue = DnclObject.Int(30, dummyAstNode)
        env.set("z", innerValue)

        // Get from inner
        assertEquals(innerValue, env.get("z"))
        // Get from outer
        assertEquals(outerValue, env.get("y"))
        // Shadowing: inner has precedence
        val shadowedValue = DnclObject.Int(40, dummyAstNode)
        env.set("y", shadowedValue)
        assertEquals(shadowedValue, env.get("y"))
        // Outer environment is not affected by shadowing in inner
        assertEquals(outerValue, outerEnv.get("y"))

        assertNull(env.get("nonexistent"))
    }

    @Test
    fun testCreateChildEnvironment() = runBlocking {
        val parentEnv = Environment()
        val parentValue = DnclObject.Int(50, dummyAstNode)
        parentEnv.set("a", parentValue)

        val childEnv = parentEnv.createChildEnvironment()
        val childValue = DnclObject.Int(60, dummyAstNode)
        childEnv.set("b", childValue)

        assertEquals(parentValue, childEnv.get("a")) // Can access parent's variable
        assertEquals(childValue, childEnv.get("b"))
        assertNull(parentEnv.get("b")) // Parent cannot access child's variable
    }

    @Test
    fun testCopyEnvironment() = runBlocking {
        val originalEnv = Environment()
        val value1 = DnclObject.Int(70, dummyAstNode)
        originalEnv.set("c", value1)

        val outerForOriginal = Environment()
        val outerValueForOriginal = DnclObject.Int(75, dummyAstNode)
        outerForOriginal.set("outer_c", outerValueForOriginal)
        // Simulate originalEnv having an outer environment for copy testing
        val envWithOuter = Environment(outerForOriginal)
        envWithOuter.set("c", value1) 

        val copiedEnv = envWithOuter.copy()

        // Check copied values
        assertEquals(value1, copiedEnv.get("c"))
        assertEquals(outerValueForOriginal, copiedEnv.get("outer_c")) // Check outer env value

        // Modify copied environment
        val value2 = DnclObject.Int(80, dummyAstNode)
        copiedEnv.set("d", value2)
        copiedEnv.set("c", DnclObject.Int(71, dummyAstNode)) // Modify existing

        // Original environment should not be affected
        assertEquals(value1, envWithOuter.get("c"))
        assertNull(envWithOuter.get("d"))
        assertEquals(outerValueForOriginal, envWithOuter.get("outer_c"))

        // Copied environment should reflect changes
        assertEquals(DnclObject.Int(71, dummyAstNode), copiedEnv.get("c"))
        assertEquals(value2, copiedEnv.get("d"))
    }

    @Test
    fun testAllKeys() = runBlocking {
        val outerEnv = Environment()
        outerEnv.set("key_outer1", DnclObject.Int(1, dummyAstNode))
        outerEnv.set("key_shared", DnclObject.Int(2, dummyAstNode))

        val env = Environment(outerEnv)
        env.set("key_inner1", DnclObject.Int(3, dummyAstNode))
        env.set("key_shared", DnclObject.Int(4, dummyAstNode)) // Shadowing

        val expectedKeys = setOf("key_outer1", "key_inner1", "key_shared")
        assertEquals(expectedKeys, env.allKeys())

        val envNoOuter = Environment()
        envNoOuter.set("key_lonely", DnclObject.Int(5,dummyAstNode))
        assertEquals(setOf("key_lonely"), envNoOuter.allKeys())

        assertEquals(emptySet(), Environment().allKeys())
    }
} 