package io.github.arashiyama11.dncl_ide.editor.core

/**
 * 1つのエディタセッションにひも付くドキュメントのメタ情報。
 * `uri` は LSP が要求する一意な識別子、`languageId` は LSP 言語識別子、`initialText` は初期内容。
 */
data class EditorDocument(
    val uri: String,
    val languageId: String,
    val initialText: String
)
