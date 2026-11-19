package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProductDto
import com.example.data.model.PromoCodeDto
import com.example.data.model.ShopDto
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.SuqiButton
import com.example.ui.components.SuqiCouponTicket
import com.example.ui.components.SuqiEmptyState
import com.example.ui.components.SuqiMaxContentWidth
import com.example.ui.components.SuqiProductCard
import com.example.ui.components.SuqiScreen
import com.example.ui.components.SuqiSectionCard
import com.example.ui.components.SuqiSkeletonList
import com.example.ui.components.SuqiTextField
import com.example.ui.components.formatEtb
import com.example.ui.components.suqiGridColumns
import com.example.ui.i18n.LocalSuqiLanguage
import com.example.ui.i18n.localizedName
import com.example.ui.i18n.suqiStrings
import com.example.ui.theme.LocalSuqiColors
import com.example.ui.theme.LucideIcons
import com.example.ui.theme.SuqiError
import com.example.ui.theme.SuqiFontFamily
import com.example.ui.theme.SuqiPrimary
import kotlinx.coroutines.launch

// ==========================================
// B-13 (P3): Wishlist 2.0 (wired to real wishlisted products + Move to Cart + Remove)
// ==========================================
@Composable
fun BuyerWishlistScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val p6 = strings.admin6
    val lang = LocalSuqiLanguage.current
    val isOffline by viewModel.isOffline.collectAsState()
    var wishlisted by remember { mutableStateOf<List<ProductDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun loadWishlist() {
        coroutineScope.launch {
            val res = viewModel.repository.getProducts()
            if (res.success && res.data != null) {
                wishlisted = res.data.filter { it.isWishlisted }
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadWishlist() }

    SuqiScreen(
        title = strings.wishlistTitle,
        subtitle = "${wishlisted.size}",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading && wishlisted.isEmpty()) {
                SuqiSkeletonList(count = 3, modifier = Modifier.padding(16.dp))
            } else if (wishlisted.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    SuqiEmptyState(
                        icon = LucideIcons.Heart,
                        title = strings.wishlistTitle,
                        subtitle = p6.wishlistEmptySub,
                        testTag = "wishlist_empty"
                    )
                }
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val columns = suqiGridColumns(maxWidth)
                    val rows = wishlisted.chunked(columns)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(rows, key = { chunk -> "wl_${chunk.joinToString("_") { it.id }}" }) { chunk ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                chunk.forEach { product ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        SuqiSectionCard(
                                            padding = 12.dp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("wishlist_item_${product.id}")
                                        ) {
                                            SuqiProductCard(
                                                product = product,
                                                onClick = { viewModel.navigate(ScreenDestination.ProductDetails(product.id)) },
                                                onAddToCart = { viewModel.quickAddToCart(product) },
                                                onShopClick = { viewModel.navigate(ScreenDestination.ShopStorefront(product.shopId)) },
                                                onToggleWishlist = {
                                                    coroutineScope.launch {
                                                        viewModel.repository.toggleWishlist(product.id)
                                                        loadWishlist()
                                                    }
                                                }
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                SuqiButton(
                                                    text = p6.wishlistMoveToCartBtn,
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            val res = viewModel.repository.changeCartLine(product.id, 1)
                                                            if (res.success) {
                                                                viewModel.showToast(strings.addedToCart)
                                                            } else {
                                                                viewModel.showToast(res.message ?: strings.common.failedToAddToast)
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .heightIn(min = 48.dp)
                                                        .testTag("wishlist_move_to_cart_${product.id}"),
                                                    isPrimary = false
                                                )
                                                IconButton(
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            viewModel.repository.toggleWishlist(product.id)
                                                            loadWishlist()
                                                            viewModel.showToast(strings.common.doneLabel)
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .testTag("wishlist_remove_${product.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = LucideIcons.Trash2,
                                                        contentDescription = "${strings.deleteLabel} ${product.localizedName(lang)}",
                                                        tint = SuqiError,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                repeat(columns - chunk.size) {
                                    Spacer(modifier = Modifier.weight(1f))
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
// B-15 (P3): Coupons & wallet (real repository promos across active shops)
// ==========================================
data class ShopPromoEntry(
    val shop: ShopDto,
    val promo: PromoCodeDto
)

@Composable
fun BuyerCouponsScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val p6 = strings.admin6
    val isOffline by viewModel.isOffline.collectAsState()
    var couponCode by remember { mutableStateOf("") }
    var promos by remember { mutableStateOf<List<ShopPromoEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val shopsRes = viewModel.repository.getShops()
        val shops = shopsRes.data.orEmpty()
        val aggregated = mutableListOf<ShopPromoEntry>()
        shops.forEach { shop ->
            val pRes = viewModel.repository.getShopPromotions(shop.id, includeInactive = false)
            if (pRes.success && pRes.data != null) {
                pRes.data.filter { it.isActive }.forEach { promo ->
                    aggregated.add(ShopPromoEntry(shop = shop, promo = promo))
                }
            }
        }
        promos = aggregated
        isLoading = false
    }

    fun applyCoupon() {
        val trimmed = couponCode.trim()
        if (trimmed.isBlank()) {
            viewModel.showToast(strings.checkout.enterPromoFirstToast)
            return
        }
        coroutineScope.launch {
            val matched = promos.firstOrNull { it.promo.code.equals(trimmed, ignoreCase = true) }
            val targetShopId = matched?.shop?.id ?: promos.firstOrNull()?.shop?.id.orEmpty()
            val checkSubtotal = matched?.promo?.minOrderAmount ?: 0.0
            val res = viewModel.repository.validatePromoCode(
                shopId = targetShopId,
                code = trimmed,
                subtotal = maxOf(1.0, checkSubtotal)
            )
            if (res.success && res.data != null) {
                viewModel.activeCheckoutShopPromoCode.value = res.data
                viewModel.showToast(strings.checkout.promoAppliedTemplate.format(res.data.code))
            } else {
                viewModel.showToast(res.message ?: strings.checkout.invalidPromoToast)
            }
        }
    }

    SuqiScreen(
        title = strings.couponsTitle,
        subtitle = "${promos.size}",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = SuqiMaxContentWidth)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            testTag = "buyer_coupon_apply_btn"
                        )
                    }
                }

                if (isLoading) {
                    item {
                        SuqiSkeletonList(count = 2)
                    }
                } else if (promos.isEmpty()) {
                    item {
                        SuqiEmptyState(
                            icon = LucideIcons.TicketPercent,
                            title = strings.couponsTitle,
                            subtitle = p6.couponsEmptySub,
                            testTag = "coupons_empty"
                        )
                    }
                } else {
                    items(promos, key = { "${it.shop.id}_${it.promo.id}" }) { entry ->
                        val promo = entry.promo
                        val discountText = when (promo.discountType.uppercase()) {
                            "PERCENTAGE" -> "${promo.discountValue.toInt()}% OFF"
                            "FREE_DELIVERY" -> strings.phase5.freeDeliveryOption
                            else -> formatEtb(promo.discountValue)
                        }
                        val minText = if (promo.minOrderAmount > 0) {
                            "${entry.shop.name} • ${strings.checkout.minOrderPromoTemplate.format(formatEtb(promo.minOrderAmount))}"
                        } else {
                            entry.shop.name
                        }
                        SuqiCouponTicket(
                            code = promo.code,
                            discountLabel = discountText,
                            title = entry.shop.name,
                            conditions = minText,
                            expiry = promo.validUntil.ifBlank { strings.adminOps.activeStatus },
                            onCopy = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Suqi Promo", promo.code))
                                couponCode = promo.code
                                viewModel.showToast(strings.common.copiedToast)
                            },
                            onPrimaryAction = {
                                viewModel.activeCheckoutShopPromoCode.value = promo
                                viewModel.navigate(ScreenDestination.ShopStorefront(entry.shop.id))
                            },
                            statusLabel = if (promo.isActive) strings.adminOps.activeStatus else null,
                            modifier = Modifier.testTag("coupon_ticket_${promo.code}")
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// B-19 (P3): Compare products (backed by SuqiViewModel.compareProducts)
// ==========================================
@Composable
fun BuyerCompareScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val p6 = strings.admin6
    val lang = LocalSuqiLanguage.current
    val isOffline by viewModel.isOffline.collectAsState()
    val compared by viewModel.compareProducts.collectAsState()

    SuqiScreen(
        title = p6.compareTitle,
        subtitle = "${compared.size}",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (compared.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    SuqiEmptyState(
                        icon = LucideIcons.SlidersHorizontal,
                        title = p6.compareTitle,
                        subtitle = p6.compareEmptySub,
                        testTag = "compare_empty"
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = SuqiMaxContentWidth)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            compared.forEach { product ->
                                SuqiSectionCard(
                                    padding = 14.dp,
                                    containerColor = colors.cardBackground,
                                    modifier = Modifier
                                        .width(220.dp)
                                        .testTag("compare_card_${product.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = product.shopName,
                                            fontFamily = SuqiFontFamily,
                                            fontSize = 11.sp,
                                            color = colors.secondaryText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { viewModel.removeCompareProduct(product.id) },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("compare_remove_${product.id}")
                                        ) {
                                            Icon(
                                                imageVector = LucideIcons.X,
                                                contentDescription = p6.removeFromCompareLabel,
                                                tint = colors.secondaryText,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = product.localizedName(lang),
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = colors.primaryText,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = formatEtb(product.price),
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = SuqiPrimary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = colors.border)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    CompareMetricRow(strings.customerReviews, "${product.rating} (${product.reviewCount})")
                                    CompareMetricRow(strings.seller.stockLabel, "${product.stockQuantity}")
                                    val specSummary = product.details.specifications.take(2)
                                        .joinToString(" • ") { "${it.label}: ${product.details.displayValue(it)}" }
                                    if (specSummary.isNotBlank()) {
                                        CompareMetricRow(p6.compareSpecsLabel, specSummary)
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    SuqiButton(
                                        text = strings.addToCart,
                                        onClick = { viewModel.quickAddToCart(product) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("compare_add_cart_${product.id}"),
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
}

@Composable
private fun CompareMetricRow(label: String, value: String) {
    val colors = LocalSuqiColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontFamily = SuqiFontFamily,
            fontSize = 11.sp,
            color = colors.secondaryText,
            modifier = Modifier.weight(0.42f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            fontFamily = SuqiFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            color = colors.primaryText,
            modifier = Modifier.weight(0.58f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
