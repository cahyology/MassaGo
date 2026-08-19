package com.massago.customer.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.massago.customer.CustomerApp
import com.massago.customer.data.model.CustomerLocation
import com.massago.customer.data.model.CustomerProfile
import com.massago.customer.data.model.SavedAddress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CustomerUserRepository private constructor() {

    private val gson = Gson()

    private val prefs by lazy {
        try {
            CustomerApp.instance.getSharedPreferences("massago_customer_user_prefs", Context.MODE_PRIVATE)
        } catch (_: Exception) {
            null
        }
    }

    private val _profile = MutableStateFlow(loadPersistedProfile())
    val profile: StateFlow<CustomerProfile> = _profile.asStateFlow()

    private val _currentLocation = MutableStateFlow(loadPersistedLocation())
    val currentLocation: StateFlow<CustomerLocation> = _currentLocation.asStateFlow()

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
    }

    fun addSavedAddress(address: SavedAddress) {
        _profile.update { current ->
            val updatedList = current.savedAddresses.filterNot { it.id == address.id } + address
            val updated = current.copy(savedAddresses = updatedList)
            persistProfile(updated)
            updated
        }
    }

    fun deleteSavedAddress(addressId: String) {
        _profile.update { current ->
            val updatedList = current.savedAddresses.filterNot { it.id == addressId }
            val updated = current.copy(savedAddresses = updatedList)
            persistProfile(updated)
            updated
        }
    }

    fun topUpWallet(amount: Long) {
        _profile.update { current ->
            val updated = current.copy(walletBalance = current.walletBalance + amount)
            persistProfile(updated)
            updated
        }
    }

    fun deductWallet(amount: Long): Boolean {
        if (_profile.value.walletBalance >= amount) {
            _profile.update { current ->
                val updated = current.copy(walletBalance = current.walletBalance - amount)
                persistProfile(updated)
                updated
            }
            return true
        }
        return false
    }

    suspend fun updateCustomerProfileInSupabase(
        name: String,
        phone: String,
        email: String = ""
    ): Result<Boolean> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
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

            val req = okhttp3.Request.Builder()
                .url("${com.massago.customer.data.network.SupabaseConfig.URL}/rest/v1/customers?or=(phone.eq.$cleanPhone,phone.eq.$localPhone)")
                .header("apikey", com.massago.customer.data.network.SupabaseConfig.ANON_KEY)
                .header("Authorization", "Bearer ${com.massago.customer.data.network.SupabaseConfig.ANON_KEY}")
                .patch(okhttp3.RequestBody.create(com.massago.customer.data.network.SupabaseConfig.JSON_MEDIA, payload.toString()))
                .build()

            val client = okhttp3.OkHttpClient()
            val res = client.newCall(req).execute()
            if (res.isSuccessful) {
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
