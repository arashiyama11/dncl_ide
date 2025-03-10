package io.github.arashiyama11.dncl_interpreter.ui

import android.content.res.Configuration
import android.util.Log
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
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.arashiyama11.dncl_interpreter.adapter.SyntaxHighLighter
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun CodeEditor(
    codeText: TextFieldValue,
    annotatedCodeText: AnnotatedString?,
    modifier: Modifier = Modifier,
    onCodeChange: (TextFieldValue) -> Unit,
    onKeyEvent: (KeyEvent) -> Boolean = { false },
) {
    val lineHeight = with(LocalDensity.current) {
        MaterialTheme.typography.bodyMedium.lineHeight.toDp()
    }
    val lineHeightPx = with(LocalDensity.current) { lineHeight.toPx() }

    val scrollState = rememberScrollState()

    var editorHeightPx by remember { mutableIntStateOf(0) }
    var textFieldHeightPx by remember { mutableIntStateOf(0) }

    val coroutineScope = rememberCoroutineScope()

    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                editorHeightPx = coordinates.size.height
            }
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }) {
                focusRequester.requestFocus()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .background(MaterialTheme.colorScheme.background)
                .padding(8.dp)
        ) {
            // 行番号部分
            Column(
                modifier = Modifier
                    .width(40.dp)
            ) {
                val lines = codeText.text.split("\n")
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
                    Text(
                        text = "${index + 1}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(lineHeight)
                            .padding(horizontal = 4.dp),
                        color = if (selectLine == index)
                            if (isSystemInDarkTheme()) Color.LightGray else Color.Black
                        else Color.DarkGray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.End,
                        fontWeight = if (selectLine == index) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // コード入力部分
            BasicTextField(
                value = codeText,
                onValueChange = { onCodeChange(it) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    if (annotatedCodeText == null) MaterialTheme.colorScheme.onBackground else Color.Gray
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 32.dp)
                    .horizontalScroll(rememberScrollState())
                    .onGloballyPositioned { coordinates ->
                        textFieldHeightPx = coordinates.size.height
                    }
                    .onKeyEvent { onKeyEvent(it) }
                    .focusRequester(focusRequester),
                cursorBrush = SolidColor(if (isSystemInDarkTheme()) Color.White else Color.Black),
                onTextLayout = { textLayoutResult ->
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
                            style = MaterialTheme.typography.bodyMedium,
                            softWrap = false
                        )
                }
            )
        }
    }

    var scrollJob: Job? by remember { mutableStateOf(null) }

    LaunchedEffect(codeText.selection, editorHeightPx) {
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
            Log.d("CodeEditor", "cursorLine: $cursorLine")
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


@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showSystemUi = true
)
@Composable
fun CodeEditorPreview() {
    var code by remember {
        (mutableStateOf(
            TextFieldValue(
                """Data = [3,18,29,33,48,52,62,77,89,97]
kazu = 要素数(Data)
表示する("0～99の数字を入力してください")
atai = 【外部からの入力】
hidari = 0 , migi = kazu - 1
owari = 0
hidari <= migi and owari == 0 の間繰り返す:
  aida = (hidari+migi) ÷ 2 # 演算子÷は商の整数値を返す
  もし Data[aida] == atai ならば:
    表示する(atai, "は", aida, "番目にありました")
    owari = 1
  そうでなくもし Data[aida] < atai ならば:
    hidari = aida + 1
  そうでなければ:
    migi = aida - 1
もし owari == 0 ならば:
  表示する(atai, "は見つかりませんでした")
表示する("添字", " ", "要素")
i を 0 から kazu - 1 まで 1 ずつ増やしながら繰り返す:
  表示する(i, " ", Data[i])"""
            )
        ))
    }

    val annotatedCodeText by remember {
        mutableStateOf(
            SyntaxHighLighter()(
                code.text,
                true,
                null
            )
        )
    }


    CodeEditor(
        code, annotatedCodeText.first, onCodeChange = { code = it }
    )
}