package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserDto(
    val id: String,
    val name: String,
    val phone: String,
    val email: String,
    val avatarUrl: String,
    val role: String, // "CUSTOMER", "SELLER", "ADMIN"
    val preferredLanguage: String, // "English", "Amharic", "Afaan Oromo"
    val region: String,
    val city: String,
    val subCity: String,
    val shopId: String? = null,
    val defaultAddressId: String? = null
)

@JsonClass(generateAdapter = true)
data class ShopDto(
    val id: String,
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
    val verificationStatus: String, // "NOT_SUBMITTED", "PENDING_REVIEW", "VERIFIED", "REJECTED"
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

@JsonClass(generateAdapter = true)
data class ProductDto(
    val id: String,
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
    val extraImages: List<String> = emptyList(),
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
    val colors: List<String> = emptyList(),
    val sizesOrSpecs: List<String> = emptyList(),
    val status: String = "ACTIVE", // "ACTIVE", "PAUSED", "ARCHIVED"
    val isWishlisted: Boolean = false
)

@JsonClass(generateAdapter = true)
data class CartItemDto(
    val id: Long = 0,
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

@JsonClass(generateAdapter = true)
data class OrderItemDto(
    val productId: String,
    val productName: String,
    val productImageUrl: String,
    val variation: String,
    val unitPrice: Double,
    val quantity: Int,
    val totalPrice: Double
)

@JsonClass(generateAdapter = true)
data class OrderDto(
    val id: String,
    val orderReference: String,
    val customerUserId: String,
    val customerName: String,
    val customerPhone: String,
    val deliveryAddress: String,
    val shopId: String,
    val shopName: String,
    val items: List<OrderItemDto>,
    val subtotal: Double,
    val deliveryFee: Double,
    val discountAmount: Double,
    val totalAmount: Double,
    val promoCodeUsed: String? = null,
    val deliveryOptionTitle: String,
    val deliveryOptionType: String,
    val deliveryEstimatedDays: String,
    val paymentMethodTitle: String,
    val paymentMethodType: String, // "TELEBIRR", "CBE_BIRR", "BANK_TRANSFER"
    val paymentMethodAccount: String,
    val transactionReference: String,
    val paymentStatus: String, // "PENDING_VERIFICATION", "VERIFIED", "REJECTED"
    val rejectionReason: String? = null,
    val verificationNotes: String? = null,
    val verifiedBy: String? = null,
    val verifiedAt: String? = null,
    val orderStatus: String, // "Awaiting Payment Confirmation", "Pending Payment", "Confirmed", "Processing", "Dispatched", "Out for Delivery", "Delivered", "Cancelled"
    val createdAt: String
)

@JsonClass(generateAdapter = true)
data class ShopPaymentMethodDto(
    val id: String,
    val shopId: String,
    val providerType: String, // "BANK_TRANSFER", "TELEBIRR", "CBE_BIRR"
    val bankName: String,
    val accountName: String,
    val accountNumber: String,
    val instructions: String,
    val isRequired: Boolean = true,
    val isEnabled: Boolean = true
)

@JsonClass(generateAdapter = true)
data class ShopDeliveryOptionDto(
    val id: String,
    val shopId: String,
    val optionType: String, // "STANDARD", "EXPRESS", "PICKUP"
    val title: String,
    val fee: Double,
    val estimatedTime: String,
    val isEnabled: Boolean = true
)

@JsonClass(generateAdapter = true)
data class PromoCodeDto(
    val id: String,
    val shopId: String,
    val code: String,
    val discountType: String, // "PERCENTAGE", "FIXED"
    val discountValue: Double,
    val minOrderAmount: Double,
    val validUntil: String,
    val isActive: Boolean = true
)

@JsonClass(generateAdapter = true)
data class ReviewDto(
    val id: String,
    val userId: String,
    val userName: String,
    val userAvatar: String,
    val targetType: String, // "PRODUCT", "SHOP"
    val targetId: String,
    val rating: Double,
    val comment: String,
    val isVerifiedPurchase: Boolean = true,
    val date: String
)

@JsonClass(generateAdapter = true)
data class ChatMessageDto(
    val id: String,
    val conversationId: String,
    val shopId: String,
    val shopName: String,
    val senderType: String, // "CUSTOMER", "SHOP"
    val message: String,
    val timestamp: String,
    val productContextName: String? = null,
    val productContextPrice: String? = null,
    val productContextImage: String? = null,
    val isRead: Boolean = true
)

@JsonClass(generateAdapter = true)
data class NotificationDto(
    val id: String,
    val type: String, // "ORDER", "PAYMENT", "CHAT", "SHOP"
    val title: String,
    val body: String,
    val timestamp: String,
    val isRead: Boolean = false
)

@JsonClass(generateAdapter = true)
data class SavedAddressDto(
    val id: String,
    val fullName: String,
    val phone: String,
    val region: String,
    val city: String,
    val subCity: String,
    val streetOrLandmark: String,
    val deliveryInstructions: String = "",
    val isDefault: Boolean = false
)

@JsonClass(generateAdapter = true)
data class CategoryDto(
    val id: String,
    val name: String,
    val nameAmharic: String,
    val nameAfaanOromo: String,
    val icon: String,
    val subCategories: List<String>
)

@JsonClass(generateAdapter = true)
data class AdminSettingsDto(
    val isChatEnabled: Boolean = true,
    val isExternalContactEnabled: Boolean = true,
    val isSubscriptionModeEnabled: Boolean = false,
    val sellerReviewsEnabled: Boolean = true,
    val showFeaturedShops: Boolean = true,
    val activePlanName: String = "Pro Seller Plan",
    val activePlanPrice: Double = 299.0
)

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null,
    val errorCode: String? = null
)
