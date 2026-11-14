package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import com.example.data.model.ProductTemplates
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        AdminSettingsEntity::class
    ],
    version = 2,
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

        @Volatile
        private var INSTANCE: SuqiDatabase? = null

        fun getInstance(context: Context): SuqiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SuqiDatabase::class.java,
                    "suqi_marketplace.db"
                ).addMigrations(MIGRATION_1_2).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            SeedData.seedDatabase(getInstance(context))
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
