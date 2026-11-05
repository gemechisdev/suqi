package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
    var cartItems by remember { mutableStateOf<List<CartItemDto>>(emptyList()) }
    var showClearDialog by remember { mutableStateOf(false) }

    fun refreshCart() {
        coroutineScope.launch {
            val res = viewModel.repository.getCartItems()
            if (res.success && res.data != null) {
                cartItems = res.data
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshCart()
    }

    // Group items by shop (Checklist Item 06 requirement!)
    val groupedByShop = cartItems.groupBy { it.shopId to it.shopName }
    val totalAllShops = cartItems.sumOf { it.unitPrice * it.quantity }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.goBack() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SuqiSecondaryBackground)
                ) {
                    Icon(LucideIcons.ArrowLeft, contentDescription = "Back", tint = SuqiPrimaryText)
                }

                Text(
                    text = "My Cart",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    color = SuqiPrimaryText
                )

                if (cartItems.isNotEmpty()) {
                    IconButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SuqiSecondaryBackground)
                    ) {
                        Icon(LucideIcons.Trash2, contentDescription = "Clear Cart", tint = SuqiPrimaryText)
                    }
                } else {
                    Spacer(modifier = Modifier.width(40.dp))
                }
            }
        },
        containerColor = SuqiBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(SuqiPrimaryTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = LucideIcons.ShoppingCart,
                            contentDescription = null,
                            tint = SuqiPrimary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your Cart is Empty",
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                        color = SuqiPrimaryText
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Explore amazing deals from independent shops across Ethiopia.",
                        fontFamily = SuqiFontFamily,
                        fontSize = 14.sp,
                        color = SuqiSecondaryText,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    SuqiButton(
                        text = "Start Shopping",
                        onClick = { viewModel.navigate(ScreenDestination.Home) }
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                groupedByShop.forEach { (shopKey, itemsInShop) ->
                    val (shopId, shopName) = shopKey
                    val shopSubtotal = itemsInShop.sumOf { it.unitPrice * it.quantity }

                    item(key = shopId) {
                        ShopCartGroupCard(
                            shopId = shopId,
                            shopName = shopName,
                            items = itemsInShop,
                            subtotal = shopSubtotal,
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
                                // Setup active checkout for this shop
                                viewModel.activeCheckoutShopId.value = shopId
                                viewModel.activeCheckoutShopName.value = shopName
                                viewModel.activeCheckoutItems.value = itemsInShop
                                viewModel.activeCheckoutShopPromoCode.value = promoCodeDto
                                viewModel.navigate(ScreenDestination.CheckoutDelivery(shopId))
                            }
                        )
                    }
                }

                // Marketplace summary footer
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Marketplace Multi-Vendor Note",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = SuqiSecondaryText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Each shop operates independently and has its own payment and delivery options. You can checkout each shop separately without delay.",
                            fontFamily = SuqiFontFamily,
                            fontSize = 12.sp,
                            color = SuqiMutedText
                        )
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text(
                    text = "Clear Cart?",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove all items from your cart?",
                    fontFamily = SuqiFontFamily
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
                    Text("Clear All", color = SuqiError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = SuqiPrimaryText)
                }
            },
            containerColor = LocalSuqiColors.current.cardBackground,
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
    onUpdateQty: (Long, Int) -> Unit,
    onRemoveItem: (Long) -> Unit,
    onCheckoutShop: (PromoCodeDto?) -> Unit
) {
    var promoCodeInput by remember { mutableStateOf("") }
    var appliedPromo by remember { mutableStateOf<PromoCodeDto?>(null) }
    var promoError by remember { mutableStateOf<String?>(null) }

    val discountAmount = if (appliedPromo != null) {
        if (appliedPromo!!.discountType == "PERCENTAGE") {
            (subtotal * (appliedPromo!!.discountValue / 100.0))
        } else {
            appliedPromo!!.discountValue
        }
    } else 0.0

    val shopTotal = maxOf(0.0, subtotal - discountAmount)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SuqiSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(SuqiBorder)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Shop Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(SuqiPrimaryTint),
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
                        color = SuqiPrimaryText
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = LucideIcons.CheckCircle,
                        contentDescription = "Verified",
                        tint = SuqiPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = "(${items.size} ${if (items.size == 1) "item" else "items"})",
                    fontFamily = SuqiFontFamily,
                    fontSize = 13.sp,
                    color = SuqiSecondaryText
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = SuqiBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(14.dp))

            // Cart Items in this shop
            items.forEach { item ->
                CartItemRow(
                    item = item,
                    onUpdateQty = { onUpdateQty(item.id, it) },
                    onRemove = { onRemoveItem(item.id) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Low Stock Warning if any item has low stock
            val hasLowStock = items.any { it.stockAvailable in 1..3 }
            if (hasLowStock) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SuqiWarningBg)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(LucideIcons.TriangleAlert, contentDescription = null, tint = SuqiWarning, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Some items have limited stock. Stock is held upon order submission.",
                        fontFamily = SuqiFontFamily,
                        fontSize = 12.sp,
                        color = SuqiWarning
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
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
                            text = "Enter promo code",
                            fontFamily = SuqiFontFamily,
                            fontSize = 13.sp,
                            color = SuqiMutedText
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SuqiSecondaryBackground,
                        unfocusedContainerColor = SuqiSecondaryBackground,
                        focusedBorderColor = SuqiPrimary,
                        unfocusedBorderColor = SuqiBorder
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(max = 48.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (promoCodeInput.trim().equals("SUMMER25", ignoreCase = true)) {
                            appliedPromo = PromoCodeDto(
                                id = "promo_1",
                                shopId = shopId,
                                code = "SUMMER25",
                                discountType = "PERCENTAGE",
                                discountValue = 25.0,
                                minOrderAmount = 1000.0,
                                validUntil = "Jun 30, 2026",
                                isActive = true
                            )
                            promoError = null
                        } else if (promoCodeInput.trim().equals("TECH10", ignoreCase = true)) {
                            appliedPromo = PromoCodeDto(
                                id = "promo_2",
                                shopId = shopId,
                                code = "TECH10",
                                discountType = "PERCENTAGE",
                                discountValue = 10.0,
                                minOrderAmount = 5000.0,
                                validUntil = "Dec 31, 2026",
                                isActive = true
                            )
                            promoError = null
                        } else {
                            promoError = "Invalid or expired promo code"
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Apply", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold)
                }
            }

            if (appliedPromo != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(LucideIcons.Check, contentDescription = null, tint = SuqiSuccess, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Promo code ${appliedPromo!!.code} applied (-${formatEtb(discountAmount)})",
                        fontFamily = SuqiFontFamily,
                        fontSize = 12.sp,
                        color = SuqiSuccess
                    )
                }
            }
            if (promoError != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = promoError!!, fontSize = 12.sp, color = SuqiError)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subtotal Line
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Subtotal (${items.sumOf { it.quantity }} items)",
                    fontFamily = SuqiFontFamily,
                    fontSize = 14.sp,
                    color = SuqiSecondaryText
                )
                Text(
                    text = formatEtb(subtotal),
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = SuqiPrimaryText
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
                        text = "Discount",
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

            // Checkout button for this shop
            Button(
                onClick = { onCheckoutShop(appliedPromo) },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("checkout_shop_${shopId}")
            ) {
                Text(
                    text = "Checkout • ${formatEtb(shopTotal)}",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItemDto,
    onUpdateQty: (Int) -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SuqiSecondaryBackground)
        ) {
            AsyncImage(
                model = item.productImageUrl,
                contentDescription = item.productName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.productName,
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = SuqiPrimaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (item.selectedVariation.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.selectedVariation,
                    fontFamily = SuqiFontFamily,
                    fontSize = 12.sp,
                    color = SuqiSecondaryText
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = formatEtb(item.unitPrice),
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = SuqiPrimary
            )
        }

        // Quantity controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(SuqiSecondaryBackground)
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            IconButton(
                onClick = {
                    if (item.quantity > 1) {
                        onUpdateQty(item.quantity - 1)
                    } else {
                        onRemove()
                    }
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (item.quantity == 1) LucideIcons.Trash2 else LucideIcons.Minus,
                    contentDescription = "Decrease",
                    tint = SuqiPrimaryText,
                    modifier = Modifier.size(14.dp)
                )
            }

            Text(
                text = item.quantity.toString(),
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = SuqiPrimaryText,
                modifier = Modifier.padding(horizontal = 6.dp)
            )

            IconButton(
                onClick = { onUpdateQty(item.quantity + 1) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = LucideIcons.Plus,
                    contentDescription = "Increase",
                    tint = SuqiPrimaryText,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
