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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.arashiyama11.dncl_ide.adapter.CodeCellState
import io.github.arashiyama11.dncl_ide.adapter.NotebookAction
import io.github.arashiyama11.dncl_ide.adapter.NotebookViewModel
import io.github.arashiyama11.dncl_ide.domain.notebook.Cell
import io.github.arashiyama11.dncl_ide.domain.notebook.CellType
import io.github.arashiyama11.dncl_ide.domain.notebook.Notebook
import io.github.arashiyama11.dncl_ide.domain.notebook.Output
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun NotebookScreen(
    modifier: Modifier = Modifier,
    notebookViewModel: NotebookViewModel = koinViewModel()
) {
    val uiState by notebookViewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            AddCellFAB(onAddCell = { cellType ->
                notebookViewModel.handleAction(
                    NotebookAction.AddCellAfter(
                        uiState.selectedCellId,
                        cellType
                    )
                )
            })
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (uiState.notebook == null || uiState.loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                NotebookContent(
                    notebook = uiState.notebook!!,
                    selectedCellId = uiState.selectedCellId,
                    onAction = notebookViewModel::handleAction,
                    codeCellStateMap = uiState.codeCellStateMap
                )
            }
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
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Notebook toolbar
        NotebookToolbar(onExecuteAllCells = { onAction(NotebookAction.ExecuteAllCells) })

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
                    codeCellStateMap = codeCellStateMap
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun NotebookToolbar(onExecuteAllCells: () -> Unit) {
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
            // Additional toolbar items can be added here
            Spacer(modifier = Modifier.weight(1f))
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
            CellType.CODE -> CodeCellContent(cell, onAction, codeCellStateMap[cell.id]!!)
            CellType.MARKDOWN -> MarkdownCellContent(cell, onAction)
        }
    }
}

@Composable
fun CodeCellContent(
    cell: Cell,
    onAction: (NotebookAction) -> Unit,
    codeCellState: CodeCellState
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        CodeEditor(
            codeText = codeCellState.textFieldValue, codeCellState.annotatedString, Modifier, 14, {
                onAction(NotebookAction.UpdateCodeCell(cell.id, it))
            }, verticalScroll = false
        )

        cell.outputs?.forEach { output ->
            OutputDisplay(output)
        }
    }
}

@Composable
fun MarkdownCellContent(cell: Cell, onAction: (NotebookAction) -> Unit) {
    var selection by remember { mutableStateOf(TextRange(0)) }
    OutlinedTextField(
        value = TextFieldValue(cell.source.joinToString("\n"), selection = selection),
        onValueChange = { newText ->
            selection = newText.selection
            // Update the cell source when the text changes
            onAction(NotebookAction.UpdateMarkdownCell(cell.id, newText.text.lines()))
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
        singleLine = false,
        maxLines = Int.MAX_VALUE,
    )
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
                output.traceback?.forEach { line ->
                    Text(
                        text = line,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
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

