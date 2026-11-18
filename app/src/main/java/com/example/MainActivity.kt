package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import com.example.ui.components.CartUiState
import com.example.ui.components.LocalCartUiState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.SuqiFloatingBottomNav
import com.example.ui.components.SuqiGuestGateSheet
import com.example.ui.components.SuqiOfflineBanner
import com.example.ui.components.SuqiRestrictedState
import com.example.ui.components.SuqiSellerFloatingBottomNav
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SuqiBackground
import com.example.ui.theme.SuqiPrimary
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: SuqiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkPref by viewModel.isDarkMode.collectAsState()
            val selectedLang by viewModel.selectedLanguage.collectAsState()
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val darkTheme = isDarkPref ?: systemDark
            val currentLang = com.example.ui.i18n.SuqiLanguage.fromSetting(selectedLang)

            MyApplicationTheme(darkTheme = darkTheme, language = currentLang) {
                SuqiAppRoot(viewModel = viewModel, onFinish = { finish() })
            }
        }
    }
}

@Composable
fun SuqiAppRoot(
    viewModel: SuqiViewModel,
    onFinish: () -> Unit
) {
    val currentNavEntry by viewModel.currentNavEntry.collectAsState()
    val currentScreen = currentNavEntry.destination
    val disposedNavIds by viewModel.disposedNavIds.collectAsState()
    val saveableStateHolder = rememberSaveableStateHolder()
    val cartLines by viewModel.cartItems.collectAsState(initial = emptyList())
    val cartUiState = remember(cartLines) {
        CartUiState(
            quantities = cartLines.groupBy { it.productId }.mapValues { (_, lines) -> lines.sumOf { it.quantity } },
            onChange = { product, delta -> viewModel.changeCartQuantity(product, delta) }
        )
    }
    val toastMessage by viewModel.toastMessage.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val activeRole by viewModel.activeRole.collectAsState()
    val adminTier by viewModel.adminTier.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    val guestGateAction by viewModel.guestGateAction.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val strings = com.example.ui.i18n.suqiStrings()

    LaunchedEffect(disposedNavIds) {
        if (disposedNavIds.isNotEmpty()) {
            disposedNavIds.forEach { saveableStateHolder.removeState(it) }
            viewModel.acknowledgeDisposedNavIds(disposedNavIds)
        }
    }

    var sellerPendingCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(currentScreen, currentUser?.id) {
        if (currentScreen in listOf(
                ScreenDestination.SellerStudio,
                ScreenDestination.SellerOrders,
                ScreenDestination.ProductManagement,
                ScreenDestination.SellerInbox,
                ScreenDestination.SellerMore
            )
        ) {
            val qRes = viewModel.repository.getSellerVerificationQueue()
            if (qRes.success && qRes.data != null) {
                sellerPendingCount = qRes.data.size
            }
        }
    }

    // Intercept hardware and gesture back button
    BackHandler(enabled = true) {
        val didPop = viewModel.goBack()
        if (!didPop) {
            onFinish()
        }
    }

    LaunchedEffect(toastMessage) {
        val msg = toastMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    val isBuyerRootNav = currentScreen in listOf(
        ScreenDestination.Home,
        ScreenDestination.Categories,
        ScreenDestination.Search,
        ScreenDestination.Shops,
        ScreenDestination.Account
    )

    val isSellerRootNav = currentScreen in listOf(
        ScreenDestination.SellerStudio,
        ScreenDestination.SellerOrders,
        ScreenDestination.ProductManagement,
        ScreenDestination.SellerInbox,
        ScreenDestination.SellerMore
    )

    val currentBuyerRouteKey = when (currentScreen) {
        ScreenDestination.Home -> "home"
        ScreenDestination.Categories -> "categories"
        ScreenDestination.Search -> "search"
        ScreenDestination.Shops -> "shops"
        ScreenDestination.Account -> "account"
        else -> ""
    }

    val currentSellerRouteKey = when (currentScreen) {
        ScreenDestination.SellerStudio -> "seller_dashboard"
        ScreenDestination.SellerOrders -> "seller_orders"
        ScreenDestination.ProductManagement -> "seller_products"
        ScreenDestination.SellerInbox -> "seller_inbox"
        ScreenDestination.SellerMore -> "seller_more"
        else -> ""
    }

    val hasSellerAccess = (currentUser?.role == "SELLER" || !currentUser?.shopId.isNullOrBlank()) ||
        (activeRole == "ADMIN")
    val hasAdminAccess = currentUser?.role == "ADMIN" || activeRole == "ADMIN"

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .padding(bottom = if (isBuyerRootNav || isSellerRootNav) 80.dp else 16.dp)
                    .navigationBarsPadding()
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = SuqiPrimary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(14.dp)
                )
            }
        },
        containerColor = SuqiBackground,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(visible = isOffline) {
                    SuqiOfflineBanner(
                        onRetry = {
                            coroutineScope.launch {
                                viewModel.setOfflineMode(false)
                                viewModel.refreshSession()
                                viewModel.showToast(strings.common.backOnlineSynced)
                            }
                        }
                    )
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    saveableStateHolder.SaveableStateProvider(key = currentNavEntry.id) {
                        CompositionLocalProvider(LocalCartUiState provides cartUiState) {
                        when (val dest = currentNavEntry.destination) {
                            is ScreenDestination.Home -> HomeScreen(viewModel = viewModel)
                        is ScreenDestination.Categories -> CategoriesScreen(viewModel = viewModel)
                        is ScreenDestination.CategoryListing -> CategoryListingScreen(
                            categoryId = dest.categoryId,
                            categoryTitle = dest.categoryName,
                            viewModel = viewModel
                        )
                        is ScreenDestination.Search -> SearchScreen(viewModel = viewModel)
                        is ScreenDestination.Shops -> ShopsScreen(viewModel = viewModel)
                        is ScreenDestination.ShopStorefront -> ShopStorefrontScreen(
                            shopId = dest.shopId,
                            viewModel = viewModel
                        )
                        is ScreenDestination.ProductDetails -> ProductDetailsScreen(
                            productId = dest.productId,
                            viewModel = viewModel
                        )
                        is ScreenDestination.Cart -> CartScreen(viewModel = viewModel)
                        is ScreenDestination.CheckoutDelivery -> CheckoutDeliveryScreen(
                            shopId = dest.shopId,
                            viewModel = viewModel
                        )
                        is ScreenDestination.CheckoutPayment -> CheckoutPaymentScreen(
                            shopId = dest.shopId,
                            viewModel = viewModel
                        )
                        is ScreenDestination.OrderSubmitted -> OrderSubmittedScreen(
                            orderReference = dest.orderReference,
                            orderId = dest.orderId,
                            viewModel = viewModel
                        )
                        is ScreenDestination.MyOrders -> MyOrdersScreen(viewModel = viewModel)
                        is ScreenDestination.OrderDetails -> OrderDetailScreen(
                            orderId = dest.orderId,
                            viewModel = viewModel
                        )
                        is ScreenDestination.Account -> AccountHubScreen(viewModel = viewModel)
                        is ScreenDestination.ProfileEdit -> ProfileEditScreen(viewModel = viewModel)
                        is ScreenDestination.Auth -> AuthScreen(viewModel = viewModel)
                        is ScreenDestination.Onboarding -> OnboardingScreen(viewModel = viewModel)
                        is ScreenDestination.BecomeSeller -> BecomeSellerScreen(viewModel = viewModel)

                        // Seller Studio (Role-Gated)
                        is ScreenDestination.SellerStudio -> {
                            if (!hasSellerAccess) {
                                SuqiRestrictedState(
                                    title = strings.seller.sellerRestrictedTitle,
                                    subtitle = strings.seller.sellerRestrictedSub,
                                    primaryButtonText = if (activeRole == "GUEST") strings.signIn else strings.becomeSellerLabel,
                                    onPrimaryClick = {
                                        viewModel.navigate(if (activeRole == "GUEST") ScreenDestination.Auth else ScreenDestination.BecomeSeller)
                                    },
                                    onBackClick = { viewModel.goBack() },
                                    testTag = "seller_restricted"
                                )
                            } else {
                                SellerStudioScreen(viewModel = viewModel)
                            }
                        }
                        is ScreenDestination.AddEditProduct -> AddEditProductScreen(
                            productId = dest.productId,
                            viewModel = viewModel
                        )
                        is ScreenDestination.ProductManagement -> ProductManagementScreen(viewModel = viewModel)
                        is ScreenDestination.SellerInventory -> SellerInventoryScreen(viewModel = viewModel)
                        is ScreenDestination.PaymentMethodsSetup -> PaymentMethodsSetupScreen(viewModel = viewModel)
                        is ScreenDestination.DeliverySetup -> DeliverySetupScreen(viewModel = viewModel)
                        is ScreenDestination.VerificationQueue -> VerificationQueueScreen(viewModel = viewModel)
                        is ScreenDestination.SellerOrders -> SellerOrdersScreen(viewModel = viewModel)
                        is ScreenDestination.SellerOrderDetail -> SellerOrderDetailScreen(
                            orderId = dest.orderId,
                            viewModel = viewModel
                        )
                        is ScreenDestination.SellerInbox -> SellerInboxScreen(viewModel = viewModel)
                        is ScreenDestination.SellerMore -> SellerMoreScreen(viewModel = viewModel)
                        is ScreenDestination.SellerVerification -> SellerVerificationScreen(viewModel = viewModel)
                        is ScreenDestination.Promotions -> PromotionsScreen(viewModel = viewModel)
                        is ScreenDestination.ShopSettings -> ShopSettingsScreen(viewModel = viewModel)

                        is ScreenDestination.Following -> FollowingScreen(viewModel = viewModel)
                        is ScreenDestination.ChatList -> ChatListScreen(viewModel = viewModel)
                        is ScreenDestination.ChatThread -> ChatThreadScreen(
                            shopId = dest.shopId,
                            productContextName = dest.productContextName,
                            productContextPrice = dest.productContextPrice,
                            productContextImage = dest.productContextImage,
                            conversationId = dest.conversationId,
                            viewModel = viewModel
                        )
                        is ScreenDestination.Notifications -> NotificationsScreen(viewModel = viewModel)
                        is ScreenDestination.NotificationPrefs -> NotificationPrefsScreen(viewModel = viewModel)
                        is ScreenDestination.AboutApp -> AboutAppScreen(viewModel = viewModel)
                        is ScreenDestination.TicketThread -> TicketThreadScreen(ticketId = dest.ticketId, viewModel = viewModel)
                        is ScreenDestination.SavedAddresses -> SavedAddressesScreen(viewModel = viewModel)
                        is ScreenDestination.Wishlist -> WishlistScreen(viewModel = viewModel)
                        is ScreenDestination.Reviews -> ReviewsScreen(
                            targetId = dest.targetId,
                            targetName = dest.targetName,
                            targetType = dest.targetType,
                            viewModel = viewModel,
                            initialStarFilter = dest.initialStarFilter
                        )
                        is ScreenDestination.SellerSubscription -> SellerSubscriptionScreen(viewModel = viewModel)
                        is ScreenDestination.ReturnRequest -> ReturnRequestScreen(
                            orderId = dest.orderId,
                            viewModel = viewModel
                        )
                        is ScreenDestination.ReturnStatus -> ReturnStatusScreen(
                            returnId = dest.returnId,
                            viewModel = viewModel
                        )
                        is ScreenDestination.Deals -> BuyerDealsScreen(viewModel = viewModel)
                        is ScreenDestination.SellerReturns -> {
                            if (!hasSellerAccess) {
                                SuqiRestrictedState(
                                    title = strings.seller.sellerRestrictedTitle,
                                    subtitle = strings.seller.sellerRestrictedSub,
                                    primaryButtonText = if (activeRole == "GUEST") strings.signIn else strings.becomeSellerLabel,
                                    onPrimaryClick = {
                                        viewModel.navigate(if (activeRole == "GUEST") ScreenDestination.Auth else ScreenDestination.BecomeSeller)
                                    },
                                    onBackClick = { viewModel.goBack() },
                                    testTag = "seller_restricted"
                                )
                            } else {
                                SellerReturnsScreen(viewModel = viewModel)
                            }
                        }
                        is ScreenDestination.SellerEarnings -> {
                            if (!hasSellerAccess) {
                                SuqiRestrictedState(
                                    title = strings.seller.sellerRestrictedTitle,
                                    subtitle = strings.seller.sellerRestrictedSub,
                                    primaryButtonText = if (activeRole == "GUEST") strings.signIn else strings.becomeSellerLabel,
                                    onPrimaryClick = {
                                        viewModel.navigate(if (activeRole == "GUEST") ScreenDestination.Auth else ScreenDestination.BecomeSeller)
                                    },
                                    onBackClick = { viewModel.goBack() },
                                    testTag = "seller_restricted"
                                )
                            } else {
                                SellerEarningsScreen(viewModel = viewModel)
                            }
                        }
                        is ScreenDestination.SellerAnalytics -> {
                            if (!hasSellerAccess) {
                                SuqiRestrictedState(
                                    title = strings.seller.sellerRestrictedTitle,
                                    subtitle = strings.seller.sellerRestrictedSub,
                                    primaryButtonText = if (activeRole == "GUEST") strings.signIn else strings.becomeSellerLabel,
                                    onPrimaryClick = {
                                        viewModel.navigate(if (activeRole == "GUEST") ScreenDestination.Auth else ScreenDestination.BecomeSeller)
                                    },
                                    onBackClick = { viewModel.goBack() },
                                    testTag = "seller_restricted"
                                )
                            } else {
                                SellerAnalyticsScreen(viewModel = viewModel)
                            }
                        }

                        // Admin Console (Role-Gated A-01)
                        is ScreenDestination.AdminSignIn -> AdminSignInScreen(viewModel = viewModel)
                        is ScreenDestination.AdminConsole -> {
                            if (!hasAdminAccess) {
                                SuqiRestrictedState(
                                    title = strings.admin.adminRestrictedTitle,
                                    subtitle = strings.admin.adminRestrictedSub,
                                    primaryButtonText = strings.admin.adminSignInBtn,
                                    onPrimaryClick = { viewModel.navigate(ScreenDestination.AdminSignIn) },
                                    onBackClick = { viewModel.goBack() },
                                    testTag = "admin_restricted"
                                )
                            } else {
                                AdminConsoleScreen(viewModel = viewModel)
                            }
                        }
                        is ScreenDestination.OwnerDashboard -> {
                            val isOwnerTier = currentUser?.adminTier?.equals("OWNER", ignoreCase = true) == true ||
                                adminTier.equals("OWNER", ignoreCase = true)
                            if (!hasAdminAccess || !isOwnerTier) {
                                SuqiRestrictedState(
                                    title = strings.adminGov.tierOwnerOnlyNotice,
                                    subtitle = strings.adminGov.unlockOwnerHint,
                                    primaryButtonText = strings.admin.adminSignInBtn,
                                    onPrimaryClick = { viewModel.navigate(ScreenDestination.AdminSignIn) },
                                    onBackClick = { viewModel.goBack() },
                                    testTag = "owner_restricted"
                                )
                            } else {
                                OwnerDashboardScreen(viewModel = viewModel)
                            }
                        }
                        is ScreenDestination.AdminFeatures -> AdminFeaturesScreen(viewModel = viewModel)
                        is ScreenDestination.AdminCategories -> AdminCategoriesScreen(viewModel = viewModel)
                        is ScreenDestination.AdminItemTypes -> AdminItemTypesScreen(viewModel = viewModel)
                        is ScreenDestination.AdminBanners -> AdminBannersScreen(viewModel = viewModel)
                        is ScreenDestination.AdminModeration -> AdminModerationScreen(viewModel = viewModel)
                        is ScreenDestination.AdminSubscriptionPlans -> AdminSubscriptionPlansScreen(viewModel = viewModel)
                        is ScreenDestination.AdminFinance -> {
                            if (!hasAdminAccess) {
                                SuqiRestrictedState(
                                    title = strings.admin.adminRestrictedTitle,
                                    subtitle = strings.admin.adminRestrictedSub,
                                    primaryButtonText = strings.admin.adminSignInBtn,
                                    onPrimaryClick = { viewModel.navigate(ScreenDestination.AdminSignIn) },
                                    onBackClick = { viewModel.goBack() },
                                    testTag = "admin_restricted"
                                )
                            } else {
                                AdminFinanceScreen(viewModel = viewModel)
                            }
                        }
                        is ScreenDestination.AdminSupportInbox -> {
                            if (!hasAdminAccess) {
                                SuqiRestrictedState(
                                    title = strings.admin.adminRestrictedTitle,
                                    subtitle = strings.admin.adminRestrictedSub,
                                    primaryButtonText = strings.admin.adminSignInBtn,
                                    onPrimaryClick = { viewModel.navigate(ScreenDestination.AdminSignIn) },
                                    onBackClick = { viewModel.goBack() },
                                    testTag = "admin_restricted"
                                )
                            } else {
                                AdminSupportInboxScreen(viewModel = viewModel)
                            }
                        }
                        is ScreenDestination.AdminBroadcast -> {
                            if (!hasAdminAccess) {
                                SuqiRestrictedState(
                                    title = strings.admin.adminRestrictedTitle,
                                    subtitle = strings.admin.adminRestrictedSub,
                                    primaryButtonText = strings.admin.adminSignInBtn,
                                    onPrimaryClick = { viewModel.navigate(ScreenDestination.AdminSignIn) },
                                    onBackClick = { viewModel.goBack() },
                                    testTag = "admin_restricted"
                                )
                            } else {
                                AdminBroadcastScreen(viewModel = viewModel)
                            }
                        }
                        is ScreenDestination.AdminDevTools -> AdminDevToolsScreen(viewModel = viewModel)
                        is ScreenDestination.AdminUsers -> {
                            if (!hasAdminAccess) {
                                SuqiRestrictedState(
                                    title = strings.admin.adminRestrictedTitle,
                                    subtitle = strings.admin.adminRestrictedSub,
                                    primaryButtonText = strings.admin.adminSignInBtn,
                                    onPrimaryClick = { viewModel.navigate(ScreenDestination.AdminSignIn) },
                                    onBackClick = { viewModel.goBack() },
                                    testTag = "admin_restricted"
                                )
                            } else {
                                AdminUsersScreen(viewModel = viewModel)
                            }
                        }
                        is ScreenDestination.AdminUserDetail -> {
                            if (!hasAdminAccess) {
                                SuqiRestrictedState(
                                    title = strings.admin.adminRestrictedTitle,
                                    subtitle = strings.admin.adminRestrictedSub,
                                    primaryButtonText = strings.admin.adminSignInBtn,
                                    onPrimaryClick = { viewModel.navigate(ScreenDestination.AdminSignIn) },
                                    onBackClick = { viewModel.goBack() },
                                    testTag = "admin_restricted"
                                )
                            } else {
                                AdminUserDetailScreen(userId = dest.userId, viewModel = viewModel)
                            }
                        }
                        is ScreenDestination.AdminShopsManager -> {
                            if (!hasAdminAccess) {
                                SuqiRestrictedState(
                                    title = strings.admin.adminRestrictedTitle,
                                    subtitle = strings.admin.adminRestrictedSub,
                                    primaryButtonText = strings.admin.adminSignInBtn,
                                    onPrimaryClick = { viewModel.navigate(ScreenDestination.AdminSignIn) },
                                    onBackClick = { viewModel.goBack() },
                                    testTag = "admin_restricted"
                                )
                            } else {
                                AdminShopsManagerScreen(viewModel = viewModel)
                            }
                        }
                        is ScreenDestination.AdminShopDetail -> {
                            if (!hasAdminAccess) {
                                SuqiRestrictedState(
                                    title = strings.admin.adminRestrictedTitle,
                                    subtitle = strings.admin.adminRestrictedSub,
                                    primaryButtonText = strings.admin.adminSignInBtn,
                                    onPrimaryClick = { viewModel.navigate(ScreenDestination.AdminSignIn) },
                                    onBackClick = { viewModel.goBack() },
                                    testTag = "admin_restricted"
                                )
                            } else {
                                AdminShopDetailScreen(shopId = dest.shopId, viewModel = viewModel)
                            }
                        }
                        is ScreenDestination.AdminAuditLog -> {
                            if (!hasAdminAccess) {
                                SuqiRestrictedState(
                                    title = strings.admin.adminRestrictedTitle,
                                    subtitle = strings.admin.adminRestrictedSub,
                                    primaryButtonText = strings.admin.adminSignInBtn,
                                    onPrimaryClick = { viewModel.navigate(ScreenDestination.AdminSignIn) },
                                    onBackClick = { viewModel.goBack() },
                                    testTag = "admin_restricted"
                                )
                            } else {
                                AdminAuditLogScreen(viewModel = viewModel)
                            }
                        }
                        is ScreenDestination.AppSettings -> AppSettingsScreen(viewModel = viewModel)
                        is ScreenDestination.HelpLegal -> HelpLegalScreen(viewModel = viewModel)
                        }
                    }
                    }
                }
            }

            // Buyer Floating Liquid Bottom Navigation
            if (isBuyerRootNav) {
                SuqiFloatingBottomNav(
                    currentRoute = currentBuyerRouteKey,
                    onNavigate = { route ->
                        when (route) {
                            "home" -> viewModel.switchBuyerTab(ScreenDestination.Home)
                            "categories" -> viewModel.switchBuyerTab(ScreenDestination.Categories)
                            "search" -> viewModel.switchBuyerTab(ScreenDestination.Search)
                            "shops" -> viewModel.switchBuyerTab(ScreenDestination.Shops)
                            "account" -> viewModel.switchBuyerTab(ScreenDestination.Account)
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            // Seller 5-Tab Floating Bottom Navigation (F-02)
            if (isSellerRootNav && hasSellerAccess) {
                SuqiSellerFloatingBottomNav(
                    currentRoute = currentSellerRouteKey,
                    pendingOrdersCount = sellerPendingCount,
                    onNavigate = { route ->
                        when (route) {
                            "seller_dashboard" -> viewModel.switchSellerTab(ScreenDestination.SellerStudio)
                            "seller_orders" -> viewModel.switchSellerTab(ScreenDestination.SellerOrders)
                            "seller_products" -> viewModel.switchSellerTab(ScreenDestination.ProductManagement)
                            "seller_inbox" -> viewModel.switchSellerTab(ScreenDestination.SellerInbox)
                            "seller_more" -> viewModel.switchSellerTab(ScreenDestination.SellerMore)
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            // B-16 Guest Gate Bottom Sheet
            val gateAction = guestGateAction
            if (gateAction != null) {
                SuqiGuestGateSheet(
                    actionLabel = gateAction,
                    onSignInClick = {
                        viewModel.dismissGuestGate()
                        viewModel.navigate(ScreenDestination.Auth)
                    },
                    onDismiss = { viewModel.dismissGuestGate() }
                )
            }
        }
    }
}
