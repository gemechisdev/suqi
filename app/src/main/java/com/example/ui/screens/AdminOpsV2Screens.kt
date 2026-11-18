package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AdminBroadcastDto
import com.example.data.model.ShopDto
import com.example.data.model.SupportTicketDto
import com.example.data.model.SupportTicketMessageDto
import com.example.ui.SuqiViewModel
import com.example.ui.components.*
import com.example.ui.i18n.suqiStrings
import com.example.ui.theme.*
import com.example.util.EthiopianMarketplaceUtils
import kotlinx.coroutines.launch

@Composable
fun AdminSupportInboxScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val p5 = strings.phase5
    val isOffline by viewModel.isOffline.collectAsState()

    var tickets by remember { mutableStateOf(viewModel.cachedAdminTickets) }
    var isLoading by remember { mutableStateOf(tickets.isEmpty()) }
    var selectedTab by rememberSaveable { mutableStateOf("OPEN") }

    var activeTicket by remember { mutableStateOf<SupportTicketDto?>(null) }
    var ticketMessages by remember { mutableStateOf<List<SupportTicketMessageDto>>(emptyList()) }
    var replyDraft by remember { mutableStateOf("") }
    var isInternalNote by remember { mutableStateOf(false) }
    var resolutionNote by remember { mutableStateOf("") }

    val assignees = remember {
        listOf("Alex (Owner)", "Mekdes (Support Lead)", "Dawit (Verifications)")
    }

    BackHandler(enabled = activeTicket != null) {
        activeTicket = null
    }

    fun loadTickets() {
        coroutineScope.launch {
            isLoading = tickets.isEmpty()
            val res = viewModel.repository.getSupportTickets()
            if (res.success && res.data != null) {
                tickets = res.data
                viewModel.cachedAdminTickets = res.data
                activeTicket?.let { cur ->
                    activeTicket = res.data.firstOrNull { it.id == cur.id } ?: cur
                }
            }
            isLoading = false
        }
    }

    fun openTicket(t: SupportTicketDto) {
        activeTicket = t
        resolutionNote = t.resolutionSummary
        replyDraft = ""
        isInternalNote = false
        coroutineScope.launch {
            val res = viewModel.repository.getTicketMessages(t.id)
            if (res.success && res.data != null) {
                ticketMessages = res.data
            }
        }
    }

    LaunchedEffect(Unit) {
        loadTickets()
    }

    val filteredTickets = remember(tickets, selectedTab) {
        tickets.filter { t ->
            when (selectedTab) {
                "PENDING" -> t.status.equals("PENDING", true)
                "RESOLVED" -> t.status.equals("RESOLVED", true)
                else -> t.status.equals("OPEN", true)
            }
        }
    }

    SuqiScreen(
        title = strings.admin.supportInboxTitle,
        subtitle = "${filteredTickets.size}",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val inboxTabs = listOf(
                SuqiTabItem("OPEN", p5.inboxOpenTab),
                SuqiTabItem("PENDING", p5.inboxPendingTab),
                SuqiTabItem("RESOLVED", p5.inboxResolvedTab)
            )
            val inboxIdx = inboxTabs.indexOfFirst { it.key == selectedTab }.coerceAtLeast(0)
            SuqiTabs(
                tabs = inboxTabs,
                selectedIndex = inboxIdx,
                onSelectTab = { selectedTab = inboxTabs[it].key },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                testTagPrefix = "inbox_tab"
            )

            if (isLoading && tickets.isEmpty()) {
                SuqiSkeletonList(count = 3, modifier = Modifier.padding(top = 8.dp))
            } else if (filteredTickets.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    SuqiEmptyState(
                        icon = LucideIcons.Headphones,
                        title = strings.admin.supportInboxTitle,
                        subtitle = strings.admin.clearBadge,
                        testTag = "inbox_empty_state"
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTickets, key = { it.id }) { ticket ->
                        val age = EthiopianMarketplaceUtils.formatWaitingAgeLabel(ticket.createdAt, strings)
                        SuqiSectionCard(
                            padding = 14.dp,
                            onClick = { openTicket(ticket) },
                            modifier = Modifier.testTag("admin_ticket_card_${ticket.id}")
                        ) {
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
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                SuqiStatusBadge(
                                    statusText = ticket.priority,
                                    type = when (ticket.priority.uppercase()) {
                                        "HIGH" -> StatusType.ERROR
                                        "LOW" -> StatusType.NEUTRAL
                                        else -> StatusType.WARNING
                                    }
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                SuqiStatusBadge(ticket.requesterRole, StatusType.NEUTRAL)
                                Text(ticket.category, fontSize = 12.sp, color = SuqiPrimary, fontWeight = FontWeight.SemiBold)
                                if (age != null) {
                                    Text("• $age", fontSize = 11.sp, color = colors.mutedText)
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = ticket.description,
                                fontFamily = SuqiFontFamily,
                                fontSize = 12.sp,
                                color = colors.secondaryText,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (ticket.assignedTo.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "${p5.assignToLabel}: ${ticket.assignedTo}",
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = SuqiSuccess
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    activeTicket?.let { ticket ->
        SuqiBottomSheet(onDismissRequest = { activeTicket = null }, title = ticket.subject) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 540.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("OPEN", "PENDING", "RESOLVED").forEach { st ->
                        val sel = ticket.status.equals(st, true)
                        FilterChip(
                            selected = sel,
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.repository.updateSupportTicket(ticket.id, status = st)
                                    loadTickets()
                                }
                            },
                            label = { Text(st, fontSize = 11.sp) },
                            modifier = Modifier.testTag("ticket_status_$st")
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("LOW", "NORMAL", "HIGH").forEach { pr ->
                        val sel = ticket.priority.equals(pr, true)
                        FilterChip(
                            selected = sel,
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.repository.updateSupportTicket(ticket.id, priority = pr)
                                    loadTickets()
                                }
                            },
                            label = { Text(pr, fontSize = 11.sp) },
                            modifier = Modifier.testTag("ticket_priority_$pr")
                        )
                    }
                }

                Text(
                    text = p5.assignToLabel,
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = colors.secondaryText
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    assignees.forEach { staff ->
                        val sel = ticket.assignedTo == staff
                        FilterChip(
                            selected = sel,
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.repository.updateSupportTicket(ticket.id, assignedTo = staff)
                                    loadTickets()
                                }
                            },
                            label = { Text(staff, fontSize = 11.sp) },
                            modifier = Modifier.testTag("ticket_assign_${staff.substringBefore(" ").lowercase()}")
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.secondaryBackground)
                        .padding(10.dp)
                ) {
                    Text(ticket.description, fontSize = 13.sp, color = colors.primaryText)
                }

                ticketMessages.forEach { msg ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (msg.isInternalNote) colors.warningBg else colors.cardBackground)
                            .border(1.dp, if (msg.isInternalNote) SuqiWarning else colors.border, RoundedCornerShape(12.dp))
                            .padding(10.dp)
                            .testTag("ticket_thread_msg_${msg.id}")
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${msg.senderName} (${msg.senderRole})",
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (msg.isInternalNote) SuqiWarning else SuqiPrimary
                                )
                                if (msg.isInternalNote) {
                                    SuqiStatusBadge(p5.internalNoteBadge, StatusType.WARNING)
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(msg.message, fontSize = 13.sp, color = colors.primaryText)
                        }
                    }
                }

                val quickReplies = listOf(p5.quickReplyAck, p5.quickReplyVerify, p5.quickReplyResolved)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    quickReplies.forEachIndexed { idx, text ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(colors.primaryTint)
                                .clickable { replyDraft = text }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("quick_reply_$idx")
                        ) {
                            Text(text, fontSize = 11.sp, color = SuqiPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                SuqiSwitchRow(
                    title = p5.internalNoteToggle,
                    checked = isInternalNote,
                    onCheckedChange = { isInternalNote = it },
                    modifier = Modifier.testTag("ticket_internal_note_switch")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                    SuqiTextField(
                        value = replyDraft,
                        onValueChange = { replyDraft = it },
                        label = strings.buyerWork.ticketReplyHint,
                        modifier = Modifier.weight(1f),
                        testTag = "admin_ticket_reply_input"
                    )
                    SuqiButton(
                        text = strings.buyerWork.ticketSendBtn,
                        enabled = replyDraft.isNotBlank(),
                        onClick = {
                            coroutineScope.launch {
                                val res = viewModel.repository.sendTicketMessage(
                                    ticketId = ticket.id,
                                    message = replyDraft.trim(),
                                    isInternalNote = isInternalNote,
                                    senderRole = "ADMIN"
                                )
                                if (res.success) {
                                    replyDraft = ""
                                    ticketMessages = viewModel.repository.getTicketMessages(ticket.id).data ?: ticketMessages
                                    loadTickets()
                                }
                            }
                        },
                        testTag = "admin_ticket_send_btn"
                    )
                }

                HorizontalDivider(color = colors.border)

                SuqiTextField(
                    value = resolutionNote,
                    onValueChange = { resolutionNote = it },
                    label = p5.resolutionSummaryHint,
                    testTag = "ticket_resolution_input"
                )
                SuqiButton(
                    text = strings.adminGov.resolveBtn,
                    icon = LucideIcons.Check,
                    onClick = {
                        coroutineScope.launch {
                            val res = viewModel.repository.updateSupportTicket(
                                ticketId = ticket.id,
                                status = "RESOLVED",
                                resolutionSummary = resolutionNote.trim()
                            )
                            if (res.success) {
                                activeTicket = null
                                loadTickets()
                                viewModel.showToast(strings.adminGov.resolveBtn)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "admin_ticket_resolve_btn"
                )
            }
        }
    }
}

@Composable
fun AdminBroadcastScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val p5 = strings.phase5
    val isOffline by viewModel.isOffline.collectAsState()

    var selectedTab by rememberSaveable { mutableStateOf("COMPOSE") }
    var broadcasts by remember { mutableStateOf(viewModel.cachedAdminBroadcasts) }
    var shops by remember { mutableStateOf<List<ShopDto>>(emptyList()) }

    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var audienceKey by remember { mutableStateOf("ALL") }
    var selectedShopId by remember { mutableStateOf("shop_techhub") }
    var selectedCity by remember { mutableStateOf("Addis Ababa") }
    var imageUrl by remember { mutableStateOf("") }
    var destinationType by remember { mutableStateOf("NONE") }
    var targetId by remember { mutableStateOf("") }
    var scheduleDate by remember { mutableStateOf("") }
    var scheduleTime by remember { mutableStateOf("") }

    fun loadData() {
        coroutineScope.launch {
            val res = viewModel.repository.getAdminBroadcasts()
            if (res.success && res.data != null) {
                broadcasts = res.data
                viewModel.cachedAdminBroadcasts = res.data
            }
            viewModel.repository.getShops().data?.let { list ->
                shops = list
                if (list.isNotEmpty() && list.none { it.id == selectedShopId }) {
                    selectedShopId = list.first().id
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    val resolvedAudience = when (audienceKey) {
        "FOLLOWERS" -> "FOLLOWERS:$selectedShopId"
        "CITY" -> "CITY:$selectedCity"
        else -> audienceKey
    }

    val historyItems = remember(broadcasts) { broadcasts.filter { !it.isTemplate } }
    val templateItems = remember(broadcasts) { broadcasts.filter { it.isTemplate } }

    SuqiScreen(
        title = strings.admin.broadcastTitle,
        subtitle = "${historyItems.size}",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val bcTabs = listOf(
                SuqiTabItem("COMPOSE", strings.adminGov.sendBroadcastBtn),
                SuqiTabItem("HISTORY", p5.broadcastHistoryTab),
                SuqiTabItem("TEMPLATES", p5.broadcastTemplatesTab)
            )
            val bcIdx = bcTabs.indexOfFirst { it.key == selectedTab }.coerceAtLeast(0)
            SuqiTabs(
                tabs = bcTabs,
                selectedIndex = bcIdx,
                onSelectTab = { selectedTab = bcTabs[it].key },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                testTagPrefix = "broadcast_tab"
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (selectedTab) {
                    "COMPOSE" -> {
                        item {
                            SuqiSectionCard {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = strings.adminGov.broadcastAudienceLabel,
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = colors.primaryText
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.horizontalScroll(rememberScrollState())
                                    ) {
                                        listOf(
                                            "ALL" to strings.adminGov.audienceAll,
                                            "BUYERS" to p5.audienceBuyers,
                                            "SELLERS" to strings.shopsTab,
                                            "FOLLOWERS" to p5.audienceShopFollowers,
                                            "CITY" to p5.audienceCityFilter
                                        ).forEach { (key, label) ->
                                            val sel = audienceKey == key
                                            FilterChip(
                                                selected = sel,
                                                onClick = { audienceKey = key },
                                                label = { Text(label, fontSize = 12.sp) },
                                                modifier = Modifier.testTag("bc_audience_$key")
                                            )
                                        }
                                    }

                                    if (audienceKey == "FOLLOWERS" && shops.isNotEmpty()) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.horizontalScroll(rememberScrollState())
                                        ) {
                                            shops.forEach { shop ->
                                                FilterChip(
                                                    selected = selectedShopId == shop.id,
                                                    onClick = { selectedShopId = shop.id },
                                                    label = { Text(shop.name, fontSize = 12.sp) }
                                                )
                                            }
                                        }
                                    } else if (audienceKey == "CITY") {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.horizontalScroll(rememberScrollState())
                                        ) {
                                            listOf("Addis Ababa", "Adama", "Hawassa", "Bahir Dar").forEach { city ->
                                                FilterChip(
                                                    selected = selectedCity == city,
                                                    onClick = { selectedCity = city },
                                                    label = { Text(city, fontSize = 12.sp) }
                                                )
                                            }
                                        }
                                    }

                                    SuqiTextField(
                                        value = title,
                                        onValueChange = { title = it },
                                        label = strings.adminGov.broadcastTitleLabel,
                                        testTag = "bc_title_input"
                                    )
                                    SuqiTextField(
                                        value = body,
                                        onValueChange = { body = it },
                                        label = strings.adminGov.broadcastBodyLabel,
                                        singleLine = false,
                                        testTag = "bc_body_input"
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        SuqiImagePickerTile(
                                            label = p5.attachImageLabel,
                                            imageUri = imageUrl.takeIf { it.isNotBlank() },
                                            onImagePicked = { imageUrl = it },
                                            onRemove = if (imageUrl.isNotBlank()) { { imageUrl = "" } } else null,
                                            modifier = Modifier.width(120.dp),
                                            testTag = "bc_image_picker"
                                        )
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(strings.adminGov.destinationTypeLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.secondaryText)
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.horizontalScroll(rememberScrollState())
                                            ) {
                                                listOf("NONE", "DEALS", "CATEGORY", "SHOP").forEach { dt ->
                                                    FilterChip(
                                                        selected = destinationType == dt,
                                                        onClick = { destinationType = dt },
                                                        label = { Text(dt, fontSize = 11.sp) }
                                                    )
                                                }
                                            }
                                            if (destinationType == "CATEGORY" || destinationType == "SHOP") {
                                                SuqiTextField(
                                                    value = targetId,
                                                    onValueChange = { targetId = it },
                                                    label = p5.broadcastTargetIdLabel
                                                )
                                            }
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        SuqiDateField(
                                            label = p5.scheduleDateLabel,
                                            dateText = scheduleDate,
                                            onDateSelected = { scheduleDate = it },
                                            modifier = Modifier.weight(1f).testTag("bc_schedule_date")
                                        )
                                        SuqiTimeField(
                                            label = p5.scheduleTimeLabel,
                                            timeText = scheduleTime,
                                            onTimeSelected = { scheduleTime = it },
                                            modifier = Modifier.weight(1f).testTag("bc_schedule_time")
                                        )
                                    }
                                }
                            }
                        }

                        // Live Phone Notification Preview
                        item {
                            SuqiSectionCard(
                                containerColor = colors.secondaryBackground,
                                modifier = Modifier.testTag("bc_phone_preview")
                            ) {
                                Text(
                                    text = p5.phonePreviewLabel,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = SuqiPrimary
                                )
                                Spacer(Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(colors.cardBackground)
                                        .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(SuqiPrimary)
                                        ) {
                                            Icon(LucideIcons.Bell, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = title.ifBlank { strings.welcomeToSuqi },
                                                fontFamily = SuqiFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = colors.primaryText
                                            )
                                            Text(
                                                text = body.ifBlank { strings.adminGov.broadcastBodyLabel },
                                                fontFamily = SuqiFontFamily,
                                                fontSize = 12.sp,
                                                color = colors.secondaryText,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (imageUrl.isNotBlank()) {
                                            Spacer(Modifier.width(8.dp))
                                            AsyncImage(
                                                model = imageUrl,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val scheduledAt = listOfNotNull(
                                    scheduleDate.takeIf { it.isNotBlank() },
                                    scheduleTime.takeIf { it.isNotBlank() }
                                ).joinToString(" ")
                                SuqiButton(
                                    text = strings.adminGov.sendBroadcastBtn,
                                    icon = LucideIcons.Send,
                                    enabled = title.isNotBlank() && body.isNotBlank(),
                                    onClick = {
                                        coroutineScope.launch {
                                            val res = viewModel.repository.sendAdminBroadcastExtended(
                                                mapOf(
                                                    "title" to title.trim(),
                                                    "body" to body.trim(),
                                                    "audience" to resolvedAudience,
                                                    "imageUrl" to imageUrl,
                                                    "destinationType" to destinationType,
                                                    "targetId" to targetId.trim(),
                                                    "scheduledAt" to scheduledAt
                                                )
                                            )
                                            if (res.success) {
                                                viewModel.showToast(String.format(p5.broadcastSentTemplate, res.data ?: 1))
                                                title = ""
                                                body = ""
                                                loadData()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    testTag = "bc_send_btn"
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                val res = viewModel.repository.sendAdminBroadcastExtended(
                                                    mapOf(
                                                        "title" to title.trim(),
                                                        "body" to body.trim(),
                                                        "audience" to resolvedAudience,
                                                        "imageUrl" to imageUrl,
                                                        "destinationType" to destinationType,
                                                        "targetId" to targetId.trim(),
                                                        "testToSelf" to "true"
                                                    )
                                                )
                                                if (res.success) {
                                                    viewModel.showToast(strings.adminGov.sendTestToMeBtn)
                                                    loadData()
                                                }
                                            }
                                        },
                                        enabled = title.isNotBlank() && body.isNotBlank(),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("bc_send_test_btn")
                                    ) {
                                        Text(strings.adminGov.sendTestToMeBtn, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                val res = viewModel.repository.sendAdminBroadcastExtended(
                                                    mapOf(
                                                        "title" to title.trim(),
                                                        "body" to body.trim(),
                                                        "audience" to resolvedAudience,
                                                        "imageUrl" to imageUrl,
                                                        "destinationType" to destinationType,
                                                        "targetId" to targetId.trim(),
                                                        "isTemplate" to "true"
                                                    )
                                                )
                                                if (res.success) {
                                                    viewModel.showToast(p5.saveTemplateBtn)
                                                    loadData()
                                                }
                                            }
                                        },
                                        enabled = title.isNotBlank() && body.isNotBlank(),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("bc_save_template_btn")
                                    ) {
                                        Text(p5.saveTemplateBtn, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }

                    "HISTORY" -> {
                        if (historyItems.isEmpty()) {
                            item {
                                SuqiEmptyState(
                                    icon = LucideIcons.Bell,
                                    title = p5.broadcastHistoryTab,
                                    subtitle = strings.admin.clearBadge
                                )
                            }
                        } else {
                            items(historyItems, key = { it.id }) { bc ->
                                SuqiSectionCard(padding = 14.dp, modifier = Modifier.testTag("bc_history_${bc.id}")) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(bc.title, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primaryText, modifier = Modifier.weight(1f))
                                        SuqiStatusBadge(
                                            statusText = bc.status,
                                            type = when (bc.status) {
                                                "SENT" -> StatusType.SUCCESS
                                                "SCHEDULED" -> StatusType.WARNING
                                                else -> StatusType.NEUTRAL
                                            }
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(bc.body, fontSize = 13.sp, color = colors.secondaryText)
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = "${bc.audience} • ${String.format(p5.broadcastStatsTemplate, bc.sentCount, bc.openedCount)}",
                                        fontSize = 11.sp,
                                        color = colors.mutedText
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        if (templateItems.isEmpty()) {
                            item {
                                SuqiEmptyState(
                                    icon = LucideIcons.FileText,
                                    title = p5.broadcastTemplatesTab,
                                    subtitle = p5.saveTemplateBtn
                                )
                            }
                        } else {
                            items(templateItems, key = { it.id }) { tpl ->
                                SuqiSectionCard(
                                    padding = 14.dp,
                                    onClick = {
                                        title = tpl.title
                                        body = tpl.body
                                        imageUrl = tpl.imageUrl
                                        destinationType = tpl.destinationType
                                        targetId = tpl.targetId
                                        when {
                                            tpl.audience.startsWith("FOLLOWERS:") -> {
                                                audienceKey = "FOLLOWERS"
                                                selectedShopId = tpl.audience.removePrefix("FOLLOWERS:")
                                            }
                                            tpl.audience.startsWith("CITY:") -> {
                                                audienceKey = "CITY"
                                                selectedCity = tpl.audience.removePrefix("CITY:")
                                            }
                                            else -> audienceKey = tpl.audience.ifBlank { "ALL" }
                                        }
                                        selectedTab = "COMPOSE"
                                    },
                                    modifier = Modifier.testTag("bc_template_${tpl.id}")
                                ) {
                                    Text(tpl.title, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primaryText)
                                    Spacer(Modifier.height(4.dp))
                                    Text(tpl.body, fontSize = 12.sp, color = colors.secondaryText, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Spacer(Modifier.height(6.dp))
                                    Text(tpl.audience, fontSize = 11.sp, color = SuqiPrimary, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
