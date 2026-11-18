package com.example.data.local

import android.content.Context

/**
 * B-09 Notification preferences (buyer). Stored locally like the seller notification switches:
 * nothing dispatches notifications yet, so these are persisted and shown but not consumed.
 */
data class BuyerNotificationPrefs(
    val orderUpdates: Boolean = true,
    val chatMessages: Boolean = true,
    val promotions: Boolean = true,
    val stockAndPrice: Boolean = true,
    val followedShops: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietStart: String = "22:00",
    val quietEnd: String = "07:00"
)

/** B-18 Privacy toggles. Both are enforced: they gate what the app remembers on this device. */
data class BuyerPrivacyPrefs(
    val rememberRecentlyViewed: Boolean = true,
    val saveSearchHistory: Boolean = true
)

class BuyerPreferencesStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadNotificationPrefs(): BuyerNotificationPrefs {
        val defaults = BuyerNotificationPrefs()
        return BuyerNotificationPrefs(
            orderUpdates = prefs.getBoolean(KEY_ORDER_UPDATES, defaults.orderUpdates),
            chatMessages = prefs.getBoolean(KEY_CHAT, defaults.chatMessages),
            promotions = prefs.getBoolean(KEY_PROMOS, defaults.promotions),
            stockAndPrice = prefs.getBoolean(KEY_STOCK_PRICE, defaults.stockAndPrice),
            followedShops = prefs.getBoolean(KEY_FOLLOWED, defaults.followedShops),
            quietHoursEnabled = prefs.getBoolean(KEY_QUIET_ON, defaults.quietHoursEnabled),
            quietStart = prefs.getString(KEY_QUIET_START, defaults.quietStart)?.ifBlank { defaults.quietStart }
                ?: defaults.quietStart,
            quietEnd = prefs.getString(KEY_QUIET_END, defaults.quietEnd)?.ifBlank { defaults.quietEnd }
                ?: defaults.quietEnd
        )
    }

    fun saveNotificationPrefs(value: BuyerNotificationPrefs) {
        prefs.edit()
            .putBoolean(KEY_ORDER_UPDATES, value.orderUpdates)
            .putBoolean(KEY_CHAT, value.chatMessages)
            .putBoolean(KEY_PROMOS, value.promotions)
            .putBoolean(KEY_STOCK_PRICE, value.stockAndPrice)
            .putBoolean(KEY_FOLLOWED, value.followedShops)
            .putBoolean(KEY_QUIET_ON, value.quietHoursEnabled)
            .putString(KEY_QUIET_START, value.quietStart)
            .putString(KEY_QUIET_END, value.quietEnd)
            .apply()
    }

    fun loadPrivacyPrefs(): BuyerPrivacyPrefs {
        val defaults = BuyerPrivacyPrefs()
        return BuyerPrivacyPrefs(
            rememberRecentlyViewed = prefs.getBoolean(KEY_REMEMBER_VIEWED, defaults.rememberRecentlyViewed),
            saveSearchHistory = prefs.getBoolean(KEY_SAVE_SEARCH, defaults.saveSearchHistory)
        )
    }

    fun savePrivacyPrefs(value: BuyerPrivacyPrefs) {
        prefs.edit()
            .putBoolean(KEY_REMEMBER_VIEWED, value.rememberRecentlyViewed)
            .putBoolean(KEY_SAVE_SEARCH, value.saveSearchHistory)
            .apply()
    }

    fun loadDataSaver(): Boolean = prefs.getBoolean(KEY_DATA_SAVER, false)

    fun saveDataSaver(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DATA_SAVER, enabled).apply()
    }

    fun loadDealsDropNotify(): Boolean = prefs.getBoolean(KEY_DEALS_DROP_NOTIFY, false)

    fun saveDealsDropNotify(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEALS_DROP_NOTIFY, enabled).apply()
    }

    /**
     * Wipes the stored recent searches. The search screen seeds demo terms once, so the
     * "initialized" flag is set to keep them from coming back after a clear.
     */
    fun clearSearchHistory() {
        appContext.getSharedPreferences(SEARCH_PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean("recent_initialized", true)
            .putString("recent_json", "[]")
            .apply()
    }

    /** Account deletion: forget every buyer preference kept on this device. */
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        const val PREFS_NAME = "suqi_buyer_prefs"
        const val SEARCH_PREFS_NAME = "suqi_search_prefs"
        private const val KEY_ORDER_UPDATES = "notif_order_updates"
        private const val KEY_CHAT = "notif_chat"
        private const val KEY_PROMOS = "notif_promos"
        private const val KEY_STOCK_PRICE = "notif_stock_price"
        private const val KEY_FOLLOWED = "notif_followed_shops"
        private const val KEY_QUIET_ON = "quiet_hours_on"
        private const val KEY_QUIET_START = "quiet_hours_start"
        private const val KEY_QUIET_END = "quiet_hours_end"
        private const val KEY_DATA_SAVER = "data_saver"
        private const val KEY_DEALS_DROP_NOTIFY = "deals_drop_notify"
        private const val KEY_REMEMBER_VIEWED = "privacy_remember_viewed"
        private const val KEY_SAVE_SEARCH = "privacy_save_search"
    }
}
