package io.github.arashiyama11.dncl_ide.adapter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.arashiyama11.dncl_ide.common.AppStateStore
import io.github.arashiyama11.dncl_ide.domain.model.DebugRunningMode
import io.github.arashiyama11.dncl_ide.domain.usecase.SettingsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class SettingsUiState(
    val list1IndexSwitchEnabled: Boolean = false,
    val fontSize: Int = 16,
    val onEvalDelay: Int = 1000,
    val debugModeEnabled: Boolean = false,
    val debugRunningMode: DebugRunningMode = DebugRunningMode.NON_BLOCKING
)

class SettingsScreenViewModel(
    private val settingsUseCase: SettingsUseCase,
    private val appStateStore: AppStateStore
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> = appStateStore.state.map { appState ->
        SettingsUiState(
            fontSize = appState.fontSize,
            onEvalDelay = appState.onEvalDelay,
            debugModeEnabled = appState.debugModeEnabled,
            debugRunningMode = appState.debugRunningMode,
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, SettingsUiState())

    fun onList1IndexSwitchClicked(enabled: Boolean) {
        settingsUseCase.setListFirstIndex(if (enabled) 1 else 0)
    }

    fun onFontSizeChanged(size: Int) {
        settingsUseCase.setFontSize(size)
    }

    fun onOnEvalDelayChanged(delay: Int) {
        settingsUseCase.setOnEvalDelay(delay)
    }

    fun onDebugModeChanged(enabled: Boolean) {
        settingsUseCase.setDebugMode(enabled)
    }

    fun onDebugRunByButtonClicked() {
        settingsUseCase.setDebugRunningMode(DebugRunningMode.BUTTON)
    }

    fun onDebugRunNonBlockingClicked() {
        settingsUseCase.setDebugRunningMode(DebugRunningMode.NON_BLOCKING)
    }
}