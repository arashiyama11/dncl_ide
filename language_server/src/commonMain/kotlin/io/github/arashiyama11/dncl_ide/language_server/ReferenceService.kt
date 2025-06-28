package io.github.arashiyama11.dncl_ide.language_server

import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.model.Token

class ReferenceService(private val diagnosticService: DiagnosticService) {
    fun getReferences(uri: String, code: String, offset: Int): List<Location> {
        val lexer = Lexer(code)
        val tokens = lexer.toList().mapNotNull { it.getOrNull() }

        val targetToken = tokens.firstOrNull { token ->
            token.range.contains(offset) && (token is Token.Identifier || token is Token.Japanese)
        }

        val references = mutableListOf<Location>()
        if (targetToken != null) {
            val targetLiteral = targetToken.literal
            tokens.filter { it.literal == targetLiteral && (it is Token.Identifier || it is Token.Japanese) }
                .forEach { token ->
                    val (startLine, startChar) = diagnosticService.calculateLineAndCharacter(
                        code,
                        token.range.first
                    )
                    val (endLine, endChar) = diagnosticService.calculateLineAndCharacter(code, token.range.last)
                    references.add(
                        Location(
                            uri = uri,
                            range = Range(
                                Position(startLine, startChar),
                                Position(endLine, endChar)
                            )
                        )
                    )
                }
        }
        return references
    }
}
