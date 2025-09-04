package com.watxaut.myjumpapp.data.database.dao

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.watxaut.myjumpapp.data.database.JumpDatabase
import com.watxaut.myjumpapp.data.database.entities.JumpSession
import com.watxaut.myjumpapp.data.database.entities.User
import com.watxaut.myjumpapp.domain.jump.SurfaceType
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
class JumpSessionDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: JumpDatabase
    private lateinit var jumpSessionDao: JumpSessionDao
    private lateinit var userDao: UserDao

    private val testUser = User(
        userId = "user-1",
        userName = "Test User",
        heightCm = 170
    )

    private val testSession1 = JumpSession(
        sessionId = "session-1",
        userId = "user-1",
        sessionName = "Hard Floor Session",
        startTime = System.currentTimeMillis() - 10000L,
        endTime = System.currentTimeMillis() - 5000L,
        surfaceType = SurfaceType.HARD_FLOOR,
        totalJumps = 1,
        bestJumpHeight = 55.0,
        averageJumpHeight = 55.0,
        isCompleted = true
    )

    private val testSession2 = JumpSession(
        sessionId = "session-2",
        userId = "user-1",
        sessionName = "Sand Session",
        startTime = System.currentTimeMillis() - 8000L,
        surfaceType = SurfaceType.SAND,
        totalJumps = 1,
        bestJumpHeight = 45.0,
        averageJumpHeight = 45.0,
        isCompleted = false
    )

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            JumpDatabase::class.java
        ).allowMainThreadQueries().build()
        
        jumpSessionDao = database.jumpSessionDao()
        userDao = database.userDao()
        
        // Insert test user first (foreign key constraint)
        runTest {
            userDao.insertUser(testUser)
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    // Test basic CRUD operations
    @Test
    fun `insertSession should insert session successfully`() = runTest {
        jumpSessionDao.insertSession(testSession1)
        
        val retrievedSession = jumpSessionDao.getSessionById(testSession1.sessionId)
        assertThat(retrievedSession).isNotNull()
        assertThat(retrievedSession!!.sessionName).isEqualTo("Hard Floor Session")
        assertThat(retrievedSession.surfaceType).isEqualTo(SurfaceType.HARD_FLOOR)
        assertThat(retrievedSession.bestJumpHeight).isEqualTo(55.0)
    }

    @Test
    fun `insertSession should handle different surface types`() = runTest {
        jumpSessionDao.insertSession(testSession1) // Hard floor
        jumpSessionDao.insertSession(testSession2) // Sand
        
        val hardFloorSession = jumpSessionDao.getSessionById(testSession1.sessionId)
        val sandSession = jumpSessionDao.getSessionById(testSession2.sessionId)
        
        assertThat(hardFloorSession!!.surfaceType).isEqualTo(SurfaceType.HARD_FLOOR)
        assertThat(sandSession!!.surfaceType).isEqualTo(SurfaceType.SAND)
    }

    @Test
    fun `getSessionsByUser should return user sessions ordered by start time`() = runTest {
        val earlierSession = testSession1.copy(
            sessionId = "earlier",
            startTime = System.currentTimeMillis() - 20000L
        )
        val laterSession = testSession1.copy(
            sessionId = "later",
            startTime = System.currentTimeMillis() - 5000L
        )
        
        jumpSessionDao.insertSession(laterSession)
        jumpSessionDao.insertSession(earlierSession)
        
        val sessions = jumpSessionDao.getSessionsByUserId("user-1")
        assertThat(sessions).hasSize(2)
        // Should be ordered by start time descending (most recent first)
        assertThat(sessions[0].sessionId).isEqualTo("later")
        assertThat(sessions[1].sessionId).isEqualTo("earlier")
    }

    @Test
    fun `updateSession should update session fields`() = runTest {
        jumpSessionDao.insertSession(testSession1)
        
        val updatedSession = testSession1.copy(
            endTime = System.currentTimeMillis(),
            bestJumpHeight = 60.0,
            isCompleted = true
        )
        
        jumpSessionDao.updateSession(updatedSession)
        
        val retrievedSession = jumpSessionDao.getSessionById(testSession1.sessionId)
        assertThat(retrievedSession!!.bestJumpHeight).isEqualTo(60.0)
        assertThat(retrievedSession.isCompleted).isTrue()
        assertThat(retrievedSession.endTime).isNotNull()
    }

    @Test
    fun `deleteSession should remove session from database`() = runTest {
        jumpSessionDao.insertSession(testSession1)
        jumpSessionDao.deleteSession(testSession1)
        
        val retrievedSession = jumpSessionDao.getSessionById(testSession1.sessionId)
        assertThat(retrievedSession).isNull()
    }

    // Test surface type filtering
    @Test
    fun `getSessionsByUserAndSurface should filter by surface type`() = runTest {
        jumpSessionDao.insertSession(testSession1) // Hard floor
        jumpSessionDao.insertSession(testSession2) // Sand
        
        jumpSessionDao.getSessionsByUserAndSurface("user-1", "HARD_FLOOR").test {
            val hardFloorSessions = awaitItem()
            assertThat(hardFloorSessions).hasSize(1)
            assertThat(hardFloorSessions[0].surfaceType).isEqualTo(SurfaceType.HARD_FLOOR)
        }
        
        jumpSessionDao.getSessionsByUserAndSurface("user-1", "SAND").test {
            val sandSessions = awaitItem()
            assertThat(sandSessions).hasSize(1)
            assertThat(sandSessions[0].surfaceType).isEqualTo(SurfaceType.SAND)
        }
    }

    @Test
    fun `getCompletedSessionsByUserAndSurface should only return completed sessions`() = runTest {
        jumpSessionDao.insertSession(testSession1) // Completed hard floor
        jumpSessionDao.insertSession(testSession2) // Incomplete sand
        
        val completedSand = testSession2.copy(sessionId = "completed-sand", isCompleted = true)
        jumpSessionDao.insertSession(completedSand)
        
        val completedHardFloorSessions = jumpSessionDao.getCompletedSessionsByUserAndSurface("user-1", "HARD_FLOOR")
        assertThat(completedHardFloorSessions).hasSize(1)
        assertThat(completedHardFloorSessions[0].isCompleted).isTrue()
        
        val completedSandSessions = jumpSessionDao.getCompletedSessionsByUserAndSurface("user-1", "SAND")
        assertThat(completedSandSessions).hasSize(1)
        assertThat(completedSandSessions[0].sessionId).isEqualTo("completed-sand")
    }

    // Test statistical queries
    @Test
    fun `getSessionCountByUserAndSurface should count sessions correctly`() = runTest {
        jumpSessionDao.insertSession(testSession1) // Hard floor
        jumpSessionDao.insertSession(testSession2) // Sand
        jumpSessionDao.insertSession(testSession1.copy(sessionId = "session-3")) // Another hard floor
        
        val hardFloorCount = jumpSessionDao.getSessionCountByUserAndSurface("user-1", "HARD_FLOOR")
        val sandCount = jumpSessionDao.getSessionCountByUserAndSurface("user-1", "SAND")
        
        assertThat(hardFloorCount).isEqualTo(2)
        assertThat(sandCount).isEqualTo(1)
    }

    @Test
    fun `getBestHeightByUserAndSurface should return maximum height`() = runTest {
        val lowSession = testSession1.copy(sessionId = "low", bestJumpHeight = 50.0)
        val highSession = testSession1.copy(sessionId = "high", bestJumpHeight = 65.0)
        
        jumpSessionDao.insertSession(lowSession)
        jumpSessionDao.insertSession(highSession)
        
        val bestHeight = jumpSessionDao.getBestHeightByUserAndSurface("user-1", "HARD_FLOOR")
        assertThat(bestHeight).isEqualTo(65.0)
    }

    @Test
    fun `getBestHeightByUserAndSurface should return null when no sessions exist`() = runTest {
        val bestHeight = jumpSessionDao.getBestHeightByUserAndSurface("user-1", "SAND")
        assertThat(bestHeight).isNull()
    }

    // Test session completion
    @Test
    fun `completeSession should mark session as completed with end time`() = runTest {
        val incompleteSession = testSession1.copy(isCompleted = false, endTime = null)
        jumpSessionDao.insertSession(incompleteSession)
        
        val endTime = System.currentTimeMillis()
        jumpSessionDao.completeSession(incompleteSession.sessionId, endTime)
        
        val completedSession = jumpSessionDao.getSessionById(incompleteSession.sessionId)
        assertThat(completedSession!!.isCompleted).isTrue()
        assertThat(completedSession.endTime).isEqualTo(endTime)
    }

    @Test
    fun `updateSessionStats should update session statistics`() = runTest {
        jumpSessionDao.insertSession(testSession1)
        
        jumpSessionDao.updateSessionStats(testSession1.sessionId, 1, 62.0, 62.0)
        
        val updatedSession = jumpSessionDao.getSessionById(testSession1.sessionId)
        assertThat(updatedSession!!.totalJumps).isEqualTo(1)
        assertThat(updatedSession.bestJumpHeight).isEqualTo(62.0)
        assertThat(updatedSession.averageJumpHeight).isEqualTo(62.0)
    }

    // Test active session management
    @Test
    fun `getActiveSession should return incomplete session for user`() = runTest {
        jumpSessionDao.insertSession(testSession1) // Completed
        jumpSessionDao.insertSession(testSession2) // Incomplete
        
        val activeSession = jumpSessionDao.getActiveSession("user-1")
        assertThat(activeSession).isNotNull()
        assertThat(activeSession!!.sessionId).isEqualTo(testSession2.sessionId)
        assertThat(activeSession.isCompleted).isFalse()
    }

    @Test
    fun `getActiveSession should return null when no active session exists`() = runTest {
        jumpSessionDao.insertSession(testSession1) // Completed
        
        val activeSession = jumpSessionDao.getActiveSession("user-1")
        assertThat(activeSession).isNull()
    }

    // Test recent sessions
    @Test
    fun `getRecentSessions should return sessions ordered by recency`() = runTest {
        val oldSession = testSession1.copy(
            sessionId = "old",
            startTime = System.currentTimeMillis() - 50000L
        )
        val recentSession = testSession1.copy(
            sessionId = "recent",
            startTime = System.currentTimeMillis() - 1000L
        )
        
        jumpSessionDao.insertSession(oldSession)
        jumpSessionDao.insertSession(recentSession)
        
        val recentSessions = jumpSessionDao.getRecentSessions(10)
        assertThat(recentSessions).hasSize(2)
        assertThat(recentSessions[0].sessionId).isEqualTo("recent")
        assertThat(recentSessions[1].sessionId).isEqualTo("old")
    }

    @Test
    fun `getRecentSessions should respect limit parameter`() = runTest {
        repeat(5) { i ->
            jumpSessionDao.insertSession(testSession1.copy(sessionId = "session-$i"))
        }
        
        val recentSessions = jumpSessionDao.getRecentSessions(3)
        assertThat(recentSessions).hasSize(3)
    }

    // Test Flow-based methods
    @Test
    fun `getSessionsByUserFlow should emit updates when sessions change`() = runTest {
        jumpSessionDao.getSessionsByUser("user-1").test {
            // Initial empty list
            assertThat(awaitItem()).isEmpty()
            
            // Insert session
            jumpSessionDao.insertSession(testSession1)
            val firstUpdate = awaitItem()
            assertThat(firstUpdate).hasSize(1)
            assertThat(firstUpdate[0].sessionId).isEqualTo(testSession1.sessionId)
            
            // Insert another session
            jumpSessionDao.insertSession(testSession2)
            val secondUpdate = awaitItem()
            assertThat(secondUpdate).hasSize(2)
        }
    }

    // Test foreign key constraints
    @Test
    fun `inserting session with non-existent userId should fail gracefully`() = runTest {
        val sessionWithInvalidUser = testSession1.copy(userId = "non-existent-user")
        
        // This should handle the foreign key constraint violation
        try {
            jumpSessionDao.insertSession(sessionWithInvalidUser)
            // If we reach here, check that session was not actually inserted
            val retrievedSession = jumpSessionDao.getSessionById(sessionWithInvalidUser.sessionId)
            assertThat(retrievedSession).isNull()
        } catch (e: Exception) {
            // Foreign key constraint violation is expected
            assertThat(e).isInstanceOf(android.database.sqlite.SQLiteConstraintException::class.java)
        }
    }
}