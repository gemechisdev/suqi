package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.SuqiFloatingBottomNav
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SuqiBackground
import com.example.ui.theme.SuqiPrimary
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val viewModel: SuqiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkPref by viewModel.isDarkMode.collectAsState()
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val darkTheme = isDarkPref ?: systemDark

            MyApplicationTheme(darkTheme = darkTheme) {
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
    val currentScreen by viewModel.screen.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

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

    // Determine whether to display the floating liquid bottom navigation bar
    // Bottom nav is shown ONLY on root destinations, and hidden on sub-screens like Checkout, ProductDetails, Cart, etc.
    val isRootNavScreen = currentScreen in listOf(
        ScreenDestination.Home,
        ScreenDestination.Categories,
        ScreenDestination.Search,
        ScreenDestination.Shops,
        ScreenDestination.Account
    )

    val currentRouteKey = when (currentScreen) {
        ScreenDestination.Home -> "home"
        ScreenDestination.Categories -> "categories"
        ScreenDestination.Search -> "search"
        ScreenDestination.Shops -> "shops"
        ScreenDestination.Account -> "account"
        else -> ""
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .padding(bottom = if (isRootNavScreen) 80.dp else 16.dp)
                    .navigationBarsPadding()
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = SuqiPrimary,
                    contentColor = androidx.compose.ui.graphics.Color.White,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
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
            // Render active screen
            when (val dest = currentScreen) {
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
                is ScreenDestination.Auth -> AuthScreen(viewModel = viewModel)
                is ScreenDestination.Onboarding -> OnboardingScreen(viewModel = viewModel)
                is ScreenDestination.BecomeSeller -> BecomeSellerScreen(viewModel = viewModel)
                is ScreenDestination.SellerStudio -> SellerStudioScreen(viewModel = viewModel)
                is ScreenDestination.AddEditProduct -> AddEditProductScreen(
                    productId = dest.productId,
                    viewModel = viewModel
                )
                is ScreenDestination.ProductManagement -> ProductManagementScreen(viewModel = viewModel)
                is ScreenDestination.PaymentMethodsSetup -> PaymentMethodsSetupScreen(viewModel = viewModel)
                is ScreenDestination.DeliverySetup -> DeliverySetupScreen(viewModel = viewModel)
                is ScreenDestination.VerificationQueue -> VerificationQueueScreen(viewModel = viewModel)
                is ScreenDestination.SellerOrders -> SellerOrdersScreen(viewModel = viewModel)
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
                    viewModel = viewModel
                )
                is ScreenDestination.Notifications -> NotificationsScreen(viewModel = viewModel)
                is ScreenDestination.SavedAddresses -> SavedAddressesScreen(viewModel = viewModel)
                is ScreenDestination.Wishlist -> WishlistScreen(viewModel = viewModel)
                is ScreenDestination.Reviews -> ReviewsScreen(
                    targetId = dest.targetId,
                    targetName = dest.targetName,
                    targetType = dest.targetType,
                    viewModel = viewModel
                )
                is ScreenDestination.SellerSubscription -> SellerSubscriptionScreen(viewModel = viewModel)
                is ScreenDestination.AdminConsole -> AdminConsoleScreen(viewModel = viewModel)
                is ScreenDestination.AdminFeatures -> AdminFeaturesScreen(viewModel = viewModel)
                is ScreenDestination.AdminCategories -> AdminCategoriesScreen(viewModel = viewModel)
                is ScreenDestination.AdminBanners -> AdminBannersScreen(viewModel = viewModel)
                is ScreenDestination.AdminModeration -> AdminModerationScreen(viewModel = viewModel)
                is ScreenDestination.AdminSubscriptionPlans -> AdminSubscriptionPlansScreen(viewModel = viewModel)
                is ScreenDestination.AppSettings -> AppSettingsScreen(viewModel = viewModel)
                is ScreenDestination.HelpLegal -> HelpLegalScreen(viewModel = viewModel)
            }

            // Floating Liquid Bottom Navigation
            if (isRootNavScreen) {
                SuqiFloatingBottomNav(
                    currentRoute = currentRouteKey,
                    onNavigate = { route ->
                        when (route) {
                            "home" -> viewModel.popToRoot(ScreenDestination.Home)
                            "categories" -> viewModel.navigate(ScreenDestination.Categories)
                            "search" -> viewModel.navigate(ScreenDestination.Search)
                            "shops" -> viewModel.navigate(ScreenDestination.Shops)
                            "account" -> viewModel.navigate(ScreenDestination.Account)
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
