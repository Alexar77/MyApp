package com.example.habittracker.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.habittracker.ui.viewmodel.TaskUiItem
import com.example.habittracker.ui.viewmodel.TasksViewModel
import java.time.LocalTime
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(viewModel: TasksViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var isAddDialogVisible by rememberSaveable { mutableStateOf(false) }
    var taskInput by rememberSaveable { mutableStateOf("") }
    var reminderEnabledInput by rememberSaveable { mutableStateOf(false) }
    var reminderTimeInput by rememberSaveable { mutableStateOf("09:00") }
    var reminderMessageInput by rememberSaveable { mutableStateOf("") }
    var reminderTimeError by rememberSaveable { mutableStateOf(false) }
    var draggingTaskId by remember { mutableStateOf<Long?>(null) }
    var taskPendingDelete by remember { mutableStateOf<TaskUiItem?>(null) }
    var taskEditing by remember { mutableStateOf<TaskUiItem?>(null) }
    var editTaskInput by rememberSaveable { mutableStateOf("") }
    var editReminderEnabledInput by rememberSaveable { mutableStateOf(false) }
    var editReminderTimeInput by rememberSaveable { mutableStateOf("09:00") }
    var editReminderMessageInput by rememberSaveable { mutableStateOf("") }
    var editReminderTimeError by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

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
        topBar = { TopAppBar(title = { Text("Tasks") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { isAddDialogVisible = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add task")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pullRefresh(pullRefreshState)
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                item {
                    Text("To do", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                if (state.pending.isEmpty()) {
                    item {
                        Text(
                            text = "No pending tasks",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(state.pending, key = { it.id }) { task ->
                        TaskRow(
                            modifier = Modifier,
                            task = task,
                            isDragging = draggingTaskId == task.id,
                            onToggle = { viewModel.toggleTask(task) },
                            onDelete = { taskPendingDelete = task },
                            onMove = { direction -> viewModel.moveTask(task.id, task.isDone, direction) },
                            onDragStart = { draggingTaskId = task.id },
                            onDragEnd = { draggingTaskId = null },
                            onEdit = {
                                taskEditing = task
                                editTaskInput = task.title
                                editReminderEnabledInput = task.reminderEnabled
                                editReminderTimeInput = task.reminderTime ?: "09:00"
                                editReminderMessageInput = task.reminderMessage.orEmpty()
                                editReminderTimeError = false
                            }
                        )
                    }
                }

                item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
                item {
                    Text("Done", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                if (state.done.isEmpty()) {
                    item {
                        Text(
                            text = "No completed tasks yet",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(state.done, key = { it.id }) { task ->
                        TaskRow(
                            modifier = Modifier,
                            task = task,
                            isDragging = draggingTaskId == task.id,
                            onToggle = { viewModel.toggleTask(task) },
                            onDelete = { taskPendingDelete = task },
                            onMove = { direction -> viewModel.moveTask(task.id, task.isDone, direction) },
                            onDragStart = { draggingTaskId = task.id },
                            onDragEnd = { draggingTaskId = null },
                            onEdit = {
                                taskEditing = task
                                editTaskInput = task.title
                                editReminderEnabledInput = task.reminderEnabled
                                editReminderTimeInput = task.reminderTime ?: "09:00"
                                editReminderMessageInput = task.reminderMessage.orEmpty()
                                editReminderTimeError = false
                            }
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
            title = { Text("Add task") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.OutlinedTextField(
                        value = taskInput,
                        onValueChange = {
                            taskInput = it
                            reminderTimeError = false
                        },
                        singleLine = true,
                        label = { Text("Task") }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Reminder")
                        androidx.compose.material3.Switch(
                            checked = reminderEnabledInput,
                            onCheckedChange = { reminderEnabledInput = it }
                        )
                    }
                    if (reminderEnabledInput) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val parsed = runCatching { LocalTime.parse(reminderTimeInput) }.getOrNull()
                                        ?: LocalTime.of(9, 0)
                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            reminderTimeInput = String.format("%02d:%02d", hour, minute)
                                            reminderTimeError = false
                                        },
                                        parsed.hour,
                                        parsed.minute,
                                        true
                                    ).show()
                                }
                        ) {
                            androidx.compose.material3.OutlinedTextField(
                                value = reminderTimeInput,
                                onValueChange = {},
                                singleLine = true,
                                enabled = false,
                                isError = reminderTimeError,
                                label = { Text("Reminder time (HH:MM)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                trailingIcon = {
                                    Icon(Icons.Default.AccessTime, contentDescription = "Select reminder time")
                                }
                            )
                        }
                        androidx.compose.material3.OutlinedTextField(
                            value = reminderMessageInput,
                            onValueChange = { reminderMessageInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Reminder message (optional)") },
                            minLines = 2,
                            maxLines = 4
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (taskInput.isNotBlank()) {
                            val ok = viewModel.addTask(
                                title = taskInput,
                                reminderEnabled = reminderEnabledInput,
                                reminderTime = reminderTimeInput,
                                reminderMessage = reminderMessageInput.takeIf { reminderEnabledInput }
                            )
                            if (ok) {
                                if (reminderEnabledInput && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (!granted) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                                taskInput = ""
                                reminderEnabledInput = false
                                reminderTimeInput = "09:00"
                                reminderMessageInput = ""
                                reminderTimeError = false
                                isAddDialogVisible = false
                            } else {
                                reminderTimeError = reminderEnabledInput
                            }
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

    if (taskPendingDelete != null) {
        AlertDialog(
            onDismissRequest = { taskPendingDelete = null },
            title = { Text("Delete task") },
            text = { Text("Do you want to delete \"${taskPendingDelete?.title}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        taskPendingDelete?.let(viewModel::deleteTask)
                        taskPendingDelete = null
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { taskPendingDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (taskEditing != null) {
        AlertDialog(
            onDismissRequest = { taskEditing = null },
            title = { Text("Edit task") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.OutlinedTextField(
                        value = editTaskInput,
                        onValueChange = {
                            editTaskInput = it
                            editReminderTimeError = false
                        },
                        singleLine = true,
                        label = { Text("Task") }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Reminder")
                        androidx.compose.material3.Switch(
                            checked = editReminderEnabledInput,
                            onCheckedChange = { editReminderEnabledInput = it }
                        )
                    }
                    if (editReminderEnabledInput) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val parsed = runCatching { LocalTime.parse(editReminderTimeInput) }.getOrNull()
                                        ?: LocalTime.of(9, 0)
                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            editReminderTimeInput = String.format("%02d:%02d", hour, minute)
                                            editReminderTimeError = false
                                        },
                                        parsed.hour,
                                        parsed.minute,
                                        true
                                    ).show()
                                }
                        ) {
                            androidx.compose.material3.OutlinedTextField(
                                value = editReminderTimeInput,
                                onValueChange = {},
                                singleLine = true,
                                enabled = false,
                                isError = editReminderTimeError,
                                label = { Text("Reminder time (HH:MM)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                trailingIcon = {
                                    Icon(Icons.Default.AccessTime, contentDescription = "Select reminder time")
                                }
                            )
                        }
                        androidx.compose.material3.OutlinedTextField(
                            value = editReminderMessageInput,
                            onValueChange = { editReminderMessageInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Reminder message (optional)") },
                            minLines = 2,
                            maxLines = 4
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val task = taskEditing ?: return@TextButton
                        if (editTaskInput.isBlank()) return@TextButton
                        val ok = viewModel.updateTask(
                            taskId = task.id,
                            title = editTaskInput,
                            reminderEnabled = editReminderEnabledInput,
                            reminderTime = editReminderTimeInput,
                            reminderMessage = editReminderMessageInput
                        )
                        if (ok) {
                            if (editReminderEnabledInput && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                                if (!granted) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                            taskEditing = null
                        } else {
                            editReminderTimeError = editReminderEnabledInput
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { taskEditing = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun TaskRow(
    modifier: Modifier = Modifier,
    task: TaskUiItem,
    isDragging: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onMove: (Int) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onEdit: () -> Unit
) {
    var dragY = 0f
    val animatedScale by animateFloatAsState(
        targetValue = if (isDragging) 1.02f else 1f,
        animationSpec = spring()
    )
    val dragBgColor by animateColorAsState(
        targetValue = if (isDragging) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
        } else {
            Color.Transparent
        }
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(dragBgColor)
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .pointerInput(task.id) {
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
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onToggle) {
            if (task.isDone) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Mark as not done")
            } else {
                Icon(Icons.Outlined.RadioButtonUnchecked, contentDescription = "Mark as done")
            }
        }
        Text(
            modifier = Modifier.weight(1f),
            text = task.title,
            textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None,
            color = if (task.isDone) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Edit task")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete task")
        }
    }
}
