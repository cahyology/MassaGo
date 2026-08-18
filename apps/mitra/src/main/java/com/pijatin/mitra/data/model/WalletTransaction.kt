package com.pijatin.mitra.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TransactionType(val label: String, val isCredit: Boolean) {
    ORDER_PAYOUT("Hasil Sesi Terapi", true),
    CLIENT_TIP("Tip dari Pelanggan", true),
    INCENTIVE_BONUS("Bonus Target Harian", true),
    PLATFORM_COMMISSION("Potongan Komisi Aplikasi", false),
    WITHDRAWAL("Penarikan Saldo Rekening", false),
    DEPOSIT_TOPUP("Isi Ulang Saldo Deposit", true)
}

data class WalletTransaction(
    val id: String,
    val type: TransactionType,
    val amount: Long,
    val orderId: String? = null,
    val title: String,
    val description: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val status: String = "Berhasil"
) {
    val formattedDate: String get() {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
        return sdf.format(Date(timestampMillis))
    }
}
