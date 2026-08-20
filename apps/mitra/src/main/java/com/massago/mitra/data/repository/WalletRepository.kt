package com.massago.mitra.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.massago.mitra.MassaGoApp
import com.massago.mitra.data.model.TransactionType
import com.massago.mitra.data.model.WalletTransaction
import com.massago.mitra.data.network.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class WalletRepository private constructor(
    private val therapistRepository: TherapistRepository = TherapistRepository.instance
) {
    private val gson = Gson()
    private val prefs = try {
        MassaGoApp.instance.getSharedPreferences("MITRA_WALLET_PREFS", Context.MODE_PRIVATE)
    } catch (_: Exception) {
        null
    }

    private val _transactions = MutableStateFlow<List<WalletTransaction>>(loadPersistedTransactions())
    val transactions: StateFlow<List<WalletTransaction>> = _transactions.asStateFlow()

    private fun loadPersistedTransactions(): List<WalletTransaction> {
        val p = prefs ?: return emptyList()
        val json = p.getString("WALLET_TRANSACTIONS_JSON", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<WalletTransaction>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persistTransactions(list: List<WalletTransaction>) {
        prefs?.edit()?.putString("WALLET_TRANSACTIONS_JSON", gson.toJson(list))?.apply()
    }

    fun syncWithCompletedOrders(ordersList: List<Map<String, Any>>, defaultCommPercent: Int) {
        val newTxList = mutableListOf<WalletTransaction>()
        val existingManualTx = _transactions.value.filter { 
            it.type == TransactionType.WITHDRAWAL || it.type == TransactionType.DEPOSIT_TOPUP 
        }

        ordersList.forEach { orderMap ->
            val status = orderMap["status"]?.toString() ?: ""
            val isDone = status == "COMPLETED" || status == "COMPLETED_PAYMENT" || status == "REVIEW_SUBMITTED" || status == "FINISHED"
            if (isDone) {
                val orderId = orderMap["id"]?.toString() ?: "ORD"
                val serviceName = orderMap["service_name"]?.toString() ?: "Layanan Pijat"
                val custName = orderMap["customer_name"]?.toString() ?: "Pelanggan"
                val rawPrice = when (val p = orderMap["total_price"]) {
                    is Number -> p.toLong()
                    is String -> p.toDoubleOrNull()?.toLong() ?: 0L
                    else -> 0L
                }
                val orderRate = when (val r = orderMap["commission_rate"]) {
                    is Number -> r.toInt()
                    is String -> r.toIntOrNull() ?: defaultCommPercent
                    else -> defaultCommPercent
                }
                val orderMitraPercent = 100 - orderRate
                val netEarning = (rawPrice * (orderMitraPercent / 100.0)).toLong()
                val platformFee = (rawPrice * (orderRate / 100.0)).toLong()

                val createdAt = SupabaseClient.parseIsoOrEpochMillis(orderMap["created_at"])

                // Order Payout Earning
                newTxList.add(
                    WalletTransaction(
                        id = "TX-$orderId",
                        type = TransactionType.ORDER_PAYOUT,
                        amount = netEarning,
                        orderId = orderId,
                        title = "Pendapatan Sesi: $serviceName",
                        description = "Bagi hasil ${orderMitraPercent}% ($custName) • ID #$orderId",
                        timestampMillis = createdAt
                    )
                )

                // Platform Commission Deduction
                newTxList.add(
                    WalletTransaction(
                        id = "COMM-$orderId",
                        type = TransactionType.PLATFORM_COMMISSION,
                        amount = platformFee,
                        orderId = orderId,
                        title = "Potongan Komisi Platform ($orderRate%)",
                        description = "Dipotong otomatis dari deposit • ID #$orderId",
                        timestampMillis = createdAt + 50
                    )
                )
            }
        }

        val allCombined = (newTxList + existingManualTx).distinctBy { it.id }.sortedByDescending { it.timestampMillis }
        _transactions.value = allCombined
        persistTransactions(allCombined)
    }

    fun recordOrderPayout(orderId: String, packageName: String, therapistNetEarning: Long, tip: Long, platformFee: Long) {
        val now = System.currentTimeMillis()
        val newTxList = mutableListOf<WalletTransaction>()
        val commPercent = therapistRepository.platformCommissionPercent.value
        val mitraPercent = 100 - commPercent

        // Main order earning
        newTxList.add(
            WalletTransaction(
                id = "TX-" + (if (orderId.isNotBlank()) orderId else UUID.randomUUID().toString().substring(0, 8).uppercase()),
                type = TransactionType.ORDER_PAYOUT,
                amount = therapistNetEarning - tip,
                orderId = orderId,
                title = "Pendapatan Sesi: $packageName",
                description = "Bagi hasil ${mitraPercent}% + Tunjangan transport • ID #$orderId",
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
                    description = "Apresiasi layanan berkualitas • ID #$orderId",
                    timestampMillis = now + 100
                )
            )
        }

        // Platform fee record
        newTxList.add(
            WalletTransaction(
                id = "COMM-" + (if (orderId.isNotBlank()) orderId else UUID.randomUUID().toString().substring(0, 8).uppercase()),
                type = TransactionType.PLATFORM_COMMISSION,
                amount = platformFee,
                orderId = orderId,
                title = "Potongan Komisi Aplikasi (${commPercent}%)",
                description = "Dipotong dari saldo deposit mitra • ID #$orderId",
                timestampMillis = now + 200
            )
        )

        _transactions.update { current -> 
            val updated = (newTxList + current).distinctBy { it.id }
            persistTransactions(updated)
            updated 
        }
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
                description = "No. Rek/E-Wallet: $accountNumber (Status: Sukses)",
                timestampMillis = System.currentTimeMillis()
            )
            _transactions.update { current -> 
                val updated = listOf(tx) + current
                persistTransactions(updated)
                updated 
            }
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
        _transactions.update { current -> 
            val updated = listOf(tx) + current
            persistTransactions(updated)
            updated 
        }
    }

    companion object {
        val instance: WalletRepository by lazy { WalletRepository() }
    }
}
