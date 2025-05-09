package io.github.arashiyama11.dncl_ide.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import io.github.arashiyama11.dncl_ide.adapter.IdeViewModel
import io.github.arashiyama11.dncl_ide.adapter.TextFieldType
import io.github.arashiyama11.dncl_ide.ui.components.EnvironmentDebugView
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun DnclIDE(modifier: Modifier = Modifier, viewModel: IdeViewModel = koinViewModel()) {
    val isDarkTheme = isSystemInDarkTheme()
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        CodeEditor(
            codeText = uiState.textFieldValue,
            annotatedCodeText = uiState.annotatedString,
            onCodeChange = { viewModel.onTextChanged(it, isDarkTheme) },
            modifier = Modifier
                .weight(2f),
            fontSize = uiState.fontSize,
            currentEvaluatingLine = uiState.currentEvaluatingLine,
        )

        Row(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.Start
        ) {
            when (uiState.textFieldType) {
                TextFieldType.DEBUG_OUTPUT -> {
                    uiState.currentEnvironment?.let { environment ->
                        EnvironmentDebugView(
                            environment = environment,
                            modifier = Modifier
                                .fillMaxSize().weight(1f, fill = true)
                        )
                    } ?: run {
                        // Fallback if environment is null
                        val textFieldDesc = "デバッグ出力"
                        OutlinedTextField(
                            value = uiState.input,
                            onValueChange = { viewModel.onInputTextChanged(it) },
                            modifier = Modifier.weight(1f, fill = true)
                                .fillMaxHeight(),
                            textStyle = MaterialTheme.typography.body1,
                            label = { Text(textFieldDesc) },
                            readOnly = true
                        )
                    }
                }

                else -> {
                    val textFieldDesc = when (uiState.textFieldType) {
                        TextFieldType.OUTPUT -> "出力"
                        TextFieldType.INPUT -> "入力"
                        else -> "" // This should never happen
                    }
                    OutlinedTextField(
                        value = if (uiState.textFieldType == TextFieldType.INPUT) uiState.input else uiState.output,
                        onValueChange = { viewModel.onInputTextChanged(it) },
                        modifier = Modifier.weight(1f, fill = true)
                            .fillMaxSize(),
                        textStyle = MaterialTheme.typography.body1,
                        label = { Text(textFieldDesc) },
                        readOnly = uiState.textFieldType == TextFieldType.OUTPUT,
                    )
                }
            }

            viewModel.IdeSideButtons(
                Modifier
                    .fillMaxHeight()
            )
        }
    }
}
