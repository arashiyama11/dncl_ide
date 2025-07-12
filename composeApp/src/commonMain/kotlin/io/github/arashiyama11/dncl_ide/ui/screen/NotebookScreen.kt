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
import io.github.arashiyama11.dncl_ide.adapter.NotebookViewModel
import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.domain.notebook.Cell
import io.github.arashiyama11.dncl_ide.domain.notebook.CellType
import io.github.arashiyama11.dncl_ide.domain.notebook.Output
import io.github.arashiyama11.dncl_ide.ui.LocalCodeTypography
import io.github.arashiyama11.dncl_ide.ui.components.CodeEditor
import io.github.arashiyama11.dncl_ide.ui.components.SuggestionListView
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun NotebookScreen(
    modifier: Modifier = Modifier,
    notebookViewModel: NotebookViewModel = koinViewModel(),
) {
    val uiState by notebookViewModel.uiState.collectAsStateWithLifecycle()
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        if (uiState.notebook == null || uiState.loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            NotebookContent(
                notebookViewModel = notebookViewModel,
            )
        }
    }
}

@Composable
fun NotebookContent(
    notebookViewModel: NotebookViewModel,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        NotebookToolbarComponent(notebookViewModel)
        CellListComponent(notebookViewModel, Modifier.weight(1f))
    }
}

@Composable
fun NotebookToolbarComponent(viewModel: NotebookViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NotebookToolbar(
        selectedEntryPath = uiState.selectedEntryPath,
        onExecuteAllCells = remember { { viewModel.handleAction(NotebookAction.ExecuteAllCells) } },
        onCancelExecution = remember { { viewModel.handleAction(NotebookAction.CancelExecution) } },
        unsavedChanges = uiState.unsavedChanges,
        onSave = viewModel::saveNotebook,
        running = uiState.running
    )
}

@Composable
fun CellListComponent(viewModel: NotebookViewModel, modifier: Modifier = Modifier) {
    val cellIds by viewModel.cellIdsFlow.collectAsStateWithLifecycle()

    //LazyColumnだとUX悪い
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        for (cellId in cellIds) {
            key(cellId) {
                CellComponent(
                    cellId = cellId,
                    viewModel = viewModel
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
    cellId: String,
    viewModel: NotebookViewModel,
) {
    val cell by viewModel.cellStateFlow(cellId).collectAsStateWithLifecycle()
    if (cell?.id == "cell-2") {
        println("Cell ID 2 update detected. ${cell.hashCode()}")
    }
    cell?.let { cellModel ->
        val isSelected by viewModel.isSelectedFlow(cellId).collectAsStateWithLifecycle()
        val codeCellState by viewModel.codeCellStateFlow(cellId).collectAsStateWithLifecycle()
        val fontSize by viewModel.fontSizeFlow.collectAsStateWithLifecycle()
        val onAction = viewModel::handleAction

        val borderColor = if (isSelected)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.outlineVariant

        var minHeight by remember(cellId) { mutableStateOf(100.dp) }
        val density = LocalDensity.current

        Column(
            modifier = Modifier
                .fillMaxWidth()//.animateContentSize()
                .heightIn(min = minHeight)
                .onGloballyPositioned { layoutCoordinates ->
                    val heightInDp = with(density) { layoutCoordinates.size.height.toDp() }
                    if (heightInDp > minHeight) {
                        minHeight = heightInDp
                    }
                }
                .clip(RoundedCornerShape(4.dp))
                .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (cellModel.type) {
                        CellType.CODE -> "code"
                        CellType.MARKDOWN -> "markdown"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.weight(1f))

                if (cellModel.type == CellType.CODE) {
                    IconButton(onClick = { onAction(NotebookAction.ExecuteCell(cellId)) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Execute Cell")
                    }
                }

                IconButton(onClick = { onAction(NotebookAction.DeleteCell(cellId)) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Cell")
                }

                TextButton(onClick = {
                    val newType = when (cellModel.type) {
                        CellType.CODE -> CellType.MARKDOWN
                        CellType.MARKDOWN -> CellType.CODE
                    }
                    onAction(NotebookAction.ChangeCellType(cellId, newType))
                }) {
                    Text(
                        text = when (cellModel.type) {
                            CellType.CODE -> "To Markdown"
                            CellType.MARKDOWN -> "To Code"
                        }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            when (cellModel.type) {
                CellType.CODE -> CodeCellContent(
                    cellModel,
                    onAction,
                    codeCellState,
                    fontSize,
                    viewModel
                )

                CellType.MARKDOWN -> MarkdownCellContent(
                    cellModel,
                    isSelected,
                    onAction,
                    fontSize,
                )
            }
        }
    }
}

@Composable
fun CodeCellContent(
    cell: Cell,
    onAction: (NotebookAction) -> Unit,
    codeCellState: CodeCellState,
    fontSize: Int,
    viewModel: NotebookViewModel
) {
    val suggestions by viewModel.suggestionsFlow(cell.id).collectAsStateWithLifecycle()

    var localTfv by remember(cell.id, codeCellState.textFieldValue) {
        mutableStateOf(codeCellState.textFieldValue)
    }

    LaunchedEffect(localTfv) {
        if (codeCellState.textFieldValue != localTfv) {
            onAction(NotebookAction.UpdateCodeCell(cell.id, localTfv))
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        CodeEditor(
            codeText = localTfv,
            codeCellState.annotatedString,
            Modifier,
            fontSize,
            { newTextFieldValue ->
                localTfv = newTextFieldValue
            },
            verticalScroll = false,
            onFocused = {
                onAction(NotebookAction.SelectCell(cell.id))
            }
        )

        if (suggestions.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxWidth()) {
                SuggestionListView(
                    textSuggestions = suggestions,
                    onConfirmTextSuggestion = { suggestion ->
                        val currentText = localTfv.text
                        val cursorPos = localTfv.selection.end

                        var startPos = cursorPos
                        while (startPos > 0) {
                            val char = currentText.getOrNull(startPos - 1)
                            if (char != null && (char.isLetterOrDigit() || char == '_')) {
                                startPos--
                            } else {
                                break
                            }
                        }

                        val beforeCursor = currentText.substring(0, startPos)
                        val afterCursor = currentText.substring(cursorPos)
                        val newText = beforeCursor + suggestion + afterCursor
                        val newCursorPos = startPos + suggestion.length

                        localTfv = TextFieldValue(
                            text = newText,
                            selection = TextRange(newCursorPos)
                        )
                    }
                )
            }
        }

        cell.outputs?.forEach { output ->
            OutputDisplay(output, fontSize)
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
            .padding(8.dp)
    ) {
        when (output.outputType) {
            "stream" -> {
                output.text?.let { textLines ->
                    Text(
                        text = textLines.joinToString("\n"),
                        style = LocalCodeTypography.current.bodyMedium.copy(
                            fontSize = fontSize.sp,
                            lineHeight = (fontSize + 2).sp
                        ),
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
