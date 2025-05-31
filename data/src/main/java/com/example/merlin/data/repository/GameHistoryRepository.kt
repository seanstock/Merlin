package com.example.merlin.data.repository

import com.example.merlin.data.database.dao.GameHistoryDao
import com.example.merlin.data.database.entities.GameHistory

class GameHistoryRepository(private val gameHistoryDao: GameHistoryDao) {

    suspend fun insert(gameHistory: GameHistory): Long {
        return gameHistoryDao.insert(gameHistory)
    }

    suspend fun update(gameHistory: GameHistory) {
        gameHistoryDao.update(gameHistory)
    }

    suspend fun delete(gameHistory: GameHistory) {
        gameHistoryDao.delete(gameHistory)
    }

    fun getById(id: Long): GameHistory? {
        return gameHistoryDao.getById(id)
    }

    fun getForChild(childId: String): List<GameHistory> {
        return gameHistoryDao.getForChild(childId)
    }

    fun getAll(): List<GameHistory> {
        return gameHistoryDao.getAll()
    }
} 