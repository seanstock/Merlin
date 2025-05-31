package com.example.merlin.data.repository

import com.example.merlin.data.database.dao.DailyUsageLogDao
import com.example.merlin.data.database.entities.DailyUsageLog

class DailyUsageLogRepository(private val dailyUsageLogDao: DailyUsageLogDao) {

    suspend fun insert(dailyUsageLog: DailyUsageLog) {
        dailyUsageLogDao.insert(dailyUsageLog)
    }

    suspend fun update(dailyUsageLog: DailyUsageLog) {
        dailyUsageLogDao.update(dailyUsageLog)
    }

    fun getByChildAndDate(childId: String, date: String): DailyUsageLog? {
        return dailyUsageLogDao.getByChildAndDate(childId, date)
    }

    fun getForChild(childId: String): List<DailyUsageLog> {
        return dailyUsageLogDao.getForChild(childId)
    }

    suspend fun delete(childId: String, date: String) {
        dailyUsageLogDao.delete(childId, date)
    }

    suspend fun delete(dailyUsageLog: DailyUsageLog) {
        dailyUsageLogDao.delete(dailyUsageLog)
    }
} 