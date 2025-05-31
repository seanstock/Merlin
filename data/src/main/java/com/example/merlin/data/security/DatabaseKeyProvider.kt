package com.example.merlin.data.security

import android.content.Context
import androidx.security.crypto.MasterKey // Changed from android.security.keystore.MasterKey
import androidx.security.crypto.EncryptedSharedPreferences
import java.security.SecureRandom
import android.util.Base64
import android.util.Log

object DatabaseKeyProvider {

    private const val ENCRYPTED_PREFS_FILE_NAME = "merlin_secure_database_prefs"
    private const val ENCRYPTED_PASSPHRASE_KEY = "db_passphrase_v1" // Added versioning to key name

    // MasterKey alias is often handled by the library, but can be specified if needed.
    // Default is _androidx_security_master_key_
    // private const val KEYSTORE_MASTER_KEY_ALIAS = "_androidx_security_master_key_"

    @Volatile
    private var cachedPassphrase: ByteArray? = null

    fun getOrCreatePassphrase(context: Context): ByteArray {
        cachedPassphrase?.let { return it }

        synchronized(this) {
            cachedPassphrase?.let { return it }

            val appContext = context.applicationContext

            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val sharedPreferences = EncryptedSharedPreferences.create(
                appContext,
                ENCRYPTED_PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            val existingPassphraseBase64 = sharedPreferences.getString(ENCRYPTED_PASSPHRASE_KEY, null)
            if (existingPassphraseBase64 != null) {
                Log.d("DatabaseKeyProvider", "Found existing passphrase from EncryptedSharedPreferences.")
                val decodedPassphrase = Base64.decode(existingPassphraseBase64, Base64.DEFAULT)
                cachedPassphrase = decodedPassphrase
                return decodedPassphrase
            }

            Log.d("DatabaseKeyProvider", "No existing passphrase found. Generating new one.")
            val newPassphraseBytes = ByteArray(32) // 32 bytes = 256 bits
            SecureRandom().nextBytes(newPassphraseBytes)
            val newPassphraseBase64 = Base64.encodeToString(newPassphraseBytes, Base64.NO_WRAP) // Use NO_WRAP for consistency

            sharedPreferences.edit()
                .putString(ENCRYPTED_PASSPHRASE_KEY, newPassphraseBase64)
                .apply()
            Log.d("DatabaseKeyProvider", "New passphrase generated and stored in EncryptedSharedPreferences.")
            cachedPassphrase = newPassphraseBytes
            return newPassphraseBytes
        }
    }

    // Call this if you suspect keys might have been compromised or need to reset for testing.
    // Use with extreme caution in production.
    fun clearStoredPassphrase(context: Context) {
        synchronized(this) {
            val appContext = context.applicationContext
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val sharedPreferences = EncryptedSharedPreferences.create(
                appContext,
                ENCRYPTED_PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            sharedPreferences.edit().remove(ENCRYPTED_PASSPHRASE_KEY).apply()
            cachedPassphrase = null
            Log.d("DatabaseKeyProvider", "Stored passphrase cleared from EncryptedSharedPreferences.")
        }
    }
} 