package com.example.habittracker.ui.viewmodel

import com.example.habittracker.MainDispatcherRule
import com.example.habittracker.data.entity.WhoAmINote
import com.example.habittracker.repository.HabitRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WhoAmIViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<HabitRepository>(relaxed = true)

    @Test
    fun `createNote selects the created note`() = runTest {
        every { repository.observeWhoAmINotes() } returns MutableStateFlow(emptyList())
        coEvery { repository.createWhoAmINote("Ideas") } returns 22L

        val viewModel = WhoAmIViewModel(repository)

        viewModel.createNote("Ideas")
        advanceUntilIdle()

        assertEquals(22L, viewModel.uiState.value.selectedNoteId)
    }

    @Test
    fun `saveNoteContent shows saving state and clears it after delay`() = runTest {
        every { repository.observeWhoAmINotes() } returns MutableStateFlow(
            listOf(WhoAmINote(id = 1L, title = "Title", content = "Old", createdAt = 0L))
        )

        val viewModel = WhoAmIViewModel(repository)
        advanceUntilIdle()
        viewModel.toggleNoteSelection(1L)

        viewModel.saveNoteContent(1L, "New")
        runCurrent()
        assertEquals(1L, viewModel.uiState.value.savingNoteId)

        advanceTimeBy(400)
        advanceUntilIdle()

        coVerify { repository.updateWhoAmINoteContent(1L, "New") }
        assertNull(viewModel.uiState.value.savingNoteId)
    }

    @Test
    fun `moveNote persists reordered ids after debounce`() = runTest {
        every { repository.observeWhoAmINotes() } returns MutableStateFlow(
            listOf(
                WhoAmINote(id = 1L, title = "One", content = "", createdAt = 0L),
                WhoAmINote(id = 2L, title = "Two", content = "", createdAt = 0L)
            )
        )

        val viewModel = WhoAmIViewModel(repository)
        advanceUntilIdle()

        viewModel.moveNote(2L, -1)
        assertEquals(listOf(2L, 1L), viewModel.uiState.value.notes.map { it.id })

        advanceTimeBy(300)
        advanceUntilIdle()

        coVerify { repository.reorderWhoAmINotes(listOf(2L, 1L)) }
    }
}
