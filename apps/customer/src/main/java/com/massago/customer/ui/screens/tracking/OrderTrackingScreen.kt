package com.massago.customer.ui.screens.tracking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.massago.customer.data.model.CustomerOrderStatus
import com.massago.customer.ui.components.CustomerMapTracker
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign
import com.massago.customer.ui.theme.TextSecondary
import com.massago.customer.ui.theme.AmberGold
import com.massago.customer.ui.theme.EmeraldDark
import com.massago.customer.ui.theme.EmeraldDeep
import com.massago.customer.ui.theme.EmeraldLight
import com.massago.customer.ui.theme.EmeraldPrimary
import com.massago.customer.ui.theme.TextMuted
import com.massago.customer.ui.theme.TextSecondary
import java.text.NumberFormat
import java.util.Locale

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import com.massago.customer.data.repository.CustomerChatRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderTrackingScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChat: () -> Unit,
    viewModel: OrderTrackingViewModel = viewModel()
) {
    val activeOrder by viewModel.activeOrder.collectAsState()
    val unreadChatCount by CustomerChatRepository.instance.unreadCount.collectAsState()
    val currencyFormat = NumberFormat.getNumberInstance(Locale("id", "ID"))

    if (activeOrder == null) {
        // Empty state / finished
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "💆‍♂️", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Tidak Ada Pesanan Aktif",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onNavigateBack,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(text = "Kembali ke Beranda", color = Color.White)
                }
            }
        }
        return
    }

    val order = activeOrder!!

    // Dialogs: Only show radar popup when actively searching and therapist has not been assigned
    if (order.status == CustomerOrderStatus.SEARCHING_THERAPIST && order.assignedTherapist == null) {
        MatchingRadarDialog(
            order = order,
            onCancel = {
                viewModel.cancelOrder()
                onNavigateBack()
            }
        )
    }

    if (order.status == CustomerOrderStatus.TREATMENT_FINISHED_PAYMENT) {
        CustomerRatingDialog(
            order = order,
            onSubmit = { rating, comment, tags, tip ->
                viewModel.submitRating(rating, comment, tags, tip)
                onNavigateBack()
            }
        )
    }

    var showSosDialog by remember { mutableStateOf(false) }
    var sosSentNotice by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    if (showSosDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showSosDialog = false }) {
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
                        text = "Pusat Bantuan Darurat (SOS)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Gunakan hanya untuk kondisi darurat keselamatan atau ancaman bahaya saat sesi layanan berlangsung.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Koordinat GPS Live Anda:",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            Text(
                                text = "${order.location.latitude}, ${order.location.longitude}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Panggilan Cepat Polisi & Ambulans
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            contentPadding = PaddingValues(vertical = 10.dp)
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Text("🚑 Ambulans 118", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // WhatsApp Satgas MassaGo
                    OutlinedButton(
                        onClick = {
                            try {
                                val url = "https://wa.me/6281234567890?text=DARURAT%20SOS%20MassaGo%20ID%20Pesanan%20${order.id}"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF059669))
                    ) {
                        Text("💬 Satgas MassaGo 24/7", color = Color(0xFF059669), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tombol Pancarkan Sinyal ke Server
                    Button(
                        onClick = {
                            com.massago.customer.data.repository.CustomerOrderRepository.instance.triggerSosAlert("Panggilan Darurat Pelanggan dari Order #${order.id}")
                            sosSentNotice = true
                            showSosDialog = false
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

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.statusBars,
                title = {
                    Column {
                        Text(
                            text = "Status Pesanan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = order.id,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    Surface(
                        modifier = Modifier
                            .padding(end = 14.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showSosDialog = true },
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFECACA))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDC2626))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SOS",
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (order.status) {
                CustomerOrderStatus.SEARCHING_THERAPIST,
                CustomerOrderStatus.THERAPIST_FOUND,
                CustomerOrderStatus.THERAPIST_ON_THE_WAY,
                CustomerOrderStatus.THERAPIST_ARRIVED -> {
                    // Map View Tracking with Real GPS Coordinates (No jumping to Jakarta)
                    val custPos = com.google.android.gms.maps.model.LatLng(
                        order.location.latitude,
                        order.location.longitude
                    )
                    val therapistPos = if (order.assignedTherapist != null && order.assignedTherapist.latitude != 0.0) {
                        com.google.android.gms.maps.model.LatLng(
                            order.assignedTherapist.latitude,
                            order.assignedTherapist.longitude
                        )
                    } else {
                        com.google.android.gms.maps.model.LatLng(
                            order.location.latitude - 0.008,
                            order.location.longitude - 0.006
                        )
                    }

                    val isSearchingTherapist = (order.status == CustomerOrderStatus.SEARCHING_THERAPIST || order.assignedTherapist == null)

                    Column(modifier = Modifier.fillMaxSize()) {
                        CustomerMapTracker(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            etaMinutes = order.assignedTherapist?.etaMinutes ?: 10,
                            customerLocation = custPos,
                            therapistLocation = therapistPos,
                            isSearching = isSearchingTherapist
                        )

                        // Bottom Card with Therapist Info
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                            color = Color.White,
                            shadowElevation = 10.dp
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                // Status Headline
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        val headline = when (order.status) {
                                            CustomerOrderStatus.SEARCHING_THERAPIST -> "🔍 Mencari Terapis Terdekat..."
                                            CustomerOrderStatus.THERAPIST_ARRIVED -> "🚪 Terapis Telah Tiba!"
                                            else -> "🛵 Terapis Menuju Lokasi"
                                        }
                                        val subHeadline = when (order.status) {
                                            CustomerOrderStatus.SEARCHING_THERAPIST -> "Menghubungi mitra terapis bersertifikasi di sekitar Anda"
                                            CustomerOrderStatus.THERAPIST_ARRIVED -> "Mohon siapkan ruangan untuk sesi pijat"
                                            else -> "Terapis ${order.assignedTherapist?.name ?: "Mitra"} dalam perjalanan (~${order.assignedTherapist?.etaMinutes ?: 10} mnt)"
                                        }

                                        Text(
                                            text = headline,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (order.status == CustomerOrderStatus.THERAPIST_ARRIVED) EmeraldDark else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = subHeadline,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = EmeraldLight
                                    ) {
                                        Text(
                                            text = "${order.durationMinutes} mnt",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldDeep,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                if (order.status == CustomerOrderStatus.THERAPIST_ARRIVED) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFFEF3C7),
                                        border = BorderStroke(1.dp, Color(0xFFF59E0B))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = "⏱️", fontSize = 20.sp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "Terapis Sudah Tiba di Lokasi Anda",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF92400E)
                                                )
                                                Text(
                                                    text = "Mohon bukakan pintu & persiapkan tempat. Sesuai SOP MassaGo, batas waktu tunggu kehadiran adalah 15 menit.",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF78350F)
                                                )
                                            }
                                        }
                                    }
                                }

                                Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))

                                // Therapist Profile Card
                                val therapist = order.assignedTherapist
                                if (therapist != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.linearGradient(listOf(EmeraldPrimary, EmeraldDark))
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = therapist.avatarInitials,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 18.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = therapist.name,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Verified,
                                                    contentDescription = null,
                                                    tint = EmeraldPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = AmberGold, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(text = "${therapist.rating}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                                Text(text = " • ${therapist.gender}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                            }
                                        }

                                        // Call & Chat Action buttons
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = EmeraldLight,
                                                    modifier = Modifier.clickable { onNavigateToChat() }
                                                ) {
                                                    Box(modifier = Modifier.padding(10.dp)) {
                                                        Icon(imageVector = Icons.Default.Chat, contentDescription = "Chat", tint = EmeraldDark, modifier = Modifier.size(20.dp))
                                                    }
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
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                if (order.status == CustomerOrderStatus.THERAPIST_ON_THE_WAY) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = EmeraldLight.copy(alpha = 0.4f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("🛵", fontSize = 20.sp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "Terapis sedang dalam perjalanan menuju alamat Anda.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = EmeraldDark
                                            )
                                        }
                                    }
                                } else if (order.status == CustomerOrderStatus.THERAPIST_ARRIVED) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = EmeraldLight.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("🚪", fontSize = 20.sp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "Terapis telah tiba di lokasi Anda. Sedang mempersiapkan perlengkapan higienis.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = EmeraldDark
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                CustomerOrderStatus.TREATMENT_IN_PROGRESS -> {
                    // Spa Mode Treatment View
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(EmeraldDeep, Color(0xFF064E3B), Color(0xFF022C22))
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Top Headline
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Sesi Terapi Sedang Berlangsung",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${order.service.name} • Bersama ${order.assignedTherapist?.name ?: "Terapis"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EmeraldLight
                                )
                            }

                            // Circular Timer Display
                            val minutes = order.remainingSeconds / 60
                            val seconds = order.remainingSeconds % 60
                            val formattedTime = String.format("%02d:%02d", minutes, seconds)

                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.08f),
                                border = androidx.compose.foundation.BorderStroke(4.dp, AmberGold.copy(alpha = 0.8f)),
                                modifier = Modifier.size(200.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = formattedTime,
                                            style = MaterialTheme.typography.displayLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            fontSize = 42.sp
                                        )
                                        Text(
                                            text = "Sisa Waktu Terapi",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = EmeraldLight,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                            // Ambient Sound Player Simulation
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color.White.copy(alpha = 0.12f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Headphones,
                                            contentDescription = null,
                                            tint = AmberGold,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Musik Ambient Relaksasi: ${order.selectedAmbientSound}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("Seruling Spa", "Hujan Tenang", "Ombak Laut").forEach { sound ->
                                            val isSelected = order.selectedAmbientSound.contains(sound.take(5))
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { viewModel.selectAmbientSound(sound) },
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSelected) AmberGold else Color.White.copy(alpha = 0.1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier.padding(vertical = 8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = sound,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Actions (+15 min / Finish)
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.extendDuration(15) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(imageVector = Icons.Default.MoreTime, contentDescription = null, tint = AmberGold, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "+15 Menit", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { viewModel.extendDuration(30) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(imageVector = Icons.Default.MoreTime, contentDescription = null, tint = AmberGold, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "+30 Menit", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                CustomerOrderStatus.TREATMENT_FINISHED_PAYMENT, CustomerOrderStatus.ORDER_RATED -> {
                    CustomerRatingView(
                        order = order,
                        onSubmit = { rating, tags, comment, tip ->
                            viewModel.submitRating(rating, comment, tags, tip)
                            onNavigateBack()
                        }
                    )
                }

                else -> {
                    // Fallback
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Status: ${order.status.name}")
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerRatingView(
    order: com.massago.customer.data.model.CustomerOrder,
    onSubmit: (rating: Int, tags: List<String>, comment: String, tip: Long) -> Unit
) {
    var selectedRating by remember { mutableStateOf(5) }
    var reviewComment by remember { mutableStateOf("") }
    var selectedTip by remember { mutableStateOf(0L) }
    val selectedTags = remember { androidx.compose.runtime.mutableStateListOf<String>() }

    val availableTags = listOf(
        "💆‍♂️ Pijatan Mantap",
        "⏱️ Tepat Waktu",
        "😊 Ramah & Sopan",
        "🧼 Sangat Higienis",
        "🌿 Minyak Wangi",
        "💪 Tekanan Pas"
    )

    val ratingLabels = mapOf(
        1 to "Kurang Memuaskan 😞",
        2 to "Perlu Peningkatan 😐",
        3 to "Cukup Baik 🙂",
        4 to "Sangat Memuaskan! 😊",
        5 to "Luar Biasa & Sempurna! 🌟"
    )

    val tipOptions = listOf(0L, 10000L, 20000L, 50000L)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Success badge & Title
        Surface(
            shape = CircleShape,
            color = EmeraldLight,
            modifier = Modifier.size(68.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = "🎉", fontSize = 32.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Sesi Pijat Telah Selesai!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = Color(0xFF0F172A),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Bagikan ulasan Anda bersama ${order.assignedTherapist?.name ?: "Mitra Terapis"}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Star Rating Selection Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Berapa bintang untuk terapis Anda?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (1..5).forEach { star ->
                        val isSelected = star <= selectedRating
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Star $star",
                            tint = if (isSelected) AmberGold else Color(0xFFE2E8F0),
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { selectedRating = star }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = ratingLabels[selectedRating] ?: "Sangat Memuaskan! 🌟",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = AmberGold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Compliment Feedback Chips
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Apa yang paling Anda sukai?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    availableTags.chunked(2).forEach { rowTags ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowTags.forEach { tag ->
                                val isSelected = selectedTags.contains(tag)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) EmeraldLight else Color(0xFFF1F5F9),
                                    border = if (isSelected) BorderStroke(1.5.dp, EmeraldPrimary) else null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            if (isSelected) selectedTags.remove(tag) else selectedTags.add(tag)
                                        }
                                ) {
                                    Text(
                                        text = tag,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) EmeraldDark else Color(0xFF475569),
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = reviewComment,
                    onValueChange = { reviewComment = it },
                    placeholder = { Text("Tulis ulasan pengalaman Anda (opsional)...", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    minLines = 2,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tip Selection
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Beri Tip untuk Terapis (Opsional)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tipOptions.forEach { tip ->
                        val isSelected = selectedTip == tip
                        val label = if (tip == 0L) "Nanti" else "Rp ${tip / 1000}k"
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) EmeraldPrimary else Color(0xFFF1F5F9),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTip = tip }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color(0xFF475569),
                                modifier = Modifier.padding(vertical = 10.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Submit Button
        Button(
            onClick = {
                onSubmit(selectedRating, selectedTags.toList(), reviewComment, selectedTip)
            },
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = "Kirim Penilaian & Selesai",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

