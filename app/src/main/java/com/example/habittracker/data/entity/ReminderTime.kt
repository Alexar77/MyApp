package com.example.habittracker.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminder_times",
    indices = [Index(value = ["timeValue"], unique = true)]
)
data class ReminderTime(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timeValue: String
)
