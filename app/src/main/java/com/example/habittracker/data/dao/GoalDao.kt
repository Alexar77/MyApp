package com.example.habittracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.habittracker.data.entity.Goal
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Goal>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(goal: Goal)

    @Query("UPDATE goals SET isDone = :isDone WHERE id = :goalId")
    suspend fun updateDone(goalId: Long, isDone: Boolean)

    @Query("DELETE FROM goals WHERE id = :goalId")
    suspend fun deleteById(goalId: Long)
}
