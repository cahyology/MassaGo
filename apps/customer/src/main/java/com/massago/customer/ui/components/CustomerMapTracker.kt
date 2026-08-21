package com.massago.customer.ui.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.massago.customer.ui.theme.EmeraldDark
import com.massago.customer.ui.theme.EmeraldPrimary
import com.massago.customer.util.GoogleDirectionsHelper
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@SuppressLint("MissingPermission")
@Composable
fun CustomerMapTracker(
    modifier: Modifier = Modifier,
    etaMinutes: Int = 10,
    customerLocation: LatLng = LatLng(-7.7956, 110.3695),
    therapistLocation: LatLng = LatLng(-7.8000, 110.3650),
    isSearching: Boolean = false
) {
    val coroutineScope = rememberCoroutineScope()

    // Smooth Coordinate Interpolation State
    var animatedTherapistPos by remember { mutableStateOf(therapistLocation) }
    var previousTherapistPos by remember { mutableStateOf(therapistLocation) }
    var rawBearing by remember { mutableFloatStateOf(0f) }

    val animatedBearing by animateFloatAsState(
        targetValue = rawBearing,
        animationSpec = tween(durationMillis = 800, easing = LinearEasing),
        label = "bearingAnim"
    )

    // Route state
    var fullRoutePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var remainingRoutePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var routeDistanceKm by remember { mutableDoubleStateOf(0.0) }
    var routeEtaMinutes by remember { mutableIntStateOf(etaMinutes.coerceAtLeast(1)) }

    // Interpolate therapist coordinate whenever therapistLocation updates
    LaunchedEffect(therapistLocation) {
        if (therapistLocation.latitude != 0.0 && therapistLocation.longitude != 0.0) {
            val dist = GoogleDirectionsHelper.distanceInMeters(previousTherapistPos, therapistLocation)
            if (dist > 1.5) {
                rawBearing = GoogleDirectionsHelper.calculateBearing(previousTherapistPos, therapistLocation)
            }

            val startPos = animatedTherapistPos
            val targetPos = therapistLocation
            val animProgress = Animatable(0f)

            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing)
            ) {
                val t = this.value
                val curLat = startPos.latitude + (targetPos.latitude - startPos.latitude) * t
                val curLng = startPos.longitude + (targetPos.longitude - startPos.longitude) * t
                val curLatLng = LatLng(curLat, curLng)
                animatedTherapistPos = curLatLng

                // Dynamically trim polyline in real-time as motorcycle glides forward
                if (fullRoutePoints.size >= 2) {
                    remainingRoutePoints = GoogleDirectionsHelper.trimPassedRoute(curLatLng, fullRoutePoints)
                }
            }

            previousTherapistPos = therapistLocation
        }
    }

    // Fetch initial route and recalculate when location deviates significantly
    LaunchedEffect(customerLocation, therapistLocation, isSearching) {
        if (isSearching) {
            fullRoutePoints = emptyList()
            remainingRoutePoints = emptyList()
            routeDistanceKm = 0.0
            routeEtaMinutes = 0
        } else {
            val shouldRecalculate = fullRoutePoints.isEmpty() ||
                    GoogleDirectionsHelper.distanceInMeters(therapistLocation, fullRoutePoints.firstOrNull() ?: therapistLocation) > 80.0

            if (shouldRecalculate) {
                val info = GoogleDirectionsHelper.getDrivingRouteInfo(therapistLocation, customerLocation)
                if (info.points.size >= 2) {
                    fullRoutePoints = info.points
                    remainingRoutePoints = GoogleDirectionsHelper.trimPassedRoute(animatedTherapistPos, info.points)
                    routeDistanceKm = info.distanceKm
                    routeEtaMinutes = info.durationMinutes

                    // If bearing is still 0, calculate along first segment
                    if (rawBearing == 0f && info.points.size >= 2) {
                        rawBearing = GoogleDirectionsHelper.calculateBearing(info.points[0], info.points[1])
                    }
                }
            }
        }
    }

    // Dynamic remaining distance calculation
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
        position = CameraPosition.fromLatLngZoom(customerLocation, 16f)
    }

    var hasAutoFramedOnce by remember { mutableStateOf(false) }

    LaunchedEffect(customerLocation, therapistLocation, isSearching) {
        if (!hasAutoFramedOnce && !isSearching) {
            try {
                val bounds = LatLngBounds.builder()
                    .include(customerLocation)
                    .include(therapistLocation)
                    .build()
                cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 130))
                hasAutoFramedOnce = true
            } catch (_: Exception) {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(customerLocation, 15.5f))
            }
        }
    }

    // Standalone High-Resolution Top-Down Motorcycle Icon
    val motorMarkerIcon = remember {
        try {
            createTopDownMotorcycleIcon()
        } catch (e: Exception) {
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
            // Marker: Lokasi Customer (Titik Antar)
            Marker(
                state = MarkerState(position = customerLocation),
                title = "📍 Lokasi Anda",
                snippet = "Titik Temu Pemijatan",
                zIndex = 1.0f
            )

            if (!isSearching) {
                // Realtime Moving Motorcycle Marker (Animated smoothly across coordinates and bearing)
                Marker(
                    state = MarkerState(position = animatedTherapistPos),
                    title = "🛵 Terapis Mitra MassaGo",
                    snippet = "Sisa jarak: ${liveRemainingKm} km (~${liveEta} mnt)",
                    icon = motorMarkerIcon,
                    rotation = animatedBearing,
                    flat = true,
                    anchor = Offset(0.5f, 0.5f),
                    zIndex = 2.0f
                )

                // Remaining Road Route Polyline (Gojek / Grab Green Line)
                val polylinePts = if (remainingRoutePoints.size >= 2) remainingRoutePoints else fullRoutePoints
                if (polylinePts.size >= 2) {
                    // Outer border for clear visibility on all map terrain
                    Polyline(
                        points = polylinePts,
                        color = Color(0xFF064E3B), // Dark Emerald Casing
                        width = 16f,
                        jointType = JointType.ROUND,
                        startCap = RoundCap(),
                        endCap = RoundCap(),
                        zIndex = 0.5f
                    )
                    // Inner glowing navigation line
                    Polyline(
                        points = polylinePts,
                        color = Color(0xFF10B981), // Vivid Emerald Green
                        width = 11f,
                        jointType = JointType.ROUND,
                        startCap = RoundCap(),
                        endCap = RoundCap(),
                        zIndex = 0.6f
                    )
                }
            }
        }

        // Top Floating Live ETA & Distance Pill (Ojol Gojek/Grab Style)
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
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFFEF08A), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TwoWheeler,
                            contentDescription = null,
                            tint = Color(0xFF854D0E),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "🛵 Mitra Menuju Lokasi Anda • ${liveRemainingKm} km (~${liveEta} mnt)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = EmeraldDark
                    )
                }
            }
        }

        // Floating Recenter Button (Fit Camera to show both Therapist & Customer)
        if (!isSearching) {
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        try {
                            val bounds = LatLngBounds.builder()
                                .include(customerLocation)
                                .include(animatedTherapistPos)
                                .build()
                            cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 130))
                        } catch (_: Exception) {
                            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(animatedTherapistPos, 16f))
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp),
                shape = CircleShape,
                containerColor = Color.White,
                contentColor = EmeraldPrimary,
                elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Pusatkan Lokasi",
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(24.dp)
                )
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
    // Front tire
    canvas.drawRoundRect(RectF(cx - 4.5f, cy - 42f, cx + 4.5f, cy - 24f), 4f, 4f, paint)
    // Rear tire
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
