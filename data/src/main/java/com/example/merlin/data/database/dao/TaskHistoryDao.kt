package com.example.merlin.data.database.dao

import androidx.room.*
import com.example.merlin.data.database.entities.TaskHistory

@Dao
interface TaskHistoryDao {
    @Insert
    suspend fun insert(taskHistory: TaskHistory): Long // Returns the new rowId for the inserted item

    @Update
    suspend fun update(taskHistory: TaskHistory)

    @Delete
    suspend fun delete(taskHistory: TaskHistory)

    @Query("SELECT * FROM task_history WHERE id = :id")
    fun getById(id: Long): TaskHistory?

    @Query("SELECT * FROM task_history WHERE child_id = :childId ORDER BY ts DESC")
    fun getForChild(childId: String): List<TaskHistory>

    @Query("SELECT * FROM task_history ORDER BY ts DESC")
    fun getAll(): List<TaskHistory>
} 