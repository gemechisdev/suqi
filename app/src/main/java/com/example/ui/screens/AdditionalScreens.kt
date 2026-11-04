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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SuqiPrimaryText)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "Notifications", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp)
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
                        colors = CardDefaults.cardColors(containerColor = if (notif.isRead) Color.White else SuqiPrimaryLight),
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
                                        "PAYMENT" -> Icons.Outlined.Payments
                                        "CHAT" -> Icons.Outlined.ChatBubbleOutline
                                        else -> Icons.Outlined.LocalShipping
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SuqiPrimaryText)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "Saved Addresses", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = SuqiPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Address")
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
                    colors = CardDefaults.cardColors(containerColor = Color.White),
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
            containerColor = Color.White,
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SuqiPrimaryText)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "Wishlist (${products.size})", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp)
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SuqiPrimaryText)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "Reviews: $targetName", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showWriteDialog = true },
                containerColor = SuqiPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.RateReview, contentDescription = null)
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
                                    Icons.Filled.Star,
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
            containerColor = Color.White,
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SuqiPrimaryText)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "Admin Console", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp)
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

                Text(text = "Admin Quick Actions", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickActionTile(Icons.Outlined.VerifiedUser, "Verify Sellers", Modifier.weight(1f)) {
                        viewModel.navigate(ScreenDestination.AdminFeatures)
                    }
                    QuickActionTile(Icons.Outlined.RateReview, "Moderate Reviews", Modifier.weight(1f)) {
                        viewModel.showToast("Review moderation queue: 0 flagged")
                    }
                    QuickActionTile(Icons.Outlined.ViewCarousel, "Banners", Modifier.weight(1f)) {
                        viewModel.showToast("Promotional banners updated")
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                Text(text = "Platform Feature Toggles", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SuqiPrimaryText)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "Admin Seller Verification", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp)
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
                        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.primaryText)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "App Settings",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
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
                            Icon(Icons.Filled.Check, contentDescription = null, tint = SuqiPrimary)
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
                                .background(SuqiPrimaryLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(22.dp))
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
                                    true -> Icons.Filled.DarkMode
                                    false -> Icons.Filled.LightMode
                                    null -> Icons.Outlined.BrightnessAuto
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
                            Icon(Icons.Filled.Check, contentDescription = null, tint = SuqiPrimary)
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
                AccountMenuItem(Icons.Filled.AccountCircle, "Google (Linked: fitsum.gebre@gmail.com)") {}
                AccountMenuItem(Icons.AutoMirrored.Filled.Send, "Telegram (Linked: +251 912 345 678)") {}

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
                    fontWeight = FontWeight.Bold,
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
                            color = if (isSel) SuqiPrimaryLight else colors.secondaryBackground,
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
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(16.dp))
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SuqiPrimaryText)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "Help & Legal", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp)
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
                    colors = CardDefaults.cardColors(containerColor = SuqiPrimaryLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Headphones, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Need Help?", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Our Ethiopia support team is here to help you.", fontSize = 13.sp, color = SuqiSecondaryText)
                        Spacer(modifier = Modifier.height(14.dp))
                        SuqiButton(text = "Contact Support", onClick = { viewModel.showToast("Opening support channel...") })
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                AccountMenuItem(Icons.Outlined.Quiz, "Frequently Asked Questions") { viewModel.showToast("Opening FAQs") }
                AccountMenuItem(Icons.Outlined.Description, "Terms of Service") { viewModel.showToast("Terms of Service") }
                AccountMenuItem(Icons.Outlined.Shield, "Privacy Policy") { viewModel.showToast("Privacy Policy") }
                AccountMenuItem(Icons.Outlined.Handshake, "Seller Agreement") { viewModel.showToast("Seller Agreement") }
                AccountMenuItem(Icons.Outlined.Info, "About Suqi (v1.0.0)") { viewModel.showToast("Suqi v1.0.0 - Made for Ethiopia") }
            }
        }
    }
}
