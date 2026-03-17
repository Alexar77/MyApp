package com.example.habittracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.habittracker.data.entity.HomeMonthSnapshot
import kotlinx.coroutines.flow.Flow

@Dao
interface HomeMonthSnapshotDao {
    @Query("SELECT * FROM home_month_snapshots WHERE monthKey = :monthKey LIMIT 1")
    fun observeSnapshot(monthKey: String): Flow<HomeMonthSnapshot?>

    @Query("SELECT * FROM home_month_snapshots WHERE monthKey = :monthKey LIMIT 1")
    suspend fun getSnapshot(monthKey: String): HomeMonthSnapshot?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(snapshot: HomeMonthSnapshot)
}
