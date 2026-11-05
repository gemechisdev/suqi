package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────
// Suqi Brand Colours (locked to Product Plan §12)
// Primary must remain #FF6500 — do not change without owner instruction.
// ─────────────────────────────────────────────────────────────

// LOCKED Plan §12 tokens — do not change without product owner decision
val SuqiPrimary = Color(0xFFFF6500)
val SuqiPrimaryHover = Color(0xFFE85B00)
val SuqiPrimaryLight = Color(0xFFFFF0E6)
val SuqiPrimarySubtle = Color(0xFFFFF7F2)

// Light mode neutrals (plan §12)
val SuqiBackgroundLight = Color(0xFFFFFFFF)
val SuqiSurfaceLight = Color(0xFFFFFFFF)
val SuqiSecondaryBgLight = Color(0xFFF8F8F8)
val SuqiPrimaryTextLight = Color(0xFF171717)
val SuqiSecondaryTextLight = Color(0xFF737373)
val SuqiMutedTextLight = Color(0xFFA3A3A3)
val SuqiBorderLight = Color(0xFFEEEEEE)

// Dark mode neutrals (kept for dark-theme support)
val SuqiBackgroundDark = Color(0xFF0F1015)
val SuqiSurfaceDark = Color(0xFF181920)
val SuqiSecondaryBgDark = Color(0xFF22232C)
val SuqiPrimaryTextDark = Color(0xFFF8FAFC)
val SuqiSecondaryTextDark = Color(0xFFA1A1AA)
val SuqiMutedTextDark = Color(0xFF8B8B96)
val SuqiBorderDark = Color(0xFF2E303E)

// Semantic
val SuqiSuccess = Color(0xFF16A34A)
val SuqiWarning = Color(0xFFF59E0B)
val SuqiError = Color(0xFFEF4444)

val SuqiSuccessLight = Color(0xFFDCFCE7)
val SuqiWarningLight = Color(0xFFFEF3C7)
val SuqiErrorLight = Color(0xFFFEE2E2)

// Dark-mode tint variants for status backgrounds
val SuqiSuccessDarkBg = Color(0xFF14532D)
val SuqiWarningDarkBg = Color(0xFF78350F)
val SuqiErrorDarkBg = Color(0xFF7F1D1D)
val SuqiPrimaryDarkBg = Color(0xFF3A1F10)

// ─────────────────────────────────────────────────────────────
// Theme-aware dynamic tokens (resolve from LocalSuqiColors)
// ─────────────────────────────────────────────────────────────

val SuqiBackground: Color
    @Composable get() = LocalSuqiColors.current.surfaceBackground

val SuqiSurface: Color
    @Composable get() = LocalSuqiColors.current.cardBackground

val SuqiSecondaryBackground: Color
    @Composable get() = LocalSuqiColors.current.secondaryBackground

val SuqiPrimaryText: Color
    @Composable get() = LocalSuqiColors.current.primaryText

val SuqiSecondaryText: Color
    @Composable get() = LocalSuqiColors.current.secondaryText

val SuqiMutedText: Color
    @Composable get() = LocalSuqiColors.current.mutedText

val SuqiBorder: Color
    @Composable get() = LocalSuqiColors.current.border

/** Soft primary tint that works in both light and dark. */
val SuqiPrimaryTint: Color
    @Composable get() = LocalSuqiColors.current.primaryTint

val SuqiSuccessBg: Color
    @Composable get() = LocalSuqiColors.current.successBg

val SuqiWarningBg: Color
    @Composable get() = LocalSuqiColors.current.warningBg

val SuqiErrorBg: Color
    @Composable get() = LocalSuqiColors.current.errorBg
