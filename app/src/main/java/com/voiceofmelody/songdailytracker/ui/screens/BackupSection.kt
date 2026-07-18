package com.voiceofmelody.songdailytracker.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voiceofmelody.songdailytracker.ui.TrackerViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSection(viewModel: TrackerViewModel) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Loading States
    var isOperating by remember { mutableStateOf(value = false) }

    // Dialog States
    var showRestoreConfirm by remember { mutableStateOf(value = false) }
    var restoreErrorMessage by remember { mutableStateOf<String?>(null) }
    var pendingRestoreAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    // --- SAF Launchers ---

    // JSON Export
    val createJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            isOperating = true
            val json = viewModel.exportBackupJson()
            viewModel.writeToFile(it, json) { success ->
                isOperating = false
                if (success) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    Toast.makeText(context, "Backup created successfully.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // JSON Import
    val openJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            isOperating = true
            viewModel.readFromFile(it) { content ->
                if (content != null) {
                    val success = viewModel.importBackupJson(content)
                    isOperating = false
                    if (success) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        Toast.makeText(context, "Backup restored successfully.", Toast.LENGTH_SHORT).show()
                    } else restoreErrorMessage = "The selected file is not a valid Voice Of Melody backup."
                } else {
                    isOperating = false
                    restoreErrorMessage = "Could not read the selected file."
                }
            }
        }
    }

    // Songs CSV Export
    val createSongsCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let {
            isOperating = true
            val csv = viewModel.exportBackupCsvSongs()
            viewModel.writeToFile(it, csv) { success ->
                isOperating = false
                if (success) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    Toast.makeText(context, "Songs exported successfully.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Songs CSV Import
    val openSongsCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            isOperating = true
            viewModel.readFromFile(it) { content ->
                if (content != null) {
                    val success = viewModel.importBackupCsvSongs(content)
                    isOperating = false
                    if (success) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        Toast.makeText(context, "Songs imported successfully.", Toast.LENGTH_SHORT).show()
                    } else restoreErrorMessage = "The selected file is not a valid Songs CSV."
                } else {
                    isOperating = false
                }
            }
        }
    }

    // Ideas CSV Export
    val createIdeasCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let {
            isOperating = true
            val csv = viewModel.exportBackupCsvIdeas()
            viewModel.writeToFile(it, csv) { success ->
                isOperating = false
                if (success) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    Toast.makeText(context, "Planner exported successfully.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Ideas CSV Import
    val openIdeasCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            isOperating = true
            viewModel.readFromFile(it) { content ->
                if (content != null) {
                    val success = viewModel.importBackupCsvIdeas(content)
                    isOperating = false
                    if (success) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        Toast.makeText(context, "Ideas imported successfully.", Toast.LENGTH_SHORT).show()
                    } else restoreErrorMessage = "The selected file is not a valid Ideas CSV."
                } else {
                    isOperating = false
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isOperating) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // JSON Section
            BackupCategoryCard(
                title = "Full JSON Backup",
                description = "Complete application data (Recommended)",
                icon = Icons.Default.Backup,
                onExport = {
                    val date = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
                    createJsonLauncher.launch("VoiceOfMelody_Backup_$date.json")
                },
                onImport = {
                    pendingRestoreAction = { openJsonLauncher.launch(arrayOf("application/json")) }
                    showRestoreConfirm = true
                },
                exportLabel = "Create Backup",
                importLabel = "Restore Backup",
                isEnabled = !isOperating
            )

            // Songs CSV Section
            BackupCategoryCard(
                title = "Songs Spreadsheet (CSV)",
                description = "Export song history for Excel/Sheets",
                icon = Icons.Default.MusicNote,
                onExport = {
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    createSongsCsvLauncher.launch("VoiceOfMelody_Songs_$date.csv")
                },
                onImport = {
                    pendingRestoreAction = { openSongsCsvLauncher.launch(arrayOf("text/comma-separated-values", "text/csv")) }
                    showRestoreConfirm = true
                },
                isEnabled = !isOperating
            )

            // Ideas CSV Section
            BackupCategoryCard(
                title = "Planner Spreadsheet (CSV)",
                description = "Export your content ideas",
                icon = Icons.Default.Lightbulb,
                onExport = {
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    createIdeasCsvLauncher.launch("VoiceOfMelody_IdeaPlanner_$date.csv")
                },
                onImport = {
                    pendingRestoreAction = { openIdeasCsvLauncher.launch(arrayOf("text/comma-separated-values", "text/csv")) }
                    showRestoreConfirm = true
                },
                isEnabled = !isOperating
            )

            Spacer(modifier = Modifier.height(100.dp))
        }

        if (isOperating) {
            Surface(
                modifier = Modifier.matchParentSize(),
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    // --- Dialogs ---

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("Confirm Import") },
            text = { Text("This will add entries from the selected file into your app. Duplicate entries will be skipped. Proceed?") },
            confirmButton = {
                Button(onClick = {
                    pendingRestoreAction?.invoke()
                    showRestoreConfirm = false
                }) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (restoreErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { restoreErrorMessage = null },
            title = { Text("Import Error") },
            text = { Text(restoreErrorMessage!!) },
            confirmButton = {
                Button(onClick = { restoreErrorMessage = null }) { Text("OK") }
            }
        )
    }
}

@Composable
fun BackupCategoryCard(
    title: String,
    description: String,
    icon: ImageVector,
    onExport: () -> Unit,
    onImport: () -> Unit,
    exportLabel: String = "Export",
    importLabel: String = "Import",
    isEnabled: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onExport,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    enabled = isEnabled
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(exportLabel, fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = onImport,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    enabled = isEnabled
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(importLabel, fontSize = 13.sp)
                }
            }
        }
    }
}
