package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.graphicsLayer
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
                        fontWeight = FontWeight.SemiBold,
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
                        colors = CardDefaults.cardColors(containerColor = SuqiWarningBg),
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
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 20.sp,
                                    color = SuqiPrimaryText
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(LocalSuqiColors.current.cardBackground)
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
                    fontWeight = FontWeight.SemiBold,
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
                    .background(SuqiPrimaryTint),
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

@OptIn(ExperimentalLayoutApi::class)
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
    var description by remember { mutableStateOf("High quality premium product with standard warranty.") }

    // Tax handling (Plan §5.2)
    var selectedTax by remember { mutableStateOf("15% Standard VAT Included") }
    val taxOptions = remember { listOf("15% Standard VAT Included", "0% Exempt / Turn-Over Tax (TOT)", "Custom (5%)") }

    // Real Media Picker with Photo & Video support
    val selectedImages = remember {
        mutableStateListOf(
            "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500",
            "https://images.unsplash.com/photo-1484704849700-f032a568e944?w=500"
        )
    }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            uris.forEach { selectedImages.add(it.toString()) }
            viewModel.showToast("${uris.size} media file(s) added")
        }
    }

    // Variations Builder
    val availableColors = remember { listOf("Black", "White", "Navy Blue", "Space Grey", "Habesha Gold", "Crimson Red") }
    val selectedColors = remember { mutableStateListOf("Black", "White") }
    var customColorInput by remember { mutableStateOf("") }

    val availableSizes = remember { listOf("S", "M", "L", "XL", "Free Size", "64GB", "128GB", "256GB") }
    val selectedSizes = remember { mutableStateListOf("128GB", "256GB") }
    var customSizeInput by remember { mutableStateOf("") }

    // Publish Gate Dialog
    var showMissingPaymentDialog by remember { mutableStateOf(false) }

    // Dynamic attributes by category (Plan §5.2)
    var attr1 by remember { mutableStateOf("") } // Brand / Material / Dimensions
    var attr2 by remember { mutableStateOf("") } // Warranty / Gender / Expiry

    // Multilingual names and descriptions (Plan §5.2)
    var showMultilingual by remember { mutableStateOf(false) }
    var nameAmharic by remember { mutableStateOf("") }
    var descAmharic by remember { mutableStateOf("") }
    var nameOromo by remember { mutableStateOf("") }
    var descOromo by remember { mutableStateOf("") }

    val categories = listOf(
        "cat_electronics" to "Electronics",
        "cat_fashion" to "Fashion",
        "cat_home" to "Home & Living",
        "cat_beauty" to "Beauty & Care",
        "cat_groceries" to "Groceries",
        "cat_sports" to "Sports"
    )

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
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp
                )
            }
        },
        bottomBar = {
            Surface(
                color = LocalSuqiColors.current.cardBackground,
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
                                // Publish Gate: Shop must have at least 1 active payment method
                                val payRes = viewModel.repository.getShopPaymentMethods("shop_techhub")
                                if (payRes.success && payRes.data.isNullOrEmpty()) {
                                    showMissingPaymentDialog = true
                                    return@launch
                                }

                                val p = (priceStr.toDoubleOrNull() ?: 4999.0)
                                val d = (discountStr.toIntOrNull() ?: 0)
                                val s = (stockStr.toIntOrNull() ?: 10)
                                val res = viewModel.repository.createProduct(
                                    mapOf(
                                        "name" to name,
                                        "nameAmharic" to nameAmharic,
                                        "nameAfaanOromo" to nameOromo,
                                        "price" to p,
                                        "discountPercent" to d,
                                        "stockQuantity" to s,
                                        "stockHoldEnabled" to stockHold,
                                        "categoryId" to categoryId,
                                        "tax" to selectedTax,
                                        "colors" to selectedColors.joinToString(", "),
                                        "sizes" to selectedSizes.joinToString(", "),
                                        "images" to selectedImages.toList(),
                                        "description" to description,
                                        "descriptionAmharic" to descAmharic,
                                        "descriptionAfaanOromo" to descOromo,
                                        "attribute1" to attr1,
                                        "attribute2" to attr2
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
            // Photos & Media Upload Box with Real Photo Picker (Checklist 18 & Mockup 3)
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SuqiSecondaryBackground),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(SuqiPrimary.copy(alpha = 0.4f))
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(SuqiPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(LucideIcons.ImagePlus, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add Photos & Videos (${selectedImages.size}/10)",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = SuqiPrimaryText
                        )
                        Text(
                            text = "Tap to choose images, GIFs or videos from device",
                            fontFamily = SuqiFontFamily,
                            fontSize = 12.sp,
                            color = SuqiMutedText
                        )
                    }
                }

                // Selected Media Preview Strip with Remove Badge
                if (selectedImages.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(selectedImages) { mediaUrl ->
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SuqiSecondaryBackground)
                                    .border(1.dp, SuqiBorder, RoundedCornerShape(14.dp))
                            ) {
                                AsyncImage(
                                    model = mediaUrl,
                                    contentDescription = "Uploaded media",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                // Delete Badge (X)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.7f))
                                        .clickable { selectedImages.remove(mediaUrl) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = LucideIcons.X,
                                        contentDescription = "Remove",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Category Selector Chips (Plan §5.2)
            item {
                Text(
                    text = "Category *",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = SuqiPrimaryText
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { (id, label) ->
                        val isSel = id == categoryId
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (isSel) SuqiPrimary else SuqiSecondaryBackground)
                                .clickable { categoryId = id }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = label,
                                fontFamily = SuqiFontFamily,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp,
                                color = if (isSel) Color.White else SuqiPrimaryText
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
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

                    // Ethiopian Tax Selector (Plan §5.2)
                    Column {
                        Text(
                            text = "Tax & VAT Status",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = SuqiPrimaryText
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(taxOptions) { opt ->
                                val isSel = opt == selectedTax
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSel) SuqiPrimary.copy(alpha = 0.12f) else SuqiSecondaryBackground)
                                        .border(1.dp, if (isSel) SuqiPrimary else SuqiBorder, RoundedCornerShape(12.dp))
                                        .clickable { selectedTax = opt }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = opt,
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal,
                                        fontSize = 12.sp,
                                        color = if (isSel) SuqiPrimary else SuqiPrimaryText
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
                        label = "Description (English) *",
                        singleLine = false
                    )

                    // Product Variations Builder: Colors & Sizes (Plan §5.2)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SuqiSecondaryBackground),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Product Variations Builder",
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )

                            // Color Variations
                            Column {
                                Text("Colors Available", fontFamily = SuqiFontFamily, fontSize = 12.sp, color = SuqiSecondaryText)
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    for (color in availableColors) {
                                        val isSel = color in selectedColors
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(if (isSel) SuqiPrimary else LocalSuqiColors.current.cardBackground)
                                                .border(1.dp, if (isSel) SuqiPrimary else SuqiBorder, RoundedCornerShape(999.dp))
                                                .clickable {
                                                    if (isSel) selectedColors.remove(color) else selectedColors.add(color)
                                                }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = color,
                                                fontFamily = SuqiFontFamily,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 12.sp,
                                                color = if (isSel) Color.White else SuqiPrimaryText
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = customColorInput,
                                        onValueChange = { customColorInput = it },
                                        placeholder = { Text("Add custom color...", fontSize = 12.sp) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Button(
                                        onClick = {
                                            if (customColorInput.isNotBlank()) {
                                                selectedColors.add(customColorInput.trim())
                                                customColorInput = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(48.dp)
                                    ) {
                                        Text("+ Add", fontSize = 12.sp)
                                    }
                                }
                            }

                            HorizontalDivider(color = SuqiBorder)

                            // Size / Spec Variations
                            Column {
                                Text("Sizes / Capacity", fontFamily = SuqiFontFamily, fontSize = 12.sp, color = SuqiSecondaryText)
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    for (sz in availableSizes) {
                                        val isSel = sz in selectedSizes
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(if (isSel) SuqiPrimary else LocalSuqiColors.current.cardBackground)
                                                .border(1.dp, if (isSel) SuqiPrimary else SuqiBorder, RoundedCornerShape(999.dp))
                                                .clickable {
                                                    if (isSel) selectedSizes.remove(sz) else selectedSizes.add(sz)
                                                }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = sz,
                                                fontFamily = SuqiFontFamily,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 12.sp,
                                                color = if (isSel) Color.White else SuqiPrimaryText
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = customSizeInput,
                                        onValueChange = { customSizeInput = it },
                                        placeholder = { Text("Add custom size / spec...", fontSize = 12.sp) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Button(
                                        onClick = {
                                            if (customSizeInput.isNotBlank()) {
                                                selectedSizes.add(customSizeInput.trim())
                                                customSizeInput = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(48.dp)
                                    ) {
                                        Text("+ Add", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Dynamic Category Attributes (Plan §5.2)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SuqiSecondaryBackground),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Category-Specific Attributes",
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            when (categoryId) {
                                "cat_electronics" -> {
                                    SuqiTextField(value = attr1, onValueChange = { attr1 = it }, label = "Brand / Manufacturer", placeholder = "e.g., Apple, Samsung, Sony")
                                    SuqiTextField(value = attr2, onValueChange = { attr2 = it }, label = "Warranty Period", placeholder = "e.g., 6 Months, 1 Year")
                                }
                                "cat_fashion" -> {
                                    SuqiTextField(value = attr1, onValueChange = { attr1 = it }, label = "Material / Fabric", placeholder = "e.g., 100% Cotton, Ethiopian Shemma")
                                    SuqiTextField(value = attr2, onValueChange = { attr2 = it }, label = "Gender & Fit", placeholder = "e.g., Unisex, Slim Fit")
                                }
                                "cat_home" -> {
                                    SuqiTextField(value = attr1, onValueChange = { attr1 = it }, label = "Dimensions & Weight", placeholder = "e.g., 120 x 60 x 75 cm")
                                    SuqiTextField(value = attr2, onValueChange = { attr2 = it }, label = "Material", placeholder = "e.g., Solid Wood, Metal")
                                }
                                else -> {
                                    SuqiTextField(value = attr1, onValueChange = { attr1 = it }, label = "Product Origin / Brand", placeholder = "e.g., Made in Ethiopia")
                                    SuqiTextField(value = attr2, onValueChange = { attr2 = it }, label = "Shelf Life / Packaging", placeholder = "e.g., 12 Months")
                                }
                            }
                        }
                    }

                    // Multilingual Accordion (Plan §5.2: Amharic & Afaan Oromo)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SuqiSecondaryBackground),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
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
                                        fontSize = 13.sp
                                    )
                                }
                                Icon(
                                    imageVector = LucideIcons.ChevronDown,
                                    contentDescription = null,
                                    tint = SuqiSecondaryText,
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
        }
    }

    if (showMissingPaymentDialog) {
        AlertDialog(
            onDismissRequest = { showMissingPaymentDialog = false },
            icon = { Icon(LucideIcons.ShieldCheck, contentDescription = null, tint = SuqiPrimary) },
            title = { Text("Payment Method Required", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "You must configure at least one active payment method (Telebirr, CBE Birr, Awash, or Bank Transfer) before publishing products to buyers.",
                    fontFamily = SuqiFontFamily,
                    fontSize = 14.sp,
                    color = SuqiSecondaryText
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
            }
        )
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
                    fontWeight = FontWeight.SemiBold,
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
                    fontWeight = FontWeight.SemiBold,
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
                        .background(SuqiWarningBg)
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
                                        text = "${order.customerName} • ${order.customerPhone}",
                                        fontFamily = SuqiFontFamily,
                                        fontSize = 13.sp,
                                        color = SuqiSecondaryText
                                    )
                                    Text(
                                        text = "Submitted ${order.createdAt.take(16)}",
                                        fontFamily = SuqiFontFamily,
                                        fontSize = 11.sp,
                                        color = SuqiMutedText
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

                            Spacer(modifier = Modifier.height(10.dp))

                            // Audit trail compliance note (Plan §5.2)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SuqiSecondaryBackground)
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(LucideIcons.ShieldCheck, contentDescription = null, tint = SuqiSecondaryText, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Audit trail: Verification action will log actor ID & timestamp.",
                                    fontFamily = SuqiFontFamily,
                                    fontSize = 11.sp,
                                    color = SuqiSecondaryText
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

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
            containerColor = LocalSuqiColors.current.cardBackground,
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
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp
                )
            }
        },
        bottomBar = {
            Surface(
                color = LocalSuqiColors.current.cardBackground,
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SuqiButton(
                        text = "Save Changes",
                        onClick = {
                            if (methods.isEmpty()) {
                                viewModel.showToast("At least 1 active payment method is required to accept orders!")
                            } else {
                                viewModel.showToast("Payment methods saved successfully!")
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
                val hasMethods = methods.isNotEmpty()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (hasMethods) LocalSuqiColors.current.primaryTint else SuqiWarningBg)
                        .border(
                            1.dp,
                            if (hasMethods) SuqiPrimary.copy(alpha = 0.3f) else SuqiWarning.copy(alpha = 0.5f),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (hasMethods) LucideIcons.Info else LucideIcons.TriangleAlert,
                        contentDescription = null,
                        tint = if (hasMethods) SuqiPrimary else SuqiWarning,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (hasMethods) {
                            "Add or manage payment methods for your shop. Customers will use these accounts to pay you directly."
                        } else {
                            "Store Activation Required: Add at least 1 active payment method (Telebirr, CBE, etc.) before your shop can accept customer orders."
                        },
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
                                        .background(SuqiSuccessBg)
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
            containerColor = LocalSuqiColors.current.cardBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun DeliverySetupScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    var standardEnabled by remember { mutableStateOf(true) }
    var standardFee by remember { mutableStateOf("50") }
    var standardEta by remember { mutableStateOf("1-3 business days") }

    var expressEnabled by remember { mutableStateOf(true) }
    var expressFee by remember { mutableStateOf("120") }
    var expressCutoff by remember { mutableStateOf("2:00 PM (Same-Day Delivery)") }

    var pickupEnabled by remember { mutableStateOf(true) }
    var pickupAddress by remember { mutableStateOf("Bole Medhanialem, Edna Mall 2nd Floor #204, Addis Ababa") }
    var pickupHours by remember { mutableStateOf("Mon - Sat: 9:00 AM - 7:00 PM") }

    var coverageAreas by remember { mutableStateOf("Addis Ababa (Bole, Kirkos, Yeka, Arada, Nifas Silk), Adama, Hawassa, Bishoftu") }

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
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp
                )
            }
        },
        bottomBar = {
            Surface(
                color = LocalSuqiColors.current.cardBackground,
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SuqiButton(
                        text = "Save Delivery Settings",
                        onClick = {
                            viewModel.showToast("Delivery settings and rates saved successfully!")
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Standard Delivery Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SuqiSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SuqiBorder)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(LucideIcons.Truck, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Standard Courier Delivery", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("Reliable nationwide & city delivery", fontSize = 12.sp, color = SuqiSecondaryText)
                                }
                            }
                            Switch(
                                checked = standardEnabled,
                                onCheckedChange = { standardEnabled = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = SuqiPrimary)
                            )
                        }

                        if (standardEnabled) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                SuqiTextField(
                                    value = standardFee,
                                    onValueChange = { standardFee = it },
                                    label = "Fee (ETB)",
                                    modifier = Modifier.weight(1f)
                                )
                                SuqiTextField(
                                    value = standardEta,
                                    onValueChange = { standardEta = it },
                                    label = "Estimated Window",
                                    modifier = Modifier.weight(1.5f)
                                )
                            }
                        }
                    }
                }
            }

            // Express Delivery Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SuqiSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SuqiBorder)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(LucideIcons.Zap, contentDescription = null, tint = SuqiWarning, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Express Same-Day Delivery", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("Fast delivery within Addis Ababa", fontSize = 12.sp, color = SuqiSecondaryText)
                                }
                            }
                            Switch(
                                checked = expressEnabled,
                                onCheckedChange = { expressEnabled = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = SuqiPrimary)
                            )
                        }

                        if (expressEnabled) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                SuqiTextField(
                                    value = expressFee,
                                    onValueChange = { expressFee = it },
                                    label = "Fee (ETB)",
                                    modifier = Modifier.weight(1f)
                                )
                                SuqiTextField(
                                    value = expressCutoff,
                                    onValueChange = { expressCutoff = it },
                                    label = "Order Cut-off Time",
                                    modifier = Modifier.weight(1.5f)
                                )
                            }
                        }
                    }
                }
            }

            // Store Pickup Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SuqiSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SuqiBorder)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(LucideIcons.Store, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("In-Store Customer Pickup", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("Buyers pick up at physical shop (Free)", fontSize = 12.sp, color = SuqiSecondaryText)
                                }
                            }
                            Switch(
                                checked = pickupEnabled,
                                onCheckedChange = { pickupEnabled = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = SuqiPrimary)
                            )
                        }

                        if (pickupEnabled) {
                            Spacer(modifier = Modifier.height(14.dp))
                            SuqiTextField(
                                value = pickupAddress,
                                onValueChange = { pickupAddress = it },
                                label = "Store Address & Landmark",
                                singleLine = false
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            SuqiTextField(
                                value = pickupHours,
                                onValueChange = { pickupHours = it },
                                label = "Pickup Operating Hours"
                            )
                        }
                    }
                }
            }

            // Delivery Coverage Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SuqiSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SuqiBorder)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Active Coverage Areas & Cities", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Specify which sub-cities and towns your shop dispatches to", fontSize = 12.sp, color = SuqiSecondaryText)
                        Spacer(modifier = Modifier.height(12.dp))
                        SuqiTextField(
                            value = coverageAreas,
                            onValueChange = { coverageAreas = it },
                            label = "Coverage Locations (Comma Separated)",
                            singleLine = false
                        )
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
    val coroutineScope = rememberCoroutineScope()
    var promoCode by remember { mutableStateOf("HOLIDAY20") }
    var discountType by remember { mutableStateOf("PERCENTAGE") } // PERCENTAGE or FIXED_BIRR
    var discountValue by remember { mutableStateOf("20") }
    var minOrder by remember { mutableStateOf("500") }
    var usageLimit by remember { mutableStateOf("50") }
    var startDate by remember { mutableStateOf("Nov 1, 2026") }
    var endDate by remember { mutableStateOf("Dec 31, 2026") }

    var promoList by remember {
        mutableStateOf(
            listOf(
                PromoCodeDto(
                    id = "promo_1",
                    shopId = "shop_techhub",
                    code = "SUMMER25",
                    discountType = "PERCENTAGE",
                    discountValue = 25.0,
                    minOrderAmount = 1000.0,
                    validUntil = "Dec 31, 2026"
                ),
                PromoCodeDto(
                    id = "promo_2",
                    shopId = "shop_techhub",
                    code = "WELCOME100",
                    discountType = "FIXED_BIRR",
                    discountValue = 100.0,
                    minOrderAmount = 800.0,
                    validUntil = "Nov 30, 2026"
                )
            )
        )
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
                    text = "Promotions & Discounts",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp
                )
            }
        },
        containerColor = SuqiBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(text = "Active Shop Promos (${promoList.size})", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            items(promoList) { promo ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = LocalSuqiColors.current.primaryTint),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(SuqiPrimary.copy(alpha = 0.3f))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = promo.code, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                SuqiStatusBadge("Active", StatusType.SUCCESS)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val discText = if (promo.discountType == "PERCENTAGE") "${promo.discountValue.toInt()}% OFF" else "ETB ${promo.discountValue.toInt()} OFF"
                            Text(text = "$discText • Min. ETB ${promo.minOrderAmount.toInt()}", fontSize = 13.sp, color = SuqiSecondaryText)
                            Text(text = "Usage limit: 50 uses • Valid until ${promo.validUntil}", fontSize = 12.sp, color = SuqiMutedText)
                        }

                        IconButton(
                            onClick = {
                                promoList = promoList.filter { it.id != promo.id }
                                viewModel.showToast("Promo ${promo.code} deactivated")
                            }
                        ) {
                            Icon(LucideIcons.Trash2, contentDescription = "Deactivate", tint = SuqiError)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Create New Promo Code", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SuqiSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SuqiBorder)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        SuqiTextField(value = promoCode, onValueChange = { promoCode = it }, label = "Code Name (e.g. HOLIDAY20) *")

                        // Discount Type Toggle
                        Column {
                            Text("Discount Type", fontFamily = SuqiFontFamily, fontSize = 12.sp, color = SuqiSecondaryText)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                val isPct = discountType == "PERCENTAGE"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isPct) SuqiPrimary else SuqiSecondaryBackground)
                                        .clickable { discountType = "PERCENTAGE" }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Percentage (%)", color = if (isPct) Color.White else SuqiPrimaryText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                                val isFix = discountType == "FIXED_BIRR"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isFix) SuqiPrimary else SuqiSecondaryBackground)
                                        .clickable { discountType = "FIXED_BIRR" }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Fixed Birr (ETB)", color = if (isFix) Color.White else SuqiPrimaryText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
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

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SuqiTextField(
                                value = usageLimit,
                                onValueChange = { usageLimit = it },
                                label = "Total Uses Limit",
                                modifier = Modifier.weight(1f)
                            )
                            SuqiTextField(
                                value = endDate,
                                onValueChange = { endDate = it },
                                label = "End Expiry Date",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        SuqiButton(
                            text = "Create Promo Code",
                            onClick = {
                                if (promoCode.isBlank()) {
                                    viewModel.showToast("Please enter a promo code name")
                                    return@SuqiButton
                                }
                                val newPromo = PromoCodeDto(
                                    id = "promo_${System.currentTimeMillis()}",
                                    shopId = "shop_techhub",
                                    code = promoCode.trim().uppercase(),
                                    discountType = discountType,
                                    discountValue = discountValue.toDoubleOrNull() ?: 10.0,
                                    minOrderAmount = minOrder.toDoubleOrNull() ?: 0.0,
                                    validUntil = endDate
                                )
                                promoList = promoList + newPromo
                                viewModel.showToast("Promo Code ${newPromo.code} created!")
                                promoCode = ""
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
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
                Text(text = "Shop Settings", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
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
                Text(text = "Seller Verification", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
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
                    .background(SuqiPrimaryTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(LucideIcons.BadgeCheck, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Get Your Shop Verified",
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.SemiBold,
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
    var adminSettings by remember { mutableStateOf(AdminSettingsDto()) }

    LaunchedEffect(Unit) {
        val res = viewModel.repository.getAdminSettings()
        if (res.success && res.data != null) {
            adminSettings = res.data
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
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(SuqiSecondaryBackground)
                ) {
                    Icon(LucideIcons.ArrowLeft, contentDescription = "Back", tint = SuqiPrimaryText)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "Seller Subscription", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
            }
        },
        containerColor = SuqiBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(20.dp)
        ) {
            // Feature flag check: If subscription mode is disabled by admin, show Free Pilot notice
            if (!adminSettings.isSubscriptionModeEnabled) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SuqiSuccessBg),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SuqiSuccess.copy(alpha = 0.4f))),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(LucideIcons.Sparkles, contentDescription = null, tint = SuqiSuccess, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Free Pilot Launch Active", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SuqiSuccess)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "All seller tools (unlimited products, sales charts, featured placement) are 100% free for all Ethiopian merchants during nationwide rollout!",
                                fontFamily = SuqiFontFamily,
                                fontSize = 12.sp,
                                color = SuqiPrimaryText
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = LocalSuqiColors.current.primaryTint),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SuqiPrimary)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pro Seller Plan", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = SuqiPrimary)
                        SuqiStatusBadge(if (adminSettings.isSubscriptionModeEnabled) "Active" else "Free Pilot", StatusType.SUCCESS)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Unlock more features and grow your business faster.", fontSize = 13.sp, color = SuqiSecondaryText)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        if (adminSettings.isSubscriptionModeEnabled) "ETB 299 / month" else "ETB 0 / Free Pilot",
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp,
                        color = SuqiPrimaryText
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("✓ Featured Shop Placement in search", fontSize = 14.sp)
                    Text("✓ Advanced Sales Analytics & Charts", fontSize = 14.sp)
                    Text("✓ Unlimited Promo Codes & Discounts", fontSize = 14.sp)
                    Text("✓ Priority Merchant Support", fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Current Billing Status", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                if (adminSettings.isSubscriptionModeEnabled) "Next Billing Date: Dec 31, 2026\nPayment Method: Telebirr Auto-Debit"
                else "Status: 100% Free Pilot (No charges or card required)",
                fontSize = 13.sp,
                color = SuqiSecondaryText
            )

            Spacer(modifier = Modifier.height(24.dp))

            SuqiButton(
                text = if (adminSettings.isSubscriptionModeEnabled) "Manage Subscription" else "Back to Studio",
                onClick = {
                    if (adminSettings.isSubscriptionModeEnabled) viewModel.showToast("Subscription settings updated")
                    else viewModel.goBack()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SellerOrdersScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var orders by remember { mutableStateOf<List<OrderDto>>(emptyList()) }
    var selectedTab by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    fun loadOrders() {
        coroutineScope.launch {
            isLoading = true
            val res = viewModel.repository.getOrders()
            if (res.success && res.data != null) {
                orders = res.data
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadOrders()
    }

    val tabs = listOf("All", "Awaiting Verification", "Confirmed", "Dispatched", "Delivered", "Cancelled")

    val filteredOrders = orders.filter { order ->
        val matchesTab = when (selectedTab) {
            "All" -> true
            "Awaiting Verification" -> order.paymentStatus == "PENDING_VERIFICATION"
            "Confirmed" -> order.orderStatus == "Confirmed" || order.paymentStatus == "VERIFIED"
            "Dispatched" -> order.orderStatus == "Dispatched"
            "Delivered" -> order.orderStatus == "Delivered"
            "Cancelled" -> order.orderStatus == "Cancelled" || order.paymentStatus == "REJECTED"
            else -> true
        }
        val matchesSearch = if (searchQuery.isBlank()) true else {
            order.orderReference.contains(searchQuery, ignoreCase = true) ||
            order.customerName.contains(searchQuery, ignoreCase = true) ||
            order.customerPhone.contains(searchQuery, ignoreCase = true)
        }
        matchesTab && matchesSearch
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(LocalSuqiColors.current.surfaceBackground)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
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
                    Text(text = "Shop Orders", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by reference, buyer name or phone...", fontSize = 13.sp, color = SuqiMutedText) },
                    leadingIcon = { Icon(LucideIcons.Search, contentDescription = null, tint = SuqiSecondaryText, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SuqiSecondaryBackground,
                        unfocusedContainerColor = SuqiSecondaryBackground,
                        focusedBorderColor = SuqiPrimary,
                        unfocusedBorderColor = SuqiBorder
                    )
                )

                // Tabs Row
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tabs) { tab ->
                        val isSel = tab == selectedTab
                        val count = when (tab) {
                            "All" -> orders.size
                            "Awaiting Verification" -> orders.count { it.paymentStatus == "PENDING_VERIFICATION" }
                            "Confirmed" -> orders.count { it.orderStatus == "Confirmed" || it.paymentStatus == "VERIFIED" }
                            "Dispatched" -> orders.count { it.orderStatus == "Dispatched" }
                            "Delivered" -> orders.count { it.orderStatus == "Delivered" }
                            "Cancelled" -> orders.count { it.orderStatus == "Cancelled" || it.paymentStatus == "REJECTED" }
                            else -> 0
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (isSel) SuqiPrimary else SuqiSecondaryBackground)
                                .clickable { selectedTab = tab }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = "$tab ($count)",
                                fontFamily = SuqiFontFamily,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp,
                                color = if (isSel) Color.White else SuqiPrimaryText
                            )
                        }
                    }
                }
            }
        },
        containerColor = SuqiBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (filteredOrders.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Text("No orders in \"$selectedTab\"", fontFamily = SuqiFontFamily, color = SuqiSecondaryText)
                    }
                }
            } else {
                items(filteredOrders) { order ->
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
                            viewModel.navigate(ScreenDestination.OrderDetails(order.id))
                        }
                    )
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
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SuqiSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SuqiBorder)),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onViewDetails)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Order #${order.orderReference}", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(text = "Buyer: ${order.customerName} • ${order.customerPhone}", fontSize = 12.sp, color = SuqiSecondaryText)
                }
                SuqiStatusBadge(
                    statusText = if (order.paymentStatus == "PENDING_VERIFICATION") "Awaiting Payment" else order.orderStatus,
                    type = when (order.paymentStatus) {
                        "PENDING_VERIFICATION" -> StatusType.WARNING
                        "REJECTED" -> StatusType.ERROR
                        else -> StatusType.SUCCESS
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Amount: ${formatEtb(order.totalAmount)}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = "Method: ${order.paymentMethodTitle}", fontSize = 13.sp, color = SuqiSecondaryText)
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

            // Audit Trail Badge
            if (order.verifiedBy != null || order.verifiedAt != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SuqiSuccessBg)
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Icon(LucideIcons.ShieldCheck, contentDescription = null, tint = SuqiSuccess, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Verified by ${order.verifiedBy ?: "Merchant"} on ${order.verifiedAt ?: "record"}",
                        fontFamily = SuqiFontFamily,
                        fontSize = 11.sp,
                        color = SuqiSuccess
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (order.paymentStatus == "PENDING_VERIFICATION") {
                    Button(
                        onClick = onVerify,
                        colors = ButtonDefaults.buttonColors(containerColor = SuqiSuccess),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Text("Verify Payment", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else if (order.orderStatus == "Confirmed") {
                    Button(
                        onClick = onDispatch,
                        colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Text("Mark Dispatched", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (order.orderStatus == "Dispatched") {
                    Button(
                        onClick = onDeliver,
                        colors = ButtonDefaults.buttonColors(containerColor = SuqiSuccess),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Text("Mark Delivered", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                OutlinedButton(
                    onClick = onChat,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SuqiBorder),
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(LucideIcons.MessageCircle, contentDescription = null, tint = SuqiPrimaryText, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Chat", fontSize = 12.sp, color = SuqiPrimaryText)
                }
            }
        }
    }
}
