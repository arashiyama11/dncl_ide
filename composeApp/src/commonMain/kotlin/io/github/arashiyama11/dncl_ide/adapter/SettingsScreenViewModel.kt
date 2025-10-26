package io.github.arashiyama11.dncl_ide.adapter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.arashiyama11.dncl_ide.common.AppStateStore
import io.github.arashiyama11.dncl_ide.common.StatePermission
import io.github.arashiyama11.dncl_ide.domain.model.DebugRunningMode
import io.github.arashiyama11.dncl_ide.domain.model.SuggestionPanelStyle
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
    val debugRunningMode: DebugRunningMode = DebugRunningMode.NON_BLOCKING,
    val suggestionPanelStyle: SuggestionPanelStyle = SuggestionPanelStyle.BOTTOM_STRIP
)

class SettingsScreenViewModel(
    private val settingsUseCase: SettingsUseCase,
    private val appStateStore: AppStateStore<StatePermission.Read>
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> = appStateStore.state.map { appState ->
        SettingsUiState(
            fontSize = appState.uiConfig.fontSize,
            onEvalDelay = appState.dnclConfig.onEvalDelay,
            debugModeEnabled = appState.dnclConfig.debugModeEnabled,
            debugRunningMode = appState.dnclConfig.debugRunningMode,
            list1IndexSwitchEnabled = appState.dnclConfig.arrayOriginIndex == 1,
            suggestionPanelStyle = appState.uiConfig.suggestionPanelStyle
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

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

    fun onSuggestionPanelStyleChanged(style: SuggestionPanelStyle) {
        settingsUseCase.setSuggestionPanelStyle(style)
    }
}
