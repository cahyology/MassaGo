package com.massago.customer.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.massago.customer.CustomerApp
import com.massago.customer.data.model.CustomerLocation
import com.massago.customer.data.model.CustomerProfile
import com.massago.customer.data.model.SavedAddress
import com.massago.customer.data.network.SupabaseConfig
import com.massago.customer.data.network.SupabaseCustomerClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class CustomerUserRepository private constructor() {

    private val gson = Gson()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val prefs by lazy {
        try {
            CustomerApp.instance.getSharedPreferences("massago_customer_user_prefs", Context.MODE_PRIVATE)
        } catch (_: Exception) {
            null
        }
    }

    private val _profile = MutableStateFlow(CustomerProfile())
    val profile: StateFlow<CustomerProfile> = _profile.asStateFlow()

    private val _currentLocation = MutableStateFlow(CustomerLocation())
    val currentLocation: StateFlow<CustomerLocation> = _currentLocation.asStateFlow()

    private val _favoriteTherapists = MutableStateFlow<List<FavoriteTherapist>>(emptyList())
    val favoriteTherapists: StateFlow<List<FavoriteTherapist>> = _favoriteTherapists.asStateFlow()

    init {
        try {
            _profile.value = loadPersistedProfile()
            _currentLocation.value = loadPersistedLocation()
            _favoriteTherapists.value = loadPersistedFavorites()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        fetchSavedAddressesFromSupabase()
        fetchFavoriteTherapistsFromSupabase()
        fetchCustomerProfileFromSupabase()
    }

    private fun loadPersistedProfile(): CustomerProfile {
        val base = CustomerProfile()
        val p = prefs ?: return base
        val name = p.getString("USER_NAME", base.name) ?: base.name
        val phone = p.getString("USER_PHONE", base.phone) ?: base.phone
        val email = p.getString("USER_EMAIL", base.email) ?: base.email
        val id = p.getString("USER_ID", base.id) ?: base.id
        val balance = p.getLong("WALLET_BALANCE", base.walletBalance)

        val savedAddressesJson = p.getString("SAVED_ADDRESSES_JSON", null)
        val savedAddresses: List<SavedAddress> = if (!savedAddressesJson.isNullOrBlank()) {
            try {
                val listType = object : TypeToken<List<SavedAddress>>() {}.type
                gson.fromJson(savedAddressesJson, listType) ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        return base.copy(
            name = name,
            phone = phone,
            email = email,
            id = id,
            walletBalance = balance,
            savedAddresses = savedAddresses
        )
    }

    private fun loadPersistedLocation(): CustomerLocation {
        val base = CustomerLocation()
        val p = prefs ?: return base
        val locJson = p.getString("CURRENT_LOCATION_JSON", null)
        return if (!locJson.isNullOrBlank()) {
            try {
                gson.fromJson(locJson, CustomerLocation::class.java) ?: base
            } catch (_: Exception) {
                base
            }
        } else {
            base
        }
    }

    private fun persistProfile(profile: CustomerProfile) {
        prefs?.edit()?.apply {
            putString("USER_NAME", profile.name)
            putString("USER_PHONE", profile.phone)
            putString("USER_EMAIL", profile.email)
            putString("USER_ID", profile.id)
            putLong("WALLET_BALANCE", profile.walletBalance)
            putString("SAVED_ADDRESSES_JSON", gson.toJson(profile.savedAddresses))
            apply()
        }
    }

    private fun persistLocation(loc: CustomerLocation) {
        prefs?.edit()?.apply {
            putString("CURRENT_LOCATION_JSON", gson.toJson(loc))
            apply()
        }
    }

    fun fetchSavedAddressesFromSupabase() {
        coroutineScope.launch {
            try {
                val currentPhone = _profile.value.phone
                if (currentPhone.isBlank()) return@launch

                val rows = SupabaseCustomerClient.instance.fetchCustomerAddresses(currentPhone)
                if (rows.isNotEmpty()) {
                    val mapped = rows.mapNotNull { row ->
                        try {
                            val id = row["id"] as? String ?: return@mapNotNull null
                            val title = row["title"] as? String ?: "Alamat"
                            val fullAddress = row["full_address"] as? String ?: ""
                            val note = row["note"] as? String ?: ""
                            val tag = row["tag"] as? String ?: "Rumah"
                            val lat = (row["latitude"] as? Number)?.toDouble() ?: -7.7956
                            val lng = (row["longitude"] as? Number)?.toDouble() ?: 110.3695
                            val isPrimary = row["is_primary"] as? Boolean ?: false

                            val emoji = when (tag.lowercase()) {
                                "rumah" -> "🏠"
                                "apartemen" -> "🏢"
                                "kantor" -> "💼"
                                "hotel" -> "🏨"
                                else -> "📍"
                            }

                            SavedAddress(
                                id = id,
                                title = title,
                                fullAddress = fullAddress,
                                note = note,
                                isPrimary = isPrimary,
                                iconEmoji = emoji,
                                latitude = lat,
                                longitude = lng
                            )
                        } catch (_: Exception) {
                            null
                        }
                    }

                    if (mapped.isNotEmpty()) {
                        _profile.update { current ->
                            val merged = (mapped + current.savedAddresses).distinctBy { it.id }
                            val updated = current.copy(savedAddresses = merged)
                            persistProfile(updated)
                            updated
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchCustomerProfileFromSupabase() {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val phone = _profile.value.phone
                if (phone.isBlank()) return@launch
                var clean = phone.replace("[^0-9]".toRegex(), "")
                if (clean.startsWith("0")) clean = "62" + clean.substring(1)
                val localPhone = if (clean.startsWith("62")) "0" + clean.substring(2) else clean

                val req = Request.Builder()
                    .url("${SupabaseConfig.URL}/rest/v1/profiles?or=(phone.eq.$clean,phone.eq.$localPhone,phone.eq.%2B$clean)&select=*")
                    .header("apikey", SupabaseConfig.ANON_KEY)
                    .header("Authorization", "Bearer ${SupabaseConfig.ANON_KEY}")
                    .build()
                var bodyStr = "[]"
                var isSuccess = false
                SupabaseCustomerClient.instance.client.newCall(req).execute().use { res ->
                    isSuccess = res.isSuccessful
                    bodyStr = res.body?.string() ?: "[]"
                }

                if (isSuccess && bodyStr.startsWith("[{")) {
                    val arr = com.google.gson.JsonParser.parseString(bodyStr).asJsonArray
                    if (arr.size() > 0) {
                        val obj = arr[0].asJsonObject
                        val id = obj.get("id")?.asString ?: _profile.value.id
                        val name = obj.get("name")?.asString ?: _profile.value.name
                        val email = obj.get("email")?.asString ?: _profile.value.email
                        val wallet = obj.get("wallet_balance")?.asLong ?: _profile.value.walletBalance

                        _profile.update { current ->
                            val updated = current.copy(
                                id = id,
                                name = name,
                                email = email,
                                walletBalance = wallet
                            )
                            persistProfile(updated)
                            updated
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun updateProfileInfo(name: String, phone: String, email: String = "", id: String = "") {
        _profile.update { current ->
            val updated = current.copy(
                name = if (name.isNotBlank()) name else current.name,
                phone = if (phone.isNotBlank()) phone else current.phone,
                email = if (email.isNotBlank()) email else current.email,
                id = if (id.isNotBlank()) id else current.id
            )
            persistProfile(updated)
            updated
        }
        fetchSavedAddressesFromSupabase()
        fetchFavoriteTherapistsFromSupabase()
        fetchCustomerProfileFromSupabase()
        CustomerOrderRepository.instance.fetchOrderHistoryFromSupabase()
    }

    fun setLocation(location: CustomerLocation) {
        _currentLocation.value = location
        persistLocation(location)
    }

    fun selectAddress(address: SavedAddress) {
        val newLoc = CustomerLocation(
            title = address.title,
            address = address.fullAddress,
            notes = address.note,
            latitude = address.latitude,
            longitude = address.longitude
        )
        _currentLocation.value = newLoc
        persistLocation(newLoc)
    }

    fun addAndSelectAddress(address: SavedAddress) {
        _profile.update { current ->
            val updatedList = listOf(address) + current.savedAddresses.filterNot { it.id == address.id }
            val updated = current.copy(savedAddresses = updatedList)
            persistProfile(updated)
            updated
        }
        selectAddress(address)

        // Asynchronously save to Supabase Cloud Database
        coroutineScope.launch {
            val phone = _profile.value.phone
            if (phone.isNotBlank()) {
                SupabaseCustomerClient.instance.saveCustomerAddress(
                    id = address.id,
                    customerPhone = phone,
                    title = address.title,
                    fullAddress = address.fullAddress,
                    note = address.note,
                    tag = address.title,
                    latitude = address.latitude,
                    longitude = address.longitude,
                    isPrimary = address.isPrimary
                )
            }
        }
    }

    fun addSavedAddress(address: SavedAddress) {
        _profile.update { current ->
            val updatedList = current.savedAddresses.filterNot { it.id == address.id } + address
            val updated = current.copy(savedAddresses = updatedList)
            persistProfile(updated)
            updated
        }

        // Asynchronously save to Supabase Cloud Database
        coroutineScope.launch {
            val phone = _profile.value.phone
            if (phone.isNotBlank()) {
                SupabaseCustomerClient.instance.saveCustomerAddress(
                    id = address.id,
                    customerPhone = phone,
                    title = address.title,
                    fullAddress = address.fullAddress,
                    note = address.note,
                    tag = address.title,
                    latitude = address.latitude,
                    longitude = address.longitude,
                    isPrimary = address.isPrimary
                )
            }
        }
    }

    fun deleteSavedAddress(addressId: String) {
        _profile.update { current ->
            val updatedList = current.savedAddresses.filterNot { it.id == addressId }
            val updated = current.copy(savedAddresses = updatedList)
            persistProfile(updated)
            updated
        }

        // Asynchronously delete from Supabase Cloud Database
        coroutineScope.launch {
            SupabaseCustomerClient.instance.deleteCustomerAddress(addressId)
        }
    }

    fun topUpWallet(amount: Long) {
        _profile.update { current ->
            val updated = current.copy(walletBalance = current.walletBalance + amount)
            persistProfile(updated)
            syncCustomerWalletToSupabase(updated.walletBalance)
            updated
        }
    }

    fun deductWallet(amount: Long): Boolean {
        if (_profile.value.walletBalance >= amount) {
            _profile.update { current ->
                val updated = current.copy(walletBalance = current.walletBalance - amount)
                persistProfile(updated)
                syncCustomerWalletToSupabase(updated.walletBalance)
                updated
            }
            return true
        }
        return false
    }

    private fun syncCustomerWalletToSupabase(newBalance: Long) {
        val phone = _profile.value.phone
        if (phone.isBlank()) return
        coroutineScope.launch {
            try {
                var clean = phone.replace("[^0-9]".toRegex(), "")
                if (clean.startsWith("0")) clean = "62" + clean.substring(1)
                val updateJson = com.google.gson.JsonObject().apply {
                    addProperty("wallet_balance", newBalance)
                }.toString()
                val req = Request.Builder()
                    .url("${SupabaseConfig.URL}/rest/v1/profiles?phone=eq.$clean")
                    .patch(updateJson.toRequestBody(SupabaseConfig.JSON_MEDIA))
                    .header("apikey", SupabaseConfig.ANON_KEY)
                    .header("Authorization", "Bearer ${SupabaseConfig.ANON_KEY}")
                    .build()
                SupabaseCustomerClient.instance.client.newCall(req).execute().use { }
            } catch (_: Exception) {}
        }
    }

    private fun loadPersistedFavorites(): List<FavoriteTherapist> {
        val p = prefs ?: return emptyList()
        val json = p.getString("FAVORITE_THERAPISTS_JSON", null)
        if (!json.isNullOrBlank()) {
            return try {
                val type = object : TypeToken<List<FavoriteTherapist>>() {}.type
                val list: List<FavoriteTherapist> = gson.fromJson(json, type) ?: emptyList()
                list
            } catch (_: Exception) {
                emptyList()
            }
        }
        return emptyList()
    }

    fun fetchFavoriteTherapistsFromSupabase() {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                var cleanPhone = _profile.value.phone.replace("[^0-9]".toRegex(), "")
                if (cleanPhone.startsWith("0")) cleanPhone = "62" + cleanPhone.substring(1)
                else if (cleanPhone.startsWith("8")) cleanPhone = "62" + cleanPhone
                val localPhone = if (cleanPhone.startsWith("62")) "0" + cleanPhone.substring(2) else cleanPhone

                if (cleanPhone.isBlank()) return@launch

                // 1. Query reviews where customer saved favorite or gave 5-star rating
                val req = Request.Builder()
                    .url("${SupabaseConfig.URL}/rest/v1/reviews?reviewer_type=eq.CUSTOMER&or=(reviewer_id.eq.$cleanPhone,reviewer_id.eq.$localPhone)&select=*&order=created_at.desc")
                    .header("apikey", SupabaseConfig.ANON_KEY)
                    .header("Authorization", "Bearer ${SupabaseConfig.ANON_KEY}")
                    .get()
                    .build()

                val listType = object : TypeToken<List<Map<String, Any>>>() {}.type
                val reviews: List<Map<String, Any>> = try {
                    SupabaseCustomerClient.instance.client.newCall(req).execute().use { res ->
                        if (res.isSuccessful) {
                            val body = res.body?.string() ?: "[]"
                            gson.fromJson(body, listType) ?: emptyList()
                        } else emptyList()
                    }
                } catch (_: Exception) {
                    emptyList()
                }

                val favTherapistIds = reviews.mapNotNull { r ->
                    val targetId = r["target_id"] as? String
                    val tags = (r["tags"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                    if (!targetId.isNullOrBlank() && tags.contains("FAVORITE_THERAPIST")) {
                        targetId
                    } else null
                }.distinct()

                if (favTherapistIds.isNotEmpty()) {
                    val loadedFavorites = mutableListOf<FavoriteTherapist>()
                    for (tid in favTherapistIds) {
                        try {
                            val rec = com.massago.customer.data.network.SupabaseCustomerClient.instance.fetchTherapistLiveRecord(tid)
                            if (rec != null) {
                                val name = (rec["name"] as? String) ?: "Terapis Langganan"
                                val gender = (rec["gender"] as? String) ?: "Wanita"
                                val rating = (rec["rating"] as? Number)?.toDouble() ?: 4.95
                                val orders = (rec["orders_completed"] as? Number)?.toInt() ?: 25
                                val isOnline = (rec["is_online"] as? Boolean) == true
                                val phone = (rec["phone"] as? String) ?: ""
                                val initials = name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase().ifBlank { "TL" }

                                loadedFavorites.add(
                                    FavoriteTherapist(
                                        id = (rec["id"] as? String) ?: tid,
                                        name = name,
                                        gender = gender,
                                        rating = rating,
                                        ordersCompleted = orders,
                                        specialty = "Tradisional & Relaksasi",
                                        avatarInitials = initials,
                                        isOnline = isOnline,
                                        phone = phone
                                    )
                                )
                            }
                        } catch (_: Exception) {}
                    }

                    if (loadedFavorites.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            _favoriteTherapists.update { cur ->
                                (loadedFavorites + cur).distinctBy { it.id }
                            }
                            persistFavorites(_favoriteTherapists.value)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearAllFavorites() {
        _favoriteTherapists.value = emptyList()
        prefs?.edit()?.remove("FAVORITE_THERAPISTS_JSON")?.apply()
    }

    fun isTherapistFavorite(therapistId: String): Boolean {
        return _favoriteTherapists.value.any { it.id == therapistId }
    }

    fun addFavoriteTherapist(therapist: FavoriteTherapist) {
        _favoriteTherapists.update { current ->
            val mutable = current.toMutableList()
            mutable.removeAll { it.id == therapist.id }
            // Enforce max 5 favorite therapists
            while (mutable.size >= 5) {
                mutable.removeAt(mutable.size - 1)
            }
            mutable.add(0, therapist)
            persistFavorites(mutable)
            mutable
        }

        // Sync to Supabase reviews table as Cloud Favorite
        coroutineScope.launch(Dispatchers.IO) {
            try {
                var cleanPhone = _profile.value.phone.replace("[^0-9]".toRegex(), "")
                if (cleanPhone.startsWith("0")) cleanPhone = "62" + cleanPhone.substring(1)
                else if (cleanPhone.startsWith("8")) cleanPhone = "62" + cleanPhone

                if (cleanPhone.isNotBlank()) {
                    val payload = com.google.gson.JsonObject().apply {
                        addProperty("id", "REV-FAV-$cleanPhone-${therapist.id}")
                        addProperty("order_id", "FAV-INITIAL")
                        addProperty("reviewer_type", "CUSTOMER")
                        addProperty("reviewer_id", cleanPhone)
                        addProperty("target_id", therapist.id)
                        addProperty("rating", 5)
                        val tagsArr = com.google.gson.JsonArray().apply { add("FAVORITE_THERAPIST") }
                        add("tags", tagsArr)
                        addProperty("review_text", "Saved as Favorite")
                    }

                    val req = Request.Builder()
                        .url("${SupabaseConfig.URL}/rest/v1/reviews")
                        .header("apikey", SupabaseConfig.ANON_KEY)
                        .header("Authorization", "Bearer ${SupabaseConfig.ANON_KEY}")
                        .header("Prefer", "resolution=merge-duplicates")
                        .post(payload.toString().toRequestBody(SupabaseConfig.JSON_MEDIA))
                        .build()

                    SupabaseCustomerClient.instance.client.newCall(req).execute().use { }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun removeFavoriteTherapist(therapistId: String) {
        _favoriteTherapists.update { current ->
            val mutable = current.toMutableList()
            mutable.removeAll { it.id == therapistId }
            persistFavorites(mutable)
            mutable
        }

        // Remove from Supabase Cloud reviews table
        coroutineScope.launch(Dispatchers.IO) {
            try {
                var cleanPhone = _profile.value.phone.replace("[^0-9]".toRegex(), "")
                if (cleanPhone.startsWith("0")) cleanPhone = "62" + cleanPhone.substring(1)
                else if (cleanPhone.startsWith("8")) cleanPhone = "62" + cleanPhone
                val localPhone = if (cleanPhone.startsWith("62")) "0" + cleanPhone.substring(2) else cleanPhone

                if (cleanPhone.isNotBlank()) {
                    val deleteUrl = "${SupabaseConfig.URL}/rest/v1/reviews?reviewer_type=eq.CUSTOMER&or=(reviewer_id.eq.$cleanPhone,reviewer_id.eq.$localPhone)&target_id=eq.$therapistId"
                    val req = Request.Builder()
                        .url(deleteUrl)
                        .header("apikey", SupabaseConfig.ANON_KEY)
                        .header("Authorization", "Bearer ${SupabaseConfig.ANON_KEY}")
                        .delete()
                        .build()

                    SupabaseCustomerClient.instance.client.newCall(req).execute().use { }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleFavoriteTherapist(therapist: FavoriteTherapist) {
        if (isTherapistFavorite(therapist.id)) {
            removeFavoriteTherapist(therapist.id)
        } else {
            addFavoriteTherapist(therapist)
        }
    }

    private fun persistFavorites(list: List<FavoriteTherapist>) {
        try {
            val json = gson.toJson(list)
            prefs?.edit()?.putString("FAVORITE_THERAPISTS_JSON", json)?.apply()
        } catch (_: Exception) {}
    }

    suspend fun updateCustomerProfileInSupabase(
        name: String,
        phone: String,
        email: String = ""
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            var cleanPhone = phone.replace("[^0-9]".toRegex(), "")
            if (cleanPhone.startsWith("0")) cleanPhone = "62" + cleanPhone.substring(1)
            else if (cleanPhone.startsWith("8")) cleanPhone = "62" + cleanPhone
            else if (cleanPhone.startsWith("620")) cleanPhone = "62" + cleanPhone.substring(3)

            val localPhone = if (cleanPhone.startsWith("62")) "0" + cleanPhone.substring(2) else cleanPhone

            val payload = com.google.gson.JsonObject().apply {
                addProperty("full_name", name)
                addProperty("phone", cleanPhone)
                if (email.isNotBlank()) addProperty("email", email)
            }

            val req = Request.Builder()
                .url("${SupabaseConfig.URL}/rest/v1/profiles?or=(phone.eq.$cleanPhone,phone.eq.$localPhone,phone.eq.%2B$cleanPhone)")
                .header("apikey", SupabaseConfig.ANON_KEY)
                .header("Authorization", "Bearer ${SupabaseConfig.ANON_KEY}")
                .patch(payload.toString().toRequestBody(SupabaseConfig.JSON_MEDIA))
                .build()

            var isSuccess = false
            SupabaseCustomerClient.instance.client.newCall(req).execute().use { res ->
                isSuccess = res.isSuccessful
            }
            if (isSuccess) {
                updateProfileInfo(name = name, phone = cleanPhone, email = email)
                Result.success(true)
            } else {
                Result.failure(Exception("Gagal memperbarui profil di server"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        val instance: CustomerUserRepository by lazy { CustomerUserRepository() }
    }
}

data class FavoriteTherapist(
    val id: String,
    val name: String,
    val gender: String = "Wanita",
    val rating: Double = 4.95,
    val ordersCompleted: Int = 120,
    val specialty: String = "Tradisional & Refleksi",
    val avatarInitials: String = "BS",
    val isOnline: Boolean = true,
    val phone: String = ""
)

enum class TherapistAvailabilityStatus {
    CHECKING,
    ONLINE_READY,
    OFFLINE,
    BUSY_HANDLING_OTHER,
    OUT_OF_RANGE
}

data class TherapistLiveStatus(
    val therapistId: String,
    val name: String,
    val isOnline: Boolean,
    val dutyStatus: String,
    val status: TherapistAvailabilityStatus,
    val distanceKm: Double = 0.0,
    val maxRadiusKm: Double = 10.0,
    val isOutOfRange: Boolean = false,
    val extraTravelSurcharge: Long = 0L,
    val busyRemainingMinutes: Int? = null,
    val activeOrderId: String? = null
)
