package io.github.arashiyama11.dncl_ide.language_server.service

import io.github.arashiyama11.dncl_ide.language_server.CodeAction
import io.github.arashiyama11.dncl_ide.language_server.Diagnostic
import io.github.arashiyama11.dncl_ide.language_server.TextDocumentEdit
import io.github.arashiyama11.dncl_ide.language_server.TextEdit
import io.github.arashiyama11.dncl_ide.language_server.VersionedTextDocumentIdentifier
import io.github.arashiyama11.dncl_ide.language_server.WorkspaceEdit

class CodeActionService {
    fun getCodeActions(uri: String, diagnostics: List<Diagnostic>): List<CodeAction> {
        val codeActions = mutableListOf<CodeAction>()
        diagnostics.forEach { diagnostic ->
            if (diagnostic.message.contains("未定義の識別子")) {
                val identifier = diagnostic.message.substringAfter("未定義の識別子 ").trim()
                val range = diagnostic.range
                val textEdit = TextEdit(range = range, newText = "表示 $identifier")
                val textDocumentEdit = TextDocumentEdit(
                    textDocument = VersionedTextDocumentIdentifier(
                        uri = uri,
                        version = -1
                    ),
                    edits = listOf(textEdit)
                )
                val workspaceEdit = WorkspaceEdit(documentChanges = listOf(textDocumentEdit))
                codeActions.add(
                    CodeAction(
                        title = "'${identifier}' を表示する",
                        kind = "quickfix",
                        diagnostics = listOf(diagnostic),
                        edit = workspaceEdit
                    )
                )
            }
        }
        return codeActions
    }
}
