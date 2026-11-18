package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SupportTicketDto
import com.example.data.model.SupportTicketMessageDto
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.*
import com.example.ui.i18n.suqiStrings
import com.example.ui.theme.*
import com.example.util.EthiopianMarketplaceUtils
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class HelpArticle(val topic: String, val question: String, val answer: String)

data class TicketMessage(val fromUser: Boolean, val text: String, val time: String)

/** Locally stored ticket replies (the demo backend has no thread API). */
class TicketThreadStore(context: android.content.Context) {
    private val prefs = context.applicationContext.getSharedPreferences("suqi_ticket_threads", android.content.Context.MODE_PRIVATE)

    fun load(ticketId: String): List<TicketMessage> {
        val raw = prefs.getString("thread_$ticketId", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                TicketMessage(o.optBoolean("u", true), o.optString("t"), o.optString("ts"))
            }
        }.getOrDefault(emptyList())
    }

    fun save(ticketId: String, messages: List<TicketMessage>) {
        val arr = JSONArray()
        messages.forEach { arr.put(JSONObject().put("u", it.fromUser).put("t", it.text).put("ts", it.time)) }
        prefs.edit().putString("thread_$ticketId", arr.toString()).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}

@Composable
fun HelpLegalScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val w = strings.buyerWork
    val isOffline by viewModel.isOffline.collectAsState()
    var activeDocument by remember { mutableStateOf<EthiopianMarketplaceUtils.LegalDocument?>(null) }
    var showTicketSheet by remember { mutableStateOf(false) }
    var tickets by remember { mutableStateOf<List<SupportTicketDto>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var selectedTopic by remember { mutableStateOf<String?>(null) }
    var expandedQuestion by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val contentPrefs = remember { context.getSharedPreferences("suqi_content_prefs", android.content.Context.MODE_PRIVATE) }
    val supportPhone = remember { contentPrefs.getString("support_phone", "+251 912 345 678").orEmpty().ifBlank { "+251 912 345 678" } }
    val telegramLink = remember { contentPrefs.getString("support_telegram", "https://t.me/suqi_support").orEmpty().ifBlank { "https://t.me/suqi_support" } }
    val customNotice = remember { contentPrefs.getString("legal_notice", "").orEmpty() }
    val customFaqTitle = remember { contentPrefs.getString("faq_custom_q", "").orEmpty() }
    val customFaqAnswer = remember { contentPrefs.getString("faq_custom_a", "").orEmpty() }

    val topics = remember(strings) {
        listOf(
            Triple("orders", w.helpTopicOrders, LucideIcons.Package),
            Triple("payments", w.helpTopicPayments, LucideIcons.Wallet),
            Triple("delivery", w.helpTopicDelivery, LucideIcons.Truck),
            Triple("returns", w.helpTopicReturns, LucideIcons.RotateCcw),
            Triple("account", w.helpTopicAccount, LucideIcons.User),
            Triple("sellers", w.helpTopicSellers, LucideIcons.Store)
        )
    }
    val articles = remember(strings) {
        buildList {
            if (customFaqTitle.isNotBlank() && customFaqAnswer.isNotBlank()) add(HelpArticle("account", customFaqTitle, customFaqAnswer))
            add(HelpArticle("orders", w.faqOrdersQ, w.faqOrdersA))
            add(HelpArticle("payments", w.faqPaymentsQ, w.faqPaymentsA))
            add(HelpArticle("delivery", w.faqDeliveryQ, w.faqDeliveryA))
            add(HelpArticle("returns", w.faqReturnsQ, w.faqReturnsA))
            add(HelpArticle("account", w.faqAccountQ, w.faqAccountA))
            add(HelpArticle("sellers", w.faqSellersQ, w.faqSellersA))
        }
    }
    val visibleArticles = remember(query, selectedTopic, articles) {
        val q = query.trim()
        articles.filter { a ->
            (selectedTopic == null || a.topic == selectedTopic) &&
                (q.isBlank() || a.question.contains(q, ignoreCase = true) || a.answer.contains(q, ignoreCase = true))
        }
    }
    val showArticles = selectedTopic != null || query.isNotBlank()

    fun loadTickets() {
        coroutineScope.launch {
            val res = viewModel.repository.getSupportTickets()
            if (res.success && res.data != null) tickets = res.data
        }
    }
    LaunchedEffect(Unit) { loadTickets() }

    SuqiScreen(
        title = strings.helpSupportTitle,
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SuqiSearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = strings.buyer.helpSearchHint,
                    testTag = "help_search_field"
                )
            }

            if (customNotice.isNotBlank()) {
                item {
                    SuqiSectionCard(containerColor = colors.primaryTint, padding = 12.dp) {
                        Text(customNotice, fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.primaryText, lineHeight = 17.sp)
                    }
                }
            }

            // 2x3 topic grid
            item {
                Text(w.helpTopicsTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText)
            }
            items(topics.chunked(2)) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { (key, label, icon) ->
                        TopicCard(
                            label = label,
                            icon = icon,
                            selected = selectedTopic == key,
                            onClick = { selectedTopic = if (selectedTopic == key) null else key },
                            modifier = Modifier.weight(1f).testTag("help_topic_$key")
                        )
                    }
                }
            }

            if (showArticles) {
                if (selectedTopic != null) {
                    item {
                        TextButton(onClick = { selectedTopic = null }, modifier = Modifier.testTag("help_all_topics")) {
                            Text(w.helpAllTopics, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, color = SuqiPrimary)
                        }
                    }
                }
                if (visibleArticles.isEmpty()) {
                    item {
                        SuqiEmptyState(
                            icon = LucideIcons.SearchX,
                            title = w.helpNoResults,
                            subtitle = w.helpNoResultsSub,
                            testTag = "help_no_results"
                        )
                    }
                } else {
                    items(visibleArticles, key = { it.question }) { art ->
                        val expanded = expandedQuestion == art.question || query.isNotBlank()
                        SuqiSectionCard(
                            containerColor = colors.secondaryBackground,
                            padding = 14.dp,
                            onClick = { expandedQuestion = if (expandedQuestion == art.question) null else art.question },
                            modifier = Modifier.testTag("help_article_${art.topic}")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(art.question, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = SuqiPrimary, modifier = Modifier.weight(1f))
                                Icon(if (expanded) LucideIcons.ChevronUp else LucideIcons.ChevronDown, contentDescription = null, tint = colors.mutedText, modifier = Modifier.size(18.dp))
                            }
                            if (expanded) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(art.answer, fontFamily = SuqiFontFamily, fontSize = 13.sp, lineHeight = 19.sp, color = colors.primaryText)
                            }
                        }
                    }
                }
            }

            // Contact card
            item {
                SuqiSectionCard(containerColor = colors.primaryTint, padding = 18.dp) {
                    Text(w.contactCardTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.primaryText)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(strings.buyer.contactUsTitle, fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.secondaryText)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        SuqiButton(
                            text = w.contactInAppBtn,
                            icon = LucideIcons.MessageSquarePlus,
                            onClick = { showTicketSheet = true },
                            testTag = "help_submit_ticket_button",
                            modifier = Modifier.weight(1f)
                        )
                        SuqiButton(
                            text = w.contactCallBtn,
                            isPrimary = false,
                            icon = LucideIcons.Phone,
                            onClick = {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + supportPhone.filter { it.isDigit() || it == '+' })))
                                }.onFailure { viewModel.showToast(supportPhone) }
                            },
                            testTag = "help_call_button",
                            modifier = Modifier.weight(1f)
                        )
                        SuqiButton(
                            text = w.contactTelegramBtn,
                            isPrimary = false,
                            icon = LucideIcons.Send,
                            onClick = {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(telegramLink)))
                                }.onFailure { viewModel.showToast(telegramLink) }
                            },
                            testTag = "help_telegram_button",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // My tickets: tap opens the thread view
            if (tickets.isNotEmpty()) {
                item {
                    SuqiSectionCard(padding = 12.dp) {
                        Text(
                            text = "${strings.buyer.myTicketsTitle} (${tickets.size})",
                            fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.primaryText,
                            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                        )
                        tickets.forEach { t ->
                            SuqiListRow(
                                icon = LucideIcons.MessageCircle,
                                title = t.subject,
                                subtitle = "${t.category} • ${t.createdAt}",
                                badge = t.status,
                                badgeColor = if (t.status == "OPEN") SuqiWarning else SuqiSuccess,
                                onClick = { viewModel.navigate(ScreenDestination.TicketThread(t.id)) },
                                testTag = "help_ticket_${t.id}"
                            )
                        }
                    }
                }
            }

            item {
                SuqiSectionCard(padding = 10.dp) {
                    SuqiListRow(
                        icon = LucideIcons.FileText,
                        title = strings.buyer.termsTitle,
                        subtitle = strings.extras.legalProclamationSubtitle,
                        onClick = { activeDocument = EthiopianMarketplaceUtils.TERMS_OF_SERVICE },
                        testTag = "help_terms_row"
                    )
                    SuqiListRow(
                        icon = LucideIcons.Shield,
                        title = strings.buyer.privacyPolicyTitle,
                        subtitle = strings.buyer.privacyTitle,
                        onClick = { activeDocument = EthiopianMarketplaceUtils.PRIVACY_POLICY }
                    )
                    SuqiListRow(
                        icon = LucideIcons.Handshake,
                        title = strings.seller.sellerVerificationTitle,
                        subtitle = strings.seller.studioTitle,
                        onClick = { activeDocument = EthiopianMarketplaceUtils.SELLER_AGREEMENT }
                    )
                    SuqiListRow(
                        icon = LucideIcons.Info,
                        title = strings.buyer.aboutSuqiTitle,
                        subtitle = w.aboutScreenSub,
                        onClick = { viewModel.navigate(ScreenDestination.AboutApp) }
                    )
                }
            }
        }
    }

    if (showTicketSheet) {
        var category by remember { mutableStateOf("Order Issue") }
        var subject by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        SuqiBottomSheet(onDismissRequest = { showTicketSheet = false }, title = strings.checkout.submitHelpTicketTitle) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(strings.checkout.issueCategoryLabel, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.secondaryText)
                val cats = listOf(
                    "Order Issue" to strings.buyer.ordersTabLabel,
                    "Payment" to strings.checkout.paymentMethodTitle,
                    "Delivery" to strings.checkout.stepDelivery,
                    "Account" to strings.accountTitle
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    cats.forEach { (key, label) ->
                        val sel = key == category
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (sel) SuqiPrimary else colors.secondaryBackground)
                                .clickable { category = key }
                                .defaultMinSize(minHeight = 48.dp)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (sel) Color.White else colors.primaryText)
                        }
                    }
                }
                SuqiTextField(value = subject, onValueChange = { subject = it }, label = "${strings.checkout.issueCategoryLabel} *")
                SuqiTextField(value = description, onValueChange = { description = it }, label = strings.checkout.messageDetailsRequiredLabel, singleLine = false)
                SuqiButton(
                    text = strings.checkout.submitTicketBtn,
                    onClick = {
                        if (subject.isBlank() || description.isBlank()) {
                            viewModel.showToast(strings.checkout.messageDetailsRequiredLabel)
                            return@SuqiButton
                        }
                        coroutineScope.launch {
                            viewModel.repository.submitSupportTicket(
                                SupportTicketDto(id = "", userId = "", category = category, subject = subject.trim(), description = description.trim(), status = "OPEN", createdAt = "")
                            )
                            showTicketSheet = false
                            loadTickets()
                            viewModel.showToast(strings.checkout.submitTicketBtn)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    activeDocument?.let { doc ->
        val listState = rememberLazyListState()
        AlertDialog(
            onDismissRequest = { activeDocument = null },
            title = {
                Column {
                    Text(doc.title, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.primaryText)
                    Text(doc.subtitle, fontSize = 12.sp, color = colors.secondaryText)
                }
            },
            text = {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Table of contents: tap a heading to jump to it
                    item {
                        Column(modifier = Modifier.testTag("legal_toc")) {
                            Text(w.legalContentsTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.mutedText)
                            Spacer(modifier = Modifier.height(4.dp))
                            doc.sections.forEachIndexed { i, sec ->
                                Text(
                                    text = sec.heading,
                                    fontFamily = SuqiFontFamily,
                                    fontSize = 13.sp,
                                    color = SuqiPrimary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { coroutineScope.launch { listState.animateScrollToItem(i + 1) } }
                                        .defaultMinSize(minHeight = 40.dp)
                                        .padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                    itemsIndexed(doc.sections) { _, sec ->
                        Column {
                            Text(sec.heading, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = SuqiPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            // 15sp body at 1.5 line height
                            Text(sec.content, fontFamily = SuqiFontFamily, fontSize = 15.sp, lineHeight = 22.5.sp, color = colors.primaryText)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeDocument = null }) {
                    Text(strings.common.closeLabel, color = SuqiPrimary, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = colors.cardBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun TopicCard(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(colors.primaryTint)
            .then(if (selected) Modifier.border(2.dp, SuqiPrimary, RoundedCornerShape(20.dp)) else Modifier)
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = 64.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(label, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.primaryText, maxLines = 2)
    }
}

/** B-17 ticket thread: chat-style view with status pill and reply box. */
@Composable
fun TicketThreadScreen(
    ticketId: String,
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val w = strings.buyerWork
    val context = LocalContext.current
    val isOffline by viewModel.isOffline.collectAsState()
    val store = remember { TicketThreadStore(context) }
    var ticket by remember { mutableStateOf<SupportTicketDto?>(null) }
    var backendMessages by remember { mutableStateOf<List<SupportTicketMessageDto>>(emptyList()) }
    var replies by remember { mutableStateOf(store.load(ticketId)) }
    var draft by remember { mutableStateOf("") }

    LaunchedEffect(ticketId) {
        val res = viewModel.repository.getSupportTickets()
        if (res.success && res.data != null) ticket = res.data.firstOrNull { it.id == ticketId }
        val msgRes = viewModel.repository.getTicketMessages(ticketId)
        if (msgRes.success && msgRes.data != null) backendMessages = msgRes.data
    }

    val t = ticket
    val isResolved = t != null && t.status != "OPEN"
    val messages = remember(t, backendMessages, replies) {
        buildList {
            if (t != null) add(TicketMessage(true, t.description, t.createdAt))
            val backendAsLocal = backendMessages
                .filter { !it.isInternalNote }
                .map { msg ->
                    TicketMessage(
                        fromUser = msg.senderRole.equals("BUYER", true) || msg.senderRole.equals("SELLER", true),
                        text = msg.message,
                        time = msg.createdAt
                    )
                }
            addAll(backendAsLocal)
            replies.forEach { localMsg ->
                if (backendAsLocal.none { it.text == localMsg.text && it.fromUser == localMsg.fromUser }) {
                    add(localMsg)
                }
            }
        }
    }

    SuqiScreen(
        title = t?.subject ?: w.ticketThreadTitle,
        subtitle = t?.category,
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        headerActions = {
            if (t != null) SuqiStatusBadge(t.status, if (t.status == "OPEN") StatusType.WARNING else StatusType.SUCCESS)
        },
        bottomBar = {
            Surface(color = colors.cardBackground, modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)) {
                if (isResolved) {
                    Text(
                        w.ticketResolvedNote,
                        fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.secondaryText, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("ticket_resolved_note")
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(12.dp)
                    ) {
                        SuqiTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            label = "",
                            placeholder = w.ticketReplyHint,
                            modifier = Modifier.weight(1f),
                            testTag = "ticket_reply_input"
                        )
                        SuqiButton(
                            text = w.ticketSendBtn,
                            icon = LucideIcons.Send,
                            enabled = draft.isNotBlank() && t != null,
                            onClick = {
                                val text = draft.trim()
                                val now = java.text.SimpleDateFormat("MMM d, HH:mm", java.util.Locale.US).format(java.util.Date())
                                val next = replies + TicketMessage(true, text, now) +
                                    TicketMessage(false, w.ticketAutoReply, now)
                                replies = next
                                store.save(ticketId, next)
                                draft = ""
                                coroutineScope.launch {
                                    viewModel.repository.sendTicketMessage(
                                        ticketId = ticketId,
                                        message = text,
                                        isInternalNote = false,
                                        senderRole = "BUYER"
                                    )
                                    viewModel.repository.getTicketMessages(ticketId).data?.let { backendMessages = it }
                                }
                            },
                            testTag = "ticket_send_btn"
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) { padding ->
        if (t == null) {
            SuqiSkeletonList(count = 3, modifier = Modifier.padding(padding).padding(top = 12.dp))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages.size) { i ->
                    val m = messages[i]
                    Column(
                        horizontalAlignment = if (m.fromUser) Alignment.End else Alignment.Start,
                        modifier = Modifier.fillMaxWidth().testTag("ticket_msg_$i")
                    ) {
                        Text(
                            if (m.fromUser) w.ticketYouLabel else w.ticketSupportLabel,
                            fontFamily = SuqiFontFamily, fontSize = 11.sp, color = colors.mutedText,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        Box(
                            modifier = Modifier
                                .widthIn(max = 300.dp)
                                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = if (m.fromUser) 20.dp else 4.dp, bottomEnd = if (m.fromUser) 4.dp else 20.dp))
                                .background(if (m.fromUser) SuqiPrimary else colors.cardBackground)
                                .then(if (m.fromUser) Modifier else Modifier.border(1.dp, colors.border, RoundedCornerShape(20.dp)))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(m.text, fontFamily = SuqiFontFamily, fontSize = 14.sp, color = if (m.fromUser) Color.White else colors.primaryText)
                        }
                        Text(m.time, fontFamily = SuqiFontFamily, fontSize = 10.sp, color = colors.mutedText, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }
    }
}
