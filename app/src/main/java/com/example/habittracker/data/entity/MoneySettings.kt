package com.example.habittracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "money_settings")
data class MoneySettings(
    @PrimaryKey val id: Int = 0,
    val budgetAmount: Double,
    val hourlyWage: Double
)
