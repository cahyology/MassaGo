package com.massago.mitra.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString().takeLast(8),
    val senderId: String = "THERAPIST",
    val senderName: String = "Mitra Terapis",
    val isFromCustomer: Boolean = false,
    val isFromTherapist: Boolean = true,
    val message: String = "",
    val imageBase64: String? = null,
    val timestampMillis: Long = System.currentTimeMillis(),
    val isRead: Boolean = true
) {
    val isMe: Boolean get() = senderId.equals("THERAPIST", ignoreCase = true) || isFromTherapist || (!isFromCustomer && senderId != "CUSTOMER")

    val formattedTime: String get() {
        val sdf = SimpleDateFormat("HH:mm", Locale("id", "ID"))
        return sdf.format(Date(timestampMillis))
    }
}
