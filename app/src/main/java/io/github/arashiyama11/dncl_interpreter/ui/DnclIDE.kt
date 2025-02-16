package io.github.arashiyama11.dncl_interpreter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel


@Composable
fun DnclIDE(modifier: Modifier = Modifier, viewModel: EditorViewModel = viewModel()) {
    var openSyntaxTemplate by remember { mutableStateOf(false) }
    val uiState = viewModel.uiState.collectAsState()
    val textSizeDp = with(LocalDensity.current) {
        MaterialTheme.typography.bodyMedium.lineHeight.toDp()
    }
    Row(
        modifier
            .fillMaxSize()
            .imePadding(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .width(textSizeDp * 3)
                .padding(top = 16.dp, end = 8.dp),
            horizontalAlignment = Alignment.End
        ) {
            for (i in 0 until uiState.value.text.lines().size) {
                Text(
                    text = (i + 1).toString(),
                    modifier = Modifier,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = uiState.value.text,
                onValueChange = { viewModel.onTextChanged(it) },
                modifier = Modifier.weight(2f),
                textStyle = MaterialTheme.typography.bodyMedium,
            )

            val isError = uiState.value.stderr.isNotEmpty()

            OutlinedTextField(
                value = uiState.value.stdout + if (isError) "\n" + uiState.value.stderr else "",
                onValueChange = {},
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyMedium,
                readOnly = true,
                isError = isError
            )
        }


        Column(
            modifier = Modifier
                .width(96.dp)
                .padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(
                onClick = viewModel::onRunButtonClicked,
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
                    .rotate(if (openSyntaxTemplate) 180f else 0f)
            ) {
                Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Syntax Template")
            }

            if (openSyntaxTemplate) {
                OutlinedButton(
                    onClick = {},
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("If", color = Color.Gray)
                }

                OutlinedButton(
                    onClick = {},
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("For", color = Color.Gray)
                }

                OutlinedButton(
                    onClick = {},
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("While", color = Color.Gray)
                }
            }
        }
    }
}
