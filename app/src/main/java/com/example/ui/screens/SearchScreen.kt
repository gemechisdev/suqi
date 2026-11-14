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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CategoryDto
import com.example.data.model.ProductDto
import com.example.data.model.ShopDto
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val cartCount by viewModel.cartCount.collectAsState(initial = 0)
    val isOffline by viewModel.isOffline.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Products, 1: Shops
    var allProducts by remember { mutableStateOf<List<ProductDto>>(emptyList()) }
    var allShops by remember { mutableStateOf<List<ShopDto>>(emptyList()) }
    var categories by remember { mutableStateOf<List<CategoryDto>>(emptyList()) }

    var productResults by remember { mutableStateOf<List<ProductDto>>(emptyList()) }
    var shopResults by remember { mutableStateOf<List<ShopDto>>(emptyList()) }
    var isGridView by remember { mutableStateOf(false) }

    // B-03 Filter & Sort State
    var sortBy by remember { mutableStateOf("POPULAR") }
    var selectedCategoryFilter by remember { mutableStateOf("") }
    var minPriceText by remember { mutableStateOf("") }
    var maxPriceText by remember { mutableStateOf("") }
    var minRating by remember { mutableDoubleStateOf(0.0) }
    var inStockOnly by remember { mutableStateOf(false) }
    var verifiedOnly by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }

    val recentSearches = remember { mutableStateListOf("laptop", "t-shirt", "nike", "headphones", "sofa") }
    val popularSearches = listOf("HP Core i5", "Samsung Galaxy", "Wireless Earbuds", "Sneakers", "Coffee Set")

    LaunchedEffect(Unit) {
        viewModel.repository.getProducts().data?.let { allProducts = it }
        viewModel.repository.getShops().data?.let { allShops = it }
        viewModel.repository.getCategories().data?.let { categories = it }
    }

    fun applyLocalFilters(baseProducts: List<ProductDto>): List<ProductDto> {
        val minP = minPriceText.toDoubleOrNull()
        val maxP = maxPriceText.toDoubleOrNull()
        val filtered = baseProducts.filter { p ->
            (selectedCategoryFilter.isBlank() || p.categoryId == selectedCategoryFilter) &&
                (minP == null || p.price >= minP) &&
                (maxP == null || p.price <= maxP) &&
                (minRating <= 0.0 || p.rating >= minRating) &&
                (!inStockOnly || p.stockQuantity > 0) &&
                (!verifiedOnly || p.shopVerified)
        }
        return when (sortBy) {
            "PRICE_LOW_HIGH" -> filtered.sortedBy { it.price }
            "PRICE_HIGH_LOW" -> filtered.sortedByDescending { it.price }
            "RATING" -> filtered.sortedByDescending { it.rating }
            "DISCOUNT" -> filtered.sortedByDescending { it.discountPercent }
            "NEWEST" -> filtered.reversed()
            else -> filtered
        }
    }

    fun performSearch(q: String, recordRecent: Boolean = true) {
        searchQuery = q
        coroutineScope.launch {
            val hasFilter = selectedCategoryFilter.isNotBlank() || minPriceText.isNotBlank() ||
                maxPriceText.isNotBlank() || minRating > 0.0 || inStockOnly || verifiedOnly
            if (q.isNotBlank() || hasFilter) {
                val pRes = viewModel.repository.getProducts(
                    query = q.takeIf { it.isNotBlank() },
                    categoryId = selectedCategoryFilter.takeIf { it.isNotBlank() },
                    inStockOnly = inStockOnly,
                    sortBy = sortBy
                )
                productResults = applyLocalFilters(pRes.data.orEmpty())
                val sRes = viewModel.repository.getShops(query = q.takeIf { it.isNotBlank() })
                shopResults = (sRes.data.orEmpty()).filter { !verifiedOnly || it.isVerified }
                if (recordRecent && q.isNotBlank() && !recentSearches.contains(q)) {
                    recentSearches.add(0, q)
                }
            } else {
                productResults = emptyList()
                shopResults = emptyList()
            }
        }
    }

    LaunchedEffect(searchQuery, sortBy, selectedCategoryFilter, minPriceText, maxPriceText, minRating, inStockOnly, verifiedOnly) {
        performSearch(searchQuery, recordRecent = false)
    }

    val activeFilterCount = listOf(
        selectedCategoryFilter.isNotBlank(),
        minPriceText.isNotBlank() || maxPriceText.isNotBlank(),
        minRating > 0.0,
        inStockOnly,
        verifiedOnly
    ).count { it }

    val hasActiveQueryOrFilters = searchQuery.isNotBlank() || activeFilterCount > 0

    // Live suggestions matching typed prefix/term
    val liveSuggestions = remember(searchQuery, allProducts, allShops) {
        if (searchQuery.length < 2) emptyList()
        else {
            val pNames = allProducts.filter { it.name.contains(searchQuery, ignoreCase = true) }
                .map { it.name }.take(3)
            val sNames = allShops.filter { it.name.contains(searchQuery, ignoreCase = true) }
                .map { it.name }.take(2)
            (pNames + sNames).distinct()
        }
    }

    SuqiScreen(
        isOffline = isOffline,
        onRetryOffline = {
            viewModel.setOfflineMode(false)
            performSearch(searchQuery, recordRecent = false)
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Top Search Bar Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .suqiWarmShadow(CircleShape, elevation = 6.dp)
                        .clip(CircleShape)
                        .background(colors.cardBackground)
                        .clickable { viewModel.goBack() }
                        .testTag("search_back_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(LucideIcons.ArrowLeft, contentDescription = "Back", tint = colors.primaryText, modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.width(10.dp))

                SuqiSearchField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Search products or shops...",
                    testTag = "search_input_field",
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .suqiWarmShadow(CircleShape, elevation = 6.dp)
                        .clip(CircleShape)
                        .background(colors.cardBackground)
                        .clickable { viewModel.navigate(ScreenDestination.Cart) }
                        .testTag("search_cart_button"),
                    contentAlignment = Alignment.Center
                ) {
                    BadgedBox(
                        badge = {
                            if (cartCount > 0) {
                                Badge(containerColor = SuqiPrimary, contentColor = Color.White) {
                                    Text(cartCount.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    ) {
                        Icon(LucideIcons.ShoppingCart, contentDescription = "Cart", tint = colors.primaryText, modifier = Modifier.size(20.dp))
                    }
                }
            }

            if (!hasActiveQueryOrFilters) {
                // Idle State
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (recentSearches.isNotEmpty()) {
                        item {
                            SuqiSectionCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Recent Searches", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                                    Text(
                                        text = "Clear",
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = SuqiPrimary,
                                        modifier = Modifier.clickable { recentSearches.clear() }
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                recentSearches.forEach { term ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { performSearch(term) }
                                            .padding(vertical = 10.dp, horizontal = 4.dp)
                                    ) {
                                        Icon(LucideIcons.History, contentDescription = null, tint = colors.secondaryText, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(term, fontFamily = SuqiFontFamily, fontSize = 14.sp, color = colors.primaryText, modifier = Modifier.weight(1f))
                                        Icon(LucideIcons.ArrowUpLeft, contentDescription = null, tint = colors.mutedText, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    item {
                        SuqiSectionCard {
                            Text("Popular Searches", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                            Spacer(modifier = Modifier.height(10.dp))
                            popularSearches.forEach { term ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { performSearch(term) }
                                        .padding(vertical = 10.dp, horizontal = 4.dp)
                                ) {
                                    Icon(LucideIcons.TrendingUp, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(term, fontFamily = SuqiFontFamily, fontSize = 14.sp, color = colors.primaryText, modifier = Modifier.weight(1f))
                                    Icon(LucideIcons.ArrowUpLeft, contentDescription = null, tint = colors.mutedText, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = SuqiBottomNavContentPadding + 16.dp)
                ) {
                    // Live autocomplete suggestions strip while typing
                    if (liveSuggestions.isNotEmpty()) {
                        item {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                liveSuggestions.forEach { suggestion ->
                                    AssistChip(
                                        onClick = { performSearch(suggestion) },
                                        label = { Text(suggestion, fontFamily = SuqiFontFamily, fontSize = 12.sp) },
                                        leadingIcon = { Icon(LucideIcons.Search, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    )
                                }
                            }
                        }
                    }

                    // Products | Shops Switcher + Filter + Sort + Grid/List Toggle
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(colors.cardBackground)
                                        .border(1.dp, colors.border, RoundedCornerShape(999.dp))
                                        .padding(4.dp)
                                ) {
                                    listOf(
                                        0 to "Products (${productResults.size})",
                                        1 to "Shops (${shopResults.size})"
                                    ).forEach { (idx, label) ->
                                        val selected = selectedTab == idx
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(if (selected) SuqiPrimary else Color.Transparent)
                                                .clickable { selectedTab = idx }
                                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                fontFamily = SuqiFontFamily,
                                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                                fontSize = 13.sp,
                                                color = if (selected) Color.White else colors.secondaryText
                                            )
                                        }
                                    }
                                }

                                if (selectedTab == 0) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        IconButton(
                                            onClick = { isGridView = !isGridView },
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(colors.cardBackground)
                                                .border(1.dp, colors.border, CircleShape)
                                                .testTag("search_toggle_view_btn")
                                        ) {
                                            Icon(
                                                imageVector = if (isGridView) LucideIcons.ListIcon else LucideIcons.LayoutGrid,
                                                contentDescription = "Toggle Grid or List",
                                                tint = colors.primaryText,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                OutlinedButton(
                                    onClick = { showFilterSheet = true },
                                    shape = RoundedCornerShape(999.dp),
                                    border = BorderStroke(1.dp, if (activeFilterCount > 0) SuqiPrimary else colors.border),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (activeFilterCount > 0) colors.primaryTint else colors.cardBackground
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("search_filter_btn")
                                ) {
                                    Icon(LucideIcons.SlidersHorizontal, contentDescription = null, tint = if (activeFilterCount > 0) SuqiPrimary else colors.primaryText, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (activeFilterCount > 0) "Filters ($activeFilterCount)" else "Filters",
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = if (activeFilterCount > 0) SuqiPrimary else colors.primaryText
                                    )
                                }

                                OutlinedButton(
                                    onClick = { showSortSheet = true },
                                    shape = RoundedCornerShape(999.dp),
                                    border = BorderStroke(1.dp, colors.border),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = colors.cardBackground),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("search_sort_btn")
                                ) {
                                    Icon(LucideIcons.ArrowUpDown, contentDescription = null, tint = colors.primaryText, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    val sortLabel = when (sortBy) {
                                        "PRICE_LOW_HIGH" -> "Price: Low-High"
                                        "PRICE_HIGH_LOW" -> "Price: High-Low"
                                        "RATING" -> "Top Rated"
                                        "DISCOUNT" -> "Biggest Discount"
                                        "NEWEST" -> "Newest"
                                        else -> "Popular"
                                    }
                                    Text(sortLabel, fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.primaryText)
                                }

                                if (activeFilterCount > 0) {
                                    TextButton(
                                        onClick = {
                                            selectedCategoryFilter = ""
                                            minPriceText = ""
                                            maxPriceText = ""
                                            minRating = 0.0
                                            inStockOnly = false
                                            verifiedOnly = false
                                        }
                                    ) {
                                        Text("Clear All", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = SuqiPrimary)
                                    }
                                }
                            }
                        }
                    }

                    if (selectedTab == 0) {
                        if (productResults.isEmpty()) {
                            item {
                                SuqiEmptyState(
                                    icon = LucideIcons.SearchX,
                                    title = "No matching products",
                                    subtitle = "Try adjusting your search keywords or clearing active price/rating filters.",
                                    ctaText = "Clear Filters",
                                    onCtaClick = {
                                        selectedCategoryFilter = ""
                                        minPriceText = ""
                                        maxPriceText = ""
                                        minRating = 0.0
                                        inStockOnly = false
                                        verifiedOnly = false
                                    }
                                )
                            }
                        } else if (isGridView) {
                            val chunks = productResults.chunked(2)
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
                                                onAddToCart = { viewModel.quickAddToCart(prod) },
                                                onShopClick = { viewModel.navigate(ScreenDestination.ShopStorefront(prod.shopId)) },
                                                onToggleWishlist = {
                                                    coroutineScope.launch {
                                                        viewModel.repository.toggleWishlist(prod.id)
                                                        performSearch(searchQuery, recordRecent = false)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                    if (chunk.size == 1) Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        } else {
                            items(productResults) { prod ->
                                SearchResultProductRow(
                                    product = prod,
                                    onClick = { viewModel.navigate(ScreenDestination.ProductDetails(prod.id)) },
                                    onToggleWishlist = {
                                        coroutineScope.launch {
                                            viewModel.repository.toggleWishlist(prod.id)
                                            performSearch(searchQuery, recordRecent = false)
                                        }
                                    }
                                )
                            }
                        }
                    } else {
                        if (shopResults.isEmpty()) {
                            item {
                                SuqiEmptyState(
                                    icon = LucideIcons.Store,
                                    title = "No matching shops",
                                    subtitle = "No shops matched \"$searchQuery\" with current filters."
                                )
                            }
                        } else {
                            items(shopResults) { shop ->
                                ShopListItemRow(
                                    shop = shop,
                                    onClick = { viewModel.navigate(ScreenDestination.ShopStorefront(shop.id)) },
                                    onToggleFollow = {
                                        coroutineScope.launch {
                                            viewModel.repository.toggleFollowShop(shop.id)
                                            performSearch(searchQuery, recordRecent = false)
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

    if (showFilterSheet) {
        SuqiBottomSheet(onDismissRequest = { showFilterSheet = false }, title = "Filter Results") {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Price Range (ETB)", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primaryText)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SuqiTextField(value = minPriceText, onValueChange = { minPriceText = it.filter(Char::isDigit) }, label = "Min ETB", placeholder = "0", modifier = Modifier.weight(1f))
                    SuqiTextField(value = maxPriceText, onValueChange = { maxPriceText = it.filter(Char::isDigit) }, label = "Max ETB", placeholder = "Any", modifier = Modifier.weight(1f))
                }

                if (categories.isNotEmpty()) {
                    Text("Category", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primaryText)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        FilterChip(
                            selected = selectedCategoryFilter.isBlank(),
                            onClick = { selectedCategoryFilter = "" },
                            label = { Text("All", fontFamily = SuqiFontFamily) }
                        )
                        categories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategoryFilter == cat.id,
                                onClick = { selectedCategoryFilter = cat.id },
                                label = { Text(cat.name, fontFamily = SuqiFontFamily) }
                            )
                        }
                    }
                }

                Text("Minimum Rating", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primaryText)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.0 to "Any", 3.0 to "3.0★+", 4.0 to "4.0★+", 4.5 to "4.5★+").forEach { (r, lbl) ->
                        FilterChip(
                            selected = minRating == r,
                            onClick = { minRating = r },
                            label = { Text(lbl, fontFamily = SuqiFontFamily) }
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("In Stock Only", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Medium, color = colors.primaryText)
                    Switch(checked = inStockOnly, onCheckedChange = { inStockOnly = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Verified Shops Only", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Medium, color = colors.primaryText)
                    Switch(checked = verifiedOnly, onCheckedChange = { verifiedOnly = it })
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SuqiButton(
                        text = "Reset",
                        isPrimary = false,
                        isPill = true,
                        onClick = {
                            selectedCategoryFilter = ""
                            minPriceText = ""
                            maxPriceText = ""
                            minRating = 0.0
                            inStockOnly = false
                            verifiedOnly = false
                            showFilterSheet = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                    SuqiButton(
                        text = "Apply",
                        isPill = true,
                        onClick = { showFilterSheet = false },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (showSortSheet) {
        SuqiBottomSheet(onDismissRequest = { showSortSheet = false }, title = "Sort Products By") {
            val sortOptions = listOf(
                "POPULAR" to "Best Match / Popular",
                "PRICE_LOW_HIGH" to "Price: Low to High",
                "PRICE_HIGH_LOW" to "Price: High to Low",
                "RATING" to "Top Rated",
                "DISCOUNT" to "Biggest Discount",
                "NEWEST" to "Newest Arrivals"
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                sortOptions.forEach { (key, label) ->
                    val selected = sortBy == key
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) colors.primaryTint else Color.Transparent)
                            .clickable {
                                sortBy = key
                                showSortSheet = false
                            }
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        RadioButton(selected = selected, onClick = {
                            sortBy = key
                            showSortSheet = false
                        })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            fontFamily = SuqiFontFamily,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) SuqiPrimary else colors.primaryText
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultProductRow(
    product: ProductDto,
    onClick: () -> Unit,
    onToggleWishlist: () -> Unit
) {
    val colors = LocalSuqiColors.current
    SuqiSectionCard(
        padding = 12.dp,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.imageBackdrop)
            ) {
                AsyncImage(
                    model = product.primaryImageUrl,
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = colors.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(LucideIcons.StarFilled, contentDescription = null, tint = SuqiWarning, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("${product.rating} (${product.reviewCount})", fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.secondaryText)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("• ${product.shopName}", fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.mutedText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(formatEtb(product.price), fontFamily = SuqiFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = SuqiPrimary)
                    if (product.discountPercent > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("-${product.discountPercent}%", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SuqiError)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                when {
                    product.stockQuantity <= 0 -> SuqiStatusBadge("Out of Stock", StatusType.ERROR)
                    product.stockQuantity in 1..5 -> SuqiStatusBadge("Only ${product.stockQuantity} left", StatusType.WARNING)
                    else -> SuqiStatusBadge("In Stock", StatusType.SUCCESS)
                }
            }

            IconButton(onClick = onToggleWishlist) {
                Icon(
                    imageVector = if (product.isWishlisted) LucideIcons.HeartFilled else LucideIcons.Heart,
                    contentDescription = "Wishlist",
                    tint = if (product.isWishlisted) SuqiPrimary else colors.secondaryText
                )
            }
        }
    }
}
