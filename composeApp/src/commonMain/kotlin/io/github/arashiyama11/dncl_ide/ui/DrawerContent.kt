package io.github.arashiyama11.dncl_ide.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.arashiyama11.dncl_ide.adapter.CreatingType
import io.github.arashiyama11.dncl_ide.adapter.DrawerViewModel
import io.github.arashiyama11.dncl_ide.domain.model.DebugRunningMode
import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.domain.model.Folder
import io.github.arashiyama11.dncl_ide.domain.model.ProgramFile
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DrawerContent(
    modifier: Modifier = Modifier,
    navigateToLicenses: () -> Unit = {},
    drawerViewModel: DrawerViewModel = koinViewModel()
) {
    val uiState by drawerViewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        drawerViewModel.onStart(focusRequester)
    }
    ModalDrawerSheet(modifier = modifier.fillMaxHeight()) {

        var isShowFiles by remember { mutableStateOf(true) }

        NavigationDrawerItem(
            label = { Text(text = "Files") },
            selected = false,

            icon = {
                Icon(
                    Icons.Outlined.Folder,
                    contentDescription = null
                )
            },
            onClick = {
                isShowFiles = !isShowFiles
                drawerViewModel.onFolderClicked(null)
            },
        )
        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .wrapContentHeight()
        ) {
            AnimatedVisibility(isShowFiles && uiState.rootFolder != null) {
                Column {
                    for (entry in uiState.rootFolder!!.entities) {
                        when (entry) {
                            is Folder -> FolderItem(
                                entry,
                                0,
                                uiState.inputtingEntryPath, uiState.inputtingFileName,
                                focusRequester,
                                uiState.creatingType,
                                drawerViewModel::onInputtingFileNameChanged,
                                drawerViewModel::onFileSelected,
                                drawerViewModel::onFolderClicked
                            )

                            is ProgramFile -> FileItem(
                                entry,
                                0,
                                drawerViewModel::onFileSelected
                            )
                        }
                    }
                    if (uiState.inputtingEntryPath != null && uiState.inputtingEntryPath.toString() == uiState.rootFolder!!.path.toString()) {
                        NavigationDrawerItem(
                            label = {
                                OutlinedTextField(
                                    value = uiState.inputtingFileName.orEmpty(),
                                    onValueChange = {
                                        drawerViewModel.onInputtingFileNameChanged(it)
                                    },
                                    modifier = Modifier
                                        .focusRequester(focusRequester)
                                        .padding(vertical = 8.dp)
                                )
                            },
                            selected = false,
                            icon = {
                                Icon(
                                    if (uiState.creatingType == CreatingType.FILE) Icons.AutoMirrored.Outlined.InsertDriveFile else Icons.Outlined.Folder,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                focusRequester.requestFocus()
                            }
                        )
                    }
                }
            }
        }

        var isShowSettings by remember { mutableStateOf(true) }

        NavigationDrawerItem(label = {
            Text(text = "Settings")
        }, selected = false, icon = {
            Icon(Icons.Outlined.Settings, contentDescription = null)
        }, onClick = {
            isShowSettings = !isShowSettings
        })

        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .wrapContentHeight()
        ) {
            AnimatedVisibility(isShowSettings) {
                Column {
                    NavigationDrawerItem(label = {
                        Text(
                            text = "配列の最初の要素の添字を1にする",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }, selected = false, onClick = {}, badge = {
                        Switch(
                            checked = uiState.list1IndexSwitchEnabled,
                            onCheckedChange = {
                                drawerViewModel.onList1IndexSwitchClicked(it)
                            },
                        )
                    })

                    var fontSizeText by remember { mutableStateOf(uiState.fontSize.toString()) }

                    NavigationDrawerItem(
                        label = {
                            Text(
                                "フォントサイズ",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }, selected = false, onClick = {}, badge = {
                            OutlinedTextField(
                                value = fontSizeText,
                                onValueChange = {
                                    fontSizeText = it
                                    it.toIntOrNull()?.let { drawerViewModel.onFontSizeChanged(it) }
                                },
                                modifier = Modifier
                                    .width(80.dp)
                                    .padding(vertical = 8.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                            )
                        }
                    )

                    var onEvalDelayText by remember { mutableStateOf(uiState.onEvalDelay.toString()) }

                    NavigationDrawerItem(label = {
                        Text(
                            text = "デバッグモード（実行中の行を表示）",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }, selected = false, onClick = {}, badge = {
                        Switch(
                            checked = uiState.debugModeEnabled,
                            onCheckedChange = {
                                drawerViewModel.onDebugModeChanged(it)
                            }
                        )
                    })

                    AnimatedVisibility(
                        uiState.debugModeEnabled,
                        modifier = Modifier.padding(start = 16.dp)
                    ) {
                        Column {


                            NavigationDrawerItem(
                                selected = false,
                                onClick = {},
                                label = {
                                    Text("ボタンを押して実行")
                                },
                                badge = {
                                    Switch(
                                        checked = uiState.debugRunningMode == DebugRunningMode.BUTTON,
                                        onCheckedChange = {
                                            drawerViewModel.onDebugRunByButtonClicked()
                                        },
                                    )
                                }
                            )

                            NavigationDrawerItem(
                                selected = false,
                                onClick = {},
                                label = {
                                    Text("自動実行")
                                }, badge = {
                                    Switch(
                                        checked = uiState.debugRunningMode == DebugRunningMode.NON_BLOCKING,
                                        onCheckedChange = {
                                            drawerViewModel.onDebugRunNonBlockingClicked()
                                        },
                                    )
                                }
                            )

                            AnimatedVisibility(uiState.debugRunningMode == DebugRunningMode.NON_BLOCKING) {

                                NavigationDrawerItem(

                                    label = {
                                        Text(
                                            "ゆっくり実行",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }, selected = false, onClick = {}, badge = {
                                        OutlinedTextField(
                                            value = onEvalDelayText,
                                            onValueChange = {
                                                onEvalDelayText = it
                                                it.toIntOrNull()
                                                    ?.let { drawerViewModel.onOnEvalDelayChanged(it) }
                                            },
                                            modifier = Modifier
                                                .width(80.dp)
                                                .padding(vertical = 8.dp),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                        )
                                    }
                                )
                            }
                        }
                    }

                    NavigationDrawerItem(
                        label = {
                            Text(
                                text = "ライセンス表示",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }, selected = false, onClick = navigateToLicenses
                    )
                }
            }
        }
    }
}

@Composable
fun FileItem(
    file: ProgramFile,
    depth: Int,
    onClick: (ProgramFile) -> Unit
) {
    NavigationDrawerItem(
        label = { Text(text = file.name.value) },
        selected = false,
        icon = {
            Icon(
                Icons.AutoMirrored.Outlined.InsertDriveFile,
                contentDescription = null
            )
        },
        onClick = {
            onClick(file)
        }, modifier = Modifier.padding(start = 16.dp * depth)
    )
}

@Composable
fun FolderItem(
    folder: Folder, depth: Int = 0,
    inputtingEntryPath: EntryPath?,
    inputtingFileName: String?,
    focusRequester: FocusRequester,
    creatingType: CreatingType?,
    onInputtingEntryNameChanged: (String) -> Unit,
    onFileClick: (ProgramFile) -> Unit,
    onFolderClick: (Folder) -> Unit
) {
    var isShowFiles by remember { mutableStateOf(true) }
    Column {
        Row(
            modifier = Modifier.padding(start = 16.dp * depth),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavigationDrawerItem(
                label = { Text(text = folder.name.value) },
                selected = false,
                icon = {
                    Icon(
                        Icons.Outlined.Folder,
                        contentDescription = null
                    )
                },
                onClick = {
                    onFolderClick(folder)
                    isShowFiles = !isShowFiles || folder.entities.isEmpty()
                },
            )
        }

        AnimatedVisibility(isShowFiles) {
            Column {
                for (entry in folder.entities) {
                    when (entry) {
                        is Folder -> FolderItem(
                            entry,
                            depth + 1,
                            inputtingEntryPath,
                            inputtingFileName,
                            focusRequester,
                            creatingType,
                            onInputtingEntryNameChanged,
                            onFileClick, onFolderClick
                        )

                        is ProgramFile -> FileItem(entry, depth + 1, onFileClick)
                    }
                }
                if (inputtingEntryPath != null && inputtingEntryPath.toString() == folder.path.toString()) {
                    NavigationDrawerItem(
                        label = {
                            OutlinedTextField(
                                value = inputtingFileName.orEmpty(),
                                onValueChange = {
                                    onInputtingEntryNameChanged(it)
                                },
                                modifier = Modifier.focusRequester(focusRequester)
                            )
                        },
                        selected = false,
                        icon = {
                            Icon(
                                if (creatingType == CreatingType.FILE) Icons.AutoMirrored.Outlined.InsertDriveFile else Icons.Outlined.Folder,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            focusRequester.requestFocus()
                        }, modifier = Modifier.padding(start = 16.dp * (depth + 1))
                    )
                }
            }
        }
    }
}
