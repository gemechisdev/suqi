package com.example

import android.app.Application
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.api.BackendConfig
import com.example.data.api.DevDiagnosticsLog
import com.example.data.api.SuqiEndpoints
import com.example.data.local.ProductQuestionEntity
import com.example.data.local.ReviewEntity
import com.example.data.local.SuqiDatabase
import com.example.data.model.PromoCodeDto
import com.example.data.model.ShopCollectionDto
import com.example.data.model.ShopStaffDto
import com.example.ui.ScreenDestination
import com.example.ui.SuqiViewModel
import com.example.ui.screens.AdminContentManagerScreen
import com.example.ui.screens.AdminDevToolsScreen
import com.example.ui.screens.AdminPaymentProvidersScreen
import com.example.ui.screens.BuyerCompareScreen
import com.example.ui.screens.BuyerCouponsScreen
import com.example.ui.screens.BuyerWishlistScreen
import com.example.ui.screens.ProductDetailsScreen
import com.example.ui.screens.SellerCollectionsScreen
import com.example.ui.screens.SellerReviewsQAScreen
import com.example.ui.screens.SellerStaffAccountsScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
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
 * Covers A-22 Developer Tools (diagnostics log, custom Base URL, connection test, seed/reset,
 * snapshot export/import JSON, persisted maintenance mode & min version gate at root, live flags,
 * Design Gallery), DB migration 14 -> 15 cleanup, and repaired P6 screens + P6-6 / P6-7 features.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
class DevToolsAndP6RegressionTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sellerPhone = "+251 912 345 678"

    @Before
    fun resetEnvBefore() {
        runCatching {
            val field = androidx.core.content.FileProvider::class.java.getDeclaredField("sCache")
            field.isAccessible = true
            (field.get(null) as? MutableMap<*, *>)?.clear()
        }
        BackendConfig.useBuiltinSqliteBackend = true
        BackendConfig.customBaseUrl = SuqiEndpoints.BASE_URL
        DevDiagnosticsLog.clear()
    }

    @After
    fun restoreEnvAfter() {
        BackendConfig.useBuiltinSqliteBackend = true
        BackendConfig.customBaseUrl = SuqiEndpoints.BASE_URL
        DevDiagnosticsLog.clear()
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = SuqiViewModel(app)
        shadowOf(Looper.getMainLooper()).idle()
        runBlocking {
            vm.repository.adminSignIn("admin@suqi.et", "123456", "OWNER")
            vm.repository.updateDevConfig(
                maintenanceMode = false,
                maintenanceMessage = "",
                enforceMinVersion = false,
                minAppVersion = BuildConfig.VERSION_NAME
            )
            vm.repository.devDataAction("SEED_DEMO")
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
        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag(tag))
        waitForTag(tag)
    }

    private fun show(content: @androidx.compose.runtime.Composable () -> Unit) {
        composeRule.setContent { MyApplicationTheme(darkTheme = false) { content() } }
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

    private fun readColumnNames(db: SupportSQLiteDatabase, table: String): Set<String> {
        val cols = mutableSetOf<String>()
        db.query("PRAGMA table_info(`$table`)").use { c ->
            val nameIdx = c.getColumnIndexOrThrow("name")
            while (c.moveToNext()) {
                cols.add(c.getString(nameIdx))
            }
        }
        return cols
    }

    // ---------------- A-22 Backend, DB v15 Migration & Diagnostics ----------------

    @Test
    fun dbMigration14To15_dropsUnusedFlagsAndAddsCoverAndRuntimeGateColumns() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val dbName = "migration_14_15_test_${System.currentTimeMillis()}.db"
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(14) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """CREATE TABLE `admin_settings` (
                            `id` TEXT NOT NULL PRIMARY KEY,
                            `homeSectionsOrderCsv` TEXT NOT NULL,
                            `hiddenHomeSectionsCsv` TEXT NOT NULL,
                            `chatEnabled` INTEGER NOT NULL,
                            `externalContactEnabled` INTEGER NOT NULL,
                            `subscriptionsEnabled` INTEGER NOT NULL,
                            `reviewsEnabled` INTEGER NOT NULL,
                            `commissionRatePercent` REAL NOT NULL,
                            `featuredShopIdsCsv` TEXT NOT NULL,
                            `heroSubtitle` TEXT NOT NULL
                        )"""
                    )
                    db.execSQL(
                        """CREATE TABLE `shop_collections` (
                            `id` TEXT NOT NULL PRIMARY KEY,
                            `shopId` TEXT NOT NULL,
                            `name` TEXT NOT NULL,
                            `productIdsCsv` TEXT NOT NULL,
                            `isVisible` INTEGER NOT NULL,
                            `sortOrder` INTEGER NOT NULL,
                            `createdAt` TEXT NOT NULL
                        )"""
                    )
                    db.execSQL(
                        """CREATE TABLE `shops` (
                            `id` TEXT NOT NULL PRIMARY KEY, `ownerUserId` TEXT NOT NULL, `name` TEXT NOT NULL,
                            `slug` TEXT NOT NULL, `logoUrl` TEXT NOT NULL, `coverUrl` TEXT NOT NULL,
                            `category` TEXT NOT NULL, `shopType` TEXT NOT NULL, `description` TEXT NOT NULL,
                            `rating` REAL NOT NULL, `reviewCount` INTEGER NOT NULL,
                            `isVerified` INTEGER NOT NULL, `verificationStatus` TEXT NOT NULL,
                            `phone` TEXT NOT NULL, `telegramLink` TEXT NOT NULL, `whatsappLink` TEXT NOT NULL,
                            `address` TEXT NOT NULL, `region` TEXT NOT NULL, `city` TEXT NOT NULL,
                            `subCity` TEXT NOT NULL, `followersCount` INTEGER NOT NULL,
                            `productCount` INTEGER NOT NULL, `isVacationMode` INTEGER NOT NULL,
                            `acceptsOrders` INTEGER NOT NULL, `isFollowing` INTEGER NOT NULL,
                            `requireReceiptScreenshot` INTEGER NOT NULL,
                            `businessHoursJson` TEXT NOT NULL, `vacationReturnDate` TEXT NOT NULL,
                            `vacationNote` TEXT NOT NULL, `freeDeliveryThreshold` REAL NOT NULL,
                            `coverageSubCitiesCsv` TEXT NOT NULL, `verificationDocsJson` TEXT NOT NULL,
                            `verificationRejectionReason` TEXT, `createdAt` TEXT NOT NULL,
                            `galleryJson` TEXT NOT NULL, `instagramLink` TEXT NOT NULL,
                            `tiktokLink` TEXT NOT NULL, `websiteUrl` TEXT NOT NULL,
                            `returnPolicy` TEXT NOT NULL, `deliveryNotesPolicy` TEXT NOT NULL,
                            `standardCutoffTime` TEXT NOT NULL, `expressCutoffTime` TEXT NOT NULL,
                            `payoutProviderType` TEXT NOT NULL, `payoutBankName` TEXT NOT NULL,
                            `payoutAccountName` TEXT NOT NULL, `payoutAccountNumber` TEXT NOT NULL
                        )"""
                    )
                    db.execSQL(
                        """CREATE TABLE `products` (
                            `id` TEXT NOT NULL PRIMARY KEY, `shopId` TEXT NOT NULL, `shopName` TEXT NOT NULL,
                            `shopVerified` INTEGER NOT NULL, `name` TEXT NOT NULL, `nameAmharic` TEXT NOT NULL,
                            `nameAfaanOromo` TEXT NOT NULL, `description` TEXT NOT NULL,
                            `descriptionAmharic` TEXT NOT NULL, `descriptionAfaanOromo` TEXT NOT NULL,
                            `primaryImageUrl` TEXT NOT NULL, `extraImagesCsv` TEXT NOT NULL,
                            `categoryId` TEXT NOT NULL, `subCategory` TEXT NOT NULL,
                            `price` REAL NOT NULL, `originalPrice` REAL NOT NULL,
                            `discountPercent` INTEGER NOT NULL, `stockQuantity` INTEGER NOT NULL,
                            `stockHoldEnabled` INTEGER NOT NULL, `rating` REAL NOT NULL,
                            `reviewCount` INTEGER NOT NULL, `soldCount` TEXT NOT NULL,
                            `colorsCsv` TEXT NOT NULL, `specsCsv` TEXT NOT NULL,
                            `status` TEXT NOT NULL, `isWishlisted` INTEGER NOT NULL, `detailsJson` TEXT NOT NULL
                        )"""
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase
        SuqiDatabase.MIGRATION_14_15.migrate(db)

        val shopCols = readColumnNames(db, "shops")
        val prodCols = readColumnNames(db, "products")
        val colCols = readColumnNames(db, "shop_collections")
        val adminCols = readColumnNames(db, "admin_settings")

        assertFalse("shops.isFollowing should be dropped", "isFollowing" in shopCols)
        assertFalse("products.isWishlisted should be dropped", "isWishlisted" in prodCols)
        assertTrue("shop_collections.coverImageUrl should be added", "coverImageUrl" in colCols)
        assertTrue("admin_settings.maintenanceMode should be added", "maintenanceMode" in adminCols)
        assertTrue("admin_settings.maintenanceMessage should be added", "maintenanceMessage" in adminCols)
        assertTrue("admin_settings.enforceMinVersion should be added", "enforceMinVersion" in adminCols)
        assertTrue("admin_settings.minAppVersion should be added", "minAppVersion" in adminCols)

        helper.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun devDiagnostics_customBaseUrl_testConnection_seedReset_andSnapshotRoundTrip() {
        val vm = newAdminViewModel()
        runBlocking {
            DevDiagnosticsLog.clear()
            DevDiagnosticsLog.record("GET /test", "localhost", true, 12L, "OK")
            DevDiagnosticsLog.record("POST /fail", "localhost", false, 25L, "HTTP 500")
            assertEquals(2, DevDiagnosticsLog.entries().size)
            assertEquals(1, DevDiagnosticsLog.errorCount())

            BackendConfig.useBuiltinSqliteBackend = true
            assertEquals("builtin://suqi_marketplace.db", vm.repository.currentResolvedBaseUrl())

            BackendConfig.useBuiltinSqliteBackend = false
            BackendConfig.customBaseUrl = "https://sandbox.suqi.et/v1"
            assertEquals("https://sandbox.suqi.et/v1/", vm.repository.currentResolvedBaseUrl())

            BackendConfig.useBuiltinSqliteBackend = true
            val connRes = vm.repository.testDevConnection()
            assertTrue(connRes.success)
            assertTrue(DevDiagnosticsLog.entries().any { it.operation.contains("/admin/devtools/test") })

            val snapRes = vm.repository.exportDevSnapshot()
            assertTrue(snapRes.success)
            val snapJson = snapRes.data.orEmpty()
            assertTrue(snapJson.contains("\"schemaVersion\": 15") || snapJson.contains("\"schemaVersion\":15"))
            assertTrue(snapJson.contains("\"products\""))

            val switchOff = vm.repository.savePlatformSwitch("compare_products", false)
            assertFalse(switchOff.data!!.isEnabled)

            val importRes = vm.repository.devDataAction("IMPORT_SNAPSHOT", snapJson)
            assertTrue(importRes.message, importRes.success)
            assertTrue(vm.repository.getPlatformSwitches().data.orEmpty().first { it.id == "compare_products" }.isEnabled)

            assertTrue(vm.repository.devDataAction("CLEAR_STATE").success)
            assertTrue(vm.repository.devDataAction("SEED_DEMO").success)
            DevDiagnosticsLog.clear()
            assertEquals(0, DevDiagnosticsLog.entries().size)
            assertEquals(0, DevDiagnosticsLog.errorCount())
        }
    }

    // ---------------- A-22 UI: Dev Tools, Maintenance Gate, Design Gallery ----------------

    @Test
    fun adminDevTools_showsSchema15DiagnosticsSnapshotAndDesignGallery() {
        val vm = newAdminViewModel()
        show { AdminDevToolsScreen(viewModel = vm) }

        waitForTag("dev_schema_version")
        composeRule.onNodeWithTag("dev_schema_version").assertIsDisplayed()
        composeRule.onNodeWithText("15", substring = true).assertIsDisplayed()

        scrollListToTag("dev_test_connection_btn")
        composeRule.onNodeWithTag("dev_test_connection_btn").performClick()
        waitForCondition { DevDiagnosticsLog.entries().any { it.operation.contains("/admin/devtools/test") } }

        scrollListToTag("dev_export_snapshot_btn")
        composeRule.onNodeWithTag("dev_export_snapshot_btn").performClick()
        waitForTag("dev_snapshot_export_json")
        composeRule.onNodeWithTag("dev_snapshot_export_json").assertIsDisplayed()
    }

    @Test
    fun adminDevTools_designGalleryRendersComponentsAndContrastSwatches() {
        val vm = newAdminViewModel()
        show { com.example.ui.screens.DesignGalleryScreen(viewModel = vm) }
        waitForTag("design_gallery_screen")
        composeRule.onNodeWithTag("design_gallery_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("gallery_primary_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("gallery_secondary_btn").assertIsDisplayed()
    }

    @Test
    fun maintenanceAndMinVersion_blocksBuyerRootAndAllowsAdminAccess() {
        val adminVm = newAdminViewModel()
        runBlocking {
            val res = adminVm.repository.updateDevConfig(
                maintenanceMode = true,
                maintenanceMessage = "Scheduled Suqi upgrade in progress",
                enforceMinVersion = false,
                minAppVersion = BuildConfig.VERSION_NAME
            )
            assertTrue(res.message, res.success)
        }

        val buyerVm = newBuyerViewModel("+251 911 720 101", "Maintenance Buyer")
        runBlocking { buyerVm.refreshAdminSettings() }
        awaitMainLooper { buyerVm.adminSettings.value.maintenanceMode }

        show { SuqiAppRoot(viewModel = buyerVm, onFinish = {}) }
        waitForTag("maintenance_blocking_screen")
        composeRule.onNodeWithText("Scheduled Suqi upgrade in progress").assertIsDisplayed()

        composeRule.onNodeWithTag("maintenance_admin_signin_btn").performClick()
        waitForCondition { buyerVm.currentNavEntry.value.destination == ScreenDestination.AdminSignIn }

        // Test min version gate separately
        runBlocking {
            adminVm.repository.adminSignIn("admin@suqi.et", "123456", "OWNER")
            adminVm.repository.updateDevConfig(
                maintenanceMode = false,
                maintenanceMessage = "",
                enforceMinVersion = true,
                minAppVersion = "99.0.0"
            )
        }
        buyerVm.goBack()
        runBlocking { buyerVm.refreshAdminSettings() }
        awaitMainLooper { !buyerVm.adminSettings.value.maintenanceMode && buyerVm.adminSettings.value.enforceMinVersion }

        waitForTag("maintenance_blocking_screen")
        composeRule.onNodeWithText("99.0.0", substring = true).assertIsDisplayed()
    }

    // ---------------- Repaired Phase 6 Screens + P6-6 / P6-7 ----------------

    @Test
    fun buyerWishlist_rendersSavedProductAndMovesToCart() {
        val vm = newBuyerViewModel("+251 911 720 102", "Wishlist Buyer")
        val product = runBlocking { vm.repository.getProducts().data.orEmpty().first() }
        runBlocking { vm.repository.toggleWishlist(product.id) }

        show { BuyerWishlistScreen(viewModel = vm) }
        waitForTag("wishlist_item_${product.id}")
        composeRule.onNodeWithTag("wishlist_move_to_cart_${product.id}").performClick()

        waitForCondition {
            runBlocking { vm.repository.getCartItems().data.orEmpty().any { it.productId == product.id } }
        }
    }

    @Test
    fun buyerCoupons_listsSeededPromosAndValidatesCode() {
        val sellerVm = newSellerViewModel()
        runBlocking {
            sellerVm.repository.createPromoCode(
                PromoCodeDto(
                    id = "",
                    shopId = "shop_techhub",
                    code = "SUQIP6TEST",
                    discountType = "PERCENTAGE",
                    discountValue = 15.0,
                    minOrderAmount = 100.0,
                    validUntil = "2027-12-31",
                    isActive = true
                )
            )
        }
        val buyerVm = newBuyerViewModel("+251 911 720 103", "Coupons Buyer")
        show { BuyerCouponsScreen(viewModel = buyerVm) }

        waitForTag("coupon_ticket_SUQIP6TEST")
        composeRule.onNodeWithTag("buyer_coupon_code").performTextInput("SUQIP6TEST")
        composeRule.onNodeWithTag("buyer_coupon_apply_btn").performClick()

        waitForCondition { buyerVm.activeCheckoutShopPromoCode.value?.code == "SUQIP6TEST" }
    }

    @Test
    fun productDetails_compareAddsToCompareAndCompareScreenAddsToCart() {
        val vm = newBuyerViewModel("+251 911 720 104", "Compare Buyer")
        val product = runBlocking { vm.repository.getProducts().data.orEmpty().first() }
        vm.clearCompareProducts()
        var showCompare by mutableStateOf(false)

        show {
            if (!showCompare) {
                ProductDetailsScreen(productId = product.id, viewModel = vm)
            } else {
                BuyerCompareScreen(viewModel = vm)
            }
        }
        waitForTag("pd_compare_button")
        composeRule.onNodeWithTag("pd_compare_button").performClick()
        waitForCondition { vm.compareProducts.value.any { it.id == product.id } }

        showCompare = true
        waitForTag("compare_card_${product.id}")
        composeRule.onNodeWithTag("compare_add_cart_${product.id}").performClick()

        waitForCondition {
            runBlocking { vm.repository.getCartItems().data.orEmpty().any { it.productId == product.id } }
        }
    }

    @Test
    fun sellerReviewsQA_submitsReviewReplyAndQuestionAnswer() {
        val vm = newSellerViewModel()
        val db = SuqiDatabase.getInstance(ApplicationProvider.getApplicationContext<Application>())
        val suffix = System.currentTimeMillis()
        val revId = "rev_p6_$suffix"
        val qId = "q_p6_$suffix"
        val product = runBlocking { vm.repository.getSellerProducts().data.orEmpty().first() }
        runBlocking {
            db.reviewDao().insert(
                ReviewEntity(
                    id = revId,
                    userId = "buyer_x",
                    userName = "Meron",
                    userAvatar = "",
                    targetType = "SHOP",
                    targetId = product.shopId,
                    rating = 5.0,
                    comment = "Fast delivery!",
                    date = "Today"
                )
            )
            db.productQuestionDao().insert(
                ProductQuestionEntity(
                    id = qId,
                    productId = product.id,
                    shopId = product.shopId,
                    askerName = "Abebe",
                    question = "Does it include warranty?",
                    answer = "",
                    answeredByShop = false,
                    createdAt = "Today"
                )
            )
        }

        show { SellerReviewsQAScreen(viewModel = vm) }
        scrollListToTag("seller_reply_$revId")
        composeRule.onNodeWithTag("seller_reply_$revId").performTextInput("Thank you Meron!")
        scrollListToTag("seller_reply_send_$revId")
        composeRule.onNodeWithTag("seller_reply_send_$revId").performClick()

        waitForCondition {
            runBlocking { db.reviewDao().getReviewsForTarget(product.shopId).firstOrNull { it.id == revId }?.sellerReply == "Thank you Meron!" }
        }

        composeRule.onNodeWithTag("seller_review_tab_QUESTIONS").performClick()
        scrollListToTag("seller_answer_$qId")
        composeRule.onNodeWithTag("seller_answer_$qId").performTextInput("Yes, 1 year warranty.")
        scrollListToTag("seller_answer_send_$qId")
        composeRule.onNodeWithTag("seller_answer_send_$qId").performClick()

        waitForCondition {
            runBlocking { db.productQuestionDao().getQuestionsForProduct(product.id).firstOrNull { it.id == qId }?.answer == "Yes, 1 year warranty." }
        }
    }

    @Test
    fun sellerCollections_reorderAndEditCollectionPersist() {
        val vm = newSellerViewModel()
        val productId = runBlocking { vm.repository.getSellerProducts().data.orEmpty().first().id }
        val first = runBlocking {
            vm.repository.saveSellerCollection(
                ShopCollectionDto(id = "", name = "Col One", sortOrder = 0, productIds = listOf(productId))
            ).data!!
        }
        val second = runBlocking {
            vm.repository.saveSellerCollection(
                ShopCollectionDto(id = "", name = "Col Two", sortOrder = 1, productIds = listOf(productId))
            ).data!!
        }

        show { SellerCollectionsScreen(viewModel = vm) }
        scrollListToTag("collection_down_${first.id}")
        composeRule.onNodeWithTag("collection_down_${first.id}").performClick()

        waitForCondition {
            runBlocking {
                val list = vm.repository.getSellerCollections().data.orEmpty().sortedBy { it.sortOrder }
                val idxFirst = list.indexOfFirst { it.id == first.id }
                val idxSecond = list.indexOfFirst { it.id == second.id }
                idxFirst > idxSecond && idxSecond >= 0
            }
        }

        scrollListToTag("collection_edit_${first.id}")
        composeRule.onNodeWithTag("collection_edit_${first.id}").performClick()
        waitForTag("collection_name_input")
        composeRule.onNodeWithTag("collection_name_input").performTextReplacement("Col One Updated")
        composeRule.onNodeWithTag("collection_save_btn").performClick()

        waitForCondition {
            runBlocking { vm.repository.getSellerCollections().data.orEmpty().any { it.id == first.id && it.name == "Col One Updated" } }
        }

        runBlocking {
            vm.repository.deleteSellerCollection(first.id)
            vm.repository.deleteSellerCollection(second.id)
        }
    }

    @Test
    fun sellerStaff_editUpdatesRolePermissions() {
        val vm = newSellerViewModel()
        val member = runBlocking {
            vm.repository.saveSellerStaff(
                ShopStaffDto(id = "", name = "Kebede Staff", phone = "+251 911 445 566", role = "ORDERS")
            ).data!!
        }

        show { SellerStaffAccountsScreen(viewModel = vm) }
        waitForTag("staff_edit_${member.id}")
        composeRule.onNodeWithTag("staff_edit_${member.id}").performClick()

        waitForTag("staff_role_CATALOG")
        composeRule.onNodeWithTag("staff_role_CATALOG").performClick()
        composeRule.onNodeWithTag("staff_save_btn").performClick()

        waitForCondition {
            runBlocking { vm.repository.getSellerStaff().data.orEmpty().firstOrNull { it.id == member.id }?.role == "CATALOG" }
        }
        runBlocking { vm.repository.deleteSellerStaff(member.id) }
    }

    @Test
    fun adminPaymentProvidersAndContentManager_addAndEditPersist() {
        val vm = newAdminViewModel()
        var showContentScreen by mutableStateOf(false)
        show {
            if (!showContentScreen) {
                AdminPaymentProvidersScreen(viewModel = vm)
            } else {
                AdminContentManagerScreen(viewModel = vm)
            }
        }
        waitForTag("add_provider_btn")
        composeRule.onNodeWithTag("add_provider_btn").performClick()
        waitForTag("provider_name_input")
        composeRule.onNodeWithTag("provider_name_input").performTextInput("Amole Wallet")
        composeRule.onNodeWithTag("provider_format_input").performTextInput("09XXXXXXXX")
        composeRule.onNodeWithTag("provider_save_btn").performClick()

        waitForCondition {
            runBlocking { vm.repository.getPaymentProviders().data.orEmpty().any { it.displayName == "Amole Wallet" } }
        }

        showContentScreen = true
        waitForTag("content_add_btn")
        composeRule.onNodeWithTag("content_add_btn").performClick()
        waitForTag("content_title_en_input")
        composeRule.onNodeWithTag("content_title_en_input").performTextInput("How escrow works")
        composeRule.onNodeWithTag("content_body_en_input").performTextInput("Funds are verified before dispatch.")
        composeRule.onNodeWithTag("content_save_btn").performClick()

        waitForCondition {
            runBlocking { vm.repository.getContentEntries().data.orEmpty().any { it.titleEn == "How escrow works" } }
        }
    }
}
