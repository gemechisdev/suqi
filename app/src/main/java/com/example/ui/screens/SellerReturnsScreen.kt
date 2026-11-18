package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ReturnRequestDto
import com.example.ui.SuqiViewModel
import com.example.ui.components.*
import com.example.ui.i18n.suqiStrings
import com.example.ui.theme.*
import com.example.util.EthiopianMarketplaceUtils
import kotlinx.coroutines.launch

@Composable
fun SellerReturnsScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val p5 = strings.phase5
    val isOffline by viewModel.isOffline.collectAsState()

    var returnsList by remember { mutableStateOf(viewModel.cachedSellerReturns) }
    var isLoading by remember { mutableStateOf(returnsList.isEmpty()) }
    var selectedTab by rememberSaveable { mutableStateOf("REQUESTED") }
    var selectedReturn by remember { mutableStateOf<ReturnRequestDto?>(null) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectNote by remember { mutableStateOf("") }
    var viewerIdx by remember { mutableStateOf<Int?>(null) }

    BackHandler(enabled = selectedReturn != null || showRejectDialog) {
        when {
            showRejectDialog -> showRejectDialog = false
            selectedReturn != null -> selectedReturn = null
        }
    }

    fun loadReturns() {
        coroutineScope.launch {
            isLoading = returnsList.isEmpty()
            val res = viewModel.repository.getSellerReturns()
            if (res.success && res.data != null) {
                returnsList = res.data
                viewModel.cachedSellerReturns = res.data
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadReturns()
    }

    val requested = remember(returnsList) { returnsList.filter { it.status == "REQUESTED" } }
    val approved = remember(returnsList) { returnsList.filter { it.status in listOf("APPROVED", "REPLACEMENT_OFFERED", "SENT_BACK") } }
    val completed = remember(returnsList) { returnsList.filter { it.status in listOf("COMPLETED", "REJECTED") } }
    val displayed = when (selectedTab) {
        "APPROVED" -> approved
        "COMPLETED" -> completed
        else -> requested
    }

    SuqiScreen(
        title = strings.returnsTitle,
        subtitle = "${returnsList.size}",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        onRetryOffline = {
            viewModel.setOfflineMode(false)
            loadReturns()
        },
        modifier = modifier
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            SuqiTabs(
                tabs = listOf(
                    SuqiTabItem("REQUESTED", p5.sellerReturnsRequestedTab, requested.size),
                    SuqiTabItem("APPROVED", p5.sellerReturnsApprovedTab, approved.size),
                    SuqiTabItem("COMPLETED", p5.sellerReturnsCompletedTab, completed.size)
                ),
                selectedKey = selectedTab,
                onSelectTab = { selectedTab = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            when {
                isLoading && returnsList.isEmpty() -> {
                    SuqiSkeletonList(count = 3, modifier = Modifier.padding(top = 12.dp))
                }
                displayed.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        SuqiEmptyState(
                            icon = LucideIcons.RotateCcw,
                            title = p5.noReturnsEmptyTitle,
                            subtitle = p5.noReturnsEmptySub,
                            testTag = "seller_returns_empty"
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(displayed, key = { it.id }) { req ->
                            val age = EthiopianMarketplaceUtils.formatWaitingAgeLabel(req.createdAt, strings)
                            SuqiSectionCard(
                                padding = 16.dp,
                                onClick = { selectedReturn = req },
                                modifier = Modifier.testTag("seller_return_card_${req.id}")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("#${req.orderReference}", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                                    SuqiStatusBadge(
                                        statusText = req.status.replace("_", " "),
                                        type = when (req.status) {
                                            "COMPLETED" -> StatusType.SUCCESS
                                            "REJECTED" -> StatusType.ERROR
                                            else -> StatusType.WARNING
                                        }
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "${req.customerName} • ${req.reason}",
                                    fontFamily = SuqiFontFamily,
                                    fontSize = 13.sp,
                                    color = colors.secondaryText
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(formatEtb(req.refundAmount), fontFamily = SuqiFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = SuqiPrimary)
                                    Text(
                                        text = buildString {
                                            if (!age.isNullOrBlank()) append(age).append(" • ")
                                            append("${req.photos.size} 📷")
                                        },
                                        fontSize = 11.sp,
                                        color = colors.mutedText
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedReturn?.let { req ->
        fun decide(action: String, note: String = "") {
            coroutineScope.launch {
                val res = viewModel.repository.decideReturnRequest(req.id, action, note, req.refundMethod)
                if (res.success && res.data != null) {
                    selectedReturn = null
                    showRejectDialog = false
                    rejectNote = ""
                    loadReturns()
                    viewModel.showToast(res.data.status)
                } else {
                    viewModel.showToast(viewModel.localizeMessage(res.message ?: strings.retryLabel))
                }
            }
        }

        SuqiBottomSheet(
            onDismissRequest = { selectedReturn = null },
            title = "#${req.orderReference}"
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(req.customerName, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                    SuqiStatusBadge(req.status.replace("_", " "), StatusType.WARNING)
                }
                Text("${req.reason} (${req.resolution})", fontFamily = SuqiFontFamily, fontSize = 13.sp, color = SuqiPrimary, fontWeight = FontWeight.SemiBold)
                if (req.comment.isNotBlank()) {
                    Text(req.comment, fontSize = 13.sp, color = colors.secondaryText)
                }
                req.items.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        AsyncImage(
                            model = item.productImageUrl,
                            contentDescription = item.productName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("${item.quantity} × ${item.productName}", fontSize = 13.sp, color = colors.primaryText, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(formatEtb(item.unitPrice * item.quantity), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.primaryText)
                    }
                }
                if (req.photos.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        req.photos.forEachIndexed { idx, uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = "${p5.addEvidencePhotosLabel} ${idx + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewerIdx = idx }
                            )
                        }
                    }
                }
                when (req.status) {
                    "REQUESTED" -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            SuqiButton(
                                text = strings.admin.approveBtn,
                                isPill = true,
                                onClick = { decide("APPROVE") },
                                testTag = "seller_return_approve_btn",
                                modifier = Modifier.weight(1f)
                            )
                            SuqiButton(
                                text = p5.offerReplacementBtn,
                                isPrimary = false,
                                isPill = true,
                                onClick = { decide("OFFER_REPLACEMENT") },
                                testTag = "seller_return_replace_btn",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedButton(
                            onClick = { showRejectDialog = true },
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, SuqiError.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("seller_return_reject_btn")
                        ) {
                            Text(strings.admin.rejectBtn, color = SuqiError, fontWeight = FontWeight.Bold)
                        }
                    }
                    "APPROVED", "REPLACEMENT_OFFERED", "SENT_BACK" -> {
                        SuqiButton(
                            text = p5.completeRefundBtn,
                            isPill = true,
                            onClick = { decide("COMPLETE_REFUND") },
                            testTag = "seller_return_complete_btn",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    if (showRejectDialog && selectedReturn != null) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.padding(horizontal = 24.dp).widthIn(max = 420.dp),
            title = { Text(strings.admin.rejectBtn, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold) },
            text = {
                SuqiTextField(
                    value = rejectNote,
                    onValueChange = { rejectNote = it },
                    label = p5.sellerNoteRequiredHint,
                    singleLine = false,
                    testTag = "seller_return_reject_note"
                )
            },
            confirmButton = {
                TextButton(
                    enabled = rejectNote.isNotBlank(),
                    onClick = {
                        val req = selectedReturn ?: return@TextButton
                        coroutineScope.launch {
                            val res = viewModel.repository.decideReturnRequest(req.id, "REJECT", rejectNote.trim(), req.refundMethod)
                            if (res.success) {
                                showRejectDialog = false
                                selectedReturn = null
                                rejectNote = ""
                                loadReturns()
                            } else {
                                viewModel.showToast(viewModel.localizeMessage(res.message ?: strings.retryLabel))
                            }
                        }
                    },
                    modifier = Modifier.testTag("seller_return_reject_confirm")
                ) {
                    Text(strings.confirmLabel, color = SuqiError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) { Text(strings.cancelLabel) }
            },
            containerColor = colors.cardBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (viewerIdx != null && selectedReturn != null) {
        SuqiDocumentViewer(
            documents = selectedReturn!!.photos.mapIndexed { i, url -> SuqiDocumentPage("${i + 1}", url, selectedReturn!!.reason) },
            initialIndex = viewerIdx ?: 0,
            onDismiss = { viewerIdx = null }
        )
    }
}
