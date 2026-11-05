package com.example.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ProductDto
import com.example.data.model.ReviewDto
import com.example.data.model.ShopDto
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ShopsScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val cartCount by viewModel.cartCount.collectAsState(initial = 0)

    var shops by remember { mutableStateOf<List<ShopDto>>(emptyList()) }
    var selectedFilter by remember { mutableStateOf("All") }
    var selectedCategory by remember { mutableStateOf("All Categories") }

    val filterOptions = listOf("All", "Verified", "Top Rated", "Following", "Nearby")
    val categoryOptions = listOf("All Categories", "Electronics", "Fashion", "Home & Living", "Beauty & Care", "Groceries", "Sports")

    fun loadShops() {
        coroutineScope.launch {
            val res = viewModel.repository.getShops(filter = selectedFilter.uppercase().replace(" ", "_"))
            if (res.success && res.data != null) {
                shops = res.data
            }
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

    Scaffold(
        topBar = {
            SuqiTopHeader(
                title = "Shops",
                cartCount = cartCount,
                onCartClick = { viewModel.navigate(ScreenDestination.Cart) },
                actions = {
                    IconButton(
                        onClick = { viewModel.navigate(ScreenDestination.Search) },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SuqiSecondaryBackground)
                    ) {
                        Icon(
                            imageVector = LucideIcons.Search,
                            contentDescription = "Search",
                            tint = SuqiPrimaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
            )
        },
        containerColor = SuqiBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = SuqiBottomNavContentPadding)
        ) {
            // Category Filter Pills Row (Checklist 04 & 11)
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categoryOptions) { cat ->
                        val isSel = cat == selectedCategory
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (isSel) SuqiPrimary else SuqiSecondaryBackground)
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = cat,
                                fontFamily = SuqiFontFamily,
                                fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 12.sp,
                                color = if (isSel) Color.White else SuqiPrimaryText
                            )
                        }
                    }
                }
            }

            // Status Filter Pills Row (Checklist 04)
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filterOptions) { filter ->
                        val isSel = filter == selectedFilter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (isSel) LocalSuqiColors.current.primaryTint else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (isSel) SuqiPrimary else SuqiBorder,
                                    RoundedCornerShape(999.dp)
                                )
                                .clickable { selectedFilter = filter }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = filter,
                                fontFamily = SuqiFontFamily,
                                fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 12.sp,
                                color = if (isSel) SuqiPrimary else SuqiSecondaryText
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (filteredShops.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No shops found matching your filters.",
                            fontFamily = SuqiFontFamily,
                            color = SuqiSecondaryText
                        )
                    }
                }
            } else {
                items(filteredShops) { shop ->
                    ShopListItemRow(
                        shop = shop,
                        onClick = { viewModel.navigate(ScreenDestination.ShopStorefront(shop.id)) },
                        onToggleFollow = {
                            coroutineScope.launch {
                                viewModel.repository.toggleFollowShop(shop.id)
                                loadShops()
                            }
                        }
                    )
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
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SuqiSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(SuqiBorder)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SuqiSecondaryBackground)
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
                        color = SuqiSecondaryText
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•",
                        fontSize = 11.sp,
                        color = SuqiMutedText
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${shop.productCount} Products",
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = SuqiPrimary
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "${shop.subCity}, ${shop.city} • ${shop.category}",
                    fontFamily = SuqiFontFamily,
                    fontSize = 12.sp,
                    color = SuqiMutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Follow button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (shop.isFollowing) SuqiSecondaryBackground else SuqiPrimaryTint)
                    .clickable(onClick = onToggleFollow)
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = if (shop.isFollowing) "Following" else "Follow",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = if (shop.isFollowing) SuqiSecondaryText else SuqiPrimary
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
    var shop by remember { mutableStateOf<ShopDto?>(null) }
    var products by remember { mutableStateOf<List<ProductDto>>(emptyList()) }
    var reviews by remember { mutableStateOf<List<ReviewDto>>(emptyList()) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Products, 1: Collections, 2: Reviews
    var shopSearchQuery by remember { mutableStateOf("") }

    LaunchedEffect(shopId) {
        val sRes = viewModel.repository.getShopDetails(shopId)
        if (sRes.success && sRes.data != null) {
            shop = sRes.data
        }
        val pRes = viewModel.repository.getProducts(shopId = shopId)
        if (pRes.success && pRes.data != null) {
            products = pRes.data
        }
        val rRes = viewModel.repository.getReviews(targetId = shopId)
        if (rRes.success && rRes.data != null) {
            reviews = rRes.data
        }
    }

    val currentShop = shop ?: return Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) { CircularProgressIndicator(color = SuqiPrimary) }

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
                IconButton(
                    onClick = { viewModel.goBack() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(LocalSuqiColors.current.cardBackground)
                ) {
                    Icon(LucideIcons.ArrowLeft, contentDescription = "Back", tint = SuqiPrimaryText)
                }

                Row {
                    IconButton(
                        onClick = { viewModel.showToast("Storefront link copied!") },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(LocalSuqiColors.current.cardBackground)
                    ) {
                        Icon(LucideIcons.Share2, contentDescription = "Share", tint = SuqiPrimaryText)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { viewModel.navigate(ScreenDestination.Cart) },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(LocalSuqiColors.current.cardBackground)
                    ) {
                        Icon(LucideIcons.ShoppingCart, contentDescription = "Cart", tint = SuqiPrimaryText)
                    }
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
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Cover Image & Logo
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    AsyncImage(
                        model = currentShop.coverUrl,
                        contentDescription = currentShop.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Logo circle overlapping bottom
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .align(Alignment.BottomStart)
                            .offset(x = 16.dp, y = 30.dp)
                            .clip(CircleShape)
                            .border(3.dp, LocalSuqiColors.current.cardBackground, CircleShape)
                            .background(LocalSuqiColors.current.cardBackground),
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

                Spacer(modifier = Modifier.height(38.dp))
            }

            // Shop Information
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentShop.name,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 20.sp,
                                    color = SuqiPrimaryText
                                )
                                if (currentShop.isVerified) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = LucideIcons.CheckCircle,
                                        contentDescription = "Verified",
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
                                    text = "${currentShop.rating} (${currentShop.reviewCount} reviews)",
                                    fontFamily = SuqiFontFamily,
                                    fontSize = 13.sp,
                                    color = SuqiSecondaryText
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "• ${currentShop.address}",
                                    fontFamily = SuqiFontFamily,
                                    fontSize = 12.sp,
                                    color = SuqiMutedText
                                )
                            }
                        }

                        if (currentShop.isVerified) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(SuqiSuccessBg)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Verified",
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = SuqiSuccess
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = currentShop.description,
                        fontFamily = SuqiFontFamily,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = SuqiSecondaryText
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Follow and Chat Actions Row (Mockup 2)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.repository.toggleFollowShop(currentShop.id)
                                    val refreshed = viewModel.repository.getShopDetails(currentShop.id)
                                    if (refreshed.success && refreshed.data != null) {
                                        shop = refreshed.data
                                    }
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SuqiPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(
                                imageVector = if (currentShop.isFollowing) LucideIcons.HeartFilled else LucideIcons.Heart,
                                contentDescription = null,
                                tint = SuqiPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (currentShop.isFollowing) "Following" else "Follow",
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                color = SuqiPrimary
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.navigate(ScreenDestination.ChatThread(currentShop.id))
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(
                                imageVector = LucideIcons.MessageCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Chat",
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Shop In-Store Search Bar
                    OutlinedTextField(
                        value = shopSearchQuery,
                        onValueChange = { shopSearchQuery = it },
                        placeholder = {
                            Text(
                                text = "Search in this shop...",
                                fontFamily = SuqiFontFamily,
                                fontSize = 14.sp,
                                color = SuqiMutedText
                            )
                        },
                        leadingIcon = {
                            Icon(LucideIcons.Search, contentDescription = null, tint = SuqiSecondaryText)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SuqiSecondaryBackground,
                            unfocusedContainerColor = SuqiSecondaryBackground,
                            focusedBorderColor = SuqiPrimary,
                            unfocusedBorderColor = SuqiBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Storefront Navigation Tabs: Products | Collections | Reviews
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TabLabel(
                            title = "Products (${products.size})",
                            isSelected = selectedTab == 0,
                            onClick = { selectedTab = 0 }
                        )
                        TabLabel(
                            title = "Collections",
                            isSelected = selectedTab == 1,
                            onClick = { selectedTab = 1 }
                        )
                        TabLabel(
                            title = "Reviews (${reviews.size})",
                            isSelected = selectedTab == 2,
                            onClick = { selectedTab = 2 }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Tab Content: 0 = Products, 1 = Collections, 2 = Reviews
            if (selectedTab == 0) {
                val filteredProducts = if (shopSearchQuery.isBlank()) products else products.filter {
                    it.name.contains(shopSearchQuery, ignoreCase = true)
                }

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
                                    onToggleWishlist = {
                                        coroutineScope.launch {
                                            viewModel.repository.toggleWishlist(prod.id)
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
            } else if (selectedTab == 1) {
                // Curated Shop Collections (Checklist Item 09 & Plan §5.2)
                val collections = listOf(
                    Triple("Best Sellers & Staff Picks", "Top rated electronics with full local warranty", "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600"),
                    Triple("New Season Arrivals", "Freshly restocked laptops, smartphones and accessories", "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=600"),
                    Triple("Flash Deals & Clearance", "Up to 35% discount on selected gadgets", "https://images.unsplash.com/photo-1546868871-7041f2a55e12?w=600"),
                    Triple("Everyday Bundles & Packs", "Combo packs with fast free Addis Ababa delivery", "https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?w=600")
                )

                items(collections) { (title, subtitle, img) ->
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SuqiSurface),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(SuqiBorder)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clickable {
                                selectedTab = 0
                                viewModel.showToast("Viewing: $title")
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SuqiSecondaryBackground)
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
                                    color = SuqiPrimaryText
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = subtitle,
                                    fontFamily = SuqiFontFamily,
                                    fontSize = 12.sp,
                                    color = SuqiSecondaryText,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Explore Collection →",
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = SuqiPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Reviews Tab
                if (reviews.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No reviews yet for this shop.", color = SuqiSecondaryText)
                        }
                    }
                } else {
                    items(reviews) { rev ->
                        ReviewItemCard(review = rev)
                    }
                }
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
            color = if (isSelected) SuqiPrimary else SuqiSecondaryText
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
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SuqiSecondaryBackground),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = review.userName,
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = review.date,
                    fontFamily = SuqiFontFamily,
                    fontSize = 12.sp,
                    color = SuqiMutedText
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                for (i in 1..5) {
                    Icon(
                        imageVector = LucideIcons.StarFilled,
                        contentDescription = null,
                        tint = if (i <= review.rating) SuqiWarning else SuqiBorder,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = review.comment,
                fontFamily = SuqiFontFamily,
                fontSize = 13.sp,
                color = SuqiPrimaryText
            )
        }
    }
}
