package com.voiceofmelody.songdailytracker.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.voiceofmelody.songdailytracker.data.local.AppDatabase
import com.voiceofmelody.songdailytracker.data.model.IdeaVaultEntry
import com.voiceofmelody.songdailytracker.data.model.Reminder
import com.voiceofmelody.songdailytracker.data.model.RepeatType
import com.voiceofmelody.songdailytracker.data.model.SongPost
import com.voiceofmelody.songdailytracker.data.model.Promotion
import com.voiceofmelody.songdailytracker.data.model.PaymentStatus
import com.voiceofmelody.songdailytracker.data.repository.TrackerRepository
import com.voiceofmelody.songdailytracker.util.NotificationHelper
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
import java.text.SimpleDateFormat
import java.util.*

enum class IdeaFilter { ALL, PENDING, POSTED }

enum class PromotionFilter { ALL, PAID, PENDING, PARTIAL }

enum class ViewMode { LIST, GRID }

enum class MatchLevel { EXACT, POSSIBLE, SIMILAR }

enum class SongStatus { SCHEDULED, POSTED }

data class MonthlyEarning(val monthYear: String, val amount: Double)

data class PromotionStats(
    val totalEarnings: Double = 0.0,
    val paidEarnings: Double = 0.0,
    val pendingEarnings: Double = 0.0,
    val partiallyPaidEarnings: Double = 0.0,
    val monthlyEarnings: List<MonthlyEarning> = emptyList()
)

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
    private val notificationHelper = NotificationHelper(application)

    // Time tracking for dynamic status
    private val _currentTime = MutableStateFlow(System.currentTimeMillis())
    val currentTime: StateFlow<Long> = _currentTime.asStateFlow()

    // Raw streams from DB
    val allSongPosts: StateFlow<List<SongPost>?>
    val allIdeas: StateFlow<List<IdeaVaultEntry>?>
    val allReminders: StateFlow<List<Reminder>?>
    val allPromotions: StateFlow<List<Promotion>?>

    // Search Query States
    val searchQuery = MutableStateFlow("")
    val ideasSearchQuery = MutableStateFlow("")
    val promotionSearchQuery = MutableStateFlow("")
    val ideasFilter = MutableStateFlow(IdeaFilter.ALL)
    val promotionStatusFilter = MutableStateFlow(PromotionFilter.ALL)
    val promotionViewMode = MutableStateFlow(ViewMode.LIST)

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TrackerRepository(database.songPostDao(), database.ideaVaultDao(), database.reminderDao(), database.promotionDao())
        
        allSongPosts = repository.allSongPosts
            .onEach { 
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

        allIdeas = repository.allIdeas
            .onEach { 
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

        allReminders = repository.allReminders
            .onEach { 
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

        allPromotions = repository.allPromotions
            .onEach { 
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

        // Setup strategic time refresh for content scheduling
        viewModelScope.launch {
            allSongPosts.collect { songs ->
                if (songs != null) {
                    updateCurrentTime()
                    scheduleNextRefresh(songs)
                }
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

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchedPromotions: StateFlow<List<Promotion>> = promotionSearchQuery
        .flatMapLatest { query ->
            repository.searchPromotions(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
    fun addSongPost(title: String, movieName: String, singers: String, notes: String, musicDirector: String, language: String, postDate: Long?, contentLink: String? = null) {
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
                    contentLink = contentLink?.trim(),
                    isPostedConfirmed = false // Legacy field, not used in v1.2 logic
                )
            )
        }
    }

    fun updateSongPost(id: Int, entryNumber: Long, title: String, movieName: String, singers: String, notes: String, musicDirector: String, language: String, postDate: Long?, contentLink: String? = null) {
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
                    contentLink = contentLink?.trim(),
                    isPostedConfirmed = false // Legacy field
                )
            )
        }
    }
    
    private var lastDeletedSong: SongPost? = null

    fun deleteSongPost(songPost: SongPost) {
        viewModelScope.launch {
            lastDeletedSong = songPost
            repository.deleteSongPost(songPost)
        }
    }

    fun undoDeleteSong() {
        lastDeletedSong?.let { song ->
            viewModelScope.launch {
                repository.insertSongPost(song)
                lastDeletedSong = null
            }
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

    private var lastDeletedIdea: IdeaVaultEntry? = null

    fun deleteIdea(idea: IdeaVaultEntry) {
        viewModelScope.launch {
            lastDeletedIdea = idea
            repository.deleteIdea(idea)
        }
    }

    fun undoDeleteIdea() {
        lastDeletedIdea?.let { idea ->
            viewModelScope.launch {
                repository.insertIdea(idea)
                lastDeletedIdea = null
            }
        }
    }

    // --- Reminder CRUD ---
    fun addReminder(title: String, note: String, reminderDate: Long, reminderTime: Long?, notificationsEnabled: Boolean, colorLabel: String?, repeatType: RepeatType = RepeatType.NONE) {
        viewModelScope.launch {
            val reminder = Reminder(
                title = title.trim(),
                note = note.trim(),
                reminderDate = normalizeDateToDayStart(reminderDate),
                reminderTime = reminderTime,
                repeatType = repeatType,
                notificationsEnabled = notificationsEnabled,
                colorLabel = colorLabel,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val id = repository.insertReminder(reminder)
            val savedReminder = reminder.copy(id = id.toInt())
            if (notificationsEnabled) {
                notificationHelper.scheduleReminder(savedReminder)
            }
        }
    }

    fun updateReminder(id: Int, title: String, note: String, reminderDate: Long, reminderTime: Long?, notificationsEnabled: Boolean, colorLabel: String?, createdAt: Long, repeatType: RepeatType = RepeatType.NONE) {
        viewModelScope.launch {
            val reminder = Reminder(
                id = id,
                title = title.trim(),
                note = note.trim(),
                reminderDate = normalizeDateToDayStart(reminderDate),
                reminderTime = reminderTime,
                repeatType = repeatType,
                notificationsEnabled = notificationsEnabled,
                colorLabel = colorLabel,
                createdAt = createdAt,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateReminder(reminder)
            notificationHelper.cancelReminder(id)
            if (notificationsEnabled) {
                notificationHelper.scheduleReminder(reminder)
            }
        }
    }

    private var lastDeletedReminder: Reminder? = null

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            lastDeletedReminder = reminder
            repository.deleteReminder(reminder)
            notificationHelper.cancelReminder(reminder.id)
        }
    }

    fun undoDeleteReminder() {
        lastDeletedReminder?.let { reminder ->
            viewModelScope.launch {
                val id = repository.insertReminder(reminder)
                val restored = reminder.copy(id = id.toInt())
                if (restored.notificationsEnabled) {
                    notificationHelper.scheduleReminder(restored)
                }
                lastDeletedReminder = null
            }
        }
    }

    // --- Promotion CRUD ---
    fun addPromotion(title: String, amount: Double, status: PaymentStatus, client: String?, link: String?, notes: String?, paymentDate: Long?) {
        viewModelScope.launch {
            repository.insertPromotion(Promotion(
                promotionTitle = title.trim(),
                amount = amount,
                paymentStatus = status,
                client = client?.trim(),
                contentLink = link?.trim(),
                notes = notes?.trim(),
                paymentDate = paymentDate?.let { normalizeDateToDayStart(it) }
            ))
        }
    }

    fun updatePromotion(id: Int, title: String, amount: Double, status: PaymentStatus, client: String?, link: String?, notes: String?, createdAt: Long, paymentDate: Long?) {
        viewModelScope.launch {
            repository.updatePromotion(Promotion(
                id = id,
                promotionTitle = title.trim(),
                amount = amount,
                paymentStatus = status,
                client = client?.trim(),
                contentLink = link?.trim(),
                notes = notes?.trim(),
                createdAt = createdAt,
                paymentDate = paymentDate?.let { normalizeDateToDayStart(it) }
            ))
        }
    }

    private var lastDeletedPromotion: Promotion? = null
    fun deletePromotion(promotion: Promotion) {
        viewModelScope.launch {
            lastDeletedPromotion = promotion
            repository.deletePromotion(promotion)
        }
    }

    fun undoDeletePromotion() {
        lastDeletedPromotion?.let { promo ->
            viewModelScope.launch {
                repository.insertPromotion(promo)
                lastDeletedPromotion = null
            }
        }
    }

    // --- Dashboard Stats Calculations ---
    val statsState: StateFlow<DashboardStats?> = combine(
        allSongPosts,
        allIdeas,
        allReminders,
        allPromotions,
        currentTime
    ) { songs, ideas, reminders, promotions, now ->
        val res = if (songs == null || ideas == null || reminders == null || promotions == null) {
            null
        } else {
            calculateStats(songs, ideas, reminders, promotions, now)
        }
        res
    }
    .flowOn(Dispatchers.Default)
    .distinctUntilChanged()
    .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val todayAgendaItems = combine(allSongPosts, allReminders, currentTime) { songs, reminders, now ->
        val res = if (songs == null || reminders == null) {
            emptyList<SongPost>() to emptyList<Reminder>()
        } else {
            val today = normalizeDateToDayStart(now)
            val todaySongs = songs.filter { it.postDate == today }
            val todayReminders = reminders.filter { it.isOccurringOn(today) }
            todaySongs to todayReminders
        }
        res
    }
    .flowOn(Dispatchers.Default)
    .distinctUntilChanged()
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList<SongPost>() to emptyList<Reminder>())

    private fun calculateStats(songs: List<SongPost>, ideas: List<IdeaVaultEntry>, reminders: List<Reminder>, promotions: List<Promotion>, now: Long): DashboardStats {
        if (songs.isEmpty() && ideas.isEmpty() && reminders.isEmpty() && promotions.isEmpty()) {
            return DashboardStats(isLibraryEmpty = true)
        }

        var postedCount = 0
        var scheduledCount = 0
        var songsPostedToday = 0
        var songsLast7Days = 0
        var songsThisMonth = 0
        var lastMonthPosted = 0
        
        val todayStart = normalizeDateToDayStart(now)
        val sevenDaysAgo = now - (7L * 24 * 60 * 60 * 1000)
        
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        calendar.add(Calendar.MONTH, -1)
        val lastMonth = calendar.get(Calendar.MONTH)
        val lastMonthYear = calendar.get(Calendar.YEAR)

        val singerMap = mutableMapOf<String, Int>()
        val movieMap = mutableMapOf<String, Int>()
        val languageMap = mutableMapOf<String, Int>()
        val dayOfWeekMap = mutableMapOf<Int, Int>()
        val heatmapMap = mutableMapOf<Long, Int>()
        val weeklyActivity = IntArray(7) { 0 }

        val startOfWeek = Calendar.getInstance().apply { 
            timeInMillis = todayStart
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            if (timeInMillis > todayStart) add(Calendar.WEEK_OF_YEAR, -1)
        }.timeInMillis

        songs.forEach { song ->
            val status = getSongStatus(song, now)
            if (status == SongStatus.POSTED) postedCount++
            else if (status == SongStatus.SCHEDULED) scheduledCount++

            song.postDate?.let { date ->
                val dayStart = normalizeDateToDayStart(date)
                
                if (date in todayStart..now) songsPostedToday++
                if (date in sevenDaysAgo..now) songsLast7Days++
                
                val c = Calendar.getInstance().apply { timeInMillis = date }
                val m = c.get(Calendar.MONTH)
                val y = c.get(Calendar.YEAR)
                
                if (date <= now) {
                    if (m == currentMonth && y == currentYear) songsThisMonth++
                    if (m == lastMonth && y == lastMonthYear) lastMonthPosted++
                    
                    // Frequencies
                    song.singers.split(",").forEach { 
                        val s = it.trim()
                        if (s.isNotBlank()) singerMap[s] = (singerMap[s] ?: 0) + 1
                    }
                    val movie = song.movieName.trim()
                    if (movie.isNotBlank()) movieMap[movie] = (movieMap[movie] ?: 0) + 1
                    val lang = song.language.trim()
                    if (lang.isNotBlank()) languageMap[lang] = (languageMap[lang] ?: 0) + 1
                    
                    val dow = c.get(Calendar.DAY_OF_WEEK)
                    dayOfWeekMap[dow] = (dayOfWeekMap[dow] ?: 0) + 1
                    
                    if (date >= startOfWeek) {
                        val idx = if (dow == Calendar.SUNDAY) 6 else dow - 2
                        if (idx in 0..6) weeklyActivity[idx]++
                    }

                    heatmapMap[dayStart] = (heatmapMap[dayStart] ?: 0) + 1
                }
            }
        }

        val categoryCounts = ideas.asSequence()
            .mapNotNull { it.category?.trim() }
            .filter { it.isNotBlank() }
            .groupBy { it }
            .mapValues { it.value.size }
        val mostUsedCategory = categoryCounts.maxByOrNull { it.value }?.key ?: "N/A"

        val favDayIdx = dayOfWeekMap.maxByOrNull { it.value }?.key
        val favDayName = when (favDayIdx) {
            Calendar.SUNDAY -> "Sunday"
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            else -> "N/A"
        }

        // Duplicate Stats
        val duplicateCount = songs.groupBy { 
                val title = it.title.lowercase().trim()
                val movie = it.movieName.lowercase().trim()
                val singers = it.singers.lowercase().trim()
                "$title|$movie|$singers"
            }.count { it.value.size > 1 }

        return DashboardStats(
            totalSongs = songs.size,
            postedSongsCount = postedCount,
            scheduledSongsCount = scheduledCount,
            postingProgress = if (songs.isNotEmpty()) (postedCount.toFloat() / songs.size * 100).toInt() else 0,
            duplicateSongsCount = duplicateCount,
            isLibraryEmpty = songs.isEmpty() && ideas.isEmpty(),
            songsPostedToday = songsPostedToday,
            songsLast7Days = songsLast7Days,
            songsThisMonth = songsThisMonth,
            mostPostedSinger = singerMap.maxByOrNull { it.value }?.key ?: "N/A",
            mostPostedMovie = movieMap.maxByOrNull { it.value }?.key ?: "N/A",
            mostUsedLanguage = languageMap.maxByOrNull { it.value }?.key ?: "N/A",
            pendingIdeasCount = ideas.count { !it.isPosted },
            postedIdeasCount = ideas.count { it.isPosted },
            currentStreak = calculateStreak(songs, todayStart),
            longestStreak = calculateLongestStreak(songs),
            weeklyActivity = weeklyActivity.toList(),
            lastMonthPostedCount = lastMonthPosted,
            favouritePostingDay = favDayName,
            mostUsedCategory = mostUsedCategory,
            activityHeatmap = heatmapMap,
            totalReminders = reminders.size,
            promotionStats = calculatePromotionStats(promotions)
        )
    }

    private fun calculatePromotionStats(promotions: List<Promotion>): PromotionStats {
        if (promotions.isEmpty()) return PromotionStats()

        var total = 0.0
        var paid = 0.0
        var pending = 0.0
        var partially = 0.0

        val monthlyMap = mutableMapOf<String, Double>()
        val cal = Calendar.getInstance()
        val internalKeySdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val displayLabelSdf = SimpleDateFormat("MMM", Locale.getDefault())

        // Last 6 months window initialization
        val last6MonthsInfo = mutableListOf<Pair<String, String>>() // InternalKey to DisplayLabel
        for (i in 5 downTo 0) {
            val c = Calendar.getInstance()
            c.add(Calendar.MONTH, -i)
            val internalKey = internalKeySdf.format(c.time)
            val displayLabel = displayLabelSdf.format(c.time)
            last6MonthsInfo.add(internalKey to displayLabel)
            monthlyMap[internalKey] = 0.0
        }

        promotions.forEach { promo ->
            total += promo.amount
            when (promo.paymentStatus) {
                PaymentStatus.PAID -> {
                    paid += promo.amount
                    
                    // FIX 1 & 2: Only PAID promotions contribute to the monthly chart, using paymentDate.
                    // FIX 3: Fallback to createdAt if paymentDate is null.
                    cal.timeInMillis = promo.paymentDate ?: promo.createdAt
                    val internalKey = internalKeySdf.format(cal.time)
                    
                    if (monthlyMap.containsKey(internalKey)) {
                        monthlyMap[internalKey] = monthlyMap[internalKey]!! + promo.amount
                    }
                }
                PaymentStatus.PENDING -> pending += promo.amount
                PaymentStatus.PARTIALLY_PAID -> partially += promo.amount
            }
        }

        val monthlyEarningsList = last6MonthsInfo.map { (key, label) ->
            MonthlyEarning(label, monthlyMap[key] ?: 0.0)
        }

        return PromotionStats(
            totalEarnings = total,
            paidEarnings = paid,
            pendingEarnings = pending,
            partiallyPaidEarnings = partially,
            monthlyEarnings = monthlyEarningsList
        )
    }

    private fun calculateStreak(songs: List<SongPost>, todayStart: Long): Int {
        val uniqueDays = songs.asSequence()
            .filter { it.postDate != null && it.postDate <= System.currentTimeMillis() }
            .map { normalizeDateToDayStart(it.postDate!!) }
            .distinct()
            .sortedDescending()
            .toList()

        if (uniqueDays.isEmpty()) return 0
        val first = uniqueDays.first()
        if (first == todayStart || first == todayStart - (24 * 60 * 60 * 1000)) {
            var streak = 1
            for (i in 0 until uniqueDays.size - 1) {
                if (uniqueDays[i] - uniqueDays[i+1] == (24 * 60 * 60 * 1000L)) {
                    streak++
                } else break
            }
            return streak
        }
        return 0
    }

    private fun calculateLongestStreak(songs: List<SongPost>): Int {
        val uniqueDays = songs.asSequence()
            .filter { it.postDate != null && it.postDate <= System.currentTimeMillis() }
            .map { normalizeDateToDayStart(it.postDate!!) }
            .distinct()
            .sorted()
            .toList()

        if (uniqueDays.isEmpty()) return 0
        var longest = 1
        var current = 1
        for (i in 0 until uniqueDays.size - 1) {
            if (uniqueDays[i+1] - uniqueDays[i] == (24 * 60 * 60 * 1000L)) {
                current++
            } else {
                longest = maxOf(longest, current)
                current = 1
            }
        }
        return maxOf(longest, current)
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
            allSongPosts.value?.forEach { song ->
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
                    put("contentLink", song.contentLink ?: JSONObject.NULL)
                    put("isPostedConfirmed", song.isPostedConfirmed)
                })
            }
            root.put("songs", songsArray)

            val ideasArray = JSONArray()
            allIdeas.value?.forEach { idea ->
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

            val remindersArray = JSONArray()
            allReminders.value?.forEach { reminder ->
                remindersArray.put(JSONObject().apply {
                    put("id", reminder.id)
                    put("title", reminder.title)
                    put("note", reminder.note)
                    put("reminderDate", reminder.reminderDate)
                    put("reminderTime", reminder.reminderTime ?: JSONObject.NULL)
                    put("notificationsEnabled", reminder.notificationsEnabled)
                    put("colorLabel", reminder.colorLabel ?: JSONObject.NULL)
                    put("createdAt", reminder.createdAt)
                    put("updatedAt", reminder.updatedAt)
                })
            }
            root.put("reminders", remindersArray)

            val promotionsArray = JSONArray()
            allPromotions.value?.forEach { promo ->
                promotionsArray.put(JSONObject().apply {
                    put("id", promo.id)
                    put("promotionTitle", promo.promotionTitle)
                    put("amount", promo.amount)
                    put("paymentStatus", promo.paymentStatus.name)
                    put("client", promo.client ?: JSONObject.NULL)
                    put("contentLink", promo.contentLink ?: JSONObject.NULL)
                    put("notes", promo.notes ?: JSONObject.NULL)
                    put("createdAt", promo.createdAt)
                    put("paymentDate", promo.paymentDate ?: JSONObject.NULL)
                })
            }
            root.put("promotions", promotionsArray)

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
                                    contentLink = if (sObj.isNull("contentLink")) null else sObj.getString("contentLink"),
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
            if (root.has("reminders")) {
                val remindersArray = root.getJSONArray("reminders")
                for (i in 0 until remindersArray.length()) {
                    val rObj = remindersArray.getJSONObject(i)
                    viewModelScope.launch {
                        try {
                            if (!rObj.has("title") || !rObj.has("reminderDate")) {
                                android.util.Log.w("TrackerViewModel", "Skipping malformed JSON reminder entry: $rObj")
                                return@launch
                            }
                            repository.insertReminder(Reminder(
                                title = rObj.getString("title"),
                                note = rObj.optString("note", ""),
                                reminderDate = normalizeDateToDayStart(rObj.getLong("reminderDate")),
                                reminderTime = if (rObj.isNull("reminderTime")) null else rObj.getLong("reminderTime"),
                                notificationsEnabled = rObj.optBoolean("notificationsEnabled", false),
                                colorLabel = if (rObj.isNull("colorLabel")) null else rObj.getString("colorLabel"),
                                createdAt = rObj.optLong("createdAt", System.currentTimeMillis()),
                                updatedAt = rObj.optLong("updatedAt", System.currentTimeMillis())
                            ))
                        } catch (e: Exception) {
                            android.util.Log.e("TrackerViewModel", "Error importing JSON reminder entry", e)
                        }
                    }
                }
            }
            if (root.has("promotions")) {
                val promoArray = root.getJSONArray("promotions")
                for (i in 0 until promoArray.length()) {
                    val pObj = promoArray.getJSONObject(i)
                    viewModelScope.launch {
                        try {
                            if (!pObj.has("promotionTitle") || !pObj.has("amount")) return@launch
                            repository.insertPromotion(Promotion(
                                promotionTitle = pObj.getString("promotionTitle"),
                                amount = pObj.getDouble("amount"),
                                paymentStatus = PaymentStatus.valueOf(pObj.optString("paymentStatus", "PENDING")),
                                client = if (pObj.isNull("client")) null else pObj.getString("client"),
                                contentLink = if (pObj.isNull("contentLink")) null else pObj.getString("contentLink"),
                                notes = if (pObj.isNull("notes")) null else pObj.getString("notes"),
                                createdAt = pObj.optLong("createdAt", System.currentTimeMillis()),
                                paymentDate = if (pObj.isNull("paymentDate")) null else normalizeDateToDayStart(pObj.getLong("paymentDate"))
                            ))
                        } catch (_: Exception) {}
                    }
                }
            }
            true
        } catch (_: Exception) { false }
    }

    // --- CSV Backup & Export ---
    fun exportBackupCsvSongs(): String {
        val sb = StringBuilder()
        sb.append("id,entryNumber,title,movieName,singers,notes,musicDirector,language,postDate,contentLink,isPostedConfirmed\n")
        allSongPosts.value?.forEach { song ->
            sb.append("${song.id},")
              .append("${song.entryNumber},")
              .append("\"${song.title.replace("\"", "\"\"")}\",")
              .append("\"${song.movieName.replace("\"", "\"\"")}\",")
              .append("\"${song.singers.replace("\"", "\"\"")}\",")
              .append("\"${song.notes.replace("\"", "\"\"")}\",")
              .append("\"${song.musicDirector.replace("\"", "\"\"")}\",")
              .append("\"${song.language.replace("\"", "\"\"")}\",")
              .append("${song.postDate ?: ""},")
              .append("\"${(song.contentLink ?: "").replace("\"", "\"\"")}\",")
              .append("${song.isPostedConfirmed}\n")
        }
        return sb.toString()
    }

    fun exportBackupCsvIdeas(): String {
        val sb = StringBuilder()
        sb.append("id,title,content,category,color,isPosted,createdAt,updatedAt,isPinned\n")
        allIdeas.value?.forEach { idea ->
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

    fun exportBackupCsvReminders(): String {
        val sb = StringBuilder()
        sb.append("id,title,note,reminderDate,reminderTime,notificationsEnabled,colorLabel,createdAt,updatedAt\n")
        allReminders.value?.forEach { reminder ->
            sb.append("${reminder.id},")
              .append("\"${reminder.title.replace("\"", "\"\"")}\",")
              .append("\"${reminder.note.replace("\"", "\"\"")}\",")
              .append("${reminder.reminderDate},")
              .append("${reminder.reminderTime ?: ""},")
              .append("${reminder.notificationsEnabled},")
              .append("\"${(reminder.colorLabel ?: "").replace("\"", "\"\"")}\",")
              .append("${reminder.createdAt},")
              .append("${reminder.updatedAt}\n")
        }
        return sb.toString()
    }

    fun exportBackupCsvPromotions(): String {
        val sb = StringBuilder()
        sb.append("id,promotionTitle,amount,paymentStatus,client,contentLink,notes,createdAt,paymentDate\n")
        allPromotions.value?.forEach { promo ->
            sb.append("${promo.id},")
              .append("\"${promo.promotionTitle.replace("\"", "\"\"")}\",")
              .append("${promo.amount},")
              .append("${promo.paymentStatus.name},")
              .append("\"${(promo.client ?: "").replace("\"", "\"\"")}\",")
              .append("\"${(promo.contentLink ?: "").replace("\"", "\"\"")}\",")
              .append("\"${(promo.notes ?: "").replace("\"", "\"\"")}\",")
              .append("${promo.createdAt},")
              .append("${promo.paymentDate ?: ""}\n")
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
            val titleIdx = header.indexOf("title")
            val movieIdx = header.indexOf("movieName")
            val singersIdx = header.indexOf("singers")
            val notesIdx = header.indexOf("notes")
            val directorIdx = header.indexOf("musicDirector")
            val langIdx = header.indexOf("language")
            val dateIdx = header.indexOf("postDate")
            val linkIdx = header.indexOf("contentLink")
            
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
                        val title = if (titleIdx != -1 && parts.size > titleIdx) parts[titleIdx] else ""
                        val movieName = if (movieIdx != -1 && parts.size > movieIdx) parts[movieIdx] else ""
                        
                        if (title.isBlank() || movieName.isBlank()) return@forEach

                        val singers = if (singersIdx != -1 && parts.size > singersIdx) parts[singersIdx] else ""
                        
                        if (repository.checkDuplicate(title, movieName, singers) == null) {
                            val importedNum = if (entryNumIdx != -1 && parts.size > entryNumIdx) parts[entryNumIdx].toLongOrNull() ?: 0L else 0L
                            val finalNum = if (importedNum > 0) importedNum else {
                                currentMax++
                                currentMax
                            }
                            
                            val postDate = if (dateIdx != -1 && parts.size > dateIdx) parts[dateIdx].toLongOrNull()?.let { normalizeDateToDayStart(it) } else null
                            val contentLink = if (linkIdx != -1 && parts.size > linkIdx && parts[linkIdx].isNotBlank()) parts[linkIdx] else null

                            repository.insertSongPost(SongPost(
                                entryNumber = finalNum,
                                title = title,
                                movieName = movieName,
                                singers = singers,
                                notes = if (notesIdx != -1 && parts.size > notesIdx) parts[notesIdx] else "",
                                musicDirector = if (directorIdx != -1 && parts.size > directorIdx) parts[directorIdx] else "",
                                language = if (langIdx != -1 && parts.size > langIdx) parts[langIdx] else "",
                                postDate = postDate,
                                contentLink = contentLink,
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
                    try {
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
                    } catch (_: Exception) {}
                }
            }
            true
        } catch (_: Exception) { false }
    }

    fun importBackupCsvReminders(csvString: String): Boolean {
        if (csvString.isBlank()) return false
        return try {
            val lines = csvString.lines()
            if (lines.size < 2) return false
            
            val header = lines.first().split(",")
            val titleIdx = header.indexOf("title")
            val noteIdx = header.indexOf("note")
            val dateIdx = header.indexOf("reminderDate")
            val timeIdx = header.indexOf("reminderTime")
            val notifyIdx = header.indexOf("notificationsEnabled")
            val colorIdx = header.indexOf("colorLabel")
            val createdIdx = header.indexOf("createdAt")
            val updatedIdx = header.indexOf("updatedAt")

            for (i in 1 until lines.size) {
                val line = lines[i]
                if (line.isBlank()) continue
                val parts = parseCsvLine(line)
                if (parts.size < 3) continue
                
                viewModelScope.launch {
                    try {
                        val title = if (titleIdx != -1) parts[titleIdx] else parts[1]
                        val date = if (dateIdx != -1) parts[dateIdx].toLongOrNull() ?: 0L else parts[3].toLongOrNull() ?: 0L
                        
                        repository.insertReminder(Reminder(
                            title = title,
                            note = if (noteIdx != -1 && parts.size > noteIdx) parts[noteIdx] else "",
                            reminderDate = normalizeDateToDayStart(date),
                            reminderTime = if (timeIdx != -1 && parts.size > timeIdx && parts[timeIdx].isNotBlank()) parts[timeIdx].toLongOrNull() else null,
                            notificationsEnabled = if (notifyIdx != -1 && parts.size > notifyIdx) parts[notifyIdx].toBoolean() else false,
                            colorLabel = if (colorIdx != -1 && parts.size > colorIdx && parts[colorIdx].isNotBlank()) parts[colorIdx] else null,
                            createdAt = if (createdIdx != -1 && parts.size > createdIdx) parts[createdIdx].toLongOrNull() ?: System.currentTimeMillis() else System.currentTimeMillis(),
                            updatedAt = if (updatedIdx != -1 && parts.size > updatedIdx) parts[updatedIdx].toLongOrNull() ?: System.currentTimeMillis() else System.currentTimeMillis()
                        ))
                    } catch (_: Exception) {}
                }
            }
            true
        } catch (_: Exception) { false }
    }

    fun importBackupCsvPromotions(csvString: String): Boolean {
        if (csvString.isBlank()) return false
        return try {
            val lines = csvString.lines()
            if (lines.size < 2) return false
            
            val header = lines.first().split(",")
            val titleIdx = header.indexOf("promotionTitle")
            val amountIdx = header.indexOf("amount")
            val statusIdx = header.indexOf("paymentStatus")
            val clientIdx = header.indexOf("client")
            val linkIdx = header.indexOf("contentLink")
            val notesIdx = header.indexOf("notes")
            val createdIdx = header.indexOf("createdAt")
            val paymentDateIdx = header.indexOf("paymentDate")

            for (i in 1 until lines.size) {
                val line = lines[i]
                if (line.isBlank()) continue
                val parts = parseCsvLine(line)
                if (parts.size < 3) continue
                
                viewModelScope.launch {
                    try {
                        val title = if (titleIdx != -1 && parts.size > titleIdx) parts[titleIdx] else parts[1]
                        val amount = if (amountIdx != -1 && parts.size > amountIdx) parts[amountIdx].toDoubleOrNull() ?: 0.0 else 0.0
                        if (title.isBlank() || amount <= 0.0) return@launch

                        val paymentDate = if (paymentDateIdx != -1 && parts.size > paymentDateIdx && parts[paymentDateIdx].isNotBlank()) {
                            parts[paymentDateIdx].toLongOrNull()?.let { normalizeDateToDayStart(it) }
                        } else null

                        repository.insertPromotion(Promotion(
                            promotionTitle = title,
                            amount = amount,
                            paymentStatus = if (statusIdx != -1 && parts.size > statusIdx) {
                                try { PaymentStatus.valueOf(parts[statusIdx]) } catch(_: Exception) { PaymentStatus.PENDING }
                            } else PaymentStatus.PENDING,
                            client = if (clientIdx != -1 && parts.size > clientIdx && parts[clientIdx].isNotBlank()) parts[clientIdx] else null,
                            contentLink = if (linkIdx != -1 && parts.size > linkIdx && parts[linkIdx].isNotBlank()) parts[linkIdx] else null,
                            notes = if (notesIdx != -1 && parts.size > notesIdx && parts[notesIdx].isNotBlank()) parts[notesIdx] else null,
                            createdAt = if (createdIdx != -1 && parts.size > createdIdx) parts[createdIdx].toLongOrNull() ?: System.currentTimeMillis() else System.currentTimeMillis(),
                            paymentDate = paymentDate
                        ))
                    } catch (_: Exception) {}
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
    val postedIdeasCount: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val weeklyActivity: List<Int> = List(7) { 0 },
    val lastMonthPostedCount: Int = 0,
    val favouritePostingDay: String = "N/A",
    val mostUsedCategory: String = "N/A",
    val activityHeatmap: Map<Long, Int> = emptyMap(),
    val totalReminders: Int = 0,
    val promotionStats: PromotionStats = PromotionStats()
)
