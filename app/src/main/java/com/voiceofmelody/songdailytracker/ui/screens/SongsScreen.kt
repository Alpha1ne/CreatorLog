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
import com.voiceofmelody.songdailytracker.data.model.SongPost
import com.voiceofmelody.songdailytracker.ui.DuplicateGroup
import com.voiceofmelody.songdailytracker.ui.MatchLevel
import com.voiceofmelody.songdailytracker.ui.SongStatus
import com.voiceofmelody.songdailytracker.ui.TrackerViewModel
import com.voiceofmelody.songdailytracker.ui.ViewMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
        SongStatus.SCHEDULED -> Color(0xFFFBBF24) // Gold
        SongStatus.POSTED -> Color(0xFF10B981) // Green
    }
    
    val text = when (status) {
        SongStatus.SCHEDULED -> "Scheduled"
        SongStatus.POSTED -> "Posted"
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
            Text(
                text = text,
                fontSize = 10.sp,
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
    var highlightedSongId by remember { mutableIntStateOf(-1) }
    val scope = rememberCoroutineScope()

    var songToDelete by remember { mutableStateOf<SongPost?>(null) }
    
    // Filters State
    var showFiltersPanel by remember { mutableStateOf(false) }
    var dateFilter by remember { mutableStateOf(DateFilter.ALL) }
    var sortBy by remember { mutableStateOf(SortOption.NEWEST) }
    var selectedLanguage by remember { mutableStateOf("All") }
    var selectedSinger by remember { mutableStateOf("All") }
    var selectedMovie by remember { mutableStateOf("All") }

    // Auto-clear highlight
    LaunchedEffect(highlightedSongId) {
        if (highlightedSongId != -1) {
            delay(3.seconds)
            highlightedSongId = -1
        }
    }

    // Dynamically extract dropdown options
    val languagesList = remember(allSongs) {
        allSongs.asSequence().map { it.language.trim() }.filter { it.isNotBlank() }.distinct().sorted().toList()
    }
    val singersList = remember(allSongs) {
        allSongs.asSequence().flatMap { it.singers.split(",") }.map { it.trim() }.filter { it.isNotBlank() }.distinct().sorted().toList()
    }
    val moviesList = remember(allSongs) {
        allSongs.asSequence().map { it.movieName.trim() }.filter { it.isNotBlank() }.distinct().sorted().toList()
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
                    allSongs.count { 
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
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UnifiedSearchToolbar(
                    query = searchQuery,
                    onQueryChange = { viewModel.searchQuery.value = it },
                    placeholder = "Search songs, movies...",
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

            Spacer(modifier = Modifier.height(4.dp))

            // Filters & Sort (Panel)
            AnimatedVisibility(visible = showFiltersPanel) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
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

            if (processedSongsList.isEmpty()) {
                EmptySongsState(isSearchActive = searchQuery.isNotEmpty() || dateFilter != DateFilter.ALL, onAddFirst = { onNavigateToAddEdit(null) })
            } else {
                if (viewMode == ViewMode.LIST) {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 120.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(processedSongsList, key = { it.id }) { song ->
                            val isExactDuplicate = remember(allSongs, song) {
                                allSongs.count { 
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
                        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 120.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(processedSongsList, key = { it.id }) { song ->
                            val isExactDuplicate = remember(allSongs, song) {
                                allSongs.count { 
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
            contentDescription = "Record Song Post"
        )
    }

    if (songToDelete != null) {
        AlertDialog(
            onDismissRequest = { songToDelete = null },
            title = { Text("Delete Song?") },
            text = { Text("Are you sure you want to permanently delete this song?") },
            confirmButton = {
                Button(onClick = { viewModel.deleteSongPost(songToDelete!!); songToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
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
                Text("Duplicate Song Detected", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "A similar song already exists in your collection. Review the matches below:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (matches.exact.isNotEmpty()) {
                    MatchSection(
                        title = "Exact Match",
                        color = Color(0xFF43A047),
                        songs = matches.exact
                    )
                }
                if (matches.possible.isNotEmpty()) {
                    MatchSection(
                        title = "Possible Duplicate",
                        color = Color(0xFFFB8C00),
                        songs = matches.possible
                    )
                }
                if (matches.similar.isNotEmpty()) {
                    MatchSection(
                        title = "Similar Song",
                        color = Color(0xFF1E88E5),
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
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(modifier = Modifier.size(100.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = CircleShape) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = if (isSearchActive) Icons.Default.SearchOff else Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                }
            }
            Text(text = if (isSearchActive) "No Results" else "No Songs Yet", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
            Text(text = if (isSearchActive) "Try different keywords." else "Start tracking your Instagram songs history.", textAlign = TextAlign.Center)
            if (!isSearchActive) Button(onClick = onAddFirst) { Text("Add First Song") }
        }
    }
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

    val infiniteTransition = rememberInfiniteTransition(label = "highlight")
    val highlightAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val haptic = LocalHapticFeedback.current

    Card(
        modifier = Modifier.fillMaxWidth().then(if (isHighlighted) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)) else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBadge(status)
                    if (isDuplicateBadgeVisible) {
                        Surface(color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))) {
                            Text("Duplicate", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                    Text(text = "#${String.format(Locale.US, "%04d", song.entryNumber)}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = song.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = "Movie: ${song.movieName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (song.singers.isNotBlank()) Text(text = "Singers: ${song.singers}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (song.musicDirector.isNotBlank()) Text(text = "Music Director: ${song.musicDirector}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.tertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (song.language.isNotBlank()) Text(text = "Language: ${song.language}", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (song.notes.isNotBlank()) Text(text = "Notes: ${song.notes}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), modifier = Modifier.padding(top = 4.dp))
            }
            Row {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onDelete() }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) }
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

    val infiniteTransition = rememberInfiniteTransition(label = "highlight")
    val highlightAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val haptic = LocalHapticFeedback.current

    Card(
        modifier = Modifier.fillMaxWidth().then(if (isHighlighted) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)) else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                StatusBadge(status)
                Text(text = "#${String.format(Locale.US, "%04d", song.entryNumber)}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = song.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = song.movieName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (song.singers.isNotBlank()) Text(text = song.singers, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (isDuplicateBadgeVisible) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Duplicate", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = dateStr, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) }
                IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onDelete() }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) }
            }
        }
    }
}
