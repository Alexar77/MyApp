package com.example.habittracker.repository

import android.content.SharedPreferences
import com.example.habittracker.data.dao.GoalDao
import com.example.habittracker.data.dao.BirthdayDao
import com.example.habittracker.data.dao.HomeMonthSnapshotDao
import com.example.habittracker.data.dao.HabitCompletionDao
import com.example.habittracker.data.dao.HabitDao
import com.example.habittracker.data.dao.HabitDayNoteDao
import com.example.habittracker.data.dao.MoneyExpenseDao
import com.example.habittracker.data.dao.MoneySettingsDao
import com.example.habittracker.data.dao.MoodEntryDao
import com.example.habittracker.data.dao.SubGoalDao
import com.example.habittracker.data.dao.TaskCategoryDao
import com.example.habittracker.data.dao.TaskDao
import com.example.habittracker.data.dao.WeightEntryDao
import com.example.habittracker.data.dao.WhoAmINoteDao
import com.example.habittracker.data.entity.Goal
import com.example.habittracker.data.entity.Birthday
import com.example.habittracker.data.entity.HomeMonthSnapshot
import com.example.habittracker.data.entity.Habit
import com.example.habittracker.data.entity.HabitCompletion
import com.example.habittracker.data.entity.HabitDayNote
import com.example.habittracker.data.entity.MoneyExpense
import com.example.habittracker.data.entity.MoneySettings
import com.example.habittracker.data.entity.MoodEntry
import com.example.habittracker.data.entity.SubGoal
import com.example.habittracker.data.entity.TaskCategory
import com.example.habittracker.data.entity.TaskItem
import com.example.habittracker.data.entity.WeightEntry
import com.example.habittracker.data.entity.WhoAmINote
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.nio.charset.StandardCharsets
import java.net.URLDecoder
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.measureTimeMillis

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
    private val birthdayDao: BirthdayDao,
    private val homeMonthSnapshotDao: HomeMonthSnapshotDao,
    private val moneySettingsDao: MoneySettingsDao,
    private val moneyExpenseDao: MoneyExpenseDao,
    private val weightEntryDao: WeightEntryDao,
    private val moodEntryDao: MoodEntryDao,
    private val appPreferences: SharedPreferences
) {

    companion object {
        const val DEFAULT_TASK_CATEGORY = "General"
        private const val HOME_SNAPSHOT_PREF_PREFIX = "home_snapshot_"
        private const val HABIT_OPTIONS_PREF_KEY = "habit_options_cache"
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

    data class SelectedHabitMonthSnapshot(
        val habitId: Long?,
        val month: YearMonth,
        val completedDates: Set<String>,
        val scheduledDates: Set<String>,
        val dayNotesByDate: Map<String, String>,
        val createdDate: LocalDate?
    )

    data class GlobalDayDetailSnapshot(
        val habitId: Long,
        val habitName: String,
        val isDone: Boolean,
        val note: String?
    )

    data class GlobalMoneyEntrySnapshot(
        val title: String,
        val category: String,
        val amount: Double,
        val isIncome: Boolean
    )

    data class GlobalMoodSnapshot(
        val mood: String,
        val note: String?
    )

    private data class GlobalMonthHabitData(
        val habits: List<Habit>,
        val completions: List<HabitCompletion>,
        val notes: List<HabitDayNote>,
        val birthdays: List<Birthday>
    )

    private data class GlobalMonthOtherData(
        val tasks: List<TaskItem>,
        val moneyEntries: List<MoneyExpense>,
        val weightEntries: List<WeightEntry>,
        val moodEntries: List<MoodEntry>
    )

    data class GlobalMonthSnapshot(
        val month: YearMonth,
        val globalCompletedDates: Set<String>,
        val globalScheduledDates: Set<String>,
        val globalBirthdayDates: Set<String>,
        val globalNoteDates: Set<String>,
        val birthdayNamesByDate: Map<String, List<String>>,
        val dayDetailsByDate: Map<String, List<GlobalDayDetailSnapshot>>,
        val completedTasksByDate: Map<String, List<String>>,
        val moneyEntriesByDate: Map<String, List<GlobalMoneyEntrySnapshot>>,
        val weightByDate: Map<String, Double>,
        val moodByDate: Map<String, GlobalMoodSnapshot>,
        val businessToday: LocalDate
    )

    data class HomeMonthSnapshotState(
        val month: YearMonth,
        val selectedHabitId: Long?,
        val selectedCompletedDates: Set<String>,
        val selectedScheduledDates: Set<String>,
        val globalCompletedDates: Set<String>,
        val globalScheduledDates: Set<String>,
        val globalBirthdayDates: Set<String>,
        val globalNoteDates: Set<String>,
        val dayNotesByDate: Map<String, String>,
        val selectedHabitCreatedDate: LocalDate?,
        val businessToday: LocalDate,
        val updatedAt: Long
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
        }.also(::cacheHabitOptions)
    }.distinctUntilChanged()

    fun peekCachedHabitOptions(): List<HabitOption> =
        appPreferences.getString(HABIT_OPTIONS_PREF_KEY, null)
            ?.takeIf { it.isNotBlank() }
            ?.let(::deserializeHabitOptions)
            .orEmpty()

    fun observeCompletedDateStrings(habitId: Long): Flow<Set<String>> =
        completionDao.observeCompletionsForHabit(habitId).map { completions ->
            completions.asSequence().filter { it.completed }.map { it.date }.toSet()
        }.distinctUntilChanged()

    fun observeCompletedDateStringsInRange(habitId: Long, month: YearMonth): Flow<Set<String>> {
        val (startDate, endDate) = monthRange(month)
        return completionDao.observeCompletionsForHabitInRange(habitId, startDate, endDate).map { completions ->
            completions.asSequence().filter { it.completed }.map { it.date }.toSet()
        }.distinctUntilChanged()
    }

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

    fun observeHabitDayNotesInRange(habitId: Long, month: YearMonth): Flow<Map<String, String>> {
        val (startDate, endDate) = monthRange(month)
        return habitDayNoteDao.observeNotesForHabitInRange(habitId, startDate, endDate).map { notes ->
            notes.associate { it.date to it.note }
        }.distinctUntilChanged()
    }

    fun observeAllHabitDayNotes(): Flow<List<HabitDayNote>> =
        habitDayNoteDao.observeAllNotes().distinctUntilChanged()

    fun observeAllHabitDayNotesInRange(month: YearMonth): Flow<List<HabitDayNote>> {
        val (startDate, endDate) = monthRange(month)
        return habitDayNoteDao.observeNotesInRange(startDate, endDate).distinctUntilChanged()
    }

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

    fun observeMoneySettings(): Flow<MoneySettings?> =
        moneySettingsDao.observe().distinctUntilChanged()

    fun observeMoneyExpenses(): Flow<List<MoneyExpense>> =
        moneyExpenseDao.observeAll().distinctUntilChanged()

    fun observeWeightEntries(): Flow<List<WeightEntry>> =
        weightEntryDao.observeAll().distinctUntilChanged()

    fun observeMoodEntries(): Flow<List<MoodEntry>> =
        moodEntryDao.observeAll().distinctUntilChanged()

    fun observeSelectedHabitMonth(habitId: Long, month: YearMonth): Flow<SelectedHabitMonthSnapshot> {
        val (startDate, endDate) = monthRange(month)
        return combine(
            habitDao.observeHabit(habitId),
            completionDao.observeCompletionsForHabitInRange(habitId, startDate, endDate),
            habitDayNoteDao.observeNotesForHabitInRange(habitId, startDate, endDate)
        ) { habit, completions, notes ->
            var snapshot: SelectedHabitMonthSnapshot? = null
            measureTimeMillis {
                val today = currentBusinessDate()
                val createdDate = habit?.let { epochMillisToLocalDate(it.createdAt) }
                val scheduledDates = if (habit == null || createdDate == null) {
                    emptySet()
                } else {
                    calculateScheduledDatesInMonth(
                        createdDate = createdDate,
                        month = month,
                        today = today,
                        frequencyTypeValue = habit.frequencyType,
                        frequencyIntervalDays = habit.frequencyIntervalDays,
                        frequencyWeekdays = habit.frequencyWeekdays
                    )
                }

                snapshot = SelectedHabitMonthSnapshot(
                    habitId = habit?.id,
                    month = month,
                    completedDates = completions.asSequence()
                        .filter { it.completed }
                        .map { it.date }
                        .toSet(),
                    scheduledDates = scheduledDates,
                    dayNotesByDate = notes.associate { it.date to it.note },
                    createdDate = createdDate
                )
            }
            snapshot ?: SelectedHabitMonthSnapshot(
                habitId = habit?.id,
                month = month,
                completedDates = emptySet(),
                scheduledDates = emptySet(),
                dayNotesByDate = emptyMap(),
                createdDate = habit?.let { epochMillisToLocalDate(it.createdAt) }
            )
        }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
    }

    fun observeGlobalMonth(month: YearMonth): Flow<GlobalMonthSnapshot> {
        val (startDate, endDate) = monthRange(month)
        val startLocalDate = month.atDay(1)
        val endLocalDate = month.atEndOfMonth()
        val habitDataFlow = combine(
            habitDao.observeHabits(),
            completionDao.observeCompletionsInRange(startDate, endDate),
            habitDayNoteDao.observeNotesInRange(startDate, endDate),
            birthdayDao.observeAll()
        ) { habits, completions, notes, birthdays ->
            GlobalMonthHabitData(
                habits = habits,
                completions = completions,
                notes = notes,
                birthdays = birthdays
            )
        }
        val otherDataFlow = combine(
            taskDao.observeAll(),
            moneyExpenseDao.observeAll(),
            weightEntryDao.observeAll(),
            moodEntryDao.observeAll()
        ) { tasks, moneyEntries, weightEntries, moodEntries ->
            GlobalMonthOtherData(
                tasks = tasks,
                moneyEntries = moneyEntries,
                weightEntries = weightEntries,
                moodEntries = moodEntries
            )
        }
        return combine(habitDataFlow, otherDataFlow) { habitData, otherData ->
            var snapshot: GlobalMonthSnapshot? = null
            measureTimeMillis {
                val today = currentBusinessDate()
                val completedByHabit = habitData.completions.asSequence()
                    .filter { it.completed }
                    .groupBy(keySelector = { it.habitId }, valueTransform = { it.date })
                    .mapValues { (_, dates) -> dates.toSet() }
                val notesByHabitAndDate = habitData.notes.associate { (it.habitId to it.date) to it.note }
                val dayScheduledCount = linkedMapOf<String, Int>()
                val dayCompletionCount = linkedMapOf<String, Int>()
                val dayDetails = linkedMapOf<String, MutableList<GlobalDayDetailSnapshot>>()

                habitData.habits.forEach { habit ->
                    val createdDate = epochMillisToLocalDate(habit.createdAt)
                    val scheduledDates = calculateScheduledDatesInMonth(
                        createdDate = createdDate,
                        month = month,
                        today = today,
                        frequencyTypeValue = habit.frequencyType,
                        frequencyIntervalDays = habit.frequencyIntervalDays,
                        frequencyWeekdays = habit.frequencyWeekdays
                    )
                    val completedDates = completedByHabit[habit.id].orEmpty()
                    scheduledDates.forEach { date ->
                        dayScheduledCount[date] = (dayScheduledCount[date] ?: 0) + 1
                        val isDone = completedDates.contains(date)
                        if (isDone) {
                            dayCompletionCount[date] = (dayCompletionCount[date] ?: 0) + 1
                        }
                        dayDetails.getOrPut(date) { mutableListOf() }
                            .add(
                                GlobalDayDetailSnapshot(
                                    habitId = habit.id,
                                    habitName = habit.name,
                                    isDone = isDone,
                                    note = notesByHabitAndDate[habit.id to date]
                                )
                            )
                    }
                }

                val birthdayNamesByDate = habitData.birthdays
                    .groupBy { birthdayOccurrenceInYear(it.month, it.day, month.year).toString() }
                    .mapValues { (_, items) ->
                        items.map { it.name }.sortedBy { it.lowercase() }
                    }

                val completedTasksByDate = otherData.tasks.asSequence()
                    .mapNotNull { task ->
                        val completedAt = task.completedAt ?: return@mapNotNull null
                        val date = Instant.ofEpochMilli(completedAt)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        if (date.isBefore(startLocalDate) || date.isAfter(endLocalDate)) return@mapNotNull null
                        date.toString() to task.title
                    }
                    .groupBy(keySelector = { it.first }, valueTransform = { it.second })
                    .mapValues { (_, titles) -> titles.sortedBy { it.lowercase() } }

                val moneyEntriesByDate = otherData.moneyEntries.asSequence()
                    .mapNotNull { entry ->
                        val date = Instant.ofEpochMilli(entry.paidAt)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        if (date.isBefore(startLocalDate) || date.isAfter(endLocalDate)) return@mapNotNull null
                        date.toString() to GlobalMoneyEntrySnapshot(
                            title = entry.title,
                            category = entry.category,
                            amount = entry.amount,
                            isIncome = entry.isIncome
                        )
                    }
                    .groupBy(keySelector = { it.first }, valueTransform = { it.second })

                val weightByDate = otherData.weightEntries.asSequence()
                    .mapNotNull { entry ->
                        val date = runCatching { LocalDate.parse(entry.date) }.getOrNull() ?: return@mapNotNull null
                        if (date.isBefore(startLocalDate) || date.isAfter(endLocalDate)) return@mapNotNull null
                        date.toString() to entry.weightKg
                    }
                    .toMap()

                val moodByDate = otherData.moodEntries.asSequence()
                    .mapNotNull { entry ->
                        val date = runCatching { LocalDate.parse(entry.date) }.getOrNull() ?: return@mapNotNull null
                        if (date.isBefore(startLocalDate) || date.isAfter(endLocalDate)) return@mapNotNull null
                        date.toString() to GlobalMoodSnapshot(
                            mood = entry.mood,
                            note = entry.note.takeIf { it.isNotBlank() }
                        )
                    }
                    .toMap()

                snapshot = GlobalMonthSnapshot(
                    month = month,
                    globalCompletedDates = dayScheduledCount.keys.filter { date ->
                        val scheduledCount = dayScheduledCount[date] ?: 0
                        scheduledCount > 0 && (dayCompletionCount[date] ?: 0) == scheduledCount
                    }.toSet(),
                    globalScheduledDates = dayScheduledCount.keys.toSet(),
                    globalBirthdayDates = birthdayNamesByDate.keys,
                    globalNoteDates = habitData.notes.asSequence().map { it.date }.toSet(),
                    birthdayNamesByDate = birthdayNamesByDate,
                    dayDetailsByDate = dayDetails.mapValues { (_, items) ->
                        items.sortedBy { it.habitName.lowercase() }
                    },
                    completedTasksByDate = completedTasksByDate,
                    moneyEntriesByDate = moneyEntriesByDate,
                    weightByDate = weightByDate,
                    moodByDate = moodByDate,
                    businessToday = today
                )
            }
            snapshot ?: GlobalMonthSnapshot(
                month = month,
                globalCompletedDates = emptySet(),
                globalScheduledDates = emptySet(),
                globalBirthdayDates = emptySet(),
                globalNoteDates = emptySet(),
                birthdayNamesByDate = emptyMap(),
                dayDetailsByDate = emptyMap(),
                completedTasksByDate = emptyMap(),
                moneyEntriesByDate = emptyMap(),
                weightByDate = emptyMap(),
                moodByDate = emptyMap(),
                businessToday = currentBusinessDate()
            )
        }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
    }

    fun observePersistedHomeMonthSnapshot(month: YearMonth): Flow<HomeMonthSnapshotState?> =
        homeMonthSnapshotDao.observeSnapshot(month.toString()).map { snapshot ->
            snapshot?.toState()?.also { cacheHomeMonthSnapshot(it) }
        }.distinctUntilChanged()

    fun peekPersistedHomeMonthSnapshot(month: YearMonth): HomeMonthSnapshotState? =
        appPreferences
            .getString(homeSnapshotPrefKey(month), null)
            ?.takeIf { it.isNotBlank() }
            ?.let(::deserializeCachedSnapshot)

    suspend fun persistHomeMonthSnapshot(snapshot: HomeMonthSnapshotState) {
        homeMonthSnapshotDao.upsert(snapshot.toEntity())
        cacheHomeMonthSnapshot(snapshot)
    }

    suspend fun refreshHomeMonthSnapshot(
        month: YearMonth = YearMonth.from(currentBusinessDate()),
        selectedHabitId: Long? = null
    ) {
        val snapshot = buildHomeMonthSnapshot(
            month = month,
            selectedHabitId = selectedHabitId
        )
        persistHomeMonthSnapshot(snapshot)
    }

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
        refreshHomeMonthSnapshot()
    }

    suspend fun seedTestData() {
        val today = currentBusinessDate()
        val nowMillis = System.currentTimeMillis()
        val seededHabitIds = mutableListOf<Long>()

        repeat(12) { index ->
            val createdDate = today.minusMonths(6).plusDays(index.toLong() * 4)
            val habitId = habitDao.insertHabit(
                Habit(
                    name = "Demo Daily Habit ${index + 1}",
                    createdAt = localDateToEpochMillis(createdDate),
                    sortOrder = habitDao.getMaxSortOrder() + 1,
                    frequencyType = HabitFrequencyType.DAILY.name,
                    reminderEnabled = index % 2 == 0,
                    reminderTime = if (index % 2 == 0) "07:${(index % 6) * 10}".padEnd(5, '0') else null,
                    reminderMessage = if (index % 2 == 0) "Daily block ${index + 1}" else null
                )
            )
            seededHabitIds += habitId

            calculateScheduledDates(
                createdDate = createdDate,
                untilDate = today,
                frequencyTypeValue = HabitFrequencyType.DAILY.name,
                frequencyIntervalDays = null,
                frequencyWeekdays = null
            ).sorted().takeLast(120).forEachIndexed { completionIndex, date ->
                completionDao.upsertCompletion(
                    HabitCompletion(
                        habitId = habitId,
                        date = date,
                        completed = (completionIndex + index) % 6 != 2
                    )
                )
            }

            listOf(1L, 9L, 24L, 41L).forEach { offset ->
                val noteDate = today.minusDays(offset + index)
                if (!noteDate.isBefore(createdDate)) {
                    habitDayNoteDao.upsert(
                        HabitDayNote(
                            habitId = habitId,
                            date = noteDate.toString(),
                            note = "Demo note ${index + 1} on ${noteDate.dayOfWeek.name.lowercase()}."
                        )
                    )
                }
            }
        }

        val weeklyPatterns = listOf("1,3,5", "2,4", "6,7", "1,4", "3,6")
        repeat(10) { index ->
            val weekdays = weeklyPatterns[index % weeklyPatterns.size]
            val createdDate = today.minusMonths(5).plusDays((index * 5).toLong())
            val habitId = habitDao.insertHabit(
                Habit(
                    name = "Demo Weekly Habit ${index + 1}",
                    createdAt = localDateToEpochMillis(createdDate),
                    sortOrder = habitDao.getMaxSortOrder() + 1,
                    frequencyType = HabitFrequencyType.WEEKLY.name,
                    frequencyWeekdays = weekdays,
                    reminderEnabled = true,
                    reminderTime = listOf("08:15", "12:30", "18:45")[index % 3],
                    reminderMessage = "Weekly pattern $weekdays"
                )
            )
            seededHabitIds += habitId

            calculateScheduledDates(
                createdDate = createdDate,
                untilDate = today,
                frequencyTypeValue = HabitFrequencyType.WEEKLY.name,
                frequencyIntervalDays = null,
                frequencyWeekdays = weekdays
            ).sorted().takeLast(80).forEachIndexed { completionIndex, date ->
                completionDao.upsertCompletion(
                    HabitCompletion(
                        habitId = habitId,
                        date = date,
                        completed = (completionIndex + index) % 4 != 1
                    )
                )
            }
        }

        repeat(8) { index ->
            val interval = (index % 4) + 2
            val createdDate = today.minusMonths(4).plusDays((index * 6).toLong())
            val habitId = habitDao.insertHabit(
                Habit(
                    name = "Demo Every-$interval-Days Habit ${index + 1}",
                    createdAt = localDateToEpochMillis(createdDate),
                    sortOrder = habitDao.getMaxSortOrder() + 1,
                    frequencyType = HabitFrequencyType.EVERY_N_DAYS.name,
                    frequencyIntervalDays = interval,
                    reminderEnabled = index % 3 == 0,
                    reminderTime = if (index % 3 == 0) "20:00" else null,
                    reminderMessage = if (index % 3 == 0) "Interval cadence $interval" else null
                )
            )
            seededHabitIds += habitId

            calculateScheduledDates(
                createdDate = createdDate,
                untilDate = today,
                frequencyTypeValue = HabitFrequencyType.EVERY_N_DAYS.name,
                frequencyIntervalDays = interval,
                frequencyWeekdays = null
            ).sorted().takeLast(60).forEachIndexed { completionIndex, date ->
                completionDao.upsertCompletion(
                    HabitCompletion(
                        habitId = habitId,
                        date = date,
                        completed = (completionIndex + index) % 5 != 0
                    )
                )
            }
        }

        repeat(25) { index ->
            val noteId = whoAmINoteDao.insert(
                WhoAmINote(
                    title = "Demo Note ${index + 1}",
                    content = "Reflection ${index + 1}: momentum improves when the next action is obvious.",
                    sortOrder = whoAmINoteDao.getMaxSortOrder() + 1,
                    createdAt = nowMillis - index * 10_000L
                )
            )
            if (index % 5 == 0) {
                whoAmINoteDao.updateContent(
                    noteId = noteId,
                    content = "Reflection ${index + 1}: momentum improves when the next action is obvious and scheduled."
                )
            }
        }

        val categories = listOf("Work", "Home", "Health", "Finance", "Learning", "Errands", "Social")
        categories.forEach { addTaskCategory(it) }

        repeat(70) { index ->
            val isDone = index % 4 == 0
            val category = categories[index % categories.size]
            val hasOneTimeReminder = index % 3 == 0
            val reminderEnabled = index % 2 == 0
            val reminderDateTimesCsv = if (reminderEnabled && hasOneTimeReminder) {
                listOf(
                    nowMillis + (index + 2) * 60L * 60L * 1000L,
                    nowMillis + (index + 30) * 60L * 60L * 1000L
                ).joinToString("|")
            } else {
                null
            }

            taskDao.insert(
                TaskItem(
                    title = "Demo Task ${index + 1}",
                    category = if (index % 9 == 0) DEFAULT_TASK_CATEGORY else category,
                    isDone = isDone,
                    completedAt = if (isDone) nowMillis - index * 3_600_000L else null,
                    sortOrder = taskDao.getMaxSortOrderForDoneState(isDone) + 1,
                    createdAt = nowMillis - index * 7_200_000L,
                    reminderEnabled = reminderEnabled,
                    reminderTime = if (reminderEnabled && !hasOneTimeReminder) listOf("09:15", "13:45", "20:30")[index % 3] else null,
                    reminderDateTimesCsv = reminderDateTimesCsv,
                    reminderMessage = if (reminderEnabled) "Demo reminder for task ${index + 1}" else null
                )
            )
        }

        repeat(18) { index ->
            val goalDone = index % 3 == 0
            val goalId = goalDao.insert(
                Goal(
                    title = "Demo Goal ${index + 1}",
                    isDone = goalDone,
                    completedAt = if (goalDone) nowMillis - index * 86_400_000L else null,
                    sortOrder = goalDao.getMaxSortOrderForDoneState(goalDone) + 1,
                    createdAt = nowMillis - index * 172_800_000L
                )
            )
            repeat(4) { subIndex ->
                val subDone = goalDone || subIndex < 2 || (index + subIndex) % 4 == 0
                subGoalDao.insert(
                    SubGoal(
                        goalId = goalId,
                        title = "Goal ${index + 1} step ${subIndex + 1}",
                        isDone = subDone,
                        completedAt = if (subDone) nowMillis - (index + subIndex) * 43_200_000L else null,
                        createdAt = nowMillis - (index * 4L + subIndex) * 21_600_000L
                    )
                )
            }
        }

        repeat(24) { index ->
            val birthMonthDate = today.minusMonths((index % 12).toLong()).plusDays((index % 20).toLong())
            val reminderCsv = if (index % 2 == 0) {
                listOf(
                    nowMillis + (index + 4) * 60L * 60L * 1000L,
                    nowMillis + (index + 36) * 60L * 60L * 1000L
                ).joinToString("|")
            } else {
                null
            }
            birthdayDao.insert(
                Birthday(
                    name = "Demo Birthday ${index + 1}",
                    year = 1985 + (index % 20),
                    month = birthMonthDate.monthValue,
                    day = minOf(birthMonthDate.dayOfMonth, YearMonth.of(today.year, birthMonthDate.monthValue).lengthOfMonth()),
                    reminderDateTimesCsv = reminderCsv,
                    createdAt = nowMillis - index * 5_000L
                )
            )
        }

        if (seededHabitIds.isNotEmpty()) {
            val spotlightHabitId = seededHabitIds.first()
            listOf(
                today.minusMonths(2).withDayOfMonth(3),
                today.minusMonths(1).withDayOfMonth(17),
                today.minusDays(2)
            ).forEachIndexed { index, date ->
                if (!date.isBefore(epochMillisToLocalDate(habitDao.getAllHabits().first { it.id == spotlightHabitId }.createdAt))) {
                    habitDayNoteDao.upsert(
                        HabitDayNote(
                            habitId = spotlightHabitId,
                            date = date.toString(),
                            note = "Spotlight note ${index + 1} for historical calendar testing."
                        )
                    )
                }
            }
        }
        refreshHomeMonthSnapshot(selectedHabitId = seededHabitIds.firstOrNull())
    }

    suspend fun reorderHabits(orderedHabitIds: List<Long>) {
        orderedHabitIds.forEachIndexed { index, habitId ->
            habitDao.updateSortOrder(habitId, index)
        }
        refreshHomeMonthSnapshot(selectedHabitId = orderedHabitIds.firstOrNull())
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
        refreshHomeMonthSnapshot(selectedHabitId = habitId)
    }

    suspend fun deleteHabit(habitId: Long) {
        habitDao.deleteHabitById(habitId)
        refreshHomeMonthSnapshot()
    }

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
        refreshHomeMonthSnapshot(selectedHabitId = habitId)
    }

    suspend fun saveHabitDayNote(habitId: Long, date: String, note: String) {
        val sanitized = note.trim()
        val existing = habitDayNoteDao.getNoteForDay(habitId, date)
        if (sanitized.isEmpty()) {
            if (existing != null) habitDayNoteDao.deleteById(existing.id)
            refreshHomeMonthSnapshot(selectedHabitId = habitId)
            return
        }
        habitDayNoteDao.upsert(HabitDayNote(id = existing?.id ?: 0, habitId = habitId, date = date, note = note))
        refreshHomeMonthSnapshot(selectedHabitId = habitId)
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
        subGoalDao.insert(
            SubGoal(
                goalId = goalId,
                title = sanitized,
                isDone = false,
                sortOrder = subGoalDao.getMaxSortOrderForGoal(goalId) + 1,
                createdAt = System.currentTimeMillis()
            )
        )
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

    suspend fun reorderSubGoals(orderedSubGoalIds: List<Long>) {
        orderedSubGoalIds.forEachIndexed { index, subGoalId ->
            subGoalDao.updateSortOrder(subGoalId = subGoalId, sortOrder = index)
        }
    }

    suspend fun addBirthday(name: String, year: Int, month: Int, day: Int): Long {
        val sanitized = name.trim()
        if (sanitized.isEmpty()) return 0L
        if (!isValidDate(year, month, day)) return 0L
        val birthdayId = birthdayDao.insert(
            Birthday(
                name = sanitized,
                year = year,
                month = month,
                day = day,
                reminderDateTimesCsv = null,
                createdAt = System.currentTimeMillis()
            )
        )
        refreshHomeMonthSnapshot()
        return birthdayId
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
        refreshHomeMonthSnapshot()
    }

    suspend fun deleteBirthday(birthdayId: Long) {
        birthdayDao.deleteById(birthdayId)
        refreshHomeMonthSnapshot()
    }

    suspend fun saveMoneySettings(budgetAmount: Double, hourlyWage: Double) {
        moneySettingsDao.upsert(
            MoneySettings(
                budgetAmount = budgetAmount.coerceAtLeast(0.0),
                hourlyWage = hourlyWage.coerceAtLeast(0.0)
            )
        )
    }

    suspend fun addMoneyExpense(
        title: String,
        amount: Double,
        isIncome: Boolean,
        paidAt: Long = System.currentTimeMillis(),
        category: String = "General"
    ) {
        val sanitized = title.trim()
        val sanitizedCategory = category.trim().ifBlank { "General" }
        if (sanitized.isEmpty() || amount <= 0.0) return
        moneyExpenseDao.insert(
            MoneyExpense(
                title = sanitized,
                category = sanitizedCategory,
                amount = amount,
                isIncome = isIncome,
                paidAt = paidAt,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteMoneyExpense(expenseId: Long) {
        moneyExpenseDao.deleteById(expenseId)
    }

    suspend fun replaceMoneyCategory(categoryName: String, replacementCategory: String = "General") {
        val source = categoryName.trim()
        val target = replacementCategory.trim().ifBlank { "General" }
        if (source.isBlank() || source == target || source == "General") return
        moneyExpenseDao.replaceCategory(source, target)
    }

    suspend fun saveWeightEntry(date: LocalDate, weightKg: Double) {
        if (weightKg <= 0.0) return
        weightEntryDao.upsert(
            WeightEntry(
                date = date.toString(),
                weightKg = weightKg,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteWeightEntry(date: String) {
        weightEntryDao.deleteByDate(date)
    }

    suspend fun saveMoodEntry(date: LocalDate, mood: String, note: String) {
        val sanitizedMood = mood.trim()
        if (sanitizedMood.isEmpty()) return
        moodEntryDao.upsert(
            MoodEntry(
                date = date.toString(),
                mood = sanitizedMood,
                note = note.trim(),
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteMoodEntry(date: String) {
        moodEntryDao.deleteByDate(date)
    }

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

    suspend fun buildHomeMonthSnapshot(
        month: YearMonth,
        selectedHabitId: Long? = null
    ): HomeMonthSnapshotState {
        val today = currentBusinessDate()
        val habits = habitDao.getAllHabits()
        val effectiveSelectedHabitId = selectedHabitId?.takeIf { candidateId ->
            habits.any { it.id == candidateId }
        } ?: habits.firstOrNull()?.id
        val (startDate, endDate) = monthRange(month)
        val completions = completionDao.getCompletionsInRange(startDate, endDate)
        val notes = habitDayNoteDao.getNotesInRange(startDate, endDate)
        val birthdays = birthdayDao.getAll()

        val completedByHabit = completions.asSequence()
            .filter { it.completed }
            .groupBy(keySelector = { it.habitId }, valueTransform = { it.date })
            .mapValues { (_, dates) -> dates.toSet() }
        val selectedHabit = habits.firstOrNull { it.id == effectiveSelectedHabitId }
        val selectedCreatedDate = selectedHabit?.let { epochMillisToLocalDate(it.createdAt) }
        val selectedScheduledDates = if (selectedHabit == null || selectedCreatedDate == null) {
            emptySet()
        } else {
            calculateScheduledDatesInMonth(
                createdDate = selectedCreatedDate,
                month = month,
                today = today,
                frequencyTypeValue = selectedHabit.frequencyType,
                frequencyIntervalDays = selectedHabit.frequencyIntervalDays,
                frequencyWeekdays = selectedHabit.frequencyWeekdays
            )
        }
        val selectedCompletedDates = completedByHabit[effectiveSelectedHabitId].orEmpty()
        val selectedDayNotes = notes.asSequence()
            .filter { it.habitId == effectiveSelectedHabitId }
            .associate { it.date to it.note }

        val dayScheduledCount = linkedMapOf<String, Int>()
        val dayCompletionCount = linkedMapOf<String, Int>()
        habits.forEach { habit ->
            val createdDate = epochMillisToLocalDate(habit.createdAt)
            val scheduledDates = calculateScheduledDatesInMonth(
                createdDate = createdDate,
                month = month,
                today = today,
                frequencyTypeValue = habit.frequencyType,
                frequencyIntervalDays = habit.frequencyIntervalDays,
                frequencyWeekdays = habit.frequencyWeekdays
            )
            val completedDates = completedByHabit[habit.id].orEmpty()
            scheduledDates.forEach { date ->
                dayScheduledCount[date] = (dayScheduledCount[date] ?: 0) + 1
                if (completedDates.contains(date)) {
                    dayCompletionCount[date] = (dayCompletionCount[date] ?: 0) + 1
                }
            }
        }

        val birthdayDates = birthdays.map {
            birthdayOccurrenceInYear(it.month, it.day, month.year).toString()
        }.toSet()

        return HomeMonthSnapshotState(
            month = month,
            selectedHabitId = effectiveSelectedHabitId,
            selectedCompletedDates = selectedCompletedDates,
            selectedScheduledDates = selectedScheduledDates,
            globalCompletedDates = dayScheduledCount.keys.filter { date ->
                val scheduledCount = dayScheduledCount[date] ?: 0
                scheduledCount > 0 && (dayCompletionCount[date] ?: 0) == scheduledCount
            }.toSet(),
            globalScheduledDates = dayScheduledCount.keys.toSet(),
            globalBirthdayDates = birthdayDates,
            globalNoteDates = notes.asSequence().map { it.date }.toSet(),
            dayNotesByDate = selectedDayNotes,
            selectedHabitCreatedDate = selectedCreatedDate,
            businessToday = today,
            updatedAt = System.currentTimeMillis()
        )
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
        var currentMonth = YearMonth.from(createdDate)
        val finalMonth = YearMonth.from(untilDate)
        val results = linkedSetOf<String>()
        while (!currentMonth.isAfter(finalMonth)) {
            results += calculateScheduledDatesInMonth(
                createdDate = createdDate,
                month = currentMonth,
                today = untilDate,
                frequencyTypeValue = frequencyTypeValue,
                frequencyIntervalDays = frequencyIntervalDays,
                frequencyWeekdays = frequencyWeekdays
            )
            currentMonth = currentMonth.plusMonths(1)
        }
        return results
    }

    fun calculateScheduledDatesInMonth(
        createdDate: LocalDate,
        month: YearMonth,
        today: LocalDate,
        frequencyTypeValue: String,
        frequencyIntervalDays: Int?,
        frequencyWeekdays: String?
    ): Set<String> {
        val monthStart = month.atDay(1)
        val monthEnd = month.atEndOfMonth()
        val rangeStart = maxOf(createdDate, monthStart)
        val rangeEnd = minOf(monthEnd, today)
        if (rangeEnd.isBefore(rangeStart)) return emptySet()

        return when (parseFrequencyType(frequencyTypeValue)) {
            HabitFrequencyType.DAILY -> generateDateRange(rangeStart, rangeEnd).map(LocalDate::toString).toSet()
            HabitFrequencyType.WEEKLY -> {
                val weekdays = parseWeekdaySet(frequencyWeekdays)
                if (weekdays.isEmpty()) {
                    emptySet()
                } else {
                    generateDateRange(rangeStart, rangeEnd)
                        .filter { weekdays.contains(it.dayOfWeek.value) }
                        .map(LocalDate::toString)
                        .toSet()
                }
            }
            HabitFrequencyType.EVERY_N_DAYS -> {
                val interval = normalizeFrequencyInterval(HabitFrequencyType.EVERY_N_DAYS.name, frequencyIntervalDays) ?: 1
                val daysFromCreated = rangeStart.toEpochDay() - createdDate.toEpochDay()
                val remainder = Math.floorMod(daysFromCreated, interval.toLong())
                val offset = if (remainder == 0L) 0L else interval - remainder
                val firstDate = rangeStart.plusDays(offset)
                if (firstDate.isAfter(rangeEnd)) {
                    emptySet()
                } else {
                    generateSequence(firstDate) { it.plusDays(interval.toLong()) }
                        .takeWhile { !it.isAfter(rangeEnd) }
                        .map(LocalDate::toString)
                        .toSet()
                }
            }
        }
    }

    private fun monthRange(month: YearMonth): Pair<String, String> =
        month.atDay(1).toString() to month.atEndOfMonth().toString()

    private fun generateDateRange(start: LocalDate, end: LocalDate): Sequence<LocalDate> =
        generateSequence(start) { current ->
            current.plusDays(1).takeUnless { it.isAfter(end) }
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

    private fun HomeMonthSnapshotState.toEntity(): HomeMonthSnapshot =
        HomeMonthSnapshot(
            monthKey = month.toString(),
            selectedHabitId = selectedHabitId,
            selectedCompletedDates = serializeStringSet(selectedCompletedDates),
            selectedScheduledDates = serializeStringSet(selectedScheduledDates),
            globalCompletedDates = serializeStringSet(globalCompletedDates),
            globalScheduledDates = serializeStringSet(globalScheduledDates),
            globalBirthdayDates = serializeStringSet(globalBirthdayDates),
            globalNoteDates = serializeStringSet(globalNoteDates),
            dayNotesByDate = serializeStringMap(dayNotesByDate),
            selectedHabitCreatedDate = selectedHabitCreatedDate?.toString(),
            businessToday = businessToday.toString(),
            updatedAt = updatedAt
        )

    private fun HomeMonthSnapshot.toState(): HomeMonthSnapshotState =
        HomeMonthSnapshotState(
            month = YearMonth.parse(monthKey),
            selectedHabitId = selectedHabitId,
            selectedCompletedDates = deserializeStringSet(selectedCompletedDates),
            selectedScheduledDates = deserializeStringSet(selectedScheduledDates),
            globalCompletedDates = deserializeStringSet(globalCompletedDates),
            globalScheduledDates = deserializeStringSet(globalScheduledDates),
            globalBirthdayDates = deserializeStringSet(globalBirthdayDates),
            globalNoteDates = deserializeStringSet(globalNoteDates),
            dayNotesByDate = deserializeStringMap(dayNotesByDate),
            selectedHabitCreatedDate = selectedHabitCreatedDate?.let(LocalDate::parse),
            businessToday = LocalDate.parse(businessToday),
            updatedAt = updatedAt
        )

    private fun serializeStringSet(values: Set<String>): String =
        values.sorted().joinToString("|") { encodeSnapshotToken(it) }

    private fun deserializeStringSet(value: String): Set<String> =
        if (value.isBlank()) {
            emptySet()
        } else {
            value.split("|").asSequence()
                .mapNotNull { token -> token.takeIf { it.isNotEmpty() }?.let(::decodeSnapshotToken) }
                .toSet()
        }

    private fun serializeStringMap(values: Map<String, String>): String =
        values.toSortedMap().entries.joinToString("|") { (key, content) ->
            "${encodeSnapshotToken(key)}=${encodeSnapshotToken(content)}"
        }

    private fun deserializeStringMap(value: String): Map<String, String> =
        if (value.isBlank()) {
            emptyMap()
        } else {
            value.split("|").asSequence()
                .mapNotNull { entry ->
                    val separator = entry.indexOf('=')
                    if (separator <= 0) return@mapNotNull null
                    val key = decodeSnapshotToken(entry.substring(0, separator))
                    val content = decodeSnapshotToken(entry.substring(separator + 1))
                    key to content
                }
                .toMap()
        }

    private fun encodeSnapshotToken(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.toString())

    private fun decodeSnapshotToken(value: String): String =
        URLDecoder.decode(value, StandardCharsets.UTF_8.toString())

    private fun cacheHabitOptions(options: List<HabitOption>) {
        appPreferences.edit()
            .putString(HABIT_OPTIONS_PREF_KEY, serializeHabitOptions(options))
            .apply()
    }

    private fun serializeHabitOptions(options: List<HabitOption>): String =
        options.joinToString("|") { option ->
            listOf(
                option.id.toString(),
                encodeSnapshotToken(option.name),
                option.createdAt.toString(),
                encodeSnapshotToken(option.frequencyType),
                option.frequencyIntervalDays?.toString().orEmpty(),
                encodeSnapshotToken(option.frequencyWeekdays.orEmpty()),
                option.reminderEnabled.toString(),
                encodeSnapshotToken(option.reminderTime.orEmpty()),
                encodeSnapshotToken(option.reminderMessage.orEmpty())
            ).joinToString("~")
        }

    private fun deserializeHabitOptions(value: String): List<HabitOption> =
        value.split("|").mapNotNull { entry ->
            if (entry.isBlank()) return@mapNotNull null
            val parts = entry.split("~")
            if (parts.size != 9) return@mapNotNull null
            HabitOption(
                id = parts[0].toLongOrNull() ?: return@mapNotNull null,
                name = decodeSnapshotToken(parts[1]),
                createdAt = parts[2].toLongOrNull() ?: return@mapNotNull null,
                frequencyType = decodeSnapshotToken(parts[3]),
                frequencyIntervalDays = parts[4].toIntOrNull(),
                frequencyWeekdays = decodeSnapshotToken(parts[5]).ifBlank { null },
                reminderEnabled = parts[6].toBooleanStrictOrNull() ?: false,
                reminderTime = decodeSnapshotToken(parts[7]).ifBlank { null },
                reminderMessage = decodeSnapshotToken(parts[8]).ifBlank { null }
            )
        }

    private fun cacheHomeMonthSnapshot(snapshot: HomeMonthSnapshotState) {
        appPreferences.edit()
            .putString(homeSnapshotPrefKey(snapshot.month), serializeCachedSnapshot(snapshot))
            .apply()
    }

    private fun homeSnapshotPrefKey(month: YearMonth): String = "$HOME_SNAPSHOT_PREF_PREFIX$month"

    private fun serializeCachedSnapshot(snapshot: HomeMonthSnapshotState): String =
        snapshot.toEntity().let { entity ->
            listOf(
                entity.monthKey,
                entity.selectedHabitId?.toString().orEmpty(),
                entity.selectedCompletedDates,
                entity.selectedScheduledDates,
                entity.globalCompletedDates,
                entity.globalScheduledDates,
                entity.globalBirthdayDates,
                entity.globalNoteDates,
                entity.dayNotesByDate,
                entity.selectedHabitCreatedDate.orEmpty(),
                entity.businessToday,
                entity.updatedAt.toString()
            ).joinToString("\n")
        }

    private fun deserializeCachedSnapshot(value: String): HomeMonthSnapshotState? {
        val parts = value.split('\n')
        if (parts.size != 12) return null
        val entity = HomeMonthSnapshot(
            monthKey = parts[0],
            selectedHabitId = parts[1].toLongOrNull(),
            selectedCompletedDates = parts[2],
            selectedScheduledDates = parts[3],
            globalCompletedDates = parts[4],
            globalScheduledDates = parts[5],
            globalBirthdayDates = parts[6],
            globalNoteDates = parts[7],
            dayNotesByDate = parts[8],
            selectedHabitCreatedDate = parts[9].ifBlank { null },
            businessToday = parts[10],
            updatedAt = parts[11].toLongOrNull() ?: return null
        )
        return runCatching { entity.toState() }.getOrNull()
    }
}
