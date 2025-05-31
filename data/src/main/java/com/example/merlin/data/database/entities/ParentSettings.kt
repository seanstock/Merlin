package com.example.merlin.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parent_settings")
data class ParentSettings(
    @PrimaryKey
    @ColumnInfo(name = "child_id")
    val childId: String,

    @ColumnInfo(name = "config_json")
    val configJson: String? // Includes pin_hash, salt, limits, subject_weights, etc.
) 