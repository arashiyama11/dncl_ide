package io.github.arashiyama11.dncl_ide.language_server

import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.model.Token
import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import io.github.arashiyama11.dncl_ide.interpreter.model.SymbolKind
import io.github.arashiyama11.dncl_ide.language_server.builtInFunctionDescriptions

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
            is Token.Japanese -> builtInFunctionDescriptions[hoveredToken.literal]
            is Token.Identifier -> {
                val symbol = astInfoService.findSymbolAtOffset(offset)
                if (symbol != null) {
                    when (symbol.kind) {
                        SymbolKind.VARIABLE -> "変数: ${symbol.name}"
                        SymbolKind.FUNCTION -> "関数: ${symbol.name}(${symbol.definitionNode?.let { (it as? AstNode.FunctionStatement)?.parameters?.joinToString() } ?: ""})"
                        SymbolKind.PARAMETER -> "パラメータ: ${symbol.name}"
                        SymbolKind.BUILT_IN_FUNCTION -> builtInFunctionDescriptions[symbol.name]
                        SymbolKind.UNKNOWN -> null
                    }
                } else {
                    null
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
