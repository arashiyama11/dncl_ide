package io.github.arashiyama11.dncl_interpreter.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp

@Composable
fun DrawerContent(drawerViewModel: DrawerViewModel) {
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
            icon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
            onClick = { isShowFiles = !isShowFiles },
        )
        AnimatedVisibility(isShowFiles && uiState.files.isNotEmpty()) {
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .wrapContentHeight()
            ) {
                VerticalDivider(Modifier.height(56.dp * uiState.files.size))
                Column() {
                    for ((i, fileName) in uiState.files.withIndex()) {
                        if (i == uiState.files.lastIndex && uiState.isFileCreating) {
                            println("add")
                            NavigationDrawerItem(
                                label = {
                                    OutlinedTextField(
                                        value = uiState.inputtingFileName.orEmpty(),
                                        onValueChange = {
                                            drawerViewModel.onInputtingFileNameChanged(
                                                it
                                            )
                                        },
                                        modifier = Modifier
                                            .focusRequester(focusRequester)
                                    )
                                },
                                selected = false,
                                icon = {
                                    Icon(
                                        Icons.AutoMirrored.Outlined.InsertDriveFile,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    focusRequester.requestFocus()
                                }
                            )
                        } else
                            NavigationDrawerItem(
                                label = { Text(text = fileName.value) },
                                selected = false,
                                icon = {
                                    Icon(
                                        Icons.AutoMirrored.Outlined.InsertDriveFile,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    drawerViewModel.onFileSelected(i)
                                }
                            )
                    }
                }
            }
        }
    }
}