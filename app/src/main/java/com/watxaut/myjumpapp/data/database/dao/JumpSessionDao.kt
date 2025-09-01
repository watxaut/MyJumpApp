package com.watxaut.myjumpapp.data.database.dao

import androidx.room.*
import com.watxaut.myjumpapp.data.database.entities.JumpSession
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
}