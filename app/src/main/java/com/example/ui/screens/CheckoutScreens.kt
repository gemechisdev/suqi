package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.SavedAddressDto
import com.example.data.model.ShopDeliveryOptionDto
import com.example.data.model.ShopDto
import com.example.data.model.ShopPaymentMethodDto
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.*
import com.example.ui.i18n.suqiStrings
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@Composable
private fun CheckoutStepIndicator(currentStep: Int) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val steps = listOf(
        1 to strings.checkout.stepDelivery,
        2 to strings.checkout.stepPayment,
        3 to strings.checkout.stepDone
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("checkout_step_indicator")
    ) {
        steps.forEachIndexed { idx, (stepNum, label) ->
            val completed = stepNum < currentStep
            val active = stepNum == currentStep
            val bg = when {
                completed -> SuqiSuccess
                active -> SuqiPrimary
                else -> colors.secondaryBackground
            }
            val fg = if (completed || active) Color.White else colors.mutedText

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(bg)
                        .border(1.dp, if (completed || active) Color.Transparent else colors.border, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (completed) {
                        Icon(
                            imageVector = LucideIcons.Check,
                            contentDescription = strings.extras.completedDesc,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Text(
                            text = stepNum.toString(),
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = fg
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    fontFamily = SuqiFontFamily,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp,
                    color = if (active) SuqiPrimary else if (completed) SuqiSuccess else colors.secondaryText
                )
            }
            if (idx < steps.lastIndex) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .height(2.dp)
                        .background(if (stepNum < currentStep) SuqiSuccess else colors.border)
                )
            }
        }
    }
}

@Composable
fun CheckoutDeliveryScreen(
    shopId: String,
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val strings = com.example.ui.i18n.suqiStrings()
    val isOffline by viewModel.isOffline.collectAsState()

    var deliveryOptions by remember { mutableStateOf<List<ShopDeliveryOptionDto>>(emptyList()) }
    var selectedOption by remember { mutableStateOf<ShopDeliveryOptionDto?>(null) }
    var savedAddresses by remember { mutableStateOf<List<SavedAddressDto>>(emptyList()) }
    var showAddressDialog by remember { mutableStateOf(false) }

    val recipientName by viewModel.activeCheckoutRecipientName.collectAsState()
    val recipientPhone by viewModel.activeCheckoutRecipientPhone.collectAsState()
    val deliveryAddress by viewModel.activeCheckoutDeliveryAddress.collectAsState()
    val items = viewModel.activeCheckoutItems.value
    val promo = viewModel.activeCheckoutShopPromoCode.value

    val subtotal = items.sumOf { it.unitPrice * it.quantity }
    val discount = if (promo != null) {
        if (promo.discountType == "PERCENTAGE") subtotal * (promo.discountValue / 100.0) else promo.discountValue
    } else 0.0

    LaunchedEffect(shopId) {
        val res = viewModel.repository.getShopDeliveryOptions(shopId)
        if (res.success && res.data != null && res.data.isNotEmpty()) {
            deliveryOptions = res.data
            selectedOption = res.data.first()
            viewModel.activeCheckoutDeliveryOption.value = res.data.first()
        }
        val addrRes = viewModel.repository.getSavedAddresses()
        if (addrRes.success && addrRes.data != null) {
            savedAddresses = addrRes.data
            if (deliveryAddress.isBlank() && addrRes.data.isNotEmpty()) {
                val def = addrRes.data.firstOrNull { it.isDefault } ?: addrRes.data.first()
                viewModel.activeCheckoutRecipientName.value = def.fullName
                viewModel.activeCheckoutRecipientPhone.value = def.phone
                viewModel.activeCheckoutDeliveryAddress.value = "${def.streetOrLandmark}, ${def.subCity}, ${def.city}"
            }
        }
    }

    val deliveryFee = selectedOption?.fee ?: 50.0
    val totalAmount = maxOf(0.0, subtotal + deliveryFee - discount)

    SuqiScreen(
        title = strings.checkoutDeliveryTitle,
        subtitle = viewModel.activeCheckoutShopName.value,
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        onRetryOffline = { viewModel.setOfflineMode(false) },
        bottomBar = {
            Surface(
                color = colors.cardBackground,
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.totalLabel,
                            fontFamily = SuqiFontFamily,
                            fontSize = 13.sp,
                            color = colors.secondaryText
                        )
                        Text(
                            text = formatEtb(totalAmount),
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = SuqiPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    SuqiButton(
                        text = strings.continueToPayment,
                        onClick = {
                            if (recipientName.isBlank() || recipientPhone.isBlank() || deliveryAddress.isBlank()) {
                                viewModel.showToast(strings.checkout.selectAddressToast)
                                showAddressDialog = true
                                return@SuqiButton
                            }
                            viewModel.activeCheckoutDeliveryOption.value = selectedOption
                            viewModel.navigate(ScreenDestination.CheckoutPayment(shopId))
                        },
                        isPill = true,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "btn_continue_to_payment"
                    )
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { CheckoutStepIndicator(currentStep = 1) }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = strings.checkoutDeliveryTitle,
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = colors.primaryText
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SuqiSectionCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(LucideIcons.User, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = recipientName.ifBlank { strings.buyer.recipientNameField },
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = colors.primaryText
                                )
                                Text(
                                    text = recipientPhone.ifBlank { strings.buyer.phoneNumberLabel },
                                    fontFamily = SuqiFontFamily,
                                    fontSize = 13.sp,
                                    color = colors.secondaryText
                                )
                            }
                            TextButton(onClick = { showAddressDialog = true }) {
                                Text(strings.editLabel, color = SuqiPrimary, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = colors.border, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(LucideIcons.MapPin, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = deliveryAddress.ifBlank { strings.checkout.noAddressSelected },
                                fontFamily = SuqiFontFamily,
                                fontSize = 14.sp,
                                color = colors.primaryText,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = { showAddressDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_choose_address")
                        ) {
                            Icon(LucideIcons.MapPinPlus, contentDescription = null, tint = colors.primaryText, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.checkout.selectAddressTitle, fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.primaryText)
                        }
                    }
                }
            }

            item {
                Text(
                    text = strings.checkout.deliveryOptionTitle,
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = colors.primaryText,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            items(deliveryOptions) { option ->
                val isSel = selectedOption?.id == option.id
                val optionDisplayTitle = when (option.optionType) {
                    "EXPRESS" -> strings.checkout.expressDeliveryTitle
                    "PICKUP" -> strings.checkout.pickupDeliveryTitle
                    else -> strings.checkout.standardDeliveryTitle
                }
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SuqiSelectionCard(
                        selected = isSel,
                        title = optionDisplayTitle,
                        subtitle = "${option.estimatedTime} • ${if (option.fee <= 0.0) strings.checkout.freeLabel else formatEtb(option.fee)}",
                        icon = when (option.optionType) {
                            "EXPRESS" -> LucideIcons.Zap
                            "PICKUP" -> LucideIcons.Store
                            else -> LucideIcons.Truck
                        },
                        onClick = { selectedOption = option }
                    )
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = strings.checkout.paymentSummaryTitle,
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = colors.primaryText
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SuqiSectionCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${strings.productsTab} ${String.format(strings.checkout.itemCountPluralTemplate, items.sumOf { it.quantity })}", fontFamily = SuqiFontFamily, color = colors.secondaryText)
                            Text(formatEtb(subtotal), fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, color = colors.primaryText)
                        }

                        if (discount > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${strings.discountLabel} (${promo?.code})", fontFamily = SuqiFontFamily, color = SuqiSuccess)
                                Text("-${formatEtb(discount)}", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, color = SuqiSuccess)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(strings.deliveryFeeLabel, fontFamily = SuqiFontFamily, color = colors.secondaryText)
                            Text(formatEtb(deliveryFee), fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, color = colors.primaryText)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = colors.border, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(strings.totalLabel, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = colors.primaryText)
                            Text(
                                formatEtb(totalAmount),
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = SuqiPrimary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddressDialog) {
        var selectedMode by remember { mutableIntStateOf(if (savedAddresses.isNotEmpty()) 0 else 1) }
        var tempName by remember { mutableStateOf(recipientName) }
        var tempPhone by remember { mutableStateOf(recipientPhone) }
        var tempAddress by remember { mutableStateOf(deliveryAddress) }

        SuqiBottomSheet(
            onDismissRequest = { showAddressDialog = false },
            title = strings.checkout.selectAddressTitle
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.secondaryBackground)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(strings.savedAddresses, strings.checkout.addAddressBtn).forEachIndexed { idx, label ->
                    val selected = selectedMode == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) SuqiPrimary else Color.Transparent)
                            .clickable { selectedMode = idx }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            maxLines = 2,
                            textAlign = TextAlign.Center,
                            color = if (selected) Color.White else colors.primaryText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (selectedMode == 0) {
                if (savedAddresses.isEmpty()) {
                    SuqiEmptyState(
                        icon = LucideIcons.MapPin,
                        title = strings.buyer.noAddressesTitle,
                        subtitle = strings.buyer.noAddressesSub,
                        ctaText = strings.checkout.addAddressBtn,
                        onCtaClick = { selectedMode = 1 }
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        savedAddresses.forEach { addr ->
                            val formatted = "${addr.streetOrLandmark}, ${addr.subCity}, ${addr.city}"
                            val isCurrent = deliveryAddress == formatted
                            SuqiSectionCard(
                                containerColor = if (isCurrent) colors.primaryTint else colors.secondaryBackground,
                                onClick = {
                                    viewModel.activeCheckoutRecipientName.value = addr.fullName
                                    viewModel.activeCheckoutRecipientPhone.value = addr.phone
                                    viewModel.activeCheckoutDeliveryAddress.value = formatted
                                    showAddressDialog = false
                                }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(addr.fullName, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primaryText)
                                    if (addr.isDefault) SuqiStatusBadge(strings.buyer.defaultBadge, StatusType.SUCCESS)
                                }
                                Text(addr.phone, fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.secondaryText)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(formatted, fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.primaryText)
                            }
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SuqiTextField(value = tempName, onValueChange = { tempName = it }, label = strings.buyer.fullNameLabel)
                    SuqiTextField(value = tempPhone, onValueChange = { tempPhone = it }, label = strings.buyer.phoneNumberLabel)
                    SuqiTextField(value = tempAddress, onValueChange = { tempAddress = it }, label = strings.buyer.landmarkField)
                    Spacer(modifier = Modifier.height(8.dp))
                    SuqiButton(
                        text = strings.confirmLabel,
                        isPill = true,
                        onClick = {
                            viewModel.activeCheckoutRecipientName.value = tempName.trim()
                            viewModel.activeCheckoutRecipientPhone.value = tempPhone.trim()
                            viewModel.activeCheckoutDeliveryAddress.value = tempAddress.trim()
                            showAddressDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * B-06 (P0): Checkout Payment Screen with Shop-Tint Pay-To card, 28sp amount,
 * Copy pill, PickVisualMedia Receipt Screenshot Upload, and enforcement of
 * shop.requireReceiptScreenshot.
 */
@Composable
fun CheckoutPaymentScreen(
    shopId: String,
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val colors = LocalSuqiColors.current
    val strings = com.example.ui.i18n.suqiStrings()
    val isOffline by viewModel.isOffline.collectAsState()

    var shopDetails by remember { mutableStateOf<ShopDto?>(null) }
    var paymentMethods by remember { mutableStateOf<List<ShopPaymentMethodDto>>(emptyList()) }
    var selectedMethod by remember { mutableStateOf<ShopPaymentMethodDto?>(null) }
    var transactionReference by remember { mutableStateOf("") }
    var payerIdentifier by remember { mutableStateOf("") }
    var receiptUri by remember { mutableStateOf(viewModel.activeCheckoutReceiptUri.value) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val receiptPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            receiptUri = uri.toString()
            viewModel.activeCheckoutReceiptUri.value = receiptUri
            errorMessage = null
        }
    }

    val items = viewModel.activeCheckoutItems.value
    val promo = viewModel.activeCheckoutShopPromoCode.value
    val deliveryOption = viewModel.activeCheckoutDeliveryOption.value
    val recipientName by viewModel.activeCheckoutRecipientName.collectAsState()
    val recipientPhone by viewModel.activeCheckoutRecipientPhone.collectAsState()
    val deliveryAddress by viewModel.activeCheckoutDeliveryAddress.collectAsState()

    val subtotal = items.sumOf { it.unitPrice * it.quantity }
    val discount = if (promo != null) {
        if (promo.discountType == "PERCENTAGE") subtotal * (promo.discountValue / 100.0) else promo.discountValue
    } else 0.0
    val deliveryFee = deliveryOption?.fee ?: 50.0
    val totalAmount = maxOf(0.0, subtotal + deliveryFee - discount)

    val orderRefPreview = remember { "SUQ-2025-${(100000..999999).random()}" }

    LaunchedEffect(shopId) {
        val shopRes = viewModel.repository.getShopDetails(shopId)
        if (shopRes.success && shopRes.data != null) {
            shopDetails = shopRes.data
        }
        val res = viewModel.repository.getShopPaymentMethods(shopId)
        if (res.success && res.data != null && res.data.isNotEmpty()) {
            paymentMethods = res.data
            selectedMethod = res.data.first()
        }
    }

    val isReceiptRequired = shopDetails?.requireReceiptScreenshot == true

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        viewModel.showToast(strings.checkout.accountCopiedToast)
    }

    SuqiScreen(
        title = strings.checkoutPaymentTitle,
        subtitle = viewModel.activeCheckoutShopName.value,
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        onRetryOffline = { viewModel.setOfflineMode(false) },
        bottomBar = {
            Surface(
                color = colors.cardBackground,
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = {
                            if (transactionReference.isBlank()) {
                                errorMessage = strings.checkout.enterTxRefToast
                                return@Button
                            }
                            if (!com.example.util.EthiopianMarketplaceUtils.isValidPaymentReference(transactionReference)) {
                                errorMessage = strings.checkout.enterTxRefToast
                                return@Button
                            }
                            if (isReceiptRequired && receiptUri.isBlank()) {
                                errorMessage = strings.checkout.uploadReceiptToast
                                return@Button
                            }
                            if (selectedMethod == null) {
                                errorMessage = strings.checkout.paymentMethodTitle
                                return@Button
                            }

                            isSubmitting = true
                            errorMessage = null

                            viewModel.activeCheckoutTransactionRef.value = transactionReference.trim()
                            viewModel.activeCheckoutPayerIdentifier.value = payerIdentifier.trim()
                            viewModel.activeCheckoutReceiptUri.value = receiptUri
                            viewModel.activeCheckoutPaymentMethod.value = selectedMethod
                            viewModel.activeCheckoutTotalAmount.value = totalAmount

                            coroutineScope.launch {
                                val itemsJsonArr = JSONArray()
                                items.forEach { item ->
                                    val obj = JSONObject()
                                    obj.put("productId", item.productId)
                                    obj.put("productName", item.productName)
                                    obj.put("productImageUrl", item.productImageUrl)
                                    obj.put("variation", item.selectedVariation)
                                    obj.put("unitPrice", item.unitPrice)
                                    obj.put("quantity", item.quantity)
                                    obj.put("totalPrice", item.unitPrice * item.quantity)
                                    itemsJsonArr.put(obj)
                                }

                                val payload = mapOf(
                                    "shopId" to shopId,
                                    "shopName" to viewModel.activeCheckoutShopName.value,
                                    "customerName" to recipientName,
                                    "customerPhone" to recipientPhone,
                                    "deliveryAddress" to deliveryAddress,
                                    "subtotal" to subtotal,
                                    "deliveryFee" to deliveryFee,
                                    "discountAmount" to discount,
                                    "promoCode" to (promo?.code ?: ""),
                                    "deliveryOptionTitle" to (deliveryOption?.title ?: "Standard Delivery"),
                                    "deliveryOptionType" to (deliveryOption?.optionType ?: "STANDARD"),
                                    "deliveryEstimatedDays" to (deliveryOption?.estimatedTime ?: "1-3 days"),
                                    "paymentMethodTitle" to (selectedMethod?.bankName ?: "Telebirr"),
                                    "paymentMethodType" to (selectedMethod?.providerType ?: "TELEBIRR"),
                                    "paymentMethodAccount" to (selectedMethod?.accountNumber ?: ""),
                                    "transactionReference" to transactionReference.trim(),
                                    "payerIdentifier" to payerIdentifier.trim(),
                                    "receiptImageUri" to receiptUri,
                                    "itemsJson" to itemsJsonArr.toString()
                                )

                                val res = viewModel.repository.submitOrder(payload)
                                isSubmitting = false
                                if (res.success && res.data != null) {
                                    viewModel.activeCheckoutReceiptUri.value = ""
                                    viewModel.navigate(
                                        ScreenDestination.OrderSubmitted(
                                            orderReference = res.data.orderReference,
                                            orderId = res.data.id
                                        )
                                    )
                                } else {
                                    errorMessage = viewModel.localizeMessage(res.message ?: strings.extras.orderSubmitFailed)
                                }
                            }
                        },
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_submit_order")
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                        } else {
                            Text(
                                text = strings.placeOrder,
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { CheckoutStepIndicator(currentStep = 2) }

            // Shop-tint Pay-To Card with Copy & 28sp orange total
            item {
                SuqiSectionCard(
                    containerColor = colors.shopTint,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .border(1.dp, colors.shopBorder, RoundedCornerShape(SuqiRadius.Card))
                        .testTag("checkout_pay_to_card")
                ) {
                    Text(
                        text = strings.checkout.transferAmountLabel,
                        fontFamily = SuqiFontFamily,
                        fontSize = 12.sp,
                        color = colors.secondaryText
                    )
                    Text(
                        text = formatEtb(totalAmount),
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 28.sp,
                        color = SuqiPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${strings.checkout.txRefLabel}: $orderRefPreview",
                        fontFamily = SuqiFontFamily,
                        fontSize = 12.sp,
                        color = colors.secondaryText
                    )

                    if (selectedMethod != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = colors.shopBorder)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedMethod!!.bankName,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = colors.primaryText
                                )
                                Text(
                                    text = "${selectedMethod!!.accountName} • ${selectedMethod!!.accountNumber}",
                                    fontFamily = SuqiFontFamily,
                                    fontSize = 13.sp,
                                    color = colors.secondaryText
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = {
                                    copyToClipboard("Account Number", selectedMethod!!.accountNumber)
                                },
                                shape = RoundedCornerShape(999.dp),
                                border = BorderStroke(1.dp, SuqiPrimary),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("btn_copy_account")
                            ) {
                                Icon(LucideIcons.Copy, contentDescription = strings.common.copyLabel, tint = SuqiPrimary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.common.copyLabel, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SuqiPrimary)
                            }
                        }
                    }
                }
            }

            // Payment Method List
            item {
                Text(
                    text = strings.checkout.paymentMethodTitle,
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = colors.primaryText,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            items(paymentMethods) { method ->
                val isSel = selectedMethod?.id == method.id
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SuqiSelectionCard(
                        selected = isSel,
                        title = method.bankName,
                        subtitle = "${method.accountName} • ${method.accountNumber}",
                        icon = when (method.providerType) {
                            "BANK_TRANSFER" -> LucideIcons.Landmark
                            "TELEBIRR" -> LucideIcons.Smartphone
                            else -> LucideIcons.Wallet
                        },
                        onClick = { selectedMethod = method }
                    )
                }
            }

            // Transaction Ref & Payer Identifier Card
            item {
                SuqiSectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = strings.checkout.txRefRequiredLabel,
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = colors.primaryText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SuqiTextField(
                        value = transactionReference,
                        onValueChange = {
                            transactionReference = it
                            errorMessage = null
                        },
                        label = "",
                        placeholder = "TXN8492041",
                        isError = errorMessage != null && transactionReference.isBlank(),
                        testTag = "input_transaction_reference"
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = strings.checkout.senderPhoneLabel,
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = colors.primaryText
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    SuqiTextField(
                        value = payerIdentifier,
                        onValueChange = { payerIdentifier = it },
                        label = "",
                        placeholder = "0911234567",
                        testTag = "input_payer_identifier"
                    )
                }
            }

            // B-06 Receipt Screenshot Proof Drop-Zone
            item {
                SuqiSectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.checkout.receiptScreenshotRequired,
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = colors.primaryText
                            )
                            Text(
                                text = strings.checkout.receiptRequiredHint,
                                fontFamily = SuqiFontFamily,
                                fontSize = 12.sp,
                                color = if (isReceiptRequired) SuqiPrimary else colors.secondaryText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (receiptUri.isBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(colors.secondaryBackground)
                                .border(1.5.dp, if (isReceiptRequired) SuqiPrimary else colors.border, RoundedCornerShape(18.dp))
                                .clickable {
                                    receiptPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .testTag("btn_upload_receipt"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = LucideIcons.Upload,
                                    contentDescription = strings.uploadReceipt,
                                    tint = SuqiPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = strings.common.tapToPickPhoto,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = colors.primaryText
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(168.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(colors.imageBackdrop)
                                .border(1.dp, SuqiSuccess, RoundedCornerShape(18.dp))
                                .testTag("receipt_preview_box")
                        ) {
                            AsyncImage(
                                model = receiptUri,
                                contentDescription = strings.uploadReceipt,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = colors.cardBackground.copy(alpha = 0.92f),
                                    modifier = Modifier.clickable {
                                        receiptPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                ) {
                                    Text(
                                        text = strings.seller.replaceLabel,
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = colors.primaryText,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = SuqiError,
                                    modifier = Modifier.clickable {
                                        receiptUri = ""
                                        viewModel.activeCheckoutReceiptUri.value = ""
                                    }
                                ) {
                                    Icon(
                                        imageVector = LucideIcons.Trash2,
                                        contentDescription = strings.extras.removeReceiptDesc,
                                        tint = Color.White,
                                        modifier = Modifier.padding(6.dp).size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (!errorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = errorMessage.orEmpty(),
                            fontFamily = SuqiFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = SuqiError,
                            modifier = Modifier.testTag("checkout_payment_error")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OrderSubmittedScreen(
    orderReference: String,
    orderId: String,
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()

    val shopName by viewModel.activeCheckoutShopName.collectAsState()
    val totalAmount by viewModel.activeCheckoutTotalAmount.collectAsState()
    val deliveryAddress by viewModel.activeCheckoutDeliveryAddress.collectAsState()
    val recipientName by viewModel.activeCheckoutRecipientName.collectAsState()
    val recipientPhone by viewModel.activeCheckoutRecipientPhone.collectAsState()
    val paymentMethod by viewModel.activeCheckoutPaymentMethod.collectAsState()
    val transactionRef by viewModel.activeCheckoutTransactionRef.collectAsState()
    val payerIdentifier by viewModel.activeCheckoutPayerIdentifier.collectAsState()

    SuqiScreen(
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                CheckoutStepIndicator(currentStep = 3)
            }

            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(colors.successBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(SuqiSuccess),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = LucideIcons.Check,
                                contentDescription = strings.orderSubmittedTitle,
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = strings.orderSubmittedTitle,
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = colors.primaryText
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.checkout.orderSubmittedSubtitle,
                        fontFamily = SuqiFontFamily,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = colors.secondaryText,
                        textAlign = TextAlign.Center
                    )
                }
            }

            item {
                SuqiSectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(strings.checkout.txRefLabel, fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.secondaryText)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(orderReference, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Order Ref", orderReference))
                                        viewModel.showToast(strings.common.copiedToast)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(LucideIcons.Copy, contentDescription = strings.common.copyLabel, tint = SuqiPrimary, modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                        SuqiStatusBadge(strings.checkout.statusPendingVerif, StatusType.WARNING)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = colors.border)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(strings.shopsTab, fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.secondaryText)
                        Text(shopName.ifBlank { strings.verifiedLabel }, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.primaryText)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(strings.checkout.totalAmountLabel, fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.secondaryText)
                        Text(formatEtb(totalAmount), fontFamily = SuqiFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = SuqiPrimary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(strings.checkout.paymentMethodTitle, fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.secondaryText)
                        Text(paymentMethod?.bankName ?: strings.checkout.telebirrLabel, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = colors.primaryText)
                    }
                    if (transactionRef.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(strings.checkout.txRefLabel, fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.secondaryText)
                            Text(transactionRef, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.primaryText)
                        }
                    }
                    if (payerIdentifier.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(strings.checkout.senderPhoneLabel, fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.secondaryText)
                            Text(payerIdentifier, fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.primaryText)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = colors.border)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.Top) {
                        Icon(LucideIcons.MapPin, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("$recipientName • $recipientPhone", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.primaryText)
                            Text(deliveryAddress, fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.secondaryText)
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    SuqiButton(
                        text = strings.checkout.viewDetailsBtn,
                        isPill = true,
                        onClick = { viewModel.navigate(ScreenDestination.OrderDetails(orderId)) },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "btn_view_order"
                    )
                    SuqiButton(
                        text = strings.checkout.continueShoppingBtn,
                        isPrimary = false,
                        isPill = true,
                        onClick = { viewModel.popToRoot(ScreenDestination.Home) },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "btn_continue_shopping"
                    )
                    TextButton(
                        onClick = { viewModel.navigate(ScreenDestination.ChatThread(viewModel.activeCheckoutShopId.value)) },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(LucideIcons.MessageCircle, contentDescription = null, tint = colors.secondaryText, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.chatWithSeller, fontFamily = SuqiFontFamily, fontSize = 14.sp, color = colors.secondaryText)
                    }
                }
            }
        }
    }
}
