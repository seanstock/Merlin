package com.example.merlin.data.database.dao

import androidx.room.*
import com.example.merlin.data.database.entities.XpTransaction
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for XpTransaction entity operations.
 */
@Dao
interface XpTransactionDao {
    
    @Query("SELECT * FROM xp_transactions WHERE childId = :childId ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getXpHistory(childId: String, limit: Int, offset: Int): List<XpTransaction>
    
    @Query("SELECT * FROM xp_transactions WHERE childId = :childId ORDER BY timestamp DESC")
    fun getXpHistoryFlow(childId: String): Flow<List<XpTransaction>>
    
    @Query("SELECT * FROM xp_transactions WHERE childId = :childId AND timestamp >= :since ORDER BY timestamp DESC")
    suspend fun getXpTransactionsSince(childId: String, since: Long): List<XpTransaction>
    
    @Query("SELECT * FROM xp_transactions WHERE childId = :childId AND source = :source ORDER BY timestamp DESC")
    suspend fun getXpTransactionsBySource(childId: String, source: String): List<XpTransaction>
    
    @Query("SELECT SUM(amount) FROM xp_transactions WHERE childId = :childId")
    suspend fun getTotalXpEarned(childId: String): Int?
    
    @Query("SELECT SUM(amount) FROM xp_transactions WHERE childId = :childId AND timestamp >= :since")
    suspend fun getXpEarnedSince(childId: String, since: Long): Int?
    
    @Query("SELECT SUM(amount) FROM xp_transactions WHERE childId = :childId AND source = :source")
    suspend fun getXpBySource(childId: String, source: String): Int?
    
    @Query("SELECT COUNT(*) FROM xp_transactions WHERE childId = :childId")
    suspend fun getTransactionCount(childId: String): Int
    
    @Query("SELECT * FROM xp_transactions WHERE childId = :childId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastXpTransaction(childId: String): XpTransaction?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertXpTransaction(transaction: XpTransaction)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertXpTransactions(transactions: List<XpTransaction>)
    
    @Update
    suspend fun updateXpTransaction(transaction: XpTransaction)
    
    @Delete
    suspend fun deleteXpTransaction(transaction: XpTransaction)
    
    @Query("DELETE FROM xp_transactions WHERE childId = :childId")
    suspend fun deleteAllXpTransactionsForChild(childId: String)
    
    @Query("DELETE FROM xp_transactions WHERE timestamp < :before")
    suspend fun deleteOldTransactions(before: Long)
} 