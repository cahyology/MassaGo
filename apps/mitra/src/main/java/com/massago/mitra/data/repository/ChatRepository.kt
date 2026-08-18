package com.massago.mitra.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.massago.mitra.MassaGoApp
import com.massago.mitra.data.model.ChatMessage
import com.massago.mitra.data.network.SupabaseClient
import com.massago.mitra.util.NotificationSoundHelper
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

class ChatRepository private constructor() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var syncJob: Job? = null
    private val gson = Gson()
    private val client = OkHttpClient()

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
    }

    fun markChatScreenClosed() {
        isChatScreenActive = false
    }

    fun initializeChatForOrder(clientName: String, orderId: String? = null) {
        currentOrderId = orderId
        if (_messages.value.isEmpty()) {
            val now = System.currentTimeMillis()
            _messages.value = listOf(
                ChatMessage(
                    id = "MSG-1",
                    senderId = "CUSTOMER",
                    senderName = clientName,
                    isFromCustomer = true,
                    isFromTherapist = false,
                    message = "Halo Pak terapis, nanti kalau sudah sampai lobi kabari ya 🙏",
                    timestampMillis = now - 120000L
                )
            )
            lastSeenMessageCount = _messages.value.size
        }

        if (!orderId.isNullOrBlank()) {
            startChatSync(orderId, clientName)
        }
    }

    private fun startChatSync(orderId: String, clientName: String) {
        syncJob?.cancel()
        syncJob = scope.launch {
            while (currentOrderId == orderId) {
                try {
                    val orderData = SupabaseClient.instance.fetchOrder(orderId)
                    val rawChatJson = orderData?.get("customer_id") as? String
                    if (!rawChatJson.isNullOrBlank() && rawChatJson.startsWith("[")) {
                        val type = object : TypeToken<List<ChatMessage>>() {}.type
                        val remoteList: List<ChatMessage> = gson.fromJson(rawChatJson, type)
                        if (remoteList.isNotEmpty() && remoteList.size != _messages.value.size) {
                            val newMsgs = remoteList.filter { !it.isMe && it.timestampMillis > (System.currentTimeMillis() - 10000) }
                            if (newMsgs.isNotEmpty() && remoteList.size > lastSeenMessageCount) {
                                val latest = newMsgs.last()
                                if (!isChatScreenActive) {
                                    _unreadCount.value += newMsgs.size
                                }
                                try {
                                    NotificationSoundHelper.notifyNewChatMessage(
                                        MassaGoApp.instance,
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
            .url("https://jrwkmedrrwvomyljdkpw.supabase.co/rest/v1/orders?id=eq.$orderId")
            .addHeader("apikey", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Impyd2ttZWRycnd2b215bGpka3B3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODY5MTcxNzQsImV4cCI6MjEwMjQ5MzE3NH0.UiN6JvJt23ds-3eID9J6wOtEt3pg4-farSwQIliPzuw")
            .addHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Impyd2ttZWRycnd2b215bGpka3B3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODY5MTcxNzQsImV4cCI6MjEwMjQ5MzE3NH0.UiN6JvJt23ds-3eID9J6wOtEt3pg4-farSwQIliPzuw")
            .patch(bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        client.newCall(request).execute()
    }

    companion object {
        val instance: ChatRepository by lazy { ChatRepository() }
    }
}
