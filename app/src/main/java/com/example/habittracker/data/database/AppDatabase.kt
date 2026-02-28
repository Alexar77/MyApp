package com.example.habittracker.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.habittracker.data.dao.GoalDao
import com.example.habittracker.data.dao.BirthdayDao
import com.example.habittracker.data.dao.HabitCompletionDao
import com.example.habittracker.data.dao.HabitDao
import com.example.habittracker.data.dao.HabitDayNoteDao
import com.example.habittracker.data.dao.ReminderSettingsDao
import com.example.habittracker.data.dao.ReminderTimeDao
import com.example.habittracker.data.dao.SubGoalDao
import com.example.habittracker.data.dao.TaskCategoryDao
import com.example.habittracker.data.dao.TaskDao
import com.example.habittracker.data.dao.WhoAmINoteDao
import com.example.habittracker.data.entity.Goal
import com.example.habittracker.data.entity.Birthday
import com.example.habittracker.data.entity.Habit
import com.example.habittracker.data.entity.HabitCompletion
import com.example.habittracker.data.entity.HabitDayNote
import com.example.habittracker.data.entity.ReminderSettings
import com.example.habittracker.data.entity.ReminderTime
import com.example.habittracker.data.entity.SubGoal
import com.example.habittracker.data.entity.TaskCategory
import com.example.habittracker.data.entity.TaskItem
import com.example.habittracker.data.entity.WhoAmINote

@Database(
    entities = [
        Habit::class,
        HabitCompletion::class,
        HabitDayNote::class,
        WhoAmINote::class,
        TaskItem::class,
        TaskCategory::class,
        Goal::class,
        SubGoal::class,
        ReminderSettings::class,
        ReminderTime::class,
        Birthday::class
    ],
    version = 16,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitCompletionDao(): HabitCompletionDao
    abstract fun habitDayNoteDao(): HabitDayNoteDao
    abstract fun whoAmINoteDao(): WhoAmINoteDao
    abstract fun taskDao(): TaskDao
    abstract fun taskCategoryDao(): TaskCategoryDao
    abstract fun goalDao(): GoalDao
    abstract fun subGoalDao(): SubGoalDao
    abstract fun reminderSettingsDao(): ReminderSettingsDao
    abstract fun reminderTimeDao(): ReminderTimeDao
    abstract fun birthdayDao(): BirthdayDao

    companion object {
        const val NAME = "habit_tracker.db"
    }
}
