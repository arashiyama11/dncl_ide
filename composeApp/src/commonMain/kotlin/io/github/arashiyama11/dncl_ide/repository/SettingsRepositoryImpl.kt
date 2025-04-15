package io.github.arashiyama11.dncl_ide.repository

import com.russhwolf.settings.Settings
import io.github.arashiyama11.dncl_ide.domain.repository.SettingsRepository
import io.github.arashiyama11.dncl_ide.domain.repository.SettingsRepository.Companion.DEFAULT_ARRAY_ORIGIN_INDEX
import io.github.arashiyama11.dncl_ide.domain.repository.SettingsRepository.Companion.DEFAULT_FONT_SIZE
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsRepositoryImpl() : SettingsRepository {
    override val arrayOriginIndex: StateFlow<Int> = MutableStateFlow(DEFAULT_ARRAY_ORIGIN_INDEX)
    override val fontSize: StateFlow<Int> = MutableStateFlow(DEFAULT_FONT_SIZE)
    private val setting = Settings()

    init {
        (arrayOriginIndex as MutableStateFlow).value =
            setting.getInt(ARRAY_ORIGIN_INDEX, DEFAULT_ARRAY_ORIGIN_INDEX)
        (fontSize as MutableStateFlow).value = setting.getInt(FONT_SIZE, DEFAULT_FONT_SIZE)
    }


    override fun setListFirstIndex(index: Int) {
        setting.putInt(ARRAY_ORIGIN_INDEX, index)
        (arrayOriginIndex as MutableStateFlow).value = index
    }

    override fun setFontSize(size: Int) {
        setting.putInt(FONT_SIZE, size)
        (fontSize as MutableStateFlow).value = size
    }

    companion object {
        const val ARRAY_ORIGIN_INDEX = "arrayOriginIndex"
        const val FONT_SIZE = "fontSize"
    }
}