package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.OrderDto
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun MyOrdersScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var orders by remember { mutableStateOf<List<OrderDto>>(emptyList()) }
    var selectedFilter by remember { mutableStateOf("All") }

    val filterTabs = listOf("All", "Pending", "Confirmed", "Delivered", "Cancelled")

    fun loadOrders() {
        coroutineScope.launch {
            val res = viewModel.repository.getOrders()
            if (res.success && res.data != null) {
                orders = res.data
            }
        }
    }

    LaunchedEffect(Unit) {
        loadOrders()
    }

    val filteredOrders = when (selectedFilter) {
        "Pending" -> orders.filter { it.paymentStatus == "PENDING_VERIFICATION" || it.orderStatus.contains("Pending", ignoreCase = true) }
        "Confirmed" -> orders.filter { it.orderStatus.equals("Confirmed", ignoreCase = true) || it.orderStatus.equals("Processing", ignoreCase = true) }
        "Delivered" -> orders.filter { it.orderStatus.equals("Delivered", ignoreCase = true) }
        "Cancelled" -> orders.filter { it.orderStatus.equals("Cancelled", ignoreCase = true) }
        else -> orders
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
                    text = "My Orders",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    color = SuqiPrimaryText
                )
            }
        },
        containerColor = SuqiBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Filter Tabs (Mockup 3)
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filterTabs) { tab ->
                        val isSel = tab == selectedFilter
                        val count = when (tab) {
                            "All" -> orders.size
                            "Pending" -> orders.count { it.paymentStatus == "PENDING_VERIFICATION" }
                            "Confirmed" -> orders.count { it.orderStatus == "Confirmed" }
                            "Delivered" -> orders.count { it.orderStatus == "Delivered" }
                            else -> orders.count { it.orderStatus == "Cancelled" }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (isSel) SuqiPrimary else SuqiSecondaryBackground)
                                .clickable { selectedFilter = tab }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "$tab ($count)",
                                fontFamily = SuqiFontFamily,
                                fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 13.sp,
                                color = if (isSel) Color.White else SuqiPrimaryText
                            )
                        }
                    }
                }
            }

            if (filteredOrders.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No orders in \"$selectedFilter\"",
                            fontFamily = SuqiFontFamily,
                            color = SuqiSecondaryText
                        )
                    }
                }
            } else {
                items(filteredOrders) { order ->
                    CustomerOrderCard(
                        order = order,
                        onViewDetails = { viewModel.navigate(ScreenDestination.OrderDetails(order.id)) }
                    )
                }
            }
        }
    }
}

@Composable
fun CustomerOrderCard(
    order: OrderDto,
    onViewDetails: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SuqiSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(SuqiBorder)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onViewDetails)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with Shop name and Status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(SuqiPrimaryTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = order.shopName.take(2).uppercase(),
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = SuqiPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = order.shopName,
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Order #${order.orderReference}",
                            fontFamily = SuqiFontFamily,
                            fontSize = 12.sp,
                            color = SuqiSecondaryText
                        )
                    }
                }

                // Status Badge
                when (order.paymentStatus) {
                    "PENDING_VERIFICATION" -> SuqiStatusBadge("Pending Verification", StatusType.WARNING)
                    "REJECTED" -> SuqiStatusBadge("Payment Rejected", StatusType.ERROR)
                    else -> when (order.orderStatus) {
                        "Delivered" -> SuqiStatusBadge("Delivered", StatusType.SUCCESS)
                        "Cancelled" -> SuqiStatusBadge("Cancelled", StatusType.ERROR)
                        else -> SuqiStatusBadge("Confirmed", StatusType.SUCCESS)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Order Thumbnails Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                order.items.take(3).forEach { item ->
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SuqiSecondaryBackground)
                    ) {
                        AsyncImage(
                            model = item.productImageUrl,
                            contentDescription = item.productName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                if (order.items.size > 3) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SuqiSecondaryBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+${order.items.size - 3}",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = SuqiSecondaryText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Total and View Details action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Amount",
                        fontFamily = SuqiFontFamily,
                        fontSize = 12.sp,
                        color = SuqiMutedText
                    )
                    Text(
                        text = formatEtb(order.totalAmount),
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = SuqiPrimary
                    )
                }

                Button(
                    onClick = onViewDetails,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LocalSuqiColors.current.primaryTint),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "View Details",
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = SuqiPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun OrderDetailScreen(
    orderId: String,
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var order by remember { mutableStateOf<OrderDto?>(null) }
    var showResubmitDialog by remember { mutableStateOf(false) }

    fun loadOrder() {
        coroutineScope.launch {
            val res = viewModel.repository.getOrderDetails(orderId)
            if (res.success && res.data != null) {
                order = res.data
            }
        }
    }

    LaunchedEffect(orderId) {
        loadOrder()
    }

    val currentOrder = order ?: return Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) { CircularProgressIndicator(color = SuqiPrimary) }

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
                    text = "Order Details",
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.navigate(ScreenDestination.ChatThread(currentOrder.shopId)) },
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SuqiBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(LucideIcons.MessageCircle, contentDescription = null, tint = SuqiPrimaryText, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Chat with Shop", fontFamily = SuqiFontFamily, color = SuqiPrimaryText)
                    }

                    if (currentOrder.paymentStatus == "REJECTED") {
                        Button(
                            onClick = { showResubmitDialog = true },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Resubmit Payment", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold)
                        }
                    } else if (currentOrder.orderStatus == "Awaiting Payment Confirmation" && currentOrder.paymentStatus != "VERIFIED") {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.repository.cancelOrder(currentOrder.id)
                                    viewModel.showToast("Order cancelled and held stock released.")
                                    loadOrder()
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SuqiSecondaryBackground),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Cancel Order", fontFamily = SuqiFontFamily, color = SuqiError)
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
            // Status Header Card (Mockup 3)
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SuqiSecondaryBackground),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val iconColor = when (currentOrder.paymentStatus) {
                            "VERIFIED" -> SuqiSuccess
                            "REJECTED" -> SuqiError
                            else -> SuqiWarning
                        }
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(iconColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (currentOrder.paymentStatus) {
                                    "VERIFIED" -> LucideIcons.Check
                                    "REJECTED" -> LucideIcons.X
                                    else -> LucideIcons.Hourglass
                                },
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Order #${currentOrder.orderReference}",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = when (currentOrder.paymentStatus) {
                                "VERIFIED" -> "Payment Verified"
                                "REJECTED" -> "Payment Rejected"
                                else -> "Awaiting Payment Confirmation"
                            },
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = iconColor
                        )

                        if (currentOrder.rejectionReason != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Reason: ${currentOrder.rejectionReason}",
                                fontFamily = SuqiFontFamily,
                                fontSize = 13.sp,
                                color = SuqiError
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Order Timeline (Mockup 3)
            item {
                Text(
                    text = "Order Status Timeline",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                val isVerified = currentOrder.paymentStatus == "VERIFIED"
                val isDispatched = currentOrder.orderStatus in listOf("Dispatched", "Out for Delivery", "Delivered")
                val isDelivered = currentOrder.orderStatus == "Delivered"

                TimelineStep(
                    title = "Order Submitted",
                    time = currentOrder.createdAt,
                    isDone = true
                )
                TimelineStep(
                    title = "Payment Verified by Seller",
                    time = if (isVerified) currentOrder.verifiedAt ?: "Verified" else "Pending verification",
                    isDone = isVerified
                )
                TimelineStep(
                    title = "Preparing Your Order",
                    time = if (isVerified) "In progress" else "Waiting for payment",
                    isDone = isVerified
                )
                TimelineStep(
                    title = "Dispatched / Out for Delivery",
                    time = if (isDispatched) "On the way" else "Pending dispatch",
                    isDone = isDispatched
                )
                TimelineStep(
                    title = "Delivered",
                    time = if (isDelivered) "Completed" else "Expected in 1-3 days",
                    isDone = isDelivered,
                    isLast = true
                )

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Delivery & Transaction Details Cards
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SuqiSecondaryBackground),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Delivery Destination",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${currentOrder.customerName} (${currentOrder.customerPhone})\n${currentOrder.deliveryAddress}",
                            fontFamily = SuqiFontFamily,
                            fontSize = 13.sp,
                            color = SuqiSecondaryText
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = SuqiBorder)
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Payment & Transaction Details",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Payment Method", fontFamily = SuqiFontFamily, fontSize = 13.sp, color = SuqiSecondaryText)
                            Text(currentOrder.paymentMethodTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Reference Number", fontFamily = SuqiFontFamily, fontSize = 13.sp, color = SuqiSecondaryText)
                            Text(currentOrder.transactionReference, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = SuqiPrimary)
                        }
                    }
                }
            }
        }
    }

    if (showResubmitDialog) {
        var newRef by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showResubmitDialog = false },
            title = { Text("Resubmit Transaction Reference", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter the corrected transaction reference number from your mobile banking or Telebirr SMS:")
                    Spacer(modifier = Modifier.height(8.dp))
                    SuqiTextField(value = newRef, onValueChange = { newRef = it }, label = "New Reference Number")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newRef.isNotBlank()) {
                            coroutineScope.launch {
                                viewModel.repository.resubmitPayment(currentOrder.id, mapOf("transactionReference" to newRef))
                                viewModel.showToast("Payment resubmitted!")
                                showResubmitDialog = false
                                loadOrder()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary)
                ) {
                    Text("Resubmit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResubmitDialog = false }) { Text("Cancel") }
            },
            containerColor = SuqiSurface,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun TimelineStep(
    title: String,
    time: String,
    isDone: Boolean,
    isLast: Boolean = false
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (isDone) SuqiPrimary else SuqiBorder),
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    Icon(LucideIcons.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(36.dp)
                        .background(if (isDone) SuqiPrimary else SuqiBorder)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.padding(bottom = if (isLast) 0.dp else 16.dp)) {
            Text(
                text = title,
                fontFamily = SuqiFontFamily,
                fontWeight = if (isDone) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 14.sp,
                color = if (isDone) SuqiPrimaryText else SuqiMutedText
            )
            Text(
                text = time,
                fontFamily = SuqiFontFamily,
                fontSize = 12.sp,
                color = SuqiSecondaryText
            )
        }
    }
}
