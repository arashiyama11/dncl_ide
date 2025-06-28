package io.github.arashiyama11.dncl_ide.language_server

import io.github.arashiyama11.dncl_ide.language_server.service.AstInfoService
import io.github.arashiyama11.dncl_ide.language_server.service.DiagnosticService
import io.github.arashiyama11.dncl_ide.language_server.service.HoverService
import kotlin.test.Test

class DebugHoverTest {

    @Test
    fun debug_hover_function() {
        val diagnosticService = DiagnosticService()
        val astInfoService = AstInfoService()
        val hoverService = HoverService(astInfoService)

        val code = """
            関数 myFunc(a, b) を:
                戻り値(a + b)
            と定義する
            
            res = myFunc(1, 2)
        """.trimIndent()

        println("Code to parse:")
        println(code)
        println("Code length: ${code.length}")

        astInfoService.parseAndAnalyze(code)

        val targetIndex = code.indexOf("myFunc", code.indexOf("res"))
        println("Target index: $targetIndex")
        println("Character at target: '${code[targetIndex]}'")

        val hover = hoverService.getHover(code, targetIndex)
        println("Hover result: $hover")

        if (hover != null) {
            println("Hover contents kind: ${hover.contents.kind}")
            println("Hover contents value: ${hover.contents.value}")
        } else {
            println("Hover is null")
        }
    }
}
