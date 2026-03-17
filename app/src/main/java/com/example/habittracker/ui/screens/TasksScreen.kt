package com.example.habittracker.ui.screens

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.habittracker.repository.HabitRepository
import com.example.habittracker.ui.components.AppModalSheet
import com.example.habittracker.ui.components.EmptyStateCard
import com.example.habittracker.ui.components.PillLabel
import com.example.habittracker.ui.components.PremiumCard
import com.example.habittracker.ui.components.ScreenBackground
import com.example.habittracker.ui.components.SectionHeader
import com.example.habittracker.ui.components.SummaryChip
import com.example.habittracker.ui.components.SurfaceCard
import com.example.habittracker.ui.icons.AppIcons
import com.example.habittracker.ui.theme.MyAppTheme
import com.example.habittracker.ui.viewmodel.ALL_CATEGORIES
import com.example.habittracker.ui.viewmodel.TaskUiItem
import com.example.habittracker.ui.viewmodel.TasksViewModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val ReminderDateTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(viewModel: TasksViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    var isEditorVisible by rememberSaveable { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskUiItem?>(null) }
    var taskInput by rememberSaveable { mutableStateOf("") }
    var taskCategoryInput by rememberSaveable { mutableStateOf(HabitRepository.DEFAULT_TASK_CATEGORY) }
    var reminderEnabledInput by rememberSaveable { mutableStateOf(false) }
    var reminderDateTimesInput by remember { mutableStateOf(listOf<Long>()) }
    var reminderMessageInput by rememberSaveable { mutableStateOf("") }
    var reminderSelectionError by rememberSaveable { mutableStateOf(false) }
    var completedExpanded by rememberSaveable { mutableStateOf(false) }
    var isCategorySheetVisible by rememberSaveable { mutableStateOf(false) }
    var newCategoryInput by rememberSaveable { mutableStateOf("") }
    var pendingDeleteTask by remember { mutableStateOf<TaskUiItem?>(null) }
    var pendingDeleteCategory by rememberSaveable { mutableStateOf<String?>(null) }

    val dueSoonCount = remember(state.pending) {
        state.pending.count { parseReminderDateTimesCsv(it.reminderDateTimesCsv).isNotEmpty() }
    }

    fun resetEditor(target: TaskUiItem? = null) {
        editingTask = target
        taskInput = target?.title.orEmpty()
        taskCategoryInput = target?.category ?: if (state.selectedCategory == ALL_CATEGORIES) {
            HabitRepository.DEFAULT_TASK_CATEGORY
        } else {
            state.selectedCategory
        }
        reminderEnabledInput = target?.reminderEnabled == true
        reminderDateTimesInput = parseReminderDateTimesCsv(target?.reminderDateTimesCsv)
        reminderMessageInput = target?.reminderMessage.orEmpty()
        reminderSelectionError = false
    }

    fun openReminderPicker(onSelected: (Long) -> Unit) {
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
                    IconButton(onClick = { isCategorySheetVisible = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Manage categories")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    resetEditor()
                    isEditorVisible = true
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add task")
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
                    PremiumCard(accent = MaterialTheme.colorScheme.primary) {
                        SectionHeader(
                            title = "Execution board",
                            subtitle = "Keep pending work visible and move completed items out of the way."
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SummaryChip(text = "Pending ${state.pending.size}", selected = true)
                            SummaryChip(text = "With reminders $dueSoonCount")
                            SummaryChip(text = "Done ${state.done.size}", onClick = { completedExpanded = !completedExpanded })
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryChip(
                            text = ALL_CATEGORIES,
                            selected = state.selectedCategory == ALL_CATEGORIES,
                            onClick = { viewModel.selectCategory(ALL_CATEGORIES) }
                        )
                        state.categories.forEach { category ->
                            SummaryChip(
                                text = category,
                                selected = state.selectedCategory == category,
                                onClick = { viewModel.selectCategory(category) }
                            )
                        }
                    }
                }

                item {
                    SectionHeader(
                        title = "Pending",
                        subtitle = if (state.selectedCategory == ALL_CATEGORIES) "Across all categories" else state.selectedCategory
                    )
                }

                if (state.pending.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "Nothing pending",
                            message = "Your active list is clear. Add the next task before it becomes mental overhead.",
                            actionLabel = "Add task",
                            onAction = {
                                resetEditor()
                                isEditorVisible = true
                            }
                        )
                    }
                } else {
                    items(state.pending, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onToggle = { viewModel.toggleTask(task) },
                            onEdit = {
                                resetEditor(task)
                                isEditorVisible = true
                            },
                            onDelete = { pendingDeleteTask = task }
                        )
                    }
                }

                item {
                    SectionHeader(
                        title = "Completed",
                        subtitle = if (completedExpanded) "Showing completed tasks" else "Collapsed to reduce noise",
                        trailing = {
                            TextButton(onClick = { completedExpanded = !completedExpanded }) {
                                Text(if (completedExpanded) "Hide" else "Show")
                            }
                        }
                    )
                }

                if (completedExpanded) {
                    if (state.done.isEmpty()) {
                        item {
                            SurfaceCard {
                                Text(
                                    "No completed tasks yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(state.done, key = { it.id }) { task ->
                            TaskCard(
                                task = task,
                                onToggle = { viewModel.toggleTask(task) },
                                onEdit = {
                                    resetEditor(task)
                                    isEditorVisible = true
                                },
                                onDelete = { pendingDeleteTask = task }
                            )
                        }
                    }
                }
            }
        }
    }

    if (isEditorVisible) {
        AppModalSheet(onDismissRequest = { isEditorVisible = false }) {
            SectionHeader(
                title = if (editingTask == null) "Add task" else "Edit task",
                subtitle = "Strong default structure, optional reminders."
            )
            OutlinedTextField(
                value = taskInput,
                onValueChange = {
                    taskInput = it
                    reminderSelectionError = false
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Task") },
                singleLine = true
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Category", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.categories.forEach { category ->
                        SummaryChip(
                            text = category,
                            selected = taskCategoryInput == category,
                            onClick = { taskCategoryInput = category }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Reminders", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Optional one-time notifications",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = reminderEnabledInput,
                    onCheckedChange = { reminderEnabledInput = it }
                )
            }

            if (reminderEnabledInput) {
                TextButton(
                    onClick = {
                        openReminderPicker { millis ->
                            reminderDateTimesInput = (reminderDateTimesInput + millis).distinct().sorted()
                            reminderSelectionError = false
                        }
                    }
                ) {
                    Text("Add reminder date/time")
                }
                if (reminderDateTimesInput.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        reminderDateTimesInput.forEach { millis ->
                            PillLabel(
                                text = Instant.ofEpochMilli(millis)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDateTime()
                                    .format(ReminderDateTimeFormat),
                                color = MyAppTheme.extraColors.warning,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                if (reminderSelectionError && reminderDateTimesInput.isEmpty()) {
                    Text(
                        text = "Add at least one reminder date/time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                OutlinedTextField(
                    value = reminderMessageInput,
                    onValueChange = { reminderMessageInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Reminder message") },
                    minLines = 2
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { isEditorVisible = false }) { Text("Cancel") }
                TextButton(
                    onClick = {
                        if (taskInput.isBlank()) return@TextButton
                        val ok = if (editingTask == null) {
                            viewModel.addTask(
                                title = taskInput.trim(),
                                category = taskCategoryInput,
                                reminderEnabled = reminderEnabledInput,
                                reminderTime = null,
                                reminderDateTimesCsv = reminderDateTimesInput.joinToString("|"),
                                reminderMessage = reminderMessageInput.takeIf { reminderEnabledInput }
                            )
                        } else {
                            viewModel.updateTask(
                                taskId = editingTask!!.id,
                                title = taskInput.trim(),
                                category = taskCategoryInput,
                                reminderEnabled = reminderEnabledInput,
                                reminderTime = null,
                                reminderDateTimesCsv = reminderDateTimesInput.joinToString("|"),
                                reminderMessage = reminderMessageInput.takeIf { reminderEnabledInput }
                            )
                        }
                        if (!ok) {
                            reminderSelectionError = reminderEnabledInput && reminderDateTimesInput.isEmpty()
                            return@TextButton
                        }
                        if (reminderEnabledInput && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (!granted) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                        isEditorVisible = false
                    }
                ) {
                    Text(if (editingTask == null) "Save" else "Update")
                }
            }
        }
    }

    if (isCategorySheetVisible) {
        AppModalSheet(onDismissRequest = { isCategorySheetVisible = false }) {
            SectionHeader(title = "Categories", subtitle = "Keep the task board simple and intentional.")
            OutlinedTextField(
                value = newCategoryInput,
                onValueChange = { newCategoryInput = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("New category") },
                singleLine = true
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        if (viewModel.addCategory(newCategoryInput)) {
                            newCategoryInput = ""
                        }
                    }
                ) { Text("Add") }
            }
            state.categories.forEach { category ->
                SurfaceCard(contentPadding = PaddingValues(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(category, style = MaterialTheme.typography.titleMedium)
                        if (category != HabitRepository.DEFAULT_TASK_CATEGORY) {
                            IconButton(onClick = { pendingDeleteCategory = category }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete category")
                            }
                        }
                    }
                }
            }
        }
    }

    if (pendingDeleteTask != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteTask = null },
            title = { Text("Delete task") },
            text = { Text("Delete ${pendingDeleteTask?.title}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDeleteTask?.let(viewModel::deleteTask)
                        pendingDeleteTask = null
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteTask = null }) { Text("Cancel") }
            }
        )
    }

    if (pendingDeleteCategory != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteCategory = null },
            title = { Text("Delete category") },
            text = { Text("Delete ${pendingDeleteCategory} and its tasks?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDeleteCategory?.let(viewModel::deleteCategory)
                        pendingDeleteCategory = null
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteCategory = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun TaskCard(
    task: TaskUiItem,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val reminderTimes = remember(task.reminderDateTimesCsv) { parseReminderDateTimesCsv(task.reminderDateTimesCsv) }
    SurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = if (task.isDone) Icons.Default.CheckCircle else AppIcons.RadioButtonUnchecked,
                        contentDescription = "Toggle task"
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        PillLabel(
                            text = task.category,
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                        if (reminderTimes.isNotEmpty()) {
                            PillLabel(
                                text = Instant.ofEpochMilli(reminderTimes.first())
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDateTime()
                                    .format(ReminderDateTimeFormat),
                                color = MyAppTheme.extraColors.warning,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    if (task.isDone && task.completedAt != null) {
                        Text(
                            text = "Completed ${
                                Instant.ofEpochMilli(task.completedAt)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                                    .format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                            }",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit task")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete task")
                }
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
