package com.massago.customer.ui.screens.checkout

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Discount
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.massago.customer.data.model.CustomerPaymentMethod
import com.massago.customer.data.model.CustomerPredefinedServices
import com.massago.customer.data.model.PressureLevel
import com.massago.customer.data.model.PromoVoucher
import com.massago.customer.ui.theme.AmberGold
import com.massago.customer.ui.theme.EmeraldDark
import com.massago.customer.ui.theme.EmeraldDeep
import com.massago.customer.ui.theme.EmeraldLight
import com.massago.customer.ui.theme.EmeraldPrimary
import com.massago.customer.ui.theme.TextMuted
import com.massago.customer.ui.theme.TextSecondary
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    serviceId: String,
    durationMinutes: Int,
    aromaId: String,
    focusAreasStr: String,
    pressureStr: String,
    genderPreference: String,
    preferredTherapistId: String = "",
    preferredTherapistName: String = "",
    onNavigateBack: () -> Unit,
    onOrderPlaced: () -> Unit,
    viewModel: CheckoutViewModel = viewModel()
) {
    val service = com.massago.customer.data.repository.CustomerOrderRepository.instance.serviceCatalog.value.find { it.id == serviceId }
        ?: CustomerPredefinedServices.SERVICES.find { it.id == serviceId }
        ?: CustomerPredefinedServices.SERVICES[0]
    val selectedAroma = CustomerPredefinedServices.AVAILABLE_AROMAS.find { it.id == aromaId }
        ?: CustomerPredefinedServices.AVAILABLE_AROMAS[0]
    val focusAreas = focusAreasStr.split(",").filter { it.isNotBlank() }
    val pressureLevel = try {
        PressureLevel.valueOf(pressureStr)
    } catch (_: Exception) {
        PressureLevel.MEDIUM
    }

    val currentLocation by viewModel.currentLocation.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val availableVouchers by viewModel.availableVouchers.collectAsState()
    val selectedVoucher by viewModel.selectedVoucher.collectAsState()
    val selectedPaymentMethod by viewModel.selectedPaymentMethod.collectAsState()
    val isScheduledLater by viewModel.isScheduledLater.collectAsState()
    val addressNote by viewModel.addressNote.collectAsState()

    LaunchedEffect(currentLocation.notes) {
        viewModel.initAddressNote(currentLocation.notes)
    }

    var showVoucherSheet by remember { mutableStateOf(false) }
    var showPaymentSheet by remember { mutableStateOf(false) }
    var liveBanks by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var liveSettings by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    LaunchedEffect(Unit) {
        liveBanks = com.massago.customer.data.network.SupabaseCustomerClient.instance.fetchBankAccounts()
        liveSettings = com.massago.customer.data.network.SupabaseCustomerClient.instance.fetchPlatformSettings()
    }

    val currencyFormat = NumberFormat.getNumberInstance(Locale("id", "ID"))

    // Pricing calculation
    val basePrice = service.durations.find { it.minutes == durationMinutes }?.price ?: service.startingPrice
    val subtotal = basePrice + selectedAroma.extraFee
    val travelFee = 15000L
    val hygieneFee = 5000L
    val discount = selectedVoucher?.calculateDiscount(subtotal) ?: 0L
    val grandTotal = (subtotal + travelFee + hygieneFee - discount).coerceAtLeast(0L)

    var activePaymentUrl by remember { mutableStateOf<String?>(null) }

    if (activePaymentUrl != null) {
        com.massago.customer.ui.components.PaymentWebViewDialog(
            paymentUrl = activePaymentUrl!!,
            onDismiss = {
                activePaymentUrl = null
                onOrderPlaced()
            },
            onPaymentSuccess = {
                activePaymentUrl = null
                onOrderPlaced()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Konfirmasi Pemesanan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Pembayaran",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Rp " + currencyFormat.format(grandTotal),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldDark
                        )
                    }

                    val scope = rememberCoroutineScope()
                    val context = androidx.compose.ui.platform.LocalContext.current
                    var isSubmitting by remember { mutableStateOf(false) }

                    Button(
                        onClick = {
                            if (isSubmitting) return@Button
                            val orderId = "ORD-${System.currentTimeMillis()}"
                            viewModel.placeOrder(
                                serviceId = service.id,
                                durationMinutes = durationMinutes,
                                aromaId = selectedAroma.id,
                                focusAreas = focusAreas,
                                pressureLevel = pressureLevel,
                                genderPreference = genderPreference,
                                preferredTherapistId = preferredTherapistId.ifBlank { null },
                                isRepeatOrder = preferredTherapistId.isNotBlank(),
                                scheduledTime = if (isScheduledLater) "SCHEDULED_LATER" else null
                            )

                            if (selectedPaymentMethod == CustomerPaymentMethod.QRIS || selectedPaymentMethod == CustomerPaymentMethod.VIRTUAL_ACCOUNT) {
                                isSubmitting = true
                                scope.launch {
                                    try {
                                        val midtransUrl = com.massago.customer.data.network.SupabaseCustomerClient.instance.createMidtransPaymentSession(
                                            orderId = orderId,
                                            amount = grandTotal,
                                            serviceName = "${service.name} (${durationMinutes} Menit)",
                                            customerName = profile.name,
                                            customerPhone = profile.phone
                                        ) ?: com.massago.customer.data.network.SupabaseCustomerClient.instance.createDokuPaymentSession(
                                            orderId = orderId,
                                            amount = grandTotal,
                                            serviceName = "${service.name} (${durationMinutes} Menit)",
                                            customerName = profile.name,
                                            customerPhone = profile.phone
                                        )
                                        isSubmitting = false
                                        if (midtransUrl != null) {
                                            activePaymentUrl = midtransUrl
                                        } else {
                                            onOrderPlaced()
                                        }
                                    } catch (e: Exception) {
                                        isSubmitting = false
                                        onOrderPlaced()
                                    }
                                }
                            } else {
                                onOrderPlaced()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = if (isSubmitting) "Menyiapkan..." else when (selectedPaymentMethod) {
                                CustomerPaymentMethod.QRIS -> "Bayar via QRIS"
                                CustomerPaymentMethod.VIRTUAL_ACCOUNT -> "Bayar via Virtual Account"
                                CustomerPaymentMethod.PIJATIN_PAY -> "Bayar dg MassaGo Pay"
                                else -> "Pesan Terapis (Tunai)"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Preferred / Favorite Therapist Card
            if (preferredTherapistName.isNotBlank() || preferredTherapistId.isNotBlank()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFFFFFBEB),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, AmberGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(AmberGold.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "⭐", fontSize = 24.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Terapis Langganan Terpilih",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberGold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = AmberGold
                                    ) {
                                        Text(
                                            text = "VIP RE-ORDER",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = preferredTherapistName.ifBlank { "Terapis Pilihan Anda" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Pesanan akan langsung diprioritaskan ke terapis langganan Anda.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                    }
                }
            }

            // Service Summary Card
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = service.iconEmoji, fontSize = 28.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = service.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$durationMinutes Menit • ${selectedAroma.name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))

                        Text(
                            text = "• Fokus: ${focusAreas.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "• Tekanan: ${pressureLevel.label} | Terapis: $genderPreference",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Location & Address Card
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Lokasi Pemijatan",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentLocation.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = currentLocation.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = addressNote,
                            onValueChange = { viewModel.setAddressNote(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Petunjuk patokan / kamar untuk terapis", fontSize = 12.sp) },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            )
                        )
                    }
                }
            }

            // Schedule Option
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Waktu Pemesanan",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.setScheduledLater(false) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (!isScheduledLater) EmeraldPrimary else Color(0xFFF8FAFC),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (!isScheduledLater) EmeraldPrimary else Color(0xFFE2E8F0)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Pesan Sekarang",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!isScheduledLater) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Tiba ~20-30 mnt",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = if (!isScheduledLater) Color.White.copy(alpha = 0.9f) else TextSecondary
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.setScheduledLater(true) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isScheduledLater) EmeraldPrimary else Color(0xFFF8FAFC),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isScheduledLater) EmeraldPrimary else Color(0xFFE2E8F0)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Jadwalkan Nanti",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isScheduledLater) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Pilih Hari & Jam",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = if (isScheduledLater) Color.White.copy(alpha = 0.9f) else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Voucher & Promo Selector
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showVoucherSheet = true }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Discount,
                                contentDescription = null,
                                tint = AmberGold,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (selectedVoucher != null) "Voucher Digunakan: ${selectedVoucher?.code}" else "Gunakan Voucher / Kode Promo",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedVoucher != null) EmeraldDark else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (selectedVoucher != null) "Hemat Rp " + currencyFormat.format(discount) else "Pilih voucher hemat untuk potongan harga",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (selectedVoucher != null) AmberGold else TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Text(
                            text = if (selectedVoucher != null) "Ubah" else "Pilih",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }
                }
            }

            // Payment Method Selector
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPaymentSheet = true }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = selectedPaymentMethod.iconEmoji, fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Metode Pembayaran",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = selectedPaymentMethod.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = "Ganti",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }
                }
            }

            // Live Bank / QRIS Payment Details Card (if selected)
            if (selectedPaymentMethod == CustomerPaymentMethod.VIRTUAL_ACCOUNT || selectedPaymentMethod == CustomerPaymentMethod.QRIS) {
                item {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            if (selectedPaymentMethod == CustomerPaymentMethod.VIRTUAL_ACCOUNT) {
                                Text(
                                    text = "Rekening Resmi Tujuan Transfer:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldDark
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                val displayBanks = if (liveBanks.isNotEmpty()) liveBanks else listOf(
                                    mapOf("bank_name" to "Bank Central Asia (BCA)", "account_number" to "8420891234", "account_holder" to "PT PIJATIN INDONESIA SEJAHTERA")
                                )
                                displayBanks.take(2).forEach { b ->
                                    val bName = b["bank_name"] as? String ?: "Bank"
                                    val acc = b["account_number"] as? String ?: "-"
                                    val holder = b["account_holder"] as? String ?: "PIJATIN"
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(text = bName, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Text(text = acc, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = EmeraldDark)
                                            Text(text = "a.n $holder", fontSize = 10.sp, color = TextSecondary)
                                        }
                                        Button(
                                            onClick = {
                                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                val clip = android.content.ClipData.newPlainText("No Rek", acc)
                                                clipboard.setPrimaryClip(clip)
                                                android.widget.Toast.makeText(context, "No Rekening $acc disalin!", android.widget.Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldLight),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Salin", color = EmeraldDark, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else {
                                // QRIS
                                val merchant = liveSettings["qris_merchant_name"] ?: "PIJATIN INDONESIA"
                                val nmid = liveSettings["qris_nmid"] ?: "ID1020030040050"
                                Text(
                                    text = "Pembayaran QRIS Instan (Otomatis)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldDark
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "Mendukung: GoPay, OVO, ShopeePay, Dana, BCA & Semua M-Banking", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                                Text(text = "Kode QRIS dinamis akan langsung muncul saat Anda menekan tombol bayar di bawah.", fontSize = 10.sp, color = TextMuted)
                            }
                        }
                    }
                }
            }

            // Price Breakdown Card
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Rincian Pembayaran",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        PriceRow(label = "Biaya Layanan (${durationMinutes} mnt)", value = "Rp " + currencyFormat.format(basePrice))
                        if (selectedAroma.extraFee > 0) {
                            PriceRow(label = "Aromaterapi (${selectedAroma.name})", value = "Rp " + currencyFormat.format(selectedAroma.extraFee))
                        }
                        PriceRow(label = "Tunjangan Transportasi Terapis", value = "Rp " + currencyFormat.format(travelFee))
                        PriceRow(label = "Biaya Jaminan Higienitas & Matras", value = "Rp " + currencyFormat.format(hygieneFee))

                        if (discount > 0) {
                            PriceRow(
                                label = "Diskon Voucher (${selectedVoucher?.code})",
                                value = "-Rp " + currencyFormat.format(discount),
                                valueColor = AmberGold
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFE2E8F0))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Pembayaran",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Rp " + currencyFormat.format(grandTotal),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldDark
                            )
                        }
                    }
                }
            }
        }
    }

    // Voucher Picker Modal Sheet
    if (showVoucherSheet) {
        ModalBottomSheet(
            onDismissRequest = { showVoucherSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Pilih Voucher Promo",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                availableVouchers.forEach { voucher ->
                    val isApplied = selectedVoucher?.code == voucher.code
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                viewModel.selectVoucher(if (isApplied) null else voucher)
                                showVoucherSheet = false
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isApplied) EmeraldLight.copy(alpha = 0.5f) else Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isApplied) EmeraldPrimary else Color(0xFFE2E8F0)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${voucher.title} (${voucher.code})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = voucher.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            if (isApplied) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = EmeraldPrimary)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Payment Method Modal Sheet
    if (showPaymentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPaymentSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Pilih Metode Pembayaran",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                CustomerPaymentMethod.values().forEach { method ->
                    val isSelected = selectedPaymentMethod == method
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                viewModel.selectPaymentMethod(method)
                                showPaymentSheet = false
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) EmeraldLight.copy(alpha = 0.5f) else Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) EmeraldPrimary else Color(0xFFE2E8F0)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = method.iconEmoji, fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = method.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                if (method == CustomerPaymentMethod.PIJATIN_PAY) {
                                    Text(
                                        text = "Saldo Anda: Rp " + currencyFormat.format(profile.walletBalance),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = EmeraldDark,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = EmeraldPrimary)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun PriceRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}
