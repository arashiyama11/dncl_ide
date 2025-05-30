package io.github.arashiyama11.dncl_ide.adapter

import androidx.compose.ui.focus.FocusRequester
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.arashiyama11.dncl_ide.domain.model.DebugRunningMode
import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.domain.model.FileName
import io.github.arashiyama11.dncl_ide.domain.model.Folder
import io.github.arashiyama11.dncl_ide.domain.model.FolderName
import io.github.arashiyama11.dncl_ide.domain.model.NotebookFile
import io.github.arashiyama11.dncl_ide.domain.model.ProgramFile
import io.github.arashiyama11.dncl_ide.domain.repository.SettingsRepository.Companion.DEFAULT_DEBUG_MODE
import io.github.arashiyama11.dncl_ide.domain.repository.SettingsRepository.Companion.DEFAULT_DEBUG_RUNNING_MODE
import io.github.arashiyama11.dncl_ide.domain.repository.SettingsRepository.Companion.DEFAULT_FONT_SIZE
import io.github.arashiyama11.dncl_ide.domain.repository.SettingsRepository.Companion.DEFAULT_ON_EVAL_DELAY
import io.github.arashiyama11.dncl_ide.domain.usecase.FileNameValidationUseCase
import io.github.arashiyama11.dncl_ide.domain.usecase.FileUseCase
import io.github.arashiyama11.dncl_ide.domain.usecase.NotebookFileUseCase
import io.github.arashiyama11.dncl_ide.domain.usecase.SettingsUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CreatingType {
    FILE,
    FOLDER
}

data class DrawerUiState(
    val selectedEntryPath: EntryPath? = null,
    val rootFolder: Folder? = null,
    val creatingType: CreatingType? = null,
    val inputtingEntryPath: EntryPath? = null,
    val inputtingFileName: String? = null,
    val lastClickedFolder: Folder? = null,
    val list1IndexSwitchEnabled: Boolean = false,
    val fontSize: Int = DEFAULT_FONT_SIZE,
    val onEvalDelay: Int = DEFAULT_ON_EVAL_DELAY,
    val debugModeEnabled: Boolean = DEFAULT_DEBUG_MODE,
    val debugRunningMode: DebugRunningMode = DEFAULT_DEBUG_RUNNING_MODE
)

class DrawerViewModel(
    private val fileUseCase: FileUseCase,
    private val notebookFileUseCase: NotebookFileUseCase,
    private val fileNameValidationUseCase: FileNameValidationUseCase,
    private val settingsUseCase: SettingsUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(DrawerUiState())
    val uiState = combine(
        _uiState,
        fileUseCase.selectedEntryPath,
        settingsUseCase.settingsState
    ) { state, filePath, settings ->
        state.copy(
            selectedEntryPath = filePath,
            list1IndexSwitchEnabled = settings.arrayOriginIndex == 1,
            fontSize = settings.fontSize,
            debugRunningMode = settings.debugRunningMode,
            debugModeEnabled = settings.debugMode,
            onEvalDelay = settings.onEvalDelay
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, DrawerUiState())

    private var focusRequester: FocusRequester? = null
    val errorChannel = Channel<String>(Channel.BUFFERED)

    fun onStart(focusRequester: FocusRequester) {
        viewModelScope.launch {
            _uiState.update { it.copy(rootFolder = fileUseCase.getRootFolder()) }
        }
        this.focusRequester = focusRequester
    }

    fun onDebugRunByButtonClicked() {
        when (settingsUseCase.debugRunningMode.value) {
            DebugRunningMode.BUTTON -> settingsUseCase.setDebugRunningMode(DebugRunningMode.NON_BLOCKING)
            DebugRunningMode.NON_BLOCKING -> settingsUseCase.setDebugRunningMode(DebugRunningMode.BUTTON)
        }
    }

    fun onDebugRunNonBlockingClicked() {
        when (settingsUseCase.debugRunningMode.value) {
            DebugRunningMode.BUTTON -> settingsUseCase.setDebugRunningMode(DebugRunningMode.NON_BLOCKING)
            DebugRunningMode.NON_BLOCKING -> settingsUseCase.setDebugRunningMode(DebugRunningMode.BUTTON)
        }
    }

    fun onFontSizeChanged(fontSize: Int) {
        settingsUseCase.setFontSize(fontSize)
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

    fun onFileAddClicked() {
        _uiState.update {
            it.copy(
                creatingType = CreatingType.FILE,
                inputtingEntryPath = it.lastClickedFolder?.path ?: _uiState.value.rootFolder!!.path,
                inputtingFileName = ""
            )
        }

        viewModelScope.launch {
            repeat(3) {
                try {
                    delay(100)
                    focusRequester?.requestFocus()
                    return@launch
                } catch (e: Exception) {
                    //e.printStackTrace()
                }
            }
        }
    }

    fun onFolderAddClicked() {
        _uiState.update {
            it.copy(
                creatingType = CreatingType.FOLDER,
                inputtingEntryPath = it.lastClickedFolder?.path ?: _uiState.value.rootFolder!!.path,
                inputtingFileName = ""
            )
        }


        viewModelScope.launch {
            repeat(3) {
                try {
                    delay(100)
                    focusRequester?.requestFocus()
                    return@launch
                } catch (e: Exception) {
                    //e.printStackTrace()
                }
            }
        }
    }

    fun onInputtingFileNameChanged(inputtingFileName: String) {
        if (inputtingFileName.lastOrNull() == '\n') {
            if (inputtingFileName.length > 1)
                return onFileAddConfirmed(inputtingFileName.dropLast(1))
            else _uiState.update { it.copy(inputtingFileName = inputtingFileName.dropLast(1)) }
        } else _uiState.update { it.copy(inputtingFileName = inputtingFileName) }
    }

    fun onList1IndexSwitchClicked(enabled: Boolean) {
        settingsUseCase.setListFirstIndex(if (enabled) 1 else 0)
    }

    fun onOnEvalDelayChanged(delay: Int) {
        settingsUseCase.setOnEvalDelay(delay)
    }

    fun onDebugModeChanged(enabled: Boolean) {
        settingsUseCase.setDebugMode(enabled)
    }

    private fun onFileAddConfirmed(newFileName: String) {
        viewModelScope.launch {
            val path = uiState.value.inputtingEntryPath ?: uiState.value.rootFolder!!.path
            fileNameValidationUseCase(path + FileName(newFileName)).mapLeft {
                errorChannel.send(it.message)
                return@launch
            }
            try {
                val path = _uiState.value.inputtingEntryPath ?: _uiState.value.rootFolder!!.path
                if (_uiState.value.creatingType == CreatingType.FILE) {
                    val fileName = FileName(newFileName)
                    if (fileName.isNotebookFile()) {
                        notebookFileUseCase.createNotebookFile(path, fileName)
                    } else fileUseCase.createFile(
                        path,
                        FileName(newFileName)
                    )
                    fileUseCase.selectFile(path + fileName)
                } else {
                    fileUseCase.createFolder(
                        path,
                        FolderName(newFileName)
                    )
                }
            } catch (e: Exception) {
                errorChannel.send(e.message ?: "Error")
            }
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
