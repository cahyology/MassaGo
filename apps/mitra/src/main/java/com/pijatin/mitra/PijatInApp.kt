package com.pijatin.mitra

import android.app.Application
import com.google.android.gms.maps.MapsInitializer

class PijatInApp : Application() {
    companion object {
        lateinit var instance: PijatInApp
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
