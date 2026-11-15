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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AccountHubScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val strings = com.example.ui.i18n.suqiStrings()
    val currentUser by viewModel.currentUser.collectAsState()
    val activeRole by viewModel.activeRole.collectAsState()
    val adminTier by viewModel.adminTier.collectAsState()
    val isDeveloperUnlocked by viewModel.isDeveloperUnlocked.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()

    var followedCount by remember { mutableIntStateOf(0) }
    var wishlistCount by remember { mutableIntStateOf(0) }
    var showRoleSheet by remember { mutableStateOf(false) }
    var versionTapCount by remember { mutableIntStateOf(0) }

    val isGuest = activeRole == "GUEST" || currentUser?.role == "GUEST"
    val isSeller = currentUser?.role == "SELLER" || !currentUser?.shopId.isNullOrBlank()
    val isAdmin = currentUser?.role == "ADMIN"

    LaunchedEffect(currentUser?.id, activeRole) {
        viewModel.refreshSession()
        val fRes = viewModel.repository.getShops(filter = "FOLLOWING")
        if (fRes.success && fRes.data != null) {
            followedCount = fRes.data.size
        }
        val pRes = viewModel.repository.getProducts()
        if (pRes.success && pRes.data != null) {
            wishlistCount = pRes.data.count { it.isWishlisted }
        }
    }

    SuqiScreen(
        title = strings.accountTitle,
        isOffline = isOffline,
        onRetryOffline = {
            viewModel.setOfflineMode(false)
            viewModel.refreshSession()
        },
        headerActions = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .suqiWarmShadow(CircleShape, elevation = 6.dp)
                    .clip(CircleShape)
                    .background(colors.cardBackground)
                    .clickable { viewModel.navigate(ScreenDestination.AppSettings) }
                    .testTag("account_settings_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = LucideIcons.Settings,
                    contentDescription = "App Settings",
                    tint = colors.primaryText,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = SuqiBottomNavContentPadding + 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Profile / Guest Header Card
            item {
                if (isGuest) {
                    SuqiSectionCard(
                        containerColor = colors.cardBackground,
                        modifier = Modifier.testTag("account_guest_card")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(colors.primaryTint),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = LucideIcons.CircleUser,
                                    contentDescription = "Guest",
                                    tint = SuqiPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Welcome to Suqi",
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = colors.primaryText
                                )
                                Text(
                                    text = "Sign in to sync cart, orders, addresses & chat",
                                    fontFamily = SuqiFontFamily,
                                    fontSize = 13.sp,
                                    color = colors.secondaryText
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        SuqiButton(
                            text = strings.signIn,
                            onClick = { viewModel.navigate(ScreenDestination.Auth) },
                            isPill = true,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "account_guest_signin_btn"
                        )
                    }
                } else {
                    val displayName = currentUser?.name?.ifBlank { "Suqi Shopper" } ?: "Suqi Shopper"
                    val initials = displayName
                        .split(" ")
                        .filter { it.isNotBlank() }
                        .take(2)
                        .joinToString("") { it.first().uppercaseChar().toString() }
                        .ifBlank { "S" }
                    val roleBadgeLabel = when {
                        isAdmin && activeRole == "ADMIN" -> adminTier.ifBlank { "ADMIN" }
                        activeRole == "SELLER" -> "SELLER"
                        isSeller -> "SELLER & BUYER"
                        else -> "CUSTOMER"
                    }

                    SuqiSectionCard(
                        onClick = { viewModel.navigate(ScreenDestination.ProfileEdit) },
                        modifier = Modifier.testTag("account_profile_card")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(SuqiPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!currentUser?.avatarUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = currentUser?.avatarUrl,
                                        contentDescription = displayName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = initials,
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = displayName,
                                    fontFamily = SuqiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = colors.primaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!currentUser?.phone.isNullOrBlank()) {
                                    Text(
                                        text = currentUser?.phone.orEmpty(),
                                        fontFamily = SuqiFontFamily,
                                        fontSize = 13.sp,
                                        color = colors.secondaryText
                                    )
                                } else if (!currentUser?.email.isNullOrBlank()) {
                                    Text(
                                        text = currentUser?.email.orEmpty(),
                                        fontFamily = SuqiFontFamily,
                                        fontSize = 13.sp,
                                        color = colors.secondaryText
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(colors.primaryTint)
                                            .clickable { showRoleSheet = true }
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                            .testTag("account_role_badge")
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = roleBadgeLabel,
                                                fontFamily = SuqiFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = SuqiPrimary
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = LucideIcons.ChevronDown,
                                                contentDescription = "Switch role",
                                                tint = SuqiPrimary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Edit profile",
                                        fontFamily = SuqiFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = SuqiPrimary
                                    )
                                }
                            }

                            Icon(
                                imageVector = LucideIcons.Pencil,
                                contentDescription = "Edit profile",
                                tint = colors.secondaryText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Role-Gated Seller Card
            if (!isGuest) {
                item {
                    if (isSeller || isAdmin) {
                        SuqiSectionCard(
                            containerColor = colors.primaryTint,
                            onClick = {
                                viewModel.setActiveRole("SELLER")
                                viewModel.navigate(ScreenDestination.SellerStudio)
                            },
                            modifier = Modifier
                                .border(1.dp, SuqiPrimary.copy(alpha = 0.25f), RoundedCornerShape(SuqiRadius.Card))
                                .testTag("account_seller_studio_card")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(SuqiPrimary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = LucideIcons.Store,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = strings.sellerStudioLabel,
                                            fontFamily = SuqiFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = colors.primaryText
                                        )
                                        Text(
                                            text = "Manage products, verification & orders",
                                            fontFamily = SuqiFontFamily,
                                            fontSize = 12.sp,
                                            color = colors.secondaryText
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                SuqiButton(
                                    text = "Open",
                                    onClick = {
                                        viewModel.setActiveRole("SELLER")
                                        viewModel.navigate(ScreenDestination.SellerStudio)
                                    },
                                    isPill = true,
                                    testTag = "account_open_seller_btn"
                                )
                            }
                        }
                    } else {
                        SuqiSectionCard(
                            containerColor = colors.shopTint,
                            onClick = { viewModel.navigate(ScreenDestination.BecomeSeller) },
                            modifier = Modifier
                                .border(1.dp, colors.shopBorder, RoundedCornerShape(SuqiRadius.Card))
                                .testTag("account_become_seller_card")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(SuqiPrimary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = LucideIcons.Store,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Sell on Suqi",
                                            fontFamily = SuqiFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = colors.primaryText
                                        )
                                        Text(
                                            text = "Open your verified Ethiopian shop in minutes",
                                            fontFamily = SuqiFontFamily,
                                            fontSize = 12.sp,
                                            color = colors.secondaryText
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                SuqiButton(
                                    text = "Start",
                                    onClick = { viewModel.navigate(ScreenDestination.BecomeSeller) },
                                    isPill = true,
                                    testTag = "account_become_seller_btn"
                                )
                            }
                        }
                    }
                }
            }

            // Shopping & Personal Hub Card
            item {
                SuqiSectionCard(padding = 12.dp) {
                    SuqiListRow(
                        icon = LucideIcons.Receipt,
                        title = strings.myOrders,
                        subtitle = "Track deliveries & payment status",
                        onClick = {
                            viewModel.requireSignedInOrPrompt("view your orders") {
                                viewModel.navigate(ScreenDestination.MyOrders)
                            }
                        },
                        testTag = "account_menu_orders"
                    )
                    HorizontalDivider(color = colors.border, modifier = Modifier.padding(horizontal = 8.dp))
                    SuqiListRow(
                        icon = LucideIcons.Heart,
                        title = strings.wishlistTitle,
                        badge = if (wishlistCount > 0) wishlistCount.toString() else null,
                        onClick = { viewModel.navigate(ScreenDestination.Wishlist) },
                        testTag = "account_menu_wishlist"
                    )
                    HorizontalDivider(color = colors.border, modifier = Modifier.padding(horizontal = 8.dp))
                    SuqiListRow(
                        icon = LucideIcons.HeartFilled,
                        title = strings.followingTitle,
                        badge = if (followedCount > 0) followedCount.toString() else null,
                        onClick = { viewModel.navigate(ScreenDestination.Following) },
                        testTag = "account_menu_following"
                    )
                    HorizontalDivider(color = colors.border, modifier = Modifier.padding(horizontal = 8.dp))
                    SuqiListRow(
                        icon = LucideIcons.MapPin,
                        title = strings.savedAddresses,
                        onClick = {
                            viewModel.requireSignedInOrPrompt("manage saved addresses") {
                                viewModel.navigate(ScreenDestination.SavedAddresses)
                            }
                        },
                        testTag = "account_menu_addresses"
                    )
                    HorizontalDivider(color = colors.border, modifier = Modifier.padding(horizontal = 8.dp))
                    SuqiListRow(
                        icon = LucideIcons.MessageCircle,
                        title = "Messages & Shop Chat",
                        onClick = {
                            viewModel.requireSignedInOrPrompt("view your messages") {
                                viewModel.navigate(ScreenDestination.ChatList)
                            }
                        },
                        testTag = "account_menu_chat"
                    )
                }
            }

            // Preferences & Support Card
            item {
                SuqiSectionCard(padding = 12.dp) {
                    SuqiListRow(
                        icon = LucideIcons.Bell,
                        title = strings.notificationsTitle,
                        onClick = { viewModel.navigate(ScreenDestination.Notifications) },
                        testTag = "account_menu_notifications"
                    )
                    HorizontalDivider(color = colors.border, modifier = Modifier.padding(horizontal = 8.dp))
                    SuqiListRow(
                        icon = LucideIcons.Settings,
                        title = strings.settingsTitle,
                        subtitle = "${strings.appearanceTitle} • ${viewModel.selectedLanguage.collectAsState().value}",
                        onClick = { viewModel.navigate(ScreenDestination.AppSettings) },
                        testTag = "account_menu_settings"
                    )
                    HorizontalDivider(color = colors.border, modifier = Modifier.padding(horizontal = 8.dp))
                    SuqiListRow(
                        icon = LucideIcons.HelpCircle,
                        title = strings.helpSupportTitle,
                        subtitle = "FAQs, returns & support tickets",
                        onClick = { viewModel.navigate(ScreenDestination.HelpLegal) },
                        testTag = "account_menu_help"
                    )

                    if (isAdmin) {
                        HorizontalDivider(color = colors.border, modifier = Modifier.padding(horizontal = 8.dp))
                        SuqiListRow(
                            icon = LucideIcons.ShieldCheck,
                            title = "Admin Console",
                            subtitle = "Tier: ${adminTier.ifBlank { "OWNER" }}",
                            badge = adminTier.ifBlank { "ADMIN" },
                            onClick = {
                                viewModel.setActiveRole("ADMIN")
                                viewModel.navigate(ScreenDestination.AdminConsole)
                            },
                            testTag = "account_menu_admin_console"
                        )
                    }

                    if (isDeveloperUnlocked) {
                        HorizontalDivider(color = colors.border, modifier = Modifier.padding(horizontal = 8.dp))
                        SuqiListRow(
                            icon = LucideIcons.Wrench,
                            title = "Developer Tools",
                            subtitle = "DB inspector, session & network simulator",
                            onClick = { viewModel.navigate(ScreenDestination.AdminDevTools) },
                            testTag = "account_menu_dev_tools"
                        )
                    }
                }
            }

            // Seller Subscription Promo (visible to Sellers only)
            if (isSeller) {
                item {
                    SuqiSectionCard(
                        containerColor = colors.warningBg,
                        onClick = { viewModel.navigate(ScreenDestination.SellerSubscription) },
                        modifier = Modifier.border(1.dp, SuqiWarning.copy(alpha = 0.35f), RoundedCornerShape(SuqiRadius.Card))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(LucideIcons.Sparkles, contentDescription = null, tint = SuqiWarning, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Suqi Pro Seller",
                                        fontFamily = SuqiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = colors.primaryText
                                    )
                                    Text(
                                        text = "Priority placement & detailed analytics",
                                        fontFamily = SuqiFontFamily,
                                        fontSize = 12.sp,
                                        color = colors.secondaryText
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Plans →",
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = SuqiWarning
                            )
                        }
                    }
                }
            }

            // Sign out button (for signed-in accounts) + Footer version pill (7 taps unlocks DevTools)
            if (!isGuest) {
                item {
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                viewModel.repository.loginAsGuest()
                                viewModel.refreshSession()
                                viewModel.showToast("Signed out — browsing as Guest")
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("account_sign_out_btn")
                    ) {
                        Icon(LucideIcons.LogOut, contentDescription = null, tint = SuqiError, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.signOut,
                            fontFamily = SuqiFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = SuqiError
                        )
                    }
                }
            }

            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Suqi Marketplace v1.0.0",
                        fontFamily = SuqiFontFamily,
                        fontSize = 12.sp,
                        color = colors.mutedText,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable {
                                versionTapCount++
                                if (versionTapCount >= 7 && !isDeveloperUnlocked) {
                                    viewModel.unlockDeveloperTools()
                                    viewModel.showToast("Developer Tools unlocked!")
                                } else if (versionTapCount in 4..6 && !isDeveloperUnlocked) {
                                    viewModel.showToast("${7 - versionTapCount} more taps for Developer Tools")
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("account_version_pill")
                    )
                }
            }
        }
    }

    if (showRoleSheet) {
        SuqiBottomSheet(
            onDismissRequest = { showRoleSheet = false },
            title = "Switch Workspace Role"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SuqiListRow(
                    icon = LucideIcons.ShoppingBag,
                    title = "Buyer Mode",
                    subtitle = "Shop, checkout & manage orders",
                    badge = if (activeRole == "CUSTOMER") "ACTIVE" else null,
                    onClick = {
                        viewModel.setActiveRole("CUSTOMER")
                        showRoleSheet = false
                        viewModel.showToast("Switched to Buyer mode")
                    }
                )
                if (isSeller || isAdmin) {
                    SuqiListRow(
                        icon = LucideIcons.Store,
                        title = "Seller Studio",
                        subtitle = "Manage your shop, products & orders",
                        badge = if (activeRole == "SELLER") "ACTIVE" else null,
                        onClick = {
                            viewModel.setActiveRole("SELLER")
                            showRoleSheet = false
                            viewModel.navigate(ScreenDestination.SellerStudio)
                        }
                    )
                }
                if (isAdmin) {
                    SuqiListRow(
                        icon = LucideIcons.ShieldCheck,
                        title = "Admin Console ($adminTier)",
                        subtitle = "Platform moderation, categories & banners",
                        badge = if (activeRole == "ADMIN") "ACTIVE" else null,
                        onClick = {
                            viewModel.setActiveRole("ADMIN")
                            showRoleSheet = false
                            viewModel.navigate(ScreenDestination.AdminConsole)
                        }
                    )
                } else {
                    SuqiListRow(
                        icon = LucideIcons.Shield,
                        title = "Admin / Staff Sign In",
                        subtitle = "Authorized platform staff portal",
                        onClick = {
                            showRoleSheet = false
                            viewModel.navigate(ScreenDestination.AdminSignIn)
                        }
                    )
                }
            }
        }
    }
}

/**
 * B-02 Profile Edit Screen with PickVisualMedia avatar picker and Room persistence.
 */
@Composable
fun ProfileEditScreen(
    viewModel: SuqiViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    val currentUser by viewModel.currentUser.collectAsState()

    var fullName by remember(currentUser?.id) { mutableStateOf(currentUser?.name.orEmpty()) }
    var phone by remember(currentUser?.id) { mutableStateOf(currentUser?.phone.orEmpty()) }
    var email by remember(currentUser?.id) { mutableStateOf(currentUser?.email.orEmpty()) }
    var avatarUrl by remember(currentUser?.id) { mutableStateOf(currentUser?.avatarUrl.orEmpty()) }
    var gender by remember(currentUser?.id) { mutableStateOf(currentUser?.gender.orEmpty()) }
    var birthday by remember(currentUser?.id) { mutableStateOf(currentUser?.birthday.orEmpty()) }
    var preferredLanguage by remember(currentUser?.id) {
        mutableStateOf(currentUser?.preferredLanguage?.ifBlank { "English" } ?: "English")
    }
    var isSaving by remember { mutableStateOf(false) }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            avatarUrl = uri.toString()
        }
    }

    SuqiScreen(
        title = "Edit Profile",
        onBackClick = { viewModel.goBack() },
        bottomBar = {
            Surface(
                color = colors.cardBackground,
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    SuqiButton(
                        text = if (isSaving) "Saving..." else "Save Profile",
                        enabled = !isSaving && fullName.isNotBlank(),
                        onClick = {
                            isSaving = true
                            coroutineScope.launch {
                                val res = viewModel.repository.updateUserProfile(
                                    mapOf(
                                        "name" to fullName.trim(),
                                        "phone" to phone.trim(),
                                        "email" to email.trim(),
                                        "avatarUrl" to avatarUrl,
                                        "gender" to gender,
                                        "birthday" to birthday.trim(),
                                        "preferredLanguage" to preferredLanguage
                                    )
                                )
                                isSaving = false
                                if (res.success) {
                                    viewModel.setSelectedLanguage(preferredLanguage)
                                    viewModel.refreshSession()
                                    viewModel.showToast("Profile updated!")
                                    viewModel.goBack()
                                } else {
                                    viewModel.showToast(res.message ?: "Failed to update profile")
                                }
                            }
                        },
                        isPill = true,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "btn_save_profile"
                    )
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(colors.primaryTint)
                            .border(2.dp, SuqiPrimary, CircleShape)
                            .clickable {
                                avatarPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("profile_avatar_picker"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "Profile photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            val initials = fullName.split(" ").filter { it.isNotBlank() }.take(2)
                                .joinToString("") { it.first().uppercaseChar().toString() }.ifBlank { "S" }
                            Text(
                                text = initials,
                                fontFamily = SuqiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                                color = SuqiPrimary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(SuqiPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = LucideIcons.Camera,
                                contentDescription = "Change avatar",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap photo to update avatar",
                        fontFamily = SuqiFontFamily,
                        fontSize = 12.sp,
                        color = colors.secondaryText
                    )
                }
            }

            item {
                SuqiSectionCard {
                    SuqiTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = "Full Name",
                        placeholder = "Enter your full name",
                        leadingIcon = LucideIcons.User,
                        testTag = "input_profile_name"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SuqiTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = "Phone Number",
                        placeholder = "+251 9XX XXX XXX",
                        leadingIcon = LucideIcons.Phone,
                        testTag = "input_profile_phone"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SuqiTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email Address",
                        placeholder = "you@example.et",
                        leadingIcon = LucideIcons.Info,
                        testTag = "input_profile_email"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SuqiTextField(
                        value = birthday,
                        onValueChange = { birthday = it },
                        label = "Birthday (Optional)",
                        placeholder = "YYYY-MM-DD",
                        leadingIcon = LucideIcons.Calendar,
                        testTag = "input_profile_birthday"
                    )
                }
            }

            item {
                SuqiSectionCard {
                    Text(
                        text = "Gender",
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = colors.primaryText
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Male", "Female", "Other").forEach { option ->
                            val selected = gender == option
                            FilterChip(
                                selected = selected,
                                onClick = { gender = option },
                                label = { Text(option, fontFamily = SuqiFontFamily) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Preferred Language",
                        fontFamily = SuqiFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = colors.primaryText
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("English", "Amharic", "Afaan Oromo").forEach { lang ->
                            val selected = preferredLanguage == lang
                            FilterChip(
                                selected = selected,
                                onClick = { preferredLanguage = lang },
                                label = { Text(lang, fontFamily = SuqiFontFamily, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountMenuItem(
    icon: ImageVector,
    title: String,
    badge: String? = null,
    onClick: () -> Unit
) {
    SuqiListRow(
        icon = icon,
        title = title,
        badge = badge,
        onClick = onClick
    )
}
