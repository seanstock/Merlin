package com.example.merlin.data.repository

import com.example.merlin.data.database.dao.ChildProfileDao
import com.example.merlin.data.database.entities.ChildProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ChildProfileRepository(private val childProfileDao: ChildProfileDao) {

    suspend fun insert(childProfile: ChildProfile) {
        withContext(Dispatchers.IO) {
            childProfileDao.insert(childProfile)
        }
    }

    suspend fun update(childProfile: ChildProfile) {
        withContext(Dispatchers.IO) {
            childProfileDao.update(childProfile)
        }
    }

    suspend fun delete(childProfile: ChildProfile) {
        childProfileDao.delete(childProfile)
    }

    suspend fun getById(id: String): ChildProfile? {
        return withContext(Dispatchers.IO) {
            childProfileDao.getById(id)
        }
    }

    fun getByIdFlow(id: String): Flow<ChildProfile?> {
        return childProfileDao.getByIdFlow(id)
    }

    suspend fun getAll(): List<ChildProfile> {
        return withContext(Dispatchers.IO) {
            childProfileDao.getAll()
        }
    }

    fun getAllFlow(): Flow<List<ChildProfile>> {
        return childProfileDao.getAllFlow()
    }
} 