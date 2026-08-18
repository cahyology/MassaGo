package com.pijatin.customer.data.model

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
            id = "SRV-TRAD",
            name = "Pijat Tradisional Jawa",
            category = "Tradisional",
            rating = 4.98,
            reviewCount = 3820,
            shortDescription = "Pijatan relaksasi seluruh tubuh dengan teknik urut Jawa turun-temurun.",
            fullDescription = "Pijatan tradisional menggunakan teknik kombinasi urut, tekan, dan remas untuk melancarkan sirkulasi darah, melemaskan otot kaku, dan mengembalikan kesegaran tubuh setelah beraktivitas padat.",
            benefits = listOf(
                "Melancarkan peredaran darah",
                "Meredakan otot tegang & lelah",
                "Membantu meningkatkan kualitas tidur",
                "Menenangkan sistem saraf"
            ),
            durations = listOf(
                DurationOption(60, 120000L),
                DurationOption(90, 160000L, isPopular = true),
                DurationOption(120, 210000L)
            ),
            tag = "Paling Favorit",
            iconEmoji = "💆‍♂️"
        ),
        MassageService(
            id = "SRV-REFL",
            name = "Refleksi Kaki & Relaksasi",
            category = "Refleksi",
            rating = 4.94,
            reviewCount = 2150,
            shortDescription = "Titik akupresur telapak kaki dan tangan untuk memulihkan vitalitas.",
            fullDescription = "Terapi penekanan titik saraf pada zona refleksi telapak kaki dan tangan yang terhubung langsung ke organ tubuh, membantu melancarkan metabolisme dan meredakan pegal kaki secara instan.",
            benefits = listOf(
                "Meredakan ketegangan kaki & telapak",
                "Merangsang fungsi organ tubuh alami",
                "Mencegah kram dan bengkak",
                "Mengurangi rasa lelah setelah berjalan jauh"
            ),
            durations = listOf(
                DurationOption(60, 110000L, isPopular = true),
                DurationOption(90, 150000L),
                DurationOption(120, 190000L)
            ),
            tag = "Cepat Pulih",
            iconEmoji = "🦶"
        ),
        MassageService(
            id = "SRV-DEEP",
            name = "Deep Tissue & Sport Massage",
            category = "Kebugaran",
            rating = 4.96,
            reviewCount = 1840,
            shortDescription = "Tekanan intensif untuk simpul otot kaku setelah olahraga berat.",
            fullDescription = "Teknik pijatan berfokus pada lapisan jaringan otot yang lebih dalam untuk melepas simpul ketegangan kronis (trigger points) dan mempercepat pemulihan tubuh setelah olahraga berat.",
            benefits = listOf(
                "Memecah asam laktat & simpul otot",
                "Mempercepat pemulihan pasca olahraga",
                "Meningkatkan fleksibilitas sendi",
                "Mencegah risiko cedera berulang"
            ),
            durations = listOf(
                DurationOption(60, 150000L),
                DurationOption(90, 210000L, isPopular = true),
                DurationOption(120, 260000L)
            ),
            tag = "Bebas Pegal",
            iconEmoji = "💪"
        ),
        MassageService(
            id = "SRV-SCRUB",
            name = "Lulur & Body Scrub Spa",
            category = "Spa & Kulit",
            rating = 4.97,
            reviewCount = 1420,
            shortDescription = "Pijat relaksasi dipadu scrub rempah organik untuk kulit cerah & halus.",
            fullDescription = "Perawatan kecantikan & relaksasi tubuh menyeluruh diawali dengan pijatan lembut, dilanjutkan scrub lulur rempah bengkoang/kopi untuk mengangkat sel kulit mati dan menghaluskan kulit.",
            benefits = listOf(
                "Mengangkat sel kulit mati & kotoran",
                "Mencerahkan dan melembutkan kulit",
                "Memberikan sensasi spa mewah di rumah",
                "Menutrisi kulit dengan bahan alami"
            ),
            durations = listOf(
                DurationOption(90, 200000L),
                DurationOption(120, 245000L, isPopular = true)
            ),
            tag = "Spa Mewah",
            iconEmoji = "✨"
        ),
        MassageService(
            id = "SRV-BEKAM",
            name = "Bekam & Kerokan Higienis",
            category = "Kesehatan",
            rating = 4.93,
            reviewCount = 1670,
            shortDescription = "Pelepas masuk angin dan letih dengan peralatan steril higienis.",
            fullDescription = "Perpaduan pijat akupresur dengan terapi kerokan halus atau bekam kering menggunakan alat steril satu kali pakai untuk meredakan gejala flu, meriang, dan masuk angin berat.",
            benefits = listOf(
                "Meredakan meriang & masuk angin",
                "Membuang angin & racun tubuh",
                "Melonggarkan pernapasan & pundak",
                "Peralatan dijamin 100% steril"
            ),
            durations = listOf(
                DurationOption(60, 135000L),
                DurationOption(90, 185000L, isPopular = true)
            ),
            tag = "Pereda Masuk Angin",
            iconEmoji = "🍃"
        ),
        MassageService(
            id = "SRV-PRENATAL",
            name = "Pijat Relaksasi Ibu Hamil",
            category = "Khusus",
            rating = 4.99,
            reviewCount = 980,
            shortDescription = "Pijatan lembut khusus ibu hamil oleh terapis bersertifikasi resmi.",
            fullDescription = "Didesain dengan posisi menyamping yang nyaman dan aman bagi ibu hamil (usia kehamilan 16-32 minggu) untuk mengurangi nyeri pinggang, bengkak kaki, dan melancarkan sirkulasi darah.",
            benefits = listOf(
                "Aman & dilakukan terapis certified prenatal",
                "Meredakan nyeri punggung & panggul",
                "Mengurangi pembengkakan kaki & tangan",
                "Membantu relaksasi calon ibu"
            ),
            durations = listOf(
                DurationOption(60, 175000L, isPopular = true),
                DurationOption(90, 230000L)
            ),
            tag = "Certified Safe",
            iconEmoji = "🤰"
        )
    )
}
