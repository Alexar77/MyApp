package com.example.habittracker.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.repository.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class StatsUiState(
    val habitId: Long = 0,
    val habitName: String = "",
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalCompletions: Int = 0,
    val recentCompletedDates: Set<String> = emptySet(),
    val businessToday: LocalDate = LocalDate.now()
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: HabitRepository
) : ViewModel() {

    private val habitId: Long = checkNotNull(savedStateHandle["habitId"])

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeHabit(habitId),
                repository.observeStats(habitId),
                repository.observeCompletedDateStrings(habitId)
            ) { habit, stats, completedDates ->
                StatsUiState(
                    habitId = habitId,
                    habitName = habit?.name ?: "Habit",
                    currentStreak = stats.currentStreak,
                    longestStreak = stats.longestStreak,
                    totalCompletions = stats.totalCompletions,
                    recentCompletedDates = completedDates,
                    businessToday = repository.currentBusinessDate()
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}
