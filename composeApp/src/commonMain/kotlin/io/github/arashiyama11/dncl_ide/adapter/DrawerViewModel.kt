package io.github.arashiyama11.dncl_ide.adapter

import androidx.compose.ui.focus.FocusRequester
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.arashiyama11.dncl_ide.common.AppStateStore
import io.github.arashiyama11.dncl_ide.domain.model.DebugRunningMode
import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.domain.model.FileName
import io.github.arashiyama11.dncl_ide.domain.model.Folder
import io.github.arashiyama11.dncl_ide.domain.model.FolderName
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
    private val settingsUseCase: SettingsUseCase,
    private val appStateStore: AppStateStore
) : ViewModel() {
    private val _localState = MutableStateFlow(
        DrawerLocalState(
            creatingType = null,
            inputtingEntryPath = null,
            inputtingFileName = null,
            lastClickedFolder = null,
            list1IndexSwitchEnabled = false
        )
    )

    val uiState = combine(
        _localState,
        appStateStore.state
    ) { localState, appState ->
        DrawerUiState(
            selectedEntryPath = appState.selectedEntryPath,
            rootFolder = appState.rootFolder,
            creatingType = localState.creatingType,
            inputtingEntryPath = localState.inputtingEntryPath,
            inputtingFileName = localState.inputtingFileName,
            lastClickedFolder = localState.lastClickedFolder,
            list1IndexSwitchEnabled = localState.list1IndexSwitchEnabled,
            fontSize = appState.fontSize,
            onEvalDelay = appState.onEvalDelay,
            debugModeEnabled = appState.debugModeEnabled,
            debugRunningMode = appState.debugRunningMode
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, DrawerUiState())

    private var focusRequester: FocusRequester? = null
    val errorChannel = Channel<String>(Channel.BUFFERED)

    fun onStart(focusRequester: FocusRequester) {
        this.focusRequester = focusRequester
    }

    fun onDebugRunByButtonClicked() {
        when (uiState.value.debugRunningMode) {
            DebugRunningMode.BUTTON -> settingsUseCase.setDebugRunningMode(DebugRunningMode.NON_BLOCKING)
            DebugRunningMode.NON_BLOCKING -> settingsUseCase.setDebugRunningMode(DebugRunningMode.BUTTON)
        }
    }

    fun onDebugRunNonBlockingClicked() {
        when (uiState.value.debugRunningMode) {
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
            _localState.update { it.copy(lastClickedFolder = folder) }
        }
    }

    fun onFileAddClicked() {
        _localState.update {
            it.copy(
                creatingType = CreatingType.FILE,
                inputtingEntryPath = it.lastClickedFolder?.path
                    ?: appStateStore.state.value.rootFolder!!.path,
                inputtingFileName = ""
            )
        }

        requestFocus()
    }

    fun onFolderAddClicked() {
        _localState.update {
            it.copy(
                creatingType = CreatingType.FOLDER,
                inputtingEntryPath = it.lastClickedFolder?.path
                    ?: appStateStore.state.value.rootFolder!!.path,
                inputtingFileName = ""
            )
        }

        requestFocus()
    }

    private fun requestFocus() {
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

    fun onFileAddConfirmed(newFileName: String) {
        viewModelScope.launch {
            val path =
                _localState.value.inputtingEntryPath ?: appStateStore.state.value.rootFolder!!.path
            fileNameValidationUseCase(path + FileName(newFileName)).mapLeft {
                errorChannel.send(it.message)
                return@launch
            }

            try {
                if (_localState.value.creatingType == CreatingType.FILE) {
                    val fileNameObj = FileName(newFileName)
                    if (fileNameObj.isNotebookFile()) {
                        // ノートブックファイルの場合はノートブックファイルとして作成
                        notebookFileUseCase.createNotebookFile(path, fileNameObj)
                    } else {
                        fileUseCase.createFile(path, fileNameObj)
                    }
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

    private fun refreshState() {
        _localState.update {
            it.copy(
                creatingType = null,
                inputtingEntryPath = null,
                inputtingFileName = null
            )
        }
    }

    fun onInputtingFileNameChanged(inputtingFileName: String) {
        if (inputtingFileName.lastOrNull() == '\n') {
            if (inputtingFileName.length > 1)
                return onFileAddConfirmed(inputtingFileName.dropLast(1))
            else _localState.update { it.copy(inputtingFileName = inputtingFileName.dropLast(1)) }
        } else {
            _localState.update { it.copy(inputtingFileName = inputtingFileName) }
        }
    }

    fun onList1IndexSwitchClicked(enabled: Boolean) {
        settingsUseCase.setListFirstIndex(if (enabled) 1 else 0)
        _localState.update { it.copy(list1IndexSwitchEnabled = enabled) }
    }

    fun onOnEvalDelayChanged(delay: Int) {
        settingsUseCase.setOnEvalDelay(delay)
    }

    fun onDebugModeChanged(enabled: Boolean) {
        settingsUseCase.setDebugMode(enabled)
    }

    private data class DrawerLocalState(
        val creatingType: CreatingType?,
        val inputtingEntryPath: EntryPath?,
        val inputtingFileName: String?,
        val lastClickedFolder: Folder?,
        val list1IndexSwitchEnabled: Boolean
    )
}
