package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdminFinanceOverviewDto
import com.example.data.model.CommissionRuleDto
import com.example.data.model.PayoutRequestDto
import com.example.ui.SuqiViewModel
import com.example.ui.components.*
import com.example.ui.i18n.suqiStrings
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AdminFinanceScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val p5 = strings.phase5
    val clipboard = LocalClipboardManager.current
    val isOffline by viewModel.isOffline.collectAsState()

    var overview by remember { mutableStateOf(viewModel.cachedAdminFinance) }
    var isLoading by remember { mutableStateOf(overview == null) }
    var selectedTab by rememberSaveable { mutableStateOf("PAYOUTS") }
    var ledgerFilter by rememberSaveable { mutableStateOf("ALL") }

    var editingRuleRate by remember { mutableStateOf("5") }
    var editingMinPayout by remember { mutableStateOf("500") }
    var payDialogTarget by remember { mutableStateOf<PayoutRequestDto?>(null) }
    var transferRef by remember { mutableStateOf("") }

    BackHandler(enabled = payDialogTarget != null) {
        payDialogTarget = null
    }

    fun loadFinance() {
        coroutineScope.launch {
            isLoading = overview == null
            val res = viewModel.repository.getAdminFinanceOverview()
            if (res.success && res.data != null) {
                overview = res.data
                viewModel.cachedAdminFinance = res.data
                val defaultRule = res.data.rules.firstOrNull { it.categoryId == "DEFAULT" }
                if (defaultRule != null) {
                    editingRuleRate = defaultRule.ratePercent.toInt().toString()
                    editingMinPayout = defaultRule.minPayoutAmount.toInt().toString()
                }
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadFinance()
    }

    val current = overview

    SuqiScreen(
        title = strings.admin.financeTitle,
        subtitle = current?.let { "${it.pendingPayoutsCount} ${p5.pendingPayoutsLabel}" },
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        onRetryOffline = {
            viewModel.setOfflineMode(false)
            loadFinance()
        },
        modifier = modifier
    ) { paddingValues ->
        if (isLoading && current == null) {
            SuqiSkeletonList(count = 4, modifier = Modifier.padding(paddingValues).padding(top = 12.dp))
        } else if (current == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                SuqiErrorCard(title = strings.admin.financeTitle, message = strings.retryLabel, onRetry = { loadFinance() })
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            SuqiStatCard(
                                label = strings.adminGov.platformRevenueLabel,
                                value = formatEtb(current.platformRevenue),
                                icon = LucideIcons.TrendingUp,
                                tint = SuqiPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            SuqiStatCard(
                                label = p5.commissionRevenueLabel,
                                value = formatEtb(current.commissionRevenue),
                                icon = LucideIcons.BadgePercent,
                                tint = SuqiSuccess,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            SuqiStatCard(
                                label = p5.subscriptionRevenueLabel,
                                value = formatEtb(current.subscriptionRevenue),
                                icon = LucideIcons.Award,
                                tint = SuqiPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            SuqiStatCard(
                                label = p5.pendingPayoutsLabel,
                                value = formatEtb(current.pendingPayoutsTotal),
                                icon = LucideIcons.Wallet,
                                tint = if (current.pendingPayoutsCount > 0) SuqiWarning else SuqiSuccess,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                item {
                    SuqiTabs(
                        tabs = listOf(
                            SuqiTabItem("PAYOUTS", p5.payoutQueueTab, current.pendingPayoutsCount),
                            SuqiTabItem("RULES", p5.commissionRatesTab, current.rules.size),
                            SuqiTabItem("LEDGER", p5.financeLedgerTab, current.ledger.size)
                        ),
                        selectedKey = selectedTab,
                        onSelectTab = { selectedTab = it }
                    )
                }

                when (selectedTab) {
                    "PAYOUTS" -> {
                        if (current.payoutRequests.isEmpty()) {
                            item {
                                SuqiEmptyState(icon = LucideIcons.CheckCircle, title = p5.payoutQueueTab, subtitle = strings.adminGov.allClearLabel)
                            }
                        } else {
                            items(current.payoutRequests, key = { it.id }) { payout ->
                                SuqiSectionCard(padding = 16.dp, modifier = Modifier.testTag("admin_payout_${payout.id}")) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(payout.shopName, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                                            Text("${payout.bankName} • ${payout.accountNumber}", fontSize = 12.sp, color = colors.secondaryText)
                                        }
                                        SuqiStatusBadge(
                                            statusText = payout.status,
                                            type = when (payout.status) {
                                                "PAID" -> StatusType.SUCCESS
                                                "REJECTED" -> StatusType.ERROR
                                                else -> StatusType.WARNING
                                            }
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(formatEtb(payout.amount), fontFamily = SuqiFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = SuqiPrimary)
                                    if (payout.referenceCode.isNotBlank()) {
                                        Text("${strings.checkout.txRefLabel}: ${payout.referenceCode}", fontSize = 11.sp, color = colors.mutedText)
                                    }
                                    if (payout.status in listOf("PENDING", "APPROVED")) {
                                        Spacer(Modifier.height(10.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                            if (payout.status == "PENDING") {
                                                SuqiButton(
                                                    text = p5.approvePayoutBtn,
                                                    isPrimary = false,
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            viewModel.repository.decidePayoutRequest(payout.id, "APPROVE")
                                                            loadFinance()
                                                        }
                                                    },
                                                    testTag = "approve_payout_${payout.id}",
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            SuqiButton(
                                                text = p5.transferRefHint.substringBefore(" "),
                                                onClick = {
                                                    transferRef = ""
                                                    payDialogTarget = payout
                                                },
                                                testTag = "mark_paid_payout_${payout.id}",
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedButton(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        viewModel.repository.decidePayoutRequest(payout.id, "REJECT", note = "Declined by finance")
                                                        loadFinance()
                                                    }
                                                },
                                                shape = RoundedCornerShape(14.dp),
                                                border = BorderStroke(1.dp, SuqiError.copy(alpha = 0.5f)),
                                                modifier = Modifier.height(48.dp)
                                            ) {
                                                Text(strings.adminOps.rejectBtn, color = SuqiError, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "RULES" -> {
                        item {
                            SuqiSectionCard(padding = 16.dp) {
                                Text(p5.commissionRatesTab, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                                Spacer(Modifier.height(10.dp))
                                SuqiTextField(value = editingRuleRate, onValueChange = { editingRuleRate = it }, label = p5.defaultRateLabel, testTag = "commission_rate_input")
                                Spacer(Modifier.height(10.dp))
                                SuqiTextField(value = editingMinPayout, onValueChange = { editingMinPayout = it }, label = p5.minPayoutThresholdLabel, testTag = "min_payout_input")
                                Spacer(Modifier.height(12.dp))
                                SuqiButton(
                                    text = strings.saveLabel,
                                    isPill = true,
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.repository.saveCommissionConfig(
                                                CommissionRuleDto(
                                                    id = "comm_default",
                                                    categoryId = "DEFAULT",
                                                    categoryName = "All Categories",
                                                    ratePercent = editingRuleRate.toDoubleOrNull() ?: 5.0,
                                                    minPayoutAmount = editingMinPayout.toDoubleOrNull() ?: 500.0
                                                )
                                            )
                                            viewModel.showToast(strings.saveLabel)
                                            loadFinance()
                                        }
                                    },
                                    testTag = "save_commission_rule_btn",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        items(current.rules.filter { it.categoryId != "DEFAULT" }, key = { it.id }) { rule ->
                            SuqiSectionCard(padding = 14.dp) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(rule.categoryName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primaryText)
                                    Text("${rule.ratePercent.toInt()}%", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = SuqiPrimary)
                                }
                            }
                        }
                    }

                    else -> {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SuqiFilterChips(
                                    items = listOf(p5.filterAll, p5.filterCommissions, p5.subscriptionRevenueLabel, p5.filterPayouts),
                                    selectedItem = when (ledgerFilter) {
                                        "COMMISSION" -> p5.filterCommissions
                                        "SUBSCRIPTION" -> p5.subscriptionRevenueLabel
                                        "PAYOUT" -> p5.filterPayouts
                                        else -> p5.filterAll
                                    },
                                    onSelect = { chosen ->
                                        ledgerFilter = when (chosen) {
                                            p5.filterCommissions -> "COMMISSION"
                                            p5.subscriptionRevenueLabel -> "SUBSCRIPTION"
                                            p5.filterPayouts -> "PAYOUT"
                                            else -> "ALL"
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedButton(
                                    onClick = {
                                        val csv = buildString {
                                            appendLine("id,type,shop,amount,status,date")
                                            current.ledger.forEach { appendLine("${it.id},${it.type},${it.shopName},${it.amount},${it.status},${it.createdAt}") }
                                        }
                                        clipboard.setText(AnnotatedString(csv))
                                        viewModel.showToast(strings.common.copiedToast)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text(strings.adminGov.copyCsvBtn, fontSize = 11.sp, color = SuqiPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        val entries = current.ledger.filter { ledgerFilter == "ALL" || it.type == ledgerFilter }
                        items(entries, key = { it.id }) { entry ->
                            SuqiSectionCard(padding = 14.dp) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(entry.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primaryText)
                                        Text("${entry.subtitle} • ${entry.createdAt}", fontSize = 11.sp, color = colors.secondaryText)
                                    }
                                    Text(
                                        "${if (entry.amount >= 0) "+" else ""}${formatEtb(entry.amount)}",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (entry.amount >= 0) SuqiSuccess else colors.primaryText
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    payDialogTarget?.let { payout ->
        AlertDialog(
            onDismissRequest = { payDialogTarget = null },
            title = { Text("${payout.shopName} • ${formatEtb(payout.amount)}", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold) },
            text = {
                SuqiTextField(
                    value = transferRef,
                    onValueChange = { transferRef = it },
                    label = p5.transferRefHint,
                    testTag = "payout_ref_input"
                )
            },
            confirmButton = {
                TextButton(
                    enabled = transferRef.isNotBlank(),
                    onClick = {
                        coroutineScope.launch {
                            val res = viewModel.repository.decidePayoutRequest(payout.id, "MARK_PAID", referenceCode = transferRef.trim())
                            if (res.success) {
                                payDialogTarget = null
                                loadFinance()
                            } else {
                                viewModel.showToast(viewModel.localizeMessage(res.message ?: strings.retryLabel))
                            }
                        }
                    },
                    modifier = Modifier.testTag("payout_mark_paid_confirm")
                ) {
                    Text(strings.confirmLabel, color = SuqiSuccess, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { payDialogTarget = null }) { Text(strings.cancelLabel) } },
            containerColor = colors.cardBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }
}
