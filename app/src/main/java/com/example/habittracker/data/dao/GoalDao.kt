package com.example.habittracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.habittracker.data.entity.Goal
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY isDone ASC, sortOrder ASC, createdAt DESC")
    fun observeAll(): Flow<List<Goal>>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM goals WHERE isDone = :isDone")
    suspend fun getMaxSortOrderForDoneState(isDone: Boolean): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(goal: Goal): Long

    @Query("UPDATE goals SET isDone = :isDone, completedAt = :completedAt WHERE id = :goalId")
    suspend fun updateDone(goalId: Long, isDone: Boolean, completedAt: Long?)

    @Query("UPDATE goals SET title = :title WHERE id = :goalId")
    suspend fun updateTitle(goalId: Long, title: String)

    @Query("DELETE FROM goals WHERE id = :goalId")
    suspend fun deleteById(goalId: Long)

    @Query("UPDATE goals SET sortOrder = :sortOrder WHERE id = :goalId")
    suspend fun updateSortOrder(goalId: Long, sortOrder: Int)
}
