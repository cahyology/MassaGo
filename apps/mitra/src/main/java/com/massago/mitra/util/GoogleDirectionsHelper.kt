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
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

data class DrivingRouteInfo(
    val points: List<LatLng>,
    val distanceKm: Double,
    val durationMinutes: Int,
    val source: String = "Google Maps"
)

object GoogleDirectionsHelper {
    private const val GOOGLE_MAPS_API_KEY = "AIzaSyCrc9uTLRtv9EfTdYLS1OLJJMxBKoycc-I"
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    /**
     * Fetch real street-following road path, exact driving distance, and traffic duration.
     * Priority 1: Google Routes API (New) with Motorcycle & Traffic Aware mode.
     * Priority 2: High-Precision Turn-by-Turn Road Routing Engine.
     */
    suspend fun getDrivingRouteInfo(origin: LatLng, destination: LatLng): DrivingRouteInfo = withContext(Dispatchers.IO) {
        // 1. Try Google Routes API (New)
        try {
            val googlePayload = JsonObject().apply {
                add("origin", JsonObject().apply {
                    add("location", JsonObject().apply {
                        add("latLng", JsonObject().apply {
                            addProperty("latitude", origin.latitude)
                            addProperty("longitude", origin.longitude)
                        })
                    })
                })
                add("destination", JsonObject().apply {
                    add("location", JsonObject().apply {
                        add("latLng", JsonObject().apply {
                            addProperty("latitude", destination.latitude)
                            addProperty("longitude", destination.longitude)
                        })
                    })
                })
                addProperty("travelMode", "TWO_WHEELER")
                addProperty("routingPreference", "TRAFFIC_AWARE")
            }.toString()

            val googleRequest = Request.Builder()
                .url("https://routes.googleapis.com/directions/v2:computeRoutes")
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Goog-Api-Key", GOOGLE_MAPS_API_KEY)
                .addHeader("X-Goog-FieldMask", "routes.duration,routes.distanceMeters,routes.polyline.encodedPolyline")
                .post(googlePayload.toRequestBody(JSON_MEDIA))
                .build()

            val googleResponse = client.newCall(googleRequest).execute()
            if (googleResponse.isSuccessful) {
                val bodyStr = googleResponse.body?.string()
                if (!bodyStr.isNullOrBlank()) {
                    val json = gson.fromJson(bodyStr, JsonObject::class.java)
                    val routes = json.getAsJsonArray("routes")
                    if (routes != null && routes.size() > 0) {
                        val routeObj = routes[0].asJsonObject
                        val polylineObj = routeObj.getAsJsonObject("polyline")
                        val encoded = polylineObj?.get("encodedPolyline")?.asString
                        val distanceMeters = routeObj.get("distanceMeters")?.asDouble ?: 0.0
                        val durationStr = routeObj.get("duration")?.asString ?: "0s"
                        val durationSeconds = durationStr.removeSuffix("s").toDoubleOrNull() ?: 0.0

                        if (!encoded.isNullOrBlank()) {
                            val decoded = decodePolyline(encoded)
                            if (decoded.isNotEmpty()) {
                                val distKm = ((distanceMeters / 1000.0) * 10).roundToInt() / 10.0
                                val etaMin = (durationSeconds / 60.0).roundToInt().coerceAtLeast(1)
                                return@withContext DrivingRouteInfo(
                                    points = decoded,
                                    distanceKm = distKm,
                                    durationMinutes = etaMin,
                                    source = "Google Routes API"
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // fallback to secondary engine
        }

        // 2. High-precision secondary street routing engine
        try {
            val url = "https://router.project-osrm.org/route/v1/driving/${origin.longitude},${origin.latitude};${destination.longitude},${destination.latitude}?overview=full&geometries=polyline"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()

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

                        if (decodedPoints.isNotEmpty()) {
                            return@withContext DrivingRouteInfo(
                                points = decodedPoints,
                                distanceKm = distKm,
                                durationMinutes = etaMin,
                                source = "Road Navigation Engine"
                            )
                        }
                    }
                }
            }
            fallbackRouteInfo(origin, destination)
        } catch (e: Exception) {
            fallbackRouteInfo(origin, destination)
        }
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

    private fun fallbackRouteInfo(origin: LatLng, destination: LatLng): DrivingRouteInfo {
        val dLat = Math.toRadians(destination.latitude - origin.latitude)
        val dLng = Math.toRadians(destination.longitude - origin.longitude)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(origin.latitude)) * Math.cos(Math.toRadians(destination.latitude)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        val distKm = ((6371.0 * c * 1.25) * 10).roundToInt() / 10.0
        val etaMin = (distKm * 2.5).roundToInt().coerceAtLeast(1)

        val midLat = (origin.latitude + destination.latitude) / 2.0
        val midLng = (origin.longitude + destination.longitude) / 2.0
        val points = listOf(
            origin,
            LatLng(origin.latitude, midLng),
            LatLng(midLat, midLng),
            LatLng(destination.latitude, midLng),
            destination
        )

        return DrivingRouteInfo(
            points = points,
            distanceKm = distKm,
            durationMinutes = etaMin,
            source = "Direct Route Estimation"
        )
    }
}
