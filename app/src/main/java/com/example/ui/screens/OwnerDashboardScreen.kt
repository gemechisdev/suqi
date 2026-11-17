package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OwnerDashboardMetricsDto
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.StatusType
import com.example.ui.components.SuqiBarChart
import com.example.ui.components.SuqiBottomSheet
import com.example.ui.components.SuqiChartSegment
import com.example.ui.components.SuqiDonutChart
import com.example.ui.components.SuqiLineChart
import com.example.ui.components.SuqiListRow
import com.example.ui.components.SuqiScreen
import com.example.ui.components.SuqiSectionCard
import com.example.ui.components.SuqiSkeletonList
import com.example.ui.components.SuqiSparkline
import com.example.ui.components.SuqiStatCard
import com.example.ui.components.SuqiStatusBadge
import com.example.ui.components.formatEtb
import com.example.ui.i18n.suqiStrings
import com.example.ui.theme.LocalSuqiColors
import com.example.ui.theme.LucideIcons
import com.example.ui.theme.SuqiError
import com.example.ui.theme.SuqiFontFamily
import com.example.ui.theme.SuqiPrimary
import com.example.ui.theme.SuqiSuccess
import com.example.ui.theme.SuqiWarning
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun OwnerDashboardScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val isOffline by viewModel.isOffline.collectAsState()

    var selectedRangeDays by rememberSaveable { mutableIntStateOf(30) }
    var metrics by remember(selectedRangeDays) { mutableStateOf(viewModel.cachedOwnerMetrics[selectedRangeDays]) }
    var isLoading by remember(selectedRangeDays) { mutableStateOf(metrics == null) }
    var showExportSheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    BackHandler(enabled = showExportSheet) {
        showExportSheet = false
    }

    val rangeOptions = listOf(
        7 to strings.adminGov.range7d,
        30 to strings.adminGov.range30d,
        90 to strings.adminGov.range90d,
        365 to strings.adminGov.rangeAll
    )

    fun loadMetrics(days: Int) {
        coroutineScope.launch {
            isLoading = viewModel.cachedOwnerMetrics[days] == null
            val res = viewModel.repository.getOwnerDashboardMetrics(days)
            if (res.success && res.data != null) {
                metrics = res.data
                viewModel.cachedOwnerMetrics[days] = res.data
            }
            isLoading = false
        }
    }

    LaunchedEffect(selectedRangeDays) {
        loadMetrics(selectedRangeDays)
    }

    val rangeLabel = rangeOptions.firstOrNull { it.first == selectedRangeDays }?.second ?: strings.adminGov.range30d

    SuqiScreen(
        title = strings.admin.ownerDashboardTitle,
        subtitle = rangeLabel,
        onBackClick = { viewModel.goBack() },
        headerActions = {
            IconButton(
                onClick = { showExportSheet = true },
                enabled = metrics != null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colors.primaryTint)
                    .testTag("owner_export_btn")
            ) {
                Icon(
                    imageVector = LucideIcons.Download,
                    contentDescription = strings.adminGov.exportCsvPdfBtn,
                    tint = SuqiPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        if (isLoading && metrics == null) {
            SuqiSkeletonList(count = 5, modifier = Modifier.padding(paddingValues).padding(top = 12.dp))
        } else {
            val data = metrics ?: OwnerDashboardMetricsDto(rangeDays = selectedRangeDays)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Range Filter Chips
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        rangeOptions.forEach { (days, label) ->
                            val selected = days == selectedRangeDays
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (selected) SuqiPrimary else colors.cardBackground)
                                    .border(1.dp, if (selected) SuqiPrimary else colors.border, RoundedCornerShape(999.dp))
                                    .clickable { selectedRangeDays = days }
                                    .padding(horizontal = 16.dp)
                                    .testTag("owner_range_$days")
                            ) {
                                Text(
                                    text = label,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = if (selected) Color.White else colors.primaryText
                                )
                            }
                        }
                    }
                }

                // 6 KPI Cards (2 cols x 3 rows)
                item {
                    val gmvDeltaPct = if (data.previousGmv > 0.0) {
                        (((data.totalGmv - data.previousGmv) / data.previousGmv) * 100.0).roundToInt()
                    } else 0
                    val trendText = when {
                        gmvDeltaPct > 0 -> "+$gmvDeltaPct%"
                        gmvDeltaPct < 0 -> "$gmvDeltaPct%"
                        else -> null
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            SuqiStatCard(
                                label = strings.adminGov.gmvLabel,
                                value = formatEtb(data.totalGmv),
                                icon = LucideIcons.Banknote,
                                tint = SuqiPrimary,
                                trendText = trendText,
                                trendPositive = gmvDeltaPct >= 0,
                                testTag = "owner_kpi_gmv",
                                modifier = Modifier.weight(1f)
                            )
                            SuqiStatCard(
                                label = strings.adminGov.platformRevenueLabel,
                                value = formatEtb(data.platformRevenue),
                                icon = LucideIcons.TrendingUp,
                                tint = SuqiSuccess,
                                testTag = "owner_kpi_revenue",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            SuqiStatCard(
                                label = strings.adminGov.totalOrdersLabel,
                                value = data.totalOrders.toString(),
                                icon = LucideIcons.Package,
                                tint = Color(0xFF2F6FE4),
                                testTag = "owner_kpi_orders",
                                modifier = Modifier.weight(1f)
                            )
                            SuqiStatCard(
                                label = strings.adminGov.activeShopsLabel,
                                value = data.activeShops.toString(),
                                icon = LucideIcons.Store,
                                tint = SuqiPrimary,
                                testTag = "owner_kpi_shops",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            SuqiStatCard(
                                label = strings.adminGov.newUsersLabel,
                                value = data.newUsersCount.toString(),
                                icon = LucideIcons.Users,
                                tint = SuqiSuccess,
                                testTag = "owner_kpi_users",
                                modifier = Modifier.weight(1f)
                            )
                            SuqiStatCard(
                                label = strings.adminGov.conversionRateLabel,
                                value = String.format(Locale.US, "%.1f%%", data.conversionRate),
                                icon = LucideIcons.Percent,
                                tint = SuqiWarning,
                                testTag = "owner_kpi_conversion",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // GMV Trend Chart with dashed Previous Period overlay
                if (data.gmvValues.isNotEmpty()) {
                    item {
                        SuqiSectionCard(padding = 16.dp) {
                            Text(
                                text = strings.adminGov.gmvLabel,
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = colors.primaryText
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(SuqiPrimary))
                                    Text(strings.adminGov.currentPeriodLegend, fontFamily = SuqiFontFamily, fontSize = 11.sp, color = colors.secondaryText)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(colors.mutedText))
                                    Text(strings.adminGov.previousPeriodLegend, fontFamily = SuqiFontFamily, fontSize = 11.sp, color = colors.secondaryText)
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            SuqiLineChart(
                                labels = data.gmvLabels,
                                values = data.gmvValues,
                                lineColor = SuqiPrimary,
                                previousValues = data.previousGmvValues,
                                previousLineColor = colors.mutedText
                            )
                        }
                    }
                }

                // Orders by Status Bar Chart
                if (data.statusCounts.isNotEmpty()) {
                    item {
                        SuqiSectionCard(padding = 16.dp) {
                            Text(
                                text = strings.adminGov.ordersByStatusTitle,
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = colors.primaryText
                            )
                            Spacer(Modifier.height(10.dp))
                            SuqiBarChart(
                                labels = data.statusLabels,
                                values = data.statusCounts,
                                barColor = Color(0xFF2F6FE4)
                            )
                        }
                    }
                }

                // New Users vs New Shops Comparison
                if (data.growthLabels.isNotEmpty()) {
                    item {
                        SuqiSectionCard(padding = 16.dp) {
                            Text(
                                text = strings.adminGov.usersVsShopsTitle,
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = colors.primaryText
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${strings.adminGov.newUsersLabel} (${data.newUsersCount})",
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        color = SuqiPrimary
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    SuqiBarChart(
                                        labels = data.growthLabels,
                                        values = data.newUsersSeries,
                                        barColor = SuqiPrimary
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${strings.adminGov.activeShopsLabel} (+${data.newShopsCount})",
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        color = SuqiSuccess
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    SuqiBarChart(
                                        labels = data.growthLabels,
                                        values = data.newShopsSeries,
                                        barColor = SuqiSuccess
                                    )
                                }
                            }
                        }
                    }
                }

                // Top Categories Donut Chart
                if (data.topCategories.isNotEmpty()) {
                    item {
                        val palette = listOf(
                            SuqiPrimary,
                            SuqiSuccess,
                            Color(0xFF2F6FE4),
                            SuqiWarning,
                            Color(0xFF7845E8)
                        )
                        val segments = data.topCategories.mapIndexed { idx, slice ->
                            SuqiChartSegment(
                                label = slice.label,
                                value = slice.value.toFloat(),
                                color = palette[idx % palette.size]
                            )
                        }
                        SuqiSectionCard(padding = 16.dp) {
                            Text(
                                text = strings.adminGov.topCategoriesTitle,
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = colors.primaryText
                            )
                            Spacer(Modifier.height(12.dp))
                            SuqiDonutChart(
                                segments = segments,
                                centerValue = formatEtb(data.totalGmv),
                                centerLabel = strings.adminGov.gmvLabel
                            )
                        }
                    }
                }

                // Top Shops Ranked List with Sparkline
                if (data.topShops.isNotEmpty()) {
                    item {
                        SuqiSectionCard(padding = 16.dp) {
                            Text(
                                text = strings.adminGov.topShopsTitle,
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = colors.primaryText
                            )
                            Spacer(Modifier.height(10.dp))
                            data.topShops.forEachIndexed { idx, shop ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 56.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { viewModel.navigate(ScreenDestination.ShopStorefront(shop.shopId)) }
                                        .padding(vertical = 8.dp, horizontal = 4.dp)
                                        .testTag("owner_top_shop_${shop.shopId}")
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(if (idx == 0) SuqiPrimary else colors.secondaryBackground)
                                    ) {
                                        Text(
                                            text = "${idx + 1}",
                                            fontFamily = SuqiFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (idx == 0) Color.White else colors.primaryText
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = shop.shopName,
                                                fontFamily = SuqiFontFamily,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp,
                                                color = colors.primaryText,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (shop.isVerified) {
                                                Icon(
                                                    imageVector = LucideIcons.BadgeCheck,
                                                    contentDescription = strings.verifiedLabel,
                                                    tint = SuqiPrimary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${shop.ordersCount} ${strings.adminGov.totalOrdersLabel.lowercase()} • ${formatEtb(shop.gmv)}",
                                            fontFamily = SuqiFontFamily,
                                            fontSize = 12.sp,
                                            color = colors.secondaryText
                                        )
                                    }
                                    if (shop.sparkline.isNotEmpty()) {
                                        SuqiSparkline(
                                            values = shop.sparkline,
                                            lineColor = SuqiPrimary
                                        )
                                    }
                                }
                                if (idx < data.topShops.lastIndex) {
                                    HorizontalDivider(color = colors.border)
                                }
                            }
                        }
                    }
                }

                // Alerts & Attention Needed Card
                item {
                    SuqiSectionCard(padding = 16.dp) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = strings.adminGov.alertsAttentionTitle,
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = colors.primaryText
                            )
                            val totalAlerts = data.verificationsWaitingOver24h +
                                data.flaggedOrStalePaymentsCount +
                                data.openTicketsAndDisputesCount
                            SuqiStatusBadge(
                                statusText = if (totalAlerts == 0) strings.admin.clearBadge else totalAlerts.toString(),
                                type = if (totalAlerts == 0) StatusType.SUCCESS else StatusType.WARNING
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        SuqiListRow(
                            icon = LucideIcons.ShieldCheck,
                            title = strings.adminGov.verifOver24hAlert,
                            badge = data.verificationsWaitingOver24h.toString(),
                            badgeColor = if (data.verificationsWaitingOver24h > 0) SuqiWarning else SuqiSuccess,
                            onClick = { viewModel.navigate(ScreenDestination.AdminFeatures) },
                            testTag = "owner_alert_verif"
                        )
                        HorizontalDivider(color = colors.border)
                        SuqiListRow(
                            icon = LucideIcons.Clock,
                            title = strings.adminGov.flaggedPaymentsAlert,
                            badge = data.flaggedOrStalePaymentsCount.toString(),
                            badgeColor = if (data.flaggedOrStalePaymentsCount > 0) SuqiError else SuqiSuccess,
                            onClick = { viewModel.openAdminConsoleTab("queues") },
                            testTag = "owner_alert_payments"
                        )
                        HorizontalDivider(color = colors.border)
                        SuqiListRow(
                            icon = LucideIcons.Headphones,
                            title = strings.adminGov.openDisputesAlert,
                            badge = data.openTicketsAndDisputesCount.toString(),
                            badgeColor = if (data.openTicketsAndDisputesCount > 0) SuqiWarning else SuqiSuccess,
                            onClick = { viewModel.openAdminConsoleTab("queues") },
                            testTag = "owner_alert_disputes"
                        )
                    }
                }
            }
        }
    }

    if (showExportSheet && metrics != null) {
        val csv = remember(metrics, selectedRangeDays) { buildOwnerCsvReport(metrics!!) }
        SuqiBottomSheet(
            onDismissRequest = { showExportSheet = false },
            title = strings.adminGov.exportCsvPdfBtn
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.secondaryBackground)
                        .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = csv,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = colors.primaryText
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            cm?.setPrimaryClip(ClipData.newPlainText("Suqi Owner Report CSV", csv))
                            viewModel.showToast(strings.adminGov.csvCopiedToast)
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("owner_copy_csv_btn")
                    ) {
                        Icon(LucideIcons.Copy, contentDescription = null, tint = colors.primaryText, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.adminGov.copyCsvBtn, color = colors.primaryText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            try {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_SUBJECT, "Suqi Marketplace Report (${rangeLabel})")
                                    putExtra(Intent.EXTRA_TEXT, csv)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, strings.adminGov.shareReportBtn).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            } catch (_: Exception) {
                                viewModel.showToast(strings.adminGov.csvCopiedToast)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("owner_share_csv_btn")
                    ) {
                        Icon(LucideIcons.Share2, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.adminGov.shareReportBtn, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

private fun buildOwnerCsvReport(metrics: OwnerDashboardMetricsDto): String {
    val sb = StringBuilder()
    sb.appendLine("Suqi Marketplace Owner Report,Range Days,${metrics.rangeDays}")
    sb.appendLine("Metric,Value")
    sb.appendLine("Total GMV (ETB),${metrics.totalGmv.roundToInt()}")
    sb.appendLine("Previous Period GMV (ETB),${metrics.previousGmv.roundToInt()}")
    sb.appendLine("Net Revenue (ETB),${metrics.platformRevenue.roundToInt()}")
    sb.appendLine("Total Orders,${metrics.totalOrders}")
    sb.appendLine("Active Shops,${metrics.activeShops}")
    sb.appendLine("New Users,${metrics.newUsersCount}")
    sb.appendLine("Conversion Rate (%),${String.format(Locale.US, "%.1f", metrics.conversionRate)}")
    sb.appendLine()
    sb.appendLine("Period,Current GMV,Previous GMV")
    metrics.gmvLabels.forEachIndexed { idx, label ->
        val curr = metrics.gmvValues.getOrNull(idx)?.roundToInt() ?: 0
        val prev = metrics.previousGmvValues.getOrNull(idx)?.roundToInt() ?: 0
        sb.appendLine("${label.replace(",", " ")},$curr,$prev")
    }
    sb.appendLine()
    sb.appendLine("Top Shops,Orders,GMV (ETB),Verified")
    metrics.topShops.forEach { s ->
        sb.appendLine("${s.shopName.replace(",", " ")},${s.ordersCount},${s.gmv.roundToInt()},${s.isVerified}")
    }
    return sb.toString()
}
