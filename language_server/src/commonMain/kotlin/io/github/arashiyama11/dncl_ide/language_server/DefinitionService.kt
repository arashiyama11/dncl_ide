package io.github.arashiyama11.dncl_ide.language_server

import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode

class DefinitionService(
    private val diagnosticService: DiagnosticService,
    private val astInfoService: AstInfoService
) {
    fun getDefinitionLocation(uri: String, code: String, offset: Int): Location? {
        // ASTを解析
        astInfoService.parseAndAnalyze(code)

        // カーソル位置のシンボルを取得
        val symbol = astInfoService.findSymbolAtOffset(offset)
            ?: return null

        // シンボルの定義位置を取得
        val definitionNode = symbol.definitionNode

        val defRange = when (definitionNode) {
            is AstNode.FunctionStatement -> definitionNode.name.range
            is AstNode.Identifier -> definitionNode.range
            else -> symbol.range
        }

        // 定義位置の行と文字位置を計算
        val (startLine, startChar) = diagnosticService.calculateLineAndCharacter(
            code,
            defRange.first
        )
        val (endLine, endChar) = diagnosticService.calculateLineAndCharacter(
            code,
            defRange.last
        )

        return Location(
            uri = uri,
            range = Range(
                start = Position(startLine, startChar),
                end = Position(endLine, endChar)
            )
        )
    }
}
