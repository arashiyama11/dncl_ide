package io.github.arashiyama11.dncl_ide.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.model.MarkdownColors
import com.mikepenz.markdown.model.MarkdownTypography
import io.github.arashiyama11.dncl_ide.adapter.CodeCellState
import io.github.arashiyama11.dncl_ide.adapter.NotebookAction
import io.github.arashiyama11.dncl_ide.adapter.NotebookViewModel
import io.github.arashiyama11.dncl_ide.domain.model.Definition
import io.github.arashiyama11.dncl_ide.domain.notebook.Cell
import io.github.arashiyama11.dncl_ide.domain.notebook.CellType
import io.github.arashiyama11.dncl_ide.domain.notebook.Notebook
import io.github.arashiyama11.dncl_ide.domain.notebook.Output
import io.github.arashiyama11.dncl_ide.ui.components.SuggestionListView
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun NotebookScreen(
    modifier: Modifier = Modifier,
    notebookViewModel: NotebookViewModel = koinViewModel(),
) {
    val uiState by notebookViewModel.uiState.collectAsStateWithLifecycle()
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        if (uiState.notebook == null || uiState.loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            NotebookContent(
                notebook = uiState.notebook!!,
                selectedCellId = uiState.selectedCellId,
                onAction = notebookViewModel::handleAction,
                codeCellStateMap = uiState.codeCellStateMap,
                cellSuggestionsMap = uiState.cellSuggestionsMap
            )
        }
    }
}

@Composable
fun AddCellFAB(onAddCell: (CellType) -> Unit) {
    var showDropdown by remember { mutableStateOf(false) }

    Box {
        FloatingActionButton(
            onClick = { showDropdown = true }
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Cell")
        }

        DropdownMenu(
            expanded = showDropdown,
            onDismissRequest = { showDropdown = false }
        ) {
            DropdownMenuItem(
                text = { Text("Add Code Cell") },
                onClick = {
                    onAddCell(CellType.CODE)
                    showDropdown = false
                }
            )
            DropdownMenuItem(
                text = { Text("Add Markdown Cell") },
                onClick = {
                    onAddCell(CellType.MARKDOWN)
                    showDropdown = false
                }
            )
        }
    }
}

@Composable
fun NotebookContent(
    notebook: Notebook,
    selectedCellId: String?,
    onAction: (NotebookAction) -> Unit,
    codeCellStateMap: Map<String, CodeCellState>,
    cellSuggestionsMap: Map<String, List<Definition>>
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Notebook toolbar with cancel capability
        NotebookToolbar(
            onExecuteAllCells = { onAction(NotebookAction.ExecuteAllCells) },
            onCancelExecution = { onAction(NotebookAction.CancelExecution) }
        )

        // Cells
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            items(notebook.cells) { cell ->
                CellComponent(
                    cell = cell,
                    isSelected = cell.id == selectedCellId,
                    onAction = onAction,
                    codeCellStateMap = codeCellStateMap,
                    suggestions = cellSuggestionsMap[cell.id] ?: emptyList()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                with(LocalDensity.current) {
                    Spacer(Modifier.height(LocalWindowInfo.current.containerSize.height.toDp() / 3))
                }
            }
        }
    }
}

@Composable
fun NotebookToolbar(
    onExecuteAllCells: () -> Unit,
    onCancelExecution: () -> Unit
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
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onCancelExecution) {
                Icon(Icons.Default.Close, contentDescription = "Cancel Execution")
            }
        }
    }
    HorizontalDivider()
}

@Composable
fun CellComponent(
    cell: Cell,
    isSelected: Boolean,
    onAction: (NotebookAction) -> Unit,
    codeCellStateMap: Map<String, CodeCellState>,
    suggestions: List<Definition> = emptyList()
) {
    val borderColor = if (isSelected)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .clickable { onAction(NotebookAction.SelectCell(cell.id)) }
            .padding(8.dp)
    ) {
        // Cell header with type indicator and controls
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

            // Cell controls
            if (cell.type == CellType.CODE) {
                IconButton(onClick = { onAction(NotebookAction.ExecuteCell(cell.id)) }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Execute Cell")
                }
            }

            IconButton(onClick = { onAction(NotebookAction.DeleteCell(cell.id)) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Cell")
            }

            // Toggle cell type button
            TextButton(onClick = {
                val newType = when (cell.type) {
                    CellType.CODE -> CellType.MARKDOWN
                    CellType.MARKDOWN -> CellType.CODE
                }
                onAction(NotebookAction.ChangeCellType(cell.id, newType))
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

        // Cell content
        when (cell.type) {
            CellType.CODE -> CodeCellContent(
                cell,
                onAction,
                codeCellStateMap[cell.id] ?: CodeCellState(),
                suggestions
            )

            CellType.MARKDOWN -> MarkdownCellContent(cell, isSelected, onAction)
        }
    }
}

@Composable
fun CodeCellContent(
    cell: Cell,
    onAction: (NotebookAction) -> Unit,
    codeCellState: CodeCellState,
    suggestions: List<Definition> = emptyList()
) {
    var localTfv by remember(cell.id) {
        mutableStateOf(codeCellState.textFieldValue)
    }

    LaunchedEffect(codeCellState.textFieldValue, cell.id) {
        if (localTfv != codeCellState.textFieldValue) {
            localTfv = codeCellState.textFieldValue
        }
    }

    LaunchedEffect(localTfv, cell.id) {
        if (codeCellState.textFieldValue != localTfv) {
            onAction(NotebookAction.UpdateCodeCell(cell.id, localTfv))
        }
    }

    Column(modifier = Modifier.fillMaxWidth().clickable {
        onAction(NotebookAction.SelectCell(cell.id))
    }) {
        CodeEditor(
            codeText = localTfv,
            codeCellState.annotatedString,
            Modifier.clickable {
                onAction(NotebookAction.SelectCell(cell.id))
            },
            14,
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
            OutputDisplay(output)
        }
    }
}

@Composable
fun MarkdownCellContent(cell: Cell, isSelected: Boolean, onAction: (NotebookAction) -> Unit) {
    var text by remember(cell.id) {
        mutableStateOf(TextFieldValue(cell.source.joinToString("\n")))
    }

    LaunchedEffect(text.text) {
        onAction(NotebookAction.UpdateMarkdownCell(cell.id, text.text.lines()))
    }

    Box(
        modifier = Modifier.clickable { onAction(NotebookAction.SelectCell(cell.id)) }
    ) {
        if (isSelected) {
            OutlinedTextField(
                value = text,
                onValueChange = { newValue -> text = newValue },   // ← ここで保持している Value を更新するだけ
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                singleLine = false,
                maxLines = Int.MAX_VALUE
            )
        } else {
            Markdown(
                content = cell.source.joinToString("\n"),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = rememberMarkdownColors(),
                typography = rememberMarkdownTypography()
            )
        }
    }


}

@Composable
fun OutputDisplay(output: Output) {
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
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            "error" -> {
                Text(
                    text = "Error: ${output.ename ?: "Unknown error"}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = output.evalue.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            else -> {
                Text(
                    text = "Output: ${output.outputType}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}


@Composable
fun rememberMarkdownColors(): MarkdownColors {
    return object : MarkdownColors {
        override val text: Color = MaterialTheme.colorScheme.onBackground
        override val codeText: Color = MaterialTheme.colorScheme.onSurface
        override val inlineCodeText: Color = MaterialTheme.colorScheme.onSurfaceVariant
        override val linkText: Color = MaterialTheme.colorScheme.primary
        override val codeBackground: Color = MaterialTheme.colorScheme.surfaceVariant
        override val inlineCodeBackground: Color = MaterialTheme.colorScheme.surfaceVariant
        override val dividerColor: Color = MaterialTheme.colorScheme.outline
        override val tableText: Color = MaterialTheme.colorScheme.onSurface
        override val tableBackground: Color = MaterialTheme.colorScheme.surface
    }
}

@Composable
fun rememberMarkdownTypography(): MarkdownTypography {
    return object : MarkdownTypography {
        override val text: TextStyle = MaterialTheme.typography.bodyLarge
        override val code: TextStyle =
            MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
        override val inlineCode: TextStyle =
            MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
        override val h1: TextStyle = MaterialTheme.typography.headlineLarge
        override val h2: TextStyle = MaterialTheme.typography.headlineMedium
        override val h3: TextStyle = MaterialTheme.typography.headlineSmall
        override val h4: TextStyle = MaterialTheme.typography.titleLarge
        override val h5: TextStyle = MaterialTheme.typography.titleMedium
        override val h6: TextStyle = MaterialTheme.typography.titleSmall
        override val quote: TextStyle =
            MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.secondary)
        override val paragraph: TextStyle = MaterialTheme.typography.bodyLarge
        override val ordered: TextStyle = MaterialTheme.typography.bodyLarge
        override val bullet: TextStyle = MaterialTheme.typography.bodyLarge
        override val list: TextStyle = MaterialTheme.typography.bodyLarge
        override val link: TextStyle =
            MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.primary)
        override val textLink: TextLinkStyles = TextLinkStyles()
        override val table: TextStyle = MaterialTheme.typography.bodyMedium
    }
}
