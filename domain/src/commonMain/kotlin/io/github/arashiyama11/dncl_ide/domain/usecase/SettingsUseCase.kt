package io.github.arashiyama11.dncl_ide.domain.usecase

import io.github.arashiyama11.dncl_ide.domain.repository.ISettingsRepository

class SettingsUseCase(private val settingsRepository: ISettingsRepository) {
    val arrayOriginIndex = settingsRepository.arrayOriginIndex

    fun setListFirstIndex(index: Int) {
        settingsRepository.setListFirstIndex(index)
    }
}