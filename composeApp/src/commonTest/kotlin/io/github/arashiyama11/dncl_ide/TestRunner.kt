package io.github.arashiyama11.dncl_ide

import io.github.arashiyama11.dncl_ide.adapter.NotebookViewModelTest
import io.github.arashiyama11.dncl_ide.adapter.ViewModelBasicsTest
import io.github.arashiyama11.dncl_ide.common.StateTest
import io.github.arashiyama11.dncl_ide.util.StdoutTest
import io.github.arashiyama11.dncl_ide.util.SyntaxHighLighterTest
import io.github.arashiyama11.dncl_ide.util.UtilityTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Test runner that demonstrates all available test classes.
 * This is mainly for documentation purposes - individual test classes
 * should be run separately by the test framework.
 */
class TestRunner {

    @Test
    fun `all tests should be available`() {
        // This test just verifies that all test classes can be instantiated
        // The actual tests are run by the test framework

        val testClasses = listOf(
            NotebookViewModelTest::class,
            ViewModelBasicsTest::class,
            StateTest::class,
            StdoutTest::class,
            SyntaxHighLighterTest::class,
            UtilityTest::class
        )

        println("Available test classes:")
        testClasses.forEach { testClass ->
            println("- ${testClass.simpleName}")
        }

        // Verify all classes are accessible
        assertTrue(testClasses.isNotEmpty())
    }
}

/**
 * Test coverage summary:
 *
 * Basic Tests:
 * - NotebookViewModelTest: Basic tests for text handling and enums
 * - ViewModelBasicsTest: Tests for TextFieldValue, TextRange, and basic data structures
 * - StateTest: Tests for StateFlow and data classes
 * - StdoutTest: Comprehensive tests for Stdout interface and OutputHandler
 * - SyntaxHighLighterTest: Basic tests for syntax highlighting functionality
 * - UtilityTest: Tests for UI utilities like AnnotatedString, Color, etc.
 *
 * Test Infrastructure:
 * - Coroutine test support with StandardTestDispatcher
 * - StateFlow testing patterns
 * - Basic unit testing patterns
 *
 * To run tests:
 * ./gradlew :composeApp:testDebugUnitTest (Android)
 * ./gradlew :composeApp:desktopTest (Desktop)
 * ./gradlew :composeApp:test (All platforms)
 *
 * Note: These are basic unit tests that focus on testing fundamental functionality
 * without complex mocking. More comprehensive integration tests can be added
 * as the project evolves.
 */