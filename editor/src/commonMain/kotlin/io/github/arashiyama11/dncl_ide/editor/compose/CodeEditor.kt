package io.github.arashiyama11.dncl_ide.editor.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.arashiyama11.dncl_ide.editor.core.EditorContentUpdate
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun CodeEditor(
    state: CodeEditorState,
    modifier: Modifier = Modifier,
    options: CodeEditorOptions = CodeEditorOptions(),
    controller: CodeEditorController = rememberCodeEditorController()
) {
    val resolvedTextStyle = (options.textStyle ?: MaterialTheme.typography.bodyMedium).copy(
        fontSize = options.fontSize.sp,
        lineHeight = (options.fontSize + 2).sp,
        fontWeight = FontWeight.Normal
    )

    val density = LocalDensity.current

    var lineHeightDp = with(density) {
        resolvedTextStyle.lineHeight.toDp()
    }

    val lineHeightPx = with(density) { lineHeightDp.toPx() }
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    var editorHeightPx by remember { mutableIntStateOf(0) }
    var editorCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var textFieldCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var latestTextLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    fun recalcCursorAnchor() {
        val rect = state.cursorRect
        val tfCoords = textFieldCoordinates
        val editorCoords = editorCoordinates
        if (rect == null || tfCoords == null || editorCoords == null) {
            state.updateCursorAnchorInEditor(null)
            return
        }
        val anchorTopLeft = tfCoords.localPositionOf(editorCoords, rect.topLeft)
        state.updateCursorAnchorInEditor(anchorTopLeft)
    }

    fun updateCursorMetrics(layoutResult: TextLayoutResult?, selection: TextRange) {
        if (layoutResult == null) return
        val textLength = layoutResult.layoutInput.text.length
        val selectionEnd = selection.end.coerceIn(0, textLength)
        val cursorRect = layoutResult.getCursorRect(selectionEnd)
        lineHeightDp = with(density) { cursorRect.height.toDp() }
        state.updateCursorRect(cursorRect)
        state.updateCursorLineHeightPx(cursorRect.height)
        recalcCursorAnchor()
    }

    DisposableEffect(controller, focusRequester) {
        controller.attachFocusRequester(focusRequester)
        onDispose { controller.attachFocusRequester(null) }
    }

    val textMeasurer = rememberTextMeasurer()

    val textValue = state.textFieldValue
    val lines = remember(textValue.text) { textValue.text.lines() }
    val largestLineNumberString = lines.size.coerceAtLeast(1).toString()

    val textLayoutResult = textMeasurer.measure(
        text = AnnotatedString(largestLineNumberString),
        style = resolvedTextStyle
    )

    val defaultColumnWidth =
        (with(LocalDensity.current) { textLayoutResult.size.width.toDp() } + 12.dp).coerceAtLeast(40.dp)
    val lineNumberColumnWidth = options.lineNumberColumnWidth ?: defaultColumnWidth

    val defaultLineNumberColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    val defaultActiveLineNumberColor =
        if (isSystemInDarkTheme()) Color.LightGray else MaterialTheme.colorScheme.onSurface
    val lineNumberColor = options.lineNumberColor ?: defaultLineNumberColor
    val activeLineNumberColor = options.activeLineNumberColor ?: defaultActiveLineNumberColor
    val cursorColor = options.cursorColor
        ?: if (isSystemInDarkTheme()) Color.White else MaterialTheme.colorScheme.onSurface

    val verticalScrollEnabled = options.verticalScrollEnabled && state.verticalScrollEnabled

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                editorHeightPx = coordinates.size.height
                editorCoordinates = coordinates
                recalcCursorAnchor()
            }
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                focusRequester.requestFocus()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .run {
                    if (verticalScrollEnabled) {
                        verticalScroll(verticalScrollState)
                    } else this
                }
                .background(MaterialTheme.colorScheme.background)
                .padding(8.dp)
        ) {
            if (options.showLineNumbers) {
                LineNumberColumn(
                    modifier = Modifier.width(lineNumberColumnWidth),
                    lines = lines,
                    selectionLine = state.content.text.selection.toLineIndex(lines),
                    evaluatingLine = state.evaluatingLine,
                    textStyle = resolvedTextStyle,
                    activeColor = activeLineNumberColor,
                    defaultColor = lineNumberColor,
                    lineHeightDp = lineHeightDp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            BasicTextField(
                value = state.textFieldValue,
                onValueChange = { value ->
                    val update = state.consumeTextFieldValue(
                        value,
                        EditorContentUpdate.UpdateCause.UserInput
                    )
                    controller.emitContentChanged(CodeEditorEvents.ContentChanged(update))
                },
                textStyle = resolvedTextStyle,
                keyboardOptions = options.keyboardOptions,
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 32.dp)
                    .horizontalScroll(horizontalScrollState)
                    .focusRequester(focusRequester)
                    .onGloballyPositioned { coordinates ->
                        textFieldCoordinates = coordinates
                        recalcCursorAnchor()
                    }
                    .onFocusChanged { focusState ->
                        controller.emitFocusChanged(CodeEditorEvents.FocusChanged(focusState.isFocused))
                        if (!focusState.isFocused) {
                            state.updateCursorAnchorInEditor(null)
                            state.updateCursorRect(null)
                            state.updateCursorLineHeightPx(null)
                        }
                    },
                cursorBrush = SolidColor(cursorColor),
                onTextLayout = { layoutResult ->
                    latestTextLayoutResult = layoutResult
                    updateCursorMetrics(layoutResult, state.content.text.selection)
                },
                decorationBox = { innerTextField ->
                    innerTextField()
                    val annotated = state.annotatedText
                    if (annotated != null) {
                        Text(
                            text = annotated,
                            modifier = Modifier.fillMaxSize(),
                            style = resolvedTextStyle,
                            softWrap = false
                        )
                    }
                }
            )
        }
    }

    if (verticalScrollEnabled) {
        var scrollJob: Job? by remember { mutableStateOf(null) }

        LaunchedEffect(state.content.text.selection, editorHeightPx, options.fontSize) {
            scrollJob?.cancel()
            scrollJob = coroutineScope.launch {
                val linesList = state.content.text.text.split("\n")
                var idx = 0
                var cursorLine = 0
                for ((i, line) in linesList.withIndex()) {
                    if (idx + line.length < state.content.text.selection.start) {
                        idx += line.length + 1
                    } else {
                        cursorLine = i
                        break
                    }
                }
                val targetOffset = (cursorLine * lineHeightPx).toInt()

                if (targetOffset - lineHeightPx.toInt() < verticalScrollState.value) {
                    verticalScrollState.animateScrollTo(
                        (targetOffset - lineHeightPx.toInt()).coerceAtLeast(
                            0
                        )
                    )
                } else if (targetOffset + lineHeightPx.toInt() * 4 > verticalScrollState.value + editorHeightPx) {
                    val desired = targetOffset + lineHeightPx.toInt() * 4 - editorHeightPx
                    val clamped = desired.coerceIn(0, verticalScrollState.maxValue)
                    verticalScrollState.animateScrollTo(clamped)
                }
            }
        }
    }

    LaunchedEffect(state.content.text.selection, latestTextLayoutResult) {
        updateCursorMetrics(latestTextLayoutResult, state.content.text.selection)
    }
}

@Composable
private fun LineNumberColumn(
    modifier: Modifier,
    lines: List<String>,
    selectionLine: Int,
    evaluatingLine: Int?,
    textStyle: TextStyle,
    activeColor: Color,
    defaultColor: Color,
    lineHeightDp: Dp
) {
    Column(modifier = modifier) {
        lines.forEachIndexed { index, _ ->
            val string = buildAnnotatedString {
                if (evaluatingLine == index) {
                    withStyle(SpanStyle(color = Color.Green)) {
                        append(">> ")
                    }
                }
                append("${index + 1}")
            }
            val currentColor = if (selectionLine == index) activeColor else defaultColor
            Text(
                text = string,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(lineHeightDp)
                    .padding(horizontal = 4.dp),
                style = textStyle.copy(
                    color = currentColor,
                    textAlign = TextAlign.End
                )
            )
        }
    }
}


fun TextRange.toLineIndex(lines: List<String>): Int {
    var idx = 0
    for ((i, line) in lines.withIndex()) {
        if (idx + line.length < start) {
            idx += line.length + 1
        } else {
            return i
        }
    }
    return lines.lastIndex
}
