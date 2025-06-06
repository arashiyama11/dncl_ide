package io.github.arashiyama11.dncl_ide.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
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
import io.github.arashiyama11.dncl_ide.domain.model.NotebookFile
import io.github.arashiyama11.dncl_ide.domain.model.ProgramFile
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SelectFileScreen(
    viewModel: DrawerViewModel = koinViewModel(),
    onCreateFile: () -> Unit = {},
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
                "Files",
                style = MaterialTheme.typography.headlineMedium
            )

            // Add file/folder buttons
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End
            ) {
                TextButton(onClick = onCreateFile) {
                    Text("New File")
                }
                TextButton(onClick = onCreateFolder) {
                    Text("New Folder")
                }
            }
        }

        // File/folder listing
        if (uiState.rootFolder != null) {
            var expandedFolders by remember { mutableStateOf(setOf<String>()) }

            // Filter out notebook files
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
                        isNotebookMode = false
                    )

                    is ProgramFile -> FileItemCard(
                        file = entry,
                        depth = 0,
                        onClick = viewModel::onFileSelected
                    )

                    else -> {} // Skip notebook files
                }
            }

            // Input field for creating new file/folder
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
                                .weight(1f)
                        )
                    }
                }
            }
        } else {
            Text("Loading files...")
        }
    }
}

@Composable
fun FileItemCard(
    file: ProgramFile,
    depth: Int,
    onClick: (EntryPath) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = (depth * 16).dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.InsertDriveFile,
                contentDescription = null
            )
            TextButton(
                onClick = { onClick(file.path) },
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = file.name.value,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun FolderItemCard(
    folder: Folder,
    depth: Int = 0,
    expandedFolders: Set<String>,
    onExpandToggle: (String) -> Unit,
    inputtingEntryPath: EntryPath?,
    inputtingFileName: String?,
    focusRequester: FocusRequester,
    creatingType: CreatingType?,
    onInputtingEntryNameChanged: (String) -> Unit,
    onFileClick: (EntryPath) -> Unit,
    onFolderClick: (Folder) -> Unit,
    isNotebookMode: Boolean
) {
    val isExpanded = expandedFolders.contains(folder.path.toString())

    Column {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = (depth * 16).dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Folder,
                    contentDescription = null
                )
                TextButton(
                    onClick = {
                        onFolderClick(folder)
                        onExpandToggle(folder.path.toString())
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = folder.name.value,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        AnimatedVisibility(isExpanded) {
            Column(
                modifier = Modifier.wrapContentHeight()
            ) {
                for (entry in folder.entities) {
                    when (entry) {
                        is Folder -> FolderItemCard(
                            folder = entry,
                            depth = depth + 1,
                            expandedFolders = expandedFolders,
                            onExpandToggle = onExpandToggle,
                            inputtingEntryPath = inputtingEntryPath,
                            inputtingFileName = inputtingFileName,
                            focusRequester = focusRequester,
                            creatingType = creatingType,
                            onInputtingEntryNameChanged = onInputtingEntryNameChanged,
                            onFileClick = onFileClick,
                            onFolderClick = onFolderClick,
                            isNotebookMode = isNotebookMode
                        )

                        is ProgramFile -> {
                            if (!isNotebookMode && !entry.name.isNotebookFile()) {
                                FileItemCard(
                                    file = entry,
                                    depth = depth + 1,
                                    onClick = onFileClick
                                )
                            }
                        }

                        else -> {
                            if (isNotebookMode && entry.name.isNotebookFile()) {
                                NotebookFileItemCard(
                                    file = entry,
                                    depth = depth + 1,
                                    onClick = onFileClick
                                )
                            }
                        }
                    }
                }

                if (inputtingEntryPath != null && inputtingEntryPath.toString() == folder.path.toString()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = ((depth + 1) * 16).dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (creatingType == CreatingType.FILE)
                                    Icons.AutoMirrored.Outlined.InsertDriveFile
                                else
                                    Icons.Outlined.Folder,
                                contentDescription = null
                            )

                            OutlinedTextField(
                                value = inputtingFileName.orEmpty(),
                                onValueChange = onInputtingEntryNameChanged,
                                modifier = Modifier
                                    .focusRequester(focusRequester)
                                    .padding(start = 8.dp)
                                    .weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotebookFileItemCard(
    file: Any, // Using Any because we're checking isNotebookFile() in the caller
    depth: Int,
    onClick: (EntryPath) -> Unit
) {
    val entryPath = when (file) {
        is NotebookFile -> file.path
        is ProgramFile -> file.path
        else -> return
    }

    val fileName = when (file) {
        is NotebookFile -> file.name.value
        is ProgramFile -> file.name.value
        else -> return
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = (depth * 16).dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.InsertDriveFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary // Different color for notebook files
            )
            TextButton(
                onClick = { onClick(entryPath) },
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}