package com.example.merlin.utils

import android.content.Context
import android.util.Log
import com.example.merlin.data.database.DatabaseProvider
import com.example.merlin.data.repository.ParentSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Service for authenticating parent PIN against stored hashed values.
 * Works with the existing parent settings infrastructure from onboarding.
 */
class PinAuthenticationService(private val context: Context) {
    
    companion object {
        private const val TAG = "PinAuthenticationService"
    }
    
    private val userSessionRepository by lazy { UserSessionRepository(context) }
    private val parentSettingsRepository by lazy {
        val database = DatabaseProvider.getInstance(context)
        ParentSettingsRepository(database.parentSettingsDao())
    }
    
    /**
     * Verify a PIN against the stored parent PIN for the active child.
     * 
     * @param enteredPin The PIN entered by the user
     * @return true if the PIN is correct, false otherwise
     */
    suspend fun verifyPin(enteredPin: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Get the active child ID
            val activeChildId = userSessionRepository.getActiveChildId()
            if (activeChildId == null) {
                Log.w(TAG, "No active child ID found")
                return@withContext false
            }
            
            // Get parent settings for this child
            val parentSettings = parentSettingsRepository.getByChildId(activeChildId)
            if (parentSettings == null) {
                Log.w(TAG, "No parent settings found for child ID: $activeChildId")
                return@withContext false
            }
            
            // Parse the config JSON to get PIN hash and salt
            val configJson = parentSettings.configJson
            if (configJson == null) {
                Log.w(TAG, "No config JSON found in parent settings")
                return@withContext false
            }
            
            val jsonObject = JSONObject(configJson)
            val storedPinHash = jsonObject.optString("pin_hash", "")
            val storedSalt = jsonObject.optString("salt", "")
            
            if (storedPinHash.isEmpty() || storedSalt.isEmpty()) {
                Log.w(TAG, "PIN hash or salt not found in config")
                return@withContext false
            }
            
            // Hash the entered PIN with the stored salt
            val enteredPinHash = hashPinWithSalt(enteredPin, storedSalt)
            
            // Compare hashes
            val isValid = enteredPinHash == storedPinHash
            
            Log.d(TAG, "PIN verification result: $isValid")
            return@withContext isValid
            
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying PIN", e)
            return@withContext false
        }
    }
    
    /**
     * Hash a PIN with the provided salt using the same method as onboarding.
     * This matches the hashPin method in OnboardingViewModel.
     * 
     * @param pin The PIN to hash
     * @param saltString The salt as a hex string
     * @return The hashed PIN as a hex string
     */
    private fun hashPinWithSalt(pin: String, saltString: String): String {
        try {
            // Convert hex salt string back to byte array
            val salt = saltString.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            
            // Hash the PIN with the salt using SHA-256
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(salt)
            val hashedPin = digest.digest(pin.toByteArray())
            
            // Convert back to hex string
            return hashedPin.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error hashing PIN with salt", e)
            throw e
        }
    }
    
    /**
     * Check if a parent PIN has been set for the active child.
     * 
     * @return true if a PIN exists, false otherwise
     */
    suspend fun isPinSet(): Boolean = withContext(Dispatchers.IO) {
        try {
            val activeChildId = userSessionRepository.getActiveChildId()
            if (activeChildId == null) {
                return@withContext false
            }
            
            val parentSettings = parentSettingsRepository.getByChildId(activeChildId)
            if (parentSettings?.configJson == null) {
                return@withContext false
            }
            
            val jsonObject = JSONObject(parentSettings.configJson)
            val pinHash = jsonObject.optString("pin_hash", "")
            val salt = jsonObject.optString("salt", "")
            
            return@withContext pinHash.isNotEmpty() && salt.isNotEmpty()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if PIN is set", e)
            return@withContext false
        }
    }
    
    /**
     * Get the active child's name for display purposes.
     * 
     * @return The child's name or null if not found
     */
    suspend fun getActiveChildName(): String? = withContext(Dispatchers.IO) {
        return@withContext userSessionRepository.getChildName()
    }
} 