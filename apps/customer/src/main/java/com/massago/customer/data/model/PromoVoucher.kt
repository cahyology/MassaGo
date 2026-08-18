package com.massago.customer.data.model

data class PromoVoucher(
    val code: String,
    val title: String,
    val description: String,
    val discountPercent: Int = 0,
    val discountFlat: Long = 0L,
    val maxDiscount: Long = 50000L,
    val minSpend: Long = 100000L,
    val expiryText: String = "Berlaku s/d 31 Des"
) {
    fun calculateDiscount(subtotal: Long): Long {
        if (subtotal < minSpend) return 0L
        val calculated = if (discountPercent > 0) {
            (subtotal * discountPercent / 100).coerceAtMost(maxDiscount)
        } else {
            discountFlat
        }
        return calculated
    }
}

object CustomerMockPromos {
    val VOUCHERS = listOf(
        PromoVoucher(
            code = "MASSAGOBARU",
            title = "Diskon 30% Pengguna Baru",
            description = "Potongan s/d Rp35.000 untuk pesanan pertama Anda",
            discountPercent = 30,
            maxDiscount = 35000L,
            minSpend = 100000L,
            expiryText = "Berlaku 7 hari lagi"
        ),
        PromoVoucher(
            code = "HEMATWEEKEND",
            title = "Potongan Langsung Rp 20.000",
            description = "Spesial relaksasi akhir pekan minimal order Rp150.000",
            discountFlat = 20000L,
            minSpend = 150000L,
            expiryText = "Berlaku Sabtu & Minggu"
        ),
        PromoVoucher(
            code = "SPALUXURY",
            title = "Diskon Rp 50.000 Paket Spa & Scrub",
            description = "Hemat Rp50.000 untuk paket perawatan 120 menit",
            discountFlat = 50000L,
            minSpend = 200000L,
            expiryText = "Berlaku s/d akhir bulan"
        )
    )
}
