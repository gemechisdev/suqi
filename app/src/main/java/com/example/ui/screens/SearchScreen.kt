package com.example.ui.screens

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
import com.example.ui.i18n.LocalSuqiLanguage
import com.example.ui.i18n.localizedName
import com.example.ui.i18n.suqiStrings
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val lang = LocalSuqiLanguage.current
    val cartCount by viewModel.cartCount.collectAsState(initial = 0)
    val isOffline by viewModel.isOffline.collectAsState()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0: Products, 1: Shops
    var allProducts by remember { mutableStateOf(viewModel.cachedAllProducts) }
    var allShops by remember { mutableStateOf(viewModel.cachedAllShops) }
    var categories by remember { mutableStateOf(viewModel.cachedAllCategories) }

    var productResults by remember { mutableStateOf<List<ProductDto>>(emptyList()) }
    var shopResults by remember { mutableStateOf<List<ShopDto>>(emptyList()) }
    var isGridView by rememberSaveable { mutableStateOf(false) }

    // B-03 Filter & Sort State
    var sortBy by rememberSaveable { mutableStateOf("POPULAR") }
    var selectedCategoryFilter by rememberSaveable { mutableStateOf("") }
    var priceRangeStart by rememberSaveable { mutableFloatStateOf(0f) }
    var priceRangeEnd by rememberSaveable { mutableFloatStateOf(50000f) }
    val priceRange = priceRangeStart..priceRangeEnd
    var minRating by rememberSaveable { mutableDoubleStateOf(0.0) }
    var inStockOnly by rememberSaveable { mutableStateOf(false) }
    var verifiedOnly by rememberSaveable { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    val idleListState = rememberLazyListState()
    val resultsListState = rememberLazyListState()

    BackHandler(enabled = showFilterSheet || showSortSheet) {
        showFilterSheet = false
        showSortSheet = false
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val searchPrefs = remember { context.getSharedPreferences("suqi_search_prefs", android.content.Context.MODE_PRIVATE) }
    val recentSearches = remember {
        mutableStateListOf<String>().apply {
            val initialized = searchPrefs.getBoolean("recent_initialized", false)
            if (!initialized && com.example.data.api.BackendConfig.DemoMode) {
                val defaults = listOf("laptop", "t-shirt", "nike", "headphones", "sofa")
                addAll(defaults)
                searchPrefs.edit()
                    .putBoolean("recent_initialized", true)
                    .putString("recent_json", org.json.JSONArray(defaults).toString())
                    .apply()
            } else {
                val raw = searchPrefs.getString("recent_json", "[]") ?: "[]"
                val arr = runCatching { org.json.JSONArray(raw) }.getOrNull()
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val term = arr.optString(i).trim()
                        if (term.isNotBlank()) add(term)
                    }
                }
            }
        }
    }
    fun persistRecent() {
        searchPrefs.edit()
            .putBoolean("recent_initialized", true)
            .putString("recent_json", org.json.JSONArray(recentSearches.toList()).toString())
            .apply()
    }
    val popularSearches = listOf("HP Core i5", "Samsung Galaxy", "Wireless Earbuds", "Sneakers", "Coffee Set")

    LaunchedEffect(Unit) {
        viewModel.repository.getProducts().data?.let {
            allProducts = it
            viewModel.cachedAllProducts = it
        }
        viewModel.repository.getShops().data?.let {
            allShops = it
            viewModel.cachedAllShops = it
        }
        viewModel.repository.getCategories().data?.let {
            categories = it
            viewModel.cachedAllCategories = it
        }
    }

    fun applyLocalFilters(baseProducts: List<ProductDto>): List<ProductDto> {
        val minP = if (priceRange.start > 0f) priceRange.start.toDouble() else null
        val maxP = if (priceRange.endInclusive < 50000f) priceRange.endInclusive.toDouble() else null
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
            val hasFilter = selectedCategoryFilter.isNotBlank() || priceRange.start > 0f ||
                priceRange.endInclusive < 50000f || minRating > 0.0 || inStockOnly || verifiedOnly
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
                if (recordRecent && q.isNotBlank()) {
                    val trimmed = q.trim()
                    recentSearches.removeAll { it.equals(trimmed, ignoreCase = true) }
                    recentSearches.add(0, trimmed)
                    if (recentSearches.size > 10) {
                        while (recentSearches.size > 10) recentSearches.removeAt(recentSearches.lastIndex)
                    }
                    persistRecent()
                }
            } else {
                productResults = emptyList()
                shopResults = emptyList()
            }
        }
    }

    // Immediately hydrate filtered results synchronously from cached lists on Back navigation so LazyColumn renders at restored scroll offset on frame 1
    fun applyLocalFiltersSync(q: String, prodList: List<ProductDto>, shopList: List<ShopDto>): Pair<List<ProductDto>, List<ShopDto>> {
        val matchedP = prodList.filter { p ->
            q.isBlank() ||
                p.name.contains(q, ignoreCase = true) ||
                p.nameAmharic.contains(q, ignoreCase = true) ||
                p.nameAfaanOromo.contains(q, ignoreCase = true) ||
                p.description.contains(q, ignoreCase = true) ||
                p.subCategory.contains(q, ignoreCase = true) ||
                p.shopName.contains(q, ignoreCase = true)
        }
        val matchedS = shopList.filter { s ->
            (q.isBlank() || s.name.contains(q, ignoreCase = true) || s.category.contains(q, ignoreCase = true)) &&
                (!verifiedOnly || s.isVerified)
        }
        return applyLocalFilters(matchedP) to matchedS
    }

    LaunchedEffect(searchQuery, sortBy, selectedCategoryFilter, priceRangeStart, priceRangeEnd, minRating, inStockOnly, verifiedOnly) {
        if (searchQuery.isNotBlank() && allProducts.isNotEmpty() && productResults.isEmpty()) {
            val (pSync, sSync) = applyLocalFiltersSync(searchQuery, allProducts, allShops)
            productResults = pSync
            shopResults = sSync
        }
        performSearch(searchQuery, recordRecent = false)
    }

    val activeFilterCount = listOf(
        selectedCategoryFilter.isNotBlank(),
        priceRange.start > 0f || priceRange.endInclusive < 50000f,
        minRating > 0.0,
        inStockOnly,
        verifiedOnly
    ).count { it }

    val hasActiveQueryOrFilters = searchQuery.isNotBlank() || activeFilterCount > 0

    // Live suggestions matching typed prefix/term
    val liveSuggestions = remember(searchQuery, allProducts, allShops, lang) {
        if (searchQuery.length < 2) emptyList()
        else {
            val pNames = allProducts.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                    it.nameAmharic.contains(searchQuery, ignoreCase = true) ||
                    it.nameAfaanOromo.contains(searchQuery, ignoreCase = true)
            }.map { it.localizedName(lang) }.take(3)
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
                    Icon(LucideIcons.ArrowLeft, contentDescription = strings.common.backLabel, tint = colors.primaryText, modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.width(10.dp))

                SuqiSearchField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    onSearch = { performSearch(searchQuery, recordRecent = true) },
                    placeholder = strings.searchPlaceholder,
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
                        Icon(LucideIcons.ShoppingCart, contentDescription = strings.cartTitle, tint = colors.primaryText, modifier = Modifier.size(20.dp))
                    }
                }
            }

            if (!hasActiveQueryOrFilters) {
                // Idle State
                LazyColumn(
                    state = idleListState,
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
                                    Text(strings.recentSearches, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                                    Text(
                                        text = strings.clearAll,
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = SuqiPrimary,
                                        modifier = Modifier
                                            .clickable {
                                                recentSearches.clear()
                                                persistRecent()
                                            }
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                            .testTag("search_clear_recent")
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                recentSearches.forEach { term ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { performSearch(term, recordRecent = true) }
                                            .padding(vertical = 8.dp, horizontal = 4.dp)
                                    ) {
                                        Icon(LucideIcons.History, contentDescription = null, tint = colors.secondaryText, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(term, fontFamily = SuqiFontFamily, fontSize = 14.sp, color = colors.primaryText, modifier = Modifier.weight(1f))
                                        IconButton(
                                            onClick = {
                                                recentSearches.remove(term)
                                                persistRecent()
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(LucideIcons.X, contentDescription = strings.buyer.removeTermDesc.replace("%s", term), tint = colors.mutedText, modifier = Modifier.size(15.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        SuqiSectionCard {
                            Text(strings.popularSearches, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
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
                    state = resultsListState,
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
                                        0 to "${strings.productsTab} (${productResults.size})",
                                        1 to "${strings.shopsTab} (${shopResults.size})"
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
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(colors.cardBackground)
                                                .border(1.dp, colors.border, CircleShape)
                                                .testTag("search_toggle_view_btn")
                                        ) {
                                            Icon(
                                                imageVector = if (isGridView) LucideIcons.ListIcon else LucideIcons.LayoutGrid,
                                                contentDescription = strings.buyer.toggleGridOrListDesc,
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
                                        text = if (activeFilterCount > 0) "${strings.filtersLabel} ($activeFilterCount)" else strings.filtersLabel,
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
                                        "PRICE_LOW_HIGH" -> strings.buyer.sortPriceLowHigh
                                        "PRICE_HIGH_LOW" -> strings.buyer.sortPriceHighLow
                                        "RATING" -> strings.buyer.sortTopRated
                                        "DISCOUNT" -> strings.buyer.sortBiggestDiscount
                                        "NEWEST" -> strings.buyer.sortNewest
                                        else -> strings.sortLabel
                                    }
                                    Text(sortLabel, fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.primaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }

                                if (activeFilterCount > 0) {
                                    TextButton(
                                        onClick = {
                                            selectedCategoryFilter = ""
                                            priceRangeStart = 0f
                                            priceRangeEnd = 50000f
                                            minRating = 0.0
                                            inStockOnly = false
                                            verifiedOnly = false
                                        }
                                    ) {
                                        Text(strings.clearAll, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = SuqiPrimary)
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
                                    title = strings.buyer.noMatchingProducts,
                                    subtitle = strings.buyer.noMatchingProductsSub,
                                    ctaText = strings.buyer.resetFiltersTitle,
                                    onCtaClick = {
                                        selectedCategoryFilter = ""
                                        priceRangeStart = 0f
                                        priceRangeEnd = 50000f
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
                                    title = strings.buyer.noMatchingShops,
                                    subtitle = strings.buyer.noMatchingShopsSubTemplate.replace("%s", searchQuery)
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
        SuqiBottomSheet(onDismissRequest = { showFilterSheet = false }, title = strings.buyer.filterResultsTitle) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(strings.buyer.priceRangeEtb, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primaryText)
                SuqiRangeSlider(
                    values = priceRange,
                    onValuesChange = {
                        priceRangeStart = it.start
                        priceRangeEnd = it.endInclusive
                    },
                    valueRange = 0f..50000f,
                    formatLabel = { v -> if (v >= 50000f) "${formatEtb(50000.0)}+" else formatEtb(v.toDouble()) },
                    modifier = Modifier.testTag("search_price_slider")
                )

                if (categories.isNotEmpty()) {
                    Text(strings.buyer.categoryLabel, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primaryText)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        FilterChip(
                            selected = selectedCategoryFilter.isBlank(),
                            onClick = { selectedCategoryFilter = "" },
                            label = { Text(strings.allLabel, fontFamily = SuqiFontFamily) }
                        )
                        categories.forEach { cat ->
                            val catName = cat.localizedName(lang)
                            FilterChip(
                                selected = selectedCategoryFilter == cat.id,
                                onClick = { selectedCategoryFilter = cat.id },
                                label = { Text(catName, fontFamily = SuqiFontFamily) }
                            )
                        }
                    }
                }

                Text(strings.buyer.minRatingLabel, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primaryText)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.0 to strings.buyer.anyLabel, 3.0 to "3.0★+", 4.0 to "4.0★+", 4.5 to "4.5★+").forEach { (r, lbl) ->
                        FilterChip(
                            selected = minRating == r,
                            onClick = { minRating = r },
                            label = { Text(lbl, fontFamily = SuqiFontFamily) }
                        )
                    }
                }

                SuqiSwitchRow(
                    title = strings.buyer.inStockOnly,
                    checked = inStockOnly,
                    onCheckedChange = { inStockOnly = it },
                    testTag = "search_in_stock_switch"
                )
                SuqiSwitchRow(
                    title = strings.buyer.verifiedShopsOnly,
                    checked = verifiedOnly,
                    onCheckedChange = { verifiedOnly = it },
                    testTag = "search_verified_switch"
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SuqiButton(
                        text = strings.common.resetLabel,
                        isPrimary = false,
                        isPill = true,
                        onClick = {
                            selectedCategoryFilter = ""
                            priceRangeStart = 0f
                            priceRangeEnd = 50000f
                            minRating = 0.0
                            inStockOnly = false
                            verifiedOnly = false
                            showFilterSheet = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                    SuqiButton(
                        text = strings.applyLabel,
                        isPill = true,
                        onClick = { showFilterSheet = false },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (showSortSheet) {
        SuqiBottomSheet(onDismissRequest = { showSortSheet = false }, title = strings.buyer.sortProductsByTitle) {
            val sortOptions = listOf(
                "POPULAR" to strings.buyer.sortPopular,
                "PRICE_LOW_HIGH" to strings.buyer.sortPriceLowHigh,
                "PRICE_HIGH_LOW" to strings.buyer.sortPriceHighLow,
                "RATING" to strings.buyer.sortTopRated,
                "DISCOUNT" to strings.buyer.sortBiggestDiscount,
                "NEWEST" to strings.buyer.sortNewest
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
    val strings = suqiStrings()
    val lang = LocalSuqiLanguage.current
    val displayTitle = product.localizedName(lang)
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
                    contentDescription = displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayTitle,
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
                    product.stockQuantity <= 0 -> SuqiStatusBadge(strings.outOfStock, StatusType.ERROR)
                    product.stockQuantity in 1..5 -> SuqiStatusBadge(strings.common.onlyNLeftTemplate.replace("%d", product.stockQuantity.toString()), StatusType.WARNING)
                    else -> SuqiStatusBadge(strings.inStock, StatusType.SUCCESS)
                }
            }

            IconButton(onClick = onToggleWishlist) {
                Icon(
                    imageVector = if (product.isWishlisted) LucideIcons.HeartFilled else LucideIcons.Heart,
                    contentDescription = strings.wishlistTitle,
                    tint = if (product.isWishlisted) SuqiPrimary else colors.secondaryText
                )
            }
        }
    }
}
