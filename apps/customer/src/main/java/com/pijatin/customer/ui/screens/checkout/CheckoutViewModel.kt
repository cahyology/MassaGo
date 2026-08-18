package com.pijatin.customer.ui.screens.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pijatin.customer.data.model.CustomerLocation
import com.pijatin.customer.data.model.CustomerOrder
import com.pijatin.customer.data.model.CustomerPaymentMethod
import com.pijatin.customer.data.model.CustomerPredefinedServices
import com.pijatin.customer.data.model.CustomerProfile
import com.pijatin.customer.data.model.MassageService
import com.pijatin.customer.data.model.PressureLevel
import com.pijatin.customer.data.model.PromoVoucher
import com.pijatin.customer.data.repository.CustomerOrderRepository
import com.pijatin.customer.data.repository.CustomerUserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

class CheckoutViewModel(
    private val orderRepository: CustomerOrderRepository = CustomerOrderRepository.instance,
    private val userRepository: CustomerUserRepository = CustomerUserRepository.instance
) : ViewModel() {

    val profile: StateFlow<CustomerProfile> = userRepository.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CustomerProfile())

    val currentLocation: StateFlow<CustomerLocation> = userRepository.currentLocation
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CustomerLocation())

    val availableVouchers: StateFlow<List<PromoVoucher>> = orderRepository.availableVouchers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedVoucher = MutableStateFlow<PromoVoucher?>(null)
    val selectedVoucher: StateFlow<PromoVoucher?> = _selectedVoucher.asStateFlow()

    private val _selectedPaymentMethod = MutableStateFlow(CustomerPaymentMethod.PIJATIN_PAY)
    val selectedPaymentMethod: StateFlow<CustomerPaymentMethod> = _selectedPaymentMethod.asStateFlow()

    private val _isScheduledLater = MutableStateFlow(false)
    val isScheduledLater: StateFlow<Boolean> = _isScheduledLater.asStateFlow()

    private val _addressNote = MutableStateFlow("")
    val addressNote: StateFlow<String> = _addressNote.asStateFlow()

    fun initAddressNote(defaultNote: String) {
        if (_addressNote.value.isBlank()) {
            _addressNote.value = defaultNote
        }
    }

    fun setAddressNote(note: String) {
        _addressNote.value = note
    }

    fun setScheduledLater(isLater: Boolean) {
        _isScheduledLater.value = isLater
    }

    fun selectVoucher(voucher: PromoVoucher?) {
        _selectedVoucher.value = voucher
    }

    fun selectPaymentMethod(method: CustomerPaymentMethod) {
        _selectedPaymentMethod.value = method
    }

    fun placeOrder(
        serviceId: String,
        durationMinutes: Int,
        aromaId: String,
        focusAreas: List<String>,
        pressureLevel: PressureLevel,
        genderPreference: String
    ): CustomerOrder {
        val service = orderRepository.serviceCatalog.value.find { it.id == serviceId }
            ?: CustomerPredefinedServices.SERVICES.find { it.id == serviceId }
            ?: CustomerPredefinedServices.SERVICES[0]

        return orderRepository.createAndPlaceOrder(
            service = service,
            durationMinutes = durationMinutes,
            aromaId = aromaId,
            focusAreas = focusAreas,
            pressureLevel = pressureLevel,
            genderPreference = genderPreference,
            voucherCode = _selectedVoucher.value?.code,
            paymentMethod = _selectedPaymentMethod.value
        )
    }
}
