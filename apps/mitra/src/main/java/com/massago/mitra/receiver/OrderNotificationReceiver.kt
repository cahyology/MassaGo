package com.massago.mitra.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.massago.mitra.MainActivity
import com.massago.mitra.data.repository.OrderRepository
import com.massago.mitra.util.NotificationSoundHelper

class OrderNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationSoundHelper.stopIncomingOrderAlert(context)
        when (intent.action) {
            ACTION_ACCEPT_ORDER -> {
                OrderRepository.instance.acceptOrder()
                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(mainIntent)
            }
            ACTION_DECLINE_ORDER -> {
                OrderRepository.instance.declineOrder("Ditolak dari notifikasi")
            }
        }
    }

    companion object {
        const val ACTION_ACCEPT_ORDER = "com.massago.mitra.ACTION_ACCEPT_ORDER"
        const val ACTION_DECLINE_ORDER = "com.massago.mitra.ACTION_DECLINE_ORDER"
    }
}
