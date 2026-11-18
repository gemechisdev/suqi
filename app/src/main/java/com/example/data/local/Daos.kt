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

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteById(id: String)
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

    @Query("SELECT * FROM cart_items WHERE userId = :userId AND productId = :productId AND selectedVariation = :variation LIMIT 1")
    suspend fun findLine(userId: String, productId: String, variation: String): CartItemEntity?

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

    @Query("UPDATE orders SET customerUserId = 'deleted_user', customerName = 'Deleted user', customerPhone = '', deliveryAddress = 'Removed', payerIdentifier = '' WHERE customerUserId = :userId")
    suspend fun anonymizeByCustomerId(userId: String)
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

    @Query("SELECT * FROM promo_codes WHERE id = :id LIMIT 1")
    suspend fun getPromoById(id: String): PromoCodeEntity?

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

    @Query("SELECT * FROM reviews WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ReviewEntity?

    @Query("SELECT * FROM reviews WHERE userId = :userId ORDER BY date DESC")
    suspend fun getReviewsByUser(userId: String): List<ReviewEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(review: ReviewEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reviews: List<ReviewEntity>)

    @Update
    suspend fun update(review: ReviewEntity)

    @Query("UPDATE reviews SET userId = 'deleted_user', userName = 'Deleted user', userAvatar = '' WHERE userId = :userId")
    suspend fun anonymizeByUserId(userId: String)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages WHERE conversationId = :convId ORDER BY rowid ASC")
    suspend fun getMessages(convId: String): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages WHERE shopId = :shopId ORDER BY rowid ASC")
    suspend fun getMessagesForShop(shopId: String): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages WHERE shopId = :shopId ORDER BY rowid DESC")
    suspend fun getMessagesForShopDesc(shopId: String): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages WHERE customerUserId = :userId ORDER BY rowid DESC")
    suspend fun getMessagesForCustomerDesc(userId: String): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages ORDER BY rowid DESC")
    suspend fun getAllMessages(): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<ChatMessageEntity>)

    @Query("UPDATE chat_messages SET isRead = :isRead WHERE conversationId = :convId")
    suspend fun setConversationRead(convId: String, isRead: Boolean)

    @Query("DELETE FROM chat_messages WHERE customerUserId = :userId")
    suspend fun deleteByCustomerUserId(userId: String)
}

@Dao
interface WishlistDao {
    @Query("SELECT productId FROM wishlist_items WHERE userId = :userId")
    suspend fun getProductIdsForUser(userId: String): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM wishlist_items WHERE userId = :userId AND productId = :productId)")
    suspend fun isWishlisted(userId: String, productId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: WishlistItemEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<WishlistItemEntity>)

    @Query("DELETE FROM wishlist_items WHERE userId = :userId AND productId = :productId")
    suspend fun delete(userId: String, productId: String)

    @Query("DELETE FROM wishlist_items WHERE userId = :userId")
    suspend fun deleteByUserId(userId: String)

    @Query("DELETE FROM wishlist_items WHERE productId = :productId")
    suspend fun deleteByProductId(productId: String)
}

@Dao
interface ShopFollowDao {
    @Query("SELECT shopId FROM shop_follows WHERE userId = :userId")
    suspend fun getShopIdsForUser(userId: String): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM shop_follows WHERE userId = :userId AND shopId = :shopId)")
    suspend fun isFollowing(userId: String, shopId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: ShopFollowEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<ShopFollowEntity>)

    @Query("DELETE FROM shop_follows WHERE userId = :userId AND shopId = :shopId")
    suspend fun delete(userId: String, shopId: String)

    @Query("DELETE FROM shop_follows WHERE userId = :userId")
    suspend fun deleteByUserId(userId: String)
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

    @Query("DELETE FROM notifications WHERE userId = :userId")
    suspend fun deleteByUserId(userId: String)
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

    @Query("DELETE FROM saved_addresses WHERE userId = :userId")
    suspend fun deleteByUserId(userId: String)
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

    @Query("SELECT * FROM flagged_reviews WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): FlaggedReviewEntity?

    @Query("SELECT * FROM flagged_reviews WHERE reviewId = :reviewId")
    suspend fun getByReviewId(reviewId: String): List<FlaggedReviewEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: FlaggedReviewEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<FlaggedReviewEntity>)

    @Query("UPDATE flagged_reviews SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE flagged_reviews SET status = :status, moderatorNote = :note WHERE id = :id")
    suspend fun updateStatusWithNote(id: String, status: String, note: String)
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

    @Query("SELECT * FROM support_tickets WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SupportTicketEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ticket: SupportTicketEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tickets: List<SupportTicketEntity>)

    @Update
    suspend fun update(ticket: SupportTicketEntity)

    @Query("UPDATE support_tickets SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("DELETE FROM support_tickets WHERE userId = :userId")
    suspend fun deleteByUserId(userId: String)
}

@Dao
interface ItemTypeDao {
    @Query("SELECT * FROM item_types ORDER BY sortOrder ASC, label ASC")
    suspend fun getAllItemTypes(): List<ItemTypeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(itemType: ItemTypeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(itemTypes: List<ItemTypeEntity>)

    @Query("DELETE FROM item_types WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface ReturnRequestDao {
    @Query("SELECT * FROM return_requests ORDER BY createdAt DESC")
    suspend fun getAll(): List<ReturnRequestEntity>

    @Query("SELECT * FROM return_requests WHERE customerUserId = :userId ORDER BY createdAt DESC")
    suspend fun getByCustomer(userId: String): List<ReturnRequestEntity>

    @Query("SELECT * FROM return_requests WHERE shopId = :shopId ORDER BY createdAt DESC")
    suspend fun getByShop(shopId: String): List<ReturnRequestEntity>

    @Query("SELECT * FROM return_requests WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ReturnRequestEntity?

    @Query("SELECT * FROM return_requests WHERE orderId = :orderId LIMIT 1")
    suspend fun getByOrderId(orderId: String): ReturnRequestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ReturnRequestEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ReturnRequestEntity>)

    @Update
    suspend fun update(entity: ReturnRequestEntity)
}

@Dao
interface PayoutRequestDao {
    @Query("SELECT * FROM payout_requests ORDER BY createdAt DESC")
    suspend fun getAll(): List<PayoutRequestEntity>

    @Query("SELECT * FROM payout_requests WHERE shopId = :shopId ORDER BY createdAt DESC")
    suspend fun getByShop(shopId: String): List<PayoutRequestEntity>

    @Query("SELECT * FROM payout_requests WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PayoutRequestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PayoutRequestEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PayoutRequestEntity>)

    @Update
    suspend fun update(entity: PayoutRequestEntity)
}

@Dao
interface CommissionConfigDao {
    @Query("SELECT * FROM commission_configs ORDER BY CASE WHEN categoryId = 'DEFAULT' THEN 0 ELSE 1 END, categoryName ASC")
    suspend fun getAll(): List<CommissionConfigEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CommissionConfigEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CommissionConfigEntity>)

    @Query("DELETE FROM commission_configs WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface SubscriptionUpgradeDao {
    @Query("SELECT * FROM subscription_upgrades ORDER BY createdAt DESC")
    suspend fun getAll(): List<SubscriptionUpgradeEntity>

    @Query("SELECT * FROM subscription_upgrades WHERE shopId = :shopId ORDER BY createdAt DESC")
    suspend fun getByShop(shopId: String): List<SubscriptionUpgradeEntity>

    @Query("SELECT * FROM subscription_upgrades WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SubscriptionUpgradeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SubscriptionUpgradeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SubscriptionUpgradeEntity>)

    @Update
    suspend fun update(entity: SubscriptionUpgradeEntity)
}

@Dao
interface FlashSaleDao {
    @Query("SELECT * FROM flash_sales ORDER BY startAt DESC")
    suspend fun getAll(): List<FlashSaleEntity>

    @Query("SELECT * FROM flash_sales WHERE isActive = 1 ORDER BY startAt DESC")
    suspend fun getActive(): List<FlashSaleEntity>

    @Query("SELECT * FROM flash_sales WHERE shopId = :shopId ORDER BY startAt DESC")
    suspend fun getByShop(shopId: String): List<FlashSaleEntity>

    @Query("SELECT * FROM flash_sales WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): FlashSaleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FlashSaleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<FlashSaleEntity>)

    @Query("DELETE FROM flash_sales WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface SupportTicketMessageDao {
    @Query("SELECT * FROM support_ticket_messages WHERE ticketId = :ticketId ORDER BY rowid ASC")
    suspend fun getByTicket(ticketId: String): List<SupportTicketMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SupportTicketMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SupportTicketMessageEntity>)
}

@Dao
interface AdminBroadcastDao {
    @Query("SELECT * FROM admin_broadcasts ORDER BY rowid DESC")
    suspend fun getAll(): List<AdminBroadcastEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AdminBroadcastEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<AdminBroadcastEntity>)

    @Query("DELETE FROM admin_broadcasts WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface AnalyticsEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: AnalyticsEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<AnalyticsEventEntity>)

    @Query("SELECT COUNT(*) FROM analytics_events WHERE shopId = :shopId AND eventType = :eventType AND timestampMillis >= :sinceMillis")
    suspend fun countByShopTypeSince(shopId: String, eventType: String, sinceMillis: Long): Int

    @Query("SELECT COUNT(*) FROM analytics_events")
    suspend fun totalCount(): Int
}

// ==================== PHASE 6: ADMIN PLATFORM MANAGEMENT ====================

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_log ORDER BY rowid DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<AuditLogEntity>

    @Query("SELECT * FROM audit_log ORDER BY rowid DESC")
    suspend fun getAll(): List<AuditLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AuditLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<AuditLogEntity>)

    @Query("DELETE FROM audit_log")
    suspend fun clearAll()
}

@Dao
interface ShopAdminNoteDao {
    @Query("SELECT * FROM shop_admin_notes WHERE shopId = :shopId LIMIT 1")
    suspend fun getByShop(shopId: String): ShopAdminNoteEntity?

    @Query("SELECT * FROM shop_admin_notes")
    suspend fun getAll(): List<ShopAdminNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ShopAdminNoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ShopAdminNoteEntity>)
}

@Dao
interface UserAdminOverrideDao {
    @Query("SELECT * FROM user_admin_overrides WHERE userId = :userId LIMIT 1")
    suspend fun getByUser(userId: String): UserAdminOverrideEntity?

    @Query("SELECT * FROM user_admin_overrides")
    suspend fun getAll(): List<UserAdminOverrideEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserAdminOverrideEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<UserAdminOverrideEntity>)
}
