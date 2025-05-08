package io.github.arashiyama11.dncl_ide.domain.model

data class SettingsState(
    val arrayOriginIndex: Int,
    val fontSize: Int,
    val onEvalDelay: Int,
    val debugMode: Boolean,
    val debugRunningMode: DebugRunningMode
)