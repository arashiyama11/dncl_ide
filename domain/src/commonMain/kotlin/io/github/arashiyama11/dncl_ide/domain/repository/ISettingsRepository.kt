package io.github.arashiyama11.dncl_ide.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface ISettingsRepository {
    val arrayOriginIndex: StateFlow<Int>
    val fontSize: StateFlow<Int>
    fun setListFirstIndex(index: Int)
    fun setFontSize(size: Int)

    companion object {
        const val DEFAULT_ARRAY_ORIGIN_INDEX = 0
        const val DEFAULT_FONT_SIZE = 16
    }
}