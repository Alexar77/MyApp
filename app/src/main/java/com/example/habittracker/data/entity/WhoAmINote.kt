package com.example.habittracker.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "who_am_i_notes",
    indices = [Index(value = ["createdAt"])]
)
data class WhoAmINote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val sortOrder: Int = 0,
    val createdAt: Long
)
