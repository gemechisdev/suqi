package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.roundToInt

/**
 * F-01 standardized page scaffold on Warm Cream (#FFF8F0) / Dark (#121212).
 */
@Composable
fun SuqiScreen(
    title: String? = null,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    headerActions: @Composable (RowScope.() -> Unit)? = null,
    isOffline: Boolean = false,
    onRetryOffline: (() -> Unit)? = null,
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    val colors = LocalSuqiColors.current
    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (title != null || onBackClick != null) {
                    SuqiHeader(
                        title = title.orEmpty(),
                        subtitle = subtitle,
                        onBackClick = onBackClick,
                        actions = headerActions
                    )
                }
                AnimatedVisibility(visible = isOffline) {
                    SuqiOfflineBanner(onRetry = onRetryOffline)
                }
            }
        },
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        containerColor = colors.surfaceBackground,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        content(padding)
    }
}

/**
 * F-01 compact unified top header.
 */
@Composable
fun SuqiHeader(
    title: String,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        if (onBackClick != null) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .suqiWarmShadow(CircleShape, elevation = 6.dp)
                    .clip(CircleShape)
                    .background(colors.cardBackground)
                    .clickable(onClick = onBackClick)
                    .testTag("header_back_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = LucideIcons.ArrowLeft,
                    contentDescription = "Back",
                    tint = colors.primaryText,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            if (title.isNotBlank()) {
                Text(
                    text = title,
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = colors.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontFamily = SuqiFontFamily,
                    fontSize = 12.sp,
                    color = colors.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (actions != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                content = actions
            )
        }
    }
}

/**
 * F-01 white/dark surface card with 24dp radius and warm light-mode elevation / 1dp dark border.
 */
@Composable
fun SuqiSectionCard(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    containerColor: Color? = null,
    cornerRadius: Dp = SuqiRadius.Card,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalSuqiColors.current
    val shape = RoundedCornerShape(cornerRadius)
    val bg = containerColor ?: colors.cardBackground
    Column(
        modifier = modifier
            .fillMaxWidth()
            .suqiWarmShadow(shape, elevation = 8.dp)
            .clip(shape)
            .background(bg)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content
    )
}

/**
 * F-05 Standardized empty state with warm circular icon well and orange pill CTA.
 */
@Composable
fun SuqiEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    ctaText: String? = null,
    onCtaClick: (() -> Unit)? = null,
    secondaryText: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
    testTag: String = "empty_state",
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(colors.primaryTint),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SuqiPrimary,
                modifier = Modifier.size(34.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            fontFamily = SuqiFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = colors.primaryText,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            fontFamily = SuqiFontFamily,
            fontSize = 14.sp,
            color = colors.secondaryText,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        if (!ctaText.isNullOrBlank() && onCtaClick != null) {
            Spacer(modifier = Modifier.height(20.dp))
            SuqiButton(
                text = ctaText,
                onClick = onCtaClick,
                isPill = true,
                testTag = "${testTag}_cta"
            )
        }
        if (!secondaryText.isNullOrBlank() && onSecondaryClick != null) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onSecondaryClick) {
                Text(
                    text = secondaryText,
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = SuqiPrimary
                )
            }
        }
    }
}

/**
 * F-05 Warm shimmer skeleton placeholder block.
 */
@Composable
fun SuqiSkeleton(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp)
) {
    val colors = LocalSuqiColors.current
    val transition = rememberInfiniteTransition(label = "suqi_skeleton")
    val progress by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )
    val baseColor = if (colors.isDark) colors.secondaryBackground else Color(0xFFEDE4DA)
    Box(
        modifier = modifier
            .clip(shape)
            .background(baseColor.copy(alpha = progress))
    )
}

@Composable
fun SuqiSkeletonProductGrid(count: Int = 4, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        val rows = (count + 1) / 2
        repeat(rows) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                repeat(2) {
                    SuqiSectionCard(
                        padding = 10.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        SuqiSkeleton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(132.dp),
                            shape = RoundedCornerShape(18.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        SuqiSkeleton(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(14.dp),
                            shape = RoundedCornerShape(7.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        SuqiSkeleton(
                            modifier = Modifier
                                .fillMaxWidth(0.45f)
                                .height(16.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SuqiSkeletonList(count: Int = 4, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        repeat(count) {
            SuqiSectionCard(padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SuqiSkeleton(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        SuqiSkeleton(
                            modifier = Modifier
                                .fillMaxWidth(0.65f)
                                .height(14.dp),
                            shape = RoundedCornerShape(7.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        SuqiSkeleton(
                            modifier = Modifier
                                .fillMaxWidth(0.45f)
                                .height(12.dp),
                            shape = RoundedCornerShape(6.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * F-01 Modal Bottom Sheet with Suqi 30dp top corners.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuqiBottomSheet(
    onDismissRequest: () -> Unit,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalSuqiColors.current
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = colors.cardBackground,
        contentColor = colors.primaryText,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = colors.primaryText,
                    modifier = Modifier.padding(bottom = 14.dp)
                )
            }
            content()
        }
    }
}

/**
 * F-01 Horizontal pill filter chip strip (36dp height).
 */
@Composable
fun SuqiFilterChips(
    items: List<Pair<String, String>>, // key to label
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "filter_chip"
) {
    val colors = LocalSuqiColors.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        items.forEach { (key, label) ->
            val selected = key == selectedKey
            val bg = if (selected) SuqiPrimary else colors.cardBackground
            val fg = if (selected) Color.White else colors.primaryText
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(bg)
                    .then(
                        if (!selected) Modifier.border(1.dp, colors.border, RoundedCornerShape(999.dp))
                        else Modifier
                    )
                    .clickable { onSelect(key) }
                    .padding(horizontal = 16.dp)
                    .testTag("${testTagPrefix}_$key")
            ) {
                Text(
                    text = label,
                    fontFamily = SuqiFontFamily,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 13.sp,
                    color = fg,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * F-01 Navigation/Settings row with 40dp tinted circular icon well and 48dp+ touch target.
 */
@Composable
fun SuqiListRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    badge: String? = null,
    badgeColor: Color = SuqiPrimary,
    iconTint: Color = SuqiPrimary,
    iconBackground: Color? = null,
    trailing: @Composable (() -> Unit)? = null,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null,
    testTag: String = "suqi_list_row",
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 4.dp, vertical = 10.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconBackground ?: colors.primaryTint),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = colors.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontFamily = SuqiFontFamily,
                    fontSize = 12.sp,
                    color = colors.secondaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (!badge.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(badgeColor.copy(alpha = 0.14f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = badge,
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = badgeColor
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        } else if (showChevron && onClick != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = LucideIcons.ChevronRight,
                contentDescription = null,
                tint = colors.mutedText,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * F-01 Rounded 20dp search input with soft warm shadow / dark border and clear button.
 */
@Composable
fun SuqiSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "Search products or shops...",
    onClear: (() -> Unit)? = { onValueChange("") },
    onSearch: (() -> Unit)? = null,
    trailingAction: @Composable (() -> Unit)? = null,
    testTag: String = "suqi_search_field",
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val shape = RoundedCornerShape(20.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .suqiWarmShadow(shape, elevation = 6.dp)
            .clip(shape)
            .background(colors.cardBackground)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Icon(
            imageVector = LucideIcons.Search,
            contentDescription = "Search",
            tint = colors.secondaryText,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    fontFamily = SuqiFontFamily,
                    fontSize = 15.sp,
                    color = colors.mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = { onSearch?.invoke() }
                ),
                textStyle = TextStyle(
                    fontFamily = SuqiFontFamily,
                    fontSize = 15.sp,
                    color = colors.primaryText
                ),
                cursorBrush = SolidColor(SuqiPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(testTag)
            )
        }
        if (value.isNotEmpty() && onClear != null) {
            IconButton(
                onClick = onClear,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("${testTag}_clear")
            ) {
                Icon(
                    imageVector = LucideIcons.X,
                    contentDescription = "Clear search",
                    tint = colors.secondaryText,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        if (trailingAction != null) {
            Spacer(modifier = Modifier.width(4.dp))
            trailingAction()
        }
    }
}

/**
 * F-01 Compact pill quantity stepper with accessible 48dp touch width/height.
 */
@Composable
fun SuqiStepper(
    quantity: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    minValue: Int = 1,
    maxValue: Int = Int.MAX_VALUE,
    testTagPrefix: String = "stepper",
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(colors.secondaryBackground)
            .border(1.dp, colors.border, RoundedCornerShape(999.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        IconButton(
            onClick = onDecrement,
            enabled = quantity > minValue,
            modifier = Modifier
                .size(40.dp)
                .testTag("${testTagPrefix}_minus")
        ) {
            Icon(
                imageVector = LucideIcons.Minus,
                contentDescription = "Decrease quantity",
                tint = if (quantity > minValue) colors.primaryText else colors.mutedText,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = quantity.toString(),
            fontFamily = SuqiFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = colors.primaryText,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        IconButton(
            onClick = onIncrement,
            enabled = quantity < maxValue,
            modifier = Modifier
                .size(40.dp)
                .testTag("${testTagPrefix}_plus")
        ) {
            Icon(
                imageVector = LucideIcons.Plus,
                contentDescription = "Increase quantity",
                tint = if (quantity < maxValue) SuqiPrimary else colors.mutedText,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * F-01 KPI Stat Card for Seller & Admin dashboards.
 */
@Composable
fun SuqiStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color = SuqiPrimary,
    trendText: String? = null,
    trendPositive: Boolean = true,
    onClick: (() -> Unit)? = null,
    testTag: String = "stat_card",
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    SuqiSectionCard(
        padding = 14.dp,
        onClick = onClick,
        modifier = modifier.testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(18.dp)
                )
            }
            if (!trendText.isNullOrBlank()) {
                val badgeBg = if (trendPositive) colors.successBg else colors.warningBg
                val badgeFg = if (trendPositive) SuqiSuccess else SuqiWarning
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = trendText,
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = badgeFg
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = value,
            fontFamily = SuqiFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 19.sp,
            color = colors.primaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontFamily = SuqiFontFamily,
            fontSize = 12.sp,
            color = colors.secondaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * F-01 Lightweight Jetpack Compose Canvas Line Chart (used on Seller & Admin dashboards).
 */
@Composable
fun SuqiLineChart(
    labels: List<String>,
    values: List<Double>,
    modifier: Modifier = Modifier,
    lineColor: Color = SuqiPrimary
) {
    val colors = LocalSuqiColors.current
    if (values.isEmpty()) return
    val maxVal = (values.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
    var selectedIdx by remember(values) { mutableIntStateOf(values.lastIndex) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Tooltip for selected point
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = labels.getOrNull(selectedIdx) ?: "",
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = colors.secondaryText
            )
            Text(
                text = formatEtb(values.getOrNull(selectedIdx) ?: 0.0),
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = lineColor
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            val w = size.width
            val h = size.height
            val padTop = 12.dp.toPx()
            val padBottom = 10.dp.toPx()
            val chartH = (h - padTop - padBottom).coerceAtLeast(1f)
            val count = values.size
            val stepX = if (count <= 1) 0f else w / (count - 1).toFloat()

            // Subtle horizontal grid lines
            val gridColor = colors.border
            for (g in 0..2) {
                val y = padTop + chartH * (g / 2f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val pts = values.mapIndexed { idx, v ->
                val x = if (count == 1) w / 2f else idx * stepX
                val ratio = (v / maxVal).toFloat().coerceIn(0f, 1f)
                val y = padTop + chartH * (1f - ratio)
                Offset(x, y)
            }

            if (pts.size >= 2) {
                val fillPath = Path().apply {
                    moveTo(pts.first().x, h - padBottom)
                    pts.forEach { lineTo(it.x, it.y) }
                    lineTo(pts.last().x, h - padBottom)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        listOf(lineColor.copy(alpha = 0.25f), Color.Transparent)
                    )
                )

                val strokePath = Path().apply {
                    moveTo(pts.first().x, pts.first().y)
                    for (i in 1 until pts.size) {
                        val prev = pts[i - 1]
                        val curr = pts[i]
                        val midX = (prev.x + curr.x) / 2f
                        cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                    }
                }
                drawPath(
                    path = strokePath,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            pts.forEachIndexed { idx, p ->
                val isSelected = idx == selectedIdx
                drawCircle(
                    color = if (isSelected) lineColor else colors.cardBackground,
                    radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                    center = p
                )
                drawCircle(
                    color = lineColor,
                    radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                    center = p,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            labels.forEachIndexed { idx, lbl ->
                Text(
                    text = lbl,
                    fontFamily = SuqiFontFamily,
                    fontWeight = if (idx == selectedIdx) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 11.sp,
                    color = if (idx == selectedIdx) lineColor else colors.mutedText,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedIdx = idx }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * F-01 Compact Canvas Bar Chart.
 */
@Composable
fun SuqiBarChart(
    labels: List<String>,
    values: List<Double>,
    modifier: Modifier = Modifier,
    barColor: Color = SuqiPrimary
) {
    val colors = LocalSuqiColors.current
    if (values.isEmpty()) return
    val maxVal = (values.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
        ) {
            val count = values.size.coerceAtLeast(1)
            val slotW = size.width / count
            val barW = (slotW * 0.55f).coerceAtMost(28.dp.toPx())
            values.forEachIndexed { idx, v ->
                val ratio = (v / maxVal).toFloat().coerceIn(0.06f, 1f)
                val barH = size.height * ratio
                val left = idx * slotW + (slotW - barW) / 2f
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(left, size.height - barH),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxWidth()
        ) {
            labels.forEach { lbl ->
                Text(
                    text = lbl,
                    fontFamily = SuqiFontFamily,
                    fontSize = 11.sp,
                    color = colors.mutedText
                )
            }
        }
    }
}

/**
 * F-05 Offline banner displayed below top headers when connectivity is unavailable.
 */
@Composable
fun SuqiOfflineBanner(
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(colors.warningBg)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("offline_banner")
    ) {
        Icon(
            imageVector = LucideIcons.WifiOff,
            contentDescription = null,
            tint = SuqiWarning,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "You're offline — showing cached marketplace data",
            fontFamily = SuqiFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = colors.primaryText,
            modifier = Modifier.weight(1f)
        )
        if (onRetry != null) {
            Text(
                text = "Retry",
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = SuqiPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * F-05 Inline recoverable error card.
 */
@Composable
fun SuqiErrorCard(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    SuqiSectionCard(
        containerColor = colors.errorBg,
        modifier = modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = LucideIcons.TriangleAlert,
                contentDescription = null,
                tint = SuqiError,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                fontFamily = SuqiFontFamily,
                fontSize = 13.sp,
                color = colors.primaryText,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = onRetry) {
                Text(
                    text = "Retry",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = SuqiError
                )
            }
        }
    }
}

/**
 * F-02 / A-01 Access Denied / Restricted screen state for role-gated routes.
 */
@Composable
fun SuqiRestrictedState(
    title: String,
    subtitle: String,
    primaryButtonText: String,
    onPrimaryClick: () -> Unit,
    onBackClick: () -> Unit,
    testTag: String = "restricted_state",
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceBackground)
    ) {
        SuqiHeader(title = "Access Restricted", onBackClick = onBackClick)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            SuqiSectionCard(padding = 24.dp, modifier = Modifier.testTag(testTag)) {
                SuqiEmptyState(
                    icon = LucideIcons.Lock,
                    title = title,
                    subtitle = subtitle,
                    ctaText = primaryButtonText,
                    onCtaClick = onPrimaryClick,
                    secondaryText = "Go Back",
                    onSecondaryClick = onBackClick,
                    testTag = "${testTag}_inner"
                )
            }
        }
    }
}

/**
 * F-02 Seller Studio 5-tab floating bottom bar (Dashboard, Orders, Products, Inbox, More).
 */
@Composable
fun SuqiSellerFloatingBottomNav(
    currentRoute: String,
    pendingOrdersCount: Int = 0,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val tabs = listOf(
        Triple("seller_dashboard", "Dashboard", LucideIcons.BarChart2),
        Triple("seller_orders", "Orders", LucideIcons.Package),
        Triple("seller_products", "Products", LucideIcons.ShoppingBag),
        Triple("seller_inbox", "Inbox", LucideIcons.MessageCircle),
        Triple("seller_more", "More", LucideIcons.Menu)
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.cardBackground,
            shadowElevation = if (colors.isDark) 0.dp else 10.dp,
            border = BorderStroke(1.dp, colors.border),
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize()
            ) {
                tabs.forEach { (route, label, icon) ->
                    val selected = currentRoute == route
                    val tint by animateColorAsState(
                        targetValue = if (selected) SuqiPrimary else colors.secondaryText,
                        label = "seller_nav_tint"
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onNavigate(route) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("seller_nav_$route")
                    ) {
                        BadgedBox(
                            badge = {
                                if (route == "seller_orders" && pendingOrdersCount > 0) {
                                    Badge(containerColor = SuqiPrimary, contentColor = Color.White) {
                                        Text(
                                            text = pendingOrdersCount.toString(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = tint,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = label,
                            fontFamily = SuqiFontFamily,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            fontSize = 11.sp,
                            color = tint,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * F-02 Admin Console 4-tab segmented pill switcher (Overview, Queues, Manage, Tools).
 */
@Composable
fun SuqiAdminSegmentedTabs(
    selectedTab: String,
    onSelectTab: (String) -> Unit,
    isDeveloperVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val tabs = buildList {
        add("overview" to "Overview")
        add("queues" to "Queues")
        add("manage" to "Manage")
        if (isDeveloperVisible) add("tools" to "Tools")
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(colors.cardBackground)
            .border(1.dp, colors.border, RoundedCornerShape(20.dp))
            .padding(4.dp)
    ) {
        tabs.forEach { (key, label) ->
            val selected = key == selectedTab
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selected) SuqiPrimary else Color.Transparent)
                    .clickable { onSelectTab(key) }
                    .testTag("admin_tab_$key")
            ) {
                Text(
                    text = label,
                    fontFamily = SuqiFontFamily,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp,
                    color = if (selected) Color.White else colors.secondaryText,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * B-16 Guest gating bottom sheet shown when a visitor taps a protected action.
 */
@Composable
fun SuqiGuestGateSheet(
    actionLabel: String,
    onSignInClick: () -> Unit,
    onDismiss: () -> Unit
) {
    SuqiBottomSheet(onDismissRequest = onDismiss, title = "Sign in to continue") {
        SuqiEmptyState(
            icon = LucideIcons.CircleUser,
            title = "Account required to $actionLabel",
            subtitle = "Create a free Suqi shopper account with your Ethiopian phone number or Google/Telegram to save items, track orders, and chat with sellers.",
            ctaText = "Sign In or Create Account",
            onCtaClick = onSignInClick,
            secondaryText = "Continue browsing as Guest",
            onSecondaryClick = onDismiss,
            testTag = "guest_gate_sheet"
        )
    }
}
