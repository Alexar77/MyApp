package com.example.habittracker.repository

import com.example.habittracker.data.dao.GoalDao
import com.example.habittracker.data.dao.BirthdayDao
import com.example.habittracker.data.dao.HabitCompletionDao
import com.example.habittracker.data.dao.HabitDao
import com.example.habittracker.data.dao.HabitDayNoteDao
import com.example.habittracker.data.dao.SubGoalDao
import com.example.habittracker.data.dao.TaskCategoryDao
import com.example.habittracker.data.dao.TaskDao
import com.example.habittracker.data.dao.WhoAmINoteDao
import com.example.habittracker.data.entity.Goal
import com.example.habittracker.data.entity.Birthday
import com.example.habittracker.data.entity.Habit
import com.example.habittracker.data.entity.HabitCompletion
import com.example.habittracker.data.entity.HabitDayNote
import com.example.habittracker.data.entity.SubGoal
import com.example.habittracker.data.entity.TaskCategory
import com.example.habittracker.data.entity.TaskItem
import com.example.habittracker.data.entity.WhoAmINote
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
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
    private val taskCategoryDao: TaskCategoryDao,
    private val goalDao: GoalDao,
    private val subGoalDao: SubGoalDao,
    private val birthdayDao: BirthdayDao
) {
    companion object {
        const val DEFAULT_TASK_CATEGORY = "General"
    }

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
    data class GoalWithSubGoals(
        val goalId: Long,
        val title: String,
        val isDone: Boolean,
        val completedAt: Long?,
        val subGoals: List<SubGoal>
    )

    data class ReminderScheduleItem(
        val uniqueKey: String,
        val title: String,
        val message: String,
        val timeValue: String,
        val triggerAtMillis: Long? = null
    )

    data class BirthdayOption(
        val id: Long,
        val name: String,
        val year: Int,
        val month: Int,
        val day: Int,
        val reminderDateTimesCsv: String?
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
    }.distinctUntilChanged()

    fun observeCompletedDateStrings(habitId: Long): Flow<Set<String>> =
        completionDao.observeCompletionsForHabit(habitId).map { completions ->
            completions.asSequence().filter { it.completed }.map { it.date }.toSet()
        }.distinctUntilChanged()

    fun observeAllCompletedDateStringsByHabit(): Flow<Map<Long, Set<String>>> =
        completionDao.observeAllCompletions().map { completions ->
            completions
                .asSequence()
                .filter { it.completed }
                .groupBy(keySelector = { it.habitId }, valueTransform = { it.date })
                .mapValues { (_, dates) -> dates.toSet() }
        }.distinctUntilChanged()

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
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)

    fun observeHabitDayNotes(habitId: Long): Flow<Map<String, String>> =
        habitDayNoteDao.observeNotesForHabit(habitId).map { notes ->
            notes.associate { it.date to it.note }
        }.distinctUntilChanged()

    fun observeAllHabitDayNotes(): Flow<List<HabitDayNote>> =
        habitDayNoteDao.observeAllNotes().distinctUntilChanged()

    fun observeWhoAmINotes(): Flow<List<WhoAmINote>> =
        whoAmINoteDao.observeAll().distinctUntilChanged()

    fun observeTasks(): Flow<List<TaskItem>> =
        taskDao.observeAll().distinctUntilChanged()

    fun observeGoalsWithSubGoals(): Flow<List<GoalWithSubGoals>> =
        combine(goalDao.observeAll(), subGoalDao.observeAll()) { goals, subGoals ->
            val subGoalsByGoalId = subGoals.groupBy { it.goalId }
            goals.map { goal ->
                GoalWithSubGoals(
                    goalId = goal.id,
                    title = goal.title,
                    isDone = goal.isDone,
                    completedAt = goal.completedAt,
                    subGoals = subGoalsByGoalId[goal.id].orEmpty()
                )
            }
        }.distinctUntilChanged()

    fun observeReminderScheduleItems(): Flow<List<ReminderScheduleItem>> =
        combine(habitDao.observeHabits(), taskDao.observeAll(), birthdayDao.observeAll()) { habits, tasks, birthdays ->
            buildScheduleItems(habits, tasks, birthdays)
        }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)

    fun observeBirthdays(): Flow<List<BirthdayOption>> =
        birthdayDao.observeAll().map { birthdays ->
            birthdays.map {
                BirthdayOption(
                    id = it.id,
                    name = it.name,
                    year = it.year,
                    month = it.month,
                    day = it.day,
                    reminderDateTimesCsv = it.reminderDateTimesCsv
                )
            }
        }.distinctUntilChanged()

    suspend fun getReminderScheduleItems(): List<ReminderScheduleItem> {
        val habits = habitDao.getAllHabits()
        val tasks = taskDao.getAll()
        val birthdays = birthdayDao.getAll()
        return buildScheduleItems(habits, tasks, birthdays)
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

        if (reminderEnabled && !isValidReminderTimesCsv(reminderTime)) return
        val normalizedReminderTime = if (reminderEnabled) {
            parseReminderTimesCsv(reminderTime).joinToString(",").takeIf { it.isNotBlank() }
        } else {
            null
        }
        val normalizedReminderMessage = reminderMessage?.trim()?.takeIf { it.isNotEmpty() }
        val normalizedFrequencyType = parseFrequencyType(frequencyTypeValue).name
        val normalizedFrequencyInterval = normalizeFrequencyInterval(normalizedFrequencyType, frequencyIntervalDays)
        val normalizedFrequencyWeekdays = normalizeFrequencyWeekdays(normalizedFrequencyType, frequencyWeekdays)
        val defaultCreated = localDateToEpochMillis(currentBusinessDate())

        habitDao.insertHabit(
            Habit(
                name = sanitized,
                createdAt = createdAtMillis ?: defaultCreated,
                sortOrder = habitDao.getMaxSortOrder() + 1,
                frequencyType = normalizedFrequencyType,
                frequencyIntervalDays = normalizedFrequencyInterval,
                frequencyWeekdays = normalizedFrequencyWeekdays,
                reminderEnabled = reminderEnabled,
                reminderTime = normalizedReminderTime,
                reminderMessage = normalizedReminderMessage
            )
        )
    }

    suspend fun reorderHabits(orderedHabitIds: List<Long>) {
        orderedHabitIds.forEachIndexed { index, habitId ->
            habitDao.updateSortOrder(habitId, index)
        }
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
        if (reminderEnabled && !isValidReminderTimesCsv(reminderTime)) return
        val normalizedReminderTime = if (reminderEnabled) {
            parseReminderTimesCsv(reminderTime).joinToString(",").takeIf { it.isNotBlank() }
        } else {
            null
        }
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
    suspend fun renameWhoAmINote(noteId: Long, title: String) {
        val sanitized = title.trim()
        if (sanitized.isEmpty()) return
        whoAmINoteDao.updateTitle(noteId, sanitized)
    }

    suspend fun deleteWhoAmINote(noteId: Long) = whoAmINoteDao.deleteById(noteId)
    suspend fun reorderWhoAmINotes(orderedNoteIds: List<Long>) {
        orderedNoteIds.forEachIndexed { index, noteId ->
            whoAmINoteDao.updateSortOrder(noteId = noteId, sortOrder = index)
        }
    }

    suspend fun addTask(
        title: String,
        category: String = DEFAULT_TASK_CATEGORY,
        reminderEnabled: Boolean = false,
        reminderTime: String? = null,
        reminderDateTimesCsv: String? = null,
        reminderMessage: String? = null
    ) {
        val sanitized = title.trim()
        if (sanitized.isEmpty()) return
        val normalizedCategory = category.trim().ifBlank { DEFAULT_TASK_CATEGORY }

        val normalizedReminderDateTimesCsv = if (reminderEnabled) {
            normalizeReminderDateTimesCsv(reminderDateTimesCsv)
        } else {
            null
        }
        if (reminderEnabled && normalizedReminderDateTimesCsv == null && !isValidTime(reminderTime.orEmpty())) return
        val normalizedReminderTime = if (reminderEnabled) reminderTime?.trim() else null
        val normalizedReminderMessage = reminderMessage?.trim()?.takeIf { it.isNotEmpty() }
        val nextSortOrder = taskDao.getMaxSortOrderForDoneState(isDone = false) + 1
        taskDao.insert(
            TaskItem(
                title = sanitized,
                category = normalizedCategory,
                isDone = false,
                sortOrder = nextSortOrder,
                createdAt = System.currentTimeMillis(),
                reminderEnabled = reminderEnabled,
                reminderTime = normalizedReminderTime,
                reminderDateTimesCsv = normalizedReminderDateTimesCsv,
                reminderMessage = normalizedReminderMessage
            )
        )
    }

    suspend fun updateTask(
        taskId: Long,
        title: String,
        category: String,
        reminderEnabled: Boolean,
        reminderTime: String?,
        reminderDateTimesCsv: String?,
        reminderMessage: String?
    ) {
        val sanitized = title.trim()
        if (sanitized.isEmpty()) return
        val normalizedCategory = category.trim().ifBlank { DEFAULT_TASK_CATEGORY }
        val normalizedReminderDateTimesCsv = if (reminderEnabled) {
            normalizeReminderDateTimesCsv(reminderDateTimesCsv)
        } else {
            null
        }
        if (reminderEnabled && normalizedReminderDateTimesCsv == null && !isValidTime(reminderTime.orEmpty())) return
        val normalizedReminderTime = if (reminderEnabled) reminderTime?.trim() else null
        val normalizedReminderMessage = reminderMessage?.trim()?.takeIf { it.isNotEmpty() }
        taskDao.updateTaskDetails(
            taskId = taskId,
            title = sanitized,
            category = normalizedCategory,
            reminderEnabled = reminderEnabled,
            reminderTime = normalizedReminderTime,
            reminderDateTimesCsv = normalizedReminderDateTimesCsv,
            reminderMessage = normalizedReminderMessage
        )
    }

    suspend fun setTaskDone(taskId: Long, isDone: Boolean) {
        val completedAt = if (isDone) System.currentTimeMillis() else null
        taskDao.updateDone(taskId, isDone, completedAt)
        val nextSortOrder = taskDao.getMaxSortOrderForDoneState(isDone = isDone) + 1
        taskDao.updateSortOrder(taskId = taskId, sortOrder = nextSortOrder)
    }

    suspend fun reorderTasksForDoneState(orderedTaskIds: List<Long>) {
        orderedTaskIds.forEachIndexed { index, taskId ->
            taskDao.updateSortOrder(taskId = taskId, sortOrder = index)
        }
    }

    suspend fun deleteTask(taskId: Long) = taskDao.deleteById(taskId)

    suspend fun moveAllTasksToCategory(fromCategory: String, toCategory: String) {
        val from = fromCategory.trim().ifBlank { return }
        val to = toCategory.trim().ifBlank { DEFAULT_TASK_CATEGORY }
        if (from == to) return
        taskDao.moveAllToCategory(fromCategory = from, toCategory = to)
    }

    suspend fun deleteAllTasksInCategory(category: String) {
        val sanitized = category.trim()
        if (sanitized.isBlank()) return
        taskDao.deleteByCategory(sanitized)
    }

    // ---- Task categories ----

    fun observeTaskCategories(): Flow<List<TaskCategory>> =
        taskCategoryDao.observeAll().distinctUntilChanged()

    suspend fun addTaskCategory(name: String) {
        val sanitized = name.trim()
        if (sanitized.isBlank()) return
        val nextOrder = taskCategoryDao.getMaxSortOrder() + 1
        taskCategoryDao.insert(TaskCategory(name = sanitized, sortOrder = nextOrder))
    }

    suspend fun deleteTaskCategory(name: String) {
        val sanitized = name.trim()
        if (sanitized.isBlank() || sanitized == DEFAULT_TASK_CATEGORY) return
        taskCategoryDao.deleteByName(sanitized)
    }

    suspend fun reorderTaskCategories(orderedNames: List<String>) {
        orderedNames.forEachIndexed { index, name ->
            taskCategoryDao.updateSortOrder(name, index)
        }
    }

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
        val completedAt = if (isDone) System.currentTimeMillis() else null
        goalDao.updateDone(goalId, isDone, completedAt)
        val nextSortOrder = goalDao.getMaxSortOrderForDoneState(isDone = isDone) + 1
        goalDao.updateSortOrder(goalId = goalId, sortOrder = nextSortOrder)
    }

    suspend fun reorderGoalsForDoneState(orderedGoalIds: List<Long>) {
        orderedGoalIds.forEachIndexed { index, goalId ->
            goalDao.updateSortOrder(goalId = goalId, sortOrder = index)
        }
    }

    suspend fun renameGoal(goalId: Long, title: String) {
        val sanitized = title.trim()
        if (sanitized.isEmpty()) return
        goalDao.updateTitle(goalId, sanitized)
    }

    suspend fun deleteGoal(goalId: Long) = goalDao.deleteById(goalId)

    suspend fun addSubGoal(goalId: Long, title: String) {
        val sanitized = title.trim()
        if (sanitized.isEmpty()) return
        subGoalDao.insert(SubGoal(goalId = goalId, title = sanitized, isDone = false, createdAt = System.currentTimeMillis()))
    }

    suspend fun renameSubGoal(subGoalId: Long, title: String) {
        val sanitized = title.trim()
        if (sanitized.isEmpty()) return
        subGoalDao.updateTitle(subGoalId, sanitized)
    }

    suspend fun setSubGoalDone(subGoalId: Long, isDone: Boolean) {
        val completedAt = if (isDone) System.currentTimeMillis() else null
        subGoalDao.updateDone(subGoalId, isDone, completedAt)
    }
    suspend fun deleteSubGoal(subGoalId: Long) = subGoalDao.deleteById(subGoalId)

    suspend fun addBirthday(name: String, year: Int, month: Int, day: Int): Long {
        val sanitized = name.trim()
        if (sanitized.isEmpty()) return 0L
        if (!isValidDate(year, month, day)) return 0L
        return birthdayDao.insert(
            Birthday(
                name = sanitized,
                year = year,
                month = month,
                day = day,
                reminderDateTimesCsv = null,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateBirthday(
        birthdayId: Long,
        name: String,
        year: Int,
        month: Int,
        day: Int,
        reminderDateTimesCsv: String?
    ) {
        val sanitized = name.trim()
        if (sanitized.isEmpty()) return
        if (!isValidDate(year, month, day)) return
        birthdayDao.update(
            birthdayId = birthdayId,
            name = sanitized,
            year = year,
            month = month,
            day = day,
            reminderDateTimesCsv = normalizeReminderDateTimesCsv(reminderDateTimesCsv)
        )
    }

    suspend fun deleteBirthday(birthdayId: Long) = birthdayDao.deleteById(birthdayId)

    fun birthdayOccurrenceInYear(month: Int, day: Int, year: Int): LocalDate {
        val safeMonth = month.coerceIn(1, 12)
        val maxDay = YearMonth.of(year, safeMonth).lengthOfMonth()
        val safeDay = day.coerceIn(1, maxDay)
        return LocalDate.of(year, safeMonth, safeDay)
    }

    fun nextBirthdayOccurrence(fromDate: LocalDate, month: Int, day: Int): LocalDate {
        val currentYearOccurrence = birthdayOccurrenceInYear(month = month, day = day, year = fromDate.year)
        return if (!currentYearOccurrence.isBefore(fromDate)) {
            currentYearOccurrence
        } else {
            birthdayOccurrenceInYear(month = month, day = day, year = fromDate.year + 1)
        }
    }

    fun currentBusinessDate(now: LocalDateTime = LocalDateTime.now()): LocalDate = now.minusHours(5).toLocalDate()

    fun localDateToEpochMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun epochMillisToLocalDate(epochMillis: Long): LocalDate =
        java.time.Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()

    fun isValidTime(value: String): Boolean = runCatching { LocalTime.parse(value) }.isSuccess
    fun parseReminderTimesCsv(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() && isValidTime(it) }
            .distinct()
    }

    fun isValidReminderTimesCsv(value: String?): Boolean {
        val parsed = parseReminderTimesCsv(value)
        return parsed.isNotEmpty()
    }

    fun parseReminderDateTimesCsv(value: String?): List<Long> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split("|")
            .mapNotNull { token -> token.trim().toLongOrNull() }
            .distinct()
            .sorted()
    }

    fun normalizeReminderDateTimesCsv(value: String?): String? {
        val parsed = parseReminderDateTimesCsv(value)
        return parsed.takeIf { it.isNotEmpty() }?.joinToString("|")
    }

    fun isValidDate(year: Int, month: Int, day: Int): Boolean =
        runCatching { LocalDate.of(year, month, day) }.isSuccess

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

    private fun buildScheduleItems(
        habits: List<Habit>,
        tasks: List<TaskItem>,
        birthdays: List<Birthday>
    ): List<ReminderScheduleItem> {
        val habitItems = habits.asSequence()
            .filter { it.reminderEnabled }
            .flatMap { habit ->
                parseReminderTimesCsv(habit.reminderTime).asSequence().map { timeValue ->
                    ReminderScheduleItem(
                        uniqueKey = "habit:${habit.id}:$timeValue",
                        title = "Habit reminder",
                        message = habit.reminderMessage ?: "Habit: ${habit.name}",
                        timeValue = timeValue
                    )
                }
            }

        val now = System.currentTimeMillis()
        val taskItems = tasks.asSequence()
            .filter { !it.isDone && it.reminderEnabled }
            .flatMap { task ->
                val oneTimeItems = parseReminderDateTimesCsv(task.reminderDateTimesCsv)
                    .asSequence()
                    .filter { triggerAt -> triggerAt > now }
                    .mapIndexed { index, triggerAt ->
                        ReminderScheduleItem(
                            uniqueKey = "task:${task.id}:one:$index",
                            title = "Task reminder",
                            message = task.reminderMessage ?: "Task: ${task.title}",
                            timeValue = "00:00",
                            triggerAtMillis = triggerAt
                        )
                    }

                val dailyFallback = if (oneTimeItems.none() && isValidTime(task.reminderTime.orEmpty())) {
                    sequenceOf(
                        ReminderScheduleItem(
                            uniqueKey = "task:${task.id}",
                            title = "Task reminder",
                            message = task.reminderMessage ?: "Task: ${task.title}",
                            timeValue = task.reminderTime.orEmpty()
                        )
                    )
                } else {
                    emptySequence()
                }

                oneTimeItems + dailyFallback
            }

        val birthdayItems = birthdays.asSequence()
            .flatMap { birthday ->
                parseReminderDateTimesCsv(birthday.reminderDateTimesCsv).asSequence()
                    .filter { triggerAtMillis -> triggerAtMillis > now }
                    .mapIndexed { index, triggerAtMillis ->
                        ReminderScheduleItem(
                            uniqueKey = "birthday:${birthday.id}:$index",
                            title = "Birthday reminder",
                            message = "Birthday: ${birthday.name}",
                            timeValue = "00:00",
                            triggerAtMillis = triggerAtMillis
                        )
                    }
            }

        return (habitItems + taskItems + birthdayItems).toList()
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
        // Walk backwards from today – O(streak_length) instead of O(days_since_creation)
        var streak = 0
        var cursor = today
        while (!cursor.isBefore(createdDate)) {
            val scheduled = isScheduledOnDate(
                createdDate = createdDate,
                targetDate = cursor,
                frequencyTypeValue = frequencyTypeValue,
                frequencyIntervalDays = frequencyIntervalDays,
                frequencyWeekdays = frequencyWeekdays
            )
            if (scheduled) {
                if (completedDates.contains(cursor)) {
                    streak += 1
                } else {
                    break
                }
            }
            cursor = cursor.minusDays(1)
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
