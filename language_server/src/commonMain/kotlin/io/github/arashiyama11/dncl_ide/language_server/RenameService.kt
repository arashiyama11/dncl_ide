package io.github.arashiyama11.dncl_ide.language_server

import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.model.Token

class RenameService(private val diagnosticService: DiagnosticService) {
    fun getRenameEdits(uri: String, code: String, offset: Int, newName: String): WorkspaceEdit? {
        val lexer = Lexer(code)
        val tokens = lexer.toList().mapNotNull { it.getOrNull() }

        val targetToken = tokens.firstOrNull { token ->
            token.range.contains(offset) && (token is Token.Identifier || token is Token.Japanese)
        }

        if (targetToken != null) {
            val targetLiteral = targetToken.literal
            val edits = mutableListOf<TextEdit>()
            tokens.filter { it.literal == targetLiteral && (it is Token.Identifier || it is Token.Japanese) }
                .forEach { token ->
                    val (startLine, startChar) = diagnosticService.calculateLineAndCharacter(
                        code,
                        token.range.first
                    )
                    val (endLine, endChar) = diagnosticService.calculateLineAndCharacter(code, token.range.last)
                    edits.add(
                        TextEdit(
                            range = Range(
                                Position(startLine, startChar),
                                Position(endLine, endChar)
                            ),
                            newText = newName
                        )
                    )
                }
            val textDocumentEdit = TextDocumentEdit(
                textDocument = VersionedTextDocumentIdentifier(
                    uri = uri,
                    version = -1
                ), // -1 for unknown version
                edits = edits
            )
            return WorkspaceEdit(documentChanges = listOf(textDocumentEdit))
        } else {
            return null
        }
    }
}
