package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
fun FollowingScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val cartCount by viewModel.cartCount.collectAsState(initial = 0)
    val colors = LocalSuqiColors.current

    var followedShops by remember { mutableStateOf<List<ShopDto>>(emptyList()) }
    var feedProducts by remember { mutableStateOf<List<ProductDto>>(emptyList()) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Followed Shops, 1: Activity Feed
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
                    allProdsRes.data.take(6)
                }
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    Scaffold(
        topBar = {
            SuqiTopHeader(
                title = "Following",
                cartCount = cartCount,
                onBackClick = { viewModel.goBack() },
                onCartClick = { viewModel.navigate(ScreenDestination.Cart) }
            )
        },
        containerColor = colors.surfaceBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Mode Tabs (Followed Shops vs Activity Feed)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.secondaryBackground)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(11.dp))
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
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (selectedTab == 1) SuqiPrimary else Color.Transparent)
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Latest Feed & Updates",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = if (selectedTab == 1) Color.White else colors.primaryText
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = SuqiPrimary)
                    }
                }
            } else if (followedShops.isEmpty() && selectedTab == 0) {
                // Empty state
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp, bottom = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(colors.primaryTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = LucideIcons.Store,
                                contentDescription = null,
                                tint = SuqiPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Not following any shops yet",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = colors.primaryText
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Follow your favorite Ethiopian merchants to see their new arrivals and deals right here.",
                            fontFamily = SuqiFontFamily,
                            fontSize = 13.sp,
                            color = colors.secondaryText,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        SuqiButton(
                            text = "Explore Verified Shops",
                            onClick = { viewModel.navigate(ScreenDestination.Shops) },
                            modifier = Modifier.width(220.dp)
                        )
                    }
                }
            } else if (selectedTab == 0) {
                // Followed Shops List
                items(followedShops) { shop ->
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { viewModel.navigate(ScreenDestination.ShopStorefront(shop.id)) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(colors.secondaryBackground)
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

                                // Unfollow / Follow button
                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.repository.toggleFollowShop(shop.id)
                                            loadData()
                                        }
                                    },
                                    shape = RoundedCornerShape(999.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SuqiPrimary),
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
                                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                ) {
                                    Text("Visit Storefront", fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.primaryText)
                                }

                                Button(
                                    onClick = { viewModel.navigate(ScreenDestination.ChatThread(shop.id)) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                ) {
                                    Icon(LucideIcons.MessageCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Chat", fontFamily = SuqiFontFamily, fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            } else {
                // Activity Feed & New Products from Followed Shops
                if (feedProducts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No new product updates from followed shops.", color = colors.secondaryText)
                        }
                    }
                } else {
                    val chunks = feedProducts.chunked(2)
                    items(chunks) { chunk ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (product in chunk) {
                                Box(modifier = Modifier.weight(1f)) {
                                    SuqiProductCard(
                                        product = product,
                                        onClick = { viewModel.navigate(ScreenDestination.ProductDetails(product.id)) },
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
