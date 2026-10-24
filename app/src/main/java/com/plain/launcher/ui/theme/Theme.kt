package com.plain.launcher.ui.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.plain.launcher.data.model.ThemeMode
import com.plain.launcher.data.model.UserSettings
import com.plain.launcher.util.ContrastCalculator

data class PlainColors(
    val background: Color,
    val onBackground: Color,
    val secondaryText: Color,
    val surface: Color,
    val onSurface: Color,
    val contrastRatio: Float
)

val LocalPlainColors = compositionLocalOf {
    PlainColors(
        background = Color.Black,
        onBackground = Color.White,
        secondaryText = Color(0xFF888888),
        surface = Color(0xFF1A1A1A),
        onSurface = Color.White,
        contrastRatio = 21f
    )
}

val LocalPlainSettings = compositionLocalOf {
    UserSettings()
}

object PlainTheme {
    val colors: PlainColors
        @Composable
        @ReadOnlyComposable
        get() = LocalPlainColors.current

    val settings: UserSettings
        @Composable
        @ReadOnlyComposable
        get() = LocalPlainSettings.current
}

private data class ThemePalette(
    val bgGray: Float,
    val textGray: Float,
    val secondaryGray: Float,
    val surfaceGray: Float
)

@Composable
fun PlainTheme(
    settings: UserSettings,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (settings.themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.AMOLED -> true
        ThemeMode.SYSTEM -> systemInDark
    }

    val palette = when (settings.themeMode) {
        ThemeMode.LIGHT -> ThemePalette(
            bgGray = 0.96f,
            textGray = 0.08f,
            secondaryGray = 0.45f,
            surfaceGray = 0.88f
        )
        ThemeMode.AMOLED -> ThemePalette(
            bgGray = 0.0f,
            textGray = 0.95f,
            secondaryGray = 0.50f,
            surfaceGray = 0.12f
        )
        ThemeMode.DARK -> ThemePalette(
            bgGray = 0.08f,
            textGray = 0.95f,
            secondaryGray = 0.55f,
            surfaceGray = 0.16f
        )
        ThemeMode.SYSTEM -> {
            if (systemInDark) {
                ThemePalette(bgGray = 0.08f, textGray = 0.95f, secondaryGray = 0.55f, surfaceGray = 0.16f)
            } else {
                ThemePalette(bgGray = 0.96f, textGray = 0.08f, secondaryGray = 0.45f, surfaceGray = 0.88f)
            }
        }
    }

    val contrastRatio = ContrastCalculator.calculateContrast(palette.bgGray, palette.textGray)

    val backgroundColor = Color(palette.bgGray, palette.bgGray, palette.bgGray, 1f)
    val onBackgroundColor = Color(palette.textGray, palette.textGray, palette.textGray, 1f)
    val secondaryColor = Color(palette.secondaryGray, palette.secondaryGray, palette.secondaryGray, 1f)
    val surfaceColor = Color(palette.surfaceGray, palette.surfaceGray, palette.surfaceGray, 1f)

    val plainColors = PlainColors(
        background = backgroundColor,
        onBackground = onBackgroundColor,
        secondaryText = secondaryColor,
        surface = surfaceColor,
        onSurface = onBackgroundColor,
        contrastRatio = contrastRatio
    )

    val m3ColorScheme = if (isDark) {
        darkColorScheme(
            background = backgroundColor,
            onBackground = onBackgroundColor,
            surface = surfaceColor,
            onSurface = onBackgroundColor,
            primary = onBackgroundColor,
            onPrimary = backgroundColor,
            surfaceVariant = surfaceColor,
            onSurfaceVariant = secondaryColor
        )
    } else {
        lightColorScheme(
            background = backgroundColor,
            onBackground = onBackgroundColor,
            surface = surfaceColor,
            onSurface = onBackgroundColor,
            primary = onBackgroundColor,
            onPrimary = backgroundColor,
            surfaceVariant = surfaceColor,
            onSurfaceVariant = secondaryColor
        )
    }

    CompositionLocalProvider(
        LocalPlainColors provides plainColors,
        LocalPlainSettings provides settings,
        LocalIndication provides NoRippleIndication
    ) {
        MaterialTheme(
            colorScheme = m3ColorScheme,
            typography = PlainTypography,
            content = content
        )
    }
}
