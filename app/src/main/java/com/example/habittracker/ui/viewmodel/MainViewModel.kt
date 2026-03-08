package com.example.habittracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.notifications.ReminderScheduler
import com.example.habittracker.repository.HabitRepository
import com.example.habittracker.util.DebugLog
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.shareIn
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
    val globalBirthdayDates: Set<String> = emptySet(),
    val globalNoteDates: Set<String> = emptySet(),
    val globalCurrentStreak: Int = 0,
    val dayNotesByDate: Map<String, String> = emptyMap(),
    val selectedHabitCreatedDate: LocalDate? = null,
    val businessToday: LocalDate,
    val isDataLoaded: Boolean = false
)

data class GlobalHabitDayDetail(
    val habitName: String,
    val isDone: Boolean,
    val note: String?
)

data class GlobalDayDetails(
    val date: String,
    val habits: List<GlobalHabitDayDetail>,
    val birthdayNames: List<String>
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {
    private val logTag = "MainVM"

    private val selectedHabitIdFlow = MutableStateFlow<Long?>(null)
    private val selectedMonthFlow = MutableStateFlow(YearMonth.from(habitRepository.currentBusinessDate()))
    private val habitsStateFlow = MutableStateFlow<List<HabitOptionUiState>>(emptyList())

    private val mutableUiState = MutableStateFlow(
        MainUiState(
            selectedMonth = YearMonth.from(habitRepository.currentBusinessDate()),
            businessToday = habitRepository.currentBusinessDate()
        )
    )
    val uiState: StateFlow<MainUiState> = mutableUiState.asStateFlow()
    @Volatile
    private var globalDayDetailsCache: Map<String, List<HabitRepository.GlobalDayDetailSnapshot>> = emptyMap()
    @Volatile
    private var globalBirthdayNamesByDateCache: Map<String, List<String>> = emptyMap()
    @Volatile
    private var hasReceivedHabitSnapshot = false
    @Volatile
    private var pendingHabitOrderIds: List<Long>? = null
    private var habitReorderJob: Job? = null

    init {
        val habitsFlow = combine(
            habitRepository.observeHabits(),
            habitRepository.observeCurrentStreaks()
        ) { habitOptions, streaks ->
            habitOptions.map {
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
        }.shareIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            replay = 1
        )

        viewModelScope.launch {
            habitsFlow.collect { habitUiOptions ->
                val orderedHabitUiOptions = pendingHabitOrderIds?.let { pendingOrder ->
                    val byId = habitUiOptions.associateBy { it.id }
                    val reordered = pendingOrder.mapNotNull(byId::get)
                    if (reordered.size == habitUiOptions.size) reordered else habitUiOptions
                } ?: habitUiOptions
                hasReceivedHabitSnapshot = true
                habitsStateFlow.value = orderedHabitUiOptions
                val currentlySelectedHabitId = selectedHabitIdFlow.value
                val selectionStillValid = currentlySelectedHabitId != null &&
                    orderedHabitUiOptions.any { it.id == currentlySelectedHabitId }
                val resolvedSelectedHabitId = if (selectionStillValid) {
                    currentlySelectedHabitId
                } else {
                    orderedHabitUiOptions.firstOrNull()?.id
                }
                DebugLog.d(
                    logTag,
                    "habit options update count=${orderedHabitUiOptions.size} selected=$currentlySelectedHabitId resolved=$resolvedSelectedHabitId pendingOrder=${pendingHabitOrderIds?.size ?: 0}"
                )
                if (!selectionStillValid) {
                    DebugLog.d(
                        logTag,
                        "selection invalid old=$currentlySelectedHabitId new=$resolvedSelectedHabitId"
                    )
                    selectedHabitIdFlow.value = resolvedSelectedHabitId
                }
            }
        }

        viewModelScope.launch(Dispatchers.Default) {
            val selectedHabitSnapshotFlow = combine(selectedHabitIdFlow, selectedMonthFlow) { selectedId, selectedMonth ->
                selectedId to selectedMonth
            }.flatMapLatest { (selectedId, selectedMonth) ->
                if (selectedId == null) {
                    flowOf(
                        HabitRepository.SelectedHabitMonthSnapshot(
                            habitId = null,
                            month = selectedMonth,
                            completedDates = emptySet(),
                            scheduledDates = emptySet(),
                            dayNotesByDate = emptyMap(),
                            createdDate = null
                        )
                    )
                } else {
                    habitRepository.observeSelectedHabitMonth(selectedId, selectedMonth)
                }
            }

            val globalMonthSnapshotFlow = selectedMonthFlow.flatMapLatest { selectedMonth ->
                habitRepository.observeGlobalMonth(selectedMonth)
            }

            combine(
                habitsStateFlow,
                selectedHabitIdFlow,
                selectedMonthFlow,
                selectedHabitSnapshotFlow,
                globalMonthSnapshotFlow
            ) { habits, selectedHabitId, selectedMonth, selectedSnapshot, globalSnapshot ->
                val businessToday = globalSnapshot.businessToday
                val selectedStateIsReady = selectedHabitId == null || selectedSnapshot.habitId == selectedHabitId
                if (!selectedStateIsReady) {
                    DebugLog.d(
                        logTag,
                        "uiState publish skipped staleSelectedSnapshot month=$selectedMonth selected=$selectedHabitId snapshot=${selectedSnapshot.habitId}"
                    )
                    return@combine null
                }

                val uiState = MainUiState(
                    habits = habits,
                    selectedHabitId = selectedHabitId,
                    selectedMonth = selectedMonth,
                    completedDates = selectedSnapshot.completedDates,
                    scheduledDates = selectedSnapshot.scheduledDates,
                    globalCompletedDates = globalSnapshot.globalCompletedDates,
                    globalScheduledDates = globalSnapshot.globalScheduledDates,
                    globalBirthdayDates = globalSnapshot.globalBirthdayDates,
                    globalNoteDates = globalSnapshot.globalNoteDates,
                    globalCurrentStreak = 0,
                    dayNotesByDate = selectedSnapshot.dayNotesByDate,
                    selectedHabitCreatedDate = selectedSnapshot.createdDate,
                    businessToday = businessToday,
                    isDataLoaded = hasReceivedHabitSnapshot
                )
                uiState to globalSnapshot
            }.collect { combinedResult ->
                if (combinedResult == null) return@collect

                val combinedState = combinedResult.first
                val globalSnapshot = combinedResult.second
                if (combinedState.habits.isNotEmpty() && combinedState.selectedHabitId == null) {
                    DebugLog.d(
                        logTag,
                        "uiState publish skipped missingSelection month=${combinedState.selectedMonth} habits=${combinedState.habits.size}"
                    )
                    return@collect
                }

                globalDayDetailsCache = globalSnapshot.dayDetailsByDate
                globalBirthdayNamesByDateCache = globalSnapshot.birthdayNamesByDate

                DebugLog.d(
                    logTag,
                    "uiState publish month=${combinedState.selectedMonth} selected=${combinedState.selectedHabitId} habits=${combinedState.habits.size} loaded=${combinedState.isDataLoaded}"
                )
                mutableUiState.value = combinedState
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
        DebugLog.d(
            logTag,
            "addHabit requested nameLength=${habitName.length} createdDateText=$createdDateText frequency=$frequencyType weekdays=${frequencyWeekdays.size} reminderEnabled=$reminderEnabled"
        )
        val createdAtMillis = createdDateText
            ?.takeIf { it.isNotBlank() }
            ?.let {
                runCatching {
                    val parsed = LocalDate.parse(it)
                    habitRepository.localDateToEpochMillis(parsed)
                }.getOrNull() ?: run {
                    DebugLog.d(logTag, "addHabit rejected invalid createdDateText=$createdDateText")
                    return false
                }
            }
        if (reminderEnabled && !habitRepository.isValidReminderTimesCsv(reminderTime)) {
            DebugLog.d(logTag, "addHabit rejected invalid reminderTime=$reminderTime")
            return false
        }

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
            rescheduleReminders("addHabit")
            DebugLog.d(logTag, "addHabit completed name=$habitName")
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
        if (reminderEnabled && !habitRepository.isValidReminderTimesCsv(reminderTime)) {
            DebugLog.d(logTag, "updateSelectedHabit rejected invalid reminderTime=$reminderTime selectedId=$selectedId")
            return false
        }
        DebugLog.d(logTag, "updateSelectedHabit selectedId=$selectedId frequency=$frequencyType reminderEnabled=$reminderEnabled")
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
            rescheduleReminders("updateSelectedHabit")
            DebugLog.d(logTag, "updateSelectedHabit completed selectedId=$selectedId")
        }
        return true
    }

    fun selectHabit(habitId: Long) {
        DebugLog.d(logTag, "selectHabit habitId=$habitId")
        selectedHabitIdFlow.value = habitId
    }

    fun moveHabit(habitId: Long, direction: Int) {
        DebugLog.d(logTag, "moveHabit habitId=$habitId direction=$direction")
        val habits = habitsStateFlow.value
        val fromIndex = habits.indexOfFirst { it.id == habitId }
        if (fromIndex == -1) return

        val toIndex = (fromIndex + direction).coerceIn(0, habits.lastIndex)
        if (toIndex == fromIndex) return

        val reordered = habits.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
        pendingHabitOrderIds = reordered.map { it.id }

        // Update UI immediately
        habitsStateFlow.value = reordered
        mutableUiState.update { it.copy(habits = reordered) }

        // Debounce DB write
        habitReorderJob?.cancel()
        habitReorderJob = viewModelScope.launch {
            delay(300)
            DebugLog.d(logTag, "persist habit reorder count=${reordered.size}")
            habitRepository.reorderHabits(reordered.map { it.id })
            pendingHabitOrderIds = null
        }
    }

    fun deleteSelectedHabit() {
        val selectedId = selectedHabitIdFlow.value ?: return
        DebugLog.d(logTag, "deleteSelectedHabit selectedId=$selectedId")
        viewModelScope.launch {
            habitRepository.deleteHabit(selectedId)
            rescheduleReminders("deleteSelectedHabit")
        }
    }

    fun seedTestData() {
        DebugLog.d(logTag, "seedTestData requested")
        viewModelScope.launch {
            habitRepository.seedTestData()
            rescheduleReminders("seedTestData")
            DebugLog.d(logTag, "seedTestData completed")
        }
    }

    fun showPreviousMonth() {
        DebugLog.d(logTag, "showPreviousMonth from=${selectedMonthFlow.value}")
        selectedMonthFlow.update { currentMonth -> currentMonth.minusMonths(1) }
    }

    fun showNextMonth() {
        DebugLog.d(logTag, "showNextMonth from=${selectedMonthFlow.value}")
        selectedMonthFlow.update { currentMonth -> currentMonth.plusMonths(1) }
    }

    fun toggleDay(date: String) {
        val selectedId = selectedHabitIdFlow.value ?: return
        DebugLog.d(logTag, "toggleDay date=$date selectedId=$selectedId")
        val uiStateSnapshot = mutableUiState.value
        val selectedHabit = uiStateSnapshot.habits.firstOrNull { it.id == selectedId } ?: return
        val createdDate = habitRepository.epochMillisToLocalDate(selectedHabit.createdAt)
        val targetDate = runCatching { LocalDate.parse(date) }.getOrNull() ?: return
        val today = habitRepository.currentBusinessDate()
        if (targetDate.isBefore(createdDate) || targetDate.isAfter(today)) {
            DebugLog.d(logTag, "toggleDay rejected outOfRange date=$date createdDate=$createdDate today=$today")
            return
        }
        val scheduled = habitRepository.isScheduledOnDate(
            createdDate = createdDate,
            targetDate = targetDate,
            frequencyTypeValue = selectedHabit.frequencyType,
            frequencyIntervalDays = selectedHabit.frequencyIntervalDays,
            frequencyWeekdays = selectedHabit.frequencyWeekdays
        )
        if (!scheduled) {
            DebugLog.d(logTag, "toggleDay rejected unscheduled date=$date selectedId=$selectedId")
            return
        }
        viewModelScope.launch {
            habitRepository.toggleCompletion(selectedId, date)
            DebugLog.d(logTag, "toggleDay completed date=$date selectedId=$selectedId")
        }
    }

    fun saveDayNote(date: String, note: String) {
        val selectedId = selectedHabitIdFlow.value ?: return
        DebugLog.d(logTag, "saveDayNote selectedId=$selectedId date=$date noteLength=${note.length}")
        viewModelScope.launch { habitRepository.saveHabitDayNote(selectedId, date, note) }
    }

    fun parseFrequencyWeekdays(value: String?): Set<Int> {
        if (value.isNullOrBlank()) return emptySet()
        return value.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..7 }
            .toSet()
    }

    fun parseReminderTimesCsv(value: String?): List<String> = habitRepository.parseReminderTimesCsv(value)

    fun getGlobalDayDetails(date: String): GlobalDayDetails? {
        runCatching { LocalDate.parse(date) }.getOrNull() ?: return null
        val habits = globalDayDetailsCache[date].orEmpty().map { detail ->
            GlobalHabitDayDetail(
                habitName = detail.habitName,
                isDone = detail.isDone,
                note = detail.note
            )
        }
        val birthdays = globalBirthdayNamesByDateCache[date].orEmpty()
        val result = GlobalDayDetails(
            date = date,
            habits = habits,
            birthdayNames = birthdays
        )
        DebugLog.d(logTag, "getGlobalDayDetails date=$date habits=${result.habits.size} birthdays=${result.birthdayNames.size}")
        return result
    }

    private suspend fun rescheduleReminders(reason: String) {
        val reminders = habitRepository.getReminderScheduleItems()
        DebugLog.d(logTag, "rescheduleReminders reason=$reason count=${reminders.size}")
        reminderScheduler.rescheduleAll(reminders)
    }
}
