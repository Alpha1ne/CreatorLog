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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    themeMode: Int,
    onThemeModeChanged: (Int) -> Unit,
    viewModel: TrackerViewModel
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
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
                BackupSection(viewModel = viewModel)
            }

            // Statistics Placeholder
            SettingsSection(title = "📊 Statistics") {
                Text("Detailed statistics and insights coming soon in a future update.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        text = "A personal content management app built for creators to organize songs, ideas, and posting schedules.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    
                    InfoItem(label = "Version", value = "1.2.0")
                    InfoItem(label = "Build", value = "2026.07.RC2")
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
}

@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
fun ActionItem(label: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable {}.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, fontWeight = FontWeight.Medium)
    }
}
