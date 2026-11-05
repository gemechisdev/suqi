package com.example.data.backend

import com.example.data.api.SuqiApiService
import com.example.data.local.*
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class BuiltinSqliteBackend(private val db: SuqiDatabase) : SuqiApiService {

    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.getDefault())
        return sdf.format(Date())
    }

    override suspend fun loginWithTelegram(payload: Map<String, String>): ApiResponse<UserDto> = withContext(Dispatchers.IO) {
        val user = db.userDao().getFirstUser() ?: UserEntity(
            id = "user_tg_${System.currentTimeMillis()}",
            name = payload["name"] ?: "Telegram User",
            phone = payload["phone"] ?: "+251 900 000 000",
            email = "",
            avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
            role = "CUSTOMER",
            preferredLanguage = "English",
            region = "Addis Ababa",
            city = "Addis Ababa",
            subCity = "Bole"
        ).also { db.userDao().insertUser(it) }
        ApiResponse(success = true, data = user.toDto())
    }

    override suspend fun loginWithGoogle(payload: Map<String, String>): ApiResponse<UserDto> = withContext(Dispatchers.IO) {
        val user = db.userDao().getFirstUser() ?: UserEntity(
            id = "user_g_${System.currentTimeMillis()}",
            name = payload["name"] ?: "Google User",
            phone = "+251 912 345 678",
            email = payload["email"] ?: "user@gmail.com",
            avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
            role = "CUSTOMER",
            preferredLanguage = "English",
            region = "Addis Ababa",
            city = "Addis Ababa",
            subCity = "Bole"
        ).also { db.userDao().insertUser(it) }
        ApiResponse(success = true, data = user.toDto())
    }

    override suspend fun loginAsGuest(): ApiResponse<UserDto> = withContext(Dispatchers.IO) {
        val user = db.userDao().getFirstUser() ?: UserEntity(
            id = "user_guest",
            name = "Guest Shopper",
            phone = "+251 900 000 000",
            email = "",
            avatarUrl = "",
            role = "CUSTOMER",
            preferredLanguage = "English",
            region = "Addis Ababa",
            city = "Addis Ababa",
            subCity = "Bole"
        ).also { db.userDao().insertUser(it) }
        ApiResponse(success = true, data = user.toDto())
    }

    override suspend fun getCurrentUser(): ApiResponse<UserDto> = withContext(Dispatchers.IO) {
        val user = db.userDao().getFirstUser()
        ApiResponse(success = user != null, data = user?.toDto())
    }

    override suspend fun upgradeToSeller(payload: Map<String, String>): ApiResponse<ShopDto> = withContext(Dispatchers.IO) {
        val currentUser = db.userDao().getFirstUser()
        val userId = currentUser?.id ?: "user_fitsum"
        val shopId = "shop_${System.currentTimeMillis()}"
        val newShop = ShopEntity(
            id = shopId,
            ownerUserId = userId,
            name = payload["shopName"] ?: "New Shop",
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
            phone = payload["phone"] ?: "+251 900 000 000",
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
            isFollowing = false
        )
        db.shopDao().insertShop(newShop)

        // Upgrade user role to SELLER
        if (currentUser != null) {
            db.userDao().updateUser(currentUser.copy(role = "SELLER", shopId = shopId))
        }

        // Add default payment method and delivery options for new shop
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

        ApiResponse(success = true, data = newShop.toDto())
    }

    override suspend fun getProducts(
        query: String?,
        categoryId: String?,
        subCategory: String?,
        shopId: String?,
        inStockOnly: Boolean,
        sortBy: String
    ): ApiResponse<List<ProductDto>> = withContext(Dispatchers.IO) {
        var list = db.productDao().getAllProducts()

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
            else -> list // "POPULAR" default
        }

        ApiResponse(success = true, data = sorted.map { it.toDto() })
    }

    override suspend fun getProductDetails(id: String): ApiResponse<ProductDto> = withContext(Dispatchers.IO) {
        val prod = db.productDao().getProductById(id)
        if (prod != null) {
            ApiResponse(success = true, data = prod.toDto())
        } else {
            ApiResponse(success = false, message = "Product not found")
        }
    }

    override suspend fun toggleWishlist(id: String): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        val prod = db.productDao().getProductById(id)
        if (prod != null) {
            val newState = !prod.isWishlisted
            db.productDao().updateWishlist(id, newState)
            ApiResponse(success = true, data = newState)
        } else {
            ApiResponse(success = false, data = false)
        }
    }

    override suspend fun getShops(
        filter: String,
        category: String?,
        query: String?
    ): ApiResponse<List<ShopDto>> = withContext(Dispatchers.IO) {
        var list = db.shopDao().getAllShops()

        if (!query.isNullOrBlank()) {
            val q = query.trim().lowercase()
            list = list.filter { it.name.lowercase().contains(q) || it.category.lowercase().contains(q) }
        }

        if (!category.isNullOrBlank() && category != "all") {
            list = list.filter { it.category.equals(category, ignoreCase = true) }
        }

        list = when (filter.uppercase()) {
            "VERIFIED" -> list.filter { it.isVerified }
            "TOP_RATED" -> list.filter { it.rating >= 4.7 }
            "FOLLOWING" -> list.filter { it.isFollowing }
            "NEARBY" -> list.filter { it.subCity.equals("Bole", ignoreCase = true) }
            else -> list
        }

        ApiResponse(success = true, data = list.map { it.toDto() })
    }

    override suspend fun getShopDetails(id: String): ApiResponse<ShopDto> = withContext(Dispatchers.IO) {
        val shop = db.shopDao().getShopById(id)
        if (shop != null) {
            ApiResponse(success = true, data = shop.toDto())
        } else {
            ApiResponse(success = false, message = "Shop not found")
        }
    }

    override suspend fun toggleFollowShop(id: String): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        val shop = db.shopDao().getShopById(id)
        if (shop != null) {
            val newState = !shop.isFollowing
            val newCount = if (newState) shop.followersCount + 1 else maxOf(0, shop.followersCount - 1)
            db.shopDao().updateShop(shop.copy(isFollowing = newState, followersCount = newCount))
            ApiResponse(success = true, data = newState)
        } else {
            ApiResponse(success = false, data = false)
        }
    }

    override suspend fun getCategories(): ApiResponse<List<CategoryDto>> = withContext(Dispatchers.IO) {
        val list = db.categoryDao().getAllCategories().map { it.toDto() }
        ApiResponse(success = true, data = list)
    }

    override suspend fun getCartItems(): ApiResponse<List<CartItemDto>> = withContext(Dispatchers.IO) {
        val user = db.userDao().getFirstUser()
        val userId = user?.id ?: "user_fitsum"
        val items = db.cartDao().getCartItems(userId).map { it.toDto() }
        ApiResponse(success = true, data = items)
    }

    override suspend fun addToCart(item: Map<String, Any>): ApiResponse<CartItemDto> = withContext(Dispatchers.IO) {
        val user = db.userDao().getFirstUser()
        val userId = user?.id ?: "user_fitsum"
        val productId = item["productId"] as String
        val quantity = (item["quantity"] as? Number)?.toInt() ?: 1
        val variation = item["selectedVariation"] as? String ?: ""

        val product = db.productDao().getProductById(productId)
            ?: return@withContext ApiResponse(success = false, message = "Product not found")

        // Validate inventory
        if (product.stockQuantity < quantity) {
            return@withContext ApiResponse(
                success = false,
                message = "Only ${product.stockQuantity} items in stock!"
            )
        }

        val cartEntity = CartItemEntity(
            userId = userId,
            productId = productId,
            shopId = product.shopId,
            shopName = product.shopName,
            shopVerified = product.shopVerified,
            productName = product.name,
            productImageUrl = product.primaryImageUrl,
            unitPrice = product.price,
            quantity = quantity,
            selectedVariation = variation,
            stockAvailable = product.stockQuantity
        )
        val id = db.cartDao().insertCartItem(cartEntity)
        ApiResponse(success = true, data = cartEntity.copy(id = id).toDto())
    }

    override suspend fun updateCartQuantity(itemId: Long, quantity: Int): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        if (quantity <= 0) {
            db.cartDao().deleteItem(itemId)
        } else {
            db.cartDao().updateQuantity(itemId, quantity)
        }
        ApiResponse(success = true, data = true)
    }

    override suspend fun removeCartItem(itemId: Long): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        db.cartDao().deleteItem(itemId)
        ApiResponse(success = true, data = true)
    }

    override suspend fun clearCart(): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        val user = db.userDao().getFirstUser()
        val userId = user?.id ?: "user_fitsum"
        db.cartDao().clearAll(userId)
        ApiResponse(success = true, data = true)
    }

    override suspend fun submitOrder(orderData: Map<String, Any>): ApiResponse<OrderDto> = withContext(Dispatchers.IO) {
        val shopId = orderData["shopId"] as String
        val shopName = orderData["shopName"] as? String ?: "Shop"
        val customerName = orderData["customerName"] as? String ?: "Customer"
        val customerPhone = orderData["customerPhone"] as? String ?: "+251 900 000 000"
        val deliveryAddress = orderData["deliveryAddress"] as? String ?: "Addis Ababa"
        val subtotal = (orderData["subtotal"] as Number).toDouble()
        val deliveryFee = (orderData["deliveryFee"] as? Number)?.toDouble() ?: 0.0
        val discountAmount = (orderData["discountAmount"] as? Number)?.toDouble() ?: 0.0
        val totalAmount = subtotal + deliveryFee - discountAmount
        val promoCode = orderData["promoCode"] as? String
        val deliveryOptionTitle = orderData["deliveryOptionTitle"] as? String ?: "Standard Delivery"
        val deliveryOptionType = orderData["deliveryOptionType"] as? String ?: "STANDARD"
        val deliveryEstimatedDays = orderData["deliveryEstimatedDays"] as? String ?: "1-3 days"
        val paymentMethodTitle = orderData["paymentMethodTitle"] as? String ?: "Telebirr"
        val paymentMethodType = orderData["paymentMethodType"] as? String ?: "TELEBIRR"
        val paymentMethodAccount = orderData["paymentMethodAccount"] as? String ?: ""
        val transactionReference = (orderData["transactionReference"] as? String)?.trim() ?: ""

        if (transactionReference.isBlank()) {
            return@withContext ApiResponse(success = false, message = "Transaction reference is required")
        }

        // Ethiopian Fraud Prevention: Prevent duplicate transaction reference submission
        val existingOrderWithRef = db.orderDao().getOrderByTransactionReference(transactionReference)
        if (existingOrderWithRef != null) {
            return@withContext ApiResponse(
                success = false,
                message = "This transaction reference ($transactionReference) has already been submitted for another active order. Please check your bank receipt or SMS confirmation."
            )
        }

        val itemsJson = orderData["itemsJson"] as? String ?: "[]"
        val user = db.userDao().getFirstUser()
        val userId = user?.id ?: "user_fitsum"

        // Generate unique order reference e.g. SUQ-2025-001239
        val refNum = 1234 + (db.orderDao().getAllOrders().size)
        val orderReference = "SUQ-2025-${String.format("%06d", refNum)}"
        val orderId = "ord_${System.currentTimeMillis()}"

        // Validate and reserve stock before order creation
        try {
            val jsonArr = JSONArray(itemsJson)
            for (i in 0 until jsonArr.length()) {
                val obj = jsonArr.getJSONObject(i)
                val pId = obj.getString("productId")
                val qty = obj.getInt("quantity")
                val prod = db.productDao().getProductById(pId)
                if (prod != null) {
                    if (prod.stockQuantity < qty) {
                        return@withContext ApiResponse(
                            success = false,
                            message = "Insufficient stock for ${prod.name}! Only ${prod.stockQuantity} left."
                        )
                    }
                    if (prod.stockHoldEnabled) {
                        db.productDao().decrementStock(pId, qty)
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
            createdAt = getCurrentTime()
        )
        db.orderDao().insertOrder(order)

        // Clear only this shop's items from the cart (multi-vendor independence!)
        db.cartDao().clearShopItems(userId, shopId)

        // Create in-app notification
        db.notificationDao().insert(
            NotificationEntity(
                id = "notif_${System.currentTimeMillis()}",
                userId = userId,
                type = "PAYMENT",
                title = "Order Submitted to $shopName",
                body = "Order #$orderReference total ETB ${totalAmount.toInt()} is pending payment verification.",
                timestamp = "Just now"
            )
        )

        ApiResponse(success = true, data = order.toDto())
    }

    override suspend fun getOrders(): ApiResponse<List<OrderDto>> = withContext(Dispatchers.IO) {
        val user = db.userDao().getFirstUser()
        val userId = user?.id ?: "user_fitsum"
        val orders = db.orderDao().getCustomerOrders(userId).map { it.toDto() }
        ApiResponse(success = true, data = orders)
    }

    override suspend fun getOrderDetails(orderId: String): ApiResponse<OrderDto> = withContext(Dispatchers.IO) {
        val order = db.orderDao().getOrderById(orderId)
        if (order != null) {
            ApiResponse(success = true, data = order.toDto())
        } else {
            ApiResponse(success = false, message = "Order not found")
        }
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

        val updated = order.copy(
            transactionReference = newRef,
            paymentMethodTitle = newMethod,
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
                timestamp = "Just now"
            )
        )

        ApiResponse(success = true, data = updated.toDto())
    }

    override suspend fun cancelOrder(orderId: String): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        val order = db.orderDao().getOrderById(orderId)
            ?: return@withContext ApiResponse(success = false, message = "Order not found")

        // Release held stock
        try {
            val jsonArr = JSONArray(order.itemsJson)
            for (i in 0 until jsonArr.length()) {
                val obj = jsonArr.getJSONObject(i)
                val pId = obj.getString("productId")
                val qty = obj.getInt("quantity")
                db.productDao().incrementStock(pId, qty)
            }
        } catch (_: Exception) {}

        val updated = order.copy(orderStatus = "Cancelled")
        db.orderDao().updateOrder(updated)

        ApiResponse(success = true, data = true)
    }

    override suspend fun getSellerDashboard(): ApiResponse<Map<String, Any>> = withContext(Dispatchers.IO) {
        val currentUser = db.userDao().getFirstUser()
        val shopId = currentUser?.shopId ?: "shop_techhub"

        val orders = db.orderDao().getShopOrders(shopId)
        val pendingCount = orders.count { it.paymentStatus == "PENDING_VERIFICATION" }
        val totalSales = orders.filter { it.paymentStatus == "VERIFIED" }.sumOf { it.totalAmount }
        val products = db.productDao().getProductsByShop(shopId)
        val lowStockCount = products.count { it.stockQuantity in 1..5 }

        val data = mapOf(
            "totalSales" to 248750.0 + totalSales,
            "pendingOrders" to pendingCount + 2,
            "lowStock" to lowStockCount,
            "productCount" to products.size,
            "shopName" to (db.shopDao().getShopById(shopId)?.name ?: "Tech Hub")
        )
        ApiResponse(success = true, data = data)
    }

    override suspend fun getSellerProducts(): ApiResponse<List<ProductDto>> = withContext(Dispatchers.IO) {
        val currentUser = db.userDao().getFirstUser()
        val shopId = currentUser?.shopId ?: "shop_techhub"
        val list = db.productDao().getProductsByShop(shopId).map { it.toDto() }
        ApiResponse(success = true, data = list)
    }

    override suspend fun createProduct(productData: Map<String, Any>): ApiResponse<ProductDto> = withContext(Dispatchers.IO) {
        val currentUser = db.userDao().getFirstUser()
        val shopId = currentUser?.shopId ?: "shop_techhub"
        val shop = db.shopDao().getShopById(shopId)

        val prodId = "prod_${System.currentTimeMillis()}"
        val name = productData["name"] as String
        val price = (productData["price"] as Number).toDouble()
        val discount = (productData["discountPercent"] as? Number)?.toInt() ?: 0
        val stock = (productData["stockQuantity"] as? Number)?.toInt() ?: 10
        val stockHold = productData["stockHoldEnabled"] as? Boolean ?: true
        val colors = productData["colors"] as? String ?: "Black,White"
        val specs = productData["specs"] as? String ?: "Standard"
        val categoryId = productData["categoryId"] as? String ?: "cat_electronics"

        val product = ProductEntity(
            id = prodId,
            shopId = shopId,
            shopName = shop?.name ?: "Tech Hub",
            shopVerified = shop?.isVerified ?: true,
            name = name,
            description = productData["description"] as? String ?: "New product listed on Suqi.",
            primaryImageUrl = productData["imageUrl"] as? String ?: "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600",
            categoryId = categoryId,
            subCategory = productData["subCategory"] as? String ?: "General",
            price = price,
            originalPrice = if (discount > 0) price / (1 - (discount / 100.0)) else price,
            discountPercent = discount,
            stockQuantity = stock,
            stockHoldEnabled = stockHold,
            rating = 5.0,
            reviewCount = 0,
            soldCount = "New",
            colorsCsv = colors,
            specsCsv = specs,
            status = "ACTIVE"
        )
        db.productDao().insertProduct(product)
        ApiResponse(success = true, data = product.toDto())
    }

    override suspend fun updateProductStatus(id: String, status: String): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        val prod = db.productDao().getProductById(id)
        if (prod != null) {
            db.productDao().updateProduct(prod.copy(status = status))
            ApiResponse(success = true, data = true)
        } else {
            ApiResponse(success = false, data = false)
        }
    }

    override suspend fun getSellerOrders(): ApiResponse<List<OrderDto>> = withContext(Dispatchers.IO) {
        val currentUser = db.userDao().getFirstUser()
        val shopId = currentUser?.shopId ?: "shop_techhub"
        val list = db.orderDao().getShopOrders(shopId).map { it.toDto() }
        ApiResponse(success = true, data = list)
    }

    override suspend fun updateSellerOrderStatus(orderId: String, status: String): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        val order = db.orderDao().getOrderById(orderId)
        if (order != null) {
            db.orderDao().updateOrder(order.copy(orderStatus = status))
            ApiResponse(success = true, data = true)
        } else {
            ApiResponse(success = false, data = false)
        }
    }

    override suspend fun getSellerVerificationQueue(): ApiResponse<List<OrderDto>> = withContext(Dispatchers.IO) {
        val currentUser = db.userDao().getFirstUser()
        val shopId = currentUser?.shopId ?: "shop_techhub"
        val list = db.orderDao().getPendingVerificationOrders(shopId).map { it.toDto() }
        ApiResponse(success = true, data = list)
    }

    override suspend fun verifyPayment(orderId: String, payload: Map<String, String>): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        val order = db.orderDao().getOrderById(orderId)
            ?: return@withContext ApiResponse(success = false, message = "Order not found")

        val currentUser = db.userDao().getFirstUser()
        val updated = order.copy(
            paymentStatus = "VERIFIED",
            orderStatus = "Confirmed",
            verifiedBy = currentUser?.name ?: "Shop Manager",
            verifiedAt = getCurrentTime(),
            verificationNotes = payload["notes"] ?: "Verified against receipt"
        )
        db.orderDao().updateOrder(updated)

        // Send customer notification
        db.notificationDao().insert(
            NotificationEntity(
                id = "notif_${System.currentTimeMillis()}",
                userId = order.customerUserId,
                type = "PAYMENT",
                title = "Payment Verified!",
                body = "${order.shopName} has confirmed your payment for Order #${order.orderReference}. Preparing order now.",
                timestamp = "Just now"
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

        // Send customer notification with rejection reason so they can resubmit
        db.notificationDao().insert(
            NotificationEntity(
                id = "notif_${System.currentTimeMillis()}",
                userId = order.customerUserId,
                type = "PAYMENT",
                title = "Payment Rejected - Resubmission Required",
                body = "${order.shopName} was unable to verify reference for Order #${order.orderReference}: $reason",
                timestamp = "Just now"
            )
        )

        ApiResponse(success = true, data = true)
    }

    override suspend fun getShopPaymentMethods(shopId: String): ApiResponse<List<ShopPaymentMethodDto>> = withContext(Dispatchers.IO) {
        val methods = db.shopPaymentMethodDao().getMethodsForShop(shopId).map { it.toDto() }
        ApiResponse(success = true, data = methods)
    }

    override suspend fun saveShopPaymentMethod(method: ShopPaymentMethodDto): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        db.shopPaymentMethodDao().insert(method.toEntity())
        ApiResponse(success = true, data = true)
    }

    override suspend fun getShopDeliveryOptions(shopId: String): ApiResponse<List<ShopDeliveryOptionDto>> = withContext(Dispatchers.IO) {
        val options = db.shopDeliveryOptionDao().getOptionsForShop(shopId).map { it.toDto() }
        ApiResponse(success = true, data = options)
    }

    override suspend fun getShopPromotions(shopId: String): ApiResponse<List<PromoCodeDto>> = withContext(Dispatchers.IO) {
        val list = db.promoCodeDao().getActivePromosForShop(shopId).map { it.toDto() }
        ApiResponse(success = true, data = list)
    }

    override suspend fun createPromoCode(promo: PromoCodeDto): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        db.promoCodeDao().insert(promo.toEntity())
        ApiResponse(success = true, data = true)
    }

    override suspend fun submitSellerVerification(details: Map<String, String>): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        val currentUser = db.userDao().getFirstUser()
        val shopId = currentUser?.shopId ?: "shop_techhub"
        val shop = db.shopDao().getShopById(shopId)
        if (shop != null) {
            db.shopDao().updateShop(shop.copy(verificationStatus = "PENDING_REVIEW"))
            ApiResponse(success = true, data = true)
        } else {
            ApiResponse(success = false, data = false)
        }
    }

    override suspend fun updateShopSettings(settings: Map<String, Any>): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        val currentUser = db.userDao().getFirstUser()
        val shopId = currentUser?.shopId ?: "shop_techhub"
        val shop = db.shopDao().getShopById(shopId)
        if (shop != null) {
            val updated = shop.copy(
                name = settings["name"] as? String ?: shop.name,
                category = settings["category"] as? String ?: shop.category,
                description = settings["description"] as? String ?: shop.description,
                phone = settings["phone"] as? String ?: shop.phone,
                telegramLink = settings["telegramLink"] as? String ?: shop.telegramLink,
                isVacationMode = settings["isVacationMode"] as? Boolean ?: shop.isVacationMode
            )
            db.shopDao().updateShop(updated)
            ApiResponse(success = true, data = true)
        } else {
            ApiResponse(success = false, data = false)
        }
    }

    override suspend fun getChatConversations(): ApiResponse<List<ChatMessageDto>> = withContext(Dispatchers.IO) {
        val list = db.chatDao().getAllMessages().distinctBy { it.conversationId }.map { it.toDto() }
        ApiResponse(success = true, data = list)
    }

    override suspend fun getChatMessages(shopId: String): ApiResponse<List<ChatMessageDto>> = withContext(Dispatchers.IO) {
        val convId = "conv_${shopId.replace("shop_", "")}"
        val list = db.chatDao().getMessages(convId).map { it.toDto() }
        ApiResponse(success = true, data = list)
    }

    override suspend fun sendChatMessage(shopId: String, messageData: Map<String, Any>): ApiResponse<ChatMessageDto> = withContext(Dispatchers.IO) {
        val convId = "conv_${shopId.replace("shop_", "")}"
        val text = messageData["message"] as String
        val shop = db.shopDao().getShopById(shopId)

        val msg = ChatMessageEntity(
            id = "msg_${System.currentTimeMillis()}",
            conversationId = convId,
            shopId = shopId,
            shopName = shop?.name ?: "Shop",
            senderType = "CUSTOMER",
            message = text,
            timestamp = "Just now",
            productContextName = messageData["productContextName"] as? String,
            productContextPrice = messageData["productContextPrice"] as? String,
            productContextImage = messageData["productContextImage"] as? String,
            isRead = true
        )
        db.chatDao().insert(msg)
        ApiResponse(success = true, data = msg.toDto())
    }

    override suspend fun getReviews(targetId: String): ApiResponse<List<ReviewDto>> = withContext(Dispatchers.IO) {
        val list = db.reviewDao().getReviewsForTarget(targetId).map { it.toDto() }
        ApiResponse(success = true, data = list)
    }

    override suspend fun submitReview(reviewData: Map<String, Any>): ApiResponse<ReviewDto> = withContext(Dispatchers.IO) {
        val currentUser = db.userDao().getFirstUser()
        val userId = currentUser?.id ?: "user_fitsum"
        val targetId = reviewData["targetId"] as String
        val targetType = reviewData["targetType"] as? String ?: "PRODUCT"
        val rating = (reviewData["rating"] as Number).toDouble().coerceIn(1.0, 5.0)
        val comment = (reviewData["comment"] as? String)?.trim() ?: ""

        if (comment.isBlank()) {
            return@withContext ApiResponse(success = false, message = "Review comment cannot be empty.")
        }

        // Check if user is a verified purchaser for this target
        val confirmedOrders = db.orderDao().getEligibleOrdersForCustomer(userId)
        val isVerifiedBuyer = when (targetType) {
            "PRODUCT" -> confirmedOrders.any { it.itemsJson.contains(targetId) }
            "SHOP" -> confirmedOrders.any { it.shopId == targetId }
            else -> confirmedOrders.isNotEmpty()
        }

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
            date = "Just now"
        )
        db.reviewDao().insert(review)

        // Update product or shop aggregate rating in the database
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

        ApiResponse(success = true, data = review.toDto())
    }

    override suspend fun getNotifications(): ApiResponse<List<NotificationDto>> = withContext(Dispatchers.IO) {
        val currentUser = db.userDao().getFirstUser()
        val userId = currentUser?.id ?: "user_fitsum"
        val list = db.notificationDao().getNotifications(userId).map { it.toDto() }
        ApiResponse(success = true, data = list)
    }

    override suspend fun getSavedAddresses(): ApiResponse<List<SavedAddressDto>> = withContext(Dispatchers.IO) {
        val currentUser = db.userDao().getFirstUser()
        val userId = currentUser?.id ?: "user_fitsum"
        val list = db.savedAddressDao().getAddresses(userId).map { it.toDto() }
        ApiResponse(success = true, data = list)
    }

    override suspend fun saveAddress(address: SavedAddressDto): ApiResponse<SavedAddressDto> = withContext(Dispatchers.IO) {
        val currentUser = db.userDao().getFirstUser()
        val userId = currentUser?.id ?: "user_fitsum"
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
        ApiResponse(success = true, data = entity.toDto())
    }

    override suspend fun getAdminSettings(): ApiResponse<AdminSettingsDto> = withContext(Dispatchers.IO) {
        val settings = db.adminSettingsDao().getSettings() ?: AdminSettingsEntity()
        ApiResponse(
            success = true,
            data = AdminSettingsDto(
                isChatEnabled = settings.isChatEnabled,
                isExternalContactEnabled = settings.isExternalContactEnabled,
                isSubscriptionModeEnabled = settings.isSubscriptionModeEnabled,
                sellerReviewsEnabled = settings.sellerReviewsEnabled,
                showFeaturedShops = settings.showFeaturedShops,
                activePlanName = settings.activePlanName,
                activePlanPrice = settings.activePlanPrice
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
        db.adminSettingsDao().update(updated)
        ApiResponse(success = true, data = true)
    }

    override suspend fun getAdminSellerVerificationQueue(): ApiResponse<List<ShopDto>> = withContext(Dispatchers.IO) {
        val pending = db.shopDao().getAllShops().filter { it.verificationStatus == "PENDING_REVIEW" }
        ApiResponse(success = true, data = pending.map { it.toDto() })
    }

    override suspend fun adminVerifySellerDecision(shopId: String, decision: String): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        val shop = db.shopDao().getShopById(shopId)
        if (shop != null) {
            val isApproved = decision.equals("APPROVE", ignoreCase = true)
            db.shopDao().updateShop(
                shop.copy(
                    isVerified = isApproved,
                    verificationStatus = if (isApproved) "VERIFIED" else "REJECTED"
                )
            )
            ApiResponse(success = true, data = true)
        } else {
            ApiResponse(success = false, data = false)
        }
    }
}

// Extension mapper functions
fun UserEntity.toDto() = UserDto(
    id = id, name = name, phone = phone, email = email, avatarUrl = avatarUrl,
    role = role, preferredLanguage = preferredLanguage, region = region, city = city,
    subCity = subCity, shopId = shopId, defaultAddressId = defaultAddressId
)

fun ShopEntity.toDto() = ShopDto(
    id = id, ownerUserId = ownerUserId, name = name, slug = slug, logoUrl = logoUrl,
    coverUrl = coverUrl, category = category, shopType = shopType, description = description,
    rating = rating, reviewCount = reviewCount, isVerified = isVerified,
    verificationStatus = verificationStatus, phone = phone, telegramLink = telegramLink,
    whatsappLink = whatsappLink, address = address, region = region, city = city,
    subCity = subCity, followersCount = followersCount, productCount = productCount,
    isVacationMode = isVacationMode, acceptsOrders = acceptsOrders, isFollowing = isFollowing
)

fun ProductEntity.toDto() = ProductDto(
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
    status = status, isWishlisted = isWishlisted
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
        verifiedAt = verifiedAt, orderStatus = orderStatus, createdAt = createdAt
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
    validUntil = validUntil, isActive = isActive
)

fun PromoCodeDto.toEntity() = PromoCodeEntity(
    id = id, shopId = shopId, code = code, discountType = discountType,
    discountValue = discountValue, minOrderAmount = minOrderAmount,
    validUntil = validUntil, isActive = isActive
)

fun ReviewEntity.toDto() = ReviewDto(
    id = id, userId = userId, userName = userName, userAvatar = userAvatar,
    targetType = targetType, targetId = targetId, rating = rating,
    comment = comment, isVerifiedPurchase = isVerifiedPurchase, date = date
)

fun ChatMessageEntity.toDto() = ChatMessageDto(
    id = id, conversationId = conversationId, shopId = shopId,
    shopName = shopName, senderType = senderType, message = message,
    timestamp = timestamp, productContextName = productContextName,
    productContextPrice = productContextPrice, productContextImage = productContextImage,
    isRead = isRead
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
    icon = icon, subCategories = if (subCategoriesCsv.isBlank()) emptyList() else subCategoriesCsv.split(",")
)
