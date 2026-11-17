package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

data class ShopDayHours(
    val dayKey: String,
    val isOpen: Boolean = true,
    val openTime: String = "08:30",
    val closeTime: String = "19:30"
)

object ShopBusinessHoursCodec {
    val dayKeys: List<String> = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

    fun defaultWeek(): List<ShopDayHours> = dayKeys.map { key ->
        if (key == "SUN") ShopDayHours(key, isOpen = false, openTime = "10:00", closeTime = "17:00")
        else ShopDayHours(key, isOpen = true, openTime = "08:30", closeTime = "19:30")
    }

    fun parse(json: String): List<ShopDayHours> {
        val fallback = defaultWeek().associateBy { it.dayKey }
        if (json.isBlank()) return defaultWeek()
        return try {
            val arr = JSONArray(json)
            val parsedMap = mutableMapOf<String, ShopDayHours>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val key = obj.optString("dayKey").uppercase()
                if (key in dayKeys) {
                    parsedMap[key] = ShopDayHours(
                        dayKey = key,
                        isOpen = obj.optBoolean("isOpen", true),
                        openTime = obj.optString("openTime").ifBlank { "08:30" },
                        closeTime = obj.optString("closeTime").ifBlank { "19:30" }
                    )
                }
            }
            dayKeys.map { key -> parsedMap[key] ?: fallback.getValue(key) }
        } catch (_: Exception) {
            defaultWeek()
        }
    }

    fun toJson(days: List<ShopDayHours>): String {
        val byKey = days.associateBy { it.dayKey.uppercase() }
        return JSONArray().apply {
            dayKeys.forEach { key ->
                val item = byKey[key] ?: ShopDayHours(key, isOpen = key != "SUN")
                put(
                    JSONObject()
                        .put("dayKey", item.dayKey)
                        .put("isOpen", item.isOpen)
                        .put("openTime", item.openTime)
                        .put("closeTime", item.closeTime)
                )
            }
        }.toString()
    }
}

object ShopGalleryCodec {
    fun parse(json: String): List<String> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i -> arr.optString(i).takeIf { it.isNotBlank() } }.take(6)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun toJson(urls: List<String>): String = JSONArray(urls.filter { it.isNotBlank() }.take(6)).toString()
}
