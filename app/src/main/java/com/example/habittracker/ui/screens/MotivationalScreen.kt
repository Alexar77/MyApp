package com.example.habittracker.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.example.habittracker.ui.components.ChartTimeRange
import com.example.habittracker.ui.components.DatePickerField
import com.example.habittracker.ui.components.FilterChipsRow
import com.example.habittracker.ui.components.TrendChart
import com.example.habittracker.ui.viewmodel.MoodEntryUiItem
import com.example.habittracker.ui.viewmodel.MotivationViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotivationScreen(
    onOpenMenu: () -> Unit = {},
    viewModel: MotivationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var selectedMood by rememberSaveable { mutableStateOf("Good") }
    var moodNote by rememberSaveable { mutableStateOf("") }
    var selectedRange by rememberSaveable { mutableStateOf(ChartTimeRange.MONTHLY) }
    var customStartDateInput by rememberSaveable { mutableStateOf(LocalDate.now().minusMonths(1).toString()) }
    var customEndDateInput by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var pendingDelete by remember { mutableStateOf<MoodEntryUiItem?>(null) }

    fun openDatePicker(currentValue: String, onSelected: (String) -> Unit) {
        val current = runCatching { LocalDate.parse(currentValue) }.getOrNull() ?: LocalDate.now()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                onSelected(LocalDate.of(year, month + 1, dayOfMonth).toString())
            },
            current.year,
            current.monthValue - 1,
            current.dayOfMonth
        ).show()
    }

    val chartPoints = remember(state.entries, selectedRange, customStartDateInput, customEndDateInput) {
        viewModel.buildTrendPoints(
            entries = state.entries,
            selectedRange = selectedRange,
            customStart = customStartDateInput,
            customEnd = customEndDateInput
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mood") },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "Open navigation menu")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Mood tracking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        DatePickerField(
                            value = selectedDate,
                            label = "Entry date",
                            onClick = { openDatePicker(selectedDate) { selectedDate = it } }
                        )
                        FilterChipsRow(
                            labels = viewModel.moodOptions(),
                            selected = selectedMood,
                            onSelect = { selectedMood = it }
                        )
                        OutlinedTextField(
                            value = moodNote,
                            onValueChange = { moodNote = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Note") },
                            minLines = 2
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    val parsedDate = runCatching { LocalDate.parse(selectedDate) }.getOrNull()
                                    if (parsedDate != null) {
                                        viewModel.saveMood(parsedDate, selectedMood, moodNote)
                                        moodNote = ""
                                    }
                                }
                            ) {
                                Text("Save mood")
                            }
                        }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Mood graph", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        FilterChipsRow(
                            labels = ChartTimeRange.entries.map { it.label },
                            selected = selectedRange.label,
                            onSelect = { label -> selectedRange = ChartTimeRange.entries.first { it.label == label } }
                        )
                        if (selectedRange == ChartTimeRange.CUSTOM) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                DatePickerField(
                                    value = customStartDateInput,
                                    label = "Start date",
                                    onClick = { openDatePicker(customStartDateInput) { customStartDateInput = it } }
                                )
                                DatePickerField(
                                    value = customEndDateInput,
                                    label = "End date",
                                    onClick = { openDatePicker(customEndDateInput) { customEndDateInput = it } }
                                )
                            }
                        }
                        if (chartPoints.isEmpty()) {
                            Text("No mood entries for the selected range", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            TrendChart(points = chartPoints)
                            Text(
                                text = "Scale: 1 Awful, 2 Low, 3 Okay, 4 Good, 5 Great",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            item {
                Text("Mood history", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (state.entries.isEmpty()) {
                item {
                    Text("No mood entries yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(state.entries.reversed(), key = { it.date }) { entry ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(entry.mood, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = viewModel.formatDate(entry.date),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (entry.note.isNotBlank()) {
                                    Text(entry.note, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            IconButton(onClick = { pendingDelete = entry }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete mood entry")
                            }
                        }
                    }
                }
            }
        }
    }

    if (pendingDelete != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete mood entry") },
            text = { Text("Delete mood entry for ${pendingDelete?.date}?") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete?.let { viewModel.deleteMood(it.date) }
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}
