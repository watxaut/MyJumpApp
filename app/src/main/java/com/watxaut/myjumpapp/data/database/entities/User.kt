package com.watxaut.myjumpapp.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: String = UUID.randomUUID().toString(),
    
    @ColumnInfo(name = "user_name")
    val userName: String,
    
    @ColumnInfo(name = "date_created")
    val dateCreated: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "height_cm")
    val heightCm: Int? = null,
    
    @ColumnInfo(name = "weight_kg")
    val weightKg: Double? = null,
    
    @ColumnInfo(name = "date_of_birth")
    val dateOfBirth: Long? = null,
    
    @ColumnInfo(name = "profile_image_path")
    val profileImagePath: String? = null,
    
    @ColumnInfo(name = "total_jumps")
    val totalJumps: Int = 0,
    
    @ColumnInfo(name = "best_jump_height")
    val bestJumpHeight: Double = 0.0,
    
    @ColumnInfo(name = "best_jump_height_hard_floor")
    val bestJumpHeightHardFloor: Double = 0.0,
    
    @ColumnInfo(name = "best_jump_height_sand")
    val bestJumpHeightSand: Double = 0.0,
    
    @ColumnInfo(name = "total_sessions_hard_floor")
    val totalSessionsHardFloor: Int = 0,
    
    @ColumnInfo(name = "total_sessions_sand")
    val totalSessionsSand: Int = 0,
    
    @ColumnInfo(name = "total_jumps_hard_floor")
    val totalJumpsHardFloor: Int = 0,
    
    @ColumnInfo(name = "total_jumps_sand")
    val totalJumpsSand: Int = 0,
    
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    
    @ColumnInfo(name = "eye_to_head_vertex_cm")
    val eyeToHeadVertexCm: Double? = null,
    
    @ColumnInfo(name = "heel_to_hand_reach_cm")
    val heelToHandReachCm: Double? = null
)