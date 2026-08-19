package com.massago.mitra.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.massago.mitra.data.model.ChecklistItemType
import com.massago.mitra.data.model.ClientInfo
import com.massago.mitra.data.model.DutyStatus
import com.massago.mitra.data.model.Order
import com.massago.mitra.data.model.OrderStatus
import com.massago.mitra.data.model.PaymentMethod
import com.massago.mitra.data.model.PredefinedServices
import com.massago.mitra.data.network.SupabaseClient
import com.massago.mitra.data.network.SupabaseConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.UUID

class OrderRepository private constructor(
    private val therapistRepository: TherapistRepository = TherapistRepository.instance,
    private val walletRepository: WalletRepository = WalletRepository.instance,
    private val chatRepository: ChatRepository = ChatRepository.instance
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var timerJob: Job? = null
    private var incomingTimeoutJob: Job? = null
    private var pollingJob: Job? = null
    private var orderStatusMonitorJob: Job? = null

    private val _activeOrder = MutableStateFlow<Order?>(null)
    val activeOrder: StateFlow<Order?> = _activeOrder.asStateFlow()

    private val gson = Gson()

    private val prefs by lazy {
        try {
            com.massago.mitra.MassaGoApp.instance.getSharedPreferences("massago_mitra_order_prefs", android.content.Context.MODE_PRIVATE)
        } catch (_: Exception) {
            null
        }
    }

    private fun loadPersistedOrderHistory(): List<Order> {
        val historyJson = prefs?.getString("MITRA_ORDER_HISTORY_JSON", null) ?: return emptyList()
        return try {
            val listType = object : TypeToken<List<Order>>() {}.type
            gson.fromJson(historyJson, listType) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persistOrderHistory(history: List<Order>) {
        prefs?.edit()?.putString("MITRA_ORDER_HISTORY_JSON", gson.toJson(history))?.apply()
    }

    private val _orderHistory = MutableStateFlow<List<Order>>(emptyList())
    val orderHistory: StateFlow<List<Order>> = _orderHistory.asStateFlow()

    private val _incomingCountdownSeconds = MutableStateFlow(30)
    val incomingCountdownSeconds: StateFlow<Int> = _incomingCountdownSeconds.asStateFlow()

    // Blacklist of orders declined/dismissed by this therapist to never receive the same order twice
    private val dismissedOrderIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    init {
        try {
            _orderHistory.value = loadPersistedOrderHistory()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        restoreActiveOrderIfAny()
        fetchOrderHistoryFromSupabase()
    }

    fun fetchOrderHistoryFromSupabase() {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val profile = therapistRepository.therapistProfile.value
                val rows = SupabaseClient.instance.fetchTherapistOrders(profile.id, profile.phone)
                if (rows.isNotEmpty()) {
                    val mapped = rows.mapNotNull { row ->
                        try {
                            val id = row["id"] as? String ?: return@mapNotNull null
                            val statusStr = row["status"] as? String ?: "COMPLETED"
                            val srvName = row["service_name"] as? String ?: "Pijat Tradisional"
                            val duration = (row["duration_minutes"] as? Number)?.toInt() ?: 90
                            val totalPrice = (row["total_price"] as? Number)?.toLong() ?: 150000L
                            val rawAddress = row["address"] as? String ?: "Lokasi Pelanggan"
                            val custName = row["customer_name"] as? String ?: "Pelanggan MassaGo"
                            val custPhone = row["customer_phone"] as? String ?: ""

                            val orderStatus = when (statusStr) {
                                "COMPLETED", "FINISHED" -> OrderStatus.REVIEW_SUBMITTED
                                "CANCELLED" -> OrderStatus.COMPLETED_PAYMENT
                                "IN_SERVICE", "TREATMENT_IN_PROGRESS" -> OrderStatus.TREATMENT_IN_PROGRESS
                                "ARRIVED" -> OrderStatus.ARRIVED_AT_LOCATION
                                "ACCEPTED" -> OrderStatus.ACCEPTED_ON_THE_WAY
                                else -> OrderStatus.INCOMING
                            }

                            val matchedService = PredefinedServices.ALL_SERVICES.find {
                                it.name.contains(srvName, ignoreCase = true)
                            } ?: PredefinedServices.ALL_SERVICES[0]

                            val netEarnings = (totalPrice * 0.80).toLong()
                            val platformFee = (totalPrice * 0.20).toLong()

                            Order(
                                id = id,
                                servicePackage = matchedService,
                                client = ClientInfo(
                                    id = "CLI-" + id.takeLast(4),
                                    name = custName,
                                    phone = custPhone,
                                    gender = "Pelanggan",
                                    address = rawAddress,
                                    addressNotes = "Lokasi Pesanan Pelanggan",
                                    distanceKm = 2.5,
                                    travelEstimateMinutes = 10
                                ),
                                paymentMethod = PaymentMethod.CASH,
                                status = orderStatus
                            )
                        } catch (_: Exception) {
                            null
                        }
                    }

                    if (mapped.isNotEmpty()) {
                        val combined = (mapped + _orderHistory.value).distinctBy { it.id }
                        _orderHistory.value = combined
                        persistOrderHistory(combined)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun restoreActiveOrderIfAny() {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val savedOrderId = prefs?.getString("ACTIVE_ORDER_ID", null) ?: return@launch
                val orderData = SupabaseClient.instance.fetchOrder(savedOrderId) ?: return@launch
                val statusStr = orderData["status"] as? String ?: return@launch
                if (statusStr.startsWith("ACCEPT") || statusStr == "ARRIVED" || statusStr == "IN_SERVICE" || statusStr == "TREATMENT_IN_PROGRESS" || statusStr == "SANITATION") {
                    val srvName = orderData["service_name"] as? String ?: "Pijat Tradisional"
                    val duration = (orderData["duration_minutes"] as? Number)?.toInt() ?: 90
                    val custName = orderData["customer_name"] as? String ?: "Pelanggan MassaGo"
                    val custPhone = orderData["customer_phone"] as? String ?: "+6281234567890"
                    val rawAddress = orderData["address"] as? String ?: "Lokasi Pelanggan"
                    val price = (orderData["total_price"] as? Number)?.toLong() ?: 150000L

                    var custLat = (orderData["latitude"] as? Number)?.toDouble()
                    var custLng = (orderData["longitude"] as? Number)?.toDouble()
                    val gpsRegex = Regex("\\[GPS:([-\\d.]+),([-\\d.]+)\\]")
                    val gpsMatch = gpsRegex.find(rawAddress)
                    if (gpsMatch != null) {
                        val (latStr, lngStr) = gpsMatch.destructured
                        custLat = latStr.toDoubleOrNull() ?: custLat
                        custLng = lngStr.toDoubleOrNull() ?: custLng
                    }
                    val finalLat = custLat ?: -7.7956
                    val finalLng = custLng ?: 110.3695

                    val matchedService = PredefinedServices.ALL_SERVICES.find {
                        it.name.contains(srvName, ignoreCase = true)
                    } ?: PredefinedServices.ALL_SERVICES[0]

                    val therapistLoc = therapistRepository.therapistProfile.value
                    val distKm = calculateDistanceKm(therapistLoc.latitude, therapistLoc.longitude, finalLat, finalLng)

                    val clientInfo = ClientInfo(
                        id = "CLI-" + savedOrderId.takeLast(4),
                        name = custName,
                        phone = custPhone,
                        gender = "Pelanggan",
                        address = rawAddress.replace(gpsRegex, "").trim(),
                        addressNotes = "Titik Temu Google Maps",
                        distanceKm = (distKm * 10).toInt() / 10.0,
                        travelEstimateMinutes = (distKm * 3.5).toInt().coerceAtLeast(5),
                        latitude = finalLat,
                        longitude = finalLng
                    )

                    val mappedStatus = when (statusStr) {
                        "ARRIVED" -> OrderStatus.ARRIVED_AT_LOCATION
                        "IN_SERVICE", "TREATMENT_IN_PROGRESS" -> OrderStatus.TREATMENT_IN_PROGRESS
                        "SANITATION" -> OrderStatus.SANITATION_AND_PREP
                        else -> OrderStatus.ACCEPTED_ON_THE_WAY
                    }

                    val restored = Order(
                        id = savedOrderId,
                        client = clientInfo,
                        servicePackage = matchedService.copy(durationMinutes = duration, basePrice = price),
                        paymentMethod = PaymentMethod.DIGITAL_WALLET,
                        status = mappedStatus,
                        acceptedAtMillis = System.currentTimeMillis()
                    )

                    withContext(Dispatchers.Main) {
                        _activeOrder.value = restored
                        therapistRepository.setDutyStatus(DutyStatus.ON_DUTY_BUSY)
                        chatRepository.initializeChatForOrder(custName, savedOrderId)
                    }

                    if (mappedStatus == OrderStatus.ACCEPTED_ON_THE_WAY) {
                        startActiveOrderStatusMonitor(savedOrderId)
                    }
                } else {
                    prefs?.edit()?.remove("ACTIVE_ORDER_ID")?.apply()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Start live polling for pending orders from Supabase when therapist goes ONLINE
     */
    fun startRealtimeOrderPolling() {
        pollingJob?.cancel()
        pollingJob = coroutineScope.launch(Dispatchers.IO) {
            while (therapistRepository.therapistProfile.value.dutyStatus == DutyStatus.ONLINE) {
                if (_activeOrder.value == null) {
                    checkForRealIncomingOrder()
                }
                delay(3000) // Poll Supabase every 3 seconds
            }
        }
    }

    fun stopRealtimeOrderPolling() {
        pollingJob?.cancel()
    }

    suspend fun checkForRealIncomingOrderDirect() = withContext(Dispatchers.IO) {
        checkForRealIncomingOrder()
    }

    private suspend fun checkForRealIncomingOrder() = withContext(Dispatchers.IO) {
        try {
            val orders = SupabaseClient.instance.fetchPendingOrders()
            val now = System.currentTimeMillis()
            // Exclude orders that have already been declined or handled by this therapist or are stale (> 3 mins old)
            val eligibleOrders = orders.filter { order ->
                val orderId = order["id"] as? String ?: ""
                val createdAt = (order["created_at"] as? Number)?.toLong() ?: 0L
                val isFresh = createdAt == 0L || (now - createdAt) < (180 * 1000)
                if (!isFresh && orderId.isNotBlank()) {
                    // Auto-expire stale order in Supabase so it never rings
                    coroutineScope.launch(Dispatchers.IO) {
                        SupabaseClient.instance.declineOrder(orderId)
                    }
                }
                orderId.isNotBlank() && !dismissedOrderIds.contains(orderId) && isFresh
            }

            if (eligibleOrders.isNotEmpty() && _activeOrder.value == null) {
                val orderMap = eligibleOrders.first()
                val orderId = orderMap["id"] as? String ?: "ORD-1"
                val srvName = orderMap["service_name"] as? String ?: "Pijat Tradisional"
                val duration = (orderMap["duration_minutes"] as? Number)?.toInt() ?: 90
                val custName = orderMap["customer_name"] as? String ?: "Pelanggan MassaGo"
                val custPhone = orderMap["customer_phone"] as? String ?: "+6281234567890"
                val rawAddress = orderMap["address"] as? String ?: "Lokasi Pelanggan"
                val price = (orderMap["total_price"] as? Number)?.toLong() ?: 150000L

                // Extract [GPS:lat,lng] if embedded in address
                var custLat = (orderMap["latitude"] as? Number)?.toDouble()
                var custLng = (orderMap["longitude"] as? Number)?.toDouble()
                var addressNotes = (orderMap["notes"] as? String) ?: ""
                var cleanAddress = rawAddress

                val gpsRegex = Regex("\\[GPS:([-\\d.]+),([-\\d.]+)\\]")
                val gpsMatch = gpsRegex.find(rawAddress)
                if (gpsMatch != null) {
                    val (latStr, lngStr) = gpsMatch.destructured
                    custLat = latStr.toDoubleOrNull() ?: custLat
                    custLng = lngStr.toDoubleOrNull() ?: custLng
                    cleanAddress = cleanAddress.replace(gpsMatch.value, "").trim()
                }

                val noteRegex = Regex("\\[NOTE:(.*?)\\]")
                val noteMatch = noteRegex.find(rawAddress)
                if (noteMatch != null) {
                    val (noteStr) = noteMatch.destructured
                    addressNotes = noteStr
                    cleanAddress = cleanAddress.replace(noteMatch.value, "").trim()
                }

                val finalLat = custLat ?: -7.7956
                val finalLng = custLng ?: 110.3695

                val therapistLoc = therapistRepository.therapistProfile.value
                val orderGenderPref = (orderMap["gender_preference"] as? String) ?: "Bebas"
                val myGender = therapistLoc.gender.trim()
                val myPreferredClient = therapistLoc.preferredClientGender.trim()

                // 1. Smart Gender Matching Filter
                if (orderGenderPref.contains("Wanita", ignoreCase = true) && !myGender.equals("Wanita", ignoreCase = true)) {
                    return@withContext
                }
                if (orderGenderPref.contains("Pria", ignoreCase = true) && !myGender.equals("Pria", ignoreCase = true)) {
                    return@withContext
                }
                if (myPreferredClient.contains("Wanita", ignoreCase = true) && orderGenderPref.contains("Pria", ignoreCase = true)) {
                    return@withContext
                }
                if (myPreferredClient.contains("Pria", ignoreCase = true) && orderGenderPref.contains("Wanita", ignoreCase = true)) {
                    return@withContext
                }

                // 2. Minimum Deposit Balance Check
                if (therapistLoc.depositBalance < 0) {
                    return@withContext
                }

                val matchedService = PredefinedServices.ALL_SERVICES.find {
                    it.name.contains(srvName, ignoreCase = true)
                } ?: PredefinedServices.ALL_SERVICES[0]

                val distKm = calculateDistanceKm(therapistLoc.latitude, therapistLoc.longitude, finalLat, finalLng)

                // Radius Filter: If order is outside configured radius, ignore it
                if (distKm > therapistLoc.maxRadiusKm) {
                    return@withContext
                }

                val etaMin = (distKm * 3.5).toInt().coerceAtLeast(5)

                val clientInfo = ClientInfo(
                    id = "CLI-" + orderId.takeLast(4),
                    name = custName,
                    phone = custPhone,
                    gender = "Pelanggan",
                    address = cleanAddress.ifBlank { "Lokasi Penjemputan Pelanggan" },
                    addressNotes = addressNotes.ifBlank { "Titik Pin Google Maps" },
                    distanceKm = (distKm * 10).toInt() / 10.0,
                    travelEstimateMinutes = etaMin,
                    latitude = finalLat,
                    longitude = finalLng
                )

                val platformCut = therapistRepository.platformCommissionPercent.value
                val therapistRate = (100.0 - platformCut) / 100.0

                val incomingOrder = Order(
                    id = orderId,
                    client = clientInfo,
                    servicePackage = matchedService.copy(
                        durationMinutes = duration,
                        basePrice = price,
                        therapistCommissionRate = therapistRate
                    ),
                    status = OrderStatus.INCOMING,
                    totalTreatmentSeconds = duration * 60,
                    remainingTreatmentSeconds = duration * 60
                )

                withContext(Dispatchers.Main) {
                    if (therapistLoc.autoAcceptOrders) {
                        _activeOrder.value = incomingOrder
                        acceptOrder()
                    } else {
                        _activeOrder.value = incomingOrder
                        _incomingCountdownSeconds.value = 30
                        startIncomingOrderCountdown()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startIncomingOrderCountdown() {
        incomingTimeoutJob?.cancel()
        incomingTimeoutJob = coroutineScope.launch {
            for (i in 30 downTo 0) {
                _incomingCountdownSeconds.value = i
                delay(1000)
                if (_activeOrder.value?.status != OrderStatus.INCOMING) {
                    break
                }
            }
            if (_activeOrder.value?.status == OrderStatus.INCOMING) {
                declineOrder("Waktu penerimaan habis")
            }
        }
    }

    fun acceptOrder() {
        incomingTimeoutJob?.cancel()
        val current = _activeOrder.value ?: return
        val updated = current.copy(
            status = OrderStatus.ACCEPTED_ON_THE_WAY,
            acceptedAtMillis = System.currentTimeMillis()
        )
        _activeOrder.value = updated
        prefs?.edit()?.putString("ACTIVE_ORDER_ID", current.id)?.apply()
        therapistRepository.setDutyStatus(DutyStatus.ON_DUTY_BUSY)
        chatRepository.initializeChatForOrder(current.client.name, current.id)

        coroutineScope.launch(Dispatchers.IO) {
            val profile = therapistRepository.therapistProfile.value
            SupabaseClient.instance.acceptOrder(
                orderId = current.id,
                therapistId = profile.id,
                therapistLat = profile.latitude,
                therapistLng = profile.longitude
            )
        }

        startActiveOrderStatusMonitor(current.id)
    }

    private fun startActiveOrderStatusMonitor(orderId: String) {
        orderStatusMonitorJob?.cancel()
        orderStatusMonitorJob = coroutineScope.launch(Dispatchers.IO) {
            while (_activeOrder.value != null && _activeOrder.value?.status == OrderStatus.ACCEPTED_ON_THE_WAY) {
                delay(2000)
                try {
                    val orderData = SupabaseClient.instance.fetchOrder(orderId)
                    val statusStr = orderData?.get("status") as? String
                    if (statusStr == "CANCELLED" || statusStr == "DECLINED") {
                        withContext(Dispatchers.Main) {
                            val clientName = _activeOrder.value?.client?.name ?: "Pelanggan"
                            _activeOrder.value = null
                            prefs?.edit()?.remove("ACTIVE_ORDER_ID")?.apply()
                            therapistRepository.setDutyStatus(DutyStatus.ONLINE)
                            try {
                                com.massago.mitra.util.NotificationSoundHelper.notifyOrderCancelled(
                                    com.massago.mitra.MassaGoApp.instance,
                                    clientName
                                )
                            } catch (_: Exception) {}
                        }
                        break
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun declineOrder(reason: String = "Ditolak oleh mitra") {
        incomingTimeoutJob?.cancel()
        orderStatusMonitorJob?.cancel()
        val current = _activeOrder.value
        if (current != null) {
            dismissedOrderIds.add(current.id)
            prefs?.edit()?.remove("ACTIVE_ORDER_ID")?.apply()
            coroutineScope.launch(Dispatchers.IO) {
                SupabaseClient.instance.declineOrder(current.id)
            }
        }
        _activeOrder.value = null
        _incomingCountdownSeconds.value = 30
    }

    fun arriveAtLocation() {
        orderStatusMonitorJob?.cancel()
        val current = _activeOrder.value
        _activeOrder.update { it?.copy(status = OrderStatus.ARRIVED_AT_LOCATION) }
        if (current != null) {
            coroutineScope.launch(Dispatchers.IO) {
                SupabaseClient.instance.updateOrderStatus(current.id, "ARRIVED")
            }
        }
    }

    fun startSanitationChecklist() {
        _activeOrder.update { it?.copy(status = OrderStatus.SANITATION_AND_PREP) }
    }

    fun updateChecklistItem(itemType: ChecklistItemType, checked: Boolean) {
        _activeOrder.update { current ->
            current?.let {
                when (itemType) {
                    ChecklistItemType.HANDS_SANITIZED -> it.copy(isHandsSanitized = checked)
                    ChecklistItemType.MAT_COVER_REPLACED -> it.copy(isMatCoverReplaced = checked)
                    ChecklistItemType.OIL_AROMA_CONFIRMED -> it.copy(isOilAromaConfirmed = checked)
                    ChecklistItemType.PRESSURE_CHECKED -> it.copy(isPressurePreferenceChecked = checked)
                }
            }
        }
    }

    fun startTreatment() {
        val current = _activeOrder.value
        _activeOrder.update {
            it?.copy(
                status = OrderStatus.TREATMENT_IN_PROGRESS,
                startedAtMillis = System.currentTimeMillis(),
                isTimerRunning = true
            )
        }
        startTreatmentTimer()

        if (current != null) {
            coroutineScope.launch(Dispatchers.IO) {
                SupabaseClient.instance.updateOrderStatus(current.id, "IN_SERVICE")
            }
        }
    }

    fun toggleTimer() {
        val current = _activeOrder.value ?: return
        val newRunning = !current.isTimerRunning
        _activeOrder.update { it?.copy(isTimerRunning = newRunning) }
        if (newRunning) {
            startTreatmentTimer()
        } else {
            timerJob?.cancel()
        }
    }

    fun extendTreatmentDuration(extraMinutes: Int) {
        val extraSeconds = extraMinutes * 60
        _activeOrder.update { current ->
            current?.let {
                it.copy(
                    totalTreatmentSeconds = it.totalTreatmentSeconds + extraSeconds,
                    remainingTreatmentSeconds = it.remainingTreatmentSeconds + extraSeconds
                )
            }
        }
    }

    fun setAmbientSound(soundName: String) {
        _activeOrder.update { it?.copy(selectedAmbientSound = soundName) }
    }

    private fun startTreatmentTimer() {
        timerJob?.cancel()
        timerJob = coroutineScope.launch {
            while (_activeOrder.value?.status == OrderStatus.TREATMENT_IN_PROGRESS && _activeOrder.value?.isTimerRunning == true) {
                delay(1000)
                _activeOrder.update { current ->
                    current?.let {
                        val newRemaining = (it.remainingTreatmentSeconds - 1).coerceAtLeast(0)
                        it.copy(remainingTreatmentSeconds = newRemaining)
                    }
                }
            }
        }
    }

    fun finishTreatment() {
        timerJob?.cancel()
        val current = _activeOrder.value
        _activeOrder.update {
            it?.copy(
                status = OrderStatus.COMPLETED_PAYMENT,
                isTimerRunning = false,
                completedAtMillis = System.currentTimeMillis()
            )
        }

        if (current != null) {
            coroutineScope.launch(Dispatchers.IO) {
                SupabaseClient.instance.updateOrderStatus(current.id, "COMPLETED")
            }
        }
    }

    fun confirmPaymentAndSettle(tip: Long = 0L) {
        val current = _activeOrder.value ?: return
        val completedOrder = current.copy(
            tipAmount = tip,
            status = OrderStatus.REVIEW_SUBMITTED
        )

        // Record earnings in wallet
        walletRepository.recordOrderPayout(
            orderId = completedOrder.id,
            packageName = completedOrder.servicePackage.name,
            therapistNetEarning = completedOrder.therapistNetEarnings,
            tip = tip,
            platformFee = completedOrder.platformFee
        )

        therapistRepository.addEarnings(completedOrder.therapistNetEarnings + tip)

        // Add to history
        _orderHistory.update { history ->
            val updated = listOf(completedOrder) + history.filterNot { it.id == completedOrder.id }
            persistOrderHistory(updated)
            updated
        }
        _activeOrder.value = completedOrder
    }

    fun submitCustomerRating(rating: Int, tags: List<String> = emptyList(), comment: String = "") {
        val current = _activeOrder.value ?: return
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val therapistId = therapistRepository.therapistProfile.value.id
                SupabaseClient.instance.submitReview(
                    orderId = current.id,
                    reviewerType = "THERAPIST",
                    reviewerId = therapistId,
                    targetId = current.client.phone,
                    rating = rating,
                    tags = tags,
                    reviewText = comment
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun triggerSosAlert(notes: String = "Panggilan Darurat Mitra Terapis"): Boolean {
        val current = _activeOrder.value
        val profile = therapistRepository.therapistProfile.value
        coroutineScope.launch(Dispatchers.IO) {
            SupabaseClient.instance.sendSosAlert(
                senderType = "THERAPIST",
                senderId = profile.id,
                senderName = profile.name,
                senderPhone = profile.phone,
                orderId = current?.id,
                latitude = profile.latitude,
                longitude = profile.longitude,
                emergencyType = "EMERGENCY_ASSISTANCE",
                notes = notes
            )
        }
        return true
    }

    fun finishOrderAndReturnHome() {
        prefs?.edit()?.remove("ACTIVE_ORDER_ID")?.apply()
        _activeOrder.value = null
        therapistRepository.setDutyStatus(DutyStatus.ONLINE)
    }

    private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    companion object {
        val instance: OrderRepository by lazy { OrderRepository() }
    }
}
