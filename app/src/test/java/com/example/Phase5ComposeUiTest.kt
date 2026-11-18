package com.example

import android.app.Application
import android.os.Looper
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
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
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.OrderEntity
import com.example.data.local.ReturnRequestEntity
import com.example.data.local.SuqiDatabase
import com.example.ui.SuqiViewModel
import com.example.ui.screens.AdminBroadcastScreen
import com.example.ui.screens.AdminFinanceScreen
import com.example.ui.screens.AdminModerationV2Screen
import com.example.ui.screens.AdminPlansV2Screen
import com.example.ui.screens.AdminSupportInboxScreen
import com.example.ui.screens.BuyerDealsScreen
import com.example.ui.screens.PromotionsV2Screen
import com.example.ui.screens.ReturnRequestScreen
import com.example.ui.screens.ReturnStatusScreen
import com.example.ui.screens.ReviewsV2Screen
import com.example.ui.screens.SellerAnalyticsScreen
import com.example.ui.screens.SellerEarningsScreen
import com.example.ui.screens.SellerReturnsScreen
import com.example.ui.screens.SellerSubscriptionV2Screen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

/** Phase 5 UI tests: at least one state-changing Robolectric Compose test for each of the 14 Phase 5 screens. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
class Phase5ComposeUiTest {

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

    private fun newBuyerViewModel(phone: String = "+251 911 600 101", name: String = "Phase5 Buyer"): SuqiViewModel {
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
        val user = runBlocking {
            vm.repository.adminSignIn("admin@suqi.et", "123456", "OWNER").data!!
        }
        vm.refreshSession()
        awaitMainLooper { vm.currentUser.value?.id == user.id && vm.currentUser.value?.activeRole == "ADMIN" }
        return vm
    }

    private fun waitForTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            Thread.sleep(20)
            val mainLooper = shadowOf(Looper.getMainLooper())
            mainLooper.idle()
            mainLooper.idleFor(16, TimeUnit.MILLISECONDS)
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
            mainLooper.idleFor(16, TimeUnit.MILLISECONDS)
            composeRule.mainClock.advanceTimeByFrame()
            mainLooper.idle()
            predicate()
        }
    }

    private fun scrollListToTag(tag: String) {
        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag(tag))
        waitForTag(tag)
    }

    private fun makeDeliveredOrder(id: String, buyerId: String, buyerName: String, buyerPhone: String, amount: Double = 5000.0) = OrderEntity(
        id = id,
        orderReference = "SUQ-UI-${id.takeLast(6)}",
        customerUserId = buyerId,
        customerName = buyerName,
        customerPhone = buyerPhone,
        deliveryAddress = "Bole, Addis Ababa",
        shopId = "shop_techhub",
        shopName = "Tech Hub",
        itemsJson = "[{\"productId\":\"prod_laptop_hp\",\"productName\":\"HP Laptop 15.6\",\"productImageUrl\":\"\",\"variation\":\"Silver\",\"unitPrice\":$amount,\"quantity\":1,\"totalPrice\":$amount}]",
        subtotal = amount,
        deliveryFee = 0.0,
        discountAmount = 0.0,
        totalAmount = amount,
        deliveryOptionTitle = "Standard Delivery",
        deliveryOptionType = "STANDARD",
        deliveryEstimatedDays = "1-2 days",
        paymentMethodTitle = "Telebirr",
        paymentMethodType = "TELEBIRR",
        paymentMethodAccount = "+251 912 345 678",
        transactionReference = "TXN-UI-$id",
        paymentStatus = "VERIFIED",
        orderStatus = "Delivered",
        createdAt = "Just now",
        deliveredAt = "Just now",
        transferredAmount = amount,
        payerIdentifier = buyerPhone
    )

    @Test
    fun returnRequest_stepsAndSubmitsReturn() {
        val db = SuqiDatabase.getInstance(ApplicationProvider.getApplicationContext<Application>())
        val phone = "+251 911 600 111"
        val vm = newBuyerViewModel(phone = phone, name = "Return Wizard Buyer")
        val buyerId = vm.currentUser.value!!.id
        val orderId = "ord_ui_ret_${System.currentTimeMillis()}"
        runBlocking { db.orderDao().insertOrder(makeDeliveredOrder(orderId, buyerId, "Return Wizard Buyer", phone)) }

        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) { ReturnRequestScreen(orderId = orderId, viewModel = vm) }
        }

        waitForTag("return_item_prod_laptop_hp")
        waitForTag("return_next_step_btn")
        composeRule.onNodeWithTag("return_next_step_btn").performClick()

        waitForTag("return_reason_1")
        composeRule.onNodeWithTag("return_reason_1").performClick()
        composeRule.onNodeWithTag("return_comment_input").performTextInput("Hinge feels loose")
        composeRule.onNodeWithTag("return_next_step_btn").performClick()

        waitForTag("return_submit_btn")
        composeRule.onNodeWithTag("return_submit_btn").performClick()

        waitForCondition {
            runBlocking {
                vm.repository.getBuyerReturns().data.orEmpty().any {
                    it.orderId == orderId && it.comment == "Hinge feels loose" && it.status == "REQUESTED"
                }
            }
        }
    }

    @Test
    fun returnStatus_buyerMarksApprovedReturnSentBack() {
        val db = SuqiDatabase.getInstance(ApplicationProvider.getApplicationContext<Application>())
        val phone = "+251 911 600 222"
        val vm = newBuyerViewModel(phone = phone, name = "Return Status Buyer")
        val buyerId = vm.currentUser.value!!.id
        val retId = "ret_ui_stat_${System.currentTimeMillis()}"
        runBlocking {
            db.returnRequestDao().insert(
                ReturnRequestEntity(
                    id = retId,
                    orderId = "ord_4",
                    orderReference = "SUQ-2026-001235",
                    shopId = "shop_techhub",
                    shopName = "Tech Hub",
                    customerUserId = buyerId,
                    customerName = "Return Status Buyer",
                    itemsJson = "[{\"productId\":\"prod_airpods_pro\",\"productName\":\"Wireless Earbuds Pro\",\"productImageUrl\":\"\",\"quantity\":1,\"unitPrice\":2800.0}]",
                    refundAmount = 2800.0,
                    reason = "Damaged or defective item",
                    comment = "Left bud silent",
                    resolution = "REFUND",
                    status = "APPROVED",
                    refundMethod = "Telebirr",
                    createdAt = "Just now",
                    updatedAt = "Just now"
                )
            )
        }

        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) { ReturnStatusScreen(returnId = retId, viewModel = vm) }
        }

        waitForTag("return_status_badge")
        waitForTag("return_mark_sent_back_btn")
        composeRule.onNodeWithTag("return_mark_sent_back_btn").performClick()

        waitForCondition {
            runBlocking { vm.repository.getReturnDetails(retId).data?.status == "SENT_BACK" }
        }
        composeRule.onNodeWithText("SENT BACK").assertIsDisplayed()
    }

    @Test
    fun sellerReturns_sellerApprovesRequestedReturn() {
        val vm = newSellerViewModel()
        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) { SellerReturnsScreen(viewModel = vm) }
        }

        waitForTag("seller_return_card_ret_seed_1")
        composeRule.onNodeWithTag("seller_return_card_ret_seed_1").performClick()

        waitForTag("seller_return_approve_btn")
        composeRule.onNodeWithTag("seller_return_approve_btn").performClick()

        waitForCondition {
            runBlocking {
                vm.repository.getSellerReturns().data.orEmpty().firstOrNull { it.id == "ret_seed_1" }?.status == "APPROVED"
            }
        }
    }

    @Test
    fun sellerEarnings_requestsPayoutAndPersistsRequest() {
        val db = SuqiDatabase.getInstance(ApplicationProvider.getApplicationContext<Application>())
        val vm = newSellerViewModel()
        runBlocking {
            db.orderDao().insertOrder(makeDeliveredOrder("ord_ui_earn_${System.currentTimeMillis()}", "user_dawit", "Dawit", "+251 911 554 433", amount = 8000.0))
        }

        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) { SellerEarningsScreen(viewModel = vm) }
        }

        waitForTag("seller_available_balance")
        waitForTag("seller_request_payout_btn")
        composeRule.onNodeWithTag("seller_request_payout_btn").assertIsEnabled().performClick()

        waitForTag("payout_amount_input")
        composeRule.onNodeWithTag("payout_amount_input").performTextReplacement("750")
        composeRule.onNodeWithTag("confirm_payout_request_btn").performClick()

        waitForCondition {
            runBlocking {
                vm.repository.getSellerEarningsSummary().data?.payoutRequests.orEmpty().any {
                    it.amount == 750.0 && it.status == "PENDING"
                }
            }
        }
    }

    @Test
    fun adminFinance_approvesAndMarksPayoutPaidWithRef() {
        val vm = newAdminViewModel()
        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) { AdminFinanceScreen(viewModel = vm) }
        }

        waitForTag("admin_payout_payout_2")
        waitForTag("approve_payout_payout_2")
        composeRule.onNodeWithTag("approve_payout_payout_2").performClick()

        waitForTag("mark_paid_payout_payout_2")
        composeRule.onNodeWithTag("mark_paid_payout_payout_2").performClick()

        waitForTag("payout_ref_input")
        composeRule.onNodeWithTag("payout_ref_input").performTextInput("FT-UI-998877")
        composeRule.onNodeWithTag("payout_mark_paid_confirm").performClick()

        waitForCondition {
            runBlocking {
                vm.repository.getAdminFinanceOverview().data?.payoutRequests.orEmpty()
                    .firstOrNull { it.id == "payout_2" }?.let { it.status == "PAID" && it.referenceCode == "FT-UI-998877" } == true
            }
        }
    }

    @Test
    fun sellerSubscription_switchesCycleAndSubmitsUpgrade() {
        val vm = newSellerViewModel()
        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) { SellerSubscriptionV2Screen(viewModel = vm) }
        }

        waitForTag("sub_cycle_YEARLY")
        composeRule.onNodeWithTag("sub_cycle_YEARLY").performClick()

        scrollListToTag("upgrade_plan_btn_plan_pro")
        composeRule.onNodeWithTag("upgrade_plan_btn_plan_pro").performClick()

        waitForTag("upgrade_tx_ref_input")
        composeRule.onNodeWithTag("upgrade_tx_ref_input").performTextInput("TXN-UI-778899")
        composeRule.onNodeWithTag("submit_upgrade_btn").performScrollTo().performClick()

        waitForCondition {
            runBlocking {
                vm.repository.getSubscriptionUpgrades().data.orEmpty().any {
                    it.transactionReference == "TXN-UI-778899" && it.billingCycle == "YEARLY" && it.status == "PENDING"
                }
            }
        }
    }

    @Test
    fun buyerDeals_filtersByCategoryAndAddsToCart() {
        val vm = newBuyerViewModel(phone = "+251 911 600 333", name = "Deals Buyer")
        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) { BuyerDealsScreen(viewModel = vm) }
        }

        waitForTag("deals_countdown")
        waitForTag("deal_card_fs_1")
        composeRule.onNodeWithTag("filter_chip_cat_electronics").performClick()

        waitForCondition {
            composeRule.onAllNodesWithTag("deal_card_fs_2").fetchSemanticsNodes().isEmpty()
        }
        composeRule.onAllNodesWithTag("deal_card_fs_2").assertCountEquals(0)

        waitForTag("deal_add_cart_fs_1")
        composeRule.onNodeWithTag("deal_add_cart_fs_1").performClick()

        waitForCondition {
            runBlocking {
                vm.repository.getCartItems().data.orEmpty().any { it.productId == "prod_airpods_pro" }
            }
        }
    }

    @Test
    fun sellerAnalytics_switchesDateRangeAndReloadsMetrics() {
        val vm = newSellerViewModel()
        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) { SellerAnalyticsScreen(viewModel = vm) }
        }

        waitForTag("analytics_range_7")
        waitForCondition { vm.cachedSellerAnalytics[7] != null }

        composeRule.onNodeWithTag("analytics_range_30").performClick()
        waitForCondition { vm.cachedSellerAnalytics[30]?.rangeDays == 30 }
        assertEquals(30, vm.cachedSellerAnalytics[30]?.rangeDays)
    }

    @Test
    fun reviewsV2_togglesHelpfulAndSubmitsSellerReply() {
        val vm = newSellerViewModel()
        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                ReviewsV2Screen(
                    targetId = "prod_laptop_hp",
                    targetName = "HP Laptop 15.6\"",
                    targetType = "PRODUCT",
                    viewModel = vm
                )
            }
        }

        waitForTag("review_helpful_rev_2")
        composeRule.onNodeWithTag("review_helpful_rev_2").performClick()
        waitForCondition {
            runBlocking {
                vm.repository.getReviews("prod_laptop_hp").data.orEmpty().firstOrNull { it.id == "rev_2" }?.helpfulCount == 1
            }
        }

        waitForTag("review_reply_btn_rev_2")
        composeRule.onNodeWithTag("review_reply_btn_rev_2").performClick()
        waitForTag("review_reply_input")
        composeRule.onNodeWithTag("review_reply_input").performTextInput("Glad you enjoyed the keyboard!")
        composeRule.onNodeWithTag("review_reply_submit_btn").performClick()

        waitForCondition {
            runBlocking {
                vm.repository.getReviews("prod_laptop_hp").data.orEmpty()
                    .firstOrNull { it.id == "rev_2" }?.sellerReply == "Glad you enjoyed the keyboard!"
            }
        }
    }

    @Test
    fun adminModerationV2_hidesFlaggedReviewAndUpdatesTabs() {
        val vm = newAdminViewModel()
        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) { AdminModerationV2Screen(viewModel = vm) }
        }

        waitForTag("mod_card_flag_1")
        waitForTag("mod_note_input")
        composeRule.onNodeWithTag("mod_note_input").performTextInput("Contains phone contact")
        composeRule.onNodeWithTag("mod_hide_flag_1").performClick()

        waitForCondition {
            runBlocking {
                vm.repository.getFlaggedReviews().data.orEmpty().firstOrNull { it.id == "flag_1" }?.status == "HIDDEN"
            }
        }

        composeRule.onNodeWithTag("mod_tab_HIDDEN").performClick()
        waitForTag("mod_restore_flag_1")
    }

    @Test
    fun adminPlansV2_togglesPilotModeAndPersists() {
        val vm = newAdminViewModel()
        val initialPilot = runBlocking { vm.repository.getAdminSettings().data!!.isPilotMode }
        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) { AdminPlansV2Screen(viewModel = vm) }
        }

        waitForTag("admin_pilot_switch")
        composeRule.onNodeWithTag("admin_pilot_switch").performClick()

        waitForCondition {
            runBlocking { vm.repository.getAdminSettings().data?.isPilotMode == !initialPilot }
        }
        assertFalse(runBlocking { vm.repository.getAdminSettings().data!!.isPilotMode } == initialPilot)
    }

    @Test
    fun adminSupportInbox_repliesAndResolvesTicket() {
        val vm = newAdminViewModel()
        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) { AdminSupportInboxScreen(viewModel = vm) }
        }

        waitForTag("admin_ticket_card_tkt_1")
        composeRule.onNodeWithTag("admin_ticket_card_tkt_1").performClick()

        waitForTag("admin_ticket_reply_input")
        composeRule.onNodeWithTag("admin_ticket_reply_input").performTextInput("Checked Telebirr batch log")
        composeRule.onNodeWithTag("admin_ticket_send_btn").performClick()

        waitForCondition {
            runBlocking {
                vm.repository.getTicketMessages("tkt_1").data.orEmpty().any { it.message == "Checked Telebirr batch log" }
            }
        }

        composeRule.onNodeWithTag("ticket_resolution_input").performScrollTo().performTextInput("Confirmed with Tech Hub")
        composeRule.onNodeWithTag("admin_ticket_resolve_btn").performScrollTo().performClick()

        waitForCondition {
            runBlocking {
                vm.repository.getSupportTickets().data.orEmpty().firstOrNull { it.id == "tkt_1" }?.status == "RESOLVED"
            }
        }
    }

    @Test
    fun adminBroadcast_sendsBroadcastAndAppearsInHistory() {
        val vm = newAdminViewModel()
        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) { AdminBroadcastScreen(viewModel = vm) }
        }

        waitForTag("bc_audience_BUYERS")
        composeRule.onNodeWithTag("bc_audience_BUYERS").performClick()
        composeRule.onNodeWithTag("bc_title_input").performTextInput("UI Flash Alert")
        composeRule.onNodeWithTag("bc_body_input").performTextInput("Save 20% until midnight")

        scrollListToTag("bc_send_btn")
        composeRule.onNodeWithTag("bc_send_btn").performClick()

        waitForCondition {
            runBlocking {
                vm.repository.getAdminBroadcasts().data.orEmpty().any {
                    it.title == "UI Flash Alert" && it.audience == "BUYERS" && !it.isTemplate
                }
            }
        }

        composeRule.onNodeWithTag("broadcast_tab_HISTORY").performClick()
        waitForCondition {
            runBlocking {
                val id = vm.repository.getAdminBroadcasts().data.orEmpty().firstOrNull { it.title == "UI Flash Alert" }?.id
                id != null && composeRule.onAllNodesWithTag("bc_history_$id").fetchSemanticsNodes().isNotEmpty()
            }
        }
    }

    @Test
    fun promotionsV2_createsPromoCodeViaWizard() {
        val vm = newSellerViewModel()
        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) { PromotionsV2Screen(viewModel = vm) }
        }

        waitForTag("promotions_create_btn")
        composeRule.onNodeWithTag("promotions_create_btn").performClick()

        waitForTag("promo_wizard_code_input")
        composeRule.onNodeWithTag("promo_wizard_code_input").performTextInput("UIWIN20")
        composeRule.onNodeWithTag("promo_wizard_next_btn").performClick()

        waitForTag("promo_wizard_next_btn")
        composeRule.onNodeWithTag("promo_wizard_next_btn").performClick()

        waitForTag("promo_wizard_submit_btn")
        composeRule.onNodeWithTag("promo_wizard_submit_btn").performClick()

        waitForCondition {
            runBlocking {
                vm.repository.getShopPromotions("shop_techhub", includeInactive = true).data.orEmpty().any {
                    it.code == "UIWIN20" && it.isActive
                }
            }
        }
        val created = runBlocking { vm.repository.getShopPromotions("shop_techhub", includeInactive = true).data!!.first { it.code == "UIWIN20" } }
        assertNotNull(created)
        assertTrue(created.isActive)
    }
}
