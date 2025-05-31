package com.example.merlin.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_history")
data class GameHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "child_id")
    val childId: String?,

    @ColumnInfo(name = "game_id")
    val gameId: String?,

    val level: String?,

    val result: String?,

    @ColumnInfo(name = "time_ms")
    val timeMs: Long?,

    val ts: Long?
) 