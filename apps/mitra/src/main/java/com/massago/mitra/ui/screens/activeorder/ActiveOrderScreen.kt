package com.massago.mitra.ui.screens.activeorder

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CleanHands
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.massago.mitra.data.model.ChecklistItemType
import com.massago.mitra.data.model.ClientInfo
import com.massago.mitra.data.model.Order
import com.massago.mitra.data.model.OrderStatus
import com.massago.mitra.data.model.PredefinedServices
import com.massago.mitra.ui.components.MapSimulationView
import com.massago.mitra.ui.theme.AmberDark
import com.massago.mitra.ui.theme.AmberGold
import com.massago.mitra.ui.theme.EmeraldDark
import com.massago.mitra.ui.theme.EmeraldLight
import com.massago.mitra.ui.theme.EmeraldPrimary
import com.massago.mitra.ui.theme.MassaGoMitraTheme
import com.massago.mitra.ui.theme.StatusAlertRed
import com.massago.mitra.ui.theme.StatusOnlineGreen
import com.massago.mitra.ui.theme.TextMuted
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import com.massago.mitra.data.repository.ChatRepository
import com.massago.mitra.ui.theme.EmeraldDark
import com.massago.mitra.ui.theme.EmeraldLight
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveOrderScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChat: () -> Unit,
    onSosClick: () -> Unit,
    viewModel: ActiveOrderViewModel = viewModel()
) {
    val context = LocalContext.current
    val activeOrder by viewModel.activeOrder.collectAsState()
    val unreadChatCount by ChatRepository.instance.unreadCount.collectAsState()
    var showMitraSosDialog by remember { mutableStateOf(false) }
    var graceSecondsRemaining by remember { mutableIntStateOf(900) } // 15 Menit

    val currentOrder = activeOrder

    androidx.compose.runtime.LaunchedEffect(currentOrder?.status) {
        if (currentOrder?.status == OrderStatus.ARRIVED_AT_LOCATION) {
            while (graceSecondsRemaining > 0) {
                kotlinx.coroutines.delay(1000)
                graceSecondsRemaining--
            }
        }
    }

    if (showMitraSosDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showMitraSosDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🚨", fontSize = 32.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Pusat Keselamatan & SOS Mitra",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Text(
                        text = "Gunakan tombol darurat jika Anda menemui ancaman kekerasan, pelecehan, atau kondisi medis darurat di lokasi klien.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Polisi & Ambulans
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:110"))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Text("🚓 Polisi 110", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:118"))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                        ) {
                            Text("🚑 Ambulans 118", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // WhatsApp Satgas MassaGo
                    OutlinedButton(
                        onClick = {
                            try {
                                val url = "https://wa.me/6281234567890?text=DARURAT%20SOS%20TERAPIS%20MassaGo%20ID%20Pesanan%20${currentOrder?.id}"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF059669))
                    ) {
                        Text("💬 Satgas MassaGo 24/7", color = Color(0xFF059669), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Kirim Signal SOS ke Server
                    Button(
                        onClick = {
                            com.massago.mitra.data.repository.OrderRepository.instance.triggerSosAlert("Panggilan Darurat Terapis dari Order #${currentOrder?.id}")
                            showMitraSosDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                    ) {
                        Text("PANCARKAN SINYAL KE SERVER", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    if (currentOrder == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Tidak ada pesanan aktif saat ini.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onNavigateBack,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Kembali ke Beranda")
                }
            }
        }
        return
    }

    val currencyFormat = NumberFormat.getNumberInstance(Locale("id", "ID"))

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.statusBars,
                title = {
                    Column {
                        Text(
                            text = when (currentOrder.status) {
                                OrderStatus.ACCEPTED_ON_THE_WAY -> "1. Menuju Lokasi Klien"
                                OrderStatus.ARRIVED_AT_LOCATION -> "2. Tiba di Lokasi"
                                OrderStatus.SANITATION_AND_PREP -> "3. Ceklis SOP Higienitas"
                                OrderStatus.TREATMENT_IN_PROGRESS -> "4. Sesi Terapi Berlangsung"
                                OrderStatus.COMPLETED_PAYMENT -> "5. Pembayaran & Struk"
                                OrderStatus.REVIEW_SUBMITTED -> "6. Selesai"
                                else -> "Status Pesanan"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Order ID: ${currentOrder.id}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showMitraSosDialog = true },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(StatusAlertRed.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "SOS",
                            tint = StatusAlertRed,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Stage 1 & 2: Route Navigation & On the way / Arrived
            if (currentOrder.status == OrderStatus.ACCEPTED_ON_THE_WAY || currentOrder.status == OrderStatus.ARRIVED_AT_LOCATION) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Map Simulation Area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        MapSimulationView(
                            modifier = Modifier.fillMaxSize(),
                            isOnline = true,
                            activeOrder = currentOrder
                        )

                        // Floating Navigate in Google Maps Button (Motorcycle Navigation mode=l)
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                                .clickable {
                                    try {
                                        val gmmIntentUri = Uri.parse("google.navigation:q=${currentOrder.client.latitude},${currentOrder.client.longitude}&mode=l")
                                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                        mapIntent.setPackage("com.google.android.apps.maps")
                                        context.startActivity(mapIntent)
                                    } catch (_: Exception) {
                                        try {
                                            val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${currentOrder.client.latitude},${currentOrder.client.longitude}&travelmode=two_wheeler")
                                            val webIntent = Intent(Intent.ACTION_VIEW, webUri)
                                            context.startActivity(webIntent)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 6.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Navigation,
                                    contentDescription = "Navigasi",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Buka Google Maps",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Client Card & Bottom Action Sheet
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 12.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            // Client Contact Bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = currentOrder.client.name.take(2).uppercase(),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 17.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = currentOrder.client.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Jarak: ${currentOrder.client.distanceKm} km (~${currentOrder.client.travelEstimateMinutes} mnt)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Quick Call & Chat Buttons
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IconButton(
                                        onClick = {
                                            val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${currentOrder.client.phone}"))
                                            context.startActivity(callIntent)
                                        },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Call,
                                            contentDescription = "Telepon",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }

                                    Box {
                                        IconButton(
                                            onClick = onNavigateToChat,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                                contentDescription = "Chat",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }
                                        if (unreadChatCount > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .align(Alignment.TopEnd)
                                                    .clip(CircleShape)
                                                    .background(Color.Red)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(14.dp))

                            // Address Details
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = StatusAlertRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = currentOrder.client.address,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.5.sp
                                    )
                                    if (currentOrder.client.addressNotes.isNotEmpty()) {
                                        Text(
                                            text = "Patokan: ${currentOrder.client.addressNotes}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Stage Actions (Gojek / Grab Step Action)
                            if (currentOrder.status == OrderStatus.ACCEPTED_ON_THE_WAY) {
                                Button(
                                    onClick = { viewModel.arriveAtLocation() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text(
                                        text = "SAYA SUDAH TIBA DI LOKASI",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp
                                    )
                                }
                            } else if (currentOrder.status == OrderStatus.ARRIVED_AT_LOCATION) {
                                val minutes = graceSecondsRemaining / 60
                                val seconds = graceSecondsRemaining % 60
                                val timeFormatted = String.format(Locale.US, "%02d:%02d", minutes, seconds)

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (graceSecondsRemaining > 0) Color(0xFFFEF3C7) else Color(0xFFFEE2E2),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (graceSecondsRemaining > 0) Color(0xFFF59E0B) else Color(0xFFEF4444)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(if (graceSecondsRemaining > 0) Color(0xFFFDE68A) else Color(0xFFFECACA)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (graceSecondsRemaining > 0) "⏱️" else "⚠️",
                                                fontSize = 20.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Batas Waktu Tunggu SOP",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = if (graceSecondsRemaining > 0) Color(0xFF92400E) else Color(0xFF991B1B)
                                                )
                                                Text(
                                                    text = timeFormatted,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 14.sp,
                                                    color = if (graceSecondsRemaining > 0) Color(0xFFB45309) else Color(0xFFDC2626)
                                                )
                                            }
                                            Text(
                                                text = if (graceSecondsRemaining > 0)
                                                    "Hubungi klien via chat/telepon. Waktu toleransi kehadiran terapis adalah 15 menit."
                                                else
                                                    "Waktu tunggu 15 menit telah habis. Anda berhak membatalkan order dengan kompensasi jika klien tidak merespon.",
                                                fontSize = 11.sp,
                                                color = if (graceSecondsRemaining > 0) Color(0xFF78350F) else Color(0xFF7F1D1D)
                                            )
                                        }
                                    }
                                }

                                Button(
                                    onClick = { viewModel.startSanitation() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusOnlineGreen)
                                ) {
                                    Icon(imageVector = Icons.Default.CleanHands, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "MULAI CEKLIS SOP & PERSIAPAN",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Stage 3: Sanitation Checklist SOP
            else if (currentOrder.status == OrderStatus.SANITATION_AND_PREP) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Prosedur Standar Higienitas (SOP)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Pastikan seluruh standar kebersihan terpenuhi sebelum memulai pijat.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    val checklist = listOf(
                        Triple(ChecklistItemType.HANDS_SANITIZED, "Cuci Tangan & Gunakan Hand Sanitizer", "Terapis membersihkan tangan hingga steril."),
                        Triple(ChecklistItemType.MAT_COVER_REPLACED, "Ganti Alas Matras Disposable 1x Pakai", "Gunakan alas higienis baru yang belum terpakai."),
                        Triple(ChecklistItemType.OIL_AROMA_CONFIRMED, "Konfirmasi Minyak Aromaterapi", "Pastikan aroma pilihan sesuai dengan preferensi klien."),
                        Triple(ChecklistItemType.PRESSURE_CHECKED, "Tanyakan Preferensi Tekanan Awal", "Sesuaikan tingkat tekanan pijat (Lembut/Sedang/Kuat).")
                    )

                    checklist.forEach { (type, title, desc) ->
                        val isChecked = when (type) {
                            ChecklistItemType.HANDS_SANITIZED -> currentOrder.isHandsSanitized
                            ChecklistItemType.MAT_COVER_REPLACED -> currentOrder.isMatCoverReplaced
                            ChecklistItemType.OIL_AROMA_CONFIRMED -> currentOrder.isOilAromaConfirmed
                            ChecklistItemType.PRESSURE_CHECKED -> currentOrder.isPressurePreferenceChecked
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable { viewModel.updateChecklist(type, !isChecked) },
                            shape = RoundedCornerShape(18.dp),
                            color = if (isChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isChecked) {
                                        Text("✓", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.5.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    val allChecked = currentOrder.isPrepComplete

                    Button(
                        onClick = { viewModel.startTreatment() },
                        enabled = allChecked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = if (allChecked) "MULAI TIMER SESI PIJAT" else "LENGKAPI SEMUA CEKLIS DI ATAS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Stage 4: Live Treatment Countdown Timer & Ambient Sound
            else if (currentOrder.status == OrderStatus.TREATMENT_IN_PROGRESS) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentOrder.servicePackage.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Sesi berlangsung • ${currentOrder.client.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Big Circular Countdown Timer
                    val totalSec = currentOrder.totalTreatmentSeconds
                    val remainingSec = currentOrder.remainingTreatmentSeconds
                    val minutesLeft = remainingSec / 60
                    val secondsLeft = remainingSec % 60

                    Surface(
                        modifier = Modifier.size(240.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(4.dp, MaterialTheme.colorScheme.primary),
                        shadowElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = String.format("%02d:%02d", minutesLeft, secondsLeft),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 46.sp,
                                letterSpacing = (-1).sp
                            )
                            Text(
                                text = if (currentOrder.isTimerRunning) "WAKTU BERJALAN" else "DIJEDA",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (currentOrder.isTimerRunning) StatusOnlineGreen else AmberGold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Play / Pause Timer Button
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { viewModel.toggleTimer() },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentOrder.isTimerRunning) AmberGold else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(if (currentOrder.isTimerRunning) "Jeda Sementara" else "Lanjutkan Timer")
                        }

                        Button(
                            onClick = { viewModel.finishTreatment() },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusOnlineGreen)
                        ) {
                            Text("Selesaikan Terapi")
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Ambient Music Selector
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "🎵 Musik Ambient Relaksasi Spa",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val sounds = listOf("Seruling Bambu Spa", "Suara Hujan & Air Mengalir", "Gamelan Meditasi", "Mati (Hening)")
                            sounds.forEach { snd ->
                                val isSelected = currentOrder.selectedAmbientSound == snd
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.setAmbientSound(snd) }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isSelected) "●" else "○",
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = snd,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Stage 5: Payment Settlement & Invoice Breakdown
            else if (currentOrder.status == OrderStatus.COMPLETED_PAYMENT) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Rincian Pembayaran & Struk",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Tarif Layanan (${currentOrder.servicePackage.durationMinutes} Mnt)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Rp ${currencyFormat.format(currentOrder.servicePackage.basePrice)}", fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Uang Transportasi", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Rp ${currencyFormat.format(currentOrder.travelAllowance)}", fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Hak Mitra (80% + Transport)", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    "Rp ${currencyFormat.format(currentOrder.servicePackage.therapistShare + currentOrder.travelAllowance)}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { viewModel.confirmPayment(0L) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("KONFIRMASI PEMBAYARAN DITERIMA", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            // Stage 6: Client Rating & Feedback Review
            else if (currentOrder.status == OrderStatus.REVIEW_SUBMITTED) {
                var customerRating by remember { androidx.compose.runtime.mutableIntStateOf(5) }
                var ratingComment by remember { mutableStateOf("") }
                val selectedMitraTags = remember { androidx.compose.runtime.mutableStateListOf<String>() }
                val mitraTags = listOf("😊 Pelanggan Ramah", "📍 Lokasi Jelas", "💬 Respon Cepat", "✨ Tempat Bersih")

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(StatusOnlineGreen.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✓", color = StatusOnlineGreen, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Pesanan Selesai dengan Sukses!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Saldo Rp ${currencyFormat.format(currentOrder.servicePackage.therapistShare + currentOrder.travelAllowance + currentOrder.tipAmount)} telah ditambahkan ke dompet Anda.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Rate Customer Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        shadowElevation = 3.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Beri Penilaian untuk ${currentOrder.client.name}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                (1..5).forEach { star ->
                                    val isSelected = star <= customerRating
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Star $star",
                                        tint = if (isSelected) AmberGold else MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clickable { customerRating = star }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Tag Chips
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                mitraTags.chunked(2).forEach { rowTags ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        rowTags.forEach { tag ->
                                            val isSelected = selectedMitraTags.contains(tag)
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSelected) EmeraldLight else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary) else null,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        if (isSelected) selectedMitraTags.remove(tag) else selectedMitraTags.add(tag)
                                                    }
                                            ) {
                                                Text(
                                                    text = tag,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) EmeraldDark else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            viewModel.submitCustomerRating(customerRating, selectedMitraTags.toList(), ratingComment)
                            viewModel.finishOrderAndReturnHome()
                            onNavigateBack()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("KIRIM ULASAN & KEMBALI ONLINE", fontWeight = FontWeight.ExtraBold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Active Order - Light")
@Composable
fun ActiveOrderScreenPreview() {
    MassaGoMitraTheme(darkTheme = false) {
        ActiveOrderScreen(
            onNavigateBack = {},
            onNavigateToChat = {},
            onSosClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Active Order - Dark")
@Composable
fun ActiveOrderScreenDarkPreview() {
    MassaGoMitraTheme(darkTheme = true) {
        ActiveOrderScreen(
            onNavigateBack = {},
            onNavigateToChat = {},
            onSosClick = {}
        )
    }
}
