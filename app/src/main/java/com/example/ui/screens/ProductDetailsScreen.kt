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
import com.example.data.model.CartItemDto
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
    var selectedColor by remember { mutableStateOf("") }
    var selectedSpec by remember { mutableStateOf("") }
    var selectedImageIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(productId) {
        val res = viewModel.repository.getProductDetails(productId)
        if (res.success && res.data != null) {
            val p = res.data
            product = p
            if (p.colors.isNotEmpty()) selectedColor = p.colors.first()
            if (p.sizesOrSpecs.isNotEmpty()) selectedSpec = p.sizesOrSpecs.first()
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

    val allImages = remember(currentProd) {
        listOf(currentProd.primaryImageUrl) + currentProd.extraImages
    }

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
                color = Color.White,
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
                            val variation = listOfNotNull(selectedColor.takeIf { it.isNotBlank() }, selectedSpec.takeIf { it.isNotBlank() }).joinToString(" • ")
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
                            val variation = listOfNotNull(selectedColor.takeIf { it.isNotBlank() }, selectedSpec.takeIf { it.isNotBlank() }).joinToString(" • ")
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
                                    productImageUrl = currentProd.primaryImageUrl,
                                    unitPrice = currentProd.price,
                                    quantity = 1,
                                    selectedVariation = variation,
                                    stockAvailable = currentProd.stockQuantity
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
            // Main Gallery View — rounded corners, image / GIF / video
            item {
                val activeUrl = allImages.getOrElse(selectedImageIndex) { currentProd.primaryImageUrl }
                val mediaType = mediaTypeOf(activeUrl)
                val context = LocalContext.current
                val gifLoader = remember {
                    ImageLoader.Builder(context)
                        .components {
                            if (Build.VERSION.SDK_INT >= 28) {
                                add(ImageDecoderDecoder.Factory())
                            } else {
                                add(GifDecoder.Factory())
                            }
                        }
                        .build()
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(300.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(SuqiSecondaryBackground)
                ) {
                    when (mediaType) {
                        "video" -> {
                            AndroidView(
                                factory = { ctx ->
                                    VideoView(ctx).apply {
                                        setVideoURI(Uri.parse(activeUrl))
                                        setOnPreparedListener { mp ->
                                            mp.isLooping = true
                                            start()
                                        }
                                    }
                                },
                                update = { vv ->
                                    vv.setVideoURI(Uri.parse(activeUrl))
                                    vv.start()
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                            // Play indicator overlay
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.35f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    LucideIcons.Zap,
                                    contentDescription = "Video",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        else -> {
                            // image + gif via Coil (GifDecoder)
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(activeUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = currentProd.name,
                                contentScale = ContentScale.Crop,
                                imageLoader = gifLoader,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (mediaType == "gif") {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(12.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.55f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        "GIF",
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Page indicator
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${selectedImageIndex + 1}/${allImages.size}",
                            fontFamily = SuqiFontFamily,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }
            }

            // Image Thumbnails Gallery
            if (allImages.size > 1) {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allImages.indices.toList()) { index ->
                            val isSel = index == selectedImageIndex
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        2.dp,
                                        if (isSel) SuqiPrimary else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedImageIndex = index }
                            ) {
                                AsyncImage(
                                    model = allImages[index],
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }

            // Product Title, Rating, Stock Status
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Stock Warning Badge
                    if (currentProd.stockQuantity <= 3) {
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
                                text = "Low Stock – Only ${currentProd.stockQuantity} left",
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
                        fontWeight = FontWeight.Bold,
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
                            text = formatEtb(currentProd.price),
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = SuqiPrimary
                        )

                        if (currentProd.discountPercent > 0) {
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

            // Variations Selectors (Colors & Sizes)
            if (currentProd.colors.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        Text(
                            text = "Color: $selectedColor",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(currentProd.colors) { color ->
                                val isSel = color == selectedColor
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSel) SuqiPrimaryTint else SuqiSecondaryBackground)
                                        .border(
                                            1.5.dp,
                                            if (isSel) SuqiPrimary else SuqiBorder,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedColor = color }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = color,
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

            if (currentProd.sizesOrSpecs.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            text = "Specification / Size: $selectedSpec",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(currentProd.sizesOrSpecs) { spec ->
                                val isSel = spec == selectedSpec
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSel) SuqiPrimaryTint else SuqiSecondaryBackground)
                                        .border(
                                            1.5.dp,
                                            if (isSel) SuqiPrimary else SuqiBorder,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedSpec = spec }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = spec,
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
                                            productContextPrice = formatEtb(currentProd.price),
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
