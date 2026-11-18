package com.example

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.OrderEntity
import com.example.data.local.SuqiDatabase
import com.example.data.model.SupportTicketDto
import com.example.ui.SuqiViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Phase 5 backend rules: returns, payouts, promo validation, reviews 2.0, support tickets. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Phase5LogicTest {

    private val sellerPhone = "+251 912 345 678"

    private fun newVm(): SuqiViewModel {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = SuqiViewModel(app)
        shadowOf(Looper.getMainLooper()).idle()
        return vm
    }

    private fun deliveredOrder(id: String, buyerId: String, buyerName: String, buyerPhone: String) = OrderEntity(
        id = id,
        orderReference = "SUQ-P5-${id.takeLast(6)}",
        customerUserId = buyerId,
        customerName = buyerName,
        customerPhone = buyerPhone,
        deliveryAddress = "Bole",
        shopId = "shop_techhub",
        shopName = "Tech Hub",
        itemsJson = "[{\"productId\":\"prod_laptop_hp\",\"productName\":\"HP Laptop\",\"productImageUrl\":\"\",\"variation\":\"\",\"unitPrice\":1000.0,\"quantity\":2,\"totalPrice\":2000.0}]",
        subtotal = 2000.0,
        deliveryFee = 0.0,
        discountAmount = 0.0,
        totalAmount = 2000.0,
        deliveryOptionTitle = "Standard",
        deliveryOptionType = "STANDARD",
        deliveryEstimatedDays = "1-2 days",
        paymentMethodTitle = "Telebirr",
        paymentMethodType = "TELEBIRR",
        paymentMethodAccount = "+251 912",
        transactionReference = "TXN-P5-$id",
        paymentStatus = "VERIFIED",
        orderStatus = "Delivered",
        createdAt = "Just now",
        payerIdentifier = buyerPhone
    )

    @Test
    fun returns_requestDuplicateBlockedAndSellerDecisionRules() {
        val db = SuqiDatabase.getInstance(ApplicationProvider.getApplicationContext<Application>())
        val vm = newVm()
        val phone = "+251 911 500 100"
        val buyer = runBlocking { vm.repository.loginWithPhoneOtp(phone, "123456", "Return Buyer").data!! }
        val orderId = "ord_p5_${System.currentTimeMillis()}"
        runBlocking { db.orderDao().insertOrder(deliveredOrder(orderId, buyer.id, buyer.name, phone)) }

        val first = runBlocking {
            vm.repository.submitReturnRequest(mapOf("orderId" to orderId, "reason" to "Damaged", "resolution" to "REFUND"))
        }
        assertTrue(first.message, first.success)
        val ret = first.data!!
        assertEquals("REQUESTED", ret.status)

        val duplicate = runBlocking { vm.repository.submitReturnRequest(mapOf("orderId" to orderId, "reason" to "Damaged")) }
        assertFalse(duplicate.success)
        assertEquals(1, runBlocking { vm.repository.getBuyerReturns().data.orEmpty().count { it.orderReference == ret.orderReference } })

        runBlocking {
            vm.repository.loginWithPhoneOtp(sellerPhone, "123456")
            vm.repository.updateUserProfile(mapOf("activeRole" to "SELLER"))
            assertFalse(vm.repository.decideReturnRequest(ret.id, "REJECT", note = "").success)
            val approved = vm.repository.decideReturnRequest(ret.id, "APPROVE", note = "Send it back")
            assertTrue(approved.success)
            assertEquals("APPROVED", approved.data!!.status)
        }
    }

    @Test
    fun returns_nonDeliveredOrderIsRejected() {
        val db = SuqiDatabase.getInstance(ApplicationProvider.getApplicationContext<Application>())
        val vm = newVm()
        val phone = "+251 911 500 200"
        val buyer = runBlocking { vm.repository.loginWithPhoneOtp(phone, "123456", "Pending Buyer").data!! }
        val orderId = "ord_p5_pending_${System.currentTimeMillis()}"
        runBlocking {
            db.orderDao().insertOrder(deliveredOrder(orderId, buyer.id, buyer.name, phone).copy(orderStatus = "Processing"))
            assertFalse(vm.repository.submitReturnRequest(mapOf("orderId" to orderId, "reason" to "Changed mind")).success)
        }
    }

    @Test
    fun payouts_requireMinimumAndAvailableBalance() {
        val vm = newVm()
        runBlocking {
            vm.repository.loginWithPhoneOtp(sellerPhone, "123456")
            vm.repository.updateUserProfile(mapOf("activeRole" to "SELLER"))
            assertFalse(vm.repository.requestSellerPayout(1.0).success)
            assertFalse(vm.repository.requestSellerPayout(1_000_000_000.0).success)
            val summary = vm.repository.getSellerEarningsSummary()
            assertTrue(summary.message, summary.success)
            assertNotNull(summary.data)
        }
    }

    @Test
    fun promoValidation_enforcesActiveAndMinimumSpend() {
        val vm = newVm()
        runBlocking {
            val promo = vm.repository.getShopPromotions("shop_techhub").data.orEmpty().firstOrNull { it.isActive }
            assertNotNull(promo)
            assertFalse(vm.repository.validatePromoCode("shop_techhub", "NO_SUCH_CODE", 100_000.0).success)
            assertTrue(vm.repository.validatePromoCode("shop_techhub", promo!!.code, promo.minOrderAmount + 100_000.0).success)
            if (promo.minOrderAmount > 0.0) {
                assertFalse(vm.repository.validatePromoCode("shop_techhub", promo.code, 0.0).success)
            }
        }
    }

    @Test
    fun reviews_helpfulTogglesPerUserAndSellerReplyNeedsText() {
        val vm = newVm()
        runBlocking {
            vm.repository.loginWithPhoneOtp("+251 911 500 300", "123456", "Reviewer")
            val review = vm.repository.submitReview(
                mapOf("targetId" to "prod_laptop_hp", "targetType" to "PRODUCT", "rating" to 4.0, "comment" to "Solid")
            ).data!!
            assertEquals(1, vm.repository.toggleReviewHelpful(review.id).data!!.helpfulCount)
            assertEquals(0, vm.repository.toggleReviewHelpful(review.id).data!!.helpfulCount)

            vm.repository.loginWithPhoneOtp(sellerPhone, "123456")
            vm.repository.updateUserProfile(mapOf("activeRole" to "SELLER"))
            assertFalse(vm.repository.replyToReview(review.id, "  ").success)
            assertEquals("Thanks!", vm.repository.replyToReview(review.id, "Thanks!").data!!.sellerReply)
        }
    }

    @Test
    fun supportTickets_messagesAreStoredPerTicket() {
        val vm = newVm()
        runBlocking {
            val buyer = vm.repository.loginWithPhoneOtp("+251 911 500 400", "123456", "Ticket Buyer").data!!
            val ticket = vm.repository.submitSupportTicket(
                SupportTicketDto(id = "", userId = buyer.id, category = "Orders", subject = "Where is it", description = "Late", status = "OPEN", createdAt = "")
            ).data!!
            assertTrue(vm.repository.sendTicketMessage(ticket.id, "Any update?").success)
            val messages = vm.repository.getTicketMessages(ticket.id).data.orEmpty()
            assertTrue(messages.any { it.message == "Any update?" })
            assertTrue(messages.all { it.ticketId == ticket.id })
        }
    }
}
