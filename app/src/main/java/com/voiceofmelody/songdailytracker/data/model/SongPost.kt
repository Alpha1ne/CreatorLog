package com.voiceofmelody.songdailytracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "song_posts")
data class SongPost(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val entryNumber: Long = 0,
    val title: String,
    val movieName: String,
    val singers: String,
    val notes: String,
    val musicDirector: String = "",
    val language: String = "",
    val postDate: Long? = null,
    val contentLink: String? = null,
    val isPostedConfirmed: Boolean = false
)
