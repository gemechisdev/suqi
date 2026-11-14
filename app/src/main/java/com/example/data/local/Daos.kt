package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE isActiveSession = 1 LIMIT 1")
    suspend fun getActiveSessionUser(): UserEntity?

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getFirstUser(): UserEntity?

    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<UserEntity>

    @Query("SELECT * FROM users WHERE isActiveSession = 1 LIMIT 1")
    fun observeCurrentUser(): Flow<UserEntity?>

    @Query("UPDATE users SET isActiveSession = 0")
    suspend fun clearActiveSessions()

    @Query("UPDATE users SET isActiveSession = 1 WHERE id = :userId")
    suspend fun setActiveSession(userId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)
}

@Dao
interface ShopDao {
    @Query("SELECT * FROM shops")
    suspend fun getAllShops(): List<ShopEntity>

    @Query("SELECT * FROM shops")
    fun observeAllShops(): Flow<List<ShopEntity>>

    @Query("SELECT * FROM shops WHERE id = :id LIMIT 1")
    suspend fun getShopById(id: String): ShopEntity?

    @Query("SELECT * FROM shops WHERE ownerUserId = :userId LIMIT 1")
    suspend fun getShopByOwner(userId: String): ShopEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShop(shop: ShopEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(shops: List<ShopEntity>)

    @Update
    suspend fun updateShop(shop: ShopEntity)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products")
    suspend fun getAllProducts(): List<ProductEntity>

    @Query("SELECT * FROM products")
    fun observeAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE shopId = :shopId")
    suspend fun getProductsByShop(shopId: String): List<ProductEntity>

    @Query("SELECT * FROM products WHERE categoryId = :categoryId")
    suspend fun getProductsByCategory(categoryId: String): List<ProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("UPDATE products SET detailsJson = :json WHERE id = :id")
    suspend fun updateDetailsJson(id: String, json: String)

    @Query("UPDATE products SET isWishlisted = :isWishlisted WHERE id = :id")
    suspend fun updateWishlist(id: String, isWishlisted: Boolean)

    @Query("UPDATE products SET stockQuantity = stockQuantity - :qty WHERE id = :id AND stockQuantity >= :qty")
    suspend fun decrementStock(id: String, qty: Int): Int

    @Query("UPDATE products SET stockQuantity = stockQuantity + :qty WHERE id = :id")
    suspend fun incrementStock(id: String, qty: Int)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_items WHERE userId = :userId")
    suspend fun getCartItems(userId: String): List<CartItemEntity>

    @Query("SELECT * FROM cart_items WHERE userId = :userId")
    fun observeCartItems(userId: String): Flow<List<CartItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(item: CartItemEntity): Long

    @Query("UPDATE cart_items SET quantity = :quantity WHERE id = :itemId")
    suspend fun updateQuantity(itemId: Long, quantity: Int)

    @Query("DELETE FROM cart_items WHERE id = :itemId")
    suspend fun deleteItem(itemId: Long)

    @Query("DELETE FROM cart_items WHERE userId = :userId AND shopId = :shopId")
    suspend fun clearShopItems(userId: String, shopId: String)

    @Query("DELETE FROM cart_items WHERE userId = :userId")
    suspend fun clearAll(userId: String)
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    suspend fun getAllOrders(): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE customerUserId = :userId ORDER BY createdAt DESC")
    suspend fun getCustomerOrders(userId: String): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE customerUserId = :userId ORDER BY createdAt DESC")
    fun observeCustomerOrders(userId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE shopId = :shopId ORDER BY createdAt DESC")
    suspend fun getShopOrders(shopId: String): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE shopId = :shopId ORDER BY createdAt DESC")
    fun observeShopOrders(shopId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    suspend fun getOrderById(id: String): OrderEntity?

    @Query("SELECT * FROM orders WHERE shopId = :shopId AND paymentStatus = 'PENDING_VERIFICATION' ORDER BY createdAt DESC")
    suspend fun getPendingVerificationOrders(shopId: String): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE transactionReference = :reference AND paymentStatus != 'REJECTED' LIMIT 1")
    suspend fun getOrderByTransactionReference(reference: String): OrderEntity?

    @Query("SELECT * FROM orders WHERE customerUserId = :userId AND orderStatus IN ('Confirmed', 'Dispatched', 'Delivered')")
    suspend fun getEligibleOrdersForCustomer(userId: String): List<OrderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(orders: List<OrderEntity>)

    @Update
    suspend fun updateOrder(order: OrderEntity)
}

@Dao
interface ShopPaymentMethodDao {
    @Query("SELECT * FROM shop_payment_methods WHERE shopId = :shopId")
    suspend fun getMethodsForShop(shopId: String): List<ShopPaymentMethodEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(method: ShopPaymentMethodEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(methods: List<ShopPaymentMethodEntity>)

    @Query("DELETE FROM shop_payment_methods WHERE id = :id")
    suspend fun deleteById(id: String)

    @Delete
    suspend fun delete(method: ShopPaymentMethodEntity)
}

@Dao
interface ShopDeliveryOptionDao {
    @Query("SELECT * FROM shop_delivery_options WHERE shopId = :shopId")
    suspend fun getOptionsForShop(shopId: String): List<ShopDeliveryOptionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(option: ShopDeliveryOptionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(options: List<ShopDeliveryOptionEntity>)

    @Query("DELETE FROM shop_delivery_options WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface PromoCodeDao {
    @Query("SELECT * FROM promo_codes WHERE shopId = :shopId AND isActive = 1")
    suspend fun getActivePromosForShop(shopId: String): List<PromoCodeEntity>

    @Query("SELECT * FROM promo_codes WHERE shopId = :shopId")
    suspend fun getAllPromosForShop(shopId: String): List<PromoCodeEntity>

    @Query("SELECT * FROM promo_codes WHERE shopId = :shopId AND code = :code LIMIT 1")
    suspend fun getPromoByCode(shopId: String, code: String): PromoCodeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(promo: PromoCodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(promos: List<PromoCodeEntity>)

    @Query("DELETE FROM promo_codes WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface ReviewDao {
    @Query("SELECT * FROM reviews WHERE targetId = :targetId ORDER BY date DESC")
    suspend fun getReviewsForTarget(targetId: String): List<ReviewEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(review: ReviewEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reviews: List<ReviewEntity>)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages WHERE conversationId = :convId ORDER BY timestamp ASC")
    suspend fun getMessages(convId: String): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages WHERE shopId = :shopId ORDER BY timestamp ASC")
    suspend fun getMessagesForShop(shopId: String): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC")
    suspend fun getAllMessages(): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<ChatMessageEntity>)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getNotifications(userId: String): List<NotificationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET isRead = :isRead WHERE id = :id")
    suspend fun setRead(id: String, isRead: Boolean)

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllRead(userId: String)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface SavedAddressDao {
    @Query("SELECT * FROM saved_addresses WHERE userId = :userId")
    suspend fun getAddresses(userId: String): List<SavedAddressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(address: SavedAddressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(addresses: List<SavedAddressEntity>)

    @Update
    suspend fun update(address: SavedAddressEntity)

    @Query("UPDATE saved_addresses SET isDefault = 0 WHERE userId = :userId")
    suspend fun clearDefaultForUser(userId: String)

    @Query("DELETE FROM saved_addresses WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name ASC")
    suspend fun getAllCategories(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface AdminSettingsDao {
    @Query("SELECT * FROM admin_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): AdminSettingsEntity?

    @Query("SELECT * FROM admin_settings WHERE id = 1 LIMIT 1")
    fun observeSettings(): Flow<AdminSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settings: AdminSettingsEntity)

    @Update
    suspend fun update(settings: AdminSettingsEntity)
}

@Dao
interface PromoBannerDao {
    @Query("SELECT * FROM promo_banners ORDER BY sortOrder ASC")
    suspend fun getAllBanners(): List<PromoBannerEntity>

    @Query("SELECT * FROM promo_banners WHERE isActive = 1 ORDER BY sortOrder ASC")
    suspend fun getActiveBanners(): List<PromoBannerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(banner: PromoBannerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(banners: List<PromoBannerEntity>)

    @Query("DELETE FROM promo_banners WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface FlaggedReviewDao {
    @Query("SELECT * FROM flagged_reviews ORDER BY createdAt DESC")
    suspend fun getAllFlagged(): List<FlaggedReviewEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: FlaggedReviewEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<FlaggedReviewEntity>)

    @Query("UPDATE flagged_reviews SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)
}

@Dao
interface ProductQuestionDao {
    @Query("SELECT * FROM product_questions WHERE productId = :productId ORDER BY createdAt DESC")
    suspend fun getQuestionsForProduct(productId: String): List<ProductQuestionEntity>

    @Query("SELECT * FROM product_questions WHERE shopId = :shopId ORDER BY createdAt DESC")
    suspend fun getQuestionsForShop(shopId: String): List<ProductQuestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(question: ProductQuestionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<ProductQuestionEntity>)
}

@Dao
interface SubscriptionPlanDao {
    @Query("SELECT * FROM subscription_plans ORDER BY monthlyPrice ASC")
    suspend fun getAllPlans(): List<SubscriptionPlanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: SubscriptionPlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(plans: List<SubscriptionPlanEntity>)

    @Query("DELETE FROM subscription_plans WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface SupportTicketDao {
    @Query("SELECT * FROM support_tickets WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getTicketsForUser(userId: String): List<SupportTicketEntity>

    @Query("SELECT * FROM support_tickets ORDER BY createdAt DESC")
    suspend fun getAllTickets(): List<SupportTicketEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ticket: SupportTicketEntity)
}
