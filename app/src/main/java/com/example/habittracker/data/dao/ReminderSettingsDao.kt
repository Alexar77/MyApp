package com.example.habittracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.habittracker.data.entity.ReminderSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderSettingsDao {
    @Query("SELECT * FROM reminder_settings WHERE id = 0 LIMIT 1")
    fun observe(): Flow<ReminderSettings?>

    @Query("SELECT * FROM reminder_settings WHERE id = 0 LIMIT 1")
    suspend fun get(): ReminderSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: ReminderSettings)
}
