package io.github.arashiyama11.dncl_ide.util

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.window.ComposeViewport
import io.github.arashiyama11.dncl_ide.adapter.IdeUiState
import io.github.arashiyama11.dncl_ide.adapter.IdeViewModel
import io.github.arashiyama11.dncl_ide.adapter.TextFieldType
import io.github.arashiyama11.dncl_ide.ui.DnclIdeTheme
import io.github.arashiyama11.dncl_ide.ui.LocalCodeTypography
import io.github.arashiyama11.dncl_ide.ui.components.EnvironmentDebugView
import io.github.arashiyama11.dncl_ide.ui.components.IdeSideButtons
import io.github.arashiyama11.dncl_ide.ui.layout.Editor
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.Koin
import org.w3c.dom.HTMLElement

@OptIn(ExperimentalComposeUiApi::class, ExperimentalWasmJsInterop::class)
context(koin: Koin)
fun webApp() {
    val composeElem = document.getElementById("compose-canvas")!! as HTMLElement
    val controllerElem = document.getElementById("controller")!! as HTMLElement
    val monacoElem = document.getElementById("monaco-editor")!! as HTMLElement

    val isMonacoEditorState = MutableStateFlow(false)
    val viewModel by koin.inject<IdeViewModel>()

    onMonacoLoaded {
        it.onDidChangeModelContent {
            if (isMonacoEditorState.value) return@onDidChangeModelContent
            viewModel.onTextChanged(TextFieldValue(monaco.getValue()))
            println("Content changed: ${monaco.getValue()}")
        }
    }

    ComposeViewport(controllerElem) {
        val isMonacoEditor by isMonacoEditorState.collectAsState()
        val uiState by viewModel.uiState.collectAsState()
        DnclIdeTheme {
            ControllerUi(
                isMonacoEditor,
                {
                    isMonacoEditorState.value = it
                },
                viewModel, uiState, Modifier.background(MaterialTheme.colorScheme.background)
            )
        }
    }

    ComposeViewport(composeElem) {
        val isMonacoEditor by isMonacoEditorState.collectAsState()
        val uiState by viewModel.uiState.collectAsState()

        LaunchedEffect(isMonacoEditor) {
            if (!isMonacoEditor) {
                composeElem.style.visibility = "visible"
                monacoElem.style.visibility = "hidden"
            } else {
                composeElem.style.visibility = "hidden"
                monacoElem.style.visibility = "visible"

                window.requestAnimationFrame {
                    monaco.layout()
                }
            }
        }

        DnclIdeTheme {
            LaunchedEffect(Unit) {
                viewModel.onStart(MutableStateFlow(true))
            }

            if (!isMonacoEditor) {
                Editor(
                    uiState = uiState,
                    viewModel = viewModel,
                    modifier = Modifier
                        .fillMaxSize()
                )
            }
        }
    }
}


@Composable
fun ControllerUi(
    isMonacoEditor: Boolean,
    setIsMonacoEditor: (Boolean) -> Unit,
    viewModel: IdeViewModel,
    uiState: IdeUiState,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Row {
            Text(
                "Monaco Editor (WIP)",
                color = MaterialTheme.colorScheme.onBackground
            )
            Switch(isMonacoEditor, setIsMonacoEditor)
        }

        Row(
            modifier = Modifier
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
