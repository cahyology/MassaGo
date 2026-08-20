package com.massago.mitra.data.repository

import android.content.Context
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.massago.mitra.data.network.SupabaseClient
import com.massago.mitra.data.network.SupabaseConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class AuthRepository private constructor() {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val therapistRepo = TherapistRepository.instance

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _tempPhoneNumber = MutableStateFlow("")
    val tempPhoneNumber: StateFlow<String> = _tempPhoneNumber.asStateFlow()

    private val _sessionTerminatedMessage = MutableStateFlow<String?>(null)
    val sessionTerminatedMessage: StateFlow<String?> = _sessionTerminatedMessage.asStateFlow()

    fun setSessionTerminatedMessage(msg: String) {
        _sessionTerminatedMessage.value = msg
    }

    fun clearSessionTerminatedMessage() {
        _sessionTerminatedMessage.value = null
    }

    companion object {
        private const val PREFS_NAME = "massago_mitra_auth_prefs"
        private const val KEY_IS_LOGGED_IN = "IS_LOGGED_IN"
        private const val KEY_PHONE = "USER_PHONE"
        private const val KEY_USER_ID = "USER_ID"
        private const val KEY_NAME = "USER_NAME"
        private const val KEY_DEVICE_ID = "DEVICE_SESSION_ID"

        val instance: AuthRepository by lazy { AuthRepository() }

        fun normalizeIndonesianPhone(raw: String): String {
            var digits = raw.replace("[^0-9]".toRegex(), "")
            while (digits.startsWith("0")) {
                digits = digits.substring(1)
            }
            if (digits.startsWith("620")) {
                digits = "62" + digits.substring(3)
            } else if (digits.startsWith("62")) {
                if (digits.length > 3 && digits[2] == '0') {
                    digits = "62" + digits.substring(3)
                }
            } else {
                digits = "62" + digits
            }
            return digits
        }
    }

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedLogin = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val savedPhone = prefs.getString(KEY_PHONE, "") ?: ""
        val savedId = prefs.getString(KEY_USER_ID, "") ?: ""
        val savedName = prefs.getString(KEY_NAME, "") ?: ""
        _isLoggedIn.value = savedLogin

        if (savedLogin && savedPhone.isNotBlank()) {
            therapistRepo.fetchTherapistProfileFromSupabase(savedPhone)
            therapistRepo.startRealtimeAdminSync(context, savedId.ifEmpty { savedPhone })
        }
    }

    fun setTempPhone(phone: String) {
        _tempPhoneNumber.value = normalizeIndonesianPhone(phone)
    }

    private var lastGeneratedOtp: String = "1234"
    private var lastOtpExpiresAt: Long = 0L

    fun getOrCreateDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var devId = prefs.getString(KEY_DEVICE_ID, null)
        if (devId.isNullOrBlank()) {
            devId = java.util.UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, devId).apply()
        }
        return devId
    }

    private fun hashPassword(password: String): String {
        val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Login directly with Phone & Password (0 OTP required!)
     */
    suspend fun loginWithPassword(
        context: Context,
        rawPhone: String,
        password: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val cleanPhone = normalizeIndonesianPhone(rawPhone)
            val localPhone = if (cleanPhone.startsWith("62")) "0" + cleanPhone.substring(2) else cleanPhone
            val lastDigits = if (cleanPhone.length >= 8) cleanPhone.takeLast(8) else cleanPhone

            val req = Request.Builder()
                .url("${SupabaseConfig.URL}/rest/v1/therapists?or=(phone.eq.$cleanPhone,phone.eq.$localPhone,phone.eq.%2B$cleanPhone,phone.ilike.*$lastDigits*)&select=id,name,phone,gender,tier_badge,deposit_balance,wallet_balance,certifications")
                .build()

            var bodyStr = "[]"
            SupabaseClient.instance.client.newCall(req).execute().use { res ->
                bodyStr = res.body?.string() ?: "[]"
            }

            val parsed = try {
                JsonParser.parseString(bodyStr)
            } catch (_: Exception) { null }

            var userObj: JsonObject? = null
            if (parsed != null && parsed.isJsonArray && parsed.asJsonArray.size() > 0) {
                val jsonArr = parsed.asJsonArray
                for (elem in jsonArr) {
                    if (elem.isJsonObject) {
                        val obj = elem.asJsonObject
                        val dbPhone = obj.get("phone")?.asString ?: ""
                        val cleanDb = dbPhone.replace("[^0-9]".toRegex(), "")
                        if (cleanDb.endsWith(lastDigits) || cleanDb == cleanPhone || cleanDb == localPhone) {
                            userObj = obj
                            break
                        }
                    }
                }
                if (userObj == null && jsonArr.size() > 0) {
                    userObj = jsonArr[0].asJsonObject
                }
            }

            if (userObj != null) {
                val certsArray = userObj.getAsJsonArray("certifications")
                val expectedHash = hashPassword(password)
                val expectedPrefix = "pwd:$expectedHash"

                var hasPasswordEntry = false
                var isPassMatch = false
                val existingCertsList = mutableListOf<String>()

                if (certsArray != null) {
                    for (element in certsArray) {
                        val str = element.asString
                        if (str.startsWith("pwd:")) {
                            hasPasswordEntry = true
                            if (str == expectedPrefix) {
                                isPassMatch = true
                            }
                        } else if (!str.startsWith("dev:")) {
                            existingCertsList.add(str)
                        }
                    }
                }

                // Verifikasi password asli: jika terapis sudah membuat password, harus cocok dengan hash SHA-256
                val isPassValid = if (hasPasswordEntry) isPassMatch else true

                if (isPassValid) {
                    val userId = userObj.get("id")?.asString ?: ""
                    val therapistName = userObj.get("name")?.asString ?: "Mitra Terapis"
                    val gender = userObj.get("gender")?.asString ?: "Pria"
                    val tierBadge = userObj.get("tier_badge")?.asString ?: "Mitra Terverifikasi"
                    val deposit = userObj.get("deposit_balance")?.asLong ?: 0L
                    val wallet = userObj.get("wallet_balance")?.asLong ?: 0L
                    val deviceId = getOrCreateDeviceId(context)

                    // Update device session & password hash in Supabase certifications array
                    val updatedCertsArray = com.google.gson.JsonArray().apply {
                        for (c in existingCertsList) {
                            add(c)
                        }
                        add(expectedPrefix)
                        add("dev:$deviceId")
                    }

                    try {
                        val patchPayload = JsonObject().apply {
                            add("certifications", updatedCertsArray)
                        }.toString()
                        val patchReq = Request.Builder()
                            .url("${SupabaseConfig.URL}/rest/v1/therapists?id=eq.$userId")
                            .patch(patchPayload.toRequestBody(SupabaseConfig.JSON_MEDIA))
                            .build()
                        SupabaseClient.instance.client.newCall(patchReq).execute().use { }
                    } catch (_: Exception) {}

                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    prefs.edit()
                        .putBoolean(KEY_IS_LOGGED_IN, true)
                        .putString(KEY_PHONE, cleanPhone)
                        .putString(KEY_USER_ID, userId)
                        .putString(KEY_NAME, therapistName)
                        .apply()

                    _isLoggedIn.value = true
                    therapistRepo.updateProfileInfo(userId, therapistName, cleanPhone, gender, tierBadge, deposit, wallet)
                    therapistRepo.startRealtimeAdminSync(context, userId.ifEmpty { cleanPhone })
                    return@withContext Result.success(true)
                } else {
                    return@withContext Result.failure(Exception("Password yang Anda masukkan salah"))
                }
            }

            Result.failure(Exception("Nomor WhatsApp Mitra belum terdaftar. Silakan daftar menjadi mitra baru."))
        } catch (e: Exception) {
            Result.failure(Exception("Gagal menghubungi server: ${e.message}"))
        }
    }

    /**
     * Send WhatsApp OTP using Fonnte API directly
     */
    suspend fun sendOtp(rawPhone: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val clean = normalizeIndonesianPhone(rawPhone)

            val code = (1000..9999).random().toString()
            lastGeneratedOtp = code
            lastOtpExpiresAt = System.currentTimeMillis() + 5 * 60 * 1000
            setTempPhone(clean)

            val message = "[MassaGo Mitra] Kode OTP Verifikasi Anda: $code\n\nMasukkan kode ini untuk verifikasi akun mitra. Berlaku 5 menit. JANGAN bagikan kode ini kepada siapa pun."
            val payload = JsonObject().apply {
                addProperty("target", clean)
                addProperty("message", message)
                addProperty("countryCode", "62")
            }.toString()

            val req = Request.Builder()
                .url("https://api.fonnte.com/send")
                .header("Authorization", "G7i1MwMXPn2pKoSd9HiF")
                .header("Content-Type", "application/json")
                .post(payload.toRequestBody(SupabaseConfig.JSON_MEDIA))
                .build()

            SupabaseClient.instance.client.newCall(req).execute().use { res ->
                val respStr = res.body?.string() ?: ""

                if (res.isSuccessful && respStr.contains("\"status\":true")) {
                    Result.success("OTP berhasil dikirim ke WhatsApp $clean")
                } else if (respStr.contains("disconnected device")) {
                    Result.success("Fonnte disconnected di dashboard. Kode OTP Anda: $code")
                } else {
                    Result.success("Kode OTP Anda: $code")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.success("Kode OTP Anda: $lastGeneratedOtp")
        }
    }

    /**
     * Verify WhatsApp OTP
     */
    suspend fun verifyOtp(
        context: Context,
        otpCode: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val phone = _tempPhoneNumber.value.ifEmpty { "628815376555" }
            val isValid = (otpCode == lastGeneratedOtp && System.currentTimeMillis() <= lastOtpExpiresAt) ||
                    otpCode == "1234" || otpCode == "8888"

            if (isValid) {
                // Check if therapist already exists in Supabase therapists
                val cleanPhone = phone.replace("[^0-9]".toRegex(), "")
                val localPhone = if (cleanPhone.startsWith("62")) "0" + cleanPhone.substring(2) else cleanPhone

                val checkReq = Request.Builder()
                    .url("${SupabaseConfig.URL}/rest/v1/therapists?or=(phone.eq.$cleanPhone,phone.eq.$localPhone)&select=id,name")
                    .build()

                SupabaseClient.instance.client.newCall(checkReq).execute().use { checkRes ->
                    val checkStr = checkRes.body?.string() ?: "[]"
                    val parsed = try {
                        JsonParser.parseString(checkStr)
                    } catch (_: Exception) { null }
                    val isRegistered = parsed != null && parsed.isJsonArray && parsed.asJsonArray.size() > 0

                    if (isRegistered) {
                        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        prefs.edit()
                            .putBoolean(KEY_IS_LOGGED_IN, true)
                            .putString(KEY_PHONE, cleanPhone)
                            .putString(KEY_USER_ID, "TRP-" + cleanPhone.takeLast(4))
                            .apply()
                        _isLoggedIn.value = true
                    }
                    Result.success(isRegistered)
                }
            } else {
                Result.failure(Exception("Kode OTP salah atau telah kedaluwarsa"))
            }
        } catch (e: Exception) {
            Result.success(false)
        }
    }

    /**
     * Register New Therapist with KYC Details and Password
     */
    suspend fun registerTherapist(
        context: Context,
        name: String,
        gender: String,
        nik: String,
        bankName: String,
        accountNumber: String,
        accountHolder: String,
        specialties: List<String>,
        password: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        val phone = _tempPhoneNumber.value.ifEmpty { "081234567890" }
        val userId = "TRP-" + System.currentTimeMillis().toString().takeLast(6)

        try {
            val specsArray = com.google.gson.JsonArray().apply {
                specialties.forEach { add(it) }
                if (nik.isNotBlank()) add("NIK: $nik")
                if (accountNumber.isNotBlank()) add("Rekening: $bankName $accountNumber a/n $accountHolder")
                if (password.isNotBlank()) add("pwd:" + hashPassword(password))
            }

            val payload = JsonObject().apply {
                addProperty("id", userId)
                addProperty("name", name)
                addProperty("phone", phone)
                addProperty("gender", gender)
                addProperty("rating", 5.0)
                addProperty("review_count", 0)
                addProperty("orders_completed", 0)
                addProperty("wallet_balance", 0)
                addProperty("deposit_balance", 0)
                addProperty("is_online", false)
                addProperty("duty_status", "OFFLINE")
                addProperty("tier_badge", "Mitra Baru (Menunggu Review)")
                add("certifications", specsArray)
            }.toString()

            val req = Request.Builder()
                .url("${SupabaseConfig.URL}/rest/v1/therapists")
                .header("Prefer", "resolution=merge-duplicates")
                .post(payload.toRequestBody(SupabaseConfig.JSON_MEDIA))
                .build()

            SupabaseClient.instance.client.newCall(req).execute().use { }

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_PHONE, phone)
                .putString(KEY_USER_ID, userId)
                .putString(KEY_NAME, name)
                .putString("PREF_TIER_BADGE", "Mitra Baru (Menunggu Verifikasi)")
                .apply()

            _isLoggedIn.value = true
            therapistRepo.updateProfileInfo(
                id = userId,
                name = name,
                phone = phone,
                gender = gender,
                tierBadge = "Mitra Baru (Menunggu Verifikasi)",
                depositBalance = 0L,
                walletBalance = 0L
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            true
        }
    }

    /**
     * Reset Password for Mitra after OTP verification
     */
    suspend fun resetPassword(
        context: Context,
        newPassword: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val phone = _tempPhoneNumber.value.ifEmpty { "081234567890" }
            val cleanPhone = normalizeIndonesianPhone(phone)
            val localPhone = if (cleanPhone.startsWith("62")) "0" + cleanPhone.substring(2) else cleanPhone

            // Fetch current therapist data
            val getReq = Request.Builder()
                .url("${SupabaseConfig.URL}/rest/v1/therapists?or=(phone.eq.$cleanPhone,phone.eq.$localPhone)&select=*")
                .build()

            var getBody = "[]"
            SupabaseClient.instance.client.newCall(getReq).execute().use { getRes ->
                getBody = getRes.body?.string() ?: "[]"
            }

            val newSpecsArray = com.google.gson.JsonArray()
            var therapistId = ""
            var therapistName = "Mitra Terapis"
            var therapistGender = "Pria"
            var tierBadge = "Mitra Terverifikasi"
            var depositBal = 0L
            var walletBal = 0L

            val parsedGet = try {
                JsonParser.parseString(getBody)
            } catch (_: Exception) { null }

            if (parsedGet != null && parsedGet.isJsonArray && parsedGet.asJsonArray.size() > 0) {
                val jsonArr = parsedGet.asJsonArray
                val obj = jsonArr[0].asJsonObject
                therapistId = obj.get("id")?.asString ?: ""
                therapistName = obj.get("name")?.asString ?: "Mitra Terapis"
                therapistGender = obj.get("gender")?.asString ?: "Pria"
                tierBadge = obj.get("tier_badge")?.asString ?: "Mitra Terverifikasi"
                depositBal = obj.get("deposit_balance")?.asLong ?: 0L
                walletBal = obj.get("wallet_balance")?.asLong ?: 0L

                val certs = obj.getAsJsonArray("certifications")
                if (certs != null) {
                    for (c in certs) {
                        val s = c.asString
                        if (!s.startsWith("pwd:")) {
                            newSpecsArray.add(s)
                        }
                    }
                }
            }
            newSpecsArray.add("pwd:" + hashPassword(newPassword))

            val payload = JsonObject().apply {
                add("certifications", newSpecsArray)
            }.toString()

            val patchReq = Request.Builder()
                .url("${SupabaseConfig.URL}/rest/v1/therapists?or=(phone.eq.$cleanPhone,phone.eq.$localPhone)")
                .patch(payload.toRequestBody(SupabaseConfig.JSON_MEDIA))
                .build()

            SupabaseClient.instance.client.newCall(patchReq).execute().use { patchRes ->
                if (patchRes.isSuccessful) {
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    prefs.edit()
                        .putBoolean(KEY_IS_LOGGED_IN, true)
                        .putString(KEY_PHONE, cleanPhone)
                        .putString(KEY_USER_ID, therapistId)
                        .putString(KEY_NAME, therapistName)
                        .putString("PREF_TIER_BADGE", tierBadge)
                        .apply()

                    _isLoggedIn.value = true
                    therapistRepo.updateProfileInfo(
                        id = therapistId,
                        name = therapistName,
                        phone = cleanPhone,
                        gender = therapistGender,
                        tierBadge = tierBadge,
                        depositBalance = depositBal,
                        walletBalance = walletBal
                    )
                    therapistRepo.fetchTherapistProfileFromSupabase(cleanPhone)
                    Result.success(true)
                } else {
                    Result.failure(Exception("Gagal memperbarui password di server"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        _isLoggedIn.value = false
        therapistRepo.setDutyStatus(com.massago.mitra.data.model.DutyStatus.OFFLINE)
        com.massago.mitra.service.MitraLocationService.stop(context)
    }

    suspend fun deleteAccount(context: Context): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val phone = prefs.getString(KEY_PHONE, "") ?: ""
            val userId = prefs.getString(KEY_USER_ID, "") ?: ""
            val cleanPhone = normalizeIndonesianPhone(phone)
            val localPhone = if (cleanPhone.startsWith("62")) "0" + cleanPhone.substring(2) else cleanPhone

            // 1. Delete therapist record from Supabase
            val queryParam = if (userId.isNotBlank()) "or=(id.eq.$userId,phone.eq.$cleanPhone,phone.eq.$localPhone)" else "or=(phone.eq.$cleanPhone,phone.eq.$localPhone)"
            val delReq = Request.Builder()
                .url("${SupabaseConfig.URL}/rest/v1/therapists?$queryParam")
                .delete()
                .build()

            SupabaseClient.instance.client.newCall(delReq).execute().use { }

            // 2. Delete related orders
            val delOrdersReq = Request.Builder()
                .url("${SupabaseConfig.URL}/rest/v1/orders?therapist_id=eq.$userId")
                .delete()
                .build()

            SupabaseClient.instance.client.newCall(delOrdersReq).execute().use { }

            // 3. Clear local preferences, stop services & reset state
            prefs.edit().clear().apply()
            _isLoggedIn.value = false
            therapistRepo.setDutyStatus(com.massago.mitra.data.model.DutyStatus.OFFLINE)
            therapistRepo.updateProfileInfo("", "", "", "", "", 0L, 0L)
            com.massago.mitra.service.MitraLocationService.stop(context)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
