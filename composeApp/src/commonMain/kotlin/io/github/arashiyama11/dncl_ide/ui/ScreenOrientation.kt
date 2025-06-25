package io.github.arashiyama11.dncl_ide.ui

import androidx.compose.ui.platform.WindowInfo

enum class ScreenOrientation {
    PORTRAIT,
    LANDSCAPE
}

val WindowInfo.orientation: ScreenOrientation
    get() = if (containerSize.width > containerSize.height) {
        ScreenOrientation.LANDSCAPE
    } else {
        ScreenOrientation.PORTRAIT
    }