package com.massago.customer.ui.screens.detail

import androidx.lifecycle.ViewModel
import com.massago.customer.data.model.AromaOption
import com.massago.customer.data.model.CustomerPredefinedServices
import com.massago.customer.data.model.MassageService
import com.massago.customer.data.model.PressureLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ServiceDetailViewModel : ViewModel() {

    private val _service = MutableStateFlow<MassageService?>(null)
    val service: StateFlow<MassageService?> = _service.asStateFlow()

    private val _selectedDuration = MutableStateFlow(90)
    val selectedDuration: StateFlow<Int> = _selectedDuration.asStateFlow()

    private val _selectedAroma = MutableStateFlow<AromaOption>(CustomerPredefinedServices.AVAILABLE_AROMAS[0])
    val selectedAroma: StateFlow<AromaOption> = _selectedAroma.asStateFlow()

    private val _selectedFocusAreas = MutableStateFlow<Set<String>>(setOf("Pundak & Leher", "Punggung Bawah"))
    val selectedFocusAreas: StateFlow<Set<String>> = _selectedFocusAreas.asStateFlow()

    private val _selectedPressure = MutableStateFlow(PressureLevel.MEDIUM)
    val selectedPressure: StateFlow<PressureLevel> = _selectedPressure.asStateFlow()

    private val _selectedGenderPreference = MutableStateFlow("Bebas (Siapa Saja)")
    val selectedGenderPreference: StateFlow<String> = _selectedGenderPreference.asStateFlow()

    fun loadService(serviceId: String) {
        val dynamicCatalog = com.massago.customer.data.repository.CustomerOrderRepository.instance.serviceCatalog.value
        val found = dynamicCatalog.find { it.id == serviceId }
            ?: CustomerPredefinedServices.SERVICES.find { it.id == serviceId }
            ?: CustomerPredefinedServices.SERVICES[0]
        _service.value = found
        _selectedDuration.value = found.durations.find { it.isPopular }?.minutes ?: found.durations.first().minutes
    }

    fun selectDuration(minutes: Int) {
        _selectedDuration.value = minutes
    }

    fun selectAroma(aroma: AromaOption) {
        _selectedAroma.value = aroma
    }

    fun toggleFocusArea(areaLabel: String) {
        _selectedFocusAreas.update { current ->
            if (current.contains(areaLabel)) {
                if (current.size > 1) current - areaLabel else current
            } else {
                current + areaLabel
            }
        }
    }

    fun selectPressure(pressure: PressureLevel) {
        _selectedPressure.value = pressure
    }

    fun selectGenderPreference(gender: String) {
        _selectedGenderPreference.value = gender
    }

    fun calculateCurrentPrice(): Long {
        val srv = _service.value ?: return 0L
        val durPrice = srv.durations.find { it.minutes == _selectedDuration.value }?.price ?: srv.startingPrice
        return durPrice + _selectedAroma.value.extraFee
    }
}
