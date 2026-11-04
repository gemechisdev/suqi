package com.example.data.api

import com.example.data.model.*
import retrofit2.http.*

interface SuqiApiService {

    @POST(SuqiEndpoints.AUTH_TELEGRAM)
    suspend fun loginWithTelegram(@Body payload: Map<String, String>): ApiResponse<UserDto>

    @POST(SuqiEndpoints.AUTH_GOOGLE)
    suspend fun loginWithGoogle(@Body payload: Map<String, String>): ApiResponse<UserDto>

    @POST(SuqiEndpoints.AUTH_GUEST)
    suspend fun loginAsGuest(): ApiResponse<UserDto>

    @GET(SuqiEndpoints.AUTH_ME)
    suspend fun getCurrentUser(): ApiResponse<UserDto>

    @POST(SuqiEndpoints.AUTH_UPGRADE_SELLER)
    suspend fun upgradeToSeller(@Body payload: Map<String, String>): ApiResponse<ShopDto>

    // Marketplace
    @GET(SuqiEndpoints.PRODUCTS)
    suspend fun getProducts(
        @Query("query") query: String? = null,
        @Query("categoryId") categoryId: String? = null,
        @Query("subCategory") subCategory: String? = null,
        @Query("shopId") shopId: String? = null,
        @Query("inStockOnly") inStockOnly: Boolean = false,
        @Query("sortBy") sortBy: String = "POPULAR"
    ): ApiResponse<List<ProductDto>>

    @GET(SuqiEndpoints.PRODUCT_DETAILS)
    suspend fun getProductDetails(@Path("id") id: String): ApiResponse<ProductDto>

    @POST(SuqiEndpoints.PRODUCT_WISHLIST)
    suspend fun toggleWishlist(@Path("id") id: String): ApiResponse<Boolean>

    @GET(SuqiEndpoints.SHOPS)
    suspend fun getShops(
        @Query("filter") filter: String = "ALL", // "ALL", "VERIFIED", "TOP_RATED", "NEARBY"
        @Query("category") category: String? = null,
        @Query("query") query: String? = null
    ): ApiResponse<List<ShopDto>>

    @GET(SuqiEndpoints.SHOP_DETAILS)
    suspend fun getShopDetails(@Path("id") id: String): ApiResponse<ShopDto>

    @POST(SuqiEndpoints.SHOP_FOLLOW)
    suspend fun toggleFollowShop(@Path("id") id: String): ApiResponse<Boolean>

    @GET(SuqiEndpoints.CATEGORIES)
    suspend fun getCategories(): ApiResponse<List<CategoryDto>>

    // Cart
    @GET(SuqiEndpoints.CART)
    suspend fun getCartItems(): ApiResponse<List<CartItemDto>>

    @POST(SuqiEndpoints.CART)
    suspend fun addToCart(@Body item: Map<String, Any>): ApiResponse<CartItemDto>

    @PUT(SuqiEndpoints.CART_ITEM)
    suspend fun updateCartQuantity(@Path("itemId") itemId: Long, @Query("quantity") quantity: Int): ApiResponse<Boolean>

    @DELETE(SuqiEndpoints.CART_ITEM)
    suspend fun removeCartItem(@Path("itemId") itemId: Long): ApiResponse<Boolean>

    @DELETE(SuqiEndpoints.CART_CLEAR)
    suspend fun clearCart(): ApiResponse<Boolean>

    // Checkout & Orders
    @POST(SuqiEndpoints.ORDERS)
    suspend fun submitOrder(@Body orderData: Map<String, Any>): ApiResponse<OrderDto>

    @GET(SuqiEndpoints.ORDERS)
    suspend fun getOrders(): ApiResponse<List<OrderDto>>

    @GET(SuqiEndpoints.ORDER_DETAILS)
    suspend fun getOrderDetails(@Path("orderId") orderId: String): ApiResponse<OrderDto>

    @POST(SuqiEndpoints.ORDER_RESUBMIT_PAYMENT)
    suspend fun resubmitPayment(
        @Path("orderId") orderId: String,
        @Body payload: Map<String, String>
    ): ApiResponse<OrderDto>

    @POST(SuqiEndpoints.ORDER_CANCEL)
    suspend fun cancelOrder(@Path("orderId") orderId: String): ApiResponse<Boolean>

    // Seller Studio
    @GET(SuqiEndpoints.SELLER_DASHBOARD)
    suspend fun getSellerDashboard(): ApiResponse<Map<String, Any>>

    @GET(SuqiEndpoints.SELLER_PRODUCTS)
    suspend fun getSellerProducts(): ApiResponse<List<ProductDto>>

    @POST(SuqiEndpoints.SELLER_PRODUCTS)
    suspend fun createProduct(@Body productData: Map<String, Any>): ApiResponse<ProductDto>

    @PUT(SuqiEndpoints.SELLER_PRODUCTS)
    suspend fun updateProductStatus(@Query("id") id: String, @Query("status") status: String): ApiResponse<Boolean>

    @GET(SuqiEndpoints.SELLER_ORDERS)
    suspend fun getSellerOrders(): ApiResponse<List<OrderDto>>

    @PUT(SuqiEndpoints.SELLER_ORDER_STATUS)
    suspend fun updateSellerOrderStatus(@Path("orderId") orderId: String, @Query("status") status: String): ApiResponse<Boolean>

    @GET(SuqiEndpoints.SELLER_VERIFICATION_QUEUE)
    suspend fun getSellerVerificationQueue(): ApiResponse<List<OrderDto>>

    @POST(SuqiEndpoints.SELLER_VERIFY_PAYMENT)
    suspend fun verifyPayment(
        @Path("orderId") orderId: String,
        @Body payload: Map<String, String>
    ): ApiResponse<Boolean>

    @POST(SuqiEndpoints.SELLER_REJECT_PAYMENT)
    suspend fun rejectPayment(
        @Path("orderId") orderId: String,
        @Body payload: Map<String, String>
    ): ApiResponse<Boolean>

    @GET(SuqiEndpoints.SELLER_PAYMENT_METHODS)
    suspend fun getShopPaymentMethods(@Query("shopId") shopId: String): ApiResponse<List<ShopPaymentMethodDto>>

    @POST(SuqiEndpoints.SELLER_PAYMENT_METHODS)
    suspend fun saveShopPaymentMethod(@Body method: ShopPaymentMethodDto): ApiResponse<Boolean>

    @GET(SuqiEndpoints.SELLER_DELIVERY_OPTIONS)
    suspend fun getShopDeliveryOptions(@Query("shopId") shopId: String): ApiResponse<List<ShopDeliveryOptionDto>>

    @GET(SuqiEndpoints.SELLER_PROMOTIONS)
    suspend fun getShopPromotions(@Query("shopId") shopId: String): ApiResponse<List<PromoCodeDto>>

    @POST(SuqiEndpoints.SELLER_PROMOTIONS)
    suspend fun createPromoCode(@Body promo: PromoCodeDto): ApiResponse<Boolean>

    @POST(SuqiEndpoints.SELLER_VERIFICATION_SUBMIT)
    suspend fun submitSellerVerification(@Body details: Map<String, String>): ApiResponse<Boolean>

    @PUT(SuqiEndpoints.SELLER_SHOP_SETTINGS)
    suspend fun updateShopSettings(@Body settings: Map<String, Any>): ApiResponse<Boolean>

    // Chat, Reviews, Notifications, Addresses
    @GET(SuqiEndpoints.CHAT_CONVERSATIONS)
    suspend fun getChatConversations(): ApiResponse<List<ChatMessageDto>>

    @GET(SuqiEndpoints.CHAT_MESSAGES)
    suspend fun getChatMessages(@Path("shopId") shopId: String): ApiResponse<List<ChatMessageDto>>

    @POST(SuqiEndpoints.CHAT_MESSAGES)
    suspend fun sendChatMessage(@Path("shopId") shopId: String, @Body messageData: Map<String, Any>): ApiResponse<ChatMessageDto>

    @GET(SuqiEndpoints.REVIEWS)
    suspend fun getReviews(@Query("targetId") targetId: String): ApiResponse<List<ReviewDto>>

    @POST(SuqiEndpoints.REVIEWS)
    suspend fun submitReview(@Body reviewData: Map<String, Any>): ApiResponse<ReviewDto>

    @GET(SuqiEndpoints.NOTIFICATIONS)
    suspend fun getNotifications(): ApiResponse<List<NotificationDto>>

    @GET(SuqiEndpoints.ADDRESSES)
    suspend fun getSavedAddresses(): ApiResponse<List<SavedAddressDto>>

    @POST(SuqiEndpoints.ADDRESSES)
    suspend fun saveAddress(@Body address: SavedAddressDto): ApiResponse<SavedAddressDto>

    // Admin
    @GET(SuqiEndpoints.ADMIN_SETTINGS)
    suspend fun getAdminSettings(): ApiResponse<AdminSettingsDto>

    @PUT(SuqiEndpoints.ADMIN_FEATURE_TOGGLES)
    suspend fun updateFeatureToggle(@Query("feature") feature: String, @Query("enabled") enabled: Boolean): ApiResponse<Boolean>

    @GET(SuqiEndpoints.ADMIN_SELLER_QUEUE)
    suspend fun getAdminSellerVerificationQueue(): ApiResponse<List<ShopDto>>

    @POST(SuqiEndpoints.ADMIN_VERIFY_SELLER)
    suspend fun adminVerifySellerDecision(
        @Path("shopId") shopId: String,
        @Query("decision") decision: String
    ): ApiResponse<Boolean>
}
