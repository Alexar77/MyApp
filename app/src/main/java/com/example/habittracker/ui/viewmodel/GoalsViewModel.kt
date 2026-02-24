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

data class SubGoalUiItem(
    val id: Long,
    val title: String,
    val isDone: Boolean
)

data class GoalUiItem(
    val id: Long,
    val title: String,
    val isDone: Boolean,
    val subGoals: List<SubGoalUiItem>
)

data class GoalsUiState(
    val goals: List<GoalUiItem> = emptyList()
)

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val repository: HabitRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(GoalsUiState())
    val uiState: StateFlow<GoalsUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeGoalsWithSubGoals().collect { goals ->
                mutableUiState.update {
                    GoalsUiState(
                        goals = goals.map { goal ->
                            GoalUiItem(
                                id = goal.goalId,
                                title = goal.title,
                                isDone = goal.isDone,
                                subGoals = goal.subGoals.map { subGoal ->
                                    SubGoalUiItem(
                                        id = subGoal.id,
                                        title = subGoal.title,
                                        isDone = subGoal.isDone
                                    )
                                }
                            )
                        }
                    )
                }
            }
        }
    }

    fun addGoal(title: String) {
        viewModelScope.launch { repository.addGoal(title) }
    }

    fun toggleGoal(goal: GoalUiItem) {
        val canMarkDone = goal.subGoals.all { it.isDone }
        val nextDone = !goal.isDone
        if (nextDone && !canMarkDone) return

        viewModelScope.launch { repository.setGoalDone(goal.id, nextDone) }
    }

    fun deleteGoal(goal: GoalUiItem) {
        viewModelScope.launch { repository.deleteGoal(goal.id) }
    }

    fun addSubGoal(goalId: Long, title: String) {
        viewModelScope.launch { repository.addSubGoal(goalId, title) }
    }

    fun toggleSubGoal(subGoal: SubGoalUiItem) {
        viewModelScope.launch { repository.setSubGoalDone(subGoal.id, !subGoal.isDone) }
    }

    fun deleteSubGoal(subGoal: SubGoalUiItem) {
        viewModelScope.launch { repository.deleteSubGoal(subGoal.id) }
    }

    fun moveGoal(goalId: Long, isDone: Boolean, direction: Int) {
        val sourceList = mutableUiState.value.goals.filter { it.isDone == isDone }
        val fromIndex = sourceList.indexOfFirst { it.id == goalId }
        if (fromIndex == -1) return

        val toIndex = (fromIndex + direction).coerceIn(0, sourceList.lastIndex)
        if (toIndex == fromIndex) return

        val reordered = sourceList.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }

        viewModelScope.launch {
            repository.reorderGoalsForDoneState(reordered.map { it.id })
        }
    }
}
