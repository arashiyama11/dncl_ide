package io.github.arashiyama11.dncl_ide.adapter

import androidx.lifecycle.viewModelScope
import io.github.arashiyama11.dncl_ide.common.AppStateStore
import io.github.arashiyama11.dncl_ide.domain.model.FileName
import io.github.arashiyama11.dncl_ide.domain.model.FolderName
import io.github.arashiyama11.dncl_ide.domain.usecase.FileNameValidationUseCase
import io.github.arashiyama11.dncl_ide.domain.usecase.FileUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SelectFileScreenViewModel(
    fileUseCase: FileUseCase,
    fileNameValidationUseCase: FileNameValidationUseCase,
    appStateStore: AppStateStore
) : BaseSelectViewModel(fileUseCase, fileNameValidationUseCase, appStateStore) {

    override val uiState: StateFlow<SelectUiState> = combine(
        _localState,
        appStateStore.state
    ) { localState, appState ->
        SelectUiState(
            selectedEntryPath = appState.selectedEntryPath,
            rootFolder = appState.rootFolder,
            creatingType = localState.creatingType,
            inputtingEntryPath = localState.inputtingEntryPath,
            inputtingFileName = localState.inputtingFileName,
            lastClickedFolder = localState.lastClickedFolder
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, SelectUiState())

    override fun onFileAddConfirmed(newFileName: String) {
        println("File onFileAddConfirmed: $newFileName")
        viewModelScope.launch {
            val path =
                _localState.value.inputtingEntryPath ?: appStateStore.state.value.rootFolder!!.path
            fileNameValidationUseCase(path + FileName(newFileName)).mapLeft {
                errorChannel.send(it.message)
                return@launch
            }

            try {
                if (_localState.value.creatingType == CreatingType.FILE) {
                    val fileName = FileName(newFileName)
                    // 通常のファイルのみ作成し、ノートブックファイルは作成しない
                    if (!fileName.isNotebookFile()) {
                        fileUseCase.createFile(path, fileName)
                        fileUseCase.selectFile(path + fileName)
                    } else {
                        errorChannel.send("このビューでは通常ファイルのみ作成できます。ノートブックはノートブックタブで作成してください。")
                        return@launch
                    }
                } else {
                    fileUseCase.createFolder(path, FolderName(newFileName))
                }

                refreshState()
            } catch (e: Exception) {
                errorChannel.send(e.message ?: "Error")
            }
        }
    }
}
