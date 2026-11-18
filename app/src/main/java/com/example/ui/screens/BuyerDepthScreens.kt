package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CartItemDto
import com.example.data.model.ProductDto
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.StatusType
import com.example.ui.components.SuqiButton
import com.example.ui.components.SuqiEmptyState
import com.example.ui.components.SuqiScreen
import com.example.ui.components.SuqiSectionCard
import com.example.ui.components.SuqiStatusBadge
import com.example.ui.components.SuqiTextField
import com.example.ui.components.formatEtb
import com.example.ui.i18n.suqiStrings
import com.example.ui.theme.LocalSuqiColors
import com.example.ui.theme.LucideIcons
import com.example.ui.theme.SuqiError
import com.example.ui.theme.SuqiFontFamily
import com.example.ui.theme.SuqiPrimary
import com.example.ui.theme.SuqiSuccess
import com.example.ui.theme.SuqiWarning
import kotlinx.coroutines.launch

// ==========================================
// B-13 (P3): Wishlist 2.0
// ==========================================
@Composable
fun BuyerWishlistScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val isOffline by viewModel.isOffline.collectAsState()
    var wishlist by remember { mutableStateOf<List<CartItemDto>>(emptyList()) }

    fun loadWishlist() {
        coroutineScope.launch {
            val res = viewModel.repository.getCartItems()
            if (res.success && res.data != null) {
                wishlist = res.data
            }
        }
    }

    LaunchedEffect(Unit) { loadWishlist() }

    SuqiScreen(
        title = strings.wishlistTitle,
        subtitle = "${wishlist.size} items",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (wishlist.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    com.example.ui.components.SuqiEmptyState(
                        icon = LucideIcons.Heart,
                        title = strings.wishlistTitle,
                        subtitle = strings.emptyWishlistSub,
                        testTag = "wishlist_empty"
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(wishlist, key = { it.id }) { item ->
                        SuqiSectionCard(
                            padding = 14.dp,
                            containerColor = colors.cardBackground,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = item.productName,
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = colors.primaryText
                                    )
                                    Text(
                                        text = item.shopName,
                                        fontSize = 12.sp,
                                        color = colors.secondaryText
                                    )
                                    Text(
                                        text = formatEtb(item.unitPrice * item.quantity),
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = SuqiPrimary
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SuqiButton(
                                        text = strings.buyer.addToCart,
                                        onClick = {
                                            viewModel.showToast("Added to cart")
                                        },
                                        modifier = Modifier.heightIn(min = 48.dp),
                                        isPrimary = false
                                    )
                                    Button(
                                        onClick = {
                                            viewModel.showToast("Removed from wishlist")
                                        },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(imageVector = LucideIcons.Trash2, contentDescription = "Delete", tint = SuqiError, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// B-15 (P3): Coupons & wallet
// ==========================================
@Composable
fun BuyerCouponsScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val isOffline by viewModel.isOffline.collectAsState()
    var couponCode by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    fun applyCoupon() {
        coroutineScope.launch {
            val res = viewModel.repository.validatePromoCode(
                shopId = "",
                code = couponCode.trim(),
                subtotal = 0.0
            )
            if (res.success) {
                viewModel.showToast("Coupon applied")
            } else {
                viewModel.showToast("Coupon not valid: ${res.message ?: "unknown"}")
            }
        }
    }

    SuqiScreen(
        title = strings.couponsTitle,
        subtitle = "Coupons strings.buyer.couponsSub Wallet",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SuqiSectionCard(padding = 16.dp, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = strings.promoCodeHint,
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = colors.primaryText
                )
                Spacer(modifier = Modifier.height(8.dp))
                SuqiTextField(
                    value = couponCode,
                    onValueChange = { couponCode = it },
                    label = strings.promoCodeHint,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "buyer_coupon_code"
                )
                Spacer(modifier = Modifier.height(12.dp))
                SuqiButton(
                    text = strings.applyLabel,
                    onClick = { applyCoupon() },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Column {
                val coupons = listOf(
                    "WELCOME10" to "Welcome Discount",
                    "SAVE20" to "20% Off Your Order",
                    "FREESHIP" to "Free Delivery"
                )
                coupons.forEachIndexed { index, (code, label) ->
                    SuqiSectionCard(
                        padding = 14.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = code,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = colors.primaryText
                                )
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    color = colors.secondaryText
                                )
                            }
                            SuqiStatusBadge(
                                if (index == 0) "Available" else "Used",
                                if (index == 0) StatusType.SUCCESS else StatusType.WARNING
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// B-19 (P3): Compare products
// ==========================================
@Composable
fun BuyerCompareScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val isOffline by viewModel.isOffline.collectAsState()
    var compared by remember { mutableStateOf<List<ProductDto>>(emptyList()) }

    SuqiScreen(
        title = strings.compareTitle,
        subtitle = "${compared.size} compared",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (compared.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    com.example.ui.components.SuqiEmptyState(
                        icon = LucideIcons.Compare,
                        title = strings.compareTitle,
                        subtitle = strings.compareEmptySub,
                        testTag = "compare_empty"
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(compared, key = { it.id }) { product ->
                        SuqiSectionCard(
                            padding = 14.dp,
                            containerColor = colors.cardBackground,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = product.name,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = colors.primaryText
                                )
                                Text(
                                    text = product.shopName,
                                    fontSize = 12.sp,
                                    color = colors.secondaryText
                                )
                                Text(
                                    text = formatEtb(product.price),
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = SuqiPrimary
                                )
                                Text(
                                    text = product.description.take(100),
                                    fontSize = 12.sp,
                                    color = colors.secondaryText,
                                    maxLines = 3
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                SuqiButton(
                                    text = strings.buyer.addToCart,
                                    onClick = { viewModel.showToast("Added to cart") },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                                    isPrimary = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
