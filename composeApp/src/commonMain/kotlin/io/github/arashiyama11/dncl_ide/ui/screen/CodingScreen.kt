package io.github.arashiyama11.dncl_ide.ui.screen

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.arashiyama11.dncl_ide.adapter.IdeViewModel
import io.github.arashiyama11.dncl_ide.adapter.NotebookViewModel
import io.github.arashiyama11.dncl_ide.domain.model.Entry
import io.github.arashiyama11.dncl_ide.domain.model.NotebookFile
import io.github.arashiyama11.dncl_ide.domain.model.ProgramFile
import io.github.arashiyama11.dncl_ide.domain.repository.FileRepository
import io.github.arashiyama11.dncl_ide.ui.layout.DnclIDE
import org.koin.compose.koinInject

@Composable
fun CodingScreen(
    ideViewModel: IdeViewModel,
    notebookViewModel: NotebookViewModel,
    fileRepository: FileRepository = koinInject()
) {
    val selectedEntryPath by fileRepository.selectedEntryPath.collectAsStateWithLifecycle()
    var entry by remember { mutableStateOf<Entry?>(null) }
    LaunchedEffect(selectedEntryPath) {
        entry = selectedEntryPath?.let { fileRepository.getEntryByPath(it) }
    }
    when (entry) {
        is NotebookFile -> NotebookScreen(notebookViewModel = notebookViewModel)
        is ProgramFile -> DnclIDE(viewModel = ideViewModel)
        else -> {
            Text("No file selected or unsupported file type")
        }
    }

}
