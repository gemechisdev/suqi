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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProductDto
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListingScreen(
    categoryId: String,
    categoryTitle: String,
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val cartCount by viewModel.cartCount.collectAsState(initial = 0)

    var products by remember { mutableStateOf<List<ProductDto>>(emptyList()) }
    var selectedSubCategory by remember { mutableStateOf("All") }
    var sortBy by remember { mutableStateOf("POPULAR") }
    var inStockOnly by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    val subCategories = when (categoryId) {
        "cat_electronics" -> listOf("All", "Laptops", "Phones", "Audio", "Smart Watches", "Accessories")
        "cat_fashion" -> listOf("All", "Men", "Women", "Shoes", "Bags", "Traditional Habesha", "Watches")
        "cat_home" -> listOf("All", "Kitchen", "Living Room", "Bedding", "Home Decor", "Lighting")
        "cat_beauty" -> listOf("All", "Skincare", "Fragrance", "Haircare", "Makeup", "Personal Care")
        "cat_furniture" -> listOf("All", "Sofas", "Desks & Chairs", "Tables", "Beds", "Storage")
        "cat_sports" -> listOf("All", "Fitness Equipment", "Sportswear", "Football", "Outdoor & Running")
        "cat_toys" -> listOf("All", "Action Figures", "Board Games", "Educational", "Plush Toys", "Baby")
        "cat_groceries" -> listOf("All", "Coffee & Tea", "Ethiopian Spices", "Fresh Produce", "Pantry", "Snacks")
        "cat_health" -> listOf("All", "Supplements", "First Aid", "Wellness", "Personal Hygiene", "Medical Supplies")
        else -> listOf("All", "Popular", "Featured", "Deals", "New")
    }

    fun loadProducts() {
        coroutineScope.launch {
            val res = viewModel.repository.getProducts(
                categoryId = if (categoryId == "all") null else categoryId,
                subCategory = if (selectedSubCategory == "All") null else selectedSubCategory,
                inStockOnly = inStockOnly,
                sortBy = sortBy
            )
            if (res.success && !res.data.isNullOrEmpty()) {
                products = res.data
            } else if (selectedSubCategory != "All") {
                val catRes = viewModel.repository.getProducts(
                    categoryId = if (categoryId == "all") null else categoryId,
                    inStockOnly = inStockOnly,
                    sortBy = sortBy
                )
                if (catRes.success && catRes.data != null) {
                    val matching = catRes.data.filter {
                        it.subCategory?.contains(selectedSubCategory, ignoreCase = true) == true ||
                        it.name.contains(selectedSubCategory, ignoreCase = true) ||
                        it.description.contains(selectedSubCategory, ignoreCase = true)
                    }
                    products = if (matching.isNotEmpty()) matching else catRes.data
                }
            } else if (res.success && res.data != null) {
                products = res.data
            }
        }
    }

    LaunchedEffect(categoryId, selectedSubCategory, sortBy, inStockOnly) {
        loadProducts()
    }

    Scaffold(
        topBar = {
            SuqiTopHeader(
                title = categoryTitle,
                cartCount = cartCount,
                onBackClick = { viewModel.goBack() },
                onCartClick = { viewModel.navigate(ScreenDestination.Cart) }
            )
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
            // Subcategory Chips Row
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(subCategories) { sub ->
                        val isSelected = sub == selectedSubCategory
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (isSelected) SuqiPrimary else SuqiSecondaryBackground)
                                .clickable { selectedSubCategory = sub }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = sub,
                                fontFamily = SuqiFontFamily,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 13.sp,
                                color = if (isSelected) Color.White else SuqiPrimaryText
                            )
                        }
                    }
                }
            }

            // Results summary + Sort / Filter row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${products.size} products found",
                        fontFamily = SuqiFontFamily,
                        fontSize = 13.sp,
                        color = SuqiSecondaryText
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Filter button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, SuqiBorder, RoundedCornerShape(12.dp))
                                .clickable { showFilterSheet = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = LucideIcons.SlidersHorizontal,
                                contentDescription = "Filter",
                                tint = SuqiPrimaryText,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Filter",
                                fontFamily = SuqiFontFamily,
                                fontSize = 13.sp,
                                color = SuqiPrimaryText
                            )
                        }
                    }
                }
            }

            if (products.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = LucideIcons.SearchX,
                            contentDescription = null,
                            tint = SuqiMutedText,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No products found",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = SuqiPrimaryText
                        )
                        Text(
                            text = "Try switching subcategories or adjusting your filter.",
                            fontFamily = SuqiFontFamily,
                            fontSize = 13.sp,
                            color = SuqiSecondaryText
                        )
                    }
                }
            } else {
                // Product Grid (2 columns)
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
                                    onToggleWishlist = {
                                        coroutineScope.launch {
                                            viewModel.repository.toggleWishlist(prod.id)
                                            loadProducts()
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

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            containerColor = LocalSuqiColors.current.cardBackground,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Sort & Filter",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = SuqiPrimaryText
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Sort By",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                val sortOptions = listOf(
                    "POPULAR" to "Best Match / Popular",
                    "PRICE_LOW_HIGH" to "Price: Low to High",
                    "PRICE_HIGH_LOW" to "Price: High to Low",
                    "RATING" to "Top Rated",
                    "DISCOUNT" to "Highest Discount"
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sortOptions.forEach { (key, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (sortBy == key) SuqiPrimaryTint else Color.Transparent)
                                .clickable { sortBy = key }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            RadioButton(
                                selected = sortBy == key,
                                onClick = { sortBy = key },
                                colors = RadioButtonDefaults.colors(selectedColor = SuqiPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                fontFamily = SuqiFontFamily,
                                fontWeight = if (sortBy == key) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 14.sp,
                                color = if (sortBy == key) SuqiPrimary else SuqiPrimaryText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "In Stock Only",
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Switch(
                        checked = inStockOnly,
                        onCheckedChange = { inStockOnly = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SuqiPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                SuqiButton(
                    text = "Apply Filters",
                    onClick = { showFilterSheet = false },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
