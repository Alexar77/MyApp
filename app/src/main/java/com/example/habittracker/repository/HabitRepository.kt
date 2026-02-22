package com.example.habittracker.repository

import com.example.habittracker.data.dao.GoalDao
import com.example.habittracker.data.dao.HabitCompletionDao
import com.example.habittracker.data.dao.HabitDao
import com.example.habittracker.data.dao.HabitDayNoteDao
import com.example.habittracker.data.dao.ReminderSettingsDao
import com.example.habittracker.data.dao.ReminderTimeDao
import com.example.habittracker.data.dao.SubGoalDao
import com.example.habittracker.data.dao.TaskDao
import com.example.habittracker.data.dao.WhoAmINoteDao
import com.example.habittracker.data.entity.Goal
import com.example.habittracker.data.entity.Habit
import com.example.habittracker.data.entity.HabitCompletion
import com.example.habittracker.data.entity.HabitDayNote
import com.example.habittracker.data.entity.ReminderSettings
import com.example.habittracker.data.entity.ReminderTime
import com.example.habittracker.data.entity.SubGoal
import com.example.habittracker.data.entity.TaskItem
import com.example.habittracker.data.entity.WhoAmINote
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitRepository @Inject constructor(
    private val habitDao: HabitDao,
    private val completionDao: HabitCompletionDao,
    private val habitDayNoteDao: HabitDayNoteDao,
    private val whoAmINoteDao: WhoAmINoteDao,
    private val taskDao: TaskDao,
    private val goalDao: GoalDao,
    private val subGoalDao: SubGoalDao,
    private val reminderSettingsDao: ReminderSettingsDao,
    private val reminderTimeDao: ReminderTimeDao
) {
    data class HabitOption(val id: Long, val name: String)
    data class HabitStats(val currentStreak: Int, val longestStreak: Int, val totalCompletions: Int)
    data class GoalWithSubGoals(val goalId: Long, val title: String, val isDone: Boolean, val subGoals: List<SubGoal>)
    data class ReminderTimeEntry(val id: Long, val timeValue: String)
    data class ReminderConfig(val habitsEnabled: Boolean, val tasksEnabled: Boolean, val times: List<ReminderTimeEntry>)

    fun observeHabits(): Flow<List<HabitOption>> = habitDao.observeHabits().map { habits ->
        habits.map { HabitOption(id = it.id, name = it.name) }
    }

    fun observeCompletedDateStrings(habitId: Long): Flow<Set<String>> =
        completionDao.observeCompletionsForHabit(habitId).map { completions ->
            completions.asSequence().filter { it.completed }.map { it.date }.toSet()
        }

    fun observeHabitDayNotes(habitId: Long): Flow<Map<String, String>> =
        habitDayNoteDao.observeNotesForHabit(habitId).map { notes ->
            notes.associate { it.date to it.note }
        }

    fun observeWhoAmINotes(): Flow<List<WhoAmINote>> = whoAmINoteDao.observeAll()
    fun observeTasks(): Flow<List<TaskItem>> = taskDao.observeAll()

    fun observeGoalsWithSubGoals(): Flow<List<GoalWithSubGoals>> =
        combine(goalDao.observeAll(), subGoalDao.observeAll()) { goals, subGoals ->
            goals.map { goal ->
                GoalWithSubGoals(
                    goalId = goal.id,
                    title = goal.title,
                    isDone = goal.isDone,
                    subGoals = subGoals.filter { it.goalId == goal.id }
                )
            }
        }

    fun observeReminderConfig(): Flow<ReminderConfig> {
        return combine(reminderSettingsDao.observe(), reminderTimeDao.observeAll()) { settings, times ->
            ReminderConfig(
                habitsEnabled = settings?.habitsEnabled ?: true,
                tasksEnabled = settings?.tasksEnabled ?: true,
                times = times.map { ReminderTimeEntry(id = it.id, timeValue = it.timeValue) }
            )
        }
    }

    suspend fun getReminderConfig(): ReminderConfig {
        val settings = reminderSettingsDao.get() ?: ReminderSettings(id = 0, habitsEnabled = true, tasksEnabled = true)
        val times = reminderTimeDao.getAll().map { ReminderTimeEntry(id = it.id, timeValue = it.timeValue) }
        return ReminderConfig(settings.habitsEnabled, settings.tasksEnabled, times)
    }

    fun observeHabit(habitId: Long): Flow<Habit?> = habitDao.observeHabit(habitId)

    fun observeStats(habitId: Long): Flow<HabitStats> =
        completionDao.observeCompletionsForHabit(habitId).map { completions ->
            val completedDates = completions.asSequence().filter { it.completed }
                .mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }
                .toSet()

            HabitStats(
                currentStreak = calculateCurrentStreak(completedDates),
                longestStreak = calculateLongestStreak(completedDates),
                totalCompletions = completedDates.size
            )
        }

    suspend fun createHabit(name: String, createdAtMillis: Long? = null) {
        val sanitized = name.trim()
        if (sanitized.isEmpty()) return
        val defaultCreated = localDateToEpochMillis(currentBusinessDate())
        habitDao.insertHabit(Habit(name = sanitized, createdAt = createdAtMillis ?: defaultCreated))
    }

    suspend fun deleteHabit(habitId: Long) = habitDao.deleteHabitById(habitId)

    suspend fun toggleCompletion(habitId: Long, date: String) {
        val existing = completionDao.getCompletion(habitId, date)
        completionDao.upsertCompletion(
            HabitCompletion(
                id = existing?.id ?: 0,
                habitId = habitId,
                date = date,
                completed = !(existing?.completed ?: false)
            )
        )
    }

    suspend fun saveHabitDayNote(habitId: Long, date: String, note: String) {
        val sanitized = note.trim()
        val existing = habitDayNoteDao.getNoteForDay(habitId, date)
        if (sanitized.isEmpty()) {
            if (existing != null) habitDayNoteDao.deleteById(existing.id)
            return
        }
        habitDayNoteDao.upsert(HabitDayNote(id = existing?.id ?: 0, habitId = habitId, date = date, note = note))
    }

    suspend fun createWhoAmINote(title: String): Long {
        val sanitized = title.trim().ifBlank { "New Note" }
        return whoAmINoteDao.insert(WhoAmINote(title = sanitized, content = "", createdAt = System.currentTimeMillis()))
    }

    suspend fun updateWhoAmINoteContent(noteId: Long, content: String) = whoAmINoteDao.updateContent(noteId, content)
    suspend fun deleteWhoAmINote(noteId: Long) = whoAmINoteDao.deleteById(noteId)

    suspend fun addTask(title: String) {
        val sanitized = title.trim()
        if (sanitized.isEmpty()) return
        taskDao.insert(TaskItem(title = sanitized, isDone = false, createdAt = System.currentTimeMillis()))
    }

    suspend fun setTaskDone(taskId: Long, isDone: Boolean) = taskDao.updateDone(taskId, isDone)
    suspend fun deleteTask(taskId: Long) = taskDao.deleteById(taskId)

    suspend fun addGoal(title: String) {
        val sanitized = title.trim()
        if (sanitized.isEmpty()) return
        goalDao.insert(Goal(title = sanitized, isDone = false, createdAt = System.currentTimeMillis()))
    }

    suspend fun setGoalDone(goalId: Long, isDone: Boolean) = goalDao.updateDone(goalId, isDone)
    suspend fun deleteGoal(goalId: Long) = goalDao.deleteById(goalId)

    suspend fun addSubGoal(goalId: Long, title: String) {
        val sanitized = title.trim()
        if (sanitized.isEmpty()) return
        subGoalDao.insert(SubGoal(goalId = goalId, title = sanitized, isDone = false, createdAt = System.currentTimeMillis()))
    }

    suspend fun setSubGoalDone(subGoalId: Long, isDone: Boolean) = subGoalDao.updateDone(subGoalId, isDone)
    suspend fun deleteSubGoal(subGoalId: Long) = subGoalDao.deleteById(subGoalId)

    suspend fun upsertReminderSettings(habitsEnabled: Boolean, tasksEnabled: Boolean) {
        reminderSettingsDao.upsert(ReminderSettings(id = 0, habitsEnabled = habitsEnabled, tasksEnabled = tasksEnabled))
    }

    suspend fun addReminderTime(timeValue: String) {
        if (!isValidTime(timeValue)) return
        reminderTimeDao.insert(ReminderTime(timeValue = timeValue))
    }

    suspend fun deleteReminderTime(id: Long) = reminderTimeDao.deleteById(id)

    fun currentBusinessDate(now: LocalDateTime = LocalDateTime.now()): LocalDate = now.minusHours(5).toLocalDate()

    fun localDateToEpochMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun isValidTime(value: String): Boolean = runCatching { LocalTime.parse(value) }.isSuccess

    private fun calculateCurrentStreak(completedDates: Set<LocalDate>): Int {
        var streak = 0
        var cursor = currentBusinessDate()
        while (completedDates.contains(cursor)) {
            streak += 1
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    private fun calculateLongestStreak(completedDates: Set<LocalDate>): Int {
        if (completedDates.isEmpty()) return 0
        val sorted = completedDates.toList().sorted()
        var longest = 1
        var current = 1
        for (i in 1 until sorted.size) {
            if (sorted[i - 1].plusDays(1) == sorted[i]) {
                current += 1
                if (current > longest) longest = current
            } else {
                current = 1
            }
        }
        return longest
    }
}
