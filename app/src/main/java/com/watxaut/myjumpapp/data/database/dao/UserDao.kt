package com.watxaut.myjumpapp.data.database.dao

import androidx.room.*
import com.watxaut.myjumpapp.data.database.entities.User
import com.watxaut.myjumpapp.domain.jump.SurfaceType
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    
    @Query("SELECT * FROM users WHERE is_active = 1 ORDER BY user_name ASC")
    fun getAllActiveUsers(): Flow<List<User>>
    
    @Query("SELECT * FROM users WHERE user_id = :userId")
    suspend fun getUserById(userId: String): User?
    
    @Query("SELECT * FROM users WHERE user_name = :userName LIMIT 1")
    suspend fun getUserByName(userName: String): User?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)
    
    @Update
    suspend fun updateUser(user: User)
    
    @Delete
    suspend fun deleteUser(user: User)
    
    @Query("UPDATE users SET is_active = 0 WHERE user_id = :userId")
    suspend fun deactivateUser(userId: String)
    
    @Query("UPDATE users SET total_jumps = :totalJumps, best_jump_height = :bestHeight WHERE user_id = :userId")
    suspend fun updateUserStats(userId: String, totalJumps: Int, bestHeight: Double)
    
    @Query("""UPDATE users SET 
              best_jump_height_hard_floor = :bestHeightHardFloor,
              best_jump_height_sand = :bestHeightSand,
              total_sessions_hard_floor = :totalSessionsHardFloor,
              total_sessions_sand = :totalSessionsSand,
              total_jumps_hard_floor = :totalJumpsHardFloor,
              total_jumps_sand = :totalJumpsSand
              WHERE user_id = :userId""")
    suspend fun updateSurfaceSpecificStats(
        userId: String,
        bestHeightHardFloor: Double,
        bestHeightSand: Double,
        totalSessionsHardFloor: Int,
        totalSessionsSand: Int,
        totalJumpsHardFloor: Int,
        totalJumpsSand: Int
    )
    
    @Query("SELECT COUNT(*) FROM users WHERE is_active = 1")
    suspend fun getActiveUserCount(): Int
    
    @Query("SELECT * FROM users ORDER BY date_created DESC")
    suspend fun getAllUsers(): List<User>
    
    // Additional methods for statistics
    @Query("SELECT * FROM users WHERE user_id = :userId")
    fun getUserByIdFlow(userId: String): Flow<User?>
}