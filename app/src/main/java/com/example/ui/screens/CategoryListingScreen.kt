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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MarketplaceCategories
import com.example.data.model.ProductDto
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun CategoryListingScreen(
    categoryId: String,
    categoryTitle: String,
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = com.example.ui.i18n.suqiStrings()
    val cartCount by viewModel.cartCount.collectAsState(initial = 0)
    val isOffline by viewModel.isOffline.collectAsState()

    var products by remember { mutableStateOf<List<ProductDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedSubCategory by remember { mutableStateOf("All") }
    var sortBy by remember { mutableStateOf("POPULAR") }
    var inStockOnly by remember { mutableStateOf(false) }
    var verifiedOnly by remember { mutableStateOf(false) }
    var priceRange by remember { mutableStateOf(0f..50000f) }
    var minRating by remember { mutableDoubleStateOf(0.0) }
    var isGridView by remember { mutableStateOf(true) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }

    val subCategories = remember(categoryId) {
        listOf("All") + MarketplaceCategories.subCategoriesForId(categoryId)
    }

    fun applyClientFilters(raw: List<ProductDto>): List<ProductDto> {
        val minP = if (priceRange.start > 0f) priceRange.start.toDouble() else null
        val maxP = if (priceRange.endInclusive < 50000f) priceRange.endInclusive.toDouble() else null
        val filtered = raw.filter { p ->
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

    fun loadProducts() {
        coroutineScope.launch {
            isLoading = products.isEmpty()
            val res = viewModel.repository.getProducts(
                categoryId = if (categoryId == "all") null else categoryId,
                subCategory = if (selectedSubCategory == "All") null else selectedSubCategory,
                inStockOnly = inStockOnly,
                sortBy = sortBy
            )
            val baseList = if (res.success && !res.data.isNullOrEmpty()) {
                res.data
            } else if (selectedSubCategory != "All") {
                val catRes = viewModel.repository.getProducts(
                    categoryId = if (categoryId == "all") null else categoryId,
                    inStockOnly = inStockOnly,
                    sortBy = sortBy
                )
                val catData = catRes.data.orEmpty()
                val matching = catData.filter {
                    it.subCategory.contains(selectedSubCategory, ignoreCase = true) ||
                        it.name.contains(selectedSubCategory, ignoreCase = true) ||
                        it.description.contains(selectedSubCategory, ignoreCase = true)
                }
                if (matching.isNotEmpty()) matching else catData
            } else {
                res.data.orEmpty()
            }
            products = applyClientFilters(baseList)
            isLoading = false
        }
    }

    LaunchedEffect(categoryId, selectedSubCategory, sortBy, inStockOnly, verifiedOnly, priceRange, minRating) {
        loadProducts()
    }

    val activeFilterCount = listOf(
        priceRange.start > 0f || priceRange.endInclusive < 50000f,
        minRating > 0.0,
        inStockOnly,
        verifiedOnly
    ).count { it }

    SuqiScreen(
        title = categoryTitle,
        subtitle = strings.buyer.productsCountTemplate.replace("%d", products.size.toString()),
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        onRetryOffline = {
            viewModel.setOfflineMode(false)
            loadProducts()
        },
        headerActions = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .suqiWarmShadow(CircleShape, elevation = 6.dp)
                    .clip(CircleShape)
                    .background(colors.cardBackground)
                    .clickable { viewModel.navigate(ScreenDestination.Cart) }
                    .testTag("category_cart_btn"),
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
        },
        modifier = modifier
    ) { paddingValues ->
        if (isLoading && products.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(top = 12.dp)) {
                SuqiSkeletonProductGrid(count = 4)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    SuqiFilterChips(
                        items = subCategories.map { it to if (it == "All") strings.allLabel else it },
                        selectedKey = selectedSubCategory,
                        onSelect = { selectedSubCategory = it },
                        testTagPrefix = "subcat"
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .weight(1f)
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
                                modifier = Modifier.testTag("category_filter_btn")
                            ) {
                                Icon(LucideIcons.SlidersHorizontal, contentDescription = null, tint = if (activeFilterCount > 0) SuqiPrimary else colors.primaryText, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (activeFilterCount > 0) "${strings.filtersLabel} ($activeFilterCount)" else strings.filtersLabel,
                                    fontFamily = SuqiFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (activeFilterCount > 0) SuqiPrimary else colors.primaryText
                                )
                            }

                            OutlinedButton(
                                onClick = { showSortSheet = true },
                                shape = RoundedCornerShape(999.dp),
                                border = BorderStroke(1.dp, colors.border),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = colors.cardBackground),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("category_sort_btn")
                            ) {
                                Icon(LucideIcons.ArrowUpDown, contentDescription = null, tint = colors.primaryText, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.sortLabel, fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.primaryText)
                            }
                        }

                        IconButton(
                            onClick = { isGridView = !isGridView },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(colors.cardBackground)
                                .border(1.dp, colors.border, CircleShape)
                                .testTag("category_view_toggle_btn")
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

                if (products.isEmpty()) {
                    item {
                        SuqiEmptyState(
                            icon = LucideIcons.SearchX,
                            title = strings.noProductsFound,
                            subtitle = strings.buyer.noProductsFilterSub,
                            ctaText = strings.buyer.resetFiltersTitle,
                            onCtaClick = {
                                selectedSubCategory = "All"
                                priceRange = 0f..50000f
                                minRating = 0.0
                                inStockOnly = false
                                verifiedOnly = false
                            }
                        )
                    }
                } else if (isGridView) {
                    val chunks = products.chunked(2)
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
                                                loadProducts()
                                            }
                                        }
                                    )
                                }
                            }
                            if (chunk.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                } else {
                    items(products) { prod ->
                        SearchResultProductRow(
                            product = prod,
                            onClick = { viewModel.navigate(ScreenDestination.ProductDetails(prod.id)) },
                            onToggleWishlist = {
                                coroutineScope.launch {
                                    viewModel.repository.toggleWishlist(prod.id)
                                    loadProducts()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        SuqiBottomSheet(onDismissRequest = { showFilterSheet = false }, title = strings.buyer.filterCategoryTitle) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(strings.buyer.priceRangeEtb, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primaryText)
                SuqiRangeSlider(
                    values = priceRange,
                    onValuesChange = { priceRange = it },
                    valueRange = 0f..50000f,
                    formatLabel = { v -> if (v >= 50000f) "${formatEtb(50000.0)}+" else formatEtb(v.toDouble()) },
                    modifier = Modifier.testTag("category_price_slider")
                )

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
                    testTag = "category_in_stock_switch"
                )
                SuqiSwitchRow(
                    title = strings.buyer.verifiedShopsOnly,
                    checked = verifiedOnly,
                    onCheckedChange = { verifiedOnly = it },
                    testTag = "category_verified_switch"
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SuqiButton(
                        text = strings.common.resetLabel,
                        isPrimary = false,
                        isPill = true,
                        onClick = {
                            priceRange = 0f..50000f
                            minRating = 0.0
                            inStockOnly = false
                            verifiedOnly = false
                            showFilterSheet = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                    SuqiButton(
                        text = strings.common.applyFiltersLabel,
                        isPill = true,
                        onClick = { showFilterSheet = false },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (showSortSheet) {
        SuqiBottomSheet(onDismissRequest = { showSortSheet = false }, title = strings.buyer.sortByTitle) {
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
                        Text(label, fontFamily = SuqiFontFamily, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = if (selected) SuqiPrimary else colors.primaryText)
                    }
                }
            }
        }
    }
}
