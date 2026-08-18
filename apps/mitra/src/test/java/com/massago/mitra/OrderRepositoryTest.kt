package com.massago.mitra

import com.massago.mitra.data.model.ChecklistItemType
import com.massago.mitra.data.model.DutyStatus
import com.massago.mitra.data.model.OrderStatus
import com.massago.mitra.data.repository.OrderRepository
import com.massago.mitra.data.repository.TherapistRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrderRepositoryTest {

    private lateinit var orderRepository: OrderRepository
    private lateinit var therapistRepository: TherapistRepository

    @Before
    fun setUp() {
        orderRepository = OrderRepository.instance
        therapistRepository = TherapistRepository.instance
        therapistRepository.setDutyStatus(DutyStatus.ONLINE)
    }

    @Test
    fun testSimulateAndAcceptOrderLifecycle() = runTest {
        // Step 1: Simulate Incoming Order
        orderRepository.simulateIncomingOrder()
        val incoming = orderRepository.activeOrder.value
        assertNotNull("Incoming order should not be null", incoming)
        assertEquals(OrderStatus.INCOMING, incoming?.status)

        // Step 2: Accept Order
        orderRepository.acceptOrder()
        val onTheWay = orderRepository.activeOrder.value
        assertEquals(OrderStatus.ACCEPTED_ON_THE_WAY, onTheWay?.status)
        assertEquals(DutyStatus.ON_DUTY_BUSY, therapistRepository.therapistProfile.value.dutyStatus)

        // Step 3: Arrive at Location
        orderRepository.arriveAtLocation()
        assertEquals(OrderStatus.ARRIVED_AT_LOCATION, orderRepository.activeOrder.value?.status)

        // Step 4: Sanitation & Prep
        orderRepository.startSanitationChecklist()
        assertEquals(OrderStatus.SANITATION_AND_PREP, orderRepository.activeOrder.value?.status)

        orderRepository.updateChecklistItem(ChecklistItemType.HANDS_SANITIZED, true)
        orderRepository.updateChecklistItem(ChecklistItemType.MAT_COVER_REPLACED, true)
        orderRepository.updateChecklistItem(ChecklistItemType.OIL_AROMA_CONFIRMED, true)
        orderRepository.updateChecklistItem(ChecklistItemType.PRESSURE_CHECKED, true)
        assertTrue("Checklist should be complete", orderRepository.activeOrder.value?.isPrepComplete == true)

        // Step 5: Start Treatment
        orderRepository.startTreatment()
        assertEquals(OrderStatus.TREATMENT_IN_PROGRESS, orderRepository.activeOrder.value?.status)

        // Step 6: Finish Treatment & Payment
        orderRepository.finishTreatment()
        assertEquals(OrderStatus.COMPLETED_PAYMENT, orderRepository.activeOrder.value?.status)

        // Step 7: Confirm Payment Settlement with Tip
        val initialBalance = therapistRepository.therapistProfile.value.mainBalance
        val tip = 25000L
        orderRepository.confirmPaymentAndSettle(tip)
        assertEquals(OrderStatus.REVIEW_SUBMITTED, orderRepository.activeOrder.value?.status)
        assertTrue("Balance should increase after settlement", therapistRepository.therapistProfile.value.mainBalance > initialBalance)

        // Step 8: Return Home
        orderRepository.finishOrderAndReturnHome()
        assertEquals(null, orderRepository.activeOrder.value)
        assertEquals(DutyStatus.ONLINE, therapistRepository.therapistProfile.value.dutyStatus)
    }
}
