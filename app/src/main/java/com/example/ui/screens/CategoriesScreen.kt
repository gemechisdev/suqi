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
    val cartCount by viewModel.cartCount.collectAsState(initial = 0)
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
            ),
            CategoryCardSpec(
                id = "cat_vehicles",
                name = "Vehicles",
                cleanName = "Vehicles",
                imageUrl = "https://images.unsplash.com/photo-1494976388531-d1058494cdd8?w=600",
                gradientColors = listOf(Color(0xFF334155), Color(0xFF475569), Color(0xFF64748B)),
                filterTag = "Vehicles"
            ),
            CategoryCardSpec(
                id = "cat_kids",
                name = "Kids & Toys",
                cleanName = "Kids & Toys",
                imageUrl = "https://images.unsplash.com/photo-1596461404969-9ae70f2830c1?w=600",
                gradientColors = listOf(Color(0xFFB45309), Color(0xFFD97706), Color(0xFFF59E0B)),
                filterTag = "Kids & Toys"
            ),
            CategoryCardSpec(
                id = "cat_books",
                name = "Books & Media",
                cleanName = "Books & Media",
                imageUrl = "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=600",
                gradientColors = listOf(Color(0xFF7C2D12), Color(0xFF9A3412), Color(0xFFC2410C)),
                filterTag = "Books & Media"
            )
        )
    }

    val colors = LocalSuqiColors.current
    val pageBg = if (colors.isDark) colors.surfaceBackground else SuqiCream

    Scaffold(
        topBar = {
            // Reference image top bar: Clean title "Categories" with Search and Cart with badge
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(pageBg)
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
                                .suqiWarmShadow(CircleShape, elevation = 6.dp)
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(colors.cardBackground)
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
                                .suqiWarmShadow(CircleShape, elevation = 6.dp)
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(colors.cardBackground)
                                .clickable { viewModel.navigate(ScreenDestination.Cart) }
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
                                        .offset(x = 2.dp, y = (-2).dp)
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
        containerColor = pageBg,
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
                        .padding(horizontal = 16.dp, vertical = 8.dp),
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

private fun categorySubtitle(id: String): String = when (id) {
    "cat_electronics" -> "Phones, laptops & more"
    "cat_fashion" -> "Clothes, shoes & bags"
    "cat_home" -> "Kitchen, decor & bedding"
    "cat_beauty" -> "Skincare & fragrance"
    "cat_furniture" -> "Sofas, beds & tables"
    "cat_sports" -> "Fitness & activewear"
    "cat_toys" -> "Toys, games & kids"
    "cat_groceries" -> "Coffee, spices & snacks"
    "cat_health" -> "Wellness & fitness"
    "cat_vehicles" -> "Cars, bikes & parts"
    "cat_kids" -> "Toys & baby items"
    "cat_books" -> "Books & instruments"
    else -> "Browse all"
}

@Composable
private fun CategoryCardItem(
    item: CategoryCardSpec,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val cardShape = RoundedCornerShape(SuqiRadius.Card)
    val label = if (item.cleanName == "Beauty & Personal Care") "Beauty & Care" else item.cleanName

    Column(
        modifier = modifier
            .suqiWarmShadow(cardShape, elevation = 10.dp)
            .clip(cardShape)
            .background(colors.cardBackground)
            .clickable(onClick = onClick)
            .padding(8.dp)
            .testTag("category_card_${item.id}")
    ) {
        // Large image tile
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.25f)
                .clip(RoundedCornerShape(18.dp))
                .background(if (colors.isDark) colors.secondaryBackground else SuqiImageBackdrop)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = item.cleanName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Name + hint + orange arrow
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 2.dp, top = 10.dp, bottom = 2.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = colors.primaryText,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = categorySubtitle(item.id),
                    fontFamily = SuqiFontFamily,
                    fontSize = 11.sp,
                    color = colors.secondaryText,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(SuqiPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = LucideIcons.ArrowRight,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
