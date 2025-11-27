package io.github.arashiyama11.dncl_ide.language_server

import io.github.arashiyama11.dncl_ide.language_server.service.AstInfoService
import io.github.arashiyama11.dncl_ide.language_server.service.SemanticTokensService
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class SemanticTokensTest {

    @Test
    fun test() = runTest {
        val code = """
            x = 10 + 3
            もし x == 13 ならば:
               表示する("xは13です")
        """.trimIndent()

        val service = SemanticTokensService(AstInfoService())

        val res = service.getSemanticTokens(code)
        println("dline dstart length type mask")
        res.data.chunked(5).forEach { token ->
            println("Token: ${token.joinToString(", ")}")
        }
    }
}
