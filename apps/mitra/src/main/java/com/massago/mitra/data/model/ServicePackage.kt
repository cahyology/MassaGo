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
            id = "SRV-TRAD-90",
            name = "Pijat Tradisional Jawa",
            category = "Tradisional",
            durationMinutes = 90,
            basePrice = 160000L,
            description = "Pijat relaksasi tubuh menyeluruh menggunakan minyak zaitun hangat herbal.",
            requiredEquipment = listOf("Minyak Zaitun / Aromaterapi", "Matras Portable", "Handuk Bersih", "Kain Penutup")
        ),
        ServicePackage(
            id = "SRV-TRAD-KEROK-90",
            name = "Pijat Tradisional + Kerokan",
            category = "Tradisional",
            durationMinutes = 90,
            basePrice = 185000L,
            description = "Kombinasi pijatan tubuh pereda masuk angin & pegal linu dengan kerokan higienis.",
            requiredEquipment = listOf("Minyak Hangat", "Koin Kerokan Steril", "Handuk Hangat")
        ),
        ServicePackage(
            id = "SRV-REFL-60",
            name = "Refleksi Kaki & Tangan",
            category = "Refleksi",
            durationMinutes = 60,
            basePrice = 110000L,
            description = "Tekanan titik saraf akupresur telapak kaki dan tangan untuk melancarkan sirkulasi darah.",
            requiredEquipment = listOf("Krim Refleksi Herbal", "Stick Kayu Akupresur", "Handuk Rendam")
        ),
        ServicePackage(
            id = "SRV-DEEP-90",
            name = "Deep Tissue & Sport Massage",
            category = "Kebugaran",
            durationMinutes = 90,
            basePrice = 210000L,
            description = "Tekanan kuat untuk melepas ketegangan simpul otot kronis dan pemulihan setelah olahraga.",
            requiredEquipment = listOf("Balsem Otot Non-Lengket", "Essential Oil Peppermint", "Handuk")
        ),
        ServicePackage(
            id = "SRV-SCRUB-120",
            name = "Lulur & Body Scrub Spa",
            category = "Spa & Perawatan",
            durationMinutes = 120,
            basePrice = 245000L,
            description = "Pijat relaksasi dilanjutkan dengan scrub lulur rempah organik untuk mengangkat sel kulit mati.",
            requiredEquipment = listOf("Scrub Organik (Bengkoang/Kopi)", "Alas Plastik Higienis", "Lotion Pelembap", "Spons Mandi")
        ),
        ServicePackage(
            id = "SRV-BEKAM-60",
            name = "Bekam Kering & Relaksasi",
            category = "Kesehatan",
            durationMinutes = 60,
            basePrice = 150000L,
            description = "Terapi bekam angin steril untuk melancarkan peredaran darah dan meredakan letih.",
            requiredEquipment = listOf("Set Kop Bekam Steril", "Alkohol Swab Sanitasi", "Minyak Habbatussauda")
        ),
        ServicePackage(
            id = "SRV-PRENATAL-60",
            name = "Pijat Relaksasi Ibu Hamil",
            category = "Khusus",
            durationMinutes = 60,
            basePrice = 175000L,
            description = "Pijatan lembut khusus ibu hamil oleh terapis bersertifikasi aman untuk meredakan nyeri pinggang.",
            requiredEquipment = listOf("Minyak Kelapa Murni (VCO)", "Bantal Pendukung Posisi", "Kain Lembut")
        )
    )
}
