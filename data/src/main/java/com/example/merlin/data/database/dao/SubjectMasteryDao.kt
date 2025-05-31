package com.example.merlin.data.database.dao

import androidx.room.*
import com.example.merlin.data.database.entities.SubjectMastery

@Dao
interface SubjectMasteryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subjectMastery: SubjectMastery)

    @Update // Or use insert with OnConflictStrategy.REPLACE if preferred for upserts
    suspend fun update(subjectMastery: SubjectMastery)

    @Query("SELECT * FROM subject_mastery WHERE child_id = :childId AND subject = :subject")
    fun getByChildAndSubject(childId: String, subject: String): SubjectMastery?

    @Query("SELECT * FROM subject_mastery WHERE child_id = :childId")
    fun getForChild(childId: String): List<SubjectMastery>

    @Query("DELETE FROM subject_mastery WHERE child_id = :childId AND subject = :subject")
    suspend fun delete(childId: String, subject: String)

    @Delete // For deleting by entity object if needed
    suspend fun delete(subjectMastery: SubjectMastery)
} 