package com.example.habittracker.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.habittracker.MainDispatcherRule
import com.example.habittracker.data.entity.Habit
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
class StatsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<HabitRepository>(relaxed = true)

    @Test
    fun `ui state combines habit and stats flows`() = runTest {
        every { repository.observeHabit(7L) } returns MutableStateFlow(
            Habit(id = 7L, name = "Meditate", createdAt = 0L)
        )
        every { repository.observeStats(7L) } returns MutableStateFlow(
            HabitRepository.HabitStats(currentStreak = 3, longestStreak = 9, totalCompletions = 12)
        )

        val viewModel = StatsViewModel(SavedStateHandle(mapOf("habitId" to 7L)), repository)
        advanceUntilIdle()

        assertEquals("Meditate", viewModel.uiState.value.habitName)
        assertEquals(3, viewModel.uiState.value.currentStreak)
        assertEquals(9, viewModel.uiState.value.longestStreak)
        assertEquals(12, viewModel.uiState.value.totalCompletions)
    }
}
