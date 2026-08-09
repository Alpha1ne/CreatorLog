package com.voiceofmelody.songdailytracker.data.repository

import com.voiceofmelody.songdailytracker.data.local.SongPostDao
import com.voiceofmelody.songdailytracker.data.local.IdeaVaultDao
import com.voiceofmelody.songdailytracker.data.local.ReminderDao
import com.voiceofmelody.songdailytracker.data.local.PromotionDao
import com.voiceofmelody.songdailytracker.data.model.IdeaVaultEntry
import com.voiceofmelody.songdailytracker.data.model.SongPost
import com.voiceofmelody.songdailytracker.data.model.Reminder
import com.voiceofmelody.songdailytracker.data.model.Promotion
import kotlinx.coroutines.flow.Flow

class TrackerRepository(
    private val songPostDao: SongPostDao,
    private val ideaVaultDao: IdeaVaultDao,
    private val reminderDao: ReminderDao,
    private val promotionDao: PromotionDao
) {
    val allSongPosts: Flow<List<SongPost>> = songPostDao.getAllSongPosts()
    val allIdeas: Flow<List<IdeaVaultEntry>> = ideaVaultDao.getAllIdeas()
    val allReminders: Flow<List<Reminder>> = reminderDao.getAllReminders()
    val allPromotions: Flow<List<Promotion>> = promotionDao.getAllPromotions()

    fun searchSongPosts(query: String): Flow<List<SongPost>> {
        return if (query.isBlank()) {
            allSongPosts
        } else {
            songPostDao.searchSongPosts("%$query%")
        }
    }

    fun searchIdeas(query: String): Flow<List<IdeaVaultEntry>> {
        return if (query.isBlank()) {
            allIdeas
        } else {
            ideaVaultDao.searchIdeas("%$query%")
        }
    }

    suspend fun insertSongPost(songPost: SongPost) = songPostDao.insertSongPost(songPost)
    suspend fun updateSongPost(songPost: SongPost) = songPostDao.updateSongPost(songPost)
    suspend fun deleteSongPost(songPost: SongPost) = songPostDao.deleteSongPost(songPost)

    suspend fun getPotentialDuplicates(title: String): List<SongPost> {
        return songPostDao.findPotentialDuplicates(title)
    }

    suspend fun checkDuplicate(title: String, movie: String, singers: String): SongPost? {
        val matches = songPostDao.findPotentialDuplicates(title)
        val cleanMovie = movie.lowercase().trim()
        val cleanSingers = singers.lowercase().trim()
        return matches.find { 
            it.movieName.lowercase().trim() == cleanMovie && 
            it.singers.lowercase().trim() == cleanSingers 
        }
    }

    suspend fun getMaxEntryNumber(): Long {
        return songPostDao.getMaxEntryNumber() ?: 0L
    }

    suspend fun insertIdea(idea: IdeaVaultEntry) = ideaVaultDao.insertIdea(idea)
    suspend fun updateIdea(idea: IdeaVaultEntry) = ideaVaultDao.updateIdea(idea)
    suspend fun deleteIdea(idea: IdeaVaultEntry) = ideaVaultDao.deleteIdea(idea)

    // --- Reminder Operations ---
    suspend fun insertReminder(reminder: Reminder): Long = reminderDao.insertReminder(reminder)
    suspend fun updateReminder(reminder: Reminder) = reminderDao.updateReminder(reminder)
    suspend fun deleteReminder(reminder: Reminder) = reminderDao.deleteReminder(reminder)

    // --- Promotion Operations ---
    suspend fun insertPromotion(promotion: Promotion) = promotionDao.insertPromotion(promotion)
    suspend fun updatePromotion(promotion: Promotion) = promotionDao.updatePromotion(promotion)
    suspend fun deletePromotion(promotion: Promotion) = promotionDao.deletePromotion(promotion)
    fun searchPromotions(query: String): Flow<List<Promotion>> = promotionDao.searchPromotions("%$query%")
}
