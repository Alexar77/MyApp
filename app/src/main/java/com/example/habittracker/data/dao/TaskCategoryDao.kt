package com.example.habittracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.habittracker.data.entity.TaskCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskCategoryDao {
    @Query("SELECT * FROM task_categories ORDER BY sortOrder ASC, name ASC")
    fun observeAll(): Flow<List<TaskCategory>>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM task_categories")
    suspend fun getMaxSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: TaskCategory)

    @Query("DELETE FROM task_categories WHERE name = :name")
    suspend fun deleteByName(name: String)

    @Query("UPDATE task_categories SET sortOrder = :sortOrder WHERE name = :name")
    suspend fun updateSortOrder(name: String, sortOrder: Int)
}
