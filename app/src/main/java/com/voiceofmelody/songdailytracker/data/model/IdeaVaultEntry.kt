package com.voiceofmelody.songdailytracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "idea_vault")
data class IdeaVaultEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val category: String? = null,
    val color: Long? = null,
    val isPosted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false
)
