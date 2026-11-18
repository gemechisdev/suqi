package com.example.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdminSettingsDto
import com.example.data.model.FlaggedReviewDto
import com.example.data.model.OrderDto
import com.example.data.model.ShopDto
import com.example.data.model.SupportTicketDto
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.StatusType
import com.example.ui.components.SuqiAdminSegmentedTabs
import com.example.ui.components.SuqiBottomSheet
import com.example.ui.components.SuqiButton
import com.example.ui.components.SuqiEmptyState
import com.example.ui.components.SuqiListRow
import com.example.ui.components.SuqiScreen
import com.example.ui.components.SuqiSectionCard
import com.example.ui.components.SuqiStatusBadge
import com.example.ui.components.SuqiSwitchRow
import com.example.ui.components.SuqiTextField
import com.example.ui.components.formatEtb
import com.example.ui.i18n.placeLabel
import com.example.ui.i18n.suqiStrings
import com.example.ui.theme.LocalSuqiColors
import com.example.ui.theme.LucideIcons
import com.example.ui.theme.SuqiError
import com.example.ui.theme.SuqiFontFamily
import com.example.ui.theme.SuqiPrimary
import com.example.ui.theme.SuqiSuccess
import com.example.ui.theme.SuqiWarning
import com.example.util.EthiopianMarketplaceUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AdminConsoleScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val currentUser by viewModel.currentUser.collectAsState()
    val adminTier by viewModel.adminTier.collectAsState()
    val isDeveloperUnlocked by viewModel.isDeveloperUnlocked.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()

    val effectiveTier = currentUser?.adminTier?.takeIf { it != "NONE" } ?: adminTier
    val isOwnerTier = effectiveTier.equals("OWNER", ignoreCase = true)
    val isDevVisible = effectiveTier.equals("DEVELOPER", ignoreCase = true) ||
        isOwnerTier || isDeveloperUnlocked

    var selectedTab by rememberSaveable { mutableStateOf(viewModel.consumeAdminConsoleInitialTab()) }
    var settings by remember { mutableStateOf(viewModel.cachedAdminSettings ?: AdminSettingsDto()) }
    var pendingShops by remember { mutableStateOf(viewModel.cachedAdminPendingShops) }
    var flaggedReviews by remember { mutableStateOf(viewModel.cachedAdminFlaggedReviews) }
    var supportTickets by remember { mutableStateOf(viewModel.cachedAdminTickets) }
    var allOrders by remember { mutableStateOf(viewModel.cachedAdminAllOrders) }
    var pendingPayoutsCount by remember { mutableIntStateOf(0) }
    var switchesExpanded by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Quick tools sheets
    var showBroadcastSheet by remember { mutableStateOf(false) }
    var showLocationsSheet by remember { mutableStateOf(false) }
    var showContentSheet by remember { mutableStateOf(false) }

    // 15-minute idle timeout guard
    var lastActivityMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showIdleDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = showBroadcastSheet || showLocationsSheet || showContentSheet || showIdleDialog) {
        when {
            showIdleDialog -> {
                showIdleDialog = false
                lastActivityMs = System.currentTimeMillis()
            }
            showBroadcastSheet -> showBroadcastSheet = false
            showLocationsSheet -> showLocationsSheet = false
            showContentSheet -> showContentSheet = false
        }
    }

    LaunchedEffect(lastActivityMs) {
        delay(15L * 60L * 1000L)
        showIdleDialog = true
    }

    fun loadConsoleData() {
        coroutineScope.launch {
            viewModel.repository.getAdminSettings().data?.let {
                settings = it
                viewModel.cachedAdminSettings = it
            }
            val shopsRes = viewModel.repository.getAdminSellerVerificationQueue()
            if (shopsRes.success && shopsRes.data != null) {
                pendingShops = shopsRes.data
                viewModel.cachedAdminPendingShops = shopsRes.data
            }
            val revRes = viewModel.repository.getFlaggedReviews()
            if (revRes.success && revRes.data != null) {
                val reported = revRes.data.filter { it.status == "REPORTED" }
                flaggedReviews = reported
                viewModel.cachedAdminFlaggedReviews = reported
            }
            val ticketRes = viewModel.repository.getSupportTickets()
            if (ticketRes.success && ticketRes.data != null) {
                supportTickets = ticketRes.data
                viewModel.cachedAdminTickets = ticketRes.data
            }
            val ordersRes = viewModel.repository.getOrders()
            if (ordersRes.success && ordersRes.data != null) {
                allOrders = ordersRes.data
                viewModel.cachedAdminAllOrders = ordersRes.data
            }
            val finRes = viewModel.repository.getAdminFinanceOverview()
            if (finRes.success && finRes.data != null) {
                viewModel.cachedAdminFinance = finRes.data
                pendingPayoutsCount = finRes.data.pendingPayoutsCount
            }
        }
    }

    LaunchedEffect(Unit) {
        loadConsoleData()
    }

    val openTickets = remember(supportTickets) {
        supportTickets.filter { !it.status.equals("RESOLVED", ignoreCase = true) }
    }
    val disputedOrders = remember(allOrders) {
        allOrders.filter {
            it.paymentStatus == "REJECTED" ||
                it.orderStatus.contains("Dispute", ignoreCase = true)
        }
    }
    val payoutRequests = remember(allOrders) {
        allOrders.filter { it.paymentStatus == "PENDING_VERIFICATION" }
    }
    val reportedListingsCount = remember(flaggedReviews) {
        flaggedReviews.count { it.reason.contains("listing", ignoreCase = true) }
    }

    fun oldestAgeFor(timestamps: List<String>): String? {
        if (timestamps.isEmpty()) return null
        val oldestRaw = timestamps.minByOrNull { ts ->
            EthiopianMarketplaceUtils.parseMarketplaceTimestampMillis(ts) ?: Long.MAX_VALUE
        } ?: return null
        return EthiopianMarketplaceUtils.formatWaitingAgeLabel(oldestRaw, strings)
    }

    val tierLabel = when {
        isOwnerTier -> strings.adminOps.ownerRoleLabel
        effectiveTier.equals("DEVELOPER", ignoreCase = true) -> strings.adminOps.developerRoleLabel
        else -> strings.adminOps.adminRoleLabel
    }

    SuqiScreen(
        title = strings.admin.consoleTitle,
        subtitle = "$tierLabel • ${currentUser?.name.orEmpty()}",
        onBackClick = { viewModel.goBack() },
        headerActions = {
            IconButton(
                onClick = {
                    lastActivityMs = System.currentTimeMillis()
                    loadConsoleData()
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colors.secondaryBackground)
                    .testTag("admin_console_refresh")
            ) {
                Icon(
                    imageVector = LucideIcons.RefreshCw,
                    contentDescription = strings.adminOps.refreshLabel,
                    tint = colors.primaryText,
                    modifier = Modifier.size(18.dp)
                )
            }
        },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SuqiAdminSegmentedTabs(
                selectedTab = selectedTab,
                onSelectTab = {
                    lastActivityMs = System.currentTimeMillis()
                    selectedTab = it
                },
                isDeveloperVisible = isDevVisible
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (selectedTab) {
                    "overview" -> {
                        if (isOwnerTier) {
                            item {
                                SuqiSectionCard(
                                    padding = 16.dp,
                                    containerColor = colors.primaryTint,
                                    onClick = { viewModel.navigate(ScreenDestination.OwnerDashboard) },
                                    modifier = Modifier.testTag("owner_dashboard_entry_card")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(SuqiPrimary)
                                        ) {
                                            Icon(
                                                imageVector = LucideIcons.TrendingUp,
                                                contentDescription = strings.admin.ownerDashboardTitle,
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = strings.admin.ownerDashboardTitle,
                                                fontFamily = SuqiFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = colors.primaryText
                                            )
                                            Text(
                                                text = strings.adminGov.openOwnerDashboardSub,
                                                fontFamily = SuqiFontFamily,
                                                fontSize = 12.sp,
                                                color = colors.secondaryText,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Icon(
                                            imageVector = LucideIcons.ChevronRight,
                                            contentDescription = null,
                                            tint = SuqiPrimary
                                        )
                                    }
                                }
                            }
                        }

                        // 6 Live Queue Cards (3 rows x 2 cols)
                        item {
                            val queueCards = listOf(
                                QueueCardModel(
                                    key = "seller_verif",
                                    title = strings.adminGov.queueSellerVerif,
                                    count = pendingShops.size,
                                    oldestAge = oldestAgeFor(
                                        pendingShops.mapNotNull {
                                            com.example.data.model.VerificationDocsCodec.parse(it.verificationDocsJson)
                                                .submittedAt.takeIf { s -> s.isNotBlank() }
                                        }
                                    ),
                                    icon = LucideIcons.ShieldCheck,
                                    accent = SuqiPrimary,
                                    onClick = { viewModel.navigate(ScreenDestination.AdminFeatures) }
                                ),
                                QueueCardModel(
                                    key = "flagged_reviews",
                                    title = strings.adminGov.queueFlaggedReviews,
                                    count = flaggedReviews.size,
                                    oldestAge = oldestAgeFor(flaggedReviews.map { it.createdAt }),
                                    icon = LucideIcons.Flag,
                                    accent = SuqiError,
                                    onClick = { viewModel.navigate(ScreenDestination.AdminModeration) }
                                ),
                                QueueCardModel(
                                    key = "reported_listings",
                                    title = strings.adminGov.queueReportedListings,
                                    count = reportedListingsCount,
                                    oldestAge = oldestAgeFor(
                                        flaggedReviews.filter { it.reason.contains("listing", ignoreCase = true) }
                                            .map { it.createdAt }
                                    ),
                                    icon = LucideIcons.TriangleAlert,
                                    accent = SuqiWarning,
                                    onClick = { viewModel.navigate(ScreenDestination.AdminModeration) }
                                ),
                                QueueCardModel(
                                    key = "open_disputes",
                                    title = strings.adminGov.queueDisputes,
                                    count = disputedOrders.size,
                                    oldestAge = oldestAgeFor(disputedOrders.map { it.createdAt }),
                                    icon = LucideIcons.Handshake,
                                    accent = SuqiWarning,
                                    onClick = { selectedTab = "queues" }
                                ),
                                QueueCardModel(
                                    key = "support_tickets",
                                    title = strings.adminGov.queueSupportTickets,
                                    count = openTickets.size,
                                    oldestAge = oldestAgeFor(openTickets.map { it.createdAt }),
                                    icon = LucideIcons.Headphones,
                                    accent = SuqiPrimary,
                                    onClick = { viewModel.navigate(ScreenDestination.AdminSupportInbox) }
                                ),
                                QueueCardModel(
                                    key = "payout_requests",
                                    title = strings.adminGov.queuePayoutRequests,
                                    count = pendingPayoutsCount,
                                    oldestAge = oldestAgeFor(payoutRequests.map { it.createdAt }),
                                    icon = LucideIcons.Wallet,
                                    accent = SuqiSuccess,
                                    onClick = { viewModel.navigate(ScreenDestination.AdminFinance) }
                                )
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                queueCards.chunked(2).forEach { pair ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        pair.forEach { card ->
                                            AdminQueueOverviewCard(
                                                model = card,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Quick tools 3x2 grid
                        item {
                            SuqiSectionCard(padding = 16.dp) {
                                Text(
                                    text = strings.adminGov.quickToolsTitle,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = colors.primaryText
                                )
                                Spacer(Modifier.height(12.dp))
                                val quickTools = listOf(
                                    QuickToolItem(
                                        key = "banners",
                                        label = strings.admin.bannersManagerTitle,
                                        icon = LucideIcons.GalleryHorizontal,
                                        onClick = { viewModel.navigate(ScreenDestination.AdminBanners) }
                                    ),
                                    QuickToolItem(
                                        key = "categories",
                                        label = strings.admin.categoriesManagerTitle,
                                        icon = LucideIcons.LayoutGrid,
                                        onClick = { viewModel.navigate(ScreenDestination.AdminCategories) }
                                    ),
                                    QuickToolItem(
                                        key = "item_types",
                                        label = strings.admin.itemTypesManagerTitle,
                                        icon = LucideIcons.SlidersHorizontal,
                                        onClick = { viewModel.navigate(ScreenDestination.AdminItemTypes) }
                                    ),
                                    QuickToolItem(
                                        key = "broadcast",
                                        label = strings.admin.broadcastTitle,
                                        icon = LucideIcons.Send,
                                        onClick = { viewModel.navigate(ScreenDestination.AdminBroadcast) }
                                    ),
                                    QuickToolItem(
                                        key = "locations",
                                        label = strings.admin.locationsTitle,
                                        icon = LucideIcons.MapPin,
                                        onClick = { showLocationsSheet = true }
                                    ),
                                    QuickToolItem(
                                        key = "content",
                                        label = strings.admin.contentManagerTitle,
                                        icon = LucideIcons.FileText,
                                        onClick = { showContentSheet = true }
                                    )
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    quickTools.chunked(3).forEach { rowTools ->
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            rowTools.forEach { tool ->
                                                AdminQuickToolTile(
                                                    item = tool,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Collapsible Platform Switches Card
                        item {
                            SuqiSectionCard(padding = 16.dp) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { switchesExpanded = !switchesExpanded }
                                        .padding(vertical = 4.dp)
                                        .testTag("platform_switches_toggle")
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(colors.primaryTint)
                                    ) {
                                        Icon(
                                            imageVector = LucideIcons.SlidersHorizontal,
                                            contentDescription = null,
                                            tint = SuqiPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = strings.admin.platformSwitchesTitle,
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = colors.primaryText,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = if (switchesExpanded) LucideIcons.ChevronUp else LucideIcons.ChevronDown,
                                        contentDescription = strings.admin.platformSwitchesTitle,
                                        tint = colors.secondaryText
                                    )
                                }

                                AnimatedVisibility(visible = switchesExpanded) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(top = 10.dp)
                                    ) {
                                        HorizontalDivider(color = colors.border)
                                        Spacer(Modifier.height(4.dp))
                                        SuqiSwitchRow(
                                            title = strings.chatWithSeller,
                                            subtitle = strings.adminGov.switchChatSub,
                                            checked = settings.isChatEnabled,
                                            onCheckedChange = { enabled ->
                                                settings = settings.copy(isChatEnabled = enabled)
                                                coroutineScope.launch {
                                                    viewModel.repository.updateFeatureToggle("chat", enabled)
                                                }
                                            },
                                            testTag = "switch_platform_chat"
                                        )
                                        SuqiSwitchRow(
                                            title = strings.buyer.contactUsTitle,
                                            subtitle = strings.adminGov.switchExternalContactSub,
                                            checked = settings.isExternalContactEnabled,
                                            onCheckedChange = { enabled ->
                                                settings = settings.copy(isExternalContactEnabled = enabled)
                                                coroutineScope.launch {
                                                    viewModel.repository.updateFeatureToggle("external_contact", enabled)
                                                }
                                            },
                                            testTag = "switch_platform_external_contact"
                                        )
                                        SuqiSwitchRow(
                                            title = strings.seller.subscriptionTitle,
                                            subtitle = strings.adminGov.switchSubscriptionSub,
                                            checked = settings.isSubscriptionModeEnabled,
                                            onCheckedChange = { enabled ->
                                                settings = settings.copy(isSubscriptionModeEnabled = enabled)
                                                coroutineScope.launch {
                                                    viewModel.repository.updateFeatureToggle("subscription_mode", enabled)
                                                }
                                            },
                                            testTag = "switch_platform_subscription"
                                        )
                                        SuqiSwitchRow(
                                            title = strings.customerReviews,
                                            subtitle = strings.adminGov.switchReviewsSub,
                                            checked = settings.sellerReviewsEnabled,
                                            onCheckedChange = { enabled ->
                                                settings = settings.copy(sellerReviewsEnabled = enabled)
                                                coroutineScope.launch {
                                                    viewModel.repository.updateFeatureToggle("seller_reviews", enabled)
                                                }
                                            },
                                            testTag = "switch_platform_reviews"
                                        )
                                    }
                                }
                            }
                        }
                    }

                    "queues" -> {
                        item {
                            SuqiSectionCard(padding = 14.dp) {
                                SuqiListRow(
                                    icon = LucideIcons.ShieldCheck,
                                    title = strings.admin.adminVerificationTitle,
                                    subtitle = strings.adminGov.queueSellerVerif,
                                    badge = if (pendingShops.isNotEmpty()) pendingShops.size.toString() else strings.admin.clearBadge,
                                    badgeColor = if (pendingShops.isNotEmpty()) SuqiPrimary else SuqiSuccess,
                                    onClick = { viewModel.navigate(ScreenDestination.AdminFeatures) },
                                    testTag = "queue_row_verifications"
                                )
                                HorizontalDivider(color = colors.border)
                                SuqiListRow(
                                    icon = LucideIcons.Flag,
                                    title = strings.admin.moderationTitle,
                                    subtitle = strings.adminGov.queueFlaggedReviews,
                                    badge = if (flaggedReviews.isNotEmpty()) flaggedReviews.size.toString() else strings.admin.clearBadge,
                                    badgeColor = if (flaggedReviews.isNotEmpty()) SuqiError else SuqiSuccess,
                                    onClick = { viewModel.navigate(ScreenDestination.AdminModeration) },
                                    testTag = "queue_row_moderation"
                                )
                            }
                        }

                        // Live Support Tickets with Resolve button
                        item {
                            Text(
                                text = "${strings.admin.supportInboxTitle} (${openTickets.size})",
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = colors.primaryText
                            )
                        }
                        if (openTickets.isEmpty()) {
                            item {
                                SuqiSectionCard(padding = 18.dp) {
                                    SuqiEmptyState(
                                        icon = LucideIcons.CheckCircle,
                                        title = strings.admin.supportInboxTitle,
                                        subtitle = strings.adminGov.allClearLabel,
                                        testTag = "support_tickets_empty"
                                    )
                                }
                            }
                        } else {
                            items(openTickets, key = { it.id }) { ticket ->
                                val ticketAge = EthiopianMarketplaceUtils.formatWaitingAgeLabel(ticket.createdAt, strings)
                                SuqiSectionCard(padding = 14.dp) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = ticket.subject,
                                            fontFamily = SuqiFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = colors.primaryText,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (ticketAge != null) {
                                            SuqiStatusBadge(ticketAge, StatusType.WARNING)
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = ticket.category,
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        color = SuqiPrimary
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = ticket.description,
                                        fontFamily = SuqiFontFamily,
                                        fontSize = 12.sp,
                                        color = colors.secondaryText
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.End,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    val res = viewModel.repository.resolveSupportTicket(ticket.id)
                                                    if (res.success) {
                                                        viewModel.showToast(strings.adminGov.resolveBtn)
                                                        loadConsoleData()
                                                    } else {
                                                        viewModel.showToast(res.message ?: strings.retryLabel)
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = SuqiSuccess),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .testTag("resolve_ticket_${ticket.id}")
                                        ) {
                                            Icon(LucideIcons.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text(strings.adminGov.resolveBtn, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Open Disputes / Pending Payment Reviews
                        if (disputedOrders.isNotEmpty() || payoutRequests.isNotEmpty()) {
                            item {
                                Text(
                                    text = strings.admin.ordersDisputesTitle,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = colors.primaryText
                                )
                            }
                            items((disputedOrders + payoutRequests).distinctBy { it.id }, key = { "ord_${it.id}" }) { ord ->
                                SuqiSectionCard(
                                    padding = 14.dp,
                                    onClick = { viewModel.navigate(ScreenDestination.OrderDetails(ord.id)) }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "#${ord.orderReference} • ${ord.shopName}",
                                                fontFamily = SuqiFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = colors.primaryText
                                            )
                                            Text(
                                                text = "${ord.customerName} • ${formatEtb(ord.totalAmount)}",
                                                fontFamily = SuqiFontFamily,
                                                fontSize = 12.sp,
                                                color = colors.secondaryText
                                            )
                                        }
                                        SuqiStatusBadge(
                                            statusText = if (ord.paymentStatus == "REJECTED") strings.checkout.statusPaymentRejected else strings.checkout.statusPendingVerif,
                                            type = if (ord.paymentStatus == "REJECTED") StatusType.ERROR else StatusType.WARNING
                                        )
                                    }
                                }
                            }
                        }
                    }

                    "manage" -> {
                        item {
                            SuqiSectionCard(padding = 14.dp) {
                                if (isOwnerTier) {
                                    SuqiListRow(
                                        icon = LucideIcons.TrendingUp,
                                        title = strings.admin.ownerDashboardTitle,
                                        subtitle = strings.adminGov.openOwnerDashboardSub,
                                        onClick = { viewModel.navigate(ScreenDestination.OwnerDashboard) },
                                        testTag = "manage_owner_dashboard"
                                    )
                                    HorizontalDivider(color = colors.border)
                                }
                                SuqiListRow(
                                    icon = LucideIcons.LayoutGrid,
                                    title = strings.admin.categoriesManagerTitle,
                                    subtitle = strings.adminOps.categoriesTypesSub,
                                    onClick = { viewModel.navigate(ScreenDestination.AdminCategories) },
                                    testTag = "manage_categories"
                                )
                                HorizontalDivider(color = colors.border)
                                SuqiListRow(
                                    icon = LucideIcons.SlidersHorizontal,
                                    title = strings.admin.itemTypesManagerTitle,
                                    subtitle = strings.adminGov.sectionGroupLabel,
                                    onClick = { viewModel.navigate(ScreenDestination.AdminItemTypes) },
                                    testTag = "manage_item_types"
                                )
                                HorizontalDivider(color = colors.border)
                                SuqiListRow(
                                    icon = LucideIcons.GalleryHorizontal,
                                    title = strings.admin.bannersManagerTitle,
                                    subtitle = strings.adminOps.bannersSub,
                                    onClick = { viewModel.navigate(ScreenDestination.AdminBanners) },
                                    testTag = "manage_banners"
                                )
                                HorizontalDivider(color = colors.border)
                                SuqiListRow(
                                    icon = LucideIcons.Award,
                                    title = strings.admin.plansMonetizationTitle,
                                    subtitle = strings.adminOps.plansSub,
                                    onClick = { viewModel.navigate(ScreenDestination.AdminSubscriptionPlans) },
                                    testTag = "manage_plans"
                                )
                                HorizontalDivider(color = colors.border)
                                SuqiListRow(
                                    icon = LucideIcons.Wallet,
                                    title = strings.admin.financeTitle,
                                    subtitle = strings.phase5.payoutQueueTab,
                                    badge = if (pendingPayoutsCount > 0) pendingPayoutsCount.toString() else null,
                                    badgeColor = SuqiWarning,
                                    onClick = { viewModel.navigate(ScreenDestination.AdminFinance) },
                                    testTag = "manage_finance"
                                )
                                HorizontalDivider(color = colors.border)
                                SuqiListRow(
                                    icon = LucideIcons.Headphones,
                                    title = strings.admin.supportInboxTitle,
                                    subtitle = strings.phase5.inboxOpenTab,
                                    badge = if (openTickets.isNotEmpty()) openTickets.size.toString() else null,
                                    badgeColor = SuqiPrimary,
                                    onClick = { viewModel.navigate(ScreenDestination.AdminSupportInbox) },
                                    testTag = "manage_support_inbox"
                                )
                                HorizontalDivider(color = colors.border)
                                SuqiListRow(
                                    icon = LucideIcons.Send,
                                    title = strings.admin.broadcastTitle,
                                    subtitle = strings.adminGov.broadcastAudienceLabel,
                                    onClick = { viewModel.navigate(ScreenDestination.AdminBroadcast) },
                                    testTag = "manage_broadcast"
                                )
                                HorizontalDivider(color = colors.border)
                                SuqiListRow(
                                    icon = LucideIcons.MapPin,
                                    title = strings.admin.locationsTitle,
                                    subtitle = strings.adminGov.regionsHubsSubtitle,
                                    onClick = { showLocationsSheet = true },
                                    testTag = "manage_locations"
                                )
                                HorizontalDivider(color = colors.border)
                                SuqiListRow(
                                    icon = LucideIcons.FileText,
                                    title = strings.admin.contentManagerTitle,
                                    subtitle = strings.helpSupportTitle,
                                    onClick = { showContentSheet = true },
                                    testTag = "manage_content"
                                )
                            }
                        }
                    }

                    "tools" -> {
                        item {
                            SuqiSectionCard(padding = 14.dp) {
                                SuqiListRow(
                                    icon = LucideIcons.Wrench,
                                    title = strings.admin.devToolsTitle,
                                    subtitle = strings.adminOps.openDiagnosticsSub,
                                    onClick = { viewModel.navigate(ScreenDestination.AdminDevTools) },
                                    testTag = "tools_dev_diagnostics"
                                )
                                HorizontalDivider(color = colors.border)
                                SuqiListRow(
                                    icon = LucideIcons.Lock,
                                    title = strings.adminOps.lockSessionTitle,
                                    subtitle = strings.adminOps.lockSessionSub,
                                    onClick = {
                                        viewModel.lockAdminSession()
                                    },
                                    testTag = "tools_lock_session"
                                )
                                HorizontalDivider(color = colors.border)
                                SuqiListRow(
                                    icon = LucideIcons.House,
                                    title = strings.adminOps.returnBuyerTitle,
                                    subtitle = strings.adminOps.returnBuyerSub,
                                    onClick = {
                                        viewModel.setActiveRole("CUSTOMER")
                                        viewModel.popToRoot(ScreenDestination.Home)
                                    },
                                    testTag = "tools_return_buyer"
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBroadcastSheet) {
        AdminBroadcastSheet(
            onDismiss = { showBroadcastSheet = false },
            onSend = { title, body, audience ->
                coroutineScope.launch {
                    val res = viewModel.repository.sendAdminBroadcast(title, body, audience)
                    if (res.success) {
                        showBroadcastSheet = false
                        viewModel.refreshUnreadNotifications()
                        viewModel.showToast(strings.adminGov.sendBroadcastBtn)
                    } else {
                        viewModel.showToast(res.message ?: strings.retryLabel)
                    }
                }
            }
        )
    }

    if (showLocationsSheet) {
        AdminLocationsSheet(onDismiss = { showLocationsSheet = false })
    }

    if (showContentSheet) {
        AdminContentEditorSheet(
            context = context,
            onDismiss = { showContentSheet = false },
            onSaved = {
                showContentSheet = false
                viewModel.showToast(strings.saveLabel)
            }
        )
    }

    if (showIdleDialog) {
        AlertDialog(
            onDismissRequest = {
                showIdleDialog = false
                lastActivityMs = System.currentTimeMillis()
            },
            title = {
                Text(strings.adminOps.staffSessionTimeoutTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(strings.adminOps.staffSessionTimeoutBody, fontFamily = SuqiFontFamily, fontSize = 13.sp)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showIdleDialog = false
                        lastActivityMs = System.currentTimeMillis()
                    }
                ) {
                    Text(strings.adminOps.staySignedInBtn, color = SuqiPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showIdleDialog = false
                        viewModel.setActiveRole("CUSTOMER")
                        viewModel.popToRoot(ScreenDestination.Home)
                    }
                ) {
                    Text(strings.adminOps.signOutStaffBtn, color = SuqiError)
                }
            },
            containerColor = colors.cardBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

private data class QueueCardModel(
    val key: String,
    val title: String,
    val count: Int,
    val oldestAge: String?,
    val icon: ImageVector,
    val accent: Color,
    val onClick: () -> Unit
)

@Composable
private fun AdminQueueOverviewCard(
    model: QueueCardModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    SuqiSectionCard(
        padding = 14.dp,
        onClick = model.onClick,
        modifier = modifier.testTag("admin_queue_card_${model.key}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(model.accent.copy(alpha = 0.14f))
            ) {
                Icon(
                    imageVector = model.icon,
                    contentDescription = null,
                    tint = model.accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            if (model.count == 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.successBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = strings.admin.clearBadge,
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = SuqiSuccess
                    )
                }
            } else {
                Icon(
                    imageVector = LucideIcons.ChevronRight,
                    contentDescription = null,
                    tint = colors.mutedText,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = model.count.toString(),
            fontFamily = SuqiFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
            color = colors.primaryText
        )
        Text(
            text = model.title,
            fontFamily = SuqiFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = colors.secondaryText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (model.count > 0 && !model.oldestAge.isNullOrBlank()) {
                String.format(strings.adminGov.oldestItemTemplate, model.oldestAge)
            } else {
                strings.adminGov.allClearLabel
            },
            fontFamily = SuqiFontFamily,
            fontSize = 10.sp,
            color = if (model.count > 0) SuqiWarning else SuqiSuccess,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private data class QuickToolItem(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
private fun AdminQuickToolTile(
    item: QuickToolItem,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .width(72.dp)
            .heightIn(min = 82.dp, max = 82.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.secondaryBackground)
            .border(1.dp, colors.border, RoundedCornerShape(16.dp))
            .clickable(onClick = item.onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp)
            .testTag("admin_quick_tool_${item.key}")
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(colors.primaryTint)
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = SuqiPrimary,
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = item.label,
            fontFamily = SuqiFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            color = colors.primaryText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .heightIn(min = 30.dp, max = 30.dp)
                .padding(horizontal = 2.dp)
        )
    }
}

@Composable
private fun AdminBroadcastSheet(
    onDismiss: () -> Unit,
    onSend: (title: String, body: String, audience: String) -> Unit
) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var audience by remember { mutableStateOf("ALL") }

    val audiences = listOf(
        "ALL" to strings.adminGov.audienceAll,
        "SELLERS" to strings.shopsTab,
        "BUYERS" to strings.adminGov.newUsersLabel
    )

    SuqiBottomSheet(onDismissRequest = onDismiss, title = strings.admin.broadcastTitle) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            SuqiTextField(
                value = title,
                onValueChange = { title = it },
                label = strings.adminGov.broadcastTitleLabel,
                testTag = "broadcast_title_input"
            )
            SuqiTextField(
                value = body,
                onValueChange = { body = it },
                label = strings.adminGov.broadcastBodyLabel,
                singleLine = false,
                testTag = "broadcast_body_input"
            )
            Text(
                text = strings.adminGov.broadcastAudienceLabel,
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = colors.primaryText
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                audiences.forEach { (key, label) ->
                    val selected = audience == key
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (selected) SuqiPrimary else colors.secondaryBackground)
                            .border(1.dp, if (selected) SuqiPrimary else colors.border, RoundedCornerShape(999.dp))
                            .clickable { audience = key }
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = label,
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = if (selected) Color.White else colors.primaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            SuqiButton(
                text = strings.adminGov.sendBroadcastBtn,
                enabled = title.isNotBlank() && body.isNotBlank(),
                onClick = { onSend(title.trim(), body.trim(), audience) },
                modifier = Modifier.fillMaxWidth(),
                testTag = "broadcast_send_btn"
            )
        }
    }
}

@Composable
private fun AdminLocationsSheet(onDismiss: () -> Unit) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    SuqiBottomSheet(onDismissRequest = onDismiss, title = strings.admin.locationsTitle) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 460.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = strings.adminGov.regionsHubsSubtitle,
                fontFamily = SuqiFontFamily,
                fontSize = 12.sp,
                color = colors.secondaryText
            )
            Text(
                text = strings.buyer.regionLabel,
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = colors.primaryText
            )
            EthiopianMarketplaceUtils.ETHIOPIA_REGIONS.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    pair.forEach { region ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.secondaryBackground)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = placeLabel(region),
                                fontFamily = SuqiFontFamily,
                                fontSize = 12.sp,
                                color = colors.primaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            Text(
                text = strings.buyer.addisSubCityLabel,
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = colors.primaryText
            )
            EthiopianMarketplaceUtils.ADDIS_ABABA_SUB_CITIES.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    pair.forEach { sc ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.primaryTint)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = placeLabel(sc),
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = SuqiPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            Text(
                text = strings.buyer.commercialHubCityLabel,
                fontFamily = SuqiFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = colors.primaryText
            )
            EthiopianMarketplaceUtils.REGIONAL_COMMERCIAL_HUBS.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    pair.forEach { hub ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.secondaryBackground)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = placeLabel(hub),
                                fontFamily = SuqiFontFamily,
                                fontSize = 12.sp,
                                color = colors.primaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) {
                Text(strings.common.closeLabel, color = colors.primaryText, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun AdminContentEditorSheet(
    context: Context,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val prefs = remember { context.getSharedPreferences("suqi_content_prefs", Context.MODE_PRIVATE) }
    var supportPhone by remember { mutableStateOf(prefs.getString("support_phone", "+251 11 555 0100").orEmpty()) }
    var legalNotice by remember { mutableStateOf(prefs.getString("legal_notice", "").orEmpty()) }
    var faqQuestion by remember { mutableStateOf(prefs.getString("faq_custom_q", "").orEmpty()) }
    var faqAnswer by remember { mutableStateOf(prefs.getString("faq_custom_a", "").orEmpty()) }

    SuqiBottomSheet(onDismissRequest = onDismiss, title = strings.admin.contentManagerTitle) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            Text(
                text = strings.adminGov.contentEditorNote,
                fontFamily = SuqiFontFamily,
                fontSize = 12.sp,
                color = colors.secondaryText
            )
            SuqiTextField(
                value = supportPhone,
                onValueChange = { supportPhone = it },
                label = strings.buyer.phoneNumberLabel
            )
            SuqiTextField(
                value = legalNotice,
                onValueChange = { legalNotice = it },
                label = strings.buyer.termsTitle,
                singleLine = false
            )
            SuqiTextField(
                value = faqQuestion,
                onValueChange = { faqQuestion = it },
                label = strings.questionsAnswers
            )
            SuqiTextField(
                value = faqAnswer,
                onValueChange = { faqAnswer = it },
                label = strings.description,
                singleLine = false
            )
            SuqiButton(
                text = strings.saveLabel,
                onClick = {
                    prefs.edit()
                        .putString("support_phone", supportPhone.trim())
                        .putString("legal_notice", legalNotice.trim())
                        .putString("faq_custom_q", faqQuestion.trim())
                        .putString("faq_custom_a", faqAnswer.trim())
                        .apply()
                    onSaved()
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "content_editor_save_btn"
            )
        }
    }
}
