package io.github.arashiyama11.dncl_ide.domain.usecase

import io.github.arashiyama11.dncl_ide.domain.repository.SettingsRepository

class SettingsUseCase(private val settingsRepository: SettingsRepository) {
    val arrayOriginIndex = settingsRepository.arrayOriginIndex
    val fontSize = settingsRepository.fontSize

    fun setListFirstIndex(index: Int) {
        settingsRepository.setListFirstIndex(index)
    }

    fun setFontSize(size: Int) {
        settingsRepository.setFontSize(size)
    }
}