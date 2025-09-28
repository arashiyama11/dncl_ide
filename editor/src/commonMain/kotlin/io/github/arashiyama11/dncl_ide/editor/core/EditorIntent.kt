package io.github.arashiyama11.dncl_ide.editor.core

import io.github.arashiyama11.dncl_ide.language_server.Position

sealed interface EditorIntent {
    data class Initialize(val document: EditorDocument) : EditorIntent
    data class UpdateContent(val update: EditorContentUpdate) : EditorIntent
    data class TriggerCompletion(val position: Position) : EditorIntent
    data object RequestSemanticTokens : EditorIntent
    data object Close : EditorIntent
}
