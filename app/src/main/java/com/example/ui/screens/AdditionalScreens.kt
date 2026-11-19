package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.example.BuildConfig
import com.example.data.api.BackendConfig
import com.example.data.api.DevDiagnosticsLog
import com.example.data.api.SuqiEndpoints
import com.example.data.local.SuqiDatabase
import com.example.ui.i18n.placeLabel
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
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
import com.example.ui.i18n.LocalSuqiLanguage
import com.example.ui.i18n.localizedName
import com.example.ui.i18n.suqiStrings
import com.example.ui.theme.*
import com.example.util.EthiopianMarketplaceUtils
import kotlinx.coroutines.launch

@Composable
fun SavedAddressesScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val isOffline by viewModel.isOffline.collectAsState()
    var addresses by remember { mutableStateOf(viewModel.cachedSavedAddresses) }
    var editingAddress by remember { mutableStateOf<SavedAddressDto?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(addresses.isEmpty()) }
    var activeOrders by remember { mutableStateOf<List<OrderDto>>(emptyList()) }
    val listState = rememberLazyListState()

    BackHandler(enabled = showSheet) {
        showSheet = false
    }

    fun loadAddresses() {
        coroutineScope.launch {
            isLoading = addresses.isEmpty()
            val res = viewModel.repository.getSavedAddresses()
            if (res.success && res.data != null) {
                addresses = res.data
                viewModel.cachedSavedAddresses = res.data
            }
            val oRes = viewModel.repository.getOrders()
            if (oRes.success && oRes.data != null) activeOrders = oRes.data
            viewModel.refreshSession()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadAddresses()
    }

    SuqiScreen(
        title = strings.savedAddresses,
        subtitle = "${addresses.size}",
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
                Icon(LucideIcons.Plus, contentDescription = strings.checkout.addAddressBtn)
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
                    title = strings.buyer.noAddressesTitle,
                    subtitle = strings.buyer.noAddressesSub,
                    ctaText = strings.buyer.addNewAddress,
                    onCtaClick = {
                        editingAddress = null
                        showSheet = true
                    },
                    testTag = "addresses_empty"
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(addresses, key = { it.id }) { addr ->
                    val inUse = isAddressInUse(addr, activeOrders)
                    SuqiSectionCard(padding = 16.dp, modifier = Modifier.testTag("address_card_${addr.id}")) {
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
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (inUse) {
                                    SuqiStatusBadge(strings.buyerWork.inUsePill, StatusType.WARNING, modifier = Modifier.testTag("address_in_use_${addr.id}"))
                                }
                                if (addr.isDefault) {
                                    SuqiStatusBadge(strings.buyer.defaultBadge, StatusType.SUCCESS)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = addr.phone, fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.secondaryText)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${addr.streetOrLandmark}, ${placeLabel(addr.subCity)}, ${placeLabel(addr.city)}",
                            fontFamily = SuqiFontFamily,
                            fontSize = 13.sp,
                            color = colors.primaryText
                        )
                        if (addr.deliveryInstructions.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = addr.deliveryInstructions,
                                fontFamily = SuqiFontFamily,
                                fontSize = 12.sp,
                                color = colors.mutedText
                            )
                        }
                        if (inUse) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = strings.buyer.addressInUseBlocked,
                                fontFamily = SuqiFontFamily,
                                fontSize = 11.sp,
                                color = SuqiWarning
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            if (!addr.isDefault) {
                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.repository.saveAddress(addr.copy(isDefault = true))
                                            viewModel.showToast(strings.buyer.defaultBadge)
                                            loadAddresses()
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, SuqiPrimary),
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                ) {
                                    Text(strings.buyer.setAsDefault, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SuqiPrimary, maxLines = 2)
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    editingAddress = addr
                                    showSheet = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, colors.border),
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                            ) {
                                Text(strings.editLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.primaryText, maxLines = 2)
                            }
                            OutlinedButton(
                                onClick = {
                                    if (inUse) {
                                        viewModel.showToast(strings.buyer.addressInUseBlocked)
                                        return@OutlinedButton
                                    }
                                    coroutineScope.launch {
                                        val res = viewModel.repository.deleteSavedAddress(addr.id)
                                        if (res.success) {
                                            viewModel.showToast(strings.deleteLabel)
                                            loadAddresses()
                                        } else {
                                            viewModel.showToast(res.message ?: strings.buyer.addressInUseBlocked)
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (inUse) colors.border else SuqiError.copy(alpha = 0.4f)),
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("address_delete_${addr.id}")
                            ) {
                                Text(strings.deleteLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (inUse) colors.mutedText else SuqiError, maxLines = 2)
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

        val sheetContext = androidx.compose.ui.platform.LocalContext.current
        var gpsNote by remember(editingAddress) { mutableStateOf("") }
        fun fillFromDevice() {
            val fix = lastKnownCoordinates(sheetContext)
            if (fix == null) {
                viewModel.showToast(strings.buyerWork.locationUnavailable)
            } else {
                val coords = formatCoordinates(fix.first, fix.second)
                gpsNote = String.format(strings.buyerWork.locationFilledTemplate, coords)
                if (address.isBlank()) address = gpsNote
            }
        }
        val locationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
        ) { grants ->
            if (grants.values.any { it }) fillFromDevice() else viewModel.showToast(strings.buyerWork.locationUnavailable)
        }

        SuqiBottomSheet(
            onDismissRequest = { showSheet = false },
            title = if (editingAddress == null) strings.buyer.addAddressTitle else strings.buyer.editAddressTitle
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MapTilePlaceholder(label = strings.buyerWork.mapPreviewLabel, modifier = Modifier.fillMaxWidth().height(120.dp))
                Text(
                    text = gpsNote.ifBlank { strings.buyerWork.mapPinNote },
                    fontFamily = SuqiFontFamily, fontSize = 11.sp, color = colors.mutedText
                )
                OutlinedButton(
                    onClick = {
                        val fine = androidx.core.content.ContextCompat.checkSelfPermission(sheetContext, android.Manifest.permission.ACCESS_FINE_LOCATION)
                        val coarse = androidx.core.content.ContextCompat.checkSelfPermission(sheetContext, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                        if (fine == android.content.pm.PackageManager.PERMISSION_GRANTED || coarse == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            fillFromDevice()
                        } else {
                            locationPermissionLauncher.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION))
                        }
                    },
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, SuqiPrimary),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("address_use_current_location")
                ) {
                    Icon(LucideIcons.MapPin, contentDescription = "Map pin", tint = SuqiPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.buyerWork.useCurrentLocationBtn, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, color = SuqiPrimary)
                }
                SuqiTextField(value = name, onValueChange = { name = it }, label = "${strings.buyer.recipientNameField} *")
                SuqiTextField(value = phone, onValueChange = { phone = it }, label = "${strings.buyer.phoneNumberLabel} (+251) *")
                SuqiTextField(value = address, onValueChange = { address = it }, label = "${strings.buyer.landmarkField} *")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SuqiTextField(value = subCity, onValueChange = { subCity = it }, label = strings.checkout.businessSubCityLabel.removeSuffix(" *"), modifier = Modifier.weight(1f))
                    SuqiTextField(value = city, onValueChange = { city = it }, label = strings.checkout.businessCityLabel.removeSuffix(" *"), modifier = Modifier.weight(1f))
                }
                SuqiTextField(value = notes, onValueChange = { notes = it }, label = strings.checkout.standardDeliverySub)
                SuqiSwitchRow(
                    title = strings.buyer.setAsDefault,
                    checked = isDefault,
                    onCheckedChange = { isDefault = it },
                    testTag = "address_default_switch"
                )
                Spacer(modifier = Modifier.height(6.dp))
                SuqiButton(
                    text = strings.saveLabel,
                    onClick = {
                        if (name.isBlank() || address.isBlank()) {
                            viewModel.showToast("${strings.buyer.recipientNameField} / ${strings.buyer.landmarkField}")
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
                            viewModel.showToast(strings.saveLabel)
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
    val strings = suqiStrings()
    val isOffline by viewModel.isOffline.collectAsState()
    val cachedWishlisted = remember {
        viewModel.cachedAllProducts.filter { it.isWishlisted }.ifEmpty { viewModel.cachedHomeProducts.filter { it.isWishlisted } }
    }
    var products by remember { mutableStateOf(cachedWishlisted) }
    var isLoading by remember { mutableStateOf(cachedWishlisted.isEmpty()) }
    val listState = rememberLazyListState()

    fun loadWishlist() {
        coroutineScope.launch {
            isLoading = products.isEmpty()
            val res = viewModel.repository.getProducts()
            if (res.success && res.data != null) {
                viewModel.cachedAllProducts = res.data
                products = res.data.filter { it.isWishlisted }
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadWishlist()
    }

    SuqiScreen(
        title = strings.wishlistTitle,
        subtitle = String.format(strings.buyer.productsCountTemplate, products.size),
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
                    title = strings.buyer.emptyWishlistTitle,
                    subtitle = strings.buyer.emptyWishlistSub,
                    ctaText = strings.startShopping,
                    onCtaClick = { viewModel.popToRoot(ScreenDestination.Home) },
                    testTag = "wishlist_empty"
                )
            }
        } else {
            LazyColumn(
                state = listState,
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
    initialStarFilter: Int? = null,
    modifier: Modifier = Modifier
) {
    ReviewsV2Screen(
        targetId = targetId,
        targetName = targetName,
        targetType = targetType,
        initialStarFilter = initialStarFilter,
        viewModel = viewModel,
        modifier = modifier
    )
}

@Composable
private fun LegacyReviewsScreen(
    targetId: String,
    targetName: String,
    targetType: String,
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
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
        title = strings.customerReviews,
        subtitle = "$targetName (${reviews.size})",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.requireSignedInOrPrompt(strings.buyer.writeReviewTitle.lowercase()) {
                        showWriteDialog = true
                    }
                },
                containerColor = SuqiPrimary,
                contentColor = Color.White
            ) {
                Icon(LucideIcons.MessageSquarePlus, contentDescription = "New message")
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.buyer.writeReviewTitle)
            }
        },
        modifier = modifier
    ) { paddingValues ->
        if (reviews.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                SuqiEmptyState(
                    icon = LucideIcons.Star,
                    title = strings.buyer.noReviewsTitle,
                    subtitle = strings.buyer.noReviewsSub,
                    ctaText = strings.buyer.writeReviewTitle,
                    onCtaClick = {
                        viewModel.requireSignedInOrPrompt(strings.buyer.writeReviewTitle.lowercase()) {
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
                                text = strings.buyer.reportReviewLabel,
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
                                            viewModel.showToast(strings.buyer.reportReviewLabel)
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
            title = strings.buyer.writeReviewTitle
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(strings.buyer.minRatingLabel, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, color = colors.primaryText)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (i in 1..5) {
                        IconButton(onClick = { rating = i.toDouble() }, modifier = Modifier.size(48.dp)) {
                            Icon(
                                LucideIcons.StarFilled,
                                contentDescription = "$i ★",
                                tint = if (i <= rating) SuqiWarning else colors.border,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
                SuqiTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = strings.buyer.writeReviewTitle,
                    singleLine = false
                )
                SuqiButton(
                    text = strings.buyer.submitReviewLabel,
                    onClick = {
                        if (comment.isBlank()) {
                            viewModel.showToast(strings.buyer.writeReviewTitle)
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
                            viewModel.showToast(strings.buyer.submitReviewLabel)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
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
    val strings = suqiStrings()
    val isOffline by viewModel.isOffline.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("suqi_admin_prefs", android.content.Context.MODE_PRIVATE) }
    var rememberDevice by remember { mutableStateOf(prefs.getBoolean("remember_device", false)) }
    var selectedTier by remember {
        mutableStateOf(
            if (rememberDevice) (prefs.getString("saved_tier", "OWNER") ?: "OWNER") else "OWNER"
        )
    }
    var identifier by remember {
        mutableStateOf(
            if (rememberDevice) (prefs.getString("saved_identifier", "") ?: "") else ""
        )
    }
    var pin by remember { mutableStateOf("") }
    var showPin by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val tiers = listOf(
        "OWNER" to strings.adminOps.ownerRoleLabel,
        "ADMIN" to strings.adminOps.adminRoleLabel,
        "DEVELOPER" to strings.adminOps.developerRoleLabel
    )

    SuqiScreen(
        title = strings.admin.staffSignInTitle,
        subtitle = strings.common.accessRestrictedTitle,
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
                            Icon(LucideIcons.ShieldCheck, contentDescription = "Shield check", tint = SuqiPrimary, modifier = Modifier.size(34.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = strings.admin.consoleTitle,
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = colors.primaryText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = strings.admin.adminRestrictedSub,
                            fontFamily = SuqiFontFamily,
                            fontSize = 13.sp,
                            color = colors.secondaryText,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        tiers.forEach { (key, label) ->
                            val selected = selectedTier == key
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (selected) SuqiPrimary else colors.secondaryBackground)
                                    .border(1.dp, if (selected) SuqiPrimary else colors.border, RoundedCornerShape(14.dp))
                                    .clickable { selectedTier = key }
                                    .padding(horizontal = 4.dp, vertical = 6.dp)
                                    .testTag("admin_tier_$key")
                            ) {
                                Text(
                                    text = label,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    maxLines = 2,
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
                        label = "${strings.admin.staffIdLabel} *",
                        placeholder = "admin@suqi.et",
                        leadingIcon = LucideIcons.CircleUser,
                        testTag = "admin_identifier_input"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "${strings.admin.staffPinLabel} *",
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
                        placeholder = { Text(strings.admin.staffPinLabel, color = colors.mutedText) },
                        leadingIcon = { Icon(LucideIcons.Lock, contentDescription = "Lock", tint = colors.secondaryText) },
                        trailingIcon = {
                            IconButton(onClick = { showPin = !showPin }) {
                                Icon(
                                    imageVector = if (showPin) LucideIcons.EyeOff else LucideIcons.Eye,
                                    contentDescription = strings.admin.staffPinLabel,
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

                    Spacer(modifier = Modifier.height(12.dp))

                    SuqiSwitchRow(
                        title = strings.admin.rememberDeviceLabel,
                        subtitle = strings.admin.staffIdLabel,
                        checked = rememberDevice,
                        onCheckedChange = { checked ->
                            rememberDevice = checked
                            prefs.edit().putBoolean("remember_device", checked).apply()
                            if (!checked) {
                                prefs.edit().remove("saved_identifier").remove("saved_tier").apply()
                            }
                        },
                        testTag = "admin_remember_device"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    SuqiButton(
                        text = if (isSubmitting) strings.common.processingLabel else strings.admin.adminSignInBtn,
                        enabled = !isSubmitting,
                        onClick = {
                            if (identifier.isBlank() || pin.length < 4) {
                                errorMsg = "${strings.admin.staffIdLabel} / ${strings.admin.staffPinLabel}"
                                return@SuqiButton
                            }
                            isSubmitting = true
                            coroutineScope.launch {
                                val res = viewModel.repository.adminSignIn(identifier.trim(), pin.trim(), selectedTier)
                                isSubmitting = false
                                if (res.success) {
                                    if (rememberDevice) {
                                        prefs.edit()
                                            .putBoolean("remember_device", true)
                                            .putString("saved_identifier", identifier.trim())
                                            .putString("saved_tier", selectedTier)
                                            .apply()
                                    }
                                    if (selectedTier == "DEVELOPER" || selectedTier == "OWNER") {
                                        viewModel.unlockDeveloperTools()
                                    }
                                    viewModel.refreshSession()
                                    viewModel.showToast(selectedTier)
                                    viewModel.completeAdminSignIn()
                                } else {
                                    errorMsg = viewModel.localizeMessage(res.message ?: strings.common.accessRestrictedTitle)
                                }
                            }
                        },
                        testTag = "admin_sign_in_submit",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (com.example.data.api.BackendConfig.DemoMode) {
                item {
                    SuqiSectionCard(containerColor = colors.secondaryBackground, padding = 14.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings.adminOps.demoCredsHintTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.primaryText)
                                Text("ID: admin@suqi.et  •  PIN: 123456", fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.secondaryText)
                            }
                            TextButton(
                                onClick = {
                                    identifier = "admin@suqi.et"
                                    pin = "123456"
                                    errorMsg = null
                                }
                            ) {
                                Text(strings.adminOps.fillDemoCredentials, fontWeight = FontWeight.Bold, color = SuqiPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

// AdminConsoleScreen moved to AdminConsoleScreen.kt (A-03)

@Composable
fun FeatureToggleRow(
    title: String,
    sub: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    SuqiSwitchRow(
        title = title,
        subtitle = sub,
        checked = checked,
        onCheckedChange = onToggle
    )
}

@Composable
fun AdminFeaturesScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    AdminVerificationV2Screen(viewModel = viewModel, modifier = modifier)
}

// AdminCategoriesScreen & AdminBannersScreen moved to AdminCatalogCuratorScreens.kt (A-10 & A-12)
// ADMIN TOOLS: Persisted Review & Report Moderation
// ==========================================
@Composable
fun AdminModerationScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    AdminModerationV2Screen(viewModel = viewModel, modifier = modifier)
}

@Composable
private fun LegacyAdminModerationScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val isOffline by viewModel.isOffline.collectAsState()
    var flaggedReviews by remember { mutableStateOf(viewModel.cachedAdminFlaggedReviews) }
    var isLoading by remember { mutableStateOf(flaggedReviews.isEmpty()) }
    val listState = rememberLazyListState()

    fun loadFlagged() {
        coroutineScope.launch {
            isLoading = flaggedReviews.isEmpty()
            val res = viewModel.repository.getFlaggedReviews()
            if (res.success && res.data != null) {
                val reported = res.data.filter { it.status == "REPORTED" }
                flaggedReviews = reported
                viewModel.cachedAdminFlaggedReviews = reported
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadFlagged()
    }

    SuqiScreen(
        title = strings.admin.moderationTitle,
        subtitle = "${flaggedReviews.size}",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        if (isLoading && flaggedReviews.isEmpty()) {
            SuqiSkeletonList(count = 3, modifier = Modifier.padding(paddingValues).padding(top = 12.dp))
        } else if (flaggedReviews.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                SuqiEmptyState(
                    icon = LucideIcons.ShieldCheck,
                    title = strings.admin.moderationTitle,
                    subtitle = strings.admin.clearBadge
                )
            }
        } else {
            LazyColumn(
                state = listState,
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
                            Text(item.authorName, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primaryText)
                            SuqiStatusBadge(strings.adminGov.queueFlaggedReviews, StatusType.ERROR)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(item.reason, fontSize = 12.sp, color = SuqiError, fontWeight = FontWeight.Medium)
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
                                        viewModel.showToast(strings.adminGov.dismissActionBtn)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.secondaryBackground),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                            ) {
                                Text(strings.adminGov.dismissActionBtn, color = colors.primaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        viewModel.repository.reportOrModerateReview(
                                            mapOf("id" to item.id, "action" to "DELETE")
                                        )
                                        loadFlagged()
                                        viewModel.showToast(strings.adminGov.takeDownBtn)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SuqiError),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                            ) {
                                Text(strings.adminGov.takeDownBtn, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
    AdminPlansV2Screen(viewModel = viewModel, modifier = modifier)
}

@Composable
private fun LegacyAdminSubscriptionPlansScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val isOffline by viewModel.isOffline.collectAsState()

    var plans by remember { mutableStateOf(viewModel.cachedAdminPlans) }
    var editingPlan by remember { mutableStateOf<SubscriptionPlanDto?>(null) }
    var showPlanSheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    BackHandler(enabled = showPlanSheet) {
        showPlanSheet = false
    }

    fun loadPlans() {
        coroutineScope.launch {
            val res = viewModel.repository.getSubscriptionPlans()
            if (res.success && res.data != null) {
                plans = res.data
                viewModel.cachedAdminPlans = res.data
            }
        }
    }

    LaunchedEffect(Unit) {
        loadPlans()
    }

    SuqiScreen(
        title = strings.admin.plansMonetizationTitle,
        subtitle = "${plans.size}",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        headerActions = {
            IconButton(
                onClick = {
                    editingPlan = null
                    showPlanSheet = true
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SuqiPrimary)
            ) {
                Icon(LucideIcons.Plus, contentDescription = strings.adminOps.addPlanBtn, tint = Color.White)
            }
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            state = listState,
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
                        SuqiStatusBadge(
                            if (plan.isActive) strings.adminOps.activeStatus else strings.adminOps.hiddenStatus,
                            if (plan.isActive) StatusType.SUCCESS else StatusType.NEUTRAL
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (plan.monthlyPrice <= 0.0) strings.adminOps.freeLabel else String.format(strings.sellerOps.perMonthTemplate, formatEtb(plan.monthlyPrice)),
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = colors.primaryText
                    )
                    Text(
                        text = if (plan.maxProducts < 0) strings.adminOps.unlimitedProducts else String.format(strings.adminOps.upToProductsTemplate, plan.maxProducts),
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
                            text = strings.adminOps.editTierBtn,
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
                                    viewModel.showToast(strings.deleteLabel)
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, SuqiError.copy(alpha = 0.5f)),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(LucideIcons.Trash2, contentDescription = strings.deleteLabel, tint = SuqiError, modifier = Modifier.size(18.dp))
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
            title = if (editingPlan == null) strings.adminOps.addPlanBtn else strings.adminOps.editTierBtn
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SuqiTextField(value = name, onValueChange = { name = it }, label = strings.seller.paramNameRequiredLabel)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SuqiTextField(value = monthlyStr, onValueChange = { monthlyStr = it }, label = strings.seller.priceEtbLabel, modifier = Modifier.weight(1f))
                    SuqiTextField(value = maxProdStr, onValueChange = { maxProdStr = it }, label = strings.seller.stockLabel, modifier = Modifier.weight(1f))
                }
                SuqiTextField(value = featuresCsv, onValueChange = { featuresCsv = it }, label = strings.description, singleLine = false)
                SuqiSwitchRow(
                    title = strings.adminOps.activeStatus,
                    checked = isActive,
                    onCheckedChange = { isActive = it }
                )
                SuqiButton(
                    text = strings.saveLabel,
                    onClick = {
                        if (name.isBlank()) {
                            viewModel.showToast(strings.seller.enterNameError)
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
                            viewModel.showToast(strings.saveLabel)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ==========================================
// A-22 (P2): Developer Tools completion (live diagnostics, URL switch, snapshot export/import, persisted maintenance/min-version, live flags, design gallery)
// ==========================================
@Composable
fun AdminDevToolsScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val p6 = strings.admin6
    val activeRole by viewModel.activeRole.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    val adminSettings by viewModel.adminSettings.collectAsState()

    var env by rememberSaveable { mutableStateOf(if (BackendConfig.useBuiltinSqliteBackend) "Builtin" else "Remote API") }
    var baseUrlInput by rememberSaveable { mutableStateOf(BackendConfig.customBaseUrl) }
    var counts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var envStatus by remember { mutableStateOf(viewModel.repository.currentResolvedBaseUrl()) }
    var requestLogs by remember { mutableStateOf(DevDiagnosticsLog.entries()) }
    var recentErrorsCount by remember { mutableIntStateOf(DevDiagnosticsLog.errorCount()) }
    var featureFlags by remember { mutableStateOf<List<PlatformSwitchDto>>(emptyList()) }

    var maintenanceMode by remember(adminSettings) { mutableStateOf(adminSettings.maintenanceMode) }
    var maintenanceMessage by remember(adminSettings) { mutableStateOf(adminSettings.maintenanceMessage) }
    var enforceMinVersion by remember(adminSettings) { mutableStateOf(adminSettings.enforceMinVersion) }
    var minAppVersion by remember(adminSettings) { mutableStateOf(adminSettings.minAppVersion.ifBlank { BuildConfig.VERSION_NAME }) }

    var showResetConfirm by remember { mutableStateOf(false) }
    var exportedSnapshotJson by remember { mutableStateOf<String?>(null) }
    var showImportSheet by remember { mutableStateOf(false) }
    var importSnapshotJson by remember { mutableStateOf("") }

    fun refreshDiagnostics() {
        requestLogs = DevDiagnosticsLog.entries()
        recentErrorsCount = DevDiagnosticsLog.errorCount()
    }

    fun refreshAll() {
        coroutineScope.launch {
            val envRes = viewModel.repository.getDevEnvironment()
            if (envRes.success && envRes.data != null) {
                counts = envRes.data.filterKeys {
                    it in setOf("products", "shops", "orders", "users", "auditLog")
                }.mapValues { (_, v) -> v.toIntOrNull() ?: 0 }
            }
            val swRes = viewModel.repository.getPlatformSwitches()
            if (swRes.success && swRes.data != null) {
                featureFlags = swRes.data
            }
            viewModel.refreshAdminSettings()
            refreshDiagnostics()
        }
    }

    LaunchedEffect(Unit) {
        refreshAll()
    }

    fun applyEnvironment(selectedEnv: String, urlText: String) {
        env = selectedEnv
        BackendConfig.useBuiltinSqliteBackend = (selectedEnv == "Builtin")
        BackendConfig.customBaseUrl = urlText.trim()
        envStatus = viewModel.repository.currentResolvedBaseUrl()
        viewModel.showToast(envStatus)
    }

    fun testConnection() {
        coroutineScope.launch {
            val res = viewModel.repository.testDevConnection()
            refreshDiagnostics()
            if (res.success && res.data != null) {
                val mode = res.data["mode"]?.toString() ?: env
                val ok = res.data["ok"]?.toString() ?: "true"
                val ms = res.data["latencyMs"]?.toString() ?: "0"
                val url = res.data["baseUrl"]?.toString() ?: envStatus
                envStatus = "$mode • ok=$ok • ${ms}ms • $url"
                viewModel.showToast(envStatus)
            } else {
                envStatus = res.message ?: strings.retryLabel
                viewModel.showToast(envStatus)
            }
        }
    }

    fun seedDemoData() {
        coroutineScope.launch {
            val res = viewModel.repository.devDataAction("SEED_DEMO")
            refreshDiagnostics()
            if (res.success) {
                viewModel.refreshSession()
                refreshAll()
                viewModel.showToast(strings.common.doneLabel)
            } else {
                viewModel.showToast(res.message ?: strings.retryLabel)
            }
        }
    }

    fun resetDatabase() {
        coroutineScope.launch {
            val res = viewModel.repository.devDataAction("CLEAR_STATE")
            refreshDiagnostics()
            if (res.success) {
                viewModel.refreshSession()
                refreshAll()
                viewModel.showToast(strings.common.doneLabel)
            } else {
                viewModel.showToast(res.message ?: strings.retryLabel)
            }
        }
    }

    fun exportSnapshot() {
        coroutineScope.launch {
            val res = viewModel.repository.exportDevSnapshot()
            refreshDiagnostics()
            if (res.success && res.data != null) {
                exportedSnapshotJson = res.data
            } else {
                viewModel.showToast(res.message ?: strings.retryLabel)
            }
        }
    }

    fun saveMaintenanceConfig(
        nextMaint: Boolean = maintenanceMode,
        nextMsg: String = maintenanceMessage,
        nextEnforceMin: Boolean = enforceMinVersion,
        nextMinVer: String = minAppVersion
    ) {
        coroutineScope.launch {
            val res = viewModel.repository.updateDevConfig(
                maintenanceMode = nextMaint,
                maintenanceMessage = nextMsg.trim(),
                enforceMinVersion = nextEnforceMin,
                minAppVersion = nextMinVer.trim().ifBlank { BuildConfig.VERSION_NAME }
            )
            refreshDiagnostics()
            if (res.success && res.data != null) {
                viewModel.updateAdminSettingsState(res.data)
                viewModel.showToast(strings.saveLabel)
            } else {
                viewModel.showToast(res.message ?: strings.retryLabel)
            }
        }
    }

    SuqiScreen(
        title = strings.admin.devToolsTitle,
        subtitle = p6.featureFlagsTitle,
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = SuqiMaxContentWidth)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Environment & Base URL selector
                item {
                    SuqiSectionCard(padding = 16.dp, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = p6.baseUrlLabel,
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = colors.primaryText
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Builtin" to p6.envBuiltin, "Remote API" to p6.envRemote).forEach { (key, label) ->
                                val selected = env == key
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selected) SuqiPrimary else colors.cardBackground)
                                        .border(if (!selected) 1.dp else 0.dp, colors.border, RoundedCornerShape(12.dp))
                                        .clickable { applyEnvironment(key, baseUrlInput) }
                                        .testTag("dev_env_${key.lowercase().replace(" ", "_")}")
                                ) {
                                    Text(
                                        text = label,
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (selected) Color.White else colors.primaryText
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        SuqiTextField(
                            value = baseUrlInput,
                            onValueChange = { baseUrlInput = it },
                            label = p6.baseUrlLabel,
                            placeholder = SuqiEndpoints.BASE_URL,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "dev_base_url_input"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SuqiButton(
                                text = p6.saveUrlBtn,
                                onClick = { applyEnvironment(env, baseUrlInput) },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("dev_save_url_btn"),
                                isPrimary = false
                            )
                            SuqiButton(
                                text = p6.resetUrlBtn,
                                onClick = {
                                    baseUrlInput = ""
                                    applyEnvironment("Builtin", "")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("dev_reset_url_btn"),
                                isPrimary = false
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SuqiButton(
                                text = p6.testConnectionBtn,
                                onClick = { testConnection() },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("dev_test_connection_btn"),
                                isPrimary = false
                            )
                            SuqiButton(
                                text = strings.adminOps.refreshLabel,
                                onClick = { refreshAll() },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("dev_refresh_btn"),
                                isPrimary = false
                            )
                        }
                        if (envStatus.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = envStatus,
                                fontSize = 12.sp,
                                color = colors.secondaryText,
                                modifier = Modifier.testTag("dev_env_status")
                            )
                        }
                        if (counts.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = counts.entries.joinToString(" • ") { "${it.key}: ${it.value}" },
                                fontSize = 11.sp,
                                color = colors.mutedText,
                                modifier = Modifier.testTag("dev_db_counts")
                            )
                        }
                    }
                }

                // Data tools (Seed, Reset with Confirmation, Snapshot Export & Import)
                item {
                    SuqiSectionCard(padding = 16.dp, modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            SuqiButton(
                                text = p6.seedDemoDataBtn,
                                onClick = { seedDemoData() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("dev_seed_demo_btn")
                            )
                            SuqiButton(
                                text = p6.resetDatabaseBtn,
                                onClick = { showResetConfirm = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("dev_reset_db_btn"),
                                isPrimary = false
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SuqiButton(
                                    text = p6.exportSnapshotBtn,
                                    onClick = { exportSnapshot() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("dev_export_snapshot_btn"),
                                    isPrimary = false
                                )
                                SuqiButton(
                                    text = p6.importSnapshotBtn,
                                    onClick = { showImportSheet = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("dev_import_snapshot_btn"),
                                    isPrimary = false
                                )
                            }
                        }
                    }
                }

                // Act as (role switcher)
                item {
                    SuqiSectionCard(padding = 16.dp, modifier = Modifier.fillMaxWidth()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf("CUSTOMER", "SELLER", "ADMIN", "GUEST").forEach { role ->
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
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
                                                viewModel.showToast(role)
                                            }
                                        }
                                        .padding(horizontal = 4.dp, vertical = 10.dp)
                                        .testTag("dev_act_as_${role.lowercase()}")
                                ) {
                                    Text(
                                        text = when (role) {
                                            "CUSTOMER" -> p6.actAsBuyer
                                            "SELLER" -> p6.actAsSeller
                                            "ADMIN" -> p6.actAsAdmin
                                            else -> role
                                        },
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (activeRole == role) Color.White else colors.primaryText
                                    )
                                }
                            }
                        }
                    }
                }

                // Live Diagnostics (Schema Version + DevDiagnosticsLog)
                item {
                    SuqiSectionCard(padding = 16.dp, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = p6.diagnosticsTitle,
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = colors.primaryText
                            )
                            TextButton(
                                onClick = {
                                    DevDiagnosticsLog.clear()
                                    refreshDiagnostics()
                                },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("dev_clear_log_btn")
                            ) {
                                Text(p6.clearLogBtn, fontFamily = SuqiFontFamily, fontSize = 12.sp, color = SuqiPrimary)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(p6.schemaVersionLabel, fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.secondaryText)
                            Text(
                                text = SuqiDatabase.SCHEMA_VERSION.toString(),
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = colors.primaryText,
                                modifier = Modifier.testTag("dev_schema_version")
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(p6.requestLogLabel, fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.secondaryText)
                            Text(
                                text = requestLogs.size.toString(),
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = colors.primaryText,
                                modifier = Modifier.testTag("dev_request_count")
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(p6.recentErrorsLabel, fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.secondaryText)
                            Text(
                                text = recentErrorsCount.toString(),
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (recentErrorsCount > 0) SuqiError else SuqiSuccess,
                                modifier = Modifier.testTag("dev_error_count")
                            )
                        }
                        if (requestLogs.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = colors.border)
                            Spacer(modifier = Modifier.height(8.dp))
                            requestLogs.take(5).forEach { entry ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${entry.operation} ${entry.target}",
                                        fontSize = 11.sp,
                                        color = if (entry.success) colors.primaryText else SuqiError,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${entry.detail.ifBlank { if (entry.success) "OK" else "ERR" }} • ${entry.durationMs}ms",
                                        fontSize = 11.sp,
                                        color = colors.mutedText
                                    )
                                }
                            }
                        }
                    }
                }

                // Persisted Maintenance Mode & Minimum Version Gate
                item {
                    SuqiSectionCard(padding = 16.dp, modifier = Modifier.fillMaxWidth()) {
                        SuqiSwitchRow(
                            title = p6.maintenanceModeLabel,
                            subtitle = p6.maintenanceBannerDefaultBody,
                            checked = maintenanceMode,
                            onCheckedChange = { enabled ->
                                maintenanceMode = enabled
                                saveMaintenanceConfig(nextMaint = enabled)
                            },
                            testTag = "maintenance_mode"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SuqiTextField(
                            value = maintenanceMessage,
                            onValueChange = { maintenanceMessage = it },
                            label = p6.maintenanceBannerTitle,
                            placeholder = p6.maintenanceBannerDefaultBody,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "dev_maintenance_msg_input"
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        SuqiSwitchRow(
                            title = p6.minVersionLabel,
                            subtitle = "Current v${BuildConfig.VERSION_NAME}",
                            checked = enforceMinVersion,
                            onCheckedChange = { enabled ->
                                enforceMinVersion = enabled
                                saveMaintenanceConfig(nextEnforceMin = enabled)
                            },
                            testTag = "min_version"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SuqiTextField(
                            value = minAppVersion,
                            onValueChange = { minAppVersion = it },
                            label = p6.minVersionLabel,
                            placeholder = BuildConfig.VERSION_NAME,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "dev_min_version_input"
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        SuqiButton(
                            text = p6.saveConfigBtn,
                            onClick = { saveMaintenanceConfig() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("dev_save_config_btn"),
                            isPrimary = false
                        )
                    }
                }

                // Live Platform Feature Flags
                item {
                    SuqiSectionCard(padding = 16.dp, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = p6.featureFlagsTitle,
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = colors.primaryText
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            featureFlags.forEach { flag ->
                                SuqiSwitchRow(
                                    title = when (flag.id) {
                                        "q_and_a" -> p6.switchQandA
                                        "video_uploads" -> p6.switchVideoUploads
                                        "product_variants" -> p6.switchVariants
                                        "flash_sales" -> p6.switchFlashSales
                                        "compare_products" -> p6.switchCompare
                                        else -> flag.id
                                    },
                                    subtitle = "${p6.affectsLabel}: ${flag.groupName}",
                                    checked = flag.isEnabled,
                                    onCheckedChange = { enabled ->
                                        featureFlags = featureFlags.map {
                                            if (it.id == flag.id) it.copy(isEnabled = enabled) else it
                                        }
                                        coroutineScope.launch {
                                            viewModel.repository.savePlatformSwitch(flag.id, enabled, flag.numericValue)
                                            refreshDiagnostics()
                                        }
                                    },
                                    testTag = "flag_${flag.id}"
                                )
                            }
                        }
                    }
                }

                // Design gallery link
                item {
                    SuqiSectionCard(padding = 16.dp, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = LucideIcons.Palette,
                                contentDescription = p6.designGalleryTitle,
                                tint = colors.primaryText,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = p6.designGalleryTitle,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = colors.primaryText
                                )
                                Text(
                                    text = strings.appearanceTitle,
                                    fontSize = 12.sp,
                                    color = colors.secondaryText
                                )
                            }
                            SuqiButton(
                                text = strings.checkout.openLabel,
                                onClick = { viewModel.navigate(ScreenDestination.DesignGallery) },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("dev_open_design_gallery"),
                                isPrimary = false
                            )
                        }
                    }
                }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(p6.confirmResetTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold) },
            text = { Text(p6.confirmResetBody, fontFamily = SuqiFontFamily, color = colors.secondaryText) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirm = false
                        resetDatabase()
                    },
                    modifier = Modifier.testTag("dev_confirm_reset_btn")
                ) {
                    Text(p6.resetDatabaseBtn, color = SuqiError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text(strings.cancelLabel, color = colors.primaryText)
                }
            },
            containerColor = colors.cardBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }

    val snapshotText = exportedSnapshotJson
    if (snapshotText != null) {
        SuqiBottomSheet(
            onDismissRequest = { exportedSnapshotJson = null },
            title = p6.exportSnapshotBtn
        ) {
            SuqiTextField(
                value = snapshotText,
                onValueChange = {},
                readOnly = true,
                singleLine = false,
                label = "JSON",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp),
                testTag = "dev_snapshot_export_json"
            )
            Spacer(modifier = Modifier.height(12.dp))
            SuqiButton(
                text = p6.copyJsonBtn,
                onClick = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("Suqi Snapshot", snapshotText))
                    viewModel.showToast(strings.common.copiedToast)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("dev_snapshot_copy_btn")
            )
        }
    }

    if (showImportSheet) {
        SuqiBottomSheet(
            onDismissRequest = { showImportSheet = false },
            title = p6.importSnapshotBtn
        ) {
            SuqiTextField(
                value = importSnapshotJson,
                onValueChange = { importSnapshotJson = it },
                label = p6.snapshotPlaceholder,
                singleLine = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 260.dp),
                testTag = "dev_snapshot_import_input"
            )
            Spacer(modifier = Modifier.height(12.dp))
            SuqiButton(
                text = p6.importSnapshotBtn,
                enabled = importSnapshotJson.isNotBlank(),
                onClick = {
                    coroutineScope.launch {
                        val res = viewModel.repository.devDataAction("IMPORT_SNAPSHOT", importSnapshotJson.trim())
                        refreshDiagnostics()
                        if (res.success) {
                            showImportSheet = false
                            viewModel.refreshSession()
                            refreshAll()
                            viewModel.showToast(strings.common.doneLabel)
                        } else {
                            viewModel.showToast(res.message ?: strings.retryLabel)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("dev_snapshot_import_confirm_btn")
            )
        }
    }
}
