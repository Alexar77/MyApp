package com.example.habittracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.habittracker.data.entity.WhoAmINote
import kotlinx.coroutines.flow.Flow

@Dao
interface WhoAmINoteDao {
    @Query("SELECT * FROM who_am_i_notes ORDER BY sortOrder ASC, createdAt DESC")
    fun observeAll(): Flow<List<WhoAmINote>>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM who_am_i_notes")
    suspend fun getMaxSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(note: WhoAmINote): Long

    @Query("UPDATE who_am_i_notes SET content = :content WHERE id = :noteId")
    suspend fun updateContent(noteId: Long, content: String)

    @Query("UPDATE who_am_i_notes SET title = :title WHERE id = :noteId")
    suspend fun updateTitle(noteId: Long, title: String)

    @Query("DELETE FROM who_am_i_notes WHERE id = :noteId")
    suspend fun deleteById(noteId: Long)

    @Query("UPDATE who_am_i_notes SET sortOrder = :sortOrder WHERE id = :noteId")
    suspend fun updateSortOrder(noteId: Long, sortOrder: Int)
}
