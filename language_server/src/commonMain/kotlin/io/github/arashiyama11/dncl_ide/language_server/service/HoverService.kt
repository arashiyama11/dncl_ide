package io.github.arashiyama11.dncl_ide.language_server.service

import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.model.Token
import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import io.github.arashiyama11.dncl_ide.language_server.ast.SymbolKind
import io.github.arashiyama11.dncl_ide.interpreter.model.AllBuiltInFunction
import io.github.arashiyama11.dncl_ide.language_server.Hover
import io.github.arashiyama11.dncl_ide.language_server.MarkupContent

class HoverService(
    private val astInfoService: AstInfoService
) {
    fun getHover(code: String, offset: Int): Hover? {
        // First parse and analyze the code
        val astInfo = astInfoService.parseAndAnalyze(code) ?: return null

        val lexer = Lexer(code)
        val tokens = lexer.toList().mapNotNull { it.getOrNull() }

        val hoveredToken = tokens.firstOrNull { token ->
            offset in token.range
        }

        val hoverContent: String? = when (hoveredToken) {
            is Token.Japanese, is Token.Identifier -> {
                val symbol = astInfoService.findSymbolAtOffset(astInfo, offset)
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
                        val builtInFunction = AllBuiltInFunction.from(symbol.name)
                        if (builtInFunction != null) {
                            "**組み込み関数**: `${builtInFunction.identifier}`\n\n" +
                                    "この関数はDNCLの組み込み関数です。"
                        } else {
                            "**組み込み関数**: `${symbol.name}`"
                        }
                    }

                    SymbolKind.UNKNOWN, null -> {
                        // シンボルが見つからない場合は組み込み関数かどうかチェック
                        val tokenText = hoveredToken.literal
                        val builtInFunction = AllBuiltInFunction.from(tokenText)
                        if (builtInFunction != null) {
                            "**組み込み関数**: `${builtInFunction.identifier}`\n\n" +
                                    "この関数はDNCLの組み込み関数です。"
                        } else null
                    }
                }
            }

            is Token.Int -> {
                "**整数リテラル**: `${hoveredToken.literal}`\n\n" +
                        "値: ${hoveredToken.literal}"
            }

            is Token.Float -> {
                "**浮動小数点リテラル**: `${hoveredToken.literal}`\n\n" +
                        "値: ${hoveredToken.literal}"
            }

            is Token.String -> {
                "**文字列リテラル**: `${hoveredToken.literal}`\n\n" +
                        "値: ${hoveredToken.literal}"
            }

            is Token.Boolean -> {
                "**ブール値リテラル**: `${hoveredToken.literal}`\n\n" +
                        "値: ${hoveredToken.value}"
            }

            else -> null
        }

        return hoverContent?.let { content ->
            Hover(
                contents = MarkupContent(
                    kind = "markdown",
                    value = content
                ),
                range = null
            )
        }
    }
}
