package com.pijatin.customer.data.model

data class TherapistItem(
    val id: String,
    val name: String,
    val gender: String, // "Wanita" / "Pria"
    val rating: Double,
    val reviewCount: Int,
    val ordersCompleted: Int,
    val distanceKm: Double,
    val etaMinutes: Int,
    val certifications: List<String>,
    val avatarInitials: String,
    val isAvailableNow: Boolean = true,
    val specialtyBadge: String = "Master Therapist",
    val latitude: Double = -6.2088,
    val longitude: Double = 106.8456
)

object CustomerMockTherapists {
    val NEARBY_THERAPISTS = emptyList<TherapistItem>()
}
