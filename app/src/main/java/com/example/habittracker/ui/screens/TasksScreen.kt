package com.example.habittracker.ui.screens

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import com.example.habittracker.ui.icons.AppIcons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.habittracker.ui.viewmodel.TaskUiItem
import com.example.habittracker.ui.viewmodel.TasksViewModel
import com.example.habittracker.ui.viewmodel.ALL_CATEGORIES
import com.example.habittracker.repository.HabitRepository
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

private val TaskRowShape = RoundedCornerShape(14.dp)
private val CompletedDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
private val ReminderDateTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(viewModel: TasksViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var isAddDialogVisible by rememberSaveable { mutableStateOf(false) }
    var taskInput by rememberSaveable { mutableStateOf("") }
    var taskCategoryInput by rememberSaveable { mutableStateOf(HabitRepository.DEFAULT_TASK_CATEGORY) }
    var reminderEnabledInput by rememberSaveable { mutableStateOf(false) }
    var reminderDateTimesInput by remember { mutableStateOf(listOf<Long>()) }
    var reminderMessageInput by rememberSaveable { mutableStateOf("") }
    var reminderSelectionError by rememberSaveable { mutableStateOf(false) }
    var isAddCategoryDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    var draggingTaskId by remember { mutableStateOf<Long?>(null) }
    var taskPendingDelete by remember { mutableStateOf<TaskUiItem?>(null) }
    var taskEditing by remember { mutableStateOf<TaskUiItem?>(null) }
    var editTaskInput by rememberSaveable { mutableStateOf("") }
    var editTaskCategoryInput by rememberSaveable { mutableStateOf(HabitRepository.DEFAULT_TASK_CATEGORY) }
    var editReminderEnabledInput by rememberSaveable { mutableStateOf(false) }
    var editReminderDateTimesInput by remember { mutableStateOf(listOf<Long>()) }
    var editReminderMessageInput by rememberSaveable { mutableStateOf("") }
    var editReminderSelectionError by rememberSaveable { mutableStateOf(false) }
    var isAddCategoryDialogVisible by rememberSaveable { mutableStateOf(false) }
    var newCategoryInput by rememberSaveable { mutableStateOf("") }
    var isDeleteCategoryConfirmVisible by rememberSaveable { mutableStateOf(false) }
    var taskToTransfer by remember { mutableStateOf<TaskUiItem?>(null) }
    var transferCategoryInput by rememberSaveable { mutableStateOf("") }
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

    fun openTaskReminderDateTimePicker(onSelected: (Long) -> Unit) {
        val current = LocalDateTime.now()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        val millis = LocalDateTime.of(year, month + 1, dayOfMonth, hour, minute)
                            .atZone(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()
                        onSelected(millis)
                    },
                    current.hour,
                    current.minute,
                    true
                ).show()
            },
            current.year,
            current.monthValue - 1,
            current.dayOfMonth
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tasks") },
                actions = {
                    IconButton(
                        onClick = {
                            newCategoryInput = ""
                            isAddCategoryDialogVisible = true
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add category")
                    }
                    IconButton(
                        enabled = state.selectedCategory != ALL_CATEGORIES &&
                            state.selectedCategory != HabitRepository.DEFAULT_TASK_CATEGORY,
                        onClick = {
                            isDeleteCategoryConfirmVisible = true
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete category")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                taskCategoryInput = if (state.selectedCategory == ALL_CATEGORIES) {
                    HabitRepository.DEFAULT_TASK_CATEGORY
                } else {
                    state.selectedCategory
                }
                reminderEnabledInput = false
                reminderDateTimesInput = emptyList()
                reminderMessageInput = ""
                reminderSelectionError = false
                isAddDialogVisible = true
            }) {
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
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        val selected = state.selectedCategory == ALL_CATEGORIES
                        AssistChip(
                            onClick = { viewModel.selectCategory(ALL_CATEGORIES) },
                            label = { Text(ALL_CATEGORIES) },
                            colors = if (selected) {
                                AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            } else {
                                AssistChipDefaults.assistChipColors()
                            }
                        )
                    }
                }
                run {
                    var draggingCategory by remember { mutableStateOf<String?>(null) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.categories.forEach { category ->
                            key(category) {
                            val selected = state.selectedCategory == category
                            val isDragging = draggingCategory == category
                            var dragOffsetX by remember(category) { mutableFloatStateOf(0f) }
                            val reorderStepPx = 80f
                            AssistChip(
                                onClick = { viewModel.selectCategory(category) },
                                label = { Text(category) },
                                colors = if (selected) {
                                    AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                } else {
                                    AssistChipDefaults.assistChipColors()
                                },
                                modifier = Modifier
                                    .graphicsLayer {
                                        translationX = dragOffsetX
                                        alpha = if (isDragging) 0.8f else 1f
                                    }
                                    .pointerInput(category) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                dragOffsetX = 0f
                                                draggingCategory = category
                                            },
                                            onDragEnd = {
                                                dragOffsetX = 0f
                                                draggingCategory = null
                                            },
                                            onDragCancel = {
                                                dragOffsetX = 0f
                                                draggingCategory = null
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffsetX += dragAmount.x
                                                while (dragOffsetX > reorderStepPx) {
                                                    viewModel.moveCategory(category, 1)
                                                    dragOffsetX -= reorderStepPx
                                                }
                                                while (dragOffsetX < -reorderStepPx) {
                                                    viewModel.moveCategory(category, -1)
                                                    dragOffsetX += reorderStepPx
                                                }
                                            }
                                        )
                                    }
                            )
                            }
                        }
                    }
                }
                Text("To do", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (state.pending.isEmpty()) {
                    Text(
                        text = "No pending tasks",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    state.pending.forEach { task ->
                        TaskRow(
                            modifier = Modifier,
                            task = task,
                            isDragging = draggingTaskId == task.id,
                            onToggle = { viewModel.toggleTask(task) },
                            onDelete = { taskPendingDelete = task },
                            onMove = { direction -> viewModel.moveTask(task.id, task.isDone, direction) },
                            onDragStart = { draggingTaskId = task.id },
                            onDragEnd = { draggingTaskId = null },
                            onTransfer = {
                                taskToTransfer = task
                                transferCategoryInput = task.category
                            },
                            onEdit = {
                                taskEditing = task
                                editTaskInput = task.title
                                editTaskCategoryInput = task.category
                                editReminderEnabledInput = task.reminderEnabled
                                editReminderDateTimesInput = parseReminderDateTimesCsv(task.reminderDateTimesCsv)
                                editReminderMessageInput = task.reminderMessage.orEmpty()
                                editReminderSelectionError = false
                            }
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Done", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (state.done.isEmpty()) {
                    Text(
                        text = "No completed tasks yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    state.done.forEach { task ->
                        TaskRow(
                            modifier = Modifier,
                            task = task,
                            isDragging = draggingTaskId == task.id,
                            onToggle = { viewModel.toggleTask(task) },
                            onDelete = { taskPendingDelete = task },
                            onMove = { direction -> viewModel.moveTask(task.id, task.isDone, direction) },
                            onDragStart = { draggingTaskId = task.id },
                            onDragEnd = { draggingTaskId = null },
                            onTransfer = {
                                taskToTransfer = task
                                transferCategoryInput = task.category
                            },
                            onEdit = {
                                taskEditing = task
                                editTaskInput = task.title
                                editTaskCategoryInput = task.category
                                editReminderEnabledInput = task.reminderEnabled
                                editReminderDateTimesInput = parseReminderDateTimesCsv(task.reminderDateTimesCsv)
                                editReminderMessageInput = task.reminderMessage.orEmpty()
                                editReminderSelectionError = false
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
                            reminderSelectionError = false
                        },
                        singleLine = true,
                        label = { Text("Task") }
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        androidx.compose.material3.OutlinedTextField(
                            value = taskCategoryInput,
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            label = { Text("Category") },
                            trailingIcon = {
                                Icon(
                                    imageVector = AppIcons.SwapHoriz,
                                    contentDescription = "Select category"
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clickable { isAddCategoryDropdownExpanded = true }
                        )
                        DropdownMenu(
                            expanded = isAddCategoryDropdownExpanded,
                            onDismissRequest = { isAddCategoryDropdownExpanded = false }
                        ) {
                            state.categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        taskCategoryInput = category
                                        isAddCategoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
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
                        TextButton(
                            onClick = {
                                openTaskReminderDateTimePicker { millis ->
                                    reminderDateTimesInput = (reminderDateTimesInput + millis).distinct().sorted()
                                    reminderSelectionError = false
                                }
                            }
                        ) { Text("Add reminder date/time") }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = false) { },
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            reminderDateTimesInput.forEach { millis ->
                                val label = Instant.ofEpochMilli(millis)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDateTime()
                                    .format(ReminderDateTimeFormat)
                                AssistChip(
                                    onClick = { reminderDateTimesInput = reminderDateTimesInput - millis },
                                    label = { Text(label) }
                                )
                            }
                        }
                        if (reminderSelectionError && reminderDateTimesInput.isEmpty()) {
                            Text(
                                text = "Add at least one reminder date/time",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
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
                                category = taskCategoryInput,
                                reminderEnabled = reminderEnabledInput,
                                reminderTime = null,
                                reminderDateTimesCsv = reminderDateTimesInput.joinToString("|"),
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
                                taskCategoryInput = if (state.selectedCategory == ALL_CATEGORIES) {
                                    HabitRepository.DEFAULT_TASK_CATEGORY
                                } else {
                                    state.selectedCategory
                                }
                                isAddCategoryDropdownExpanded = false
                                reminderEnabledInput = false
                                reminderDateTimesInput = emptyList()
                                reminderMessageInput = ""
                                reminderSelectionError = false
                                isAddDialogVisible = false
                            } else {
                                reminderSelectionError = reminderEnabledInput && reminderDateTimesInput.isEmpty()
                            }
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    isAddCategoryDropdownExpanded = false
                    isAddDialogVisible = false
                }) { Text("Cancel") }
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
                            editReminderSelectionError = false
                        },
                        singleLine = true,
                        label = { Text("Task") }
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = editTaskCategoryInput,
                        onValueChange = { editTaskCategoryInput = it },
                        singleLine = true,
                        label = { Text("Category") }
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
                        TextButton(
                            onClick = {
                                openTaskReminderDateTimePicker { millis ->
                                    editReminderDateTimesInput = (editReminderDateTimesInput + millis).distinct().sorted()
                                    editReminderSelectionError = false
                                }
                            }
                        ) { Text("Add reminder date/time") }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            editReminderDateTimesInput.forEach { millis ->
                                val label = Instant.ofEpochMilli(millis)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDateTime()
                                    .format(ReminderDateTimeFormat)
                                AssistChip(
                                    onClick = { editReminderDateTimesInput = editReminderDateTimesInput - millis },
                                    label = { Text(label) }
                                )
                            }
                        }
                        if (editReminderSelectionError && editReminderDateTimesInput.isEmpty()) {
                            Text(
                                text = "Add at least one reminder date/time",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
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
                            category = editTaskCategoryInput,
                            reminderEnabled = editReminderEnabledInput,
                            reminderTime = null,
                            reminderDateTimesCsv = editReminderDateTimesInput.joinToString("|"),
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
                            editReminderSelectionError = editReminderEnabledInput &&
                                editReminderDateTimesInput.isEmpty()
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { taskEditing = null }) { Text("Cancel") }
            }
        )
    }

    if (isAddCategoryDialogVisible) {
        AlertDialog(
            onDismissRequest = { isAddCategoryDialogVisible = false },
            title = { Text("Add category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.OutlinedTextField(
                        value = newCategoryInput,
                        onValueChange = { newCategoryInput = it },
                        singleLine = true,
                        label = { Text("Category name") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (viewModel.addCategory(newCategoryInput)) {
                            viewModel.selectCategory(newCategoryInput.trim())
                            isAddCategoryDialogVisible = false
                        }
                    }
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { isAddCategoryDialogVisible = false }) { Text("Cancel") }
            }
        )
    }

    if (isDeleteCategoryConfirmVisible) {
        AlertDialog(
            onDismissRequest = { isDeleteCategoryConfirmVisible = false },
            title = { Text("Delete category") },
            text = {
                Text("Delete \"${state.selectedCategory}\"? All tasks inside will be permanently deleted.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCategory(state.selectedCategory)
                        isDeleteCategoryConfirmVisible = false
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { isDeleteCategoryConfirmVisible = false }) { Text("Cancel") }
            }
        )
    }

    if (taskToTransfer != null) {
        AlertDialog(
            onDismissRequest = { taskToTransfer = null },
            title = { Text("Transfer task") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Move \"${taskToTransfer?.title}\" to category:")
                    androidx.compose.material3.OutlinedTextField(
                        value = transferCategoryInput,
                        onValueChange = { transferCategoryInput = it },
                        singleLine = true,
                        label = { Text("Target category") }
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.categories.filter { it != ALL_CATEGORIES }, key = { it }) { category ->
                            AssistChip(
                                onClick = { transferCategoryInput = category },
                                label = { Text(category) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val current = taskToTransfer ?: return@TextButton
                        if (viewModel.transferTaskToCategory(current.id, transferCategoryInput)) {
                            taskToTransfer = null
                        }
                    }
                ) { Text("Transfer") }
            },
            dismissButton = {
                TextButton(onClick = { taskToTransfer = null }) { Text("Cancel") }
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
    onTransfer: () -> Unit,
    onMove: (Int) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onEdit: () -> Unit
) {
    var dragOffsetY by remember(task.id) { mutableFloatStateOf(0f) }
    val reorderStepPx = 72f
    var menuExpanded by remember { mutableStateOf(false) }

    val dragModifier = if (isDragging) {
        Modifier
            .clip(TaskRowShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f))
            .zIndex(1f)
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = dragOffsetY }
            .then(dragModifier)
            .pointerInput(task.id) {
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
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onToggle) {
            if (task.isDone) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Mark as not done")
            } else {
                Icon(AppIcons.RadioButtonUnchecked, contentDescription = "Mark as done")
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None,
                color = if (task.isDone) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                text = task.category,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (task.isDone && task.completedAt != null) {
                val completedDateText = remember(task.completedAt) {
                    java.time.Instant.ofEpochMilli(task.completedAt)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                        .format(CompletedDateFormatter)
                }
                Text(
                    text = "Completed on $completedDateText",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More actions")
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = { menuExpanded = false; onEdit() },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Transfer") },
                    onClick = { menuExpanded = false; onTransfer() },
                    leadingIcon = { Icon(AppIcons.SwapHoriz, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = { menuExpanded = false; onDelete() },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                )
            }
        }
    }
}

private fun parseReminderDateTimesCsv(value: String?): List<Long> {
    if (value.isNullOrBlank()) return emptyList()
    return value.split("|")
        .mapNotNull { token -> token.trim().toLongOrNull() }
        .distinct()
        .sorted()
}
