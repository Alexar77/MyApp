package com.example.habittracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.habittracker.ui.components.AppModalSheet
import com.example.habittracker.ui.components.EmptyStateCard
import com.example.habittracker.ui.components.PillLabel
import com.example.habittracker.ui.components.PremiumCard
import com.example.habittracker.ui.components.ScreenBackground
import com.example.habittracker.ui.components.SectionHeader
import com.example.habittracker.ui.components.SurfaceCard
import com.example.habittracker.ui.icons.AppIcons
import com.example.habittracker.ui.theme.MyAppTheme
import com.example.habittracker.ui.viewmodel.WhoAmINoteUiState
import com.example.habittracker.ui.viewmodel.WhoAmIViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhoAmIScreen(viewModel: WhoAmIViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    val selectedNote = state.notes.firstOrNull { it.id == state.selectedNoteId }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isCreateSheetVisible by rememberSaveable { mutableStateOf(false) }
    var newNoteTitle by rememberSaveable { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<WhoAmINoteUiState?>(null) }
    var renameInput by rememberSaveable { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<WhoAmINoteUiState?>(null) }

    if (selectedNote != null) {
        JournalEditorScreen(
            note = selectedNote,
            isSaving = state.savingNoteId == selectedNote.id,
            onBack = { viewModel.toggleNoteSelection(selectedNote.id) },
            onContentChange = { content ->
                viewModel.updateSelectedNoteContent(content)
                viewModel.saveNoteContent(selectedNote.id, content)
            },
            onCopy = { clipboardManager.setText(AnnotatedString(selectedNote.content)) },
            onRename = {
                renameTarget = selectedNote
                renameInput = selectedNote.title
            },
            onDelete = { pendingDelete = selectedNote }
        )
    } else {
        val filteredNotes = remember(state.notes, searchQuery) {
            state.notes.filter { note ->
                searchQuery.isBlank() ||
                    note.title.contains(searchQuery, ignoreCase = true) ||
                    note.content.contains(searchQuery, ignoreCase = true)
            }
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Journal") },
                    actions = {
                        IconButton(
                            onClick = {
                                if (state.notes.isEmpty()) return@IconButton
                                val combined = state.notes.joinToString("\n\n") { note ->
                                    "${note.title}\n${note.content}"
                                }
                                clipboardManager.setText(AnnotatedString(combined))
                            }
                        ) {
                            Icon(AppIcons.ContentCopy, contentDescription = "Copy all notes")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { isCreateSheetVisible = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Create note")
                }
            }
        ) { innerPadding ->
            ScreenBackground {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    item {
                        PremiumCard(accent = MyAppTheme.extraColors.accent) {
                            SectionHeader(
                                title = "Private thinking space",
                                subtitle = "Capture identity notes, beliefs, and reminders you want to revisit."
                            )
                            PillLabel(
                                text = "${state.notes.size} notes",
                                color = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Search notes") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                        )
                    }

                    if (filteredNotes.isEmpty()) {
                        item {
                            EmptyStateCard(
                                title = if (state.notes.isEmpty()) "No notes yet" else "No matches",
                                message = if (state.notes.isEmpty()) {
                                    "Start with one note about who you are becoming."
                                } else {
                                    "Try a different word or create a new note."
                                },
                                actionLabel = "New note",
                                onAction = { isCreateSheetVisible = true }
                            )
                        }
                    } else {
                        item {
                            SectionHeader(
                                title = "Recent notes",
                                subtitle = "Tap a card to open the full editor."
                            )
                        }
                        items(filteredNotes, key = { it.id }) { note ->
                            JournalNoteCard(
                                note = note,
                                onOpen = { viewModel.toggleNoteSelection(note.id) },
                                onRename = {
                                    renameTarget = note
                                    renameInput = note.title
                                },
                                onDelete = { pendingDelete = note }
                            )
                        }
                    }
                }
            }
        }
    }

    if (isCreateSheetVisible) {
        AppModalSheet(onDismissRequest = { isCreateSheetVisible = false }) {
            SectionHeader(
                title = "Create note",
                subtitle = "Start with a short title. You can refine the content in the editor."
            )
            OutlinedTextField(
                value = newNoteTitle,
                onValueChange = { newNoteTitle = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Title") },
                singleLine = true
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { isCreateSheetVisible = false }) { Text("Cancel") }
                TextButton(
                    onClick = {
                        if (newNoteTitle.isBlank()) return@TextButton
                        viewModel.createNote(newNoteTitle.trim())
                        newNoteTitle = ""
                        isCreateSheetVisible = false
                    }
                ) { Text("Create") }
            }
        }
    }

    if (renameTarget != null) {
        AppModalSheet(onDismissRequest = { renameTarget = null }) {
            SectionHeader(title = "Rename note")
            OutlinedTextField(
                value = renameInput,
                onValueChange = { renameInput = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Title") },
                singleLine = true
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { renameTarget = null }) { Text("Cancel") }
                TextButton(
                    onClick = {
                        val target = renameTarget ?: return@TextButton
                        if (renameInput.isBlank()) return@TextButton
                        viewModel.renameNote(target, renameInput.trim())
                        renameTarget = null
                    }
                ) { Text("Save") }
            }
        }
    }

    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete note") },
            text = { Text("Delete ${pendingDelete?.title}? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete?.let(viewModel::deleteNote)
                        pendingDelete = null
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JournalEditorScreen(
    note: WhoAmINoteUiState,
    isSaving: Boolean,
    onBack: () -> Unit,
    onContentChange: (String) -> Unit,
    onCopy: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var content by remember(note.id, note.content) { mutableStateOf(note.content) }
    val formatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(note.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onRename) {
                        Icon(Icons.Default.Edit, contentDescription = "Rename note")
                    }
                    IconButton(onClick = onCopy) {
                        Icon(AppIcons.ContentCopy, contentDescription = "Copy note")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete note")
                    }
                }
            )
        }
    ) { innerPadding ->
        ScreenBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PremiumCard(accent = MyAppTheme.extraColors.accent) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PillLabel(
                            text = "Journal note",
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isSaving) "Autosaving..." else "Saved",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSaving) MyAppTheme.extraColors.warning else MyAppTheme.extraColors.success
                        )
                    }
                    Text(
                        text = "Created ${Instant.ofEpochMilli(note.createdAt).atZone(ZoneId.systemDefault()).toLocalDate().format(formatter)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = content,
                    onValueChange = {
                        content = it
                        onContentChange(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    minLines = 16,
                    label = { Text("Write without rushing") }
                )
            }
        }
    }
}

@Composable
private fun JournalNoteCard(
    note: WhoAmINoteUiState,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val formatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy") }
    SurfaceCard(
        modifier = Modifier.clickable { onOpen() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(note.title, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = note.content.ifBlank { "Open the note to start writing." }.take(140),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = Instant.ofEpochMilli(note.createdAt)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                        .format(formatter),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row {
                IconButton(onClick = onRename) {
                    Icon(Icons.Default.Edit, contentDescription = "Rename")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}
