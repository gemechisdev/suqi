package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class VerificationDocSlot(
    val key: String,
    val label: String,
    val guidance: String,
    val demoSampleUrl: String
)

data class VerificationHistoryEntry(
    val status: String,
    val at: String,
    val note: String = "",
    val failedKeys: List<String> = emptyList()
)

data class VerificationSubmission(
    val docs: Map<String, String> = emptyMap(),
    val licenseRef: String = "",
    val tin: String = "",
    val submittedAt: String = "",
    val failedKeys: List<String> = emptyList(),
    val history: List<VerificationHistoryEntry> = emptyList()
)

object VerificationDocsCodec {
    val docSlots = listOf(
        VerificationDocSlot(
            key = "idFront",
            label = "National ID / Kebele ID (Front)",
            guidance = "All four corners visible, no glare. Photo & ID number clearly readable.",
            demoSampleUrl = "https://images.unsplash.com/photo-1554224155-6726b3ff858f?w=600"
        ),
        VerificationDocSlot(
            key = "idBack",
            label = "National ID / Kebele ID (Back)",
            guidance = "Back side address, stamp, and issue/expiry dates clearly visible.",
            demoSampleUrl = "https://images.unsplash.com/photo-1554224154-26032ffc0d07?w=600"
        ),
        VerificationDocSlot(
            key = "tradeLicense",
            label = "Ethiopian Trade License / Business Reg",
            guidance = "Current fiscal year trade license or commercial registration certificate.",
            demoSampleUrl = "https://images.unsplash.com/photo-1450101499163-c8848c66ca85?w=600"
        ),
        VerificationDocSlot(
            key = "shopPhoto",
            label = "Shopfront or Workspace Photo",
            guidance = "Clear photo of your shop signboard, stock shelf, or packing desk.",
            demoSampleUrl = "https://images.unsplash.com/photo-1441986300917-64674bd600d8?w=600"
        ),
        VerificationDocSlot(
            key = "selfieWithId",
            label = "Selfie Holding Your ID",
            guidance = "Face and ID card both clearly visible in good natural lighting.",
            demoSampleUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=600"
        )
    )

    val checklistItems = listOf(
        "id_readable" to "National ID front & back clearly readable",
        "name_matches" to "Owner name matches ID and trade license",
        "license_valid" to "Trade license valid for current fiscal year",
        "shop_authentic" to "Shop photo appears authentic and active",
        "address_plausible" to "Business address matches sub-city & hub"
    )

    fun nowLabel(): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())

    fun parse(rawJson: String?): VerificationSubmission {
        if (rawJson.isNullOrBlank()) return VerificationSubmission()
        val trimmed = rawJson.trim()
        if (trimmed.startsWith("{")) {
            val obj = runCatching { JSONObject(trimmed) }.getOrNull() ?: return VerificationSubmission()
            val docsObj = obj.optJSONObject("docs")
            val docs = mutableMapOf<String, String>()
            if (docsObj != null) {
                docsObj.keys().forEach { k ->
                    val v = docsObj.optString(k).trim()
                    if (v.isNotBlank()) docs[k] = v
                }
            } else {
                docSlots.forEach { slot ->
                    val v = obj.optString(slot.key).trim()
                    if (v.isNotBlank()) docs[slot.key] = v
                }
            }
            val failedArr = obj.optJSONArray("failedKeys")
            val failed = buildList {
                if (failedArr != null) {
                    for (i in 0 until failedArr.length()) {
                        val s = failedArr.optString(i).trim()
                        if (s.isNotBlank()) add(s)
                    }
                }
            }
            val histArr = obj.optJSONArray("history")
            val history = buildList {
                if (histArr != null) {
                    for (i in 0 until histArr.length()) {
                        val item = histArr.optJSONObject(i) ?: continue
                        val fArr = item.optJSONArray("failedKeys")
                        val itemFailed = buildList {
                            if (fArr != null) {
                                for (j in 0 until fArr.length()) {
                                    val fk = fArr.optString(j).trim()
                                    if (fk.isNotBlank()) add(fk)
                                }
                            }
                        }
                        add(
                            VerificationHistoryEntry(
                                status = item.optString("status"),
                                at = item.optString("at"),
                                note = item.optString("note"),
                                failedKeys = itemFailed
                            )
                        )
                    }
                }
            }
            return VerificationSubmission(
                docs = docs,
                licenseRef = obj.optString("licenseRef").ifBlank { obj.optString("license") },
                tin = obj.optString("tin"),
                submittedAt = obj.optString("submittedAt"),
                failedKeys = failed,
                history = history
            )
        }
        if (trimmed.startsWith("[")) {
            val arr = runCatching { JSONArray(trimmed) }.getOrNull() ?: return VerificationSubmission()
            val first = arr.optString(0)
            val second = arr.optString(1)
            return VerificationSubmission(licenseRef = first, tin = second)
        }
        return VerificationSubmission()
    }

    fun toJson(submission: VerificationSubmission): String {
        val obj = JSONObject()
        val docsObj = JSONObject()
        submission.docs.forEach { (k, v) -> if (v.isNotBlank()) docsObj.put(k, v) }
        obj.put("docs", docsObj)
        obj.put("licenseRef", submission.licenseRef)
        obj.put("tin", submission.tin)
        obj.put("submittedAt", submission.submittedAt)
        obj.put("failedKeys", JSONArray(submission.failedKeys))
        val histArr = JSONArray()
        submission.history.forEach { h ->
            val hObj = JSONObject()
            hObj.put("status", h.status)
            hObj.put("at", h.at)
            hObj.put("note", h.note)
            hObj.put("failedKeys", JSONArray(h.failedKeys))
            histArr.put(hObj)
        }
        obj.put("history", histArr)
        return obj.toString()
    }
}
