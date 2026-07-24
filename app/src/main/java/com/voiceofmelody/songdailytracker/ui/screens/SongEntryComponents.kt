package com.voiceofmelody.songdailytracker.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voiceofmelody.songdailytracker.data.model.SongPost
import com.voiceofmelody.songdailytracker.ui.DuplicateGroup
import com.voiceofmelody.songdailytracker.ui.MatchLevel
import com.voiceofmelody.songdailytracker.ui.SongStatus
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DuplicateWarningSection(duplicateMatches: DuplicateGroup) {
    AnimatedVisibility(visible = !duplicateMatches.isEmpty) {
        val topLevel = duplicateMatches.topLevel
        val color = when (topLevel) {
            MatchLevel.EXACT -> MaterialTheme.colorScheme.error
            MatchLevel.POSSIBLE -> Color(0xFFFB8C00)
            MatchLevel.SIMILAR -> Color(0xFF1E88E5)
            else -> MaterialTheme.colorScheme.primary
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
            border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = color)
                Column {
                    Text(
                        text = when (topLevel) {
                            MatchLevel.EXACT -> "EXACT MATCH DETECTED"
                            MatchLevel.POSSIBLE -> "POSSIBLE DUPLICATE"
                            MatchLevel.SIMILAR -> "SIMILAR SONG RECORDED"
                            else -> ""
                        },
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    Text("A similar song exists in your history. Review details in the list.", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun SongInformationSection(
    title: String, onTitleChange: (String) -> Unit,
    movieName: String, onMovieNameChange: (String) -> Unit,
    singers: String, onSingersChange: (String) -> Unit,
    musicDirector: String, onMusicDirectorChange: (String) -> Unit,
    language: String, onLanguageChange: (String) -> Unit,
    notes: String, onNotesChange: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Song Information", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            
            OutlinedTextField(value = title, onValueChange = onTitleChange, label = { Text("Song Title *") }, placeholder = { Text("e.g., Starboy") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = movieName, onValueChange = onMovieNameChange, label = { Text("Movie / Album Name *") }, placeholder = { Text("e.g., Starboy Album") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = singers, onValueChange = onSingersChange, label = { Text("Singer(s)") }, placeholder = { Text("e.g., The Weeknd") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = musicDirector, onValueChange = onMusicDirectorChange, label = { Text("Music Director") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = language, onValueChange = onLanguageChange, label = { Text("Language") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = notes, onValueChange = onNotesChange, label = { Text("Notes / Details") }, modifier = Modifier.fillMaxWidth(), minLines = 3, shape = RoundedCornerShape(12.dp))
        }
    }
}

@Composable
fun SchedulingSection(
    postDate: Long?,
    status: SongStatus?,
    onPickDate: () -> Unit,
    onClearDate: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Scheduling", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

            if (postDate != null) {
                val formatted = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(postDate))
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (status != null) {
                        Surface(
                            color = when(status) {
                                SongStatus.SCHEDULED -> Color(0xFFFBBF24).copy(alpha = 0.1f)
                                SongStatus.POSTED -> Color(0xFF10B981).copy(alpha = 0.1f)
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, when(status) {
                                SongStatus.SCHEDULED -> Color(0xFFFBBF24)
                                SongStatus.POSTED -> Color(0xFF10B981)
                            }.copy(alpha = 0.5f))
                        ) {
                            Text(status.name, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text("Date: $formatted", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onPickDate, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Change Date")
                    }
                    OutlinedButton(onClick = onClearDate, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Remove Date")
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("No posting date selected. This song will be recorded without a status.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onPickDate, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Posting Date")
                    }
                }
            }
        }
    }
}

@Composable
fun MetadataSection(
    entryNumber: Long?,
    status: SongStatus?,
    duplicateStatus: String? = null
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Metadata", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (entryNumber != null) {
                MetadataRow(label = "Entry Number", value = "#${String.format(Locale.US, "%04d", entryNumber)}")
            }
            if (status != null) {
                MetadataRow(label = "Status", value = status.name)
            }
            if (duplicateStatus != null) {
                MetadataRow(label = "Duplicate Status", value = duplicateStatus)
            }
        }
    }
}

@Composable
fun MetadataRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AttachmentsSection() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Attachments", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text("Coming Soon", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("This section will later support Videos, Audio, Images, and Documents.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        }
    }
}
