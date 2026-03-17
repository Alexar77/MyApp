package com.example.habittracker.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.example.habittracker.ui.viewmodel.BirthdayUiItem
import com.example.habittracker.ui.viewmodel.BirthdaysViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthdaysScreen(
    onBack: () -> Unit = {},
    viewModel: BirthdaysViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val today = remember(state.birthdays) { LocalDate.now() }

    var isEditorVisible by rememberSaveable { mutableStateOf(false) }
    var editingBirthdayId by rememberSaveable { mutableStateOf<Long?>(null) }
    var nameInput by rememberSaveable { mutableStateOf("") }
    var dateInput by rememberSaveable { mutableStateOf("") }
    var reminderDateTimes by remember { mutableStateOf(listOf<Long>()) }
    var pendingDelete by remember { mutableStateOf<BirthdayUiItem?>(null) }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy") }
    val dateTimeFormatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm") }
    val nextBirthday = state.birthdays.firstOrNull()

    fun resetForm(target: BirthdayUiItem? = null) {
        editingBirthdayId = target?.id
        nameInput = target?.name.orEmpty()
        dateInput = target?.let { LocalDate.of(it.year, it.month, it.day).toString() }.orEmpty()
        reminderDateTimes = target?.reminderDateTimes.orEmpty()
    }

    fun openReminderPicker() {
        val currentDateTime = LocalDateTime.now()
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
                        reminderDateTimes = (reminderDateTimes + millis).distinct().sorted()
                    },
                    currentDateTime.hour,
                    currentDateTime.minute,
                    true
                ).show()
            },
            currentDateTime.year,
            currentDateTime.monthValue - 1,
            currentDateTime.dayOfMonth
        ).show()
    }

    fun openDatePicker() {
        val currentDate = runCatching { LocalDate.parse(dateInput) }.getOrNull() ?: LocalDate.now()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                dateInput = LocalDate.of(year, month + 1, dayOfMonth).toString()
            },
            currentDate.year,
            currentDate.monthValue - 1,
            currentDate.dayOfMonth
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Birthdays") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    resetForm()
                    isEditorVisible = true
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add birthday")
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
                    if (nextBirthday != null) {
                        val daysAway = ChronoUnit.DAYS.between(today, nextBirthday.nextOccurrence)
                        PremiumCard(accent = MyAppTheme.extraColors.accent) {
                            PillLabel(
                                text = if (daysAway == 0L) "Today" else "$daysAway days away",
                                color = MyAppTheme.extraColors.accent,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = nextBirthday.name,
                                style = MaterialTheme.typography.displayMedium
                            )
                            Text(
                                text = "Next celebration on ${nextBirthday.nextOccurrence.format(dateFormatter)}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        EmptyStateCard(
                            title = "No birthdays yet",
                            message = "Add people you want to remember and schedule reminders that feel intentional.",
                            actionLabel = "Add birthday",
                            onAction = {
                                resetForm()
                                isEditorVisible = true
                            }
                        )
                    }
                }

                state.birthdays.groupBy { it.nextOccurrence.month }.forEach { (month, birthdays) ->
                    item {
                        SectionHeader(
                            title = month.name.lowercase().replaceFirstChar(Char::titlecase),
                            subtitle = "${birthdays.size} upcoming"
                        )
                    }
                    items(birthdays, key = { it.id }) { birthday ->
                        val daysAway = ChronoUnit.DAYS.between(today, birthday.nextOccurrence)
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
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(AppIcons.Cake, contentDescription = null, tint = MyAppTheme.extraColors.warning)
                                        Text(
                                            birthday.name,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Text(
                                        "Birthday: ${LocalDate.of(birthday.year, birthday.month, birthday.day).format(dateFormatter)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        PillLabel(
                                            text = if (daysAway == 0L) "Today" else "In $daysAway days",
                                            color = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        )
                                        PillLabel(
                                            text = "${birthday.reminderDateTimes.size} reminders",
                                            color = MyAppTheme.extraColors.warning,
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                Row {
                                    IconButton(
                                        onClick = {
                                            resetForm(birthday)
                                            isEditorVisible = true
                                        }
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit birthday")
                                    }
                                    IconButton(onClick = { pendingDelete = birthday }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete birthday")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (isEditorVisible) {
        AppModalSheet(onDismissRequest = { isEditorVisible = false }) {
            SectionHeader(
                title = if (editingBirthdayId == null) "Add birthday" else "Edit birthday",
                subtitle = "Clear date, reminders, and a cleaner celebration overview."
            )
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Name") },
                singleLine = true
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { openDatePicker() }
            ) {
                OutlinedTextField(
                    value = dateInput,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Birthday date") },
                    trailingIcon = { Icon(AppIcons.Cake, contentDescription = null) }
                )
            }
            SurfaceCard(contentPadding = PaddingValues(14.dp)) {
                SectionHeader(
                    title = "Reminders",
                    subtitle = if (reminderDateTimes.isEmpty()) "No reminders yet" else "${reminderDateTimes.size} scheduled"
                )
                TextButton(onClick = { openReminderPicker() }) {
                    Text("Add reminder")
                }
                if (reminderDateTimes.isNotEmpty()) {
                    reminderDateTimes.forEach { millis ->
                        PillLabel(
                            text = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime()
                                .format(dateTimeFormatter),
                            color = MyAppTheme.extraColors.warning,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { isEditorVisible = false }) { Text("Cancel") }
                TextButton(
                    onClick = {
                        val parsedDate = runCatching { LocalDate.parse(dateInput) }.getOrNull()
                        if (nameInput.isBlank() || parsedDate == null) return@TextButton
                        if (editingBirthdayId == null) {
                            viewModel.addBirthday(nameInput.trim(), parsedDate, reminderDateTimes)
                        } else {
                            viewModel.updateBirthday(editingBirthdayId!!, nameInput.trim(), parsedDate, reminderDateTimes)
                        }
                        isEditorVisible = false
                    }
                ) {
                    Text(if (editingBirthdayId == null) "Save" else "Update")
                }
            }
        }
    }

    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete birthday") },
            text = { Text("Remove ${pendingDelete?.name} and all birthday reminders?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete?.let { viewModel.deleteBirthday(it.id) }
                        pendingDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}
