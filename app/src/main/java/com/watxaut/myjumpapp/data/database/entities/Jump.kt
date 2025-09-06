package com.watxaut.myjumpapp.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "jumps",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["user_id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = JumpSession::class,
            parentColumns = ["session_id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["session_id"]),
        Index(value = ["timestamp"])
    ]
)
data class Jump(
    @PrimaryKey
    @ColumnInfo(name = "jump_id")
    val jumpId: String = UUID.randomUUID().toString(),
    
    @ColumnInfo(name = "user_id")
    val userId: String,
    
    @ColumnInfo(name = "session_id")
    val sessionId: String? = null,
    
    @ColumnInfo(name = "height_cm")
    val heightCm: Double,
    
    @ColumnInfo(name = "spike_reach_cm")
    val spikeReachCm: Double = 0.0,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "video_path")
    val videoPath: String? = null,
    
    @ColumnInfo(name = "takeoff_time")
    val takeoffTime: Long? = null,
    
    @ColumnInfo(name = "landing_time")
    val landingTime: Long? = null,
    
    @ColumnInfo(name = "flight_time_ms")
    val flightTimeMs: Long? = null,
    
    @ColumnInfo(name = "peak_height_frame")
    val peakHeightFrame: Int? = null,
    
    @ColumnInfo(name = "calibration_reference")
    val calibrationReference: Double? = null,
    
    @ColumnInfo(name = "confidence_score")
    val confidenceScore: Double = 1.0,
    
    @ColumnInfo(name = "notes")
    val notes: String? = null,
    
    @ColumnInfo(name = "measurement_method")
    val measurementMethod: String = "camera_vision"
) {
    init {
        validateJumpData()
    }
    
    companion object {
        private const val MAX_JUMP_HEIGHT_CM = 500.0 // 5 meters - reasonable max for human jump
        private const val MIN_JUMP_HEIGHT_CM = 0.0
        private const val MAX_CONFIDENCE_SCORE = 1.0
        private const val MIN_CONFIDENCE_SCORE = 0.0
        private const val MAX_NOTES_LENGTH = 500
        private const val MAX_MEASUREMENT_METHOD_LENGTH = 50
        
        /**
         * Creates a validated Jump instance with input sanitization
         */
        fun createValidated(
            userId: String,
            heightCm: Double,
            sessionId: String? = null,
            timestamp: Long = System.currentTimeMillis(),
            videoPath: String? = null,
            takeoffTime: Long? = null,
            landingTime: Long? = null,
            flightTimeMs: Long? = null,
            peakHeightFrame: Int? = null,
            calibrationReference: Double? = null,
            confidenceScore: Double = 1.0,
            notes: String? = null,
            measurementMethod: String = "camera_vision"
        ): Jump {
            return Jump(
                userId = userId.trim(),
                heightCm = heightCm.coerceIn(MIN_JUMP_HEIGHT_CM, MAX_JUMP_HEIGHT_CM),
                sessionId = sessionId?.trim(),
                timestamp = if (timestamp > 0) timestamp else System.currentTimeMillis(),
                videoPath = videoPath?.trim(),
                takeoffTime = takeoffTime?.takeIf { it >= 0 },
                landingTime = landingTime?.takeIf { it >= 0 },
                flightTimeMs = flightTimeMs?.takeIf { it >= 0 },
                peakHeightFrame = peakHeightFrame?.takeIf { it >= 0 },
                calibrationReference = calibrationReference?.takeIf { it.isFinite() },
                confidenceScore = confidenceScore.coerceIn(MIN_CONFIDENCE_SCORE, MAX_CONFIDENCE_SCORE),
                notes = notes?.trim()?.take(MAX_NOTES_LENGTH),
                measurementMethod = measurementMethod.trim().take(MAX_MEASUREMENT_METHOD_LENGTH).ifBlank { "camera_vision" }
            )
        }
    }
    
    private fun validateJumpData() {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        require(heightCm.isFinite()) { "Height must be a valid finite number" }
        require(heightCm >= MIN_JUMP_HEIGHT_CM) { "Height cannot be negative: $heightCm" }
        require(heightCm <= MAX_JUMP_HEIGHT_CM) { "Height is unreasonably high: $heightCm cm (max: $MAX_JUMP_HEIGHT_CM cm)" }
        require(confidenceScore.isFinite()) { "Confidence score must be a valid finite number" }
        require(confidenceScore in MIN_CONFIDENCE_SCORE..MAX_CONFIDENCE_SCORE) { 
            "Confidence score must be between $MIN_CONFIDENCE_SCORE and $MAX_CONFIDENCE_SCORE: $confidenceScore" 
        }
        require(timestamp > 0) { "Timestamp must be positive: $timestamp" }
        
        // Validate optional fields
        takeoffTime?.let { require(it >= 0) { "Takeoff time cannot be negative: $it" } }
        landingTime?.let { require(it >= 0) { "Landing time cannot be negative: $it" } }
        flightTimeMs?.let { require(it >= 0) { "Flight time cannot be negative: $it" } }
        peakHeightFrame?.let { require(it >= 0) { "Peak height frame cannot be negative: $it" } }
        calibrationReference?.let { require(it.isFinite()) { "Calibration reference must be finite: $it" } }
        notes?.let { require(it.length <= MAX_NOTES_LENGTH) { "Notes too long: ${it.length} > $MAX_NOTES_LENGTH" } }
        require(measurementMethod.isNotBlank()) { "Measurement method cannot be blank" }
        require(measurementMethod.length <= MAX_MEASUREMENT_METHOD_LENGTH) { 
            "Measurement method too long: ${measurementMethod.length} > $MAX_MEASUREMENT_METHOD_LENGTH" 
        }
        
        // Validate flight time consistency
        if (takeoffTime != null && landingTime != null && flightTimeMs != null) {
            val calculatedFlightTime = landingTime - takeoffTime
            require(kotlin.math.abs(calculatedFlightTime - flightTimeMs) <= 100) {
                "Flight time inconsistent with takeoff/landing times. Calculated: $calculatedFlightTime, provided: $flightTimeMs"
            }
        }
    }
}