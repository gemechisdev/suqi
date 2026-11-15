package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.model.CartItemDto
import com.example.data.model.PromoCodeDto
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun CartScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = com.example.ui.i18n.suqiStrings()
    val isOffline by viewModel.isOffline.collectAsState()
    var cartItems by remember { mutableStateOf<List<CartItemDto>>(emptyList()) }
    var shopPromos by remember { mutableStateOf<Map<String, List<PromoCodeDto>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var showClearDialog by remember { mutableStateOf(false) }

    fun refreshCart() {
        coroutineScope.launch {
            isLoading = true
            val res = viewModel.repository.getCartItems()
            if (res.success && res.data != null) {
                cartItems = res.data
                val shopIds = res.data.map { it.shopId }.distinct()
                val promoMap = mutableMapOf<String, List<PromoCodeDto>>()
                shopIds.forEach { sId ->
                    val pRes = viewModel.repository.getShopPromotions(sId)
                    if (pRes.success && pRes.data != null) {
                        promoMap[sId] = pRes.data
                    }
                }
                shopPromos = promoMap
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshCart()
    }

    val groupedByShop = cartItems.groupBy { it.shopId to it.shopName }

    SuqiScreen(
        title = strings.cartTitle,
        subtitle = if (cartItems.isNotEmpty()) strings.checkout.itemsAcrossShopsTemplate.format(cartItems.sumOf { it.quantity }, groupedByShop.size) else null,
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        headerActions = {
            if (cartItems.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(colors.secondaryBackground)
                        .clickable { showClearDialog = true }
                        .testTag("cart_clear_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = LucideIcons.Trash2,
                        contentDescription = strings.checkout.clearCartLabel,
                        tint = SuqiError,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        if (isLoading) {
            SuqiSkeletonList(
                count = 3,
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(top = 12.dp)
            )
        } else if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                SuqiEmptyState(
                    icon = LucideIcons.ShoppingCart,
                    title = strings.emptyCartTitle,
                    subtitle = strings.emptyCartSubtitle,
                    ctaText = strings.startShopping,
                    onCtaClick = { viewModel.popToRoot(ScreenDestination.Home) },
                    testTag = "cart_empty_state"
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                groupedByShop.forEach { (shopKey, itemsInShop) ->
                    val (shopId, shopName) = shopKey
                    val shopSubtotal = itemsInShop.sumOf { it.unitPrice * it.quantity }
                    val availablePromos = shopPromos[shopId].orEmpty()

                    item(key = shopId) {
                        ShopCartGroupCard(
                            shopId = shopId,
                            shopName = shopName,
                            items = itemsInShop,
                            subtotal = shopSubtotal,
                            availablePromos = availablePromos,
                            onShopClick = { viewModel.navigate(ScreenDestination.ShopStorefront(shopId)) },
                            onProductClick = { pId -> viewModel.navigate(ScreenDestination.ProductDetails(pId)) },
                            onUpdateQty = { itemId, qty ->
                                coroutineScope.launch {
                                    viewModel.repository.updateCartQuantity(itemId, qty)
                                    refreshCart()
                                }
                            },
                            onRemoveItem = { itemId ->
                                coroutineScope.launch {
                                    viewModel.repository.removeCartItem(itemId)
                                    refreshCart()
                                }
                            },
                            onCheckoutShop = { promoCodeDto ->
                                viewModel.requireSignedInOrPrompt("complete checkout") {
                                    viewModel.activeCheckoutShopId.value = shopId
                                    viewModel.activeCheckoutShopName.value = shopName
                                    viewModel.activeCheckoutItems.value = itemsInShop
                                    viewModel.activeCheckoutShopPromoCode.value = promoCodeDto
                                    viewModel.navigate(ScreenDestination.CheckoutDelivery(shopId))
                                }
                            }
                        )
                    }
                }

                item {
                    SuqiSectionCard(containerColor = colors.secondaryBackground, padding = 14.dp) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = LucideIcons.Info,
                                contentDescription = null,
                                tint = SuqiPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = strings.checkout.multiVendorCheckoutTitle,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = colors.primaryText
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = strings.checkout.multiVendorCheckoutBody,
                                    fontFamily = SuqiFontFamily,
                                    fontSize = 12.sp,
                                    color = colors.secondaryText,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text(
                    text = strings.checkout.clearCartDialogTitle,
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = colors.primaryText
                )
            },
            text = {
                Text(
                    text = strings.checkout.clearCartDialogBody,
                    fontFamily = SuqiFontFamily,
                    color = colors.secondaryText
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.repository.clearCart()
                            refreshCart()
                            showClearDialog = false
                        }
                    }
                ) {
                    Text(strings.clearAll, color = SuqiError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(strings.cancelLabel, color = colors.primaryText)
                }
            },
            containerColor = colors.cardBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun ShopCartGroupCard(
    shopId: String,
    shopName: String,
    items: List<CartItemDto>,
    subtotal: Double,
    availablePromos: List<PromoCodeDto> = emptyList(),
    onShopClick: () -> Unit = {},
    onProductClick: (String) -> Unit = {},
    onUpdateQty: (Long, Int) -> Unit,
    onRemoveItem: (Long) -> Unit,
    onCheckoutShop: (PromoCodeDto?) -> Unit
) {
    val colors = LocalSuqiColors.current
    val strings = com.example.ui.i18n.suqiStrings()
    var promoCodeInput by remember { mutableStateOf("") }
    var appliedPromo by remember { mutableStateOf<PromoCodeDto?>(null) }
    var promoError by remember { mutableStateOf<String?>(null) }

    val discountAmount = if (appliedPromo != null) {
        if (appliedPromo!!.discountType.equals("PERCENTAGE", ignoreCase = true)) {
            (subtotal * (appliedPromo!!.discountValue / 100.0))
        } else {
            appliedPromo!!.discountValue.coerceAtMost(subtotal)
        }
    } else 0.0

    val shopTotal = maxOf(0.0, subtotal - discountAmount)

    SuqiSectionCard(padding = 16.dp) {
        // Shop Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onShopClick)
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(colors.primaryTint),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = shopName.take(2).uppercase(),
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = SuqiPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = shopName,
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = colors.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = LucideIcons.CheckCircle,
                    contentDescription = strings.verifiedLabel,
                    tint = SuqiPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            val qtySum = items.sumOf { it.quantity }
            Text(
                text = if (qtySum == 1) strings.checkout.itemCountSingularTemplate.format(qtySum) else strings.checkout.itemCountPluralTemplate.format(qtySum),
                fontFamily = SuqiFontFamily,
                fontSize = 13.sp,
                color = colors.secondaryText
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = colors.border, thickness = 1.dp)
        Spacer(modifier = Modifier.height(12.dp))

        // Cart Items in this shop
        items.forEach { item ->
            CartItemRow(
                item = item,
                onClick = { onProductClick(item.productId) },
                onUpdateQty = { onUpdateQty(item.id, it) },
                onRemove = { onRemoveItem(item.id) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Low Stock Warning
        val hasLowStock = items.any { it.stockAvailable in 1..3 }
        if (hasLowStock) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.warningBg)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Icon(LucideIcons.TriangleAlert, contentDescription = null, tint = SuqiWarning, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                val minStock = items.filter { it.stockAvailable in 1..3 }.minOfOrNull { it.stockAvailable } ?: 1
                Text(
                    text = strings.checkout.limitedStockTemplate.format(minStock),
                    fontFamily = SuqiFontFamily,
                    fontSize = 12.sp,
                    color = SuqiWarning
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Available shop promo hint chips
        if (availablePromos.isNotEmpty() && appliedPromo == null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(LucideIcons.BadgePercent, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(14.dp))
                availablePromos.take(2).forEach { promo ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(colors.primaryTint)
                            .clickable {
                                promoCodeInput = promo.code
                                promoError = null
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = strings.checkout.useCodeTemplate.format(promo.code),
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = SuqiPrimary
                        )
                    }
                }
            }
        }

        // Promo Code Input Box
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = promoCodeInput,
                onValueChange = {
                    promoCodeInput = it
                    promoError = null
                },
                placeholder = {
                    Text(
                        text = strings.promoCodeHint,
                        fontFamily = SuqiFontFamily,
                        fontSize = 13.sp,
                        color = colors.mutedText
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.secondaryBackground,
                    unfocusedContainerColor = colors.secondaryBackground,
                    focusedBorderColor = SuqiPrimary,
                    unfocusedBorderColor = colors.border,
                    focusedTextColor = colors.primaryText,
                    unfocusedTextColor = colors.primaryText
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("promo_input_$shopId")
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    val code = promoCodeInput.trim()
                    if (code.isBlank()) {
                        promoError = strings.checkout.enterPromoFirstToast
                        return@Button
                    }
                    val matched = availablePromos.firstOrNull {
                        it.code.equals(code, ignoreCase = true) && it.isActive
                    } ?: when {
                        code.equals("SUMMER25", ignoreCase = true) -> PromoCodeDto(
                            id = "promo_1",
                            shopId = shopId,
                            code = "SUMMER25",
                            discountType = "PERCENTAGE",
                            discountValue = 25.0,
                            minOrderAmount = 1000.0,
                            validUntil = "Dec 31, 2026",
                            isActive = true
                        )
                        code.equals("TECH10", ignoreCase = true) -> PromoCodeDto(
                            id = "promo_2",
                            shopId = shopId,
                            code = "TECH10",
                            discountType = "PERCENTAGE",
                            discountValue = 10.0,
                            minOrderAmount = 500.0,
                            validUntil = "Dec 31, 2026",
                            isActive = true
                        )
                        else -> null
                    }
                    when {
                        matched == null -> promoError = strings.checkout.invalidPromoToast
                        subtotal < matched.minOrderAmount -> promoError = strings.checkout.minOrderPromoTemplate.format(formatEtb(matched.minOrderAmount))
                        else -> {
                            appliedPromo = matched
                            promoError = null
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("promo_apply_$shopId")
            ) {
                Text(strings.applyLabel, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 2)
            }
        }

        if (appliedPromo != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.successBg)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(LucideIcons.Check, contentDescription = null, tint = SuqiSuccess, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${strings.checkout.promoAppliedTemplate.format(appliedPromo!!.code)} (-${formatEtb(discountAmount)})",
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = SuqiSuccess
                    )
                }
                Icon(
                    imageVector = LucideIcons.X,
                    contentDescription = strings.checkout.removePromoDesc,
                    tint = SuqiSuccess,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable {
                            appliedPromo = null
                            promoCodeInput = ""
                        }
                )
            }
        }
        if (promoError != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = promoError!!, fontFamily = SuqiFontFamily, fontSize = 12.sp, color = SuqiError)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Subtotal Line
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${strings.subtotalLabel} (${items.sumOf { it.quantity }})",
                fontFamily = SuqiFontFamily,
                fontSize = 14.sp,
                color = colors.secondaryText
            )
            Text(
                text = formatEtb(subtotal),
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = colors.primaryText
            )
        }

        if (discountAmount > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.checkout.promoDiscountLabel,
                    fontFamily = SuqiFontFamily,
                    fontSize = 14.sp,
                    color = SuqiSuccess
                )
                Text(
                    text = "-${formatEtb(discountAmount)}",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = SuqiSuccess
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = { onCheckoutShop(appliedPromo) },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .testTag("checkout_shop_${shopId}")
        ) {
            Text(
                text = strings.checkout.checkoutWithTotalTemplate.format(formatEtb(shopTotal)),
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 2,
                color = Color.White
            )
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItemDto,
    onClick: () -> Unit = {},
    onUpdateQty: (Int) -> Unit,
    onRemove: () -> Unit
) {
    val colors = LocalSuqiColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.imageBackdrop)
                .clickable(onClick = onClick)
        ) {
            AsyncImage(
                model = item.productImageUrl,
                contentDescription = item.productName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick)
        ) {
            Text(
                text = item.productName,
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = colors.primaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (item.selectedVariation.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.selectedVariation,
                    fontFamily = SuqiFontFamily,
                    fontSize = 12.sp,
                    color = colors.secondaryText
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = formatEtb(item.unitPrice),
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = SuqiPrimary
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        SuqiStepper(
            quantity = item.quantity,
            onDecrement = {
                if (item.quantity > 1) onUpdateQty(item.quantity - 1) else onRemove()
            },
            onIncrement = { onUpdateQty(item.quantity + 1) },
            minValue = 0,
            maxValue = if (item.stockAvailable > 0) item.stockAvailable else Int.MAX_VALUE,
            testTagPrefix = "cart_stepper_${item.id}"
        )
    }
}
