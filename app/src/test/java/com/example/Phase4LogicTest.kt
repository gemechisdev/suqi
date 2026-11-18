package com.example

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.BuyerNotificationPrefs
import com.example.data.local.BuyerPreferencesStore
import com.example.data.local.NotificationEntity
import com.example.data.local.OrderEntity
import com.example.data.local.SuqiDatabase
import com.example.data.model.OrderDto
import com.example.data.model.SavedAddressDto
import com.example.data.model.ShopBusinessHoursCodec
import com.example.data.model.SupportTicketDto
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.i18n.SuqiStringCatalog
import com.example.ui.screens.NotificationDayGroup
import com.example.ui.screens.estimatedArrivalDate
import com.example.ui.screens.formatBytes
import com.example.ui.screens.isAddressInUse
import com.example.ui.screens.isShopOpenNow
import com.example.ui.screens.notificationDayGroup
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
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

    private fun awaitMainCondition(timeoutMs: Long = 5_000, predicate: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!predicate() && System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
            shadowOf(Looper.getMainLooper()).idle()
        }
        assertTrue(predicate())
    }

    @Test
    fun deleteAccount_removesBuyerDataAnonymizesHistoryAndResetsSession() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = SuqiDatabase.getInstance(app)
        val vm = SuqiViewModel(app)
        shadowOf(Looper.getMainLooper()).idle()

        // Create a second independent buyer who also wishlists, follows, and chats with Tech Hub
        val otherBuyer = runBlocking {
            vm.repository.loginWithPhoneOtp("+251 911 666 777", "123456", "Other Buyer").data!!
        }
        runBlocking {
            vm.repository.toggleWishlist("prod_laptop_hp")
            vm.repository.toggleFollowShop("shop_fashionworld")
            vm.repository.sendChatMessage("shop_techhub", mapOf("message" to "Keep this seller inbox thread"))
        }
        val fashionFollowersWithOther = runBlocking { db.shopDao().getShopById("shop_fashionworld")!!.followersCount }

        val buyer = runBlocking {
            vm.repository.loginWithPhoneOtp("+251 911 777 888", "123456", "Cleanup Buyer").data!!
        }
        vm.refreshSession()
        awaitMainCondition { vm.currentUser.value?.id == buyer.id }

        val orderId = "ord_del_${System.currentTimeMillis()}"
        val reviewId = runBlocking {
            vm.repository.addToCart("prod_laptop_hp", 1, "Silver")
            vm.repository.saveAddress(
                SavedAddressDto(
                    id = "", fullName = "Cleanup Buyer", phone = buyer.phone,
                    region = "Addis Ababa", city = "Addis Ababa", subCity = "Bole",
                    streetOrLandmark = "Bole Medhanialem", isDefault = true
                )
            )
            db.notificationDao().insert(
                NotificationEntity(
                    id = "notif_del_${System.currentTimeMillis()}",
                    userId = buyer.id,
                    type = "ORDER",
                    title = "Order update",
                    body = "Body",
                    timestamp = "Just now",
                    isRead = false
                )
            )
            vm.repository.toggleWishlist("prod_laptop_hp")
            vm.repository.toggleFollowShop("shop_fashionworld")
            vm.repository.sendChatMessage("shop_techhub", mapOf("message" to "Hello from buyer"))
            vm.repository.submitSupportTicket(
                SupportTicketDto(id = "", userId = buyer.id, category = "Account", subject = "Help", description = "Need help", status = "OPEN", createdAt = "")
            )
            val rev = vm.repository.submitReview(
                mapOf("targetId" to "prod_laptop_hp", "targetType" to "PRODUCT", "rating" to 5.0, "comment" to "Great product!")
            ).data!!
            db.orderDao().insertOrder(
                OrderEntity(
                    id = orderId,
                    orderReference = "SUQ-DEL-1",
                    customerUserId = buyer.id,
                    customerName = buyer.name,
                    customerPhone = buyer.phone,
                    deliveryAddress = "Bole Medhanialem",
                    shopId = "shop_techhub",
                    shopName = "Tech Hub",
                    itemsJson = "[]",
                    subtotal = 100.0,
                    deliveryFee = 0.0,
                    discountAmount = 0.0,
                    totalAmount = 100.0,
                    deliveryOptionTitle = "Standard",
                    deliveryOptionType = "STANDARD",
                    deliveryEstimatedDays = "1-2 days",
                    paymentMethodTitle = "Telebirr",
                    paymentMethodType = "TELEBIRR",
                    paymentMethodAccount = "+251 912",
                    transactionReference = "TXN-DEL-${System.currentTimeMillis()}",
                    paymentStatus = "VERIFIED",
                    orderStatus = "Delivered",
                    createdAt = "Now",
                    payerIdentifier = buyer.phone
                )
            )
            rev.id
        }

        vm.cachedBuyerOrders = listOf(order("Bole", "Delivered"))
        vm.cachedSavedAddresses = listOf(address)

        val deleteRes = runBlocking { vm.repository.deleteAccount() }
        assertTrue(deleteRes.success)
        assertEquals(true, deleteRes.data)

        runBlocking { vm.signOut(ScreenDestination.Home) }
        awaitMainCondition { vm.currentUser.value?.role == "GUEST" }

        assertEquals("GUEST", vm.activeRole.value)
        assertTrue(vm.cachedBuyerOrders.isEmpty())
        assertTrue(vm.cachedSavedAddresses.isEmpty())
        assertEquals(listOf(ScreenDestination.Home), vm.navBackStack.value.map { it.destination })

        runBlocking {
            assertNull(db.userDao().getUserById(buyer.id))
            assertTrue(db.cartDao().getCartItems(buyer.id).isEmpty())
            assertTrue(db.savedAddressDao().getAddresses(buyer.id).isEmpty())
            assertTrue(db.notificationDao().getNotifications(buyer.id).isEmpty())
            assertTrue(db.wishlistDao().getProductIdsForUser(buyer.id).isEmpty())
            assertTrue(db.shopFollowDao().getShopIdsForUser(buyer.id).isEmpty())
            assertTrue(db.chatDao().getMessagesForCustomerDesc(buyer.id).isEmpty())
            assertTrue(db.supportTicketDao().getTicketsForUser(buyer.id).isEmpty())

            // Deleting buyer must NOT wipe seeded user_fitsum or otherBuyer data, nor seller inbox
            assertTrue(db.wishlistDao().isWishlisted("user_fitsum", "prod_laptop_hp"))
            assertTrue(db.wishlistDao().isWishlisted(otherBuyer.id, "prod_laptop_hp"))
            assertTrue(db.shopFollowDao().isFollowing("user_fitsum", "shop_techhub"))
            assertTrue(db.shopFollowDao().isFollowing(otherBuyer.id, "shop_fashionworld"))
            assertEquals(fashionFollowersWithOther, db.shopDao().getShopById("shop_fashionworld")!!.followersCount)
            assertTrue(db.chatDao().getMessagesForCustomerDesc(otherBuyer.id).any { it.message == "Keep this seller inbox thread" })
            assertTrue(db.chatDao().getMessagesForShop("shop_techhub").any { it.customerUserId == otherBuyer.id })

            val anonymizedRev = db.reviewDao().getReviewsForTarget("prod_laptop_hp").firstOrNull { it.id == reviewId }
            assertNotNull(anonymizedRev)
            assertEquals("deleted_user", anonymizedRev!!.userId)
            assertEquals("Deleted user", anonymizedRev.userName)
            assertEquals("", anonymizedRev.userAvatar)

            val anonymizedOrder = db.orderDao().getOrderById(orderId)
            assertNotNull(anonymizedOrder)
            assertEquals("deleted_user", anonymizedOrder!!.customerUserId)
            assertEquals("Deleted user", anonymizedOrder.customerName)
            assertEquals("", anonymizedOrder.customerPhone)
            assertEquals("Removed", anonymizedOrder.deliveryAddress)
            assertEquals("", anonymizedOrder.payerIdentifier)
        }
    }

    @Test
    fun perUserIsolation_wishlistFollowingAndChatsAreScopedToActiveUser() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = SuqiViewModel(app)
        shadowOf(Looper.getMainLooper()).idle()

        val buyerA = runBlocking {
            vm.repository.loginWithPhoneOtp("+251 911 200 100", "123456", "Buyer A").data!!
        }
        runBlocking {
            vm.repository.toggleWishlist("prod_laptop_lenovo")
            vm.repository.toggleFollowShop("shop_homeplus")
            vm.repository.sendChatMessage("shop_techhub", mapOf("message" to "Question from Buyer A"))
        }

        val buyerB = runBlocking {
            vm.repository.loginWithPhoneOtp("+251 911 200 200", "123456", "Buyer B").data!!
        }
        runBlocking {
            val prodsForB = vm.repository.getProducts().data.orEmpty()
            assertFalse(prodsForB.first { it.id == "prod_laptop_lenovo" }.isWishlisted)
            assertTrue(vm.repository.getShops(filter = "FOLLOWING").data.orEmpty().isEmpty())
            assertTrue(vm.repository.getChatConversations().data.orEmpty().isEmpty())
            assertTrue(vm.repository.getChatMessages("shop_techhub").data.orEmpty().isEmpty())

            vm.repository.sendChatMessage("shop_techhub", mapOf("message" to "Question from Buyer B"))
            val msgsForB = vm.repository.getChatMessages("shop_techhub").data.orEmpty()
            assertEquals(listOf("Question from Buyer B"), msgsForB.map { it.message })
        }

        // Seller of shop_techhub sees both Buyer A and Buyer B conversations in Seller Inbox and can reply per conversation
        runBlocking {
            vm.repository.loginWithPhoneOtp("+251 912 345 678", "123456")
            vm.repository.updateUserProfile(mapOf("activeRole" to "SELLER"))
            val inbox = vm.repository.getChatConversations().data.orEmpty()
            val convA = inbox.firstOrNull { it.customerUserId == buyerA.id }
            val convB = inbox.firstOrNull { it.customerUserId == buyerB.id }
            assertNotNull(convA)
            assertNotNull(convB)

            vm.repository.sendChatMessage(
                "shop_techhub",
                mapOf("senderType" to "SHOP", "message" to "Reply only to A", "conversationId" to convA!!.conversationId)
            )

            vm.repository.loginWithPhoneOtp("+251 911 200 100", "123456")
            val msgsA = vm.repository.getChatMessages("shop_techhub").data.orEmpty().map { it.message }
            assertTrue("Reply only to A" in msgsA)
            assertFalse("Question from Buyer B" in msgsA)
        }
    }

    @Test
    fun deleteAccount_blocksAccountsThatOwnAShop() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = SuqiDatabase.getInstance(app)
        val vm = SuqiViewModel(app)
        shadowOf(Looper.getMainLooper()).idle()

        val seller = runBlocking {
            vm.repository.loginWithPhoneOtp("+251 912 345 678", "123456").data!!
        }
        vm.refreshSession()
        awaitMainCondition { vm.currentUser.value?.id == seller.id }

        val deleteRes = runBlocking { vm.repository.deleteAccount() }
        assertFalse(deleteRes.success)
        assertEquals(false, deleteRes.data)
        runBlocking {
            assertNotNull(db.userDao().getUserById(seller.id))
            assertNotNull(db.shopDao().getShopByOwner(seller.id))
        }
    }
}
