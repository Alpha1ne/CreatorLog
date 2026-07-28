package com.voiceofmelody.songdailytracker.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.voiceofmelody.songdailytracker.data.model.Reminder
import com.voiceofmelody.songdailytracker.data.model.RepeatType
import com.voiceofmelody.songdailytracker.ui.theme.DesignSystem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderEditDialog(
    reminder: Reminder?,
    initialDate: Long,
    onDismiss: () -> Unit,
    onSave: (String, String, Long?, Boolean, RepeatType) -> Unit
) {
    var title by remember { mutableStateOf(reminder?.title ?: "") }
    var note by remember { mutableStateOf(reminder?.note ?: "") }
    var notificationsEnabled by remember { mutableStateOf(reminder?.notificationsEnabled ?: true) }
    var repeatType by remember { mutableStateOf(reminder?.repeatType ?: RepeatType.NONE) }
    
    var showTimePicker by remember { mutableStateOf(false) }
    val calendar = Calendar.getInstance().apply {
        if (reminder?.reminderTime != null) {
            timeInMillis = reminder.reminderDate + reminder.reminderTime
        }
    }
    
    var selectedHour by remember { mutableIntStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableIntStateOf(calendar.get(Calendar.MINUTE)) }
    var hasTime by remember { mutableStateOf(reminder?.reminderTime != null) }

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(DesignSystem.CornerRadiusLarge),
            modifier = Modifier.fillMaxWidth().padding(DesignSystem.SpacingMedium),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(DesignSystem.BorderThickness, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(DesignSystem.CardPadding),
                verticalArrangement = Arrangement.spacedBy(DesignSystem.SpacingMedium)
            ) {
                Text(
                    text = if (reminder == null) "Add Reminder" else "Edit Reminder",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(DesignSystem.CornerRadiusSmall),
                    textStyle = MaterialTheme.typography.bodyLarge
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(DesignSystem.CornerRadiusSmall),
                    minLines = 2,
                    textStyle = MaterialTheme.typography.bodyMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Reminder Time", style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = if (hasTime) {
                                val c = Calendar.getInstance()
                                c.set(Calendar.HOUR_OF_DAY, selectedHour)
                                c.set(Calendar.MINUTE, selectedMinute)
                                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(c.time)
                            } else "Not set",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Row {
                        if (hasTime) {
                            IconButton(onClick = { hasTime = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Time", modifier = Modifier.size(DesignSystem.IconSizeSmall))
                            }
                        }
                        Button(onClick = { showTimePicker = true }, shape = RoundedCornerShape(DesignSystem.CornerRadiusSmall)) {
                            Text(if (hasTime) "Change" else "Set Time", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Notifications", style = MaterialTheme.typography.labelLarge)
                    Switch(checked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it })
                }

                // Repeat Section
                Column(verticalArrangement = Arrangement.spacedBy(DesignSystem.SpacingSmall)) {
                    Text("Repeat", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(DesignSystem.SpacingSmall)
                    ) {
                        RepeatType.entries.forEach { type ->
                            FilterChip(
                                selected = repeatType == type,
                                onClick = { repeatType = type },
                                label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                shape = RoundedCornerShape(DesignSystem.CornerRadiusChip)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel", style = MaterialTheme.typography.labelLarge) }
                    Spacer(modifier = Modifier.width(DesignSystem.SpacingSmall))
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            val timeMillis = if (hasTime) {
                                (selectedHour * 3600000L) + (selectedMinute * 60000L)
                            } else null
                            onSave(title, note, timeMillis, notificationsEnabled, repeatType)
                        },
                        enabled = title.isNotBlank(),
                        shape = RoundedCornerShape(DesignSystem.CornerRadiusSmall)
                    ) {
                        Text("Save", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            is24Hour = false
        )
        
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedHour = timePickerState.hour
                    selectedMinute = timePickerState.minute
                    hasTime = true
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}
