package io.github.arashiyama11.dncl_ide.editor.core

/**
 * 表示テキストとそのカーソル/選択情報を表す純粋データ。
 * IME 中間状態など UI 固有情報は Compose レイヤー側で扱い、確定後のスナップショットのみを保持する。
 */
data class EditorContent(
    val text: String,
    val selection: EditorSelection,
    val revision: Long = 0L
)

/**
 * 変更差分を表すための簡易モデル。現状は全テキスト上書きのみを扱うが、
 * LSP のインクリメンタル更新へ拡張しやすいように type フラグを用意しておく。
 */
data class EditorContentUpdate(
    val content: EditorContent,
    val cause: UpdateCause = UpdateCause.UserInput
) {
    enum class UpdateCause {
        UserInput,
        ExternalSync,
        Programmatic
    }
}
