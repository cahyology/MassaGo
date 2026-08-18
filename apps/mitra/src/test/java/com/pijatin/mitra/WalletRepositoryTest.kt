package com.pijatin.mitra

import com.pijatin.mitra.data.model.TransactionType
import com.pijatin.mitra.data.repository.TherapistRepository
import com.pijatin.mitra.data.repository.WalletRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WalletRepositoryTest {

    private lateinit var walletRepository: WalletRepository
    private lateinit var therapistRepository: TherapistRepository

    @Before
    fun setUp() {
        walletRepository = WalletRepository.instance
        therapistRepository = TherapistRepository.instance
    }

    @Test
    fun testRecordOrderPayoutAndCommission() {
        val initialMainBalance = therapistRepository.therapistProfile.value.mainBalance
        val initialDeposit = therapistRepository.therapistProfile.value.depositBalance

        val earning = 150000L
        val tip = 20000L
        val commission = 30000L

        walletRepository.recordOrderPayout(
            orderId = "ORD-TEST-1",
            packageName = "Pijat Tradisional",
            therapistNetEarning = earning,
            tip = tip,
            platformFee = commission
        )

        assertEquals(initialMainBalance + earning, therapistRepository.therapistProfile.value.mainBalance)
        assertEquals(initialDeposit - commission, therapistRepository.therapistProfile.value.depositBalance)

        val latestTx = walletRepository.transactions.value.first()
        assertTrue(latestTx.type == TransactionType.PLATFORM_COMMISSION || latestTx.type == TransactionType.CLIENT_TIP || latestTx.type == TransactionType.ORDER_PAYOUT)
    }

    @Test
    fun testWithdrawFunds() {
        val currentBalance = therapistRepository.therapistProfile.value.mainBalance
        val withdrawAmount = 100000L

        val success = walletRepository.requestWithdrawal("Bank BCA", "8820192812", withdrawAmount)
        assertTrue(success)
        assertEquals(currentBalance - withdrawAmount, therapistRepository.therapistProfile.value.mainBalance)
    }
}
