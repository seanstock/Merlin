package com.example.merlin.data.database.dao

import androidx.room.*
import com.example.merlin.data.database.entities.Experience
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Experience entity operations.
 */
@Dao
interface ExperienceDao {
    
    @Query("SELECT * FROM experiences WHERE childId = :childId")
    suspend fun getExperienceForChild(childId: String): Experience?
    
    @Query("SELECT * FROM experiences WHERE childId = :childId")
    fun getExperienceForChildFlow(childId: String): Flow<Experience?>
    
    @Query("SELECT level FROM experiences WHERE childId = :childId")
    suspend fun getCurrentLevel(childId: String): Int?
    
    @Query("SELECT totalXpEarned FROM experiences WHERE childId = :childId")
    suspend fun getTotalXp(childId: String): Int?
    
    @Query("SELECT * FROM experiences ORDER BY totalXpEarned DESC LIMIT :limit")
    suspend fun getTopExperienceLeaders(limit: Int): List<Experience>
    
    @Query("SELECT * FROM experiences ORDER BY level DESC, totalXpEarned DESC LIMIT :limit")
    suspend fun getTopLevelLeaders(limit: Int): List<Experience>
    
    @Query("SELECT COUNT(*) FROM experiences WHERE totalXpEarned > (SELECT totalXpEarned FROM experiences WHERE childId = :childId)")
    suspend fun getChildRank(childId: String): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExperience(experience: Experience)
    
    @Update
    suspend fun updateExperience(experience: Experience)
    
    @Delete
    suspend fun deleteExperience(experience: Experience)
    
    @Query("DELETE FROM experiences WHERE childId = :childId")
    suspend fun deleteExperienceForChild(childId: String)
    
    @Query("SELECT EXISTS(SELECT 1 FROM experiences WHERE childId = :childId)")
    suspend fun hasExperience(childId: String): Boolean
} 