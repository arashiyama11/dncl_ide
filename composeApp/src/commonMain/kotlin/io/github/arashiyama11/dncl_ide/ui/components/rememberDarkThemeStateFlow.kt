package io.github.arashiyama11.dncl_ide.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


@Composable
fun rememberDarkThemeStateFlow(): StateFlow<Boolean> {
    val isDark = isSystemInDarkTheme()
    val flow = remember { MutableStateFlow(isDark) }
    LaunchedEffect(isDark) {
        flow.emit(isDark)
    }
    return flow.asStateFlow()
}