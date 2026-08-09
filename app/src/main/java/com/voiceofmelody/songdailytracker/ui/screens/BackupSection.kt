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
import com.voiceofmelody.songdailytracker.ui.theme.DesignSystem
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSection(viewModel: TrackerViewModel, snackbarHostState: SnackbarHostState) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

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
                    scope.launch { snackbarHostState.showSnackbar("Backup created successfully") }
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
                        scope.launch { snackbarHostState.showSnackbar("Backup restored successfully") }
                    } else restoreErrorMessage = "The selected file is not a valid CreatorLog backup."
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
                    scope.launch { snackbarHostState.showSnackbar("Content Library exported successfully") }
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
                        scope.launch { snackbarHostState.showSnackbar("Content Library imported successfully") }
                    } else restoreErrorMessage = "The selected file is not a valid Content Library CSV."
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
                    scope.launch { snackbarHostState.showSnackbar("Planner exported successfully") }
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
                        scope.launch { snackbarHostState.showSnackbar("Ideas imported successfully") }
                    } else restoreErrorMessage = "The selected file is not a valid Ideas CSV."
                } else {
                    isOperating = false
                }
            }
        }
    }

    // Promotions CSV Export
    val createPromotionsCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let {
            isOperating = true
            val csv = viewModel.exportBackupCsvPromotions()
            viewModel.writeToFile(it, csv) { success ->
                isOperating = false
                if (success) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch { snackbarHostState.showSnackbar("Promotions exported successfully") }
                }
            }
        }
    }

    // Promotions CSV Import
    val openPromotionsCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            isOperating = true
            viewModel.readFromFile(it) { content ->
                if (content != null) {
                    val success = viewModel.importBackupCsvPromotions(content)
                    isOperating = false
                    if (success) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch { snackbarHostState.showSnackbar("Promotions imported successfully") }
                    } else restoreErrorMessage = "The selected file is not a valid Promotions CSV."
                } else {
                    isOperating = false
                }
            }
        }
    }

    Box {
        Column(
            verticalArrangement = Arrangement.spacedBy(DesignSystem.SpacingMedium)
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
                    createJsonLauncher.launch("CreatorLog_Backup_$date.json")
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
                title = "Content Library (CSV)",
                description = "Export content history for Excel/Sheets",
                icon = Icons.Default.AutoAwesome,
                onExport = {
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    createSongsCsvLauncher.launch("CreatorLog_Content_$date.csv")
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
                    createIdeasCsvLauncher.launch("CreatorLog_IdeaPlanner_$date.csv")
                },
                onImport = {
                    pendingRestoreAction = { openIdeasCsvLauncher.launch(arrayOf("text/comma-separated-values", "text/csv")) }
                    showRestoreConfirm = true
                },
                isEnabled = !isOperating
            )

            // Promotions CSV Section
            BackupCategoryCard(
                title = "Promotions Spreadsheet (CSV)",
                description = "Export your earnings data",
                icon = Icons.Default.Payments,
                onExport = {
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    createPromotionsCsvLauncher.launch("CreatorLog_Promotions_$date.csv")
                },
                onImport = {
                    pendingRestoreAction = { openPromotionsCsvLauncher.launch(arrayOf("text/comma-separated-values", "text/csv")) }
                    showRestoreConfirm = true
                },
                isEnabled = !isOperating
            )

            Spacer(modifier = Modifier.height(100.dp))
        }

        if (isOperating) {
            Surface(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f)
            ) {
                Box(modifier = Modifier.padding(32.dp), contentAlignment = Alignment.Center) {
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
        shape = RoundedCornerShape(DesignSystem.CornerRadiusLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(DesignSystem.BorderThickness, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(DesignSystem.CardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(DesignSystem.IconSizeMedium))
                Spacer(modifier = Modifier.width(DesignSystem.SpacingNormal))
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(DesignSystem.SpacingLarge))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DesignSystem.SpacingSmall)
            ) {
                Button(
                    onClick = onExport,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(DesignSystem.CornerRadiusSmall),
                    enabled = isEnabled
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(DesignSystem.IconSizeSmall))
                    Spacer(modifier = Modifier.width(DesignSystem.SpacingSmall))
                    Text(exportLabel, style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = onImport,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(DesignSystem.CornerRadiusSmall),
                    enabled = isEnabled
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(DesignSystem.IconSizeSmall))
                    Spacer(modifier = Modifier.width(DesignSystem.SpacingSmall))
                    Text(importLabel, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
