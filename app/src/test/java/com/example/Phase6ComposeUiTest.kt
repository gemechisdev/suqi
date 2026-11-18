package com.example

import android.app.Application
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.OrderEntity
import com.example.data.local.SuqiDatabase
import com.example.ui.SuqiViewModel
import com.example.ui.screens.AdminAuditLogScreen
import com.example.ui.screens.AdminContentManagerScreen
import com.example.ui.screens.AdminListingsModerationScreen
import com.example.ui.screens.AdminLocationsManagerScreen
import com.example.ui.screens.AdminOrdersDisputesScreen
import com.example.ui.screens.AdminPaymentProvidersScreen
import com.example.ui.screens.AdminPlatformSwitchesScreen
import com.example.ui.screens.AdminReportsExportsScreen
import com.example.ui.screens.AdminShopDetailScreen
import com.example.ui.screens.AdminShopsManagerScreen
import com.example.ui.screens.AdminUserDetailScreen
import com.example.ui.screens.AdminUsersScreen
import com.example.ui.screens.BuyerCompareScreen
import com.example.ui.screens.BuyerCouponsScreen
import com.example.ui.screens.BuyerWishlistScreen
import com.example.ui.screens.SellerCollectionsScreen
import com.example.ui.screens.SellerCustomersScreen
import com.example.ui.screens.SellerReviewsQAScreen
import com.example.ui.screens.SellerStaffAccountsScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

/**
 * Phase 6 UI tests, one per new screen (19 routes; user and shop detail are separate screens).
 * Screens whose actions are still stubs (locations, provider switch, dispute buttons, seller depth screens)
 * are covered for what they really do today: render real data and expose their controls.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
class Phase6ComposeUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sellerPhone = "+251 912 345 678"

    @Before
    fun clearFileProviderCache() {
        runCatching {
            val field = androidx.core.content.FileProvider::class.java.getDeclaredField("sCache")
            field.isAccessible = true
            (field.get(null) as? MutableMap<*, *>)?.clear()
        }
    }

    private fun awaitMainLooper(timeoutMs: Long = 5_000L, predicate: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!predicate() && System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(15)
        }
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun newBuyerViewModel(phone: String, name: String): SuqiViewModel {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = SuqiViewModel(app)
        shadowOf(Looper.getMainLooper()).idle()
        vm.buyerPrefs.clearAll()
        val user = runBlocking { vm.repository.loginWithPhoneOtp(phone, "123456", name).data!! }
        vm.refreshSession()
        awaitMainLooper { vm.currentUser.value?.id == user.id }
        return vm
    }

    private fun newSellerViewModel(): SuqiViewModel {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = SuqiViewModel(app)
        shadowOf(Looper.getMainLooper()).idle()
        vm.buyerPrefs.clearAll()
        val user = runBlocking {
            vm.repository.loginWithPhoneOtp(sellerPhone, "123456").data!!
            vm.repository.updateUserProfile(mapOf("activeRole" to "SELLER")).data!!
        }
        vm.refreshSession()
        awaitMainLooper { vm.currentUser.value?.id == user.id && vm.currentUser.value?.activeRole == "SELLER" }
        return vm
    }

    private fun newAdminViewModel(): SuqiViewModel {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = SuqiViewModel(app)
        shadowOf(Looper.getMainLooper()).idle()
        vm.buyerPrefs.clearAll()
        val user = runBlocking { vm.repository.adminSignIn("admin@suqi.et", "123456", "OWNER").data!! }
        vm.refreshSession()
        awaitMainLooper { vm.currentUser.value?.id == user.id && vm.currentUser.value?.activeRole == "ADMIN" }
        return vm
    }

    private fun pump() {
        Thread.sleep(20)
        val mainLooper = shadowOf(Looper.getMainLooper())
        mainLooper.idle()
        mainLooper.idleFor(16, TimeUnit.MILLISECONDS)
        composeRule.mainClock.advanceTimeByFrame()
        mainLooper.idle()
    }

    private fun waitForTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            pump()
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForCondition(predicate: () -> Boolean) {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            pump()
            predicate()
        }
    }

    private fun scrollListToTag(tag: String) {
        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag(tag))
        waitForTag(tag)
    }

    private fun show(content: @androidx.compose.runtime.Composable () -> Unit) {
        composeRule.setContent { MyApplicationTheme(darkTheme = false) { content() } }
    }

    private fun rejectedOrder(id: String, buyerId: String) = OrderEntity(
        id = id,
        orderReference = "SUQ-P6UI-${id.takeLast(6)}",
        customerUserId = buyerId,
        customerName = "Dispute UI Buyer",
        customerPhone = "+251 911 710 000",
        deliveryAddress = "Bole",
        shopId = "shop_techhub",
        shopName = "Tech Hub",
        itemsJson = "[]",
        subtotal = 900.0,
        deliveryFee = 0.0,
        discountAmount = 0.0,
        totalAmount = 900.0,
        deliveryOptionTitle = "Standard",
        deliveryOptionType = "STANDARD",
        deliveryEstimatedDays = "1-2 days",
        paymentMethodTitle = "Telebirr",
        paymentMethodType = "TELEBIRR",
        paymentMethodAccount = "+251 912",
        transactionReference = "TXN-P6UI-$id",
        paymentStatus = "REJECTED",
        orderStatus = "Processing",
        createdAt = "Just now",
        payerIdentifier = "+251 911 710 000"
    )

    // ---------------- Admin: platform management ----------------

    @Test
    fun adminUsers_listsBuyerAndSearchFilters() {
        val buyerVm = newBuyerViewModel("+251 911 710 101", "Zelalem Searchable")
        val buyerId = buyerVm.currentUser.value!!.id
        val vm = newAdminViewModel()
        show { AdminUsersScreen(viewModel = vm) }

        waitForTag("admin_user_card_$buyerId")
        composeRule.onNodeWithTag("admin_users_search_field").performTextInput("Zelalem")
        waitForTag("admin_user_card_$buyerId")
        composeRule.onNodeWithTag("admin_user_card_$buyerId").assertIsDisplayed()

        composeRule.onNodeWithTag("admin_users_search_field").performTextInput("zzzz-no-match")
        waitForCondition { composeRule.onAllNodesWithTag("admin_user_card_$buyerId").fetchSemanticsNodes().isEmpty() }
    }

    @Test
    fun adminUserDetail_suspendFlowPersistsAndWritesAudit() {
        val buyerVm = newBuyerViewModel("+251 911 710 102", "Suspend UI Buyer")
        val buyerId = buyerVm.currentUser.value!!.id
        val vm = newAdminViewModel()
        show { AdminUserDetailScreen(userId = buyerId, viewModel = vm) }

        waitForTag("admin_user_detail_header")
        scrollListToTag("admin_user_suspend_btn")
        composeRule.onNodeWithTag("admin_user_suspend_btn").performClick()

        waitForTag("admin_user_suspend_reason")
        composeRule.onNodeWithTag("admin_user_suspend_reason").performTextInput("Fake orders")
        composeRule.onNodeWithTag("admin_user_suspend_confirm").performClick()

        waitForCondition { runBlocking { vm.repository.getAdminUserDetail(buyerId).data?.isSuspended == true } }
        assertTrue(runBlocking { vm.repository.getAuditLog().data.orEmpty().any { it.action == "USER_SUSPENDED" && it.targetId == buyerId } })
    }

    @Test
    fun adminShopsManager_listsSeededShop() {
        val vm = newAdminViewModel()
        show { AdminShopsManagerScreen(viewModel = vm) }
        waitForTag("admin_shop_card_shop_techhub")
        composeRule.onNodeWithTag("admin_shop_card_shop_techhub").assertIsDisplayed()
    }

    @Test
    fun adminShopDetail_saveNotesPersists() {
        val vm = newAdminViewModel()
        show { AdminShopDetailScreen(shopId = "shop_techhub", viewModel = vm) }

        waitForTag("admin_shop_tab_NOTES")
        composeRule.onNodeWithTag("admin_shop_tab_NOTES").performClick()
        waitForTag("admin_shop_notes_input")
        composeRule.onNodeWithTag("admin_shop_notes_input").performTextInput("Verified docs in person")
        scrollListToTag("admin_shop_notes_save")
        composeRule.onNodeWithTag("admin_shop_notes_save").performClick()

        waitForCondition {
            runBlocking { vm.repository.getAdminShopDetail("shop_techhub").data?.notes.orEmpty().contains("Verified docs in person") }
        }
    }

    @Test
    fun adminAuditLog_showsEntriesWrittenByAdminActions() {
        val vm = newAdminViewModel()
        runBlocking { vm.repository.savePlatformSwitch("compare_products", true) }
        val entryId = runBlocking { vm.repository.getAuditLog().data!!.first { it.action == "SWITCH_UPDATED" }.id }
        show { AdminAuditLogScreen(viewModel = vm) }
        waitForTag("audit_entry_$entryId")
        composeRule.onNodeWithTag("audit_entry_$entryId").assertIsDisplayed()
    }

    // ---------------- Admin: ops ----------------

    @Test
    fun adminListingsModeration_rendersTabsWithoutCrashing() {
        val vm = newAdminViewModel()
        show { AdminListingsModerationScreen(viewModel = vm) }
        waitForTag("moderation_tab_REPORTED")
        composeRule.onNodeWithTag("moderation_tab_REPORTED").assertIsDisplayed()
    }

    @Test
    fun adminOrdersDisputes_showsRejectedPaymentCaseWithDecisionButtons() {
        val db = SuqiDatabase.getInstance(ApplicationProvider.getApplicationContext<Application>())
        val buyerVm = newBuyerViewModel("+251 911 710 103", "Dispute UI Buyer")
        val orderId = "ord_p6ui_${System.currentTimeMillis()}"
        runBlocking { db.orderDao().insertOrder(rejectedOrder(orderId, buyerVm.currentUser.value!!.id)) }
        val vm = newAdminViewModel()
        show { AdminOrdersDisputesScreen(viewModel = vm) }

        waitForTag("dispute_card_$orderId")
        scrollListToTag("dispute_refund_$orderId")
        composeRule.onNodeWithTag("dispute_refund_$orderId").assertIsDisplayed()
        composeRule.onNodeWithTag("dispute_escalate_$orderId").assertIsDisplayed()
        // Today the buttons only show a toast and do not call decideDispute (see Phase 6 findings), so state is unchanged.
        assertEquals("OPEN", runBlocking { vm.repository.getAdminDisputes().data.orEmpty().first { it.orderId == orderId }.status })
    }

    @Test
    fun adminReportsExports_rendersKindSelector() {
        val vm = newAdminViewModel()
        show { AdminReportsExportsScreen(viewModel = vm) }
        waitForTag("report_kind_SALES")
        composeRule.onNodeWithTag("report_kind_SALES").assertIsDisplayed()
    }

    // ---------------- Admin: config ----------------

    @Test
    fun adminLocations_showsSeededAreasAndDefaultZoneToggle() {
        val vm = newAdminViewModel()
        show { AdminLocationsManagerScreen(viewModel = vm) }
        waitForTag("default_zone_loc_bole")
        composeRule.onNodeWithTag("default_zone_loc_bole").assertIsDisplayed()
        assertFalse(composeRule.onAllNodesWithTag("locations_empty").fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun adminPaymentProviders_showsSeededProviders() {
        val vm = newAdminViewModel()
        show { AdminPaymentProvidersScreen(viewModel = vm) }
        waitForTag("provider_active_prov_telebirr")
        composeRule.onNodeWithTag("provider_active_prov_telebirr").assertIsDisplayed()
        assertFalse(composeRule.onAllNodesWithTag("providers_empty").fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun adminContentManager_showsSeededEntries() {
        val vm = newAdminViewModel()
        show { AdminContentManagerScreen(viewModel = vm) }
        waitForCondition { composeRule.onAllNodesWithTag("content_empty").fetchSemanticsNodes().isEmpty() && runBlocking { vm.repository.getContentEntries().data.orEmpty().isNotEmpty() } }
        assertTrue(runBlocking { vm.repository.getContentEntries().data.orEmpty().any { it.section == "FAQ" } })
    }

    @Test
    fun adminPlatformSwitches_toggleSavesAndWritesAudit() {
        val vm = newAdminViewModel()
        show { AdminPlatformSwitchesScreen(viewModel = vm) }

        waitForTag("switch_video_uploads")
        composeRule.onNodeWithTag("switch_video_uploads").performClick()
        waitForCondition {
            runBlocking { vm.repository.getPlatformSwitches().data.orEmpty().first { it.id == "video_uploads" }.isEnabled.not() }
        }
        assertTrue(runBlocking { vm.repository.getAuditLog().data.orEmpty().any { it.action == "SWITCH_UPDATED" && it.targetId == "video_uploads" } })
    }

    // ---------------- Buyer ----------------

    @Test
    fun buyerWishlist_showsEmptyStateForNewBuyer() {
        val vm = newBuyerViewModel("+251 911 710 104", "Empty Wishlist Buyer")
        show { BuyerWishlistScreen(viewModel = vm) }
        waitForTag("wishlist_empty")
        composeRule.onNodeWithTag("wishlist_empty").assertIsDisplayed()
    }

    @Test
    fun buyerCoupons_codeFieldAcceptsInput() {
        val vm = newBuyerViewModel("+251 911 710 105", "Coupon Buyer")
        show { BuyerCouponsScreen(viewModel = vm) }
        waitForTag("buyer_coupon_code")
        composeRule.onNodeWithTag("buyer_coupon_code").performTextInput("WELCOME10")
        composeRule.onNodeWithTag("buyer_coupon_code").assertIsDisplayed()
    }

    @Test
    fun buyerCompare_showsEmptyStateWhenNothingSelected() {
        val vm = newBuyerViewModel("+251 911 710 106", "Compare Buyer")
        show { BuyerCompareScreen(viewModel = vm) }
        waitForTag("compare_empty")
        composeRule.onNodeWithTag("compare_empty").assertIsDisplayed()
    }

    // ---------------- Seller ----------------

    @Test
    fun sellerReviewsQA_switchesBetweenTabs() {
        val vm = newSellerViewModel()
        show { SellerReviewsQAScreen(viewModel = vm) }
        waitForTag("seller_review_tab_REVIEWS")
        composeRule.onNodeWithTag("seller_review_tab_REVIEWS").assertIsDisplayed()
        composeRule.onNodeWithTag("seller_review_tab_QUESTIONS").performClick()
        composeRule.onNodeWithTag("seller_review_tab_QUESTIONS").assertIsDisplayed()
    }

    @Test
    fun sellerCustomers_composesForSeller() {
        val vm = newSellerViewModel()
        show { SellerCustomersScreen(viewModel = vm) }
        // SellerCustomersScreen loads via the admin-only getAdminUsers(), so a seller sees the empty state (known gap).
        waitForTag("seller_customers_empty")
    }

    @Test
    fun sellerCollections_composesForSeller() {
        val vm = newSellerViewModel()
        show { SellerCollectionsScreen(viewModel = vm) }
        composeRule.waitForIdle()
    }

    @Test
    fun sellerStaffAccounts_composesForSeller() {
        val vm = newSellerViewModel()
        show { SellerStaffAccountsScreen(viewModel = vm) }
        // Same admin-only data source as Customers, so a seller sees the empty state (known gap).
        waitForTag("seller_staff_empty")
    }
}
