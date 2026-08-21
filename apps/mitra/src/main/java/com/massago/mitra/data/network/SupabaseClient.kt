package com.massago.mitra.data.network

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object SupabaseConfig {
    const val URL = "https://jrwkmedrrwvomyljdkpw.supabase.co"
    const val ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Impyd2ttZWRycnd2b215bGpka3B3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODY5MTcxNzQsImV4cCI6MjEwMjQ5MzE3NH0.UiN6JvJt23ds-3eID9J6wOtEt3pg4-farSwQIliPzuw"
    val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
}

class SupabaseClient(
    private val baseUrl: String = SupabaseConfig.URL,
    private val anonKey: String = SupabaseConfig.ANON_KEY
) {
    val gson = Gson()

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectionPool(ConnectionPool(8, 2, TimeUnit.MINUTES))
        .dispatcher(Dispatcher(Executors.newFixedThreadPool(8)).apply {
            maxRequests = 16
            maxRequestsPerHost = 8
        })
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        })
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("apikey", anonKey)
                .header("Authorization", "Bearer $anonKey")
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .build()
            chain.proceed(request)
        }
        .build()

    companion object {
        val instance by lazy { SupabaseClient() }

        fun parseIsoOrEpochMillis(raw: Any?): Long {
            if (raw == null) return System.currentTimeMillis()
            if (raw is Number) {
                val v = raw.toLong()
                return if (v < 10_000_000_000L) v * 1000L else v
            }
            if (raw is String) {
                val num = raw.toDoubleOrNull()
                if (num != null) {
                    val v = num.toLong()
                    return if (v < 10_000_000_000L) v * 1000L else v
                }
                try {
                    val parser = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply {
                        timeZone = java.util.TimeZone.getTimeZone("UTC")
                    }
                    val clean = raw.take(19)
                    val d = parser.parse(clean)
                    if (d != null) return d.time
                } catch (_: Exception) {}
            }
            return System.currentTimeMillis()
        }
    }

    /**
     * Update Live Location ONLY (Without overriding duty status set by Admin)
     */
    suspend fun updateLocationOnly(
        therapistId: String,
        latitude: Double,
        longitude: Double
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (therapistId.isBlank()) return@withContext false
            val bodyJson = JsonObject().apply {
                addProperty("latitude", latitude)
                addProperty("longitude", longitude)
            }.toString()

            var clean = therapistId.replace("[^0-9]".toRegex(), "")
            if (clean.startsWith("0")) clean = "62" + clean.substring(1)
            else if (clean.startsWith("8")) clean = "62" + clean
            val localPhone = if (clean.startsWith("62")) "0" + clean.substring(2) else clean

            val queryParam = if (clean.length >= 8) {
                "or=(id.eq.$therapistId,phone.eq.$clean,phone.eq.$localPhone)"
            } else {
                "id=eq.$therapistId"
            }

            val request = Request.Builder()
                .url("$baseUrl/rest/v1/therapists?$queryParam")
                .patch(bodyJson.toRequestBody(SupabaseConfig.JSON_MEDIA))
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Update Duty Status of Therapist explicitly (ONLINE, ON_DUTY_BUSY, OFFLINE)
     */
    suspend fun updateDutyStatus(
        therapistId: String,
        isOnline: Boolean,
        dutyStatus: String = if (isOnline) "ONLINE" else "OFFLINE"
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (therapistId.isBlank()) return@withContext false
            val effectiveOnline = isOnline || (dutyStatus == "ON_DUTY_BUSY" || dutyStatus == "ONLINE")
            val bodyJson = JsonObject().apply {
                addProperty("is_online", effectiveOnline)
                addProperty("duty_status", dutyStatus)
            }.toString()

            var clean = therapistId.replace("[^0-9]".toRegex(), "")
            if (clean.startsWith("0")) clean = "62" + clean.substring(1)
            else if (clean.startsWith("8")) clean = "62" + clean
            val localPhone = if (clean.startsWith("62")) "0" + clean.substring(2) else clean

            val queryParam = if (clean.length >= 8) {
                "or=(id.eq.$therapistId,phone.eq.$clean,phone.eq.$localPhone)"
            } else {
                "id=eq.$therapistId"
            }

            val request = Request.Builder()
                .url("$baseUrl/rest/v1/therapists?$queryParam")
                .patch(bodyJson.toRequestBody(SupabaseConfig.JSON_MEDIA))
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Update Live Location & Duty Status of Therapist (Legacy fallback)
     */
    suspend fun updateLocationAndDuty(
        therapistId: String,
        latitude: Double,
        longitude: Double,
        isOnline: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (therapistId.isBlank()) return@withContext false
            val dutyStr = if (isOnline) "ONLINE" else "OFFLINE"
            val bodyJson = JsonObject().apply {
                addProperty("latitude", latitude)
                addProperty("longitude", longitude)
                addProperty("is_online", isOnline)
                addProperty("duty_status", dutyStr)
            }.toString()

            var clean = therapistId.replace("[^0-9]".toRegex(), "")
            if (clean.startsWith("0")) clean = "62" + clean.substring(1)
            else if (clean.startsWith("8")) clean = "62" + clean
            val localPhone = if (clean.startsWith("62")) "0" + clean.substring(2) else clean

            val queryParam = if (clean.length >= 8) {
                "or=(id.eq.$therapistId,phone.eq.$clean,phone.eq.$localPhone)"
            } else {
                "id=eq.$therapistId"
            }

            val request = Request.Builder()
                .url("$baseUrl/rest/v1/therapists?$queryParam")
                .patch(bodyJson.toRequestBody(SupabaseConfig.JSON_MEDIA))
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Fetch Active Service Packages from Database
     */
    suspend fun fetchServicePackages(): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/service_packages?select=*&order=orders_count.desc")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext emptyList()
                    val type = object : TypeToken<List<Map<String, Any>>>() {}.type
                    gson.fromJson(body, type)
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Fetch pending incoming orders from Supabase reliably
     */
    suspend fun fetchPendingOrders(therapistId: String = "", phone: String = ""): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            val query = "status=eq.PENDING&select=*&order=created_at.desc&limit=10"

            val request = Request.Builder()
                .url("$baseUrl/rest/v1/orders?$query")
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext emptyList()
                    val type = object : TypeToken<List<Map<String, Any>>>() {}.type
                    val orders: List<Map<String, Any>> = gson.fromJson(body, type)
                    orders
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Decline or Expire an unhandled order with reason
     */
    suspend fun declineOrder(orderId: String, reason: String = "Ditolak oleh mitra"): Boolean = withContext(Dispatchers.IO) {
        try {
            val bodyJson = JsonObject().apply {
                addProperty("status", "DECLINED")
                addProperty("cancellation_reason", reason)
                addProperty("notes", "[CANCEL_REASON:$reason]")
            }.toString()

            val request = Request.Builder()
                .url("$baseUrl/rest/v1/orders?id=eq.$orderId")
                .patch(bodyJson.toRequestBody(SupabaseConfig.JSON_MEDIA))
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Accept an incoming order atomically with therapist GPS coordinates
     */
    suspend fun acceptOrder(orderId: String, therapistId: String, therapistLat: Double = 0.0, therapistLng: Double = 0.0): Boolean = withContext(Dispatchers.IO) {
        val cleanOrderId = orderId.trim()
        val profile = com.massago.mitra.data.repository.TherapistRepository.instance.therapistProfile.value
        val effectiveTherapistId = therapistId.ifBlank { profile.id.ifBlank { profile.phone.ifBlank { "TRP-8821" } } }
        val lat = if (therapistLat != 0.0) therapistLat else profile.latitude
        val lng = if (therapistLng != 0.0) therapistLng else profile.longitude

        var isOrderUpdated = false

        // 1. Primary update on orders table in Supabase
        try {
            val bodyJson = JsonObject().apply {
                addProperty("status", "ACCEPTED_ON_THE_WAY")
                addProperty("therapist_id", effectiveTherapistId)
                if (lat != 0.0) addProperty("latitude", lat)
                if (lng != 0.0) addProperty("longitude", lng)
            }.toString()

            val request = Request.Builder()
                .url("$baseUrl/rest/v1/orders?id=eq.$cleanOrderId")
                .patch(bodyJson.toRequestBody(SupabaseConfig.JSON_MEDIA))
                .build()

            client.newCall(request).execute().use { response ->
                isOrderUpdated = response.isSuccessful
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Fallback update if first attempt failed
        if (!isOrderUpdated) {
            try {
                val simpleJson = JsonObject().apply {
                    addProperty("status", "ACCEPTED_ON_THE_WAY")
                    addProperty("therapist_id", effectiveTherapistId)
                }.toString()

                val simpleReq = Request.Builder()
                    .url("$baseUrl/rest/v1/orders?id=eq.$cleanOrderId")
                    .patch(simpleJson.toRequestBody(SupabaseConfig.JSON_MEDIA))
                    .build()

                client.newCall(simpleReq).execute().use { response ->
                    isOrderUpdated = response.isSuccessful
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 3. Update therapist location & duty separately
        try {
            if (effectiveTherapistId.isNotBlank()) {
                updateLocationAndDuty(effectiveTherapistId, lat, lng, isOnline = true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        isOrderUpdated
    }

    suspend fun updateTherapistGpsForOrder(orderId: String, lat: Double, lng: Double): Boolean = withContext(Dispatchers.IO) {
        try {
            val profile = com.massago.mitra.data.repository.TherapistRepository.instance.therapistProfile.value
            val identifier = profile.id.ifBlank { profile.phone }
            updateLocationOnly(identifier, lat, lng)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Update order status throughout therapy lifecycle
     */
    suspend fun updateOrderStatus(orderId: String, newStatus: String, cancellationReason: String = ""): Boolean = withContext(Dispatchers.IO) {
        try {
            val bodyJson = JsonObject().apply {
                addProperty("status", newStatus)
                if (cancellationReason.isNotBlank()) {
                    addProperty("cancellation_reason", cancellationReason)
                    addProperty("notes", "[CANCEL_REASON:$cancellationReason]")
                }
            }.toString()

            val request = Request.Builder()
                .url("$baseUrl/rest/v1/orders?id=eq.$orderId")
                .patch(bodyJson.toRequestBody(SupabaseConfig.JSON_MEDIA))
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Post SOS Emergency Alert to database & admin center
     */
    suspend fun sendSosAlert(
        orderId: String?,
        therapistId: String,
        lat: Double,
        lng: Double,
        notes: String = "Pemicu SOS Darurat Ditekan"
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val bodyJson = JsonObject().apply {
                addProperty("therapist_id", therapistId)
                if (orderId != null) addProperty("order_id", orderId)
                addProperty("latitude", lat)
                addProperty("longitude", lng)
                addProperty("status", "ACTIVE_EMERGENCY")
                addProperty("notes", notes)
            }.toString()

            val request = Request.Builder()
                .url("$baseUrl/rest/v1/sos_emergency_logs")
                .post(bodyJson.toRequestBody(SupabaseConfig.JSON_MEDIA))
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Fetch Single Order Data
     */
    suspend fun fetchOrder(orderId: String): Map<String, Any>? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/orders?id=eq.$orderId&select=*")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext null
                    val type = object : TypeToken<List<Map<String, Any>>>() {}.type
                    val list: List<Map<String, Any>> = gson.fromJson(body, type)
                    list.firstOrNull()
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Submit Review for Customer
     */
    suspend fun submitReview(
        orderId: String,
        reviewerType: String,
        reviewerId: String,
        targetId: String,
        rating: Int,
        tags: List<String>,
        reviewText: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val bodyJson = JsonObject().apply {
                addProperty("id", "REV-" + java.util.UUID.randomUUID().toString().take(8).uppercase())
                addProperty("order_id", orderId)
                addProperty("reviewer_type", reviewerType)
                addProperty("reviewer_id", reviewerId)
                addProperty("target_id", targetId)
                addProperty("rating", rating)
                val tagsArray = com.google.gson.JsonArray()
                tags.forEach { tagsArray.add(it) }
                add("tags", tagsArray)
                addProperty("review_text", reviewText)
            }.toString()

            val request = Request.Builder()
                .url("$baseUrl/rest/v1/reviews")
                .post(bodyJson.toRequestBody(SupabaseConfig.JSON_MEDIA))
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Fetch Active Platform Bank Accounts for Deposit Top-Up
     */
    suspend fun fetchBankAccounts(): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/platform_bank_accounts?is_active=eq.true&order=created_at.asc")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext emptyList()
                    val type = object : TypeToken<List<Map<String, Any>>>() {}.type
                    gson.fromJson(body, type)
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Fetch Platform Settings (QRIS info, WhatsApp CS, Commission)
     */
    suspend fun fetchPlatformSettings(): Map<String, String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/platform_settings?select=*")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext emptyMap()
                    val type = object : TypeToken<List<Map<String, Any>>>() {}.type
                    val list: List<Map<String, Any>> = gson.fromJson(body, type)
                    val map = mutableMapOf<String, String>()
                    list.forEach { item ->
                        val key = item["key"] as? String
                        val value = item["value"] as? String
                        if (key != null && value != null) {
                            map[key] = value
                        }
                    }
                    map
                } else {
                    emptyMap()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }
    }

    /**
     * Create DOKU Checkout Session URL for Mitra Deposit Top-Up
     */
    suspend fun createDokuDepositSession(
        therapistId: String,
        therapistName: String,
        amount: Long
    ): String? = withContext(Dispatchers.IO) {
        try {
            val settings = fetchPlatformSettings()
            val clientId = settings["doku_client_id"] ?: "BRN-0242-1787022128265"
            val secretKey = settings["doku_secret_key"] ?: "SK-v7V59V0cdjBexAHtq4Xd"
            val isProduction = settings["doku_is_production"] != "false"
            val dokuBaseUrl = if (isProduction) "https://api.doku.com" else "https://api-sandbox.doku.com"
            val targetPath = "/checkout/v1/payment"
            val invoiceNumber = "TOPUP-${System.currentTimeMillis()}-${therapistId.takeLast(4)}"

            val payload = JsonObject().apply {
                add("order", JsonObject().apply {
                    addProperty("amount", amount)
                    addProperty("invoice_number", invoiceNumber)
                    addProperty("currency", "IDR")
                    addProperty("callback_url", "https://massago.com/api/doku/notify")
                    addProperty("auto_redirect", true)
                })
                add("payment", JsonObject().apply {
                    addProperty("payment_due_date", 60)
                })
                add("customer", JsonObject().apply {
                    addProperty("id", therapistId)
                    addProperty("name", therapistName.ifBlank { "Mitra MassaGo" })
                    addProperty("phone", "+6281234567890")
                })
            }

            val rawBody = gson.toJson(payload)
            val requestId = "REQ-${System.currentTimeMillis()}"
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            val requestTimestamp = sdf.format(java.util.Date())

            val md = java.security.MessageDigest.getInstance("SHA-256")
            val digest = android.util.Base64.encodeToString(md.digest(rawBody.toByteArray(Charsets.UTF_8)), android.util.Base64.NO_WRAP)

            val signatureComponent = "Client-Id:$clientId\nRequest-Id:$requestId\nRequest-Timestamp:$requestTimestamp\nRequest-Target:$targetPath\nDigest:$digest"
            val mac = javax.crypto.Mac.getInstance("HmacSHA256")
            mac.init(javax.crypto.spec.SecretKeySpec(secretKey.toByteArray(Charsets.UTF_8), "HmacSHA256"))
            val signature = android.util.Base64.encodeToString(mac.doFinal(signatureComponent.toByteArray(Charsets.UTF_8)), android.util.Base64.NO_WRAP)

            val req = Request.Builder()
                .url("$dokuBaseUrl$targetPath")
                .addHeader("Content-Type", "application/json")
                .addHeader("Client-Id", clientId)
                .addHeader("Request-Id", requestId)
                .addHeader("Request-Timestamp", requestTimestamp)
                .addHeader("Signature", "HMACSHA256=$signature")
                .post(rawBody.toRequestBody(SupabaseConfig.JSON_MEDIA))
                .build()

            client.newCall(req).execute().use { res ->
                if (res.isSuccessful) {
                    val respBody = res.body?.string() ?: return@withContext null
                    val obj = gson.fromJson(respBody, JsonObject::class.java)
                    obj.getAsJsonObject("response")?.getAsJsonObject("payment")?.get("url")?.asString
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Submit SOS Emergency Alert to Supabase sos_emergency_logs
     */
    suspend fun sendSosAlert(
        senderType: String,
        senderId: String,
        senderName: String,
        senderPhone: String,
        orderId: String?,
        latitude: Double?,
        longitude: Double?,
        emergencyType: String = "EMERGENCY_ASSISTANCE",
        notes: String = "Panggilan Darurat Terapis MassaGo"
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = JsonObject().apply {
                addProperty("sender_type", senderType)
                addProperty("sender_id", senderId)
                addProperty("sender_name", senderName)
                addProperty("sender_phone", senderPhone)
                if (orderId != null) addProperty("order_id", orderId)
                if (latitude != null) addProperty("latitude", latitude)
                if (longitude != null) addProperty("longitude", longitude)
                addProperty("emergency_type", emergencyType)
                addProperty("status", "ACTIVE_EMERGENCY")
                addProperty("notes", notes)
            }
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/sos_emergency_logs")
                .post(payload.toString().toRequestBody(SupabaseConfig.JSON_MEDIA))
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Fetch order history for therapist from Supabase
     */
    suspend fun fetchTherapistOrders(therapistId: String, therapistPhone: String): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            var cleanPhone = therapistPhone.replace("[^0-9]".toRegex(), "")
            if (cleanPhone.startsWith("0")) cleanPhone = "62" + cleanPhone.substring(1)
            else if (cleanPhone.startsWith("8")) cleanPhone = "62" + cleanPhone
            val localPhone = if (cleanPhone.startsWith("62")) "0" + cleanPhone.substring(2) else cleanPhone
            val plusPhone = if (cleanPhone.startsWith("62")) "+$cleanPhone" else "+62$cleanPhone"
            val raw8Phone = if (cleanPhone.startsWith("62")) cleanPhone.substring(2) else cleanPhone

            if (therapistId.isBlank() && cleanPhone.isBlank()) {
                return@withContext emptyList()
            }

            val query = if (therapistId.isNotBlank() && cleanPhone.isNotBlank()) {
                "or=(therapist_id.eq.$therapistId,therapist_id.eq.$cleanPhone,therapist_id.eq.$localPhone,therapist_id.eq.$plusPhone,therapist_id.eq.$raw8Phone)"
            } else if (therapistId.isNotBlank()) {
                "therapist_id=eq.$therapistId"
            } else {
                "or=(therapist_id.eq.$cleanPhone,therapist_id.eq.$localPhone,therapist_id.eq.$plusPhone,therapist_id.eq.$raw8Phone)"
            }

            val request = Request.Builder()
                .url("$baseUrl/rest/v1/orders?$query&order=created_at.desc&limit=50")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val json = response.body?.string() ?: return@withContext emptyList()
                    val listType = object : TypeToken<List<Map<String, Any>>>() {}.type
                    gson.fromJson(json, listType) ?: emptyList()
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
