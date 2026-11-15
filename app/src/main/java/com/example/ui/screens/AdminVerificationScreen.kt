package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.data.model.*
import com.example.ui.SuqiViewModel
import com.example.ui.components.*
import com.example.ui.i18n.suqiStrings
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AdminVerificationV2Screen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val isOffline by viewModel.isOffline.collectAsState()

    var allShops by remember { mutableStateOf<List<ShopDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedMainTab by remember { mutableIntStateOf(0) } // 0: Pending, 1: Verified, 2: Rejected
    var pendingSubFilter by remember { mutableStateOf("ALL") } // ALL, NEW, RESUBMITTED, WAITING_INFO

    var selectedShop by remember { mutableStateOf<ShopDto?>(null) }
    val checklistState = remember { mutableStateMapOf<String, Boolean>() }
    var viewerPages by remember { mutableStateOf<List<SuqiDocumentPage>?>(null) }
    var viewerInitialIndex by remember { mutableIntStateOf(0) }

    // Decision dialogs
    var showRequestChangesDialog by remember { mutableStateOf(false) }
    val selectedFailedKeys = remember { mutableStateListOf<String>() }
    var changeNote by remember { mutableStateOf("") }

    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectReason by remember { mutableStateOf("") }

    BackHandler(enabled = selectedShop != null) {
        selectedShop = null
    }

    fun slotLabel(key: String, fallback: String): String = when (key) {
        "idFront" -> strings.seller.docIdFront
        "idBack" -> strings.seller.docIdBack
        "tradeLicense" -> strings.seller.docTradeLicense
        "shopPhoto" -> strings.seller.docShopPhoto
        "selfieWithId" -> strings.seller.docSelfieWithId
        else -> fallback
    }

    fun slotGuide(key: String, fallback: String): String = when (key) {
        "idFront" -> strings.sellerOps.docIdFrontGuide
        "idBack" -> strings.sellerOps.docIdBackGuide
        "tradeLicense" -> strings.sellerOps.docTradeLicenseGuide
        "shopPhoto" -> strings.sellerOps.docShopPhotoGuide
        "selfieWithId" -> strings.sellerOps.docSelfieWithIdGuide
        else -> fallback
    }

    fun checklistLabel(key: String, fallback: String): String = when (key) {
        "id_readable" -> strings.adminOps.checkIdReadable
        "name_matches" -> strings.adminOps.checkNameMatches
        "license_valid" -> strings.adminOps.checkLicenseValid
        "shop_authentic" -> strings.adminOps.checkShopAuthentic
        "address_plausible" -> strings.adminOps.checkAddressPlausible
        else -> fallback
    }

    fun loadShops() {
        coroutineScope.launch {
            isLoading = true
            val res = viewModel.repository.getShops()
            if (res.success && res.data != null) {
                allShops = res.data
                selectedShop?.let { current ->
                    selectedShop = res.data.firstOrNull { it.id == current.id }
                }
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadShops() }

    val pendingAll = remember(allShops) {
        allShops.filter { it.verificationStatus in listOf("PENDING_REVIEW", "RESUBMITTED", "CHANGES_REQUESTED") }
    }
    val verifiedList = remember(allShops) {
        allShops.filter { it.verificationStatus == "VERIFIED" || it.isVerified }
    }
    val rejectedList = remember(allShops) {
        allShops.filter { it.verificationStatus == "REJECTED" }
    }

    val filteredPending = remember(pendingAll, pendingSubFilter) {
        when (pendingSubFilter) {
            "NEW" -> pendingAll.filter { it.verificationStatus == "PENDING_REVIEW" }
            "RESUBMITTED" -> pendingAll.filter { it.verificationStatus == "RESUBMITTED" }
            "WAITING_INFO" -> pendingAll.filter { it.verificationStatus == "CHANGES_REQUESTED" }
            else -> pendingAll
        }
    }

    val activeList = when (selectedMainTab) {
        0 -> filteredPending
        1 -> verifiedList
        else -> rejectedList
    }

    fun openReview(shop: ShopDto) {
        selectedShop = shop
        checklistState.clear()
        VerificationDocsCodec.checklistItems.forEach { (k, _) ->
            checklistState[k] = shop.verificationStatus == "VERIFIED"
        }
    }

    SuqiScreen(
        title = if (selectedShop != null) strings.adminOps.reviewShopTitle else strings.admin.adminVerificationTitle,
        subtitle = "${pendingAll.size} ${strings.admin.queuePending} · ${verifiedList.size} ${strings.admin.queueVerified}",
        onBackClick = {
            if (selectedShop != null) selectedShop = null else viewModel.goBack()
        },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        val reviewing = selectedShop
        if (reviewing != null) {
            val submission = remember(reviewing.verificationDocsJson) {
                VerificationDocsCodec.parse(reviewing.verificationDocsJson)
            }
            val docPages = remember(submission, strings) {
                VerificationDocsCodec.docSlots.mapNotNull { slot ->
                    val uri = submission.docs[slot.key]
                    if (uri.isNullOrBlank()) null
                    else SuqiDocumentPage(slotLabel(slot.key, slot.label), uri, slotGuide(slot.key, slot.guidance))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Applicant & Shop Card
                    SuqiSectionCard(padding = 16.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = reviewing.logoUrl,
                                contentDescription = reviewing.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(colors.imageBackdrop)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(reviewing.name, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = colors.primaryText)
                                Text("${reviewing.category} • ${reviewing.shopType}", fontSize = 12.sp, color = colors.secondaryText)
                                Text("${reviewing.subCity}, ${reviewing.city} · ${reviewing.phone}", fontSize = 12.sp, color = colors.mutedText)
                            }
                            SuqiStatusBadge(
                                statusText = when (reviewing.verificationStatus) {
                                    "VERIFIED" -> strings.admin.queueVerified
                                    "REJECTED" -> strings.admin.queueRejected
                                    "CHANGES_REQUESTED" -> strings.admin.queueWaitingInfo
                                    "RESUBMITTED" -> strings.admin.queueResubmitted
                                    else -> strings.admin.queuePending
                                },
                                type = when (reviewing.verificationStatus) {
                                    "VERIFIED" -> StatusType.SUCCESS
                                    "REJECTED", "CHANGES_REQUESTED" -> StatusType.ERROR
                                    else -> StatusType.WARNING
                                }
                            )
                        }
                        if (submission.licenseRef.isNotBlank() || submission.tin.isNotBlank() || submission.submittedAt.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            HorizontalDivider(color = colors.border)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                if (submission.licenseRef.isNotBlank()) {
                                    Text("${strings.seller.licenseNumberLabel}: ${submission.licenseRef}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = colors.primaryText)
                                }
                                if (submission.tin.isNotBlank()) {
                                    Text("${strings.seller.tinNumberLabel}: ${submission.tin}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = colors.primaryText)
                                }
                            }
                            if (submission.submittedAt.isNotBlank()) {
                                Text(submission.submittedAt, fontSize = 11.sp, color = colors.mutedText)
                            }
                        }
                    }

                    // Document Thumbnails Strip -> Opens SuqiDocumentViewer pager + pinch zoom
                    SuqiSectionCard(padding = 16.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${strings.adminOps.submittedDocsTitle} (${docPages.size}/${VerificationDocsCodec.docSlots.size})",
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = colors.primaryText
                            )
                            if (docPages.isNotEmpty()) {
                                Text(
                                    text = strings.common.viewAllLabel,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = SuqiPrimary,
                                    modifier = Modifier
                                        .clickable {
                                            viewerPages = docPages
                                            viewerInitialIndex = 0
                                        }
                                        .padding(4.dp)
                                        .testTag("admin_verify_open_viewer")
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        if (docPages.isEmpty()) {
                            Text(
                                text = strings.admin.noShopsInQueue,
                                fontSize = 12.sp,
                                color = colors.secondaryText
                            )
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                itemsIndexed(docPages) { index, page ->
                                    Column(
                                        modifier = Modifier
                                            .width(124.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(colors.secondaryBackground)
                                            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                                            .clickable {
                                                viewerPages = docPages
                                                viewerInitialIndex = index
                                            }
                                    ) {
                                        AsyncImage(
                                            model = page.imageUrl,
                                            contentDescription = page.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(84.dp)
                                                .background(colors.imageBackdrop)
                                        )
                                        Text(
                                            text = page.title,
                                            fontFamily = SuqiFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp,
                                            color = colors.primaryText,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 5-Point Verification Checklist
                    SuqiSectionCard(padding = 16.dp) {
                        Text(
                            text = strings.admin.checklistTitle,
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = colors.primaryText
                        )
                        Spacer(Modifier.height(6.dp))
                        VerificationDocsCodec.checklistItems.forEach { (key, label) ->
                            val checked = checklistState[key] == true
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { checklistState[key] = !checked }
                                    .padding(vertical = 4.dp)
                                    .testTag("admin_verify_check_$key")
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { checklistState[key] = it },
                                    colors = CheckboxDefaults.colors(checkedColor = SuqiPrimary)
                                )
                                Text(
                                    text = checklistLabel(key, label),
                                    fontFamily = SuqiFontFamily,
                                    fontSize = 13.sp,
                                    color = colors.primaryText,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Decision History Card
                    if (submission.history.isNotEmpty()) {
                        SuqiSectionCard(padding = 16.dp) {
                            Text(
                                text = strings.adminOps.decisionNotesTitle,
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = colors.primaryText
                            )
                            Spacer(Modifier.height(8.dp))
                            submission.history.asReversed().forEachIndexed { idx, entry ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(entry.status.replace("_", " "), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SuqiPrimary)
                                    Text(entry.at, fontSize = 11.sp, color = colors.mutedText)
                                }
                                if (entry.note.isNotBlank()) {
                                    Text(entry.note, fontSize = 12.sp, color = colors.secondaryText)
                                }
                                if (idx < submission.history.lastIndex) {
                                    HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = 6.dp))
                                }
                            }
                        }
                    }
                }

                // Decision Bar (Approve · Request changes · Reject)
                Surface(
                    color = colors.cardBackground,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                rejectReason = ""
                                showRejectDialog = true
                            },
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.5.dp, SuqiError),
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("admin_verify_reject_btn")
                        ) {
                            Text(strings.admin.rejectBtn, color = SuqiError, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 2)
                        }
                        OutlinedButton(
                            onClick = {
                                selectedFailedKeys.clear()
                                changeNote = ""
                                showRequestChangesDialog = true
                            },
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.5.dp, colors.border),
                            modifier = Modifier.weight(1.15f).heightIn(min = 48.dp).testTag("admin_verify_changes_btn")
                        ) {
                            Text(strings.admin.requestChangesBtn, color = colors.primaryText, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 2)
                        }
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.repository.adminVerifySellerDecision(
                                        shopId = reviewing.id,
                                        decision = "APPROVE",
                                        reason = "All verification checks passed"
                                    )
                                    viewModel.showToast(strings.adminOps.verifyActionToast)
                                    loadShops()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary),
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier.weight(1.1f).heightIn(min = 48.dp).testTag("admin_verify_approve_btn")
                        ) {
                            Text(strings.admin.approveBtn, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 2)
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                SuqiTabs(
                    tabs = listOf(
                        SuqiTabItem("pending", strings.admin.queuePending, count = pendingAll.size, showDot = pendingAll.isNotEmpty()),
                        SuqiTabItem("verified", strings.admin.queueVerified, count = verifiedList.size),
                        SuqiTabItem("rejected", strings.admin.queueRejected, count = rejectedList.size)
                    ),
                    selectedIndex = selectedMainTab,
                    onSelectTab = { selectedMainTab = it },
                    style = SuqiTabStyle.Underline
                )

                if (selectedMainTab == 0) {
                    Spacer(Modifier.height(6.dp))
                    SuqiFilterChips(
                        items = listOf(
                            "ALL" to "${strings.allLabel} (${pendingAll.size})",
                            "NEW" to strings.checkout.newBadge,
                            "RESUBMITTED" to strings.admin.queueResubmitted,
                            "WAITING_INFO" to strings.admin.queueWaitingInfo
                        ),
                        selectedKey = pendingSubFilter,
                        onSelect = { pendingSubFilter = it },
                        testTagPrefix = "admin_verify_filter"
                    )
                }

                if (isLoading) {
                    SuqiSkeletonList(count = 3, modifier = Modifier.padding(top = 12.dp))
                } else if (activeList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        SuqiEmptyState(
                            icon = LucideIcons.BadgeCheck,
                            title = strings.admin.noShopsInQueue,
                            subtitle = strings.admin.clearBadge
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(activeList, key = { it.id }) { shop ->
                            val sub = VerificationDocsCodec.parse(shop.verificationDocsJson)
                            SuqiSectionCard(
                                onClick = { openReview(shop) },
                                padding = 16.dp
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(
                                        model = shop.logoUrl,
                                        contentDescription = shop.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(colors.imageBackdrop)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(shop.name, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                                        Text("${shop.category} · ${shop.subCity}, ${shop.city}", fontSize = 12.sp, color = colors.secondaryText)
                                        Spacer(Modifier.height(4.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${sub.docs.size}/5",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = SuqiPrimary
                                            )
                                            if (sub.submittedAt.isNotBlank()) {
                                                Text("· ${sub.submittedAt}", fontSize = 11.sp, color = colors.mutedText)
                                            }
                                        }
                                    }
                                    SuqiStatusBadge(
                                        statusText = when (shop.verificationStatus) {
                                            "VERIFIED" -> strings.admin.queueVerified
                                            "RESUBMITTED" -> strings.admin.queueResubmitted
                                            "CHANGES_REQUESTED" -> strings.admin.queueWaitingInfo
                                            "REJECTED" -> strings.admin.queueRejected
                                            else -> strings.checkout.newBadge
                                        },
                                        type = when (shop.verificationStatus) {
                                            "VERIFIED" -> StatusType.SUCCESS
                                            "REJECTED" -> StatusType.ERROR
                                            else -> StatusType.WARNING
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

    val pages = viewerPages
    if (!pages.isNullOrEmpty()) {
        SuqiDocumentViewer(
            documents = pages,
            initialIndex = viewerInitialIndex,
            onDismiss = { viewerPages = null }
        )
    }

    if (showRequestChangesDialog && selectedShop != null) {
        val target = selectedShop!!
        AlertDialog(
            onDismissRequest = { showRequestChangesDialog = false },
            title = { Text(strings.admin.requestChangesBtn, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    VerificationDocsCodec.docSlots.forEach { slot ->
                        val picked = slot.key in selectedFailedKeys
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (picked) selectedFailedKeys.remove(slot.key) else selectedFailedKeys.add(slot.key)
                                }
                        ) {
                            Checkbox(
                                checked = picked,
                                onCheckedChange = {
                                    if (it) selectedFailedKeys.add(slot.key) else selectedFailedKeys.remove(slot.key)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = SuqiPrimary)
                            )
                            Text(slotLabel(slot.key, slot.label), fontSize = 12.sp, color = colors.primaryText)
                        }
                    }
                    SuqiTextField(
                        value = changeNote,
                        onValueChange = { changeNote = it },
                        label = strings.admin.reviewReasonHint,
                        placeholder = strings.adminOps.rejectReasonPlaceholder
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (changeNote.isBlank()) {
                            viewModel.showToast(strings.adminOps.rejectReasonPlaceholder)
                            return@Button
                        }
                        coroutineScope.launch {
                            viewModel.repository.adminVerifySellerDecision(
                                shopId = target.id,
                                decision = "REQUEST_CHANGES",
                                reason = changeNote.trim(),
                                failedKeys = selectedFailedKeys.joinToString(",")
                            )
                            showRequestChangesDialog = false
                            viewModel.showToast(strings.adminOps.verifyActionToast)
                            loadShops()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary)
                ) {
                    Text(strings.admin.requestChangesBtn, color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRequestChangesDialog = false }) { Text(strings.cancelLabel) }
            },
            containerColor = colors.cardBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showRejectDialog && selectedShop != null) {
        val target = selectedShop!!
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text(strings.admin.rejectBtn, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, color = SuqiError) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SuqiTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        label = strings.sellerOps.rejectReasonRequired,
                        placeholder = strings.adminOps.rejectReasonPlaceholder
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rejectReason.isBlank()) {
                            viewModel.showToast(strings.sellerOps.rejectReasonRequired)
                            return@Button
                        }
                        coroutineScope.launch {
                            viewModel.repository.adminVerifySellerDecision(
                                shopId = target.id,
                                decision = "REJECT",
                                reason = rejectReason.trim()
                            )
                            showRejectDialog = false
                            viewModel.showToast(strings.adminOps.verifyActionToast)
                            loadShops()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuqiError)
                ) {
                    Text(strings.sellerOps.confirmRejectionBtn, color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) { Text(strings.cancelLabel) }
            },
            containerColor = colors.cardBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }
}
