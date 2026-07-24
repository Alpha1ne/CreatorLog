package com.voiceofmelody.songdailytracker.data.local

import androidx.room.*
import com.voiceofmelody.songdailytracker.data.model.SongPost
import kotlinx.coroutines.flow.Flow

@Dao
interface SongPostDao {
    @Query("SELECT * FROM song_posts ORDER BY postDate DESC")
    fun getAllSongPosts(): Flow<List<SongPost>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongPost(songPost: SongPost)

    @Update
    suspend fun updateSongPost(songPost: SongPost)

    @Delete
    suspend fun deleteSongPost(songPost: SongPost)

    @Query("SELECT * FROM song_posts WHERE title LIKE :query OR movieName LIKE :query OR singers LIKE :query OR musicDirector LIKE :query OR language LIKE :query OR notes LIKE :query ORDER BY COALESCE(postDate, 0) DESC")
    fun searchSongPosts(query: String): Flow<List<SongPost>>

    @Query("SELECT * FROM song_posts WHERE LOWER(TRIM(title)) = LOWER(TRIM(:title))")
    suspend fun findPotentialDuplicates(title: String): List<SongPost>

    @Query("SELECT MAX(entryNumber) FROM song_posts")
    suspend fun getMaxEntryNumber(): Long?
}
