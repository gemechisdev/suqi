package com.example.ui.i18n

/** Display helpers for delivery titles and ETAs that are stored as English text. */
fun SuqiExtraStrings.localizeDeliveryTitle(title: String): String = when (title.trim()) {
    "Standard Courier Delivery", "Standard Delivery" -> deliveryTitleStandard
    "Express Same-Day Delivery", "Express Delivery" -> deliveryTitleExpress
    "In-Store Customer Pickup", "Store Pickup" -> deliveryTitlePickup
    else -> title
}

private val daysRegex = Regex("^(\\d+)(?:\\s*-\\s*(\\d+))?\\s+(business\\s+)?days?$", RegexOption.IGNORE_CASE)

fun SuqiExtraStrings.localizeEta(text: String, language: SuqiLanguage): String {
    val t = text.trim()
    if (language == SuqiLanguage.EN || t.isEmpty()) return text
    if (t.equals("Same day", true) || t.equals("Same-day", true)) return etaExpressDefault
    if (t.equals("Ready in 2 hours", true)) return etaPickupDefault
    val m = daysRegex.find(t) ?: return text
    val from = m.groupValues[1]
    val to = m.groupValues[2]
    val range = if (to.isNotEmpty()) "$from-$to" else from
    val business = m.groupValues[3].isNotEmpty()
    return when (language) {
        SuqiLanguage.AM -> if (business) "$range የስራ ቀናት" else "$range ቀናት"
        SuqiLanguage.OM -> if (business) "Guyyaa hojii $range" else "Guyyaa $range"
        else -> text
    }
}
