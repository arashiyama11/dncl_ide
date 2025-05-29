package io.github.arashiyama11.dncl_ide.ui

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.arashiyama11.dncl_ide.adapter.NotebookViewModel
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun NotebookScreen(modifier: Modifier, notebookViewModel: NotebookViewModel = koinViewModel()) {
    val uiState by notebookViewModel.uiState.collectAsState()
    if (uiState.notebook == null) {
        CircularProgressIndicator()
        return
    }

    Text(uiState.notebook.toString())
}