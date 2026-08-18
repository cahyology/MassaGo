package com.massago.mitra

import android.app.Application
import com.google.android.gms.maps.MapsInitializer

class MassaGoApp : Application() {
    companion object {
        lateinit var instance: MassaGoApp
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
