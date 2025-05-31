package com.example.merlin.data.database

import android.content.Context
import androidx.room.Room
// import androidx.sqlite.db.SupportSQLiteOpenHelper // Not directly needed for SupportFactory usage here
import com.example.merlin.data.security.DatabaseKeyProvider
import net.sqlcipher.database.SupportFactory
import com.example.merlin.database.MerlinDatabase
import com.example.merlin.data.BuildConfig

object DatabaseProvider {
    @Volatile
    private var INSTANCE: MerlinDatabase? = null

    fun getInstance(context: Context): MerlinDatabase {
        synchronized(this) {
            var instance = INSTANCE
            if (instance == null) {
                val appContext = context.applicationContext // Use application context to avoid leaks
                val passphrase = DatabaseKeyProvider.getOrCreatePassphrase(appContext)
                val factory = SupportFactory(passphrase)

                val databaseBuilder = Room.databaseBuilder(
                    appContext,
                    MerlinDatabase::class.java,
                    "merlin_encrypted.db" // Database file name
                )
                .openHelperFactory(factory)
                // Add migrations here when they are defined, e.g.:
                // .addMigrations(MerlinDatabase.MIGRATION_1_2, MerlinDatabase.MIGRATION_2_3)
                
                // Only allow destructive migration in debug and staging builds, not production
                if (BuildConfig.DEBUG || BuildConfig.BUILD_TYPE == "staging") {
                    databaseBuilder.fallbackToDestructiveMigration()
                }
                
                instance = databaseBuilder.build()
                INSTANCE = instance
                // Zero out the passphrase from memory after use
                passphrase.fill(0.toByte())
            }
            return instance
        }
    }

    // Optional: Method to close the database if ever needed explicitly (e.g., in tests or specific scenarios)
    // fun closeInstance() {
    //     INSTANCE?.close()
    //     INSTANCE = null
    // }
} 