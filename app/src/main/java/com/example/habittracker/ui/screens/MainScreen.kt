package com.example.habittracker.ui.screens

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.habittracker.repository.HabitRepository.HabitFrequencyType
import com.example.habittracker.ui.components.AppModalSheet
import com.example.habittracker.ui.components.EmptyStateCard
import com.example.habittracker.ui.components.LoadingRow
import com.example.habittracker.ui.components.PillLabel
import com.example.habittracker.ui.components.PremiumCard
import com.example.habittracker.ui.components.ScreenBackground
import com.example.habittracker.ui.components.SectionHeader
import com.example.habittracker.ui.components.SummaryChip
import com.example.habittracker.ui.components.SurfaceCard
import com.example.habittracker.ui.components.InlineProgressLabel
import com.example.habittracker.ui.icons.AppIcons
import com.example.habittracker.ui.theme.MyAppTheme
import com.example.habittracker.ui.viewmodel.HabitOptionUiState
import com.example.habittracker.ui.viewmodel.MainViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onOpenBirthdays: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenMotivation: () -> Unit = {},
    onOpenTasks: () -> Unit = {},
    onOpenStats: (Long) -> Unit = {},
    viewModel: MainViewModel = hiltViewModel()
) {
    val screenState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    val selectedHabit = remember(screenState.habits, screenState.selectedHabitId) {
        screenState.habits.firstOrNull { it.id == screenState.selectedHabitId }
    }
    val monthLabel = remember(screenState.selectedMonth) {
        screenState.selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }
    val completionCount = screenState.completedDates.size
    val scheduledCount = screenState.scheduledDates.size
    val selectedToday = screenState.businessToday.toString()
    val todayNote = screenState.dayNotesByDate[selectedToday].orEmpty()

    var isHabitEditorVisible by rememberSaveable { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<HabitOptionUiState?>(null) }
    var habitNameInput by rememberSaveable { mutableStateOf("") }
    var createdDateInput by rememberSaveable { mutableStateOf("") }
    var frequencyTypeInput by rememberSaveable { mutableStateOf(HabitFrequencyType.DAILY.name) }
    var frequencyIntervalInput by rememberSaveable { mutableStateOf("3") }
    var frequencyWeekdaysInput by rememberSaveable { mutableStateOf(setOf(DayOfWeek.MONDAY.value)) }
    var reminderEnabledInput by rememberSaveable { mutableStateOf(false) }
    var reminderTimesInput by remember { mutableStateOf(listOf("09:00")) }
    var reminderMessageInput by rememberSaveable { mutableStateOf("") }
    var createdDateError by rememberSaveable { mutableStateOf(false) }
    var reminderTimeError by rememberSaveable { mutableStateOf(false) }

    var isDayNoteSheetVisible by rememberSaveable { mutableStateOf(false) }
    var selectedNoteDate by rememberSaveable { mutableStateOf(selectedToday) }
    var dayNoteInput by rememberSaveable { mutableStateOf("") }
    var isDeleteConfirmVisible by rememberSaveable { mutableStateOf(false) }
    var isQuickActionsVisible by rememberSaveable { mutableStateOf(false) }

    fun resetHabitEditor(target: HabitOptionUiState?) {
        editingHabit = target
        habitNameInput = target?.name.orEmpty()
        createdDateInput = target?.let {
            Instant.ofEpochMilli(it.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
        }?.toString().orEmpty()
        frequencyTypeInput = target?.frequencyType ?: HabitFrequencyType.DAILY.name
        frequencyIntervalInput = (target?.frequencyIntervalDays ?: 3).toString()
        frequencyWeekdaysInput = target?.let { viewModel.parseFrequencyWeekdays(it.frequencyWeekdays) }
            ?.ifEmpty { setOf(DayOfWeek.MONDAY.value) }
            ?: setOf(DayOfWeek.MONDAY.value)
        reminderEnabledInput = target?.reminderEnabled == true
        reminderTimesInput = target?.let { viewModel.parseReminderTimesCsv(it.reminderTime) }?.ifEmpty { listOf("09:00") }
            ?: listOf("09:00")
        reminderMessageInput = target?.reminderMessage.orEmpty()
        createdDateError = false
        reminderTimeError = false
    }

    fun openCreatedDatePicker() {
        val current = runCatching { LocalDate.parse(createdDateInput) }.getOrNull() ?: LocalDate.now()
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

    fun openReminderTimePicker(onSelected: (String) -> Unit) {
        val parsed = runCatching { LocalTime.parse(reminderTimesInput.firstOrNull() ?: "09:00") }.getOrNull()
            ?: LocalTime.of(9, 0)
        TimePickerDialog(
            context,
            { _, hour, minute ->
                onSelected(String.format("%02d:%02d", hour, minute))
                reminderTimeError = false
            },
            parsed.hour,
            parsed.minute,
            true
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Today") },
                actions = {
                    IconButton(onClick = onOpenMotivation) {
                        Icon(Icons.Default.Star, contentDescription = "Open motivation")
                    }
                    IconButton(onClick = onOpenNotifications) {
                        Icon(Icons.Default.Notifications, contentDescription = "Open notifications")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { isQuickActionsVisible = true }) {
                Icon(Icons.Default.Add, contentDescription = "Create")
            }
        }
    ) { innerPadding ->
        ScreenBackground {
            if (!screenState.isDataLoaded && screenState.habits.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    LoadingRow("Loading your daily system...")
                }
            } else if (screenState.habits.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    EmptyStateCard(
                        title = "Start with one habit",
                        message = "Build Today around a single repeatable action, then expand from there.",
                        actionLabel = "Create habit",
                        onAction = {
                            resetHabitEditor(null)
                            isHabitEditorVisible = true
                        }
                    )
                }
            } else {
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
                            PillLabel(
                                text = "Today ${screenState.businessToday.format(DateTimeFormatter.ofPattern("dd MMM"))}",
                                color = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = selectedHabit?.name ?: "Your habits",
                                style = MaterialTheme.typography.displayMedium
                            )
                            Text(
                                text = if (selectedHabit != null) {
                                    "${selectedHabit.currentStreak} day streak. Keep the standard visible and easy to hit."
                                } else {
                                    "Choose a habit and make today's action obvious."
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                InlineProgressLabel("Completed", completionCount.toString(), MyAppTheme.extraColors.success)
                                InlineProgressLabel("Scheduled", scheduledCount.toString(), MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    item {
                        SectionHeader(
                            title = "Habits",
                            subtitle = "Swipe between your active habits"
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            screenState.habits.forEach { habit ->
                                SummaryChip(
                                    text = "${habit.name} ${habit.currentStreak}",
                                    selected = habit.id == screenState.selectedHabitId,
                                    onClick = { viewModel.selectHabit(habit.id) }
                                )
                            }
                        }
                    }

                    item {
                        SurfaceCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { viewModel.showPreviousMonth() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous month")
                                }
                                Text(monthLabel, style = MaterialTheme.typography.headlineMedium)
                                IconButton(onClick = { viewModel.showNextMonth() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next month")
                                }
                            }
                            HabitMonthCalendar(
                                month = screenState.selectedMonth,
                                completedDates = screenState.completedDates,
                                scheduledDates = screenState.scheduledDates,
                                birthdayDates = screenState.globalBirthdayDates,
                                noteDates = screenState.dayNotesByDate.keys,
                                todayDate = screenState.businessToday,
                                onToggleDate = viewModel::toggleDay,
                                onOpenDayNote = { date ->
                                    selectedNoteDate = date
                                    dayNoteInput = screenState.dayNotesByDate[date].orEmpty()
                                    isDayNoteSheetVisible = true
                                },
                                interactive = true
                            )
                        }
                    }

                    item {
                        SectionHeader(title = "Today actions", subtitle = "High-frequency shortcuts for the selected habit")
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SurfaceCard(modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MyAppTheme.extraColors.success)
                                Text("Mark today", style = MaterialTheme.typography.titleMedium)
                                TextButton(onClick = { viewModel.toggleDay(selectedToday) }) {
                                    Text("Complete")
                                }
                            }
                            SurfaceCard(modifier = Modifier.weight(1f)) {
                                Icon(AppIcons.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("Daily note", style = MaterialTheme.typography.titleMedium)
                                TextButton(
                                    onClick = {
                                        selectedNoteDate = selectedToday
                                        dayNoteInput = todayNote
                                        isDayNoteSheetVisible = true
                                    }
                                ) {
                                    Text(if (todayNote.isBlank()) "Add note" else "Edit note")
                                }
                            }
                            SurfaceCard(modifier = Modifier.weight(1f)) {
                                Icon(AppIcons.AccessTime, contentDescription = null, tint = MyAppTheme.extraColors.warning)
                                Text("Insights", style = MaterialTheme.typography.titleMedium)
                                TextButton(
                                    onClick = {
                                        selectedHabit?.let { onOpenStats(it.id) }
                                    },
                                    enabled = selectedHabit != null
                                ) {
                                    Text("Open stats")
                                }
                            }
                        }
                    }

                    item {
                        SectionHeader(title = "Secondary panels", subtitle = "Birthdays, reminders, and tools")
                    }

                    item {
                        SurfaceCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Birthdays", style = MaterialTheme.typography.titleLarge)
                                    Text(
                                        "${screenState.globalBirthdayDates.size} marked days this month",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(onClick = onOpenBirthdays) { Text("Open") }
                            }
                        }
                    }

                    item {
                        SurfaceCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Reminders", style = MaterialTheme.typography.titleLarge)
                                    Text(
                                        if (selectedHabit?.reminderEnabled == true) {
                                            "Habit reminders at ${selectedHabit.reminderTime.orEmpty()}"
                                        } else {
                                            "No reminder enabled for the selected habit"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(onClick = onOpenNotifications) { Text("Manage") }
                            }
                        }
                    }

                    item {
                        SurfaceCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Habit tools", style = MaterialTheme.typography.titleLarge)
                                    Text(
                                        "Edit the selected habit or seed demo data.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row {
                                    IconButton(
                                        onClick = {
                                            resetHabitEditor(selectedHabit)
                                            isHabitEditorVisible = true
                                        },
                                        enabled = selectedHabit != null
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit habit")
                                    }
                                    IconButton(
                                        onClick = { isDeleteConfirmVisible = true },
                                        enabled = selectedHabit != null
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete habit")
                                    }
                                    TextButton(onClick = { viewModel.seedTestData() }) {
                                        Text("Seed")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (isHabitEditorVisible) {
        AppModalSheet(onDismissRequest = { isHabitEditorVisible = false }) {
            SectionHeader(
                title = if (editingHabit == null) "Create habit" else "Edit habit",
                subtitle = "Keep the setup flexible, but default to the simplest schedule possible."
            )
            OutlinedTextField(
                value = habitNameInput,
                onValueChange = { habitNameInput = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Habit name") },
                singleLine = true
            )
            if (editingHabit == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { openCreatedDatePicker() }
                ) {
                    OutlinedTextField(
                        value = createdDateInput,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        readOnly = true,
                        label = { Text("Created date") },
                        isError = createdDateError,
                        trailingIcon = { Icon(AppIcons.CalendarToday, contentDescription = null) }
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Frequency", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryChip(
                        text = "Daily",
                        selected = frequencyTypeInput == HabitFrequencyType.DAILY.name,
                        onClick = { frequencyTypeInput = HabitFrequencyType.DAILY.name }
                    )
                    SummaryChip(
                        text = "Weekly",
                        selected = frequencyTypeInput == HabitFrequencyType.WEEKLY.name,
                        onClick = { frequencyTypeInput = HabitFrequencyType.WEEKLY.name }
                    )
                    SummaryChip(
                        text = "Every N",
                        selected = frequencyTypeInput == HabitFrequencyType.EVERY_N_DAYS.name,
                        onClick = { frequencyTypeInput = HabitFrequencyType.EVERY_N_DAYS.name }
                    )
                }
            }

            if (frequencyTypeInput == HabitFrequencyType.WEEKLY.name) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        DayOfWeek.MONDAY.value to "Mon",
                        DayOfWeek.TUESDAY.value to "Tue",
                        DayOfWeek.WEDNESDAY.value to "Wed",
                        DayOfWeek.THURSDAY.value to "Thu",
                        DayOfWeek.FRIDAY.value to "Fri",
                        DayOfWeek.SATURDAY.value to "Sat",
                        DayOfWeek.SUNDAY.value to "Sun"
                    ).forEach { (value, label) ->
                        SummaryChip(
                            text = label,
                            selected = frequencyWeekdaysInput.contains(value),
                            onClick = {
                                frequencyWeekdaysInput =
                                    if (frequencyWeekdaysInput.contains(value)) frequencyWeekdaysInput - value
                                    else frequencyWeekdaysInput + value
                            }
                        )
                    }
                }
            }

            if (frequencyTypeInput == HabitFrequencyType.EVERY_N_DAYS.name) {
                OutlinedTextField(
                    value = frequencyIntervalInput,
                    onValueChange = { frequencyIntervalInput = it.filter(Char::isDigit).take(3) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Repeat every N days") },
                    singleLine = true
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Reminder", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Optional daily nudge",
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
                        openReminderTimePicker { time ->
                            reminderTimesInput = (reminderTimesInput + time).distinct().sorted()
                        }
                    }
                ) {
                    Text("Add reminder time")
                }
                reminderTimesInput.forEach { time ->
                    PillLabel(
                        text = time,
                        color = MyAppTheme.extraColors.warning,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (reminderTimeError) {
                    Text(
                        text = "Add at least one valid reminder time.",
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
                TextButton(onClick = { isHabitEditorVisible = false }) { Text("Cancel") }
                TextButton(
                    onClick = {
                        val reminderCsv = reminderTimesInput.joinToString(",").takeIf { reminderEnabledInput }
                        val frequencyInterval = frequencyIntervalInput.toIntOrNull()
                        val createdValue = createdDateInput.takeIf { editingHabit == null }
                        val ok = if (editingHabit == null) {
                            viewModel.addHabit(
                                habitName = habitNameInput.trim(),
                                createdDateText = createdValue,
                                frequencyType = frequencyTypeInput,
                                frequencyIntervalDays = frequencyInterval,
                                frequencyWeekdays = frequencyWeekdaysInput,
                                reminderEnabled = reminderEnabledInput,
                                reminderTime = reminderCsv,
                                reminderMessage = reminderMessageInput.takeIf { reminderEnabledInput }
                            )
                        } else {
                            viewModel.updateSelectedHabit(
                                name = habitNameInput.trim(),
                                frequencyType = frequencyTypeInput,
                                frequencyIntervalDays = frequencyInterval,
                                frequencyWeekdays = frequencyWeekdaysInput,
                                reminderEnabled = reminderEnabledInput,
                                reminderTime = reminderCsv,
                                reminderMessage = reminderMessageInput.takeIf { reminderEnabledInput }
                            )
                        }
                        if (!ok) {
                            createdDateError = editingHabit == null && createdDateInput.isNotBlank()
                            reminderTimeError = reminderEnabledInput
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
                        isHabitEditorVisible = false
                    }
                ) {
                    Text(if (editingHabit == null) "Create" else "Update")
                }
            }
        }
    }

    if (isDayNoteSheetVisible) {
        AppModalSheet(onDismissRequest = { isDayNoteSheetVisible = false }) {
            SectionHeader(
                title = "Day note",
                subtitle = "Capture context for ${selectedNoteDate}"
            )
            OutlinedTextField(
                value = dayNoteInput,
                onValueChange = { dayNoteInput = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("What mattered today?") },
                minLines = 6
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { isDayNoteSheetVisible = false }) { Text("Cancel") }
                TextButton(
                    onClick = {
                        viewModel.saveDayNote(selectedNoteDate, dayNoteInput)
                        isDayNoteSheetVisible = false
                    }
                ) { Text("Save") }
            }
        }
    }

    if (isQuickActionsVisible) {
        AppModalSheet(onDismissRequest = { isQuickActionsVisible = false }) {
            SectionHeader(title = "Quick add", subtitle = "Choose the destination, then keep moving.")
            SurfaceCard(modifier = Modifier.clickable {
                resetHabitEditor(null)
                isHabitEditorVisible = true
                isQuickActionsVisible = false
            }) {
                Text("New habit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Create a new repeatable action.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SurfaceCard(modifier = Modifier.clickable {
                isQuickActionsVisible = false
                onOpenTasks()
            }) {
                Text("New task", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Jump to Tasks and add the next item.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SurfaceCard(modifier = Modifier.clickable {
                isQuickActionsVisible = false
                onOpenBirthdays()
            }) {
                Text("New birthday", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Add someone important and schedule reminders.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (isDeleteConfirmVisible) {
        AlertDialog(
            onDismissRequest = { isDeleteConfirmVisible = false },
            title = { Text("Delete habit") },
            text = { Text("Delete ${selectedHabit?.name} and its calendar history?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSelectedHabit()
                        isDeleteConfirmVisible = false
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { isDeleteConfirmVisible = false }) { Text("Cancel") }
            }
        )
    }
}

private data class HabitCalendarCell(
    val date: LocalDate?,
    val dateKey: String? = null,
    val isCompleted: Boolean = false,
    val isScheduled: Boolean = false,
    val hasBirthday: Boolean = false,
    val hasNote: Boolean = false,
    val isFuture: Boolean = false
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HabitMonthCalendar(
    month: YearMonth,
    completedDates: Set<String>,
    scheduledDates: Set<String>,
    birthdayDates: Set<String>,
    noteDates: Set<String>,
    todayDate: LocalDate,
    onToggleDate: (String) -> Unit,
    onOpenDayNote: (String) -> Unit,
    interactive: Boolean
) {
    val cells = remember(month, completedDates, scheduledDates, birthdayDates, noteDates, todayDate) {
        buildHabitCalendarCells(
            month = month,
            completedDates = completedDates,
            scheduledDates = scheduledDates,
            birthdayDates = birthdayDates,
            noteDates = noteDates,
            todayDate = todayDate
        )
    }
    val weekdays = remember { listOf("M", "T", "W", "T", "F", "S", "S") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            weekdays.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                week.forEach { cell ->
                    val backgroundColor = when {
                        cell.date == null -> Color.Transparent
                        cell.isCompleted -> MyAppTheme.extraColors.success.copy(alpha = 0.9f)
                        cell.isScheduled -> MaterialTheme.colorScheme.surfaceVariant
                        else -> MaterialTheme.colorScheme.background.copy(alpha = 0.55f)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(backgroundColor)
                            .then(
                                if (cell.dateKey != null) {
                                    Modifier.combinedClickable(
                                        enabled = interactive && !cell.isFuture,
                                        onClick = { onToggleDate(cell.dateKey) },
                                        onLongClick = {
                                            if (cell.isScheduled) onOpenDayNote(cell.dateKey)
                                        }
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .padding(8.dp)
                    ) {
                        if (cell.date != null) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = cell.date.dayOfMonth.toString(),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (cell.isScheduled || cell.isCompleted) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                    if (cell.hasBirthday) {
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(MyAppTheme.extraColors.accent)
                                                .padding(4.dp)
                                        )
                                    }
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (cell.hasNote) {
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(MyAppTheme.extraColors.warning)
                                                .padding(3.dp)
                                        )
                                    }
                                    if (cell.isCompleted) {
                                        Text(
                                            text = "Done",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    } else if (cell.isFuture) {
                                        Text(
                                            text = "Soon",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun buildHabitCalendarCells(
    month: YearMonth,
    completedDates: Set<String>,
    scheduledDates: Set<String>,
    birthdayDates: Set<String>,
    noteDates: Set<String>,
    todayDate: LocalDate
): List<HabitCalendarCell> {
    val firstDay = month.atDay(1)
    val paddingStart = firstDay.dayOfWeek.value - 1
    val totalDays = month.lengthOfMonth()
    val cells = mutableListOf<HabitCalendarCell>()
    repeat(paddingStart) { cells += HabitCalendarCell(date = null) }
    repeat(totalDays) { offset ->
        val date = month.atDay(offset + 1)
        val key = date.toString()
        cells += HabitCalendarCell(
            date = date,
            dateKey = key,
            isCompleted = completedDates.contains(key),
            isScheduled = scheduledDates.contains(key),
            hasBirthday = birthdayDates.contains(key),
            hasNote = noteDates.contains(key),
            isFuture = date.isAfter(todayDate)
        )
    }
    while (cells.size % 7 != 0) {
        cells += HabitCalendarCell(date = null)
    }
    return cells
}
