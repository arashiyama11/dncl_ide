package io.github.arashiyama11.dncl_interpreter.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun CodeEditor(
    codeText: TextFieldValue,
    annotatedCodeText: AnnotatedString?,
    onCodeChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier
) {
    val lineHeight = with(LocalDensity.current) {
        MaterialTheme.typography.bodyMedium.lineHeight.toDp()
    }

    val scrollState = rememberScrollState()


    Row(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(MaterialTheme.colorScheme.background)
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .width(40.dp)
                .padding(vertical = 8.dp)
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
                return@run null
            }
            lines.forEachIndexed { index, _ ->
                Text(
                    text = "${index + 1}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(lineHeight)
                        .padding(horizontal = 4.dp),
                    color = if (selectLine == index) if (isSystemInDarkTheme()) Color.LightGray else Color.Black else Color.DarkGray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.End,
                    fontWeight = if (selectLine == index) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        BasicTextField(
            value = codeText,
            onValueChange = { onCodeChange(it) },
            textStyle = MaterialTheme.typography.bodyMedium.copy(if (annotatedCodeText == null) MaterialTheme.colorScheme.onBackground else Color.Gray),
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
                .horizontalScroll(rememberScrollState()),
            cursorBrush = SolidColor(if (isSystemInDarkTheme()) Color.White else Color.Black),
            decorationBox = { innerTextField ->
                innerTextField()
                if (annotatedCodeText != null) Text(
                    text = annotatedCodeText,
                    modifier = Modifier.fillMaxSize(),
                    style = MaterialTheme.typography.bodyMedium,
                    softWrap = false
                )
            }
        )
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


    CodeEditor(
        code, null, { code = it }
    )
}