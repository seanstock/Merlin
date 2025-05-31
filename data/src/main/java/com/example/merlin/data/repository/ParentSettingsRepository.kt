package com.example.merlin.data.repository

import com.example.merlin.data.database.dao.ParentSettingsDao
import com.example.merlin.data.database.entities.ParentSettings

class ParentSettingsRepository(private val parentSettingsDao: ParentSettingsDao) {

    suspend fun insert(parentSettings: ParentSettings) {
        parentSettingsDao.insert(parentSettings)
    }

    suspend fun update(parentSettings: ParentSettings) {
        parentSettingsDao.update(parentSettings)
    }

    fun getByChildId(childId: String): ParentSettings? {
        return parentSettingsDao.getByChildId(childId)
    }

    suspend fun delete(parentSettings: ParentSettings) {
        parentSettingsDao.delete(parentSettings)
    }
} 