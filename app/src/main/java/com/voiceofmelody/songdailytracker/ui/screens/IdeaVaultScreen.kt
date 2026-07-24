package com.voiceofmelody.songdailytracker.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.voiceofmelody.songdailytracker.data.model.IdeaVaultEntry
import com.voiceofmelody.songdailytracker.ui.IdeaFilter
import com.voiceofmelody.songdailytracker.ui.TrackerViewModel
import com.voiceofmelody.songdailytracker.ui.ViewMode
import java.text.SimpleDateFormat
import java.util.*

val IdeaLabelColors = listOf(
    Color.Transparent, // No color
    Color(0xFFE53935), // Red
    Color(0xFFFB8C00), // Orange
    Color(0xFFFDD835), // Yellow
    Color(0xFF43A047), // Green
    Color(0xFF00ACC1), // Cyan
    Color(0xFF1E88E5), // Blue
    Color(0xFF8E24AA), // Purple
    Color(0xFFD81B60), // Pink
    Color(0xFF616161), // Gray
)

val IdeaCategories = listOf(
    "Reel",
    "Post",
    "Story",
    "Caption",
    "Song",
    "Content Plan",
    "Random Idea"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeaVaultScreen(viewModel: TrackerViewModel, modifier: Modifier = Modifier) {
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

    val lazyGridState = rememberLazyGridState()

    var showAddDialog by remember { mutableStateOf(false) }
    var ideaToEdit by remember { mutableStateOf<IdeaVaultEntry?>(null) }
    var ideaToDelete by remember { mutableStateOf<IdeaVaultEntry?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Unified Search Toolbar Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
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
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Filter Chips (Scrollable)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IdeaFilter.entries.forEach { filter ->
                            val count = when (filter) {
                                IdeaFilter.ALL -> allIdeas.size
                                IdeaFilter.PENDING -> allIdeas.count { !it.isPosted }
                                IdeaFilter.POSTED -> allIdeas.count { it.isPosted }
                            }
                            FilterChip(
                                selected = currentFilter == filter,
                                onClick = { viewModel.ideasFilter.value = filter },
                                label = { 
                                    Text(
                                        text = "${filter.name.lowercase().replaceFirstChar { it.uppercase() }} ($count)",
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    ) 
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (ideas.isEmpty()) {
                val isActive = searchQuery.isNotEmpty() || currentFilter != IdeaFilter.ALL
                EmptyIdeasState(
                    isSearchActive = isActive,
                    onAddFirst = { showAddDialog = true }
                )
            } else {
                if (viewMode == ViewMode.LIST) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 120.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(ideas, key = { it.id }) { idea ->
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
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = lazyGridState,
                        modifier = Modifier.fillMaxSize().testTag("planner_grid"),
                        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 120.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(ideas, key = { it.id }) { idea ->
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
        IdeaFormDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, content, category, color, isPosted ->
                viewModel.addIdea(title, content, category, color, isPosted)
                showAddDialog = false
            }
        )
    }

    if (ideaToEdit != null) {
        IdeaFormDialog(
            editingIdea = ideaToEdit,
            onDismiss = { ideaToEdit = null },
            onSave = { title, content, category, color, isPosted ->
                ideaToEdit?.let {
                    viewModel.updateIdea(it.id, title, content, category, color, isPosted, it.isPinned, it.createdAt)
                }
                ideaToEdit = null
            }
        )
    }

    if (ideaToDelete != null) {
        AlertDialog(
            onDismissRequest = { ideaToDelete = null },
            title = { Text("Delete Idea?") },
            text = { Text("Are you sure you want to permanently delete this idea from your planner?") },
            confirmButton = {
                Button(
                    onClick = {
                        ideaToDelete?.let { viewModel.deleteIdea(it) }
                        ideaToDelete = null
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

    // Success animation for scale/check icon
    val checkIconScale by animateFloatAsState(
        targetValue = if (idea.isPosted) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "check_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (idea.isPosted) 0.98f else 1.0f) // Subtle scale animation
            .alpha(if (idea.isPosted) 0.9f else 1.0f)
            .clickable(onClick = onClick)
            .testTag("idea_card_${idea.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (idea.color != null) Color(idea.color).copy(alpha = 0.25f) 
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (idea.category != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = idea.category,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = idea.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (idea.isPosted) TextDecoration.LineThrough else TextDecoration.None
                    )
                    
                    if (idea.isPosted) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF43A047),
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .size(18.dp)
                                .scale(checkIconScale)
                        )
                    }
                }
                
                IconButton(onClick = onPinToggle, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = if (idea.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Pin",
                        tint = if (idea.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (idea.isPosted) {
                    Surface(
                        color = Color(0xFF43A047),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Posted",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Surface(
                        color = Color(0xFFFB8C00),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Pending",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = idea.content,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Edited $dateText",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun IdeaGridCard(
    idea: IdeaVaultEntry,
    onClick: () -> Unit,
    onPinToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val dateText = remember(idea.updatedAt) {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        sdf.format(Date(idea.updatedAt))
    }

    val checkIconScale by animateFloatAsState(
        targetValue = if (idea.isPosted) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "check_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (idea.isPosted) 0.98f else 1.0f)
            .alpha(if (idea.isPosted) 0.9f else 1.0f)
            .clickable(onClick = onClick)
            .testTag("idea_grid_card_${idea.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (idea.color != null) Color(idea.color).copy(alpha = 0.25f) 
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                if (idea.category != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = idea.category,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                
                IconButton(onClick = onPinToggle, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = if (idea.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Pin",
                        tint = if (idea.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = idea.title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (idea.isPosted) TextDecoration.LineThrough else TextDecoration.None
            )
            
            if (idea.isPosted) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF43A047),
                        modifier = Modifier.size(14.dp).scale(checkIconScale)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Posted", fontSize = 9.sp, color = Color(0xFF43A047), fontWeight = FontWeight.Bold)
                }
            } else {
                Text("Pending", fontSize = 9.sp, color = Color(0xFFFB8C00), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = idea.content,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateText,
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyIdeasState(isSearchActive: Boolean, onAddFirst: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(100.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = CircleShape
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isSearchActive) Icons.Default.SearchOff else Icons.Outlined.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            Text(
                text = if (isSearchActive) "No ideas found" else "No Ideas Yet",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = if (isSearchActive) "Try a different search query or reset your filters." 
                       else "Capture your creative sparks for reels, posts, and captions. Your next viral post starts here!",
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!isSearchActive) {
                Button(
                    onClick = onAddFirst,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create First Idea")
                }
            }
        }
    }
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
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            if (createdDateText != null) {
                Text(
                    text = "Created on $createdDateText",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Column {
                    OutlinedTextField(
                        value = content,
                        onValueChange = { if (it.length <= 1000) content = it },
                        label = { Text("Idea details...") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Text(
                        text = "${content.length}/1000",
                        fontSize = 10.sp,
                        modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
                        color = if (content.length > 900) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Category Selection
                Text("Category", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        IdeaCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    isCustomCategory = false
                                    categoryExpanded = false
                                }
                            )
                        }
                        DropdownMenuItem(
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
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Color Selection
                Text("Color Label", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IdeaLabelColors.forEach { color ->
                        val isSelected = selectedColor == color
                        Box(
                            modifier = Modifier
                                .size(32.dp)
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clickable { isPosted = !isPosted },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isPosted,
                        onCheckedChange = { isPosted = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Posted to Instagram", fontWeight = FontWeight.Medium)
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
                enabled = title.isNotBlank() && content.isNotBlank()
            ) {
                Text("Save to Planner")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
