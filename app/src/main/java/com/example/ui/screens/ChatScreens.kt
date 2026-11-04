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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SuqiPrimaryText)
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
                        Text("No active conversations.", color = SuqiSecondaryText)
                    }
                }
            } else {
                items(conversations) { conv ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                                    .background(SuqiPrimaryLight),
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.primaryText)
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
                        Icon(Icons.Filled.CheckCircle, contentDescription = "Verified", tint = SuqiPrimary, modifier = Modifier.size(14.dp))
                    }
                    Text(text = "Online", fontSize = 11.sp, color = SuqiSuccess)
                }
            }
        },
        bottomBar = {
            Surface(
                color = colors.cardBackground,
                shadowElevation = 12.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Quick Mention Toolbar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Add to chat:", fontFamily = SuqiFontFamily, fontSize = 11.sp, color = colors.secondaryText, fontWeight = FontWeight.Medium)

                        // Product Mention Chip Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(SuqiPrimaryLight)
                                .clickable { showProductPicker = true }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.ShoppingBag, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("# Product", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SuqiPrimary)
                            }
                        }

                        // Shop Mention Chip Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0xFFEFF6FF))
                                .clickable { showShopPicker = true }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Storefront, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("@ Shop", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF2563EB))
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.showToast("Attach photo") }) {
                            Icon(Icons.Outlined.SentimentSatisfied, contentDescription = "Emoji", tint = colors.secondaryText)
                        }

                        OutlinedTextField(
                            value = inputMessage,
                            onValueChange = { inputMessage = it },
                            placeholder = { Text("Type a message or use #product / @shop...", color = colors.mutedText, fontSize = 13.sp) },
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = colors.secondaryBackground,
                                unfocusedContainerColor = colors.secondaryBackground,
                                focusedBorderColor = SuqiPrimary,
                                unfocusedBorderColor = colors.border,
                                focusedTextColor = colors.primaryText,
                                unfocusedTextColor = colors.primaryText
                            ),
                            modifier = Modifier.weight(1f).heightIn(max = 48.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (inputMessage.isNotBlank()) {
                                    val text = inputMessage.trim()
                                    inputMessage = ""
                                    coroutineScope.launch {
                                        viewModel.repository.sendChatMessage(
                                            shopId = shopId,
                                            messageData = mapOf(
                                                "message" to text,
                                                "productContextName" to (productContextName ?: ""),
                                                "productContextPrice" to (productContextPrice ?: "")
                                            )
                                        )
                                        loadMessages()
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(SuqiPrimary)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        containerColor = colors.surfaceBackground,
        modifier = modifier.fillMaxSize()
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
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isCustomer) 16.dp else 4.dp,
                                    bottomEnd = if (isCustomer) 4.dp else 16.dp
                                )
                            )
                            .background(if (isCustomer) SuqiPrimary else colors.secondaryBackground)
                            .padding(12.dp)
                    ) {
                        Column {
                            ChatBubbleMessageContent(
                                text = msg.message,
                                isCustomer = isCustomer,
                                allProducts = allProducts,
                                allShops = allShops,
                                onNavigateToProduct = { pId ->
                                    viewModel.navigate(ScreenDestination.ProductDetails(pId))
                                },
                                onNavigateToShop = { sId ->
                                    viewModel.navigate(ScreenDestination.ShopStorefront(sId))
                                }
                            )
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

    // Modal Sheet to select a Product to #mention
    if (showProductPicker) {
        ModalBottomSheet(
            onDismissRequest = { showProductPicker = false },
            containerColor = colors.cardBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Select Product to Mention",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = colors.primaryText
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(modifier = Modifier.fillMaxHeight(0.6f)) {
                    items(allProducts) { p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    inputMessage = "$inputMessage #[${p.name}:${p.id}] ".trimStart()
                                    showProductPicker = false
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = p.primaryImageUrl,
                                contentDescription = p.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.secondaryBackground)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(p.name, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primaryText, maxLines = 1)
                                Text("ETB ${p.price.toLong()}", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = SuqiPrimary)
                            }
                            Icon(Icons.Filled.Add, contentDescription = "Add", tint = SuqiPrimary)
                        }
                    }
                }
            }
        }
    }

    // Modal Sheet to select a Shop to @mention
    if (showShopPicker) {
        ModalBottomSheet(
            onDismissRequest = { showShopPicker = false },
            containerColor = colors.cardBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Select Shop to Mention",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = colors.primaryText
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(modifier = Modifier.fillMaxHeight(0.6f)) {
                    items(allShops) { s ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    inputMessage = "$inputMessage @[${s.name}:${s.id}] ".trimStart()
                                    showShopPicker = false
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(SuqiPrimaryLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(s.name.take(2).uppercase(), fontWeight = FontWeight.Bold, color = SuqiPrimary)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(s.name, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primaryText)
                                Text(s.category, fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.secondaryText)
                            }
                            Icon(Icons.Filled.Add, contentDescription = "Add", tint = Color(0xFF2563EB))
                        }
                    }
                }
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
    onNavigateToShop: (String) -> Unit
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
                        val prod = allProducts.firstOrNull { it.name.contains(name, ignoreCase = true) || it.id.equals(name, ignoreCase = true) }
                        if (prod != null) {
                            MentionChip(
                                type = "product",
                                label = prod.name,
                                onClick = { onNavigateToProduct(prod.id) },
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    } else if (token.startsWith("@") && token.length > 1) {
                        val name = token.removePrefix("@")
                        val shop = allShops.firstOrNull { it.name.contains(name, ignoreCase = true) || it.id.equals(name, ignoreCase = true) }
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
            val symbol = match.groupValues[1]
            val label = match.groupValues[2]
            cleanedText = cleanedText.replace(match.value, "$symbol$label")
        }

        Column {
            Text(
                text = cleanedText,
                fontFamily = SuqiFontFamily,
                fontSize = 14.sp,
                color = if (isCustomer) Color.White else colors.primaryText
            )
            Spacer(modifier = Modifier.height(6.dp))
            matches.forEach { match ->
                val isProduct = match.groupValues[1] == "#"
                val label = match.groupValues[2]
                val id = match.groupValues[3]

                MentionChip(
                    type = if (isProduct) "product" else "shop",
                    label = label,
                    onClick = {
                        if (isProduct) onNavigateToProduct(id) else onNavigateToShop(id)
                    },
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}
