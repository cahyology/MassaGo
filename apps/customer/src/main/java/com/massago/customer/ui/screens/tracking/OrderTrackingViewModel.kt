package com.massago.customer.ui.screens.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.massago.customer.data.model.CustomerOrder
import com.massago.customer.data.repository.CustomerOrderRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class OrderTrackingViewModel(
    private val orderRepository: CustomerOrderRepository = CustomerOrderRepository.instance
) : ViewModel() {

    val activeOrder: StateFlow<CustomerOrder?> = orderRepository.activeOrder

    fun startTreatment() {
        orderRepository.startTreatmentSession()
    }

    fun extendDuration(extraMinutes: Int) {
        orderRepository.extendTreatmentDuration(extraMinutes)
    }

    fun selectAmbientSound(soundName: String) {
        orderRepository.selectAmbientSound(soundName)
    }

    fun submitRating(rating: Int, comment: String, tags: List<String>, tip: Long) {
        orderRepository.submitRatingAndComplete(rating, tags, comment, tip)
    }

    fun cancelOrder(reason: String = "Dibatalkan oleh pelanggan") {
        orderRepository.cancelActiveOrder(reason)
    }

    fun clearActiveOrder() {
        orderRepository.clearActiveOrder()
    }
}
