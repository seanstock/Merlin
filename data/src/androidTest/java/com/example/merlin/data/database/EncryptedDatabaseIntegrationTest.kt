package com.example.merlin.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.merlin.data.database.entities.DeviceState
import com.example.merlin.data.security.DatabaseKeyProvider
import com.example.merlin.database.MerlinDatabase
import net.sqlcipher.database.SupportFactory
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class EncryptedDatabaseIntegrationTest {

    private lateinit var database: MerlinDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        // Clear any previously stored key for a clean test environment
        DatabaseKeyProvider.clearStoredPassphrase(context)

        val passphrase = DatabaseKeyProvider.getOrCreatePassphrase(context)
        val factory = SupportFactory(passphrase)

        database = Room.databaseBuilder(
            context,
            MerlinDatabase::class.java,
            "test_merlin_encrypted.db" // Use a different name for test DB to avoid conflicts
        )
            .openHelperFactory(factory)
            // .allowMainThreadQueries() // Not recommended for integration tests unless simple & quick
            .fallbackToDestructiveMigration() // Good for tests
            .build()

        // Zero out passphrase after use
        passphrase.fill(0.toByte())
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
        // Optionally, delete the test database file
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.deleteDatabase("test_merlin_encrypted.db")
        // Clear the key provider cache again
        DatabaseKeyProvider.clearStoredPassphrase(context)
    }

    @Test
    @Throws(Exception::class)
    fun insertAndRetrieveDeviceState_encrypted() {
        val deviceStateDao = database.deviceStateDao()
        val testKey = "test_app_version"
        val testValue = "1.0.0-test"
        val deviceState = DeviceState(key = testKey, value = testValue)

        // Run DB operations on a background thread or use runBlocking for tests
        // For simplicity in this example, direct calls are made (ensure tests run on instrumentation thread)
        // Consider using runBlocking or a test dispatcher if DAO methods are suspend functions
        kotlinx.coroutines.runBlocking {
            deviceStateDao.insert(deviceState)
            val retrievedState = deviceStateDao.getByKey(testKey)

            assertNotNull("Retrieved state should not be null", retrievedState)
            assertEquals("Retrieved key should match", testKey, retrievedState?.key)
            assertEquals("Retrieved value should match", testValue, retrievedState?.value)
        }
    }
} 