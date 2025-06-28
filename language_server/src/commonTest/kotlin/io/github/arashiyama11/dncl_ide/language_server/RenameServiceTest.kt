package io.github.arashiyama11.dncl_ide.language_server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RenameServiceTest {

    private fun createServices(): Triple<RenameService, DiagnosticService, AstInfoService> {
        val diagnosticService = DiagnosticService()
        val astInfoService = AstInfoService()
        val renameService = RenameService(diagnosticService)
        return Triple(renameService, diagnosticService, astInfoService)
    }

    @Test
    fun test_rename_variable() {
        // Red: 変数のリネーム機能をテスト
        val (renameService, _, astInfoService) = createServices()
        val code = """
            x = 10
            y = x + 5
            表示(x)
        """.trimIndent()

        astInfoService.parseAndAnalyze(code)

        // 1行目のxをnewVarにリネーム
        val xPosition = code.indexOf("x")
        val workspaceEdit =
            renameService.getRenameEdits("test://file.dncl", code, xPosition, "newVar")

        assertNotNull(workspaceEdit)
        assertNotNull(workspaceEdit.documentChanges)
        assertEquals(1, workspaceEdit.documentChanges!!.size)

        val textDocumentEdit = workspaceEdit.documentChanges!![0]
        // 変数xは3箇所で使用されている
        assertEquals(3, textDocumentEdit.edits.size, "変数xは3箇所でリネームされるはずです")

        // 各編集がnewVarに置換されることを確認
        textDocumentEdit.edits.forEach { edit ->
            assertEquals("newVar", edit.newText, "新しい名前がnewVarになるはずです")
        }
    }

    @Test
    fun test_rename_function() {
        // Red: 関数のリネーム機能をテスト
        val (renameService, _, astInfoService) = createServices()
        val code = """
            関数 add(a, b)
                戻り値(a + b)
            関数終了
            
            結果 = add(1, 2)
        """.trimIndent()

        astInfoService.parseAndAnalyze(code)

        // 関数定義のaddをcalculateにリネーム
        val addPosition = code.indexOf("add")
        val workspaceEdit =
            renameService.getRenameEdits("test://file.dncl", code, addPosition, "calculate")

        assertNotNull(workspaceEdit)
        val textDocumentEdit = workspaceEdit.documentChanges!![0]
        // 関数addは2箇所で使用されている（定義1回 + 呼び出し1回）
        assertEquals(2, textDocumentEdit.edits.size, "関数addは2箇所でリネームされるはずです")
    }

    @Test
    fun test_rename_scoped_variables_correctly() {
        // Red: スコープを考慮した変数リネームのテスト
        val (renameService, _, astInfoService) = createServices()
        val code = """
            x = 10
            関数 test()を:
                x = 20
                表示する(x)
            と定義する
            表示する(x)
        """.trimIndent()

        astInfoService.parseAndAnalyze(code)

        // グローバルのxをglobalVarにリネーム
        val globalXPosition = code.indexOf("x")
        val globalEdit =
            renameService.getRenameEdits("test://file.dncl", code, globalXPosition, "globalVar")

        assertNotNull(globalEdit)
        val globalTextEdit = globalEdit.documentChanges!![0]
        // グローバルのxは2箇所のみ（関数内のxは別のスコープなので対象外）
        assertEquals(
            2,
            globalTextEdit.edits.size,
            "グローバル変数xは2箇所のみリネームされるはずです"
        )

        // 関数内のxをlocalVarにリネーム
        val localXPosition = code.indexOf("x ← 20")
        val localEdit =
            renameService.getRenameEdits("test://file.dncl", code, localXPosition, "localVar")

        assertNotNull(localEdit)
        val localTextEdit = localEdit.documentChanges!![0]
        // ローカルのxは2箇所のみ（グローバルのxは対象外）
        assertEquals(2, localTextEdit.edits.size, "ローカル変数xは2箇所のみリネームされるはずです")
    }

    @Test
    fun test_rename_parameter() {
        // Red: 関数パラメータのリネーム機能をテスト
        val (renameService, _, astInfoService) = createServices()
        val code = """
            関数 calc(num1, num2)を:
                結果 = num1 + num2
                表示する(num1)
                戻り値(結果)
            と定義する
        """.trimIndent()

        astInfoService.parseAndAnalyze(code)

        // パラメータnum1をfirstNumにリネーム
        val param1Position = code.indexOf("num1")
        val workspaceEdit =
            renameService.getRenameEdits("test://file.dncl", code, param1Position, "firstNum")

        assertNotNull(workspaceEdit)
        val textDocumentEdit = workspaceEdit.documentChanges!![0]
        // num1は3箇所で使用されている（パラメータ定義1回 + 使用2回）
        assertEquals(
            3,
            textDocumentEdit.edits.size,
            "パラメータnum1は3箇所でリネームされるはずです"
        )
    }

    @Test
    fun test_rename_invalid_position() {
        // Green: 無効な位置でのリネーム試行テスト
        val (renameService, _, astInfoService) = createServices()
        val code = """
            表示する("hello")
        """.trimIndent()

        astInfoService.parseAndAnalyze(code)

        // 文字列リテラル内の位置でリネーム試行
        val invalidPosition = code.indexOf("hello")
        val workspaceEdit =
            renameService.getRenameEdits("test://file.dncl", code, invalidPosition, "newName")

        assertNull(workspaceEdit, "文字列リテラル内ではリ���ームできないはずです")
    }

    @Test
    fun test_rename_builtin_function_should_fail() {
        // Green: 組み込み関数のリネーム試行テスト（失敗すべき）
        val (renameService, _, astInfoService) = createServices()
        val code = """
            表示する("hello")
        """.trimIndent()

        astInfoService.parseAndAnalyze(code)

        // 組み込み関数「表示」のリネーム試行
        val displayPosition = code.indexOf("表示する")
        val workspaceEdit =
            renameService.getRenameEdits("test://file.dncl", code, displayPosition, "show")

        assertNull(workspaceEdit, "組み込み関数はリネームできないはずです")
    }
}
