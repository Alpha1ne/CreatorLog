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

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge) },
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
                .verticalScroll(rememberScrollState())
                .padding(DesignSystem.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(DesignSystem.SectionSpacing)
        ) {
            // Appearance Section
            SettingsSection(title = "🎨 Appearance") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Theme Selection", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    listOf("System Default" to 0, "Light Mode" to 1, "Dark Mode" to 2).forEach { (label, value) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onThemeModeChanged(value) }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = themeMode == value, onClick = { onThemeModeChanged(value) })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }
            }

            // Backup & Restore Section
            SettingsSection(title = "💾 Backup & Restore") {
                BackupSection(viewModel = viewModel, snackbarHostState = snackbarHostState)
            }

            // What's New Section
            SettingsSection(title = "✨ What's New") {
                ActionItem(
                    label = "View Release Notes", 
                    icon = Icons.Default.NewReleases,
                    onClick = { showWhatsNew = true }
                )
            }

            // About Section
            SettingsSection(title = "ℹ️ About CreatorLog") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "CreatorLog",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "A personal content management platform built for creators to organize content, ideas, and posting schedules.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    
                    InfoItem(label = "Version", value = "1.4.0")
                    InfoItem(label = "Build", value = "2026.08.V1.4")
                    InfoItem(label = "Developer", value = "ATHUL C")
                    
                    Text(
                        text = "Built with ❤️ for Content Creators",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = "Powered by Android Jetpack Compose, Material 3, and Room Database.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    
                    Text(
                        text = "© 2026 ATHUL C. All rights reserved.",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Support
            SettingsSection(title = "❤️ Support & Feedback") {
                ActionItem(label = "Report an Issue", icon = Icons.Default.BugReport)
                ActionItem(label = "Send Feedback", icon = Icons.Default.Feedback)
                ActionItem(label = "Rate Application", icon = Icons.Default.Star)
            }
            
            // Add extra space at the bottom for floating nav bar
            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    if (showWhatsNew) {
        WhatsNewDialog(onDismiss = { showWhatsNew = false })
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
                Text("CreatorLog 1.4.0", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
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
                        "Promotion Earnings Insights" to "View detailed financial performance directly from the Dashboard.",
                        "Accurate Monthly Earnings" to "Monthly earnings now reflect actual payments received."
                    )
                )

                WhatsNewSection(
                    title = "IMPROVEMENTS",
                    items = listOf(
                        "Faster Startup" to "Improved the app's cold-start experience.",
                        "Smoother Navigation" to "Improved transitions between Dashboard, Content, and Planner.",
                        "State Preservation" to "Scroll positions and search/filter states are preserved when switching between sections.",
                        "Improved Navigation UI" to "Improved navigation icon alignment and rendering."
                    )
                )

                WhatsNewSection(
                    title = "BUG FIXES",
                    items = listOf(
                        "Dashboard Loading" to "Fixed the Welcome Card/loading flicker during startup.",
                        "Promotion Earnings" to "Fixed pending promotions incorrectly affecting monthly earnings.",
                        "Payment Date" to "Promotions are now attributed to the month the payment was received, rather than the month the promotion was created.",
                        "Navigation Animation" to "Fixed the remaining navigation bar animation hitch."
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

@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DesignSystem.CornerRadiusLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(DesignSystem.BorderThickness, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(DesignSystem.CardPadding), verticalArrangement = Arrangement.spacedBy(DesignSystem.SpacingMedium)) {
            Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            content()
        }
    }
}

@Composable
fun InfoItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ActionItem(label: String, icon: ImageVector, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = DesignSystem.SpacingSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(DesignSystem.IconSizeMedium))
        Spacer(modifier = Modifier.width(DesignSystem.SpacingNormal))
        Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}
