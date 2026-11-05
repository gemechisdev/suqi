package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
fun NotificationsScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    var notifications by remember { mutableStateOf<List<NotificationDto>>(emptyList()) }

    LaunchedEffect(Unit) {
        val res = viewModel.repository.getNotifications()
        if (res.success && res.data != null) {
            notifications = res.data
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
                Text(text = "Notifications", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
            }
        },
        containerColor = SuqiBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp)
        ) {
            if (notifications.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Text("No notifications yet.", color = SuqiSecondaryText)
                    }
                }
            } else {
                items(notifications) { notif ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = if (notif.isRead) LocalSuqiColors.current.cardBackground else LocalSuqiColors.current.primaryTint),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SuqiBorder)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(SuqiPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (notif.type) {
                                        "PAYMENT" -> LucideIcons.Wallet
                                        "CHAT" -> LucideIcons.MessageCircle
                                        else -> LucideIcons.Truck
                                    },
                                    contentDescription = null,
                                    tint = SuqiPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = notif.title, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = notif.body, fontFamily = SuqiFontFamily, fontSize = 13.sp, color = SuqiSecondaryText)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = notif.timestamp, fontFamily = SuqiFontFamily, fontSize = 11.sp, color = SuqiMutedText)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SavedAddressesScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var addresses by remember { mutableStateOf<List<SavedAddressDto>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }

    fun loadAddresses() {
        coroutineScope.launch {
            val res = viewModel.repository.getSavedAddresses()
            if (res.success && res.data != null) {
                addresses = res.data
            }
        }
    }

    LaunchedEffect(Unit) {
        loadAddresses()
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
                Text(text = "Saved Addresses", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = SuqiPrimary,
                contentColor = Color.White
            ) {
                Icon(LucideIcons.Plus, contentDescription = "Add Address")
            }
        },
        containerColor = SuqiBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(addresses) { addr ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = LocalSuqiColors.current.cardBackground),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(if (addr.isDefault) SuqiPrimary else SuqiBorder)
                    ),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = addr.fullName, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            if (addr.isDefault) {
                                SuqiStatusBadge("Default", StatusType.SUCCESS)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = addr.phone, fontFamily = SuqiFontFamily, fontSize = 13.sp, color = SuqiSecondaryText)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "${addr.streetOrLandmark}, ${addr.subCity}, ${addr.city}", fontFamily = SuqiFontFamily, fontSize = 13.sp)
                        if (addr.deliveryInstructions.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Note: ${addr.deliveryInstructions}", fontSize = 12.sp, color = SuqiMutedText)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("+251 ") }
        var address by remember { mutableStateOf("") }
        var subCity by remember { mutableStateOf("Bole") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Delivery Address", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SuqiTextField(value = name, onValueChange = { name = it }, label = "Full Name")
                    SuqiTextField(value = phone, onValueChange = { phone = it }, label = "Phone (+251)")
                    SuqiTextField(value = address, onValueChange = { address = it }, label = "Street & House No")
                    SuqiTextField(value = subCity, onValueChange = { subCity = it }, label = "Sub-City")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.repository.saveAddress(
                                SavedAddressDto(
                                    id = "",
                                    fullName = name,
                                    phone = phone,
                                    region = "Addis Ababa",
                                    city = "Addis Ababa",
                                    subCity = subCity,
                                    streetOrLandmark = address
                                )
                            )
                            loadAddresses()
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary)
                ) { Text("Save Address") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } },
            containerColor = LocalSuqiColors.current.cardBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun WishlistScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var products by remember { mutableStateOf<List<ProductDto>>(emptyList()) }

    fun loadWishlist() {
        coroutineScope.launch {
            val res = viewModel.repository.getProducts()
            if (res.success && res.data != null) {
                products = res.data.filter { it.isWishlisted }
            }
        }
    }

    LaunchedEffect(Unit) {
        loadWishlist()
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
                Text(text = "Wishlist (${products.size})", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
            }
        },
        containerColor = SuqiBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        if (products.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Your wishlist is empty.", color = SuqiSecondaryText)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(products) { prod ->
                    SearchResultProductRow(
                        product = prod,
                        onClick = { viewModel.navigate(ScreenDestination.ProductDetails(prod.id)) },
                        onToggleWishlist = {
                            coroutineScope.launch {
                                viewModel.repository.toggleWishlist(prod.id)
                                loadWishlist()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ReviewsScreen(
    targetId: String,
    targetName: String,
    targetType: String,
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var reviews by remember { mutableStateOf<List<ReviewDto>>(emptyList()) }
    var rating by remember { mutableDoubleStateOf(5.0) }
    var comment by remember { mutableStateOf("") }
    var showWriteDialog by remember { mutableStateOf(false) }

    fun loadReviews() {
        coroutineScope.launch {
            val res = viewModel.repository.getReviews(targetId)
            if (res.success && res.data != null) {
                reviews = res.data
            }
        }
    }

    LaunchedEffect(targetId) {
        loadReviews()
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
                Text(text = "Reviews: $targetName", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showWriteDialog = true },
                containerColor = SuqiPrimary,
                contentColor = Color.White
            ) {
                Icon(LucideIcons.MessageSquarePlus, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Write Review")
            }
        },
        containerColor = SuqiBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(reviews) { rev ->
                ReviewItemCard(review = rev)
            }
        }
    }

    if (showWriteDialog) {
        AlertDialog(
            onDismissRequest = { showWriteDialog = false },
            title = { Text("Write a Review", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select Rating:")
                    Row {
                        for (i in 1..5) {
                            IconButton(onClick = { rating = i.toDouble() }, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    LucideIcons.StarFilled,
                                    contentDescription = null,
                                    tint = if (i <= rating) SuqiWarning else SuqiBorder
                                )
                            }
                        }
                    }
                    SuqiTextField(value = comment, onValueChange = { comment = it }, label = "Your Review", singleLine = false)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (comment.isNotBlank()) {
                            coroutineScope.launch {
                                viewModel.repository.submitReview(
                                    mapOf(
                                        "targetId" to targetId,
                                        "targetType" to targetType,
                                        "rating" to rating,
                                        "comment" to comment
                                    )
                                )
                                comment = ""
                                showWriteDialog = false
                                loadReviews()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary)
                ) { Text("Submit") }
            },
            dismissButton = { TextButton(onClick = { showWriteDialog = false }) { Text("Cancel") } },
            containerColor = LocalSuqiColors.current.cardBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun AdminConsoleScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
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
                Text(text = "Admin Console", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
            }
        },
        containerColor = SuqiBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Stats 3 Cards (Mockup 5)
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricStatCard("Total Users", "2.4K", "Active", Modifier.weight(1f))
                    MetricStatCard("Total Shops", "845", "+18 this wk", Modifier.weight(1f))
                    MetricStatCard("Total Orders", "18K", "ETB 2.8M", Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(24.dp))

                Text(text = "Admin Quick Actions", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickActionTile(LucideIcons.BadgeCheck, "Verify Sellers", Modifier.weight(1f)) {
                        viewModel.navigate(ScreenDestination.AdminFeatures)
                    }
                    QuickActionTile(LucideIcons.MessageSquarePlus, "Moderate Reviews", Modifier.weight(1f)) {
                        viewModel.navigate(ScreenDestination.AdminModeration)
                    }
                    QuickActionTile(LucideIcons.GalleryHorizontal, "Banners", Modifier.weight(1f)) {
                        viewModel.navigate(ScreenDestination.AdminBanners)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickActionTile(LucideIcons.Tag, "Categories & Types", Modifier.weight(1f)) {
                        viewModel.navigate(ScreenDestination.AdminCategories)
                    }
                    QuickActionTile(LucideIcons.Award, "Subscription Plans", Modifier.weight(1f)) {
                        viewModel.navigate(ScreenDestination.AdminSubscriptionPlans)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                Text(text = "Platform Feature Toggles", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = LocalSuqiColors.current.cardBackground),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SuqiBorder)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        FeatureToggleRow(
                            title = "In-App Chat Service",
                            sub = "Allow direct messaging between buyers and shops",
                            checked = adminSettings.isChatEnabled,
                            onToggle = {
                                coroutineScope.launch {
                                    viewModel.repository.updateFeatureToggle("chat", it)
                                    adminSettings = adminSettings.copy(isChatEnabled = it)
                                }
                            }
                        )
                        HorizontalDivider(color = SuqiBorder, modifier = Modifier.padding(vertical = 12.dp))

                        FeatureToggleRow(
                            title = "External Contact Buttons",
                            sub = "Show call, Telegram and WhatsApp links",
                            checked = adminSettings.isExternalContactEnabled,
                            onToggle = {
                                coroutineScope.launch {
                                    viewModel.repository.updateFeatureToggle("contact", it)
                                    adminSettings = adminSettings.copy(isExternalContactEnabled = it)
                                }
                            }
                        )
                        HorizontalDivider(color = SuqiBorder, modifier = Modifier.padding(vertical = 12.dp))

                        FeatureToggleRow(
                            title = "Subscription Monetization Mode",
                            sub = "Switch between Free mode and Subscription plans",
                            checked = adminSettings.isSubscriptionModeEnabled,
                            onToggle = {
                                coroutineScope.launch {
                                    viewModel.repository.updateFeatureToggle("subscription", it)
                                    adminSettings = adminSettings.copy(isSubscriptionModeEnabled = it)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FeatureToggleRow(
    title: String,
    sub: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(text = sub, fontFamily = SuqiFontFamily, fontSize = 12.sp, color = SuqiSecondaryText)
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedTrackColor = SuqiPrimary)
        )
    }
}

@Composable
fun AdminFeaturesScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var pendingShops by remember { mutableStateOf<List<ShopDto>>(emptyList()) }

    fun loadShops() {
        coroutineScope.launch {
            val res = viewModel.repository.getAdminSellerVerificationQueue()
            if (res.success && res.data != null) {
                pendingShops = res.data
            }
        }
    }

    LaunchedEffect(Unit) {
        loadShops()
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
                Text(text = "Admin Seller Verification", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
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
                Text("Seller Verification Queue", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (pendingShops.isEmpty()) {
                item {
                    Text("No shops currently awaiting verification.", color = SuqiSecondaryText)
                }
            } else {
                items(pendingShops) { shop ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = LocalSuqiColors.current.cardBackground),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SuqiBorder)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(text = shop.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(text = "${shop.category} • ${shop.address}", fontSize = 13.sp, color = SuqiSecondaryText)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.repository.adminVerifySellerDecision(shop.id, "APPROVE")
                                            viewModel.showToast("${shop.name} approved!")
                                            loadShops()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SuqiSuccess),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) { Text("Approve") }

                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.repository.adminVerifySellerDecision(shop.id, "REJECT")
                                            viewModel.showToast("${shop.name} rejected.")
                                            loadShops()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SuqiError),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) { Text("Reject") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var language by remember { mutableStateOf(viewModel.selectedLanguage.value) }
    val colors = LocalSuqiColors.current
    val isDarkPref by viewModel.isDarkMode.collectAsState()

    val curRegion by viewModel.selectedRegion.collectAsState()
    val curCity by viewModel.selectedCity.collectAsState()
    val curSubCity by viewModel.selectedSubCity.collectAsState()
    var showLocationSheet by remember { mutableStateOf(false) }

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
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(colors.secondaryBackground)
                ) {
                    Icon(LucideIcons.ArrowLeft, contentDescription = "Back", tint = colors.primaryText)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "App Settings",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    color = colors.primaryText
                )
            }
        },
        containerColor = colors.surfaceBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Text("Language & Region", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.primaryText)
                Spacer(modifier = Modifier.height(8.dp))

                val langs = listOf("English", "Amharic (አማርኛ)", "Afaan Oromo")
                langs.forEach { l ->
                    val isSel = language.startsWith(l.split(" ").first())
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                language = l
                                viewModel.selectedLanguage.value = l.split(" ").first()
                            }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(l, fontFamily = SuqiFontFamily, fontSize = 14.sp, color = colors.primaryText)
                        if (isSel) {
                            Icon(LucideIcons.Check, contentDescription = null, tint = SuqiPrimary)
                        }
                    }
                }

                // Delivery Location Switcher moved here from Header
                Spacer(modifier = Modifier.height(14.dp))
                Text("Default Delivery Location", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primaryText)
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.cardBackground,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLocationSheet = true }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SuqiPrimaryTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(LucideIcons.MapPin, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$curCity, $curSubCity",
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = colors.primaryText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Region: $curRegion",
                                fontFamily = SuqiFontFamily,
                                fontSize = 12.sp,
                                color = colors.secondaryText
                            )
                        }
                        Text(
                            text = "Change",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = SuqiPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Appearance & Theme", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.primaryText)
                Spacer(modifier = Modifier.height(8.dp))

                val themeOptions = listOf(
                    "System Default" to null,
                    "Light Mode" to false,
                    "Dark Mode" to true
                )

                themeOptions.forEach { (label, value) ->
                    val isSelected = isDarkPref == value
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.setDarkMode(value)
                                viewModel.showToast("Theme set to $label")
                            }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (value) {
                                    true -> LucideIcons.Moon
                                    false -> LucideIcons.Sun
                                    null -> LucideIcons.SunMoon
                                },
                                contentDescription = null,
                                tint = if (isSelected) SuqiPrimary else colors.secondaryText,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = label,
                                fontFamily = SuqiFontFamily,
                                fontSize = 14.sp,
                                color = colors.primaryText,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                        if (isSelected) {
                            Icon(LucideIcons.Check, contentDescription = null, tint = SuqiPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text("Notifications", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.primaryText)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Order & Payment Notifications", fontFamily = SuqiFontFamily, fontSize = 14.sp, color = colors.primaryText)
                        Text("Get notified when sellers verify your payments", fontSize = 12.sp, color = colors.secondaryText)
                    }
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { notificationsEnabled = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = SuqiPrimary)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("Linked Sign-Ins", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.primaryText)
                Spacer(modifier = Modifier.height(8.dp))
                AccountMenuItem(LucideIcons.CircleUser, "Google (Linked: fitsum.gebre@gmail.com)") {}
                AccountMenuItem(LucideIcons.Send, "Telegram (Linked: +251 912 345 678)") {}

                Spacer(modifier = Modifier.height(24.dp))
                TextButton(
                    onClick = {
                        viewModel.showToast("Signed out.")
                        viewModel.popToRoot(ScreenDestination.Auth)
                    }
                ) {
                    Text("Sign Out", color = SuqiError, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showLocationSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLocationSheet = false },
            containerColor = colors.cardBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "Select Delivery Location",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = colors.primaryText
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text("Region", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.secondaryText)
                Spacer(modifier = Modifier.height(6.dp))
                val regions = listOf("Addis Ababa", "Oromia", "Amhara", "Sidama", "Dire Dawa")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    regions.take(3).forEach { r ->
                        val isSel = curRegion == r
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (isSel) SuqiPrimary else colors.secondaryBackground,
                            modifier = Modifier.clickable { viewModel.selectedRegion.value = r }
                        ) {
                            Text(
                                text = r,
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isSel) Color.White else colors.primaryText,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("City & Sub-City", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.secondaryText)
                Spacer(modifier = Modifier.height(6.dp))

                val subCities = listOf("Bole", "Kirkos", "Yeka", "Arada", "Lideta", "Nifas Silk")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    subCities.forEach { sc ->
                        val isSel = curSubCity == sc
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) SuqiPrimaryTint else colors.secondaryBackground,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) SuqiPrimary else colors.border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectedSubCity.value = sc
                                    viewModel.selectedCity.value = "Addis Ababa"
                                    viewModel.showToast("Delivery location set to Addis Ababa, $sc")
                                    showLocationSheet = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Addis Ababa, $sc",
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = if (isSel) SuqiPrimary else colors.primaryText
                                )
                                if (isSel) {
                                    Icon(LucideIcons.Check, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HelpLegalScreen(
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
                Text(text = "Help & Legal", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
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
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = LocalSuqiColors.current.primaryTint),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(LucideIcons.Headphones, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Need Help?", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        Text("Our Ethiopia support team is here to help you.", fontSize = 13.sp, color = SuqiSecondaryText)
                        Spacer(modifier = Modifier.height(14.dp))
                        SuqiButton(text = "Contact Support", onClick = { viewModel.showToast("Opening support channel...") })
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                AccountMenuItem(LucideIcons.HelpCircle, "Frequently Asked Questions") { viewModel.showToast("Opening FAQs") }
                AccountMenuItem(LucideIcons.FileText, "Terms of Service") { viewModel.showToast("Terms of Service") }
                AccountMenuItem(LucideIcons.Shield, "Privacy Policy") { viewModel.showToast("Privacy Policy") }
                AccountMenuItem(LucideIcons.Handshake, "Seller Agreement") { viewModel.showToast("Seller Agreement") }
                AccountMenuItem(LucideIcons.Info, "About Suqi (v1.0.0)") { viewModel.showToast("Suqi v1.0.0 - Made for Ethiopia") }
            }
        }
    }
}

// ==========================================
// ADMIN TOOLS: Categories & Shop Types (Plan §5.2)
// ==========================================
@Composable
fun AdminCategoriesScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Categories, 1: Shop Types
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryAmharic by remember { mutableStateOf("") }

    var categoriesList by remember {
        mutableStateOf(
            listOf(
                Triple("Electronics", "ኤሌክትሮኒክስ", 184),
                Triple("Fashion & Clothing", "የፋሽን ልብሶች", 312),
                Triple("Home & Living", "የቤት እቃዎች", 145),
                Triple("Beauty & Personal Care", "ውበትና የግል እንክብካቤ", 98),
                Triple("Furniture", "የቤት እቃዎች (ፈርኒቸር)", 64),
                Triple("Sports & Fitness", "የስፖርት እቃዎች", 72),
                Triple("Toys & Games", "የህፃናት መጫወቻዎች", 45),
                Triple("Groceries & Food", "የሸቀጣ ሸቀጥና ምግብ", 210),
                Triple("Health & Fitness", "ጤናና ደህንነት", 58)
            )
        )
    }

    val shopTypes = remember {
        listOf(
            Triple("Retail Store", "Standard merchant with physical or online shop", "2.5% Fee"),
            Triple("Official Brand Outlet", "Direct verified brand manufacturer or authorized distributor", "1.8% Fee"),
            Triple("Habesha Artisan & Crafts", "Traditional handmade Ethiopian clothing, crafts and cultural goods", "1.0% Fee"),
            Triple("Wholesale & Bulk Supplier", "B2B bulk supplier with minimum quantity orders", "1.5% Fee")
        )
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.goBack() },
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(SuqiSecondaryBackground)
                        ) {
                            Icon(LucideIcons.ArrowLeft, contentDescription = "Back", tint = SuqiPrimaryText)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "Categories & Shop Types", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                    }

                    if (selectedTab == 0) {
                        IconButton(
                            onClick = { showAddCategoryDialog = true },
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(SuqiPrimary)
                        ) {
                            Icon(LucideIcons.Plus, contentDescription = "Add", tint = Color.White)
                        }
                    }
                }

                // Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedTab == 0) SuqiPrimary else SuqiSecondaryBackground)
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Categories (${categoriesList.size})", color = if (selectedTab == 0) Color.White else SuqiPrimaryText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedTab == 1) SuqiPrimary else SuqiSecondaryBackground)
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Shop Types (${shopTypes.size})", color = if (selectedTab == 1) Color.White else SuqiPrimaryText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (selectedTab == 0) {
                items(categoriesList) { (en, am, count) ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SuqiSurface),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SuqiBorder)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(en, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(am, fontFamily = SuqiFontFamily, fontSize = 13.sp, color = SuqiPrimary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("$count active products listed", fontSize = 12.sp, color = SuqiSecondaryText)
                            }
                            SuqiStatusBadge("Active", StatusType.SUCCESS)
                        }
                    }
                }
            } else {
                items(shopTypes) { (title, desc, fee) ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SuqiSurface),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SuqiBorder)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(title, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(LocalSuqiColors.current.primaryTint)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(fee, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SuqiPrimary)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(desc, fontSize = 12.sp, color = SuqiSecondaryText)
                            }
                            SuqiStatusBadge("Approved", StatusType.SUCCESS)
                        }
                    }
                }
            }
        }
    }

    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Add New Category", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SuqiTextField(value = newCategoryName, onValueChange = { newCategoryName = it }, label = "Category Name (English)")
                    SuqiTextField(value = newCategoryAmharic, onValueChange = { newCategoryAmharic = it }, label = "Category Name (Amharic / አማርኛ)")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            categoriesList = categoriesList + Triple(newCategoryName.trim(), if (newCategoryAmharic.isBlank()) newCategoryName else newCategoryAmharic, 0)
                            viewModel.showToast("Category \"$newCategoryName\" created!")
                            showAddCategoryDialog = false
                            newCategoryName = ""
                            newCategoryAmharic = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Add Category")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ==========================================
// ADMIN TOOLS: Promotional Banners (Plan §5.2)
// ==========================================
@Composable
fun AdminBannersScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    var banners by remember {
        mutableStateOf(
            listOf(
                Triple("Ethiopian New Year Super Sale", "Up to 50% off on traditional Habesha & electronics", "https://images.unsplash.com/photo-1607082348824-0a96f2a4b9da?w=600"),
                Triple("Tech Week Addis Ababa", "Exclusive launches from certified Bole merchants", "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=600"),
                Triple("Free Nationwide Delivery", "On all orders above ETB 2,500 using Telebirr", "https://images.unsplash.com/photo-1526367790999-0150786686a2?w=600")
            )
        )
    }
    var showAddDialog by remember { mutableStateOf(false) }
    var bannerTitle by remember { mutableStateOf("") }
    var bannerSubtitle by remember { mutableStateOf("") }

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
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(SuqiSecondaryBackground)
                    ) {
                        Icon(LucideIcons.ArrowLeft, contentDescription = "Back", tint = SuqiPrimaryText)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "Promotional Banners", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                }

                IconButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(SuqiPrimary)
                ) {
                    Icon(LucideIcons.Plus, contentDescription = "Add Banner", tint = Color.White)
                }
            }
        },
        containerColor = SuqiBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(text = "Active Home Carousel Banners (${banners.size})", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            items(banners) { (title, sub, img) ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SuqiSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SuqiBorder)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                            AsyncImage(
                                model = img,
                                contentDescription = title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.35f))
                            )
                            Text(
                                text = title,
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White,
                                modifier = Modifier.align(Alignment.BottomStart).padding(14.dp)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(sub, fontSize = 12.sp, color = SuqiSecondaryText, modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = {
                                    banners = banners.filter { it.first != title }
                                    viewModel.showToast("Banner removed from carousel")
                                }
                            ) {
                                Icon(LucideIcons.Trash2, contentDescription = "Delete", tint = SuqiError)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Carousel Banner", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SuqiTextField(value = bannerTitle, onValueChange = { bannerTitle = it }, label = "Banner Title")
                    SuqiTextField(value = bannerSubtitle, onValueChange = { bannerSubtitle = it }, label = "Subtitle / Campaign Details")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (bannerTitle.isNotBlank()) {
                            banners = banners + Triple(bannerTitle.trim(), bannerSubtitle.trim(), "https://images.unsplash.com/photo-1472851294608-062f824d29cc?w=600")
                            viewModel.showToast("Banner added to homepage!")
                            showAddDialog = false
                            bannerTitle = ""
                            bannerSubtitle = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Add Banner")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ==========================================
// ADMIN TOOLS: Review & Report Moderation (Plan §5.2)
// ==========================================
@Composable
fun AdminModerationScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    var flaggedReviews by remember {
        mutableStateOf(
            listOf(
                Triple("Fitsum B.", "Fake review suspected. Competitor spamming negative feedback without purchase.", "Phone arrived damaged and seller refused to answer telegram!"),
                Triple("Anonymous", "Inappropriate language and spam link in review comments.", "Visit my website www.example.com for 50% cheaper price than this shop!")
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
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(SuqiSecondaryBackground)
                ) {
                    Icon(LucideIcons.ArrowLeft, contentDescription = "Back", tint = SuqiPrimaryText)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "Review & Report Moderation", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
            }
        },
        containerColor = SuqiBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(LucideIcons.ShieldCheck, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Flagged Content Queue (${flaggedReviews.size})", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            if (flaggedReviews.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Text("Moderation queue clean! No pending reports.", color = SuqiSecondaryText)
                    }
                }
            } else {
                items(flaggedReviews) { (author, reason, text) ->
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
                                Text("Report by: $author", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                SuqiStatusBadge("Flagged", StatusType.ERROR)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Flag reason: $reason", fontSize = 12.sp, color = SuqiError, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SuqiSecondaryBackground)
                                    .padding(10.dp)
                            ) {
                                Text("\"$text\"", fontSize = 13.sp, color = SuqiPrimaryText)
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = {
                                        flaggedReviews = flaggedReviews.filter { it.first != author }
                                        viewModel.showToast("Review report dismissed (Kept published)")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SuqiSecondaryBackground),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(40.dp)
                                ) {
                                    Text("Dismiss Report", color = SuqiPrimaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Button(
                                    onClick = {
                                        flaggedReviews = flaggedReviews.filter { it.first != author }
                                        viewModel.showToast("Review hidden & removed from platform")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SuqiError),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(40.dp)
                                ) {
                                    Text("Delete Content", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// ADMIN TOOLS: Subscription Plans Management (Plan §5.2)
// ==========================================
@Composable
fun AdminSubscriptionPlansScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    var plans by remember {
        mutableStateOf(
            listOf(
                Triple("Free Starter Tier", "ETB 0 / mo", listOf("Up to 25 Products", "Standard Search Placement", "Basic Order Messaging", "2.5% Platform Fee")),
                Triple("Pro Merchant Plan", "ETB 299 / mo", listOf("Unlimited Products", "Featured Search Placement", "Sales Analytics & Charts", "Unlimited Promo Codes", "Priority Support")),
                Triple("Enterprise Brand Plan", "ETB 799 / mo", listOf("Multi-Branch Storefront", "0% Extra Platform Fee", "Dedicated Account Manager", "Homepage Carousel Banner Spot", "Custom API Access"))
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
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(SuqiSecondaryBackground)
                ) {
                    Icon(LucideIcons.ArrowLeft, contentDescription = "Back", tint = SuqiPrimaryText)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "Subscription Plans", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
            }
        },
        containerColor = SuqiBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(text = "Monetization Tiers (${plans.size})", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Manage tier pricing and feature bundles for Ethiopian sellers", fontSize = 12.sp, color = SuqiSecondaryText)
            }

            items(plans) { (name, price, features) ->
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
                            Text(name, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SuqiPrimary)
                            SuqiStatusBadge("Active", StatusType.SUCCESS)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(price, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        for (feat in features) {
                            Text("✓ $feat", fontSize = 13.sp, color = SuqiSecondaryText, modifier = Modifier.padding(vertical = 2.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.showToast("Editing $name tier parameters") },
                            colors = ButtonDefaults.buttonColors(containerColor = SuqiSecondaryBackground),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(38.dp)
                        ) {
                            Text("Edit Tier Settings", color = SuqiPrimaryText, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
