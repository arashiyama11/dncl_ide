package io.github.arashiyama11.dncl_ide.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.arashiyama11.dncl_ide.domain.model.Entry
import io.github.arashiyama11.dncl_ide.domain.model.NotebookFile
import io.github.arashiyama11.dncl_ide.domain.model.ProgramFile
import io.github.arashiyama11.dncl_ide.domain.repository.FileRepository
import org.koin.compose.koinInject

@Composable
fun CodingScreen(fileRepository: FileRepository = koinInject()) {
    val selectedEntryPath by fileRepository.selectedEntryPath.collectAsStateWithLifecycle()
    var entry by remember(selectedEntryPath) { mutableStateOf<Entry?>(null) }
    LaunchedEffect(selectedEntryPath) {
        entry = if (selectedEntryPath != null) {
            fileRepository.getEntryByPath(selectedEntryPath!!)
        } else {
            null
        }
    }
    when (entry) {
        is NotebookFile -> NotebookScreen()
        is ProgramFile -> DnclIDE()
        else -> {
            Text("No file selected or unsupported file type")
        }
    }

}