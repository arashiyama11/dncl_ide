package io.github.arashiyama11.dncl_ide.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import io.github.arashiyama11.dncl_ide.adapter.BaseSelectViewModel
import io.github.arashiyama11.dncl_ide.adapter.CreatingType
import io.github.arashiyama11.dncl_ide.adapter.SelectFileScreenViewModel
import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.domain.model.Folder
import io.github.arashiyama11.dncl_ide.domain.model.NotebookFile
import io.github.arashiyama11.dncl_ide.domain.model.ProgramFile
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SelectFileScreen(
    viewModel: SelectFileScreenViewModel = koinViewModel(),
    navigateToCodeScreen: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        viewModel.onStart(focusRequester)
    }

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        // Header with title and actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Files",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // File/folder listing
        if (uiState.rootFolder != null) {
            var expandedFolders by remember { mutableStateOf(setOf<String>()) }

            // Filter out notebook files
            for (entry in uiState.rootFolder!!.entities) {
                when (entry) {
                    is Folder -> with(viewModel) {
                        FolderItemCard(
                            folder = entry,
                            depth = 0,
                            expandedFolders = expandedFolders,
                            onExpandToggle = { folderPath, b ->
                                expandedFolders = if (b != null) {
                                    if (b) {
                                        expandedFolders + folderPath
                                    } else {
                                        expandedFolders - folderPath
                                    }
                                } else
                                    (if ((expandedFolders.contains(folderPath))) {
                                        expandedFolders - folderPath
                                    } else {
                                        expandedFolders + folderPath
                                    })
                            },
                            isNotebookMode = false,
                            onFileClicked = {
                                viewModel.onFileSelected(it)
                                navigateToCodeScreen()
                            }
                        )
                    }

                    is ProgramFile -> {
                        if (!entry.name.isNotebookFile()) {
                            FileItemCard(
                                file = entry,
                                depth = 0,
                                onClick = {
                                    viewModel.onFileSelected(it)
                                    navigateToCodeScreen()
                                }
                            )
                        }
                    }

                    is NotebookFile -> {}
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
            .clickable {
                onClick(file.path)
            }
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
            Spacer(Modifier.width(16.dp))
            Text(
                text = file.name.value,
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

context(viewModel: BaseSelectViewModel)
@Composable
fun FolderItemCard(
    folder: Folder,
    depth: Int = 0,
    expandedFolders: Set<String>,
    onExpandToggle: (String, Boolean?) -> Unit,
    isNotebookMode: Boolean,
    onFileClicked: (EntryPath) -> Unit = {}
) {
    val isExpanded = expandedFolders.contains(folder.path.toString())
    val uiState = viewModel.uiState.collectAsState().value
    val inputtingEntryPath = uiState.inputtingEntryPath
    val inputtingFileName = uiState.inputtingFileName
    val focusRequester = remember { FocusRequester() }
    val creatingType = uiState.creatingType

    Column {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = (depth * 16).dp)
                .clickable {
                    viewModel.onFolderClicked(folder)
                    onExpandToggle(folder.path.toString(), null)
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    Icons.Outlined.ChevronRight,
                    null,
                    modifier = Modifier.rotate(if (isExpanded) 90f else 0f)
                )

                Spacer(Modifier.width(8.dp))

                Icon(
                    Icons.Outlined.Folder,
                    contentDescription = null
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = folder.name.value,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                )

                Row(
                    modifier = Modifier.weight(1f, fill = true),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.clickable {
                        viewModel.onFolderClicked(folder)
                        viewModel.onFileAddClicked(folder.path)
                        onExpandToggle(folder.path.toString(), true)
                    }) {
                        Icon(Icons.AutoMirrored.Outlined.InsertDriveFile, null)
                        Icon(Icons.Outlined.Add, null, modifier = Modifier.size(14.dp))
                    }

                    Spacer(Modifier.width(8.dp))
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.clickable {
                        viewModel.onFolderClicked(folder)
                        viewModel.onFolderAddClicked(folder.path)
                        onExpandToggle(folder.path.toString(), true)
                    }) {
                        Icon(Icons.Outlined.Folder, null)
                        Icon(Icons.Outlined.Add, null, modifier = Modifier.size(14.dp))
                    }
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
                            isNotebookMode = isNotebookMode
                        )

                        is ProgramFile -> {
                            if (!isNotebookMode && !entry.name.isNotebookFile()) {
                                FileItemCard(
                                    file = entry,
                                    depth = depth + 1,
                                    onClick = onFileClicked
                                )
                            }
                        }

                        else -> {
                            if (isNotebookMode && entry.name.isNotebookFile()) {
                                NotebookFileItemCard(
                                    file = entry,
                                    depth = depth + 1,
                                    onClick = onFileClicked
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
                                onValueChange = viewModel::onInputtingFileNameChanged,
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
            .clickable { onClick(entryPath) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.List,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = fileName.removeSuffix(".dnclnb"),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}