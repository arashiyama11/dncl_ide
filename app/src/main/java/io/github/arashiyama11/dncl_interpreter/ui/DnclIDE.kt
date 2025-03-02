package io.github.arashiyama11.dncl_interpreter.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
            onKeyEvent = { viewModel.onEditorKeyEvent(it) },
            modifier = Modifier
                .weight(2f)
        )

        Row(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.End
        ) {
            if (uiState.isInputMode)
                OutlinedTextField(
                    value = uiState.input,
                    onValueChange = { viewModel.onInputTextChanged(it) },
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    label = { Text("入力") }
                )
            else
                OutlinedTextField(
                    value = uiState.output,
                    onValueChange = {},
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    readOnly = true,
                    isError = uiState.isError,
                    label = { Text("出力") }
                )

            IdeSideButtons(
                onRunButtonClicked = { viewModel.onRunButtonClicked() },
                onCancelButtonClicked = { viewModel.onCancelButtonClicked() },
                insertText = { viewModel.insertText(it) },
                onChangeIOButtonClicked = { viewModel.onChangeIOButtonClicked() },
                isInputMode = uiState.isInputMode,
                modifier = Modifier
                    .fillMaxHeight()
            )
        }
    }
}
