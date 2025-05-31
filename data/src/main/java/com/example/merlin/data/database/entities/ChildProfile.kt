package com.example.merlin.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "child_profile")
data class ChildProfile(
    @PrimaryKey
    val id: String,

    val name: String?,

    val birthdate: Long?, // Unix ms timestamp

    val age: Int?, // Cached, auto-update daily (logic outside entity)

    val gender: String?,

    @ColumnInfo(name = "preferred_language")
    val preferredLanguage: String?,

    val location: String?
) 