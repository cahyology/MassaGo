package com.massago.mitra.data.model

data class ServicePackage(
    val id: String,
    val name: String,
    val category: String,
    val durationMinutes: Int,
    val basePrice: Long,
    val therapistCommissionRate: Double = 0.80, // 80% to therapist
    val description: String,
    val requiredEquipment: List<String>,
    val iconName: String = "spa"
) {
    val therapistShare: Long get() = (basePrice * therapistCommissionRate).toLong()
}

object PredefinedServices {
    val ALL_SERVICES = listOf(
        ServicePackage(
            id = "SRV-MASSAGO-01-60",
            name = "Pijat Tradisional & Kebugaran Keluarga",
            category = "Kebugaran & Relaksasi",
            durationMinutes = 60,
            basePrice = 100000L,
            description = "Pijat tradisional seluruh tubuh untuk meredakan pegal linu dan memulihkan kebugaran tubuh secara profesional.",
            requiredEquipment = listOf("Minyak Zaitun / Aromaterapi", "Matras Portable", "Handuk Bersih", "Kain Penutup Higienis")
        ),
        ServicePackage(
            id = "SRV-MASSAGO-01-90",
            name = "Pijat Tradisional & Kebugaran Keluarga",
            category = "Kebugaran & Relaksasi",
            durationMinutes = 90,
            basePrice = 140000L,
            description = "Pijat tradisional relaksasi menyeluruh 90 menit untuk meredakan simpul otot kaku dan melancarkan sirkulasi darah.",
            requiredEquipment = listOf("Minyak Zaitun / Aromaterapi", "Matras Portable", "Handuk Bersih", "Kain Penutup Higienis")
        ),
        ServicePackage(
            id = "SRV-MASSAGO-01-120",
            name = "Pijat Tradisional & Kebugaran Keluarga",
            category = "Kebugaran & Relaksasi",
            durationMinutes = 120,
            basePrice = 180000L,
            description = "Pijat kebugaran keluarga menyeluruh 120 menit untuk pemulihan tubuh maksimal dan kualitas tidur terbaik.",
            requiredEquipment = listOf("Minyak Zaitun / Aromaterapi", "Matras Portable", "Handuk Bersih", "Kain Penutup Higienis")
        )
    )
}
