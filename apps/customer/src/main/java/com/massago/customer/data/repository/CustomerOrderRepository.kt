package com.massago.customer.data.repository

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.massago.customer.data.model.CustomerLocation
import com.massago.customer.data.model.CustomerMockPromos
import com.massago.customer.data.model.CustomerOrder
import com.massago.customer.data.model.CustomerOrderStatus
import com.massago.customer.data.model.CustomerPaymentMethod
import com.massago.customer.data.model.CustomerPredefinedServices
import com.massago.customer.data.model.DurationOption
import com.massago.customer.data.model.MassageService
import com.massago.customer.data.model.PressureLevel
import com.massago.customer.data.model.PromoVoucher
import com.massago.customer.data.model.TherapistItem
import com.massago.customer.data.network.SupabaseCustomerClient
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

    private val gson = Gson()

    private val prefs by lazy {
        try {
            com.massago.customer.CustomerApp.instance.getSharedPreferences("massago_customer_order_prefs", android.content.Context.MODE_PRIVATE)
        } catch (_: Exception) {
            null
        }
    }

    private fun loadPersistedOrderHistory(): List<CustomerOrder> {
        val historyJson = prefs?.getString("CUSTOMER_ORDER_HISTORY_JSON", null) ?: return emptyList()
        return try {
            val listType = object : TypeToken<List<CustomerOrder>>() {}.type
            gson.fromJson(historyJson, listType) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persistOrderHistory(history: List<CustomerOrder>) {
        prefs?.edit()?.putString("CUSTOMER_ORDER_HISTORY_JSON", gson.toJson(history))?.apply()
    }

    private val _orderHistory = MutableStateFlow<List<CustomerOrder>>(emptyList())
    val orderHistory: StateFlow<List<CustomerOrder>> = _orderHistory.asStateFlow()

    private val _availableVouchers = MutableStateFlow<List<PromoVoucher>>(CustomerMockPromos.VOUCHERS)
    val availableVouchers: StateFlow<List<PromoVoucher>> = _availableVouchers.asStateFlow()

    private val _serviceCatalog = MutableStateFlow<List<MassageService>>(CustomerPredefinedServices.SERVICES)
    val serviceCatalog: StateFlow<List<MassageService>> = _serviceCatalog.asStateFlow()

    init {
        try {
            _orderHistory.value = loadPersistedOrderHistory()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        restoreActiveOrderIfAny()
        refreshCatalogAndPromos()
        fetchOrderHistoryFromSupabase()
    }

    fun fetchOrderHistoryFromSupabase() {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val profile = userRepository.profile.value
                val phone = profile.phone.ifBlank {
                    prefs?.getString("USER_PHONE", "") ?: ""
                }
                val customerId = profile.id.ifBlank {
                    prefs?.getString("USER_ID", "") ?: ""
                }
                val therapistsMap = SupabaseCustomerClient.instance.fetchTherapistsMap()
                val rows = SupabaseCustomerClient.instance.fetchCustomerOrders(phone, customerId)
                if (rows.isNotEmpty()) {
                    val mapped = rows.mapNotNull { row ->
                        try {
                            val id = row["id"] as? String ?: return@mapNotNull null
                            val statusStr = row["status"] as? String ?: "COMPLETED"
                            val srvName = row["service_name"] as? String ?: "Pijat Tradisional Jawa"
                            val duration = (row["duration_minutes"] as? Number)?.toInt() ?: 90
                            val totalPrice = (row["total_price"] as? Number)?.toLong() ?: 180000L
                            val rawAddress = row["address"] as? String ?: "Lokasi Pelanggan"
                            val therapistId = row["therapist_id"] as? String ?: ""
                            val therapistName = therapistsMap[therapistId]
                                ?: (row["therapist_name"] as? String)
                                ?: if (therapistId.isNotBlank()) "Mitra $therapistId" else "Terapis Pilihan"
                            val createdAt = (row["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis()

                            val orderStatus = when (statusStr) {
                                "COMPLETED", "FINISHED" -> CustomerOrderStatus.ORDER_RATED
                                "CANCELLED" -> CustomerOrderStatus.CANCELLED
                                "IN_SERVICE", "TREATMENT_IN_PROGRESS" -> CustomerOrderStatus.TREATMENT_IN_PROGRESS
                                "ARRIVED", "ARRIVED_AT_LOCATION" -> CustomerOrderStatus.THERAPIST_ARRIVED
                                "ACCEPTED", "ACCEPTED_ON_THE_WAY" -> CustomerOrderStatus.THERAPIST_ON_THE_WAY
                                else -> CustomerOrderStatus.SEARCHING_THERAPIST
                            }

                            val service = _serviceCatalog.value.find { it.name.contains(srvName, ignoreCase = true) }
                                ?: MassageService(
                                    id = "srv-$id",
                                    name = srvName,
                                    category = "Pijat",
                                    shortDescription = "Layanan pemijatan profesional MassaGo",
                                    fullDescription = "Layanan pemijatan profesional MassaGo",
                                    benefits = emptyList(),
                                    durations = listOf(DurationOption(duration, totalPrice)),
                                    iconEmoji = "💆"
                                )

                            val therapist = TherapistItem(
                                id = therapistId,
                                name = therapistName,
                                gender = "Terapis",
                                rating = 5.0,
                                reviewCount = 25,
                                ordersCompleted = 50,
                                distanceKm = 2.4,
                                etaMinutes = 10,
                                certifications = listOf("BNSP Certified", "Traditional Massage Master"),
                                avatarInitials = therapistName.take(2).uppercase()
                            )

                            CustomerOrder(
                                id = id,
                                service = service,
                                durationMinutes = duration,
                                location = CustomerLocation(title = "Lokasi Pesanan", address = rawAddress),
                                status = orderStatus,
                                assignedTherapist = therapist,
                                basePrice = totalPrice,
                                travelFee = 0L,
                                hygieneKitFee = 0L,
                                discountAmount = 0L,
                                tipAmount = 0L,
                                paymentMethod = CustomerPaymentMethod.PIJATIN_PAY,
                                createdAtMillis = createdAt
                            )
                        } catch (_: Exception) {
                            null
                        }
                    }

                    if (mapped.isNotEmpty()) {
                        val combined = (mapped + _orderHistory.value).distinctBy { it.id }.sortedByDescending { it.createdAtMillis }
                        _orderHistory.value = combined
                        persistOrderHistory(combined)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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
                        val tName = dbTherapist?.get("name") as? String ?: "Mitra Terapis MassaGo"
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
        recipientGender: String = "Wanita",
        voucherCode: String?,
        paymentMethod: CustomerPaymentMethod,
        preferredTherapistId: String? = null,
        isRepeatOrder: Boolean = false,
        scheduledTime: String? = null,
        extraTravelSurcharge: Long = 0L
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
            travelFee = extraTravelSurcharge,
            discountAmount = discount,
            appliedVoucher = voucher,
            paymentMethod = paymentMethod,
            preferredTherapistId = preferredTherapistId,
            isRepeatOrder = isRepeatOrder,
            scheduledTime = scheduledTime,
            totalSeconds = totalSeconds,
            remainingSeconds = totalSeconds
        )

        // Deduct from wallet if MassaGo Pay is selected
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
                    append(" [RECIPIENT_GENDER:").append(recipientGender).append("]")
                    append(" [PREF_GENDER:").append(genderPreference).append("]")
                    if (loc.notes.isNotBlank()) {
                        append(" [NOTE:").append(loc.notes).append("]")
                    }
                    if (!preferredTherapistId.isNullOrBlank()) {
                        append(" [PREFERRED_THERAPIST:").append(preferredTherapistId).append("]")
                        append(" [REPEAT_ORDER:true]")
                    }
                    if (!scheduledTime.isNullOrBlank()) {
                        append(" [SCHEDULED_TIME:").append(scheduledTime).append("]")
                    }
                }

                val currentProfile = userRepository.profile.value
                val resolvedCustomerId = currentProfile.id.ifBlank { "CUST-${currentProfile.phone.takeLast(6)}" }

                val orderJson = JsonObject().apply {
                    addProperty("id", newOrder.id)
                    addProperty("customer_id", resolvedCustomerId)
                    addProperty("service_name", service.name)
                    addProperty("duration_minutes", durationMinutes)
                    addProperty("total_price", newOrder.grandTotal)
                    addProperty("status", "PENDING")
                    addProperty("customer_name", currentProfile.name)
                    addProperty("customer_phone", currentProfile.phone)
                    addProperty("address", formattedAddress)
                    addProperty("gender_preference", genderPreference)
                    addProperty("created_at", System.currentTimeMillis())

                    if (!preferredTherapistId.isNullOrBlank()) {
                        addProperty("therapist_id", preferredTherapistId)
                    }
                }
                val created = SupabaseCustomerClient.instance.createOrder(orderJson)
                if (created == null) {
                    // Fallback without therapist_id if targeted therapist column has foreign key constraint
                    orderJson.remove("therapist_id")
                    SupabaseCustomerClient.instance.createOrder(orderJson)
                }
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
                                val tName = dbTherapist?.get("name") as? String ?: "Mitra Terapis MassaGo"
                                val tGender = dbTherapist?.get("gender") as? String ?: "Pria"
                                val tRating = (dbTherapist?.get("rating") as? Number)?.toDouble() ?: 4.95
                                val custLat = _activeOrder.value?.location?.latitude ?: order.location.latitude
                                val custLng = _activeOrder.value?.location?.longitude ?: order.location.longitude

                                val rawTLat = (dbTherapist?.get("latitude") as? Number)?.toDouble()
                                val rawTLng = (dbTherapist?.get("longitude") as? Number)?.toDouble()

                                val finalTLat = rawTLat ?: (custLat - 0.006)
                                val finalTLng = rawTLng ?: (custLng - 0.005)

                                val dLat = Math.toRadians(custLat - finalTLat)
                                val dLng = Math.toRadians(custLng - finalTLng)
                                val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                                        Math.cos(Math.toRadians(finalTLat)) * Math.cos(Math.toRadians(custLat)) *
                                        Math.sin(dLng / 2) * Math.sin(dLng / 2)
                                val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
                                val distKm = ((6371.0 * c * 10).toInt() / 10.0).coerceAtLeast(0.1)

                                val etaMin = (distKm * 2.5).toInt().coerceIn(1, 45)
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
                                        com.massago.customer.util.CustomerNotificationHelper.notifyTherapistOnTheWay(
                                            com.massago.customer.CustomerApp.instance,
                                            tName,
                                            etaMin
                                        )
                                    } else if (previousStatus == CustomerOrderStatus.THERAPIST_ON_THE_WAY && newStatus == CustomerOrderStatus.THERAPIST_ARRIVED) {
                                        com.massago.customer.util.CustomerNotificationHelper.notifyTherapistArrived(
                                            com.massago.customer.CustomerApp.instance,
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
                        } else if (statusStr.startsWith("CANCEL") || statusStr == "DECLINED") {
                            timerJob?.cancel()
                            withContext(Dispatchers.Main) {
                                _activeOrder.update { current ->
                                    current?.copy(
                                        status = CustomerOrderStatus.CANCELLED
                                    )
                                }
                            }
                            break
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
        _orderHistory.update { history ->
            val updated = listOf(completed) + history.filterNot { it.id == completed.id }
            persistOrderHistory(updated)
            updated
        }
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

    fun clearActiveOrder() {
        matchingJob?.cancel()
        timerJob?.cancel()
        prefs?.edit()?.remove("ACTIVE_ORDER_ID")?.apply()
        _activeOrder.value = null
    }

    fun triggerSosAlert(notes: String = "Panggilan Darurat Pelanggan MassaGo"): Boolean {
        val current = _activeOrder.value
        val profile = userRepository.profile.value
        val loc = userRepository.currentLocation.value
        coroutineScope.launch(Dispatchers.IO) {
            SupabaseCustomerClient.instance.sendSosAlert(
                senderType = "CUSTOMER",
                senderId = profile.id.ifBlank { profile.phone },
                senderName = profile.name,
                senderPhone = profile.phone,
                orderId = current?.id,
                latitude = loc.latitude,
                longitude = loc.longitude,
                emergencyType = "EMERGENCY_ASSISTANCE",
                notes = notes
            )
        }
        return true
    }

    companion object {
        val instance: CustomerOrderRepository by lazy { CustomerOrderRepository() }
    }
}
