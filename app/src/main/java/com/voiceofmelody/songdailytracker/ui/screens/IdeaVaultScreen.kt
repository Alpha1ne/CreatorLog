package com.voiceofmelody.songdailytracker.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.voiceofmelody.songdailytracker.TrackerTab
import com.voiceofmelody.songdailytracker.data.model.IdeaVaultEntry
import com.voiceofmelody.songdailytracker.ui.IdeaFilter
import com.voiceofmelody.songdailytracker.ui.TrackerViewModel
import com.voiceofmelody.songdailytracker.ui.ViewMode
import com.voiceofmelody.songdailytracker.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

val IdeaLabelColors = listOf(
    Color.Transparent, // No color
    StatusReminder, // Red
    StatusScheduled, // Orange
    Color(0xFFFDD835), // Yellow
    StatusPosted, // Green
    AccentAzure, // Cyan/Blue
    SecondaryPurple, // Purple
    StatusDuplicate, // Pink -> Duplicate color
    Color(0xFF64748B), // Gray -> Standard Secondary Text
)

val IdeaCategories = listOf(
    "Reel",
    "Post",
    "Story",
    "Caption",
    "Track",
    "Content Plan",
    "Random Idea"
)

@Composable
private fun getCategoryIcon(category: String?): androidx.compose.ui.graphics.vector.ImageVector {
    return when (category) {
        "Reel" -> Icons.Default.Movie
        "Post" -> Icons.Default.Photo
        "Story" -> Icons.Default.AutoAwesome
        "Caption" -> Icons.AutoMirrored.Filled.Article
        "Track" -> Icons.Default.MusicNote
        "Content Plan" -> Icons.Default.CalendarMonth
        "Random Idea" -> Icons.Default.Lightbulb
        else -> Icons.Default.Lightbulb
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeaVaultScreen(
    viewModel: TrackerViewModel, 
    onTabSelected: (TrackerTab) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val ideas by viewModel.searchedIdeas.collectAsState()
    val allIdeas by viewModel.allIdeas.collectAsState()
    val searchQuery by viewModel.ideasSearchQuery.collectAsState()
    val currentFilter by viewModel.ideasFilter.collectAsState()
    val haptic = LocalHapticFeedback.current
    
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("vof_settings", android.content.Context.MODE_PRIVATE) }
    var viewMode by rememberSaveable { 
        mutableStateOf(ViewMode.entries[sharedPrefs.getInt("planner_view_mode", 0)]) 
    }
    var showFiltersPanel by rememberSaveable { mutableStateOf(true) }

    val lazyStaggeredGridState = rememberLazyStaggeredGridState()

    var isInitialLoading by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        if (isInitialLoading) {
            isInitialLoading = false
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var ideaToEdit by remember { mutableStateOf<IdeaVaultEntry?>(null) }
    var ideaToDelete by remember { mutableStateOf<IdeaVaultEntry?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Unified Search Toolbar Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = DesignSystem.ScreenPadding, end = DesignSystem.ScreenPadding, top = DesignSystem.ScreenPadding, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UnifiedSearchToolbar(
                    query = searchQuery,
                    onQueryChange = { viewModel.ideasSearchQuery.value = it },
                    placeholder = "Search ideas, categories...",
                    showFiltersPanel = showFiltersPanel,
                    onFilterToggle = { showFiltersPanel = !showFiltersPanel },
                    viewMode = viewMode,
                    onViewModeToggle = { newMode ->
                        viewMode = newMode
                        sharedPrefs.edit { putInt("planner_view_mode", newMode.ordinal) }
                    },
                    testTag = "ideas_search_input"
                )
            }

            // Controls Row (Filter Chips & View Mode Toggle)
            AnimatedVisibility(visible = showFiltersPanel) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DesignSystem.ScreenPadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Filter Chips (Scrollable)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(DesignSystem.SpacingSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IdeaFilter.entries.forEach { filter ->
                            val count by produceState<Int>(initialValue = 0, allIdeas, filter) {
                                value = withContext(Dispatchers.Default) {
                                    when (filter) {
                                        IdeaFilter.ALL -> allIdeas?.size ?: 0
                                        IdeaFilter.PENDING -> allIdeas?.count { !it.isPosted } ?: 0
                                        IdeaFilter.POSTED -> allIdeas?.count { it.isPosted } ?: 0
                                    }
                                }
                            }
                            FilterChip(
                                selected = currentFilter == filter,
                                onClick = { viewModel.ideasFilter.value = filter },
                                label = { 
                                    Text(
                                        text = "${filter.name.lowercase().replaceFirstChar { it.uppercase() }} ($count)",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (currentFilter == filter) FontWeight.Bold else FontWeight.Medium
                                    ) 
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                shape = RoundedCornerShape(DesignSystem.CornerRadiusChip),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = currentFilter == filter,
                                    borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.height(DesignSystem.ChipHeight)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(DesignSystem.SpacingNormal))

            if (allIdeas == null && isInitialLoading) {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    contentPadding = PaddingValues(DesignSystem.ScreenPadding, DesignSystem.SpacingSmall, DesignSystem.ScreenPadding, 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(DesignSystem.CardSpacing),
                    verticalItemSpacing = DesignSystem.CardSpacing
                ) {
                    items(6, key = { it }, contentType = { "shimmer" }) { ShimmerCard(height = (150..250).random().dp) }
                }
            } else {
                val isActive by remember { derivedStateOf { searchQuery.isNotEmpty() || currentFilter != IdeaFilter.ALL } }
                if (ideas.isEmpty()) {
                    EmptyIdeasState(
                        isSearchActive = isActive,
                        onAddFirst = { showAddDialog = true }
                    )
                } else {
                    if (viewMode == ViewMode.LIST) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(DesignSystem.ScreenPadding, DesignSystem.SpacingSmall, DesignSystem.ScreenPadding, 120.dp),
                            verticalArrangement = Arrangement.spacedBy(DesignSystem.CardSpacing)
                        ) {
                            items(
                                items = ideas,
                                key = { it.id },
                                contentType = { "idea_card" }
                            ) { idea ->
                                IdeaCard(
                                    idea = idea,
                                    onClick = { ideaToEdit = idea },
                                    onPinToggle = { 
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.togglePin(idea) 
                                    },
                                    onDelete = { ideaToDelete = idea }
                                )
                            }
                        }
                    } else {
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(2),
                            state = lazyStaggeredGridState,
                            modifier = Modifier.fillMaxSize().testTag("planner_grid"),
                            contentPadding = PaddingValues(DesignSystem.ScreenPadding, DesignSystem.SpacingSmall, DesignSystem.ScreenPadding, 120.dp),
                            horizontalArrangement = Arrangement.spacedBy(DesignSystem.CardSpacing),
                            verticalItemSpacing = DesignSystem.CardSpacing
                        ) {
                            items(
                                items = ideas,
                                key = { it.id },
                                contentType = { "idea_grid_card" }
                            ) { idea ->
                                IdeaGridCard(
                                    idea = idea,
                                    onClick = { ideaToEdit = idea },
                                    onPinToggle = { 
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.togglePin(idea) 
                                    },
                                    onDelete = { ideaToDelete = idea }
                                )
                            }
                        }
                    }
                }
            }
        }

        CreatorLogFAB(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_idea_fab"),
            contentDescription = "Add Idea"
        )
    }

    if (showAddDialog) {
        val scope = rememberCoroutineScope()
        IdeaFormDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, content, category, color, isPosted ->
                viewModel.addIdea(title, content, category, color, isPosted)
                showAddDialog = false
                scope.launch { snackbarHostState.showSnackbar("Idea created") }
            }
        )
    }

    if (ideaToEdit != null) {
        val scope = rememberCoroutineScope()
        IdeaFormDialog(
            editingIdea = ideaToEdit,
            onDismiss = { ideaToEdit = null },
            onSave = { title, content, category, color, isPosted ->
                ideaToEdit?.let {
                    viewModel.updateIdea(it.id, title, content, category, color, isPosted, it.isPinned, it.createdAt)
                }
                ideaToEdit = null
                scope.launch { snackbarHostState.showSnackbar("Idea updated") }
            }
        )
    }

    if (ideaToDelete != null) {
        val scope = rememberCoroutineScope()
        AlertDialog(
            onDismissRequest = { ideaToDelete = null },
            title = { Text("Delete Idea?") },
            text = { Text("Are you sure you want to permanently delete this idea from your planner?") },
            confirmButton = {
                Button(
                    onClick = {
                        val idea = ideaToDelete!!
                        viewModel.deleteIdea(idea)
                        ideaToDelete = null
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Idea deleted",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.undoDeleteIdea()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { ideaToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeaCard(
    idea: IdeaVaultEntry,
    onClick: () -> Unit,
    onPinToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val dateText = remember(idea.updatedAt) {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        sdf.format(Date(idea.updatedAt))
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(DesignSystem.AnimationDurationMedium)) + scaleIn(tween(DesignSystem.AnimationDurationMedium), initialScale = 0.95f)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .workspacePressAnimation()
                .clickable(
                    onClick = onClick
                )
                .testTag("idea_card_${idea.id}"),
            shape = RoundedCornerShape(DesignSystem.CornerRadiusLarge),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(DesignSystem.BorderThickness, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 1.dp,
                pressedElevation = 4.dp
            )
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Category Accent (Left Border)
                if (idea.color != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(6.dp)
                            .fillMaxHeight()
                            .background(Color(idea.color))
                    )
                }

                Column(modifier = Modifier.padding(DesignSystem.CardPadding)) {
                    // TOP ROW: Category & Pin
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = { /* Chip is non-functional in card */ },
                            label = { Text(idea.category ?: "Idea", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) },
                            leadingIcon = {
                                Icon(
                                    getCategoryIcon(idea.category),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (idea.color != null) Color(idea.color) else MaterialTheme.colorScheme.primary
                                )
                            },
                            shape = CircleShape,
                            border = AssistChipDefaults.assistChipBorder(
                                enabled = true,
                                borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (idea.color != null) Color(idea.color).copy(alpha = 0.05f) 
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.height(DesignSystem.ChipHeight)
                        )
                        
                        IconButton(
                            onClick = onPinToggle,
                            modifier = Modifier.size(32.dp).alpha(if (idea.isPinned) 1f else 0.4f)
                        ) {
                            Icon(
                                imageVector = if (idea.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                contentDescription = "Pin",
                                tint = if (idea.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(DesignSystem.SpacingMedium))

                    // TITLE
                    Text(
                        text = idea.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (idea.isPosted) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (idea.isPosted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(DesignSystem.SpacingSmall))

                    // STATUS BADGE
                    Surface(
                        color = if (idea.isPosted) StatusPosted.copy(alpha = 0.12f) else StatusScheduled.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(DesignSystem.CornerRadiusBadge),
                        modifier = Modifier.height(DesignSystem.StatusBadgeHeight)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(if (idea.isPosted) StatusPosted else StatusScheduled, CircleShape)
                            )
                            Text(
                                text = if (idea.isPosted) "Posted" else "Pending",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (idea.isPosted) StatusPosted else StatusScheduled
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(DesignSystem.SpacingMedium))

                    // DESCRIPTION
                    Text(
                        text = idea.content,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.alpha(if (idea.isPosted) 0.6f else 1.0f)
                    )

                    Spacer(modifier = Modifier.height(DesignSystem.SpacingLarge))

                    // FOOTER: Date & Delete
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Modified $dateText",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        )
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(48.dp).alpha(0.3f) // Standard touch target
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeaGridCard(
    idea: IdeaVaultEntry,
    onClick: () -> Unit,
    onPinToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val dateText = remember(idea.updatedAt) {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        sdf.format(Date(idea.updatedAt))
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(DesignSystem.AnimationDurationMedium)) + scaleIn(tween(DesignSystem.AnimationDurationMedium), initialScale = 0.95f)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .workspacePressAnimation()
                .clickable(
                    onClick = onClick
                )
                .testTag("idea_grid_card_${idea.id}"),
            shape = RoundedCornerShape(DesignSystem.CornerRadiusLarge),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(DesignSystem.BorderThickness, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 1.dp,
                pressedElevation = 4.dp
            )
        ) {
            Column(modifier = Modifier.padding(DesignSystem.SpacingMedium)) {
                // TOP: Category Icon & Pin
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(DesignSystem.IconSizeLarge)
                            .background(
                                color = if (idea.color != null) Color(idea.color).copy(alpha = 0.1f) 
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            getCategoryIcon(idea.category),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (idea.color != null) Color(idea.color) else MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    IconButton(
                        onClick = onPinToggle,
                        modifier = Modifier.size(24.dp).alpha(if (idea.isPinned) 1f else 0.4f)
                    ) {
                        Icon(
                            imageVector = if (idea.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin",
                            tint = if (idea.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // TITLE
                Text(
                    text = idea.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (idea.isPosted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (idea.isPosted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(DesignSystem.SpacingSmall))

                // CONTENT
                Text(
                    text = idea.content,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 8,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.alpha(if (idea.isPosted) 0.6f else 1.0f)
                )

                Spacer(modifier = Modifier.height(DesignSystem.SpacingMedium))

                // FOOTER: Status Icon & Date & Delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DesignSystem.SpacingSmall)) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (idea.isPosted) StatusPosted else StatusScheduled, CircleShape)
                        )
                        Text(
                            text = dateText.split(",")[0], // Just date for grid
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        )
                    }
                    
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(48.dp).alpha(0.3f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyIdeasState(isSearchActive: Boolean, onAddFirst: () -> Unit) {
    PremiumEmptyState(
        icon = if (isSearchActive) Icons.Default.SearchOff else Icons.Default.AutoAwesome,
        title = if (isSearchActive) "No ideas found" else "Creator Workspace is empty",
        subtitle = if (isSearchActive) "Try different keywords or reset your filters." else "Capture your next great idea. Your creative spark starts here!",
        actionLabel = if (isSearchActive) null else "Create First Idea",
        onAction = if (isSearchActive) null else onAddFirst
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeaFormDialog(
    editingIdea: IdeaVaultEntry? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, category: String?, color: Long?, isPosted: Boolean) -> Unit
) {
    var title by remember { mutableStateOf(editingIdea?.title ?: "") }
    var content by remember { mutableStateOf(editingIdea?.content ?: "") }
    var category by remember { mutableStateOf(editingIdea?.category ?: IdeaCategories.first()) }
    var isCustomCategory by remember { mutableStateOf(editingIdea?.category != null && editingIdea.category !in IdeaCategories) }
    var customCategory by remember { mutableStateOf(if (isCustomCategory) editingIdea?.category ?: "" else "") }
    var selectedColor by remember { mutableStateOf(editingIdea?.color?.let { Color(it) } ?: IdeaLabelColors.first()) }
    var isPosted by remember { mutableStateOf(editingIdea?.isPosted ?: false) }

    val createdDateText = remember(editingIdea) {
        editingIdea?.let {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it.createdAt))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (editingIdea == null) "New Idea" else "Edit Idea",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        shape = RoundedCornerShape(DesignSystem.CornerRadiusLarge),
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(DesignSystem.SpacingMedium)
            ) {
            if (createdDateText != null) {
                Text(
                    text = "Created on $createdDateText",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(DesignSystem.CornerRadiusSmall)
                )

                Column {
                    OutlinedTextField(
                        value = content,
                        onValueChange = { if (it.length <= 1000) content = it },
                        label = { Text("Idea details...") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                        shape = RoundedCornerShape(DesignSystem.CornerRadiusSmall)
                    )
                    Text(
                        text = "${content.length}/1000",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
                        color = if (content.length > 900) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Category Selection
                Text("Category", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                var categoryExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = if (isCustomCategory) "Custom: $customCategory" else category,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        shape = RoundedCornerShape(DesignSystem.CornerRadiusSmall)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        IdeaCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(getCategoryIcon(cat), contentDescription = null, modifier = Modifier.size(DesignSystem.IconSizeSmall), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(DesignSystem.SpacingNormal))
                                        Text(cat)
                                    }
                                },
                                onClick = {
                                    category = cat
                                    isCustomCategory = false
                                    categoryExpanded = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                            text = { Text("Custom...") },
                            onClick = {
                                isCustomCategory = true
                                categoryExpanded = false
                            }
                        )
                    }
                }

                if (isCustomCategory) {
                    OutlinedTextField(
                        value = customCategory,
                        onValueChange = { customCategory = it },
                        label = { Text("Custom Category Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(DesignSystem.CornerRadiusSmall)
                    )
                }

                // Color Selection
                Text("Color Label", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DesignSystem.SpacingSmall)
                ) {
                    IdeaLabelColors.forEach { color ->
                        val isSelected = selectedColor == color
                        Box(
                            modifier = Modifier
                                .size(DesignSystem.IconSizeLarge)
                                .clip(CircleShape)
                                .background(if (color == Color.Transparent) MaterialTheme.colorScheme.surfaceVariant else color)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = color },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (color == Color.Transparent) MaterialTheme.colorScheme.primary else Color.White
                                )
                            } else if (color == Color.Transparent) {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Posted Checkbox
                val haptic = LocalHapticFeedback.current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = DesignSystem.SpacingSmall)
                        .clickable { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isPosted = !isPosted 
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isPosted,
                        onCheckedChange = { isPosted = it }
                    )
                    Spacer(modifier = Modifier.width(DesignSystem.SpacingSmall))
                    Text("Posted to Instagram", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            }
        },
        confirmButton = {
            val haptic = LocalHapticFeedback.current
            Button(
                onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val finalCategory = if (isCustomCategory) customCategory.ifBlank { "Custom" } else category
                    onSave(title, content, finalCategory, if (selectedColor == Color.Transparent) null else selectedColor.toArgb().toLong(), isPosted) 
                },
                enabled = title.isNotBlank() && content.isNotBlank(),
                shape = RoundedCornerShape(DesignSystem.CornerRadiusSmall)
            ) {
                Text("Save to Workspace")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
