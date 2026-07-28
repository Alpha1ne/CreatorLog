package com.voiceofmelody.songdailytracker.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voiceofmelody.songdailytracker.data.model.SongPost
import com.voiceofmelody.songdailytracker.ui.SongStatus
import com.voiceofmelody.songdailytracker.ui.TrackerViewModel
import com.voiceofmelody.songdailytracker.ui.theme.DesignSystem
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSongScreen(
    viewModel: TrackerViewModel,
    editingSong: SongPost? = null,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    var title by rememberSaveable { mutableStateOf(editingSong?.title ?: "") }
    var movieName by rememberSaveable { mutableStateOf(editingSong?.movieName ?: "") }
    var singers by rememberSaveable { mutableStateOf(editingSong?.singers ?: "") }
    var notes by rememberSaveable { mutableStateOf(editingSong?.notes ?: "") }
    var musicDirector by rememberSaveable { mutableStateOf(editingSong?.musicDirector ?: "") }
    var language by rememberSaveable { mutableStateOf(editingSong?.language ?: "") }
    var postDate by rememberSaveable { mutableStateOf(editingSong?.postDate) }
    var contentLink by rememberSaveable { mutableStateOf(editingSong?.contentLink ?: "") }

    var showDiscardDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = postDate ?: System.currentTimeMillis())

    val duplicateMatches by viewModel.duplicateMatches.collectAsState()
    val now by viewModel.currentTime.collectAsState()
    val scope = rememberCoroutineScope()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    val hasChanges = remember(title, movieName, singers, notes, musicDirector, language, postDate, contentLink) {
        title != (editingSong?.title ?: "") ||
        movieName != (editingSong?.movieName ?: "") ||
        singers != (editingSong?.singers ?: "") ||
        notes != (editingSong?.notes ?: "") ||
        musicDirector != (editingSong?.musicDirector ?: "") ||
        language != (editingSong?.language ?: "") ||
        postDate != editingSong?.postDate ||
        contentLink != (editingSong?.contentLink ?: "")
    }

    LaunchedEffect(title, movieName, singers) {
        viewModel.checkDuplicateLive(title, movieName, singers, editingSong?.id ?: 0)
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.clearDuplicateWarning() }
    }

    BackHandler {
        if (hasChanges) showDiscardDialog = true else onBack()
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(if (editingSong == null) "Add Content" else "Edit Content", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { if (hasChanges) showDiscardDialog = true else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(DesignSystem.IconSizeMedium))
                    }
                },
                actions = {
                    val isLinkValid = contentLink.isBlank() || android.util.Patterns.WEB_URL.matcher(contentLink).matches()
                    IconButton(onClick = { 
                        if (title.isNotBlank() && isLinkValid) {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            saveSong(viewModel, editingSong, title, movieName, singers, notes, musicDirector, language, postDate, contentLink, onBack)
                            scope.launch {
                                snackbarHostState.showSnackbar("Content saved successfully")
                            }
                        }
                    }, enabled = title.isNotBlank() && isLinkValid) {
                        Icon(Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(DesignSystem.IconSizeMedium))
                    }
                }
            )
        },
        bottomBar = {
            val isLinkValid = contentLink.isBlank() || android.util.Patterns.WEB_URL.matcher(contentLink).matches()
            Surface(shadowElevation = 8.dp, tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(DesignSystem.SpacingMedium),
                    horizontalArrangement = Arrangement.spacedBy(DesignSystem.SpacingNormal)
                ) {
                    OutlinedButton(
                        onClick = onBack, 
                        modifier = Modifier.weight(1f), 
                        shape = RoundedCornerShape(DesignSystem.CornerRadiusSmall)
                    ) {
                        Text("Cancel", style = MaterialTheme.typography.labelLarge)
                    }
                    Button(
                        onClick = { 
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            saveSong(viewModel, editingSong, title, movieName, singers, notes, musicDirector, language, postDate, contentLink, onBack)
                            scope.launch {
                                snackbarHostState.showSnackbar("Content saved successfully")
                            }
                        },
                        enabled = title.isNotBlank() && isLinkValid,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(DesignSystem.CornerRadiusSmall)
                    ) {
                        Text("Save Content", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(DesignSystem.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(DesignSystem.CardSpacing)
        ) {
            DuplicateWarningSection(duplicateMatches = duplicateMatches)

            SongInformationSection(
                title = title, onTitleChange = { title = it },
                movieName = movieName, onMovieNameChange = { movieName = it },
                singers = singers, onSingersChange = { singers = it },
                musicDirector = musicDirector, onMusicDirectorChange = { musicDirector = it },
                language = language, onLanguageChange = { language = it },
                notes = notes, onNotesChange = { notes = it },
                contentLink = contentLink, onContentLinkChange = { contentLink = it }
            )

            SchedulingSection(
                postDate = postDate,
                status = postDate?.let { viewModel.getSongStatus(SongPost(title = title, movieName = movieName, singers = singers, notes = notes, postDate = it), now) },
                onPickDate = { showDatePicker = true },
                onClearDate = { postDate = null }
            )

            if (editingSong != null) {
                MetadataSection(
                    entryNumber = editingSong.entryNumber,
                    status = viewModel.getSongStatus(editingSong, now)
                )
            }

            AttachmentsSection()

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { postDate = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }
}

private fun saveSong(
    viewModel: TrackerViewModel,
    editingSong: SongPost?,
    title: String,
    movie: String,
    singers: String,
    notes: String,
    director: String,
    lang: String,
    date: Long?,
    link: String,
    onSuccess: () -> Unit
) {
    val finalLink = link.ifBlank { null }
    if (editingSong == null) {
        viewModel.addSongPost(title, movie, singers, notes, director, lang, date, finalLink)
    } else {
        viewModel.updateSongPost(editingSong.id, editingSong.entryNumber, title, movie, singers, notes, director, lang, date, finalLink)
    }
    onSuccess()
}
