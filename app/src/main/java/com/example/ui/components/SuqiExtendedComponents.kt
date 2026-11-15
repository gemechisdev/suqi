package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.api.BackendConfig
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

// ==========================================
// F-07 Extended Shared UI Components
// ==========================================

enum class SuqiTabStyle { Underline, Pill }

data class SuqiTabItem(
    val key: String,
    val label: String,
    val count: Int? = null,
    val showDot: Boolean = false
)

@Composable
fun SuqiTabs(
    tabs: List<SuqiTabItem>,
    selectedIndex: Int,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier,
    style: SuqiTabStyle = SuqiTabStyle.Underline,
    scrollable: Boolean = false,
    testTagPrefix: String = "suqi_tab"
) {
    val colors = LocalSuqiColors.current
    val rowModifier = modifier
        .fillMaxWidth()
        .then(if (scrollable) Modifier.horizontalScroll(rememberScrollState()) else Modifier)

    if (style == SuqiTabStyle.Pill) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = rowModifier.padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = index == selectedIndex
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .then(if (!scrollable) Modifier.weight(1f) else Modifier)
                        .heightIn(min = 42.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (selected) SuqiPrimary else colors.cardBackground)
                        .then(
                            if (!selected) Modifier.border(1.dp, colors.border, RoundedCornerShape(999.dp))
                            else Modifier
                        )
                        .clickable { onSelectTab(index) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("${testTagPrefix}_${tab.key}")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (tab.count != null) "${tab.label} (${tab.count})" else tab.label,
                            fontFamily = SuqiFontFamily,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            color = if (selected) Color.White else colors.primaryText,
                            maxLines = 1
                        )
                        if (tab.showDot) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (selected) Color.White else SuqiPrimary)
                            )
                        }
                    }
                }
            }
        }
    } else {
        Column(modifier = modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (scrollable) Modifier.horizontalScroll(rememberScrollState()) else Modifier)
                    .padding(horizontal = 12.dp)
            ) {
                tabs.forEachIndexed { index, tab ->
                    val selected = index == selectedIndex
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .then(if (!scrollable) Modifier.weight(1f) else Modifier)
                            .defaultMinSize(minHeight = 48.dp)
                            .clickable { onSelectTab(index) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("${testTagPrefix}_${tab.key}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = if (tab.count != null) "${tab.label} (${tab.count})" else tab.label,
                                fontFamily = SuqiFontFamily,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (selected) SuqiPrimary else colors.secondaryText,
                                maxLines = 1
                            )
                            if (tab.showDot) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(SuqiPrimary)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (selected) SuqiPrimary else Color.Transparent)
                        )
                    }
                }
            }
            HorizontalDivider(color = colors.border, thickness = 1.dp)
        }
    }
}

@Composable
fun SuqiSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    testTag: String? = null
) {
    val colors = LocalSuqiColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 6.dp)
            .let { if (testTag != null) it.testTag(testTag) else it }
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(colors.primaryTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = if (enabled) colors.primaryText else colors.mutedText
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontFamily = SuqiFontFamily,
                    fontSize = 12.sp,
                    color = colors.secondaryText,
                    lineHeight = 16.sp
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = if (enabled) onCheckedChange else null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = SuqiPrimary,
                checkedThumbColor = Color.White
            )
        )
    }
}

data class SuqiChartSegment(
    val label: String,
    val value: Float,
    val color: Color
)

@Composable
fun SuqiDonutChart(
    segments: List<SuqiChartSegment>,
    centerValue: String,
    centerLabel: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val total = segments.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(0.001f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(132.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                var startAngle = -90f
                val stroke = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Butt)
                segments.forEach { seg ->
                    val sweep = (seg.value / total) * 360f
                    if (sweep > 0f) {
                        drawArc(
                            color = seg.color,
                            startAngle = startAngle,
                            sweepAngle = sweep.coerceAtLeast(1f),
                            useCenter = false,
                            style = stroke
                        )
                        startAngle += sweep
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = centerValue,
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = colors.primaryText
                )
                Text(
                    text = centerLabel,
                    fontFamily = SuqiFontFamily,
                    fontSize = 11.sp,
                    color = colors.secondaryText
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            segments.forEach { seg ->
                val pct = ((seg.value / total) * 100f).toInt()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(seg.color)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = seg.label,
                        fontFamily = SuqiFontFamily,
                        fontSize = 12.sp,
                        color = colors.primaryText,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$pct%",
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = colors.secondaryText
                    )
                }
            }
        }
    }
}

data class SuqiFunnelStage(
    val label: String,
    val count: Int,
    val conversionText: String? = null
)

@Composable
fun SuqiFunnelBars(
    stages: List<SuqiFunnelStage>,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val maxVal = (stages.maxOfOrNull { it.count } ?: 1).coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier.fillMaxWidth()) {
        stages.forEachIndexed { idx, stage ->
            val ratio = (stage.count.toFloat() / maxVal.toFloat()).coerceIn(0.08f, 1f)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stage.label,
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = colors.primaryText
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (!stage.conversionText.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(colors.primaryTint)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = stage.conversionText,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = SuqiPrimary
                                )
                            }
                        }
                        Text(
                            text = stage.count.toString(),
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = colors.primaryText
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.secondaryBackground)
                ) {
                    val alpha = 1f - (idx * 0.18f).coerceAtMost(0.45f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(ratio)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(999.dp))
                            .background(SuqiPrimary.copy(alpha = alpha))
                    )
                }
            }
        }
    }
}

@Composable
fun SuqiSparkline(
    values: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = SuqiPrimary
) {
    if (values.isEmpty()) return
    Canvas(modifier = modifier.height(26.dp).width(68.dp)) {
        val min = values.minOrNull() ?: 0f
        val max = values.maxOrNull() ?: 1f
        val range = (max - min).coerceAtLeast(1f)
        val step = if (values.size > 1) size.width / (values.size - 1) else size.width
        val pts = values.mapIndexed { idx, v ->
            val x = idx * step
            val y = size.height - ((v - min) / range) * (size.height - 6f) - 3f
            Offset(x, y)
        }
        for (i in 0 until pts.size - 1) {
            drawLine(
                color = lineColor,
                start = pts[i],
                end = pts[i + 1],
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        pts.lastOrNull()?.let { last ->
            drawCircle(color = lineColor, radius = 3.dp.toPx(), center = last)
        }
    }
}

@Composable
fun SuqiRangeSlider(
    valueRange: ClosedFloatingPointRange<Float>,
    values: ClosedFloatingPointRange<Float>,
    onValuesChange: (ClosedFloatingPointRange<Float>) -> Unit,
    modifier: Modifier = Modifier,
    formatLabel: (Float) -> String = { "ETB ${it.toInt()}" }
) {
    val colors = LocalSuqiColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatLabel(values.start),
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = SuqiPrimary
            )
            Text(
                text = formatLabel(values.endInclusive),
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = SuqiPrimary
            )
        }
        RangeSlider(
            value = values,
            onValueChange = onValuesChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = SuqiPrimary,
                activeTrackColor = SuqiPrimary,
                inactiveTrackColor = colors.secondaryBackground
            )
        )
    }
}

@Composable
fun SuqiStepIndicator(
    steps: List<String>,
    currentStep: Int,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier.fillMaxWidth()
    ) {
        steps.forEachIndexed { index, label ->
            val completed = index < currentStep
            val active = index == currentStep
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(
                                when {
                                    index == 0 -> Color.Transparent
                                    completed || active -> SuqiPrimary
                                    else -> colors.border
                                }
                            )
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    completed || active -> SuqiPrimary
                                    else -> colors.secondaryBackground
                                }
                            )
                            .border(
                                width = if (active) 2.dp else 1.dp,
                                color = if (completed || active) SuqiPrimary else colors.border,
                                shape = CircleShape
                            )
                    ) {
                        if (completed) {
                            Icon(LucideIcons.Check, contentDescription = label, tint = Color.White, modifier = Modifier.size(14.dp))
                        } else {
                            Text(
                                text = "${index + 1}",
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (active) Color.White else colors.secondaryText
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(
                                when {
                                    index == steps.lastIndex -> Color.Transparent
                                    completed -> SuqiPrimary
                                    else -> colors.border
                                }
                            )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = label,
                    fontFamily = SuqiFontFamily,
                    fontWeight = if (active || completed) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 11.sp,
                    color = if (active || completed) colors.primaryText else colors.secondaryText,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuqiTimeField(
    label: String,
    timeText: String,
    onTimeSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = LocalSuqiColors.current
    var showPicker by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        if (label.isNotBlank()) {
            Text(
                text = label,
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = colors.secondaryText,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (enabled) colors.secondaryBackground else colors.surfaceBackground)
                .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                .clickable(enabled = enabled) { showPicker = true }
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = timeText.ifBlank { "09:00" },
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = if (enabled) colors.primaryText else colors.mutedText
            )
            Icon(LucideIcons.Clock, contentDescription = "Pick time", tint = SuqiPrimary, modifier = Modifier.size(16.dp))
        }
    }

    if (showPicker) {
        val parts = timeText.split(":")
        val initHour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 9
        val initMinute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        val state = rememberTimePickerState(initialHour = initHour, initialMinute = initMinute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text(label.ifBlank { "Select Time" }, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold) },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    val formatted = String.format(Locale.US, "%02d:%02d", state.hour, state.minute)
                    onTimeSelected(formatted)
                    showPicker = false
                }) {
                    Text("Confirm", color = SuqiPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            },
            containerColor = colors.cardBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuqiDateField(
    label: String,
    dateText: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = LocalSuqiColors.current
    var showPicker by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        if (label.isNotBlank()) {
            Text(
                text = label,
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = colors.secondaryText,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (enabled) colors.secondaryBackground else colors.surfaceBackground)
                .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                .clickable(enabled = enabled) { showPicker = true }
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = dateText.ifBlank { "Select date" },
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = if (dateText.isNotBlank() && enabled) colors.primaryText else colors.mutedText
            )
            Icon(LucideIcons.Calendar, contentDescription = "Pick date", tint = SuqiPrimary, modifier = Modifier.size(16.dp))
        }
    }

    if (showPicker) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) {
                        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        onDateSelected(fmt.format(Date(millis)))
                    }
                    showPicker = false
                }) {
                    Text("Confirm", color = SuqiPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
fun SuqiCountdownChips(
    hours: Int,
    minutes: Int,
    seconds: Int,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
    ) {
        listOf(hours, minutes, seconds).forEachIndexed { idx, unit ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .suqiWarmShadow(RoundedCornerShape(12.dp), elevation = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
            ) {
                Text(
                    text = String.format(Locale.US, "%02d", unit.coerceAtLeast(0)),
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = Color(0xFF171717)
                )
            }
            if (idx < 2) {
                Text(
                    text = ":",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = SuqiPrimary
                )
            }
        }
    }
}

@Composable
fun SuqiCouponTicket(
    code: String,
    discountLabel: String,
    title: String,
    conditions: String,
    expiry: String,
    onCopy: () -> Unit,
    onPrimaryAction: () -> Unit,
    modifier: Modifier = Modifier,
    statusLabel: String? = null,
    isExpired: Boolean = false,
    primaryActionLabel: String = "Use now"
) {
    val colors = LocalSuqiColors.current
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .suqiWarmShadow(shape, elevation = 6.dp)
            .clip(shape)
            .background(colors.cardBackground)
            .border(1.dp, colors.border, shape)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // Left discount ticket stub
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(106.dp)
                    .fillMaxHeight()
                    .background(if (isExpired) colors.secondaryBackground else colors.primaryTint)
                    .padding(12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = discountLabel,
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = if (isExpired) colors.mutedText else SuqiPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.cardBackground)
                            .border(1.dp, if (isExpired) colors.border else SuqiPrimary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = code,
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = if (isExpired) colors.mutedText else SuqiPrimary,
                            maxLines = 1
                        )
                    }
                }
            }

            // Perforated notch divider
            Box(
                modifier = Modifier
                    .width(14.dp)
                    .fillMaxHeight()
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val notchR = 7.dp.toPx()
                    drawCircle(color = colors.surfaceBackground, radius = notchR, center = Offset(size.width / 2f, 0f))
                    drawCircle(color = colors.surfaceBackground, radius = notchR, center = Offset(size.width / 2f, size.height))
                    drawLine(
                        color = colors.border,
                        start = Offset(size.width / 2f, notchR + 4f),
                        end = Offset(size.width / 2f, size.height - notchR - 4f),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                    )
                }
            }

            // Right details & action column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 12.dp, bottom = 12.dp, end = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isExpired) colors.secondaryText else colors.primaryText,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!statusLabel.isNullOrBlank()) {
                        SuqiStatusBadge(
                            statusText = statusLabel,
                            type = if (isExpired) StatusType.NEUTRAL else StatusType.SUCCESS
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = conditions,
                    fontFamily = SuqiFontFamily,
                    fontSize = 12.sp,
                    color = colors.secondaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = expiry,
                    fontFamily = SuqiFontFamily,
                    fontSize = 11.sp,
                    color = colors.mutedText
                )
                if (!isExpired) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onCopy,
                            shape = RoundedCornerShape(999.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(LucideIcons.Copy, contentDescription = "Copy code", tint = colors.primaryText, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Copy", fontFamily = SuqiFontFamily, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.primaryText)
                        }
                        Button(
                            onClick = onPrimaryAction,
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(primaryActionLabel, fontFamily = SuqiFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuqiSwipeRow(
    onSwipeLeftDelete: (() -> Unit)? = null,
    onSwipeRightAction: (() -> Unit)? = null,
    rightActionLabel: String = "Read",
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    if (onSwipeLeftDelete != null) {
                        onSwipeLeftDelete()
                        true
                    } else false
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (onSwipeRightAction != null) {
                        onSwipeRightAction()
                        false // Perform action and bounce back smoothly
                    } else false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = onSwipeRightAction != null,
        enableDismissFromEndToStart = onSwipeLeftDelete != null,
        modifier = modifier,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val bgColor = when (direction) {
                SwipeToDismissBoxValue.EndToStart -> SuqiError.copy(alpha = 0.16f)
                SwipeToDismissBoxValue.StartToEnd -> SuqiSuccess.copy(alpha = 0.16f)
                SwipeToDismissBoxValue.Settled -> Color.Transparent
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = when (direction) {
                    SwipeToDismissBoxValue.EndToStart -> Arrangement.End
                    else -> Arrangement.Start
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(bgColor)
                    .padding(horizontal = 20.dp)
            ) {
                if (direction == SwipeToDismissBoxValue.StartToEnd && onSwipeRightAction != null) {
                    Icon(LucideIcons.CheckCheck, contentDescription = rightActionLabel, tint = SuqiSuccess, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(rightActionLabel, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SuqiSuccess)
                } else if (direction == SwipeToDismissBoxValue.EndToStart && onSwipeLeftDelete != null) {
                    Text("Delete", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SuqiError)
                    Spacer(Modifier.width(6.dp))
                    Icon(LucideIcons.Trash2, contentDescription = "Delete", tint = SuqiError, modifier = Modifier.size(20.dp))
                }
            }
        },
        content = { content() }
    )
}

@Composable
fun <T> SuqiDragHandleList(
    items: List<T>,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    itemContent: @Composable (index: Int, item: T, moveUp: (() -> Unit)?, moveDown: (() -> Unit)?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier.fillMaxWidth()) {
        items.forEachIndexed { index, item ->
            val moveUp = if (index > 0) ({ onMove(index, index - 1) }) else null
            val moveDown = if (index < items.lastIndex) ({ onMove(index, index + 1) }) else null
            itemContent(index, item, moveUp, moveDown)
        }
    }
}

private suspend fun importPickerImageOnly(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val mime = resolver.getType(uri)
    val size = try { resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L } catch (_: Exception) { -1L }
    if (size > 15L * 1024 * 1024) error("Image must be under 15 MB")

    val dir = File(context.filesDir, "suqi_uploads").apply { mkdirs() }
    val stamp = System.currentTimeMillis()

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth > 0) {
        var sample = 1
        val maxEdge = 1600
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxEdge * 2) sample *= 2
        var bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        }
        if (bitmap != null) {
            val scale = maxEdge.toFloat() / maxOf(bitmap.width, bitmap.height)
            if (scale < 1f) {
                bitmap = Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
            }
            val orientation = try {
                resolver.openInputStream(uri)?.use {
                    ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                } ?: ExifInterface.ORIENTATION_NORMAL
            } catch (_: Exception) {
                ExifInterface.ORIENTATION_NORMAL
            }
            val degrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (degrees != 0f) {
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degrees) }, true)
            }
            val isPng = mime == "image/png"
            val file = File(dir, "upload_$stamp.${if (isPng) "png" else "jpg"}")
            file.outputStream().use { out ->
                bitmap.compress(if (isPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, 86, out)
            }
            return@withContext Uri.fromFile(file).toString()
        }
    }

    val file = File(dir, "upload_$stamp.jpg")
    resolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } }
        ?: error("Unable to read image")
    Uri.fromFile(file).toString()
}

@Composable
fun SuqiImagePickerTile(
    label: String,
    imageUri: String?,
    onImagePicked: (String) -> Unit,
    modifier: Modifier = Modifier,
    onRemove: (() -> Unit)? = null,
    aspectRatio: Float = 1.4f,
    tileHeight: Dp = 120.dp,
    guidance: String? = null,
    errorMessage: String? = null,
    isLockedVerified: Boolean = false,
    demoSampleUrl: String? = null,
    testTag: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    var isImporting by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    var showDemoLinkRow by remember { mutableStateOf(false) }
    var linkInput by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isImporting = true
            localError = null
            try {
                val imported = importPickerImageOnly(context, uri)
                onImagePicked(imported)
            } catch (e: Exception) {
                localError = e.message ?: "Failed to import photo"
            }
            isImporting = false
        }
    }

    val activeError = errorMessage ?: localError
    val dashColor = when {
        activeError != null -> SuqiError
        isLockedVerified -> SuqiSuccess
        !imageUri.isNullOrBlank() -> SuqiPrimary
        else -> colors.border
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = colors.primaryText
            )
            if (isLockedVerified) {
                SuqiStatusBadge("Verified", StatusType.SUCCESS)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(tileHeight)
                .clip(RoundedCornerShape(18.dp))
                .background(if (!imageUri.isNullOrBlank()) colors.imageBackdrop else colors.secondaryBackground)
                .clickable(enabled = !isLockedVerified && !isImporting) {
                    launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                .let { if (testTag != null) it.testTag(testTag) else it }
        ) {
            if (!imageUri.isNullOrBlank()) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .aspectRatio(aspectRatio, matchHeightConstraintsFirst = true)
                        .clip(RoundedCornerShape(18.dp))
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f))
                )
                if (!isLockedVerified) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color.Black.copy(alpha = 0.7f))
                                .clickable {
                                    launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Retake", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        if (onRemove != null) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(SuqiError)
                                    .clickable { onRemove() }
                            ) {
                                Icon(LucideIcons.Trash2, contentDescription = "Remove image", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRoundRect(
                        color = dashColor,
                        cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
                        style = Stroke(
                            width = 1.6.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
                        )
                    )
                }
                if (isImporting) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = SuqiPrimary, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                        Spacer(Modifier.height(6.dp))
                        Text("Processing...", fontSize = 12.sp, color = colors.secondaryText)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(colors.primaryTint)
                        ) {
                            Icon(LucideIcons.Camera, contentDescription = "Upload $label", tint = SuqiPrimary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Tap to pick or upload photo",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = colors.primaryText
                        )
                    }
                }
            }
        }

        if (BackendConfig.DemoMode && !isLockedVerified && imageUri.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!demoSampleUrl.isNullOrBlank()) {
                    TextButton(
                        onClick = { onImagePicked(demoSampleUrl) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Use sample photo", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SuqiPrimary)
                    }
                }
                TextButton(
                    onClick = { showDemoLinkRow = !showDemoLinkRow },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(if (showDemoLinkRow) "Hide URL" else "Paste URL", fontSize = 11.sp, color = colors.secondaryText)
                }
            }
            if (showDemoLinkRow) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    SuqiTextField(
                        value = linkInput,
                        onValueChange = { linkInput = it },
                        label = "",
                        placeholder = "https://...",
                        modifier = Modifier.weight(1f)
                    )
                    SuqiButton(
                        text = "Set",
                        onClick = {
                            if (linkInput.isNotBlank()) {
                                onImagePicked(linkInput.trim())
                                showDemoLinkRow = false
                            }
                        },
                        modifier = Modifier.width(72.dp)
                    )
                }
            }
        }

        if (!activeError.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(activeError, fontFamily = SuqiFontFamily, fontSize = 11.sp, color = SuqiError)
        } else if (!guidance.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(guidance, fontFamily = SuqiFontFamily, fontSize = 11.sp, color = colors.secondaryText)
        }
    }
}

data class SuqiDocumentPage(
    val title: String,
    val imageUrl: String,
    val note: String = ""
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SuqiDocumentViewer(
    documents: List<SuqiDocumentPage>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit
) {
    if (documents.isEmpty()) return
    val pagerState = rememberPagerState(initialPage = initialIndex.coerceIn(0, documents.lastIndex)) { documents.size }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                val doc = documents[pageIndex]
                var scale by remember(pageIndex) { mutableFloatStateOf(1f) }
                var offset by remember(pageIndex) { mutableStateOf(Offset.Zero) }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(pageIndex) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (scale > 1.2f) {
                                        scale = 1f
                                        offset = Offset.Zero
                                    } else {
                                        scale = 2.4f
                                    }
                                }
                            )
                        }
                        .pointerInput(pageIndex) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val nextScale = (scale * zoom).coerceIn(1f, 4f)
                                scale = nextScale
                                offset = if (nextScale > 1.05f) offset + pan else Offset.Zero
                            }
                        }
                ) {
                    AsyncImage(
                        model = doc.imageUrl,
                        contentDescription = doc.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                    )
                }
            }

            // Header bar
            val currentDoc = documents[pagerState.currentPage]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentDoc.title,
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    if (currentDoc.note.isNotBlank()) {
                        Text(
                            text = currentDoc.note,
                            fontFamily = SuqiFontFamily,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                    }
                }
                Text(
                    text = "${pagerState.currentPage + 1} / ${documents.size}",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = SuqiPrimary,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                ) {
                    Icon(LucideIcons.X, contentDescription = "Close viewer", tint = Color.White)
                }
            }

            Text(
                text = "Pinch or double-tap to zoom · Swipe between documents",
                fontFamily = SuqiFontFamily,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.72f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 18.dp)
            )
        }
    }
}

@Composable
fun SuqiQrCard(
    title: String,
    subtitle: String,
    codeValue: String,
    onCopyCode: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String? = null
) {
    val context = LocalContext.current
    val colors = LocalSuqiColors.current
    SuqiSectionCard(modifier = modifier, padding = 18.dp) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            if (!badgeText.isNullOrBlank()) {
                SuqiStatusBadge(badgeText, StatusType.PRIMARY)
                Spacer(Modifier.height(8.dp))
            }
            Text(
                text = title,
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = colors.primaryText,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                fontFamily = SuqiFontFamily,
                fontSize = 12.sp,
                color = colors.secondaryText,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))

            // Deterministic 21x21 QR matrix visual
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(164.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                    .padding(14.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val grid = 21
                    val cell = size.width / grid
                    val seed = codeValue.hashCode()
                    fun isFinder(r: Int, c: Int): Boolean {
                        return (r < 7 && c < 7) || (r < 7 && c >= grid - 7) || (r >= grid - 7 && c < 7)
                    }
                    fun finderFill(r: Int, c: Int): Boolean {
                        val lr = if (r >= grid - 7) r - (grid - 7) else r
                        val lc = if (c >= grid - 7) c - (grid - 7) else c
                        return lr == 0 || lr == 6 || lc == 0 || lc == 6 || (lr in 2..4 && lc in 2..4)
                    }
                    for (r in 0 until grid) {
                        for (c in 0 until grid) {
                            val drawCell = if (isFinder(r, c)) {
                                finderFill(r, c)
                            } else {
                                val h = abs(seed * 31 + r * 17 + c * 13 + (r xor c) * 7)
                                h % 11 < 5
                            }
                            if (drawCell) {
                                val isCenterFinder = (r in 2..4 && c in 2..4) ||
                                    (r in 2..4 && c in (grid - 5)..(grid - 3)) ||
                                    (r in (grid - 5)..(grid - 3) && c in 2..4)
                                drawRect(
                                    color = if (isCenterFinder) SuqiPrimary else Color(0xFF171717),
                                    topLeft = Offset(c * cell, r * cell),
                                    size = Size(cell * 0.92f, cell * 0.92f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = codeValue,
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = colors.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        cm?.setPrimaryClip(ClipData.newPlainText("Suqi", codeValue))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        onCopyCode()
                    },
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Icon(LucideIcons.Copy, contentDescription = "Copy", tint = colors.primaryText, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copy Link", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.primaryText)
                }
                Button(
                    onClick = onShare,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Icon(LucideIcons.Share2, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Share QR", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                }
            }
        }
    }
}
