package com.example.habittracker.di

import android.content.Context
import androidx.room.Room
import com.example.habittracker.data.dao.GoalDao
import com.example.habittracker.data.dao.BirthdayDao
import com.example.habittracker.data.dao.HabitCompletionDao
import com.example.habittracker.data.dao.HabitDao
import com.example.habittracker.data.dao.HabitDayNoteDao
import com.example.habittracker.data.dao.SubGoalDao
import com.example.habittracker.data.dao.TaskDao
import com.example.habittracker.data.dao.WhoAmINoteDao
import com.example.habittracker.data.database.AppDatabase
import com.example.habittracker.data.database.DatabaseMigrations
import com.example.habittracker.repository.HabitRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .addMigrations(*DatabaseMigrations.ALL)
            .build()
    }

    @Provides fun provideHabitDao(database: AppDatabase): HabitDao = database.habitDao()
    @Provides fun provideHabitCompletionDao(database: AppDatabase): HabitCompletionDao = database.habitCompletionDao()
    @Provides fun provideHabitDayNoteDao(database: AppDatabase): HabitDayNoteDao = database.habitDayNoteDao()
    @Provides fun provideWhoAmINoteDao(database: AppDatabase): WhoAmINoteDao = database.whoAmINoteDao()
    @Provides fun provideTaskDao(database: AppDatabase): TaskDao = database.taskDao()
    @Provides fun provideGoalDao(database: AppDatabase): GoalDao = database.goalDao()
    @Provides fun provideSubGoalDao(database: AppDatabase): SubGoalDao = database.subGoalDao()
    @Provides fun provideBirthdayDao(database: AppDatabase): BirthdayDao = database.birthdayDao()

    @Provides
    @Singleton
    fun provideHabitRepository(
        habitDao: HabitDao,
        completionDao: HabitCompletionDao,
        habitDayNoteDao: HabitDayNoteDao,
        whoAmINoteDao: WhoAmINoteDao,
        taskDao: TaskDao,
        goalDao: GoalDao,
        subGoalDao: SubGoalDao,
        birthdayDao: BirthdayDao
    ): HabitRepository {
        return HabitRepository(
            habitDao = habitDao,
            completionDao = completionDao,
            habitDayNoteDao = habitDayNoteDao,
            whoAmINoteDao = whoAmINoteDao,
            taskDao = taskDao,
            goalDao = goalDao,
            subGoalDao = subGoalDao,
            birthdayDao = birthdayDao
        )
    }
}
