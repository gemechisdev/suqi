package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import com.example.data.model.MarketplaceCategories
import com.example.data.model.ProductTemplates
import com.example.data.model.attributesToJson
import com.example.data.model.parseAttributesJson
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Database(
    entities = [
        UserEntity::class,
        ShopEntity::class,
        ProductEntity::class,
        CartItemEntity::class,
        OrderEntity::class,
        ShopPaymentMethodEntity::class,
        ShopDeliveryOptionEntity::class,
        PromoCodeEntity::class,
        ReviewEntity::class,
        ChatMessageEntity::class,
        NotificationEntity::class,
        SavedAddressEntity::class,
        CategoryEntity::class,
        AdminSettingsEntity::class,
        PromoBannerEntity::class,
        FlaggedReviewEntity::class,
        ProductQuestionEntity::class,
        SubscriptionPlanEntity::class,
        SupportTicketEntity::class,
        ItemTypeEntity::class,
        WishlistItemEntity::class,
        ShopFollowEntity::class,
        ReturnRequestEntity::class,
        PayoutRequestEntity::class,
        CommissionConfigEntity::class,
        SubscriptionUpgradeEntity::class,
        FlashSaleEntity::class,
        SupportTicketMessageEntity::class,
        AdminBroadcastEntity::class,
        AnalyticsEventEntity::class
    ],
    version = 11,
    exportSchema = false
)
abstract class SuqiDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun shopDao(): ShopDao
    abstract fun productDao(): ProductDao
    abstract fun cartDao(): CartDao
    abstract fun orderDao(): OrderDao
    abstract fun shopPaymentMethodDao(): ShopPaymentMethodDao
    abstract fun shopDeliveryOptionDao(): ShopDeliveryOptionDao
    abstract fun promoCodeDao(): PromoCodeDao
    abstract fun reviewDao(): ReviewDao
    abstract fun chatDao(): ChatDao
    abstract fun notificationDao(): NotificationDao
    abstract fun savedAddressDao(): SavedAddressDao
    abstract fun categoryDao(): CategoryDao
    abstract fun adminSettingsDao(): AdminSettingsDao
    abstract fun promoBannerDao(): PromoBannerDao
    abstract fun flaggedReviewDao(): FlaggedReviewDao
    abstract fun productQuestionDao(): ProductQuestionDao
    abstract fun subscriptionPlanDao(): SubscriptionPlanDao
    abstract fun supportTicketDao(): SupportTicketDao
    abstract fun itemTypeDao(): ItemTypeDao
    abstract fun wishlistDao(): WishlistDao
    abstract fun shopFollowDao(): ShopFollowDao
    abstract fun returnRequestDao(): ReturnRequestDao
    abstract fun payoutRequestDao(): PayoutRequestDao
    abstract fun commissionConfigDao(): CommissionConfigDao
    abstract fun subscriptionUpgradeDao(): SubscriptionUpgradeDao
    abstract fun flashSaleDao(): FlashSaleDao
    abstract fun supportTicketMessageDao(): SupportTicketMessageDao
    abstract fun adminBroadcastDao(): AdminBroadcastDao
    abstract fun analyticsEventDao(): AnalyticsEventDao

    private val seedMutex = Mutex()
    @Volatile
    private var isSeedVerified = false

    suspend fun ensureSeeded() {
        if (isSeedVerified) return
        seedMutex.withLock {
            if (isSeedVerified) return
            if (productDao().getAllProducts().isEmpty()) {
                SeedData.seedDatabase(this)
            } else {
                SeedData.ensureSecondaryTablesSeeded(this)
            }
            val persistedTypes = itemTypeDao().getAllItemTypes()
            if (persistedTypes.isNotEmpty()) {
                ProductTemplates.syncFromPersisted(
                    persistedTypes.map {
                        com.example.data.model.ItemType(
                            id = it.id,
                            label = it.label,
                            emoji = it.emoji,
                            categoryId = it.categoryId,
                            group = it.groupName,
                            attributes = parseAttributesJson(it.attributesJson)
                        )
                    }
                )
            }
            isSeedVerified = true
        }
    }

    companion object {
        /** v2: products.detailsJson (media, dynamic attributes, variants) + new item-type categories. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN detailsJson TEXT NOT NULL DEFAULT ''")
                ProductTemplates.extraCategories.forEach { c ->
                    db.execSQL(
                        "INSERT OR IGNORE INTO categories (id, name, nameAmharic, nameAfaanOromo, icon, subCategoriesCsv) VALUES (?, ?, ?, ?, ?, ?)",
                        arrayOf<Any?>(c[0], c[1], c[2], c[3], c[4], c[5])
                    )
                }
            }
        }

        /** v3: roles/sessions, order receipts, shop delivery/receipt settings, banners, moderation, Q&A, plans, tickets. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN adminTier TEXT NOT NULL DEFAULT 'NONE'")
                db.execSQL("ALTER TABLE users ADD COLUMN gender TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE users ADD COLUMN birthday TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE users ADD COLUMN activeRole TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE users ADD COLUMN isActiveSession INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE users SET isActiveSession = 1, activeRole = role WHERE id = (SELECT id FROM users LIMIT 1)")

                db.execSQL("ALTER TABLE shops ADD COLUMN requireReceiptScreenshot INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE shops ADD COLUMN businessHoursJson TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE shops ADD COLUMN vacationReturnDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE shops ADD COLUMN vacationNote TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE shops ADD COLUMN freeDeliveryThreshold REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE shops ADD COLUMN coverageSubCitiesCsv TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE shops ADD COLUMN verificationDocsJson TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE shops ADD COLUMN verificationRejectionReason TEXT")

                db.execSQL("ALTER TABLE orders ADD COLUMN receiptImageUri TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE orders ADD COLUMN payerIdentifier TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE orders ADD COLUMN sellerNotes TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE orders ADD COLUMN courierName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE orders ADD COLUMN courierPhone TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE orders ADD COLUMN dispatchedAt TEXT")

                db.execSQL("ALTER TABLE chat_messages ADD COLUMN imageUrl TEXT NOT NULL DEFAULT ''")

                db.execSQL("ALTER TABLE categories ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE categories ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")

                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `promo_banners` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `badge` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `highlight` TEXT NOT NULL,
                        `subtitle` TEXT NOT NULL,
                        `buttonText` TEXT NOT NULL,
                        `imageUrl` TEXT NOT NULL,
                        `targetCategory` TEXT NOT NULL,
                        `targetName` TEXT NOT NULL,
                        `isActive` INTEGER NOT NULL DEFAULT 1,
                        `sortOrder` INTEGER NOT NULL DEFAULT 0
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `flagged_reviews` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `authorName` TEXT NOT NULL,
                        `reason` TEXT NOT NULL,
                        `reviewText` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `createdAt` TEXT NOT NULL
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `product_questions` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `productId` TEXT NOT NULL,
                        `shopId` TEXT NOT NULL,
                        `askerName` TEXT NOT NULL,
                        `question` TEXT NOT NULL,
                        `answer` TEXT NOT NULL,
                        `answeredByShop` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` TEXT NOT NULL
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `subscription_plans` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `monthlyPrice` REAL NOT NULL,
                        `yearlyPrice` REAL NOT NULL,
                        `maxProducts` INTEGER NOT NULL,
                        `featuresCsv` TEXT NOT NULL,
                        `isPopular` INTEGER NOT NULL DEFAULT 0,
                        `isActive` INTEGER NOT NULL DEFAULT 1
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `support_tickets` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `userId` TEXT NOT NULL,
                        `orderId` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `subject` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `createdAt` TEXT NOT NULL
                    )"""
                )
            }
        }

        /** v4: category CDN imageUrl + canonical 16 marketplace categories. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN imageUrl TEXT NOT NULL DEFAULT ''")
                db.execSQL("DELETE FROM categories WHERE id = 'cat_toys'")
                MarketplaceCategories.all.forEach { c ->
                    db.execSQL(
                        """INSERT OR REPLACE INTO categories
                            (id, name, nameAmharic, nameAfaanOromo, icon, imageUrl, subCategoriesCsv, isActive, sortOrder)
                            VALUES (?, ?, ?, ?, ?, ?, ?, 1, ?)""",
                        arrayOf<Any?>(
                            c.id,
                            c.name,
                            c.nameAmharic,
                            c.nameAfaanOromo,
                            c.icon,
                            c.imageUrl,
                            c.subCategories.joinToString(","),
                            c.sortOrder
                        )
                    )
                }
            }
        }

        /** v5: buyer-submitted transferredAmount on orders for seller amount-match verification. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE orders ADD COLUMN transferredAmount REAL")
            }
        }

        /** v6: persisted item_types templates, category linked item types, banner destination/schedule/audience, and home section ordering. */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN linkedItemTypesCsv TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE admin_settings ADD COLUMN homeSectionsOrderCsv TEXT NOT NULL DEFAULT 'featured_shops,new_arrivals,popular,recommended,recently_viewed'")
                db.execSQL("ALTER TABLE admin_settings ADD COLUMN hiddenHomeSectionsCsv TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE promo_banners ADD COLUMN badgeIcon TEXT NOT NULL DEFAULT 'sparkles'")
                db.execSQL("ALTER TABLE promo_banners ADD COLUMN destinationType TEXT NOT NULL DEFAULT 'CATEGORY'")
                db.execSQL("ALTER TABLE promo_banners ADD COLUMN targetId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE promo_banners ADD COLUMN startDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE promo_banners ADD COLUMN endDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE promo_banners ADD COLUMN audience TEXT NOT NULL DEFAULT 'ALL'")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `item_types` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `label` TEXT NOT NULL,
                        `emoji` TEXT NOT NULL DEFAULT '✨',
                        `categoryId` TEXT NOT NULL DEFAULT '',
                        `groupName` TEXT NOT NULL DEFAULT 'Other',
                        `attributesJson` TEXT NOT NULL DEFAULT '[]',
                        `sortOrder` INTEGER NOT NULL DEFAULT 0
                    )"""
                )
                ProductTemplates.defaultAll.forEachIndexed { idx, t ->
                    db.execSQL(
                        """INSERT OR IGNORE INTO item_types
                            (id, label, emoji, categoryId, groupName, attributesJson, sortOrder)
                            VALUES (?, ?, ?, ?, ?, ?, ?)""",
                        arrayOf<Any?>(t.id, t.label, t.emoji, t.categoryId, t.group, attributesToJson(t.attributes), idx)
                    )
                }
                ProductTemplates.defaultAll.groupBy { it.categoryId }.forEach { (catId, list) ->
                    if (catId.isNotBlank()) {
                        db.execSQL(
                            "UPDATE categories SET linkedItemTypesCsv = ? WHERE id = ? AND linkedItemTypesCsv = ''",
                            arrayOf<Any?>(list.joinToString(",") { it.id }, catId)
                        )
                    }
                }
            }
        }

        /** v7: createdAt timestamps on users and shops for real time-windowed analytics & NEW_USERS audience targeting. */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN createdAt TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE shops ADD COLUMN createdAt TEXT NOT NULL DEFAULT ''")
            }
        }

        /** v8: shop gallery, social links, policies, delivery cutoffs, and chat order-context/category fields. */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shops ADD COLUMN galleryJson TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE shops ADD COLUMN instagramLink TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE shops ADD COLUMN tiktokLink TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE shops ADD COLUMN websiteUrl TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE shops ADD COLUMN returnPolicy TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE shops ADD COLUMN deliveryNotesPolicy TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE shops ADD COLUMN standardCutoffTime TEXT NOT NULL DEFAULT '16:00'")
                db.execSQL("ALTER TABLE shops ADD COLUMN expressCutoffTime TEXT NOT NULL DEFAULT '14:00'")
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN orderContextRef TEXT")
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN orderContextStatus TEXT")
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN orderContextTotal TEXT")
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN threadCategory TEXT NOT NULL DEFAULT 'QUESTION'")
            }
        }

        /** v9: per-user wishlist_items, shop_follows, and chat_messages.customerUserId. */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `wishlist_items` (
                        `userId` TEXT NOT NULL,
                        `productId` TEXT NOT NULL,
                        `createdAt` TEXT NOT NULL DEFAULT '',
                        PRIMARY KEY(`userId`, `productId`)
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `shop_follows` (
                        `userId` TEXT NOT NULL,
                        `shopId` TEXT NOT NULL,
                        `createdAt` TEXT NOT NULL DEFAULT '',
                        PRIMARY KEY(`userId`, `shopId`)
                    )"""
                )
                db.execSQL(
                    """INSERT OR IGNORE INTO wishlist_items (userId, productId)
                        SELECT 'user_fitsum', id FROM products WHERE isWishlisted = 1"""
                )
                db.execSQL(
                    """INSERT OR IGNORE INTO shop_follows (userId, shopId)
                        SELECT 'user_fitsum', id FROM shops WHERE isFollowing = 1"""
                )
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN customerUserId TEXT NOT NULL DEFAULT 'user_fitsum'")
                db.execSQL(
                    """UPDATE chat_messages
                        SET conversationId = conversationId || '_user_fitsum'
                        WHERE conversationId IN ('conv_techhub', 'conv_fashionworld')"""
                )
            }
        }

        /** v10: Phase 5 returns, finance/payouts/commissions, subscription upgrades, flash sales, promo scopes, review photos/tags/replies, support thread messages, broadcasts. */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shops ADD COLUMN payoutProviderType TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE shops ADD COLUMN payoutBankName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE shops ADD COLUMN payoutAccountName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE shops ADD COLUMN payoutAccountNumber TEXT NOT NULL DEFAULT ''")

                db.execSQL("ALTER TABLE promo_codes ADD COLUMN scopeType TEXT NOT NULL DEFAULT 'SHOP'")
                db.execSQL("ALTER TABLE promo_codes ADD COLUMN scopeTargetIdsCsv TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE promo_codes ADD COLUMN maxUsesTotal INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE promo_codes ADD COLUMN usesCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE promo_codes ADD COLUMN perCustomerLimit INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE promo_codes ADD COLUMN startDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE promo_codes ADD COLUMN ordersCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE promo_codes ADD COLUMN revenueGenerated REAL NOT NULL DEFAULT 0.0")

                db.execSQL("ALTER TABLE reviews ADD COLUMN tagsCsv TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE reviews ADD COLUMN photosJson TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE reviews ADD COLUMN isAnonymous INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE reviews ADD COLUMN helpfulCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE reviews ADD COLUMN helpfulUserIdsCsv TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE reviews ADD COLUMN sellerReply TEXT")
                db.execSQL("ALTER TABLE reviews ADD COLUMN sellerReplyAt TEXT")

                db.execSQL("ALTER TABLE admin_settings ADD COLUMN isPilotMode INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE admin_settings ADD COLUMN pilotEndDate TEXT NOT NULL DEFAULT '2026-12-31'")

                db.execSQL("ALTER TABLE flagged_reviews ADD COLUMN reviewId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE flagged_reviews ADD COLUMN productName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE flagged_reviews ADD COLUMN rating REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE flagged_reviews ADD COLUMN moderatorNote TEXT NOT NULL DEFAULT ''")

                db.execSQL("ALTER TABLE subscription_plans ADD COLUMN maxPromoCodes INTEGER NOT NULL DEFAULT 5")
                db.execSQL("ALTER TABLE subscription_plans ADD COLUMN analyticsDays INTEGER NOT NULL DEFAULT 7")

                db.execSQL("ALTER TABLE support_tickets ADD COLUMN priority TEXT NOT NULL DEFAULT 'NORMAL'")
                db.execSQL("ALTER TABLE support_tickets ADD COLUMN requesterRole TEXT NOT NULL DEFAULT 'BUYER'")
                db.execSQL("ALTER TABLE support_tickets ADD COLUMN assignedTo TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE support_tickets ADD COLUMN resolutionSummary TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE support_tickets ADD COLUMN updatedAt TEXT NOT NULL DEFAULT ''")

                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `return_requests` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `orderId` TEXT NOT NULL,
                        `orderReference` TEXT NOT NULL,
                        `shopId` TEXT NOT NULL,
                        `shopName` TEXT NOT NULL,
                        `customerUserId` TEXT NOT NULL,
                        `customerName` TEXT NOT NULL,
                        `itemsJson` TEXT NOT NULL,
                        `refundAmount` REAL NOT NULL,
                        `reason` TEXT NOT NULL,
                        `comment` TEXT NOT NULL DEFAULT '',
                        `photosJson` TEXT NOT NULL DEFAULT '[]',
                        `resolution` TEXT NOT NULL DEFAULT 'REFUND',
                        `status` TEXT NOT NULL DEFAULT 'REQUESTED',
                        `sellerNote` TEXT NOT NULL DEFAULT '',
                        `refundMethod` TEXT NOT NULL DEFAULT 'Telebirr',
                        `createdAt` TEXT NOT NULL,
                        `updatedAt` TEXT NOT NULL DEFAULT ''
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `payout_requests` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `shopId` TEXT NOT NULL,
                        `shopName` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `providerType` TEXT NOT NULL,
                        `bankName` TEXT NOT NULL,
                        `accountName` TEXT NOT NULL,
                        `accountNumber` TEXT NOT NULL,
                        `status` TEXT NOT NULL DEFAULT 'PENDING',
                        `referenceCode` TEXT NOT NULL DEFAULT '',
                        `note` TEXT NOT NULL DEFAULT '',
                        `createdAt` TEXT NOT NULL,
                        `processedAt` TEXT NOT NULL DEFAULT ''
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `commission_configs` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `categoryId` TEXT NOT NULL,
                        `categoryName` TEXT NOT NULL,
                        `ratePercent` REAL NOT NULL,
                        `minPayoutAmount` REAL NOT NULL DEFAULT 500.0
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `subscription_upgrades` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `shopId` TEXT NOT NULL,
                        `shopName` TEXT NOT NULL,
                        `planId` TEXT NOT NULL,
                        `planName` TEXT NOT NULL,
                        `billingCycle` TEXT NOT NULL DEFAULT 'MONTHLY',
                        `amount` REAL NOT NULL,
                        `paymentMethodTitle` TEXT NOT NULL DEFAULT 'Telebirr',
                        `transactionReference` TEXT NOT NULL,
                        `receiptImageUri` TEXT NOT NULL DEFAULT '',
                        `status` TEXT NOT NULL DEFAULT 'PENDING',
                        `adminNote` TEXT NOT NULL DEFAULT '',
                        `createdAt` TEXT NOT NULL,
                        `reviewedAt` TEXT NOT NULL DEFAULT ''
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `flash_sales` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `shopId` TEXT NOT NULL,
                        `shopName` TEXT NOT NULL,
                        `productId` TEXT NOT NULL,
                        `productName` TEXT NOT NULL,
                        `productImageUrl` TEXT NOT NULL DEFAULT '',
                        `categoryId` TEXT NOT NULL DEFAULT '',
                        `originalPrice` REAL NOT NULL,
                        `salePrice` REAL NOT NULL,
                        `discountPercent` INTEGER NOT NULL,
                        `stockCap` INTEGER NOT NULL,
                        `claimedCount` INTEGER NOT NULL DEFAULT 0,
                        `startAt` TEXT NOT NULL DEFAULT '',
                        `endAt` TEXT NOT NULL DEFAULT '',
                        `isActive` INTEGER NOT NULL DEFAULT 1
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `support_ticket_messages` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `ticketId` TEXT NOT NULL,
                        `senderRole` TEXT NOT NULL,
                        `senderName` TEXT NOT NULL,
                        `message` TEXT NOT NULL,
                        `isInternalNote` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` TEXT NOT NULL
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `admin_broadcasts` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `title` TEXT NOT NULL,
                        `body` TEXT NOT NULL,
                        `audience` TEXT NOT NULL DEFAULT 'ALL',
                        `imageUrl` TEXT NOT NULL DEFAULT '',
                        `destinationType` TEXT NOT NULL DEFAULT 'NONE',
                        `targetId` TEXT NOT NULL DEFAULT '',
                        `scheduledAt` TEXT NOT NULL DEFAULT '',
                        `status` TEXT NOT NULL DEFAULT 'SENT',
                        `sentCount` INTEGER NOT NULL DEFAULT 0,
                        `openedCount` INTEGER NOT NULL DEFAULT 0,
                        `isTemplate` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` TEXT NOT NULL
                    )"""
                )
            }
        }

        /** v11: orders.deliveredAt for 7-day return window + analytics_events for real product views and cart adds. */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `orders` ADD COLUMN `deliveredAt` TEXT")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `analytics_events` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `shopId` TEXT NOT NULL,
                        `productId` TEXT NOT NULL DEFAULT '',
                        `eventType` TEXT NOT NULL,
                        `timestampMillis` INTEGER NOT NULL
                    )"""
                )
            }
        }

        @Volatile
        private var INSTANCE: SuqiDatabase? = null

        @Volatile
        private var INSTANCE_DIR: String? = null

        fun getInstance(context: Context): SuqiDatabase {
            val appContext = context.applicationContext
            val dir = runCatching { appContext.filesDir.absolutePath }.getOrDefault("")
            val existing = INSTANCE
            if (existing != null && INSTANCE_DIR == dir) {
                return existing
            }
            return synchronized(this) {
                val current = INSTANCE
                if (current != null && INSTANCE_DIR == dir) {
                    current
                } else {
                    runCatching { current?.close() }
                    val instance = Room.databaseBuilder(
                        appContext,
                        SuqiDatabase::class.java,
                        "suqi_marketplace.db"
                    ).addMigrations(
                        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                        MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10,
                        MIGRATION_10_11
                    ).addCallback(object : Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                runCatching {
                                    if (INSTANCE_DIR == dir) {
                                        INSTANCE?.ensureSeeded()
                                    }
                                }
                            }
                        }
                    }).build()
                    INSTANCE = instance
                    INSTANCE_DIR = dir
                    instance
                }
            }
        }
    }
}
