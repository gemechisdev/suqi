package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.SuqiViewModel
import com.example.ui.components.*
import com.example.ui.i18n.suqiStrings
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SellerVerificationV2Screen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val isOffline by viewModel.isOffline.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var shop by remember { mutableStateOf<ShopDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }

    val docUris = remember { mutableStateMapOf<String, String>() }
    var licenseNumber by remember { mutableStateOf("") }
    var tinNumber by remember { mutableStateOf("") }
    var docErrorSlot by remember { mutableStateOf<String?>(null) }
    var viewerPages by remember { mutableStateOf<List<SuqiDocumentPage>?>(null) }
    var viewerIndex by remember { mutableIntStateOf(0) }

    fun slotLabel(key: String, fallback: String): String = when (key) {
        "idFront" -> strings.seller.docIdFront
        "idBack" -> strings.seller.docIdBack
        "tradeLicense" -> strings.seller.docTradeLicense
        "shopPhoto" -> strings.seller.docShopPhoto
        "selfieWithId" -> strings.seller.docSelfieWithId
        else -> fallback
    }

    fun slotGuidance(key: String, fallback: String): String = when (key) {
        "idFront" -> strings.sellerOps.docIdFrontGuide
        "idBack" -> strings.sellerOps.docIdBackGuide
        "tradeLicense" -> strings.sellerOps.docTradeLicenseGuide
        "shopPhoto" -> strings.sellerOps.docShopPhotoGuide
        "selfieWithId" -> strings.sellerOps.docSelfieWithIdGuide
        else -> fallback
    }

    fun loadShop() {
        coroutineScope.launch {
            isLoading = true
            val sid = resolveActiveSellerShopId(viewModel)
            if (sid.isNotBlank()) {
                val res = viewModel.repository.getShopDetails(sid)
                if (res.success && res.data != null) {
                    shop = res.data
                    val parsed = VerificationDocsCodec.parse(res.data.verificationDocsJson)
                    docUris.clear()
                    docUris.putAll(parsed.docs)
                    if (parsed.licenseRef.isNotBlank()) licenseNumber = parsed.licenseRef
                    if (parsed.tin.isNotBlank()) tinNumber = parsed.tin
                }
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadShop() }

    val status = shop?.verificationStatus ?: "NOT_SUBMITTED"
    val parsedSubmission = remember(shop?.verificationDocsJson) {
        VerificationDocsCodec.parse(shop?.verificationDocsJson)
    }
    val failedKeys = parsedSubmission.failedKeys.toSet()

    val currentStep = when (status) {
        "VERIFIED" -> 3
        "REJECTED", "CHANGES_REQUESTED" -> 2
        "PENDING_REVIEW", "RESUBMITTED" -> 1
        else -> 0
    }

    val isEditable = status !in listOf("VERIFIED", "PENDING_REVIEW", "RESUBMITTED")

    SuqiScreen(
        title = strings.seller.sellerVerificationTitle,
        subtitle = strings.sellerOps.merchantVerificationSub,
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.padding(paddingValues).padding(top = 12.dp)) {
                SuqiSkeletonList(count = 3)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Status Tracker Top Card (4 steps)
                SuqiSectionCard(padding = 18.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(
                                    when (status) {
                                        "VERIFIED" -> colors.successBg
                                        "PENDING_REVIEW", "RESUBMITTED" -> colors.warningBg
                                        "REJECTED", "CHANGES_REQUESTED" -> colors.errorBg
                                        else -> colors.primaryTint
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = LucideIcons.BadgeCheck,
                                contentDescription = strings.seller.verificationStatusTitle,
                                tint = when (status) {
                                    "VERIFIED" -> SuqiSuccess
                                    "PENDING_REVIEW", "RESUBMITTED" -> SuqiWarning
                                    "REJECTED", "CHANGES_REQUESTED" -> SuqiError
                                    else -> SuqiPrimary
                                },
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = when (status) {
                                "VERIFIED" -> strings.admin.queueVerified
                                "PENDING_REVIEW" -> strings.admin.queuePending
                                "RESUBMITTED" -> strings.admin.queueResubmitted
                                "CHANGES_REQUESTED" -> strings.admin.queueWaitingInfo
                                "REJECTED" -> strings.admin.queueRejected
                                else -> strings.seller.sellerVerificationTitle
                            },
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = colors.primaryText,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = when (status) {
                                "CHANGES_REQUESTED", "REJECTED" -> shop?.verificationRejectionReason
                                    ?: strings.sellerOps.merchantVerificationSub
                                else -> strings.sellerOps.merchantVerificationSub
                            },
                            fontFamily = SuqiFontFamily,
                            fontSize = 13.sp,
                            color = colors.secondaryText,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(14.dp))

                        // Verified badge preview chip
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(colors.secondaryBackground)
                                .border(1.dp, colors.border, RoundedCornerShape(999.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = shop?.name ?: strings.sellerOps.mySuqiShopFallback,
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = colors.primaryText
                            )
                            Icon(
                                LucideIcons.BadgeCheck,
                                contentDescription = strings.verifiedLabel,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (status == "VERIFIED") strings.adminOps.activeStatus else strings.verifiedLabel,
                                fontFamily = SuqiFontFamily,
                                fontSize = 11.sp,
                                color = if (status == "VERIFIED") SuqiSuccess else colors.mutedText
                            )
                        }

                        Spacer(Modifier.height(18.dp))
                        SuqiStepIndicator(
                            steps = listOf(
                                strings.checkout.stepPaymentSubmitted,
                                strings.admin.queuePending,
                                strings.admin.queueWaitingInfo,
                                strings.verifiedLabel
                            ),
                            currentStep = currentStep
                        )
                    }
                }

                // Rejection / Change request banner
                if (status == "REJECTED" || status == "CHANGES_REQUESTED") {
                    SuqiSectionCard(containerColor = colors.errorBg, padding = 14.dp) {
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(LucideIcons.TriangleAlert, contentDescription = null, tint = SuqiError, modifier = Modifier.size(20.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.checkout.rejectionReasonTitle,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = SuqiError
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = shop?.verificationRejectionReason ?: strings.admin.reviewReasonHint,
                                    fontFamily = SuqiFontFamily,
                                    fontSize = 12.sp,
                                    color = colors.primaryText
                                )
                                if (failedKeys.isNotEmpty()) {
                                    Spacer(Modifier.height(6.dp))
                                    val failedLabels = VerificationDocsCodec.docSlots
                                        .filter { it.key in failedKeys }
                                        .joinToString(" • ") { slotLabel(it.key, it.label) }
                                    Text(
                                        text = failedLabels,
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        color = SuqiError
                                    )
                                }
                            }
                        }
                    }
                }

                // 5 Document Capture Tiles
                SuqiSectionCard(padding = 16.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.adminOps.submittedDocsTitle,
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = colors.primaryText
                            )
                            Text(
                                text = "${docUris.count { it.value.isNotBlank() }} / ${VerificationDocsCodec.docSlots.size}",
                                fontFamily = SuqiFontFamily,
                                fontSize = 12.sp,
                                color = colors.secondaryText
                            )
                        }
                        if (docUris.isNotEmpty()) {
                            Text(
                                text = strings.common.viewAllLabel,
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = SuqiPrimary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .clickable {
                                        viewerPages = VerificationDocsCodec.docSlots.mapNotNull { slot ->
                                            val uri = docUris[slot.key]
                                            if (uri.isNullOrBlank()) null
                                            else SuqiDocumentPage(slotLabel(slot.key, slot.label), uri, slotGuidance(slot.key, slot.guidance))
                                        }
                                        viewerIndex = 0
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    VerificationDocsCodec.docSlots.forEachIndexed { index, slot ->
                        val currentUri = docUris[slot.key]
                        val localizedTitle = slotLabel(slot.key, slot.label)
                        val localizedGuide = slotGuidance(slot.key, slot.guidance)
                        // In CHANGES_REQUESTED, non-failed tiles with already-uploaded docs stay locked/verified
                        val tileLocked = when (status) {
                            "VERIFIED", "PENDING_REVIEW", "RESUBMITTED" -> true
                            "CHANGES_REQUESTED" -> failedKeys.isNotEmpty() && slot.key !in failedKeys && !currentUri.isNullOrBlank()
                            else -> false
                        }
                        val slotError = when {
                            docErrorSlot == slot.key -> "$localizedTitle (${strings.common.requiredLabel})"
                            status in listOf("CHANGES_REQUESTED", "REJECTED") && slot.key in failedKeys -> strings.admin.requestChangesBtn
                            else -> null
                        }

                        SuqiImagePickerTile(
                            label = localizedTitle,
                            imageUri = currentUri,
                            onImagePicked = { picked ->
                                docUris[slot.key] = picked
                                if (docErrorSlot == slot.key) docErrorSlot = null
                            },
                            onRemove = if (!tileLocked) ({ docUris.remove(slot.key) }) else null,
                            guidance = localizedGuide,
                            errorMessage = slotError,
                            isLockedVerified = tileLocked && !currentUri.isNullOrBlank(),
                            demoSampleUrl = slot.demoSampleUrl,
                            testTag = "seller_verify_doc_${slot.key}"
                        )
                        if (index < VerificationDocsCodec.docSlots.lastIndex) {
                            Spacer(Modifier.height(14.dp))
                        }
                    }
                }

                // Optional Reference Numbers Card
                SuqiSectionCard(padding = 16.dp) {
                    Text(
                        text = strings.seller.licenseNumberLabel,
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = colors.primaryText
                    )
                    Spacer(Modifier.height(10.dp))
                    SuqiTextField(
                        value = licenseNumber,
                        onValueChange = { licenseNumber = it },
                        label = strings.seller.licenseNumberLabel,
                        placeholder = "ET-AA-2026-4410",
                        enabled = isEditable,
                        testTag = "seller_verify_license_ref"
                    )
                    Spacer(Modifier.height(10.dp))
                    SuqiTextField(
                        value = tinNumber,
                        onValueChange = { tinNumber = it },
                        label = strings.seller.tinNumberLabel,
                        placeholder = "0041298501",
                        enabled = isEditable,
                        testTag = "seller_verify_tin"
                    )
                }

                // Decision / Review History Card
                if (parsedSubmission.history.isNotEmpty()) {
                    SuqiSectionCard(padding = 16.dp) {
                        Text(
                            text = strings.seller.verificationActivityTitle,
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = colors.primaryText
                        )
                        Spacer(Modifier.height(8.dp))
                        parsedSubmission.history.asReversed().forEachIndexed { idx, entry ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = entry.status.replace("_", " "),
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = when (entry.status) {
                                        "VERIFIED" -> SuqiSuccess
                                        "REJECTED", "CHANGES_REQUESTED" -> SuqiError
                                        else -> SuqiPrimary
                                    }
                                )
                                Text(entry.at, fontSize = 11.sp, color = colors.mutedText)
                            }
                            if (entry.note.isNotBlank()) {
                                Spacer(Modifier.height(2.dp))
                                Text(entry.note, fontSize = 12.sp, color = colors.secondaryText)
                            }
                            if (idx < parsedSubmission.history.lastIndex) {
                                HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                }

                if (isEditable) {
                    SuqiButton(
                        text = when {
                            isSubmitting -> strings.checkout.submittingLabel
                            status in listOf("REJECTED", "CHANGES_REQUESTED") -> strings.seller.resubmitVerificationBtn
                            else -> strings.seller.submitVerificationBtn
                        },
                        enabled = !isSubmitting,
                        onClick = {
                            val missingSlot = VerificationDocsCodec.docSlots.firstOrNull {
                                docUris[it.key].isNullOrBlank()
                            }
                            if (missingSlot != null) {
                                docErrorSlot = missingSlot.key
                                viewModel.showToast(slotLabel(missingSlot.key, missingSlot.label))
                                return@SuqiButton
                            }
                            isSubmitting = true
                            coroutineScope.launch {
                                val payload = buildMap {
                                    VerificationDocsCodec.docSlots.forEach { slot ->
                                        put(slot.key, docUris[slot.key].orEmpty())
                                    }
                                    put("license", licenseNumber.trim())
                                    put("tin", tinNumber.trim())
                                }
                                val res = viewModel.repository.submitSellerVerification(payload)
                                isSubmitting = false
                                if (res.success) {
                                    viewModel.showToast(strings.adminOps.verifyActionToast)
                                    loadShop()
                                } else {
                                    viewModel.showToast(res.message ?: strings.retryLabel)
                                }
                            }
                        },
                        testTag = "submit_seller_verification_btn",
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    SuqiButton(
                        text = strings.common.goBackLabel,
                        isPrimary = false,
                        onClick = { viewModel.goBack() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    val pages = viewerPages
    if (!pages.isNullOrEmpty()) {
        SuqiDocumentViewer(
            documents = pages,
            initialIndex = viewerIndex,
            onDismiss = { viewerPages = null }
        )
    }
}
