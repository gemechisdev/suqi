package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FlashSaleDto
import com.example.data.model.ProductDto
import com.example.data.model.PromoCodeDto
import com.example.ui.SuqiViewModel
import com.example.ui.components.*
import com.example.ui.i18n.suqiStrings
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun PromotionsV2Screen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val p5 = strings.phase5
    val clipboard = LocalClipboardManager.current
    val isOffline by viewModel.isOffline.collectAsState()

    var activeShopId by remember { mutableStateOf("") }
    var selectedTab by rememberSaveable { mutableStateOf("COUPONS") }
    var promos by remember { mutableStateOf<List<PromoCodeDto>>(emptyList()) }
    var flashSales by remember { mutableStateOf<List<FlashSaleDto>>(emptyList()) }
    var shopProducts by remember { mutableStateOf<List<ProductDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var showPromoWizard by remember { mutableStateOf(false) }
    var promoStep by remember { mutableIntStateOf(0) }
    var codeInput by remember { mutableStateOf("") }
    var discountType by remember { mutableStateOf("PERCENTAGE") }
    var discountValStr by remember { mutableStateOf("15") }
    var minOrderStr by remember { mutableStateOf("500") }
    var scopeType by remember { mutableStateOf("SHOP") }
    val selectedScopeIds = remember { mutableStateListOf<String>() }
    var maxUsesStr by remember { mutableStateOf("50") }
    var perCustomerStr by remember { mutableStateOf("1") }
    var startDate by remember { mutableStateOf("2026-01-01") }
    var endDate by remember { mutableStateOf("2026-12-31") }

    var showFlashSheet by remember { mutableStateOf(false) }
    var flashProduct by remember { mutableStateOf<ProductDto?>(null) }
    var flashPriceStr by remember { mutableStateOf("") }
    var flashStockCapStr by remember { mutableStateOf("15") }

    BackHandler(enabled = showPromoWizard || showFlashSheet) {
        when {
            showPromoWizard -> showPromoWizard = false
            showFlashSheet -> showFlashSheet = false
        }
    }

    fun reload() {
        coroutineScope.launch {
            isLoading = true
            val sid = resolveActiveSellerShopId(viewModel).ifBlank { "shop_techhub" }
            activeShopId = sid
            viewModel.repository.getShopPromotions(sid, includeInactive = true).data?.let { promos = it }
            viewModel.repository.getFlashSales(activeOnly = false).data?.let { list ->
                flashSales = list.filter { it.shopId == sid }
            }
            viewModel.repository.getSellerProducts().data?.let { list ->
                shopProducts = list
                if (flashProduct == null) flashProduct = list.firstOrNull()
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    SuqiScreen(
        title = strings.seller.promotionsTitle,
        subtitle = "${promos.size} / ${flashSales.size}",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        headerActions = {
            IconButton(
                onClick = {
                    if (selectedTab == "COUPONS") {
                        promoStep = 0
                        showPromoWizard = true
                    } else {
                        flashProduct = shopProducts.firstOrNull()
                        flashPriceStr = flashProduct?.let { (it.price * 0.75).toInt().toString() }.orEmpty()
                        showFlashSheet = true
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SuqiPrimary)
                    .testTag("promotions_create_btn")
            ) {
                Icon(LucideIcons.Plus, contentDescription = strings.sellerOps.createPromoTitle, tint = Color.White)
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            SuqiTabs(
                tabs = listOf(
                    SuqiTabItem("COUPONS", p5.couponTicketsTab, promos.size),
                    SuqiTabItem("FLASH", p5.flashSalesTab, flashSales.size)
                ),
                selectedKey = selectedTab,
                onSelectTab = { selectedTab = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (isLoading && promos.isEmpty() && flashSales.isEmpty()) {
                SuqiSkeletonList(count = 3, modifier = Modifier.padding(top = 12.dp))
            } else if (selectedTab == "COUPONS") {
                if (promos.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        SuqiEmptyState(
                            icon = LucideIcons.TicketPercent,
                            title = strings.sellerOps.noPromosYetTitle,
                            subtitle = strings.sellerOps.noPromosYetSub,
                            ctaText = strings.sellerOps.createPromoBtn,
                            onCtaClick = { showPromoWizard = true },
                            testTag = "promotions_empty"
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(promos, key = { it.id }) { promo ->
                            val discLbl = when (promo.discountType.uppercase()) {
                                "FREE_DELIVERY" -> p5.freeDeliveryOption
                                "PERCENTAGE" -> "${promo.discountValue.toInt()}% OFF"
                                else -> formatEtb(promo.discountValue)
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                SuqiCouponTicket(
                                    code = promo.code,
                                    discountLabel = discLbl,
                                    title = promo.scopeType,
                                    conditions = "${String.format(p5.usesCountTemplate, promo.usesCount, promo.ordersCount)} • Min ${formatEtb(promo.minOrderAmount)}",
                                    expiry = promo.validUntil,
                                    statusLabel = if (promo.isActive) strings.seller.statusActive else strings.seller.statusPaused,
                                    isExpired = !promo.isActive,
                                    primaryActionLabel = if (promo.isActive) strings.seller.statusPaused else strings.seller.statusActive,
                                    onCopy = {
                                        clipboard.setText(AnnotatedString(promo.code))
                                        viewModel.showToast(strings.common.copiedToast)
                                    },
                                    onPrimaryAction = {
                                        coroutineScope.launch {
                                            viewModel.repository.createPromoCode(promo.copy(isActive = !promo.isActive))
                                            reload()
                                        }
                                    }
                                )
                                if (promo.maxUsesTotal > 0) {
                                    LinearProgressIndicator(
                                        progress = { (promo.usesCount.toFloat() / promo.maxUsesTotal.toFloat()).coerceIn(0f, 1f) },
                                        color = SuqiPrimary,
                                        trackColor = colors.secondaryBackground,
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                                    )
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                viewModel.repository.deletePromoCode(promo.id)
                                                reload()
                                            }
                                        },
                                        modifier = Modifier.size(48.dp).testTag("delete_promo_${promo.id}")
                                    ) {
                                        Icon(LucideIcons.Trash2, contentDescription = strings.deleteLabel, tint = SuqiError)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (flashSales.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        SuqiEmptyState(
                            icon = LucideIcons.Zap,
                            title = p5.noFlashSalesTitle,
                            subtitle = p5.flashDealsHeaderTitle,
                            ctaText = p5.createFlashSaleBtn,
                            onCtaClick = { showFlashSheet = true },
                            testTag = "flash_sales_empty"
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(flashSales, key = { it.id }) { sale ->
                            SuqiSectionCard(padding = 14.dp, modifier = Modifier.testTag("seller_flash_card_${sale.id}")) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(sale.productName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primaryText)
                                        Text("-${sale.discountPercent}% • ${formatEtb(sale.salePrice)} (${sale.claimedCount}/${sale.stockCap})", fontSize = 12.sp, color = SuqiPrimary, fontWeight = FontWeight.SemiBold)
                                    }
                                    Switch(
                                        checked = sale.isActive,
                                        onCheckedChange = { next ->
                                            coroutineScope.launch {
                                                viewModel.repository.saveFlashSale(sale.copy(isActive = next))
                                                reload()
                                            }
                                        }
                                    )
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                viewModel.repository.deleteFlashSale(sale.id)
                                                reload()
                                            }
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) { Icon(LucideIcons.Trash2, contentDescription = strings.deleteLabel, tint = SuqiError) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPromoWizard) {
        SuqiBottomSheet(onDismissRequest = { showPromoWizard = false }, title = strings.sellerOps.createPromoTitle) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SuqiStepIndicator(steps = listOf(p5.createPromoStepBasics, p5.createPromoStepScope, p5.createPromoStepLimits), currentStep = promoStep)
                when (promoStep) {
                    0 -> {
                        SuqiTextField(value = codeInput, onValueChange = { codeInput = it.uppercase() }, label = strings.sellerOps.promoCodeNameLabel, testTag = "promo_wizard_code_input")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("PERCENTAGE" to strings.sellerOps.percentageTypeLabel, "FIXED" to strings.sellerOps.fixedTypeLabel, "FREE_DELIVERY" to p5.freeDeliveryOption).forEach { (type, lbl) ->
                                val sel = discountType == type
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (sel) SuqiPrimary else colors.secondaryBackground)
                                        .clickable { discountType = type }
                                        .padding(6.dp)
                                        .testTag("promo_type_$type")
                                ) {
                                    Text(lbl, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (sel) Color.White else colors.primaryText)
                                }
                            }
                        }
                        if (discountType != "FREE_DELIVERY") {
                            SuqiTextField(value = discountValStr, onValueChange = { discountValStr = it }, label = strings.discountLabel, testTag = "promo_wizard_val_input")
                        }
                        SuqiTextField(value = minOrderStr, onValueChange = { minOrderStr = it }, label = strings.sellerOps.minOrderEtbLabel)
                    }
                    1 -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("SHOP" to p5.scopeEntireShop, "CATEGORY" to p5.scopeCategories, "PRODUCTS" to p5.scopeProducts).forEach { (sc, lbl) ->
                                val sel = scopeType == sc
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (sel) SuqiPrimary else colors.secondaryBackground)
                                        .clickable {
                                            scopeType = sc
                                            selectedScopeIds.clear()
                                        }
                                        .padding(6.dp)
                                ) {
                                    Text(lbl, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (sel) Color.White else colors.primaryText)
                                }
                            }
                        }
                        if (scopeType == "PRODUCTS") {
                            shopProducts.take(6).forEach { prod ->
                                val checked = prod.id in selectedScopeIds
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { if (checked) selectedScopeIds.remove(prod.id) else selectedScopeIds.add(prod.id) }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Checkbox(checked = checked, onCheckedChange = { if (it) selectedScopeIds.add(prod.id) else selectedScopeIds.remove(prod.id) })
                                    Text(prod.name, fontSize = 13.sp, color = colors.primaryText)
                                }
                            }
                        }
                    }
                    else -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SuqiTextField(value = maxUsesStr, onValueChange = { maxUsesStr = it }, label = p5.maxUsesTotalLabel, modifier = Modifier.weight(1f))
                            SuqiTextField(value = perCustomerStr, onValueChange = { perCustomerStr = it }, label = p5.perCustomerLimitLabel, modifier = Modifier.weight(1f))
                        }
                        SuqiDateField(label = strings.sellerOps.expiryDateLabel, dateText = endDate, onDateSelected = { endDate = it })
                        Text(p5.couponPreviewLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.secondaryText)
                        SuqiCouponTicket(
                            code = codeInput.ifBlank { "SUQI15" },
                            discountLabel = if (discountType == "FREE_DELIVERY") p5.freeDeliveryOption else "${discountValStr}%",
                            title = scopeType,
                            conditions = "Min ${formatEtb(minOrderStr.toDoubleOrNull() ?: 0.0)}",
                            expiry = endDate,
                            onCopy = {},
                            onPrimaryAction = {}
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    if (promoStep > 0) {
                        OutlinedButton(onClick = { promoStep -= 1 }, shape = RoundedCornerShape(999.dp), modifier = Modifier.weight(1f).height(48.dp)) {
                            Text(strings.common.backLabel)
                        }
                    }
                    if (promoStep < 2) {
                        SuqiButton(
                            text = strings.buyer.nextLabel,
                            isPill = true,
                            onClick = { promoStep += 1 },
                            testTag = "promo_wizard_next_btn",
                            modifier = Modifier.weight(1.3f)
                        )
                    } else {
                        SuqiButton(
                            text = strings.sellerOps.createPromoBtn,
                            isPill = true,
                            onClick = {
                                val clean = codeInput.trim().uppercase()
                                if (clean.isBlank()) return@SuqiButton
                                coroutineScope.launch {
                                    viewModel.repository.createPromoCode(
                                        PromoCodeDto(
                                            id = "promo_${System.currentTimeMillis()}",
                                            shopId = activeShopId,
                                            code = clean,
                                            discountType = discountType,
                                            discountValue = if (discountType == "FREE_DELIVERY") 0.0 else (discountValStr.toDoubleOrNull() ?: 10.0),
                                            minOrderAmount = minOrderStr.toDoubleOrNull() ?: 0.0,
                                            validUntil = endDate,
                                            isActive = true,
                                            scopeType = scopeType,
                                            scopeTargetIds = selectedScopeIds.toList(),
                                            maxUsesTotal = maxUsesStr.toIntOrNull() ?: 0,
                                            perCustomerLimit = perCustomerStr.toIntOrNull() ?: 1,
                                            startDate = startDate
                                        )
                                    )
                                    showPromoWizard = false
                                    reload()
                                }
                            },
                            testTag = "promo_wizard_submit_btn",
                            modifier = Modifier.weight(1.3f)
                        )
                    }
                }
            }
        }
    }

    if (showFlashSheet) {
        SuqiBottomSheet(onDismissRequest = { showFlashSheet = false }, title = p5.createFlashSaleBtn) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                shopProducts.take(5).forEach { prod ->
                    val sel = flashProduct?.id == prod.id
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (sel) colors.primaryTint else colors.secondaryBackground)
                            .border(1.dp, if (sel) SuqiPrimary else colors.border, RoundedCornerShape(12.dp))
                            .clickable {
                                flashProduct = prod
                                flashPriceStr = (prod.price * 0.75).toInt().toString()
                            }
                            .padding(10.dp)
                    ) {
                        Text(prod.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.primaryText, modifier = Modifier.weight(1f))
                        Text(formatEtb(prod.price), fontSize = 12.sp, color = SuqiPrimary)
                    }
                }
                SuqiTextField(value = flashPriceStr, onValueChange = { flashPriceStr = it }, label = p5.flashSalePriceLabel, testTag = "flash_price_input")
                SuqiTextField(value = flashStockCapStr, onValueChange = { flashStockCapStr = it }, label = p5.flashSaleStockCapLabel)
                SuqiButton(
                    text = p5.createFlashSaleBtn,
                    isPill = true,
                    onClick = {
                        val prod = flashProduct ?: return@SuqiButton
                        val saleP = flashPriceStr.toDoubleOrNull() ?: 0.0
                        coroutineScope.launch {
                            val res = viewModel.repository.saveFlashSale(
                                FlashSaleDto(
                                    id = "",
                                    shopId = activeShopId,
                                    shopName = prod.shopName,
                                    productId = prod.id,
                                    productName = prod.name,
                                    productImageUrl = prod.primaryImageUrl,
                                    categoryId = prod.categoryId,
                                    originalPrice = prod.price,
                                    salePrice = saleP,
                                    discountPercent = 0,
                                    stockCap = flashStockCapStr.toIntOrNull() ?: 10
                                )
                            )
                            if (res.success) {
                                showFlashSheet = false
                                reload()
                            } else {
                                viewModel.showToast(viewModel.localizeMessage(res.message ?: strings.retryLabel))
                            }
                        }
                    },
                    testTag = "submit_flash_sale_btn",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
