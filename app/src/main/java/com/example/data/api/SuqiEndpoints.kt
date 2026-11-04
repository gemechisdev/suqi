package com.example.data.api

object SuqiEndpoints {
    const val BASE_URL = "https://api.suqi.et/v1/"

    // Auth
    const val AUTH_TELEGRAM = "auth/telegram"
    const val AUTH_GOOGLE = "auth/google"
    const val AUTH_GUEST = "auth/guest"
    const val AUTH_ME = "auth/me"
    const val AUTH_UPGRADE_SELLER = "auth/upgrade-seller"

    // Marketplace
    const val PRODUCTS = "products"
    const val PRODUCT_DETAILS = "products/{id}"
    const val PRODUCT_WISHLIST = "products/{id}/wishlist"
    const val SHOPS = "shops"
    const val SHOP_DETAILS = "shops/{id}"
    const val SHOP_FOLLOW = "shops/{id}/follow"
    const val CATEGORIES = "categories"

    // Cart
    const val CART = "cart"
    const val CART_ITEM = "cart/{itemId}"
    const val CART_CLEAR = "cart/clear"

    // Checkout & Orders
    const val CHECKOUT_VALIDATE = "checkout/validate"
    const val ORDERS = "orders"
    const val ORDER_DETAILS = "orders/{orderId}"
    const val ORDER_RESUBMIT_PAYMENT = "orders/{orderId}/resubmit-payment"
    const val ORDER_CANCEL = "orders/{orderId}/cancel"

    // Seller Studio
    const val SELLER_DASHBOARD = "seller/dashboard"
    const val SELLER_PRODUCTS = "seller/products"
    const val SELLER_ORDERS = "seller/orders"
    const val SELLER_ORDER_STATUS = "seller/orders/{orderId}/status"
    const val SELLER_VERIFICATION_QUEUE = "seller/verification-queue"
    const val SELLER_VERIFY_PAYMENT = "seller/orders/{orderId}/verify-payment"
    const val SELLER_REJECT_PAYMENT = "seller/orders/{orderId}/reject-payment"
    const val SELLER_PAYMENT_METHODS = "seller/payment-methods"
    const val SELLER_DELIVERY_OPTIONS = "seller/delivery-options"
    const val SELLER_PROMOTIONS = "seller/promotions"
    const val SELLER_VERIFICATION_SUBMIT = "seller/verification"
    const val SELLER_SHOP_SETTINGS = "seller/shop-settings"

    // Chat, Reviews, Notifications, Addresses
    const val CHAT_CONVERSATIONS = "chat/conversations"
    const val CHAT_MESSAGES = "chat/{shopId}/messages"
    const val REVIEWS = "reviews"
    const val NOTIFICATIONS = "notifications"
    const val ADDRESSES = "addresses"

    // Admin
    const val ADMIN_SETTINGS = "admin/settings"
    const val ADMIN_FEATURE_TOGGLES = "admin/features"
    const val ADMIN_SELLER_QUEUE = "admin/sellers"
    const val ADMIN_VERIFY_SELLER = "admin/sellers/{shopId}/decision"
}
