package com.example.habittracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.habittracker.data.entity.MoneySettings
import kotlinx.coroutines.flow.Flow

@Dao
interface MoneySettingsDao {
    @Query("SELECT * FROM money_settings WHERE id = 0 LIMIT 1")
    fun observe(): Flow<MoneySettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: MoneySettings)
}
