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
import com.example.data.model.AdminUserRowDto
import com.example.data.model.ProductQuestionDto
import com.example.data.model.ReviewDto
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.StatusType
import com.example.ui.components.SuqiBottomSheet
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
// S-10 (P2): Reviews & Q&A management
// ==========================================
@Composable
fun SellerReviewsQAScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val isOffline by viewModel.isOffline.collectAsState()
    var reviews by remember { mutableStateOf<List<ReviewDto>>(emptyList()) }
    var questions by remember { mutableStateOf<List<ProductQuestionDto>>(emptyList()) }
    var selectedTab by rememberSaveable { mutableStateOf("REVIEWS") }
    var isLoading by remember { mutableStateOf(true) }

    fun loadData() {
        coroutineScope.launch {
            val sid = resolveActiveSellerShopId(viewModel)
            val res = viewModel.repository.getReviews(targetId = sid)
            if (res.success && res.data != null) {
                reviews = res.data
            }
            val qRes = viewModel.repository.getProductQuestions(productId = "")
            if (qRes.success && qRes.data != null) {
                questions = qRes.data
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadData() }

    SuqiScreen(
        title = strings.sellerOps.sellerToolsShopTitle,
        subtitle = "${reviews.size + questions.size}",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tabs = listOf("REVIEWS" to strings.customerReviews, "QUESTIONS" to strings.questionsAnswers)
                tabs.forEach { (key, label) ->
                    val selected = key == selectedTab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 36.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (selected) SuqiPrimary else colors.cardBackground)
                            .border(if (!selected) 1.dp else 0.dp, colors.border, RoundedCornerShape(999.dp))
                            .clickable { selectedTab = key }
                            .testTag("seller_review_tab_$key")
                    ) {
                        Text(
                            text = label,
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (selected) Color.White else colors.primaryText,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (isLoading && reviews.isEmpty() && questions.isEmpty()) {
                com.example.ui.components.SuqiSkeletonList(count = 3, modifier = Modifier.fillMaxWidth())
            } else if (reviews.isEmpty() && questions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    com.example.ui.components.SuqiEmptyState(
                        icon = LucideIcons.MessageCircle,
                        title = strings.sellerOps.sellerToolsShopTitle,
                        subtitle = "No ${selectedTab.lowercase()} yet",
                        testTag = "seller_reviews_qa_empty"
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val tabs = listOf("REVIEWS" to strings.customerReviews, "QUESTIONS" to strings.questionsAnswers)
                            tabs.forEach { (key, label) ->
                                val selected = key == selectedTab
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 36.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(if (selected) SuqiPrimary else colors.cardBackground)
                                        .border(if (!selected) 1.dp else 0.dp, colors.border, RoundedCornerShape(999.dp))
                                        .clickable { selectedTab = key }
                                        .testTag("seller_review_tab_$key")
                                ) {
                                    Text(
                                        text = label,
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (selected) Color.White else colors.primaryText,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    if (selectedTab == "REVIEWS") {
                        items(reviews, key = { it.id }) { review ->
                            SuqiSectionCard(padding = 14.dp, modifier = Modifier.fillMaxWidth()) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = review.userName,
                                                fontFamily = SuqiFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = colors.primaryText
                                            )
                                            Text(
                                                text = review.date,
                                                fontSize = 11.sp,
                                                color = colors.mutedText
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            repeat(5) { i ->
                                                Icon(
                                                    imageVector = LucideIcons.Star,
                                                    contentDescription = "Icon",
                                                    tint = if (i < review.rating.toInt()) SuqiPrimary else colors.border,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = review.comment,
                                        fontSize = 13.sp,
                                        color = colors.secondaryText,
                                        maxLines = 3
                                    )
                                    if (review.sellerReply?.isNotBlank() == true) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        SuqiSectionCard(
                                            padding = 10.dp,
                                            containerColor = colors.secondaryBackground,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(horizontalAlignment = Alignment.Start) {
                                                Text(
                                                    text = "Your reply:",
                                                    fontSize = 11.sp,
                                                    color = colors.mutedText,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = review.sellerReply,
                                                    fontSize = 13.sp,
                                                    color = colors.primaryText,
                                                    maxLines = 3
                                                )
                                            }
                                        }
                                    } else {
                                        SuqiTextField(
                                            value = "",
                                            onValueChange = {},
                                            label = strings.admin.reviewReasonHint,
                                            modifier = Modifier.fillMaxWidth(),
                                            testTag = "seller_reply_${review.id}"
                                        )
                                        SuqiButton(
                                            text = strings.adminGov.sendBroadcastBtn,
                                            onClick = {
                                                coroutineScope.launch {
                                                    viewModel.repository.replyToReview(review.id, "Reply not yet wired")
                                                    viewModel.showToast("Reply sent")
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                                            isPrimary = false
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        items(questions, key = { it.id }) { q ->
                            SuqiSectionCard(padding = 14.dp, modifier = Modifier.fillMaxWidth()) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = q.askerName,
                                                fontFamily = SuqiFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = colors.primaryText
                                            )
                                            Text(
                                                text = q.createdAt,
                                                fontSize = 11.sp,
                                                color = colors.mutedText
                                            )
                                        }
                                        SuqiStatusBadge(
                                            if (q.answeredByShop) "Answered" else "Open",
                                            if (q.answeredByShop) StatusType.SUCCESS else StatusType.WARNING
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = q.question,
                                        fontSize = 13.sp,
                                        color = colors.secondaryText,
                                        maxLines = 3
                                    )
                                    if (q.answer.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Answer: ${q.answer}",
                                            fontSize = 13.sp,
                                            color = colors.primaryText,
                                            maxLines = 3
                                        )
                                    } else {
                                        SuqiTextField(
                                            value = "",
                                            onValueChange = {},
                                            label = strings.admin.reviewReasonHint,
                                            modifier = Modifier.fillMaxWidth(),
                                            testTag = "seller_answer_${q.id}"
                                        )
                                        SuqiButton(
                                            text = strings.adminGov.sendBroadcastBtn,
                                            onClick = {
                                                viewModel.showToast("Answer not yet wired")
                                            },
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
    }
}

// ==========================================
// S-11 (P3): Customers (list, segments, detail sheet)
// ==========================================
@Composable
fun SellerCustomersScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val isOffline by viewModel.isOffline.collectAsState()
    var customers by remember { mutableStateOf<List<AdminUserRowDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun loadCustomers() {
        coroutineScope.launch {
            // Seller-scoped: customers are the distinct buyers found in this shop's own orders.
            val res = viewModel.repository.getSellerOrders()
            if (res.success && res.data != null) {
                customers = res.data.groupBy { it.customerUserId }.map { (id, orders) ->
                    val latest = orders.first()
                    AdminUserRowDto(
                        id = id,
                        name = latest.customerName,
                        phone = latest.customerPhone,
                        role = "CUSTOMER",
                        ordersCount = orders.size,
                        totalSpent = orders.sumOf { it.totalAmount },
                        createdAt = latest.createdAt
                    )
                }
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadCustomers() }

    SuqiScreen(
        title = strings.sellerOps.sellerToolsShopTitle,
        subtitle = "${customers.size} customers",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading && customers.isEmpty()) {
                com.example.ui.components.SuqiSkeletonList(count = 3, modifier = Modifier.fillMaxWidth())
            } else if (customers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    com.example.ui.components.SuqiEmptyState(
                        icon = LucideIcons.Users,
                        title = strings.sellerOps.sellerToolsShopTitle,
                        subtitle = "No customers yet",
                        testTag = "seller_customers_empty"
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(customers, key = { it.id }) { customer ->
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
                                        text = customer.name,
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = colors.primaryText
                                    )
                                    Text(
                                        text = customer.phone,
                                        fontSize = 12.sp,
                                        color = colors.secondaryText
                                    )
                                    Text(
                                        text = "${customer.ordersCount} orders • ETB ${customer.totalSpent.toInt()} • ${customer.createdAt}",
                                        fontSize = 11.sp,
                                        color = colors.mutedText
                                    )
                                }
                                SuqiStatusBadge(customer.role, StatusType.NEUTRAL)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// S-17 (P3): Collections manager
// ==========================================
@Composable
fun SellerCollectionsScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val isOffline by viewModel.isOffline.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var collections by remember { mutableStateOf<List<com.example.data.model.ShopCollectionDto>>(emptyList()) }
    var products by remember { mutableStateOf<List<com.example.data.model.ProductDto>>(emptyList()) }
    var showSheet by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }

    fun load() {
        coroutineScope.launch {
            val res = viewModel.repository.getSellerCollections()
            if (res.success && res.data != null) collections = res.data
            val prod = viewModel.repository.getSellerProducts()
            if (prod.success && prod.data != null) products = prod.data
        }
    }

    LaunchedEffect(Unit) { load() }

    SuqiScreen(
        title = strings.sellerOps.sellerToolsShopTitle,
        subtitle = "${collections.size} collections",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (collections.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        com.example.ui.components.SuqiEmptyState(
                            icon = LucideIcons.ShoppingBag,
                            title = strings.sellerOps.sellerToolsShopTitle,
                            subtitle = "No collections yet",
                            testTag = "seller_collections_empty"
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(collections, key = { it.id }) { col ->
                            SuqiSectionCard(
                                padding = 14.dp,
                                modifier = Modifier.fillMaxWidth().testTag("collection_card_${col.id}")
                            ) {
                                Text(
                                    text = col.name,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = colors.primaryText
                                )
                                Text(
                                    text = "${col.productIds.size} products",
                                    fontSize = 12.sp,
                                    color = colors.secondaryText
                                )
                                com.example.ui.components.SuqiSwitchRow(
                                    title = "Visible in shop",
                                    checked = col.isVisible,
                                    onCheckedChange = { on ->
                                        coroutineScope.launch {
                                            viewModel.repository.saveSellerCollection(col.copy(isVisible = on))
                                            load()
                                        }
                                    },
                                    testTag = "collection_visible_${col.id}"
                                )
                                SuqiButton(
                                    text = "Delete",
                                    onClick = {
                                        coroutineScope.launch {
                                            val res = viewModel.repository.deleteSellerCollection(col.id)
                                            if (!res.success) viewModel.showToast(res.message ?: strings.retryLabel)
                                            load()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).testTag("collection_delete_${col.id}"),
                                    isPrimary = false
                                )
                            }
                        }
                    }
                }
            }
            SuqiButton(
                text = "Create Collection",
                onClick = {
                    newName = ""
                    selectedIds = emptySet()
                    showSheet = true
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(16.dp).testTag("collection_create_btn")
            )
        }

        if (showSheet) {
            SuqiBottomSheet(onDismissRequest = { showSheet = false }) {
                SuqiTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = "Collection name",
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "collection_name_input"
                )
                Spacer(modifier = Modifier.height(8.dp))
                products.take(20).forEach { prod ->
                    com.example.ui.components.SuqiSwitchRow(
                        title = prod.name,
                        checked = prod.id in selectedIds,
                        onCheckedChange = { on -> selectedIds = if (on) selectedIds + prod.id else selectedIds - prod.id },
                        testTag = "collection_product_${prod.id}"
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                SuqiButton(
                    text = strings.saveLabel,
                    onClick = {
                        coroutineScope.launch {
                            val res = viewModel.repository.saveSellerCollection(
                                com.example.data.model.ShopCollectionDto(id = "", name = newName, productIds = selectedIds.toList())
                            )
                            if (res.success) {
                                showSheet = false
                                load()
                            } else viewModel.showToast(res.message ?: strings.retryLabel)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("collection_save_btn")
                )
            }
        }
    }
}

// ==========================================
// S-18 (P3): Staff accounts
// ==========================================
@Composable
fun SellerStaffAccountsScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val isOffline by viewModel.isOffline.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var staff by remember { mutableStateOf<List<com.example.data.model.ShopStaffDto>>(emptyList()) }
    var showSheet by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("ORDERS") }
    val owner = viewModel.currentUser.value

    fun load() {
        coroutineScope.launch {
            val res = viewModel.repository.getSellerStaff()
            if (res.success && res.data != null) staff = res.data
        }
    }

    LaunchedEffect(Unit) { load() }

    SuqiScreen(
        title = strings.sellerOps.sellerToolsShopTitle,
        subtitle = "${staff.size + 1} accounts",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (owner != null) {
                    item(key = "owner") {
                        SuqiSectionCard(padding = 14.dp, modifier = Modifier.fillMaxWidth().testTag("seller_staff_${owner.id}")) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(owner.name, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primaryText)
                                    Text(owner.phone, fontSize = 12.sp, color = colors.secondaryText)
                                }
                                SuqiStatusBadge("OWNER", StatusType.SUCCESS)
                            }
                        }
                    }
                }
                items(staff, key = { it.id }) { member ->
                    SuqiSectionCard(padding = 14.dp, modifier = Modifier.fillMaxWidth().testTag("staff_card_${member.id}")) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(member.name, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primaryText)
                                Text(member.phone, fontSize = 12.sp, color = colors.secondaryText)
                            }
                            SuqiStatusBadge(member.role, StatusType.NEUTRAL)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        com.example.ui.components.SuqiSwitchRow(
                            title = "Active",
                            checked = member.isActive,
                            onCheckedChange = { on ->
                                coroutineScope.launch {
                                    viewModel.repository.saveSellerStaff(member.copy(isActive = on))
                                    load()
                                }
                            },
                            testTag = "staff_active_${member.id}"
                        )
                        SuqiButton(
                            text = "Remove",
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.repository.deleteSellerStaff(member.id)
                                    load()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).testTag("staff_delete_${member.id}"),
                            isPrimary = false
                        )
                    }
                }
            }
            SuqiButton(
                text = "Invite Staff",
                onClick = {
                    name = ""
                    phone = ""
                    role = "ORDERS"
                    showSheet = true
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(16.dp).testTag("staff_invite_btn")
            )
        }

        if (showSheet) {
            SuqiBottomSheet(onDismissRequest = { showSheet = false }) {
                SuqiTextField(value = name, onValueChange = { name = it }, label = "Name", modifier = Modifier.fillMaxWidth(), testTag = "staff_name_input")
                Spacer(modifier = Modifier.height(8.dp))
                SuqiTextField(value = phone, onValueChange = { phone = it }, label = "Phone", modifier = Modifier.fillMaxWidth(), testTag = "staff_phone_input")
                Spacer(modifier = Modifier.height(8.dp))
                com.example.ui.components.SuqiFilterChips(
                    items = listOf("MANAGER" to "Manager", "ORDERS" to "Orders", "CATALOG" to "Catalog"),
                    selectedKey = role,
                    onSelect = { role = it },
                    testTagPrefix = "staff_role"
                )
                Spacer(modifier = Modifier.height(12.dp))
                SuqiButton(
                    text = strings.saveLabel,
                    onClick = {
                        coroutineScope.launch {
                            val res = viewModel.repository.saveSellerStaff(
                                com.example.data.model.ShopStaffDto(id = "", name = name, phone = phone, role = role)
                            )
                            if (res.success) {
                                showSheet = false
                                load()
                            } else viewModel.showToast(res.message ?: strings.retryLabel)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("staff_save_btn")
                )
            }
        }
    }
}
