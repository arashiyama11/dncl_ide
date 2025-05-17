package io.github.arashiyama11.dncl_ide.repository

import com.russhwolf.settings.Settings
import io.github.arashiyama11.dncl_ide.domain.model.DebugRunningMode
import io.github.arashiyama11.dncl_ide.domain.repository.SettingsRepository
import io.github.arashiyama11.dncl_ide.domain.repository.SettingsRepository.Companion.DEFAULT_ARRAY_ORIGIN_INDEX
import io.github.arashiyama11.dncl_ide.domain.repository.SettingsRepository.Companion.DEFAULT_DEBUG_MODE
import io.github.arashiyama11.dncl_ide.domain.repository.SettingsRepository.Companion.DEFAULT_DEBUG_RUNNING_MODE
import io.github.arashiyama11.dncl_ide.domain.repository.SettingsRepository.Companion.DEFAULT_FONT_SIZE
import io.github.arashiyama11.dncl_ide.domain.repository.SettingsRepository.Companion.DEFAULT_ON_EVAL_DELAY
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsRepositoryImpl() : SettingsRepository {
    private val _arrayOriginIndex = MutableStateFlow(DEFAULT_ARRAY_ORIGIN_INDEX)
    private val _fontSize = MutableStateFlow(DEFAULT_FONT_SIZE)
    private val _onEvalDelay = MutableStateFlow(DEFAULT_ON_EVAL_DELAY)
    private val _debugMode = MutableStateFlow(DEFAULT_DEBUG_MODE)
    private val _debugRunningMode = MutableStateFlow(DEFAULT_DEBUG_RUNNING_MODE)

    override val arrayOriginIndex: StateFlow<Int> = _arrayOriginIndex
    override val fontSize: StateFlow<Int> = _fontSize
    override val onEvalDelay: StateFlow<Int> = _onEvalDelay
    override val debugMode: StateFlow<Boolean> = _debugMode
    override val debugRunningMode: StateFlow<DebugRunningMode> = _debugRunningMode
    private val setting = Settings()

    init {
        _arrayOriginIndex.value =
            setting.getInt(ARRAY_ORIGIN_INDEX, DEFAULT_ARRAY_ORIGIN_INDEX)
        _fontSize.value = setting.getInt(FONT_SIZE, DEFAULT_FONT_SIZE)
        _onEvalDelay.value =
            setting.getInt(ON_EVAL_DELAY, DEFAULT_ON_EVAL_DELAY)
        _debugMode.value =
            setting.getBoolean(DEBUG_MODE, DEFAULT_DEBUG_MODE)
        _debugRunningMode.value = try {
            DebugRunningMode.valueOf(
                setting.getString(
                    DEBUG_RUNNING_MODE,
                    DEFAULT_DEBUG_RUNNING_MODE.name
                )
            )
        } catch (e: IllegalArgumentException) {
            DEFAULT_DEBUG_RUNNING_MODE
        }
    }

    override fun setListFirstIndex(index: Int) {
        setting.putInt(ARRAY_ORIGIN_INDEX, index)
        _arrayOriginIndex.value = index
    }

    override fun setFontSize(size: Int) {
        setting.putInt(FONT_SIZE, size)
        _fontSize.value = size
    }

    override fun setOnEvalDelay(delay: Int) {
        setting.putInt(ON_EVAL_DELAY, delay)
        _onEvalDelay.value = delay
    }

    override fun setDebugMode(enabled: Boolean) {
        setting.putBoolean(DEBUG_MODE, enabled)
        _debugMode.value = enabled
    }

    override fun setDebugRunningMode(mode: DebugRunningMode) {
        setting.putString(DEBUG_RUNNING_MODE, mode.name)
        _debugRunningMode.value = mode
    }

    companion object {
        const val ARRAY_ORIGIN_INDEX = "arrayOriginIndex"
        const val FONT_SIZE = "fontSize"
        const val ON_EVAL_DELAY = "onEvalDelay"
        const val DEBUG_MODE = "debugMode"
        const val DEBUG_RUNNING_MODE = "debugRunningMode"
    }
}
