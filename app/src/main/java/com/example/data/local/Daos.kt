package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getFirstUser(): UserEntity?

    @Query("SELECT * FROM users LIMIT 1")
    fun observeCurrentUser(): Flow<UserEntity?>

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

    @Query("UPDATE products SET isWishlisted = :isWishlisted WHERE id = :id")
    suspend fun updateWishlist(id: String, isWishlisted: Boolean)

    @Query("UPDATE products SET stockQuantity = stockQuantity - :qty WHERE id = :id AND stockQuantity >= :qty")
    suspend fun decrementStock(id: String, qty: Int): Int

    @Query("UPDATE products SET stockQuantity = stockQuantity + :qty WHERE id = :id")
    suspend fun incrementStock(id: String, qty: Int)
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
}

@Dao
interface PromoCodeDao {
    @Query("SELECT * FROM promo_codes WHERE shopId = :shopId AND isActive = 1")
    suspend fun getActivePromosForShop(shopId: String): List<PromoCodeEntity>

    @Query("SELECT * FROM promo_codes WHERE shopId = :shopId AND code = :code LIMIT 1")
    suspend fun getPromoByCode(shopId: String, code: String): PromoCodeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(promo: PromoCodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(promos: List<PromoCodeEntity>)
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
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    suspend fun getAllCategories(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)
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
