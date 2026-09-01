package com.creatorlog.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creatorlog.app.ui.TrackerViewModel
import com.creatorlog.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyStreakScreen(
    viewModel: TrackerViewModel,
    onBack: () -> Unit
) {
    val stats by viewModel.statsState.collectAsState()

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("Daily Streak", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(DesignSystem.IconSizeMedium))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DesignSystem.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(DesignSystem.SectionSpacing)
        ) {
            Spacer(modifier = Modifier.height(DesignSystem.SpacingSmall))

            stats?.let { dashboardStats ->
                // 1. Current Streak Hero
                StreakHeroSection(streak = dashboardStats.currentStreak)

                // 2. Weekly Activity
                WeeklyActivitySection(weeklyData = dashboardStats.streakWeeklyActivity)

                // 3. Today's Status
                TodayStatusSection(isActiveToday = dashboardStats.streakWeeklyActivity.find { it.isToday }?.isActive ?: false)

                // 4. Next Milestone
                MilestoneProgressSection(
                    currentDays = dashboardStats.totalActiveDays,
                    nextMilestone = dashboardStats.nextMilestone,
                    progress = dashboardStats.milestoneProgress
                )

                // 5. Milestone Badges
                MilestoneBadgesSection(
                    achievedMilestones = dashboardStats.achievedMilestones
                )

                // 6. Streak Statistics
                StreakStatsSection(
                    longestStreak = dashboardStats.longestStreak,
                    totalActiveDays = dashboardStats.totalActiveDays
                )

                // 7. Motivational Card
                MotivationalSection()
            }

            Spacer(modifier = Modifier.height(116.dp))
        }
    }
}

@Composable
fun StreakHeroSection(streak: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // CreatorLog-themed Blue -> Purple Flame
            Icon(
                imageVector = Icons.Default.Whatshot,
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .graphicsLayer(alpha = 0.99f)
                    .drawWithCache {
                        onDrawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(PrimaryBlue, SecondaryPurple)
                                ),
                                blendMode = BlendMode.SrcIn
                            )
                        }
                    },
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        Text(
            text = "$streak Day Streak",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Text(
            text = "Keep the streak alive!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun WeeklyActivitySection(weeklyData: List<com.creatorlog.app.ui.StreakDayActivity>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DesignSystem.CornerRadiusLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(DesignSystem.BorderThickness, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "Weekly Activity",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weeklyData.forEach { day ->
                    WeeklyDayItem(day)
                }
            }
        }
    }
}

@Composable
fun WeeklyDayItem(day: com.creatorlog.app.ui.StreakDayActivity) {
    val sdf = SimpleDateFormat("d", Locale.getDefault())
    val dateStr = sdf.format(Date(day.date))
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = day.dayName,
            style = MaterialTheme.typography.labelSmall,
            color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    when {
                        day.isActive -> MaterialTheme.colorScheme.primary
                        day.isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
                .then(
                    if (day.isToday && !day.isActive) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (day.isActive) {
                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
            } else if (day.isToday) {
                Text(text = "⭐", fontSize = 14.sp)
            }
        }
        
        Text(
            text = dateStr,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TodayStatusSection(isActiveToday: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DesignSystem.CornerRadiusLarge),
        colors = CardDefaults.cardColors(
            containerColor = if (isActiveToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) 
                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(
            DesignSystem.BorderThickness, 
            if (isActiveToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) 
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isActiveToday) Icons.Default.CheckCircle else Icons.Default.Info,
                contentDescription = null,
                tint = if (isActiveToday) StatusPosted else MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (isActiveToday) "✓ Active Today" else "○ No Activity Today",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (isActiveToday) StatusPosted else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun MilestoneProgressSection(currentDays: Int, nextMilestone: Int, progress: Float) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text(
                text = "Next Milestone",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$currentDays / $nextMilestone Days",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
        
        Text(
            text = "${nextMilestone - currentDays} days to go",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MilestoneBadgesSection(achievedMilestones: Map<Int, Long>) {
    val milestones = listOf(7, 14, 30, 50, 100)
    
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Milestones",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            milestones.take(3).forEach { milestone ->
                MilestoneBadgeItem(
                    milestone = milestone,
                    isAchieved = achievedMilestones.containsKey(milestone),
                    achievementDate = achievedMilestones[milestone],
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(0.66f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            milestones.drop(3).forEach { milestone ->
                MilestoneBadgeItem(
                    milestone = milestone,
                    isAchieved = achievedMilestones.containsKey(milestone),
                    achievementDate = achievedMilestones[milestone],
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun MilestoneBadgeItem(milestone: Int, isAchieved: Boolean, achievementDate: Long?, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(DesignSystem.CornerRadiusMedium),
        colors = CardDefaults.cardColors(
            containerColor = if (isAchieved) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) 
                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(
            DesignSystem.BorderThickness, 
            if (isAchieved) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) 
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isAchieved) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) 
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = if (isAchieved) MaterialTheme.colorScheme.primary 
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Text(
                text = "$milestone Days",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            if (isAchieved && achievementDate != null) {
                val sdf = SimpleDateFormat("dd MMM yy", Locale.getDefault())
                Text(
                    text = sdf.format(Date(achievementDate)),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            } else if (!isAchieved) {
                Text(
                    text = "Locked",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun StreakStatsSection(longestStreak: Int, totalActiveDays: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DesignSystem.CardSpacing)
    ) {
        StatItemCard(
            label = "Longest Streak",
            value = "$longestStreak Days",
            icon = Icons.Default.Timeline,
            modifier = Modifier.weight(1f)
        )
        StatItemCard(
            label = "Total Active Days",
            value = "$totalActiveDays Days",
            icon = Icons.Default.CalendarToday,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatItemCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(DesignSystem.CornerRadiusLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(DesignSystem.BorderThickness, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun MotivationalSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DesignSystem.CornerRadiusLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Consistency is the key to growth.",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Create consistently and keep your streak alive!",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
