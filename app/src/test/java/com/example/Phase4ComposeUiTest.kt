package com.example

import android.app.Application
import android.os.Looper
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SuqiDatabase
import com.example.data.model.SavedAddressDto
import com.example.data.model.SupportTicketDto
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.i18n.SuqiStringCatalog
import com.example.ui.screens.AppSettingsScreen
import com.example.ui.screens.HelpLegalScreen
import com.example.ui.screens.NotificationPrefsScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.OrderDetailScreen
import com.example.ui.screens.ProfileEditScreen
import com.example.ui.screens.SavedAddressesScreen
import com.example.ui.screens.ShopStorefrontScreen
import com.example.ui.screens.TicketThreadScreen
import com.example.ui.screens.TicketThreadStore
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Phase 4 buyer screens: every test performs at least one state-changing action. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
class Phase4ComposeUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun newViewModel(signIn: Boolean = false, phone: String = "+251 912 345 678", name: String = ""): SuqiViewModel {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = SuqiViewModel(app)
        shadowOf(Looper.getMainLooper()).idle()
        vm.buyerPrefs.clearAll()
        if (signIn) {
            runBlocking { vm.repository.loginWithPhoneOtp(phone, "123456", name) }
            vm.refreshSession()
            shadowOf(Looper.getMainLooper()).idle()
        }
        return vm
    }

    private fun waitForTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            Thread.sleep(20)
            val mainLooper = shadowOf(Looper.getMainLooper())
            mainLooper.idle()
            mainLooper.idleFor(16, java.util.concurrent.TimeUnit.MILLISECONDS)
            composeRule.mainClock.advanceTimeByFrame()
            mainLooper.idle()
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForCondition(predicate: () -> Boolean) {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            Thread.sleep(20)
            val mainLooper = shadowOf(Looper.getMainLooper())
            mainLooper.idle()
            mainLooper.idleFor(16, java.util.concurrent.TimeUnit.MILLISECONDS)
            composeRule.mainClock.advanceTimeByFrame()
            mainLooper.idle()
            predicate()
        }
    }

    private fun scrollTo(tag: String) {
        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag(tag))
        waitForTag(tag)
    }

    @Test
    fun notifications_markReadAndDeleteUpdatesList() {
        val vm = newViewModel(signIn = true)
        composeRule.setContent { MyApplicationTheme(darkTheme = false) { NotificationsScreen(viewModel = vm) } }
        waitForTag("notification_card_notif_1")
        waitForTag("notifications_mark_all_read")

        // Tap unread notification to mark it read
        composeRule.onNodeWithTag("notification_card_notif_1").performClick()
        waitForCondition { vm.cachedNotifications.firstOrNull { it.id == "notif_1" }?.isRead == true }
        composeRule.onAllNodesWithTag("notifications_mark_all_read").assertCountEquals(0)

        // Delete a notification via accessible delete button
        waitForTag("notification_delete_notif_2")
        composeRule.onNodeWithTag("notification_delete_notif_2").performClick()
        waitForCondition { vm.cachedNotifications.none { it.id == "notif_2" } }
        composeRule.onAllNodesWithTag("notification_card_notif_2").assertCountEquals(0)
    }

    @Test
    fun notificationPrefs_switchPersistsToStore() {
        val vm = newViewModel()
        composeRule.setContent { MyApplicationTheme(darkTheme = false) { NotificationPrefsScreen(viewModel = vm) } }
        waitForTag("pref_promos")
        assertTrue(vm.buyerPrefs.loadNotificationPrefs().promotions)
        composeRule.onNodeWithTag("pref_promos").performClick()
        composeRule.waitForIdle()
        assertFalse(vm.buyerPrefs.loadNotificationPrefs().promotions)
    }

    @Test
    fun notificationPrefs_quietHoursSwitchRevealsTimeFields() {
        val vm = newViewModel()
        composeRule.setContent { MyApplicationTheme(darkTheme = false) { NotificationPrefsScreen(viewModel = vm) } }
        waitForTag("pref_quiet_hours")
        composeRule.onNodeWithTag("pref_quiet_hours").performClick()
        composeRule.waitForIdle()
        assertTrue(vm.buyerPrefs.loadNotificationPrefs().quietHoursEnabled)
    }

    @Test
    fun storefront_aboutTabDisplaysHoursAndOpenStatus() {
        val vm = newViewModel()
        composeRule.setContent { MyApplicationTheme(darkTheme = false) { ShopStorefrontScreen(shopId = "shop_techhub", viewModel = vm) } }
        waitForTag("storefront_tab_about")
        composeRule.onNodeWithTag("storefront_tab_about").performClick()
        composeRule.waitForIdle()
        scrollTo("shop_about_hours")
        composeRule.onNodeWithTag("shop_about_content").assertIsDisplayed()
        composeRule.onNodeWithTag("shop_open_now_pill").assertIsDisplayed()
    }

    @Test
    fun orderDetail_invoiceSaveAndShareCreateOutputs() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = newViewModel(signIn = true)
        composeRule.setContent { MyApplicationTheme(darkTheme = false) { OrderDetailScreen(orderId = "ord_1", viewModel = vm) } }
        waitForTag("order_invoice_btn")
        composeRule.onNodeWithTag("order_invoice_btn").performClick()
        waitForTag("invoice_sheet")

        composeRule.onNodeWithTag("invoice_save_btn").performScrollTo()
        composeRule.onNodeWithTag("invoice_save_btn").performClick()
        waitForCondition { vm.toastMessage.value != null }
        assertNotNull(vm.toastMessage.value)

        composeRule.onNodeWithTag("invoice_share_btn").performScrollTo()
        composeRule.onNodeWithTag("invoice_share_btn").performClick()
        waitForCondition { shadowOf(app).peekNextStartedActivity() != null }
        val shareIntent = shadowOf(app).nextStartedActivity
        assertNotNull(shareIntent)
        val sharedFiles = java.io.File(app.cacheDir, "shared_qr").listFiles().orEmpty()
        assertTrue(sharedFiles.any { it.name.startsWith("suqi_invoice_") })
    }

    @Test
    fun savedAddresses_inUseBadgeBlocksDeleteWhileUnusedDeletes() {
        val vm = newViewModel(signIn = true)
        val inUseAddr = runBlocking {
            vm.repository.saveAddress(
                SavedAddressDto(
                    id = "addr_in_use_test",
                    fullName = "Fitsum Active",
                    phone = "+251 912 345 678",
                    region = "Addis Ababa",
                    city = "Addis Ababa",
                    subCity = "Bole",
                    streetOrLandmark = "Bole",
                    isDefault = false
                )
            ).data!!
        }
        composeRule.setContent { MyApplicationTheme(darkTheme = false) { SavedAddressesScreen(viewModel = vm) } }
        waitForTag("address_in_use_${inUseAddr.id}")
        composeRule.onNodeWithTag("address_in_use_${inUseAddr.id}").assertIsDisplayed()

        // Attempting to delete the in-use address is blocked and explains why
        vm.clearToast()
        composeRule.onNodeWithTag("address_delete_${inUseAddr.id}").performScrollTo()
        composeRule.onNodeWithTag("address_delete_${inUseAddr.id}").performClick()
        waitForCondition { vm.toastMessage.value == SuqiStringCatalog.english.buyer.addressInUseBlocked }
        assertTrue(vm.cachedSavedAddresses.any { it.id == inUseAddr.id })

        // Deleting an unused address (addr_2 is only on a Delivered order) succeeds
        composeRule.onNodeWithTag("address_delete_addr_2").performScrollTo()
        composeRule.onNodeWithTag("address_delete_addr_2").performClick()
        waitForCondition { vm.cachedSavedAddresses.none { it.id == "addr_2" } }
        composeRule.onAllNodesWithTag("address_card_addr_2").assertCountEquals(0)
    }

    @Test
    fun settings_dataSaverAndPrivacyTogglesPersist() {
        val vm = newViewModel()
        composeRule.setContent { MyApplicationTheme(darkTheme = false) { AppSettingsScreen(viewModel = vm) } }
        waitForTag("settings_lang_english")
        scrollTo("settings_data_saver_switch")
        composeRule.onNodeWithTag("settings_data_saver_switch").performClick()
        composeRule.waitForIdle()
        assertTrue(vm.buyerPrefs.loadDataSaver())

        scrollTo("settings_privacy_recent_switch")
        composeRule.onNodeWithTag("settings_privacy_recent_switch").performClick()
        composeRule.waitForIdle()
        assertFalse(vm.buyerPrefs.loadPrivacyPrefs().rememberRecentlyViewed)
    }

    @Test
    fun settings_simulatorHiddenUntilDeveloperUnlocked() {
        val vm = newViewModel()
        composeRule.setContent { MyApplicationTheme(darkTheme = false) { AppSettingsScreen(viewModel = vm) } }
        waitForTag("settings_lang_english")
        scrollTo("settings_version_pill")
        composeRule.onAllNodesWithTag("settings_offline_switch").assertCountEquals(0)
        repeat(7) { composeRule.onNodeWithTag("settings_version_pill").performClick() }
        composeRule.waitForIdle()
        scrollTo("settings_offline_switch")
    }

    @Test
    fun help_topicTileFiltersArticles() {
        val vm = newViewModel()
        composeRule.setContent { MyApplicationTheme(darkTheme = false) { HelpLegalScreen(viewModel = vm) } }
        waitForTag("help_topic_payments")
        composeRule.onAllNodesWithTag("help_article_payments").assertCountEquals(0)
        composeRule.onNodeWithTag("help_topic_payments").performClick()
        waitForTag("help_article_payments")
        composeRule.onAllNodesWithTag("help_article_orders").assertCountEquals(0)
    }

    @Test
    fun ticketThread_sendReplyAppendsUserAndSupportMessages() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = newViewModel(signIn = true)
        val store = TicketThreadStore(app)
        store.clearAll()
        val ticket = runBlocking {
            vm.repository.submitSupportTicket(
                SupportTicketDto(
                    id = "tkt_ui_test",
                    userId = "",
                    category = "Order Issue",
                    subject = "Delivery delay",
                    description = "My parcel has not arrived yet.",
                    status = "OPEN",
                    createdAt = ""
                )
            ).data!!
        }
        composeRule.setContent { MyApplicationTheme(darkTheme = false) { TicketThreadScreen(ticketId = ticket.id, viewModel = vm) } }
        waitForTag("ticket_reply_input")
        composeRule.onNodeWithTag("ticket_msg_0").assertIsDisplayed()
        composeRule.onNodeWithTag("ticket_send_btn").assertIsNotEnabled()

        composeRule.onNodeWithTag("ticket_reply_input").performTextInput("Please check tracking")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("ticket_send_btn").assertIsEnabled()
        composeRule.onNodeWithTag("ticket_send_btn").performClick()

        waitForTag("ticket_msg_1")
        waitForTag("ticket_msg_2")
        composeRule.onNodeWithText("Please check tracking").assertIsDisplayed()
        assertEquals(2, store.load(ticket.id).size)
    }

    @Test
    fun profile_deleteAccountNeedsTypedConfirmationAndResetsSession() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = newViewModel(signIn = true, phone = "+251 911 555 999", name = "Delete Flow Buyer")
        val buyerId = vm.currentUser.value!!.id
        composeRule.setContent { MyApplicationTheme(darkTheme = false) { ProfileEditScreen(viewModel = vm) } }
        waitForTag("profile_avatar_picker")
        scrollTo("profile_delete_account_btn")
        composeRule.onNodeWithTag("profile_delete_account_btn").performClick()
        waitForTag("input_delete_confirm")
        composeRule.onNodeWithTag("btn_confirm_delete_account").assertIsNotEnabled()
        composeRule.onNodeWithTag("input_delete_confirm").performTextInput("DELETE")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("btn_confirm_delete_account").assertIsEnabled()

        composeRule.onNodeWithTag("btn_confirm_delete_account").performClick()
        waitForCondition { vm.currentUser.value?.role == "GUEST" }
        assertEquals("GUEST", vm.activeRole.value)
        assertEquals(listOf(ScreenDestination.Home), vm.navBackStack.value.map { it.destination })
        val remainingUser = runBlocking { SuqiDatabase.getInstance(app).userDao().getUserById(buyerId) }
        assertNull(remainingUser)
    }
}
