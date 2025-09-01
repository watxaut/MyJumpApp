package com.watxaut.myjumpapp.data.repository

import com.watxaut.myjumpapp.data.database.dao.JumpDao
import com.watxaut.myjumpapp.data.database.entities.Jump
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JumpRepository @Inject constructor(
    private val jumpDao: JumpDao
) {
    
    fun getJumpsBySessionId(sessionId: String): Flow<List<Jump>> {
        return jumpDao.getJumpsBySession(sessionId)
    }
    
    fun getJumpsByUserId(userId: String): Flow<List<Jump>> {
        return jumpDao.getJumpsByUser(userId)
    }
    
    suspend fun getJumpById(jumpId: String): Jump? {
        return jumpDao.getJumpById(jumpId)
    }
    
    suspend fun insertJump(jump: Jump) {
        jumpDao.insertJump(jump)
    }
    
    suspend fun insertJumps(jumps: List<Jump>) {
        jumpDao.insertJumps(jumps)
    }
    
    suspend fun updateJump(jump: Jump) {
        jumpDao.updateJump(jump)
    }
    
    suspend fun deleteJump(jump: Jump) {
        jumpDao.deleteJump(jump)
    }
    
    suspend fun deleteJumpsBySessionId(sessionId: String) {
        jumpDao.deleteJumpsBySession(sessionId)
    }
    
    suspend fun getBestJumpHeightForUser(userId: String): Double? {
        return jumpDao.getBestJumpHeight(userId)
    }
    
    suspend fun getAverageHeightForUser(userId: String): Double? {
        return jumpDao.getAverageJumpHeight(userId)
    }
    
    suspend fun getTotalJumpsForUser(userId: String): Int {
        return jumpDao.getTotalJumpCount(userId)
    }
    
    suspend fun getTopJumpsForUser(userId: String, limit: Int = 10): List<Jump> {
        return jumpDao.getTopJumps(userId, limit)
    }
    
    suspend fun getRecentJumps(limit: Int = 10): List<Jump> {
        return jumpDao.getRecentJumps(limit)
    }
}