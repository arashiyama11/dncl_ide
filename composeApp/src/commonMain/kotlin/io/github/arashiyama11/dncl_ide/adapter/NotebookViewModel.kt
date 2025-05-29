package io.github.arashiyama11.dncl_ide.adapter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.arashiyama11.dncl_ide.domain.model.NotebookFile
import io.github.arashiyama11.dncl_ide.domain.notebook.Notebook
import io.github.arashiyama11.dncl_ide.domain.usecase.FileUseCase
import io.github.arashiyama11.dncl_ide.domain.usecase.NotebookFileUseCase
import io.github.arashiyama11.dncl_ide.domain.usecase.SettingsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class NotebookUiState(
    val notebook: Notebook? = null
)

class NotebookViewModel(
    private val fileUseCase: FileUseCase,
    private val notebookFileUseCase: NotebookFileUseCase,
    private val settingsUSeCase: SettingsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NotebookUiState())
    val uiState = combine(
        _uiState,
        fileUseCase.selectedEntryPath,
        settingsUSeCase.settingsState
    ) { state, filePath, settings ->
        state.copy(
            notebook = state.notebook,
        )
    }.stateIn(
        viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
        initialValue = NotebookUiState()
    )

    fun onStart() {
        fileUseCase.selectedEntryPath.onEach {
            println("Selected entry path: $it")
            if (it?.isNotebookFile() == true) {
                val notebook = fileUseCase.getEntryByPath(it)
                if (notebook is NotebookFile) {
                    notebookFileUseCase.getNotebookFileContent(notebook)
                    _uiState.update {
                        it.copy(
                            notebook = notebookFileUseCase.getNotebookFileContent(notebook)
                        )
                    }
                } else {
                    println("Selected entry is not a NotebookFile: $notebook")
                }
            } else println("Selected entry path is not a notebook file: $it")
        }.launchIn(viewModelScope)
    }
}