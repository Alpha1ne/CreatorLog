package com.voiceofmelody.songdailytracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.voiceofmelody.songdailytracker.data.model.IdeaVaultEntry
import com.voiceofmelody.songdailytracker.data.model.Reminder
import com.voiceofmelody.songdailytracker.data.model.RepeatType
import com.voiceofmelody.songdailytracker.data.model.SongPost

class Converters {
    @androidx.room.TypeConverter
    fun fromRepeatType(value: RepeatType): String {
        return value.name
    }

    @androidx.room.TypeConverter
    fun toRepeatType(value: String): RepeatType {
        return try {
            RepeatType.valueOf(value)
        } catch (e: Exception) {
            RepeatType.NONE
        }
    }
}

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

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // --- Migrate song_posts ---
        // 1. Create new table with exact schema expected by Room (postDate is nullable, isPostedConfirmed is added as NOT NULL)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `song_posts_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `entryNumber` INTEGER NOT NULL, 
                `title` TEXT NOT NULL, 
                `movieName` TEXT NOT NULL, 
                `singers` TEXT NOT NULL, 
                `notes` TEXT NOT NULL, 
                `musicDirector` TEXT NOT NULL, 
                `language` TEXT NOT NULL, 
                `postDate` INTEGER, 
                `isPostedConfirmed` INTEGER NOT NULL
            )
        """.trimIndent())

        // 2. Copy data. Set isPostedConfirmed = 1 for existing songs to preserve their "Posted" status.
        db.execSQL("""
            INSERT INTO song_posts_new (id, entryNumber, title, movieName, singers, notes, musicDirector, language, postDate, isPostedConfirmed)
            SELECT id, entryNumber, title, movieName, singers, notes, musicDirector, language, postDate, 1 FROM song_posts
        """.trimIndent())

        // 3. Drop old table and rename new one
        db.execSQL("DROP TABLE song_posts")
        db.execSQL("ALTER TABLE song_posts_new RENAME TO song_posts")

        // --- Migrate idea_vault (to remove metadata DEFAULT 0 which causes Room validation failure) ---
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `idea_vault_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `title` TEXT NOT NULL, 
                `content` TEXT NOT NULL, 
                `category` TEXT, 
                `color` INTEGER, 
                `isPosted` INTEGER NOT NULL, 
                `createdAt` INTEGER NOT NULL, 
                `updatedAt` INTEGER NOT NULL, 
                `isPinned` INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            INSERT INTO idea_vault_new (id, title, content, category, color, isPosted, createdAt, updatedAt, isPinned)
            SELECT id, title, content, category, color, isPosted, createdAt, updatedAt, isPinned FROM idea_vault
        """.trimIndent())

        db.execSQL("DROP TABLE idea_vault")
        db.execSQL("ALTER TABLE idea_vault_new RENAME TO idea_vault")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `reminders` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `title` TEXT NOT NULL, 
                `note` TEXT NOT NULL, 
                `reminderDate` INTEGER NOT NULL, 
                `reminderTime` INTEGER, 
                `notificationsEnabled` INTEGER NOT NULL, 
                `colorLabel` TEXT, 
                `createdAt` INTEGER NOT NULL, 
                `updatedAt` INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE reminders ADD COLUMN repeatType TEXT NOT NULL DEFAULT 'NONE'")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE song_posts ADD COLUMN contentLink TEXT")
    }
}

@Database(entities = [SongPost::class, IdeaVaultEntry::class, Reminder::class], version = 9, exportSchema = false)
@androidx.room.TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songPostDao(): SongPostDao
    abstract fun ideaVaultDao(): IdeaVaultDao
    abstract fun reminderDao(): ReminderDao

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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
