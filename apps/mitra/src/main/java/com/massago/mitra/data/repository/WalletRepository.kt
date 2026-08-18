package com.massago.mitra.data.repository

import com.massago.mitra.data.model.TransactionType
import com.massago.mitra.data.model.WalletTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class WalletRepository private constructor(
    private val therapistRepository: TherapistRepository = TherapistRepository.instance
) {
    private val _transactions = MutableStateFlow<List<WalletTransaction>>(emptyList())
    val transactions: StateFlow<List<WalletTransaction>> = _transactions.asStateFlow()

    fun recordOrderPayout(orderId: String, packageName: String, therapistNetEarning: Long, tip: Long, platformFee: Long) {
        val now = System.currentTimeMillis()
        val newTxList = mutableListOf<WalletTransaction>()

        // Main order earning
        newTxList.add(
            WalletTransaction(
                id = "TX-" + UUID.randomUUID().toString().substring(0, 8).uppercase(),
                type = TransactionType.ORDER_PAYOUT,
                amount = therapistNetEarning - tip,
                orderId = orderId,
                title = "Pendapatan Sesi: $packageName",
                description = "Bagi hasil 80% + Tunjangan transport",
                timestampMillis = now
            )
        )

        // Tip if any
        if (tip > 0) {
            newTxList.add(
                WalletTransaction(
                    id = "TX-TIP-" + UUID.randomUUID().toString().substring(0, 6).uppercase(),
                    type = TransactionType.CLIENT_TIP,
                    amount = tip,
                    orderId = orderId,
                    title = "Tip dari Pelanggan",
                    description = "Apresiasi layanan berkualitas",
                    timestampMillis = now + 100
                )
            )
        }

        // Platform fee record
        newTxList.add(
            WalletTransaction(
                id = "TX-COMM-" + UUID.randomUUID().toString().substring(0, 6).uppercase(),
                type = TransactionType.PLATFORM_COMMISSION,
                amount = platformFee,
                orderId = orderId,
                title = "Potongan Komisi Aplikasi (20%)",
                description = "Dipotong dari saldo deposit mitra",
                timestampMillis = now + 200
            )
        )

        _transactions.update { current -> newTxList + current }
        therapistRepository.addEarnings(therapistNetEarning)
        therapistRepository.deductDeposit(platformFee)
    }

    fun requestWithdrawal(bankName: String, accountNumber: String, amount: Long): Boolean {
        if (therapistRepository.withdrawMainBalance(amount)) {
            val tx = WalletTransaction(
                id = "WD-" + UUID.randomUUID().toString().substring(0, 8).uppercase(),
                type = TransactionType.WITHDRAWAL,
                amount = amount,
                title = "Penarikan ke $bankName",
                description = "No. Rek/E-Wallet: $accountNumber (Status: Sukses Ditransfer)",
                timestampMillis = System.currentTimeMillis()
            )
            _transactions.update { current -> listOf(tx) + current }
            return true
        }
        return false
    }

    fun topUpDeposit(amount: Long, paymentChannel: String) {
        therapistRepository.addDeposit(amount)
        val tx = WalletTransaction(
            id = "DEP-" + UUID.randomUUID().toString().substring(0, 8).uppercase(),
            type = TransactionType.DEPOSIT_TOPUP,
            amount = amount,
            title = "Isi Ulang Saldo Deposit",
            description = "Melalui $paymentChannel (BCA Virtual Account / QRIS)",
            timestampMillis = System.currentTimeMillis()
        )
        _transactions.update { current -> listOf(tx) + current }
    }

    companion object {
        val instance: WalletRepository by lazy { WalletRepository() }
    }
}
