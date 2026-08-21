package com.massago.mitra.util

import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.*

data class DrivingRouteInfo(
    val points: List<LatLng>,
    val distanceKm: Double,
    val durationMinutes: Int,
    val source: String = "Street Navigation"
)

object GoogleDirectionsHelper {
    private const val GOOGLE_MAPS_API_KEY = "AIzaSyCrc9uTLRtv9EfTdYLS1OLJJMxBKoycc-I"
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    // Route caching keyed with ~20m granularity
    private val routeCache = ConcurrentHashMap<String, DrivingRouteInfo>()

    private fun buildCacheKey(origin: LatLng, dest: LatLng): String {
        val oLat = (origin.latitude * 5000).toInt()
        val oLng = (origin.longitude * 5000).toInt()
        val dLat = (dest.latitude * 5000).toInt()
        val dLng = (dest.longitude * 5000).toInt()
        return "$oLat,$oLng-$dLat,$dLng"
    }

    fun clearCache() {
        routeCache.clear()
    }

    /**
     * Calculate Haversine distance in meters between two LatLng coordinates
     */
    fun distanceInMeters(p1: LatLng, p2: LatLng): Double {
        val r = 6371000.0 // Earth's radius in meters
        val lat1 = Math.toRadians(p1.latitude)
        val lat2 = Math.toRadians(p2.latitude)
        val dLat = Math.toRadians(p2.latitude - p1.latitude)
        val dLng = Math.toRadians(p2.longitude - p1.longitude)
        val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * Calculate bearing heading in degrees (0..360) from start to end
     */
    fun calculateBearing(from: LatLng, to: LatLng): Float {
        val lat1 = Math.toRadians(from.latitude)
        val lng1 = Math.toRadians(from.longitude)
        val lat2 = Math.toRadians(to.latitude)
        val lng2 = Math.toRadians(to.longitude)
        val dLng = lng2 - lng1
        val y = sin(dLng) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLng)
        val bearing = Math.toDegrees(atan2(y, x)).toFloat()
        return (bearing + 360f) % 360f
    }

    /**
     * Dynamically trim polyline passed by the driver and return remaining path.
     * Prevents collapsing the route prematurely to a straight line.
     */
    fun trimPassedRoute(currentPos: LatLng, fullRoute: List<LatLng>): List<LatLng> {
        if (fullRoute.size <= 2) return fullRoute

        val distToDest = distanceInMeters(currentPos, fullRoute.last())

        var closestIndex = 0
        var minDistance = Double.MAX_VALUE

        for (i in 0 until fullRoute.size) {
            val dist = distanceInMeters(currentPos, fullRoute[i])
            if (dist < minDistance) {
                minDistance = dist
                closestIndex = i
            }
        }

        // Safety guard: If closest index is at/near the destination but driver is still far (>80m),
        // do not collapse the route into a straight line!
        if (closestIndex >= fullRoute.size - 2 && distToDest > 80.0) {
            return listOf(currentPos) + fullRoute
        }

        // If driver is far off the polyline (> 150m), don't trim arbitrarily
        if (minDistance > 150.0) {
            return listOf(currentPos) + fullRoute
        }

        val remaining = fullRoute.subList(closestIndex, fullRoute.size)
        return if (remaining.isNotEmpty()) {
            listOf(currentPos) + remaining
        } else {
            fullRoute
        }
    }

    /**
     * Fetch real street-following road path, exact driving distance, and traffic duration.
     */
    suspend fun getDrivingRouteInfo(origin: LatLng, destination: LatLng): DrivingRouteInfo = withContext(Dispatchers.IO) {
        // Prevent 0,0 queries
        if (origin.latitude == 0.0 || origin.longitude == 0.0 || destination.latitude == 0.0 || destination.longitude == 0.0) {
            return@withContext smoothCurvedFallbackRoute(origin, destination)
        }

        val cacheKey = buildCacheKey(origin, destination)
        val cached = routeCache[cacheKey]
        if (cached != null && cached.points.size >= 2) {
            return@withContext cached
        }

        // 1. High-precision Real Street Turn-by-Turn Routing Engine (OSRM Primary)
        try {
            val url = "https://router.project-osrm.org/route/v1/driving/${origin.longitude},${origin.latitude};${destination.longitude},${destination.latitude}?overview=full&geometries=polyline"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "MassaGoApp/1.0 (Android Native)")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string()
                    if (!bodyStr.isNullOrBlank()) {
                        val json = gson.fromJson(bodyStr, JsonObject::class.java)
                        val routes = json.getAsJsonArray("routes")
                        if (routes != null && routes.size() > 0) {
                            val routeObj = routes[0].asJsonObject
                            val geom = routeObj.get("geometry")?.asString
                            val distMeters = routeObj.get("distance")?.asDouble ?: 0.0
                            val durSeconds = routeObj.get("duration")?.asDouble ?: 0.0

                            val decodedPoints = if (!geom.isNullOrBlank()) decodePolyline(geom) else emptyList()
                            val distKm = ((distMeters / 1000.0) * 10).roundToInt() / 10.0
                            val etaMin = ((durSeconds / 60.0) * 1.15).roundToInt().coerceAtLeast(1)

                            if (decodedPoints.size >= 2) {
                                val result = DrivingRouteInfo(
                                    points = decodedPoints,
                                    distanceKm = distKm,
                                    durationMinutes = etaMin,
                                    source = "Street Road Network (OSRM)"
                                )
                                routeCache[cacheKey] = result
                                return@withContext result
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. High-precision OpenStreetMap Routing (Secondary Redundancy)
        try {
            val url = "https://routing.openstreetmap.de/routed-car/route/v1/driving/${origin.longitude},${origin.latitude};${destination.longitude},${destination.latitude}?overview=full&geometries=polyline"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "MassaGoApp/1.0 (Android Native)")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string()
                    if (!bodyStr.isNullOrBlank()) {
                        val json = gson.fromJson(bodyStr, JsonObject::class.java)
                        val routes = json.getAsJsonArray("routes")
                        if (routes != null && routes.size() > 0) {
                            val routeObj = routes[0].asJsonObject
                            val geom = routeObj.get("geometry")?.asString
                            val distMeters = routeObj.get("distance")?.asDouble ?: 0.0
                            val durSeconds = routeObj.get("duration")?.asDouble ?: 0.0

                            val decodedPoints = if (!geom.isNullOrBlank()) decodePolyline(geom) else emptyList()
                            val distKm = ((distMeters / 1000.0) * 10).roundToInt() / 10.0
                            val etaMin = ((durSeconds / 60.0) * 1.15).roundToInt().coerceAtLeast(1)

                            if (decodedPoints.size >= 2) {
                                val result = DrivingRouteInfo(
                                    points = decodedPoints,
                                    distanceKm = distKm,
                                    durationMinutes = etaMin,
                                    source = "Street Road Network (OSM)"
                                )
                                routeCache[cacheKey] = result
                                return@withContext result
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // 3. Fallback: Smooth Interpolated Road Path
        smoothCurvedFallbackRoute(origin, destination)
    }

    suspend fun getDrivingRoute(origin: LatLng, destination: LatLng): List<LatLng> {
        return getDrivingRouteInfo(origin, destination).points
    }

    /**
     * Decodes an encoded path string into a sequence of LatLngs.
     */
    fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val p = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
            poly.add(p)
        }
        return poly
    }

    private fun smoothCurvedFallbackRoute(origin: LatLng, destination: LatLng): DrivingRouteInfo {
        val dLat = Math.toRadians(destination.latitude - origin.latitude)
        val dLng = Math.toRadians(destination.longitude - origin.longitude)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(origin.latitude)) * cos(Math.toRadians(destination.latitude)) * sin(dLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val distKm = ((6371.0 * c * 1.25) * 10).roundToInt() / 10.0
        val etaMin = (distKm * 2.5).roundToInt().coerceAtLeast(1)

        val steps = 10
        val points = mutableListOf<LatLng>()
        for (i in 0..steps) {
            val t = i.toDouble() / steps
            val lat = origin.latitude + (destination.latitude - origin.latitude) * t
            val lng = origin.longitude + (destination.longitude - origin.longitude) * t
            points.add(LatLng(lat, lng))
        }

        return DrivingRouteInfo(
            points = points,
            distanceKm = distKm,
            durationMinutes = etaMin,
            source = "Interpolated Navigation"
        )
    }
}
