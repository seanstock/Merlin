package com.example.merlin.utils

import android.content.Context
import android.content.SharedPreferences

class UserSessionRepository(context: Context) {

    private val prefs: SharedPreferences = 
        context.getSharedPreferences(SharedPrefConstants.PREF_NAME, Context.MODE_PRIVATE)

    fun setActiveChildId(childId: String) {
        prefs.edit().putString(SharedPrefConstants.KEY_ACTIVE_CHILD_ID, childId).apply()
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
        prefs.edit().remove(SharedPrefConstants.KEY_ACTIVE_CHILD_ID).remove(SharedPrefConstants.KEY_CHILD_NAME).apply()
    }
} 