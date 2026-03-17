package com.example.habittracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.habittracker.ui.components.FabScrollClearance
import com.example.habittracker.ui.icons.AppIcons
import com.example.habittracker.ui.viewmodel.WhoAmINoteUiState
import com.example.habittracker.ui.viewmodel.WhoAmIViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WhoAmIScreen(viewModel: WhoAmIViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current

    var isAddDialogVisible by rememberSaveable { mutableStateOf(false) }
    var noteTitleInput by rememberSaveable { mutableStateOf("") }
    var notePendingDelete by remember { mutableStateOf<WhoAmINoteUiState?>(null) }
    var notePendingRename by remember { mutableStateOf<WhoAmINoteUiState?>(null) }
    var renameInput by rememberSaveable { mutableStateOf("") }
    var draggingNoteId by remember { mutableStateOf<Long?>(null) }

    var isRefreshing by rememberSaveable { mutableStateOf(false) }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { isRefreshing = true }
    )
    if (isRefreshing) {
        LaunchedEffect(Unit) {
            delay(650)
            isRefreshing = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Who am I?") },
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
            FloatingActionButton(onClick = { isAddDialogVisible = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add note")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pullRefresh(pullRefreshState)
        ) {
            if (state.notes.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = FabScrollClearance),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No notes yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Tap + to add a note",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = FabScrollClearance)
                ) {
                    items(state.notes, key = { it.id }) { note ->
                        NoteCard(
                            modifier = Modifier,
                            note = note,
                            expanded = state.selectedNoteId == note.id,
                            isDragging = draggingNoteId == note.id,
                            onToggleExpanded = { viewModel.toggleNoteSelection(note.id) },
                            onMove = { direction -> viewModel.moveNote(note.id, direction) },
                            onDragStart = { draggingNoteId = note.id },
                            onDragEnd = { draggingNoteId = null },
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(note.content))
                            },
                            onDelete = { notePendingDelete = note },
                            onRename = {
                                notePendingRename = note
                                renameInput = note.title
                            },
                            isSaving = state.savingNoteId == note.id,
                            onContentChange = { viewModel.updateSelectedNoteContent(it) },
                            onSave = { viewModel.saveNoteContent(note.id, it) }
                        )
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    if (isAddDialogVisible) {
        AlertDialog(
            onDismissRequest = { isAddDialogVisible = false },
            title = { Text("Add note") },
            text = {
                OutlinedTextField(
                    value = noteTitleInput,
                    onValueChange = { noteTitleInput = it },
                    label = { Text("Note title") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (noteTitleInput.isNotBlank()) {
                            viewModel.createNote(noteTitleInput)
                            noteTitleInput = ""
                            isAddDialogVisible = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { isAddDialogVisible = false }) { Text("Cancel") }
            }
        )
    }

    if (notePendingDelete != null) {
        AlertDialog(
            onDismissRequest = { notePendingDelete = null },
            title = { Text("Delete note") },
            text = { Text("Do you want to delete \"${notePendingDelete?.title}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        notePendingDelete?.let(viewModel::deleteNote)
                        notePendingDelete = null
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { notePendingDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (notePendingRename != null) {
        AlertDialog(
            onDismissRequest = { notePendingRename = null },
            title = { Text("Rename note") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Note title") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val note = notePendingRename
                        if (note != null && renameInput.isNotBlank()) {
                            viewModel.renameNote(note, renameInput)
                            notePendingRename = null
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { notePendingRename = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun NoteCard(
    modifier: Modifier = Modifier,
    note: WhoAmINoteUiState,
    expanded: Boolean,
    isDragging: Boolean,
    onToggleExpanded: () -> Unit,
    onMove: (Int) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    isSaving: Boolean,
    onContentChange: (String) -> Unit,
    onSave: (String) -> Unit
) {
    var dragOffsetY by remember(note.id) { mutableFloatStateOf(0f) }
    val reorderStepPx = 72f
    var localContent by rememberSaveable(note.id) { mutableStateOf(note.content) }
    val dragContainerColor = if (isDragging) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = dragContainerColor),
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = dragOffsetY
            }
            .zIndex(if (isDragging) 1f else 0f)
            .pointerInput(note.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        dragOffsetY = 0f
                        onDragStart()
                    },
                    onDragEnd = {
                        dragOffsetY = 0f
                        onDragEnd()
                    },
                    onDragCancel = {
                        dragOffsetY = 0f
                        onDragEnd()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffsetY += dragAmount.y

                        while (dragOffsetY > reorderStepPx) {
                            onMove(1)
                            dragOffsetY -= reorderStepPx
                        }
                        while (dragOffsetY < -reorderStepPx) {
                            onMove(-1)
                            dragOffsetY += reorderStepPx
                        }
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onToggleExpanded() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete note")
                    }
                    IconButton(onClick = onRename) {
                        Icon(Icons.Default.Edit, contentDescription = "Rename note")
                    }
                    Icon(
                        imageVector = if (expanded) AppIcons.ExpandLess else AppIcons.ExpandMore,
                        contentDescription = if (expanded) "Collapse note" else "Expand note"
                    )
                }
            }

            if (expanded) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onCopy) {
                        Icon(AppIcons.ContentCopy, contentDescription = "Copy note")
                    }
                }
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = localContent,
                    onValueChange = {
                        localContent = it
                        onContentChange(it)
                    },
                    minLines = 10,
                    maxLines = Int.MAX_VALUE,
                    label = { Text("Write your note") },
                    shape = RoundedCornerShape(22.dp)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = { onSave(localContent) },
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Saving...")
                        } else {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}
