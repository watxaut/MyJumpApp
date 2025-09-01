package com.watxaut.myjumpapp.domain.models

sealed class JumpResult {
    data class Success(
        val heightCm: Double,
        val confidence: Double,
        val timestamp: Long,
        val flightTimeMs: Long? = null,
        val videoPath: String? = null
    ) : JumpResult()
    
    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : JumpResult()
    
    object InProgress : JumpResult()
    object Idle : JumpResult()
}

data class JumpMeasurement(
    val heightCm: Double,
    val confidence: Double,
    val measurementMethods: List<String>,
    val timestamp: Long = System.currentTimeMillis(),
    val flightTimeMs: Long? = null,
    val takeoffFrame: Int? = null,
    val landingFrame: Int? = null,
    val peakFrame: Int? = null
)