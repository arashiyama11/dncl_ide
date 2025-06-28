package io.github.arashiyama11.dncl_ide.language_server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class HoverServiceTest {

    private fun createServices(): Triple<HoverService, DiagnosticService, AstInfoService> {
        val diagnosticService = DiagnosticService()
        val astInfoService = AstInfoService()
        val hoverService = HoverService(diagnosticService, astInfoService)
        return Triple(hoverService, diagnosticService, astInfoService)
    }

    @Test
    fun test_hover_on_builtin_function() {
        // Red: テストが失敗することを確認
        val (hoverService, _, astInfoService) = createServices()
        val code = "表示する(\"Hello World\")"

        astInfoService.parseAndAnalyze(code)
        val hover = hoverService.getHover(code, 1) // "表示" の位置

        assertNotNull(hover)
        assertEquals("markdown", hover.contents.kind)
        // builtInFunctionDescriptionsに「表示」の説明があることを期待
    }

    @Test
    fun test_hover_on_user_defined_variable() {
        // Red: ユーザー定義変数に対するホバー情報をテスト
        val (hoverService, _, astInfoService) = createServices()
        val code = """
            x = 10
            表示する(x)
        """.trimIndent()

        astInfoService.parseAndAnalyze(code)
        val hover =
            hoverService.getHover(code, code.indexOf("x)", code.indexOf("表示する"))) // 2行目のxの位置

        assertNotNull(hover)
        assertEquals("markdown", hover.contents.kind)
        // 変数xの情報が含まれることを期待
    }

    @Test
    fun test_hover_on_function_definition() {
        // Red: 関数定��に対するホバー情報をテスト
        val (hoverService, _, astInfoService) = createServices()
        val code = """
            関数 myFunc(a, b)を:
                戻り値(a + b)
            と定義する
            
            結果 = myFunc(1, 2)
        """.trimIndent()

        astInfoService.parseAndAnalyze(code)
        val hover = hoverService.getHover(
            code,
            code.indexOf("myFunc", code.indexOf("結果"))
        ) // 関数呼び出しのmyFunc

        assertNotNull(hover)
        assertEquals("markdown", hover.contents.kind)
        // 関数myFuncの情報（パラメータなど）が含まれることを期待
    }

    @Test
    fun test_hover_on_empty_space_returns_null() {
        // Green: 空白部分では何も返さないことをテスト
        val (hoverService, _, astInfoService) = createServices()
        val code = "表示(\"Hello World\")"

        astInfoService.parseAndAnalyze(code)
        val hover = hoverService.getHover(code, code.length - 1) // 文字列の最後の空白部分

        assertNull(hover)
    }

    @Test
    fun test_hover_on_parameter() {
        // Red: 関数パラメータに対するホバー情報をテスト
        val (hoverService, _, astInfoService) = createServices()
        val code = """
            関数 myFunc(param1, param2)を:
                表示する(param1)
            と定義する
        """.trimIndent()

        astInfoService.parseAndAnalyze(code)
        val hover =
            hoverService.getHover(code, code.indexOf("param1", code.indexOf("表示"))) // 関数内のparam1

        assertNotNull(hover)
        assertEquals("markdown", hover.contents.kind)
        // パラメータparam1の情報が含まれることを期待
    }
}
