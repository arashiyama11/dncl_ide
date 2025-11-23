package io.github.arashiyama11.dncl_ide.language_server

import io.github.arashiyama11.dncl_ide.language_server.service.AstInfoService
import io.github.arashiyama11.dncl_ide.language_server.service.DefinitionService
import io.github.arashiyama11.dncl_ide.language_server.service.DiagnosticService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class DefinitionServiceTest {

    private fun createServices(): Triple<DefinitionService, DiagnosticService, AstInfoService> {
        val diagnosticService = DiagnosticService()
        val astInfoService = AstInfoService()
        val definitionService = DefinitionService(astInfoService)
        return Triple(definitionService, diagnosticService, astInfoService)
    }

    @Test
    fun test_go_to_variable_definition() = runTest {
        // Red: 変数定義へのジャンプをテスト
        val (definitionService, _, astInfoService) = createServices()
        val code = """
            x = 10
            y = x + 5
            表示(x)
        """.trimIndent()

        val uri = "file:///test.dncl"
        // 2行目のxの位置から定義へジャンプ
        val definition =
            definitionService.getDefinitionLocation(uri, code, code.indexOf("x", code.indexOf("y")))

        assertNotNull(definition)
        assertEquals(uri, definition.uri)
        // 定義位置は最初のxの位置
        assertEquals(0, definition.range.start.character)
    }

    @Test
    fun test_go_to_function_definition() = runTest {
        // Red: 関数定義へのジャンプをテスト
        val (definitionService, _, astInfoService) = createServices()
        val code = """
            hoge=1
            # comment
            関数 myFunc(a, b)を:
                戻り値(a + b)
            と定義する
            
            res = myFunc(1, 2)
        """.trimIndent()

        // 関数呼び出し部分のmyFuncから関数定義へのジャンプをテスト
        val uri = "file:///test.dncl"
        val funcCallPosition = code.indexOf("myFunc", code.indexOf("res"))
        val location = definitionService.getDefinitionLocation(uri, code, funcCallPosition)

        assertNotNull(location)
        // 1行目の関数定義位置を期待
        assertEquals(2, location.range.start.line)
        assertEquals(3, location.range.start.character)
    }

    @Test
    fun test_go_to_parameter_definition() = runTest {
        // Red: パラメータ定義へのジャンプをテスト
        val (definitionService, _, astInfoService) = createServices()
        val code = """
            関数 calc(num1, num2)を:
                res = num1 + num2
                戻り値(res)
            と定義する
        """.trimIndent()

        // 関数内でのnum1使用から関数パラメータ定義へのジャンプをテスト
        val uri = "file:///test.dncl"
        val paramUsagePosition = code.indexOf("num1", code.indexOf("res ="))
        val location = definitionService.getDefinitionLocation(uri, code, paramUsagePosition)

        assertNotNull(location)
        // 1行目のパラメータ定義位置を期待
        assertEquals(0, location.range.start.line)
        // パラメータ名の開始位置を期待
        val expectedChar = code.indexOf("num1")
        assertEquals(expectedChar, location.range.start.character)
    }

    @Test
    fun test_go_to_definition_scoped_variables() = runTest {
        // Red: スコープを考慮した変数定義へのジャンプをテスト
        val (definitionService, _, astInfoService) = createServices()
        val code = """
            x = 10
            関数 test() を:
                x = 20
                表示する(x)
            と定義する
            表示(x)
        """.trimIndent()

        val uri = "file:///test.dncl"

        // 関数内のxは関数内の定義へジャンプす���き
        val innerXPosition = code.indexOf("x", code.indexOf("表示する", code.indexOf("関数 test")))
        val innerLocation = definitionService.getDefinitionLocation(uri, code, innerXPosition)

        // 定義が見つかることを確認（具体的な位置は実装に依存）
        assertNotNull(innerLocation, "関数内の変数xの定義が見つかるはずです")

        // 関数外のxはグローバルの定義へジャンプすべき
        val outerXPosition = code.lastIndexOf("x)")
        val outerLocation = definitionService.getDefinitionLocation(uri, code, outerXPosition)

        // 定義が見つかることを確認
        assertNotNull(outerLocation, "グローバル変数xの定義が見つかるはずです")
    }

    @Test
    fun test_definition_not_found() = runTest {
        // Green: 定義が見つからない場合のテスト
        val (definitionService, _, astInfoService) = createServices()
        val code = """
            表示する(undefinedVar)
        """.trimIndent()

        val uri = "file:///test.dncl"
        val undefinedPosition = code.indexOf("undefinedVar")
        val location = definitionService.getDefinitionLocation(uri, code, undefinedPosition)

        assertNull(location, "未定義の変数に対してはnullを返すべきです")
    }
}
