package com.example

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.OrderEntity
import com.example.data.local.SuqiDatabase
import com.example.data.model.ContentEntryDto
import com.example.data.model.LocationAreaDto
import com.example.data.model.PaymentProviderDto
import com.example.ui.SuqiViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Phase 6 backend rules: seeded config, user suspension, disputes, config CRUD, switches, shop actions, audit log. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Phase6LogicTest {

    private fun newVm(): SuqiViewModel {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = SuqiViewModel(app)
        shadowOf(Looper.getMainLooper()).idle()
        return vm
    }

    private fun signInAdmin(vm: SuqiViewModel) = runBlocking {
        assertTrue(vm.repository.adminSignIn("admin@suqi.et", "123456", "OWNER").success)
    }

    private fun createBuyer(vm: SuqiViewModel, phone: String, name: String) =
        runBlocking { vm.repository.loginWithPhoneOtp(phone, "123456", name).data!! }

    private fun auditActions(vm: SuqiViewModel): List<String> =
        runBlocking { vm.repository.getAuditLog().data.orEmpty().map { it.action } }

    private fun order(id: String, buyerId: String, paymentStatus: String = "REJECTED") = OrderEntity(
        id = id,
        orderReference = "SUQ-P6-${id.takeLast(6)}",
        customerUserId = buyerId,
        customerName = "P6 Buyer",
        customerPhone = "+251 911 700 000",
        deliveryAddress = "Bole",
        shopId = "shop_techhub",
        shopName = "Tech Hub",
        itemsJson = "[]",
        subtotal = 1500.0,
        deliveryFee = 0.0,
        discountAmount = 0.0,
        totalAmount = 1500.0,
        deliveryOptionTitle = "Standard",
        deliveryOptionType = "STANDARD",
        deliveryEstimatedDays = "1-2 days",
        paymentMethodTitle = "Telebirr",
        paymentMethodType = "TELEBIRR",
        paymentMethodAccount = "+251 912",
        transactionReference = "TXN-P6-$id",
        paymentStatus = paymentStatus,
        orderStatus = "Processing",
        createdAt = "Just now",
        payerIdentifier = "+251 911 700 000"
    )

    @Test
    fun seededConfig_switchesProvidersLocationsAndContentExist() {
        val vm = newVm()
        runBlocking {
            val switches = vm.repository.getPlatformSwitches().data.orEmpty()
            assertTrue("platform switches must be seeded", switches.map { it.id }.containsAll(
                listOf("q_and_a", "video_uploads", "product_variants", "flash_sales", "compare_products")
            ))
            assertTrue(vm.repository.getPaymentProviders().data.orEmpty().any { it.id == "prov_telebirr" })
            val areas = vm.repository.getLocationAreas().data.orEmpty()
            assertEquals(setOf("REGION", "CITY", "SUB_CITY"), areas.map { it.level }.toSet())
            assertTrue(vm.repository.getContentEntries().data.orEmpty().any { it.section == "FAQ" })
        }
    }

    @Test
    fun suspendUser_requiresAdmin_writesAuditAndCanBeRestored() {
        val vm = newVm()
        val buyer = createBuyer(vm, "+251 911 700 101", "Suspend Me")
        // Signed in as a buyer: admin APIs are refused.
        runBlocking {
            assertFalse(vm.repository.setUserSuspended(buyer.id, true, "spam").success)
            assertFalse(vm.repository.getAuditLog().success)
        }
        signInAdmin(vm)
        runBlocking {
            val suspended = vm.repository.setUserSuspended(buyer.id, true, "Repeated fake orders")
            assertTrue(suspended.message, suspended.success)
            assertTrue(suspended.data!!.isSuspended)
            assertTrue(vm.repository.getAdminUserDetail(buyer.id).data!!.isSuspended)

            val restored = vm.repository.setUserSuspended(buyer.id, false)
            assertTrue(restored.success)
            assertFalse(restored.data!!.isSuspended)

            assertFalse("staff accounts cannot be suspended", vm.repository.setUserSuspended("user_admin", true).success)
            assertFalse(vm.repository.setUserSuspended("no_such_user", true).success)
        }
        val actions = auditActions(vm)
        assertTrue(actions.contains("USER_SUSPENDED"))
        assertTrue(actions.contains("USER_RESTORED"))
    }

    @Test
    fun disputeDecision_requiresNoteAndKnownDecision_cancelsOrderOnRefund() {
        val db = SuqiDatabase.getInstance(ApplicationProvider.getApplicationContext<Application>())
        val vm = newVm()
        val buyer = createBuyer(vm, "+251 911 700 102", "Dispute Buyer")
        val orderId = "ord_p6_${System.currentTimeMillis()}"
        runBlocking { db.orderDao().insertOrder(order(orderId, buyer.id)) }
        signInAdmin(vm)
        runBlocking {
            assertTrue(vm.repository.getAdminDisputes().data.orEmpty().any { it.orderId == orderId })
            assertFalse("note is required", vm.repository.decideDispute(orderId, "REFUND_BUYER", note = "  ").success)
            assertFalse("unknown decision", vm.repository.decideDispute(orderId, "FLIP_COIN", note = "x").success)
            val res = vm.repository.decideDispute(orderId, "REFUND_BUYER", note = "Seller never shipped", refundAmount = 1500.0)
            assertTrue(res.message, res.success)
            assertEquals("RESOLVED_BUYER", res.data!!.status)
            assertEquals("Cancelled", db.orderDao().getOrderById(orderId)!!.orderStatus)
            assertFalse(vm.repository.decideDispute("missing_order", "ESCALATE", note = "x").success)
        }
        assertTrue(auditActions(vm).contains("DISPUTE_DECIDED"))
    }

    @Test
    fun disputeDecision_sideWithSellerKeepsOrderStatus() {
        val db = SuqiDatabase.getInstance(ApplicationProvider.getApplicationContext<Application>())
        val vm = newVm()
        val buyer = createBuyer(vm, "+251 911 700 103", "Seller Side Buyer")
        val orderId = "ord_p6_side_${System.currentTimeMillis()}"
        runBlocking { db.orderDao().insertOrder(order(orderId, buyer.id)) }
        signInAdmin(vm)
        runBlocking {
            val res = vm.repository.decideDispute(orderId, "SIDE_WITH_SELLER", note = "Proof of delivery supplied")
            assertTrue(res.success)
            assertEquals("RESOLVED_SELLER", res.data!!.status)
            assertEquals("Processing", db.orderDao().getOrderById(orderId)!!.orderStatus)
        }
    }

    @Test
    fun paymentProviders_crudValidationAndAudit() {
        val vm = newVm()
        createBuyer(vm, "+251 911 700 104", "Not Admin")
        runBlocking {
            assertFalse(vm.repository.savePaymentProvider(PaymentProviderDto(id = "", displayName = "Nope")).success)
        }
        signInAdmin(vm)
        runBlocking {
            assertFalse("name required", vm.repository.savePaymentProvider(PaymentProviderDto(id = "", displayName = " ")).success)
            val saved = vm.repository.savePaymentProvider(
                PaymentProviderDto(id = "prov_test_p6", displayName = " Dashen Bank ", referenceHint = "DB + 8 digits", sortOrder = 9)
            )
            assertTrue(saved.message, saved.success)
            assertEquals("Dashen Bank", saved.data!!.displayName)
            assertTrue(vm.repository.getPaymentProviders().data.orEmpty().any { it.id == "prov_test_p6" })

            val deactivated = vm.repository.savePaymentProvider(saved.data!!.copy(isActive = false))
            assertFalse(deactivated.data!!.isActive)

            assertTrue(vm.repository.deletePaymentProvider("prov_test_p6").success)
            assertFalse(vm.repository.getPaymentProviders().data.orEmpty().any { it.id == "prov_test_p6" })
            assertFalse(vm.repository.deletePaymentProvider("prov_test_p6").success)
        }
        val actions = auditActions(vm)
        assertTrue(actions.contains("PAYMENT_PROVIDER_SAVED"))
        assertTrue(actions.contains("PAYMENT_PROVIDER_DELETED"))
    }

    @Test
    fun locations_crudValidationAndAudit() {
        val vm = newVm()
        signInAdmin(vm)
        runBlocking {
            assertFalse(vm.repository.saveLocationArea(LocationAreaDto(id = "", name = "", level = "CITY")).success)
            val saved = vm.repository.saveLocationArea(
                LocationAreaDto(id = "loc_test_p6", name = "Adama", parentId = "loc_oromia", level = "CITY", sortOrder = 5)
            )
            assertTrue(saved.message, saved.success)
            assertEquals("Adama", vm.repository.getLocationAreas().data.orEmpty().first { it.id == "loc_test_p6" }.name)
            assertTrue(vm.repository.deleteLocationArea("loc_test_p6").success)
            assertNull(vm.repository.getLocationAreas().data.orEmpty().firstOrNull { it.id == "loc_test_p6" })
            assertFalse(vm.repository.deleteLocationArea("loc_test_p6").success)
        }
        val actions = auditActions(vm)
        assertTrue(actions.contains("LOCATION_SAVED"))
        assertTrue(actions.contains("LOCATION_DELETED"))
    }

    @Test
    fun contentEntry_savePersistsAndRequiresAdmin() {
        val vm = newVm()
        createBuyer(vm, "+251 911 700 105", "Content Buyer")
        runBlocking {
            assertFalse(vm.repository.saveContentEntry(ContentEntryDto(id = "faq_p6", section = "FAQ", titleEn = "x")).success)
        }
        signInAdmin(vm)
        runBlocking {
            val res = vm.repository.saveContentEntry(
                ContentEntryDto(id = "faq_p6", section = "FAQ", titleEn = "Do you deliver outside Addis?", bodyEn = "Yes, to major cities.")
            )
            assertTrue(res.message, res.success)
            val stored = vm.repository.getContentEntries().data.orEmpty().first { it.id == "faq_p6" }
            assertEquals("Do you deliver outside Addis?", stored.titleEn)
            assertTrue(stored.updatedAt.isNotBlank())
        }
        assertTrue(auditActions(vm).contains("CONTENT_SAVED"))
    }

    @Test
    fun platformSwitch_togglePersistsAndWritesAuditWithBeforeAfter() {
        val vm = newVm()
        createBuyer(vm, "+251 911 700 106", "Switch Buyer")
        runBlocking { assertFalse(vm.repository.savePlatformSwitch("video_uploads", false).success) }
        signInAdmin(vm)
        runBlocking {
            assertFalse("id required", vm.repository.savePlatformSwitch("", false).success)
            val res = vm.repository.savePlatformSwitch("video_uploads", false, 3.0)
            assertTrue(res.message, res.success)
            val stored = vm.repository.getPlatformSwitches().data.orEmpty().first { it.id == "video_uploads" }
            assertFalse(stored.isEnabled)
            assertEquals(3.0, stored.numericValue, 0.0)
            val entry = vm.repository.getAuditLog().data.orEmpty().first { it.action == "SWITCH_UPDATED" && it.targetId == "video_uploads" }
            assertEquals("true", entry.beforeValue)
            assertEquals("false", entry.afterValue)
            // Toggle back so shared state is left as seeded.
            assertTrue(vm.repository.savePlatformSwitch("video_uploads", true, 0.0).data!!.isEnabled)
        }
    }

    @Test
    fun shopActions_suspendFeatureNotesAndUnknownAction() {
        val vm = newVm()
        signInAdmin(vm)
        runBlocking {
            val suspended = vm.repository.adminShopAction("shop_techhub", "SUSPEND", reason = "Policy check")
            assertTrue(suspended.message, suspended.success)
            assertTrue(suspended.data!!.isSuspended)
            assertFalse(vm.repository.adminShopAction("shop_techhub", "RESTORE").data!!.isSuspended)
            assertTrue(vm.repository.adminShopAction("shop_techhub", "FEATURE").data!!.isFeatured)
            assertFalse(vm.repository.adminShopAction("shop_techhub", "UNFEATURE").data!!.isFeatured)
            assertFalse(vm.repository.adminShopAction("shop_techhub", "EXPLODE").success)
            assertFalse(vm.repository.adminShopAction("no_shop", "SUSPEND").success)
            assertEquals("Call owner Monday", vm.repository.saveAdminShopNotes("shop_techhub", "Call owner Monday").data!!.notes)
        }
        val actions = auditActions(vm)
        listOf("SHOP_SUSPENDED", "SHOP_RESTORED", "SHOP_FEATURED", "SHOP_UNFEATURED", "SHOP_NOTE_SAVED").forEach {
            assertTrue("missing audit $it", actions.contains(it))
        }
    }

    @Test
    fun auditLog_recordsActorAndIsNewestFirstWithBoundedSize() {
        val vm = newVm()
        signInAdmin(vm)
        runBlocking {
            vm.repository.savePlatformSwitch("flash_sales", false)
            vm.repository.savePlatformSwitch("flash_sales", true)
            val log = vm.repository.getAuditLog().data.orEmpty()
            assertTrue(log.size <= 500)
            val latest = log.first { it.targetId == "flash_sales" }
            assertNotNull(latest.actorUserId.takeIf { it.isNotBlank() })
            assertEquals("OWNER", latest.actorRole)
        }
    }
}
