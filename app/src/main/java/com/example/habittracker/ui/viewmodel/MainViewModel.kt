package com.example.habittracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.notifications.ReminderScheduler
import com.example.habittracker.repository.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HabitOptionUiState(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val frequencyType: String,
    val frequencyIntervalDays: Int?,
    val frequencyWeekdays: String?,
    val reminderEnabled: Boolean,
    val reminderTime: String?,
    val reminderMessage: String?,
    val currentStreak: Int
)

data class MainUiState(
    val habits: List<HabitOptionUiState> = emptyList(),
    val selectedHabitId: Long? = null,
    val selectedMonth: YearMonth,
    val completedDates: Set<String> = emptySet(),
    val scheduledDates: Set<String> = emptySet(),
    val globalCompletedDates: Set<String> = emptySet(),
    val globalScheduledDates: Set<String> = emptySet(),
    val globalCurrentStreak: Int = 0,
    val dayNotesByDate: Map<String, String> = emptyMap(),
    val selectedHabitCreatedDate: LocalDate? = null,
    val businessToday: LocalDate
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val selectedHabitIdFlow = MutableStateFlow<Long?>(null)
    private val selectedMonthFlow = MutableStateFlow(YearMonth.from(habitRepository.currentBusinessDate()))

    private val mutableUiState = MutableStateFlow(
        MainUiState(
            selectedMonth = YearMonth.from(habitRepository.currentBusinessDate()),
            businessToday = habitRepository.currentBusinessDate()
        )
    )
    val uiState: StateFlow<MainUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(habitRepository.observeHabits(), habitRepository.observeCurrentStreaks()) { habitOptions, streaks ->
                val habitUiOptions = habitOptions.map {
                    HabitOptionUiState(
                        id = it.id,
                        name = it.name,
                        createdAt = it.createdAt,
                        frequencyType = it.frequencyType,
                        frequencyIntervalDays = it.frequencyIntervalDays,
                        frequencyWeekdays = it.frequencyWeekdays,
                        reminderEnabled = it.reminderEnabled,
                        reminderTime = it.reminderTime,
                        reminderMessage = it.reminderMessage,
                        currentStreak = streaks[it.id] ?: 0
                    )
                }
                habitUiOptions
            }.collect { habitUiOptions ->
                mutableUiState.update { currentState -> currentState.copy(habits = habitUiOptions) }

                val currentlySelectedHabitId = selectedHabitIdFlow.value
                val selectionStillValid = currentlySelectedHabitId != null &&
                    habitUiOptions.any { it.id == currentlySelectedHabitId }

                if (!selectionStillValid) {
                    selectedHabitIdFlow.value = habitUiOptions.firstOrNull()?.id
                }
            }
        }

        viewModelScope.launch {
            val completionDatesFlow = selectedHabitIdFlow.flatMapLatest { selectedId ->
                if (selectedId == null) flowOf(emptySet()) else habitRepository.observeCompletedDateStrings(selectedId)
            }
            val dayNotesFlow = selectedHabitIdFlow.flatMapLatest { selectedId ->
                if (selectedId == null) flowOf(emptyMap()) else habitRepository.observeHabitDayNotes(selectedId)
            }
            val allCompletionsByHabitFlow = habitRepository.observeAllCompletedDateStringsByHabit()

            combine(
                selectedHabitIdFlow,
                selectedMonthFlow,
                completionDatesFlow,
                dayNotesFlow,
                allCompletionsByHabitFlow
            ) {
                    selectedId,
                    selectedYearMonth,
                    completedDateSet,
                    dayNotesMap,
                    allCompletedDateMap ->
                val currentHabits = mutableUiState.value.habits
                val selectedHabit = currentHabits.firstOrNull { it.id == selectedId }
                val monthStart = selectedYearMonth.atDay(1)
                val monthEnd = selectedYearMonth.atEndOfMonth()
                val today = habitRepository.currentBusinessDate()
                val scheduledDates = selectedHabit?.let {
                    val createdDate = habitRepository.epochMillisToLocalDate(it.createdAt)
                    val untilDate = minOf(monthEnd, today)
                    habitRepository.calculateScheduledDates(
                        createdDate = createdDate,
                        untilDate = untilDate,
                        frequencyTypeValue = it.frequencyType,
                        frequencyIntervalDays = it.frequencyIntervalDays,
                        frequencyWeekdays = it.frequencyWeekdays
                    ).filter { dateText ->
                        runCatching {
                            val day = LocalDate.parse(dateText)
                            !day.isBefore(monthStart) && !day.isAfter(monthEnd)
                        }.getOrDefault(false)
                    }.toSet()
                } ?: emptySet()

                val untilDate = minOf(monthEnd, today)
                val dayCompletionCount = linkedMapOf<String, Int>()
                val dayScheduledCount = linkedMapOf<String, Int>()
                currentHabits.forEach { habit ->
                    val createdDate = habitRepository.epochMillisToLocalDate(habit.createdAt)
                    val scheduled = habitRepository.calculateScheduledDates(
                        createdDate = createdDate,
                        untilDate = untilDate,
                        frequencyTypeValue = habit.frequencyType,
                        frequencyIntervalDays = habit.frequencyIntervalDays,
                        frequencyWeekdays = habit.frequencyWeekdays
                    ).filter { dateText ->
                        runCatching {
                            val day = LocalDate.parse(dateText)
                            !day.isBefore(monthStart) && !day.isAfter(monthEnd)
                        }.getOrDefault(false)
                    }
                    val completed = allCompletedDateMap[habit.id].orEmpty()
                    scheduled.forEach { dateText ->
                        dayScheduledCount[dateText] = (dayScheduledCount[dateText] ?: 0) + 1
                        if (completed.contains(dateText)) {
                            dayCompletionCount[dateText] = (dayCompletionCount[dateText] ?: 0) + 1
                        }
                    }
                }
                val globalScheduledDates = dayScheduledCount.keys
                val globalCompletedDates = globalScheduledDates.filter { dateText ->
                    val scheduledCount = dayScheduledCount[dateText] ?: 0
                    scheduledCount > 0 && (dayCompletionCount[dateText] ?: 0) == scheduledCount
                }.toSet()
                val globalCurrentStreak = calculateGlobalStreak(
                    habits = currentHabits,
                    allCompletedDateMap = allCompletedDateMap,
                    today = today
                )
                MainUiState(
                    habits = currentHabits,
                    selectedHabitId = selectedId,
                    selectedMonth = selectedYearMonth,
                    completedDates = completedDateSet,
                    scheduledDates = scheduledDates,
                    globalCompletedDates = globalCompletedDates,
                    globalScheduledDates = globalScheduledDates.toSet(),
                    globalCurrentStreak = globalCurrentStreak,
                    dayNotesByDate = dayNotesMap,
                    selectedHabitCreatedDate = selectedHabit?.let {
                        habitRepository.epochMillisToLocalDate(it.createdAt)
                    },
                    businessToday = today
                )
            }.collect { combinedState ->
                mutableUiState.update { currentState ->
                    currentState.copy(
                        selectedHabitId = combinedState.selectedHabitId,
                        selectedMonth = combinedState.selectedMonth,
                        completedDates = combinedState.completedDates,
                        scheduledDates = combinedState.scheduledDates,
                        globalCompletedDates = combinedState.globalCompletedDates,
                        globalScheduledDates = combinedState.globalScheduledDates,
                        globalCurrentStreak = combinedState.globalCurrentStreak,
                        dayNotesByDate = combinedState.dayNotesByDate,
                        selectedHabitCreatedDate = combinedState.selectedHabitCreatedDate,
                        businessToday = combinedState.businessToday
                    )
                }
            }
        }

        viewModelScope.launch {
            habitRepository.observeReminderScheduleItems().collect { reminders ->
                reminderScheduler.rescheduleAll(reminders)
            }
        }
    }

    fun addHabit(
        habitName: String,
        createdDateText: String?,
        frequencyType: String,
        frequencyIntervalDays: Int?,
        frequencyWeekdays: Set<Int>,
        reminderEnabled: Boolean,
        reminderTime: String?,
        reminderMessage: String?
    ): Boolean {
        val createdAtMillis = createdDateText
            ?.takeIf { it.isNotBlank() }
            ?.let {
                runCatching {
                    val parsed = LocalDate.parse(it)
                    habitRepository.localDateToEpochMillis(parsed)
                }.getOrNull() ?: return false
            }
        if (reminderEnabled && !habitRepository.isValidTime(reminderTime.orEmpty())) return false

        viewModelScope.launch {
            habitRepository.createHabit(
                name = habitName,
                createdAtMillis = createdAtMillis,
                frequencyTypeValue = frequencyType,
                frequencyIntervalDays = frequencyIntervalDays,
                frequencyWeekdays = frequencyWeekdays.sorted().joinToString(","),
                reminderEnabled = reminderEnabled,
                reminderTime = reminderTime,
                reminderMessage = reminderMessage
            )
        }
        return true
    }

    fun updateSelectedHabit(
        name: String,
        frequencyType: String,
        frequencyIntervalDays: Int?,
        frequencyWeekdays: Set<Int>,
        reminderEnabled: Boolean,
        reminderTime: String?,
        reminderMessage: String?
    ): Boolean {
        val selectedId = selectedHabitIdFlow.value ?: return false
        if (reminderEnabled && !habitRepository.isValidTime(reminderTime.orEmpty())) return false
        viewModelScope.launch {
            habitRepository.updateHabit(
                habitId = selectedId,
                name = name,
                frequencyTypeValue = frequencyType,
                frequencyIntervalDays = frequencyIntervalDays,
                frequencyWeekdays = frequencyWeekdays.sorted().joinToString(","),
                reminderEnabled = reminderEnabled,
                reminderTime = reminderTime,
                reminderMessage = reminderMessage
            )
        }
        return true
    }

    fun selectHabit(habitId: Long) {
        selectedHabitIdFlow.value = habitId
    }

    fun deleteSelectedHabit() {
        val selectedId = selectedHabitIdFlow.value ?: return
        viewModelScope.launch { habitRepository.deleteHabit(selectedId) }
    }

    fun showPreviousMonth() {
        selectedMonthFlow.update { currentMonth -> currentMonth.minusMonths(1) }
    }

    fun showNextMonth() {
        selectedMonthFlow.update { currentMonth -> currentMonth.plusMonths(1) }
    }

    fun toggleDay(date: String) {
        val selectedId = selectedHabitIdFlow.value ?: return
        val uiStateSnapshot = mutableUiState.value
        val selectedHabit = uiStateSnapshot.habits.firstOrNull { it.id == selectedId } ?: return
        val createdDate = habitRepository.epochMillisToLocalDate(selectedHabit.createdAt)
        val targetDate = runCatching { LocalDate.parse(date) }.getOrNull() ?: return
        val today = habitRepository.currentBusinessDate()
        if (targetDate.isBefore(createdDate) || targetDate.isAfter(today)) return
        val scheduled = habitRepository.isScheduledOnDate(
            createdDate = createdDate,
            targetDate = targetDate,
            frequencyTypeValue = selectedHabit.frequencyType,
            frequencyIntervalDays = selectedHabit.frequencyIntervalDays,
            frequencyWeekdays = selectedHabit.frequencyWeekdays
        )
        if (!scheduled) return
        viewModelScope.launch { habitRepository.toggleCompletion(selectedId, date) }
    }

    fun saveDayNote(date: String, note: String) {
        val selectedId = selectedHabitIdFlow.value ?: return
        viewModelScope.launch { habitRepository.saveHabitDayNote(selectedId, date, note) }
    }

    fun parseFrequencyWeekdays(value: String?): Set<Int> {
        if (value.isNullOrBlank()) return emptySet()
        return value.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..7 }
            .toSet()
    }

    private fun calculateGlobalStreak(
        habits: List<HabitOptionUiState>,
        allCompletedDateMap: Map<Long, Set<String>>,
        today: LocalDate
    ): Int {
        if (habits.isEmpty()) return 0
        val createdByHabit = habits.associate { habit ->
            habit.id to habitRepository.epochMillisToLocalDate(habit.createdAt)
        }
        val earliestCreatedDate = createdByHabit.values.minOrNull() ?: return 0

        var streak = 0
        var cursor = today
        while (!cursor.isBefore(earliestCreatedDate)) {
            val scheduledHabits = habits.filter { habit ->
                val createdDate = createdByHabit[habit.id] ?: return@filter false
                habitRepository.isScheduledOnDate(
                    createdDate = createdDate,
                    targetDate = cursor,
                    frequencyTypeValue = habit.frequencyType,
                    frequencyIntervalDays = habit.frequencyIntervalDays,
                    frequencyWeekdays = habit.frequencyWeekdays
                )
            }

            if (scheduledHabits.isEmpty()) {
                cursor = cursor.minusDays(1)
                continue
            }

            val allCompleted = scheduledHabits.all { habit ->
                allCompletedDateMap[habit.id].orEmpty().contains(cursor.toString())
            }
            if (!allCompleted) break

            streak += 1
            cursor = cursor.minusDays(1)
        }

        return streak
    }
}
