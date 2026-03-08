package com.example.habittracker.ui.viewmodel

import com.example.habittracker.MainDispatcherRule
import com.example.habittracker.data.entity.Habit
import com.example.habittracker.notifications.ReminderScheduler
import com.example.habittracker.repository.HabitRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import java.time.LocalDateTime
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
class MainViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<HabitRepository>(relaxed = true)
    private val reminderScheduler = mockk<ReminderScheduler>(relaxed = true)

    @Test
    fun `addHabit returns false for invalid created date`() = runTest {
        stubMainFlows()
        every { repository.currentBusinessDate(any()) } returns LocalDate.of(2026, 3, 7)

        val viewModel = MainViewModel(repository, reminderScheduler)

        val result = viewModel.addHabit(
            habitName = "Run",
            createdDateText = "not-a-date",
            frequencyType = "DAILY",
            frequencyIntervalDays = null,
            frequencyWeekdays = emptySet(),
            reminderEnabled = false,
            reminderTime = null,
            reminderMessage = null
        )

        assertFalse(result)
        coVerify(exactly = 0) { repository.createHabit(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `toggleDay only toggles scheduled dates within allowed range`() = runTest {
        val createdAt = LocalDate.of(2026, 3, 1)
        stubMainFlows(
            habits = listOf(
                Habit(
                    id = 10L,
                    name = "Read",
                    createdAt = repositoryDate(createdAt),
                    frequencyType = "WEEKLY",
                    frequencyWeekdays = "1"
                )
            )
        )
        every { repository.currentBusinessDate(any()) } returns LocalDate.of(2026, 3, 9)
        every { repository.epochMillisToLocalDate(any()) } returns createdAt
        every {
            repository.isScheduledOnDate(
                createdDate = createdAt,
                targetDate = LocalDate.of(2026, 3, 9),
                frequencyTypeValue = "WEEKLY",
                frequencyIntervalDays = null,
                frequencyWeekdays = "1"
            )
        } returns true

        val viewModel = MainViewModel(repository, reminderScheduler)
        advanceUntilIdle()

        viewModel.toggleDay("2026-03-09")
        viewModel.toggleDay("2026-03-10")
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.toggleCompletion(10L, "2026-03-09") }
    }

    @Test
    fun `moveHabit updates UI immediately and persists debounced order`() = runTest {
        stubMainFlows(
            habits = listOf(
                Habit(id = 1L, name = "One", createdAt = repositoryDate(LocalDate.of(2026, 3, 1))),
                Habit(id = 2L, name = "Two", createdAt = repositoryDate(LocalDate.of(2026, 3, 2)))
            )
        )
        every { repository.currentBusinessDate(any()) } returns LocalDate.of(2026, 3, 7)
        every { repository.epochMillisToLocalDate(any()) } returns LocalDate.of(2026, 3, 1)
        every { repository.calculateScheduledDates(any(), any(), any(), any(), any()) } returns emptySet()

        val viewModel = MainViewModel(repository, reminderScheduler)
        advanceUntilIdle()

        viewModel.moveHabit(2L, -1)
        assertEquals(listOf(2L, 1L), viewModel.uiState.value.habits.map { it.id })

        advanceTimeBy(300)
        advanceUntilIdle()

        coVerify { repository.reorderHabits(listOf(2L, 1L)) }
    }

    private fun stubMainFlows(habits: List<Habit> = emptyList()) {
        every { repository.observeHabits() } returns MutableStateFlow(
            habits.map {
                HabitRepository.HabitOption(
                    id = it.id,
                    name = it.name,
                    createdAt = it.createdAt,
                    frequencyType = it.frequencyType,
                    frequencyIntervalDays = it.frequencyIntervalDays,
                    frequencyWeekdays = it.frequencyWeekdays,
                    reminderEnabled = it.reminderEnabled,
                    reminderTime = it.reminderTime,
                    reminderMessage = it.reminderMessage
                )
            }
        )
        every { repository.observeCurrentStreaks() } returns MutableStateFlow(habits.associate { it.id to 0 })
        every { repository.observeCompletedDateStrings(any()) } returns MutableStateFlow<Set<String>>(emptySet())
        every { repository.observeHabitDayNotes(any()) } returns MutableStateFlow<Map<String, String>>(emptyMap())
        every { repository.observeAllCompletedDateStringsByHabit() } returns MutableStateFlow<Map<Long, Set<String>>>(emptyMap())
        every { repository.observeAllHabitDayNotes() } returns MutableStateFlow(emptyList<com.example.habittracker.data.entity.HabitDayNote>())
        every { repository.observeBirthdays() } returns MutableStateFlow(emptyList<HabitRepository.BirthdayOption>())
        every { repository.observeReminderScheduleItems() } returns MutableStateFlow(emptyList<HabitRepository.ReminderScheduleItem>())
        every { repository.observeSelectedHabitMonth(any(), any()) } answers {
            MutableStateFlow(
                HabitRepository.SelectedHabitMonthSnapshot(
                    habitId = firstArg(),
                    month = secondArg(),
                    completedDates = emptySet(),
                    scheduledDates = emptySet(),
                    dayNotesByDate = emptyMap(),
                    createdDate = LocalDate.of(2026, 3, 1)
                )
            )
        }
        every { repository.observeGlobalMonth(any()) } answers {
            MutableStateFlow(
                HabitRepository.GlobalMonthSnapshot(
                    month = firstArg(),
                    globalCompletedDates = emptySet(),
                    globalScheduledDates = emptySet(),
                    globalBirthdayDates = emptySet(),
                    globalNoteDates = emptySet(),
                    birthdayNamesByDate = emptyMap(),
                    dayDetailsByDate = emptyMap(),
                    businessToday = LocalDate.of(2026, 3, 7)
                )
            )
        }
        every { repository.calculateScheduledDates(any(), any(), any(), any(), any()) } returns emptySet()
        every { repository.epochMillisToLocalDate(any()) } answers { LocalDate.of(2026, 3, 1) }
    }

    private fun repositoryDate(date: LocalDate): Long = date.toEpochDay() * 86_400_000L
}
