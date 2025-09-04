package com.watxaut.myjumpapp.presentation.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.watxaut.myjumpapp.domain.statistics.*
import com.watxaut.myjumpapp.domain.jump.SurfaceType
import com.watxaut.myjumpapp.presentation.ui.theme.MyJumpAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class StatisticsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testUserStatistics = UserStatistics(
        userId = "test-user",
        userName = "Test User",
        overallStats = OverallStats(
            totalJumps = 25,
            totalSessions = 25,
            bestJumpHeight = 65.0,
            averageJumpHeight = 55.0,
            totalFlightTime = 0L,
            averageFlightTime = 0L,
            firstJumpDate = LocalDateTime.now().minusDays(30),
            lastJumpDate = LocalDateTime.now(),
            activeDays = 15
        ),
        recentStats = RecentStats(
            last7Days = PeriodStats(
                jumpCount = 3,
                sessionCount = 3,
                bestJumpHeight = 60.0,
                averageJumpHeight = 58.0,
                totalFlightTime = 0L,
                improvement = 5.0,
                consistencyScore = 85.0
            ),
            last30Days = PeriodStats(
                jumpCount = 10,
                sessionCount = 10,
                bestJumpHeight = 65.0,
                averageJumpHeight = 55.0,
                totalFlightTime = 0L,
                improvement = 10.0,
                consistencyScore = 78.0
            ),
            thisWeek = PeriodStats(
                jumpCount = 4,
                sessionCount = 4,
                bestJumpHeight = 62.0,
                averageJumpHeight = 57.0,
                totalFlightTime = 0L,
                improvement = 8.0,
                consistencyScore = 82.0
            ),
            thisMonth = PeriodStats(
                jumpCount = 8,
                sessionCount = 8,
                bestJumpHeight = 65.0,
                averageJumpHeight = 56.0,
                totalFlightTime = 0L,
                improvement = 12.0,
                consistencyScore = 80.0
            )
        ),
        progressStats = ProgressStats(
            heightProgression = listOf(
                HeightDataPoint(LocalDate.now().minusDays(7), 50.0, 50.0, 1),
                HeightDataPoint(LocalDate.now().minusDays(3), 55.0, 55.0, 1),
                HeightDataPoint(LocalDate.now(), 60.0, 60.0, 1)
            ),
            volumeProgression = listOf(),
            consistencyProgression = listOf()
        ),
        achievementStats = AchievementStats(
            personalRecords = PersonalRecords(
                highestJump = JumpRecord(
                    jumpId = "jump-1",
                    height = 65.0,
                    flightTime = null,
                    date = LocalDateTime.now(),
                    sessionId = "session-1"
                ),
                longestFlightTime = null,
                mostJumpsInSession = null,
                mostJumpsInDay = DayRecord(
                    date = LocalDate.now(),
                    jumpCount = 3,
                    sessionCount = 3,
                    bestHeight = 62.0
                ),
                bestAverageHeightInSession = null
            ),
            milestones = listOf(
                Milestone(
                    id = "height_60",
                    title = "60cm Jump",
                    description = "Achieve a jump height of 60cm",
                    targetValue = 60.0,
                    currentValue = 65.0,
                    isAchieved = true,
                    achievedDate = LocalDateTime.now().minusDays(5),
                    category = MilestoneCategory.HEIGHT
                )
            ),
            streaks = StreakStats(
                currentStreak = 5,
                longestStreak = 8,
                currentWeeklyStreak = 2,
                longestWeeklyStreak = 4,
                lastActiveDate = LocalDate.now()
            )
        ),
        surfaceStats = SurfaceFilteredStats(
            hardFloorStats = SurfaceSpecificStats(
                surfaceType = SurfaceType.HARD_FLOOR,
                totalSessions = 15,
                bestHeight = 65.0,
                averageHeight = 58.0,
                last7DaysSessions = 2,
                last30DaysSessions = 8,
                firstSessionDate = LocalDateTime.now().minusDays(25),
                lastSessionDate = LocalDateTime.now()
            ),
            sandStats = SurfaceSpecificStats(
                surfaceType = SurfaceType.SAND,
                totalSessions = 10,
                bestHeight = 52.0,
                averageHeight = 48.0,
                last7DaysSessions = 1,
                last30DaysSessions = 5,
                firstSessionDate = LocalDateTime.now().minusDays(20),
                lastSessionDate = LocalDateTime.now().minusDays(2)
            ),
            comparison = SurfaceComparison(
                heightDifferencePercent = 25.0,
                preferredSurface = SurfaceType.HARD_FLOOR,
                sessionRatio = 1.5
            )
        )
    )

    @Test
    fun statisticsScreen_displaysUserName() {
        composeTestRule.setContent {
            MyJumpAppTheme {
                StatisticsScreen(
                    userId = "test-user",
                    statistics = testUserStatistics,
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Test User")
            .assertIsDisplayed()
    }

    @Test
    fun statisticsScreen_displaysOverallStats() {
        composeTestRule.setContent {
            MyJumpAppTheme {
                StatisticsScreen(
                    userId = "test-user",
                    statistics = testUserStatistics,
                    onNavigateBack = {}
                )
            }
        }

        // Check overall statistics are displayed
        composeTestRule
            .onNodeWithText("25") // Total sessions
            .assertIsDisplayed()
            
        composeTestRule
            .onNodeWithText("65.0cm") // Best jump height
            .assertIsDisplayed()
            
        composeTestRule
            .onNodeWithText("55.0cm") // Average height
            .assertIsDisplayed()
    }

    @Test
    fun statisticsScreen_hasNavigationTabs() {
        composeTestRule.setContent {
            MyJumpAppTheme {
                StatisticsScreen(
                    userId = "test-user",
                    statistics = testUserStatistics,
                    onNavigateBack = {}
                )
            }
        }

        // Check that main tabs are present
        composeTestRule
            .onNodeWithText("Overview")
            .assertIsDisplayed()
            .assertIsSelected()
            
        composeTestRule
            .onNodeWithText("Progress")
            .assertIsDisplayed()
            
        composeTestRule
            .onNodeWithText("Achievements")
            .assertIsDisplayed()
            
        composeTestRule
            .onNodeWithText("Surfaces")
            .assertIsDisplayed()
    }

    @Test
    fun statisticsScreen_canNavigateBetweenTabs() {
        composeTestRule.setContent {
            MyJumpAppTheme {
                StatisticsScreen(
                    userId = "test-user",
                    statistics = testUserStatistics,
                    onNavigateBack = {}
                )
            }
        }

        // Click on Surfaces tab
        composeTestRule
            .onNodeWithText("Surfaces")
            .performClick()
            
        // Verify surface-specific content is shown
        composeTestRule
            .onNodeWithText("🏀") // Hard floor icon
            .assertIsDisplayed()
            
        composeTestRule
            .onNodeWithText("🏖️") // Sand icon
            .assertIsDisplayed()
    }

    @Test
    fun statisticsScreen_displaysRecentStats() {
        composeTestRule.setContent {
            MyJumpAppTheme {
                StatisticsScreen(
                    userId = "test-user",
                    statistics = testUserStatistics,
                    onNavigateBack = {}
                )
            }
        }

        // Check recent statistics section
        composeTestRule
            .onNodeWithText("Last 7 Days")
            .assertIsDisplayed()
            
        composeTestRule
            .onNodeWithText("Last 30 Days")
            .assertIsDisplayed()
            
        composeTestRule
            .onNodeWithText("3 sessions") // Last 7 days sessions
            .assertIsDisplayed()
            
        composeTestRule
            .onNodeWithText("60.0cm best") // Last 7 days best
            .assertIsDisplayed()
    }

    @Test
    fun statisticsScreen_displaysPersonalRecords() {
        composeTestRule.setContent {
            MyJumpAppTheme {
                StatisticsScreen(
                    userId = "test-user",
                    statistics = testUserStatistics,
                    onNavigateBack = {}
                )
            }
        }

        // Navigate to Achievements tab
        composeTestRule
            .onNodeWithText("Achievements")
            .performClick()

        // Check personal records are displayed
        composeTestRule
            .onNodeWithText("Personal Records")
            .assertIsDisplayed()
            
        composeTestRule
            .onNodeWithText("Highest Jump")
            .assertIsDisplayed()
            
        composeTestRule
            .onNodeWithText("65.0cm")
            .assertIsDisplayed()
    }

    @Test
    fun statisticsScreen_displaysSurfaceComparison() {
        composeTestRule.setContent {
            MyJumpAppTheme {
                StatisticsScreen(
                    userId = "test-user",
                    statistics = testUserStatistics,
                    onNavigateBack = {}
                )
            }
        }

        // Navigate to Surfaces tab
        composeTestRule
            .onNodeWithText("Surfaces")
            .performClick()

        // Check surface statistics are displayed
        composeTestRule
            .onNodeWithText("Hard Floor")
            .assertIsDisplayed()
            
        composeTestRule
            .onNodeWithText("Sand")
            .assertIsDisplayed()
            
        // Check specific surface stats
        composeTestRule
            .onNodeWithText("15") // Hard floor total sessions
            .assertIsDisplayed()
            
        composeTestRule
            .onNodeWithText("10") // Sand total sessions
            .assertIsDisplayed()
    }

    @Test
    fun statisticsScreen_backButtonWorks() {
        var backPressed = false
        
        composeTestRule.setContent {
            MyJumpAppTheme {
                StatisticsScreen(
                    userId = "test-user",
                    statistics = testUserStatistics,
                    onNavigateBack = { backPressed = true }
                )
            }
        }

        // Find and click the back button
        composeTestRule
            .onNodeWithContentDescription("Back")
            .performClick()

        assert(backPressed)
    }

    @Test
    fun statisticsScreen_displaysLoadingState() {
        composeTestRule.setContent {
            MyJumpAppTheme {
                StatisticsScreen(
                    userId = "test-user",
                    statistics = null, // No statistics loaded
                    onNavigateBack = {}
                )
            }
        }

        // Should display loading indicator or empty state
        composeTestRule
            .onNodeWithText("Test User", useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun statisticsScreen_displaysConsistencyScores() {
        composeTestRule.setContent {
            MyJumpAppTheme {
                StatisticsScreen(
                    userId = "test-user",
                    statistics = testUserStatistics,
                    onNavigateBack = {}
                )
            }
        }

        // Check that consistency scores are displayed
        composeTestRule
            .onNodeWithText("85%", substring = true) // 7-day consistency
            .assertExists()
            
        composeTestRule
            .onNodeWithText("78%", substring = true) // 30-day consistency  
            .assertExists()
    }

    @Test
    fun statisticsScreen_displaysImprovementPercentages() {
        composeTestRule.setContent {
            MyJumpAppTheme {
                StatisticsScreen(
                    userId = "test-user",
                    statistics = testUserStatistics,
                    onNavigateBack = {}
                )
            }
        }

        // Check that improvement percentages are displayed
        composeTestRule
            .onNodeWithText("+5.0%", substring = true) // 7-day improvement
            .assertExists()
            
        composeTestRule
            .onNodeWithText("+10.0%", substring = true) // 30-day improvement
            .assertExists()
    }
}