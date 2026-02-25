package com.example.habittracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.habittracker.data.entity.HabitDayNote
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDayNoteDao {
    @Query("SELECT * FROM habit_day_notes WHERE habitId = :habitId")
    fun observeNotesForHabit(habitId: Long): Flow<List<HabitDayNote>>

    @Query("SELECT * FROM habit_day_notes")
    fun observeAllNotes(): Flow<List<HabitDayNote>>

    @Query("SELECT * FROM habit_day_notes WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun getNoteForDay(habitId: Long, date: String): HabitDayNote?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: HabitDayNote)

    @Query("DELETE FROM habit_day_notes WHERE id = :noteId")
    suspend fun deleteById(noteId: Long)
}
