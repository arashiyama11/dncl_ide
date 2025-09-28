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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class CodeEditorConfig(
    val text: TextFieldValue,
    val annotatedText: AnnotatedString? = null,
    val fontSize: Int,
    val evaluatingLine: Int? = null,
    val verticalScrollEnabled: Boolean = true
)

data class CodeEditorStyle(
    val textStyle: TextStyle? = null,
    val lineNumberColor: Color? = null,
    val activeLineNumberColor: Color? = null,
    val cursorColor: Color? = null,
    val lineNumberColumnWidth: Dp? = null
)

sealed interface CodeEditorEvent {
    data class ContentChange(val value: TextFieldValue) : CodeEditorEvent
    data class FocusChanged(val isFocused: Boolean) : CodeEditorEvent
}

@Composable
fun CodeEditor(
    config: CodeEditorConfig,
    modifier: Modifier = Modifier,
    style: CodeEditorStyle = CodeEditorStyle(),
    onEvent: (CodeEditorEvent) -> Unit
) {
    val fontSizeDouble =
        config.fontSize.toDouble() + if (config.fontSize % 8 == 0 || config.fontSize % 8 == 3 || config.fontSize % 8 == 5) 0.2 else 0.0

    val resolvedTextStyle = (style.textStyle ?: MaterialTheme.typography.bodyMedium).copy(
        fontSize = fontSizeDouble.sp,
        lineHeight = (fontSizeDouble + 2).sp,
        fontWeight = FontWeight.Normal
    )

    var lineHeightDp = with(LocalDensity.current) {
        resolvedTextStyle.lineHeight.toDp()
    }

    val lineHeightPx = with(LocalDensity.current) { lineHeightDp.toPx() }
    val scrollState = rememberScrollState()

    var editorHeightPx by remember { mutableIntStateOf(0) }

    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    val textMeasurer = rememberTextMeasurer()

    val lines = config.text.text.lines()
    val largestLineNumberString = lines.size.coerceAtLeast(1).toString()

    val textLayoutResult = textMeasurer.measure(
        text = AnnotatedString(largestLineNumberString),
        style = resolvedTextStyle
    )

    val defaultColumnWidth = (with(LocalDensity.current) { textLayoutResult.size.width.toDp() } + 12.dp).coerceAtLeast(40.dp)
    val lineNumberColumnWidth = style.lineNumberColumnWidth ?: defaultColumnWidth

    val defaultLineNumberColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    val defaultActiveLineNumberColor = if (isSystemInDarkTheme()) Color.LightGray else MaterialTheme.colorScheme.onSurface
    val lineNumberColor = style.lineNumberColor ?: defaultLineNumberColor
    val activeLineNumberColor = style.activeLineNumberColor ?: defaultActiveLineNumberColor
    val cursorColor = style.cursorColor ?: if (isSystemInDarkTheme()) Color.White else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                editorHeightPx = coordinates.size.height
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
                    if (config.verticalScrollEnabled) {
                        verticalScroll(scrollState)
                    } else this
                }
                .background(MaterialTheme.colorScheme.background)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .width(lineNumberColumnWidth)
            ) {
                val selectLine = run {
                    var idx = 0
                    for ((i, line) in lines.withIndex()) {
                        if (idx + line.length < config.text.selection.start) {
                            idx += line.length + 1
                        } else {
                            return@run i
                        }
                    }
                    lines.lastIndex
                }
                lines.forEachIndexed { index, _ ->
                    val string = buildAnnotatedString {
                        if (config.evaluatingLine == index) {
                            withStyle(SpanStyle(color = Color.Green)) {
                                append(">> ")
                            }
                        }
                        append("${index + 1}")
                    }
                    val currentColor = if (selectLine == index) activeLineNumberColor else lineNumberColor
                    Text(
                        text = string,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(lineHeightDp)
                            .padding(horizontal = 4.dp),
                        style = resolvedTextStyle.copy(
                            color = currentColor,
                            textAlign = TextAlign.End
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            BasicTextField(
                value = config.text,
                onValueChange = { onEvent(CodeEditorEvent.ContentChange(it)) },
                textStyle = resolvedTextStyle,
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 32.dp)
                    .horizontalScroll(rememberScrollState())
                    .focusRequester(focusRequester)
                    .onFocusChanged { state ->
                        onEvent(CodeEditorEvent.FocusChanged(state.isFocused))
                    },
                cursorBrush = SolidColor(cursorColor),
                onTextLayout = { layoutResult ->
                    lineHeightDp = layoutResult.multiParagraph.getLineHeight(0).dp
                },
                decorationBox = { innerTextField ->
                    innerTextField()
                    if (config.annotatedText != null) {
                        Text(
                            text = config.annotatedText,
                            modifier = Modifier.fillMaxSize(),
                            style = resolvedTextStyle,
                            softWrap = false
                        )
                    }
                }
            )
        }
    }

    if (config.verticalScrollEnabled) {
        var scrollJob: Job? by remember { mutableStateOf(null) }

        LaunchedEffect(config.text.selection, editorHeightPx, config.fontSize) {
            scrollJob?.cancel()
            scrollJob = coroutineScope.launch {
                val linesList = config.text.text.split("\n")
                var idx = 0
                var cursorLine = 0
                for ((i, line) in linesList.withIndex()) {
                    if (idx + line.length < config.text.selection.start) {
                        idx += line.length + 1
                    } else {
                        cursorLine = i
                        break
                    }
                }
                val targetOffset = (cursorLine * lineHeightPx).toInt()

                if (targetOffset - lineHeightPx.toInt() < scrollState.value) {
                    scrollState.animateScrollTo((targetOffset - lineHeightPx.toInt()).coerceAtLeast(0))
                } else if (targetOffset + lineHeightPx.toInt() * 4 > scrollState.value + editorHeightPx) {
                    val desired = targetOffset + lineHeightPx.toInt() * 4 - editorHeightPx
                    val clamped = desired.coerceIn(0, scrollState.maxValue)
                    scrollState.animateScrollTo(clamped)
                }
            }
        }
    }
}
