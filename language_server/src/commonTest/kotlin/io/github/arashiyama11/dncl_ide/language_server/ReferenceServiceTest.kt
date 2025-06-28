package io.github.arashiyama11.dncl_ide.language_server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReferenceServiceTest {

    private fun createServices(): Triple<ReferenceService, DiagnosticService, AstInfoService> {
        val diagnosticService = DiagnosticService()
        val astInfoService = AstInfoService()
        val referenceService = ReferenceService(diagnosticService, astInfoService)
        return Triple(referenceService, diagnosticService, astInfoService)
    }

    @Test
    fun test_find_variable_references() {
        // Red: 変数の全参照箇所を検索するテスト
        val (referenceService, _, astInfoService) = createServices()
        val code = """
            x = 10
            y = x + 5
            表示する(x)
            z = x * 2
        """.trimIndent()

        astInfoService.parseAndAnalyze(code)

        // 1行目のxの定義位置から全参照を検索
        val xDefinitionPosition = code.indexOf("x")
        val references =
            referenceService.getReferences("test://file.dncl", code, xDefinitionPosition)

        // 変数xは4箇所で使用されている（定義1回 + 使用3回）
        assertEquals(4, references.size, "変数xの参照は4箇所あるはずです")

        // 各参照位置が正しいことを確認
        val expectedPositions = listOf(
            code.indexOf("x"),                    // 定義
            code.indexOf("x", code.indexOf("y")), // y ← x + 5 での使用
            code.indexOf("x", code.indexOf("表示")), // 表示(x) での使用
            code.indexOf("x", code.indexOf("z"))   // z ← x * 2 での使用
        )

        val actualPositions = references.map { ref ->
            val line = ref.range.start.line
            val char = ref.range.start.character
            // 行と文字位置からオフセットを逆算
            val lines = code.split("\n")
            var offset = 0
            for (i in 0 until line) {
                offset += lines[i].length + 1 // +1 for newline
            }
            offset + char
        }.sorted()

        assertEquals(
            expectedPositions.sorted(),
            actualPositions,
            "参照位置が期待値と一致するはずです"
        )
    }

    @Test
    fun test_find_function_references() {
        // Red: 関数の全参照箇所を検索するテスト
        val (referenceService, _, astInfoService) = createServices()
        val code = """
            関数 add(a, b)を:
                戻り値(a + b)
            関数終了
            
            結果1 ← add(1, 2)
            結果2 ← add(3, 4)
        """.trimIndent()

        astInfoService.parseAndAnalyze(code)

        // 関数定義のaddから全参照を検索
        val addDefinitionPosition = code.indexOf("add")
        val references =
            referenceService.getReferences("test://file.dncl", code, addDefinitionPosition)

        // 関数addは3箇所で使用されている（定義1回 + 呼び出し2回）
        assertEquals(3, references.size, "関数addの参照は3箇所あるはずです")
    }

    @Test
    fun test_scoped_variable_references() {
        // Red: スコープを考慮した変数参照の検索テスト
        val (referenceService, _, astInfoService) = createServices()
        val code = """
            x = 10
            関数 test()を:
                x = 20
                表示する(x)
            と定義する
            表示する(x)
        """.trimIndent()

        astInfoService.parseAndAnalyze(code)

        // グローバルのxの参照を検索
        val globalXPosition = code.indexOf("x")
        val globalReferences =
            referenceService.getReferences("test://file.dncl", code, globalXPosition)

        // グローバルのxは2箇所（定義1回 + 最後の表示での使用1回）
        assertEquals(2, globalReferences.size, "グローバル変数xの参照は2箇所あるはずです")

        // 関数内のxの参照を検索
        val localXPosition = code.indexOf("x = 20")
        val localReferences =
            referenceService.getReferences("test://file.dncl", code, localXPosition)

        // ローカルのxは2箇所（定義1回 + 関数内の表示での使用1回）
        assertEquals(2, localReferences.size, "ローカル変数xの参照は2箇所あるはずです")
    }

    @Test
    fun test_parameter_references() {
        // Red: 関数パラメータの参照検索テスト
        val (referenceService, _, astInfoService) = createServices()
        val code = """
            関数 calc(num1, num2)を
                結果 = num1 + num2
                表示(num1)
                戻り値(結果)
            と定義する
        """.trimIndent()

        astInfoService.parseAndAnalyze(code)

        // パラメータnum1の参照を検索
        val param1Position = code.indexOf("num1")
        val references = referenceService.getReferences("test://file.dncl", code, param1Position)

        // num1は3箇所で使用されてい���（パラメータ定義1回 + 使用2回）
        assertEquals(3, references.size, "パラメータnum1の参照は3箇所あるはずです")
    }

    @Test
    fun test_no_references_for_undefined_symbol() {
        // Green: 未定義シンボルに対する参照検索テスト
        val (referenceService, _, astInfoService) = createServices()
        val code = """
            表示する("hello")
        """.trimIndent()

        astInfoService.parseAndAnalyze(code)

        // 存���しない位置での参照検索
        val invalidPosition = code.length + 10
        val references = referenceService.getReferences("test://file.dncl", code, invalidPosition)

        assertTrue(references.isEmpty(), "無効な位置では参照が見つからないはずです")
    }
}
