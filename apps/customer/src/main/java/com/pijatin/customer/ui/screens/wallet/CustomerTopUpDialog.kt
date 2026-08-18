package com.pijatin.customer.ui.screens.wallet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pijatin.customer.data.network.SupabaseCustomerClient
import com.pijatin.customer.ui.theme.EmeraldDark
import com.pijatin.customer.ui.theme.EmeraldLight
import com.pijatin.customer.ui.theme.EmeraldPrimary
import com.pijatin.customer.ui.theme.TextMuted
import com.pijatin.customer.ui.theme.TextSecondary
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CustomerTopUpDialog(
    onTopUp: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedAmount by remember { mutableStateOf(200000L) }
    val amounts = listOf(50000L, 100000L, 200000L, 500000L)
    val currencyFormat = NumberFormat.getNumberInstance(Locale("id", "ID"))

    var bankAccounts by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var platformSettings by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var selectedMethodTab by remember { mutableStateOf("BANK") } // "BANK" or "QRIS"

    var activePaymentUrl by remember { mutableStateOf<String?>(null) }

    if (activePaymentUrl != null) {
        com.pijatin.customer.ui.components.PaymentWebViewDialog(
            paymentUrl = activePaymentUrl!!,
            onDismiss = {
                activePaymentUrl = null
                onDismiss()
            },
            onPaymentSuccess = {
                onTopUp(selectedAmount)
                activePaymentUrl = null
                onDismiss()
            }
        )
    }

    LaunchedEffect(Unit) {
        val banks = SupabaseCustomerClient.instance.fetchBankAccounts()
        val settings = SupabaseCustomerClient.instance.fetchPlatformSettings()
        bankAccounts = banks
        platformSettings = settings
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Isi Saldo PijatIn Pay",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Pilih Nominal Top Up:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Nominal Selector Grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    amounts.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { amt ->
                                val isSelected = amt == selectedAmount
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedAmount = amt },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) EmeraldPrimary else Color.White,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) EmeraldPrimary else Color(0xFFE2E8F0)
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Rp " + currencyFormat.format(amt),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Method Selector Tabs (Bank / QRIS)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedMethodTab = "BANK" },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedMethodTab == "BANK") EmeraldLight else Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedMethodTab == "BANK") EmeraldPrimary else Color(0xFFE2E8F0)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.AccountBalance, contentDescription = null, tint = EmeraldDark, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Transfer Bank", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldDark)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedMethodTab = "QRIS" },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedMethodTab == "QRIS") EmeraldLight else Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedMethodTab == "QRIS") EmeraldPrimary else Color(0xFFE2E8F0)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.QrCode, contentDescription = null, tint = EmeraldDark, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("QRIS Statis", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldDark)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Destination Info Card (Live from Supabase)
                if (selectedMethodTab == "BANK") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (bankAccounts.isEmpty()) {
                            // Fallback default
                            BankItemCard(bankName = "BCA", accNum = "8420891234", holder = "PT PIJATIN INDONESIA", context = context)
                        } else {
                            bankAccounts.take(2).forEach { bank ->
                                val name = bank["bank_name"] as? String ?: "Bank"
                                val accNum = bank["account_number"] as? String ?: "-"
                                val holder = bank["account_holder"] as? String ?: "PIJATIN"
                                BankItemCard(bankName = name, accNum = accNum, holder = holder, context = context)
                            }
                        }
                    }
                } else {
                    // QRIS Info
                    val merchant = platformSettings["qris_merchant_name"] ?: "PIJATIN INDONESIA"
                    val nmid = platformSettings["qris_nmid"] ?: "ID1020030040050"
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "Scan QRIS Resmi Platform", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(text = merchant, fontSize = 11.sp, color = EmeraldDark, fontWeight = FontWeight.Bold)
                            Text(text = "NMID: $nmid", fontSize = 10.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Dukung: BCA, Mandiri, GoPay, OVO, ShopeePay, Dana", fontSize = 9.5.sp, color = TextMuted)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Instant Online Payment
                val coroutineScope = rememberCoroutineScope()
                var isProcessing by remember { mutableStateOf(false) }

                Button(
                    onClick = {
                        if (isProcessing) return@Button
                        isProcessing = true
                        coroutineScope.launch {
                            try {
                                val midtransUrl = com.pijatin.customer.data.network.SupabaseCustomerClient.instance.createMidtransPaymentSession(
                                    orderId = "TOPUP-${System.currentTimeMillis()}",
                                    amount = selectedAmount,
                                    serviceName = "Top-Up Saldo PijatIn Pay",
                                    customerName = "Pelanggan PijatIn",
                                    customerPhone = "+6289680078070"
                                ) ?: com.pijatin.customer.data.network.SupabaseCustomerClient.instance.createDokuPaymentSession(
                                    orderId = "TOPUP-${System.currentTimeMillis()}",
                                    amount = selectedAmount,
                                    serviceName = "Top-Up Saldo PijatIn Pay",
                                    customerName = "Pelanggan PijatIn",
                                    customerPhone = "+6289680078070"
                                )
                                isProcessing = false
                                if (midtransUrl != null) {
                                    activePaymentUrl = midtransUrl
                                } else {
                                    Toast.makeText(context, "Gagal membuat sesi pembayaran, periksa koneksi internet.", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                isProcessing = false
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isProcessing) {
                        Text("Menyiapkan Pembayaran...", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    } else {
                        Text(
                            text = "Bayar Instan Rp " + currencyFormat.format(selectedAmount) + " (QRIS / VA)",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = {
                        onTopUp(selectedAmount)
                        Toast.makeText(context, "Top Up Rp ${currencyFormat.format(selectedAmount)} berhasil dikonfirmasi!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Konfirmasi Transfer Manual Rp " + currencyFormat.format(selectedAmount),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun BankItemCard(bankName: String, accNum: String, holder: String, context: Context) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = bankName, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text(text = accNum, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = EmeraldDark)
                Text(text = "a.n $holder", fontSize = 10.sp, color = TextSecondary)
            }
            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("No Rekening", accNum)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Nomor rekening $accNum disalin!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Salin", tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
            }
        }
    }
}
