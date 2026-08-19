package com.massago.customer.ui.screens.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.massago.customer.data.model.CustomerLocation
import com.massago.customer.data.model.CustomerOrder
import com.massago.customer.data.model.CustomerPaymentMethod
import com.massago.customer.data.model.CustomerPredefinedServices
import com.massago.customer.data.model.CustomerProfile
import com.massago.customer.data.model.MassageService
import com.massago.customer.data.model.PressureLevel
import com.massago.customer.data.model.PromoVoucher
import com.massago.customer.data.repository.CustomerOrderRepository
import com.massago.customer.data.repository.CustomerUserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

import com.massago.customer.data.repository.TherapistAvailabilityStatus
import com.massago.customer.data.repository.TherapistLiveStatus
import com.massago.customer.data.network.SupabaseCustomerClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

    // Live Therapist Status for Repeat / Preferred Orders
    private val _therapistLiveStatus = MutableStateFlow<TherapistLiveStatus?>(null)
    val therapistLiveStatus: StateFlow<TherapistLiveStatus?> = _therapistLiveStatus.asStateFlow()

    private val _isSurchargeAccepted = MutableStateFlow(true)
    val isSurchargeAccepted: StateFlow<Boolean> = _isSurchargeAccepted.asStateFlow()

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

    fun setSurchargeAccepted(accepted: Boolean) {
        _isSurchargeAccepted.value = accepted
    }

    fun checkTherapistAvailability(
        therapistId: String,
        therapistNameFallback: String = "",
        custLat: Double = -7.7956,
        custLng: Double = 110.3695
    ) {
        if (therapistId.isBlank()) {
            _therapistLiveStatus.value = null
            return
        }

        _therapistLiveStatus.value = TherapistLiveStatus(
            therapistId = therapistId,
            name = therapistNameFallback.ifBlank { "Terapis Langganan" },
            isOnline = false,
            dutyStatus = "CHECKING",
            status = TherapistAvailabilityStatus.CHECKING
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val record = SupabaseCustomerClient.instance.fetchTherapistLiveRecord(therapistId)
                if (record == null) {
                    _therapistLiveStatus.value = TherapistLiveStatus(
                        therapistId = therapistId,
                        name = therapistNameFallback.ifBlank { "Terapis Langganan" },
                        isOnline = false,
                        dutyStatus = "OFFLINE",
                        status = TherapistAvailabilityStatus.OFFLINE
                    )
                    return@launch
                }

                val tName = (record["name"] as? String) ?: therapistNameFallback.ifBlank { "Terapis Langganan" }
                val isOnline = (record["is_online"] as? Boolean) == true
                val dutyStatus = (record["duty_status"] as? String) ?: if (isOnline) "ONLINE" else "OFFLINE"
                val tLat = (record["latitude"] as? Number)?.toDouble() ?: custLat
                val tLng = (record["longitude"] as? Number)?.toDouble() ?: custLng
                val maxRadius = ((record["max_radius_km"] as? Number)?.toDouble() ?: 10.0).coerceAtLeast(5.0)

                // 1. If therapist is offline
                if (!isOnline || dutyStatus.equals("OFFLINE", ignoreCase = true)) {
                    _therapistLiveStatus.value = TherapistLiveStatus(
                        therapistId = therapistId,
                        name = tName,
                        isOnline = false,
                        dutyStatus = "OFFLINE",
                        status = TherapistAvailabilityStatus.OFFLINE
                    )
                    return@launch
                }

                // 2. Check if therapist is currently BUSY handling another order
                val activeOrder = SupabaseCustomerClient.instance.fetchTherapistActiveOrder(therapistId)
                if (activeOrder != null) {
                    val durationMin = (activeOrder["duration_minutes"] as? Number)?.toInt() ?: 90
                    val createdAt = (activeOrder["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    val elapsedMin = ((System.currentTimeMillis() - createdAt) / 60000L).toInt().coerceAtLeast(0)
                    val remainingMin = (durationMin - elapsedMin).coerceIn(5, 120)

                    _therapistLiveStatus.value = TherapistLiveStatus(
                        therapistId = therapistId,
                        name = tName,
                        isOnline = true,
                        dutyStatus = "BUSY",
                        status = TherapistAvailabilityStatus.BUSY_HANDLING_OTHER,
                        busyRemainingMinutes = remainingMin,
                        activeOrderId = activeOrder["id"] as? String
                    )
                    return@launch
                }

                // 3. Distance & Out of Range Check
                val dLat = Math.toRadians(tLat - custLat)
                val dLng = Math.toRadians(tLng - custLng)
                val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                        Math.cos(Math.toRadians(custLat)) * Math.cos(Math.toRadians(tLat)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2)
                val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
                val distKm = ((6371.0 * c * 10).toInt() / 10.0).coerceAtLeast(0.1)

                if (distKm > maxRadius) {
                    val excessKm = distKm - maxRadius
                    val surcharge = (excessKm * 3000.0).toLong().coerceAtLeast(15000L)
                    _therapistLiveStatus.value = TherapistLiveStatus(
                        therapistId = therapistId,
                        name = tName,
                        isOnline = true,
                        dutyStatus = "ONLINE",
                        status = TherapistAvailabilityStatus.OUT_OF_RANGE,
                        distanceKm = distKm,
                        maxRadiusKm = maxRadius,
                        isOutOfRange = true,
                        extraTravelSurcharge = surcharge
                    )
                    return@launch
                }

                // 4. Online & Ready!
                _therapistLiveStatus.value = TherapistLiveStatus(
                    therapistId = therapistId,
                    name = tName,
                    isOnline = true,
                    dutyStatus = "ONLINE",
                    status = TherapistAvailabilityStatus.ONLINE_READY,
                    distanceKm = distKm,
                    maxRadiusKm = maxRadius
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun placeOrder(
        serviceId: String,
        durationMinutes: Int,
        aromaId: String,
        focusAreas: List<String>,
        pressureLevel: PressureLevel,
        genderPreference: String,
        recipientGender: String = "Wanita",
        preferredTherapistId: String? = null,
        isRepeatOrder: Boolean = false,
        scheduledTime: String? = null
    ): CustomerOrder {
        val service = orderRepository.serviceCatalog.value.find { it.id == serviceId }
            ?: CustomerPredefinedServices.SERVICES.find { it.id == serviceId }
            ?: CustomerPredefinedServices.SERVICES[0]

        val surcharge = if (_therapistLiveStatus.value?.isOutOfRange == true && _isSurchargeAccepted.value) {
            _therapistLiveStatus.value?.extraTravelSurcharge ?: 0L
        } else {
            0L
        }

        return orderRepository.createAndPlaceOrder(
            service = service,
            durationMinutes = durationMinutes,
            aromaId = aromaId,
            focusAreas = focusAreas,
            pressureLevel = pressureLevel,
            genderPreference = genderPreference,
            recipientGender = recipientGender,
            voucherCode = _selectedVoucher.value?.code,
            paymentMethod = _selectedPaymentMethod.value,
            preferredTherapistId = preferredTherapistId,
            isRepeatOrder = isRepeatOrder,
            scheduledTime = scheduledTime,
            extraTravelSurcharge = surcharge
        )
    }
}
