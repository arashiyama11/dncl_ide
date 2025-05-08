package io.github.arashiyama11.dncl_ide.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.DrawerValue
import androidx.compose.material.FabPosition
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalDrawer
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dncl_ide.composeapp.generated.resources.Res
import dncl_ide.composeapp.generated.resources.file_outlined
import dncl_ide.composeapp.generated.resources.folder_outlined
import io.github.arashiyama11.dncl_ide.adapter.DrawerViewModel
import io.github.arashiyama11.dncl_ide.adapter.IdeViewModel
import io.github.arashiyama11.dncl_ide.ui.components.Fab
import io.github.arashiyama11.dncl_ide.ui.components.SmallFab
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    DnclIdeTheme {
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val drawerViewModel = koinViewModel<DrawerViewModel>()
        val ideViewModel = koinViewModel<IdeViewModel>()
        val snackbarHostState = remember { SnackbarHostState() }
        val isDarkTheme = isSystemInDarkTheme()

        LaunchedEffect(Unit) {
            ideViewModel.onStart(isDarkTheme)
            for (err in ideViewModel.errorChannel) {
                snackbarHostState.showSnackbar(err, "OK", SnackbarDuration.Indefinite)
            }
        }

        LaunchedEffect(Unit) {
            for (err in drawerViewModel.errorChannel) {
                snackbarHostState.showSnackbar(err, "OK", SnackbarDuration.Indefinite)
            }
        }

        Scaffold(snackbarHost = {
            SnackbarHost(snackbarHostState)
        }, floatingActionButton = {
            if (drawerState.isOpen) {
                var isShowDetails by remember { mutableStateOf(false) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedVisibility(isShowDetails) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            SmallFab(onClick = { drawerViewModel.onFileAddClicked() }) {
                                Icon(
                                    painterResource(Res.drawable.file_outlined),
                                    contentDescription = null,
                                    tint = MaterialTheme.colors.primary
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            SmallFab(onClick = { drawerViewModel.onFolderAddClicked() }) {
                                Icon(
                                    painterResource(Res.drawable.folder_outlined),
                                    null,
                                    tint = MaterialTheme.colors.primary
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Fab(onClick = { isShowDetails = !isShowDetails }) {
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colors.onPrimary
                        )
                    }
                }
            }
        }, floatingActionButtonPosition = FabPosition.End) { contentPadding ->
            ModalDrawer(drawerContent = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    DrawerContent(drawerViewModel)
                }
            }, drawerState = drawerState) {
                DnclIDE(
                    modifier = Modifier, ideViewModel
                )
            }
        }
    }
}


