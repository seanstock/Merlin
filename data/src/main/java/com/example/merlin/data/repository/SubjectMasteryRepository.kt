package com.example.merlin.data.repository

import com.example.merlin.data.database.dao.SubjectMasteryDao
import com.example.merlin.data.database.entities.SubjectMastery

class SubjectMasteryRepository(private val subjectMasteryDao: SubjectMasteryDao) {

    suspend fun insert(subjectMastery: SubjectMastery) {
        subjectMasteryDao.insert(subjectMastery)
    }

    suspend fun update(subjectMastery: SubjectMastery) {
        subjectMasteryDao.update(subjectMastery)
    }

    fun getByChildAndSubject(childId: String, subject: String): SubjectMastery? {
        return subjectMasteryDao.getByChildAndSubject(childId, subject)
    }

    fun getForChild(childId: String): List<SubjectMastery> {
        return subjectMasteryDao.getForChild(childId)
    }

    suspend fun delete(childId: String, subject: String) {
        subjectMasteryDao.delete(childId, subject)
    }

    suspend fun delete(subjectMastery: SubjectMastery) {
        subjectMasteryDao.delete(subjectMastery)
    }
} 