package com.massago.mitra.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.massago.mitra.data.model.Order
import com.massago.mitra.data.model.OrderStatus
import com.massago.mitra.ui.theme.EmeraldDark
import com.massago.mitra.ui.theme.EmeraldPrimary
import com.massago.mitra.util.DrivingRouteInfo
import com.massago.mitra.util.GoogleDirectionsHelper
import kotlinx.coroutines.launch

import com.massago.mitra.data.network.SupabaseClient
import com.massago.mitra.data.repository.TherapistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@SuppressLint("MissingPermission")
@Composable
fun MitraLiveMapView(
    modifier: Modifier = Modifier,
    isOnline: Boolean = true,
    activeOrder: Order? = null,
    radiusKm: Int = 10,
    mitraLocation: LatLng = LatLng(-7.7956, 110.3695)
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val customerLocation = remember(activeOrder) {
        if (activeOrder != null && activeOrder.client.latitude != 0.0) {
            LatLng(activeOrder.client.latitude, activeOrder.client.longitude)
        } else {
            mitraLocation
        }
    }

    val effectiveMitraGps = if (mitraLocation.latitude != 0.0 && mitraLocation.latitude != -7.7956) {
        mitraLocation
    } else {
        mitraLocation
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(effectiveMitraGps, 15.5f)
    }

    // Auto-center camera when mitraLocation moves
    LaunchedEffect(effectiveMitraGps) {
        if (activeOrder == null) {
            cameraPositionState.animate(CameraUpdateFactory.newLatLng(effectiveMitraGps))
        }
    }

    var hasInitiallyFramedOrder by remember(activeOrder?.id) { mutableStateOf(false) }

    LaunchedEffect(activeOrder?.id) {
        if (activeOrder != null && !hasInitiallyFramedOrder) {
            try {
                val bounds = LatLngBounds.builder()
                    .include(customerLocation)
                    .include(effectiveMitraGps)
                    .build()
                cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 120))
                hasInitiallyFramedOrder = true
            } catch (_: Exception) {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(customerLocation, 15.5f))
            }
        } else if (activeOrder == null) {
            hasInitiallyFramedOrder = false
        }
    }

    // Persistent Real driving road route state (never resets to straight line on GPS tick)
    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var routeDistanceKm by remember { androidx.compose.runtime.mutableDoubleStateOf(0.0) }
    var routeEtaMinutes by remember { androidx.compose.runtime.mutableIntStateOf(10) }

    LaunchedEffect(customerLocation, effectiveMitraGps, activeOrder?.id) {
        if (activeOrder != null && activeOrder.status != OrderStatus.INCOMING) {
            val info = GoogleDirectionsHelper.getDrivingRouteInfo(effectiveMitraGps, customerLocation)
            if (info.points.size >= 2) {
                routePoints = info.points
                routeDistanceKm = info.distanceKm
                routeEtaMinutes = info.durationMinutes
            }
        } else {
            routePoints = emptyList()
            routeDistanceKm = 0.0
            routeEtaMinutes = 0
        }
    }

    val realDistanceKm = routeDistanceKm
    val realEtaMinutes = routeEtaMinutes

    // Standalone Yellow Motorcycle Custom Marker Icon (NO background circle!)
    val yellowMotorIcon = remember(context) {
        try {
            createStandaloneYellowMotorcycleIcon()
        } catch (e: Exception) {
            try {
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
            } catch (e2: Exception) {
                null
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Real Google Maps Platform View
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = true,
                isBuildingEnabled = true,
                isTrafficEnabled = true
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                compassEnabled = true,
                myLocationButtonEnabled = false,
                mapToolbarEnabled = false
            )
        ) {
            // Marker: Lokasi Mitra (Ikon Sepeda Motor Tanpa Background Bulat Saat On The Way)
            if (activeOrder != null && activeOrder.status != OrderStatus.INCOMING) {
                Marker(
                    state = MarkerState(position = effectiveMitraGps),
                    title = "🛵 Posisi Anda (Mitra)",
                    snippet = "Menuju ke lokasi ${activeOrder.client.name} (${realDistanceKm} km)",
                    icon = yellowMotorIcon
                )
            } else {
                Marker(
                    state = MarkerState(position = effectiveMitraGps),
                    title = "🛵 Posisi Anda (Mitra)",
                    snippet = if (isOnline) "Siap Menerima Pesanan" else "Sedang Istirahat"
                )
            }

            // Radius Radar Layanan Terkunci pada Posisi Mitra (Saat Standby)
            if (isOnline && activeOrder == null) {
                Circle(
                    center = effectiveMitraGps,
                    radius = (radiusKm * 1000.0),
                    fillColor = EmeraldPrimary.copy(alpha = 0.12f),
                    strokeColor = EmeraldPrimary.copy(alpha = 0.55f),
                    strokeWidth = 4f
                )
            }

            // Jika Ada Pesanan Aktif: Tampilkan Marker Klien & Rute Jalan Nyata (Smooth, Tanpa Flicker)
            if (activeOrder != null && activeOrder.status != OrderStatus.INCOMING) {
                Marker(
                    state = MarkerState(position = customerLocation),
                    title = "📍 Alamat Klien: ${activeOrder.client.name}",
                    snippet = activeOrder.client.address
                )

                if (routePoints.size >= 2) {
                    Polyline(
                        points = routePoints,
                        color = EmeraldPrimary,
                        width = 14f,
                        jointType = com.google.android.gms.maps.model.JointType.ROUND,
                        startCap = com.google.android.gms.maps.model.RoundCap(),
                        endCap = com.google.android.gms.maps.model.RoundCap()
                    )
                }
            }
        }

        // Floating Recenter on Real GPS Button (Positioned cleanly at bottom-right above card)
        FloatingActionButton(
            onClick = {
                try {
                    fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                        if (loc != null) {
                            coroutineScope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(loc.latitude, loc.longitude),
                                        16.5f
                                    )
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    coroutineScope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(mitraLocation, 16.5f)
                        )
                    }
                }
            },
            containerColor = Color.White,
            contentColor = EmeraldDark,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 120.dp)
                .size(46.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Posisiku",
                modifier = Modifier.size(22.dp)
            )
        }

        // Floating Live ETA Badge when active order is on the way
        if (activeOrder != null && activeOrder.status == OrderStatus.ACCEPTED_ON_THE_WAY) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TwoWheeler,
                        contentDescription = null,
                        tint = Color(0xFFEAB308),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Menuju ke klien • ${realDistanceKm} km (~${realEtaMinutes} mnt)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = EmeraldDark
                    )
                }
            }
        }
    }
}

// Generate standalone yellow motorcycle marker icon (NO background circle!)
private fun createStandaloneYellowMotorcycleIcon(): BitmapDescriptor {
    val sizePx = 100
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val textPaint = Paint().apply {
        isAntiAlias = true
        textSize = 68f
        textAlign = Paint.Align.CENTER
    }
    val yPos = (sizePx / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
    canvas.drawText("🛵", sizePx / 2f, yPos, textPaint)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}
