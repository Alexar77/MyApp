package com.example.habittracker.ui.screens

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
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
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.habittracker.notifications.ReminderReceiver
import com.example.habittracker.ui.components.MonthCalendar
import com.example.habittracker.ui.viewmodel.MainViewModel
import com.example.habittracker.repository.HabitRepository.HabitFrequencyType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
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
    var frequencyTypeInput by rememberSaveable { mutableStateOf(HabitFrequencyType.DAILY.name) }
    var frequencyIntervalInput by rememberSaveable { mutableStateOf("3") }
    var frequencyWeekdaysInput by rememberSaveable { mutableStateOf(setOf(DayOfWeek.MONDAY.value)) }
    var reminderEnabledInput by rememberSaveable { mutableStateOf(false) }
    var reminderTimeInput by rememberSaveable { mutableStateOf("09:00") }
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
    var editReminderMessageInput by rememberSaveable { mutableStateOf("") }
    var editReminderTimeError by rememberSaveable { mutableStateOf(false) }

    var isDayNoteDialogVisible by rememberSaveable { mutableStateOf(false) }
    var selectedNoteDate by rememberSaveable { mutableStateOf("") }
    var dayNoteInput by rememberSaveable { mutableStateOf("") }

    var isDeleteHabitConfirmVisible by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

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
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                                if (!granted) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    return@IconButton
                                }
                            }
                            sendRandomTestNotification(context)
                        }
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = "Send test notification")
                    }
                    IconButton(
                        enabled = screenState.selectedHabitId != null,
                        onClick = {
                            val selectedHabit = screenState.habits.firstOrNull { it.id == screenState.selectedHabitId }
                            if (selectedHabit != null) {
                                editHabitNameInput = selectedHabit.name
                                editFrequencyTypeInput = selectedHabit.frequencyType
                                editFrequencyIntervalInput = (selectedHabit.frequencyIntervalDays ?: 3).toString()
                                editFrequencyWeekdaysInput = viewModel.parseFrequencyWeekdays(
                                    selectedHabit.frequencyWeekdays
                                ).ifEmpty { setOf(DayOfWeek.MONDAY.value) }
                                editReminderEnabledInput = selectedHabit.reminderEnabled
                                editReminderTimeInput = selectedHabit.reminderTime ?: "09:00"
                                editReminderMessageInput = selectedHabit.reminderMessage.orEmpty()
                                editReminderTimeError = false
                                isEditDialogVisible = true
                            }
                        }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit selected habit")
                    }
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "All habits 🔥 ${screenState.globalCurrentStreak}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                                MonthCalendar(
                                    month = screenState.selectedMonth,
                                    completedDates = screenState.globalCompletedDates,
                                    scheduledDates = screenState.globalScheduledDates,
                                    noteDates = emptySet(),
                                    createdDate = null,
                                    todayDate = screenState.businessToday,
                                    onToggleDate = { },
                                    onOpenDayNote = { }
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Select habit",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    item {
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
                                    label = { Text("${habitOption.name} 🔥 ${habitOption.currentStreak}") },
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

                                MonthCalendar(
                                    month = screenState.selectedMonth,
                                    completedDates = screenState.completedDates,
                                    scheduledDates = screenState.scheduledDates,
                                    noteDates = screenState.dayNotesByDate.keys,
                                    createdDate = screenState.selectedHabitCreatedDate,
                                    todayDate = screenState.businessToday,
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
                                    Icon(Icons.Default.CalendarToday, contentDescription = "Select created date")
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
                                        Icon(Icons.Default.AccessTime, contentDescription = "Select reminder time")
                                    }
                                )
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
                                reminderTimeInput,
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
                                reminderMessageInput = ""
                                createdDateError = false
                                reminderTimeError = false
                                isAddDialogVisible = false
                            } else {
                                createdDateError = false
                                reminderTimeError = reminderEnabledInput &&
                                    runCatching { LocalTime.parse(reminderTimeInput) }.isFailure
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
                                    Icon(Icons.Default.AccessTime, contentDescription = "Select reminder time")
                                }
                            )
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
                            isEditDialogVisible = false
                        } else {
                            editReminderTimeError = editReminderEnabledInput
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
}

private fun sendRandomTestNotification(context: android.content.Context) {
    val titles = listOf(
        "Quick test",
        "MyApp reminder",
        "Notification check"
    )
    val messages = listOf(
        "This is a random test notification.",
        "Your notification setup works.",
        "Test ping from Home screen."
    )

    val now = java.time.LocalTime.now()
    val timeValue = String.format("%02d:%02d", now.hour, now.minute)
    val intent = Intent(context, ReminderReceiver::class.java).apply {
        putExtra(ReminderReceiver.EXTRA_UNIQUE_KEY, "test:${System.currentTimeMillis()}")
        putExtra(ReminderReceiver.EXTRA_TIME, timeValue)
        putExtra(ReminderReceiver.EXTRA_TITLE, titles.random())
        putExtra(ReminderReceiver.EXTRA_MESSAGE, messages.random())
        putExtra(ReminderReceiver.EXTRA_SKIP_RESCHEDULE, true)
    }
    context.sendBroadcast(intent)
}
