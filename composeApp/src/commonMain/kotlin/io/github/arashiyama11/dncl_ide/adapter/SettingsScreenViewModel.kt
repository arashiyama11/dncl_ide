package io.github.arashiyama11.dncl_ide.adapter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.arashiyama11.dncl_ide.domain.model.DebugRunningMode
import io.github.arashiyama11.dncl_ide.domain.repository.FileRepository
import io.github.arashiyama11.dncl_ide.domain.usecase.SettingsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class SettingsUiState(
    val list1IndexSwitchEnabled: Boolean = false,
    val fontSize: Int = 16,
    val onEvalDelay: Int = 1000,
    val debugModeEnabled: Boolean = false,
    val debugRunningMode: DebugRunningMode = DebugRunningMode.NON_BLOCKING
)

class SettingsScreenViewModel(
    private val settingsUseCase: SettingsUseCase,
    private val fileRepository: FileRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> =
        combine(_uiState, settingsUseCase.settingsState) { state, settings ->
            state.copy(
                fontSize = settings.fontSize,
                onEvalDelay = settings.onEvalDelay,
                debugModeEnabled = settings.debugMode,
                debugRunningMode = settings.debugRunningMode,
            )
        }.stateIn(viewModelScope, SharingStarted.Lazily, SettingsUiState())


    fun onList1IndexSwitchClicked(enabled: Boolean) {
        settingsUseCase.setListFirstIndex(if (enabled) 1 else 0)
        _uiState.update { it.copy(list1IndexSwitchEnabled = enabled) }
    }

    fun onFontSizeChanged(size: Int) {
        settingsUseCase.setFontSize(size)
        _uiState.update { it.copy(fontSize = size) }
    }

    fun onOnEvalDelayChanged(delay: Int) {
        settingsUseCase.setOnEvalDelay(delay)
        _uiState.update { it.copy(onEvalDelay = delay) }
    }

    fun onDebugModeChanged(enabled: Boolean) {
        settingsUseCase.setDebugMode(enabled)
        _uiState.update { it.copy(debugModeEnabled = enabled) }
    }

    fun onDebugRunByButtonClicked() {
        settingsUseCase.setDebugRunningMode(DebugRunningMode.BUTTON)
        _uiState.update { it.copy(debugRunningMode = DebugRunningMode.BUTTON) }
    }

    fun onDebugRunNonBlockingClicked() {
        settingsUseCase.setDebugRunningMode(DebugRunningMode.NON_BLOCKING)
        _uiState.update { it.copy(debugRunningMode = DebugRunningMode.NON_BLOCKING) }
    }
}