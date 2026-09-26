package com.creatorlog.app.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creatorlog.app.R
import com.creatorlog.app.ui.StreakDayActivity
import com.creatorlog.app.ui.TrackerViewModel
import com.creatorlog.app.ui.theme.*
import com.creatorlog.app.util.openInstagramApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyStreakScreen(
    viewModel: TrackerViewModel,
    onBack: () -> Unit
) {
    val stats by viewModel.statsState.collectAsState()

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("Daily Streak", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
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
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DesignSystem.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(DesignSystem.SectionSpacing)
        ) {
            Spacer(modifier = Modifier.height(DesignSystem.SpacingSmall))

            stats?.let { dashboardStats ->
                // 1. Creator Profile Section
                CreatorProfileSection()

                // 2. Current Streak Hero
                StreakHeroSection(streak = dashboardStats.currentStreak)

                // 3. Weekly Activity
                WeeklyActivitySection(weeklyData = dashboardStats.streakWeeklyActivity)

                // 4. Today's Status
                TodayStatusSection(isActiveToday = dashboardStats.streakWeeklyActivity.find { it.isToday }?.isActive ?: false)

                // 5. Next Milestone
                MilestoneProgressSection(
                    currentDays = dashboardStats.totalActiveDays,
                    nextMilestone = dashboardStats.nextMilestone,
                    progress = dashboardStats.milestoneProgress
                )

                // 6. Milestone Badges
                MilestoneBadgesSection(
                    achievedMilestones = dashboardStats.achievedMilestones
                )

                // 7. Streak Statistics
                StreakStatsSection(
                    longestStreak = dashboardStats.longestStreak,
                    totalActiveDays = dashboardStats.totalActiveDays
                )

                // 8. Motivational Card
                MotivationalSection()
            }

            Spacer(modifier = Modifier.height(116.dp))
        }
    }
}

fun normalizeUsername(input: String): String {
    val trimmed = input.trim()
    val noAt = trimmed.dropWhile { it == '@' }.trim()
    return if (noAt.isBlank()) "creatorlog" else noAt
}

@Composable
fun CreatorProfileSection() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("vof_settings", Context.MODE_PRIVATE) }
    
    var storedUsername by remember {
        val raw = prefs.getString("instagram_username", "creatorlog") ?: "creatorlog"
        mutableStateOf(normalizeUsername(raw))
    }
    var dpRefreshTrigger by remember { mutableIntStateOf(0) }
    var profileBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    val formattedUsername = remember(storedUsername) {
        normalizeUsername(storedUsername)
    }

    LaunchedEffect(dpRefreshTrigger) {
        val file = File(context.filesDir, "profile_dp.jpg")
        if (file.exists() && file.length() > 0) {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                } catch (_: Exception) {
                    null
                }
            }
            profileBitmap = bitmap
        } else {
            profileBitmap = null
        }
    }

    Card(
        onClick = { showEditDialog = true },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DesignSystem.CornerRadiusLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(DesignSystem.BorderThickness, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Circular 48.dp DP Container
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(listOf(PrimaryBlue, SecondaryPurple)),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (profileBitmap != null) {
                    Image(
                        bitmap = profileBitmap!!,
                        contentDescription = "Profile Picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            // Username and Subtitle
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formattedUsername,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Instagram Creator",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Open Instagram App Shortcut Button
            IconButton(
                onClick = { openInstagramApp(context) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_instagram),
                    contentDescription = "Open Instagram",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Edit Profile Button
            IconButton(
                onClick = { showEditDialog = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Profile",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    if (showEditDialog) {
        EditProfileDialog(
            currentUsername = storedUsername,
            hasDpImage = profileBitmap != null,
            onDismiss = { showEditDialog = false },
            onSaveUsername = { newUsername ->
                val normalized = normalizeUsername(newUsername)
                prefs.edit().putString("instagram_username", normalized).apply()
                storedUsername = normalized
                showEditDialog = false
            },
            onImageSelected = { uri ->
                val success = saveProfileDpImage(context, uri)
                if (success) {
                    dpRefreshTrigger++
                }
            },
            onRemoveImage = {
                removeProfileDpImage(context)
                dpRefreshTrigger++
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(
    currentUsername: String,
    hasDpImage: Boolean,
    onDismiss: () -> Unit,
    onSaveUsername: (String) -> Unit,
    onImageSelected: (Uri) -> Unit,
    onRemoveImage: () -> Unit
) {
    var usernameInput by remember { mutableStateOf(currentUsername) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            onImageSelected(it)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(DesignSystem.CornerRadiusLarge),
        title = {
            Text(
                text = "Edit Creator Profile",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Profile Picture",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(DesignSystem.CornerRadiusSmall)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Change Picture", style = MaterialTheme.typography.labelMedium)
                    }

                    if (hasDpImage) {
                        OutlinedButton(
                            onClick = onRemoveImage,
                            shape = RoundedCornerShape(DesignSystem.CornerRadiusSmall),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                Text(
                    text = "Instagram Username",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = { usernameInput = it },
                    label = { Text("Username") },
                    placeholder = { Text("e.g. mycreatorpage") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(DesignSystem.CornerRadiusSmall)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveUsername(usernameInput) },
                shape = RoundedCornerShape(DesignSystem.CornerRadiusSmall)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun saveProfileDpImage(context: Context, uri: Uri): Boolean {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return false
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        if (originalBitmap == null) return false

        // Center Crop to 1:1 Square
        val width = originalBitmap.width
        val height = originalBitmap.height
        val squareSize = minOf(width, height)
        val x = (width - squareSize) / 2
        val y = (height - squareSize) / 2

        val croppedBitmap = Bitmap.createBitmap(
            originalBitmap, x, y, squareSize, squareSize
        )

        // Scale to max 800x800 for visual sharpness & small file footprint
        val finalBitmap = if (squareSize > 800) {
            Bitmap.createScaledBitmap(croppedBitmap, 800, 800, true)
        } else {
            croppedBitmap
        }

        val file = File(context.filesDir, "profile_dp.jpg")
        file.outputStream().use { outputStream ->
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        }

        // Clean up memory
        if (croppedBitmap != originalBitmap && croppedBitmap != finalBitmap) {
            croppedBitmap.recycle()
        }
        if (finalBitmap != originalBitmap) {
            finalBitmap.recycle()
        }
        originalBitmap.recycle()

        true
    } catch (_: Exception) {
        false
    }
}

private fun removeProfileDpImage(context: Context) {
    try {
        val file = File(context.filesDir, "profile_dp.jpg")
        if (file.exists()) {
            file.delete()
        }
    } catch (_: Exception) {}
}

@Composable
fun StreakHeroSection(streak: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // CreatorLog-themed Blue -> Purple Flame
            Icon(
                imageVector = Icons.Default.Whatshot,
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .graphicsLayer(alpha = 0.99f)
                    .drawWithCache {
                        onDrawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(PrimaryBlue, SecondaryPurple)
                                ),
                                blendMode = BlendMode.SrcIn
                            )
                        }
                    },
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        Text(
            text = "$streak Day Streak",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Text(
            text = "Keep the streak alive!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun WeeklyActivitySection(weeklyData: List<StreakDayActivity>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DesignSystem.CornerRadiusLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(DesignSystem.BorderThickness, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "Weekly Activity",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weeklyData.forEach { day ->
                    WeeklyDayItem(day)
                }
            }
        }
    }
}

@Composable
fun WeeklyDayItem(day: StreakDayActivity) {
    val sdf = SimpleDateFormat("d", Locale.getDefault())
    val dateStr = sdf.format(Date(day.date))
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = day.dayName,
            style = MaterialTheme.typography.labelSmall,
            color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    when {
                        day.isActive -> MaterialTheme.colorScheme.primary
                        day.isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
                .then(
                    if (day.isToday && !day.isActive) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (day.isActive) {
                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
            } else if (day.isToday) {
                Text(text = "⭐", fontSize = 14.sp)
            }
        }
        
        Text(
            text = dateStr,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TodayStatusSection(isActiveToday: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DesignSystem.CornerRadiusLarge),
        colors = CardDefaults.cardColors(
            containerColor = if (isActiveToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) 
                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(
            DesignSystem.BorderThickness, 
            if (isActiveToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) 
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isActiveToday) Icons.Default.CheckCircle else Icons.Default.Info,
                contentDescription = null,
                tint = if (isActiveToday) StatusPosted else MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (isActiveToday) "✓ Active Today" else "○ No Activity Today",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (isActiveToday) StatusPosted else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun MilestoneProgressSection(currentDays: Int, nextMilestone: Int, progress: Float) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text(
                text = "Next Milestone",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$currentDays / $nextMilestone Days",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
        
        Text(
            text = "${nextMilestone - currentDays} days to go",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MilestoneBadgesSection(achievedMilestones: Map<Int, Long>) {
    val milestones = listOf(7, 14, 30, 50, 100)
    
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Milestones",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            milestones.take(3).forEach { milestone ->
                MilestoneBadgeItem(
                    milestone = milestone,
                    isAchieved = achievedMilestones.containsKey(milestone),
                    achievementDate = achievedMilestones[milestone],
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(0.66f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            milestones.drop(3).forEach { milestone ->
                MilestoneBadgeItem(
                    milestone = milestone,
                    isAchieved = achievedMilestones.containsKey(milestone),
                    achievementDate = achievedMilestones[milestone],
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun MilestoneBadgeItem(milestone: Int, isAchieved: Boolean, achievementDate: Long?, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(DesignSystem.CornerRadiusMedium),
        colors = CardDefaults.cardColors(
            containerColor = if (isAchieved) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) 
                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(
            DesignSystem.BorderThickness, 
            if (isAchieved) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) 
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isAchieved) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) 
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = if (isAchieved) MaterialTheme.colorScheme.primary 
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Text(
                text = "$milestone Days",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            if (isAchieved && achievementDate != null) {
                val sdf = SimpleDateFormat("dd MMM yy", Locale.getDefault())
                Text(
                    text = sdf.format(Date(achievementDate)),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            } else if (!isAchieved) {
                Text(
                    text = "Locked",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun StreakStatsSection(longestStreak: Int, totalActiveDays: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DesignSystem.CardSpacing)
    ) {
        StatItemCard(
            label = "Longest Streak",
            value = "$longestStreak Days",
            icon = Icons.Default.Timeline,
            modifier = Modifier.weight(1f)
        )
        StatItemCard(
            label = "Total Active Days",
            value = "$totalActiveDays Days",
            icon = Icons.Default.CalendarToday,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatItemCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(DesignSystem.CornerRadiusLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(DesignSystem.BorderThickness, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun MotivationalSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DesignSystem.CornerRadiusLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Consistency is the key to growth.",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Create consistently and keep your streak alive!",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
