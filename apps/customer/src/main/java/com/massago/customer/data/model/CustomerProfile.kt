package com.massago.customer.data.model

data class SavedAddress(
    val id: String,
    val title: String, // "Rumah", "Apartemen", "Kantor", "Hotel"
    val fullAddress: String,
    val note: String,
    val isPrimary: Boolean = false,
    val iconEmoji: String = "📍",
    val latitude: Double = -6.2088,
    val longitude: Double = 106.8456
)

data class CustomerProfile(
    val id: String = "",
    val name: String = "Pelanggan MassaGo",
    val phone: String = "",
    val email: String = "",
    val walletBalance: Long = 0L,
    val savedAddresses: List<SavedAddress> = emptyList()
)
