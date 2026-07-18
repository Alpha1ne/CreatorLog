package com.voiceofmelody.songdailytracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.voiceofmelody.songdailytracker.data.model.IdeaVaultEntry
import com.voiceofmelody.songdailytracker.data.model.SongPost

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE song_posts ADD COLUMN musicDirector TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE song_posts ADD COLUMN language TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE paid_promotions ADD COLUMN isPaid INTEGER NOT NULL DEFAULT 1")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create idea_vault table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `idea_vault` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `title` TEXT NOT NULL, 
                `content` TEXT NOT NULL, 
                `category` TEXT, 
                `color` INTEGER, 
                `createdAt` INTEGER NOT NULL, 
                `updatedAt` INTEGER NOT NULL, 
                `isPinned` INTEGER NOT NULL
            )
        """.trimIndent())
        
        // Drop paid_promotions table
        db.execSQL("DROP TABLE IF EXISTS `paid_promotions`")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Add entryNumber column
        db.execSQL("ALTER TABLE song_posts ADD COLUMN entryNumber INTEGER NOT NULL DEFAULT 0")
        
        // 2. Assign sequential numbers based on postDate (oldest first)
        // We use a subquery to calculate the rank for each row.
        db.execSQL("""
            UPDATE song_posts 
            SET entryNumber = (
                SELECT COUNT(*) 
                FROM song_posts AS p2 
                WHERE p2.postDate < song_posts.postDate 
                   OR (p2.postDate = song_posts.postDate AND p2.id <= song_posts.id)
            )
        """.trimIndent())
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE idea_vault ADD COLUMN isPosted INTEGER NOT NULL DEFAULT 0")
    }
}

@Database(entities = [SongPost::class, IdeaVaultEntry::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songPostDao(): SongPostDao
    abstract fun ideaVaultDao(): IdeaVaultDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "song_tracker_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
