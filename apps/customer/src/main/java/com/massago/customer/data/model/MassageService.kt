package com.massago.customer.data.model

data class DurationOption(
    val minutes: Int,
    val price: Long,
    val isPopular: Boolean = false
)

data class AromaOption(
    val id: String,
    val name: String,
    val description: String,
    val extraFee: Long = 0L,
    val iconEmoji: String = "🌿"
)

data class FocusAreaOption(
    val id: String,
    val label: String,
    val isSelectedByDefault: Boolean = false
)

enum class PressureLevel(val label: String, val description: String) {
    SOFT("Lembut", "Pijatan ringan untuk relaksasi murni & menghilangkan stres"),
    MEDIUM("Sedang", "Tekanan seimbang ideal untuk melepas pegal sehari-hari"),
    STRONG("Kuat", "Tekanan dalam untuk mengatasi simpul otot kaku & pegal kronis")
}

data class MassageService(
    val id: String,
    val name: String,
    val category: String,
    val rating: Double = 4.95,
    val reviewCount: Int = 1240,
    val shortDescription: String,
    val fullDescription: String,
    val benefits: List<String>,
    val durations: List<DurationOption>,
    val tag: String = "Terlaris",
    val iconEmoji: String = "💆‍♂️"
) {
    val startingPrice: Long get() = durations.minOf { it.price }
}

object CustomerPredefinedServices {
    val AVAILABLE_AROMAS = listOf(
        AromaOption("aroma-olive", "Minyak Zaitun Murni", "Melembapkan kulit & melancarkan pijatan tanpa rasa lengket", 0L, "🫒"),
        AromaOption("aroma-lavender", "Lavender Essential Oil", "Menenangkan pikiran, membantu tidur pulas & meredakan stres", 15000L, "💜"),
        AromaOption("aroma-herbal", "Rempah Herbal Hangat", "Ekstrak jahe & serai untuk meredakan masuk angin dan rasa dingin", 15000L, "🌿"),
        AromaOption("aroma-vco", "Virgin Coconut Oil (VCO)", "Minyak kelapa murni organik sangat lembut untuk kulit sensitif", 10000L, "🥥")
    )

    val FOCUS_AREAS = listOf(
        FocusAreaOption("focus-shoulders", "Pundak & Leher", true),
        FocusAreaOption("focus-back", "Punggung Bawah", true),
        FocusAreaOption("focus-legs", "Kaki & Betis", false),
        FocusAreaOption("focus-arms", "Lengan & Tangan", false),
        FocusAreaOption("focus-head", "Kepala & Pelipis", false),
        FocusAreaOption("focus-full", "Seluruh Tubuh", true)
    )

    val SERVICES = listOf(
        MassageService(
            id = "SRV-MASSAGO-01",
            name = "Pijat Tradisional & Kebugaran Keluarga",
            category = "Kebugaran & Relaksasi",
            rating = 4.98,
            reviewCount = 3820,
            shortDescription = "Pijat tradisional seluruh tubuh untuk meredakan pegal linu, melancarkan sirkulasi darah, dan memulihkan kebugaran tubuh secara profesional.",
            fullDescription = "Layanan pemijatan keluarga profesional langsung ke lokasi Anda. Menggabungkan teknik kombinasi urut tradisional, peregangan relaksasi, dan penekanan titik simpul otot kaku dengan minyak aromaterapi alami berkualitas tinggi.",
            benefits = listOf(
                "Melancarkan sirkulasi darah & metabolisme",
                "Meredakan pegal linu & simpul otot kaku",
                "Meningkatkan kualitas tidur & kebugaran",
                "Terapis profesional bersertifikasi resmi & SOP higienis"
            ),
            durations = listOf(
                DurationOption(60, 100000L),
                DurationOption(90, 140000L, isPopular = true),
                DurationOption(120, 180000L)
            ),
            tag = "Layanan Utama",
            iconEmoji = "💆‍♂️"
        )
    )
}
