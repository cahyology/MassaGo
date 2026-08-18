package com.pijatin.customer.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class CustomerChatMessage(
    val id: String = UUID.randomUUID().toString().takeLast(8),
    val senderId: String = "CUSTOMER",
    val senderName: String = "Pelanggan",
    val isFromCustomer: Boolean = true,
    val isFromTherapist: Boolean = false,
    val message: String = "",
    val imageBase64: String? = null,
    val timestampMillis: Long = System.currentTimeMillis(),
    val isRead: Boolean = true
) {
    val isMe: Boolean get() = senderId.equals("CUSTOMER", ignoreCase = true) || isFromCustomer || (!isFromTherapist && senderId != "THERAPIST")

    val formattedTime: String get() {
        val sdf = SimpleDateFormat("HH:mm", Locale("id", "ID"))
        return sdf.format(Date(timestampMillis))
    }
}
