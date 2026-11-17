package com.example

import android.app.Application
import android.os.Looper
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.ui.SuqiViewModel
import com.example.ui.screens.AppSettingsScreen
import com.example.ui.screens.HelpLegalScreen
import com.example.ui.screens.NotificationPrefsScreen
import com.example.ui.screens.ProfileEditScreen
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

/** Phase 4 buyer screens: every test performs at least one state-changing action. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
class Phase4ComposeUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun newViewModel(signIn: Boolean = false): SuqiViewModel {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = SuqiViewModel(app)
        shadowOf(Looper.getMainLooper()).idle()
        vm.buyerPrefs.clearAll()
        if (signIn) {
            runBlocking { vm.repository.loginWithPhoneOtp("+251 912 345 678", "123456") }
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

    private fun scrollTo(tag: String) {
        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag(tag))
        waitForTag(tag)
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
    fun profile_deleteAccountNeedsTypedConfirmation() {
        val vm = newViewModel(signIn = true)
        composeRule.setContent { MyApplicationTheme(darkTheme = false) { ProfileEditScreen(viewModel = vm) } }
        waitForTag("profile_avatar_picker")
        scrollTo("profile_delete_account_btn")
        composeRule.onNodeWithTag("profile_delete_account_btn").performClick()
        waitForTag("input_delete_confirm")
        composeRule.onNodeWithTag("btn_confirm_delete_account").assertIsNotEnabled()
        composeRule.onNodeWithTag("input_delete_confirm").performTextInput("DELETE")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("btn_confirm_delete_account").assertIsEnabled()
    }
}
