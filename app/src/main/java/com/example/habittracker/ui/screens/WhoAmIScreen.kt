package com.example.habittracker.ui.screens

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy all notes")
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
                        .padding(24.dp),
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            onContentChange = { viewModel.updateSelectedNoteContent(it) }
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
    onContentChange: (String) -> Unit
) {
    var dragY = 0f
    var localContent by rememberSaveable(note.id) { mutableStateOf(note.content) }
    val animatedScale by animateFloatAsState(
        targetValue = if (isDragging) 1.02f else 1f,
        animationSpec = spring()
    )
    val dragContainerColor by animateColorAsState(
        targetValue = if (isDragging) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    )
    Card(
        colors = CardDefaults.cardColors(containerColor = dragContainerColor),
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .pointerInput(note.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDragEnd = {
                        dragY = 0f
                        onDragEnd()
                    },
                    onDragCancel = {
                        dragY = 0f
                        onDragEnd()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragY += dragAmount.y
                        when {
                            dragY > 28f -> {
                                onMove(1)
                                dragY = 0f
                            }
                            dragY < -28f -> {
                                onMove(-1)
                                dragY = 0f
                            }
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
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse note" else "Expand note"
                    )
                }
            }

            if (expanded) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onCopy) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy note")
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
            }
        }
    }
}
