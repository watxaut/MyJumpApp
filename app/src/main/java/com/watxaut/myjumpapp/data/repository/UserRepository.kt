package com.watxaut.myjumpapp.data.repository

import com.watxaut.myjumpapp.data.database.dao.UserDao
import com.watxaut.myjumpapp.data.database.entities.User
import com.watxaut.myjumpapp.domain.jump.SurfaceType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao
) {
    
    fun getAllActiveUsers(): Flow<List<User>> {
        return userDao.getAllActiveUsers()
    }
    
    suspend fun getUserById(userId: String): User? {
        return userDao.getUserById(userId)
    }
    
    suspend fun getUserByName(userName: String): User? {
        return userDao.getUserByName(userName)
    }
    
    suspend fun insertUser(user: User) {
        userDao.insertUser(user)
    }
    
    suspend fun updateUser(user: User) {
        userDao.updateUser(user)
    }
    
    suspend fun deleteUser(user: User) {
        userDao.deleteUser(user)
    }
    
    suspend fun deactivateUser(userId: String) {
        userDao.deactivateUser(userId)
    }
    
    suspend fun updateUserStats(userId: String, totalJumps: Int, bestHeight: Double) {
        userDao.updateUserStats(userId, totalJumps, bestHeight)
    }
    
    suspend fun updateSurfaceSpecificStats(
        userId: String,
        bestHeightHardFloor: Double,
        bestHeightSand: Double,
        totalSessionsHardFloor: Int,
        totalSessionsSand: Int,
        totalJumpsHardFloor: Int,
        totalJumpsSand: Int
    ) {
        userDao.updateSurfaceSpecificStats(
            userId = userId,
            bestHeightHardFloor = bestHeightHardFloor,
            bestHeightSand = bestHeightSand,
            totalSessionsHardFloor = totalSessionsHardFloor,
            totalSessionsSand = totalSessionsSand,
            totalJumpsHardFloor = totalJumpsHardFloor,
            totalJumpsSand = totalJumpsSand
        )
    }
    
    suspend fun getActiveUserCount(): Int {
        return userDao.getActiveUserCount()
    }
    
    suspend fun getAllUsers(): List<User> {
        return userDao.getAllUsers()
    }
}