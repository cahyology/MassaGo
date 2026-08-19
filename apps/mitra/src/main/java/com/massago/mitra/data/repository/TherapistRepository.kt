package com.massago.mitra.data.repository

import android.content.Context
import com.massago.mitra.MassaGoApp
import com.massago.mitra.data.model.DutyStatus
import com.massago.mitra.data.model.TherapistProfile
import com.massago.mitra.data.network.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TherapistRepository private constructor() {

    private val prefs = try {
        MassaGoApp.instance.getSharedPreferences("MITRA_PREFS", Context.MODE_PRIVATE)
    } catch (_: Exception) {
        null
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    private val initialAutoAccept = prefs?.getBoolean("PREF_AUTO_ACCEPT", false) ?: false
    private val initialRadius = prefs?.getInt("PREF_MAX_RADIUS", 10) ?: 10
    private val initialGenderPref = prefs?.getString("PREF_GENDER_PREF", "Semua") ?: "Semua"
    private val initialName = prefs?.getString("PREF_THERAPIST_NAME", "Mitra Terapis") ?: "Mitra Terapis"
    private val initialId = prefs?.getString("PREF_THERAPIST_ID", "") ?: ""
    private val initialPhone = prefs?.getString("PREF_THERAPIST_PHONE", "") ?: ""
    private val initialBadge = prefs?.getString("PREF_TIER_BADGE", "Mitra Baru (Menunggu Verifikasi)") ?: "Mitra Baru (Menunggu Verifikasi)"
    private val initialVerified = !initialBadge.contains("Menunggu") && !initialBadge.contains("Review")
    private val initialLat = prefs?.getString("PREF_LAST_LAT", "-7.7956")?.toDoubleOrNull() ?: -7.7956
    private val initialLng = prefs?.getString("PREF_LAST_LNG", "110.3695")?.toDoubleOrNull() ?: 110.3695
    private val initialDutyStr = prefs?.getString("PREF_DUTY_STATUS", "OFFLINE") ?: "OFFLINE"
    private val initialDuty = if (initialDutyStr == "ONLINE" && initialVerified) DutyStatus.ONLINE else DutyStatus.OFFLINE

    private val _therapistProfile = MutableStateFlow(
        TherapistProfile(
            id = initialId,
            name = initialName,
            phone = initialPhone,
            dutyStatus = initialDuty,
            tierBadge = initialBadge,
            isVerified = initialVerified,
            autoAcceptOrders = initialAutoAccept,
            maxRadiusKm = initialRadius,
            preferredClientGender = initialGenderPref,
            latitude = initialLat,
            longitude = initialLng
        )
    )
    val therapistProfile: StateFlow<TherapistProfile> = _therapistProfile.asStateFlow()

    private val _platformCommissionPercent = MutableStateFlow(20)
    val platformCommissionPercent: StateFlow<Int> = _platformCommissionPercent.asStateFlow()

    init {
        fetchPlatformCommission()
        if (initialDuty == DutyStatus.ONLINE) {
            try {
                com.massago.mitra.service.MitraLocationService.start(MassaGoApp.instance)
                com.massago.mitra.data.repository.OrderRepository.instance.startRealtimeOrderPolling()
            } catch (_: Exception) {}
        }
        if (initialPhone.isNotBlank() || initialId.isNotBlank()) {
            fetchTherapistProfileFromSupabase(initialPhone.ifEmpty { initialId })
        }
        startRealtimeAdminSync()
    }

    private fun startRealtimeAdminSync() {
        scope.launch {
            while (true) {
                kotlinx.coroutines.delay(2500)
                val currentId = _therapistProfile.value.id
                val currentPhone = _therapistProfile.value.phone
                if (currentId.isNotBlank() || currentPhone.isNotBlank()) {
                    try {
                        var clean = (currentPhone.ifEmpty { currentId }).replace("[^0-9]".toRegex(), "")
                        if (clean.startsWith("0")) clean = "62" + clean.substring(1)
                        else if (clean.startsWith("8")) clean = "62" + clean

                        val localPhone = if (clean.startsWith("62")) "0" + clean.substring(2) else clean

                        val req = okhttp3.Request.Builder()
                            .url("${com.massago.mitra.data.network.SupabaseConfig.URL}/rest/v1/therapists?or=(id.eq.$currentId,phone.eq.$clean,phone.eq.$localPhone)&select=id,name,phone,gender,is_online,duty_status,tier_badge,deposit_balance,wallet_balance,rating,orders_completed,latitude,longitude")
                            .header("apikey", com.massago.mitra.data.network.SupabaseConfig.ANON_KEY)
                            .header("Authorization", "Bearer ${com.massago.mitra.data.network.SupabaseConfig.ANON_KEY}")
                            .build()

                        val res = okhttp3.OkHttpClient().newCall(req).execute()
                        val bodyStr = res.body?.string() ?: ""

                        if (res.isSuccessful && bodyStr.startsWith("[{")) {
                            val arr = com.google.gson.JsonParser.parseString(bodyStr).asJsonArray
                            if (arr.size() > 0) {
                                val obj = arr[0].asJsonObject
                                val id = obj.get("id")?.asString ?: currentId
                                val name = obj.get("name")?.asString ?: _therapistProfile.value.name
                                val phone = obj.get("phone")?.asString ?: _therapistProfile.value.phone
                                val gender = obj.get("gender")?.asString ?: _therapistProfile.value.gender
                                val remoteIsOnline = obj.get("is_online")?.asBoolean ?: false
                                val remoteDuty = obj.get("duty_status")?.asString ?: "OFFLINE"
                                val remoteTier = obj.get("tier_badge")?.asString ?: "Mitra Terverifikasi"
                                val remoteDeposit = obj.get("deposit_balance")?.asLong ?: 0L
                                val remoteWallet = obj.get("wallet_balance")?.asLong ?: 0L
                                val rating = obj.get("rating")?.asDouble ?: 5.0
                                val orders = obj.get("orders_completed")?.asInt ?: 0
                                val remoteLat = obj.get("latitude")?.asDouble ?: _therapistProfile.value.latitude
                                val remoteLng = obj.get("longitude")?.asDouble ?: _therapistProfile.value.longitude

                                val isVerif = !remoteTier.contains("Menunggu") && !remoteTier.contains("Review") && !remoteTier.contains("Tolak") && !remoteTier.contains("Nonaktif")

                                _therapistProfile.update { curr ->
                                    val finalDuty = if (!remoteIsOnline || !isVerif) {
                                        DutyStatus.OFFLINE
                                    } else if (remoteDuty == "ON_DUTY_BUSY") {
                                        DutyStatus.ON_DUTY_BUSY
                                    } else if (remoteDuty == "ONLINE") {
                                        DutyStatus.ONLINE
                                    } else {
                                        DutyStatus.OFFLINE
                                    }

                                    curr.copy(
                                        id = id,
                                        name = name,
                                        phone = phone,
                                        gender = gender,
                                        dutyStatus = finalDuty,
                                        tierBadge = remoteTier,
                                        isVerified = isVerif,
                                        depositBalance = remoteDeposit,
                                        mainBalance = remoteWallet,
                                        rating = rating,
                                        totalOrdersCompleted = orders,
                                        latitude = remoteLat,
                                        longitude = remoteLng
                                    )
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    fun updateProfileInfo(
        id: String,
        name: String,
        phone: String,
        gender: String = "Pria",
        tierBadge: String = "Mitra Terverifikasi",
        depositBalance: Long = 0L,
        walletBalance: Long = 0L
    ) {
        val isVerif = !tierBadge.contains("Menunggu") && !tierBadge.contains("Review") && !tierBadge.contains("Tolak") && !tierBadge.contains("Nonaktif")
        prefs?.edit()
            ?.putString("PREF_THERAPIST_ID", id)
            ?.putString("PREF_THERAPIST_NAME", name)
            ?.putString("PREF_THERAPIST_PHONE", phone)
            ?.putString("PREF_TIER_BADGE", tierBadge)
            ?.apply()

        _therapistProfile.update { current ->
            current.copy(
                id = id.ifBlank { current.id },
                name = name.ifBlank { current.name },
                phone = phone.ifBlank { current.phone },
                gender = gender,
                tierBadge = tierBadge,
                isVerified = isVerif,
                depositBalance = depositBalance,
                mainBalance = walletBalance
            )
        }
    }

    fun fetchTherapistProfileFromSupabase(phoneOrId: String) {
        scope.launch {
            try {
                var clean = phoneOrId.replace("[^0-9]".toRegex(), "")
                if (clean.startsWith("0")) clean = "62" + clean.substring(1)
                else if (clean.startsWith("8")) clean = "62" + clean

                val localPhone = if (clean.startsWith("62")) "0" + clean.substring(2) else clean

                val req = okhttp3.Request.Builder()
                    .url("${com.massago.mitra.data.network.SupabaseConfig.URL}/rest/v1/therapists?or=(id.eq.$phoneOrId,phone.eq.$clean,phone.eq.$localPhone)&select=*")
                    .header("apikey", com.massago.mitra.data.network.SupabaseConfig.ANON_KEY)
                    .header("Authorization", "Bearer ${com.massago.mitra.data.network.SupabaseConfig.ANON_KEY}")
                    .build()

                val client = okhttp3.OkHttpClient()
                val res = client.newCall(req).execute()
                val bodyStr = res.body?.string() ?: "[]"

                if (res.isSuccessful && bodyStr.startsWith("[{")) {
                    val jsonArr = com.google.gson.JsonParser.parseString(bodyStr).asJsonArray
                    if (jsonArr.size() > 0) {
                        val obj = jsonArr[0].asJsonObject
                        val id = obj.get("id")?.asString ?: ""
                        val name = obj.get("name")?.asString ?: ""
                        val phone = obj.get("phone")?.asString ?: ""
                        val gender = obj.get("gender")?.asString ?: "Pria"
                        val tierBadge = obj.get("tier_badge")?.asString ?: "Mitra Terverifikasi"
                        val deposit = obj.get("deposit_balance")?.asLong ?: 0L
                        val wallet = obj.get("wallet_balance")?.asLong ?: 0L
                        val rating = obj.get("rating")?.asDouble ?: 5.0
                        val orders = obj.get("orders_completed")?.asInt ?: 0

                        updateProfileInfo(id, name, phone, gender, tierBadge, deposit, wallet)
                        _therapistProfile.update { it.copy(rating = rating, totalOrdersCompleted = orders) }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    suspend fun updateTherapistProfileInSupabase(
        name: String,
        phone: String,
        gender: String,
        bankName: String = "",
        bankAccount: String = "",
        maxRadiusKm: Int = 15
    ): Result<Boolean> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            val currentId = _therapistProfile.value.id
            val phoneClean = normalizeIndonesianPhone(phone)
            val queryTarget = if (currentId.isNotBlank()) "id=eq.$currentId" else "phone=eq.$phoneClean"

            val payload = com.google.gson.JsonObject().apply {
                addProperty("name", name)
                addProperty("phone", phoneClean)
                addProperty("gender", gender)
                if (bankAccount.isNotBlank()) {
                    addProperty("bank_name", bankName)
                    addProperty("bank_account_number", bankAccount)
                }
            }

            val req = okhttp3.Request.Builder()
                .url("${com.massago.mitra.data.network.SupabaseConfig.URL}/rest/v1/therapists?$queryTarget")
                .header("apikey", com.massago.mitra.data.network.SupabaseConfig.ANON_KEY)
                .header("Authorization", "Bearer ${com.massago.mitra.data.network.SupabaseConfig.ANON_KEY}")
                .patch(okhttp3.RequestBody.create(com.massago.mitra.data.network.SupabaseConfig.JSON_MEDIA, payload.toString()))
                .build()

            val client = okhttp3.OkHttpClient()
            val res = client.newCall(req).execute()
            if (res.isSuccessful) {
                _therapistProfile.update {
                    it.copy(
                        name = name,
                        phone = phoneClean,
                        gender = gender,
                        maxRadiusKm = maxRadiusKm
                    )
                }
                prefs?.edit()
                    ?.putString("PREF_THERAPIST_NAME", name)
                    ?.putString("PREF_THERAPIST_PHONE", phoneClean)
                    ?.putInt("PREF_MAX_RADIUS", maxRadiusKm)
                    ?.apply()
                Result.success(true)
            } else {
                Result.failure(Exception("Gagal memperbarui profil di server"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun normalizeIndonesianPhone(phone: String): String {
        var clean = phone.replace("[^0-9]".toRegex(), "")
        return when {
            clean.startsWith("0") -> "62" + clean.substring(1)
            clean.startsWith("8") -> "62" + clean
            clean.startsWith("620") -> "62" + clean.substring(3)
            else -> clean
        }
    }

    fun fetchPlatformCommission() {
        scope.launch {
            try {
                val settings = SupabaseClient.instance.fetchPlatformSettings()
                val rate = settings["platform_commission_percent"]?.toIntOrNull() ?: 20
                _platformCommissionPercent.value = rate
            } catch (_: Exception) {}
        }
    }

    fun setDutyStatus(status: DutyStatus) {
        val current = _therapistProfile.value
        // If not verified, prevent turning ONLINE
        if (status == DutyStatus.ONLINE && !current.isVerified) {
            return
        }

        prefs?.edit()?.putString("PREF_DUTY_STATUS", status.name)?.apply()
        _therapistProfile.update { it.copy(dutyStatus = status) }
        val identifier = current.id.ifBlank { current.phone }
        scope.launch {
            if (identifier.isNotBlank()) {
                SupabaseClient.instance.updateDutyStatus(
                    therapistId = identifier,
                    isOnline = (status == DutyStatus.ONLINE)
                )
            }
        }
    }

    fun isPersistedOnline(): Boolean {
        return (prefs?.getString("PREF_DUTY_STATUS", "OFFLINE") == "ONLINE")
    }

    fun toggleAutoAccept(enabled: Boolean) {
        prefs?.edit()?.putBoolean("PREF_AUTO_ACCEPT", enabled)?.apply()
        _therapistProfile.update { it.copy(autoAcceptOrders = enabled) }
    }

    fun setMaxRadiusKm(radius: Int) {
        prefs?.edit()?.putInt("PREF_MAX_RADIUS", radius)?.apply()
        _therapistProfile.update { it.copy(maxRadiusKm = radius) }
    }

    fun setPreferredClientGender(gender: String) {
        prefs?.edit()?.putString("PREF_GENDER_PREF", gender)?.apply()
        _therapistProfile.update { it.copy(preferredClientGender = gender) }
    }

    fun toggleServiceSpecialty(serviceName: String, isEnabled: Boolean) {
        _therapistProfile.update { current ->
            val updated = current.activeSpecialties.toMutableSet()
            if (isEnabled) {
                updated.add(serviceName)
            } else {
                if (updated.size > 1) { // ensure at least one service is enabled
                    updated.remove(serviceName)
                }
            }
            current.copy(activeSpecialties = updated)
        }
    }

    fun addEarnings(netAmount: Long, isBonus: Boolean = false) {
        _therapistProfile.update { current ->
            val newMainBalance = current.mainBalance + netAmount
            val newTodayEarnings = current.todayEarnings + netAmount
            val newOrdersCount = if (isBonus) current.todayOrdersCount else current.todayOrdersCount + 1
            current.copy(
                mainBalance = newMainBalance,
                todayEarnings = newTodayEarnings,
                todayOrdersCount = newOrdersCount,
                totalOrdersCompleted = current.totalOrdersCompleted + (if (isBonus) 0 else 1)
            )
        }
    }

    fun deductDeposit(amount: Long) {
        _therapistProfile.update { current ->
            current.copy(depositBalance = (current.depositBalance - amount).coerceAtLeast(0L))
        }
    }

    fun addDeposit(amount: Long) {
        _therapistProfile.update { current ->
            current.copy(depositBalance = current.depositBalance + amount)
        }
    }

    fun withdrawMainBalance(amount: Long): Boolean {
        if (_therapistProfile.value.mainBalance >= amount) {
            _therapistProfile.update { current ->
                current.copy(mainBalance = current.mainBalance - amount)
            }
            return true
        }
        return false
    }

    fun updateCurrentLocation(lat: Double, lng: Double) {
        prefs?.edit()
            ?.putString("PREF_LAST_LAT", lat.toString())
            ?.putString("PREF_LAST_LNG", lng.toString())
            ?.apply()
        _therapistProfile.update { current ->
            current.copy(
                latitude = lat,
                longitude = lng
            )
        }
    }

    companion object {
        val instance: TherapistRepository by lazy { TherapistRepository() }
    }
}
