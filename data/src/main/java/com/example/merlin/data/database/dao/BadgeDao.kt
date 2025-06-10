package com.example.merlin.data.database.dao

import androidx.room.*
import com.example.merlin.data.database.entities.Badge
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Badge entity operations.
 */
@Dao
interface BadgeDao {
    
    @Query("SELECT * FROM badges WHERE childId = :childId ORDER BY earnedAt DESC")
    suspend fun getBadgesForChild(childId: String): List<Badge>
    
    @Query("SELECT * FROM badges WHERE childId = :childId ORDER BY earnedAt DESC")
    fun getBadgesForChildFlow(childId: String): Flow<List<Badge>>
    
    @Query("SELECT * FROM badges WHERE childId = :childId AND category = :category ORDER BY earnedAt DESC")
    suspend fun getBadgesByCategory(childId: String, category: String): List<Badge>
    
    @Query("SELECT * FROM badges WHERE id = :badgeId")
    suspend fun getBadgeById(badgeId: String): Badge?
    
    @Query("SELECT EXISTS(SELECT 1 FROM badges WHERE childId = :childId AND id = :badgeId)")
    suspend fun hasBadge(childId: String, badgeId: String): Boolean
    
    @Query("SELECT COUNT(*) FROM badges WHERE childId = :childId")
    suspend fun getBadgeCount(childId: String): Int
    
    @Query("SELECT COUNT(*) FROM badges WHERE childId = :childId AND category = :category")
    suspend fun getBadgeCountByCategory(childId: String, category: String): Int
    
    @Query("SELECT COUNT(*) FROM badges WHERE childId = :childId AND rarity = :rarity")
    suspend fun getBadgeCountByRarity(childId: String, rarity: String): Int
    
    @Query("SELECT * FROM badges WHERE childId = :childId ORDER BY earnedAt DESC LIMIT 1")
    suspend fun getMostRecentBadge(childId: String): Badge?
    
    @Query("SELECT * FROM badges WHERE childId = :childId AND earnedAt >= :since ORDER BY earnedAt DESC")
    suspend fun getBadgesSince(childId: String, since: Long): List<Badge>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBadge(badge: Badge)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBadges(badges: List<Badge>)
    
    @Update
    suspend fun updateBadge(badge: Badge)
    
    @Delete
    suspend fun deleteBadge(badge: Badge)
    
    @Query("DELETE FROM badges WHERE childId = :childId")
    suspend fun deleteAllBadgesForChild(childId: String)
} 