package com.example.habittracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.repository.HabitRepository
import com.example.habittracker.ui.components.ChartTimeRange
import com.example.habittracker.ui.components.TrendPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MoodEntryUiItem(
    val date: String,
    val mood: String,
    val score: Int,
    val note: String
)

data class MotivationUiState(
    val entries: List<MoodEntryUiItem> = emptyList()
)

@HiltViewModel
class MotivationViewModel @Inject constructor(
    private val repository: HabitRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(MotivationUiState())
    val uiState: StateFlow<MotivationUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeMoodEntries().collect { entries ->
                mutableUiState.update {
                    MotivationUiState(
                        entries = entries
                            .sortedBy { entry -> entry.date }
                            .map { entry ->
                                MoodEntryUiItem(
                                    date = entry.date,
                                    mood = entry.mood,
                                    score = moodScore(entry.mood),
                                    note = entry.note
                                )
                            }
                    )
                }
            }
        }
    }

    fun saveMood(date: LocalDate, mood: String, note: String) {
        viewModelScope.launch {
            repository.saveMoodEntry(date, mood, note)
        }
    }

    fun deleteMood(date: String) {
        viewModelScope.launch {
            repository.deleteMoodEntry(date)
        }
    }

    fun formatDate(date: String): String =
        LocalDate.parse(date).format(DateTimeFormatter.ofPattern("dd MMM yyyy"))

    fun moodOptions(): List<String> = listOf("Great", "Good", "Okay", "Low", "Awful")

    fun buildTrendPoints(
        entries: List<MoodEntryUiItem>,
        selectedRange: ChartTimeRange,
        customStart: String,
        customEnd: String
    ): List<TrendPoint> {
        if (entries.isEmpty()) return emptyList()
        val today = LocalDate.now()
        val range = resolveDateRange(selectedRange, customStart, customEnd, today)
        return entries
            .filter { item ->
                val date = LocalDate.parse(item.date)
                !date.isBefore(range.first) && !date.isAfter(range.second)
            }
            .map { item ->
                TrendPoint(
                    label = LocalDate.parse(item.date).format(DateTimeFormatter.ofPattern("dd MMM")),
                    value = item.score.toDouble()
                )
            }
    }

    private fun moodScore(mood: String): Int = when (mood.trim().lowercase()) {
        "great" -> 5
        "good" -> 4
        "okay" -> 3
        "low" -> 2
        "awful" -> 1
        else -> 3
    }

    private fun resolveDateRange(
        selectedRange: ChartTimeRange,
        customStart: String,
        customEnd: String,
        today: LocalDate
    ): Pair<LocalDate, LocalDate> {
        val defaultStart = today.minusMonths(1)
        return when (selectedRange) {
            ChartTimeRange.WEEKLY -> today.minusDays(6) to today
            ChartTimeRange.MONTHLY -> today.minusMonths(1).plusDays(1) to today
            ChartTimeRange.YEARLY -> today.minusYears(1).plusDays(1) to today
            ChartTimeRange.CUSTOM -> {
                val first = runCatching { LocalDate.parse(customStart) }.getOrNull() ?: defaultStart
                val second = runCatching { LocalDate.parse(customEnd) }.getOrNull() ?: today
                if (first <= second) first to second else second to first
            }
        }
    }
}
