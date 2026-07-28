package com.voiceofmelody.songdailytracker.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
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
import com.voiceofmelody.songdailytracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DuplicateWarningSection(duplicateMatches: DuplicateGroup) {
    AnimatedVisibility(visible = !duplicateMatches.isEmpty) {
        val topLevel = duplicateMatches.topLevel
        val color = when (topLevel) {
            MatchLevel.EXACT -> StatusReminder
            MatchLevel.POSSIBLE -> StatusScheduled
            MatchLevel.SIMILAR -> AccentAzure
            else -> MaterialTheme.colorScheme.primary
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
            border = BorderStroke(DesignSystem.BorderThickness, color.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(DesignSystem.CornerRadiusLarge)
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
                            MatchLevel.SIMILAR -> "SIMILAR CONTENT RECORDED"
                            else -> ""
                        },
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    Text("Similar content exists in your history. Review details in the list.", fontSize = 12.sp)
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
    notes: String, onNotesChange: (String) -> Unit,
    contentLink: String, onContentLinkChange: (String) -> Unit
) {
    var linkError by remember(contentLink) {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(contentLink) {
        if (contentLink.isNotBlank()) {
            val isValid = android.util.Patterns.WEB_URL.matcher(contentLink).matches()
            if (!isValid) {
                linkError = "Please enter a valid URL"
            } else if (!contentLink.contains("instagram.com")) {
                linkError = "Preferred: Instagram link"
            } else {
                linkError = null
            }
        } else {
            linkError = null
        }
    }

    Card(
        shape = RoundedCornerShape(DesignSystem.CornerRadiusLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(DesignSystem.BorderThickness, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(DesignSystem.CardPadding), verticalArrangement = Arrangement.spacedBy(DesignSystem.SpacingMedium)) {
            Text("Content Information", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            
            OutlinedTextField(value = title, onValueChange = onTitleChange, label = { Text("Content Title *") }, placeholder = { Text("Enter content title") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(DesignSystem.CornerRadiusSmall))
            OutlinedTextField(value = movieName, onValueChange = onMovieNameChange, label = { Text("Movie / Song") }, placeholder = { Text("Enter movie or song name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(DesignSystem.CornerRadiusSmall))
            OutlinedTextField(value = singers, onValueChange = onSingersChange, label = { Text("Singer(s) / Actor(s)") }, placeholder = { Text("Enter singer or actor names") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(DesignSystem.CornerRadiusSmall))
            OutlinedTextField(value = musicDirector, onValueChange = onMusicDirectorChange, label = { Text("Director / Music Director") }, placeholder = { Text("Enter director or music director") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(DesignSystem.CornerRadiusSmall))
            OutlinedTextField(value = language, onValueChange = onLanguageChange, label = { Text("Language") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(DesignSystem.CornerRadiusSmall))
            OutlinedTextField(value = notes, onValueChange = onNotesChange, label = { Text("Notes / Details") }, modifier = Modifier.fillMaxWidth(), minLines = 3, shape = RoundedCornerShape(DesignSystem.CornerRadiusSmall))
            
            Column(verticalArrangement = Arrangement.spacedBy(DesignSystem.SpacingTiny)) {
                OutlinedTextField(
                    value = contentLink,
                    onValueChange = onContentLinkChange,
                    label = { Text("Content Link (Optional)") },
                    placeholder = { Text("https://www.instagram.com/p/...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(DesignSystem.CornerRadiusSmall),
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                    isError = linkError != null,
                    supportingText = linkError?.let { { Text(it) } }
                )

                if (contentLink.isNotBlank() && linkError == null) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val haptic = LocalHapticFeedback.current
                    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = DesignSystem.SpacingTiny),
                        horizontalArrangement = Arrangement.spacedBy(DesignSystem.SpacingSmall)
                    ) {
                        TextButton(
                            onClick = { 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                com.voiceofmelody.songdailytracker.util.openContentLink(context, contentLink) 
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(DesignSystem.SpacingSmall))
                            Text("Open", style = MaterialTheme.typography.labelMedium)
                        }

                        TextButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(contentLink))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(DesignSystem.SpacingSmall))
                            Text("Copy", style = MaterialTheme.typography.labelMedium)
                        }

                        TextButton(
                            onClick = { 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onContentLinkChange("") 
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(DesignSystem.SpacingSmall))
                            Text("Clear", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
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
        shape = RoundedCornerShape(DesignSystem.CornerRadiusLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(DesignSystem.BorderThickness, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(DesignSystem.CardPadding), verticalArrangement = Arrangement.spacedBy(DesignSystem.SpacingMedium)) {
            Text("Scheduling", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

            if (postDate != null) {
                val formatted = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(postDate))
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DesignSystem.SpacingSmall)) {
                    if (status != null) {
                        Surface(
                            color = when(status) {
                                SongStatus.SCHEDULED -> StatusScheduled.copy(alpha = 0.1f)
                                SongStatus.POSTED -> StatusPosted.copy(alpha = 0.1f)
                            },
                            shape = RoundedCornerShape(DesignSystem.CornerRadiusBadge),
                            border = BorderStroke(DesignSystem.BorderThickness, when(status) {
                                SongStatus.SCHEDULED -> StatusScheduled
                                SongStatus.POSTED -> StatusPosted
                            }.copy(alpha = 0.5f))
                        ) {
                            Text(status.name, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text("Date: $formatted", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DesignSystem.SpacingSmall)) {
                    Button(onClick = onPickDate, modifier = Modifier.weight(1f), shape = RoundedCornerShape(DesignSystem.CornerRadiusSmall)) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(DesignSystem.IconSizeSmall))
                        Spacer(modifier = Modifier.width(DesignSystem.SpacingSmall))
                        Text("Change Date", style = MaterialTheme.typography.labelLarge)
                    }
                    OutlinedButton(onClick = onClearDate, modifier = Modifier.weight(1f), shape = RoundedCornerShape(DesignSystem.CornerRadiusSmall)) {
                        Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(DesignSystem.IconSizeSmall))
                        Spacer(modifier = Modifier.width(DesignSystem.SpacingSmall))
                        Text("Remove Date", style = MaterialTheme.typography.labelLarge)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(DesignSystem.SpacingSmall)) {
                    Text("No posting date selected. This content will be recorded without a status.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onPickDate, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(DesignSystem.CornerRadiusSmall)) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(DesignSystem.IconSizeSmall))
                        Spacer(modifier = Modifier.width(DesignSystem.SpacingSmall))
                        Text("Select Posting Date", style = MaterialTheme.typography.labelLarge)
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
        shape = RoundedCornerShape(DesignSystem.CornerRadiusLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(DesignSystem.BorderThickness, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(DesignSystem.CardPadding), verticalArrangement = Arrangement.spacedBy(DesignSystem.SpacingSmall)) {
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
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun AttachmentsSection() {
    Card(
        shape = RoundedCornerShape(DesignSystem.CornerRadiusLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(DesignSystem.BorderThickness, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(DesignSystem.CardPadding), verticalArrangement = Arrangement.spacedBy(DesignSystem.SpacingSmall)) {
            Text("Attachments", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text("Coming Soon", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("This section will later support Videos, Audio, Images, and Documents.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        }
    }
}
