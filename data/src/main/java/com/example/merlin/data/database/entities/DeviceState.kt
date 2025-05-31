package com.example.merlin.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "device_state")
data class DeviceState(
    @PrimaryKey
    val key: String,

    val value: String?
) 