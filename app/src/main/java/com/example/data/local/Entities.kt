package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val email: String,
    val avatarUrl: String,
    val role: String,
    val preferredLanguage: String,
    val region: String,
    val city: String,
    val subCity: String,
    val shopId: String? = null,
    val defaultAddressId: String? = null
)

@Entity(tableName = "shops")
data class ShopEntity(
    @PrimaryKey val id: String,
    val ownerUserId: String,
    val name: String,
    val slug: String,
    val logoUrl: String,
    val coverUrl: String,
    val category: String,
    val shopType: String,
    val description: String,
    val rating: Double,
    val reviewCount: Int,
    val isVerified: Boolean,
    val verificationStatus: String,
    val phone: String,
    val telegramLink: String,
    val whatsappLink: String,
    val address: String,
    val region: String,
    val city: String,
    val subCity: String,
    val followersCount: Int,
    val productCount: Int,
    val isVacationMode: Boolean = false,
    val acceptsOrders: Boolean = true,
    val isFollowing: Boolean = false
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val shopId: String,
    val shopName: String,
    val shopVerified: Boolean,
    val name: String,
    val nameAmharic: String = "",
    val nameAfaanOromo: String = "",
    val description: String,
    val descriptionAmharic: String = "",
    val descriptionAfaanOromo: String = "",
    val primaryImageUrl: String,
    val extraImagesCsv: String = "",
    val categoryId: String,
    val subCategory: String,
    val price: Double,
    val originalPrice: Double,
    val discountPercent: Int,
    val stockQuantity: Int,
    val stockHoldEnabled: Boolean = true,
    val rating: Double,
    val reviewCount: Int,
    val soldCount: String,
    val colorsCsv: String = "",
    val specsCsv: String = "",
    val status: String = "ACTIVE",
    val isWishlisted: Boolean = false
)

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val productId: String,
    val shopId: String,
    val shopName: String,
    val shopVerified: Boolean,
    val productName: String,
    val productImageUrl: String,
    val unitPrice: Double,
    val quantity: Int,
    val selectedVariation: String,
    val stockAvailable: Int
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val orderReference: String,
    val customerUserId: String,
    val customerName: String,
    val customerPhone: String,
    val deliveryAddress: String,
    val shopId: String,
    val shopName: String,
    val itemsJson: String,
    val subtotal: Double,
    val deliveryFee: Double,
    val discountAmount: Double,
    val totalAmount: Double,
    val promoCodeUsed: String? = null,
    val deliveryOptionTitle: String,
    val deliveryOptionType: String,
    val deliveryEstimatedDays: String,
    val paymentMethodTitle: String,
    val paymentMethodType: String,
    val paymentMethodAccount: String,
    val transactionReference: String,
    val paymentStatus: String,
    val rejectionReason: String? = null,
    val verificationNotes: String? = null,
    val verifiedBy: String? = null,
    val verifiedAt: String? = null,
    val orderStatus: String,
    val createdAt: String
)

@Entity(tableName = "shop_payment_methods")
data class ShopPaymentMethodEntity(
    @PrimaryKey val id: String,
    val shopId: String,
    val providerType: String,
    val bankName: String,
    val accountName: String,
    val accountNumber: String,
    val instructions: String,
    val isRequired: Boolean = true,
    val isEnabled: Boolean = true
)

@Entity(tableName = "shop_delivery_options")
data class ShopDeliveryOptionEntity(
    @PrimaryKey val id: String,
    val shopId: String,
    val optionType: String,
    val title: String,
    val fee: Double,
    val estimatedTime: String,
    val isEnabled: Boolean = true
)

@Entity(tableName = "promo_codes")
data class PromoCodeEntity(
    @PrimaryKey val id: String,
    val shopId: String,
    val code: String,
    val discountType: String,
    val discountValue: Double,
    val minOrderAmount: Double,
    val validUntil: String,
    val isActive: Boolean = true
)

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val userName: String,
    val userAvatar: String,
    val targetType: String,
    val targetId: String,
    val rating: Double,
    val comment: String,
    val isVerifiedPurchase: Boolean = true,
    val date: String
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val shopId: String,
    val shopName: String,
    val senderType: String,
    val message: String,
    val timestamp: String,
    val productContextName: String? = null,
    val productContextPrice: String? = null,
    val productContextImage: String? = null,
    val isRead: Boolean = true
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String,
    val title: String,
    val body: String,
    val timestamp: String,
    val isRead: Boolean = false
)

@Entity(tableName = "saved_addresses")
data class SavedAddressEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val fullName: String,
    val phone: String,
    val region: String,
    val city: String,
    val subCity: String,
    val streetOrLandmark: String,
    val deliveryInstructions: String = "",
    val isDefault: Boolean = false
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val nameAmharic: String,
    val nameAfaanOromo: String,
    val icon: String,
    val subCategoriesCsv: String
)

@Entity(tableName = "admin_settings")
data class AdminSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val isChatEnabled: Boolean = true,
    val isExternalContactEnabled: Boolean = true,
    val isSubscriptionModeEnabled: Boolean = false,
    val sellerReviewsEnabled: Boolean = true,
    val showFeaturedShops: Boolean = true,
    val activePlanName: String = "Pro Seller Plan",
    val activePlanPrice: Double = 299.0
)
