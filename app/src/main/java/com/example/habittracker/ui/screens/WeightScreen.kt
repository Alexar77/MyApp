package com.example.habittracker.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.habittracker.ui.components.ChartTimeRange
import com.example.habittracker.ui.components.DatePickerField
import com.example.habittracker.ui.components.FabScrollClearance
import com.example.habittracker.ui.components.FilterChipsRow
import com.example.habittracker.ui.components.TrendChart
import com.example.habittracker.ui.components.TrendPoint
import com.example.habittracker.ui.viewmodel.WeightEntryUiItem
import com.example.habittracker.ui.viewmodel.WeightViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.IsoFields

private enum class WeightChartRate(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightScreen(
    onOpenMenu: () -> Unit = {},
    viewModel: WeightViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var isAddDialogVisible by rememberSaveable { mutableStateOf(false) }
    var dateInput by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var weightInput by rememberSaveable { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<WeightEntryUiItem?>(null) }
    var selectedRate by rememberSaveable { mutableStateOf(WeightChartRate.DAILY) }
    var selectedRange by rememberSaveable { mutableStateOf(ChartTimeRange.MONTHLY) }
    var customStartDateInput by rememberSaveable { mutableStateOf(LocalDate.now().minusMonths(1).toString()) }
    var customEndDateInput by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }

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

    val chartPoints = remember(
        state.entries,
        selectedRate,
        selectedRange,
        customStartDateInput,
        customEndDateInput
    ) {
        buildWeightTrendPoints(
            entries = state.entries,
            selectedRate = selectedRate,
            selectedRange = selectedRange,
            customStart = customStartDateInput,
            customEnd = customEndDateInput
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weight") },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "Open navigation menu")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                dateInput = LocalDate.now().toString()
                weightInput = ""
                isAddDialogVisible = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add weight entry")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = 16.dp,
                bottom = FabScrollClearance
            )
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = state.latestWeightKg?.let(viewModel::formatWeight) ?: "No weight logged yet",
                            color = MaterialTheme.colorScheme.primary
                        )
                        state.changeFromFirstKg?.let { delta ->
                            Text(
                                text = "Change: ${viewModel.formatDelta(delta)}",
                                color = if (delta <= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilterChipsRow(
                            labels = WeightChartRate.entries.map { it.label },
                            selected = selectedRate.label,
                            onSelect = { label -> selectedRate = WeightChartRate.entries.first { it.label == label } }
                        )
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
                            Text("No weight data for the selected range", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            TrendChart(points = chartPoints)
                        }
                    }
                }
            }

            item {
                Text("Daily entries", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            if (state.entries.isEmpty()) {
                item {
                    Text("No weight entries yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(state.entries.reversed(), key = { it.date }) { entry ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(viewModel.formatWeight(entry.weightKg), fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = viewModel.formatDate(entry.date),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            IconButton(onClick = { pendingDelete = entry }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete weight entry")
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
            title = { Text("Add weight") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DatePickerField(
                        value = dateInput,
                        label = "Date",
                        onClick = { openDatePicker(dateInput) { dateInput = it } }
                    )
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Weight (kg)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val parsedDate = runCatching { LocalDate.parse(dateInput) }.getOrNull()
                    val parsedWeight = weightInput.toDoubleOrNull()
                    if (parsedDate != null && parsedWeight != null) {
                        viewModel.saveWeight(parsedDate, parsedWeight)
                        isAddDialogVisible = false
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { isAddDialogVisible = false }) { Text("Cancel") }
            }
        )
    }

    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete entry") },
            text = { Text("Delete weight entry for ${pendingDelete?.date}?") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete?.let { viewModel.deleteWeight(it.date) }
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

private fun buildWeightTrendPoints(
    entries: List<WeightEntryUiItem>,
    selectedRate: WeightChartRate,
    selectedRange: ChartTimeRange,
    customStart: String,
    customEnd: String
): List<TrendPoint> {
    val today = LocalDate.now()
    val rawStartDate = when (selectedRange) {
        ChartTimeRange.WEEKLY -> today.minusWeeks(1)
        ChartTimeRange.MONTHLY -> today.minusMonths(1)
        ChartTimeRange.YEARLY -> today.minusYears(1)
        ChartTimeRange.CUSTOM -> runCatching { LocalDate.parse(customStart) }.getOrElse { today.minusMonths(1) }
    }
    val rawEndDate = when (selectedRange) {
        ChartTimeRange.CUSTOM -> runCatching { LocalDate.parse(customEnd) }.getOrElse { today }
        else -> today
    }
    val startDate = minOf(rawStartDate, rawEndDate)
    val endDate = maxOf(rawStartDate, rawEndDate)

    return entries
        .mapNotNull { entry ->
            val date = runCatching { LocalDate.parse(entry.date) }.getOrNull() ?: return@mapNotNull null
            if (date.isBefore(startDate) || date.isAfter(endDate)) return@mapNotNull null
            date to entry.weightKg
        }
        .groupBy(
            keySelector = { (date, _) ->
                when (selectedRate) {
                    WeightChartRate.DAILY -> date.toString()
                    WeightChartRate.WEEKLY -> "${date.get(IsoFields.WEEK_BASED_YEAR)}-W${date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}"
                    WeightChartRate.MONTHLY -> YearMonth.from(date).toString()
                    WeightChartRate.YEARLY -> date.year.toString()
                }
            },
            valueTransform = { it.second }
        )
        .toSortedMap()
        .map { (label, values) -> TrendPoint(label = label, value = values.average()) }
}
