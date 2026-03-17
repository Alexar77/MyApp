package com.example.habittracker.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.habittracker.data.entity.Habit
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY sortOrder ASC, createdAt DESC")
    fun observeHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits ORDER BY sortOrder ASC, createdAt DESC")
    suspend fun getAllHabits(): List<Habit>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM habits")
    suspend fun getMaxSortOrder(): Int

    @Query("UPDATE habits SET sortOrder = :sortOrder WHERE id = :habitId")
    suspend fun updateSortOrder(habitId: Long, sortOrder: Int)

    @Query("SELECT * FROM habits WHERE id = :habitId LIMIT 1")
    fun observeHabit(habitId: Long): Flow<Habit?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertHabit(habit: Habit): Long

    @Delete
    suspend fun deleteHabit(habit: Habit)

    @Query("DELETE FROM habits WHERE id = :habitId")
    suspend fun deleteHabitById(habitId: Long)

    @Query(
        """
        UPDATE habits
        SET name = :name,
            frequencyType = :frequencyType,
            frequencyIntervalDays = :frequencyIntervalDays,
            frequencyWeekdays = :frequencyWeekdays,
            reminderEnabled = :reminderEnabled,
            reminderTime = :reminderTime,
            reminderMessage = :reminderMessage
        WHERE id = :habitId
        """
    )
    suspend fun updateHabitDetails(
        habitId: Long,
        name: String,
        frequencyType: String,
        frequencyIntervalDays: Int?,
        frequencyWeekdays: String?,
        reminderEnabled: Boolean,
        reminderTime: String?,
        reminderMessage: String?
    )
}
