package com.example.habittracker.ui.viewmodel

import com.example.habittracker.MainDispatcherRule
import com.example.habittracker.notifications.ReminderScheduler
import com.example.habittracker.repository.HabitRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BirthdaysViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<HabitRepository>(relaxed = true)
    private val reminderScheduler = mockk<ReminderScheduler>(relaxed = true)

    @Test
    fun `ui state sorts birthdays by next occurrence then name`() = runTest {
        every { repository.currentBusinessDate(any()) } returns LocalDate.of(2026, 3, 7)
        every { repository.parseReminderDateTimesCsv(any()) } answers { emptyList() }
        every { repository.nextBirthdayOccurrence(LocalDate.of(2026, 3, 7), 3, 9) } returns LocalDate.of(2026, 3, 9)
        every { repository.nextBirthdayOccurrence(LocalDate.of(2026, 3, 7), 3, 8) } returns LocalDate.of(2026, 3, 8)
        every { repository.observeBirthdays() } returns MutableStateFlow(
            listOf(
                HabitRepository.BirthdayOption(1L, "Zoe", 1990, 3, 9, null),
                HabitRepository.BirthdayOption(2L, "Amy", 1991, 3, 8, null),
                HabitRepository.BirthdayOption(3L, "Ben", 1992, 3, 8, null)
            )
        )

        val viewModel = BirthdaysViewModel(repository, reminderScheduler)
        advanceUntilIdle()

        assertEquals(listOf("Amy", "Ben", "Zoe"), viewModel.uiState.value.birthdays.map { it.name })
    }

    @Test
    fun `addBirthday updates reminder csv after insert`() = runTest {
        every { repository.currentBusinessDate(any()) } returns LocalDate.of(2026, 3, 7)
        every { repository.observeBirthdays() } returns MutableStateFlow(emptyList())
        coEvery { repository.addBirthday("Alex", 1995, 5, 20) } returns 12L

        val viewModel = BirthdaysViewModel(repository, reminderScheduler)

        viewModel.addBirthday("Alex", LocalDate.of(1995, 5, 20), listOf(10L, 20L))
        advanceUntilIdle()

        coVerify { repository.updateBirthday(12L, "Alex", 1995, 5, 20, "10|20") }
    }

    @Test
    fun `deleteBirthday reschedules reminders`() = runTest {
        every { repository.currentBusinessDate(any()) } returns LocalDate.of(2026, 3, 7)
        every { repository.observeBirthdays() } returns MutableStateFlow(emptyList())
        coEvery { repository.getReminderScheduleItems() } returns emptyList()

        val viewModel = BirthdaysViewModel(repository, reminderScheduler)

        viewModel.deleteBirthday(4L)
        advanceUntilIdle()

        coVerify { repository.deleteBirthday(4L) }
        verify { reminderScheduler.rescheduleAll(emptyList()) }
    }
}
