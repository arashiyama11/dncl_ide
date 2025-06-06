package io.github.arashiyama11.dncl_ide.adapter

import androidx.lifecycle.viewModelScope
import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.domain.model.FileName
import io.github.arashiyama11.dncl_ide.domain.model.FolderName
import io.github.arashiyama11.dncl_ide.domain.usecase.FileNameValidationUseCase
import io.github.arashiyama11.dncl_ide.domain.usecase.FileUseCase
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SelectFileScreenViewModel(
    fileUseCase: FileUseCase,
    fileNameValidationUseCase: FileNameValidationUseCase
) : BaseSelectViewModel(fileUseCase, fileNameValidationUseCase) {

    override val uiState: StateFlow<SelectUiState> = _uiState.asStateFlow()

    override fun onFileAddConfirmed(newFileName: String) {
        viewModelScope.launch {
            val path = uiState.value.inputtingEntryPath ?: uiState.value.rootFolder!!.path
            fileNameValidationUseCase(path + FileName(newFileName)).mapLeft {
                errorChannel.send(it.message)
                return@launch
            }

            try {
                if (_uiState.value.creatingType == CreatingType.FILE) {
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
