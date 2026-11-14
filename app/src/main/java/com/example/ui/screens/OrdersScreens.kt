package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.OrderDto
import com.example.data.model.SupportTicketDto
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
    val colors = LocalSuqiColors.current
    val isOffline by viewModel.isOffline.collectAsState()

    var orders by remember { mutableStateOf<List<OrderDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("All") }

    val filterTabs = listOf("All", "Pending", "Confirmed", "Delivered", "Cancelled")

    fun loadOrders() {
        coroutineScope.launch {
            isLoading = orders.isEmpty()
            val res = viewModel.repository.getOrders()
            if (res.success && res.data != null) {
                orders = res.data
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadOrders()
    }

    val filteredOrders = when (selectedFilter) {
        "Pending" -> orders.filter { it.paymentStatus == "PENDING_VERIFICATION" || it.orderStatus.contains("Pending", ignoreCase = true) || it.orderStatus.contains("Awaiting", ignoreCase = true) }
        "Confirmed" -> orders.filter { it.orderStatus in listOf("Confirmed", "Processing", "Dispatched", "Out for Delivery") }
        "Delivered" -> orders.filter { it.orderStatus.equals("Delivered", ignoreCase = true) }
        "Cancelled" -> orders.filter { it.orderStatus.equals("Cancelled", ignoreCase = true) }
        else -> orders
    }

    SuqiScreen(
        title = "My Orders",
        subtitle = "${orders.size} total orders",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        onRetryOffline = {
            viewModel.setOfflineMode(false)
            loadOrders()
        },
        modifier = modifier
    ) { paddingValues ->
        if (isLoading && orders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(top = 12.dp)) {
                SuqiSkeletonList(count = 4)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    val chipItems = filterTabs.map { tab ->
                        val count = when (tab) {
                            "All" -> orders.size
                            "Pending" -> orders.count { it.paymentStatus == "PENDING_VERIFICATION" || it.orderStatus.contains("Awaiting", ignoreCase = true) }
                            "Confirmed" -> orders.count { it.orderStatus in listOf("Confirmed", "Processing", "Dispatched", "Out for Delivery") }
                            "Delivered" -> orders.count { it.orderStatus.equals("Delivered", ignoreCase = true) }
                            else -> orders.count { it.orderStatus.equals("Cancelled", ignoreCase = true) }
                        }
                        tab to "$tab ($count)"
                    }
                    SuqiFilterChips(
                        items = chipItems,
                        selectedKey = selectedFilter,
                        onSelect = { selectedFilter = it },
                        testTagPrefix = "orders_filter"
                    )
                }

                if (filteredOrders.isEmpty()) {
                    item {
                        SuqiEmptyState(
                            icon = LucideIcons.PackageOpen,
                            title = if (orders.isEmpty()) "No orders yet" else "No $selectedFilter orders",
                            subtitle = if (orders.isEmpty()) "Items you purchase across Ethiopian shops will appear here." else "Try switching status tabs to see other orders.",
                            ctaText = if (orders.isEmpty()) "Browse Marketplace" else "Show All Orders",
                            onCtaClick = {
                                if (orders.isEmpty()) viewModel.popToRoot(ScreenDestination.Home)
                                else selectedFilter = "All"
                            }
                        )
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
}

@Composable
fun CustomerOrderCard(
    order: OrderDto,
    onViewDetails: () -> Unit
) {
    val colors = LocalSuqiColors.current
    SuqiSectionCard(
        onClick = onViewDetails,
        modifier = Modifier.padding(horizontal = 16.dp).testTag("order_card_${order.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colors.primaryTint),
                    contentAlignment = Alignment.Center
                ) {
                    Text(order.shopName.take(2).uppercase(), fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SuqiPrimary)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(order.shopName, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Order #${order.orderReference}", fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.secondaryText)
                }
            }

            when (order.paymentStatus) {
                "PENDING_VERIFICATION" -> SuqiStatusBadge("Pending Verification", StatusType.WARNING)
                "REJECTED" -> SuqiStatusBadge("Payment Rejected", StatusType.ERROR)
                else -> when (order.orderStatus) {
                    "Delivered" -> SuqiStatusBadge("Delivered", StatusType.SUCCESS)
                    "Cancelled" -> SuqiStatusBadge("Cancelled", StatusType.ERROR)
                    "Dispatched", "Out for Delivery" -> SuqiStatusBadge(order.orderStatus, StatusType.NEUTRAL)
                    else -> SuqiStatusBadge(order.orderStatus, StatusType.SUCCESS)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            order.items.take(3).forEach { item ->
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.imageBackdrop)
                ) {
                    AsyncImage(model = item.productImageUrl, contentDescription = item.productName, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }
            if (order.items.size > 3) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.secondaryBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+${order.items.size - 3}", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.secondaryText)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Total Amount", fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.mutedText)
                Text(formatEtb(order.totalAmount), fontFamily = SuqiFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = SuqiPrimary)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.primaryTint)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("View Details", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SuqiPrimary)
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
    val colors = LocalSuqiColors.current
    val currentUser by viewModel.currentUser.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()

    var order by remember { mutableStateOf<OrderDto?>(null) }
    var showResubmitSheet by remember { mutableStateOf(false) }
    var showSupportSheet by remember { mutableStateOf(false) }
    var zoomReceiptUri by remember { mutableStateOf<String?>(null) }

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

    val currentOrder = order
    if (currentOrder == null) {
        SuqiScreen(title = "Order Details", onBackClick = { viewModel.goBack() }) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(top = 12.dp)) {
                SuqiSkeletonList(count = 3)
            }
        }
        return
    }

    SuqiScreen(
        title = "Order Details",
        subtitle = "#${currentOrder.orderReference}",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        onRetryOffline = {
            viewModel.setOfflineMode(false)
            loadOrder()
        },
        headerActions = {
            IconButton(
                onClick = { showSupportSheet = true },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.cardBackground)
                    .testTag("order_need_help_btn")
            ) {
                Icon(LucideIcons.Headphones, contentDescription = "Need help with order", tint = SuqiPrimary, modifier = Modifier.size(19.dp))
            }
        },
        bottomBar = {
            Surface(
                color = colors.cardBackground,
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.navigate(ScreenDestination.ChatThread(currentOrder.shopId)) },
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(LucideIcons.MessageCircle, contentDescription = null, tint = colors.primaryText, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Chat", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, color = colors.primaryText)
                    }

                    when {
                        currentOrder.paymentStatus == "REJECTED" -> {
                            SuqiButton(
                                text = "Resubmit Proof",
                                isPill = true,
                                onClick = { showResubmitSheet = true },
                                modifier = Modifier.weight(1.3f),
                                testTag = "btn_resubmit_payment"
                            )
                        }
                        currentOrder.orderStatus == "Awaiting Payment Confirmation" && currentOrder.paymentStatus != "VERIFIED" -> {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        viewModel.repository.cancelOrder(currentOrder.id)
                                        viewModel.showToast("Order cancelled and held stock released.")
                                        loadOrder()
                                    }
                                },
                                shape = RoundedCornerShape(999.dp),
                                border = BorderStroke(1.dp, SuqiError.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1.2f).height(48.dp)
                            ) {
                                Text("Cancel Order", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, color = SuqiError)
                            }
                        }
                        else -> {
                            SuqiButton(
                                text = "Reorder Items",
                                icon = LucideIcons.RotateCcw,
                                isPill = true,
                                onClick = {
                                    coroutineScope.launch {
                                        currentOrder.items.forEach { item ->
                                            viewModel.repository.addToCart(item.productId, item.quantity, item.variation)
                                        }
                                        viewModel.showToast("Items added to cart!")
                                        viewModel.navigate(ScreenDestination.Cart)
                                    }
                                },
                                modifier = Modifier.weight(1.3f),
                                testTag = "btn_reorder_items"
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Status Header Card
            item {
                SuqiSectionCard {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        val iconColor = when (currentOrder.paymentStatus) {
                            "VERIFIED" -> SuqiSuccess
                            "REJECTED" -> SuqiError
                            else -> SuqiWarning
                        }
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(iconColor.copy(alpha = 0.14f)),
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
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Order #${currentOrder.orderReference}", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.primaryText)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (currentOrder.paymentStatus) {
                                "VERIFIED" -> "${currentOrder.orderStatus} • Payment Verified"
                                "REJECTED" -> "Payment Rejected"
                                else -> "Awaiting Payment Confirmation"
                            },
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = iconColor
                        )

                        if (!currentOrder.rejectionReason.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Reason: ${currentOrder.rejectionReason}", fontFamily = SuqiFontFamily, fontSize = 13.sp, color = SuqiError)
                        }

                        if (!currentOrder.verifiedBy.isNullOrBlank() || !currentOrder.verifiedAt.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.successBg)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(LucideIcons.ShieldCheck, contentDescription = null, tint = SuqiSuccess, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Verified by ${currentOrder.verifiedBy ?: "Merchant"} (${currentOrder.verifiedAt ?: "recorded"})",
                                    fontFamily = SuqiFontFamily,
                                    fontSize = 11.sp,
                                    color = SuqiSuccess
                                )
                            }
                        }
                    }
                }
            }

            // Courier card — ONLY rendered when actually dispatched or delivered (zero hardcoded Alemayehu T.)
            if (currentOrder.orderStatus in listOf("Dispatched", "Out for Delivery", "Delivered")) {
                item {
                    SuqiSectionCard(containerColor = colors.shopTint, modifier = Modifier.border(1.dp, colors.shopBorder, RoundedCornerShape(SuqiRadius.Card))) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(colors.primaryTint),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(LucideIcons.Truck, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(currentOrder.deliveryOptionTitle.ifBlank { "Courier Dispatch" }, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primaryText)
                                    val courierInfo = listOfNotNull(
                                        currentOrder.courierName.takeIf { it.isNotBlank() },
                                        currentOrder.courierPhone.takeIf { it.isNotBlank() }
                                    ).joinToString(" • ").ifBlank { "Assigned by ${currentOrder.shopName}" }
                                    Text(courierInfo, fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.secondaryText)
                                }
                            }
                            SuqiStatusBadge(currentOrder.orderStatus, if (currentOrder.orderStatus == "Delivered") StatusType.SUCCESS else StatusType.NEUTRAL)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Estimated window: ${currentOrder.deliveryEstimatedDays} • Destination: ${currentOrder.deliveryAddress}",
                            fontFamily = SuqiFontFamily,
                            fontSize = 12.sp,
                            color = colors.mutedText
                        )
                    }
                }
            }

            // Purchased Items
            item {
                SuqiSectionCard {
                    Text("Purchased Items (${currentOrder.items.size})", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.primaryText)
                    Spacer(modifier = Modifier.height(10.dp))
                    currentOrder.items.forEachIndexed { idx, item ->
                        if (idx > 0) HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = 8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = item.productImageUrl,
                                contentDescription = item.productName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)).background(colors.imageBackdrop)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.productName, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.primaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Qty: ${item.quantity} • ${formatEtb(item.unitPrice)}", fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.secondaryText)
                            }
                            TextButton(
                                onClick = {
                                    viewModel.navigate(ScreenDestination.Reviews(item.productId, item.productName, "PRODUCT"))
                                }
                            ) {
                                Icon(LucideIcons.Star, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Review", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SuqiPrimary)
                            }
                        }
                    }
                }
            }

            // Order Timeline
            item {
                SuqiSectionCard {
                    Text("Order Status Timeline", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.primaryText)
                    Spacer(modifier = Modifier.height(12.dp))
                    val isVerified = currentOrder.paymentStatus == "VERIFIED"
                    val isDispatched = currentOrder.orderStatus in listOf("Dispatched", "Out for Delivery", "Delivered")
                    val isDelivered = currentOrder.orderStatus == "Delivered"

                    TimelineStep(title = "Order Submitted", time = currentOrder.createdAt, isDone = true)
                    TimelineStep(
                        title = "Payment Verified by Seller",
                        time = if (isVerified) currentOrder.verifiedAt ?: "Verified" else "Pending verification",
                        isDone = isVerified
                    )
                    TimelineStep(
                        title = "Preparing Order",
                        time = if (isVerified) "Seller preparing items" else "Waiting for payment confirmation",
                        isDone = isVerified
                    )
                    TimelineStep(
                        title = "Dispatched / Out for Delivery",
                        time = if (isDispatched) currentOrder.dispatchedAt ?: "On the way" else "Pending dispatch",
                        isDone = isDispatched
                    )
                    TimelineStep(
                        title = "Delivered",
                        time = if (isDelivered) "Completed" else "Expected in ${currentOrder.deliveryEstimatedDays}",
                        isDone = isDelivered,
                        isLast = true
                    )
                }
            }

            // Payment & Receipt Details + Delivery Destination
            item {
                SuqiSectionCard {
                    Text("Delivery Destination", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${currentOrder.customerName} (${currentOrder.customerPhone})\n${currentOrder.deliveryAddress}", fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.secondaryText)

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = colors.border)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Payment Proof & Summary", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Payment Method", fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.secondaryText)
                        Text(currentOrder.paymentMethodTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.primaryText)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Transaction Ref", fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.secondaryText)
                        Text(currentOrder.transactionReference, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SuqiPrimary)
                    }
                    if (currentOrder.payerIdentifier.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Payer", fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.secondaryText)
                            Text(currentOrder.payerIdentifier, fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.primaryText)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Paid", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.primaryText)
                        Text(formatEtb(currentOrder.totalAmount), fontFamily = SuqiFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = SuqiPrimary)
                    }

                    if (currentOrder.receiptImageUri.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Uploaded Receipt Screenshot (Tap to zoom)", fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.secondaryText)
                        Spacer(modifier = Modifier.height(6.dp))
                        AsyncImage(
                            model = currentOrder.receiptImageUri,
                            contentDescription = "Uploaded payment receipt",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { zoomReceiptUri = currentOrder.receiptImageUri }
                                .testTag("order_receipt_thumbnail")
                        )
                    }
                }
            }
        }
    }

    // Full-screen receipt preview dialog
    if (!zoomReceiptUri.isNullOrBlank()) {
        Dialog(onDismissRequest = { zoomReceiptUri = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .clickable { zoomReceiptUri = null }
            ) {
                AsyncImage(
                    model = zoomReceiptUri,
                    contentDescription = "Full payment receipt",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                )
                IconButton(
                    onClick = { zoomReceiptUri = null },
                    modifier = Modifier.align(Alignment.TopEnd).padding(20.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(LucideIcons.X, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }

    // Resubmit Payment Proof Sheet (Ref + Optional PickVisualMedia receipt)
    if (showResubmitSheet) {
        var newRef by remember { mutableStateOf(currentOrder.transactionReference) }
        var newReceiptUri by remember { mutableStateOf(currentOrder.receiptImageUri) }
        val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) newReceiptUri = uri.toString()
        }
        SuqiBottomSheet(onDismissRequest = { showResubmitSheet = false }, title = "Resubmit Payment Proof") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Update your transaction reference and/or attach a clearer receipt screenshot:", fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.secondaryText)
                SuqiTextField(value = newRef, onValueChange = { newRef = it }, label = "Transaction Reference *", placeholder = "e.g., TXN991022")
                OutlinedButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(LucideIcons.Upload, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (newReceiptUri.isBlank()) "Attach Receipt Screenshot" else "Replace Receipt Screenshot", fontFamily = SuqiFontFamily, color = SuqiPrimary)
                }
                SuqiButton(
                    text = "Submit Updated Proof",
                    isPill = true,
                    enabled = newRef.isNotBlank(),
                    onClick = {
                        coroutineScope.launch {
                            viewModel.repository.resubmitPayment(
                                currentOrder.id,
                                mapOf("transactionReference" to newRef.trim(), "receiptImageUri" to newReceiptUri)
                            )
                            viewModel.showToast("Payment proof resubmitted!")
                            showResubmitSheet = false
                            loadOrder()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Need Help / Dispute & Return Ticket Sheet
    if (showSupportSheet) {
        var category by remember { mutableStateOf("Return / Refund") }
        var description by remember { mutableStateOf("") }
        SuqiBottomSheet(onDismissRequest = { showSupportSheet = false }, title = "Need Help With Order?") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Return / Refund", "Payment Issue", "Delivery Delay").forEach { cat ->
                        FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat, fontFamily = SuqiFontFamily, fontSize = 12.sp) })
                    }
                }
                SuqiTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "Describe the issue",
                    placeholder = "Tell us how our Ethiopian support team can help...",
                    singleLine = false
                )
                SuqiButton(
                    text = "Submit Support Ticket",
                    isPill = true,
                    enabled = description.isNotBlank(),
                    onClick = {
                        coroutineScope.launch {
                            viewModel.repository.submitSupportTicket(
                                SupportTicketDto(
                                    id = "tkt_${System.currentTimeMillis()}",
                                    userId = currentUser?.id ?: "user_fitsum",
                                    orderId = currentOrder.id,
                                    category = category,
                                    subject = "$category — Order #${currentOrder.orderReference}",
                                    description = description.trim(),
                                    status = "OPEN",
                                    createdAt = "Just now"
                                )
                            )
                            viewModel.showToast("Support ticket created!")
                            showSupportSheet = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun TimelineStep(
    title: String,
    time: String,
    isDone: Boolean,
    isLast: Boolean = false
) {
    val colors = LocalSuqiColors.current
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (isDone) SuqiPrimary else colors.border),
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
                        .height(34.dp)
                        .background(if (isDone) SuqiPrimary else colors.border)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.padding(bottom = if (isLast) 0.dp else 14.dp)) {
            Text(
                text = title,
                fontFamily = SuqiFontFamily,
                fontWeight = if (isDone) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 14.sp,
                color = if (isDone) colors.primaryText else colors.mutedText
            )
            Text(
                text = time,
                fontFamily = SuqiFontFamily,
                fontSize = 12.sp,
                color = colors.secondaryText
            )
        }
    }
}
