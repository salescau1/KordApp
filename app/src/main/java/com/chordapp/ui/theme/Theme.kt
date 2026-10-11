package com.chordapp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private fun createDarkColorScheme(colors: ChordAppColors) = darkColorScheme(
    primary = colors.accentGold,
    secondary = colors.mutedGold,
    background = colors.background,
    surface = colors.cardSurface,
    onPrimary = colors.background,
    onSecondary = colors.primaryText,
    onBackground = colors.primaryText,
    onSurface = colors.primaryText
)

private fun createLightColorScheme(colors: ChordAppColors) = lightColorScheme(
    primary = colors.accentGold,
    secondary = colors.mutedGold,
    background = colors.background,
    surface = colors.cardSurface,
    onPrimary = colors.cardSurface,
    onSecondary = colors.primaryText,
    onBackground = colors.primaryText,
    onSurface = colors.primaryText
)

@Composable
fun ChordAppTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val customColors = when (themeMode) {
        AppThemeMode.DARK -> DarkColors
        AppThemeMode.LIGHT -> LightColors
        AppThemeMode.PAPER -> PaperColors
        AppThemeMode.BLACKBOARD -> BlackboardColors
        AppThemeMode.GREENBOARD -> GreenboardColors
    }
    val isDark = customColors.isDark
    val colorScheme = if (isDark) createDarkColorScheme(customColors) else createLightColorScheme(customColors)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = customColors.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    CompositionLocalProvider(LocalChordAppColors provides customColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
