package com.example.habittracker.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "birthdays",
    indices = [
        Index(value = ["month", "day"]),
        Index(value = ["name"])
    ]
)
data class Birthday(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val year: Int,
    val month: Int,
    val day: Int,
    val reminderDateTimesCsv: String? = null,
    val createdAt: Long
)
