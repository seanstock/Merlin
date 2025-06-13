package com.example.merlin.utils

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserSessionRepository private constructor(context: Context) {
    private val prefs: SharedPreferences = 
        context.getSharedPreferences(SharedPrefConstants.PREF_NAME, Context.MODE_PRIVATE)

    private val _activeChildIdFlow = MutableStateFlow<String?>(getActiveChildId())
    val activeChildIdFlow: StateFlow<String?> = _activeChildIdFlow.asStateFlow()

    fun setActiveChildId(childId: String?) {
        prefs.edit().putString(SharedPrefConstants.KEY_ACTIVE_CHILD_ID, childId).apply()
        _activeChildIdFlow.value = childId
    }

    fun getActiveChildId(): String? {
        return prefs.getString(SharedPrefConstants.KEY_ACTIVE_CHILD_ID, null)
    }

    fun saveChildName(name: String) {
        prefs.edit().putString(SharedPrefConstants.KEY_CHILD_NAME, name).apply()
    }

    fun getChildName(): String? {
        return prefs.getString(SharedPrefConstants.KEY_CHILD_NAME, null)
    }

    fun clearActiveUserSession() {
        prefs.edit()
            .remove(SharedPrefConstants.KEY_ACTIVE_CHILD_ID)
            .remove(SharedPrefConstants.KEY_CHILD_NAME)
            .remove(SharedPrefConstants.KEY_ONBOARDING_COMPLETED)
            .apply()
        _activeChildIdFlow.value = null
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(SharedPrefConstants.KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(SharedPrefConstants.KEY_ONBOARDING_COMPLETED, false)
    }

    companion object {
        @Volatile
        private var INSTANCE: UserSessionRepository? = null

        fun getInstance(context: Context): UserSessionRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserSessionRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
} 