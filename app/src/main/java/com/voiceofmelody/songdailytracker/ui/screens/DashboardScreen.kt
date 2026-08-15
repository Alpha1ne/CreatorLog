package com.voiceofmelody.songdailytracker.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voiceofmelody.songdailytracker.TrackerTab
import com.voiceofmelody.songdailytracker.data.model.IdeaVaultEntry
import com.voiceofmelody.songdailytracker.data.model.Reminder
import com.voiceofmelody.songdailytracker.data.model.SongPost
import com.voiceofmelody.songdailytracker.data.model.Promotion
import com.voiceofmelody.songdailytracker.ui.DashboardStats
import com.voiceofmelody.songdailytracker.ui.TrackerViewModel
import com.voiceofmelody.songdailytracker.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: TrackerViewModel, 
    onNavigateToAddEdit: (SongPost?) -> Unit,
    onNavigateToPromotions: () -> Unit,
    onNavigateToAddEditPromotion: (Promotion?) -> Unit,
    onTabSelected: (TrackerTab) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    onReady: () -> Unit = {}
) {
    val stats by viewModel.statsState.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(stats) {
        if (stats != null) {
            onReady()
        }
    }

    val lazyListState = rememberLazyListState()

    var selectedDateHub by remember { mutableStateOf<Long?>(null) }
    var hubSongs by remember { mutableStateOf<List<SongPost>>(emptyList()) }
    var hubReminders by remember { mutableStateOf<List<Reminder>>(emptyList()) }
    
    var reminderToEdit by remember { mutableStateOf<Reminder?>(null) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showIdeaDialog by remember { mutableStateOf(false) }

    LazyColumn(
        state = lazyListState,
        modifier = modifier.fillMaxSize().navigationBarsPadding(),
        contentPadding = PaddingValues(
            start = DesignSystem.ScreenPadding,
            top = DesignSystem.ScreenPadding,
            end = DesignSystem.ScreenPadding,
            bottom = 116.dp // Clear the Floating Nav Bar
        ),
        verticalArrangement = Arrangement.spacedBy(DesignSystem.SectionSpacing)
    ) {
        if (stats == null) {
            item {
                DashboardShimmerSkeleton()
            }
        } else if (stats!!.isLibraryEmpty) {
            item {
                WelcomeCard(onQuickAddClicked = { onNavigateToAddEdit(null) })
            }
        } else {
            val dashboardStats = stats!!
            
            // Secondary state collections moved inside LazyColumn scopes where needed
            
            // 1. Premium Hero Header
            item {
                DashboardPremiumHeroCard(stats = dashboardStats)
            }

            // 2. Productivity Summary
            item {
                ProductivitySummaryGrid(stats = dashboardStats)
            }

            // 3. Creator Calendar
            item {
                val songPosts by viewModel.allSongPosts.collectAsState()
                val reminders by viewModel.allReminders.collectAsState()
                
                Column(verticalArrangement = Arrangement.spacedBy(DesignSystem.SpacingMedium)) {
                    SectionTitle(title = "Creator Calendar", icon = Icons.Default.CalendarMonth)
                    CreatorCalendar(
                        songs = songPosts ?: emptyList(),
                        reminders = reminders ?: emptyList(),
                        now = System.currentTimeMillis(),
                        onDateSelected = { date, songs, items ->
                            selectedDateHub = date
                            hubSongs = songs
                            hubReminders = items
                        }
                    )
                }
            }

            // 4. Promotion Earnings Insight
            item {
                PromotionStatsCard(
                    stats = dashboardStats.promotionStats,
                    hasPromotions = dashboardStats.promotionStats.totalEarnings > 0,
                    onViewAll = onNavigateToPromotions,
                    onAddPromotion = { onNavigateToAddEditPromotion(null) }
                )
            }

            // 5. Today's Agenda
            item {
                val allIdeas by viewModel.allIdeas.collectAsState()
                val todayAgenda by viewModel.todayAgendaItems.collectAsState()
                
                TodayAgendaSection(
                    todaySongs = todayAgenda.first,
                    todayReminders = todayAgenda.second,
                    pendingIdeasCount = dashboardStats.pendingIdeasCount,
                    ideas = allIdeas ?: emptyList(),
                    onOpenSong = onNavigateToAddEdit,
                    onOpenReminders = {
                        val today = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        selectedDateHub = today
                        hubSongs = todayAgenda.first
                        hubReminders = todayAgenda.second
                    },
                    onOpenPlanner = { onTabSelected(TrackerTab.PLANNER) }
                )
            }

            // 5. Quick Actions
            item {
                QuickActionsHub(
                    onAddSong = { onNavigateToAddEdit(null) },
                    onAddIdea = { showIdeaDialog = true },
                    onAddReminder = {
                        reminderToEdit = null
                        selectedDateHub = System.currentTimeMillis()
                        showReminderDialog = true
                    }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Hub Bottom Sheet
    if (selectedDateHub != null) {
        DayDetailBottomSheet(
            selectedDate = selectedDateHub!!,
            songs = hubSongs,
            reminders = hubReminders,
            onDismiss = { selectedDateHub = null },
            onAddReminder = { 
                reminderToEdit = null
                showReminderDialog = true 
            },
            onEditReminder = { 
                reminderToEdit = it
                showReminderDialog = true 
            },
            onDeleteReminder = { reminder ->
                viewModel.deleteReminder(reminder)
                selectedDateHub = null
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "Reminder deleted",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoDeleteReminder()
                    }
                }
            },
            onOpenSong = { onNavigateToAddEdit(it); selectedDateHub = null }
        )
    }

    // Reminder Edit Dialog
    if (showReminderDialog) {
        ReminderEditDialog(
            reminder = reminderToEdit,
            initialDate = selectedDateHub ?: System.currentTimeMillis(),
            onDismiss = { showReminderDialog = false },
            onSave = { title, note, time, notify, repeat ->
                if (reminderToEdit == null) {
                    viewModel.addReminder(title, note, selectedDateHub!!, time, notify, null, repeat)
                    scope.launch { snackbarHostState.showSnackbar("Reminder created") }
                } else {
                    viewModel.updateReminder(reminderToEdit!!.id, title, note, selectedDateHub!!, time, notify, null, reminderToEdit!!.createdAt, repeat)
                    scope.launch { snackbarHostState.showSnackbar("Reminder updated") }
                }
                showReminderDialog = false
                selectedDateHub = null // Close hub to refresh
            }
        )
    }

    // Idea Dialog
    if (showIdeaDialog) {
        IdeaFormDialog(
            onDismiss = { showIdeaDialog = false },
            onSave = { title, content, category, color, isPosted ->
                viewModel.addIdea(title, content, category, color, isPosted)
                showIdeaDialog = false
                scope.launch { snackbarHostState.showSnackbar("Idea created") }
            }
        )
    }
}

@Composable
fun WelcomeCard(onQuickAddClicked: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("welcome_card"),
        shape = RoundedCornerShape(DesignSystem.CornerRadiusLarge),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(DesignSystem.CardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DesignSystem.SpacingMedium)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(DesignSystem.IconSizeExtraLarge)
            )
            Text(
                text = "Welcome to CreatorLog",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Start by recording your first piece of content to see your library grow!",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onQuickAddClicked,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DesignSystem.CornerRadiusSmall)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(DesignSystem.SpacingSmall))
                Text("Add First Content")
            }
        }
    }
}

@Composable
fun DashboardShimmerSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(DesignSystem.SectionSpacing)) {
        ShimmerCard(height = 180.dp) // Hero
        Row(horizontalArrangement = Arrangement.spacedBy(DesignSystem.CardSpacing)) {
            ShimmerCard(modifier = Modifier.weight(1f), height = 80.dp)
            ShimmerCard(modifier = Modifier.weight(1f), height = 80.dp)
            ShimmerCard(modifier = Modifier.weight(1f), height = 80.dp)
        }
        ShimmerCard(height = 300.dp) // Calendar
        ShimmerCard(height = 150.dp) // Agenda
    }
}

@Composable
fun DashboardPremiumHeroCard(stats: DashboardStats) {
    val animatedCount by animateIntAsState(
        targetValue = stats.postedSongsCount,
        animationSpec = tween(DesignSystem.AnimationDurationLong, easing = FastOutSlowInEasing),
        label = "heroCount"
    )

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) DesignSystem.PressScale else 1.0f,
        label = "hero_scale"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(DesignSystem.AnimationDurationMedium)) + scaleIn(tween(DesignSystem.AnimationDurationMedium, easing = FastOutSlowInEasing), initialScale = 0.92f)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {}
                ),
            shape = RoundedCornerShape(DesignSystem.CornerRadiusLarge),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            listOf(HeroGradientStart, HeroGradientEnd),
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset.Infinite
                        )
                    )
                    .padding(DesignSystem.CardPadding)
            ) {
                val greeting = remember { getTimeGreeting() }
                Column(verticalArrangement = Arrangement.spacedBy(DesignSystem.SpacingMedium)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = greeting,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "CreatorLog",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "Your Content Hub",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(64.dp)
                        )
                    }

                    Column {
                        Text(
                            text = animatedCount.toString(),
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "Posts Published",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}

private fun getTimeGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }
}

@Composable
fun SectionTitle(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignSystem.SpacingSmall),
        modifier = Modifier.padding(vertical = DesignSystem.SpacingTiny)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductivitySummaryGrid(stats: DashboardStats) {
    val metrics = remember(stats) {
        listOf(
            "Total Content" to stats.totalSongs,
            "Ideas" to (stats.postedIdeasCount + stats.pendingIdeasCount),
            "Tasks" to stats.totalReminders,
            "Posted" to stats.postedSongsCount,
            "Scheduled" to stats.scheduledSongsCount,
            "Duplicates" to stats.duplicateSongsCount
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(DesignSystem.SpacingMedium)) {
        SectionTitle(title = "Daily Summary", icon = Icons.Default.Dashboard)
        
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DesignSystem.CardSpacing),
            verticalArrangement = Arrangement.spacedBy(DesignSystem.CardSpacing),
            maxItemsInEachRow = 3
        ) {
            metrics.forEach { (label, value) ->
                Card(
                    modifier = Modifier.weight(1f).widthIn(min = 100.dp),
                    shape = RoundedCornerShape(DesignSystem.CornerRadiusLarge),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(DesignSystem.BorderThickness, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(DesignSystem.SpacingMedium),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun TodayAgendaSection(
    todaySongs: List<SongPost>,
    todayReminders: List<Reminder>,
    pendingIdeasCount: Int,
    ideas: List<IdeaVaultEntry>,
    onOpenSong: (SongPost) -> Unit,
    onOpenReminders: () -> Unit,
    onOpenPlanner: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(DesignSystem.SpacingMedium)) {
        SectionTitle(title = "Today's Agenda", icon = Icons.AutoMirrored.Filled.EventNote)

        if (todaySongs.isEmpty() && todayReminders.isEmpty() && pendingIdeasCount == 0) {
            PremiumEmptyState(
                icon = Icons.Default.DoneAll,
                title = "All clear for today!",
                subtitle = "Enjoy your free time or start planning your next post."
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(DesignSystem.SpacingSmall)) {
                // Today's Songs
                todaySongs.forEach { song ->
                    AgendaItem(
                        title = song.title,
                        subtitle = "Scheduled Content",
                        icon = Icons.Default.AutoAwesome,
                        color = StatusPosted,
                        onClick = { onOpenSong(song) }
                    )
                }

                // Today's Reminders
                todayReminders.forEach { reminder ->
                    AgendaItem(
                        title = reminder.title,
                        subtitle = "Reminder",
                        icon = Icons.Default.Notifications,
                        color = StatusReminder,
                        onClick = onOpenReminders
                    )
                }

                // Pending Ideas
                if (pendingIdeasCount > 0) {
                    val displayIdeas = ideas.filter { !it.isPosted }.take(3)
                    displayIdeas.forEach { idea ->
                        AgendaItem(
                            title = idea.title,
                            subtitle = "Idea in Workspace",
                            icon = Icons.Default.Lightbulb,
                            color = MaterialTheme.colorScheme.primary,
                            onClick = onOpenPlanner
                        )
                    }
                    if (pendingIdeasCount > 3) {
                        TextButton(
                            onClick = onOpenPlanner,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("View all ${pendingIdeasCount} ideas")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AgendaItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(DesignSystem.CornerRadiusLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(DesignSystem.BorderThickness, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(DesignSystem.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignSystem.SpacingLarge)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        }
    }
}

@Composable
fun QuickActionsHub(
    onAddSong: () -> Unit,
    onAddIdea: () -> Unit,
    onAddReminder: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(DesignSystem.SpacingMedium)) {
        SectionTitle(title = "Quick Actions", icon = Icons.Default.RocketLaunch)
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DesignSystem.CardSpacing)
        ) {
            ActionCard(
                title = "Add Content",
                icon = Icons.Default.AddCircle,
                color = MaterialTheme.colorScheme.primary,
                onClick = onAddSong,
                modifier = Modifier.weight(1f)
            )
            ActionCard(
                title = "New Idea",
                icon = Icons.Default.Lightbulb,
                color = StatusScheduled,
                onClick = onAddIdea,
                modifier = Modifier.weight(1f)
            )
            ActionCard(
                title = "Reminder",
                icon = Icons.Default.NotificationsActive,
                color = StatusReminder,
                onClick = onAddReminder,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ActionCard(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) DesignSystem.PressScale else 1.0f,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "action_scale"
    )

    Card(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            ),
        shape = RoundedCornerShape(DesignSystem.CornerRadiusLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(DesignSystem.BorderThickness, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(DesignSystem.SpacingMedium).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DesignSystem.SpacingSmall)
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                color = color.copy(alpha = 0.1f),
                shape = CircleShape
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
