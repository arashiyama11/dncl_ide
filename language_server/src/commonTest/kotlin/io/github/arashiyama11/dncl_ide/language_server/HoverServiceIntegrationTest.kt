package io.github.arashiyama11.dncl_ide.language_server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HoverServiceIntegrationTest {

    @Test
    fun test_hover_integration_user_variable() {
        // 統合テスト: ユーザー定義変数のホバー情報
        val diagnosticService = DiagnosticService()
        val astInfoService = AstInfoService()
        val hoverService = HoverService(astInfoService)

        val code = """
            x = 10
            表示する(x)
        """.trimIndent()

        // ASTを解析
        astInfoService.parseAndAnalyze(code)

        // 2行目の変数xの位置でホバーテスト
        val xPosition = code.indexOf("x", code.indexOf("表示する"))
        val hover = hoverService.getHover(code, xPosition)

        assertNotNull(hover, "変数xのホバー情報が取得できるはずです")
        assertTrue(
            hover.contents.value.contains("変数"),
            "ホバー内容に「変数」という文字が含まれるはずです"
        )
        assertTrue(hover.contents.value.contains("x"), "ホバー内容に変数名「x」が含まれるはずです")
    }

    @Test
    fun test_hover_integration_user_function() {
        // 統合テスト: ユーザー定義関数のホバー情報
        val diagnosticService = DiagnosticService()
        val astInfoService = AstInfoService()
        val hoverService = HoverService(astInfoService)

        val code = """
            関数 add(a, b) を:
                戻り値(a + b)
            と定義する
            
            res = add(1, 2)
        """.trimIndent()

        // ASTを解析
        astInfoService.parseAndAnalyze(code)

        // 関数呼び出し部分のaddの位置でホバーテスト
        val addPosition = code.indexOf("add", code.indexOf("res"))
        val hover = hoverService.getHover(code, addPosition)
        println("hover: $hover")

        assertNotNull(hover, "関数addのホバー情報が取得できるはずです")
        assertTrue(
            hover.contents.value.contains("関数"),
            "ホバー内容に「関数」という文字が含まれるはずです"
        )
        assertTrue(
            hover.contents.value.contains("add"),
            "ホバー内容に関数名「add」が含まれるはずです"
        )
        assertTrue(
            hover.contents.value.contains("a, b"),
            "ホバー内容にパラメータ「a, b」が含まれるはずです"
        )
    }

    @Test
    fun test_hover_integration_builtin_function() {
        // 統合テスト: 組み込み関数のホバー情報（既存機能の確認）
        val diagnosticService = DiagnosticService()
        val astInfoService = AstInfoService()
        val hoverService = HoverService(astInfoService)

        val code = "表示する(\"Hello World\")"

        // ASTを解析
        astInfoService.parseAndAnalyze(code)

        // 表示関数の位置でホバーテスト
        val displayPosition = code.indexOf("表示する") + 1
        val hover = hoverService.getHover(code, displayPosition)

        assertNotNull(hover, "組み込み関数「表示する」のホバー情報が取得できるはずです")
        assertTrue(
            hover.contents.value.contains("この関数はDNCLの組み込み関数です。"),
            "ホバー内容に組み込み関数の説明が含まれるはずです"
        )
    }
}
