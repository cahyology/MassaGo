package com.massago.customer

import android.app.Application
import android.content.ComponentCallbacks2
import android.util.Log
import com.google.android.gms.maps.MapsInitializer

class CustomerApp : Application() {
    companion object {
        lateinit var instance: CustomerApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Install Global Uncaught Exception Handler to prevent hard crashes from transient background exceptions
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e("MassaGoCustomerApp", "Uncaught exception in thread ${thread.name}: ${throwable.message}", throwable)
            } catch (_: Exception) {}

            // Let default handler process severe non-recoverable VM errors
            if (throwable is OutOfMemoryError || throwable is StackOverflowError) {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }

        try {
            MapsInitializer.initialize(applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            try {
                System.gc()
            } catch (_: Exception) {}
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        try {
            System.gc()
        } catch (_: Exception) {}
    }
}
