package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.example.data.model.AdminShopRowDto
import com.example.data.model.AdminUserRowDto
import com.example.data.model.AuditLogEntryDto
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.StatusType
import com.example.ui.components.SuqiEmptyState
import com.example.ui.components.SuqiFilterChips
import com.example.ui.components.SuqiScreen
import com.example.ui.components.SuqiSectionCard
import com.example.ui.components.SuqiStatusBadge
import com.example.ui.components.SuqiTabItem
import com.example.ui.components.SuqiTabs
import com.example.ui.components.SuqiTextField
import com.example.ui.components.formatEtb
import com.example.ui.i18n.suqiStrings
import com.example.ui.theme.LocalSuqiColors
import com.example.ui.theme.LucideIcons
import com.example.ui.theme.SuqiError
import com.example.ui.theme.SuqiFontFamily
import com.example.ui.theme.SuqiPrimary
import com.example.ui.theme.SuqiSuccess
import com.example.ui.theme.SuqiWarning
import kotlinx.coroutines.launch

// ==========================================
// A-04 (P2): Users management
// ==========================================
@Composable
fun AdminUsersScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val p6 = strings.admin6
    val isOffline by viewModel.isOffline.collectAsState()

    var users by remember { mutableStateOf(viewModel.cachedAdminUsers) }
    var query by rememberSaveable { mutableStateOf("") }
    var chip by rememberSaveable { mutableStateOf("ALL") }
    var isLoading by remember { mutableStateOf(viewModel.cachedAdminUsers.isEmpty()) }

    fun load() {
        coroutineScope.launch {
            val res = viewModel.repository.getAdminUsers()
            if (res.success && res.data != null) {
                users = res.data
                viewModel.cachedAdminUsers = res.data
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    val filtered = remember(users, query, chip) {
        users.filter { u ->
            val chipOk = when (chip) {
                "BUYERS" -> u.role.equals("CUSTOMER", true) || u.role.equals("GUEST", true)
                "SELLERS" -> u.role.equals("SELLER", true)
                "STAFF" -> u.role.equals("ADMIN", true)
                "NEW" -> u.createdAt.isNotBlank()
                else -> true
            }
            val q = query.trim().lowercase()
            val searchOk = q.isEmpty() ||
                u.name.lowercase().contains(q) ||
                u.phone.lowercase().contains(q) ||
                u.email.lowercase().contains(q)
            chipOk && searchOk
        }
    }

    SuqiScreen(
        title = strings.admin.usersManagementTitle,
        subtitle = "${filtered.size}",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SuqiTextField(
                value = query,
                onValueChange = { query = it },
                label = "",
                placeholder = p6.usersSearchHint,
                leadingIcon = LucideIcons.Search,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("admin_users_search"),
                testTag = "admin_users_search_field"
            )
            SuqiFilterChips(
                items = listOf(
                    "ALL" to strings.allLabel,
                    "BUYERS" to p6.chipBuyers,
                    "SELLERS" to p6.chipSellers,
                    "STAFF" to p6.chipStaff,
                    "NEW" to p6.chipNewThisWeek
                ),
                selectedKey = chip,
                onSelect = { chip = it },
                testTagPrefix = "users_chip"
            )
            Spacer(Modifier.height(6.dp))
            if (filtered.isEmpty()) {
                SuqiEmptyState(
                    icon = LucideIcons.Users,
                    title = strings.admin.usersManagementTitle,
                    subtitle = p6.noUsersSub,
                    testTag = "admin_users_empty"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { user ->
                        AdminUserCard(
                            user = user,
                            onOpen = { viewModel.navigate(ScreenDestination.AdminUserDetail(user.id)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminUserCard(
    user: AdminUserRowDto,
    onOpen: () -> Unit
) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val p6 = strings.admin6
    SuqiSectionCard(
        padding = 14.dp,
        onClick = onOpen,
        modifier = Modifier.testTag("admin_user_card_${user.id}")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.primaryTint)
            ) {
                Text(
                    text = user.name.trim().take(1).uppercase().ifBlank { "?" },
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = SuqiPrimary
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name.ifBlank { user.phone },
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = colors.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = user.phone,
                    fontFamily = SuqiFontFamily,
                    fontSize = 12.sp,
                    color = colors.secondaryText
                )
            }
            if (user.isSuspended) {
                SuqiStatusBadge(p6.suspendedBadge, StatusType.ERROR)
            } else {
                SuqiStatusBadge(roleLabel(user.role, p6), StatusType.PRIMARY)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            MiniStat(p6.statOrdersLabel, user.ordersCount.toString())
            MiniStat(p6.statSpentLabel, formatEtb(user.totalSpent))
            MiniStat(p6.statReviewsLabel, user.reviewsCount.toString())
            if (user.reportsAgainst > 0) {
                MiniStat(p6.statReportsLabel, user.reportsAgainst.toString(), SuqiError)
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, valueColor: Color? = null) {
    val colors = LocalSuqiColors.current
    Column {
        Text(
            text = value,
            fontFamily = SuqiFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = valueColor ?: colors.primaryText
        )
        Text(
            text = label,
            fontFamily = SuqiFontFamily,
            fontSize = 10.sp,
            color = colors.mutedText
        )
    }
}

private fun roleLabel(role: String, p6: com.example.ui.i18n.SuqiPhase6Strings): String = when {
    role.equals("ADMIN", true) -> p6.roleStaff
    role.equals("SELLER", true) -> p6.roleSeller
    else -> p6.roleBuyer
}

// ==========================================
// A-04 detail: user profile, orders, suspension
// ==========================================
@Composable
fun AdminUserDetailScreen(
    userId: String,
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val p6 = strings.admin6
    val isOffline by viewModel.isOffline.collectAsState()

    var user by remember { mutableStateOf<AdminUserRowDto?>(null) }
    var reason by rememberSaveable { mutableStateOf("") }
    var showSuspendSheet by remember { mutableStateOf(false) }

    fun load() {
        coroutineScope.launch {
            val res = viewModel.repository.getAdminUserDetail(userId)
            if (res.success && res.data != null) user = res.data
        }
    }

    LaunchedEffect(userId) { load() }

    SuqiScreen(
        title = user?.name ?: strings.admin.usersManagementTitle,
        subtitle = user?.phone,
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        val current = user
        if (current == null) {
            SuqiEmptyState(
                icon = LucideIcons.Users,
                title = strings.admin.usersManagementTitle,
                subtitle = p6.loadingLabel,
                testTag = "admin_user_detail_loading"
            )
            return@SuqiScreen
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SuqiSectionCard(padding = 16.dp, modifier = Modifier.testTag("admin_user_detail_header")) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(colors.primaryTint)
                        ) {
                            Text(
                                text = current.name.trim().take(1).uppercase().ifBlank { "?" },
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = SuqiPrimary
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = current.name.ifBlank { current.phone },
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = colors.primaryText
                            )
                            Text(
                                text = locationLine(current),
                                fontFamily = SuqiFontFamily,
                                fontSize = 12.sp,
                                color = colors.secondaryText
                            )
                        }
                        if (current.isSuspended) {
                            SuqiStatusBadge(p6.suspendedBadge, StatusType.ERROR)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        MiniStat(p6.statOrdersLabel, current.ordersCount.toString())
                        MiniStat(p6.statSpentLabel, formatEtb(current.totalSpent))
                        MiniStat(p6.statReviewsLabel, current.reviewsCount.toString())
                        MiniStat(p6.statReportsLabel, current.reportsAgainst.toString(), SuqiError)
                    }
                }
            }

            item {
                SuqiSectionCard(padding = 14.dp) {
                    AdminActionRow(
                        icon = if (current.isSuspended) LucideIcons.RotateCcw else LucideIcons.Lock,
                        title = if (current.isSuspended) p6.restoreUserBtn else p6.suspendUserBtn,
                        tint = if (current.isSuspended) SuqiSuccess else SuqiError,
                        testTag = "admin_user_suspend_btn"
                    ) {
                        if (current.isSuspended) {
                            coroutineScope.launch {
                                val res = viewModel.repository.setUserSuspended(current.id, false)
                                if (res.success) {
                                    viewModel.showToast(p6.userRestoredToast)
                                    load()
                                } else viewModel.showToast(res.message ?: strings.retryLabel)
                            }
                        } else {
                            reason = ""
                            showSuspendSheet = true
                        }
                    }
                    HorizontalDivider(color = colors.border)
                    AdminActionRow(
                        icon = LucideIcons.RefreshCw,
                        title = p6.resetSessionsBtn,
                        tint = SuqiWarning,
                        testTag = "admin_user_reset_btn"
                    ) {
                        coroutineScope.launch {
                            val res = viewModel.repository.resetUserSessions(current.id)
                            if (res.success) viewModel.showToast(p6.sessionsResetToast)
                            else viewModel.showToast(res.message ?: strings.retryLabel)
                        }
                    }
                    HorizontalDivider(color = colors.border)
                    AdminActionRow(
                        icon = LucideIcons.ShoppingBag,
                        title = p6.viewOrdersBtn,
                        tint = SuqiPrimary,
                        testTag = "admin_user_orders_btn"
                    ) {
                        viewModel.navigate(ScreenDestination.MyOrders)
                    }
                }
            }
        }

        if (showSuspendSheet) {
            SuqiBottomSheet(onDismissRequest = { showSuspendSheet = false }) {
                Text(
                    text = p6.suspendUserBtn,
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = colors.primaryText
                )
                Spacer(Modifier.height(12.dp))
                SuqiTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = p6.suspendReasonHint,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "admin_user_suspend_reason"
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val res = viewModel.repository.setUserSuspended(current.id, true, reason.trim())
                            if (res.success) {
                                showSuspendSheet = false
                                viewModel.showToast(p6.userSuspendedToast)
                                load()
                            } else viewModel.showToast(res.message ?: strings.retryLabel)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuqiError),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("admin_user_suspend_confirm")
                ) {
                    Text(p6.suspendUserBtn, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AdminActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    tint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    val colors = LocalSuqiColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp)
            .testTag(testTag)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.12f))
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            fontFamily = SuqiFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = colors.primaryText,
            modifier = Modifier.weight(1f)
        )
        Icon(LucideIcons.ChevronRight, contentDescription = null, tint = colors.mutedText)
    }
}

private fun locationLine(user: AdminUserRowDto): String {
    val parts = listOf(user.subCity, user.city, user.region).filter { it.isNotBlank() }
    return parts.joinToString(", ")
}

// ==========================================
// A-05 (P2): Shops management
// ==========================================
@Composable
fun AdminShopsManagerScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val p6 = strings.admin6
    val isOffline by viewModel.isOffline.collectAsState()

    var shops by remember { mutableStateOf(viewModel.cachedAdminShopRows) }
    var query by rememberSaveable { mutableStateOf("") }
    var chip by rememberSaveable { mutableStateOf("ALL") }
    var isLoading by remember { mutableStateOf(viewModel.cachedAdminShopRows.isEmpty()) }

    fun load() {
        coroutineScope.launch {
            val res = viewModel.repository.getAdminShops()
            if (res.success && res.data != null) {
                shops = res.data
                viewModel.cachedAdminShopRows = res.data
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    val filtered = remember(shops, query, chip) {
        shops.filter { s ->
            val chipOk = when (chip) {
                "VERIFIED" -> s.isVerified
                "PENDING" -> s.verificationStatus == "PENDING_REVIEW" || s.verificationStatus == "RESUBMITTED"
                "PRO" -> s.planName != "Free Pilot"
                else -> true
            }
            val q = query.trim().lowercase()
            val searchOk = q.isEmpty() || s.name.lowercase().contains(q) || s.ownerName.lowercase().contains(q)
            chipOk && searchOk
        }
    }

    SuqiScreen(
        title = strings.admin.shopsManagementTitle,
        subtitle = "${filtered.size}",
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SuqiTextField(
                value = query,
                onValueChange = { query = it },
                label = "",
                placeholder = p6.shopsSearchHint,
                leadingIcon = LucideIcons.Search,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                testTag = "admin_shops_search_field"
            )
            SuqiFilterChips(
                items = listOf(
                    "ALL" to strings.allLabel,
                    "VERIFIED" to p6.chipVerified,
                    "PENDING" to p6.chipPending,
                    "PRO" to p6.chipPro
                ),
                selectedKey = chip,
                onSelect = { chip = it },
                testTagPrefix = "shops_chip"
            )
            Spacer(Modifier.height(6.dp))
            if (filtered.isEmpty()) {
                SuqiEmptyState(
                    icon = LucideIcons.Store,
                    title = strings.admin.shopsManagementTitle,
                    subtitle = p6.noShopsSub,
                    testTag = "admin_shops_empty"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { shop ->
                        AdminShopCard(
                            shop = shop,
                            onOpen = { viewModel.navigate(ScreenDestination.AdminShopDetail(shop.id)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminShopCard(
    shop: AdminShopRowDto,
    onOpen: () -> Unit
) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val p6 = strings.admin6
    SuqiSectionCard(
        padding = 14.dp,
        onClick = onOpen,
        modifier = Modifier.testTag("admin_shop_card_${shop.id}")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.secondaryBackground),
                contentAlignment = Alignment.Center
            ) {
                if (shop.logoUrl.isNotBlank()) {
                    AsyncImage(
                        model = shop.logoUrl,
                        contentDescription = shop.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Icon(LucideIcons.Store, contentDescription = null, tint = SuqiPrimary)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = shop.name,
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = colors.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = shop.ownerName.ifBlank { shop.category },
                    fontFamily = SuqiFontFamily,
                    fontSize = 12.sp,
                    color = colors.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                if (shop.isSuspended) {
                    SuqiStatusBadge(p6.suspendShopBtn, StatusType.ERROR)
                } else if (shop.isFeatured) {
                    SuqiStatusBadge(p6.featureShopBtn, StatusType.PRIMARY)
                } else {
                    SuqiStatusBadge(shop.planName, StatusType.NEUTRAL)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            MiniStat(p6.productsCountLabel, shop.productsCount.toString())
            MiniStat(p6.ordersCountLabel, shop.ordersCount.toString())
            MiniStat(strings.admin.financeTitle, formatEtb(shop.gmv))
            if (shop.isVerified) MiniStat(p6.chipVerified, "✓", SuqiSuccess)
        }
    }
}

// ==========================================
// A-05 detail: shop tabs, admin actions, notes
// ==========================================
@Composable
fun AdminShopDetailScreen(
    shopId: String,
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val p6 = strings.admin6
    val isOffline by viewModel.isOffline.collectAsState()

    var shop by remember { mutableStateOf<AdminShopRowDto?>(null) }
    var tab by rememberSaveable { mutableStateOf("OVERVIEW") }
    var notes by rememberSaveable { mutableStateOf("") }
    var notesInitialised by remember { mutableStateOf(false) }

    fun load() {
        coroutineScope.launch {
            val res = viewModel.repository.getAdminShopDetail(shopId)
            if (res.success && res.data != null) {
                shop = res.data
                if (!notesInitialised) {
                    notes = res.data.notes
                    notesInitialised = true
                }
            }
        }
    }

    LaunchedEffect(shopId) { load() }

    SuqiScreen(
        title = shop?.name ?: strings.admin.shopsManagementTitle,
        subtitle = shop?.ownerName,
        onBackClick = { viewModel.goBack() },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        val current = shop
        if (current == null) {
            SuqiEmptyState(
                icon = LucideIcons.Store,
                title = strings.admin.shopsManagementTitle,
                subtitle = p6.loadingLabel,
                testTag = "admin_shop_detail_loading"
            )
            return@SuqiScreen
        }

        fun action(name: String, toast: String) {
            coroutineScope.launch {
                val res = viewModel.repository.adminShopAction(current.id, name)
                if (res.success) {
                    viewModel.showToast(toast)
                    res.data?.let { shop = it }
                } else viewModel.showToast(res.message ?: strings.retryLabel)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SuqiTabs(
                tabs = listOf(
                    SuqiTabItem("OVERVIEW", strings.admin.tabOverview),
                    SuqiTabItem("PRODUCTS", p6.tabProducts),
                    SuqiTabItem("ORDERS", p6.tabOrders),
                    SuqiTabItem("NOTES", p6.tabNotes)
                ),
                selectedKey = tab,
                onSelectTab = { tab = it },
                scrollable = true,
                testTagPrefix = "admin_shop_tab"
            )
            Spacer(Modifier.height(6.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (tab) {
                    "OVERVIEW" -> {
                        item {
                            SuqiSectionCard(padding = 16.dp, modifier = Modifier.testTag("admin_shop_overview_card")) {
                                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                                    MiniStat(p6.productsCountLabel, current.productsCount.toString())
                                    MiniStat(p6.ordersCountLabel, current.ordersCount.toString())
                                    MiniStat(strings.admin.financeTitle, formatEtb(current.gmv))
                                }
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SuqiStatusBadge(current.planName, StatusType.NEUTRAL)
                                    SuqiStatusBadge(
                                        if (current.isVerified) p6.chipVerified else p6.chipPending,
                                        if (current.isVerified) StatusType.SUCCESS else StatusType.WARNING
                                    )
                                    if (current.isSuspended) SuqiStatusBadge(p6.suspendShopBtn, StatusType.ERROR)
                                }
                            }
                        }
                        item {
                            SuqiSectionCard(padding = 14.dp) {
                                AdminActionRow(
                                    icon = if (current.isFeatured) LucideIcons.Star else LucideIcons.Sparkles,
                                    title = if (current.isFeatured) p6.unfeatureShopBtn else p6.featureShopBtn,
                                    tint = SuqiPrimary,
                                    testTag = "admin_shop_feature_btn"
                                ) {
                                    if (current.isFeatured) action("UNFEATURE", p6.shopRestoredToast)
                                    else action("FEATURE", p6.shopFeaturedToast)
                                }
                                HorizontalDivider(color = colors.border)
                                AdminActionRow(
                                    icon = if (current.isSuspended) LucideIcons.RotateCcw else LucideIcons.Lock,
                                    title = if (current.isSuspended) p6.restoreShopBtn else p6.suspendShopBtn,
                                    tint = if (current.isSuspended) SuqiSuccess else SuqiError,
                                    testTag = "admin_shop_suspend_btn"
                                ) {
                                    if (current.isSuspended) action("RESTORE", p6.shopRestoredToast)
                                    else action("SUSPEND", p6.shopSuspendedToast)
                                }
                                HorizontalDivider(color = colors.border)
                                AdminActionRow(
                                    icon = LucideIcons.MessageSquarePlus,
                                    title = p6.messageOwnerBtn,
                                    tint = SuqiWarning,
                                    testTag = "admin_shop_message_btn"
                                ) {
                                    viewModel.showToast(p6.messageOwnerBtn)
                                }
                            }
                        }
                    }

                    "PRODUCTS" -> {
                        item {
                            ShopProductsList(shopId = current.id, viewModel = viewModel)
                        }
                    }

                    "ORDERS" -> {
                        item {
                            ShopOrdersList(shopId = current.id, viewModel = viewModel)
                        }
                    }

                    "NOTES" -> {
                        item {
                            SuqiSectionCard(padding = 16.dp, modifier = Modifier.testTag("admin_shop_notes_card")) {
                                SuqiTextField(
                                    value = notes,
                                    onValueChange = { notes = it },
                                    label = p6.adminNotesHint,
                                    singleLine = false,
                                    modifier = Modifier.fillMaxWidth(),
                                    testTag = "admin_shop_notes_input"
                                )
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val res = viewModel.repository.saveAdminShopNotes(current.id, notes)
                                            if (res.success) {
                                                viewModel.showToast(p6.notesSavedToast)
                                                res.data?.let { shop = it }
                                            } else viewModel.showToast(res.message ?: strings.retryLabel)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("admin_shop_notes_save")
                                ) {
                                    Text(strings.saveLabel, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShopProductsList(shopId: String, viewModel: SuqiViewModel) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    var products by remember { mutableStateOf(emptyList<com.example.data.model.ProductDto>()) }
    LaunchedEffect(shopId) {
        val res = viewModel.repository.getProducts()
        if (res.success) products = res.data.orEmpty().filter { it.shopId == shopId }
    }
    if (products.isEmpty()) {
        SuqiEmptyState(
            icon = LucideIcons.Package,
            title = strings.admin6.tabProducts,
            subtitle = strings.noProductsFound,
            testTag = "admin_shop_products_empty"
        )
    } else {
        SuqiSectionCard(padding = 14.dp, modifier = Modifier.testTag("admin_shop_products_card")) {
            products.take(12).forEachIndexed { index, product ->
                if (index > 0) HorizontalDivider(color = colors.border)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = product.name,
                        fontFamily = SuqiFontFamily,
                        fontSize = 13.sp,
                        color = colors.primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatEtb(product.price),
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
private fun ShopOrdersList(shopId: String, viewModel: SuqiViewModel) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    var orders by remember { mutableStateOf(emptyList<com.example.data.model.OrderDto>()) }
    LaunchedEffect(shopId) {
        val res = viewModel.repository.getOrders()
        if (res.success) orders = res.data.orEmpty().filter { it.shopId == shopId }
    }
    if (orders.isEmpty()) {
        SuqiEmptyState(
            icon = LucideIcons.ShoppingBag,
            title = strings.admin6.tabOrders,
            subtitle = strings.admin6.noDisputesSub,
            testTag = "admin_shop_orders_empty"
        )
    } else {
        SuqiSectionCard(padding = 14.dp, modifier = Modifier.testTag("admin_shop_orders_card")) {
            orders.take(12).forEachIndexed { index, order ->
                if (index > 0) HorizontalDivider(color = colors.border)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "#${order.orderReference}",
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = colors.primaryText
                        )
                        Text(
                            text = order.customerName,
                            fontFamily = SuqiFontFamily,
                            fontSize = 11.sp,
                            color = colors.mutedText
                        )
                    }
                    Text(
                        text = formatEtb(order.totalAmount),
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

// ==========================================
// A-20 (P2): Audit log
// ==========================================
@Composable
fun AdminAuditLogScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val p6 = strings.admin6
    val isOffline by viewModel.isOffline.collectAsState()

    var entries by remember { mutableStateOf(viewModel.cachedAuditLog) }
    var query by rememberSaveable { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(viewModel.cachedAuditLog.isEmpty()) }

    LaunchedEffect(Unit) {
        val res = viewModel.repository.getAuditLog()
        if (res.success && res.data != null) {
            entries = res.data
            viewModel.cachedAuditLog = res.data
        }
        isLoading = false
    }

    val filtered = remember(entries, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) entries else entries.filter {
            it.actorName.lowercase().contains(q) ||
                it.action.lowercase().contains(q) ||
                it.targetLabel.lowercase().contains(q)
        }
    }

    SuqiScreen(
        title = strings.admin.auditLogTitle,
        subtitle = "${filtered.size}",
        onBackClick = { viewModel.goBack() },
        headerActions = {
            Icon(
                imageVector = LucideIcons.History,
                contentDescription = null,
                tint = colors.primaryText,
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 4.dp)
            )
        },
        isOffline = isOffline,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SuqiTextField(
                value = query,
                onValueChange = { query = it },
                label = "",
                placeholder = p6.auditSearchHint,
                leadingIcon = LucideIcons.Search,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                testTag = "admin_audit_search_field"
            )
            Spacer(Modifier.height(6.dp))
            if (filtered.isEmpty()) {
                SuqiEmptyState(
                    icon = LucideIcons.History,
                    title = strings.admin.auditLogTitle,
                    subtitle = p6.noAuditSub,
                    testTag = "admin_audit_empty"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { entry ->
                        AuditLogCard(entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditLogCard(entry: AuditLogEntryDto) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val p6 = strings.admin6
    SuqiSectionCard(padding = 14.dp, modifier = Modifier.testTag("audit_entry_${entry.id}")) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(colors.primaryTint)
            ) {
                Text(
                    text = entry.actorName.trim().take(1).uppercase().ifBlank { "?" },
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = SuqiPrimary
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${entry.actorName} • ${entry.actorRole}",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = colors.primaryText
                )
                Text(
                    text = entry.createdAt,
                    fontFamily = SuqiFontFamily,
                    fontSize = 11.sp,
                    color = colors.mutedText
                )
            }
            SuqiStatusBadge(entry.action, StatusType.NEUTRAL)
        }
        if (entry.targetLabel.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${entry.targetType}: ${entry.targetLabel}",
                fontFamily = SuqiFontFamily,
                fontSize = 12.sp,
                color = colors.secondaryText
            )
        }
        if (entry.beforeValue.isNotBlank() || entry.afterValue.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (entry.beforeValue.isNotBlank()) {
                    Text(
                        text = "${p6.beforeLabel}: ${entry.beforeValue}",
                        fontFamily = SuqiFontFamily,
                        fontSize = 11.sp,
                        color = colors.mutedText
                    )
                }
                if (entry.afterValue.isNotBlank()) {
                    Text(
                        text = "${p6.afterLabel}: ${entry.afterValue}",
                        fontFamily = SuqiFontFamily,
                        fontSize = 11.sp,
                        color = SuqiPrimary
                    )
                }
            }
        }
        if (entry.note.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = entry.note,
                fontFamily = SuqiFontFamily,
                fontSize = 12.sp,
                color = colors.secondaryText
            )
        }
    }
}
