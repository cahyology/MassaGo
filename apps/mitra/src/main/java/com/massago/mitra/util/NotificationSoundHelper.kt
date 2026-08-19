package com.massago.mitra.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.massago.mitra.MainActivity
import com.massago.mitra.R
import com.massago.mitra.data.model.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

object NotificationSoundHelper {

    private const val CHANNEL_ID = "massago_orders_channel_v4"
    private const val CHANNEL_NAME = "Order Pijat Masuk (Panggilan Masuk)"
    private const val NOTIFICATION_ID = 8821

    private var activeRingtone: Ringtone? = null
    private var toneGenerator: ToneGenerator? = null
    private var soundJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi suara dan getaran saat ada pesanan home massage baru"
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                vibrationPattern = longArrayOf(0, 600, 200, 600, 200, 800)
                setSound(soundUri, audioAttributes)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun triggerIncomingOrderAlert(context: Context, order: Order) {
        // Wake up screen if locked or turned off
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            val screenWakeLock = pm?.newWakeLock(
                android.os.PowerManager.FULL_WAKE_LOCK or
                android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP or
                android.os.PowerManager.ON_AFTER_RELEASE,
                "MassaGoMitra::IncomingOrderScreenWake"
            )
            screenWakeLock?.acquire(15000L) // Turn on screen for 15 seconds
        } catch (_: Exception) {}

        createNotificationChannel(context)
        showHeadsUpNotification(context, order)
        startAudioAndVibrationAlert(context)
    }

    fun stopIncomingOrderAlert(context: Context) {
        // Stop sound loop
        soundJob?.cancel()
        soundJob = null

        try {
            activeRingtone?.stop()
            activeRingtone = null
        } catch (_: Exception) {}

        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (_: Exception) {}

        // Stop vibration
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.cancel()
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.cancel()
            }
        } catch (_: Exception) {}

        // Dismiss notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun showHeadsUpNotification(context: Context, order: Order) {
        val currencyFormat = NumberFormat.getNumberInstance(Locale("id", "ID"))
        val formattedEarning = "Rp " + currencyFormat.format(order.therapistNetEarnings)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "🔔 ORDER PIJAT MASUK! ($formattedEarning)"
        val content = "${order.servicePackage.name} (${order.servicePackage.durationMinutes} mnt) • ${order.client.name} (${order.client.distanceKm} km)"
        val subText = "Tarif: Rp ${currencyFormat.format(order.subtotal)}"

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_stat)
            .setContentTitle(title)
            .setContentText(content)
            .setSubText(subText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$content\nAlamat: ${order.client.address}\nEstimasi Tempuh: ~${order.client.travelEstimateMinutes} menit"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setOngoing(false)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .setSound(defaultSoundUri)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 600))
            .build()

        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startAudioAndVibrationAlert(context: Context) {
        // Play system ringtone / chime
        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val ringtone = RingtoneManager.getRingtone(context, notificationUri)
            activeRingtone = ringtone
            ringtone?.play()
        } catch (_: Exception) {}

        // Start repeating harmonic chime loop (driver order alert sound simulation)
        soundJob?.cancel()
        soundJob = scope.launch {
            try {
                toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            } catch (_: Exception) {}

            while (isActive) {
                // Play rhythmic double-chime (Ding-Dong / Beep-Beep alert)
                try {
                    toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 250)
                } catch (_: Exception) {}

                // Trigger vibration pulse
                triggerVibrationPulse(context)

                delay(1200)

                if (!isActive) break

                try {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 250)
                } catch (_: Exception) {}

                delay(1500)
            }
        }
    }

    fun notifyOrderCancelled(context: Context, clientName: String) {
        createNotificationChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            201,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_stat)
            .setContentTitle("⚠️ Pesanan Dibatalkan")
            .setContentText("Pesanan dari $clientName telah dibatalkan.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Pelanggan $clientName telah membatalkan pesanan. Status Anda otomatis kembali Siap Menerima Order (Online)."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(8822, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun notifyNewChatMessage(context: Context, senderName: String, messageText: String) {
        createNotificationChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            202,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_stat)
            .setContentTitle("💬 Pesan dari $senderName")
            .setContentText(messageText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(8823, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun triggerVibrationPulse(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                val effect = VibrationEffect.createWaveform(
                    longArrayOf(0, 250, 150, 250),
                    intArrayOf(0, 200, 0, 255),
                    -1
                )
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(longArrayOf(0, 250, 150, 250), -1)
            }
        } catch (_: Exception) {}
    }
}
