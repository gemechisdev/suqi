package com.example.ui.screens

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
import com.example.ui.theme.*
import kotlinx.coroutines.launch

private suspend fun resolveActiveSellerShopId(viewModel: SuqiViewModel): String {
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

    val shopName = (dashboardData["shopName"] as? String).orEmpty().ifBlank { "My Shop" }
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
        else -> listOf("D-6", "D-5", "D-4", "D-3", "D-2", "D-1", "Today")
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

    val statusLabels = listOf("Pending", "Confirmed", "Dispatched", "Delivered")
    val statusValues = listOf(
        sellerOrders.count { it.paymentStatus == "PENDING_VERIFICATION" }.toDouble(),
        sellerOrders.count { it.orderStatus in listOf("Confirmed", "Processing") }.toDouble(),
        sellerOrders.count { it.orderStatus in listOf("Dispatched", "Out for Delivery") }.toDouble(),
        sellerOrders.count { it.orderStatus == "Delivered" }.toDouble()
    )

    SuqiScreen(
        title = "Seller Studio",
        subtitle = shopName,
        onBackClick = { viewModel.goBack() },
        headerActions = {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .suqiWarmShadow(CircleShape, elevation = 6.dp)
                    .clip(CircleShape)
                    .background(colors.cardBackground)
                    .clickable { viewModel.navigate(ScreenDestination.ShopSettings) }
                    .testTag("seller_settings_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = LucideIcons.Settings,
                    contentDescription = "Shop Settings",
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
                                        text = "$pendingCount payments awaiting verification",
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = colors.primaryText
                                    )
                                }
                                Text(
                                    text = "Review →",
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
                                label = "Verified Sales",
                                value = formatEtb(totalSales),
                                icon = LucideIcons.TrendingUp,
                                tint = SuqiSuccess,
                                trendText = "$verifiedCount paid",
                                trendPositive = true,
                                onClick = { viewModel.switchSellerTab(ScreenDestination.SellerOrders) },
                                testTag = "seller_kpi_sales",
                                modifier = Modifier.weight(1f)
                            )
                            SuqiStatCard(
                                label = "Pending Verify",
                                value = pendingOrdersCount.toString(),
                                icon = LucideIcons.ClipboardCheck,
                                tint = if (pendingOrdersCount > 0) SuqiWarning else SuqiPrimary,
                                trendText = if (pendingOrdersCount > 0) "Action" else "Clear",
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
                                label = "Active Catalog",
                                value = "$activeProductCount / $totalProductCount",
                                icon = LucideIcons.Package,
                                tint = SuqiPrimary,
                                trendText = if (lowStockCount > 0) "$lowStockCount low stock" else "Healthy",
                                trendPositive = lowStockCount == 0,
                                onClick = { viewModel.switchSellerTab(ScreenDestination.ProductManagement) },
                                testTag = "seller_kpi_catalog",
                                modifier = Modifier.weight(1f)
                            )
                            SuqiStatCard(
                                label = "Total Orders",
                                value = sellerOrders.size.toString(),
                                icon = LucideIcons.ShoppingBag,
                                tint = SuqiPrimary,
                                trendText = if (sellerOrders.isEmpty()) "None yet" else "View all",
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
                                text = "Store Readiness Checklist",
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = colors.primaryText
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            if (!hasPaymentMethod) {
                                SuqiListRow(
                                    icon = LucideIcons.Wallet,
                                    title = "Add Payment Account",
                                    subtitle = "Required before publishing or receiving orders",
                                    badge = "Required",
                                    badgeColor = SuqiError,
                                    onClick = { viewModel.navigate(ScreenDestination.PaymentMethodsSetup) },
                                    testTag = "checklist_payment"
                                )
                            }
                            if (!hasDeliveryOption) {
                                SuqiListRow(
                                    icon = LucideIcons.Truck,
                                    title = "Configure Delivery & Receipt Rules",
                                    subtitle = "Set courier fees, ETA, and pickup options",
                                    badge = "Setup",
                                    badgeColor = SuqiWarning,
                                    onClick = { viewModel.navigate(ScreenDestination.DeliverySetup) },
                                    testTag = "checklist_delivery"
                                )
                            }
                            if (!isVerified) {
                                SuqiListRow(
                                    icon = LucideIcons.BadgeCheck,
                                    title = "Verify Merchant Identity",
                                    subtitle = "Status: ${verificationStatus.replace("_", " ")}",
                                    badge = if (verificationStatus == "PENDING_REVIEW") "In Review" else "Verify",
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
                                    text = "Sales Overview",
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
                                text = "No verified sales recorded yet. Verified order totals will plot here automatically.",
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
                            text = "Order Pipeline",
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
                        text = "Quick Actions",
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
                                title = "Add Product",
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.navigate(ScreenDestination.AddEditProduct()) }
                            )
                            QuickActionTile(
                                icon = LucideIcons.Package,
                                title = "Products",
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.switchSellerTab(ScreenDestination.ProductManagement) }
                            )
                            QuickActionTile(
                                icon = LucideIcons.ClipboardCheck,
                                title = "Verify Pays",
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.navigate(ScreenDestination.VerificationQueue) }
                            )
                            QuickActionTile(
                                icon = LucideIcons.Wallet,
                                title = "Payments",
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
                                title = "Delivery",
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.navigate(ScreenDestination.DeliverySetup) }
                            )
                            QuickActionTile(
                                icon = LucideIcons.BadgePercent,
                                title = "Promotions",
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.navigate(ScreenDestination.Promotions) }
                            )
                            QuickActionTile(
                                icon = LucideIcons.BadgeCheck,
                                title = "Verification",
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.navigate(ScreenDestination.SellerVerification) }
                            )
                            QuickActionTile(
                                icon = LucideIcons.Store,
                                title = "Shop Info",
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
                maxLines = 1,
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
    val isOffline by viewModel.isOffline.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf("cat_electronics") }
    var categories by remember {
        mutableStateOf(
            com.example.data.model.MarketplaceCategories.all.map { it.id to it.name }
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

    LaunchedEffect(productId) {
        val catRes = viewModel.repository.getCategories()
        if (catRes.success && !catRes.data.isNullOrEmpty()) {
            categories = catRes.data.filter { it.isActive }.map { it.id to it.name }
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
        title = if (productId == null) "Add Product" else "Edit Product",
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
                        text = if (productId == null) "Save Product" else "Save Changes",
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
                                    media.none { !it.isVideo } -> { viewModel.showToast("Add at least one photo or GIF for the cover"); return@launch }
                                    name.isBlank() -> { viewModel.showToast("Enter a product name"); return@launch }
                                    p <= 0.0 -> { viewModel.showToast("Enter a valid price"); return@launch }
                                    d !in 0..90 -> { viewModel.showToast("Discount must be between 0 and 90%"); return@launch }
                                    description.isBlank() -> { viewModel.showToast("Enter a description"); return@launch }
                                }
                                form.missingRequired()?.let {
                                    viewModel.showToast("${it.label} is required")
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
                                    viewModel.showToast(if (productId == null) "Product saved successfully!" else "Product updated!")
                                    viewModel.goBack()
                                } else {
                                    viewModel.showToast(res.message ?: "Could not save the product")
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
                    text = "Category *",
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
                        SuqiTextField(value = name, onValueChange = { name = it }, label = "Product Name (English) *")

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SuqiTextField(
                                value = priceStr,
                                onValueChange = { priceStr = it },
                                label = "Price (ETB) *",
                                modifier = Modifier.weight(1f)
                            )
                            SuqiTextField(
                                value = discountStr,
                                onValueChange = { discountStr = it },
                                label = "Discount (%)",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Column {
                            Text(
                                text = "Tax & VAT Status",
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
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSel) colors.primaryTint else colors.secondaryBackground)
                                            .border(1.dp, if (isSel) SuqiPrimary else colors.border, RoundedCornerShape(12.dp))
                                            .clickable { selectedTax = opt }
                                            .padding(horizontal = 12.dp, vertical = 7.dp)
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
                                label = "Stock Quantity *",
                                modifier = Modifier.weight(1f)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Stock Hold",
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
                                        text = if (stockHold) "Hold on order" else "Off",
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
                            label = "Description (English) *",
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
                            .clickable { showMultilingual = !showMultilingual },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(LucideIcons.Sparkles, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Multilingual Details (አማርኛ / Afaan Oromoo)",
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = colors.primaryText
                            )
                        }
                        Icon(
                            imageVector = LucideIcons.ChevronDown,
                            contentDescription = "Expand multilingual fields",
                            tint = colors.secondaryText,
                            modifier = Modifier.graphicsLayer { rotationZ = if (showMultilingual) 180f else 0f }
                        )
                    }

                    if (showMultilingual) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Amharic (አማርኛ)", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SuqiPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        SuqiTextField(value = nameAmharic, onValueChange = { nameAmharic = it }, label = "የእቃው ስም (Name in Amharic)")
                        Spacer(modifier = Modifier.height(8.dp))
                        SuqiTextField(value = descAmharic, onValueChange = { descAmharic = it }, label = "ዝርዝር መግለጫ (Description in Amharic)", singleLine = false)

                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Afaan Oromoo", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SuqiPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        SuqiTextField(value = nameOromo, onValueChange = { nameOromo = it }, label = "Maqaa Oomishaa (Name in Afaan Oromo)")
                        Spacer(modifier = Modifier.height(8.dp))
                        SuqiTextField(value = descOromo, onValueChange = { descOromo = it }, label = "Ibsa Oomishaa (Description in Afaan Oromo)", singleLine = false)
                    }
                }
            }
        }
    }

    if (showMissingPaymentDialog) {
        AlertDialog(
            onDismissRequest = { showMissingPaymentDialog = false },
            icon = { Icon(LucideIcons.ShieldCheck, contentDescription = null, tint = SuqiPrimary) },
            title = { Text("Payment Method Required", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, color = colors.primaryText) },
            text = {
                Text(
                    "You must configure at least one active payment method (Telebirr, CBE Birr, or Bank Transfer) before publishing products to buyers.",
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
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Setup Payment Methods", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMissingPaymentDialog = false }) {
                    Text("Cancel", fontFamily = SuqiFontFamily)
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
    val isOffline by viewModel.isOffline.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var products by remember { mutableStateOf<List<ProductDto>>(emptyList()) }
    var selectedTab by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var actionProduct by remember { mutableStateOf<ProductDto?>(null) }
    var confirmDeleteProduct by remember { mutableStateOf<ProductDto?>(null) }

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

    val tabs = remember(products) {
        listOf(
            "ALL" to "All (${products.size})",
            "ACTIVE" to "Active (${products.count { it.status == "ACTIVE" }})",
            "PAUSED" to "Paused (${products.count { it.status == "PAUSED" }})",
            "ARCHIVED" to "Archived (${products.count { it.status == "ARCHIVED" }})",
            "LOW_STOCK" to "Low Stock (${products.count { it.stockQuantity in 0..5 }})"
        )
    }

    val filtered = remember(products, selectedTab, searchQuery) {
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
                prod.subCategory.contains(searchQuery, ignoreCase = true)
            matchesStatus && matchesQuery
        }
    }

    SuqiScreen(
        title = "Products",
        subtitle = "${products.size} items in catalog",
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
                Icon(LucideIcons.Plus, contentDescription = "Add Product")
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
                placeholder = "Search catalog...",
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
                    title = if (products.isEmpty()) "No products listed yet" else "No matching products",
                    subtitle = if (products.isEmpty()) "Add your first product to start receiving orders from Ethiopian shoppers." else "Try another status filter or search keyword.",
                    ctaText = if (products.isEmpty()) "Add First Product" else "Show All Products",
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
                                        contentDescription = prod.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = prod.name,
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
                                        SuqiStatusBadge(
                                            statusText = prod.status,
                                            type = when (prod.status) {
                                                "ACTIVE" -> StatusType.SUCCESS
                                                "PAUSED" -> StatusType.WARNING
                                                else -> StatusType.NEUTRAL
                                            }
                                        )
                                        Text(
                                            text = "Stock: ${prod.stockQuantity}",
                                            fontFamily = SuqiFontFamily,
                                            fontSize = 12.sp,
                                            color = if (prod.stockQuantity <= 5) SuqiWarning else colors.secondaryText
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { actionProduct = prod },
                                    modifier = Modifier.testTag("product_actions_${prod.id}")
                                ) {
                                    Icon(
                                        imageVector = LucideIcons.MoreVertical,
                                        contentDescription = "Manage ${prod.name}",
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
        SuqiBottomSheet(
            onDismissRequest = { actionProduct = null },
            title = target.name
        ) {
            SuqiListRow(
                icon = LucideIcons.Pencil,
                title = "Edit Product",
                subtitle = "Update photos, pricing, stock, and options",
                onClick = {
                    actionProduct = null
                    viewModel.navigate(ScreenDestination.AddEditProduct(target.id))
                },
                testTag = "product_sheet_edit"
            )
            SuqiListRow(
                icon = if (target.status == "ACTIVE") LucideIcons.Pause else LucideIcons.CheckCircle,
                title = if (target.status == "ACTIVE") "Pause Listing" else "Activate Listing",
                subtitle = if (target.status == "ACTIVE") "Hide temporarily from storefront without deleting" else "Make visible to buyers again",
                onClick = {
                    val nextStatus = if (target.status == "ACTIVE") "PAUSED" else "ACTIVE"
                    actionProduct = null
                    coroutineScope.launch {
                        val res = viewModel.repository.updateProductStatus(target.id, nextStatus)
                        viewModel.showToast(if (res.success) "${target.name} marked $nextStatus" else "Failed to update status")
                        loadProducts()
                    }
                },
                testTag = "product_sheet_toggle"
            )
            if (target.status != "ARCHIVED") {
                SuqiListRow(
                    icon = LucideIcons.Package,
                    title = "Archive Listing",
                    subtitle = "Move out of active store inventory",
                    onClick = {
                        actionProduct = null
                        coroutineScope.launch {
                            viewModel.repository.updateProductStatus(target.id, "ARCHIVED")
                            viewModel.showToast("${target.name} archived")
                            loadProducts()
                        }
                    },
                    testTag = "product_sheet_archive"
                )
            }
            SuqiListRow(
                icon = LucideIcons.Eye,
                title = "Preview as Buyer",
                subtitle = "Open the customer product detail page",
                onClick = {
                    actionProduct = null
                    viewModel.navigate(ScreenDestination.ProductDetails(target.id))
                },
                testTag = "product_sheet_preview"
            )
            SuqiListRow(
                icon = LucideIcons.Trash2,
                title = "Delete Product",
                subtitle = "Permanently remove this listing",
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
        AlertDialog(
            onDismissRequest = { confirmDeleteProduct = null },
            title = { Text("Delete Product?", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, color = colors.primaryText) },
            text = {
                Text(
                    "Remove \"${target.name}\" permanently from your catalog? This cannot be undone.",
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
                            viewModel.showToast(if (res.success) "Product deleted" else (res.message ?: "Delete failed"))
                            loadProducts()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuqiError)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteProduct = null }) {
                    Text("Cancel")
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
    val isOffline by viewModel.isOffline.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var queue by remember { mutableStateOf<List<OrderDto>>(emptyList()) }
    var selectedOrderForReject by remember { mutableStateOf<OrderDto?>(null) }
    var rejectReason by remember { mutableStateOf("Reference not found in bank statement") }
    var zoomReceiptUri by remember { mutableStateOf<String?>(null) }

    val quickRejectReasons = remember {
        listOf(
            "Reference not found in bank statement",
            "Receipt screenshot blurry or incomplete",
            "Amount does not match order total",
            "Duplicate transaction reference"
        )
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
        title = "Verification Queue",
        subtitle = "${queue.size} awaiting payment review",
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
                                text = "${queue.size} pending verifications",
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
                            title = "All Caught Up",
                            subtitle = "No customer payment proofs are awaiting verification right now.",
                            ctaText = "View All Shop Orders",
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
                                        text = "Order #${order.orderReference}",
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
                                        text = "Submitted ${order.createdAt.take(16)}",
                                        fontFamily = SuqiFontFamily,
                                        fontSize = 11.sp,
                                        color = colors.mutedText
                                    )
                                }
                                SuqiStatusBadge("Pending", StatusType.WARNING)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Payment Method", fontSize = 13.sp, color = colors.secondaryText)
                                Text(order.paymentMethodTitle, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.primaryText)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Submitted Reference", fontSize = 13.sp, color = colors.secondaryText)
                                Text(order.transactionReference, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SuqiPrimary)
                            }
                            if (order.payerIdentifier.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Payer Account", fontSize = 13.sp, color = colors.secondaryText)
                                    Text(order.payerIdentifier, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = colors.primaryText)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Amount to Verify", fontSize = 13.sp, color = colors.secondaryText)
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
                                        contentDescription = "Payment receipt screenshot",
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
                                            Text("Tap to inspect receipt", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.secondaryBackground)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(LucideIcons.ShieldCheck, contentDescription = null, tint = colors.secondaryText, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Audit trail logs your staff/merchant identity & timestamp.",
                                    fontFamily = SuqiFontFamily,
                                    fontSize = 11.sp,
                                    color = colors.secondaryText
                                )
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
                                            viewModel.showToast("Payment verified! Order #${order.orderReference} confirmed.")
                                            loadQueue()
                                        }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SuqiSuccess),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("approve_payment_${order.id}")
                                ) {
                                    Text("Approve", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, color = Color.White)
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
                                        .height(48.dp)
                                        .testTag("reject_payment_${order.id}")
                                ) {
                                    Text("Reject", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, color = SuqiError)
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
            title = "Reject Payment #${targetOrder.orderReference}"
        ) {
            Text(
                text = "Select a reason or type instructions so the buyer can resubmit a valid proof:",
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
                label = "Rejection Reason *",
                singleLine = false
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { selectedOrderForReject = null },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.repository.rejectPayment(
                                targetOrder.id,
                                mapOf("reason" to rejectReason.trim().ifBlank { quickRejectReasons.first() })
                            )
                            viewModel.showToast("Payment rejected. Buyer notified to resubmit.")
                            selectedOrderForReject = null
                            loadQueue()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuqiError),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("confirm_reject_payment")
                ) {
                    Text("Reject Payment", color = Color.White, fontWeight = FontWeight.Bold)
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
                contentDescription = "Zoomable payment receipt",
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
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                ) {
                    Icon(LucideIcons.RotateCcw, contentDescription = "Reset zoom", tint = Color.White)
                }
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
                text = "Pinch to zoom · Drag to pan",
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
    val isOffline by viewModel.isOffline.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var activeShopId by remember { mutableStateOf("") }
    var activeShopName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var methods by remember { mutableStateOf<List<ShopPaymentMethodDto>>(emptyList()) }
    var editingMethod by remember { mutableStateOf<ShopPaymentMethodDto?>(null) }
    var showFormSheet by remember { mutableStateOf(false) }

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
        title = "Payment Methods",
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
                        text = "Add Payment Method",
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
                                    "Buyers see enabled accounts directly at checkout and pay you using these details."
                                } else {
                                    "Store Activation Required: Add at least 1 active payment method (Telebirr, CBE, etc.) before publishing products."
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
                            title = "No Payment Accounts Yet",
                            subtitle = "Add Telebirr, CBE Birr, or a bank transfer account so buyers can pay your shop.",
                            ctaText = "Add Payment Account",
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
                                    Text(
                                        text = method.providerType.replace("_", " "),
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
                                text = "Account: ${method.accountNumber} (${method.accountName})",
                                fontFamily = SuqiFontFamily,
                                fontSize = 13.sp,
                                color = colors.secondaryText
                            )
                            if (method.instructions.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Instructions: ${method.instructions}",
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
                                    modifier = Modifier.weight(1f).height(44.dp).testTag("edit_pm_${method.id}")
                                ) {
                                    Icon(LucideIcons.Pencil, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Edit", fontSize = 13.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.repository.deleteShopPaymentMethod(method.id)
                                            viewModel.showToast("Removed ${method.bankName}")
                                            reloadMethods(activeShopId)
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, SuqiError.copy(alpha = 0.5f)),
                                    modifier = Modifier.weight(1f).height(44.dp).testTag("delete_pm_${method.id}")
                                ) {
                                    Icon(LucideIcons.Trash2, contentDescription = null, tint = SuqiError, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Remove", color = SuqiError, fontSize = 13.sp)
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
            title = if (current == null) "Add Payment Method" else "Edit Payment Method"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("TELEBIRR" to "Telebirr", "CBE_BIRR" to "CBE Birr", "BANK_TRANSFER" to "Bank").forEach { (type, label) ->
                        val sel = providerType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
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
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = if (sel) Color.White else colors.primaryText
                            )
                        }
                    }
                }
                SuqiTextField(value = bankName, onValueChange = { bankName = it }, label = "Provider / Bank Name *")
                SuqiTextField(value = accName, onValueChange = { accName = it }, label = "Account Holder Name *")
                SuqiTextField(value = accNum, onValueChange = { accNum = it }, label = "Account / Phone Number *")
                SuqiTextField(value = inst, onValueChange = { inst = it }, label = "Buyer Payment Instructions", singleLine = false)

                Spacer(modifier = Modifier.height(4.dp))

                SuqiButton(
                    text = if (current == null) "Save Payment Method" else "Update Payment Method",
                    onClick = {
                        if (bankName.isBlank() || accName.isBlank() || accNum.isBlank()) {
                            viewModel.showToast("Fill in provider, account name, and account number")
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
                                viewModel.showToast(if (current == null) "Payment method added" else "Payment method updated")
                                showFormSheet = false
                                reloadMethods(activeShopId)
                            } else {
                                viewModel.showToast(res.message ?: "Could not save payment method")
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
        title = "Delivery & Receipt Rules",
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
                        text = "Save Delivery & Receipt Rules",
                        onClick = {
                            coroutineScope.launch {
                                if (activeShopId.isBlank()) {
                                    viewModel.showToast("No shop found for account")
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
                                viewModel.showToast("Delivery options & receipt rules saved!")
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
                                    Text("Require Receipt Screenshot", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                                    Text("Buyers must attach a screenshot alongside the transaction ref", fontSize = 12.sp, color = colors.secondaryText)
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(LucideIcons.Truck, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Standard Courier Delivery", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                                    Text("City & nationwide delivery", fontSize = 12.sp, color = colors.secondaryText)
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
                                SuqiTextField(value = standardFee, onValueChange = { standardFee = it }, label = "Fee (ETB)", modifier = Modifier.weight(1f))
                                SuqiTextField(value = standardEta, onValueChange = { standardEta = it }, label = "ETA Window", modifier = Modifier.weight(1.4f))
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(LucideIcons.Zap, contentDescription = null, tint = SuqiWarning, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Express Same-Day Delivery", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                                    Text("Fast courier dispatch", fontSize = 12.sp, color = colors.secondaryText)
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
                                SuqiTextField(value = expressFee, onValueChange = { expressFee = it }, label = "Fee (ETB)", modifier = Modifier.weight(1f))
                                SuqiTextField(value = expressCutoff, onValueChange = { expressCutoff = it }, label = "Cut-off / Window", modifier = Modifier.weight(1.4f))
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(LucideIcons.Store, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("In-Store Customer Pickup", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                                    Text("Free pickup at your shop address", fontSize = 12.sp, color = colors.secondaryText)
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
                            SuqiTextField(value = pickupAddress, onValueChange = { pickupAddress = it }, label = "Pickup Landmark / Hours", singleLine = false)
                        }
                    }
                }

                item {
                    SuqiSectionCard {
                        Text("Coverage & Free Delivery Threshold", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                        Spacer(modifier = Modifier.height(10.dp))
                        SuqiTextField(value = freeThreshold, onValueChange = { freeThreshold = it }, label = "Free Delivery Minimum (ETB, 0 = off)")
                        Spacer(modifier = Modifier.height(10.dp))
                        SuqiTextField(value = coverageAreas, onValueChange = { coverageAreas = it }, label = "Coverage Sub-Cities & Towns", singleLine = false)
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
        title = "Promotions & Discounts",
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
                        text = "Shop Promo Codes (${promoList.size})",
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
                            title = "No Promo Codes Yet",
                            subtitle = "Create a percentage or fixed-Birr coupon below for your buyers at checkout.",
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
                                            statusText = if (promo.isActive) "Active" else "Paused",
                                            type = if (promo.isActive) StatusType.SUCCESS else StatusType.NEUTRAL
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val discText = if (promo.discountType == "PERCENTAGE") {
                                        "${promo.discountValue.toInt()}% OFF"
                                    } else {
                                        "ETB ${promo.discountValue.toInt()} OFF"
                                    }
                                    Text(
                                        text = "$discText • Min. ETB ${promo.minOrderAmount.toInt()}",
                                        fontSize = 13.sp,
                                        color = colors.secondaryText
                                    )
                                    Text(
                                        text = "Valid until ${promo.validUntil}",
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
                                            viewModel.showToast("Promo ${promo.code} deleted")
                                            reloadPromos(activeShopId)
                                        }
                                    },
                                    modifier = Modifier.testTag("delete_promo_${promo.id}")
                                ) {
                                    Icon(LucideIcons.Trash2, contentDescription = "Delete promo", tint = SuqiError)
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Create New Promo Code",
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
                                label = "Code Name (e.g. ENKUTATASH15) *"
                            )

                            Column {
                                Text("Discount Type", fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.secondaryText)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    val isPct = discountType == "PERCENTAGE"
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isPct) SuqiPrimary else colors.secondaryBackground)
                                            .clickable { discountType = "PERCENTAGE" }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Percentage (%)", color = if (isPct) Color.White else colors.primaryText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    }
                                    val isFix = discountType == "FIXED"
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isFix) SuqiPrimary else colors.secondaryBackground)
                                            .clickable { discountType = "FIXED" }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Fixed Birr (ETB)", color = if (isFix) Color.White else colors.primaryText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                SuqiTextField(
                                    value = discountValue,
                                    onValueChange = { discountValue = it },
                                    label = if (discountType == "PERCENTAGE") "Discount (%) *" else "Discount (ETB) *",
                                    modifier = Modifier.weight(1f)
                                )
                                SuqiTextField(
                                    value = minOrder,
                                    onValueChange = { minOrder = it },
                                    label = "Min Order (ETB)",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            SuqiTextField(value = endDate, onValueChange = { endDate = it }, label = "Expiry Date")

                            SuqiButton(
                                text = "Create Promo Code",
                                onClick = {
                                    val codeClean = promoCode.trim().uppercase()
                                    val valNum = discountValue.toDoubleOrNull() ?: 0.0
                                    if (codeClean.isBlank() || valNum <= 0.0) {
                                        viewModel.showToast("Enter a valid promo code and discount value")
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
                                            viewModel.showToast("Promo $codeClean created!")
                                            promoCode = ""
                                            reloadPromos(activeShopId)
                                        } else {
                                            viewModel.showToast(res.message ?: "Could not create promo")
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
        title = "Shop Branding & Settings",
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
                        text = "Save Changes",
                        onClick = {
                            if (shopName.isBlank()) {
                                viewModel.showToast("Shop name cannot be empty")
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
                                    viewModel.showToast("Shop settings updated!")
                                    viewModel.goBack()
                                } else {
                                    viewModel.showToast(res.message ?: "Could not save shop settings")
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
                        Text("Storefront Branding", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
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
                                    contentDescription = "Shop cover banner",
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
                                    Text("Change Cover", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium)
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
                                        contentDescription = "Shop logo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(LucideIcons.Store, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(28.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Shop Logo", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.primaryText)
                                Text("Tap the avatar to choose a logo photo", fontSize = 12.sp, color = colors.secondaryText)
                            }
                            TextButton(
                                onClick = {
                                    logoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }
                            ) {
                                Text("Upload")
                            }
                        }
                    }
                }

                item {
                    SuqiSectionCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SuqiTextField(value = shopName, onValueChange = { shopName = it }, label = "Shop Name *")
                            SuqiTextField(value = category, onValueChange = { category = it }, label = "Shop Category")
                            SuqiTextField(value = description, onValueChange = { description = it }, label = "Shop Description", singleLine = false)
                            SuqiTextField(value = phone, onValueChange = { phone = it }, label = "Phone (+251)")
                            SuqiTextField(value = telegram, onValueChange = { telegram = it }, label = "Telegram Link")
                            SuqiTextField(value = whatsapp, onValueChange = { whatsapp = it }, label = "WhatsApp Link")
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
                                Text(text = "Vacation Mode", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                                Text(text = "Display a notice & pause new orders while away", fontSize = 12.sp, color = colors.secondaryText)
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
                            SuqiTextField(value = vacationReturnDate, onValueChange = { vacationReturnDate = it }, label = "Expected Return Date")
                            Spacer(modifier = Modifier.height(10.dp))
                            SuqiTextField(value = vacationNote, onValueChange = { vacationNote = it }, label = "Buyer Banner Message", singleLine = false)
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
    val colors = LocalSuqiColors.current
    val isOffline by viewModel.isOffline.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var shop by remember { mutableStateOf<ShopDto?>(null) }
    var licenseNumber by remember { mutableStateOf("") }
    var tinNumber by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    fun loadShop() {
        coroutineScope.launch {
            isLoading = true
            val sid = resolveActiveSellerShopId(viewModel)
            if (sid.isNotBlank()) {
                val res = viewModel.repository.getShopDetails(sid)
                if (res.success) shop = res.data
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadShop() }

    SuqiScreen(
        title = "Seller Verification",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.padding(paddingValues).padding(top = 12.dp)) {
                SuqiSkeletonList(count = 2)
            }
        } else {
            val status = shop?.verificationStatus ?: "NOT_SUBMITTED"
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(
                            when (status) {
                                "VERIFIED" -> colors.successBg
                                "PENDING_REVIEW" -> colors.warningBg
                                "REJECTED" -> colors.errorBg
                                else -> colors.primaryTint
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = LucideIcons.BadgeCheck,
                        contentDescription = null,
                        tint = when (status) {
                            "VERIFIED" -> SuqiSuccess
                            "PENDING_REVIEW" -> SuqiWarning
                            "REJECTED" -> SuqiError
                            else -> SuqiPrimary
                        },
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = when (status) {
                        "VERIFIED" -> "Shop Verified"
                        "PENDING_REVIEW" -> "Verification Under Review"
                        "REJECTED" -> "Resubmission Required"
                        else -> "Get Your Shop Verified"
                    },
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = colors.primaryText
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = when (status) {
                        "VERIFIED" -> "${shop?.name ?: "Your shop"} displays the Verified Merchant badge across all listings."
                        "PENDING_REVIEW" -> "Marketplace admins are reviewing your documents. We'll notify you once approved."
                        "REJECTED" -> shop?.verificationRejectionReason ?: "Please verify your trade license details and resubmit."
                        else -> "Submit your Ethiopian trade license or Kebele ID to earn the Verified Merchant badge."
                    },
                    fontFamily = SuqiFontFamily,
                    fontSize = 14.sp,
                    color = colors.secondaryText,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                SuqiSectionCard {
                    Text("Verification Checklist", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primaryText)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("✓ Business name & Addis/regional address", fontSize = 13.sp, color = colors.secondaryText)
                    Text("✓ Valid Ethiopian ID or Trade License", fontSize = 13.sp, color = colors.secondaryText)
                    Text("✓ Shop details and active payment method", fontSize = 13.sp, color = colors.secondaryText)

                    if (status != "VERIFIED") {
                        Spacer(modifier = Modifier.height(14.dp))
                        SuqiTextField(
                            value = licenseNumber,
                            onValueChange = { licenseNumber = it },
                            label = "Trade License / National ID Ref *"
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        SuqiTextField(
                            value = tinNumber,
                            onValueChange = { tinNumber = it },
                            label = "TIN Number (Optional)"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (status != "VERIFIED" && status != "PENDING_REVIEW") {
                    SuqiButton(
                        text = if (status == "REJECTED") "Resubmit for Verification" else "Submit for Verification",
                        onClick = {
                            if (licenseNumber.isBlank()) {
                                viewModel.showToast("Enter your Trade License or National ID reference")
                                return@SuqiButton
                            }
                            coroutineScope.launch {
                                val res = viewModel.repository.submitSellerVerification(
                                    mapOf("license" to licenseNumber.trim(), "tin" to tinNumber.trim())
                                )
                                if (res.success) {
                                    viewModel.showToast("Verification documents submitted for review!")
                                    loadShop()
                                } else {
                                    viewModel.showToast(res.message ?: "Submission failed")
                                }
                            }
                        },
                        testTag = "submit_seller_verification_btn",
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    SuqiButton(
                        text = "Back to Studio",
                        isPrimary = false,
                        onClick = { viewModel.goBack() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun SellerSubscriptionScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
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
        title = "Seller Subscription",
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
                                    Text("Free Pilot Launch Active", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SuqiSuccess)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "All merchant tools (unlimited products, sales analytics, featured placement) are 100% free during rollout!",
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
                                SuqiStatusBadge(if (adminSettings.isSubscriptionModeEnabled) "Active" else "Free Pilot", StatusType.SUCCESS)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (adminSettings.isSubscriptionModeEnabled) "${formatEtb(adminSettings.activePlanPrice)} / month" else "ETB 0 / Free Pilot",
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
                                        statusText = if (!adminSettings.isSubscriptionModeEnabled) "Free Pilot" else "Recommended",
                                        type = StatusType.SUCCESS
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (adminSettings.isSubscriptionModeEnabled) "${formatEtb(plan.monthlyPrice)} / month" else "ETB 0 during Pilot",
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

    val tabs = remember(orders) {
        listOf(
            "All" to "All (${orders.size})",
            "Awaiting Verification" to "Verify (${orders.count { it.paymentStatus == "PENDING_VERIFICATION" }})",
            "Confirmed" to "Confirmed (${orders.count { it.orderStatus in listOf("Confirmed", "Processing") }})",
            "Dispatched" to "Dispatched (${orders.count { it.orderStatus in listOf("Dispatched", "Out for Delivery") }})",
            "Delivered" to "Delivered (${orders.count { it.orderStatus == "Delivered" }})",
            "Cancelled" to "Cancelled (${orders.count { it.orderStatus == "Cancelled" || it.paymentStatus == "REJECTED" }})"
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
        title = "Shop Orders",
        subtitle = "${orders.size} total orders",
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
                placeholder = "Search reference, buyer name or phone...",
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
                SuqiEmptyState(
                    icon = LucideIcons.PackageOpen,
                    title = "No Orders Found",
                    subtitle = if (orders.isEmpty()) "Incoming customer orders for your shop will appear here." else "No orders match \"$selectedTab\".",
                    ctaText = if (selectedTab != "All") "Show All Orders" else null,
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
                                    viewModel.showToast("Payment verified for Order #${order.orderReference}!")
                                    loadOrders()
                                }
                            },
                            onDispatch = {
                                coroutineScope.launch {
                                    viewModel.repository.updateSellerOrderStatus(order.id, "Dispatched")
                                    viewModel.showToast("Order #${order.orderReference} marked as Dispatched!")
                                    loadOrders()
                                }
                            },
                            onDeliver = {
                                coroutineScope.launch {
                                    viewModel.repository.updateSellerOrderStatus(order.id, "Delivered")
                                    viewModel.showToast("Order #${order.orderReference} marked as Delivered!")
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
                    text = "Order #${order.orderReference}",
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
                statusText = if (order.paymentStatus == "PENDING_VERIFICATION") "Awaiting Payment" else order.orderStatus,
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
            Text(text = "${order.items.sumOf { it.quantity }} item(s) • ${order.paymentMethodTitle}", fontSize = 12.sp, color = colors.secondaryText)
        }

        if (order.transactionReference.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Ref: ${order.transactionReference}",
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
                    text = "Verified by ${order.verifiedBy ?: "Merchant"} · ${order.verifiedAt ?: ""}",
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
                    modifier = Modifier.weight(1f).height(42.dp)
                ) {
                    Text("Verify Payment", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            } else if (order.orderStatus in listOf("Confirmed", "Processing")) {
                Button(
                    onClick = onDispatch,
                    colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(42.dp)
                ) {
                    Text("Mark Dispatched", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            } else if (order.orderStatus in listOf("Dispatched", "Out for Delivery")) {
                Button(
                    onClick = onDeliver,
                    colors = ButtonDefaults.buttonColors(containerColor = SuqiSuccess),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(42.dp)
                ) {
                    Text("Mark Delivered", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            } else {
                OutlinedButton(
                    onClick = onViewDetails,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(42.dp)
                ) {
                    Text("Order Details", fontSize = 12.sp)
                }
            }

            OutlinedButton(
                onClick = onChat,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.height(42.dp)
            ) {
                Icon(LucideIcons.MessageCircle, contentDescription = null, tint = colors.primaryText, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Chat", fontSize = 12.sp, color = colors.primaryText)
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
    val isOffline by viewModel.isOffline.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var order by remember { mutableStateOf<OrderDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showRejectSheet by remember { mutableStateOf(false) }
    var rejectReason by remember { mutableStateOf("Reference not found in bank statement") }
    var zoomReceiptUri by remember { mutableStateOf<String?>(null) }

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
        title = order?.let { "Order #${it.orderReference}" } ?: "Seller Order Detail",
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
                title = "Order Not Found",
                subtitle = "This order record could not be loaded.",
                ctaText = "Go Back",
                onCtaClick = { viewModel.goBack() }
            )
        } else {
            val current = order!!
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
                                Text("Fulfilment Status", fontSize = 12.sp, color = colors.secondaryText)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(current.orderStatus, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = colors.primaryText)
                            }
                            SuqiStatusBadge(
                                statusText = current.paymentStatus.replace("_", " "),
                                type = when (current.paymentStatus) {
                                    "VERIFIED" -> StatusType.SUCCESS
                                    "REJECTED" -> StatusType.ERROR
                                    else -> StatusType.WARNING
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${current.deliveryOptionTitle} • ${current.deliveryEstimatedDays}",
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
                            Text("Buyer & Delivery", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                            TextButton(onClick = { viewModel.navigate(ScreenDestination.ChatThread(current.shopId)) }) {
                                Icon(LucideIcons.MessageCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Message Buyer")
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
                        Text("Payment Proof", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Method", fontSize = 13.sp, color = colors.secondaryText)
                            Text(current.paymentMethodTitle, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.primaryText)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Reference", fontSize = 13.sp, color = colors.secondaryText)
                            Text(current.transactionReference, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SuqiPrimary)
                        }
                        if (current.payerIdentifier.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Payer Phone/Account", fontSize = 13.sp, color = colors.secondaryText)
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
                                    contentDescription = "Receipt screenshot",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        if (!current.rejectionReason.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Rejection Note: ${current.rejectionReason}", fontSize = 12.sp, color = SuqiError)
                        }
                    }
                }

                // Line Items
                item {
                    SuqiSectionCard {
                        Text("Items (${current.items.size})", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
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
                                    Text("Qty ${item.quantity} × ${formatEtb(item.unitPrice)}", fontSize = 12.sp, color = colors.mutedText)
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
                            Text("Subtotal", fontSize = 13.sp, color = colors.secondaryText)
                            Text(formatEtb(current.subtotal), fontSize = 13.sp, color = colors.primaryText)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Delivery", fontSize = 13.sp, color = colors.secondaryText)
                            Text(formatEtb(current.deliveryFee), fontSize = 13.sp, color = colors.primaryText)
                        }
                        if (current.discountAmount > 0.0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Promo (${current.promoCodeUsed ?: ""})", fontSize = 13.sp, color = SuqiSuccess)
                                Text("-${formatEtb(current.discountAmount)}", fontSize = 13.sp, color = SuqiSuccess)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.primaryText)
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
                                        viewModel.showToast("Payment approved!")
                                        loadOrder()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SuqiSuccess),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f).height(48.dp).testTag("seller_detail_approve")
                            ) {
                                Text("Approve Payment", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { showRejectSheet = true },
                                border = BorderStroke(1.dp, SuqiError.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f).height(48.dp).testTag("seller_detail_reject")
                            ) {
                                Text("Reject", color = SuqiError, fontWeight = FontWeight.Bold)
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
                        SuqiButton(
                            text = "Advance to $nextStatus",
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.repository.updateSellerOrderStatus(current.id, nextStatus)
                                    viewModel.showToast("Order marked as $nextStatus")
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
            title = "Reject Payment"
        ) {
            SuqiTextField(
                value = rejectReason,
                onValueChange = { rejectReason = it },
                label = "Reason for Rejection *",
                singleLine = false
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = {
                    coroutineScope.launch {
                        viewModel.repository.rejectPayment(order!!.id, mapOf("reason" to rejectReason.trim()))
                        viewModel.showToast("Payment rejected")
                        showRejectSheet = false
                        loadOrder()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SuqiError),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Confirm Rejection", color = Color.White, fontWeight = FontWeight.Bold)
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
        title = "Seller Inbox",
        subtitle = "Customer inquiries & receipts",
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
                    title = "No Buyer Messages Yet",
                    subtitle = "When shoppers ask about products or delivery, their messages appear here.",
                    ctaText = "View Orders",
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
                                        text = if (conv.shopId == activeShopId) "Customer Thread" else conv.shopName,
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
        title = "Seller Tools & Shop",
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
                                    contentDescription = shop?.name ?: "Shop logo",
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
                                text = shop?.name ?: "My Suqi Shop",
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = colors.primaryText
                            )
                            Text(
                                text = "${shop?.category ?: "Marketplace"} · ★ ${shop?.rating ?: 5.0}",
                                fontSize = 12.sp,
                                color = colors.secondaryText
                            )
                        }
                        SuqiStatusBadge(
                            statusText = if (shop?.isVerified == true) "Verified" else "Standard",
                            type = if (shop?.isVerified == true) StatusType.SUCCESS else StatusType.NEUTRAL
                        )
                    }
                }
            }

            item {
                SuqiSectionCard(padding = 12.dp) {
                    SuqiListRow(
                        icon = LucideIcons.ClipboardCheck,
                        title = "Payment Verification Queue",
                        subtitle = "Approve or reject pending customer receipts",
                        onClick = { viewModel.navigate(ScreenDestination.VerificationQueue) },
                        testTag = "seller_more_verify_queue"
                    )
                    SuqiListRow(
                        icon = LucideIcons.Wallet,
                        title = "Payment Methods",
                        subtitle = "Telebirr, CBE Birr & bank transfer accounts",
                        onClick = { viewModel.navigate(ScreenDestination.PaymentMethodsSetup) },
                        testTag = "seller_more_payment_methods"
                    )
                    SuqiListRow(
                        icon = LucideIcons.Truck,
                        title = "Delivery & Receipt Rules",
                        subtitle = "Standard/express fees, coverage & screenshot requirement",
                        onClick = { viewModel.navigate(ScreenDestination.DeliverySetup) },
                        testTag = "seller_more_delivery"
                    )
                    SuqiListRow(
                        icon = LucideIcons.BadgePercent,
                        title = "Promotions & Promo Codes",
                        subtitle = "Create and toggle shop discounts",
                        onClick = { viewModel.navigate(ScreenDestination.Promotions) },
                        testTag = "seller_more_promotions"
                    )
                    SuqiListRow(
                        icon = LucideIcons.Store,
                        title = "Shop Branding & Vacation Mode",
                        subtitle = "Logo, cover banner, hours, and holiday notice",
                        onClick = { viewModel.navigate(ScreenDestination.ShopSettings) },
                        testTag = "seller_more_shop_settings"
                    )
                    SuqiListRow(
                        icon = LucideIcons.BadgeCheck,
                        title = "Merchant Verification",
                        subtitle = "Submit trade license or national ID",
                        onClick = { viewModel.navigate(ScreenDestination.SellerVerification) },
                        testTag = "seller_more_verification"
                    )
                    SuqiListRow(
                        icon = LucideIcons.Sparkles,
                        title = "Seller Subscription Plan",
                        subtitle = "View plan benefits & billing status",
                        onClick = { viewModel.navigate(ScreenDestination.SellerSubscription) },
                        testTag = "seller_more_subscription"
                    )
                }
            }

            item {
                SuqiSectionCard(padding = 12.dp) {
                    SuqiListRow(
                        icon = LucideIcons.ShoppingBag,
                        title = "Switch to Buyer Mode",
                        subtitle = "Browse the Suqi marketplace as a shopper",
                        onClick = {
                            viewModel.setActiveRole("CUSTOMER")
                            viewModel.popToRoot(ScreenDestination.Home)
                        },
                        testTag = "seller_switch_to_buyer"
                    )
                    SuqiListRow(
                        icon = LucideIcons.Settings,
                        title = "App Settings",
                        subtitle = "Language, region, dark mode & help",
                        onClick = { viewModel.navigate(ScreenDestination.AppSettings) },
                        testTag = "seller_more_app_settings"
                    )
                }
            }
        }
    }
}
