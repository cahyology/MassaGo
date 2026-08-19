package com.massago.mitra.ui.screens.activeorder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.massago.mitra.data.model.ChecklistItemType
import com.massago.mitra.data.model.Order
import com.massago.mitra.data.repository.OrderRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ActiveOrderViewModel(
    private val orderRepository: OrderRepository = OrderRepository.instance
) : ViewModel() {

    val activeOrder: StateFlow<Order?> = orderRepository.activeOrder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun arriveAtLocation() {
        orderRepository.arriveAtLocation()
    }

    fun startSanitation() {
        orderRepository.startSanitationChecklist()
    }

    fun updateChecklist(type: ChecklistItemType, checked: Boolean) {
        orderRepository.updateChecklistItem(type, checked)
    }

    fun startTreatment() {
        orderRepository.startTreatment()
    }

    fun toggleTimer() {
        orderRepository.toggleTimer()
    }

    fun extendDuration(extraMinutes: Int) {
        orderRepository.extendTreatmentDuration(extraMinutes)
    }

    fun setAmbientSound(soundName: String) {
        orderRepository.setAmbientSound(soundName)
    }

    fun finishTreatment() {
        orderRepository.finishTreatment()
    }

    fun confirmPayment(tip: Long) {
        orderRepository.confirmPaymentAndSettle(tip)
    }

    fun submitCustomerRating(rating: Int, tags: List<String>, comment: String) {
        orderRepository.submitCustomerRating(rating, tags, comment)
    }

    fun refuseOrderForSafetyMismatch(reason: String, notes: String = "") {
        orderRepository.refuseOrderForSafetyMismatch(reason, notes)
    }

    fun finishOrderAndReturnHome() {
        orderRepository.finishOrderAndReturnHome()
    }
}
