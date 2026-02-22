package com.example.habittracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val isDone: Boolean
)

data class TasksUiState(
    val pending: List<TaskUiItem> = emptyList(),
    val done: List<TaskUiItem> = emptyList()
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val repository: HabitRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeTasks().collect { tasks ->
                val mapped = tasks.map { TaskUiItem(id = it.id, title = it.title, isDone = it.isDone) }
                mutableUiState.update {
                    TasksUiState(
                        pending = mapped.filter { task -> !task.isDone },
                        done = mapped.filter { task -> task.isDone }
                    )
                }
            }
        }
    }

    fun addTask(title: String) {
        viewModelScope.launch { repository.addTask(title) }
    }

    fun toggleTask(task: TaskUiItem) {
        viewModelScope.launch { repository.setTaskDone(task.id, !task.isDone) }
    }

    fun deleteTask(task: TaskUiItem) {
        viewModelScope.launch { repository.deleteTask(task.id) }
    }
}
