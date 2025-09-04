package com.watxaut.myjumpapp.data.repository

import com.google.common.truth.Truth.assertThat
import com.watxaut.myjumpapp.data.database.dao.JumpDao
import com.watxaut.myjumpapp.data.database.dao.JumpSessionDao
import com.watxaut.myjumpapp.data.database.dao.UserDao
import com.watxaut.myjumpapp.data.database.entities.Jump
import com.watxaut.myjumpapp.data.database.entities.JumpSession
import com.watxaut.myjumpapp.data.database.entities.User
import com.watxaut.myjumpapp.domain.jump.SurfaceType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@ExperimentalCoroutinesApi
class StatisticsRepositoryTest {

    private lateinit var statisticsRepository: StatisticsRepository
    private lateinit var mockJumpDao: JumpDao
    private lateinit var mockJumpSessionDao: JumpSessionDao
    private lateinit var mockUserDao: UserDao

    private val testUserId = "test-user-123"
    private val testUser = User(
        userId = testUserId,
        userName = "Test User",
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

    @Before
    fun setUp() {
        mockJumpDao = mock()
        mockJumpSessionDao = mock()
        mockUserDao = mock()
        
        statisticsRepository = StatisticsRepository(
            jumpDao = mockJumpDao,
            jumpSessionDao = mockJumpSessionDao,
            userDao = mockUserDao
        )
    }

    // Test overall statistics calculation from sessions
    @Test
    fun `getUserStatistics should calculate correct overall stats from completed sessions`() = runTest {
        val completedSessions = createTestSessions(
            count = 5,
            completed = true,
            heights = listOf(50.0, 55.0, 48.0, 52.0, 58.0)
        )
        
        whenever(mockUserDao.getUserById(testUserId)).thenReturn(testUser)
        whenever(mockJumpDao.getAllJumpsByUserId(testUserId)).thenReturn(emptyList())
        whenever(mockJumpSessionDao.getSessionsByUserId(testUserId)).thenReturn(completedSessions)
        
        val result = statisticsRepository.getUserStatistics(testUserId)
        
        assertThat(result).isNotNull()
        assertThat(result!!.overallStats.totalJumps).isEqualTo(5) // Each session = 1 jump
        assertThat(result.overallStats.bestJumpHeight).isEqualTo(58.0)
        assertThat(result.overallStats.averageJumpHeight).isWithin(0.1).of(52.6) // Average of heights
    }

    @Test
    fun `getUserStatistics should return zero stats when no completed sessions`() = runTest {
        val incompleteSessions = createTestSessions(count = 3, completed = false)
        
        whenever(mockUserDao.getUserById(testUserId)).thenReturn(testUser)
        whenever(mockJumpDao.getAllJumpsByUserId(testUserId)).thenReturn(emptyList())
        whenever(mockJumpSessionDao.getSessionsByUserId(testUserId)).thenReturn(incompleteSessions)
        
        val result = statisticsRepository.getUserStatistics(testUserId)
        
        assertThat(result).isNotNull()
        assertThat(result!!.overallStats.totalJumps).isEqualTo(0)
        assertThat(result.overallStats.bestJumpHeight).isEqualTo(0.0)
        assertThat(result.overallStats.averageJumpHeight).isEqualTo(0.0)
    }

    // Test surface-specific statistics
    @Test
    fun `getUserStatistics should calculate surface-specific stats correctly`() = runTest {
        val hardFloorSessions = createTestSessionsWithSurface(
            count = 3,
            surfaceType = SurfaceType.HARD_FLOOR,
            heights = listOf(55.0, 60.0, 58.0)
        )
        val sandSessions = createTestSessionsWithSurface(
            count = 2,
            surfaceType = SurfaceType.SAND,
            heights = listOf(45.0, 48.0)
        )
        val allSessions = hardFloorSessions + sandSessions
        
        whenever(mockUserDao.getUserById(testUserId)).thenReturn(testUser)
        whenever(mockJumpDao.getAllJumpsByUserId(testUserId)).thenReturn(emptyList())
        whenever(mockJumpSessionDao.getSessionsByUserId(testUserId)).thenReturn(allSessions)
        
        val result = statisticsRepository.getUserStatistics(testUserId)
        
        assertThat(result).isNotNull()
        val surfaceStats = result!!.surfaceStats
        
        assertThat(surfaceStats.hardFloorStats.bestHeight).isEqualTo(60.0)
        assertThat(surfaceStats.hardFloorStats.averageHeight).isWithin(0.1).of(57.67) // Average of hard floor
        assertThat(surfaceStats.sandStats.bestHeight).isEqualTo(48.0)
        assertThat(surfaceStats.sandStats.averageHeight).isWithin(0.1).of(46.5) // Average of sand
        
        // Test comparison calculation
        val expectedHeightDifference = ((60.0 - 48.0) / 60.0) * 100
        assertThat(surfaceStats.comparison.heightDifferencePercent).isWithin(0.1).of(expectedHeightDifference)
    }

    // Test recent statistics calculations
    @Test
    fun `getUserStatistics should calculate recent stats correctly`() = runTest {
        val now = System.currentTimeMillis()
        val fiveDaysAgo = now - (5 * 24 * 60 * 60 * 1000L)
        val tenDaysAgo = now - (10 * 24 * 60 * 60 * 1000L)
        val twentyDaysAgo = now - (20 * 24 * 60 * 60 * 1000L)
        
        val recentSessions = listOf(
            createTestSession("1", fiveDaysAgo, 55.0, SurfaceType.HARD_FLOOR, completed = true),
            createTestSession("2", tenDaysAgo, 50.0, SurfaceType.HARD_FLOOR, completed = true),
            createTestSession("3", twentyDaysAgo, 48.0, SurfaceType.SAND, completed = true)
        )
        
        whenever(mockUserDao.getUserById(testUserId)).thenReturn(testUser)
        whenever(mockJumpDao.getAllJumpsByUserId(testUserId)).thenReturn(emptyList())
        whenever(mockJumpSessionDao.getSessionsByUserId(testUserId)).thenReturn(recentSessions)
        
        val result = statisticsRepository.getUserStatistics(testUserId)
        
        assertThat(result).isNotNull()
        // Last 7 days should include sessions from 5 days ago only
        assertThat(result!!.recentStats.last7Days.sessionCount).isEqualTo(1)
        assertThat(result.recentStats.last7Days.bestJumpHeight).isEqualTo(55.0)
        
        // Last 30 days should include all sessions
        assertThat(result.recentStats.last30Days.sessionCount).isEqualTo(3)
        assertThat(result.recentStats.last30Days.bestJumpHeight).isEqualTo(55.0)
    }

    // Test dashboard statistics
    @Test
    fun `getDashboardStats should calculate quick stats correctly`() = runTest {
        val sessions = createTestSessions(count = 8, completed = true, heights = listOf(50.0, 55.0, 48.0, 52.0, 58.0, 47.0, 53.0, 56.0))
        
        whenever(mockJumpDao.getAllJumpsByUserId(testUserId)).thenReturn(emptyList())
        whenever(mockJumpSessionDao.getSessionsByUserId(testUserId)).thenReturn(sessions)
        
        val result = statisticsRepository.getDashboardStats(testUserId)
        
        assertThat(result).isNotNull()
        assertThat(result!!.quickStats.totalJumps).isEqualTo(8)
        assertThat(result.quickStats.personalBest).isEqualTo(58.0)
    }

    // Test progress statistics
    @Test
    fun `getUserStatistics should calculate progress stats with height progression`() = runTest {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val twoDaysAgo = today.minusDays(2)
        
        val sessionsWithDifferentDates = listOf(
            createTestSessionOnDate("1", today, 55.0),
            createTestSessionOnDate("2", yesterday, 50.0),
            createTestSessionOnDate("3", twoDaysAgo, 48.0)
        )
        
        whenever(mockUserDao.getUserById(testUserId)).thenReturn(testUser)
        whenever(mockJumpDao.getAllJumpsByUserId(testUserId)).thenReturn(emptyList())
        whenever(mockJumpSessionDao.getSessionsByUserId(testUserId)).thenReturn(sessionsWithDifferentDates)
        
        val result = statisticsRepository.getUserStatistics(testUserId)
        
        assertThat(result).isNotNull()
        val progressStats = result!!.progressStats
        
        assertThat(progressStats.heightProgression).hasSize(3)
        assertThat(progressStats.heightProgression.last().bestHeight).isEqualTo(55.0) // Today's session
        assertThat(progressStats.heightProgression.last().jumpCount).isEqualTo(1) // One session per day
    }

    // Test achievement statistics
    @Test
    fun `getUserStatistics should calculate achievement stats with milestones`() = runTest {
        val highPerformanceSessions = createTestSessions(
            count = 3,
            completed = true,
            heights = listOf(65.0, 70.0, 68.0) // Heights that should unlock milestones
        )
        
        whenever(mockUserDao.getUserById(testUserId)).thenReturn(testUser)
        whenever(mockJumpDao.getAllJumpsByUserId(testUserId)).thenReturn(emptyList())
        whenever(mockJumpSessionDao.getSessionsByUserId(testUserId)).thenReturn(highPerformanceSessions)
        
        val result = statisticsRepository.getUserStatistics(testUserId)
        
        assertThat(result).isNotNull()
        val achievements = result!!.achievementStats
        
        assertThat(achievements.personalRecords.highestJump).isNotNull()
        assertThat(achievements.personalRecords.highestJump!!.height).isEqualTo(70.0)
        
        // Check that height milestones are marked as achieved
        val heightMilestones = achievements.milestones.filter { it.category.name == "HEIGHT" }
        val achieved60cm = heightMilestones.find { it.targetValue == 60.0 }
        assertThat(achieved60cm?.isAchieved).isTrue()
    }

    // Test consistency score calculation
    @Test
    fun `consistency score should be high for consistent performances`() = runTest {
        val consistentSessions = createTestSessions(
            count = 5,
            completed = true,
            heights = listOf(50.0, 51.0, 49.0, 50.5, 49.5) // Very consistent
        )
        
        whenever(mockUserDao.getUserById(testUserId)).thenReturn(testUser)
        whenever(mockJumpDao.getAllJumpsByUserId(testUserId)).thenReturn(emptyList())
        whenever(mockJumpSessionDao.getSessionsByUserId(testUserId)).thenReturn(consistentSessions)
        
        val result = statisticsRepository.getUserStatistics(testUserId)
        
        assertThat(result).isNotNull()
        // Consistent performances should have high consistency score (> 80%)
        assertThat(result!!.recentStats.last7Days.consistencyScore).isGreaterThan(80.0)
    }

    @Test
    fun `consistency score should be low for inconsistent performances`() = runTest {
        val inconsistentSessions = createTestSessions(
            count = 5,
            completed = true,
            heights = listOf(30.0, 70.0, 20.0, 65.0, 25.0) // Very inconsistent
        )
        
        whenever(mockUserDao.getUserById(testUserId)).thenReturn(testUser)
        whenever(mockJumpDao.getAllJumpsByUserId(testUserId)).thenReturn(emptyList())
        whenever(mockJumpSessionDao.getSessionsByUserId(testUserId)).thenReturn(inconsistentSessions)
        
        val result = statisticsRepository.getUserStatistics(testUserId)
        
        assertThat(result).isNotNull()
        // Inconsistent performances should have low consistency score (< 50%)
        assertThat(result!!.recentStats.last7Days.consistencyScore).isLessThan(50.0)
    }

    // Test Flow-based getUserStatisticsFlow
    @Test
    fun `getUserStatisticsFlow should emit updated statistics when data changes`() = runTest {
        val sessions = createTestSessions(count = 3, completed = true)
        
        whenever(mockJumpDao.getAllJumpsByUserIdFlow(testUserId)).thenReturn(flowOf(emptyList()))
        whenever(mockJumpSessionDao.getSessionsByUserIdFlow(testUserId)).thenReturn(flowOf(sessions))
        whenever(mockUserDao.getUserByIdFlow(testUserId)).thenReturn(flowOf(testUser))
        
        val result = statisticsRepository.getUserStatisticsFlow(testUserId)
        
        // Collect the first emission
        result.collect { statistics ->
            assertThat(statistics).isNotNull()
            assertThat(statistics!!.overallStats.totalJumps).isEqualTo(3)
        }
    }

    // Test null user handling
    @Test
    fun `getUserStatistics should return null when user not found`() = runTest {
        whenever(mockUserDao.getUserById(testUserId)).thenReturn(null)
        
        val result = statisticsRepository.getUserStatistics(testUserId)
        
        assertThat(result).isNull()
    }

    // Helper functions to create test data
    private fun createTestSessions(
        count: Int,
        completed: Boolean = true,
        heights: List<Double>? = null
    ): List<JumpSession> {
        return (1..count).map { i ->
            createTestSession(
                id = "session-$i",
                startTime = System.currentTimeMillis() - (i * 1000L),
                height = heights?.getOrNull(i - 1) ?: (50.0 + i),
                completed = completed
            )
        }
    }

    private fun createTestSessionsWithSurface(
        count: Int,
        surfaceType: SurfaceType,
        heights: List<Double>
    ): List<JumpSession> {
        return (1..count).map { i ->
            createTestSession(
                id = "session-$surfaceType-$i",
                startTime = System.currentTimeMillis() - (i * 1000L),
                height = heights[i - 1],
                surfaceType = surfaceType,
                completed = true
            )
        }
    }

    private fun createTestSession(
        id: String,
        startTime: Long,
        height: Double,
        surfaceType: SurfaceType = SurfaceType.HARD_FLOOR,
        completed: Boolean = true
    ): JumpSession {
        return JumpSession(
            sessionId = id,
            userId = testUserId,
            sessionName = "Test Session $id",
            startTime = startTime,
            endTime = if (completed) startTime + 30000L else null,
            surfaceType = surfaceType,
            totalJumps = 1,
            bestJumpHeight = height,
            averageJumpHeight = height,
            isCompleted = completed
        )
    }

    private fun createTestSessionOnDate(
        id: String,
        date: LocalDate,
        height: Double
    ): JumpSession {
        val startTime = date.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000L
        return createTestSession(id, startTime, height, completed = true)
    }
}