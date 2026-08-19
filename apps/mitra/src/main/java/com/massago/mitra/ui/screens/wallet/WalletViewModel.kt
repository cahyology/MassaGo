package com.massago.mitra.ui.screens.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.massago.mitra.data.model.TherapistProfile
import com.massago.mitra.data.model.WalletTransaction
import com.massago.mitra.data.repository.TherapistRepository
import com.massago.mitra.data.repository.WalletRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class WalletViewModel(
    private val walletRepository: WalletRepository = WalletRepository.instance,
    private val therapistRepository: TherapistRepository = TherapistRepository.instance
) : ViewModel() {

    val therapistProfile: StateFlow<TherapistProfile> = therapistRepository.therapistProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TherapistProfile())

    val transactions: StateFlow<List<WalletTransaction>> = walletRepository.transactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        therapistRepository.refreshTodayMetricsAndHistory()
    }

    fun withdrawFunds(bankName: String, accountNumber: String, amount: Long): Boolean {
        return walletRepository.requestWithdrawal(bankName, accountNumber, amount)
    }

    fun topUpDeposit(amount: Long, paymentChannel: String) {
        walletRepository.topUpDeposit(amount, paymentChannel)
    }
}
