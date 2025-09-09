package com.watxaut.myjumpapp.domain.statistics

import com.watxaut.myjumpapp.domain.jump.SurfaceType
import com.watxaut.myjumpapp.domain.jump.JumpType
import java.time.LocalDate
import java.time.LocalDateTime

data class UserStatistics(
    val userId: String,
    val userName: String,
    val overallStats: OverallStats,
    val recentStats: RecentStats,
    val progressStats: ProgressStats,
    val achievementStats: AchievementStats,
    val surfaceStats: SurfaceFilteredStats,
    val jumpTypeStats: JumpTypeFilteredStats,
    val combinedStats: CombinedFilteredStats
)

data class OverallStats(
    val totalJumps: Int,
    val totalSessions: Int,
    val bestJumpHeight: Double,
    val averageJumpHeight: Double,
    val bestSpikeReach: Double,
    val averageSpikeReach: Double,
    val totalFlightTime: Long, // in milliseconds
    val averageFlightTime: Long, // in milliseconds
    val firstJumpDate: LocalDateTime?,
    val lastJumpDate: LocalDateTime?,
    val activeDays: Int
)

data class RecentStats(
    val last7Days: PeriodStats,
    val last30Days: PeriodStats,
    val thisWeek: PeriodStats,
    val thisMonth: PeriodStats
)

data class PeriodStats(
    val jumpCount: Int,
    val sessionCount: Int,
    val bestJumpHeight: Double,
    val averageJumpHeight: Double,
    val bestSpikeReach: Double,
    val averageSpikeReach: Double,
    val totalFlightTime: Long,
    val improvement: Double, // percentage change from previous period
    val spikeReachImprovement: Double, // percentage change from previous period for spike reach
    val consistencyScore: Double // 0-100, based on regularity of jumping
)

data class ProgressStats(
    val heightProgression: List<HeightDataPoint>,
    val volumeProgression: List<VolumeDataPoint>,
    val consistencyProgression: List<ConsistencyDataPoint>
)

data class HeightDataPoint(
    val date: LocalDate,
    val averageHeight: Double,
    val bestHeight: Double,
    val averageSpikeReach: Double,
    val bestSpikeReach: Double,
    val jumpCount: Int
)

data class VolumeDataPoint(
    val date: LocalDate,
    val jumpCount: Int,
    val sessionCount: Int,
    val totalFlightTime: Long
)

data class ConsistencyDataPoint(
    val date: LocalDate,
    val hasJumped: Boolean,
    val jumpCount: Int
)

data class AchievementStats(
    val personalRecords: PersonalRecords,
    val milestones: List<Milestone>,
    val streaks: StreakStats
)

data class PersonalRecords(
    val highestJump: JumpRecord?,
    val highestSpikeReach: JumpRecord?,
    val longestFlightTime: JumpRecord?,
    val mostJumpsInSession: SessionRecord?,
    val mostJumpsInDay: DayRecord?,
    val bestAverageHeightInSession: SessionRecord?,
    val bestAverageSpikeReachInSession: SessionRecord?
)

data class JumpRecord(
    val jumpId: String,
    val height: Double,
    val spikeReach: Double,
    val flightTime: Long?,
    val date: LocalDateTime,
    val sessionId: String?
)

data class SessionRecord(
    val sessionId: String,
    val value: Double, // height or count
    val jumpCount: Int,
    val date: LocalDateTime
)

data class DayRecord(
    val date: LocalDate,
    val jumpCount: Int,
    val sessionCount: Int,
    val bestHeight: Double,
    val bestSpikeReach: Double
)

data class Milestone(
    val id: String,
    val title: String,
    val description: String,
    val targetValue: Double,
    val currentValue: Double,
    val isAchieved: Boolean,
    val achievedDate: LocalDateTime?,
    val category: MilestoneCategory
)

enum class MilestoneCategory {
    HEIGHT,
    SPIKE_REACH,
    VOLUME,
    CONSISTENCY,
    FLIGHT_TIME
}

data class StreakStats(
    val currentStreak: Int, // consecutive days with jumps
    val longestStreak: Int,
    val currentWeeklyStreak: Int, // consecutive weeks with jumps
    val longestWeeklyStreak: Int,
    val lastActiveDate: LocalDate?
)

// Statistics for dashboard/summary views
data class DashboardStats(
    val todayStats: DayStats,
    val weekStats: WeekStats,
    val quickStats: QuickStats,
    val recentSessions: List<SessionSummary>,
    val jumpTypeBreakdown: JumpTypeBreakdown
)

data class JumpTypeBreakdown(
    val staticCount: Int,
    val dynamicCount: Int,
    val staticBestHeight: Double,
    val dynamicBestHeight: Double,
    val staticAverageHeight: Double,
    val dynamicAverageHeight: Double
)

data class DayStats(
    val date: LocalDate,
    val jumpCount: Int,
    val sessionCount: Int,
    val bestJumpHeight: Double,
    val bestSpikeReach: Double,
    val totalFlightTime: Long
)

data class WeekStats(
    val weekStart: LocalDate,
    val jumpCount: Int,
    val sessionCount: Int,
    val activeDays: Int,
    val bestJumpHeight: Double,
    val averageJumpHeight: Double,
    val bestSpikeReach: Double,
    val averageSpikeReach: Double
)

data class QuickStats(
    val totalJumps: Int,
    val currentStreak: Int,
    val personalBest: Double,
    val personalBestSpikeReach: Double,
    val last7DaysJumps: Int
)

data class SessionSummary(
    val sessionId: String,
    val sessionName: String?,
    val date: LocalDateTime,
    val jumpCount: Int,
    val bestJumpHeight: Double,
    val averageJumpHeight: Double,
    val bestSpikeReach: Double,
    val averageSpikeReach: Double,
    val duration: Long, // in milliseconds
    val isCompleted: Boolean
)

// Time period enums for filtering
enum class TimePeriod {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    LAST_7_DAYS,
    LAST_30_DAYS,
    LAST_90_DAYS,
    THIS_YEAR,
    ALL_TIME
}

enum class StatisticType {
    HEIGHT,
    SPIKE_REACH,
    VOLUME,
    CONSISTENCY,
    FLIGHT_TIME,
    SESSIONS
}

// Surface-specific statistics
data class SurfaceFilteredStats(
    val hardFloorStats: SurfaceSpecificStats,
    val sandStats: SurfaceSpecificStats,
    val comparison: SurfaceComparison
)

data class SurfaceSpecificStats(
    val surfaceType: SurfaceType,
    val totalSessions: Int,
    val bestHeight: Double,
    val averageHeight: Double,
    val bestSpikeReach: Double,
    val averageSpikeReach: Double,
    val last7DaysSessions: Int,
    val last30DaysSessions: Int,
    val firstSessionDate: LocalDateTime?,
    val lastSessionDate: LocalDateTime?
)

data class SurfaceComparison(
    val heightDifferencePercent: Double, // % difference between hard floor and sand
    val spikeReachDifferencePercent: Double, // % difference between hard floor and sand spike reach
    val preferredSurface: SurfaceType?, // Surface with more sessions
    val sessionRatio: Double // Ratio of hard floor to sand sessions
)

// Jump Type specific statistics
data class JumpTypeFilteredStats(
    val staticStats: JumpTypeSpecificStats,
    val dynamicStats: JumpTypeSpecificStats,
    val comparison: JumpTypeComparison
)

data class JumpTypeSpecificStats(
    val jumpType: JumpType,
    val totalSessions: Int,
    val bestHeight: Double,
    val averageHeight: Double,
    val bestSpikeReach: Double,
    val averageSpikeReach: Double,
    val last7DaysSessions: Int,
    val last30DaysSessions: Int,
    val firstSessionDate: LocalDateTime?,
    val lastSessionDate: LocalDateTime?
)

data class JumpTypeComparison(
    val heightDifferencePercent: Double, // % difference between static and dynamic
    val spikeReachDifferencePercent: Double, // % difference between static and dynamic spike reach
    val preferredJumpType: JumpType?, // Jump type with more sessions
    val sessionRatio: Double // Ratio of static to dynamic sessions
)

// Combined Surface + Jump Type statistics
data class CombinedFilteredStats(
    val hardFloorStatic: PerformanceStats,
    val hardFloorDynamic: PerformanceStats,
    val sandStatic: PerformanceStats,
    val sandDynamic: PerformanceStats,
    val comparisons: CombinedComparisons
)

data class PerformanceStats(
    val surface: SurfaceType,
    val jumpType: JumpType,
    val totalSessions: Int,
    val bestHeight: Double,
    val averageHeight: Double,
    val bestSpikeReach: Double,
    val averageSpikeReach: Double,
    val improvementPercent: Double, // vs previous period
    val consistencyScore: Double,
    val firstSessionDate: LocalDateTime?,
    val lastSessionDate: LocalDateTime?
)

data class CombinedComparisons(
    val bestCombination: CombinationResult?, // Which surface+jump type combo performs best
    val surfaceEffectOnJumpTypes: SurfaceEffectAnalysis,
    val jumpTypeEffectOnSurfaces: JumpTypeEffectAnalysis
)

data class CombinationResult(
    val surface: SurfaceType,
    val jumpType: JumpType,
    val averageHeight: Double,
    val sessionCount: Int
)

data class SurfaceEffectAnalysis(
    val staticJumpDifference: Double, // % difference hard floor vs sand for static jumps
    val dynamicJumpDifference: Double // % difference hard floor vs sand for dynamic jumps
)

data class JumpTypeEffectAnalysis(
    val hardFloorDifference: Double, // % difference static vs dynamic on hard floor
    val sandDifference: Double // % difference static vs dynamic on sand
)