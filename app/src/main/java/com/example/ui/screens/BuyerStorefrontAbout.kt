package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ShopBusinessHoursCodec
import com.example.data.model.ShopDayHours
import com.example.data.model.ShopDeliveryOptionDto
import com.example.data.model.ShopDto
import com.example.data.model.ShopPaymentMethodDto
import com.example.ui.components.*
import com.example.ui.i18n.LocalSuqiLanguage
import com.example.ui.i18n.localizeDeliveryTitle
import com.example.ui.i18n.localizeEta
import com.example.ui.i18n.placeLabel
import com.example.ui.i18n.suqiStrings
import com.example.ui.theme.*
import java.util.Calendar

/** Pure helper: is a shop open at the given moment? Compares HH:mm strings for that weekday. */
fun isShopOpenNow(days: List<ShopDayHours>, calendar: Calendar = Calendar.getInstance()): Boolean {
    val key = when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "MON"
        Calendar.TUESDAY -> "TUE"
        Calendar.WEDNESDAY -> "WED"
        Calendar.THURSDAY -> "THU"
        Calendar.FRIDAY -> "FRI"
        Calendar.SATURDAY -> "SAT"
        else -> "SUN"
    }
    val today = days.firstOrNull { it.dayKey == key } ?: return false
    if (!today.isOpen) return false
    fun minutes(t: String): Int? {
        val p = t.split(":")
        val h = p.getOrNull(0)?.trim()?.toIntOrNull() ?: return null
        val m = p.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
        return h * 60 + m
    }
    val open = minutes(today.openTime) ?: return false
    val close = minutes(today.closeTime) ?: return false
    val now = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    return now in open until close
}

@Composable
fun ShopAboutContent(
    shop: ShopDto,
    deliveryOptions: List<ShopDeliveryOptionDto>,
    paymentMethods: List<ShopPaymentMethodDto>,
    modifier: Modifier = Modifier
) {
    val colors = LocalSuqiColors.current
    val strings = suqiStrings()
    val w = strings.buyerWork
    val lang = LocalSuqiLanguage.current
    val context = LocalContext.current
    val days = androidx.compose.runtime.remember(shop.businessHoursJson) { ShopBusinessHoursCodec.parse(shop.businessHoursJson) }
    val openNow = androidx.compose.runtime.remember(days) { isShopOpenNow(days) }
    val todayKey = androidx.compose.runtime.remember {
        listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1]
    }
    fun dayLabel(key: String) = when (key) {
        "MON" -> strings.sellerWork.dayMon
        "TUE" -> strings.sellerWork.dayTue
        "WED" -> strings.sellerWork.dayWed
        "THU" -> strings.sellerWork.dayThu
        "FRI" -> strings.sellerWork.dayFri
        "SAT" -> strings.sellerWork.daySat
        else -> strings.sellerWork.daySun
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("shop_about_content"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Description + joined date
        SuqiSectionCard(padding = 16.dp) {
            Text(w.aboutShopTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.primaryText)
            Spacer(modifier = Modifier.height(6.dp))
            Text(shop.description.ifBlank { w.noPolicyYet }, fontFamily = SuqiFontFamily, fontSize = 14.sp, lineHeight = 20.sp, color = colors.secondaryText)
            if (shop.createdAt.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                val joined = shop.createdAt.substringBefore("•").trim().ifBlank { shop.createdAt }
                Text(String.format(w.joinedLabelTemplate, joined), fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.mutedText)
            }
        }

        // Opening hours
        SuqiSectionCard(padding = 16.dp, modifier = Modifier.testTag("shop_about_hours")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(w.openingHoursTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.primaryText, modifier = Modifier.weight(1f))
                SuqiStatusBadge(
                    if (openNow) w.openNowPill else w.closedNowPill,
                    if (openNow) StatusType.SUCCESS else StatusType.NEUTRAL,
                    modifier = Modifier.testTag("shop_open_now_pill")
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            days.forEach { d ->
                val isToday = d.dayKey == todayKey
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        dayLabel(d.dayKey),
                        fontFamily = SuqiFontFamily, fontSize = 13.sp,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (isToday) SuqiPrimary else colors.secondaryText
                    )
                    Text(
                        if (d.isOpen) "${d.openTime} – ${d.closeTime}" else strings.sellerWork.closedLabel,
                        fontFamily = SuqiFontFamily, fontSize = 13.sp,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                        color = if (isToday) SuqiPrimary else colors.primaryText
                    )
                }
            }
        }

        // Address + map tile
        SuqiSectionCard(padding = 16.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(LucideIcons.MapPin, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        listOf(shop.address, placeLabel(shop.subCity), placeLabel(shop.city)).filter { it.isNotBlank() }.joinToString(", "),
                        fontFamily = SuqiFontFamily, fontSize = 14.sp, color = colors.primaryText
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            MapTilePlaceholder(label = w.mapPreviewLabel, modifier = Modifier.fillMaxWidth().height(110.dp))
        }

        // Delivery and pickup summary
        SuqiSectionCard(padding = 16.dp, modifier = Modifier.testTag("shop_about_delivery")) {
            Text(w.deliveryPickupTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.primaryText)
            Spacer(modifier = Modifier.height(8.dp))
            val enabled = deliveryOptions.filter { it.isEnabled }
            if (enabled.isEmpty()) {
                Text(w.noDeliveryOptions, fontFamily = SuqiFontFamily, fontSize = 13.sp, color = colors.secondaryText)
            } else {
                enabled.forEach { opt ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (opt.optionType == "PICKUP") LucideIcons.Store else LucideIcons.Truck, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.extras.localizeDeliveryTitle(opt.title), fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.primaryText)
                            Text(strings.extras.localizeEta(opt.estimatedTime, lang), fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.secondaryText)
                        }
                        Text(formatEtb(opt.fee), fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.primaryText)
                    }
                }
            }
            if (shop.freeDeliveryThreshold > 0.0) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(String.format(strings.sellerWork.freeDeliveryAboveTemplate, formatEtb(shop.freeDeliveryThreshold)), fontFamily = SuqiFontFamily, fontSize = 12.sp, color = SuqiSuccess)
            }
            if (shop.standardCutoffTime.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(String.format(w.cutoffTemplate, shop.standardCutoffTime), fontFamily = SuqiFontFamily, fontSize = 12.sp, color = colors.mutedText)
            }
        }

        // Return policy
        SuqiSectionCard(padding = 16.dp) {
            Text(w.returnPolicyTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.primaryText)
            Spacer(modifier = Modifier.height(6.dp))
            Text(shop.returnPolicy.ifBlank { w.noPolicyYet }, fontFamily = SuqiFontFamily, fontSize = 13.sp, lineHeight = 19.sp, color = colors.secondaryText)
        }

        // Accepted payment methods
        val methods = paymentMethods.filter { it.isEnabled }
        if (methods.isNotEmpty()) {
            SuqiSectionCard(padding = 16.dp, modifier = Modifier.testTag("shop_about_payments")) {
                Text(w.acceptedPaymentsTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.primaryText)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    methods.take(3).forEach { m ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(colors.primaryTint).padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(if (m.providerType == "BANK_TRANSFER") LucideIcons.Landmark else LucideIcons.Wallet, contentDescription = null, tint = SuqiPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(m.bankName.ifBlank { m.providerType }, fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = colors.primaryText, maxLines = 1)
                        }
                    }
                }
            }
        }

        // Social links
        val links = listOf(
            "Instagram" to shop.instagramLink,
            "TikTok" to shop.tiktokLink,
            strings.sellerWork.websiteLabel to shop.websiteUrl,
            "Telegram" to shop.telegramLink
        ).filter { it.second.isNotBlank() }
        if (links.isNotEmpty()) {
            SuqiSectionCard(padding = 10.dp) {
                Text(strings.sellerWork.socialLinksTitle, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.primaryText, modifier = Modifier.padding(start = 6.dp, top = 6.dp, bottom = 2.dp))
                links.forEach { (label, url) ->
                    SuqiListRow(
                        icon = LucideIcons.Link,
                        title = label,
                        subtitle = url,
                        onClick = {
                            val target = if (url.startsWith("http", true)) url else "https://$url"
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target))) }
                        },
                        testTag = "shop_link_$label"
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/** Static map tile stand-in with an orange pin until a map provider exists. */
@Composable
fun MapTilePlaceholder(label: String, modifier: Modifier = Modifier) {
    val colors = LocalSuqiColors.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(colors.secondaryBackground)
            .testTag("map_tile_placeholder")
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 28.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawLine(colors.border, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), strokeWidth = 1f)
                x += step
            }
            var y = 0f
            while (y < size.height) {
                drawLine(colors.border, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 1f)
                y += step
            }
        }
        Icon(LucideIcons.MapPin, contentDescription = label, tint = SuqiPrimary, modifier = Modifier.size(36.dp))
    }
}
