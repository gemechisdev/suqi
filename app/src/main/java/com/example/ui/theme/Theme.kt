package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class SuqiExtendedColors(
    val primaryText: Color,
    val secondaryText: Color,
    val mutedText: Color,
    val surfaceBackground: Color,
    val secondaryBackground: Color,
    val cardBackground: Color,
    val border: Color,
    val isDark: Boolean
)

val LocalSuqiColors = staticCompositionLocalOf {
    SuqiExtendedColors(
        primaryText = SuqiPrimaryTextLight,
        secondaryText = SuqiSecondaryTextLight,
        mutedText = SuqiMutedTextLight,
        surfaceBackground = SuqiBackgroundLight,
        secondaryBackground = SuqiSecondaryBgLight,
        cardBackground = Color.White,
        border = SuqiBorderLight,
        isDark = false
    )
}

val SuqiLightColorScheme = lightColorScheme(
    primary = SuqiPrimary,
    onPrimary = Color.White,
    primaryContainer = SuqiPrimaryLight,
    onPrimaryContainer = SuqiPrimary,
    secondary = SuqiSecondaryTextLight,
    onSecondary = Color.White,
    secondaryContainer = SuqiSecondaryBgLight,
    onSecondaryContainer = SuqiPrimaryTextLight,
    tertiary = SuqiPrimaryHover,
    onTertiary = Color.White,
    background = SuqiBackgroundLight,
    onBackground = SuqiPrimaryTextLight,
    surface = SuqiSurfaceLight,
    onSurface = SuqiPrimaryTextLight,
    surfaceVariant = SuqiSecondaryBgLight,
    onSurfaceVariant = SuqiSecondaryTextLight,
    outline = SuqiBorderLight,
    outlineVariant = SuqiBorderLight,
    error = SuqiError,
    onError = Color.White,
    errorContainer = SuqiErrorLight,
    onErrorContainer = SuqiError
)

val SuqiDarkColorScheme = darkColorScheme(
    primary = SuqiPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3A1F10),
    onPrimaryContainer = Color(0xFFFF9E66),
    secondary = SuqiSecondaryTextDark,
    onSecondary = Color.White,
    secondaryContainer = SuqiSecondaryBgDark,
    onSecondaryContainer = SuqiPrimaryTextDark,
    tertiary = SuqiPrimaryHover,
    onTertiary = Color.White,
    background = SuqiBackgroundDark,
    onBackground = SuqiPrimaryTextDark,
    surface = SuqiSurfaceDark,
    onSurface = SuqiPrimaryTextDark,
    surfaceVariant = SuqiSecondaryBgDark,
    onSurfaceVariant = SuqiSecondaryTextDark,
    outline = SuqiBorderDark,
    outlineVariant = SuqiBorderDark,
    error = SuqiError,
    onError = Color.White,
    errorContainer = Color(0xFF450A0A),
    onErrorContainer = Color(0xFFFCA5A5)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SuqiDarkColorScheme else SuqiLightColorScheme
    val extendedColors = if (darkTheme) {
        SuqiExtendedColors(
            primaryText = SuqiPrimaryTextDark,
            secondaryText = SuqiSecondaryTextDark,
            mutedText = SuqiMutedTextDark,
            surfaceBackground = SuqiBackgroundDark,
            secondaryBackground = SuqiSecondaryBgDark,
            cardBackground = SuqiSurfaceDark,
            border = SuqiBorderDark,
            isDark = true
        )
    } else {
        SuqiExtendedColors(
            primaryText = SuqiPrimaryTextLight,
            secondaryText = SuqiSecondaryTextLight,
            mutedText = SuqiMutedTextLight,
            surfaceBackground = SuqiBackgroundLight,
            secondaryBackground = SuqiSecondaryBgLight,
            cardBackground = Color.White,
            border = SuqiBorderLight,
            isDark = false
        )
    }

    CompositionLocalProvider(LocalSuqiColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
