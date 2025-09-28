package io.github.arashiyama11.dncl_ide.editor.core

import io.github.arashiyama11.dncl_ide.language_server.CompletionItem
import io.github.arashiyama11.dncl_ide.language_server.Diagnostic
import io.github.arashiyama11.dncl_ide.language_server.SemanticTokens

/**
 * エディタが保持する UI 非依存の状態。Compose 側はこの state を監視してレンダリングを行う。
 */
data class EditorState(
    val document: EditorDocument? = null,
    val content: EditorContent = EditorContent(text = "", selection = EditorSelection(start = 0, end = 0)),
    val diagnostics: List<Diagnostic> = emptyList(),
    val completions: List<CompletionItem> = emptyList(),
    val semanticTokens: SemanticTokens? = null,
    val isBusy: Boolean = false,
    val isInitialized: Boolean = false,
    val isDirty: Boolean = false,
    val lastError: EditorError? = null
)

sealed interface EditorError {
    val message: String?

    data class Initialization(override val message: String?, val cause: Throwable? = null) : EditorError
    data class LanguageFeature(override val message: String?, val cause: Throwable? = null) : EditorError
}
