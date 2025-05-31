package com.example.merlin.data.repository

import com.example.merlin.data.database.dao.ChildProfileDao
import com.example.merlin.data.database.entities.ChildProfile
// import kotlinx.coroutines.flow.Flow // Uncomment if/when using Flow for reactive data

class ChildProfileRepository(private val childProfileDao: ChildProfileDao) {

    suspend fun insert(childProfile: ChildProfile) {
        childProfileDao.insert(childProfile)
    }

    suspend fun update(childProfile: ChildProfile) {
        childProfileDao.update(childProfile)
    }

    suspend fun delete(childProfile: ChildProfile) {
        childProfileDao.delete(childProfile)
    }

    fun getById(id: String): ChildProfile? { // Or: Flow<ChildProfile?>
        return childProfileDao.getById(id)
    }

    fun getAll(): List<ChildProfile> { // Or: Flow<List<ChildProfile>>
        return childProfileDao.getAll()
    }
} 