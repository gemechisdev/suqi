package com.example.ui.screens

import com.example.ui.i18n.localizeDeliveryTitle
import com.example.ui.i18n.localizeEta
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.*
import com.example.ui.i18n.LocalSuqiLanguage
import com.example.ui.i18n.localizedName
import com.example.ui.i18n.suqiStrings
import com.example.ui.theme.*
import kotlinx.coroutines.launch

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

    var isLoading by remember { mutableStateOf(true) }
    var dashboardData by remember { mutableStateOf<Map<String, Any>>(emptyMap()) }
    var pendingQueue by remember { mutableStateOf<List<OrderDto>>(emptyList()) }
    var sellerOrders by remember { mutableStateOf<List<OrderDto>>(emptyList()) }
    var sellerProducts by remember { mutableStateOf<List<ProductDto>>(emptyList()) }
    var selectedPeriod by remember { mutableStateOf("7D") }

    fun loadDashboard() {
        coroutineScope.launch {
            isLoading = true
            val dashRes = viewModel.repository.getSellerDashboard()
            if (dashRes.success && dashRes.data != null) {
                dashboardData = dashRes.data
            }
            val qRes = viewModel.repository.getSellerVerificationQueue()
            if (qRes.success && qRes.data != null) {
                pendingQueue = qRes.data
            }
            val ordersRes = viewModel.repository.getSellerOrders()
            if (ordersRes.success && ordersRes.data != null) {
                sellerOrders = ordersRes.data
            }
            val prodRes = viewModel.repository.getSellerProducts()
            if (prodRes.success && prodRes.data != null) {
                sellerProducts = prodRes.data
            }
            isLoading = false
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
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QuickActionTile(
                                icon = LucideIcons.Truck,
                                title = strings.checkout.stepDelivery,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.navigate(ScreenDestination.DeliverySetup) }
                            )
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

@Composable
fun AddEditProductScreen(
    productId: String? = null,
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val lang = LocalSuqiLanguage.current
    val isOffline by viewModel.isOffline.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf("cat_electronics") }
    var categories by remember(lang) {
        mutableStateOf(
            com.example.data.model.MarketplaceCategories.all.map { it.id to it.toDto().localizedName(lang) }
        )
    }
    var priceStr by remember { mutableStateOf("") }
    var discountStr by remember { mutableStateOf("0") }
    var stockStr by remember { mutableStateOf("10") }
    var stockHold by remember { mutableStateOf(true) }
    var description by remember { mutableStateOf("") }

    var selectedTax by remember { mutableStateOf("15% Standard VAT Included") }
    val taxOptions = remember { listOf("15% Standard VAT Included", "0% Exempt / Turn-Over Tax (TOT)", "Custom (5%)") }

    val media = remember { mutableStateListOf<MediaItem>() }
    val form = remember { ProductFormState() }
    var showMissingPaymentDialog by remember { mutableStateOf(false) }

    var showMultilingual by remember { mutableStateOf(false) }
    var nameAmharic by remember { mutableStateOf("") }
    var descAmharic by remember { mutableStateOf("") }
    var nameOromo by remember { mutableStateOf("") }
    var descOromo by remember { mutableStateOf("") }

    LaunchedEffect(productId, lang) {
        val catRes = viewModel.repository.getCategories()
        if (catRes.success && !catRes.data.isNullOrEmpty()) {
            categories = catRes.data.filter { it.isActive }.map { it.id to it.localizedName(lang) }
        }
        if (productId != null) {
            val res = viewModel.repository.getProductDetails(productId)
            val p = res.data
            if (res.success && p != null) {
                name = p.name
                categoryId = p.categoryId
                priceStr = if (p.price % 1.0 == 0.0) p.price.toLong().toString() else p.price.toString()
                discountStr = p.discountPercent.toString()
                stockStr = p.stockQuantity.toString()
                stockHold = p.stockHoldEnabled
                description = p.description
                nameAmharic = p.nameAmharic
                nameOromo = p.nameAfaanOromo
                descAmharic = p.descriptionAmharic
                descOromo = p.descriptionAfaanOromo
                media.clear()
                media.addAll(p.details.media)
                form.load(p.details)
            }
        }
    }

    SuqiScreen(
        title = if (productId == null) strings.seller.addProductTitle else strings.seller.editProductTitle,
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        bottomBar = {
            Surface(
                color = colors.cardBackground,
                shadowElevation = if (colors.isDark) 0.dp else 12.dp,
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SuqiButton(
                        text = if (productId == null) strings.seller.publishProductBtn else strings.seller.saveChangesBtn,
                        onClick = {
                            coroutineScope.launch {
                                if (productId == null) {
                                    val activeShopId = resolveActiveSellerShopId(viewModel)
                                    val payRes = viewModel.repository.getShopPaymentMethods(activeShopId)
                                    if (activeShopId.isBlank() || (payRes.success && payRes.data.orEmpty().none { it.isEnabled })) {
                                        showMissingPaymentDialog = true
                                        return@launch
                                    }
                                }

                                val p = priceStr.toDoubleOrNull() ?: 0.0
                                val d = discountStr.toIntOrNull() ?: 0
                                val s = stockStr.toIntOrNull() ?: 0
                                when {
                                    media.none { !it.isVideo } -> { viewModel.showToast(strings.extras.toastAddCoverPhoto); return@launch }
                                    name.isBlank() -> { viewModel.showToast(strings.extras.toastEnterProductName); return@launch }
                                    p <= 0.0 -> { viewModel.showToast(strings.extras.toastEnterValidPrice); return@launch }
                                    d !in 0..90 -> { viewModel.showToast(strings.extras.toastDiscountRange); return@launch }
                                    description.isBlank() -> { viewModel.showToast(strings.extras.toastEnterDescription); return@launch }
                                }
                                form.missingRequired()?.let {
                                    viewModel.showToast(String.format(strings.extras.toastFieldRequiredTemplate, it.label))
                                    return@launch
                                }

                                val payload = mapOf<String, Any>(
                                    "name" to name.trim(),
                                    "nameAmharic" to nameAmharic,
                                    "nameAfaanOromo" to nameOromo,
                                    "price" to p,
                                    "discountPercent" to d,
                                    "stockQuantity" to s,
                                    "stockHoldEnabled" to stockHold,
                                    "categoryId" to categoryId,
                                    "tax" to selectedTax,
                                    "description" to description,
                                    "descriptionAmharic" to descAmharic,
                                    "descriptionAfaanOromo" to descOromo,
                                    "detailsJson" to form.toDetails(media.toList()).toJson()
                                )
                                val res = if (productId == null) {
                                    viewModel.repository.createProduct(payload)
                                } else {
                                    viewModel.repository.updateProduct(productId, payload)
                                }
                                if (res.success) {
                                    viewModel.showToast(if (productId == null) strings.extras.toastProductSaved else strings.extras.toastProductUpdated)
                                    viewModel.goBack()
                                } else {
                                    viewModel.showToast(res.message ?: strings.extras.toastProductSaveFailed)
                                }
                            }
                        },
                        testTag = "save_product_button",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SuqiMediaEditor(
                    media = media,
                    onMessage = { viewModel.showToast(it) }
                )
            }

            item {
                ItemTypeSelector(
                    state = form,
                    onTypeChosen = { type ->
                        form.applyItemType(type)
                        if (type.categoryId.isNotBlank()) categoryId = type.categoryId
                    }
                )
            }

            item {
                Text(
                    text = "${strings.buyer.categoryLabel} *",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = colors.primaryText
                )
                Spacer(modifier = Modifier.height(8.dp))
                SuqiFilterChips(
                    items = categories,
                    selectedKey = categoryId,
                    onSelect = { categoryId = it },
                    testTagPrefix = "product_cat_chip"
                )
            }

            item {
                SuqiSectionCard {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        SuqiTextField(value = name, onValueChange = { name = it }, label = "${strings.seller.paramNameRequiredLabel} (EN)")

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SuqiTextField(
                                value = priceStr,
                                onValueChange = { priceStr = it },
                                label = "${strings.seller.priceEtbLabel} *",
                                modifier = Modifier.weight(1f)
                            )
                            SuqiTextField(
                                value = discountStr,
                                onValueChange = { discountStr = it },
                                label = strings.discountLabel,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Column {
                            Text(
                                text = strings.extras.vatLabel,
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = colors.primaryText
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(taxOptions) { opt ->
                                    val isSel = opt == selectedTax
                                    Box(
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSel) colors.primaryTint else colors.secondaryBackground)
                                            .border(1.dp, if (isSel) SuqiPrimary else colors.border, RoundedCornerShape(12.dp))
                                            .clickable { selectedTax = opt }
                                            .padding(horizontal = 12.dp, vertical = 7.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = opt,
                                            fontFamily = SuqiFontFamily,
                                            fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal,
                                            fontSize = 12.sp,
                                            color = if (isSel) SuqiPrimary else colors.primaryText
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SuqiTextField(
                                value = stockStr,
                                onValueChange = { stockStr = it },
                                label = "${strings.seller.stockLabel} *",
                                modifier = Modifier.weight(1f)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.seller.stockLabel,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = colors.primaryText
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = stockHold,
                                        onCheckedChange = { stockHold = it },
                                        colors = SwitchDefaults.colors(checkedTrackColor = SuqiPrimary)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (stockHold) strings.common.yesLabel else strings.common.noLabel,
                                        fontFamily = SuqiFontFamily,
                                        fontSize = 12.sp,
                                        color = colors.secondaryText
                                    )
                                }
                            }
                        }

                        SuqiTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = "${strings.description} (EN) *",
                            singleLine = false
                        )
                    }
                }
            }

            item { ParametersEditor(state = form) }
            item {
                VariantsEditor(
                    state = form,
                    media = media,
                    onMessage = { viewModel.showToast(it) }
                )
            }

            item {
                SuqiSectionCard(containerColor = colors.secondaryBackground) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable { showMultilingual = !showMultilingual },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(LucideIcons.Sparkles, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.sellerOps.bilingualInfoTitle,
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = colors.primaryText
                            )
                        }
                        Icon(
                            imageVector = LucideIcons.ChevronDown,
                            contentDescription = strings.sellerOps.bilingualInfoTitle,
                            tint = colors.secondaryText,
                            modifier = Modifier.graphicsLayer { rotationZ = if (showMultilingual) 180f else 0f }
                        )
                    }

                    if (showMultilingual) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(strings.sellerOps.amharicSectionLabel, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SuqiPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        SuqiTextField(value = nameAmharic, onValueChange = { nameAmharic = it }, label = strings.seller.paramNameRequiredLabel)
                        Spacer(modifier = Modifier.height(8.dp))
                        SuqiTextField(value = descAmharic, onValueChange = { descAmharic = it }, label = strings.description, singleLine = false)

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(strings.sellerOps.afaanOromoSectionLabel, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SuqiPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        SuqiTextField(value = nameOromo, onValueChange = { nameOromo = it }, label = strings.seller.paramNameRequiredLabel)
                        Spacer(modifier = Modifier.height(8.dp))
                        SuqiTextField(value = descOromo, onValueChange = { descOromo = it }, label = strings.description, singleLine = false)
                    }
                }
            }
        }
    }

    if (showMissingPaymentDialog) {
        AlertDialog(
            onDismissRequest = { showMissingPaymentDialog = false },
            icon = { Icon(LucideIcons.ShieldCheck, contentDescription = null, tint = SuqiPrimary) },
            title = { Text(strings.sellerOps.paymentMethodRequiredTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, color = colors.primaryText) },
            text = {
                Text(
                    strings.sellerOps.paymentMethodRequiredBody,
                    fontFamily = SuqiFontFamily,
                    fontSize = 14.sp,
                    color = colors.secondaryText
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMissingPaymentDialog = false
                        viewModel.navigate(ScreenDestination.PaymentMethodsSetup)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(strings.sellerOps.setupPaymentMethodsBtn, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showMissingPaymentDialog = false },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(strings.cancelLabel, fontFamily = SuqiFontFamily)
                }
            },
            containerColor = colors.cardBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun ProductManagementScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val lang = LocalSuqiLanguage.current
    val isOffline by viewModel.isOffline.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var products by remember { mutableStateOf<List<ProductDto>>(emptyList()) }
    var selectedTab by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var actionProduct by remember { mutableStateOf<ProductDto?>(null) }
    var confirmDeleteProduct by remember { mutableStateOf<ProductDto?>(null) }

    BackHandler(enabled = actionProduct != null) {
        actionProduct = null
    }

    fun loadProducts() {
        coroutineScope.launch {
            isLoading = true
            val res = viewModel.repository.getSellerProducts()
            if (res.success && res.data != null) {
                products = res.data
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadProducts()
    }

    val tabs = remember(products, strings) {
        listOf(
            "ALL" to String.format(strings.sellerOps.allCountTemplate, products.size),
            "ACTIVE" to String.format(strings.sellerOps.activeCountTemplate, products.count { it.status == "ACTIVE" }),
            "PAUSED" to String.format(strings.sellerOps.pausedCountTemplate, products.count { it.status == "PAUSED" }),
            "ARCHIVED" to String.format(strings.sellerOps.archivedCountTemplate, products.count { it.status == "ARCHIVED" }),
            "LOW_STOCK" to String.format(strings.sellerOps.lowStockCountTemplate, products.count { it.stockQuantity in 0..5 })
        )
    }

    val filtered = remember(products, selectedTab, searchQuery, lang) {
        products.filter { prod ->
            val matchesStatus = when (selectedTab) {
                "ALL" -> true
                "ACTIVE" -> prod.status == "ACTIVE"
                "PAUSED" -> prod.status == "PAUSED"
                "ARCHIVED" -> prod.status == "ARCHIVED"
                "LOW_STOCK" -> prod.stockQuantity in 0..5
                else -> true
            }
            val matchesQuery = searchQuery.isBlank() ||
                prod.name.contains(searchQuery, ignoreCase = true) ||
                prod.localizedName(lang).contains(searchQuery, ignoreCase = true) ||
                prod.subCategory.contains(searchQuery, ignoreCase = true)
            matchesStatus && matchesQuery
        }
    }

    SuqiScreen(
        title = strings.seller.productsTabTitle,
        subtitle = String.format(strings.sellerOps.catalogItemsSubTemplate, products.size),
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        onRetryOffline = {
            viewModel.setOfflineMode(false)
            loadProducts()
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.navigate(ScreenDestination.AddEditProduct()) },
                containerColor = SuqiPrimary,
                contentColor = Color.White,
                modifier = Modifier
                    .padding(bottom = SuqiBottomNavContentPadding - 16.dp)
                    .testTag("seller_add_product_fab")
            ) {
                Icon(LucideIcons.Plus, contentDescription = strings.seller.addProductTitle)
            }
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
                placeholder = strings.sellerOps.catalogSearchHint,
                testTag = "seller_products_search",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            SuqiFilterChips(
                items = tabs,
                selectedKey = selectedTab,
                onSelect = { selectedTab = it },
                testTagPrefix = "seller_product_tab"
            )

            if (isLoading) {
                Box(modifier = Modifier.padding(top = 12.dp)) {
                    SuqiSkeletonList(count = 4)
                }
            } else if (filtered.isEmpty()) {
                SuqiEmptyState(
                    icon = LucideIcons.PackageOpen,
                    title = if (products.isEmpty()) strings.sellerOps.noProductsYetTitle else strings.buyer.noMatchingProducts,
                    subtitle = if (products.isEmpty()) strings.sellerOps.noProductsYetSub else strings.buyer.noMatchingProductsSub,
                    ctaText = if (products.isEmpty()) strings.sellerOps.addFirstProductBtn else strings.sellerOps.showAllProductsBtn,
                    onCtaClick = {
                        if (products.isEmpty()) viewModel.navigate(ScreenDestination.AddEditProduct())
                        else {
                            selectedTab = "ALL"
                            searchQuery = ""
                        }
                    },
                    testTag = "seller_products_empty"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 10.dp,
                        bottom = SuqiBottomNavContentPadding + 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { prod ->
                        val prodTitle = prod.localizedName(lang)
                        SuqiSectionCard(
                            padding = 12.dp,
                            onClick = { viewModel.navigate(ScreenDestination.AddEditProduct(prod.id)) },
                            modifier = Modifier.testTag("seller_product_card_${prod.id}")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(colors.imageBackdrop)
                                ) {
                                    AsyncImage(
                                        model = prod.primaryImageUrl,
                                        contentDescription = prodTitle,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = prodTitle,
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = colors.primaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = formatEtb(prod.price),
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = SuqiPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val statusLabel = when (prod.status) {
                                            "ACTIVE" -> strings.seller.statusActive
                                            "PAUSED" -> strings.seller.statusPaused
                                            "ARCHIVED" -> strings.seller.statusArchived
                                            "DRAFT" -> strings.seller.statusDraft
                                            else -> prod.status
                                        }
                                        SuqiStatusBadge(
                                            statusText = statusLabel,
                                            type = when (prod.status) {
                                                "ACTIVE" -> StatusType.SUCCESS
                                                "PAUSED" -> StatusType.WARNING
                                                else -> StatusType.NEUTRAL
                                            }
                                        )
                                        Text(
                                            text = String.format(strings.sellerOps.stockPrefixTemplate, prod.stockQuantity),
                                            fontFamily = SuqiFontFamily,
                                            fontSize = 12.sp,
                                            color = if (prod.stockQuantity <= 5) SuqiWarning else colors.secondaryText
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { actionProduct = prod },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("product_actions_${prod.id}")
                                ) {
                                    Icon(
                                        imageVector = LucideIcons.MoreVertical,
                                        contentDescription = prodTitle,
                                        tint = colors.primaryText
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    actionProduct?.let { target ->
        val targetTitle = target.localizedName(lang)
        SuqiBottomSheet(
            onDismissRequest = { actionProduct = null },
            title = targetTitle
        ) {
            SuqiListRow(
                icon = LucideIcons.Pencil,
                title = strings.sellerOps.editProductAction,
                subtitle = strings.sellerOps.editProductActionSub,
                onClick = {
                    actionProduct = null
                    viewModel.navigate(ScreenDestination.AddEditProduct(target.id))
                },
                testTag = "product_sheet_edit"
            )
            SuqiListRow(
                icon = if (target.status == "ACTIVE") LucideIcons.Pause else LucideIcons.CheckCircle,
                title = if (target.status == "ACTIVE") strings.sellerOps.pauseListingAction else strings.sellerOps.activateListingAction,
                subtitle = if (target.status == "ACTIVE") strings.sellerOps.pauseListingActionSub else strings.sellerOps.activateListingActionSub,
                onClick = {
                    val nextStatus = if (target.status == "ACTIVE") "PAUSED" else "ACTIVE"
                    actionProduct = null
                    coroutineScope.launch {
                        val res = viewModel.repository.updateProductStatus(target.id, nextStatus)
                        viewModel.showToast(if (res.success) targetTitle else strings.common.failedToAddToast)
                        loadProducts()
                    }
                },
                testTag = "product_sheet_toggle"
            )
            if (target.status != "ARCHIVED") {
                SuqiListRow(
                    icon = LucideIcons.Package,
                    title = strings.sellerOps.archiveListingAction,
                    subtitle = strings.sellerOps.archiveListingActionSub,
                    onClick = {
                        actionProduct = null
                        coroutineScope.launch {
                            viewModel.repository.updateProductStatus(target.id, "ARCHIVED")
                            viewModel.showToast(targetTitle)
                            loadProducts()
                        }
                    },
                    testTag = "product_sheet_archive"
                )
            }
            SuqiListRow(
                icon = LucideIcons.Eye,
                title = strings.sellerOps.previewAsBuyerAction,
                subtitle = strings.sellerOps.previewAsBuyerActionSub,
                onClick = {
                    actionProduct = null
                    viewModel.navigate(ScreenDestination.ProductDetails(target.id))
                },
                testTag = "product_sheet_preview"
            )
            SuqiListRow(
                icon = LucideIcons.Trash2,
                title = strings.sellerOps.deleteProductAction,
                subtitle = strings.sellerOps.deleteProductActionSub,
                iconTint = SuqiError,
                iconBackground = colors.errorBg,
                onClick = {
                    actionProduct = null
                    confirmDeleteProduct = target
                },
                testTag = "product_sheet_delete"
            )
        }
    }

    confirmDeleteProduct?.let { target ->
        val targetTitle = target.localizedName(lang)
        AlertDialog(
            onDismissRequest = { confirmDeleteProduct = null },
            title = { Text(strings.sellerOps.deleteProductDialogTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, color = colors.primaryText) },
            text = {
                Text(
                    String.format(strings.sellerOps.deleteProductDialogBodyTemplate, targetTitle),
                    fontFamily = SuqiFontFamily,
                    color = colors.secondaryText
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDeleteProduct = null
                        coroutineScope.launch {
                            val res = viewModel.repository.deleteProduct(target.id)
                            viewModel.showToast(if (res.success) strings.deleteLabel else (res.message ?: strings.deleteLabel))
                            loadProducts()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuqiError),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(strings.deleteLabel, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmDeleteProduct = null },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(strings.cancelLabel)
                }
            },
            containerColor = colors.cardBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun VerificationQueueScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val isOffline by viewModel.isOffline.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var queue by remember { mutableStateOf<List<OrderDto>>(emptyList()) }
    var selectedOrderForReject by remember { mutableStateOf<OrderDto?>(null) }
    var rejectReason by remember { mutableStateOf(strings.extras.rejectReasonRefNotFound) }
    var zoomReceiptUri by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = selectedOrderForReject != null) {
        selectedOrderForReject = null
    }

    val quickRejectReasons = remember(strings) {
        listOf(strings.extras.rejectReasonRefNotFound, strings.extras.rejectReasonBlurry, strings.extras.rejectReasonAmountMismatch, strings.extras.rejectReasonDuplicateRef)
    }

    fun loadQueue() {
        coroutineScope.launch {
            isLoading = true
            val res = viewModel.repository.getSellerVerificationQueue()
            if (res.success && res.data != null) {
                queue = res.data
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadQueue()
    }

    SuqiScreen(
        title = strings.seller.verificationQueueTitle,
        subtitle = String.format(strings.sellerOps.waitingReviewSubTemplate, queue.size),
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        onRetryOffline = {
            viewModel.setOfflineMode(false)
            loadQueue()
        },
        modifier = modifier
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.padding(paddingValues).padding(top = 12.dp)) {
                SuqiSkeletonList(count = 3)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    SuqiSectionCard(containerColor = colors.warningBg, padding = 12.dp, cornerRadius = 16.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(LucideIcons.Clock, contentDescription = null, tint = SuqiWarning, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = String.format(strings.sellerOps.waitingReviewSubTemplate, queue.size),
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = colors.primaryText
                            )
                        }
                    }
                }

                if (queue.isEmpty()) {
                    item {
                        SuqiEmptyState(
                            icon = LucideIcons.CheckCircle,
                            title = strings.buyer.noNotificationsTitle,
                            subtitle = strings.sellerOps.verifyQueueSub,
                            ctaText = strings.seller.ordersTabTitle,
                            onCtaClick = { viewModel.navigate(ScreenDestination.SellerOrders) },
                            testTag = "verification_queue_empty"
                        )
                    }
                } else {
                    items(queue, key = { it.id }) { order ->
                        SuqiSectionCard(modifier = Modifier.testTag("verify_card_${order.id}")) {
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
                                        text = order.createdAt.take(16),
                                        fontFamily = SuqiFontFamily,
                                        fontSize = 11.sp,
                                        color = colors.mutedText
                                    )
                                }
                                SuqiStatusBadge(strings.checkout.filterPending, StatusType.WARNING)
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

                            if (order.receiptImageUri.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(colors.imageBackdrop)
                                        .clickable { zoomReceiptUri = order.receiptImageUri }
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

    zoomReceiptUri?.let { uri ->
        ReceiptZoomViewerDialog(imageUri = uri, onDismiss = { zoomReceiptUri = null })
    }
}

@Composable
private fun ReceiptZoomViewerDialog(
    imageUri: String,
    onDismiss: () -> Unit
) {
    val strings = suqiStrings()
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f))
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val nextScale = (scale * zoom).coerceIn(1f, 4f)
                        scale = nextScale
                        if (nextScale > 1f) {
                            offsetX += pan.x
                            offsetY += pan.y
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                }
        ) {
            AsyncImage(
                model = imageUri,
                contentDescription = strings.checkout.receiptAttachedLabel,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        scale = 1f
                        offsetX = 0f
                        offsetY = 0f
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                ) {
                    Icon(LucideIcons.RotateCcw, contentDescription = strings.common.resetLabel, tint = Color.White)
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                ) {
                    Icon(LucideIcons.X, contentDescription = strings.common.closeViewerDesc, tint = Color.White)
                }
            }
            Text(
                text = strings.common.viewerGestureHint,
                fontFamily = SuqiFontFamily,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.75f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
            )
        }
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
fun DeliverySetupScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val isOffline by viewModel.isOffline.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var activeShopId by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    var stdId by remember { mutableStateOf("") }
    var expId by remember { mutableStateOf("") }
    var pickId by remember { mutableStateOf("") }

    var standardEnabled by remember { mutableStateOf(true) }
    var standardFee by remember { mutableStateOf("50") }
    var standardEta by remember { mutableStateOf("1-3 business days") }

    var expressEnabled by remember { mutableStateOf(false) }
    var expressFee by remember { mutableStateOf("120") }
    var expressCutoff by remember { mutableStateOf("Same-day before 2:00 PM") }

    var pickupEnabled by remember { mutableStateOf(true) }
    var pickupAddress by remember { mutableStateOf("") }

    var freeThreshold by remember { mutableStateOf("0") }
    var coverageAreas by remember { mutableStateOf("") }
    var requireReceiptScreenshot by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        val sid = resolveActiveSellerShopId(viewModel)
        activeShopId = sid
        if (sid.isNotBlank()) {
            val shopRes = viewModel.repository.getShopDetails(sid)
            shopRes.data?.let { s ->
                pickupAddress = s.address
                coverageAreas = s.coverageSubCitiesCsv.ifBlank { "${s.city} (${s.subCity})" }
                freeThreshold = if (s.freeDeliveryThreshold > 0.0) s.freeDeliveryThreshold.toLong().toString() else "0"
                requireReceiptScreenshot = s.requireReceiptScreenshot
            }
            val delRes = viewModel.repository.getShopDeliveryOptions(sid)
            val options = delRes.data.orEmpty()
            options.firstOrNull { it.optionType == "STANDARD" }?.let {
                stdId = it.id
                standardEnabled = it.isEnabled
                standardFee = it.fee.toLong().toString()
                standardEta = it.estimatedTime
            }
            options.firstOrNull { it.optionType == "EXPRESS" }?.let {
                expId = it.id
                expressEnabled = it.isEnabled
                expressFee = it.fee.toLong().toString()
                expressCutoff = it.estimatedTime
            }
            options.firstOrNull { it.optionType == "PICKUP" }?.let {
                pickId = it.id
                pickupEnabled = it.isEnabled
                if (it.estimatedTime.isNotBlank()) pickupAddress = it.estimatedTime
            }
        }
        isLoading = false
    }

    SuqiScreen(
        title = strings.sellerOps.deliveryRulesTitle,
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
                        text = strings.saveLabel,
                        onClick = {
                            coroutineScope.launch {
                                if (activeShopId.isBlank()) {
                                    viewModel.showToast(strings.seller.sellerRestrictedTitle)
                                    return@launch
                                }
                                viewModel.repository.saveShopDeliveryOption(
                                    ShopDeliveryOptionDto(
                                        id = stdId.ifBlank { "del_${activeShopId}_std" },
                                        shopId = activeShopId,
                                        optionType = "STANDARD",
                                        title = "Standard Courier Delivery",
                                        fee = standardFee.toDoubleOrNull() ?: 50.0,
                                        estimatedTime = standardEta.trim().ifBlank { "1-3 business days" },
                                        isEnabled = standardEnabled
                                    )
                                )
                                viewModel.repository.saveShopDeliveryOption(
                                    ShopDeliveryOptionDto(
                                        id = expId.ifBlank { "del_${activeShopId}_exp" },
                                        shopId = activeShopId,
                                        optionType = "EXPRESS",
                                        title = "Express Same-Day Delivery",
                                        fee = expressFee.toDoubleOrNull() ?: 120.0,
                                        estimatedTime = expressCutoff.trim().ifBlank { "Same-day" },
                                        isEnabled = expressEnabled
                                    )
                                )
                                viewModel.repository.saveShopDeliveryOption(
                                    ShopDeliveryOptionDto(
                                        id = pickId.ifBlank { "del_${activeShopId}_pickup" },
                                        shopId = activeShopId,
                                        optionType = "PICKUP",
                                        title = "In-Store Customer Pickup",
                                        fee = 0.0,
                                        estimatedTime = pickupAddress.trim().ifBlank { "Ready in 2 hours" },
                                        isEnabled = pickupEnabled
                                    )
                                )
                                viewModel.repository.updateShopSettings(
                                    mapOf(
                                        "shopId" to activeShopId,
                                        "freeDeliveryThreshold" to (freeThreshold.toDoubleOrNull() ?: 0.0),
                                        "coverageSubCitiesCsv" to coverageAreas.trim(),
                                        "requireReceiptScreenshot" to requireReceiptScreenshot
                                    )
                                )
                                viewModel.showToast(strings.saveLabel)
                                viewModel.goBack()
                            }
                        },
                        testTag = "save_delivery_settings_button",
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    SuqiSectionCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(LucideIcons.Receipt, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(strings.checkout.receiptScreenshotRequired, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                                    Text(strings.checkout.receiptRequiredHint, fontSize = 12.sp, color = colors.secondaryText)
                                }
                            }
                            Switch(
                                checked = requireReceiptScreenshot,
                                onCheckedChange = { requireReceiptScreenshot = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = SuqiPrimary),
                                modifier = Modifier.testTag("toggle_require_receipt")
                            )
                        }
                    }
                }

                item {
                    SuqiSectionCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(LucideIcons.Truck, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(strings.sellerOps.standardDeliveryTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                                    Text(strings.sellerOps.standardDeliveryDesc, fontSize = 12.sp, color = colors.secondaryText)
                                }
                            }
                            Switch(
                                checked = standardEnabled,
                                onCheckedChange = { standardEnabled = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = SuqiPrimary)
                            )
                        }
                        if (standardEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                SuqiTextField(value = standardFee, onValueChange = { standardFee = it }, label = strings.sellerOps.feeEtbLabel, modifier = Modifier.weight(1f))
                                SuqiTextField(value = standardEta, onValueChange = { standardEta = it }, label = strings.sellerOps.etaWindowLabel, modifier = Modifier.weight(1.4f))
                            }
                        }
                    }
                }

                item {
                    SuqiSectionCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(LucideIcons.Zap, contentDescription = null, tint = SuqiWarning, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(strings.sellerOps.expressDeliveryTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                                    Text(strings.sellerOps.expressDeliveryDesc, fontSize = 12.sp, color = colors.secondaryText)
                                }
                            }
                            Switch(
                                checked = expressEnabled,
                                onCheckedChange = { expressEnabled = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = SuqiPrimary)
                            )
                        }
                        if (expressEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                SuqiTextField(value = expressFee, onValueChange = { expressFee = it }, label = strings.sellerOps.feeEtbLabel, modifier = Modifier.weight(1f))
                                SuqiTextField(value = expressCutoff, onValueChange = { expressCutoff = it }, label = strings.sellerOps.cutoffWindowLabel, modifier = Modifier.weight(1.4f))
                            }
                        }
                    }
                }

                item {
                    SuqiSectionCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(LucideIcons.Store, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(strings.sellerOps.pickupTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                                    Text(strings.sellerOps.pickupDesc, fontSize = 12.sp, color = colors.secondaryText)
                                }
                            }
                            Switch(
                                checked = pickupEnabled,
                                onCheckedChange = { pickupEnabled = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = SuqiPrimary)
                            )
                        }
                        if (pickupEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            SuqiTextField(value = pickupAddress, onValueChange = { pickupAddress = it }, label = strings.sellerOps.pickupLandmarkLabel, singleLine = false)
                        }
                    }
                }

                item {
                    SuqiSectionCard {
                        Text(strings.sellerOps.coverageSectionTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                        Spacer(modifier = Modifier.height(10.dp))
                        SuqiTextField(value = freeThreshold, onValueChange = { freeThreshold = it }, label = strings.sellerOps.freeDeliveryMinLabel)
                        Spacer(modifier = Modifier.height(10.dp))
                        SuqiTextField(value = coverageAreas, onValueChange = { coverageAreas = it }, label = strings.sellerOps.coverageSubCitiesLabel, singleLine = false)
                    }
                }
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
fun ShopSettingsScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val isOffline by viewModel.isOffline.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var activeShopId by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    var shopName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var telegram by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }
    var logoUrl by remember { mutableStateOf("") }
    var coverUrl by remember { mutableStateOf("") }
    var vacationMode by remember { mutableStateOf(false) }
    var vacationReturnDate by remember { mutableStateOf("") }
    var vacationNote by remember { mutableStateOf("") }

    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) logoUrl = uri.toString()
    }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) coverUrl = uri.toString()
    }

    LaunchedEffect(Unit) {
        isLoading = true
        val sid = resolveActiveSellerShopId(viewModel)
        activeShopId = sid
        if (sid.isNotBlank()) {
            val res = viewModel.repository.getShopDetails(sid)
            res.data?.let { s ->
                shopName = s.name
                category = s.category
                description = s.description
                phone = s.phone
                telegram = s.telegramLink
                whatsapp = s.whatsappLink
                logoUrl = s.logoUrl
                coverUrl = s.coverUrl
                vacationMode = s.isVacationMode
                vacationReturnDate = s.vacationReturnDate
                vacationNote = s.vacationNote
            }
        }
        isLoading = false
    }

    SuqiScreen(
        title = strings.seller.shopBrandingTitle,
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
                        text = strings.seller.saveChangesBtn,
                        onClick = {
                            if (shopName.isBlank()) {
                                viewModel.showToast(strings.seller.enterNameError)
                                return@SuqiButton
                            }
                            coroutineScope.launch {
                                val res = viewModel.repository.updateShopSettings(
                                    mapOf(
                                        "shopId" to activeShopId,
                                        "name" to shopName.trim(),
                                        "category" to category.trim(),
                                        "description" to description.trim(),
                                        "phone" to phone.trim(),
                                        "telegramLink" to telegram.trim(),
                                        "whatsappLink" to whatsapp.trim(),
                                        "logoUrl" to logoUrl,
                                        "coverUrl" to coverUrl,
                                        "isVacationMode" to vacationMode,
                                        "vacationReturnDate" to vacationReturnDate.trim(),
                                        "vacationNote" to vacationNote.trim()
                                    )
                                )
                                if (res.success) {
                                    viewModel.showToast(strings.buyer.profileUpdatedToast)
                                    viewModel.goBack()
                                } else {
                                    viewModel.showToast(res.message ?: strings.common.failedToAddToast)
                                }
                            }
                        },
                        testTag = "save_shop_settings_button",
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Branding Cover & Logo Pickers
                item {
                    SuqiSectionCard(padding = 14.dp) {
                        Text(strings.sellerOps.shopLogoCoverTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.imageBackdrop)
                                .clickable {
                                    coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }
                                .testTag("pick_shop_cover"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (coverUrl.isNotBlank()) {
                                AsyncImage(
                                    model = coverUrl,
                                    contentDescription = strings.seller.coverImageTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(LucideIcons.Camera, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(strings.common.changeLabel, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(colors.primaryTint)
                                    .clickable {
                                        logoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    }
                                    .testTag("pick_shop_logo"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (logoUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = logoUrl,
                                        contentDescription = strings.sellerOps.shopLogoCoverTitle,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(LucideIcons.Store, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(28.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings.sellerOps.shopLogoCoverTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.primaryText)
                                Text(strings.common.tapToPickPhoto, fontSize = 12.sp, color = colors.secondaryText)
                            }
                            TextButton(
                                onClick = {
                                    logoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text(strings.common.uploadPhotoLabel, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }

                item {
                    SuqiSectionCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SuqiTextField(value = shopName, onValueChange = { shopName = it }, label = strings.checkout.shopNameRequiredLabel)
                            SuqiTextField(value = category, onValueChange = { category = it }, label = strings.buyer.categoryLabel)
                            SuqiTextField(value = description, onValueChange = { description = it }, label = strings.checkout.shopDescriptionLabel, singleLine = false)
                            SuqiTextField(value = phone, onValueChange = { phone = it }, label = strings.buyer.phoneNumberLabel)
                            SuqiTextField(value = telegram, onValueChange = { telegram = it }, label = strings.checkout.telegramLabel)
                            SuqiTextField(value = whatsapp, onValueChange = { whatsapp = it }, label = "WhatsApp")
                        }
                    }
                }

                item {
                    SuqiSectionCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = strings.sellerOps.vacationModeTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                                Text(text = strings.sellerOps.vacationModeSub, fontSize = 12.sp, color = colors.secondaryText)
                            }
                            Switch(
                                checked = vacationMode,
                                onCheckedChange = { vacationMode = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = SuqiPrimary),
                                modifier = Modifier.testTag("toggle_vacation_mode")
                            )
                        }
                        if (vacationMode) {
                            Spacer(modifier = Modifier.height(12.dp))
                            SuqiTextField(value = vacationReturnDate, onValueChange = { vacationReturnDate = it }, label = strings.common.selectDate)
                            Spacer(modifier = Modifier.height(10.dp))
                            SuqiTextField(value = vacationNote, onValueChange = { vacationNote = it }, label = strings.checkout.vacationNoticeTitle, singleLine = false)
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

    var orders by remember { mutableStateOf<List<OrderDto>>(emptyList()) }
    var selectedTab by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    fun loadOrders() {
        coroutineScope.launch {
            isLoading = true
            val res = viewModel.repository.getSellerOrders()
            if (res.success && res.data != null) {
                orders = res.data
            }
            isLoading = false
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
    var zoomReceiptUri by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = showRejectSheet) {
        showRejectSheet = false
    }

    fun loadOrder() {
        coroutineScope.launch {
            isLoading = true
            val res = viewModel.repository.getOrderDetails(orderId)
            if (res.success) order = res.data
            isLoading = false
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
                        Text(strings.checkout.paymentProofSummaryTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
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
                        if (current.receiptImageUri.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.imageBackdrop)
                                    .clickable { zoomReceiptUri = current.receiptImageUri }
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

    zoomReceiptUri?.let { uri ->
        ReceiptZoomViewerDialog(imageUri = uri, onDismiss = { zoomReceiptUri = null })
    }
}

@Composable
fun SellerInboxScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val isOffline by viewModel.isOffline.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var activeShopId by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var conversations by remember { mutableStateOf<List<ChatMessageDto>>(emptyList()) }

    fun loadInbox() {
        coroutineScope.launch {
            isLoading = true
            val sid = resolveActiveSellerShopId(viewModel)
            activeShopId = sid
            val res = viewModel.repository.getChatConversations()
            if (res.success && res.data != null) {
                conversations = if (sid.isBlank()) res.data
                else res.data.filter { it.shopId == sid }.ifEmpty { res.data }
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadInbox() }

    SuqiScreen(
        title = strings.seller.navInbox,
        subtitle = strings.sellerOps.sellerInboxSub,
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        onRetryOffline = {
            viewModel.setOfflineMode(false)
            loadInbox()
        },
        modifier = modifier
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.padding(paddingValues).padding(top = 12.dp)) {
                SuqiSkeletonList(count = 4)
            }
        } else if (conversations.isEmpty()) {
            Box(modifier = Modifier.padding(paddingValues).padding(bottom = SuqiBottomNavContentPadding)) {
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
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = SuqiBottomNavContentPadding + 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(conversations, key = { it.conversationId }) { conv ->
                    SuqiSectionCard(
                        onClick = { viewModel.navigate(ScreenDestination.ChatThread(conv.shopId)) },
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
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (conv.shopId == activeShopId) strings.sellerOps.customerThreadLabel else conv.shopName,
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = colors.primaryText
                                    )
                                    Text(conv.timestamp, fontSize = 11.sp, color = colors.mutedText)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = conv.message,
                                    fontSize = 13.sp,
                                    color = colors.secondaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(LucideIcons.ChevronRight, contentDescription = null, tint = colors.mutedText, modifier = Modifier.size(18.dp))
                        }
                    }
                }
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

    SuqiScreen(
        title = strings.sellerOps.sellerToolsShopTitle,
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
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
