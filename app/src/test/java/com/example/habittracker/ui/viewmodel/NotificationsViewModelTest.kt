package com.example.habittracker.ui.viewmodel

import com.example.habittracker.MainDispatcherRule
import com.example.habittracker.repository.HabitRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<HabitRepository>(relaxed = true)

    @Test
    fun `ui state keeps only future reminders sorted by trigger time`() = runTest {
        val now = System.currentTimeMillis()
        every { repository.observeReminderScheduleItems() } returns MutableStateFlow(
            listOf(
                HabitRepository.ReminderScheduleItem("a", "A", "m", "00:00", triggerAtMillis = now + 5_000),
                HabitRepository.ReminderScheduleItem("b", "B", "m", "00:00", triggerAtMillis = now - 5_000),
                HabitRepository.ReminderScheduleItem("c", "C", "m", "bad")
            )
        )

        val viewModel = NotificationsViewModel(repository)
        advanceUntilIdle()

        assertEquals(listOf("a"), viewModel.uiState.value.upcoming.map { it.uniqueKey })
    }
}
