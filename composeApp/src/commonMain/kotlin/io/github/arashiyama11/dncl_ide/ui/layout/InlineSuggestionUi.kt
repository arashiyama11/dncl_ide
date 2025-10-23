package io.github.arashiyama11.dncl_ide.ui.layout

import io.github.arashiyama11.dncl_ide.adapter.IdeUiState
import io.github.arashiyama11.dncl_ide.editor.compose.CodeEditorState

internal fun shouldShowInlineSuggestions(
    uiState: IdeUiState,
    editorState: CodeEditorState
): Boolean {
    val hasAnchor = editorState.cursorAnchorInEditor != null
    val hasLineHeight = editorState.cursorLineHeightPx != null
    return uiState.isFocused &&
        uiState.showInlineSuggestions &&
        hasAnchor &&
        hasLineHeight
}
