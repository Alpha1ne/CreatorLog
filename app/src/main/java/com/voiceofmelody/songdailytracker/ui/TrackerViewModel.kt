package com.voiceofmelody.songdailytracker.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.voiceofmelody.songdailytracker.data.local.AppDatabase
import com.voiceofmelody.songdailytracker.data.model.IdeaVaultEntry
import com.voiceofmelody.songdailytracker.data.model.SongPost
import com.voiceofmelody.songdailytracker.data.repository.TrackerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.*

enum class IdeaFilter { ALL, PENDING, POSTED }

enum class ViewMode { LIST, GRID }

enum class MatchLevel { EXACT, POSSIBLE, SIMILAR }

enum class SongStatus { SCHEDULED, POSTED }

data class DuplicateGroup(
    val exact: List<SongPost> = emptyList(),
    val possible: List<SongPost> = emptyList(),
    val similar: List<SongPost> = emptyList()
) {
    val isEmpty: Boolean get() = exact.isEmpty() && possible.isEmpty() && similar.isEmpty()
    val bestMatch: SongPost? get() = exact.firstOrNull() ?: possible.firstOrNull() ?: similar.firstOrNull()
    val topLevel: MatchLevel? get() = when {
        exact.isNotEmpty() -> MatchLevel.EXACT
        possible.isNotEmpty() -> MatchLevel.POSSIBLE
        similar.isNotEmpty() -> MatchLevel.SIMILAR
        else -> null
    }
}

class TrackerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TrackerRepository

    // Time tracking for dynamic status
    private val _currentTime = MutableStateFlow(System.currentTimeMillis())
    val currentTime: StateFlow<Long> = _currentTime.asStateFlow()

    // Raw streams from DB
    val allSongPosts: StateFlow<List<SongPost>>
    val allIdeas: StateFlow<List<IdeaVaultEntry>>

    // Search Query States
    val searchQuery = MutableStateFlow("")
    val ideasSearchQuery = MutableStateFlow("")
    val ideasFilter = MutableStateFlow(IdeaFilter.ALL)

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TrackerRepository(database.songPostDao(), database.ideaVaultDao())
        
        allSongPosts = repository.allSongPosts
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allIdeas = repository.allIdeas
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Setup strategic time refresh for content scheduling
        viewModelScope.launch {
            allSongPosts.collect { songs ->
                updateCurrentTime()
                scheduleNextRefresh(songs)
            }
        }
    }

    private fun updateCurrentTime() {
        _currentTime.value = System.currentTimeMillis()
    }

    private var nextRefreshJob: kotlinx.coroutines.Job? = null
    private fun scheduleNextRefresh(songs: List<SongPost>) {
        nextRefreshJob?.cancel()
        val now = System.currentTimeMillis()
        val nextScheduledTime = songs
            .mapNotNull { it.postDate }
            .filter { it > now }
            .minOrNull()

        if (nextScheduledTime != null) {
            val delayMillis = nextScheduledTime - now + 1000 // Add 1s buffer
            nextRefreshJob = viewModelScope.launch {
                kotlinx.coroutines.delay(delayMillis)
                updateCurrentTime()
            }
        }
    }

    fun onResume() {
        updateCurrentTime()
    }

    // Reactive list of songs matching the search query
    @OptIn(ExperimentalCoroutinesApi::class)
    val searchedSongs: StateFlow<List<SongPost>> = searchQuery
        .flatMapLatest { query ->
            repository.searchSongPosts(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reactive list of ideas matching the search query and filter
    @OptIn(ExperimentalCoroutinesApi::class)
    val searchedIdeas: StateFlow<List<IdeaVaultEntry>> = combine(ideasSearchQuery, ideasFilter) { query, filter ->
        query to filter
    }.flatMapLatest { (query, filter) ->
        repository.searchIdeas(query).map { list ->
            when (filter) {
                IdeaFilter.ALL -> list
                IdeaFilter.PENDING -> list.filter { !it.isPosted }
                IdeaFilter.POSTED -> list.filter { it.isPosted }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live Duplication Alert State (Grouped)
    private val _duplicateMatches = MutableStateFlow(DuplicateGroup())
    val duplicateMatches: StateFlow<DuplicateGroup> = _duplicateMatches.asStateFlow()

    // Screen-level state helpers
    fun checkDuplicateLive(title: String, movie: String, singers: String, excludeId: Int = 0) {
        if (title.isBlank()) {
            _duplicateMatches.value = DuplicateGroup()
            return
        }
        viewModelScope.launch {
            val potential = repository.getPotentialDuplicates(title.trim())
            val matches = potential.filter { it.id != excludeId }
            
            val cleanTitle = title.lowercase().trim()
            val cleanMovie = movie.lowercase().trim()
            val cleanSingers = singers.lowercase().trim()

            val exact = mutableListOf<SongPost>()
            val possible = mutableListOf<SongPost>()
            val similar = mutableListOf<SongPost>()

            matches.forEach { song ->
                val sTitle = song.title.lowercase().trim()
                val sMovie = song.movieName.lowercase().trim()
                val sSingers = song.singers.lowercase().trim()

                if (sTitle == cleanTitle) {
                    when {
                        sMovie == cleanMovie && sSingers == cleanSingers -> exact.add(song)
                        sMovie == cleanMovie -> possible.add(song)
                        else -> similar.add(song)
                    }
                }
            }
            
            _duplicateMatches.value = DuplicateGroup(
                exact = exact.sortedByDescending { it.postDate ?: 0L },
                possible = possible.sortedByDescending { it.postDate ?: 0L },
                similar = similar.sortedByDescending { it.postDate ?: 0L }
            )
        }
    }

    fun clearDuplicateWarning() {
        _duplicateMatches.value = DuplicateGroup()
    }

    // --- Song CRUD ---
    fun addSongPost(title: String, movieName: String, singers: String, notes: String, musicDirector: String, language: String, postDate: Long?) {
        viewModelScope.launch {
            val maxNum = repository.getMaxEntryNumber()
            repository.insertSongPost(
                SongPost(
                    entryNumber = maxNum + 1,
                    title = title.trim(),
                    movieName = movieName.trim(),
                    singers = singers.trim(),
                    notes = notes.trim(),
                    musicDirector = musicDirector.trim(),
                    language = language.trim(),
                    postDate = postDate?.let { normalizeDateToDayStart(it) },
                    isPostedConfirmed = false // Legacy field, not used in v1.2 logic
                )
            )
        }
    }

    fun updateSongPost(id: Int, entryNumber: Long, title: String, movieName: String, singers: String, notes: String, musicDirector: String, language: String, postDate: Long?) {
        viewModelScope.launch {
            repository.updateSongPost(
                SongPost(
                    id = id,
                    entryNumber = entryNumber,
                    title = title.trim(),
                    movieName = movieName.trim(),
                    singers = singers.trim(),
                    notes = notes.trim(),
                    musicDirector = musicDirector.trim(),
                    language = language.trim(),
                    postDate = postDate?.let { normalizeDateToDayStart(it) },
                    isPostedConfirmed = false // Legacy field
                )
            )
        }
    }
    
    fun deleteSongPost(songPost: SongPost) {
        viewModelScope.launch {
            repository.deleteSongPost(songPost)
        }
    }

    // --- Content Planner CRUD ---
    fun addIdea(title: String, content: String, category: String?, color: Long?, isPosted: Boolean) {
        viewModelScope.launch {
            repository.insertIdea(
                IdeaVaultEntry(
                    title = title.trim(),
                    content = content.trim(),
                    category = category?.trim(),
                    color = color,
                    isPosted = isPosted,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    isPinned = false
                )
            )
        }
    }

    fun updateIdea(id: Int, title: String, content: String, category: String?, color: Long?, isPosted: Boolean, isPinned: Boolean, createdAt: Long) {
        viewModelScope.launch {
            repository.updateIdea(
                IdeaVaultEntry(
                    id = id,
                    title = title.trim(),
                    content = content.trim(),
                    category = category?.trim(),
                    color = color,
                    isPosted = isPosted,
                    createdAt = createdAt,
                    updatedAt = System.currentTimeMillis(),
                    isPinned = isPinned
                )
            )
        }
    }

    fun togglePin(idea: IdeaVaultEntry) {
        viewModelScope.launch {
            repository.updateIdea(idea.copy(isPinned = !idea.isPinned, updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteIdea(idea: IdeaVaultEntry) {
        viewModelScope.launch {
            repository.deleteIdea(idea)
        }
    }

    // --- Dashboard Stats Calculations ---
    val statsState = combine(allSongPosts, allIdeas, currentTime) { songs, ideas, now ->
        calculateStats(songs, ideas, now)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    private fun calculateStats(songs: List<SongPost>, ideas: List<IdeaVaultEntry>, now: Long): DashboardStats {
        val totalSongs = songs.size
        val totalIdeas = ideas.size
        
        // Dynamic Status Counts
        var postedCount = 0
        var scheduledCount = 0
        
        songs.forEach { song ->
            val status = getSongStatus(song, now)
            when (status) {
                SongStatus.POSTED -> postedCount++
                SongStatus.SCHEDULED -> scheduledCount++
                null -> {}
            }
        }

        val completionProgress = if (totalSongs > 0) (postedCount.toFloat() / totalSongs * 100).toInt() else 0

        // Stats for DashboardScreen
        val todayStart = normalizeDateToDayStart(now)
        val songsPostedToday = songs.count { 
            it.postDate != null && it.postDate >= todayStart && it.postDate <= now // Simple today check
        }

        val sevenDaysAgo = now - (7L * 24 * 60 * 60 * 1000)
        val songsLast7Days = songs.count { it.postDate != null && it.postDate in sevenDaysAgo..now }

        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        val songsThisMonth = songs.count {
            if (it.postDate != null && it.postDate <= now) {
                val c = Calendar.getInstance().apply { timeInMillis = it.postDate }
                c.get(Calendar.MONTH) == currentMonth && c.get(Calendar.YEAR) == currentYear
            } else false
        }

        val singerCounts = songs.asSequence()
            .filter { it.postDate != null && it.postDate <= now }
            .flatMap { it.singers.split(",") }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .groupBy { it }
            .mapValues { it.value.size }
        val mostPostedSinger = singerCounts.maxByOrNull { it.value }?.key ?: "N/A"

        val movieCounts = songs.asSequence()
            .filter { it.postDate != null && it.postDate <= now }
            .map { it.movieName.trim() }
            .filter { it.isNotBlank() }
            .groupBy { it }
            .mapValues { it.value.size }
        val mostPostedMovie = movieCounts.maxByOrNull { it.value }?.key ?: "N/A"

        val languageCounts = songs.asSequence()
            .filter { it.postDate != null && it.postDate <= now }
            .map { it.language.trim() }
            .filter { it.isNotBlank() }
            .groupBy { it }
            .mapValues { it.value.size }
        val mostUsedLanguage = languageCounts.maxByOrNull { it.value }?.key ?: "N/A"

        val pendingIdeas = ideas.count { !it.isPosted }
        val postedIdeas = ideas.count { it.isPosted }

        // Duplicate Stats
        val duplicateCount = songs.groupBy { 
                val title = it.title.lowercase().trim()
                val movie = it.movieName.lowercase().trim()
                val singers = it.singers.lowercase().trim()
                "$title|$movie|$singers"
            }.count { it.value.size > 1 }

        return DashboardStats(
            totalSongs = totalSongs,
            postedSongsCount = postedCount,
            scheduledSongsCount = scheduledCount,
            postingProgress = completionProgress,
            duplicateSongsCount = duplicateCount,
            isLibraryEmpty = totalSongs == 0 && totalIdeas == 0,
            songsPostedToday = songsPostedToday,
            songsLast7Days = songsLast7Days,
            songsThisMonth = songsThisMonth,
            mostPostedSinger = mostPostedSinger,
            mostPostedMovie = mostPostedMovie,
            mostUsedLanguage = mostUsedLanguage,
            pendingIdeasCount = pendingIdeas,
            postedIdeasCount = postedIdeas
        )
    }
    
    fun getSongStatus(song: SongPost, now: Long): SongStatus? {
        val date = song.postDate ?: return null
        val todayStart = normalizeDateToDayStart(now)
        return if (date <= todayStart) SongStatus.POSTED else SongStatus.SCHEDULED
    }

    private fun normalizeDateToDayStart(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    // --- JSON Backup & Export ---
    fun exportBackupJson(): String {
        return try {
            val root = JSONObject()
            val songsArray = JSONArray()
            allSongPosts.value.forEach { song ->
                songsArray.put(JSONObject().apply {
                    put("id", song.id)
                    put("entryNumber", song.entryNumber)
                    put("title", song.title)
                    put("movieName", song.movieName)
                    put("singers", song.singers)
                    put("notes", song.notes)
                    put("musicDirector", song.musicDirector)
                    put("language", song.language)
                    put("postDate", song.postDate ?: JSONObject.NULL)
                    put("isPostedConfirmed", song.isPostedConfirmed)
                })
            }
            root.put("songs", songsArray)

            val ideasArray = JSONArray()
            allIdeas.value.forEach { idea ->
                ideasArray.put(JSONObject().apply {
                    put("id", idea.id)
                    put("title", idea.title)
                    put("content", idea.content)
                    put("category", idea.category)
                    put("color", idea.color)
                    put("isPosted", idea.isPosted)
                    put("createdAt", idea.createdAt)
                    put("updatedAt", idea.updatedAt)
                    put("isPinned", idea.isPinned)
                })
            }
            root.put("ideas", ideasArray)
            root.toString(4)
        } catch (_: Exception) { "" }
    }

    fun importBackupJson(jsonString: String): Boolean {
        if (jsonString.isBlank()) return false
        return try {
            val root = JSONObject(jsonString)
            if (root.has("songs")) {
                val songsArray = root.getJSONArray("songs")
                val songsToImport = mutableListOf<JSONObject>()
                for (i in 0 until songsArray.length()) {
                    songsToImport.add(songsArray.getJSONObject(i))
                }
                songsToImport.sortBy { it.optLong("postDate", 0L) }

                viewModelScope.launch {
                    var currentMax = repository.getMaxEntryNumber()
                    songsToImport.forEach { sObj ->
                        try {
                            if (!sObj.has("title") || !sObj.has("movieName")) {
                                android.util.Log.w("TrackerViewModel", "Skipping malformed JSON entry: $sObj")
                                return@forEach
                            }
                            val title = sObj.getString("title")
                            val movie = sObj.getString("movieName")
                            val singers = sObj.optString("singers", "")
                            if (repository.checkDuplicate(title, movie, singers) == null) {
                                val importedNum = sObj.optLong("entryNumber", 0L)
                                val finalNum = if (importedNum > 0) importedNum else {
                                    currentMax++
                                    currentMax
                                }
                                repository.insertSongPost(SongPost(
                                    entryNumber = finalNum,
                                    title = title,
                                    movieName = movie,
                                    singers = singers,
                                    notes = sObj.optString("notes", ""),
                                    musicDirector = sObj.optString("musicDirector", ""),
                                    language = sObj.optString("language", ""),
                                    postDate = if (sObj.isNull("postDate")) null else normalizeDateToDayStart(sObj.getLong("postDate")),
                                    isPostedConfirmed = false
                                ))
                                if (finalNum > currentMax) currentMax = finalNum
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("TrackerViewModel", "Error importing JSON song entry", e)
                        }
                    }
                }
            }
            if (root.has("ideas")) {
                val ideasArray = root.getJSONArray("ideas")
                for (i in 0 until ideasArray.length()) {
                    val iObj = ideasArray.getJSONObject(i)
                    viewModelScope.launch {
                        try {
                            if (!iObj.has("title") || !iObj.has("content")) {
                                android.util.Log.w("TrackerViewModel", "Skipping malformed JSON idea entry: $iObj")
                                return@launch
                            }
                            repository.insertIdea(IdeaVaultEntry(
                                title = iObj.getString("title"),
                                content = iObj.getString("content"),
                                category = iObj.optString("category", ""),
                                color = if (iObj.isNull("color")) null else iObj.getLong("color"),
                                createdAt = iObj.optLong("createdAt", System.currentTimeMillis()),
                                updatedAt = iObj.optLong("updatedAt", System.currentTimeMillis()),
                                isPinned = iObj.optBoolean("isPinned", false)
                            ))
                        } catch (e: Exception) {
                            android.util.Log.e("TrackerViewModel", "Error importing JSON idea entry", e)
                        }
                    }
                }
            }
            true
        } catch (_: Exception) { false }
    }

    // --- CSV Backup & Export ---
    fun exportBackupCsvSongs(): String {
        val sb = StringBuilder()
        sb.append("id,entryNumber,title,movieName,singers,notes,musicDirector,language,postDate,isPostedConfirmed\n")
        allSongPosts.value.forEach { song ->
            sb.append("${song.id},")
              .append("${song.entryNumber},")
              .append("\"${song.title.replace("\"", "\"\"")}\",")
              .append("\"${song.movieName.replace("\"", "\"\"")}\",")
              .append("\"${song.singers.replace("\"", "\"\"")}\",")
              .append("\"${song.notes.replace("\"", "\"\"")}\",")
              .append("\"${song.musicDirector.replace("\"", "\"\"")}\",")
              .append("\"${song.language.replace("\"", "\"\"")}\",")
              .append("${song.postDate ?: ""},")
              .append("${song.isPostedConfirmed}\n")
        }
        return sb.toString()
    }

    fun exportBackupCsvIdeas(): String {
        val sb = StringBuilder()
        sb.append("id,title,content,category,color,isPosted,createdAt,updatedAt,isPinned\n")
        allIdeas.value.forEach { idea ->
            sb.append("${idea.id},")
              .append("\"${idea.title.replace("\"", "\"\"")}\",")
              .append("\"${idea.content.replace("\"", "\"\"")}\",")
              .append("\"${(idea.category ?: "").replace("\"", "\"\"")}\",")
              .append("${idea.color ?: ""},")
              .append("${idea.isPosted},")
              .append("${idea.createdAt},")
              .append("${idea.updatedAt},")
              .append("${idea.isPinned}\n")
        }
        return sb.toString()
    }

    fun importBackupCsvSongs(csvString: String): Boolean {
        if (csvString.isBlank()) return false
        return try {
            val lines = csvString.lines()
            if (lines.size < 2) return false
            
            val header = lines.first().split(",")
            val entryNumIdx = header.indexOf("entryNumber")
            val isPostedConfirmedIdx = header.indexOf("isPostedConfirmed")
            
            val rowsToImport = mutableListOf<List<String>>()
            for (i in 1 until lines.size) {
                val line = lines[i]
                if (line.isBlank()) continue
                val parts = parseCsvLine(line)
                if (parts.size >= 3) rowsToImport.add(parts)
            }
            
            viewModelScope.launch {
                var currentMax = repository.getMaxEntryNumber()
                rowsToImport.forEach { parts ->
                    try {
                        val titleIdx = if (entryNumIdx != -1) 2 else 1
                        val movieIdx = if (entryNumIdx != -1) 3 else 2
                        
                        if (parts.size <= titleIdx || parts.size <= movieIdx) {
                            android.util.Log.w("TrackerViewModel", "Skipping malformed CSV row: $parts")
                            return@forEach
                        }

                        val title = parts[titleIdx]
                        val movieName = parts[movieIdx]
                        val singers = if (parts.size > (if (entryNumIdx != -1) 4 else 3)) parts[if (entryNumIdx != -1) 4 else 3] else ""
                        
                        if (repository.checkDuplicate(title, movieName, singers) == null) {
                            val importedNum = if (entryNumIdx != -1) parts[entryNumIdx].toLongOrNull() ?: 0L else 0L
                            val finalNum = if (importedNum > 0) importedNum else {
                                currentMax++
                                currentMax
                            }
                            
                            val postDateStr = if (parts.size > (if (entryNumIdx != -1) 8 else 7)) parts[if (entryNumIdx != -1) 8 else 7] else ""
                            val postDate = postDateStr.toLongOrNull()?.let { normalizeDateToDayStart(it) }

                            repository.insertSongPost(SongPost(
                                entryNumber = finalNum,
                                title = title,
                                movieName = movieName,
                                singers = singers,
                                notes = if (parts.size > (if (entryNumIdx != -1) 5 else 4)) parts[if (entryNumIdx != -1) 5 else 4] else "",
                                musicDirector = if (parts.size > (if (entryNumIdx != -1) 6 else 5)) parts[if (entryNumIdx != -1) 6 else 5] else "",
                                language = if (parts.size > (if (entryNumIdx != -1) 7 else 6)) parts[if (entryNumIdx != -1) 7 else 6] else "",
                                postDate = postDate,
                                isPostedConfirmed = false
                            ))
                            if (finalNum > currentMax) currentMax = finalNum
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("TrackerViewModel", "Error importing CSV song row", e)
                    }
                }
            }
            true
        } catch (_: Exception) { false }
    }

    fun importBackupCsvIdeas(csvString: String): Boolean {
        if (csvString.isBlank()) return false
        return try {
            val lines = csvString.lines()
            if (lines.size < 2) return false
            
            val header = lines.first().split(",")
            val titleIdx = header.indexOf("title")
            val contentIdx = header.indexOf("content")
            val catIdx = header.indexOf("category")
            val colorIdx = header.indexOf("color")
            val isPostedIdx = header.indexOf("isPosted")
            val createdIdx = header.indexOf("createdAt")
            val updatedIdx = header.indexOf("updatedAt")
            val pinnedIdx = header.indexOf("isPinned")

            for (i in 1 until lines.size) {
                val line = lines[i]
                if (line.isBlank()) continue
                val parts = parseCsvLine(line)
                if (parts.size < 3) continue
                
                viewModelScope.launch {
                    repository.insertIdea(IdeaVaultEntry(
                        title = if (titleIdx != -1) parts[titleIdx] else parts[1],
                        content = if (contentIdx != -1) parts[contentIdx] else parts[2],
                        category = if (catIdx != -1 && parts.size > catIdx && parts[catIdx].isNotBlank()) parts[catIdx] else null,
                        color = if (colorIdx != -1 && parts.size > colorIdx && parts[colorIdx].isNotBlank()) parts[colorIdx].toLongOrNull() else null,
                        isPosted = if (isPostedIdx != -1 && parts.size > isPostedIdx) parts[isPostedIdx].toBoolean() else false,
                        createdAt = if (createdIdx != -1 && parts.size > createdIdx) parts[createdIdx].toLongOrNull() ?: System.currentTimeMillis() else System.currentTimeMillis(),
                        updatedAt = if (updatedIdx != -1 && parts.size > updatedIdx) parts[updatedIdx].toLongOrNull() ?: System.currentTimeMillis() else System.currentTimeMillis(),
                        isPinned = if (pinnedIdx != -1 && parts.size > pinnedIdx) parts[pinnedIdx].toBoolean() else false
                    ))
                }
            }
            true
        } catch (_: Exception) { false }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    current.append('\"')
                    i++
                } else inQuotes = !inQuotes
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString())
                current.setLength(0)
            } else current.append(c)
            i++
        }
        result.add(current.toString())
        return result
    }

    // --- File I/O Helpers for SAF ---
    fun writeToFile(uri: Uri, content: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { outputStream ->
                        OutputStreamWriter(outputStream).use { writer ->
                            writer.write(content)
                        }
                    }
                    true
                } catch (e: Exception) {
                    false
                }
            }
            onResult(success)
        }
    }

    fun readFromFile(uri: Uri, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val content = withContext(Dispatchers.IO) {
                try {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { inputStream ->
                        BufferedReader(InputStreamReader(inputStream)).use { reader ->
                            reader.readText()
                        }
                    }
                } catch (e: Exception) {
                    null
                }
            }
            onResult(content)
        }
    }
}

class TrackerViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TrackerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TrackerViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class DashboardStats(
    val totalSongs: Int = 0,
    val postedSongsCount: Int = 0,
    val scheduledSongsCount: Int = 0,
    val postingProgress: Int = 0,
    val duplicateSongsCount: Int = 0,
    val isLibraryEmpty: Boolean = true,
    val songsPostedToday: Int = 0,
    val songsLast7Days: Int = 0,
    val songsThisMonth: Int = 0,
    val mostPostedSinger: String = "N/A",
    val mostPostedMovie: String = "N/A",
    val mostUsedLanguage: String = "N/A",
    val pendingIdeasCount: Int = 0,
    val postedIdeasCount: Int = 0
)
