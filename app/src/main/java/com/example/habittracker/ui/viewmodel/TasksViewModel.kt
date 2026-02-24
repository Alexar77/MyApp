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

data class TaskUiItem(
    val id: Long,
    val title: String,
    val isDone: Boolean,
    val reminderEnabled: Boolean,
    val reminderTime: String?,
    val reminderMessage: String?
)

data class TasksUiState(
    val pending: List<TaskUiItem> = emptyList(),
    val done: List<TaskUiItem> = emptyList()
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val repository: HabitRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeTasks().collect { tasks ->
                val mapped = tasks.map {
                    TaskUiItem(
                        id = it.id,
                        title = it.title,
                        isDone = it.isDone,
                        reminderEnabled = it.reminderEnabled,
                        reminderTime = it.reminderTime,
                        reminderMessage = it.reminderMessage
                    )
                }
                mutableUiState.update {
                    TasksUiState(
                        pending = mapped.filter { task -> !task.isDone },
                        done = mapped.filter { task -> task.isDone }
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.observeReminderScheduleItems().collect { reminders ->
                reminderScheduler.rescheduleAll(reminders)
            }
        }
    }

    fun addTask(
        title: String,
        reminderEnabled: Boolean,
        reminderTime: String?,
        reminderMessage: String?
    ): Boolean {
        if (reminderEnabled && !repository.isValidTime(reminderTime.orEmpty())) return false
        viewModelScope.launch { repository.addTask(title, reminderEnabled, reminderTime, reminderMessage) }
        return true
    }

    fun updateTask(
        taskId: Long,
        title: String,
        reminderEnabled: Boolean,
        reminderTime: String?,
        reminderMessage: String?
    ): Boolean {
        if (reminderEnabled && !repository.isValidTime(reminderTime.orEmpty())) return false
        viewModelScope.launch {
            repository.updateTask(taskId, title, reminderEnabled, reminderTime, reminderMessage)
        }
        return true
    }

    fun toggleTask(task: TaskUiItem) {
        viewModelScope.launch { repository.setTaskDone(task.id, !task.isDone) }
    }

    fun deleteTask(task: TaskUiItem) {
        viewModelScope.launch { repository.deleteTask(task.id) }
    }

    fun moveTask(taskId: Long, isDone: Boolean, direction: Int) {
        val sourceList = if (isDone) mutableUiState.value.done else mutableUiState.value.pending
        val fromIndex = sourceList.indexOfFirst { it.id == taskId }
        if (fromIndex == -1) return

        val toIndex = (fromIndex + direction).coerceIn(0, sourceList.lastIndex)
        if (toIndex == fromIndex) return

        val reordered = sourceList.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }

        viewModelScope.launch {
            repository.reorderTasksForDoneState(reordered.map { it.id })
        }
    }
}
