package com.pijatin.customer.data.repository

import com.google.gson.JsonObject
import com.pijatin.customer.data.model.CustomerMockPromos
import com.pijatin.customer.data.model.CustomerOrder
import com.pijatin.customer.data.model.CustomerOrderStatus
import com.pijatin.customer.data.model.CustomerPaymentMethod
import com.pijatin.customer.data.model.CustomerPredefinedServices
import com.pijatin.customer.data.model.DurationOption
import com.pijatin.customer.data.model.MassageService
import com.pijatin.customer.data.model.PressureLevel
import com.pijatin.customer.data.model.PromoVoucher
import com.pijatin.customer.data.model.TherapistItem
import com.pijatin.customer.data.network.SupabaseCustomerClient
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
import java.util.UUID

class CustomerOrderRepository private constructor(
    private val userRepository: CustomerUserRepository = CustomerUserRepository.instance
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var matchingJob: Job? = null
    private var timerJob: Job? = null

    private val _activeOrder = MutableStateFlow<CustomerOrder?>(null)
    val activeOrder: StateFlow<CustomerOrder?> = _activeOrder.asStateFlow()

    private val _orderHistory = MutableStateFlow<List<CustomerOrder>>(emptyList())
    val orderHistory: StateFlow<List<CustomerOrder>> = _orderHistory.asStateFlow()

    private val _availableVouchers = MutableStateFlow<List<PromoVoucher>>(CustomerMockPromos.VOUCHERS)
    val availableVouchers: StateFlow<List<PromoVoucher>> = _availableVouchers.asStateFlow()

    private val _serviceCatalog = MutableStateFlow<List<MassageService>>(CustomerPredefinedServices.SERVICES)
    val serviceCatalog: StateFlow<List<MassageService>> = _serviceCatalog.asStateFlow()

    private val prefs by lazy {
        try {
            com.pijatin.customer.CustomerApp.instance.getSharedPreferences("pijatin_customer_order_prefs", android.content.Context.MODE_PRIVATE)
        } catch (_: Exception) {
            null
        }
    }

    init {
        restoreActiveOrderIfAny()
        refreshCatalogAndPromos()
    }

    fun refreshCatalogAndPromos() {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val dbServices = SupabaseCustomerClient.instance.fetchServicePackages()
                if (dbServices.isNotEmpty()) {
                    val mapped = dbServices.map { item ->
                        val id = item["id"] as? String ?: "SRV-${System.currentTimeMillis()}"
                        val name = item["name"] as? String ?: "Paket Pijat"
                        val category = item["category"] as? String ?: "Tradisional"
                        val shortDesc = item["short_description"] as? String ?: ""
                        val fullDesc = item["full_description"] as? String ?: shortDesc
                        val icon = item["icon_emoji"] as? String ?: "💆‍♂️"
                        val p60 = (item["price_60"] as? Number)?.toLong() ?: 0L
                        val p90 = (item["price_90"] as? Number)?.toLong() ?: 0L
                        val p120 = (item["price_120"] as? Number)?.toLong() ?: 0L

                        val durationList = mutableListOf<DurationOption>()
                        if (p60 > 0) durationList.add(DurationOption(60, p60, isPopular = false))
                        if (p90 > 0) durationList.add(DurationOption(90, p90, isPopular = true))
                        if (p120 > 0) durationList.add(DurationOption(120, p120, isPopular = false))

                        val benefits = (item["benefits"] as? List<*>)?.mapNotNull { it?.toString() }
                            ?: listOf("Melancarkan peredaran darah", "Meredakan otot kaku")

                        MassageService(
                            id = id,
                            name = name,
                            category = category,
                            rating = 4.95,
                            reviewCount = 380,
                            shortDescription = shortDesc,
                            fullDescription = fullDesc,
                            benefits = benefits,
                            durations = if (durationList.isNotEmpty()) durationList else listOf(DurationOption(90, 150000L, true)),
                            tag = if (category == "Tradisional") "Terpopuler" else "Pilihan",
                            iconEmoji = icon
                        )
                    }
                    withContext(Dispatchers.Main) {
                        _serviceCatalog.value = mapped
                    }
                }

                val dbVouchers = SupabaseCustomerClient.instance.fetchPromoVouchers()
                if (dbVouchers.isNotEmpty()) {
                    val mappedVouchers = dbVouchers.map { item ->
                        val code = item["code"] as? String ?: "PROMO"
                        val title = item["title"] as? String ?: "Diskon Promo"
                        val desc = item["description"] as? String ?: ""
                        val percent = (item["discount_percent"] as? Number)?.toInt() ?: 0
                        val flat = (item["discount_flat"] as? Number)?.toLong() ?: 0L
                        val maxDisc = (item["max_discount"] as? Number)?.toLong() ?: 50000L
                        val minSpend = (item["min_spend"] as? Number)?.toLong() ?: 100000L

                        PromoVoucher(
                            code = code,
                            title = title,
                            description = desc,
                            discountPercent = percent,
                            discountFlat = flat,
                            maxDiscount = maxDisc,
                            minSpend = minSpend,
                            expiryText = "Berlaku s/d Akhir Bulan"
                        )
                    }
                    withContext(Dispatchers.Main) {
                        _availableVouchers.value = mappedVouchers
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
                val orderData = SupabaseCustomerClient.instance.fetchOrder(savedOrderId) ?: return@launch
                val statusStr = orderData["status"] as? String ?: return@launch

                if (statusStr != "CANCELLED" && statusStr != "DECLINED" && statusStr != "ORDER_RATED") {
                    val srvName = orderData["service_name"] as? String ?: "Pijat Tradisional"
                    val duration = (orderData["duration_minutes"] as? Number)?.toInt() ?: 90
                    val totalPrice = (orderData["total_price"] as? Number)?.toLong() ?: 150000L
                    val matchedService = CustomerPredefinedServices.SERVICES.find {
                        it.name.contains(srvName, ignoreCase = true)
                    } ?: CustomerPredefinedServices.SERVICES[0]

                    val mappedStatus = when {
                        statusStr.startsWith("COMPLETE") -> CustomerOrderStatus.TREATMENT_FINISHED_PAYMENT
                        statusStr == "ARRIVED" -> CustomerOrderStatus.THERAPIST_ARRIVED
                        statusStr == "TREATMENT_IN_PROGRESS" || statusStr == "IN_SERVICE" -> CustomerOrderStatus.TREATMENT_IN_PROGRESS
                        statusStr.startsWith("ACCEPT") -> CustomerOrderStatus.THERAPIST_ON_THE_WAY
                        else -> CustomerOrderStatus.SEARCHING_THERAPIST
                    }

                    val therapistId = orderData["therapist_id"] as? String
                    var restoredTherapist: TherapistItem? = null
                    if (!therapistId.isNullOrBlank() && mappedStatus != CustomerOrderStatus.SEARCHING_THERAPIST) {
                        val dbTherapist = SupabaseCustomerClient.instance.fetchTherapist(therapistId)
                        val tName = dbTherapist?.get("name") as? String ?: "Budi Santoso, S.Tr.Kes"
                        val tGender = dbTherapist?.get("gender") as? String ?: "Pria"
                        val tRating = (dbTherapist?.get("rating") as? Number)?.toDouble() ?: 4.95
                        val custLat = userRepository.currentLocation.value.latitude
                        val custLng = userRepository.currentLocation.value.longitude
                        val rawTLat = (dbTherapist?.get("latitude") as? Number)?.toDouble()
                        val rawTLng = (dbTherapist?.get("longitude") as? Number)?.toDouble()

                        var tLat = rawTLat ?: (custLat - 0.006)
                        var tLng = rawTLng ?: (custLng - 0.005)

                        var dLat = Math.toRadians(custLat - tLat)
                        var dLng = Math.toRadians(custLng - tLng)
                        var a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                                Math.cos(Math.toRadians(tLat)) * Math.cos(Math.toRadians(custLat)) *
                                Math.sin(dLng / 2) * Math.sin(dLng / 2)
                        var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
                        var distKm = (6371.0 * c * 10).toInt() / 10.0

                        if (distKm > 40.0) {
                            tLat = custLat - 0.008
                            tLng = custLng - 0.006
                            distKm = 1.2
                        }

                        val etaMin = (distKm * 2.5).toInt().coerceIn(2, 30)
                        val initials = tName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").ifEmpty { "BS" }

                        restoredTherapist = TherapistItem(
                            id = therapistId,
                            name = tName,
                            gender = tGender,
                            rating = tRating,
                            reviewCount = 142,
                            ordersCompleted = 320,
                            distanceKm = distKm,
                            etaMinutes = etaMin,
                            certifications = listOf("Sertifikasi BNSP", "Pijat Tradisional"),
                            avatarInitials = initials,
                            isAvailableNow = true,
                            specialtyBadge = "Master Therapist",
                            latitude = tLat,
                            longitude = tLng
                        )
                    }

                    val restoredOrder = CustomerOrder(
                        id = savedOrderId,
                        service = matchedService,
                        durationMinutes = duration,
                        selectedAroma = CustomerPredefinedServices.AVAILABLE_AROMAS[0],
                        focusAreas = listOf("Seluruh Tubuh"),
                        pressureLevel = PressureLevel.MEDIUM,
                        therapistGenderPreference = (orderData["gender_preference"] as? String) ?: "Bebas",
                        location = userRepository.currentLocation.value,
                        status = mappedStatus,
                        assignedTherapist = restoredTherapist,
                        basePrice = totalPrice,
                        discountAmount = 0L,
                        appliedVoucher = null,
                        paymentMethod = CustomerPaymentMethod.PIJATIN_PAY,
                        totalSeconds = duration * 60,
                        remainingSeconds = duration * 60,
                        isTimerRunning = (mappedStatus == CustomerOrderStatus.TREATMENT_IN_PROGRESS)
                    )

                    withContext(Dispatchers.Main) {
                        _activeOrder.value = restoredOrder
                        if (mappedStatus == CustomerOrderStatus.TREATMENT_IN_PROGRESS) {
                            startTreatmentTimer()
                        }
                    }

                    startLiveOrderTracking(restoredOrder)
                } else {
                    prefs?.edit()?.remove("ACTIVE_ORDER_ID")?.apply()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun createAndPlaceOrder(
        service: MassageService,
        durationMinutes: Int,
        aromaId: String,
        focusAreas: List<String>,
        pressureLevel: PressureLevel,
        genderPreference: String,
        voucherCode: String?,
        paymentMethod: CustomerPaymentMethod
    ): CustomerOrder {
        val selectedAroma = CustomerPredefinedServices.AVAILABLE_AROMAS.find { it.id == aromaId }
            ?: CustomerPredefinedServices.AVAILABLE_AROMAS[0]

        val durationOption = service.durations.find { it.minutes == durationMinutes }
            ?: service.durations.first()

        val basePrice = durationOption.price
        val subtotal = basePrice + selectedAroma.extraFee

        val voucher = _availableVouchers.value.find { it.code.equals(voucherCode, ignoreCase = true) }
        val discount = voucher?.calculateDiscount(subtotal) ?: 0L

        val totalSeconds = durationMinutes * 60

        val newOrder = CustomerOrder(
            id = "ORD-PJ-" + UUID.randomUUID().toString().substring(0, 6).uppercase(),
            service = service,
            durationMinutes = durationMinutes,
            selectedAroma = selectedAroma,
            focusAreas = focusAreas,
            pressureLevel = pressureLevel,
            therapistGenderPreference = genderPreference,
            location = userRepository.currentLocation.value,
            status = CustomerOrderStatus.SEARCHING_THERAPIST,
            basePrice = basePrice,
            discountAmount = discount,
            appliedVoucher = voucher,
            paymentMethod = paymentMethod,
            totalSeconds = totalSeconds,
            remainingSeconds = totalSeconds
        )

        // Deduct from wallet if PijatIn Pay is selected
        if (paymentMethod == CustomerPaymentMethod.PIJATIN_PAY) {
            userRepository.deductWallet(newOrder.grandTotal)
        }

        _activeOrder.value = newOrder
        prefs?.edit()?.putString("ACTIVE_ORDER_ID", newOrder.id)?.apply()

        // Broadcast real order to Supabase Cloud orders table
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val loc = userRepository.currentLocation.value
                val formattedAddress = buildString {
                    if (loc.title.isNotBlank() && !loc.address.contains(loc.title)) {
                        append(loc.title).append(" - ")
                    }
                    append(loc.address)
                    append(" [GPS:").append(loc.latitude).append(",").append(loc.longitude).append("]")
                    if (loc.notes.isNotBlank()) {
                        append(" [NOTE:").append(loc.notes).append("]")
                    }
                }

                val orderJson = JsonObject().apply {
                    addProperty("id", newOrder.id)
                    addProperty("service_name", service.name)
                    addProperty("duration_minutes", durationMinutes)
                    addProperty("total_price", newOrder.grandTotal)
                    addProperty("status", "PENDING")
                    addProperty("customer_name", userRepository.profile.value.name)
                    addProperty("customer_phone", userRepository.profile.value.phone)
                    addProperty("address", formattedAddress)
                    addProperty("gender_preference", genderPreference)
                    addProperty("created_at", System.currentTimeMillis())
                }
                SupabaseCustomerClient.instance.createOrder(orderJson)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Start live tracking loop waiting for real Mitra to accept order
        startLiveOrderTracking(newOrder)
        return newOrder
    }

    private fun startLiveOrderTracking(order: CustomerOrder) {
        matchingJob?.cancel()
        matchingJob = coroutineScope.launch(Dispatchers.IO) {
            while (_activeOrder.value != null && _activeOrder.value?.status != CustomerOrderStatus.ORDER_RATED) {
                try {
                    val orderData = SupabaseCustomerClient.instance.fetchOrder(order.id)
                    if (orderData != null) {
                        val statusStr = (orderData["status"] as? String ?: "PENDING").uppercase()
                        val therapistId = orderData["therapist_id"] as? String

                        if (statusStr.startsWith("ACCEPT") || statusStr == "ARRIVED" || statusStr == "IN_SERVICE" || statusStr == "TREATMENT_IN_PROGRESS" || statusStr.startsWith("COMPLETE")) {
                            val dbTherapist = if (!therapistId.isNullOrBlank()) {
                                SupabaseCustomerClient.instance.fetchTherapist(therapistId)
                            } else null

                            withContext(Dispatchers.Main) {
                                val tName = dbTherapist?.get("name") as? String ?: "Budi Santoso, S.Tr.Kes"
                                val tGender = dbTherapist?.get("gender") as? String ?: "Pria"
                                val tRating = (dbTherapist?.get("rating") as? Number)?.toDouble() ?: 4.95
                                val custLat = _activeOrder.value?.location?.latitude ?: order.location.latitude
                                val custLng = _activeOrder.value?.location?.longitude ?: order.location.longitude

                                val rawTLat = (dbTherapist?.get("latitude") as? Number)?.toDouble()
                                val rawTLng = (dbTherapist?.get("longitude") as? Number)?.toDouble()

                                var finalTLat = rawTLat ?: (custLat - 0.006)
                                var finalTLng = rawTLng ?: (custLng - 0.005)

                                var dLat = Math.toRadians(custLat - finalTLat)
                                var dLng = Math.toRadians(custLng - finalTLng)
                                var a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                                        Math.cos(Math.toRadians(finalTLat)) * Math.cos(Math.toRadians(custLat)) *
                                        Math.sin(dLng / 2) * Math.sin(dLng / 2)
                                var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
                                var distKm = (6371.0 * c * 10).toInt() / 10.0

                                if (distKm > 40.0) {
                                    finalTLat = custLat - 0.008
                                    finalTLng = custLng - 0.006
                                    distKm = 1.2
                                }

                                val etaMin = (distKm * 2.5).toInt().coerceIn(2, 30)
                                val initials = tName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").ifEmpty { "BS" }

                                val assigned = TherapistItem(
                                    id = therapistId ?: "TRP-8821",
                                    name = tName,
                                    gender = tGender,
                                    rating = tRating,
                                    reviewCount = 142,
                                    ordersCompleted = 320,
                                    distanceKm = distKm,
                                    etaMinutes = etaMin,
                                    certifications = listOf("Sertifikasi BNSP", "Pijat Tradisional"),
                                    avatarInitials = initials,
                                    isAvailableNow = true,
                                    specialtyBadge = "Master Therapist",
                                    latitude = finalTLat,
                                    longitude = finalTLng
                                )

                                val previousStatus = _activeOrder.value?.status

                                val newStatus = when {
                                    statusStr.startsWith("COMPLETE") -> {
                                        timerJob?.cancel()
                                        CustomerOrderStatus.TREATMENT_FINISHED_PAYMENT
                                    }
                                    statusStr == "ARRIVED" -> CustomerOrderStatus.THERAPIST_ARRIVED
                                    statusStr == "TREATMENT_IN_PROGRESS" || statusStr == "IN_SERVICE" -> {
                                        if (timerJob == null || timerJob?.isActive == false) {
                                            startTreatmentTimer()
                                        }
                                        CustomerOrderStatus.TREATMENT_IN_PROGRESS
                                    }
                                    else -> CustomerOrderStatus.THERAPIST_ON_THE_WAY
                                }

                                // Trigger System Tray Notification when status changes
                                try {
                                    if (previousStatus == CustomerOrderStatus.SEARCHING_THERAPIST && newStatus == CustomerOrderStatus.THERAPIST_ON_THE_WAY) {
                                        com.pijatin.customer.util.CustomerNotificationHelper.notifyTherapistOnTheWay(
                                            com.pijatin.customer.CustomerApp.instance,
                                            tName,
                                            etaMin
                                        )
                                    } else if (previousStatus == CustomerOrderStatus.THERAPIST_ON_THE_WAY && newStatus == CustomerOrderStatus.THERAPIST_ARRIVED) {
                                        com.pijatin.customer.util.CustomerNotificationHelper.notifyTherapistArrived(
                                            com.pijatin.customer.CustomerApp.instance,
                                            tName
                                        )
                                    }
                                } catch (_: Exception) {}

                                _activeOrder.update { current ->
                                    (current ?: order).copy(
                                        status = newStatus,
                                        assignedTherapist = assigned,
                                        isTimerRunning = (newStatus == CustomerOrderStatus.TREATMENT_IN_PROGRESS)
                                    )
                                }
                            }
                            if (statusStr.startsWith("COMPLETE")) {
                                break
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                delay(1000)
            }
        }
    }

    fun startTreatmentSession() {
        _activeOrder.update { current ->
            current?.copy(
                status = CustomerOrderStatus.TREATMENT_IN_PROGRESS,
                isTimerRunning = true
            )
        }
        startTreatmentTimer()
    }

    fun extendTreatmentDuration(extraMinutes: Int) {
        val extraSeconds = extraMinutes * 60
        _activeOrder.update { current ->
            current?.let {
                it.copy(
                    totalSeconds = it.totalSeconds + extraSeconds,
                    remainingSeconds = it.remainingSeconds + extraSeconds
                )
            }
        }
    }

    fun selectAmbientSound(soundName: String) {
        _activeOrder.update { current ->
            current?.copy(selectedAmbientSound = soundName)
        }
    }

    private fun startTreatmentTimer() {
        timerJob?.cancel()
        timerJob = coroutineScope.launch {
            while (_activeOrder.value?.status == CustomerOrderStatus.TREATMENT_IN_PROGRESS && _activeOrder.value?.isTimerRunning == true) {
                delay(1000)
                _activeOrder.update { current ->
                    current?.let {
                        val newRemaining = (it.remainingSeconds - 1).coerceAtLeast(0)
                        if (newRemaining == 0) {
                            it.copy(remainingSeconds = 0, isTimerRunning = false, status = CustomerOrderStatus.TREATMENT_FINISHED_PAYMENT)
                        } else {
                            it.copy(remainingSeconds = newRemaining)
                        }
                    }
                }
            }
        }
    }

    fun toggleTreatmentTimer() {
        val current = _activeOrder.value ?: return
        val newRunning = !current.isTimerRunning
        _activeOrder.update { it?.copy(isTimerRunning = newRunning) }
        if (newRunning) {
            startTreatmentTimer()
        } else {
            timerJob?.cancel()
        }
    }

    fun submitRatingAndComplete(rating: Int, tags: List<String> = emptyList(), review: String = "", tipAmount: Long = 0L) {
        val current = _activeOrder.value ?: return
        val completed = current.copy(
            ratingGiven = rating,
            reviewComment = review,
            tipAmount = tipAmount,
            status = CustomerOrderStatus.ORDER_RATED
        )

        prefs?.edit()?.remove("ACTIVE_ORDER_ID")?.apply()
        _orderHistory.update { history -> listOf(completed) + history }
        _activeOrder.value = null
        matchingJob?.cancel()
        timerJob?.cancel()

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val therapistId = current.assignedTherapist?.id ?: "TRP-8821"
                val customerPhone = userRepository.profile.value.phone
                SupabaseCustomerClient.instance.submitReview(
                    orderId = current.id,
                    reviewerType = "CUSTOMER",
                    reviewerId = customerPhone,
                    targetId = therapistId,
                    rating = rating,
                    tags = tags,
                    reviewText = review
                )
                SupabaseCustomerClient.instance.updateOrderStatus(current.id, "COMPLETED")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun cancelActiveOrder(reason: String = "Dibatalkan oleh pelanggan") {
        matchingJob?.cancel()
        timerJob?.cancel()
        val current = _activeOrder.value ?: return
        prefs?.edit()?.remove("ACTIVE_ORDER_ID")?.apply()

        coroutineScope.launch(Dispatchers.IO) {
            try {
                SupabaseCustomerClient.instance.updateOrderStatus(current.id, "CANCELLED")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (current.paymentMethod == CustomerPaymentMethod.PIJATIN_PAY) {
            userRepository.topUpWallet(current.grandTotal)
        }

        _activeOrder.value = null
    }

    companion object {
        val instance: CustomerOrderRepository by lazy { CustomerOrderRepository() }
    }
}
