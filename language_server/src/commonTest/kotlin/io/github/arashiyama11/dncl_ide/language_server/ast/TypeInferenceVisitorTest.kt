package io.github.arashiyama11.dncl_ide.language_server.ast

import io.github.arashiyama11.dncl_ide.language_server.service.AstInfoService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TypeInferenceVisitorTest {

    private val astInfoService = AstInfoService()

    @Test
    fun `test integer literal type inference`() {
        val code = "a = 123"

        val astInfo = astInfoService.parseAndAnalyze(code)
        assertNotNull(astInfo, "ASTの解析に失敗しました")
        println("SymbolTable after AstVisitor: ${astInfo.symbolTable.allSymbols()}")

        // TypeInferenceVisitorを適用
        val typeVisitor = TypeInferenceVisitor(astInfo.symbolTable)
        typeVisitor.visit(astInfo.ast)

        val symbol = astInfo.symbolTable.resolve("a", code.indexOf("a"), null)
        println("Resolved symbol: $symbol")
        assertNotNull(symbol, "シンボル 'a' が見つかりません")
        assertEquals("Int", symbol.type, "型の推論が正しくありません")
    }
}
