package com.example.habittracker.repository

import com.example.habittracker.data.dao.GoalDao
import com.example.habittracker.data.dao.HabitCompletionDao
import com.example.habittracker.data.dao.HabitDao
import com.example.habittracker.data.dao.HabitDayNoteDao
import com.example.habittracker.data.dao.SubGoalDao
import com.example.habittracker.data.dao.TaskDao
import com.example.habittracker.data.dao.WhoAmINoteDao
import com.example.habittracker.data.entity.Goal
import com.example.habittracker.data.entity.Habit
import com.example.habittracker.data.entity.HabitCompletion
import com.example.habittracker.data.entity.HabitDayNote
import com.example.habittracker.data.entity.SubGoal
import com.example.habittracker.data.entity.TaskItem
import com.example.habittracker.data.entity.WhoAmINote
import java.time.DayOfWeek
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
    private val subGoalDao: SubGoalDao
) {
    enum class HabitFrequencyType { DAILY, WEEKLY, EVERY_N_DAYS }

    data class HabitOption(
        val id: Long,
        val name: String,
        val createdAt: Long,
        val frequencyType: String,
        val frequencyIntervalDays: Int?,
        val frequencyWeekdays: String?,
        val reminderEnabled: Boolean,
        val reminderTime: String?,
        val reminderMessage: String?
    )

    data class HabitStats(val currentStreak: Int, val longestStreak: Int, val totalCompletions: Int)
    data class GoalWithSubGoals(val goalId: Long, val title: String, val isDone: Boolean, val subGoals: List<SubGoal>)

    data class ReminderScheduleItem(
        val uniqueKey: String,
        val title: String,
        val message: String,
        val timeValue: String
    )

    fun observeHabits(): Flow<List<HabitOption>> = habitDao.observeHabits().map { habits ->
        habits.map {
            HabitOption(
                id = it.id,
                name = it.name,
                createdAt = it.createdAt,
                frequencyType = it.frequencyType,
                frequencyIntervalDays = it.frequencyIntervalDays,
                frequencyWeekdays = it.frequencyWeekdays,
                reminderEnabled = it.reminderEnabled,
                reminderTime = it.reminderTime,
                reminderMessage = it.reminderMessage
            )
        }
    }

    fun observeCompletedDateStrings(habitId: Long): Flow<Set<String>> =
        completionDao.observeCompletionsForHabit(habitId).map { completions ->
            completions.asSequence().filter { it.completed }.map { it.date }.toSet()
        }

    fun observeAllCompletedDateStringsByHabit(): Flow<Map<Long, Set<String>>> =
        completionDao.observeAllCompletions().map { completions ->
            completions
                .asSequence()
                .filter { it.completed }
                .groupBy(keySelector = { it.habitId }, valueTransform = { it.date })
                .mapValues { (_, dates) -> dates.toSet() }
        }

    fun observeCurrentStreaks(): Flow<Map<Long, Int>> =
        combine(habitDao.observeHabits(), completionDao.observeAllCompletions()) { habits, completions ->
            val today = currentBusinessDate()
            val completedByHabitId = completions
                .asSequence()
                .filter { it.completed }
                .groupBy({ it.habitId }, { it.date })
                .mapValues { (_, dates) ->
                    dates.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet()
                }

            habits.associate { habit ->
                val createdDate = epochMillisToLocalDate(habit.createdAt)
                val completedDates = completedByHabitId[habit.id].orEmpty()
                val streak = calculateCurrentScheduledStreak(
                    createdDate = createdDate,
                    today = today,
                    completedDates = completedDates,
                    frequencyTypeValue = habit.frequencyType,
                    frequencyIntervalDays = habit.frequencyIntervalDays,
                    frequencyWeekdays = habit.frequencyWeekdays
                )
                habit.id to streak
            }
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

    fun observeReminderScheduleItems(): Flow<List<ReminderScheduleItem>> =
        combine(habitDao.observeHabits(), taskDao.observeAll()) { habits, tasks ->
            buildScheduleItems(habits, tasks)
        }

    suspend fun getReminderScheduleItems(): List<ReminderScheduleItem> {
        val habits = habitDao.getAllHabits()
        val tasks = taskDao.getAll()
        return buildScheduleItems(habits, tasks)
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

    suspend fun createHabit(
        name: String,
        createdAtMillis: Long? = null,
        frequencyTypeValue: String = HabitFrequencyType.DAILY.name,
        frequencyIntervalDays: Int? = null,
        frequencyWeekdays: String? = null,
        reminderEnabled: Boolean = false,
        reminderTime: String? = null,
        reminderMessage: String? = null
    ) {
        val sanitized = name.trim()
        if (sanitized.isEmpty()) return

        if (reminderEnabled && !isValidTime(reminderTime.orEmpty())) return
        val normalizedReminderTime = if (reminderEnabled) reminderTime?.trim() else null
        val normalizedReminderMessage = reminderMessage?.trim()?.takeIf { it.isNotEmpty() }
        val normalizedFrequencyType = parseFrequencyType(frequencyTypeValue).name
        val normalizedFrequencyInterval = normalizeFrequencyInterval(normalizedFrequencyType, frequencyIntervalDays)
        val normalizedFrequencyWeekdays = normalizeFrequencyWeekdays(normalizedFrequencyType, frequencyWeekdays)
        val defaultCreated = localDateToEpochMillis(currentBusinessDate())

        habitDao.insertHabit(
            Habit(
                name = sanitized,
                createdAt = createdAtMillis ?: defaultCreated,
                frequencyType = normalizedFrequencyType,
                frequencyIntervalDays = normalizedFrequencyInterval,
                frequencyWeekdays = normalizedFrequencyWeekdays,
                reminderEnabled = reminderEnabled,
                reminderTime = normalizedReminderTime,
                reminderMessage = normalizedReminderMessage
            )
        )
    }

    suspend fun updateHabit(
        habitId: Long,
        name: String,
        frequencyTypeValue: String,
        frequencyIntervalDays: Int?,
        frequencyWeekdays: String?,
        reminderEnabled: Boolean,
        reminderTime: String?,
        reminderMessage: String?
    ) {
        val sanitized = name.trim()
        if (sanitized.isEmpty()) return
        if (reminderEnabled && !isValidTime(reminderTime.orEmpty())) return
        val normalizedReminderTime = if (reminderEnabled) reminderTime?.trim() else null
        val normalizedReminderMessage = reminderMessage?.trim()?.takeIf { it.isNotEmpty() }
        val normalizedFrequencyType = parseFrequencyType(frequencyTypeValue).name
        val normalizedFrequencyInterval = normalizeFrequencyInterval(normalizedFrequencyType, frequencyIntervalDays)
        val normalizedFrequencyWeekdays = normalizeFrequencyWeekdays(normalizedFrequencyType, frequencyWeekdays)
        habitDao.updateHabitDetails(
            habitId = habitId,
            name = sanitized,
            frequencyType = normalizedFrequencyType,
            frequencyIntervalDays = normalizedFrequencyInterval,
            frequencyWeekdays = normalizedFrequencyWeekdays,
            reminderEnabled = reminderEnabled,
            reminderTime = normalizedReminderTime,
            reminderMessage = normalizedReminderMessage
        )
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
        val nextSortOrder = whoAmINoteDao.getMaxSortOrder() + 1
        return whoAmINoteDao.insert(
            WhoAmINote(
                title = sanitized,
                content = "",
                sortOrder = nextSortOrder,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateWhoAmINoteContent(noteId: Long, content: String) = whoAmINoteDao.updateContent(noteId, content)
    suspend fun deleteWhoAmINote(noteId: Long) = whoAmINoteDao.deleteById(noteId)
    suspend fun reorderWhoAmINotes(orderedNoteIds: List<Long>) {
        orderedNoteIds.forEachIndexed { index, noteId ->
            whoAmINoteDao.updateSortOrder(noteId = noteId, sortOrder = index)
        }
    }

    suspend fun addTask(
        title: String,
        reminderEnabled: Boolean = false,
        reminderTime: String? = null,
        reminderMessage: String? = null
    ) {
        val sanitized = title.trim()
        if (sanitized.isEmpty()) return

        if (reminderEnabled && !isValidTime(reminderTime.orEmpty())) return
        val normalizedReminderTime = if (reminderEnabled) reminderTime?.trim() else null
        val normalizedReminderMessage = reminderMessage?.trim()?.takeIf { it.isNotEmpty() }
        val nextSortOrder = taskDao.getMaxSortOrderForDoneState(isDone = false) + 1
        taskDao.insert(
            TaskItem(
                title = sanitized,
                isDone = false,
                sortOrder = nextSortOrder,
                createdAt = System.currentTimeMillis(),
                reminderEnabled = reminderEnabled,
                reminderTime = normalizedReminderTime,
                reminderMessage = normalizedReminderMessage
            )
        )
    }

    suspend fun updateTask(
        taskId: Long,
        title: String,
        reminderEnabled: Boolean,
        reminderTime: String?,
        reminderMessage: String?
    ) {
        val sanitized = title.trim()
        if (sanitized.isEmpty()) return
        if (reminderEnabled && !isValidTime(reminderTime.orEmpty())) return
        val normalizedReminderTime = if (reminderEnabled) reminderTime?.trim() else null
        val normalizedReminderMessage = reminderMessage?.trim()?.takeIf { it.isNotEmpty() }
        taskDao.updateTaskDetails(
            taskId = taskId,
            title = sanitized,
            reminderEnabled = reminderEnabled,
            reminderTime = normalizedReminderTime,
            reminderMessage = normalizedReminderMessage
        )
    }

    suspend fun setTaskDone(taskId: Long, isDone: Boolean) {
        taskDao.updateDone(taskId, isDone)
        val nextSortOrder = taskDao.getMaxSortOrderForDoneState(isDone = isDone) + 1
        taskDao.updateSortOrder(taskId = taskId, sortOrder = nextSortOrder)
    }

    suspend fun reorderTasksForDoneState(orderedTaskIds: List<Long>) {
        orderedTaskIds.forEachIndexed { index, taskId ->
            taskDao.updateSortOrder(taskId = taskId, sortOrder = index)
        }
    }

    suspend fun deleteTask(taskId: Long) = taskDao.deleteById(taskId)

    suspend fun addGoal(title: String) {
        val sanitized = title.trim()
        if (sanitized.isEmpty()) return
        val nextSortOrder = goalDao.getMaxSortOrderForDoneState(isDone = false) + 1
        goalDao.insert(
            Goal(
                title = sanitized,
                isDone = false,
                sortOrder = nextSortOrder,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun setGoalDone(goalId: Long, isDone: Boolean) {
        goalDao.updateDone(goalId, isDone)
        val nextSortOrder = goalDao.getMaxSortOrderForDoneState(isDone = isDone) + 1
        goalDao.updateSortOrder(goalId = goalId, sortOrder = nextSortOrder)
    }

    suspend fun reorderGoalsForDoneState(orderedGoalIds: List<Long>) {
        orderedGoalIds.forEachIndexed { index, goalId ->
            goalDao.updateSortOrder(goalId = goalId, sortOrder = index)
        }
    }

    suspend fun deleteGoal(goalId: Long) = goalDao.deleteById(goalId)

    suspend fun addSubGoal(goalId: Long, title: String) {
        val sanitized = title.trim()
        if (sanitized.isEmpty()) return
        subGoalDao.insert(SubGoal(goalId = goalId, title = sanitized, isDone = false, createdAt = System.currentTimeMillis()))
    }

    suspend fun setSubGoalDone(subGoalId: Long, isDone: Boolean) = subGoalDao.updateDone(subGoalId, isDone)
    suspend fun deleteSubGoal(subGoalId: Long) = subGoalDao.deleteById(subGoalId)

    fun currentBusinessDate(now: LocalDateTime = LocalDateTime.now()): LocalDate = now.minusHours(5).toLocalDate()

    fun localDateToEpochMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun epochMillisToLocalDate(epochMillis: Long): LocalDate =
        java.time.Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()

    fun isValidTime(value: String): Boolean = runCatching { LocalTime.parse(value) }.isSuccess

    fun isScheduledOnDate(
        createdDate: LocalDate,
        targetDate: LocalDate,
        frequencyTypeValue: String,
        frequencyIntervalDays: Int?,
        frequencyWeekdays: String?
    ): Boolean {
        if (targetDate.isBefore(createdDate)) return false
        val frequencyType = parseFrequencyType(frequencyTypeValue)
        return when (frequencyType) {
            HabitFrequencyType.DAILY -> true
            HabitFrequencyType.WEEKLY -> {
                val weekdays = parseWeekdaySet(frequencyWeekdays)
                weekdays.contains(targetDate.dayOfWeek.value)
            }
            HabitFrequencyType.EVERY_N_DAYS -> {
                val interval = normalizeFrequencyInterval(frequencyType.name, frequencyIntervalDays) ?: 1
                val daysBetween = targetDate.toEpochDay() - createdDate.toEpochDay()
                daysBetween >= 0 && daysBetween % interval == 0L
            }
        }
    }

    fun calculateScheduledDates(
        createdDate: LocalDate,
        untilDate: LocalDate,
        frequencyTypeValue: String,
        frequencyIntervalDays: Int?,
        frequencyWeekdays: String?
    ): Set<String> {
        if (untilDate.isBefore(createdDate)) return emptySet()
        return generateSequence(createdDate) { it.plusDays(1) }
            .takeWhile { !it.isAfter(untilDate) }
            .filter {
                isScheduledOnDate(
                    createdDate = createdDate,
                    targetDate = it,
                    frequencyTypeValue = frequencyTypeValue,
                    frequencyIntervalDays = frequencyIntervalDays,
                    frequencyWeekdays = frequencyWeekdays
                )
            }
            .map(LocalDate::toString)
            .toSet()
    }

    private fun buildScheduleItems(habits: List<Habit>, tasks: List<TaskItem>): List<ReminderScheduleItem> {
        val habitItems = habits.asSequence()
            .filter { it.reminderEnabled && isValidTime(it.reminderTime.orEmpty()) }
            .map {
                ReminderScheduleItem(
                    uniqueKey = "habit:${it.id}",
                    title = "Habit reminder",
                    message = it.reminderMessage ?: "Habit: ${it.name}",
                    timeValue = it.reminderTime.orEmpty()
                )
            }

        val taskItems = tasks.asSequence()
            .filter { !it.isDone && it.reminderEnabled && isValidTime(it.reminderTime.orEmpty()) }
            .map {
                ReminderScheduleItem(
                    uniqueKey = "task:${it.id}",
                    title = "Task reminder",
                    message = it.reminderMessage ?: "Task: ${it.title}",
                    timeValue = it.reminderTime.orEmpty()
                )
            }

        return (habitItems + taskItems).toList()
    }

    private fun calculateCurrentStreak(completedDates: Set<LocalDate>): Int {
        return calculateCurrentStreakFrom(currentBusinessDate(), completedDates)
    }

    private fun calculateCurrentStreakFrom(today: LocalDate, completedDates: Set<LocalDate>): Int {
        var streak = 0
        var cursor = today
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

    private fun calculateCurrentScheduledStreak(
        createdDate: LocalDate,
        today: LocalDate,
        completedDates: Set<LocalDate>,
        frequencyTypeValue: String,
        frequencyIntervalDays: Int?,
        frequencyWeekdays: String?
    ): Int {
        val scheduledDates = generateSequence(createdDate) { it.plusDays(1) }
            .takeWhile { !it.isAfter(today) }
            .filter {
                isScheduledOnDate(
                    createdDate = createdDate,
                    targetDate = it,
                    frequencyTypeValue = frequencyTypeValue,
                    frequencyIntervalDays = frequencyIntervalDays,
                    frequencyWeekdays = frequencyWeekdays
                )
            }
            .toList()
            .asReversed()
        if (scheduledDates.isEmpty()) return 0

        var streak = 0
        for (scheduledDate in scheduledDates) {
            if (completedDates.contains(scheduledDate)) {
                streak += 1
            } else {
                break
            }
        }
        return streak
    }

    private fun parseFrequencyType(value: String?): HabitFrequencyType {
        return runCatching { HabitFrequencyType.valueOf(value.orEmpty()) }
            .getOrElse { HabitFrequencyType.DAILY }
    }

    private fun normalizeFrequencyInterval(frequencyTypeValue: String, value: Int?): Int? {
        return if (parseFrequencyType(frequencyTypeValue) == HabitFrequencyType.EVERY_N_DAYS) {
            val interval = value ?: 1
            interval.coerceAtLeast(1)
        } else {
            null
        }
    }

    private fun normalizeFrequencyWeekdays(frequencyTypeValue: String, value: String?): String? {
        return if (parseFrequencyType(frequencyTypeValue) == HabitFrequencyType.WEEKLY) {
            val parsed = parseWeekdaySet(value)
            val normalized = if (parsed.isEmpty()) {
                setOf(DayOfWeek.MONDAY.value)
            } else {
                parsed
            }
            normalized.sorted().joinToString(",")
        } else {
            null
        }
    }

    private fun parseWeekdaySet(value: String?): Set<Int> {
        if (value.isNullOrBlank()) return emptySet()
        return value.split(",")
            .mapNotNull { token -> token.trim().toIntOrNull() }
            .filter { it in 1..7 }
            .toSet()
    }
}
