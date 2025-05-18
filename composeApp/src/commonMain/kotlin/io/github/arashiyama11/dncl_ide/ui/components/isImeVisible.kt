package io.github.arashiyama11.dncl_ide.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity

@Composable
fun isImeVisible(): Boolean {
    return WindowInsets.ime.getBottom(LocalDensity.current) > 0
}
