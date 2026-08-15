package com.voiceofmelody.songdailytracker.ui.screens

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
import com.voiceofmelody.songdailytracker.ui.TrackerViewModel
import com.voiceofmelody.songdailytracker.ui.theme.DesignSystem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    themeMode: Int,
    onThemeModeChanged: (Int) -> Unit,
    viewModel: TrackerViewModel,
    snackbarHostState: SnackbarHostState
) {
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
                    description = "CreatorLog v1.5.0",
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
                        text = "v1.5.0",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                
                InfoItem(label = "Build", value = "2026.08.V1.5")
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
                ActionItem(label = "Report an Issue", icon = Icons.Default.BugReport)
                ActionItem(label = "Send Feedback", icon = Icons.Default.Feedback)
                ActionItem(label = "Rate Application", icon = Icons.Default.Star)
            }
            
            Spacer(modifier = Modifier.height(116.dp))
        }
    }

    if (showWhatsNew) {
        WhatsNewDialog(onDismiss = { showWhatsNew = false })
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
fun WhatsNewDialog(onDismiss: () -> Unit) {
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
                Text("CreatorLog 1.5.0", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(DesignSystem.SpacingLarge)
            ) {
                WhatsNewSection(
                    title = "NEW",
                    items = listOf(
                        "Floating Navigation Bar" to "Premium floating navigation experience with a concave notch and elevated active tab circle.",
                        "Navigation Overlay" to "Navigation now floats above content, allowing it to visually continue behind the bar.",
                        "Compact Search" to "Cleaner rounded-rectangle search bars in Content Library and Planner.",
                        "Improved Planner Workspace" to "Redesigned Planner cards with clearer hierarchy and better organization."
                    )
                )

                WhatsNewSection(
                    title = "IMPROVEMENTS",
                    items = listOf(
                        "Compact Settings" to "Settings sections are now smaller, better grouped, and easier to scan.",
                        "Improved Planner Forms" to "New Idea and Edit Idea dialogs now have clearer field boundaries and better spacing.",
                        "Theme-Aware Navigation" to "Navigation icons and labels automatically adapt to Light and Dark mode.",
                        "Optimized FABs" to "Add (+) buttons are now positioned correctly above the floating navigation bar.",
                        "Navigation Alignment" to "Synchronized positioning for smoother navigation interaction."
                    )
                )

                WhatsNewSection(
                    title = "BUG FIXES",
                    items = listOf(
                        "Planner Backup/Restore" to "Fixed issue where task completion states were lost during restore.",
                        "Promotion Insights" to "Monthly earnings now correctly use actual PAID promotions and their payment dates.",
                        "Promotion Workflow" to "Amount is now optional for Pending and Partially Paid promotions.",
                        "Settings Spacing" to "Fixed unwanted gaps between sections in the Settings screen.",
                        "UI Overlaps" to "Fixed collision between floating buttons and the navigation bar.",
                        "Light Mode Visibility" to "Fixed navigation visibility issues in Light Mode."
                    )
                )

                WhatsNewSection(
                    title = "PERFORMANCE",
                    items = listOf(
                        "Faster Startup" to "Improved cold-start performance and reduced splash delay.",
                        "Optimized Loading" to "Deferred heavy content registration and optimized icon loading using local vectors.",
                        "Clean Production Code" to "Removed all unnecessary debug performance logging for the final release."
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
