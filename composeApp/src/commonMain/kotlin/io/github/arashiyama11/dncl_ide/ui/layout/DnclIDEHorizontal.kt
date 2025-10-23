package io.github.arashiyama11.dncl_ide.ui.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.github.arashiyama11.dncl_ide.adapter.IdeUiState
import io.github.arashiyama11.dncl_ide.adapter.IdeViewModel
import io.github.arashiyama11.dncl_ide.adapter.TextFieldType
import io.github.arashiyama11.dncl_ide.editor.compose.CodeEditor
import io.github.arashiyama11.dncl_ide.editor.compose.BindCodeEditorState
import io.github.arashiyama11.dncl_ide.editor.compose.CodeEditorController
import io.github.arashiyama11.dncl_ide.editor.compose.CodeEditorOptions
import io.github.arashiyama11.dncl_ide.editor.compose.CodeEditorState
import io.github.arashiyama11.dncl_ide.editor.compose.rememberCodeEditorController
import io.github.arashiyama11.dncl_ide.editor.compose.rememberCodeEditorState
import io.github.arashiyama11.dncl_ide.editor.core.EditorContent
import io.github.arashiyama11.dncl_ide.ui.LocalCodeTypography
import io.github.arashiyama11.dncl_ide.ui.components.EnvironmentDebugView
import io.github.arashiyama11.dncl_ide.ui.components.IdeSideButtons
import io.github.arashiyama11.dncl_ide.ui.components.InlineSuggestionPopup
import io.github.arashiyama11.dncl_ide.ui.components.SuggestionStripView
import io.github.arashiyama11.dncl_ide.domain.model.SuggestionPanelStyle
import org.koin.compose.viewmodel.koinViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnclIDEHorizontal(modifier: Modifier = Modifier, viewModel: IdeViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
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

        Editor(
            uiState = uiState,
            viewModel = viewModel,
            modifier = Modifier
                .weight(2f)
                .fillMaxHeight()
        )


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

@Composable
fun Editor(
    uiState: IdeUiState,
    viewModel: IdeViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
    ) {
        val editorController: CodeEditorController = rememberCodeEditorController()
        val editorContent = EditorContent(text = uiState.codeTextFieldValue)
        val editorState: CodeEditorState = rememberCodeEditorState(
            content = editorContent,
            annotatedText = uiState.annotatedString,
            evaluatingLine = uiState.currentEvaluatingLine,
            verticalScrollEnabled = true,
        )

        BindCodeEditorState(
            state = editorState,
            content = editorContent,
            annotatedText = uiState.annotatedString,
            highlightRevision = uiState.highlightRevision,
            evaluatingLine = uiState.currentEvaluatingLine,
            verticalScrollEnabled = true,
        )

        LaunchedEffect(
            uiState.annotatedString,
            uiState.codeTextFieldValue.text,
            uiState.highlightRevision
        ) {
            if (
                uiState.highlightRevision == 0L &&
                uiState.annotatedString == null &&
                uiState.codeTextFieldValue.text.isNotEmpty()
            ) {
                viewModel.onTextChanged(uiState.codeTextFieldValue)
            }
        }

        LaunchedEffect(uiState.codeTextFieldValue) {
            val currentContent = editorState.content
            val nextRevision = if (
                currentContent.text == uiState.codeTextFieldValue
            ) {
                currentContent.revision
            } else {
                currentContent.revision + 1
            }
            editorState.updateContent(
                content = EditorContent(
                    text = uiState.codeTextFieldValue,
                    revision = nextRevision
                ),
            )
        }

        LaunchedEffect(uiState.currentEvaluatingLine) {
            editorState.updateEvaluatingLine(uiState.currentEvaluatingLine)
        }

        LaunchedEffect(editorController) {
            editorController.events.contentChanges.collectLatest { event ->
                val content = event.update.content
                viewModel.onTextChanged(content.text)
            }
        }

        LaunchedEffect(editorController) {
            editorController.events.focusChanges.collectLatest { event ->
                viewModel.onCodeEditorFocused(event.isFocused)
            }
        }

        val editorOptions = CodeEditorOptions(
            fontSize = uiState.fontSize,
            textStyle = LocalCodeTypography.current.bodyMedium,
            verticalScrollEnabled = true
        )

        val suggestionPanelStyle = uiState.suggestionPanelStyle
        val canRenderInline = editorState.cursorAnchorInEditor != null && editorState.cursorLineHeightPx != null
        val showSuggestionStrip = uiState.isFocused && uiState.showInlineSuggestions
        val shouldRenderInlineSuggestions =
            suggestionPanelStyle == SuggestionPanelStyle.INLINE_DROPDOWN &&
                shouldShowInlineSuggestions(uiState, editorState)

        Box(modifier = Modifier.weight(1f)) {
            CodeEditor(
                state = editorState,
                modifier = Modifier.fillMaxSize(),
                options = editorOptions,
                controller = editorController
            )

            if (shouldRenderInlineSuggestions) {
                InlineSuggestionPopup(
                    suggestions = uiState.textSuggestions,
                    cursorAnchor = editorState.cursorAnchorInEditor,
                    lineHeightPx = editorState.cursorLineHeightPx,
                    modifier = Modifier.fillMaxSize(),
                    onConfirmTextSuggestion = { viewModel.onConfirmTextSuggestion(it) },
                    onRequestEditorFocus = { editorController.requestFocus() }
                )
            }
        }

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

        val shouldShowStrip = suggestionPanelStyle == SuggestionPanelStyle.BOTTOM_STRIP || !canRenderInline

        if (shouldShowStrip) {
            AnimatedVisibility(showSuggestionStrip) {
                SuggestionStripView(
                    uiState.textSuggestions,
                    modifier = Modifier.height(48.dp).background(MaterialTheme.colorScheme.surface)
                ) { viewModel.onConfirmTextSuggestion(it) }
            }
        }
    }
}
