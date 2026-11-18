package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.api.BackendConfig
import com.example.data.model.FlaggedReviewDto
import com.example.data.model.ReviewDto
import com.example.ui.SuqiViewModel
import com.example.ui.components.*
import com.example.ui.i18n.suqiStrings
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ReviewsV2Screen(
    targetId: String,
    targetName: String,
    targetType: String,
    initialStarFilter: Int? = null,
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val p5 = strings.phase5
    val isOffline by viewModel.isOffline.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var reviews by remember { mutableStateOf<List<ReviewDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isShopOwner by remember { mutableStateOf(false) }

    var starFilter by rememberSaveable(initialStarFilter) { mutableStateOf(initialStarFilter) }
    var withPhotosOnly by rememberSaveable { mutableStateOf(false) }
    var sortMode by rememberSaveable { mutableStateOf("NEWEST") }

    var showWriteSheet by remember { mutableStateOf(false) }
    var rating by remember { mutableDoubleStateOf(5.0) }
    var comment by remember { mutableStateOf("") }
    val selectedTags = remember { mutableStateListOf<String>() }
    val reviewPhotos = remember { mutableStateListOf<String>() }
    var isAnonymous by remember { mutableStateOf(false) }

    var replyTarget by remember { mutableStateOf<ReviewDto?>(null) }
    var replyText by remember { mutableStateOf("") }
    var reportTarget by remember { mutableStateOf<ReviewDto?>(null) }
    var reportReason by remember { mutableStateOf("") }
    var viewerImageUrl by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = showWriteSheet || replyTarget != null || reportTarget != null || viewerImageUrl != null) {
        when {
            viewerImageUrl != null -> viewerImageUrl = null
            reportTarget != null -> reportTarget = null
            replyTarget != null -> replyTarget = null
            showWriteSheet -> showWriteSheet = false
        }
    }

    fun loadAll() {
        coroutineScope.launch {
            isLoading = reviews.isEmpty()
            val res = viewModel.repository.getReviews(targetId)
            if (res.success && res.data != null) {
                reviews = res.data
            }
            val ownerShopId = currentUser?.shopId
            isShopOwner = if (!ownerShopId.isNullOrBlank()) {
                when (targetType.uppercase()) {
                    "SHOP" -> ownerShopId == targetId
                    "PRODUCT" -> viewModel.repository.getProductDetails(targetId).data?.shopId == ownerShopId
                    else -> false
                }
            } else {
                false
            }
            isLoading = false
        }
    }

    LaunchedEffect(targetId, currentUser?.id) {
        loadAll()
    }

    val filteredReviews = remember(reviews, starFilter, withPhotosOnly, sortMode) {
        reviews
            .filter { rev ->
                val starOk = starFilter == null || rev.rating.toInt() == starFilter
                val photoOk = !withPhotosOnly || rev.photos.isNotEmpty()
                starOk && photoOk
            }
            .let { list ->
                if (sortMode == "HELPFUL") list.sortedByDescending { it.helpfulCount }
                else list
            }
    }

    val avgRating = remember(reviews) {
        if (reviews.isEmpty()) 0.0 else reviews.map { it.rating }.average()
    }

    SuqiScreen(
        title = strings.customerReviews,
        subtitle = "$targetName (${reviews.size})",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.requireSignedInOrPrompt(strings.buyer.writeReviewTitle) {
                        showWriteSheet = true
                    }
                },
                containerColor = SuqiPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("write_review_fab")
            ) {
                Icon(LucideIcons.MessageSquarePlus, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(strings.buyer.writeReviewTitle)
            }
        },
        modifier = modifier
    ) { paddingValues ->
        if (isLoading && reviews.isEmpty()) {
            SuqiSkeletonList(count = 4, modifier = Modifier.padding(paddingValues).padding(top = 12.dp))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    SuqiSectionCard(padding = 16.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(88.dp)) {
                                Text(
                                    text = String.format("%.1f", avgRating),
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
                                            tint = if (idx < avgRating.toInt()) SuqiWarning else colors.border,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = String.format(strings.checkout.ratingsCountTemplate, reviews.size),
                                    fontFamily = SuqiFontFamily,
                                    fontSize = 11.sp,
                                    color = colors.secondaryText
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                val total = reviews.size.coerceAtLeast(1)
                                for (stars in 5 downTo 1) {
                                    val count = reviews.count { it.rating.toInt() == stars }
                                    val fraction = if (reviews.isNotEmpty()) count.toFloat() / total.toFloat() else 0f
                                    val selected = starFilter == stars
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (selected) colors.primaryTint else Color.Transparent)
                                            .clickable {
                                                starFilter = if (selected) null else stars
                                            }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                            .testTag("reviews_star_bar_$stars")
                                    ) {
                                        Text("${stars}★", fontFamily = SuqiFontFamily, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = if (selected) SuqiPrimary else colors.secondaryText, modifier = Modifier.width(26.dp))
                                        LinearProgressIndicator(
                                            progress = { fraction },
                                            color = SuqiWarning,
                                            trackColor = colors.secondaryBackground,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(7.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(count.toString(), fontFamily = SuqiFontFamily, fontSize = 11.sp, color = colors.mutedText, modifier = Modifier.width(22.dp), textAlign = TextAlign.End)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            FilterChip(
                                selected = starFilter == null && !withPhotosOnly,
                                onClick = {
                                    starFilter = null
                                    withPhotosOnly = false
                                },
                                label = { Text(p5.filterAll) },
                                modifier = Modifier.testTag("reviews_filter_all")
                            )
                            FilterChip(
                                selected = withPhotosOnly,
                                onClick = { withPhotosOnly = !withPhotosOnly },
                                label = { Text(p5.withPhotosFilter) },
                                leadingIcon = { Icon(LucideIcons.Camera, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                modifier = Modifier.testTag("reviews_filter_photos")
                            )
                            for (s in 5 downTo 1) {
                                FilterChip(
                                    selected = starFilter == s,
                                    onClick = { starFilter = if (starFilter == s) null else s },
                                    label = { Text("${s}★") },
                                    modifier = Modifier.testTag("reviews_filter_star_$s")
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("NEWEST" to p5.sortNewest, "HELPFUL" to p5.sortHelpful).forEach { (key, label) ->
                                val selected = sortMode == key
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .heightIn(min = 40.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(if (selected) SuqiPrimary else colors.secondaryBackground)
                                        .clickable { sortMode = key }
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                        .testTag("reviews_sort_${key.lowercase()}")
                                ) {
                                    Text(
                                        text = label,
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = if (selected) Color.White else colors.primaryText
                                    )
                                }
                            }
                        }
                    }
                }

                if (filteredReviews.isEmpty()) {
                    item {
                        SuqiSectionCard(padding = 20.dp) {
                            SuqiEmptyState(
                                icon = LucideIcons.Star,
                                title = strings.buyer.noReviewsTitle,
                                subtitle = strings.buyer.noReviewsSub,
                                ctaText = strings.buyer.writeReviewTitle,
                                onCtaClick = {
                                    viewModel.requireSignedInOrPrompt(strings.buyer.writeReviewTitle) {
                                        showWriteSheet = true
                                    }
                                },
                                testTag = "reviews_empty"
                            )
                        }
                    }
                } else {
                    items(filteredReviews, key = { it.id }) { rev ->
                        SuqiSectionCard(padding = 14.dp, modifier = Modifier.testTag("review_card_${rev.id}")) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = rev.userName,
                                            fontFamily = SuqiFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = colors.primaryText
                                        )
                                        if (rev.isVerifiedPurchase) {
                                            SuqiStatusBadge(strings.buyer.verifiedPurchaseLabel, StatusType.SUCCESS)
                                        }
                                    }
                                }
                                Text(rev.date, fontFamily = SuqiFontFamily, fontSize = 11.sp, color = colors.mutedText)
                            }
                            Spacer(Modifier.height(4.dp))
                            Row {
                                for (i in 1..5) {
                                    Icon(
                                        imageVector = LucideIcons.StarFilled,
                                        contentDescription = null,
                                        tint = if (i <= rev.rating) SuqiWarning else colors.border,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            if (rev.tags.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(rev.tags) { tag ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(colors.primaryTint)
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(tag, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = SuqiPrimary)
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(rev.comment, fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.primaryText)

                            if (rev.photos.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(rev.photos) { photoUrl ->
                                        AsyncImage(
                                            model = photoUrl,
                                            contentDescription = strings.customerReviews,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                                                .clickable { viewerImageUrl = photoUrl }
                                                .testTag("review_photo_${rev.id}")
                                        )
                                    }
                                }
                            }

                            val revSellerReply = rev.sellerReply
                            if (!revSellerReply.isNullOrBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.shopTint)
                                        .border(1.dp, colors.shopBorder, RoundedCornerShape(12.dp))
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "${p5.sellerRepliedLabel} • ${rev.sellerReplyAt.orEmpty()}",
                                            fontFamily = SuqiFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = SuqiPrimary
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(revSellerReply, fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.primaryText)
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            val res = viewModel.repository.toggleReviewHelpful(rev.id)
                                            if (res.success && res.data != null) {
                                                reviews = reviews.map { if (it.id == rev.id) res.data else it }
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(999.dp),
                                    border = BorderStroke(1.dp, if (rev.isHelpfulByCurrentUser) SuqiPrimary else colors.border),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("review_helpful_${rev.id}")
                                ) {
                                    Icon(
                                        imageVector = LucideIcons.ThumbsUp,
                                        contentDescription = null,
                                        tint = if (rev.isHelpfulByCurrentUser) SuqiPrimary else colors.secondaryText,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = String.format(p5.helpfulBtnTemplate, rev.helpfulCount),
                                        fontSize = 12.sp,
                                        color = if (rev.isHelpfulByCurrentUser) SuqiPrimary else colors.primaryText
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (isShopOwner) {
                                        TextButton(
                                            onClick = {
                                                replyTarget = rev
                                                replyText = rev.sellerReply.orEmpty()
                                            },
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .testTag("review_reply_btn_${rev.id}")
                                        ) {
                                            Text(p5.writeReplyBtn, fontSize = 12.sp, color = SuqiPrimary, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    TextButton(
                                        onClick = {
                                            reportTarget = rev
                                            reportReason = ""
                                        },
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("review_report_${rev.id}")
                                    ) {
                                        Text(strings.buyer.reportReviewLabel, fontSize = 12.sp, color = colors.mutedText)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showWriteSheet) {
        val ratingLabel = when (rating.toInt()) {
            5 -> p5.ratingExcellent
            4 -> p5.ratingGood
            3 -> p5.ratingOkay
            2 -> p5.ratingFair
            else -> p5.ratingPoor
        }
        val tagChoices = remember(p5.reviewTagsListCsv) {
            p5.reviewTagsListCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
        }
        SuqiBottomSheet(onDismissRequest = { showWriteSheet = false }, title = strings.buyer.writeReviewTitle) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (i in 1..5) {
                            IconButton(
                                onClick = { rating = i.toDouble() },
                                modifier = Modifier.size(48.dp).testTag("review_star_$i")
                            ) {
                                Icon(
                                    LucideIcons.StarFilled,
                                    contentDescription = "$i ★",
                                    tint = if (i <= rating) SuqiWarning else colors.border,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                    Text(ratingLabel, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SuqiPrimary)
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tagChoices.size) { idx ->
                        val tag = tagChoices[idx]
                        val selected = tag in selectedTags
                        FilterChip(
                            selected = selected,
                            onClick = {
                                if (selected) selectedTags.remove(tag) else selectedTags.add(tag)
                            },
                            label = { Text(tag, fontSize = 12.sp) },
                            modifier = Modifier.testTag("review_tag_$idx")
                        )
                    }
                }

                SuqiTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = strings.buyer.writeReviewTitle,
                    singleLine = false,
                    testTag = "review_comment_input"
                )

                Text(
                    text = p5.addEvidencePhotosLabel,
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = colors.secondaryText
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    for (idx in 0 until 4) {
                        val uri = reviewPhotos.getOrNull(idx)
                        SuqiImagePickerTile(
                            label = "#${idx + 1}",
                            imageUri = uri,
                            onImagePicked = { newUri ->
                                if (idx < reviewPhotos.size) reviewPhotos[idx] = newUri else reviewPhotos.add(newUri)
                            },
                            onRemove = if (uri != null) {
                                { reviewPhotos.removeAt(idx) }
                            } else null,
                            modifier = Modifier.weight(1f),
                            testTag = "review_photo_tile_$idx"
                        )
                    }
                }

                SuqiSwitchRow(
                    title = p5.postAnonymouslyToggle,
                    checked = isAnonymous,
                    onCheckedChange = { isAnonymous = it },
                    modifier = Modifier.testTag("review_anonymous_switch")
                )

                SuqiButton(
                    text = strings.buyer.submitReviewLabel,
                    onClick = {
                        if (comment.isBlank()) {
                            viewModel.showToast(strings.buyer.writeReviewTitle)
                            return@SuqiButton
                        }
                        coroutineScope.launch {
                            val res = viewModel.repository.submitReview(
                                mapOf(
                                    "targetId" to targetId,
                                    "targetType" to targetType,
                                    "rating" to rating,
                                    "comment" to comment.trim(),
                                    "tags" to selectedTags.toList(),
                                    "photos" to reviewPhotos.toList(),
                                    "isAnonymous" to isAnonymous
                                )
                            )
                            if (res.success) {
                                comment = ""
                                selectedTags.clear()
                                reviewPhotos.clear()
                                isAnonymous = false
                                showWriteSheet = false
                                loadAll()
                                viewModel.showToast(strings.buyer.submitReviewLabel)
                            } else {
                                viewModel.showToast(viewModel.localizeMessage(res.message ?: strings.retryLabel))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "review_submit_btn"
                )
            }
        }
    }

    replyTarget?.let { rev ->
        SuqiBottomSheet(onDismissRequest = { replyTarget = null }, title = p5.writeReplyBtn) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("\"${rev.comment}\"", fontSize = 13.sp, color = colors.secondaryText)
                SuqiTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    label = p5.sellerRepliedLabel,
                    singleLine = false,
                    testTag = "review_reply_input"
                )
                SuqiButton(
                    text = strings.saveLabel,
                    enabled = replyText.isNotBlank(),
                    onClick = {
                        coroutineScope.launch {
                            val res = viewModel.repository.replyToReview(rev.id, replyText.trim())
                            if (res.success && res.data != null) {
                                reviews = reviews.map { if (it.id == rev.id) res.data else it }
                                replyTarget = null
                                viewModel.showToast(strings.saveLabel)
                            } else {
                                viewModel.showToast(viewModel.localizeMessage(res.message ?: strings.retryLabel))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "review_reply_submit_btn"
                )
            }
        }
    }

    reportTarget?.let { rev ->
        SuqiBottomSheet(onDismissRequest = { reportTarget = null }, title = strings.buyer.reportReviewLabel) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("\"${rev.comment}\"", fontSize = 13.sp, color = colors.secondaryText)
                SuqiTextField(
                    value = reportReason,
                    onValueChange = { reportReason = it },
                    label = strings.checkout.rejectionReasonTitle,
                    testTag = "review_report_reason_input"
                )
                SuqiButton(
                    text = strings.buyer.reportReviewLabel,
                    onClick = {
                        coroutineScope.launch {
                            viewModel.repository.reportOrModerateReview(
                                mapOf(
                                    "action" to "REPORT",
                                    "reviewId" to rev.id,
                                    "authorName" to rev.userName,
                                    "reason" to reportReason.trim().ifBlank { "Reported from Reviews" },
                                    "reviewText" to rev.comment,
                                    "productName" to targetName,
                                    "rating" to rev.rating.toString()
                                )
                            )
                            reportTarget = null
                            viewModel.showToast(strings.buyer.reportReviewLabel)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "review_report_confirm_btn"
                )
            }
        }
    }

    viewerImageUrl?.let { url ->
        SuqiDocumentViewer(
            documents = listOf(SuqiDocumentPage(strings.customerReviews, url)),
            onDismiss = { viewerImageUrl = null }
        )
    }
}

@Composable
fun AdminModerationV2Screen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val p5 = strings.phase5
    val isOffline by viewModel.isOffline.collectAsState()

    var allFlagged by remember { mutableStateOf(viewModel.cachedAdminFlaggedReviews) }
    var isLoading by remember { mutableStateOf(allFlagged.isEmpty()) }
    var selectedTab by rememberSaveable { mutableStateOf("REPORTED") }
    var bulkMode by rememberSaveable { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<String>() }
    var modNote by remember { mutableStateOf("") }

    fun loadFlagged() {
        coroutineScope.launch {
            isLoading = allFlagged.isEmpty()
            val res = viewModel.repository.getFlaggedReviews()
            if (res.success && res.data != null) {
                allFlagged = res.data
                viewModel.cachedAdminFlaggedReviews = res.data
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadFlagged()
    }

    val filtered = remember(allFlagged, selectedTab) {
        allFlagged.filter { item ->
            when (selectedTab) {
                "HIDDEN" -> item.status.equals("HIDDEN", true) || item.status.equals("DELETED", true)
                "RESTORED" -> item.status.equals("RESTORED", true) || item.status.equals("DISMISSED", true)
                else -> item.status.equals("REPORTED", true)
            }
        }
    }

    SuqiScreen(
        title = strings.admin.moderationTitle,
        subtitle = "${filtered.size}",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        bottomBar = {
            if (bulkMode && selectedIds.isNotEmpty()) {
                Surface(
                    color = colors.cardBackground,
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.repository.bulkModerateReviews(selectedIds.toList(), "HIDE", modNote.trim())
                                    selectedIds.clear()
                                    bulkMode = false
                                    loadFlagged()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SuqiError),
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("mod_bulk_hide")
                        ) {
                            Text(p5.hideActionBtn, color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.repository.bulkModerateReviews(selectedIds.toList(), "RESTORE", modNote.trim())
                                    selectedIds.clear()
                                    bulkMode = false
                                    loadFlagged()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SuqiSuccess),
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("mod_bulk_restore")
                        ) {
                            Text(strings.adminGov.restoreActionBtn, color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.repository.bulkModerateReviews(selectedIds.toList(), "DISMISS", modNote.trim())
                                    selectedIds.clear()
                                    bulkMode = false
                                    loadFlagged()
                                }
                            },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("mod_bulk_dismiss")
                        ) {
                            Text(strings.adminGov.dismissActionBtn, color = colors.primaryText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val modTabs = listOf(
                SuqiTabItem("REPORTED", p5.modReportedTab),
                SuqiTabItem("HIDDEN", p5.modHiddenTab),
                SuqiTabItem("RESTORED", p5.modRestoredTab)
            )
            val modIdx = modTabs.indexOfFirst { it.key == selectedTab }.coerceAtLeast(0)
            SuqiTabs(
                tabs = modTabs,
                selectedIndex = modIdx,
                onSelectTab = { idx ->
                    selectedTab = modTabs[idx].key
                    selectedIds.clear()
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                testTagPrefix = "mod_tab"
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        bulkMode = !bulkMode
                        if (!bulkMode) selectedIds.clear()
                    },
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.heightIn(min = 48.dp).testTag("mod_bulk_toggle")
                ) {
                    Icon(LucideIcons.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (bulkMode) String.format(strings.sellerWork.selectedCountTemplate, selectedIds.size) else p5.selectMultipleBtn,
                        fontSize = 12.sp,
                        color = colors.primaryText
                    )
                }
            }

            if (isLoading && allFlagged.isEmpty()) {
                SuqiSkeletonList(count = 3, modifier = Modifier.padding(top = 12.dp))
            } else if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    SuqiEmptyState(
                        icon = LucideIcons.ShieldCheck,
                        title = strings.admin.moderationTitle,
                        subtitle = strings.admin.clearBadge,
                        testTag = "mod_empty_state"
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SuqiTextField(
                            value = modNote,
                            onValueChange = { modNote = it },
                            label = p5.modNoteHint,
                            testTag = "mod_note_input"
                        )
                    }
                    items(filtered, key = { it.id }) { item ->
                        val isChecked = item.id in selectedIds
                        SuqiSectionCard(
                            padding = 14.dp,
                            modifier = Modifier.testTag("mod_card_${item.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (bulkMode) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { chk ->
                                            if (chk) selectedIds.add(item.id) else selectedIds.remove(item.id)
                                        },
                                        modifier = Modifier.testTag("mod_select_${item.id}")
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.authorName,
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = colors.primaryText
                                    )
                                    if (item.productName.isNotBlank() || item.rating > 0.0) {
                                        Text(
                                            text = listOfNotNull(
                                                item.productName.takeIf { it.isNotBlank() },
                                                item.rating.takeIf { it > 0.0 }?.let { "★ ${String.format("%.1f", it)}" }
                                            ).joinToString(" • "),
                                            fontFamily = SuqiFontFamily,
                                            fontSize = 11.sp,
                                            color = colors.secondaryText
                                        )
                                    }
                                }
                                SuqiStatusBadge(
                                    statusText = item.status,
                                    type = when (item.status.uppercase()) {
                                        "HIDDEN", "DELETED" -> StatusType.ERROR
                                        "RESTORED", "DISMISSED" -> StatusType.SUCCESS
                                        else -> StatusType.WARNING
                                    }
                                )
                            }

                            Spacer(Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.errorBg)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(item.reason, fontSize = 12.sp, color = SuqiError, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.secondaryBackground)
                                    .padding(10.dp)
                            ) {
                                Text("\"${item.reviewText}\"", fontSize = 13.sp, color = colors.primaryText)
                            }
                            if (item.moderatorNote.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = item.moderatorNote,
                                    fontFamily = SuqiFontFamily,
                                    fontSize = 11.sp,
                                    color = colors.mutedText
                                )
                            }

                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.repository.reportOrModerateReview(
                                                mapOf("id" to item.id, "action" to "DISMISS", "note" to modNote.trim())
                                            )
                                            loadFlagged()
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("mod_dismiss_${item.id}")
                                ) {
                                    Text(strings.adminGov.dismissActionBtn, color = colors.primaryText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                if (!item.status.equals("HIDDEN", true)) {
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                viewModel.repository.reportOrModerateReview(
                                                    mapOf("id" to item.id, "action" to "HIDE", "note" to modNote.trim())
                                                )
                                                loadFlagged()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SuqiError),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("mod_hide_${item.id}")
                                    ) {
                                        Text(p5.hideActionBtn, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                viewModel.repository.reportOrModerateReview(
                                                    mapOf("id" to item.id, "action" to "RESTORE", "note" to modNote.trim())
                                                )
                                                loadFlagged()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SuqiSuccess),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("mod_restore_${item.id}")
                                    ) {
                                        Text(strings.adminGov.restoreActionBtn, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
