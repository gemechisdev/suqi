package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

private data class CategoryDisplayItem(
    val id: String,
    val name: String,
    val imageUrl: String,
    val department: String
)

@Composable
fun CategoriesScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val cartCount by viewModel.cartCount.collectAsState(initial = 0)
    val isDark by viewModel.isDarkMode.collectAsState()
    val effectiveDark = isDark ?: isSystemInDarkTheme()
    val colors = LocalSuqiColors.current

    var selectedFilter by remember { mutableStateOf("All") }

    val filterPills = listOf("All", "Electronics", "Fashion", "Home & Living", "Beauty", "Sports")

    // Full 9-grid category catalog matching reference image
    val allCategories = remember {
        listOf(
            CategoryDisplayItem("cat_electronics", "Electronics", "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=300", "Electronics"),
            CategoryDisplayItem("cat_fashion", "Fashion", "https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=300", "Fashion"),
            CategoryDisplayItem("cat_home", "Home & Living", "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=300", "Home & Living"),
            CategoryDisplayItem("cat_beauty", "Beauty &\nPersonal Care", "https://images.unsplash.com/photo-1522337360788-8b13dee7a37e?w=300", "Beauty"),
            CategoryDisplayItem("cat_furniture", "Furniture", "https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=300", "Home & Living"),
            CategoryDisplayItem("cat_sports", "Sports", "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=300", "Sports"),
            CategoryDisplayItem("cat_toys", "Toys & Games", "https://images.unsplash.com/photo-1558877385-81a1c7e67d72?w=300", "Fashion"),
            CategoryDisplayItem("cat_groceries", "Groceries", "https://images.unsplash.com/photo-1542838132-92c53300491e?w=300", "Home & Living"),
            CategoryDisplayItem("cat_health", "Health &\nFitness", "https://images.unsplash.com/photo-1584735935682-2f2b69dff9d2?w=300", "Sports")
        )
    }

    val filteredCategories = remember(selectedFilter) {
        if (selectedFilter == "All") allCategories
        else allCategories.filter { it.department.equals(selectedFilter, ignoreCase = true) || it.name.contains(selectedFilter, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            SuqiTopHeader(
                title = "Categories",
                cartCount = cartCount,
                onCartClick = { viewModel.navigate(ScreenDestination.Cart) },
                showThemeToggle = true,
                isDarkTheme = effectiveDark,
                onToggleTheme = { viewModel.setDarkMode(!effectiveDark) },
                actions = {
                    IconButton(
                        onClick = { viewModel.navigate(ScreenDestination.Search) },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SuqiSecondaryBackground)
                            .testTag("categories_search_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
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
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Horizontal Filter Pills Row
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filterPills) { pill ->
                        val isSelected = selectedFilter == pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (isSelected) SuqiPrimary else SuqiSecondaryBackground)
                                .clickable { selectedFilter = pill }
                                .padding(horizontal = 18.dp, vertical = 9.dp)
                                .testTag("category_pill_$pill")
                        ) {
                            Text(
                                text = pill,
                                fontFamily = SuqiFontFamily,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (isSelected) Color.White else SuqiPrimaryText
                            )
                        }
                    }
                }
            }

            // 3-Column Grid of Category Cards
            val chunks = filteredCategories.chunked(3)
            items(chunks) { chunk ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (cat in chunk) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(126.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(SuqiSecondaryBackground)
                                .border(1.dp, SuqiBorder, RoundedCornerShape(22.dp))
                                .clickable {
                                    viewModel.navigate(ScreenDestination.CategoryListing(cat.id, cat.name.replace("\n", " ")))
                                }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(62.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(cat.imageUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = cat.name,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = cat.name,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    lineHeight = 15.sp,
                                    color = SuqiPrimaryText,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    if (chunk.size < 3) {
                        for (i in 0 until (3 - chunk.size)) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Promotional Shop Local Banner (matching reference design)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = if (effectiveDark) listOf(Color(0xFF261D17), Color(0xFF332014))
                                else listOf(Color(0xFFFFF1EB), Color(0xFFFFE0D0))
                            )
                        )
                        .border(
                            1.dp,
                            if (effectiveDark) Color(0xFF4D2B17) else Color(0xFFFFD5C0),
                            RoundedCornerShape(24.dp)
                        )
                        .clickable { viewModel.navigate(ScreenDestination.Shops) }
                        .padding(18.dp)
                        .testTag("categories_shop_local_banner")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text(
                                text = "Shop Local",
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = if (effectiveDark) Color(0xFFFF8A3D) else Color(0xFF171717)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Support Ethiopian\nBusinesses",
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = if (effectiveDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(SuqiPrimary)
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Explore Shops",
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }

                        // Ethiopian Landmark Image Illustration
                        Box(
                            modifier = Modifier
                                .weight(0.9f)
                                .height(95.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data("https://images.unsplash.com/photo-1578925518470-4def7a0f08bb?w=400")
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Ethiopian Landmark",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}
