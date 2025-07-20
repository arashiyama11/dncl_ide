package io.github.arashiyama11.dncl_ide.common

import io.github.arashiyama11.dncl_ide.domain.model.DebugRunningMode
import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.domain.model.Folder
import io.github.arashiyama11.dncl_ide.domain.repository.FileRepository
import io.github.arashiyama11.dncl_ide.domain.repository.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.jvm.JvmInline
import kotlin.time.Duration.Companion.seconds

// 統合された状態を表すデータクラス
data class AppState(
    // 設定関連の状態
    val fontSize: Int = 16,
    val onEvalDelay: Int = 0,
    val debugModeEnabled: Boolean = false,
    val debugRunningMode: DebugRunningMode = DebugRunningMode.NON_BLOCKING,
    val selectedEntryPath: EntryPath? = null,
    val arrayOriginIndex: Int = 0,
    val rootFolder: Folder? = null,
    val running: Boolean = false,
)

sealed interface Action {
    @JvmInline
    value class SetRunning(val running: Boolean) : Action

    @JvmInline
    value class SetFontSize(val fontSize: Int) : Action

    @JvmInline
    value class SetOnEvalDelay(val onEvalDelay: Int) : Action

    @JvmInline
    value class SetDebugMode(val enabled: Boolean) : Action

    @JvmInline
    value class SetDebugRunningMode(val mode: DebugRunningMode) : Action

    @JvmInline
    value class SetSelectedEntryPath(val entryPath: EntryPath?) : Action

    @JvmInline
    value class SetRootFolder(val folder: Folder?) : Action
}

sealed interface StatePermission {
    sealed interface Read : StatePermission
    sealed interface Write : Read
}

// DON'T DELETE GENERICS
open class AppStateStore<out T : StatePermission>(
    fileRepository: FileRepository,
    settingsRepository: SettingsRepository,
    appScope: AppScope
) : AutoCloseable {
    operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>) = state.value

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()
    private val actionBuffer = Channel<Action>(capacity = 1024)

    private val jobs: MutableList<Job> = mutableListOf()

    init {
        combine(
            settingsRepository.fontSize,
            settingsRepository.onEvalDelay,
            settingsRepository.debugRunningMode,
            settingsRepository.debugMode,
            settingsRepository.arrayOriginIndex
        ) { f, e, d, dm, i ->
            _state.update {
                it.copy(
                    fontSize = f,
                    onEvalDelay = e,
                    debugRunningMode = d,
                    debugModeEnabled = dm,
                    arrayOriginIndex = i
                )
            }
        }.launchIn(appScope)


        combine(
            fileRepository.selectedEntryPath,
            fileRepository.rootFolder
        ) { selectedPath, rootFolder ->
            _state.value.copy(
                selectedEntryPath = selectedPath,
                rootFolder = rootFolder
            )
        }.onEach { newState ->
            _state.value = newState
        }.launchIn(appScope)

        appScope.launch {
            _state.update {
                it.copy(
                    rootFolder = fileRepository.getRootFolder()
                )
            }
        }


        appScope.launch {
            startProcessAction()
        }

        appScope.launch {
            delay(5.seconds)
            println(_state.value)
        }
    }


    override fun close() {
        jobs.forEach { it.cancel() }
        jobs.clear()
    }


    private suspend fun startProcessAction() {
        for (action in actionBuffer) {
            when (action) {
                is Action.SetRunning -> {
                    _state.update { it.copy(running = action.running) }
                }

                is Action.SetFontSize -> {
                    _state.update { it.copy(fontSize = action.fontSize) }
                }

                is Action.SetOnEvalDelay -> {
                    _state.update { it.copy(onEvalDelay = action.onEvalDelay) }
                }

                is Action.SetDebugMode -> {
                    _state.update { it.copy(debugModeEnabled = action.enabled) }
                }

                is Action.SetDebugRunningMode -> {
                    _state.update { it.copy(debugRunningMode = action.mode) }
                }

                is Action.SetSelectedEntryPath -> {
                    _state.update { it.copy(selectedEntryPath = action.entryPath) }
                }

                is Action.SetRootFolder -> {
                    _state.update { it.copy(rootFolder = action.folder) }
                }
            }
        }
    }

    companion object {
        fun AppStateStore<StatePermission.Write>.dispatch(action: Action) {
            actionBuffer.trySend(action)
        }
    }
}
