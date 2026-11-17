package com.example.ui.screens

import com.example.ui.i18n.placeLabel
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MarketplaceCategories
import com.example.data.model.ProductDto
import com.example.data.model.ReviewDto
import com.example.data.model.ShopDto
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.*
import com.example.ui.i18n.LocalSuqiLanguage
import com.example.ui.i18n.localizedName
import com.example.ui.i18n.suqiStrings
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ShopsScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val lang = LocalSuqiLanguage.current
    val cartCount by viewModel.cartCount.collectAsState(initial = 0)
    val isOffline by viewModel.isOffline.collectAsState()

    var selectedFilter by rememberSaveable { mutableStateOf("All") }
    var selectedCategory by rememberSaveable { mutableStateOf("All Categories") }
    val initialShops = viewModel.cachedShopsByFilter[selectedFilter] ?: if (selectedFilter == "All") viewModel.cachedHomeShops else emptyList()
    var shops by remember { mutableStateOf(initialShops) }
    var isLoading by remember { mutableStateOf(initialShops.isEmpty()) }
    val listState = rememberLazyListState()

    val filterOptions = listOf(
        "All" to strings.allLabel,
        "Verified" to strings.verifiedLabel,
        "Top Rated" to strings.checkout.filterTopRated,
        "Following" to strings.checkout.filterFollowing,
        "Nearby" to strings.checkout.filterNearby
    )
    val categoryOptions = listOf(
        "All Categories" to strings.allLabel,
        "Electronics" to (MarketplaceCategories.byId("cat_electronics")?.toDto()?.localizedName(lang) ?: "Electronics"),
        "Fashion" to (MarketplaceCategories.byId("cat_fashion")?.toDto()?.localizedName(lang) ?: "Fashion"),
        "Home & Living" to (MarketplaceCategories.byId("cat_home")?.toDto()?.localizedName(lang) ?: "Home & Living"),
        "Beauty & Care" to (MarketplaceCategories.byId("cat_beauty")?.toDto()?.localizedName(lang) ?: "Beauty & Care"),
        "Groceries" to (MarketplaceCategories.byId("cat_groceries")?.toDto()?.localizedName(lang) ?: "Groceries"),
        "Sports" to (MarketplaceCategories.byId("cat_sports")?.toDto()?.localizedName(lang) ?: "Sports")
    )

    fun loadShops() {
        coroutineScope.launch {
            isLoading = shops.isEmpty()
            val res = viewModel.repository.getShops(filter = selectedFilter.uppercase().replace(" ", "_"))
            if (res.success && res.data != null) {
                shops = res.data
                viewModel.cachedShopsByFilter[selectedFilter] = res.data
            }
            isLoading = false
        }
    }

    LaunchedEffect(selectedFilter) {
        loadShops()
    }

    val filteredShops = remember(shops, selectedCategory) {
        if (selectedCategory == "All Categories") {
            shops
        } else {
            shops.filter { it.category.contains(selectedCategory.take(5), ignoreCase = true) }
        }
    }

    SuqiScreen(
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SuqiTopHeader(
                title = strings.shopsTab,
                cartCount = cartCount,
                onCartClick = { viewModel.navigate(ScreenDestination.Cart) },
                actions = {
                    IconButton(
                        onClick = { viewModel.navigate(ScreenDestination.Search) },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(colors.secondaryBackground)
                            .testTag("shops_search_button")
                    ) {
                        Icon(
                            imageVector = LucideIcons.Search,
                            contentDescription = strings.buyer.searchShopsHint,
                            tint = colors.primaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = SuqiBottomNavContentPadding)
            ) {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categoryOptions) { (catKey, catLabel) ->
                            val isSel = catKey == selectedCategory
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (isSel) SuqiPrimary else colors.secondaryBackground)
                                    .clickable { selectedCategory = catKey }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = catLabel,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    color = if (isSel) Color.White else colors.primaryText
                                )
                            }
                        }
                    }
                }

                item {
                    SuqiFilterChips(
                        items = filterOptions,
                        selectedKey = selectedFilter,
                        onSelect = { selectedFilter = it },
                        testTagPrefix = "shop_filter"
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                if (isLoading) {
                    item {
                        SuqiSkeletonList(count = 4, modifier = Modifier.padding(top = 8.dp))
                    }
                } else if (filteredShops.isEmpty()) {
                    item {
                        SuqiEmptyState(
                            icon = LucideIcons.Store,
                            title = strings.buyer.noMatchingShops,
                            subtitle = strings.buyer.noProductsFilterSub,
                            ctaText = strings.buyer.resetFiltersTitle,
                            onCtaClick = {
                                selectedFilter = "All"
                                selectedCategory = "All Categories"
                            },
                            testTag = "shops_empty_state"
                        )
                    }
                } else {
                    items(filteredShops, key = { it.id }) { shop ->
                        ShopListItemRow(
                            shop = shop,
                            onClick = { viewModel.navigate(ScreenDestination.ShopStorefront(shop.id)) },
                            onToggleFollow = {
                                viewModel.requireSignedInOrPrompt(strings.extras.gateFollowShops) {
                                    coroutineScope.launch {
                                        viewModel.repository.toggleFollowShop(shop.id)
                                        loadShops()
                                    }
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
fun ShopListItemRow(
    shop: ShopDto,
    onClick: () -> Unit,
    onToggleFollow: () -> Unit
) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    SuqiSectionCard(
        padding = 14.dp,
        onClick = onClick,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("shop_card_${shop.id}")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.imageBackdrop)
            ) {
                AsyncImage(
                    model = shop.logoUrl,
                    contentDescription = shop.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = shop.name,
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = colors.primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (shop.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = LucideIcons.CheckCircle,
                            contentDescription = strings.verifiedLabel,
                            tint = SuqiPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(LucideIcons.StarFilled, contentDescription = null, tint = SuqiWarning, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${shop.rating} (${shop.reviewCount})",
                        fontFamily = SuqiFontFamily,
                        fontSize = 12.sp,
                        color = colors.secondaryText
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "•", fontSize = 11.sp, color = colors.mutedText)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.buyer.productsCountTemplate.format(shop.productCount),
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = SuqiPrimary
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "${placeLabel(shop.subCity)}, ${placeLabel(shop.city)} • ${shop.category}",
                    fontFamily = SuqiFontFamily,
                    fontSize = 12.sp,
                    color = colors.mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .defaultMinSize(minHeight = 40.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (shop.isFollowing) colors.secondaryBackground else colors.primaryTint)
                    .clickable(onClick = onToggleFollow)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (shop.isFollowing) strings.buyer.followingLabel else strings.buyer.followLabel,
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    maxLines = 2,
                    color = if (shop.isFollowing) colors.secondaryText else SuqiPrimary
                )
            }
        }
    }
}

@Composable
fun ShopStorefrontScreen(
    shopId: String,
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val lang = LocalSuqiLanguage.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isOffline by viewModel.isOffline.collectAsState()

    val cachedShop = viewModel.cachedShopDetails[shopId]
        ?: viewModel.cachedHomeShops.firstOrNull { it.id == shopId }
        ?: viewModel.cachedAllShops.firstOrNull { it.id == shopId }
    var shop by remember { mutableStateOf(cachedShop) }
    var products by remember { mutableStateOf(viewModel.cachedShopProducts[shopId].orEmpty()) }
    var reviews by remember { mutableStateOf(viewModel.cachedShopReviews[shopId].orEmpty()) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0: Products, 1: Collections, 2: Reviews
    var activeCollectionFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var shopSearchQuery by rememberSaveable { mutableStateOf("") }
    var showShareSheet by remember { mutableStateOf(false) }
    val storefrontListState = rememberLazyListState()

    BackHandler(enabled = showShareSheet) {
        showShareSheet = false
    }

    LaunchedEffect(shopId) {
        val sRes = viewModel.repository.getShopDetails(shopId)
        if (sRes.success && sRes.data != null) {
            shop = sRes.data
            viewModel.cachedShopDetails[shopId] = sRes.data
        }
        val pRes = viewModel.repository.getProducts(shopId = shopId)
        if (pRes.success && pRes.data != null) {
            products = pRes.data
            viewModel.cachedShopProducts[shopId] = pRes.data
        }
        val rRes = viewModel.repository.getReviews(targetId = shopId)
        if (rRes.success && rRes.data != null) {
            reviews = rRes.data
            viewModel.cachedShopReviews[shopId] = rRes.data
        }
    }

    val currentShop = shop
    if (currentShop == null) {
        SuqiScreen(
            title = strings.checkout.shopStorefrontTitle,
            onBackClick = { viewModel.goBack() },
            isOffline = isOffline,
            modifier = modifier
        ) { padding ->
            SuqiSkeletonList(
                count = 4,
                modifier = Modifier
                    .padding(padding)
                    .padding(top = 16.dp)
            )
        }
        return
    }

    fun collectionDisplayTitle(key: String): String = when (key) {
        "Best Sellers" -> strings.popularNow
        "Discount Deals" -> strings.hotDeals
        "In Stock Ready" -> strings.inStock
        else -> strings.recommendedForYou
    }

    SuqiScreen(
        title = currentShop.name,
        subtitle = "${placeLabel(currentShop.subCity)}, ${placeLabel(currentShop.city)}",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        headerActions = {
            IconButton(
                onClick = { showShareSheet = true },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colors.secondaryBackground)
                    .testTag("shop_share_button")
            ) {
                Icon(LucideIcons.Share2, contentDescription = strings.checkout.shareShopTitle, tint = colors.primaryText, modifier = Modifier.size(18.dp))
            }
            IconButton(
                onClick = { viewModel.navigate(ScreenDestination.Cart) },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colors.secondaryBackground)
                    .testTag("shop_cart_button")
            ) {
                Icon(LucideIcons.ShoppingCart, contentDescription = strings.cartTitle, tint = colors.primaryText, modifier = Modifier.size(18.dp))
            }
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            state = storefrontListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Cover Image & Logo
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp)
                ) {
                    AsyncImage(
                        model = currentShop.coverUrl,
                        contentDescription = currentShop.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .align(Alignment.BottomStart)
                            .offset(x = 16.dp, y = 28.dp)
                            .clip(CircleShape)
                            .border(3.dp, colors.cardBackground, CircleShape)
                            .background(colors.cardBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = currentShop.logoUrl,
                            contentDescription = currentShop.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(modifier = Modifier.height(36.dp))
            }

            // Shop Info & Vacation Banner
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    if (currentShop.isVacationMode) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.warningBg)
                                .padding(12.dp)
                        ) {
                            Icon(LucideIcons.Clock, contentDescription = null, tint = SuqiWarning, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.checkout.vacationNoticeTitle,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = colors.primaryText
                                )
                                val note = currentShop.vacationNote.ifBlank {
                                    if (currentShop.vacationReturnDate.isNotBlank()) currentShop.vacationReturnDate
                                    else strings.checkout.vacationNoticeBody
                                }
                                Text(text = note, fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.secondaryText)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentShop.name,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = colors.primaryText
                                )
                                if (currentShop.isVerified) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = LucideIcons.CheckCircle,
                                        contentDescription = strings.verifiedLabel,
                                        tint = SuqiPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(LucideIcons.StarFilled, contentDescription = null, tint = SuqiWarning, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${currentShop.rating} (${strings.checkout.reviewsCountTemplate.format(currentShop.reviewCount)})",
                                    fontFamily = SuqiFontFamily,
                                    fontSize = 13.sp,
                                    color = colors.secondaryText
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "• ${strings.buyer.followersCountTemplate.format(currentShop.followersCount)}",
                                    fontFamily = SuqiFontFamily,
                                    fontSize = 12.sp,
                                    color = colors.mutedText
                                )
                            }
                        }

                        if (currentShop.isVerified) {
                            SuqiStatusBadge(strings.verifiedLabel, StatusType.SUCCESS)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = currentShop.description,
                        fontFamily = SuqiFontFamily,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = colors.secondaryText
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.requireSignedInOrPrompt(strings.extras.gateFollowShops) {
                                    coroutineScope.launch {
                                        viewModel.repository.toggleFollowShop(currentShop.id)
                                        val refreshed = viewModel.repository.getShopDetails(currentShop.id)
                                        if (refreshed.success && refreshed.data != null) {
                                            shop = refreshed.data
                                        }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, SuqiPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Icon(
                                imageVector = if (currentShop.isFollowing) LucideIcons.HeartFilled else LucideIcons.Heart,
                                contentDescription = null,
                                tint = SuqiPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (currentShop.isFollowing) strings.buyer.followingLabel else strings.buyer.followLabel,
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                color = SuqiPrimary
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.requireSignedInOrPrompt(strings.extras.gateChatSellers) {
                                    viewModel.navigate(ScreenDestination.ChatThread(currentShop.id))
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Icon(
                                imageVector = LucideIcons.MessageCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.buyer.chatTabLabel,
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SuqiSearchField(
                        value = shopSearchQuery,
                        onValueChange = { shopSearchQuery = it },
                        placeholder = strings.checkout.searchProductsHint,
                        testTag = "shop_storefront_search"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        TabLabel(
                            title = "${strings.productsTab} (${products.size})",
                            isSelected = selectedTab == 0,
                            onClick = { selectedTab = 0 }
                        )
                        TabLabel(
                            title = strings.buyer.collectionsTabLabel,
                            isSelected = selectedTab == 1,
                            onClick = { selectedTab = 1 }
                        )
                        TabLabel(
                            title = "${strings.customerReviews} (${reviews.size})",
                            isSelected = selectedTab == 2,
                            onClick = { selectedTab = 2 }
                        )
                    }

                    if (activeCollectionFilter != null && selectedTab == 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(colors.primaryTint)
                                .clickable { activeCollectionFilter = null }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${strings.buyer.collectionsTabLabel}: ${collectionDisplayTitle(activeCollectionFilter!!)}",
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = SuqiPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(LucideIcons.X, contentDescription = strings.common.clearSearchDesc, tint = SuqiPrimary, modifier = Modifier.size(14.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            if (selectedTab == 0) {
                val collectionFiltered = when (activeCollectionFilter) {
                    "Best Sellers" -> products.sortedByDescending { it.rating }
                    "Discount Deals" -> products.filter { it.discountPercent > 0 }
                    "In Stock Ready" -> products.filter { it.stockQuantity > 0 }
                    "Premium Picks" -> products.filter { it.price >= 5000.0 }.ifEmpty { products }
                    else -> products
                }
                val filteredProducts = if (shopSearchQuery.isBlank()) {
                    collectionFiltered
                } else {
                    collectionFiltered.filter {
                        it.localizedName(lang).contains(shopSearchQuery, ignoreCase = true) ||
                            it.name.contains(shopSearchQuery, ignoreCase = true) ||
                            it.nameAmharic.contains(shopSearchQuery, ignoreCase = true) ||
                            it.nameAfaanOromo.contains(shopSearchQuery, ignoreCase = true)
                    }
                }

                if (filteredProducts.isEmpty()) {
                    item {
                        SuqiEmptyState(
                            icon = LucideIcons.ShoppingBag,
                            title = strings.buyer.noMatchingProducts,
                            subtitle = strings.buyer.noMatchingProductsSub,
                            ctaText = strings.sellerOps.showAllProductsBtn,
                            onCtaClick = {
                                shopSearchQuery = ""
                                activeCollectionFilter = null
                            }
                        )
                    }
                } else {
                    val chunks = filteredProducts.chunked(2)
                    items(chunks) { chunk ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (prod in chunk) {
                                Box(modifier = Modifier.weight(1f)) {
                                    SuqiProductCard(
                                        product = prod,
                                        onClick = { viewModel.navigate(ScreenDestination.ProductDetails(prod.id)) },
                                        onAddToCart = {
                                            viewModel.requireSignedInOrPrompt(strings.addToCart) {
                                                viewModel.quickAddToCart(prod)
                                            }
                                        },
                                        onToggleWishlist = {
                                            viewModel.requireSignedInOrPrompt(strings.extras.gateSaveWishlist) {
                                                coroutineScope.launch {
                                                    viewModel.repository.toggleWishlist(prod.id)
                                                }
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
            } else if (selectedTab == 1) {
                val collections = listOf(
                    Triple("Best Sellers", strings.popularNow, products.firstOrNull()?.primaryImageUrl ?: currentShop.coverUrl),
                    Triple("Discount Deals", strings.hotDeals, products.firstOrNull { it.discountPercent > 0 }?.primaryImageUrl ?: currentShop.coverUrl),
                    Triple("In Stock Ready", strings.inStock, products.firstOrNull { it.stockQuantity > 0 }?.primaryImageUrl ?: currentShop.logoUrl),
                    Triple("Premium Picks", strings.recommendedForYou, products.maxByOrNull { it.price }?.primaryImageUrl ?: currentShop.coverUrl)
                )

                items(collections) { (key, title, img) ->
                    SuqiSectionCard(
                        padding = 14.dp,
                        onClick = {
                            activeCollectionFilter = key
                            selectedTab = 0
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(colors.imageBackdrop)
                            ) {
                                AsyncImage(
                                    model = img,
                                    contentDescription = title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = colors.primaryText
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = strings.seeAllArrow,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = SuqiPrimary
                                )
                            }
                        }
                    }
                }
            } else {
                if (reviews.isEmpty()) {
                    item {
                        SuqiEmptyState(
                            icon = LucideIcons.Star,
                            title = strings.buyer.noReviewsTitle,
                            subtitle = strings.buyer.noReviewsSub
                        )
                    }
                } else {
                    items(reviews, key = { it.id }) { rev ->
                        ReviewItemCard(review = rev)
                    }
                }
            }
        }
    }

    if (showShareSheet) {
        val shareUrl = "https://suqi.et/shop/${currentShop.slug.ifBlank { currentShop.id }}"
        SuqiBottomSheet(
            onDismissRequest = { showShareSheet = false },
            title = strings.checkout.shareShopTitle
        ) {
            SuqiSectionCard(containerColor = colors.secondaryBackground, padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = currentShop.logoUrl,
                        contentDescription = currentShop.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(currentShop.name, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, color = colors.primaryText)
                        Text("${placeLabel(currentShop.subCity)}, ${placeLabel(currentShop.city)} · ${currentShop.rating}★", fontSize = 12.sp, color = colors.secondaryText)
                        Text(shareUrl, fontSize = 11.sp, color = SuqiPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SuqiButton(
                    text = strings.common.copyLinkLabel,
                    isPrimary = false,
                    icon = LucideIcons.Copy,
                    onClick = {
                        clipboardManager.setText(AnnotatedString(shareUrl))
                        showShareSheet = false
                        viewModel.showToast(strings.common.copiedToast)
                    },
                    modifier = Modifier.weight(1f)
                )
                SuqiButton(
                    text = strings.shareProduct,
                    icon = LucideIcons.Share2,
                    onClick = {
                        showShareSheet = false
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, currentShop.name)
                            putExtra(Intent.EXTRA_TEXT, "Check out ${currentShop.name} on Suqi: $shareUrl")
                        }
                        context.startActivity(Intent.createChooser(intent, currentShop.name))
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun TabLabel(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalSuqiColors.current
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(bottom = 6.dp)
    ) {
        Text(
            text = title,
            fontFamily = SuqiFontFamily,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 15.sp,
            color = if (isSelected) SuqiPrimary else colors.secondaryText
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (isSelected) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(SuqiPrimary)
            )
        }
    }
}

@Composable
fun ReviewItemCard(review: ReviewDto) {
    val colors = LocalSuqiColors.current
    SuqiSectionCard(
        padding = 14.dp,
        containerColor = colors.secondaryBackground,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = review.userName,
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = colors.primaryText
            )
            Text(
                text = review.date,
                fontFamily = SuqiFontFamily,
                fontSize = 12.sp,
                color = colors.mutedText
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            for (i in 1..5) {
                Icon(
                    imageVector = LucideIcons.StarFilled,
                    contentDescription = null,
                    tint = if (i <= review.rating) SuqiWarning else colors.border,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = review.comment,
            fontFamily = SuqiFontFamily,
            fontSize = 13.sp,
            color = colors.primaryText
        )
    }
}
