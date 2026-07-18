package com.voiceofmelody.songdailytracker.data.local

import androidx.room.*
import com.voiceofmelody.songdailytracker.data.model.IdeaVaultEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface IdeaVaultDao {
    @Query("SELECT * FROM idea_vault ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllIdeas(): Flow<List<IdeaVaultEntry>>

    @Query("SELECT * FROM idea_vault WHERE title LIKE :query OR content LIKE :query OR category LIKE :query ORDER BY isPinned DESC, updatedAt DESC")
    fun searchIdeas(query: String): Flow<List<IdeaVaultEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIdea(idea: IdeaVaultEntry)

    @Update
    suspend fun updateIdea(idea: IdeaVaultEntry)

    @Delete
    suspend fun deleteIdea(idea: IdeaVaultEntry)
}
