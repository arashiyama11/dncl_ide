package io.github.arashiyama11.dncl_ide.editor.core

import kotlinx.coroutines.flow.StateFlow

interface EditorSession {
    val state: StateFlow<EditorState>
    fun dispatch(intent: EditorIntent)
    suspend fun close()
}
