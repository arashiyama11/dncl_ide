package io.github.arashiyama11.dncl_ide.domain.usecase

import io.github.arashiyama11.dncl_ide.domain.model.DebugRunningMode
import io.github.arashiyama11.dncl_ide.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsUseCaseTest {
    private lateinit var settingsRepository: MockSettingsRepository
    private lateinit var settingsUseCase: SettingsUseCase

    @BeforeTest
    fun setup() {
        settingsRepository = MockSettingsRepository()
        settingsUseCase = SettingsUseCase(settingsRepository)
    }

    @Test
    fun `配列の開始インデックスが正しく設定されること`() = runTest {
        val newIndex = 1
        settingsUseCase.setListFirstIndex(newIndex)
        assertEquals(newIndex, settingsUseCase.arrayOriginIndex.value)
        assertEquals(newIndex, settingsRepository.arrayOriginIndex.value)
    }

    @Test
    fun `フォントサイズが正しく設定されること`() = runTest {
        val newSize = 20
        settingsUseCase.setFontSize(newSize)
        assertEquals(newSize, settingsUseCase.fontSize.value)
        assertEquals(newSize, settingsRepository.fontSize.value)
    }

    @Test
    fun `評価遅延時間が正しく設定されること`() = runTest {
        val newDelay = 100
        settingsUseCase.setOnEvalDelay(newDelay)
        assertEquals(newDelay, settingsUseCase.onEvalDelay.value)
        assertEquals(newDelay, settingsRepository.onEvalDelay.value)
    }

    @Test
    fun `デバッグモードが正しく設定されること`() = runTest {
        val newValue = true
        settingsUseCase.setDebugMode(newValue)
        assertEquals(newValue, settingsUseCase.debugMode.value)
        assertEquals(newValue, settingsRepository.debugMode.value)
    }

    @Test
    fun `デバッグ実行モードが正しく設定されること`() = runTest {
        val newMode = DebugRunningMode.BUTTON
        settingsUseCase.setDebugRunningMode(newMode)
        assertEquals(newMode, settingsUseCase.debugRunningMode.value)
        assertEquals(newMode, settingsRepository.debugRunningMode.value)
    }


    private class MockSettingsRepository : SettingsRepository {
        override val arrayOriginIndex =
            MutableStateFlow(SettingsRepository.DEFAULT_ARRAY_ORIGIN_INDEX)
        override val fontSize = MutableStateFlow(SettingsRepository.DEFAULT_FONT_SIZE)
        override val onEvalDelay = MutableStateFlow(SettingsRepository.DEFAULT_ON_EVAL_DELAY)
        override val debugMode = MutableStateFlow(SettingsRepository.DEFAULT_DEBUG_MODE)
        override val debugRunningMode =
            MutableStateFlow(SettingsRepository.DEFAULT_DEBUG_RUNNING_MODE)

        override fun setListFirstIndex(index: Int) {
            arrayOriginIndex.value = index
        }

        override fun setFontSize(size: Int) {
            fontSize.value = size
        }

        override fun setOnEvalDelay(delay: Int) {
            onEvalDelay.value = delay
        }

        override fun setDebugMode(enabled: Boolean) {
            debugMode.value = enabled
        }

        override fun setDebugRunningMode(mode: DebugRunningMode) {
            debugRunningMode.value = mode
        }
    }
}