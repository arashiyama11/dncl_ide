package io.github.arashiyama11.dncl_ide.language_server

import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.model.Token
import io.github.arashiyama11.dncl_ide.language_server.builtInFunctionDescriptions

class HoverService(private val diagnosticService: DiagnosticService) {
    fun getHover(code: String, offset: Int): Hover? {
        val lexer = Lexer(code)
        val tokens = lexer.toList().mapNotNull { it.getOrNull() }

        val hoveredToken = tokens.firstOrNull { token ->
            token.range.contains(offset)
        }

        val hoverContent = when (hoveredToken) {
            is Token.Japanese -> builtInFunctionDescriptions[hoveredToken.literal]
            is Token.Identifier -> builtInFunctionDescriptions[hoveredToken.literal]
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
                    val (endLine, endChar) = diagnosticService.calculateLineAndCharacter(code, token.range.last)
                    Range(Position(startLine, startChar), Position(endLine, endChar))
                }
            )
        } else {
            null
        }
    }
}
