package com.example.habittracker.ui.screens

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.habittracker.ui.components.FabScrollClearance
import com.example.habittracker.ui.components.MonthCalendar
import com.example.habittracker.ui.icons.AppIcons
import com.example.habittracker.ui.viewmodel.GlobalDayDetails
import com.example.habittracker.ui.viewmodel.MainViewModel
import com.example.habittracker.repository.HabitRepository.HabitFrequencyType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private data class MainScreenHeaderState(
    val monthLabel: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onOpenMenu: () -> Unit = {},
    viewModel: MainViewModel = hiltViewModel()
) {
    val screenState by viewModel.uiState.collectAsStateWithLifecycle()
    val hasSnapshotContent = screenState.selectedHabitId != null ||
        screenState.scheduledDates.isNotEmpty() ||
        screenState.globalScheduledDates.isNotEmpty() ||
        screenState.globalBirthdayDates.isNotEmpty() ||
        screenState.dayNotesByDate.isNotEmpty()
    val headerState = remember(screenState.selectedMonth) {
        MainScreenHeaderState(
            monthLabel = screenState.selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        )
    }

    var isAddDialogVisible by rememberSaveable { mutableStateOf(false) }
    var habitNameInput by rememberSaveable { mutableStateOf("") }
    var showMoreHabitOptions by rememberSaveable { mutableStateOf(false) }
    var createdDateInput by rememberSaveable { mutableStateOf("") }
    var frequencyTypeInput by rememberSaveable { mutableStateOf(HabitFrequencyType.DAILY.name) }
    var frequencyIntervalInput by rememberSaveable { mutableStateOf("3") }
    var frequencyWeekdaysInput by rememberSaveable { mutableStateOf(setOf(DayOfWeek.MONDAY.value)) }
    var reminderEnabledInput by rememberSaveable { mutableStateOf(false) }
    var reminderTimeInput by rememberSaveable { mutableStateOf("09:00") }
    var reminderTimesInput by remember { mutableStateOf(listOf("09:00")) }
    var reminderMessageInput by rememberSaveable { mutableStateOf("") }
    var createdDateError by rememberSaveable { mutableStateOf(false) }
    var reminderTimeError by rememberSaveable { mutableStateOf(false) }

    var isEditDialogVisible by rememberSaveable { mutableStateOf(false) }
    var editHabitNameInput by rememberSaveable { mutableStateOf("") }
    var editFrequencyTypeInput by rememberSaveable { mutableStateOf(HabitFrequencyType.DAILY.name) }
    var editFrequencyIntervalInput by rememberSaveable { mutableStateOf("3") }
    var editFrequencyWeekdaysInput by rememberSaveable { mutableStateOf(setOf(DayOfWeek.MONDAY.value)) }
    var editReminderEnabledInput by rememberSaveable { mutableStateOf(false) }
    var editReminderTimeInput by rememberSaveable { mutableStateOf("09:00") }
    var editReminderTimesInput by remember { mutableStateOf(listOf("09:00")) }
    var editReminderMessageInput by rememberSaveable { mutableStateOf("") }
    var editReminderTimeError by rememberSaveable { mutableStateOf(false) }

    var isDayNoteDialogVisible by rememberSaveable { mutableStateOf(false) }
    var selectedNoteDate by rememberSaveable { mutableStateOf("") }
    var dayNoteInput by rememberSaveable { mutableStateOf("") }

    var isDeleteHabitConfirmVisible by rememberSaveable { mutableStateOf(false) }
    var isSeedDataConfirmVisible by rememberSaveable { mutableStateOf(false) }
    var isGlobalDayDetailsDialogVisible by rememberSaveable { mutableStateOf(false) }
    var isHabitDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    var isSelectedHabitMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var globalDayDetails by remember { mutableStateOf<GlobalDayDetails?>(null) }
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    fun openEditSelectedHabit() {
        val selectedHabit = screenState.habits.firstOrNull { it.id == screenState.selectedHabitId }
        if (selectedHabit != null) {
            editHabitNameInput = selectedHabit.name
            editFrequencyTypeInput = selectedHabit.frequencyType
            editFrequencyIntervalInput = (selectedHabit.frequencyIntervalDays ?: 3).toString()
            editFrequencyWeekdaysInput = viewModel.parseFrequencyWeekdays(
                selectedHabit.frequencyWeekdays
            ).ifEmpty { setOf(DayOfWeek.MONDAY.value) }
            editReminderEnabledInput = selectedHabit.reminderEnabled
            editReminderTimesInput = viewModel.parseReminderTimesCsv(selectedHabit.reminderTime)
                .ifEmpty { listOf("09:00") }
            editReminderTimeInput = editReminderTimesInput.firstOrNull() ?: "09:00"
            editReminderMessageInput = selectedHabit.reminderMessage.orEmpty()
            editReminderTimeError = false
            isEditDialogVisible = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MyApp") },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "Open navigation menu")
                    }
                },
                actions = {
                    TextButton(onClick = { isSeedDataConfirmVisible = true }) {
                        Text("Seed")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                reminderTimeInput = "09:00"
                reminderTimesInput = listOf("09:00")
                isAddDialogVisible = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add habit")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!screenState.isDataLoaded && !hasSnapshotContent) {
                HomeLoadingState()
            } else if (screenState.habits.isEmpty() && !hasSnapshotContent) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = FabScrollClearance),
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = FabScrollClearance)
                ) {
                    item {
                        Surface(
                            tonalElevation = 1.dp,
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Month overview",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "All habits",
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                MonthCalendar(
                                    month = screenState.selectedMonth,
                                    completedDates = screenState.globalCompletedDates,
                                    scheduledDates = screenState.globalScheduledDates,
                                    birthdayDates = screenState.globalBirthdayDates,
                                    noteDates = screenState.globalNoteDates,
                                    todayDate = screenState.businessToday,
                                    onToggleDate = { date ->
                                        globalDayDetails = viewModel.getGlobalDayDetails(date)
                                        isGlobalDayDetailsDialogVisible = true
                                    },
                                    onOpenDayNote = { },
                                    interactive = screenState.isDataLoaded
                                )
                            }
                        }
                    }

                    item {
                        Surface(
                            tonalElevation = 2.dp,
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val selectedHabitLabel = screenState.habits.firstOrNull { it.id == screenState.selectedHabitId }?.let { habit ->
                                    "${habit.name} \uD83D\uDD25 ${habit.currentStreak}"
                                } ?: if (hasSnapshotContent) "Saved snapshot" else "Select habit"
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
                                        text = headerState.monthLabel,
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

                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { isHabitDropdownExpanded = true },
                                            enabled = screenState.habits.isNotEmpty(),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(selectedHabitLabel)
                                        }
                                        Box {
                                            IconButton(
                                                enabled = screenState.isDataLoaded && screenState.selectedHabitId != null,
                                                onClick = { isSelectedHabitMenuExpanded = true }
                                            ) {
                                                Icon(
                                                    Icons.Default.MoreVert,
                                                    contentDescription = "Open habit actions"
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = isSelectedHabitMenuExpanded,
                                                onDismissRequest = { isSelectedHabitMenuExpanded = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Edit habit") },
                                                    onClick = {
                                                        isSelectedHabitMenuExpanded = false
                                                        openEditSelectedHabit()
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Delete habit") },
                                                    onClick = {
                                                        isSelectedHabitMenuExpanded = false
                                                        isDeleteHabitConfirmVisible = true
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = isHabitDropdownExpanded,
                                        onDismissRequest = { isHabitDropdownExpanded = false },
                                        modifier = Modifier.fillMaxWidth(0.94f)
                                    ) {
                                        screenState.habits.forEach { habitOption ->
                                            DropdownMenuItem(
                                                text = { Text("${habitOption.name} \uD83D\uDD25 ${habitOption.currentStreak}") },
                                                onClick = {
                                                    isHabitDropdownExpanded = false
                                                    viewModel.selectHabit(habitOption.id)
                                                }
                                            )
                                        }
                                    }
                                }

                                MonthCalendar(
                                    month = screenState.selectedMonth,
                                    completedDates = screenState.completedDates,
                                    scheduledDates = screenState.scheduledDates,
                                    birthdayDates = emptySet(),
                                    noteDates = screenState.dayNotesByDate.keys,
                                    todayDate = screenState.businessToday,
                                    onToggleDate = viewModel::toggleDay,
                                    onOpenDayNote = { date ->
                                        selectedNoteDate = date
                                        dayNoteInput = screenState.dayNotesByDate[date].orEmpty()
                                        isDayNoteDialogVisible = true
                                    },
                                    interactive = screenState.isDataLoaded
                                )
                            }
                        }
                    }
                }
            }
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
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val current = runCatching { LocalDate.parse(createdDateInput) }.getOrNull()
                                        ?: LocalDate.now()
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            createdDateInput = LocalDate.of(year, month + 1, dayOfMonth).toString()
                                            createdDateError = false
                                        },
                                        current.year,
                                        current.monthValue - 1,
                                        current.dayOfMonth
                                    ).show()
                                }
                        ) {
                            OutlinedTextField(
                                value = createdDateInput,
                                onValueChange = {},
                                singleLine = true,
                                enabled = false,
                                isError = createdDateError,
                                label = { Text("Created date (YYYY-MM-DD)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                                    disabledSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                trailingIcon = {
                                    Icon(AppIcons.CalendarToday, contentDescription = "Select created date")
                                },
                                supportingText = {
                                    Text(
                                        if (createdDateError) "Use format YYYY-MM-DD"
                                        else "Leave empty to use today"
                                    )
                                }
                            )
                        }

                        Text(
                            text = "Frequency",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                HabitFrequencyType.DAILY.name to "Daily",
                                HabitFrequencyType.WEEKLY.name to "Weekly",
                                HabitFrequencyType.EVERY_N_DAYS.name to "Every N days"
                            ).forEach { (value, label) ->
                                AssistChip(
                                    onClick = { frequencyTypeInput = value },
                                    label = { Text(label) },
                                    colors = if (frequencyTypeInput == value) {
                                        AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    } else {
                                        AssistChipDefaults.assistChipColors()
                                    }
                                )
                            }
                        }

                        if (frequencyTypeInput == HabitFrequencyType.WEEKLY.name) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val weekdayItems = listOf(
                                    DayOfWeek.MONDAY.value to "Mon",
                                    DayOfWeek.TUESDAY.value to "Tue",
                                    DayOfWeek.WEDNESDAY.value to "Wed",
                                    DayOfWeek.THURSDAY.value to "Thu",
                                    DayOfWeek.FRIDAY.value to "Fri",
                                    DayOfWeek.SATURDAY.value to "Sat",
                                    DayOfWeek.SUNDAY.value to "Sun"
                                )
                                weekdayItems.forEach { (dayValue, dayLabel) ->
                                    val isSelectedDay = frequencyWeekdaysInput.contains(dayValue)
                                    AssistChip(
                                        onClick = {
                                            frequencyWeekdaysInput =
                                                if (isSelectedDay) frequencyWeekdaysInput - dayValue
                                                else frequencyWeekdaysInput + dayValue
                                        },
                                        label = { Text(dayLabel) },
                                        colors = if (isSelectedDay) {
                                            AssistChipDefaults.assistChipColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer
                                            )
                                        } else {
                                            AssistChipDefaults.assistChipColors()
                                        }
                                    )
                                }
                            }
                        }

                        if (frequencyTypeInput == HabitFrequencyType.EVERY_N_DAYS.name) {
                            OutlinedTextField(
                                value = frequencyIntervalInput,
                                onValueChange = { value ->
                                    frequencyIntervalInput = value.filter { it.isDigit() }.take(3)
                                },
                                singleLine = true,
                                label = { Text("Every how many days") },
                                supportingText = { Text("Example: 3 means every 3 days") },
                                modifier = Modifier.fillMaxWidth()
                            )
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
                                OutlinedTextField(
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
                                        Icon(AppIcons.AccessTime, contentDescription = "Select reminder time")
                                    }
                                )
                            }

                            TextButton(
                                onClick = {
                                    if (runCatching { LocalTime.parse(reminderTimeInput) }.isSuccess) {
                                        reminderTimesInput = (reminderTimesInput + reminderTimeInput).distinct().sorted()
                                        reminderTimeError = false
                                    } else {
                                        reminderTimeError = true
                                    }
                                }
                            ) { Text("Add reminder time") }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                reminderTimesInput.forEach { timeValue ->
                                    AssistChip(
                                        onClick = {
                                            reminderTimesInput = reminderTimesInput - timeValue
                                        },
                                        label = { Text(timeValue) }
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = reminderMessageInput,
                                onValueChange = { reminderMessageInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Reminder message (optional)") },
                                minLines = 2,
                                maxLines = 4
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (habitNameInput.isNotBlank()) {
                            val ok = viewModel.addHabit(
                                habitNameInput,
                                createdDateInput.takeIf { showMoreHabitOptions },
                                frequencyTypeInput,
                                frequencyIntervalInput.toIntOrNull(),
                                frequencyWeekdaysInput,
                                reminderEnabledInput,
                                reminderTimesInput.joinToString(","),
                                reminderMessageInput.takeIf {
                                    showMoreHabitOptions && reminderEnabledInput
                                }
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
                                habitNameInput = ""
                                createdDateInput = ""
                                showMoreHabitOptions = false
                                frequencyTypeInput = HabitFrequencyType.DAILY.name
                                frequencyIntervalInput = "3"
                                frequencyWeekdaysInput = setOf(DayOfWeek.MONDAY.value)
                                reminderEnabledInput = false
                                reminderTimeInput = "09:00"
                                reminderTimesInput = listOf("09:00")
                                reminderMessageInput = ""
                                createdDateError = false
                                reminderTimeError = false
                                isAddDialogVisible = false
                            } else {
                                createdDateError = false
                                reminderTimeError = reminderEnabledInput && reminderTimesInput.isEmpty()
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

    if (isSeedDataConfirmVisible) {
        AlertDialog(
            onDismissRequest = { isSeedDataConfirmVisible = false },
            title = { Text("Seed test data") },
            text = {
                Text("This will add a large set of demo habits, completions, notes, tasks, goals, birthdays, and reminders for testing.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isSeedDataConfirmVisible = false
                        viewModel.seedTestData()
                    }
                ) {
                    Text("Add data")
                }
            },
            dismissButton = {
                TextButton(onClick = { isSeedDataConfirmVisible = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (isEditDialogVisible) {
        AlertDialog(
            onDismissRequest = { isEditDialogVisible = false },
            title = { Text("Edit habit") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editHabitNameInput,
                        onValueChange = { editHabitNameInput = it },
                        singleLine = true,
                        label = { Text("Habit name") }
                    )

                    Text(
                        text = "Frequency",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            HabitFrequencyType.DAILY.name to "Daily",
                            HabitFrequencyType.WEEKLY.name to "Weekly",
                            HabitFrequencyType.EVERY_N_DAYS.name to "Every N days"
                        ).forEach { (value, label) ->
                            AssistChip(
                                onClick = { editFrequencyTypeInput = value },
                                label = { Text(label) },
                                colors = if (editFrequencyTypeInput == value) {
                                    AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                } else {
                                    AssistChipDefaults.assistChipColors()
                                }
                            )
                        }
                    }

                    if (editFrequencyTypeInput == HabitFrequencyType.WEEKLY.name) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val weekdayItems = listOf(
                                DayOfWeek.MONDAY.value to "Mon",
                                DayOfWeek.TUESDAY.value to "Tue",
                                DayOfWeek.WEDNESDAY.value to "Wed",
                                DayOfWeek.THURSDAY.value to "Thu",
                                DayOfWeek.FRIDAY.value to "Fri",
                                DayOfWeek.SATURDAY.value to "Sat",
                                DayOfWeek.SUNDAY.value to "Sun"
                            )
                            weekdayItems.forEach { (dayValue, dayLabel) ->
                                val isSelectedDay = editFrequencyWeekdaysInput.contains(dayValue)
                                AssistChip(
                                    onClick = {
                                        editFrequencyWeekdaysInput =
                                            if (isSelectedDay) editFrequencyWeekdaysInput - dayValue
                                            else editFrequencyWeekdaysInput + dayValue
                                    },
                                    label = { Text(dayLabel) },
                                    colors = if (isSelectedDay) {
                                        AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    } else {
                                        AssistChipDefaults.assistChipColors()
                                    }
                                )
                            }
                        }
                    }

                    if (editFrequencyTypeInput == HabitFrequencyType.EVERY_N_DAYS.name) {
                        OutlinedTextField(
                            value = editFrequencyIntervalInput,
                            onValueChange = { value ->
                                editFrequencyIntervalInput = value.filter { it.isDigit() }.take(3)
                            },
                            singleLine = true,
                            label = { Text("Every how many days") },
                            supportingText = { Text("Example: 3 means every 3 days") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

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
                            OutlinedTextField(
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
                                    Icon(AppIcons.AccessTime, contentDescription = "Select reminder time")
                                }
                            )
                        }

                        TextButton(
                            onClick = {
                                if (runCatching { LocalTime.parse(editReminderTimeInput) }.isSuccess) {
                                    editReminderTimesInput = (editReminderTimesInput + editReminderTimeInput).distinct().sorted()
                                    editReminderTimeError = false
                                } else {
                                    editReminderTimeError = true
                                }
                            }
                        ) { Text("Add reminder time") }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            editReminderTimesInput.forEach { timeValue ->
                                AssistChip(
                                    onClick = { editReminderTimesInput = editReminderTimesInput - timeValue },
                                    label = { Text(timeValue) }
                                )
                            }
                        }

                        OutlinedTextField(
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
                        val ok = viewModel.updateSelectedHabit(
                            name = editHabitNameInput,
                            frequencyType = editFrequencyTypeInput,
                            frequencyIntervalDays = editFrequencyIntervalInput.toIntOrNull(),
                            frequencyWeekdays = editFrequencyWeekdaysInput,
                            reminderEnabled = editReminderEnabledInput,
                            reminderTime = editReminderTimesInput.joinToString(","),
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
                            isEditDialogVisible = false
                        } else {
                            editReminderTimeError = editReminderEnabledInput && editReminderTimesInput.isEmpty()
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { isEditDialogVisible = false }) { Text("Cancel") }
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

    if (isGlobalDayDetailsDialogVisible) {
        AlertDialog(
            onDismissRequest = { isGlobalDayDetailsDialogVisible = false },
            title = { Text("Details for ${globalDayDetails?.date.orEmpty()}") },
            text = {
                val details = globalDayDetails
                if (details == null) {
                    Text("No details available.")
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Birthdays",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (details.birthdayNames.isEmpty()) {
                            Text(
                                text = "No birthdays",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            details.birthdayNames.forEach { birthdayName ->
                                Text(text = "🎂 $birthdayName")
                            }
                        }

                        Text(
                            text = "Habits",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (details.habits.isEmpty()) {
                            Text(
                                text = "No habits scheduled for this date",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            details.habits.forEach { habitDetail ->
                                Text(
                                    text = if (habitDetail.isDone) {
                                        "✓ ${habitDetail.habitName}"
                                    } else {
                                        "✕ ${habitDetail.habitName}"
                                    }
                                )
                                Text(
                                    text = "Note: ${habitDetail.note?.takeIf { it.isNotBlank() } ?: "No note"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = "Completed tasks",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (details.completedTasks.isEmpty()) {
                            Text(
                                text = "No completed tasks",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            details.completedTasks.forEach { taskTitle ->
                                Text(text = "✓ $taskTitle")
                            }
                        }

                        Text(
                            text = "Money",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (details.moneyEntries.isEmpty()) {
                            Text(
                                text = "No payments",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val totalSpent = details.moneyEntries
                                .filterNot { it.isIncome }
                                .sumOf { it.amount }
                            Text(
                                text = "Spent total: ${String.format("%.2f", totalSpent)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            details.moneyEntries.forEach { entry ->
                                val sign = if (entry.isIncome) "+" else "-"
                                Text(
                                    text = "$sign ${entry.title} (${entry.category}) - ${String.format("%.2f", entry.amount)}"
                                )
                            }
                        }

                        Text(
                            text = "Weight",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = details.weightKg?.let { String.format("%.1f kg", it) } ?: "No weight logged",
                            color = if (details.weightKg == null) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )

                        Text(
                            text = "Mood",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (details.mood == null) {
                            Text(
                                text = "No mood logged",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(details.mood.mood)
                            if (!details.mood.note.isNullOrBlank()) {
                                Text(
                                    text = details.mood.note.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { isGlobalDayDetailsDialogVisible = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun HomeLoadingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Loading habits...",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        HomeCalendarPlaceholder()
        HomeCalendarPlaceholder()
    }
}

@Composable
private fun HomeCalendarPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(6) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(7) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 2.dp),
                                tonalElevation = 1.dp,
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
