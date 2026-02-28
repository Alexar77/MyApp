package com.example.habittracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_categories")
data class TaskCategory(
    @PrimaryKey val name: String,
    val sortOrder: Int = 0
)
