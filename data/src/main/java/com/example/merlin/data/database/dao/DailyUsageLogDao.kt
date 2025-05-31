package com.example.merlin.data.database.dao

import androidx.room.*
import com.example.merlin.data.database.entities.DailyUsageLog

@Dao
interface DailyUsageLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dailyUsageLog: DailyUsageLog)

    @Update // Or use insert with OnConflictStrategy.REPLACE
    suspend fun update(dailyUsageLog: DailyUsageLog)

    @Query("SELECT * FROM daily_usage_log WHERE child_id = :childId AND date = :date")
    fun getByChildAndDate(childId: String, date: String): DailyUsageLog?

    @Query("SELECT * FROM daily_usage_log WHERE child_id = :childId ORDER BY date DESC")
    fun getForChild(childId: String): List<DailyUsageLog>

    @Query("DELETE FROM daily_usage_log WHERE child_id = :childId AND date = :date")
    suspend fun delete(childId: String, date: String)

    @Delete // For deleting by entity object if needed
    suspend fun delete(dailyUsageLog: DailyUsageLog)
} 