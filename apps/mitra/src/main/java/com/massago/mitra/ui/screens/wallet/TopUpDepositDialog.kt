package com.massago.mitra.ui.screens.wallet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.massago.mitra.data.network.SupabaseClient
import com.massago.mitra.ui.theme.EmeraldDark
import com.massago.mitra.ui.theme.EmeraldLight
import com.massago.mitra.ui.theme.EmeraldPrimary
import com.massago.mitra.ui.theme.TextMuted
import com.massago.mitra.ui.theme.TextSecondary
import java.text.NumberFormat
import java.util.Locale

@Composable
fun TopUpDepositDialog(
    onDismiss: () -> Unit,
    onConfirmTopUp: (amount: Long, channel: String) -> Unit
) {
    val context = LocalContext.current
    var selectedAmount by remember { mutableLongStateOf(100000L) }
    var selectedMethodTab by remember { mutableStateOf("BANK") } // "BANK" or "QRIS"

    val amounts = listOf(50000L, 100000L, 200000L, 500000L)
    val currencyFormat = NumberFormat.getNumberInstance(Locale("id", "ID"))

    var bankAccounts by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var platformSettings by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    var activePaymentUrl by remember { mutableStateOf<String?>(null) }

    if (activePaymentUrl != null) {
        com.massago.mitra.ui.components.PaymentWebViewDialog(
            paymentUrl = activePaymentUrl!!,
            onDismiss = {
                activePaymentUrl = null
                onDismiss()
            },
            onPaymentSuccess = {
                onConfirmTopUp(selectedAmount, if (selectedMethodTab == "BANK") "Virtual Account" else "QRIS")
                activePaymentUrl = null
                onDismiss()
            }
        )
    }

    LaunchedEffect(Unit) {
        val banks = SupabaseClient.instance.fetchBankAccounts()
        val settings = SupabaseClient.instance.fetchPlatformSettings()
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
                        text = "Isi Ulang Saldo Deposit",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Deposit dipotong 20% komisi saat menerima order tunai.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Pilih Nominal Top Up:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    amounts.forEach { amt ->
                        val isSelected = selectedAmount == amt
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) EmeraldPrimary else Color.White,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) EmeraldPrimary else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedAmount = amt }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${amt / 1000}rb",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Method Selector Tabs
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

                // Destination Info Card
                if (selectedMethodTab == "BANK") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val displayBanks = if (bankAccounts.isNotEmpty()) bankAccounts else listOf(
                            mapOf("bank_name" to "Bank Central Asia (BCA)", "account_number" to "8420891234", "account_holder" to "PT PIJATIN INDONESIA SEJAHTERA")
                        )
                        displayBanks.take(2).forEach { bank ->
                            val name = bank["bank_name"] as? String ?: "Bank"
                            val accNum = bank["account_number"] as? String ?: "-"
                            val holder = bank["account_holder"] as? String ?: "PIJATIN"

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
                                        Text(text = name, fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
                    }
                } else {
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
                                val profile = com.massago.mitra.data.repository.TherapistRepository.instance.therapistProfile.value
                                val dokuUrl = SupabaseClient.instance.createDokuDepositSession(
                                    therapistId = profile.id,
                                    therapistName = profile.name,
                                    amount = selectedAmount
                                )
                                isProcessing = false
                                if (dokuUrl != null) {
                                    activePaymentUrl = dokuUrl
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

                // WhatsApp Confirmation Button
                val adminWa = platformSettings["admin_whatsapp"] ?: "+6281234567890"
                Button(
                    onClick = {
                        try {
                            val cleanWa = adminWa.replace("+", "").replace("-", "").replace(" ", "")
                            val msg = "Halo Admin MassaGo, saya Mitra MassaGo ingin konfirmasi transfer top-up saldo deposit sebesar Rp ${currencyFormat.format(selectedAmount)}."
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://api.whatsapp.com/send?phone=$cleanWa&text=" + Uri.encode(msg))
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                        onConfirmTopUp(selectedAmount, if (selectedMethodTab == "BANK") "Transfer Bank" else "QRIS")
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Kirim Bukti Transfer ke Admin WA",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = {
                        onConfirmTopUp(selectedAmount, if (selectedMethodTab == "BANK") "Transfer Bank" else "QRIS")
                        Toast.makeText(context, "Top Up Deposit Rp ${currencyFormat.format(selectedAmount)} tercatat!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Konfirmasi Top Up Rp " + currencyFormat.format(selectedAmount),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
