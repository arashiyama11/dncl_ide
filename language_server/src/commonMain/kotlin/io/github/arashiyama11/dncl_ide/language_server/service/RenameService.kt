package io.github.arashiyama11.dncl_ide.language_server.service

import io.github.arashiyama11.dncl_ide.language_server.WorkspaceEdit
import io.github.arashiyama11.dncl_ide.language_server.TextEdit
import io.github.arashiyama11.dncl_ide.language_server.Position
import io.github.arashiyama11.dncl_ide.language_server.Range

class RenameService(
    private val astInfoService: AstInfoService
) {
    fun rename(uri: String, code: String, offset: Int, newName: String): WorkspaceEdit? {
        val astInfo = astInfoService.parseAndAnalyze(code) ?: return null

        // カーソル位置のシンボルを取得
        val symbol = astInfoService.findSymbolAtOffset(astInfo, offset) ?: return null

        // 参照箇所を取得（宣言も含む）
        val referenceService = ReferenceService(astInfoService)
        val references =
            referenceService.findReferences(uri, code, offset, includeDeclaration = true)

        // 各参照箇所をTextEditに変換
        val textEdits = references.map { location ->
            TextEdit(
                range = location.range,
                newText = newName
            )
        }

        return WorkspaceEdit(
            changes = mapOf(uri to textEdits)
        )
    }
}
