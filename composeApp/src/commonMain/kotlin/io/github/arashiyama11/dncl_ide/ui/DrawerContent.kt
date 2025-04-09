package io.github.arashiyama11.dncl_ide.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import dncl_ide.composeapp.generated.resources.Res
import dncl_ide.composeapp.generated.resources.file_outlined
import dncl_ide.composeapp.generated.resources.folder_outlined
import io.github.arashiyama11.dncl_ide.adapter.CreatingType
import io.github.arashiyama11.dncl_ide.adapter.DrawerViewModel
import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.domain.model.Folder
import io.github.arashiyama11.dncl_ide.domain.model.ProgramFile
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DrawerContent(drawerViewModel: DrawerViewModel = koinViewModel()) {
    val uiState by drawerViewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        drawerViewModel.onStart(focusRequester)
    }
    ModalDrawerSheet {

        var isShowFiles by remember { mutableStateOf(true) }

        NavigationDrawerItem(
            label = { Text(text = "Files") },
            selected = false,

            icon = {
                Icon(
                    painterResource(Res.drawable.folder_outlined),
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
                                    painterResource(if (uiState.creatingType == CreatingType.FILE) Res.drawable.file_outlined else Res.drawable.folder_outlined),
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

        var isShowSettings by remember { mutableStateOf(false) }

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
                            style = MaterialTheme.typography.body2
                        )
                    }, selected = false, onClick = null, badge = {
                        Switch(
                            checked = uiState.list1IndexSwitchEnabled,
                            onCheckedChange = {
                                drawerViewModel.onList1IndexSwitchClicked(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colors.primary,
                                checkedTrackColor = MaterialTheme.colors.primary.copy(alpha = 0.5f),
                                uncheckedThumbColor = MaterialTheme.colors.secondary,
                                uncheckedTrackColor = MaterialTheme.colors.secondary.copy(alpha = 0.5f)
                            )
                        )
                    })
                }
            }
        }
    }
}

@Composable
fun NavigationDrawerItem(
    label: @Composable (() -> Unit) = {},
    selected: Boolean = false,
    icon: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null, modifier: Modifier = Modifier,
    badge: @Composable (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier.size(width = 360.dp, height = 66.dp)
            .clip(RoundedCornerShape(28.dp)).let {
                if (onClick != null) it.clickable(
                    interactionSource = interactionSource,
                    indication = ripple(),
                    onClick = onClick
                ).indication(interactionSource, indication = ripple()) else it
            }
            .padding(end = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            if (icon != null)
                Box(modifier = Modifier.size(24.dp)) {
                    icon()
                }
            Spacer(Modifier.size(12.dp))
            label()
        }
        badge?.invoke()
    }
}

@Composable
fun ModalDrawerSheet(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .width(360.dp)
            .padding(16.dp)
            .wrapContentHeight()
    ) {
        content()
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
                painterResource(Res.drawable.file_outlined),
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
                        painterResource(Res.drawable.folder_outlined),
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
                                painterResource(if (creatingType == CreatingType.FILE) Res.drawable.file_outlined else Res.drawable.folder_outlined),
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
