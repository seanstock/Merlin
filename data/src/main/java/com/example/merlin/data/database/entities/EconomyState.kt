package com.example.merlin.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "economy_state")
data class EconomyState(
    @PrimaryKey
    @ColumnInfo(name = "child_id")
    val childId: String,

    val streak: Int?,

    @ColumnInfo(name = "fatigue_score")
    val fatigueScore: Double?,

    @ColumnInfo(name = "wallet_seconds")
    val walletSeconds: Int?,

    @ColumnInfo(name = "badges_json")
    val badgesJson: String?,

    @ColumnInfo(name = "xp_level")
    val xpLevel: Int?,

    @ColumnInfo(name = "last_earned_ts")
    val lastEarnedTs: Long?
) 