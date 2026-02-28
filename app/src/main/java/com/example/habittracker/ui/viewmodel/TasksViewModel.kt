package com.example.habittracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.notifications.ReminderScheduler
import com.example.habittracker.repository.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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
    private var categoriesCache: List<String> = listOf(HabitRepository.DEFAULT_TASK_CATEGORY)
    private var reorderJob: Job? = null
    private var categoryReorderJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                repository.observeTasks().distinctUntilChanged(),
                repository.observeTaskCategories()
            ) { tasks, categories ->
                Pair(tasks, categories)
            }.collect { (tasks, dbCategories) ->
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
                categoriesCache = dbCategories.map { it.name }
                mutableUiState.update { currentState -> buildUiState(currentState.selectedCategory) }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
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

        // Update UI immediately
        mutableUiState.update { current ->
            if (isDone) current.copy(done = reordered) else current.copy(pending = reordered)
        }

        // Debounce DB write - only persist after dragging settles
        reorderJob?.cancel()
        reorderJob = viewModelScope.launch {
            delay(300)
            repository.reorderTasksForDoneState(reordered.map { it.id })
        }
    }

    fun selectCategory(category: String) {
        mutableUiState.update { buildUiState(category) }
    }

    fun addCategory(category: String): Boolean {
        val sanitized = category.trim()
        if (sanitized.isEmpty() || sanitized == ALL_CATEGORIES) return false
        viewModelScope.launch { repository.addTaskCategory(sanitized) }
        mutableUiState.update { buildUiState(sanitized) }
        return true
    }

    fun deleteCategory(category: String): Boolean {
        val sanitized = category.trim()
        if (sanitized.isEmpty() || sanitized == ALL_CATEGORIES || sanitized == HabitRepository.DEFAULT_TASK_CATEGORY) {
            return false
        }
        viewModelScope.launch {
            repository.deleteAllTasksInCategory(sanitized)
            repository.deleteTaskCategory(sanitized)
        }
        mutableUiState.update { buildUiState(HabitRepository.DEFAULT_TASK_CATEGORY) }
        return true
    }

    fun moveCategory(categoryName: String, direction: Int) {
        val cats = mutableUiState.value.categories
        val fromIndex = cats.indexOf(categoryName)
        if (fromIndex == -1) return

        val toIndex = (fromIndex + direction).coerceIn(0, cats.lastIndex)
        if (toIndex == fromIndex) return

        val reordered = cats.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }

        // Update UI immediately
        categoriesCache = reordered
        mutableUiState.update { it.copy(categories = reordered) }

        // Debounce DB write
        categoryReorderJob?.cancel()
        categoryReorderJob = viewModelScope.launch {
            delay(300)
            repository.reorderTaskCategories(reordered)
        }
    }

    fun transferTaskToCategory(taskId: Long, targetCategory: String): Boolean {
        val sanitizedTarget = targetCategory.trim()
        if (sanitizedTarget.isEmpty() || sanitizedTarget == ALL_CATEGORIES) return false

        val existingTask = allTasksCache.firstOrNull { it.id == taskId } ?: return false
        if (existingTask.category == sanitizedTarget) return true

        viewModelScope.launch {
            repository.addTaskCategory(sanitizedTarget)
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
        val categories = categoriesCache

        val selectedCategory = requestedCategory.takeIf {
            it == ALL_CATEGORIES || categories.contains(it)
        } ?: ALL_CATEGORIES

        val filtered = if (selectedCategory == ALL_CATEGORIES) {
            allTasksCache
        } else {
            allTasksCache.filter { it.category == selectedCategory }
        }

        val (done, pending) = filtered.partition { it.isDone }

        return TasksUiState(
            categories = categories,
            selectedCategory = selectedCategory,
            pending = pending,
            done = done
        )
    }
}
