package com.example.merlin.data.repository

import com.example.merlin.data.database.dao.TaskHistoryDao
import com.example.merlin.data.database.entities.TaskHistory

class TaskHistoryRepository(private val taskHistoryDao: TaskHistoryDao) {

    suspend fun insert(taskHistory: TaskHistory): Long {
        return taskHistoryDao.insert(taskHistory)
    }

    suspend fun update(taskHistory: TaskHistory) {
        taskHistoryDao.update(taskHistory)
    }

    suspend fun delete(taskHistory: TaskHistory) {
        taskHistoryDao.delete(taskHistory)
    }

    fun getById(id: Long): TaskHistory? {
        return taskHistoryDao.getById(id)
    }

    fun getForChild(childId: String): List<TaskHistory> {
        return taskHistoryDao.getForChild(childId)
    }

    fun getAll(): List<TaskHistory> {
        return taskHistoryDao.getAll()
    }
} 