package com.example.data.model

import com.squareup.moshi.JsonClass
import org.json.JSONArray
import org.json.JSONObject

/**
 * Data-driven product information.
 *
 * Everything that varies between product types (media, specifications, buyer options and
 * variants) lives in [ProductDetails], which is persisted as a single JSON document.
 * New item types or attributes therefore never require a database schema change.
 */

object MediaKind {
    const val IMAGE = "image"
    const val GIF = "gif"
    const val VIDEO = "video"
}

object AttrType {
    const val TEXT = "text"
    const val NUMBER = "number"
    const val BOOLEAN = "boolean"
    const val SELECT = "select"
    const val MULTI_SELECT = "multi_select"
    const val COLOR = "color"
    const val DATE = "date"
    const val MEASUREMENT = "measurement"

    val all = listOf(TEXT, NUMBER, BOOLEAN, SELECT, MULTI_SELECT, COLOR, DATE, MEASUREMENT)

    fun label(type: String): String = when (type) {
        TEXT -> "Text"
        NUMBER -> "Number"
        BOOLEAN -> "Yes / No"
        SELECT -> "Single choice"
        MULTI_SELECT -> "Multiple choice"
        COLOR -> "Color"
        DATE -> "Date"
        MEASUREMENT -> "Measurement"
        else -> type
    }
}

@JsonClass(generateAdapter = true)
data class MediaItem(
    val url: String,
    val type: String = MediaKind.IMAGE, // image | gif | video
    val thumbnailUrl: String = ""
) {
    val isVideo: Boolean get() = type == MediaKind.VIDEO
    val isGif: Boolean get() = type == MediaKind.GIF
}

/** Definition of one product parameter (what it is, how it is entered, how it is shown). */
@JsonClass(generateAdapter = true)
data class AttributeDef(
    val key: String,
    val label: String,
    val type: String = AttrType.TEXT,
    val options: List<String> = emptyList(), // choices (select/multi/color) or units (measurement)
    val unit: String = "",
    val required: Boolean = false,
    val enabled: Boolean = true,
    /** true = buyers choose a value when purchasing (size, color, storage...). false = a fixed spec. */
    val selectable: Boolean = false,
    val group: String = "General"
)

@JsonClass(generateAdapter = true)
data class ProductVariant(
    val id: String,
    /** attribute key -> chosen value, for every selectable attribute */
    val options: Map<String, String> = emptyMap(),
    val sku: String = "",
    val price: Double? = null,
    val stock: Int? = null,
    val imageUrl: String = ""
)

@JsonClass(generateAdapter = true)
data class ProductDetails(
    val itemType: String = "",
    val media: List<MediaItem> = emptyList(),
    val attributes: List<AttributeDef> = emptyList(),
    /** attribute key -> values (single-valued attributes hold exactly one element) */
    val values: Map<String, List<String>> = emptyMap(),
    val variants: List<ProductVariant> = emptyList()
) {
    fun valuesOf(key: String): List<String> = values[key].orEmpty().filter { it.isNotBlank() }

    /** Buyer-selectable options that are enabled and actually have values. */
    val buyerOptions: List<AttributeDef>
        get() = attributes.filter { it.enabled && it.selectable && valuesOf(it.key).isNotEmpty() }

    /** Fixed specifications that are enabled and have a meaningful value. */
    val specifications: List<AttributeDef>
        get() = attributes.filter { it.enabled && !it.selectable && valuesOf(it.key).isNotEmpty() }

    fun displayValue(def: AttributeDef): String {
        val list = valuesOf(def.key)
        return when (def.type) {
            AttrType.BOOLEAN -> if (list.firstOrNull().equals("true", true)) "Yes" else "No"
            else -> list.joinToString(", ")
        }
    }

    /** Variant matching the buyer's chosen options (key -> value), if variants are configured. */
    fun variantFor(chosen: Map<String, String>): ProductVariant? {
        if (variants.isEmpty() || chosen.isEmpty()) return null
        return variants.firstOrNull { v -> v.options.all { (k, value) -> chosen[k] == value } }
    }

    companion object {
        fun variantLabel(options: List<AttributeDef>, chosen: Map<String, String>): String =
            options.mapNotNull { chosen[it.key]?.takeIf { v -> v.isNotBlank() } }.joinToString(" • ")
    }
}

// ───────────────────────── media helpers ─────────────────────────

fun mediaTypeOfUrl(url: String, mimeType: String? = null): String {
    val mime = mimeType?.lowercase().orEmpty()
    if (mime == "image/gif") return MediaKind.GIF
    if (mime.startsWith("video/")) return MediaKind.VIDEO
    if (mime.startsWith("image/")) return MediaKind.IMAGE
    val u = url.lowercase().substringBefore("?")
    return when {
        u.endsWith(".gif") -> MediaKind.GIF
        u.endsWith(".mp4") || u.endsWith(".webm") || u.endsWith(".m3u8") ||
            u.endsWith(".mkv") || u.endsWith(".3gp") || u.endsWith(".mov") -> MediaKind.VIDEO
        else -> MediaKind.IMAGE
    }
}

/** First non-video media = card/cover image. Falls back to the first item. */
fun ProductDetails.coverImageUrl(): String =
    (media.firstOrNull { !it.isVideo } ?: media.firstOrNull())?.url.orEmpty()

// ───────────────────────── JSON codec ─────────────────────────

fun ProductDetails.toJson(): String {
    val root = JSONObject()
    root.put("itemType", itemType)
    root.put("media", JSONArray().apply {
        media.forEach { m ->
            put(JSONObject().put("url", m.url).put("type", m.type).put("thumbnailUrl", m.thumbnailUrl))
        }
    })
    root.put("attributes", JSONArray().apply {
        attributes.forEach { a ->
            put(
                JSONObject()
                    .put("key", a.key).put("label", a.label).put("type", a.type)
                    .put("options", JSONArray(a.options)).put("unit", a.unit)
                    .put("required", a.required).put("enabled", a.enabled)
                    .put("selectable", a.selectable).put("group", a.group)
            )
        }
    })
    root.put("values", JSONObject().apply {
        values.forEach { (k, list) -> put(k, JSONArray(list)) }
    })
    root.put("variants", JSONArray().apply {
        variants.forEach { v ->
            put(
                JSONObject()
                    .put("id", v.id)
                    .put("options", JSONObject().apply { v.options.forEach { (k, x) -> put(k, x) } })
                    .put("sku", v.sku)
                    .put("price", v.price ?: JSONObject.NULL)
                    .put("stock", v.stock ?: JSONObject.NULL)
                    .put("imageUrl", v.imageUrl)
            )
        }
    })
    return root.toString()
}

private fun JSONArray.strings(): List<String> =
    (0 until length()).map { optString(it) }

fun parseProductDetails(json: String): ProductDetails? {
    if (json.isBlank()) return null
    return try {
        val root = JSONObject(json)
        val media = root.optJSONArray("media")?.let { arr ->
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val url = o.optString("url")
                MediaItem(url, o.optString("type").ifBlank { mediaTypeOfUrl(url) }, o.optString("thumbnailUrl"))
            }
        }.orEmpty()
        val attrs = root.optJSONArray("attributes")?.let { arr ->
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                AttributeDef(
                    key = o.optString("key"),
                    label = o.optString("label"),
                    type = o.optString("type").ifBlank { AttrType.TEXT },
                    options = o.optJSONArray("options")?.strings().orEmpty(),
                    unit = o.optString("unit"),
                    required = o.optBoolean("required", false),
                    enabled = o.optBoolean("enabled", true),
                    selectable = o.optBoolean("selectable", false),
                    group = o.optString("group").ifBlank { "General" }
                )
            }
        }.orEmpty()
        val values = LinkedHashMap<String, List<String>>()
        root.optJSONObject("values")?.let { obj ->
            obj.keys().forEach { k -> values[k] = obj.optJSONArray(k)?.strings().orEmpty() }
        }
        val variants = root.optJSONArray("variants")?.let { arr ->
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val opts = LinkedHashMap<String, String>()
                o.optJSONObject("options")?.let { oo -> oo.keys().forEach { k -> opts[k] = oo.optString(k) } }
                ProductVariant(
                    id = o.optString("id"),
                    options = opts,
                    sku = o.optString("sku"),
                    price = if (o.isNull("price")) null else o.optDouble("price"),
                    stock = if (o.isNull("stock")) null else o.optInt("stock"),
                    imageUrl = o.optString("imageUrl")
                )
            }
        }.orEmpty()
        ProductDetails(root.optString("itemType"), media, attrs, values, variants)
    } catch (_: Exception) {
        null
    }
}

/**
 * Backward compatibility: products created before the dynamic system only have
 * an image list plus colour / size CSVs. Map them onto the new model.
 */
fun legacyProductDetails(
    primaryImageUrl: String,
    extraImages: List<String>,
    colors: List<String>,
    sizesOrSpecs: List<String>
): ProductDetails {
    val media = (listOf(primaryImageUrl) + extraImages)
        .filter { it.isNotBlank() }
        .map { MediaItem(it, mediaTypeOfUrl(it)) }
    val attrs = mutableListOf<AttributeDef>()
    val values = LinkedHashMap<String, List<String>>()
    if (colors.isNotEmpty()) {
        attrs += AttributeDef("color", "Color", AttrType.COLOR, selectable = true)
        values["color"] = colors.map { it.trim() }
    }
    if (sizesOrSpecs.isNotEmpty()) {
        attrs += AttributeDef("option", "Options", AttrType.SELECT, selectable = true)
        values["option"] = sizesOrSpecs.map { it.trim() }
    }
    return ProductDetails(media = media, attributes = attrs, values = values)
}
