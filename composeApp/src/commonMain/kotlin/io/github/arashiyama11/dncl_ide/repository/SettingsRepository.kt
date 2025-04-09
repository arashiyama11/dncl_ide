package io.github.arashiyama11.dncl_ide.repository

import com.russhwolf.settings.Settings
import io.github.arashiyama11.dncl_ide.domain.repository.ISettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsRepository() : ISettingsRepository {
    override val arrayOriginIndex: StateFlow<Int> = MutableStateFlow(0)
    private val setting = Settings()

    init {
        (arrayOriginIndex as MutableStateFlow).value =
            setting.getInt(ARRAY_ORIGIN_INDEX, DEFAULT_ARRAY_ORIGIN_INDEX)
    }


    override fun setListFirstIndex(index: Int) {
        setting.putInt(ARRAY_ORIGIN_INDEX, index)
        (arrayOriginIndex as MutableStateFlow).value = index
    }

    companion object {
        const val DEFAULT_ARRAY_ORIGIN_INDEX = 0
        const val ARRAY_ORIGIN_INDEX = "arrayOriginIndex"
    }
}