package com.example.data.backend

import android.content.Context
import com.example.data.api.SuqiApiService
import com.example.data.local.*
import com.example.data.model.*
import com.example.util.EthiopianMarketplaceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class BuiltinSqliteBackend(private val db: SuqiDatabase) : SuqiApiService {

    constructor(context: Context) : this(SuqiDatabase.getInstance(context))

    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.US)
        return sdf.format(Date())
    }

    private suspend fun resolveCurrentUser(): UserEntity? {
        db.ensureSeeded()
        return db.userDao().getActiveSessionUser() ?: db.userDao().getFirstUser()
    }

    private suspend fun resolveActiveSellerShopId(): String? {
        val user = resolveCurrentUser() ?: return null
        if (!user.shopId.isNullOrBlank()) return user.shopId
        return db.shopDao().getShopByOwner(user.id)?.id
    }

    private suspend fun requireSignedInUser(): UserEntity? {
        val user = resolveCurrentUser() ?: return null
        return user.takeIf { !it.role.equals("GUEST", ignoreCase = true) }
    }

    private fun UserEntity.isAdmin(): Boolean =
        role.equals("ADMIN", ignoreCase = true)

    private suspend fun UserEntity.ownsShop(targetShopId: String): Boolean {
        if (targetShopId.isBlank()) return false
        if (isAdmin()) return true
        if (!shopId.isNullOrBlank() && shopId == targetShopId) return true
        return db.shopDao().getShopByOwner(id)?.id == targetShopId
    }

    private suspend fun recordAnalyticsEvent(shopId: String, productId: String, eventType: String) {
        if (shopId.isBlank()) return
        db.analyticsEventDao().insert(
            AnalyticsEventEntity(
                id = "evt_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}",
                shopId = shopId,
                productId = productId,
                eventType = eventType,
                timestampMillis = System.currentTimeMillis()
            )
        )
    }

    private suspend fun currentWishlistedProductIds(): Set<String> {
        val userId = resolveCurrentUser()?.id ?: return emptySet()
        return db.wishlistDao().getProductIdsForUser(userId).toSet()
    }

    private suspend fun currentFollowedShopIds(): Set<String> {
        val userId = resolveCurrentUser()?.id ?: return emptySet()
        return db.shopFollowDao().getShopIdsForUser(userId).toSet()
    }

    private data class ResolvedChatContext(
        val conversationId: String,
        val customerUserId: String
    )

    private suspend fun resolveChatContext(
        shopId: String,
        explicitConvId: String? = null,
        senderTypeHint: String? = null
    ): ResolvedChatContext {
        val current = resolveCurrentUser()
        val currentUserId = current?.id ?: "user_guest"
        val sellerShopId = resolveActiveSellerShopId()
        val isSellerForShop = sellerShopId == shopId && (
            senderTypeHint.equals("SHOP", ignoreCase = true) ||
                current?.activeRole.equals("SELLER", ignoreCase = true) ||
                (current?.activeRole.isNullOrBlank() && current?.role.equals("SELLER", ignoreCase = true))
            )
        if (!explicitConvId.isNullOrBlank()) {
            val existingCustomer = db.chatDao().getMessages(explicitConvId)
                .firstOrNull { it.customerUserId.isNotBlank() }?.customerUserId
            val resolvedCustomer = existingCustomer ?: if (isSellerForShop) {
                explicitConvId.removePrefix("conv_${shopId.replace("shop_", "")}_").ifBlank { currentUserId }
            } else {
                currentUserId
            }
            return ResolvedChatContext(conversationId = explicitConvId, customerUserId = resolvedCustomer)
        }
        if (isSellerForShop) {
            val latestMsg = db.chatDao().getMessagesForShopDesc(shopId).firstOrNull()
            if (latestMsg != null) {
                return ResolvedChatContext(
                    conversationId = latestMsg.conversationId,
                    customerUserId = latestMsg.customerUserId.ifBlank { currentUserId }
                )
            }
        }
        val convId = "conv_${shopId.replace("shop_", "")}_$currentUserId"
        return ResolvedChatContext(conversationId = convId, customerUserId = currentUserId)
    }

    override suspend fun loginWithTelegram(payload: Map<String, String>): ApiResponse<UserDto> = withContext(Dispatchers.IO) {
        db.ensureSeeded()
        val name = payload["name"]?.trim()?.takeIf { it.isNotBlank() } ?: "Telegram Shopper"
        val phone = payload["phone"]?.trim()?.takeIf { it.isNotBlank() } ?: "+251 911 234 567"
        val existing = db.userDao().getAllUsers().firstOrNull {
            it.role != "GUEST" && (it.phone == phone || it.name.equals(name, ignoreCase = true))
        }
        db.userDao().clearActiveSessions()
        val user = if (existing != null) {
            val updated = existing.copy(isActiveSession = true)
            db.userDao().updateUser(updated)
            updated
        } else {
            UserEntity(
                id = "user_tg_${System.currentTimeMillis()}",
                name = name,
                phone = phone,
                email = "",
                avatarUrl = "",
                role = "CUSTOMER",
                preferredLanguage = "English",
                region = "Addis Ababa",
                city = "Addis Ababa",
                subCity = "Bole",
                activeRole = "CUSTOMER",
                isActiveSession = true,
                createdAt = getCurrentTime()
            ).also { db.userDao().insertUser(it) }
        }
        ApiResponse(success = true, data = user.toDto())
    }

    override suspend fun loginWithGoogle(payload: Map<String, String>): ApiResponse<UserDto> = withContext(Dispatchers.IO) {
        db.ensureSeeded()
        val name = payload["name"]?.trim()?.takeIf { it.isNotBlank() } ?: "Suqi Shopper"
        val email = payload["email"]?.trim()?.takeIf { it.isNotBlank() } ?: "shopper@suqi.et"
        val existing = db.userDao().getAllUsers().firstOrNull {
            it.role != "GUEST" && it.email.equals(email, ignoreCase = true)
        }
        db.userDao().clearActiveSessions()
        val user = if (existing != null) {
            val updated = existing.copy(isActiveSession = true)
            db.userDao().updateUser(updated)
            updated
        } else {
            UserEntity(
                id = "user_g_${System.currentTimeMillis()}",
                name = name,
                phone = "+251 912 345 678",
                email = email,
                avatarUrl = "",
                role = "CUSTOMER",
                preferredLanguage = "English",
                region = "Addis Ababa",
                city = "Addis Ababa",
                subCity = "Bole",
                activeRole = "CUSTOMER",
                isActiveSession = true,
                createdAt = getCurrentTime()
            ).also { db.userDao().insertUser(it) }
        }
        ApiResponse(success = true, data = user.toDto())
    }

    override suspend fun loginWithPhoneOtp(payload: Map<String, String>): ApiResponse<UserDto> = withContext(Dispatchers.IO) {
        db.ensureSeeded()
        val rawPhone = payload["phone"]?.trim().orEmpty()
        val otp = payload["otp"]?.trim().orEmpty()
        val name = payload["name"]?.trim().orEmpty()
        if (rawPhone.length < 9) {
            return@withContext ApiResponse(success = false, message = "Enter a valid Ethiopian phone number (+251 9xx xxx xxx)")
        }
        if (otp.length != 6 || !otp.all { it.isDigit() }) {
            return@withContext ApiResponse(success = false, message = "Enter the 6-digit SMS verification code (demo: 123456)")
        }
        val normalizedPhone = if (rawPhone.startsWith("+251")) rawPhone else "+251 ${rawPhone.removePrefix("0")}"
        val existing = db.userDao().getAllUsers().firstOrNull {
            it.phone.replace(" ", "") == normalizedPhone.replace(" ", "")
        }
        db.userDao().clearActiveSessions()
        val user = if (existing != null) {
            val updated = existing.copy(
                name = name.ifBlank { existing.name },
                isActiveSession = true
            )
            db.userDao().updateUser(updated)
            updated
        } else {
            UserEntity(
                id = "user_ph_${System.currentTimeMillis()}",
                name = name.ifBlank { "Suqi Shopper" },
                phone = normalizedPhone,
                email = "",
                avatarUrl = "",
                role = "CUSTOMER",
                preferredLanguage = "English",
                region = "Addis Ababa",
                city = "Addis Ababa",
                subCity = "Bole",
                activeRole = "CUSTOMER",
                isActiveSession = true,
                createdAt = getCurrentTime()
            ).also { db.userDao().insertUser(it) }
        }
        ApiResponse(success = true, data = user.toDto())
    }

    override suspend fun loginAsGuest(): ApiResponse<UserDto> = withContext(Dispatchers.IO) {
        db.ensureSeeded()
        db.userDao().clearActiveSessions()
        val existing = db.userDao().getUserById("user_guest")
        val guest = if (existing != null) {
            existing.copy(isActiveSession = true, role = "GUEST", activeRole = "GUEST").also {
                db.userDao().updateUser(it)
            }
        } else {
            UserEntity(
                id = "user_guest",
                name = "Guest Shopper",
                phone = "",
                email = "",
                avatarUrl = "",
                role = "GUEST",
                preferredLanguage = "English",
                region = "Addis Ababa",
                city = "Addis Ababa",
                subCity = "Bole",
                activeRole = "GUEST",
                isActiveSession = true
            ).also { db.userDao().insertUser(it) }
        }
        ApiResponse(success = true, data = guest.toDto())
    }

    override suspend fun adminSignIn(payload: Map<String, String>): ApiResponse<UserDto> = withContext(Dispatchers.IO) {
        db.ensureSeeded()
        val identifier = payload["identifier"]?.trim().orEmpty()
        val pin = payload["pin"]?.trim().orEmpty()
        val tier = payload["tier"]?.trim()?.uppercase()?.takeIf { it in listOf("OWNER", "ADMIN", "DEVELOPER") } ?: "OWNER"
        if (identifier.isBlank() || pin.length < 4) {
            return@withContext ApiResponse(success = false, message = "Enter admin email/phone and at least a 4-digit PIN.")
        }
        // Accept seeded owner credentials or valid staff PIN "123456" / "2026"
        val isValidCreds = (identifier.equals("admin@suqi.et", ignoreCase = true) ||
            identifier.contains("911") ||
            identifier.equals("dev@suqi.et", ignoreCase = true)) &&
            (pin == "123456" || pin == "suqi2026" || pin == "2026" || pin == "0000")
        if (!isValidCreds) {
            return@withContext ApiResponse(
                success = false,
                message = "Invalid admin credentials. Demo Owner: admin@suqi.et • PIN: 123456"
            )
        }
        db.userDao().clearActiveSessions()
        val existingAdmin = db.userDao().getUserById("user_admin")
        val admin = if (existingAdmin != null) {
            existingAdmin.copy(
                adminTier = tier,
                role = "ADMIN",
                activeRole = "ADMIN",
                isActiveSession = true
            ).also { db.userDao().updateUser(it) }
        } else {
            UserEntity(
                id = "user_admin",
                name = "Abebe Kebede",
                phone = "+251 911 000 111",
                email = identifier,
                avatarUrl = "",
                role = "ADMIN",
                preferredLanguage = "English",
                region = "Addis Ababa",
                city = "Addis Ababa",
                subCity = "Kirkos",
                adminTier = tier,
                activeRole = "ADMIN",
                isActiveSession = true,
                createdAt = getCurrentTime()
            ).also { db.userDao().insertUser(it) }
        }
        ApiResponse(success = true, data = admin.toDto())
    }

    override suspend fun updateUserProfile(payload: Map<String, String>): ApiResponse<UserDto> = withContext(Dispatchers.IO) {
        val current = resolveCurrentUser() ?: return@withContext ApiResponse(success = false, message = "No active user session")
        val updated = current.copy(
            name = payload["name"]?.trim()?.takeIf { it.isNotBlank() } ?: current.name,
            phone = payload["phone"]?.trim()?.takeIf { it.isNotBlank() } ?: current.phone,
            email = payload["email"]?.trim() ?: current.email,
            avatarUrl = payload["avatarUrl"] ?: current.avatarUrl,
            preferredLanguage = payload["preferredLanguage"] ?: current.preferredLanguage,
            region = payload["region"] ?: current.region,
            city = payload["city"] ?: current.city,
            subCity = payload["subCity"] ?: current.subCity,
            gender = payload["gender"] ?: current.gender,
            birthday = payload["birthday"] ?: current.birthday,
            activeRole = payload["activeRole"]?.takeIf { it.isNotBlank() } ?: current.activeRole
        )
        db.userDao().updateUser(updated)
        ApiResponse(success = true, data = updated.toDto())
    }

    override suspend fun deleteAccount(): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        val current = resolveCurrentUser()
            ?: return@withContext ApiResponse(success = false, data = false, message = "No active user session")
        if (current.role == "GUEST") {
            return@withContext ApiResponse(success = false, data = false, message = "No active user session")
        }
        if (current.role == "ADMIN" || current.adminTier != "NONE") {
            return@withContext ApiResponse(success = false, data = false, message = "Staff accounts cannot be deleted here.")
        }
        val ownedShop = db.shopDao().getShopByOwner(current.id)
            ?: current.shopId?.takeIf { it.isNotBlank() }?.let { db.shopDao().getShopById(it) }
        if (ownedShop != null) {
            return@withContext ApiResponse(
                success = false,
                data = false,
                message = "Accounts that own a shop cannot be deleted while the shop is active."
            )
        }
        val followedShopIds = db.shopFollowDao().getShopIdsForUser(current.id)
        followedShopIds.forEach { shopId ->
            db.shopDao().getShopById(shopId)?.let { shop ->
                db.shopDao().updateShop(shop.copy(followersCount = maxOf(0, shop.followersCount - 1)))
            }
        }
        db.cartDao().clearAll(current.id)
        db.savedAddressDao().deleteByUserId(current.id)
        db.notificationDao().deleteByUserId(current.id)
        db.wishlistDao().deleteByUserId(current.id)
        db.shopFollowDao().deleteByUserId(current.id)
        db.chatDao().deleteByCustomerUserId(current.id)
        db.supportTicketDao().deleteByUserId(current.id)
        db.reviewDao().anonymizeByUserId(current.id)
        db.orderDao().anonymizeByCustomerId(current.id)
        db.userDao().deleteById(current.id)
        ApiResponse(success = true, data = true)
    }

    override suspend fun getCurrentUser(): ApiResponse<UserDto> = withContext(Dispatchers.IO) {
        val user = resolveCurrentUser()
        ApiResponse(success = user != null, data = user?.toDto())
    }

    override suspend fun upgradeToSeller(payload: Map<String, String>): ApiResponse<ShopDto> = withContext(Dispatchers.IO) {
        val currentUser = resolveCurrentUser()
            ?: return@withContext ApiResponse(success = false, message = "Please sign in before opening a shop.")
        val userId = currentUser.id
        val shopId = "shop_${System.currentTimeMillis()}"
        val newShop = ShopEntity(
            id = shopId,
            ownerUserId = userId,
            name = payload["shopName"]?.trim()?.ifBlank { "My Suqi Shop" } ?: "My Suqi Shop",
            slug = (payload["shopName"] ?: "new-shop").lowercase().replace(" ", "-"),
            logoUrl = "https://images.unsplash.com/photo-1550009158-9ebf69173e03?w=150",
            coverUrl = "https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=800",
            category = payload["category"] ?: "Retail Shop",
            shopType = payload["shopType"] ?: "Retail",
            description = payload["description"] ?: "Welcome to our shop on Suqi!",
            rating = 5.0,
            reviewCount = 0,
            isVerified = false,
            verificationStatus = "NOT_SUBMITTED",
            phone = payload["phone"] ?: currentUser.phone.ifBlank { "+251 900 000 000" },
            telegramLink = payload["telegram"] ?: "",
            whatsappLink = "",
            address = payload["address"] ?: "Addis Ababa, Ethiopia",
            region = payload["region"] ?: "Addis Ababa",
            city = payload["city"] ?: "Addis Ababa",
            subCity = payload["subCity"] ?: "Bole",
            followersCount = 0,
            productCount = 0,
            isVacationMode = false,
            acceptsOrders = true,
            isFollowing = false,
            createdAt = getCurrentTime()
        )
        db.shopDao().insertShop(newShop)

        db.userDao().updateUser(currentUser.copy(role = "SELLER", activeRole = "SELLER", shopId = shopId))

        db.shopPaymentMethodDao().insert(
            ShopPaymentMethodEntity(
                id = "pm_${shopId}_1",
                shopId = shopId,
                providerType = "TELEBIRR",
                bankName = "Telebirr",
                accountName = newShop.name,
                accountNumber = newShop.phone,
                instructions = "Pay with Telebirr and enter the reference number",
                isRequired = true,
                isEnabled = true
            )
        )
        db.shopDeliveryOptionDao().insert(
            ShopDeliveryOptionEntity(
                id = "del_${shopId}_1",
                shopId = shopId,
                optionType = "STANDARD",
                title = "Standard Delivery",
                fee = 50.0,
                estimatedTime = "1-3 days",
                isEnabled = true
            )
        )

        ApiResponse(success = true, data = newShop.toDto(isFollowing = false))
    }

    override suspend fun getProducts(
        query: String?,
        categoryId: String?,
        subCategory: String?,
        shopId: String?,
        inStockOnly: Boolean,
        sortBy: String
    ): ApiResponse<List<ProductDto>> = withContext(Dispatchers.IO) {
        db.ensureSeeded()
        var list = db.productDao().getAllProducts()

        // Filter out archived items on public storefront unless querying a specific shop directly via seller tools
        list = list.filter { it.status == "ACTIVE" }

        if (!query.isNullOrBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.name.lowercase().contains(q) ||
                    it.description.lowercase().contains(q) ||
                    it.shopName.lowercase().contains(q) ||
                    it.subCategory.lowercase().contains(q)
            }
        }

        if (!categoryId.isNullOrBlank() && categoryId != "all") {
            list = list.filter { it.categoryId.equals(categoryId, ignoreCase = true) }
        }

        if (!subCategory.isNullOrBlank() && subCategory != "all") {
            list = list.filter { it.subCategory.equals(subCategory, ignoreCase = true) }
        }

        if (!shopId.isNullOrBlank()) {
            list = list.filter { it.shopId == shopId }
        }

        if (inStockOnly) {
            list = list.filter { it.stockQuantity > 0 }
        }

        val sorted = when (sortBy.uppercase()) {
            "PRICE_LOW_HIGH" -> list.sortedBy { it.price }
            "PRICE_HIGH_LOW" -> list.sortedByDescending { it.price }
            "RATING" -> list.sortedByDescending { it.rating }
            "DISCOUNT" -> list.sortedByDescending { it.discountPercent }
            "NEWEST" -> list.reversed()
            else -> list
        }

        val wishlistedIds = currentWishlistedProductIds()
        ApiResponse(success = true, data = sorted.map { it.toDto(isWishlisted = it.id in wishlistedIds) })
    }

    override suspend fun getProductDetails(id: String): ApiResponse<ProductDto> = withContext(Dispatchers.IO) {
        db.ensureSeeded()
        val prod = db.productDao().getProductById(id)
        if (prod != null) {
            recordAnalyticsEvent(prod.shopId, prod.id, "PRODUCT_VIEW")
            val userId = resolveCurrentUser()?.id
            val isWishlisted = if (userId != null) db.wishlistDao().isWishlisted(userId, id) else false
            ApiResponse(success = true, data = prod.toDto(isWishlisted = isWishlisted))
        } else {
            ApiResponse(success = false, message = "Product not found")
        }
    }

    override suspend fun toggleWishlist(id: String): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        val prod = db.productDao().getProductById(id)
        if (prod != null) {
            val userId = resolveCurrentUser()?.id ?: "user_guest"
            val wasWishlisted = db.wishlistDao().isWishlisted(userId, id)
            val newState = !wasWishlisted
            if (newState) {
                db.wishlistDao().insert(WishlistItemEntity(userId = userId, productId = id, createdAt = getCurrentTime()))
            } else {
                db.wishlistDao().delete(userId, id)
            }
            ApiResponse(success = true, data = newState)
        } else {
            ApiResponse(success = false, data = false)
        }
    }

    override suspend fun getProductQuestions(productId: String): ApiResponse<List<ProductQuestionDto>> = withContext(Dispatchers.IO) {
        val list = db.productQuestionDao().getQuestionsForProduct(productId).map { it.toDto() }
        ApiResponse(success = true, data = list)
    }

    override suspend fun submitProductQuestion(productId: String, payload: Map<String, String>): ApiResponse<ProductQuestionDto> = withContext(Dispatchers.IO) {
        val user = resolveCurrentUser()
        val prod = db.productDao().getProductById(productId)
            ?: return@withContext ApiResponse(success = false, message = "Product not found")
        val questionText = payload["question"]?.trim().orEmpty()
        if (questionText.length < 5) {
            return@withContext ApiResponse(success = false, message = "Please enter at least 5 characters.")
        }
        val entity = ProductQuestionEntity(
            id = "pq_${System.currentTimeMillis()}",
            productId = productId,
            shopId = prod.shopId,
            askerName = user?.name ?: "Shopper",
            question = questionText,
            answer = "",
            answeredByShop = false,
            createdAt = "Just now"
        )
        db.productQuestionDao().insert(entity)
        ApiResponse(success = true, data = entity.toDto())
    }

    override suspend fun getShops(
        filter: String,
        category: String?,
        query: String?
    ): ApiResponse<List<ShopDto>> = withContext(Dispatchers.IO) {
        db.ensureSeeded()
        var list = db.shopDao().getAllShops()

        if (!query.isNullOrBlank()) {
            val q = query.trim().lowercase()
            list = list.filter { it.name.lowercase().contains(q) || it.category.lowercase().contains(q) }
        }

        if (!category.isNullOrBlank() && category != "all") {
            list = list.filter { it.category.equals(category, ignoreCase = true) }
        }

        val followedIds = currentFollowedShopIds()
        list = when (filter.uppercase()) {
            "VERIFIED" -> list.filter { it.isVerified }
            "TOP_RATED" -> list.filter { it.rating >= 4.7 }
            "FOLLOWING" -> list.filter { it.id in followedIds }
            "NEARBY" -> list.filter { it.subCity.equals("Bole", ignoreCase = true) }
            else -> list
        }

        ApiResponse(success = true, data = list.map { it.toDto(isFollowing = it.id in followedIds) })
    }

    override suspend fun getShopDetails(id: String): ApiResponse<ShopDto> = withContext(Dispatchers.IO) {
        val shop = db.shopDao().getShopById(id)
        if (shop != null) {
            val userId = resolveCurrentUser()?.id
            val isFollowing = if (userId != null) db.shopFollowDao().isFollowing(userId, id) else false
            ApiResponse(success = true, data = shop.toDto(isFollowing = isFollowing))
        } else {
            ApiResponse(success = false, message = "Shop not found")
        }
    }

    override suspend fun toggleFollowShop(id: String): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        val shop = db.shopDao().getShopById(id)
        if (shop != null) {
            val userId = resolveCurrentUser()?.id ?: "user_guest"
            val newState = !db.shopFollowDao().isFollowing(userId, id)
            if (newState) {
                db.shopFollowDao().insert(ShopFollowEntity(userId = userId, shopId = id, createdAt = getCurrentTime()))
            } else {
                db.shopFollowDao().delete(userId, id)
            }
            val newCount = if (newState) shop.followersCount + 1 else maxOf(0, shop.followersCount - 1)
            db.shopDao().updateShop(shop.copy(followersCount = newCount))
            ApiResponse(success = true, data = newState)
        } else {
            ApiResponse(success = false, data = false)
        }
    }

    override suspend fun getCategories(): ApiResponse<List<CategoryDto>> = withContext(Dispatchers.IO) {
        db.ensureSeeded()
        val list = db.categoryDao().getAllCategories().map { it.toDto() }
        ApiResponse(success = true, data = list)
    }

    override suspend fun getPromoBanners(activeOnly: Boolean): ApiResponse<List<PromoBannerDto>> = withContext(Dispatchers.IO) {
        db.ensureSeeded()
        val list = (if (activeOnly) db.promoBannerDao().getActiveBanners() else db.promoBannerDao().getAllBanners())
            .map { it.toDto() }
        ApiResponse(success = true, data = list)
    }

    override suspend fun getCartItems(): ApiResponse<List<CartItemDto>> = withContext(Dispatchers.IO) {
        val userId = resolveCurrentUser()?.id ?: return@withContext ApiResponse(success = true, data = emptyList())
        val items = db.cartDao().getCartItems(userId).map { it.toDto() }
        ApiResponse(success = true, data = items)
    }

    override suspend fun addToCart(item: Map<String, Any>): ApiResponse<CartItemDto> = withContext(Dispatchers.IO) {
        val userId = resolveCurrentUser()?.id ?: "user_guest"
        val productId = item["productId"] as String
        val quantity = (item["quantity"] as? Number)?.toInt() ?: 1
        val variation = item["selectedVariation"] as? String ?: ""

        val product = db.productDao().getProductById(productId)
            ?: return@withContext ApiResponse(success = false, message = "Product not found")

        val variant = product.variantForLabel(variation)
        val available = variant?.stock ?: product.stockQuantity
        val unitPrice = variant?.price ?: product.price

        // Same product + variation is one cart line: merge instead of inserting a duplicate row.
        val existing = db.cartDao().findLine(userId, productId, variation)
        val mergedQuantity = (existing?.quantity ?: 0) + quantity
        if (available < mergedQuantity) {
            return@withContext ApiResponse(success = false, message = "Only $available items in stock!")
        }
        if (existing != null) {
            db.cartDao().updateQuantity(existing.id, mergedQuantity)
            recordAnalyticsEvent(product.shopId, product.id, "CART_ADD")
            return@withContext ApiResponse(
                success = true,
                data = existing.copy(quantity = mergedQuantity, stockAvailable = available).toDto()
            )
        }

        val cartEntity = CartItemEntity(
            userId = userId,
            productId = productId,
            shopId = product.shopId,
            shopName = product.shopName,
            shopVerified = product.shopVerified,
            productName = product.name,
            productImageUrl = variant?.imageUrl?.takeIf { it.isNotBlank() } ?: product.primaryImageUrl,
            unitPrice = unitPrice,
            quantity = quantity,
            selectedVariation = variation,
            stockAvailable = available
        )
        val id = db.cartDao().insertCartItem(cartEntity)
        recordAnalyticsEvent(product.shopId, product.id, "CART_ADD")
        ApiResponse(success = true, data = cartEntity.copy(id = id).toDto())
    }

    override suspend fun updateCartQuantity(itemId: Long, quantity: Int): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        if (quantity <= 0) db.cartDao().deleteItem(itemId) else db.cartDao().updateQuantity(itemId, quantity)
        ApiResponse(success = true, data = true)
    }

    override suspend fun removeCartItem(itemId: Long): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        db.cartDao().deleteItem(itemId)
        ApiResponse(success = true, data = true)
    }

    override suspend fun clearCart(): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        val userId = resolveCurrentUser()?.id ?: return@withContext ApiResponse(success = true, data = true)
        db.cartDao().clearAll(userId)
        ApiResponse(success = true, data = true)
    }

    override suspend fun submitOrder(orderData: Map<String, Any>): ApiResponse<OrderDto> = withContext(Dispatchers.IO) {
        val shopId = orderData["shopId"] as String
        val shop = db.shopDao().getShopById(shopId)
        val shopName = orderData["shopName"] as? String ?: shop?.name ?: "Shop"
        val customerName = (orderData["customerName"] as? String)?.trim().orEmpty().ifBlank { "Customer" }
        val customerPhone = (orderData["customerPhone"] as? String)?.trim().orEmpty().ifBlank { "+251 900 000 000" }
        val deliveryAddress = (orderData["deliveryAddress"] as? String)?.trim().orEmpty().ifBlank { "Addis Ababa" }
        val subtotal = (orderData["subtotal"] as Number).toDouble()
        val deliveryFee = (orderData["deliveryFee"] as? Number)?.toDouble() ?: 0.0
        val discountAmount = (orderData["discountAmount"] as? Number)?.toDouble() ?: 0.0
        val totalAmount = (subtotal + deliveryFee - discountAmount).coerceAtLeast(0.0)
        val promoCode = orderData["promoCode"] as? String
        val deliveryOptionTitle = orderData["deliveryOptionTitle"] as? String ?: "Standard Delivery"
        val deliveryOptionType = orderData["deliveryOptionType"] as? String ?: "STANDARD"
        val deliveryEstimatedDays = orderData["deliveryEstimatedDays"] as? String ?: "1-3 days"
        val paymentMethodTitle = orderData["paymentMethodTitle"] as? String ?: "Telebirr"
        val paymentMethodType = orderData["paymentMethodType"] as? String ?: "TELEBIRR"
        val paymentMethodAccount = orderData["paymentMethodAccount"] as? String ?: ""
        val transactionReference = (orderData["transactionReference"] as? String)?.trim() ?: ""
        val receiptImageUri = (orderData["receiptImageUri"] as? String)?.trim() ?: ""
        val payerIdentifier = (orderData["payerIdentifier"] as? String)?.trim() ?: ""
        val transferredAmount = (orderData["transferredAmount"] as? Number)?.toDouble()
            ?: (orderData["transferredAmount"] as? String)?.trim()?.toDoubleOrNull()

        if (transactionReference.isBlank()) {
            return@withContext ApiResponse(success = false, message = "Transaction reference is required")
        }
        if (shop?.requireReceiptScreenshot == true && receiptImageUri.isBlank()) {
            return@withContext ApiResponse(
                success = false,
                message = "${shop.name} requires a payment receipt screenshot before submitting your order."
            )
        }

        val existingOrderWithRef = db.orderDao().getOrderByTransactionReference(transactionReference)
        if (existingOrderWithRef != null) {
            return@withContext ApiResponse(
                success = false,
                message = "This transaction reference ($transactionReference) has already been submitted for another active order."
            )
        }

        val itemsJson = orderData["itemsJson"] as? String ?: "[]"
        val user = resolveCurrentUser()
        val userId = user?.id ?: "user_guest"

        val refNum = 1234 + (db.orderDao().getAllOrders().size)
        val orderReference = "SUQ-2026-${String.format(Locale.US, "%06d", refNum)}"
        val orderId = "ord_${System.currentTimeMillis()}"

        try {
            val jsonArr = JSONArray(itemsJson)
            for (i in 0 until jsonArr.length()) {
                val obj = jsonArr.getJSONObject(i)
                val pId = obj.getString("productId")
                val qty = obj.getInt("quantity")
                val prod = db.productDao().getProductById(pId)
                if (prod != null) {
                    val variant = prod.variantForLabel(obj.optString("variation"))
                    val variantStock = variant?.stock
                    if (prod.stockQuantity < qty || (variantStock != null && variantStock < qty)) {
                        return@withContext ApiResponse(
                            success = false,
                            message = "Insufficient stock for ${prod.name}! Only ${variantStock ?: prod.stockQuantity} left."
                        )
                    }
                    if (prod.stockHoldEnabled) {
                        db.productDao().decrementStock(pId, qty)
                        if (variant != null && variantStock != null) {
                            parseProductDetails(prod.detailsJson)?.let { d ->
                                val updated = d.copy(variants = d.variants.map {
                                    if (it.id == variant.id) it.copy(stock = variantStock - qty) else it
                                })
                                db.productDao().updateDetailsJson(pId, updated.toJson())
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        val order = OrderEntity(
            id = orderId,
            orderReference = orderReference,
            customerUserId = userId,
            customerName = customerName,
            customerPhone = customerPhone,
            deliveryAddress = deliveryAddress,
            shopId = shopId,
            shopName = shopName,
            itemsJson = itemsJson,
            subtotal = subtotal,
            deliveryFee = deliveryFee,
            discountAmount = discountAmount,
            totalAmount = totalAmount,
            promoCodeUsed = promoCode,
            deliveryOptionTitle = deliveryOptionTitle,
            deliveryOptionType = deliveryOptionType,
            deliveryEstimatedDays = deliveryEstimatedDays,
            paymentMethodTitle = paymentMethodTitle,
            paymentMethodType = paymentMethodType,
            paymentMethodAccount = paymentMethodAccount,
            transactionReference = transactionReference,
            paymentStatus = "PENDING_VERIFICATION",
            orderStatus = "Awaiting Payment Confirmation",
            createdAt = getCurrentTime(),
            receiptImageUri = receiptImageUri,
            payerIdentifier = payerIdentifier,
            transferredAmount = transferredAmount
        )
        db.orderDao().insertOrder(order)
        db.cartDao().clearShopItems(userId, shopId)

        if (!promoCode.isNullOrBlank()) {
            db.promoCodeDao().getPromoByCode(shopId, promoCode.trim().uppercase())?.let { promoEntity ->
                db.promoCodeDao().insert(
                    promoEntity.copy(
                        usesCount = promoEntity.usesCount + 1,
                        ordersCount = promoEntity.ordersCount + 1,
                        revenueGenerated = promoEntity.revenueGenerated + totalAmount
                    )
                )
            }
        }
        try {
            val activeSales = db.flashSaleDao().getActive().filter { it.shopId == shopId }.associateBy { it.productId }
            val jsonArr = JSONArray(itemsJson)
            for (i in 0 until jsonArr.length()) {
                val obj = jsonArr.getJSONObject(i)
                val pId = obj.getString("productId")
                val qty = obj.optInt("quantity", 1)
                activeSales[pId]?.let { fs ->
                    db.flashSaleDao().insert(fs.copy(claimedCount = (fs.claimedCount + qty).coerceAtMost(fs.stockCap)))
                }
            }
        } catch (_: Exception) {}

        db.notificationDao().insert(
            NotificationEntity(
                id = "notif_${System.currentTimeMillis()}",
                userId = userId,
                type = "PAYMENT",
                title = "Order Submitted to $shopName",
                body = "Order #$orderReference total ETB ${totalAmount.toInt()} is pending payment verification.",
                timestamp = "Just now",
                isRead = false
            )
        )

        ApiResponse(success = true, data = order.toDto())
    }

    override suspend fun getOrders(): ApiResponse<List<OrderDto>> = withContext(Dispatchers.IO) {
        val userId = resolveCurrentUser()?.id ?: return@withContext ApiResponse(success = true, data = emptyList())
        val orders = db.orderDao().getCustomerOrders(userId).map { it.toDto() }
        ApiResponse(success = true, data = orders)
    }

    override suspend fun getOrderDetails(orderId: String): ApiResponse<OrderDto> = withContext(Dispatchers.IO) {
        val order = db.orderDao().getOrderById(orderId)
        if (order != null) ApiResponse(success = true, data = order.toDto())
        else ApiResponse(success = false, message = "Order not found")
    }

    override suspend fun resubmitPayment(
        orderId: String,
        payload: Map<String, String>
    ): ApiResponse<OrderDto> = withContext(Dispatchers.IO) {
        val order = db.orderDao().getOrderById(orderId)
            ?: return@withContext ApiResponse(success = false, message = "Order not found")

        val newRef = payload["transactionReference"]?.trim() ?: ""
        if (newRef.isBlank()) {
            return@withContext ApiResponse(success = false, message = "Transaction reference is required")
        }

        val existing = db.orderDao().getOrderByTransactionReference(newRef)
        if (existing != null && existing.id != orderId) {
            return@withContext ApiResponse(
                success = false,
                message = "This transaction reference ($newRef) is already registered on another order."
            )
        }

        val newMethod = payload["paymentMethodTitle"] ?: order.paymentMethodTitle
        val newReceipt = payload["receiptImageUri"]?.takeIf { it.isNotBlank() } ?: order.receiptImageUri
        val newPayer = payload["payerIdentifier"]?.takeIf { it.isNotBlank() } ?: order.payerIdentifier
        val newTransferredAmount = payload["transferredAmount"]?.trim()?.toDoubleOrNull() ?: order.transferredAmount

        val updated = order.copy(
            transactionReference = newRef,
            paymentMethodTitle = newMethod,
            receiptImageUri = newReceipt,
            payerIdentifier = newPayer,
            transferredAmount = newTransferredAmount,
            paymentStatus = "PENDING_VERIFICATION",
            orderStatus = "Awaiting Payment Confirmation",
            rejectionReason = null
        )
        db.orderDao().updateOrder(updated)

        db.notificationDao().insert(
            NotificationEntity(
                id = "notif_${System.currentTimeMillis()}",
                userId = order.customerUserId,
                type = "PAYMENT",
                title = "Payment Resubmitted",
                body = "Updated transaction reference $newRef submitted for Order #${order.orderReference}.",
                timestamp = "Just now",
                isRead = false
            )
        )

        ApiResponse(success = true, data = updated.toDto())
    }

    override suspend fun cancelOrder(orderId: String): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        val order = db.orderDao().getOrderById(orderId)
            ?: return@withContext ApiResponse(success = false, message = "Order not found")

        try {
            val jsonArr = JSONArray(order.itemsJson)
            for (i in 0 until jsonArr.length()) {
                val obj = jsonArr.getJSONObject(i)
                val pId = obj.getString("productId")
                val qty = obj.getInt("quantity")
                db.productDao().incrementStock(pId, qty)
            }
        } catch (_: Exception) {}

        db.orderDao().updateOrder(order.copy(orderStatus = "Cancelled"))
        ApiResponse(success = true, data = true)
    }

    override suspend fun getSellerDashboard(): ApiResponse<Map<String, Any>> = withContext(Dispatchers.IO) {
        val shopId = resolveActiveSellerShopId()
            ?: return@withContext ApiResponse(success = false, message = "No shop associated with current account")

        val shop = db.shopDao().getShopById(shopId)
        val orders = db.orderDao().getShopOrders(shopId)
        val pendingCount = orders.count { it.paymentStatus == "PENDING_VERIFICATION" }
        val toDispatchCount = orders.count { it.paymentStatus == "VERIFIED" && it.orderStatus in listOf("Confirmed", "Processing") }
        val totalSales = orders.filter { it.paymentStatus == "VERIFIED" }.sumOf { it.totalAmount }
        val products = db.productDao().getProductsByShop(shopId)
        val activeProducts = products.count { it.status == "ACTIVE" }
        val lowStockCount = products.count { it.stockQuantity in 1..5 }
        val outOfStockCount = products.count { it.stockQuantity == 0 }
        val paymentMethodsCount = db.shopPaymentMethodDao().getMethodsForShop(shopId).count { it.isEnabled }
        val deliveryCount = db.shopDeliveryOptionDao().getOptionsForShop(shopId).count { it.isEnabled }
        val unansweredChats = db.chatDao().getMessagesForShop(shopId).count { !it.isRead && it.senderType == "CUSTOMER" }

        // Build clean chart series grounded in real verified sales (evenly distributed across past orders/days when orders exist)
        val chart7d = if (orders.isEmpty()) listOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        else {
            val verifiedOrders = orders.filter { it.paymentStatus == "VERIFIED" }
            val buckets = MutableList(7) { 0.0 }
            verifiedOrders.forEachIndexed { idx, ord -> buckets[6 - (idx % 7)] += ord.totalAmount }
            buckets
        }

        val data = mapOf<String, Any>(
            "shopId" to shopId,
            "shopName" to (shop?.name ?: "My Shop"),
            "isVerified" to (shop?.isVerified == true),
            "verificationStatus" to (shop?.verificationStatus ?: "NOT_SUBMITTED"),
            "totalSales" to totalSales,
            "totalOrders" to orders.size,
            "pendingOrders" to pendingCount,
            "toDispatchOrders" to toDispatchCount,
            "lowStock" to lowStockCount,
            "outOfStock" to outOfStockCount,
            "productCount" to activeProducts,
            "totalProductCount" to products.size,
            "unansweredChats" to unansweredChats,
            "hasPaymentMethod" to (paymentMethodsCount > 0),
            "hasDeliveryOption" to (deliveryCount > 0),
            "hasLogo" to (!shop?.logoUrl.isNullOrBlank()),
            "shopRating" to (shop?.rating ?: 5.0),
            "chart7d" to chart7d
        )
        ApiResponse(success = true, data = data)
    }

    override suspend fun getSellerProducts(): ApiResponse<List<ProductDto>> = withContext(Dispatchers.IO) {
        val shopId = resolveActiveSellerShopId()
            ?: return@withContext ApiResponse(success = false, message = "No shop associated with current account", data = emptyList())
        val wishlistedIds = currentWishlistedProductIds()
        val list = db.productDao().getProductsByShop(shopId).map { it.toDto(isWishlisted = it.id in wishlistedIds) }
        ApiResponse(success = true, data = list)
    }

    override suspend fun createProduct(productData: Map<String, Any>): ApiResponse<ProductDto> = withContext(Dispatchers.IO) {
        val shopId = resolveActiveSellerShopId()
            ?: return@withContext ApiResponse(success = false, message = "Create a seller shop first before adding products.")
        val shop = db.shopDao().getShopById(shopId)

        validateProductInput(productData)?.let { return@withContext ApiResponse(success = false, message = it) }

        val product = buildProductEntity(
            id = "prod_${System.currentTimeMillis()}",
            shopId = shopId,
            shop = shop,
            data = productData,
            existing = null
        )
        db.productDao().insertProduct(product)
        if (shop != null) {
            db.shopDao().updateShop(shop.copy(productCount = db.productDao().getProductsByShop(shopId).size))
        }
        ApiResponse(success = true, data = product.toDto(isWishlisted = false))
    }

    override suspend fun updateProduct(id: String, productData: Map<String, Any>): ApiResponse<ProductDto> = withContext(Dispatchers.IO) {
        val existing = db.productDao().getProductById(id)
            ?: return@withContext ApiResponse(success = false, message = "Product not found")

        validateProductInput(productData)?.let { return@withContext ApiResponse(success = false, message = it) }

        val product = buildProductEntity(id, existing.shopId, null, productData, existing)
        db.productDao().updateProduct(product)
        val userId = resolveCurrentUser()?.id
        val isWishlisted = if (userId != null) db.wishlistDao().isWishlisted(userId, id) else false
        ApiResponse(success = true, data = product.toDto(isWishlisted = isWishlisted))
    }

    override suspend fun deleteProduct(id: String): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        val existing = db.productDao().getProductById(id)
            ?: return@withContext ApiResponse(success = false, data = false, message = "Product not found")
        db.productDao().deleteById(id)
        db.wishlistDao().deleteByProductId(id)
        db.shopDao().getShopById(existing.shopId)?.let { shop ->
            db.shopDao().updateShop(shop.copy(productCount = db.productDao().getProductsByShop(shop.id).size))
        }
        ApiResponse(success = true, data = true)
    }

    private fun validateProductInput(data: Map<String, Any>): String? {
        val name = (data["name"] as? String)?.trim().orEmpty()
        if (name.isBlank()) return "Product name is required"
        val price = (data["price"] as? Number)?.toDouble() ?: return "A valid price is required"
        if (price <= 0.0) return "Price must be greater than 0"
        val discount = (data["discountPercent"] as? Number)?.toInt() ?: 0
        if (discount !in 0..90) return "Discount must be between 0 and 90%"
        val stock = (data["stockQuantity"] as? Number)?.toInt() ?: 0
        if (stock < 0) return "Stock cannot be negative"
        val details = (data["detailsJson"] as? String)?.let { parseProductDetails(it) }
        if (details != null) {
            if (details.media.size > 12) return "You can add up to 12 media files"
            details.attributes
                .firstOrNull { it.enabled && it.required && details.valuesOf(it.key).isEmpty() }
                ?.let { return "${it.label} is required" }
        }
        return null
    }

    private fun buildProductEntity(
        id: String,
        shopId: String,
        shop: ShopEntity?,
        data: Map<String, Any>,
        existing: ProductEntity?
    ): ProductEntity {
        val details = (data["detailsJson"] as? String)?.let { parseProductDetails(it) }
        val price = (data["price"] as? Number)?.toDouble() ?: existing?.price ?: 0.0
        val discount = (data["discountPercent"] as? Number)?.toInt() ?: existing?.discountPercent ?: 0

        var stock = (data["stockQuantity"] as? Number)?.toInt() ?: existing?.stockQuantity ?: 10
        if (details != null && details.variants.isNotEmpty() && details.variants.all { it.stock != null }) {
            stock = details.variants.sumOf { it.stock ?: 0 }
        }

        val cover = details?.coverImageUrl().orEmpty().ifBlank {
            data["imageUrl"] as? String
                ?: existing?.primaryImageUrl
                ?: "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600"
        }
        val extraImages = details?.media.orEmpty().map { it.url }.filter { it != cover }

        val colorAttr = details?.buyerOptions?.firstOrNull { it.type == AttrType.COLOR }
        val otherOption = details?.buyerOptions?.firstOrNull { it.type != AttrType.COLOR }
        val colorsCsv = if (details != null) {
            colorAttr?.let { details.valuesOf(it.key).joinToString(",") }.orEmpty()
        } else {
            data["colors"] as? String ?: existing?.colorsCsv ?: ""
        }
        val specsCsv = if (details != null) {
            otherOption?.let { details.valuesOf(it.key).joinToString(",") }.orEmpty()
        } else {
            data["specs"] as? String ?: existing?.specsCsv ?: ""
        }

        val typeLabel = details?.itemType?.takeIf { it.isNotBlank() && it != "custom" }
            ?.let { ProductTemplates.byId(it).label }

        return ProductEntity(
            id = id,
            shopId = shopId,
            shopName = shop?.name ?: existing?.shopName ?: "Suqi Shop",
            shopVerified = shop?.isVerified ?: existing?.shopVerified ?: false,
            name = (data["name"] as? String)?.trim() ?: existing?.name ?: "",
            nameAmharic = data["nameAmharic"] as? String ?: existing?.nameAmharic ?: "",
            nameAfaanOromo = data["nameAfaanOromo"] as? String ?: existing?.nameAfaanOromo ?: "",
            description = data["description"] as? String ?: existing?.description ?: "Listed on Suqi Marketplace.",
            descriptionAmharic = data["descriptionAmharic"] as? String ?: existing?.descriptionAmharic ?: "",
            descriptionAfaanOromo = data["descriptionAfaanOromo"] as? String ?: existing?.descriptionAfaanOromo ?: "",
            primaryImageUrl = cover,
            extraImagesCsv = extraImages.joinToString(","),
            categoryId = data["categoryId"] as? String ?: existing?.categoryId ?: "cat_electronics",
            subCategory = data["subCategory"] as? String ?: typeLabel ?: existing?.subCategory ?: "General",
            price = price,
            originalPrice = if (discount > 0) price / (1 - (discount / 100.0)) else price,
            discountPercent = discount,
            stockQuantity = stock,
            stockHoldEnabled = data["stockHoldEnabled"] as? Boolean ?: existing?.stockHoldEnabled ?: true,
            rating = existing?.rating ?: 5.0,
            reviewCount = existing?.reviewCount ?: 0,
            soldCount = existing?.soldCount ?: "New",
            colorsCsv = colorsCsv,
            specsCsv = specsCsv,
            status = data["status"] as? String ?: existing?.status ?: "ACTIVE",
            isWishlisted = false,
            detailsJson = details?.toJson() ?: existing?.detailsJson ?: ""
        )
    }

    private fun ProductEntity.variantForLabel(label: String): ProductVariant? {
        if (label.isBlank()) return null
        val d = parseProductDetails(detailsJson) ?: return null
        if (d.variants.isEmpty()) return null
        val opts = d.buyerOptions
        return d.variants.firstOrNull { v -> ProductDetails.variantLabel(opts, v.options) == label }
    }

    override suspend fun updateProductStatus(id: String, status: String): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        val prod = db.productDao().getProductById(id)
        if (prod != null) {
            db.productDao().updateProduct(prod.copy(status = status))
            ApiResponse(success = true, data = true)
        } else {
            ApiResponse(success = false, data = false, message = "Product not found")
        }
    }

    override suspend fun updateProductStock(id: String, payload: Map<String, Any>): ApiResponse<ProductDto> = withContext(Dispatchers.IO) {
        val existing = db.productDao().getProductById(id)
            ?: return@withContext ApiResponse(success = false, message = "Product not found")
        val targetStock = (payload["stockQuantity"] as? Number)?.toInt()?.coerceAtLeast(0)
            ?: existing.stockQuantity
        val detailsJson = (payload["detailsJson"] as? String)?.takeIf { it.isNotBlank() } ?: existing.detailsJson
        val parsedDetails = parseProductDetails(detailsJson)
        val finalStock = if (parsedDetails != null && parsedDetails.variants.isNotEmpty() && parsedDetails.variants.all { it.stock != null }) {
            parsedDetails.variants.sumOf { (it.stock ?: 0).coerceAtLeast(0) }
        } else {
            targetStock
        }
        val updated = existing.copy(
            stockQuantity = finalStock,
            detailsJson = detailsJson
        )
        db.productDao().updateProduct(updated)
        val userId = resolveCurrentUser()?.id
        val isWishlisted = if (userId != null) db.wishlistDao().isWishlisted(userId, id) else false
        ApiResponse(success = true, data = updated.toDto(isWishlisted = isWishlisted))
    }

    override suspend fun bulkUpdateProducts(payload: Map<String, Any>): ApiResponse<Int> = withContext(Dispatchers.IO) {
        @Suppress("UNCHECKED_CAST")
        val ids = (payload["ids"] as? List<*>)?.mapNotNull { it as? String }?.distinct().orEmpty()
        val action = (payload["action"] as? String)?.uppercase().orEmpty()
        val value = payload["value"] as? String
        if (ids.isEmpty()) {
            return@withContext ApiResponse(success = false, data = 0, message = "Select at least one product")
        }
        var affected = 0
        val touchedShopIds = mutableSetOf<String>()
        for (id in ids) {
            val prod = db.productDao().getProductById(id) ?: continue
            touchedShopIds.add(prod.shopId)
            when (action) {
                "PAUSE" -> {
                    db.productDao().updateProduct(prod.copy(status = "PAUSED"))
                    affected++
                }
                "ACTIVATE" -> {
                    db.productDao().updateProduct(prod.copy(status = "ACTIVE"))
                    affected++
                }
                "ARCHIVE" -> {
                    db.productDao().updateProduct(prod.copy(status = "ARCHIVED"))
                    affected++
                }
                "DELETE" -> {
                    db.productDao().deleteById(id)
                    db.wishlistDao().deleteByProductId(id)
                    affected++
                }
                "OUT_OF_STOCK" -> {
                    val details = parseProductDetails(prod.detailsJson)
                    val updatedDetailsJson = if (details != null && details.variants.isNotEmpty()) {
                        details.copy(variants = details.variants.map { it.copy(stock = 0) }).toJson()
                    } else prod.detailsJson
                    db.productDao().updateProduct(prod.copy(stockQuantity = 0, detailsJson = updatedDetailsJson))
                    affected++
                }
                "SET_STOCK" -> {
                    val targetStock = (value?.toIntOrNull() ?: 0).coerceAtLeast(0)
                    val details = parseProductDetails(prod.detailsJson)
                    val updatedDetailsJson = if (details != null && details.variants.isNotEmpty()) {
                        val perVariant = if (targetStock == 0) 0 else targetStock
                        details.copy(variants = details.variants.map { it.copy(stock = perVariant) }).toJson()
                    } else prod.detailsJson
                    val total = if (details != null && details.variants.isNotEmpty()) {
                        targetStock * details.variants.size
                    } else targetStock
                    db.productDao().updateProduct(prod.copy(stockQuantity = total, detailsJson = updatedDetailsJson))
                    affected++
                }
                "CHANGE_CATEGORY" -> {
                    val catId = value?.trim().orEmpty()
                    if (catId.isNotBlank()) {
                        db.productDao().updateProduct(prod.copy(categoryId = catId))
                        affected++
                    }
                }
                "APPLY_DISCOUNT" -> {
                    val pct = (value?.toIntOrNull() ?: 0).coerceIn(0, 90)
                    val basePrice = maxOf(prod.originalPrice, prod.price).coerceAtLeast(1.0)
                    val newPrice = if (pct > 0) {
                        kotlin.math.round(basePrice * (1.0 - pct / 100.0)).coerceAtLeast(1.0)
                    } else basePrice
                    db.productDao().updateProduct(
                        prod.copy(
                            price = newPrice,
                            originalPrice = basePrice,
                            discountPercent = pct
                        )
                    )
                    affected++
                }
            }
        }
        touchedShopIds.forEach { shopId ->
            db.shopDao().getShopById(shopId)?.let { shop ->
                db.shopDao().updateShop(shop.copy(productCount = db.productDao().getProductsByShop(shopId).size))
            }
        }
        ApiResponse(success = true, data = affected, message = "Bulk update applied")
    }

    override suspend fun duplicateProduct(id: String): ApiResponse<ProductDto> = withContext(Dispatchers.IO) {
        val existing = db.productDao().getProductById(id)
            ?: return@withContext ApiResponse(success = false, message = "Product not found")
        val copy = existing.copy(
            id = "prod_dup_${System.currentTimeMillis()}",
            name = "${existing.name} (Copy)",
            nameAmharic = if (existing.nameAmharic.isNotBlank()) "${existing.nameAmharic} (ቅጂ)" else "",
            nameAfaanOromo = if (existing.nameAfaanOromo.isNotBlank()) "${existing.nameAfaanOromo} (Garagalcha)" else "",
            status = "DRAFT",
            reviewCount = 0,
            soldCount = "New",
            isWishlisted = false
        )
        db.productDao().insertProduct(copy)
        db.shopDao().getShopById(existing.shopId)?.let { shop ->
            db.shopDao().updateShop(shop.copy(productCount = db.productDao().getProductsByShop(existing.shopId).size))
        }
        ApiResponse(success = true, data = copy.toDto(isWishlisted = false), message = "Product duplicated")
    }

    override suspend fun getSellerOrders(): ApiResponse<List<OrderDto>> = withContext(Dispatchers.IO) {
        val shopId = resolveActiveSellerShopId()
            ?: return@withContext ApiResponse(success = false, message = "No shop associated with current account", data = emptyList())
        val list = db.orderDao().getShopOrders(shopId).map { it.toDto() }
        ApiResponse(success = true, data = list)
    }

    override suspend fun updateSellerOrderStatus(orderId: String, status: String): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        val caller = requireSignedInUser()
            ?: return@withContext ApiResponse(success = false, data = false, message = "Permission denied.")
        val order = db.orderDao().getOrderById(orderId)
            ?: return@withContext ApiResponse(success = false, data = false, message = "Order not found")
        if (!caller.ownsShop(order.shopId)) {
            return@withContext ApiResponse(success = false, data = false, message = "Permission denied.")
        }
        val shop = db.shopDao().getShopById(order.shopId)
        val dispatchedAt = if (status in listOf("Dispatched", "Out for Delivery")) {
            order.dispatchedAt ?: getCurrentTime()
        } else order.dispatchedAt
        val deliveredAt = if (status.equals("Delivered", ignoreCase = true)) {
            order.deliveredAt?.takeIf { it.isNotBlank() } ?: getCurrentTime()
        } else order.deliveredAt
        val courierName = if (status in listOf("Dispatched", "Out for Delivery") && order.courierName.isBlank()) {
            "${order.shopName} Dispatch"
        } else order.courierName
        val courierPhone = if (status in listOf("Dispatched", "Out for Delivery") && order.courierPhone.isBlank()) {
            shop?.phone ?: order.customerPhone
        } else order.courierPhone

        db.orderDao().updateOrder(
            order.copy(
                orderStatus = status,
                dispatchedAt = dispatchedAt,
                deliveredAt = deliveredAt,
                courierName = courierName,
                courierPhone = courierPhone
            )
        )
        db.notificationDao().insert(
            NotificationEntity(
                id = "notif_${System.currentTimeMillis()}",
                userId = order.customerUserId,
                type = "ORDER",
                title = "Order #${order.orderReference} $status",
                body = "${order.shopName} updated your order status to $status.",
                timestamp = "Just now",
                isRead = false
            )
        )
        ApiResponse(success = true, data = true)
    }

    override suspend fun getSellerVerificationQueue(): ApiResponse<List<OrderDto>> = withContext(Dispatchers.IO) {
        val shopId = resolveActiveSellerShopId()
            ?: return@withContext ApiResponse(success = false, message = "No shop associated with current account", data = emptyList())
        val list = db.orderDao().getPendingVerificationOrders(shopId).map { it.toDto() }
        ApiResponse(success = true, data = list)
    }

    override suspend fun verifyPayment(orderId: String, payload: Map<String, String>): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        val order = db.orderDao().getOrderById(orderId)
            ?: return@withContext ApiResponse(success = false, message = "Order not found")

        val currentUser = resolveCurrentUser()
        val updated = order.copy(
            paymentStatus = "VERIFIED",
            orderStatus = "Confirmed",
            verifiedBy = currentUser?.name ?: "Shop Manager",
            verifiedAt = getCurrentTime(),
            verificationNotes = payload["notes"] ?: "Verified against receipt"
        )
        db.orderDao().updateOrder(updated)

        db.notificationDao().insert(
            NotificationEntity(
                id = "notif_${System.currentTimeMillis()}",
                userId = order.customerUserId,
                type = "PAYMENT",
                title = "Payment Verified!",
                body = "${order.shopName} has confirmed your payment for Order #${order.orderReference}. Preparing order now.",
                timestamp = "Just now",
                isRead = false
            )
        )
        ApiResponse(success = true, data = true)
    }

    override suspend fun rejectPayment(orderId: String, payload: Map<String, String>): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        val order = db.orderDao().getOrderById(orderId)
            ?: return@withContext ApiResponse(success = false, message = "Order not found")

        val reason = payload["reason"] ?: "Transaction reference could not be found or amount mismatched"
        val updated = order.copy(
            paymentStatus = "REJECTED",
            orderStatus = "Payment Rejected",
            rejectionReason = reason
        )
        db.orderDao().updateOrder(updated)

        db.notificationDao().insert(
            NotificationEntity(
                id = "notif_${System.currentTimeMillis()}",
                userId = order.customerUserId,
                type = "PAYMENT",
                title = "Payment Rejected - Resubmission Required",
                body = "${order.shopName} was unable to verify reference for Order #${order.orderReference}: $reason",
                timestamp = "Just now",
                isRead = false
            )
        )
        ApiResponse(success = true, data = true)
    }

    override suspend fun getShopPaymentMethods(shopId: String): ApiResponse<List<ShopPaymentMethodDto>> = withContext(Dispatchers.IO) {
        val targetShopId = shopId.ifBlank { resolveActiveSellerShopId().orEmpty() }
        val methods = if (targetShopId.isBlank()) emptyList() else db.shopPaymentMethodDao().getMethodsForShop(targetShopId).map { it.toDto() }
        ApiResponse(success = true, data = methods)
    }

    override suspend fun saveShopPaymentMethod(method: ShopPaymentMethodDto): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        val targetShopId = method.shopId.ifBlank { resolveActiveSellerShopId() }
            ?: return@withContext ApiResponse(success = false, message = "No shop found for payment method")
        db.shopPaymentMethodDao().insert(method.copy(shopId = targetShopId).toEntity())
        ApiResponse(success = true, data = true)
    }

    override suspend fun deleteShopPaymentMethod(id: String): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        db.shopPaymentMethodDao().deleteById(id)
        ApiResponse(success = true, data = true)
    }

    override suspend fun getShopDeliveryOptions(shopId: String): ApiResponse<List<ShopDeliveryOptionDto>> = withContext(Dispatchers.IO) {
        val targetShopId = shopId.ifBlank { resolveActiveSellerShopId().orEmpty() }
        val options = if (targetShopId.isBlank()) emptyList() else db.shopDeliveryOptionDao().getOptionsForShop(targetShopId).map { it.toDto() }
        ApiResponse(success = true, data = options)
    }

    override suspend fun saveShopDeliveryOption(option: ShopDeliveryOptionDto): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        val targetShopId = option.shopId.ifBlank { resolveActiveSellerShopId() }
            ?: return@withContext ApiResponse(success = false, message = "No shop found for delivery option")
        db.shopDeliveryOptionDao().insert(
            ShopDeliveryOptionEntity(
                id = option.id.ifBlank { "del_${System.currentTimeMillis()}" },
                shopId = targetShopId,
                optionType = option.optionType,
                title = option.title,
                fee = option.fee,
                estimatedTime = option.estimatedTime,
                isEnabled = option.isEnabled
            )
        )
        ApiResponse(success = true, data = true)
    }

    override suspend fun deleteShopDeliveryOption(id: String): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        db.shopDeliveryOptionDao().deleteById(id)
        ApiResponse(success = true, data = true)
    }

    override suspend fun getShopPromotions(shopId: String, includeInactive: Boolean): ApiResponse<List<PromoCodeDto>> = withContext(Dispatchers.IO) {
        db.ensureSeeded()
        val targetShopId = shopId.ifBlank { resolveActiveSellerShopId().orEmpty() }
        val list = if (targetShopId.isBlank()) emptyList() else {
            val entities = if (includeInactive) db.promoCodeDao().getAllPromosForShop(targetShopId)
            else db.promoCodeDao().getActivePromosForShop(targetShopId)
            entities.map { it.toDto() }
        }
        ApiResponse(success = true, data = list)
    }

    override suspend fun createPromoCode(promo: PromoCodeDto): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        val caller = requireSignedInUser()
            ?: return@withContext ApiResponse(success = false, data = false, message = "Permission denied.")
        val targetShopId = promo.shopId.ifBlank { resolveActiveSellerShopId() }
            ?: return@withContext ApiResponse(success = false, data = false, message = "No shop found for promotion")
        if (!caller.ownsShop(targetShopId)) {
            return@withContext ApiResponse(success = false, data = false, message = "Permission denied.")
        }
        db.promoCodeDao().insert(promo.copy(shopId = targetShopId).toEntity())
        ApiResponse(success = true, data = true)
    }

    override suspend fun deletePromoCode(id: String): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        val caller = requireSignedInUser()
            ?: return@withContext ApiResponse(success = false, data = false, message = "Permission denied.")
        val existing = db.promoCodeDao().getPromoById(id)
        if (existing != null && !caller.ownsShop(existing.shopId)) {
            return@withContext ApiResponse(success = false, data = false, message = "Permission denied.")
        }
        db.promoCodeDao().deleteById(id)
        ApiResponse(success = true, data = true)
    }

    override suspend fun submitSellerVerification(details: Map<String, String>): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        val shopId = resolveActiveSellerShopId()
            ?: return@withContext ApiResponse(success = false, data = false, message = "No shop associated with account")
        val shop = db.shopDao().getShopById(shopId)
        if (shop != null) {
            val existing = VerificationDocsCodec.parse(shop.verificationDocsJson)
            val mergedDocs = existing.docs.toMutableMap()
            VerificationDocsCodec.docSlots.forEach { slot ->
                val incoming = details[slot.key]?.trim()
                if (!incoming.isNullOrBlank()) {
                    mergedDocs[slot.key] = incoming
                }
            }
            val now = VerificationDocsCodec.nowLabel()
            val wasResubmission = shop.verificationStatus in listOf("REJECTED", "CHANGES_REQUESTED")
            val nextStatus = if (wasResubmission) "RESUBMITTED" else "PENDING_REVIEW"
            val updatedSubmission = existing.copy(
                docs = mergedDocs,
                licenseRef = details["license"]?.trim().orEmpty().ifBlank { existing.licenseRef },
                tin = details["tin"]?.trim().orEmpty().ifBlank { existing.tin },
                submittedAt = now,
                failedKeys = emptyList(),
                history = existing.history + VerificationHistoryEntry(
                    status = nextStatus,
                    at = now,
                    note = if (wasResubmission) "Seller resubmitted updated verification documents" else "Submitted verification documents for review"
                )
            )
            db.shopDao().updateShop(
                shop.copy(
                    verificationStatus = nextStatus,
                    verificationDocsJson = VerificationDocsCodec.toJson(updatedSubmission),
                    verificationRejectionReason = null
                )
            )
            ApiResponse(success = true, data = true)
        } else {
            ApiResponse(success = false, data = false)
        }
    }

    override suspend fun updateShopSettings(settings: Map<String, Any>): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        val shopId = (settings["shopId"] as? String)?.takeIf { it.isNotBlank() } ?: resolveActiveSellerShopId()
            ?: return@withContext ApiResponse(success = false, data = false, message = "No shop associated with account")
        val shop = db.shopDao().getShopById(shopId)
        if (shop != null) {
            val updated = shop.copy(
                name = settings["name"] as? String ?: shop.name,
                category = settings["category"] as? String ?: shop.category,
                description = settings["description"] as? String ?: shop.description,
                phone = settings["phone"] as? String ?: shop.phone,
                telegramLink = settings["telegramLink"] as? String ?: shop.telegramLink,
                whatsappLink = settings["whatsappLink"] as? String ?: shop.whatsappLink,
                logoUrl = settings["logoUrl"] as? String ?: shop.logoUrl,
                coverUrl = settings["coverUrl"] as? String ?: shop.coverUrl,
                isVacationMode = settings["isVacationMode"] as? Boolean ?: shop.isVacationMode,
                acceptsOrders = settings["acceptsOrders"] as? Boolean ?: shop.acceptsOrders,
                requireReceiptScreenshot = settings["requireReceiptScreenshot"] as? Boolean ?: shop.requireReceiptScreenshot,
                vacationReturnDate = settings["vacationReturnDate"] as? String ?: shop.vacationReturnDate,
                vacationNote = settings["vacationNote"] as? String ?: shop.vacationNote,
                freeDeliveryThreshold = (settings["freeDeliveryThreshold"] as? Number)?.toDouble() ?: shop.freeDeliveryThreshold,
                coverageSubCitiesCsv = settings["coverageSubCitiesCsv"] as? String ?: shop.coverageSubCitiesCsv,
                businessHoursJson = settings["businessHoursJson"] as? String ?: shop.businessHoursJson,
                galleryJson = settings["galleryJson"] as? String ?: shop.galleryJson,
                instagramLink = settings["instagramLink"] as? String ?: shop.instagramLink,
                tiktokLink = settings["tiktokLink"] as? String ?: shop.tiktokLink,
                websiteUrl = settings["websiteUrl"] as? String ?: shop.websiteUrl,
                returnPolicy = settings["returnPolicy"] as? String ?: shop.returnPolicy,
                deliveryNotesPolicy = settings["deliveryNotesPolicy"] as? String ?: shop.deliveryNotesPolicy,
                standardCutoffTime = settings["standardCutoffTime"] as? String ?: shop.standardCutoffTime,
                expressCutoffTime = settings["expressCutoffTime"] as? String ?: shop.expressCutoffTime
            )
            db.shopDao().updateShop(updated)
            ApiResponse(success = true, data = true)
        } else {
            ApiResponse(success = false, data = false)
        }
    }

    override suspend fun getChatConversations(): ApiResponse<List<ChatMessageDto>> = withContext(Dispatchers.IO) {
        val current = resolveCurrentUser()
            ?: return@withContext ApiResponse(success = true, data = emptyList())
        val isSellerMode = current.activeRole.equals("SELLER", ignoreCase = true) ||
            (current.activeRole.isBlank() && current.role.equals("SELLER", ignoreCase = true))
        val sellerShopId = if (isSellerMode) resolveActiveSellerShopId() else null
        val messages = if (!sellerShopId.isNullOrBlank()) {
            db.chatDao().getMessagesForShopDesc(sellerShopId)
        } else {
            db.chatDao().getMessagesForCustomerDesc(current.id)
        }
        val list = messages.distinctBy { it.conversationId }.map { it.toDto() }
        ApiResponse(success = true, data = list)
    }

    override suspend fun getChatMessages(shopId: String, conversationId: String?): ApiResponse<List<ChatMessageDto>> = withContext(Dispatchers.IO) {
        val ctx = resolveChatContext(shopId = shopId, explicitConvId = conversationId)
        val list = db.chatDao().getMessages(ctx.conversationId).map { it.toDto() }
        ApiResponse(success = true, data = list)
    }

    override suspend fun sendChatMessage(shopId: String, messageData: Map<String, Any>): ApiResponse<ChatMessageDto> = withContext(Dispatchers.IO) {
        val explicitConvId = (messageData["conversationId"] as? String)?.trim()?.takeIf { it.isNotBlank() }
        val senderType = (messageData["senderType"] as? String)?.takeIf { it.isNotBlank() } ?: "CUSTOMER"
        val ctx = resolveChatContext(shopId = shopId, explicitConvId = explicitConvId, senderTypeHint = senderType)
        val customerUserId = (messageData["customerUserId"] as? String)?.trim()?.takeIf { it.isNotBlank() }
            ?: ctx.customerUserId
        val text = (messageData["message"] as? String)?.trim().orEmpty()
        val imageUrl = (messageData["imageUrl"] as? String)?.trim().orEmpty()
        val orderRef = (messageData["orderContextRef"] as? String)?.trim()?.takeIf { it.isNotBlank() }
        val orderStatus = (messageData["orderContextStatus"] as? String)?.trim()?.takeIf { it.isNotBlank() }
        val orderTotal = (messageData["orderContextTotal"] as? String)?.trim()?.takeIf { it.isNotBlank() }
        val threadCategory = (messageData["threadCategory"] as? String)?.takeIf { it.isNotBlank() }
            ?: if (orderRef != null) "ORDER" else "QUESTION"
        if (text.isBlank() && imageUrl.isBlank() && orderRef == null) {
            return@withContext ApiResponse(success = false, message = "Message cannot be empty")
        }
        val shop = db.shopDao().getShopById(shopId)

        val msg = ChatMessageEntity(
            id = "msg_${System.currentTimeMillis()}",
            conversationId = ctx.conversationId,
            shopId = shopId,
            shopName = shop?.name ?: "Shop",
            senderType = senderType,
            message = text.ifBlank { if (orderRef != null) "Order #$orderRef" else "Sent a photo" },
            timestamp = "Just now",
            productContextName = messageData["productContextName"] as? String,
            productContextPrice = messageData["productContextPrice"] as? String,
            productContextImage = messageData["productContextImage"] as? String,
            isRead = true,
            imageUrl = imageUrl,
            orderContextRef = orderRef,
            orderContextStatus = orderStatus,
            orderContextTotal = orderTotal,
            threadCategory = threadCategory,
            customerUserId = customerUserId
        )
        db.chatDao().insert(msg)
        ApiResponse(success = true, data = msg.toDto())
    }

    override suspend fun markChatConversationRead(shopId: String, isRead: Boolean, conversationId: String?): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        val ctx = resolveChatContext(shopId = shopId, explicitConvId = conversationId)
        db.chatDao().setConversationRead(ctx.conversationId, isRead)
        ApiResponse(success = true, data = true)
    }

    override suspend fun getReviews(targetId: String): ApiResponse<List<ReviewDto>> = withContext(Dispatchers.IO) {
        val currentUserId = resolveCurrentUser()?.id ?: ""
        val hiddenIds = db.flaggedReviewDao().getAllFlagged()
            .filter { it.status == "HIDDEN" && it.reviewId.isNotBlank() }
            .map { it.reviewId }
            .toSet()
        val list = db.reviewDao().getReviewsForTarget(targetId)
            .filter { it.id !in hiddenIds }
            .map { it.toDto(currentUserId) }
        ApiResponse(success = true, data = list)
    }

    override suspend fun submitReview(reviewData: Map<String, Any>): ApiResponse<ReviewDto> = withContext(Dispatchers.IO) {
        val currentUser = resolveCurrentUser()
        val userId = currentUser?.id ?: "user_guest"
        val targetId = reviewData["targetId"] as String
        val targetType = reviewData["targetType"] as? String ?: "PRODUCT"
        val rating = (reviewData["rating"] as Number).toDouble().coerceIn(1.0, 5.0)
        val comment = (reviewData["comment"] as? String)?.trim() ?: ""
        @Suppress("UNCHECKED_CAST")
        val tags = (reviewData["tags"] as? List<*>)?.mapNotNull { it as? String }?.filter { it.isNotBlank() }.orEmpty()
        @Suppress("UNCHECKED_CAST")
        val photos = (reviewData["photos"] as? List<*>)?.mapNotNull { it as? String }?.filter { it.isNotBlank() }.orEmpty()
        val isAnonymous = reviewData["isAnonymous"] as? Boolean ?: false

        if (comment.isBlank()) {
            return@withContext ApiResponse(success = false, message = "Review comment cannot be empty.")
        }

        val confirmedOrders = db.orderDao().getEligibleOrdersForCustomer(userId)
        val isVerifiedBuyer = when (targetType) {
            "PRODUCT" -> confirmedOrders.any { it.itemsJson.contains(targetId) }
            "SHOP" -> confirmedOrders.any { it.shopId == targetId }
            else -> confirmedOrders.isNotEmpty()
        }

        val photosJsonArr = JSONArray().apply { photos.forEach { put(it) } }.toString()
        val review = ReviewEntity(
            id = "rev_${System.currentTimeMillis()}",
            userId = userId,
            userName = currentUser?.name ?: "Customer",
            userAvatar = currentUser?.avatarUrl ?: "",
            targetType = targetType,
            targetId = targetId,
            rating = rating,
            comment = comment,
            isVerifiedPurchase = isVerifiedBuyer,
            date = "Just now",
            tagsCsv = tags.joinToString(","),
            photosJson = photosJsonArr,
            isAnonymous = isAnonymous
        )
        db.reviewDao().insert(review)

        if (targetType == "PRODUCT") {
            val prod = db.productDao().getProductById(targetId)
            if (prod != null) {
                val newCount = prod.reviewCount + 1
                val newRating = ((prod.rating * prod.reviewCount) + rating) / newCount
                val roundedRating = Math.round(newRating * 10.0) / 10.0
                db.productDao().updateProduct(prod.copy(rating = roundedRating, reviewCount = newCount))
            }
        } else if (targetType == "SHOP") {
            val shop = db.shopDao().getShopById(targetId)
            if (shop != null) {
                val newCount = shop.reviewCount + 1
                val newRating = ((shop.rating * shop.reviewCount) + rating) / newCount
                val roundedRating = Math.round(newRating * 10.0) / 10.0
                db.shopDao().updateShop(shop.copy(rating = roundedRating, reviewCount = newCount))
            }
        }

        ApiResponse(success = true, data = review.toDto(userId))
    }

    override suspend fun getNotifications(): ApiResponse<List<NotificationDto>> = withContext(Dispatchers.IO) {
        val currentUser = resolveCurrentUser()
            ?: return@withContext ApiResponse(success = true, data = emptyList())
        val list = db.notificationDao().getNotifications(currentUser.id).map { it.toDto() }
        ApiResponse(success = true, data = list)
    }

    override suspend fun markNotificationRead(id: String, isRead: Boolean): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        db.notificationDao().setRead(id, isRead)
        ApiResponse(success = true, data = true)
    }

    override suspend fun markAllNotificationsRead(): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        val currentUser = resolveCurrentUser() ?: return@withContext ApiResponse(success = true, data = true)
        db.notificationDao().markAllRead(currentUser.id)
        ApiResponse(success = true, data = true)
    }

    override suspend fun deleteNotification(id: String): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        db.notificationDao().deleteById(id)
        ApiResponse(success = true, data = true)
    }

    override suspend fun getSavedAddresses(): ApiResponse<List<SavedAddressDto>> = withContext(Dispatchers.IO) {
        val currentUser = resolveCurrentUser()
            ?: return@withContext ApiResponse(success = true, data = emptyList())
        val list = db.savedAddressDao().getAddresses(currentUser.id).map { it.toDto() }
        ApiResponse(success = true, data = list)
    }

    override suspend fun saveAddress(address: SavedAddressDto): ApiResponse<SavedAddressDto> = withContext(Dispatchers.IO) {
        val currentUser = resolveCurrentUser()
            ?: return@withContext ApiResponse(success = false, message = "Sign in to save delivery addresses.")
        val userId = currentUser.id
        if (address.isDefault) {
            db.savedAddressDao().clearDefaultForUser(userId)
        }
        val entity = SavedAddressEntity(
            id = if (address.id.isBlank()) "addr_${System.currentTimeMillis()}" else address.id,
            userId = userId,
            fullName = address.fullName,
            phone = address.phone,
            region = address.region,
            city = address.city,
            subCity = address.subCity,
            streetOrLandmark = address.streetOrLandmark,
            deliveryInstructions = address.deliveryInstructions,
            isDefault = address.isDefault
        )
        db.savedAddressDao().insert(entity)
        if (entity.isDefault) {
            db.userDao().updateUser(currentUser.copy(defaultAddressId = entity.id))
        }
        ApiResponse(success = true, data = entity.toDto())
    }

    override suspend fun deleteSavedAddress(id: String): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        val currentUser = resolveCurrentUser() ?: return@withContext ApiResponse(success = false, data = false)
        val addresses = db.savedAddressDao().getAddresses(currentUser.id)
        val target = addresses.firstOrNull { it.id == id }
            ?: return@withContext ApiResponse(success = false, data = false, message = "Address not found")
        val activeOrders = db.orderDao().getCustomerOrders(currentUser.id)
            .filter { it.orderStatus !in listOf("Delivered", "Cancelled") }
        val inUse = activeOrders.any { it.deliveryAddress.contains(target.streetOrLandmark, ignoreCase = true) }
        if (inUse) {
            return@withContext ApiResponse(
                success = false,
                data = false,
                message = "Cannot delete an address currently used by an active order."
            )
        }
        db.savedAddressDao().deleteById(id)
        ApiResponse(success = true, data = true)
    }

    override suspend fun submitSupportTicket(ticket: SupportTicketDto): ApiResponse<SupportTicketDto> = withContext(Dispatchers.IO) {
        val user = resolveCurrentUser()
        val now = getCurrentTime()
        val entity = SupportTicketEntity(
            id = ticket.id.ifBlank { "tkt_${System.currentTimeMillis()}" },
            userId = user?.id ?: ticket.userId.ifBlank { "user_guest" },
            orderId = ticket.orderId,
            category = ticket.category,
            subject = ticket.subject.trim(),
            description = ticket.description.trim(),
            status = "OPEN",
            createdAt = now,
            priority = ticket.priority.ifBlank { "NORMAL" },
            requesterRole = if (user?.activeRole.equals("SELLER", true)) "SELLER" else ticket.requesterRole.ifBlank { "BUYER" },
            updatedAt = now
        )
        db.supportTicketDao().insert(entity)
        ApiResponse(success = true, data = entity.toDto())
    }

    override suspend fun getSupportTickets(): ApiResponse<List<SupportTicketDto>> = withContext(Dispatchers.IO) {
        val user = resolveCurrentUser()
        val list = if (user?.role == "ADMIN") db.supportTicketDao().getAllTickets()
        else db.supportTicketDao().getTicketsForUser(user?.id ?: "user_guest")
        ApiResponse(success = true, data = list.map { it.toDto() })
    }

    override suspend fun getAdminSettings(): ApiResponse<AdminSettingsDto> = withContext(Dispatchers.IO) {
        db.ensureSeeded()
        val settings = db.adminSettingsDao().getSettings() ?: AdminSettingsEntity()
        val parsedOrder = settings.homeSectionsOrderCsv
            .split(",")
            .map { it.trim() }
            .filter { it in HomeSectionKeys.defaultOrder }
        val normalizedOrder = (parsedOrder + HomeSectionKeys.defaultOrder).distinct()
        val hidden = settings.hiddenHomeSectionsCsv
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        ApiResponse(
            success = true,
            data = AdminSettingsDto(
                isChatEnabled = settings.isChatEnabled,
                isExternalContactEnabled = settings.isExternalContactEnabled,
                isSubscriptionModeEnabled = settings.isSubscriptionModeEnabled,
                sellerReviewsEnabled = settings.sellerReviewsEnabled,
                showFeaturedShops = settings.showFeaturedShops,
                activePlanName = settings.activePlanName,
                activePlanPrice = settings.activePlanPrice,
                homeSectionsOrder = normalizedOrder,
                hiddenHomeSections = hidden,
                isPilotMode = settings.isPilotMode,
                pilotEndDate = settings.pilotEndDate
            )
        )
    }

    override suspend fun updateFeatureToggle(feature: String, enabled: Boolean): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        val current = db.adminSettingsDao().getSettings() ?: AdminSettingsEntity()
        val updated = when (feature) {
            "chat" -> current.copy(isChatEnabled = enabled)
            "contact" -> current.copy(isExternalContactEnabled = enabled)
            "subscription" -> current.copy(isSubscriptionModeEnabled = enabled)
            "reviews" -> current.copy(sellerReviewsEnabled = enabled)
            "featuredShops" -> current.copy(showFeaturedShops = enabled)
            else -> current
        }
        db.adminSettingsDao().insert(updated)
        ApiResponse(success = true, data = true)
    }

    override suspend fun getAdminSellerVerificationQueue(): ApiResponse<List<ShopDto>> = withContext(Dispatchers.IO) {
        val pending = db.shopDao().getAllShops().filter {
            it.verificationStatus in listOf("PENDING_REVIEW", "RESUBMITTED", "CHANGES_REQUESTED")
        }
        val followedIds = currentFollowedShopIds()
        ApiResponse(success = true, data = pending.map { it.toDto(isFollowing = it.id in followedIds) })
    }

    override suspend fun adminVerifySellerDecision(
        shopId: String,
        decision: String,
        reason: String?,
        failedKeys: String?
    ): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        val shop = db.shopDao().getShopById(shopId)
        if (shop != null) {
            val parsed = VerificationDocsCodec.parse(shop.verificationDocsJson)
            val now = VerificationDocsCodec.nowLabel()
            val normalizedDecision = decision.uppercase()
            val isApproved = normalizedDecision == "APPROVE"
            val nextStatus = when (normalizedDecision) {
                "APPROVE" -> "VERIFIED"
                "REQUEST_CHANGES" -> "CHANGES_REQUESTED"
                else -> "REJECTED"
            }
            val failedList = failedKeys?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }.orEmpty()
            val cleanedReason = reason?.trim().orEmpty()
            val historyNote = when (normalizedDecision) {
                "APPROVE" -> cleanedReason.ifBlank { "Verified by platform admin" }
                "REQUEST_CHANGES" -> cleanedReason.ifBlank { "Changes requested on selected verification items" }
                else -> cleanedReason.ifBlank { "Verification rejected by platform admin" }
            }
            val updatedSubmission = parsed.copy(
                failedKeys = if (isApproved) emptyList() else failedList,
                history = parsed.history + VerificationHistoryEntry(
                    status = nextStatus,
                    at = now,
                    note = historyNote,
                    failedKeys = if (isApproved) emptyList() else failedList
                )
            )
            db.shopDao().updateShop(
                shop.copy(
                    isVerified = isApproved,
                    verificationStatus = nextStatus,
                    verificationDocsJson = VerificationDocsCodec.toJson(updatedSubmission),
                    verificationRejectionReason = if (isApproved) null else cleanedReason.ifBlank { historyNote }
                )
            )
            ApiResponse(success = true, data = true)
        } else {
            ApiResponse(success = false, data = false)
        }
    }

    override suspend fun saveCategory(category: CategoryDto): ApiResponse<CategoryDto> = withContext(Dispatchers.IO) {
        val catId = category.id.ifBlank { "cat_${System.currentTimeMillis()}" }
        val resolvedImage = category.imageUrl.trim().ifBlank { MarketplaceCategories.imageUrlForId(catId) }
        val entity = CategoryEntity(
            id = catId,
            name = category.name.trim(),
            nameAmharic = category.nameAmharic.trim(),
            nameAfaanOromo = category.nameAfaanOromo.trim(),
            icon = category.icon.ifBlank { "sparkles" },
            imageUrl = resolvedImage,
            subCategoriesCsv = category.subCategories.filter { it.isNotBlank() }.joinToString(","),
            isActive = category.isActive,
            sortOrder = category.sortOrder,
            linkedItemTypesCsv = category.linkedItemTypes.filter { it.isNotBlank() }.joinToString(",")
        )
        db.categoryDao().insert(entity)
        ApiResponse(success = true, data = entity.toDto())
    }

    override suspend fun reorderCategories(orderedIds: List<String>): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        val all = db.categoryDao().getAllCategories().associateBy { it.id }.toMutableMap()
        val reordered = orderedIds.mapIndexedNotNull { idx, id ->
            all.remove(id)?.copy(sortOrder = idx)
        } + all.values.sortedBy { it.sortOrder }.mapIndexed { extraIdx, c ->
            c.copy(sortOrder = orderedIds.size + extraIdx)
        }
        db.categoryDao().insertAll(reordered)
        ApiResponse(success = true, data = true)
    }

    override suspend fun deleteCategory(id: String): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        db.categoryDao().deleteById(id)
        ApiResponse(success = true, data = true)
    }

    override suspend fun savePromoBanner(banner: PromoBannerDto): ApiResponse<PromoBannerDto> = withContext(Dispatchers.IO) {
        val entity = PromoBannerEntity(
            id = banner.id.ifBlank { "banner_${System.currentTimeMillis()}" },
            badge = banner.badge.trim(),
            title = banner.title.trim(),
            highlight = banner.highlight.trim(),
            subtitle = banner.subtitle.trim(),
            buttonText = banner.buttonText.trim().ifBlank { "Shop Now" },
            imageUrl = banner.imageUrl.trim(),
            targetCategory = banner.targetCategory,
            targetName = banner.targetName,
            isActive = banner.isActive,
            sortOrder = banner.sortOrder,
            badgeIcon = banner.badgeIcon.ifBlank { "sparkles" },
            destinationType = banner.destinationType.ifBlank { "CATEGORY" },
            targetId = banner.targetId.ifBlank { banner.targetCategory },
            startDate = banner.startDate.trim(),
            endDate = banner.endDate.trim(),
            audience = banner.audience.ifBlank { "ALL" }
        )
        db.promoBannerDao().insert(entity)
        ApiResponse(success = true, data = entity.toDto())
    }

    override suspend fun reorderPromoBanners(orderedIds: List<String>): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        val all = db.promoBannerDao().getAllBanners().associateBy { it.id }.toMutableMap()
        val reordered = orderedIds.mapIndexedNotNull { idx, id ->
            all.remove(id)?.copy(sortOrder = idx)
        } + all.values.sortedBy { it.sortOrder }.mapIndexed { extraIdx, b ->
            b.copy(sortOrder = orderedIds.size + extraIdx)
        }
        db.promoBannerDao().insertAll(reordered)
        ApiResponse(success = true, data = true)
    }

    override suspend fun deletePromoBanner(id: String): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        db.promoBannerDao().deleteById(id)
        ApiResponse(success = true, data = true)
    }

    override suspend fun updateHomeSectionsConfig(payload: Map<String, List<String>>): ApiResponse<AdminSettingsDto> = withContext(Dispatchers.IO) {
        db.ensureSeeded()
        val current = db.adminSettingsDao().getSettings() ?: AdminSettingsEntity()
        val order = payload["order"]?.filter { it in HomeSectionKeys.defaultOrder }.orEmpty()
        val normalizedOrder = (order + HomeSectionKeys.defaultOrder).distinct()
        val hidden = payload["hidden"]?.filter { it in HomeSectionKeys.defaultOrder }.orEmpty().distinct()
        val updated = current.copy(
            homeSectionsOrderCsv = normalizedOrder.joinToString(","),
            hiddenHomeSectionsCsv = hidden.joinToString(",")
        )
        db.adminSettingsDao().insert(updated)
        getAdminSettings()
    }

    private suspend fun syncPersistedItemTypes() {
        val persisted = db.itemTypeDao().getAllItemTypes()
        if (persisted.isNotEmpty()) {
            ProductTemplates.syncFromPersisted(
                persisted.map {
                    ItemType(
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
    }

    override suspend fun getItemTypes(): ApiResponse<List<ItemTypeDto>> = withContext(Dispatchers.IO) {
        db.ensureSeeded()
        val list = db.itemTypeDao().getAllItemTypes().map { it.toDto() }
        ApiResponse(success = true, data = list)
    }

    override suspend fun saveItemType(itemType: ItemTypeDto): ApiResponse<ItemTypeDto> = withContext(Dispatchers.IO) {
        db.ensureSeeded()
        val resolvedId = itemType.id.ifBlank {
            val slug = itemType.label.lowercase(Locale.US).replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "type" }
            "${slug}_${System.currentTimeMillis() % 10000}"
        }
        val entity = ItemTypeEntity(
            id = resolvedId,
            label = itemType.label.trim(),
            emoji = itemType.emoji.ifBlank { "✨" },
            categoryId = itemType.categoryId,
            groupName = itemType.group.ifBlank { "Other" },
            attributesJson = attributesToJson(itemType.attributes),
            sortOrder = itemType.sortOrder
        )
        db.itemTypeDao().insert(entity)
        syncPersistedItemTypes()
        ApiResponse(success = true, data = entity.toDto())
    }

    override suspend fun deleteItemType(id: String): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        db.itemTypeDao().deleteById(id)
        syncPersistedItemTypes()
        ApiResponse(success = true, data = true)
    }

    override suspend fun resolveSupportTicket(id: String): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        val caller = requireSignedInUser()
        if (caller == null || !caller.isAdmin()) {
            return@withContext ApiResponse(success = false, data = false, message = "Admin permission required.")
        }
        db.supportTicketDao().updateStatus(id, "RESOLVED")
        ApiResponse(success = true, data = true)
    }

    override suspend fun sendAdminBroadcast(payload: Map<String, String>): ApiResponse<Int> = withContext(Dispatchers.IO) {
        val caller = requireSignedInUser()
        if (caller == null || !caller.isAdmin()) {
            return@withContext ApiResponse(success = false, message = "Admin permission required.", data = 0)
        }
        val title = payload["title"]?.trim().orEmpty()
        val body = payload["body"]?.trim().orEmpty()
        val audience = payload["audience"]?.trim().orEmpty().ifBlank { "ALL" }
        val imageUrl = payload["imageUrl"]?.trim().orEmpty()
        val destinationType = payload["destinationType"]?.trim().orEmpty().ifBlank { "NONE" }
        val targetId = payload["targetId"]?.trim().orEmpty()
        val scheduledAt = payload["scheduledAt"]?.trim().orEmpty()
        val isTemplate = payload["isTemplate"]?.equals("true", ignoreCase = true) == true
        val testToSelf = payload["testToSelf"]?.equals("true", ignoreCase = true) == true

        if (title.isBlank() || body.isBlank()) {
            return@withContext ApiResponse(success = false, message = "Notification title and message are required.", data = 0)
        }
        val allUsers = db.userDao().getAllUsers()
        val currentUser = resolveCurrentUser()
        val recipients = when {
            isTemplate -> emptyList()
            testToSelf -> listOfNotNull(currentUser)
            audience == "ADDIS_ONLY" -> allUsers.filter { it.city.equals("Addis Ababa", ignoreCase = true) || it.region.equals("Addis Ababa", ignoreCase = true) }
            audience.startsWith("CITY:") -> {
                val city = audience.removePrefix("CITY:").trim()
                allUsers.filter { it.city.equals(city, ignoreCase = true) || it.region.equals(city, ignoreCase = true) }
            }
            audience.startsWith("FOLLOWERS:") -> {
                val shopId = audience.removePrefix("FOLLOWERS:").trim()
                allUsers.filter { u -> db.shopFollowDao().isFollowing(u.id, shopId) }
            }
            audience == "BUYERS" -> allUsers.filter { it.role == "CUSTOMER" }
            audience == "SELLERS" -> allUsers.filter { it.role == "SELLER" || !it.shopId.isNullOrBlank() }
            else -> allUsers
        }.ifEmpty { if (isTemplate) emptyList() else listOfNotNull(currentUser) }

        val now = getCurrentTime()
        if (!isTemplate && scheduledAt.isBlank()) {
            recipients.forEachIndexed { idx, user ->
                db.notificationDao().insert(
                    NotificationEntity(
                        id = "notif_bc_${System.currentTimeMillis()}_$idx",
                        userId = user.id,
                        type = "PROMO",
                        title = title,
                        body = body,
                        timestamp = now,
                        isRead = false
                    )
                )
            }
        }
        val status = when {
            isTemplate -> "TEMPLATE"
            testToSelf -> "TEST"
            scheduledAt.isNotBlank() -> "SCHEDULED"
            else -> "SENT"
        }
        db.adminBroadcastDao().insert(
            AdminBroadcastEntity(
                id = "bc_${System.currentTimeMillis()}",
                title = title,
                body = body,
                audience = audience,
                imageUrl = imageUrl,
                destinationType = destinationType,
                targetId = targetId,
                scheduledAt = scheduledAt,
                status = status,
                sentCount = recipients.size,
                openedCount = 0,
                isTemplate = isTemplate,
                createdAt = now
            )
        )
        ApiResponse(success = true, data = recipients.size)
    }

    override suspend fun getOwnerDashboardMetrics(rangeDays: Int): ApiResponse<OwnerDashboardMetricsDto> = withContext(Dispatchers.IO) {
        db.ensureSeeded()
        val nowMillis = System.currentTimeMillis()
        val dayMs = 86_400_000L

        val orders = db.orderDao().getAllOrders()
        val shops = db.shopDao().getAllShops()
        val users = db.userDao().getAllUsers()
        val products = db.productDao().getAllProducts()
        val categories = db.categoryDao().getAllCategories()
        val flaggedReviews = db.flaggedReviewDao().getAllFlagged()
        val tickets = db.supportTicketDao().getAllTickets()
        val adminSettings = db.adminSettingsDao().getSettings() ?: AdminSettingsEntity()

        val validOrders = orders.filter { !it.orderStatus.equals("Cancelled", ignoreCase = true) }
        val orderTimestamps = orders.associate { o ->
            o.id to (EthiopianMarketplaceUtils.resolveTimestampMillis(o.createdAt, nowMillis) ?: nowMillis)
        }
        val userTimestamps = users.associate { u ->
            u.id to EthiopianMarketplaceUtils.resolveTimestampMillis(u.createdAt, nowMillis)
        }
        val shopTimestamps = shops.associate { s ->
            val raw = s.createdAt.ifBlank { VerificationDocsCodec.parse(s.verificationDocsJson).submittedAt }
            s.id to EthiopianMarketplaceUtils.resolveTimestampMillis(raw, nowMillis)
        }

        val isAllRange = rangeDays <= 0 || rangeDays >= 365
        val windowMs = if (isAllRange) {
            val earliestOrderTs = orders.mapNotNull { orderTimestamps[it.id] }.minOrNull() ?: (nowMillis - 90L * dayMs)
            (nowMillis - earliestOrderTs).coerceAtLeast(30L * dayMs)
        } else {
            rangeDays.toLong() * dayMs
        }
        val currentStart = nowMillis - windowMs
        val prevStart = currentStart - windowMs

        val currentOrders = if (isAllRange) {
            validOrders
        } else {
            validOrders.filter { (orderTimestamps[it.id] ?: nowMillis) in currentStart..nowMillis }
        }
        val previousOrders = if (isAllRange) {
            emptyList()
        } else {
            validOrders.filter {
                val ts = orderTimestamps[it.id] ?: nowMillis
                ts >= prevStart && ts < currentStart
            }
        }
        val allOrdersInWindow = if (isAllRange) {
            orders
        } else {
            orders.filter { (orderTimestamps[it.id] ?: nowMillis) in currentStart..nowMillis }
        }

        val rangeGmv = currentOrders.sumOf { it.totalAmount }
        val prevGmv = previousOrders.sumOf { it.totalAmount }

        val commissionRules = db.commissionConfigDao().getAll()
        val defaultCommissionRate = commissionRules.firstOrNull { it.categoryId == "DEFAULT" }?.ratePercent ?: 5.0
        val commissionRevenue = currentOrders.sumOf { o ->
            o.subtotal * (defaultCommissionRate / 100.0)
        }
        val approvedUpgrades = db.subscriptionUpgradeDao().getAll().filter { it.status == "APPROVED" }
        val subscriptionRevenue = when {
            approvedUpgrades.isNotEmpty() -> approvedUpgrades.sumOf { it.amount }
            adminSettings.isSubscriptionModeEnabled -> {
                val proShops = shops.count { it.isVerified }
                val monthFactor = if (isAllRange || rangeDays >= 30) 1.0 else (rangeDays.toDouble() / 30.0)
                proShops * adminSettings.activePlanPrice * monthFactor
            }
            else -> 0.0
        }
        val platformRevenue = commissionRevenue + subscriptionRevenue

        // Real chronological buckets for GMV & sparklines
        val bucketCount = when {
            rangeDays == 7 -> 7
            rangeDays == 30 -> 4
            rangeDays == 90 -> 3
            else -> 4
        }
        val bucketSpanMs = (windowMs / bucketCount).coerceAtLeast(1L)
        val dayLabelFmt = SimpleDateFormat("EEE", Locale.US)
        val monthLabelFmt = SimpleDateFormat("MMM", Locale.US)
        val dateLabelFmt = SimpleDateFormat("MMM d", Locale.US)

        val bucketLabels = (0 until bucketCount).map { idx ->
            val midMs = currentStart + idx * bucketSpanMs + bucketSpanMs / 2L
            when {
                rangeDays == 7 -> dayLabelFmt.format(Date(midMs))
                rangeDays == 30 -> "W${idx + 1}"
                rangeDays == 90 -> monthLabelFmt.format(Date(midMs))
                else -> dateLabelFmt.format(Date(midMs))
            }
        }

        fun bucketIndexFor(ts: Long, startMs: Long, spanMs: Long, count: Int): Int? {
            if (ts < startMs || ts > startMs + spanMs * count + dayMs) return null
            return (((ts - startMs) / spanMs).toInt()).coerceIn(0, count - 1)
        }

        val gmvBuckets = DoubleArray(bucketCount)
        currentOrders.forEach { order ->
            val ts = orderTimestamps[order.id] ?: nowMillis
            val idx = if (isAllRange) {
                (((ts - currentStart) / bucketSpanMs).toInt()).coerceIn(0, bucketCount - 1)
            } else {
                bucketIndexFor(ts, currentStart, bucketSpanMs, bucketCount)
            }
            if (idx != null) {
                gmvBuckets[idx] += order.totalAmount
            }
        }
        val prevGmvBuckets = DoubleArray(bucketCount)
        if (!isAllRange) {
            previousOrders.forEach { order ->
                val ts = orderTimestamps[order.id] ?: return@forEach
                val idx = (((ts - prevStart) / bucketSpanMs).toInt()).coerceIn(0, bucketCount - 1)
                if (ts >= prevStart && ts < currentStart) {
                    prevGmvBuckets[idx] += order.totalAmount
                }
            }
        }
        val gmvValues = gmvBuckets.toList()
        val prevValues = prevGmvBuckets.toList()

        // Orders by Status strictly from orders inside the selected window
        val statusGroups = listOf("Pending", "Confirmed", "Dispatched", "Delivered", "Cancelled")
        val statusCounts = statusGroups.map { grp ->
            allOrdersInWindow.count { o ->
                when (grp) {
                    "Pending" -> o.orderStatus.contains("Pending", true) ||
                        o.orderStatus.contains("Awaiting", true) ||
                        o.paymentStatus == "PENDING_VERIFICATION"
                    "Confirmed" -> o.orderStatus.equals("Confirmed", true) || o.orderStatus.equals("Processing", true)
                    "Dispatched" -> o.orderStatus.equals("Dispatched", true) || o.orderStatus.contains("Delivery", true)
                    "Delivered" -> o.orderStatus.equals("Delivered", true)
                    else -> o.orderStatus.equals("Cancelled", true)
                }
            }.toDouble()
        }

        // New Users vs New Shops strictly computed from real creation timestamps in the window
        val nonGuestUsers = users.filter { !it.role.equals("GUEST", ignoreCase = true) }
        val usersInWindow = if (isAllRange) {
            nonGuestUsers
        } else {
            nonGuestUsers.filter { u ->
                val ts = userTimestamps[u.id]
                ts != null && ts in currentStart..nowMillis
            }
        }
        val shopsInWindow = if (isAllRange) {
            shops
        } else {
            shops.filter { s ->
                val ts = shopTimestamps[s.id]
                ts != null && ts in currentStart..nowMillis
            }
        }

        val growthBucketCount = 4
        val growthSpanMs = (windowMs / growthBucketCount).coerceAtLeast(1L)
        val growthLabels = (0 until growthBucketCount).map { idx ->
            val midMs = currentStart + idx * growthSpanMs + growthSpanMs / 2L
            when {
                rangeDays == 30 -> "W${idx + 1}"
                else -> dateLabelFmt.format(Date(midMs))
            }
        }
        val userSeries = DoubleArray(growthBucketCount)
        usersInWindow.forEach { u ->
            val ts = userTimestamps[u.id] ?: nowMillis
            val idx = (((ts - currentStart) / growthSpanMs).toInt()).coerceIn(0, growthBucketCount - 1)
            userSeries[idx] += 1.0
        }
        val shopSeries = DoubleArray(growthBucketCount)
        shopsInWindow.forEach { s ->
            val ts = shopTimestamps[s.id] ?: nowMillis
            val idx = (((ts - currentStart) / growthSpanMs).toInt()).coerceIn(0, growthBucketCount - 1)
            shopSeries[idx] += 1.0
        }

        // Top Categories ranked strictly by actual line-item sales revenue in currentOrders
        val productCategoryMap = products.associateBy({ it.id }, { it.categoryId })
        val categoryMap = categories.associateBy { it.id }
        val categorySales = mutableMapOf<String, Double>()
        currentOrders.forEach { order ->
            var attributedFromItems = false
            try {
                val arr = JSONArray(order.itemsJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val productId = obj.optString("productId")
                    val catId = productCategoryMap[productId]
                    val lineRevenue = obj.optDouble("totalPrice", Double.NaN).takeIf { !it.isNaN() && it > 0.0 }
                        ?: (obj.optDouble("unitPrice", 0.0) * obj.optInt("quantity", 1).coerceAtLeast(1))
                    if (!catId.isNullOrBlank() && lineRevenue > 0.0) {
                        categorySales[catId] = (categorySales[catId] ?: 0.0) + lineRevenue
                        attributedFromItems = true
                    }
                }
            } catch (_: Exception) {
            }
            if (!attributedFromItems && order.totalAmount > 0.0) {
                val shopCatName = shops.firstOrNull { it.id == order.shopId }?.category.orEmpty()
                val matchedCatId = categories.firstOrNull { it.name.equals(shopCatName, ignoreCase = true) }?.id
                if (!matchedCatId.isNullOrBlank()) {
                    categorySales[matchedCatId] = (categorySales[matchedCatId] ?: 0.0) + order.totalAmount
                }
            }
        }
        val topCategories = categorySales.entries
            .filter { it.value > 0.0 }
            .sortedByDescending { it.value }
            .take(5)
            .map { (catId, sales) ->
                OwnerChartSlice(
                    id = catId,
                    label = categoryMap[catId]?.name ?: catId,
                    value = sales
                )
            }

        // Top Shops ranked by real GMV in currentOrders, with sparklines from chronological bucket sums
        val ordersByShop = currentOrders.groupBy { it.shopId }
        val topShops = shops.map { shop ->
            val shopOrders = ordersByShop[shop.id].orEmpty()
            val shopGmv = shopOrders.sumOf { it.totalAmount }
            val sparkBuckets = FloatArray(bucketCount)
            shopOrders.forEach { order ->
                val ts = orderTimestamps[order.id] ?: nowMillis
                val idx = (((ts - currentStart) / bucketSpanMs).toInt()).coerceIn(0, bucketCount - 1)
                sparkBuckets[idx] += order.totalAmount.toFloat()
            }
            OwnerTopShopMetric(
                shopId = shop.id,
                shopName = shop.name,
                isVerified = shop.isVerified,
                gmv = shopGmv,
                ordersCount = shopOrders.size,
                sparkline = sparkBuckets.toList()
            )
        }
            .sortedWith(compareByDescending<OwnerTopShopMetric> { it.gmv }.thenByDescending { it.ordersCount })
            .take(5)

        val verifOver24h = shops.count { s ->
            if (s.verificationStatus !in listOf("PENDING_REVIEW", "RESUBMITTED", "CHANGES_REQUESTED")) {
                false
            } else {
                val parsed = VerificationDocsCodec.parse(s.verificationDocsJson)
                val hours = EthiopianMarketplaceUtils.computeElapsedHours(parsed.submittedAt, nowMillis)
                hours == null || hours >= 24.0
            }
        }
        val flaggedOrStalePayments = orders.count { o ->
            val mismatch = o.transferredAmount != null && kotlin.math.abs(o.transferredAmount - o.totalAmount) > 0.5
            val hours = EthiopianMarketplaceUtils.computeElapsedHours(o.createdAt, nowMillis)
            o.paymentStatus == "REJECTED" || (o.paymentStatus == "PENDING_VERIFICATION" && (mismatch || (hours != null && hours >= 6.0)))
        }
        val openTickets = tickets.count { it.status.equals("OPEN", ignoreCase = true) } +
            flaggedReviews.count { it.status == "REPORTED" }

        val activeShopsCount = shops.count { !it.isVacationMode && it.acceptsOrders }
        val uniqueOrderingBuyers = currentOrders.map { it.customerUserId }.distinct().size
        val conversion = ((uniqueOrderingBuyers.toDouble() / maxOf(nonGuestUsers.size, 1).toDouble()) * 100.0)
            .coerceIn(0.0, 100.0)

        ApiResponse(
            success = true,
            data = OwnerDashboardMetricsDto(
                rangeDays = rangeDays,
                totalGmv = rangeGmv,
                previousGmv = prevGmv,
                platformRevenue = platformRevenue,
                totalOrders = allOrdersInWindow.size,
                activeShops = activeShopsCount,
                totalUsers = nonGuestUsers.size,
                newUsersCount = usersInWindow.size,
                newShopsCount = shopsInWindow.size,
                conversionRate = conversion,
                gmvLabels = bucketLabels,
                gmvValues = gmvValues,
                previousGmvValues = prevValues,
                statusLabels = statusGroups,
                statusCounts = statusCounts,
                growthLabels = growthLabels,
                newUsersSeries = userSeries.toList(),
                newShopsSeries = shopSeries.toList(),
                topCategories = topCategories,
                topShops = topShops,
                verificationsWaitingOver24h = verifOver24h,
                flaggedOrStalePaymentsCount = flaggedOrStalePayments,
                openTicketsAndDisputesCount = openTickets
            )
        )
    }

    override suspend fun getFlaggedReviews(): ApiResponse<List<FlaggedReviewDto>> = withContext(Dispatchers.IO) {
        db.ensureSeeded()
        val list = db.flaggedReviewDao().getAllFlagged().map { it.toDto() }
        ApiResponse(success = true, data = list)
    }

    override suspend fun reportOrModerateReview(payload: Map<String, String>): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        val caller = resolveCurrentUser()
        val action = (payload["action"] ?: "REPORT").trim().uppercase()
        if (action in listOf("MODERATE", "DISMISS", "DELETE", "HIDE", "RESTORE")) {
            if (caller == null || !caller.isAdmin()) {
                return@withContext ApiResponse(success = false, data = false, message = "Admin permission required.")
            }
            val id = payload["id"] ?: return@withContext ApiResponse(success = false, data = false)
            val note = payload["note"]?.trim().orEmpty()
            val status = payload["status"] ?: when (action) {
                "DISMISS" -> "DISMISSED"
                "DELETE", "HIDE" -> "HIDDEN"
                "RESTORE" -> "RESTORED"
                else -> "DISMISSED"
            }
            db.flaggedReviewDao().updateStatusWithNote(id, status, note)
        } else {
            val entity = FlaggedReviewEntity(
                id = "flag_${System.currentTimeMillis()}",
                authorName = payload["authorName"] ?: "Listing Report",
                reason = payload["reason"] ?: "Reported by user",
                reviewText = payload["reviewText"] ?: "",
                status = "REPORTED",
                createdAt = "Just now",
                reviewId = payload["reviewId"].orEmpty(),
                productName = payload["productName"].orEmpty(),
                rating = payload["rating"]?.toDoubleOrNull() ?: 0.0
            )
            db.flaggedReviewDao().insert(entity)
        }
        ApiResponse(success = true, data = true)
    }

    override suspend fun getSubscriptionPlans(): ApiResponse<List<SubscriptionPlanDto>> = withContext(Dispatchers.IO) {
        db.ensureSeeded()
        val list = db.subscriptionPlanDao().getAllPlans().map { it.toDto() }
        ApiResponse(success = true, data = list)
    }

    override suspend fun saveSubscriptionPlan(plan: SubscriptionPlanDto): ApiResponse<SubscriptionPlanDto> = withContext(Dispatchers.IO) {
        val caller = requireSignedInUser()
        if (caller == null || !caller.isAdmin()) {
            return@withContext ApiResponse(success = false, message = "Admin permission required.")
        }
        val entity = SubscriptionPlanEntity(
            id = plan.id.ifBlank { "plan_${System.currentTimeMillis()}" },
            name = plan.name.trim(),
            monthlyPrice = plan.monthlyPrice,
            yearlyPrice = plan.yearlyPrice,
            maxProducts = plan.maxProducts,
            featuresCsv = plan.featuresCsv,
            isPopular = plan.isPopular,
            isActive = plan.isActive,
            maxPromoCodes = plan.maxPromoCodes,
            analyticsDays = plan.analyticsDays
        )
        db.subscriptionPlanDao().insert(entity)
        ApiResponse(success = true, data = entity.toDto())
    }

    override suspend fun deleteSubscriptionPlan(id: String): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        val caller = requireSignedInUser()
        if (caller == null || !caller.isAdmin()) {
            return@withContext ApiResponse(success = false, data = false, message = "Admin permission required.")
        }
        db.subscriptionPlanDao().deleteById(id)
        ApiResponse(success = true, data = true)
    }

    // ==================== PHASE 5: RETURNS & REFUNDS ====================

    override suspend fun getBuyerReturns(): ApiResponse<List<ReturnRequestDto>> = withContext(Dispatchers.IO) {
        db.ensureSeeded()
        val userId = resolveCurrentUser()?.id ?: return@withContext ApiResponse(success = true, data = emptyList())
        ApiResponse(success = true, data = db.returnRequestDao().getByCustomer(userId).map { it.toDto() })
    }

    override suspend fun getSellerReturns(): ApiResponse<List<ReturnRequestDto>> = withContext(Dispatchers.IO) {
        db.ensureSeeded()
        val shopId = resolveActiveSellerShopId()
            ?: return@withContext ApiResponse(success = false, message = "No shop associated with current account", data = emptyList())
        ApiResponse(success = true, data = db.returnRequestDao().getByShop(shopId).map { it.toDto() })
    }

    override suspend fun getReturnDetails(id: String): ApiResponse<ReturnRequestDto> = withContext(Dispatchers.IO) {
        val caller = requireSignedInUser()
            ?: return@withContext ApiResponse(success = false, message = "Permission denied.")
        val req = db.returnRequestDao().getById(id)
            ?: return@withContext ApiResponse(success = false, message = "Return request not found")
        if (req.customerUserId != caller.id && !caller.ownsShop(req.shopId)) {
            return@withContext ApiResponse(success = false, message = "Permission denied.")
        }
        ApiResponse(success = true, data = req.toDto())
    }

    override suspend fun submitReturnRequest(payload: Map<String, Any>): ApiResponse<ReturnRequestDto> = withContext(Dispatchers.IO) {
        val user = requireSignedInUser()
            ?: return@withContext ApiResponse(success = false, message = "No active user session")
        val orderId = (payload["orderId"] as? String)?.trim().orEmpty()
        val order = db.orderDao().getOrderById(orderId)
            ?: return@withContext ApiResponse(success = false, message = "Order not found")
        if (order.customerUserId != user.id && !user.isAdmin()) {
            return@withContext ApiResponse(success = false, message = "You can only request returns for your own orders.")
        }

        val nowMs = System.currentTimeMillis()
        val deliveredAgeHours = order.deliveredAt
            ?.takeIf { it.isNotBlank() }
            ?.let { EthiopianMarketplaceUtils.computeElapsedHours(it, nowMs) }
        if (!order.orderStatus.equals("Delivered", ignoreCase = true) ||
            deliveredAgeHours == null ||
            deliveredAgeHours > 7.0 * 24.0
        ) {
            return@withContext ApiResponse(
                success = false,
                message = "Return window expired (orders are eligible within 7 days of delivery)."
            )
        }
        val existing = db.returnRequestDao().getByOrderId(orderId)
        if (existing != null && existing.status != "REJECTED") {
            return@withContext ApiResponse(success = false, message = "A return request already exists for this order.")
        }

        val orderDto = order.toDto()
        @Suppress("UNCHECKED_CAST")
        val selectedProductIds = (payload["itemProductIds"] as? List<*>)?.mapNotNull { it as? String }.orEmpty()
        val returnItems = if (selectedProductIds.isEmpty()) {
            orderDto.items.map {
                ReturnItemDto(it.productId, it.productName, it.productImageUrl, it.quantity, it.unitPrice)
            }
        } else {
            orderDto.items.filter { it.productId in selectedProductIds }.map {
                ReturnItemDto(it.productId, it.productName, it.productImageUrl, it.quantity, it.unitPrice)
            }
        }
        if (returnItems.isEmpty()) {
            return@withContext ApiResponse(success = false, message = "Please select at least one item to return.")
        }

        val reason = (payload["reason"] as? String)?.trim().orEmpty().ifBlank { "Other issue" }
        val comment = (payload["comment"] as? String)?.trim().orEmpty()
        val resolution = (payload["resolution"] as? String)?.trim()?.uppercase().orEmpty().ifBlank { "REFUND" }
        val refundMethod = (payload["refundMethod"] as? String)?.trim().orEmpty().ifBlank { order.paymentMethodTitle }
        @Suppress("UNCHECKED_CAST")
        val photos = (payload["photos"] as? List<*>)?.mapNotNull { it as? String }?.filter { it.isNotBlank() }.orEmpty()
        val refundAmount = returnItems.sumOf { it.unitPrice * it.quantity }
        val now = getCurrentTime()

        val itemsArr = JSONArray().apply {
            returnItems.forEach { item ->
                put(
                    org.json.JSONObject().apply {
                        put("productId", item.productId)
                        put("productName", item.productName)
                        put("productImageUrl", item.productImageUrl)
                        put("quantity", item.quantity)
                        put("unitPrice", item.unitPrice)
                    }
                )
            }
        }
        val photosArr = JSONArray().apply { photos.forEach { put(it) } }
        val entity = ReturnRequestEntity(
            id = "ret_${System.currentTimeMillis()}",
            orderId = order.id,
            orderReference = order.orderReference,
            shopId = order.shopId,
            shopName = order.shopName,
            customerUserId = user.id,
            customerName = user.name,
            itemsJson = itemsArr.toString(),
            refundAmount = refundAmount,
            reason = reason,
            comment = comment,
            photosJson = photosArr.toString(),
            resolution = resolution,
            status = "REQUESTED",
            refundMethod = refundMethod,
            createdAt = now,
            updatedAt = now
        )
        db.returnRequestDao().insert(entity)

        db.shopDao().getShopById(order.shopId)?.let { shop ->
            db.notificationDao().insert(
                NotificationEntity(
                    id = "notif_ret_${System.currentTimeMillis()}",
                    userId = shop.ownerUserId,
                    type = "ORDER",
                    title = "New Return Request #${order.orderReference}",
                    body = "${user.name} requested a $resolution (${entity.reason}).",
                    timestamp = "Just now",
                    isRead = false
                )
            )
        }
        ApiResponse(success = true, data = entity.toDto())
    }

    override suspend fun decideReturnRequest(id: String, payload: Map<String, String>): ApiResponse<ReturnRequestDto> = withContext(Dispatchers.IO) {
        val caller = requireSignedInUser()
            ?: return@withContext ApiResponse(success = false, message = "Permission denied.")
        val existing = db.returnRequestDao().getById(id)
            ?: return@withContext ApiResponse(success = false, message = "Return request not found")
        val action = (payload["action"] ?: "").trim().uppercase()
        if (action == "SENT_BACK") {
            if (existing.customerUserId != caller.id && !caller.isAdmin()) {
                return@withContext ApiResponse(success = false, message = "Only the buyer or admin can mark a return shipped back.")
            }
        } else if (action in listOf("APPROVE", "OFFER_REPLACEMENT", "COMPLETE_REFUND", "REJECT")) {
            if (!caller.ownsShop(existing.shopId)) {
                return@withContext ApiResponse(success = false, message = "Only the shop owner or admin can decide this return.")
            }
        } else {
            return@withContext ApiResponse(success = false, message = "Cannot $action return from status ${existing.status}.")
        }
        val allowedCurrent = when (action) {
            "APPROVE", "OFFER_REPLACEMENT", "REJECT" -> setOf("REQUESTED")
            "SENT_BACK" -> setOf("APPROVED", "REPLACEMENT_OFFERED")
            "COMPLETE_REFUND" -> setOf("APPROVED", "REPLACEMENT_OFFERED", "SENT_BACK")
            else -> emptySet()
        }
        if (existing.status.uppercase() !in allowedCurrent) {
            return@withContext ApiResponse(success = false, message = "Cannot $action return from status ${existing.status}.")
        }
        val note = payload["note"]?.trim().orEmpty()
        if (action == "REJECT" && note.isBlank()) {
            return@withContext ApiResponse(success = false, message = "Rejection reason is required.")
        }
        val nextStatus = when (action) {
            "APPROVE" -> "APPROVED"
            "OFFER_REPLACEMENT" -> "REPLACEMENT_OFFERED"
            "SENT_BACK" -> "SENT_BACK"
            "COMPLETE_REFUND" -> "COMPLETED"
            "REJECT" -> "REJECTED"
            else -> existing.status
        }
        val updated = existing.copy(
            status = nextStatus,
            sellerNote = note.ifBlank { existing.sellerNote },
            refundMethod = payload["refundMethod"]?.takeIf { it.isNotBlank() } ?: existing.refundMethod,
            updatedAt = getCurrentTime()
        )
        db.returnRequestDao().update(updated)
        db.notificationDao().insert(
            NotificationEntity(
                id = "notif_ret_dec_${System.currentTimeMillis()}",
                userId = existing.customerUserId,
                type = "ORDER",
                title = "Return #${existing.orderReference} updated",
                body = "Status changed to $nextStatus${if (note.isNotBlank()) ": $note" else "."}",
                timestamp = "Just now",
                isRead = false
            )
        )
        ApiResponse(success = true, data = updated.toDto())
    }

    // ==================== PHASE 5: EARNINGS, PAYOUTS & COMMISSIONS ====================

    private fun buildSellerFinanceData(
        shop: ShopEntity,
        orders: List<OrderEntity>,
        returns: List<ReturnRequestEntity>,
        payouts: List<PayoutRequestEntity>,
        rules: List<CommissionConfigEntity>
    ): SellerEarningsSummaryDto {
        val defaultRate = rules.firstOrNull { it.categoryId == "DEFAULT" }?.ratePercent ?: 5.0
        val minPayout = rules.firstOrNull { it.categoryId == "DEFAULT" }?.minPayoutAmount ?: 500.0

        val verifiedOrders = orders.filter { it.paymentStatus == "VERIFIED" && !it.orderStatus.equals("Cancelled", true) }
        val completedOrDelivered = verifiedOrders.filter { it.orderStatus.equals("Delivered", true) }
        val pendingClearance = verifiedOrders.filter { !it.orderStatus.equals("Delivered", true) }

        val completedRefunds = returns.filter { it.status == "COMPLETED" }
        val grossDelivered = completedOrDelivered.sumOf { it.subtotal }
        val commissionDelivered = grossDelivered * (defaultRate / 100.0)
        val refundTotal = completedRefunds.sumOf { it.refundAmount }
        val committedPayouts = payouts.filter { it.status != "REJECTED" }.sumOf { it.amount }
        val availableBalance = (grossDelivered - commissionDelivered - refundTotal - committedPayouts).coerceAtLeast(0.0)
        val pendingBalance = pendingClearance.sumOf { it.subtotal * (1.0 - defaultRate / 100.0) }.coerceAtLeast(0.0)

        val entries = mutableListOf<LedgerEntryDto>()
        val nowMs = System.currentTimeMillis()
        verifiedOrders.forEach { o ->
            val comm = o.subtotal * (defaultRate / 100.0)
            val net = o.subtotal - comm
            val weekLbl = EthiopianMarketplaceUtils.computeElapsedHours(o.createdAt, nowMs)?.let { h ->
                if (h < 168.0) "This week" else "Earlier this month"
            } ?: "Recent"
            entries += LedgerEntryDto(
                id = "led_sale_${o.id}",
                shopId = shop.id,
                shopName = shop.name,
                type = "SALE",
                title = "Order #${o.orderReference}",
                subtitle = "${o.customerName} • ${o.paymentMethodTitle}",
                amount = net,
                grossAmount = o.subtotal,
                feeAmount = comm,
                orderRef = o.orderReference,
                weekLabel = weekLbl,
                createdAt = o.createdAt,
                status = if (o.orderStatus.equals("Delivered", true)) "CLEARED" else "PENDING"
            )
            entries += LedgerEntryDto(
                id = "led_comm_${o.id}",
                shopId = shop.id,
                shopName = shop.name,
                type = "COMMISSION",
                title = "Suqi Commission (${defaultRate.toInt()}%)",
                subtitle = "Order #${o.orderReference}",
                amount = -comm,
                grossAmount = o.subtotal,
                feeAmount = comm,
                orderRef = o.orderReference,
                weekLabel = weekLbl,
                createdAt = o.createdAt,
                status = "COMPLETED"
            )
        }
        completedRefunds.forEach { r ->
            entries += LedgerEntryDto(
                id = "led_ref_${r.id}",
                shopId = shop.id,
                shopName = shop.name,
                type = "REFUND",
                title = "Refund • #${r.orderReference}",
                subtitle = r.reason,
                amount = -r.refundAmount,
                grossAmount = r.refundAmount,
                orderRef = r.orderReference,
                weekLabel = "This week",
                createdAt = r.updatedAt.ifBlank { r.createdAt },
                status = "COMPLETED"
            )
        }
        payouts.forEach { p ->
            entries += LedgerEntryDto(
                id = "led_pay_${p.id}",
                shopId = shop.id,
                shopName = shop.name,
                type = "PAYOUT",
                title = "Payout • ${p.bankName}",
                subtitle = p.referenceCode.ifBlank { p.accountNumber },
                amount = -p.amount,
                grossAmount = p.amount,
                weekLabel = "This week",
                createdAt = p.createdAt,
                status = p.status
            )
        }

        val currentMonthFmt = SimpleDateFormat("yyyy-MM", Locale.US)
        val labelMonthFmt = SimpleDateFormat("MMMM yyyy", Locale.US)
        val cal = Calendar.getInstance()
        val thisMonthKey = currentMonthFmt.format(cal.time)
        val thisMonthLabel = labelMonthFmt.format(cal.time)
        cal.add(Calendar.MONTH, -1)
        val lastMonthKey = currentMonthFmt.format(cal.time)
        val lastMonthLabel = labelMonthFmt.format(cal.time)

        val statements = listOf(
            MonthlyStatementDto(
                monthKey = thisMonthKey,
                monthLabel = thisMonthLabel,
                grossSales = verifiedOrders.sumOf { it.subtotal },
                commissions = verifiedOrders.sumOf { it.subtotal * (defaultRate / 100.0) },
                refunds = refundTotal,
                payouts = payouts.filter { it.status == "PAID" }.sumOf { it.amount },
                netAmount = (verifiedOrders.sumOf { it.subtotal * (1.0 - defaultRate / 100.0) } - refundTotal),
                ordersCount = verifiedOrders.size
            ),
            MonthlyStatementDto(
                monthKey = lastMonthKey,
                monthLabel = lastMonthLabel,
                grossSales = 0.0,
                commissions = 0.0,
                refunds = 0.0,
                payouts = 0.0,
                netAmount = 0.0,
                ordersCount = 0
            )
        )

        return SellerEarningsSummaryDto(
            shopId = shop.id,
            availableBalance = availableBalance,
            pendingBalance = pendingBalance,
            minPayoutAmount = minPayout,
            defaultCommissionRate = defaultRate,
            payoutAccount = PayoutAccountDto(
                providerType = shop.payoutProviderType.ifBlank { "TELEBIRR" },
                bankName = shop.payoutBankName.ifBlank { "Telebirr" },
                accountName = shop.payoutAccountName.ifBlank { shop.name },
                accountNumber = shop.payoutAccountNumber
            ),
            ledger = entries,
            statements = statements,
            payoutRequests = payouts.map { it.toDto() }
        )
    }

    override suspend fun getSellerEarningsSummary(): ApiResponse<SellerEarningsSummaryDto> = withContext(Dispatchers.IO) {
        db.ensureSeeded()
        val shopId = resolveActiveSellerShopId()
            ?: return@withContext ApiResponse(success = false, message = "No shop associated with current account")
        val shop = db.shopDao().getShopById(shopId)
            ?: return@withContext ApiResponse(success = false, message = "Shop not found")
        val orders = db.orderDao().getShopOrders(shopId)
        val returns = db.returnRequestDao().getByShop(shopId)
        val payouts = db.payoutRequestDao().getByShop(shopId)
        val rules = db.commissionConfigDao().getAll()
        ApiResponse(success = true, data = buildSellerFinanceData(shop, orders, returns, payouts, rules))
    }

    override suspend fun saveShopPayoutAccount(account: PayoutAccountDto): ApiResponse<PayoutAccountDto> = withContext(Dispatchers.IO) {
        val caller = requireSignedInUser()
            ?: return@withContext ApiResponse(success = false, message = "Permission denied.")
        val shopId = resolveActiveSellerShopId()
            ?: return@withContext ApiResponse(success = false, message = "No shop associated with current account")
        if (!caller.ownsShop(shopId)) {
            return@withContext ApiResponse(success = false, message = "Permission denied.")
        }
        val shop = db.shopDao().getShopById(shopId)
            ?: return@withContext ApiResponse(success = false, message = "Shop not found")
        if (account.accountNumber.isBlank() || account.accountName.isBlank()) {
            return@withContext ApiResponse(success = false, message = "Account name and number are required")
        }
        db.shopDao().updateShop(
            shop.copy(
                payoutProviderType = account.providerType,
                payoutBankName = account.bankName,
                payoutAccountName = account.accountName.trim(),
                payoutAccountNumber = account.accountNumber.trim()
            )
        )
        ApiResponse(success = true, data = account)
    }

    override suspend fun requestSellerPayout(payload: Map<String, Any>): ApiResponse<PayoutRequestDto> = withContext(Dispatchers.IO) {
        val caller = requireSignedInUser()
            ?: return@withContext ApiResponse(success = false, message = "Permission denied.")
        val shopId = resolveActiveSellerShopId()
            ?: return@withContext ApiResponse(success = false, message = "No shop associated with current account")
        if (!caller.ownsShop(shopId)) {
            return@withContext ApiResponse(success = false, message = "Permission denied.")
        }
        val shop = db.shopDao().getShopById(shopId)
            ?: return@withContext ApiResponse(success = false, message = "Shop not found")
        if (shop.payoutAccountNumber.isBlank()) {
            return@withContext ApiResponse(success = false, message = "Save a payout account first.")
        }
        val amount = (payload["amount"] as? Number)?.toDouble() ?: 0.0
        val rules = db.commissionConfigDao().getAll()
        val minPayout = rules.firstOrNull { it.categoryId == "DEFAULT" }?.minPayoutAmount ?: 500.0
        if (amount < minPayout) {
            return@withContext ApiResponse(success = false, message = "Minimum payout is ETB ${minPayout.toInt()}.")
        }
        val summary = buildSellerFinanceData(
            shop,
            db.orderDao().getShopOrders(shopId),
            db.returnRequestDao().getByShop(shopId),
            db.payoutRequestDao().getByShop(shopId),
            rules
        )
        if (amount > summary.availableBalance + 0.01) {
            return@withContext ApiResponse(success = false, message = "Insufficient available balance.")
        }
        val entity = PayoutRequestEntity(
            id = "payout_${System.currentTimeMillis()}",
            shopId = shop.id,
            shopName = shop.name,
            amount = amount,
            providerType = shop.payoutProviderType.ifBlank { "TELEBIRR" },
            bankName = shop.payoutBankName.ifBlank { "Telebirr" },
            accountName = shop.payoutAccountName.ifBlank { shop.name },
            accountNumber = shop.payoutAccountNumber,
            status = "PENDING",
            createdAt = getCurrentTime()
        )
        db.payoutRequestDao().insert(entity)
        ApiResponse(success = true, data = entity.toDto())
    }

    override suspend fun getAdminFinanceOverview(): ApiResponse<AdminFinanceOverviewDto> = withContext(Dispatchers.IO) {
        db.ensureSeeded()
        val rules = db.commissionConfigDao().getAll().map { it.toDto() }
        val defaultRate = rules.firstOrNull { it.categoryId == "DEFAULT" }?.ratePercent ?: 5.0
        val allOrders = db.orderDao().getAllOrders().filter { it.paymentStatus == "VERIFIED" && !it.orderStatus.equals("Cancelled", true) }
        val commissionRevenue = allOrders.sumOf { it.subtotal * (defaultRate / 100.0) }
        val approvedUpgrades = db.subscriptionUpgradeDao().getAll().filter { it.status == "APPROVED" }
        val subscriptionRevenue = approvedUpgrades.sumOf { it.amount }
        val payouts = db.payoutRequestDao().getAll()
        val pendingPayouts = payouts.filter { it.status in listOf("PENDING", "APPROVED") }

        val ledger = mutableListOf<LedgerEntryDto>()
        allOrders.forEach { o ->
            val comm = o.subtotal * (defaultRate / 100.0)
            ledger += LedgerEntryDto(
                id = "adm_comm_${o.id}",
                shopId = o.shopId,
                shopName = o.shopName,
                type = "COMMISSION",
                title = "Commission • ${o.shopName}",
                subtitle = "#${o.orderReference} (${defaultRate.toInt()}%)",
                amount = comm,
                grossAmount = o.subtotal,
                feeAmount = comm,
                orderRef = o.orderReference,
                createdAt = o.createdAt,
                status = "COMPLETED"
            )
        }
        approvedUpgrades.forEach { u ->
            ledger += LedgerEntryDto(
                id = "adm_sub_${u.id}",
                shopId = u.shopId,
                shopName = u.shopName,
                type = "SUBSCRIPTION",
                title = "${u.planName} • ${u.shopName}",
                subtitle = u.billingCycle,
                amount = u.amount,
                grossAmount = u.amount,
                createdAt = u.reviewedAt.ifBlank { u.createdAt },
                status = "COMPLETED"
            )
        }
        payouts.forEach { p ->
            ledger += LedgerEntryDto(
                id = "adm_pay_${p.id}",
                shopId = p.shopId,
                shopName = p.shopName,
                type = "PAYOUT",
                title = "Payout to ${p.shopName}",
                subtitle = p.referenceCode.ifBlank { p.bankName },
                amount = -p.amount,
                grossAmount = p.amount,
                createdAt = p.processedAt.ifBlank { p.createdAt },
                status = p.status
            )
        }

        ApiResponse(
            success = true,
            data = AdminFinanceOverviewDto(
                platformRevenue = commissionRevenue + subscriptionRevenue,
                commissionRevenue = commissionRevenue,
                subscriptionRevenue = subscriptionRevenue,
                pendingPayoutsTotal = pendingPayouts.sumOf { it.amount },
                pendingPayoutsCount = pendingPayouts.size,
                rules = rules,
                payoutRequests = payouts.map { it.toDto() },
                ledger = ledger
            )
        )
    }

    override suspend fun saveCommissionConfig(rule: CommissionRuleDto): ApiResponse<CommissionRuleDto> = withContext(Dispatchers.IO) {
        val caller = requireSignedInUser()
        if (caller == null || !caller.isAdmin()) {
            return@withContext ApiResponse(success = false, message = "Admin permission required.")
        }
        val entity = CommissionConfigEntity(
            id = rule.id.ifBlank { "comm_${rule.categoryId.ifBlank { System.currentTimeMillis() }}" },
            categoryId = rule.categoryId.ifBlank { "DEFAULT" },
            categoryName = rule.categoryName.ifBlank { "All Categories" },
            ratePercent = rule.ratePercent.coerceIn(0.0, 50.0),
            minPayoutAmount = rule.minPayoutAmount.coerceAtLeast(100.0)
        )
        db.commissionConfigDao().insert(entity)
        ApiResponse(success = true, data = entity.toDto())
    }

    override suspend fun decidePayoutRequest(id: String, payload: Map<String, String>): ApiResponse<PayoutRequestDto> = withContext(Dispatchers.IO) {
        val caller = requireSignedInUser()
        if (caller == null || !caller.isAdmin()) {
            return@withContext ApiResponse(success = false, message = "Admin permission required.")
        }
        val existing = db.payoutRequestDao().getById(id)
            ?: return@withContext ApiResponse(success = false, message = "Payout request not found")
        val decision = (payload["decision"] ?: "").trim().uppercase()
        val allowedCurrent = when (decision) {
            "APPROVE" -> setOf("PENDING")
            "MARK_PAID" -> setOf("APPROVED")
            "REJECT" -> setOf("PENDING", "APPROVED")
            else -> emptySet()
        }
        if (existing.status.uppercase() !in allowedCurrent) {
            return@withContext ApiResponse(success = false, message = "Cannot $decision payout from status ${existing.status}.")
        }
        val refCode = payload["referenceCode"]?.trim().orEmpty()
        val note = payload["note"]?.trim().orEmpty()
        if (decision == "MARK_PAID" && refCode.isBlank()) {
            return@withContext ApiResponse(success = false, message = "Payout reference is required.")
        }
        val nextStatus = when (decision) {
            "APPROVE" -> "APPROVED"
            "MARK_PAID" -> "PAID"
            "REJECT" -> "REJECTED"
            else -> existing.status
        }
        val updated = existing.copy(
            status = nextStatus,
            referenceCode = refCode.ifBlank { existing.referenceCode },
            note = note.ifBlank { existing.note },
            processedAt = getCurrentTime()
        )
        db.payoutRequestDao().update(updated)
        db.shopDao().getShopById(existing.shopId)?.let { shop ->
            db.notificationDao().insert(
                NotificationEntity(
                    id = "notif_pay_${System.currentTimeMillis()}",
                    userId = shop.ownerUserId,
                    type = "PAYMENT",
                    title = "Payout ETB ${existing.amount.toInt()} $nextStatus",
                    body = if (refCode.isNotBlank()) "Reference: $refCode" else note.ifBlank { "Updated by Suqi Finance." },
                    timestamp = "Just now",
                    isRead = false
                )
            )
        }
        ApiResponse(success = true, data = updated.toDto())
    }

    // ==================== PHASE 5: SUBSCRIPTIONS & PILOT ====================

    override suspend fun updatePilotConfig(payload: Map<String, Any>): ApiResponse<AdminSettingsDto> = withContext(Dispatchers.IO) {
        val caller = requireSignedInUser()
        if (caller == null || !caller.isAdmin()) {
            return@withContext ApiResponse(success = false, message = "Admin permission required.")
        }
        val current = db.adminSettingsDao().getSettings() ?: AdminSettingsEntity()
        val isPilot = payload["isPilotMode"] as? Boolean ?: current.isPilotMode
        val endDate = payload["pilotEndDate"] as? String ?: current.pilotEndDate
        db.adminSettingsDao().insert(current.copy(isPilotMode = isPilot, pilotEndDate = endDate))
        getAdminSettings()
    }

    override suspend fun getSubscriptionUpgrades(): ApiResponse<List<SubscriptionUpgradeDto>> = withContext(Dispatchers.IO) {
        db.ensureSeeded()
        val current = resolveCurrentUser()
        val list = if (current?.role == "ADMIN") {
            db.subscriptionUpgradeDao().getAll()
        } else {
            val shopId = resolveActiveSellerShopId()
            if (shopId.isNullOrBlank()) emptyList() else db.subscriptionUpgradeDao().getByShop(shopId)
        }
        ApiResponse(success = true, data = list.map { it.toDto() })
    }

    override suspend fun submitSubscriptionUpgrade(payload: Map<String, Any>): ApiResponse<SubscriptionUpgradeDto> = withContext(Dispatchers.IO) {
        val caller = requireSignedInUser()
            ?: return@withContext ApiResponse(success = false, message = "Permission denied.")
        val shopId = resolveActiveSellerShopId()
            ?: return@withContext ApiResponse(success = false, message = "No shop associated with current account")
        if (!caller.ownsShop(shopId)) {
            return@withContext ApiResponse(success = false, message = "Permission denied.")
        }
        val shop = db.shopDao().getShopById(shopId)
            ?: return@withContext ApiResponse(success = false, message = "Shop not found")
        val planId = (payload["planId"] as? String)?.trim().orEmpty()
        val plan = db.subscriptionPlanDao().getAllPlans().firstOrNull { it.id == planId }
            ?: return@withContext ApiResponse(success = false, message = "Subscription plan not found")
        val cycle = ((payload["billingCycle"] as? String) ?: "MONTHLY").uppercase()
        val amount = if (cycle == "YEARLY") plan.yearlyPrice else plan.monthlyPrice
        val ref = (payload["transactionReference"] as? String)?.trim().orEmpty()
        if (ref.isBlank()) {
            return@withContext ApiResponse(success = false, message = "Transaction reference is required")
        }
        val entity = SubscriptionUpgradeEntity(
            id = "upg_${System.currentTimeMillis()}",
            shopId = shop.id,
            shopName = shop.name,
            planId = plan.id,
            planName = plan.name,
            billingCycle = cycle,
            amount = amount,
            paymentMethodTitle = (payload["paymentMethodTitle"] as? String)?.ifBlank { "Telebirr" } ?: "Telebirr",
            transactionReference = ref,
            receiptImageUri = (payload["receiptImageUri"] as? String).orEmpty(),
            status = "PENDING",
            createdAt = getCurrentTime()
        )
        db.subscriptionUpgradeDao().insert(entity)
        ApiResponse(success = true, data = entity.toDto(), message = "Upgrade request submitted for verification.")
    }

    override suspend fun decideSubscriptionUpgrade(id: String, payload: Map<String, String>): ApiResponse<SubscriptionUpgradeDto> = withContext(Dispatchers.IO) {
        val caller = requireSignedInUser()
        if (caller == null || !caller.isAdmin()) {
            return@withContext ApiResponse(success = false, message = "Admin permission required.")
        }
        val existing = db.subscriptionUpgradeDao().getById(id)
            ?: return@withContext ApiResponse(success = false, message = "Upgrade request not found")
        if (!existing.status.equals("PENDING", ignoreCase = true)) {
            return@withContext ApiResponse(success = false, message = "Upgrade request already ${existing.status}.")
        }
        val decision = (payload["decision"] ?: "APPROVE").trim().uppercase()
        val nextStatus = when (decision) {
            "APPROVE" -> "APPROVED"
            "REJECT" -> "REJECTED"
            else -> return@withContext ApiResponse(success = false, message = "Permission denied.")
        }
        val updated = existing.copy(
            status = nextStatus,
            adminNote = payload["note"]?.trim().orEmpty(),
            reviewedAt = getCurrentTime()
        )
        db.subscriptionUpgradeDao().update(updated)
        db.shopDao().getShopById(existing.shopId)?.let { shop ->
            db.notificationDao().insert(
                NotificationEntity(
                    id = "notif_upg_${System.currentTimeMillis()}",
                    userId = shop.ownerUserId,
                    type = "SYSTEM",
                    title = "${existing.planName} Upgrade $nextStatus",
                    body = if (nextStatus == "APPROVED") "Your ${existing.planName} subscription is now active." else updated.adminNote.ifBlank { "Upgrade request rejected." },
                    timestamp = "Just now",
                    isRead = false
                )
            )
        }
        ApiResponse(success = true, data = updated.toDto())
    }

    override suspend fun cancelShopSubscription(): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        val caller = requireSignedInUser()
            ?: return@withContext ApiResponse(success = false, data = false, message = "Permission denied.")
        val shopId = resolveActiveSellerShopId()
            ?: return@withContext ApiResponse(success = false, data = false)
        if (!caller.ownsShop(shopId)) {
            return@withContext ApiResponse(success = false, data = false, message = "Permission denied.")
        }
        val upgrades = db.subscriptionUpgradeDao().getByShop(shopId).filter { it.status == "APPROVED" }
        upgrades.forEach {
            db.subscriptionUpgradeDao().update(it.copy(status = "CANCELLED", reviewedAt = getCurrentTime()))
        }
        ApiResponse(success = true, data = true)
    }

    // ==================== PHASE 5: PROMOTIONS 2.0 & FLASH SALES ====================

    override suspend fun validatePromoCode(payload: Map<String, Any>): ApiResponse<PromoCodeDto> = withContext(Dispatchers.IO) {
        db.ensureSeeded()
        val shopId = (payload["shopId"] as? String)?.trim().orEmpty()
        val code = (payload["code"] as? String)?.trim()?.uppercase().orEmpty()
        val subtotal = (payload["subtotal"] as? Number)?.toDouble() ?: 0.0
        @Suppress("UNCHECKED_CAST")
        val productIds = (payload["productIds"] as? List<*>)?.mapNotNull { it as? String }.orEmpty()

        val promo = db.promoCodeDao().getPromoByCode(shopId, code)
            ?: return@withContext ApiResponse(success = false, message = "Promo code expired or inactive.")
        if (!promo.isActive) {
            return@withContext ApiResponse(success = false, message = "Promo code expired or inactive.")
        }
        if (promo.maxUsesTotal > 0 && promo.usesCount >= promo.maxUsesTotal) {
            return@withContext ApiResponse(success = false, message = "Promo usage limit reached.")
        }
        if (subtotal < promo.minOrderAmount) {
            return@withContext ApiResponse(success = false, message = "Min spend ETB ${promo.minOrderAmount.toInt()} required")
        }
        val targets = promo.scopeTargetIdsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
        if (promo.scopeType == "PRODUCTS" && targets.isNotEmpty() && productIds.isNotEmpty() && productIds.none { it in targets }) {
            return@withContext ApiResponse(success = false, message = "Promo does not apply to items in cart.")
        }
        if (promo.scopeType == "CATEGORY" && targets.isNotEmpty() && productIds.isNotEmpty()) {
            val cartCats = productIds.mapNotNull { db.productDao().getProductById(it)?.categoryId }.toSet()
            if (cartCats.none { it in targets }) {
                return@withContext ApiResponse(success = false, message = "Promo does not apply to items in cart.")
            }
        }
        ApiResponse(success = true, data = promo.toDto())
    }

    override suspend fun getFlashSales(activeOnly: Boolean): ApiResponse<List<FlashSaleDto>> = withContext(Dispatchers.IO) {
        db.ensureSeeded()
        val list = (if (activeOnly) db.flashSaleDao().getActive() else db.flashSaleDao().getAll())
            .map { it.toDto() }
        ApiResponse(success = true, data = list)
    }

    override suspend fun saveFlashSale(flashSale: FlashSaleDto): ApiResponse<FlashSaleDto> = withContext(Dispatchers.IO) {
        val caller = requireSignedInUser()
            ?: return@withContext ApiResponse(success = false, message = "Permission denied.")
        val shopId = flashSale.shopId.ifBlank { resolveActiveSellerShopId() }
            ?: return@withContext ApiResponse(success = false, message = "No shop associated with current account")
        if (!caller.ownsShop(shopId)) {
            return@withContext ApiResponse(success = false, message = "Permission denied.")
        }
        val shop = db.shopDao().getShopById(shopId)
        val prod = db.productDao().getProductById(flashSale.productId)
            ?: return@withContext ApiResponse(success = false, message = "Product not found")
        if (prod.shopId != shopId && !caller.isAdmin()) {
            return@withContext ApiResponse(success = false, message = "Permission denied.")
        }
        if (flashSale.salePrice <= 0.0 || flashSale.salePrice >= prod.price) {
            return@withContext ApiResponse(success = false, message = "Sale price must be lower than original price.")
        }
        val discountPct = kotlin.math.round(((prod.price - flashSale.salePrice) / prod.price) * 100.0).toInt().coerceIn(1, 95)
        val entity = FlashSaleEntity(
            id = flashSale.id.ifBlank { "fs_${System.currentTimeMillis()}" },
            shopId = shopId,
            shopName = shop?.name ?: prod.shopName,
            productId = prod.id,
            productName = prod.name,
            productImageUrl = prod.primaryImageUrl,
            categoryId = prod.categoryId,
            originalPrice = prod.price,
            salePrice = flashSale.salePrice,
            discountPercent = discountPct,
            stockCap = flashSale.stockCap.coerceIn(1, maxOf(prod.stockQuantity, flashSale.stockCap)),
            claimedCount = flashSale.claimedCount,
            startAt = flashSale.startAt.ifBlank { System.currentTimeMillis().toString() },
            endAt = flashSale.endAt.ifBlank { (System.currentTimeMillis() + 6L * 3600_000L).toString() },
            isActive = flashSale.isActive
        )
        db.flashSaleDao().insert(entity)
        ApiResponse(success = true, data = entity.toDto())
    }

    override suspend fun deleteFlashSale(id: String): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        val caller = requireSignedInUser()
            ?: return@withContext ApiResponse(success = false, data = false, message = "Permission denied.")
        val existing = db.flashSaleDao().getById(id)
        if (existing != null && !caller.ownsShop(existing.shopId)) {
            return@withContext ApiResponse(success = false, data = false, message = "Permission denied.")
        }
        db.flashSaleDao().deleteById(id)
        ApiResponse(success = true, data = true)
    }

    // ==================== PHASE 5: SELLER ANALYTICS ====================

    override suspend fun getSellerAnalytics(rangeDays: Int): ApiResponse<SellerAnalyticsDto> = withContext(Dispatchers.IO) {
        db.ensureSeeded()
        val shopId = resolveActiveSellerShopId()
            ?: return@withContext ApiResponse(success = false, message = "No shop associated with current account")
        val settings = db.adminSettingsDao().getSettings() ?: AdminSettingsEntity()
        val upgrades = db.subscriptionUpgradeDao().getByShop(shopId)
        val approvedUpgrade = upgrades.firstOrNull { it.status == "APPROVED" }
        val plans = db.subscriptionPlanDao().getAllPlans()
        val activePlan = approvedUpgrade?.let { u -> plans.firstOrNull { it.id == u.planId } }
        val allowedMaxDays = when {
            activePlan != null -> activePlan.analyticsDays
            !settings.isSubscriptionModeEnabled || settings.isPilotMode -> 90
            else -> plans.firstOrNull { it.monthlyPrice == 0.0 }?.analyticsDays ?: 7
        }
        val isProLocked = rangeDays > allowedMaxDays

        val nowMs = System.currentTimeMillis()
        val dayMs = 86_400_000L
        val effectiveDays = minOf(rangeDays, allowedMaxDays).coerceAtLeast(1)
        val windowStart = nowMs - effectiveDays.toLong() * dayMs

        val allOrders = db.orderDao().getShopOrders(shopId)
        val ordersInWindow = allOrders.filter { o ->
            val ts = EthiopianMarketplaceUtils.resolveTimestampMillis(o.createdAt, nowMs) ?: nowMs
            ts in windowStart..nowMs
        }
        val validOrders = ordersInWindow.filter { !it.orderStatus.equals("Cancelled", true) }
        val verifiedRevenue = validOrders.filter { it.paymentStatus == "VERIFIED" }.sumOf { it.totalAmount }

        val bucketCount = if (effectiveDays <= 7) 7 else 4
        val bucketSpan = (effectiveDays.toLong() * dayMs / bucketCount).coerceAtLeast(1L)
        val dayFmt = SimpleDateFormat("EEE", Locale.US)
        val revLabels = (0 until bucketCount).map { idx ->
            if (effectiveDays <= 7) dayFmt.format(Date(windowStart + idx * bucketSpan + bucketSpan / 2L)) else "W${idx + 1}"
        }
        val revBuckets = DoubleArray(bucketCount)
        validOrders.forEach { o ->
            val ts = EthiopianMarketplaceUtils.resolveTimestampMillis(o.createdAt, nowMs) ?: nowMs
            val idx = (((ts - windowStart) / bucketSpan).toInt()).coerceIn(0, bucketCount - 1)
            revBuckets[idx] += o.totalAmount
        }

        val statusGroups = listOf("Pending", "Confirmed", "Dispatched", "Delivered")
        val statusCounts = statusGroups.map { g ->
            ordersInWindow.count { o ->
                when (g) {
                    "Pending" -> o.paymentStatus == "PENDING_VERIFICATION" || o.orderStatus.contains("Awaiting", true)
                    "Confirmed" -> o.orderStatus.equals("Confirmed", true) || o.orderStatus.equals("Processing", true)
                    "Dispatched" -> o.orderStatus.equals("Dispatched", true) || o.orderStatus.contains("Delivery", true)
                    else -> o.orderStatus.equals("Delivered", true)
                }
            }.toDouble()
        }

        val products = db.productDao().getProductsByShop(shopId)
        val productRev = mutableMapOf<String, Double>()
        val productUnits = mutableMapOf<String, Int>()
        validOrders.forEach { o ->
            try {
                val arr = JSONArray(o.itemsJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val pId = obj.optString("productId")
                    val qty = obj.optInt("quantity", 1)
                    val rev = obj.optDouble("totalPrice", obj.optDouble("unitPrice", 0.0) * qty)
                    productRev[pId] = (productRev[pId] ?: 0.0) + rev
                    productUnits[pId] = (productUnits[pId] ?: 0) + qty
                }
            } catch (_: Exception) {}
        }
        val topProducts = products.map { p ->
            val rev = productRev[p.id] ?: 0.0
            val units = productUnits[p.id] ?: 0
            SellerProductMetricDto(
                productId = p.id,
                productName = p.name,
                imageUrl = p.primaryImageUrl,
                revenue = rev,
                unitsSold = units,
                sparkline = listOf(0.2f, 0.4f, 0.35f, 0.7f, if (rev > 0) 1f else 0.3f)
            )
        }.sortedWith(compareByDescending<SellerProductMetricDto> { it.revenue }.thenByDescending { it.unitsSold }).take(5)

        val areas = validOrders.groupBy { ord ->
            ord.deliveryAddress.split(",").map { it.trim() }.firstOrNull { part ->
                part.equals("Bole", true) || part.equals("Kirkos", true) || part.equals("Yeka", true) ||
                    part.equals("Arada", true) || part.equals("Lideta", true) || part.equals("Nifas Silk", true)
            } ?: ord.deliveryAddress.substringAfterLast(",").trim().ifBlank { "Addis Ababa" }
        }.map { (area, list) ->
            SellerAreaMetricDto(
                subCity = area,
                customersCount = list.map { it.customerUserId }.distinct().size,
                ordersCount = list.size,
                revenue = list.sumOf { it.totalAmount }
            )
        }.sortedByDescending { it.revenue }.take(5)

        val paymentSplit = validOrders.groupBy { it.paymentMethodTitle.ifBlank { it.paymentMethodType } }
            .map { (title, list) -> OwnerChartSlice(title, title, list.sumOf { it.totalAmount }) }

        val productViews = db.analyticsEventDao().countByShopTypeSince(shopId, "PRODUCT_VIEW", windowStart)
        val cartAdds = db.analyticsEventDao().countByShopTypeSince(shopId, "CART_ADD", windowStart)
        val completedCount = validOrders.count { it.paymentStatus == "VERIFIED" }

        ApiResponse(
            success = true,
            data = SellerAnalyticsDto(
                rangeDays = rangeDays,
                allowedMaxDays = allowedMaxDays,
                isProLocked = isProLocked,
                totalRevenue = verifiedRevenue,
                totalOrders = ordersInWindow.size,
                productViews = productViews,
                cartAdds = cartAdds,
                completedOrders = completedCount,
                revenueLabels = revLabels,
                revenueValues = revBuckets.toList(),
                statusLabels = statusGroups,
                statusCounts = statusCounts,
                topProducts = topProducts,
                customerAreas = areas,
                paymentSplit = paymentSplit
            )
        )
    }

    // ==================== PHASE 5: REVIEWS 2.0 & MODERATION 2.0 ====================

    override suspend fun toggleReviewHelpful(reviewId: String): ApiResponse<ReviewDto> = withContext(Dispatchers.IO) {
        db.ensureSeeded()
        val currentUserId = resolveCurrentUser()?.id ?: "user_guest"
        val review = db.reviewDao().getById(reviewId)
            ?: return@withContext ApiResponse(success = false, message = "Review not found")
        val voters = review.helpfulUserIdsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableSet()
        val nextCount = if (voters.contains(currentUserId)) {
            voters.remove(currentUserId)
            maxOf(0, review.helpfulCount - 1)
        } else {
            voters.add(currentUserId)
            review.helpfulCount + 1
        }
        val updated = review.copy(helpfulCount = nextCount, helpfulUserIdsCsv = voters.joinToString(","))
        db.reviewDao().update(updated)
        ApiResponse(success = true, data = updated.toDto(currentUserId))
    }

    override suspend fun replyToReview(reviewId: String, payload: Map<String, String>): ApiResponse<ReviewDto> = withContext(Dispatchers.IO) {
        val caller = requireSignedInUser()
            ?: return@withContext ApiResponse(success = false, message = "Only the shop owner can reply to this review.")
        val reply = payload["reply"]?.trim().orEmpty()
        if (reply.isBlank()) {
            return@withContext ApiResponse(success = false, message = "Reply cannot be empty.")
        }
        val review = db.reviewDao().getById(reviewId)
            ?: return@withContext ApiResponse(success = false, message = "Review not found")
        val reviewShopId = when (review.targetType.uppercase()) {
            "PRODUCT" -> db.productDao().getProductById(review.targetId)?.shopId.orEmpty()
            "SHOP" -> review.targetId
            else -> ""
        }
        if (!caller.ownsShop(reviewShopId)) {
            return@withContext ApiResponse(success = false, message = "Only the shop owner can reply to this review.")
        }
        val updated = review.copy(sellerReply = reply, sellerReplyAt = "Just now")
        db.reviewDao().update(updated)
        ApiResponse(success = true, data = updated.toDto(caller.id))
    }

    override suspend fun bulkModerateReviews(payload: Map<String, Any>): ApiResponse<Int> = withContext(Dispatchers.IO) {
        val caller = requireSignedInUser()
        if (caller == null || !caller.isAdmin()) {
            return@withContext ApiResponse(success = false, message = "Admin permission required.", data = 0)
        }
        @Suppress("UNCHECKED_CAST")
        val ids = (payload["ids"] as? List<*>)?.mapNotNull { it as? String }.orEmpty()
        val action = (payload["action"] as? String)?.uppercase().orEmpty()
        val note = (payload["note"] as? String)?.trim().orEmpty()
        val status = when (action) {
            "HIDE", "DELETE" -> "HIDDEN"
            "RESTORE" -> "RESTORED"
            else -> "DISMISSED"
        }
        ids.forEach { id ->
            db.flaggedReviewDao().updateStatusWithNote(id, status, note)
        }
        ApiResponse(success = true, data = ids.size)
    }

    // ==================== PHASE 5: SUPPORT INBOX & BROADCASTS ====================

    override suspend fun getTicketMessages(ticketId: String): ApiResponse<List<SupportTicketMessageDto>> = withContext(Dispatchers.IO) {
        val currentUser = resolveCurrentUser()
        val ticket = db.supportTicketDao().getById(ticketId)
        if (ticket != null && (currentUser == null || (!currentUser.isAdmin() && ticket.userId != currentUser.id))) {
            return@withContext ApiResponse(success = false, message = "Permission denied.", data = emptyList())
        }
        val isAdmin = currentUser?.isAdmin() == true
        val messages = db.supportTicketMessageDao().getByTicket(ticketId)
            .filter { isAdmin || !it.isInternalNote }
            .map { it.toDto() }
        ApiResponse(success = true, data = messages)
    }

    override suspend fun sendTicketMessage(ticketId: String, payload: Map<String, Any>): ApiResponse<SupportTicketMessageDto> = withContext(Dispatchers.IO) {
        val currentUser = requireSignedInUser()
            ?: return@withContext ApiResponse(success = false, message = "No active user session")
        val ticket = db.supportTicketDao().getById(ticketId)
            ?: return@withContext ApiResponse(success = false, message = "Support ticket not found")
        if (!currentUser.isAdmin() && ticket.userId != currentUser.id) {
            return@withContext ApiResponse(success = false, message = "Permission denied.")
        }
        val text = (payload["message"] as? String)?.trim().orEmpty()
        if (text.isBlank()) {
            return@withContext ApiResponse(success = false, message = "Message cannot be empty.")
        }
        val isInternalNote = payload["isInternalNote"] as? Boolean ?: false
        if (isInternalNote && !currentUser.isAdmin()) {
            return@withContext ApiResponse(success = false, message = "Only admins can add internal notes.")
        }
        val senderRole = when {
            currentUser.isAdmin() -> "ADMIN"
            currentUser.activeRole.equals("SELLER", ignoreCase = true) -> "SELLER"
            else -> "BUYER"
        }
        val now = getCurrentTime()
        val entity = SupportTicketMessageEntity(
            id = "tmsg_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}",
            ticketId = ticketId,
            senderRole = senderRole,
            senderName = currentUser.name,
            message = text,
            isInternalNote = isInternalNote && currentUser.isAdmin(),
            createdAt = now
        )
        db.supportTicketMessageDao().insert(entity)
        val nextStatus = if (senderRole == "ADMIN" && !isInternalNote && ticket.status == "OPEN") "PENDING" else ticket.status
        db.supportTicketDao().update(ticket.copy(status = nextStatus, updatedAt = now))
        ApiResponse(success = true, data = entity.toDto())
    }

    override suspend fun updateSupportTicket(ticketId: String, payload: Map<String, String>): ApiResponse<SupportTicketDto> = withContext(Dispatchers.IO) {
        val caller = requireSignedInUser()
        if (caller == null || !caller.isAdmin()) {
            return@withContext ApiResponse(success = false, message = "Admin permission required.")
        }
        val ticket = db.supportTicketDao().getById(ticketId)
            ?: return@withContext ApiResponse(success = false, message = "Support ticket not found")
        val requestedStatus = payload["status"]?.trim()?.uppercase()?.takeIf { it.isNotBlank() }
        if (requestedStatus != null && requestedStatus !in setOf("OPEN", "PENDING", "RESOLVED", "CLOSED")) {
            return@withContext ApiResponse(success = false, message = "Permission denied.")
        }
        val updated = ticket.copy(
            status = requestedStatus ?: ticket.status,
            priority = payload["priority"]?.takeIf { it.isNotBlank() } ?: ticket.priority,
            assignedTo = payload["assignedTo"] ?: ticket.assignedTo,
            resolutionSummary = payload["resolutionSummary"] ?: ticket.resolutionSummary,
            updatedAt = getCurrentTime()
        )
        db.supportTicketDao().update(updated)
        if (updated.status == "RESOLVED") {
            db.notificationDao().insert(
                NotificationEntity(
                    id = "notif_tkt_${System.currentTimeMillis()}",
                    userId = ticket.userId,
                    type = "SYSTEM",
                    title = "Support Ticket Resolved",
                    body = "${ticket.subject}: ${updated.resolutionSummary.ifBlank { "Marked as resolved by Suqi Support." }}",
                    timestamp = "Just now",
                    isRead = false
                )
            )
        }
        ApiResponse(success = true, data = updated.toDto())
    }

    override suspend fun getAdminBroadcasts(): ApiResponse<List<AdminBroadcastDto>> = withContext(Dispatchers.IO) {
        db.ensureSeeded()
        ApiResponse(success = true, data = db.adminBroadcastDao().getAll().map { it.toDto() })
    }
}

// Extension mapper functions
fun UserEntity.toDto() = UserDto(
    id = id, name = name, phone = phone, email = email, avatarUrl = avatarUrl,
    role = role, preferredLanguage = preferredLanguage, region = region, city = city,
    subCity = subCity, shopId = shopId, defaultAddressId = defaultAddressId,
    adminTier = adminTier, gender = gender, birthday = birthday,
    activeRole = activeRole.ifBlank { role }, isActiveSession = isActiveSession, createdAt = createdAt
)

fun ShopEntity.toDto(isFollowing: Boolean = this.isFollowing) = ShopDto(
    id = id, ownerUserId = ownerUserId, name = name, slug = slug, logoUrl = logoUrl,
    coverUrl = coverUrl, category = category, shopType = shopType, description = description,
    rating = rating, reviewCount = reviewCount, isVerified = isVerified,
    verificationStatus = verificationStatus, phone = phone, telegramLink = telegramLink,
    whatsappLink = whatsappLink, address = address, region = region, city = city,
    subCity = subCity, followersCount = followersCount, productCount = productCount,
    isVacationMode = isVacationMode, acceptsOrders = acceptsOrders, isFollowing = isFollowing,
    requireReceiptScreenshot = requireReceiptScreenshot, businessHoursJson = businessHoursJson,
    vacationReturnDate = vacationReturnDate, vacationNote = vacationNote,
    freeDeliveryThreshold = freeDeliveryThreshold, coverageSubCitiesCsv = coverageSubCitiesCsv,
    verificationDocsJson = verificationDocsJson, verificationRejectionReason = verificationRejectionReason,
    createdAt = createdAt,
    galleryJson = galleryJson, instagramLink = instagramLink, tiktokLink = tiktokLink, websiteUrl = websiteUrl,
    returnPolicy = returnPolicy, deliveryNotesPolicy = deliveryNotesPolicy,
    standardCutoffTime = standardCutoffTime, expressCutoffTime = expressCutoffTime
)

fun ProductEntity.toDto(isWishlisted: Boolean = this.isWishlisted) = ProductDto(
    id = id, shopId = shopId, shopName = shopName, shopVerified = shopVerified,
    name = name, nameAmharic = nameAmharic, nameAfaanOromo = nameAfaanOromo,
    description = description, descriptionAmharic = descriptionAmharic,
    descriptionAfaanOromo = descriptionAfaanOromo, primaryImageUrl = primaryImageUrl,
    extraImages = if (extraImagesCsv.isBlank()) emptyList() else extraImagesCsv.split(","),
    categoryId = categoryId, subCategory = subCategory, price = price,
    originalPrice = originalPrice, discountPercent = discountPercent,
    stockQuantity = stockQuantity, stockHoldEnabled = stockHoldEnabled,
    rating = rating, reviewCount = reviewCount, soldCount = soldCount,
    colors = if (colorsCsv.isBlank()) emptyList() else colorsCsv.split(","),
    sizesOrSpecs = if (specsCsv.isBlank()) emptyList() else specsCsv.split(","),
    status = status, isWishlisted = isWishlisted,
    details = parseProductDetails(detailsJson) ?: legacyProductDetails(
        primaryImageUrl = primaryImageUrl,
        extraImages = if (extraImagesCsv.isBlank()) emptyList() else extraImagesCsv.split(","),
        colors = if (colorsCsv.isBlank()) emptyList() else colorsCsv.split(","),
        sizesOrSpecs = if (specsCsv.isBlank()) emptyList() else specsCsv.split(",")
    )
)

fun CartItemEntity.toDto() = CartItemDto(
    id = id, userId = userId, productId = productId, shopId = shopId,
    shopName = shopName, shopVerified = shopVerified, productName = productName,
    productImageUrl = productImageUrl, unitPrice = unitPrice, quantity = quantity,
    selectedVariation = selectedVariation, stockAvailable = stockAvailable
)

fun OrderEntity.toDto(): OrderDto {
    val items = mutableListOf<OrderItemDto>()
    try {
        val arr = JSONArray(itemsJson)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            items.add(
                OrderItemDto(
                    productId = o.optString("productId"),
                    productName = o.optString("productName"),
                    productImageUrl = o.optString("productImageUrl"),
                    variation = o.optString("variation"),
                    unitPrice = o.optDouble("unitPrice", 0.0),
                    quantity = o.optInt("quantity", 1),
                    totalPrice = o.optDouble("totalPrice", 0.0)
                )
            )
        }
    } catch (_: Exception) {}

    return OrderDto(
        id = id, orderReference = orderReference, customerUserId = customerUserId,
        customerName = customerName, customerPhone = customerPhone,
        deliveryAddress = deliveryAddress, shopId = shopId, shopName = shopName,
        items = items, subtotal = subtotal, deliveryFee = deliveryFee,
        discountAmount = discountAmount, totalAmount = totalAmount,
        promoCodeUsed = promoCodeUsed, deliveryOptionTitle = deliveryOptionTitle,
        deliveryOptionType = deliveryOptionType, deliveryEstimatedDays = deliveryEstimatedDays,
        paymentMethodTitle = paymentMethodTitle, paymentMethodType = paymentMethodType,
        paymentMethodAccount = paymentMethodAccount, transactionReference = transactionReference,
        paymentStatus = paymentStatus, rejectionReason = rejectionReason,
        verificationNotes = verificationNotes, verifiedBy = verifiedBy,
        verifiedAt = verifiedAt, orderStatus = orderStatus, createdAt = createdAt,
        receiptImageUri = receiptImageUri, payerIdentifier = payerIdentifier,
        sellerNotes = sellerNotes, courierName = courierName,
        courierPhone = courierPhone, dispatchedAt = dispatchedAt,
        deliveredAt = deliveredAt, transferredAmount = transferredAmount
    )
}

fun ShopPaymentMethodEntity.toDto() = ShopPaymentMethodDto(
    id = id, shopId = shopId, providerType = providerType, bankName = bankName,
    accountName = accountName, accountNumber = accountNumber, instructions = instructions,
    isRequired = isRequired, isEnabled = isEnabled
)

fun ShopPaymentMethodDto.toEntity() = ShopPaymentMethodEntity(
    id = id, shopId = shopId, providerType = providerType, bankName = bankName,
    accountName = accountName, accountNumber = accountNumber, instructions = instructions,
    isRequired = isRequired, isEnabled = isEnabled
)

fun ShopDeliveryOptionEntity.toDto() = ShopDeliveryOptionDto(
    id = id, shopId = shopId, optionType = optionType, title = title,
    fee = fee, estimatedTime = estimatedTime, isEnabled = isEnabled
)

fun PromoCodeEntity.toDto() = PromoCodeDto(
    id = id, shopId = shopId, code = code, discountType = discountType,
    discountValue = discountValue, minOrderAmount = minOrderAmount,
    validUntil = validUntil, isActive = isActive,
    scopeType = scopeType,
    scopeTargetIds = if (scopeTargetIdsCsv.isBlank()) emptyList() else scopeTargetIdsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() },
    maxUsesTotal = maxUsesTotal,
    usesCount = usesCount,
    perCustomerLimit = perCustomerLimit,
    startDate = startDate,
    ordersCount = ordersCount,
    revenueGenerated = revenueGenerated
)

fun PromoCodeDto.toEntity() = PromoCodeEntity(
    id = id, shopId = shopId, code = code.trim().uppercase(), discountType = discountType,
    discountValue = discountValue, minOrderAmount = minOrderAmount,
    validUntil = validUntil, isActive = isActive,
    scopeType = scopeType,
    scopeTargetIdsCsv = scopeTargetIds.filter { it.isNotBlank() }.joinToString(","),
    maxUsesTotal = maxUsesTotal,
    usesCount = usesCount,
    perCustomerLimit = perCustomerLimit,
    startDate = startDate,
    ordersCount = ordersCount,
    revenueGenerated = revenueGenerated
)

fun ReviewEntity.toDto(currentUserId: String = ""): ReviewDto {
    val photos = mutableListOf<String>()
    try {
        val arr = JSONArray(photosJson)
        for (i in 0 until arr.length()) {
            arr.optString(i).takeIf { it.isNotBlank() }?.let { photos.add(it) }
        }
    } catch (_: Exception) {}
    val voters = helpfulUserIdsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
    return ReviewDto(
        id = id,
        userId = userId,
        userName = if (isAnonymous) "Verified Shopper" else userName,
        userAvatar = if (isAnonymous) "" else userAvatar,
        targetType = targetType,
        targetId = targetId,
        rating = rating,
        comment = comment,
        isVerifiedPurchase = isVerifiedPurchase,
        date = date,
        tags = if (tagsCsv.isBlank()) emptyList() else tagsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() },
        photos = photos,
        isAnonymous = isAnonymous,
        helpfulCount = helpfulCount,
        isHelpfulByCurrentUser = currentUserId.isNotBlank() && currentUserId in voters,
        sellerReply = sellerReply,
        sellerReplyAt = sellerReplyAt
    )
}

fun ChatMessageEntity.toDto() = ChatMessageDto(
    id = id, conversationId = conversationId, shopId = shopId,
    shopName = shopName, senderType = senderType, message = message,
    timestamp = timestamp, productContextName = productContextName,
    productContextPrice = productContextPrice, productContextImage = productContextImage,
    isRead = isRead, imageUrl = imageUrl,
    orderContextRef = orderContextRef, orderContextStatus = orderContextStatus,
    orderContextTotal = orderContextTotal, threadCategory = threadCategory,
    customerUserId = customerUserId
)

fun NotificationEntity.toDto() = NotificationDto(
    id = id, type = type, title = title, body = body, timestamp = timestamp, isRead = isRead
)

fun SavedAddressEntity.toDto() = SavedAddressDto(
    id = id, fullName = fullName, phone = phone, region = region, city = city,
    subCity = subCity, streetOrLandmark = streetOrLandmark,
    deliveryInstructions = deliveryInstructions, isDefault = isDefault
)

fun CategoryEntity.toDto() = CategoryDto(
    id = id, name = name, nameAmharic = nameAmharic, nameAfaanOromo = nameAfaanOromo,
    icon = icon, imageUrl = imageUrl.ifBlank { MarketplaceCategories.imageUrlForId(id) },
    subCategories = if (subCategoriesCsv.isBlank()) emptyList() else subCategoriesCsv.split(",").map { it.trim() }.filter { it.isNotBlank() },
    isActive = isActive, sortOrder = sortOrder,
    linkedItemTypes = if (linkedItemTypesCsv.isBlank()) emptyList() else linkedItemTypesCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
)

fun PromoBannerEntity.toDto() = PromoBannerDto(
    id = id, badge = badge, title = title, highlight = highlight, subtitle = subtitle,
    buttonText = buttonText, imageUrl = imageUrl, targetCategory = targetCategory,
    targetName = targetName, isActive = isActive, sortOrder = sortOrder,
    badgeIcon = badgeIcon, destinationType = destinationType, targetId = targetId,
    startDate = startDate, endDate = endDate, audience = audience
)

fun ItemTypeEntity.toDto() = ItemTypeDto(
    id = id, label = label, emoji = emoji, categoryId = categoryId, group = groupName,
    attributes = parseAttributesJson(attributesJson), sortOrder = sortOrder
)

fun FlaggedReviewEntity.toDto() = FlaggedReviewDto(
    id = id, authorName = authorName, reason = reason, reviewText = reviewText,
    status = status, createdAt = createdAt,
    reviewId = reviewId, productName = productName, rating = rating, moderatorNote = moderatorNote
)

fun ProductQuestionEntity.toDto() = ProductQuestionDto(
    id = id, productId = productId, shopId = shopId, askerName = askerName,
    question = question, answer = answer, answeredByShop = answeredByShop, createdAt = createdAt
)

fun SubscriptionPlanEntity.toDto() = SubscriptionPlanDto(
    id = id, name = name, monthlyPrice = monthlyPrice, yearlyPrice = yearlyPrice,
    maxProducts = maxProducts, featuresCsv = featuresCsv, isPopular = isPopular, isActive = isActive,
    maxPromoCodes = maxPromoCodes, analyticsDays = analyticsDays
)

fun SupportTicketEntity.toDto() = SupportTicketDto(
    id = id, userId = userId, orderId = orderId, category = category,
    subject = subject, description = description, status = status, createdAt = createdAt,
    priority = priority, requesterRole = requesterRole, assignedTo = assignedTo,
    resolutionSummary = resolutionSummary, updatedAt = updatedAt
)

fun ReturnRequestEntity.toDto(): ReturnRequestDto {
    val parsedItems = mutableListOf<ReturnItemDto>()
    try {
        val arr = JSONArray(itemsJson)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            parsedItems += ReturnItemDto(
                productId = o.optString("productId"),
                productName = o.optString("productName"),
                productImageUrl = o.optString("productImageUrl"),
                quantity = o.optInt("quantity", 1),
                unitPrice = o.optDouble("unitPrice", 0.0)
            )
        }
    } catch (_: Exception) {}
    val parsedPhotos = mutableListOf<String>()
    try {
        val arr = JSONArray(photosJson)
        for (i in 0 until arr.length()) {
            arr.optString(i).takeIf { it.isNotBlank() }?.let { parsedPhotos += it }
        }
    } catch (_: Exception) {}
    return ReturnRequestDto(
        id = id, orderId = orderId, orderReference = orderReference, shopId = shopId, shopName = shopName,
        customerUserId = customerUserId, customerName = customerName, items = parsedItems,
        refundAmount = refundAmount, reason = reason, comment = comment, photos = parsedPhotos,
        resolution = resolution, status = status, sellerNote = sellerNote, refundMethod = refundMethod,
        createdAt = createdAt, updatedAt = updatedAt
    )
}

fun PayoutRequestEntity.toDto() = PayoutRequestDto(
    id = id, shopId = shopId, shopName = shopName, amount = amount, providerType = providerType,
    bankName = bankName, accountName = accountName, accountNumber = accountNumber,
    status = status, referenceCode = referenceCode, note = note, createdAt = createdAt, processedAt = processedAt
)

fun CommissionConfigEntity.toDto() = CommissionRuleDto(
    id = id, categoryId = categoryId, categoryName = categoryName,
    ratePercent = ratePercent, minPayoutAmount = minPayoutAmount
)

fun SubscriptionUpgradeEntity.toDto() = SubscriptionUpgradeDto(
    id = id, shopId = shopId, shopName = shopName, planId = planId, planName = planName,
    billingCycle = billingCycle, amount = amount, paymentMethodTitle = paymentMethodTitle,
    transactionReference = transactionReference, receiptImageUri = receiptImageUri,
    status = status, adminNote = adminNote, createdAt = createdAt, reviewedAt = reviewedAt
)

fun FlashSaleEntity.toDto() = FlashSaleDto(
    id = id, shopId = shopId, shopName = shopName, productId = productId, productName = productName,
    productImageUrl = productImageUrl, categoryId = categoryId, originalPrice = originalPrice,
    salePrice = salePrice, discountPercent = discountPercent, stockCap = stockCap,
    claimedCount = claimedCount, startAt = startAt, endAt = endAt, isActive = isActive
)

fun SupportTicketMessageEntity.toDto() = SupportTicketMessageDto(
    id = id, ticketId = ticketId, senderRole = senderRole, senderName = senderName,
    message = message, isInternalNote = isInternalNote, createdAt = createdAt
)

fun AdminBroadcastEntity.toDto() = AdminBroadcastDto(
    id = id, title = title, body = body, audience = audience, imageUrl = imageUrl,
    destinationType = destinationType, targetId = targetId, scheduledAt = scheduledAt,
    status = status, sentCount = sentCount, openedCount = openedCount,
    isTemplate = isTemplate, createdAt = createdAt
)
