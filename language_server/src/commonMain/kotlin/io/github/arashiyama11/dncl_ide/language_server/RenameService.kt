package io.github.arashiyama11.dncl_ide.language_server

import io.github.arashiyama11.dncl_ide.language_server.ast.SymbolKind

class RenameService(
    private val diagnosticService: DiagnosticService,
    private val astInfoService: AstInfoService // AstInfoServiceを追加
) {
    fun getRenameEdits(uri: String, code: String, offset: Int, newName: String): WorkspaceEdit? {
        // ASTを解析
        astInfoService.parseAndAnalyze(code) // 既存のastInfoServiceを使用

        // カーソル位置のシンボルを取得（スコープ認識版を使用）
        val targetSymbol = astInfoService.findSymbolAtOffset(offset)
            ?: return null

        // 組み込み関数はリネーム不可
        if (targetSymbol.kind == SymbolKind.BUILT_IN_FUNCTION) {
            return null
        }

        // ReferenceServiceを作成してすべての参照箇所を取得
        val referenceService =
            ReferenceService(diagnosticService, astInfoService) // astInfoServiceを渡す
        val references = referenceService.getReferences(uri, code, offset)

        if (references.isEmpty()) {
            return null
        }

        // 各参照箇所をリネーム用の編集に変換
        val edits = references.map { location ->
            TextEdit(
                range = location.range,
                newText = newName
            )
        }

        val textDocumentEdit = TextDocumentEdit(
            textDocument = VersionedTextDocumentIdentifier(
                uri = uri,
                version = -1 // -1 for unknown version
            ),
            edits = edits
        )

        return WorkspaceEdit(documentChanges = listOf(textDocumentEdit))
    }
}
