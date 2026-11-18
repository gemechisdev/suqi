package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.SellerAnalyticsDto
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.*
import com.example.ui.i18n.suqiStrings
import com.example.ui.theme.*
import com.example.util.EthiopianMarketplaceUtils
import kotlinx.coroutines.launch

@Composable
fun SellerAnalyticsScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val p5 = strings.phase5
    val isOffline by viewModel.isOffline.collectAsState()

    var rangeDays by rememberSaveable { mutableIntStateOf(7) }
    var analytics by remember { mutableStateOf<SellerAnalyticsDto?>(viewModel.cachedSellerAnalytics[rangeDays]) }
    var isLoading by remember { mutableStateOf(analytics == null) }

    fun loadAnalytics(days: Int) {
        coroutineScope.launch {
            analytics = viewModel.cachedSellerAnalytics[days]
            isLoading = analytics == null
            val res = viewModel.repository.getSellerAnalytics(days)
            if (res.success && res.data != null) {
                analytics = res.data
                viewModel.cachedSellerAnalytics[days] = res.data
            }
            isLoading = false
        }
    }

    LaunchedEffect(rangeDays) {
        loadAnalytics(rangeDays)
    }

    fun buildCsv(dto: SellerAnalyticsDto): String = buildString {
        appendLine("Metric,Value")
        appendLine("RangeDays,${dto.rangeDays}")
        appendLine("TotalRevenue,${dto.totalRevenue}")
        appendLine("TotalOrders,${dto.totalOrders}")
        appendLine("CompletedOrders,${dto.completedOrders}")
        appendLine("ProductViews,${dto.productViews}")
        appendLine("CartAdds,${dto.cartAdds}")
        appendLine()
        appendLine("Period,Revenue")
        dto.revenueLabels.forEachIndexed { idx, lbl ->
            appendLine("\"$lbl\",${dto.revenueValues.getOrElse(idx) { 0.0 }}")
        }
        appendLine()
        appendLine("TopProduct,Revenue,UnitsSold")
        dto.topProducts.forEach { prod ->
            appendLine("\"${prod.productName}\",${prod.revenue},${prod.unitsSold}")
        }
        appendLine()
        appendLine("SubCity,Customers,Orders,Revenue")
        dto.customerAreas.forEach { area ->
            appendLine("\"${area.subCity}\",${area.customersCount},${area.ordersCount},${area.revenue}")
        }
    }

    val current = analytics

    SuqiScreen(
        title = strings.seller.analyticsTitle,
        subtitle = current?.let { "${it.rangeDays}D • ${formatEtb(it.totalRevenue)}" },
        onBackClick = { viewModel.goBack() },
        headerActions = {
            if (current != null && !current.isProLocked) {
                IconButton(
                    onClick = {
                        val csv = buildCsv(current)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Seller Analytics CSV", csv))
                        viewModel.showToast(strings.common.copiedToast)
                        runCatching {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_SUBJECT, "${strings.seller.analyticsTitle} (${rangeDays}D)")
                                putExtra(Intent.EXTRA_TEXT, csv)
                            }
                            context.startActivity(Intent.createChooser(intent, strings.seller.analyticsTitle))
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("analytics_export_csv_btn")
                ) {
                    Icon(LucideIcons.Download, contentDescription = strings.adminGov.exportCsvPdfBtn, tint = SuqiPrimary)
                }
            }
        },
        isOffline = isOffline,
        onRetryOffline = {
            viewModel.setOfflineMode(false)
            loadAnalytics(rangeDays)
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                listOf(7, 30, 90).forEach { days ->
                    val selected = rangeDays == days
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (selected) SuqiPrimary else colors.secondaryBackground)
                            .border(1.dp, if (selected) SuqiPrimary else colors.border, RoundedCornerShape(999.dp))
                            .clickable { rangeDays = days }
                            .testTag("analytics_range_$days")
                    ) {
                        Text(
                            text = "${days}D",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (selected) Color.White else colors.primaryText
                        )
                    }
                }
            }

            if (isLoading && current == null) {
                SuqiSkeletonList(count = 4, modifier = Modifier.padding(top = 8.dp))
            } else if (current == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    SuqiEmptyState(
                        icon = LucideIcons.BarChart3,
                        title = p5.emptyAnalyticsTitle,
                        subtitle = p5.emptyAnalyticsSub,
                        ctaText = p5.shareShopCta,
                        onCtaClick = { viewModel.navigate(ScreenDestination.ShopSettings) }
                    )
                }
            } else if (current.isProLocked) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        SuqiSectionCard(
                            containerColor = colors.primaryTint,
                            modifier = Modifier.testTag("analytics_pro_lock_card")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(SuqiPrimary)
                                ) {
                                    Icon(LucideIcons.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                                }
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = p5.proAnalyticsLockedTitle,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = colors.primaryText
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = p5.proAnalyticsLockedSub,
                                    fontFamily = SuqiFontFamily,
                                    fontSize = 13.sp,
                                    color = colors.secondaryText
                                )
                                Spacer(Modifier.height(14.dp))
                                SuqiButton(
                                    text = p5.upgradePlanBtn,
                                    icon = LucideIcons.Sparkles,
                                    onClick = { viewModel.navigate(ScreenDestination.SellerSubscription) },
                                    modifier = Modifier.fillMaxWidth(),
                                    testTag = "analytics_upgrade_pro_btn"
                                )
                            }
                        }
                    }
                }
            } else if (current.totalOrders == 0 && current.totalRevenue <= 0.0) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    SuqiEmptyState(
                        icon = LucideIcons.BarChart3,
                        title = p5.emptyAnalyticsTitle,
                        subtitle = p5.emptyAnalyticsSub,
                        ctaText = p5.shareShopCta,
                        onCtaClick = { viewModel.navigate(ScreenDestination.ShopSettings) },
                        testTag = "analytics_empty_state"
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SuqiStatCard(
                                label = strings.sellerOps.todayRevenueLabel,
                                value = formatEtb(current.totalRevenue),
                                icon = LucideIcons.TrendingUp,
                                tint = SuqiSuccess,
                                trendPositive = true,
                                modifier = Modifier.weight(1f),
                                testTag = "analytics_kpi_revenue"
                            )
                            SuqiStatCard(
                                label = strings.sellerOps.activeOrdersLabel,
                                value = "${current.completedOrders}/${current.totalOrders}",
                                icon = LucideIcons.ShoppingBag,
                                tint = SuqiPrimary,
                                trendPositive = true,
                                modifier = Modifier.weight(1f),
                                testTag = "analytics_kpi_orders"
                            )
                        }
                    }

                    if (current.revenueValues.isNotEmpty()) {
                        item {
                            SuqiSectionCard {
                                Text(
                                    text = strings.seller.analyticsTitle,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = colors.primaryText
                                )
                                Spacer(Modifier.height(10.dp))
                                SuqiLineChart(
                                    labels = current.revenueLabels,
                                    values = current.revenueValues
                                )
                            }
                        }
                    }

                    if (current.statusCounts.isNotEmpty()) {
                        item {
                            SuqiSectionCard {
                                Text(
                                    text = strings.seller.ordersTabTitle,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = colors.primaryText
                                )
                                Spacer(Modifier.height(10.dp))
                                SuqiBarChart(
                                    labels = current.statusLabels,
                                    values = current.statusCounts
                                )
                            }
                        }
                    }

                    item {
                        SuqiSectionCard {
                            Text(
                                text = p5.funnelTitle,
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = colors.primaryText
                            )
                            Spacer(Modifier.height(12.dp))
                            val views = current.productViews.coerceAtLeast(1)
                            val addsPct = ((current.cartAdds * 100.0) / views).toInt().coerceIn(0, 100)
                            val compPct = ((current.completedOrders * 100.0) / current.cartAdds.coerceAtLeast(1)).toInt().coerceIn(0, 100)
                            SuqiFunnelBars(
                                stages = listOf(
                                    SuqiFunnelStage(label = p5.funnelViews, count = current.productViews),
                                    SuqiFunnelStage(label = p5.funnelCartAdds, count = current.cartAdds, conversionText = "$addsPct%"),
                                    SuqiFunnelStage(label = p5.funnelCompleted, count = current.completedOrders, conversionText = "$compPct%")
                                )
                            )
                        }
                    }

                    if (current.topProducts.isNotEmpty()) {
                        item {
                            SuqiSectionCard {
                                Text(
                                    text = p5.topProductsSection,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = colors.primaryText
                                )
                                Spacer(Modifier.height(10.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    current.topProducts.forEach { prod ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            AsyncImage(
                                                model = prod.imageUrl,
                                                contentDescription = prod.productName,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(colors.secondaryBackground)
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = prod.productName,
                                                    fontFamily = SuqiFontFamily,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp,
                                                    color = colors.primaryText,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${formatEtb(prod.revenue)} • ${String.format(strings.checkout.itemsSummaryTemplate, prod.unitsSold)}",
                                                    fontFamily = SuqiFontFamily,
                                                    fontSize = 11.sp,
                                                    color = colors.secondaryText
                                                )
                                            }
                                            SuqiSparkline(values = prod.sparkline)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (current.customerAreas.isNotEmpty()) {
                        val maxAreaRev = (current.customerAreas.maxOfOrNull { it.revenue } ?: 1.0).coerceAtLeast(1.0)
                        item {
                            SuqiSectionCard {
                                Text(
                                    text = p5.customerLocationsSection,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = colors.primaryText
                                )
                                Spacer(Modifier.height(10.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    current.customerAreas.forEach { area ->
                                        val pct = (area.revenue / maxAreaRev).toFloat().coerceIn(0.08f, 1f)
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = com.example.ui.i18n.placeLabel(area.subCity),
                                                    fontFamily = SuqiFontFamily,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp,
                                                    color = colors.primaryText
                                                )
                                                Text(
                                                    text = "${area.ordersCount} • ${formatEtb(area.revenue)}",
                                                    fontFamily = SuqiFontFamily,
                                                    fontSize = 12.sp,
                                                    color = colors.secondaryText
                                                )
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                progress = { pct },
                                                color = SuqiPrimary,
                                                trackColor = colors.secondaryBackground,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp)
                                                    .clip(RoundedCornerShape(999.dp))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (current.paymentSplit.isNotEmpty()) {
                        val palette = listOf(SuqiPrimary, SuqiSuccess, SuqiWarning, Color(0xFF6366F1))
                        item {
                            SuqiSectionCard {
                                Text(
                                    text = p5.paymentMethodsSplitSection,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = colors.primaryText
                                )
                                Spacer(Modifier.height(12.dp))
                                SuqiDonutChart(
                                    segments = current.paymentSplit.mapIndexed { idx, slice ->
                                        SuqiChartSegment(
                                            label = slice.label,
                                            value = slice.value.toFloat().coerceAtLeast(0.1f),
                                            color = palette[idx % palette.size]
                                        )
                                    },
                                    centerValue = current.totalOrders.toString(),
                                    centerLabel = strings.seller.ordersTabTitle
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
