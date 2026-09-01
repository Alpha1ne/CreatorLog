package com.creatorlog.app.ui.screens

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.creatorlog.app.ui.TrackerViewModel
import com.creatorlog.app.ui.theme.DesignSystem
import com.creatorlog.app.util.sendFeedbackEmail
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    themeMode: Int,
    onThemeModeChanged: (Int) -> Unit,
    viewModel: TrackerViewModel,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val packageInfo = remember {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
        } catch (_: Exception) {
            null
        }
    }
    val versionName = packageInfo?.versionName ?: "1.6.0"
    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo?.longVersionCode?.toString() ?: "6"
    } else {
        @Suppress("DEPRECATION")
        packageInfo?.versionCode?.toString() ?: "6"
    }

    var showWhatsNew by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
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
                .fillMaxWidth()
                .padding(innerPadding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DesignSystem.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Appearance Section
            SettingsSection(title = "Appearance") {
                val themeLabel = when (themeMode) {
                    0 -> "System Default"
                    1 -> "Light Mode"
                    else -> "Dark Mode"
                }
                ActionItem(
                    label = "Theme",
                    description = themeLabel,
                    icon = Icons.Default.Palette,
                    onClick = { showThemeDialog = true }
                )
            }

            // Backup & Restore Section
            SettingsSection(title = "Backup & Restore") {
                BackupSection(viewModel = viewModel, snackbarHostState = snackbarHostState)
            }

            // What's New Section
            SettingsSection(title = "Updates") {
                ActionItem(
                    label = "What's New",
                    description = "CreatorLog v$versionName",
                    icon = Icons.Default.NewReleases,
                    onClick = { showWhatsNew = true }
                )
            }

            // About Section
            SettingsSection(title = "About") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CreatorLog",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Content management for creators",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "v$versionName",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                
                InfoItem(label = "Build", value = versionCode)
                InfoItem(label = "Developer", value = "ATHUL C")
                
                Text(
                    text = "© 2026 ATHUL C. All rights reserved.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 12.dp)
                )
            }

            // Support
            SettingsSection(title = "Support & Feedback") {
                val context = LocalContext.current
                ActionItem(
                    label = "Feedback & Bug Reports",
                    icon = Icons.Default.Feedback,
                    onClick = { sendFeedbackEmail(context) }
                )
                ActionItem(label = "Rate Application", icon = Icons.Default.Star)
            }
            
            Spacer(modifier = Modifier.height(116.dp))
        }
    }

    if (showWhatsNew) {
        WhatsNewDialog(
            versionName = versionName,
            versionCode = versionCode,
            onDismiss = { showWhatsNew = false }
        )
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentMode = themeMode,
            onDismiss = { showThemeDialog = false },
            onSelect = { 
                onThemeModeChanged(it)
                showThemeDialog = false
            }
        )
    }
}

@Composable
fun ThemeSelectionDialog(currentMode: Int, onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Theme", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                listOf("System Default" to 0, "Light Mode" to 1, "Dark Mode" to 2).forEach { (label, value) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value) }
                            .padding(vertical = 12.dp)
                    ) {
                        RadioButton(selected = currentMode == value, onClick = { onSelect(value) })
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(DesignSystem.CornerRadiusLarge)
    )
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title.uppercase(), 
            style = MaterialTheme.typography.labelMedium, 
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(DesignSystem.CornerRadiusMedium),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(DesignSystem.BorderThickness, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                content()
            }
        }
    }
}

@Composable
fun ActionItem(label: String, description: String? = null, icon: ImageVector, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(
            Icons.Default.ChevronRight, 
            contentDescription = null, 
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun InfoItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun WhatsNewDialog(versionName: String, versionCode: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it")
            }
        },
        title = {
            Column {
                Text("What's New", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("CreatorLog $versionName", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(DesignSystem.SpacingLarge)
            ) {
                WhatsNewSection(
                    title = "NEW & IMPROVED",
                    items = listOf(
                        "Complete CreatorLog Rebranding" to "The application has been fully transitioned from its original identity to CreatorLog, including the new technical package identity com.creatorlog.app.",
                        "Daily Streak" to "Added a dedicated Daily Streak experience with current streak, longest streak, total active days, weekly activity, milestones, and achievement tracking.",
                        "Permanent Streak History" to "Earned streak days remain recorded even if the original content is later deleted or edited.",
                        "Streak Activation" to "Streak calculations only begin from the date the Daily Streak feature was activated and do not retroactively count older content.",
                        "Floating Navigation Bar" to "Redesigned the main navigation experience with a compact floating navigation bar, animated active circle, concave notch, and improved alignment.",
                        "Navigation & Back Behavior" to "Improved Android Back navigation so secondary screens return to the Dashboard instead of unexpectedly closing the application.",
                        "Content & Planner UI Improvements" to "Improved search-bar design, Planner cards, form-field visibility, spacing, and floating Add (+) button positioning.",
                        "Settings UI Improvements" to "Redesigned Settings into a more compact and organized layout with cleaner grouped sections.",
                        "Feedback & Bug Reports" to "Added a functional \"Feedback & Bug Reports\" action that opens the user's email application addressed to creatorlogsupport@gmail.com.",
                        "Backup & Restore Improvements" to "Preserved existing backup/restore compatibility while maintaining Planner completion state and existing user data behavior.",
                        "Light & Dark Mode Improvements" to "Improved theme-aware navigation and interface visibility across both Light and Dark modes."
                    )
                )

                WhatsNewSection(
                    title = "VERSION INFORMATION",
                    items = listOf(
                        "CreatorLog $versionName" to "Build $versionCode"
                    )
                )
            }
        },
        shape = RoundedCornerShape(DesignSystem.CornerRadiusLarge)
    )
}

@Composable
private fun WhatsNewSection(title: String, items: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
        items.forEach { (header, description) ->
            Column {
                Text(text = "• $header", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }
    }
}
