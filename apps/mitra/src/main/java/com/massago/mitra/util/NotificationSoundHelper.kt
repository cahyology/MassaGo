package com.massago.mitra.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.massago.mitra.MainActivity
import com.massago.mitra.R
import com.massago.mitra.data.model.Order
import java.text.NumberFormat
import java.util.Locale

object NotificationSoundHelper {

    private const val CHANNEL_ID = "massago_orders_channel_v5"
    private const val CHANNEL_NAME = "Notifikasi Pesanan Masuk"
    private const val NOTIFICATION_ID = 8821

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi pesanan home massage baru"
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                vibrationPattern = longArrayOf(0, 300, 150, 300)
                setSound(soundUri, audioAttributes)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun triggerIncomingOrderAlert(context: Context, order: Order) {
        createNotificationChannel(context)
        showHeadsUpNotification(context, order)
    }

    fun stopIncomingOrderAlert(context: Context) {
        // Dismiss notification if needed
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

        val title = "🔔 Order Pijat Masuk! ($formattedEarning)"
        val content = "${order.servicePackage.name} (${order.servicePackage.durationMinutes} mnt) • ${order.client.name} (${order.client.distanceKm} km)"
        val subText = "Tarif: Rp ${currencyFormat.format(order.subtotal)}"

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_stat)
            .setContentTitle(title)
            .setContentText(content)
            .setSubText(subText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$content\nAlamat: ${order.client.address}\nEstimasi Tempuh: ~${order.client.travelEstimateMinutes} menit"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOngoing(false)
            .setContentIntent(pendingIntent)
            .setSound(defaultSoundUri)
            .setVibrate(longArrayOf(0, 300, 150, 300))
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
}
