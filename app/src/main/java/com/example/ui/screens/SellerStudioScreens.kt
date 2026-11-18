package com.example.ui.screens

import com.example.ui.i18n.localizeDeliveryTitle
import com.example.util.applyOnMain
import com.example.ui.i18n.localizeEta
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.SellerNotificationPrefs
import com.example.data.local.SellerPreferencesStore
import com.example.data.model.*
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.*
import com.example.ui.i18n.LocalSuqiLanguage
import com.example.ui.i18n.localizedName
import com.example.ui.i18n.suqiStrings
import com.example.ui.theme.*
import com.example.util.EthiopianMarketplaceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

internal suspend fun resolveActiveSellerShopId(viewModel: SuqiViewModel): String {
    val dashRes = viewModel.repository.getSellerDashboard()
    val fromDash = dashRes.data?.get("shopId") as? String
    if (!fromDash.isNullOrBlank()) return fromDash
    val user = viewModel.currentUser.value
    if (!user?.shopId.isNullOrBlank()) return user!!.shopId!!
    val shopsRes = viewModel.repository.getShops()
    val owned = shopsRes.data?.firstOrNull { it.ownerUserId == user?.id }
    return owned?.id.orEmpty()
}

@Composable
fun SellerStudioScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val isOffline by viewModel.isOffline.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var dashboardData by remember { mutableStateOf(viewModel.cachedSellerDashboard) }
    var pendingQueue by remember { mutableStateOf(viewModel.cachedSellerQueue) }
    var sellerOrders by remember { mutableStateOf(viewModel.cachedSellerOrders) }
    var sellerProducts by remember { mutableStateOf(viewModel.cachedSellerProducts) }
    var isLoading by remember {
        mutableStateOf(dashboardData.isEmpty() && sellerOrders.isEmpty() && sellerProducts.isEmpty())
    }
    var selectedPeriod by rememberSaveable { mutableStateOf("7D") }
    val listState = rememberLazyListState()

    fun loadDashboard() {
        coroutineScope.launch {
            isLoading = dashboardData.isEmpty() && sellerOrders.isEmpty() && sellerProducts.isEmpty()
            val dashRes = viewModel.repository.getSellerDashboard()
            val qRes = viewModel.repository.getSellerVerificationQueue()
            val ordersRes = viewModel.repository.getSellerOrders()
            val prodRes = viewModel.repository.getSellerProducts()
            applyOnMain {
                if (dashRes.success && dashRes.data != null) {
                    dashboardData = dashRes.data
                    viewModel.cachedSellerDashboard = dashRes.data
                }
                if (qRes.success && qRes.data != null) {
                    pendingQueue = qRes.data
                    viewModel.cachedSellerQueue = qRes.data
                }
                if (ordersRes.success && ordersRes.data != null) {
                    sellerOrders = ordersRes.data
                    viewModel.cachedSellerOrders = ordersRes.data
                }
                if (prodRes.success && prodRes.data != null) {
                    sellerProducts = prodRes.data
                    viewModel.cachedSellerProducts = prodRes.data
                }
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadDashboard()
    }

    val shopName = (dashboardData["shopName"] as? String).orEmpty().ifBlank { strings.sellerOps.mySuqiShopFallback }
    val pendingCount = pendingQueue.size
    val totalSales = (dashboardData["totalSales"] as? Number)?.toDouble()
        ?: sellerOrders.filter { it.paymentStatus == "VERIFIED" }.sumOf { it.totalAmount }
    val verifiedCount = sellerOrders.count { it.paymentStatus == "VERIFIED" }
    val pendingOrdersCount = (dashboardData["pendingOrders"] as? Number)?.toInt() ?: pendingCount
    val lowStockCount = (dashboardData["lowStock"] as? Number)?.toInt()
        ?: sellerProducts.count { it.stockQuantity in 1..5 }
    val activeProductCount = (dashboardData["productCount"] as? Number)?.toInt()
        ?: sellerProducts.count { it.status == "ACTIVE" }
    val totalProductCount = (dashboardData["totalProductCount"] as? Number)?.toInt()
        ?: sellerProducts.size
    val hasPaymentMethod = (dashboardData["hasPaymentMethod"] as? Boolean) ?: false
    val hasDeliveryOption = (dashboardData["hasDeliveryOption"] as? Boolean) ?: false
    val isVerified = (dashboardData["isVerified"] as? Boolean) ?: false
    val verificationStatus = (dashboardData["verificationStatus"] as? String) ?: "NOT_SUBMITTED"

    @Suppress("UNCHECKED_CAST")
    val baseChart7d = (dashboardData["chart7d"] as? List<Number>)?.map { it.toDouble() }
        ?: List(7) { 0.0 }

    val chartLabels = when (selectedPeriod) {
        "30D" -> listOf("W1", "W2", "W3", "W4")
        "90D" -> listOf("M1", "M2", "M3")
        else -> listOf("D-6", "D-5", "D-4", "D-3", "D-2", "D-1", strings.buyer.todayLabel)
    }
    val chartValues = when (selectedPeriod) {
        "30D" -> listOf(
            baseChart7d.take(2).sum(),
            baseChart7d.drop(2).take(2).sum(),
            baseChart7d.drop(4).take(2).sum(),
            baseChart7d.lastOrNull() ?: 0.0
        )
        "90D" -> listOf(
            baseChart7d.take(2).sum(),
            baseChart7d.drop(2).take(3).sum(),
            baseChart7d.drop(5).sum()
        )
        else -> baseChart7d
    }

    val statusLabels = listOf(
        strings.checkout.filterPending,
        strings.checkout.filterConfirmed,
        strings.checkout.statusDispatched,
        strings.checkout.filterDelivered
    )
    val statusValues = listOf(
        sellerOrders.count { it.paymentStatus == "PENDING_VERIFICATION" }.toDouble(),
        sellerOrders.count { it.orderStatus in listOf("Confirmed", "Processing") }.toDouble(),
        sellerOrders.count { it.orderStatus in listOf("Dispatched", "Out for Delivery") }.toDouble(),
        sellerOrders.count { it.orderStatus == "Delivered" }.toDouble()
    )

    SuqiScreen(
        title = strings.seller.studioTitle,
        subtitle = shopName,
        onBackClick = { viewModel.goBack() },
        headerActions = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .suqiWarmShadow(CircleShape, elevation = 6.dp)
                    .clip(CircleShape)
                    .background(colors.cardBackground)
                    .clickable { viewModel.navigate(ScreenDestination.ShopSettings) }
                    .testTag("seller_settings_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = LucideIcons.Settings,
                    contentDescription = strings.seller.shopBrandingTitle,
                    tint = colors.primaryText,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        isOffline = isOffline,
        onRetryOffline = {
            viewModel.setOfflineMode(false)
            loadDashboard()
        },
        modifier = modifier
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(top = 12.dp)
            ) {
                SuqiSkeletonList(count = 4)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = SuqiBottomNavContentPadding + 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (pendingCount > 0) {
                    item {
                        SuqiSectionCard(
                            containerColor = colors.warningBg,
                            onClick = { viewModel.navigate(ScreenDestination.VerificationQueue) },
                            modifier = Modifier.testTag("seller_pending_banner")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = LucideIcons.TriangleAlert,
                                        contentDescription = null,
                                        tint = SuqiWarning,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = String.format(strings.sellerOps.waitingReviewSubTemplate, pendingCount),
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = colors.primaryText
                                    )
                                }
                                Text(
                                    text = strings.checkout.openLabel,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = SuqiPrimary
                                )
                            }
                        }
                    }
                }

                // KPI Cards
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SuqiStatCard(
                                label = strings.sellerOps.todayRevenueLabel,
                                value = formatEtb(totalSales),
                                icon = LucideIcons.TrendingUp,
                                tint = SuqiSuccess,
                                trendText = "$verifiedCount",
                                trendPositive = true,
                                onClick = { viewModel.switchSellerTab(ScreenDestination.SellerOrders) },
                                testTag = "seller_kpi_sales",
                                modifier = Modifier.weight(1f)
                            )
                            SuqiStatCard(
                                label = strings.sellerOps.pendingVerifLabel,
                                value = pendingOrdersCount.toString(),
                                icon = LucideIcons.ClipboardCheck,
                                tint = if (pendingOrdersCount > 0) SuqiWarning else SuqiPrimary,
                                trendText = if (pendingOrdersCount > 0) strings.admin.queuePending else strings.admin.clearBadge,
                                trendPositive = pendingOrdersCount == 0,
                                onClick = { viewModel.navigate(ScreenDestination.VerificationQueue) },
                                testTag = "seller_kpi_pending",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SuqiStatCard(
                                label = strings.sellerOps.totalProductsLabel,
                                value = "$activeProductCount / $totalProductCount",
                                icon = LucideIcons.Package,
                                tint = SuqiPrimary,
                                trendText = if (lowStockCount > 0) String.format(strings.sellerOps.lowStockCountTemplate, lowStockCount) else strings.seller.statusActive,
                                trendPositive = lowStockCount == 0,
                                onClick = { viewModel.switchSellerTab(ScreenDestination.ProductManagement) },
                                testTag = "seller_kpi_catalog",
                                modifier = Modifier.weight(1f)
                            )
                            SuqiStatCard(
                                label = strings.sellerOps.activeOrdersLabel,
                                value = sellerOrders.size.toString(),
                                icon = LucideIcons.ShoppingBag,
                                tint = SuqiPrimary,
                                trendText = strings.common.viewAllLabel,
                                trendPositive = true,
                                onClick = { viewModel.switchSellerTab(ScreenDestination.SellerOrders) },
                                testTag = "seller_kpi_orders",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Store Setup Checklist (only shown when setup tasks remain)
                if (!hasPaymentMethod || !hasDeliveryOption || !isVerified) {
                    item {
                        SuqiSectionCard {
                            Text(
                                text = strings.sellerOps.readinessChecklistTitle,
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = colors.primaryText
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            if (!hasPaymentMethod) {
                                SuqiListRow(
                                    icon = LucideIcons.Wallet,
                                    title = strings.seller.paymentMethodsTitle,
                                    subtitle = strings.sellerOps.paymentMethodsSub,
                                    badge = strings.common.requiredLabel,
                                    badgeColor = SuqiError,
                                    onClick = { viewModel.navigate(ScreenDestination.PaymentMethodsSetup) },
                                    testTag = "checklist_payment"
                                )
                            }
                            if (!hasDeliveryOption) {
                                SuqiListRow(
                                    icon = LucideIcons.Truck,
                                    title = strings.sellerOps.deliveryRulesTitle,
                                    subtitle = strings.sellerOps.deliveryRulesSub,
                                    badge = strings.checkout.openLabel,
                                    badgeColor = SuqiWarning,
                                    onClick = { viewModel.navigate(ScreenDestination.DeliverySetup) },
                                    testTag = "checklist_delivery"
                                )
                            }
                            if (!isVerified) {
                                val verifStatusText = when (verificationStatus) {
                                    "PENDING_REVIEW" -> strings.admin.queuePending
                                    "VERIFIED" -> strings.admin.queueVerified
                                    "REJECTED" -> strings.admin.queueRejected
                                    else -> verificationStatus.replace("_", " ")
                                }
                                SuqiListRow(
                                    icon = LucideIcons.BadgeCheck,
                                    title = strings.sellerOps.merchantVerificationTitle,
                                    subtitle = verifStatusText,
                                    badge = if (verificationStatus == "PENDING_REVIEW") strings.admin.queuePending else strings.verifiedLabel,
                                    badgeColor = if (verificationStatus == "PENDING_REVIEW") SuqiWarning else SuqiPrimary,
                                    onClick = { viewModel.navigate(ScreenDestination.SellerVerification) },
                                    testTag = "checklist_verify"
                                )
                            }
                        }
                    }
                }

                // Sales Overview Chart with Period Switcher
                item {
                    SuqiSectionCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = strings.seller.analyticsTitle,
                                    fontFamily = SuqiFontFamily,
                                    fontSize = 12.sp,
                                    color = colors.secondaryText
                                )
                                Text(
                                    text = formatEtb(totalSales),
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = colors.primaryText
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("7D", "30D", "90D").forEach { period ->
                                    val selected = period == selectedPeriod
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(if (selected) SuqiPrimary else colors.secondaryBackground)
                                            .clickable { selectedPeriod = period }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                            .testTag("sales_period_$period")
                                    ) {
                                        Text(
                                            text = period,
                                            fontFamily = SuqiFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp,
                                            color = if (selected) Color.White else colors.primaryText
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        if (totalSales <= 0.0) {
                            Text(
                                text = strings.checkout.noOrdersSub,
                                fontFamily = SuqiFontFamily,
                                fontSize = 12.sp,
                                color = colors.mutedText
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        SuqiLineChart(labels = chartLabels, values = chartValues)
                    }
                }

                // Order Status Breakdown
                item {
                    SuqiSectionCard {
                        Text(
                            text = strings.seller.ordersTabTitle,
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = colors.primaryText
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        SuqiBarChart(labels = statusLabels, values = statusValues)
                    }
                }

                // Quick Actions Grid
                item {
                    Text(
                        text = strings.seller.quickActionsTitle,
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = colors.primaryText
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QuickActionTile(
                                icon = LucideIcons.CirclePlus,
                                title = strings.seller.addProductTitle,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.navigate(ScreenDestination.AddEditProduct()) }
                            )
                            QuickActionTile(
                                icon = LucideIcons.Package,
                                title = strings.seller.navProducts,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.switchSellerTab(ScreenDestination.ProductManagement) }
                            )
                            QuickActionTile(
                                icon = LucideIcons.SlidersHorizontal,
                                title = strings.sellerWork.inventoryTitle,
                                modifier = Modifier.weight(1f).testTag("quick_action_inventory"),
                                onClick = { viewModel.navigate(ScreenDestination.SellerInventory) }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QuickActionTile(
                                icon = LucideIcons.ClipboardCheck,
                                title = strings.seller.verificationQueueTitle,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.navigate(ScreenDestination.VerificationQueue) }
                            )
                            QuickActionTile(
                                icon = LucideIcons.Wallet,
                                title = strings.seller.paymentMethodsTitle,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.navigate(ScreenDestination.PaymentMethodsSetup) }
                            )
                            QuickActionTile(
                                icon = LucideIcons.Truck,
                                title = strings.checkout.stepDelivery,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.navigate(ScreenDestination.DeliverySetup) }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QuickActionTile(
                                icon = LucideIcons.BadgePercent,
                                title = strings.seller.promotionsTitle,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.navigate(ScreenDestination.Promotions) }
                            )
                            QuickActionTile(
                                icon = LucideIcons.BadgeCheck,
                                title = strings.seller.sellerVerificationTitle,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.navigate(ScreenDestination.SellerVerification) }
                            )
                            QuickActionTile(
                                icon = LucideIcons.Store,
                                title = strings.seller.shopBrandingTitle,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.navigate(ScreenDestination.ShopSettings) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricStatCard(
    title: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val colors = LocalSuqiColors.current
    SuqiSectionCard(
        padding = 12.dp,
        cornerRadius = 18.dp,
        onClick = onClick,
        modifier = modifier
    ) {
        Text(text = title, fontFamily = SuqiFontFamily, fontSize = 11.sp, color = colors.secondaryText)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.primaryText)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = sub, fontFamily = SuqiFontFamily, fontSize = 10.sp, color = SuqiPrimary)
    }
}

@Composable
fun QuickActionTile(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = LocalSuqiColors.current
    SuqiSectionCard(
        padding = 0.dp,
        cornerRadius = 18.dp,
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(colors.primaryTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = SuqiPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                color = colors.primaryText,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VerificationQueueScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val isOffline by viewModel.isOffline.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var allOrders by remember { mutableStateOf(viewModel.cachedSellerOrders) }
    var isLoading by remember { mutableStateOf(allOrders.isEmpty()) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0: Pending, 1: Verified, 2: Rejected
    var selectedOrderForReject by remember { mutableStateOf<OrderDto?>(null) }
    var rejectReason by remember { mutableStateOf(strings.extras.rejectReasonRefNotFound) }
    var zoomReceiptOrder by remember { mutableStateOf<OrderDto?>(null) }
    val listState = rememberLazyListState()

    BackHandler(enabled = zoomReceiptOrder != null || selectedOrderForReject != null) {
        when {
            zoomReceiptOrder != null -> zoomReceiptOrder = null
            selectedOrderForReject != null -> selectedOrderForReject = null
        }
    }

    val quickRejectReasons = remember(strings) {
        listOf(
            strings.extras.rejectReasonRefNotFound,
            strings.extras.rejectReasonBlurry,
            strings.extras.rejectReasonAmountMismatch,
            strings.extras.rejectReasonDuplicateRef
        )
    }

    fun loadQueue() {
        coroutineScope.launch {
            isLoading = allOrders.isEmpty()
            val ordersRes = viewModel.repository.getSellerOrders()
            if (ordersRes.success && ordersRes.data != null) {
                allOrders = ordersRes.data
                viewModel.cachedSellerOrders = ordersRes.data
            } else {
                val qRes = viewModel.repository.getSellerVerificationQueue()
                if (qRes.success && qRes.data != null) {
                    allOrders = qRes.data
                }
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadQueue()
    }

    val pendingOrders = remember(allOrders) {
        allOrders.filter { it.paymentStatus == "PENDING_VERIFICATION" }
    }
    val verifiedOrders = remember(allOrders) {
        allOrders.filter { it.paymentStatus == "VERIFIED" }
    }
    val rejectedOrders = remember(allOrders) {
        allOrders.filter { it.paymentStatus == "REJECTED" }
    }

    val activeOrders = when (selectedTab) {
        0 -> pendingOrders
        1 -> verifiedOrders
        else -> rejectedOrders
    }

    SuqiScreen(
        title = strings.seller.verificationQueueTitle,
        subtitle = String.format(strings.sellerOps.waitingReviewSubTemplate, pendingOrders.size),
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        onRetryOffline = {
            viewModel.setOfflineMode(false)
            loadQueue()
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SuqiTabs(
                tabs = listOf(
                    SuqiTabItem(
                        key = "pending",
                        label = strings.checkout.filterPending,
                        count = pendingOrders.size,
                        showDot = pendingOrders.isNotEmpty()
                    ),
                    SuqiTabItem(
                        key = "verified",
                        label = strings.verifiedLabel,
                        count = verifiedOrders.size
                    ),
                    SuqiTabItem(
                        key = "rejected",
                        label = strings.admin.queueRejected,
                        count = rejectedOrders.size
                    )
                ),
                selectedIndex = selectedTab,
                onSelectTab = { selectedTab = it },
                style = SuqiTabStyle.Underline,
                testTagPrefix = "seller_verify_tab"
            )

            if (isLoading) {
                Box(modifier = Modifier.padding(top = 12.dp)) {
                    SuqiSkeletonList(count = 3)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (selectedTab == 0 && pendingOrders.isNotEmpty()) {
                        item {
                            SuqiSectionCard(containerColor = colors.warningBg, padding = 12.dp, cornerRadius = 16.dp) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(LucideIcons.Clock, contentDescription = null, tint = SuqiWarning, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = String.format(strings.sellerOps.waitingReviewSubTemplate, pendingOrders.size),
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = colors.primaryText
                                    )
                                }
                            }
                        }
                    }

                    if (activeOrders.isEmpty()) {
                        item {
                            SuqiEmptyState(
                                icon = if (selectedTab == 2) LucideIcons.X else LucideIcons.CheckCircle,
                                title = strings.buyer.noNotificationsTitle,
                                subtitle = strings.sellerOps.verifyQueueSub,
                                ctaText = strings.seller.ordersTabTitle,
                                onCtaClick = { viewModel.navigate(ScreenDestination.SellerOrders) },
                                testTag = "verification_queue_empty"
                            )
                        }
                    } else {
                        items(activeOrders, key = { it.id }) { order ->
                            val elapsedHours = EthiopianMarketplaceUtils.computeElapsedHours(order.createdAt)
                            val ageLabel = EthiopianMarketplaceUtils.formatWaitingAgeLabel(order.createdAt, strings)
                            val isPending = order.paymentStatus == "PENDING_VERIFICATION"
                            val isWaitingOver6h = isPending && elapsedHours != null && elapsedHours >= 6L

                            SuqiSectionCard(
                                onClick = { viewModel.navigate(ScreenDestination.SellerOrderDetail(order.id)) },
                                modifier = Modifier.testTag("verify_card_${order.id}")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = String.format(strings.checkout.orderNumberTemplate, order.orderReference),
                                            fontFamily = SuqiFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = colors.primaryText
                                        )
                                        Text(
                                            text = "${order.customerName} • ${order.customerPhone}",
                                            fontFamily = SuqiFontFamily,
                                            fontSize = 13.sp,
                                            color = colors.secondaryText
                                        )
                                        Text(
                                            text = order.createdAt,
                                            fontFamily = SuqiFontFamily,
                                            fontSize = 11.sp,
                                            color = colors.mutedText
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    SuqiStatusBadge(
                                        statusText = when (order.paymentStatus) {
                                            "VERIFIED" -> strings.verifiedLabel
                                            "REJECTED" -> strings.admin.queueRejected
                                            else -> strings.checkout.filterPending
                                        },
                                        type = when (order.paymentStatus) {
                                            "VERIFIED" -> StatusType.SUCCESS
                                            "REJECTED" -> StatusType.ERROR
                                            else -> StatusType.WARNING
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isPending && elapsedHours != null) {
                                        val badgeText = when {
                                            isWaitingOver6h && elapsedHours >= 24L ->
                                                "${strings.seller.waitingOver6h} (${String.format(strings.extras.ageDaysTemplate, elapsedHours / 24L)})"
                                            isWaitingOver6h -> strings.seller.waitingOver6h
                                            else -> ageLabel ?: strings.extras.justNowLabel
                                        }
                                        SuqiStatusBadge(
                                            statusText = badgeText,
                                            type = if (isWaitingOver6h) StatusType.WARNING else StatusType.NEUTRAL,
                                            modifier = Modifier.testTag("verify_age_${order.id}")
                                        )
                                    } else if (!ageLabel.isNullOrBlank()) {
                                        SuqiStatusBadge(
                                            statusText = ageLabel,
                                            type = StatusType.NEUTRAL,
                                            modifier = Modifier.testTag("verify_age_${order.id}")
                                        )
                                    }

                                    AmountMatchChip(
                                        order = order,
                                        testTag = "verify_amount_chip_${order.id}"
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(strings.checkout.paymentMethodTitle, fontSize = 13.sp, color = colors.secondaryText)
                                    Text(order.paymentMethodTitle, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.primaryText)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(strings.checkout.txRefLabel, fontSize = 13.sp, color = colors.secondaryText)
                                    Text(order.transactionReference, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SuqiPrimary)
                                }
                                if (order.payerIdentifier.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(strings.checkout.senderPhoneLabel, fontSize = 13.sp, color = colors.secondaryText)
                                        Text(order.payerIdentifier, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = colors.primaryText)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(strings.checkout.totalAmountLabel, fontSize = 13.sp, color = colors.secondaryText)
                                    Text(formatEtb(order.totalAmount), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                                }
                                if (order.transferredAmount != null && abs(order.transferredAmount - order.totalAmount) >= 1.0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(strings.checkout.transferAmountLabel, fontSize = 13.sp, color = colors.secondaryText)
                                        Text(formatEtb(order.transferredAmount), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SuqiWarning)
                                    }
                                }

                                if (order.paymentStatus == "VERIFIED" && (!order.verifiedBy.isNullOrBlank() || !order.verifiedAt.isNullOrBlank())) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = listOfNotNull(
                                            order.verifiedBy?.takeIf { it.isNotBlank() }?.let {
                                                String.format(strings.extras.verifiedByTemplate, it)
                                            },
                                            order.verifiedAt?.takeIf { it.isNotBlank() }
                                        ).joinToString(" • "),
                                        fontFamily = SuqiFontFamily,
                                        fontSize = 12.sp,
                                        color = SuqiSuccess
                                    )
                                }

                                if (order.paymentStatus == "REJECTED" && !order.rejectionReason.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(colors.errorBg)
                                            .padding(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "${strings.checkout.rejectionReasonTitle}: ${order.rejectionReason}",
                                            fontFamily = SuqiFontFamily,
                                            fontSize = 12.sp,
                                            color = SuqiError
                                        )
                                    }
                                }

                                if (order.receiptImageUri.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(130.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(colors.imageBackdrop)
                                            .clickable { zoomReceiptOrder = order }
                                            .testTag("verify_receipt_thumb_${order.id}")
                                    ) {
                                        AsyncImage(
                                            model = order.receiptImageUri,
                                            contentDescription = strings.checkout.receiptAttachedLabel,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(8.dp)
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(Color.Black.copy(alpha = 0.65f))
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(LucideIcons.Maximize2, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(strings.viewInvoice, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                }

                                if (isPending) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    viewModel.repository.verifyPayment(order.id, mapOf("notes" to "Confirmed against receipt"))
                                                    viewModel.showToast(strings.sellerOps.approvePaymentLabel)
                                                    loadQueue()
                                                }
                                            },
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = SuqiSuccess),
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp)
                                                .testTag("approve_payment_${order.id}")
                                        ) {
                                            Text(strings.seller.approvePaymentBtn, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                selectedOrderForReject = order
                                                rejectReason = quickRejectReasons.first()
                                            },
                                            shape = RoundedCornerShape(14.dp),
                                            border = BorderStroke(1.dp, SuqiError.copy(alpha = 0.5f)),
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp)
                                                .testTag("reject_payment_${order.id}")
                                        ) {
                                            Text(strings.seller.rejectPaymentBtn, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, color = SuqiError, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedOrderForReject?.let { targetOrder ->
        SuqiBottomSheet(
            onDismissRequest = { selectedOrderForReject = null },
            title = "${strings.sellerOps.rejectPaymentTitle} #${targetOrder.orderReference}"
        ) {
            Text(
                text = strings.checkout.selectReasonLabel,
                fontFamily = SuqiFontFamily,
                fontSize = 13.sp,
                color = colors.secondaryText
            )
            Spacer(modifier = Modifier.height(10.dp))
            quickRejectReasons.forEach { preset ->
                val isSel = rejectReason == preset
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSel) colors.errorBg else colors.secondaryBackground)
                        .border(1.dp, if (isSel) SuqiError else colors.border, RoundedCornerShape(12.dp))
                        .clickable { rejectReason = preset }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = preset,
                        fontFamily = SuqiFontFamily,
                        fontSize = 13.sp,
                        fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSel) SuqiError else colors.primaryText
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            SuqiTextField(
                value = rejectReason,
                onValueChange = { rejectReason = it },
                label = strings.sellerOps.rejectReasonRequired,
                singleLine = false
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { selectedOrderForReject = null },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Text(strings.cancelLabel, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Button(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.repository.rejectPayment(
                                targetOrder.id,
                                mapOf("reason" to rejectReason.trim().ifBlank { quickRejectReasons.first() })
                            )
                            viewModel.showToast(strings.sellerOps.confirmRejectionBtn)
                            selectedOrderForReject = null
                            loadQueue()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuqiError),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("confirm_reject_payment")
                ) {
                    Text(strings.sellerOps.confirmRejectionBtn, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }

    zoomReceiptOrder?.let { docOrder ->
        SuqiDocumentViewer(
            documents = listOf(
                SuqiDocumentPage(
                    title = "${String.format(strings.checkout.orderNumberTemplate, docOrder.orderReference)} • ${formatEtb(docOrder.totalAmount)}",
                    imageUrl = docOrder.receiptImageUri,
                    note = "${docOrder.paymentMethodTitle} • ${strings.checkout.txRefLabel}: ${docOrder.transactionReference}"
                )
            ),
            onDismiss = { zoomReceiptOrder = null }
        )
    }
}

@Composable
private fun AmountMatchChip(
    order: OrderDto,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val entered = order.transferredAmount
    val matches = entered != null && abs(entered - order.totalAmount) < 1.0
    val bgColor = if (matches) colors.successBg else colors.warningBg
    val tint = if (matches) SuqiSuccess else SuqiWarning
    val icon = if (matches) LucideIcons.Check else LucideIcons.TriangleAlert
    val label = when {
        matches -> strings.seller.amountMatchesLabel
        entered != null -> "${strings.seller.amountMismatchLabel} (${formatEtb(entered)})"
        else -> strings.seller.amountMismatchLabel
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .let { if (testTag != null) it.testTag(testTag) else it }
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(12.dp))
        Text(
            text = label,
            fontFamily = SuqiFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun PaymentMethodsSetupScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val isOffline by viewModel.isOffline.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var activeShopId by remember { mutableStateOf("") }
    var activeShopName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var methods by remember { mutableStateOf<List<ShopPaymentMethodDto>>(emptyList()) }
    var editingMethod by remember { mutableStateOf<ShopPaymentMethodDto?>(null) }
    var showFormSheet by remember { mutableStateOf(false) }

    BackHandler(enabled = showFormSheet) {
        showFormSheet = false
    }

    fun reloadMethods(shopId: String) {
        coroutineScope.launch {
            if (shopId.isBlank()) {
                isLoading = false
                return@launch
            }
            val res = viewModel.repository.getShopPaymentMethods(shopId)
            if (res.success && res.data != null) {
                methods = res.data
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        val sid = resolveActiveSellerShopId(viewModel)
        activeShopId = sid
        if (sid.isNotBlank()) {
            val shopRes = viewModel.repository.getShopDetails(sid)
            activeShopName = shopRes.data?.name ?: ""
        }
        reloadMethods(sid)
    }

    SuqiScreen(
        title = strings.seller.paymentMethodsTitle,
        subtitle = activeShopName.ifBlank { null },
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        bottomBar = {
            Surface(
                color = colors.cardBackground,
                shadowElevation = if (colors.isDark) 0.dp else 12.dp,
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SuqiButton(
                        text = strings.sellerOps.addPaymentMethodBtn,
                        icon = LucideIcons.Plus,
                        onClick = {
                            editingMethod = null
                            showFormSheet = true
                        },
                        testTag = "add_payment_method_button",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.padding(paddingValues).padding(top = 12.dp)) {
                SuqiSkeletonList(count = 3)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    val hasEnabled = methods.any { it.isEnabled }
                    SuqiSectionCard(
                        containerColor = if (hasEnabled) colors.primaryTint else colors.warningBg,
                        padding = 14.dp,
                        cornerRadius = 16.dp
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (hasEnabled) LucideIcons.Info else LucideIcons.TriangleAlert,
                                contentDescription = null,
                                tint = if (hasEnabled) SuqiPrimary else SuqiWarning,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (hasEnabled) {
                                    strings.sellerOps.paymentMethodsSub
                                } else {
                                    strings.sellerOps.paymentMethodRequiredBody
                                },
                                fontFamily = SuqiFontFamily,
                                fontSize = 13.sp,
                                color = colors.primaryText
                            )
                        }
                    }
                }

                if (methods.isEmpty()) {
                    item {
                        SuqiEmptyState(
                            icon = LucideIcons.Wallet,
                            title = strings.seller.paymentMethodsTitle,
                            subtitle = strings.sellerOps.paymentMethodsSub,
                            ctaText = strings.sellerOps.addPaymentMethodBtn,
                            onCtaClick = {
                                editingMethod = null
                                showFormSheet = true
                            },
                            testTag = "payment_methods_empty"
                        )
                    }
                } else {
                    items(methods, key = { it.id }) { method ->
                        SuqiSectionCard(modifier = Modifier.testTag("payment_method_card_${method.id}")) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = method.bankName,
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = colors.primaryText
                                    )
                                    val providerLabel = when (method.providerType) {
                                        "TELEBIRR" -> strings.checkout.telebirrLabel
                                        "CBE_BIRR" -> strings.checkout.cbeBirrLabel
                                        "BANK_TRANSFER" -> strings.checkout.bankTransferLabel
                                        else -> method.providerType.replace("_", " ")
                                    }
                                    Text(
                                        text = providerLabel,
                                        fontFamily = SuqiFontFamily,
                                        fontSize = 11.sp,
                                        color = colors.mutedText
                                    )
                                }
                                Switch(
                                    checked = method.isEnabled,
                                    onCheckedChange = { enabled ->
                                        coroutineScope.launch {
                                            viewModel.repository.saveShopPaymentMethod(method.copy(isEnabled = enabled))
                                            reloadMethods(activeShopId)
                                        }
                                    },
                                    colors = SwitchDefaults.colors(checkedTrackColor = SuqiPrimary)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${method.accountNumber} (${method.accountName})",
                                fontFamily = SuqiFontFamily,
                                fontSize = 13.sp,
                                color = colors.secondaryText
                            )
                            if (method.instructions.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = method.instructions,
                                    fontFamily = SuqiFontFamily,
                                    fontSize = 12.sp,
                                    color = colors.mutedText
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        editingMethod = method
                                        showFormSheet = true
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("edit_pm_${method.id}")
                                ) {
                                    Icon(LucideIcons.Pencil, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.editLabel, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.repository.deleteShopPaymentMethod(method.id)
                                            viewModel.showToast(strings.common.removeLabel)
                                            reloadMethods(activeShopId)
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, SuqiError.copy(alpha = 0.5f)),
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("delete_pm_${method.id}")
                                ) {
                                    Icon(LucideIcons.Trash2, contentDescription = null, tint = SuqiError, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.common.removeLabel, color = SuqiError, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFormSheet) {
        val current = editingMethod
        var providerType by remember(current) { mutableStateOf(current?.providerType ?: "TELEBIRR") }
        var bankName by remember(current) { mutableStateOf(current?.bankName ?: "Telebirr") }
        var accName by remember(current) { mutableStateOf(current?.accountName ?: activeShopName) }
        var accNum by remember(current) { mutableStateOf(current?.accountNumber ?: "") }
        var inst by remember(current) { mutableStateOf(current?.instructions ?: "Send total amount and upload receipt") }

        SuqiBottomSheet(
            onDismissRequest = { showFormSheet = false },
            title = if (current == null) strings.sellerOps.addPaymentMethodBtn else "${strings.editLabel} ${strings.seller.paymentMethodsTitle}"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "TELEBIRR" to strings.checkout.telebirrLabel,
                        "CBE_BIRR" to strings.checkout.cbeBirrLabel,
                        "BANK_TRANSFER" to strings.checkout.bankTransferLabel
                    ).forEach { (type, label) ->
                        val sel = providerType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (sel) SuqiPrimary else colors.secondaryBackground)
                                .clickable {
                                    providerType = type
                                    if (current == null) {
                                        bankName = when (type) {
                                            "TELEBIRR" -> "Telebirr"
                                            "CBE_BIRR" -> "CBE Birr"
                                            else -> "Commercial Bank of Ethiopia"
                                        }
                                    }
                                }
                                .padding(horizontal = 6.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = if (sel) Color.White else colors.primaryText
                            )
                        }
                    }
                }
                SuqiTextField(value = bankName, onValueChange = { bankName = it }, label = strings.checkout.paymentMethodTitle)
                SuqiTextField(value = accName, onValueChange = { accName = it }, label = strings.sellerOps.accountHolderLabel)
                SuqiTextField(value = accNum, onValueChange = { accNum = it }, label = strings.sellerOps.accountNumberLabel)
                SuqiTextField(value = inst, onValueChange = { inst = it }, label = strings.description, singleLine = false)

                Spacer(modifier = Modifier.height(4.dp))

                SuqiButton(
                    text = strings.saveLabel,
                    onClick = {
                        if (bankName.isBlank() || accName.isBlank() || accNum.isBlank()) {
                            viewModel.showToast(strings.common.requiredLabel)
                            return@SuqiButton
                        }
                        coroutineScope.launch {
                            val dto = ShopPaymentMethodDto(
                                id = current?.id ?: "pm_${System.currentTimeMillis()}",
                                shopId = current?.shopId?.takeIf { it.isNotBlank() } ?: activeShopId,
                                providerType = providerType,
                                bankName = bankName.trim(),
                                accountName = accName.trim(),
                                accountNumber = accNum.trim(),
                                instructions = inst.trim(),
                                isRequired = current?.isRequired ?: true,
                                isEnabled = current?.isEnabled ?: true
                            )
                            val res = viewModel.repository.saveShopPaymentMethod(dto)
                            if (res.success) {
                                viewModel.showToast(strings.saveLabel)
                                showFormSheet = false
                                reloadMethods(activeShopId)
                            } else {
                                viewModel.showToast(res.message ?: strings.common.failedToAddToast)
                            }
                        }
                    },
                    testTag = "save_payment_method_sheet_btn",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun PromotionsScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val isOffline by viewModel.isOffline.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var activeShopId by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var promoList by remember { mutableStateOf<List<PromoCodeDto>>(emptyList()) }

    var promoCode by remember { mutableStateOf("") }
    var discountType by remember { mutableStateOf("PERCENTAGE") }
    var discountValue by remember { mutableStateOf("15") }
    var minOrder by remember { mutableStateOf("500") }
    var endDate by remember { mutableStateOf("Dec 31, 2026") }

    fun reloadPromos(shopId: String) {
        coroutineScope.launch {
            if (shopId.isBlank()) {
                isLoading = false
                return@launch
            }
            val res = viewModel.repository.getShopPromotions(shopId, includeInactive = true)
            if (res.success && res.data != null) {
                promoList = res.data
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        val sid = resolveActiveSellerShopId(viewModel)
        activeShopId = sid
        reloadPromos(sid)
    }

    SuqiScreen(
        title = strings.seller.promotionsTitle,
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.padding(paddingValues).padding(top = 12.dp)) {
                SuqiSkeletonList(count = 3)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = String.format(strings.sellerOps.shopPromoCodesTemplate, promoList.size),
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = colors.primaryText
                    )
                }

                if (promoList.isEmpty()) {
                    item {
                        SuqiEmptyState(
                            icon = LucideIcons.TicketPercent,
                            title = strings.sellerOps.noPromosYetTitle,
                            subtitle = strings.sellerOps.noPromosYetSub,
                            testTag = "promotions_empty"
                        )
                    }
                } else {
                    items(promoList, key = { it.id }) { promo ->
                        SuqiSectionCard(
                            containerColor = if (promo.isActive) colors.primaryTint else colors.secondaryBackground
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = promo.code,
                                            fontFamily = SuqiFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = colors.primaryText
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        SuqiStatusBadge(
                                            statusText = if (promo.isActive) strings.seller.statusActive else strings.seller.statusPaused,
                                            type = if (promo.isActive) StatusType.SUCCESS else StatusType.NEUTRAL
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val discText = if (promo.discountType == "PERCENTAGE") {
                                        "${promo.discountValue.toInt()}%"
                                    } else {
                                        formatEtb(promo.discountValue)
                                    }
                                    Text(
                                        text = "$discText • ${formatEtb(promo.minOrderAmount)}",
                                        fontSize = 13.sp,
                                        color = colors.secondaryText
                                    )
                                    Text(
                                        text = promo.validUntil,
                                        fontSize = 12.sp,
                                        color = colors.mutedText
                                    )
                                }

                                Switch(
                                    checked = promo.isActive,
                                    onCheckedChange = { active ->
                                        coroutineScope.launch {
                                            viewModel.repository.createPromoCode(promo.copy(isActive = active))
                                            reloadPromos(activeShopId)
                                        }
                                    },
                                    colors = SwitchDefaults.colors(checkedTrackColor = SuqiPrimary)
                                )

                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.repository.deletePromoCode(promo.id)
                                            viewModel.showToast(strings.deleteLabel)
                                            reloadPromos(activeShopId)
                                        }
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("delete_promo_${promo.id}")
                                ) {
                                    Icon(LucideIcons.Trash2, contentDescription = strings.deleteLabel, tint = SuqiError)
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = strings.sellerOps.createPromoTitle,
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = colors.primaryText
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    SuqiSectionCard {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            SuqiTextField(
                                value = promoCode,
                                onValueChange = { promoCode = it },
                                label = strings.sellerOps.promoCodeNameLabel
                            )

                            Column {
                                Text(strings.sellerOps.discountTypeLabel, fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.secondaryText)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    val isPct = discountType == "PERCENTAGE"
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isPct) SuqiPrimary else colors.secondaryBackground)
                                            .clickable { discountType = "PERCENTAGE" }
                                            .padding(horizontal = 8.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(strings.sellerOps.percentageTypeLabel, color = if (isPct) Color.White else colors.primaryText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                                    }
                                    val isFix = discountType == "FIXED"
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isFix) SuqiPrimary else colors.secondaryBackground)
                                            .clickable { discountType = "FIXED" }
                                            .padding(horizontal = 8.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(strings.sellerOps.fixedTypeLabel, color = if (isFix) Color.White else colors.primaryText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                SuqiTextField(
                                    value = discountValue,
                                    onValueChange = { discountValue = it },
                                    label = if (discountType == "PERCENTAGE") strings.sellerOps.discountPctLabel else strings.sellerOps.discountEtbLabel,
                                    modifier = Modifier.weight(1f)
                                )
                                SuqiTextField(
                                    value = minOrder,
                                    onValueChange = { minOrder = it },
                                    label = strings.sellerOps.minOrderEtbLabel,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            SuqiTextField(value = endDate, onValueChange = { endDate = it }, label = strings.sellerOps.expiryDateLabel)

                            SuqiButton(
                                text = strings.sellerOps.createPromoBtn,
                                onClick = {
                                    val codeClean = promoCode.trim().uppercase()
                                    val valNum = discountValue.toDoubleOrNull() ?: 0.0
                                    if (codeClean.isBlank() || valNum <= 0.0) {
                                        viewModel.showToast(strings.checkout.invalidPromoToast)
                                        return@SuqiButton
                                    }
                                    coroutineScope.launch {
                                        val newPromo = PromoCodeDto(
                                            id = "promo_${System.currentTimeMillis()}",
                                            shopId = activeShopId,
                                            code = codeClean,
                                            discountType = discountType,
                                            discountValue = valNum,
                                            minOrderAmount = minOrder.toDoubleOrNull() ?: 0.0,
                                            validUntil = endDate.trim().ifBlank { "Dec 31, 2026" },
                                            isActive = true
                                        )
                                        val res = viewModel.repository.createPromoCode(newPromo)
                                        if (res.success) {
                                            viewModel.showToast(String.format(strings.checkout.promoAppliedTemplate, codeClean))
                                            promoCode = ""
                                            reloadPromos(activeShopId)
                                        } else {
                                            viewModel.showToast(res.message ?: strings.common.failedToAddToast)
                                        }
                                    }
                                },
                                testTag = "create_promo_button",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SellerVerificationScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    SellerVerificationV2Screen(viewModel = viewModel, modifier = modifier)
}

@Composable
fun SellerSubscriptionScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val isOffline by viewModel.isOffline.collectAsState()
    var adminSettings by remember { mutableStateOf(AdminSettingsDto()) }
    var plans by remember { mutableStateOf<List<SubscriptionPlanDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoading = true
        val res = viewModel.repository.getAdminSettings()
        if (res.success && res.data != null) {
            adminSettings = res.data
        }
        val plansRes = viewModel.repository.getSubscriptionPlans()
        if (plansRes.success && plansRes.data != null) {
            plans = plansRes.data.filter { it.isActive }
        }
        isLoading = false
    }

    SuqiScreen(
        title = strings.seller.subscriptionTitle,
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.padding(paddingValues).padding(top = 12.dp)) {
                SuqiSkeletonList(count = 3)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (!adminSettings.isSubscriptionModeEnabled) {
                    item {
                        SuqiSectionCard(containerColor = colors.successBg) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(LucideIcons.Sparkles, contentDescription = null, tint = SuqiSuccess, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(strings.sellerOps.freePilotBadge, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SuqiSuccess)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        strings.sellerOps.subscriptionPlanSub,
                                        fontFamily = SuqiFontFamily,
                                        fontSize = 12.sp,
                                        color = colors.primaryText
                                    )
                                }
                            }
                        }
                    }
                }

                if (plans.isEmpty()) {
                    item {
                        SuqiSectionCard(containerColor = colors.primaryTint) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(adminSettings.activePlanName, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = SuqiPrimary)
                                SuqiStatusBadge(if (adminSettings.isSubscriptionModeEnabled) strings.adminOps.activeStatus else strings.sellerOps.freePilotBadge, StatusType.SUCCESS)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (adminSettings.isSubscriptionModeEnabled) String.format(strings.sellerOps.perMonthTemplate, formatEtb(adminSettings.activePlanPrice)) else strings.sellerOps.freeDuringPilot,
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp,
                                color = colors.primaryText
                            )
                        }
                    }
                } else {
                    items(plans, key = { it.id }) { plan ->
                        SuqiSectionCard(
                            containerColor = if (plan.isPopular) colors.primaryTint else colors.cardBackground
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(plan.name, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.primaryText)
                                if (plan.isPopular || !adminSettings.isSubscriptionModeEnabled) {
                                    SuqiStatusBadge(
                                        statusText = if (!adminSettings.isSubscriptionModeEnabled) strings.sellerOps.freePilotBadge else strings.sellerOps.recommendedBadge,
                                        type = StatusType.SUCCESS
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (adminSettings.isSubscriptionModeEnabled) String.format(strings.sellerOps.perMonthTemplate, formatEtb(plan.monthlyPrice)) else strings.sellerOps.freeDuringPilot,
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = SuqiPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            plan.featuresCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { feat ->
                                Text("✓ $feat", fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.secondaryText)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SellerOrdersScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val isOffline by viewModel.isOffline.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var orders by remember { mutableStateOf(viewModel.cachedSellerOrders) }
    var selectedTab by rememberSaveable { mutableStateOf("All") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(orders.isEmpty()) }
    val listState = rememberLazyListState()

    fun loadOrders() {
        coroutineScope.launch {
            isLoading = orders.isEmpty()
            val res = viewModel.repository.getSellerOrders()
            applyOnMain {
                if (res.success && res.data != null) {
                    orders = res.data
                    viewModel.cachedSellerOrders = res.data
                }
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadOrders()
    }

    val tabs = remember(orders, strings) {
        listOf(
            "All" to "${strings.checkout.filterAll} (${orders.size})",
            "Awaiting Verification" to "${strings.checkout.filterPending} (${orders.count { it.paymentStatus == "PENDING_VERIFICATION" }})",
            "Confirmed" to "${strings.checkout.filterConfirmed} (${orders.count { it.orderStatus in listOf("Confirmed", "Processing") }})",
            "Dispatched" to "${strings.checkout.statusDispatched} (${orders.count { it.orderStatus in listOf("Dispatched", "Out for Delivery") }})",
            "Delivered" to "${strings.checkout.filterDelivered} (${orders.count { it.orderStatus == "Delivered" }})",
            "Cancelled" to "${strings.checkout.filterCancelled} (${orders.count { it.orderStatus == "Cancelled" || it.paymentStatus == "REJECTED" }})"
        )
    }

    val filteredOrders = remember(orders, selectedTab, searchQuery) {
        orders.filter { order ->
            val matchesTab = when (selectedTab) {
                "All" -> true
                "Awaiting Verification" -> order.paymentStatus == "PENDING_VERIFICATION"
                "Confirmed" -> order.orderStatus in listOf("Confirmed", "Processing")
                "Dispatched" -> order.orderStatus in listOf("Dispatched", "Out for Delivery")
                "Delivered" -> order.orderStatus == "Delivered"
                "Cancelled" -> order.orderStatus == "Cancelled" || order.paymentStatus == "REJECTED"
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                order.orderReference.contains(searchQuery, ignoreCase = true) ||
                order.customerName.contains(searchQuery, ignoreCase = true) ||
                order.customerPhone.contains(searchQuery, ignoreCase = true)
            matchesTab && matchesSearch
        }
    }

    SuqiScreen(
        title = strings.seller.ordersTabTitle,
        subtitle = "${orders.size} • ${strings.sellerOps.activeOrdersLabel}",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        onRetryOffline = {
            viewModel.setOfflineMode(false)
            loadOrders()
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SuqiSearchField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = strings.common.searchHintGeneric,
                testTag = "seller_orders_search",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            SuqiFilterChips(
                items = tabs,
                selectedKey = selectedTab,
                onSelect = { selectedTab = it },
                testTagPrefix = "seller_orders_tab"
            )

            if (isLoading) {
                Box(modifier = Modifier.padding(top = 12.dp)) {
                    SuqiSkeletonList(count = 4)
                }
            } else if (filteredOrders.isEmpty()) {
                val localizedTabName = tabs.firstOrNull { it.first == selectedTab }?.second ?: selectedTab
                SuqiEmptyState(
                    icon = LucideIcons.PackageOpen,
                    title = strings.checkout.noOrdersFound,
                    subtitle = if (orders.isEmpty()) strings.checkout.noOrdersSub else String.format(strings.checkout.noOrdersStatusTemplate, localizedTabName),
                    ctaText = if (selectedTab != "All") strings.checkout.filterAll else null,
                    onCtaClick = if (selectedTab != "All") { { selectedTab = "All" } } else null,
                    testTag = "seller_orders_empty"
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 10.dp,
                        bottom = SuqiBottomNavContentPadding + 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredOrders, key = { it.id }) { order ->
                        SellerOrderCard(
                            order = order,
                            onVerify = {
                                coroutineScope.launch {
                                    viewModel.repository.verifyPayment(order.id, mapOf("notes" to "Verified by seller"))
                                    viewModel.showToast("${strings.seller.approvePaymentBtn} • #${order.orderReference}")
                                    loadOrders()
                                }
                            },
                            onDispatch = {
                                coroutineScope.launch {
                                    viewModel.repository.updateSellerOrderStatus(order.id, "Dispatched")
                                    viewModel.showToast("${strings.checkout.statusDispatched} • #${order.orderReference}")
                                    loadOrders()
                                }
                            },
                            onDeliver = {
                                coroutineScope.launch {
                                    viewModel.repository.updateSellerOrderStatus(order.id, "Delivered")
                                    viewModel.showToast("${strings.checkout.filterDelivered} • #${order.orderReference}")
                                    loadOrders()
                                }
                            },
                            onChat = {
                                viewModel.navigate(ScreenDestination.ChatThread(order.shopId))
                            },
                            onViewDetails = {
                                viewModel.navigate(ScreenDestination.SellerOrderDetail(order.id))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SellerOrderCard(
    order: OrderDto,
    onVerify: () -> Unit,
    onDispatch: () -> Unit,
    onDeliver: () -> Unit,
    onChat: () -> Unit,
    onViewDetails: () -> Unit
) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val localizedOrderStatus = when (order.orderStatus) {
        "Confirmed" -> strings.checkout.filterConfirmed
        "Processing" -> strings.checkout.statusProcessing
        "Dispatched" -> strings.checkout.statusDispatched
        "Out for Delivery" -> strings.checkout.statusOutForDelivery
        "Delivered" -> strings.checkout.filterDelivered
        "Cancelled" -> strings.checkout.filterCancelled
        else -> order.orderStatus
    }
    SuqiSectionCard(
        onClick = onViewDetails,
        modifier = Modifier.testTag("seller_order_card_${order.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = String.format(strings.checkout.orderNumberTemplate, order.orderReference),
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = colors.primaryText
                )
                Text(
                    text = "${order.customerName} • ${order.customerPhone}",
                    fontSize = 12.sp,
                    color = colors.secondaryText
                )
            }
            SuqiStatusBadge(
                statusText = if (order.paymentStatus == "PENDING_VERIFICATION") strings.checkout.statusPendingVerif else localizedOrderStatus,
                type = when {
                    order.paymentStatus == "PENDING_VERIFICATION" -> StatusType.WARNING
                    order.paymentStatus == "REJECTED" || order.orderStatus == "Cancelled" -> StatusType.ERROR
                    else -> StatusType.SUCCESS
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = formatEtb(order.totalAmount), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primaryText)
            Text(text = "${String.format(strings.checkout.itemsSummaryTemplate, order.items.sumOf { it.quantity })} • ${order.paymentMethodTitle}", fontSize = 12.sp, color = colors.secondaryText)
        }

        if (order.transactionReference.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${strings.checkout.txRefLabel}: ${order.transactionReference}",
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = SuqiPrimary
            )
        }

        if (order.verifiedBy != null || order.verifiedAt != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.successBg)
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Icon(LucideIcons.ShieldCheck, contentDescription = null, tint = SuqiSuccess, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${strings.verifiedLabel}: ${order.verifiedBy.orEmpty()} · ${order.verifiedAt.orEmpty()}",
                    fontFamily = SuqiFontFamily,
                    fontSize = 11.sp,
                    color = SuqiSuccess
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (order.paymentStatus == "PENDING_VERIFICATION") {
                Button(
                    onClick = onVerify,
                    colors = ButtonDefaults.buttonColors(containerColor = SuqiSuccess),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Text(strings.seller.approvePaymentBtn, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            } else if (order.orderStatus in listOf("Confirmed", "Processing")) {
                Button(
                    onClick = onDispatch,
                    colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Text(String.format(strings.sellerOps.advanceToTemplate, strings.checkout.statusDispatched), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            } else if (order.orderStatus in listOf("Dispatched", "Out for Delivery")) {
                Button(
                    onClick = onDeliver,
                    colors = ButtonDefaults.buttonColors(containerColor = SuqiSuccess),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Text(String.format(strings.sellerOps.advanceToTemplate, strings.checkout.filterDelivered), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            } else {
                OutlinedButton(
                    onClick = onViewDetails,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Text(strings.orderDetails, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }

            OutlinedButton(
                onClick = onChat,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Icon(LucideIcons.MessageCircle, contentDescription = null, tint = colors.primaryText, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(strings.checkout.chatShortBtn, fontSize = 12.sp, color = colors.primaryText, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun SellerOrderDetailScreen(
    orderId: String,
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val isOffline by viewModel.isOffline.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var order by remember { mutableStateOf<OrderDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showRejectSheet by remember { mutableStateOf(false) }
    var rejectReason by remember { mutableStateOf("") }
    var zoomReceiptOrder by remember { mutableStateOf<OrderDto?>(null) }

    BackHandler(enabled = showRejectSheet) {
        showRejectSheet = false
    }

    fun loadOrder() {
        coroutineScope.launch {
            isLoading = true
            val res = viewModel.repository.getOrderDetails(orderId)
            applyOnMain {
                if (res.success) order = res.data
                isLoading = false
            }
        }
    }

    LaunchedEffect(orderId) { loadOrder() }

    SuqiScreen(
        title = order?.let { String.format(strings.checkout.orderNumberTemplate, it.orderReference) } ?: strings.orderDetails,
        subtitle = order?.createdAt,
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.padding(paddingValues).padding(top = 12.dp)) {
                SuqiSkeletonList(count = 4)
            }
        } else if (order == null) {
            SuqiEmptyState(
                icon = LucideIcons.PackageOpen,
                title = strings.checkout.noOrdersFound,
                subtitle = strings.checkout.noOrdersSub,
                ctaText = strings.common.goBackLabel,
                onCtaClick = { viewModel.goBack() }
            )
        } else {
            val current = order!!
            val localizedOrderStatus = when (current.orderStatus) {
                "Confirmed" -> strings.checkout.filterConfirmed
                "Processing" -> strings.checkout.statusProcessing
                "Dispatched" -> strings.checkout.statusDispatched
                "Out for Delivery" -> strings.checkout.statusOutForDelivery
                "Delivered" -> strings.checkout.filterDelivered
                "Cancelled" -> strings.checkout.filterCancelled
                else -> current.orderStatus
            }
            val localizedPaymentStatus = when (current.paymentStatus) {
                "VERIFIED" -> strings.verifiedLabel
                "REJECTED" -> strings.checkout.statusPaymentRejected
                else -> strings.checkout.statusPendingVerif
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Status & Summary
                item {
                    SuqiSectionCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(strings.checkout.orderTimelineTitle, fontSize = 12.sp, color = colors.secondaryText)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(localizedOrderStatus, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = colors.primaryText)
                            }
                            SuqiStatusBadge(
                                statusText = localizedPaymentStatus,
                                type = when (current.paymentStatus) {
                                    "VERIFIED" -> StatusType.SUCCESS
                                    "REJECTED" -> StatusType.ERROR
                                    else -> StatusType.WARNING
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${strings.extras.localizeDeliveryTitle(current.deliveryOptionTitle)} • ${strings.extras.localizeEta(current.deliveryEstimatedDays, com.example.ui.i18n.LocalSuqiLanguage.current)}",
                            fontSize = 13.sp,
                            color = colors.secondaryText
                        )
                    }
                }

                // Buyer & Shipping
                item {
                    SuqiSectionCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(strings.checkout.deliveryAddressTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                            TextButton(
                                onClick = { viewModel.navigate(ScreenDestination.ChatThread(current.shopId)) },
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Icon(LucideIcons.MessageCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.checkout.chatShortBtn, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Text(current.customerName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.primaryText)
                        Text(current.customerPhone, fontSize = 13.sp, color = colors.secondaryText)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(current.deliveryAddress, fontSize = 13.sp, color = colors.secondaryText)
                    }
                }

                // Payment Proof
                item {
                    SuqiSectionCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(strings.checkout.paymentProofSummaryTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                            AmountMatchChip(order = current, testTag = "seller_detail_amount_chip")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(strings.checkout.paymentMethodTitle, fontSize = 13.sp, color = colors.secondaryText)
                            Text(current.paymentMethodTitle, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.primaryText)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(strings.checkout.txRefLabel, fontSize = 13.sp, color = colors.secondaryText)
                            Text(current.transactionReference, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SuqiPrimary)
                        }
                        if (current.payerIdentifier.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(strings.checkout.senderPhoneLabel, fontSize = 13.sp, color = colors.secondaryText)
                                Text(current.payerIdentifier, fontSize = 13.sp, color = colors.primaryText)
                            }
                        }
                        if (current.transferredAmount != null && abs(current.transferredAmount - current.totalAmount) >= 1.0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(strings.checkout.transferAmountLabel, fontSize = 13.sp, color = colors.secondaryText)
                                Text(formatEtb(current.transferredAmount), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SuqiWarning)
                            }
                        }
                        if (current.receiptImageUri.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.imageBackdrop)
                                    .clickable { zoomReceiptOrder = current }
                                    .testTag("seller_detail_receipt_thumb")
                            ) {
                                AsyncImage(
                                    model = current.receiptImageUri,
                                    contentDescription = strings.checkout.receiptAttachedLabel,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        if (!current.rejectionReason.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("${strings.checkout.rejectionReasonTitle}: ${current.rejectionReason}", fontSize = 12.sp, color = SuqiError)
                        }
                    }
                }

                // Line Items
                item {
                    SuqiSectionCard {
                        Text(String.format(strings.checkout.itemsSummaryTemplate, current.items.size), fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                        Spacer(modifier = Modifier.height(10.dp))
                        current.items.forEachIndexed { idx, item ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = item.productImageUrl,
                                    contentDescription = item.productName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.imageBackdrop)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.productName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.primaryText)
                                    if (item.variation.isNotBlank()) {
                                        Text(item.variation, fontSize = 12.sp, color = colors.secondaryText)
                                    }
                                    Text("${item.quantity} × ${formatEtb(item.unitPrice)}", fontSize = 12.sp, color = colors.mutedText)
                                }
                                Text(formatEtb(item.totalPrice), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.primaryText)
                            }
                            if (idx < current.items.lastIndex) {
                                HorizontalDivider(color = colors.border)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = colors.border)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(strings.subtotalLabel, fontSize = 13.sp, color = colors.secondaryText)
                            Text(formatEtb(current.subtotal), fontSize = 13.sp, color = colors.primaryText)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(strings.deliveryFeeLabel, fontSize = 13.sp, color = colors.secondaryText)
                            Text(formatEtb(current.deliveryFee), fontSize = 13.sp, color = colors.primaryText)
                        }
                        if (current.discountAmount > 0.0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${strings.discountLabel} (${current.promoCodeUsed ?: ""})", fontSize = 13.sp, color = SuqiSuccess)
                                Text("-${formatEtb(current.discountAmount)}", fontSize = 13.sp, color = SuqiSuccess)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(strings.totalLabel, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.primaryText)
                            Text(formatEtb(current.totalAmount), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = SuqiPrimary)
                        }
                    }
                }

                // Actions
                item {
                    if (current.paymentStatus == "PENDING_VERIFICATION") {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        viewModel.repository.verifyPayment(current.id, mapOf("notes" to "Verified from order detail"))
                                        viewModel.showToast(strings.sellerOps.approvePaymentLabel)
                                        loadOrder()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SuqiSuccess),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("seller_detail_approve")
                            ) {
                                Text(strings.sellerOps.approvePaymentLabel, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            OutlinedButton(
                                onClick = { showRejectSheet = true },
                                border = BorderStroke(1.dp, SuqiError.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("seller_detail_reject")
                            ) {
                                Text(strings.seller.rejectPaymentBtn, color = SuqiError, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    } else if (current.paymentStatus == "VERIFIED" && current.orderStatus !in listOf("Delivered", "Cancelled")) {
                        val nextStatus = when (current.orderStatus) {
                            "Confirmed" -> "Processing"
                            "Processing" -> "Dispatched"
                            "Dispatched" -> "Out for Delivery"
                            "Out for Delivery" -> "Delivered"
                            else -> "Dispatched"
                        }
                        val localizedNextStatus = when (nextStatus) {
                            "Processing" -> strings.checkout.statusProcessing
                            "Dispatched" -> strings.checkout.statusDispatched
                            "Out for Delivery" -> strings.checkout.statusOutForDelivery
                            "Delivered" -> strings.checkout.filterDelivered
                            else -> nextStatus
                        }
                        SuqiButton(
                            text = String.format(strings.sellerOps.advanceToTemplate, localizedNextStatus),
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.repository.updateSellerOrderStatus(current.id, nextStatus)
                                    viewModel.showToast("$localizedNextStatus • #${current.orderReference}")
                                    loadOrder()
                                }
                            },
                            testTag = "seller_detail_advance",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    if (showRejectSheet && order != null) {
        SuqiBottomSheet(
            onDismissRequest = { showRejectSheet = false },
            title = strings.sellerOps.rejectPaymentTitle
        ) {
            SuqiTextField(
                value = rejectReason,
                onValueChange = { rejectReason = it },
                label = strings.sellerOps.rejectReasonRequired,
                singleLine = false
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = {
                    val trimmedReason = rejectReason.trim()
                    if (trimmedReason.isBlank()) {
                        viewModel.showToast(strings.sellerOps.rejectReasonRequired)
                        return@Button
                    }
                    coroutineScope.launch {
                        viewModel.repository.rejectPayment(order!!.id, mapOf("reason" to trimmedReason))
                        viewModel.showToast(strings.sellerOps.rejectPaymentTitle)
                        showRejectSheet = false
                        loadOrder()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SuqiError),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) {
                Text(strings.sellerOps.confirmRejectionBtn, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }

    zoomReceiptOrder?.let { docOrder ->
        SuqiDocumentViewer(
            documents = listOf(
                SuqiDocumentPage(
                    title = "${String.format(strings.checkout.orderNumberTemplate, docOrder.orderReference)} • ${formatEtb(docOrder.totalAmount)}",
                    imageUrl = docOrder.receiptImageUri,
                    note = "${docOrder.paymentMethodTitle} • ${strings.checkout.txRefLabel}: ${docOrder.transactionReference}"
                )
            ),
            onDismiss = { zoomReceiptOrder = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerInboxScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefsStore = remember(context) { SellerPreferencesStore(context) }
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val work = strings.sellerWork
    val isOffline by viewModel.isOffline.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var activeShopId by remember { mutableStateOf("") }
    var conversations by remember { mutableStateOf(viewModel.cachedSellerConversations) }
    var isLoading by remember { mutableStateOf(conversations.isEmpty()) }
    var selectedFilter by rememberSaveable { mutableStateOf("ALL") }
    var showNotifSheet by remember { mutableStateOf(false) }
    var notifPrefs by remember { mutableStateOf(prefsStore.loadNotificationPrefs()) }
    val listState = rememberLazyListState()

    BackHandler(enabled = showNotifSheet) {
        showNotifSheet = false
    }

    fun loadInbox() {
        coroutineScope.launch {
            isLoading = conversations.isEmpty()
            val sid = resolveActiveSellerShopId(viewModel)
            activeShopId = sid
            val res = viewModel.repository.getChatConversations()
            if (res.success && res.data != null) {
                val filteredList = if (sid.isBlank()) res.data
                else res.data.filter { it.shopId == sid }.ifEmpty { res.data }
                conversations = filteredList
                viewModel.cachedSellerConversations = filteredList
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadInbox() }

    val unreadCount = conversations.count { !it.isRead }
    val ordersCount = conversations.count { it.threadCategory == "ORDER" || !it.orderContextRef.isNullOrBlank() }
    val questionsCount = conversations.count { it.threadCategory != "ORDER" && it.orderContextRef.isNullOrBlank() }

    val filteredConversations = remember(conversations, selectedFilter) {
        when (selectedFilter) {
            "UNREAD" -> conversations.filter { !it.isRead }
            "ORDERS" -> conversations.filter { it.threadCategory == "ORDER" || !it.orderContextRef.isNullOrBlank() }
            "QUESTIONS" -> conversations.filter { it.threadCategory != "ORDER" && it.orderContextRef.isNullOrBlank() }
            else -> conversations
        }
    }

    SuqiScreen(
        title = strings.seller.navInbox,
        subtitle = strings.sellerOps.sellerInboxSub,
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        onRetryOffline = {
            viewModel.setOfflineMode(false)
            loadInbox()
        },
        headerActions = {
            IconButton(
                onClick = { showNotifSheet = true },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colors.secondaryBackground)
                    .testTag("inbox_notif_settings_btn")
            ) {
                Icon(LucideIcons.Settings, contentDescription = work.inboxNotifSettingsTitle, tint = colors.primaryText)
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            SuqiTabs(
                tabs = listOf(
                    SuqiTabItem("ALL", work.inboxFilterAll, conversations.size),
                    SuqiTabItem("UNREAD", work.inboxFilterUnread, unreadCount, showDot = unreadCount > 0),
                    SuqiTabItem("ORDERS", work.inboxFilterOrders, ordersCount),
                    SuqiTabItem("QUESTIONS", work.inboxFilterQuestions, questionsCount)
                ),
                selectedIndex = when (selectedFilter) {
                    "ALL" -> 0
                    "UNREAD" -> 1
                    "ORDERS" -> 2
                    else -> 3
                },
                onSelectTab = { idx ->
                    selectedFilter = when (idx) {
                        0 -> "ALL"
                        1 -> "UNREAD"
                        2 -> "ORDERS"
                        else -> "QUESTIONS"
                    }
                },
                style = SuqiTabStyle.Pill,
                scrollable = true,
                testTagPrefix = "seller_inbox_filter"
            )

            if (isLoading) {
                Box(modifier = Modifier.padding(top = 12.dp)) {
                    SuqiSkeletonList(count = 4)
                }
            } else if (filteredConversations.isEmpty()) {
                Box(modifier = Modifier.padding(bottom = SuqiBottomNavContentPadding)) {
                    SuqiEmptyState(
                        icon = LucideIcons.MessageCircle,
                        title = strings.sellerOps.noBuyerMsgsTitle,
                        subtitle = strings.sellerOps.noBuyerMsgsSub,
                        ctaText = strings.sellerOps.viewOrdersBtn,
                        onCtaClick = { viewModel.switchSellerTab(ScreenDestination.SellerOrders) },
                        testTag = "seller_inbox_empty"
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = SuqiBottomNavContentPadding + 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredConversations, key = { it.conversationId }) { conv ->
                        val nextRead = !conv.isRead
                        SuqiSwipeRow(
                            onSwipeRightAction = {
                                coroutineScope.launch {
                                    val res = viewModel.repository.markChatConversationRead(conv.shopId, nextRead, conv.conversationId)
                                    if (res.success) {
                                        conversations = conversations.map {
                                            if (it.conversationId == conv.conversationId) it.copy(isRead = nextRead) else it
                                        }
                                    }
                                }
                            },
                            rightActionLabel = if (conv.isRead) work.markUnreadLabel else work.markReadLabel
                        ) {
                            SuqiSectionCard(
                                onClick = {
                                    coroutineScope.launch {
                                        if (!conv.isRead) {
                                            viewModel.repository.markChatConversationRead(conv.shopId, true, conv.conversationId)
                                            conversations = conversations.map {
                                                if (it.conversationId == conv.conversationId) it.copy(isRead = true) else it
                                            }
                                        }
                                        viewModel.navigate(ScreenDestination.ChatThread(conv.shopId, conversationId = conv.conversationId))
                                    }
                                },
                                modifier = Modifier.testTag("seller_inbox_item_${conv.shopId}")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(colors.primaryTint),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(LucideIcons.MessageCircle, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(22.dp))
                                        if (!conv.isRead) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .size(11.dp)
                                                    .clip(CircleShape)
                                                    .background(SuqiPrimary)
                                                    .border(1.5.dp, colors.cardBackground, CircleShape)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = if (conv.shopId == activeShopId) strings.sellerOps.customerThreadLabel else conv.shopName,
                                                    fontFamily = SuqiFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = colors.primaryText,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (!conv.isRead) {
                                                    SuqiStatusBadge(work.unreadBadge, StatusType.PRIMARY)
                                                }
                                            }
                                            Text(conv.timestamp, fontSize = 11.sp, color = colors.mutedText)
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = conv.message,
                                            fontFamily = SuqiFontFamily,
                                            fontWeight = if (!conv.isRead) FontWeight.SemiBold else FontWeight.Normal,
                                            fontSize = 13.sp,
                                            color = if (!conv.isRead) colors.primaryText else colors.secondaryText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (!conv.orderContextRef.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(999.dp))
                                                    .background(colors.primaryTint)
                                                    .padding(horizontal = 9.dp, vertical = 3.dp)
                                                    .testTag("inbox_order_chip_${conv.orderContextRef}")
                                            ) {
                                                Icon(LucideIcons.Receipt, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(12.dp))
                                                Text(
                                                    text = buildString {
                                                        append("${work.orderContextPrefix} #${conv.orderContextRef}")
                                                        if (!conv.orderContextStatus.isNullOrBlank()) {
                                                            append(" · ${conv.orderContextStatus}")
                                                        }
                                                    },
                                                    fontFamily = SuqiFontFamily,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 11.sp,
                                                    color = SuqiPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                val res = viewModel.repository.markChatConversationRead(conv.shopId, nextRead, conv.conversationId)
                                                if (res.success) {
                                                    conversations = conversations.map {
                                                        if (it.conversationId == conv.conversationId) it.copy(isRead = nextRead) else it
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (conv.isRead) LucideIcons.ChevronRight else LucideIcons.CheckCheck,
                                            contentDescription = if (conv.isRead) strings.buyer.messagesTitle else work.markReadLabel,
                                            tint = if (conv.isRead) colors.mutedText else SuqiPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNotifSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNotifSheet = false },
            containerColor = colors.cardBackground,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = work.inboxNotifSettingsTitle,
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = colors.primaryText
                )
                HorizontalDivider(color = colors.border)
                SuqiSwitchRow(
                    title = work.notifNewOrderTitle,
                    subtitle = work.notifNewOrderSub,
                    icon = LucideIcons.ShoppingBag,
                    checked = notifPrefs.newOrder,
                    onCheckedChange = { checked ->
                        notifPrefs = notifPrefs.copy(newOrder = checked)
                        prefsStore.saveNotificationPrefs(notifPrefs)
                    },
                    testTag = "notif_switch_new_order"
                )
                SuqiSwitchRow(
                    title = work.notifPaymentSubmittedTitle,
                    subtitle = work.notifPaymentSubmittedSub,
                    icon = LucideIcons.Receipt,
                    checked = notifPrefs.paymentSubmitted,
                    onCheckedChange = { checked ->
                        notifPrefs = notifPrefs.copy(paymentSubmitted = checked)
                        prefsStore.saveNotificationPrefs(notifPrefs)
                    },
                    testTag = "notif_switch_payment"
                )
                SuqiSwitchRow(
                    title = work.notifLowStockTitle,
                    subtitle = work.notifLowStockSub,
                    icon = LucideIcons.Package,
                    checked = notifPrefs.lowStock,
                    onCheckedChange = { checked ->
                        notifPrefs = notifPrefs.copy(lowStock = checked)
                        prefsStore.saveNotificationPrefs(notifPrefs)
                    },
                    testTag = "notif_switch_low_stock"
                )
                SuqiSwitchRow(
                    title = work.notifNewReviewTitle,
                    subtitle = work.notifNewReviewSub,
                    icon = LucideIcons.Star,
                    checked = notifPrefs.newReview,
                    onCheckedChange = { checked ->
                        notifPrefs = notifPrefs.copy(newReview = checked)
                        prefsStore.saveNotificationPrefs(notifPrefs)
                    },
                    testTag = "notif_switch_review"
                )
                SuqiSwitchRow(
                    title = work.notifBuyerQuestionTitle,
                    subtitle = work.notifBuyerQuestionSub,
                    icon = LucideIcons.MessageCircle,
                    checked = notifPrefs.buyerQuestion,
                    onCheckedChange = { checked ->
                        notifPrefs = notifPrefs.copy(buyerQuestion = checked)
                        prefsStore.saveNotificationPrefs(notifPrefs)
                    },
                    testTag = "notif_switch_question"
                )
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

@Composable
fun SellerMoreScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val isOffline by viewModel.isOffline.collectAsState()
    var shop by remember { mutableStateOf<ShopDto?>(null) }

    LaunchedEffect(Unit) {
        val sid = resolveActiveSellerShopId(viewModel)
        if (sid.isNotBlank()) {
            val res = viewModel.repository.getShopDetails(sid)
            if (res.success) shop = res.data
        }
    }

    val listState = rememberLazyListState()

    SuqiScreen(
        title = strings.sellerOps.sellerToolsShopTitle,
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = SuqiBottomNavContentPadding + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SuqiSectionCard(
                    containerColor = colors.shopTint,
                    onClick = { viewModel.navigate(ScreenDestination.ShopSettings) }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(colors.primaryTint),
                            contentAlignment = Alignment.Center
                        ) {
                            val logo = shop?.logoUrl.orEmpty()
                            if (logo.isNotBlank()) {
                                AsyncImage(
                                    model = logo,
                                    contentDescription = shop?.name ?: strings.sellerOps.shopLogoCoverTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(LucideIcons.Store, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(26.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = shop?.name ?: strings.sellerOps.mySuqiShopFallback,
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = colors.primaryText
                            )
                            Text(
                                text = "${shop?.category ?: strings.buyer.categoryLabel} · ★ ${shop?.rating ?: 5.0}",
                                fontSize = 12.sp,
                                color = colors.secondaryText
                            )
                        }
                        SuqiStatusBadge(
                            statusText = if (shop?.isVerified == true) strings.verifiedLabel else strings.adminOps.activeStatus,
                            type = if (shop?.isVerified == true) StatusType.SUCCESS else StatusType.NEUTRAL
                        )
                    }
                }
            }

            item {
                SuqiSectionCard(padding = 12.dp) {
                    SuqiListRow(
                        icon = LucideIcons.SlidersHorizontal,
                        title = strings.sellerWork.inventoryTitle,
                        subtitle = strings.sellerWork.tapVariantsToExpandHint,
                        onClick = { viewModel.navigate(ScreenDestination.SellerInventory) },
                        testTag = "seller_more_inventory"
                    )
                    SuqiListRow(
                        icon = LucideIcons.ClipboardCheck,
                        title = strings.sellerOps.verifyQueueTitle,
                        subtitle = strings.sellerOps.verifyQueueSub,
                        onClick = { viewModel.navigate(ScreenDestination.VerificationQueue) },
                        testTag = "seller_more_verify_queue"
                    )
                    SuqiListRow(
                        icon = LucideIcons.Wallet,
                        title = strings.seller.paymentMethodsTitle,
                        subtitle = strings.sellerOps.paymentMethodsRowSub,
                        onClick = { viewModel.navigate(ScreenDestination.PaymentMethodsSetup) },
                        testTag = "seller_more_payment_methods"
                    )
                    SuqiListRow(
                        icon = LucideIcons.Truck,
                        title = strings.sellerOps.deliveryRulesTitle,
                        subtitle = strings.sellerOps.deliveryRulesSub,
                        onClick = { viewModel.navigate(ScreenDestination.DeliverySetup) },
                        testTag = "seller_more_delivery"
                    )
                    SuqiListRow(
                        icon = LucideIcons.BadgePercent,
                        title = strings.sellerOps.promotionsRowTitle,
                        subtitle = strings.sellerOps.promotionsRowSub,
                        onClick = { viewModel.navigate(ScreenDestination.Promotions) },
                        testTag = "seller_more_promotions"
                    )
                    SuqiListRow(
                        icon = LucideIcons.Store,
                        title = strings.sellerOps.shopBrandingRowTitle,
                        subtitle = strings.sellerOps.shopBrandingRowSub,
                        onClick = { viewModel.navigate(ScreenDestination.ShopSettings) },
                        testTag = "seller_more_shop_settings"
                    )
                    SuqiListRow(
                        icon = LucideIcons.BadgeCheck,
                        title = strings.sellerOps.merchantVerificationTitle,
                        subtitle = strings.sellerOps.merchantVerificationSub,
                        onClick = { viewModel.navigate(ScreenDestination.SellerVerification) },
                        testTag = "seller_more_verification"
                    )
                    SuqiListRow(
                        icon = LucideIcons.Sparkles,
                        title = strings.sellerOps.subscriptionPlanTitle,
                        subtitle = strings.sellerOps.subscriptionPlanSub,
                        onClick = { viewModel.navigate(ScreenDestination.SellerSubscription) },
                        testTag = "seller_more_subscription"
                    )
                }
            }

            item {
                SuqiSectionCard(padding = 12.dp) {
                    SuqiListRow(
                        icon = LucideIcons.ShoppingBag,
                        title = strings.sellerOps.switchToBuyerTitle,
                        subtitle = strings.sellerOps.switchToBuyerSub,
                        onClick = {
                            viewModel.setActiveRole("CUSTOMER")
                            viewModel.popToRoot(ScreenDestination.Home)
                        },
                        testTag = "seller_switch_to_buyer"
                    )
                    SuqiListRow(
                        icon = LucideIcons.Settings,
                        title = strings.sellerOps.appSettingsRowTitle,
                        subtitle = strings.sellerOps.appSettingsRowSub,
                        onClick = { viewModel.navigate(ScreenDestination.AppSettings) },
                        testTag = "seller_more_app_settings"
                    )
                }
            }
        }
    }
}
