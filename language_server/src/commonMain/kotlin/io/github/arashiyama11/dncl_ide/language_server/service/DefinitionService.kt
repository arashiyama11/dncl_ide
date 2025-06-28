package io.github.arashiyama11.dncl_ide.language_server.service

import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import io.github.arashiyama11.dncl_ide.language_server.Location
import io.github.arashiyama11.dncl_ide.language_server.Position
import io.github.arashiyama11.dncl_ide.language_server.Range

class DefinitionService(
    private val astInfoService: AstInfoService
) {
    fun getDefinitionLocation(uri: String, code: String, offset: Int): Location? {
        // Parse and analyze the code
        val astInfo = astInfoService.parseAndAnalyze(code) ?: return null

        // カーソル位置のシンボルを取得
        val symbol = astInfoService.findSymbolAtOffset(astInfo, offset)
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
                start = Position(line = 0, character = defRange.first),
                end = Position(line = 0, character = defRange.last + 1)
            )
        )
    }
}
