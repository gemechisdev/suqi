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
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.screens.AdminBannersScreen
import com.example.ui.screens.AdminCategoriesScreen
import com.example.ui.screens.AdminConsoleScreen
import com.example.ui.screens.AdminItemTypesScreen
import com.example.ui.screens.OwnerDashboardScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
class Phase2ComposeUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun newViewModel(): SuqiViewModel =
        SuqiViewModel(ApplicationProvider.getApplicationContext<Application>())

    private fun waitForTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = 8_000) {
            shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun adminConsole_switchesTabsAndNavigatesQuickTools() {
        val vm = newViewModel()
        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                AdminConsoleScreen(viewModel = vm)
            }
        }
        waitForTag("admin_tab_queues")

        composeRule.onNodeWithTag("admin_tab_queues").performClick()
        waitForTag("queue_row_verifications")
        composeRule.onNodeWithTag("queue_row_verifications").assertIsDisplayed()

        composeRule.onNodeWithTag("admin_tab_overview").performClick()
        composeRule.waitForIdle()
        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag("admin_quick_tool_banners"))
        composeRule.onNodeWithTag("admin_quick_tool_banners").performClick()
        assertEquals(ScreenDestination.AdminBanners, vm.screen.value)
    }

    @Test
    fun adminBanners_switchesToHomeSectionsAndOpensEditor() {
        val vm = newViewModel()
        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                AdminBannersScreen(viewModel = vm)
            }
        }
        waitForTag("admin_curator_tab_sections")

        composeRule.onNodeWithTag("admin_curator_tab_sections").performClick()
        waitForTag("section_switch_featured_shops")
        composeRule.onNodeWithTag("section_switch_featured_shops").assertIsDisplayed()

        composeRule.onNodeWithTag("admin_curator_tab_banners").performClick()
        waitForTag("add_banner_btn")
        composeRule.onNodeWithTag("add_banner_btn").performClick()
        waitForTag("save_banner_btn")
        composeRule.onNodeWithTag("save_banner_btn").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun adminItemTypes_opensNewTemplateEditorAndPreviewTabs() {
        val vm = newViewModel()
        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                AdminItemTypesScreen(viewModel = vm)
            }
        }
        waitForTag("new_item_type_btn")

        composeRule.onNodeWithTag("new_item_type_btn").performClick()
        waitForTag("save_item_type_btn")
        composeRule.onNodeWithTag("save_item_type_btn").assertIsDisplayed()

        composeRule.onNodeWithTag("item_type_editor_tab_seller_preview").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("item_type_editor_tab_buyer_preview").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun adminCategories_switchesToShopTypesAndOpensAddSheet() {
        val vm = newViewModel()
        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                AdminCategoriesScreen(viewModel = vm)
            }
        }
        waitForTag("admin_cat_tab_shop_types")

        composeRule.onNodeWithTag("admin_cat_tab_shop_types").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("admin_cat_tab_categories").performClick()
        waitForTag("add_category_btn")
        composeRule.onNodeWithTag("add_category_btn").performClick()
        waitForTag("save_category_btn")
        composeRule.onNodeWithTag("save_category_btn").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun ownerDashboard_switchesDateRangeNavigatesAlertAndOpensExport() {
        val vm = newViewModel()
        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                OwnerDashboardScreen(viewModel = vm)
            }
        }
        waitForTag("owner_range_7")

        composeRule.onNodeWithTag("owner_range_7").performClick()
        waitForTag("owner_kpi_gmv")
        composeRule.onNodeWithTag("owner_kpi_gmv").assertIsDisplayed()

        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag("owner_alert_payments"))
        composeRule.onNodeWithTag("owner_alert_payments").performClick()
        assertEquals(ScreenDestination.AdminConsole, vm.screen.value)
        assertEquals("queues", vm.consumeAdminConsoleInitialTab())

        composeRule.onNodeWithTag("owner_export_btn").performClick()
        waitForTag("owner_copy_csv_btn")
        composeRule.onNodeWithTag("owner_copy_csv_btn").performScrollTo().assertIsDisplayed()
    }
}
