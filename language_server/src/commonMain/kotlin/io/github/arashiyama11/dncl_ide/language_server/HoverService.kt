package io.github.arashiyama11.dncl_ide.language_server

import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.model.Token
import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import io.github.arashiyama11.dncl_ide.interpreter.model.SymbolKind
import io.github.arashiyama11.dncl_ide.interpreter.model.AllBuiltInFunction

class HoverService(
    private val astInfoService: AstInfoService
) {
    fun getHover(code: String, offset: Int): Hover? {
        // First parse and analyze the code
        astInfoService.parseAndAnalyze(code)

        val lexer = Lexer(code)
        val tokens = lexer.toList().mapNotNull { it.getOrNull() }

        val hoveredToken = tokens.firstOrNull { token ->
            offset in token.range
        }

        val hoverContent: String? = when (hoveredToken) {
            is Token.Japanese, is Token.Identifier -> {

                val symbol = astInfoService.findSymbolAtOffset(offset)
                when (symbol?.kind) {
                    SymbolKind.VARIABLE -> {
                        "**変数**: `${symbol.name}`\n\n" +
                                "定義位置: ${symbol.range.first}-${symbol.range.last}\n" +
                                if (symbol.type != null) "型: ${symbol.type}" else "型: 不明"
                    }

                    SymbolKind.FUNCTION -> {
                        val functionNode = symbol.definitionNode as? AstNode.FunctionStatement
                        val params =
                            functionNode?.parameters?.joinToString(", ") { it.literal } ?: ""
                        "**関数**: `${symbol.name}($params)`\n\n" +
                                "定義位置: ${symbol.range.first}-${symbol.range.last}\n" +
                                if (params.isNotEmpty()) "パラメータ: $params" else "パラメータなし"
                    }

                    SymbolKind.PARAMETER -> {
                        "**パラメータ**: `${symbol.name}`\n\n" +
                                "定義位置: ${symbol.range.first}-${symbol.range.last}\n" +
                                "関数のパラメータとして定義されています"
                    }

                    SymbolKind.BUILT_IN_FUNCTION -> {
                        val builtInFunction =
                            AllBuiltInFunction.values().find { it.identifier == symbol.name }
                        if (builtInFunction != null) {
                            """**組み込み関数**: `${builtInFunction.identifier}`

この関数はDNCLの組み込み関数です。"""
                        } else {
                            null
                        }
                    }

                    SymbolKind.UNKNOWN, null -> {
                        // シンボル解決に失敗した場合も組み込み関数をチェック
                        val builtInFunction = AllBuiltInFunction.values()
                            .find { it.identifier == hoveredToken.literal }
                        if (builtInFunction != null) {
                            """**組み込み関数**: `${builtInFunction.identifier}`

この関数はDNCLの組み込み関数です。"""
                        } else {
                            null
                        }
                    }
                }
            }

            else -> null
        }

        return if (hoverContent != null) {
            Hover(
                contents = MarkupContent(
                    kind = "markdown",
                    value = hoverContent
                )
            )
        } else null
    }
}