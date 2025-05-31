package com.example.merlin.data.database.dao

import androidx.room.*
import com.example.merlin.data.database.entities.ParentSettings

@Dao
interface ParentSettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(parentSettings: ParentSettings)

    @Update
    suspend fun update(parentSettings: ParentSettings)

    @Query("SELECT * FROM parent_settings WHERE child_id = :childId")
    fun getByChildId(childId: String): ParentSettings?

    // Delete might be by child_id if needed, or accept the entity
    @Delete
    suspend fun delete(parentSettings: ParentSettings)
} 