package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.backend.BuiltinSqliteBackend
import com.example.ui.i18n.localizeDeliveryTitle
import com.example.ui.i18n.localizeEta
import com.example.util.EthiopianMarketplaceUtils
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var backend: BuiltinSqliteBackend

    @Before
    fun setup() {
        runCatching {
            val field = androidx.core.content.FileProvider::class.java.getDeclaredField("sCache")
            field.isAccessible = true
            (field.get(null) as? MutableMap<*, *>)?.clear()
        }
        context = ApplicationProvider.getApplicationContext<Context>()
        backend = BuiltinSqliteBackend(context)
    }

    @Test
    fun testAppNameResource() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Suqi", appName)
    }

    @Test
    fun testEthiopianPhoneValidationEthioTelecom() {
        // Standard formats for Ethio Telecom
        assertTrue(EthiopianMarketplaceUtils.isValidEthiopianPhone("+251912345678"))
        assertTrue(EthiopianMarketplaceUtils.isValidEthiopianPhone("+251 912 345 678"))
        assertTrue(EthiopianMarketplaceUtils.isValidEthiopianPhone("0912345678"))
        assertTrue(EthiopianMarketplaceUtils.isValidEthiopianPhone("0911234567"))
        assertTrue(EthiopianMarketplaceUtils.isValidEthiopianPhone("912345678"))

        // Normalized format
        assertEquals("+251 912 345 678", EthiopianMarketplaceUtils.normalizeEthiopianPhone("0912345678"))
    }

    @Test
    fun testEthiopianPhoneValidationSafaricom() {
        // Safaricom Ethiopia prefixes: 07... / +251 7...
        assertTrue(EthiopianMarketplaceUtils.isValidEthiopianPhone("+251712345678"))
        assertTrue(EthiopianMarketplaceUtils.isValidEthiopianPhone("+251 712 345 678"))
        assertTrue(EthiopianMarketplaceUtils.isValidEthiopianPhone("0712345678"))
        assertTrue(EthiopianMarketplaceUtils.isValidEthiopianPhone("712345678"))

        assertEquals("+251 712 345 678", EthiopianMarketplaceUtils.normalizeEthiopianPhone("0712345678"))
    }

    @Test
    fun testInvalidEthiopianPhoneNumbers() {
        assertFalse(EthiopianMarketplaceUtils.isValidEthiopianPhone("0812345678")) // invalid prefix
        assertFalse(EthiopianMarketplaceUtils.isValidEthiopianPhone("12345")) // too short
        assertFalse(EthiopianMarketplaceUtils.isValidEthiopianPhone("+123456789012")) // wrong country
        assertFalse(EthiopianMarketplaceUtils.isValidEthiopianPhone("abcdefghij")) // non-numeric
    }

    @Test
    fun testAddisAbabaSubCitiesCoverage() {
        val subCities = EthiopianMarketplaceUtils.ADDIS_ABABA_SUB_CITIES
        // Must contain all 11 official Addis Ababa sub-cities
        assertEquals(11, subCities.size)
        assertTrue(subCities.contains("Bole"))
        assertTrue(subCities.contains("Kirkos"))
        assertTrue(subCities.contains("Yeka"))
        assertTrue(subCities.contains("Arada"))
        assertTrue(subCities.contains("Nifas Silk Lafto"))
        assertTrue(subCities.contains("Lideta"))
        assertTrue(subCities.contains("Kolfe Keranio"))
        assertTrue(subCities.contains("Gullele"))
        assertTrue(subCities.contains("Akaky Kaliti"))
        assertTrue(subCities.contains("Addis Ketema"))
        assertTrue(subCities.contains("Lemi Kura"))
    }

    @Test
    fun testPaymentReferenceValidation() {
        assertTrue(EthiopianMarketplaceUtils.isValidPaymentReference("CBE1234567"))
        assertTrue(EthiopianMarketplaceUtils.isValidPaymentReference("TLB-987654"))
        assertTrue(EthiopianMarketplaceUtils.isValidPaymentReference("AWASH_TXN_001"))

        // Invalid: too short or contains illegal chars
        assertFalse(EthiopianMarketplaceUtils.isValidPaymentReference("123"))
        assertFalse(EthiopianMarketplaceUtils.isValidPaymentReference("TXN <SCRIPT>"))
    }

    @Test
    fun testOrderPlacementAndDuplicateReferenceRejection() = runBlocking {
        val initialProducts = backend.getProducts().data ?: emptyList()
        assertTrue("Products should be seeded", initialProducts.isNotEmpty())
        val testProduct = initialProducts.first()
        val initialStock = testProduct.stockQuantity

        val itemsArr = JSONArray()
        val itemObj = JSONObject()
        itemObj.put("productId", testProduct.id)
        itemObj.put("quantity", 2)
        itemsArr.put(itemObj)

        val uniqueTxnRef = "TXN_TEST_${System.currentTimeMillis()}"

        val orderPayload = mapOf(
            "shopId" to testProduct.shopId,
            "shopName" to testProduct.shopName,
            "customerName" to "Fitsum Gebre",
            "customerPhone" to "+251 912 345 678",
            "deliveryAddress" to "Bole, Addis Ababa",
            "subtotal" to testProduct.price * 2,
            "deliveryFee" to 150.0,
            "discountAmount" to 0.0,
            "promoCode" to "",
            "deliveryOptionTitle" to "Standard Delivery",
            "deliveryOptionType" to "STANDARD",
            "deliveryEstimatedDays" to "1-2 days",
            "paymentMethodTitle" to "Telebirr",
            "paymentMethodType" to "TELEBIRR",
            "paymentMethodAccount" to "0911234567",
            "transactionReference" to uniqueTxnRef,
            "itemsJson" to itemsArr.toString()
        )

        // 1. First order placement must succeed
        val firstOrderRes = backend.submitOrder(orderPayload)
        assertTrue("First order placement should succeed", firstOrderRes.success)
        assertNotNull(firstOrderRes.data)
        val order = firstOrderRes.data!!

        // 2. Stock must be decremented if stockHoldEnabled
        val updatedProduct = backend.getProductDetails(testProduct.id).data
        assertNotNull(updatedProduct)
        if (testProduct.stockHoldEnabled) {
            assertEquals(initialStock - 2, updatedProduct!!.stockQuantity)
        }

        // 3. Attempting duplicate order placement with same transactionReference must fail!
        val duplicateRes = backend.submitOrder(orderPayload)
        assertFalse("Duplicate transaction reference must be rejected", duplicateRes.success)
        assertTrue(duplicateRes.message?.contains("already been submitted") == true)

        // 4. Test order cancellation restores held stock
        val cancelRes = backend.cancelOrder(order.id)
        assertTrue("Order cancellation should succeed", cancelRes.success)

        val restoredProduct = backend.getProductDetails(testProduct.id).data
        if (testProduct.stockHoldEnabled) {
            assertEquals(initialStock, restoredProduct!!.stockQuantity)
        }
    }

    @Test
    fun testSellerPaymentVerificationAndAudit() = runBlocking {
        val products = backend.getProducts().data ?: emptyList()
        val prod = products.first()

        val itemsArr = JSONArray()
        val itemObj = JSONObject()
        itemObj.put("productId", prod.id)
        itemObj.put("quantity", 1)
        itemsArr.put(itemObj)

        val txnRef = "TXN_AUDIT_${System.currentTimeMillis()}"
        val orderRes = backend.submitOrder(
            mapOf(
                "shopId" to prod.shopId,
                "shopName" to prod.shopName,
                "customerName" to "Hanna Tadesse",
                "customerPhone" to "+251 922 334 455",
                "deliveryAddress" to "Kirkos, Addis Ababa",
                "subtotal" to prod.price,
                "deliveryFee" to 100.0,
                "discountAmount" to 0.0,
                "promoCode" to "",
                "deliveryOptionTitle" to "Standard Delivery",
                "deliveryOptionType" to "STANDARD",
                "deliveryEstimatedDays" to "1-2 days",
                "paymentMethodTitle" to "CBE Birr",
                "paymentMethodType" to "CBE_BIRR",
                "paymentMethodAccount" to "100012345678",
                "transactionReference" to txnRef,
                "itemsJson" to itemsArr.toString()
            )
        )
        assertTrue(orderRes.success)
        val orderId = orderRes.data!!.id

        // Seller verifies payment
        val verifyRes = backend.verifyPayment(orderId, mapOf("notes" to "Bank SMS matches ETB amount"))
        assertTrue("Seller verification should succeed", verifyRes.success)

        val updatedOrder = backend.getOrderDetails(orderId).data
        assertNotNull(updatedOrder)
        assertEquals("VERIFIED", updatedOrder!!.paymentStatus)
        assertEquals("Confirmed", updatedOrder.orderStatus)
        assertNotNull(updatedOrder.verifiedAt)
    }

    @Test
    fun testVerifiedBuyerReviewEligibility() = runBlocking {
        val products = backend.getProducts().data ?: emptyList()
        val prod = products.first()
        val initialReviewCount = prod.reviewCount

        // 1. Submit review
        val reviewRes = backend.submitReview(
            mapOf(
                "targetId" to prod.id,
                "targetType" to "PRODUCT",
                "rating" to 5.0,
                "comment" to "Excellent quality Ethiopian product. Arrived quickly in Bole!"
            )
        )
        assertTrue(reviewRes.success)
        assertNotNull(reviewRes.data)

        // 2. Product review count should be incremented
        val updatedProd = backend.getProductDetails(prod.id).data
        assertNotNull(updatedProd)
        assertEquals(initialReviewCount + 1, updatedProd!!.reviewCount)
    }

    @Test
    fun testAdminFeatureToggles() = runBlocking {
        val initialSettings = backend.getAdminSettings().data
        assertNotNull(initialSettings)

        // Toggle chat feature
        val currentChat = initialSettings!!.isChatEnabled
        backend.updateFeatureToggle("chat", !currentChat)

        val updatedSettings = backend.getAdminSettings().data
        assertNotNull(updatedSettings)
        assertEquals(!currentChat, updatedSettings!!.isChatEnabled)
    }

    @Test
    fun testCanonicalSixteenCategoriesSeeded() = runBlocking {
        val cats = backend.getCategories().data ?: emptyList()
        assertEquals(16, cats.size)
        assertEquals(
            listOf(
                "Electronics",
                "Fashion",
                "Home & Living",
                "Beauty & Care",
                "Sports & Outdoors",
                "Bags & Accessories",
                "Furniture",
                "Health & Fitness",
                "Baby, Kids & Toys",
                "Groceries & Food",
                "Traditional Clothes",
                "Automotive",
                "Books & Stationery",
                "Arts & Crafts",
                "Garden & Agriculture",
                "Gifts & Occasions"
            ),
            cats.map { it.name }
        )
        assertTrue(cats.none { it.id == "cat_toys" })
        assertTrue(cats.all { it.imageUrl.startsWith("https://cdn.jsdelivr.net/gh/gamistudios/myfiles@") })
    }

    @Test
    fun testSuqiStringCatalogHasNoMissingTranslations() {
        val missing = com.example.ui.i18n.SuqiStringCatalog.findMissingTranslations()
        assertTrue("Missing translations found: $missing", missing.isEmpty())
    }

    @Test
    fun testVerificationDocsCodecRoundTripAndFailedKeys() {
        val submission = com.example.data.model.VerificationSubmission(
            docs = mapOf(
                "idFront" to "https://example.com/id_front.jpg",
                "tradeLicense" to "https://example.com/license.jpg"
            ),
            licenseRef = "ETH-LIC-1002",
            tin = "0045678912",
            submittedAt = "2026-11-14 10:00",
            failedKeys = listOf("idBack", "shopPhoto"),
            history = listOf(
                com.example.data.model.VerificationHistoryEntry(
                    status = "CHANGES_REQUESTED",
                    at = "2026-11-14 10:30",
                    note = "Please retake blurry ID back and shop photo",
                    failedKeys = listOf("idBack", "shopPhoto")
                )
            )
        )
        val json = com.example.data.model.VerificationDocsCodec.toJson(submission)
        val parsed = com.example.data.model.VerificationDocsCodec.parse(json)
        assertEquals(submission.docs, parsed.docs)
        assertEquals("ETH-LIC-1002", parsed.licenseRef)
        assertEquals("0045678912", parsed.tin)
        assertEquals(listOf("idBack", "shopPhoto"), parsed.failedKeys)
        assertEquals(1, parsed.history.size)
        assertEquals("CHANGES_REQUESTED", parsed.history.first().status)
    }

    @Test
    fun testSellerVerificationSubmitRequestChangesResubmitApprove() = runBlocking {
        // Ensure a seller user & shop exist in DB
        val signIn = backend.loginWithPhoneOtp(
            mapOf("phone" to "+251911000001", "otp" to "123456", "name" to "Test Seller")
        )
        assertTrue(signIn.success)
        val openShopRes = backend.upgradeToSeller(
            mapOf(
                "shopName" to "Addis Craft Studio",
                "city" to "Addis Ababa",
                "subCity" to "Bole",
                "description" to "Handmade Ethiopian crafts",
                "phone" to "+251911000001"
            )
        )
        assertTrue(openShopRes.success)
        val shopId = openShopRes.data!!.id

        val submitRes = backend.submitSellerVerification(
            mapOf(
                "idFront" to "content://doc/id_front",
                "idBack" to "content://doc/id_back",
                "tradeLicense" to "content://doc/license",
                "shopPhoto" to "content://doc/shop",
                "selfieWithId" to "content://doc/selfie",
                "license" to "LIC-8899",
                "tin" to "0011223344"
            )
        )
        assertTrue(submitRes.success)

        val queue1 = backend.getAdminSellerVerificationQueue().data.orEmpty()
        assertTrue(queue1.any { it.id == shopId && it.verificationStatus == "PENDING_REVIEW" })

        val reqChangesRes = backend.adminVerifySellerDecision(
            shopId = shopId,
            decision = "REQUEST_CHANGES",
            reason = "Back of ID is cropped",
            failedKeys = "idBack"
        )
        assertTrue(reqChangesRes.success)

        val afterReq = backend.getShopDetails(shopId).data!!
        assertEquals("CHANGES_REQUESTED", afterReq.verificationStatus)
        val parsedAfterReq = com.example.data.model.VerificationDocsCodec.parse(afterReq.verificationDocsJson)
        assertEquals(listOf("idBack"), parsedAfterReq.failedKeys)

        val resubmitRes = backend.submitSellerVerification(
            mapOf("idBack" to "content://doc/id_back_clear")
        )
        assertTrue(resubmitRes.success)
        val afterResubmit = backend.getShopDetails(shopId).data!!
        assertEquals("RESUBMITTED", afterResubmit.verificationStatus)

        val approveRes = backend.adminVerifySellerDecision(
            shopId = shopId,
            decision = "APPROVE",
            reason = "All documents verified",
            failedKeys = null
        )
        assertTrue(approveRes.success)
        val verifiedShop = backend.getShopDetails(shopId).data!!
        assertTrue(verifiedShop.isVerified)
        assertEquals("VERIFIED", verifiedShop.verificationStatus)
    }

    @Test
    fun testPlaceNamesLocalizedForAllLanguages() {
        assertTrue(com.example.ui.i18n.SuqiPlaces.isComplete())
        val am = com.example.ui.i18n.SuqiLanguage.AM
        val om = com.example.ui.i18n.SuqiLanguage.OM
        assertEquals("አዲስ አበባ", com.example.ui.i18n.SuqiPlaces.localize("Addis Ababa", am))
        assertEquals("Boolee", com.example.ui.i18n.SuqiPlaces.localize("Bole", om))
        assertEquals("ቦሌ, አዲስ አበባ", com.example.ui.i18n.SuqiPlaces.localize("Bole, Addis Ababa", am))
        assertEquals("Unknown Town", com.example.ui.i18n.SuqiPlaces.localize("Unknown Town", am))
        // every sub-city and hub offered in the app must have a translation
        (com.example.util.EthiopianMarketplaceUtils.ADDIS_ABABA_SUB_CITIES +
            com.example.util.EthiopianMarketplaceUtils.REGIONAL_COMMERCIAL_HUBS).forEach {
            assertTrue("Missing place translation: $it", it in com.example.ui.i18n.SuqiPlaces.allKeys())
        }
    }

    @Test
    fun testApiMessagesAreLocalizedAndUnknownPassThrough() {
        val am = com.example.ui.i18n.SuqiLanguage.AM
        val om = com.example.ui.i18n.SuqiLanguage.OM
        val en = com.example.ui.i18n.SuqiLanguage.EN
        assertTrue(com.example.ui.i18n.SuqiApiMessages.isComplete())
        assertEquals("Order not found", com.example.ui.i18n.SuqiApiMessages.localize("Order not found", en))
        assertEquals("ትዕዛዙ አልተገኘም።", com.example.ui.i18n.SuqiApiMessages.localize("Order not found", am))
        assertEquals("Ajajni hin argamne.", com.example.ui.i18n.SuqiApiMessages.localize("Order not found", om))
        val stock = com.example.ui.i18n.SuqiApiMessages.localize("Only 3 items in stock!", am)
        assertTrue(stock.contains("3"))
        val insufficient = com.example.ui.i18n.SuqiApiMessages.localize("Insufficient stock for Phone! Only 2 left.", om)
        assertTrue(insufficient.contains("Phone") && insufficient.contains("2"))
        assertEquals("Something custom from server", com.example.ui.i18n.SuqiApiMessages.localize("Something custom from server", am))
    }

    @Test
    fun testDeliveryTitleAndEtaLocalization() {
        val am = com.example.ui.i18n.SuqiExtraCatalog.amharic
        assertEquals(am.deliveryTitleExpress, am.localizeDeliveryTitle("Express Delivery"))
        assertEquals(am.deliveryTitlePickup, am.localizeDeliveryTitle("Store Pickup"))
        assertEquals("Custom Courier", am.localizeDeliveryTitle("Custom Courier"))
        assertEquals("1-3 የስራ ቀናት", am.localizeEta("1-3 business days", com.example.ui.i18n.SuqiLanguage.AM))
        assertEquals("Guyyaa 1-2", com.example.ui.i18n.SuqiExtraCatalog.afaanOromo.localizeEta("1-2 days", com.example.ui.i18n.SuqiLanguage.OM))
    }

    @Test
    fun testVerificationWaitingAgeFormattingAndThresholds() {
        val enStrings = com.example.ui.i18n.SuqiStringCatalog.forLanguage(com.example.ui.i18n.SuqiLanguage.EN)

        val isoBase = EthiopianMarketplaceUtils.parseMarketplaceTimestampMillis("2026-11-15 10:00")
        assertNotNull(isoBase)
        val formattedBase = EthiopianMarketplaceUtils.parseMarketplaceTimestampMillis("Nov 15, 2026 • 10:00 AM")
        assertNotNull(formattedBase)
        assertEquals(isoBase, formattedBase)

        assertEquals(0L, EthiopianMarketplaceUtils.computeElapsedHours("Just now"))
        assertEquals(24L, EthiopianMarketplaceUtils.computeElapsedHours("Yesterday"))

        // < 1h -> Just now
        assertEquals(
            enStrings.extras.justNowLabel,
            EthiopianMarketplaceUtils.formatWaitingAgeLabel("2026-11-15 10:00", enStrings, isoBase!! + 30L * 60_000L)
        )
        // 4h (< 6h seller warning)
        assertEquals(
            "4h ago",
            EthiopianMarketplaceUtils.formatWaitingAgeLabel("2026-11-15 10:00", enStrings, isoBase + 4L * 3_600_000L)
        )
        // 8h (>= 6h seller warning threshold)
        val hours8 = EthiopianMarketplaceUtils.computeElapsedHours("2026-11-15 10:00", isoBase + 8L * 3_600_000L)
        assertEquals(8L, hours8)
        assertEquals(
            "8h ago",
            EthiopianMarketplaceUtils.formatWaitingAgeLabel("2026-11-15 10:00", enStrings, isoBase + 8L * 3_600_000L)
        )
        // 48h (>= 24h admin warning threshold)
        assertEquals(
            "2d ago",
            EthiopianMarketplaceUtils.formatWaitingAgeLabel("2026-11-15 10:00", enStrings, isoBase + 48L * 3_600_000L)
        )
    }

    @Test
    fun testOrderTransferredAmountAndVerificationStatusTransitions() = runBlocking {
        // Ensure active user owns shop_techhub deterministically
        val loginRes = backend.loginWithPhoneOtp(
            mapOf("phone" to "+251 912 345 678", "otp" to "123456")
        )
        assertTrue(loginRes.success)

        val shopProducts = backend.getProducts(shopId = "shop_techhub").data.orEmpty()
        assertTrue("shop_techhub should have seeded products", shopProducts.isNotEmpty())
        val prod = shopProducts.first()

        val itemsArr = JSONArray().apply {
            put(JSONObject().apply {
                put("productId", prod.id)
                put("quantity", 1)
            })
        }
        val expectedTotal = prod.price + 100.0

        val basePayload = mapOf(
            "shopId" to "shop_techhub",
            "shopName" to prod.shopName,
            "customerName" to "Meron Tadesse",
            "customerPhone" to "+251 922 334 455",
            "deliveryAddress" to "Bole, Addis Ababa",
            "subtotal" to prod.price,
            "deliveryFee" to 100.0,
            "discountAmount" to 0.0,
            "promoCode" to "",
            "deliveryOptionTitle" to "Standard Delivery",
            "deliveryOptionType" to "STANDARD",
            "deliveryEstimatedDays" to "1-2 days",
            "paymentMethodTitle" to "Telebirr",
            "paymentMethodType" to "TELEBIRR",
            "paymentMethodAccount" to "0911234567",
            "itemsJson" to itemsArr.toString()
        )

        val ts = System.currentTimeMillis()
        val matchTxn = "TXN_MATCH_${ts}_A"
        val mismatchTxn = "TXN_MISMATCH_${ts}_B"

        val matchOrderRes = backend.submitOrder(
            basePayload + mapOf(
                "transactionReference" to matchTxn,
                "transferredAmount" to expectedTotal
            )
        )
        assertTrue(matchOrderRes.success)
        val matchOrder = matchOrderRes.data!!
        assertEquals(expectedTotal, matchOrder.transferredAmount ?: -1.0, 0.01)

        kotlinx.coroutines.delay(5)

        val mismatchOrderRes = backend.submitOrder(
            basePayload + mapOf(
                "transactionReference" to mismatchTxn,
                "transferredAmount" to (expectedTotal - 50.0)
            )
        )
        assertTrue(mismatchOrderRes.success)
        val mismatchOrder = mismatchOrderRes.data!!
        assertEquals(expectedTotal - 50.0, mismatchOrder.transferredAmount ?: -1.0, 0.01)

        // Seller approves matching order & rejects mismatched order
        assertTrue(backend.verifyPayment(matchOrder.id, mapOf("notes" to "Exact ETB match")).success)
        assertTrue(backend.rejectPayment(mismatchOrder.id, mapOf("reason" to "Transferred ETB is 50 short")).success)

        val sellerOrders = backend.getSellerOrders().data.orEmpty()
        val verified = sellerOrders.firstOrNull { it.id == matchOrder.id }
        val rejected = sellerOrders.firstOrNull { it.id == mismatchOrder.id }
        assertNotNull(verified)
        assertNotNull(rejected)
        assertEquals("VERIFIED", verified!!.paymentStatus)
        assertEquals("REJECTED", rejected!!.paymentStatus)
        assertEquals(expectedTotal, verified.transferredAmount ?: -1.0, 0.01)

        // Buyer resubmits corrected transfer amount & new ref -> moves back to PENDING_VERIFICATION
        val resubmittedTxn = "TXN_RESUB_${System.currentTimeMillis()}"
        val resubmitRes = backend.resubmitPayment(
            mismatchOrder.id,
            mapOf(
                "transactionReference" to resubmittedTxn,
                "transferredAmount" to expectedTotal.toInt().toString()
            )
        )
        assertTrue(resubmitRes.success)
        val resubmitted = resubmitRes.data!!
        assertEquals("PENDING_VERIFICATION", resubmitted.paymentStatus)
        assertEquals(expectedTotal, resubmitted.transferredAmount ?: -1.0, 0.01)
    }

    @Test
    fun testItemTypesCrudAndTemplateSync() = runBlocking {
        val initialRes = backend.getItemTypes()
        assertTrue(initialRes.success)
        val initialTypes = initialRes.data.orEmpty()
        assertTrue("Default item types should be seeded into Room", initialTypes.size >= 10)

        val customId = "type_test_${System.currentTimeMillis()}"
        val newType = com.example.data.model.ItemTypeDto(
            id = customId,
            label = "Ethiopian Jebena",
            emoji = "☕",
            categoryId = "cat_home",
            group = "Home & Living",
            attributes = listOf(
                com.example.data.model.AttributeDef(
                    key = "capacity",
                    label = "Capacity",
                    type = com.example.data.model.AttrType.MEASUREMENT,
                    options = listOf("ml", "L"),
                    unit = "ml",
                    required = true,
                    selectable = true
                )
            ),
            sortOrder = initialTypes.size
        )
        val saveRes = backend.saveItemType(newType)
        assertTrue(saveRes.success)
        val afterSave = backend.getItemTypes().data.orEmpty()
        assertTrue(afterSave.any { it.id == customId && it.label == "Ethiopian Jebena" })
        assertTrue(
            "ProductTemplates.all should sync from persisted item types",
            com.example.data.model.ProductTemplates.byId(customId).label == "Ethiopian Jebena"
        )

        val delRes = backend.deleteItemType(customId)
        assertTrue(delRes.success)
        val afterDelete = backend.getItemTypes().data.orEmpty()
        assertFalse(afterDelete.any { it.id == customId })
    }

    @Test
    fun testCategoriesBannersReorderAndHomeSectionsConfig() = runBlocking {
        val initialCats = backend.getCategories().data.orEmpty()
        assertTrue(initialCats.size >= 3)
        val initialCatIds = initialCats.map { it.id }
        val initialBanners = backend.getPromoBanners(activeOnly = false).data.orEmpty()
        val initialBannerIds = initialBanners.map { it.id }
        val initialSettings = backend.getAdminSettings().data!!

        val reversedIds = initialCatIds.reversed()
        val targetOriginalCat = initialCats.first { it.id == reversedIds.first() }
        try {
            val reorderCatsRes = backend.reorderCategories(reversedIds)
            assertTrue(reorderCatsRes.success)
            val reorderedCats = backend.getCategories().data.orEmpty()
            assertEquals(reversedIds.first(), reorderedCats.first().id)

            // Update linked item types on first category
            val targetCat = reorderedCats.first()
            val savedCatRes = backend.saveCategory(
                targetCat.copy(linkedItemTypes = listOf("laptop", "smartphone"))
            )
            assertTrue(savedCatRes.success)
            assertEquals(listOf("laptop", "smartphone"), savedCatRes.data!!.linkedItemTypes)

            // Banners reorder & Home sections visibility/order
            if (initialBanners.size >= 2) {
                val revBanners = initialBannerIds.reversed()
                val reorderBannerRes = backend.reorderPromoBanners(revBanners)
                assertTrue(reorderBannerRes.success)
                val reorderedBanners = backend.getPromoBanners(activeOnly = false).data.orEmpty()
                assertEquals(revBanners.first(), reorderedBanners.first().id)
            }

            val customOrder = listOf(
                com.example.data.model.HomeSectionKeys.NEW_ARRIVALS,
                com.example.data.model.HomeSectionKeys.FEATURED_SHOPS,
                com.example.data.model.HomeSectionKeys.POPULAR,
                com.example.data.model.HomeSectionKeys.RECOMMENDED,
                com.example.data.model.HomeSectionKeys.RECENTLY_VIEWED
            )
            val hidden = listOf(com.example.data.model.HomeSectionKeys.RECENTLY_VIEWED)
            val secRes = backend.updateHomeSectionsConfig(
                mapOf("order" to customOrder, "hidden" to hidden)
            )
            assertTrue(secRes.success)
            val updatedSettings = backend.getAdminSettings().data!!
            assertEquals(customOrder, updatedSettings.homeSectionsOrder)
            assertEquals(hidden, updatedSettings.hiddenHomeSections)
        } finally {
            backend.saveCategory(targetOriginalCat)
            backend.reorderCategories(initialCatIds)
            if (initialBannerIds.size >= 2) {
                backend.reorderPromoBanners(initialBannerIds)
            }
            backend.updateHomeSectionsConfig(
                mapOf(
                    "order" to initialSettings.homeSectionsOrder,
                    "hidden" to initialSettings.hiddenHomeSections
                )
            )
        }
    }

    @Test
    fun testOwnerMetricsSupportResolveAndBroadcast() = runBlocking {
        val m7 = backend.getOwnerDashboardMetrics(7).data
        val m30 = backend.getOwnerDashboardMetrics(30).data
        val m90 = backend.getOwnerDashboardMetrics(90).data
        val mAll = backend.getOwnerDashboardMetrics(365).data
        assertNotNull(m7)
        assertNotNull(m30)
        assertNotNull(m90)
        assertNotNull(mAll)
        assertEquals(7, m7!!.rangeDays)
        assertEquals(30, m30!!.rangeDays)
        // Seeded orders are 1d (25,049), 10d (12,549), and 45d (9,999) ago
        assertTrue("7D GMV (${m7.totalGmv}) should be strictly less than 30D GMV (${m30.totalGmv})", m7.totalGmv < m30.totalGmv)
        assertTrue("30D GMV (${m30.totalGmv}) should be strictly less than 90D GMV (${m90!!.totalGmv})", m30.totalGmv < m90.totalGmv)
        assertEquals("GMV buckets must sum to real range GMV without artificial floor", m30.totalGmv, m30.gmvValues.sum(), 0.01)
        assertEquals("7D GMV buckets must sum to real 7D GMV", m7.totalGmv, m7.gmvValues.sum(), 0.01)
        assertEquals(7, m7.gmvValues.size)
        assertEquals(4, m30.gmvValues.size)
        assertEquals(3, m90.gmvValues.size)
        assertEquals(m30.gmvValues.size, m30.previousGmvValues.size)
        // Top categories ranked by actual ordered item sales
        assertTrue(m30.topCategories.isNotEmpty())
        assertEquals("cat_electronics", m30.topCategories.first().id)
        assertTrue(m30.topShops.isNotEmpty())
        assertEquals(m30.gmvValues.size, m30.topShops.first().sparkline.size)

        // Support ticket creation + resolution
        val ticketRes = backend.submitSupportTicket(
            com.example.data.model.SupportTicketDto(
                id = "",
                userId = "",
                category = "Order Issue",
                subject = "Receipt inquiry #99",
                description = "Need help verifying receipt",
                status = "OPEN",
                createdAt = ""
            )
        )
        assertTrue(ticketRes.success)
        val ticketId = ticketRes.data!!.id
        val adminRes = backend.adminSignIn(mapOf("identifier" to "admin@suqi.et", "pin" to "123456", "tier" to "OWNER"))
        assertTrue(adminRes.success)
        val resolveRes = backend.resolveSupportTicket(ticketId)
        assertTrue(resolveRes.success)
        assertEquals(true, resolveRes.data)
        val ticketsAfterResolve = backend.getSupportTickets().data.orEmpty()
        assertEquals("RESOLVED", ticketsAfterResolve.first { it.id == ticketId }.status)

        // Admin broadcast creates notification
        val broadcastRes = backend.sendAdminBroadcast(
            mapOf(
                "title" to "System Maintenance Notice",
                "body" to "Scheduled upgrade tonight at 11 PM EAT.",
                "audience" to "ALL"
            )
        )
        assertTrue(broadcastRes.success)
        val notifs = backend.getNotifications().data.orEmpty()
        assertTrue(notifs.any { it.title == "System Maintenance Notice" })
    }

    @Test
    fun testBannerAudienceFilteringAndWindowedPreviousPeriod() = runBlocking {
        val now = System.currentTimeMillis()
        val sdf = java.text.SimpleDateFormat("MMM d, yyyy • hh:mm a", java.util.Locale.US)
        val recentUser = com.example.data.model.UserDto(
            id = "u_new", name = "New User", phone = "+251911000000", email = "",
            avatarUrl = "", role = "CUSTOMER", preferredLanguage = "English",
            region = "Addis Ababa", city = "Addis Ababa", subCity = "Bole",
            createdAt = sdf.format(java.util.Date(now - 2L * 86_400_000L))
        )
        val olderUser = recentUser.copy(
            id = "u_old",
            city = "Adama",
            createdAt = sdf.format(java.util.Date(now - 40L * 86_400_000L))
        )

        assertTrue(EthiopianMarketplaceUtils.matchesBannerAudience("ALL", olderUser, 3, "Adama", now))
        assertTrue(EthiopianMarketplaceUtils.matchesBannerAudience("NEW_USERS", recentUser, 2, "Adama", now))
        assertTrue(EthiopianMarketplaceUtils.matchesBannerAudience("NEW_USERS", olderUser, 0, "Adama", now))
        assertFalse(EthiopianMarketplaceUtils.matchesBannerAudience("NEW_USERS", olderUser, 3, "Adama", now))
        assertTrue(EthiopianMarketplaceUtils.matchesBannerAudience("ADDIS_ONLY", olderUser, 3, "Addis Ababa", now))
        assertFalse(EthiopianMarketplaceUtils.matchesBannerAudience("ADDIS_ONLY", olderUser, 3, "Adama", now))
    }

    @Test
    fun testShopExtendedCodecsAndSellerPreferencesStore() {
        val defaultWeek = com.example.data.model.ShopBusinessHoursCodec.defaultWeek()
        assertEquals(7, defaultWeek.size)
        assertFalse(defaultWeek.first { it.dayKey == "SUN" }.isOpen)

        val editedWeek = defaultWeek.map {
            if (it.dayKey == "MON") it.copy(openTime = "09:00", closeTime = "20:00") else it
        }
        val hoursJson = com.example.data.model.ShopBusinessHoursCodec.toJson(editedWeek)
        val parsedWeek = com.example.data.model.ShopBusinessHoursCodec.parse(hoursJson)
        assertEquals("09:00", parsedWeek.first { it.dayKey == "MON" }.openTime)
        assertEquals("20:00", parsedWeek.first { it.dayKey == "MON" }.closeTime)

        val galleryInput = listOf("a.jpg", "b.jpg", "c.jpg", "d.jpg", "e.jpg", "f.jpg", "extra.jpg")
        val galleryJson = com.example.data.model.ShopGalleryCodec.toJson(galleryInput)
        val parsedGallery = com.example.data.model.ShopGalleryCodec.parse(galleryJson)
        assertEquals(6, parsedGallery.size)

        val store = com.example.data.local.SellerPreferencesStore(context)
        val draft = com.example.data.local.ProductDraftData(
            name = "Ethiopian Coffee Grinder",
            priceStr = "3200",
            savedAtLabel = "14:20"
        )
        store.saveProductDraft(draft)
        val loadedDraft = store.loadProductDraft()
        assertNotNull(loadedDraft)
        assertEquals("Ethiopian Coffee Grinder", loadedDraft!!.name)
        store.clearProductDraft()
        assertNull(store.loadProductDraft())

        val notifPrefs = com.example.data.local.SellerNotificationPrefs(
            newOrder = true,
            paymentSubmitted = false,
            lowStock = true,
            newReview = false,
            buyerQuestion = true
        )
        store.saveNotificationPrefs(notifPrefs)
        assertEquals(notifPrefs, store.loadNotificationPrefs())

        val customReplies = listOf("Payment received ✓", "Ready for pickup at Bole")
        store.saveQuickReplies(customReplies)
        assertEquals(customReplies, store.loadQuickReplies(listOf("Default")))
    }

    @Test
    fun testPhase3InventoryBulkDuplicateProfileAndInboxBackend() = runBlocking {
        val loginRes = backend.loginWithPhoneOtp(
            mapOf("phone" to "+251 912 345 678", "otp" to "123456")
        )
        assertTrue(loginRes.success)

        val initialSellerProds = backend.getSellerProducts().data.orEmpty()
        assertTrue("Seller should have seeded products", initialSellerProds.isNotEmpty())
        val baseProd = initialSellerProds.first()

        // 1. Duplicate listing creates DRAFT copy
        val dupRes = backend.duplicateProduct(baseProd.id)
        assertTrue(dupRes.success)
        val duplicated = dupRes.data!!
        assertEquals("DRAFT", duplicated.status)
        assertTrue(duplicated.name.contains("Copy"))

        // 2. Stock update + bulk operations
        val stockRes = backend.updateProductStock(duplicated.id, mapOf("stockQuantity" to 19))
        assertTrue(stockRes.success)
        assertEquals(19, stockRes.data!!.stockQuantity)

        val bulkStockRes = backend.bulkUpdateProducts(
            mapOf("ids" to listOf(duplicated.id), "action" to "SET_STOCK", "value" to "4")
        )
        assertTrue(bulkStockRes.success)
        assertEquals(1, bulkStockRes.data)
        val afterBulkStock = backend.getProductDetails(duplicated.id).data!!
        val expectedBulkStock = if (duplicated.details.variants.isNotEmpty()) {
            4 * duplicated.details.variants.size
        } else {
            4
        }
        assertEquals(expectedBulkStock, afterBulkStock.stockQuantity)

        val bulkDiscountRes = backend.bulkUpdateProducts(
            mapOf("ids" to listOf(duplicated.id), "action" to "APPLY_DISCOUNT", "value" to "20")
        )
        assertTrue(bulkDiscountRes.success)
        assertEquals(20, backend.getProductDetails(duplicated.id).data!!.discountPercent)

        val bulkDeleteRes = backend.bulkUpdateProducts(
            mapOf("ids" to listOf(duplicated.id), "action" to "DELETE")
        )
        assertTrue(bulkDeleteRes.success)
        assertFalse(backend.getSellerProducts().data.orEmpty().any { it.id == duplicated.id })

        // 3. Shop profile & delivery extended fields
        val weekJson = com.example.data.model.ShopBusinessHoursCodec.toJson(
            com.example.data.model.ShopBusinessHoursCodec.defaultWeek()
        )
        val galleryJson = com.example.data.model.ShopGalleryCodec.toJson(listOf("https://example.com/1.jpg"))
        val updateShopRes = backend.updateShopSettings(
            mapOf(
                "shopId" to baseProd.shopId,
                "businessHoursJson" to weekJson,
                "galleryJson" to galleryJson,
                "instagramLink" to "@suqi_shop",
                "tiktokLink" to "@suqi_tiktok",
                "websiteUrl" to "https://suqi.et",
                "returnPolicy" to "7 days exchange",
                "deliveryNotesPolicy" to "Dispatched same day",
                "standardCutoffTime" to "15:30",
                "expressCutoffTime" to "13:00"
            )
        )
        assertTrue(updateShopRes.success)
        val updatedShop = backend.getShopDetails(baseProd.shopId).data!!
        assertEquals("@suqi_shop", updatedShop.instagramLink)
        assertEquals("@suqi_tiktok", updatedShop.tiktokLink)
        assertEquals("https://suqi.et", updatedShop.websiteUrl)
        assertEquals("7 days exchange", updatedShop.returnPolicy)
        assertEquals("Dispatched same day", updatedShop.deliveryNotesPolicy)
        assertEquals("15:30", updatedShop.standardCutoffTime)
        assertEquals("13:00", updatedShop.expressCutoffTime)
        assertEquals(1, com.example.data.model.ShopGalleryCodec.parse(updatedShop.galleryJson).size)

        // 4. Seller inbox read/unread & order-context chat message
        val markUnreadRes = backend.markChatConversationRead(baseProd.shopId, false)
        assertTrue(markUnreadRes.success)
        val convAfterUnread = backend.getChatConversations().data.orEmpty().first { it.shopId == baseProd.shopId }
        assertFalse(convAfterUnread.isRead)

        val markReadRes = backend.markChatConversationRead(baseProd.shopId, true)
        assertTrue(markReadRes.success)
        val convAfterRead = backend.getChatConversations().data.orEmpty().first { it.shopId == baseProd.shopId }
        assertTrue(convAfterRead.isRead)

        val msgRes = backend.sendChatMessage(
            baseProd.shopId,
            mapOf(
                "senderType" to "SHOP",
                "message" to "Your parcel is packed",
                "orderContextRef" to "SUQ-2025-001234",
                "orderContextStatus" to "Dispatched",
                "orderContextTotal" to "ETB 25,049",
                "threadCategory" to "ORDER"
            )
        )
        assertTrue(msgRes.success)
        val sentMsg = msgRes.data!!
        assertEquals("SHOP", sentMsg.senderType)
        assertEquals("SUQ-2025-001234", sentMsg.orderContextRef)
        assertEquals("ORDER", sentMsg.threadCategory)
    }

    @Test
    fun testQrCodeEncodesAndDecodesViaZxingAndCreatesShareUri() = runBlocking {
        val url = "https://suqi.et/shop/techhub"
        val bitmap = com.example.ui.components.SuqiQrGenerator.renderQrCardBitmap(
            content = url,
            title = "Share Storefront & QR",
            badgeText = "Tech Hub"
        )
        assertTrue(bitmap.width >= 800)
        assertTrue(bitmap.height > bitmap.width)

        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val source = com.google.zxing.RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
        val binaryBitmap = com.google.zxing.BinaryBitmap(com.google.zxing.common.HybridBinarizer(source))
        val decoded = com.google.zxing.qrcode.QRCodeReader().decode(binaryBitmap)
        assertEquals(url, decoded.text)

        val shareUri = com.example.ui.components.SuqiQrGenerator.createShareQrImageUri(context, bitmap, "techhub")
        assertEquals("content", shareUri.scheme)
        assertEquals("${context.packageName}.fileprovider", shareUri.authority)
        bitmap.recycle()
    }

    @Test
    fun testImportPickerImageOnlyCopiesToFilesDir() = runBlocking {
        val sourceFile = java.io.File(context.cacheDir, "picker_source_test.png")
        val bmp = android.graphics.Bitmap.createBitmap(64, 64, android.graphics.Bitmap.Config.ARGB_8888)
        sourceFile.outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()

        val importedUriStr = com.example.ui.components.importPickerImageOnly(
            context,
            android.net.Uri.fromFile(sourceFile)
        )
        val importedUri = android.net.Uri.parse(importedUriStr)
        assertEquals("file", importedUri.scheme)
        val importedFile = java.io.File(requireNotNull(importedUri.path))
        assertTrue("Imported file should exist", importedFile.exists())
        assertTrue("Imported file should reside in filesDir/suqi_uploads", importedFile.absolutePath.contains("suqi_uploads"))
    }
}



