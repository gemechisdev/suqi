package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SellerStudioScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var dashboardData by remember { mutableStateOf<Map<String, Any>>(emptyMap()) }
    var pendingQueue by remember { mutableStateOf<List<OrderDto>>(emptyList()) }

    LaunchedEffect(Unit) {
        val res = viewModel.repository.getSellerDashboard()
        if (res.success && res.data != null) {
            dashboardData = res.data
        }
        val qRes = viewModel.repository.getSellerVerificationQueue()
        if (qRes.success && qRes.data != null) {
            pendingQueue = qRes.data
        }
    }

    val pendingCount = pendingQueue.size

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.goBack() },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SuqiSecondaryBackground)
                    ) {
                        Icon(LucideIcons.ArrowLeft, contentDescription = "Back", tint = SuqiPrimaryText)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Seller Studio",
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = SuqiPrimaryText
                    )
                }

                IconButton(
                    onClick = { viewModel.navigate(ScreenDestination.ShopSettings) },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SuqiSecondaryBackground)
                ) {
                    Icon(LucideIcons.Settings, contentDescription = "Shop Settings", tint = SuqiPrimaryText)
                }
            }
        },
        containerColor = SuqiBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Payments Awaiting Verification Alert Banner (Checklist 17 & Mockup 3)
            if (pendingCount > 0) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = SuqiWarningLight),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(SuqiWarning.copy(alpha = 0.5f))
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.navigate(ScreenDestination.VerificationQueue) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(LucideIcons.TriangleAlert, contentDescription = null, tint = SuqiWarning, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$pendingCount payments awaiting verification",
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = SuqiPrimaryText
                                )
                            }
                            Text(
                                text = "View →",
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = SuqiPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Key Metrics 3 Cards Row (Total Sales, Pending Orders, Low Stock)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatCard(
                        title = "Total Sales",
                        value = "ETB 248k",
                        sub = "↑ +12%",
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        title = "Pending Orders",
                        value = "${dashboardData["pendingOrders"] ?: 3}",
                        sub = "Action needed",
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        title = "Low Stock",
                        value = "${dashboardData["lowStock"] ?: 1}",
                        sub = "View items →",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigate(ScreenDestination.ProductManagement) }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Sales Overview Chart representation
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = SuqiSecondaryBackground),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                                    color = SuqiSecondaryText
                                )
                                Text(
                                    text = "ETB 248,750",
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = SuqiPrimaryText
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Last 7 Days ▾",
                                    fontFamily = SuqiFontFamily,
                                    fontSize = 11.sp,
                                    color = SuqiPrimaryText
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Wave chart canvas
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                        ) {
                            val w = size.width
                            val h = size.height
                            val path = Path().apply {
                                moveTo(0f, h * 0.7f)
                                cubicTo(w * 0.2f, h * 0.85f, w * 0.35f, h * 0.4f, w * 0.5f, h * 0.6f)
                                cubicTo(w * 0.65f, h * 0.8f, w * 0.8f, h * 0.2f, w, h * 0.3f)
                            }
                            drawPath(
                                path = path,
                                color = SuqiPrimary,
                                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                                Text(text = day, fontFamily = SuqiFontFamily, fontSize = 11.sp, color = SuqiMutedText)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Quick Actions 4 Grid Items
            item {
                Text(
                    text = "Quick Actions",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = SuqiPrimaryText
                )
                Spacer(modifier = Modifier.height(12.dp))

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
                        onClick = { viewModel.navigate(ScreenDestination.ProductManagement) }
                    )
                    QuickActionTile(
                        icon = LucideIcons.ClipboardCheck,
                        title = "Verify Orders",
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

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionTile(
                        icon = LucideIcons.Truck,
                        title = "Delivery Setup",
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
                        title = "Shop Settings",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigate(ScreenDestination.ShopSettings) }
                    )
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
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SuqiSecondaryBackground),
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, fontFamily = SuqiFontFamily, fontSize = 11.sp, color = SuqiSecondaryText)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SuqiPrimaryText)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = sub, fontFamily = SuqiFontFamily, fontSize = 10.sp, color = SuqiPrimary)
        }
    }
}

@Composable
fun QuickActionTile(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SuqiSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(SuqiBorder)
        ),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(SuqiPrimaryLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                color = SuqiPrimaryText,
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
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf("Wireless Headphones Pro") }
    var categoryId by remember { mutableStateOf("cat_electronics") }
    var priceStr by remember { mutableStateOf("4999") }
    var discountStr by remember { mutableStateOf("10") }
    var stockStr by remember { mutableStateOf("100") }
    var stockHold by remember { mutableStateOf(true) }
    var colorsStr by remember { mutableStateOf("Color: Black, White, Blue") }
    var description by remember { mutableStateOf("High quality premium product with standard warranty.") }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.goBack() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SuqiSecondaryBackground)
                ) {
                    Icon(LucideIcons.ArrowLeft, contentDescription = "Back", tint = SuqiPrimaryText)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (productId == null) "Add Product" else "Edit Product",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SuqiButton(
                        text = "Save Product",
                        onClick = {
                            coroutineScope.launch {
                                val p = (priceStr.toDoubleOrNull() ?: 4999.0)
                                val d = (discountStr.toIntOrNull() ?: 0)
                                val s = (stockStr.toIntOrNull() ?: 10)
                                val res = viewModel.repository.createProduct(
                                    mapOf(
                                        "name" to name,
                                        "price" to p,
                                        "discountPercent" to d,
                                        "stockQuantity" to s,
                                        "stockHoldEnabled" to stockHold,
                                        "categoryId" to categoryId,
                                        "description" to description
                                    )
                                )
                                if (res.success) {
                                    viewModel.showToast("Product saved successfully!")
                                    viewModel.goBack()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        containerColor = SuqiBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Photos & Media Upload Box (Checklist 18 & Mockup 3)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(SuqiSecondaryBackground)
                        .border(1.5.dp, SuqiBorder, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(LucideIcons.ImagePlus, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Add Photos & Videos",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = SuqiPrimaryText
                        )
                        Text(
                            text = "Upload images, GIFs or videos (Max 10 files)",
                            fontFamily = SuqiFontFamily,
                            fontSize = 12.sp,
                            color = SuqiMutedText
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SuqiTextField(value = name, onValueChange = { name = it }, label = "Product Name *")

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
                                fontSize = 14.sp
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
                                    text = if (stockHold) "Hold during payment" else "Off",
                                    fontFamily = SuqiFontFamily,
                                    fontSize = 12.sp,
                                    color = SuqiSecondaryText
                                )
                            }
                        }
                    }

                    SuqiTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = "Description *",
                        singleLine = false
                    )

                    SuqiTextField(
                        value = colorsStr,
                        onValueChange = { colorsStr = it },
                        label = "Product Variations"
                    )
                }
            }
        }
    }
}

@Composable
fun ProductManagementScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var products by remember { mutableStateOf<List<ProductDto>>(emptyList()) }
    var selectedTab by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    val tabs = listOf("All (24)", "Active (20)", "Paused (2)", "Archived (2)")

    fun loadProducts() {
        coroutineScope.launch {
            val res = viewModel.repository.getSellerProducts()
            if (res.success && res.data != null) {
                products = res.data
            }
        }
    }

    LaunchedEffect(Unit) {
        loadProducts()
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.goBack() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SuqiSecondaryBackground)
                ) {
                    Icon(LucideIcons.ArrowLeft, contentDescription = "Back", tint = SuqiPrimaryText)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Product Management",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.navigate(ScreenDestination.AddEditProduct()) },
                containerColor = SuqiPrimary,
                contentColor = Color.White
            ) {
                Icon(LucideIcons.Plus, contentDescription = "Add Product")
            }
        },
        containerColor = SuqiBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Search field
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search products...", color = SuqiMutedText) },
                    leadingIcon = { Icon(LucideIcons.Search, contentDescription = null, tint = SuqiSecondaryText) },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SuqiSecondaryBackground,
                        unfocusedContainerColor = SuqiSecondaryBackground,
                        focusedBorderColor = SuqiPrimary,
                        unfocusedBorderColor = SuqiBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Tabs Row (Mockup 4)
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tabs) { tab ->
                        val isSel = tab.startsWith(selectedTab)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (isSel) SuqiPrimary else SuqiSecondaryBackground)
                                .clickable { selectedTab = tab.split(" ").first() }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = tab,
                                fontFamily = SuqiFontFamily,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp,
                                color = if (isSel) Color.White else SuqiPrimaryText
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            val filtered = if (searchQuery.isBlank()) products else products.filter {
                it.name.contains(searchQuery, ignoreCase = true)
            }

            items(filtered) { prod ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SuqiSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SuqiBorder)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(SuqiSecondaryBackground)
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
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (prod.status == "ACTIVE") SuqiSuccess else SuqiWarning)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${prod.status} • In Stock: ${prod.stockQuantity}",
                                    fontFamily = SuqiFontFamily,
                                    fontSize = 12.sp,
                                    color = SuqiSecondaryText
                                )
                            }
                        }

                        IconButton(onClick = { viewModel.showToast("Product options") }) {
                            Icon(LucideIcons.MoreVertical, contentDescription = "Options", tint = SuqiSecondaryText)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VerificationQueueScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var queue by remember { mutableStateOf<List<OrderDto>>(emptyList()) }
    var selectedOrderForReject by remember { mutableStateOf<OrderDto?>(null) }
    var rejectReason by remember { mutableStateOf("") }

    fun loadQueue() {
        coroutineScope.launch {
            val res = viewModel.repository.getSellerVerificationQueue()
            if (res.success && res.data != null) {
                queue = res.data
            }
        }
    }

    LaunchedEffect(Unit) {
        loadQueue()
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.goBack() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SuqiSecondaryBackground)
                ) {
                    Icon(LucideIcons.ArrowLeft, contentDescription = "Back", tint = SuqiPrimaryText)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Verification Queue",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        },
        containerColor = SuqiBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Header notice
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SuqiWarningLight)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(LucideIcons.Clock, contentDescription = null, tint = SuqiWarning, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${queue.size} pending verifications",
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = SuqiPrimaryText
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (queue.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Text("No pending payments to verify. All caught up!", color = SuqiSecondaryText)
                    }
                }
            } else {
                items(queue) { order ->
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SuqiSurface),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SuqiBorder)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Order #${order.orderReference}",
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = order.customerName,
                                        fontFamily = SuqiFontFamily,
                                        fontSize = 13.sp,
                                        color = SuqiSecondaryText
                                    )
                                }
                                SuqiStatusBadge("Pending", StatusType.WARNING)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Payment Method", fontSize = 13.sp, color = SuqiSecondaryText)
                                Text(order.paymentMethodTitle, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Submitted Reference", fontSize = 13.sp, color = SuqiSecondaryText)
                                Text(order.transactionReference, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SuqiPrimary)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Amount to Verify", fontSize = 13.sp, color = SuqiSecondaryText)
                                Text(formatEtb(order.totalAmount), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Approve & Reject Buttons (Checklist 22 & Mockup 4)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.repository.verifyPayment(order.id, mapOf("notes" to "Confirmed by seller"))
                                            viewModel.showToast("Payment verified! Order #${order.orderReference} confirmed.")
                                            loadQueue()
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SuqiSuccess),
                                    modifier = Modifier.weight(1f).height(44.dp)
                                ) {
                                    Text("Approve", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                Button(
                                    onClick = {
                                        selectedOrderForReject = order
                                        rejectReason = "Reference not found in bank statement"
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SuqiSecondaryBackground),
                                    modifier = Modifier.weight(1f).height(44.dp)
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

    if (selectedOrderForReject != null) {
        AlertDialog(
            onDismissRequest = { selectedOrderForReject = null },
            title = { Text("Reject Payment", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Please provide the rejection reason for Order #${selectedOrderForReject!!.orderReference}:")
                    Spacer(modifier = Modifier.height(10.dp))
                    SuqiTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        label = "Reason *"
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.repository.rejectPayment(
                                selectedOrderForReject!!.id,
                                mapOf("reason" to rejectReason)
                            )
                            viewModel.showToast("Payment rejected. Customer notified to resubmit.")
                            selectedOrderForReject = null
                            loadQueue()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuqiError)
                ) {
                    Text("Reject Payment")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedOrderForReject = null }) { Text("Cancel") }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun PaymentMethodsSetupScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var methods by remember { mutableStateOf<List<ShopPaymentMethodDto>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val res = viewModel.repository.getShopPaymentMethods("shop_techhub")
        if (res.success && res.data != null) {
            methods = res.data
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.goBack() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SuqiSecondaryBackground)
                ) {
                    Icon(LucideIcons.ArrowLeft, contentDescription = "Back", tint = SuqiPrimaryText)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Payment Methods",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SuqiButton(
                        text = "Save Changes",
                        onClick = {
                            viewModel.showToast("Payment methods updated!")
                            viewModel.goBack()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        containerColor = SuqiBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SuqiPrimaryLight)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(LucideIcons.Info, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Add at least one payment method for your shop. Customers will use these to pay you directly.",
                        fontFamily = SuqiFontFamily,
                        fontSize = 13.sp,
                        color = SuqiPrimaryText
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(methods) { method ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SuqiSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SuqiBorder)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = method.bankName,
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            if (method.isRequired) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SuqiSuccessLight)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Required", fontSize = 11.sp, color = SuqiSuccess, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Account: ${method.accountNumber} (${method.accountName})", fontSize = 13.sp, color = SuqiSecondaryText)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Instructions: ${method.instructions}", fontSize = 12.sp, color = SuqiMutedText)

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.showToast("Edit method") },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Text("Edit", fontSize = 13.sp)
                            }
                            Button(
                                onClick = { viewModel.showToast("Removed method") },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SuqiSecondaryBackground),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Text("Remove", color = SuqiError, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, SuqiPrimary),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(LucideIcons.Plus, contentDescription = null, tint = SuqiPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+ Add Payment Method", color = SuqiPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showAddDialog) {
        var provider by remember { mutableStateOf("Telebirr") }
        var accName by remember { mutableStateOf("Tech Hub") }
        var accNum by remember { mutableStateOf("+251 912 345 678") }
        var inst by remember { mutableStateOf("Send via Telebirr") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Payment Method", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SuqiTextField(value = provider, onValueChange = { provider = it }, label = "Provider / Bank Name")
                    SuqiTextField(value = accName, onValueChange = { accName = it }, label = "Account Name")
                    SuqiTextField(value = accNum, onValueChange = { accNum = it }, label = "Account / Phone Number")
                    SuqiTextField(value = inst, onValueChange = { inst = it }, label = "Instructions")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.repository.saveShopPaymentMethod(
                                ShopPaymentMethodDto(
                                    id = "pm_${System.currentTimeMillis()}",
                                    shopId = "shop_techhub",
                                    providerType = "TELEBIRR",
                                    bankName = provider,
                                    accountName = accName,
                                    accountNumber = accNum,
                                    instructions = inst
                                )
                            )
                            val res = viewModel.repository.getShopPaymentMethods("shop_techhub")
                            if (res.success && res.data != null) methods = res.data
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary)
                ) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun DeliverySetupScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.goBack() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SuqiSecondaryBackground)
                ) {
                    Icon(LucideIcons.ArrowLeft, contentDescription = "Back", tint = SuqiPrimaryText)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Delivery & Fulfilment",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        },
        bottomBar = {
            Surface(color = SuqiSurface, shadowElevation = 12.dp, modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SuqiButton(
                        text = "Save Settings",
                        onClick = {
                            viewModel.showToast("Delivery settings saved!")
                            viewModel.goBack()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        containerColor = SuqiBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Text(text = "Delivery Options", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))
                SuqiSelectionCard(
                    selected = true,
                    title = "Standard Delivery",
                    subtitle = "1-3 days • ETB 50",
                    icon = LucideIcons.Truck,
                    onClick = {}
                )
                Spacer(modifier = Modifier.height(10.dp))
                SuqiSelectionCard(
                    selected = true,
                    title = "Express Delivery",
                    subtitle = "Same day • ETB 100",
                    icon = LucideIcons.Zap,
                    onClick = {}
                )
                Spacer(modifier = Modifier.height(10.dp))
                SuqiSelectionCard(
                    selected = true,
                    title = "Store Pickup",
                    subtitle = "Pick up at our store (Free)",
                    icon = LucideIcons.Store,
                    onClick = {}
                )
                Spacer(modifier = Modifier.height(24.dp))

                Text(text = "Coverage Areas", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Addis Ababa (All Sub-Cities), Adama, Hawassa", fontSize = 14.sp, color = SuqiSecondaryText)
            }
        }
    }
}

@Composable
fun PromotionsScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var promoCode by remember { mutableStateOf("AUTUMN15") }
    var discountType by remember { mutableStateOf("Percentage (%)") }
    var discountValue by remember { mutableStateOf("15") }
    var minOrder by remember { mutableStateOf("1000") }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.goBack() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SuqiSecondaryBackground)
                ) {
                    Icon(LucideIcons.ArrowLeft, contentDescription = "Back", tint = SuqiPrimaryText)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Promotions & Discounts",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        },
        containerColor = SuqiBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Active Promo Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SuqiPrimaryLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "SUMMER25", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                SuqiStatusBadge("Active", StatusType.SUCCESS)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "25% OFF • Min. ETB 1,000", fontSize = 13.sp, color = SuqiSecondaryText)
                            Text(text = "Valid: Jun 1 – Jun 30, 2026", fontSize = 12.sp, color = SuqiMutedText)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(text = "Create Promo Code", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(14.dp))
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SuqiTextField(value = promoCode, onValueChange = { promoCode = it }, label = "Code Name (e.g. SUMMER25)")
                    SuqiTextField(value = discountType, onValueChange = { discountType = it }, label = "Discount Type")
                    SuqiTextField(value = discountValue, onValueChange = { discountValue = it }, label = "Discount Value")
                    SuqiTextField(value = minOrder, onValueChange = { minOrder = it }, label = "Minimum Order Amount (Optional)")

                    Spacer(modifier = Modifier.height(12.dp))

                    SuqiButton(
                        text = "Create Promo Code",
                        onClick = {
                            coroutineScope.launch {
                                viewModel.repository.createPromoCode(
                                    PromoCodeDto(
                                        id = "promo_${System.currentTimeMillis()}",
                                        shopId = "shop_techhub",
                                        code = promoCode.trim().uppercase(),
                                        discountType = "PERCENTAGE",
                                        discountValue = discountValue.toDoubleOrNull() ?: 10.0,
                                        minOrderAmount = minOrder.toDoubleOrNull() ?: 0.0,
                                        validUntil = "Dec 31, 2026"
                                    )
                                )
                                viewModel.showToast("Promo Code created successfully!")
                                viewModel.goBack()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
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
    val coroutineScope = rememberCoroutineScope()
    var shopName by remember { mutableStateOf("Tech Hub") }
    var category by remember { mutableStateOf("Electronics") }
    var description by remember { mutableStateOf("Your trusted electronics store in Ethiopia with authentic products.") }
    var phone by remember { mutableStateOf("+251 912 345 678") }
    var telegram by remember { mutableStateOf("https://t.me/techhub") }
    var vacationMode by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.goBack() },
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(SuqiSecondaryBackground)
                ) {
                    Icon(LucideIcons.ArrowLeft, contentDescription = "Back", tint = SuqiPrimaryText)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "Shop Settings", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        },
        bottomBar = {
            Surface(color = SuqiSurface, shadowElevation = 12.dp, modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SuqiButton(
                        text = "Save Changes",
                        onClick = {
                            coroutineScope.launch {
                                viewModel.repository.updateShopSettings(
                                    mapOf(
                                        "name" to shopName,
                                        "category" to category,
                                        "description" to description,
                                        "phone" to phone,
                                        "telegramLink" to telegram,
                                        "isVacationMode" to vacationMode
                                    )
                                )
                                viewModel.showToast("Shop settings updated!")
                                viewModel.goBack()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        containerColor = SuqiBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SuqiTextField(value = shopName, onValueChange = { shopName = it }, label = "Shop Name")
                    SuqiTextField(value = category, onValueChange = { category = it }, label = "Shop Category")
                    SuqiTextField(value = description, onValueChange = { description = it }, label = "Shop Description", singleLine = false)
                    SuqiTextField(value = phone, onValueChange = { phone = it }, label = "Phone (+251)")
                    SuqiTextField(value = telegram, onValueChange = { telegram = it }, label = "Telegram Link")

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Vacation Mode", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text(text = "Temporarily pause receiving new orders", fontSize = 12.sp, color = SuqiSecondaryText)
                        }
                        Switch(
                            checked = vacationMode,
                            onCheckedChange = { vacationMode = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = SuqiPrimary)
                        )
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
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.goBack() },
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(SuqiSecondaryBackground)
                ) {
                    Icon(LucideIcons.ArrowLeft, contentDescription = "Back", tint = SuqiPrimaryText)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "Seller Verification", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        },
        containerColor = SuqiBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(SuqiPrimaryLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(LucideIcons.BadgeCheck, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Get Your Shop Verified",
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Complete manual review to earn the Verified badge and build high trust with shoppers.",
                fontFamily = SuqiFontFamily,
                fontSize = 14.sp,
                color = SuqiSecondaryText,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SuqiSecondaryBackground),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Verification Requirements:", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("✓ Business information & location", fontSize = 13.sp)
                    Text("✓ Valid Ethiopian ID or Trade License", fontSize = 13.sp)
                    Text("✓ Shop details and authentic photo", fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            SuqiButton(
                text = "Submit for Verification",
                onClick = {
                    coroutineScope.launch {
                        viewModel.repository.submitSellerVerification(mapOf("status" to "PENDING_REVIEW"))
                        viewModel.showToast("Verification documents submitted for admin review!")
                        viewModel.goBack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SellerSubscriptionScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.goBack() },
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(SuqiSecondaryBackground)
                ) {
                    Icon(LucideIcons.ArrowLeft, contentDescription = "Back", tint = SuqiPrimaryText)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "Seller Subscription", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        },
        containerColor = SuqiBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(20.dp)
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SuqiPrimaryLight),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SuqiPrimary)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pro Seller Plan", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = SuqiPrimary)
                        SuqiStatusBadge("Active", StatusType.SUCCESS)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Unlock more features and grow your business faster.", fontSize = 13.sp, color = SuqiSecondaryText)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("ETB 299 / month", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = SuqiPrimaryText)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("✓ Featured Shop Placement in search", fontSize = 14.sp)
                    Text("✓ Advanced Sales Analytics & Charts", fontSize = 14.sp)
                    Text("✓ Unlimited Promo Codes & Discounts", fontSize = 14.sp)
                    Text("✓ Priority Merchant Support", fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Current Billing", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Next Billing Date: Jul 28, 2026\nPayment Method: Telebirr", fontSize = 13.sp, color = SuqiSecondaryText)

            Spacer(modifier = Modifier.height(24.dp))

            SuqiButton(
                text = "Manage Subscription",
                onClick = { viewModel.showToast("Subscription managed") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
