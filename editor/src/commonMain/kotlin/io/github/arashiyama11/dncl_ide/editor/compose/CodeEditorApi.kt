package io.github.arashiyama11.dncl_ide.editor.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Stable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import io.github.arashiyama11.dncl_ide.editor.core.EditorContent
import io.github.arashiyama11.dncl_ide.editor.core.EditorContentUpdate
import io.github.arashiyama11.dncl_ide.editor.core.EditorDocument
import io.github.arashiyama11.dncl_ide.editor.core.EditorSelection
import kotlin.math.max
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Monaco の `IStandaloneCodeEditor` に倣い、UI 層が監視・操作するエディタ状態を表現する。
 * Compose 固有の `TextFieldValue` は内部に保持しつつ、外部には純粋データ `EditorContent` を返す。
 */
@Stable
class CodeEditorState internal constructor(
    initialDocument: EditorDocument?,
    initialContent: EditorContent,
    initialAnnotatedText: AnnotatedString?,
    initialEvaluatingLine: Int?,
    initialVerticalScrollEnabled: Boolean,
    initialDecorations: List<EditorDecoration>
) {

    private var documentState by mutableStateOf(initialDocument)
    var document: EditorDocument?
        get() = documentState
        private set(value) {
            if (documentState == value) return
            documentState = value
        }

    private var contentState by mutableStateOf(initialContent)
    val content: EditorContent
        get() = contentState
    val textFieldValue: TextFieldValue
        get() = contentState.text

    private var annotatedTextHolder by mutableStateOf(
        AnnotatedTextHolder(
            initialAnnotatedText,
            null
        )
    )
    val annotatedText: AnnotatedString?
        get() = annotatedTextHolder.text

    var evaluatingLine by mutableStateOf(initialEvaluatingLine)
        private set

    var verticalScrollEnabled by mutableStateOf(initialVerticalScrollEnabled)
        private set

    private val _decorations = mutableStateListOf(*initialDecorations.toTypedArray())
    val decorations: List<EditorDecoration> get() = _decorations

    fun updateDocument(next: EditorDocument?) {
        document = next
    }

    fun updateAnnotatedText(next: AnnotatedString?, revision: Long? = null) {
        val current = annotatedTextHolder
        if (current.text == next && current.revision == revision) return
        annotatedTextHolder = AnnotatedTextHolder(next, revision)
    }

    fun updateEvaluatingLine(line: Int?) {
        if (evaluatingLine == line) return
        evaluatingLine = line
    }

    fun updateVerticalScrollEnabled(enabled: Boolean) {
        if (verticalScrollEnabled == enabled) return
        verticalScrollEnabled = enabled
    }

    fun setDecorations(decorations: List<EditorDecoration>) {
        if (_decorations == decorations) return
        _decorations.clear()
        _decorations.addAll(decorations)
    }

    fun updateContent(content: EditorContent) {
        if (contentState == content) return
        contentState = content
    }

    fun consumeTextFieldValue(
        value: TextFieldValue,
        cause: EditorContentUpdate.UpdateCause
    ): EditorContentUpdate {
        val nextContent = EditorContent(
            text = value,
            revision = contentState.revision + 1
        )
        contentState = nextContent
        return EditorContentUpdate(nextContent, cause)
    }

    private fun TextRange.constrain(text: String): TextRange {
        val endIndex = max(0, text.length)
        val startValue = start.coerceIn(0, endIndex)
        val endValue = end.coerceIn(0, endIndex)
        return if (startValue == start && endValue == end) this else TextRange(startValue, endValue)
    }
}

private data class AnnotatedTextHolder(
    val text: AnnotatedString?,
    val revision: Long?
)

/**
 * エディタの見た目と動作オプション。
 */
data class CodeEditorOptions(
    val fontSize: Int = 14,
    val textStyle: TextStyle? = null,
    val lineNumberColor: Color? = null,
    val activeLineNumberColor: Color? = null,
    val cursorColor: Color? = null,
    val lineNumberColumnWidth: Dp? = null,
    val showLineNumbers: Boolean = true,
    val verticalScrollEnabled: Boolean = true,
    val tabSize: Int = 4,
    val insertSpaces: Boolean = true
)

class CodeEditorEvents internal constructor(
    val contentChanges: SharedFlow<ContentChanged>,
    val focusChanges: SharedFlow<FocusChanged>
) {
    data class ContentChanged(val update: EditorContentUpdate)
    data class FocusChanged(val isFocused: Boolean)
}

/**
 * Compose 側からエディタを制御するためのコントローラ。
 */
@Stable
class CodeEditorController internal constructor(
    private val focusRequesterState: MutableState<FocusRequester?>,
    private val contentChanges: MutableSharedFlow<CodeEditorEvents.ContentChanged>,
    private val focusChanges: MutableSharedFlow<CodeEditorEvents.FocusChanged>
) {

    val events: CodeEditorEvents = CodeEditorEvents(
        contentChanges = contentChanges.asSharedFlow(),
        focusChanges = focusChanges.asSharedFlow()
    )

    fun requestFocus() {
        focusRequesterState.value?.requestFocus()
    }

    internal fun attachFocusRequester(focusRequester: FocusRequester?) {
        focusRequesterState.value = focusRequester
    }

    internal fun emitContentChanged(change: CodeEditorEvents.ContentChanged) {
        contentChanges.tryEmit(change)
    }

    internal fun emitFocusChanged(change: CodeEditorEvents.FocusChanged) {
        focusChanges.tryEmit(change)
    }
}

@Composable
fun rememberCodeEditorState(
    document: EditorDocument? = null,
    content: EditorContent = EditorContent(text = TextFieldValue()),
    annotatedText: AnnotatedString? = null,
    evaluatingLine: Int? = null,
    verticalScrollEnabled: Boolean = true,
    decorations: List<EditorDecoration> = emptyList(),
): CodeEditorState = remember {
    CodeEditorState(
        initialDocument = document,
        initialContent = content,
        initialAnnotatedText = annotatedText,
        initialEvaluatingLine = evaluatingLine,
        initialVerticalScrollEnabled = verticalScrollEnabled,
        initialDecorations = decorations
    )
}

@Composable
fun BindCodeEditorState(
    state: CodeEditorState,
    document: EditorDocument? = null,
    content: EditorContent? = null,
    annotatedText: AnnotatedString? = null,
    highlightRevision: Long? = null,
    evaluatingLine: Int? = null,
    verticalScrollEnabled: Boolean? = null,
    decorations: List<EditorDecoration>? = null,
) {
    LaunchedEffect(document) {
        if (document != state.document) {
            state.updateDocument(document)
        }
    }

    LaunchedEffect(content) {
        if (content != null && content != state.content) {
            state.updateContent(content)
        }
    }

    LaunchedEffect(annotatedText, highlightRevision) {
        state.updateAnnotatedText(annotatedText, highlightRevision)
    }

    LaunchedEffect(evaluatingLine) {
        state.updateEvaluatingLine(evaluatingLine)
    }

    LaunchedEffect(verticalScrollEnabled) {
        verticalScrollEnabled?.let { state.updateVerticalScrollEnabled(it) }
    }

    LaunchedEffect(decorations) {
        decorations?.let { state.setDecorations(it) }
    }
}

@Composable
fun rememberCodeEditorController(): CodeEditorController {
    val focusRequesterState = remember { mutableStateOf<FocusRequester?>(null) }
    val contentChanges = remember {
        MutableSharedFlow<CodeEditorEvents.ContentChanged>(replay = 0, extraBufferCapacity = 16)
    }
    val focusChanges = remember {
        MutableSharedFlow<CodeEditorEvents.FocusChanged>(replay = 0, extraBufferCapacity = 16)
    }
    return remember {
        CodeEditorController(
            focusRequesterState = focusRequesterState,
            contentChanges = contentChanges,
            focusChanges = focusChanges
        )
    }
}

/**
 * 行・範囲の強調表示などに利用する簡易デコレーションモデル。
 */
data class EditorDecoration(
    val id: String,
    val range: IntRange,
    val kind: DecorationKind,
    val payload: DecorationPayload = DecorationPayload.None
)

enum class DecorationKind {
    LineHighlight,
    InlineRange,
    GutterIcon
}

sealed interface DecorationPayload {
    data object None : DecorationPayload
    data class Color(val value: androidx.compose.ui.graphics.Color) : DecorationPayload
}
