package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.*
import com.example.ui.i18n.LocalSuqiLanguage
import com.example.ui.i18n.localizedDescription
import com.example.ui.i18n.localizedName
import com.example.ui.i18n.suqiStrings
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ProductDetailsScreen(
    productId: String,
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val lang = LocalSuqiLanguage.current
    val cartCount by viewModel.cartCount.collectAsState(initial = 0)
    val currentUser by viewModel.currentUser.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()

    var product by remember { mutableStateOf<ProductDto?>(null) }
    var shopDetails by remember { mutableStateOf<ShopDto?>(null) }
    var deliveryOptions by remember { mutableStateOf<List<ShopDeliveryOptionDto>>(emptyList()) }
    var reviews by remember { mutableStateOf<List<ReviewDto>>(emptyList()) }
    var questions by remember { mutableStateOf<List<ProductQuestionDto>>(emptyList()) }
    var moreFromShop by remember { mutableStateOf<List<ProductDto>>(emptyList()) }
    var similarProducts by remember { mutableStateOf<List<ProductDto>>(emptyList()) }

    var showShareSheet by remember { mutableStateOf(false) }
    var showSizeGuideSheet by remember { mutableStateOf(false) }
    var showAskQuestionSheet by remember { mutableStateOf(false) }
    var showReportSheet by remember { mutableStateOf(false) }

    val chosen = remember { mutableStateMapOf<String, String>() }

    fun loadAll() {
        coroutineScope.launch {
            val res = viewModel.repository.getProductDetails(productId)
            if (res.success && res.data != null) {
                val p = res.data
                product = p
                viewModel.recordProductView(p)
                chosen.clear()
                p.details.buyerOptions.forEach { opt ->
                    p.details.valuesOf(opt.key).firstOrNull()?.let { chosen[opt.key] = it }
                }
                viewModel.repository.getShopDetails(p.shopId).data?.let { shopDetails = it }
                viewModel.repository.getShopDeliveryOptions(p.shopId).data?.let { deliveryOptions = it }
                viewModel.repository.getProducts(shopId = p.shopId).data?.let { list ->
                    moreFromShop = list.filter { it.id != p.id }.take(6)
                }
                viewModel.repository.getProducts(categoryId = p.categoryId).data?.let { list ->
                    similarProducts = list.filter { it.id != p.id }.take(6)
                }
            }
            viewModel.repository.getReviews(productId).data?.let { reviews = it }
            viewModel.repository.getProductQuestions(productId).data?.let { questions = it }
        }
    }

    LaunchedEffect(productId) {
        loadAll()
    }

    val currentProd = product
    if (currentProd == null) {
        SuqiScreen(onBackClick = { viewModel.goBack() }) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                SuqiSkeletonList(count = 3)
            }
        }
        return
    }

    val details = currentProd.details
    val buyerOptions = details.buyerOptions
    val selectedVariant = details.variantFor(chosen.toMap())
    val variationLabel = ProductDetails.variantLabel(buyerOptions, chosen)
    val hasVariantPrice = selectedVariant?.price != null
    val displayPrice = selectedVariant?.price ?: currentProd.price
    val displayStock = selectedVariant?.stock ?: currentProd.stockQuantity
    val hasSizeOption = buyerOptions.any { it.label.contains("Size", ignoreCase = true) || it.key.contains("size", ignoreCase = true) }

    SuqiScreen(
        isOffline = isOffline,
        onRetryOffline = {
            viewModel.setOfflineMode(false)
            loadAll()
        },
        bottomBar = {
            Surface(
                color = colors.cardBackground,
                border = BorderStroke(1.dp, colors.border),
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
                            coroutineScope.launch {
                                val res = viewModel.repository.addToCart(currentProd.id, 1, variationLabel)
                                viewModel.showToast(if (res.success) strings.addedToCart else (res.message ?: strings.common.failedToAddToast))
                            }
                        },
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.5.dp, SuqiPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("btn_add_to_cart")
                    ) {
                        Text(strings.addToCart, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = SuqiPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }

                    Button(
                        onClick = {
                            viewModel.requireSignedInOrPrompt(strings.buyNow) {
                                viewModel.activeCheckoutShopId.value = currentProd.shopId
                                viewModel.activeCheckoutShopName.value = currentProd.shopName
                                viewModel.activeCheckoutItems.value = listOf(
                                    CartItemDto(
                                        id = 0,
                                        userId = currentUser?.id ?: "user_fitsum",
                                        productId = currentProd.id,
                                        shopId = currentProd.shopId,
                                        shopName = currentProd.shopName,
                                        shopVerified = currentProd.shopVerified,
                                        productName = currentProd.localizedName(lang),
                                        productImageUrl = selectedVariant?.imageUrl?.takeIf { it.isNotBlank() } ?: currentProd.primaryImageUrl,
                                        unitPrice = displayPrice,
                                        quantity = 1,
                                        selectedVariation = variationLabel,
                                        stockAvailable = displayStock
                                    )
                                )
                                viewModel.navigate(ScreenDestination.CheckoutDelivery(currentProd.shopId))
                            }
                        },
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("btn_buy_now")
                    ) {
                        Text(strings.buyNow, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    }
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Custom Top Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .suqiWarmShadow(CircleShape, elevation = 6.dp)
                            .clip(CircleShape)
                            .background(colors.cardBackground)
                            .clickable { viewModel.goBack() }
                            .testTag("product_back_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(LucideIcons.ArrowLeft, contentDescription = strings.common.backLabel, tint = colors.primaryText, modifier = Modifier.size(20.dp))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .suqiWarmShadow(CircleShape, elevation = 6.dp)
                                .clip(CircleShape)
                                .background(colors.cardBackground)
                                .clickable {
                                    coroutineScope.launch {
                                        viewModel.repository.toggleWishlist(currentProd.id)
                                        viewModel.repository.getProductDetails(currentProd.id).data?.let { product = it }
                                    }
                                }
                                .testTag("product_wishlist_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (currentProd.isWishlisted) LucideIcons.HeartFilled else LucideIcons.Heart,
                                contentDescription = strings.wishlistTitle,
                                tint = if (currentProd.isWishlisted) SuqiPrimary else colors.primaryText,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .suqiWarmShadow(CircleShape, elevation = 6.dp)
                                .clip(CircleShape)
                                .background(colors.cardBackground)
                                .clickable { showShareSheet = true }
                                .testTag("product_share_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(LucideIcons.Share2, contentDescription = strings.shareProduct, tint = colors.primaryText, modifier = Modifier.size(20.dp))
                        }

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .suqiWarmShadow(CircleShape, elevation = 6.dp)
                                .clip(CircleShape)
                                .background(colors.cardBackground)
                                .clickable { showReportSheet = true }
                                .testTag("product_report_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(LucideIcons.Flag, contentDescription = strings.reportListing, tint = colors.primaryText, modifier = Modifier.size(18.dp))
                        }

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .suqiWarmShadow(CircleShape, elevation = 6.dp)
                                .clip(CircleShape)
                                .background(colors.cardBackground)
                                .clickable { viewModel.navigate(ScreenDestination.Cart) }
                                .testTag("product_cart_btn"),
                            contentAlignment = Alignment.Center
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
                                Icon(LucideIcons.ShoppingCart, contentDescription = strings.cartTitle, tint = colors.primaryText, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            // Media Gallery
            item {
                SuqiMediaGallery(
                    media = details.media,
                    contentDescription = currentProd.localizedName(lang),
                    jumpToUrl = selectedVariant?.imageUrl?.takeIf { it.isNotBlank() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Title, Rating & Price Card
            item {
                SuqiSectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    if (displayStock in 1..3) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.warningBg)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(LucideIcons.Flame, contentDescription = null, tint = SuqiWarning, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(String.format(strings.checkout.lowStockLeftTemplate, displayStock), fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SuqiWarning)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text(
                        text = currentProd.localizedName(lang),
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = colors.primaryText
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(LucideIcons.StarFilled, contentDescription = null, tint = SuqiWarning, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${currentProd.rating} (${String.format(strings.checkout.reviewsCountTemplate, currentProd.reviewCount)})", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = colors.primaryText)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("|  ${currentProd.soldCount}", fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.secondaryText)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(formatEtb(displayPrice), fontFamily = SuqiFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = SuqiPrimary)
                        if (!hasVariantPrice && currentProd.discountPercent > 0) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(formatEtb(currentProd.originalPrice), fontFamily = SuqiFontFamily, fontSize = 15.sp, color = colors.mutedText, textDecoration = TextDecoration.LineThrough)
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.primaryTint)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text("-${currentProd.discountPercent}%", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SuqiPrimary)
                            }
                        }
                    }

                    if (buyerOptions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = colors.border)
                        Spacer(modifier = Modifier.height(12.dp))
                        buyerOptions.forEach { option ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${option.label}: ${chosen[option.key].orEmpty()}",
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = colors.primaryText
                                )
                                if (hasSizeOption && (option.label.contains("Size", ignoreCase = true) || option.key.contains("size", ignoreCase = true))) {
                                    Text(
                                        text = strings.sizeGuide,
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = SuqiPrimary,
                                        modifier = Modifier
                                            .clickable { showSizeGuideSheet = true }
                                            .testTag("btn_size_guide")
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                items(details.valuesOf(option.key)) { value ->
                                    val isSel = chosen[option.key] == value
                                    val swatch = if (option.type == AttrType.COLOR) ColorSwatches.argbOf(value) else null
                                    if (swatch != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .border(if (isSel) 2.5.dp else 1.dp, if (isSel) SuqiPrimary else colors.border, CircleShape)
                                                .padding(4.dp)
                                                .clip(CircleShape)
                                                .background(Color(swatch))
                                                .clickable { chosen[option.key] = value }
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSel) colors.primaryTint else colors.secondaryBackground)
                                                .border(1.5.dp, if (isSel) SuqiPrimary else colors.border, RoundedCornerShape(12.dp))
                                                .clickable { chosen[option.key] = value }
                                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = value,
                                                fontFamily = SuqiFontFamily,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 13.sp,
                                                color = if (isSel) SuqiPrimary else colors.primaryText
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }

            // Shop & Delivery Promise Strip (B-05)
            item {
                SuqiSectionCard(
                    containerColor = colors.shopTint,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .border(1.dp, colors.shopBorder, RoundedCornerShape(SuqiRadius.Card))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.navigate(ScreenDestination.ShopStorefront(currentProd.shopId)) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(colors.cardBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!shopDetails?.logoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = shopDetails?.logoUrl,
                                    contentDescription = currentProd.shopName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(currentProd.shopName.take(2).uppercase(), fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, color = SuqiPrimary)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(currentProd.shopName, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                                if (currentProd.shopVerified) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(LucideIcons.CheckCircle, contentDescription = strings.common.verifiedShopDesc, tint = SuqiPrimary, modifier = Modifier.size(15.dp))
                                }
                            }
                            val followerText = shopDetails?.let { "${String.format(strings.buyer.followersCountTemplate, it.followersCount)} • ${it.subCity}, ${it.city}" } ?: "${strings.checkout.defaultCityAddis} • ${strings.verifiedLabel}"
                            Text(followerText, fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.secondaryText)
                        }
                        Icon(LucideIcons.ChevronRight, contentDescription = null, tint = colors.secondaryText)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    val primaryDelivery = deliveryOptions.firstOrNull()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.cardBackground.copy(alpha = 0.7f))
                            .padding(10.dp)
                    ) {
                        Icon(LucideIcons.Truck, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = primaryDelivery?.let { "${it.title}: ${it.estimatedTime}" } ?: "${strings.checkout.deliveryPromiseTitle}: ${strings.checkout.deliveryPromiseEta}",
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = colors.primaryText
                            )
                            Text(
                                text = String.format(strings.checkout.deliveryFeeProtectionTemplate, formatEtb(primaryDelivery?.fee ?: 50.0)),
                                fontFamily = SuqiFontFamily,
                                fontSize = 11.sp,
                                color = colors.secondaryText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                viewModel.requireSignedInOrPrompt(strings.chatWithSeller) {
                                    viewModel.navigate(
                                        ScreenDestination.ChatThread(
                                            shopId = currentProd.shopId,
                                            productContextName = currentProd.localizedName(lang),
                                            productContextPrice = formatEtb(displayPrice),
                                            productContextImage = currentProd.primaryImageUrl
                                        )
                                    )
                                }
                            },
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, SuqiPrimary),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(LucideIcons.MessageCircle, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.checkout.chatShortBtn, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, color = SuqiPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        SuqiButton(
                            text = strings.visitShop,
                            isPrimary = false,
                            isPill = true,
                            onClick = { viewModel.navigate(ScreenDestination.ShopStorefront(currentProd.shopId)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Description & Specifications
            item {
                SuqiSectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(strings.description, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.primaryText)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(currentProd.localizedDescription(lang), fontFamily = SuqiFontFamily, fontSize = 14.sp, lineHeight = 22.sp, color = colors.secondaryText)

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = colors.border)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(strings.specifications, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.primaryText)
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val localizedCategory = MarketplaceCategories.all.firstOrNull { it.id == currentProd.categoryId }?.toDto()?.localizedName(lang)
                            ?: MarketplaceCategories.nameForId(currentProd.categoryId)
                        SpecRowItem(label = strings.buyer.categoryLabel, value = localizedCategory)
                        SpecRowItem(label = strings.checkout.stockAvailabilityLabel, value = String.format(strings.seller.stockUnitsTemplate, displayStock))
                        selectedVariant?.sku?.takeIf { it.isNotBlank() }?.let {
                            SpecRowItem(label = strings.seller.skuLabel, value = it)
                        }
                        DynamicSpecGroups(details)
                    }
                }
            }

            // B-05 Rating Breakdown Histogram + Customer Reviews
            item {
                SuqiSectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${strings.customerReviews} (${reviews.size})", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.primaryText)
                        Text(
                            text = strings.buyer.writeReviewTitle,
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = SuqiPrimary,
                            modifier = Modifier.clickable {
                                viewModel.requireSignedInOrPrompt(strings.buyer.writeReviewTitle) {
                                    viewModel.navigate(ScreenDestination.Reviews(currentProd.id, currentProd.localizedName(lang), "PRODUCT"))
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Histogram
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(86.dp)) {
                            Text(
                                text = String.format("%.1f", currentProd.rating),
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 30.sp,
                                color = colors.primaryText
                            )
                            Row {
                                repeat(5) { idx ->
                                    Icon(
                                        imageVector = LucideIcons.StarFilled,
                                        contentDescription = null,
                                        tint = if (idx < currentProd.rating.toInt()) SuqiWarning else colors.border,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                            Text(String.format(strings.checkout.ratingsCountTemplate, currentProd.reviewCount), fontFamily = SuqiFontFamily, fontSize = 11.sp, color = colors.secondaryText)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            val totalR = reviews.size.coerceAtLeast(1)
                            for (stars in 5 downTo 1) {
                                val countForStar = if (reviews.isNotEmpty()) reviews.count { it.rating.toInt() == stars } else 0
                                val fraction = if (reviews.isNotEmpty()) countForStar.toFloat() / totalR.toFloat() else 0f
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${stars}★", fontFamily = SuqiFontFamily, fontSize = 11.sp, color = colors.secondaryText, modifier = Modifier.width(24.dp))
                                    LinearProgressIndicator(
                                        progress = { fraction },
                                        color = SuqiWarning,
                                        trackColor = colors.secondaryBackground,
                                        modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("$countForStar", fontFamily = SuqiFontFamily, fontSize = 11.sp, color = colors.mutedText, modifier = Modifier.width(20.dp), textAlign = TextAlign.End)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    if (reviews.isEmpty()) {
                        Text(strings.buyer.noReviewsSub, fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.mutedText)
                    } else {
                        reviews.take(3).forEach { rev ->
                            ReviewItemCard(review = rev)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            // B-05 Persisted Questions & Answers Section
            item {
                SuqiSectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${strings.questionsAnswers} (${questions.size})", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.primaryText)
                        Text(
                            text = strings.checkout.askQuestionBtn,
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = SuqiPrimary,
                            modifier = Modifier
                                .clickable {
                                    viewModel.requireSignedInOrPrompt(strings.checkout.askQuestionBtn) {
                                        showAskQuestionSheet = true
                                    }
                                }
                                .testTag("btn_ask_question")
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    if (questions.isEmpty()) {
                        Text(strings.checkout.awaitingSellerReply, fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.mutedText)
                    } else {
                        questions.forEachIndexed { index, qa ->
                            if (index > 0) {
                                HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = 10.dp))
                            }
                            Text("Q: ${qa.question}", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.primaryText)
                            Text("${qa.askerName} • ${qa.createdAt}", fontFamily = SuqiFontFamily, fontSize = 11.sp, color = colors.mutedText)
                            if (qa.answer.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.Top) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(colors.primaryTint)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(strings.shopsTab, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = SuqiPrimary)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(qa.answer, fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.secondaryText)
                                }
                            } else {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(strings.checkout.awaitingSellerReply, fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.mutedText)
                            }
                        }
                    }
                }
            }

            // More from this Shop Carousel
            if (moreFromShop.isNotEmpty()) {
                item {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${strings.moreFromShop}: ${currentProd.shopName}", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.primaryText, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.checkout.seeShopArrow,
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = SuqiPrimary,
                                modifier = Modifier.clickable { viewModel.navigate(ScreenDestination.ShopStorefront(currentProd.shopId)) }
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(moreFromShop) { prod ->
                                Box(modifier = Modifier.width(160.dp)) {
                                    SuqiProductCard(
                                        product = prod,
                                        onClick = { viewModel.navigate(ScreenDestination.ProductDetails(prod.id)) },
                                        onAddToCart = { viewModel.quickAddToCart(prod) },
                                        onShopClick = { viewModel.navigate(ScreenDestination.ShopStorefront(prod.shopId)) },
                                        onToggleWishlist = {
                                            coroutineScope.launch {
                                                viewModel.repository.toggleWishlist(prod.id)
                                                loadAll()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Similar Products Carousel
            if (similarProducts.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = strings.similarProducts,
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = colors.primaryText,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(similarProducts) { prod ->
                                Box(modifier = Modifier.width(160.dp)) {
                                    SuqiProductCard(
                                        product = prod,
                                        onClick = { viewModel.navigate(ScreenDestination.ProductDetails(prod.id)) },
                                        onAddToCart = { viewModel.quickAddToCart(prod) },
                                        onShopClick = { viewModel.navigate(ScreenDestination.ShopStorefront(prod.shopId)) },
                                        onToggleWishlist = {
                                            coroutineScope.launch {
                                                viewModel.repository.toggleWishlist(prod.id)
                                                loadAll()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Real Share Bottom Sheet (replaces toast-only stub)
    if (showShareSheet) {
        val shareLink = "https://suqi.et/p/${currentProd.id}"
        val shareText = "${currentProd.localizedName(lang)} — ${formatEtb(displayPrice)} • ${currentProd.shopName}\n$shareLink"
        SuqiBottomSheet(onDismissRequest = { showShareSheet = false }, title = strings.shareProduct) {
            SuqiSectionCard(containerColor = colors.secondaryBackground, padding = 12.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = currentProd.primaryImageUrl,
                        contentDescription = currentProd.localizedName(lang),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(currentProd.localizedName(lang), fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${formatEtb(displayPrice)} • ${currentProd.shopName}", fontFamily = SuqiFontFamily, fontSize = 12.sp, color = SuqiPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SuqiButton(
                    text = strings.common.copyLinkLabel,
                    icon = LucideIcons.Copy,
                    isPrimary = false,
                    isPill = true,
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Suqi Product", shareLink))
                        viewModel.showToast(strings.common.copiedToast)
                        showShareSheet = false
                    },
                    modifier = Modifier.weight(1f),
                    testTag = "btn_copy_product_link"
                )
                SuqiButton(
                    text = strings.shareProduct,
                    icon = LucideIcons.Share2,
                    isPill = true,
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, currentProd.localizedName(lang))
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, strings.shareProduct))
                        showShareSheet = false
                    },
                    modifier = Modifier.weight(1f),
                    testTag = "btn_native_share"
                )
            }
        }
    }

    // Size Guide Bottom Sheet
    if (showSizeGuideSheet) {
        SuqiBottomSheet(onDismissRequest = { showSizeGuideSheet = false }, title = strings.sizeGuide) {
            val rows = listOf(
                Triple("EU / Intl", "Chest (cm)", "Shoe (cm)"),
                Triple("S / 38–39", "88 – 94", "24.5"),
                Triple("M / 40–41", "96 – 102", "26.0"),
                Triple("L / 42–43", "104 – 110", "27.5"),
                Triple("XL / 44–45", "112 – 118", "28.8")
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEachIndexed { idx, (a, b, c) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (idx == 0) colors.primaryTint else colors.secondaryBackground)
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(a, fontFamily = SuqiFontFamily, fontWeight = if (idx == 0) FontWeight.Bold else FontWeight.Medium, fontSize = 13.sp, color = colors.primaryText, modifier = Modifier.weight(1f))
                        Text(b, fontFamily = SuqiFontFamily, fontWeight = if (idx == 0) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp, color = colors.primaryText, modifier = Modifier.weight(1f))
                        Text(c, fontFamily = SuqiFontFamily, fontWeight = if (idx == 0) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp, color = colors.primaryText, modifier = Modifier.weight(0.8f), textAlign = TextAlign.End)
                    }
                }
            }
        }
    }

    // Ask Question Sheet
    if (showAskQuestionSheet) {
        var qText by remember { mutableStateOf("") }
        SuqiBottomSheet(onDismissRequest = { showAskQuestionSheet = false }, title = "${strings.checkout.askQuestionBtn} — ${currentProd.shopName}") {
            SuqiTextField(
                value = qText,
                onValueChange = { qText = it },
                label = strings.checkout.questionRequiredLabel,
                placeholder = strings.checkout.questionRequiredLabel,
                singleLine = false,
                testTag = "input_product_question"
            )
            Spacer(modifier = Modifier.height(14.dp))
            SuqiButton(
                text = strings.checkout.sendQuestionBtn,
                isPill = true,
                enabled = qText.isNotBlank(),
                onClick = {
                    coroutineScope.launch {
                        val res = viewModel.repository.submitProductQuestion(currentProd.id, qText.trim())
                        if (res.success) {
                            viewModel.showToast(strings.common.doneLabel)
                            showAskQuestionSheet = false
                            loadAll()
                        } else {
                            viewModel.showToast(res.message ?: strings.common.failedToAddToast)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "btn_submit_question"
            )
        }
    }

    // Report Listing Sheet
    if (showReportSheet) {
        var selectedReason by remember { mutableStateOf("Counterfeit or misleading listing") }
        var extraDetails by remember { mutableStateOf("") }
        val reasons = listOf(
            "Counterfeit or misleading listing",
            "Incorrect price or out of stock",
            "Prohibited item on Suqi",
            "Suspicious payment instructions"
        )
        SuqiBottomSheet(onDismissRequest = { showReportSheet = false }, title = strings.reportListing) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                reasons.forEach { r ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedReason == r) colors.primaryTint else Color.Transparent)
                            .clickable { selectedReason = r }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        RadioButton(selected = selectedReason == r, onClick = { selectedReason = r })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(r, fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.primaryText)
                    }
                }
                SuqiTextField(
                    value = extraDetails,
                    onValueChange = { extraDetails = it },
                    label = strings.checkout.reportDetailsLabel,
                    placeholder = strings.checkout.reportDetailsLabel
                )
                Spacer(modifier = Modifier.height(8.dp))
                SuqiButton(
                    text = strings.checkout.submitReportBtn,
                    isPill = true,
                    onClick = {
                        coroutineScope.launch {
                            viewModel.repository.reportOrModerateReview(
                                mapOf(
                                    "action" to "REPORT",
                                    "authorName" to currentProd.shopName,
                                    "reason" to selectedReason,
                                    "reviewText" to "${currentProd.name}: ${extraDetails.ifBlank { selectedReason }}"
                                )
                            )
                            viewModel.showToast(strings.common.doneLabel)
                            showReportSheet = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SpecRowItem(label: String, value: String) {
    val colors = LocalSuqiColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(label, fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.secondaryText, modifier = Modifier.weight(0.42f))
        Text(value, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = colors.primaryText, textAlign = TextAlign.End, modifier = Modifier.weight(0.58f))
    }
}

@Composable
private fun DynamicSpecGroups(details: ProductDetails) {
    val colors = LocalSuqiColors.current
    val groups = details.specifications.groupBy { it.group }
    val showHeadings = groups.size > 1 || (groups.keys.firstOrNull() ?: "General") != "General"
    groups.forEach { (group, defs) ->
        HorizontalDivider(color = colors.border)
        if (showHeadings) {
            Text(group, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = SuqiPrimary)
        }
        defs.forEach { def ->
            SpecRowItem(label = def.label, value = details.displayValue(def))
        }
    }
}
