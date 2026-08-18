package com.pijatin.customer

import android.app.Application
import com.google.android.gms.maps.MapsInitializer

class CustomerApp : Application() {
    companion object {
        lateinit var instance: CustomerApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        try {
            MapsInitializer.initialize(applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
