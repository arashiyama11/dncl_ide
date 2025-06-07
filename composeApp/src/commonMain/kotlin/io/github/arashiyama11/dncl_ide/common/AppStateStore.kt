package io.github.arashiyama11.dncl_ide.common

import io.github.arashiyama11.dncl_ide.domain.model.DebugRunningMode
import io.github.arashiyama11.dncl_ide.domain.model.Definition
import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.domain.model.FileName
import io.github.arashiyama11.dncl_ide.domain.model.Folder
import io.github.arashiyama11.dncl_ide.domain.model.ProgramFile
import io.github.arashiyama11.dncl_ide.domain.notebook.Cell
import io.github.arashiyama11.dncl_ide.domain.notebook.Notebook
import io.github.arashiyama11.dncl_ide.domain.repository.FileRepository
import io.github.arashiyama11.dncl_ide.domain.usecase.SettingsUseCase
import io.github.arashiyama11.dncl_ide.adapter.CodeCellState
import io.github.arashiyama11.dncl_ide.adapter.CreatingType
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 統合された状態を表すデータクラス
data class AppState(
    // 設定関連の状態
    val fontSize: Int = 16,
    val onEvalDelay: Int = 0,
    val debugModeEnabled: Boolean = false,
    val debugRunningMode: DebugRunningMode = DebugRunningMode.NON_BLOCKING,
    val selectedEntryPath: EntryPath? = null,
    val rootFolder: Folder? = null,
    val running: Boolean = false,
)

class AppStateStore(
    private val settingsUseCase: SettingsUseCase,
    private val fileRepository: FileRepository,
    private val appScope: AppScope
) : AutoCloseable {
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()
    private val jobs: MutableList<Job> = mutableListOf()

    init {
        // 設定状態を監視して更新
        jobs += settingsUseCase.settingsState.onEach { settings ->
            _state.update { currentState ->
                currentState.copy(
                    fontSize = settings.fontSize,
                    onEvalDelay = settings.onEvalDelay,
                    debugModeEnabled = settings.debugMode,
                    debugRunningMode = settings.debugRunningMode
                )
            }
        }.launchIn(appScope)

        // 選択されたファイルパスを監視して更新
        jobs += fileRepository.selectedEntryPath.onEach { path ->
            _state.update { it.copy(selectedEntryPath = path) }
        }.launchIn(appScope)

        jobs += fileRepository.rootFolder.onEach { folder ->
            _state.update { it.copy(rootFolder = folder) }
        }.launchIn(appScope)

        // 初期データ読み込み
        jobs += appScope.launch {
            val rootFolder = fileRepository.getRootFolder()
            _state.update { it.copy(rootFolder = rootFolder) }
        }
    }

    override fun close() {
        jobs.forEach { it.cancel() }
        jobs.clear()
    }
}