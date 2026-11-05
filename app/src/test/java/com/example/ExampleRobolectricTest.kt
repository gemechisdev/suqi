package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.backend.BuiltinSqliteBackend
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
}
