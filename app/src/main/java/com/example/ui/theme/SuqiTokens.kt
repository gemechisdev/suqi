package com.example.ui.theme

import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────
// Suqi "orange on cream" UI direction
// Used across the whole marketplace (F-01 Design System).
// ─────────────────────────────────────────────────────────────

/** Warm page background (#FFF8F0) for light mode screens. */
val SuqiCream = Color(0xFFFFF8F0)

/** Deeper peach used as the end of the banner gradient. */
val SuqiPeach = Color(0xFFFFDFC7)

/** Soft warm-grey backdrop behind product photos. */
val SuqiImageBackdrop = Color(0xFFF5EFE9)
val SuqiImageBackdropDark = Color(0xFF262629)

/** Idle pagination dot on a white surface. */
val SuqiDotIdle = Color(0xFFE2DDD8)

/** Warm pink-orange surface and border for shop cards (light) and their dark-theme variants. */
val SuqiShopTintLight = Color(0xFFFFEBE4)
val SuqiShopTintDark = Color(0xFF3A1D22)
val SuqiShopBorderLight = Color(0xFFFFC9B8)
val SuqiShopBorderDark = Color(0xFF5E2F33)

/** Warm tint used for soft elevation shadows (never pure black). */
val SuqiWarmShadow = Color(0xFFC2540A)

/** Corner radii (24–30dp range for cards/heroes). */
object SuqiRadius {
    /** Product cards, section cards, and inner banner. */
    val Card = 24.dp

    /** Outer carousel container and sheets. */
    val Hero = 30.dp

    /** Inner promo banner (Hero minus its 6dp inset). */
    val HeroInner = 24.dp

    /** Chips, pills, buttons. */
    val Pill = 999.dp
}

/**
 * Soft, warm, subtle elevation in light mode.
 * In dark mode (F-01), drops elevation shadows and renders a subtle 1dp border instead.
 */
@Composable
fun Modifier.suqiWarmShadow(shape: Shape, elevation: Dp = 10.dp): Modifier {
    val colors = LocalSuqiColors.current
    return if (colors.isDark) {
        this.border(1.dp, colors.border, shape)
    } else {
        this.shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = SuqiWarmShadow.copy(alpha = 0.16f),
            spotColor = SuqiWarmShadow.copy(alpha = 0.26f)
        )
    }
}

