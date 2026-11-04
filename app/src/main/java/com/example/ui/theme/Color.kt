package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Suqi Brand Colors - Vibrant, high-contrast, matching modern mobile commerce
val SuqiPrimary = Color(0xFFFF5500)       // Vivid punchy brand orange
val SuqiPrimaryHover = Color(0xFFE04A00)
val SuqiPrimaryLight = Color(0xFFFFF0E6)  // Soft warm tint
val SuqiPrimarySubtle = Color(0xFFFFF7F2)

// Light Mode Surface Tokens
val SuqiBackgroundLight = Color(0xFFFFFFFF)
val SuqiSurfaceLight = Color(0xFFFFFFFF)
val SuqiSecondaryBgLight = Color(0xFFF6F7F9) // Clean off-white card/container background (FitHub style)
val SuqiPrimaryTextLight = Color(0xFF0F172A)  // Deep slate/charcoal black - high contrast bold
val SuqiSecondaryTextLight = Color(0xFF64748B) // Slate secondary
val SuqiMutedTextLight = Color(0xFF94A3B8)
val SuqiBorderLight = Color(0xFFE2E8F0)

// Dark Mode Surface Tokens
val SuqiBackgroundDark = Color(0xFF0F1015)
val SuqiSurfaceDark = Color(0xFF181920)
val SuqiSecondaryBgDark = Color(0xFF22232C)
val SuqiPrimaryTextDark = Color(0xFFF8FAFC)
val SuqiSecondaryTextDark = Color(0xFF94A3B8)
val SuqiMutedTextDark = Color(0xFF64748B)
val SuqiBorderDark = Color(0xFF2E303E)

// Semantic State Colors
val SuqiSuccess = Color(0xFF16A34A)
val SuqiWarning = Color(0xFFF59E0B)
val SuqiError = Color(0xFFEF4444)

val SuqiSuccessLight = Color(0xFFDCFCE7)
val SuqiWarningLight = Color(0xFFFEF3C7)
val SuqiErrorLight = Color(0xFFFEE2E2)

// Dynamic contextual tokens - automatically resolves to Light or Dark based on current active theme
val SuqiBackground: Color
    @Composable
    get() = LocalSuqiColors.current.surfaceBackground

val SuqiSurface: Color
    @Composable
    get() = LocalSuqiColors.current.cardBackground

val SuqiSecondaryBackground: Color
    @Composable
    get() = LocalSuqiColors.current.secondaryBackground

val SuqiPrimaryText: Color
    @Composable
    get() = LocalSuqiColors.current.primaryText

val SuqiSecondaryText: Color
    @Composable
    get() = LocalSuqiColors.current.secondaryText

val SuqiMutedText: Color
    @Composable
    get() = LocalSuqiColors.current.mutedText

val SuqiBorder: Color
    @Composable
    get() = LocalSuqiColors.current.border


