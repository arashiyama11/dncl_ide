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
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.arashiyama11.dncl_ide.adapter.IdeViewModel
import io.github.arashiyama11.dncl_ide.adapter.TextFieldType
import io.github.arashiyama11.dncl_ide.adapter.TextInputMode
import io.github.arashiyama11.dncl_ide.editor.compose.CodeEditor
import io.github.arashiyama11.dncl_ide.editor.compose.CodeEditorController
import io.github.arashiyama11.dncl_ide.editor.compose.CodeEditorOptions
import io.github.arashiyama11.dncl_ide.editor.compose.CodeEditorState
import io.github.arashiyama11.dncl_ide.editor.compose.BindCodeEditorState
import io.github.arashiyama11.dncl_ide.editor.compose.rememberCodeEditorController
import io.github.arashiyama11.dncl_ide.editor.compose.rememberCodeEditorState
import io.github.arashiyama11.dncl_ide.editor.core.EditorContent
import io.github.arashiyama11.dncl_ide.ui.LocalCodeTypography
import io.github.arashiyama11.dncl_ide.ui.components.CanvasAwareOutputField
import io.github.arashiyama11.dncl_ide.ui.components.EnvironmentDebugView
import io.github.arashiyama11.dncl_ide.ui.components.IdeSideButtons
import io.github.arashiyama11.dncl_ide.ui.components.InlineSuggestionPopup
import io.github.arashiyama11.dncl_ide.ui.components.SuggestionStripView
import io.github.arashiyama11.dncl_ide.ui.components.CustomImePanel
import io.github.arashiyama11.dncl_ide.ui.components.isImeVisible
import io.github.arashiyama11.dncl_ide.domain.model.SuggestionPanelStyle
import io.github.arashiyama11.dncl_ide.util.Platform
import io.github.arashiyama11.dncl_ide.util.currentPlatform
import kotlinx.coroutines.flow.collectLatest


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnclIDEVertical(modifier: Modifier = Modifier, viewModel: IdeViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val isMobilePlatform = currentPlatform == Platform.Android || currentPlatform == Platform.Ios
    val isCustomMode = uiState.textInputMode == TextInputMode.CUSTOM
    val fileDisplayName = uiState.selectedEntryPath?.value?.lastOrNull()?.value.orEmpty()
    val keyboardController = LocalSoftwareKeyboardController.current
    val latestIsCustomMode = rememberUpdatedState(isCustomMode)
    val baseTextInputService = LocalTextInputService.current
    val systemImeVisible = if (isMobilePlatform) isImeVisible() else false

    LaunchedEffect(isCustomMode, uiState.isFocused) {
        if (!isMobilePlatform) return@LaunchedEffect
        keyboardController?.let { controller ->
            if (isCustomMode) {
                controller.hide()
            } else if (uiState.isFocused) {
                controller.show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = fileDisplayName,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                FilterChip(
                    selected = isCustomMode,
                    onClick = { viewModel.toggleTextInputMode() },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Keyboard,
                            contentDescription = null
                        )
                    },
                    label = {
                        Text(if (isCustomMode) "専用IME中" else "専用IME")
                    }
                )
            }
        }

        HorizontalDivider()

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
            viewModel.onTextChanged(uiState.codeTextFieldValue)
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

        LaunchedEffect(isCustomMode) {
            if (isCustomMode) {
                editorController.requestFocus()
                if (isMobilePlatform) {
                    keyboardController?.hide()
                }
            } else if (isMobilePlatform) {
                editorController.requestFocus()
                keyboardController?.show()
            }
        }

        LaunchedEffect(editorController) {
            editorController.events.contentChanges.collectLatest { event ->
                val content = event.update.content
                viewModel.onTextChanged(
                    text = content.text,
                )
            }
        }

        LaunchedEffect(editorController, keyboardController) {
            editorController.events.focusChanges.collectLatest { event ->
                viewModel.onCodeEditorFocused(event.isFocused)
                if (!isMobilePlatform) return@collectLatest
                if (event.isFocused) {
                    if (latestIsCustomMode.value) {
                        keyboardController?.hide()
                    } else {
                        keyboardController?.show()
                    }
                }
            }
        }

        val shouldDisableSystemKeyboard = isCustomMode && isMobilePlatform
        val codeEditorKeyboardOptions = if (shouldDisableSystemKeyboard) {
            KeyboardOptions.Default.copy(showKeyboardOnFocus = false)
        } else {
            KeyboardOptions.Default
        }

        val editorOptions = CodeEditorOptions(
            fontSize = uiState.fontSize,
            textStyle = LocalCodeTypography.current.bodyMedium,
            verticalScrollEnabled = true,
            keyboardOptions = codeEditorKeyboardOptions,
            readOnly = shouldDisableSystemKeyboard
        )

        val suggestionPanelStyle = uiState.suggestionPanelStyle
        val canRenderInline =
            editorState.cursorAnchorInEditor != null && editorState.cursorLineHeightPx != null
        val showSuggestionStrip =
            uiState.isFocused && uiState.showInlineSuggestions && !isCustomMode
        val shouldRenderInlineSuggestions =
            !isCustomMode &&
                    suggestionPanelStyle == SuggestionPanelStyle.INLINE_DROPDOWN &&
                    shouldShowInlineSuggestions(uiState, editorState)

        val editorBox: @Composable (Modifier) -> Unit = { boxModifier ->
            Box(modifier = boxModifier) {
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
        }

        if (shouldDisableSystemKeyboard && baseTextInputService != null) {
            CompositionLocalProvider(LocalTextInputService provides null) {
                editorBox(Modifier.weight(2f))
            }
        } else {
            editorBox(Modifier.weight(2f))
        }

        // Conditionally display Input Row when isWaitingForInput is true
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
                        enabled = uiState.running// Should always be true if waiting for input
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "送信")
                    }
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .weight(1f, fill = true),
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
                            value = uiState.output, // Debug output shows general output when env is null
                            onValueChange = { }, // ReadOnly
                            modifier = Modifier.weight(1f, fill = true)
                                .fillMaxHeight(),
                            textStyle = MaterialTheme.typography.bodyLarge,
                            label = { Text(textFieldDesc) },
                            readOnly = true
                        )
                    }
                }

                TextFieldType.OUTPUT -> {
                    CanvasAwareOutputField(
                        uiState = uiState,
                        onSelectPane = viewModel::selectOutputPane,
                        onSelectCanvas = viewModel::selectCanvasSurface,
                        modifier = Modifier.weight(1f, fill = true)
                    )
                }
            }
            with(viewModel) {
                IdeSideButtons(Modifier.fillMaxHeight())
            }
        }

        val shouldShowStrip = !isCustomMode &&
                (suggestionPanelStyle == SuggestionPanelStyle.BOTTOM_STRIP || !canRenderInline)
        if (shouldShowStrip) {
            AnimatedVisibility(showSuggestionStrip) {
                SuggestionStripView(
                    uiState.textSuggestions,
                    modifier = Modifier.height(48.dp)
                ) { viewModel.onConfirmTextSuggestion(it) }
            }
        }

        val shouldShowCustomImePanel = when {
            !isCustomMode -> false
            isMobilePlatform -> !systemImeVisible
            else -> true
        }

        AnimatedVisibility(
            visible = shouldShowCustomImePanel,
            modifier = Modifier.heightIn(max = 320.dp)
        ) {
            CustomImePanel(
                snippets = uiState.customImeSnippets,
                quickKeys = uiState.customImeQuickKeys,
                keywords = uiState.customImeKeywords,
                panelMode = uiState.customImePanelMode,
                onModeChange = { viewModel.onCustomImePanelModeChange(it) },
                onQuickKeyClick = {
                    viewModel.onCustomImeQuickKeySelected(it)
                    editorController.requestFocus()
                },
                onKeywordClick = {
                    viewModel.onCustomImeKeywordSelected(it)
                    editorController.requestFocus()
                },
                onSnippetClick = {
                    viewModel.onCustomImeSnippetSelected(it)
                    editorController.requestFocus()
                },
                onInsertNewLine = {
                    viewModel.onCustomImeInsertNewLine()
                    editorController.requestFocus()
                },
                onDeleteBackward = {
                    viewModel.onCustomImeDeleteBackward()
                    editorController.requestFocus()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .padding(vertical = 4.dp)
            )
        }
    }
}
