package com.watxaut.myjumpapp.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watxaut.myjumpapp.data.repository.StatisticsRepository
import com.watxaut.myjumpapp.domain.statistics.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatisticsUiState(
    val isLoading: Boolean = true,
    val userStatistics: UserStatistics? = null,
    val dashboardStats: DashboardStats? = null,
    val selectedTimePeriod: TimePeriod = TimePeriod.LAST_7_DAYS,
    val selectedStatisticType: StatisticType = StatisticType.HEIGHT,
    val error: String? = null
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val statisticsRepository: StatisticsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    private var currentUserId: String? = null

    fun loadUserStatistics(userId: String) {
        if (currentUserId == userId && _uiState.value.userStatistics != null) {
            return // Already loaded for this user
        }
        
        currentUserId = userId
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                // Load both user statistics and dashboard stats
                val userStats = statisticsRepository.getUserStatistics(userId)
                val dashboardStats = statisticsRepository.getDashboardStats(userId)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userStatistics = userStats,
                        dashboardStats = dashboardStats,
                        error = if (userStats == null) "No statistics available" else null
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load statistics"
                    )
                }
            }
        }
    }

    fun observeUserStatistics(userId: String) {
        currentUserId = userId
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                statisticsRepository.getUserStatisticsFlow(userId).collect { userStats ->
                    val dashboardStats = statisticsRepository.getDashboardStats(userId)
                    
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userStatistics = userStats,
                            dashboardStats = dashboardStats,
                            error = if (userStats == null) "No statistics available" else null
                        )
                    }
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to observe statistics"
                    )
                }
            }
        }
    }

    fun setTimePeriod(period: TimePeriod) {
        _uiState.update { it.copy(selectedTimePeriod = period) }
    }

    fun setStatisticType(type: StatisticType) {
        _uiState.update { it.copy(selectedStatisticType = type) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun refresh() {
        currentUserId?.let { userId ->
            loadUserStatistics(userId)
        }
    }

    // Helper functions for UI
    fun getFilteredProgressData(): List<HeightDataPoint>? {
        val progressStats = _uiState.value.userStatistics?.progressStats ?: return null
        val period = _uiState.value.selectedTimePeriod
        
        return when (period) {
            TimePeriod.LAST_7_DAYS -> {
                val sevenDaysAgo = java.time.LocalDate.now().minusDays(7)
                progressStats.heightProgression.filter { !it.date.isBefore(sevenDaysAgo) }
            }
            TimePeriod.LAST_30_DAYS -> {
                val thirtyDaysAgo = java.time.LocalDate.now().minusDays(30)
                progressStats.heightProgression.filter { !it.date.isBefore(thirtyDaysAgo) }
            }
            TimePeriod.LAST_90_DAYS -> {
                val ninetyDaysAgo = java.time.LocalDate.now().minusDays(90)
                progressStats.heightProgression.filter { !it.date.isBefore(ninetyDaysAgo) }
            }
            TimePeriod.THIS_WEEK -> {
                val weekStart = java.time.LocalDate.now().minusDays(
                    java.time.LocalDate.now().dayOfWeek.value.toLong() - 1
                )
                progressStats.heightProgression.filter { !it.date.isBefore(weekStart) }
            }
            TimePeriod.THIS_MONTH -> {
                val monthStart = java.time.LocalDate.now().withDayOfMonth(1)
                progressStats.heightProgression.filter { !it.date.isBefore(monthStart) }
            }
            TimePeriod.THIS_YEAR -> {
                val yearStart = java.time.LocalDate.now().withDayOfYear(1)
                progressStats.heightProgression.filter { !it.date.isBefore(yearStart) }
            }
            TimePeriod.TODAY -> {
                val today = java.time.LocalDate.now()
                progressStats.heightProgression.filter { it.date == today }
            }
            TimePeriod.ALL_TIME -> progressStats.heightProgression
        }
    }

    fun getFilteredVolumeData(): List<VolumeDataPoint>? {
        val progressStats = _uiState.value.userStatistics?.progressStats ?: return null
        val period = _uiState.value.selectedTimePeriod
        
        return when (period) {
            TimePeriod.LAST_7_DAYS -> {
                val sevenDaysAgo = java.time.LocalDate.now().minusDays(7)
                progressStats.volumeProgression.filter { !it.date.isBefore(sevenDaysAgo) }
            }
            TimePeriod.LAST_30_DAYS -> {
                val thirtyDaysAgo = java.time.LocalDate.now().minusDays(30)
                progressStats.volumeProgression.filter { !it.date.isBefore(thirtyDaysAgo) }
            }
            TimePeriod.LAST_90_DAYS -> {
                val ninetyDaysAgo = java.time.LocalDate.now().minusDays(90)
                progressStats.volumeProgression.filter { !it.date.isBefore(ninetyDaysAgo) }
            }
            TimePeriod.THIS_WEEK -> {
                val weekStart = java.time.LocalDate.now().minusDays(
                    java.time.LocalDate.now().dayOfWeek.value.toLong() - 1
                )
                progressStats.volumeProgression.filter { !it.date.isBefore(weekStart) }
            }
            TimePeriod.THIS_MONTH -> {
                val monthStart = java.time.LocalDate.now().withDayOfMonth(1)
                progressStats.volumeProgression.filter { !it.date.isBefore(monthStart) }
            }
            TimePeriod.THIS_YEAR -> {
                val yearStart = java.time.LocalDate.now().withDayOfYear(1)
                progressStats.volumeProgression.filter { !it.date.isBefore(yearStart) }
            }
            TimePeriod.TODAY -> {
                val today = java.time.LocalDate.now()
                progressStats.volumeProgression.filter { it.date == today }
            }
            TimePeriod.ALL_TIME -> progressStats.volumeProgression
        }
    }

    fun getPeriodStats(): PeriodStats? {
        val recentStats = _uiState.value.userStatistics?.recentStats ?: return null
        
        return when (_uiState.value.selectedTimePeriod) {
            TimePeriod.LAST_7_DAYS -> recentStats.last7Days
            TimePeriod.LAST_30_DAYS -> recentStats.last30Days
            TimePeriod.THIS_WEEK -> recentStats.thisWeek
            TimePeriod.THIS_MONTH -> recentStats.thisMonth
            else -> recentStats.last7Days // Default fallback
        }
    }

    fun getAchievedMilestones(): List<Milestone> {
        return _uiState.value.userStatistics?.achievementStats?.milestones?.filter { it.isAchieved } 
            ?: emptyList()
    }

    fun getUpcomingMilestones(): List<Milestone> {
        return _uiState.value.userStatistics?.achievementStats?.milestones?.filter { !it.isAchieved }
            ?.sortedBy { it.targetValue }
            ?.take(3)
            ?: emptyList()
    }
}