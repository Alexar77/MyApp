package com.example.habittracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.repository.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SubGoalUiItem(
    val id: Long,
    val goalId: Long,
    val title: String,
    val isDone: Boolean,
    val completedAt: Long?
)

data class GoalUiItem(
    val id: Long,
    val title: String,
    val isDone: Boolean,
    val completedAt: Long?,
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
    private var reorderJob: Job? = null

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
                                completedAt = goal.completedAt,
                                subGoals = goal.subGoals.map { subGoal ->
                                    SubGoalUiItem(
                                        id = subGoal.id,
                                        goalId = subGoal.goalId,
                                        title = subGoal.title,
                                        isDone = subGoal.isDone,
                                        completedAt = subGoal.completedAt
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
        if (nextDone && !canMarkDone) {
            return
        }
        viewModelScope.launch { repository.setGoalDone(goal.id, nextDone) }
    }

    fun deleteGoal(goal: GoalUiItem) {
        viewModelScope.launch { repository.deleteGoal(goal.id) }
    }

    fun renameGoal(goal: GoalUiItem, title: String) {
        viewModelScope.launch { repository.renameGoal(goal.id, title) }
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

    fun renameSubGoal(subGoal: SubGoalUiItem, title: String) {
        viewModelScope.launch { repository.renameSubGoal(subGoal.id, title) }
    }

    fun moveSubGoal(goalId: Long, subGoalId: Long, direction: Int) {
        val currentGoals = mutableUiState.value.goals
        val goal = currentGoals.firstOrNull { it.id == goalId } ?: return
        val fromIndex = goal.subGoals.indexOfFirst { it.id == subGoalId }
        if (fromIndex == -1) return
        val toIndex = (fromIndex + direction).coerceIn(0, goal.subGoals.lastIndex)
        if (toIndex == fromIndex) return

        val reorderedSubGoals = goal.subGoals.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
        val reorderedGoals = currentGoals.map { currentGoal ->
            if (currentGoal.id == goalId) currentGoal.copy(subGoals = reorderedSubGoals) else currentGoal
        }
        mutableUiState.update { it.copy(goals = reorderedGoals) }

        reorderJob?.cancel()
        reorderJob = viewModelScope.launch {
            delay(300)
            repository.reorderSubGoals(reorderedSubGoals.map { it.id })
        }
    }

    fun moveGoal(goalId: Long, isDone: Boolean, direction: Int) {
        val currentGoals = mutableUiState.value.goals
        val sourceList = currentGoals.filter { it.isDone == isDone }
        val fromIndex = sourceList.indexOfFirst { it.id == goalId }
        if (fromIndex == -1) return

        val toIndex = (fromIndex + direction).coerceIn(0, sourceList.lastIndex)
        if (toIndex == fromIndex) return

        val reorderedSubset = sourceList.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }

        val reorderedIds = reorderedSubset.map { it.id }.iterator()
        val reorderedGoals = currentGoals.map { goal ->
            if (goal.isDone == isDone) {
                val nextId = reorderedIds.next()
                reorderedSubset.first { it.id == nextId }
            } else {
                goal
            }
        }

        mutableUiState.update { it.copy(goals = reorderedGoals) }

        reorderJob?.cancel()
        reorderJob = viewModelScope.launch {
            delay(300)
            repository.reorderGoalsForDoneState(reorderedSubset.map { it.id })
        }
    }
}
