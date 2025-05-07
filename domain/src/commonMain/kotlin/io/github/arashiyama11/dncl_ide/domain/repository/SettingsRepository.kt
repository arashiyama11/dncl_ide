package io.github.arashiyama11.dncl_ide.domain.repository

import io.github.arashiyama11.dncl_ide.domain.model.DebugRunningMode
import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    val arrayOriginIndex: StateFlow<Int>
    val fontSize: StateFlow<Int>
    val onEvalDelay: StateFlow<Int>
    val debugMode: StateFlow<Boolean>
    val debugRunningMode: StateFlow<DebugRunningMode>
    fun setListFirstIndex(index: Int)
    fun setFontSize(size: Int)
    fun setOnEvalDelay(delay: Int)
    fun setDebugMode(enabled: Boolean)
    fun setDebugRunningMode(mode: DebugRunningMode)

    companion object {
        const val DEFAULT_ARRAY_ORIGIN_INDEX = 0
        const val DEFAULT_FONT_SIZE = 16
        const val DEFAULT_ON_EVAL_DELAY = 10
        const val DEFAULT_DEBUG_MODE = false
        val DEFAULT_DEBUG_RUNNING_MODE = DebugRunningMode.NON_BLOCKING
    }
}
