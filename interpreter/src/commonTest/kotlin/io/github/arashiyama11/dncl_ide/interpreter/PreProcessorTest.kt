package io.github.arashiyama11.dncl_ide.interpreter

import arrow.core.firstOrNone
import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.model.BuiltInFunctionSignature
import io.github.arashiyama11.dncl_ide.interpreter.preprocessor.preProcess
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlin.test.assertNotNull

class PreProcessorTest {

    @Test
    fun test() = runTest {
        val program = """
            @インポート("hogehoge")
        """.trimIndent()

        val tokens = preProcess(Lexer(program), resolveLib = { "hoge = 1" }).toList()
        println(tokens)
    }

    @Test
    fun collectBuiltInSignature() = runTest {
        val program = """
            組み込み関数 長さ(x, y)
            a = 1
        """.trimIndent()

        val collected = mutableListOf<BuiltInFunctionSignature>()
        val tokens = preProcess(
            Lexer(program),
            resolveLib = { "" },
            onBuiltInSignature = { collected += it }
        ).toList()

        val literals = tokens.mapNotNull { it.getOrNull()?.literal }
        assertTrue(literals.none { it == "組み込み関数" })
        //assertEquals(1, collected.size)
        val target = collected.firstOrNull { it.name == "長さ" }
        assertNotNull(target)
        assertEquals(listOf("x", "y"), target.params)
    }
}
