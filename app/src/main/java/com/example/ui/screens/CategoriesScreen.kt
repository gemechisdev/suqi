package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.CategoryDto
import com.example.data.model.MarketplaceCategories
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.SuqiBottomNavContentPadding
import com.example.ui.components.SuqiOfflineBanner
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun CategoriesScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val cartCount by viewModel.cartCount.collectAsState(initial = 0)
    val isOffline by viewModel.isOffline.collectAsState()
    var categories by remember {
        mutableStateOf(MarketplaceCategories.all.map { it.toDto() })
    }

    fun loadCategories() {
        coroutineScope.launch {
            val res = viewModel.repository.getCategories()
            if (res.success && !res.data.isNullOrEmpty()) {
                categories = res.data
                    .filter { it.isActive }
                    .sortedWith(compareBy({ it.sortOrder }, { it.name }))
            }
        }
    }

    LaunchedEffect(Unit) {
        loadCategories()
    }

    val colors = LocalSuqiColors.current
    val pageBg = if (colors.isDark) colors.surfaceBackground else SuqiCream

    Scaffold(
        topBar = {
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
                if (isOffline) {
                    SuqiOfflineBanner(onRetry = {
                        viewModel.setOfflineMode(false)
                        loadCategories()
                    })
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
            val chunkedPairs = categories.chunked(2)
            items(chunkedPairs, key = { pair -> pair.joinToString("_") { it.id } }) { pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CategoryCardItem(
                        category = pair[0],
                        onClick = {
                            viewModel.navigate(ScreenDestination.CategoryListing(pair[0].id, pair[0].name))
                        },
                        modifier = Modifier.weight(1f)
                    )

                    if (pair.size > 1) {
                        CategoryCardItem(
                            category = pair[1],
                            onClick = {
                                viewModel.navigate(ScreenDestination.CategoryListing(pair[1].id, pair[1].name))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryCardItem(
    category: CategoryDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val cardShape = RoundedCornerShape(SuqiRadius.Card)
    val resolvedImageUrl = category.imageUrl.ifBlank { MarketplaceCategories.imageUrlForId(category.id) }
    val subtitle = MarketplaceCategories.subtitleForId(category.id, category.subCategories)

    Column(
        modifier = modifier
            .suqiWarmShadow(cardShape, elevation = 10.dp)
            .clip(cardShape)
            .background(colors.cardBackground)
            .clickable(onClick = onClick)
            .padding(8.dp)
            .testTag("category_card_${category.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.25f)
                .clip(RoundedCornerShape(18.dp))
                .background(if (colors.isDark) colors.secondaryBackground else SuqiImageBackdrop)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(resolvedImageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = category.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 2.dp, top = 10.dp, bottom = 2.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = colors.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontFamily = SuqiFontFamily,
                    fontSize = 11.sp,
                    color = colors.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
