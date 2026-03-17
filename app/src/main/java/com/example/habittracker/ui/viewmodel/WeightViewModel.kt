package com.example.habittracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.repository.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WeightEntryUiItem(
    val date: String,
    val weightKg: Double
)

data class WeightUiState(
    val entries: List<WeightEntryUiItem> = emptyList(),
    val latestWeightKg: Double? = null,
    val changeFromFirstKg: Double? = null
)

@HiltViewModel
class WeightViewModel @Inject constructor(
    private val repository: HabitRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(WeightUiState())
    val uiState: StateFlow<WeightUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeWeightEntries().collect { entries ->
                val mapped = entries.map { entry ->
                    WeightEntryUiItem(
                        date = entry.date,
                        weightKg = entry.weightKg
                    )
                }
                val latest = mapped.lastOrNull()?.weightKg
                val first = mapped.firstOrNull()?.weightKg
                mutableUiState.update {
                    WeightUiState(
                        entries = mapped,
                        latestWeightKg = latest,
                        changeFromFirstKg = if (latest != null && first != null) latest - first else null
                    )
                }
            }
        }
    }

    fun saveWeight(date: LocalDate, weightKg: Double) {
        viewModelScope.launch {
            repository.saveWeightEntry(date, weightKg)
        }
    }

    fun deleteWeight(date: String) {
        viewModelScope.launch {
            repository.deleteWeightEntry(date)
        }
    }

    fun formatWeight(weightKg: Double): String = String.format("%.1f kg", weightKg)

    fun formatDelta(delta: Double): String =
        if (delta > 0) "+${String.format("%.1f", delta)} kg" else "${String.format("%.1f", delta)} kg"

    fun formatDate(date: String): String =
        LocalDate.parse(date).format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
}
