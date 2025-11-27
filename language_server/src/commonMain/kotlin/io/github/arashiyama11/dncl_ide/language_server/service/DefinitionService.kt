package io.github.arashiyama11.dncl_ide.language_server.service

import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import io.github.arashiyama11.dncl_ide.language_server.Location
import io.github.arashiyama11.dncl_ide.language_server.Range
import io.github.arashiyama11.dncl_ide.language_server.util.calculatePosition

class DefinitionService(
    private val astInfoService: AstInfoService
) {
    suspend fun getDefinitionLocation(
        uri: String,
        code: String,
        offset: Int,
        cachedAstInfo: AstInfo? = null
    ): Location? {
        // 解析済みASTがない場合は何もしない（パースはスケジューラ経由に統一）
        val astInfo = cachedAstInfo ?: return null

        // カーソル位置のシンボルを取得
        val symbol = astInfoService.findSymbolAtOffset(astInfo, offset, uri)
            ?: return null

        // シンボルの定義位置を取得
        val definitionNode = symbol.definitionNode

        // Use the symbol's own range as the definition range, or fallback to the definition node
        val defRange = when (definitionNode) {
            is AstNode.FunctionStatement -> definitionNode.name.range
            is AstNode.Identifier -> definitionNode.range
            else -> symbol.range
        }

        return Location(
            uri = uri,
            range = Range(
                start = calculatePosition(code, defRange.first),
                end = calculatePosition(code, defRange.last)
            )
        )
    }
}
