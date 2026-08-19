package com.massago.mitra.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.massago.mitra.data.model.DutyStatus
import com.massago.mitra.data.model.Order
import com.massago.mitra.data.model.OrderStatus
import com.massago.mitra.data.model.TherapistProfile
import com.massago.mitra.data.repository.OrderRepository
import com.massago.mitra.data.repository.TherapistRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val therapistRepository: TherapistRepository = TherapistRepository.instance,
    private val orderRepository: OrderRepository = OrderRepository.instance
) : ViewModel() {

    val therapistProfile: StateFlow<TherapistProfile> = therapistRepository.therapistProfile
        .stateIn(viewModelScope, SharingStarted.Eagerly, therapistRepository.therapistProfile.value)

    init {
        therapistRepository.refreshTodayMetricsAndHistory()
    }

    val activeOrder: StateFlow<Order?> = orderRepository.activeOrder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val incomingCountdown: StateFlow<Int> = orderRepository.incomingCountdownSeconds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 30)

    fun setDutyStatus(status: DutyStatus) {
        therapistRepository.setDutyStatus(status)
        if (status == DutyStatus.ONLINE) {
            orderRepository.startRealtimeOrderPolling()
        } else {
            orderRepository.stopRealtimeOrderPolling()
        }
    }

    fun acceptOrder() {
        orderRepository.acceptOrder()
    }

    fun declineOrder(reason: String = "Jarak terlalu jauh") {
        orderRepository.declineOrder(reason)
    }

    fun setMaxRadius(radiusKm: Int) {
        therapistRepository.setMaxRadiusKm(radiusKm)
    }

    fun setGenderPreference(gender: String) {
        therapistRepository.setPreferredClientGender(gender)
    }

    fun updatePreferences(radiusKm: Int, genderPref: String) {
        therapistRepository.setMaxRadiusKm(radiusKm)
        therapistRepository.setPreferredClientGender(genderPref)
    }

    fun checkPendingOrdersNow() {
        if (therapistRepository.therapistProfile.value.dutyStatus == DutyStatus.ONLINE) {
            orderRepository.startRealtimeOrderPolling()
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                orderRepository.checkForRealIncomingOrderDirect()
            }
        }
    }

    fun toggleAutoAccept(enabled: Boolean) {
        therapistRepository.toggleAutoAccept(enabled)
    }
}
