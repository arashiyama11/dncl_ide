package io.github.arashiyama11.dncl_interpreter.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import io.github.arashiyama11.dncl_interpreter.ui.theme.Dncl_interpreterTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    Dncl_interpreterTheme {
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val drawerViewModel = koinViewModel<DrawerViewModel>()
        val ideViewModel = koinViewModel<IdeViewModel>()
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(Unit) {
            ideViewModel.onStart()
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
            if (drawerState.isOpen)
                FloatingActionButton(onClick = { drawerViewModel.onFileAddClicked() }) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                }
        }, floatingActionButtonPosition = FabPosition.EndOverlay) { contentPadding ->
            ModalNavigationDrawer(drawerContent = {
                DrawerContent(drawerViewModel)
            }, drawerState = drawerState, modifier = Modifier.padding(contentPadding)) {
                DnclIDE(
                    modifier = Modifier, ideViewModel
                )
            }
        }
    }
}


