package com.example.merlin.data.repository

import com.example.merlin.data.database.dao.DeviceStateDao
import com.example.merlin.data.database.entities.DeviceState

class DeviceStateRepository(private val deviceStateDao: DeviceStateDao) {

    suspend fun insert(deviceState: DeviceState) {
        deviceStateDao.insert(deviceState)
    }

    suspend fun update(deviceState: DeviceState) {
        deviceStateDao.update(deviceState)
    }

    fun getByKey(key: String): DeviceState? {
        return deviceStateDao.getByKey(key)
    }

    fun getValueByKey(key: String): String? {
        return deviceStateDao.getValueByKey(key)
    }

    suspend fun deleteByKey(key: String) {
        deviceStateDao.deleteByKey(key)
    }

    suspend fun delete(deviceState: DeviceState) {
        deviceStateDao.delete(deviceState)
    }

    fun getAll(): List<DeviceState> {
        return deviceStateDao.getAll()
    }
} 