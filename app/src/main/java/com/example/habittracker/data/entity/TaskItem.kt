package com.example.habittracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String = "General",
    val isDone: Boolean,
    val completedAt: Long? = null,
    val sortOrder: Int = 0,
    val createdAt: Long,
    val reminderEnabled: Boolean = false,
    val reminderTime: String? = null,
    val reminderDateTimesCsv: String? = null,
    val reminderMessage: String? = null
)
