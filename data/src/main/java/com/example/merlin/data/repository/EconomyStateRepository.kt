package com.example.merlin.data.repository

import com.example.merlin.data.database.dao.EconomyStateDao
import com.example.merlin.data.database.entities.EconomyState

class EconomyStateRepository(private val economyStateDao: EconomyStateDao) {

    suspend fun insert(economyState: EconomyState) {
        economyStateDao.insert(economyState)
    }

    suspend fun update(economyState: EconomyState) {
        economyStateDao.update(economyState)
    }

    fun getByChildId(childId: String): EconomyState? {
        return economyStateDao.getByChildId(childId)
    }

    suspend fun deleteByChildId(childId: String) {
        economyStateDao.deleteByChildId(childId)
    }
} 