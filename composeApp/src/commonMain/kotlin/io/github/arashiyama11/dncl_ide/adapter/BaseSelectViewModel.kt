package io.github.arashiyama11.dncl_ide.adapter

import androidx.compose.ui.focus.FocusRequester
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.domain.model.FileName
import io.github.arashiyama11.dncl_ide.domain.model.Folder
import io.github.arashiyama11.dncl_ide.domain.model.FolderName
import io.github.arashiyama11.dncl_ide.domain.usecase.FileNameValidationUseCase
import io.github.arashiyama11.dncl_ide.domain.usecase.FileUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CreatingType {
    FILE,
    FOLDER
}

data class SelectUiState(
    val selectedEntryPath: EntryPath? = null,
    val rootFolder: Folder? = null,
    val creatingType: CreatingType? = null,
    val inputtingEntryPath: EntryPath? = null,
    val inputtingFileName: String? = null,
    val lastClickedFolder: Folder? = null
)

abstract class BaseSelectViewModel(
    protected val fileUseCase: FileUseCase,
    protected val fileNameValidationUseCase: FileNameValidationUseCase
) : ViewModel() {
    protected val _uiState = MutableStateFlow(SelectUiState())
    abstract val uiState: StateFlow<SelectUiState>

    protected var focusRequester: FocusRequester? = null
    val errorChannel = Channel<String>(Channel.BUFFERED)

    fun onStart(focusRequester: FocusRequester) {
        viewModelScope.launch {
            _uiState.update { it.copy(rootFolder = fileUseCase.getRootFolder()) }
        }
        this.focusRequester = focusRequester
    }

    fun onFileSelected(path: EntryPath) {
        viewModelScope.launch {
            fileUseCase.selectFile(path)
        }
    }

    fun onFolderClicked(folder: Folder?) {
        viewModelScope.launch {
            _uiState.update { it.copy(lastClickedFolder = folder) }
        }
    }

    open fun onFileAddClicked() {
        _uiState.update {
            it.copy(
                creatingType = CreatingType.FILE,
                inputtingEntryPath = it.lastClickedFolder?.path ?: _uiState.value.rootFolder!!.path,
                inputtingFileName = ""
            )
        }

        requestFocus()
    }

    fun onFolderAddClicked() {
        _uiState.update {
            it.copy(
                creatingType = CreatingType.FOLDER,
                inputtingEntryPath = it.lastClickedFolder?.path ?: _uiState.value.rootFolder!!.path,
                inputtingFileName = ""
            )
        }

        requestFocus()
    }

    protected fun requestFocus() {
        viewModelScope.launch {
            repeat(3) {
                try {
                    delay(100)
                    focusRequester?.requestFocus()
                    return@launch
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    open fun onInputtingFileNameChanged(inputtingFileName: String) {
        if (inputtingFileName.lastOrNull() == '\n') {
            if (inputtingFileName.length > 1)
                return onFileAddConfirmed(inputtingFileName.dropLast(1))
            else _uiState.update { it.copy(inputtingFileName = inputtingFileName.dropLast(1)) }
        } else _uiState.update { it.copy(inputtingFileName = inputtingFileName) }
    }

    abstract fun onFileAddConfirmed(newFileName: String)

    protected fun createFolder(path: EntryPath, folderName: FolderName) {
        viewModelScope.launch {
            try {
                fileUseCase.createFolder(path, folderName)
                _uiState.update {
                    it.copy(
                        creatingType = null,
                        inputtingEntryPath = null,
                        inputtingFileName = null,
                        rootFolder = fileUseCase.getRootFolder(),
                    )
                }
            } catch (e: Exception) {
                errorChannel.send(e.message ?: "Error creating folder")
            }
        }
    }

    protected fun refreshState() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    creatingType = null,
                    inputtingEntryPath = null,
                    inputtingFileName = null,
                    rootFolder = fileUseCase.getRootFolder(),
                )
            }
        }
    }
}
