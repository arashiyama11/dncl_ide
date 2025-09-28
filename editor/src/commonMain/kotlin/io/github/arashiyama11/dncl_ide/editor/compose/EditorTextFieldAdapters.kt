package io.github.arashiyama11.dncl_ide.editor.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import io.github.arashiyama11.dncl_ide.editor.core.EditorContent
import io.github.arashiyama11.dncl_ide.editor.core.EditorContentUpdate
import io.github.arashiyama11.dncl_ide.editor.core.EditorSelection
import io.github.arashiyama11.dncl_ide.editor.core.EditorSession
import io.github.arashiyama11.dncl_ide.editor.core.EditorState

@Composable
fun rememberEditorState(session: EditorSession): State<EditorState> = session.state.collectAsState()

fun TextFieldValue.toEditorContentUpdate(
    revision: Long,
    cause: EditorContentUpdate.UpdateCause = EditorContentUpdate.UpdateCause.UserInput
): EditorContentUpdate = EditorContentUpdate(
    content = EditorContent(
        text = text,
        selection = EditorSelection(selection.start, selection.end),
        revision = revision
    ),
    cause = cause
)

fun EditorState.toTextFieldValue(): TextFieldValue = TextFieldValue(
    text = content.text,
    selection = TextRange(content.selection.start, content.selection.end)
)
