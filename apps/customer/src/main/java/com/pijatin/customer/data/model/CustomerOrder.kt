package com.pijatin.customer.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CustomerOrderStatus {
    SEARCHING_THERAPIST,
    THERAPIST_FOUND,
    THERAPIST_ON_THE_WAY,
    THERAPIST_ARRIVED,
    TREATMENT_IN_PROGRESS,
    TREATMENT_FINISHED_PAYMENT,
    ORDER_RATED,
    CANCELLED
}

enum class CustomerPaymentMethod(val label: String, val iconEmoji: String) {
    PIJATIN_PAY("PijatIn Pay (Saldo E-Wallet)", "💳"),
    QRIS("QRIS (GoPay, OVO, Dana, ShopeePay)", "📱"),
    VIRTUAL_ACCOUNT("Virtual Account Bank (BCA/Mandiri/BRI)", "🏦"),
    CASH_ON_DELIVERY("Tunai di Tempat (Bayar Selesai)", "💵")
}

data class CustomerLocation(
    val title: String = "Pilih Titik Lokasi",
    val address: String = "Ketuk untuk memilih lokasi jemput di peta",
    val notes: String = "",
    val latitude: Double = -6.2088,
    val longitude: Double = 106.8456
)

data class CustomerOrder(
    val id: String,
    val service: MassageService,
    val durationMinutes: Int = 90,
    val selectedAroma: AromaOption = CustomerPredefinedServices.AVAILABLE_AROMAS[0],
    val focusAreas: List<String> = listOf("Pundak & Leher", "Punggung Bawah"),
    val pressureLevel: PressureLevel = PressureLevel.MEDIUM,
    val therapistGenderPreference: String = "Bebas (Siapa Saja)",
    val location: CustomerLocation = CustomerLocation(),
    
    // Status & Assigned Therapist
    val status: CustomerOrderStatus = CustomerOrderStatus.SEARCHING_THERAPIST,
    val assignedTherapist: TherapistItem? = null,
    
    // Pricing Breakdown
    val basePrice: Long = 160000L,
    val travelFee: Long = 15000L,
    val hygieneKitFee: Long = 5000L,
    val discountAmount: Long = 0L,
    val appliedVoucher: PromoVoucher? = null,
    val tipAmount: Long = 0L,
    val paymentMethod: CustomerPaymentMethod = CustomerPaymentMethod.PIJATIN_PAY,
    
    // Live treatment session state
    val totalSeconds: Int = 90 * 60,
    val remainingSeconds: Int = 90 * 60,
    val isTimerRunning: Boolean = false,
    val selectedAmbientSound: String = "Seruling Bambu Spa",
    
    // Rating given by customer
    val ratingGiven: Int = 5,
    val reviewComment: String = "",
    val reviewTags: List<String> = emptyList(),
    
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    val subtotal: Long get() = basePrice + selectedAroma.extraFee
    val grandTotal: Long get() = (subtotal + travelFee + hygieneKitFee - discountAmount + tipAmount).coerceAtLeast(0L)
    
    val formattedCreatedAt: String get() {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
        return sdf.format(Date(createdAtMillis))
    }
}
