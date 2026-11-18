package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.BackendConfig
import com.example.data.model.AdminSettingsDto
import com.example.data.model.SubscriptionPlanDto
import com.example.data.model.SubscriptionUpgradeDto
import com.example.ui.SuqiViewModel
import com.example.ui.components.*
import com.example.ui.i18n.suqiStrings
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SellerSubscriptionV2Screen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val p5 = strings.phase5
    val isOffline by viewModel.isOffline.collectAsState()

    var settings by remember { mutableStateOf(viewModel.cachedAdminSettings ?: AdminSettingsDto()) }
    var plans by remember { mutableStateOf(viewModel.cachedAdminPlans) }
    var upgrades by remember { mutableStateOf(viewModel.cachedSubscriptionUpgrades) }
    var isLoading by remember { mutableStateOf(plans.isEmpty()) }
    var billingCycle by rememberSaveable { mutableStateOf("MONTHLY") }

    var upgradeTargetPlan by remember { mutableStateOf<SubscriptionPlanDto?>(null) }
    var upgradeStep by remember { mutableIntStateOf(0) }
    var txRef by remember { mutableStateOf("") }
    var receiptUri by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Telebirr") }
    var showCancelConfirm by remember { mutableStateOf(false) }

    BackHandler(enabled = upgradeTargetPlan != null || showCancelConfirm) {
        when {
            showCancelConfirm -> showCancelConfirm = false
            upgradeTargetPlan != null -> upgradeTargetPlan = null
        }
    }

    fun loadAll() {
        coroutineScope.launch {
            isLoading = plans.isEmpty()
            viewModel.repository.getAdminSettings().data?.let {
                settings = it
                viewModel.cachedAdminSettings = it
            }
            val plansRes = viewModel.repository.getSubscriptionPlans()
            if (plansRes.success && plansRes.data != null) {
                plans = plansRes.data.filter { it.isActive }
                viewModel.cachedAdminPlans = plansRes.data
            }
            val upRes = viewModel.repository.getSubscriptionUpgrades()
            if (upRes.success && upRes.data != null) {
                upgrades = upRes.data
                viewModel.cachedSubscriptionUpgrades = upRes.data
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadAll()
    }

    val activeApproved = remember(upgrades) { upgrades.firstOrNull { it.status == "APPROVED" } }
    val pendingUpgrade = remember(upgrades) { upgrades.firstOrNull { it.status == "PENDING" } }
    val currentPlanId = activeApproved?.planId
        ?: plans.firstOrNull { it.monthlyPrice <= 0.0 }?.id
        ?: plans.firstOrNull()?.id

    SuqiScreen(
        title = strings.seller.subscriptionTitle,
        subtitle = activeApproved?.planName ?: plans.firstOrNull { it.id == currentPlanId }?.name,
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        if (isLoading && plans.isEmpty()) {
            SuqiSkeletonList(count = 3, modifier = Modifier.padding(paddingValues).padding(top = 12.dp))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (settings.isPilotMode || !settings.isSubscriptionModeEnabled) {
                    item {
                        SuqiSectionCard(containerColor = colors.successBg, padding = 14.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(LucideIcons.Sparkles, contentDescription = null, tint = SuqiSuccess, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(strings.sellerOps.freePilotBadge, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SuqiSuccess)
                                    Text("${p5.pilotEndDateLabel}: ${settings.pilotEndDate}", fontSize = 12.sp, color = colors.primaryText)
                                }
                            }
                        }
                    }
                }

                // Billing cycle switch
                item {
                    SuqiSectionCard(padding = 8.dp) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf("MONTHLY" to p5.monthlyLabel, "YEARLY" to "${p5.yearlyLabel} (${p5.yearlySavingsBadge})").forEach { (cycle, label) ->
                                val sel = billingCycle == cycle
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (sel) SuqiPrimary else Color.Transparent)
                                        .clickable { billingCycle = cycle }
                                        .padding(horizontal = 8.dp, vertical = 10.dp)
                                        .testTag("sub_cycle_$cycle")
                                ) {
                                    Text(label, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = if (sel) Color.White else colors.primaryText)
                                }
                            }
                        }
                    }
                }

                if (pendingUpgrade != null) {
                    item {
                        SuqiSectionCard(containerColor = colors.warningBg, padding = 14.dp) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${pendingUpgrade.planName} (${pendingUpgrade.billingCycle})", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primaryText)
                                    Text("${pendingUpgrade.transactionReference} • ${formatEtb(pendingUpgrade.amount)}", fontSize = 12.sp, color = colors.secondaryText)
                                }
                                SuqiStatusBadge(strings.admin.queuePending, StatusType.WARNING)
                            }
                        }
                    }
                }

                items(plans, key = { it.id }) { plan ->
                    val isCurrent = plan.id == currentPlanId
                    val price = if (billingCycle == "YEARLY") plan.yearlyPrice else plan.monthlyPrice
                    SuqiSectionCard(
                        containerColor = if (plan.isPopular) colors.primaryTint else colors.cardBackground,
                        padding = 16.dp,
                        modifier = Modifier.testTag("seller_plan_card_${plan.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(plan.name, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.primaryText)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (isCurrent) SuqiStatusBadge(p5.currentPlanBadge, StatusType.SUCCESS)
                                if (plan.isPopular) SuqiStatusBadge(strings.sellerOps.recommendedBadge, StatusType.WARNING)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (price <= 0.0) strings.adminOps.freeLabel else formatEtb(price),
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
                            color = SuqiPrimary
                        )
                        Text(
                            text = "${if (plan.maxProducts < 0) strings.adminOps.unlimitedProducts else String.format(strings.adminOps.upToProductsTemplate, plan.maxProducts)} • ${plan.analyticsDays}d analytics",
                            fontSize = 12.sp,
                            color = colors.secondaryText
                        )
                        Spacer(Modifier.height(10.dp))
                        plan.benefitsList().forEach { feat ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                Icon(LucideIcons.Check, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(feat, fontSize = 13.sp, color = colors.primaryText)
                            }
                        }
                        if (!isCurrent && price > 0.0) {
                            Spacer(Modifier.height(12.dp))
                            SuqiButton(
                                text = p5.upgradePlanBtn,
                                isPill = true,
                                onClick = {
                                    upgradeTargetPlan = plan
                                    upgradeStep = 0
                                    txRef = ""
                                    receiptUri = ""
                                },
                                testTag = "upgrade_plan_btn_${plan.id}",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Compare summary + cancel link
                if (plans.size >= 2) {
                    item {
                        SuqiSectionCard(padding = 16.dp) {
                            Text(p5.freeVsProTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                            Spacer(Modifier.height(8.dp))
                            plans.take(2).forEach { pl ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(pl.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.primaryText)
                                    Text("${pl.maxPromoCodes} promos • ${pl.analyticsDays}d", fontSize = 12.sp, color = colors.secondaryText)
                                }
                            }
                            if (activeApproved != null) {
                                Spacer(Modifier.height(8.dp))
                                TextButton(
                                    onClick = { showCancelConfirm = true },
                                    modifier = Modifier.testTag("cancel_subscription_btn")
                                ) {
                                    Text(p5.downgradeCancelBtn, color = SuqiError, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                // Billing history
                if (upgrades.isNotEmpty()) {
                    item {
                        Text(p5.billingHistoryTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.primaryText)
                    }
                    items(upgrades, key = { it.id }) { upg ->
                        SuqiSectionCard(padding = 14.dp) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("${upg.planName} (${upg.billingCycle})", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primaryText)
                                    Text("${upg.transactionReference} • ${upg.createdAt}", fontSize = 11.sp, color = colors.mutedText)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(formatEtb(upg.amount), fontWeight = FontWeight.Bold, color = SuqiPrimary)
                                    SuqiStatusBadge(
                                        upg.status,
                                        when (upg.status) {
                                            "APPROVED" -> StatusType.SUCCESS
                                            "REJECTED", "CANCELLED" -> StatusType.ERROR
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

    upgradeTargetPlan?.let { target ->
        val amount = if (billingCycle == "YEARLY") target.yearlyPrice else target.monthlyPrice
        SuqiBottomSheet(
            onDismissRequest = { upgradeTargetPlan = null },
            title = "${p5.upgradePlanBtn}: ${target.name}"
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SuqiStepIndicator(
                    steps = listOf(target.name, strings.checkoutPaymentTitle, strings.admin.queuePending),
                    currentStep = upgradeStep
                )
                SuqiSectionCard(containerColor = colors.shopTint, padding = 14.dp) {
                    Text("Suqi Ethiopia Marketplace", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primaryText)
                    Text("$paymentMethod • +251 911 000 999", fontSize = 13.sp, color = colors.secondaryText)
                    Spacer(Modifier.height(4.dp))
                    Text(formatEtb(amount), fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = SuqiPrimary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Telebirr", "CBE Birr").forEach { method ->
                        val sel = paymentMethod == method
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (sel) SuqiPrimary else colors.secondaryBackground)
                                .clickable { paymentMethod = method }
                                .padding(8.dp)
                        ) {
                            Text(method, color = if (sel) Color.White else colors.primaryText, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                SuqiTextField(
                    value = txRef,
                    onValueChange = { txRef = it },
                    label = strings.checkout.txRefLabel,
                    testTag = "upgrade_tx_ref_input"
                )
                SuqiImagePickerTile(
                    label = strings.uploadReceipt,
                    imageUri = receiptUri,
                    onImagePicked = { receiptUri = it },
                    onRemove = if (receiptUri.isNotBlank()) { { receiptUri = "" } } else null,
                    demoSampleUrl = if (BackendConfig.DemoMode) "https://images.unsplash.com/photo-1554224155-6726b3ff858f?w=600" else null,
                    testTag = "upgrade_receipt_picker"
                )
                SuqiButton(
                    text = p5.upgradePlanBtn,
                    isPill = true,
                    onClick = {
                        coroutineScope.launch {
                            val res = viewModel.repository.submitSubscriptionUpgrade(
                                planId = target.id,
                                billingCycle = billingCycle,
                                paymentMethodTitle = paymentMethod,
                                transactionReference = txRef.trim(),
                                receiptImageUri = receiptUri
                            )
                            if (res.success) {
                                upgradeTargetPlan = null
                                loadAll()
                                viewModel.showToast(strings.admin.queuePending)
                            } else {
                                viewModel.showToast(viewModel.localizeMessage(res.message ?: strings.retryLabel))
                            }
                        }
                    },
                    testTag = "submit_upgrade_btn",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            title = { Text(p5.downgradeCancelBtn, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold) },
            text = { Text(p5.cancelPlanConfirmMsg, fontSize = 13.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.repository.cancelShopSubscription()
                            showCancelConfirm = false
                            loadAll()
                        }
                    },
                    modifier = Modifier.testTag("confirm_cancel_subscription_btn")
                ) { Text(strings.confirmLabel, color = SuqiError, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showCancelConfirm = false }) { Text(strings.cancelLabel) } },
            containerColor = colors.cardBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }
}
