package com.massago.customer.data.repository

import com.massago.customer.data.model.CustomerLocation
import com.massago.customer.data.model.CustomerProfile
import com.massago.customer.data.model.SavedAddress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CustomerUserRepository private constructor() {

    private val _profile = MutableStateFlow(CustomerProfile())
    val profile: StateFlow<CustomerProfile> = _profile.asStateFlow()

    private val _currentLocation = MutableStateFlow(CustomerLocation())
    val currentLocation: StateFlow<CustomerLocation> = _currentLocation.asStateFlow()

    fun updateProfileInfo(name: String, phone: String, email: String = "", id: String = "") {
        _profile.update { current ->
            current.copy(
                name = if (name.isNotBlank()) name else current.name,
                phone = if (phone.isNotBlank()) phone else current.phone,
                email = if (email.isNotBlank()) email else current.email,
                id = if (id.isNotBlank()) id else current.id
            )
        }
    }

    fun setLocation(location: CustomerLocation) {
        _currentLocation.value = location
    }

    fun selectAddress(address: SavedAddress) {
        _currentLocation.value = CustomerLocation(
            title = address.title,
            address = address.fullAddress,
            notes = address.note,
            latitude = address.latitude,
            longitude = address.longitude
        )
    }

    fun addAndSelectAddress(address: SavedAddress) {
        _profile.update { current ->
            current.copy(savedAddresses = listOf(address) + current.savedAddresses.filterNot { it.id == address.id })
        }
        selectAddress(address)
    }

    fun topUpWallet(amount: Long) {
        _profile.update { current ->
            current.copy(walletBalance = current.walletBalance + amount)
        }
    }

    fun deductWallet(amount: Long): Boolean {
        if (_profile.value.walletBalance >= amount) {
            _profile.update { current ->
                current.copy(walletBalance = current.walletBalance - amount)
            }
            return true
        }
        return false
    }

    fun addSavedAddress(address: SavedAddress) {
        _profile.update { current ->
            current.copy(savedAddresses = current.savedAddresses + address)
        }
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
