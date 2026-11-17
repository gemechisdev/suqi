package com.example

import android.app.Application
import android.os.Looper
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.printToString
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.core.app.ApplicationProvider
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
class NavigationBackstackTest {

    @get:Rule
    val composeRule = createComposeRule()

    private class TestBackOwner : OnBackPressedDispatcherOwner, LifecycleOwner {
        private val registry = LifecycleRegistry(this).apply {
            currentState = Lifecycle.State.RESUMED
        }
        override val lifecycle: Lifecycle get() = registry
        override val onBackPressedDispatcher = OnBackPressedDispatcher {}
    }

    private fun newViewModel(): SuqiViewModel =
        SuqiViewModel(ApplicationProvider.getApplicationContext<Application>())

    private fun waitForTag(tag: String) {
        try {
            composeRule.waitUntil(timeoutMillis = 15_000) {
                Thread.sleep(20)
                val mainLooper = shadowOf(Looper.getMainLooper())
                mainLooper.idle()
                mainLooper.idleFor(16, java.util.concurrent.TimeUnit.MILLISECONDS)
                composeRule.mainClock.advanceTimeByFrame()
                mainLooper.idle()
                composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
            }
        } catch (t: Throwable) {
            System.err.println("WAIT_FOR_TAG_TIMEOUT[$tag]:\n" + composeRule.onRoot().printToString())
            throw t
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

    // ---------------------------------------------------------
    // ViewModel Backstack Unit Tests
    // ---------------------------------------------------------

    @Test
    fun navigate_deduplicatesConsecutiveIdenticalDestinations() {
        val vm = newViewModel()
        val initialSize = vm.navBackStack.value.size
        vm.navigate(ScreenDestination.ProductDetails("prod_laptop_hp"))
        vm.navigate(ScreenDestination.ProductDetails("prod_laptop_hp"))

        assertEquals(initialSize + 1, vm.navBackStack.value.size)
        assertEquals(ScreenDestination.ProductDetails("prod_laptop_hp"), vm.screen.value)
    }

    @Test
    fun switchBuyerTab_preservesHomeRootEntryIdAndPopsToHome() {
        val vm = newViewModel()
        val initialHomeId = vm.navBackStack.value.first().id

        vm.switchBuyerTab(ScreenDestination.Categories)
        assertEquals(2, vm.navBackStack.value.size)
        assertEquals(initialHomeId, vm.navBackStack.value.first().id)
        assertEquals(ScreenDestination.Categories, vm.screen.value)

        vm.switchBuyerTab(ScreenDestination.Search)
        assertEquals(2, vm.navBackStack.value.size)
        assertEquals(initialHomeId, vm.navBackStack.value.first().id)
        assertEquals(ScreenDestination.Search, vm.screen.value)

        assertTrue(vm.goBack())
        assertEquals(ScreenDestination.Home, vm.screen.value)
        assertEquals(initialHomeId, vm.currentNavEntry.value.id)
    }

    @Test
    fun switchSellerTab_preservesAccountPrefixAndSellerStudioRoot() {
        val vm = newViewModel()
        vm.switchBuyerTab(ScreenDestination.Account)
        vm.switchSellerTab(ScreenDestination.SellerStudio)
        val studioId = vm.currentNavEntry.value.id

        vm.switchSellerTab(ScreenDestination.SellerOrders)
        vm.switchSellerTab(ScreenDestination.ProductManagement)
        vm.navigate(ScreenDestination.AddEditProduct("prod_laptop_hp"))

        assertEquals(ScreenDestination.AddEditProduct("prod_laptop_hp"), vm.screen.value)
        assertTrue(vm.goBack())
        assertEquals(ScreenDestination.ProductManagement, vm.screen.value)

        // Back from any Seller tab returns to SellerStudio, keeping Buyer Account underneath
        assertTrue(vm.goBack())
        assertEquals(ScreenDestination.SellerStudio, vm.screen.value)
        assertEquals(studioId, vm.currentNavEntry.value.id)

        // Back from SellerStudio returns to Buyer Account, then Home
        assertTrue(vm.goBack())
        assertEquals(ScreenDestination.Account, vm.screen.value)
        assertTrue(vm.goBack())
        assertEquals(ScreenDestination.Home, vm.screen.value)
        assertFalse(vm.goBack())
    }

    @Test
    fun completeAdminSignIn_andLockAdminSession_preserveAccountAndAvoidAppExit() {
        val vm = newViewModel()
        vm.switchBuyerTab(ScreenDestination.Account)
        vm.navigate(ScreenDestination.AdminSignIn)
        vm.completeAdminSignIn()

        assertEquals(
            listOf(ScreenDestination.Home, ScreenDestination.Account, ScreenDestination.AdminConsole),
            vm.navBackStack.value.map { it.destination }
        )

        vm.navigate(ScreenDestination.AdminBanners)
        vm.lockAdminSession()

        assertEquals("CUSTOMER", vm.activeRole.value)
        assertEquals(
            listOf(ScreenDestination.Home, ScreenDestination.Account, ScreenDestination.AdminSignIn),
            vm.navBackStack.value.map { it.destination }
        )

        // Backing out from locked AdminSignIn returns to Account (not AdminConsole, not exit)
        assertTrue(vm.goBack())
        assertEquals(ScreenDestination.Account, vm.screen.value)
    }

    @Test
    fun completeAuth_popsToCallerOrRootHome() {
        val vm = newViewModel()
        vm.navigate(ScreenDestination.ProductDetails("prod_laptop_hp"))
        vm.navigate(ScreenDestination.Auth)
        vm.completeAuth()
        assertEquals(ScreenDestination.ProductDetails("prod_laptop_hp"), vm.screen.value)

        vm.popToRoot(ScreenDestination.Auth)
        vm.completeAuth()
        assertEquals(ScreenDestination.Home, vm.screen.value)
    }

    @Test
    fun navigateAfterOrderSubmitted_stripsCheckoutStepsBeforeOrderSubmitted() {
        val vm = newViewModel()
        vm.navigate(ScreenDestination.Cart)
        vm.navigate(ScreenDestination.CheckoutDelivery("shop_techhub"))
        vm.navigate(ScreenDestination.CheckoutPayment("shop_techhub"))
        vm.navigateAfterOrderSubmitted(ScreenDestination.OrderSubmitted("SQ-999", "ord_999"))

        assertEquals(
            listOf(
                ScreenDestination.Home,
                ScreenDestination.Cart,
                ScreenDestination.OrderSubmitted("SQ-999", "ord_999")
            ),
            vm.navBackStack.value.map { it.destination }
        )
        assertTrue(vm.goBack())
        assertEquals(ScreenDestination.Cart, vm.screen.value)
    }

    @Test
    fun disposedNavIds_tracksPoppedEntriesAndClearsOnAcknowledge() {
        val vm = newViewModel()
        vm.navigate(ScreenDestination.Search)
        val searchEntryId = vm.currentNavEntry.value.id
        vm.goBack()

        assertTrue(searchEntryId in vm.disposedNavIds.value)
        vm.acknowledgeDisposedNavIds(setOf(searchEntryId))
        assertFalse(searchEntryId in vm.disposedNavIds.value)
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
    fun refreshSession_clearsUserCachesOnlyWhenAccountChanges() {
        val vm = newViewModel()
        val signedInUser = runBlocking {
            vm.repository.loginWithPhoneOtp("+251 912 345 678", "123456").data!!
        }
        vm.refreshSession()
        awaitMainCondition { vm.currentUser.value?.id == signedInUser.id }

        val sampleOrder = com.example.data.model.OrderDto(
            id = "ord_test",
            orderReference = "SQ-100",
            customerUserId = signedInUser.id,
            customerName = "Test",
            customerPhone = "+251 912 345 678",
            deliveryAddress = "Bole",
            shopId = "shop_techhub",
            shopName = "TechHub",
            items = emptyList(),
            subtotal = 500.0,
            deliveryFee = 50.0,
            discountAmount = 0.0,
            totalAmount = 550.0,
            deliveryOptionTitle = "Standard",
            deliveryOptionType = "STANDARD",
            deliveryEstimatedDays = "1-3 days",
            paymentMethodTitle = "Telebirr",
            paymentMethodType = "TELEBIRR",
            paymentMethodAccount = "0912",
            transactionReference = "TXN-1",
            paymentStatus = "VERIFIED",
            orderStatus = "Confirmed",
            createdAt = "Now"
        )
        val sampleAddr = com.example.data.model.SavedAddressDto(
            id = "addr_1",
            fullName = "Test",
            phone = "+251 912 345 678",
            region = "Addis Ababa",
            city = "Addis Ababa",
            subCity = "Bole",
            streetOrLandmark = "Main St",
            isDefault = true
        )
        val sampleChat = com.example.data.model.ChatMessageDto(
            id = "chat_1",
            conversationId = "conv_1",
            shopId = "shop_techhub",
            shopName = "TechHub",
            senderType = "CUSTOMER",
            message = "Hi",
            timestamp = "Now"
        )
        vm.cachedBuyerOrders = listOf(sampleOrder)
        vm.cachedSavedAddresses = listOf(sampleAddr)
        vm.cachedBuyerConversations = listOf(sampleChat)
        vm.cachedSellerDashboard = mapOf("totalSales" to 1200.0)
        vm.cachedAdminAllOrders = listOf(sampleOrder)

        // Refreshing for the same signed-in user preserves caches
        vm.refreshSession()
        shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(30)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(1, vm.cachedBuyerOrders.size)
        assertEquals(1, vm.cachedSavedAddresses.size)

        // Switching to a different account (guest) clears all caches
        val guestUser = runBlocking {
            vm.repository.loginAsGuest().data!!
        }
        vm.refreshSession()
        awaitMainCondition { vm.currentUser.value?.id == guestUser.id }
        assertTrue(vm.cachedBuyerOrders.isEmpty())
        assertTrue(vm.cachedSavedAddresses.isEmpty())
        assertTrue(vm.cachedBuyerConversations.isEmpty())
        assertTrue(vm.cachedSellerDashboard.isEmpty())
        assertTrue(vm.cachedAdminAllOrders.isEmpty())
    }

    @Test
    fun signOut_clearsCachesResetsNavStackAndDisposesOldEntries() {
        val vm = newViewModel()
        val signedInUser = runBlocking {
            vm.repository.loginWithPhoneOtp("+251 912 345 678", "123456").data!!
        }
        vm.refreshSession()
        awaitMainCondition { vm.currentUser.value?.id == signedInUser.id }

        val initialHomeId = vm.navBackStack.value.first().id
        vm.switchBuyerTab(ScreenDestination.Account)
        val accountId = vm.currentNavEntry.value.id
        vm.navigate(ScreenDestination.MyOrders)
        val ordersId = vm.currentNavEntry.value.id

        val sampleOrder = com.example.data.model.OrderDto(
            id = "ord_test",
            orderReference = "SQ-200",
            customerUserId = signedInUser.id,
            customerName = "Test",
            customerPhone = "+251 912 345 678",
            deliveryAddress = "Bole",
            shopId = "shop_techhub",
            shopName = "TechHub",
            items = emptyList(),
            subtotal = 500.0,
            deliveryFee = 50.0,
            discountAmount = 0.0,
            totalAmount = 550.0,
            deliveryOptionTitle = "Standard",
            deliveryOptionType = "STANDARD",
            deliveryEstimatedDays = "1-3 days",
            paymentMethodTitle = "Telebirr",
            paymentMethodType = "TELEBIRR",
            paymentMethodAccount = "0912",
            transactionReference = "TXN-2",
            paymentStatus = "VERIFIED",
            orderStatus = "Confirmed",
            createdAt = "Now"
        )
        vm.cachedBuyerOrders = listOf(sampleOrder)
        vm.cachedAdminAllOrders = listOf(sampleOrder)
        vm.cachedSellerDashboard = mapOf("totalSales" to 900.0)

        runBlocking {
            vm.signOut(ScreenDestination.Home)
        }
        awaitMainCondition { vm.currentUser.value?.role == "GUEST" }

        assertTrue(vm.cachedBuyerOrders.isEmpty())
        assertTrue(vm.cachedAdminAllOrders.isEmpty())
        assertTrue(vm.cachedSellerDashboard.isEmpty())
        assertEquals(listOf(ScreenDestination.Home), vm.navBackStack.value.map { it.destination })
        assertFalse(vm.navBackStack.value.first().id == initialHomeId)
        assertTrue(initialHomeId in vm.disposedNavIds.value)
        assertTrue(accountId in vm.disposedNavIds.value)
        assertTrue(ordersId in vm.disposedNavIds.value)
        assertEquals("GUEST", vm.activeRole.value)
    }

    // ---------------------------------------------------------
    // End-to-End Compose + Robolectric Back & State Preservation Tests
    // ---------------------------------------------------------

    @Test
    fun homeScrollAndBack_fromProductDetailsRestoresExactHomePosition() {
        val vm = newViewModel()
        val backOwner = TestBackOwner()
        var finished = false

        composeRule.setContent {
            CompositionLocalProvider(LocalOnBackPressedDispatcherOwner provides backOwner) {
                MyApplicationTheme(darkTheme = false) {
                    SuqiAppRoot(viewModel = vm, onFinish = { finished = true })
                }
            }
        }

        waitForTag("home_location")
        waitForCondition { vm.cachedHomeProducts.isNotEmpty() }

        // Scroll Home LazyColumn down so the top location header scrolls off-screen
        composeRule.onNode(
            hasScrollToIndexAction() and androidx.compose.ui.test.SemanticsMatcher.keyIsDefined(
                androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange
            )
        ).performScrollToIndex(6)
        composeRule.waitForIdle()
        waitForCondition {
            composeRule.onAllNodesWithTag("home_location").fetchSemanticsNodes().isEmpty()
        }

        // Open a product details page from the scrolled viewport
        val homeProductMatcher = hasTestTag("product_card_prod_laptop_hp")
            .or(hasTestTag("product_card_prod_living_sofa"))
            .or(hasTestTag("product_card_prod_backpack"))
            .or(hasTestTag("product_card_prod_tshirt_mens"))
        waitForCondition {
            composeRule.onAllNodes(homeProductMatcher).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodes(homeProductMatcher)
            .onFirst()
            .performClick()

        waitForTag("product_back_btn")
        assertTrue(vm.screen.value is ScreenDestination.ProductDetails)

        // Trigger hardware/system Back via dispatcher
        composeRule.runOnIdle {
            backOwner.onBackPressedDispatcher.onBackPressed()
        }
        waitForCondition { vm.screen.value == ScreenDestination.Home }
        composeRule.waitForIdle()

        // Home is restored without calling onFinish and remains at the scrolled offset (home_location still scrolled off top)
        assertFalse(finished)
        assertEquals(ScreenDestination.Home, vm.screen.value)
        assertTrue(composeRule.onAllNodesWithTag("home_location").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun searchQueryAndResultsTab_preservedAfterForwardNavigationAndHeaderBack() {
        val vm = newViewModel()
        val backOwner = TestBackOwner()
        var finished = false

        composeRule.setContent {
            CompositionLocalProvider(LocalOnBackPressedDispatcherOwner provides backOwner) {
                MyApplicationTheme(darkTheme = false) {
                    SuqiAppRoot(viewModel = vm, onFinish = { finished = true })
                }
            }
        }

        waitForTag("nav_search")
        composeRule.onNodeWithTag("nav_search").performClick()
        waitForTag("search_input_field")

        composeRule.onNodeWithTag("search_input_field").performTextInput("Laptop")
        waitForTag("search_filter_btn")
        waitForTag("product_card_prod_laptop_hp")

        composeRule.onAllNodesWithTag("product_card_prod_laptop_hp").onFirst().performClick()
        waitForTag("product_back_btn")

        composeRule.onNodeWithTag("product_back_btn").performClick()
        waitForCondition { vm.screen.value == ScreenDestination.Search }
        waitForTag("search_filter_btn")

        assertFalse(finished)
        composeRule.onNodeWithText("Laptop").assertIsDisplayed()
        composeRule.onAllNodesWithTag("product_card_prod_laptop_hp").onFirst().assertIsDisplayed()
    }

    @Test
    fun sellerStudio_headerAndSystemBack_followProfessionalHierarchy() {
        val vm = newViewModel()
        val sellerUser = runBlocking {
            vm.repository.loginWithPhoneOtp("+251 912 345 678", "123456").data!!
        }
        vm.refreshSession()
        awaitMainCondition { vm.currentUser.value?.id == sellerUser.id }
        vm.switchBuyerTab(ScreenDestination.Account)
        vm.setActiveRole("SELLER")
        vm.switchSellerTab(ScreenDestination.SellerStudio)
        shadowOf(Looper.getMainLooper()).idle()

        val backOwner = TestBackOwner()
        var finished = false

        composeRule.setContent {
            CompositionLocalProvider(LocalOnBackPressedDispatcherOwner provides backOwner) {
                MyApplicationTheme(darkTheme = false) {
                    SuqiAppRoot(viewModel = vm, onFinish = { finished = true })
                }
            }
        }

        waitForTag("seller_nav_seller_orders")
        composeRule.onNodeWithTag("seller_nav_seller_orders").performClick()
        waitForCondition { vm.screen.value == ScreenDestination.SellerOrders }
        waitForTag("seller_order_card_ord_1")

        // Drill into an order detail
        composeRule.onNodeWithTag("seller_order_card_ord_1").performClick()
        waitForCondition { vm.screen.value is ScreenDestination.SellerOrderDetail }
        waitForTag("header_back_button")

        // System Back pops SellerOrderDetail -> SellerOrders
        composeRule.runOnIdle {
            backOwner.onBackPressedDispatcher.onBackPressed()
        }
        waitForCondition { vm.screen.value == ScreenDestination.SellerOrders }
        waitForTag("header_back_button")

        // Header Back from SellerOrders goes to SellerStudio (NOT dumped to Buyer Home/Account)
        composeRule.onNodeWithTag("header_back_button").performClick()
        waitForCondition { vm.screen.value == ScreenDestination.SellerStudio }
        waitForTag("header_back_button")

        // Header Back from SellerStudio returns to Buyer Account without finishing app
        composeRule.onNodeWithTag("header_back_button").performClick()
        waitForCondition { vm.screen.value == ScreenDestination.Account }
        assertFalse(finished)
    }

    @Test
    fun adminConsole_preservesActiveTabAndReturnsToAccountInsteadOfExiting() {
        val vm = newViewModel()
        val adminUser = runBlocking {
            vm.repository.adminSignIn("admin@suqi.et", "123456", "OWNER").data!!
        }
        vm.refreshSession()
        awaitMainCondition { vm.currentUser.value?.id == adminUser.id }
        vm.switchBuyerTab(ScreenDestination.Account)
        vm.completeAdminSignIn()

        val backOwner = TestBackOwner()
        var finished = false

        composeRule.setContent {
            CompositionLocalProvider(LocalOnBackPressedDispatcherOwner provides backOwner) {
                MyApplicationTheme(darkTheme = false) {
                    SuqiAppRoot(viewModel = vm, onFinish = { finished = true })
                }
            }
        }

        waitForTag("admin_tab_manage")
        composeRule.onNodeWithTag("admin_tab_manage").performClick()
        waitForTag("manage_banners")

        composeRule.onNodeWithTag("manage_banners").performClick()
        waitForCondition { vm.screen.value == ScreenDestination.AdminBanners }
        waitForTag("header_back_button")

        // Back from AdminBanners returns to AdminConsole with "manage" tab still active
        composeRule.onNodeWithTag("header_back_button").performClick()
        waitForCondition { vm.screen.value == ScreenDestination.AdminConsole }
        waitForTag("manage_banners")
        composeRule.onNodeWithTag("manage_banners").assertIsDisplayed()

        // Back from AdminConsole returns to Account instead of calling onFinish
        composeRule.runOnIdle {
            backOwner.onBackPressedDispatcher.onBackPressed()
        }
        waitForCondition { vm.screen.value == ScreenDestination.Account }
        assertFalse(finished)
    }
}
