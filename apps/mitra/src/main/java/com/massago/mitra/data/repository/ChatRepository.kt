package com.massago.mitra.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.massago.mitra.MassaGoApp
import com.massago.mitra.data.model.ChatMessage
import com.massago.mitra.data.network.SupabaseClient
import com.massago.mitra.data.network.SupabaseConfig
import com.massago.mitra.util.NotificationSoundHelper
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

class ChatRepository private constructor() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var syncJob: Job? = null
    private val gson = Gson()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private var isChatScreenActive = false

    private var currentOrderId: String? = null
    private var lastSeenMessageCount = 0

    val quickReplies = listOf(
        "Halo, saya sedang menuju ke lokasi Anda saat ini 🛵",
        "Mohon tunggu sebentar ya, jalanan sedikit padat ⏳",
        "Saya sudah tiba di depan rumah / lobi apartemen 🏠",
        "Bisa tolong infokan patokan rumah / blok apartemennya?",
        "Semua perlengkapan & minyak aromaterapi sudah steril 🧘‍♂️"
    )

    fun markChatScreenOpened() {
        isChatScreenActive = true
        _unreadCount.value = 0
        lastSeenMessageCount = _messages.value.size
    }

    fun markChatScreenClosed() {
        isChatScreenActive = false
    }

    fun initializeChatForOrder(customerName: String, orderId: String) {
        currentOrderId = orderId
        lastSeenMessageCount = 0
        _unreadCount.value = 0
        _messages.value = emptyList()

        startChatSync(orderId)
    }

    private fun startChatSync(orderId: String) {
        syncJob?.cancel()
        syncJob = scope.launch {
            while (true) {
                try {
                    val orderMap = SupabaseClient.instance.fetchOrder(orderId)
                    if (orderMap != null) {
                        val rawChatJson = orderMap["customer_id"] as? String
                        if (!rawChatJson.isNullOrBlank() && rawChatJson.startsWith("[")) {
                            val listType = object : TypeToken<List<ChatMessage>>() {}.type
                            val remoteMessages: List<ChatMessage> = gson.fromJson(rawChatJson, listType) ?: emptyList()

                            if (remoteMessages.size > _messages.value.size) {
                                val newIncoming = remoteMessages.drop(_messages.value.size)
                                val hasNewCustomerMsg = newIncoming.any { it.isFromCustomer }

                                if (hasNewCustomerMsg && !isChatScreenActive) {
                                    _unreadCount.value = (_unreadCount.value + newIncoming.count { it.isFromCustomer })
                                    NotificationSoundHelper.notifyNewChatMessage(MassaGoApp.instance, "Pelanggan", "Pesan baru diterima")
                                }
                            }
                            _messages.value = remoteMessages
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

    fun sendMessage(messageText: String, imageBase64: String? = null, isTherapist: Boolean = true, senderName: String = "Terapis") {
        if (messageText.isBlank() && imageBase64.isNullOrBlank()) return

        val newMsg = ChatMessage(
            id = "MSG-" + UUID.randomUUID().toString().takeLast(6),
            senderId = if (isTherapist) "THERAPIST" else "CUSTOMER",
            senderName = senderName,
            isFromCustomer = !isTherapist,
            isFromTherapist = isTherapist,
            message = messageText.trim(),
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

    private fun pushChatToSupabase(orderId: String, list: List<ChatMessage>) {
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

        SupabaseClient.instance.client.newCall(request).execute().use { }
    }

    companion object {
        val instance: ChatRepository by lazy { ChatRepository() }
    }
}
