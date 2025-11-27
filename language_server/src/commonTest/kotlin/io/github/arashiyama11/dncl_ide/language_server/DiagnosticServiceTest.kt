package io.github.arashiyama11.dncl_ide.language_server

import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class DiagnosticServiceTest {

    @Test
    fun test() = runTest {
        val documentAnalyzerService = DocumentAnalyzerImpl()
        val res = documentAnalyzerService.analyze(
            "test.dncl", """
組み込み関数 hoge()
"""
        )
        println(res)
    }
}