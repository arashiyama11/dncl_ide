package io.github.arashiyama11.dncl_ide.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import dncl_ide.composeapp.generated.resources.NOTONOTO_Black
import dncl_ide.composeapp.generated.resources.NOTONOTO_Bold
import dncl_ide.composeapp.generated.resources.NOTONOTO_ExtraBold
import dncl_ide.composeapp.generated.resources.NOTONOTO_ExtraLight
import dncl_ide.composeapp.generated.resources.NOTONOTO_Light
import dncl_ide.composeapp.generated.resources.NOTONOTO_Medium
import dncl_ide.composeapp.generated.resources.NOTONOTO_Regular
import dncl_ide.composeapp.generated.resources.NOTONOTO_SemiBold
import dncl_ide.composeapp.generated.resources.NOTONOTO_Thin
import dncl_ide.composeapp.generated.resources.Res

@Composable
fun DnclIdeTheme(content: @Composable () -> Unit) {
    val isDark = isSystemInDarkTheme()


    val darkColorScheme =
        darkColorScheme(
            primary = Color(0xFF4b8aff),
            secondary = Color(0xFFbec6dc),
            background = Color(0xFF121212),
            surface = Color(0xFF111318),
            error = Color(0xFFCF6679),
            onPrimary = Color(0xFF0a305f),
            onSecondary = Color(0xFF283141),
            onBackground = Color.White,
            onSurface = Color(0xFFe2e2e9),
            onError = Color.White,
            primaryContainer = Color(0xFF284777),
            secondaryContainer = Color(0xFF3e4759),
        )

    val lightColorScheme = lightColorScheme(
        primary = Color(0xFF4b8aff),
        secondary = Color(0xFF565f71),
        background = Color(0xFFFFFFFF),
        surface = Color(0xFFf9f9ff),
        error = Color(0xFFB00020),
        onPrimary = Color(0xff0a305f),
        onSecondary = Color.White,
        onBackground = Color.Black,
        onSurface = Color.Black,
        onError = Color.White,
        primaryContainer = Color(0xFFd6e3ff),
        secondaryContainer = Color(0xFFdae2f9),
    )


    val codeFontFamily = FontFamily(
        Font(resource = Res.font.NOTONOTO_Regular, weight = FontWeight.Normal),
        Font(resource = Res.font.NOTONOTO_Bold, weight = FontWeight.Bold),
        Font(resource = Res.font.NOTONOTO_Light, weight = FontWeight.Light),
        Font(resource = Res.font.NOTONOTO_Thin, weight = FontWeight.Thin),
        Font(resource = Res.font.NOTONOTO_Black, weight = FontWeight.Black),
        Font(resource = Res.font.NOTONOTO_ExtraBold, weight = FontWeight.ExtraBold),
        Font(resource = Res.font.NOTONOTO_ExtraLight, weight = FontWeight.ExtraLight),
        Font(resource = Res.font.NOTONOTO_SemiBold, weight = FontWeight.SemiBold),
        Font(resource = Res.font.NOTONOTO_Medium, weight = FontWeight.Medium)
    )


    val codeTypography = MaterialTheme.typography.copy(
        bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontFamily = codeFontFamily),
        bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontFamily = codeFontFamily),
        bodySmall = MaterialTheme.typography.bodySmall.copy(fontFamily = codeFontFamily),
        titleLarge = MaterialTheme.typography.titleLarge.copy(fontFamily = codeFontFamily),
        titleMedium = MaterialTheme.typography.titleMedium.copy(fontFamily = codeFontFamily),
        titleSmall = MaterialTheme.typography.titleSmall.copy(fontFamily = codeFontFamily),
    )


    CompositionLocalProvider(LocalCodeTypography provides codeTypography) {
        MaterialTheme(
            colorScheme = if (isDark) darkColorScheme else lightColorScheme,
        ) { content() }
    }
}


val LocalCodeTypography = compositionLocalOf<Typography> {
    error("No typography provided")
}