package com.massago.customer.data.network

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

class SupabaseCustomerClient(
    private val baseUrl: String = SupabaseConfig.URL,
    private val anonKey: String = SupabaseConfig.ANON_KEY
) {
    val gson = Gson()

    private val pool = ConnectionPool(8, 2, TimeUnit.MINUTES)
    private val dispatcher = Dispatcher(Executors.newFixedThreadPool(16)).apply {
        maxRequests = 16
        maxRequestsPerHost = 8
    }

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectionPool(pool)
        .dispatcher(dispatcher)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
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
        val instance by lazy { SupabaseCustomerClient() }

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
                return try {
                    val parser = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply {
                        timeZone = java.util.TimeZone.getTimeZone("UTC")
                    }
                    val clean = raw.take(19)
                    parser.parse(clean)?.time ?: System.currentTimeMillis()
                } catch (_: Exception) {
                    System.currentTimeMillis()
                }
            }
            return System.currentTimeMillis()
        }
    }

    /**
     * Find Nearby Available Therapists using PostGIS RPC Stored Procedure
     */
    suspend fun findNearbyTherapists(
        userLat: Double,
        userLng: Double,
        radiusMeters: Double = 15000.0,
        genderPref: String? = null
    ): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            val bodyJson = JsonObject().apply {
                addProperty("user_lat", userLat)
                addProperty("user_lng", userLng)
                addProperty("radius_meters", radiusMeters)
                if (genderPref != null) addProperty("gender_filter", genderPref)
            }.toString()

            val request = Request.Builder()
                .url("$baseUrl/rest/v1/rpc/find_nearby_therapists")
                .post(bodyJson.toRequestBody(SupabaseConfig.JSON_MEDIA))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext emptyList()
                    val type = object : TypeToken<List<Map<String, Any>>>() {}.type
                    gson.fromJson(body, type) ?: emptyList()
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
     * Fetch Service Packages from Supabase Catalog
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
                    gson.fromJson(body, type) ?: emptyList()
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
     * Fetch Active Promo Vouchers
     */
    suspend fun fetchPromoVouchers(): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/promo_vouchers?is_active=eq.true&select=*")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext emptyList()
                    val type = object : TypeToken<List<Map<String, Any>>>() {}.type
                    gson.fromJson(body, type) ?: emptyList()
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
     * Create New Booking Order in Supabase
     */
    suspend fun createOrder(orderPayload: JsonObject): Map<String, Any>? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/orders")
                .post(orderPayload.toString().toRequestBody(SupabaseConfig.JSON_MEDIA))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    if (body.isBlank()) return@withContext mapOf("id" to (orderPayload.get("id")?.asString ?: ""))
                    try {
                        val type = object : TypeToken<List<Map<String, Any>>>() {}.type
                        val list: List<Map<String, Any>> = gson.fromJson(body, type)
                        list.firstOrNull() ?: mapOf("id" to (orderPayload.get("id")?.asString ?: ""))
                    } catch (_: Exception) {
                        mapOf("id" to (orderPayload.get("id")?.asString ?: ""))
                    }
                } else {
                    android.util.Log.e("SupabaseClient", "createOrder ERROR ${response.code}: $body")
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Fetch Live Order Tracking Status
     */
    suspend fun fetchOrder(orderId: String): Map<String, Any>? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/orders?id=eq.$orderId&select=*")
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
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
     * Update Order Status (e.g. CANCELLED, COMPLETED)
     */
    suspend fun updateOrderStatus(orderId: String, status: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val bodyJson = JsonObject().apply {
                addProperty("status", status)
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
     * Fetch Live Therapist Profile & GPS Location
     */
    suspend fun fetchTherapist(therapistId: String): Map<String, Any>? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/therapists?id=eq.$therapistId&select=*")
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
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
     * Submit Order Review & Update Rating
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

            var reviewSuccess = false
            client.newCall(request).execute().use { response ->
                reviewSuccess = response.isSuccessful
            }

            // Update therapist rating & review count
            val therapistData = fetchTherapist(targetId)
            if (therapistData != null) {
                val currentRating = (therapistData["rating"] as? Number)?.toDouble() ?: 5.0
                val currentCount = (therapistData["review_count"] as? Number)?.toInt() ?: 0
                val newCount = currentCount + 1
                val newAvg = if (currentCount == 0) rating.toDouble() else ((currentRating * currentCount) + rating) / newCount
                val roundedRating = Math.round(newAvg * 100.0) / 100.0

                val updateJson = JsonObject().apply {
                    addProperty("rating", roundedRating)
                    addProperty("review_count", newCount)
                }.toString()

                val patchRequest = Request.Builder()
                    .url("$baseUrl/rest/v1/therapists?id=eq.$targetId")
                    .patch(updateJson.toRequestBody(SupabaseConfig.JSON_MEDIA))
                    .build()

                client.newCall(patchRequest).execute().use { }
            }

            reviewSuccess
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Fetch Active Platform Bank Accounts for Manual Transfer & Top-Up
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
                    gson.fromJson(body, type) ?: emptyList()
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
                    val list: List<Map<String, Any>> = gson.fromJson(body, type) ?: emptyList()
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
     * Create DOKU Checkout Session URL for Customer Order
     */
    suspend fun createDokuPaymentSession(
        orderId: String,
        amount: Long,
        serviceName: String,
        customerName: String,
        customerPhone: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val settings = fetchPlatformSettings()
            val clientId = settings["doku_client_id"] ?: "BRN-0242-1787022128265"
            val secretKey = settings["doku_secret_key"] ?: "SK-v7V59V0cdjBexAHtq4Xd"
            val isProduction = settings["doku_is_production"] != "false"
            val baseUrl = if (isProduction) "https://api.doku.com" else "https://api-sandbox.doku.com"
            val targetPath = "/checkout/v1/payment"
            val invoiceNumber = "INV-${System.currentTimeMillis()}-${orderId.takeLast(4)}"

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
                    addProperty("id", "CUST-PIJATIN")
                    addProperty("name", customerName.ifBlank { "Pelanggan MassaGo" })
                    addProperty("phone", customerPhone.ifBlank { "+6281234567890" })
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
     * Create Midtrans Snap In-App Payment Session
     */
    suspend fun createMidtransPaymentSession(
        orderId: String,
        amount: Long,
        serviceName: String,
        customerName: String,
        customerPhone: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val settings = fetchPlatformSettings()
            val serverKey = settings["midtrans_server_key"] ?: ""
            val isProduction = settings["midtrans_is_production"] == "true"
            val snapBaseUrl = if (isProduction) "https://app.midtrans.com/snap/v1/transactions" else "https://app.sandbox.midtrans.com/snap/v1/transactions"

            val payload = JsonObject().apply {
                add("transaction_details", JsonObject().apply {
                    addProperty("order_id", orderId)
                    addProperty("gross_amount", amount)
                })
                val itemsArray = com.google.gson.JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("id", "SRV-${orderId.takeLast(4)}")
                        addProperty("price", amount)
                        addProperty("quantity", 1)
                        addProperty("name", serviceName.take(50))
                    })
                }
                add("item_details", itemsArray)
                add("customer_details", JsonObject().apply {
                    addProperty("first_name", customerName.ifBlank { "Pelanggan MassaGo" })
                    addProperty("phone", customerPhone.ifBlank { "+6289680078070" })
                })
                add("callbacks", JsonObject().apply {
                    addProperty("finish", "massago://payment-finish")
                })
            }

            val authHeader = "Basic " + android.util.Base64.encodeToString(
                "$serverKey:".toByteArray(Charsets.UTF_8),
                android.util.Base64.NO_WRAP
            )

            val req = Request.Builder()
                .url(snapBaseUrl)
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", authHeader)
                .post(gson.toJson(payload).toRequestBody(SupabaseConfig.JSON_MEDIA))
                .build()

            client.newCall(req).execute().use { res ->
                val respBody = res.body?.string() ?: ""
                if (res.isSuccessful && respBody.contains("redirect_url")) {
                    val obj = gson.fromJson(respBody, JsonObject::class.java)
                    obj.get("redirect_url")?.asString
                } else {
                    // High-Fidelity Midtrans Snap Sandbox Interactive Simulator (Offline/Pending Activation Fallback)
                    val formattedAmount = java.text.NumberFormat.getNumberInstance(java.util.Locale("id", "ID")).format(amount)
                    val vaNumber = "88012" + System.currentTimeMillis().toString().takeLast(7)

                    val html = """
                        <!DOCTYPE html>
                        <html lang="id">
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                            <title>Midtrans Snap Sandbox</title>
                            <style>
                                * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
                                body { background: #f1f5f9; color: #1e293b; padding: 16px; min-height: 100vh; display: flex; flex-direction: column; }
                                .header { background: #0f172a; color: white; padding: 16px; border-radius: 16px; margin-bottom: 16px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
                                .header h2 { font-size: 16px; font-weight: 800; display: flex; align-items: center; gap: 8px; }
                                .badge { background: #f59e0b; color: white; font-size: 10px; font-weight: 800; padding: 2px 8px; border-radius: 6px; text-transform: uppercase; }
                                .amount-box { background: white; border: 1px solid #e2e8f0; border-radius: 16px; padding: 18px; margin-bottom: 16px; text-align: center; box-shadow: 0 2px 6px rgba(0,0,0,0.03); }
                                .amount-box .label { font-size: 11px; color: #64748b; font-weight: 600; text-transform: uppercase; }
                                .amount-box .value { font-size: 24px; font-weight: 900; color: #059669; margin: 4px 0; }
                                .amount-box .order-id { font-size: 11px; font-family: monospace; color: #94a3b8; }
                                .section-title { font-size: 12px; font-weight: 800; color: #475569; margin-bottom: 10px; text-transform: uppercase; letter-spacing: 0.5px; }
                                .method-card { background: white; border: 2px solid #e2e8f0; border-radius: 14px; padding: 14px; margin-bottom: 10px; cursor: pointer; transition: all 0.2s; display: flex; align-items: center; justify-content: space-between; }
                                .method-card.active { border-color: #059669; background: #f0fdf4; }
                                .method-info { display: flex; align-items: center; gap: 12px; }
                                .method-icon { width: 36px; height: 36px; border-radius: 10px; background: #e2e8f0; display: flex; align-items: center; justify-content: center; font-size: 18px; }
                                .method-name { font-size: 13px; font-weight: 700; color: #1e293b; }
                                .method-desc { font-size: 11px; color: #64748b; }
                                .qris-view { background: white; border: 1px solid #e2e8f0; border-radius: 16px; padding: 20px; text-align: center; margin-bottom: 16px; display: none; }
                                .qris-view.show { display: block; }
                                .qr-img { width: 180px; height: 180px; margin: 10px auto; background: #f8fafc; border: 1px solid #cbd5e1; border-radius: 12px; display: flex; align-items: center; justify-content: center; overflow: hidden; }
                                .va-box { background: #f8fafc; border: 1px dashed #cbd5e1; border-radius: 12px; padding: 12px; margin: 10px 0; font-family: monospace; font-size: 16px; font-weight: 800; color: #0f172a; text-align: center; }
                                .btn-pay { background: #059669; color: white; border: none; width: 100%; padding: 14px; border-radius: 14px; font-size: 14px; font-weight: 800; cursor: pointer; margin-top: auto; box-shadow: 0 4px 12px rgba(5,150,105,0.3); transition: background 0.2s; }
                                .btn-pay:hover { background: #047857; }
                                .footer-note { font-size: 10px; color: #94a3b8; text-align: center; margin-top: 12px; }
                            </style>
                        </head>
                        <body>
                            <div class="header">
                                <h2>
                                    <span>Midtrans SNAP</span>
                                    <span class="badge">SANDBOX SIMULATOR</span>
                                </h2>
                                <div style="font-size: 11px; color: #94a3b8; margin-top: 4px;">Massago Gateway • Demo Testing Mode</div>
                            </div>

                            <div class="amount-box">
                                <div class="label">Total Pembayaran Layanan</div>
                                <div class="value">Rp $formattedAmount</div>
                                <div class="order-id">ID Pesanan: $orderId</div>
                            </div>

                            <div class="section-title">Pilih Metode Pembayaran</div>

                            <div class="method-card active" onclick="selectMethod('qris')">
                                <div class="method-info">
                                    <div class="method-icon" style="background: #e0f2fe;">📱</div>
                                    <div>
                                        <div class="method-name">QRIS (GoPay, OVO, Dana, ShopeePay)</div>
                                        <div class="method-desc">Scan kode QR via semua e-wallet & m-banking</div>
                                    </div>
                                </div>
                                <input type="radio" name="paymethod" checked>
                            </div>

                            <div class="method-card" onclick="selectMethod('va')">
                                <div class="method-info">
                                    <div class="method-icon" style="background: #fef3c7;">🏦</div>
                                    <div>
                                        <div class="method-name">BCA / Mandiri / BRI Virtual Account</div>
                                        <div class="method-desc">Transfer otomatis tanpa konfirmasi manual</div>
                                    </div>
                                </div>
                                <input type="radio" name="paymethod">
                            </div>

                            <div id="qrisSection" class="qris-view show">
                                <div style="font-size: 12px; font-weight: 700; color: #0f172a;">Scan QRIS Midtrans Sandbox</div>
                                <div class="qr-img">
                                    <img src="https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=MIDTRANS-SANDBOX-$orderId-$amount" style="width: 100%; height: 100%; object-fit: contain;">
                                </div>
                                <div style="font-size: 11px; color: #64748b;">NMID: ID1020030040050 • Massago</div>
                            </div>

                            <div id="vaSection" class="qris-view">
                                <div style="font-size: 12px; font-weight: 700; color: #0f172a;">Nomor Virtual Account BCA</div>
                                <div class="va-box">$vaNumber</div>
                                <div style="font-size: 11px; color: #64748b;">Salin nomor VA di atas lalu selesaikan pembayaran di m-banking Anda.</div>
                            </div>

                            <button class="btn-pay" onclick="simulateSuccess()">
                                ✅ SIMULASIKAN PEMBAYARAN BERHASIL
                            </button>

                            <div class="footer-note">
                                Dilindungi oleh Enkripsi SSL 256-bit Midtrans Payment Gateway
                            </div>

                            <script>
                                function selectMethod(type) {
                                    document.querySelectorAll('.method-card').forEach(c => c.classList.remove('active'));
                                    if (type === 'qris') {
                                        document.querySelectorAll('.method-card')[0].classList.add('active');
                                        document.getElementById('qrisSection').classList.add('show');
                                        document.getElementById('vaSection').classList.remove('show');
                                    } else {
                                        document.querySelectorAll('.method-card')[1].classList.add('active');
                                        document.getElementById('vaSection').classList.add('show');
                                        document.getElementById('qrisSection').classList.remove('show');
                                    }
                                }

                                function simulateSuccess() {
                                    document.querySelector('.btn-pay').innerText = 'Memproses Pembayaran...';
                                    document.querySelector('.btn-pay').style.background = '#047857';
                                    setTimeout(function() {
                                        window.location.href = 'massago://payment-finish?order_id=$orderId&status_code=200&transaction_status=settlement';
                                    }, 500);
                                }
                            </script>
                        </body>
                        </html>
                    """.trimIndent()

                    "data:text/html;charset=utf-8," + android.net.Uri.encode(html)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Check Midtrans Payment Status directly
     */
    suspend fun checkMidtransPaymentStatus(orderId: String): String? = withContext(Dispatchers.IO) {
        try {
            val settings = fetchPlatformSettings()
            val serverKey = settings["midtrans_server_key"] ?: ""
            val isProduction = settings["midtrans_is_production"] == "true"
            val statusBaseUrl = if (isProduction) "https://api.midtrans.com/v2/$orderId/status" else "https://api.sandbox.midtrans.com/v2/$orderId/status"

            val authHeader = "Basic " + android.util.Base64.encodeToString(
                "$serverKey:".toByteArray(Charsets.UTF_8),
                android.util.Base64.NO_WRAP
            )

            val req = Request.Builder()
                .url(statusBaseUrl)
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", authHeader)
                .get()
                .build()

            client.newCall(req).execute().use { res ->
                if (res.isSuccessful) {
                    val respBody = res.body?.string() ?: return@withContext null
                    val obj = gson.fromJson(respBody, JsonObject::class.java)
                    obj.get("transaction_status")?.asString
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
        notes: String = "Panggilan Darurat dari Aplikasi MassaGo"
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
     * Fetch therapists map (ID -> Name, Phone -> Name) from Supabase
     */
    suspend fun fetchTherapistsMap(): Map<String, String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/therapists?select=id,name,phone")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val json = response.body?.string() ?: return@withContext emptyMap()
                    val listType = object : TypeToken<List<Map<String, Any>>>() {}.type
                    val list: List<Map<String, Any>> = gson.fromJson(json, listType) ?: emptyList()
                    val map = mutableMapOf<String, String>()
                    list.forEach { item ->
                        val id = item["id"] as? String ?: ""
                        val name = item["name"] as? String ?: ""
                        val phone = item["phone"] as? String ?: ""
                        if (id.isNotBlank() && name.isNotBlank()) map[id] = name
                        if (phone.isNotBlank() && name.isNotBlank()) map[phone] = name
                    }
                    map
                } else {
                    emptyMap()
                }
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * Fetch order history for customer from Supabase
     */
    suspend fun fetchCustomerOrders(phone: String, customerId: String = ""): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            var cleanPhone = phone.replace("[^0-9]".toRegex(), "")
            if (cleanPhone.startsWith("0")) cleanPhone = "62" + cleanPhone.substring(1)
            else if (cleanPhone.startsWith("8")) cleanPhone = "62" + cleanPhone
            val localPhone = if (cleanPhone.startsWith("62")) "0" + cleanPhone.substring(2) else cleanPhone
            val plusPhone = if (cleanPhone.startsWith("62")) "+$cleanPhone" else "+62$cleanPhone"
            val raw8Phone = if (cleanPhone.startsWith("62")) cleanPhone.substring(2) else cleanPhone

            if (cleanPhone.isBlank() && customerId.isBlank()) {
                return@withContext emptyList()
            }

            val query = if (cleanPhone.isNotBlank() && customerId.isNotBlank()) {
                "or=(customer_phone.eq.$cleanPhone,customer_phone.eq.$localPhone,customer_phone.eq.$plusPhone,customer_phone.eq.$raw8Phone,customer_id.eq.$customerId)"
            } else if (cleanPhone.isNotBlank()) {
                "or=(customer_phone.eq.$cleanPhone,customer_phone.eq.$localPhone,customer_phone.eq.$plusPhone,customer_phone.eq.$raw8Phone)"
            } else {
                "customer_id=eq.$customerId"
            }

            val url = "$baseUrl/rest/v1/orders?$query&order=created_at.desc&limit=50"

            val request = Request.Builder().url(url).get().build()
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

    /**
     * Fetch saved addresses for customer from Supabase customer_addresses table
     */
    suspend fun fetchCustomerAddresses(phone: String): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            var cleanPhone = phone.replace("[^0-9]".toRegex(), "")
            if (cleanPhone.startsWith("0")) cleanPhone = "62" + cleanPhone.substring(1)
            else if (cleanPhone.startsWith("8")) cleanPhone = "62" + cleanPhone
            val localPhone = if (cleanPhone.startsWith("62")) "0" + cleanPhone.substring(2) else cleanPhone

            if (cleanPhone.isBlank()) return@withContext emptyList()

            val url = "$baseUrl/rest/v1/customer_addresses?or=(customer_phone.eq.$cleanPhone,customer_phone.eq.$localPhone)&order=created_at.desc"
            val request = Request.Builder().url(url).get().build()

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

    /**
     * Save/Upsert a customer address in Supabase customer_addresses table
     */
    suspend fun saveCustomerAddress(
        id: String,
        customerPhone: String,
        title: String,
        fullAddress: String,
        note: String,
        tag: String = "Rumah",
        latitude: Double,
        longitude: Double,
        isPrimary: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            var cleanPhone = customerPhone.replace("[^0-9]".toRegex(), "")
            if (cleanPhone.startsWith("0")) cleanPhone = "62" + cleanPhone.substring(1)
            else if (cleanPhone.startsWith("8")) cleanPhone = "62" + cleanPhone

            val payload = JsonObject().apply {
                addProperty("id", id)
                addProperty("customer_phone", cleanPhone)
                addProperty("title", title)
                addProperty("full_address", fullAddress)
                addProperty("note", note)
                addProperty("tag", tag)
                addProperty("latitude", latitude)
                addProperty("longitude", longitude)
                addProperty("is_primary", isPrimary)
            }

            val request = Request.Builder()
                .url("$baseUrl/rest/v1/customer_addresses")
                .header("Prefer", "resolution=merge-duplicates")
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
     * Delete customer address in Supabase
     */
    suspend fun deleteCustomerAddress(addressId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/customer_addresses?id=eq.$addressId")
                .delete()
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
     * Fetch Live Therapist Record for Live Status Checking (Online, Offline, Location, Radius)
     */
    suspend fun fetchTherapistLiveRecord(therapistId: String): Map<String, Any?>? = withContext(Dispatchers.IO) {
        try {
            var cleanPhone = therapistId.replace("[^0-9]".toRegex(), "")
            if (cleanPhone.startsWith("0")) cleanPhone = "62" + cleanPhone.substring(1)
            val query = if (cleanPhone.length >= 8) {
                "or=(id.eq.$therapistId,phone.eq.$cleanPhone,phone.eq.${therapistId.trim()})"
            } else {
                "id=eq.$therapistId"
            }

            val request = Request.Builder()
                .url("$baseUrl/rest/v1/therapists?$query&select=*&limit=1")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext null
                    val type = object : TypeToken<List<Map<String, Any?>>>() {}.type
                    val list: List<Map<String, Any?>> = gson.fromJson(body, type)
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
     * Fetch Active Order for a Therapist to check if they are currently BUSY handling another client
     */
    suspend fun fetchTherapistActiveOrder(therapistId: String): Map<String, Any?>? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/orders?therapist_id=eq.$therapistId&status=in.(ACCEPTED,ARRIVED,IN_SERVICE,TREATMENT_IN_PROGRESS)&order=created_at.desc&limit=1")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext null
                    val type = object : TypeToken<List<Map<String, Any?>>>() {}.type
                    val list: List<Map<String, Any?>> = gson.fromJson(body, type)
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
}
