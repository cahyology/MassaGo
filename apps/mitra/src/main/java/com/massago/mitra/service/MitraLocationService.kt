package com.massago.mitra.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.massago.mitra.MainActivity
import com.massago.mitra.R
import com.massago.mitra.data.network.SupabaseClient
import com.massago.mitra.data.repository.TherapistRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MitraLocationService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private val supabaseClient = SupabaseClient.instance
    private val therapistRepository = TherapistRepository.instance

    companion object {
        const val ACTION_START = "ACTION_START_LOCATION_SERVICE"
        const val ACTION_STOP = "ACTION_STOP_LOCATION_SERVICE"
        private const val NOTIFICATION_ID = 9921
        private const val CHANNEL_ID = "massago_mitra_location_channel"

        fun start(context: Context) {
            val intent = Intent(context, MitraLocationService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, MitraLocationService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location: Location? = result.lastLocation
                if (location != null) {
                    val lat = location.latitude
                    val lng = location.longitude
                    
                    val currentProfile = therapistRepository.therapistProfile.value
                    if (currentProfile.dutyStatus == com.massago.mitra.data.model.DutyStatus.OFFLINE && !therapistRepository.isPersistedOnline()) {
                        stopSelf()
                        return
                    }

                    // Update local repository immediately so UI markers move instantly
                    therapistRepository.updateCurrentLocation(lat, lng)

                    serviceScope.launch {
                        val identifier = currentProfile.id.ifBlank { currentProfile.phone }
                        if (identifier.isNotBlank()) {
                            supabaseClient.updateLocationOnly(
                                therapistId = identifier,
                                latitude = lat,
                                longitude = lng
                            )
                        }

                        // If active order is ACCEPTED_ON_THE_WAY, stream therapist GPS directly to the order in Supabase
                        val activeOrder = com.massago.mitra.data.repository.OrderRepository.instance.activeOrder.value
                        if (activeOrder != null && activeOrder.status == com.massago.mitra.data.model.OrderStatus.ACCEPTED_ON_THE_WAY) {
                            supabaseClient.updateTherapistGpsForOrder(activeOrder.id, lat, lng)
                        }
                    }
                }
            }
        }
    }

    private var isExplicitStop = false
    private var wakeLock: android.os.PowerManager.WakeLock? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        when (action) {
            ACTION_START -> {
                isExplicitStop = false
                try {
                    val powerManager = getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
                    if (wakeLock == null) {
                        wakeLock = powerManager?.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "MassaGoMitra::LocationAndOrderWakeLock")?.apply {
                            setReferenceCounted(false)
                            acquire(12 * 60 * 60 * 1000L) // 12 hours max standby
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        buildForegroundNotification(),
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                    )
                } else {
                    startForeground(NOTIFICATION_ID, buildForegroundNotification())
                }
                requestLocationUpdates()
                com.massago.mitra.data.repository.OrderRepository.instance.startRealtimeOrderPolling()
            }
            ACTION_STOP -> {
                isExplicitStop = true
                try {
                    if (wakeLock?.isHeld == true) {
                        wakeLock?.release()
                    }
                    wakeLock = null
                } catch (_: Exception) {}

                com.massago.mitra.data.repository.OrderRepository.instance.stopRealtimeOrderPolling()
                stopLocationUpdates()
                val currentProfile = therapistRepository.therapistProfile.value
                val identifier = currentProfile.id.ifBlank { currentProfile.phone }
                CoroutineScope(Dispatchers.IO).launch {
                    supabaseClient.updateLocationAndDuty(
                        therapistId = identifier,
                        latitude = currentProfile.latitude,
                        longitude = currentProfile.longitude,
                        isOnline = false
                    )
                }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Keep foreground service alive and listening for orders even when app is swiped away from recent apps
        val currentProfile = therapistRepository.therapistProfile.value
        if (currentProfile.dutyStatus == com.massago.mitra.data.model.DutyStatus.ONLINE || currentProfile.dutyStatus == com.massago.mitra.data.model.DutyStatus.ON_DUTY_BUSY || therapistRepository.isPersistedOnline()) {
            val restartServiceIntent = Intent(applicationContext, MitraLocationService::class.java).apply {
                setPackage(packageName)
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(restartServiceIntent)
            } else {
                startService(restartServiceIntent)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        try {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                1500L // 1.5 seconds interval for smooth realtime GPS navigation streaming
            ).apply {
                setMinUpdateIntervalMillis(1000L)
                setMinUpdateDistanceMeters(0f) // continuous displacement streaming
            }.build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopLocationUpdates() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildForegroundNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🟢 MassaGo Mitra — Siap Terima Order")
            .setContentText("Radar GPS aktif • Menyiarkan lokasi untuk order pelanggan")
            .setSubText("Online On Duty")
            .setSmallIcon(R.drawable.ic_notification_stat)
            .setOngoing(true)
            .setAutoCancel(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Status Layanan Mitra MassaGo",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifikasi status online terapis dan radar pesanan di latar belakang"
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            wakeLock = null
        } catch (_: Exception) {}

        if (isExplicitStop) {
            com.massago.mitra.data.repository.OrderRepository.instance.stopRealtimeOrderPolling()
            stopLocationUpdates()
            val currentProfile = therapistRepository.therapistProfile.value
            CoroutineScope(Dispatchers.IO).launch {
                supabaseClient.updateLocationAndDuty(
                    therapistId = currentProfile.id,
                    latitude = currentProfile.latitude,
                    longitude = currentProfile.longitude,
                    isOnline = false
                )
            }
        }
        serviceScope.cancel()
    }
}
