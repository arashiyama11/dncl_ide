package io.github.arashiyama11.domain.usecase

import io.github.arashiyama11.domain.repository.ISettingsRepository
import org.koin.core.annotation.Single

@Single
class SettingsUseCase(private val settingsRepository: ISettingsRepository) {
    val arrayOriginIndex = settingsRepository.arrayOriginIndex

    fun setListFirstIndex(index: Int) {
        settingsRepository.setListFirstIndex(index)
    }
}