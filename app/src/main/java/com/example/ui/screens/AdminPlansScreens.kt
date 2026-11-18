package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AdminSettingsDto
import com.example.data.model.SubscriptionPlanDto
import com.example.data.model.SubscriptionUpgradeDto
import com.example.ui.SuqiViewModel
import com.example.ui.components.*
import com.example.ui.i18n.suqiStrings
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AdminPlansV2Screen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val p5 = strings.phase5
    val isOffline by viewModel.isOffline.collectAsState()

    var selectedTab by rememberSaveable { mutableStateOf("PLANS") }
    var plans by remember { mutableStateOf(viewModel.cachedAdminPlans) }
    var upgrades by remember { mutableStateOf(viewModel.cachedSubscriptionUpgrades) }
    var settings by remember { mutableStateOf(viewModel.cachedAdminSettings ?: AdminSettingsDto()) }
    var editingPlan by remember { mutableStateOf<SubscriptionPlanDto?>(null) }
    var showPlanSheet by remember { mutableStateOf(false) }
    var receiptPreview by remember { mutableStateOf<SubscriptionUpgradeDto?>(null) }

    BackHandler(enabled = showPlanSheet || receiptPreview != null) {
        when {
            receiptPreview != null -> receiptPreview = null
            showPlanSheet -> showPlanSheet = false
        }
    }

    fun loadAll() {
        coroutineScope.launch {
            viewModel.repository.getAdminSettings().data?.let {
                settings = it
                viewModel.cachedAdminSettings = it
            }
            viewModel.repository.getSubscriptionPlans().data?.let {
                plans = it
                viewModel.cachedAdminPlans = it
            }
            viewModel.repository.getSubscriptionUpgrades().data?.let {
                upgrades = it
                viewModel.cachedSubscriptionUpgrades = it
            }
        }
    }

    LaunchedEffect(Unit) {
        loadAll()
    }

    val pendingUpgrades = remember(upgrades) { upgrades.filter { it.status == "PENDING" } }
    val approvedUpgrades = remember(upgrades) { upgrades.filter { it.status == "APPROVED" } }
    val mrr = approvedUpgrades.sumOf { if (it.billingCycle == "YEARLY") it.amount / 12.0 else it.amount }

    SuqiScreen(
        title = strings.admin.plansMonetizationTitle,
        subtitle = "${plans.size} • ${pendingUpgrades.size} ${strings.admin.queuePending}",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        headerActions = {
            IconButton(
                onClick = {
                    editingPlan = null
                    showPlanSheet = true
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SuqiPrimary)
                    .testTag("admin_add_plan_btn")
            ) {
                Icon(LucideIcons.Plus, contentDescription = strings.adminOps.addPlanBtn, tint = Color.White)
            }
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    SuqiStatCard(
                        label = p5.mrrLabel,
                        value = formatEtb(mrr),
                        icon = LucideIcons.TrendingUp,
                        tint = SuqiPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    SuqiStatCard(
                        label = p5.activeSubscribersLabel,
                        value = approvedUpgrades.size.toString(),
                        icon = LucideIcons.Award,
                        tint = SuqiSuccess,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                SuqiTabs(
                    tabs = listOf(
                        SuqiTabItem("PLANS", p5.plansCatalogTab, plans.size),
                        SuqiTabItem("APPROVALS", p5.upgradeApprovalsTab, pendingUpgrades.size)
                    ),
                    selectedKey = selectedTab,
                    onSelectTab = { selectedTab = it }
                )
            }

            if (selectedTab == "PLANS") {
                item {
                    SuqiSectionCard(padding = 16.dp) {
                        SuqiSwitchRow(
                            title = p5.pilotModeSwitchLabel,
                            subtitle = "${p5.pilotEndDateLabel}: ${settings.pilotEndDate}",
                            checked = settings.isPilotMode,
                            onCheckedChange = { enabled ->
                                settings = settings.copy(isPilotMode = enabled)
                                coroutineScope.launch {
                                    viewModel.repository.updatePilotConfig(enabled, settings.pilotEndDate)
                                }
                            },
                            testTag = "admin_pilot_switch"
                        )
                        Spacer(Modifier.height(8.dp))
                        SuqiDateField(
                            label = p5.pilotEndDateLabel,
                            dateText = settings.pilotEndDate,
                            onDateSelected = { picked ->
                                settings = settings.copy(pilotEndDate = picked)
                                coroutineScope.launch {
                                    viewModel.repository.updatePilotConfig(settings.isPilotMode, picked)
                                }
                            }
                        )
                    }
                }

                items(plans, key = { it.id }) { plan ->
                    SuqiSectionCard(padding = 16.dp, modifier = Modifier.testTag("admin_plan_card_${plan.id}")) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(plan.name, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SuqiPrimary)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (plan.isPopular) SuqiStatusBadge(strings.sellerOps.recommendedBadge, StatusType.WARNING)
                                SuqiStatusBadge(
                                    if (plan.isActive) strings.adminOps.activeStatus else strings.adminOps.hiddenStatus,
                                    if (plan.isActive) StatusType.SUCCESS else StatusType.NEUTRAL
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (plan.monthlyPrice <= 0.0) strings.adminOps.freeLabel else "${formatEtb(plan.monthlyPrice)} / ${formatEtb(plan.yearlyPrice)}",
                            fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.primaryText
                        )
                        Text(
                            text = "${if (plan.maxProducts < 0) strings.adminOps.unlimitedProducts else String.format(strings.adminOps.upToProductsTemplate, plan.maxProducts)} • ${plan.maxPromoCodes} promos • ${plan.analyticsDays}d",
                            fontSize = 12.sp, color = colors.mutedText
                        )
                        Spacer(Modifier.height(8.dp))
                        plan.benefitsList().forEach { feat ->
                            Text("✓ $feat", fontSize = 13.sp, color = colors.secondaryText, modifier = Modifier.padding(vertical = 1.dp))
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            SuqiButton(
                                text = strings.adminOps.editTierBtn,
                                isPrimary = false,
                                onClick = {
                                    editingPlan = plan
                                    showPlanSheet = true
                                },
                                testTag = "edit_plan_${plan.id}",
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        viewModel.repository.deleteSubscriptionPlan(plan.id)
                                        loadAll()
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, SuqiError.copy(alpha = 0.5f)),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(LucideIcons.Trash2, contentDescription = strings.deleteLabel, tint = SuqiError, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            } else {
                if (upgrades.isEmpty()) {
                    item {
                        SuqiEmptyState(icon = LucideIcons.CheckCircle, title = p5.upgradeApprovalsTab, subtitle = p5.noPendingUpgrades)
                    }
                } else {
                    items(upgrades, key = { it.id }) { upg ->
                        SuqiSectionCard(padding = 16.dp, modifier = Modifier.testTag("admin_upgrade_${upg.id}")) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${upg.shopName} → ${upg.planName}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                                    Text("${upg.billingCycle} • ${upg.paymentMethodTitle} • ${upg.transactionReference}", fontSize = 12.sp, color = colors.secondaryText)
                                }
                                SuqiStatusBadge(
                                    upg.status,
                                    when (upg.status) {
                                        "APPROVED" -> StatusType.SUCCESS
                                        "REJECTED" -> StatusType.ERROR
                                        else -> StatusType.WARNING
                                    }
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(formatEtb(upg.amount), fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = SuqiPrimary)
                            if (upg.receiptImageUri.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                AsyncImage(
                                    model = upg.receiptImageUri,
                                    contentDescription = strings.uploadReceipt,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(96.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { receiptPreview = upg }
                                )
                            }
                            if (upg.status == "PENDING") {
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    SuqiButton(
                                        text = strings.admin.approveBtn,
                                        onClick = {
                                            coroutineScope.launch {
                                                viewModel.repository.decideSubscriptionUpgrade(upg.id, "APPROVE")
                                                loadAll()
                                            }
                                        },
                                        testTag = "approve_upgrade_${upg.id}",
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                viewModel.repository.decideSubscriptionUpgrade(upg.id, "REJECT", "Receipt mismatch")
                                                loadAll()
                                            }
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, SuqiError.copy(alpha = 0.5f)),
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Text(strings.admin.rejectBtn, color = SuqiError, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPlanSheet) {
        var name by remember(editingPlan) { mutableStateOf(editingPlan?.name.orEmpty()) }
        var monthlyStr by remember(editingPlan) { mutableStateOf(editingPlan?.monthlyPrice?.toInt()?.toString() ?: "299") }
        var yearlyStr by remember(editingPlan) { mutableStateOf(editingPlan?.yearlyPrice?.toInt()?.toString() ?: "2990") }
        var maxProdStr by remember(editingPlan) { mutableStateOf(editingPlan?.maxProducts?.toString() ?: "50") }
        var maxPromosStr by remember(editingPlan) { mutableStateOf(editingPlan?.maxPromoCodes?.toString() ?: "10") }
        var analyticsDaysStr by remember(editingPlan) { mutableStateOf(editingPlan?.analyticsDays?.toString() ?: "90") }
        var isPopular by remember(editingPlan) { mutableStateOf(editingPlan?.isPopular ?: false) }
        var isActive by remember(editingPlan) { mutableStateOf(editingPlan?.isActive ?: true) }
        val benefits = remember(editingPlan) {
            mutableStateListOf<String>().apply {
                addAll(editingPlan?.benefitsList().orEmpty().ifEmpty { listOf("Featured Placement", "Full Analytics", "Unlimited Promos") })
            }
        }
        var newBenefit by remember { mutableStateOf("") }

        SuqiBottomSheet(
            onDismissRequest = { showPlanSheet = false },
            title = if (editingPlan == null) strings.adminOps.addPlanBtn else strings.adminOps.editTierBtn
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SuqiTextField(value = name, onValueChange = { name = it }, label = strings.seller.paramNameRequiredLabel, testTag = "plan_name_input")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SuqiTextField(value = monthlyStr, onValueChange = { monthlyStr = it }, label = p5.monthlyLabel, modifier = Modifier.weight(1f))
                    SuqiTextField(value = yearlyStr, onValueChange = { yearlyStr = it }, label = p5.yearlyLabel, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SuqiTextField(value = maxProdStr, onValueChange = { maxProdStr = it }, label = strings.seller.stockLabel, modifier = Modifier.weight(1f))
                    SuqiTextField(value = maxPromosStr, onValueChange = { maxPromosStr = it }, label = p5.maxPromoCodesLabel, modifier = Modifier.weight(1f))
                    SuqiTextField(value = analyticsDaysStr, onValueChange = { analyticsDaysStr = it }, label = p5.analyticsWindowDaysLabel, modifier = Modifier.weight(1f))
                }

                Text(p5.benefitsEditorLabel, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.primaryText)
                benefits.forEachIndexed { idx, item ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("✓ $item", fontSize = 13.sp, color = colors.primaryText, modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { benefits.removeAt(idx) },
                            modifier = Modifier.size(48.dp)
                        ) { Icon(LucideIcons.Trash2, contentDescription = strings.deleteLabel, tint = SuqiError, modifier = Modifier.size(16.dp)) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SuqiTextField(value = newBenefit, onValueChange = { newBenefit = it }, label = p5.addBenefitHint, modifier = Modifier.weight(1f), testTag = "plan_benefit_input")
                    Button(
                        onClick = {
                            if (newBenefit.isNotBlank()) {
                                benefits.add(newBenefit.trim())
                                newBenefit = ""
                            }
                        },
                        modifier = Modifier.height(48.dp).testTag("add_benefit_btn")
                    ) { Text(strings.common.addLabel) }
                }

                SuqiSwitchRow(title = strings.sellerOps.recommendedBadge, checked = isPopular, onCheckedChange = { isPopular = it })
                SuqiSwitchRow(title = strings.adminOps.activeStatus, checked = isActive, onCheckedChange = { isActive = it })
                SuqiButton(
                    text = strings.saveLabel,
                    isPill = true,
                    onClick = {
                        if (name.isBlank()) return@SuqiButton
                        coroutineScope.launch {
                            viewModel.repository.saveSubscriptionPlan(
                                SubscriptionPlanDto(
                                    id = editingPlan?.id.orEmpty(),
                                    name = name.trim(),
                                    monthlyPrice = monthlyStr.toDoubleOrNull() ?: 0.0,
                                    yearlyPrice = yearlyStr.toDoubleOrNull() ?: 0.0,
                                    maxProducts = maxProdStr.toIntOrNull() ?: 50,
                                    featuresCsv = benefits.joinToString("|"),
                                    isPopular = isPopular,
                                    isActive = isActive,
                                    maxPromoCodes = maxPromosStr.toIntOrNull() ?: 5,
                                    analyticsDays = analyticsDaysStr.toIntOrNull() ?: 7
                                )
                            )
                            showPlanSheet = false
                            loadAll()
                        }
                    },
                    testTag = "save_plan_btn",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    receiptPreview?.let { upg ->
        SuqiDocumentViewer(
            documents = listOf(SuqiDocumentPage("${upg.shopName} • ${upg.transactionReference}", upg.receiptImageUri, formatEtb(upg.amount))),
            initialIndex = 0,
            onDismiss = { receiptPreview = null }
        )
    }
}
