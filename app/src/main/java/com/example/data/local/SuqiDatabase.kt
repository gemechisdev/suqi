package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
    version = 1,
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
        @Volatile
        private var INSTANCE: SuqiDatabase? = null

        fun getInstance(context: Context): SuqiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SuqiDatabase::class.java,
                    "suqi_marketplace.db"
                ).addCallback(object : Callback() {
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
