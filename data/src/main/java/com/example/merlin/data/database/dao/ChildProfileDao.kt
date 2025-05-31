package com.example.merlin.data.database.dao

import androidx.room.*
import com.example.merlin.data.database.entities.ChildProfile

@Dao
interface ChildProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(childProfile: ChildProfile)

    @Update
    suspend fun update(childProfile: ChildProfile)

    @Delete
    suspend fun delete(childProfile: ChildProfile)

    @Query("SELECT * FROM child_profile WHERE id = :id")
    fun getById(id: String): ChildProfile?

    @Query("SELECT * FROM child_profile")
    fun getAll(): List<ChildProfile>

    // Consider adding Flow variants for observable queries later if needed
    // @Query("SELECT * FROM child_profile")
    // fun getAllFlow(): Flow<List<ChildProfile>>
} 