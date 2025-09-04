package com.watxaut.myjumpapp.data.database.dao

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.watxaut.myjumpapp.data.database.JumpDatabase
import com.watxaut.myjumpapp.data.database.entities.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class UserDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: JumpDatabase
    private lateinit var userDao: UserDao

    private val testUser1 = User(
        userId = "user-1",
        userName = "Test User 1",
        heightCm = 170,
        bestJumpHeight = 55.0,
        totalJumps = 10,
        bestJumpHeightHardFloor = 60.0,
        bestJumpHeightSand = 45.0,
        totalSessionsHardFloor = 6,
        totalSessionsSand = 4,
        totalJumpsHardFloor = 6,
        totalJumpsSand = 4
    )

    private val testUser2 = User(
        userId = "user-2",
        userName = "Test User 2",
        heightCm = 165,
        bestJumpHeight = 50.0,
        totalJumps = 5,
        bestJumpHeightHardFloor = 50.0,
        bestJumpHeightSand = 40.0,
        totalSessionsHardFloor = 3,
        totalSessionsSand = 2,
        totalJumpsHardFloor = 3,
        totalJumpsSand = 2
    )

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            JumpDatabase::class.java
        ).allowMainThreadQueries().build()
        
        userDao = database.userDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // Test basic CRUD operations
    @Test
    fun `insertUser should insert user successfully`() = runTest {
        userDao.insertUser(testUser1)
        
        val retrievedUser = userDao.getUserById(testUser1.userId)
        assertThat(retrievedUser).isNotNull()
        assertThat(retrievedUser!!.userName).isEqualTo("Test User 1")
        assertThat(retrievedUser.heightCm).isEqualTo(170)
    }

    @Test
    fun `insertUser should replace existing user on conflict`() = runTest {
        userDao.insertUser(testUser1)
        
        val updatedUser = testUser1.copy(userName = "Updated User Name")
        userDao.insertUser(updatedUser)
        
        val retrievedUser = userDao.getUserById(testUser1.userId)
        assertThat(retrievedUser!!.userName).isEqualTo("Updated User Name")
    }

    @Test
    fun `getAllUsers should return all users`() = runTest {
        userDao.insertUser(testUser1)
        userDao.insertUser(testUser2)
        
        val users = userDao.getAllUsers()
        assertThat(users).hasSize(2)
        assertThat(users.map { it.userId }).containsExactly("user-1", "user-2")
    }

    @Test
    fun `getUserById should return null for non-existent user`() = runTest {
        val user = userDao.getUserById("non-existent")
        assertThat(user).isNull()
    }

    @Test
    fun `deleteUser should remove user from database`() = runTest {
        userDao.insertUser(testUser1)
        userDao.deleteUser(testUser1)
        
        val retrievedUser = userDao.getUserById(testUser1.userId)
        assertThat(retrievedUser).isNull()
    }

    // Test Flow-based methods
    @Test
    fun `getAllActiveUsersFlow should emit users when database changes`() = runTest {
        userDao.getAllActiveUsers().test {
            // Initial empty list
            assertThat(awaitItem()).isEmpty()
            
            // Insert first user (active by default)
            userDao.insertUser(testUser1)
            val firstUpdate = awaitItem()
            assertThat(firstUpdate).hasSize(1)
            assertThat(firstUpdate[0].userId).isEqualTo("user-1")
            
            // Insert second user (active by default)
            userDao.insertUser(testUser2)
            val secondUpdate = awaitItem()
            assertThat(secondUpdate).hasSize(2)
        }
    }

    @Test
    fun `getUserByIdFlow should emit user updates`() = runTest {
        userDao.getUserByIdFlow(testUser1.userId).test {
            // Initial null (user doesn't exist)
            assertThat(awaitItem()).isNull()
            
            // Insert user
            userDao.insertUser(testUser1)
            val insertedUser = awaitItem()
            assertThat(insertedUser).isNotNull()
            assertThat(insertedUser!!.userName).isEqualTo("Test User 1")
            
            // Update user
            val updatedUser = testUser1.copy(userName = "Updated Name")
            userDao.insertUser(updatedUser)
            val updated = awaitItem()
            assertThat(updated!!.userName).isEqualTo("Updated Name")
        }
    }

    // Test statistics update methods
    @Test
    fun `updateUserStats should update total jumps and best height`() = runTest {
        userDao.insertUser(testUser1)
        
        userDao.updateUserStats(testUser1.userId, 15, 70.0)
        
        val updatedUser = userDao.getUserById(testUser1.userId)
        assertThat(updatedUser!!.totalJumps).isEqualTo(15)
        assertThat(updatedUser.bestJumpHeight).isEqualTo(70.0)
    }

    @Test
    fun `updateUserStats should not affect non-existent user`() = runTest {
        userDao.updateUserStats("non-existent", 10, 50.0)
        
        val user = userDao.getUserById("non-existent")
        assertThat(user).isNull()
    }

    @Test
    fun `updateSurfaceSpecificStats should update surface-specific fields`() = runTest {
        userDao.insertUser(testUser1)
        
        userDao.updateSurfaceSpecificStats(
            userId = testUser1.userId,
            bestHeightHardFloor = 65.0,
            bestHeightSand = 50.0,
            totalSessionsHardFloor = 8,
            totalSessionsSand = 6,
            totalJumpsHardFloor = 8,
            totalJumpsSand = 6
        )
        
        val updatedUser = userDao.getUserById(testUser1.userId)
        assertThat(updatedUser!!.bestJumpHeightHardFloor).isEqualTo(65.0)
        assertThat(updatedUser.bestJumpHeightSand).isEqualTo(50.0)
        assertThat(updatedUser.totalSessionsHardFloor).isEqualTo(8)
        assertThat(updatedUser.totalSessionsSand).isEqualTo(6)
        assertThat(updatedUser.totalJumpsHardFloor).isEqualTo(8)
        assertThat(updatedUser.totalJumpsSand).isEqualTo(6)
    }

    // Test data validation
    @Test
    fun `user with duplicate userId should replace existing user`() = runTest {
        userDao.insertUser(testUser1)
        
        val duplicateUser = User(
            userId = testUser1.userId, // Same ID
            userName = "Duplicate User",
            heightCm = 180,
            bestJumpHeight = 70.0,
            totalJumps = 20
        )
        
        userDao.insertUser(duplicateUser)
        
        val users = userDao.getAllUsers()
        assertThat(users).hasSize(1) // Should still be only one user
        assertThat(users[0].userName).isEqualTo("Duplicate User")
        assertThat(users[0].heightCm).isEqualTo(180)
    }

    // Test surface statistics calculations
    @Test
    fun `surface statistics should be correctly stored and retrieved`() = runTest {
        val userWithSurfaceStats = testUser1.copy(
            bestJumpHeightHardFloor = 65.0,
            bestJumpHeightSand = 45.0,
            totalSessionsHardFloor = 8,
            totalSessionsSand = 3,
            totalJumpsHardFloor = 8,
            totalJumpsSand = 3
        )
        
        userDao.insertUser(userWithSurfaceStats)
        
        val retrievedUser = userDao.getUserById(testUser1.userId)
        assertThat(retrievedUser!!.bestJumpHeightHardFloor).isEqualTo(65.0)
        assertThat(retrievedUser.bestJumpHeightSand).isEqualTo(45.0)
        assertThat(retrievedUser.totalSessionsHardFloor).isEqualTo(8)
        assertThat(retrievedUser.totalSessionsSand).isEqualTo(3)
        
        // Verify that hard floor performance is better than sand
        assertThat(retrievedUser.bestJumpHeightHardFloor)
            .isGreaterThan(retrievedUser.bestJumpHeightSand)
    }

    // Test concurrent access
    @Test
    fun `multiple updates to same user should be handled correctly`() = runTest {
        userDao.insertUser(testUser1)
        
        // Simulate multiple statistics updates
        userDao.updateUserStats(testUser1.userId, 11, 56.0)
        userDao.updateUserStats(testUser1.userId, 12, 58.0)
        userDao.updateSurfaceSpecificStats(testUser1.userId, 62.0, 48.0, 7, 5, 7, 5)
        
        val finalUser = userDao.getUserById(testUser1.userId)
        assertThat(finalUser!!.totalJumps).isEqualTo(12)
        assertThat(finalUser.bestJumpHeight).isEqualTo(58.0)
        assertThat(finalUser.bestJumpHeightHardFloor).isEqualTo(62.0)
        assertThat(finalUser.totalSessionsHardFloor).isEqualTo(7)
    }
}