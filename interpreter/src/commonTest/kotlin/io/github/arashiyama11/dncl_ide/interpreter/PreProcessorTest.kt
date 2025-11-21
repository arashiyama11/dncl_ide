package io.github.arashiyama11.dncl_ide.interpreter

import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.preprocessor.preProcess
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class PreProcessorTest {

    @Test
    fun test() = runTest {
        val program = """
            @インポート("hogehoge")
        """.trimIndent()

        val tokens = preProcess(Lexer(program)) { "hoge = 1" }.toList()
        println(tokens)
    }
}