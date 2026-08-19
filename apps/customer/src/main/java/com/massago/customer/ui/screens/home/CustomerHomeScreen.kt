package com.massago.customer.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.massago.customer.data.model.MassageService
import com.massago.customer.ui.components.CustomerTopBar
import com.massago.customer.ui.components.PromoBannerCarousel
import com.massago.customer.ui.components.ServiceCategoryList
import com.massago.customer.ui.components.TherapistCardItem
import com.massago.customer.ui.theme.AmberGold
import com.massago.customer.ui.theme.EmeraldDark
import com.massago.customer.ui.theme.EmeraldDeep
import com.massago.customer.ui.theme.EmeraldLight
import com.massago.customer.ui.theme.EmeraldPrimary
import com.massago.customer.ui.theme.TextMuted
import com.massago.customer.ui.theme.TextSecondary

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerHomeScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToTracking: () -> Unit,
    onNavigateToWallet: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToCheckout: ((serviceId: String, therapistId: String, therapistName: String) -> Unit)? = null,
    viewModel: CustomerHomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val profile by viewModel.profile.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val activeOrder by viewModel.activeOrder.collectAsState()
    val unreadChatCount by com.massago.customer.data.repository.CustomerChatRepository.instance.unreadCount.collectAsState()
    val favoriteTherapists by com.massago.customer.data.repository.CustomerUserRepository.instance.favoriteTherapists.collectAsState()
    val vouchers by viewModel.vouchers.collectAsState()
    val services by viewModel.services.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    var showLocationSheet by remember { mutableStateOf(false) }
    var showMapPinPicker by remember { mutableStateOf(false) }

    fun detectGpsLocation() {
        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    coroutineScope.launch(Dispatchers.IO) {
                        var resolvedTitle = "Lokasi Saat Ini"
                        var resolvedAddress = "${loc.latitude}, ${loc.longitude}"
                        try {
                            val geocoder = Geocoder(context, Locale("id", "ID"))
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                geocoder.getFromLocation(loc.latitude, loc.longitude, 1) { addresses ->
                                    val addr = addresses.firstOrNull()
                                    if (addr != null) {
                                        val street = addr.thoroughfare ?: addr.subLocality ?: addr.locality ?: "Lokasi Saat Ini"
                                        val full = addr.getAddressLine(0) ?: "$street, ${addr.locality ?: ""}"
                                        com.massago.customer.data.repository.CustomerUserRepository.instance.setLocation(
                                            com.massago.customer.data.model.CustomerLocation(
                                                title = street,
                                                address = full,
                                                notes = "Titik GPS Real",
                                                latitude = loc.latitude,
                                                longitude = loc.longitude
                                            )
                                        )
                                    }
                                }
                            } else {
                                @Suppress("DEPRECATION")
                                val addrs = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                                val addr = addrs?.firstOrNull()
                                if (addr != null) {
                                    resolvedTitle = addr.thoroughfare ?: addr.subLocality ?: addr.locality ?: "Lokasi Saat Ini"
                                    resolvedAddress = addr.getAddressLine(0) ?: "$resolvedTitle, ${addr.locality ?: ""}"
                                }
                                withContext(Dispatchers.Main) {
                                    com.massago.customer.data.repository.CustomerUserRepository.instance.setLocation(
                                        com.massago.customer.data.model.CustomerLocation(
                                            title = resolvedTitle,
                                            address = resolvedAddress,
                                            notes = "Titik GPS Real",
                                            latitude = loc.latitude,
                                            longitude = loc.longitude
                                        )
                                    )
                                }
                            }
                        } catch (_: Exception) {
                            withContext(Dispatchers.Main) {
                                com.massago.customer.data.repository.CustomerUserRepository.instance.setLocation(
                                    com.massago.customer.data.model.CustomerLocation(
                                        title = "Lokasi GPS",
                                        address = "Titik Koordinat: ${loc.latitude}, ${loc.longitude}",
                                        notes = "Titik GPS Real",
                                        latitude = loc.latitude,
                                        longitude = loc.longitude
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            detectGpsLocation()
        }
    }

    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            detectGpsLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    val categories = listOf("Semua", "Tradisional", "Refleksi", "Kebugaran", "Spa & Kulit", "Kesehatan", "Khusus")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        CustomerTopBar(
            currentLocation = currentLocation,
            walletBalance = profile.walletBalance,
            onLocationClick = { showLocationSheet = true },
            onWalletClick = onNavigateToWallet
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Active Order Floating Alert
            if (activeOrder != null && activeOrder?.status != CustomerOrderStatus.ORDER_RATED && activeOrder?.status != CustomerOrderStatus.CANCELLED) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clickable { onNavigateToTracking() },
                        shape = RoundedCornerShape(16.dp),
                        color = EmeraldDeep,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
                        shadowElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(AmberGold),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val icon = when (activeOrder?.status) {
                                        CustomerOrderStatus.SEARCHING_THERAPIST -> "🔍"
                                        CustomerOrderStatus.THERAPIST_ON_THE_WAY -> "🛵"
                                        CustomerOrderStatus.THERAPIST_ARRIVED -> "🚪"
                                        CustomerOrderStatus.TREATMENT_IN_PROGRESS -> "💆"
                                        else -> "⚡"
                                    }
                                    Text(text = icon, fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    val statusDisplay = when (activeOrder?.status) {
                                        CustomerOrderStatus.SEARCHING_THERAPIST -> "Sedang Mencari Terapis..."
                                        CustomerOrderStatus.THERAPIST_FOUND -> "Terapis Ditemukan!"
                                        CustomerOrderStatus.THERAPIST_ON_THE_WAY -> "Terapis Menuju Lokasi"
                                        CustomerOrderStatus.THERAPIST_ARRIVED -> "Terapis Telah Tiba di Lokasi"
                                        CustomerOrderStatus.TREATMENT_IN_PROGRESS -> "Sesi Terapi Berlangsung"
                                        CustomerOrderStatus.TREATMENT_FINISHED_PAYMENT -> "Selesai & Pembayaran"
                                        else -> "Pesanan Aktif"
                                    }
                                    Text(
                                        text = statusDisplay,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberGold,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = activeOrder?.service?.name ?: "Layanan Pijat",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 11.5.sp,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Box {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = EmeraldLight
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Tracking",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldDark,
                                            fontSize = 11.sp
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = EmeraldDark,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                                if (unreadChatCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .align(Alignment.TopEnd)
                                            .clip(CircleShape)
                                            .background(Color.Red)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = {
                        Text(
                            text = "Cari pijat tradisional, refleksi, scrub spa...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    ),
                    singleLine = true
                )
            }

            // Promo Banner Carousel
            item {
                Spacer(modifier = Modifier.height(6.dp))
                PromoBannerCarousel(
                    vouchers = vouchers,
                    onVoucherClick = { /* Handle voucher click */ }
                )
            }

            // Section: Terapis Langganan Anda (Favorite Therapists Slider)
            if (favoriteTherapists.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⭐ Terapis Langganan Anda",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "${favoriteTherapists.size} Tersimpan",
                                style = MaterialTheme.typography.bodySmall,
                                color = AmberGold,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(favoriteTherapists) { therapist ->
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = Color.White,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    shadowElevation = 2.dp,
                                    modifier = Modifier.width(260.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .clip(CircleShape)
                                                    .background(if (therapist.gender == "Wanita") Color(0xFFFCE7F3) else Color(0xFFE0F2FE)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = therapist.avatarInitials,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp,
                                                    color = if (therapist.gender == "Wanita") Color(0xFFDB2777) else Color(0xFF0284C7)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(10.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = therapist.name,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = null,
                                                        tint = AmberGold,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text(
                                                        text = "${therapist.rating} (${therapist.ordersCompleted}+ order)",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontSize = 11.sp,
                                                        color = TextSecondary
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = therapist.specialty,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextMuted,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Button(
                                            onClick = {
                                                val targetServiceId = services.firstOrNull()?.id ?: "SRV-TRAD"
                                                if (onNavigateToCheckout != null) {
                                                    onNavigateToCheckout(targetServiceId, therapist.id, therapist.name)
                                                } else {
                                                    onNavigateToDetail(targetServiceId)
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = AmberGold),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                            contentPadding = PaddingValues(vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = "⭐ Pesan Lagi",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Category Filter Chips
            item {
                Spacer(modifier = Modifier.height(16.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { cat ->
                        val isSelected = cat == selectedCategory
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) EmeraldPrimary else Color.White,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) EmeraldPrimary else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.clickable { viewModel.setSelectedCategory(cat) }
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Service Catalog Header
            item {
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Katalog Layanan Pijat",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${viewModel.filteredServices(services).size} Paket Tersedia",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Services List
            item {
                ServiceCategoryList(
                    services = viewModel.filteredServices(services),
                    onServiceClick = { service -> onNavigateToDetail(service.id) }
                )
            }

            // Section: Verified Nearby Therapists
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "Terapis Terverifikasi di Sekitar Anda",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Telah lolos verifikasi SKCK, tes kesehatan & sertifikasi keahlian",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    viewModel.nearbyTherapists.forEach { therapist ->
                        TherapistCardItem(
                            therapist = therapist,
                            onClick = {
                                // Direct to default service with this therapist preference
                                val targetId = services.firstOrNull()?.id ?: "SRV-TRAD"
                                onNavigateToDetail(targetId)
                            },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }

    // Modal Location Picker Sheet
    if (showLocationSheet) {
        LocationPickerSheet(
            savedAddresses = profile.savedAddresses,
            currentLocation = currentLocation,
            onSelectAddress = { addr -> viewModel.selectAddress(addr) },
            onOpenMapPinPicker = {
                showLocationSheet = false
                showMapPinPicker = true
            },
            onDismiss = { showLocationSheet = false }
        )
    }

    // Interactive Google Maps Pin Point Location Dialog
    if (showMapPinPicker) {
        LocationPinPickerDialog(
            initialLocation = currentLocation,
            onConfirmLocation = { newAddress ->
                viewModel.addAndSelectAddress(newAddress)
                showMapPinPicker = false
            },
            onDismiss = { showMapPinPicker = false }
        )
    }
}
