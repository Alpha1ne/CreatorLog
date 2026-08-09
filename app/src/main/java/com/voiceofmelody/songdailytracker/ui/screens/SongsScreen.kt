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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
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
import com.voiceofmelody.songdailytracker.TrackerTab
import com.voiceofmelody.songdailytracker.data.model.SongPost
import com.voiceofmelody.songdailytracker.ui.DuplicateGroup
import com.voiceofmelody.songdailytracker.ui.MatchLevel
import com.voiceofmelody.songdailytracker.ui.SongStatus
import com.voiceofmelody.songdailytracker.ui.TrackerViewModel
import com.voiceofmelody.songdailytracker.ui.ViewMode
import com.voiceofmelody.songdailytracker.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds
import java.text.SimpleDateFormat
import java.util.*

enum class DateFilter(val displayName: String) {
    ALL("All"),
    POSTED("Posted"),
    SCHEDULED("Scheduled"),
    NO_DATE("No Date"),
    DUPLICATE("Duplicate")
}

enum class SortOption(val displayName: String) {
    NEWEST("Newest First"),
    OLDEST("Oldest First"),
    SCHEDULED_FIRST("Scheduled First"),
    POSTED_FIRST("Posted First"),
    ALPHABETICAL("Alphabetical")
}

fun cleanStringForComparison(input: String): String {
    return input
        .lowercase()
        .trim()
        .replace(Regex("\\s+"), " ")
        .replace(Regex("[^a-zA-Z0-9\\s]"), "")
}

@Composable
fun StatusBadge(status: SongStatus?) {
    if (status == null) return

    val color = when (status) {
        SongStatus.SCHEDULED -> StatusScheduled
        SongStatus.POSTED -> StatusPosted
    }
    
    val text = when (status) {
        SongStatus.SCHEDULED -> "Scheduled"
        SongStatus.POSTED -> "Posted"
    }

    Surface(
        modifier = Modifier.height(DesignSystem.StatusBadgeHeight),
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(DesignSystem.CornerRadiusBadge),
        border = BorderStroke(DesignSystem.BorderThickness, color.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = DesignSystem.SpacingNormal),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignSystem.SpacingTiny + 2.dp)
        ) {
            Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongsScreen(
    viewModel: TrackerViewModel, 
    onNavigateToAddEdit: (SongPost?) -> Unit,
    onTabSelected: (TrackerTab) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val searchResults by viewModel.searchedSongs.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val allSongs by viewModel.allSongPosts.collectAsState()
    val now by viewModel.currentTime.collectAsState()
    
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("vof_settings", android.content.Context.MODE_PRIVATE) }
    var viewMode by rememberSaveable { 
        mutableStateOf(ViewMode.entries[sharedPrefs.getInt("songs_view_mode", 0)]) 
    }

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()
    var highlightedSongId by rememberSaveable { mutableIntStateOf(-1) }
    
    var isInitialLoading by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        if (isInitialLoading) {
            isInitialLoading = false
        }
    }

    var songToDelete by remember { mutableStateOf<SongPost?>(null) }
    
    // Filters State
    var showFiltersPanel by rememberSaveable { mutableStateOf(false) }
    var dateFilter by rememberSaveable { mutableStateOf(DateFilter.ALL) }
    var sortBy by rememberSaveable { mutableStateOf(SortOption.NEWEST) }
    var selectedLanguage by rememberSaveable { mutableStateOf("All") }
    var selectedSinger by rememberSaveable { mutableStateOf("All") }
    var selectedMovie by rememberSaveable { mutableStateOf("All") }

    // Auto-clear highlight
    LaunchedEffect(highlightedSongId) {
        if (highlightedSongId != -1) {
            delay(3.seconds)
            highlightedSongId = -1
        }
    }

    // Background Metadata Processing (O(N) operations moved off-main)
    val languagesList by produceState<List<String>>(initialValue = emptyList(), allSongs) {
        value = withContext(Dispatchers.Default) {
            allSongs?.asSequence()?.map { it.language.trim() }?.filter { it.isNotBlank() }?.distinct()?.sorted()?.toList() ?: emptyList()
        }
    }
    val singersList by produceState<List<String>>(initialValue = emptyList(), allSongs) {
        value = withContext(Dispatchers.Default) {
            allSongs?.asSequence()?.flatMap { it.singers.split(",") }?.map { it.trim() }?.filter { it.isNotBlank() }?.distinct()?.sorted()?.toList() ?: emptyList()
        }
    }
    val moviesList by produceState<List<String>>(initialValue = emptyList(), allSongs) {
        value = withContext(Dispatchers.Default) {
            allSongs?.asSequence()?.map { it.movieName.trim() }?.filter { it.isNotBlank() }?.distinct()?.sorted()?.toList() ?: emptyList()
        }
    }

    // Apply Client-Side Filters & Sort
    val processedSongsList by remember(searchResults, dateFilter, selectedLanguage, selectedSinger, selectedMovie, sortBy, now, allSongs) {
        derivedStateOf {
            var list = searchResults
            
            // Status Filtering
            list = when (dateFilter) {
                DateFilter.ALL -> list
                DateFilter.POSTED -> list.filter { viewModel.getSongStatus(it, now) == SongStatus.POSTED }
                DateFilter.SCHEDULED -> list.filter { viewModel.getSongStatus(it, now) == SongStatus.SCHEDULED }
                DateFilter.NO_DATE -> list.filter { it.postDate == null }
                DateFilter.DUPLICATE -> list.filter { song ->
                    (allSongs ?: emptyList()).count { 
                        it.title.lowercase().trim() == song.title.lowercase().trim() &&
                        it.movieName.lowercase().trim() == song.movieName.lowercase().trim() &&
                        it.singers.lowercase().trim() == song.singers.lowercase().trim()
                    } > 1
                }
            }

            // Metadata Filters
            if (selectedLanguage != "All") list = list.filter { it.language.trim().equals(selectedLanguage, ignoreCase = true) }
            if (selectedSinger != "All") list = list.filter { song -> song.singers.split(",").any { it.trim().equals(selectedSinger, ignoreCase = true) } }
            if (selectedMovie != "All") list = list.filter { it.movieName.trim().equals(selectedMovie, ignoreCase = true) }

            // Sorting
            list = when (sortBy) {
                SortOption.NEWEST -> list.sortedByDescending { it.postDate ?: 0L }
                SortOption.OLDEST -> list.sortedBy { it.postDate ?: Long.MAX_VALUE }
                SortOption.SCHEDULED_FIRST -> list.sortedByDescending { viewModel.getSongStatus(it, now) == SongStatus.SCHEDULED }
                SortOption.POSTED_FIRST -> list.sortedByDescending { viewModel.getSongStatus(it, now) == SongStatus.POSTED }
                SortOption.ALPHABETICAL -> list.sortedBy { it.title.lowercase() }
            }
            list
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Unified Search Toolbar Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = DesignSystem.ScreenPadding, end = DesignSystem.ScreenPadding, top = DesignSystem.ScreenPadding, bottom = DesignSystem.SpacingSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UnifiedSearchToolbar(
                    query = searchQuery,
                    onQueryChange = { viewModel.searchQuery.value = it },
                    placeholder = "Search content...",
                    showFiltersPanel = showFiltersPanel,
                    onFilterToggle = { showFiltersPanel = !showFiltersPanel },
                    viewMode = viewMode,
                    onViewModeToggle = { newMode ->
                        viewMode = newMode
                        sharedPrefs.edit { putInt("songs_view_mode", newMode.ordinal) }
                    },
                    testTag = "songs_search_input"
                )
            }

            Spacer(modifier = Modifier.height(DesignSystem.SpacingTiny))

            // Filters & Sort (Panel)
            AnimatedVisibility(
                visible = showFiltersPanel,
                enter = expandVertically(animationSpec = tween(DesignSystem.AnimationDurationShort, easing = FastOutSlowInEasing)) + fadeIn(),
                exit = shrinkVertically(animationSpec = tween(DesignSystem.AnimationDurationShort, easing = FastOutSlowInEasing)) + fadeOut()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = DesignSystem.ScreenPadding, vertical = DesignSystem.SpacingTiny),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(DesignSystem.CornerRadiusLarge),
                    border = BorderStroke(DesignSystem.BorderThickness, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Filter by Status", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                            DateFilter.entries.forEach { filter ->
                                FilterChip(selected = dateFilter == filter, onClick = { dateFilter = filter }, label = { Text(filter.displayName, fontSize = 11.sp) })
                            }
                        }

                        Text("Sort By", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                            SortOption.entries.forEach { option ->
                                FilterChip(selected = sortBy == option, onClick = { sortBy = option }, label = { Text(option.displayName, fontSize = 11.sp) })
                            }
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                FilterDropdown(label = "Language", selectedOption = selectedLanguage, options = languagesList, onOptionSelected = { selectedLanguage = it })
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                FilterDropdown(label = "Singer", selectedOption = selectedSinger, options = singersList, onOptionSelected = { selectedSinger = it })
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                FilterDropdown(label = "Movie", selectedOption = selectedMovie, options = moviesList, onOptionSelected = { selectedMovie = it })
                            }
                            Box(modifier = Modifier.weight(1f).align(Alignment.CenterVertically)) {
                                TextButton(onClick = { dateFilter = DateFilter.ALL; sortBy = SortOption.NEWEST; selectedLanguage = "All"; selectedSinger = "All"; selectedMovie = "All" }, modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reset Filters")
                                }
                            }
                        }
                    }
                }
            }

            if (allSongs == null && isInitialLoading) {
                LazyColumn(
                    contentPadding = PaddingValues(DesignSystem.ScreenPadding, 8.dp, DesignSystem.ScreenPadding, 120.dp),
                    verticalArrangement = Arrangement.spacedBy(DesignSystem.CardSpacing)
                ) {
                    items(5, key = { it }, contentType = { "shimmer" }) { ShimmerCard(height = 120.dp) }
                }
            } else if (processedSongsList.isEmpty()) {
                EmptySongsState(isSearchActive = searchQuery.isNotEmpty() || dateFilter != DateFilter.ALL, onAddFirst = { onNavigateToAddEdit(null) })
            } else {
                if (viewMode == ViewMode.LIST) {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(DesignSystem.ScreenPadding, DesignSystem.SpacingSmall, DesignSystem.ScreenPadding, 120.dp),
                        verticalArrangement = Arrangement.spacedBy(DesignSystem.CardSpacing)
                    ) {
                        items(
                            items = processedSongsList,
                            key = { it.id },
                            contentType = { "song_card" }
                        ) { song ->
                            val isExactDuplicate = remember(allSongs, song) {
                                (allSongs ?: emptyList()).count { 
                                    it.title.lowercase().trim() == song.title.lowercase().trim() &&
                                    it.movieName.lowercase().trim() == song.movieName.lowercase().trim() &&
                                    it.singers.lowercase().trim() == song.singers.lowercase().trim()
                                } > 1
                            }
                            SongHistoryCard(
                                song = song,
                                status = viewModel.getSongStatus(song, now),
                                now = now,
                                isHighlighted = song.id == highlightedSongId,
                                isDuplicateBadgeVisible = isExactDuplicate,
                                onEdit = { onNavigateToAddEdit(song) },
                                onDelete = { songToDelete = song }
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = lazyGridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(DesignSystem.ScreenPadding, DesignSystem.SpacingSmall, DesignSystem.ScreenPadding, 120.dp),
                        horizontalArrangement = Arrangement.spacedBy(DesignSystem.CardSpacing),
                        verticalArrangement = Arrangement.spacedBy(DesignSystem.CardSpacing)
                    ) {
                        items(
                            items = processedSongsList,
                            key = { it.id },
                            contentType = { "song_grid_card" }
                        ) { song ->
                            val isExactDuplicate = remember(allSongs, song) {
                                (allSongs ?: emptyList()).count { 
                                    it.title.lowercase().trim() == song.title.lowercase().trim() &&
                                    it.movieName.lowercase().trim() == song.movieName.lowercase().trim() &&
                                    it.singers.lowercase().trim() == song.singers.lowercase().trim()
                                } > 1
                            }
                            SongGridCard(
                                song = song,
                                status = viewModel.getSongStatus(song, now),
                                now = now,
                                isHighlighted = song.id == highlightedSongId,
                                isDuplicateBadgeVisible = isExactDuplicate,
                                onEdit = { onNavigateToAddEdit(song) },
                                onDelete = { songToDelete = song }
                            )
                        }
                    }
                }
            }
        }

        CreatorLogFAB(
            onClick = { onNavigateToAddEdit(null) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_song_fab"),
            contentDescription = "Record Content"
        )
    }

    if (songToDelete != null) {
        val scope = rememberCoroutineScope()
        AlertDialog(
            onDismissRequest = { songToDelete = null },
            title = { Text("Delete Content?") },
            text = { Text("Are you sure you want to permanently delete this content?") },
            confirmButton = {
                Button(
                    onClick = { 
                        val song = songToDelete!!
                        viewModel.deleteSongPost(song)
                        songToDelete = null
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Content deleted",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.undoDeleteSong()
                            }
                        }
                    }, 
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { songToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun GroupedDuplicateDialog(
    matches: DuplicateGroup,
    onDismiss: () -> Unit,
    onViewExisting: (SongPost) -> Unit,
    onEditNew: () -> Unit,
    onSaveAnyway: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text("Duplicate Content Detected", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(DesignSystem.SpacingMedium)
            ) {
                Text(
                    text = "Similar content already exists in your collection. Review the matches below:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (matches.exact.isNotEmpty()) {
                    MatchSection(
                        title = "Exact Match",
                        color = StatusPosted,
                        songs = matches.exact
                    )
                }
                if (matches.possible.isNotEmpty()) {
                    MatchSection(
                        title = "Possible Duplicate",
                        color = StatusScheduled,
                        songs = matches.possible
                    )
                }
                if (matches.similar.isNotEmpty()) {
                    MatchSection(
                        title = "Similar Song",
                        color = AccentAzure,
                        songs = matches.similar
                    )
                }
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onSaveAnyway,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Anyway")
                }
                OutlinedButton(
                    onClick = onEditNew,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Edit New Entry")
                }
                TextButton(
                    onClick = { matches.bestMatch?.let { onViewExisting(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = matches.bestMatch != null
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Existing")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun MatchSection(title: String, color: Color, songs: List<SongPost>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = color
            )
        }
        songs.forEach { song ->
            val dateStr = if (song.postDate != null) {
                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(song.postDate))
            } else "Draft"
            
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Entry #${String.format(Locale.US, "%04d", song.entryNumber)} - ${song.title}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Movie: ${song.movieName}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (song.singers.isNotBlank()) {
                        Text(
                            text = "Singer: ${song.singers}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Date: $dateStr",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDropdown(label: String, selectedOption: String, options: List<String>, onOptionSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(readOnly = true, value = selectedOption, onValueChange = {}, label = { Text(label, fontSize = 11.sp) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("All") }, onClick = { onOptionSelected("All"); expanded = false })
            options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onOptionSelected(option); expanded = false }) }
        }
    }
}

@Composable
fun EmptySongsState(isSearchActive: Boolean, onAddFirst: () -> Unit) {
    PremiumEmptyState(
        icon = if (isSearchActive) Icons.Default.SearchOff else Icons.Default.AutoAwesome,
        title = if (isSearchActive) "No results found" else "No content yet",
        subtitle = if (isSearchActive) "Try different keywords or reset your filters." else "Your content history starts here. Add your first entry to begin tracking.",
        actionLabel = if (isSearchActive) null else "Add First Entry",
        onAction = if (isSearchActive) null else onAddFirst
    )
}

@Composable
fun SongHistoryCard(
    song: SongPost,
    status: SongStatus?,
    now: Long,
    isHighlighted: Boolean = false,
    isDuplicateBadgeVisible: Boolean = false,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = if (song.postDate != null) {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(song.postDate))
    } else "No Date"

    val haptic = LocalHapticFeedback.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .workspacePressAnimation()
            .then(if (isHighlighted) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(DesignSystem.CornerRadiusLarge)) else Modifier),
        shape = RoundedCornerShape(DesignSystem.CornerRadiusLarge),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) 
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(DesignSystem.BorderThickness, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp, pressedElevation = 4.dp),
        onClick = onEdit
    ) {
        Column(modifier = Modifier.padding(DesignSystem.CardPadding)) {
            // TOP ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DesignSystem.SpacingSmall)) {
                    StatusBadge(status)
                    if (isDuplicateBadgeVisible) {
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f), 
                            shape = RoundedCornerShape(DesignSystem.CornerRadiusBadge), 
                            border = BorderStroke(DesignSystem.BorderThickness, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                            modifier = Modifier.height(DesignSystem.StatusBadgeHeight)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
                                Text(
                                    "Duplicate", 
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold, 
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                Text(
                    text = "#${String.format(Locale.US, "%04d", song.entryNumber)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(DesignSystem.SpacingNormal))

            // SECOND ROW: Song Title
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // THIRD ROW: Movie
            if (song.movieName.isNotBlank()) {
                Spacer(modifier = Modifier.height(DesignSystem.SpacingTiny))
                Text(
                    text = song.movieName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // FOURTH ROW: Singers
            if (song.singers.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song.singers,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // FIFTH ROW: Music Director
            if (song.musicDirector.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song.musicDirector,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(DesignSystem.SpacingNormal))

            // BOTTOM ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DesignSystem.SpacingSmall)) {
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    if (song.contentLink != null) {
                        val context = LocalContext.current
                        IconButton(
                            onClick = { 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                com.voiceofmelody.songdailytracker.util.openContentLink(context, song.contentLink) 
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew, 
                                contentDescription = "Open Post", 
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(DesignSystem.IconSizeMedium))
                    }
                    IconButton(onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDelete() 
                    }, modifier = Modifier.size(48.dp)) {
                        Icon(
                            Icons.Default.Delete, 
                            contentDescription = "Delete", 
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(DesignSystem.IconSizeMedium)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SongGridCard(
    song: SongPost,
    status: SongStatus?,
    now: Long,
    isHighlighted: Boolean = false,
    isDuplicateBadgeVisible: Boolean = false,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = if (song.postDate != null) {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(song.postDate))
    } else "No Date"

    val haptic = LocalHapticFeedback.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .workspacePressAnimation()
            .then(if (isHighlighted) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(DesignSystem.CornerRadiusLarge)) else Modifier),
        shape = RoundedCornerShape(DesignSystem.CornerRadiusLarge),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) 
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(DesignSystem.BorderThickness, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp, pressedElevation = 4.dp),
        onClick = onEdit
    ) {
        Column(modifier = Modifier.padding(DesignSystem.SpacingMedium).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.SpaceBetween, 
                verticalAlignment = Alignment.Top
            ) {
                StatusBadge(status)
                Text(
                    text = "#${String.format(Locale.US, "%04d", song.entryNumber)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            
            Spacer(modifier = Modifier.height(DesignSystem.SpacingSmall))
            
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            if (song.movieName.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song.movieName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isDuplicateBadgeVisible) {
                Spacer(modifier = Modifier.height(DesignSystem.SpacingTiny))
                Text(
                    "Duplicate", 
                    color = MaterialTheme.colorScheme.error, 
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.SpaceBetween, 
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DesignSystem.SpacingTiny)) {
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    if (song.contentLink != null) {
                        val context = LocalContext.current
                        IconButton(
                            onClick = { 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                com.voiceofmelody.songdailytracker.util.openContentLink(context, song.contentLink) 
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew, 
                                contentDescription = "Open Post", 
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(22.dp))
                    }
                    IconButton(
                        onClick = { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDelete() 
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.error, 
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
