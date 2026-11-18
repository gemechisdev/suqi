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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import com.example.data.model.AdminDisputeCaseDto
import com.example.data.model.AdminReportRowDto
import com.example.data.model.FlaggedReviewDto
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.StatusType
import com.example.ui.components.SuqiBottomSheet
import com.example.ui.components.SuqiButton
import com.example.ui.components.SuqiEmptyState
import com.example.ui.components.SuqiFilterChips
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
// A-07 (P2): Listings moderation
// ==========================================
@Composable
fun AdminListingsModerationScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val p6 = strings.admin6
    val isOffline by viewModel.isOffline.collectAsState()

    var flagged by remember { mutableStateOf(viewModel.cachedAdminFlaggedReviews) }
    var isLoading by remember { mutableStateOf(flagged.isEmpty()) }
    var selectedTab by rememberSaveable { mutableStateOf("REPORTED") }

    fun loadFlagged() {
        coroutineScope.launch {
            isLoading = flagged.isEmpty()
            val res = viewModel.repository.getFlaggedReviews()
            if (res.success && res.data != null) {
                flagged = res.data
                viewModel.cachedAdminFlaggedReviews = res.data
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadFlagged() }

    val filtered = remember(flagged, selectedTab) {
        flagged.filter { item ->
            when (selectedTab) {
                "HIDDEN" -> item.status.equals("HIDDEN", true) || item.status.equals("DELETED", true)
                "RESTORED" -> item.status.equals("RESTORED", true) || item.status.equals("DISMISSED", true)
                else -> item.status.equals("REPORTED", true)
            }
        }
    }

    SuqiScreen(
        title = strings.admin.listingsModerationTitle,
        subtitle = "${filtered.size}",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SuqiFilterChips(
                items = listOf("REPORTED" to strings.adminGov.queueFlaggedReviews, "HIDDEN" to "Hidden", "RESTORED" to "Restored"),
                selectedKey = selectedTab,
                onSelect = { selectedTab = it },
                testTagPrefix = "moderation_tab"
            )
            Spacer(Modifier.height(6.dp))
            if (isLoading && filtered.isEmpty()) {
                com.example.ui.components.SuqiSkeletonList(count = 3, modifier = Modifier.fillMaxWidth())
            } else if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    com.example.ui.components.SuqiEmptyState(
                        icon = LucideIcons.ShieldCheck,
                        title = strings.admin.listingsModerationTitle,
                        subtitle = p6.noListingsSub,
                        testTag = "moderation_empty"
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtered, key = { it.id }) { item ->
                        SuqiSectionCard(padding = 16.dp) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = item.authorName,
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = colors.primaryText
                                    )
                                    Text(
                                        text = p6.reportedReasonLabel + ": ${item.reason}",
                                        fontSize = 12.sp,
                                        color = colors.secondaryText,
                                        maxLines = 2
                                    )
                                }
                                SuqiStatusBadge(item.status, StatusType.ERROR)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.secondaryBackground)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "\"${item.reviewText}\"",
                                    fontSize = 13.sp,
                                    color = colors.primaryText,
                                    maxLines = 3
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.repository.reportOrModerateReview(
                                                mapOf("id" to item.id, "action" to "DISMISS")
                                            )
                                            loadFlagged()
                                            viewModel.showToast(strings.adminGov.dismissActionBtn)
                                        }
                                    },
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("moderation_keep_${item.id}"),
                                    colors = ButtonDefaults.buttonColors(containerColor = SuqiSuccess)
                                ) { Text(p6.keepListingBtn, color = Color.White) }
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.repository.reportOrModerateReview(
                                                mapOf("id" to item.id, "action" to "REQUEST_EDIT")
                                            )
                                            viewModel.showToast(strings.admin.requestChangesBtn)
                                            loadFlagged()
                                        }
                                    },
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("moderation_edit_${item.id}"),
                                    colors = ButtonDefaults.buttonColors(containerColor = SuqiWarning)
                                ) { Text(strings.admin.requestChangesBtn, color = Color.White) }
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.repository.reportOrModerateReview(
                                                mapOf("id" to item.id, "action" to "HIDE")
                                            )
                                            loadFlagged()
                                            viewModel.showToast(strings.adminGov.dismissActionBtn)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SuqiError),
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("moderation_takedown_${item.id}")
                                ) { Text(strings.adminGov.takeDownBtn, color = Color.White) }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// A-08 (P2): Orders & disputes
// ==========================================
@Composable
fun AdminOrdersDisputesScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val p6 = strings.admin6
    val isOffline by viewModel.isOffline.collectAsState()

    var disputes by remember { mutableStateOf(viewModel.cachedAdminDisputes) }
    var isLoading by remember { mutableStateOf(viewModel.cachedAdminDisputes.isEmpty()) }
    var selectedTab by rememberSaveable { mutableStateOf("OPEN") }

    fun loadDisputes() {
        coroutineScope.launch {
            val res = viewModel.repository.getAdminDisputes()
            if (res.success && res.data != null) {
                disputes = res.data
                viewModel.cachedAdminDisputes = res.data
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadDisputes() }

    val openDisputes = remember(disputes) {
        disputes.filter { it.status.equals("OPEN", true) }
    }
    val resolvedDisputes = remember(disputes) {
        disputes.filter { !it.status.equals("OPEN", true) }
    }
    val showing = if (selectedTab == "OPEN") openDisputes else resolvedDisputes

    SuqiScreen(
        title = strings.admin.ordersDisputesTitle,
        subtitle = "${openDisputes.size} open",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SuqiFilterChips(
                items = listOf("OPEN" to p6.tabDisputes, "RESOLVED" to "Resolved"),
                selectedKey = selectedTab,
                onSelect = { selectedTab = it },
                testTagPrefix = "disputes_tab"
            )
            Spacer(Modifier.height(6.dp))
            if (isLoading && showing.isEmpty()) {
                com.example.ui.components.SuqiSkeletonList(count = 3, modifier = Modifier.fillMaxWidth())
            } else if (showing.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    com.example.ui.components.SuqiEmptyState(
                        icon = LucideIcons.Truck,
                        title = p6.tabDisputes,
                        subtitle = p6.noDisputesSub,
                        testTag = "disputes_empty"
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(showing, key = { it.orderId }) { item ->
                        SuqiSectionCard(padding = 16.dp) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = item.orderReference,
                                            fontFamily = SuqiFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = colors.primaryText
                                        )
                                        Text(
                                            text = "${item.buyerName} • ${item.shopName}",
                                            fontSize = 12.sp,
                                            color = colors.secondaryText
                                        )
                                    }
                                    SuqiStatusBadge(item.status, StatusType.WARNING)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                SuqiSectionCard(
                                    padding = 12.dp,
                                    containerColor = colors.secondaryBackground,
                                    modifier = Modifier.testTag("dispute_card_${item.orderId}")
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = p6.buyerClaimLabel,
                                            fontSize = 11.sp,
                                            color = colors.mutedText,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = item.buyerClaim,
                                            fontSize = 13.sp,
                                            color = colors.primaryText,
                                            maxLines = 3
                                        )
                                        Text(
                                            text = p6.sellerResponseLabel,
                                            fontSize = 11.sp,
                                            color = colors.mutedText,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = item.sellerResponse.ifBlank { "—" },
                                            fontSize = 13.sp,
                                            color = colors.primaryText,
                                            maxLines = 3
                                        )
                                        if (item.evidence.isNotEmpty()) {
                                            Text(
                                                text = p6.evidenceLabel + " (${item.evidence.size})",
                                                fontSize = 11.sp,
                                                color = colors.mutedText,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Text(
                                            text = "Amount: ${formatEtb(item.amount)}",
                                            fontSize = 13.sp,
                                            color = colors.primaryText,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SuqiButton(
                                        text = p6.refundBuyerBtn,
                                        onClick = {
                                            SuqiBottomSheet(
                                                onDismissRequest = {},
                                                title = strings.adminGov.queueDisputes
                                            ) {
                                                com.example.ui.components.SuqiTextField(
                                                    value = "",
                                                    onValueChange = {},
                                                    label = p6.disputeNoteHint,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        },
                                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("dispute_refund_${item.orderId}"),
                                        isPrimary = true
                                    )
                                    SuqiButton(
                                        text = p6.sideWithSellerBtn,
                                        onClick = {
                                            SuqiBottomSheet(
                                                onDismissRequest = {},
                                                title = strings.adminGov.queueDisputes
                                            ) {
                                                com.example.ui.components.SuqiTextField(
                                                    value = "",
                                                    onValueChange = {},
                                                    label = p6.disputeNoteHint,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        },
                                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("dispute_side_${item.orderId}"),
                                        isPrimary = false
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SuqiButton(
                                        text = p6.partialRefundBtn,
                                        onClick = {},
                                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("dispute_partial_${item.orderId}"),
                                        isPrimary = false
                                    )
                                    SuqiButton(
                                        text = p6.escalateBtn,
                                        onClick = {},
                                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("dispute_escalate_${item.orderId}"),
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

// ==========================================
// A-21 (P3): Reports & exports
// ==========================================
@Composable
fun AdminReportsExportsScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val p6 = strings.admin6
    val isOffline by viewModel.isOffline.collectAsState()

    var report by remember { mutableStateOf<AdminReportRowDto?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedReport by rememberSaveable { mutableStateOf("SALES") }

    fun runReport(kind: String) {
        coroutineScope.launch {
            isLoading = true
            val res = viewModel.repository.getAdminReport(kind)
            if (res.success && res.data != null) {
                report = res.data
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { runReport(selectedReport) }

    val reports = listOf(
        "SALES" to p6.reportSales,
        "USERS" to p6.reportUsers,
        "SHOPS" to p6.reportShops,
        "ORDERS" to p6.reportOrders,
        "FINANCE" to p6.reportFinance,
        "MODERATION" to p6.reportModeration
    )

    SuqiScreen(
        title = strings.admin.reportsExportsTitle,
        subtitle = "Export CSV / PDF",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SuqiFilterChips(
                items = reports,
                selectedKey = selectedReport,
                onSelect = { key ->
                    selectedReport = key
                    runReport(key)
                },
                testTagPrefix = "report_kind"
            )
            Spacer(Modifier.height(12.dp))
            if (isLoading && report == null) {
                com.example.ui.components.SuqiSkeletonList(count = 3, modifier = Modifier.fillMaxWidth())
            } else {
                val rows = report?.rows.orEmpty()
                if (rows.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        com.example.ui.components.SuqiEmptyState(
                            icon = LucideIcons.FileText,
                            title = strings.admin.reportsExportsTitle,
                            subtitle = "No data",
                            testTag = "reports_empty"
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            SuqiSectionCard(padding = 16.dp) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = report?.label ?: selectedReport,
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = colors.primaryText
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        SuqiButton(
                                            text = p6.exportCsvBtn,
                                            onClick = { viewModel.showToast("CSV export not yet wired") },
                                            modifier = Modifier.heightIn(min = 40.dp),
                                            isPrimary = false
                                        )
                                        SuqiButton(
                                            text = p6.exportPdfBtn,
                                            onClick = { viewModel.showToast("PDF export not yet wired") },
                                            modifier = Modifier.heightIn(min = 40.dp),
                                            isPrimary = false
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    report?.columns.forEach { col ->
                                        Text(
                                            text = col,
                                            fontFamily = SuqiFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = colors.mutedText,
                                            modifier = Modifier.widthIn(min = 120.dp)
                                        )
                                    }
                                }
                            }
                        }
                        items(rows, key = { it.joinToString("|") }) { row ->
                            SuqiSectionCard(padding = 12.dp, modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    row.forEachIndexed { idx, cell ->
                                        Text(
                                            text = cell,
                                            fontFamily = SuqiFontFamily,
                                            fontSize = 12.sp,
                                            color = colors.primaryText,
                                            modifier = Modifier.weight(1f).widthIn(min = 100.dp)
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
