package com.example.merlin.config

import android.content.Context
import android.util.Log
import com.example.merlin.data.database.DatabaseProvider
import com.example.merlin.data.repository.ChildProfileRepository
import com.example.merlin.ui.UIVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Local implementation that chooses UI variant based solely on child's age.
 * If no child profile or age unknown, defaults to ADVANCED to keep full feature set.
 */
class LocalUIConfigurationService(private val context: Context) : UIConfigurationService {
    
    private val childProfileRepository = ChildProfileRepository(
        DatabaseProvider.getInstance(context).childProfileDao()
    )
    
    override suspend fun getUIVariant(childId: String): UIVariant {
        return try {
            withContext(Dispatchers.IO) {
                val childProfile = childProfileRepository.getById(childId)
                if (childProfile != null) {
                    when (childProfile.age) {
                        in 3..4 -> UIVariant.SIMPLE
                        else -> UIVariant.ADVANCED
                    }
                } else {
                    Log.e(TAG, "Child profile not found for ID: $childId")
                    UIVariant.ADVANCED // Default to ADVANCED if profile not found
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting UI variant for child $childId", e)
            UIVariant.ADVANCED // Default to ADVANCED on error
        }
    }

    companion object {
        private const val TAG = "LocalUIConfigService"
    }
} 