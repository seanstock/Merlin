package com.example.merlin.database

import androidx.room.Database
import androidx.room.RoomDatabase
// import androidx.room.migration.Migration // Will be needed for actual migrations
// import androidx.sqlite.db.SupportSQLiteDatabase // Will be needed for actual migrations
import com.example.merlin.data.database.dao.*
import com.example.merlin.data.database.entities.*

// Define a placeholder entity if abstract class without entities causes issues during initial setup.
// For now, we'll keep it commented out as Room might allow it for initial KAPT processing.
/*
@Entity
data class PlaceholderEntity(
    @PrimaryKey val id: Int
)
*/

// The @Database annotation will be fully configured in Subtask 3.2 (Entity Class Creation)
// For now, it's commented or minimal to allow KAPT to process the abstract class structure if needed.
@Database(
    entities = [
        ChildProfile::class,
        ParentSettings::class,
        TaskHistory::class,
        GameHistory::class,
        ChatHistory::class,
        Memory::class,
        SubjectMastery::class,
        EconomyState::class,
        DailyUsageLog::class,
        DeviceState::class,
        Badge::class,
        Experience::class,
        XpTransaction::class,
        // Curriculum entities
        CurriculumEntity::class,
        CurriculumProgressEntity::class,
        LessonProgressEntity::class,
        TaskProgressEntity::class
    ],
    version = 4, // Incremented for lesson_progress schema change
    exportSchema = true // Changed to true to export schema
)
// SQLCipher Performance Note:
// Encryption adds overhead. Operations like inserts can be ~25% slower,
// indexed queries ~5% slower, and non-indexed queries significantly slower (e.g., up to 10x).
// Optimize queries and use indices judiciously. Consider performing bulk operations in transactions.
abstract class MerlinDatabase : RoomDatabase() {
    abstract fun childProfileDao(): ChildProfileDao
    abstract fun parentSettingsDao(): ParentSettingsDao
    abstract fun taskHistoryDao(): TaskHistoryDao
    abstract fun gameHistoryDao(): GameHistoryDao
    abstract fun chatHistoryDao(): ChatHistoryDao
    abstract fun memoryDao(): MemoryDao
    abstract fun subjectMasteryDao(): SubjectMasteryDao
    abstract fun economyStateDao(): EconomyStateDao
    abstract fun dailyUsageLogDao(): DailyUsageLogDao
    abstract fun deviceStateDao(): DeviceStateDao
    abstract fun badgeDao(): BadgeDao
    abstract fun experienceDao(): ExperienceDao
    abstract fun xpTransactionDao(): XpTransactionDao
    
    // Curriculum DAO
    abstract fun curriculumDao(): CurriculumDao

    // DAOs will be defined here in Subtask 3.3 (DAO Interface Implementation)
    // abstract fun placeholderDao(): PlaceholderDao // Example placeholder DAO
    // ... and so on for other DAOs

    companion object {
        // Example: Migration from version 2 to 3 (curriculum tables)
        // val MIGRATION_2_3 = object : Migration(2, 3) {
        //     override fun migrate(db: SupportSQLiteDatabase) {
        //         // Create curriculum tables
        //         db.execSQL("""
        //             CREATE TABLE IF NOT EXISTS curricula (
        //                 id TEXT PRIMARY KEY NOT NULL,
        //                 title TEXT NOT NULL,
        //                 description TEXT NOT NULL,
        //                 gradeLevel TEXT NOT NULL,
        //                 subject TEXT NOT NULL,
        //                 lessonsJson TEXT NOT NULL
        //             )
        //         """)
        //         // ... other table creation SQL
        //     }
        // }
        // Add more migrations as needed: MIGRATION_3_4, etc.

        // Note on Plaintext to SQLCipher Migration (for future reference if ever needed):
        // If migrating an existing *unencrypted* SQLite database to use SQLCipher with Room,
        // the process typically involves:
        // 1. Opening the existing plaintext database.
        // 2. Attaching a new, empty, encrypted SQLCipher database.
        // 3. Using the SQLCipher `sqlcipher_export('main')` command (or `sqlcipher_export('main', 'attached_db_alias')`)
        //    to copy data into the attached encrypted database.
        // 4. Detaching databases and then using the new encrypted database file with Room/SQLCipher.
        // This requires careful file management and is not part of the initial setup for this app,
        // as we are starting directly with an encrypted database.
    }

    // The actual database instance will be provided by a DI framework (e.g., Hilt)
    // which will also handle the openHelperFactory with SQLCipher using DatabaseKeyProvider.
} 