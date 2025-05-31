package com.example.merlin.data.database.dao

import androidx.room.*
import com.example.merlin.data.database.entities.DeviceState

@Dao
interface DeviceStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(deviceState: DeviceState)

    @Update
    suspend fun update(deviceState: DeviceState)

    @Query("SELECT * FROM device_state WHERE key = :key")
    fun getByKey(key: String): DeviceState?

    @Query("SELECT value FROM device_state WHERE key = :key")
    fun getValueByKey(key: String): String?

    @Query("DELETE FROM device_state WHERE key = :key")
    suspend fun deleteByKey(key: String)

    @Delete
    suspend fun delete(deviceState: DeviceState)

    @Query("SELECT * FROM device_state")
    fun getAll(): List<DeviceState>
} 