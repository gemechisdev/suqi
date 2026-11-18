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
    val defaultAddressId: String? = null,
    val adminTier: String = "NONE",
    val gender: String = "",
    val birthday: String = "",
    val activeRole: String = "",
    val isActiveSession: Boolean = false,
    val createdAt: String = ""
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
    val isFollowing: Boolean = false,
    val requireReceiptScreenshot: Boolean = false,
    val businessHoursJson: String = "",
    val vacationReturnDate: String = "",
    val vacationNote: String = "",
    val freeDeliveryThreshold: Double = 0.0,
    val coverageSubCitiesCsv: String = "",
    val verificationDocsJson: String = "",
    val verificationRejectionReason: String? = null,
    val createdAt: String = "",
    val galleryJson: String = "[]",
    val instagramLink: String = "",
    val tiktokLink: String = "",
    val websiteUrl: String = "",
    val returnPolicy: String = "",
    val deliveryNotesPolicy: String = "",
    val standardCutoffTime: String = "16:00",
    val expressCutoffTime: String = "14:00",
    val payoutProviderType: String = "",
    val payoutBankName: String = "",
    val payoutAccountName: String = "",
    val payoutAccountNumber: String = ""
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
    val isWishlisted: Boolean = false,
    /** JSON document: item type, media, attributes, values, variants (see ProductDetails). */
    val detailsJson: String = ""
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
    val createdAt: String,
    val receiptImageUri: String = "",
    val payerIdentifier: String = "",
    val sellerNotes: String = "",
    val courierName: String = "",
    val courierPhone: String = "",
    val dispatchedAt: String? = null,
    val deliveredAt: String? = null,
    val transferredAmount: Double? = null,
    val disputeNote: String = "",
    val disputeResolution: String = ""
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
    val isActive: Boolean = true,
    val scopeType: String = "SHOP",
    val scopeTargetIdsCsv: String = "",
    val maxUsesTotal: Int = 0,
    val usesCount: Int = 0,
    val perCustomerLimit: Int = 1,
    val startDate: String = "",
    val ordersCount: Int = 0,
    val revenueGenerated: Double = 0.0
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
    val date: String,
    val tagsCsv: String = "",
    val photosJson: String = "[]",
    val isAnonymous: Boolean = false,
    val helpfulCount: Int = 0,
    val helpfulUserIdsCsv: String = "",
    val sellerReply: String? = null,
    val sellerReplyAt: String? = null
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
    val isRead: Boolean = true,
    val imageUrl: String = "",
    val orderContextRef: String? = null,
    val orderContextStatus: String? = null,
    val orderContextTotal: String? = null,
    val threadCategory: String = "QUESTION",
    val customerUserId: String = ""
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
    val imageUrl: String = "",
    val subCategoriesCsv: String,
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
    val linkedItemTypesCsv: String = ""
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
    val activePlanPrice: Double = 299.0,
    val homeSectionsOrderCsv: String = "featured_shops,new_arrivals,popular,recommended,recently_viewed",
    val hiddenHomeSectionsCsv: String = "",
    val isPilotMode: Boolean = true,
    val pilotEndDate: String = "2026-12-31"
)

@Entity(tableName = "promo_banners")
data class PromoBannerEntity(
    @PrimaryKey val id: String,
    val badge: String,
    val title: String,
    val highlight: String,
    val subtitle: String,
    val buttonText: String,
    val imageUrl: String,
    val targetCategory: String,
    val targetName: String,
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
    val badgeIcon: String = "sparkles",
    val destinationType: String = "CATEGORY",
    val targetId: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val audience: String = "ALL"
)

@Entity(tableName = "item_types")
data class ItemTypeEntity(
    @PrimaryKey val id: String,
    val label: String,
    val emoji: String = "✨",
    val categoryId: String = "",
    val groupName: String = "Other",
    val attributesJson: String = "[]",
    val sortOrder: Int = 0
)

@Entity(tableName = "flagged_reviews")
data class FlaggedReviewEntity(
    @PrimaryKey val id: String,
    val authorName: String,
    val reason: String,
    val reviewText: String,
    val status: String = "REPORTED",
    val createdAt: String,
    val reviewId: String = "",
    val productName: String = "",
    val rating: Double = 0.0,
    val moderatorNote: String = ""
)

@Entity(tableName = "product_questions")
data class ProductQuestionEntity(
    @PrimaryKey val id: String,
    val productId: String,
    val shopId: String,
    val askerName: String,
    val question: String,
    val answer: String = "",
    val answeredByShop: Boolean = false,
    val createdAt: String
)

@Entity(tableName = "subscription_plans")
data class SubscriptionPlanEntity(
    @PrimaryKey val id: String,
    val name: String,
    val monthlyPrice: Double,
    val yearlyPrice: Double,
    val maxProducts: Int,
    val featuresCsv: String,
    val isPopular: Boolean = false,
    val isActive: Boolean = true,
    val maxPromoCodes: Int = 5,
    val analyticsDays: Int = 7
)

@Entity(tableName = "support_tickets")
data class SupportTicketEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val orderId: String = "",
    val category: String,
    val subject: String,
    val description: String,
    val status: String = "OPEN",
    val createdAt: String,
    val priority: String = "NORMAL",
    val requesterRole: String = "BUYER",
    val assignedTo: String = "",
    val resolutionSummary: String = "",
    val updatedAt: String = ""
)

@Entity(
    tableName = "wishlist_items",
    primaryKeys = ["userId", "productId"]
)
data class WishlistItemEntity(
    val userId: String,
    val productId: String,
    val createdAt: String = ""
)

@Entity(
    tableName = "shop_follows",
    primaryKeys = ["userId", "shopId"]
)
data class ShopFollowEntity(
    val userId: String,
    val shopId: String,
    val createdAt: String = ""
)

@Entity(tableName = "return_requests")
data class ReturnRequestEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val orderReference: String,
    val shopId: String,
    val shopName: String,
    val customerUserId: String,
    val customerName: String,
    val itemsJson: String,
    val refundAmount: Double,
    val reason: String,
    val comment: String = "",
    val photosJson: String = "[]",
    val resolution: String = "REFUND",
    val status: String = "REQUESTED",
    val sellerNote: String = "",
    val refundMethod: String = "Telebirr",
    val createdAt: String,
    val updatedAt: String = ""
)

@Entity(tableName = "payout_requests")
data class PayoutRequestEntity(
    @PrimaryKey val id: String,
    val shopId: String,
    val shopName: String,
    val amount: Double,
    val providerType: String,
    val bankName: String,
    val accountName: String,
    val accountNumber: String,
    val status: String = "PENDING",
    val referenceCode: String = "",
    val note: String = "",
    val createdAt: String,
    val processedAt: String = ""
)

@Entity(tableName = "commission_configs")
data class CommissionConfigEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val categoryName: String,
    val ratePercent: Double,
    val minPayoutAmount: Double = 500.0
)

@Entity(tableName = "subscription_upgrades")
data class SubscriptionUpgradeEntity(
    @PrimaryKey val id: String,
    val shopId: String,
    val shopName: String,
    val planId: String,
    val planName: String,
    val billingCycle: String = "MONTHLY",
    val amount: Double,
    val paymentMethodTitle: String = "Telebirr",
    val transactionReference: String,
    val receiptImageUri: String = "",
    val status: String = "PENDING",
    val adminNote: String = "",
    val createdAt: String,
    val reviewedAt: String = ""
)

@Entity(tableName = "flash_sales")
data class FlashSaleEntity(
    @PrimaryKey val id: String,
    val shopId: String,
    val shopName: String,
    val productId: String,
    val productName: String,
    val productImageUrl: String = "",
    val categoryId: String = "",
    val originalPrice: Double,
    val salePrice: Double,
    val discountPercent: Int,
    val stockCap: Int,
    val claimedCount: Int = 0,
    val startAt: String = "",
    val endAt: String = "",
    val isActive: Boolean = true
)

@Entity(tableName = "support_ticket_messages")
data class SupportTicketMessageEntity(
    @PrimaryKey val id: String,
    val ticketId: String,
    val senderRole: String,
    val senderName: String,
    val message: String,
    val isInternalNote: Boolean = false,
    val createdAt: String
)

@Entity(tableName = "admin_broadcasts")
data class AdminBroadcastEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val audience: String = "ALL",
    val imageUrl: String = "",
    val destinationType: String = "NONE",
    val targetId: String = "",
    val scheduledAt: String = "",
    val status: String = "SENT",
    val sentCount: Int = 0,
    val openedCount: Int = 0,
    val isTemplate: Boolean = false,
    val createdAt: String
)

@Entity(tableName = "analytics_events")
data class AnalyticsEventEntity(
    @PrimaryKey val id: String,
    val shopId: String,
    val productId: String = "",
    val eventType: String, // "CART_ADD", "PRODUCT_VIEW"
    val timestampMillis: Long
)

// ==================== PHASE 6: ADMIN PLATFORM MANAGEMENT ====================

@Entity(tableName = "audit_log")
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val actorUserId: String,
    val actorName: String,
    val actorRole: String,
    val action: String,
    val targetType: String,
    val targetId: String = "",
    val targetLabel: String = "",
    val beforeValue: String = "",
    val afterValue: String = "",
    val note: String = "",
    val createdAt: String
)

@Entity(tableName = "shop_admin_notes")
data class ShopAdminNoteEntity(
    @PrimaryKey val shopId: String,
    val notes: String = "",
    val isFeatured: Boolean = false,
    val isSuspended: Boolean = false,
    val updatedAt: String = ""
)

@Entity(tableName = "user_admin_overrides")
data class UserAdminOverrideEntity(
    @PrimaryKey val userId: String,
    val isSuspended: Boolean = false,
    val note: String = "",
    val updatedAt: String = ""
)

// ==================== PHASE 6: ADMIN OPS / CONFIG ====================

@Entity(tableName = "location_areas")
data class LocationAreaEntity(
    @PrimaryKey val id: String,
    val name: String,
    val parentId: String = "",
    val level: String, // "REGION", "CITY", "SUB_CITY"
    val isEnabled: Boolean = true,
    val isDefaultZone: Boolean = false,
    val sortOrder: Int = 0
)

@Entity(tableName = "payment_providers")
data class PaymentProviderEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val logoEmoji: String = "🏦",
    val accountFormat: String = "",
    val referenceHint: String = "",
    val isActive: Boolean = true,
    val sortOrder: Int = 0
)

@Entity(tableName = "content_entries")
data class ContentEntryEntity(
    @PrimaryKey val id: String,
    val section: String, // "FAQ", "HELP_TOPIC", "TERMS", "PRIVACY", "ONBOARDING", "EMPTY_STATE"
    val titleEn: String = "",
    val titleAm: String = "",
    val titleOm: String = "",
    val bodyEn: String = "",
    val bodyAm: String = "",
    val bodyOm: String = "",
    val isPublished: Boolean = true,
    val updatedAt: String = ""
)

@Entity(tableName = "platform_switches")
data class PlatformSwitchEntity(
    @PrimaryKey val id: String,
    val groupName: String, // "COMMERCE", "MEDIA", "CATALOG", "MONETIZATION", "EXPERIENCE"
    val isEnabled: Boolean = true,
    val numericValue: Double = 0.0,
    val sortOrder: Int = 0
)
