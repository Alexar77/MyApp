package com.example.habittracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.repository.HabitRepository
import com.example.habittracker.util.DebugLog
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
    val selectedNoteId: Long? = null,
    val savingNoteId: Long? = null
)

@HiltViewModel
class WhoAmIViewModel @Inject constructor(
    private val habitRepository: HabitRepository
) : ViewModel() {
    private val logTag = "WhoAmIVM"

    private val mutableUiState = MutableStateFlow(WhoAmIUiState())
    val uiState: StateFlow<WhoAmIUiState> = mutableUiState.asStateFlow()
    private var contentSaveJob: Job? = null
    private var reorderJob: Job? = null

    init {
        viewModelScope.launch {
            habitRepository.observeWhoAmINotes().distinctUntilChanged().collect { notes ->
                DebugLog.d(logTag, "flow update notes=${notes.size}")
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
                DebugLog.d(logTag, "uiState publish notes=${mutableUiState.value.notes.size} selected=${mutableUiState.value.selectedNoteId}")
            }
        }
    }

    fun createNote(title: String) {
        DebugLog.d(logTag, "createNote titleLength=${title.length}")
        viewModelScope.launch {
            val newId = habitRepository.createWhoAmINote(title)
            mutableUiState.update { currentState -> currentState.copy(selectedNoteId = newId) }
            DebugLog.d(logTag, "createNote completed newId=$newId")
        }
    }

    fun toggleNoteSelection(noteId: Long) {
        DebugLog.d(logTag, "toggleNoteSelection noteId=$noteId currentSelected=${mutableUiState.value.selectedNoteId}")
        mutableUiState.update { currentState ->
            currentState.copy(
                selectedNoteId = if (currentState.selectedNoteId == noteId) null else noteId
            )
        }
    }

    fun updateSelectedNoteContent(content: String) {
        val selectedId = mutableUiState.value.selectedNoteId ?: return
        DebugLog.d(logTag, "updateSelectedNoteContent noteId=$selectedId contentLength=${content.length}")
        // Update local UI state so the text field reflects the change immediately
        mutableUiState.update { state ->
            state.copy(
                notes = state.notes.map { note ->
                    if (note.id == selectedId) note.copy(content = content) else note
                }
            )
        }
    }

    fun saveNoteContent(noteId: Long, content: String) {
        DebugLog.d(logTag, "saveNoteContent noteId=$noteId contentLength=${content.length}")
        contentSaveJob?.cancel()
        contentSaveJob = viewModelScope.launch {
            mutableUiState.update { it.copy(savingNoteId = noteId) }
            habitRepository.updateWhoAmINoteContent(noteId, content)
            delay(400)
            mutableUiState.update { it.copy(savingNoteId = null) }
            DebugLog.d(logTag, "saveNoteContent completed noteId=$noteId")
        }
    }

    fun renameNote(note: WhoAmINoteUiState, title: String) {
        DebugLog.d(logTag, "renameNote noteId=${note.id} titleLength=${title.length}")
        viewModelScope.launch {
            habitRepository.renameWhoAmINote(note.id, title)
        }
    }

    fun deleteNote(note: WhoAmINoteUiState) {
        DebugLog.d(logTag, "deleteNote noteId=${note.id}")
        viewModelScope.launch {
            habitRepository.deleteWhoAmINote(note.id)
        }
    }

    fun moveNote(noteId: Long, direction: Int) {
        DebugLog.d(logTag, "moveNote noteId=$noteId direction=$direction")
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
            DebugLog.d(logTag, "moveNote persisted count=${reordered.size}")
        }
    }
}
