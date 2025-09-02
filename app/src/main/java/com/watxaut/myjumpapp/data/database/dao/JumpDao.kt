package com.watxaut.myjumpapp.data.database.dao

import androidx.room.*
import com.watxaut.myjumpapp.data.database.entities.Jump
import kotlinx.coroutines.flow.Flow

@Dao
interface JumpDao {
    
    @Query("SELECT * FROM jumps WHERE user_id = :userId ORDER BY timestamp DESC")
    fun getJumpsByUser(userId: String): Flow<List<Jump>>
    
    @Query("SELECT * FROM jumps WHERE user_id = :userId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getJumpsByUserAndDateRange(userId: String, startTime: Long, endTime: Long): Flow<List<Jump>>
    
    @Query("SELECT * FROM jumps WHERE session_id = :sessionId ORDER BY timestamp ASC")
    fun getJumpsBySession(sessionId: String): Flow<List<Jump>>
    
    @Query("SELECT MAX(height_cm) FROM jumps WHERE user_id = :userId")
    suspend fun getBestJumpHeight(userId: String): Double?
    
    @Query("SELECT AVG(height_cm) FROM jumps WHERE user_id = :userId")
    suspend fun getAverageJumpHeight(userId: String): Double?
    
    @Query("SELECT COUNT(*) FROM jumps WHERE user_id = :userId")
    suspend fun getTotalJumpCount(userId: String): Int
    
    @Query("SELECT * FROM jumps WHERE jump_id = :jumpId")
    suspend fun getJumpById(jumpId: String): Jump?
    
    @Query("SELECT * FROM jumps WHERE user_id = :userId ORDER BY height_cm DESC LIMIT :limit")
    suspend fun getTopJumps(userId: String, limit: Int): List<Jump>
    
    @Query("SELECT * FROM jumps ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentJumps(limit: Int): List<Jump>
    
    @Insert
    suspend fun insertJump(jump: Jump)
    
    @Insert
    suspend fun insertJumps(jumps: List<Jump>)
    
    @Update
    suspend fun updateJump(jump: Jump)
    
    @Delete
    suspend fun deleteJump(jump: Jump)
    
    @Query("DELETE FROM jumps WHERE user_id = :userId")
    suspend fun deleteAllJumpsForUser(userId: String)
    
    @Query("DELETE FROM jumps WHERE session_id = :sessionId")
    suspend fun deleteJumpsBySession(sessionId: String)
    
    // Additional methods for statistics
    @Query("SELECT * FROM jumps WHERE user_id = :userId ORDER BY timestamp ASC")
    suspend fun getAllJumpsByUserId(userId: String): List<Jump>
    
    @Query("SELECT * FROM jumps WHERE user_id = :userId ORDER BY timestamp ASC")
    fun getAllJumpsByUserIdFlow(userId: String): Flow<List<Jump>>
}