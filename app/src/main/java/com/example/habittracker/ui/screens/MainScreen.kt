package com.example.habittracker.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.habittracker.ui.components.MonthCalendar
import com.example.habittracker.ui.viewmodel.MainViewModel
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    val screenState by viewModel.uiState.collectAsStateWithLifecycle()

    var isAddDialogVisible by rememberSaveable { mutableStateOf(false) }
    var habitNameInput by rememberSaveable { mutableStateOf("") }
    var showMoreHabitOptions by rememberSaveable { mutableStateOf(false) }
    var createdDateInput by rememberSaveable { mutableStateOf("") }
    var createdDateError by rememberSaveable { mutableStateOf(false) }

    var isDayNoteDialogVisible by rememberSaveable { mutableStateOf(false) }
    var selectedNoteDate by rememberSaveable { mutableStateOf("") }
    var dayNoteInput by rememberSaveable { mutableStateOf("") }

    var isDeleteHabitConfirmVisible by rememberSaveable { mutableStateOf(false) }

    var isRefreshing by rememberSaveable { mutableStateOf(false) }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
        }
    )

    if (isRefreshing) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            delay(650)
            isRefreshing = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MyApp") },
                actions = {
                    IconButton(
                        enabled = screenState.selectedHabitId != null,
                        onClick = { isDeleteHabitConfirmVisible = true }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete selected habit")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { isAddDialogVisible = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add habit")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pullRefresh(pullRefreshState)
        ) {
            if (screenState.habits.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No habits yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Tap + to create your first habit",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Select habit",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        screenState.habits.forEach { habitOption ->
                            val isSelectedHabit = screenState.selectedHabitId == habitOption.id
                            AssistChip(
                                onClick = { viewModel.selectHabit(habitOption.id) },
                                label = { Text(habitOption.name) },
                                colors = if (isSelectedHabit) {
                                    AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                } else {
                                    AssistChipDefaults.assistChipColors()
                                }
                            )
                        }
                    }

                    Surface(
                        tonalElevation = 2.dp,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(onClick = { viewModel.showPreviousMonth() }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Previous month"
                                    )
                                }
                                Text(
                                    text = screenState.selectedMonth.format(
                                        DateTimeFormatter.ofPattern("MMMM yyyy")
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                IconButton(onClick = { viewModel.showNextMonth() }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Next month"
                                    )
                                }
                            }

                            Text(
                                text = "Tap day to toggle, long-press day to write a note",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            MonthCalendar(
                                month = screenState.selectedMonth,
                                completedDates = screenState.completedDates,
                                noteDates = screenState.dayNotesByDate.keys,
                                onToggleDate = viewModel::toggleDay,
                                onOpenDayNote = { date ->
                                    selectedNoteDate = date
                                    dayNoteInput = screenState.dayNotesByDate[date].orEmpty()
                                    isDayNoteDialogVisible = true
                                }
                            )
                        }
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
            title = { Text("Create habit") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = habitNameInput,
                        onValueChange = { habitNameInput = it },
                        singleLine = true,
                        label = { Text("Habit name") }
                    )

                    TextButton(onClick = { showMoreHabitOptions = !showMoreHabitOptions }) {
                        Text(if (showMoreHabitOptions) "Hide options" else "More options")
                    }

                    if (showMoreHabitOptions) {
                        OutlinedTextField(
                            value = createdDateInput,
                            onValueChange = {
                                createdDateInput = it
                                createdDateError = false
                            },
                            singleLine = true,
                            isError = createdDateError,
                            label = { Text("Created date (YYYY-MM-DD)") },
                            supportingText = {
                                Text(
                                    if (createdDateError) "Use format YYYY-MM-DD"
                                    else "Leave empty to use today"
                                )
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (habitNameInput.isNotBlank()) {
                            val ok = viewModel.addHabit(
                                habitNameInput,
                                createdDateInput.takeIf { showMoreHabitOptions }
                            )
                            if (ok) {
                                habitNameInput = ""
                                createdDateInput = ""
                                showMoreHabitOptions = false
                                createdDateError = false
                                isAddDialogVisible = false
                            } else {
                                createdDateError = true
                            }
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { isAddDialogVisible = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (isDeleteHabitConfirmVisible) {
        AlertDialog(
            onDismissRequest = { isDeleteHabitConfirmVisible = false },
            title = { Text("Delete habit") },
            text = { Text("Do you want to delete this habit?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSelectedHabit()
                        isDeleteHabitConfirmVisible = false
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { isDeleteHabitConfirmVisible = false }) { Text("Cancel") }
            }
        )
    }

    if (isDayNoteDialogVisible) {
        AlertDialog(
            onDismissRequest = { isDayNoteDialogVisible = false },
            title = { Text("Day note: $selectedNoteDate") },
            text = {
                OutlinedTextField(
                    value = dayNoteInput,
                    onValueChange = { dayNoteInput = it },
                    label = { Text("Note") },
                    minLines = 5,
                    maxLines = Int.MAX_VALUE
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.saveDayNote(selectedNoteDate, dayNoteInput)
                        isDayNoteDialogVisible = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { isDayNoteDialogVisible = false }) {
                    Text("Close")
                }
            }
        )
    }
}
