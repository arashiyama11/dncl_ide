package io.github.arashiyama11.dncl_ide.ui

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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun CodeEditor(
    codeText: TextFieldValue,
    annotatedCodeText: AnnotatedString?,
    modifier: Modifier = Modifier,
    fontSize: Int,
    onCodeChange: (TextFieldValue) -> Unit,
    currentEvaluatingLine: Int? = null,
) {
    val fontSizeDouble =
        fontSize.toDouble() + if (fontSize % 8 == 0 || fontSize % 8 == 3 || fontSize % 8 == 5) 0.2 else 0.0
    val codeStyle = TextStyle(
        fontSize = fontSizeDouble.sp,
        lineHeight = (fontSizeDouble + 2).sp
    )

    var lineHeightDp = with(LocalDensity.current) {
        codeStyle.lineHeight.toDp()
    }

    val lineHeightPx = with(LocalDensity.current) { lineHeightDp.toPx() }
    val scrollState = rememberScrollState()

    var editorHeightPx by remember { mutableIntStateOf(0) }
    var textFieldHeightPx by remember { mutableIntStateOf(0) }

    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    val textMeasurer = rememberTextMeasurer()

    val lines = codeText.text.lines()

    val largestLineNumberString = lines.size.toString()

    val textLayoutResult = textMeasurer.measure(
        text = AnnotatedString(largestLineNumberString),
        style = codeStyle
    )

    val measuredLineNumberWidth = with(LocalDensity.current) { textLayoutResult.size.width.toDp() }
    val lineNumberColumnWidth = (measuredLineNumberWidth + 12.dp).coerceAtLeast(40.dp)

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
                .verticalScroll(scrollState)
                .background(MaterialTheme.colors.background)
                .padding(8.dp)
        ) {
            // 行番号部分
            Column(
                modifier = Modifier
                    .width(lineNumberColumnWidth)
            ) {
                val selectLine = run {
                    var idx = 0
                    for ((i, line) in lines.withIndex()) {
                        if (idx + line.length < codeText.selection.start) {
                            idx += line.length + 1
                        } else {
                            return@run i
                        }
                    }
                    lines.lastIndex
                }
                lines.forEachIndexed { index, _ ->
                    val string = buildAnnotatedString {
                        if (currentEvaluatingLine == index) {
                            withStyle(SpanStyle(color = Color.Green)) {
                                append(">> ")
                            }
                        }
                        append("${index + 1}")
                    }
                    Text(
                        text = string,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(lineHeightDp)
                            .padding(horizontal = 4.dp),
                        //.background(
                        /* if (currentEvaluatingLine == index)
                             if (isSystemInDarkTheme()) Color(0xFF3F51B5) else Color(0xFFBBDEFB)
                         else*? Color.Transparent
                     ),*/
                        style = codeStyle.copy(
                            color = if (selectLine == index)
                                if (isSystemInDarkTheme()) Color.LightGray else Color.Black
                            else Color.DarkGray,
                            textAlign = TextAlign.End
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // コード入力部分
            BasicTextField(
                value = codeText,
                onValueChange = { onCodeChange(it) },
                textStyle = codeStyle,
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 32.dp)
                    .horizontalScroll(rememberScrollState())
                    .onGloballyPositioned { coordinates ->
                        textFieldHeightPx = coordinates.size.height
                    }
                    .focusRequester(focusRequester),
                cursorBrush = SolidColor(if (isSystemInDarkTheme()) Color.White else Color.Black),
                onTextLayout = { textLayoutResult ->
                    lineHeightDp = textLayoutResult.multiParagraph.getLineHeight(0).dp
                    val cursorOffset = codeText.selection.start
                    val cursorRect = textLayoutResult.getCursorRect(cursorOffset)
                    val margin = 32f
                    if (cursorRect.top < margin) {
                        coroutineScope.launch {
                            val delta = (margin - cursorRect.top).toInt()
                            scrollState.animateScrollTo((scrollState.value - delta).coerceAtLeast(0))
                        }
                    } else if (cursorRect.bottom > textFieldHeightPx - margin) {
                        coroutineScope.launch {
                            val delta = (cursorRect.bottom - (textFieldHeightPx - margin)).toInt()
                            scrollState.animateScrollTo(scrollState.value + delta)
                        }
                    }
                },
                decorationBox = { innerTextField ->
                    innerTextField()
                    if (annotatedCodeText != null)
                        Text(
                            text = annotatedCodeText,
                            modifier = Modifier.fillMaxSize(),
                            style = codeStyle,
                            softWrap = false
                        )
                }
            )
        }
    }

    var scrollJob: Job? by remember { mutableStateOf(null) }

    LaunchedEffect(codeText.selection, editorHeightPx, fontSize) {
        scrollJob?.cancel()
        scrollJob = launch {
            val lines = codeText.text.split("\n")
            var idx = 0
            var cursorLine = 0
            for ((i, line) in lines.withIndex()) {
                if (idx + line.length < codeText.selection.start) {
                    idx += line.length + 1
                } else {
                    cursorLine = i
                    break
                }
            }
            val targetOffset = (cursorLine * lineHeightPx).toInt()

            // 表示領域からカーソルがはみ出している場合にスクロール調整
            if (targetOffset - lineHeightPx.toInt() < scrollState.value) {
                scrollState.animateScrollTo(targetOffset - lineHeightPx.toInt())
            } else if (targetOffset + lineHeightPx.toInt() * 4 > scrollState.value + editorHeightPx) {
                if (targetOffset + lineHeightPx.toInt() * 4 - editorHeightPx > scrollState.maxValue)
                    scrollState.animateScrollTo(scrollState.maxValue)
                else
                    scrollState.animateScrollTo(targetOffset + lineHeightPx.toInt() * 4 - editorHeightPx)
            }
        }
    }
}
