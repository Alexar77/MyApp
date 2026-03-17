package com.example.habittracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "money_expenses")
data class MoneyExpense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val isIncome: Boolean,
    val paidAt: Long,
    val createdAt: Long
)
