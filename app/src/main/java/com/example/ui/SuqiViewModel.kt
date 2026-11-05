package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.SuqiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ScreenDestination {
    data object Home : ScreenDestination()
    data object Categories : ScreenDestination()
    data class CategoryListing(val categoryId: String, val categoryName: String) : ScreenDestination()
    data object Search : ScreenDestination()
    data object Shops : ScreenDestination()
    data class ShopStorefront(val shopId: String) : ScreenDestination()
    data class ProductDetails(val productId: String) : ScreenDestination()
    data object Cart : ScreenDestination()
    data class CheckoutDelivery(val shopId: String) : ScreenDestination()
    data class CheckoutPayment(val shopId: String) : ScreenDestination()
    data class OrderSubmitted(val orderReference: String, val orderId: String) : ScreenDestination()
    data object MyOrders : ScreenDestination()
    data class OrderDetails(val orderId: String) : ScreenDestination()
    data object Account : ScreenDestination()
    data object Auth : ScreenDestination()
    data object Onboarding : ScreenDestination()
    data object BecomeSeller : ScreenDestination()
    data object SellerStudio : ScreenDestination()
    data class AddEditProduct(val productId: String? = null) : ScreenDestination()
    data object ProductManagement : ScreenDestination()
    data object PaymentMethodsSetup : ScreenDestination()
    data object DeliverySetup : ScreenDestination()
    data object VerificationQueue : ScreenDestination()
    data object SellerOrders : ScreenDestination()
    data object SellerVerification : ScreenDestination()
    data object Promotions : ScreenDestination()
    data object ShopSettings : ScreenDestination()
    data object ChatList : ScreenDestination()
    data class ChatThread(val shopId: String, val productContextName: String? = null, val productContextPrice: String? = null, val productContextImage: String? = null) : ScreenDestination()
    data object Notifications : ScreenDestination()
    data object SavedAddresses : ScreenDestination()
    data object Wishlist : ScreenDestination()
    data object Following : ScreenDestination()
    data class Reviews(val targetId: String, val targetName: String, val targetType: String) : ScreenDestination()
    data object SellerSubscription : ScreenDestination()
    data object AdminConsole : ScreenDestination()
    data object AdminFeatures : ScreenDestination()
    data object AdminCategories : ScreenDestination()
    data object AdminBanners : ScreenDestination()
    data object AdminModeration : ScreenDestination()
    data object AdminSubscriptionPlans : ScreenDestination()
    data object AppSettings : ScreenDestination()
    data object HelpLegal : ScreenDestination()
}

class SuqiViewModel(application: Application) : AndroidViewModel(application) {

    val repository = SuqiRepository.getInstance(application)

    // Navigation Stack
    private val _navStack = MutableStateFlow<List<ScreenDestination>>(listOf(ScreenDestination.Home))
    val currentScreen: StateFlow<ScreenDestination> = MutableStateFlow(ScreenDestination.Home)

    private val _currentScreenState = MutableStateFlow<ScreenDestination>(ScreenDestination.Home)
    val screen: StateFlow<ScreenDestination> = _currentScreenState.asStateFlow()

    fun navigate(destination: ScreenDestination) {
        val currentList = _navStack.value.toMutableList()
        currentList.add(destination)
        _navStack.value = currentList
        _currentScreenState.value = destination
    }

    fun goBack(): Boolean {
        val currentList = _navStack.value.toMutableList()
        if (currentList.size > 1) {
            currentList.removeAt(currentList.size - 1)
            _navStack.value = currentList
            _currentScreenState.value = currentList.last()
            return true
        }
        return false
    }

    fun popToRoot(destination: ScreenDestination = ScreenDestination.Home) {
        _navStack.value = listOf(destination)
        _currentScreenState.value = destination
    }

    // Cart Count
    val cartCount = repository.cartCount

    // Theme Mode: null = system default, true = dark mode, false = light mode
    val isDarkMode = MutableStateFlow<Boolean?>(null)
    fun setDarkMode(dark: Boolean?) {
        isDarkMode.value = dark
    }

    // Unread notification badge count (default 3 matching FitHub / Suqi designs)
    val unreadNotificationCount = MutableStateFlow(3)
    fun clearNotificationCount() {
        unreadNotificationCount.value = 0
    }

    // Location State
    val selectedRegion = MutableStateFlow("Addis Ababa")
    val selectedCity = MutableStateFlow("Addis Ababa")
    val selectedSubCity = MutableStateFlow("Bole")
    val selectedLanguage = MutableStateFlow("English")

    // Active Checkout State for current shop checkout
    val activeCheckoutDeliveryAddress = MutableStateFlow("Bole, Addis Ababa, Ethiopia")
    val activeCheckoutRecipientName = MutableStateFlow("Fitsum Gebre")
    val activeCheckoutRecipientPhone = MutableStateFlow("+251 912 345 678")
    val activeCheckoutDeliveryOption = MutableStateFlow<ShopDeliveryOptionDto?>(null)
    val activeCheckoutPaymentMethod = MutableStateFlow<ShopPaymentMethodDto?>(null)
    val activeCheckoutTransactionRef = MutableStateFlow("")
    val activeCheckoutPayerIdentifier = MutableStateFlow("")
    val activeCheckoutTotalAmount = MutableStateFlow(0.0)
    val activeCheckoutShopPromoCode = MutableStateFlow<PromoCodeDto?>(null)
    val activeCheckoutItems = MutableStateFlow<List<CartItemDto>>(emptyList())
    val activeCheckoutShopId = MutableStateFlow("")
    val activeCheckoutShopName = MutableStateFlow("")

    // Toast message channel
    val toastMessage = MutableStateFlow<String?>(null)
    fun showToast(message: String) {
        toastMessage.value = message
    }
    fun clearToast() {
        toastMessage.value = null
    }

    init {
        viewModelScope.launch {
            repository.refreshCartCount()
        }
    }
}
