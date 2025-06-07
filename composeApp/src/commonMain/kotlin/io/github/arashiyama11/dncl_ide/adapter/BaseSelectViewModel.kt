package io.github.arashiyama11.dncl_ide.adapter

import androidx.compose.ui.focus.FocusRequester
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.arashiyama11.dncl_ide.common.AppStateStore
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
    protected val fileNameValidationUseCase: FileNameValidationUseCase,
    protected val appStateStore: AppStateStore
) : ViewModel() {
    protected val _localState = MutableStateFlow(
        SelectLocalState(
            creatingType = null,
            inputtingEntryPath = null,
            inputtingFileName = null,
            lastClickedFolder = null
        )
    )

    abstract val uiState: StateFlow<SelectUiState>

    protected var focusRequester: FocusRequester? = null
    val errorChannel = Channel<String>(Channel.BUFFERED)

    fun onStart(focusRequester: FocusRequester) {
        this.focusRequester = focusRequester
    }

    fun onFileSelected(path: EntryPath) {
        viewModelScope.launch {
            fileUseCase.selectFile(path)
        }
    }

    fun onFolderClicked(folder: Folder?) {
        viewModelScope.launch {
            _localState.update { it.copy(lastClickedFolder = folder) }
        }
    }

    open fun onFileAddClicked() {
        val currentState = appStateStore.state.value
        _localState.update {
            it.copy(
                creatingType = CreatingType.FILE,
                inputtingEntryPath = it.lastClickedFolder?.path ?: currentState.rootFolder!!.path,
                inputtingFileName = ""
            )
        }

        requestFocus()
    }

    fun onFolderAddClicked() {
        val currentState = appStateStore.state.value
        _localState.update {
            it.copy(
                creatingType = CreatingType.FOLDER,
                inputtingEntryPath = it.lastClickedFolder?.path ?: currentState.rootFolder!!.path,
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

    protected fun refreshState() {
        _localState.update {
            it.copy(
                creatingType = null,
                inputtingEntryPath = null,
                inputtingFileName = null
            )
        }
    }

    open fun onInputtingFileNameChanged(inputtingFileName: String) {
        println("onInputtingFileNameChanged: $inputtingFileName")
        if (inputtingFileName.lastOrNull() == '\n') {
            if (inputtingFileName.length > 1)
                return onFileAddConfirmed(inputtingFileName.dropLast(1))
            else _localState.update { it.copy(inputtingFileName = inputtingFileName.dropLast(1)) }
        } else {
            _localState.update { it.copy(inputtingFileName = inputtingFileName) }
        }
    }

    open fun onFileAddConfirmed(newFileName: String) {
        viewModelScope.launch {
            val path =
                _localState.value.inputtingEntryPath ?: appStateStore.state.value.rootFolder!!.path

            if (_localState.value.creatingType == CreatingType.FILE) {
                fileNameValidationUseCase(path + FileName(newFileName)).mapLeft {
                    errorChannel.send(it.message)
                    return@launch
                }
                fileUseCase.createFile(path, FileName(newFileName))
            } else {
                fileUseCase.createFolder(path, FolderName(newFileName))
            }

            refreshState()
        }
    }

    protected data class SelectLocalState(
        val creatingType: CreatingType? = null,
        val inputtingEntryPath: EntryPath? = null,
        val inputtingFileName: String? = null,
        val lastClickedFolder: Folder? = null
    )
}
