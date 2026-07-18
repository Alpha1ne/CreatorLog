package com.voiceofmelody.songdailytracker.data.repository

import com.voiceofmelody.songdailytracker.data.local.SongPostDao
import com.voiceofmelody.songdailytracker.data.local.IdeaVaultDao
import com.voiceofmelody.songdailytracker.data.model.IdeaVaultEntry
import com.voiceofmelody.songdailytracker.data.model.SongPost
import kotlinx.coroutines.flow.Flow

class TrackerRepository(
    private val songPostDao: SongPostDao,
    private val ideaVaultDao: IdeaVaultDao
) {
    val allSongPosts: Flow<List<SongPost>> = songPostDao.getAllSongPosts()
    val allIdeas: Flow<List<IdeaVaultEntry>> = ideaVaultDao.getAllIdeas()

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
}
