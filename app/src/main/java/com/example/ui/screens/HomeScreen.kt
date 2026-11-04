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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.CategoryDto
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
    val colors = LocalSuqiColors.current

    var products by remember { mutableStateOf<List<ProductDto>>(emptyList()) }
    var shops by remember { mutableStateOf<List<ShopDto>>(emptyList()) }
    var categories by remember { mutableStateOf<List<CategoryDto>>(emptyList()) }
    var showLocationSheet by remember { mutableStateOf(false) }

    // FitHub-style carousel slides (theme colors applied at render)
    data class BannerSlide(
        val id: String,
        val title: String,
        val subtitle: String,
        val buttonText: String,
        val imageUrl: String,
        val targetCategory: String,
        val targetName: String
    )
    val slides = remember {
        listOf(
            BannerSlide(
                id = "1",
                title = "Discover\nAmazing Deals",
                subtitle = "Shop Smart. Shop Suqi — premium products across Ethiopia.",
                buttonText = "Shop Now",
                imageUrl = "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600",
                targetCategory = "cat_electronics",
                targetName = "Electronics"
            ),
            BannerSlide(
                id = "2",
                title = "Ethiopian\nFashion Week",
                subtitle = "Handcrafted styles and modern looks from local sellers.",
                buttonText = "Explore Style",
                imageUrl = "https://images.unsplash.com/photo-1523381294911-8d3cead13475?w=600",
                targetCategory = "cat_fashion",
                targetName = "Fashion"
            ),
            BannerSlide(
                id = "3",
                title = "Transform\nYour Home",
                subtitle = "Furniture and living essentials delivered across Ethiopia.",
                buttonText = "View Decor",
                imageUrl = "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=600",
                targetCategory = "cat_home",
                targetName = "Home & Living"
            )
        )
    }

        val pagerState = rememberPagerState(pageCount = { slides.size })

    // Auto-advance banner slides smoothly
    LaunchedEffect(pagerState) {
        while (true) {
            kotlinx.coroutines.delay(4500)
            if (!pagerState.isScrollInProgress) {
                val next = (pagerState.currentPage + 1) % slides.size
                pagerState.animateScrollToPage(next)
            }
        }
    }

    LaunchedEffect(Unit) {
        val prodRes = viewModel.repository.getProducts()
        if (prodRes.success && prodRes.data != null) {
            products = prodRes.data
        }
        val shopRes = viewModel.repository.getShops()
        if (shopRes.success && shopRes.data != null) {
            shops = shopRes.data
        }
        val catRes = viewModel.repository.getCategories()
        if (catRes.success && catRes.data != null) {
            categories = catRes.data
        }
    }

    val isDark by viewModel.isDarkMode.collectAsState()
    val effectiveDark = isDark ?: androidx.compose.foundation.isSystemInDarkTheme()

    Scaffold(
        topBar = {
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
        },
        containerColor = colors.surfaceBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
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
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.secondaryBackground)
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

            // Promotional Hero Banner Carousel (FitHub reference styling)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        pageSpacing = 14.dp
                    ) { page ->
                        val slide = slides[page]
                        // 3D page-turn rotation based on scroll offset (FitHub-like depth)
                        val pageOffset = (
                            (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                        )
                        val rotationY = pageOffset * -25f
                        val scale = 0.92f + (1f - kotlin.math.abs(pageOffset).coerceIn(0f, 1f)) * 0.08f

                        val gradient = if (effectiveDark) {
                            listOf(Color(0xFF2A1F18), Color(0xFF1A1410))
                        } else {
                            listOf(Color(0xFFFFF3EB), Color(0xFFFFE5D3))
                        }
                        val titleCol = if (effectiveDark) Color(0xFFF8FAFC) else Color(0xFF171717)
                        val subCol = if (effectiveDark) Color(0xFF94A3B8) else Color(0xFF737373)

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    this.rotationY = rotationY
                                    cameraDistance = 16f * density
                                    scaleX = scale
                                    scaleY = scale
                                    alpha = 0.55f + (1f - kotlin.math.abs(pageOffset).coerceIn(0f, 1f)) * 0.45f
                                }
                                .clip(RoundedCornerShape(28.dp))
                                .background(Brush.horizontalGradient(gradient))
                                .clickable {
                                    viewModel.navigate(
                                        ScreenDestination.CategoryListing(slide.targetCategory, slide.targetName)
                                    )
                                }
                                .testTag("banner_slide_${slide.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(start = 22.dp, end = 8.dp, top = 22.dp, bottom = 22.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1.15f)
                                        .fillMaxHeight(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = slide.title,
                                            fontFamily = SuqiFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 24.sp,
                                            lineHeight = 30.sp,
                                            color = titleCol
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = slide.subtitle,
                                            fontFamily = SuqiFontFamily,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp,
                                            color = subCol,
                                            maxLines = 2
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(SuqiPrimary)
                                            .clickable {
                                                viewModel.navigate(
                                                    ScreenDestination.CategoryListing(
                                                        slide.targetCategory,
                                                        slide.targetName
                                                    )
                                                )
                                            }
                                            .padding(horizontal = 18.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = slide.buttonText,
                                            fontFamily = SuqiFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = LucideIcons.ArrowRight,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(slide.imageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = slide.title,
                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // FitHub-style pill indicators
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(slides.size) { index ->
                            val isSelected = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .height(6.dp)
                                    .width(if (isSelected) 20.dp else 6.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (isSelected) SuqiPrimary else colors.border)
                            )
                        }
                    }
                }
            }

            // Categories Shortcuts Row (consistent line icons)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CategoryShortcutItem(
                        title = "Electronics",
                        icon = LucideIcons.Laptop,
                        onClick = { viewModel.navigate(ScreenDestination.CategoryListing("cat_electronics", "Electronics")) }
                    )
                    CategoryShortcutItem(
                        title = "Fashion",
                        icon = LucideIcons.Shirt,
                        onClick = { viewModel.navigate(ScreenDestination.CategoryListing("cat_fashion", "Fashion")) }
                    )
                    CategoryShortcutItem(
                        title = "Home & Living",
                        icon = LucideIcons.House,
                        onClick = { viewModel.navigate(ScreenDestination.CategoryListing("cat_home", "Home & Living")) }
                    )
                    CategoryShortcutItem(
                        title = "Beauty",
                        icon = LucideIcons.Sparkles,
                        onClick = { viewModel.navigate(ScreenDestination.CategoryListing("cat_beauty", "Beauty & Care")) }
                    )
                    CategoryShortcutItem(
                        title = "More",
                        icon = LucideIcons.LayoutGrid,
                        onClick = { viewModel.navigate(ScreenDestination.Categories) }
                    )
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
                        contentPadding = PaddingValues(horizontal = 16.dp),
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
                            color = SuqiPrimaryText
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
fun CategoryShortcutItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(SuqiPrimaryTint),
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
            fontSize = 12.sp,
            color = SuqiPrimaryText,
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
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SuqiSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(SuqiBorder)
        ),
        modifier = Modifier
            .width(170.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(SuqiSecondaryBackground)
            ) {
                AsyncImage(
                    model = shop.logoUrl,
                    contentDescription = shop.name,
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

    val regions = listOf("Addis Ababa", "Oromia", "Amhara", "Dire Dawa", "Sidama")
    val subCitiesAddis = listOf("Bole", "Kirkos", "Yeka", "Arada", "Lideta", "Nifas Silk-Lafto")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Select Your Location",
            fontFamily = SuqiFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = SuqiPrimaryText
        )
        Text(
            text = "See products, shops and delivery fees tailored to your area.",
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

        Text(
            text = "Sub-City / District",
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

        Spacer(modifier = Modifier.height(24.dp))

        SuqiButton(
            text = "Set Location",
            onClick = { onSelectLocation(selectedRegion, selectedCity, selectedSubCity) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
