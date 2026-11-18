package com.example.data.api

import com.example.data.model.*
import retrofit2.http.*

interface SuqiApiService {

    @POST(SuqiEndpoints.AUTH_TELEGRAM)
    suspend fun loginWithTelegram(@Body payload: Map<String, String>): ApiResponse<UserDto>

    @POST(SuqiEndpoints.AUTH_GOOGLE)
    suspend fun loginWithGoogle(@Body payload: Map<String, String>): ApiResponse<UserDto>

    @POST(SuqiEndpoints.AUTH_PHONE)
    suspend fun loginWithPhoneOtp(@Body payload: Map<String, String>): ApiResponse<UserDto>

    @POST(SuqiEndpoints.AUTH_GUEST)
    suspend fun loginAsGuest(): ApiResponse<UserDto>

    @POST(SuqiEndpoints.AUTH_ADMIN)
    suspend fun adminSignIn(@Body payload: Map<String, String>): ApiResponse<UserDto>

    @PUT(SuqiEndpoints.AUTH_PROFILE)
    suspend fun updateUserProfile(@Body payload: Map<String, String>): ApiResponse<UserDto>

    @DELETE(SuqiEndpoints.AUTH_DELETE_ACCOUNT)
    suspend fun deleteAccount(): ApiResponse<Boolean>

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

    @GET(SuqiEndpoints.PRODUCT_QUESTIONS)
    suspend fun getProductQuestions(@Path("id") productId: String): ApiResponse<List<ProductQuestionDto>>

    @POST(SuqiEndpoints.PRODUCT_QUESTIONS)
    suspend fun submitProductQuestion(@Path("id") productId: String, @Body payload: Map<String, String>): ApiResponse<ProductQuestionDto>

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

    @GET(SuqiEndpoints.BANNERS)
    suspend fun getPromoBanners(@Query("activeOnly") activeOnly: Boolean = true): ApiResponse<List<PromoBannerDto>>

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

    @PUT(SuqiEndpoints.SELLER_PRODUCT_UPDATE)
    suspend fun updateProduct(@Query("id") id: String, @Body productData: Map<String, Any>): ApiResponse<ProductDto>

    @PUT(SuqiEndpoints.SELLER_PRODUCTS)
    suspend fun updateProductStatus(@Query("id") id: String, @Query("status") status: String): ApiResponse<Boolean>

    @PUT(SuqiEndpoints.SELLER_PRODUCT_STOCK)
    suspend fun updateProductStock(@Path("id") id: String, @Body payload: Map<String, Any>): ApiResponse<ProductDto>

    @POST(SuqiEndpoints.SELLER_PRODUCTS_BULK)
    suspend fun bulkUpdateProducts(@Body payload: Map<String, Any>): ApiResponse<Int>

    @POST(SuqiEndpoints.SELLER_PRODUCT_DUPLICATE)
    suspend fun duplicateProduct(@Path("id") id: String): ApiResponse<ProductDto>

    @DELETE(SuqiEndpoints.SELLER_PRODUCTS)
    suspend fun deleteProduct(@Query("id") id: String): ApiResponse<Boolean>

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

    @DELETE(SuqiEndpoints.SELLER_PAYMENT_METHODS)
    suspend fun deleteShopPaymentMethod(@Query("id") id: String): ApiResponse<Boolean>

    @GET(SuqiEndpoints.SELLER_DELIVERY_OPTIONS)
    suspend fun getShopDeliveryOptions(@Query("shopId") shopId: String): ApiResponse<List<ShopDeliveryOptionDto>>

    @POST(SuqiEndpoints.SELLER_DELIVERY_OPTIONS)
    suspend fun saveShopDeliveryOption(@Body option: ShopDeliveryOptionDto): ApiResponse<Boolean>

    @DELETE(SuqiEndpoints.SELLER_DELIVERY_OPTIONS)
    suspend fun deleteShopDeliveryOption(@Query("id") id: String): ApiResponse<Boolean>

    @GET(SuqiEndpoints.SELLER_PROMOTIONS)
    suspend fun getShopPromotions(@Query("shopId") shopId: String, @Query("includeInactive") includeInactive: Boolean = false): ApiResponse<List<PromoCodeDto>>

    @POST(SuqiEndpoints.SELLER_PROMOTIONS)
    suspend fun createPromoCode(@Body promo: PromoCodeDto): ApiResponse<Boolean>

    @DELETE(SuqiEndpoints.SELLER_PROMOTIONS)
    suspend fun deletePromoCode(@Query("id") id: String): ApiResponse<Boolean>

    @POST(SuqiEndpoints.SELLER_VERIFICATION_SUBMIT)
    suspend fun submitSellerVerification(@Body details: Map<String, String>): ApiResponse<Boolean>

    @PUT(SuqiEndpoints.SELLER_SHOP_SETTINGS)
    suspend fun updateShopSettings(@Body settings: Map<String, Any>): ApiResponse<Boolean>

    // Chat, Reviews, Notifications, Addresses, Support
    @GET(SuqiEndpoints.CHAT_CONVERSATIONS)
    suspend fun getChatConversations(): ApiResponse<List<ChatMessageDto>>

    @GET(SuqiEndpoints.CHAT_MESSAGES)
    suspend fun getChatMessages(
        @Path("shopId") shopId: String,
        @Query("conversationId") conversationId: String? = null
    ): ApiResponse<List<ChatMessageDto>>

    @POST(SuqiEndpoints.CHAT_MESSAGES)
    suspend fun sendChatMessage(@Path("shopId") shopId: String, @Body messageData: Map<String, Any>): ApiResponse<ChatMessageDto>

    @PUT(SuqiEndpoints.CHAT_MARK_READ)
    suspend fun markChatConversationRead(
        @Path("shopId") shopId: String,
        @Query("isRead") isRead: Boolean,
        @Query("conversationId") conversationId: String? = null
    ): ApiResponse<Boolean>

    @GET(SuqiEndpoints.REVIEWS)
    suspend fun getReviews(@Query("targetId") targetId: String): ApiResponse<List<ReviewDto>>

    @POST(SuqiEndpoints.REVIEWS)
    suspend fun submitReview(@Body reviewData: Map<String, Any>): ApiResponse<ReviewDto>

    @GET(SuqiEndpoints.NOTIFICATIONS)
    suspend fun getNotifications(): ApiResponse<List<NotificationDto>>

    @PUT(SuqiEndpoints.NOTIFICATIONS)
    suspend fun markNotificationRead(@Query("id") id: String, @Query("isRead") isRead: Boolean): ApiResponse<Boolean>

    @POST(SuqiEndpoints.NOTIFICATIONS)
    suspend fun markAllNotificationsRead(): ApiResponse<Boolean>

    @DELETE(SuqiEndpoints.NOTIFICATIONS)
    suspend fun deleteNotification(@Query("id") id: String): ApiResponse<Boolean>

    @GET(SuqiEndpoints.ADDRESSES)
    suspend fun getSavedAddresses(): ApiResponse<List<SavedAddressDto>>

    @POST(SuqiEndpoints.ADDRESSES)
    suspend fun saveAddress(@Body address: SavedAddressDto): ApiResponse<SavedAddressDto>

    @DELETE(SuqiEndpoints.ADDRESSES)
    suspend fun deleteSavedAddress(@Query("id") id: String): ApiResponse<Boolean>

    @POST(SuqiEndpoints.SUPPORT_TICKETS)
    suspend fun submitSupportTicket(@Body ticket: SupportTicketDto): ApiResponse<SupportTicketDto>

    @GET(SuqiEndpoints.SUPPORT_TICKETS)
    suspend fun getSupportTickets(): ApiResponse<List<SupportTicketDto>>

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
        @Query("decision") decision: String,
        @Query("reason") reason: String? = null,
        @Query("failedKeys") failedKeys: String? = null
    ): ApiResponse<Boolean>

    @POST(SuqiEndpoints.ADMIN_CATEGORIES)
    suspend fun saveCategory(@Body category: CategoryDto): ApiResponse<CategoryDto>

    @PUT(SuqiEndpoints.ADMIN_CATEGORIES_REORDER)
    suspend fun reorderCategories(@Body orderedIds: List<String>): ApiResponse<Boolean>

    @DELETE(SuqiEndpoints.ADMIN_CATEGORIES)
    suspend fun deleteCategory(@Query("id") id: String): ApiResponse<Boolean>

    @POST(SuqiEndpoints.BANNERS)
    suspend fun savePromoBanner(@Body banner: PromoBannerDto): ApiResponse<PromoBannerDto>

    @PUT(SuqiEndpoints.BANNERS_REORDER)
    suspend fun reorderPromoBanners(@Body orderedIds: List<String>): ApiResponse<Boolean>

    @DELETE(SuqiEndpoints.BANNERS)
    suspend fun deletePromoBanner(@Query("id") id: String): ApiResponse<Boolean>

    @PUT(SuqiEndpoints.ADMIN_HOME_SECTIONS)
    suspend fun updateHomeSectionsConfig(@Body payload: Map<String, List<String>>): ApiResponse<AdminSettingsDto>

    @GET(SuqiEndpoints.ADMIN_ITEM_TYPES)
    suspend fun getItemTypes(): ApiResponse<List<ItemTypeDto>>

    @POST(SuqiEndpoints.ADMIN_ITEM_TYPES)
    suspend fun saveItemType(@Body itemType: ItemTypeDto): ApiResponse<ItemTypeDto>

    @DELETE(SuqiEndpoints.ADMIN_ITEM_TYPES)
    suspend fun deleteItemType(@Query("id") id: String): ApiResponse<Boolean>

    @GET(SuqiEndpoints.ADMIN_OWNER_METRICS)
    suspend fun getOwnerDashboardMetrics(@Query("rangeDays") rangeDays: Int = 30): ApiResponse<OwnerDashboardMetricsDto>

    @POST(SuqiEndpoints.SUPPORT_TICKET_RESOLVE)
    suspend fun resolveSupportTicket(@Path("id") id: String): ApiResponse<Boolean>

    @POST(SuqiEndpoints.ADMIN_BROADCAST)
    suspend fun sendAdminBroadcast(@Body payload: Map<String, String>): ApiResponse<Int>

    @GET(SuqiEndpoints.ADMIN_MODERATION)
    suspend fun getFlaggedReviews(): ApiResponse<List<FlaggedReviewDto>>

    @POST(SuqiEndpoints.ADMIN_MODERATION)
    suspend fun reportOrModerateReview(@Body payload: Map<String, String>): ApiResponse<Boolean>

    @GET(SuqiEndpoints.SUBSCRIPTION_PLANS)
    suspend fun getSubscriptionPlans(): ApiResponse<List<SubscriptionPlanDto>>

    @POST(SuqiEndpoints.SUBSCRIPTION_PLANS)
    suspend fun saveSubscriptionPlan(@Body plan: SubscriptionPlanDto): ApiResponse<SubscriptionPlanDto>

    @DELETE(SuqiEndpoints.SUBSCRIPTION_PLANS)
    suspend fun deleteSubscriptionPlan(@Query("id") id: String): ApiResponse<Boolean>
}
