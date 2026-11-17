package com.example.data.local

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ProductDraftData(
    val name: String = "",
    val nameAmharic: String = "",
    val nameAfaanOromo: String = "",
    val categoryId: String = "cat_electronics",
    val priceStr: String = "",
    val discountStr: String = "0",
    val stockStr: String = "10",
    val stockHold: Boolean = true,
    val selectedTax: String = "15% Standard VAT Included",
    val description: String = "",
    val descAmharic: String = "",
    val descOromo: String = "",
    val detailsJson: String = "",
    val savedAtLabel: String = ""
)

data class SellerNotificationPrefs(
    val newOrder: Boolean = true,
    val paymentSubmitted: Boolean = true,
    val lowStock: Boolean = true,
    val newReview: Boolean = true,
    val buyerQuestion: Boolean = true
)

class SellerPreferencesStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("suqi_seller_prefs", Context.MODE_PRIVATE)

    fun saveProductDraft(draft: ProductDraftData) {
        val obj = JSONObject()
            .put("name", draft.name)
            .put("nameAmharic", draft.nameAmharic)
            .put("nameAfaanOromo", draft.nameAfaanOromo)
            .put("categoryId", draft.categoryId)
            .put("priceStr", draft.priceStr)
            .put("discountStr", draft.discountStr)
            .put("stockStr", draft.stockStr)
            .put("stockHold", draft.stockHold)
            .put("selectedTax", draft.selectedTax)
            .put("description", draft.description)
            .put("descAmharic", draft.descAmharic)
            .put("descOromo", draft.descOromo)
            .put("detailsJson", draft.detailsJson)
            .put("savedAtLabel", draft.savedAtLabel)
        prefs.edit().putString(KEY_PRODUCT_DRAFT, obj.toString()).apply()
    }

    fun loadProductDraft(): ProductDraftData? {
        val raw = prefs.getString(KEY_PRODUCT_DRAFT, null) ?: return null
        return try {
            val obj = JSONObject(raw)
            ProductDraftData(
                name = obj.optString("name"),
                nameAmharic = obj.optString("nameAmharic"),
                nameAfaanOromo = obj.optString("nameAfaanOromo"),
                categoryId = obj.optString("categoryId").ifBlank { "cat_electronics" },
                priceStr = obj.optString("priceStr"),
                discountStr = obj.optString("discountStr").ifBlank { "0" },
                stockStr = obj.optString("stockStr").ifBlank { "10" },
                stockHold = obj.optBoolean("stockHold", true),
                selectedTax = obj.optString("selectedTax").ifBlank { "15% Standard VAT Included" },
                description = obj.optString("description"),
                descAmharic = obj.optString("descAmharic"),
                descOromo = obj.optString("descOromo"),
                detailsJson = obj.optString("detailsJson"),
                savedAtLabel = obj.optString("savedAtLabel")
            )
        } catch (_: Exception) {
            null
        }
    }

    fun clearProductDraft() {
        prefs.edit().remove(KEY_PRODUCT_DRAFT).apply()
    }

    fun loadNotificationPrefs(): SellerNotificationPrefs {
        return SellerNotificationPrefs(
            newOrder = prefs.getBoolean(KEY_NOTIF_NEW_ORDER, true),
            paymentSubmitted = prefs.getBoolean(KEY_NOTIF_PAYMENT, true),
            lowStock = prefs.getBoolean(KEY_NOTIF_LOW_STOCK, true),
            newReview = prefs.getBoolean(KEY_NOTIF_REVIEW, true),
            buyerQuestion = prefs.getBoolean(KEY_NOTIF_QUESTION, true)
        )
    }

    fun saveNotificationPrefs(value: SellerNotificationPrefs) {
        prefs.edit()
            .putBoolean(KEY_NOTIF_NEW_ORDER, value.newOrder)
            .putBoolean(KEY_NOTIF_PAYMENT, value.paymentSubmitted)
            .putBoolean(KEY_NOTIF_LOW_STOCK, value.lowStock)
            .putBoolean(KEY_NOTIF_REVIEW, value.newReview)
            .putBoolean(KEY_NOTIF_QUESTION, value.buyerQuestion)
            .apply()
    }

    fun loadQuickReplies(defaults: List<String>): List<String> {
        val raw = prefs.getString(KEY_QUICK_REPLIES, null) ?: return defaults
        return try {
            val arr = JSONArray(raw)
            val list = (0 until arr.length()).mapNotNull { i -> arr.optString(i).takeIf { it.isNotBlank() } }
            list.ifEmpty { defaults }
        } catch (_: Exception) {
            defaults
        }
    }

    fun saveQuickReplies(templates: List<String>) {
        val arr = JSONArray(templates.map { it.trim() }.filter { it.isNotBlank() })
        prefs.edit().putString(KEY_QUICK_REPLIES, arr.toString()).apply()
    }

    companion object {
        private const val KEY_PRODUCT_DRAFT = "product_draft_v1"
        private const val KEY_NOTIF_NEW_ORDER = "notif_new_order"
        private const val KEY_NOTIF_PAYMENT = "notif_payment_submitted"
        private const val KEY_NOTIF_LOW_STOCK = "notif_low_stock"
        private const val KEY_NOTIF_REVIEW = "notif_new_review"
        private const val KEY_NOTIF_QUESTION = "notif_buyer_question"
        private const val KEY_QUICK_REPLIES = "seller_quick_replies_v1"
    }
}
