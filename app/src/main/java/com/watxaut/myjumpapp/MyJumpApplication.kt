package com.watxaut.myjumpapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyJumpApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize any global components here if needed
        // Example: Timber.plant(Timber.DebugTree()) for logging
    }
}