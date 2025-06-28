package io.github.arashiyama11.dncl_ide.language_server

import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.model.Token
import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import io.github.arashiyama11.dncl_ide.interpreter.model.SymbolKind

class HoverService(
    private val diagnosticService: DiagnosticService,
    private val astInfoService: AstInfoService
) {
    fun getHover(code: String, offset: Int): Hover? {
        val lexer = Lexer(code)
        val tokens = lexer.toList().mapNotNull { it.getOrNull() }

        val hoveredToken = tokens.firstOrNull { token ->
            token.range.contains(offset)
        }

        val hoverContent = when (hoveredToken) {
            is Token.Japanese -> {
                // 組み込み関数のホバー情報
                builtInFunctionDescriptions[hoveredToken.literal]
            }

            is Token.Identifier -> {
                // ユーザー定義シンボルのホバー情報
                val symbol = astInfoService.findSymbolAtOffset(offset)
                if (symbol != null) {
                    when (symbol.kind) {
                        SymbolKind.VARIABLE -> {
                            "**変数**: `${symbol.name}`\n\n" +
                                    "定義位置: ${symbol.range.first}-${symbol.range.last}\n" +
                                    if (symbol.type != null) "型: ${symbol.type}" else "型: 不明"
                        }

                        SymbolKind.FUNCTION -> {
                            val functionNode = symbol.definitionNode as? AstNode.FunctionStatement
                            val params = functionNode?.parameters?.joinToString(", ") ?: ""
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
                            builtInFunctionDescriptions[symbol.name]
                                ?: "**組み込み関数**: `${symbol.name}`"
                        }

                        SymbolKind.UNKNOWN -> null
                    }
                } else {
                    // シンボルが見つからない場合、組み込み関数かチェック
                    builtInFunctionDescriptions[hoveredToken.literal]
                }
            }

            else -> null
        }

        return if (hoverContent != null) {
            Hover(
                contents = MarkupContent(kind = "markdown", value = hoverContent),
                range = hoveredToken?.let { token ->
                    val (startLine, startChar) = diagnosticService.calculateLineAndCharacter(
                        code,
                        token.range.first
                    )
                    val (endLine, endChar) = diagnosticService.calculateLineAndCharacter(
                        code,
                        token.range.last
                    )
                    Range(Position(startLine, startChar), Position(endLine, endChar))
                }
            )
        } else {
            null
        }
    }
}
