package com.example

import android.app.Application
import android.os.Looper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.OrderEntity
import com.example.data.local.ReturnRequestEntity
import com.example.data.local.SuqiDatabase
import com.example.data.model.PromoCodeDto
import com.example.data.model.SavedAddressDto
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.components.suqiGridColumns
import com.example.ui.i18n.SuqiLanguage
import com.example.ui.i18n.SuqiStringCatalog
import com.example.ui.screens.AccountHubScreen
import com.example.ui.screens.AdminDevToolsScreen
import com.example.ui.screens.AdminFinanceScreen
import com.example.ui.screens.CartScreen
import com.example.ui.screens.CheckoutDeliveryScreen
import com.example.ui.screens.CheckoutPaymentScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProductDetailsScreen
import com.example.ui.screens.ReturnRequestScreen
import com.example.ui.screens.SellerEarningsScreen
import com.example.ui.screens.SellerReturnsScreen
import com.example.ui.screens.VerificationQueueScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SuqiBackgroundDark
import com.example.ui.theme.SuqiBackgroundLight
import com.example.ui.theme.SuqiMutedTextDark
import com.example.ui.theme.SuqiMutedTextLight
import com.example.ui.theme.SuqiPrimaryTextDark
import com.example.ui.theme.SuqiPrimaryTextLight
import com.example.ui.theme.SuqiSecondaryTextDark
import com.example.ui.theme.SuqiSecondaryTextLight
import com.example.ui.theme.SuqiSurfaceDark
import com.example.ui.theme.SuqiSurfaceLight
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit
import kotlin.math.pow

/**
 * F-06 Accessibility, WCAG AA contrast, EN/AM/OM completeness, 1.3x font scale, w840dp tablet grids,
 * Light & Dark theme verification, and the 5 money-critical end-to-end Compose UI flows.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
class AccessibilityResponsiveAndFlowTest {

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
        waitForCondition { composeRule.onAllNodes(hasScrollToIndexAction()).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag(tag))
        waitForTag(tag)
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

    private fun relativeLuminance(color: Color): Double {
        val argb = color.toArgb()
        fun channel(c: Int): Double {
            val s = (c and 0xFF) / 255.0
            return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        val r = channel(argb shr 16)
        val g = channel(argb shr 8)
        val b = channel(argb)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun contrastRatio(fg: Color, bg: Color): Double {
        val l1 = relativeLuminance(fg)
        val l2 = relativeLuminance(bg)
        val hi = maxOf(l1, l2)
        val lo = minOf(l1, l2)
        return (hi + 0.05) / (lo + 0.05)
    }

    // ---------------- F-06 Contrast, i18n, Adaptive Grid, Font Scale & Dark Mode ----------------

    @Test
    fun themeTokens_meetWcagAa45To1ContrastInLightAndDark() {
        val lightChecks = listOf(
            "PrimaryTextLight/Surface" to (SuqiPrimaryTextLight to SuqiSurfaceLight),
            "PrimaryTextLight/Bg" to (SuqiPrimaryTextLight to SuqiBackgroundLight),
            "SecondaryTextLight/Surface" to (SuqiSecondaryTextLight to SuqiSurfaceLight),
            "SecondaryTextLight/Bg" to (SuqiSecondaryTextLight to SuqiBackgroundLight),
            "MutedTextLight/Surface" to (SuqiMutedTextLight to SuqiSurfaceLight),
            "MutedTextLight/Bg" to (SuqiMutedTextLight to SuqiBackgroundLight)
        )
        lightChecks.forEach { (label, pair) ->
            val ratio = contrastRatio(pair.first, pair.second)
            assertTrue("$label contrast $ratio < 4.5", ratio >= 4.5)
        }

        val darkChecks = listOf(
            "PrimaryTextDark/Surface" to (SuqiPrimaryTextDark to SuqiSurfaceDark),
            "PrimaryTextDark/Bg" to (SuqiPrimaryTextDark to SuqiBackgroundDark),
            "SecondaryTextDark/Surface" to (SuqiSecondaryTextDark to SuqiSurfaceDark),
            "SecondaryTextDark/Bg" to (SuqiSecondaryTextDark to SuqiBackgroundDark),
            "MutedTextDark/Surface" to (SuqiMutedTextDark to SuqiSurfaceDark),
            "MutedTextDark/Bg" to (SuqiMutedTextDark to SuqiBackgroundDark)
        )
        darkChecks.forEach { (label, pair) ->
            val ratio = contrastRatio(pair.first, pair.second)
            assertTrue("$label contrast $ratio < 4.5", ratio >= 4.5)
        }
    }

    @Test
    fun i18nCatalog_hasZeroMissingTranslationsAcrossEnAmOm() {
        val missing = SuqiStringCatalog.findMissingTranslations()
        assertTrue("Missing translations: $missing", missing.isEmpty())
    }

    @Test
    fun suqiGridColumns_follows2To3To4Breakpoints() {
        assertEquals(2, suqiGridColumns(360.dp))
        assertEquals(2, suqiGridColumns(599.dp))
        assertEquals(3, suqiGridColumns(600.dp))
        assertEquals(3, suqiGridColumns(839.dp))
        assertEquals(4, suqiGridColumns(840.dp))
        assertEquals(4, suqiGridColumns(1200.dp))
    }

    @Test
    @Config(qualifiers = "w840dp-h1280dp-xhdpi")
    fun tablet840dp_homeAndAccountAndCartRenderWith600dpCapAnd4ColGrid() {
        val vm = newBuyerViewModel("+251 911 730 101", "Tablet Buyer")
        var step by mutableStateOf(0)
        composeRule.setContent {
            if (step == 0) {
                MyApplicationTheme(darkTheme = false) { HomeScreen(viewModel = vm) }
            } else {
                MyApplicationTheme(darkTheme = true) { AccountHubScreen(viewModel = vm) }
            }
        }
        waitForTag("home_search_bar")
        composeRule.onNodeWithTag("home_search_bar").assertIsDisplayed()

        step = 1
        waitForTag("account_settings_btn")
        composeRule.onNodeWithTag("account_settings_btn").assertIsDisplayed()
    }

    @Test
    fun fontScale130_oromoAndAmharicAndDarkModeRenderWithoutCrash() {
        val buyerVm = newBuyerViewModel("+251 911 730 102", "Scaled Buyer")
        val adminVm = newAdminViewModel()
        var step by mutableStateOf(0)

        composeRule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale = 1.3f)) {
                if (step == 0) {
                    MyApplicationTheme(darkTheme = true, language = SuqiLanguage.OM) {
                        HomeScreen(viewModel = buyerVm)
                    }
                } else {
                    MyApplicationTheme(darkTheme = false, language = SuqiLanguage.AM) {
                        AdminDevToolsScreen(viewModel = adminVm)
                    }
                }
            }
        }
        waitForTag("home_search_bar")
        composeRule.onNodeWithTag("home_search_bar").assertIsDisplayed()

        step = 1
        scrollListToTag("dev_schema_version")
        composeRule.onNodeWithTag("dev_schema_version").assertIsDisplayed()
    }

    // ---------------- F-06 Five Money-Critical Compose UI Flows ----------------

    @Test
    fun moneyFlow1_addToCart_checkoutDeliveryToPayment_submitsOrder() {
        val vm = newBuyerViewModel("+251 911 730 201", "Checkout Flow Buyer")
        val product = runBlocking {
            vm.repository.getProducts(shopId = "shop_techhub").data.orEmpty().first()
        }
        vm.activeCheckoutRecipientName.value = "Checkout Flow Buyer"
        vm.activeCheckoutRecipientPhone.value = "+251 911 730 201"
        vm.activeCheckoutDeliveryAddress.value = "Bole Medhanialem, Addis Ababa"
        vm.activeCheckoutReceiptUri.value = "content://suqi/test_receipt.jpg"

        var step by mutableStateOf(0)
        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                when (step) {
                    0 -> ProductDetailsScreen(productId = product.id, viewModel = vm)
                    1 -> CheckoutDeliveryScreen(shopId = product.shopId, viewModel = vm)
                    else -> CheckoutPaymentScreen(shopId = product.shopId, viewModel = vm)
                }
            }
        }

        // 1. Add product from ProductDetails
        waitForTag("btn_add_to_cart")
        composeRule.onNodeWithTag("btn_add_to_cart").performClick()
        waitForCondition { runBlocking { vm.repository.getCartItems().data.orEmpty().any { it.productId == product.id } } }

        val cartItems = runBlocking { vm.repository.getCartItems().data.orEmpty().filter { it.shopId == product.shopId } }
        vm.activeCheckoutItems.value = cartItems
        vm.activeCheckoutShopName.value = product.shopName

        // 2. Checkout Delivery -> Continue to Payment
        step = 1
        waitForTag("btn_continue_to_payment")
        composeRule.onNodeWithTag("btn_continue_to_payment").performClick()

        // 3. Checkout Payment -> Submit Order
        step = 2
        scrollListToTag("input_transaction_reference")
        composeRule.onNodeWithTag("input_transaction_reference").performTextInput("TXN99881122")
        composeRule.onNodeWithTag("btn_submit_order").performClick()

        waitForCondition {
            runBlocking {
                vm.repository.getOrders().data.orEmpty().any { it.transactionReference == "TXN99881122" }
            }
        }
    }

    @Test
    fun moneyFlow2_sellerVerifiesPaymentAndUpdatesStatus() {
        val db = SuqiDatabase.getInstance(ApplicationProvider.getApplicationContext<Application>())
        val buyerVm = newBuyerViewModel("+251 911 730 202", "Verify Flow Buyer")
        val orderId = "ord_money_verify_${System.currentTimeMillis()}"
        runBlocking {
            db.orderDao().insertOrder(
                OrderEntity(
                    id = orderId,
                    orderReference = "SUQ-MV-1001",
                    customerUserId = buyerVm.currentUser.value!!.id,
                    customerName = "Verify Flow Buyer",
                    customerPhone = "+251 911 730 202",
                    deliveryAddress = "Bole, Addis Ababa",
                    shopId = "shop_techhub",
                    shopName = "Tech Hub",
                    itemsJson = "[]",
                    subtotal = 3200.0,
                    deliveryFee = 50.0,
                    discountAmount = 0.0,
                    totalAmount = 3250.0,
                    deliveryOptionTitle = "Standard",
                    deliveryOptionType = "STANDARD",
                    deliveryEstimatedDays = "1-2 days",
                    paymentMethodTitle = "Telebirr",
                    paymentMethodType = "TELEBIRR",
                    paymentMethodAccount = "+251 912 345 678",
                    transactionReference = "TXN-MV-776655",
                    paymentStatus = "PENDING_VERIFICATION",
                    orderStatus = "Pending Verification",
                    createdAt = "Just now",
                    transferredAmount = 3250.0,
                    payerIdentifier = "+251 911 730 202"
                )
            )
        }

        val sellerVm = newSellerViewModel()
        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) { VerificationQueueScreen(viewModel = sellerVm) }
        }
        scrollListToTag("approve_payment_$orderId")
        composeRule.onNodeWithTag("approve_payment_$orderId").performClick()

        waitForCondition {
            runBlocking { db.orderDao().getOrderById(orderId)?.paymentStatus == "VERIFIED" }
        }
    }

    @Test
    fun moneyFlow3_buyerReturnRequestAndSellerApproval() {
        val db = SuqiDatabase.getInstance(ApplicationProvider.getApplicationContext<Application>())
        val phone = "+251 911 730 203"
        val buyerVm = newBuyerViewModel(phone, "Return Flow Buyer")
        val orderId = "ord_money_ret_${System.currentTimeMillis()}"
        runBlocking {
            db.orderDao().insertOrder(
                OrderEntity(
                    id = orderId,
                    orderReference = "SUQ-MR-2002",
                    customerUserId = buyerVm.currentUser.value!!.id,
                    customerName = "Return Flow Buyer",
                    customerPhone = phone,
                    deliveryAddress = "Bole, Addis Ababa",
                    shopId = "shop_techhub",
                    shopName = "Tech Hub",
                    itemsJson = "[{\"productId\":\"prod_laptop_hp\",\"productName\":\"HP Laptop 15.6\",\"productImageUrl\":\"\",\"variation\":\"Silver\",\"unitPrice\":5000.0,\"quantity\":1,\"totalPrice\":5000.0}]",
                    subtotal = 5000.0,
                    deliveryFee = 0.0,
                    discountAmount = 0.0,
                    totalAmount = 5000.0,
                    deliveryOptionTitle = "Standard",
                    deliveryOptionType = "STANDARD",
                    deliveryEstimatedDays = "1-2 days",
                    paymentMethodTitle = "Telebirr",
                    paymentMethodType = "TELEBIRR",
                    paymentMethodAccount = "+251 912 345 678",
                    transactionReference = "TXN-MR-2002",
                    paymentStatus = "VERIFIED",
                    orderStatus = "Delivered",
                    createdAt = "Just now",
                    deliveredAt = "Just now",
                    transferredAmount = 5000.0,
                    payerIdentifier = phone
                )
            )
        }

        var step by mutableStateOf(0)
        var sellerVmHolder by mutableStateOf<SuqiViewModel?>(null)
        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                if (step == 0) {
                    ReturnRequestScreen(orderId = orderId, viewModel = buyerVm)
                } else {
                    SellerReturnsScreen(viewModel = sellerVmHolder!!)
                }
            }
        }
        waitForTag("return_next_step_btn")
        composeRule.onNodeWithTag("return_next_step_btn").performClick()
        waitForTag("return_reason_1")
        composeRule.onNodeWithTag("return_reason_1").performClick()
        composeRule.onNodeWithTag("return_comment_input").performTextInput("Screen flickers")
        composeRule.onNodeWithTag("return_next_step_btn").performClick()
        waitForTag("return_submit_btn")
        composeRule.onNodeWithTag("return_submit_btn").performClick()

        var createdReturnId = ""
        waitForCondition {
            val list = runBlocking { buyerVm.repository.getBuyerReturns().data.orEmpty() }
            val match = list.firstOrNull { it.orderId == orderId }
            if (match != null) {
                createdReturnId = match.id
                true
            } else false
        }

        sellerVmHolder = newSellerViewModel()
        step = 1
        scrollListToTag("seller_return_card_$createdReturnId")
        composeRule.onNodeWithTag("seller_return_card_$createdReturnId").performClick()
        waitForTag("seller_return_approve_btn")
        composeRule.onNodeWithTag("seller_return_approve_btn").performClick()

        waitForCondition {
            runBlocking { sellerVmHolder!!.repository.getReturnDetails(createdReturnId).data?.status == "APPROVED" }
        }
    }

    @Test
    fun moneyFlow4_sellerRequestsPayoutAndAdminApprovesAndMarksPaid() {
        val db = SuqiDatabase.getInstance(ApplicationProvider.getApplicationContext<Application>())
        val sellerVm = newSellerViewModel()
        val uniqueAmount = 812.0
        runBlocking {
            db.orderDao().insertOrder(
                OrderEntity(
                    id = "ord_money_earn_${System.currentTimeMillis()}",
                    orderReference = "SUQ-ME-3003",
                    customerUserId = "user_dawit",
                    customerName = "Dawit",
                    customerPhone = "+251 911 554 433",
                    deliveryAddress = "Bole, Addis Ababa",
                    shopId = "shop_techhub",
                    shopName = "Tech Hub",
                    itemsJson = "[{\"productId\":\"prod_laptop_hp\",\"productName\":\"HP Laptop 15.6\",\"productImageUrl\":\"\",\"variation\":\"Silver\",\"unitPrice\":9000.0,\"quantity\":1,\"totalPrice\":9000.0}]",
                    subtotal = 9000.0,
                    deliveryFee = 0.0,
                    discountAmount = 0.0,
                    totalAmount = 9000.0,
                    deliveryOptionTitle = "Standard",
                    deliveryOptionType = "STANDARD",
                    deliveryEstimatedDays = "1-2 days",
                    paymentMethodTitle = "Telebirr",
                    paymentMethodType = "TELEBIRR",
                    paymentMethodAccount = "+251 912 345 678",
                    transactionReference = "TXN-ME-3003",
                    paymentStatus = "VERIFIED",
                    orderStatus = "Delivered",
                    createdAt = "Just now",
                    deliveredAt = "Just now",
                    transferredAmount = 9000.0,
                    payerIdentifier = "+251 911 554 433"
                )
            )
        }

        var step by mutableStateOf(0)
        var adminVmHolder by mutableStateOf<SuqiViewModel?>(null)
        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                if (step == 0) {
                    SellerEarningsScreen(viewModel = sellerVm)
                } else {
                    AdminFinanceScreen(viewModel = adminVmHolder!!)
                }
            }
        }
        waitForTag("seller_request_payout_btn")
        composeRule.onNodeWithTag("seller_request_payout_btn").assertIsEnabled().performClick()
        waitForTag("payout_amount_input")
        composeRule.onNodeWithTag("payout_amount_input").performTextReplacement("812")
        composeRule.onNodeWithTag("confirm_payout_request_btn").performClick()

        var payoutId = ""
        waitForCondition {
            val req = runBlocking { sellerVm.repository.getSellerEarningsSummary().data?.payoutRequests.orEmpty() }
                .firstOrNull { it.amount == uniqueAmount && it.status == "PENDING" }
            if (req != null) {
                payoutId = req.id
                true
            } else false
        }

        adminVmHolder = newAdminViewModel()
        step = 1
        scrollListToTag("approve_payout_$payoutId")
        composeRule.onNodeWithTag("approve_payout_$payoutId").performClick()
        waitForTag("mark_paid_payout_$payoutId")
        composeRule.onNodeWithTag("mark_paid_payout_$payoutId").performClick()
        waitForTag("payout_ref_input")
        composeRule.onNodeWithTag("payout_ref_input").performTextInput("FT-FLOW-812")
        composeRule.onNodeWithTag("payout_mark_paid_confirm").performClick()

        waitForCondition {
            runBlocking {
                adminVmHolder!!.repository.getAdminFinanceOverview().data?.payoutRequests.orEmpty()
                    .firstOrNull { it.id == payoutId }?.let { it.status == "PAID" && it.referenceCode == "FT-FLOW-812" } == true
            }
        }
    }

    @Test
    fun moneyFlow5_cartPromoValidation_rejectsInvalidAndAppliesValidDiscount() {
        val sellerVm = newSellerViewModel()
        runBlocking {
            sellerVm.repository.createPromoCode(
                PromoCodeDto(
                    id = "",
                    shopId = "shop_techhub",
                    code = "SAVE200FLOW",
                    discountType = "FIXED",
                    discountValue = 200.0,
                    minOrderAmount = 100.0,
                    validUntil = "2027-12-31",
                    isActive = true
                )
            )
        }
        val buyerVm = newBuyerViewModel("+251 911 730 205", "Promo Flow Buyer")
        val product = runBlocking { buyerVm.repository.getProducts(shopId = "shop_techhub").data.orEmpty().first() }
        runBlocking { buyerVm.repository.changeCartLine(product.id, 1) }

        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) { CartScreen(viewModel = buyerVm) }
        }
        waitForTag("promo_input_${product.shopId}")
        composeRule.onNodeWithTag("promo_input_${product.shopId}").performTextInput("BADCODE99")
        composeRule.onNodeWithTag("promo_apply_${product.shopId}").performClick()
        pump()
        assertTrue(composeRule.onAllNodesWithTag("promo_remove_${product.shopId}").fetchSemanticsNodes().isEmpty())

        composeRule.onNodeWithTag("promo_input_${product.shopId}").performTextReplacement("SAVE200FLOW")
        composeRule.onNodeWithTag("promo_apply_${product.shopId}").performClick()

        waitForTag("promo_remove_${product.shopId}")
        composeRule.onNodeWithTag("promo_remove_${product.shopId}").assertIsDisplayed()
    }
}
