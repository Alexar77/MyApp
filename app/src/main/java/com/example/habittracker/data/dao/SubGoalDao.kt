package com.example.habittracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.habittracker.data.entity.SubGoal
import kotlinx.coroutines.flow.Flow

@Dao
interface SubGoalDao {
    @Query("SELECT * FROM sub_goals ORDER BY goalId ASC, sortOrder ASC, createdAt ASC")
    fun observeAll(): Flow<List<SubGoal>>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM sub_goals WHERE goalId = :goalId")
    suspend fun getMaxSortOrderForGoal(goalId: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(subGoal: SubGoal)

    @Query("UPDATE sub_goals SET isDone = :isDone, completedAt = :completedAt WHERE id = :subGoalId")
    suspend fun updateDone(subGoalId: Long, isDone: Boolean, completedAt: Long?)

    @Query("UPDATE sub_goals SET title = :title WHERE id = :subGoalId")
    suspend fun updateTitle(subGoalId: Long, title: String)

    @Query("UPDATE sub_goals SET sortOrder = :sortOrder WHERE id = :subGoalId")
    suspend fun updateSortOrder(subGoalId: Long, sortOrder: Int)

    @Query("DELETE FROM sub_goals WHERE id = :subGoalId")
    suspend fun deleteById(subGoalId: Long)
}
