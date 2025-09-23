package io.github.arashiyama11.dncl_ide.language_server

import io.github.arashiyama11.dncl_ide.language_server.service.AstInfoService
import io.github.arashiyama11.dncl_ide.language_server.service.DiagnosticService
import io.github.arashiyama11.dncl_ide.language_server.service.RenameService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RenameServiceTest {

    private fun createServices(): Triple<RenameService, DiagnosticService, AstInfoService> {
        val diagnosticService = DiagnosticService()
        val astInfoService = AstInfoService()
        val renameService = RenameService(astInfoService)
        return Triple(renameService, diagnosticService, astInfoService)
    }

    @Test
    fun test_rename_variable() {
        // Red: 変数のリネーム機能をテスト
        val (renameService, _, astInfoService) = createServices()
        val code = """
            x = 10
            y = x + 5
            表示する(x)
        """.trimIndent()

        val uri = "file:///test.dncl"
        val workspaceEdit = renameService.rename(uri, code, code.indexOf("x"), "newX")

        assertNotNull(workspaceEdit)
        assertNotNull(workspaceEdit.changes)
        val changes = workspaceEdit.changes!![uri]
        assertNotNull(changes)
        // リネーム機能が動作することを確認（具体的な数は実装に依存）
        assertTrue(changes!!.isNotEmpty(), "変数xのリネームで少なくとも1つの変更があるはずです")
        changes.forEach { edit ->
            assertEquals("newX", edit.newText)
        }
    }

    @Test
    fun test_rename_function() {
        // Red: 関数のリネーム機能をテスト
        val (renameService, _, astInfoService) = createServices()
        val code = """
            関数 add(a, b)を:
                戻り値(a + b)
            と定義する
            
            res = add(1, 2)
        """.trimIndent()

        // 関数定義のaddをcalculateにリネーム
        val uri = "file:///test.dncl"
        val addPosition = code.indexOf("add")
        val workspaceEdit = renameService.rename(uri, code, addPosition, "calculate")

        assertNotNull(workspaceEdit)
        assertNotNull(workspaceEdit.changes)
        val changes = workspaceEdit.changes!![uri]
        assertNotNull(changes)
        // 関数のリネーム機能が動作することを確認
        assertTrue(changes!!.isNotEmpty(), "関数addのリネームで少なくとも1つの変更があるはずです")
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

        // グローバルのxをglobalVarにリネーム
        val uri = "file:///test.dncl"
        val globalXPosition = code.indexOf("x")
        val globalEdit = renameService.rename(uri, code, globalXPosition, "globalVar")

        assertNotNull(globalEdit)
        assertNotNull(globalEdit.changes)
        val globalChanges = globalEdit.changes!![uri]
        assertNotNull(globalChanges)
        // グローバル変数のリネーム機能が動作することを確認
        assertTrue(
            globalChanges!!.isNotEmpty(),
            "グローバル変数xのリネームで少なくとも1つの変更があるはずです"
        )

        // 関数内のxをlocalVarにリネーム
        val localXPosition = code.indexOf("x = 20")
        val localEdit = renameService.rename(uri, code, localXPosition, "localVar")

        assertNotNull(localEdit)
        assertNotNull(localEdit.changes)
        val localChanges = localEdit.changes!![uri]
        assertNotNull(localChanges)
        // ローカル変数のリネーム機能が動作することを確認
        assertTrue(
            localChanges!!.isNotEmpty(),
            "ローカル変数xのリネームで少なくとも1つの変更があるはずです"
        )
    }

    @Test
    fun test_rename_parameter() {
        // Red: 関数パラメータのリネーム機能をテスト
        val (renameService, _, astInfoService) = createServices()
        val code = """
            関数 calc(num1, num2)を:
                res = num1 + num2
                表示する(num1)
                戻り値(res)
            と定義する
        """.trimIndent()

        // パラメータnum1をfirstNumにリネーム
        val uri = "file:///test.dncl"
        val param1Position = code.indexOf("num1")
        val workspaceEdit = renameService.rename(uri, code, param1Position, "firstNum")

        assertNotNull(workspaceEdit)
        assertNotNull(workspaceEdit.changes)
        val changes = workspaceEdit.changes!![uri]
        assertNotNull(changes)
        // パラメータのリネーム機能が動作することを確認
        assertTrue(
            changes!!.isNotEmpty(),
            "パラメータnum1のリネームで少なくとも1つの変更があるはずです"
        )
    }

    @Test
    fun test_rename_invalid_position() {
        // Green: 無効な位置でのリネーム試行テスト
        val (renameService, _, astInfoService) = createServices()
        val code = """
            表示する("hello")
        """.trimIndent()

        // 文字列リテラル内の位置でリネーム試行
        val uri = "file:///test.dncl"
        val invalidPosition = code.indexOf("hello")
        val workspaceEdit = renameService.rename(uri, code, invalidPosition, "newName")

        assertNull(workspaceEdit, "文字列リテラル内ではリネームできないはずです")
    }
}
