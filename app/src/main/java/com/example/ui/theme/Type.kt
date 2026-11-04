package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val SuqiFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

val EthiopicFontFamily = FontFamily(
    Font(R.font.noto_sans_ethiopic, FontWeight.Normal),
    Font(R.font.noto_sans_ethiopic, FontWeight.Medium),
    Font(R.font.noto_sans_ethiopic, FontWeight.SemiBold),
    Font(R.font.noto_sans_ethiopic, FontWeight.Bold)
)

// Bold, high-clarity typography hierarchy (FitHub aesthetic)
val Typography = Typography(
    // Page Title: Bold 24sp
    headlineLarge = TextStyle(
        fontFamily = SuqiFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.5).sp
    ),
    // Section Heading: Bold 18sp
    titleLarge = TextStyle(
        fontFamily = SuqiFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.3).sp
    ),
    titleMedium = TextStyle(
        fontFamily = SuqiFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    // Product Title: Bold 15sp
    titleSmall = TextStyle(
        fontFamily = SuqiFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    // Body: Medium / Normal 15-16sp
    bodyLarge = TextStyle(
        fontFamily = SuqiFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = SuqiFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    // Button & Action Labels: Bold 15-16sp
    labelLarge = TextStyle(
        fontFamily = SuqiFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = SuqiFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelSmall = TextStyle(
        fontFamily = SuqiFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp
    )
)
