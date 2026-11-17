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
import androidx.test.core.app.ApplicationProvider
import com.example.ui.SuqiViewModel
import com.example.ui.screens.AddEditProductScreen
import com.example.ui.screens.ChatThreadScreen
import com.example.ui.screens.DeliverySetupScreen
import com.example.ui.screens.ProductManagementScreen
import com.example.ui.screens.SellerInboxScreen
import com.example.ui.screens.SellerInventoryScreen
import com.example.ui.screens.ShopSettingsScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
class Phase3ComposeUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun newSellerViewModel(): SuqiViewModel {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = SuqiViewModel(app)
        runBlocking {
            vm.repository.loginWithPhoneOtp("+251 912 345 678", "123456")
            vm.repository.getSellerProducts()
        }
        vm.setActiveRole("SELLER")
        vm.refreshSession()
        return vm
    }

    private fun waitForTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            Thread.sleep(20)
            shadowOf(Looper.getMainLooper()).idleFor(16, java.util.concurrent.TimeUnit.MILLISECONDS)
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun sellerInventory_togglesMultiSelectAndDisplaysBulkBar() {
        val vm = newSellerViewModel()
        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                SellerInventoryScreen(viewModel = vm)
            }
        }
        waitForTag("inventory_select_toggle")

        composeRule.onNodeWithTag("inventory_select_toggle").performClick()
        waitForTag("inventory_check_prod_laptop_hp")
        composeRule.onNodeWithTag("inventory_check_prod_laptop_hp").performClick()
        waitForTag("inventory_bulk_pause")
        composeRule.onNodeWithTag("inventory_bulk_set_stock").assertIsDisplayed()
        composeRule.onNodeWithTag("inventory_bulk_pause").assertIsDisplayed()
        composeRule.onNodeWithTag("inventory_bulk_out_of_stock").assertIsDisplayed()
    }

    @Test
    fun productManagement_togglesMultiSelectAndOpensSortSheet() {
        val vm = newSellerViewModel()
        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                ProductManagementScreen(viewModel = vm)
            }
        }
        waitForTag("products_select_toggle")

        composeRule.onNodeWithTag("products_select_toggle").performClick()
        waitForTag("product_check_prod_smart_watch")
        composeRule.onNodeWithTag("product_check_prod_smart_watch").performClick()
        waitForTag("products_bulk_pause")
        composeRule.onNodeWithTag("products_bulk_pause").assertIsDisplayed()
        composeRule.onNodeWithTag("products_bulk_discount").assertIsDisplayed()

        composeRule.onNodeWithTag("products_sort_button").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun addEditProduct_displaysCompletenessChipsAndOpensBuyerPreview() {
        val vm = newSellerViewModel()
        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                AddEditProductScreen(productId = null, viewModel = vm)
            }
        }
        waitForTag("product_completeness_row")
        composeRule.onNodeWithTag("product_completeness_row").assertIsDisplayed()
        composeRule.onNodeWithTag("preview_as_buyer_btn").performClick()
        waitForTag("buyer_preview_sheet")
        composeRule.onNodeWithTag("buyer_preview_sheet").assertIsDisplayed()
    }

    @Test
    fun shopSettings_displaysLiveHeaderAndQrShareCard() {
        val vm = newSellerViewModel()
        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                ShopSettingsScreen(viewModel = vm)
            }
        }
        waitForTag("shop_live_header_preview")
        composeRule.onNodeWithTag("shop_live_header_preview").assertIsDisplayed()

        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag("shop_share_qr_card"))
        composeRule.onNodeWithTag("shop_share_qr_card").assertIsDisplayed()
    }

    @Test
    fun deliverySetup_displaysCutOffPickersAndBuyerPreviewCard() {
        val vm = newSellerViewModel()
        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                DeliverySetupScreen(viewModel = vm)
            }
        }
        waitForTag("standard_cutoff_time")
        composeRule.onNodeWithTag("standard_cutoff_time").assertIsDisplayed()

        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag("select_all_addis"))
        composeRule.onNodeWithTag("select_all_addis").performClick()

        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag("delivery_buyer_preview_card"))
        composeRule.onNodeWithTag("delivery_buyer_preview_card").assertIsDisplayed()
    }

    @Test
    fun sellerInbox_opensNotificationSettingsSheet() {
        val vm = newSellerViewModel()
        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                SellerInboxScreen(viewModel = vm)
            }
        }
        waitForTag("inbox_notif_settings_btn")
        composeRule.onNodeWithTag("inbox_notif_settings_btn").performClick()
        waitForTag("notif_switch_new_order")
        composeRule.onNodeWithTag("notif_switch_new_order").assertIsDisplayed()
        composeRule.onNodeWithTag("notif_switch_question").assertIsDisplayed()
    }

    @Test
    fun chatThread_sellerModeDisplaysQuickRepliesAndOrderAttachment() {
        val vm = newSellerViewModel()
        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                ChatThreadScreen(shopId = "shop_techhub", viewModel = vm)
            }
        }
        waitForTag("quick_replies_bar")
        composeRule.onNodeWithTag("quick_replies_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("edit_quick_replies_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("chat_attach_order_btn").assertIsDisplayed()
    }
}
