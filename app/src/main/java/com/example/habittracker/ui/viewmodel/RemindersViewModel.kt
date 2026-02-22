package com.example.habittracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.notifications.ReminderScheduler
import com.example.habittracker.repository.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReminderTimeUi(
    val id: Long,
    val timeValue: String
)

data class RemindersUiState(
    val habitsEnabled: Boolean = true,
    val tasksEnabled: Boolean = true,
    val times: List<ReminderTimeUi> = emptyList()
)

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val repository: HabitRepository,
    private val scheduler: ReminderScheduler
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(RemindersUiState())
    val uiState: StateFlow<RemindersUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeReminderConfig().collect { config ->
                mutableUiState.update {
                    RemindersUiState(
                        habitsEnabled = config.habitsEnabled,
                        tasksEnabled = config.tasksEnabled,
                        times = config.times.map { entry ->
                            ReminderTimeUi(id = entry.id, timeValue = entry.timeValue)
                        }
                    )
                }
                scheduler.rescheduleAll(
                    config.times.map { it.timeValue },
                    config.habitsEnabled,
                    config.tasksEnabled
                )
            }
        }
    }

    fun setHabitsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = repository.getReminderConfig()
            repository.upsertReminderSettings(enabled, current.tasksEnabled)
        }
    }

    fun setTasksEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = repository.getReminderConfig()
            repository.upsertReminderSettings(current.habitsEnabled, enabled)
        }
    }

    fun addTime(timeValue: String): Boolean {
        val valid = Regex("^([01]\\d|2[0-3]):[0-5]\\d$").matches(timeValue)
        if (!valid) return false
        viewModelScope.launch { repository.addReminderTime(timeValue) }
        return true
    }

    fun deleteTime(timeId: Long) {
        viewModelScope.launch { repository.deleteReminderTime(timeId) }
    }
}
