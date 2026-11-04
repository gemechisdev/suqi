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
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = SuqiPrimaryText
                )
            }
        },
        bottomBar = {
            Surface(
                color = Color.White,
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
                    fontWeight = FontWeight.Bold,
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
                    fontWeight = FontWeight.Bold,
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
                    fontWeight = FontWeight.Bold,
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
                            Text("Total", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                formatEtb(totalAmount),
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
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
        var tempName by remember { mutableStateOf(recipientName) }
        var tempPhone by remember { mutableStateOf(recipientPhone) }
        var tempAddress by remember { mutableStateOf(deliveryAddress) }

        AlertDialog(
            onDismissRequest = { showAddressDialog = false },
            title = { Text("Edit Delivery Address", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SuqiTextField(value = tempName, onValueChange = { tempName = it }, label = "Full Name")
                    SuqiTextField(value = tempPhone, onValueChange = { tempPhone = it }, label = "Phone (+251)")
                    SuqiTextField(value = tempAddress, onValueChange = { tempAddress = it }, label = "Street & Sub-City")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.activeCheckoutRecipientName.value = tempName
                        viewModel.activeCheckoutRecipientPhone.value = tempPhone
                        viewModel.activeCheckoutDeliveryAddress.value = tempAddress
                        showAddressDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddressDialog = false }) { Text("Cancel") }
            },
            containerColor = Color.White,
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
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = SuqiPrimaryText
                )
            }
        },
        bottomBar = {
            Surface(
                color = Color.White,
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
                            if (selectedMethod == null) {
                                errorMessage = "Please select a payment method"
                                return@Button
                            }

                            isSubmitting = true
                            errorMessage = null

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
                    fontWeight = FontWeight.Bold,
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
                        colors = CardDefaults.cardColors(containerColor = SuqiPrimaryLight),
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

            // Transaction Reference Required Input
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

    Scaffold(
        containerColor = SuqiBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Success Green Circle with Checkmark
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(SuqiSuccessLight),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(SuqiSuccess),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = LucideIcons.Check,
                            contentDescription = "Success",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Order Submitted!",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = SuqiPrimaryText
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Your order has been placed successfully.\nPlease wait for the seller to verify your payment.",
                    fontFamily = SuqiFontFamily,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = SuqiSecondaryText,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Order Reference Card with copy button & Pending Verification status badge
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SuqiSecondaryBackground),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Order Reference",
                            fontFamily = SuqiFontFamily,
                            fontSize = 12.sp,
                            color = SuqiSecondaryText
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = orderReference,
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = SuqiPrimaryText
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Order Ref", orderReference))
                                    viewModel.showToast("Order Reference copied!")
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(LucideIcons.Copy, contentDescription = "Copy", tint = SuqiSecondaryText, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Status Badge (Pending Verification)
                        SuqiStatusBadge(
                            statusText = "Pending Verification",
                            type = StatusType.WARNING
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Primary Action: View Order
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
                        text = "View Order",
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
                        Icon(LucideIcons.MessageCircle, contentDescription = null, tint = SuqiSecondaryText, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Chat with Shop",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = SuqiSecondaryText
                        )
                    }
                }
            }
        }
    }
}
