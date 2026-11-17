package com.example.ui.screens

import com.example.ui.i18n.localizeDeliveryTitle
import com.example.ui.i18n.localizeEta
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.ui.i18n.suqiStrings
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
private fun localizedOrderStatus(status: String): String {
    val strings = suqiStrings()
    return when {
        status.equals("Delivered", ignoreCase = true) -> strings.checkout.filterDelivered
        status.equals("Cancelled", ignoreCase = true) -> strings.checkout.filterCancelled
        status.equals("Dispatched", ignoreCase = true) -> strings.checkout.statusDispatched
        status.equals("Out for Delivery", ignoreCase = true) -> strings.checkout.statusOutForDelivery
        status.equals("Confirmed", ignoreCase = true) -> strings.checkout.filterConfirmed
        status.equals("Processing", ignoreCase = true) -> strings.checkout.statusProcessing
        status.contains("Awaiting", ignoreCase = true) -> strings.checkout.statusPendingVerif
        else -> status
    }
}

@Composable
fun MyOrdersScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = com.example.ui.i18n.suqiStrings()
    val isOffline by viewModel.isOffline.collectAsState()

    var orders by remember { mutableStateOf(viewModel.cachedBuyerOrders) }
    var isLoading by remember { mutableStateOf(orders.isEmpty()) }
    var selectedFilter by rememberSaveable { mutableStateOf("All") }
    val listState = rememberLazyListState()

    val filterTabs = listOf("All", "Pending", "Confirmed", "Delivered", "Cancelled")

    fun loadOrders() {
        coroutineScope.launch {
            isLoading = orders.isEmpty()
            val res = viewModel.repository.getOrders()
            if (res.success && res.data != null) {
                orders = res.data
                viewModel.cachedBuyerOrders = res.data
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
        title = strings.myOrders,
        subtitle = "${orders.size} ${strings.myOrders.lowercase()}",
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
                state = listState,
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
                        val localizedTab = when (tab) {
                            "All" -> strings.checkout.filterAll
                            "Pending" -> strings.checkout.filterPending
                            "Confirmed" -> strings.checkout.filterConfirmed
                            "Delivered" -> strings.checkout.filterDelivered
                            else -> strings.checkout.filterCancelled
                        }
                        tab to "$localizedTab ($count)"
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
                        val localizedSelectedFilter = when (selectedFilter) {
                            "All" -> strings.checkout.filterAll
                            "Pending" -> strings.checkout.filterPending
                            "Confirmed" -> strings.checkout.filterConfirmed
                            "Delivered" -> strings.checkout.filterDelivered
                            else -> strings.checkout.filterCancelled
                        }
                        SuqiEmptyState(
                            icon = LucideIcons.PackageOpen,
                            title = if (orders.isEmpty()) strings.checkout.noOrdersFound else String.format(strings.checkout.noOrdersStatusTemplate, localizedSelectedFilter),
                            subtitle = strings.checkout.noOrdersSub,
                            ctaText = if (orders.isEmpty()) strings.checkout.continueShoppingBtn else strings.checkout.filterAll,
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
    val strings = suqiStrings()
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
                    Text(String.format(strings.checkout.orderNumberTemplate, order.orderReference), fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.secondaryText)
                }
            }

            when (order.paymentStatus) {
                "PENDING_VERIFICATION" -> SuqiStatusBadge(strings.checkout.statusPendingVerif, StatusType.WARNING)
                "REJECTED" -> SuqiStatusBadge(strings.checkout.statusPaymentRejected, StatusType.ERROR)
                else -> when (order.orderStatus) {
                    "Delivered" -> SuqiStatusBadge(localizedOrderStatus(order.orderStatus), StatusType.SUCCESS)
                    "Cancelled" -> SuqiStatusBadge(localizedOrderStatus(order.orderStatus), StatusType.ERROR)
                    "Dispatched", "Out for Delivery" -> SuqiStatusBadge(localizedOrderStatus(order.orderStatus), StatusType.NEUTRAL)
                    else -> SuqiStatusBadge(localizedOrderStatus(order.orderStatus), StatusType.SUCCESS)
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
                Text(strings.checkout.totalAmountLabel, fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.mutedText)
                Text(formatEtb(order.totalAmount), fontFamily = SuqiFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = SuqiPrimary)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.primaryTint)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(strings.checkout.viewDetailsBtn, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SuqiPrimary)
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
    val strings = com.example.ui.i18n.suqiStrings()
    val currentUser by viewModel.currentUser.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()

    var order by remember { mutableStateOf<OrderDto?>(null) }
    var showResubmitSheet by remember { mutableStateOf(false) }
    var showSupportSheet by remember { mutableStateOf(false) }
    var showInvoice by remember { mutableStateOf(false) }
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
        SuqiScreen(title = strings.orderDetails, onBackClick = { viewModel.goBack() }) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(top = 12.dp)) {
                SuqiSkeletonList(count = 3)
            }
        }
        return
    }

    SuqiScreen(
        title = strings.orderDetails,
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
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colors.cardBackground)
                    .testTag("order_need_help_btn")
            ) {
                Icon(LucideIcons.Headphones, contentDescription = strings.checkout.needHelpBtn, tint = SuqiPrimary, modifier = Modifier.size(19.dp))
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
                        Text(strings.checkout.chatShortBtn, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, color = colors.primaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    when {
                        currentOrder.paymentStatus == "REJECTED" -> {
                            SuqiButton(
                                text = strings.checkout.resubmitProofBtn,
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
                                        viewModel.showToast(strings.checkout.filterCancelled)
                                        loadOrder()
                                    }
                                },
                                shape = RoundedCornerShape(999.dp),
                                border = BorderStroke(1.dp, SuqiError.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1.2f).height(48.dp)
                            ) {
                                Text(strings.cancelOrder, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, color = SuqiError, maxLines = 2)
                            }
                        }
                        else -> {
                            SuqiButton(
                                text = strings.reorder,
                                icon = LucideIcons.RotateCcw,
                                isPill = true,
                                onClick = {
                                    coroutineScope.launch {
                                        currentOrder.items.forEach { item ->
                                            viewModel.repository.addToCart(item.productId, item.quantity, item.variation)
                                        }
                                        viewModel.showToast(strings.addedToCart)
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
                        Text(String.format(strings.checkout.orderNumberTemplate, currentOrder.orderReference), fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.primaryText)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (currentOrder.paymentStatus) {
                                "VERIFIED" -> "${localizedOrderStatus(currentOrder.orderStatus)} • ${strings.verifiedLabel}"
                                "REJECTED" -> strings.checkout.statusPaymentRejected
                                else -> strings.checkout.statusPendingVerif
                            },
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = iconColor
                        )

                        if (!currentOrder.rejectionReason.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("${strings.checkout.rejectionReasonTitle}: ${currentOrder.rejectionReason}", fontFamily = SuqiFontFamily, fontSize = 13.sp, color = SuqiError)
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
                                    text = "${strings.verifiedLabel}: ${currentOrder.verifiedBy ?: currentOrder.shopName} (${currentOrder.verifiedAt ?: ""})",
                                    fontFamily = SuqiFontFamily,
                                    fontSize = 11.sp,
                                    color = SuqiSuccess
                                )
                            }
                        }
                    }
                }
            }

            // ETA chip + invoice entry
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    EstimatedDeliveryChip(order = currentOrder)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { showInvoice = true },
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, SuqiPrimary),
                        modifier = Modifier.heightIn(min = 48.dp).testTag("order_invoice_btn")
                    ) {
                        Icon(LucideIcons.Receipt, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.buyerWork.invoiceBtn, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, color = SuqiPrimary)
                    }
                    ReturnItemPill(order = currentOrder, modifier = Modifier.padding(top = 8.dp))
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
                                    Text(strings.extras.localizeDeliveryTitle(currentOrder.deliveryOptionTitle).ifBlank { strings.checkout.courierDeliveryTitle }, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primaryText)
                                    val courierInfo = listOfNotNull(
                                        currentOrder.courierName.takeIf { it.isNotBlank() },
                                        currentOrder.courierPhone.takeIf { it.isNotBlank() }
                                    ).joinToString(" • ").ifBlank { currentOrder.shopName }
                                    Text(courierInfo, fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.secondaryText)
                                }
                            }
                            SuqiStatusBadge(localizedOrderStatus(currentOrder.orderStatus), if (currentOrder.orderStatus == "Delivered") StatusType.SUCCESS else StatusType.NEUTRAL)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${String.format(strings.checkout.etaEstimateTemplate, strings.extras.localizeEta(currentOrder.deliveryEstimatedDays, com.example.ui.i18n.LocalSuqiLanguage.current))} • ${currentOrder.deliveryAddress}",
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
                    Text("${strings.checkout.purchasedItemsTitle} (${currentOrder.items.size})", fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.primaryText)
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
                                Text("${item.quantity} × ${formatEtb(item.unitPrice)}", fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.secondaryText)
                            }
                            TextButton(
                                onClick = {
                                    viewModel.navigate(ScreenDestination.Reviews(item.productId, item.productName, "PRODUCT"))
                                }
                            ) {
                                Icon(LucideIcons.Star, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.buyer.writeReviewTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SuqiPrimary)
                            }
                        }
                    }
                }
            }

            // Order Timeline
            item {
                SuqiSectionCard {
                    Text(strings.checkout.orderTimelineTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.primaryText)
                    Spacer(modifier = Modifier.height(12.dp))
                    val isVerified = currentOrder.paymentStatus == "VERIFIED"
                    val isDispatched = currentOrder.orderStatus in listOf("Dispatched", "Out for Delivery", "Delivered")
                    val isDelivered = currentOrder.orderStatus == "Delivered"

                    TimelineStep(title = strings.checkout.stepOrderPlaced, time = currentOrder.createdAt, isDone = true)
                    TimelineStep(
                        title = strings.checkout.stepSellerVerified,
                        time = if (isVerified) currentOrder.verifiedAt ?: strings.verifiedLabel else strings.checkout.statusPendingVerif,
                        isDone = isVerified
                    )
                    TimelineStep(
                        title = strings.checkout.statusProcessing,
                        time = if (isVerified) strings.checkout.statusProcessing else strings.checkout.statusPendingVerif,
                        isDone = isVerified
                    )
                    TimelineStep(
                        title = strings.checkout.statusOutForDelivery,
                        time = if (isDispatched) currentOrder.dispatchedAt ?: strings.checkout.statusOutForDelivery else strings.checkout.filterPending,
                        isDone = isDispatched
                    )
                    TimelineStep(
                        title = strings.checkout.filterDelivered,
                        time = if (isDelivered) strings.common.doneLabel else String.format(strings.checkout.etaEstimateTemplate, strings.extras.localizeEta(currentOrder.deliveryEstimatedDays, com.example.ui.i18n.LocalSuqiLanguage.current)),
                        isDone = isDelivered,
                        isLast = true
                    )
                }
            }

            // Payment & Receipt Details + Delivery Destination
            item {
                SuqiSectionCard {
                    Text(strings.checkout.deliveryAddressTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${currentOrder.customerName} (${currentOrder.customerPhone})\n${currentOrder.deliveryAddress}", fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.secondaryText)

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = colors.border)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(strings.checkout.paymentProofSummaryTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(strings.checkout.paymentMethodTitle, fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.secondaryText)
                        Text(currentOrder.paymentMethodTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.primaryText)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(strings.checkout.txRefLabel, fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.secondaryText)
                        Text(currentOrder.transactionReference, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SuqiPrimary)
                    }
                    if (currentOrder.payerIdentifier.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(strings.checkout.senderPhoneLabel, fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.secondaryText)
                            Text(currentOrder.payerIdentifier, fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.primaryText)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(strings.checkout.amountPaidLabel, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.primaryText)
                        Text(formatEtb(currentOrder.totalAmount), fontFamily = SuqiFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = SuqiPrimary)
                    }

                    if (currentOrder.receiptImageUri.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(strings.checkout.receiptAttachedLabel, fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.secondaryText)
                        Spacer(modifier = Modifier.height(6.dp))
                        AsyncImage(
                            model = currentOrder.receiptImageUri,
                            contentDescription = strings.checkout.receiptAttachedLabel,
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
                    contentDescription = strings.checkout.receiptAttachedLabel,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                )
                IconButton(
                    onClick = { zoomReceiptUri = null },
                    modifier = Modifier.align(Alignment.TopEnd).padding(20.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(LucideIcons.X, contentDescription = strings.common.closeLabel, tint = Color.White)
                }
            }
        }
    }

    // Resubmit Payment Proof Sheet (Ref + Optional PickVisualMedia receipt)
    if (showInvoice) {
        InvoiceSheet(order = currentOrder, viewModel = viewModel, onDismiss = { showInvoice = false })
    }

    if (showResubmitSheet) {
        var newRef by remember { mutableStateOf(currentOrder.transactionReference) }
        var newAmount by remember {
            mutableStateOf((currentOrder.transferredAmount ?: currentOrder.totalAmount).toLong().toString())
        }
        var newReceiptUri by remember { mutableStateOf(currentOrder.receiptImageUri) }
        val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) newReceiptUri = uri.toString()
        }
        SuqiBottomSheet(onDismissRequest = { showResubmitSheet = false }, title = strings.checkout.resubmitProofBtn) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(strings.checkout.receiptRequiredHint, fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.secondaryText)
                SuqiTextField(value = newRef, onValueChange = { newRef = it }, label = strings.checkout.newTxRefRequiredLabel, placeholder = "TXN991022")
                SuqiTextField(
                    value = newAmount,
                    onValueChange = { newAmount = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = strings.checkout.transferAmountLabel,
                    placeholder = currentOrder.totalAmount.toLong().toString(),
                    testTag = "resubmit_transfer_amount_input"
                )
                OutlinedButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Icon(LucideIcons.Upload, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (newReceiptUri.isBlank()) strings.uploadReceipt else strings.checkout.changeReceiptLabel, fontFamily = SuqiFontFamily, color = SuqiPrimary)
                }
                SuqiButton(
                    text = strings.checkout.submitNewProofBtn,
                    isPill = true,
                    enabled = newRef.isNotBlank(),
                    onClick = {
                        coroutineScope.launch {
                            viewModel.repository.resubmitPayment(
                                currentOrder.id,
                                buildMap {
                                    put("transactionReference", newRef.trim())
                                    put("receiptImageUri", newReceiptUri)
                                    newAmount.trim().toDoubleOrNull()?.let { amt ->
                                        put("transferredAmount", amt.toString())
                                    }
                                }
                            )
                            viewModel.showToast(strings.common.doneLabel)
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
        SuqiBottomSheet(onDismissRequest = { showSupportSheet = false }, title = strings.checkout.submitHelpTicketTitle) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "Return / Refund" to strings.returnItem,
                        "Payment Issue" to strings.checkout.paymentMethodTitle,
                        "Delivery Delay" to strings.checkout.courierDeliveryTitle
                    ).forEach { (catKey, catLabel) ->
                        FilterChip(
                            selected = category == catKey,
                            onClick = { category = catKey },
                            label = { Text(catLabel, fontFamily = SuqiFontFamily, fontSize = 12.sp, maxLines = 2) }
                        )
                    }
                }
                SuqiTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = strings.checkout.messageDetailsRequiredLabel,
                    placeholder = strings.checkout.messageDetailsRequiredLabel,
                    singleLine = false
                )
                SuqiButton(
                    text = strings.checkout.submitTicketBtn,
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
                                    createdAt = strings.extras.justNowLabel
                                )
                            )
                            viewModel.showToast(strings.common.doneLabel)
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
