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
    data object ProfileEdit : ScreenDestination()
    data object Auth : ScreenDestination()
    data object Onboarding : ScreenDestination()
    data object BecomeSeller : ScreenDestination()
    data object SellerStudio : ScreenDestination()
    data class AddEditProduct(val productId: String? = null) : ScreenDestination()
    data object ProductManagement : ScreenDestination()
    data object SellerInventory : ScreenDestination()
    data object PaymentMethodsSetup : ScreenDestination()
    data object DeliverySetup : ScreenDestination()
    data object VerificationQueue : ScreenDestination()
    data object SellerOrders : ScreenDestination()
    data class SellerOrderDetail(val orderId: String) : ScreenDestination()
    data object SellerInbox : ScreenDestination()
    data object SellerMore : ScreenDestination()
    data object SellerVerification : ScreenDestination()
    data object Promotions : ScreenDestination()
    data object ShopSettings : ScreenDestination()
    data object ChatList : ScreenDestination()
    data class ChatThread(
        val shopId: String,
        val productContextName: String? = null,
        val productContextPrice: String? = null,
        val productContextImage: String? = null
    ) : ScreenDestination()
    data object Notifications : ScreenDestination()
    data object SavedAddresses : ScreenDestination()
    data object Wishlist : ScreenDestination()
    data object Following : ScreenDestination()
    data class Reviews(val targetId: String, val targetName: String, val targetType: String) : ScreenDestination()
    data object SellerSubscription : ScreenDestination()
    data object AdminSignIn : ScreenDestination()
    data object AdminConsole : ScreenDestination()
    data object OwnerDashboard : ScreenDestination()
    data object AdminFeatures : ScreenDestination()
    data object AdminCategories : ScreenDestination()
    data object AdminItemTypes : ScreenDestination()
    data object AdminBanners : ScreenDestination()
    data object AdminModeration : ScreenDestination()
    data object AdminSubscriptionPlans : ScreenDestination()
    data object AdminDevTools : ScreenDestination()
    data object AppSettings : ScreenDestination()
    data object HelpLegal : ScreenDestination()
}

data class NavEntry(
    val id: String,
    val destination: ScreenDestination
)

class SuqiViewModel(application: Application) : AndroidViewModel(application) {

    val repository = SuqiRepository.getInstance(application)

    private var navSequence = 0L
    private fun nextNavId(): String = "nav_${++navSequence}"

    private val sellerRootDestinations = setOf(
        ScreenDestination.SellerStudio,
        ScreenDestination.SellerOrders,
        ScreenDestination.ProductManagement,
        ScreenDestination.SellerInbox,
        ScreenDestination.SellerMore
    )

    // Navigation Stack with unique entry IDs for SaveableStateHolder state & scroll preservation
    private val initialHomeEntry = NavEntry(nextNavId(), ScreenDestination.Home)
    private val _navStack = MutableStateFlow<List<NavEntry>>(listOf(initialHomeEntry))
    val navBackStack: StateFlow<List<NavEntry>> = _navStack.asStateFlow()

    private val _currentNavEntry = MutableStateFlow(initialHomeEntry)
    val currentNavEntry: StateFlow<NavEntry> = _currentNavEntry.asStateFlow()

    private val _currentScreenState = MutableStateFlow<ScreenDestination>(ScreenDestination.Home)
    val screen: StateFlow<ScreenDestination> = _currentScreenState.asStateFlow()

    private val _disposedNavIds = MutableStateFlow<Set<String>>(emptySet())
    val disposedNavIds: StateFlow<Set<String>> = _disposedNavIds.asStateFlow()

    fun acknowledgeDisposedNavIds(ids: Set<String>) {
        if (ids.isEmpty()) return
        _disposedNavIds.value = _disposedNavIds.value - ids
    }

    private fun updateNavStack(next: List<NavEntry>) {
        val prev = _navStack.value
        val normalized = next.ifEmpty { listOf(NavEntry(nextNavId(), ScreenDestination.Home)) }
        val keptIds = normalized.mapTo(HashSet()) { it.id }
        val removedIds = prev.map { it.id }.filter { it !in keptIds }
        if (removedIds.isNotEmpty()) {
            _disposedNavIds.value = _disposedNavIds.value + removedIds
        }
        _navStack.value = normalized
        val top = normalized.last()
        _currentNavEntry.value = top
        _currentScreenState.value = top.destination
    }

    fun navigate(destination: ScreenDestination) {
        val currentList = _navStack.value
        if (currentList.lastOrNull()?.destination == destination) return
        updateNavStack(currentList + NavEntry(nextNavId(), destination))
    }

    fun switchBuyerTab(destination: ScreenDestination) {
        val currentList = _navStack.value
        if (currentList.lastOrNull()?.destination == destination) return
        val homeRoot = currentList.firstOrNull { it.destination == ScreenDestination.Home }
            ?: NavEntry(nextNavId(), ScreenDestination.Home)
        if (destination == ScreenDestination.Home) {
            updateNavStack(listOf(homeRoot))
        } else {
            updateNavStack(listOf(homeRoot, NavEntry(nextNavId(), destination)))
        }
    }

    fun switchSellerTab(destination: ScreenDestination) {
        val currentList = _navStack.value
        if (currentList.lastOrNull()?.destination == destination) return
        val firstSellerIdx = currentList.indexOfFirst { it.destination in sellerRootDestinations }
        val prefix: List<NavEntry>
        val studioEntry: NavEntry
        if (firstSellerIdx >= 0) {
            prefix = currentList.subList(0, firstSellerIdx)
            studioEntry = currentList.getOrNull(firstSellerIdx)
                ?.takeIf { it.destination == ScreenDestination.SellerStudio }
                ?: NavEntry(nextNavId(), ScreenDestination.SellerStudio)
        } else {
            val homeRoot = currentList.firstOrNull { it.destination == ScreenDestination.Home }
                ?: NavEntry(nextNavId(), ScreenDestination.Home)
            prefix = if (currentList.size > 1) {
                currentList
            } else {
                listOf(homeRoot, NavEntry(nextNavId(), ScreenDestination.Account))
            }
            studioEntry = NavEntry(nextNavId(), ScreenDestination.SellerStudio)
        }
        if (destination == ScreenDestination.SellerStudio) {
            updateNavStack(prefix + listOf(studioEntry))
        } else {
            updateNavStack(prefix + listOf(studioEntry, NavEntry(nextNavId(), destination)))
        }
    }

    fun completeAdminSignIn() {
        val currentList = _navStack.value.toMutableList()
        while (currentList.isNotEmpty() && currentList.last().destination in setOf(
                ScreenDestination.AdminSignIn,
                ScreenDestination.AdminConsole,
                ScreenDestination.OwnerDashboard
            )
        ) {
            currentList.removeAt(currentList.lastIndex)
        }
        val prefix = if (currentList.isEmpty()) {
            listOf(
                NavEntry(nextNavId(), ScreenDestination.Home),
                NavEntry(nextNavId(), ScreenDestination.Account)
            )
        } else {
            currentList
        }
        updateNavStack(prefix + NavEntry(nextNavId(), ScreenDestination.AdminConsole))
    }

    fun lockAdminSession() {
        setActiveRole("CUSTOMER")
        val adminFlowDestinations = setOf(
            ScreenDestination.AdminSignIn,
            ScreenDestination.AdminConsole,
            ScreenDestination.OwnerDashboard,
            ScreenDestination.AdminFeatures,
            ScreenDestination.AdminCategories,
            ScreenDestination.AdminItemTypes,
            ScreenDestination.AdminBanners,
            ScreenDestination.AdminModeration,
            ScreenDestination.AdminSubscriptionPlans,
            ScreenDestination.AdminDevTools
        )
        val currentList = _navStack.value.toMutableList()
        while (currentList.isNotEmpty() && currentList.last().destination in adminFlowDestinations) {
            currentList.removeAt(currentList.lastIndex)
        }
        val prefix = if (currentList.isEmpty()) {
            listOf(
                NavEntry(nextNavId(), ScreenDestination.Home),
                NavEntry(nextNavId(), ScreenDestination.Account)
            )
        } else {
            currentList
        }
        updateNavStack(prefix + NavEntry(nextNavId(), ScreenDestination.AdminSignIn))
    }

    fun completeAuth() {
        val currentList = _navStack.value
        if (currentList.size > 1 && currentList.last().destination == ScreenDestination.Auth) {
            goBack()
        } else {
            popToRoot(ScreenDestination.Home)
        }
    }

    fun navigateAfterOrderSubmitted(destination: ScreenDestination.OrderSubmitted) {
        val currentList = _navStack.value.toMutableList()
        while (currentList.isNotEmpty() && (
                currentList.last().destination is ScreenDestination.CheckoutDelivery ||
                    currentList.last().destination is ScreenDestination.CheckoutPayment
                )
        ) {
            currentList.removeAt(currentList.lastIndex)
        }
        updateNavStack(currentList + NavEntry(nextNavId(), destination))
    }

    fun goBack(): Boolean {
        val currentList = _navStack.value
        if (currentList.size > 1) {
            updateNavStack(currentList.dropLast(1))
            return true
        }
        return false
    }

    fun popToRoot(destination: ScreenDestination = ScreenDestination.Home) {
        val reusable = if (destination == ScreenDestination.Home) {
            _navStack.value.firstOrNull { it.destination == ScreenDestination.Home }
        } else {
            null
        }
        updateNavStack(listOf(reusable ?: NavEntry(nextNavId(), destination)))
    }

    // Lightweight In-Memory Screen Caches so returning via Back renders lists immediately at restored scroll offsets
    var cachedHomeProducts: List<ProductDto> = emptyList()
    var cachedHomeShops: List<ShopDto> = emptyList()
    var cachedHomeCategories: List<CategoryDto> = emptyList()
    var cachedHomeBanners: List<PromoBannerDto> = emptyList()
    var cachedAdminSettings: AdminSettingsDto? = null
    val cachedCategoryProducts: MutableMap<String, List<ProductDto>> = mutableMapOf()
    var cachedAllProducts: List<ProductDto> = emptyList()
    var cachedAllShops: List<ShopDto> = emptyList()
    var cachedAllCategories: List<CategoryDto> = emptyList()
    val cachedProductDetails: MutableMap<String, ProductDto> = mutableMapOf()
    val cachedProductReviews: MutableMap<String, List<ReviewDto>> = mutableMapOf()
    val cachedProductQuestions: MutableMap<String, List<ProductQuestionDto>> = mutableMapOf()
    val cachedShopDeliveryOptions: MutableMap<String, List<ShopDeliveryOptionDto>> = mutableMapOf()
    val cachedShopsByFilter: MutableMap<String, List<ShopDto>> = mutableMapOf()
    val cachedShopDetails: MutableMap<String, ShopDto> = mutableMapOf()
    val cachedShopProducts: MutableMap<String, List<ProductDto>> = mutableMapOf()
    val cachedShopReviews: MutableMap<String, List<ReviewDto>> = mutableMapOf()
    var cachedBuyerOrders: List<OrderDto> = emptyList()
    var cachedCartItems: List<CartItemDto> = emptyList()
    var cachedNotifications: List<NotificationDto> = emptyList()
    var cachedSavedAddresses: List<SavedAddressDto> = emptyList()
    var cachedFollowedShops: List<ShopDto> = emptyList()
    var cachedBuyerConversations: List<ChatMessageDto> = emptyList()
    var cachedSellerDashboard: Map<String, Any> = emptyMap()
    var cachedSellerQueue: List<OrderDto> = emptyList()
    var cachedSellerOrders: List<OrderDto> = emptyList()
    var cachedSellerProducts: List<ProductDto> = emptyList()
    var cachedSellerConversations: List<ChatMessageDto> = emptyList()
    var cachedAdminAllShops: List<ShopDto> = emptyList()
    var cachedAdminPendingShops: List<ShopDto> = emptyList()
    var cachedAdminFlaggedReviews: List<FlaggedReviewDto> = emptyList()
    var cachedAdminTickets: List<SupportTicketDto> = emptyList()
    var cachedAdminAllOrders: List<OrderDto> = emptyList()
    var cachedAdminItemTypes: List<ItemTypeDto> = emptyList()
    var cachedAdminPlans: List<SubscriptionPlanDto> = emptyList()
    val cachedOwnerMetrics: MutableMap<Int, OwnerDashboardMetricsDto> = mutableMapOf()

    // Session & Role State (F-02, B-16, A-01)
    private val _currentUser = MutableStateFlow<UserDto?>(null)
    val currentUser: StateFlow<UserDto?> = _currentUser.asStateFlow()

    private val _activeRole = MutableStateFlow("CUSTOMER") // "GUEST", "CUSTOMER", "SELLER", "ADMIN"
    val activeRole: StateFlow<String> = _activeRole.asStateFlow()

    private val _adminTier = MutableStateFlow("NONE") // "OWNER", "ADMIN", "DEVELOPER", "NONE"
    val adminTier: StateFlow<String> = _adminTier.asStateFlow()

    private val _isDeveloperUnlocked = MutableStateFlow(false)
    val isDeveloperUnlocked: StateFlow<Boolean> = _isDeveloperUnlocked.asStateFlow()

    fun unlockDeveloperTools() {
        _isDeveloperUnlocked.value = true
    }

    private val _guestGateAction = MutableStateFlow<String?>(null)
    val guestGateAction: StateFlow<String?> = _guestGateAction.asStateFlow()

    fun requireSignedInOrPrompt(actionLabel: String, onAllowed: () -> Unit) {
        if (_activeRole.value == "GUEST" || _currentUser.value?.role == "GUEST") {
            _guestGateAction.value = actionLabel
        } else {
            onAllowed()
        }
    }

    fun dismissGuestGate() {
        _guestGateAction.value = null
    }

    fun setActiveRole(role: String) {
        _activeRole.value = role
        viewModelScope.launch {
            repository.updateUserProfile(mapOf("activeRole" to role))
        }
    }

    // Connectivity / Offline mode banner toggle (F-05)
    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()
    fun setOfflineMode(offline: Boolean) {
        _isOffline.value = offline
    }

    private val _adminConsoleInitialTab = MutableStateFlow("overview")
    val adminConsoleInitialTab: StateFlow<String> = _adminConsoleInitialTab.asStateFlow()

    fun openAdminConsoleTab(tabKey: String = "overview") {
        _adminConsoleInitialTab.value = tabKey
        navigate(ScreenDestination.AdminConsole)
    }

    fun consumeAdminConsoleInitialTab(): String {
        val current = _adminConsoleInitialTab.value
        _adminConsoleInitialTab.value = "overview"
        return current
    }


    // Cart Count
    val cartCount = repository.cartCount

    // Theme Mode: null = system default, true = dark mode, false = light mode
    val isDarkMode = MutableStateFlow<Boolean?>(null)
    fun setDarkMode(dark: Boolean?) {
        isDarkMode.value = dark
    }

    // Unread notification badge count (dynamically loaded from DB, initialized at 0)
    val unreadNotificationCount = MutableStateFlow(0)

    fun refreshUnreadNotifications() {
        viewModelScope.launch {
            val res = repository.getNotifications()
            if (res.success && res.data != null) {
                unreadNotificationCount.value = res.data.count { !it.isRead }
            }
        }
    }

    fun clearNotificationCount() {
        unreadNotificationCount.value = 0
        viewModelScope.launch {
            repository.markAllNotificationsRead()
        }
    }

    private val prefs = application.getSharedPreferences("suqi_prefs", android.content.Context.MODE_PRIVATE)

    // Location & Language State
    val selectedRegion = MutableStateFlow("Addis Ababa")
    val selectedCity = MutableStateFlow("Addis Ababa")
    val selectedSubCity = MutableStateFlow("Bole")
    val selectedLanguage = MutableStateFlow(prefs.getString("selected_language", "English") ?: "English")

    fun setSelectedLanguage(language: String) {
        val canonical = when (com.example.ui.i18n.SuqiLanguage.fromSetting(language)) {
            com.example.ui.i18n.SuqiLanguage.AM -> "Amharic"
            com.example.ui.i18n.SuqiLanguage.OM -> "Afaan Oromo"
            com.example.ui.i18n.SuqiLanguage.EN -> "English"
        }
        selectedLanguage.value = canonical
        prefs.edit().putString("selected_language", canonical).apply()
        viewModelScope.launch {
            repository.updateUserProfile(mapOf("preferredLanguage" to canonical))
        }
    }

    // Active Checkout State for current shop checkout (populated from active user & default address)
    val activeCheckoutDeliveryAddress = MutableStateFlow("")
    val activeCheckoutRecipientName = MutableStateFlow("")
    val activeCheckoutRecipientPhone = MutableStateFlow("")
    val activeCheckoutDeliveryOption = MutableStateFlow<ShopDeliveryOptionDto?>(null)
    val activeCheckoutPaymentMethod = MutableStateFlow<ShopPaymentMethodDto?>(null)
    val activeCheckoutTransactionRef = MutableStateFlow("")
    val activeCheckoutPayerIdentifier = MutableStateFlow("")
    val activeCheckoutReceiptUri = MutableStateFlow("")
    val activeCheckoutTotalAmount = MutableStateFlow(0.0)
    val activeCheckoutShopPromoCode = MutableStateFlow<PromoCodeDto?>(null)
    val activeCheckoutItems = MutableStateFlow<List<CartItemDto>>(emptyList())
    val activeCheckoutShopId = MutableStateFlow("")
    val activeCheckoutShopName = MutableStateFlow("")

    // Recently Viewed Products
    private val _recentlyViewed = MutableStateFlow<List<ProductDto>>(emptyList())
    val recentlyViewed: StateFlow<List<ProductDto>> = _recentlyViewed.asStateFlow()

    fun recordProductView(product: ProductDto) {
        val current = _recentlyViewed.value.filter { it.id != product.id }.toMutableList()
        current.add(0, product)
        _recentlyViewed.value = current.take(10)
    }

    // Toast message channel
    val toastMessage = MutableStateFlow<String?>(null)
    fun showToast(message: String) {
        toastMessage.value = localizeMessage(message)
    }

    /** Localizes known service messages into the active language; unknown text passes through. */
    fun localizeMessage(message: String?): String =
        com.example.ui.i18n.SuqiApiMessages.localize(
            message,
            com.example.ui.i18n.SuqiLanguage.fromSetting(selectedLanguage.value)
        )

    fun localizePlace(name: String?): String =
        com.example.ui.i18n.SuqiPlaces.localize(
            name,
            com.example.ui.i18n.SuqiLanguage.fromSetting(selectedLanguage.value)
        )

    fun clearToast() {
        toastMessage.value = null
    }

    /** One-tap add from product cards (quantity 1, default variation). */
    fun quickAddToCart(product: ProductDto) {
        viewModelScope.launch {
            val res = repository.addToCart(product.id, 1, "")
            val strings = com.example.ui.i18n.SuqiStringCatalog.forLanguage(
                com.example.ui.i18n.SuqiLanguage.fromSetting(selectedLanguage.value)
            )
            showToast(if (res.success) strings.addedToCart else (res.message ?: strings.common.failedToAddToast))
        }
    }

    fun refreshSession() {
        viewModelScope.launch {
            repository.refreshCartCount()
            val userRes = repository.getCurrentUser()
            if (userRes.success && userRes.data != null) {
                val u = userRes.data
                _currentUser.value = u
                _activeRole.value = u.activeRole.ifBlank { u.role }
                _adminTier.value = u.adminTier
                if (u.city.isNotBlank()) selectedCity.value = u.city
                if (u.subCity.isNotBlank()) selectedSubCity.value = u.subCity
                if (u.region.isNotBlank()) selectedRegion.value = u.region
                if (!prefs.contains("selected_language") && u.preferredLanguage.isNotBlank()) {
                    selectedLanguage.value = u.preferredLanguage
                    prefs.edit().putString("selected_language", u.preferredLanguage).apply()
                }

                val addrRes = repository.getSavedAddresses()
                val defAddr = addrRes.data?.firstOrNull { it.isDefault } ?: addrRes.data?.firstOrNull()
                if (defAddr != null) {
                    activeCheckoutRecipientName.value = defAddr.fullName
                    activeCheckoutRecipientPhone.value = defAddr.phone
                    activeCheckoutDeliveryAddress.value = listOfNotNull(
                        defAddr.streetOrLandmark.takeIf { it.isNotBlank() },
                        defAddr.subCity.takeIf { it.isNotBlank() },
                        defAddr.city.takeIf { it.isNotBlank() }
                    ).joinToString(", ")
                } else {
                    activeCheckoutRecipientName.value = if (u.role == "GUEST") "" else u.name
                    activeCheckoutRecipientPhone.value = u.phone
                    activeCheckoutDeliveryAddress.value = listOfNotNull(
                        u.subCity.takeIf { it.isNotBlank() },
                        u.city.takeIf { it.isNotBlank() }
                    ).joinToString(", ")
                }
            }
            refreshUnreadNotifications()
        }
    }

    init {
        refreshSession()
    }
}
