package com.massago.mitra.data.network

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
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
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .build()
            chain.proceed(request)
        }
        .build()

    companion object {
        val instance by lazy { SupabaseClient() }
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

            val request = Request.Builder()
                .url("$baseUrl/rest/v1/therapists?id=eq.$therapistId")
                .patch(bodyJson.toRequestBody(SupabaseConfig.JSON_MEDIA))
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Update Duty Status of Therapist explicitly
     */
    suspend fun updateDutyStatus(
        therapistId: String,
        isOnline: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (therapistId.isBlank()) return@withContext false
            val bodyJson = JsonObject().apply {
                addProperty("is_online", isOnline)
                addProperty("duty_status", if (isOnline) "ONLINE" else "OFFLINE")
            }.toString()

            val request = Request.Builder()
                .url("$baseUrl/rest/v1/therapists?id=eq.$therapistId")
                .patch(bodyJson.toRequestBody(SupabaseConfig.JSON_MEDIA))
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
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
            val bodyJson = JsonObject().apply {
                addProperty("latitude", latitude)
                addProperty("longitude", longitude)
                addProperty("is_online", isOnline)
                addProperty("duty_status", if (isOnline) "ONLINE" else "OFFLINE")
            }.toString()

            val request = Request.Builder()
                .url("$baseUrl/rest/v1/therapists?id=eq.$therapistId")
                .patch(bodyJson.toRequestBody(SupabaseConfig.JSON_MEDIA))
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
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

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return@withContext emptyList()
                val type = object : TypeToken<List<Map<String, Any>>>() {}.type
                gson.fromJson(body, type)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Fetch pending incoming orders from Supabase reliably
     */
    suspend fun fetchPendingOrders(): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/orders?status=eq.PENDING&therapist_id=is.null&select=*&order=created_at.desc&limit=1")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return@withContext emptyList()
                val type = object : TypeToken<List<Map<String, Any>>>() {}.type
                val orders: List<Map<String, Any>> = gson.fromJson(body, type)
                orders
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Decline or Expire an unhandled order
     */
    suspend fun declineOrder(orderId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val bodyJson = JsonObject().apply {
                addProperty("status", "DECLINED")
            }.toString()

            val request = Request.Builder()
                .url("$baseUrl/rest/v1/orders?id=eq.$orderId")
                .patch(bodyJson.toRequestBody(SupabaseConfig.JSON_MEDIA))
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Accept an incoming order atomically with therapist GPS coordinates
     */
    suspend fun acceptOrder(orderId: String, therapistId: String, therapistLat: Double = 0.0, therapistLng: Double = 0.0): Boolean = withContext(Dispatchers.IO) {
        try {
            val profile = com.massago.mitra.data.repository.TherapistRepository.instance.therapistProfile.value
            val lat = if (therapistLat != 0.0) therapistLat else profile.latitude
            val lng = if (therapistLng != 0.0) therapistLng else profile.longitude

            updateLocationAndDuty(therapistId, lat, lng, isOnline = true)

            val bodyJson = JsonObject().apply {
                addProperty("therapist_id", therapistId)
                addProperty("status", "ACCEPTED_ON_THE_WAY")
            }.toString()

            val request = Request.Builder()
                .url("$baseUrl/rest/v1/orders?id=eq.$orderId")
                .patch(bodyJson.toRequestBody(SupabaseConfig.JSON_MEDIA))
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun updateTherapistGpsForOrder(orderId: String, lat: Double, lng: Double): Boolean = withContext(Dispatchers.IO) {
        try {
            val therapistId = com.massago.mitra.data.repository.TherapistRepository.instance.therapistProfile.value.id
            updateLocationAndDuty(therapistId, lat, lng, isOnline = true)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Update order status throughout therapy lifecycle
     */
    suspend fun updateOrderStatus(orderId: String, newStatus: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val bodyJson = JsonObject().apply {
                addProperty("status", newStatus)
            }.toString()

            val request = Request.Builder()
                .url("$baseUrl/rest/v1/orders?id=eq.$orderId")
                .patch(bodyJson.toRequestBody(SupabaseConfig.JSON_MEDIA))
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
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

            val response = client.newCall(request).execute()
            response.isSuccessful
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

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return@withContext null
                val type = object : TypeToken<List<Map<String, Any>>>() {}.type
                val list: List<Map<String, Any>> = gson.fromJson(body, type)
                list.firstOrNull()
            } else {
                null
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

            val response = client.newCall(request).execute()
            response.isSuccessful
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

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return@withContext emptyList()
                val type = object : TypeToken<List<Map<String, Any>>>() {}.type
                gson.fromJson(body, type)
            } else {
                emptyList()
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

            val response = client.newCall(request).execute()
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
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }
    }

    private val rawClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

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
            val baseUrl = if (isProduction) "https://api.doku.com" else "https://api-sandbox.doku.com"
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
                .url("$baseUrl$targetPath")
                .addHeader("Content-Type", "application/json")
                .addHeader("Client-Id", clientId)
                .addHeader("Request-Id", requestId)
                .addHeader("Request-Timestamp", requestTimestamp)
                .addHeader("Signature", "HMACSHA256=$signature")
                .post(rawBody.toRequestBody(SupabaseConfig.JSON_MEDIA))
                .build()

            val res = rawClient.newCall(req).execute()
            if (res.isSuccessful) {
                val respBody = res.body?.string() ?: return@withContext null
                val obj = gson.fromJson(respBody, JsonObject::class.java)
                obj.getAsJsonObject("response")?.getAsJsonObject("payment")?.get("url")?.asString
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}


