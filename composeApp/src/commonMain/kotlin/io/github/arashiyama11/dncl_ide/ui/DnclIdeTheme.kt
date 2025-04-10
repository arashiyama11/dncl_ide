package io.github.arashiyama11.dncl_ide.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun DnclIdeTheme(content: @Composable () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val colors = if (isDark)
        Colors(
            primary = Color(0xFF4b8aff),
            primaryVariant = Color(0xFF284777),
            secondary = Color(0xFFbec6dc),
            secondaryVariant = Color(0xFF3e4759),
            background = Color(0xFF121212),
            surface = Color(0xFF111318),
            error = Color(0xFFCF6679),
            onPrimary = Color(0xFF0a305f),
            onSecondary = Color(0xFF283141),
            onBackground = Color.White,
            onSurface = Color(0xFFe2e2e9),
            onError = Color.White,
            isLight = false
        )
    else
        Colors(
            primary = Color(0xFF4b8aff),
            primaryVariant = Color(0xFFd6e3ff),
            secondary = Color(0xFF565f71),
            secondaryVariant = Color(0xFFdae2f9),
            background = Color(0xFFFFFFFF),
            surface = Color(0xFFf9f9ff),
            error = Color(0xFFB00020),
            onPrimary = Color(0xff0a305f),
            onSecondary = Color.White,
            onBackground = Color.Black,
            onSurface = Color.Black,
            onError = Color.White,
            isLight = true
        )
    MaterialTheme(colors = colors) { content() }
}