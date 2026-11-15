package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.EthiopianMarketplaceUtils
import kotlinx.coroutines.launch

@Composable
fun NotificationsScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val isOffline by viewModel.isOffline.collectAsState()
    var notifications by remember { mutableStateOf<List<NotificationDto>>(emptyList()) }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var isLoading by remember { mutableStateOf(true) }

    fun loadNotifications() {
        coroutineScope.launch {
            isLoading = true
            val res = viewModel.repository.getNotifications()
            if (res.success && res.data != null) {
                notifications = res.data
            }
            viewModel.refreshUnreadNotifications()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadNotifications()
    }

    val filtered = remember(notifications, selectedFilter) {
        if (selectedFilter == "ALL") notifications
        else notifications.filter { it.type.equals(selectedFilter, ignoreCase = true) }
    }

    val filterChips = remember(notifications) {
        listOf(
            "ALL" to "All (${notifications.size})",
            "ORDER" to "Orders (${notifications.count { it.type == "ORDER" }})",
            "PAYMENT" to "Payments (${notifications.count { it.type == "PAYMENT" }})",
            "CHAT" to "Chat (${notifications.count { it.type == "CHAT" }})",
            "PROMO" to "Promo (${notifications.count { it.type == "PROMO" }})"
        )
    }

    SuqiScreen(
        title = "Notifications",
        subtitle = "${notifications.count { !it.isRead }} unread",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        headerActions = {
            if (notifications.any { !it.isRead }) {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.clearNotificationCount()
                            loadNotifications()
                            viewModel.showToast("All notifications marked read")
                        }
                    },
                    modifier = Modifier.testTag("notifications_mark_all_read")
                ) {
                    Text("Mark all read", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, color = SuqiPrimary)
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SuqiFilterChips(
                items = filterChips,
                selectedKey = selectedFilter,
                onSelect = { selectedFilter = it },
                testTagPrefix = "notif_filter"
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (isLoading) {
                SuqiSkeletonList(count = 4, modifier = Modifier.padding(top = 8.dp))
            } else if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    SuqiEmptyState(
                        icon = LucideIcons.Bell,
                        title = "You're all caught up",
                        subtitle = "Order confirmations, payment verifications, and shop updates will appear here.",
                        ctaText = if (selectedFilter != "ALL") "Show All" else null,
                        onCtaClick = if (selectedFilter != "ALL") { { selectedFilter = "ALL" } } else null,
                        testTag = "notifications_empty"
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { notif ->
                        SuqiSectionCard(
                            padding = 14.dp,
                            containerColor = if (notif.isRead) colors.cardBackground else colors.primaryTint,
                            onClick = {
                                if (!notif.isRead) {
                                    coroutineScope.launch {
                                        viewModel.repository.markNotificationRead(notif.id, true)
                                        loadNotifications()
                                    }
                                }
                            },
                            modifier = Modifier.testTag("notification_card_${notif.id}")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(SuqiPrimary.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (notif.type) {
                                            "PAYMENT" -> LucideIcons.Wallet
                                            "CHAT" -> LucideIcons.MessageCircle
                                            "PROMO" -> LucideIcons.BadgePercent
                                            else -> LucideIcons.Truck
                                        },
                                        contentDescription = null,
                                        tint = SuqiPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = notif.title,
                                            fontFamily = SuqiFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = colors.primaryText,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (!notif.isRead) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(SuqiPrimary)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = notif.body,
                                        fontFamily = SuqiFontFamily,
                                        fontSize = 13.sp,
                                        color = colors.secondaryText
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = notif.timestamp,
                                        fontFamily = SuqiFontFamily,
                                        fontSize = 11.sp,
                                        color = colors.mutedText
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.repository.deleteNotification(notif.id)
                                            loadNotifications()
                                        }
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(LucideIcons.Trash2, contentDescription = "Delete notification", tint = colors.mutedText, modifier = Modifier.size(16.dp))
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
fun SavedAddressesScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val isOffline by viewModel.isOffline.collectAsState()
    var addresses by remember { mutableStateOf<List<SavedAddressDto>>(emptyList()) }
    var editingAddress by remember { mutableStateOf<SavedAddressDto?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    fun loadAddresses() {
        coroutineScope.launch {
            isLoading = true
            val res = viewModel.repository.getSavedAddresses()
            if (res.success && res.data != null) {
                addresses = res.data
            }
            viewModel.refreshSession()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadAddresses()
    }

    SuqiScreen(
        title = "Saved Addresses",
        subtitle = "${addresses.size} delivery locations",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingAddress = null
                    showSheet = true
                },
                containerColor = SuqiPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_address_fab")
            ) {
                Icon(LucideIcons.Plus, contentDescription = "Add Address")
            }
        },
        modifier = modifier
    ) { paddingValues ->
        if (isLoading) {
            SuqiSkeletonList(count = 3, modifier = Modifier.padding(paddingValues).padding(top = 12.dp))
        } else if (addresses.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                SuqiEmptyState(
                    icon = LucideIcons.MapPin,
                    title = "No saved addresses",
                    subtitle = "Save your home or office location in Addis Ababa or regional cities for faster checkout.",
                    ctaText = "Add Address",
                    onCtaClick = {
                        editingAddress = null
                        showSheet = true
                    },
                    testTag = "addresses_empty"
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(addresses, key = { it.id }) { addr ->
                    SuqiSectionCard(padding = 16.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = addr.fullName,
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = colors.primaryText
                            )
                            if (addr.isDefault) {
                                SuqiStatusBadge("Default", StatusType.SUCCESS)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = addr.phone, fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.secondaryText)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${addr.streetOrLandmark}, ${addr.subCity}, ${addr.city}",
                            fontFamily = SuqiFontFamily,
                            fontSize = 13.sp,
                            color = colors.primaryText
                        )
                        if (addr.deliveryInstructions.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Note: ${addr.deliveryInstructions}",
                                fontFamily = SuqiFontFamily,
                                fontSize = 12.sp,
                                color = colors.mutedText
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            if (!addr.isDefault) {
                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.repository.saveAddress(addr.copy(isDefault = true))
                                            viewModel.showToast("Default address updated")
                                            loadAddresses()
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, SuqiPrimary),
                                    modifier = Modifier.weight(1f).height(44.dp)
                                ) {
                                    Text("Set Default", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SuqiPrimary)
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    editingAddress = addr
                                    showSheet = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, colors.border),
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Text("Edit", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.primaryText)
                            }
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val res = viewModel.repository.deleteSavedAddress(addr.id)
                                        if (res.success) {
                                            viewModel.showToast("Address removed")
                                            loadAddresses()
                                        } else {
                                            viewModel.showToast(res.message ?: "Cannot remove address")
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, SuqiError.copy(alpha = 0.4f)),
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Text("Delete", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SuqiError)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSheet) {
        val currentUser by viewModel.currentUser.collectAsState()
        var name by remember(editingAddress) { mutableStateOf(editingAddress?.fullName ?: currentUser?.name.orEmpty()) }
        var phone by remember(editingAddress) { mutableStateOf(editingAddress?.phone ?: currentUser?.phone?.ifBlank { "+251 " } ?: "+251 ") }
        var address by remember(editingAddress) { mutableStateOf(editingAddress?.streetOrLandmark.orEmpty()) }
        var subCity by remember(editingAddress) { mutableStateOf(editingAddress?.subCity ?: "Bole") }
        var city by remember(editingAddress) { mutableStateOf(editingAddress?.city ?: "Addis Ababa") }
        var notes by remember(editingAddress) { mutableStateOf(editingAddress?.deliveryInstructions.orEmpty()) }
        var isDefault by remember(editingAddress) { mutableStateOf(editingAddress?.isDefault ?: addresses.isEmpty()) }

        SuqiBottomSheet(
            onDismissRequest = { showSheet = false },
            title = if (editingAddress == null) "Add Delivery Address" else "Edit Address"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SuqiTextField(value = name, onValueChange = { name = it }, label = "Recipient Name *")
                SuqiTextField(value = phone, onValueChange = { phone = it }, label = "Phone (+251) *")
                SuqiTextField(value = address, onValueChange = { address = it }, label = "Street / Landmark *")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SuqiTextField(value = subCity, onValueChange = { subCity = it }, label = "Sub-City", modifier = Modifier.weight(1f))
                    SuqiTextField(value = city, onValueChange = { city = it }, label = "City", modifier = Modifier.weight(1f))
                }
                SuqiTextField(value = notes, onValueChange = { notes = it }, label = "Delivery Instructions")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Set as default delivery address", fontFamily = SuqiFontFamily, fontSize = 14.sp, color = colors.primaryText)
                    Switch(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = SuqiPrimary)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                SuqiButton(
                    text = if (editingAddress == null) "Save Address" else "Update Address",
                    onClick = {
                        if (name.isBlank() || address.isBlank()) {
                            viewModel.showToast("Enter recipient name and street/landmark")
                            return@SuqiButton
                        }
                        coroutineScope.launch {
                            viewModel.repository.saveAddress(
                                SavedAddressDto(
                                    id = editingAddress?.id.orEmpty(),
                                    fullName = name.trim(),
                                    phone = phone.trim(),
                                    region = city.trim(),
                                    city = city.trim(),
                                    subCity = subCity.trim(),
                                    streetOrLandmark = address.trim(),
                                    deliveryInstructions = notes.trim(),
                                    isDefault = isDefault
                                )
                            )
                            showSheet = false
                            loadAddresses()
                            viewModel.showToast("Address saved")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun WishlistScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val isOffline by viewModel.isOffline.collectAsState()
    var products by remember { mutableStateOf<List<ProductDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun loadWishlist() {
        coroutineScope.launch {
            isLoading = true
            val res = viewModel.repository.getProducts()
            if (res.success && res.data != null) {
                products = res.data.filter { it.isWishlisted }
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadWishlist()
    }

    SuqiScreen(
        title = "Wishlist",
        subtitle = "${products.size} saved items",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        if (isLoading) {
            SuqiSkeletonProductGrid(count = 4, modifier = Modifier.padding(paddingValues).padding(top = 12.dp))
        } else if (products.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                SuqiEmptyState(
                    icon = LucideIcons.Heart,
                    title = "Your wishlist is empty",
                    subtitle = "Tap the heart icon on any product to save it here for quick comparison and checkout.",
                    ctaText = "Explore Marketplace",
                    onCtaClick = { viewModel.popToRoot(ScreenDestination.Home) },
                    testTag = "wishlist_empty"
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val chunks = products.chunked(2)
                items(chunks) { chunk ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (prod in chunk) {
                            Box(modifier = Modifier.weight(1f)) {
                                SuqiProductCard(
                                    product = prod,
                                    onClick = { viewModel.navigate(ScreenDestination.ProductDetails(prod.id)) },
                                    onAddToCart = { viewModel.quickAddToCart(prod) },
                                    onShopClick = { viewModel.navigate(ScreenDestination.ShopStorefront(prod.shopId)) },
                                    onToggleWishlist = {
                                        coroutineScope.launch {
                                            viewModel.repository.toggleWishlist(prod.id)
                                            loadWishlist()
                                        }
                                    }
                                )
                            }
                        }
                        if (chunk.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
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
    val colors = LocalSuqiColors.current
    val isOffline by viewModel.isOffline.collectAsState()
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

    SuqiScreen(
        title = "Reviews",
        subtitle = "$targetName (${reviews.size})",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.requireSignedInOrPrompt("write a review") {
                        showWriteDialog = true
                    }
                },
                containerColor = SuqiPrimary,
                contentColor = Color.White
            ) {
                Icon(LucideIcons.MessageSquarePlus, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Write Review")
            }
        },
        modifier = modifier
    ) { paddingValues ->
        if (reviews.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                SuqiEmptyState(
                    icon = LucideIcons.Star,
                    title = "No reviews yet",
                    subtitle = "Be the first customer to review $targetName.",
                    ctaText = "Write a Review",
                    onCtaClick = {
                        viewModel.requireSignedInOrPrompt("write a review") {
                            showWriteDialog = true
                        }
                    }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(reviews, key = { it.id }) { rev ->
                    Column {
                        ReviewItemCard(review = rev)
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                        ) {
                            Text(
                                text = "Report review",
                                fontFamily = SuqiFontFamily,
                                fontSize = 11.sp,
                                color = colors.mutedText,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        coroutineScope.launch {
                                            viewModel.repository.reportOrModerateReview(
                                                mapOf(
                                                    "action" to "REPORT",
                                                    "authorName" to rev.userName,
                                                    "reason" to "Reported from Reviews page",
                                                    "reviewText" to rev.comment
                                                )
                                            )
                                            viewModel.showToast("Review reported for moderation")
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showWriteDialog) {
        SuqiBottomSheet(
            onDismissRequest = { showWriteDialog = false },
            title = "Write a Review"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select Rating", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, color = colors.primaryText)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (i in 1..5) {
                        IconButton(onClick = { rating = i.toDouble() }, modifier = Modifier.size(44.dp)) {
                            Icon(
                                LucideIcons.StarFilled,
                                contentDescription = "$i stars",
                                tint = if (i <= rating) SuqiWarning else colors.border,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
                SuqiTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = "Your Review",
                    singleLine = false
                )
                SuqiButton(
                    text = "Submit Review",
                    onClick = {
                        if (comment.isBlank()) {
                            viewModel.showToast("Please write a short comment")
                            return@SuqiButton
                        }
                        coroutineScope.launch {
                            viewModel.repository.submitReview(
                                mapOf(
                                    "targetId" to targetId,
                                    "targetType" to targetType,
                                    "rating" to rating,
                                    "comment" to comment.trim()
                                )
                            )
                            comment = ""
                            showWriteDialog = false
                            loadReviews()
                            viewModel.showToast("Thank you for your review!")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun AppSettingsScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val currentUser by viewModel.currentUser.collectAsState()
    val isDarkPref by viewModel.isDarkMode.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    val isDevUnlocked by viewModel.isDeveloperUnlocked.collectAsState()

    val curRegion by viewModel.selectedRegion.collectAsState()
    val curCity by viewModel.selectedCity.collectAsState()
    val curSubCity by viewModel.selectedSubCity.collectAsState()
    val curLang by viewModel.selectedLanguage.collectAsState()

    var notificationsEnabled by remember { mutableStateOf(true) }
    var showLocationSheet by remember { mutableStateOf(false) }
    var versionTapCount by remember { mutableIntStateOf(0) }

    SuqiScreen(
        title = "App Settings",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SuqiSectionCard(padding = 16.dp) {
                    Text("Language & Region", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.primaryText)
                    Spacer(modifier = Modifier.height(10.dp))

                    val langs = listOf(
                        "English" to "English",
                        "Amharic" to "Amharic (አማርኛ)",
                        "Afaan Oromo" to "Afaan Oromo"
                    )
                    langs.forEach { (key, label) ->
                        val isSel = curLang.equals(key, ignoreCase = true) || curLang.startsWith(key.take(5), ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.selectedLanguage.value = key
                                    coroutineScope.launch {
                                        viewModel.repository.updateUserProfile(mapOf("preferredLanguage" to key))
                                        viewModel.showToast("Language set to $key")
                                    }
                                }
                                .padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, fontFamily = SuqiFontFamily, fontSize = 14.sp, color = colors.primaryText)
                            if (isSel) {
                                Icon(LucideIcons.Check, contentDescription = "Selected", tint = SuqiPrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = 10.dp))

                    SuqiListRow(
                        icon = LucideIcons.MapPin,
                        title = "$curCity, $curSubCity",
                        subtitle = "Region: $curRegion",
                        badge = "Change",
                        onClick = { showLocationSheet = true },
                        testTag = "settings_location_row"
                    )
                }
            }

            item {
                SuqiSectionCard(padding = 16.dp) {
                    Text("Appearance & Theme", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.primaryText)
                    Spacer(modifier = Modifier.height(8.dp))

                    val themeOptions = listOf(
                        "System Default" to null,
                        "Light Mode (Warm Cream)" to false,
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
                                .padding(vertical = 10.dp, horizontal = 6.dp),
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
                                Icon(LucideIcons.Check, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = 10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Order & Payment Alerts", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.primaryText)
                            Text("Receive updates when sellers verify payments", fontSize = 12.sp, color = colors.secondaryText)
                        }
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { notificationsEnabled = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = SuqiPrimary)
                        )
                    }
                }
            }

            item {
                SuqiSectionCard(padding = 16.dp) {
                    Text("Account & Staff Tools", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.primaryText)
                    Spacer(modifier = Modifier.height(8.dp))

                    SuqiListRow(
                        icon = LucideIcons.CircleUser,
                        title = "Account Email",
                        subtitle = currentUser?.email?.ifBlank { "Not linked" } ?: "Guest session",
                        showChevron = false,
                        onClick = { viewModel.navigate(ScreenDestination.ProfileEdit) }
                    )
                    SuqiListRow(
                        icon = LucideIcons.Phone,
                        title = "Phone Number",
                        subtitle = currentUser?.phone?.ifBlank { "Not linked" } ?: "Not linked",
                        showChevron = false,
                        onClick = { viewModel.navigate(ScreenDestination.ProfileEdit) }
                    )
                    HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = 6.dp))

                    SuqiListRow(
                        icon = LucideIcons.ShieldCheck,
                        title = "Admin / Staff Sign In",
                        subtitle = "Owner, Admin & Developer console access",
                        onClick = { viewModel.navigate(ScreenDestination.AdminSignIn) },
                        testTag = "settings_admin_signin_row"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(colors.warningBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(LucideIcons.WifiOff, contentDescription = null, tint = SuqiWarning, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Offline Mode Simulator", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.primaryText)
                                Text("Preview offline cached banner across screens", fontSize = 12.sp, color = colors.secondaryText)
                            }
                        }
                        Switch(
                            checked = isOffline,
                            onCheckedChange = { viewModel.setOfflineMode(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = SuqiPrimary),
                            modifier = Modifier.testTag("settings_offline_switch")
                        )
                    }

                    if (isDevUnlocked) {
                        SuqiListRow(
                            icon = LucideIcons.Wrench,
                            title = "Developer Diagnostics",
                            subtitle = "Inspect database counts, session & tools",
                            badge = "Unlocked",
                            onClick = { viewModel.navigate(ScreenDestination.AdminDevTools) },
                            testTag = "settings_dev_tools_row"
                        )
                    }

                    HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                versionTapCount++
                                if (versionTapCount >= 7 && !isDevUnlocked) {
                                    viewModel.unlockDeveloperTools()
                                    viewModel.showToast("Developer Tools unlocked!")
                                } else if (!isDevUnlocked && versionTapCount >= 4) {
                                    viewModel.showToast("${7 - versionTapCount} taps remaining for Developer Mode")
                                }
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                            .testTag("settings_version_pill"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Suqi App Version", fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.secondaryText)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(colors.secondaryBackground)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isDevUnlocked) "v1.0.0 (Dev)" else "v1.0.0",
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = if (isDevUnlocked) SuqiPrimary else colors.primaryText
                            )
                        }
                    }
                }
            }

            item {
                SuqiButton(
                    text = "Sign Out",
                    isPrimary = false,
                    icon = LucideIcons.LogOut,
                    onClick = {
                        coroutineScope.launch {
                            viewModel.repository.loginAsGuest()
                            viewModel.refreshSession()
                            viewModel.showToast("Signed out")
                            viewModel.popToRoot(ScreenDestination.Auth)
                        }
                    },
                    testTag = "settings_sign_out_button",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showLocationSheet) {
        SuqiBottomSheet(
            onDismissRequest = { showLocationSheet = false },
            title = "Select Delivery Location"
        ) {
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

            Spacer(modifier = Modifier.height(14.dp))
            Text("City & Sub-City", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.secondaryText)
            Spacer(modifier = Modifier.height(6.dp))

            val subCities = listOf("Bole", "Kirkos", "Yeka", "Arada", "Lideta", "Nifas Silk")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                subCities.forEach { sc ->
                    val isSel = curSubCity == sc
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSel) colors.primaryTint else colors.secondaryBackground,
                        border = BorderStroke(1.dp, if (isSel) SuqiPrimary else colors.border),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selectedSubCity.value = sc
                                viewModel.selectedCity.value = "Addis Ababa"
                                coroutineScope.launch {
                                    viewModel.repository.updateUserProfile(
                                        mapOf(
                                            "region" to curRegion,
                                            "city" to "Addis Ababa",
                                            "subCity" to sc
                                        )
                                    )
                                }
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

@Composable
fun HelpLegalScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val isOffline by viewModel.isOffline.collectAsState()
    var activeDocument by remember { mutableStateOf<EthiopianMarketplaceUtils.LegalDocument?>(null) }
    var showFaqDialog by remember { mutableStateOf(false) }
    var showTicketSheet by remember { mutableStateOf(false) }
    var tickets by remember { mutableStateOf<List<SupportTicketDto>>(emptyList()) }

    fun loadTickets() {
        coroutineScope.launch {
            val res = viewModel.repository.getSupportTickets()
            if (res.success && res.data != null) {
                tickets = res.data
            }
        }
    }

    LaunchedEffect(Unit) {
        loadTickets()
    }

    SuqiScreen(
        title = "Help & Legal",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SuqiSectionCard(containerColor = colors.primaryTint, padding = 20.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Icon(LucideIcons.Headphones, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Need Help?", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.primaryText)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Our Addis Ababa support team assists buyers and merchants 7 days a week.",
                            fontFamily = SuqiFontFamily,
                            fontSize = 13.sp,
                            color = colors.secondaryText,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            SuqiButton(
                                text = "Submit Ticket",
                                icon = LucideIcons.MessageSquarePlus,
                                onClick = { showTicketSheet = true },
                                testTag = "help_submit_ticket_button",
                                modifier = Modifier.weight(1f)
                            )
                            SuqiButton(
                                text = "Call Support",
                                isPrimary = false,
                                icon = LucideIcons.Phone,
                                onClick = { viewModel.showToast("Support line: +251 912 345 678") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            if (tickets.isNotEmpty()) {
                item {
                    SuqiSectionCard(padding = 16.dp) {
                        Text(
                            text = "Your Support Tickets (${tickets.size})",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = colors.primaryText
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        tickets.forEachIndexed { idx, t ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(t.subject, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.primaryText)
                                    SuqiStatusBadge(t.status, if (t.status == "OPEN") StatusType.WARNING else StatusType.SUCCESS)
                                }
                                Text("${t.category} • ${t.createdAt}", fontSize = 11.sp, color = colors.mutedText)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(t.description, fontSize = 12.sp, color = colors.secondaryText)
                            }
                            if (idx < tickets.lastIndex) {
                                HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = 10.dp))
                            }
                        }
                    }
                }
            }

            item {
                SuqiSectionCard(padding = 14.dp) {
                    SuqiListRow(
                        icon = LucideIcons.HelpCircle,
                        title = "Frequently Asked Questions",
                        subtitle = "Orders, Telebirr, & returns",
                        onClick = { showFaqDialog = true }
                    )
                    SuqiListRow(
                        icon = LucideIcons.FileText,
                        title = "Terms of Service",
                        subtitle = "Ethiopian Proclamation 1205/2020",
                        onClick = { activeDocument = EthiopianMarketplaceUtils.TERMS_OF_SERVICE }
                    )
                    SuqiListRow(
                        icon = LucideIcons.Shield,
                        title = "Privacy Policy",
                        subtitle = "Data protection & security",
                        onClick = { activeDocument = EthiopianMarketplaceUtils.PRIVACY_POLICY }
                    )
                    SuqiListRow(
                        icon = LucideIcons.Handshake,
                        title = "Vendor Marketplace Agreement",
                        subtitle = "Merchant payout & fulfillment rules",
                        onClick = { activeDocument = EthiopianMarketplaceUtils.SELLER_AGREEMENT }
                    )
                    SuqiListRow(
                        icon = LucideIcons.Info,
                        title = "About Suqi",
                        subtitle = "v1.0.0 — Ethiopian Multi-Vendor Marketplace",
                        showChevron = false,
                        onClick = { viewModel.showToast("Suqi v1.0.0") }
                    )
                }
            }
        }
    }

    if (showTicketSheet) {
        var category by remember { mutableStateOf("Order Issue") }
        var subject by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }

        SuqiBottomSheet(
            onDismissRequest = { showTicketSheet = false },
            title = "Submit Support Ticket"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Topic", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.secondaryText)
                val cats = listOf("Order Issue", "Payment", "Delivery", "Account")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    cats.forEach { cat ->
                        val sel = cat == category
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (sel) SuqiPrimary else colors.secondaryBackground)
                                .clickable { category = cat }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(cat, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (sel) Color.White else colors.primaryText)
                        }
                    }
                }
                SuqiTextField(value = subject, onValueChange = { subject = it }, label = "Subject *")
                SuqiTextField(value = description, onValueChange = { description = it }, label = "Details *", singleLine = false)
                SuqiButton(
                    text = "Send Ticket",
                    onClick = {
                        if (subject.isBlank() || description.isBlank()) {
                            viewModel.showToast("Please complete subject and details")
                            return@SuqiButton
                        }
                        coroutineScope.launch {
                            viewModel.repository.submitSupportTicket(
                                SupportTicketDto(
                                    id = "",
                                    userId = "",
                                    category = category,
                                    subject = subject.trim(),
                                    description = description.trim(),
                                    status = "OPEN",
                                    createdAt = ""
                                )
                            )
                            showTicketSheet = false
                            loadTickets()
                            viewModel.showToast("Support ticket submitted!")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    activeDocument?.let { doc ->
        AlertDialog(
            onDismissRequest = { activeDocument = null },
            title = {
                Column {
                    Text(text = doc.title, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.primaryText)
                    Text(text = doc.subtitle, fontSize = 12.sp, color = colors.secondaryText)
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(doc.sections) { sec ->
                        Column {
                            Text(text = sec.heading, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = SuqiPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = sec.content, fontFamily = SuqiFontFamily, fontSize = 13.sp, lineHeight = 18.sp, color = colors.primaryText)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeDocument = null }) {
                    Text("Close", color = SuqiPrimary, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = colors.cardBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showFaqDialog) {
        AlertDialog(
            onDismissRequest = { showFaqDialog = false },
            title = {
                Text("Frequently Asked Questions", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.primaryText)
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(EthiopianMarketplaceUtils.FAQS) { (q, a) ->
                        SuqiSectionCard(containerColor = colors.secondaryBackground, padding = 12.dp) {
                            Text(text = "Q: $q", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = SuqiPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = a, fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.primaryText, lineHeight = 17.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFaqDialog = false }) {
                    Text("Close", color = SuqiPrimary, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = colors.cardBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

// ==========================================
// A-01 (P0): Admin / Staff Sign In
// ==========================================
@Composable
fun AdminSignInScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val isOffline by viewModel.isOffline.collectAsState()

    var selectedTier by remember { mutableStateOf("OWNER") }
    var identifier by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var showPin by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val tiers = listOf("OWNER" to "Owner", "ADMIN" to "Admin", "DEVELOPER" to "Developer")

    SuqiScreen(
        title = "Staff & Admin Sign In",
        subtitle = "Restricted platform access",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
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
                SuqiSectionCard(padding = 20.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(colors.primaryTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(LucideIcons.ShieldCheck, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(34.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Suqi Governance Portal",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = colors.primaryText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Authenticate as an Owner, Platform Admin, or Developer to manage categories, banners, verification queues, and diagnostics.",
                            fontFamily = SuqiFontFamily,
                            fontSize = 13.sp,
                            color = colors.secondaryText,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Text("Staff Tier", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.secondaryText)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        tiers.forEach { (key, label) ->
                            val selected = selectedTier == key
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (selected) SuqiPrimary else colors.secondaryBackground)
                                    .border(1.dp, if (selected) SuqiPrimary else colors.border, RoundedCornerShape(14.dp))
                                    .clickable { selectedTier = key }
                                    .testTag("admin_tier_$key")
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

                    Spacer(modifier = Modifier.height(16.dp))

                    SuqiTextField(
                        value = identifier,
                        onValueChange = {
                            identifier = it
                            errorMsg = null
                        },
                        label = "Staff Email or ID *",
                        placeholder = "admin@suqi.et",
                        leadingIcon = LucideIcons.CircleUser,
                        testTag = "admin_identifier_input"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Staff PIN *",
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = colors.primaryText,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = pin,
                        onValueChange = {
                            pin = it
                            errorMsg = null
                        },
                        placeholder = { Text("Enter 4-6 digit PIN", color = colors.mutedText) },
                        leadingIcon = { Icon(LucideIcons.Lock, contentDescription = null, tint = colors.secondaryText) },
                        trailingIcon = {
                            IconButton(onClick = { showPin = !showPin }) {
                                Icon(
                                    imageVector = if (showPin) LucideIcons.EyeOff else LucideIcons.Eye,
                                    contentDescription = if (showPin) "Hide PIN" else "Show PIN",
                                    tint = colors.secondaryText
                                )
                            }
                        },
                        visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.secondaryBackground,
                            unfocusedContainerColor = colors.secondaryBackground,
                            focusedBorderColor = SuqiPrimary,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.primaryText,
                            unfocusedTextColor = colors.primaryText
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_pin_input")
                    )

                    if (!errorMsg.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(errorMsg!!, fontFamily = SuqiFontFamily, fontSize = 12.sp, color = SuqiError)
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    SuqiButton(
                        text = if (isSubmitting) "Verifying..." else "Sign In to Admin Console",
                        enabled = !isSubmitting,
                        onClick = {
                            if (identifier.isBlank() || pin.length < 4) {
                                errorMsg = "Enter your staff identifier and PIN (min 4 digits)."
                                return@SuqiButton
                            }
                            isSubmitting = true
                            coroutineScope.launch {
                                val res = viewModel.repository.adminSignIn(identifier.trim(), pin.trim(), selectedTier)
                                isSubmitting = false
                                if (res.success) {
                                    if (selectedTier == "DEVELOPER" || selectedTier == "OWNER") {
                                        viewModel.unlockDeveloperTools()
                                    }
                                    viewModel.refreshSession()
                                    viewModel.showToast("Signed in as $selectedTier")
                                    viewModel.popToRoot(ScreenDestination.AdminConsole)
                                } else {
                                    errorMsg = res.message ?: "Invalid staff credentials"
                                }
                            }
                        },
                        testTag = "admin_sign_in_submit",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                SuqiSectionCard(containerColor = colors.secondaryBackground, padding = 14.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Staff Access Reference", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.primaryText)
                            Text("ID: admin@suqi.et  •  PIN: 123456", fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.secondaryText)
                        }
                        TextButton(
                            onClick = {
                                identifier = "admin@suqi.et"
                                pin = "123456"
                                errorMsg = null
                            }
                        ) {
                            Text("Fill", fontWeight = FontWeight.Bold, color = SuqiPrimary)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// Admin Console (F-02 Segmented Tabs + Real DB Counts)
// ==========================================
@Composable
fun AdminConsoleScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val isOffline by viewModel.isOffline.collectAsState()
    val adminTier by viewModel.adminTier.collectAsState()
    val isDevUnlocked by viewModel.isDeveloperUnlocked.collectAsState()

    var selectedTab by remember { mutableStateOf("overview") }
    var adminSettings by remember { mutableStateOf(AdminSettingsDto()) }
    var shopsCount by remember { mutableIntStateOf(0) }
    var productsCount by remember { mutableIntStateOf(0) }
    var ordersCount by remember { mutableIntStateOf(0) }
    var pendingVerificationsCount by remember { mutableIntStateOf(0) }
    var flaggedReviewsCount by remember { mutableIntStateOf(0) }
    var tickets by remember { mutableStateOf<List<SupportTicketDto>>(emptyList()) }

    fun loadAdminData() {
        coroutineScope.launch {
            viewModel.repository.getAdminSettings().data?.let { adminSettings = it }
            viewModel.repository.getShops().data?.let { shopsCount = it.size }
            viewModel.repository.getProducts().data?.let { productsCount = it.size }
            viewModel.repository.getOrders().data?.let { ordersCount = it.size }
            viewModel.repository.getAdminSellerVerificationQueue().data?.let { pendingVerificationsCount = it.size }
            viewModel.repository.getFlaggedReviews().data?.let { flaggedReviewsCount = it.count { r -> r.status == "REPORTED" } }
            viewModel.repository.getSupportTickets().data?.let { tickets = it }
        }
    }

    LaunchedEffect(Unit) {
        loadAdminData()
    }

    SuqiScreen(
        title = "Admin Console",
        subtitle = "Tier: ${adminTier.ifBlank { "OWNER" }}",
        onBackClick = { viewModel.popToRoot(ScreenDestination.Home) },
        isOffline = isOffline,
        headerActions = {
            IconButton(
                onClick = { loadAdminData() },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.secondaryBackground)
            ) {
                Icon(LucideIcons.RefreshCw, contentDescription = "Refresh stats", tint = colors.primaryText, modifier = Modifier.size(18.dp))
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SuqiAdminSegmentedTabs(
                selectedTab = selectedTab,
                onSelectTab = { selectedTab = it },
                isDeveloperVisible = isDevUnlocked || adminTier == "DEVELOPER" || adminTier == "OWNER"
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (selectedTab) {
                    "overview" -> {
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                SuqiStatCard(
                                    label = "Active Shops",
                                    value = shopsCount.toString(),
                                    icon = LucideIcons.Store,
                                    tint = SuqiPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                SuqiStatCard(
                                    label = "Live Products",
                                    value = productsCount.toString(),
                                    icon = LucideIcons.Package,
                                    tint = SuqiSuccess,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                SuqiStatCard(
                                    label = "Orders Logged",
                                    value = ordersCount.toString(),
                                    icon = LucideIcons.Receipt,
                                    tint = SuqiPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                SuqiStatCard(
                                    label = "Pending Reviews",
                                    value = (pendingVerificationsCount + flaggedReviewsCount).toString(),
                                    icon = LucideIcons.ShieldCheck,
                                    tint = SuqiWarning,
                                    onClick = { selectedTab = "queues" },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        item {
                            SuqiSectionCard(padding = 16.dp) {
                                Text(
                                    text = "Platform Feature Toggles",
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = colors.primaryText
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                FeatureToggleRow(
                                    title = "In-App Chat Service",
                                    sub = "Direct buyer-to-seller messaging",
                                    checked = adminSettings.isChatEnabled,
                                    onToggle = {
                                        coroutineScope.launch {
                                            viewModel.repository.updateFeatureToggle("chat", it)
                                            adminSettings = adminSettings.copy(isChatEnabled = it)
                                        }
                                    }
                                )
                                HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = 12.dp))
                                FeatureToggleRow(
                                    title = "External Contact Buttons",
                                    sub = "Show Call, Telegram and WhatsApp links",
                                    checked = adminSettings.isExternalContactEnabled,
                                    onToggle = {
                                        coroutineScope.launch {
                                            viewModel.repository.updateFeatureToggle("contact", it)
                                            adminSettings = adminSettings.copy(isExternalContactEnabled = it)
                                        }
                                    }
                                )
                                HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = 12.dp))
                                FeatureToggleRow(
                                    title = "Subscription Monetization",
                                    sub = "Toggle Free Pilot vs Paid Seller Plans",
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

                    "queues" -> {
                        item {
                            SuqiSectionCard(padding = 14.dp) {
                                Text("Moderation & Approval Queues", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                                Spacer(modifier = Modifier.height(8.dp))
                                SuqiListRow(
                                    icon = LucideIcons.BadgeCheck,
                                    title = "Seller Verification Queue",
                                    subtitle = "Review merchant trade licenses & IDs",
                                    badge = "$pendingVerificationsCount pending",
                                    badgeColor = if (pendingVerificationsCount > 0) SuqiWarning else SuqiSuccess,
                                    onClick = { viewModel.navigate(ScreenDestination.AdminFeatures) },
                                    testTag = "admin_queue_verifications"
                                )
                                SuqiListRow(
                                    icon = LucideIcons.Flag,
                                    title = "Review Moderation Queue",
                                    subtitle = "Dismiss or delete flagged buyer reviews",
                                    badge = "$flaggedReviewsCount flagged",
                                    badgeColor = if (flaggedReviewsCount > 0) SuqiError else SuqiSuccess,
                                    onClick = { viewModel.navigate(ScreenDestination.AdminModeration) },
                                    testTag = "admin_queue_moderation"
                                )
                            }
                        }
                        item {
                            SuqiSectionCard(padding = 16.dp) {
                                Text("Customer & Seller Support Tickets (${tickets.size})", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                                Spacer(modifier = Modifier.height(10.dp))
                                if (tickets.isEmpty()) {
                                    Text("No open support tickets.", fontSize = 13.sp, color = colors.secondaryText)
                                } else {
                                    tickets.forEachIndexed { idx, ticket ->
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(ticket.subject, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.primaryText)
                                                SuqiStatusBadge(ticket.status, StatusType.WARNING)
                                            }
                                            Text("${ticket.category} · ${ticket.createdAt}", fontSize = 11.sp, color = colors.mutedText)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(ticket.description, fontSize = 12.sp, color = colors.secondaryText)
                                        }
                                        if (idx < tickets.lastIndex) {
                                            HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = 10.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "manage" -> {
                        item {
                            SuqiSectionCard(padding = 14.dp) {
                                Text("Catalog, Merchandising & Plans", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                                Spacer(modifier = Modifier.height(8.dp))
                                SuqiListRow(
                                    icon = LucideIcons.Tag,
                                    title = "Categories & Shop Types",
                                    subtitle = "Add, edit, or delete marketplace categories",
                                    onClick = { viewModel.navigate(ScreenDestination.AdminCategories) },
                                    testTag = "admin_manage_categories"
                                )
                                SuqiListRow(
                                    icon = LucideIcons.GalleryHorizontal,
                                    title = "Home Promotional Banners",
                                    subtitle = "Control live campaigns shown in Home carousel",
                                    onClick = { viewModel.navigate(ScreenDestination.AdminBanners) },
                                    testTag = "admin_manage_banners"
                                )
                                SuqiListRow(
                                    icon = LucideIcons.Award,
                                    title = "Subscription Plans",
                                    subtitle = "Edit pricing, quotas, and tier features",
                                    onClick = { viewModel.navigate(ScreenDestination.AdminSubscriptionPlans) },
                                    testTag = "admin_manage_plans"
                                )
                            }
                        }
                    }

                    "tools" -> {
                        item {
                            SuqiSectionCard(padding = 14.dp) {
                                Text("Developer & Testing Tools", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                                Spacer(modifier = Modifier.height(8.dp))
                                SuqiListRow(
                                    icon = LucideIcons.Wrench,
                                    title = "Open Diagnostics Inspector",
                                    subtitle = "Inspect Room table counts, switch roles, and resync state",
                                    onClick = { viewModel.navigate(ScreenDestination.AdminDevTools) },
                                    testTag = "admin_tools_devtools"
                                )
                                SuqiListRow(
                                    icon = LucideIcons.User,
                                    title = "Return to Buyer Storefront",
                                    subtitle = "Switch active role to CUSTOMER",
                                    onClick = {
                                        viewModel.setActiveRole("CUSTOMER")
                                        viewModel.popToRoot(ScreenDestination.Home)
                                    }
                                )
                            }
                        }
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
    val colors = LocalSuqiColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.primaryText)
            Text(text = sub, fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.secondaryText)
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
    val colors = LocalSuqiColors.current
    val isOffline by viewModel.isOffline.collectAsState()
    var pendingShops by remember { mutableStateOf<List<ShopDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun loadShops() {
        coroutineScope.launch {
            isLoading = true
            val res = viewModel.repository.getAdminSellerVerificationQueue()
            if (res.success && res.data != null) {
                pendingShops = res.data
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadShops()
    }

    SuqiScreen(
        title = "Seller Verification Queue",
        subtitle = "${pendingShops.size} pending reviews",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        if (isLoading) {
            SuqiSkeletonList(count = 3, modifier = Modifier.padding(paddingValues).padding(top = 12.dp))
        } else if (pendingShops.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                SuqiEmptyState(
                    icon = LucideIcons.BadgeCheck,
                    title = "Verification queue clear",
                    subtitle = "All submitted Ethiopian merchant verification requests have been reviewed."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(pendingShops, key = { it.id }) { shop ->
                    SuqiSectionCard(padding = 16.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = shop.logoUrl,
                                contentDescription = shop.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.imageBackdrop)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = shop.name, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                                Text(text = "${shop.category} • ${shop.subCity}, ${shop.city}", fontSize = 12.sp, color = colors.secondaryText)
                            }
                            SuqiStatusBadge("Pending", StatusType.WARNING)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        viewModel.repository.adminVerifySellerDecision(shop.id, "APPROVE")
                                        viewModel.showToast("${shop.name} verified!")
                                        loadShops()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SuqiSuccess),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) { Text("Approve", color = Color.White, fontWeight = FontWeight.Bold) }

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        viewModel.repository.adminVerifySellerDecision(shop.id, "REJECT")
                                        viewModel.showToast("${shop.name} rejected.")
                                        loadShops()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SuqiError),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) { Text("Reject", color = Color.White, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// ADMIN TOOLS: Persisted Categories & Shop Types
// ==========================================
@Composable
fun AdminCategoriesScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val isOffline by viewModel.isOffline.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var categories by remember { mutableStateOf<List<CategoryDto>>(emptyList()) }
    var editingCategory by remember { mutableStateOf<CategoryDto?>(null) }
    var showCategorySheet by remember { mutableStateOf(false) }

    fun loadCategories() {
        coroutineScope.launch {
            val res = viewModel.repository.getCategories()
            if (res.success && res.data != null) {
                categories = res.data
            }
        }
    }

    LaunchedEffect(Unit) {
        loadCategories()
    }

    val shopTypes = remember {
        listOf(
            Triple("Retail Store", "Standard merchant with physical or online shop", "2.5% Fee"),
            Triple("Official Brand Outlet", "Verified brand manufacturer or authorized distributor", "1.8% Fee"),
            Triple("Habesha Artisan & Crafts", "Traditional handmade Ethiopian clothing & crafts", "1.0% Fee"),
            Triple("Wholesale & Bulk Supplier", "B2B supplier with minimum quantity orders", "1.5% Fee")
        )
    }

    SuqiScreen(
        title = "Categories & Shop Types",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        headerActions = {
            if (selectedTab == 0) {
                IconButton(
                    onClick = {
                        editingCategory = null
                        showCategorySheet = true
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SuqiPrimary)
                        .testTag("admin_add_category_btn")
                ) {
                    Icon(LucideIcons.Plus, contentDescription = "Add category", tint = Color.White)
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selectedTab == 0) SuqiPrimary else colors.secondaryBackground)
                        .clickable { selectedTab = 0 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Categories (${categories.size})",
                        color = if (selectedTab == 0) Color.White else colors.primaryText,
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selectedTab == 1) SuqiPrimary else colors.secondaryBackground)
                        .clickable { selectedTab = 1 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Shop Types (${shopTypes.size})",
                        color = if (selectedTab == 1) Color.White else colors.primaryText,
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (selectedTab == 0) {
                    items(categories, key = { it.id }) { cat ->
                        SuqiSectionCard(padding = 14.dp) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val thumbUrl = cat.imageUrl.ifBlank { MarketplaceCategories.imageUrlForId(cat.id) }
                                if (thumbUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = thumbUrl,
                                        contentDescription = cat.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(colors.imageBackdrop)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(cat.name, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                                    if (cat.nameAmharic.isNotBlank()) {
                                        Text(cat.nameAmharic, fontFamily = SuqiFontFamily, fontSize = 13.sp, color = SuqiPrimary)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${cat.subCategories.size} subcategories · Order #${cat.sortOrder}",
                                        fontSize = 12.sp,
                                        color = colors.secondaryText
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            editingCategory = cat
                                            showCategorySheet = true
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(LucideIcons.Pencil, contentDescription = "Edit category", tint = SuqiPrimary, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                viewModel.repository.deleteCategory(cat.id)
                                                loadCategories()
                                                viewModel.showToast("Category removed")
                                            }
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(LucideIcons.Trash2, contentDescription = "Delete category", tint = SuqiError, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(shopTypes) { (title, desc, fee) ->
                        SuqiSectionCard(padding = 14.dp) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(title, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(colors.primaryTint)
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(fee, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SuqiPrimary)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(desc, fontSize = 12.sp, color = colors.secondaryText)
                                }
                                SuqiStatusBadge("Active", StatusType.SUCCESS)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCategorySheet) {
        var nameEn by remember(editingCategory) { mutableStateOf(editingCategory?.name.orEmpty()) }
        var nameAm by remember(editingCategory) { mutableStateOf(editingCategory?.nameAmharic.orEmpty()) }
        var nameOr by remember(editingCategory) { mutableStateOf(editingCategory?.nameAfaanOromo.orEmpty()) }
        var imageUrl by remember(editingCategory) { mutableStateOf(editingCategory?.imageUrl.orEmpty()) }
        var subsCsv by remember(editingCategory) { mutableStateOf(editingCategory?.subCategories?.joinToString(", ").orEmpty()) }

        SuqiBottomSheet(
            onDismissRequest = { showCategorySheet = false },
            title = if (editingCategory == null) "Add Marketplace Category" else "Edit Category"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SuqiTextField(value = nameEn, onValueChange = { nameEn = it }, label = "Category Name (English) *")
                SuqiTextField(value = nameAm, onValueChange = { nameAm = it }, label = "Name (Amharic / አማርኛ)")
                SuqiTextField(value = nameOr, onValueChange = { nameOr = it }, label = "Name (Afaan Oromo)")
                SuqiTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = "Image URL")
                SuqiTextField(value = subsCsv, onValueChange = { subsCsv = it }, label = "Sub-categories (comma separated)")
                SuqiButton(
                    text = if (editingCategory == null) "Create Category" else "Save Category",
                    onClick = {
                        if (nameEn.isBlank()) {
                            viewModel.showToast("Category name is required")
                            return@SuqiButton
                        }
                        val subs = subsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        coroutineScope.launch {
                            viewModel.repository.saveCategory(
                                CategoryDto(
                                    id = editingCategory?.id.orEmpty(),
                                    name = nameEn.trim(),
                                    nameAmharic = nameAm.trim(),
                                    nameAfaanOromo = nameOr.trim(),
                                    icon = editingCategory?.icon ?: "Tag",
                                    imageUrl = imageUrl.trim(),
                                    subCategories = subs,
                                    isActive = true,
                                    sortOrder = editingCategory?.sortOrder ?: (categories.size + 1)
                                )
                            )
                            showCategorySheet = false
                            loadCategories()
                            viewModel.showToast("Category saved")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ==========================================
// ADMIN TOOLS: Persisted Home Promotional Banners
// ==========================================
@Composable
fun AdminBannersScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val isOffline by viewModel.isOffline.collectAsState()

    var banners by remember { mutableStateOf<List<PromoBannerDto>>(emptyList()) }
    var editingBanner by remember { mutableStateOf<PromoBannerDto?>(null) }
    var showBannerSheet by remember { mutableStateOf(false) }

    fun loadBanners() {
        coroutineScope.launch {
            val res = viewModel.repository.getPromoBanners(activeOnly = false)
            if (res.success && res.data != null) {
                banners = res.data
            }
        }
    }

    LaunchedEffect(Unit) {
        loadBanners()
    }

    SuqiScreen(
        title = "Promotional Banners",
        subtitle = "${banners.count { it.isActive }} active on Home carousel",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        headerActions = {
            IconButton(
                onClick = {
                    editingBanner = null
                    showBannerSheet = true
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(SuqiPrimary)
                    .testTag("admin_add_banner_btn")
            ) {
                Icon(LucideIcons.Plus, contentDescription = "Add banner", tint = Color.White)
            }
        },
        modifier = modifier
    ) { paddingValues ->
        if (banners.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                SuqiEmptyState(
                    icon = LucideIcons.GalleryHorizontal,
                    title = "No promotional banners",
                    subtitle = "Create a banner campaign to feature it on the Home screen carousel.",
                    ctaText = "Add Banner",
                    onCtaClick = {
                        editingBanner = null
                        showBannerSheet = true
                    }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(banners, key = { it.id }) { banner ->
                    SuqiSectionCard(padding = 0.dp) {
                        Box(modifier = Modifier.fillMaxWidth().height(132.dp)) {
                            AsyncImage(
                                model = banner.imageUrl,
                                contentDescription = banner.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.42f))
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(14.dp)
                            ) {
                                if (banner.badge.isNotBlank()) {
                                    Text(
                                        text = banner.badge.uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SuqiPrimary
                                    )
                                }
                                Text(
                                    text = "${banner.title} ${banner.highlight}".trim(),
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(10.dp)
                            ) {
                                SuqiStatusBadge(
                                    statusText = if (banner.isActive) "Active" else "Paused",
                                    type = if (banner.isActive) StatusType.SUCCESS else StatusType.NEUTRAL
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(banner.subtitle, fontSize = 12.sp, color = colors.secondaryText, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("Target: ${banner.targetName.ifBlank { banner.targetCategory }}", fontSize = 11.sp, color = colors.mutedText)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = banner.isActive,
                                    onCheckedChange = { checked ->
                                        coroutineScope.launch {
                                            viewModel.repository.savePromoBanner(banner.copy(isActive = checked))
                                            loadBanners()
                                        }
                                    },
                                    colors = SwitchDefaults.colors(checkedTrackColor = SuqiPrimary)
                                )
                                IconButton(
                                    onClick = {
                                        editingBanner = banner
                                        showBannerSheet = true
                                    }
                                ) {
                                    Icon(LucideIcons.Pencil, contentDescription = "Edit banner", tint = SuqiPrimary, modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.repository.deletePromoBanner(banner.id)
                                            loadBanners()
                                            viewModel.showToast("Banner deleted")
                                        }
                                    }
                                ) {
                                    Icon(LucideIcons.Trash2, contentDescription = "Delete banner", tint = SuqiError, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBannerSheet) {
        var badge by remember(editingBanner) { mutableStateOf(editingBanner?.badge ?: "FEATURED") }
        var title by remember(editingBanner) { mutableStateOf(editingBanner?.title.orEmpty()) }
        var highlight by remember(editingBanner) { mutableStateOf(editingBanner?.highlight.orEmpty()) }
        var subtitle by remember(editingBanner) { mutableStateOf(editingBanner?.subtitle.orEmpty()) }
        var ctaText by remember(editingBanner) { mutableStateOf(editingBanner?.buttonText ?: "Shop Now") }
        var imageUrl by remember(editingBanner) {
            mutableStateOf(
                editingBanner?.imageUrl?.ifBlank { "https://images.unsplash.com/photo-1607082348824-0a96f2a4b9da?w=800" }
                    ?: "https://images.unsplash.com/photo-1607082348824-0a96f2a4b9da?w=800"
            )
        }
        var targetCat by remember(editingBanner) { mutableStateOf(editingBanner?.targetCategory ?: "cat_electronics") }
        var targetName by remember(editingBanner) { mutableStateOf(editingBanner?.targetName ?: "Electronics") }

        SuqiBottomSheet(
            onDismissRequest = { showBannerSheet = false },
            title = if (editingBanner == null) "Add Carousel Banner" else "Edit Banner"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SuqiTextField(value = badge, onValueChange = { badge = it }, label = "Badge Label")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SuqiTextField(value = title, onValueChange = { title = it }, label = "Title *", modifier = Modifier.weight(1f))
                    SuqiTextField(value = highlight, onValueChange = { highlight = it }, label = "Highlight", modifier = Modifier.weight(1f))
                }
                SuqiTextField(value = subtitle, onValueChange = { subtitle = it }, label = "Subtitle")
                SuqiTextField(value = ctaText, onValueChange = { ctaText = it }, label = "Button CTA")
                SuqiTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = "Banner Image URL")
                val categoryOptions = MarketplaceCategories.all.map { it.id to it.name }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    categoryOptions.forEach { (id, lbl) ->
                        val sel = targetCat == id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (sel) SuqiPrimary else colors.secondaryBackground)
                                .clickable {
                                    targetCat = id
                                    targetName = lbl
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(lbl, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (sel) Color.White else colors.primaryText)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                SuqiButton(
                    text = if (editingBanner == null) "Publish Banner" else "Save Banner",
                    onClick = {
                        if (title.isBlank()) {
                            viewModel.showToast("Enter banner title")
                            return@SuqiButton
                        }
                        coroutineScope.launch {
                            viewModel.repository.savePromoBanner(
                                PromoBannerDto(
                                    id = editingBanner?.id.orEmpty(),
                                    badge = badge.trim(),
                                    title = title.trim(),
                                    highlight = highlight.trim(),
                                    subtitle = subtitle.trim(),
                                    buttonText = ctaText.trim().ifBlank { "Shop Now" },
                                    imageUrl = imageUrl.trim(),
                                    targetCategory = targetCat,
                                    targetName = targetName,
                                    isActive = editingBanner?.isActive ?: true,
                                    sortOrder = editingBanner?.sortOrder ?: (banners.size + 1)
                                )
                            )
                            showBannerSheet = false
                            loadBanners()
                            viewModel.showToast("Banner saved to Home carousel")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ==========================================
// ADMIN TOOLS: Persisted Review & Report Moderation
// ==========================================
@Composable
fun AdminModerationScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val isOffline by viewModel.isOffline.collectAsState()
    var flaggedReviews by remember { mutableStateOf<List<FlaggedReviewDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun loadFlagged() {
        coroutineScope.launch {
            isLoading = true
            val res = viewModel.repository.getFlaggedReviews()
            if (res.success && res.data != null) {
                flaggedReviews = res.data.filter { it.status == "REPORTED" }
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadFlagged()
    }

    SuqiScreen(
        title = "Review Moderation",
        subtitle = "${flaggedReviews.size} flagged reports",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        if (isLoading) {
            SuqiSkeletonList(count = 3, modifier = Modifier.padding(paddingValues).padding(top = 12.dp))
        } else if (flaggedReviews.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                SuqiEmptyState(
                    icon = LucideIcons.ShieldCheck,
                    title = "Moderation queue clean",
                    subtitle = "No flagged reviews or spam reports currently require review."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(flaggedReviews, key = { it.id }) { item ->
                    SuqiSectionCard(padding = 16.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Review by ${item.authorName}", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primaryText)
                            SuqiStatusBadge("Flagged", StatusType.ERROR)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Reason: ${item.reason}", fontSize = 12.sp, color = SuqiError, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.secondaryBackground)
                                .padding(10.dp)
                        ) {
                            Text("\"${item.reviewText}\"", fontSize = 13.sp, color = colors.primaryText)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        viewModel.repository.reportOrModerateReview(
                                            mapOf("id" to item.id, "action" to "DISMISS")
                                        )
                                        loadFlagged()
                                        viewModel.showToast("Report dismissed")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.secondaryBackground),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Text("Dismiss Report", color = colors.primaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        viewModel.repository.reportOrModerateReview(
                                            mapOf("id" to item.id, "action" to "DELETE")
                                        )
                                        loadFlagged()
                                        viewModel.showToast("Review removed")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SuqiError),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Text("Remove Content", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// ADMIN TOOLS: Persisted Subscription Plans
// ==========================================
@Composable
fun AdminSubscriptionPlansScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val isOffline by viewModel.isOffline.collectAsState()

    var plans by remember { mutableStateOf<List<SubscriptionPlanDto>>(emptyList()) }
    var editingPlan by remember { mutableStateOf<SubscriptionPlanDto?>(null) }
    var showPlanSheet by remember { mutableStateOf(false) }

    fun loadPlans() {
        coroutineScope.launch {
            val res = viewModel.repository.getSubscriptionPlans()
            if (res.success && res.data != null) {
                plans = res.data
            }
        }
    }

    LaunchedEffect(Unit) {
        loadPlans()
    }

    SuqiScreen(
        title = "Subscription Plans",
        subtitle = "${plans.size} merchant tiers",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        headerActions = {
            IconButton(
                onClick = {
                    editingPlan = null
                    showPlanSheet = true
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(SuqiPrimary)
            ) {
                Icon(LucideIcons.Plus, contentDescription = "Add plan", tint = Color.White)
            }
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(plans, key = { it.id }) { plan ->
                SuqiSectionCard(padding = 16.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(plan.name, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SuqiPrimary)
                        SuqiStatusBadge(if (plan.isActive) "Active" else "Hidden", if (plan.isActive) StatusType.SUCCESS else StatusType.NEUTRAL)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (plan.monthlyPrice <= 0.0) "Free" else "${formatEtb(plan.monthlyPrice)} / month",
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = colors.primaryText
                    )
                    Text(
                        text = if (plan.maxProducts < 0) "Unlimited products" else "Up to ${plan.maxProducts} products",
                        fontSize = 12.sp,
                        color = colors.mutedText
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    plan.featuresCsv.split("|").map { it.trim() }.filter { it.isNotBlank() }.forEach { feat ->
                        Text("✓ $feat", fontSize = 13.sp, color = colors.secondaryText, modifier = Modifier.padding(vertical = 2.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        SuqiButton(
                            text = "Edit Tier",
                            isPrimary = false,
                            onClick = {
                                editingPlan = plan
                                showPlanSheet = true
                            },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.repository.deleteSubscriptionPlan(plan.id)
                                    loadPlans()
                                    viewModel.showToast("Plan removed")
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, SuqiError.copy(alpha = 0.5f)),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(LucideIcons.Trash2, contentDescription = "Delete plan", tint = SuqiError, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }

    if (showPlanSheet) {
        var name by remember(editingPlan) { mutableStateOf(editingPlan?.name.orEmpty()) }
        var monthlyStr by remember(editingPlan) { mutableStateOf(editingPlan?.monthlyPrice?.toInt()?.toString() ?: "299") }
        var maxProdStr by remember(editingPlan) { mutableStateOf(editingPlan?.maxProducts?.toString() ?: "50") }
        var featuresCsv by remember(editingPlan) { mutableStateOf(editingPlan?.featuresCsv ?: "Featured Placement|Sales Analytics|Unlimited Promos") }
        var isActive by remember(editingPlan) { mutableStateOf(editingPlan?.isActive ?: true) }

        SuqiBottomSheet(
            onDismissRequest = { showPlanSheet = false },
            title = if (editingPlan == null) "Create Subscription Tier" else "Edit Subscription Tier"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SuqiTextField(value = name, onValueChange = { name = it }, label = "Plan Name *")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SuqiTextField(value = monthlyStr, onValueChange = { monthlyStr = it }, label = "Monthly (ETB)", modifier = Modifier.weight(1f))
                    SuqiTextField(value = maxProdStr, onValueChange = { maxProdStr = it }, label = "Max Products (-1=∞)", modifier = Modifier.weight(1f))
                }
                SuqiTextField(value = featuresCsv, onValueChange = { featuresCsv = it }, label = "Features (| separated)", singleLine = false)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Plan Active", fontFamily = SuqiFontFamily, fontSize = 14.sp, color = colors.primaryText)
                    Switch(checked = isActive, onCheckedChange = { isActive = it }, colors = SwitchDefaults.colors(checkedTrackColor = SuqiPrimary))
                }
                SuqiButton(
                    text = "Save Plan",
                    onClick = {
                        if (name.isBlank()) {
                            viewModel.showToast("Plan name is required")
                            return@SuqiButton
                        }
                        val monthly = monthlyStr.toDoubleOrNull() ?: 0.0
                        coroutineScope.launch {
                            viewModel.repository.saveSubscriptionPlan(
                                SubscriptionPlanDto(
                                    id = editingPlan?.id.orEmpty(),
                                    name = name.trim(),
                                    monthlyPrice = monthly,
                                    yearlyPrice = monthly * 10.0,
                                    maxProducts = maxProdStr.toIntOrNull() ?: -1,
                                    featuresCsv = featuresCsv.trim(),
                                    isPopular = editingPlan?.isPopular ?: false,
                                    isActive = isActive
                                )
                            )
                            showPlanSheet = false
                            loadPlans()
                            viewModel.showToast("Subscription plan saved")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ==========================================
// Developer Diagnostics & Live Inspector
// ==========================================
@Composable
fun AdminDevToolsScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val currentUser by viewModel.currentUser.collectAsState()
    val activeRole by viewModel.activeRole.collectAsState()
    val adminTier by viewModel.adminTier.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()

    var counts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    fun refreshCounts() {
        coroutineScope.launch {
            val map = linkedMapOf<String, Int>()
            map["Products"] = viewModel.repository.getProducts().data?.size ?: 0
            map["Shops"] = viewModel.repository.getShops().data?.size ?: 0
            map["Orders"] = viewModel.repository.getOrders().data?.size ?: 0
            map["Categories"] = viewModel.repository.getCategories().data?.size ?: 0
            map["Banners"] = viewModel.repository.getPromoBanners(activeOnly = false).data?.size ?: 0
            map["Flagged Reviews"] = viewModel.repository.getFlaggedReviews().data?.size ?: 0
            map["Support Tickets"] = viewModel.repository.getSupportTickets().data?.size ?: 0
            map["Subscription Plans"] = viewModel.repository.getSubscriptionPlans().data?.size ?: 0
            counts = map
        }
    }

    LaunchedEffect(Unit) {
        refreshCounts()
    }

    SuqiScreen(
        title = "Developer Diagnostics",
        subtitle = "Live Room DB & session inspector",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SuqiSectionCard(padding = 16.dp) {
                    Text("Active Session", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("User ID: ${currentUser?.id ?: "none"} (${currentUser?.name ?: "Guest"})", fontSize = 13.sp, color = colors.secondaryText)
                    Text("Role: ${currentUser?.role ?: "GUEST"}  •  Active Role: $activeRole  •  Admin Tier: $adminTier", fontSize = 13.sp, color = colors.secondaryText)
                    Text("Shop ID: ${currentUser?.shopId ?: "none"}", fontSize = 13.sp, color = colors.secondaryText)
                }
            }

            item {
                SuqiSectionCard(padding = 16.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Database Table Counts", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                        TextButton(onClick = { refreshCounts() }) {
                            Text("Refresh", fontWeight = FontWeight.Bold, color = SuqiPrimary)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    counts.forEach { (table, cnt) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(table, fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.secondaryText)
                            Text(cnt.toString(), fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.primaryText)
                        }
                    }
                }
            }

            item {
                SuqiSectionCard(padding = 16.dp) {
                    Text("Quick Actions & Simulators", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Offline Mode Banner", fontFamily = SuqiFontFamily, fontSize = 14.sp, color = colors.primaryText)
                        Switch(
                            checked = isOffline,
                            onCheckedChange = { viewModel.setOfflineMode(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = SuqiPrimary)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("CUSTOMER", "SELLER", "ADMIN", "GUEST").forEach { role ->
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (activeRole == role) SuqiPrimary else colors.secondaryBackground)
                                    .clickable {
                                        coroutineScope.launch {
                                            if (role == "GUEST") {
                                                viewModel.repository.loginAsGuest()
                                            } else if (role == "ADMIN") {
                                                viewModel.repository.adminSignIn("admin@suqi.et", "123456", "DEVELOPER")
                                            } else {
                                                viewModel.setActiveRole(role)
                                            }
                                            viewModel.refreshSession()
                                            viewModel.showToast("Switched mode to $role")
                                        }
                                    }
                                    .padding(vertical = 10.dp)
                            ) {
                                Text(
                                    text = role,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (activeRole == role) Color.White else colors.primaryText
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    SuqiButton(
                        text = "Resync Session, Cart & Notifications",
                        icon = LucideIcons.RefreshCw,
                        onClick = {
                            viewModel.refreshSession()
                            refreshCounts()
                            viewModel.showToast("Session and caches resynced")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
