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
)