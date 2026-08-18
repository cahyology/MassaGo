package com.massago.customer

import com.massago.customer.data.model.CustomerOrderStatus
import com.massago.customer.data.model.CustomerPaymentMethod
import com.massago.customer.data.model.CustomerPredefinedServices
import com.massago.customer.data.model.PressureLevel
import com.massago.customer.data.repository.CustomerOrderRepository
import com.massago.customer.data.repository.CustomerUserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CustomerOrderRepositoryTest {

    private lateinit var orderRepository: CustomerOrderRepository
    private lateinit var userRepository: CustomerUserRepository

    @Before
    fun setUp() {
        userRepository = CustomerUserRepository.instance
        orderRepository = CustomerOrderRepository.instance
    }

    @Test
    fun `test creating and placing customer order calculates pricing and voucher correctly`() = runTest {
        val service = CustomerPredefinedServices.SERVICES[0] // Pijat Tradisional Jawa (90 min = 160000)
        val initialBalance = userRepository.profile.value.walletBalance

        val order = orderRepository.createAndPlaceOrder(
            service = service,
            durationMinutes = 90,
            aromaId = "aroma-lavender", // extra fee 15000
            focusAreas = listOf("Pundak & Leher", "Punggung Bawah"),
            pressureLevel = PressureLevel.MEDIUM,
            genderPreference = "Wanita Saja",
            voucherCode = "MASSAGOBARU", // 30% discount capped at 35000
            paymentMethod = CustomerPaymentMethod.PIJATIN_PAY
        )

        assertNotNull(order)
        assertEquals(160000L, order.basePrice)
        assertEquals(15000L, order.selectedAroma.extraFee)
        assertEquals(175000L, order.subtotal)
        assertEquals(35000L, order.discountAmount) // capped at 35000
        assertEquals(CustomerOrderStatus.SEARCHING_THERAPIST, order.status)

        // Subtotal (175k) + Travel (15k) + Hygiene (5k) - Discount (35k) = 160k
        assertEquals(160000L, order.grandTotal)

        // Verify wallet deducted
        val newBalance = userRepository.profile.value.walletBalance
        assertEquals(initialBalance - 160000L, newBalance)
    }

    @Test
    fun `test order tracking state transitions and rating completion`() = runTest {
        val service = CustomerPredefinedServices.SERVICES[1] // Refleksi

        val order = orderRepository.createAndPlaceOrder(
            service = service,
            durationMinutes = 60,
            aromaId = "aroma-olive",
            focusAreas = listOf("Kaki & Betis"),
            pressureLevel = PressureLevel.STRONG,
            genderPreference = "Bebas",
            voucherCode = null,
            paymentMethod = CustomerPaymentMethod.CASH_ON_DELIVERY
        )

        assertEquals(CustomerOrderStatus.SEARCHING_THERAPIST, orderRepository.activeOrder.value?.status)

        // Simulate arrival
        orderRepository.simulateTherapistArrival()
        assertEquals(CustomerOrderStatus.THERAPIST_ARRIVED, orderRepository.activeOrder.value?.status)

        // Start treatment
        orderRepository.startTreatmentSession()
        assertEquals(CustomerOrderStatus.TREATMENT_IN_PROGRESS, orderRepository.activeOrder.value?.status)
        assertTrue(orderRepository.activeOrder.value?.isTimerRunning == true)

        // Extend duration (+15 min)
        val initialRemaining = orderRepository.activeOrder.value?.remainingSeconds ?: 0
        orderRepository.extendTreatmentDuration(15)
        val extendedRemaining = orderRepository.activeOrder.value?.remainingSeconds ?: 0
        assertEquals(initialRemaining + 900, extendedRemaining)

        // Finish session
        orderRepository.finishTreatmentSession()
        assertEquals(CustomerOrderStatus.TREATMENT_FINISHED_PAYMENT, orderRepository.activeOrder.value?.status)

        // Submit rating and tip
        orderRepository.submitRatingAndReview(
            rating = 5,
            comment = "Pelayanan sangat memuaskan dan higienis!",
            tags = listOf("Sangat Higienis", "Tekanan Pas"),
            tip = 20000L
        )

        // Active order should be cleared and added to history
        assertEquals(null, orderRepository.activeOrder.value)
        val latestHistory = orderRepository.orderHistory.value.first()
        assertEquals(5, latestHistory.ratingGiven)
        assertEquals(20000L, latestHistory.tipAmount)
    }
}
