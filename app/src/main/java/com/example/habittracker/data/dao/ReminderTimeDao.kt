package com.example.habittracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.habittracker.data.entity.ReminderTime
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderTimeDao {
    @Query("SELECT * FROM reminder_times ORDER BY timeValue ASC")
    fun observeAll(): Flow<List<ReminderTime>>

    @Query("SELECT * FROM reminder_times ORDER BY timeValue ASC")
    suspend fun getAll(): List<ReminderTime>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(time: ReminderTime)

    @Query("DELETE FROM reminder_times WHERE id = :id")
    suspend fun deleteById(id: Long)
}
