package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NotificationDto
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.*
import com.example.ui.i18n.suqiStrings
import com.example.ui.theme.*
import com.example.util.EthiopianMarketplaceUtils
import kotlinx.coroutines.launch
import java.util.Calendar

/** Day buckets used to group the notification list (B-09). */
enum class NotificationDayGroup { TODAY, YESTERDAY, EARLIER }

/** Pure helper so grouping can be unit tested without Compose. */
fun notificationDayGroup(timestamp: String, nowMillis: Long = System.currentTimeMillis()): NotificationDayGroup {
    val millis = EthiopianMarketplaceUtils.resolveTimestampMillis(timestamp, nowMillis)
        ?: return NotificationDayGroup.EARLIER
    val cal = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val startToday = cal.timeInMillis
    return when {
        millis >= startToday -> NotificationDayGroup.TODAY
        millis >= startToday - 86_400_000L -> NotificationDayGroup.YESTERDAY
        else -> NotificationDayGroup.EARLIER
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotificationsScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val isOffline by viewModel.isOffline.collectAsState()
    var notifications by remember { mutableStateOf(viewModel.cachedNotifications) }
    var selectedFilter by rememberSaveable { mutableStateOf("ALL") }
    var isLoading by remember { mutableStateOf(notifications.isEmpty()) }
    val listState = rememberLazyListState()

    fun loadNotifications() {
        coroutineScope.launch {
            isLoading = notifications.isEmpty()
            val res = viewModel.repository.getNotifications()
            if (res.success && res.data != null) {
                notifications = res.data
                viewModel.cachedNotifications = res.data
            }
            viewModel.refreshUnreadNotifications()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadNotifications() }

    val filtered = remember(notifications, selectedFilter) {
        if (selectedFilter == "ALL") notifications
        else notifications.filter { it.type.equals(selectedFilter, ignoreCase = true) }
    }
    val grouped = remember(filtered) {
        val order = NotificationDayGroup.values().toList()
        filtered.groupBy { notificationDayGroup(it.timestamp) }
            .toList()
            .sortedBy { order.indexOf(it.first) }
    }

    val tabItems = remember(notifications, strings) {
        fun unread(type: String) = notifications.any { it.type.equals(type, true) && !it.isRead }
        buildList {
            add(SuqiTabItem("ALL", strings.checkout.filterAll, notifications.size, notifications.any { !it.isRead }))
            add(SuqiTabItem("ORDER", strings.buyer.ordersTabLabel, notifications.count { it.type == "ORDER" }, unread("ORDER")))
            add(SuqiTabItem("PAYMENT", strings.checkout.paymentMethodTitle, notifications.count { it.type == "PAYMENT" }, unread("PAYMENT")))
            add(SuqiTabItem("CHAT", strings.buyer.chatTabLabel, notifications.count { it.type == "CHAT" }, unread("CHAT")))
            add(SuqiTabItem("PROMO", strings.buyer.promosTabLabel, notifications.count { it.type == "PROMO" }, unread("PROMO")))
            if (notifications.any { it.type.equals("SYSTEM", true) }) {
                add(SuqiTabItem("SYSTEM", strings.buyerWork.notifTabSystem, notifications.count { it.type == "SYSTEM" }, unread("SYSTEM")))
            }
        }
    }

    SuqiScreen(
        title = strings.notificationsTitle,
        subtitle = "${notifications.count { !it.isRead }}",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        headerActions = {
            if (notifications.any { !it.isRead }) {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.clearNotificationCount()
                            loadNotifications()
                            viewModel.showToast(strings.buyer.markAllRead)
                        }
                    },
                    modifier = Modifier.testTag("notifications_mark_all_read")
                ) {
                    Text(strings.buyer.markAllRead, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, color = SuqiPrimary)
                }
            }
            IconButton(
                onClick = { viewModel.navigate(ScreenDestination.NotificationPrefs) },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colors.secondaryBackground)
                    .testTag("notifications_prefs_btn")
            ) {
                Icon(LucideIcons.Settings, contentDescription = strings.buyerWork.notifSettingsOpenDesc, tint = colors.primaryText, modifier = Modifier.size(18.dp))
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            SuqiTabs(
                tabs = tabItems,
                selectedIndex = tabItems.indexOfFirst { it.key == selectedFilter }.coerceAtLeast(0),
                onSelectTab = { selectedFilter = tabItems[it].key },
                scrollable = true,
                testTagPrefix = "notif_filter"
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (isLoading) {
                SuqiSkeletonList(count = 4, modifier = Modifier.padding(top = 8.dp))
            } else if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    SuqiEmptyState(
                        icon = LucideIcons.Bell,
                        title = strings.buyer.noNotificationsTitle,
                        subtitle = strings.buyer.noNotificationsSub,
                        ctaText = if (selectedFilter != "ALL") strings.checkout.filterAll else null,
                        onCtaClick = if (selectedFilter != "ALL") { { selectedFilter = "ALL" } } else null,
                        testTag = "notifications_empty"
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    grouped.forEach { (group, groupItems) ->
                        stickyHeader(key = "hdr_${group.name}") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.surfaceBackground)
                                    .padding(vertical = 6.dp)
                                    .testTag("notif_group_${group.name.lowercase()}")
                            ) {
                                Text(
                                    text = when (group) {
                                        NotificationDayGroup.TODAY -> strings.buyerWork.notifGroupToday
                                        NotificationDayGroup.YESTERDAY -> strings.buyerWork.notifGroupYesterday
                                        NotificationDayGroup.EARLIER -> strings.buyerWork.notifGroupEarlier
                                    },
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = colors.mutedText
                                )
                            }
                        }
                        items(groupItems, key = { it.id }) { notif ->
                            NotificationRow(
                                notif = notif,
                                onMarkRead = {
                                    coroutineScope.launch {
                                        viewModel.repository.markNotificationRead(notif.id, true)
                                        loadNotifications()
                                    }
                                },
                                onDelete = {
                                    coroutineScope.launch {
                                        viewModel.repository.deleteNotification(notif.id)
                                        loadNotifications()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(
    notif: NotificationDto,
    onMarkRead: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    SuqiSwipeRow(
        onSwipeLeftDelete = onDelete,
        onSwipeRightAction = if (!notif.isRead) onMarkRead else null,
        modifier = Modifier.testTag("notification_swipe_${notif.id}")
    ) {
        SuqiSectionCard(
            padding = 14.dp,
            containerColor = if (notif.isRead) colors.cardBackground else colors.primaryTint,
            onClick = { if (!notif.isRead) onMarkRead() },
            modifier = Modifier.testTag("notification_card_${notif.id}")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(SuqiPrimary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (notif.type) {
                            "PAYMENT" -> LucideIcons.Wallet
                            "CHAT" -> LucideIcons.MessageCircle
                            "PROMO" -> LucideIcons.BadgePercent
                            "SYSTEM" -> LucideIcons.Info
                            else -> LucideIcons.Truck
                        },
                        contentDescription = null,
                        tint = SuqiPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = notif.title,
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = colors.primaryText,
                            modifier = Modifier.weight(1f)
                        )
                        if (!notif.isRead) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SuqiPrimary))
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = notif.body, fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.secondaryText)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = notif.timestamp, fontFamily = SuqiFontFamily, fontSize = 11.sp, color = colors.mutedText)
                }
                // Accessible fallback for the swipe-to-delete gesture
                IconButton(onClick = onDelete, modifier = Modifier.size(48.dp).testTag("notification_delete_${notif.id}")) {
                    Icon(LucideIcons.Trash2, contentDescription = strings.deleteLabel, tint = colors.mutedText, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

/** B-09 Notification preferences: switches plus quiet hours, persisted locally. */
@Composable
fun NotificationPrefsScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val w = strings.buyerWork
    val isOffline by viewModel.isOffline.collectAsState()
    val store = viewModel.buyerPrefs
    var prefs by remember { mutableStateOf(store.loadNotificationPrefs()) }
    fun update(next: com.example.data.local.BuyerNotificationPrefs) {
        prefs = next
        store.saveNotificationPrefs(next)
    }

    SuqiScreen(
        title = w.notifPrefsTitle,
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(w.notifPrefsSub, fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.secondaryText)
            }
            item {
                SuqiSectionCard(padding = 8.dp) {
                    SuqiSwitchRow(w.notifPrefOrders, prefs.orderUpdates, { update(prefs.copy(orderUpdates = it)) },
                        subtitle = w.notifPrefOrdersSub, icon = LucideIcons.Truck, testTag = "pref_orders")
                    SuqiSwitchRow(w.notifPrefChat, prefs.chatMessages, { update(prefs.copy(chatMessages = it)) },
                        subtitle = w.notifPrefChatSub, icon = LucideIcons.MessageCircle, testTag = "pref_chat")
                    SuqiSwitchRow(w.notifPrefPromos, prefs.promotions, { update(prefs.copy(promotions = it)) },
                        subtitle = w.notifPrefPromosSub, icon = LucideIcons.BadgePercent, testTag = "pref_promos")
                    SuqiSwitchRow(w.notifPrefStock, prefs.stockAndPrice, { update(prefs.copy(stockAndPrice = it)) },
                        subtitle = w.notifPrefStockSub, icon = LucideIcons.Heart, testTag = "pref_stock")
                    SuqiSwitchRow(w.notifPrefShops, prefs.followedShops, { update(prefs.copy(followedShops = it)) },
                        subtitle = w.notifPrefShopsSub, icon = LucideIcons.Store, testTag = "pref_shops")
                }
            }
            item {
                SuqiSectionCard(padding = 8.dp) {
                    SuqiSwitchRow(w.quietHoursTitle, prefs.quietHoursEnabled, { update(prefs.copy(quietHoursEnabled = it)) },
                        subtitle = w.quietHoursSub, icon = LucideIcons.Clock, testTag = "pref_quiet_hours")
                    if (prefs.quietHoursEnabled) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            SuqiTimeField(
                                label = w.quietFromLabel,
                                timeText = prefs.quietStart,
                                onTimeSelected = { update(prefs.copy(quietStart = it)) },
                                modifier = Modifier.weight(1f)
                            )
                            SuqiTimeField(
                                label = w.quietToLabel,
                                timeText = prefs.quietEnd,
                                onTimeSelected = { update(prefs.copy(quietEnd = it)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
