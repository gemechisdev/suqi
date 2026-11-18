package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LedgerEntryDto
import com.example.data.model.PayoutAccountDto
import com.example.data.model.SellerEarningsSummaryDto
import com.example.ui.SuqiViewModel
import com.example.ui.components.*
import com.example.ui.i18n.suqiStrings
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SellerEarningsScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val p5 = strings.phase5
    val isOffline by viewModel.isOffline.collectAsState()

    var summary by remember { mutableStateOf(viewModel.cachedSellerEarnings) }
    var isLoading by remember { mutableStateOf(summary == null) }
    var selectedTab by rememberSaveable { mutableStateOf("LEDGER") }
    var ledgerFilter by rememberSaveable { mutableStateOf("ALL") }
    var showPayoutSheet by remember { mutableStateOf(false) }
    var payoutAmountStr by remember { mutableStateOf("") }
    var selectedEntry by remember { mutableStateOf<LedgerEntryDto?>(null) }

    var providerType by remember { mutableStateOf(summary?.payoutAccount?.providerType ?: "TELEBIRR") }
    var bankName by remember { mutableStateOf(summary?.payoutAccount?.bankName ?: "Telebirr") }
    var accountName by remember { mutableStateOf(summary?.payoutAccount?.accountName.orEmpty()) }
    var accountNumber by remember { mutableStateOf(summary?.payoutAccount?.accountNumber.orEmpty()) }

    BackHandler(enabled = showPayoutSheet || selectedEntry != null) {
        when {
            showPayoutSheet -> showPayoutSheet = false
            selectedEntry != null -> selectedEntry = null
        }
    }

    fun loadEarnings() {
        coroutineScope.launch {
            isLoading = summary == null
            val res = viewModel.repository.getSellerEarningsSummary()
            if (res.success && res.data != null) {
                summary = res.data
                viewModel.cachedSellerEarnings = res.data
                providerType = res.data.payoutAccount.providerType
                bankName = res.data.payoutAccount.bankName
                accountName = res.data.payoutAccount.accountName
                accountNumber = res.data.payoutAccount.accountNumber
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadEarnings()
    }

    val current = summary

    SuqiScreen(
        title = strings.seller.earningsTitle,
        subtitle = current?.let { formatEtb(it.availableBalance) },
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        onRetryOffline = {
            viewModel.setOfflineMode(false)
            loadEarnings()
        },
        modifier = modifier
    ) { paddingValues ->
        if (isLoading && current == null) {
            SuqiSkeletonList(count = 4, modifier = Modifier.padding(paddingValues).padding(top = 12.dp))
        } else if (current == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                SuqiErrorCard(message = "${strings.seller.earningsTitle} — ${strings.retryLabel}", onRetry = { loadEarnings() })
            }
        } else {
            val filteredLedger = remember(current.ledger, ledgerFilter) {
                if (ledgerFilter == "ALL") current.ledger
                else current.ledger.filter { it.type == ledgerFilter }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    SuqiSectionCard(containerColor = colors.primaryTint, padding = 18.dp) {
                        Text(p5.availableBalanceLabel, fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.secondaryText)
                        Text(
                            text = formatEtb(current.availableBalance),
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 30.sp,
                            color = SuqiPrimary,
                            modifier = Modifier.testTag("seller_available_balance")
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "${p5.pendingClearanceLabel}: ${formatEtb(current.pendingBalance)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.primaryText
                                )
                                Text(
                                    String.format(p5.payoutMinNoteTemplate, formatEtb(current.minPayoutAmount), current.defaultCommissionRate.toInt()),
                                    fontSize = 11.sp,
                                    color = colors.secondaryText
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        SuqiButton(
                            text = p5.requestPayoutBtn,
                            isPill = true,
                            enabled = current.availableBalance >= current.minPayoutAmount,
                            onClick = {
                                payoutAmountStr = current.availableBalance.toInt().toString()
                                showPayoutSheet = true
                            },
                            testTag = "seller_request_payout_btn",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    SuqiTabs(
                        tabs = listOf(
                            SuqiTabItem("LEDGER", p5.ledgerTab, current.ledger.size),
                            SuqiTabItem("STATEMENTS", p5.statementsTab, current.statements.size),
                            SuqiTabItem("ACCOUNT", p5.payoutAccountTab)
                        ),
                        selectedKey = selectedTab,
                        onSelectTab = { selectedTab = it }
                    )
                }

                when (selectedTab) {
                    "LEDGER" -> {
                        item {
                            val filterOptions = listOf(
                                "ALL" to p5.filterAll,
                                "SALE" to p5.filterSales,
                                "COMMISSION" to p5.filterCommissions,
                                "REFUND" to p5.filterRefunds,
                                "PAYOUT" to p5.filterPayouts
                            )
                            SuqiFilterChips(
                                items = filterOptions,
                                selectedKey = ledgerFilter,
                                onSelect = { ledgerFilter = it }
                            )
                        }
                        if (filteredLedger.isEmpty()) {
                            item {
                                SuqiEmptyState(
                                    icon = LucideIcons.Wallet,
                                    title = strings.seller.earningsTitle,
                                    subtitle = p5.emptyAnalyticsSub
                                )
                            }
                        } else {
                            val grouped = filteredLedger.groupBy { it.weekLabel.ifBlank { p5.ledgerTab } }
                            grouped.forEach { (week, entries) ->
                                item(key = "hdr_$week") {
                                    Text(week, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.secondaryText)
                                }
                                items(entries, key = { it.id }) { entry ->
                                    SuqiSectionCard(
                                        padding = 14.dp,
                                        onClick = { selectedEntry = entry },
                                        modifier = Modifier.testTag("ledger_entry_${entry.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(entry.title, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text("${entry.subtitle} • ${entry.createdAt}", fontSize = 11.sp, color = colors.secondaryText)
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Column(horizontalAlignment = Alignment.End) {
                                                val isPos = entry.amount >= 0
                                                Text(
                                                    text = "${if (isPos) "+" else ""}${formatEtb(entry.amount)}",
                                                    fontFamily = SuqiFontFamily,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 14.sp,
                                                    color = if (isPos) SuqiSuccess else colors.primaryText
                                                )
                                                Text(entry.status, fontSize = 10.sp, color = colors.mutedText)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "STATEMENTS" -> {
                        items(current.statements, key = { it.monthKey }) { st ->
                            SuqiSectionCard(padding = 16.dp) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(st.monthLabel, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                                        Text("${st.ordersCount} • Net ${formatEtb(st.netAmount)}", fontSize = 12.sp, color = SuqiPrimary, fontWeight = FontWeight.SemiBold)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            val report = buildString {
                                                appendLine("Suqi Monthly Statement — ${st.monthLabel}")
                                                appendLine("${p5.filterSales}: ${formatEtb(st.grossSales)}")
                                                appendLine("${p5.filterCommissions}: -${formatEtb(st.commissions)}")
                                                appendLine("${p5.filterRefunds}: -${formatEtb(st.refunds)}")
                                                appendLine("${p5.filterPayouts}: -${formatEtb(st.payouts)}")
                                                appendLine("Net: ${formatEtb(st.netAmount)}")
                                            }
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_SUBJECT, st.monthLabel)
                                                putExtra(Intent.EXTRA_TEXT, report)
                                            }
                                            runCatching { context.startActivity(Intent.createChooser(intent, p5.saveStatementBtn)) }
                                            viewModel.showToast(strings.common.copiedToast)
                                        },
                                        shape = RoundedCornerShape(999.dp),
                                        modifier = Modifier.height(48.dp)
                                    ) {
                                        Icon(LucideIcons.Share2, contentDescription = p5.saveStatementBtn, tint = SuqiPrimary, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(p5.saveStatementBtn, color = SuqiPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        item {
                            SuqiSectionCard(padding = 16.dp) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("TELEBIRR" to "Telebirr", "CBE_BIRR" to "CBE Birr", "BANK_TRANSFER" to "Bank").forEach { (type, label) ->
                                            val sel = providerType == type
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .heightIn(min = 48.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(if (sel) SuqiPrimary else colors.secondaryBackground)
                                                    .clickable {
                                                        providerType = type
                                                        bankName = label
                                                    }
                                                    .padding(8.dp)
                                            ) {
                                                Text(label, color = if (sel) Color.White else colors.primaryText, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                    SuqiTextField(value = bankName, onValueChange = { bankName = it }, label = strings.checkout.bankTransferLabel)
                                    SuqiTextField(value = accountName, onValueChange = { accountName = it }, label = p5.payoutAccountNameLabel, testTag = "payout_account_name_input")
                                    SuqiTextField(value = accountNumber, onValueChange = { accountNumber = it }, label = p5.payoutAccountNumberLabel, testTag = "payout_account_number_input")
                                    SuqiButton(
                                        text = p5.savePayoutAccountBtn,
                                        isPill = true,
                                        onClick = {
                                            coroutineScope.launch {
                                                val res = viewModel.repository.saveShopPayoutAccount(
                                                    PayoutAccountDto(providerType, bankName.trim(), accountName.trim(), accountNumber.trim())
                                                )
                                                if (res.success) {
                                                    viewModel.showToast(strings.saveLabel)
                                                    loadEarnings()
                                                } else {
                                                    viewModel.showToast(viewModel.localizeMessage(res.message ?: strings.retryLabel))
                                                }
                                            }
                                        },
                                        testTag = "save_payout_account_btn",
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPayoutSheet && current != null) {
        SuqiBottomSheet(onDismissRequest = { showPayoutSheet = false }, title = p5.requestPayoutBtn) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${current.payoutAccount.bankName} • ${current.payoutAccount.accountNumber.ifBlank { "-" }}", fontSize = 13.sp, color = colors.secondaryText)
                SuqiTextField(
                    value = payoutAmountStr,
                    onValueChange = { payoutAmountStr = it },
                    label = strings.seller.priceEtbLabel,
                    testTag = "payout_amount_input"
                )
                SuqiButton(
                    text = p5.requestPayoutBtn,
                    isPill = true,
                    onClick = {
                        val amt = payoutAmountStr.toDoubleOrNull() ?: 0.0
                        coroutineScope.launch {
                            val res = viewModel.repository.requestSellerPayout(amt)
                            if (res.success) {
                                showPayoutSheet = false
                                loadEarnings()
                                viewModel.showToast(p5.requestPayoutBtn)
                            } else {
                                viewModel.showToast(viewModel.localizeMessage(res.message ?: strings.retryLabel))
                            }
                        }
                    },
                    testTag = "confirm_payout_request_btn",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    selectedEntry?.let { entry ->
        SuqiBottomSheet(onDismissRequest = { selectedEntry = null }, title = entry.title) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(entry.subtitle, fontSize = 13.sp, color = colors.secondaryText)
                Text("${strings.totalLabel}: ${formatEtb(entry.amount)}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = SuqiPrimary)
                if (entry.grossAmount > 0) Text("${p5.filterSales}: ${formatEtb(entry.grossAmount)}", fontSize = 13.sp, color = colors.primaryText)
                if (entry.feeAmount > 0) Text("${p5.filterCommissions}: -${formatEtb(entry.feeAmount)}", fontSize = 13.sp, color = colors.primaryText)
                Text("${entry.status} • ${entry.createdAt}", fontSize = 12.sp, color = colors.mutedText)
            }
        }
    }
}
