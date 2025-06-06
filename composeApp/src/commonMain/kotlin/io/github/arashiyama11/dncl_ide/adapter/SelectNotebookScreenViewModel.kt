package io.github.arashiyama11.dncl_ide.adapter

import androidx.lifecycle.viewModelScope
import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.domain.model.FileName
import io.github.arashiyama11.dncl_ide.domain.model.FolderName
import io.github.arashiyama11.dncl_ide.domain.usecase.FileNameValidationUseCase
import io.github.arashiyama11.dncl_ide.domain.usecase.FileUseCase
import io.github.arashiyama11.dncl_ide.domain.usecase.NotebookFileUseCase
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SelectNotebookScreenViewModel(
    fileUseCase: FileUseCase,
    fileNameValidationUseCase: FileNameValidationUseCase,
    private val notebookFileUseCase: NotebookFileUseCase
) : BaseSelectViewModel(fileUseCase, fileNameValidationUseCase) {

    override val uiState: StateFlow<SelectUiState> = _uiState.asStateFlow()

    override fun onFileAddClicked() {
        _uiState.update {
            it.copy(
                creatingType = CreatingType.FILE,
                inputtingEntryPath = it.lastClickedFolder?.path ?: _uiState.value.rootFolder!!.path,
                inputtingFileName = "" // 拡張子は表示しない
            )
        }

        requestFocus()
    }

    override fun onFileAddConfirmed(newFileName: String) {
        viewModelScope.launch {
            // ファイル名に.dnclnbが含まれていない場合は自動的に追加する
            val fileName = if (!newFileName.endsWith(".dnclnb")) {
                "$newFileName.dnclnb"
            } else {
                newFileName
            }

            val path = uiState.value.inputtingEntryPath ?: uiState.value.rootFolder!!.path
            val fileNameObj = FileName(fileName)

            fileNameValidationUseCase(path + fileNameObj).mapLeft {
                errorChannel.send(it.message)
                return@launch
            }

            try {
                if (_uiState.value.creatingType == CreatingType.FILE) {
                    // ノートブックファイルを作成
                    notebookFileUseCase.createNotebookFile(path, fileNameObj)
                    fileUseCase.selectFile(path + fileNameObj)
                } else {
                    fileUseCase.createFolder(path, FolderName(newFileName))
                }

                refreshState()
            } catch (e: Exception) {
                errorChannel.send(e.message ?: "Error")
            }
        }
    }

    // ユーザーが入力したファイル名を表示用に加工する（拡張子を隠す）
    override fun onInputtingFileNameChanged(inputtingFileName: String) {
        if (inputtingFileName.lastOrNull() == '\n') {
            if (inputtingFileName.length > 1)
                return onFileAddConfirmed(inputtingFileName.dropLast(1))
            else _uiState.update { it.copy(inputtingFileName = inputtingFileName.dropLast(1)) }
        } else {
            // 入力時には拡張子を自動で追加しない（表示のみ）
            _uiState.update { it.copy(inputtingFileName = inputtingFileName) }
        }
    }
}
