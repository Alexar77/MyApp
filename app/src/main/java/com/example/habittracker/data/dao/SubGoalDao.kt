package com.example.habittracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.habittracker.data.entity.SubGoal
import kotlinx.coroutines.flow.Flow

@Dao
interface SubGoalDao {
    @Query("SELECT * FROM sub_goals ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<SubGoal>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(subGoal: SubGoal)

    @Query("UPDATE sub_goals SET isDone = :isDone WHERE id = :subGoalId")
    suspend fun updateDone(subGoalId: Long, isDone: Boolean)

    @Query("DELETE FROM sub_goals WHERE id = :subGoalId")
    suspend fun deleteById(subGoalId: Long)
}
