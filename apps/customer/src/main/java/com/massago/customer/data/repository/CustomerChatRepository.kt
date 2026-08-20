package com.massago.customer.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.massago.customer.CustomerApp
import com.massago.customer.data.model.CustomerChatMessage
import com.massago.customer.data.network.SupabaseConfig
import com.massago.customer.data.network.SupabaseCustomerClient
import com.massago.customer.util.CustomerNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID

class CustomerChatRepository private constructor() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var syncJob: Job? = null
    private val gson = Gson()

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

        syncJob?.cancel()
        syncJob = scope.launch {
            while (currentOrderId == orderId) {
                try {
                    val orderData = SupabaseCustomerClient.instance.fetchOrder(orderId)
                    val rawChatJson = orderData?.get("customer_id") as? String
                    if (!rawChatJson.isNullOrBlank() && rawChatJson.startsWith("[")) {
                        val type = object : TypeToken<List<CustomerChatMessage>>() {}.type
                        val remoteList: List<CustomerChatMessage> = gson.fromJson(rawChatJson, type) ?: emptyList()
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
                delay(2000)
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
            .url("${SupabaseConfig.URL}/rest/v1/orders?id=eq.$orderId")
            .addHeader("apikey", SupabaseConfig.ANON_KEY)
            .addHeader("Authorization", "Bearer ${SupabaseConfig.ANON_KEY}")
            .patch(bodyJson.toRequestBody(SupabaseConfig.JSON_MEDIA))
            .build()

        SupabaseCustomerClient.instance.client.newCall(request).execute().use { }
    }

    companion object {
        val instance: CustomerChatRepository by lazy { CustomerChatRepository() }
    }
}
