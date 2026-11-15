package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import com.example.data.model.MarketplaceCategories
import com.example.data.model.ProductTemplates
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
        SupportTicketEntity::class
    ],
    version = 5,
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

        @Volatile
        private var INSTANCE: SuqiDatabase? = null

        fun getInstance(context: Context): SuqiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SuqiDatabase::class.java,
                    "suqi_marketplace.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).addCallback(object : Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            getInstance(context).ensureSeeded()
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
