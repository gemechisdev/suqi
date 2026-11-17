package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.BuyerNotificationPrefs
import com.example.data.local.BuyerPreferencesStore
import com.example.data.model.OrderDto
import com.example.data.model.SavedAddressDto
import com.example.data.model.ShopBusinessHoursCodec
import com.example.ui.i18n.SuqiStringCatalog
import com.example.ui.screens.NotificationDayGroup
import com.example.ui.screens.estimatedArrivalDate
import com.example.ui.screens.formatBytes
import com.example.ui.screens.isAddressInUse
import com.example.ui.screens.isShopOpenNow
import com.example.ui.screens.notificationDayGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Phase4LogicTest {

    private fun noonToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    @Test
    fun notificationGrouping_bucketsTodayYesterdayEarlier() {
        val now = noonToday()
        assertEquals(NotificationDayGroup.TODAY, notificationDayGroup("10 min ago", now))
        assertEquals(NotificationDayGroup.TODAY, notificationDayGroup("2 hours ago", now))
        assertEquals(NotificationDayGroup.TODAY, notificationDayGroup("Just now", now))
        assertEquals(NotificationDayGroup.YESTERDAY, notificationDayGroup("Yesterday", now))
        assertEquals(NotificationDayGroup.EARLIER, notificationDayGroup("3 days ago", now))
        assertEquals(NotificationDayGroup.EARLIER, notificationDayGroup("1 week ago", now))
        assertEquals(NotificationDayGroup.EARLIER, notificationDayGroup("not a date", now))
    }

    private fun cal(day: Int, hour: Int, min: Int = 0) = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_WEEK, day); set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, min)
    }

    @Test
    fun shopOpenNow_followsWeeklyHours() {
        val week = ShopBusinessHoursCodec.defaultWeek() // Mon-Sat 08:30-19:30, Sun closed
        assertTrue(isShopOpenNow(week, cal(Calendar.MONDAY, 10)))
        assertFalse(isShopOpenNow(week, cal(Calendar.MONDAY, 8, 0)))
        assertFalse(isShopOpenNow(week, cal(Calendar.MONDAY, 20)))
        assertFalse(isShopOpenNow(week, cal(Calendar.SUNDAY, 12)))
    }

    private fun order(address: String, status: String, created: String = "Nov 10, 2026 • 09:00 AM", eta: String = "2-3 days") = OrderDto(
        id = "o1", orderReference = "SUQ-1", customerUserId = "u", customerName = "n", customerPhone = "p",
        deliveryAddress = address, shopId = "s", shopName = "Shop", items = emptyList(),
        subtotal = 0.0, deliveryFee = 0.0, discountAmount = 0.0, totalAmount = 0.0,
        deliveryOptionTitle = "Standard", deliveryOptionType = "STANDARD", deliveryEstimatedDays = eta,
        paymentMethodTitle = "Telebirr", paymentMethodType = "TELEBIRR", paymentMethodAccount = "a",
        transactionReference = "T", paymentStatus = "VERIFIED", orderStatus = status, createdAt = created
    )

    private val address = SavedAddressDto(
        id = "a1", fullName = "n", phone = "p", region = "Addis Ababa", city = "Addis Ababa",
        subCity = "Bole", streetOrLandmark = "Edna Mall"
    )

    @Test
    fun addressInUse_onlyForActiveOrdersMentioningTheLandmark() {
        assertTrue(isAddressInUse(address, listOf(order("Near Edna Mall, Bole", "Processing"))))
        assertFalse(isAddressInUse(address, listOf(order("Near Edna Mall, Bole", "Delivered"))))
        assertFalse(isAddressInUse(address, listOf(order("Near Edna Mall, Bole", "Cancelled"))))
        assertFalse(isAddressInUse(address, listOf(order("Piassa", "Processing"))))
    }

    @Test
    fun estimatedArrival_usesUpperBoundOfWindowAndSkipsClosedOrders() {
        assertEquals("Fri, 13 Nov", estimatedArrivalDate(order("x", "Confirmed")))
        assertNull(estimatedArrivalDate(order("x", "Delivered")))
        assertNull(estimatedArrivalDate(order("x", "Cancelled")))
        assertNull(estimatedArrivalDate(order("x", "Confirmed", eta = "")))
    }

    @Test
    fun buyerPreferences_roundTripAndClear() {
        val store = BuyerPreferencesStore(ApplicationProvider.getApplicationContext<Application>())
        store.clearAll()
        assertTrue(store.loadNotificationPrefs().orderUpdates)
        store.saveNotificationPrefs(BuyerNotificationPrefs(chatMessages = false, quietHoursEnabled = true, quietStart = "21:00", quietEnd = "06:30"))
        val loaded = store.loadNotificationPrefs()
        assertFalse(loaded.chatMessages)
        assertTrue(loaded.quietHoursEnabled)
        assertEquals("21:00", loaded.quietStart)
        assertEquals("06:30", loaded.quietEnd)
        store.saveDataSaver(true)
        assertTrue(store.loadDataSaver())
        store.clearAll()
        assertFalse(store.loadDataSaver())
        assertTrue(store.loadNotificationPrefs().chatMessages)
    }

    @Test
    fun formatBytes_scalesUnits() {
        assertEquals("512 B", formatBytes(512))
        assertEquals("1.5 KB", formatBytes(1536))
        assertEquals("2.0 MB", formatBytes(2L * 1024 * 1024))
    }

    @Test
    fun phase4Strings_haveNoMissingTranslationsAndKeepFormatSpecifiers() {
        assertTrue(SuqiStringCatalog.findMissingTranslations().none { it.contains("buyerWork") })
        val en = SuqiStringCatalog.english.buyerWork
        val am = SuqiStringCatalog.amharic.buyerWork
        val om = SuqiStringCatalog.afaanOromo.buyerWork
        listOf(en, am, om).forEach {
            assertTrue(it.cutoffTemplate.contains("%s"))
            assertTrue(it.etaArrivesTemplate.contains("%s"))
            assertTrue(it.deleteTypePromptTemplate.contains("%s"))
            assertTrue(it.clearCacheSizeTemplate.contains("%s"))
            assertTrue(it.joinedLabelTemplate.contains("%s"))
        }
        assertEquals("DELETE", en.deleteConfirmWord)
    }
}
