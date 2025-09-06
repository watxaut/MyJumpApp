package com.watxaut.myjumpapp.data.repository

import com.watxaut.myjumpapp.data.database.dao.JumpDao
import com.watxaut.myjumpapp.data.database.dao.JumpSessionDao
import com.watxaut.myjumpapp.data.database.dao.UserDao
import com.watxaut.myjumpapp.domain.statistics.*
import com.watxaut.myjumpapp.domain.jump.SurfaceType
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
            overallStats = calculateOverallStats(user, jumps, sessions),
            recentStats = calculateRecentStats(user, jumps, sessions),
            progressStats = calculateProgressStats(jumps, sessions),
            achievementStats = calculateAchievementStats(jumps, sessions),
            surfaceStats = calculateSurfaceStats(user, sessions)
        )
    }

    suspend fun getDashboardStats(userId: String): DashboardStats? {
        val jumps = jumpDao.getAllJumpsByUserId(userId)
        val sessions = jumpSessionDao.getSessionsByUserId(userId)
        
        val today = LocalDate.now()
        val weekStart = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        
        return DashboardStats(
            todayStats = calculateDayStatsFromSessions(sessions, today),
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
                    overallStats = calculateOverallStats(it, jumps, sessions),
                    recentStats = calculateRecentStats(it, jumps, sessions),
                    progressStats = calculateProgressStats(jumps, sessions),
                    achievementStats = calculateAchievementStats(jumps, sessions),
                    surfaceStats = calculateSurfaceStats(it, sessions)
                )
            }
        }
    }

    private fun calculateOverallStats(
        user: com.watxaut.myjumpapp.data.database.entities.User,
        jumps: List<com.watxaut.myjumpapp.data.database.entities.Jump>,
        sessions: List<com.watxaut.myjumpapp.data.database.entities.JumpSession>
    ): OverallStats {
        // Calculate stats from completed sessions (each session = 1 jump)
        val completedSessions = sessions.filter { it.isCompleted }
        
        if (completedSessions.isEmpty()) {
            return OverallStats(
                totalJumps = 0,
                totalSessions = sessions.size,
                bestJumpHeight = 0.0,
                averageJumpHeight = 0.0,
                bestSpikeReach = 0.0,
                averageSpikeReach = 0.0,
                totalFlightTime = 0L,
                averageFlightTime = 0L,
                firstJumpDate = null,
                lastJumpDate = null,
                activeDays = 0
            )
        }

        val activeDays = completedSessions.map { 
            LocalDate.ofEpochDay(it.startTime / (24 * 60 * 60 * 1000)) 
        }.distinct().size

        val heelToHandReach = user.heelToHandReachCm ?: 0.0
        
        return OverallStats(
            totalJumps = completedSessions.size, // Each completed session = 1 jump
            totalSessions = sessions.size,
            bestJumpHeight = completedSessions.maxOfOrNull { it.bestJumpHeight } ?: 0.0,
            averageJumpHeight = if (completedSessions.isNotEmpty()) {
                completedSessions.map { it.bestJumpHeight }.average()
            } else 0.0,
            bestSpikeReach = if (heelToHandReach > 0) {
                (completedSessions.maxOfOrNull { it.bestJumpHeight } ?: 0.0) + heelToHandReach
            } else 0.0,
            averageSpikeReach = if (completedSessions.isNotEmpty() && heelToHandReach > 0) {
                completedSessions.map { it.bestJumpHeight }.average() + heelToHandReach
            } else 0.0,
            totalFlightTime = 0L, // Flight time not tracked in simplified system
            averageFlightTime = 0L,
            firstJumpDate = completedSessions.minOfOrNull { 
                LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(it.startTime),
                    ZoneId.systemDefault()
                )
            },
            lastJumpDate = completedSessions.maxOfOrNull { 
                LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(it.startTime),
                    ZoneId.systemDefault()
                )
            },
            activeDays = activeDays
        )
    }

    private fun calculateRecentStats(
        user: com.watxaut.myjumpapp.data.database.entities.User,
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
                user,
                sessions.filter { it.startTime >= sevenDaysAgo },
                sessions.filter { it.startTime >= fourteenDaysAgo && it.startTime < sevenDaysAgo }
            ),
            last30Days = calculatePeriodStats(
                user,
                sessions.filter { it.startTime >= thirtyDaysAgo },
                sessions.filter { it.startTime >= sixtyDaysAgo && it.startTime < thirtyDaysAgo }
            ),
            thisWeek = calculatePeriodStats(
                user,
                sessions.filter {
                    val sessionDate = LocalDate.ofEpochDay(it.startTime / (24 * 60 * 60 * 1000))
                    !sessionDate.isBefore(weekStart)
                },
                emptyList()
            ),
            thisMonth = calculatePeriodStats(
                user,
                sessions.filter {
                    val sessionDate = LocalDate.ofEpochDay(it.startTime / (24 * 60 * 60 * 1000))
                    !sessionDate.isBefore(monthStart)
                },
                emptyList()
            )
        )
    }

    private fun calculatePeriodStats(
        user: com.watxaut.myjumpapp.data.database.entities.User,
        currentSessions: List<com.watxaut.myjumpapp.data.database.entities.JumpSession>,
        previousSessions: List<com.watxaut.myjumpapp.data.database.entities.JumpSession>
    ): PeriodStats {
        // Calculate stats from sessions with valid data (completed or with height data)
        val completedCurrentSessions = currentSessions.filter { it.isCompleted || it.bestJumpHeight > 0 }
        val completedPreviousSessions = previousSessions.filter { it.isCompleted || it.bestJumpHeight > 0 }
        
        val currentAvgHeight = if (completedCurrentSessions.isNotEmpty()) {
            completedCurrentSessions.map { it.bestJumpHeight }.average()
        } else 0.0
        
        val previousAvgHeight = if (completedPreviousSessions.isNotEmpty()) {
            completedPreviousSessions.map { it.bestJumpHeight }.average()
        } else 0.0
        
        val improvement = if (previousAvgHeight > 0) {
            ((currentAvgHeight - previousAvgHeight) / previousAvgHeight) * 100
        } else 0.0

        val consistencyScore = calculateConsistencyScoreFromSessions(completedCurrentSessions)
        
        val heelToHandReach = user.heelToHandReachCm ?: 0.0
        val bestSpikeReach = if (heelToHandReach > 0) {
            (completedCurrentSessions.maxOfOrNull { it.bestJumpHeight } ?: 0.0) + heelToHandReach
        } else 0.0
        
        val averageSpikeReach = if (completedCurrentSessions.isNotEmpty() && heelToHandReach > 0) {
            currentAvgHeight + heelToHandReach
        } else 0.0
        
        val previousAvgSpikeReach = if (completedPreviousSessions.isNotEmpty() && heelToHandReach > 0) {
            previousAvgHeight + heelToHandReach
        } else 0.0
        
        val spikeReachImprovement = if (previousAvgSpikeReach > 0) {
            ((averageSpikeReach - previousAvgSpikeReach) / previousAvgSpikeReach) * 100
        } else 0.0

        return PeriodStats(
            jumpCount = completedCurrentSessions.size, // Each completed session = 1 jump
            sessionCount = currentSessions.size,
            bestJumpHeight = completedCurrentSessions.maxOfOrNull { it.bestJumpHeight } ?: 0.0,
            averageJumpHeight = currentAvgHeight,
            bestSpikeReach = bestSpikeReach,
            averageSpikeReach = averageSpikeReach,
            totalFlightTime = 0L, // Flight time not tracked in simplified system
            improvement = improvement,
            spikeReachImprovement = spikeReachImprovement,
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

    private fun calculateConsistencyScoreFromSessions(sessions: List<com.watxaut.myjumpapp.data.database.entities.JumpSession>): Double {
        if (sessions.isEmpty()) return 0.0
        
        val heights = sessions.map { it.bestJumpHeight }
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
        // For now, we don't have user data in this context, so spike reach will be 0
        val heelToHandReach = 0.0
        val completedSessions = sessions.filter { it.isCompleted }
        val sessionsByDay = completedSessions.groupBy { 
            LocalDate.ofEpochDay(it.startTime / (24 * 60 * 60 * 1000))
        }

        val heightProgression = sessionsByDay.map { (date, daySessions) ->
            val avgHeight = daySessions.map { it.bestJumpHeight }.average()
            val bestHeight = daySessions.maxOf { it.bestJumpHeight }
            HeightDataPoint(
                date = date,
                averageHeight = avgHeight,
                bestHeight = bestHeight,
                averageSpikeReach = if (heelToHandReach > 0) avgHeight + heelToHandReach else 0.0,
                bestSpikeReach = if (heelToHandReach > 0) bestHeight + heelToHandReach else 0.0,
                jumpCount = daySessions.size // Each session = 1 jump
            )
        }.sortedBy { it.date }

        val volumeProgression = sessionsByDay.map { (date, daySessions) ->
            VolumeDataPoint(
                date = date,
                jumpCount = daySessions.size, // Each session = 1 jump
                sessionCount = daySessions.size,
                totalFlightTime = 0L // Flight time not tracked in simplified system
            )
        }.sortedBy { it.date }

        val consistencyProgression = generateDateRange(
            completedSessions.minOfOrNull { LocalDate.ofEpochDay(it.startTime / (24 * 60 * 60 * 1000)) } 
                ?: LocalDate.now(),
            LocalDate.now()
        ).map { date ->
            val daySessions = sessionsByDay[date] ?: emptyList()
            ConsistencyDataPoint(
                date = date,
                hasJumped = daySessions.isNotEmpty(),
                jumpCount = daySessions.size // Each session = 1 jump
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
        // Use completed sessions for records (each session = 1 jump)
        val completedSessions = sessions.filter { it.isCompleted }
        
        if (completedSessions.isEmpty()) {
            return PersonalRecords(null, null, null, null, null, null, null)
        }
        
        // Get heel to hand reach (simplified approach)
        val heelToHandReach = 0.0 // Would need user data here

        // Highest jump based on sessions (since each session = 1 jump)
        val highestJump = completedSessions.maxByOrNull { it.bestJumpHeight }?.let { session ->
            JumpRecord(
                jumpId = session.sessionId, // Use sessionId as jumpId
                height = session.bestJumpHeight,
                spikeReach = if (heelToHandReach > 0) session.bestJumpHeight + heelToHandReach else 0.0,
                flightTime = null, // Flight time not tracked in simplified system
                date = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(session.startTime),
                    ZoneId.systemDefault()
                ),
                sessionId = session.sessionId
            )
        }
        
        // Highest spike reach record
        val highestSpikeReach = if (heelToHandReach > 0) {
            completedSessions.maxByOrNull { it.bestJumpHeight }?.let { session ->
                JumpRecord(
                    jumpId = session.sessionId,
                    height = session.bestJumpHeight,
                    spikeReach = session.bestJumpHeight + heelToHandReach,
                    flightTime = null,
                    date = LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(session.startTime),
                        ZoneId.systemDefault()
                    ),
                    sessionId = session.sessionId
                )
            }
        } else null

        // For flight time, use jumps if available, otherwise skip
        val longestFlightTime = jumps.filter { it.flightTimeMs != null }
            .maxByOrNull { it.flightTimeMs!! }?.let { jump ->
                JumpRecord(
                    jumpId = jump.jumpId,
                    height = jump.heightCm,
                    spikeReach = if (heelToHandReach > 0) jump.heightCm + heelToHandReach else 0.0,
                    flightTime = jump.flightTimeMs,
                    date = LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(jump.timestamp),
                        ZoneId.systemDefault()
                    ),
                    sessionId = jump.sessionId
                )
            }

        // Most sessions in a day (since each session = 1 jump)
        val sessionsByDay = completedSessions.groupBy { 
            LocalDate.ofEpochDay(it.startTime / (24 * 60 * 60 * 1000))
        }
        val mostJumpsInDay = sessionsByDay.maxByOrNull { it.value.size }?.let { (date, daySessions) ->
            val bestHeight = daySessions.maxOf { it.bestJumpHeight }
            DayRecord(
                date = date,
                jumpCount = daySessions.size, // Each session = 1 jump
                sessionCount = daySessions.size,
                bestHeight = bestHeight,
                bestSpikeReach = if (heelToHandReach > 0) bestHeight + heelToHandReach else 0.0
            )
        }

        // Since each session = 1 jump, mostJumpsInSession doesn't make sense in simplified system
        // Instead, use best session height
        val bestSession = completedSessions.maxByOrNull { it.bestJumpHeight }?.let { session ->
            SessionRecord(
                sessionId = session.sessionId,
                value = session.bestJumpHeight,
                jumpCount = 1, // Each session = 1 jump
                date = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(session.startTime),
                    ZoneId.systemDefault()
                )
            )
        }
        
        // Best average spike reach in session
        val bestAverageSpikeReachInSession = if (heelToHandReach > 0) {
            completedSessions.maxByOrNull { it.bestJumpHeight }?.let { session ->
                SessionRecord(
                    sessionId = session.sessionId,
                    value = session.bestJumpHeight + heelToHandReach,
                    jumpCount = 1,
                    date = LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(session.startTime),
                        ZoneId.systemDefault()
                    )
                )
            }
        } else null

        return PersonalRecords(
            highestJump = highestJump,
            highestSpikeReach = highestSpikeReach,
            longestFlightTime = longestFlightTime,
            mostJumpsInSession = bestSession, // Repurposed as best session
            mostJumpsInDay = mostJumpsInDay,
            bestAverageHeightInSession = bestSession, // Same as best session since 1 jump per session
            bestAverageSpikeReachInSession = bestAverageSpikeReachInSession
        )
    }

    private fun calculateMilestones(
        jumps: List<com.watxaut.myjumpapp.data.database.entities.Jump>,
        sessions: List<com.watxaut.myjumpapp.data.database.entities.JumpSession>
    ): List<Milestone> {
        // Use completed sessions for accurate data (each session = 1 jump)
        val completedSessions = sessions.filter { it.isCompleted }
        val totalJumps = completedSessions.size // Each completed session = 1 jump
        val bestHeight = completedSessions.maxOfOrNull { it.bestJumpHeight } ?: 0.0

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
                        completedSessions.filter { it.bestJumpHeight >= target }
                            .minByOrNull { it.startTime }
                            ?.let { 
                                LocalDateTime.ofInstant(
                                    java.time.Instant.ofEpochMilli(it.startTime),
                                    ZoneId.systemDefault()
                                )
                            }
                    } else null,
                    category = MilestoneCategory.HEIGHT
                )
            )
        }

        // Volume milestones (number of completed sessions)
        val volumeTargets = listOf(1, 5, 10, 25, 50, 100, 250)
        volumeTargets.forEach { target ->
            milestones.add(
                Milestone(
                    id = "volume_$target",
                    title = "$target Sessions",
                    description = "Complete $target jump sessions",
                    targetValue = target.toDouble(),
                    currentValue = totalJumps.toDouble(),
                    isAchieved = totalJumps >= target,
                    achievedDate = if (totalJumps >= target) {
                        completedSessions.sortedBy { it.startTime }
                            .getOrNull(target - 1)
                            ?.let { 
                                LocalDateTime.ofInstant(
                                    java.time.Instant.ofEpochMilli(it.startTime),
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
        
        val bestHeight = dayJumps.maxOfOrNull { it.heightCm } ?: 0.0
        return DayStats(
            date = date,
            jumpCount = dayJumps.size,
            sessionCount = sessionIds.size,
            bestJumpHeight = bestHeight,
            bestSpikeReach = 0.0, // Would need user data for heel to hand reach
            totalFlightTime = dayJumps.mapNotNull { it.flightTimeMs }.sum()
        )
    }
    
    private fun calculateDayStatsFromSessions(
        sessions: List<com.watxaut.myjumpapp.data.database.entities.JumpSession>,
        date: LocalDate
    ): DayStats {
        val dayStart = date.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
        val dayEnd = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
        
        val daySessions = sessions.filter { 
            it.startTime >= dayStart && it.startTime < dayEnd && it.isCompleted
        }
        
        val bestHeight = daySessions.maxOfOrNull { it.bestJumpHeight } ?: 0.0
        return DayStats(
            date = date,
            jumpCount = daySessions.size, // Each session = 1 jump
            sessionCount = daySessions.size,
            bestJumpHeight = bestHeight,
            bestSpikeReach = 0.0, // Would need user data for heel to hand reach
            totalFlightTime = 0L // Flight time not tracked in simplified system
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

        val bestHeight = weekJumps.maxOfOrNull { it.heightCm } ?: 0.0
        val avgHeight = if (weekJumps.isNotEmpty()) weekJumps.map { it.heightCm }.average() else 0.0
        return WeekStats(
            weekStart = weekStart,
            jumpCount = weekJumps.size,
            sessionCount = weekSessions.size,
            activeDays = activeDays,
            bestJumpHeight = bestHeight,
            averageJumpHeight = avgHeight,
            bestSpikeReach = 0.0, // Would need user data for heel to hand reach
            averageSpikeReach = 0.0
        )
    }

    private fun calculateQuickStats(
        jumps: List<com.watxaut.myjumpapp.data.database.entities.Jump>,
        sessions: List<com.watxaut.myjumpapp.data.database.entities.JumpSession>
    ): QuickStats {
        val completedSessions = sessions.filter { it.isCompleted }
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
        val last7DaysSessions = completedSessions.filter { it.startTime >= sevenDaysAgo }
        
        val sessionDates = completedSessions.map { 
            LocalDate.ofEpochDay(it.startTime / (24 * 60 * 60 * 1000))
        }.distinct().sorted()
        
        val currentStreak = calculateCurrentDayStreak(sessionDates)

        return QuickStats(
            totalJumps = completedSessions.size, // Each completed session = 1 jump
            currentStreak = currentStreak,
            personalBest = completedSessions.maxOfOrNull { it.bestJumpHeight } ?: 0.0,
            personalBestSpikeReach = 0.0, // Would need user data for heel to hand reach
            last7DaysJumps = last7DaysSessions.size
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
                    bestSpikeReach = 0.0, // Would need user data for heel to hand reach
                    averageSpikeReach = 0.0,
                    duration = duration,
                    isCompleted = session.isCompleted
                )
            }
    }
    
    private fun calculateSurfaceStats(
        user: com.watxaut.myjumpapp.data.database.entities.User,
        sessions: List<com.watxaut.myjumpapp.data.database.entities.JumpSession>
    ): SurfaceFilteredStats {
        val hardFloorSessions = sessions.filter { it.surfaceType == SurfaceType.HARD_FLOOR }
        val sandSessions = sessions.filter { it.surfaceType == SurfaceType.SAND }
        
        val heelToHandReach = user.heelToHandReachCm ?: 0.0
        val hardFloorAvgHeight = if (hardFloorSessions.isNotEmpty()) {
            hardFloorSessions.map { it.bestJumpHeight }.average()
        } else 0.0
        
        val hardFloorStats = SurfaceSpecificStats(
            surfaceType = SurfaceType.HARD_FLOOR,
            totalSessions = user.totalSessionsHardFloor,
            bestHeight = user.bestJumpHeightHardFloor,
            averageHeight = hardFloorAvgHeight,
            bestSpikeReach = if (heelToHandReach > 0) user.bestJumpHeightHardFloor + heelToHandReach else 0.0,
            averageSpikeReach = if (heelToHandReach > 0) hardFloorAvgHeight + heelToHandReach else 0.0,
            last7DaysSessions = 0, // TODO: Calculate from actual sessions
            last30DaysSessions = 0, // TODO: Calculate from actual sessions
            firstSessionDate = hardFloorSessions.minByOrNull { it.startTime }?.let {
                LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(it.startTime), ZoneId.systemDefault())
            },
            lastSessionDate = hardFloorSessions.maxByOrNull { it.startTime }?.let {
                LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(it.startTime), ZoneId.systemDefault())
            }
        )
        
        val sandAvgHeight = if (sandSessions.isNotEmpty()) {
            sandSessions.map { it.bestJumpHeight }.average()
        } else 0.0
        
        val sandStats = SurfaceSpecificStats(
            surfaceType = SurfaceType.SAND,
            totalSessions = user.totalSessionsSand,
            bestHeight = user.bestJumpHeightSand,
            averageHeight = sandAvgHeight,
            bestSpikeReach = if (heelToHandReach > 0) user.bestJumpHeightSand + heelToHandReach else 0.0,
            averageSpikeReach = if (heelToHandReach > 0) sandAvgHeight + heelToHandReach else 0.0,
            last7DaysSessions = 0, // TODO: Calculate from actual sessions
            last30DaysSessions = 0, // TODO: Calculate from actual sessions
            firstSessionDate = sandSessions.minByOrNull { it.startTime }?.let {
                LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(it.startTime), ZoneId.systemDefault())
            },
            lastSessionDate = sandSessions.maxByOrNull { it.startTime }?.let {
                LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(it.startTime), ZoneId.systemDefault())
            }
        )
        
        val comparison = SurfaceComparison(
            heightDifferencePercent = if (hardFloorStats.bestHeight > 0 && sandStats.bestHeight > 0) {
                ((hardFloorStats.bestHeight - sandStats.bestHeight) / hardFloorStats.bestHeight) * 100
            } else 0.0,
            spikeReachDifferencePercent = if (hardFloorStats.bestSpikeReach > 0 && sandStats.bestSpikeReach > 0) {
                ((hardFloorStats.bestSpikeReach - sandStats.bestSpikeReach) / hardFloorStats.bestSpikeReach) * 100
            } else 0.0,
            preferredSurface = when {
                hardFloorStats.totalSessions > sandStats.totalSessions -> SurfaceType.HARD_FLOOR
                sandStats.totalSessions > hardFloorStats.totalSessions -> SurfaceType.SAND
                else -> null
            },
            sessionRatio = if (sandStats.totalSessions > 0) {
                hardFloorStats.totalSessions.toDouble() / sandStats.totalSessions.toDouble()
            } else if (hardFloorStats.totalSessions > 0) Double.POSITIVE_INFINITY else 0.0
        )
        
        return SurfaceFilteredStats(
            hardFloorStats = hardFloorStats,
            sandStats = sandStats,
            comparison = comparison
        )
    }
}