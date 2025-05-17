package io.github.arashiyama11.dncl_ide.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.arashiyama11.dncl_ide.adapter.DrawerViewModel
import io.github.arashiyama11.dncl_ide.adapter.IdeViewModel
import io.github.arashiyama11.dncl_ide.ui.components.rememberDarkThemeStateFlow
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    DnclIdeTheme {
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val drawerViewModel = koinViewModel<DrawerViewModel>()
        val ideViewModel = koinViewModel<IdeViewModel>()
        val snackbarHostState = remember { SnackbarHostState() }
        val isDarkThemeStateFlow = rememberDarkThemeStateFlow()

        LaunchedEffect(Unit) {
            ideViewModel.onStart(isDarkThemeStateFlow)
            for (err in ideViewModel.errorChannel) {
                snackbarHostState.showSnackbar(err, "OK", false, SnackbarDuration.Indefinite)
            }
        }

        LaunchedEffect(Unit) {
            for (err in drawerViewModel.errorChannel) {
                snackbarHostState.showSnackbar(err, "OK", false, SnackbarDuration.Indefinite)
            }
        }


        Scaffold(snackbarHost = {
            SnackbarHost(snackbarHostState)
        }, floatingActionButton = {
            if (drawerState.isOpen || drawerState.isAnimationRunning) {
                var isShowDetails by remember { mutableStateOf(false) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedVisibility(isShowDetails) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            SmallFloatingActionButton(
                                onClick = {
                                    drawerViewModel.onFileAddClicked()
                                },
                                containerColor = Color.White
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.InsertDriveFile,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            SmallFloatingActionButton(
                                onClick = { drawerViewModel.onFolderAddClicked() },
                                containerColor = Color.White
                            ) {

                                Icon(
                                    Icons.Outlined.Folder,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    FloatingActionButton(
                        onClick = { isShowDetails = !isShowDetails },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }, floatingActionButtonPosition = FabPosition.EndOverlay) { contentPadding ->
            ModalNavigationDrawer(modifier = Modifier.fillMaxSize(), drawerContent = {
                Column(modifier = Modifier.fillMaxHeight()) {
                    DrawerContent(
                        Modifier.weight(1f, fill = true).verticalScroll(rememberScrollState()),
                        drawerViewModel
                    )
                }
            }, drawerState = drawerState) {
                DnclIDE(
                    modifier = Modifier.padding(contentPadding), ideViewModel
                )
            }
        }
    }
}

