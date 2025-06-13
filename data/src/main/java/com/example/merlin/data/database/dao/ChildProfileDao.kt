package com.example.merlin.data.database.dao

import androidx.room.*
import com.example.merlin.data.database.entities.ChildProfile
import kotlinx.coroutines.flow.Flow

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

    @Query("SELECT * FROM child_profile WHERE id = :id")
    fun getByIdFlow(id: String): Flow<ChildProfile?>

    @Query("SELECT * FROM child_profile")
    fun getAll(): List<ChildProfile>

    @Query("SELECT * FROM child_profile")
    fun getAllFlow(): Flow<List<ChildProfile>>
} 