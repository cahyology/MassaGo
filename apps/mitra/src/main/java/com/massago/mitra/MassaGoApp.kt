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

        // Install Global Uncaught Exception Handler to prevent hard crashes from background coroutines/threads
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("MassaGoApp", "Uncaught exception on thread ${thread.name}: ${throwable.message}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        try {
            MapsInitializer.initialize(applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_MODERATE) {
            System.gc()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        System.gc()
    }
}
