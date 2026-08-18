package com.massago.customer.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.massago.customer.data.model.CustomerLocation
import com.massago.customer.data.model.CustomerOrder
import com.massago.customer.data.model.CustomerPredefinedServices
import com.massago.customer.data.model.CustomerProfile
import com.massago.customer.data.model.MassageService
import com.massago.customer.data.model.PromoVoucher
import com.massago.customer.data.model.SavedAddress
import com.massago.customer.data.model.TherapistItem
import com.massago.customer.data.repository.CustomerOrderRepository
import com.massago.customer.data.repository.CustomerUserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

class CustomerHomeViewModel(
    private val userRepository: CustomerUserRepository = CustomerUserRepository.instance,
    private val orderRepository: CustomerOrderRepository = CustomerOrderRepository.instance
) : ViewModel() {

    val profile: StateFlow<CustomerProfile> = userRepository.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CustomerProfile())

    val currentLocation: StateFlow<CustomerLocation> = userRepository.currentLocation
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CustomerLocation())

    val activeOrder: StateFlow<CustomerOrder?> = orderRepository.activeOrder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val vouchers: StateFlow<List<PromoVoucher>> = orderRepository.availableVouchers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val services: StateFlow<List<MassageService>> = orderRepository.serviceCatalog
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CustomerPredefinedServices.SERVICES)
    val nearbyTherapists: List<TherapistItem> = emptyList()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Semua")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun selectAddress(address: SavedAddress) {
        userRepository.selectAddress(address)
    }

    fun addAndSelectAddress(address: SavedAddress) {
        userRepository.addAndSelectAddress(address)
    }

    fun filteredServices(allServices: List<MassageService> = services.value): List<MassageService> {
        val query = _searchQuery.value.trim().lowercase()
        val category = _selectedCategory.value
        return allServices.filter { service ->
            val matchesCategory = (category == "Semua" || service.category == category)
            val matchesQuery = query.isBlank() || service.name.lowercase().contains(query) || service.shortDescription.lowercase().contains(query)
            matchesCategory && matchesQuery
        }
    }
}
