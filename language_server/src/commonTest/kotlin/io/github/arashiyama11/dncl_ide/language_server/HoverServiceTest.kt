package io.github.arashiyama11.dncl_ide.language_server

import io.github.arashiyama11.dncl_ide.language_server.service.AstInfoService
import io.github.arashiyama11.dncl_ide.language_server.service.DiagnosticService
import io.github.arashiyama11.dncl_ide.language_server.service.HoverService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class HoverServiceTest {

    private fun createServices(): Triple<HoverService, DiagnosticService, AstInfoService> {
        val diagnosticService = DiagnosticService()
        val astInfoService = AstInfoService()
        val hoverService = HoverService(astInfoService)
        return Triple(hoverService, diagnosticService, astInfoService)
    }

    @Test
    fun test_hover_on_builtin_function() = runTest {
        val (hoverService, diagnosticService, astInfoService) = createServices()

        val code = "表示する(x)"
        val hover = hoverService.getHover(code, 1) // 「表示」の位置

        assertNotNull(hover)
        assertNotNull(hover.contents)
        assertEquals("markdown", hover.contents.kind)
    }

    @Test
    fun test_hover_on_user_defined_variable() = runTest {
        // Red: ユーザー定義変数に対するホバー情報をテスト
        val (hoverService, _, astInfoService) = createServices()
        val code = """
            x = 10
            表示する(x)
        """.trimIndent()

        val hover =
            hoverService.getHover(code, code.indexOf("x)", code.indexOf("表示する"))) // 2行目のxの位置

        assertNotNull(hover)
        assertEquals("markdown", hover.contents.kind)
        // 変数xの情報が含まれることを期待
    }

    @Test
    fun test_hover_on_function_definition() = runTest {
        // Red: 関数定義に対するホバー情報をテスト
        val (hoverService, _, astInfoService) = createServices()
        val code = """
            関数 myFunc(a, b) を:
                戻り値(a + b)
            と定義する
            
            res = myFunc(1, 2)
        """.trimIndent()

        val hover = hoverService.getHover(
            code,
            code.indexOf("myFunc", code.indexOf("res"))
        ) // 関数呼び出しのmyFunc

        assertNotNull(hover)
        assertEquals("markdown", hover.contents.kind)
        // 関数myFuncの情報（パラメータなど）が含まれることを期待
    }

    @Test
    fun test_hover_on_empty_space_returns_null() = runTest {
        // Green: 空白部分では何も返さないことをテスト
        val (hoverService, _, astInfoService) = createServices()
        val code = "表示(\"Hello World\")"

        val hover = hoverService.getHover(code, code.length - 1) // 文字列の最後の空白部分

        assertNull(hover)
    }

    @Test
    fun test_hover_on_parameter() = runTest {
        // Red: 関数パラメータに対するホバー情報をテスト
        val (hoverService, _, astInfoService) = createServices()
        val code = """
            関数 myFunc(param1, param2) を:
                表示する(param1)
            と定義する
        """.trimIndent()

        val hover =
            hoverService.getHover(code, code.indexOf("param1", code.indexOf("表示"))) // 関数内のparam1

        println("Hover result: $hover")
        assertNotNull(hover)
        assertEquals("markdown", hover.contents.kind)
        // パラメータparam1の情報が含まれることを期待
    }

    @Test
    fun expandWithLeadingComments_includes_consecutive_hash_lines() = runTest {
        val (hoverService, _, _) = createServices()
        val src = """
            # コメント1
            # コメント2
            関数 foo() を:
                戻り値(1)
            と定義する
        """.trimIndent()
        val defStart = src.indexOf("関数")
        val defEnd = src.indexOf("定義する") + "定義する".length
        val original = defStart..defEnd

        val expanded = hoverService.expandWithLeadingComments(src, original)

        // コメント行を含んで開始位置が前に広がることを確認
        kotlin.test.assertTrue(expanded.first < original.first, "コメント分だけ開始が前に広がるはず")
        val prefix = src.substring(expanded.first, original.first)
        kotlin.test.assertTrue(prefix.contains("# コメント1"), "コメント1を含むはず")
        kotlin.test.assertTrue(prefix.contains("# コメント2"), "コメント2を含むはず")
        assertEquals(original.last, expanded.last)
    }

    @Test
    fun expandWithLeadingComments_stops_at_blank_line() = runTest {
        val (hoverService, _, _) = createServices()
        val src = """
            # コメント1

            関数 bar() を:
                戻り値(1)
            と定義する
        """.trimIndent()
        val defStart = src.indexOf("関数")
        val defEnd = src.indexOf("定義する") + "定義する".length
        val original = defStart..defEnd

        val expanded = hoverService.expandWithLeadingComments(src, original)

        assertEquals(original.first, expanded.first)
        assertEquals(original.last, expanded.last)
    }
}
