package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.CategoryDto
import com.example.data.model.MarketplaceCategories
import com.example.data.model.ProductDto
import com.example.data.model.ShopDto
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val cartCount by viewModel.cartCount.collectAsState(initial = 0)
    val unreadNotifCount by viewModel.unreadNotificationCount.collectAsState()
    val selectedCity by viewModel.selectedCity.collectAsState()
    val selectedSubCity by viewModel.selectedSubCity.collectAsState()
    val recentlyViewed by viewModel.recentlyViewed.collectAsState()
    val colors = LocalSuqiColors.current
    val isOffline by viewModel.isOffline.collectAsState()

    var products by remember { mutableStateOf<List<ProductDto>>(emptyList()) }
    var shops by remember { mutableStateOf<List<ShopDto>>(emptyList()) }
    var categories by remember { mutableStateOf(MarketplaceCategories.all.map { it.toDto() }) }
    var liveBanners by remember { mutableStateOf<List<PromoBanner>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showLocationSheet by remember { mutableStateOf(false) }

    val fallbackBanners = remember {
        listOf(
            PromoBanner(
                id = "1",
                badge = "Hot Deal",
                badgeIcon = LucideIcons.Flame,
                title = "Up to 50% Off",
                highlight = "50%",
                subtitle = "On Selected Items",
                buttonText = "Shop Now",
                imageUrl = "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600",
                targetCategory = "cat_electronics",
                targetName = "Electronics"
            )
        )
    }
    val banners = if (liveBanners.isNotEmpty()) liveBanners else fallbackBanners

    fun loadHomeData() {
        coroutineScope.launch {
            isLoading = products.isEmpty()
            val bannerRes = viewModel.repository.getPromoBanners(activeOnly = true)
            if (bannerRes.success && bannerRes.data != null) {
                liveBanners = bannerRes.data.map { b ->
                    PromoBanner(
                        id = b.id,
                        badge = b.badge,
                        badgeIcon = when {
                            b.badge.contains("Hot", ignoreCase = true) -> LucideIcons.Flame
                            b.badge.contains("Home", ignoreCase = true) -> LucideIcons.House
                            else -> LucideIcons.Sparkles
                        },
                        title = b.title,
                        highlight = b.highlight,
                        subtitle = b.subtitle,
                        buttonText = b.buttonText,
                        imageUrl = b.imageUrl,
                        targetCategory = b.targetCategory,
                        targetName = b.targetName
                    )
                }
            }
            val prodRes = viewModel.repository.getProducts()
            if (prodRes.success && prodRes.data != null) {
                products = prodRes.data
            }
            val shopRes = viewModel.repository.getShops()
            if (shopRes.success && shopRes.data != null) {
                shops = shopRes.data
            }
            val catRes = viewModel.repository.getCategories()
            if (catRes.success && !catRes.data.isNullOrEmpty()) {
                categories = catRes.data
                    .filter { it.isActive }
                    .sortedWith(compareBy({ it.sortOrder }, { it.name }))
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadHomeData()
    }

    val isDark by viewModel.isDarkMode.collectAsState()
    val effectiveDark = isDark ?: androidx.compose.foundation.isSystemInDarkTheme()

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                SuqiTopHeader(
                    cartCount = cartCount,
                    notificationCount = unreadNotifCount,
                    onCartClick = { viewModel.navigate(ScreenDestination.Cart) },
                    onNotificationClick = {
                        viewModel.clearNotificationCount()
                        viewModel.navigate(ScreenDestination.Notifications)
                    },
                    showThemeToggle = true,
                    isDarkTheme = effectiveDark,
                    onToggleTheme = { viewModel.setDarkMode(!effectiveDark) },
                    locationText = listOfNotNull(selectedSubCity.takeIf { it.isNotBlank() }, selectedCity).joinToString(", ").ifBlank { selectedCity },
                    onLocationClick = { showLocationSheet = true }
                )
                if (isOffline) {
                    SuqiOfflineBanner(onRetry = {
                        viewModel.setOfflineMode(false)
                        loadHomeData()
                    })
                }
            }
        },
        containerColor = if (colors.isDark) colors.surfaceBackground else SuqiCream,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        if (isLoading && products.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(top = 16.dp)) {
                SuqiSkeletonProductGrid(count = 4)
            }
        } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = SuqiBottomNavContentPadding)
        ) {
            // Location row (mockup: under logo)
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .clickable { showLocationSheet = true }
                        .testTag("home_location")
                ) {
                    Icon(
                        imageVector = LucideIcons.MapPin,
                        contentDescription = null,
                        tint = SuqiPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = listOfNotNull(
                            selectedSubCity.takeIf { it.isNotBlank() },
                            selectedCity.takeIf { it.isNotBlank() }
                        ).joinToString(", ").ifBlank { selectedCity.ifBlank { "Addis Ababa" } },
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = colors.primaryText
                    )
                    Icon(
                        imageVector = LucideIcons.ChevronDown,
                        contentDescription = null,
                        tint = colors.secondaryText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Search Bar Button (idle entry to SearchScreen)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .suqiWarmShadow(RoundedCornerShape(20.dp), elevation = 6.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.cardBackground)
                        .clickable { viewModel.navigate(ScreenDestination.Search) }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .testTag("home_search_bar")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = LucideIcons.Search,
                            contentDescription = "Search",
                            tint = colors.secondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Search products or shops...",
                            fontFamily = SuqiFontFamily,
                            fontSize = 15.sp,
                            color = colors.mutedText
                        )
                    }
                }
            }

            // Promotional carousel (full-width, Suqi UI direction)
            item {
                SuqiPromoCarousel(
                    banners = banners,
                    onBannerClick = { banner ->
                        viewModel.navigate(
                            ScreenDestination.CategoryListing(banner.targetCategory, banner.targetName)
                        )
                    }
                )
            }

            // Categories Shortcuts Row (live categories with CDN thumbnails + All Categories link)
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(categories, key = { it.id }) { cat ->
                        CategoryImageShortcutItem(
                            title = cat.name,
                            imageUrl = cat.imageUrl.ifBlank { MarketplaceCategories.imageUrlForId(cat.id) },
                            onClick = { viewModel.navigate(ScreenDestination.CategoryListing(cat.id, cat.name)) },
                            testTag = "home_cat_${cat.id}"
                        )
                    }
                    item(key = "all_categories_shortcut") {
                        CategoryShortcutItem(
                            title = "All",
                            icon = LucideIcons.LayoutGrid,
                            onClick = { viewModel.navigate(ScreenDestination.Categories) }
                        )
                    }
                }
            }

            // Featured Shops Section
            item {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Featured Shops",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            color = SuqiPrimaryText
                        )
                        Text(
                            text = "See all →",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = SuqiPrimary,
                            modifier = Modifier.clickable { viewModel.navigate(ScreenDestination.Shops) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(shops) { shop ->
                            FeaturedShopCard(
                                shop = shop,
                                onClick = { viewModel.navigate(ScreenDestination.ShopStorefront(shop.id)) }
                            )
                        }
                    }
                }
            }

            // New Arrivals Section (Plan §19 Screen 01)
            item {
                Column(modifier = Modifier.padding(top = 22.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "New Arrivals",
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp,
                                color = colors.primaryText
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SuqiPrimary)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "NEW",
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = Color.White
                                )
                            }
                        }
                        Text(
                            text = "See all →",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = SuqiPrimary,
                            modifier = Modifier.clickable {
                                viewModel.navigate(ScreenDestination.CategoryListing("all", "New Arrivals"))
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(products.reversed().take(6)) { prod ->
                            Box(modifier = Modifier.width(160.dp)) {
                                SuqiProductCard(
                                    product = prod,
                                    onClick = { viewModel.navigate(ScreenDestination.ProductDetails(prod.id)) },
                                    onAddToCart = { viewModel.quickAddToCart(prod) },
                                    onShopClick = { viewModel.navigate(ScreenDestination.ShopStorefront(prod.shopId)) },
                                    onToggleWishlist = {
                                        coroutineScope.launch {
                                            viewModel.repository.toggleWishlist(prod.id)
                                            val refreshed = viewModel.repository.getProducts()
                                            if (refreshed.success && refreshed.data != null) {
                                                products = refreshed.data
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Popular Products Section
            item {
                Column(modifier = Modifier.padding(top = 24.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Popular Products",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            color = colors.primaryText
                        )
                        Text(
                            text = "See all →",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = SuqiPrimary,
                            modifier = Modifier.clickable {
                                viewModel.navigate(ScreenDestination.CategoryListing("all", "All Products"))
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Product Grid items (2 columns)
            val productChunks = products.chunked(2)
            items(productChunks) { chunk ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (product in chunk) {
                        Box(modifier = Modifier.weight(1f)) {
                            SuqiProductCard(
                                product = product,
                                onClick = { viewModel.navigate(ScreenDestination.ProductDetails(product.id)) },
                                onAddToCart = { viewModel.quickAddToCart(product) },
                                onShopClick = { viewModel.navigate(ScreenDestination.ShopStorefront(product.shopId)) },
                                onToggleWishlist = {
                                    coroutineScope.launch {
                                        viewModel.repository.toggleWishlist(product.id)
                                        val refreshed = viewModel.repository.getProducts()
                                        if (refreshed.success && refreshed.data != null) {
                                            products = refreshed.data
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

            // Recommended for You Section (Plan §19 Screen 01)
            item {
                Column(modifier = Modifier.padding(top = 24.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(LucideIcons.Sparkles, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Recommended for You",
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp,
                                color = colors.primaryText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(products.take(6)) { prod ->
                            Box(modifier = Modifier.width(160.dp)) {
                                SuqiProductCard(
                                    product = prod,
                                    onClick = { viewModel.navigate(ScreenDestination.ProductDetails(prod.id)) },
                                    onAddToCart = { viewModel.quickAddToCart(prod) },
                                    onShopClick = { viewModel.navigate(ScreenDestination.ShopStorefront(prod.shopId)) },
                                    onToggleWishlist = {
                                        coroutineScope.launch {
                                            viewModel.repository.toggleWishlist(prod.id)
                                            val refreshed = viewModel.repository.getProducts()
                                            if (refreshed.success && refreshed.data != null) {
                                                products = refreshed.data
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Recently Viewed Products Section (Plan §19 & P3 Operational Features)
            if (recentlyViewed.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(top = 24.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(LucideIcons.History, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Recently Viewed",
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 18.sp,
                                    color = colors.primaryText
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(recentlyViewed) { prod ->
                                Box(modifier = Modifier.width(160.dp)) {
                                    SuqiProductCard(
                                        product = prod,
                                        onClick = { viewModel.navigate(ScreenDestination.ProductDetails(prod.id)) },
                                        onAddToCart = { viewModel.quickAddToCart(prod) },
                                        onShopClick = { viewModel.navigate(ScreenDestination.ShopStorefront(prod.shopId)) },
                                        onToggleWishlist = {
                                            coroutineScope.launch {
                                                viewModel.repository.toggleWishlist(prod.id)
                                            }
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
    }

    // Location Picker Bottom Sheet (Reusable as specified in Checklist Item 08)
    if (showLocationSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLocationSheet = false },
            containerColor = SuqiSurface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            LocationPickerContent(
                currentRegion = viewModel.selectedRegion.value,
                currentCity = viewModel.selectedCity.value,
                currentSubCity = viewModel.selectedSubCity.value,
                onSelectLocation = { region, city, subCity ->
                    viewModel.selectedRegion.value = region
                    viewModel.selectedCity.value = city
                    viewModel.selectedSubCity.value = subCity
                    showLocationSheet = false
                }
            )
        }
    }
}

@Composable
fun CategoryImageShortcutItem(
    title: String,
    imageUrl: String,
    onClick: () -> Unit,
    testTag: String = ""
) {
    val colors = LocalSuqiColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(68.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
            .let { if (testTag.isNotBlank()) it.testTag(testTag) else it }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(colors.imageBackdrop)
                .border(1.dp, colors.border, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
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

@Composable
fun CategoryShortcutItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val colors = LocalSuqiColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(64.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(colors.primaryTint),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = SuqiPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
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

@Composable
fun FeaturedShopCard(
    shop: ShopDto,
    onClick: () -> Unit
) {
    val isDark = LocalSuqiColors.current.isDark
    val shape = RoundedCornerShape(SuqiRadius.Card)
    val tint = if (isDark) SuqiShopTintDark else SuqiShopTintLight
    val outline = if (isDark) SuqiShopBorderDark else SuqiShopBorderLight
    Box(
        modifier = Modifier
            .width(170.dp)
            .suqiWarmShadow(shape, elevation = 8.dp)
            .clip(shape)
            .background(tint)
            .border(1.dp, outline, shape)
            .clickable(onClick = onClick)
            .testTag("featured_shop_${shop.id}")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(3.dp)
                    .clip(CircleShape)
            ) {
                AsyncImage(
                    model = shop.logoUrl,
                    contentDescription = shop.name,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = shop.name,
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = SuqiPrimaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (shop.isVerified) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = LucideIcons.CheckCircle,
                        contentDescription = "Verified",
                        tint = SuqiPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = LucideIcons.StarFilled,
                    contentDescription = null,
                    tint = SuqiWarning,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "${shop.rating} (${shop.reviewCount})",
                    fontFamily = SuqiFontFamily,
                    fontSize = 11.sp,
                    color = SuqiSecondaryText
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = shop.address,
                fontFamily = SuqiFontFamily,
                fontSize = 11.sp,
                color = SuqiMutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun LocationPickerContent(
    currentRegion: String,
    currentCity: String,
    currentSubCity: String,
    onSelectLocation: (String, String, String) -> Unit
) {
    var selectedRegion by remember { mutableStateOf(currentRegion) }
    var selectedCity by remember { mutableStateOf(currentCity) }
    var selectedSubCity by remember { mutableStateOf(currentSubCity) }

    val regions = com.example.util.EthiopianMarketplaceUtils.ETHIOPIA_REGIONS
    val subCitiesAddis = com.example.util.EthiopianMarketplaceUtils.ADDIS_ABABA_SUB_CITIES
    val regionalHubs = com.example.util.EthiopianMarketplaceUtils.REGIONAL_COMMERCIAL_HUBS

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Select Your Location",
            fontFamily = SuqiFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = SuqiPrimaryText
        )
        Text(
            text = "See products, shops and delivery fees tailored to your area across Ethiopia.",
            fontFamily = SuqiFontFamily,
            fontSize = 13.sp,
            color = SuqiSecondaryText
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Region",
            fontFamily = SuqiFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(regions) { reg ->
                val isSel = reg == selectedRegion
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isSel) SuqiPrimaryTint else SuqiSecondaryBackground)
                        .border(1.dp, if (isSel) SuqiPrimary else SuqiBorder, RoundedCornerShape(999.dp))
                        .clickable {
                            selectedRegion = reg
                            selectedCity = if (reg == "Addis Ababa") "Addis Ababa" else "Adama"
                            if (reg == "Addis Ababa") {
                                selectedSubCity = "Bole"
                            } else {
                                selectedSubCity = "Center"
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = reg,
                        fontFamily = SuqiFontFamily,
                        fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 13.sp,
                        color = if (isSel) SuqiPrimary else SuqiPrimaryText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedRegion == "Addis Ababa") {
            Text(
                text = "Addis Ababa Sub-City (All 11 Kifle Ketemas)",
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(subCitiesAddis) { sc ->
                    val isSel = sc == selectedSubCity
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (isSel) SuqiPrimaryTint else SuqiSecondaryBackground)
                            .border(1.dp, if (isSel) SuqiPrimary else SuqiBorder, RoundedCornerShape(999.dp))
                            .clickable { selectedSubCity = sc }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = sc,
                            fontFamily = SuqiFontFamily,
                            fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 13.sp,
                            color = if (isSel) SuqiPrimary else SuqiPrimaryText
                        )
                    }
                }
            }
        } else {
            Text(
                text = "Commercial Hub City",
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(regionalHubs) { hub ->
                    val isSel = hub == selectedCity
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (isSel) SuqiPrimaryTint else SuqiSecondaryBackground)
                            .border(1.dp, if (isSel) SuqiPrimary else SuqiBorder, RoundedCornerShape(999.dp))
                            .clickable {
                                selectedCity = hub
                                selectedSubCity = "Center"
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = hub,
                            fontFamily = SuqiFontFamily,
                            fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 13.sp,
                            color = if (isSel) SuqiPrimary else SuqiPrimaryText
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SuqiButton(
            text = "Set Location",
            onClick = { onSelectLocation(selectedRegion, selectedCity, selectedSubCity) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
