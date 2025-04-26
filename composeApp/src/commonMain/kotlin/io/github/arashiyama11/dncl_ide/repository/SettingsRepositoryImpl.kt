package io.github.arashiyama11.dncl_ide.repository

import com.russhwolf.settings.Settings
import io.github.arashiyama11.dncl_ide.domain.repository.SettingsRepository
import io.github.arashiyama11.dncl_ide.domain.repository.SettingsRepository.Companion.DEFAULT_ARRAY_ORIGIN_INDEX
import io.github.arashiyama11.dncl_ide.domain.repository.SettingsRepository.Companion.DEFAULT_DEBUG_MODE
import io.github.arashiyama11.dncl_ide.domain.repository.SettingsRepository.Companion.DEFAULT_FONT_SIZE
import io.github.arashiyama11.dncl_ide.domain.repository.SettingsRepository.Companion.DEFAULT_ON_EVAL_DELAY
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsRepositoryImpl() : SettingsRepository {
    override val arrayOriginIndex: StateFlow<Int> = MutableStateFlow(DEFAULT_ARRAY_ORIGIN_INDEX)
    override val fontSize: StateFlow<Int> = MutableStateFlow(DEFAULT_FONT_SIZE)
    override val onEvalDelay: StateFlow<Int> = MutableStateFlow(DEFAULT_ON_EVAL_DELAY)
    override val debugMode: StateFlow<Boolean> = MutableStateFlow(DEFAULT_DEBUG_MODE)
    private val setting = Settings()

    init {
        (arrayOriginIndex as MutableStateFlow).value =
            setting.getInt(ARRAY_ORIGIN_INDEX, DEFAULT_ARRAY_ORIGIN_INDEX)
        (fontSize as MutableStateFlow).value = setting.getInt(FONT_SIZE, DEFAULT_FONT_SIZE)
        (onEvalDelay as MutableStateFlow).value = setting.getInt(ON_EVAL_DELAY, DEFAULT_ON_EVAL_DELAY)
        (debugMode as MutableStateFlow).value = setting.getBoolean(DEBUG_MODE, DEFAULT_DEBUG_MODE)
    }


    override fun setListFirstIndex(index: Int) {
        setting.putInt(ARRAY_ORIGIN_INDEX, index)
        (arrayOriginIndex as MutableStateFlow).value = index
    }

    override fun setFontSize(size: Int) {
        setting.putInt(FONT_SIZE, size)
        (fontSize as MutableStateFlow).value = size
    }

    override fun setOnEvalDelay(delay: Int) {
        setting.putInt(ON_EVAL_DELAY, delay)
        (onEvalDelay as MutableStateFlow).value = delay
    }

    override fun setDebugMode(enabled: Boolean) {
        setting.putBoolean(DEBUG_MODE, enabled)
        (debugMode as MutableStateFlow).value = enabled
    }

    companion object {
        const val ARRAY_ORIGIN_INDEX = "arrayOriginIndex"
        const val FONT_SIZE = "fontSize"
        const val ON_EVAL_DELAY = "onEvalDelay"
        const val DEBUG_MODE = "debugMode"
    }
}
