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
    val category: String,
    val isDone: Boolean,
    val completedAt: Long?,
    val reminderEnabled: Boolean,
    val reminderTime: String?,
    val reminderDateTimesCsv: String?,
    val reminderMessage: String?
)

data class TasksUiState(
    val categories: List<String> = listOf(HabitRepository.DEFAULT_TASK_CATEGORY),
    val selectedCategory: String = ALL_CATEGORIES,
    val pending: List<TaskUiItem> = emptyList(),
    val done: List<TaskUiItem> = emptyList()
)

const val ALL_CATEGORIES = "All"

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val repository: HabitRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = mutableUiState.asStateFlow()
    private var allTasksCache: List<TaskUiItem> = emptyList()
    private val manualCategories = mutableSetOf(HabitRepository.DEFAULT_TASK_CATEGORY)

    init {
        viewModelScope.launch {
            repository.observeTasks().collect { tasks ->
                val mapped = tasks.map {
                    TaskUiItem(
                        id = it.id,
                        title = it.title,
                        category = it.category,
                        isDone = it.isDone,
                        completedAt = it.completedAt,
                        reminderEnabled = it.reminderEnabled,
                        reminderTime = it.reminderTime,
                        reminderDateTimesCsv = it.reminderDateTimesCsv,
                        reminderMessage = it.reminderMessage
                    )
                }
                allTasksCache = mapped
                mutableUiState.update { currentState -> buildUiState(currentState.selectedCategory) }
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
        category: String,
        reminderEnabled: Boolean,
        reminderTime: String?,
        reminderDateTimesCsv: String?,
        reminderMessage: String?
    ): Boolean {
        val hasOneTimeReminders = repository.parseReminderDateTimesCsv(reminderDateTimesCsv).isNotEmpty()
        if (reminderEnabled && !hasOneTimeReminders) return false
        viewModelScope.launch {
            repository.addTask(title, category, reminderEnabled, reminderTime, reminderDateTimesCsv, reminderMessage)
        }
        return true
    }

    fun updateTask(
        taskId: Long,
        title: String,
        category: String,
        reminderEnabled: Boolean,
        reminderTime: String?,
        reminderDateTimesCsv: String?,
        reminderMessage: String?
    ): Boolean {
        val hasOneTimeReminders = repository.parseReminderDateTimesCsv(reminderDateTimesCsv).isNotEmpty()
        if (reminderEnabled && !hasOneTimeReminders) return false
        viewModelScope.launch {
            repository.updateTask(taskId, title, category, reminderEnabled, reminderTime, reminderDateTimesCsv, reminderMessage)
        }
        return true
    }

    fun toggleTask(task: TaskUiItem) {
        viewModelScope.launch { repository.setTaskDone(task.id, !task.isDone) }
    }

    fun deleteTask(task: TaskUiItem) {
        viewModelScope.launch {
            repository.deleteTask(task.id)
            reminderScheduler.rescheduleAll(repository.getReminderScheduleItems())
        }
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

    fun selectCategory(category: String) {
        mutableUiState.update { buildUiState(category) }
    }

    fun addCategory(category: String): Boolean {
        val sanitized = category.trim()
        if (sanitized.isEmpty() || sanitized == ALL_CATEGORIES) return false
        manualCategories.add(sanitized)
        mutableUiState.update { buildUiState(sanitized) }
        return true
    }

    fun deleteCategory(category: String): Boolean {
        val sanitized = category.trim()
        if (sanitized.isEmpty() || sanitized == ALL_CATEGORIES || sanitized == HabitRepository.DEFAULT_TASK_CATEGORY) {
            return false
        }
        manualCategories.remove(sanitized)
        viewModelScope.launch {
            repository.moveAllTasksToCategory(
                fromCategory = sanitized,
                toCategory = HabitRepository.DEFAULT_TASK_CATEGORY
            )
        }
        mutableUiState.update { buildUiState(HabitRepository.DEFAULT_TASK_CATEGORY) }
        return true
    }

    fun transferTaskToCategory(taskId: Long, targetCategory: String): Boolean {
        val sanitizedTarget = targetCategory.trim()
        if (sanitizedTarget.isEmpty() || sanitizedTarget == ALL_CATEGORIES) return false

        val existingTask = allTasksCache.firstOrNull { it.id == taskId } ?: return false
        if (existingTask.category == sanitizedTarget) return true

        manualCategories.add(sanitizedTarget)
        viewModelScope.launch {
            repository.updateTask(
                taskId = existingTask.id,
                title = existingTask.title,
                category = sanitizedTarget,
                reminderEnabled = existingTask.reminderEnabled,
                reminderTime = existingTask.reminderTime,
                reminderDateTimesCsv = existingTask.reminderDateTimesCsv,
                reminderMessage = existingTask.reminderMessage
            )
        }
        return true
    }

    private fun buildUiState(requestedCategory: String): TasksUiState {
        val categories = (allTasksCache.asSequence()
            .map { it.category }
            .filter { it.isNotBlank() }
            .toSet()
            .toMutableSet())
            .apply {
                add(HabitRepository.DEFAULT_TASK_CATEGORY)
                addAll(manualCategories)
            }
            .toList()
            .sortedBy { it.lowercase() }

        val selectedCategory = requestedCategory.takeIf {
            it == ALL_CATEGORIES || categories.contains(it)
        } ?: ALL_CATEGORIES

        val filtered = if (selectedCategory == ALL_CATEGORIES) {
            allTasksCache
        } else {
            allTasksCache.filter { it.category == selectedCategory }
        }

        return TasksUiState(
            categories = categories,
            selectedCategory = selectedCategory,
            pending = filtered.filter { !it.isDone },
            done = filtered.filter { it.isDone }
        )
    }
}
