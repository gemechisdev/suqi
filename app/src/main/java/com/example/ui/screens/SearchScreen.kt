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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
    val cartCount by viewModel.cartCount.collectAsState(initial = 0)

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Products, 1: Shops
    var productResults by remember { mutableStateOf<List<ProductDto>>(emptyList()) }
    var shopResults by remember { mutableStateOf<List<ShopDto>>(emptyList()) }
    var sortBy by remember { mutableStateOf("POPULAR") }

    val recentSearches = remember { mutableStateListOf("laptop", "t-shirt", "nike", "headphones", "sofa") }
    val popularSearches = listOf("HP Core i5", "Samsung Galaxy", "Wireless Earbuds", "Sneakers", "Smart Watch")

    fun performSearch(q: String) {
        searchQuery = q
        coroutineScope.launch {
            if (q.isNotBlank()) {
                val pRes = viewModel.repository.getProducts(query = q, sortBy = sortBy)
                if (pRes.success && pRes.data != null) {
                    productResults = pRes.data
                }
                val sRes = viewModel.repository.getShops(query = q)
                if (sRes.success && sRes.data != null) {
                    shopResults = sRes.data
                }
                if (!recentSearches.contains(q)) {
                    recentSearches.add(0, q)
                }
            } else {
                productResults = emptyList()
                shopResults = emptyList()
            }
        }
    }

    LaunchedEffect(searchQuery, sortBy) {
        if (searchQuery.isNotBlank()) {
            performSearch(searchQuery)
        }
    }

    Scaffold(
        topBar = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                IconButton(
                    onClick = { viewModel.goBack() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SuqiSecondaryBackground)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = SuqiPrimaryText,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Search TextField
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        performSearch(it)
                    },
                    placeholder = {
                        Text(
                            text = "Search products or shops...",
                            fontFamily = SuqiFontFamily,
                            fontSize = 14.sp,
                            color = SuqiMutedText
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = SuqiSecondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                productResults = emptyList()
                                shopResults = emptyList()
                            }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = SuqiSecondaryText)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SuqiSecondaryBackground,
                        unfocusedContainerColor = SuqiSecondaryBackground,
                        focusedBorderColor = SuqiPrimary,
                        unfocusedBorderColor = SuqiBorder
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(max = 52.dp)
                        .testTag("search_input_field")
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Cart Icon with badge
                IconButton(
                    onClick = { viewModel.navigate(ScreenDestination.Cart) },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SuqiSecondaryBackground)
                ) {
                    BadgedBox(
                        badge = {
                            if (cartCount > 0) {
                                Badge(containerColor = SuqiPrimary, contentColor = Color.White) {
                                    Text(
                                        text = cartCount.toString(),
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ShoppingCart,
                            contentDescription = "Cart",
                            tint = SuqiPrimaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        containerColor = SuqiBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        if (searchQuery.isBlank()) {
            // Idle State: Recent Searches (Clock) & Popular Searches (Trend)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp)
            ) {
                if (recentSearches.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent Searches",
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = SuqiPrimaryText
                            )
                            Text(
                                text = "Clear",
                                fontFamily = SuqiFontFamily,
                                fontSize = 13.sp,
                                color = SuqiSecondaryText,
                                modifier = Modifier.clickable { recentSearches.clear() }
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    items(recentSearches) { term ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { performSearch(term) }
                                .padding(vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = "Recent",
                                tint = SuqiSecondaryText,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = term,
                                fontFamily = SuqiFontFamily,
                                fontSize = 14.sp,
                                color = SuqiPrimaryText,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Outlined.NorthWest,
                                contentDescription = null,
                                tint = SuqiMutedText,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }

                item {
                    Text(
                        text = "Popular Searches",
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = SuqiPrimaryText
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                items(popularSearches) { term ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { performSearch(term) }
                            .padding(vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.TrendingUp,
                            contentDescription = "Trending",
                            tint = SuqiPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = term,
                            fontFamily = SuqiFontFamily,
                            fontSize = 14.sp,
                            color = SuqiPrimaryText,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Outlined.NorthWest,
                            contentDescription = null,
                            tint = SuqiMutedText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        } else {
            // Active Results State: Products | Shops Tabs & Cards
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                // Products | Shops Switcher
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(SuqiSecondaryBackground)
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (selectedTab == 0) SuqiPrimary else Color.Transparent)
                                    .clickable { selectedTab = 0 }
                                    .padding(horizontal = 18.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Products (${productResults.size})",
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = if (selectedTab == 0) Color.White else SuqiSecondaryText
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (selectedTab == 1) SuqiPrimary else Color.Transparent)
                                    .clickable { selectedTab = 1 }
                                    .padding(horizontal = 18.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Shops (${shopResults.size})",
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = if (selectedTab == 1) Color.White else SuqiSecondaryText
                                )
                            }
                        }

                        // Sort dropdown
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, SuqiBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    sortBy = if (sortBy == "POPULAR") "PRICE_LOW_HIGH" else "POPULAR"
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (sortBy == "POPULAR") "Sort: Best" else "Sort: Price",
                                fontFamily = SuqiFontFamily,
                                fontSize = 12.sp,
                                color = SuqiPrimaryText
                            )
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                if (selectedTab == 0) {
                    // Products List
                    if (productResults.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No products found for \"$searchQuery\"",
                                    fontFamily = SuqiFontFamily,
                                    color = SuqiSecondaryText
                                )
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
                                        performSearch(searchQuery)
                                    }
                                }
                            )
                        }
                    }
                } else {
                    // Shops List
                    if (shopResults.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No shops found for \"$searchQuery\"",
                                    fontFamily = SuqiFontFamily,
                                    color = SuqiSecondaryText
                                )
                            }
                        }
                    } else {
                        items(shopResults) { shop ->
                            ShopListItemRow(
                                shop = shop,
                                onClick = { viewModel.navigate(ScreenDestination.ShopStorefront(shop.id)) },
                                onToggleFollow = {
                                    coroutineScope.launch {
                                        viewModel.repository.toggleFollowShop(shop.id)
                                        performSearch(searchQuery)
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

@Composable
fun SearchResultProductRow(
    product: ProductDto,
    onClick: () -> Unit,
    onToggleWishlist: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
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
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SuqiSecondaryBackground)
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
                    color = SuqiPrimaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = SuqiWarning, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${product.rating} (${product.reviewCount})",
                        fontFamily = SuqiFontFamily,
                        fontSize = 12.sp,
                        color = SuqiSecondaryText
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${product.shopName}",
                        fontFamily = SuqiFontFamily,
                        fontSize = 12.sp,
                        color = SuqiMutedText
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatEtb(product.price),
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = SuqiPrimary
                    )
                    if (product.discountPercent > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "-${product.discountPercent}%",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = SuqiError
                        )
                    }
                }
            }

            IconButton(onClick = onToggleWishlist) {
                Icon(
                    imageVector = if (product.isWishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Wishlist",
                    tint = if (product.isWishlisted) SuqiPrimary else SuqiSecondaryText
                )
            }
        }
    }
}
