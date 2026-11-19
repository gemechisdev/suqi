package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserDto(
    val id: String,
    val name: String,
    val phone: String,
    val email: String,
    val avatarUrl: String,
    val role: String, // "GUEST", "CUSTOMER", "SELLER", "ADMIN"
    val preferredLanguage: String, // "English", "Amharic", "Afaan Oromo"
    val region: String,
    val city: String,
    val subCity: String,
    val shopId: String? = null,
    val defaultAddressId: String? = null,
    val adminTier: String = "NONE", // "OWNER", "ADMIN", "DEVELOPER", "NONE"
    val gender: String = "",
    val birthday: String = "",
    val activeRole: String = role,
    val isActiveSession: Boolean = true,
    val createdAt: String = ""
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
    val expressCutoffTime: String = "14:00"
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
    val isWishlisted: Boolean = false,
    /** Media gallery, dynamic attributes and variants (always populated; legacy products are mapped). */
    val details: ProductDetails = ProductDetails()
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
    val discountType: String, // "PERCENTAGE", "FIXED", "FREE_DELIVERY"
    val discountValue: Double,
    val minOrderAmount: Double,
    val validUntil: String,
    val isActive: Boolean = true,
    val scopeType: String = "SHOP", // "SHOP", "CATEGORY", "PRODUCTS"
    val scopeTargetIds: List<String> = emptyList(),
    val maxUsesTotal: Int = 0, // 0 = unlimited
    val usesCount: Int = 0,
    val perCustomerLimit: Int = 1,
    val startDate: String = "",
    val ordersCount: Int = 0,
    val revenueGenerated: Double = 0.0
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
    val date: String,
    val tags: List<String> = emptyList(),
    val photos: List<String> = emptyList(),
    val isAnonymous: Boolean = false,
    val helpfulCount: Int = 0,
    val isHelpfulByCurrentUser: Boolean = false,
    val sellerReply: String? = null,
    val sellerReplyAt: String? = null
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
    val isRead: Boolean = true,
    val imageUrl: String = "",
    val orderContextRef: String? = null,
    val orderContextStatus: String? = null,
    val orderContextTotal: String? = null,
    val threadCategory: String = "QUESTION",
    val customerUserId: String = ""
)

@JsonClass(generateAdapter = true)
data class NotificationDto(
    val id: String,
    val type: String, // "ORDER", "PAYMENT", "CHAT", "SHOP", "PROMO", "SYSTEM"
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

object HomeSectionKeys {
    const val FEATURED_SHOPS = "featured_shops"
    const val NEW_ARRIVALS = "new_arrivals"
    const val POPULAR = "popular"
    const val RECOMMENDED = "recommended"
    const val RECENTLY_VIEWED = "recently_viewed"

    val defaultOrder = listOf(
        FEATURED_SHOPS,
        NEW_ARRIVALS,
        POPULAR,
        RECOMMENDED,
        RECENTLY_VIEWED
    )
}

@JsonClass(generateAdapter = true)
data class CategoryDto(
    val id: String,
    val name: String,
    val nameAmharic: String,
    val nameAfaanOromo: String,
    val icon: String,
    val imageUrl: String = "",
    val subCategories: List<String>,
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
    val linkedItemTypes: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AdminSettingsDto(
    val isChatEnabled: Boolean = true,
    val isExternalContactEnabled: Boolean = true,
    val isSubscriptionModeEnabled: Boolean = false,
    val sellerReviewsEnabled: Boolean = true,
    val showFeaturedShops: Boolean = true,
    val activePlanName: String = "Pro Seller Plan",
    val activePlanPrice: Double = 299.0,
    val homeSectionsOrder: List<String> = HomeSectionKeys.defaultOrder,
    val hiddenHomeSections: List<String> = emptyList(),
    val isPilotMode: Boolean = true,
    val pilotEndDate: String = "2026-12-31",
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String = "",
    val enforceMinVersion: Boolean = false,
    val minAppVersion: String = "1.0"
)

@JsonClass(generateAdapter = true)
data class PromoBannerDto(
    val id: String,
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
    val destinationType: String = "CATEGORY", // "CATEGORY", "SHOP", "PRODUCT", "DEALS", "URL"
    val targetId: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val audience: String = "ALL" // "ALL", "NEW_USERS", "ADDIS_ONLY"
)

@JsonClass(generateAdapter = true)
data class ItemTypeDto(
    val id: String,
    val label: String,
    val emoji: String = "✨",
    val categoryId: String = "",
    val group: String = "Other",
    val attributes: List<AttributeDef> = emptyList(),
    val sortOrder: Int = 0
) {
    fun toItemType() = ItemType(
        id = id,
        label = label,
        emoji = emoji,
        categoryId = categoryId,
        group = group,
        attributes = attributes
    )
}

fun ItemType.toDto(sortOrder: Int = 0) = ItemTypeDto(
    id = id,
    label = label,
    emoji = emoji,
    categoryId = categoryId,
    group = group,
    attributes = attributes,
    sortOrder = sortOrder
)

@JsonClass(generateAdapter = true)
data class OwnerChartSlice(
    val id: String,
    val label: String,
    val value: Double
)

@JsonClass(generateAdapter = true)
data class OwnerTopShopMetric(
    val shopId: String,
    val shopName: String,
    val isVerified: Boolean,
    val gmv: Double,
    val ordersCount: Int,
    val sparkline: List<Float> = emptyList()
)

@JsonClass(generateAdapter = true)
data class OwnerDashboardMetricsDto(
    val rangeDays: Int = 30,
    val totalGmv: Double = 0.0,
    val previousGmv: Double = 0.0,
    val platformRevenue: Double = 0.0,
    val totalOrders: Int = 0,
    val activeShops: Int = 0,
    val totalUsers: Int = 0,
    val newUsersCount: Int = 0,
    val newShopsCount: Int = 0,
    val conversionRate: Double = 0.0,
    val gmvLabels: List<String> = emptyList(),
    val gmvValues: List<Double> = emptyList(),
    val previousGmvValues: List<Double> = emptyList(),
    val statusLabels: List<String> = emptyList(),
    val statusCounts: List<Double> = emptyList(),
    val growthLabels: List<String> = emptyList(),
    val newUsersSeries: List<Double> = emptyList(),
    val newShopsSeries: List<Double> = emptyList(),
    val topCategories: List<OwnerChartSlice> = emptyList(),
    val topShops: List<OwnerTopShopMetric> = emptyList(),
    val verificationsWaitingOver24h: Int = 0,
    val flaggedOrStalePaymentsCount: Int = 0,
    val openTicketsAndDisputesCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class FlaggedReviewDto(
    val id: String,
    val authorName: String,
    val reason: String,
    val reviewText: String,
    val status: String = "REPORTED", // "REPORTED", "HIDDEN", "RESTORED", "DISMISSED"
    val createdAt: String,
    val reviewId: String = "",
    val productName: String = "",
    val rating: Double = 0.0,
    val moderatorNote: String = ""
)

@JsonClass(generateAdapter = true)
data class ProductQuestionDto(
    val id: String,
    val productId: String,
    val shopId: String,
    val askerName: String,
    val question: String,
    val answer: String = "",
    val answeredByShop: Boolean = false,
    val createdAt: String
)

@JsonClass(generateAdapter = true)
data class SubscriptionPlanDto(
    val id: String,
    val name: String,
    val monthlyPrice: Double,
    val yearlyPrice: Double,
    val maxProducts: Int,
    val featuresCsv: String,
    val isPopular: Boolean = false,
    val isActive: Boolean = true,
    val maxPromoCodes: Int = 5,
    val analyticsDays: Int = 7
) {
    fun benefitsList(): List<String> {
        val raw = featuresCsv.trim()
        if (raw.isEmpty()) return emptyList()
        val sep = if (raw.contains('|')) '|' else ','
        return raw.split(sep).map { it.trim() }.filter { it.isNotEmpty() }
    }
}

@JsonClass(generateAdapter = true)
data class SupportTicketDto(
    val id: String,
    val userId: String,
    val orderId: String = "",
    val category: String,
    val subject: String,
    val description: String,
    val status: String = "OPEN", // "OPEN", "PENDING", "RESOLVED"
    val createdAt: String,
    val priority: String = "NORMAL", // "URGENT", "HIGH", "NORMAL", "LOW"
    val requesterRole: String = "BUYER", // "BUYER", "SELLER"
    val assignedTo: String = "",
    val resolutionSummary: String = "",
    val updatedAt: String = ""
)

@JsonClass(generateAdapter = true)
data class ReturnItemDto(
    val productId: String,
    val productName: String,
    val productImageUrl: String = "",
    val quantity: Int = 1,
    val unitPrice: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class ReturnRequestDto(
    val id: String,
    val orderId: String,
    val orderReference: String,
    val shopId: String,
    val shopName: String,
    val customerUserId: String,
    val customerName: String,
    val items: List<ReturnItemDto>,
    val refundAmount: Double,
    val reason: String,
    val comment: String = "",
    val photos: List<String> = emptyList(),
    val resolution: String = "REFUND", // "REFUND", "REPLACE"
    val status: String = "REQUESTED", // "REQUESTED", "APPROVED", "REPLACEMENT_OFFERED", "SENT_BACK", "COMPLETED", "REJECTED"
    val sellerNote: String = "",
    val refundMethod: String = "Telebirr",
    val createdAt: String,
    val updatedAt: String = ""
)

@JsonClass(generateAdapter = true)
data class PayoutAccountDto(
    val providerType: String = "TELEBIRR",
    val bankName: String = "Telebirr",
    val accountName: String = "",
    val accountNumber: String = ""
)

@JsonClass(generateAdapter = true)
data class PayoutRequestDto(
    val id: String,
    val shopId: String,
    val shopName: String,
    val amount: Double,
    val providerType: String,
    val bankName: String,
    val accountName: String,
    val accountNumber: String,
    val status: String = "PENDING", // "PENDING", "APPROVED", "PAID", "REJECTED"
    val referenceCode: String = "",
    val note: String = "",
    val createdAt: String,
    val processedAt: String = ""
)

@JsonClass(generateAdapter = true)
data class LedgerEntryDto(
    val id: String,
    val shopId: String,
    val shopName: String,
    val type: String, // "SALE", "COMMISSION", "SUBSCRIPTION", "REFUND", "PAYOUT"
    val title: String,
    val subtitle: String = "",
    val amount: Double,
    val grossAmount: Double = 0.0,
    val feeAmount: Double = 0.0,
    val orderRef: String = "",
    val weekLabel: String = "",
    val createdAt: String,
    val status: String = "COMPLETED"
)

@JsonClass(generateAdapter = true)
data class MonthlyStatementDto(
    val monthKey: String,
    val monthLabel: String,
    val grossSales: Double,
    val commissions: Double,
    val refunds: Double,
    val payouts: Double,
    val netAmount: Double,
    val ordersCount: Int
)

@JsonClass(generateAdapter = true)
data class SellerEarningsSummaryDto(
    val shopId: String,
    val availableBalance: Double = 0.0,
    val pendingBalance: Double = 0.0,
    val minPayoutAmount: Double = 500.0,
    val defaultCommissionRate: Double = 5.0,
    val payoutAccount: PayoutAccountDto = PayoutAccountDto(),
    val ledger: List<LedgerEntryDto> = emptyList(),
    val statements: List<MonthlyStatementDto> = emptyList(),
    val payoutRequests: List<PayoutRequestDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class CommissionRuleDto(
    val id: String,
    val categoryId: String,
    val categoryName: String,
    val ratePercent: Double,
    val minPayoutAmount: Double = 500.0
)

@JsonClass(generateAdapter = true)
data class AdminFinanceOverviewDto(
    val platformRevenue: Double = 0.0,
    val commissionRevenue: Double = 0.0,
    val subscriptionRevenue: Double = 0.0,
    val pendingPayoutsTotal: Double = 0.0,
    val pendingPayoutsCount: Int = 0,
    val rules: List<CommissionRuleDto> = emptyList(),
    val payoutRequests: List<PayoutRequestDto> = emptyList(),
    val ledger: List<LedgerEntryDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SubscriptionUpgradeDto(
    val id: String,
    val shopId: String,
    val shopName: String,
    val planId: String,
    val planName: String,
    val billingCycle: String = "MONTHLY", // "MONTHLY", "YEARLY"
    val amount: Double,
    val paymentMethodTitle: String = "Telebirr",
    val transactionReference: String,
    val receiptImageUri: String = "",
    val status: String = "PENDING", // "PENDING", "APPROVED", "REJECTED"
    val adminNote: String = "",
    val createdAt: String,
    val reviewedAt: String = ""
)

@JsonClass(generateAdapter = true)
data class FlashSaleDto(
    val id: String,
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

@JsonClass(generateAdapter = true)
data class SellerProductMetricDto(
    val productId: String,
    val productName: String,
    val imageUrl: String = "",
    val revenue: Double,
    val unitsSold: Int,
    val sparkline: List<Float> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SellerAreaMetricDto(
    val subCity: String,
    val customersCount: Int,
    val ordersCount: Int,
    val revenue: Double
)

@JsonClass(generateAdapter = true)
data class SellerAnalyticsDto(
    val rangeDays: Int = 7,
    val allowedMaxDays: Int = 7,
    val isProLocked: Boolean = false,
    val totalRevenue: Double = 0.0,
    val totalOrders: Int = 0,
    val productViews: Int = 0,
    val cartAdds: Int = 0,
    val completedOrders: Int = 0,
    val revenueLabels: List<String> = emptyList(),
    val revenueValues: List<Double> = emptyList(),
    val statusLabels: List<String> = emptyList(),
    val statusCounts: List<Double> = emptyList(),
    val topProducts: List<SellerProductMetricDto> = emptyList(),
    val customerAreas: List<SellerAreaMetricDto> = emptyList(),
    val paymentSplit: List<OwnerChartSlice> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SupportTicketMessageDto(
    val id: String,
    val ticketId: String,
    val senderRole: String, // "BUYER", "SELLER", "ADMIN", "SYSTEM"
    val senderName: String,
    val message: String,
    val isInternalNote: Boolean = false,
    val createdAt: String
)

@JsonClass(generateAdapter = true)
data class AdminBroadcastDto(
    val id: String,
    val title: String,
    val body: String,
    val audience: String = "ALL",
    val imageUrl: String = "",
    val destinationType: String = "NONE",
    val targetId: String = "",
    val scheduledAt: String = "",
    val status: String = "SENT", // "SENT", "SCHEDULED", "TEST", "TEMPLATE"
    val sentCount: Int = 0,
    val openedCount: Int = 0,
    val isTemplate: Boolean = false,
    val createdAt: String
)

// ==================== PHASE 6: ADMIN PLATFORM MANAGEMENT ====================

@JsonClass(generateAdapter = true)
data class AdminUserRowDto(
    val id: String,
    val name: String,
    val phone: String,
    val email: String = "",
    val avatarUrl: String = "",
    val role: String,
    val activeRole: String = "",
    val region: String = "",
    val city: String = "",
    val subCity: String = "",
    val isSuspended: Boolean = false,
    val ordersCount: Int = 0,
    val totalSpent: Double = 0.0,
    val reviewsCount: Int = 0,
    val reportsAgainst: Int = 0,
    val createdAt: String = ""
)

@JsonClass(generateAdapter = true)
data class AdminShopRowDto(
    val id: String,
    val name: String,
    val logoUrl: String = "",
    val ownerUserId: String,
    val ownerName: String = "",
    val category: String = "",
    val shopType: String = "",
    val isVerified: Boolean = false,
    val verificationStatus: String = "NOT_SUBMITTED",
    val isSuspended: Boolean = false,
    val isFeatured: Boolean = false,
    val planName: String = "Free Pilot",
    val productsCount: Int = 0,
    val ordersCount: Int = 0,
    val gmv: Double = 0.0,
    val notes: String = "",
    val createdAt: String = ""
)

@JsonClass(generateAdapter = true)
data class AuditLogEntryDto(
    val id: String,
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

@JsonClass(generateAdapter = true)
data class LocationAreaDto(
    val id: String,
    val name: String,
    val nameAm: String = "",
    val nameOm: String = "",
    val parentId: String = "",
    val level: String, // "REGION", "CITY", "SUB_CITY"
    val isEnabled: Boolean = true,
    val isDefaultZone: Boolean = false,
    val sortOrder: Int = 0
)

@JsonClass(generateAdapter = true)
data class PaymentProviderDto(
    val id: String,
    val displayName: String,
    val logoEmoji: String = "🏦",
    val accountFormat: String = "",
    val referenceHint: String = "",
    val isActive: Boolean = true,
    val sortOrder: Int = 0
)

@JsonClass(generateAdapter = true)
data class ContentEntryDto(
    val id: String,
    val section: String,
    val titleEn: String = "",
    val titleAm: String = "",
    val titleOm: String = "",
    val bodyEn: String = "",
    val bodyAm: String = "",
    val bodyOm: String = "",
    val isPublished: Boolean = true,
    val updatedAt: String = ""
) {
    fun titleFor(language: SuqiLanguageCode): String = when (language) {
        SuqiLanguageCode.AM -> titleAm.ifBlank { titleEn }
        SuqiLanguageCode.OM -> titleOm.ifBlank { titleEn }
        else -> titleEn
    }

    fun bodyFor(language: SuqiLanguageCode): String = when (language) {
        SuqiLanguageCode.AM -> bodyAm.ifBlank { bodyEn }
        SuqiLanguageCode.OM -> bodyOm.ifBlank { bodyEn }
        else -> bodyEn
    }

    fun isMissingTranslation(language: SuqiLanguageCode): Boolean = when (language) {
        SuqiLanguageCode.AM -> titleAm.isBlank() || bodyAm.isBlank()
        SuqiLanguageCode.OM -> titleOm.isBlank() || bodyOm.isBlank()
        else -> false
    }
}

/** Lightweight language code so the data layer does not depend on the UI i18n package. */
enum class SuqiLanguageCode { EN, AM, OM }

@JsonClass(generateAdapter = true)
data class PlatformSwitchDto(
    val id: String,
    val groupName: String, // "COMMERCE", "MEDIA", "CATALOG", "MONETIZATION", "EXPERIENCE"
    val isEnabled: Boolean = true,
    val numericValue: Double = 0.0,
    val sortOrder: Int = 0
)

@JsonClass(generateAdapter = true)
data class AdminPlatformConfigDto(
    val locations: List<LocationAreaDto> = emptyList(),
    val providers: List<PaymentProviderDto> = emptyList(),
    val switches: List<PlatformSwitchDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AdminDisputeCaseDto(
    val orderId: String,
    val orderReference: String,
    val buyerUserId: String,
    val buyerName: String,
    val sellerName: String,
    val shopName: String,
    val buyerClaim: String = "",
    val sellerResponse: String = "",
    val evidence: List<String> = emptyList(),
    val amount: Double = 0.0,
    val status: String = "OPEN", // "OPEN", "RESOLVED_BUYER", "RESOLVED_SELLER", "PARTIAL", "ESCALATED"
    val resolutionNote: String = "",
    val createdAt: String = ""
)

@JsonClass(generateAdapter = true)
data class AdminReportRowDto(
    val label: String,
    val columns: List<String> = emptyList(),
    val rows: List<List<String>> = emptyList(),
    val isMissingTranslation: Boolean = false
)

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null,
    val errorCode: String? = null
)

@JsonClass(generateAdapter = true)
data class ShopCollectionDto(
    val id: String,
    val shopId: String = "",
    val name: String,
    val productIds: List<String> = emptyList(),
    val isVisible: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: String = "",
    val coverImageUrl: String = ""
)

@JsonClass(generateAdapter = true)
data class ShopStaffDto(
    val id: String,
    val shopId: String = "",
    val name: String,
    val phone: String,
    val role: String = "ORDERS", // "MANAGER", "ORDERS", "CATALOG"
    val isActive: Boolean = true,
    val createdAt: String = ""
)
