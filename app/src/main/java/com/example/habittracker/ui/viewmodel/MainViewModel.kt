package com.example.habittracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val name: String
)

data class MainUiState(
    val habits: List<HabitOptionUiState> = emptyList(),
    val selectedHabitId: Long? = null,
    val selectedMonth: YearMonth,
    val completedDates: Set<String> = emptySet(),
    val dayNotesByDate: Map<String, String> = emptyMap()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val habitRepository: HabitRepository
) : ViewModel() {

    private val selectedHabitIdFlow = MutableStateFlow<Long?>(null)
    private val selectedMonthFlow = MutableStateFlow(YearMonth.from(habitRepository.currentBusinessDate()))

    private val mutableUiState = MutableStateFlow(
        MainUiState(selectedMonth = YearMonth.from(habitRepository.currentBusinessDate()))
    )
    val uiState: StateFlow<MainUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            habitRepository.observeHabits().collect { habitOptions ->
                val habitUiOptions = habitOptions.map { HabitOptionUiState(id = it.id, name = it.name) }
                mutableUiState.update { currentState -> currentState.copy(habits = habitUiOptions) }

                val currentlySelectedHabitId = selectedHabitIdFlow.value
                val selectionStillValid = currentlySelectedHabitId != null &&
                    habitOptions.any { it.id == currentlySelectedHabitId }

                if (!selectionStillValid) {
                    selectedHabitIdFlow.value = habitOptions.firstOrNull()?.id
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

            combine(selectedHabitIdFlow, selectedMonthFlow, completionDatesFlow, dayNotesFlow) {
                    selectedId,
                    selectedYearMonth,
                    completedDateSet,
                    dayNotesMap ->
                MainUiState(
                    habits = mutableUiState.value.habits,
                    selectedHabitId = selectedId,
                    selectedMonth = selectedYearMonth,
                    completedDates = completedDateSet,
                    dayNotesByDate = dayNotesMap
                )
            }.collect { combinedState ->
                mutableUiState.update { currentState ->
                    currentState.copy(
                        selectedHabitId = combinedState.selectedHabitId,
                        selectedMonth = combinedState.selectedMonth,
                        completedDates = combinedState.completedDates,
                        dayNotesByDate = combinedState.dayNotesByDate
                    )
                }
            }
        }
    }

    fun addHabit(habitName: String, createdDateText: String?): Boolean {
        val createdAtMillis = createdDateText
            ?.takeIf { it.isNotBlank() }
            ?.let {
                runCatching {
                    val parsed = LocalDate.parse(it)
                    habitRepository.localDateToEpochMillis(parsed)
                }.getOrNull() ?: return false
            }

        viewModelScope.launch {
            habitRepository.createHabit(habitName, createdAtMillis)
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
        viewModelScope.launch { habitRepository.toggleCompletion(selectedId, date) }
    }

    fun saveDayNote(date: String, note: String) {
        val selectedId = selectedHabitIdFlow.value ?: return
        viewModelScope.launch { habitRepository.saveHabitDayNote(selectedId, date, note) }
    }
}
