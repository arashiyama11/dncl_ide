package io.github.arashiyama11.dncl_ide.ui

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.sp
import io.github.arashiyama11.dncl_ide.adapter.IdeViewModel
import io.github.arashiyama11.dncl_ide.adapter.TextFieldType
import io.github.arashiyama11.dncl_ide.domain.model.DebugRunningMode
import io.github.arashiyama11.dncl_ide.ui.components.TapOrRepeatButton


@Composable
fun IdeViewModel.IdeSideButtons(
    modifier: Modifier = Modifier
) {
    val uiState by uiState.collectAsState()
    var openSyntaxTemplate by remember { mutableStateOf(false) }
    val fontSize = 14.sp
    val fontSize5 = 11.sp
    Row(modifier = modifier.fillMaxHeight()) {
        AnimatedVisibility(
            openSyntaxTemplate,
            modifier = Modifier
                .fillMaxHeight()
        ) {
            if (!uiState.debugMode || uiState.debugRunningMode == DebugRunningMode.NON_BLOCKING)
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
                            Text(
                                "IF",
                                color = Color.Gray,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = fontSize
                            )
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
                            Text(
                                "ELIF",
                                color = Color.Gray,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = fontSize
                            )
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
                            Text(
                                "ELSE",
                                color = Color.Gray,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = fontSize
                            )
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
                                color = Color.Gray,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = fontSize
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
                                color = Color.Gray,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = fontSize5
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
                                color = Color.Gray,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = fontSize
                            )
                        }
                    }

                    item {
                        OutlinedButton(
                            onClick = { insertText("【外部からの入力】") },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            border = BorderStroke(4.dp, Color.Gray),
                            modifier = Modifier
                                .width(52.dp)
                                .height(36.dp)
                        ) {
                            Text(
                                "INPUT",
                                color = Color.Gray,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = fontSize5
                            )
                        }
                    }
                }
            else {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    TapOrRepeatButton(
                        modifier = Modifier.width(104.dp).weight(1f, true),
                        onClick = { onStepButtonClicked() },
                        onRepeat = { onStepButtonClicked() },
                    ) {
                        Text(
                            "Next",
                            color = Color.Gray,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = fontSize5
                        )
                    }
                    TapOrRepeatButton(
                        onClick = {
                            onLineButtonClicked()
                        },
                        onRepeat = {
                            onLineButtonClicked()
                        },
                        modifier = Modifier.width(104.dp).height(40.dp).weight(1f, true)
                    ) {
                        Text(
                            "Next Line",
                            color = Color.Gray,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = fontSize5
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
                .alpha(0.7f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(
                onClick = ::onRunButtonClicked,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = "Run")
            }

            IconButton(onClick = { onCancelButtonClicked() }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Close, contentDescription = "Stop")
            }

            IconButton(
                onClick = { onChangeIOButtonClicked() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    when (uiState.textFieldType) {
                        TextFieldType.OUTPUT -> Icons.Outlined.FileUpload
                        TextFieldType.INPUT -> Icons.Outlined.BugReport
                        TextFieldType.DEBUG_OUTPUT -> Icons.Outlined.FileDownload
                    },
                    contentDescription = "Change IO",
                )
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
