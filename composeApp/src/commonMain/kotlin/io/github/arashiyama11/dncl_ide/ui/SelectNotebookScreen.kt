package io.github.arashiyama11.dncl_ide.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import io.github.arashiyama11.dncl_ide.adapter.CreatingType
import io.github.arashiyama11.dncl_ide.adapter.DrawerViewModel
import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.domain.model.Folder
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SelectNotebookScreen(
    viewModel: DrawerViewModel = koinViewModel(),
    onCreateNotebook: () -> Unit = {},
    onCreateFolder: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        viewModel.onStart(focusRequester)
    }

    Column(modifier = Modifier.padding(16.dp)) {
        // Header with title and actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Notebooks",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        // File/folder listing
        if (uiState.rootFolder != null) {
            var expandedFolders by remember { mutableStateOf(setOf<String>()) }

            // Only show notebook files
            for (entry in uiState.rootFolder!!.entities) {
                when (entry) {
                    is Folder -> FolderItemCard(
                        folder = entry,
                        depth = 0,
                        expandedFolders = expandedFolders,
                        onExpandToggle = { folderPath ->
                            expandedFolders = if (expandedFolders.contains(folderPath)) {
                                expandedFolders - folderPath
                            } else {
                                expandedFolders + folderPath
                            }
                        },
                        inputtingEntryPath = uiState.inputtingEntryPath,
                        inputtingFileName = uiState.inputtingFileName,
                        focusRequester = focusRequester,
                        creatingType = uiState.creatingType,
                        onInputtingEntryNameChanged = viewModel::onInputtingFileNameChanged,
                        onFileClick = viewModel::onFileSelected,
                        onFolderClick = viewModel::onFolderClicked,
                        isNotebookMode = true
                    )

                    else -> {
                        // Show only notebook files at root level
                        if (entry.name.isNotebookFile()) {
                            NotebookFileItemCard(
                                file = entry,
                                depth = 0,
                                onClick = viewModel::onFileSelected
                            )
                        }
                    }
                }
            }

            // Input field for creating new notebook/folder
            if (uiState.inputtingEntryPath != null &&
                uiState.inputtingEntryPath.toString() == uiState.rootFolder!!.path.toString()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (uiState.creatingType == CreatingType.FILE)
                                Icons.AutoMirrored.Outlined.InsertDriveFile
                            else
                                Icons.Outlined.Folder,
                            contentDescription = null
                        )

                        OutlinedTextField(
                            value = uiState.inputtingFileName.orEmpty(),
                            onValueChange = viewModel::onInputtingFileNameChanged,
                            modifier = Modifier
                                .focusRequester(focusRequester)
                                .padding(start = 8.dp)
                                .weight(1f),
                            placeholder = {
                                Text(if (uiState.creatingType == CreatingType.FILE) "notebook.dnclnb" else "Folder Name")
                            }
                        )
                    }
                }
            }
        } else {
            Text("Loading files...")
        }
    }
}
