package com.voiceofmelody.songdailytracker.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.voiceofmelody.songdailytracker.data.model.SongPost
import com.voiceofmelody.songdailytracker.ui.DuplicateGroup
import com.voiceofmelody.songdailytracker.ui.MatchLevel
import com.voiceofmelody.songdailytracker.ui.TrackerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import java.text.SimpleDateFormat
import java.util.*

enum class DateFilter(val displayName: String) {
    ALL("All Time"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    THIS_YEAR("This Year")
}

enum class SortOption(val displayName: String) {
    NEWEST("Newest First"),
    OLDEST("Oldest First"),
    ALPHABETICAL("Alphabetical")
}

fun cleanStringForComparison(input: String): String {
    return input
        .lowercase()
        .trim()
        .replace(Regex("\\s+"), " ")
        .replace(Regex("[^a-zA-Z0-9\\s]"), "")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongsScreen(viewModel: TrackerViewModel, modifier: Modifier = Modifier) {
    val searchResults by viewModel.searchedSongs.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val allSongs by viewModel.allSongPosts.collectAsState()
    
    val lazyListState = rememberLazyListState()
    var highlightedSongId by remember { mutableIntStateOf(-1) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var showAddDialog by remember { mutableStateOf(false) }
    var songToEdit by remember { mutableStateOf<SongPost?>(null) }
    var songToDelete by remember { mutableStateOf<SongPost?>(null) }
    
    // Filters State
    var showFiltersPanel by remember { mutableStateOf(false) }
    var dateFilter by remember { mutableStateOf(DateFilter.ALL) }
    var sortBy by remember { mutableStateOf(SortOption.NEWEST) }
    var selectedLanguage by remember { mutableStateOf("All") }
    var selectedSinger by remember { mutableStateOf("All") }
    var selectedMovie by remember { mutableStateOf("All") }

    // Grouped Duplicate Dialog State
    var showDuplicateMatchesDialog by remember { mutableStateOf(false) }
    var pendingSongData by remember { mutableStateOf<SongPost?>(null) }
    val duplicateMatches by viewModel.duplicateMatches.collectAsState()

    // Auto-clear highlight
    LaunchedEffect(highlightedSongId) {
        if (highlightedSongId != -1) {
            delay(3.seconds)
            highlightedSongId = -1
        }
    }

    // Dynamically extract dropdown options based on the actual DB content
    val languagesList = remember(allSongs) {
        allSongs.asSequence().map { it.language.trim() }.filter { it.isNotBlank() }.distinct().sorted().toList()
    }
    val singersList = remember(allSongs) {
        allSongs.asSequence().flatMap { it.singers.split(",") }.map { it.trim() }.filter { it.isNotBlank() }.distinct().sorted().toList()
    }
    val moviesList = remember(allSongs) {
        allSongs.asSequence().map { it.movieName.trim() }.filter { it.isNotBlank() }.distinct().sorted().toList()
    }

    // Apply Client-Side Filters & Sort on top of Search results
    val processedSongsList by remember(searchResults, dateFilter, selectedLanguage, selectedSinger, selectedMovie, sortBy) {
        derivedStateOf {
            var list = searchResults
            
            // Date Filtering
            val now = System.currentTimeMillis()
            list = when (dateFilter) {
                DateFilter.TODAY -> {
                    val todayStart = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    list.filter { it.postDate >= todayStart }
                }
                DateFilter.THIS_WEEK -> {
                    val weekAgo = now - (7L * 24 * 60 * 60 * 1000)
                    list.filter { it.postDate >= weekAgo }
                }
                DateFilter.THIS_MONTH -> {
                    val monthStart = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    list.filter { it.postDate >= monthStart }
                }
                DateFilter.THIS_YEAR -> {
                    val yearStart = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_YEAR, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    list.filter { it.postDate >= yearStart }
                }
                DateFilter.ALL -> list
            }

            // Language Filtering
            if (selectedLanguage != "All") {
                list = list.filter { it.language.trim().equals(selectedLanguage, ignoreCase = true) }
            }

            // Singer Filtering
            if (selectedSinger != "All") {
                list = list.filter { song ->
                    song.singers.split(",").any { it.trim().equals(selectedSinger, ignoreCase = true) }
                }
            }

            // Movie Filtering
            if (selectedMovie != "All") {
                list = list.filter { it.movieName.trim().equals(selectedMovie, ignoreCase = true) }
            }

            // Sorting
            list = when (sortBy) {
                SortOption.NEWEST -> list.sortedByDescending { it.postDate }
                SortOption.OLDEST -> list.sortedBy { it.postDate }
                SortOption.ALPHABETICAL -> list.sortedBy { it.title.lowercase() }
            }

            list
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Search & Filter Toggle Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Search songs, movies, director, singer...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("songs_search_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                IconButton(
                    onClick = { showFiltersPanel = !showFiltersPanel },
                    modifier = Modifier
                        .background(
                            if (showFiltersPanel) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(12.dp)
                        )
                        .size(56.dp)
                ) {
                    Icon(
                        imageVector = if (showFiltersPanel) Icons.Default.FilterListOff else Icons.Default.FilterList,
                        contentDescription = "Toggle Filters",
                        tint = if (showFiltersPanel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Filters & Sort Expandable Card Panel
            AnimatedVisibility(
                visible = showFiltersPanel,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Date Filter Selection
                        Text("Posting Date", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            DateFilter.entries.forEach { filter ->
                                FilterChip(
                                    selected = dateFilter == filter,
                                    onClick = { dateFilter = filter },
                                    label = { Text(filter.displayName, fontSize = 11.sp) }
                                )
                            }
                        }

                        // Sort Selection
                        Text("Sort By", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SortOption.entries.forEach { option ->
                                FilterChip(
                                    selected = sortBy == option,
                                    onClick = { sortBy = option },
                                    label = { Text(option.displayName, fontSize = 11.sp) }
                                )
                            }
                        }

                        // Metadata filters (Drop-downs)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                FilterDropdown(
                                    label = "Language",
                                    selectedOption = selectedLanguage,
                                    options = languagesList,
                                    onOptionSelected = { selectedLanguage = it }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                FilterDropdown(
                                    label = "Singer",
                                    selectedOption = selectedSinger,
                                    options = singersList,
                                    onOptionSelected = { selectedSinger = it }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                FilterDropdown(
                                    label = "Movie",
                                    selectedOption = selectedMovie,
                                    options = moviesList,
                                    onOptionSelected = { selectedMovie = it }
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .align(Alignment.CenterVertically)
                            ) {
                                TextButton(
                                    onClick = {
                                        dateFilter = DateFilter.ALL
                                        sortBy = SortOption.NEWEST
                                        selectedLanguage = "All"
                                        selectedSinger = "All"
                                        selectedMovie = "All"
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reset Filters", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Results List
            if (processedSongsList.isEmpty()) {
                val isActive = searchQuery.isNotEmpty() || dateFilter != DateFilter.ALL || selectedLanguage != "All" || selectedSinger != "All" || selectedMovie != "All"
                EmptySongsState(
                    isSearchActive = isActive,
                    onAddFirst = { showAddDialog = true }
                )
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("songs_lazy_column"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
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
                            isHighlighted = song.id == highlightedSongId,
                            isDuplicateBadgeVisible = isExactDuplicate,
                            onEdit = { songToEdit = song },
                            onDelete = { 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                songToDelete = song 
                            }
                        )
                    }
                }
            }
        }

        // Add Post Floating Action Button
        LargeFloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_song_fab"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Record Song Post")
        }
    }

    // Add Song Dialog
    if (showAddDialog) {
        SongFormDialog(
            viewModel = viewModel,
            onDismiss = { showAddDialog = false },
            onSaveWithInterception = { title, movie, singers, notes, director, lang, date ->
                viewModel.checkDuplicateLive(title, movie, singers)
                pendingSongData = SongPost(
                    entryNumber = 0,
                    title = title,
                    movieName = movie,
                    singers = singers,
                    notes = notes,
                    musicDirector = director,
                    language = lang,
                    postDate = date
                )
                showDuplicateMatchesDialog = true
            }
        )
    }

    // Edit Song Dialog
    if (songToEdit != null) {
        SongFormDialog(
            viewModel = viewModel,
            editingSong = songToEdit,
            onDismiss = { songToEdit = null },
            onSaveWithInterception = { title, movie, singers, notes, director, lang, date ->
                songToEdit?.let { oldSong ->
                    viewModel.checkDuplicateLive(title, movie, singers, oldSong.id)
                    pendingSongData = SongPost(
                        id = oldSong.id,
                        entryNumber = oldSong.entryNumber,
                        title = title,
                        movieName = movie,
                        singers = singers,
                        notes = notes,
                        musicDirector = director,
                        language = lang,
                        postDate = date
                    )
                    showDuplicateMatchesDialog = true
                }
            }
        )
    }

    // Advanced Grouped Duplicate Confirmation Dialog
    if (showDuplicateMatchesDialog && pendingSongData != null) {
        val matches = duplicateMatches
        
        if (matches.isEmpty) {
            // No matches found after logic ran (should not usually happen due to checkDuplicateLive, but safety first)
            val pending = pendingSongData!!
            if (pending.id == 0) {
                viewModel.addSongPost(pending.title, pending.movieName, pending.singers, pending.notes, pending.musicDirector, pending.language, pending.postDate)
                showAddDialog = false
            } else {
                viewModel.updateSongPost(pending.id, pending.entryNumber, pending.title, pending.movieName, pending.singers, pending.notes, pending.musicDirector, pending.language, pending.postDate)
                songToEdit = null
            }
            showDuplicateMatchesDialog = false
            pendingSongData = null
        } else {
            GroupedDuplicateDialog(
                matches = matches,
                onDismiss = { 
                    showDuplicateMatchesDialog = false
                    pendingSongData = null
                },
                onViewExisting = { targetSong ->
                    showDuplicateMatchesDialog = false
                    if (pendingSongData?.id == 0) showAddDialog = false else songToEdit = null
                    pendingSongData = null
                    
                    scope.launch {
                        // Priority scroll
                        val index = processedSongsList.indexOfFirst { it.id == targetSong.id }
                        if (index != -1) {
                            lazyListState.animateScrollToItem(index)
                            highlightedSongId = targetSong.id
                        }
                    }
                },
                onEditNew = {
                    showDuplicateMatchesDialog = false
                    // Stay in Add/Edit dialog
                },
                onSaveAnyway = {
                    val pending = pendingSongData!!
                    if (pending.id == 0) {
                        viewModel.addSongPost(pending.title, pending.movieName, pending.singers, pending.notes, pending.musicDirector, pending.language, pending.postDate)
                        showAddDialog = false
                    } else {
                        viewModel.updateSongPost(pending.id, pending.entryNumber, pending.title, pending.movieName, pending.singers, pending.notes, pending.musicDirector, pending.language, pending.postDate)
                        songToEdit = null
                    }
                    showDuplicateMatchesDialog = false
                    pendingSongData = null
                }
            )
        }
    }

    // Delete Song Confirmation Dialog
    if (songToDelete != null) {
        AlertDialog(
            onDismissRequest = { songToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("Delete Song?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text("Are you sure you want to permanently delete this song?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        songToDelete?.let { viewModel.deleteSongPost(it) }
                        songToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { songToDelete = null }) {
                    Text("Cancel")
                }
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text("Duplicate Song Detected", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "A similar song already exists in your collection. Review the matches below:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (matches.exact.isNotEmpty()) {
                    MatchSection(title = "Exact Match", color = Color(0xFF43A047), songs = matches.exact)
                }
                if (matches.possible.isNotEmpty()) {
                    MatchSection(title = "Possible Duplicate", color = Color(0xFFFB8C00), songs = matches.possible)
                }
                if (matches.similar.isNotEmpty()) {
                    MatchSection(title = "Similar Song", color = Color(0xFF1E88E5), songs = matches.similar)
                }
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onSaveAnyway,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Save Anyway")
                }
                OutlinedButton(
                    onClick = onEditNew,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Edit New Entry")
                }
                TextButton(
                    onClick = { matches.bestMatch?.let { onViewExisting(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = matches.bestMatch != null
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = color)
        }
        songs.forEach { song ->
            val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(song.postDate))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Entry #${String.format(Locale.US, "%04d", song.entryNumber)} - ${song.title}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text("Movie: ${song.movieName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Singer: ${song.singers}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Posted: $dateStr", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
    @OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDropdown(
    label: String,
    selectedOption: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            readOnly = true,
            value = selectedOption,
            onValueChange = {},
            label = { Text(label, fontSize = 11.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("All") },
                onClick = {
                    onOptionSelected("All")
                    expanded = false
                }
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun EmptySongsState(isSearchActive: Boolean, onAddFirst: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.testTag("empty_songs_state")
        ) {
            Surface(
                modifier = Modifier.size(100.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = CircleShape
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isSearchActive) Icons.Default.SearchOff else Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            Text(
                text = if (isSearchActive) "No results found" else "No Songs Yet",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (isSearchActive) "Try resetting filters or changing your query to find what you're looking for." 
                       else "Start tracking your Instagram songs history to prevent double-posting. Your library is waiting!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (!isSearchActive) {
                Button(
                    onClick = onAddFirst,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add First Song")
                }
            }
        }
    }
}

@Composable
fun SongHistoryCard(
    song: SongPost,
    isHighlighted: Boolean = false,
    isDuplicateBadgeVisible: Boolean = false,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val formattedDate = remember(song.postDate) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(song.postDate))
    }

    val infiniteTransition = rememberInfiniteTransition(label = "highlight")
    val haptic = LocalHapticFeedback.current
    val highlightAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isHighlighted) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = highlightAlpha),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else Modifier
            )
            .testTag("song_item_card_${song.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format(Locale.US, "#%04d", song.entryNumber),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (isDuplicateBadgeVisible) {
                            AssistChip(
                                onClick = {},
                                label = { Text("Duplicate", fontSize = 10.sp) },
                                colors = AssistChipDefaults.assistChipColors(
                                    labelColor = MaterialTheme.colorScheme.error
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(20.dp)
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Posted",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Movie: ${song.movieName}",
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (song.singers.isNotBlank()) {
                    Text(
                        text = "Singers: ${song.singers}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (song.musicDirector.isNotBlank()) {
                    Text(
                        text = "Music Director: ${song.musicDirector}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.tertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (song.language.isNotBlank()) {
                    Text(
                        text = "Language: ${song.language}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (song.notes.isNotBlank()) {
                    Text(
                        text = "Notes: ${song.notes}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "Posted on: $formattedDate",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit song",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDelete()
                }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete record",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongFormDialog(
    viewModel: TrackerViewModel,
    editingSong: SongPost? = null,
    onDismiss: () -> Unit,
    onSaveWithInterception: (title: String, movie: String, singers: String, notes: String, director: String, lang: String, date: Long) -> Unit
) {
    var title by remember { mutableStateOf(editingSong?.title ?: "") }
    var movieName by remember { mutableStateOf(editingSong?.movieName ?: "") }
    var singers by remember { mutableStateOf(editingSong?.singers ?: "") }
    var notes by remember { mutableStateOf(editingSong?.notes ?: "") }
    var musicDirector by remember { mutableStateOf(editingSong?.musicDirector ?: "") }
    var language by remember { mutableStateOf(editingSong?.language ?: "") }
    var postDate by remember { mutableLongStateOf(editingSong?.postDate ?: System.currentTimeMillis()) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = postDate
    )

    val timePickerState = rememberTimePickerState(
        initialHour = Calendar.getInstance().apply { timeInMillis = postDate }.get(Calendar.HOUR_OF_DAY),
        initialMinute = Calendar.getInstance().apply { timeInMillis = postDate }.get(Calendar.MINUTE)
    )

    val duplicateMatches by viewModel.duplicateMatches.collectAsState()

    LaunchedEffect(title, movieName, singers) {
        viewModel.checkDuplicateLive(title, movieName, singers, editingSong?.id ?: 0)
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.clearDuplicateWarning() }
    }

    val formattedSelectedDate = remember(postDate) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(postDate))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (editingSong == null) "Record Daily Song Post" else "Edit Song Post",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Live warning banner (for typing visual support)
                AnimatedVisibility(
                    visible = !duplicateMatches.isEmpty,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    val topLevel = duplicateMatches.topLevel
                    val color = when (topLevel) {
                        MatchLevel.EXACT -> MaterialTheme.colorScheme.error
                        MatchLevel.POSSIBLE -> Color(0xFFFB8C00)
                        MatchLevel.SIMILAR -> Color(0xFF1E88E5)
                        else -> MaterialTheme.colorScheme.primary
                    }
                    val label = when (topLevel) {
                        MatchLevel.EXACT -> "EXACT MATCH DETECTED"
                        MatchLevel.POSSIBLE -> "POSSIBLE DUPLICATE"
                        MatchLevel.SIMILAR -> "SIMILAR SONG RECORDED"
                        else -> ""
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
                        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = color
                                )
                                Text(
                                    text = "Tap 'Save' to review matching entries.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Song Title *") },
                    placeholder = { Text("e.g., Starboy") },
                    modifier = Modifier.fillMaxWidth().testTag("song_form_title"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                // Movie Name Input
                OutlinedTextField(
                    value = movieName,
                    onValueChange = { movieName = it },
                    label = { Text("Movie / Album Name *") },
                    placeholder = { Text("e.g., Starboy Album") },
                    modifier = Modifier.fillMaxWidth().testTag("song_form_movie"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                // Singers Input
                OutlinedTextField(
                    value = singers,
                    onValueChange = { singers = it },
                    label = { Text("Singers") },
                    placeholder = { Text("e.g., The Weeknd, Daft Punk") },
                    modifier = Modifier.fillMaxWidth().testTag("song_form_singers"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                // Music Director Input
                OutlinedTextField(
                    value = musicDirector,
                    onValueChange = { musicDirector = it },
                    label = { Text("Music Director") },
                    placeholder = { Text("e.g., A.R. Rahman") },
                    modifier = Modifier.fillMaxWidth().testTag("song_form_music_director"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                // Language Input
                OutlinedTextField(
                    value = language,
                    onValueChange = { language = it },
                    label = { Text("Language") },
                    placeholder = { Text("e.g., English, Tamil") },
                    modifier = Modifier.fillMaxWidth().testTag("song_form_language"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                // Music Details/Platform/Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Instagram details") },
                    placeholder = { Text("e.g., Used 15s audio loop, trending reel") },
                    modifier = Modifier.fillMaxWidth().testTag("song_form_notes"),
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(10.dp)
                )

                // Posting Time
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = formattedSelectedDate,
                        onValueChange = {},
                        label = { Text("Posting Date & Time") },
                        readOnly = true,
                        trailingIcon = {
                            Icon(Icons.Default.EditCalendar, contentDescription = "Pick date and time")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("song_form_date"),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showDatePicker = true }
                    )
                }
            }
        },
        confirmButton = {
            val haptic = LocalHapticFeedback.current
            Button(
                onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSaveWithInterception(title, movieName, singers, notes, musicDirector, language, postDate) 
                },
                enabled = title.isNotBlank() && movieName.isNotBlank(),
                modifier = Modifier.testTag("song_form_save_button")
            ) {
                Text("Save Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    showTimePicker = true
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedDate = datePickerState.selectedDateMillis ?: postDate
                    val cal = Calendar.getInstance().apply {
                        val selectedCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                            timeInMillis = selectedDate
                        }
                        set(Calendar.YEAR, selectedCal.get(Calendar.YEAR))
                        set(Calendar.MONTH, selectedCal.get(Calendar.MONTH))
                        set(Calendar.DAY_OF_MONTH, selectedCal.get(Calendar.DAY_OF_MONTH))
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    postDate = cal.timeInMillis
                    showTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}
