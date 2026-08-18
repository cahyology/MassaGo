package com.pijatin.customer.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pijatin.customer.CustomerApp
import com.pijatin.customer.data.model.CustomerChatMessage
import com.pijatin.customer.data.network.SupabaseCustomerClient
import com.pijatin.customer.util.CustomerNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID

class CustomerChatRepository private constructor() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var syncJob: Job? = null
    private val gson = Gson()
    private val client = OkHttpClient()

    private val _messages = MutableStateFlow<List<CustomerChatMessage>>(emptyList())
    val messages: StateFlow<List<CustomerChatMessage>> = _messages.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private var isChatScreenActive = false

    private var currentOrderId: String? = null
    private var lastSeenMessageCount = 0

    val quickReplies = listOf(
        "Saya sudah di kamar / unit",
        "Tolong naik ke lantai 12 ya",
        "Patokan rumah pagar hitam sebelah warung",
        "Bisa tolong tekan bel jika sudah sampai?",
        "Parkir motor ada di halaman dalam"
    )

    fun markChatScreenOpened() {
        isChatScreenActive = true
        _unreadCount.value = 0
    }

    fun markChatScreenClosed() {
        isChatScreenActive = false
    }

    fun startChatSync(orderId: String, therapistName: String) {
        if (currentOrderId == orderId && syncJob?.isActive == true) return
        currentOrderId = orderId

        if (_messages.value.isEmpty()) {
            _messages.value = listOf(
                CustomerChatMessage(
                    id = "init-1",
                    senderId = "THERAPIST",
                    senderName = therapistName,
                    isFromCustomer = false,
                    isFromTherapist = true,
                    message = "Halo Kak! Saya sedang dalam perjalanan menuju lokasi Anda ya 🙏",
                    timestampMillis = System.currentTimeMillis() - 60000
                )
            )
            lastSeenMessageCount = _messages.value.size
        }

        syncJob?.cancel()
        syncJob = scope.launch {
            while (currentOrderId == orderId) {
                try {
                    val orderData = SupabaseCustomerClient.instance.fetchOrder(orderId)
                    val rawChatJson = orderData?.get("customer_id") as? String
                    if (!rawChatJson.isNullOrBlank() && rawChatJson.startsWith("[")) {
                        val type = object : TypeToken<List<CustomerChatMessage>>() {}.type
                        val remoteList: List<CustomerChatMessage> = gson.fromJson(rawChatJson, type)
                        if (remoteList.isNotEmpty() && remoteList.size != _messages.value.size) {
                            val newMsgs = remoteList.filter { !it.isMe && it.timestampMillis > (System.currentTimeMillis() - 10000) }
                            if (newMsgs.isNotEmpty() && remoteList.size > lastSeenMessageCount) {
                                val latest = newMsgs.last()
                                if (!isChatScreenActive) {
                                    _unreadCount.value += newMsgs.size
                                }
                                try {
                                    CustomerNotificationHelper.notifyNewChatMessage(
                                        CustomerApp.instance,
                                        latest.senderName,
                                        latest.message.ifBlank { "📷 [Foto dikirim]" }
                                    )
                                } catch (_: Exception) {}
                            }
                            _messages.value = remoteList
                            lastSeenMessageCount = remoteList.size
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(1500)
            }
        }
    }

    fun stopChatSync() {
        syncJob?.cancel()
        syncJob = null
    }

    fun sendMessage(text: String, imageBase64: String? = null, customerName: String = "Anda") {
        if (text.isBlank() && imageBase64.isNullOrBlank()) return

        val newMsg = CustomerChatMessage(
            id = "MSG-" + UUID.randomUUID().toString().takeLast(6),
            senderId = "CUSTOMER",
            senderName = customerName,
            isFromCustomer = true,
            isFromTherapist = false,
            message = text.trim(),
            imageBase64 = imageBase64,
            timestampMillis = System.currentTimeMillis()
        )

        val updatedList = _messages.value + newMsg
        _messages.value = updatedList
        lastSeenMessageCount = updatedList.size

        val orderId = currentOrderId ?: return
        scope.launch {
            try {
                pushChatToSupabase(orderId, updatedList)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun pushChatToSupabase(orderId: String, list: List<CustomerChatMessage>) {
        val json = gson.toJson(list)
        val bodyJson = com.google.gson.JsonObject().apply {
            addProperty("customer_id", json)
        }.toString()

        val request = Request.Builder()
            .url("https://jrwkmedrrwvomyljdkpw.supabase.co/rest/v1/orders?id=eq.$orderId")
            .addHeader("apikey", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Impyd2ttZWRycnd2b215bGpka3B3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODY5MTcxNzQsImV4cCI6MjEwMjQ5MzE3NH0.UiN6JvJt23ds-3eID9J6wOtEt3pg4-farSwQIliPzuw")
            .addHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Impyd2ttZWRycnd2b215bGpka3B3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODY5MTcxNzQsImV4cCI6MjEwMjQ5MzE3NH0.UiN6JvJt23ds-3eID9J6wOtEt3pg4-farSwQIliPzuw")
            .patch(bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        client.newCall(request).execute()
    }

    companion object {
        val instance: CustomerChatRepository by lazy { CustomerChatRepository() }
    }
}
