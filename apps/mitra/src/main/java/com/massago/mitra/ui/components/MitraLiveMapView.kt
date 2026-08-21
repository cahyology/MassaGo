package com.massago.mitra.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.RoundCap
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
import com.massago.mitra.util.GoogleDirectionsHelper
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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

    val effectiveMitraGps = remember(mitraLocation) {
        if (mitraLocation.latitude != 0.0 && mitraLocation.latitude != -7.7956) {
            mitraLocation
        } else {
            mitraLocation
        }
    }

    // Smooth Coordinate Interpolation State for Mitra
    var animatedMitraPos by remember { mutableStateOf(effectiveMitraGps) }
    var previousMitraPos by remember { mutableStateOf(effectiveMitraGps) }
    var rawBearing by remember { mutableFloatStateOf(0f) }

    val animatedBearing by animateFloatAsState(
        targetValue = rawBearing,
        animationSpec = tween(durationMillis = 800, easing = LinearEasing),
        label = "mitraBearingAnim"
    )

    // Route state
    var fullRoutePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var remainingRoutePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var routeDistanceKm by remember { mutableDoubleStateOf(0.0) }
    var routeEtaMinutes by remember { mutableIntStateOf(10) }

    // Interpolate coordinate whenever effectiveMitraGps moves
    LaunchedEffect(effectiveMitraGps) {
        if (effectiveMitraGps.latitude != 0.0 && effectiveMitraGps.longitude != 0.0) {
            val dist = GoogleDirectionsHelper.distanceInMeters(previousMitraPos, effectiveMitraGps)
            if (dist > 1.5) {
                rawBearing = GoogleDirectionsHelper.calculateBearing(previousMitraPos, effectiveMitraGps)
            }

            val startPos = animatedMitraPos
            val targetPos = effectiveMitraGps
            val animProgress = Animatable(0f)

            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
            ) {
                val t = this.value
                val curLat = startPos.latitude + (targetPos.latitude - startPos.latitude) * t
                val curLng = startPos.longitude + (targetPos.longitude - startPos.longitude) * t
                val curLatLng = LatLng(curLat, curLng)
                animatedMitraPos = curLatLng

                // Dynamically trim polyline in real-time as Mitra rides forward
                if (fullRoutePoints.size >= 2) {
                    remainingRoutePoints = GoogleDirectionsHelper.trimPassedRoute(curLatLng, fullRoutePoints)
                }
            }

            previousMitraPos = effectiveMitraGps
        }
    }

    // Fetch driving directions when active order is on the way
    LaunchedEffect(customerLocation, effectiveMitraGps, activeOrder?.id) {
        if (activeOrder != null && activeOrder.status != OrderStatus.INCOMING) {
            val shouldRecalculate = fullRoutePoints.isEmpty() ||
                    GoogleDirectionsHelper.distanceInMeters(effectiveMitraGps, fullRoutePoints.firstOrNull() ?: effectiveMitraGps) > 80.0

            if (shouldRecalculate) {
                val info = GoogleDirectionsHelper.getDrivingRouteInfo(effectiveMitraGps, customerLocation)
                if (info.points.size >= 2) {
                    fullRoutePoints = info.points
                    remainingRoutePoints = GoogleDirectionsHelper.trimPassedRoute(animatedMitraPos, info.points)
                    routeDistanceKm = info.distanceKm
                    routeEtaMinutes = info.durationMinutes

                    if (rawBearing == 0f && info.points.size >= 2) {
                        rawBearing = GoogleDirectionsHelper.calculateBearing(info.points[0], info.points[1])
                    }
                }
            }
        } else {
            fullRoutePoints = emptyList()
            remainingRoutePoints = emptyList()
            routeDistanceKm = 0.0
            routeEtaMinutes = 0
        }
    }

    // Live remaining distance calculation
    val liveRemainingKm = remember(remainingRoutePoints, routeDistanceKm) {
        if (remainingRoutePoints.size >= 2) {
            var totalMeters = 0.0
            for (i in 0 until remainingRoutePoints.size - 1) {
                totalMeters += GoogleDirectionsHelper.distanceInMeters(remainingRoutePoints[i], remainingRoutePoints[i + 1])
            }
            ((totalMeters / 1000.0) * 10).roundToInt() / 10.0
        } else {
            routeDistanceKm
        }
    }

    val liveEta = remember(liveRemainingKm) {
        (liveRemainingKm * 2.5).roundToInt().coerceAtLeast(1)
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(effectiveMitraGps, 15.5f)
    }

    var hasInitiallyFramedOrder by remember(activeOrder?.id) { mutableStateOf(false) }

    LaunchedEffect(activeOrder?.id) {
        if (activeOrder != null && !hasInitiallyFramedOrder) {
            try {
                val bounds = LatLngBounds.builder()
                    .include(customerLocation)
                    .include(effectiveMitraGps)
                    .build()
                cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 130))
                hasInitiallyFramedOrder = true
            } catch (_: Exception) {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(customerLocation, 15.5f))
            }
        } else if (activeOrder == null) {
            hasInitiallyFramedOrder = false
        }
    }

    // Top-down motorcycle vehicle marker icon
    val motorMarkerIcon = remember {
        try {
            createTopDownMotorcycleIcon()
        } catch (_: Exception) {
            try {
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
            } catch (_: Exception) {
                null
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Google Maps View
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = false,
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
            // Marker: Lokasi Mitra (Animated smoothly along coordinates and bearing)
            Marker(
                state = MarkerState(position = animatedMitraPos),
                title = "🛵 Posisi Anda (Mitra)",
                snippet = if (activeOrder != null && activeOrder.status != OrderStatus.INCOMING)
                    "Menuju ke lokasi ${activeOrder.client.name} (${liveRemainingKm} km)"
                else if (isOnline) "Siap Menerima Pesanan" else "Sedang Istirahat",
                icon = motorMarkerIcon,
                rotation = animatedBearing,
                flat = true,
                anchor = Offset(0.5f, 0.5f),
                zIndex = 2.0f
            )

            // Radius Radar Layanan Terkunci pada Posisi Mitra (Saat Standby)
            if (isOnline && activeOrder == null) {
                Circle(
                    center = animatedMitraPos,
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
                    snippet = activeOrder.client.address,
                    zIndex = 1.0f
                )

                val polylinePts = if (remainingRoutePoints.size >= 2) remainingRoutePoints else fullRoutePoints
                if (polylinePts.size >= 2) {
                    // Outer border
                    Polyline(
                        points = polylinePts,
                        color = Color(0xFF064E3B),
                        width = 16f,
                        jointType = JointType.ROUND,
                        startCap = RoundCap(),
                        endCap = RoundCap(),
                        zIndex = 0.5f
                    )
                    // Inner green line
                    Polyline(
                        points = polylinePts,
                        color = Color(0xFF10B981),
                        width = 11f,
                        jointType = JointType.ROUND,
                        startCap = RoundCap(),
                        endCap = RoundCap(),
                        zIndex = 0.6f
                    )
                }
            }
        }

        // Floating Recenter on Real GPS Button
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
                } catch (_: Exception) {
                    coroutineScope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(effectiveMitraGps, 16.5f)
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
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(Color(0xFFFEF08A), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TwoWheeler,
                            contentDescription = null,
                            tint = Color(0xFF854D0E),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Menuju ke klien • ${liveRemainingKm} km (~${liveEta} mnt)",
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

/**
 * Creates a high-DPI Top-Down 2D Motorcycle Vehicle Marker Bitmap
 * Facing UP (0°) so Google Maps `rotation = bearing` aligns it precisely with the road.
 */
private fun createTopDownMotorcycleIcon(): BitmapDescriptor {
    val width = 110
    val height = 110
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cx = width / 2f
    val cy = height / 2f

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 1. Soft Pulse Radar Shadow
    paint.color = android.graphics.Color.argb(45, 16, 185, 129)
    canvas.drawCircle(cx, cy, 50f, paint)

    // 2. Headlight Beam (Pointing UP)
    val beamPath = Path().apply {
        moveTo(cx, cy - 10f)
        lineTo(cx - 24f, cy - 50f)
        lineTo(cx + 24f, cy - 50f)
        close()
    }
    paint.color = android.graphics.Color.argb(80, 253, 224, 71)
    canvas.drawPath(beamPath, paint)

    // 3. Front & Rear Tires (Dark Slate)
    paint.color = android.graphics.Color.parseColor("#1E293B")
    canvas.drawRoundRect(RectF(cx - 4.5f, cy - 42f, cx + 4.5f, cy - 24f), 4f, 4f, paint)
    canvas.drawRoundRect(RectF(cx - 5.5f, cy + 18f, cx + 5.5f, cy + 38f), 5f, 5f, paint)

    // 4. Handlebars
    paint.color = android.graphics.Color.parseColor("#334155")
    paint.strokeWidth = 5f
    paint.style = Paint.Style.STROKE
    paint.strokeCap = Paint.Cap.ROUND
    canvas.drawLine(cx - 22f, cy - 22f, cx + 22f, cy - 22f, paint)

    // Side Mirrors
    paint.style = Paint.Style.FILL
    paint.color = android.graphics.Color.parseColor("#64748B")
    canvas.drawCircle(cx - 22f, cy - 24f, 3.5f, paint)
    canvas.drawCircle(cx + 22f, cy - 24f, 3.5f, paint)

    // 5. Motorcycle Chassis Body (MassaGo Bright Yellow / Amber)
    paint.color = android.graphics.Color.parseColor("#F59E0B")
    val chassisPath = Path().apply {
        moveTo(cx, cy - 28f)
        lineTo(cx + 11f, cy - 14f)
        lineTo(cx + 10f, cy + 20f)
        lineTo(cx - 10f, cy + 20f)
        lineTo(cx - 11f, cy - 14f)
        close()
    }
    canvas.drawPath(chassisPath, paint)

    // Front Fender Tip
    paint.color = android.graphics.Color.parseColor("#D97706")
    canvas.drawCircle(cx, cy - 26f, 4f, paint)

    // 6. Rider Helmet (MassaGo Emerald Green)
    paint.color = android.graphics.Color.parseColor("#059669")
    canvas.drawCircle(cx, cy - 2f, 13f, paint)

    // Visor / Face Shield (Dark gloss)
    paint.color = android.graphics.Color.parseColor("#0F172A")
    val visorRect = RectF(cx - 10f, cy - 14f, cx + 10f, cy - 4f)
    canvas.drawRoundRect(visorRect, 5f, 5f, paint)

    // Helmet highlight reflection
    paint.color = android.graphics.Color.argb(160, 255, 255, 255)
    canvas.drawCircle(cx + 4f, cy - 6f, 2.5f, paint)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}
