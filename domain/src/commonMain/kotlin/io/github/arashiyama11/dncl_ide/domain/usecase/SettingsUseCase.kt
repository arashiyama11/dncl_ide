package io.github.arashiyama11.dncl_ide.domain.usecase

import io.github.arashiyama11.dncl_ide.domain.model.DebugRunningMode
import io.github.arashiyama11.dncl_ide.domain.model.SettingsState
import io.github.arashiyama11.dncl_ide.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class SettingsUseCase(private val settingsRepository: SettingsRepository) {
    val arrayOriginIndex = settingsRepository.arrayOriginIndex
    val fontSize = settingsRepository.fontSize
    val onEvalDelay = settingsRepository.onEvalDelay
    val debugMode = settingsRepository.debugMode
    val debugRunningMode = settingsRepository.debugRunningMode
    val settingsState = combine(
        arrayOriginIndex,
        fontSize,
        onEvalDelay,
        debugMode,
        debugRunningMode
    ) { i, s, e, d, r ->
        SettingsState(i, s, e, d, r)
    }

    fun setListFirstIndex(index: Int) {
        settingsRepository.setListFirstIndex(index)
    }

    fun setFontSize(size: Int) {
        settingsRepository.setFontSize(size)
    }

    fun setOnEvalDelay(delay: Int) {
        settingsRepository.setOnEvalDelay(delay)
    }

    fun setDebugMode(enabled: Boolean) {
        settingsRepository.setDebugMode(enabled)
    }

    fun setDebugRunningMode(mode: DebugRunningMode) {
        settingsRepository.setDebugRunningMode(mode)
    }
}
