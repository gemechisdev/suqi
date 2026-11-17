package com.example.data.repository

import android.content.Context
import com.example.data.api.BackendConfig
import com.example.data.api.SuqiApiService
import com.example.data.backend.BuiltinSqliteBackend
import com.example.data.local.SuqiDatabase
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class SuqiRepository private constructor(context: Context) {

    private val db = SuqiDatabase.getInstance(context)
    private val builtinBackend: SuqiApiService = BuiltinSqliteBackend(db)

    // Remote Retrofit service for when BackendConfig.useBuiltinSqliteBackend is toggled to false
    private val remoteService: SuqiApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl(BackendConfig.customBaseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(SuqiApiService::class.java)
    }

    private val api: SuqiApiService
        get() = if (BackendConfig.useBuiltinSqliteBackend) builtinBackend else remoteService

    // Reactive Cart Count Flow
    private val _cartCount = MutableStateFlow(0)
    val cartCount: Flow<Int> = _cartCount.asStateFlow()

    suspend fun refreshCartCount() {
        val res = api.getCartItems()
        if (res.success && res.data != null) {
            _cartCount.value = res.data.sumOf { it.quantity }
        }
    }

    // User & Auth
    suspend fun getCurrentUser(): ApiResponse<UserDto> = api.getCurrentUser()
    suspend fun loginWithTelegram(name: String, phone: String): ApiResponse<UserDto> {
        val res = api.loginWithTelegram(mapOf("name" to name, "phone" to phone))
        refreshCartCount()
        return res
    }
    suspend fun loginWithGoogle(name: String, email: String): ApiResponse<UserDto> {
        val res = api.loginWithGoogle(mapOf("name" to name, "email" to email))
        refreshCartCount()
        return res
    }
    suspend fun loginWithPhoneOtp(phone: String, otp: String, name: String = ""): ApiResponse<UserDto> {
        val res = api.loginWithPhoneOtp(mapOf("phone" to phone, "otp" to otp, "name" to name))
        refreshCartCount()
        return res
    }
    suspend fun loginAsGuest(): ApiResponse<UserDto> {
        val res = api.loginAsGuest()
        refreshCartCount()
        return res
    }
    suspend fun adminSignIn(identifier: String, pin: String, tier: String = "OWNER"): ApiResponse<UserDto> {
        val res = api.adminSignIn(mapOf("identifier" to identifier, "pin" to pin, "tier" to tier))
        refreshCartCount()
        return res
    }
    suspend fun updateUserProfile(payload: Map<String, String>) = api.updateUserProfile(payload)
    suspend fun upgradeToSeller(details: Map<String, String>) = api.upgradeToSeller(details)

    // Marketplace
    suspend fun getProducts(
        query: String? = null,
        categoryId: String? = null,
        subCategory: String? = null,
        shopId: String? = null,
        inStockOnly: Boolean = false,
        sortBy: String = "POPULAR"
    ) = api.getProducts(query, categoryId, subCategory, shopId, inStockOnly, sortBy)

    suspend fun getProductDetails(id: String) = api.getProductDetails(id)
    suspend fun toggleWishlist(id: String) = api.toggleWishlist(id)
    suspend fun getProductQuestions(productId: String) = api.getProductQuestions(productId)
    suspend fun submitProductQuestion(productId: String, question: String) =
        api.submitProductQuestion(productId, mapOf("question" to question))
    suspend fun getShops(filter: String = "ALL", category: String? = null, query: String? = null) =
        api.getShops(filter, category, query)
    suspend fun getShopDetails(id: String) = api.getShopDetails(id)
    suspend fun toggleFollowShop(id: String) = api.toggleFollowShop(id)
    suspend fun getCategories() = api.getCategories()
    suspend fun getPromoBanners(activeOnly: Boolean = true) = api.getPromoBanners(activeOnly)

    // Cart
    suspend fun getCartItems() = api.getCartItems()
    suspend fun addToCart(productId: String, quantity: Int, variation: String): ApiResponse<CartItemDto> {
        val res = api.addToCart(mapOf("productId" to productId, "quantity" to quantity, "selectedVariation" to variation))
        refreshCartCount()
        return res
    }
    suspend fun updateCartQuantity(itemId: Long, quantity: Int): ApiResponse<Boolean> {
        val res = api.updateCartQuantity(itemId, quantity)
        refreshCartCount()
        return res
    }
    suspend fun removeCartItem(itemId: Long): ApiResponse<Boolean> {
        val res = api.removeCartItem(itemId)
        refreshCartCount()
        return res
    }
    suspend fun clearCart(): ApiResponse<Boolean> {
        val res = api.clearCart()
        refreshCartCount()
        return res
    }

    // Orders & Checkout
    suspend fun submitOrder(orderData: Map<String, Any>): ApiResponse<OrderDto> {
        val res = api.submitOrder(orderData)
        refreshCartCount()
        return res
    }
    suspend fun getOrders() = api.getOrders()
    suspend fun getOrderDetails(orderId: String) = api.getOrderDetails(orderId)
    suspend fun resubmitPayment(orderId: String, payload: Map<String, String>) = api.resubmitPayment(orderId, payload)
    suspend fun cancelOrder(orderId: String) = api.cancelOrder(orderId)

    // Seller Studio
    suspend fun getSellerDashboard() = api.getSellerDashboard()
    suspend fun getSellerProducts() = api.getSellerProducts()
    suspend fun createProduct(data: Map<String, Any>) = api.createProduct(data)
    suspend fun updateProduct(id: String, data: Map<String, Any>) = api.updateProduct(id, data)
    suspend fun updateProductStatus(id: String, status: String) = api.updateProductStatus(id, status)
    suspend fun updateProductStock(id: String, stockQuantity: Int, detailsJson: String? = null): ApiResponse<ProductDto> {
        val payload = buildMap<String, Any> {
            put("stockQuantity", stockQuantity)
            if (detailsJson != null) put("detailsJson", detailsJson)
        }
        return api.updateProductStock(id, payload)
    }
    suspend fun bulkUpdateProducts(ids: List<String>, action: String, value: String? = null): ApiResponse<Int> {
        val payload = buildMap<String, Any> {
            put("ids", ids)
            put("action", action)
            if (value != null) put("value", value)
        }
        return api.bulkUpdateProducts(payload)
    }
    suspend fun duplicateProduct(id: String) = api.duplicateProduct(id)
    suspend fun deleteProduct(id: String) = api.deleteProduct(id)
    suspend fun getSellerOrders() = api.getSellerOrders()
    suspend fun updateSellerOrderStatus(orderId: String, status: String) = api.updateSellerOrderStatus(orderId, status)
    suspend fun getSellerVerificationQueue() = api.getSellerVerificationQueue()
    suspend fun verifyPayment(orderId: String, payload: Map<String, String>) = api.verifyPayment(orderId, payload)
    suspend fun rejectPayment(orderId: String, payload: Map<String, String>) = api.rejectPayment(orderId, payload)
    suspend fun getShopPaymentMethods(shopId: String) = api.getShopPaymentMethods(shopId)
    suspend fun saveShopPaymentMethod(method: ShopPaymentMethodDto) = api.saveShopPaymentMethod(method)
    suspend fun deleteShopPaymentMethod(id: String) = api.deleteShopPaymentMethod(id)
    suspend fun getShopDeliveryOptions(shopId: String) = api.getShopDeliveryOptions(shopId)
    suspend fun saveShopDeliveryOption(option: ShopDeliveryOptionDto) = api.saveShopDeliveryOption(option)
    suspend fun deleteShopDeliveryOption(id: String) = api.deleteShopDeliveryOption(id)
    suspend fun getShopPromotions(shopId: String, includeInactive: Boolean = false) = api.getShopPromotions(shopId, includeInactive)
    suspend fun createPromoCode(promo: PromoCodeDto) = api.createPromoCode(promo)
    suspend fun deletePromoCode(id: String) = api.deletePromoCode(id)
    suspend fun submitSellerVerification(details: Map<String, String>) = api.submitSellerVerification(details)
    suspend fun updateShopSettings(settings: Map<String, Any>) = api.updateShopSettings(settings)

    // Chat, Reviews, Notifications, Addresses, Support
    suspend fun getChatConversations() = api.getChatConversations()
    suspend fun getChatMessages(shopId: String) = api.getChatMessages(shopId)
    suspend fun sendChatMessage(shopId: String, messageData: Map<String, Any>) = api.sendChatMessage(shopId, messageData)
    suspend fun markChatConversationRead(shopId: String, isRead: Boolean) = api.markChatConversationRead(shopId, isRead)
    suspend fun getReviews(targetId: String) = api.getReviews(targetId)
    suspend fun submitReview(data: Map<String, Any>) = api.submitReview(data)
    suspend fun getNotifications() = api.getNotifications()
    suspend fun markNotificationRead(id: String, isRead: Boolean) = api.markNotificationRead(id, isRead)
    suspend fun markAllNotificationsRead() = api.markAllNotificationsRead()
    suspend fun deleteNotification(id: String) = api.deleteNotification(id)
    suspend fun getSavedAddresses() = api.getSavedAddresses()
    suspend fun saveAddress(address: SavedAddressDto) = api.saveAddress(address)
    suspend fun deleteSavedAddress(id: String) = api.deleteSavedAddress(id)
    suspend fun submitSupportTicket(ticket: SupportTicketDto) = api.submitSupportTicket(ticket)
    suspend fun getSupportTickets() = api.getSupportTickets()

    // Admin
    suspend fun getAdminSettings() = api.getAdminSettings()
    suspend fun updateFeatureToggle(feature: String, enabled: Boolean) = api.updateFeatureToggle(feature, enabled)
    suspend fun getAdminSellerVerificationQueue() = api.getAdminSellerVerificationQueue()
    suspend fun adminVerifySellerDecision(
        shopId: String,
        decision: String,
        reason: String? = null,
        failedKeys: String? = null
    ) = api.adminVerifySellerDecision(shopId, decision, reason, failedKeys)
    suspend fun saveCategory(category: CategoryDto) = api.saveCategory(category)
    suspend fun reorderCategories(orderedIds: List<String>) = api.reorderCategories(orderedIds)
    suspend fun deleteCategory(id: String) = api.deleteCategory(id)
    suspend fun savePromoBanner(banner: PromoBannerDto) = api.savePromoBanner(banner)
    suspend fun reorderPromoBanners(orderedIds: List<String>) = api.reorderPromoBanners(orderedIds)
    suspend fun deletePromoBanner(id: String) = api.deletePromoBanner(id)
    suspend fun updateHomeSectionsConfig(order: List<String>, hidden: List<String>) =
        api.updateHomeSectionsConfig(mapOf("order" to order, "hidden" to hidden))
    suspend fun getItemTypes() = api.getItemTypes()
    suspend fun saveItemType(itemType: ItemTypeDto) = api.saveItemType(itemType)
    suspend fun deleteItemType(id: String) = api.deleteItemType(id)
    suspend fun getOwnerDashboardMetrics(rangeDays: Int = 30) = api.getOwnerDashboardMetrics(rangeDays)
    suspend fun resolveSupportTicket(id: String) = api.resolveSupportTicket(id)
    suspend fun sendAdminBroadcast(title: String, body: String, audience: String = "ALL") =
        api.sendAdminBroadcast(mapOf("title" to title, "body" to body, "audience" to audience))
    suspend fun getFlaggedReviews() = api.getFlaggedReviews()
    suspend fun reportOrModerateReview(payload: Map<String, String>) = api.reportOrModerateReview(payload)
    suspend fun getSubscriptionPlans() = api.getSubscriptionPlans()
    suspend fun saveSubscriptionPlan(plan: SubscriptionPlanDto) = api.saveSubscriptionPlan(plan)
    suspend fun deleteSubscriptionPlan(id: String) = api.deleteSubscriptionPlan(id)

    companion object {
        @Volatile
        private var INSTANCE: SuqiRepository? = null

        fun getInstance(context: Context): SuqiRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SuqiRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
