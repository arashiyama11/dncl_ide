package io.github.arashiyama11.dncl_interpreter.ui

import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel


@Composable
fun DnclIDE(modifier: Modifier = Modifier, viewModel: IdeViewModel = viewModel()) {
    var openSyntaxTemplate by remember { mutableStateOf(false) }
    val isDarkTheme = isSystemInDarkTheme()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onStart(isDarkTheme)
    }


    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        CodeEditor(
            codeText = uiState.textFieldValue,
            annotatedCodeText = uiState.annotatedString,
            onCodeChange = { viewModel.onTextChanged(it, isDarkTheme) },
            modifier = Modifier.weight(2f)
        )

        Row(
            Modifier
                .fillMaxWidth()
                .weight(1f), //horizontalArrangement = Arrangement.End
        ) {

            OutlinedTextField(
                value = uiState.output,
                onValueChange = {},
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f),
                textStyle = MaterialTheme.typography.bodyMedium,
                readOnly = true,
                isError = uiState.isError
            )

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(96.dp)
                    .padding(horizontal = 8.dp)
                    .alpha(0.7f),
                horizontalAlignment = Alignment.CenterHorizontally
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
}
