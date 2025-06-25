package io.github.arashiyama11.dncl_ide.ui.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.arashiyama11.dncl_ide.adapter.IdeViewModel
import io.github.arashiyama11.dncl_ide.adapter.TextFieldType
import io.github.arashiyama11.dncl_ide.ui.LocalCodeTypography
import io.github.arashiyama11.dncl_ide.ui.components.CodeEditor
import io.github.arashiyama11.dncl_ide.ui.components.EnvironmentDebugView
import io.github.arashiyama11.dncl_ide.ui.components.IdeSideButtons
import io.github.arashiyama11.dncl_ide.ui.components.SuggestionListView
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnclIDEHorizontal(modifier: Modifier = Modifier, viewModel: IdeViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Row(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .weight(2f)
                .fillMaxHeight()
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                tonalElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(uiState.selectedEntryPath?.value?.lastOrNull()?.value.orEmpty())
                }
            }

            HorizontalDivider()
            CodeEditor(
                codeText = uiState.codeTextFieldValue,
                annotatedCodeText = uiState.annotatedString,
                onCodeChange = { viewModel.onTextChanged(it) },
                modifier = Modifier.weight(1f),
                fontSize = uiState.fontSize,
                currentEvaluatingLine = uiState.currentEvaluatingLine,
                onFocused = { viewModel.onCodeEditorFocused(it) },
                verticalScroll = true
            )

            if (uiState.isWaitingForInput) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.currentInput,
                            onValueChange = { viewModel.onCurrentInputChanged(it) },
                            modifier = Modifier.weight(1f),
                            label = { Text("入力待ち...") },
                        )
                        Button(
                            onClick = { viewModel.onSendInputClicked() },
                            enabled = uiState.running
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "送信")
                        }
                    }
                }
            }

            AnimatedVisibility(uiState.isFocused) {
                SuggestionListView(
                    uiState.textSuggestions,
                    modifier = Modifier.height(48.dp)
                ) { viewModel.onConfirmTextSuggestion(it) }
            }
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            when (uiState.textFieldType) {
                TextFieldType.DEBUG_OUTPUT -> {
                    uiState.currentEnvironment?.let { environment ->
                        EnvironmentDebugView(
                            environment = environment,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    } ?: run {
                        val textFieldDesc = "デバッグ出力"
                        OutlinedTextField(
                            value = uiState.output,
                            onValueChange = { },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            textStyle = MaterialTheme.typography.bodyLarge,
                            label = { Text(textFieldDesc) },
                            readOnly = true
                        )
                    }
                }

                TextFieldType.OUTPUT -> {
                    val textFieldDesc = "出力"
                    OutlinedTextField(
                        value = uiState.output,
                        onValueChange = { },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        textStyle = LocalCodeTypography.current.bodyLarge,
                        label = { Text(textFieldDesc) },
                        readOnly = true,
                    )
                }
            }
            with(viewModel) {
                IdeSideButtons(Modifier.fillMaxHeight())
            }
        }
    }
}

