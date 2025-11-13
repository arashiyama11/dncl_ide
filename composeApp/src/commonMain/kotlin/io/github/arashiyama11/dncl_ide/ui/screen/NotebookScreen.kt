package io.github.arashiyama11.dncl_ide.ui.screen

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikepenz.markdown.compose.LocalMarkdownColors
import com.mikepenz.markdown.compose.LocalMarkdownTypography
import com.mikepenz.markdown.compose.Markdown
import io.github.arashiyama11.dncl_ide.adapter.CodeCellState
import io.github.arashiyama11.dncl_ide.adapter.NotebookAction
import io.github.arashiyama11.dncl_ide.adapter.NotebookUiState
import io.github.arashiyama11.dncl_ide.adapter.NotebookViewModel
import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.domain.notebook.Cell
import io.github.arashiyama11.dncl_ide.domain.notebook.CellType
import io.github.arashiyama11.dncl_ide.domain.notebook.Output
import io.github.arashiyama11.dncl_ide.editor.compose.CodeEditor
import io.github.arashiyama11.dncl_ide.editor.compose.BindCodeEditorState
import io.github.arashiyama11.dncl_ide.editor.compose.CodeEditorController
import io.github.arashiyama11.dncl_ide.editor.compose.CodeEditorOptions
import io.github.arashiyama11.dncl_ide.editor.compose.CodeEditorState
import io.github.arashiyama11.dncl_ide.editor.compose.rememberCodeEditorController
import io.github.arashiyama11.dncl_ide.editor.compose.rememberCodeEditorState
import io.github.arashiyama11.dncl_ide.editor.core.EditorContent
import io.github.arashiyama11.dncl_ide.editor.core.EditorContentUpdate
import io.github.arashiyama11.dncl_ide.ui.LocalCodeTypography
import io.github.arashiyama11.dncl_ide.ui.components.SuggestionListView
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest


@Composable
fun NotebookScreen(
    modifier: Modifier = Modifier,
    notebookViewModel: NotebookViewModel,
) {
    val uiState by notebookViewModel.uiState.collectAsStateWithLifecycle()
    val onAction = remember(notebookViewModel) { notebookViewModel::handleAction }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        if (uiState.notebook == null || uiState.loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            NotebookContent(
                uiState = uiState,
                onAction = onAction,
                onSave = notebookViewModel::saveNotebook
            )
        }
    }
}

@Composable
fun NotebookContent(
    uiState: NotebookUiState,
    onAction: (NotebookAction) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        NotebookToolbarComponent(uiState, onAction, onSave)
        CellListComponent(uiState, onAction, Modifier.weight(1f))
    }
}

@Composable
fun NotebookToolbarComponent(
    uiState: NotebookUiState,
    onAction: (NotebookAction) -> Unit,
    onSave: () -> Unit
) {
    NotebookToolbar(
        selectedEntryPath = uiState.selectedEntryPath,
        onExecuteAllCells = { onAction(NotebookAction.ExecuteAllCells) },
        onCancelExecution = { onAction(NotebookAction.CancelExecution) },
        unsavedChanges = uiState.unsavedChanges,
        onSave = onSave,
        running = uiState.running
    )
}

@Composable
fun CellListComponent(
    uiState: NotebookUiState,
    onAction: (NotebookAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        for (cellId in uiState.cellIds) {
            key(cellId) {
                CellComponent(
                    uiState = uiState,
                    cellId = cellId,
                    onAction = onAction
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun NotebookToolbar(
    selectedEntryPath: EntryPath?,
    onExecuteAllCells: () -> Unit,
    onCancelExecution: () -> Unit,
    unsavedChanges: Boolean,
    onSave: () -> Unit,
    running: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onExecuteAllCells) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Execute All")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Run All")
            }
            Text(selectedEntryPath?.value?.lastOrNull()?.value.orEmpty().removeSuffix(".dnclnb"))

            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onCancelExecution, enabled = running) {
                Icon(Icons.Default.Close, contentDescription = "Cancel Execution")
            }
            IconButton(onClick = onSave, enabled = unsavedChanges) {
                Icon(Icons.Outlined.Save, contentDescription = "Save")
            }
        }
    }
    HorizontalDivider()
}

@Composable
fun CellComponent(
    uiState: NotebookUiState,
    cellId: String,
    onAction: (NotebookAction) -> Unit,
) {
    val cell = uiState.notebook?.cells?.find { it.id == cellId } ?: return
    val isSelected = uiState.selectedCellId == cellId
    val codeCellState = uiState.codeCellStateMap[cellId]
    val fontSize = uiState.fontSize

    val borderColor = if (isSelected)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (cell.type) {
                    CellType.CODE -> "code"
                    CellType.MARKDOWN -> "markdown"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.weight(1f))

            if (cell.type == CellType.CODE) {
                IconButton(onClick = { onAction(NotebookAction.ExecuteCell(cellId)) }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Execute Cell")
                }
            }

            IconButton(onClick = { onAction(NotebookAction.DeleteCell(cellId)) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Cell")
            }

            TextButton(onClick = {
                val newType = when (cell.type) {
                    CellType.CODE -> CellType.MARKDOWN
                    CellType.MARKDOWN -> CellType.CODE
                }
                onAction(NotebookAction.ChangeCellType(cellId, newType))
            }) {
                Text(
                    text = when (cell.type) {
                        CellType.CODE -> "To Markdown"
                        CellType.MARKDOWN -> "To Code"
                    }
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        when (cell.type) {
            CellType.CODE -> if (codeCellState != null) {
                CodeCellContent(
                    uiState = uiState,
                    cell = cell,
                    onAction = onAction,
                    codeCellState = codeCellState,
                    fontSize = fontSize,
                )
            }

            CellType.MARKDOWN -> MarkdownCellContent(
                cell = cell,
                isSelected = isSelected,
                onAction = onAction,
                fontSize = fontSize,
            )
        }
    }
}

@Composable
fun CodeCellContent(
    uiState: NotebookUiState,
    cell: Cell,
    onAction: (NotebookAction) -> Unit,
    codeCellState: CodeCellState,
    fontSize: Int,
) {
    val suggestions = uiState.cellSuggestionsMap[cell.id] ?: emptyList()

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        val editorController: CodeEditorController = rememberCodeEditorController()
        val editorState: CodeEditorState = rememberCodeEditorState(
            content = EditorContent(text = codeCellState.textFieldValue),
            annotatedText = codeCellState.annotatedString,
            evaluatingLine = null,
            verticalScrollEnabled = false,
        )

        val syncedContent = remember(codeCellState.textFieldValue) {
            EditorContent(text = codeCellState.textFieldValue)
        }
        BindCodeEditorState(
            state = editorState,
            content = syncedContent,
            annotatedText = codeCellState.annotatedString,
            highlightRevision = codeCellState.highlightRevision,
            verticalScrollEnabled = false,
        )

        val latestOnAction by rememberUpdatedState(onAction)

        LaunchedEffect(editorController) {
            editorController.events.contentChanges.collectLatest { event ->
                latestOnAction(NotebookAction.UpdateCodeCell(cell.id, event.update.content.text))
            }
        }

        LaunchedEffect(editorController) {
            editorController.events.focusChanges.collectLatest { event ->
                if (event.isFocused) {
                    latestOnAction(NotebookAction.SelectCell(cell.id))
                }
            }
        }

        val editorOptions = CodeEditorOptions(
            fontSize = fontSize,
            textStyle = LocalCodeTypography.current.bodyMedium,
            verticalScrollEnabled = false
        )

        CodeEditor(
            state = editorState,
            options = editorOptions,
            controller = editorController
        )

        if (suggestions.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxWidth()) {
                SuggestionListView(
                    textSuggestions = suggestions,
                    onConfirmTextSuggestion = { suggestion ->
                        val currentValue = editorState.textFieldValue
                        val currentText = currentValue.text
                        val cursorPos = currentValue.selection.end

                        val beforeCursorFull = currentText.substring(0, cursorPos)
                        val afterCursor = currentText.substring(cursorPos)

                        val overlapLength = run {
                            val maxLen = minOf(beforeCursorFull.length, suggestion.length)
                            var overlap = 0
                            for (len in maxLen downTo 0) {
                                if (beforeCursorFull.endsWith(suggestion.substring(0, len))) {
                                    overlap = len
                                    break
                                }
                            }
                            overlap
                        }

                        val trimmedBefore = beforeCursorFull.dropLast(overlapLength)
                        val newText = buildString {
                            append(trimmedBefore)
                            append(suggestion)
                            append(afterCursor)
                        }
                        val newCursorPos = trimmedBefore.length + suggestion.length

                        val newValue = TextFieldValue(
                            text = newText,
                            selection = TextRange(newCursorPos)
                        )
                        val update = editorState.consumeTextFieldValue(
                            value = newValue,
                            cause = EditorContentUpdate.UpdateCause.Programmatic
                        )
                        latestOnAction(
                            NotebookAction.UpdateCodeCell(
                                cell.id,
                                update.content.text
                            )
                        )
                    }
                )
            }
        }

        val density = LocalDensity.current
        var minHeight by remember(cell.id) { mutableStateOf(0.dp) }

        Column(
            modifier = Modifier.onGloballyPositioned {
                val heightInDp = with(density) { it.size.height.toDp() }
                if (heightInDp > minHeight) {
                    minHeight = heightInDp
                }
            }.heightIn(min = minHeight)
        ) {
            cell.outputs?.forEachIndexed { i, output ->
                key(i) {
                    OutputDisplay(output, fontSize)
                }
            }
        }
    }
}

@Composable
fun MarkdownCellContent(
    cell: Cell,
    isSelected: Boolean,
    onAction: (NotebookAction) -> Unit,
    fontSize: Int = 16,
) {
    var text by remember(cell.id) {
        mutableStateOf(TextFieldValue(cell.source.joinToString("\n")))
    }
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(text.text) {
        if (cell.source.joinToString("\n") != text.text) {
            onAction(NotebookAction.UpdateMarkdownCell(cell.id, text.text.lines()))
        }
    }

    LaunchedEffect(isSelected) {
        if (isSelected) {
            coroutineScope.launch {
                focusRequester.requestFocus()
            }
        }
    }

    Box(
        modifier = Modifier.clickable(enabled = !isSelected) {
            onAction(NotebookAction.SelectCell(cell.id))
        }
    ) {
        if (isSelected) {
            OutlinedTextField(
                value = text,
                onValueChange = { newValue -> text = newValue },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .focusRequester(focusRequester),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = fontSize.sp,
                    lineHeight = fontSize.sp
                ),
                singleLine = false
            )
        } else {
            Markdown(
                content = text.text,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp)
                    .padding(8.dp),
                colors = LocalMarkdownColors.current,
                typography = LocalMarkdownTypography.current
            )
        }
    }
}

@Composable
fun OutputDisplay(output: Output, fontSize: Int) {
    var minHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
            .padding(8.dp)
            .onGloballyPositioned {
                val heightInDp = with(density) { it.size.height.toDp() }
                if (heightInDp > minHeight) {
                    minHeight = heightInDp
                }
            }.heightIn(min = minHeight)
        //.heightIn(min = 500.dp)
    ) {
        when (output.outputType) {
            "stream" -> {
                // stdout/stderr を name で判定
                val isStderr = output.name == "stderr"
                val textColor = if (isStderr) MaterialTheme.colorScheme.error else Color.Unspecified

                output.text?.let { textLines ->
                    Text(
                        text = textLines.joinToString("\n"),
                        style = LocalCodeTypography.current.bodyMedium.copy(
                            fontSize = fontSize.sp,
                            lineHeight = (fontSize + 2).sp
                        ),
                        color = textColor
                    )
                }
            }

            "error" -> {
                Text(
                    text = "Error: ${output.ename ?: "Unknown error"}",
                    color = MaterialTheme.colorScheme.error,
                    style = LocalCodeTypography.current.bodyMedium.copy(
                        fontSize = fontSize.sp,
                        lineHeight = fontSize.sp
                    ),
                )

                Text(
                    text = output.evalue.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = LocalCodeTypography.current.bodyMedium.copy(
                        fontSize = fontSize.sp,
                        lineHeight = fontSize.sp
                    ),
                )
            }

            else -> {
                Text(
                    text = "Output: ${output.outputType}",
                    style = LocalCodeTypography.current.bodyMedium.copy(
                        fontSize = fontSize.sp,
                        lineHeight = fontSize.sp
                    ),
                )
            }
        }
    }
}
