package com.example.habittracker.ui.viewmodel

import com.example.habittracker.MainDispatcherRule
import com.example.habittracker.data.entity.TaskCategory
import com.example.habittracker.data.entity.TaskItem
import com.example.habittracker.notifications.ReminderScheduler
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<HabitRepository>(relaxed = true)
    private val reminderScheduler = mockk<ReminderScheduler>(relaxed = true)

    @Test
    fun `addTask rejects enabled reminders without parsed reminder times`() = runTest {
        stubTaskFlows()
        every { repository.parseReminderDateTimesCsv(null) } returns emptyList()

        val viewModel = TasksViewModel(repository, reminderScheduler)

        assertFalse(
            viewModel.addTask("Task", "General", reminderEnabled = true, reminderTime = null, reminderDateTimesCsv = null, reminderMessage = null)
        )
        coVerify(exactly = 0) { repository.addTask(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `moveTask updates state immediately and persists reordered ids`() = runTest {
        stubTaskFlows(
            tasks = listOf(
                TaskItem(id = 1L, title = "One", isDone = false, createdAt = 0L),
                TaskItem(id = 2L, title = "Two", isDone = false, createdAt = 0L)
            )
        )

        val viewModel = TasksViewModel(repository, reminderScheduler)
        advanceUntilIdle()

        viewModel.moveTask(2L, isDone = false, direction = -1)
        assertEquals(listOf(2L, 1L), viewModel.uiState.value.pending.map { it.id })

        advanceTimeBy(300)
        advanceUntilIdle()

        coVerify { repository.reorderTasksForDoneState(listOf(2L, 1L)) }
    }

    @Test
    fun `transferTaskToCategory updates existing task`() = runTest {
        stubTaskFlows(
            tasks = listOf(TaskItem(id = 5L, title = "Ship", category = "Inbox", isDone = false, createdAt = 0L))
        )

        val viewModel = TasksViewModel(repository, reminderScheduler)
        advanceUntilIdle()

        assertTrue(viewModel.transferTaskToCategory(5L, "Work"))
        advanceUntilIdle()

        coVerify { repository.addTaskCategory("Work") }
        coVerify { repository.updateTask(5L, "Ship", "Work", false, null, null, null) }
    }

    private fun stubTaskFlows(tasks: List<TaskItem> = emptyList()) {
        every { repository.observeTasks() } returns MutableStateFlow(tasks)
        every { repository.observeTaskCategories() } returns MutableStateFlow(
            listOf(TaskCategory(HabitRepository.DEFAULT_TASK_CATEGORY))
        )
        every { repository.observeReminderScheduleItems() } returns MutableStateFlow(emptyList())
        every { repository.parseReminderDateTimesCsv(any()) } returns emptyList()
    }
}
