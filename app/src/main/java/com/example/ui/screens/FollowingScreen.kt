package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
fun FollowingScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val cartCount by viewModel.cartCount.collectAsState(initial = 0)
    val isOffline by viewModel.isOffline.collectAsState()
    val colors = LocalSuqiColors.current

    var followedShops by remember { mutableStateOf<List<ShopDto>>(emptyList()) }
    var feedProducts by remember { mutableStateOf<List<ProductDto>>(emptyList()) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    fun loadData() {
        coroutineScope.launch {
            isLoading = true
            val shopsRes = viewModel.repository.getShops(filter = "FOLLOWING")
            if (shopsRes.success && shopsRes.data != null) {
                followedShops = shopsRes.data
            }
            val allProdsRes = viewModel.repository.getProducts()
            if (allProdsRes.success && allProdsRes.data != null) {
                val followedIds = followedShops.map { it.id }.toSet()
                feedProducts = if (followedIds.isNotEmpty()) {
                    allProdsRes.data.filter { it.shopId in followedIds }
                } else {
                    emptyList()
                }
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    SuqiScreen(
        title = "Following",
        subtitle = "${followedShops.size} followed shops",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        headerActions = {
            IconButton(
                onClick = { viewModel.navigate(ScreenDestination.Cart) },
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.secondaryBackground)
                    .testTag("following_cart_button")
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
                    Icon(LucideIcons.ShoppingCart, contentDescription = "Cart", tint = colors.primaryText, modifier = Modifier.size(18.dp))
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.secondaryBackground)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedTab == 0) SuqiPrimary else Color.Transparent)
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Followed Shops (${followedShops.size})",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = if (selectedTab == 0) Color.White else colors.primaryText
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedTab == 1) SuqiPrimary else Color.Transparent)
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Latest Feed (${feedProducts.size})",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = if (selectedTab == 1) Color.White else colors.primaryText
                        )
                    }
                }
            }

            if (isLoading) {
                item {
                    SuqiSkeletonList(count = 4)
                }
            } else if (followedShops.isEmpty()) {
                item {
                    SuqiEmptyState(
                        icon = LucideIcons.Store,
                        title = "Not following any shops yet",
                        subtitle = "Follow Ethiopian merchants you trust to keep up with their new stock and flash discounts.",
                        ctaText = "Explore Verified Shops",
                        onCtaClick = { viewModel.navigate(ScreenDestination.Shops) },
                        testTag = "following_empty_state"
                    )
                }
            } else if (selectedTab == 0) {
                items(followedShops, key = { it.id }) { shop ->
                    SuqiSectionCard(
                        padding = 16.dp,
                        onClick = { viewModel.navigate(ScreenDestination.ShopStorefront(shop.id)) }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.imageBackdrop)
                            ) {
                                AsyncImage(
                                    model = shop.logoUrl,
                                    contentDescription = shop.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = shop.name,
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = colors.primaryText
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
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${shop.productCount} Products • ${shop.subCity}, ${shop.city}",
                                    fontFamily = SuqiFontFamily,
                                    fontSize = 12.sp,
                                    color = colors.secondaryText
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(LucideIcons.StarFilled, contentDescription = null, tint = SuqiWarning, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "${shop.rating} (${shop.reviewCount} reviews)",
                                        fontFamily = SuqiFontFamily,
                                        fontSize = 12.sp,
                                        color = colors.mutedText
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        viewModel.repository.toggleFollowShop(shop.id)
                                        loadData()
                                    }
                                },
                                shape = RoundedCornerShape(999.dp),
                                border = BorderStroke(1.dp, SuqiPrimary),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Following",
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = SuqiPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.navigate(ScreenDestination.ShopStorefront(shop.id)) },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, colors.border),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Text("Visit Storefront", fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.primaryText)
                            }

                            Button(
                                onClick = { viewModel.navigate(ScreenDestination.ChatThread(shop.id)) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Icon(LucideIcons.MessageCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Chat", fontFamily = SuqiFontFamily, fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
            } else {
                if (feedProducts.isEmpty()) {
                    item {
                        SuqiEmptyState(
                            icon = LucideIcons.ShoppingBag,
                            title = "No product updates yet",
                            subtitle = "New listings from your followed shops will appear here automatically."
                        )
                    }
                } else {
                    val chunks = feedProducts.chunked(2)
                    items(chunks) { chunk ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (product in chunk) {
                                Box(modifier = Modifier.weight(1f)) {
                                    SuqiProductCard(
                                        product = product,
                                        onClick = { viewModel.navigate(ScreenDestination.ProductDetails(product.id)) },
                                        onAddToCart = { viewModel.quickAddToCart(product) },
                                        onShopClick = { viewModel.navigate(ScreenDestination.ShopStorefront(product.shopId)) },
                                        onToggleWishlist = {
                                            coroutineScope.launch {
                                                viewModel.repository.toggleWishlist(product.id)
                                                loadData()
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
    }
}
