package io.github.arashiyama11.dncl_ide.language_server

import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.parser.Parser
import io.github.arashiyama11.dncl_ide.language_server.service.AstInfoService
import kotlin.test.Test

class SimpleDebugTest {

    @Test
    fun test() {
        val astInfoService = AstInfoService()
        val code = """
            関数 myFunc(a, b)を:
                戻り値(a + b)
            と定義する
            
            res = myFunc(1, 2)
        """.trimIndent()

        val program = Parser(Lexer(code)).getOrNull()!!.parseProgram().getOrNull()!!
        println(program.statements[0].range)
    }

    @Test
    fun debug_simple_variable() {
        val astInfoService = AstInfoService()
        val code = "x = 10"

        println("=== DEBUG: Simple Variable Test ===")
        println("Code: '$code'")

        val astInfo = astInfoService.parseAndAnalyze(code)
        println("AstInfo: $astInfo")

        if (astInfo != null) {
            println("AST: ${astInfo.ast}")
            println("Symbol Table: ${astInfo.symbolTable}")

            val allSymbols = astInfo.symbolTable.allSymbols()
            println("All symbols: $allSymbols")

            allSymbols.forEach { symbol ->
                println("Symbol: ${symbol.name}, kind: ${symbol.kind}, range: ${symbol.range}")
            }
        }

        val targetOffset = code.indexOf("x")
        println("Target offset: $targetOffset")

        if (astInfo != null) {
            val symbol = astInfoService.findSymbolAtOffset(astInfo, targetOffset)
            println("Found symbol: $symbol")

            val node = astInfoService.findNodeAtOffset(astInfo, targetOffset)
            println("Found node: $node")
        }
    }

    @Test
    fun debug_function_call() {
        val astInfoService = AstInfoService()
        val code = """
            関数 add(a, b)を:
                戻り値(a + b)
            と定義する
            
            res = add(1, 2)
        """.trimIndent()

        println("=== DEBUG: Function Call Test ===")
        println("Code: '$code'")

        val astInfo = astInfoService.parseAndAnalyze(code)
        println("AstInfo: $astInfo")

        if (astInfo != null) {
            println("AST: ${astInfo.ast}")
            println("Symbol Table: ${astInfo.symbolTable}")

            val allSymbols = astInfo.symbolTable.allSymbols()
            println("All symbols: $allSymbols")

            allSymbols.forEach { symbol ->
                println("Symbol: ${symbol.name}, kind: ${symbol.kind}, range: ${symbol.range}")
            }
        }

        val targetOffset = code.indexOf("add", code.indexOf("res"))
        println("Target offset: $targetOffset")

        if (astInfo != null) {
            val symbol = astInfoService.findSymbolAtOffset(astInfo, targetOffset)
            println("Found symbol: $symbol")
        }
    }

    @Test
    fun debug_parameter_reference() {
        val astInfoService = AstInfoService()
        val code = """
            関数 calc(num1, num2)を:
                res = num1 + num2
                戻り値(res)
            と定義する
        """.trimIndent()

        println("=== DEBUG: Parameter Reference Test ===")
        println("Code: '$code'")

        val astInfo = astInfoService.parseAndAnalyze(code)
        println("AstInfo: $astInfo")

        if (astInfo != null) {
            println("AST: ${astInfo.ast}")
            println("Symbol Table: ${astInfo.symbolTable}")

            val allSymbols = astInfo.symbolTable.allSymbols()
            println("All symbols: $allSymbols")

            allSymbols.forEach { symbol ->
                println("Symbol: ${symbol.name}, kind: ${symbol.kind}, range: ${symbol.range}")
            }
        }

        val targetOffset = code.indexOf("num1", code.indexOf("res ="))
        println("Target offset: $targetOffset")

        if (astInfo != null) {
            val symbol = astInfoService.findSymbolAtOffset(astInfo, targetOffset)
            println("Found symbol: $symbol")
        }
    }
}
