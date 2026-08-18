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
                    if (currentProfile.dutyStatus == com.massago.mitra.data.model.DutyStatus.OFFLINE || !currentProfile.isVerified) {
                        stopSelf()
                        return
                    }

                    // Update local repository & Supabase Cloud
                    therapistRepository.updateCurrentLocation(lat, lng)

                    serviceScope.launch {
                        supabaseClient.updateLocationOnly(
                            therapistId = currentProfile.id,
                            latitude = lat,
                            longitude = lng
                        )

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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, buildForegroundNotification())
                requestLocationUpdates()
            }
            ACTION_STOP -> {
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
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        try {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                4000L // 4 seconds interval for realtime navigation streaming
            ).apply {
                setMinUpdateIntervalMillis(3000L)
                setMinUpdateDistanceMeters(4f) // 4 meters displacement
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
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MassaGo Mitra — Siap Terima Pesanan")
            .setContentText("GPS & Radar aktif. Menyiarkan koordinat untuk klien terdekat.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Layanan GPS & Radar Mitra",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifikasi status online terapis di latar belakang"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
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
        serviceScope.cancel()
    }
}
