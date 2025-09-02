package com.watxaut.myjumpapp.data.repository

import com.watxaut.myjumpapp.data.database.dao.JumpDao
import com.watxaut.myjumpapp.data.database.dao.JumpSessionDao
import com.watxaut.myjumpapp.data.database.dao.UserDao
import com.watxaut.myjumpapp.domain.statistics.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class StatisticsRepository @Inject constructor(
    private val jumpDao: JumpDao,
    private val jumpSessionDao: JumpSessionDao,
    private val userDao: UserDao
) {

    suspend fun getUserStatistics(userId: String): UserStatistics? {
        val user = userDao.getUserById(userId) ?: return null
        val jumps = jumpDao.getAllJumpsByUserId(userId)
        val sessions = jumpSessionDao.getSessionsByUserId(userId)
        
        return UserStatistics(
            userId = userId,
            userName = user.userName,
            overallStats = calculateOverallStats(jumps, sessions),
            recentStats = calculateRecentStats(jumps, sessions),
            progressStats = calculateProgressStats(jumps, sessions),
            achievementStats = calculateAchievementStats(jumps, sessions)
        )
    }

    suspend fun getDashboardStats(userId: String): DashboardStats? {
        val jumps = jumpDao.getAllJumpsByUserId(userId)
        val sessions = jumpSessionDao.getSessionsByUserId(userId)
        
        val today = LocalDate.now()
        val weekStart = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        
        return DashboardStats(
            todayStats = calculateDayStats(jumps, today),
            weekStats = calculateWeekStats(jumps, sessions, weekStart),
            quickStats = calculateQuickStats(jumps, sessions),
            recentSessions = getRecentSessions(sessions, 5)
        )
    }

    fun getUserStatisticsFlow(userId: String): Flow<UserStatistics?> {
        return combine(
            jumpDao.getAllJumpsByUserIdFlow(userId),
            jumpSessionDao.getSessionsByUserIdFlow(userId),
            userDao.getUserByIdFlow(userId)
        ) { jumps, sessions, user ->
            user?.let {
                UserStatistics(
                    userId = userId,
                    userName = it.userName,
                    overallStats = calculateOverallStats(jumps, sessions),
                    recentStats = calculateRecentStats(jumps, sessions),
                    progressStats = calculateProgressStats(jumps, sessions),
                    achievementStats = calculateAchievementStats(jumps, sessions)
                )
            }
        }
    }

    private fun calculateOverallStats(
        jumps: List<com.watxaut.myjumpapp.data.database.entities.Jump>,
        sessions: List<com.watxaut.myjumpapp.data.database.entities.JumpSession>
    ): OverallStats {
        if (jumps.isEmpty()) {
            return OverallStats(
                totalJumps = 0,
                totalSessions = sessions.size,
                bestJumpHeight = 0.0,
                averageJumpHeight = 0.0,
                totalFlightTime = 0L,
                averageFlightTime = 0L,
                firstJumpDate = null,
                lastJumpDate = null,
                activeDays = 0
            )
        }

        val totalFlightTime = jumps.mapNotNull { it.flightTimeMs }.sum()
        val activeDays = jumps.map { 
            LocalDate.ofEpochDay(it.timestamp / (24 * 60 * 60 * 1000)) 
        }.distinct().size

        return OverallStats(
            totalJumps = jumps.size,
            totalSessions = sessions.size,
            bestJumpHeight = jumps.maxOfOrNull { it.heightCm } ?: 0.0,
            averageJumpHeight = jumps.map { it.heightCm }.average(),
            totalFlightTime = totalFlightTime,
            averageFlightTime = if (jumps.isNotEmpty()) totalFlightTime / jumps.size else 0L,
            firstJumpDate = jumps.minOfOrNull { 
                LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(it.timestamp),
                    ZoneId.systemDefault()
                )
            },
            lastJumpDate = jumps.maxOfOrNull { 
                LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(it.timestamp),
                    ZoneId.systemDefault()
                )
            },
            activeDays = activeDays
        )
    }

    private fun calculateRecentStats(
        jumps: List<com.watxaut.myjumpapp.data.database.entities.Jump>,
        sessions: List<com.watxaut.myjumpapp.data.database.entities.JumpSession>
    ): RecentStats {
        val now = System.currentTimeMillis()
        val sevenDaysAgo = now - (7 * 24 * 60 * 60 * 1000)
        val thirtyDaysAgo = now - (30 * 24 * 60 * 60 * 1000)
        val fourteenDaysAgo = now - (14 * 24 * 60 * 60 * 1000)
        val sixtyDaysAgo = now - (60 * 24 * 60 * 60 * 1000)

        val today = LocalDate.now()
        val weekStart = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        val monthStart = today.withDayOfMonth(1)

        return RecentStats(
            last7Days = calculatePeriodStats(
                jumps.filter { it.timestamp >= sevenDaysAgo },
                sessions.filter { it.startTime >= sevenDaysAgo },
                jumps.filter { it.timestamp >= fourteenDaysAgo && it.timestamp < sevenDaysAgo },
                sessions.filter { it.startTime >= fourteenDaysAgo && it.startTime < sevenDaysAgo }
            ),
            last30Days = calculatePeriodStats(
                jumps.filter { it.timestamp >= thirtyDaysAgo },
                sessions.filter { it.startTime >= thirtyDaysAgo },
                jumps.filter { it.timestamp >= sixtyDaysAgo && it.timestamp < thirtyDaysAgo },
                sessions.filter { it.startTime >= sixtyDaysAgo && it.startTime < thirtyDaysAgo }
            ),
            thisWeek = calculatePeriodStats(
                jumps.filter { 
                    val jumpDate = LocalDate.ofEpochDay(it.timestamp / (24 * 60 * 60 * 1000))
                    !jumpDate.isBefore(weekStart)
                },
                sessions.filter {
                    val sessionDate = LocalDate.ofEpochDay(it.startTime / (24 * 60 * 60 * 1000))
                    !sessionDate.isBefore(weekStart)
                },
                emptyList(),
                emptyList()
            ),
            thisMonth = calculatePeriodStats(
                jumps.filter { 
                    val jumpDate = LocalDate.ofEpochDay(it.timestamp / (24 * 60 * 60 * 1000))
                    !jumpDate.isBefore(monthStart)
                },
                sessions.filter {
                    val sessionDate = LocalDate.ofEpochDay(it.startTime / (24 * 60 * 60 * 1000))
                    !sessionDate.isBefore(monthStart)
                },
                emptyList(),
                emptyList()
            )
        )
    }

    private fun calculatePeriodStats(
        currentJumps: List<com.watxaut.myjumpapp.data.database.entities.Jump>,
        currentSessions: List<com.watxaut.myjumpapp.data.database.entities.JumpSession>,
        previousJumps: List<com.watxaut.myjumpapp.data.database.entities.Jump>,
        previousSessions: List<com.watxaut.myjumpapp.data.database.entities.JumpSession>
    ): PeriodStats {
        val currentAvgHeight = if (currentJumps.isNotEmpty()) currentJumps.map { it.heightCm }.average() else 0.0
        val previousAvgHeight = if (previousJumps.isNotEmpty()) previousJumps.map { it.heightCm }.average() else 0.0
        
        val improvement = if (previousAvgHeight > 0) {
            ((currentAvgHeight - previousAvgHeight) / previousAvgHeight) * 100
        } else 0.0

        val consistencyScore = calculateConsistencyScore(currentJumps)

        return PeriodStats(
            jumpCount = currentJumps.size,
            sessionCount = currentSessions.size,
            bestJumpHeight = currentJumps.maxOfOrNull { it.heightCm } ?: 0.0,
            averageJumpHeight = currentAvgHeight,
            totalFlightTime = currentJumps.mapNotNull { it.flightTimeMs }.sum(),
            improvement = improvement,
            consistencyScore = consistencyScore
        )
    }

    private fun calculateConsistencyScore(jumps: List<com.watxaut.myjumpapp.data.database.entities.Jump>): Double {
        if (jumps.isEmpty()) return 0.0
        
        val heights = jumps.map { it.heightCm }
        val mean = heights.average()
        val variance = heights.map { (it - mean) * (it - mean) }.average()
        val standardDeviation = kotlin.math.sqrt(variance)
        
        // Lower standard deviation relative to mean = higher consistency
        return if (mean > 0) {
            100.0 - ((standardDeviation / mean) * 100).coerceAtMost(100.0)
        } else 0.0
    }

    private fun calculateProgressStats(
        jumps: List<com.watxaut.myjumpapp.data.database.entities.Jump>,
        sessions: List<com.watxaut.myjumpapp.data.database.entities.JumpSession>
    ): ProgressStats {
        val jumpsByDay = jumps.groupBy { 
            LocalDate.ofEpochDay(it.timestamp / (24 * 60 * 60 * 1000))
        }
        
        val sessionsByDay = sessions.groupBy { 
            LocalDate.ofEpochDay(it.startTime / (24 * 60 * 60 * 1000))
        }

        val heightProgression = jumpsByDay.map { (date, dayJumps) ->
            HeightDataPoint(
                date = date,
                averageHeight = dayJumps.map { it.heightCm }.average(),
                bestHeight = dayJumps.maxOf { it.heightCm },
                jumpCount = dayJumps.size
            )
        }.sortedBy { it.date }

        val volumeProgression = jumpsByDay.map { (date, dayJumps) ->
            VolumeDataPoint(
                date = date,
                jumpCount = dayJumps.size,
                sessionCount = sessionsByDay[date]?.size ?: 0,
                totalFlightTime = dayJumps.mapNotNull { it.flightTimeMs }.sum()
            )
        }.sortedBy { it.date }

        val consistencyProgression = generateDateRange(
            jumps.minOfOrNull { LocalDate.ofEpochDay(it.timestamp / (24 * 60 * 60 * 1000)) } 
                ?: LocalDate.now(),
            LocalDate.now()
        ).map { date ->
            val dayJumps = jumpsByDay[date] ?: emptyList()
            ConsistencyDataPoint(
                date = date,
                hasJumped = dayJumps.isNotEmpty(),
                jumpCount = dayJumps.size
            )
        }

        return ProgressStats(
            heightProgression = heightProgression,
            volumeProgression = volumeProgression,
            consistencyProgression = consistencyProgression
        )
    }

    private fun generateDateRange(start: LocalDate, end: LocalDate): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var current = start
        while (!current.isAfter(end)) {
            dates.add(current)
            current = current.plusDays(1)
        }
        return dates
    }

    private fun calculateAchievementStats(
        jumps: List<com.watxaut.myjumpapp.data.database.entities.Jump>,
        sessions: List<com.watxaut.myjumpapp.data.database.entities.JumpSession>
    ): AchievementStats {
        val personalRecords = calculatePersonalRecords(jumps, sessions)
        val milestones = calculateMilestones(jumps, sessions)
        val streaks = calculateStreaks(jumps)

        return AchievementStats(
            personalRecords = personalRecords,
            milestones = milestones,
            streaks = streaks
        )
    }

    private fun calculatePersonalRecords(
        jumps: List<com.watxaut.myjumpapp.data.database.entities.Jump>,
        sessions: List<com.watxaut.myjumpapp.data.database.entities.JumpSession>
    ): PersonalRecords {
        if (jumps.isEmpty()) {
            return PersonalRecords(null, null, null, null, null)
        }

        val highestJump = jumps.maxByOrNull { it.heightCm }?.let { jump ->
            JumpRecord(
                jumpId = jump.jumpId,
                height = jump.heightCm,
                flightTime = jump.flightTimeMs,
                date = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(jump.timestamp),
                    ZoneId.systemDefault()
                ),
                sessionId = jump.sessionId
            )
        }

        val longestFlightTime = jumps.filter { it.flightTimeMs != null }
            .maxByOrNull { it.flightTimeMs!! }?.let { jump ->
                JumpRecord(
                    jumpId = jump.jumpId,
                    height = jump.heightCm,
                    flightTime = jump.flightTimeMs,
                    date = LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(jump.timestamp),
                        ZoneId.systemDefault()
                    ),
                    sessionId = jump.sessionId
                )
            }

        val mostJumpsInSession = sessions.maxByOrNull { it.totalJumps }?.let { session ->
            SessionRecord(
                sessionId = session.sessionId,
                value = session.totalJumps.toDouble(),
                jumpCount = session.totalJumps,
                date = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(session.startTime),
                    ZoneId.systemDefault()
                )
            )
        }

        val jumpsByDay = jumps.groupBy { 
            LocalDate.ofEpochDay(it.timestamp / (24 * 60 * 60 * 1000))
        }
        val mostJumpsInDay = jumpsByDay.maxByOrNull { it.value.size }?.let { (date, dayJumps) ->
            DayRecord(
                date = date,
                jumpCount = dayJumps.size,
                sessionCount = dayJumps.mapNotNull { it.sessionId }.distinct().size,
                bestHeight = dayJumps.maxOf { it.heightCm }
            )
        }

        val bestAverageHeightInSession = sessions.maxByOrNull { it.averageJumpHeight }?.let { session ->
            SessionRecord(
                sessionId = session.sessionId,
                value = session.averageJumpHeight,
                jumpCount = session.totalJumps,
                date = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(session.startTime),
                    ZoneId.systemDefault()
                )
            )
        }

        return PersonalRecords(
            highestJump = highestJump,
            longestFlightTime = longestFlightTime,
            mostJumpsInSession = mostJumpsInSession,
            mostJumpsInDay = mostJumpsInDay,
            bestAverageHeightInSession = bestAverageHeightInSession
        )
    }

    private fun calculateMilestones(
        jumps: List<com.watxaut.myjumpapp.data.database.entities.Jump>,
        sessions: List<com.watxaut.myjumpapp.data.database.entities.JumpSession>
    ): List<Milestone> {
        val totalJumps = jumps.size
        val bestHeight = jumps.maxOfOrNull { it.heightCm } ?: 0.0
        val totalSessions = sessions.size
        val totalFlightTime = jumps.mapNotNull { it.flightTimeMs }.sum()

        val milestones = mutableListOf<Milestone>()

        // Height milestones
        val heightTargets = listOf(30.0, 40.0, 50.0, 60.0, 70.0, 80.0)
        heightTargets.forEach { target ->
            milestones.add(
                Milestone(
                    id = "height_${target.toInt()}",
                    title = "${target.toInt()}cm Jump",
                    description = "Achieve a jump height of ${target.toInt()}cm",
                    targetValue = target,
                    currentValue = bestHeight,
                    isAchieved = bestHeight >= target,
                    achievedDate = if (bestHeight >= target) {
                        jumps.filter { it.heightCm >= target }
                            .minByOrNull { it.timestamp }
                            ?.let { 
                                LocalDateTime.ofInstant(
                                    java.time.Instant.ofEpochMilli(it.timestamp),
                                    ZoneId.systemDefault()
                                )
                            }
                    } else null,
                    category = MilestoneCategory.HEIGHT
                )
            )
        }

        // Volume milestones
        val volumeTargets = listOf(10, 25, 50, 100, 250, 500, 1000)
        volumeTargets.forEach { target ->
            milestones.add(
                Milestone(
                    id = "volume_$target",
                    title = "$target Jumps",
                    description = "Complete $target total jumps",
                    targetValue = target.toDouble(),
                    currentValue = totalJumps.toDouble(),
                    isAchieved = totalJumps >= target,
                    achievedDate = if (totalJumps >= target) {
                        jumps.sortedBy { it.timestamp }
                            .getOrNull(target - 1)
                            ?.let { 
                                LocalDateTime.ofInstant(
                                    java.time.Instant.ofEpochMilli(it.timestamp),
                                    ZoneId.systemDefault()
                                )
                            }
                    } else null,
                    category = MilestoneCategory.VOLUME
                )
            )
        }

        return milestones.sortedBy { it.targetValue }
    }

    private fun calculateStreaks(jumps: List<com.watxaut.myjumpapp.data.database.entities.Jump>): StreakStats {
        if (jumps.isEmpty()) {
            return StreakStats(0, 0, 0, 0, null)
        }

        val jumpDates = jumps.map { 
            LocalDate.ofEpochDay(it.timestamp / (24 * 60 * 60 * 1000))
        }.distinct().sorted()

        val currentStreak = calculateCurrentDayStreak(jumpDates)
        val longestStreak = calculateLongestDayStreak(jumpDates)
        
        // Calculate weekly streaks
        val jumpWeeks = jumpDates.map { 
            it.minusDays(it.dayOfWeek.value.toLong() - 1) // Get Monday of that week
        }.distinct().sorted()
        
        val currentWeeklyStreak = calculateCurrentWeekStreak(jumpWeeks)
        val longestWeeklyStreak = calculateLongestWeekStreak(jumpWeeks)

        return StreakStats(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            currentWeeklyStreak = currentWeeklyStreak,
            longestWeeklyStreak = longestWeeklyStreak,
            lastActiveDate = jumpDates.maxOrNull()
        )
    }

    private fun calculateCurrentDayStreak(jumpDates: List<LocalDate>): Int {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        
        // Check if there were jumps today or yesterday
        val hasJumpedRecently = jumpDates.contains(today) || jumpDates.contains(yesterday)
        if (!hasJumpedRecently) return 0

        var streak = 0
        var currentDate = if (jumpDates.contains(today)) today else yesterday
        
        while (jumpDates.contains(currentDate)) {
            streak++
            currentDate = currentDate.minusDays(1)
        }
        
        return streak
    }

    private fun calculateLongestDayStreak(jumpDates: List<LocalDate>): Int {
        if (jumpDates.isEmpty()) return 0
        
        var maxStreak = 1
        var currentStreak = 1
        
        for (i in 1 until jumpDates.size) {
            if (jumpDates[i] == jumpDates[i-1].plusDays(1)) {
                currentStreak++
                maxStreak = maxOf(maxStreak, currentStreak)
            } else {
                currentStreak = 1
            }
        }
        
        return maxStreak
    }

    private fun calculateCurrentWeekStreak(jumpWeeks: List<LocalDate>): Int {
        val thisWeek = LocalDate.now().minusDays(LocalDate.now().dayOfWeek.value.toLong() - 1)
        val lastWeek = thisWeek.minusDays(7)
        
        val hasJumpedThisWeek = jumpWeeks.contains(thisWeek) || jumpWeeks.contains(lastWeek)
        if (!hasJumpedThisWeek) return 0

        var streak = 0
        var currentWeek = if (jumpWeeks.contains(thisWeek)) thisWeek else lastWeek
        
        while (jumpWeeks.contains(currentWeek)) {
            streak++
            currentWeek = currentWeek.minusDays(7)
        }
        
        return streak
    }

    private fun calculateLongestWeekStreak(jumpWeeks: List<LocalDate>): Int {
        if (jumpWeeks.isEmpty()) return 0
        
        var maxStreak = 1
        var currentStreak = 1
        
        for (i in 1 until jumpWeeks.size) {
            if (jumpWeeks[i] == jumpWeeks[i-1].plusDays(7)) {
                currentStreak++
                maxStreak = maxOf(maxStreak, currentStreak)
            } else {
                currentStreak = 1
            }
        }
        
        return maxStreak
    }

    private fun calculateDayStats(
        jumps: List<com.watxaut.myjumpapp.data.database.entities.Jump>,
        date: LocalDate
    ): DayStats {
        val dayStart = date.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
        val dayEnd = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
        
        val dayJumps = jumps.filter { it.timestamp >= dayStart && it.timestamp < dayEnd }
        val sessionIds = dayJumps.mapNotNull { it.sessionId }.distinct()
        
        return DayStats(
            date = date,
            jumpCount = dayJumps.size,
            sessionCount = sessionIds.size,
            bestJumpHeight = dayJumps.maxOfOrNull { it.heightCm } ?: 0.0,
            totalFlightTime = dayJumps.mapNotNull { it.flightTimeMs }.sum()
        )
    }

    private fun calculateWeekStats(
        jumps: List<com.watxaut.myjumpapp.data.database.entities.Jump>,
        sessions: List<com.watxaut.myjumpapp.data.database.entities.JumpSession>,
        weekStart: LocalDate
    ): WeekStats {
        val weekStartMillis = weekStart.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
        val weekEndMillis = weekStart.plusDays(7).atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
        
        val weekJumps = jumps.filter { it.timestamp >= weekStartMillis && it.timestamp < weekEndMillis }
        val weekSessions = sessions.filter { it.startTime >= weekStartMillis && it.startTime < weekEndMillis }
        
        val activeDays = weekJumps.map { 
            LocalDate.ofEpochDay(it.timestamp / (24 * 60 * 60 * 1000))
        }.distinct().size

        return WeekStats(
            weekStart = weekStart,
            jumpCount = weekJumps.size,
            sessionCount = weekSessions.size,
            activeDays = activeDays,
            bestJumpHeight = weekJumps.maxOfOrNull { it.heightCm } ?: 0.0,
            averageJumpHeight = if (weekJumps.isNotEmpty()) weekJumps.map { it.heightCm }.average() else 0.0
        )
    }

    private fun calculateQuickStats(
        jumps: List<com.watxaut.myjumpapp.data.database.entities.Jump>,
        sessions: List<com.watxaut.myjumpapp.data.database.entities.JumpSession>
    ): QuickStats {
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
        val last7DaysJumps = jumps.filter { it.timestamp >= sevenDaysAgo }
        
        val jumpDates = jumps.map { 
            LocalDate.ofEpochDay(it.timestamp / (24 * 60 * 60 * 1000))
        }.distinct().sorted()
        
        val currentStreak = calculateCurrentDayStreak(jumpDates)

        return QuickStats(
            totalJumps = jumps.size,
            currentStreak = currentStreak,
            personalBest = jumps.maxOfOrNull { it.heightCm } ?: 0.0,
            last7DaysJumps = last7DaysJumps.size
        )
    }

    private fun getRecentSessions(
        sessions: List<com.watxaut.myjumpapp.data.database.entities.JumpSession>,
        limit: Int
    ): List<SessionSummary> {
        return sessions.sortedByDescending { it.startTime }
            .take(limit)
            .map { session ->
                val duration = if (session.endTime != null) {
                    session.endTime!! - session.startTime
                } else 0L

                SessionSummary(
                    sessionId = session.sessionId,
                    sessionName = session.sessionName,
                    date = LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(session.startTime),
                        ZoneId.systemDefault()
                    ),
                    jumpCount = session.totalJumps,
                    bestJumpHeight = session.bestJumpHeight,
                    averageJumpHeight = session.averageJumpHeight,
                    duration = duration,
                    isCompleted = session.isCompleted
                )
            }
    }
}