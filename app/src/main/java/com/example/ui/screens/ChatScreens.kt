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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import android.net.Uri
import com.example.data.model.ChatMessageDto
import com.example.data.model.ProductDto
import com.example.data.model.ShopDto
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ChatListScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var conversations by remember { mutableStateOf<List<ChatMessageDto>>(emptyList()) }

    LaunchedEffect(Unit) {
        val res = viewModel.repository.getChatConversations()
        if (res.success && res.data != null) {
            conversations = res.data
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.goBack() },
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(SuqiSecondaryBackground)
                ) {
                    Icon(LucideIcons.ArrowLeft, contentDescription = "Back", tint = SuqiPrimaryText)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "Chat Messages", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        },
        containerColor = SuqiBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp)
        ) {
            if (conversations.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Text(text = "No active conversations.", color = SuqiSecondaryText)
                    }
                }
            } else {
                items(conversations) { conv ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = LocalSuqiColors.current.cardBackground),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SuqiBorder)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { viewModel.navigate(ScreenDestination.ChatThread(conv.shopId)) }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(SuqiPrimaryTint),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = conv.shopName.take(2).uppercase(),
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = SuqiPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = conv.shopName,
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = conv.timestamp,
                                        fontFamily = SuqiFontFamily,
                                        fontSize = 11.sp,
                                        color = SuqiMutedText
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = conv.message,
                                    fontFamily = SuqiFontFamily,
                                    fontSize = 13.sp,
                                    color = SuqiSecondaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatThreadScreen(
    shopId: String,
    productContextName: String? = null,
    productContextPrice: String? = null,
    productContextImage: String? = null,
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    var messages by remember { mutableStateOf<List<ChatMessageDto>>(emptyList()) }
    var inputMessage by remember { mutableStateOf("") }
    var shopTitle by remember { mutableStateOf("Tech Hub") }

    var allProducts by remember { mutableStateOf<List<ProductDto>>(emptyList()) }
    var allShops by remember { mutableStateOf<List<ShopDto>>(emptyList()) }
    var showProductPicker by remember { mutableStateOf(false) }
    var showShopPicker by remember { mutableStateOf(false) }

    // Pending product attachment to embed in the next sent message
    var pendingProductName by remember { mutableStateOf(productContextName) }
    var pendingProductPrice by remember { mutableStateOf(productContextPrice) }
    var pendingProductImage by remember { mutableStateOf(productContextImage) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var productSearchQuery by remember { mutableStateOf("") }
    var shopSearchQuery by remember { mutableStateOf("") }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingPhotoUri = uri
            viewModel.showToast("Photo attached")
        }
    }

    val emojiList = remember {
        listOf(
            "😀", "😂", "😊", "😍", "🥰", "😎", "🤔", "😢", "😭", "👍",
            "👎", "🙏", "👏", "🔥", "❤️", "🧡", "✅", "🎉", "🚚", "📦",
            "💬", "📍", "⭐", "💯", "🙌", "🤝", "👋", "😅", "🙂", "😉"
        )
    }

    fun loadMessages() {
        coroutineScope.launch {
            val res = viewModel.repository.getChatMessages(shopId)
            if (res.success && res.data != null) {
                messages = res.data
                if (res.data.isNotEmpty()) {
                    shopTitle = res.data.first().shopName
                }
            }
        }
    }

    LaunchedEffect(shopId) {
        loadMessages()
        val prodRes = viewModel.repository.getProducts()
        if (prodRes.success && prodRes.data != null) allProducts = prodRes.data
        val shopRes = viewModel.repository.getShops()
        if (shopRes.success && shopRes.data != null) allShops = shopRes.data
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.goBack() },
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(colors.secondaryBackground)
                ) {
                    Icon(LucideIcons.ArrowLeft, contentDescription = "Back", tint = colors.primaryText)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = shopTitle,
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = colors.primaryText
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(LucideIcons.CheckCircle, contentDescription = "Verified", tint = SuqiPrimary, modifier = Modifier.size(14.dp))
                    }
                    Text(text = "Online", fontSize = 11.sp, color = SuqiSuccess)
                }
            }
        },
        bottomBar = {
            Surface(
                color = colors.cardBackground,
                shadowElevation = 8.dp,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    // Quick mention toolbar
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Add:",
                            fontFamily = SuqiFontFamily,
                            fontSize = 11.sp,
                            color = colors.secondaryText,
                            fontWeight = FontWeight.Medium
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(SuqiPrimaryTint)
                                .clickable { showProductPicker = true }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = LucideIcons.ShoppingBag, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "# Product", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = SuqiPrimary)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(colors.secondaryBackground)
                                .clickable { showShopPicker = true }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = LucideIcons.Store, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "@ Shop", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = colors.primaryText)
                            }
                        }
                    }

                    // Pending attachments preview
                    if (pendingProductName != null || pendingPhotoUri != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (pendingProductName != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SuqiPrimaryTint)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(imageVector = LucideIcons.ShoppingBag, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = pendingProductName.orEmpty(),
                                        fontFamily = SuqiFontFamily,
                                        fontSize = 12.sp,
                                        color = SuqiPrimary,
                                        maxLines = 1,
                                        modifier = Modifier.widthIn(max = 140.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        LucideIcons.X,
                                        contentDescription = "Remove",
                                        tint = SuqiPrimary,
                                        modifier = Modifier.size(14.dp).clickable {
                                            pendingProductName = null
                                            pendingProductPrice = null
                                            pendingProductImage = null
                                        }
                                    )
                                }
                            }
                            if (pendingPhotoUri != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.secondaryBackground)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(imageVector = LucideIcons.ImagePlus, contentDescription = null, tint = colors.primaryText, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Photo", fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.primaryText)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        LucideIcons.X,
                                        contentDescription = "Remove",
                                        tint = colors.mutedText,
                                        modifier = Modifier.size(14.dp).clickable { pendingPhotoUri = null }
                                    )
                                }
                            }
                        }
                    }

                    // Input row: + | multi-line field | emoji | send
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = { photoPicker.launch("image/*") },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(colors.secondaryBackground)
                        ) {
                            Icon(LucideIcons.Plus, contentDescription = "Attach photo", tint = colors.primaryText)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedTextField(
                            value = inputMessage,
                            onValueChange = { inputMessage = it },
                            placeholder = {
                                Text(
                                    text = "Type a message...",
                                    color = colors.mutedText,
                                    fontFamily = SuqiFontFamily,
                                    fontSize = 14.sp
                                )
                            },
                            shape = RoundedCornerShape(22.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = colors.secondaryBackground,
                                unfocusedContainerColor = colors.secondaryBackground,
                                focusedBorderColor = SuqiPrimary.copy(alpha = 0.5f),
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = colors.primaryText,
                                unfocusedTextColor = colors.primaryText
                            ),
                            minLines = 1,
                            maxLines = 5,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp, max = 120.dp)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = { showEmojiPicker = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(LucideIcons.Smile, contentDescription = "Emoji", tint = colors.secondaryText)
                        }

                        IconButton(
                            onClick = {
                                if (inputMessage.isNotBlank() || pendingProductName != null || pendingPhotoUri != null) {
                                    val text = inputMessage.trim().ifBlank {
                                        if (pendingPhotoUri != null) "📷 Photo" else ""
                                    }
                                    inputMessage = ""
                                    coroutineScope.launch {
                                        viewModel.repository.sendChatMessage(
                                            shopId = shopId,
                                            messageData = mapOf(
                                                "message" to text,
                                                "productContextName" to (pendingProductName ?: ""),
                                                "productContextPrice" to (pendingProductPrice ?: ""),
                                                "productContextImage" to (pendingProductImage ?: "")
                                            )
                                        )
                                        pendingProductName = null
                                        pendingProductPrice = null
                                        pendingProductImage = null
                                        pendingPhotoUri = null
                                        loadMessages()
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(SuqiPrimary)
                        ) {
                            Icon(LucideIcons.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Product Context Card (Mockup 4 & Checklist 24)
            if (productContextName != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.secondaryBackground),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (productContextImage != null) {
                                Box(
                                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(colors.cardBackground)
                                ) {
                                    AsyncImage(model = productContextImage, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                            }
                            Column {
                                Text(text = productContextName, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.primaryText)
                                if (productContextPrice != null) {
                                    Text(text = productContextPrice, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = SuqiPrimary)
                                }
                            }
                        }
                    }
                }
            }

            items(messages) { msg ->
                val isCustomer = msg.senderType == "CUSTOMER"
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = if (isCustomer) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 300.dp)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 18.dp,
                                    topEnd = 18.dp,
                                    bottomStart = if (isCustomer) 18.dp else 4.dp,
                                    bottomEnd = if (isCustomer) 4.dp else 18.dp
                                )
                            )
                            .background(if (isCustomer) SuqiPrimary else colors.cardBackground)
                            .then(
                                if (!isCustomer) Modifier.border(1.dp, colors.border, RoundedCornerShape(
                                    topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp
                                )) else Modifier
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            val ctxName = msg.productContextName?.takeIf { it.isNotBlank() }
                            val ctxPrice = msg.productContextPrice?.takeIf { it.isNotBlank() }
                            val ctxImage = msg.productContextImage?.takeIf { it.isNotBlank() }
                            val suppressKeys = buildSet {
                                if (ctxName != null) {
                                    add(ctxName.lowercase())
                                    allProducts.firstOrNull {
                                        it.name.equals(ctxName, ignoreCase = true) ||
                                            it.name.contains(ctxName, ignoreCase = true)
                                    }?.id?.lowercase()?.let { add(it) }
                                }
                            }
                            // Strip structured #[Name:id] tokens for products already in productContext
                            val displayText = if (ctxName != null) {
                                // Remove structured product mentions already shown as productContext card
                                val mentionPattern = Regex("""#\[[^\]]+\]""")
                                msg.message.replace(mentionPattern, "").trim()
                            } else msg.message

                            ChatBubbleMessageContent(
                                text = displayText,
                                isCustomer = isCustomer,
                                allProducts = allProducts,
                                allShops = allShops,
                                onNavigateToProduct = { pId ->
                                    viewModel.navigate(ScreenDestination.ProductDetails(pId))
                                },
                                onNavigateToShop = { sId ->
                                    viewModel.navigate(ScreenDestination.ShopStorefront(sId))
                                },
                                suppressProductKeys = suppressKeys
                            )

                            if (ctxName != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                val matched = allProducts.firstOrNull {
                                    it.name.equals(ctxName, ignoreCase = true) ||
                                        it.name.contains(ctxName, ignoreCase = true)
                                }
                                ChatEmbeddedProductCard(
                                    name = ctxName,
                                    price = ctxPrice ?: matched?.let { formatEtb(it.price) },
                                    imageUrl = ctxImage ?: matched?.primaryImageUrl,
                                    isOnPrimary = isCustomer,
                                    onClick = {
                                        val id = matched?.id
                                        if (id != null) {
                                            viewModel.navigate(ScreenDestination.ProductDetails(id))
                                        }
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = msg.timestamp,
                                fontFamily = SuqiFontFamily,
                                fontSize = 10.sp,
                                color = if (isCustomer) Color.White.copy(alpha = 0.7f) else colors.mutedText,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            }
        }
    }


    // Product picker with search
    if (showProductPicker) {
        ModalBottomSheet(
            onDismissRequest = {
                showProductPicker = false
                productSearchQuery = ""
            },
            containerColor = colors.cardBackground,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            val filteredProducts = remember(allProducts, productSearchQuery) {
                val q = productSearchQuery.trim()
                if (q.isEmpty()) allProducts
                else allProducts.filter {
                    it.name.contains(q, ignoreCase = true) ||
                        it.shopName.contains(q, ignoreCase = true) ||
                        it.categoryId.contains(q, ignoreCase = true)
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "Select product",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = colors.primaryText
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = productSearchQuery,
                    onValueChange = { productSearchQuery = it },
                    placeholder = { Text(text = "Search products...", color = colors.mutedText) },
                    leadingIcon = {
                        Icon(imageVector = LucideIcons.Search, contentDescription = null, tint = colors.mutedText, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (productSearchQuery.isNotBlank()) {
                            IconButton(onClick = { productSearchQuery = "" }) {
                                Icon(imageVector = LucideIcons.X, contentDescription = null, tint = colors.mutedText, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.secondaryBackground,
                        unfocusedContainerColor = colors.secondaryBackground,
                        focusedBorderColor = SuqiPrimary,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.primaryText,
                        unfocusedTextColor = colors.primaryText
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${filteredProducts.size} products",
                    fontFamily = SuqiFontFamily,
                    fontSize = 12.sp,
                    color = colors.mutedText
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    if (filteredProducts.isEmpty()) {
                        item {
                            Text(
                                "No products match \"$productSearchQuery\"",
                                fontFamily = SuqiFontFamily,
                                color = colors.secondaryText,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    }
                    items(filteredProducts, key = { it.id }) { productItem ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    pendingProductName = productItem.name
                                    pendingProductPrice = formatEtb(productItem.price)
                                    pendingProductImage = productItem.primaryImageUrl
                                    productSearchQuery = ""
                                    showProductPicker = false
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            AsyncImage(
                                model = productItem.primaryImageUrl,
                                contentDescription = productItem.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.secondaryBackground)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                Text(
                                    text = productItem.name,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = colors.primaryText,
                                    maxLines = 1
                                )
                                Text(
                                    text = productItem.shopName,
                                    fontFamily = SuqiFontFamily,
                                    fontSize = 12.sp,
                                    color = colors.mutedText
                                )
                            }
                            Text(
                                text = formatEtb(productItem.price),
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = SuqiPrimary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

        if (showShopPicker) {
        ModalBottomSheet(
            onDismissRequest = {
                showShopPicker = false
                shopSearchQuery = ""
            },
            containerColor = colors.cardBackground,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            val filteredShops = remember(allShops, shopSearchQuery) {
                val q = shopSearchQuery.trim()
                if (q.isEmpty()) allShops
                else allShops.filter {
                    it.name.contains(q, ignoreCase = true) ||
                        it.city.contains(q, ignoreCase = true) ||
                        it.category.contains(q, ignoreCase = true)
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "Select shop",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = colors.primaryText
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = shopSearchQuery,
                    onValueChange = { shopSearchQuery = it },
                    placeholder = { Text(text = "Search shops...", color = colors.mutedText) },
                    leadingIcon = {
                        Icon(imageVector = LucideIcons.Search, contentDescription = null, tint = colors.mutedText, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (shopSearchQuery.isNotBlank()) {
                            IconButton(onClick = { shopSearchQuery = "" }) {
                                Icon(imageVector = LucideIcons.X, contentDescription = null, tint = colors.mutedText, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.secondaryBackground,
                        unfocusedContainerColor = colors.secondaryBackground,
                        focusedBorderColor = SuqiPrimary,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.primaryText,
                        unfocusedTextColor = colors.primaryText
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${filteredShops.size} shops",
                    fontFamily = SuqiFontFamily,
                    fontSize = 12.sp,
                    color = colors.mutedText
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    if (filteredShops.isEmpty()) {
                        item {
                            Text(
                                "No shops match \"$shopSearchQuery\"",
                                fontFamily = SuqiFontFamily,
                                color = colors.secondaryText,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    }
                    items(filteredShops, key = { it.id }) { shop ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    inputMessage = "$inputMessage @[${shop.name}:${shop.id}] ".trimStart()
                                    shopSearchQuery = ""
                                    showShopPicker = false
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            AsyncImage(
                                model = shop.logoUrl.ifBlank { shop.coverUrl },
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.secondaryBackground)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = shop.name,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = colors.primaryText
                                )
                                Text(
                                    text = "${shop.city} · ${shop.category}",
                                    fontFamily = SuqiFontFamily,
                                    fontSize = 12.sp,
                                    color = colors.mutedText
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Emoji picker sheet
    if (showEmojiPicker) {
        ModalBottomSheet(
            onDismissRequest = { showEmojiPicker = false },
            containerColor = colors.cardBackground,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "Emoji",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = colors.primaryText
                )
                Spacer(modifier = Modifier.height(12.dp))
                // 6-column grid
                val rows = emojiList.chunked(6)
                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 28.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        inputMessage += emoji
                                        showEmojiPicker = false
                                    }
                                    .padding(10.dp)
                            )
                        }
                        // pad incomplete row
                        repeat(6 - row.size) {
                            Spacer(modifier = Modifier.size(48.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

}

/**
 * Parses and renders message text with interactive #product and @shop mention chips.
 */
@Composable
private fun ChatBubbleMessageContent(
    text: String,
    isCustomer: Boolean,
    allProducts: List<ProductDto>,
    allShops: List<ShopDto>,
    onNavigateToProduct: (String) -> Unit,
    onNavigateToShop: (String) -> Unit,
    /** Product names/ids already shown via productContext — skip duplicate cards */
    suppressProductKeys: Set<String> = emptySet()
) {
    val colors = LocalSuqiColors.current
    // Regex matching #[Title:id] or @[Title:id] or simple #Word or @Word
    val structuredMentionRegex = Regex("""([#@])\[([^:]+):([^\]]+)\]""")

    val matches = structuredMentionRegex.findAll(text).toList()

    if (matches.isEmpty()) {
        // Plain text or manual hashtag/mention check
        val manualTokens = text.split(" ")
        val hasManualMention = manualTokens.any { it.startsWith("#") || it.startsWith("@") }

        if (!hasManualMention) {
            Text(
                text = text,
                fontFamily = SuqiFontFamily,
                fontSize = 14.sp,
                color = if (isCustomer) Color.White else colors.primaryText
            )
        } else {
            Column {
                Text(
                    text = text,
                    fontFamily = SuqiFontFamily,
                    fontSize = 14.sp,
                    color = if (isCustomer) Color.White else colors.primaryText
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Render chips for manual #tags and @tags
                manualTokens.forEach { token ->
                    if (token.startsWith("#") && token.length > 1) {
                        val name = token.removePrefix("#")
                        if (name.lowercase() in suppressProductKeys) return@forEach
                        val prod = allProducts.firstOrNull {
                            it.name.contains(name, ignoreCase = true) || it.id.equals(name, ignoreCase = true)
                        }
                        if (prod != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            ChatEmbeddedProductCard(
                                name = prod.name,
                                price = formatEtb(prod.price),
                                imageUrl = prod.primaryImageUrl,
                                isOnPrimary = isCustomer,
                                onClick = { onNavigateToProduct(prod.id) }
                            )
                        }
                    } else if (token.startsWith("@") && token.length > 1) {
                        val name = token.removePrefix("@")
                        val shop = allShops.firstOrNull {
                            it.name.contains(name, ignoreCase = true) || it.id.equals(name, ignoreCase = true)
                        }
                        if (shop != null) {
                            MentionChip(
                                type = "shop",
                                label = shop.name,
                                onClick = { onNavigateToShop(shop.id) },
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    } else {
        // Render cleaned text + interactive chips
        var cleanedText = text
        matches.forEach { match ->
            // Remove structured tokens from body text; products render as cards below
            cleanedText = cleanedText.replace(match.value, "").trim()
        }

        Column {
            if (cleanedText.isNotBlank()) {
                Text(
                    text = cleanedText,
                    fontFamily = SuqiFontFamily,
                    fontSize = 14.sp,
                    color = if (isCustomer) Color.White else colors.primaryText
                )
            }
            matches.forEach { match ->
                val isProduct = match.groupValues[1] == "#"
                val label = match.groupValues[2]
                val id = match.groupValues[3]
                if (isProduct) {
                    val keyMatch = label.lowercase() in suppressProductKeys || id.lowercase() in suppressProductKeys
                    if (keyMatch) return@forEach // already shown as productContext card
                    val prod = allProducts.firstOrNull { it.id == id }
                        ?: allProducts.firstOrNull { it.name.equals(label, ignoreCase = true) }
                    Spacer(modifier = Modifier.height(8.dp))
                    ChatEmbeddedProductCard(
                        name = prod?.name ?: label,
                        price = prod?.let { formatEtb(it.price) },
                        imageUrl = prod?.primaryImageUrl,
                        isOnPrimary = isCustomer,
                        onClick = { onNavigateToProduct(id) }
                    )
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    MentionChip(
                        type = "shop",
                        label = label,
                        onClick = { onNavigateToShop(id) },
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Embedded product card inside a chat bubble (image + name + price + chevron).
 * Matches the in-chat product mention mockup.
 */
@Composable
private fun ChatEmbeddedProductCard(
    name: String,
    price: String?,
    imageUrl: String?,
    isOnPrimary: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalSuqiColors.current
    val cardBg = if (isOnPrimary) Color.White.copy(alpha = 0.18f) else colors.secondaryBackground
    val titleColor = if (isOnPrimary) Color.White else colors.primaryText
    val priceColor = if (isOnPrimary) Color.White else SuqiPrimary
    val chevronColor = if (isOnPrimary) Color.White.copy(alpha = 0.85f) else colors.mutedText

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isOnPrimary) Color.White.copy(alpha = 0.2f) else colors.cardBackground)
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    LucideIcons.ShoppingBag,
                    contentDescription = null,
                    tint = if (isOnPrimary) Color.White else SuqiPrimary,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = titleColor,
                maxLines = 2
            )
            if (!price.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = price,
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = priceColor
                )
            }
        }
        Icon(
            LucideIcons.ChevronRight,
            contentDescription = null,
            tint = chevronColor,
            modifier = Modifier.size(18.dp)
        )
    }
}
