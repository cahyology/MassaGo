package com.pijatin.mitra.data.model

enum class DutyStatus {
    ONLINE,
    OFFLINE,
    ON_DUTY_BUSY
}

enum class TherapistTier(val label: String, val bonusPercent: Int) {
    STANDARD("Mitra Reguler", 0),
    SILVER("Mitra Silver", 5),
    GOLD("Mitra Gold Master", 10)
}

data class TherapistProfile(
    val id: String = "",
    val name: String = "Mitra Terapis",
    val phone: String = "",
    val avatarUrl: String = "",
    val gender: String = "Pria",
    val rating: Double = 5.0,
    val totalOrdersCompleted: Int = 0,
    val acceptanceRate: Int = 100, // 100%
    val completionRate: Int = 100, // 100%
    val tier: TherapistTier = TherapistTier.STANDARD,
    val tierBadge: String = "Mitra Baru (Menunggu Verifikasi)",
    val isVerified: Boolean = false,
    val dutyStatus: DutyStatus = DutyStatus.OFFLINE,
    val autoAcceptOrders: Boolean = false,
    val maxRadiusKm: Int = 10,
    val preferredClientGender: String = "Semua", // Semua, Pria Saja, Wanita Saja
    val certifiedSpecialties: List<String> = listOf(
        "Pijat Tradisional",
        "Refleksi Kaki",
        "Deep Tissue & Sport Massage",
        "Lulur & Scrub Relaksasi",
        "Bekam & Kerokan Higienis"
    ),
    val activeSpecialties: Set<String> = setOf(
        "Pijat Tradisional",
        "Refleksi Kaki",
        "Deep Tissue & Sport Massage",
        "Lulur & Scrub Relaksasi",
        "Bekam & Kerokan Higienis"
    ),
    val mainBalance: Long = 0L, // Saldo Penghasilan
    val depositBalance: Long = 0L, // Saldo Deposit
    val todayEarnings: Long = 0L,
    val todayOrdersCount: Int = 0,
    val dailyTargetOrders: Int = 5,
    val dailyTargetBonus: Long = 50000L,
    val latitude: Double = -6.2088,
    val longitude: Double = 106.8456
)
