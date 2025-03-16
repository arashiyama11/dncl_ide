package io.github.arashiyama11.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface ISettingsRepository {
    val arrayOriginIndex: StateFlow<Int>
    fun setListFirstIndex(index: Int)
}