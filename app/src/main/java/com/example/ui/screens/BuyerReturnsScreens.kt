package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.data.api.BackendConfig
import com.example.data.model.OrderDto
import com.example.data.model.ReturnRequestDto
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.*
import com.example.ui.i18n.suqiStrings
import com.example.ui.theme.*
import com.example.util.EthiopianMarketplaceUtils
import kotlinx.coroutines.launch

@Composable
fun ReturnRequestScreen(
    orderId: String,
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val p5 = strings.phase5
    val isOffline by viewModel.isOffline.collectAsState()

    var order by remember { mutableStateOf<OrderDto?>(null) }
    var existingReturn by remember { mutableStateOf<ReturnRequestDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    var currentStep by remember { mutableIntStateOf(0) }
    val selectedProductIds = remember { mutableStateListOf<String>() }
    val reasons = listOf(
        p5.reasonWrongItem,
        p5.reasonDamaged,
        p5.reasonNotAsDescribed,
        p5.reasonChangedMind
    )
    var selectedReason by remember { mutableStateOf(reasons.first()) }
    var comment by remember { mutableStateOf("") }
    val photos = remember { mutableStateListOf<String>() }
    var resolution by remember { mutableStateOf("REFUND") }
    var isSubmitting by remember { mutableStateOf(false) }

    BackHandler(enabled = currentStep > 0) {
        currentStep -= 1
    }

    fun loadData() {
        coroutineScope.launch {
            isLoading = true
            errorMsg = null
            val res = viewModel.repository.getOrderDetails(orderId)
            if (res.success && res.data != null) {
                order = res.data
                if (selectedProductIds.isEmpty()) {
                    res.data.items.forEach { selectedProductIds.add(it.productId) }
                }
                val retRes = viewModel.repository.getBuyerReturns()
                if (retRes.success && retRes.data != null) {
                    viewModel.cachedBuyerReturns = retRes.data
                    existingReturn = retRes.data.firstOrNull {
                        it.orderId == orderId && it.status != "REJECTED"
                    }
                }
            } else {
                errorMsg = viewModel.localizeMessage(res.message ?: strings.retryLabel)
            }
            isLoading = false
        }
    }

    LaunchedEffect(orderId) {
        loadData()
    }

    val currentOrder = order
    val ageHours = currentOrder?.let { EthiopianMarketplaceUtils.computeElapsedHours(it.createdAt) }
    val isEligible = currentOrder != null &&
        currentOrder.orderStatus.equals("Delivered", ignoreCase = true) &&
        (ageHours == null || ageHours <= 7.0 * 24.0)

    val selectedRefundAmount = currentOrder?.items
        ?.filter { it.productId in selectedProductIds }
        ?.sumOf { it.unitPrice * it.quantity } ?: 0.0

    SuqiScreen(
        title = strings.returnsTitle,
        subtitle = currentOrder?.let { "#${it.orderReference}" },
        onBackClick = {
            if (currentStep > 0) currentStep -= 1 else viewModel.goBack()
        },
        isOffline = isOffline,
        onRetryOffline = {
            viewModel.setOfflineMode(false)
            loadData()
        },
        bottomBar = {
            if (currentOrder != null && isEligible && existingReturn == null) {
                Surface(
                    color = colors.cardBackground,
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        if (currentStep > 0) {
                            OutlinedButton(
                                onClick = { currentStep -= 1 },
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier.weight(0.9f).height(48.dp)
                            ) {
                                Text(strings.common.backLabel, color = colors.primaryText, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        if (currentStep < 2) {
                            SuqiButton(
                                text = strings.buyer.nextLabel,
                                isPill = true,
                                enabled = selectedProductIds.isNotEmpty(),
                                onClick = { currentStep += 1 },
                                testTag = "return_next_step_btn",
                                modifier = Modifier.weight(1.4f)
                            )
                        } else {
                            SuqiButton(
                                text = if (isSubmitting) strings.common.processingLabel else p5.submitReturnBtn,
                                isPill = true,
                                enabled = !isSubmitting && selectedProductIds.isNotEmpty(),
                                onClick = {
                                    isSubmitting = true
                                    coroutineScope.launch {
                                        val res = viewModel.repository.submitReturnRequest(
                                            mapOf(
                                                "orderId" to currentOrder.id,
                                                "itemProductIds" to selectedProductIds.toList(),
                                                "reason" to selectedReason,
                                                "comment" to comment.trim(),
                                                "photos" to photos.toList(),
                                                "resolution" to resolution,
                                                "refundMethod" to currentOrder.paymentMethodTitle
                                            )
                                        )
                                        isSubmitting = false
                                        if (res.success && res.data != null) {
                                            viewModel.cachedBuyerReturns = listOf(res.data) + viewModel.cachedBuyerReturns
                                            viewModel.goBack()
                                            viewModel.navigate(ScreenDestination.ReturnStatus(res.data.id))
                                        } else {
                                            viewModel.showToast(viewModel.localizeMessage(res.message ?: strings.retryLabel))
                                        }
                                    }
                                },
                                testTag = "return_submit_btn",
                                modifier = Modifier.weight(1.4f)
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        when {
            isLoading -> {
                SuqiSkeletonList(count = 3, modifier = Modifier.padding(paddingValues).padding(top = 12.dp))
            }
            errorMsg != null || currentOrder == null -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    SuqiErrorCard(
                        title = strings.returnsTitle,
                        message = errorMsg ?: strings.retryLabel,
                        onRetry = { loadData() }
                    )
                }
            }
            existingReturn != null -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    SuqiEmptyState(
                        icon = LucideIcons.RotateCcw,
                        title = p5.returnStatusTitle,
                        subtitle = "#${currentOrder.orderReference} • ${existingReturn!!.status}",
                        ctaText = p5.viewReturnStatusBtn,
                        onCtaClick = {
                            viewModel.goBack()
                            viewModel.navigate(ScreenDestination.ReturnStatus(existingReturn!!.id))
                        },
                        testTag = "return_already_exists_state"
                    )
                }
            }
            !isEligible -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    SuqiEmptyState(
                        icon = LucideIcons.Clock,
                        title = strings.returnsTitle,
                        subtitle = p5.returnWindowExpiredMsg,
                        ctaText = strings.helpSupportTitle,
                        onCtaClick = { viewModel.navigate(ScreenDestination.HelpLegal) },
                        testTag = "return_expired_state"
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        SuqiSectionCard(padding = 14.dp) {
                            SuqiStepIndicator(
                                steps = listOf(p5.selectItemsStep, p5.reasonEvidenceStep, p5.resolutionStep),
                                currentStep = currentStep
                            )
                        }
                    }

                    when (currentStep) {
                        0 -> {
                            item {
                                SuqiSectionCard(padding = 16.dp) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(currentOrder.shopName, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                                        SuqiStatusBadge(p5.returnsWindowBadge, StatusType.SUCCESS)
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    currentOrder.items.forEach { item ->
                                        val checked = item.productId in selectedProductIds
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(if (checked) colors.primaryTint else colors.secondaryBackground)
                                                .border(1.dp, if (checked) SuqiPrimary else colors.border, RoundedCornerShape(14.dp))
                                                .clickable {
                                                    if (checked) selectedProductIds.remove(item.productId)
                                                    else selectedProductIds.add(item.productId)
                                                }
                                                .padding(10.dp)
                                                .testTag("return_item_${item.productId}")
                                        ) {
                                            Checkbox(
                                                checked = checked,
                                                onCheckedChange = {
                                                    if (it) {
                                                        if (!checked) selectedProductIds.add(item.productId)
                                                    } else {
                                                        selectedProductIds.remove(item.productId)
                                                    }
                                                },
                                                colors = CheckboxDefaults.colors(checkedColor = SuqiPrimary)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            AsyncImage(
                                                model = item.productImageUrl,
                                                contentDescription = item.productName,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp))
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(item.productName, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.primaryText, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                                Text("${item.quantity} × ${formatEtb(item.unitPrice)}", fontSize = 12.sp, color = SuqiPrimary, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Spacer(Modifier.height(8.dp))
                                    }
                                }
                            }
                        }

                        1 -> {
                            item {
                                SuqiSectionCard(padding = 16.dp) {
                                    Text(p5.reasonEvidenceStep, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                                    Spacer(Modifier.height(10.dp))
                                    reasons.forEachIndexed { idx, reason ->
                                        val isSel = selectedReason == reason
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(if (isSel) colors.primaryTint else colors.secondaryBackground)
                                                .border(1.dp, if (isSel) SuqiPrimary else colors.border, RoundedCornerShape(14.dp))
                                                .clickable { selectedReason = reason }
                                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                                .testTag("return_reason_$idx")
                                        ) {
                                            RadioButton(
                                                selected = isSel,
                                                onClick = { selectedReason = reason },
                                                colors = RadioButtonDefaults.colors(selectedColor = SuqiPrimary)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(reason, fontFamily = SuqiFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.primaryText)
                                        }
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    SuqiTextField(
                                        value = comment,
                                        onValueChange = { comment = it },
                                        label = p5.returnCommentHint,
                                        singleLine = false,
                                        testTag = "return_comment_input"
                                    )
                                }
                            }
                            item {
                                SuqiSectionCard(padding = 16.dp) {
                                    Text(p5.addEvidencePhotosLabel, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primaryText)
                                    Spacer(Modifier.height(10.dp))
                                    val sampleUrls = listOf(
                                        "https://images.unsplash.com/photo-1610945415295-d9bbf067e59c?w=400",
                                        "https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?w=400"
                                    )
                                    val slots = minOf(4, photos.size + 1)
                                    for (i in 0 until slots) {
                                        val uri = photos.getOrNull(i)
                                        SuqiImagePickerTile(
                                            label = "${strings.common.uploadPhotoLabel} ${i + 1}",
                                            imageUri = uri,
                                            onImagePicked = { picked ->
                                                if (i < photos.size) photos[i] = picked else photos.add(picked)
                                            },
                                            onRemove = if (uri != null) {
                                                { photos.removeAt(i) }
                                            } else null,
                                            demoSampleUrl = if (BackendConfig.DemoMode) sampleUrls[i % sampleUrls.size] else null,
                                            testTag = "return_photo_picker_$i"
                                        )
                                        if (i < slots - 1) Spacer(Modifier.height(10.dp))
                                    }
                                }
                            }
                        }

                        else -> {
                            item {
                                SuqiSectionCard(padding = 16.dp) {
                                    Text(p5.resolutionStep, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                                    Spacer(Modifier.height(10.dp))
                                    SuqiSelectionCard(
                                        selected = resolution == "REFUND",
                                        title = String.format(p5.resolutionRefund, currentOrder.paymentMethodTitle),
                                        subtitle = String.format(p5.refundSummaryTemplate, formatEtb(selectedRefundAmount)),
                                        icon = LucideIcons.Wallet,
                                        onClick = { resolution = "REFUND" }
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    SuqiSelectionCard(
                                        selected = resolution == "REPLACE",
                                        title = p5.resolutionReplace,
                                        subtitle = currentOrder.shopName,
                                        icon = LucideIcons.RotateCcw,
                                        onClick = { resolution = "REPLACE" }
                                    )
                                }
                            }
                            item {
                                SuqiSectionCard(containerColor = colors.primaryTint, padding = 16.dp) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            String.format(p5.refundSummaryTemplate, formatEtb(selectedRefundAmount)),
                                            fontFamily = SuqiFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = SuqiPrimary
                                        )
                                        Text("${selectedProductIds.size} • ${photos.size} 📷", fontSize = 12.sp, color = colors.secondaryText)
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

@Composable
fun ReturnStatusScreen(
    returnId: String,
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val p5 = strings.phase5
    val isOffline by viewModel.isOffline.collectAsState()

    var returnReq by remember { mutableStateOf<ReturnRequestDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var viewerIndex by remember { mutableStateOf<Int?>(null) }

    fun loadDetails() {
        coroutineScope.launch {
            isLoading = returnReq == null
            val res = viewModel.repository.getReturnDetails(returnId)
            if (res.success && res.data != null) {
                returnReq = res.data
            }
            isLoading = false
        }
    }

    LaunchedEffect(returnId) {
        loadDetails()
    }

    val current = returnReq
    val stepIndex = when (current?.status) {
        "REQUESTED" -> 0
        "APPROVED", "REPLACEMENT_OFFERED" -> 1
        "SENT_BACK" -> 2
        "COMPLETED" -> 3
        else -> 0
    }

    SuqiScreen(
        title = p5.returnStatusTitle,
        subtitle = current?.let { "#${it.orderReference}" },
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        bottomBar = {
            if (current != null) {
                Surface(
                    color = colors.cardBackground,
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.navigate(ScreenDestination.ChatThread(current.shopId)) },
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(LucideIcons.MessageCircle, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.checkout.chatShortBtn, color = SuqiPrimary, fontWeight = FontWeight.SemiBold)
                        }
                        if (current.status in listOf("APPROVED", "REPLACEMENT_OFFERED")) {
                            SuqiButton(
                                text = p5.returnStepSentBack,
                                isPill = true,
                                onClick = {
                                    coroutineScope.launch {
                                        val res = viewModel.repository.decideReturnRequest(current.id, "SENT_BACK")
                                        if (res.success && res.data != null) {
                                            returnReq = res.data
                                            viewModel.showToast(p5.returnStepSentBack)
                                        }
                                    }
                                },
                                testTag = "return_mark_sent_back_btn",
                                modifier = Modifier.weight(1.2f)
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        if (isLoading && current == null) {
            SuqiSkeletonList(count = 3, modifier = Modifier.padding(paddingValues).padding(top = 12.dp))
        } else if (current == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                SuqiErrorCard(title = p5.returnStatusTitle, message = strings.retryLabel, onRetry = { loadDetails() })
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    SuqiSectionCard(padding = 16.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(current.shopName, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.primaryText)
                            SuqiStatusBadge(
                                statusText = current.status.replace("_", " "),
                                type = when (current.status) {
                                    "COMPLETED" -> StatusType.SUCCESS
                                    "REJECTED" -> StatusType.ERROR
                                    else -> StatusType.WARNING
                                },
                                modifier = Modifier.testTag("return_status_badge")
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        if (current.status == "REJECTED") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.errorBg)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = current.sellerNote.ifBlank { p5.returnWindowExpiredMsg },
                                    fontFamily = SuqiFontFamily,
                                    fontSize = 13.sp,
                                    color = SuqiError
                                )
                            }
                        } else {
                            SuqiStepIndicator(
                                steps = listOf(p5.returnStepRequested, p5.returnStepApproved, p5.returnStepSentBack, p5.returnStepCompleted),
                                currentStep = stepIndex
                            )
                            if (current.sellerNote.isNotBlank()) {
                                Spacer(Modifier.height(10.dp))
                                Text(current.sellerNote, fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.secondaryText)
                            }
                        }
                    }
                }

                item {
                    SuqiSectionCard(padding = 16.dp) {
                        Text(String.format(p5.refundSummaryTemplate, formatEtb(current.refundAmount)), fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SuqiPrimary)
                        Text("${current.reason} • ${current.resolution}", fontSize = 13.sp, color = colors.secondaryText)
                        if (current.comment.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(current.comment, fontSize = 13.sp, color = colors.primaryText)
                        }
                        Spacer(Modifier.height(10.dp))
                        current.items.forEach { item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                AsyncImage(
                                    model = item.productImageUrl,
                                    contentDescription = item.productName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
                                )
                                Spacer(Modifier.width(10.dp))
                                Text("${item.quantity} × ${item.productName}", fontSize = 13.sp, color = colors.primaryText, modifier = Modifier.weight(1f))
                                Text(formatEtb(item.unitPrice * item.quantity), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.primaryText)
                            }
                        }
                        if (current.photos.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                current.photos.forEachIndexed { idx, uri ->
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = "${p5.addEvidencePhotosLabel} ${idx + 1}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { viewerIndex = idx }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (viewerIndex != null && current != null && current.photos.isNotEmpty()) {
        SuqiDocumentViewer(
            documents = current.photos.mapIndexed { idx, url -> SuqiDocumentPage("${idx + 1}", url, current.reason) },
            initialIndex = viewerIndex ?: 0,
            onDismiss = { viewerIndex = null }
        )
    }
}
