package com.example.habittracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val sortOrder: Int = 0,
    val frequencyType: String = "DAILY",
    val frequencyIntervalDays: Int? = null,
    val frequencyWeekdays: String? = null,
    val reminderEnabled: Boolean = false,
    val reminderTime: String? = null,
    val reminderMessage: String? = null
)
