package com.watxaut.myjumpapp.utils

import android.app.Activity
import android.content.Context
import android.os.PowerManager
import android.view.WindowManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WakeLockManager @Inject constructor() {
    
    private var wakeLock: PowerManager.WakeLock? = null
    private var isWindowFlagSet = false
    private var sessionStartTime: Long = 0L
    private val maxSessionDuration = 15 * 60 * 1000L // 15 minutes
    
    fun acquireWakeLock(context: Context) {
        try {
            // Method 1: Use WindowManager flags (more reliable for keeping screen on)
            if (context is Activity) {
                context.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                isWindowFlagSet = true
                SecureLogger.i("WakeLockManager", "Window flag FLAG_KEEP_SCREEN_ON set")
            }
            
            // Method 2: PowerManager WakeLock as backup
            if (wakeLock?.isHeld == true) {
                SecureLogger.i("WakeLockManager", "Wake lock already acquired")
                return // Already acquired
            }
            
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, // Changed to PARTIAL_WAKE_LOCK for better compatibility
                "MyJumpApp:JumpDetectionWakeLock"
            )
            
            // Acquire with a timeout of 15 minutes as a safety measure
            wakeLock?.acquire(maxSessionDuration)
            sessionStartTime = System.currentTimeMillis()
            SecureLogger.i("WakeLockManager", "Wake lock acquired successfully, isHeld: ${wakeLock?.isHeld}")
        } catch (e: Exception) {
            SecureLogger.e("WakeLockManager", "Failed to acquire wake lock", e)
        }
    }
    
    fun releaseWakeLock() {
        releaseWakeLock(null)
    }
    
    fun releaseWakeLock(context: Context?) {
        try {
            // Method 1: Clear window flags if set
            if (isWindowFlagSet && context is Activity) {
                context.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                isWindowFlagSet = false
                SecureLogger.i("WakeLockManager", "Window flag FLAG_KEEP_SCREEN_ON cleared")
            }
            
            // Method 2: Release PowerManager WakeLock
            wakeLock?.let { lock ->
                if (lock.isHeld) {
                    lock.release()
                    SecureLogger.i("WakeLockManager", "Wake lock released")
                }
            }
            wakeLock = null
            sessionStartTime = 0L
        } catch (e: Exception) {
            SecureLogger.e("WakeLockManager", "Failed to release wake lock", e)
        }
    }
    
    fun isHeld(): Boolean {
        return wakeLock?.isHeld == true
    }
    
    fun checkSessionTimeout(): Boolean {
        if (sessionStartTime == 0L) return false
        val elapsed = System.currentTimeMillis() - sessionStartTime
        if (elapsed > maxSessionDuration) {
            SecureLogger.w("WakeLockManager", "Session timeout reached, releasing wake lock")
            releaseWakeLock()
            return true
        }
        return false
    }
    
    fun getSessionElapsed(): Long {
        return if (sessionStartTime > 0) System.currentTimeMillis() - sessionStartTime else 0L
    }
    
    fun getRemainingSessionTime(): Long {
        val elapsed = getSessionElapsed()
        return maxOf(0L, maxSessionDuration - elapsed)
    }
}