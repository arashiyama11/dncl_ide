package io.github.arashiyama11.dncl_ide.language_server

import kotlin.test.Test

class SimpleDebugTest {

    @Test
    fun debug_simple_variable() {
        val astInfoService = AstInfoService()
        val code = "x = 10"

        println("=== DEBUG: Simple Variable Test ===")
        println("Code: '$code'")

        astInfoService.parseAndAnalyze(code)

        val ast = astInfoService.getAst()
        println("AST: $ast")

        val symbolTable = astInfoService.getSymbolTable()
        println("Symbol Table: $symbolTable")

        if (symbolTable != null) {
            val allSymbols = symbolTable.allSymbols()
            println("All symbols: $allSymbols")
        }

        val targetOffset = code.indexOf("x")
        println("Target offset: $targetOffset")

        val symbol = astInfoService.findSymbolAtOffset(targetOffset)
        println("Found symbol: $symbol")
    }

    @Test
    fun debug_function_resolution() {
        val astInfoService = AstInfoService()
        val code = """
            関数 myFunc(a, b)を:
                戻り値(a + b)
            と定義する
            
            res = myFunc(1, 2)
        """.trimIndent()

        println("=== DEBUG: Function Resolution ===")
        println("Code: '$code'")

        astInfoService.parseAndAnalyze(code)

        val ast = astInfoService.getAst()
        println("AST parsed: ${ast != null}")

        val symbolTable = astInfoService.getSymbolTable()
        println("Symbol table created: ${symbolTable != null}")

        if (symbolTable != null) {
            val allSymbols = symbolTable.allSymbols()
            println("All symbols found: ${allSymbols.size}")
            allSymbols.forEach { symbol ->
                println("  Symbol: ${symbol.name}, kind: ${symbol.kind}, range: ${symbol.range}")
            }
        }

        val funcCallPosition = code.indexOf("myFunc", code.indexOf("res"))
        println("Function call position: $funcCallPosition")
        println("Character at position: '${code[funcCallPosition]}'")

        val symbol = astInfoService.findSymbolAtOffset(funcCallPosition)
        println("Found symbol at offset: $symbol")

        val node = astInfoService.findNodeAtOffset(funcCallPosition)
        println("Found node at offset: $node")
    }

    @Test
    fun debug_exact_failing_test() {
        val astInfoService = AstInfoService()
        val code = """
            関数 myFunc(a, b)を:
                戻り値(a + b)
            と定義する
            
            res = myFunc(1, 2)
        """.trimIndent()

        println("=== DEBUG: Exact Failing Test Code ===")
        println("Code: '$code'")
        println("Code length: ${code.length}")

        astInfoService.parseAndAnalyze(code)

        val ast = astInfoService.getAst()
        println("AST parsed: ${ast != null}")

        val symbolTable = astInfoService.getSymbolTable()
        println("Symbol table created: ${symbolTable != null}")

        if (symbolTable != null) {
            val allSymbols = symbolTable.allSymbols()
            println("All symbols found: ${allSymbols.size}")
            allSymbols.forEach { symbol ->
                println("  Symbol: ${symbol.name}, kind: ${symbol.kind}, range: ${symbol.range}")
            }
        }

        val funcCallPosition = code.indexOf("myFunc", code.indexOf("res"))
        println("Function call position: $funcCallPosition")
        println("Character at position: '${if (funcCallPosition < code.length) code[funcCallPosition] else "OUT_OF_BOUNDS"}'")

        val symbol = astInfoService.findSymbolAtOffset(funcCallPosition)
        println("Found symbol at offset: $symbol")
    }
}
