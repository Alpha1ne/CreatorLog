package com.voiceofmelody.songdailytracker.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voiceofmelody.songdailytracker.data.model.Reminder
import com.voiceofmelody.songdailytracker.data.model.SongPost
import com.voiceofmelody.songdailytracker.ui.SongStatus
import com.voiceofmelody.songdailytracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CreatorCalendar(
    songs: List<SongPost>,
    reminders: List<Reminder>,
    now: Long,
    onDateSelected: (Long, List<SongPost>, List<Reminder>) -> Unit,
    modifier: Modifier = Modifier
) {
    var calendar by rememberSaveable(saver = CalendarSaver) { mutableStateOf(Calendar.getInstance()) }
    val currentMonth = remember(calendar) {
        val c = calendar.clone() as Calendar
        c.set(Calendar.DAY_OF_MONTH, 1)
        c
    }

    val monthYearFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DesignSystem.CornerRadiusLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(DesignSystem.BorderThickness, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(DesignSystem.SpacingMedium)) {
            // Header with Gradient
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(DesignSystem.CornerRadiusMedium))
                    .background(Brush.linearGradient(listOf(CalendarHeaderStart, CalendarHeaderEnd)))
                    .padding(horizontal = DesignSystem.SpacingMedium, vertical = DesignSystem.SpacingSmall),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedContent(
                    targetState = currentMonth,
                    transitionSpec = {
                        if (targetState.after(initialState)) {
                            slideInHorizontally { width -> width } + fadeIn() togetherWith
                                    slideOutHorizontally { width -> -width } + fadeOut()
                        } else {
                            slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                    slideOutHorizontally { width -> width } + fadeOut()
                        }.using(SizeTransform(clip = false))
                    },
                    label = "month_transition"
                ) { targetMonth ->
                    Text(
                        text = monthYearFormat.format(targetMonth.time),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Row {
                    IconButton(onClick = {
                        val newCal = calendar.clone() as Calendar
                        newCal.add(Calendar.MONTH, -1)
                        calendar = newCal
                    }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month", tint = Color.White, modifier = Modifier.size(DesignSystem.IconSizeMedium))
                    }
                    IconButton(onClick = {
                        val newCal = calendar.clone() as Calendar
                        newCal.add(Calendar.MONTH, 1)
                        calendar = newCal
                    }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = Color.White, modifier = Modifier.size(DesignSystem.IconSizeMedium))
                    }
                }
            }

            Spacer(modifier = Modifier.height(DesignSystem.SpacingMedium))

            // Weekdays Header
            Row(modifier = Modifier.fillMaxWidth()) {
                val weekdays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                weekdays.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(DesignSystem.SpacingSmall))

            // Days Grid
            AnimatedContent(
                targetState = currentMonth,
                transitionSpec = {
                    fadeIn(animationSpec = tween(DesignSystem.AnimationDurationShort)) togetherWith
                            fadeOut(animationSpec = tween(DesignSystem.AnimationDurationShort))
                },
                label = "grid_transition"
            ) { targetMonth ->
                val daysInMonth = targetMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
                val firstDayOfWeek = targetMonth.get(Calendar.DAY_OF_WEEK)
                val totalCells = ((daysInMonth + firstDayOfWeek - 1 + 6) / 7) * 7

                val dayStartTimestamps = remember(targetMonth) {
                    (1..daysInMonth).associateWith { day ->
                        val dateCal = targetMonth.clone() as Calendar
                        dateCal.set(Calendar.DAY_OF_MONTH, day)
                        dateCal.set(Calendar.HOUR_OF_DAY, 0)
                        dateCal.set(Calendar.MINUTE, 0)
                        dateCal.set(Calendar.SECOND, 0)
                        dateCal.set(Calendar.MILLISECOND, 0)
                        dateCal.timeInMillis
                    }
                }

                val songsByDay = remember(songs, dayStartTimestamps) {
                    dayStartTimestamps.values.associateWith { ts -> songs.filter { it.postDate == ts } }
                }
                val remindersByDay = remember(reminders, dayStartTimestamps) {
                    dayStartTimestamps.values.associateWith { ts -> reminders.filter { it.isOccurringOn(ts) } }
                }
                val normalizedNow = remember(now) { normalizeDateToDayStart(now) }
                
                Column {
                    for (row in 0 until (totalCells / 7)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (col in 0 until 7) {
                                val dayIndex = row * 7 + col
                                val dayOfMonth = dayIndex - (firstDayOfWeek - 2)
                                
                                if (dayOfMonth in 1..daysInMonth) {
                                    val dateTimestamp = dayStartTimestamps[dayOfMonth] ?: 0L
                                    val songsOnDay = songsByDay[dateTimestamp] ?: emptyList()
                                    val remindersOnDay = remindersByDay[dateTimestamp] ?: emptyList()
                                    
                                    val isToday = dateTimestamp == normalizedNow

                                    val cellOnClick = remember(dateTimestamp, songsOnDay, remindersOnDay) {
                                        {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            onDateSelected(dateTimestamp, songsOnDay, remindersOnDay)
                                        }
                                    }

                                    DayCell(
                                        day = dayOfMonth,
                                        isToday = isToday,
                                        hasPosted = songsOnDay.any { it.postDate != null && it.postDate!! <= normalizedNow },
                                        hasScheduled = songsOnDay.any { it.postDate != null && it.postDate!! > normalizedNow },
                                        hasReminders = remindersOnDay.isNotEmpty(),
                                        onClick = cellOnClick,
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayCell(
    day: Int,
    isToday: Boolean,
    hasPosted: Boolean,
    hasScheduled: Boolean,
    hasReminders: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(DesignSystem.CornerRadiusSmall))
            .background(if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(DesignSystem.SpacingTiny))
        
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            if (hasPosted) {
                Box(modifier = Modifier.size(4.dp).background(StatusPosted, CircleShape))
            }
            if (hasScheduled) {
                Box(modifier = Modifier.size(4.dp).background(StatusScheduled, CircleShape))
            }
            if (hasReminders) {
                Box(modifier = Modifier.size(4.dp).background(StatusReminder, CircleShape))
            }
        }
    }
}

private fun normalizeDateToDayStart(timestamp: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

val CalendarSaver = androidx.compose.runtime.saveable.Saver<MutableState<Calendar>, Long>(
    save = { it.value.timeInMillis },
    restore = { mutableStateOf(Calendar.getInstance().apply { timeInMillis = it }) }
)
