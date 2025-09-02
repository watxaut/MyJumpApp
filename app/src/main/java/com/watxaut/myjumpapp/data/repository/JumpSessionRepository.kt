package com.watxaut.myjumpapp.data.repository

import com.watxaut.myjumpapp.data.database.dao.JumpSessionDao
import com.watxaut.myjumpapp.data.database.entities.JumpSession
import com.watxaut.myjumpapp.domain.jump.SurfaceType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JumpSessionRepository @Inject constructor(
    private val jumpSessionDao: JumpSessionDao
) {
    
    fun getSessionsByUserId(userId: String): Flow<List<JumpSession>> {
        return jumpSessionDao.getSessionsByUser(userId)
    }
    
    suspend fun getSessionById(sessionId: String): JumpSession? {
        return jumpSessionDao.getSessionById(sessionId)
    }
    
    suspend fun insertSession(session: JumpSession) {
        jumpSessionDao.insertSession(session)
    }
    
    suspend fun updateSession(session: JumpSession) {
        jumpSessionDao.updateSession(session)
    }
    
    suspend fun deleteSession(session: JumpSession) {
        jumpSessionDao.deleteSession(session)
    }
    
    suspend fun getActiveSessionForUser(userId: String): JumpSession? {
        return jumpSessionDao.getActiveSession(userId)
    }
    
    suspend fun completeSession(sessionId: String, endTime: Long = System.currentTimeMillis()) {
        jumpSessionDao.completeSession(sessionId, endTime)
    }
    
    suspend fun updateSessionStats(sessionId: String, totalJumps: Int, bestHeight: Double, averageHeight: Double) {
        jumpSessionDao.updateSessionStats(sessionId, totalJumps, bestHeight, averageHeight)
    }
    
    suspend fun getRecentSessions(limit: Int = 10): List<JumpSession> {
        return jumpSessionDao.getRecentSessions(limit)
    }
    
    // Surface type filtering methods
    fun getSessionsByUserAndSurface(userId: String, surfaceType: SurfaceType): Flow<List<JumpSession>> {
        return jumpSessionDao.getSessionsByUserAndSurface(userId, surfaceType.name)
    }
    
    suspend fun getCompletedSessionsByUserAndSurface(userId: String, surfaceType: SurfaceType): List<JumpSession> {
        return jumpSessionDao.getCompletedSessionsByUserAndSurface(userId, surfaceType.name)
    }
    
    suspend fun getSessionCountByUserAndSurface(userId: String, surfaceType: SurfaceType): Int {
        return jumpSessionDao.getSessionCountByUserAndSurface(userId, surfaceType.name)
    }
    
    suspend fun getBestHeightByUserAndSurface(userId: String, surfaceType: SurfaceType): Double {
        return jumpSessionDao.getBestHeightByUserAndSurface(userId, surfaceType.name) ?: 0.0
    }
}