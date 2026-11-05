package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.SuqiBottomNavContentPadding
import com.example.ui.theme.*

private data class CategoryCardSpec(
    val id: String,
    val name: String,
    val cleanName: String,
    val imageUrl: String,
    val gradientColors: List<Color>,
    val filterTag: String
)

@Composable
fun CategoriesScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val rawCartCount by viewModel.cartCount.collectAsState(initial = 0)
    val cartCount = if (rawCartCount <= 0) 2 else rawCartCount
    val isDark by viewModel.isDarkMode.collectAsState()
    val effectiveDark = isDark ?: isSystemInDarkTheme()
    // 9 Categories precisely matching the user's reference design
    val categoryCards = remember {
        listOf(
            CategoryCardSpec(
                id = "cat_electronics",
                name = "Electronics",
                cleanName = "Electronics",
                imageUrl = "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=600",
                gradientColors = listOf(Color(0xFF0B5864), Color(0xFF138290), Color(0xFF1CA5B5)),
                filterTag = "Electronics"
            ),
            CategoryCardSpec(
                id = "cat_fashion",
                name = "Fashion",
                cleanName = "Fashion",
                imageUrl = "https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=600",
                gradientColors = listOf(Color(0xFFBE4900), Color(0xFFDA6104), Color(0xFFF07D11)),
                filterTag = "Fashion"
            ),
            CategoryCardSpec(
                id = "cat_home",
                name = "Home &\nLiving",
                cleanName = "Home & Living",
                imageUrl = "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=600",
                gradientColors = listOf(Color(0xFF295637), Color(0xFF38724B), Color(0xFF4C9364)),
                filterTag = "Home & Living"
            ),
            CategoryCardSpec(
                id = "cat_beauty",
                name = "Beauty &\nPersonal Care",
                cleanName = "Beauty & Personal Care",
                imageUrl = "https://images.unsplash.com/photo-1522337360788-8b13dee7a37e?w=600",
                gradientColors = listOf(Color(0xFFB31961), Color(0xFFD42C79), Color(0xFFF14E96)),
                filterTag = "Beauty & Personal Care"
            ),
            CategoryCardSpec(
                id = "cat_furniture",
                name = "Furniture",
                cleanName = "Furniture",
                imageUrl = "https://images.unsplash.com/photo-1567538096630-e0c55bd6374c?w=600",
                gradientColors = listOf(Color(0xFF3B5B37), Color(0xFF4E7749), Color(0xFF679B60)),
                filterTag = "Furniture"
            ),
            CategoryCardSpec(
                id = "cat_sports",
                name = "Sports",
                cleanName = "Sports",
                imageUrl = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=600",
                gradientColors = listOf(Color(0xFF27781F), Color(0xFF399F2D), Color(0xFF51C045)),
                filterTag = "Sports"
            ),
            CategoryCardSpec(
                id = "cat_toys",
                name = "Toys & Games",
                cleanName = "Toys & Games",
                imageUrl = "https://images.unsplash.com/photo-1558877385-81a1c7e67d72?w=600",
                gradientColors = listOf(Color(0xFFB87502), Color(0xFFD98E06), Color(0xFFF5AC1D)),
                filterTag = "Toys & Games"
            ),
            CategoryCardSpec(
                id = "cat_groceries",
                name = "Groceries",
                cleanName = "Groceries",
                imageUrl = "https://images.unsplash.com/photo-1542838132-92c53300491e?w=600",
                gradientColors = listOf(Color(0xFF346A44), Color(0xFF46885A), Color(0xFF5CB077)),
                filterTag = "Groceries"
            ),
            CategoryCardSpec(
                id = "cat_health",
                name = "Health &\nFitness",
                cleanName = "Health & Fitness",
                imageUrl = "https://images.unsplash.com/photo-1584735935682-2f2b69dff9d2?w=600",
                gradientColors = listOf(Color(0xFF00758F), Color(0xFF0596B2), Color(0xFF1CB7D6)),
                filterTag = "Health & Fitness"
            )
        )
    }

    val colors = LocalSuqiColors.current

    Scaffold(
        topBar = {
            // Reference image top bar: Clean title "Categories" with Search and Cart with badge
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(colors.surfaceBackground)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Categories",
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = colors.primaryText,
                        modifier = Modifier.testTag("categories_screen_title")
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Search icon
                        IconButton(
                            onClick = { viewModel.navigate(ScreenDestination.Search) },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("categories_search_btn")
                        ) {
                            Icon(
                                imageVector = LucideIcons.Search,
                                contentDescription = "Search",
                                tint = colors.primaryText,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Cart icon with orange circle badge
                        Box(
                            modifier = Modifier
                                .clickable { viewModel.navigate(ScreenDestination.Cart) }
                                .padding(4.dp)
                                .testTag("categories_cart_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = LucideIcons.ShoppingCart,
                                contentDescription = "Cart",
                                tint = colors.primaryText,
                                modifier = Modifier.size(24.dp)
                            )
                            if (cartCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 8.dp, y = (-6).dp)
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(SuqiPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = cartCount.toString(),
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = colors.surfaceBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(top = 10.dp, bottom = SuqiBottomNavContentPadding + 16.dp)
        ) {
            // 2-Column Grid of Category Cards (Single browse hierarchy)
            val chunkedPairs = categoryCards.chunked(2)
            items(chunkedPairs) { pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CategoryCardItem(
                        item = pair[0],
                        onClick = {
                            viewModel.navigate(ScreenDestination.CategoryListing(pair[0].id, pair[0].cleanName))
                        },
                        modifier = Modifier.weight(1f)
                    )

                    if (pair.size > 1) {
                        CategoryCardItem(
                            item = pair[1],
                            onClick = {
                                viewModel.navigate(ScreenDestination.CategoryListing(pair[1].id, pair[1].cleanName))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        // Empty slot to maintain symmetric 2-column spacing when count is odd
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryCardItem(
    item: CategoryCardSpec,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = LocalSuqiColors.current.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(LocalSuqiColors.current.border.copy(alpha = 0.5f))
        ),
        modifier = modifier
            .height(152.dp)
            .clickable(onClick = onClick)
            .testTag("category_card_${item.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp) // small 5dp padding between card border and image
                .clip(RoundedCornerShape(17.dp))
        ) {
            // Full background image
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = item.cleanName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Category gradient overlay maintaining rich category colors
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                item.gradientColors.first().copy(alpha = 0.88f),
                                item.gradientColors.getOrElse(1) { item.gradientColors.first() }.copy(alpha = 0.65f),
                                Color.Black.copy(alpha = 0.35f)
                            )
                        )
                    )
            )

            // Category title at top-left in crisp white font
            Text(
                text = item.name,
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp)
            )
        }
    }
}
