package com.voiceofmelody.songdailytracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voiceofmelody.songdailytracker.data.model.Reminder
import com.voiceofmelody.songdailytracker.data.model.SongPost
import com.voiceofmelody.songdailytracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailBottomSheet(
    selectedDate: Long,
    songs: List<SongPost>,
    reminders: List<Reminder>,
    onDismiss: () -> Unit,
    onAddReminder: () -> Unit,
    onEditReminder: (Reminder) -> Unit,
    onDeleteReminder: (Reminder) -> Unit,
    onOpenSong: (SongPost) -> Unit
) {
    val dateStr = remember(selectedDate) {
        SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(selectedDate))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = DesignSystem.CornerRadiusLarge, topEnd = DesignSystem.CornerRadiusLarge),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DesignSystem.CardPadding)
                .padding(bottom = DesignSystem.SpacingXXLarge)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(DesignSystem.SpacingXLarge)
        ) {
            HeaderSection(dateStr, onAddReminder)

            if (songs.isNotEmpty()) {
                EventSection(title = "Content", icon = Icons.Default.AutoAwesome, color = MaterialTheme.colorScheme.primary) {
                    songs.forEach { song ->
                        SongItem(song, onOpenSong)
                    }
                }
            }

            if (reminders.isNotEmpty()) {
                val remindersOnDay = reminders.filter { it.isOccurringOn(selectedDate) }
                if (remindersOnDay.isNotEmpty()) {
                    EventSection(title = "Reminders", icon = Icons.Default.NotificationImportant, color = StatusReminder) {
                        remindersOnDay.forEach { reminder ->
                            ReminderItem(reminder, onEditReminder, onDeleteReminder)
                        }
                    }
                }
            }

            if (songs.isEmpty() && reminders.isEmpty()) {
                EmptyDayState()
            }
        }
    }
}

@Composable
fun HeaderSection(dateStr: String, onAddReminder: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = "Daily Hub", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(text = dateStr, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        FilledTonalButton(
            onClick = { 
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onAddReminder() 
            },
            shape = RoundedCornerShape(DesignSystem.CornerRadiusSmall),
            contentPadding = PaddingValues(horizontal = DesignSystem.SpacingMedium, vertical = DesignSystem.SpacingSmall)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(DesignSystem.IconSizeSmall))
            Spacer(modifier = Modifier.width(DesignSystem.SpacingSmall))
            Text("Reminder", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EventSection(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}

@Composable
fun SongItem(song: SongPost, onOpenSong: (SongPost) -> Unit) {
    val haptic = LocalHapticFeedback.current
    Card(
        onClick = { 
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onOpenSong(song) 
        },
        shape = RoundedCornerShape(DesignSystem.CornerRadiusMedium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(DesignSystem.BorderThickness, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(DesignSystem.SpacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(DesignSystem.SpacingNormal))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = song.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = song.movieName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun ReminderItem(reminder: Reminder, onEdit: (Reminder) -> Unit, onDelete: (Reminder) -> Unit) {
    val haptic = LocalHapticFeedback.current
    Card(
        shape = RoundedCornerShape(DesignSystem.CornerRadiusMedium),
        colors = CardDefaults.cardColors(containerColor = StatusReminder.copy(alpha = 0.05f)),
        border = androidx.compose.foundation.BorderStroke(DesignSystem.BorderThickness, StatusReminder.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(DesignSystem.SpacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).background(StatusReminder.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = StatusReminder, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(DesignSystem.SpacingNormal))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = reminder.title, style = MaterialTheme.typography.titleSmall)
                if (reminder.note.isNotBlank()) {
                    Text(text = reminder.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                if (reminder.reminderTime != null) {
                    val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(reminder.reminderDate + reminder.reminderTime))
                    Text(text = timeStr, style = MaterialTheme.typography.labelSmall, color = StatusReminder, fontWeight = FontWeight.Bold)
                }
            }
            Row {
                IconButton(onClick = { onEdit(reminder) }) { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp)) }
                IconButton(onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDelete(reminder) 
                }) { Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
fun EmptyDayState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
        Text(text = "Nothing scheduled today.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
