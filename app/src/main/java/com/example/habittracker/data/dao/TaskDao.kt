package com.example.habittracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.habittracker.data.entity.TaskItem
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isDone ASC, sortOrder ASC, createdAt DESC")
    fun observeAll(): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks ORDER BY isDone ASC, sortOrder ASC, createdAt DESC")
    suspend fun getAll(): List<TaskItem>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM tasks WHERE isDone = :isDone")
    suspend fun getMaxSortOrderForDoneState(isDone: Boolean): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(taskItem: TaskItem)

    @Query("UPDATE tasks SET isDone = :isDone, completedAt = :completedAt WHERE id = :taskId")
    suspend fun updateDone(taskId: Long, isDone: Boolean, completedAt: Long?)

    @Query(
        """
        UPDATE tasks
        SET title = :title,
            category = :category,
            reminderEnabled = :reminderEnabled,
            reminderTime = :reminderTime,
            reminderDateTimesCsv = :reminderDateTimesCsv,
            reminderMessage = :reminderMessage
        WHERE id = :taskId
        """
    )
    suspend fun updateTaskDetails(
        taskId: Long,
        title: String,
        category: String,
        reminderEnabled: Boolean,
        reminderTime: String?,
        reminderDateTimesCsv: String?,
        reminderMessage: String?
    )

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteById(taskId: Long)

    @Query("UPDATE tasks SET sortOrder = :sortOrder WHERE id = :taskId")
    suspend fun updateSortOrder(taskId: Long, sortOrder: Int)

    @Query("UPDATE tasks SET category = :toCategory WHERE category = :fromCategory")
    suspend fun moveAllToCategory(fromCategory: String, toCategory: String)
}
