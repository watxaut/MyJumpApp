package com.watxaut.myjumpapp.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.watxaut.myjumpapp.domain.statistics.*
import com.watxaut.myjumpapp.domain.jump.SurfaceType
import com.watxaut.myjumpapp.presentation.viewmodels.StatisticsViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    userId: String?,
    onNavigateBack: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(userId) {
        userId?.let { id ->
            viewModel.loadUserStatistics(id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            ErrorView(
                message = uiState.error!!,
                onRetry = { viewModel.refresh() },
                modifier = Modifier.padding(paddingValues)
            )
        } else if (uiState.userStatistics != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Tab Row
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { 
                            Text(
                                text = "Overview",
                                maxLines = 1,
                                style = MaterialTheme.typography.labelMedium
                            ) 
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { 
                            Text(
                                text = "Progress",
                                maxLines = 1,
                                style = MaterialTheme.typography.labelMedium
                            ) 
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { 
                            Text(
                                text = "Records",
                                maxLines = 1,
                                style = MaterialTheme.typography.labelMedium
                            ) 
                        }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { 
                            Text(
                                text = "Surfaces",
                                maxLines = 1,
                                style = MaterialTheme.typography.labelMedium
                            ) 
                        }
                    )
                    Tab(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        text = { 
                            Text(
                                text = "Achievements",
                                maxLines = 1,
                                style = MaterialTheme.typography.labelMedium
                            ) 
                        }
                    )
                }

                // Tab Content
                when (selectedTab) {
                    0 -> OverviewTab(
                        statistics = uiState.userStatistics!!,
                        dashboardStats = uiState.dashboardStats
                    )
                    1 -> ProgressTab(
                        statistics = uiState.userStatistics!!,
                        viewModel = viewModel
                    )
                    2 -> RecordsTab(
                        statistics = uiState.userStatistics!!
                    )
                    3 -> SurfacesTab(
                        statistics = uiState.userStatistics!!
                    )
                    4 -> AchievementsTab(
                        statistics = uiState.userStatistics!!,
                        viewModel = viewModel
                    )
                }
            }
        } else {
            EmptyStatisticsView(
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun OverviewTab(
    statistics: UserStatistics,
    dashboardStats: DashboardStats?
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick Stats Cards
        item {
            Text(
                text = "Quick Stats",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickStatCard(
                    title = "Total Jumps",
                    value = "${statistics.overallStats.totalJumps}",
                    icon = Icons.Default.Home,
                    modifier = Modifier.weight(1f)
                )
                QuickStatCard(
                    title = "Personal Best",
                    value = "${String.format("%.1f", statistics.overallStats.bestJumpHeight)}cm",
                    icon = Icons.Default.Person,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickStatCard(
                    title = "Average Height",
                    value = "${String.format("%.1f", statistics.overallStats.averageJumpHeight)}cm",
                    icon = Icons.Default.Info,
                    modifier = Modifier.weight(1f)
                )
                QuickStatCard(
                    title = "Sessions",
                    value = "${statistics.overallStats.totalSessions}",
                    icon = Icons.Default.Phone,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Today's Stats
        dashboardStats?.let { dashboard ->
            item {
                Text(
                    text = "Today",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                TodayStatsCard(todayStats = dashboard.todayStats)
            }
        }

        // Recent Activity
        item {
            Text(
                text = "Recent Activity",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Card {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    RecentStatsRow(
                        label = "Last 7 Days",
                        sessionCount = statistics.recentStats.last7Days.sessionCount,
                        bestHeight = statistics.recentStats.last7Days.bestJumpHeight,
                        improvement = statistics.recentStats.last7Days.improvement
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    RecentStatsRow(
                        label = "Last 30 Days",
                        sessionCount = statistics.recentStats.last30Days.sessionCount,
                        bestHeight = statistics.recentStats.last30Days.bestJumpHeight,
                        improvement = statistics.recentStats.last30Days.improvement
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressTab(
    statistics: UserStatistics,
    viewModel: StatisticsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Time Period Selector
        item {
            Text(
                text = "Time Period",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(TimePeriod.values()) { period ->
                    FilterChip(
                        onClick = { viewModel.setTimePeriod(period) },
                        label = { Text(period.displayName()) },
                        selected = uiState.selectedTimePeriod == period
                    )
                }
            }
        }

        // Height Progress Chart
        item {
            val heightData = viewModel.getFilteredProgressData()
            if (heightData != null && heightData.isNotEmpty()) {
                Text(
                    text = "Height Progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Average jump height over time",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Simple chart representation (you can replace with actual chart library)
                        SimpleHeightChart(heightData)
                    }
                }
            }
        }

        // Volume Progress
        item {
            val volumeData = viewModel.getFilteredVolumeData()
            if (volumeData != null && volumeData.isNotEmpty()) {
                Text(
                    text = "Volume Progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Jump count over time",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        SimpleVolumeChart(volumeData)
                    }
                }
            }
        }

        // Period Stats
        item {
            val periodStats = viewModel.getPeriodStats()
            if (periodStats != null) {
                PeriodStatsCard(periodStats, uiState.selectedTimePeriod)
            }
        }
    }
}

@Composable
private fun RecordsTab(
    statistics: UserStatistics
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Personal Records",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        // Personal Records
        item {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    val hasAnyRecords = statistics.achievementStats.personalRecords.highestJump != null ||
                            statistics.achievementStats.personalRecords.longestFlightTime != null ||
                            statistics.achievementStats.personalRecords.mostJumpsInDay != null
                    
                    if (hasAnyRecords) {
                        statistics.achievementStats.personalRecords.highestJump?.let { record ->
                            PersonalRecordItem(
                                title = "Highest Jump",
                                value = "${String.format("%.1f", record.height)}cm",
                                date = record.date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                                icon = Icons.Default.KeyboardArrowUp
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        
                        statistics.achievementStats.personalRecords.longestFlightTime?.let { record ->
                            PersonalRecordItem(
                                title = "Longest Flight Time",
                                value = "${record.flightTime ?: 0}ms",
                                date = record.date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                                icon = Icons.Default.Phone
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        
                        // Removed "Most Jumps in Session" and "Most Jumps in Day" since each session = 1 jump
                        statistics.achievementStats.personalRecords.mostJumpsInDay?.let { record ->
                            PersonalRecordItem(
                                title = "Most Sessions in Day",
                                value = "${record.jumpCount} sessions",
                                date = record.date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                                icon = Icons.Default.DateRange
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No records yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Complete your first jump session to see your personal records!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Streak Information
        item {
            Text(
                text = "Streaks",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Card {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    StreakItem(
                        title = "Current Streak",
                        value = "${statistics.achievementStats.streaks.currentStreak} days",
                        icon = Icons.Default.Favorite
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    StreakItem(
                        title = "Longest Streak",
                        value = "${statistics.achievementStats.streaks.longestStreak} days",
                        icon = Icons.Default.Star
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementsTab(
    statistics: UserStatistics,
    viewModel: StatisticsViewModel
) {
    val achievedMilestones = viewModel.getAchievedMilestones()
    val upcomingMilestones = viewModel.getUpcomingMilestones()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Achieved Milestones
        item {
            Text(
                text = "Achievements Unlocked",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        items(achievedMilestones) { milestone ->
            MilestoneCard(milestone = milestone, isAchieved = true)
        }

        if (achievedMilestones.isEmpty()) {
            item {
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No achievements yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Keep jumping to unlock your first milestone!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Upcoming Milestones
        if (upcomingMilestones.isNotEmpty()) {
            item {
                Text(
                    text = "Next Goals",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            items(upcomingMilestones) { milestone ->
                MilestoneCard(milestone = milestone, isAchieved = false)
            }
        }
    }
}

// Helper Composables
@Composable
private fun QuickStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TodayStatsCard(todayStats: DayStats) {
    Card {
        if (todayStats.jumpCount > 0) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "${todayStats.jumpCount}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Sessions Today",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${String.format("%.1f", todayStats.bestJumpHeight)}cm",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Best Today",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No activity today",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Start jumping to see your daily progress!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun RecentStatsRow(
    label: String,
    sessionCount: Int,
    bestHeight: Double,
    improvement: Double
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (improvement != 0.0) {
                    val isImprovement = improvement > 0
                    Icon(
                        imageVector = if (isImprovement) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = if (isImprovement) Color(0xFF4CAF50) else Color(0xFFF44336),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${String.format("%.1f", kotlin.math.abs(improvement))}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isImprovement) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        if (sessionCount > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$sessionCount sessions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${String.format("%.1f", bestHeight)}cm best",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                text = "No activity in this period",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SimpleHeightChart(data: List<HeightDataPoint>) {
    // Simple visual representation - you can replace with actual chart library
    val maxHeight = data.maxOfOrNull { it.bestHeight } ?: 0.0
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        data.takeLast(7).forEach { dataPoint ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(((dataPoint.averageHeight / maxHeight) * 80).dp)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${dataPoint.date.dayOfMonth}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SimpleVolumeChart(data: List<VolumeDataPoint>) {
    val maxJumps = data.maxOfOrNull { it.jumpCount } ?: 0
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        data.takeLast(7).forEach { dataPoint ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(((dataPoint.jumpCount.toDouble() / maxJumps) * 80).dp)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(MaterialTheme.colorScheme.secondary)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${dataPoint.date.dayOfMonth}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PeriodStatsCard(
    periodStats: PeriodStats,
    period: TimePeriod
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "${period.displayName()} Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${periodStats.jumpCount}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Total Sessions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${String.format("%.1f", periodStats.averageJumpHeight)}cm",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Average Height",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Consistency: ${String.format("%.0f", periodStats.consistencyScore)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (periodStats.improvement != 0.0) {
                    val isImprovement = periodStats.improvement > 0
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isImprovement) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = if (isImprovement) Color(0xFF4CAF50) else Color(0xFFF44336),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${String.format("%.1f", kotlin.math.abs(periodStats.improvement))}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isImprovement) Color(0xFF4CAF50) else Color(0xFFF44336)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonalRecordItem(
    title: String,
    value: String,
    date: String,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun StreakItem(
    title: String,
    value: String,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun MilestoneCard(
    milestone: Milestone,
    isAchieved: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isAchieved) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isAchieved) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isAchieved) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = milestone.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = milestone.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (!isAchieved) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = (milestone.currentValue / milestone.targetValue).toFloat().coerceIn(0f, 1f),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${String.format("%.1f", milestone.currentValue)} / ${String.format("%.1f", milestone.targetValue)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    milestone.achievedDate?.let { date ->
                        Text(
                            text = "Achieved: ${date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Error",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun EmptyStatisticsView(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "No Statistics Yet",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Start jumping to see your progress and achievements!",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Extension function for TimePeriod display names
private fun TimePeriod.displayName(): String {
    return when (this) {
        TimePeriod.TODAY -> "Today"
        TimePeriod.THIS_WEEK -> "This Week"
        TimePeriod.THIS_MONTH -> "This Month"
        TimePeriod.LAST_7_DAYS -> "Last 7 Days"
        TimePeriod.LAST_30_DAYS -> "Last 30 Days"
        TimePeriod.LAST_90_DAYS -> "Last 90 Days"
        TimePeriod.THIS_YEAR -> "This Year"
        TimePeriod.ALL_TIME -> "All Time"
    }
}

@Composable
private fun SurfacesTab(
    statistics: UserStatistics
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Surface Comparison Header
        item {
            Text(
                text = "Performance by Surface",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        // Hard Floor vs Sand Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SurfaceStatsCard(
                    surfaceStats = statistics.surfaceStats.hardFloorStats,
                    modifier = Modifier.weight(1f)
                )
                SurfaceStatsCard(
                    surfaceStats = statistics.surfaceStats.sandStats,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // Comparison Section
        item {
            ComparisonCard(
                comparison = statistics.surfaceStats.comparison
            )
        }
        
        // Detailed Performance Section
        item {
            Text(
                text = "Detailed Performance",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        item {
            DetailedSurfaceComparison(
                hardFloorStats = statistics.surfaceStats.hardFloorStats,
                sandStats = statistics.surfaceStats.sandStats
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SurfaceStatsCard(
    surfaceStats: SurfaceSpecificStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (surfaceStats.surfaceType == SurfaceType.HARD_FLOOR) 
                MaterialTheme.colorScheme.primaryContainer
            else 
                MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Surface Icon and Name
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = surfaceStats.surfaceType.icon,
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = surfaceStats.surfaceType.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Best Height
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${String.format("%.1f", surfaceStats.bestHeight)} cm",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Best Height",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Sessions Count
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${surfaceStats.totalSessions}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Sessions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Average Height
            if (surfaceStats.averageHeight > 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${String.format("%.1f", surfaceStats.averageHeight)} cm",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Average",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComparisonCard(
    comparison: SurfaceComparison
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Performance Comparison",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Height Difference
            if (comparison.heightDifferencePercent != 0.0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Height Difference:",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "${String.format("%.1f", kotlin.math.abs(comparison.heightDifferencePercent))}% ${if (comparison.heightDifferencePercent > 0) "higher on hard floor" else "higher on sand"}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Preferred Surface
            comparison.preferredSurface?.let { preferred ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Most Used Surface:",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = preferred.icon)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = preferred.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Session Ratio
            if (comparison.sessionRatio.isFinite() && comparison.sessionRatio > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Session Ratio:",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "${String.format("%.1f", comparison.sessionRatio)}:1 (Hard Floor:Sand)",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailedSurfaceComparison(
    hardFloorStats: SurfaceSpecificStats,
    sandStats: SurfaceSpecificStats
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DetailedComparisonRow(
            label = "Best Height",
            hardFloorValue = "${String.format("%.1f", hardFloorStats.bestHeight)} cm",
            sandValue = "${String.format("%.1f", sandStats.bestHeight)} cm"
        )
        
        DetailedComparisonRow(
            label = "Average Height", 
            hardFloorValue = "${String.format("%.1f", hardFloorStats.averageHeight)} cm",
            sandValue = "${String.format("%.1f", sandStats.averageHeight)} cm"
        )
        
        DetailedComparisonRow(
            label = "Total Sessions",
            hardFloorValue = "${hardFloorStats.totalSessions}",
            sandValue = "${sandStats.totalSessions}"
        )
        
        DetailedComparisonRow(
            label = "First Session",
            hardFloorValue = hardFloorStats.firstSessionDate?.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) ?: "None",
            sandValue = sandStats.firstSessionDate?.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) ?: "None"
        )
        
        DetailedComparisonRow(
            label = "Last Session", 
            hardFloorValue = hardFloorStats.lastSessionDate?.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) ?: "None",
            sandValue = sandStats.lastSessionDate?.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) ?: "None"
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailedComparisonRow(
    label: String,
    hardFloorValue: String,
    sandValue: String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            
            Row(
                modifier = Modifier.weight(2f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🏀",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = hardFloorValue,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🏖️",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = sandValue,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}