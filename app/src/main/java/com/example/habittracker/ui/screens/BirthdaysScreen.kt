package com.example.habittracker.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
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
import com.example.habittracker.ui.viewmodel.BirthdayUiItem
import com.example.habittracker.ui.viewmodel.BirthdaysViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthdaysScreen(
    onBack: () -> Unit = {},
    viewModel: BirthdaysViewModel = hiltViewModel()
) {
    val screenState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var isAddDialogVisible by rememberSaveable { mutableStateOf(false) }
    var isEditDialogVisible by rememberSaveable { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<BirthdayUiItem?>(null) }
    var nameInput by rememberSaveable { mutableStateOf("") }
    var dateInput by rememberSaveable { mutableStateOf("") }
    var reminderDateTimes by remember { mutableStateOf(listOf<Long>()) }
    var pendingDelete by remember { mutableStateOf<BirthdayUiItem?>(null) }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy") }
    val dateTimeFormatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm") }

    fun resetForm() {
        nameInput = ""
        dateInput = ""
        reminderDateTimes = emptyList()
    }

    fun openReminderDateTimePicker() {
        val currentDateTime = LocalDateTime.now()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        val millis = LocalDateTime.of(
                            year,
                            month + 1,
                            dayOfMonth,
                            hour,
                            minute
                        ).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
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
                    isAddDialogVisible = true
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add birthday")
            }
        }
    ) { innerPadding ->
        if (screenState.birthdays.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No birthdays yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Tap + to add one",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(screenState.birthdays, key = { it.id }) { birthday ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Cake, contentDescription = null)
                                    Text(
                                        text = birthday.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Birthday: ${
                                        LocalDate.of(birthday.year, birthday.month, birthday.day).format(dateFormatter)
                                    }",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Next: ${birthday.nextOccurrence.format(dateFormatter)}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Reminders: ${birthday.reminderDateTimes.size}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row {
                                IconButton(
                                    onClick = {
                                        editTarget = birthday
                                        nameInput = birthday.name
                                        dateInput = LocalDate.of(
                                            birthday.year,
                                            birthday.month,
                                            birthday.day
                                        ).toString()
                                        reminderDateTimes = birthday.reminderDateTimes
                                        isEditDialogVisible = true
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

    @Composable
    fun BirthdayFormContent() {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                singleLine = true,
                label = { Text("Name") }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val currentDate = runCatching { LocalDate.parse(dateInput) }.getOrNull()
                            ?: LocalDate.now()
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
            ) {
                OutlinedTextField(
                    value = dateInput,
                    onValueChange = {},
                    enabled = false,
                    singleLine = true,
                    label = { Text("Birthday date (includes year)") },
                    trailingIcon = { Icon(Icons.Default.Cake, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            TextButton(onClick = { openReminderDateTimePicker() }) {
                Text("Add reminder date/time")
            }

            reminderDateTimes.forEach { millis ->
                val text = remember(millis) {
                    Instant.ofEpochMilli(millis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime()
                        .format(dateTimeFormatter)
                }
                AssistChip(
                    onClick = { reminderDateTimes = reminderDateTimes - millis },
                    label = { Text("$text  (tap to remove)") }
                )
            }
        }
    }

    if (isAddDialogVisible) {
        AlertDialog(
            onDismissRequest = { isAddDialogVisible = false },
            title = { Text("Add birthday") },
            text = { BirthdayFormContent() },
            confirmButton = {
                TextButton(
                    onClick = {
                        val parsedDate = runCatching { LocalDate.parse(dateInput) }.getOrNull()
                        if (nameInput.isNotBlank() && parsedDate != null) {
                            viewModel.addBirthday(nameInput, parsedDate, reminderDateTimes)
                            resetForm()
                            isAddDialogVisible = false
                        }
                    }
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { isAddDialogVisible = false }) { Text("Cancel") }
            }
        )
    }

    if (isEditDialogVisible && editTarget != null) {
        AlertDialog(
            onDismissRequest = { isEditDialogVisible = false },
            title = { Text("Edit birthday") },
            text = { BirthdayFormContent() },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = editTarget ?: return@TextButton
                        val parsedDate = runCatching { LocalDate.parse(dateInput) }.getOrNull()
                        if (nameInput.isNotBlank() && parsedDate != null) {
                            viewModel.updateBirthday(
                                id = target.id,
                                name = nameInput,
                                date = parsedDate,
                                reminderDateTimes = reminderDateTimes
                            )
                            isEditDialogVisible = false
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { isEditDialogVisible = false }) { Text("Cancel") }
            }
        )
    }

    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete birthday") },
            text = { Text("Do you want to delete \"${pendingDelete?.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete?.let { viewModel.deleteBirthday(it.id) }
                        pendingDelete = null
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}
