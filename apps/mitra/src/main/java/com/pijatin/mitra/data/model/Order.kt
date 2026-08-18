package com.pijatin.mitra.data.model

enum class OrderStatus {
    IDLE,
    INCOMING,
    ACCEPTED_ON_THE_WAY,
    ARRIVED_AT_LOCATION,
    SANITATION_AND_PREP,
    TREATMENT_IN_PROGRESS,
    COMPLETED_PAYMENT,
    REVIEW_SUBMITTED,
    CANCELLED
}

enum class ChecklistItemType {
    HANDS_SANITIZED,
    MAT_COVER_REPLACED,
    OIL_AROMA_CONFIRMED,
    PRESSURE_CHECKED
}

enum class PaymentMethod(val label: String) {
    CASH("Tunai di Tempat"),
    DIGITAL_WALLET("PijatIn Pay (Non-Tunai)")
}

data class ClientInfo(
    val id: String,
    val name: String,
    val phone: String,
    val gender: String, // "Pria" / "Wanita"
    val avatarUrl: String = "",
    val address: String,
    val addressNotes: String,
    val distanceKm: Double,
    val travelEstimateMinutes: Int,
    val latitude: Double = -6.2088,
    val longitude: Double = 106.8456
)

data class Order(
    val id: String,
    val client: ClientInfo,
    val servicePackage: ServicePackage,
    val status: OrderStatus = OrderStatus.INCOMING,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val travelAllowance: Long = 15000L,
    val tipAmount: Long = 0L,
    val clientNotes: String = "Fokus area punggung atas & pundak yang tegang. Tekanan sedang cenderung kuat.",
    val isOilProvidedByCustomer: Boolean = false,
    
    // Treatment timer state
    val totalTreatmentSeconds: Int = 90 * 60,
    val remainingTreatmentSeconds: Int = 90 * 60,
    val isTimerRunning: Boolean = false,
    val selectedAmbientSound: String = "Seruling Bambu Spa",
    
    // Sanitation checklist state
    val isHandsSanitized: Boolean = false,
    val isMatCoverReplaced: Boolean = false,
    val isOilAromaConfirmed: Boolean = false,
    val isPressurePreferenceChecked: Boolean = false,
    
    // Timestamps & Settlement
    val createdAtMillis: Long = System.currentTimeMillis(),
    val acceptedAtMillis: Long? = null,
    val startedAtMillis: Long? = null,
    val completedAtMillis: Long? = null,
    val incomingTimeoutSeconds: Int = 30
) {
    val subtotal: Long get() = servicePackage.basePrice
    val platformFee: Long get() = (servicePackage.basePrice * (1.0 - servicePackage.therapistCommissionRate)).toLong()
    val therapistNetEarnings: Long get() = servicePackage.therapistShare + travelAllowance + tipAmount
    val totalCustomerBill: Long get() = subtotal + travelAllowance + tipAmount
    val isPrepComplete: Boolean get() = isHandsSanitized && isMatCoverReplaced && isOilAromaConfirmed && isPressurePreferenceChecked
}
