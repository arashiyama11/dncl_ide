package io.github.arashiyama11.dncl_ide.adapter

import androidx.lifecycle.ViewModel
import io.github.arashiyama11.dncl_ide.domain.model.DebugRunningMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsUiState(
    val list1IndexSwitchEnabled: Boolean = false,
    val fontSize: Int = 16,
    val onEvalDelay: Int = 1000,
    val debugModeEnabled: Boolean = false,
    val debugRunningMode: DebugRunningMode = DebugRunningMode.NON_BLOCKING
)

class SettingsScreenViewModel() : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()
}