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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.example.data.model.AdminUserRowDto
import com.example.data.model.ProductQuestionDto
import com.example.data.model.ReviewDto
import com.example.data.model.ShopCollectionDto
import com.example.data.model.ShopStaffDto
import com.example.ui.SuqiViewModel
import com.example.ui.components.StatusType
import com.example.ui.components.SuqiBottomSheet
import com.example.ui.components.SuqiButton
import com.example.ui.components.SuqiDragHandleList
import com.example.ui.components.SuqiEmptyState
import com.example.ui.components.SuqiFilterChips
import com.example.ui.components.SuqiImagePickerTile
import com.example.ui.components.SuqiMaxContentWidth
import com.example.ui.components.SuqiScreen
import com.example.ui.components.SuqiSectionCard
import com.example.ui.components.SuqiSkeletonList
import com.example.ui.components.SuqiStatusBadge
import com.example.ui.components.SuqiSwitchRow
import com.example.ui.components.SuqiTextField
import com.example.ui.i18n.suqiStrings
import com.example.ui.theme.LocalSuqiColors
import com.example.ui.theme.LucideIcons
import com.example.ui.theme.SuqiFontFamily
import com.example.ui.theme.SuqiPrimary
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
    val p6 = strings.admin6
    val isOffline by viewModel.isOffline.collectAsState()
    var reviews by remember { mutableStateOf<List<ReviewDto>>(emptyList()) }
    var questions by remember { mutableStateOf<List<ProductQuestionDto>>(emptyList()) }
    var selectedTab by rememberSaveable { mutableStateOf("REVIEWS") }
    var isLoading by remember { mutableStateOf(true) }
    val replyDrafts = remember { mutableStateMapOf<String, String>() }
    val answerDrafts = remember { mutableStateMapOf<String, String>() }

    fun loadData() {
        coroutineScope.launch {
            val sid = resolveActiveSellerShopId(viewModel)
            val res = viewModel.repository.getReviews(targetId = sid)
            if (res.success && res.data != null) {
                reviews = res.data
            }
            val prodRes = viewModel.repository.getProducts(shopId = sid)
            val shopProducts = prodRes.data.orEmpty()
            val collected = mutableListOf<ProductQuestionDto>()
            for (p in shopProducts) {
                val qRes = viewModel.repository.getProductQuestions(productId = p.id)
                if (qRes.success && qRes.data != null) {
                    collected.addAll(qRes.data)
                }
            }
            questions = collected.distinctBy { it.id }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadData() }

    SuqiScreen(
        title = strings.seller.reviewsQaTitle,
        subtitle = "${reviews.size + questions.size}",
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
            Column(
                modifier = Modifier
                    .widthIn(max = SuqiMaxContentWidth)
                    .fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = listOf("REVIEWS" to strings.customerReviews, "QUESTIONS" to strings.questionsAnswers)
                    tabs.forEach { (key, label) ->
                        val selected = key == selectedTab
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (selected) SuqiPrimary else colors.cardBackground)
                                .border(if (!selected) 1.dp else 0.dp, colors.border, RoundedCornerShape(999.dp))
                                .clickable { selectedTab = key }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("seller_review_tab_$key")
                        ) {
                            Text(
                                text = label,
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (selected) Color.White else colors.primaryText
                            )
                        }
                    }
                }

                val tabItemsEmpty = if (selectedTab == "REVIEWS") reviews.isEmpty() else questions.isEmpty()
                if (isLoading && reviews.isEmpty() && questions.isEmpty()) {
                    SuqiSkeletonList(count = 3, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                } else if (tabItemsEmpty) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        SuqiEmptyState(
                            icon = LucideIcons.MessageCircle,
                            title = if (selectedTab == "REVIEWS") strings.customerReviews else strings.questionsAnswers,
                            subtitle = if (selectedTab == "REVIEWS") strings.buyer.noReviewsSub else strings.checkout.awaitingSellerReply,
                            testTag = "seller_reviews_qa_empty"
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (selectedTab == "REVIEWS") {
                            items(reviews, key = { it.id }) { review ->
                                val draft = replyDrafts[review.id].orEmpty()
                                SuqiSectionCard(padding = 14.dp, modifier = Modifier.fillMaxWidth()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
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
                                                        contentDescription = null,
                                                        tint = if (i < review.rating.toInt()) SuqiPrimary else colors.border,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = review.comment,
                                            fontSize = 13.sp,
                                            color = colors.secondaryText
                                        )
                                        if (review.sellerReply?.isNotBlank() == true) {
                                            SuqiSectionCard(
                                                padding = 10.dp,
                                                containerColor = colors.secondaryBackground,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(horizontalAlignment = Alignment.Start) {
                                                    Text(
                                                        text = strings.phase5.sellerRepliedLabel,
                                                        fontSize = 11.sp,
                                                        color = colors.mutedText,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = review.sellerReply,
                                                        fontSize = 13.sp,
                                                        color = colors.primaryText
                                                    )
                                                }
                                            }
                                        } else {
                                            SuqiTextField(
                                                value = draft,
                                                onValueChange = { replyDrafts[review.id] = it },
                                                label = p6.replyHint,
                                                placeholder = p6.replyHint,
                                                modifier = Modifier.fillMaxWidth(),
                                                testTag = "seller_reply_${review.id}"
                                            )
                                            SuqiButton(
                                                text = p6.sendReplyBtn,
                                                enabled = draft.isNotBlank(),
                                                onClick = {
                                                    coroutineScope.launch {
                                                        val res = viewModel.repository.replyToReview(review.id, draft.trim())
                                                        if (res.success) {
                                                            replyDrafts.remove(review.id)
                                                            loadData()
                                                            viewModel.showToast(strings.common.doneLabel)
                                                        } else {
                                                            viewModel.showToast(res.message ?: strings.retryLabel)
                                                        }
                                                    }
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = 48.dp)
                                                    .testTag("seller_reply_send_${review.id}"),
                                                isPrimary = false
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            items(questions, key = { it.id }) { q ->
                                val draft = answerDrafts[q.id].orEmpty()
                                SuqiSectionCard(padding = 14.dp, modifier = Modifier.fillMaxWidth()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
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
                                                if (q.answeredByShop || q.answer.isNotBlank()) p6.answeredBadge else p6.unansweredBadge,
                                                if (q.answeredByShop || q.answer.isNotBlank()) StatusType.SUCCESS else StatusType.WARNING
                                            )
                                        }
                                        Text(
                                            text = q.question,
                                            fontSize = 13.sp,
                                            color = colors.secondaryText
                                        )
                                        if (q.answer.isNotBlank()) {
                                            SuqiSectionCard(
                                                padding = 10.dp,
                                                containerColor = colors.secondaryBackground,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = q.answer,
                                                    fontSize = 13.sp,
                                                    color = colors.primaryText
                                                )
                                            }
                                        } else {
                                            SuqiTextField(
                                                value = draft,
                                                onValueChange = { answerDrafts[q.id] = it },
                                                label = p6.answerHint,
                                                placeholder = p6.answerHint,
                                                modifier = Modifier.fillMaxWidth(),
                                                testTag = "seller_answer_${q.id}"
                                            )
                                            SuqiButton(
                                                text = p6.sendAnswerBtn,
                                                enabled = draft.isNotBlank(),
                                                onClick = {
                                                    coroutineScope.launch {
                                                        val res = viewModel.repository.answerProductQuestion(
                                                            productId = q.productId,
                                                            questionId = q.id,
                                                            answer = draft.trim()
                                                        )
                                                        if (res.success) {
                                                            answerDrafts.remove(q.id)
                                                            loadData()
                                                            viewModel.showToast(strings.common.doneLabel)
                                                        } else {
                                                            viewModel.showToast(res.message ?: strings.retryLabel)
                                                        }
                                                    }
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = 48.dp)
                                                    .testTag("seller_answer_send_${q.id}"),
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
        title = strings.seller.customersTitle,
        subtitle = "${customers.size}",
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
            Column(
                modifier = Modifier
                    .widthIn(max = SuqiMaxContentWidth)
                    .fillMaxSize()
            ) {
                if (isLoading && customers.isEmpty()) {
                    SuqiSkeletonList(count = 3, modifier = Modifier.fillMaxWidth().padding(16.dp))
                } else if (customers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        SuqiEmptyState(
                            icon = LucideIcons.Users,
                            title = strings.seller.customersTitle,
                            subtitle = strings.admin6.noUsersSub,
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
                                    Column(modifier = Modifier.weight(1f)) {
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
                                            text = "${customer.ordersCount} ${strings.admin6.statOrdersLabel} • ETB ${customer.totalSpent.toInt()} • ${customer.createdAt}",
                                            fontSize = 11.sp,
                                            color = colors.mutedText
                                        )
                                    }
                                    SuqiStatusBadge(strings.admin6.roleBuyer, StatusType.NEUTRAL)
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
// S-17 (P3): Collections manager (P6-7 reorder + cover photo + edit)
// ==========================================
@Composable
fun SellerCollectionsScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val p6 = strings.admin6
    val isOffline by viewModel.isOffline.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var collections by remember { mutableStateOf<List<ShopCollectionDto>>(emptyList()) }
    var products by remember { mutableStateOf<List<com.example.data.model.ProductDto>>(emptyList()) }
    var showSheet by remember { mutableStateOf(false) }
    var editingCollection by remember { mutableStateOf<ShopCollectionDto?>(null) }
    var newName by remember { mutableStateOf("") }
    var coverImageUrl by remember { mutableStateOf("") }
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }

    fun load() {
        coroutineScope.launch {
            val res = viewModel.repository.getSellerCollections()
            if (res.success && res.data != null) {
                collections = res.data.sortedBy { it.sortOrder }
            }
            val prod = viewModel.repository.getSellerProducts()
            if (prod.success && prod.data != null) products = prod.data
        }
    }

    fun reorderCollection(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in collections.indices || toIndex !in collections.indices) return
        val mutable = collections.toMutableList()
        val moved = mutable.removeAt(fromIndex)
        mutable.add(toIndex, moved)
        val reindexed = mutable.mapIndexed { idx, col -> col.copy(sortOrder = idx) }
        collections = reindexed
        coroutineScope.launch {
            reindexed.forEach { col ->
                viewModel.repository.saveSellerCollection(col)
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    SuqiScreen(
        title = strings.seller.collectionsManagerTitle,
        subtitle = "${collections.size}",
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
            Column(
                modifier = Modifier
                    .widthIn(max = SuqiMaxContentWidth)
                    .fillMaxSize()
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (collections.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            SuqiEmptyState(
                                icon = LucideIcons.ShoppingBag,
                                title = strings.seller.collectionsManagerTitle,
                                subtitle = strings.buyer.collectionsTabLabel,
                                testTag = "seller_collections_empty"
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            item {
                                SuqiDragHandleList(
                                    items = collections,
                                    onMove = { from, to -> reorderCollection(from, to) }
                                ) { _, col, moveUp, moveDown ->
                                    SuqiSectionCard(
                                        padding = 14.dp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("collection_card_${col.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(52.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(colors.secondaryBackground),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (col.coverImageUrl.isNotBlank()) {
                                                    AsyncImage(
                                                        model = col.coverImageUrl,
                                                        contentDescription = col.name,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = LucideIcons.ShoppingBag,
                                                        contentDescription = col.name,
                                                        tint = SuqiPrimary,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = col.name,
                                                    fontFamily = SuqiFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = colors.primaryText,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${col.productIds.size} ${strings.productsTab}",
                                                    fontSize = 12.sp,
                                                    color = colors.secondaryText
                                                )
                                            }
                                            Row {
                                                IconButton(
                                                    onClick = { moveUp?.invoke() },
                                                    enabled = moveUp != null,
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .testTag("collection_up_${col.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = LucideIcons.ChevronUp,
                                                        contentDescription = p6.moveUpLabel,
                                                        tint = if (moveUp != null) colors.primaryText else colors.mutedText,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { moveDown?.invoke() },
                                                    enabled = moveDown != null,
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .testTag("collection_down_${col.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = LucideIcons.ChevronDown,
                                                        contentDescription = p6.moveDownLabel,
                                                        tint = if (moveDown != null) colors.primaryText else colors.mutedText,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        SuqiSwitchRow(
                                            title = p6.collectionVisibleLabel,
                                            checked = col.isVisible,
                                            onCheckedChange = { on ->
                                                coroutineScope.launch {
                                                    viewModel.repository.saveSellerCollection(col.copy(isVisible = on))
                                                    load()
                                                }
                                            },
                                            testTag = "collection_visible_${col.id}"
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            SuqiButton(
                                                text = p6.editCollectionBtn,
                                                onClick = {
                                                    editingCollection = col
                                                    newName = col.name
                                                    coverImageUrl = col.coverImageUrl
                                                    selectedIds = col.productIds.toSet()
                                                    showSheet = true
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .heightIn(min = 48.dp)
                                                    .testTag("collection_edit_${col.id}"),
                                                isPrimary = false
                                            )
                                            SuqiButton(
                                                text = strings.deleteLabel,
                                                onClick = {
                                                    coroutineScope.launch {
                                                        val res = viewModel.repository.deleteSellerCollection(col.id)
                                                        if (!res.success) viewModel.showToast(res.message ?: strings.retryLabel)
                                                        load()
                                                    }
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .heightIn(min = 48.dp)
                                                    .testTag("collection_delete_${col.id}"),
                                                isPrimary = false
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                SuqiButton(
                    text = p6.createCollectionBtn,
                    onClick = {
                        editingCollection = null
                        newName = ""
                        coverImageUrl = ""
                        selectedIds = emptySet()
                        showSheet = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .padding(16.dp)
                        .testTag("collection_create_btn")
                )
            }
        }

        if (showSheet) {
            SuqiBottomSheet(onDismissRequest = { showSheet = false }) {
                SuqiTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = p6.collectionNameHint,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "collection_name_input"
                )
                Spacer(modifier = Modifier.height(10.dp))
                SuqiImagePickerTile(
                    label = p6.collectionCoverLabel,
                    imageUri = coverImageUrl.takeIf { it.isNotBlank() },
                    onImagePicked = { coverImageUrl = it },
                    onRemove = { coverImageUrl = "" },
                    tileHeight = 100.dp,
                    testTag = "collection_cover_picker"
                )
                Spacer(modifier = Modifier.height(8.dp))
                products.take(20).forEach { prod ->
                    SuqiSwitchRow(
                        title = prod.name,
                        checked = prod.id in selectedIds,
                        onCheckedChange = { on -> selectedIds = if (on) selectedIds + prod.id else selectedIds - prod.id },
                        testTag = "collection_product_${prod.id}"
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                SuqiButton(
                    text = strings.saveLabel,
                    enabled = newName.isNotBlank(),
                    onClick = {
                        coroutineScope.launch {
                            val existing = editingCollection
                            val dto = if (existing != null) {
                                existing.copy(
                                    name = newName.trim(),
                                    coverImageUrl = coverImageUrl,
                                    productIds = selectedIds.toList()
                                )
                            } else {
                                ShopCollectionDto(
                                    id = "",
                                    name = newName.trim(),
                                    coverImageUrl = coverImageUrl,
                                    sortOrder = collections.size,
                                    productIds = selectedIds.toList()
                                )
                            }
                            val res = viewModel.repository.saveSellerCollection(dto)
                            if (res.success) {
                                showSheet = false
                                load()
                            } else {
                                viewModel.showToast(res.message ?: strings.retryLabel)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("collection_save_btn")
                )
            }
        }
    }
}

// ==========================================
// S-18 (P3): Staff accounts (P6-6 roles & permissions + edit)
// ==========================================
@Composable
fun SellerStaffAccountsScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val p6 = strings.admin6
    val isOffline by viewModel.isOffline.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var staff by remember { mutableStateOf<List<ShopStaffDto>>(emptyList()) }
    var showSheet by remember { mutableStateOf(false) }
    var editingStaff by remember { mutableStateOf<ShopStaffDto?>(null) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("ORDERS") }
    val owner by viewModel.currentUser.collectAsState()

    fun rolePermissionDesc(roleKey: String): String = when (roleKey.uppercase()) {
        "MANAGER" -> p6.staffRoleManagerDesc
        "CATALOG" -> p6.staffRoleCatalogDesc
        else -> p6.staffRoleOrdersDesc
    }

    fun load() {
        coroutineScope.launch {
            val res = viewModel.repository.getSellerStaff()
            if (res.success && res.data != null) staff = res.data
        }
    }

    LaunchedEffect(Unit) { load() }

    SuqiScreen(
        title = strings.seller.staffAccountsTitle,
        subtitle = "${staff.size + 1}",
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
            Column(
                modifier = Modifier
                    .widthIn(max = SuqiMaxContentWidth)
                    .fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val ownerSnapshot = owner
                    if (ownerSnapshot != null) {
                        item(key = "owner") {
                            SuqiSectionCard(
                                padding = 14.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("seller_staff_${ownerSnapshot.id}")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            ownerSnapshot.name,
                                            fontFamily = SuqiFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = colors.primaryText
                                        )
                                        Text(ownerSnapshot.phone, fontSize = 12.sp, color = colors.secondaryText)
                                        Text(p6.staffRoleManagerDesc, fontSize = 11.sp, color = colors.mutedText)
                                    }
                                    SuqiStatusBadge("OWNER", StatusType.SUCCESS)
                                }
                            }
                        }
                    }
                    items(staff, key = { it.id }) { member ->
                        SuqiSectionCard(
                            padding = 14.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("staff_card_${member.id}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        member.name,
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = colors.primaryText
                                    )
                                    Text(member.phone, fontSize = 12.sp, color = colors.secondaryText)
                                    Text(
                                        text = rolePermissionDesc(member.role),
                                        fontSize = 11.sp,
                                        color = colors.mutedText
                                    )
                                }
                                SuqiStatusBadge(member.role, if (member.isActive) StatusType.SUCCESS else StatusType.NEUTRAL)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            SuqiSwitchRow(
                                title = strings.adminOps.activeStatus,
                                checked = member.isActive,
                                onCheckedChange = { on ->
                                    coroutineScope.launch {
                                        viewModel.repository.saveSellerStaff(member.copy(isActive = on))
                                        load()
                                    }
                                },
                                testTag = "staff_active_${member.id}"
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SuqiButton(
                                    text = p6.editStaffBtn,
                                    onClick = {
                                        editingStaff = member
                                        name = member.name
                                        phone = member.phone
                                        role = member.role
                                        showSheet = true
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("staff_edit_${member.id}"),
                                    isPrimary = false
                                )
                                SuqiButton(
                                    text = strings.deleteLabel,
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.repository.deleteSellerStaff(member.id)
                                            load()
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("staff_delete_${member.id}"),
                                    isPrimary = false
                                )
                            }
                        }
                    }
                }
                SuqiButton(
                    text = p6.inviteStaffBtn,
                    onClick = {
                        editingStaff = null
                        name = ""
                        phone = ""
                        role = "ORDERS"
                        showSheet = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .padding(16.dp)
                        .testTag("staff_invite_btn")
                )
            }
        }

        if (showSheet) {
            SuqiBottomSheet(onDismissRequest = { showSheet = false }) {
                SuqiTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = strings.buyer.fullNameLabel,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "staff_name_input"
                )
                Spacer(modifier = Modifier.height(8.dp))
                SuqiTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = strings.buyer.phoneNumberLabel,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "staff_phone_input"
                )
                Spacer(modifier = Modifier.height(8.dp))
                SuqiFilterChips(
                    items = listOf(
                        "MANAGER" to p6.roleManager,
                        "ORDERS" to p6.roleOrdersOnly,
                        "CATALOG" to p6.roleCatalogOnly
                    ),
                    selectedKey = role,
                    onSelect = { role = it },
                    testTagPrefix = "staff_role"
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = rolePermissionDesc(role),
                    fontFamily = SuqiFontFamily,
                    fontSize = 12.sp,
                    color = colors.secondaryText,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                SuqiButton(
                    text = strings.saveLabel,
                    enabled = name.isNotBlank() && phone.isNotBlank(),
                    onClick = {
                        coroutineScope.launch {
                            val existing = editingStaff
                            val dto = existing?.copy(
                                name = name.trim(),
                                phone = phone.trim(),
                                role = role
                            ) ?: ShopStaffDto(id = "", name = name.trim(), phone = phone.trim(), role = role)
                            val res = viewModel.repository.saveSellerStaff(dto)
                            if (res.success) {
                                showSheet = false
                                load()
                            } else {
                                viewModel.showToast(res.message ?: strings.retryLabel)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("staff_save_btn")
                )
            }
        }
    }
}
