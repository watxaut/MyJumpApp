package com.watxaut.myjumpapp.data.database.dao

import androidx.room.*
import com.watxaut.myjumpapp.data.database.entities.JumpSession
import com.watxaut.myjumpapp.domain.jump.SurfaceType
import kotlinx.coroutines.flow.Flow

@Dao
interface JumpSessionDao {
    
    @Query("SELECT * FROM jump_sessions WHERE user_id = :userId ORDER BY start_time DESC")
    fun getSessionsByUser(userId: String): Flow<List<JumpSession>>
    
    @Query("SELECT * FROM jump_sessions WHERE session_id = :sessionId")
    suspend fun getSessionById(sessionId: String): JumpSession?
    
    @Query("SELECT * FROM jump_sessions WHERE user_id = :userId AND is_completed = 0 LIMIT 1")
    suspend fun getActiveSession(userId: String): JumpSession?
    
    @Query("SELECT * FROM jump_sessions ORDER BY start_time DESC LIMIT :limit")
    suspend fun getRecentSessions(limit: Int): List<JumpSession>
    
    @Insert
    suspend fun insertSession(session: JumpSession)
    
    @Update
    suspend fun updateSession(session: JumpSession)
    
    @Query("UPDATE jump_sessions SET is_completed = 1, end_time = :endTime WHERE session_id = :sessionId")
    suspend fun completeSession(sessionId: String, endTime: Long = System.currentTimeMillis())
    
    @Query("UPDATE jump_sessions SET total_jumps = :totalJumps, best_jump_height = :bestHeight, average_jump_height = :avgHeight WHERE session_id = :sessionId")
    suspend fun updateSessionStats(
        sessionId: String, 
        totalJumps: Int, 
        bestHeight: Double, 
        avgHeight: Double
    )
    
    @Delete
    suspend fun deleteSession(session: JumpSession)
    
    @Query("DELETE FROM jump_sessions WHERE user_id = :userId")
    suspend fun deleteSessionsForUser(userId: String)
    
    // Additional methods for statistics
    @Query("SELECT * FROM jump_sessions WHERE user_id = :userId ORDER BY start_time ASC")
    suspend fun getSessionsByUserId(userId: String): List<JumpSession>
    
    @Query("SELECT * FROM jump_sessions WHERE user_id = :userId ORDER BY start_time ASC")
    fun getSessionsByUserIdFlow(userId: String): Flow<List<JumpSession>>
    
    // Surface type filtering methods
    @Query("SELECT * FROM jump_sessions WHERE user_id = :userId AND surface_type = :surfaceType ORDER BY start_time DESC")
    fun getSessionsByUserAndSurface(userId: String, surfaceType: String): Flow<List<JumpSession>>
    
    @Query("SELECT * FROM jump_sessions WHERE user_id = :userId AND surface_type = :surfaceType AND is_completed = 1 ORDER BY start_time DESC")
    suspend fun getCompletedSessionsByUserAndSurface(userId: String, surfaceType: String): List<JumpSession>
    
    @Query("SELECT COUNT(*) FROM jump_sessions WHERE user_id = :userId AND surface_type = :surfaceType AND is_completed = 1")
    suspend fun getSessionCountByUserAndSurface(userId: String, surfaceType: String): Int
    
    @Query("SELECT MAX(best_jump_height) FROM jump_sessions WHERE user_id = :userId AND surface_type = :surfaceType AND is_completed = 1")
    suspend fun getBestHeightByUserAndSurface(userId: String, surfaceType: String): Double?
}