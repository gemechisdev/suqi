package com.example.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────
// Suqi "orange on cream" UI direction
// Used by the promo carousel and the product item card.
// Existing locked tokens (SuqiPrimary #FF6500, SuqiPrimaryLight #FFF0E6,
// text #171717 / #737373) live in Color.kt and are reused as-is.
// ─────────────────────────────────────────────────────────────

/** Warm page background (#FFF8F0) for screens that show the carousel / product grid. */
val SuqiCream = Color(0xFFFFF8F0)

/** Deeper peach used as the end of the banner gradient. */
val SuqiPeach = Color(0xFFFFDFC7)

/** Soft warm-grey backdrop behind product photos. */
val SuqiImageBackdrop = Color(0xFFF5EFE9)

/** Idle pagination dot on a white surface. */
val SuqiDotIdle = Color(0xFFE2DDD8)

/** Warm tint used for soft elevation shadows (never pure black). */
val SuqiWarmShadow = Color(0xFFC2540A)

/** Corner radii (24–30dp range). */
object SuqiRadius {
    /** Product cards and inner banner. */
    val Card = 24.dp

    /** Outer carousel container. */
    val Hero = 30.dp

    /** Inner promo banner (Hero minus its 6dp inset). */
    val HeroInner = 24.dp
}

/** Soft, warm, subtle elevation. Does not clip — clip separately. */
fun Modifier.suqiWarmShadow(shape: Shape, elevation: Dp = 10.dp): Modifier =
    shadow(
        elevation = elevation,
        shape = shape,
        clip = false,
        ambientColor = SuqiWarmShadow.copy(alpha = 0.16f),
        spotColor = SuqiWarmShadow.copy(alpha = 0.26f)
    )
