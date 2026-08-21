package com.massago.mitra.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.massago.mitra.MainActivity
import com.massago.mitra.R
import com.massago.mitra.data.model.Order
import com.massago.mitra.receiver.OrderNotificationReceiver
import java.text.NumberFormat
import java.util.Locale

object NotificationSoundHelper {

    private const val CHANNEL_ID = "massago_incoming_orders_custom_sound_v7"
    private const val CHANNEL_NAME = "Alarm Pesanan Masuk MassaGo"
    private const val NOTIFICATION_ID = 8821

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var screenWakeLock: PowerManager.WakeLock? = null
    private val lock = Any()

    fun getCustomIncomingSoundUri(context: Context): Uri {
        return try {
            Uri.parse("android.resource://${context.packageName}/${R.raw.incoming}")
        } catch (_: Exception) {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = getCustomIncomingSoundUri(context)

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alarm dan notifikasi suara pesanan masuk MassaGo"
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                vibrationPattern = longArrayOf(0, 800, 400, 800, 400)
                setSound(soundUri, audioAttributes)
                setBypassDnd(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Start continuous looping custom incoming.mp3 alarm sound, repeating vibration, and wake the screen.
     */
    fun triggerIncomingOrderAlert(context: Context, order: Order) {
        synchronized(lock) {
            createNotificationChannel(context)

            // 1. Wake up the phone screen immediately so driver sees the order
            try {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                if (screenWakeLock == null) {
                    @Suppress("DEPRECATION")
                    screenWakeLock = powerManager?.newWakeLock(
                        PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                        "MassaGoMitra::IncomingOrderScreenWakeLock"
                    )
                }
                screenWakeLock?.acquire(45 * 1000L) // 45 seconds max lease
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. Start Continuous Looping Custom incoming.mp3 Alarm Sound
            try {
                if (mediaPlayer == null) {
                    try {
                        mediaPlayer = MediaPlayer.create(context, R.raw.incoming)?.apply {
                            setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_ALARM)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .setLegacyStreamType(AudioManager.STREAM_ALARM)
                                    .build()
                            )
                            isLooping = true
                            setVolume(1.0f, 1.0f)
                            start()
                        }
                    } catch (e1: Exception) {
                        val alertUri = getCustomIncomingSoundUri(context)
                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(context, alertUri)
                            setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_ALARM)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .setLegacyStreamType(AudioManager.STREAM_ALARM)
                                    .build()
                            )
                            isLooping = true
                            setVolume(1.0f, 1.0f)
                            prepare()
                            start()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 3. Start Continuous Repeating Vibration
            try {
                if (vibrator == null) {
                    vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                        vibratorManager?.defaultVibrator
                    } else {
                        @Suppress("DEPRECATION")
                        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    }
                }

                val pattern = longArrayOf(0, 800, 400, 800, 400, 800, 400)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0)) // 0 = loop from start
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, 0)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 4. Show Full-Screen Heads-Up Notification with Direct Action Buttons
            showHeadsUpNotification(context, order)
        }
    }

    /**
     * Stop looping sound, stop vibration, release screen lock, and cancel notification.
     */
    fun stopIncomingOrderAlert(context: Context) {
        synchronized(lock) {
            try {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.stop()
                }
                mediaPlayer?.reset()
                mediaPlayer?.release()
                mediaPlayer = null
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                vibrator?.cancel()
                vibrator = null
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                if (screenWakeLock?.isHeld == true) {
                    screenWakeLock?.release()
                }
                screenWakeLock = null
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(NOTIFICATION_ID)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showHeadsUpNotification(context: Context, order: Order) {
        val currencyFormat = NumberFormat.getNumberInstance(Locale("id", "ID"))
        val formattedEarning = "Rp " + currencyFormat.format(order.therapistNetEarnings)

        val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            101,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Accept Action Button Intent
        val acceptIntent = Intent(context, OrderNotificationReceiver::class.java).apply {
            action = OrderNotificationReceiver.ACTION_ACCEPT_ORDER
        }
        val acceptPendingIntent = PendingIntent.getBroadcast(
            context,
            102,
            acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Decline Action Button Intent
        val declineIntent = Intent(context, OrderNotificationReceiver::class.java).apply {
            action = OrderNotificationReceiver.ACTION_DECLINE_ORDER
        }
        val declinePendingIntent = PendingIntent.getBroadcast(
            context,
            103,
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "🔔 ORDER PIJAT MASUK! ($formattedEarning)"
        val content = "${order.servicePackage.name} (${order.servicePackage.durationMinutes} mnt) • ${order.client.name} (${order.client.distanceKm} km)"
        val subText = "Pendapatan Bersih: $formattedEarning"
        val soundUri = getCustomIncomingSoundUri(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_stat)
            .setContentTitle(title)
            .setContentText(content)
            .setSubText(subText)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "$content\n" +
                            "📍 Alamat: ${order.client.address}\n" +
                            "⏱️ Estimasi Tempuh: ~${order.client.travelEstimateMinutes} menit\n" +
                            "💰 Bersih: $formattedEarning"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setSound(soundUri)
            .setAutoCancel(false)
            .setOngoing(true)
            .addAction(R.drawable.ic_notification_stat, "✅ TERIMA ORDER", acceptPendingIntent)
            .addAction(R.drawable.ic_notification_stat, "❌ TOLAK", declinePendingIntent)
            .build()

        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun notifyOrderCancelled(context: Context, clientName: String) {
        createNotificationChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
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
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
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
}
