package com.example.habittracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.habittracker.data.entity.Birthday
import kotlinx.coroutines.flow.Flow

@Dao
interface BirthdayDao {
    @Query("SELECT * FROM birthdays ORDER BY month ASC, day ASC, name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Birthday>>

    @Query("SELECT * FROM birthdays ORDER BY month ASC, day ASC, name COLLATE NOCASE ASC")
    suspend fun getAll(): List<Birthday>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(birthday: Birthday): Long

    @Query(
        """
        UPDATE birthdays
        SET name = :name,
            year = :year,
            month = :month,
            day = :day,
            reminderDateTimesCsv = :reminderDateTimesCsv
        WHERE id = :birthdayId
        """
    )
    suspend fun update(
        birthdayId: Long,
        name: String,
        year: Int,
        month: Int,
        day: Int,
        reminderDateTimesCsv: String?
    )

    @Query("DELETE FROM birthdays WHERE id = :birthdayId")
    suspend fun deleteById(birthdayId: Long)
}
