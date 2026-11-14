package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import android.widget.VideoView
import androidx.compose.ui.viewinterop.AndroidView
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import android.os.Build
import com.example.data.model.AttrType
import com.example.data.model.CartItemDto
import com.example.data.model.ColorSwatches
import com.example.data.model.ProductDetails
import com.example.data.model.ProductDto
import com.example.data.model.ReviewDto
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ProductDetailsScreen(
    productId: String,
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val cartCount by viewModel.cartCount.collectAsState(initial = 0)

    var product by remember { mutableStateOf<ProductDto?>(null) }
    var reviews by remember { mutableStateOf<List<ReviewDto>>(emptyList()) }
    // attribute key -> value the buyer picked (size, color, storage…)
    val chosen = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(productId) {
        val res = viewModel.repository.getProductDetails(productId)
        if (res.success && res.data != null) {
            val p = res.data
            product = p
            viewModel.recordProductView(p)
            chosen.clear()
            p.details.buyerOptions.forEach { opt ->
                p.details.valuesOf(opt.key).firstOrNull()?.let { chosen[opt.key] = it }
            }
        }
        val revRes = viewModel.repository.getReviews(productId)
        if (revRes.success && revRes.data != null) {
            reviews = revRes.data
        }
    }

    val currentProd = product ?: return Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) { CircularProgressIndicator(color = SuqiPrimary) }

    val details = currentProd.details
    val buyerOptions = details.buyerOptions
    val selectedVariant = details.variantFor(chosen.toMap())
    val variationLabel = ProductDetails.variantLabel(buyerOptions, chosen)
    val hasVariantPrice = selectedVariant?.price != null
    val displayPrice = selectedVariant?.price ?: currentProd.price
    val displayStock = selectedVariant?.stock ?: currentProd.stockQuantity

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
                    Icon(
                        imageVector = LucideIcons.ArrowLeft,
                        contentDescription = "Back",
                        tint = SuqiPrimaryText
                    )
                }

                Row {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                viewModel.repository.toggleWishlist(currentProd.id)
                                val refreshed = viewModel.repository.getProductDetails(currentProd.id)
                                if (refreshed.success && refreshed.data != null) {
                                    product = refreshed.data
                                }
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SuqiSecondaryBackground)
                    ) {
                        Icon(
                            imageVector = if (currentProd.isWishlisted) LucideIcons.HeartFilled else LucideIcons.Heart,
                            contentDescription = "Wishlist",
                            tint = if (currentProd.isWishlisted) SuqiPrimary else SuqiPrimaryText
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { viewModel.showToast("Product link copied!") },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SuqiSecondaryBackground)
                    ) {
                        Icon(LucideIcons.Share2, contentDescription = "Share", tint = SuqiPrimaryText)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { viewModel.navigate(ScreenDestination.Cart) },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SuqiSecondaryBackground)
                    ) {
                        BadgedBox(
                            badge = {
                                if (cartCount > 0) {
                                    Badge(containerColor = SuqiPrimary, contentColor = Color.White) {
                                        Text(
                                            text = cartCount.toString(),
                                            fontFamily = SuqiFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(LucideIcons.ShoppingCart, contentDescription = "Cart", tint = SuqiPrimaryText)
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Sticky Bottom Action Bar with Add to Cart and Buy Now
            Surface(
                color = LocalSuqiColors.current.cardBackground,
                shadowElevation = 16.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, SuqiBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val variation = variationLabel
                            coroutineScope.launch {
                                val res = viewModel.repository.addToCart(currentProd.id, 1, variation)
                                if (res.success) {
                                    viewModel.showToast("Added to Cart!")
                                } else {
                                    viewModel.showToast(res.message ?: "Failed to add")
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, SuqiPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("btn_add_to_cart")
                    ) {
                        Text(
                            text = "Add to Cart",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = SuqiPrimary
                        )
                    }

                    Button(
                        onClick = {
                            val variation = variationLabel
                            // Buy Now directly enters checkout flow
                            viewModel.activeCheckoutShopId.value = currentProd.shopId
                            viewModel.activeCheckoutShopName.value = currentProd.shopName
                            viewModel.activeCheckoutItems.value = listOf(
                                CartItemDto(
                                    id = 0,
                                    userId = "user_fitsum",
                                    productId = currentProd.id,
                                    shopId = currentProd.shopId,
                                    shopName = currentProd.shopName,
                                    shopVerified = currentProd.shopVerified,
                                    productName = currentProd.name,
                                    productImageUrl = selectedVariant?.imageUrl?.takeIf { it.isNotBlank() } ?: currentProd.primaryImageUrl,
                                    unitPrice = displayPrice,
                                    quantity = 1,
                                    selectedVariation = variation,
                                    stockAvailable = displayStock
                                )
                            )
                            viewModel.navigate(ScreenDestination.CheckoutDelivery(currentProd.shopId))
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("btn_buy_now")
                    ) {
                        Text(
                            text = "Buy Now",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }
            }
        },
        containerColor = SuqiBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Unified gallery: images, GIFs and videos (swipe, thumbnails, fullscreen)
            item {
                SuqiMediaGallery(
                    media = details.media,
                    contentDescription = currentProd.name,
                    jumpToUrl = selectedVariant?.imageUrl?.takeIf { it.isNotBlank() },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Product Title, Rating, Stock Status
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Stock Warning Badge
                    if (displayStock in 1..3) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SuqiWarningBg)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(LucideIcons.Flame, contentDescription = null, tint = SuqiWarning, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Low Stock – Only $displayStock left",
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = SuqiWarning
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text(
                        text = currentProd.name,
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                        color = SuqiPrimaryText
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(LucideIcons.StarFilled, contentDescription = null, tint = SuqiWarning, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${currentProd.rating} (${currentProd.reviewCount} reviews)",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = SuqiPrimaryText
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "|  ${currentProd.soldCount}",
                            fontFamily = SuqiFontFamily,
                            fontSize = 13.sp,
                            color = SuqiSecondaryText
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Price Block
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formatEtb(displayPrice),
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = SuqiPrimary
                        )

                        if (!hasVariantPrice && currentProd.discountPercent > 0) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = formatEtb(currentProd.originalPrice),
                                fontFamily = SuqiFontFamily,
                                fontSize = 15.sp,
                                color = SuqiMutedText,
                                textDecoration = TextDecoration.LineThrough
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SuqiPrimaryTint)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "-${currentProd.discountPercent}%",
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = SuqiPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Buyer options (size, color, storage…): fully data-driven
            buyerOptions.forEach { option ->
                item(key = "option_${option.key}") {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            text = "${option.label}: ${chosen[option.key].orEmpty()}",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(details.valuesOf(option.key)) { value ->
                                val isSel = chosen[option.key] == value
                                val swatch = if (option.type == AttrType.COLOR) ColorSwatches.argbOf(value) else null
                                if (swatch != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .border(if (isSel) 2.5.dp else 1.dp, if (isSel) SuqiPrimary else SuqiBorder, CircleShape)
                                            .padding(4.dp)
                                            .clip(CircleShape)
                                            .background(Color(swatch))
                                            .clickable { chosen[option.key] = value }
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSel) SuqiPrimaryTint else SuqiSecondaryBackground)
                                            .border(
                                                1.5.dp,
                                                if (isSel) SuqiPrimary else SuqiBorder,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable { chosen[option.key] = value }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = value,
                                            fontFamily = SuqiFontFamily,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp,
                                            color = if (isSel) SuqiPrimary else SuqiPrimaryText
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Seller Card (Checklist 05)
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SuqiSecondaryBackground),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(SuqiSurface),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentProd.shopName.take(2).uppercase(),
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = SuqiPrimary
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = currentProd.shopName,
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = SuqiPrimaryText
                                    )
                                    if (currentProd.shopVerified) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = LucideIcons.CheckCircle,
                                            contentDescription = "Verified",
                                            tint = SuqiPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Addis Ababa • Verified Merchant",
                                    fontFamily = SuqiFontFamily,
                                    fontSize = 12.sp,
                                    color = SuqiSecondaryText
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.navigate(
                                        ScreenDestination.ChatThread(
                                            shopId = currentProd.shopId,
                                            productContextName = currentProd.name,
                                            productContextPrice = formatEtb(displayPrice),
                                            productContextImage = currentProd.primaryImageUrl
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SuqiPrimary),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                            ) {
                                Icon(LucideIcons.MessageCircle, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Chat", fontFamily = SuqiFontFamily, color = SuqiPrimary)
                            }

                            Button(
                                onClick = { viewModel.navigate(ScreenDestination.ShopStorefront(currentProd.shopId)) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SuqiSurface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SuqiBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                            ) {
                                Text("Visit Shop", fontFamily = SuqiFontFamily, color = SuqiPrimaryText)
                            }
                        }
                    }
                }
            }

            // Description Section
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = "Description",
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currentProd.description,
                        fontFamily = SuqiFontFamily,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = SuqiSecondaryText
                    )
                }
            }

            // Specifications Section (Plan §19 Screen 05)
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = "Specifications",
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = SuqiSecondaryBackground),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SpecRowItem(label = "Category", value = currentProd.categoryId.replace("cat_", "").replaceFirstChar { it.uppercase() })
                            SpecRowItem(label = "Stock Availability", value = "$displayStock in stock")
                            selectedVariant?.sku?.takeIf { it.isNotBlank() }?.let {
                                SpecRowItem(label = "SKU", value = it)
                            }
                            DynamicSpecGroups(details)
                        }
                    }
                }
            }

            // Delivery & Shipping Information Block (Plan §19 Screen 05)
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = "Delivery & Shipping",
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = SuqiSecondaryBackground),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(LucideIcons.Truck, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Addis Ababa Delivery", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("1–2 business days • Starting from 50 ETB", fontFamily = SuqiFontFamily, fontSize = 12.sp, color = SuqiSecondaryText)
                                }
                            }
                            HorizontalDivider(color = SuqiBorder, thickness = 1.dp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(LucideIcons.Store, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Store Pickup Available", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("Free collection from ${currentProd.shopName} in Addis Ababa", fontFamily = SuqiFontFamily, fontSize = 12.sp, color = SuqiSecondaryText)
                                }
                            }
                            HorizontalDivider(color = SuqiBorder, thickness = 1.dp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(LucideIcons.ShieldCheck, contentDescription = null, tint = SuqiSuccess, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Verified Payment Protection", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("Order verified against merchant transaction statement before dispatch", fontFamily = SuqiFontFamily, fontSize = 12.sp, color = SuqiSecondaryText)
                                }
                            }
                        }
                    }
                }
            }

            // Customer Reviews Section
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Customer Reviews (${reviews.size})",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Write a Review",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = SuqiPrimary,
                            modifier = Modifier.clickable {
                                viewModel.navigate(ScreenDestination.Reviews(currentProd.id, currentProd.name, "PRODUCT"))
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (reviews.isEmpty()) {
                        Text("No reviews yet. Be the first to review!", fontSize = 13.sp, color = SuqiMutedText)
                    } else {
                        reviews.take(3).forEach { rev ->
                            ReviewItemCard(review = rev)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecRowItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontFamily = SuqiFontFamily,
            fontSize = 13.sp,
            color = SuqiSecondaryText,
            modifier = Modifier.weight(0.42f)
        )
        Text(
            text = value,
            fontFamily = SuqiFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = SuqiPrimaryText,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.58f)
        )
    }
}

/** Enabled, non-empty specifications of any product type, grouped by their section. */
@Composable
private fun DynamicSpecGroups(details: ProductDetails) {
    val groups = details.specifications.groupBy { it.group }
    val showHeadings = groups.size > 1 || (groups.keys.firstOrNull() ?: "General") != "General"
    groups.forEach { (group, defs) ->
        HorizontalDivider(color = SuqiBorder)
        if (showHeadings) {
            Text(
                text = group,
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = SuqiPrimary
            )
        }
        defs.forEach { def ->
            SpecRowItem(label = def.label, value = details.displayValue(def))
        }
    }
}
