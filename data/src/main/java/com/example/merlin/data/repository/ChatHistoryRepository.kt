package com.example.merlin.data.repository

import com.example.merlin.data.database.dao.ChatHistoryDao
import com.example.merlin.data.database.entities.ChatHistory

class ChatHistoryRepository(private val chatHistoryDao: ChatHistoryDao) {

    suspend fun insert(chatHistory: ChatHistory): Long {
        return chatHistoryDao.insert(chatHistory)
    }

    fun getRecentForChild(childId: String, limit: Int = 20): List<ChatHistory> {
        return chatHistoryDao.getRecentForChild(childId, limit)
    }

    fun getAllForChild(childId: String): List<ChatHistory> {
        return chatHistoryDao.getAllForChild(childId)
    }

    suspend fun deleteOlderThanForChild(childId: String, timestamp: Long) {
        chatHistoryDao.deleteOlderThanForChild(childId, timestamp)
    }

    suspend fun delete(chatHistory: ChatHistory) {
        chatHistoryDao.delete(chatHistory)
    }

    suspend fun clearForChild(childId: String) {
        chatHistoryDao.clearForChild(childId)
    }
} 