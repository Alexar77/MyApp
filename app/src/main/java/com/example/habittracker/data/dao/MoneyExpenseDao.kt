package com.example.habittracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.habittracker.data.entity.MoneyExpense
import kotlinx.coroutines.flow.Flow

@Dao
interface MoneyExpenseDao {
    @Query("SELECT * FROM money_expenses ORDER BY paidAt DESC, id DESC")
    fun observeAll(): Flow<List<MoneyExpense>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(expense: MoneyExpense)

    @Query("UPDATE money_expenses SET category = :replacementCategory WHERE category = :categoryName")
    suspend fun replaceCategory(categoryName: String, replacementCategory: String)

    @Query("DELETE FROM money_expenses WHERE id = :expenseId")
    suspend fun deleteById(expenseId: Long)
}
