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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WhoAmINoteUiState(
    val id: Long,
    val title: String,
    val content: String
)

data class WhoAmIUiState(
    val notes: List<WhoAmINoteUiState> = emptyList(),
    val selectedNoteId: Long? = null
)

@HiltViewModel
class WhoAmIViewModel @Inject constructor(
    private val habitRepository: HabitRepository
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(WhoAmIUiState())
    val uiState: StateFlow<WhoAmIUiState> = mutableUiState.asStateFlow()
    private var contentSaveJob: Job? = null
    private var reorderJob: Job? = null

    init {
        viewModelScope.launch {
            habitRepository.observeWhoAmINotes().distinctUntilChanged().collect { notes ->
                val noteUiList = notes.map { note ->
                    WhoAmINoteUiState(id = note.id, title = note.title, content = note.content)
                }

                mutableUiState.update { currentState ->
                    val selectedStillExists = currentState.selectedNoteId != null &&
                        noteUiList.any { it.id == currentState.selectedNoteId }

                    currentState.copy(
                        notes = noteUiList,
                        selectedNoteId = if (selectedStillExists) currentState.selectedNoteId else null
                    )
                }
            }
        }
    }

    fun createNote(title: String) {
        viewModelScope.launch {
            val newId = habitRepository.createWhoAmINote(title)
            mutableUiState.update { currentState -> currentState.copy(selectedNoteId = newId) }
        }
    }

    fun toggleNoteSelection(noteId: Long) {
        mutableUiState.update { currentState ->
            currentState.copy(
                selectedNoteId = if (currentState.selectedNoteId == noteId) null else noteId
            )
        }
    }

    fun updateSelectedNoteContent(content: String) {
        val selectedId = mutableUiState.value.selectedNoteId ?: return
        contentSaveJob?.cancel()
        contentSaveJob = viewModelScope.launch {
            delay(300)
            habitRepository.updateWhoAmINoteContent(selectedId, content)
        }
    }

    fun renameNote(note: WhoAmINoteUiState, title: String) {
        viewModelScope.launch {
            habitRepository.renameWhoAmINote(note.id, title)
        }
    }

    fun deleteNote(note: WhoAmINoteUiState) {
        viewModelScope.launch {
            habitRepository.deleteWhoAmINote(note.id)
        }
    }

    fun moveNote(noteId: Long, direction: Int) {
        val sourceList = mutableUiState.value.notes
        val fromIndex = sourceList.indexOfFirst { it.id == noteId }
        if (fromIndex == -1) return

        val toIndex = (fromIndex + direction).coerceIn(0, sourceList.lastIndex)
        if (toIndex == fromIndex) return

        val reordered = sourceList.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }

        // Update UI immediately
        mutableUiState.update { it.copy(notes = reordered) }

        // Debounce DB write
        reorderJob?.cancel()
        reorderJob = viewModelScope.launch {
            delay(300)
            habitRepository.reorderWhoAmINotes(reordered.map { it.id })
        }
    }
}
