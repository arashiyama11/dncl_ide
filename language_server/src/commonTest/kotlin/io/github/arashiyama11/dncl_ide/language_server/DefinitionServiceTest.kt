package io.github.arashiyama11.dncl_ide.language_server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DefinitionServiceTest {

    private fun createServices(): Triple<DefinitionService, DiagnosticService, AstInfoService> {
        val diagnosticService = DiagnosticService()
        val astInfoService = AstInfoService()
        val definitionService = DefinitionService(astInfoService)
        return Triple(definitionService, diagnosticService, astInfoService)
    }

    @Test
    fun test_go_to_variable_definition() {
        // Red: 変数定義へのジャンプをテスト
        val (definitionService, _, astInfoService) = createServices()
        val code = """
            x = 10
            y = x + 5
            表示(x)
        """.trimIndent()

        astInfoService.parseAndAnalyze(code)

        // 2行目のxの位置から1行目の定義へのジャンプをテスト
        val xUsagePosition = code.indexOf("x", code.indexOf("y ="))
        val location =
            definitionService.getDefinitionLocation("test://file.dncl", code, xUsagePosition)

        assertNotNull(location)
        // 1行目の変数定義位置を期待
        assertEquals(0, location.range.start.line)
        // 変数名の開始位置を期待
        val expectedChar = code.indexOf("x")
        assertEquals(expectedChar, location.range.start.character)
    }

    @Test
    fun test_go_to_function_definition() {
        // Red: 関数定義へのジャンプをテスト
        val (definitionService, _, astInfoService) = createServices()
        val code = """
            関数 myFunc(a, b)を:
                戻り値(a + b)
            と定義する
            
            res = myFunc(1, 2)
        """.trimIndent()

        astInfoService.parseAndAnalyze(code)

        // 関数呼び出し部分のmyFuncから関数定義へのジャンプをテスト
        val funcCallPosition = code.indexOf("myFunc", code.indexOf("res"))
        val location =
            definitionService.getDefinitionLocation("test://file.dncl", code, funcCallPosition)

        assertNotNull(location)
        // 1行目の関数定義位置を期待
        assertEquals(0, location.range.start.line)
        // 関数名の開始位置を期待
        val expectedChar = code.indexOf("myFunc")
        assertEquals(expectedChar, location.range.start.character)
    }

    @Test
    fun test_go_to_parameter_definition() {
        // Red: パラメータ定義へのジャンプをテスト
        val (definitionService, _, astInfoService) = createServices()
        val code = """
            関数 calc(num1, num2)を:
                res = num1 + num2
                戻り値(res)
            と定義する
        """.trimIndent()

        astInfoService.parseAndAnalyze(code)

        // 関数内でのnum1使用から関数パラメータ定義へのジャンプをテスト
        val paramUsagePosition = code.indexOf("num1", code.indexOf("res ="))
        val location =
            definitionService.getDefinitionLocation("test://file.dncl", code, paramUsagePosition)

        assertNotNull(location)
        // 1行目のパラメータ定義位置を期待
        assertEquals(0, location.range.start.line)
        // パラメータ名の開始位置を期待
        val expectedChar = code.indexOf("num1")
        assertEquals(expectedChar, location.range.start.character)
    }

    @Test
    fun test_go_to_definition_scoped_variables() {
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

        astInfoService.parseAndAnalyze(code)

        // 関数内のxは関数内の定義へジャンプすべき
        val innerXPosition =
            code.indexOf("x", code.indexOf("表示する", code.indexOf("関数 test")))
        val innerLocation =
            definitionService.getDefinitionLocation("test://file.dncl", code, innerXPosition)

        assertNotNull(innerLocation)
        // 関数内のx定義（3行目）を期待
        val innerXDefLine = code.substring(0, code.indexOf("x = 20")).count { it == '\n' }
        assertEquals(innerXDefLine, innerLocation.range.start.line)

        // 関数外のxはグローバルの定義へジャンプすべき
        val outerXPosition = code.lastIndexOf("x)")
        val outerLocation =
            definitionService.getDefinitionLocation("test://file.dncl", code, outerXPosition)

        assertNotNull(outerLocation)
        // グローバルのx定義（1行目）を期待
        assertEquals(0, outerLocation.range.start.line)
    }

    @Test
    fun test_definition_not_found() {
        // Green: 定義が見つからない場合のテスト
        val (definitionService, _, astInfoService) = createServices()
        val code = """
            表示する(undefinedVar)
        """.trimIndent()

        astInfoService.parseAndAnalyze(code)

        val undefinedPosition = code.indexOf("undefinedVar")
        val location =
            definitionService.getDefinitionLocation("test://file.dncl", code, undefinedPosition)

        assertNull(location, "未定義の変数に対してはnullを返すべきです")
    }
}
