package com.watxaut.myjumpapp.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.watxaut.myjumpapp.domain.jump.SurfaceType
import java.util.UUID

@Entity(
    tableName = "jump_sessions",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["user_id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["start_time"]),
        Index(value = ["surface_type"])
    ]
)
data class JumpSession(
    @PrimaryKey
    @ColumnInfo(name = "session_id")
    val sessionId: String = UUID.randomUUID().toString(),
    
    @ColumnInfo(name = "user_id")
    val userId: String,
    
    @ColumnInfo(name = "session_name")
    val sessionName: String? = null,
    
    @ColumnInfo(name = "start_time")
    val startTime: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "end_time")
    val endTime: Long? = null,
    
    @ColumnInfo(name = "total_jumps")
    val totalJumps: Int = 0,
    
    @ColumnInfo(name = "best_jump_height")
    val bestJumpHeight: Double = 0.0,
    
    @ColumnInfo(name = "average_jump_height")
    val averageJumpHeight: Double = 0.0,
    
    @ColumnInfo(name = "session_notes")
    val sessionNotes: String? = null,
    
    @ColumnInfo(name = "surface_type")
    val surfaceType: SurfaceType = SurfaceType.HARD_FLOOR,
    
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false
)