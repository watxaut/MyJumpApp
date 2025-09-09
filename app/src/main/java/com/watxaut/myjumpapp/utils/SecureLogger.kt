package com.watxaut.myjumpapp.utils

import android.util.Log
import com.watxaut.myjumpapp.BuildConfig

object SecureLogger {
    
    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }
    
    fun i(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, message)
        }
    }
    
    fun v(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.v(tag, message)
        }
    }
    
    fun w(tag: String, message: String) {
        Log.w(tag, message)
    }
    
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }
    
    fun redactSensitiveData(data: String?): String {
        return when {
            data.isNullOrBlank() -> "[EMPTY]"
            data.length <= 1 -> "[REDACTED]"
            else -> "${data.take(1)}***"
        }
    }
    
    fun redactMeasurement(value: Number?): String {
        return if (value != null) "[MEASUREMENT]" else "[NULL]"
    }
    
    fun logUserAction(tag: String, action: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, "User action: $action")
        }
    }
}