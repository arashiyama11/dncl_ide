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
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.arashiyama11.dncl_interpreter.ui.theme.Dncl_interpreterTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    Dncl_interpreterTheme {
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val drawerViewModel = koinViewModel<DrawerViewModel>()
        Scaffold(floatingActionButton = {
            if (drawerState.isOpen)
                FloatingActionButton(onClick = { drawerViewModel.onFileAddClicked() }) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                }
        }, floatingActionButtonPosition = FabPosition.EndOverlay) { contentPadding ->
            ModalNavigationDrawer(drawerContent = {
                DrawerContent(drawerViewModel)
            }, drawerState = drawerState, modifier = Modifier.padding(contentPadding)) {
                DnclIDE(
                    modifier = Modifier
                )
            }
        }
    }
}


