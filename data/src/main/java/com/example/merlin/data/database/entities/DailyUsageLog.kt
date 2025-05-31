package com.example.merlin.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "daily_usage_log",
    primaryKeys = ["child_id", "date"]
)
data class DailyUsageLog(
    @ColumnInfo(name = "child_id")
    val childId: String,

    val date: String, // Format YYYY-MM-DD

    @ColumnInfo(name = "seconds_used")
    val secondsUsed: Int?
) 