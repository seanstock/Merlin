package com.example.merlin.data.database.dao

import androidx.room.*
import com.example.merlin.data.database.entities.GameHistory

@Dao
interface GameHistoryDao {
    @Insert
    suspend fun insert(gameHistory: GameHistory): Long

    @Update
    suspend fun update(gameHistory: GameHistory)

    @Delete
    suspend fun delete(gameHistory: GameHistory)

    @Query("SELECT * FROM game_history WHERE id = :id")
    fun getById(id: Long): GameHistory?

    @Query("SELECT * FROM game_history WHERE child_id = :childId ORDER BY ts DESC")
    fun getForChild(childId: String): List<GameHistory>

    @Query("SELECT * FROM game_history ORDER BY ts DESC")
    fun getAll(): List<GameHistory>
} 