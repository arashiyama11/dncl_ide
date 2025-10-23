package io.github.arashiyama11.dncl_ide.common

import io.github.arashiyama11.dncl_ide.domain.model.DebugRunningMode
import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.domain.model.Folder
import io.github.arashiyama11.dncl_ide.domain.model.SuggestionPanelStyle
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
    val uiConfig: UiConfig = UiConfig(),
    val dnclConfig: DnclConfig = DnclConfig(),
    val selectedEntryPath: EntryPath? = null,
    val rootFolder: Folder? = null,
    val running: Boolean = false,
) {
    data class UiConfig(
        val fontSize: Int = 16,
        val suggestionPanelStyle: SuggestionPanelStyle = SuggestionPanelStyle.BOTTOM_STRIP,
    )

    data class DnclConfig(
        val onEvalDelay: Int = 0,
        val debugModeEnabled: Boolean = false,
        val debugRunningMode: DebugRunningMode = DebugRunningMode.NON_BLOCKING,
        val arrayOriginIndex: Int = 0,
    )
}

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
    value class SetSuggestionPanelStyle(val style: SuggestionPanelStyle) : Action

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
            combine(
                settingsRepository.fontSize,
                settingsRepository.suggestionPanelStyle
            ) { fontSize, suggestionPanelStyle ->
                AppState.UiConfig(
                    fontSize = fontSize,
                    suggestionPanelStyle = suggestionPanelStyle
                )
            },
            combine(
                settingsRepository.onEvalDelay,
                settingsRepository.debugRunningMode,
                settingsRepository.debugMode,
                settingsRepository.arrayOriginIndex
            ) { onEvalDelay, debugRunningMode, debugModeEnabled, arrayOriginIndex ->
                AppState.DnclConfig(
                    onEvalDelay = onEvalDelay,
                    debugModeEnabled = debugModeEnabled,
                    debugRunningMode = debugRunningMode,
                    arrayOriginIndex = arrayOriginIndex
                )
            }
        ) { uiConfig, dnclConfig ->
            _state.update {
                it.copy(
                    uiConfig = uiConfig,
                    dnclConfig = dnclConfig
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
                    _state.update { it.copy(uiConfig = it.uiConfig.copy(fontSize = action.fontSize)) }
                }

                is Action.SetOnEvalDelay -> {
                    _state.update {
                        it.copy(dnclConfig = it.dnclConfig.copy(onEvalDelay = action.onEvalDelay))
                    }
                }

                is Action.SetDebugMode -> {
                    _state.update {
                        it.copy(
                            dnclConfig = it.dnclConfig.copy(debugModeEnabled = action.enabled)
                        )
                    }
                }

                is Action.SetDebugRunningMode -> {
                    _state.update {
                        it.copy(
                            dnclConfig = it.dnclConfig.copy(debugRunningMode = action.mode)
                        )
                    }
                }

                is Action.SetSuggestionPanelStyle -> {
                    _state.update {
                        it.copy(
                            uiConfig = it.uiConfig.copy(suggestionPanelStyle = action.style)
                        )
                    }
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
