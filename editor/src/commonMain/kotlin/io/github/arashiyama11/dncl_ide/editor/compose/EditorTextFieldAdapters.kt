package io.github.arashiyama11.dncl_ide.editor.compose

import androidx.compose.ui.text.input.TextFieldValue
import io.github.arashiyama11.dncl_ide.editor.core.EditorContent
import io.github.arashiyama11.dncl_ide.editor.core.EditorContentUpdate

fun TextFieldValue.toEditorContentUpdate(
    revision: Long,
    cause: EditorContentUpdate.UpdateCause = EditorContentUpdate.UpdateCause.UserInput
): EditorContentUpdate = EditorContentUpdate(
    content = EditorContent(
        text = this,
        revision = revision
    ),
    cause = cause
)

