package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CartItemDto
import com.example.data.model.SavedAddressDto
import com.example.data.model.ShopDeliveryOptionDto
import com.example.data.model.ShopPaymentMethodDto
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun CheckoutDeliveryScreen(
    shopId: String,
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

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
        }
    }

    val deliveryFee = selectedOption?.fee ?: 50.0
    val totalAmount = maxOf(0.0, subtotal + deliveryFee - discount)

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.goBack() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SuqiSecondaryBackground)
                ) {
                    Icon(LucideIcons.ArrowLeft, contentDescription = "Back", tint = SuqiPrimaryText)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Checkout",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    color = SuqiPrimaryText
                )
            }
        },
        bottomBar = {
            Surface(
                color = LocalSuqiColors.current.cardBackground,
                shadowElevation = 12.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, SuqiBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SuqiButton(
                        text = "Continue to Payment",
                        onClick = {
                            viewModel.activeCheckoutDeliveryOption.value = selectedOption
                            viewModel.navigate(ScreenDestination.CheckoutPayment(shopId))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "btn_continue_to_payment"
                    )
                }
            }
        },
        containerColor = SuqiBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Recipient & Delivery Details Card (Checklist 09 & Mockup 2)
            item {
                Text(
                    text = "Delivery Details",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = SuqiPrimaryText
                )
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SuqiSecondaryBackground),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(LucideIcons.User, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = recipientName,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = recipientPhone,
                                    fontFamily = SuqiFontFamily,
                                    fontSize = 13.sp,
                                    color = SuqiSecondaryText
                                )
                            }
                            TextButton(onClick = { showAddressDialog = true }) {
                                Text("Edit", color = SuqiPrimary, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = SuqiBorder, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(LucideIcons.MapPin, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = deliveryAddress,
                                fontFamily = SuqiFontFamily,
                                fontSize = 14.sp,
                                color = SuqiPrimaryText,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Choose another address button
                        OutlinedButton(
                            onClick = { showAddressDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SuqiBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(LucideIcons.MapPinPlus, contentDescription = null, tint = SuqiPrimaryText, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Choose Another Address", fontFamily = SuqiFontFamily, fontSize = 13.sp, color = SuqiPrimaryText)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Delivery Option Cards (Checklist 09)
            item {
                Text(
                    text = "Delivery Option (From ${viewModel.activeCheckoutShopName.value})",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = SuqiPrimaryText
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            items(deliveryOptions) { option ->
                val isSel = selectedOption?.id == option.id
                SuqiSelectionCard(
                    selected = isSel,
                    title = option.title,
                    subtitle = "${option.estimatedTime} • ${formatEtb(option.fee)}",
                    icon = when (option.optionType) {
                        "EXPRESS" -> LucideIcons.Zap
                        "PICKUP" -> LucideIcons.Store
                        else -> LucideIcons.Truck
                    },
                    onClick = { selectedOption = option }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Order Summary
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Order Summary",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = SuqiPrimaryText
                )
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SuqiSecondaryBackground),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Products (${items.sumOf { it.quantity }} items)", fontFamily = SuqiFontFamily, color = SuqiSecondaryText)
                            Text(formatEtb(subtotal), fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold)
                        }

                        if (discount > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Shop Discount (${promo?.code})", fontFamily = SuqiFontFamily, color = SuqiSuccess)
                                Text("-${formatEtb(discount)}", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, color = SuqiSuccess)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Delivery Fee", fontFamily = SuqiFontFamily, color = SuqiSecondaryText)
                            Text(formatEtb(deliveryFee), fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = SuqiBorder, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text(
                                formatEtb(totalAmount),
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.SemiBold,
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
        var selectedMode by remember { mutableIntStateOf(if (savedAddresses.isNotEmpty()) 0 else 1) } // 0: Saved, 1: Custom
        var tempName by remember { mutableStateOf(recipientName) }
        var tempPhone by remember { mutableStateOf(recipientPhone) }
        var tempAddress by remember { mutableStateOf(deliveryAddress) }

        AlertDialog(
            onDismissRequest = { showAddressDialog = false },
            title = {
                Text(
                    text = "Select Delivery Address",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    // Mode selector pills
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SuqiSecondaryBackground)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedMode == 0) SuqiPrimary else Color.Transparent)
                                .clickable { selectedMode = 0 }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Saved Addresses",
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = if (selectedMode == 0) Color.White else SuqiPrimaryText
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedMode == 1) SuqiPrimary else Color.Transparent)
                                .clickable { selectedMode = 1 }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Custom Address",
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = if (selectedMode == 1) Color.White else SuqiPrimaryText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (selectedMode == 0) {
                        if (savedAddresses.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No saved addresses found.\nPlease enter a custom address.",
                                    fontFamily = SuqiFontFamily,
                                    fontSize = 13.sp,
                                    color = SuqiSecondaryText,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(savedAddresses) { addr ->
                                    val isCurrent = deliveryAddress.contains(addr.streetOrLandmark)
                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isCurrent) LocalSuqiColors.current.primaryTint else SuqiSecondaryBackground
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isCurrent) SuqiPrimary else SuqiBorder
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.activeCheckoutRecipientName.value = addr.fullName
                                                viewModel.activeCheckoutRecipientPhone.value = addr.phone
                                                viewModel.activeCheckoutDeliveryAddress.value = "${addr.streetOrLandmark}, ${addr.subCity}, ${addr.city}"
                                                showAddressDialog = false
                                            }
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = addr.fullName,
                                                    fontFamily = SuqiFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                if (addr.isDefault) {
                                                    SuqiStatusBadge("Default", StatusType.SUCCESS)
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = addr.phone,
                                                fontFamily = SuqiFontFamily,
                                                fontSize = 12.sp,
                                                color = SuqiSecondaryText
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${addr.streetOrLandmark}, ${addr.subCity}, ${addr.city}",
                                                fontFamily = SuqiFontFamily,
                                                fontSize = 13.sp,
                                                color = SuqiPrimaryText
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SuqiTextField(value = tempName, onValueChange = { tempName = it }, label = "Full Name")
                            SuqiTextField(value = tempPhone, onValueChange = { tempPhone = it }, label = "Phone (+251)")
                            SuqiTextField(value = tempAddress, onValueChange = { tempAddress = it }, label = "Street & Sub-City")
                        }
                    }
                }
            },
            confirmButton = {
                if (selectedMode == 1) {
                    Button(
                        onClick = {
                            viewModel.activeCheckoutRecipientName.value = tempName
                            viewModel.activeCheckoutRecipientPhone.value = tempPhone
                            viewModel.activeCheckoutDeliveryAddress.value = tempAddress
                            showAddressDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Use This Address", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddressDialog = false }) {
                    Text("Close", fontFamily = SuqiFontFamily)
                }
            },
            containerColor = LocalSuqiColors.current.cardBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun CheckoutPaymentScreen(
    shopId: String,
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var paymentMethods by remember { mutableStateOf<List<ShopPaymentMethodDto>>(emptyList()) }
    var selectedMethod by remember { mutableStateOf<ShopPaymentMethodDto?>(null) }
    var transactionReference by remember { mutableStateOf("") }
    var payerIdentifier by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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

    // Proposed order reference to display in payment memo instructions
    val orderRefPreview = remember { "SUQ-2025-${(100000..999999).random()}" }

    LaunchedEffect(shopId) {
        val res = viewModel.repository.getShopPaymentMethods(shopId)
        if (res.success && res.data != null && res.data.isNotEmpty()) {
            paymentMethods = res.data
            selectedMethod = res.data.first()
        }
    }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        viewModel.showToast("Copied $label to clipboard!")
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.goBack() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SuqiSecondaryBackground)
                ) {
                    Icon(LucideIcons.ArrowLeft, contentDescription = "Back", tint = SuqiPrimaryText)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Payment",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    color = SuqiPrimaryText
                )
            }
        },
        bottomBar = {
            Surface(
                color = LocalSuqiColors.current.cardBackground,
                shadowElevation = 12.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, SuqiBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = {
                            if (transactionReference.isBlank()) {
                                errorMessage = "Please enter the transaction reference number"
                                return@Button
                            }
                            if (!com.example.util.EthiopianMarketplaceUtils.isValidPaymentReference(transactionReference)) {
                                errorMessage = "Invalid reference format. Please enter at least 6 alphanumeric characters from your bank receipt/SMS."
                                return@Button
                            }
                            if (selectedMethod == null) {
                                errorMessage = "Please select a payment method"
                                return@Button
                            }

                            isSubmitting = true
                            errorMessage = null

                            // Save checkout state for OrderSubmitted confirmation
                            viewModel.activeCheckoutTransactionRef.value = transactionReference.trim()
                            viewModel.activeCheckoutPayerIdentifier.value = payerIdentifier.trim()
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
                                    "itemsJson" to itemsJsonArr.toString()
                                )

                                val res = viewModel.repository.submitOrder(payload)
                                isSubmitting = false
                                if (res.success && res.data != null) {
                                    viewModel.navigate(
                                        ScreenDestination.OrderSubmitted(
                                            orderReference = res.data.orderReference,
                                            orderId = res.data.id
                                        )
                                    )
                                } else {
                                    errorMessage = res.message ?: "Failed to submit order"
                                }
                            }
                        },
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_submit_order")
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text(
                                text = "Submit Order",
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
        containerColor = SuqiBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Payment Methods Selection Cards (Checklist 10 & Mockup 2)
            item {
                Text(
                    text = "Payment Method (From ${viewModel.activeCheckoutShopName.value})",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = SuqiPrimaryText
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            items(paymentMethods) { method ->
                val isSel = selectedMethod?.id == method.id
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
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Payment Instructions Box with Copy Details button
            if (selectedMethod != null) {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = LocalSuqiColors.current.primaryTint),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(SuqiPrimary.copy(alpha = 0.3f))
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Payment Instructions",
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = SuqiPrimaryText
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "1. Transfer the exact amount: ${formatEtb(totalAmount)}",
                                fontFamily = SuqiFontFamily,
                                fontSize = 13.sp,
                                color = SuqiPrimaryText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "2. Use Order Reference in transfer memo: $orderRefPreview",
                                fontFamily = SuqiFontFamily,
                                fontSize = 13.sp,
                                color = SuqiPrimaryText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "3. Destination: ${selectedMethod!!.accountNumber} (${selectedMethod!!.accountName})",
                                fontFamily = SuqiFontFamily,
                                fontSize = 13.sp,
                                color = SuqiPrimaryText
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = {
                                    val copyText = "Amount: ${formatEtb(totalAmount)}\nOrder Ref: $orderRefPreview\nAccount: ${selectedMethod!!.accountNumber}\nName: ${selectedMethod!!.accountName}"
                                    copyToClipboard("Payment Details", copyText)
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SuqiPrimary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(LucideIcons.Copy, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Details", fontFamily = SuqiFontFamily, color = SuqiPrimary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // Transaction Reference Required Input & Payer Phone / Account
            item {
                Text(
                    text = "Transaction Reference *",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = SuqiPrimaryText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "After completing your transfer, enter the transaction reference shown in your banking or mobile money receipt.",
                    fontFamily = SuqiFontFamily,
                    fontSize = 12.sp,
                    color = SuqiSecondaryText
                )
                Spacer(modifier = Modifier.height(10.dp))

                SuqiTextField(
                    value = transactionReference,
                    onValueChange = {
                        transactionReference = it
                        errorMessage = null
                    },
                    label = "",
                    placeholder = "Enter your transaction reference number...",
                    isError = errorMessage != null,
                    errorMessage = errorMessage,
                    testTag = "input_transaction_reference"
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Payer Phone / Account Identifier (Optional)",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = SuqiPrimaryText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Phone number or account name from which the transfer was sent, helping the seller verify faster.",
                    fontFamily = SuqiFontFamily,
                    fontSize = 12.sp,
                    color = SuqiSecondaryText
                )
                Spacer(modifier = Modifier.height(10.dp))

                SuqiTextField(
                    value = payerIdentifier,
                    onValueChange = { payerIdentifier = it },
                    label = "",
                    placeholder = "e.g., 0911234567 or Fitsum Gebre",
                    testTag = "input_payer_identifier"
                )
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

    val shopName by viewModel.activeCheckoutShopName.collectAsState()
    val totalAmount by viewModel.activeCheckoutTotalAmount.collectAsState()
    val deliveryAddress by viewModel.activeCheckoutDeliveryAddress.collectAsState()
    val recipientName by viewModel.activeCheckoutRecipientName.collectAsState()
    val recipientPhone by viewModel.activeCheckoutRecipientPhone.collectAsState()
    val paymentMethod by viewModel.activeCheckoutPaymentMethod.collectAsState()
    val transactionRef by viewModel.activeCheckoutTransactionRef.collectAsState()
    val payerIdentifier by viewModel.activeCheckoutPayerIdentifier.collectAsState()

    Scaffold(
        containerColor = colors.surfaceBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))

                // Success Green Circle with Checkmark
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(SuqiSuccessBg),
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
                            contentDescription = "Success",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Order Submitted!",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 24.sp,
                    color = colors.primaryText
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Your order has been placed with $shopName.\nPlease wait for the seller to verify your payment.",
                    fontFamily = SuqiFontFamily,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = colors.secondaryText,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Detailed Confirmation Summary Card (Plan §6 & §19 Screen 11)
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Order Reference",
                                    fontFamily = SuqiFontFamily,
                                    fontSize = 12.sp,
                                    color = colors.secondaryText
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = orderReference,
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = colors.primaryText
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Order Ref", orderReference))
                                            viewModel.showToast("Order Reference copied!")
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(LucideIcons.Copy, contentDescription = "Copy", tint = SuqiPrimary, modifier = Modifier.size(15.dp))
                                    }
                                }
                            }

                            // Explicit status wording (Plan §6)
                            SuqiStatusBadge(
                                statusText = "Awaiting Payment Confirmation",
                                type = StatusType.WARNING
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = colors.border, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(14.dp))

                        // Key Confirmation Details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Merchant / Shop", fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.secondaryText)
                            Text(shopName.ifBlank { "Verified Shop" }, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.primaryText)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Amount", fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.secondaryText)
                            Text(formatEtb(totalAmount), fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SuqiPrimary)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Payment Method", fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.secondaryText)
                            Text(paymentMethod?.bankName ?: "Telebirr", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = colors.primaryText)
                        }

                        if (transactionRef.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Transaction Ref", fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.secondaryText)
                                Text(transactionRef, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.primaryText)
                            }
                        }

                        if (payerIdentifier.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Payer Identifier", fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.secondaryText)
                                Text(payerIdentifier, fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.primaryText)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = colors.border, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(14.dp))

                        // Delivery destination block
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(LucideIcons.MapPin, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "$recipientName • $recipientPhone",
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = colors.primaryText
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = deliveryAddress,
                                    fontFamily = SuqiFontFamily,
                                    fontSize = 12.sp,
                                    color = colors.secondaryText
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // 3-Step Order Status Progress Timeline (Plan §6 & §19 Screen 11)
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.secondaryBackground),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Next Steps",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = colors.primaryText
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Step 1: Placed
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(SuqiSuccess),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(LucideIcons.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Order Placed", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.primaryText)
                                Text("Details submitted to seller", fontFamily = SuqiFontFamily, fontSize = 11.sp, color = colors.secondaryText)
                            }
                        }

                        // Connector line
                        Box(
                            modifier = Modifier
                                .padding(start = 11.dp)
                                .width(2.dp)
                                .height(16.dp)
                                .background(SuqiPrimary)
                        )

                        // Step 2: Verification
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(SuqiPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(LucideIcons.Clock, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Payment Verification (Current)", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SuqiPrimary)
                                Text("Seller matches your transaction reference", fontFamily = SuqiFontFamily, fontSize = 11.sp, color = colors.secondaryText)
                            }
                        }

                        // Connector line
                        Box(
                            modifier = Modifier
                                .padding(start = 11.dp)
                                .width(2.dp)
                                .height(16.dp)
                                .background(colors.border)
                        )

                        // Step 3: Delivery
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(colors.border),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(LucideIcons.Truck, contentDescription = null, tint = colors.secondaryText, modifier = Modifier.size(13.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Dispatch & Delivery", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = colors.secondaryText)
                                Text("Shipped to your delivery destination", fontFamily = SuqiFontFamily, fontSize = 11.sp, color = colors.mutedText)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }

            // Primary Action: View Order
            item {
                Button(
                    onClick = {
                        viewModel.navigate(ScreenDestination.OrderDetails(orderId))
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_view_order")
                ) {
                    Text(
                        text = "View Order Details",
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Secondary Action: Continue Shopping
                OutlinedButton(
                    onClick = { viewModel.popToRoot(ScreenDestination.Home) },
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, SuqiPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = "Continue Shopping",
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = SuqiPrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Chat with shop
                TextButton(
                    onClick = {
                        viewModel.navigate(ScreenDestination.ChatThread(viewModel.activeCheckoutShopId.value))
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(LucideIcons.MessageCircle, contentDescription = null, tint = colors.secondaryText, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Chat with Shop about this order",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = colors.secondaryText
                        )
                    }
                }
            }
        }
    }
}
