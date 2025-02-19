package io.github.arashiyama11.dncl_interpreter.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


@Composable
fun IdeSideButtons(
    onRunButtonClicked: () -> Unit,
    insertText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    //TODO アニメーションをつける
    var openSyntaxTemplate by remember { mutableStateOf(false) }
    Row(modifier = modifier.fillMaxHeight()) {
        AnimatedVisibility(
            openSyntaxTemplate,
            modifier = Modifier
                .fillMaxHeight()
        ) {
            LazyVerticalGrid(
                GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(top = 8.dp)
                    .widthIn(52.dp, 104.dp),
            ) {
                item {
                    OutlinedButton(
                        onClick = { insertText("もし 1 ならば:") },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp),
                        border = BorderStroke(4.dp, Color.Gray),
                        modifier = Modifier
                            .defaultMinSize(1.dp, 1.dp)
                            .width(52.dp)
                            .height(36.dp)
                    ) {
                        Text("IF", color = Color.Gray, fontWeight = FontWeight.ExtraBold)
                    }
                }

                item {
                    OutlinedButton(
                        onClick = { insertText("そうでなくもし 1 ならば:") },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(1.dp),
                        border = BorderStroke(4.dp, Color.Gray),
                        modifier = Modifier
                            .defaultMinSize(1.dp, 1.dp)
                            .width(52.dp)
                            .height(36.dp)
                    ) {
                        Text("ELIF", color = Color.Gray, fontWeight = FontWeight.ExtraBold)
                    }
                }

                item {
                    OutlinedButton(
                        onClick = { insertText("そうでなければ:") },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(1.dp),
                        border = BorderStroke(4.dp, Color.Gray),
                        modifier = Modifier
                            .defaultMinSize(1.dp, 1.dp)
                            .width(52.dp)
                            .height(36.dp)
                    ) {
                        Text("ELSE", color = Color.Gray, fontWeight = FontWeight.ExtraBold)
                    }
                }

                item {
                    OutlinedButton(
                        onClick = { insertText("i を 1 から 10 まで 1 ずつ増やしながら繰り返す:") },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp),
                        border = BorderStroke(4.dp, Color.Gray),
                        modifier = Modifier
                            .width(52.dp)
                            .height(36.dp)
                    ) {
                        Text(
                            "FOR",
                            color = Color.Gray, fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                item {
                    OutlinedButton(
                        onClick = { insertText("i < 10 の間繰り返す:") },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        border = BorderStroke(4.dp, Color.Gray),
                        modifier = Modifier
                            .width(52.dp)
                            .height(36.dp)
                    ) {
                        Text(
                            "WHILE",
                            color = Color.Gray, fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                item {
                    OutlinedButton(
                        onClick = { insertText("関数 f(x) を:\n  戻り値(x+1)\nと定義する") },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp),
                        border = BorderStroke(4.dp, Color.Gray),
                        modifier = Modifier
                            .width(52.dp)
                            .height(36.dp)
                    ) {
                        Text(
                            "FUNC",
                            color = Color.Gray, fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                item {
                    OutlinedButton(
                        onClick = { insertText("表示する()") },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        border = BorderStroke(4.dp, Color.Gray),
                        modifier = Modifier
                            .width(52.dp)
                            .height(36.dp)
                    ) {
                        Text(
                            "PRINT",
                            color = Color.Gray, fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                item {
                    OutlinedButton(
                        onClick = { insertText("要素数()") },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp),
                        border = BorderStroke(4.dp, Color.Gray),
                        modifier = Modifier
                            .width(52.dp)
                            .height(36.dp)
                    ) {
                        Text(
                            "LEN",
                            color = Color.Gray, fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(56.dp)
                .padding(end = 8.dp)
                .alpha(0.7f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(
                onClick = onRunButtonClicked,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = "Run")
            }

            IconButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Warning, contentDescription = "Stop")
            }

            IconButton(
                onClick = {
                    openSyntaxTemplate = !openSyntaxTemplate
                }, modifier = Modifier
                    .fillMaxWidth()
                    .rotate(if (openSyntaxTemplate) 270f else 90f)
            ) {
                Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Syntax Template")
            }
        }
    }
}