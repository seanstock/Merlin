package com.example.merlin.data.database.dao

import androidx.room.*
import com.example.merlin.data.database.entities.EconomyState

@Dao
interface EconomyStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(economyState: EconomyState)

    @Update
    suspend fun update(economyState: EconomyState)

    @Query("SELECT * FROM economy_state WHERE child_id = :childId")
    fun getByChildId(childId: String): EconomyState?

    // Delete might be by child_id if needed
    @Query("DELETE FROM economy_state WHERE child_id = :childId")
    suspend fun deleteByChildId(childId: String)
} 