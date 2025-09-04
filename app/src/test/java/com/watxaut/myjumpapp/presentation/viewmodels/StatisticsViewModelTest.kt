package com.watxaut.myjumpapp.presentation.viewmodels

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.watxaut.myjumpapp.data.repository.StatisticsRepository
import com.watxaut.myjumpapp.domain.statistics.*
import com.watxaut.myjumpapp.domain.jump.SurfaceType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import java.time.LocalDateTime
import java.time.LocalDate

@ExperimentalCoroutinesApi
class StatisticsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var mockStatisticsRepository: StatisticsRepository

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: StatisticsViewModel
    private val testUserId = "test-user-id"

    private val testUserStatistics = UserStatistics(
        userId = testUserId,
        userName = "Test User",
        overallStats = OverallStats(
            totalJumps = 10,
            totalSessions = 3,
            bestJumpHeight = 55.5,
            averageJumpHeight = 45.2,
            totalFlightTime = 5000L,
            averageFlightTime = 500L,
            firstJumpDate = LocalDateTime.now().minusDays(7),
            lastJumpDate = LocalDateTime.now(),
            activeDays = 5
        ),
        recentStats = RecentStats(
            last7Days = PeriodStats(8, 2, 55.5, 44.0, 4000L, 10.0, 85.0),
            last30Days = PeriodStats(10, 3, 55.5, 45.2, 5000L, 5.0, 80.0),
            thisWeek = PeriodStats(5, 1, 50.0, 42.0, 2500L, 15.0, 90.0),
            thisMonth = PeriodStats(10, 3, 55.5, 45.2, 5000L, 8.0, 82.0)
        ),
        progressStats = ProgressStats(
            heightProgression = emptyList(),
            volumeProgression = emptyList(),
            consistencyProgression = emptyList()
        ),
        achievementStats = AchievementStats(
            personalRecords = PersonalRecords(null, null, null, null, null),
            milestones = emptyList(),
            streaks = StreakStats(3, 5, 2, 3, null)
        ),
        surfaceStats = SurfaceFilteredStats(
            hardFloorStats = SurfaceSpecificStats(
                surfaceType = SurfaceType.HARD_FLOOR,
                totalSessions = 2,
                bestHeight = 55.5,
                averageHeight = 45.0,
                last7DaysSessions = 2,
                last30DaysSessions = 2,
                firstSessionDate = LocalDateTime.now().minusDays(5),
                lastSessionDate = LocalDateTime.now()
            ),
            sandStats = SurfaceSpecificStats(
                surfaceType = SurfaceType.SAND,
                totalSessions = 1,
                bestHeight = 40.0,
                averageHeight = 38.0,
                last7DaysSessions = 1,
                last30DaysSessions = 1,
                firstSessionDate = LocalDateTime.now().minusDays(3),
                lastSessionDate = LocalDateTime.now().minusDays(3)
            ),
            comparison = SurfaceComparison(
                heightDifferencePercent = 15.5,
                preferredSurface = SurfaceType.HARD_FLOOR,
                sessionRatio = 2.0
            )
        )
    )

    private val testDashboardStats = DashboardStats(
        todayStats = DayStats(
            date = LocalDate.now(),
            jumpCount = 5,
            sessionCount = 1,
            bestJumpHeight = 50.0,
            totalFlightTime = 2500L
        ),
        weekStats = WeekStats(
            weekStart = LocalDate.now().minusDays(6),
            jumpCount = 10,
            sessionCount = 3,
            activeDays = 3,
            bestJumpHeight = 55.5,
            averageJumpHeight = 45.0
        ),
        quickStats = QuickStats(
            totalJumps = 10,
            currentStreak = 3,
            personalBest = 55.5,
            last7DaysJumps = 8
        ),
        recentSessions = emptyList()
    )

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        viewModel = StatisticsViewModel(mockStatisticsRepository)
    }

    @Test
    fun `viewModel should have correct initial state`() = runTest {
        viewModel.uiState.test {
            val initialState = awaitItem()
            
            assertThat(initialState.isLoading).isTrue()
            assertThat(initialState.userStatistics).isNull()
            assertThat(initialState.dashboardStats).isNull()
            assertThat(initialState.error).isNull()
        }
    }

    @Test
    fun `loadUserStatistics should update state with loaded statistics`() = runTest {
        whenever(mockStatisticsRepository.getUserStatistics(testUserId))
            .thenReturn(testUserStatistics)
        whenever(mockStatisticsRepository.getDashboardStats(testUserId))
            .thenReturn(testDashboardStats)
        
        viewModel.uiState.test {
            val initialState = awaitItem()
            
            viewModel.loadUserStatistics(testUserId)
            
            // Should show loading state
            val loadingState = awaitItem()
            assertThat(loadingState.isLoading).isTrue()
            
            // Should show loaded statistics
            val loadedState = awaitItem()
            assertThat(loadedState.isLoading).isFalse()
            assertThat(loadedState.userStatistics).isEqualTo(testUserStatistics)
            assertThat(loadedState.dashboardStats).isEqualTo(testDashboardStats)
            assertThat(loadedState.error).isNull()
        }
    }

    @Test
    fun `loadUserStatistics should handle repository error`() = runTest {
        whenever(mockStatisticsRepository.getUserStatistics(testUserId))
            .thenThrow(RuntimeException("Database error"))
        
        viewModel.uiState.test {
            val initialState = awaitItem()
            
            viewModel.loadUserStatistics(testUserId)
            
            // Should show loading state
            val loadingState = awaitItem()
            assertThat(loadingState.isLoading).isTrue()
            
            // Should show error state
            val errorState = awaitItem()
            assertThat(errorState.isLoading).isFalse()
            assertThat(errorState.userStatistics).isNull()
            assertThat(errorState.error).isEqualTo("Database error")
        }
    }

    @Test
    fun `loadUserStatistics should skip if already loaded for same user`() = runTest {
        whenever(mockStatisticsRepository.getUserStatistics(testUserId))
            .thenReturn(testUserStatistics)
        whenever(mockStatisticsRepository.getDashboardStats(testUserId))
            .thenReturn(testDashboardStats)
        
        viewModel.uiState.test {
            val initialState = awaitItem()
            
            // First load
            viewModel.loadUserStatistics(testUserId)
            skipItems(2) // Skip loading and loaded states
            
            // Second load with same user ID
            viewModel.loadUserStatistics(testUserId)
            
            // Should not emit any new states since data is already loaded
            expectNoEvents()
        }
    }
}