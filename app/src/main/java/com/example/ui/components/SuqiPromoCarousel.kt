package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.LocalSuqiColors
import com.example.ui.theme.LucideIcons
import com.example.ui.theme.SuqiDotIdle
import com.example.ui.theme.SuqiFontFamily
import com.example.ui.theme.SuqiImageBackdrop
import com.example.ui.theme.SuqiPeach
import com.example.ui.theme.SuqiPrimary
import com.example.ui.theme.SuqiPrimarySubtle
import com.example.ui.theme.SuqiRadius
import com.example.ui.theme.suqiWarmShadow
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * One promotional slide.
 *
 * [highlight] must be a substring of [title]; it is rendered larger in the
 * primary orange (e.g. title "Up to 50% Off", highlight "50%").
 */
data class PromoBanner(
    val id: String,
    val badge: String,
    val title: String,
    val highlight: String = "",
    val subtitle: String,
    val buttonText: String,
    val imageUrl: String,
    val badgeIcon: ImageVector? = null,
    val targetCategory: String = "",
    val targetName: String = "",
    val destinationType: String = "CATEGORY",
    val targetId: String = ""
)

fun promoBadgeIconVector(key: String): ImageVector = when (key.lowercase()) {
    "flame" -> LucideIcons.Flame
    "house", "home" -> LucideIcons.House
    "tag" -> LucideIcons.Tag
    "percent" -> LucideIcons.Percent
    "zap" -> LucideIcons.Zap
    "gift" -> LucideIcons.Gift
    "award" -> LucideIcons.Award
    "star" -> LucideIcons.Star
    "trending", "trending-up" -> LucideIcons.TrendingUp
    else -> LucideIcons.Sparkles
}

val promoBadgeIconOptions: List<Pair<String, ImageVector>> = listOf(
    "sparkles" to LucideIcons.Sparkles,
    "flame" to LucideIcons.Flame,
    "tag" to LucideIcons.Tag,
    "percent" to LucideIcons.Percent,
    "zap" to LucideIcons.Zap,
    "gift" to LucideIcons.Gift,
    "award" to LucideIcons.Award,
    "house" to LucideIcons.House,
    "star" to LucideIcons.Star
)

/**
 * Full-width promotional carousel (Suqi UI direction).
 *
 * White rounded container → peach banner with Hot-Deal style badge, highlighted
 * discount headline, orange CTA and product photography → pill page indicators.
 * Auto-advances; pauses while the user is swiping.
 */
@Composable
fun SuqiPromoCarousel(
    banners: List<PromoBanner>,
    onBannerClick: (PromoBanner) -> Unit,
    modifier: Modifier = Modifier,
    autoAdvanceMillis: Long = 4500L
) {
    if (banners.isEmpty()) return

    val colors = LocalSuqiColors.current
    val pagerState = rememberPagerState(pageCount = { banners.size })
    val outerShape = RoundedCornerShape(SuqiRadius.Hero)

    LaunchedEffect(pagerState, banners.size) {
        while (banners.size > 1) {
            delay(autoAdvanceMillis)
            if (!pagerState.isScrollInProgress) {
                pagerState.animateScrollToPage((pagerState.currentPage + 1) % banners.size)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .suqiWarmShadow(outerShape, elevation = 14.dp)
                .clip(outerShape)
                .background(colors.cardBackground)
                .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 12.dp)
        ) {
            HorizontalPager(
                state = pagerState,
                pageSpacing = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(208.dp)
            ) { page ->
                val banner = banners[page]
                SuqiPromoBannerSlide(
                    banner = banner,
                    onClick = { onBannerClick(banner) },
                    modifier = Modifier.graphicsLayer {
                        val offset = abs(
                            (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                        ).coerceIn(0f, 1f)
                        alpha = 1f - offset * 0.35f
                    }
                )
            }

            if (banners.size > 1) {
                Spacer(modifier = Modifier.height(12.dp))
                PromoPagerDots(count = banners.size, current = pagerState.currentPage)
            }
        }

        // Small decorative strokes at the container's top-right corner
        SparkAccent(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 15.dp, y = (-15).dp)
        )
    }
}

@Composable
fun SuqiPromoBannerSlide(
    banner: PromoBanner,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val bannerShape = RoundedCornerShape(SuqiRadius.HeroInner)
    val imageShape = RoundedCornerShape(
        topStart = 64.dp,
        topEnd = 20.dp,
        bottomEnd = 20.dp,
        bottomStart = 20.dp
    )
    val gradient = if (colors.isDark) {
        listOf(colors.secondaryBackground, colors.cardBackground)
    } else {
        listOf(SuqiPrimarySubtle, SuqiPeach)
    }

    val headline = buildAnnotatedString {
        val index = if (banner.highlight.isBlank()) -1 else banner.title.indexOf(banner.highlight)
        if (index < 0) {
            append(banner.title)
        } else {
            append(banner.title.substring(0, index))
            withStyle(SpanStyle(color = SuqiPrimary, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)) {
                append(banner.highlight)
            }
            append(banner.title.substring(index + banner.highlight.length))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(bannerShape)
            .background(Brush.linearGradient(gradient))
            .clickable(onClick = onClick)
            .testTag("banner_slide_${banner.id}")
    ) {
        // Orange backdrop shape behind the photo
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 46.dp, y = 54.dp)
                .size(210.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(Color(0xFFFF8F3D), SuqiPrimary))
                )
        )

        // Product photography
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(banner.imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = banner.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(top = 14.dp, bottom = 14.dp, end = 14.dp)
                .width(124.dp)
                .fillMaxHeight()
                .suqiWarmShadow(imageShape, elevation = 10.dp)
                .clip(imageShape)
                .background(SuqiImageBackdrop)
        )

        // Copy
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .fillMaxWidth(0.58f)
                .padding(start = 18.dp, top = 16.dp, bottom = 16.dp)
        ) {
            Column {
                // Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(SuqiPrimary)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    if (banner.badgeIcon != null) {
                        Icon(
                            imageVector = banner.badgeIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = banner.badge,
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = Color.White,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = headline,
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    lineHeight = 30.sp,
                    color = colors.primaryText,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = banner.subtitle,
                    fontFamily = SuqiFontFamily,
                    fontSize = 13.sp,
                    color = colors.primaryText,
                    maxLines = 1
                )
            }

            // CTA (the whole banner is the tap target)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(SuqiPrimary)
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            ) {
                Text(
                    text = banner.buttonText,
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color.White,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = LucideIcons.ArrowRight,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun PromoPagerDots(count: Int, current: Int) {
    val colors = LocalSuqiColors.current
    val idle = if (colors.isDark) colors.border else SuqiDotIdle
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            val selected = index == current
            val dotWidth by animateDpAsState(
                targetValue = if (selected) 28.dp else 8.dp,
                label = "promo_dot_width"
            )
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(dotWidth)
                    .clip(CircleShape)
                    .background(if (selected) SuqiPrimary else idle)
            )
        }
    }
}

@Composable
private fun SparkAccent(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val pivot = Offset(0f, size.height)
        val strokeWidth = 3.dp.toPx()
        val inner = 8.dp.toPx()
        val outer = 18.dp.toPx()
        listOf(78.0, 45.0, 12.0).forEach { degrees ->
            val radians = Math.toRadians(degrees)
            val dx = cos(radians).toFloat()
            val dy = sin(radians).toFloat()
            drawLine(
                color = SuqiPrimary,
                start = Offset(pivot.x + dx * inner, pivot.y - dy * inner),
                end = Offset(pivot.x + dx * outer, pivot.y - dy * outer),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}
