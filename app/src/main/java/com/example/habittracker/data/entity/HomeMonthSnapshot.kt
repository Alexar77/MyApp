package com.example.habittracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "home_month_snapshots")
data class HomeMonthSnapshot(
    @PrimaryKey val monthKey: String,
    val selectedHabitId: Long?,
    val selectedCompletedDates: String,
    val selectedScheduledDates: String,
    val globalCompletedDates: String,
    val globalScheduledDates: String,
    val globalBirthdayDates: String,
    val globalNoteDates: String,
    val dayNotesByDate: String,
    val selectedHabitCreatedDate: String?,
    val businessToday: String,
    val updatedAt: Long
)
