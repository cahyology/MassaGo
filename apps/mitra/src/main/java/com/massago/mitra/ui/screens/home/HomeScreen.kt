package com.massago.mitra.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.massago.mitra.data.model.DutyStatus
import com.massago.mitra.data.model.OrderStatus
import com.massago.mitra.ui.components.DailySummaryCard
import com.massago.mitra.ui.components.MitraLiveMapView
import com.massago.mitra.ui.components.StatusToggleSwitch
import com.massago.mitra.ui.components.TopHeaderBar
import com.massago.mitra.ui.theme.AmberGold
import com.massago.mitra.ui.theme.EmeraldDark
import com.massago.mitra.ui.theme.EmeraldLight
import com.massago.mitra.ui.theme.EmeraldPrimary
import com.massago.mitra.ui.theme.TextPrimary
import com.massago.mitra.ui.theme.TextSecondary
import com.massago.mitra.util.NotificationSoundHelper
import java.text.NumberFormat
import java.util.Locale

import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToActiveOrder: () -> Unit,
    onNavigateToWallet: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onSosClick: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val therapistProfile by viewModel.therapistProfile.collectAsState()
    val activeOrder by viewModel.activeOrder.collectAsState()
    val incomingCountdown by viewModel.incomingCountdown.collectAsState()

    var showPreferencesSheet by remember { mutableStateOf(false) }
    var isSummaryExpanded by remember { mutableStateOf(false) }

    val currencyFormat = remember { NumberFormat.getNumberInstance(Locale("id", "ID")) }
    val formattedEarnings = "Rp " + currencyFormat.format(therapistProfile.todayEarnings)

    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    // Auto-detect physical device GPS on launch and sync to Supabase therapists table
    LaunchedEffect(Unit) {
        try {
            val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        com.massago.mitra.data.repository.TherapistRepository.instance.updateCurrentLocation(loc.latitude, loc.longitude)
                        val targetId = therapistProfile.id.ifBlank { therapistProfile.phone }
                        if (targetId.isNotBlank()) {
                            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                com.massago.mitra.data.network.SupabaseClient.instance.updateLocationOnly(
                                    therapistId = targetId,
                                    latitude = loc.latitude,
                                    longitude = loc.longitude
                                )
                            }
                        }
                    } else {
                        fusedLocationClient.lastLocation.addOnSuccessListener { cachedLoc ->
                            if (cachedLoc != null) {
                                com.massago.mitra.data.repository.TherapistRepository.instance.updateCurrentLocation(cachedLoc.latitude, cachedLoc.longitude)
                            }
                        }
                    }
                }
        } catch (_: Exception) {}
    }

    // Audio Chime & Push Notification Effect when order arrives in foreground
    LaunchedEffect(activeOrder?.id, activeOrder?.status) {
        val current = activeOrder
        if (current != null && current.status == OrderStatus.INCOMING) {
            NotificationSoundHelper.triggerIncomingOrderAlert(context, current)
        } else if (current != null && current.status != OrderStatus.INCOMING) {
            NotificationSoundHelper.stopIncomingOrderAlert(context)
        }
    }

    val lifecycleOwner = context as? androidx.lifecycle.LifecycleOwner
    if (lifecycleOwner != null) {
        DisposableEffect(lifecycleOwner) {
            val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                    viewModel.checkPendingOrdersNow()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    }

    // If order becomes active (accepted), navigate automatically
    if (activeOrder != null && activeOrder?.status != OrderStatus.INCOMING && activeOrder?.status != OrderStatus.IDLE) {
        onNavigateToActiveOrder()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Full-Screen Interactive Google Map (Dominates the screen)
        MitraLiveMapView(
            modifier = Modifier.fillMaxSize(),
            isOnline = therapistProfile.dutyStatus == DutyStatus.ONLINE,
            activeOrder = activeOrder,
            radiusKm = therapistProfile.maxRadiusKm,
            mitraLocation = com.google.android.gms.maps.model.LatLng(therapistProfile.latitude, therapistProfile.longitude)
        )

        var showKycWarningDialog by remember { mutableStateOf(false) }

        // 2. Top Floating Controls: Header & Minimal Combined Duty/Auto-Accept Pill Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            TopHeaderBar(
                profile = therapistProfile,
                onSosClick = onSosClick,
                onWalletClick = onNavigateToWallet,
                onProfileClick = onNavigateToProfile
            )

            // KYC Pending In-Review Alert Banner
            if (!therapistProfile.isVerified) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFEF3C7),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Pendaftaran Dalam Peninjauan Admin",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = Color(0xFF92400E)
                            )
                            Text(
                                text = "Status 'Online' akan aktif setelah KYC disetujui.",
                                fontSize = 9.5.sp,
                                color = Color(0xFFB45309)
                            )
                        }
                    }
                }
            }

            // Minimalist Combined Duty & Auto-Accept Floating Bar
            StatusToggleSwitch(
                currentStatus = therapistProfile.dutyStatus,
                onStatusChange = { newStatus ->
                    if (newStatus == DutyStatus.ONLINE) {
                        if (!therapistProfile.isVerified) {
                            showKycWarningDialog = true
                            return@StatusToggleSwitch
                        }
                        com.massago.mitra.service.MitraLocationService.start(context)
                    } else if (newStatus == DutyStatus.OFFLINE) {
                        com.massago.mitra.service.MitraLocationService.stop(context)
                    }
                    viewModel.setDutyStatus(newStatus)
                },
                autoAcceptOrders = therapistProfile.autoAcceptOrders,
                onAutoAcceptChange = { isChecked ->
                    viewModel.toggleAutoAccept(isChecked)
                }
            )
        }

        if (showKycWarningDialog) {
            AlertDialog(
                onDismissRequest = { showKycWarningDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text("Akun Belum Terverifikasi", fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                },
                text = {
                    Text(
                        text = "Halo ${therapistProfile.name}, pendaftaran akun mitra Anda saat ini sedang dalam proses peninjauan oleh tim admin MassaGo.\n\nAnda belum dapat mengaktifkan status Online sampai data KYC dan rekening Anda disetujui.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showKycWarningDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("Mengerti")
                    }
                }
            )
        }

        // 3. Floating Quick Recenter Button (on Map)
        FloatingActionButton(
            onClick = { /* Recenter map camera on Mitra GPS */ },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = EmeraldDark,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = if (isSummaryExpanded) 220.dp else 115.dp)
                .size(44.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Posisiku",
                modifier = Modifier.size(20.dp)
            )
        }

        // 4. Modern Bottom Floating Panel: Compact Earnings Strip & Filter Pill
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .padding(bottom = 8.dp) // tight clearance for bottom navigation
        ) {
            // Expanded Daily Summary (Optional on demand)
            if (isSummaryExpanded) {
                DailySummaryCard(
                    profile = therapistProfile,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Compact Floating Bottom Dashboard Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                shadowElevation = 0.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color(0xFFF1F5F9)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Row 1: Earnings & Trips Summary with Expand Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isSummaryExpanded = !isSummaryExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(EmeraldPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MonetizationOn,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Pendapatan Bersih",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = formattedEarnings,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = EmeraldLight.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = "${therapistProfile.todayOrdersCount}/5 Order",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldDark,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (isSummaryExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                contentDescription = "Detail",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Row 2: Filter Radius & Auto Accept Pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Radius: ${therapistProfile.maxRadiusKm} km",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (therapistProfile.autoAcceptOrders) " • ⚡ Auto-Accept" else " • Manual",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (therapistProfile.autoAcceptOrders) EmeraldPrimary else TextSecondary,
                                fontWeight = if (therapistProfile.autoAcceptOrders) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.5.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.clickable { showPreferencesSheet = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Ubah Preferensi",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Atur",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Incoming Order Bottom Sheet Modal
        if (activeOrder?.status == OrderStatus.INCOMING) {
            activeOrder?.let { incoming ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    IncomingOrderSheet(
                        order = incoming,
                        countdownSeconds = incomingCountdown,
                        onAccept = {
                            NotificationSoundHelper.stopIncomingOrderAlert(context)
                            viewModel.acceptOrder()
                        },
                        onDecline = { reason ->
                            NotificationSoundHelper.stopIncomingOrderAlert(context)
                            viewModel.declineOrder(reason)
                        }
                    )
                }
            }
        }
    }

    // Modal Sheet for Quick Dispatch Preferences
    if (showPreferencesSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPreferencesSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Preferensi Penerimaan Order",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Sesuaikan radius penjemputan dan mode penerimaan order",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Auto-Accept Toggle Switch
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Otomatis Terima Orderan",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                if (therapistProfile.autoAcceptOrders) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "⚡ AKTIF",
                                        color = EmeraldPrimary,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            Text(
                                text = "Langsung terima pesanan terdekat dalam radius tanpa perlu konfirmasi manual",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Switch(
                            checked = therapistProfile.autoAcceptOrders,
                            onCheckedChange = { viewModel.toggleAutoAccept(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = EmeraldPrimary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                var currentRadius by remember { mutableStateOf(therapistProfile.maxRadiusKm.toFloat()) }
                Text(
                    text = "Radius Maksimal Penjemputan: ${currentRadius.toInt()} km",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Slider(
                    value = currentRadius,
                    onValueChange = { 
                        currentRadius = it
                        viewModel.updatePreferences(
                            radiusKm = it.toInt(),
                            genderPref = therapistProfile.preferredClientGender
                        )
                    },
                    valueRange = 3f..25f,
                    steps = 21,
                    colors = SliderDefaults.colors(
                        thumbColor = EmeraldPrimary,
                        activeTrackColor = EmeraldPrimary
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Preferensi Gender Pelanggan",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Semua", "Pria Saja", "Wanita Saja").forEach { gender ->
                        val isSelected = therapistProfile.preferredClientGender == gender
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    viewModel.updatePreferences(
                                        radiusKm = currentRadius.toInt(),
                                        genderPref = gender
                                    )
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) EmeraldPrimary else Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) EmeraldPrimary else Color(0xFFE2E8F0)),
                            shadowElevation = 0.dp
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = gender,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
