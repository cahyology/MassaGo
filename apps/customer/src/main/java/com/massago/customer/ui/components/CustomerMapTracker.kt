package com.massago.customer.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLngBounds
import com.massago.customer.ui.theme.EmeraldDark
import com.massago.customer.ui.theme.EmeraldPrimary
import com.massago.customer.util.GoogleDirectionsHelper

@SuppressLint("MissingPermission")
@Composable
fun CustomerMapTracker(
    modifier: Modifier = Modifier,
    etaMinutes: Int = 10,
    customerLocation: LatLng = LatLng(-7.7956, 110.3695),
    therapistLocation: LatLng = LatLng(-7.8000, 110.3650),
    isSearching: Boolean = false
) {
    val effectiveTherapistLoc = remember(customerLocation, therapistLocation) {
        if (therapistLocation.latitude != 0.0 && therapistLocation.latitude != -7.8000) {
            therapistLocation
        } else {
            therapistLocation
        }
    }

    // Persistent Real driving road route state (never resets to straight line on GPS tick)
    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var routeDistanceKm by remember { androidx.compose.runtime.mutableDoubleStateOf(0.0) }
    var routeEtaMinutes by remember { androidx.compose.runtime.mutableIntStateOf(etaMinutes.coerceAtLeast(1)) }

    LaunchedEffect(customerLocation, effectiveTherapistLoc, isSearching) {
        if (isSearching) {
            routePoints = emptyList()
            routeDistanceKm = 0.0
            routeEtaMinutes = 0
        } else {
            val info = GoogleDirectionsHelper.getDrivingRouteInfo(effectiveTherapistLoc, customerLocation)
            if (info.points.size >= 2) {
                routePoints = info.points
                routeDistanceKm = info.distanceKm
                routeEtaMinutes = info.durationMinutes
            }
        }
    }

    val distanceKm = routeDistanceKm
    val liveEta = routeEtaMinutes

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(customerLocation, 16f)
    }

    LaunchedEffect(customerLocation, effectiveTherapistLoc, isSearching) {
        try {
            if (isSearching) {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(customerLocation, 16.5f))
            } else {
                val bounds = LatLngBounds.builder()
                    .include(customerLocation)
                    .include(effectiveTherapistLoc)
                    .build()
                cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 120))
            }
        } catch (_: Exception) {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(customerLocation, 15.5f))
        }
    }

    // Pure standalone yellow motorcycle marker icon (NO background circle!)
    val yellowMotorIcon = remember {
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
        // Real Google Maps View
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
            // Marker: Titik Antar / Rumah Pelanggan
            Marker(
                state = MarkerState(position = customerLocation),
                title = "📍 Lokasi Anda",
                snippet = "Titik Temu Pemijatan"
            )

            if (!isSearching) {
                // Marker: Terapis Bergerak Realtime (Ikon Sepeda Motor)
                Marker(
                    state = MarkerState(position = effectiveTherapistLoc),
                    title = "🛵 Terapis Mitra On The Way",
                    snippet = "Jarak: ${distanceKm} km • Estimasi tiba ~${liveEta} mnt",
                    icon = yellowMotorIcon
                )

                // Real Street-following Direction Polyline (Smooth & Never Flickering)
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

        // Top Floating Live ETA & Distance Pill (Gojek/Grab style)
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
                if (isSearching) {
                    Text(text = "🔍", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Mencari Mitra Terapis Terdekat...",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = EmeraldDark
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.TwoWheeler,
                        contentDescription = null,
                        tint = Color(0xFFEAB308), // Yellow
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "🛵 Mitra menuju lokasi Anda • ${distanceKm} km (~${liveEta} mnt)",
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

    // Direct standalone yellow scooter icon text on transparent canvas
    val textPaint = Paint().apply {
        isAntiAlias = true
        textSize = 68f
        textAlign = Paint.Align.CENTER
    }
    val yPos = (sizePx / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
    canvas.drawText("🛵", sizePx / 2f, yPos, textPaint)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}
