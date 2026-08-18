package com.massago.mitra.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.massago.mitra.data.model.ChatMessage
import com.massago.mitra.data.model.Order
import com.massago.mitra.data.repository.ChatRepository
import com.massago.mitra.data.repository.OrderRepository
import com.massago.mitra.data.repository.TherapistRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ChatViewModel(
    private val chatRepository: ChatRepository = ChatRepository.instance,
    private val orderRepository: OrderRepository = OrderRepository.instance,
    private val therapistRepository: TherapistRepository = TherapistRepository.instance
) : ViewModel() {

    val messages: StateFlow<List<ChatMessage>> = chatRepository.messages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeOrder: StateFlow<Order?> = orderRepository.activeOrder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val quickReplies = chatRepository.quickReplies

    fun initializeChat(orderId: String, clientName: String) {
        chatRepository.initializeChatForOrder(clientName, orderId)
    }

    fun sendMessage(text: String) {
        val tName = therapistRepository.therapistProfile.value.name.ifBlank { "Mitra Terapis" }
        chatRepository.sendMessage(text, imageBase64 = null, isTherapist = true, senderName = tName)
    }

    fun sendImage(base64: String) {
        val tName = therapistRepository.therapistProfile.value.name.ifBlank { "Mitra Terapis" }
        chatRepository.sendMessage("", imageBase64 = base64, isTherapist = true, senderName = tName)
    }
}
