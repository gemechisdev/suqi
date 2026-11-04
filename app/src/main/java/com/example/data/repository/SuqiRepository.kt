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
    private val _cartCount = MutableStateFlow(3)
    val cartCount: Flow<Int> = _cartCount.asStateFlow()

    suspend fun refreshCartCount() {
        val res = api.getCartItems()
        if (res.success && res.data != null) {
            _cartCount.value = res.data.sumOf { it.quantity }
        }
    }

    // User & Auth
    suspend fun getCurrentUser(): ApiResponse<UserDto> = api.getCurrentUser()
    suspend fun loginWithTelegram(name: String, phone: String) = api.loginWithTelegram(mapOf("name" to name, "phone" to phone))
    suspend fun loginWithGoogle(name: String, email: String) = api.loginWithGoogle(mapOf("name" to name, "email" to email))
    suspend fun loginAsGuest() = api.loginAsGuest()
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
    suspend fun getShops(filter: String = "ALL", category: String? = null, query: String? = null) =
        api.getShops(filter, category, query)
    suspend fun getShopDetails(id: String) = api.getShopDetails(id)
    suspend fun toggleFollowShop(id: String) = api.toggleFollowShop(id)
    suspend fun getCategories() = api.getCategories()

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
    suspend fun updateProductStatus(id: String, status: String) = api.updateProductStatus(id, status)
    suspend fun getSellerOrders() = api.getSellerOrders()
    suspend fun updateSellerOrderStatus(orderId: String, status: String) = api.updateSellerOrderStatus(orderId, status)
    suspend fun getSellerVerificationQueue() = api.getSellerVerificationQueue()
    suspend fun verifyPayment(orderId: String, payload: Map<String, String>) = api.verifyPayment(orderId, payload)
    suspend fun rejectPayment(orderId: String, payload: Map<String, String>) = api.rejectPayment(orderId, payload)
    suspend fun getShopPaymentMethods(shopId: String) = api.getShopPaymentMethods(shopId)
    suspend fun saveShopPaymentMethod(method: ShopPaymentMethodDto) = api.saveShopPaymentMethod(method)
    suspend fun getShopDeliveryOptions(shopId: String) = api.getShopDeliveryOptions(shopId)
    suspend fun getShopPromotions(shopId: String) = api.getShopPromotions(shopId)
    suspend fun createPromoCode(promo: PromoCodeDto) = api.createPromoCode(promo)
    suspend fun submitSellerVerification(details: Map<String, String>) = api.submitSellerVerification(details)
    suspend fun updateShopSettings(settings: Map<String, Any>) = api.updateShopSettings(settings)

    // Chat, Reviews, Notifications, Addresses
    suspend fun getChatConversations() = api.getChatConversations()
    suspend fun getChatMessages(shopId: String) = api.getChatMessages(shopId)
    suspend fun sendChatMessage(shopId: String, messageData: Map<String, Any>) = api.sendChatMessage(shopId, messageData)
    suspend fun getReviews(targetId: String) = api.getReviews(targetId)
    suspend fun submitReview(data: Map<String, Any>) = api.submitReview(data)
    suspend fun getNotifications() = api.getNotifications()
    suspend fun getSavedAddresses() = api.getSavedAddresses()
    suspend fun saveAddress(address: SavedAddressDto) = api.saveAddress(address)

    // Admin
    suspend fun getAdminSettings() = api.getAdminSettings()
    suspend fun updateFeatureToggle(feature: String, enabled: Boolean) = api.updateFeatureToggle(feature, enabled)
    suspend fun getAdminSellerVerificationQueue() = api.getAdminSellerVerificationQueue()
    suspend fun adminVerifySellerDecision(shopId: String, decision: String) = api.adminVerifySellerDecision(shopId, decision)

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
