package com.example.habittracker.ui.viewmodel

import com.example.habittracker.MainDispatcherRule
import com.example.habittracker.data.entity.SubGoal
import com.example.habittracker.repository.HabitRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GoalsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<HabitRepository>(relaxed = true)

    @Test
    fun `toggleGoal refuses to complete when subgoals remain incomplete`() = runTest {
        every { repository.observeGoalsWithSubGoals() } returns MutableStateFlow(emptyList())

        val viewModel = GoalsViewModel(repository)

        viewModel.toggleGoal(
            GoalUiItem(
                id = 1L,
                title = "Goal",
                isDone = false,
                completedAt = null,
                subGoals = listOf(SubGoalUiItem(2L, "Sub", isDone = false, completedAt = null))
            )
        )
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.setGoalDone(any(), any()) }
    }

    @Test
    fun `toggleGoal completes when all subgoals are done`() = runTest {
        every { repository.observeGoalsWithSubGoals() } returns MutableStateFlow(emptyList())

        val viewModel = GoalsViewModel(repository)

        viewModel.toggleGoal(
            GoalUiItem(
                id = 1L,
                title = "Goal",
                isDone = false,
                completedAt = null,
                subGoals = listOf(SubGoalUiItem(2L, "Sub", isDone = true, completedAt = 1L))
            )
        )
        advanceUntilIdle()

        coVerify { repository.setGoalDone(1L, true) }
    }

    @Test
    fun `moveGoal reorders goals within the same done state`() = runTest {
        every { repository.observeGoalsWithSubGoals() } returns MutableStateFlow(
            listOf(
                HabitRepository.GoalWithSubGoals(1L, "One", false, null, emptyList()),
                HabitRepository.GoalWithSubGoals(2L, "Two", false, null, emptyList()),
                HabitRepository.GoalWithSubGoals(3L, "Done", true, 1L, listOf(SubGoal(goalId = 3L, title = "x", isDone = true, createdAt = 0L)))
            )
        )

        val viewModel = GoalsViewModel(repository)
        advanceUntilIdle()
        assertEquals(listOf(1L, 2L, 3L), viewModel.uiState.value.goals.map { it.id })

        viewModel.moveGoal(2L, isDone = false, direction = -1)
        advanceTimeBy(1)
        advanceUntilIdle()

        coVerify { repository.reorderGoalsForDoneState(listOf(2L, 1L)) }
    }
}
