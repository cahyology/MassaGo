package com.massago.customer.ui.screens.home

import android.annotation.SuppressLint
import android.location.Geocoder
import android.os.Build
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import com.massago.customer.data.model.CustomerLocation
import com.massago.customer.data.model.SavedAddress
import com.massago.customer.ui.theme.AmberGold
import com.massago.customer.ui.theme.EmeraldDark
import com.massago.customer.ui.theme.EmeraldDeep
import com.massago.customer.ui.theme.EmeraldPrimary
import com.massago.customer.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@SuppressLint("MissingPermission")
@Composable
fun LocationPinPickerDialog(
    initialLocation: CustomerLocation,
    onConfirmLocation: (SavedAddress) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val initialPos = LatLng(initialLocation.latitude, initialLocation.longitude)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPos, 16.5f)
    }

    var locationTitle by remember { mutableStateOf(if (initialLocation.title.isNotBlank() && initialLocation.title != "Pilih Titik Lokasi") initialLocation.title else "Lokasi Penjemputan") }
    var locationAddress by remember { mutableStateOf(if (initialLocation.address.isNotBlank() && !initialLocation.address.startsWith("Ketuk")) initialLocation.address else "Sedang memuat alamat...") }
    var locationNote by remember { mutableStateOf(initialLocation.notes) }
    var isGeocodingLoading by remember { mutableStateOf(false) }

    // Pin bounce animation when dragging camera
    val pinElevation by animateDpAsState(
        targetValue = if (cameraPositionState.isMoving) 18.dp else 0.dp,
        label = "pinBounce"
    )

    // Initial GPS alignment with device real location
    LaunchedEffect(Unit) {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    val userGps = LatLng(loc.latitude, loc.longitude)
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(userGps, 17f)
                }
            }
        } catch (e: Exception) {
            // fallback
        }
    }

    // Real-Time Reverse Geocoding with Google Maps / Android Geocoder as map moves
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            val target = cameraPositionState.position.target
            isGeocodingLoading = true
            withContext(Dispatchers.IO) {
                try {
                    val geocoder = Geocoder(context, Locale("id", "ID"))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        geocoder.getFromLocation(target.latitude, target.longitude, 1) { addresses ->
                            val addr = addresses.firstOrNull()
                            if (addr != null) {
                                val full = addr.getAddressLine(0) ?: "${addr.thoroughfare ?: ""}, ${addr.subLocality ?: ""}, ${addr.locality ?: ""}"
                                val title = when {
                                    !addr.featureName.isNullOrBlank() && addr.featureName != addr.thoroughfare -> addr.featureName
                                    !addr.thoroughfare.isNullOrBlank() -> "${addr.thoroughfare} ${addr.subThoroughfare ?: ""}".trim()
                                    !addr.subLocality.isNullOrBlank() -> addr.subLocality
                                    else -> "Titik Jemput di Peta"
                                }
                                locationTitle = title
                                locationAddress = full
                            }
                            isGeocodingLoading = false
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(target.latitude, target.longitude, 1)
                        val addr = addresses?.firstOrNull()
                        if (addr != null) {
                            val full = addr.getAddressLine(0) ?: "${addr.thoroughfare ?: ""}, ${addr.subLocality ?: ""}, ${addr.locality ?: ""}"
                            val title = when {
                                !addr.featureName.isNullOrBlank() && addr.featureName != addr.thoroughfare -> addr.featureName
                                !addr.thoroughfare.isNullOrBlank() -> "${addr.thoroughfare} ${addr.subThoroughfare ?: ""}".trim()
                                !addr.subLocality.isNullOrBlank() -> addr.subLocality
                                else -> "Titik Jemput di Peta"
                            }
                            locationTitle = title
                            locationAddress = full
                        }
                        isGeocodingLoading = false
                    }
                } catch (e: Exception) {
                    locationAddress = "Koordinat: %.5f, %.5f".format(target.latitude, target.longitude)
                    locationTitle = "Titik Koordinat Peta"
                    isGeocodingLoading = false
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Real Interactive Google Maps View
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = true,
                    isBuildingEnabled = true,
                    isTrafficEnabled = false
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    compassEnabled = true,
                    myLocationButtonEnabled = false,
                    mapToolbarEnabled = false
                )
            )

            // Center Pin Indicator with dynamic bounce
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = -pinElevation - 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EmeraldDeep,
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isGeocodingLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    color = AmberGold,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = "📍 Titik Temu Pemijatan",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Pin Location",
                        tint = AmberGold,
                        modifier = Modifier.size(46.dp)
                    )
                }
            }

            // Top Header: Title & Close Button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationSearching,
                        contentDescription = null,
                        tint = EmeraldPrimary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tentukan Titik di Google Maps",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Geser peta untuk memposisikan titik jemput akurat",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup")
                    }
                }
            }

            // Floating GPS Recenter Button (Real Device Location)
            FloatingActionButton(
                onClick = {
                    try {
                        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                            if (loc != null) {
                                coroutineScope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(
                                            LatLng(loc.latitude, loc.longitude),
                                            17f
                                        )
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        coroutineScope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(initialPos, 17f)
                            )
                        }
                    }
                },
                containerColor = Color.White,
                contentColor = EmeraldDark,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 310.dp)
                    .size(48.dp)
            ) {
                Icon(imageVector = Icons.Default.MyLocation, contentDescription = "GPS Saya")
            }

            // Bottom Confirmation Card with auto-synced address
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Detail Alamat Terpilih",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        if (isGeocodingLoading) {
                            Text(
                                text = "Menyinkronkan...",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldPrimary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = locationTitle,
                        onValueChange = { locationTitle = it },
                        label = { Text("Nama Tempat (cth: Rumah, Kantor, Kos)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = locationAddress,
                        onValueChange = { locationAddress = it },
                        label = { Text("Alamat Lengkap (Otomatis dari Maps)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = locationNote,
                        onValueChange = { locationNote = it },
                        label = { Text("Catatan untuk Terapis (No. Kamar / Pagar)") },
                        placeholder = { Text("cth: Pagar hitam, lantai 2 unit 203") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val targetPos = cameraPositionState.position.target
                            val selectedAddress = SavedAddress(
                                id = "ADDR-" + System.currentTimeMillis().toString().takeLast(5),
                                title = locationTitle.ifBlank { "Lokasi Pin Point" },
                                fullAddress = locationAddress,
                                note = locationNote,
                                latitude = targetPos.latitude,
                                longitude = targetPos.longitude,
                                iconEmoji = "📍"
                            )
                            onConfirmLocation(selectedAddress)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Pasang & Simpan Titik Lokasi Ini",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
